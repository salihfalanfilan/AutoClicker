package com.kinetic.autoclicker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.domain.model.AutomationMode
import com.kinetic.autoclicker.domain.model.ScriptProfile
import com.kinetic.autoclicker.presentation.contract.MainUiIntent
import com.kinetic.autoclicker.presentation.contract.MainUiState
import com.kinetic.autoclicker.presentation.theme.KineticComponentShape
import com.kinetic.autoclicker.presentation.theme.KineticTileShape
import com.kinetic.autoclicker.presentation.theme.LocalAppStrings
import com.kinetic.autoclicker.presentation.theme.LocalKineticPalette
import kotlin.math.roundToInt

@Composable
fun ScriptsScreen(
    uiState: MainUiState,
    onIntent: (MainUiIntent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current
    val engineState = uiState.engineState
    var newProfileName by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, KineticComponentShape)
                    .clip(KineticComponentShape)
                    .background(palette.surfaceContainer)
                    .border(1.dp, palette.outlineVariant.copy(alpha = 0.45f), KineticComponentShape)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.configuredTargetsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = strings.activeAnchorsSubtitle(engineState.targets.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.surfaceContainerHigh)
                                .clickable { onIntent(MainUiIntent.AddTarget(ActionType.TAP)) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = strings.addTapButton,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.surfaceContainerHigh)
                                .clickable { onIntent(MainUiIntent.AddTarget(ActionType.SWIPE)) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = strings.addSwipeButton,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                engineState.targets.forEach { target ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(KineticTileShape)
                            .background(palette.surfaceContainerLow)
                            .clickable { onIntent(MainUiIntent.OpenClickSettings(target.id)) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(palette.secondaryContainer)
                            ) {
                                Text(
                                    text = target.orderIndex.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = target.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${target.actionType.localizedLabel(strings.isTurkish)} • ${target.intervalValue} ${target.timeUnit.localizedSuffix(strings.isTurkish)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "X: ${target.x.roundToInt()} • Y: ${target.y.roundToInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(KineticTileShape)
                            .background(palette.surfaceContainerLowest)
                            .border(1.dp, palette.outlineVariant, KineticTileShape)
                            .padding(horizontal = 14.dp)
                    ) {
                        if (newProfileName.isEmpty()) {
                            Text(
                                text = strings.newPresetPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        BasicTextField(
                            value = newProfileName,
                            onValueChange = { newProfileName = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.onSurface),
                            cursorBrush = SolidColor(palette.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(palette.primaryContainer)
                            .clickable {
                                onIntent(MainUiIntent.SaveCurrentAsProfile(newProfileName))
                                newProfileName = ""
                            }
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = palette.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = strings.saveButton,
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = strings.savedScenariosHeader,
                style = MaterialTheme.typography.titleMedium,
                color = palette.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(uiState.profiles, key = { it.id }) { profile ->
            val isSelected = engineState.activeProfile?.id == profile.id
            ScriptProfileCard(
                profile = profile,
                isSelected = isSelected,
                onSelect = { onIntent(MainUiIntent.SelectScriptProfile(profile)) },
                onDelete = { onIntent(MainUiIntent.DeleteScriptProfile(profile.id)) }
            )
        }
    }
}

@Composable
private fun ScriptProfileCard(
    profile: ScriptProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, KineticComponentShape)
            .clip(KineticComponentShape)
            .background(palette.surfaceContainer)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) palette.primary else palette.outlineVariant.copy(alpha = 0.45f),
                shape = KineticComponentShape
            )
            .clickable(onClick = onSelect)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (profile.mode == AutomationMode.SINGLE_TARGET) {
                            palette.primary.copy(alpha = 0.2f)
                        } else {
                            palette.secondary.copy(alpha = 0.2f)
                        }
                    )
            ) {
                Icon(
                    imageVector = if (profile.mode == AutomationMode.SINGLE_TARGET) {
                        Icons.Rounded.TouchApp
                    } else {
                        Icons.Rounded.Layers
                    },
                    contentDescription = null,
                    tint = if (profile.mode == AutomationMode.SINGLE_TARGET) palette.primary else palette.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${profile.summarySubtitle(strings.isTurkish)} • ${profile.settings.intervalValue} ${profile.settings.timeUnit.localizedSuffix(strings.isTurkish)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) palette.primary.copy(alpha = 0.16f) else palette.surfaceContainerHigh)
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (isSelected) palette.primary else palette.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (isSelected) strings.statusActive else strings.loadButton,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) palette.primary else palette.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceContainerHigh)
                    .clickable(onClick = onDelete)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete preset",
                    tint = palette.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
