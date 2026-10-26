package com.kinetic.autoclicker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EngineSettings(
    val intervalValue: Int = 100,
    val timeUnit: TimeUnitOption = TimeUnitOption.MILLISECONDS,
    val stopCondition: StopCondition = StopCondition(
        type = StopConditionType.INFINITE,
        timerMinutes = 5,
        cycleCount = 100
    ),
    val smartJitterEnabled: Boolean = true,
    val jitterRadiusPx: Int = 6,
    val holdDurationMs: Long = 45L,
    val swipeDurationMs: Long = 280L,
    val hudOpacity: Float = 0.95f,
    val autoCollapseToEdgePill: Boolean = true,
    val touchProtectionEnabled: Boolean = true,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val primaryAccentHex: Long = 0xFF299EECL,
    val secondaryAccentHex: Long = 0xFF9449DBL
) {
    val intervalMs: Long
        get() = timeUnit.toMilliseconds(intervalValue)
}
