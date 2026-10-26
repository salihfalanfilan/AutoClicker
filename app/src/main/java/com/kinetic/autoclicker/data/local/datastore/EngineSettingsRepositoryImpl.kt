package com.kinetic.autoclicker.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kinetic.autoclicker.domain.model.AppLanguage
import com.kinetic.autoclicker.domain.model.EngineSettings
import com.kinetic.autoclicker.domain.model.StopCondition
import com.kinetic.autoclicker.domain.model.StopConditionType
import com.kinetic.autoclicker.domain.model.ThemeMode
import com.kinetic.autoclicker.domain.model.TimeUnitOption
import com.kinetic.autoclicker.domain.repository.EngineSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngineSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : EngineSettingsRepository {

    private object Keys {
        val INTERVAL_VALUE = intPreferencesKey("interval_value")
        val TIME_UNIT = stringPreferencesKey("time_unit")
        val STOP_CONDITION_TYPE = stringPreferencesKey("stop_condition_type")
        val TIMER_MINUTES = intPreferencesKey("timer_minutes")
        val CYCLE_COUNT = intPreferencesKey("cycle_count")
        val SMART_JITTER_ENABLED = booleanPreferencesKey("smart_jitter_enabled")
        val JITTER_RADIUS_PX = intPreferencesKey("jitter_radius_px")
        val HOLD_DURATION_MS = longPreferencesKey("hold_duration_ms")
        val SWIPE_DURATION_MS = longPreferencesKey("swipe_duration_ms")
        val HUD_OPACITY = floatPreferencesKey("hud_opacity")
        val AUTO_COLLAPSE_PILL = booleanPreferencesKey("auto_collapse_pill")
        val TOUCH_PROTECTION_ENABLED = booleanPreferencesKey("touch_protection_enabled")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PRIMARY_ACCENT_HEX = longPreferencesKey("primary_accent_hex")
        val SECONDARY_ACCENT_HEX = longPreferencesKey("secondary_accent_hex")
    }

    override val engineSettingsFlow: Flow<EngineSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val unitName = prefs[Keys.TIME_UNIT] ?: TimeUnitOption.MILLISECONDS.name
            val timeUnit = runCatching { TimeUnitOption.valueOf(unitName) }
                .getOrDefault(TimeUnitOption.MILLISECONDS)

            val stopTypeName = prefs[Keys.STOP_CONDITION_TYPE] ?: StopConditionType.INFINITE.name
            val stopType = runCatching { StopConditionType.valueOf(stopTypeName) }
                .getOrDefault(StopConditionType.INFINITE)

            val langName = prefs[Keys.APP_LANGUAGE] ?: AppLanguage.SYSTEM.name
            val appLang = runCatching { AppLanguage.valueOf(langName) }
                .getOrDefault(AppLanguage.SYSTEM)

            val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = runCatching { ThemeMode.valueOf(themeName) }
                .getOrDefault(ThemeMode.SYSTEM)

            EngineSettings(
                intervalValue = prefs[Keys.INTERVAL_VALUE] ?: 100,
                timeUnit = timeUnit,
                stopCondition = StopCondition(
                    type = stopType,
                    timerMinutes = prefs[Keys.TIMER_MINUTES] ?: 5,
                    cycleCount = prefs[Keys.CYCLE_COUNT] ?: 100
                ),
                smartJitterEnabled = prefs[Keys.SMART_JITTER_ENABLED] ?: true,
                jitterRadiusPx = prefs[Keys.JITTER_RADIUS_PX] ?: 6,
                holdDurationMs = prefs[Keys.HOLD_DURATION_MS] ?: 45L,
                swipeDurationMs = prefs[Keys.SWIPE_DURATION_MS] ?: 280L,
                hudOpacity = prefs[Keys.HUD_OPACITY] ?: 0.95f,
                autoCollapseToEdgePill = prefs[Keys.AUTO_COLLAPSE_PILL] ?: true,
                touchProtectionEnabled = prefs[Keys.TOUCH_PROTECTION_ENABLED] ?: true,
                appLanguage = appLang,
                themeMode = themeMode,
                primaryAccentHex = prefs[Keys.PRIMARY_ACCENT_HEX] ?: 0xFF299EECL,
                secondaryAccentHex = prefs[Keys.SECONDARY_ACCENT_HEX] ?: 0xFF9449DBL
            )
        }

    override suspend fun updateEngineSettings(settings: EngineSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.INTERVAL_VALUE] = settings.intervalValue
            prefs[Keys.TIME_UNIT] = settings.timeUnit.name
            prefs[Keys.STOP_CONDITION_TYPE] = settings.stopCondition.type.name
            prefs[Keys.TIMER_MINUTES] = settings.stopCondition.timerMinutes
            prefs[Keys.CYCLE_COUNT] = settings.stopCondition.cycleCount
            prefs[Keys.SMART_JITTER_ENABLED] = settings.smartJitterEnabled
            prefs[Keys.JITTER_RADIUS_PX] = settings.jitterRadiusPx
            prefs[Keys.HOLD_DURATION_MS] = settings.holdDurationMs
            prefs[Keys.SWIPE_DURATION_MS] = settings.swipeDurationMs
            prefs[Keys.HUD_OPACITY] = settings.hudOpacity
            prefs[Keys.AUTO_COLLAPSE_PILL] = settings.autoCollapseToEdgePill
            prefs[Keys.TOUCH_PROTECTION_ENABLED] = settings.touchProtectionEnabled
            prefs[Keys.APP_LANGUAGE] = settings.appLanguage.name
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.PRIMARY_ACCENT_HEX] = settings.primaryAccentHex
            prefs[Keys.SECONDARY_ACCENT_HEX] = settings.secondaryAccentHex
        }
    }

    override suspend fun updateIntervalAndStop(
        intervalValue: Int,
        timeUnit: TimeUnitOption,
        stopCondition: StopCondition
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.INTERVAL_VALUE] = intervalValue
            prefs[Keys.TIME_UNIT] = timeUnit.name
            prefs[Keys.STOP_CONDITION_TYPE] = stopCondition.type.name
            prefs[Keys.TIMER_MINUTES] = stopCondition.timerMinutes
            prefs[Keys.CYCLE_COUNT] = stopCondition.cycleCount
        }
    }

    override suspend fun toggleSmartJitter(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SMART_JITTER_ENABLED] = enabled
        }
    }

    override suspend fun toggleTouchProtection(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.TOUCH_PROTECTION_ENABLED] = enabled
        }
    }

    override suspend fun updateHudPreferences(opacity: Float, autoCollapsePill: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.HUD_OPACITY] = opacity.coerceIn(0.4f, 1.0f)
            prefs[Keys.AUTO_COLLAPSE_PILL] = autoCollapsePill
        }
    }

    override suspend fun updateAppearance(
        appLanguage: AppLanguage,
        themeMode: ThemeMode,
        primaryAccentHex: Long,
        secondaryAccentHex: Long
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = appLanguage.name
            prefs[Keys.THEME_MODE] = themeMode.name
            prefs[Keys.PRIMARY_ACCENT_HEX] = primaryAccentHex
            prefs[Keys.SECONDARY_ACCENT_HEX] = secondaryAccentHex
        }
    }
}
