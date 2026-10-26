package com.kinetic.autoclicker.domain.model

data class AutomationEngineState(
    val isRunning: Boolean = false,
    val isOverlayVisible: Boolean = false,
    val hudDisplayMode: HudDisplayMode = HudDisplayMode.HIDDEN,
    val activeMode: AutomationMode = AutomationMode.MULTI_TARGET,
    val targets: List<TargetPoint> = listOf(
        TargetPoint(
            id = "default-target-1",
            orderIndex = 1,
            label = "Anchor point #1",
            x = 540f,
            y = 1120f,
            normalizedX = 0.5f,
            normalizedY = 0.46666667f,
            intervalValue = 100,
            timeUnit = TimeUnitOption.MILLISECONDS
        )
    ),
    val screenWidthPx: Int = 1080,
    val screenHeightPx: Int = 2400,
    val editingTargetId: String? = null,
    val isClickSettingsDialogVisible: Boolean = false,
    val isAddTargetDialogVisible: Boolean = false,
    val isStartChooserDialogVisible: Boolean = false,
    val isUserInteractingWithHud: Boolean = false,
    val globalSettings: EngineSettings = EngineSettings(),
    val activeProfile: ScriptProfile? = null,
    val totalClicksExecuted: Long = 0L,
    val activeTargetIndex: Int = 0,
    val elapsedRunTimeMs: Long = 0L,
    val hudPositionX: Int = 24,
    val hudPositionY: Int = 260,
    val isDockedRightEdge: Boolean = false,
    val isAccessibilityServiceConnected: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
    val showDragHintTooltip: Boolean = false
) {
    val activeIntervalMs: Long
        get() = globalSettings.intervalMs

    val editingTarget: TargetPoint?
        get() = editingTargetId?.let { id -> targets.find { it.id == id } }
}

sealed interface AutomationEngineEvent {
    data class PulseTarget(val targetId: String, val x: Float, val y: Float) : AutomationEngineEvent
    data class StatusToast(val message: String) : AutomationEngineEvent
    data object RequestAccessibilitySettings : AutomationEngineEvent
    data object RequestOverlayPermission : AutomationEngineEvent
}
