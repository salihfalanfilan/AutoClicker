package com.kinetic.autoclicker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.kinetic.autoclicker.R
import com.kinetic.autoclicker.domain.engine.AutomationStateStore
import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.domain.model.HudDisplayMode
import com.kinetic.autoclicker.domain.model.TargetPoint
import com.kinetic.autoclicker.domain.repository.EngineSettingsRepository
import com.kinetic.autoclicker.presentation.MainActivity
import com.kinetic.autoclicker.presentation.components.AddTargetTypeModalOverlay
import com.kinetic.autoclicker.presentation.components.ClickSettingsModalOverlay
import com.kinetic.autoclicker.presentation.components.FloatingHudContainer
import com.kinetic.autoclicker.presentation.components.SwipeDirectionLineCanvas
import com.kinetic.autoclicker.presentation.components.SwipeEndReticlePin
import com.kinetic.autoclicker.presentation.components.TargetReticlePin
import com.kinetic.autoclicker.presentation.theme.KineticAutoClickerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * WindowManager overlay service that draws the Floating HUD Bar, Edge Capsule Pill,
 * Draggable Target Reticle Pins, Draggable Swipe End Pins, Directional Swipe Arrow Lines,
 * Add Target Type Chooser, and Click Settings Dialog strictly via Jetpack ComposeView.
 */
@AndroidEntryPoint
class FloatingHudService : Service() {

    @Inject
    lateinit var stateStore: AutomationStateStore

    @Inject
    lateinit var settingsRepository: EngineSettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var windowManager: WindowManager
    private lateinit var overlayLifecycleOwner: ComposeOverlayLifecycleOwner

    private var hudComposeView: ComposeView? = null
    private var hudLayoutParams: WindowManager.LayoutParams? = null
    private var hudExactX: Float = 24f
    private var hudExactY: Float = 260f

    private var swipeLinesComposeView: ComposeView? = null
    private var settingsDialogComposeView: ComposeView? = null
    private var addTargetDialogComposeView: ComposeView? = null
    private var isScreenOffReceiverRegistered = false

    private class TargetPinWindow(
        val targetId: String,
        val composeView: ComposeView,
        val layoutParams: WindowManager.LayoutParams,
        var centerX: Float,
        var centerY: Float
    )

