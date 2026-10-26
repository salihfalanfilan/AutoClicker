package com.kinetic.autoclicker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ScriptProfile(
    val id: Long = 0L,
    val name: String,
    val mode: AutomationMode = AutomationMode.MULTI_TARGET,
    val targets: List<TargetPoint> = emptyList(),
    val settings: EngineSettings = EngineSettings(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val tapTargetCount: Int
        get() = targets.count { it.actionType != ActionType.SWIPE }

    val swipeGestureCount: Int
        get() = targets.count { it.actionType == ActionType.SWIPE }

    fun summarySubtitle(isTurkish: Boolean = false): String {
        val tapCount = tapTargetCount
        val swipeCount = swipeGestureCount
        return if (isTurkish) {
            "$tapCount hedef · $swipeCount kaydırma hareketi"
        } else {
            val tapPart = "$tapCount target${if (tapCount == 1) "" else "s"}"
            val swipePart = "$swipeCount swipe gesture${if (swipeCount == 1) "" else "s"}"
            "$tapPart · $swipePart"
        }
    }
}
