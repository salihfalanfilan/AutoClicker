package com.kinetic.autoclicker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.domain.model.EngineSettings
import com.kinetic.autoclicker.domain.model.TargetPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Asenkron AccessibilityService.dispatchGesture() çağrılarını suspendCancellableCoroutine ve Mutex
 * ile senkronize eden, iptal edilebilir, Jitter (insanlaştırma) ve Swipe destekli jest yürütücüsü.
 */
@Singleton
class GestureExecutor @Inject constructor() {

    private val dispatchMutex = Mutex()

    data class DispatchedGestureOutcome(
        val completed: Boolean,
        val dispatchedX: Float,
        val dispatchedY: Float,
        val durationMs: Long
    )

    /**
     * Verilen hedef noktasını (TAP, LONG_PRESS veya SWIPE) insanlaştırılmış Jitter sapmalarıyla
     * oluşturur ve önceki jest tamamlanmadan (onCompleted / onCancelled) yeni jeste geçmez.
     */
    suspend fun executeTargetGesture(
        service: AccessibilityService,
        target: TargetPoint,
        settings: EngineSettings,
        screenWidthPx: Int,
        screenHeightPx: Int
    ): DispatchedGestureOutcome = dispatchMutex.withLock {
        val maxW = screenWidthPx.coerceAtLeast(1).toFloat()
        val maxH = screenHeightPx.coerceAtLeast(1).toFloat()

        val startPoint = computeJitteredCoordinates(
            baseX = target.x,
            baseY = target.y,
            jitterEnabled = settings.smartJitterEnabled,
            jitterRadiusPx = settings.jitterRadiusPx,
            maxWidth = maxW,
            maxHeight = maxH
        )

        val endPoint = if (target.actionType == ActionType.SWIPE) {
            val safeEndX = target.endX ?: target.x
            val safeEndY = target.endY ?: (target.y - 350f).coerceAtLeast(100f)
            computeJitteredCoordinates(
                baseX = safeEndX,
                baseY = safeEndY,
                jitterEnabled = settings.smartJitterEnabled,
                jitterRadiusPx = settings.jitterRadiusPx,
                maxWidth = maxW,
                maxHeight = maxH
            )
        } else {
            null
        }

        val rawDurationMs = when (target.actionType) {
            ActionType.TAP -> target.holdDurationMs
            ActionType.LONG_PRESS -> target.holdDurationMs.coerceAtLeast(550L)
            ActionType.SWIPE -> target.swipeDurationMs.coerceIn(50L, 5000L)
        }

        val humanizedDurationMs = computeHumanizedDurationMs(
            rawDurationMs = rawDurationMs,
            jitterEnabled = settings.smartJitterEnabled
        )

        val completed = dispatchStrokeSuspend(
            service = service,
            actionType = target.actionType,
            startX = startPoint.x,
            startY = startPoint.y,
            endX = endPoint?.x,
            endY = endPoint?.y,
            durationMs = humanizedDurationMs,
            holdBeforeSwipeMs = target.holdBeforeSwipeMs
        )

        DispatchedGestureOutcome(
            completed = completed,
            dispatchedX = startPoint.x,
            dispatchedY = startPoint.y,
            durationMs = humanizedDurationMs
        )
    }

