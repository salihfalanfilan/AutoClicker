package com.kinetic.autoclicker.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.kinetic.autoclicker.domain.engine.AutomationStateStore
import com.kinetic.autoclicker.domain.model.AutomationEngineEvent
import com.kinetic.autoclicker.domain.model.StopConditionType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Root gerektirmeyen resmi AccessibilityService tıklama ve kaydırma motoru.
 * - Ekran kapanma/kilitlenme durumunda otomatik durdurma (Intent.ACTION_SCREEN_OFF)
 * - Ekran döndürmede (onConfigurationChanged) gerçek cam koordinatlarını güncelleme
 * - Yüzen HUD barına dokunulduğu anda (isUserInteractingWithHud) jest çakışmasını engelleme
 * - GestureExecutor üzerinden suspendCancellableCoroutine tabanlı senkron jest yürütücüsü
 */
@AndroidEntryPoint
class ClickAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var stateStore: AutomationStateStore

    @Inject
    lateinit var gestureExecutor: GestureExecutor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var executionJob: Job? = null
    private var isScreenOffReceiverRegistered = false

    private var isSystemKillSwitchesRegistered = false

    private val systemKillSwitchesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    triggerEmergencyStop("Emergency Stop: Screen turned off")
                }
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    val reason = intent.getStringExtra("reason")
                    if (reason == "statusbar_expanded" || reason == "notification_panel") {
                        if (stateStore.state.value.isRunning) {
                            triggerEmergencyStop("Bildirim çekmecesi açıldı, otomatik durduruldu")
                        }
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        syncRealScreenMetrics()
        registerSystemKillSwitches()
        stateStore.updatePermissions(isAccessibilityConnected = true)
        observeEngineRunningState()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == "com.android.systemui") {
            val cls = event.className?.toString().orEmpty()
            // Bildirim çekmecesi, durum çubuğu paneli veya hızlı ayarlar açıldığında motoru derhal durdur
            val isNotificationShade = cls.contains("Notification", ignoreCase = true) ||
                cls.contains("StatusBar", ignoreCase = true) ||
                cls.contains("QuickSettings", ignoreCase = true) ||
                cls.contains("Panel", ignoreCase = true) ||
                cls.contains("Shade", ignoreCase = true) ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

            if (isNotificationShade && stateStore.state.value.isRunning) {
                triggerEmergencyStop("Bildirim çekmecesi açıldı, otomatik durduruldu")
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        syncRealScreenMetrics()
    }

    override fun onInterrupt() {
        triggerEmergencyStop("Automation interrupted by system")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        executionJob?.cancel()
        unregisterSystemKillSwitches()
        stateStore.setRunning(false)
        stateStore.updatePermissions(isAccessibilityConnected = false)
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        executionJob?.cancel()
        unregisterSystemKillSwitches()
        serviceScope.cancel()
        instance = null
        super.onDestroy()
    }

    private fun triggerEmergencyStop(reason: String) {
        executionJob?.cancel()
        executionJob = null
        stateStore.emergencyStop(reason)
    }

    private fun registerSystemKillSwitches() {
        if (isSystemKillSwitchesRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemKillSwitchesReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(systemKillSwitchesReceiver, filter)
        }
        isSystemKillSwitchesRegistered = true
    }

    private fun unregisterSystemKillSwitches() {
        if (!isSystemKillSwitchesRegistered) return
        runCatching { unregisterReceiver(systemKillSwitchesReceiver) }
        isSystemKillSwitchesRegistered = false
    }

    @Suppress("DEPRECATION")
    private fun syncRealScreenMetrics() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            stateStore.onScreenConfigurationChanged(bounds.width(), bounds.height())
        } else {
            val metrics = DisplayMetrics()
            wm.defaultDisplay?.getRealMetrics(metrics)
            if (metrics.widthPixels > 0 && metrics.heightPixels > 0) {
                stateStore.onScreenConfigurationChanged(metrics.widthPixels, metrics.heightPixels)
            }
        }
    }

    private fun observeEngineRunningState() {
        serviceScope.launch {
            stateStore.state
                .map { it.isRunning && it.isOverlayVisible }
                .distinctUntilChanged()
                .collect { shouldRun ->
                    if (shouldRun) {
                        startGestureExecutionLoop()
                    } else {
                        executionJob?.cancel()
                        executionJob = null
                    }
                }
        }
    }

    private fun startGestureExecutionLoop() {
        executionJob?.cancel()
        executionJob = serviceScope.launch {
            val startRealtimeMs = SystemClock.elapsedRealtime()
            var targetCursor = 0
            var executedCount = 0L

            while (isActive && stateStore.state.value.isRunning && stateStore.state.value.isOverlayVisible) {
                val snapshot = stateStore.state.value

                // Kullanıcı yüzen HUD barına dokunuyorsa jest gönderimini beklet; HUD kontrolü her zaman önceliklidir!
                if (snapshot.isUserInteractingWithHud) {
                    delay(16L)
                    continue
                }

                val activeTargets = snapshot.targets.filter { it.enabled }

                if (activeTargets.isEmpty()) {
                    stateStore.setRunning(false)
                    break
                }

                val currentIndex = targetCursor % activeTargets.size
                val target = activeTargets[currentIndex]
                val settings = snapshot.globalSettings
                val stopCondition = settings.stopCondition

                // 1. Durdurma koşullarını (Süre veya Tıklama Sayısı limiti) kontrol et
                val elapsedMs = SystemClock.elapsedRealtime() - startRealtimeMs
                when (stopCondition.type) {
                    StopConditionType.TIMER_LIMIT -> {
                        val maxMs = stopCondition.timerMinutes.coerceAtLeast(1) * 60_000L
                        if (elapsedMs >= maxMs) {
                            stateStore.setRunning(false)
                            stateStore.emitEvent(
                                AutomationEngineEvent.StatusToast("Timer limit (${stopCondition.timerMinutes}m) reached")
                            )
                            break
                        }
                    }
                    StopConditionType.CYCLE_COUNT -> {
                        val maxClicks = stopCondition.cycleCount.coerceAtLeast(1).toLong()
                        if (executedCount >= maxClicks) {
                            stateStore.setRunning(false)
                            stateStore.emitEvent(
                                AutomationEngineEvent.StatusToast("Completed $maxClicks clicks")
                            )
                            break
                        }
                    }
                    StopConditionType.INFINITE -> Unit
                }

                if (!stateStore.state.value.isRunning || stateStore.state.value.isUserInteractingWithHud) {
                    delay(16L)
                    continue
                }

                // 2. GestureExecutor ile jesti asenkron gönder ve tamamlanmasını (onCompleted/onCancelled) bekle
                val outcome = gestureExecutor.executeTargetGesture(
                    service = this@ClickAccessibilityService,
                    target = target,
                    settings = settings,
                    screenWidthPx = snapshot.screenWidthPx,
                    screenHeightPx = snapshot.screenHeightPx
                )

                if (!stateStore.state.value.isRunning) break

                executedCount++
                val updatedElapsed = SystemClock.elapsedRealtime() - startRealtimeMs
                stateStore.recordGestureExecution(
                    targetIndex = currentIndex,
                    targetId = target.id,
                    x = outcome.dispatchedX,
                    y = outcome.dispatchedY,
                    elapsedMs = updatedElapsed
                )

                targetCursor = (currentIndex + 1) % activeTargets.size

                // 3. Sonraki adıma geçmeden önce normalize edilmiş (en az 10ms) bekleme süresini uygula
                val intervalDelayMs = target.intervalMs.coerceAtLeast(10L)
                delay(intervalDelayMs)
            }
        }
    }

    companion object {
        @Volatile
        var instance: ClickAccessibilityService? = null
            private set

        fun isConnected(): Boolean = instance != null
    }
}
