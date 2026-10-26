package com.kinetic.autoclicker.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class StopConditionType {
    INFINITE,
    TIMER_LIMIT,
    CYCLE_COUNT
}

@Serializable
data class StopCondition(
    val type: StopConditionType = StopConditionType.INFINITE,
    val timerMinutes: Int = 5,
    val cycleCount: Int = 100
) {
    fun summaryLabel(isTurkish: Boolean = false): String = when (type) {
        StopConditionType.INFINITE -> if (isTurkish) "Süresiz" else "Infinity"
        StopConditionType.TIMER_LIMIT -> if (isTurkish) "$timerMinutes dk" else "$timerMinutes min"
        StopConditionType.CYCLE_COUNT -> if (isTurkish) "$cycleCount tıklama" else "$cycleCount clicks"
    }
}
