package com.kinetic.autoclicker.presentation.contract

import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.domain.model.AppLanguage
import com.kinetic.autoclicker.domain.model.AutomationEngineState
import com.kinetic.autoclicker.domain.model.HudDisplayMode
import com.kinetic.autoclicker.domain.model.ScriptProfile
import com.kinetic.autoclicker.domain.model.StopCondition
import com.kinetic.autoclicker.domain.model.ThemeMode
import com.kinetic.autoclicker.domain.model.TimeUnitOption

enum class NavigationTab(val label: String) {
    AUTOMATE("Automate"),
    TARGETS("Targets"),
    SCRIPTS("Scripts"),
    SETTINGS("Settings")
}

data class MainUiState(
    val selectedTab: NavigationTab = NavigationTab.AUTOMATE,
    val engineState: AutomationEngineState = AutomationEngineState(),
    val profiles: List<ScriptProfile> = emptyList(),
    val isLoadingProfiles: Boolean = true
)

sealed interface MainUiIntent {
    data class SelectTab(val tab: NavigationTab) : MainUiIntent
    data class RefreshPermissions(
        val isAccessibilityEnabled: Boolean,
        val isOverlayGranted: Boolean
    ) : MainUiIntent
    data object RequestAccessibilityPermission : MainUiIntent
    data object RequestOverlayPermission : MainUiIntent
    data object ToggleOverlayController : MainUiIntent
    data object ToggleSingleTargetMode : MainUiIntent
    data object ToggleMultiTargetMode : MainUiIntent
    data object OpenStartChooser : MainUiIntent
    data object DismissStartChooser : MainUiIntent
    data class StartOverlayWithTargetType(val actionType: ActionType) : MainUiIntent
    data object ToggleEnginePlayPause : MainUiIntent
    data class SetHudDisplayMode(val mode: HudDisplayMode) : MainUiIntent
    data object OpenAddTargetChooser : MainUiIntent
    data object DismissAddTargetChooser : MainUiIntent
    data class AddTarget(val actionType: ActionType = ActionType.TAP) : MainUiIntent
    data object RemoveLastTarget : MainUiIntent
    data class RemoveTargetById(val targetId: String) : MainUiIntent
    data class UpdateTargetPosition(val targetId: String, val x: Float, val y: Float) : MainUiIntent
    data class UpdateSwipeEndPosition(val targetId: String, val endX: Float, val endY: Float) : MainUiIntent
    data class OpenClickSettings(val targetId: String? = null) : MainUiIntent
    data object DismissClickSettings : MainUiIntent
    data class SaveClickSettings(
        val intervalValue: Int,
        val timeUnit: TimeUnitOption,
        val stopCondition: StopCondition,
        val actionType: ActionType? = null,
        val swipeDurationMs: Long? = null,
        val holdBeforeSwipeMs: Long? = null
    ) : MainUiIntent
    data object ResetTotalClicks : MainUiIntent
    data object IncrementExecutionCount : MainUiIntent
    data class ToggleSmartJitter(val enabled: Boolean) : MainUiIntent
    data class ToggleTouchProtection(val enabled: Boolean) : MainUiIntent
    data class UpdateJitterRadius(val radiusPx: Int) : MainUiIntent
    data class UpdateHoldAndSwipeDuration(val holdMs: Long, val swipeMs: Long) : MainUiIntent
    data class UpdateHudPreferences(val opacity: Float, val autoCollapsePill: Boolean) : MainUiIntent
    data class UpdateAppearance(
        val appLanguage: AppLanguage? = null,
        val themeMode: ThemeMode? = null,
        val primaryAccentHex: Long? = null,
        val secondaryAccentHex: Long? = null
    ) : MainUiIntent
    data class SelectScriptProfile(val profile: ScriptProfile) : MainUiIntent
    data class SaveCurrentAsProfile(val name: String) : MainUiIntent
    data class DeleteScriptProfile(val profileId: Long) : MainUiIntent
}

sealed interface MainUiEffect {
    data object OpenAccessibilitySettings : MainUiEffect
    data object OpenOverlaySettings : MainUiEffect
    data object StartFloatingHudService : MainUiEffect
    data object StopFloatingHudService : MainUiEffect
    data class ShowMessage(val message: String) : MainUiEffect
}
