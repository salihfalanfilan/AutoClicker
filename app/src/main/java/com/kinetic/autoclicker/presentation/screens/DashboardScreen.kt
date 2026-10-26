package com.kinetic.autoclicker.presentation.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.presentation.contract.MainUiIntent
import com.kinetic.autoclicker.presentation.contract.MainUiState
import com.kinetic.autoclicker.presentation.contract.NavigationTab
import com.kinetic.autoclicker.presentation.theme.KineticComponentShape
import com.kinetic.autoclicker.presentation.theme.KineticTileShape
import com.kinetic.autoclicker.presentation.theme.LocalAppStrings
import com.kinetic.autoclicker.presentation.theme.LocalKineticPalette

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    onIntent: (MainUiIntent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val strings = LocalAppStrings.current
    val palette = LocalKineticPalette.current
    val engineState = uiState.engineState
    val settings = engineState.globalSettings
    val isOverlayActive = engineState.isOverlayVisible

    val tapCount = engineState.targets.count { it.actionType != ActionType.SWIPE }
    val swipeCount = engineState.targets.count { it.actionType == ActionType.SWIPE }
    val targetsSummary = if (strings.isTurkish) {
        "$tapCount tıklama · $swipeCount kaydırma"
    } else {
        "$tapCount tap${if (tapCount == 1) "" else "s"} · $swipeCount swipe${if (swipeCount == 1) "" else "s"}"
    }
    val activeProfile = engineState.activeProfile ?: uiState.profiles.firstOrNull()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Eksik İzin Uyarısı (Yalnızca izin verilmemişse görünür)
        AnimatedVisibility(
            visible = !engineState.isAccessibilityServiceConnected || !engineState.isOverlayPermissionGranted
        ) {
            val missingAccessibility = !engineState.isAccessibilityServiceConnected
            PermissionAlertCard(
                title = if (missingAccessibility) strings.accessibilityRequiredTitle else strings.overlayRequiredTitle,
                subtitle = if (missingAccessibility) {
                    strings.accessibilityRequiredSubtitle
                } else {
                    strings.overlayRequiredSubtitle
                },
                onEnableClick = {
                    if (missingAccessibility) {
                        onIntent(MainUiIntent.RequestAccessibilityPermission)
                    } else {
                        onIntent(MainUiIntent.RequestOverlayPermission)
                    }
                }
            )
        }

        // Ana Kontrol Merkezi (Minimalist & İnsan Eliyle Tasarlanmış)
        MainControlHeroCard(
            isOverlayActive = isOverlayActive,
            isRunning = engineState.isRunning,
            totalClicks = engineState.totalClicksExecuted,
            onToggleOverlay = { onIntent(MainUiIntent.ToggleOverlayController) }
        )

        // Hızlı Ayarlar Kartı (Aralık, Süre ve Koşul)
        QuickParametersCard(
            intervalText = "${settings.intervalValue} ${settings.timeUnit.localizedSuffix(strings.isTurkish)}",
            swipeDurationText = "${settings.swipeDurationMs} ms",
            stopConditionText = settings.stopCondition.summaryLabel(strings.isTurkish),
            onConfigureClick = {
                onIntent(MainUiIntent.OpenClickSettings(engineState.targets.firstOrNull()?.id))
            }
        )

        // Hedefler ve Aksiyon Yönetimi (Tıklama / Kaydırma Ekleme)
        TargetsManagementCard(
            targetsSummary = targetsSummary,
            targetCount = engineState.targets.size,
            onAddTarget = { onIntent(MainUiIntent.OpenAddTargetChooser) },
            onRemoveLastTarget = { onIntent(MainUiIntent.RemoveLastTarget) },
            onConfigureTargets = {
                onIntent(MainUiIntent.OpenClickSettings(engineState.targets.firstOrNull()?.id))
            }
        )

        // Profil & Senaryo Seçici
        MacroProfileCard(
            profileTitle = activeProfile?.name ?: "Varsayılan Senaryo",
            profileSubtitle = activeProfile?.summarySubtitle(strings.isTurkish) ?: targetsSummary,
            onSelectScripts = { onIntent(MainUiIntent.SelectTab(NavigationTab.SCRIPTS)) },
            onCycleProfile = {
                if (uiState.profiles.isNotEmpty()) {
                    val idx = uiState.profiles.indexOfFirst { it.id == activeProfile?.id }
                    val next = uiState.profiles[(idx + 1).coerceAtLeast(0) % uiState.profiles.size]
                    onIntent(MainUiIntent.SelectScriptProfile(next))
                } else {
                    onIntent(MainUiIntent.SelectTab(NavigationTab.SCRIPTS))
                }
            }
        )

        // Basit ve Şık Acil Durdurma İpucu
        HelpfulTipsBox()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Ana Kontrol Kartı: Sahte grafikler yerine net, sade ve güven veren bir kontrol merkezi.
 */
@Composable
private fun MainControlHeroCard(
    isOverlayActive: Boolean,
    isRunning: Boolean,
    totalClicks: Long,
    onToggleOverlay: () -> Unit
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, KineticComponentShape)
            .clip(KineticComponentShape)
            .background(palette.surfaceContainer)
            .border(1.dp, palette.outlineVariant.copy(alpha = 0.40f), KineticComponentShape)
            .padding(20.dp)
    ) {
        // Başlık ve Durum Rozeti
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = strings.unifiedControllerTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isOverlayActive) {
                        if (isRunning) strings.runningClicksFormat(totalClicks) else strings.overlayActiveOnScreen
                    } else {
                        strings.readyForCapture
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.onSurfaceVariant
                )
            }

            // Durum Göstergesi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isOverlayActive) {
                            if (isRunning) palette.primary.copy(alpha = 0.15f) else palette.secondary.copy(alpha = 0.15f)
                        } else {
                            palette.surfaceContainerHigh
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOverlayActive) {
                                if (isRunning) palette.primary else palette.secondary
                            } else {
                                palette.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                )
                Text(
                    text = when {
                        !isOverlayActive -> strings.statusReady
                        isRunning -> strings.statusActive
                        else -> "Panel Açık"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isOverlayActive) palette.onSurface else palette.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Büyük, Ergonomik "YÜZEN PANELİ BAŞLAT / DURDUR" Butonu
        val btnBg = if (isOverlayActive) palette.errorContainer else palette.primaryContainer
        val btnContent = if (isOverlayActive) palette.onErrorContainer else palette.onPrimaryContainer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = if (isOverlayActive) 2.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = btnBg,
                    spotColor = btnBg
                )
                .clip(CircleShape)
                .background(btnBg)
                .clickable(onClick = onToggleOverlay)
        ) {
            Icon(
                imageVector = if (isOverlayActive) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = btnContent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isOverlayActive) strings.disableAction else strings.enableAction,
                style = MaterialTheme.typography.titleMedium,
                color = btnContent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Hızlı Parametreler: Tıklama aralığı, kaydırma süresi ve durma koşulunu net biçimde gösterir.
 */
@Composable
private fun QuickParametersCard(
    intervalText: String,
    swipeDurationText: String,
    stopConditionText: String,
    onConfigureClick: () -> Unit
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, KineticComponentShape)
            .clip(KineticComponentShape)
            .background(palette.surfaceContainer)
            .border(1.dp, palette.outlineVariant.copy(alpha = 0.35f), KineticComponentShape)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Zamanlama & Ayarlar",
                style = MaterialTheme.typography.titleMedium,
                color = palette.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onConfigureClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Düzenle",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Tıklama Aralığı
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(KineticTileShape)
                    .background(palette.surfaceContainerLow)
                    .clickable(onClick = onConfigureClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = strings.tapIntervalLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = intervalText,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            // Kaydırma Süresi
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(KineticTileShape)
                    .background(palette.surfaceContainerLow)
                    .clickable(onClick = onConfigureClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Kaydırma Hızı",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = swipeDurationText,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            // Durma Koşulu
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(KineticTileShape)
                    .background(palette.surfaceContainerLow)
                    .clickable(onClick = onConfigureClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = strings.stopConditionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stopConditionText,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Hedefler ve Aksiyon Yönetimi: Tekli / çoklu fark etmeksizin tüm hedefleri temiz bir liste halinde sunar.
 */
@Composable
private fun TargetsManagementCard(
    targetsSummary: String,
    targetCount: Int,
    onAddTarget: () -> Unit,
    onRemoveLastTarget: () -> Unit,
    onConfigureTargets: () -> Unit
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, KineticComponentShape)
            .clip(KineticComponentShape)
            .background(palette.surfaceContainer)
            .border(1.dp, palette.outlineVariant.copy(alpha = 0.35f), KineticComponentShape)
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
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(palette.primary.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Gesture,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aktif Hedef Noktaları",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = targetsSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (targetCount > 1) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceContainerHigh)
                        .clickable(onClick = onRemoveLastTarget)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Remove target",
                        tint = palette.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(palette.primaryContainer)
                    .clickable(onClick = onAddTarget)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add target",
                    tint = palette.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Profil & Senaryo Kartı: Kayıtlı makrolara hızlı erişim sağlar.
 */
@Composable
private fun MacroProfileCard(
    profileTitle: String,
    profileSubtitle: String,
    onSelectScripts: () -> Unit,
    onCycleProfile: () -> Unit
) {
    val palette = LocalKineticPalette.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, KineticComponentShape)
            .clip(KineticComponentShape)
            .background(palette.surfaceContainer)
            .border(1.dp, palette.outlineVariant.copy(alpha = 0.35f), KineticComponentShape)
            .clickable(onClick = onSelectScripts)
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
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(palette.secondary.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Reorder,
                    contentDescription = null,
                    tint = palette.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profileTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = profileSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(palette.surfaceContainerHigh)
                .clickable(onClick = onCycleProfile)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Switch profile",
                tint = palette.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * İzin Uyarısı: Sadece eksik sistem izni olduğunda sade ve kibar bir şekilde uyarır.
 */
@Composable
private fun PermissionAlertCard(
    title: String,
    subtitle: String,
    onEnableClick: () -> Unit
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(KineticComponentShape)
            .background(palette.surfaceContainerHigh)
            .border(1.dp, palette.tertiary.copy(alpha = 0.35f), KineticComponentShape)
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(palette.tertiaryContainer.copy(alpha = 0.30f))
        ) {
            Icon(
                imageVector = Icons.Rounded.AccessibilityNew,
                contentDescription = null,
                tint = palette.tertiary,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = palette.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = palette.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(CircleShape)
                .background(palette.primaryContainer)
                .clickable(onClick = onEnableClick)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = strings.enableButton,
                style = MaterialTheme.typography.labelMedium,
                color = palette.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Acil Durdurma İpucu
 */
@Composable
private fun HelpfulTipsBox() {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(KineticComponentShape)
            .background(palette.surfaceContainerLow)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Lightbulb,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = strings.helpfulTipText,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.onSurfaceVariant
        )
    }
}
