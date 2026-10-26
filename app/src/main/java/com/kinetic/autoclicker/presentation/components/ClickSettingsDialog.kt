package com.kinetic.autoclicker.presentation.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import com.kinetic.autoclicker.domain.model.ActionType
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetic.autoclicker.domain.model.StopCondition
import com.kinetic.autoclicker.domain.model.StopConditionType
import com.kinetic.autoclicker.domain.model.TimeUnitOption
import com.kinetic.autoclicker.presentation.theme.KineticComponentShape
import com.kinetic.autoclicker.presentation.theme.KineticModalShape
import com.kinetic.autoclicker.presentation.theme.LocalAppStrings
import com.kinetic.autoclicker.presentation.theme.LocalKineticPalette
import com.kinetic.autoclicker.presentation.theme.TabularCounterStyle

@Composable
fun ClickSettingsModalOverlay(
    initialIntervalValue: Int,
    initialTimeUnit: TimeUnitOption,
    initialStopCondition: StopCondition,
    initialActionType: ActionType = ActionType.TAP,
    initialSwipeDurationMs: Long = 300L,
    initialHoldBeforeSwipeMs: Long = 80L,
    targetSubtitle: String? = null,
    onDeleteTarget: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSaveAndApply: (
        intervalValue: Int,
        timeUnit: TimeUnitOption,
        stopCondition: StopCondition,
        actionType: ActionType,
        swipeDurationMs: Long,
        holdBeforeSwipeMs: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        ClickSettingsDialogCard(
            initialIntervalValue = initialIntervalValue,
            initialTimeUnit = initialTimeUnit,
            initialStopCondition = initialStopCondition,
            initialActionType = initialActionType,
            initialSwipeDurationMs = initialSwipeDurationMs,
            initialHoldBeforeSwipeMs = initialHoldBeforeSwipeMs,
            targetSubtitle = targetSubtitle ?: strings.pacingAndRunRules,
            onDeleteTarget = onDeleteTarget,
            onDismiss = onDismiss,
            onSaveAndApply = onSaveAndApply,
            modifier = Modifier.clickable(
                interactionSource = cardInteraction,
                indication = null,
                onClick = {}
            )
        )
    }
}

