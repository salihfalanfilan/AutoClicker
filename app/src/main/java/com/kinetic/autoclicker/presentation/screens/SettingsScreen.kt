package com.kinetic.autoclicker.presentation.screens

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kinetic.autoclicker.domain.model.AppLanguage
import com.kinetic.autoclicker.domain.model.BuiltInColorPresets
import com.kinetic.autoclicker.domain.model.ThemeMode
import com.kinetic.autoclicker.presentation.contract.MainUiIntent
import com.kinetic.autoclicker.presentation.contract.MainUiState
import com.kinetic.autoclicker.presentation.theme.KineticComponentShape
import com.kinetic.autoclicker.presentation.theme.KineticTileShape
import com.kinetic.autoclicker.presentation.theme.LocalAppStrings
import com.kinetic.autoclicker.presentation.theme.LocalKineticPalette
import com.kinetic.autoclicker.presentation.theme.hexToComposeColor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onIntent: (MainUiIntent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current
    val settings = uiState.engineState.globalSettings

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Card 1: Language, Theme Mode (System / Light / Dark / AMOLED) & Color Spectrum Studio
        AppearanceAndColorStudioCard(
            appLanguage = settings.appLanguage,
            themeMode = settings.themeMode,
            primaryAccentHex = settings.primaryAccentHex,
            secondaryAccentHex = settings.secondaryAccentHex,
            onIntent = onIntent
        )

        // Card 2: Anti-Detection & Gesture Precision (DataStore)
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, KineticComponentShape)
                .clip(KineticComponentShape)
                .background(palette.surfaceContainer)
                .border(1.dp, palette.outlineVariant.copy(alpha = 0.45f), KineticComponentShape)
                .padding(16.dp)
        ) {
            Text(
                text = strings.gestureEngineSection,
                style = MaterialTheme.typography.titleMedium,
                color = palette.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KineticTileShape)
                    .background(palette.surfaceContainerLow)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.smartJitterTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.smartJitterSubtitle(settings.jitterRadiusPx),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }

                Switch(
                    checked = settings.smartJitterEnabled,
                    onCheckedChange = { onIntent(MainUiIntent.ToggleSmartJitter(it)) },
                    thumbContent = if (settings.smartJitterEnabled) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = palette.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else null,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = palette.primaryContainer,
                        checkedThumbColor = palette.onSurface,
                        uncheckedTrackColor = palette.surfaceContainerHighest,
                        uncheckedThumbColor = palette.onSurfaceVariant
                    )
                )
            }

            // Dokunma Koruması (Farklı yerlere aynı anda tıklamaz)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KineticTileShape)
                    .background(palette.surfaceContainerLow)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.touchProtectionTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.touchProtectionSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }

                Switch(
                    checked = settings.touchProtectionEnabled,
                    onCheckedChange = { onIntent(MainUiIntent.ToggleTouchProtection(it)) },
                    thumbContent = if (settings.touchProtectionEnabled) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = palette.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else null,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = palette.primaryContainer,
                        checkedThumbColor = palette.onSurface,
                        uncheckedTrackColor = palette.surfaceContainerHighest,
                        uncheckedThumbColor = palette.onSurfaceVariant
                    )
                )
            }

            SettingsStepperRow(
                label = strings.jitterRadiusLabel,
                valueText = "${settings.jitterRadiusPx} px",
                onDecrement = {
                    onIntent(MainUiIntent.UpdateJitterRadius(settings.jitterRadiusPx - 2))
                },
                onIncrement = {
                    onIntent(MainUiIntent.UpdateJitterRadius(settings.jitterRadiusPx + 2))
                }
            )

            SettingsStepperRow(
                label = strings.tapHoldDurationLabel,
                valueText = "${settings.holdDurationMs} ms",
                onDecrement = {
                    onIntent(
                        MainUiIntent.UpdateHoldAndSwipeDuration(
                            holdMs = settings.holdDurationMs - 10L,
                            swipeMs = settings.swipeDurationMs
                        )
                    )
                },
                onIncrement = {
                    onIntent(
                        MainUiIntent.UpdateHoldAndSwipeDuration(
                            holdMs = settings.holdDurationMs + 10L,
                            swipeMs = settings.swipeDurationMs
                        )
                    )
                }
            )

            SettingsStepperRow(
                label = strings.swipeDurationLabel,
                valueText = "${settings.swipeDurationMs} ms",
                onDecrement = {
                    onIntent(
                        MainUiIntent.UpdateHoldAndSwipeDuration(
                            holdMs = settings.holdDurationMs,
                            swipeMs = settings.swipeDurationMs - 40L
                        )
                    )
                },
                onIncrement = {
                    onIntent(
                        MainUiIntent.UpdateHoldAndSwipeDuration(
                            holdMs = settings.holdDurationMs,
                            swipeMs = settings.swipeDurationMs + 40L
                        )
                    )
                }
            )
        }

        // Card 3: Floating HUD & Edge Pill Mode Settings
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, KineticComponentShape)
                .clip(KineticComponentShape)
                .background(palette.surfaceContainer)
                .border(1.dp, palette.outlineVariant.copy(alpha = 0.45f), KineticComponentShape)
                .padding(16.dp)
        ) {
            Text(
                text = strings.floatingHudSection,
                style = MaterialTheme.typography.titleMedium,
                color = palette.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KineticTileShape)
                    .background(palette.surfaceContainerLow)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.autoCollapsePillTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.autoCollapsePillSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }

                Switch(
                    checked = settings.autoCollapseToEdgePill,
                    onCheckedChange = {
                        onIntent(
                            MainUiIntent.UpdateHudPreferences(
                                opacity = settings.hudOpacity,
                                autoCollapsePill = it
                            )
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = palette.primaryContainer,
                        checkedThumbColor = palette.onSurface,
                        uncheckedTrackColor = palette.surfaceContainerHighest,
                        uncheckedThumbColor = palette.onSurfaceVariant
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KineticTileShape)
                    .background(palette.surfaceContainerLow)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.overlayOpacityLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(settings.hudOpacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = settings.hudOpacity,
                    onValueChange = { nextOpacity ->
                        onIntent(
                            MainUiIntent.UpdateHudPreferences(
                                opacity = nextOpacity,
                                autoCollapsePill = settings.autoCollapseToEdgePill
                            )
                        )
                    },
                    valueRange = 0.45f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.primary,
                        activeTrackColor = palette.primaryContainer,
                        inactiveTrackColor = palette.surfaceContainerHighest
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AppearanceAndColorStudioCard(
    appLanguage: AppLanguage,
    themeMode: ThemeMode,
    primaryAccentHex: Long,
    secondaryAccentHex: Long,
    onIntent: (MainUiIntent) -> Unit
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current

    // 0 = Primary Accent, 1 = Secondary Accent
    var editingChannelIndex by remember { mutableIntStateOf(0) }
    val activeHex = if (editingChannelIndex == 0) primaryAccentHex else secondaryAccentHex

    var hexInputText by remember(activeHex) {
        mutableStateOf(String.format(Locale.US, "#%06X", activeHex and 0xFFFFFFL))
    }
    var isColorStudioExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, KineticComponentShape)
            .clip(KineticComponentShape)
            .background(palette.surfaceContainer)
            .border(1.dp, palette.outlineVariant.copy(alpha = 0.45f), KineticComponentShape)
            .padding(16.dp)
    ) {
        Text(
            text = strings.appearanceAndThemeSection,
            style = MaterialTheme.typography.titleMedium,
            color = palette.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        // 1. Language Selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.languageLabel,
                style = MaterialTheme.typography.labelLarge,
                color = palette.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val langOptions = listOf(
                    AppLanguage.SYSTEM to strings.langSystem,
                    AppLanguage.TR to strings.langTurkish,
                    AppLanguage.EN to strings.langEnglish
                )
                langOptions.forEach { (lang, label) ->
                    val selected = appLanguage == lang
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(CircleShape)
                            .background(if (selected) palette.primaryContainer else palette.surfaceContainerLow)
                            .border(
                                width = 1.dp,
                                color = if (selected) palette.primary else palette.outlineVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable {
                                onIntent(MainUiIntent.UpdateAppearance(appLanguage = lang))
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) palette.onPrimaryContainer else palette.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Theme Mode Selector (Default: System, Light/Beyaz, Dark/Koyu, AMOLED)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.themeModeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = palette.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val themeOptions = listOf(
                    ThemeMode.SYSTEM to strings.themeSystem,
                    ThemeMode.LIGHT to strings.themeLight,
                    ThemeMode.DARK to strings.themeDark,
                    ThemeMode.AMOLED to strings.themeAmoled
                )
                themeOptions.forEach { (mode, label) ->
                    val selected = themeMode == mode
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(CircleShape)
                            .background(if (selected) palette.primaryContainer else palette.surfaceContainerLow)
                            .border(
                                width = 1.dp,
                                color = if (selected) palette.primary else palette.outlineVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable {
                                onIntent(MainUiIntent.UpdateAppearance(themeMode = mode))
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) palette.onPrimaryContainer else palette.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. Color Spectrum Studio (Renk Tayfı / Spektrum ile Renk Seçimi)
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(KineticTileShape)
                .background(palette.surfaceContainerLow)
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(palette.primary.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = strings.customColorStudioTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.customColorStudioSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                }
            }

            // Curated Color Presets Horizontal Strip
            Text(
                text = strings.colorPresetsLabel,
                style = MaterialTheme.typography.labelMedium,
                color = palette.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                BuiltInColorPresets.presets.forEach { preset ->
                    val isPresetSelected = primaryAccentHex == preset.primaryHex &&
                        secondaryAccentHex == preset.secondaryHex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.surfaceContainerHigh)
                            .border(
                                width = if (isPresetSelected) 1.5.dp else 1.dp,
                                color = if (isPresetSelected) hexToComposeColor(preset.primaryHex) else palette.outlineVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable {
                                onIntent(
                                    MainUiIntent.UpdateAppearance(
                                        primaryAccentHex = preset.primaryHex,
                                        secondaryAccentHex = preset.secondaryHex
                                    )
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(hexToComposeColor(preset.primaryHex))
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(hexToComposeColor(preset.secondaryHex))
                        )
                        Text(
                            text = if (strings.isTurkish) preset.nameTr else preset.nameEn,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Toggle Button to Open/Close 2D Spectrum Picker Studio
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KineticTileShape)
                    .background(palette.surfaceContainerHigh)
                    .clickable { isColorStudioExpanded = !isColorStudioExpanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isColorStudioExpanded) strings.colorStudioToggleHide else strings.colorStudioToggleShow,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (isColorStudioExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = palette.onSurfaceVariant
                )
            }

            if (isColorStudioExpanded) {
                // Channel Switcher (Primary Accent vs Secondary Accent)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                val primarySelected = editingChannelIndex == 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(KineticTileShape)
                        .background(if (primarySelected) palette.surfaceContainerHigh else palette.surfaceContainerLowest)
                        .border(
                            1.5.dp,
                            if (primarySelected) palette.primary else Color.Transparent,
                            KineticTileShape
                        )
                        .clickable { editingChannelIndex = 0 }
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(palette.primary)
                    )
                    Text(
                        text = strings.primaryAccentLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }

                val secondarySelected = editingChannelIndex == 1
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(KineticTileShape)
                        .background(if (secondarySelected) palette.surfaceContainerHigh else palette.surfaceContainerLowest)
                        .border(
                            1.5.dp,
                            if (secondarySelected) palette.secondary else Color.Transparent,
                            KineticTileShape
                        )
                        .clickable { editingChannelIndex = 1 }
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(palette.secondary)
                    )
                    Text(
                        text = strings.secondaryAccentLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }
            }

            // Interactive 2D Saturation-Value + Rainbow Hue Color Spectrum Picker
            Text(
                text = strings.spectrumPickerLabel,
                style = MaterialTheme.typography.labelMedium,
                color = palette.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            ColorSpectrumPicker(
                selectedHex = activeHex,
                onColorHexSelected = { nextHex ->
                    if (editingChannelIndex == 0) {
                        onIntent(MainUiIntent.UpdateAppearance(primaryAccentHex = nextHex))
                    } else {
                        onIntent(MainUiIntent.UpdateAppearance(secondaryAccentHex = nextHex))
                    }
                }
            )

            // Custom HEX Input + Selected Swatch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.customHexInputLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(hexToComposeColor(activeHex))
                            .border(1.5.dp, palette.onSurface.copy(alpha = 0.5f), CircleShape)
                    )

                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .width(108.dp)
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceContainerLowest)
                            .border(1.dp, palette.outlineVariant, CircleShape)
                            .padding(horizontal = 12.dp)
                    ) {
                        BasicTextField(
                            value = hexInputText,
                            onValueChange = { raw ->
                                val cleaned = raw.uppercase(Locale.US)
                                    .filter { it in "0123456789ABCDEF#" }
                                    .take(7)
                                hexInputText = cleaned
                                val digits = cleaned.removePrefix("#")
                                if (digits.length == 6) {
                                    digits.toLongOrNull(16)?.let { rgb ->
                                        val fullArgb = 0xFF000000L or rgb
                                        if (editingChannelIndex == 0) {
                                            onIntent(MainUiIntent.UpdateAppearance(primaryAccentHex = fullArgb))
                                        } else {
                                            onIntent(MainUiIntent.UpdateAppearance(secondaryAccentHex = fullArgb))
                                        }
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.labelLarge.copy(
                                color = palette.onSurface,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(palette.primary)
                        )
                    }
                }
            }
            }

            // Reset Colors Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceContainerHigh)
                    .clickable {
                        onIntent(
                            MainUiIntent.UpdateAppearance(
                                primaryAccentHex = 0xFF299EECL,
                                secondaryAccentHex = 0xFF9449DBL
                            )
                        )
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = palette.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.resetColorsButton,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Interactive 2D Saturation-Brightness Spectrum Panel + 360° Rainbow Hue Spectrum Bar.
 * Uses RequestDisallowInterceptTouchEvent so parent verticalScroll never steals touch gestures.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ColorSpectrumPicker(
    selectedHex: Long,
    onColorHexSelected: (Long) -> Unit
) {
    val palette = LocalKineticPalette.current
    val currentOnColorSelected by rememberUpdatedState(onColorHexSelected)

    val initialHsv = remember(selectedHex) { argbHexToHsv(selectedHex) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    // Synchronize HSV when selectedHex changes externally (e.g. preset click or channel switch)
    LaunchedEffect(selectedHex) {
        val currentComputedHex = hsvToArgbHex(hue, saturation, value)
        if ((currentComputedHex and 0xFFFFFFL) != (selectedHex and 0xFFFFFFL)) {
            val hsv = argbHexToHsv(selectedHex)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
        }
    }

    var svBoxSize by remember { mutableStateOf(IntSize(1, 1)) }
    var hueBarSize by remember { mutableStateOf(IntSize(1, 1)) }
    val svDisallowIntercept = remember { RequestDisallowInterceptTouchEvent() }
    val hueDisallowIntercept = remember { RequestDisallowInterceptTouchEvent() }

    val pureHueColor = remember(hue) { hexToComposeColor(hsvToArgbHex(hue, 1f, 1f)) }
    val activeColor = remember(hue, saturation, value) {
        hexToComposeColor(hsvToArgbHex(hue, saturation, value))
    }

    val rainbowColors = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF00FFFF),
            Color(0xFF0000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0000)
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. 2D Saturation (X) & Brightness/Value (Y) Spectrum Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, palette.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .onSizeChanged { svBoxSize = it }
                .pointerInteropFilter(requestDisallowInterceptTouchEvent = svDisallowIntercept) { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE -> {
                            svDisallowIntercept.invoke(true)
                            val w = svBoxSize.width.toFloat().coerceAtLeast(1f)
                            val h = svBoxSize.height.toFloat().coerceAtLeast(1f)
                            saturation = (event.x / w).coerceIn(0f, 1f)
                            value = (1f - (event.y / h)).coerceIn(0f, 1f)
                            currentOnColorSelected(hsvToArgbHex(hue, saturation, value))
                            true
                        }
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            svDisallowIntercept.invoke(false)
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Horizontal White -> Pure Hue gradient
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, pureHueColor)
                    )
                )
                // Vertical Transparent -> Black gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )

                val selectorX = (saturation * size.width).coerceIn(0f, size.width)
                val selectorY = ((1f - value) * size.height).coerceIn(0f, size.height)
                val center = Offset(selectorX, selectorY)

                drawCircle(
                    color = Color.Black.copy(alpha = 0.55f),
                    radius = 12.dp.toPx(),
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 10.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = activeColor,
                    radius = 7.dp.toPx(),
                    center = center
                )
            }
        }

        // 2. 360° Full-Rainbow Hue Spectrum Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(CircleShape)
                .border(1.dp, palette.outlineVariant.copy(alpha = 0.6f), CircleShape)
                .onSizeChanged { hueBarSize = it }
                .pointerInteropFilter(requestDisallowInterceptTouchEvent = hueDisallowIntercept) { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE -> {
                            hueDisallowIntercept.invoke(true)
                            val w = hueBarSize.width.toFloat().coerceAtLeast(1f)
                            hue = ((event.x / w) * 360f).coerceIn(0f, 360f)
                            currentOnColorSelected(hsvToArgbHex(hue, saturation, value))
                            true
                        }
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            hueDisallowIntercept.invoke(false)
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.horizontalGradient(colors = rainbowColors)
                )

                val thumbX = ((hue / 360f) * size.width).coerceIn(10.dp.toPx(), size.width - 10.dp.toPx())
                val center = Offset(thumbX, size.height / 2f)

                drawCircle(
                    color = Color.Black.copy(alpha = 0.6f),
                    radius = 12.dp.toPx(),
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 10.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = pureHueColor,
                    radius = 7.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

private fun argbHexToHsv(hex: Long): FloatArray {
    val r = ((hex shr 16) and 0xFFL).toFloat() / 255f
    val g = ((hex shr 8) and 0xFFL).toFloat() / 255f
    val b = (hex and 0xFFL).toFloat() / 255f

    val maxVal = max(r, max(g, b))
    val minVal = min(r, min(g, b))
    val delta = maxVal - minVal

    var h = when {
        delta == 0f -> 0f
        maxVal == r -> 60f * (((g - b) / delta) % 6f)
        maxVal == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    if (h < 0f) h += 360f

    val s = if (maxVal == 0f) 0f else (delta / maxVal)
    val v = maxVal
    return floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
}

private fun hsvToArgbHex(h: Float, s: Float, v: Float): Long {
    val cleanH = ((h % 360f) + 360f) % 360f
    val cleanS = s.coerceIn(0f, 1f)
    val cleanV = v.coerceIn(0f, 1f)

    val c = cleanV * cleanS
    val x = c * (1f - abs(((cleanH / 60f) % 2f) - 1f))
    val m = cleanV - c

    val (rPrime, gPrime, bPrime) = when {
        cleanH < 60f -> Triple(c, x, 0f)
        cleanH < 120f -> Triple(x, c, 0f)
        cleanH < 180f -> Triple(0f, c, x)
        cleanH < 240f -> Triple(0f, x, c)
        cleanH < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val r = ((rPrime + m) * 255f).roundToInt().coerceIn(0, 255).toLong()
    val g = ((gPrime + m) * 255f).roundToInt().coerceIn(0, 255).toLong()
    val b = ((bPrime + m) * 255f).roundToInt().coerceIn(0, 255).toLong()

    return 0xFF000000L or (r shl 16) or (g shl 8) or b
}

@Composable
private fun SettingsStepperRow(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val palette = LocalKineticPalette.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(KineticTileShape)
            .background(palette.surfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = palette.onSurface,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceContainerHigh)
                    .clickable(onClick = onDecrement)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Decrease",
                    tint = palette.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                color = palette.primary,
                fontWeight = FontWeight.Bold
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceContainerHigh)
                    .clickable(onClick = onIncrement)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Increase",
                    tint = palette.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
