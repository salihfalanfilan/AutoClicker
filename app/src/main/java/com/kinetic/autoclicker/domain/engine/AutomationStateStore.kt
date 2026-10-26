package com.kinetic.autoclicker.domain.engine

import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.domain.model.AutomationEngineEvent
import com.kinetic.autoclicker.domain.model.AutomationEngineState
import com.kinetic.autoclicker.domain.model.AutomationMode
import com.kinetic.autoclicker.domain.model.EngineSettings
import com.kinetic.autoclicker.domain.model.HudDisplayMode
import com.kinetic.autoclicker.domain.model.ScriptProfile
import com.kinetic.autoclicker.domain.model.StopCondition
import com.kinetic.autoclicker.domain.model.TargetPoint
import com.kinetic.autoclicker.domain.model.TimeUnitOption
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Shared in-process (:main) state container and event bus.
 * Synchronizes MainActivity, FloatingHudService, and ClickAccessibilityService
 * via reactive StateFlow and SharedFlow without IPC overhead.
 */
@Singleton
class AutomationStateStore @Inject constructor() {

    private val _state = MutableStateFlow(AutomationEngineState())
    val state: StateFlow<AutomationEngineState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AutomationEngineEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<AutomationEngineEvent> = _events.asSharedFlow()

    fun emitEvent(event: AutomationEngineEvent) {
        _events.tryEmit(event)
    }

    fun updatePermissions(
        isAccessibilityConnected: Boolean? = null,
        isOverlayGranted: Boolean? = null
    ) {
        _state.update { current ->
            current.copy(
                isAccessibilityServiceConnected = isAccessibilityConnected
                    ?: current.isAccessibilityServiceConnected,
                isOverlayPermissionGranted = isOverlayGranted
                    ?: current.isOverlayPermissionGranted
            )
        }
    }

    fun setUserInteractingWithHud(interacting: Boolean) {
        _state.update { current ->
            if (current.isUserInteractingWithHud == interacting) current
            else current.copy(isUserInteractingWithHud = interacting)
        }
    }

    /**
     * Recalculates absolute target coordinates and HUD position from normalized (0.0f - 1.0f)
     * ratios whenever screen dimensions or orientation change.
     */
    fun onScreenConfigurationChanged(newWidthPx: Int, newHeightPx: Int) {
        val safeW = newWidthPx.coerceAtLeast(1)
        val safeH = newHeightPx.coerceAtLeast(1)

        _state.update { current ->
            if (current.screenWidthPx == safeW && current.screenHeightPx == safeH) {
                return@update current
            }

            val remappedTargets = current.targets.map { point ->
                point.remapToScreenSize(safeW, safeH)
            }

            val hudRatioX = current.hudPositionX.toFloat() / current.screenWidthPx.coerceAtLeast(1).toFloat()
            val hudRatioY = current.hudPositionY.toFloat() / current.screenHeightPx.coerceAtLeast(1).toFloat()
            val nextHudX = if (current.isDockedRightEdge) {
                (safeW - 180).coerceAtLeast(8)
            } else {
                (hudRatioX * safeW).roundToInt().coerceIn(8, (safeW - 120).coerceAtLeast(8))
            }
            val nextHudY = (hudRatioY * safeH).roundToInt().coerceIn(48, (safeH - 180).coerceAtLeast(48))

            current.copy(
                screenWidthPx = safeW,
                screenHeightPx = safeH,
                targets = remappedTargets,
                hudPositionX = nextHudX,
                hudPositionY = nextHudY
            )
        }
    }

    fun applyGlobalSettings(settings: EngineSettings) {
        val safeSettings = settings.copy(
            holdDurationMs = settings.holdDurationMs.coerceAtLeast(10L),
            swipeDurationMs = settings.swipeDurationMs.coerceAtLeast(10L)
        )
        _state.update { current ->
            current.copy(
                globalSettings = safeSettings
            )
        }
    }

