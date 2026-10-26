package com.kinetic.autoclicker.presentation.components

import android.view.MotionEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.domain.model.HudDisplayMode
import com.kinetic.autoclicker.presentation.theme.KineticComponentShape
import com.kinetic.autoclicker.presentation.theme.KineticTileShape
import com.kinetic.autoclicker.presentation.theme.LocalAppStrings
import com.kinetic.autoclicker.presentation.theme.LocalKineticPalette
import kotlin.math.hypot

@Composable
fun FloatingHudContainer(
    hudDisplayMode: HudDisplayMode,
    isRunning: Boolean,
    intervalMs: Long,
    hudOpacity: Float = 0.95f,
    isDockedRightEdge: Boolean = false,
    onTogglePlayPause: () -> Unit,
    onAddTarget: () -> Unit,
    onRemoveTarget: () -> Unit,
    onOpenSettings: () -> Unit,
    onCollapseToEdgePill: () -> Unit,
    onExpandFromEdgePill: () -> Unit,
    onCloseOverlay: () -> Unit,
    onDragHud: (dx: Float, dy: Float) -> Unit,
    onHudTouchActiveChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = hudDisplayMode,
        label = "hud_mode_transition",
        modifier = modifier
    ) { mode ->
        when (mode) {
            HudDisplayMode.EDGE_PILL -> {
                EdgeCapsulePillOverlay(
                    isRunning = isRunning,
                    intervalMs = intervalMs,
                    hudOpacity = hudOpacity,
                    isDockedRightEdge = isDockedRightEdge,
                    onTogglePlayPause = onTogglePlayPause,
                    onExpand = onExpandFromEdgePill,
                    onDragHud = onDragHud,
                    onHudTouchActiveChanged = onHudTouchActiveChanged
                )
            }
            HudDisplayMode.EXPANDED_BAR -> {
                FloatingHudBarOverlay(
                    isRunning = isRunning,
                    hudOpacity = hudOpacity,
                    onTogglePlayPause = onTogglePlayPause,
                    onAddTarget = onAddTarget,
                    onRemoveTarget = onRemoveTarget,
                    onOpenSettings = onOpenSettings,
                    onCollapseToEdgePill = onCollapseToEdgePill,
                    onCloseOverlay = onCloseOverlay,
                    onDragHud = onDragHud,
                    onHudTouchActiveChanged = onHudTouchActiveChanged
                )
            }
            HudDisplayMode.HIDDEN -> Unit
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FloatingHudBarOverlay(
    isRunning: Boolean,
    hudOpacity: Float = 0.95f,
    onTogglePlayPause: () -> Unit,
    onAddTarget: () -> Unit,
    onRemoveTarget: () -> Unit,
    onOpenSettings: () -> Unit,
    onCollapseToEdgePill: () -> Unit,
    onCloseOverlay: () -> Unit,
    onDragHud: (dx: Float, dy: Float) -> Unit,
    onHudTouchActiveChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalKineticPalette.current
    val currentOnDragHud by rememberUpdatedState(onDragHud)
    val currentOnCollapse by rememberUpdatedState(onCollapseToEdgePill)
    val currentOnTouchChanged by rememberUpdatedState(onHudTouchActiveChanged)
    val disallowIntercept = remember { RequestDisallowInterceptTouchEvent() }

    var downRawX by remember { mutableFloatStateOf(0f) }
    var downRawY by remember { mutableFloatStateOf(0f) }
    var lastRawX by remember { mutableFloatStateOf(0f) }
    var lastRawY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .alpha(hudOpacity.coerceIn(0.4f, 1f))
            .shadow(24.dp, CircleShape)
            .clip(CircleShape)
            .background(palette.surfaceContainerLow.copy(alpha = 0.96f))
            .border(
                width = 1.5.dp,
                color = if (isRunning) palette.primary else palette.outlineVariant.copy(alpha = 0.6f),
                shape = CircleShape
            )
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        // Drag Handle / Tap to Collapse into Edge Pill
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 48.dp, height = 30.dp)
                .pointerInteropFilter(requestDisallowInterceptTouchEvent = disallowIntercept) { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            disallowIntercept.invoke(true)
                            currentOnTouchChanged(true)
                            downRawX = event.rawX
                            downRawY = event.rawY
                            lastRawX = event.rawX
                            lastRawY = event.rawY
                            isDragging = false
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val totalDist = hypot(event.rawX - downRawX, event.rawY - downRawY)
                            if (!isDragging && totalDist > 6f) {
                                isDragging = true
                            }
                            if (isDragging) {
                                val dx = event.rawX - lastRawX
                                val dy = event.rawY - lastRawY
                                lastRawX = event.rawX
                                lastRawY = event.rawY
                                if (dx != 0f || dy != 0f) {
                                    currentOnDragHud(dx, dy)
                                }
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            disallowIntercept.invoke(false)
                            currentOnTouchChanged(false)
                            if (!isDragging) {
                                currentOnCollapse()
                            }
                            isDragging = false
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            disallowIntercept.invoke(false)
                            currentOnTouchChanged(false)
                            isDragging = false
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = "Drag HUD or tap to collapse into Edge Pill",
                tint = palette.outline,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val playBg = if (isRunning) palette.tertiaryContainer else palette.primary
            val playTint = if (isRunning) palette.onTertiaryContainer else palette.onPrimary

            // Instant-response Play/Pause button: triggers on ACTION_DOWN so it NEVER misses a tap during fast auto-clicking!
            HudCircleIconButton(
                icon = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Start",
                containerColor = playBg,
                contentColor = playTint,
                hasGlow = true,
                onHudTouchActiveChanged = currentOnTouchChanged,
                onClick = onTogglePlayPause
            )

            HudCircleIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Add Target (Tap or Swipe)",
                containerColor = palette.surfaceContainerHigh,
                contentColor = palette.onSurface,
                onHudTouchActiveChanged = currentOnTouchChanged,
                onClick = onAddTarget
            )

            HudCircleIconButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Remove Last Target",
                containerColor = palette.surfaceContainerHigh,
                contentColor = palette.onSurfaceVariant,
                onHudTouchActiveChanged = currentOnTouchChanged,
                onClick = onRemoveTarget
            )

            HudCircleIconButton(
                icon = Icons.Rounded.Tune,
                contentDescription = "Target Configuration",
                containerColor = palette.surfaceContainerHigh,
                contentColor = palette.onSurfaceVariant,
                onHudTouchActiveChanged = currentOnTouchChanged,
                onClick = onOpenSettings
            )

            Box(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .width(24.dp)
                    .height(1.5.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceContainerHighest)
            )

            HudCircleIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Dismiss Overlay",
                containerColor = palette.surfaceContainer,
                contentColor = palette.outline.copy(alpha = 0.9f),
                onHudTouchActiveChanged = currentOnTouchChanged,
                onClick = onCloseOverlay
            )
        }
    }
}