    private suspend fun dispatchStrokeSuspend(
        service: AccessibilityService,
        actionType: ActionType,
        startX: Float,
        startY: Float,
        endX: Float?,
        endY: Float?,
        durationMs: Long,
        holdBeforeSwipeMs: Long = 80L
    ): Boolean {
        val safeStartX = startX.coerceAtLeast(0f)
        val safeStartY = startY.coerceAtLeast(0f)

        if (actionType == ActionType.SWIPE) {
            val safeSwipeDurationMs = durationMs.coerceIn(50L, 5000L)
            val safeHoldMs = holdBeforeSwipeMs.coerceIn(0L, 3000L)

            var targetEndX = endX ?: (safeStartX + 180f)
            var targetEndY = endY ?: (safeStartY - 350f).coerceAtLeast(0f)
            val dist = kotlin.math.hypot(targetEndX - safeStartX, targetEndY - safeStartY)
            if (dist < 30f) {
                targetEndY = (safeStartY - 350f).coerceAtLeast(0f)
            }

            // Android dokunma motorunun hareketi kesin olarak kaydırma (scroll/fling)
            // olarak tanıması için ara ivme noktaları
            val p1X = safeStartX + (targetEndX - safeStartX) * 0.15f
            val p1Y = safeStartY + (targetEndY - safeStartY) * 0.15f
            val p2X = safeStartX + (targetEndX - safeStartX) * 0.55f
            val p2Y = safeStartY + (targetEndY - safeStartY) * 0.55f
            val p3X = safeStartX + (targetEndX - safeStartX) * 0.85f
            val p3Y = safeStartY + (targetEndY - safeStartY) * 0.85f

            val dragPath = Path().apply {
                moveTo(safeStartX, safeStartY)
                lineTo(p1X, p1Y)
                lineTo(p2X, p2Y)
                lineTo(p3X, p3Y)
                lineTo(targetEndX.coerceAtLeast(0f), targetEndY.coerceAtLeast(0f))
            }

            // KULLANICI İSTEĞİ: "kaydırmada basılı tutup kaydırsın"
            // safeHoldMs >= 10ms ise önce başlangıç noktasında parmak basılı tutulur (willContinue = true),
            // ardından dragStroke continueStroke ile bitişe sürüklenip bırakılır.
            if (safeHoldMs >= 10L) {
                val holdPath = Path().apply {
                    moveTo(safeStartX, safeStartY)
                    lineTo(safeStartX, safeStartY)
                }
                val holdSuccess = runCatching {
                    val holdStroke = GestureDescription.StrokeDescription(
                        holdPath,
                        0L,
                        safeHoldMs,
                        true // Parmak ekranda basılı kalacak
                    )
                    val holdGesture = GestureDescription.Builder().addStroke(holdStroke).build()
                    val ok = dispatchGestureDirect(service, holdGesture)
                    if (!ok) return@runCatching false

                    val dragStroke = holdStroke.continueStroke(
                        dragPath,
                        0L,
                        safeSwipeDurationMs,
                        false // Kaydırma bitince parmak kaldırılacak
                    )
                    val dragGesture = GestureDescription.Builder().addStroke(dragStroke).build()
                    dispatchGestureDirect(service, dragGesture)
                }.getOrDefault(false)

                if (holdSuccess) return true
                // Herhangi bir OEM kısıtı durumunda doğrudan tek adımlı kaydırmaya geri çekil
            }

            val singleStroke = GestureDescription.StrokeDescription(dragPath, 0L, safeSwipeDurationMs)
            val singleGesture = GestureDescription.Builder().addStroke(singleStroke).build()
            return dispatchGestureDirect(service, singleGesture)
        } else {
            // TAP veya LONG_PRESS
            val safeDurationMs = normalizeStrokeDurationMs(durationMs)
            val path = Path().apply {
                moveTo(safeStartX, safeStartY)
                lineTo(safeStartX, safeStartY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, safeDurationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            return dispatchGestureDirect(service, gesture)
        }
    }

    private suspend fun dispatchGestureDirect(
        service: AccessibilityService,
        gestureDescription: GestureDescription
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (continuation.isActive) {
                    continuation.resume(true)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }

        val dispatched = runCatching {
            service.dispatchGesture(gestureDescription, callback, null)
        }.getOrDefault(false)

        if (!dispatched && continuation.isActive) {
            continuation.resume(false)
        }
    }

    /**
     * Kutupsal (polar) rastgele dağılım kullanarak hedef koordinat etrafında dairesel mikro-sapma üretir.
     */
    fun computeJitteredCoordinates(
        baseX: Float,
        baseY: Float,
        jitterEnabled: Boolean,
        jitterRadiusPx: Int,
        maxWidth: Float,
        maxHeight: Float
    ): PointF {
        if (!jitterEnabled || jitterRadiusPx <= 0) {
            return PointF(
                baseX.coerceIn(1f, maxWidth.coerceAtLeast(1f)),
                baseY.coerceIn(1f, maxHeight.coerceAtLeast(1f))
            )
        }

        val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
        val distance = Random.nextDouble(0.0, jitterRadiusPx.toDouble())
        val offsetX = (cos(angle) * distance).toFloat()
        val offsetY = (sin(angle) * distance).toFloat()

        return PointF(
            (baseX + offsetX).coerceIn(1f, maxWidth.coerceAtLeast(1f)),
            (baseY + offsetY).coerceIn(1f, maxHeight.coerceAtLeast(1f))
        )
    }

    /**
     * Basılı tutma / kaydırma süresine küçük insanlaştırma varyasyonu ekler ve >= 10ms olarak normalize eder.
     */
    fun computeHumanizedDurationMs(rawDurationMs: Long, jitterEnabled: Boolean): Long {
        val baseSafe = normalizeStrokeDurationMs(rawDurationMs)
        if (!jitterEnabled) return baseSafe
        val delta = Random.nextLong(-6L, 7L)
        return normalizeStrokeDurationMs(baseSafe + delta)
    }

    companion object {
        const val MIN_SAFE_STROKE_DURATION_MS = 10L
        const val MAX_SAFE_STROKE_DURATION_MS = 60_000L

        /**
         * Android StrokeDescription yapısında duration <= 0 IllegalArgumentException fırlattığından
         * süreyi her zaman coerceAtLeast(10L) ile güvenli aralığa çeker.
         */
        fun normalizeStrokeDurationMs(durationMs: Long): Long {
            return durationMs.coerceAtLeast(MIN_SAFE_STROKE_DURATION_MS)
                .coerceAtMost(MAX_SAFE_STROKE_DURATION_MS)
        }
    }
}