@Composable
fun ClickSettingsDialogCard(
    initialIntervalValue: Int,
    initialTimeUnit: TimeUnitOption,
    initialStopCondition: StopCondition,
    initialActionType: ActionType = ActionType.TAP,
    initialSwipeDurationMs: Long = 300L,
    initialHoldBeforeSwipeMs: Long = 80L,
    targetSubtitle: String = "Pacing and run rules",
    onDeleteTarget: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSaveAndApply: (
        intervalValue: Int,
        timeUnit: TimeUnitOption,
        stopCondition: StopCondition,
        actionType: ActionType,
        swipeDurationMs: Long,
        holdBeforeSwipeMs: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current

    var selectedActionType by remember(initialActionType) {
        mutableStateOf(initialActionType)
    }
    var swipeDuration by remember(initialSwipeDurationMs) {
        mutableStateOf(initialSwipeDurationMs.coerceIn(50L, 5000L))
    }
    var holdBeforeSwipe by remember(initialHoldBeforeSwipeMs) {
        mutableStateOf(initialHoldBeforeSwipeMs.coerceIn(0L, 2000L))
    }

    var intervalText by remember(initialIntervalValue) {
        mutableStateOf(initialIntervalValue.toString())
    }
    var selectedUnit by remember(initialTimeUnit) {
        mutableStateOf(initialTimeUnit)
    }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    var stopType by remember(initialStopCondition.type) {
        mutableStateOf(initialStopCondition.type)
    }
    var timerMinutes by remember(initialStopCondition.timerMinutes) {
        mutableIntStateOf(initialStopCondition.timerMinutes.coerceAtLeast(1))
    }
    var cycleCount by remember(initialStopCondition.cycleCount) {
        mutableIntStateOf(initialStopCondition.cycleCount.coerceAtLeast(1))
    }

    val numericInterval = intervalText.toIntOrNull()?.coerceIn(
        if (selectedUnit == TimeUnitOption.MILLISECONDS) 10 else 1,
        9999
    ) ?: if (selectedUnit == TimeUnitOption.MILLISECONDS) 100 else 1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 390.dp)
            .shadow(28.dp, KineticModalShape)
            .clip(KineticModalShape)
            .background(palette.surfaceContainer)
            .border(1.dp, palette.outlineVariant.copy(alpha = 0.45f), KineticModalShape)
    ) {
        // Modal Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 20.dp, top = 24.dp, bottom = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceContainerHighest)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = strings.clickSettingsTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = targetSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = palette.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Modal Body Content
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Section 0: Aksiyon Türü Seçimi (Tıklama mı Kaydırma mı?)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.actionTypeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tıklama Butonu
                    val isTap = selectedActionType == ActionType.TAP
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(KineticComponentShape)
                            .background(if (isTap) palette.primaryContainer else palette.surfaceContainerHigh)
                            .border(
                                1.dp,
                                if (isTap) palette.primary else palette.outlineVariant.copy(alpha = 0.4f),
                                KineticComponentShape
                            )
                            .clickable { selectedActionType = ActionType.TAP }
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = if (isTap) palette.onPrimaryContainer else palette.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.actionTapName,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isTap) palette.onPrimaryContainer else palette.onSurface,
                                fontWeight = if (isTap) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Kaydırma Butonu
                    val isSwipe = selectedActionType == ActionType.SWIPE
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(KineticComponentShape)
                            .background(if (isSwipe) palette.primaryContainer else palette.surfaceContainerHigh)
                            .border(
                                1.dp,
                                if (isSwipe) palette.primary else palette.outlineVariant.copy(alpha = 0.4f),
                                KineticComponentShape
                            )
                            .clickable { selectedActionType = ActionType.SWIPE }
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = if (isSwipe) palette.onPrimaryContainer else palette.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.actionSwipeName,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSwipe) palette.onPrimaryContainer else palette.onSurface,
                                fontWeight = if (isSwipe) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Section 0B: Kaydırma Özellikleri (Sadece Kaydırma Seçiliyse)
            if (selectedActionType == ActionType.SWIPE) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Kaydırma Süresi (Swipe Duration)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = strings.swipeDurationSettingLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = strings.swipeDurationSettingDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(palette.surfaceContainerHigh)
                                    .clickable {
                                        swipeDuration = (swipeDuration - 50L).coerceAtLeast(50L)
                                    }
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "-50ms", tint = palette.onSurface)
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(KineticComponentShape)
                                    .background(palette.surfaceContainerHigh)
                                    .border(1.dp, palette.outlineVariant.copy(alpha = 0.4f), KineticComponentShape)
                            ) {
                                Text(
                                    text = "$swipeDuration ms",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(palette.surfaceContainerHigh)
                                    .clickable {
                                        swipeDuration = (swipeDuration + 50L).coerceAtMost(5000L)
                                    }
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "+50ms", tint = palette.onSurface)
                            }
                        }
                    }

                    // Basılı Tutup Kaydır (Hold Before Swipe)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = strings.swipeHoldSettingLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = strings.swipeHoldSettingDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(palette.surfaceContainerHigh)
                                    .clickable {
                                        holdBeforeSwipe = (holdBeforeSwipe - 20L).coerceAtLeast(0L)
                                    }
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "-20ms", tint = palette.onSurface)
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(KineticComponentShape)
                                    .background(palette.surfaceContainerHigh)
                                    .border(1.dp, palette.outlineVariant.copy(alpha = 0.4f), KineticComponentShape)
                            ) {
                                Text(
                                    text = if (holdBeforeSwipe <= 0L) "0 ms (Hemen)" else "$holdBeforeSwipe ms",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(palette.surfaceContainerHigh)
                                    .clickable {
                                        holdBeforeSwipe = (holdBeforeSwipe + 20L).coerceAtMost(2000L)
                                    }
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "+20ms", tint = palette.onSurface)
                            }
                        }
                    }
                }
            }

            // Section 1: Interval
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.clickEveryLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = strings.intervalBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Numeric Stepper Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .weight(0.6f)
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceContainerLowest)
                            .border(1.dp, palette.outlineVariant.copy(alpha = 0.4f), CircleShape)
                            .padding(horizontal = 6.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceContainerHigh)
                                .clickable {
                                    val step = if (selectedUnit == TimeUnitOption.MILLISECONDS) 25 else 1
                                    val min = if (selectedUnit == TimeUnitOption.MILLISECONDS) 10 else 1
                                    val next = (numericInterval - step).coerceAtLeast(min)
                                    intervalText = next.toString()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Decrease interval",
                                tint = palette.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        BasicTextField(
                            value = intervalText,
                            onValueChange = { raw ->
                                val filtered = raw.filter { it.isDigit() }.take(4)
                                intervalText = filtered
                            },
                            singleLine = true,
                            textStyle = TabularCounterStyle.copy(
                                color = palette.onSurface,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = SolidColor(palette.primary),
                            modifier = Modifier.width(68.dp)
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceContainerHigh)
                                .clickable {
                                    val step = if (selectedUnit == TimeUnitOption.MILLISECONDS) 25 else 1
                                    val next = (numericInterval + step).coerceAtMost(9999)
                                    intervalText = next.toString()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Increase interval",
                                tint = palette.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Unit Dropdown Selector
                    Box(modifier = Modifier.weight(0.4f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceContainerHighest)
                                .clickable { unitMenuExpanded = true }
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = selectedUnit.localizedName(strings.isTurkish),
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = palette.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false },
                            modifier = Modifier.background(palette.surfaceContainerHighest)
                        ) {
                            TimeUnitOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = option.localizedName(strings.isTurkish),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = palette.onSurface
                                            )
                                            if (option == selectedUnit) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = palette.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedUnit = option
                                        unitMenuExpanded = false
                                        if (option == TimeUnitOption.MILLISECONDS && numericInterval < 10) {
                                            intervalText = "100"
                                        } else if (option != TimeUnitOption.MILLISECONDS && numericInterval > 60) {
                                            intervalText = "1"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Helper Note with Dynamic Pace Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = selectedUnit.formatRateSummary(numericInterval, strings.isTurkish),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }
            }

            // Section 2: Stop condition
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.stopAfterLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.loopLimitLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StopConditionRadioCard(
                        selected = stopType == StopConditionType.INFINITE,
                        title = strings.runIndefinitelyTitle,
                        subtitle = strings.runIndefinitelyDesc,
                        trailingIcon = Icons.Rounded.AllInclusive,
                        onClick = { stopType = StopConditionType.INFINITE }
                    )

                    StopConditionRadioCard(
                        selected = stopType == StopConditionType.TIMER_LIMIT,
                        title = strings.timerLimitTitle(timerMinutes),
                        subtitle = strings.timerLimitDesc,
                        trailingIcon = Icons.Rounded.Timer,
                        onClick = { stopType = StopConditionType.TIMER_LIMIT },
                        onDecrement = if (stopType == StopConditionType.TIMER_LIMIT) {
                            { timerMinutes = (timerMinutes - 1).coerceAtLeast(1) }
                        } else null,
                        onIncrement = if (stopType == StopConditionType.TIMER_LIMIT) {
                            { timerMinutes = (timerMinutes + 5).coerceAtMost(240) }
                        } else null
                    )

                    StopConditionRadioCard(
                        selected = stopType == StopConditionType.CYCLE_COUNT,
                        title = strings.cycleLimitTitle(cycleCount),
                        subtitle = strings.cycleLimitDesc,
                        trailingIcon = Icons.Rounded.Pin,
                        onClick = { stopType = StopConditionType.CYCLE_COUNT },
                        onDecrement = if (stopType == StopConditionType.CYCLE_COUNT) {
                            { cycleCount = (cycleCount - 25).coerceAtLeast(10) }
                        } else null,
                        onIncrement = if (stopType == StopConditionType.CYCLE_COUNT) {
                            { cycleCount = (cycleCount + 25).coerceAtMost(50_000) }
                        } else null
                    )
                }
            }
        }

        // Modal Bottom Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            if (onDeleteTarget != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(palette.errorContainer.copy(alpha = 0.90f))
                        .clickable {
                            onDeleteTarget()
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = strings.deleteTargetButton,
                        tint = palette.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.deleteTargetButton,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = strings.cancelButton,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            ambientColor = palette.primaryContainer,
                            spotColor = palette.primaryContainer
                        )
                        .clip(CircleShape)
                        .background(palette.primaryContainer)
                        .clickable {
                            onSaveAndApply(
                                numericInterval,
                                selectedUnit,
                                StopCondition(
                                    type = stopType,
                                    timerMinutes = timerMinutes,
                                    cycleCount = cycleCount
                                ),
                                selectedActionType,
                                swipeDuration,
                                holdBeforeSwipe
                            )
                        }
                        .padding(horizontal = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = palette.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.saveAndApplyButton,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StopConditionRadioCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    trailingIcon: ImageVector,
    onClick: () -> Unit,
    onDecrement: (() -> Unit)? = null,
    onIncrement: (() -> Unit)? = null
) {
    val palette = LocalKineticPalette.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(KineticComponentShape)
            .background(if (selected) palette.surfaceContainerHigh else palette.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) palette.primary else palette.surfaceContainerHighest)
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette.onPrimary)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.onSurfaceVariant
                )
            }
        }

        if (selected && onDecrement != null && onIncrement != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceContainerHighest)
                        .clickable(onClick = onDecrement)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Decrease limit",
                        tint = palette.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceContainerHighest)
                        .clickable(onClick = onIncrement)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Increase limit",
                        tint = palette.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = if (selected) palette.primary else palette.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