/**
 * Hardware-priority HUD icon button that triggers immediately on ACTION_DOWN so that
 * rapid AccessibilityService.dispatchGesture() strokes can NEVER cancel or block the user's click!
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HudCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    hasGlow: Boolean = false,
    onHudTouchActiveChanged: (Boolean) -> Unit = {},
    onClick: () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnTouchChanged by rememberUpdatedState(onHudTouchActiveChanged)
    val disallowIntercept = remember { RequestDisallowInterceptTouchEvent() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .then(
                if (hasGlow) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = containerColor,
                        spotColor = containerColor
                    )
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .background(containerColor)
            .pointerInteropFilter(requestDisallowInterceptTouchEvent = disallowIntercept) { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        disallowIntercept.invoke(true)
                        currentOnTouchChanged(true)
                        currentOnClick()
                        true
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        disallowIntercept.invoke(false)
                        currentOnTouchChanged(false)
                        true
                    }
                    else -> true
                }
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EdgeCapsulePillOverlay(
    isRunning: Boolean,
    intervalMs: Long,
    hudOpacity: Float = 0.95f,
    isDockedRightEdge: Boolean = false,
    onTogglePlayPause: () -> Unit,
    onExpand: () -> Unit,
    onDragHud: (dx: Float, dy: Float) -> Unit,
    onHudTouchActiveChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalKineticPalette.current
    val currentOnToggle by rememberUpdatedState(onTogglePlayPause)
    val currentOnDragHud by rememberUpdatedState(onDragHud)
    val currentOnExpand by rememberUpdatedState(onExpand)
    val currentOnTouchChanged by rememberUpdatedState(onHudTouchActiveChanged)
    val playDisallowIntercept = remember { RequestDisallowInterceptTouchEvent() }
    val dragDisallowIntercept = remember { RequestDisallowInterceptTouchEvent() }

    var downRawX by remember { mutableFloatStateOf(0f) }
    var downRawY by remember { mutableFloatStateOf(0f) }
    var lastRawX by remember { mutableFloatStateOf(0f) }
    var lastRawY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "edge_pill_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 0.3f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    val capsuleShape = RoundedCornerShape(20.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .alpha(hudOpacity.coerceIn(0.45f, 1f))
            .shadow(16.dp, capsuleShape)
            .clip(capsuleShape)
            .background(palette.surfaceContainerLow.copy(alpha = 0.96f))
            .border(
                width = 1.5.dp,
                color = if (isRunning) palette.primary else palette.outlineVariant,
                shape = capsuleShape
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isRunning) palette.tertiaryContainer else palette.primary)
                .pointerInteropFilter(requestDisallowInterceptTouchEvent = playDisallowIntercept) { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            playDisallowIntercept.invoke(true)
                            currentOnTouchChanged(true)
                            currentOnToggle()
                            true
                        }
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            playDisallowIntercept.invoke(false)
                            currentOnTouchChanged(false)
                            true
                        }
                        else -> true
                    }
                }
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Start",
                tint = if (isRunning) palette.onTertiaryContainer else palette.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .pointerInteropFilter(requestDisallowInterceptTouchEvent = dragDisallowIntercept) { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            dragDisallowIntercept.invoke(true)
                            currentOnTouchChanged(true)
                            downRawX = event.rawX
                            downRawY = event.rawY
                            lastRawX = event.rawX
                            lastRawY = event.rawY
                            isDragging = false
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val totalDist = hypot(event.rawX - downRawX, event.rawY - downRawY)
                            if (!isDragging && totalDist > 6f) {
                                isDragging = true
                            }
                            if (isDragging) {
                                val dx = event.rawX - lastRawX
                                val dy = event.rawY - lastRawY
                                lastRawX = event.rawX
                                lastRawY = event.rawY
                                if (dx != 0f || dy != 0f) {
                                    currentOnDragHud(dx, dy)
                                }
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            dragDisallowIntercept.invoke(false)
                            currentOnTouchChanged(false)
                            if (!isDragging) {
                                currentOnExpand()
                            }
                            isDragging = false
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            dragDisallowIntercept.invoke(false)
                            currentOnTouchChanged(false)
                            isDragging = false
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(dotAlpha)
                    .clip(CircleShape)
                    .background(palette.primary)
            )
            Text(
                text = "${intervalMs}ms",
                style = MaterialTheme.typography.labelMedium,
                color = palette.onSurface,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (isDockedRightEdge) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
                contentDescription = "Expand HUD Bar",
                tint = palette.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Modal overlay asking the user whether to add a Tap (Tıklama) or Swipe (Kaydırma) target
 * whenever they press the (+) button.
 */
