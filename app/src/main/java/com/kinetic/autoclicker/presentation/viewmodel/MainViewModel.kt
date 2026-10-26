package com.kinetic.autoclicker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetic.autoclicker.domain.engine.AutomationStateStore
import com.kinetic.autoclicker.domain.model.AutomationEngineEvent
import com.kinetic.autoclicker.domain.model.AutomationMode
import com.kinetic.autoclicker.domain.model.ScriptProfile
import com.kinetic.autoclicker.domain.repository.EngineSettingsRepository
import com.kinetic.autoclicker.domain.repository.ScriptProfileRepository
import com.kinetic.autoclicker.presentation.contract.MainUiEffect
import com.kinetic.autoclicker.presentation.contract.MainUiIntent
import com.kinetic.autoclicker.presentation.contract.MainUiState
import com.kinetic.autoclicker.presentation.contract.NavigationTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val stateStore: AutomationStateStore,
    private val settingsRepository: EngineSettingsRepository,
    private val profileRepository: ScriptProfileRepository
) : ViewModel() {

    private val selectedTabFlow = MutableStateFlow(NavigationTab.AUTOMATE)

    private val _effects = MutableSharedFlow<MainUiEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<MainUiEffect> = _effects.asSharedFlow()

    val uiState: StateFlow<MainUiState> = combine(
        selectedTabFlow,
        stateStore.state,
        profileRepository.observeAllProfiles()
    ) { tab, engineState, profiles ->
        MainUiState(
            selectedTab = tab,
            engineState = engineState,
            profiles = profiles,
            isLoadingProfiles = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            profileRepository.ensureDefaultProfilesSeeded()
        }
        viewModelScope.launch {
            settingsRepository.engineSettingsFlow.collect { savedSettings ->
                stateStore.applyGlobalSettings(savedSettings)
            }
        }
        viewModelScope.launch {
            stateStore.events.collect { event ->
                when (event) {
                    is AutomationEngineEvent.StatusToast -> {
                        _effects.tryEmit(MainUiEffect.ShowMessage(event.message))
                    }
                    is AutomationEngineEvent.RequestAccessibilitySettings -> {
                        _effects.tryEmit(MainUiEffect.OpenAccessibilitySettings)
                    }
                    is AutomationEngineEvent.RequestOverlayPermission -> {
                        _effects.tryEmit(MainUiEffect.OpenOverlaySettings)
                    }
                    is AutomationEngineEvent.PulseTarget -> Unit
                }
            }
        }
    }

    fun onIntent(intent: MainUiIntent) {
        when (intent) {
            is MainUiIntent.SelectTab -> {
                if (intent.tab == NavigationTab.TARGETS && stateStore.state.value.isOverlayVisible) {
                    stateStore.disableOverlayAndStop()
                    _effects.tryEmit(MainUiEffect.StopFloatingHudService)
                }
                selectedTabFlow.value = intent.tab
            }

            is MainUiIntent.RefreshPermissions -> {
                stateStore.updatePermissions(
                    isAccessibilityConnected = intent.isAccessibilityEnabled,
                    isOverlayGranted = intent.isOverlayGranted
                )
            }

            MainUiIntent.RequestAccessibilityPermission -> {
                _effects.tryEmit(MainUiEffect.OpenAccessibilitySettings)
            }

            MainUiIntent.RequestOverlayPermission -> {
                _effects.tryEmit(MainUiEffect.OpenOverlaySettings)
            }

            MainUiIntent.ToggleOverlayController -> {
                val current = stateStore.state.value
                if (current.isOverlayVisible) {
                    stateStore.disableOverlayAndStop()
                    _effects.tryEmit(MainUiEffect.StopFloatingHudService)
                } else {
                    if (!current.isOverlayPermissionGranted) {
                        _effects.tryEmit(MainUiEffect.OpenOverlaySettings)
                        return
                    }
                    stateStore.openStartChooserDialog()
                }
            }

            MainUiIntent.OpenStartChooser -> {
                val current = stateStore.state.value
                if (!current.isOverlayPermissionGranted) {
                    _effects.tryEmit(MainUiEffect.OpenOverlaySettings)
                    return
                }
                stateStore.openStartChooserDialog()
            }

            MainUiIntent.DismissStartChooser -> {
                stateStore.dismissStartChooserDialog()
            }

            is MainUiIntent.StartOverlayWithTargetType -> {
                val current = stateStore.state.value
                if (!current.isOverlayPermissionGranted) {
                    _effects.tryEmit(MainUiEffect.OpenOverlaySettings)
                    return
                }
                stateStore.startOverlayWithInitialTarget(intent.actionType)
                _effects.tryEmit(MainUiEffect.StartFloatingHudService)
            }

            MainUiIntent.ToggleSingleTargetMode,
            MainUiIntent.ToggleMultiTargetMode -> {
                val current = stateStore.state.value
                if (current.isOverlayVisible) {
                    stateStore.disableOverlayAndStop()
                    _effects.tryEmit(MainUiEffect.StopFloatingHudService)
                } else {
                    if (!current.isOverlayPermissionGranted) {
                        _effects.tryEmit(MainUiEffect.OpenOverlaySettings)
                        return
                    }
                    stateStore.enableOverlayMode(AutomationMode.MULTI_TARGET)
                    _effects.tryEmit(MainUiEffect.StartFloatingHudService)
                }
            }

            MainUiIntent.ToggleEnginePlayPause -> {
                val current = stateStore.state.value
                val isTr = current.globalSettings.appLanguage.resolveIsTurkish()
                if (!current.isRunning && !current.isAccessibilityServiceConnected) {
                    val msg = if (isTr) {
                        "Tıklamayı başlatmak için lütfen Erişilebilirlik Servisini etkinleştirin"
                    } else {
                        "Please enable Accessibility Service to start clicking"
                    }
                    _effects.tryEmit(MainUiEffect.ShowMessage(msg))
                    _effects.tryEmit(MainUiEffect.OpenAccessibilitySettings)
                    return
                }
                stateStore.toggleRunning()
            }

            is MainUiIntent.SetHudDisplayMode -> {
                stateStore.setHudDisplayMode(intent.mode)
            }

            MainUiIntent.OpenAddTargetChooser -> {
                stateStore.openAddTargetDialog()
            }

            MainUiIntent.DismissAddTargetChooser -> {
                stateStore.dismissAddTargetDialog()
            }

            is MainUiIntent.AddTarget -> {
                stateStore.addTarget(intent.actionType)
            }

            MainUiIntent.RemoveLastTarget -> {
                stateStore.removeLastTarget()
            }

            is MainUiIntent.RemoveTargetById -> {
                stateStore.removeTargetById(intent.targetId)
            }

            is MainUiIntent.UpdateTargetPosition -> {
                stateStore.updateTargetPosition(intent.targetId, intent.x, intent.y)
            }

            is MainUiIntent.UpdateSwipeEndPosition -> {
                stateStore.updateSwipeEndPosition(intent.targetId, intent.endX, intent.endY)
            }

            is MainUiIntent.OpenClickSettings -> {
                stateStore.openClickSettingsDialog(intent.targetId)
            }

            MainUiIntent.DismissClickSettings -> {
                stateStore.dismissClickSettingsDialog()
            }

            is MainUiIntent.SaveClickSettings -> {
                val isGlobal = stateStore.state.value.editingTargetId == null
                stateStore.saveClickSettings(
                    intervalValue = intent.intervalValue,
                    timeUnit = intent.timeUnit,
                    stopCondition = intent.stopCondition,
                    actionType = intent.actionType,
                    swipeDurationMs = intent.swipeDurationMs,
                    holdBeforeSwipeMs = intent.holdBeforeSwipeMs
                )
                if (isGlobal) {
                    viewModelScope.launch {
                        settingsRepository.updateIntervalAndStop(
                            intervalValue = intent.intervalValue,
                            timeUnit = intent.timeUnit,
                            stopCondition = intent.stopCondition
                        )
                    }
                }
            }

            MainUiIntent.ResetTotalClicks -> {
                stateStore.resetTotalClicks()
            }

            MainUiIntent.IncrementExecutionCount -> {
                stateStore.incrementExecutionCount()
            }

            is MainUiIntent.ToggleSmartJitter -> {
                val nextSettings = stateStore.state.value.globalSettings.copy(
                    smartJitterEnabled = intent.enabled
                )
                stateStore.applyGlobalSettings(nextSettings)
                viewModelScope.launch {
                    settingsRepository.toggleSmartJitter(intent.enabled)
                }
            }

            is MainUiIntent.ToggleTouchProtection -> {
                val nextSettings = stateStore.state.value.globalSettings.copy(
                    touchProtectionEnabled = intent.enabled
                )
                stateStore.applyGlobalSettings(nextSettings)
                viewModelScope.launch {
                    settingsRepository.toggleTouchProtection(intent.enabled)
                }
            }

            is MainUiIntent.UpdateJitterRadius -> {
                val nextSettings = stateStore.state.value.globalSettings.copy(
                    jitterRadiusPx = intent.radiusPx.coerceIn(0, 32)
                )
                stateStore.applyGlobalSettings(nextSettings)
                viewModelScope.launch {
                    settingsRepository.updateEngineSettings(nextSettings)
                }
            }

            is MainUiIntent.UpdateHoldAndSwipeDuration -> {
                val nextSettings = stateStore.state.value.globalSettings.copy(
                    holdDurationMs = intent.holdMs.coerceIn(10L, 2_000L),
                    swipeDurationMs = intent.swipeMs.coerceIn(50L, 5_000L)
                )
                stateStore.applyGlobalSettings(nextSettings)
                viewModelScope.launch {
                    settingsRepository.updateEngineSettings(nextSettings)
                }
            }

            is MainUiIntent.UpdateHudPreferences -> {
                val nextSettings = stateStore.state.value.globalSettings.copy(
                    hudOpacity = intent.opacity.coerceIn(0.4f, 1.0f),
                    autoCollapseToEdgePill = intent.autoCollapsePill
                )
                stateStore.applyGlobalSettings(nextSettings)
                viewModelScope.launch {
                    settingsRepository.updateHudPreferences(intent.opacity, intent.autoCollapsePill)
                }
            }

            is MainUiIntent.UpdateAppearance -> {
                val current = stateStore.state.value.globalSettings
                val nextLanguage = intent.appLanguage ?: current.appLanguage
                val nextTheme = intent.themeMode ?: current.themeMode
                val nextPrimary = intent.primaryAccentHex ?: current.primaryAccentHex
                val nextSecondary = intent.secondaryAccentHex ?: current.secondaryAccentHex

                val nextSettings = current.copy(
                    appLanguage = nextLanguage,
                    themeMode = nextTheme,
                    primaryAccentHex = nextPrimary,
                    secondaryAccentHex = nextSecondary
                )
                stateStore.applyGlobalSettings(nextSettings)
                viewModelScope.launch {
                    settingsRepository.updateAppearance(
                        appLanguage = nextLanguage,
                        themeMode = nextTheme,
                        primaryAccentHex = nextPrimary,
                        secondaryAccentHex = nextSecondary
                    )
                }
            }

            is MainUiIntent.SelectScriptProfile -> {
                val currentAppearance = stateStore.state.value.globalSettings
                val mergedProfile = intent.profile.copy(
                    settings = intent.profile.settings.copy(
                        appLanguage = currentAppearance.appLanguage,
                        themeMode = currentAppearance.themeMode,
                        primaryAccentHex = currentAppearance.primaryAccentHex,
                        secondaryAccentHex = currentAppearance.secondaryAccentHex
                    )
                )
                stateStore.selectProfile(mergedProfile)
            }

            is MainUiIntent.SaveCurrentAsProfile -> {
                val current = stateStore.state.value
                val trimmedName = intent.name.trim().ifBlank {
                    "Config #${uiState.value.profiles.size + 1}"
                }
                viewModelScope.launch {
                    val newProfile = ScriptProfile(
                        id = 0L,
                        name = trimmedName,
                        mode = AutomationMode.MULTI_TARGET,
                        targets = current.targets,
                        settings = current.globalSettings
                    )
                    val savedId = profileRepository.saveProfile(newProfile)
                    val saved = profileRepository.getProfileById(savedId)
                    if (saved != null) {
                        stateStore.selectProfile(saved)
                    }
                }
            }

            is MainUiIntent.DeleteScriptProfile -> {
                viewModelScope.launch {
                    profileRepository.deleteProfile(intent.profileId)
                }
            }
        }
    }
}