    /**
     * Enables the floating overlay while preserving the user's current targets and positions!
     * Only loads profile targets if explicitly passed AND current targets are empty.
     */
    fun enableOverlayMode(
        mode: AutomationMode = AutomationMode.MULTI_TARGET,
        profile: ScriptProfile? = null
    ) {
        _state.update { current ->
            val screenW = current.screenWidthPx.coerceAtLeast(1)
            val screenH = current.screenHeightPx.coerceAtLeast(1)
            val preservedTargets = when {
                current.targets.isNotEmpty() -> {
                    current.targets.mapIndexed { idx, point ->
                        point.remapToScreenSize(screenW, screenH).copy(orderIndex = idx + 1)
                    }
                }
                profile != null && profile.targets.isNotEmpty() -> {
                    profile.targets.mapIndexed { idx, point ->
                        point.remapToScreenSize(screenW, screenH).copy(orderIndex = idx + 1)
                    }
                }
                else -> {
                    listOf(
                        TargetPoint(
                            id = UUID.randomUUID().toString(),
                            orderIndex = 1,
                            label = "Anchor point #1",
                            x = screenW * 0.5f,
                            y = screenH * 0.46f,
                            normalizedX = 0.5f,
                            normalizedY = 0.46f,
                            intervalValue = current.globalSettings.intervalValue,
                            timeUnit = current.globalSettings.timeUnit,
                            stopCondition = current.globalSettings.stopCondition
                        )
                    )
                }
            }
            current.copy(
                activeMode = mode,
                targets = preservedTargets,
                isOverlayVisible = true,
                hudDisplayMode = HudDisplayMode.EXPANDED_BAR,
                isRunning = false,
                isStartChooserDialogVisible = false,
                isAddTargetDialogVisible = false
            )
        }
    }

    fun openStartChooserDialog() {
        _state.update { current ->
            current.copy(
                isStartChooserDialogVisible = true,
                isAddTargetDialogVisible = false,
                isClickSettingsDialogVisible = false
            )
        }
    }

    fun dismissStartChooserDialog() {
        _state.update { current ->
            current.copy(isStartChooserDialogVisible = false)
        }
    }

    fun startOverlayWithInitialTarget(actionType: ActionType) {
        _state.update { current ->
            val screenW = current.screenWidthPx.coerceAtLeast(1)
            val screenH = current.screenHeightPx.coerceAtLeast(1)
            val initialTarget = if (actionType == ActionType.SWIPE) {
                val startX = screenW * 0.5f
                val startY = screenH * 0.65f
                val endX = screenW * 0.5f
                val endY = screenH * 0.35f
                TargetPoint(
                    id = UUID.randomUUID().toString(),
                    orderIndex = 1,
                    label = "Swipe #1",
                    x = startX,
                    y = startY,
                    normalizedX = 0.5f,
                    normalizedY = 0.65f,
                    endX = endX,
                    endY = endY,
                    normalizedEndX = 0.5f,
                    normalizedEndY = 0.35f,
                    actionType = ActionType.SWIPE,
                    intervalValue = current.globalSettings.intervalValue,
                    timeUnit = current.globalSettings.timeUnit,
                    holdDurationMs = current.globalSettings.swipeDurationMs.coerceAtLeast(300L),
                    stopCondition = current.globalSettings.stopCondition
                )
            } else {
                TargetPoint(
                    id = UUID.randomUUID().toString(),
                    orderIndex = 1,
                    label = "Anchor point #1",
                    x = screenW * 0.5f,
                    y = screenH * 0.46f,
                    normalizedX = 0.5f,
                    normalizedY = 0.46f,
                    actionType = ActionType.TAP,
                    intervalValue = current.globalSettings.intervalValue,
                    timeUnit = current.globalSettings.timeUnit,
                    holdDurationMs = current.globalSettings.holdDurationMs.coerceAtLeast(10L),
                    stopCondition = current.globalSettings.stopCondition
                )
            }

            current.copy(
                activeMode = AutomationMode.MULTI_TARGET,
                targets = listOf(initialTarget),
                isOverlayVisible = true,
                hudDisplayMode = HudDisplayMode.EXPANDED_BAR,
                isRunning = false,
                isStartChooserDialogVisible = false,
                isAddTargetDialogVisible = false,
                isClickSettingsDialogVisible = false
            )
        }
    }

    fun disableOverlayAndStop() {
        _state.update { current ->
            current.copy(
                isRunning = false,
                isOverlayVisible = false,
                hudDisplayMode = HudDisplayMode.HIDDEN,
                isClickSettingsDialogVisible = false,
                isAddTargetDialogVisible = false,
                isStartChooserDialogVisible = false,
                isUserInteractingWithHud = false,
                editingTargetId = null,
                elapsedRunTimeMs = 0L
            )
        }
    }

    /**
     * Emergency Kill-Switch triggered by hardware volume keys or ACTION_SCREEN_OFF.
     */
    fun emergencyStop(reason: String) {
        val wasRunning = _state.value.isRunning
        _state.update { current ->
            current.copy(
                isRunning = false,
                activeTargetIndex = 0
            )
        }
        if (wasRunning) {
            _events.tryEmit(AutomationEngineEvent.StatusToast(reason))
        }
    }

    fun setRunning(running: Boolean) {
        _state.update { current ->
            val nextHudMode = if (running && current.globalSettings.autoCollapseToEdgePill && current.isOverlayVisible) {
                HudDisplayMode.EDGE_PILL
            } else {
                current.hudDisplayMode
            }
            current.copy(
                isRunning = running,
                hudDisplayMode = nextHudMode,
                isAddTargetDialogVisible = false,
                isClickSettingsDialogVisible = if (running) false else current.isClickSettingsDialogVisible,
                totalClicksExecuted = if (running) 0L else current.totalClicksExecuted,
                elapsedRunTimeMs = if (running) 0L else current.elapsedRunTimeMs,
                activeTargetIndex = 0
            )
        }
    }