    private val targetPinWindows = mutableMapOf<String, TargetPinWindow>()
    private val swipeEndPinWindows = mutableMapOf<String, TargetPinWindow>()

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                stateStore.emergencyStop("Emergency Stop: Screen turned off")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayLifecycleOwner = ComposeOverlayLifecycleOwner().also { it.onCreate() }
        syncRealScreenDimensions()
        registerScreenOffReceiver()
        startForegroundWithNotification()
        observeOverlayState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                stateStore.disableOverlayAndStop()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                syncRealScreenDimensions()
                if (Settings.canDrawOverlays(this)) {
                    ensureHudWindowAttached()
                }
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        syncRealScreenDimensions()
        repositionWindowsAfterRotation()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterScreenOffReceiver()
        removeAllOverlayWindows()
        overlayLifecycleOwner.onDestroy()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerScreenOffReceiver() {
        if (isScreenOffReceiverRegistered) return
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, filter)
        }
        isScreenOffReceiverRegistered = true
    }

    private fun unregisterScreenOffReceiver() {
        if (!isScreenOffReceiverRegistered) return
        runCatching { unregisterReceiver(screenOffReceiver) }
        isScreenOffReceiverRegistered = false
    }

    @Suppress("DEPRECATION")
    private fun syncRealScreenDimensions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            stateStore.onScreenConfigurationChanged(bounds.width(), bounds.height())
        } else {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay?.getRealMetrics(metrics)
            if (metrics.widthPixels > 0 && metrics.heightPixels > 0) {
                stateStore.onScreenConfigurationChanged(metrics.widthPixels, metrics.heightPixels)
            }
        }
    }

    private fun repositionWindowsAfterRotation() {
        val currentState = stateStore.state.value
        val pinHalfSizePx = (28 * resources.displayMetrics.density).roundToInt()

        hudComposeView?.let { view ->
            hudLayoutParams?.let { lp ->
                hudExactX = currentState.hudPositionX.toFloat()
                hudExactY = currentState.hudPositionY.toFloat()
                lp.x = currentState.hudPositionX
                lp.y = currentState.hudPositionY
                runCatching { windowManager.updateViewLayout(view, lp) }
            }
        }

        currentState.targets.forEach { target ->
            targetPinWindows[target.id]?.let { pinWindow ->
                pinWindow.centerX = target.x
                pinWindow.centerY = target.y
                pinWindow.layoutParams.x = target.x.roundToInt() - pinHalfSizePx
                pinWindow.layoutParams.y = target.y.roundToInt() - pinHalfSizePx
                runCatching {
                    windowManager.updateViewLayout(pinWindow.composeView, pinWindow.layoutParams)
                }
            }

            if (target.actionType == ActionType.SWIPE) {
                swipeEndPinWindows[target.id]?.let { endWindow ->
                    val ex = target.endX ?: (target.x + 200f)
                    val ey = target.endY ?: target.y
                    endWindow.centerX = ex
                    endWindow.centerY = ey
                    endWindow.layoutParams.x = ex.roundToInt() - pinHalfSizePx
                    endWindow.layoutParams.y = ey.roundToInt() - pinHalfSizePx
                    runCatching {
                        windowManager.updateViewLayout(endWindow.composeView, endWindow.layoutParams)
                    }
                }
            }
        }
    }

    private fun observeOverlayState() {
        // Immediate reaction to isRunning changes for Touch Passthrough (Section 2.D)
        serviceScope.launch {
            stateStore.state
                .map { it.isRunning }
                .distinctUntilChanged()
                .collect { isRunning ->
                    updateTargetPinsTouchableFlag(isTouchable = !isRunning)
                }
        }

        // Observe overall visibility, targets, and modal dialogs
        serviceScope.launch {
            stateStore.state.collect { state ->
                if (!Settings.canDrawOverlays(this@FloatingHudService) || !state.isOverlayVisible) {
                    removeAllOverlayWindows()
                    if (!state.isOverlayVisible) {
                        stopSelf()
                    }
                    return@collect
                }

                syncSwipeLinesWindow(
                    hasSwipeTargets = state.targets.any { it.actionType == ActionType.SWIPE }
                )
                val addedNewPinWindow = syncTargetPinWindows(
                    targets = state.targets,
                    isRunning = state.isRunning
                )
                if (hudComposeView == null) {
                    ensureHudWindowAttached()
                } else if (addedNewPinWindow) {
                    bringHudWindowToFront()
                }
                syncAddTargetDialogWindow(state.isAddTargetDialogVisible)
                syncSettingsDialogWindow(state.isClickSettingsDialogVisible)
            }
        }
    }

    private fun applyCutoutAndFullscreenParams(params: WindowManager.LayoutParams) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    /**
     * Brings the Floating HUD window to the top safely via updateViewLayout
     * without destroying and recreating the window, preventing launcher overlay alerts.
     */
    private fun bringHudWindowToFront() {
        val currentView = hudComposeView ?: return
        val lp = hudLayoutParams ?: return
        runCatching {
            windowManager.updateViewLayout(currentView, lp)
        }
    }

    private fun syncSwipeLinesWindow(hasSwipeTargets: Boolean) {
        if (!hasSwipeTargets || !Settings.canDrawOverlays(this)) {
            swipeLinesComposeView?.let { view ->
                runCatching { windowManager.removeView(view) }
            }
            swipeLinesComposeView = null
            return
        }

        if (swipeLinesComposeView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            applyCutoutAndFullscreenParams(this)
        }

        val view = ComposeView(this).apply {
            overlayLifecycleOwner.attachToView(this)
            setContent {
                val engineState by stateStore.state.collectAsState()
                KineticAutoClickerTheme(settings = engineState.globalSettings) {
                    val pairs = engineState.targets
                        .filter { it.actionType == ActionType.SWIPE }
                        .map { target ->
                            val start = Offset(target.x, target.y)
                            val end = Offset(
                                target.endX ?: (target.x + 200f),
                                target.endY ?: target.y
                            )
                            start to end
                        }
                    SwipeDirectionLineCanvas(
                        swipePairs = pairs,
                        isEngineRunning = engineState.isRunning,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        runCatching {
            windowManager.addView(view, params)
            swipeLinesComposeView = view
        }
    }

    private fun ensureHudWindowAttached() {
        if (hudComposeView != null || !Settings.canDrawOverlays(this)) return

        val currentState = stateStore.state.value
        val screenWidth = currentState.screenWidthPx.coerceAtLeast(320)
        val screenHeight = currentState.screenHeightPx.coerceAtLeast(480)
        val safeX = currentState.hudPositionX.coerceIn(8, (screenWidth - 80).coerceAtLeast(8))
        val safeY = currentState.hudPositionY.coerceIn(48, (screenHeight - 140).coerceAtLeast(48))

        hudExactX = safeX.toFloat()
        hudExactY = safeY.toFloat()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = safeX
            y = safeY
            applyCutoutAndFullscreenParams(this)
        }

        val view = ComposeView(this).apply {
            overlayLifecycleOwner.attachToView(this)
            setContent {
                val engineState by stateStore.state.collectAsState()
                KineticAutoClickerTheme(settings = engineState.globalSettings) {
                    FloatingHudContainer(
                        hudDisplayMode = engineState.hudDisplayMode,
                        isRunning = engineState.isRunning,
                        intervalMs = engineState.activeIntervalMs,
                        hudOpacity = engineState.globalSettings.hudOpacity,
                        isDockedRightEdge = engineState.isDockedRightEdge,
                        onHudTouchActiveChanged = { active ->
                            stateStore.setUserInteractingWithHud(active)
                        },
                        onTogglePlayPause = {
                            val nextRunning = !engineState.isRunning
                            updateTargetPinsTouchableFlag(isTouchable = !nextRunning)
                            stateStore.setRunning(nextRunning)
                        },
                        onAddTarget = {
                            // Opens chooser asking "Tıklama (Tap) mı, Kaydırma (Swipe) mı?"
                            stateStore.openAddTargetDialog()
                        },
                        onRemoveTarget = {
                            stateStore.removeLastTarget()
                        },
                        onOpenSettings = {
                            stateStore.openClickSettingsDialog(
                                targetId = engineState.targets.firstOrNull()?.id
                            )
                        },
                        onCollapseToEdgePill = {
                            val screenWidth = engineState.screenWidthPx
                            val dockRight = (hudLayoutParams?.x ?: 0) > screenWidth / 2
                            val snappedX = if (dockRight) (screenWidth - 220).coerceAtLeast(0) else 8
                            hudExactX = snappedX.toFloat()
                            hudLayoutParams?.let { lp ->
                                lp.x = snappedX
                                runCatching { windowManager.updateViewLayout(this, lp) }
                            }
                            stateStore.updateHudPosition(
                                x = snappedX,
                                y = hudLayoutParams?.y ?: engineState.hudPositionY,
                                isRightEdge = dockRight
                            )
                            stateStore.setHudDisplayMode(HudDisplayMode.EDGE_PILL)
                        },
                        onExpandFromEdgePill = {
                            stateStore.setHudDisplayMode(HudDisplayMode.EXPANDED_BAR)
                        },
                        onCloseOverlay = {
                            stateStore.disableOverlayAndStop()
                            stopSelf()
                        },
                        onDragHud = { dx, dy ->
                            hudLayoutParams?.let { lp ->
                                val screenWidth = engineState.screenWidthPx.toFloat().coerceAtLeast(320f)
                                val screenHeight = engineState.screenHeightPx.toFloat().coerceAtLeast(480f)
                                hudExactX = (hudExactX + dx).coerceIn(0f, (screenWidth - 80f).coerceAtLeast(0f))
                                hudExactY = (hudExactY + dy).coerceIn(16f, (screenHeight - 140f).coerceAtLeast(16f))
                                lp.x = hudExactX.roundToInt()
                                lp.y = hudExactY.roundToInt()
                                runCatching { windowManager.updateViewLayout(this, lp) }
                                stateStore.updateHudPosition(
                                    x = lp.x,
                                    y = lp.y,
                                    isRightEdge = lp.x > screenWidth / 2
                                )
                            }
                        }
                    )
                }
            }
        }

        runCatching {
            windowManager.addView(view, params)
            hudComposeView = view
            hudLayoutParams = params
        }
    }

    private fun buildTargetPinFlags(isTouchable: Boolean): Int {
        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        return if (isTouchable) {
            baseFlags
        } else {
            baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
    }

    private fun syncTargetPinWindows(targets: List<TargetPoint>, isRunning: Boolean): Boolean {
        if (!Settings.canDrawOverlays(this)) return false
        var addedAnyWindow = false

        val desiredIds = targets.map { it.id }.toSet()
        val desiredSwipeIds = targets.filter { it.actionType == ActionType.SWIPE }.map { it.id }.toSet()

        val startIterator = targetPinWindows.entries.iterator()
        while (startIterator.hasNext()) {
            val (id, pinWindow) = startIterator.next()
            if (id !in desiredIds) {
                runCatching { windowManager.removeView(pinWindow.composeView) }
                startIterator.remove()
            }
        }

        val endIterator = swipeEndPinWindows.entries.iterator()
        while (endIterator.hasNext()) {
            val (id, endWindow) = endIterator.next()
            if (id !in desiredSwipeIds) {
                runCatching { windowManager.removeView(endWindow.composeView) }
                endIterator.remove()
            }
        }

        val pinHalfSizePx = (28 * resources.displayMetrics.density).roundToInt()

        targets.forEach { target ->
            val existing = targetPinWindows[target.id]
            if (existing == null) {
                val flags = buildTargetPinFlags(isTouchable = !isRunning)

                val lp = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    flags,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = target.x.roundToInt() - pinHalfSizePx
                    y = target.y.roundToInt() - pinHalfSizePx
                    applyCutoutAndFullscreenParams(this)
                }

                val targetId = target.id
                val pinView = ComposeView(this).apply {
                    overlayLifecycleOwner.attachToView(this)
                    setContent {
                        val engineState by stateStore.state.collectAsState()
                        KineticAutoClickerTheme(settings = engineState.globalSettings) {
                            val liveTarget = engineState.targets.find { it.id == targetId }
                            if (liveTarget != null) {
                                val isCurrentActive = engineState.isRunning &&
                                    engineState.targets.indexOfFirst { it.id == targetId } == engineState.activeTargetIndex
                                TargetReticlePin(
                                    orderIndex = liveTarget.orderIndex,
                                    actionType = liveTarget.actionType,
                                    isActive = engineState.isRunning,
                                    isPulsing = isCurrentActive,
                                    isSelected = false,
                                    showDragHint = false,
                                    onTap = {
                                        if (!engineState.isRunning) {
                                            stateStore.openClickSettingsDialog(targetId)
                                        }
                                    },
                                    onDrag = { dx, dy ->
                                        if (!engineState.isRunning) {
                                            val window = targetPinWindows[targetId] ?: return@TargetReticlePin
                                            val screenW = engineState.screenWidthPx.toFloat().coerceAtLeast(320f)
                                            val screenH = engineState.screenHeightPx.toFloat().coerceAtLeast(480f)
                                            window.centerX = (window.centerX + dx).coerceIn(16f, screenW - 16f)
                                            window.centerY = (window.centerY + dy).coerceIn(16f, screenH - 16f)
                                            window.layoutParams.x = window.centerX.roundToInt() - pinHalfSizePx
                                            window.layoutParams.y = window.centerY.roundToInt() - pinHalfSizePx
                                            runCatching {
                                                windowManager.updateViewLayout(window.composeView, window.layoutParams)
                                            }
                                            stateStore.updateTargetPosition(targetId, window.centerX, window.centerY)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                runCatching {
                    windowManager.addView(pinView, lp)
                    targetPinWindows[targetId] = TargetPinWindow(
                        targetId = targetId,
                        composeView = pinView,
                        layoutParams = lp,
                        centerX = target.x,
                        centerY = target.y
                    )
                    addedAnyWindow = true
                }
            } else {
                val expectedX = target.x.roundToInt() - pinHalfSizePx
                val expectedY = target.y.roundToInt() - pinHalfSizePx
                val expectedFlags = buildTargetPinFlags(isTouchable = !isRunning)
                val posDiffers = abs(existing.layoutParams.x - expectedX) > 2 ||
                    abs(existing.layoutParams.y - expectedY) > 2

                if (posDiffers || existing.layoutParams.flags != expectedFlags) {
                    if (posDiffers) {
                        existing.centerX = target.x
                        existing.centerY = target.y
                        existing.layoutParams.x = expectedX
                        existing.layoutParams.y = expectedY
                    }
                    existing.layoutParams.flags = expectedFlags
                    runCatching {
                        windowManager.updateViewLayout(existing.composeView, existing.layoutParams)
                    }
                }
            }

            // If this target is a SWIPE, also create/sync its draggable End Pin (#NB)
            if (target.actionType == ActionType.SWIPE) {
                val endX = target.endX ?: (target.x + 200f)
                val endY = target.endY ?: target.y
                val existingEnd = swipeEndPinWindows[target.id]

                if (existingEnd == null) {
                    val flags = buildTargetPinFlags(isTouchable = !isRunning)
                    val endLp = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        flags,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.START
                        x = endX.roundToInt() - pinHalfSizePx
                        y = endY.roundToInt() - pinHalfSizePx
                        applyCutoutAndFullscreenParams(this)
                    }

                    val targetId = target.id
                    val endPinView = ComposeView(this).apply {
                        overlayLifecycleOwner.attachToView(this)
                        setContent {
                            val engineState by stateStore.state.collectAsState()
                            KineticAutoClickerTheme(settings = engineState.globalSettings) {
                                val liveTarget = engineState.targets.find { it.id == targetId }
                                if (liveTarget != null) {
                                    SwipeEndReticlePin(
                                        orderIndex = liveTarget.orderIndex,
                                        isActive = engineState.isRunning,
                                        onTap = {
                                            if (!engineState.isRunning) {
                                                stateStore.openClickSettingsDialog(targetId)
                                            }
                                        },
                                        onDrag = { dx, dy ->
                                            if (!engineState.isRunning) {
                                                val window = swipeEndPinWindows[targetId] ?: return@SwipeEndReticlePin
                                                val screenW = engineState.screenWidthPx.toFloat().coerceAtLeast(320f)
                                                val screenH = engineState.screenHeightPx.toFloat().coerceAtLeast(480f)
                                                window.centerX = (window.centerX + dx).coerceIn(16f, screenW - 16f)
                                                window.centerY = (window.centerY + dy).coerceIn(16f, screenH - 16f)
                                                window.layoutParams.x = window.centerX.roundToInt() - pinHalfSizePx
                                                window.layoutParams.y = window.centerY.roundToInt() - pinHalfSizePx
                                                runCatching {
                                                    windowManager.updateViewLayout(window.composeView, window.layoutParams)
                                                }
                                                stateStore.updateSwipeEndPosition(targetId, window.centerX, window.centerY)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    runCatching {
                        windowManager.addView(endPinView, endLp)
                        swipeEndPinWindows[targetId] = TargetPinWindow(
                            targetId = targetId,
                            composeView = endPinView,
                            layoutParams = endLp,
                            centerX = endX,
                            centerY = endY
                        )
                        addedAnyWindow = true
                    }
                } else {
                    val expectedEndX = endX.roundToInt() - pinHalfSizePx
                    val expectedEndY = endY.roundToInt() - pinHalfSizePx
                    val expectedFlags = buildTargetPinFlags(isTouchable = !isRunning)
                    val posDiffers = abs(existingEnd.layoutParams.x - expectedEndX) > 2 ||
                        abs(existingEnd.layoutParams.y - expectedEndY) > 2

                    if (posDiffers || existingEnd.layoutParams.flags != expectedFlags) {
                        if (posDiffers) {
                            existingEnd.centerX = endX
                            existingEnd.centerY = endY
                            existingEnd.layoutParams.x = expectedEndX
                            existingEnd.layoutParams.y = expectedEndY
                        }
                        existingEnd.layoutParams.flags = expectedFlags
                        runCatching {
                            windowManager.updateViewLayout(existingEnd.composeView, existingEnd.layoutParams)
                        }
                    }
                }
            }
        }

        return addedAnyWindow
    }

    private fun updateTargetPinsTouchableFlag(isTouchable: Boolean) {
        val updatedFlags = buildTargetPinFlags(isTouchable = isTouchable)
        targetPinWindows.values.forEach { window ->
            if (window.layoutParams.flags != updatedFlags) {
                window.layoutParams.flags = updatedFlags
                runCatching {
                    windowManager.updateViewLayout(window.composeView, window.layoutParams)
                }
            }
        }
        swipeEndPinWindows.values.forEach { window ->
            if (window.layoutParams.flags != updatedFlags) {
                window.layoutParams.flags = updatedFlags
                runCatching {
                    windowManager.updateViewLayout(window.composeView, window.layoutParams)
                }
            }
        }
    }

    private fun syncAddTargetDialogWindow(showDialog: Boolean) {
        if (!showDialog || !Settings.canDrawOverlays(this)) {
            addTargetDialogComposeView?.let { view ->
                runCatching { windowManager.removeView(view) }
            }
            addTargetDialogComposeView = null
            return
        }

        if (addTargetDialogComposeView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.55f
            applyCutoutAndFullscreenParams(this)
        }

        val dialogView = ComposeView(this).apply {
            overlayLifecycleOwner.attachToView(this)
            setContent {
                val engineState by stateStore.state.collectAsState()
                KineticAutoClickerTheme(settings = engineState.globalSettings) {
                    AddTargetTypeModalOverlay(
                        onSelectTap = {
                            stateStore.addTarget(ActionType.TAP)
                        },
                        onSelectSwipe = {
                            stateStore.addTarget(ActionType.SWIPE)
                        },
                        onDismiss = {
                            stateStore.dismissAddTargetDialog()
                        }
                    )
                }
            }
        }

        runCatching {
            windowManager.addView(dialogView, params)
            addTargetDialogComposeView = dialogView
        }
    }

    private fun syncSettingsDialogWindow(showDialog: Boolean) {
        if (!showDialog || !Settings.canDrawOverlays(this)) {
            settingsDialogComposeView?.let { view ->
                runCatching { windowManager.removeView(view) }
            }
            settingsDialogComposeView = null
            return
        }

        if (settingsDialogComposeView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.55f
            applyCutoutAndFullscreenParams(this)
        }

        val dialogView = ComposeView(this).apply {
            overlayLifecycleOwner.attachToView(this)
            setContent {
                val engineState by stateStore.state.collectAsState()
                KineticAutoClickerTheme(settings = engineState.globalSettings) {
                    val editingTarget = engineState.editingTarget
                    val intervalValue = editingTarget?.intervalValue ?: engineState.globalSettings.intervalValue
                    val timeUnit = editingTarget?.timeUnit ?: engineState.globalSettings.timeUnit
                    val stopCond = editingTarget?.stopCondition ?: engineState.globalSettings.stopCondition

                    ClickSettingsModalOverlay(
                        initialIntervalValue = intervalValue,
                        initialTimeUnit = timeUnit,
                        initialStopCondition = stopCond,
                        initialActionType = editingTarget?.actionType ?: ActionType.TAP,
                        initialSwipeDurationMs = editingTarget?.swipeDurationMs ?: engineState.globalSettings.swipeDurationMs,
                        initialHoldBeforeSwipeMs = editingTarget?.holdBeforeSwipeMs ?: 80L,
                        targetSubtitle = editingTarget?.let { "#${it.orderIndex} • ${it.label}" },
                        onDeleteTarget = editingTarget?.let { target ->
                            {
                                stateStore.removeTargetById(target.id)
                            }
                        },
                        onDismiss = {
                            stateStore.dismissClickSettingsDialog()
                        },
                        onSaveAndApply = { newInterval, newUnit, newStop, newActionType, newSwipeDuration, newHoldBeforeSwipe ->
                            stateStore.saveClickSettings(
                                intervalValue = newInterval,
                                timeUnit = newUnit,
                                stopCondition = newStop,
                                actionType = newActionType,
                                swipeDurationMs = newSwipeDuration,
                                holdBeforeSwipeMs = newHoldBeforeSwipe
                            )
                            val isGlobal = engineState.editingTargetId == null
                            if (isGlobal) {
                                serviceScope.launch {
                                    settingsRepository.updateIntervalAndStop(newInterval, newUnit, newStop)
                                }
                            }
                        }
                    )
                }
            }
        }

        runCatching {
            windowManager.addView(dialogView, params)
            settingsDialogComposeView = dialogView
        }
    }

    private fun removeAllOverlayWindows() {
        hudComposeView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        hudComposeView = null
        hudLayoutParams = null

        swipeLinesComposeView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        swipeLinesComposeView = null

        addTargetDialogComposeView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        addTargetDialogComposeView = null

        settingsDialogComposeView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        settingsDialogComposeView = null

        targetPinWindows.values.forEach { pinWindow ->
            runCatching { windowManager.removeView(pinWindow.composeView) }
        }
        targetPinWindows.clear()

        swipeEndPinWindows.values.forEach { endWindow ->
            runCatching { windowManager.removeView(endWindow.composeView) }
        }
        swipeEndPinWindows.clear()
    }

    private fun startForegroundWithNotification() {
        val channelId = "kinetic_floating_hud_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.hud_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.hud_notification_text)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopOverlayIntent = Intent(this, FloatingHudService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopOverlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.hud_notification_title))
            .setContentText(getString(R.string.hud_notification_text))
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(0, "Stop", pendingStop)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 4101
        const val ACTION_START_OVERLAY = "com.kinetic.autoclicker.action.START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.kinetic.autoclicker.action.STOP_OVERLAY"

        fun startService(context: Context) {
            val intent = Intent(context, FloatingHudService::class.java).apply {
                action = ACTION_START_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingHudService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            }
            context.startService(intent)
        }
    }
}
