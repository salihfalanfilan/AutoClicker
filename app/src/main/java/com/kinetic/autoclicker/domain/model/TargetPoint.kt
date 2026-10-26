package com.kinetic.autoclicker.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TargetPoint(
    val id: String = UUID.randomUUID().toString(),
    val orderIndex: Int = 1,
    val label: String = "Anchor point #1",
    val x: Float = 540f,
    val y: Float = 1120f,
    val normalizedX: Float = (x / DEFAULT_SCREEN_WIDTH).coerceIn(0f, 1f),
    val normalizedY: Float = (y / DEFAULT_SCREEN_HEIGHT).coerceIn(0f, 1f),
    val endX: Float? = null,
    val endY: Float? = null,
    val normalizedEndX: Float? = endX?.let { (it / DEFAULT_SCREEN_WIDTH).coerceIn(0f, 1f) },
    val normalizedEndY: Float? = endY?.let { (it / DEFAULT_SCREEN_HEIGHT).coerceIn(0f, 1f) },
    val actionType: ActionType = ActionType.TAP,
    val intervalValue: Int = 100,
    val timeUnit: TimeUnitOption = TimeUnitOption.MILLISECONDS,
    val holdDurationMs: Long = 45L,
    val swipeDurationMs: Long = 300L,
    val holdBeforeSwipeMs: Long = 80L,
    val stopCondition: StopCondition = StopCondition(),
    val enabled: Boolean = true
) {
    val intervalMs: Long
        get() = timeUnit.toMilliseconds(intervalValue)

    val safeHoldDurationMs: Long
        get() = holdDurationMs.coerceAtLeast(10L)

    fun withAbsoluteCoordinates(
        newX: Float,
        newY: Float,
        screenWidth: Int,
        screenHeight: Int
    ): TargetPoint {
        val safeW = screenWidth.coerceAtLeast(1).toFloat()
        val safeH = screenHeight.coerceAtLeast(1).toFloat()
        val clampedX = newX.coerceIn(0f, safeW)
        val clampedY = newY.coerceIn(0f, safeH)

        val nextEndX = if (actionType == ActionType.SWIPE) (endX ?: clampedX) else endX
        val nextEndY = if (actionType == ActionType.SWIPE) (endY ?: (clampedY - safeH * 0.25f).coerceIn(0f, safeH)) else endY

        return copy(
            x = clampedX,
            y = clampedY,
            normalizedX = (clampedX / safeW).coerceIn(0f, 1f),
            normalizedY = (clampedY / safeH).coerceIn(0f, 1f),
            endX = nextEndX,
            endY = nextEndY,
            normalizedEndX = nextEndX?.let { (it / safeW).coerceIn(0f, 1f) },
            normalizedEndY = nextEndY?.let { (it / safeH).coerceIn(0f, 1f) }
        )
    }

    fun withSwipeEndCoordinates(
        newEndX: Float,
        newEndY: Float,
        screenWidth: Int,
        screenHeight: Int
    ): TargetPoint {
        val safeW = screenWidth.coerceAtLeast(1).toFloat()
        val safeH = screenHeight.coerceAtLeast(1).toFloat()
        val clampedEndX = newEndX.coerceIn(0f, safeW)
        val clampedEndY = newEndY.coerceIn(0f, safeH)

        return copy(
            endX = clampedEndX,
            endY = clampedEndY,
            normalizedEndX = (clampedEndX / safeW).coerceIn(0f, 1f),
            normalizedEndY = (clampedEndY / safeH).coerceIn(0f, 1f)
        )
    }

    fun remapToScreenSize(screenWidth: Int, screenHeight: Int): TargetPoint {
        val safeW = screenWidth.coerceAtLeast(1).toFloat()
        val safeH = screenHeight.coerceAtLeast(1).toFloat()
        val mappedX = (normalizedX.coerceIn(0f, 1f) * safeW).coerceIn(0f, safeW)
        val mappedY = (normalizedY.coerceIn(0f, 1f) * safeH).coerceIn(0f, safeH)
        val mappedEndX = normalizedEndX?.let { (it.coerceIn(0f, 1f) * safeW).coerceIn(0f, safeW) }
            ?: if (actionType == ActionType.SWIPE) mappedX else null
        val mappedEndY = normalizedEndY?.let { (it.coerceIn(0f, 1f) * safeH).coerceIn(0f, safeH) }
            ?: if (actionType == ActionType.SWIPE) (mappedY - safeH * 0.25f).coerceIn(0f, safeH) else null

        return copy(
            x = mappedX,
            y = mappedY,
            endX = mappedEndX,
            endY = mappedEndY,
            normalizedEndX = mappedEndX?.let { (it / safeW).coerceIn(0f, 1f) },
            normalizedEndY = mappedEndY?.let { (it / safeH).coerceIn(0f, 1f) }
        )
    }

    companion object {
        const val DEFAULT_SCREEN_WIDTH = 1080f
        const val DEFAULT_SCREEN_HEIGHT = 2400f
    }
}