    fun toggleRunning() {
        val next = !_state.value.isRunning
        setRunning(next)
    }

    fun setHudDisplayMode(hudMode: HudDisplayMode) {
        _state.update { current ->
            current.copy(
                hudDisplayMode = hudMode,
                isOverlayVisible = hudMode != HudDisplayMode.HIDDEN
            )
        }
    }

    fun updateHudPosition(x: Int, y: Int, isRightEdge: Boolean = _state.value.isDockedRightEdge) {
        _state.update { current ->
            current.copy(
                hudPositionX = x,
                hudPositionY = y,
                isDockedRightEdge = isRightEdge
            )
        }
    }

    fun openAddTargetDialog() {
        _state.update { current ->
            current.copy(
                isRunning = false,
                isAddTargetDialogVisible = true
            )
        }
    }

    fun dismissAddTargetDialog() {
        _state.update { current ->
            current.copy(isAddTargetDialogVisible = false)
        }
    }

    fun addTarget(actionType: ActionType = ActionType.TAP) {
        _state.update { current ->
            val nextOrder = current.targets.size + 1
            val screenW = current.screenWidthPx.coerceAtLeast(1)
            val screenH = current.screenHeightPx.coerceAtLeast(1)
            val offsetRatio = ((nextOrder - 1) % 6) * 0.05f
            val normX = (0.38f + offsetRatio).coerceIn(0.15f, 0.80f)
            val normY = (0.38f + offsetRatio).coerceIn(0.15f, 0.80f)
            val baseX = normX * screenW
            val baseY = normY * screenH
            val endNormX = if (actionType == ActionType.SWIPE) (normX + 0.22f).coerceIn(0.15f, 0.90f) else null
            val endNormY = if (actionType == ActionType.SWIPE) (normY - 0.12f).coerceIn(0.12f, 0.88f) else null

            val newTarget = TargetPoint(
                id = UUID.randomUUID().toString(),
                orderIndex = nextOrder,
                label = if (actionType == ActionType.SWIPE) "Swipe #$nextOrder" else "Anchor point #$nextOrder",
                x = baseX,
                y = baseY,
                normalizedX = normX,
                normalizedY = normY,
                endX = endNormX?.let { it * screenW },
                endY = endNormY?.let { it * screenH },
                normalizedEndX = endNormX,
                normalizedEndY = endNormY,
                actionType = actionType,
                intervalValue = current.globalSettings.intervalValue,
                timeUnit = current.globalSettings.timeUnit,
                holdDurationMs = current.globalSettings.holdDurationMs.coerceAtLeast(10L),
                stopCondition = current.globalSettings.stopCondition
            )
            current.copy(
                activeMode = AutomationMode.MULTI_TARGET,
                targets = current.targets + newTarget,
                isAddTargetDialogVisible = false
            )
        }
    }

    fun removeLastTarget() {
        _state.update { current ->
            if (current.targets.isEmpty()) return@update current
            val trimmed = current.targets.dropLast(1).mapIndexed { index, point ->
                val newOrder = index + 1
                point.copy(
                    orderIndex = newOrder,
                    label = if (point.actionType == ActionType.SWIPE) "Swipe #$newOrder" else "Anchor point #$newOrder"
                )
            }
            current.copy(targets = trimmed)
        }
    }

    fun removeTargetById(targetId: String) {
        _state.update { current ->
            val filtered = current.targets.filterNot { it.id == targetId }
                .mapIndexed { index, point ->
                    val newOrder = index + 1
                    point.copy(
                        orderIndex = newOrder,
                        label = if (point.actionType == ActionType.SWIPE) "Swipe #$newOrder" else "Anchor point #$newOrder"
                    )
                }
            current.copy(
                targets = filtered,
                editingTargetId = null,
                isClickSettingsDialogVisible = false
            )
        }
    }

    fun resetTotalClicks() {
        _state.update { current ->
            current.copy(totalClicksExecuted = 0L, elapsedRunTimeMs = 0L)
        }
    }

    fun incrementExecutionCount() {
        _state.update { current ->
            current.copy(totalClicksExecuted = current.totalClicksExecuted + 1L)
        }
    }