@Composable
fun AddTargetTypeModalOverlay(
    onSelectTap: () -> Unit,
    onSelectSwipe: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss
            )
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .shadow(24.dp, KineticComponentShape)
                .clip(KineticComponentShape)
                .background(palette.surfaceContainer)
                .border(1.dp, palette.outlineVariant.copy(alpha = 0.5f), KineticComponentShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {}
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = title ?: strings.addTargetDialogTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle ?: strings.addTargetDialogSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceContainerHigh)
                        .clickable(onClick = onDismiss)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = palette.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Option 1: Add Tap (Tıklama)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KineticTileShape)
                    .background(palette.surfaceContainerLow)
                    .border(1.5.dp, palette.primary.copy(alpha = 0.65f), KineticTileShape)
                    .clickable(onClick = onSelectTap)
                    .padding(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(palette.primary.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.addTapOptionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.addTapOptionDesc,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }
            }

            // Option 2: Add Swipe (Kaydırma)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KineticTileShape)
                    .background(palette.surfaceContainerLow)
                    .border(1.5.dp, palette.secondary.copy(alpha = 0.65f), KineticTileShape)
                    .clickable(onClick = onSelectSwipe)
                    .padding(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(palette.secondary.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Gesture,
                        contentDescription = null,
                        tint = palette.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.addSwipeOptionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.addSwipeOptionDesc,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SequenceMetricsBannerPill(
    intervalMs: Long,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(palette.surfaceContainerLow.copy(alpha = 0.92f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(palette.primary)
        )
        Text(
            text = strings.intervalPrefix,
            style = MaterialTheme.typography.labelMedium,
            color = palette.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${intervalMs}ms",
            style = MaterialTheme.typography.labelMedium,
            color = palette.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BottomStatusToastPill(
    modeLabel: String,
    statusLabel: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalKineticPalette.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(palette.surfaceContainerHigh.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(palette.primary)
        )
        Text(
            text = modeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = palette.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "•",
            style = MaterialTheme.typography.labelMedium,
            color = palette.outline
        )
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelMedium,
            color = palette.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
