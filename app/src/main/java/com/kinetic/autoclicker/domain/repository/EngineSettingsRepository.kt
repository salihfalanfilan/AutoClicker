package com.kinetic.autoclicker.domain.repository

import com.kinetic.autoclicker.domain.model.AppLanguage
import com.kinetic.autoclicker.domain.model.EngineSettings
import com.kinetic.autoclicker.domain.model.StopCondition
import com.kinetic.autoclicker.domain.model.ThemeMode
import com.kinetic.autoclicker.domain.model.TimeUnitOption
import kotlinx.coroutines.flow.Flow

interface EngineSettingsRepository {
    val engineSettingsFlow: Flow<EngineSettings>
    suspend fun updateEngineSettings(settings: EngineSettings)
    suspend fun updateIntervalAndStop(
        intervalValue: Int,
        timeUnit: TimeUnitOption,
        stopCondition: StopCondition
    )
    suspend fun toggleSmartJitter(enabled: Boolean)
    suspend fun toggleTouchProtection(enabled: Boolean)
    suspend fun updateHudPreferences(opacity: Float, autoCollapsePill: Boolean)
    suspend fun updateAppearance(
        appLanguage: AppLanguage,
        themeMode: ThemeMode,
        primaryAccentHex: Long,
        secondaryAccentHex: Long
    )
}