    fun updateTargetPosition(targetId: String, newX: Float, newY: Float) {
        _state.update { current ->
            if (current.isRunning) return@update current
            val screenW = current.screenWidthPx.coerceAtLeast(1)
            val screenH = current.screenHeightPx.coerceAtLeast(1)
            val updated = current.targets.map { target ->
                if (target.id == targetId) {
                    target.withAbsoluteCoordinates(
                        newX = newX,
                        newY = newY,
                        screenWidth = screenW,
                        screenHeight = screenH
                    )
                } else {
                    target
                }
            }
            current.copy(
                targets = updated,
                showDragHintTooltip = false
            )
        }
    }

    fun updateSwipeEndPosition(targetId: String, endX: Float, endY: Float) {
        _state.update { current ->
            if (current.isRunning) return@update current
            val screenW = current.screenWidthPx.coerceAtLeast(1)
            val screenH = current.screenHeightPx.coerceAtLeast(1)
            val updated = current.targets.map { target ->
                if (target.id == targetId) {
                    target.withSwipeEndCoordinates(
                        newEndX = endX,
                        newEndY = endY,
                        screenWidth = screenW,
                        screenHeight = screenH
                    )
                } else {
                    target
                }
            }
            current.copy(targets = updated)
        }
    }

    fun openClickSettingsDialog(targetId: String? = null) {
        _state.update { current ->
            current.copy(
                isRunning = false,
                editingTargetId = targetId,
                isClickSettingsDialogVisible = true
            )
        }
    }

    fun dismissClickSettingsDialog() {
        _state.update { current ->
            current.copy(
                editingTargetId = null,
                isClickSettingsDialogVisible = false
            )
        }
    }

    fun saveClickSettings(
        intervalValue: Int,
        timeUnit: TimeUnitOption,
        stopCondition: StopCondition,
        actionType: ActionType? = null,
        swipeDurationMs: Long? = null,
        holdBeforeSwipeMs: Long? = null
    ) {
        _state.update { current ->
            val targetId = current.editingTargetId
            val updatedSettings = if (targetId == null) {
                current.globalSettings.copy(
                    intervalValue = intervalValue,
                    timeUnit = timeUnit,
                    stopCondition = stopCondition,
                    swipeDurationMs = swipeDurationMs ?: current.globalSettings.swipeDurationMs
                )
            } else {
                current.globalSettings
            }
            val updatedTargets = current.targets.map { point ->
                if (targetId == null || point.id == targetId) {
                    val nextActionType = actionType ?: point.actionType
                    val screenW = current.screenWidthPx.coerceAtLeast(1)
                    val screenH = current.screenHeightPx.coerceAtLeast(1)

                    val updatedEndX = if (nextActionType == ActionType.SWIPE) {
                        point.endX ?: (point.x + 180f).coerceIn(24f, screenW - 24f.toFloat())
                    } else null

                    val updatedEndY = if (nextActionType == ActionType.SWIPE) {
                        point.endY ?: (point.y - 350f).coerceIn(24f, screenH - 24f.toFloat())
                    } else null

                    point.copy(
                        intervalValue = intervalValue,
                        timeUnit = timeUnit,
                        stopCondition = stopCondition,
                        actionType = nextActionType,
                        label = if (nextActionType == ActionType.SWIPE) "Swipe #${point.orderIndex}" else "Anchor point #${point.orderIndex}",
                        swipeDurationMs = swipeDurationMs ?: point.swipeDurationMs,
                        holdBeforeSwipeMs = holdBeforeSwipeMs ?: point.holdBeforeSwipeMs,
                        endX = updatedEndX,
                        endY = updatedEndY,
                        normalizedEndX = updatedEndX?.let { (it / screenW).coerceIn(0f, 1f) },
                        normalizedEndY = updatedEndY?.let { (it / screenH).coerceIn(0f, 1f) }
                    )
                } else {
                    point
                }
            }
            current.copy(
                globalSettings = updatedSettings,
                targets = updatedTargets,
                editingTargetId = null,
                isClickSettingsDialogVisible = false
            )
        }
    }

    fun selectProfile(profile: ScriptProfile) {
        _state.update { current ->
            val remappedTargets = profile.targets.mapIndexed { idx, point ->
                point.remapToScreenSize(current.screenWidthPx, current.screenHeightPx)
                    .copy(orderIndex = idx + 1)
            }
            current.copy(
                activeProfile = profile,
                activeMode = AutomationMode.MULTI_TARGET,
                targets = if (remappedTargets.isNotEmpty()) remappedTargets else current.targets,
                globalSettings = profile.settings
            )
        }
    }

    fun recordGestureExecution(targetIndex: Int, targetId: String, x: Float, y: Float, elapsedMs: Long) {
        _state.update { current ->
            current.copy(
                totalClicksExecuted = current.totalClicksExecuted + 1L,
                activeTargetIndex = targetIndex,
                elapsedRunTimeMs = elapsedMs
            )
        }
        _events.tryEmit(AutomationEngineEvent.PulseTarget(targetId, x, y))
    }
}
