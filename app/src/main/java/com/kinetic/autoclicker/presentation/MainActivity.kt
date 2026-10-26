package com.kinetic.autoclicker.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.presentation.components.AddTargetTypeModalOverlay
import com.kinetic.autoclicker.presentation.components.ClickSettingsModalOverlay
import com.kinetic.autoclicker.presentation.contract.MainUiEffect
import com.kinetic.autoclicker.presentation.contract.MainUiIntent
import com.kinetic.autoclicker.presentation.contract.NavigationTab
import com.kinetic.autoclicker.presentation.screens.DashboardScreen
import com.kinetic.autoclicker.presentation.screens.ScriptsScreen
import com.kinetic.autoclicker.presentation.screens.SettingsScreen
import com.kinetic.autoclicker.presentation.screens.TargetsOverlayScreen
import com.kinetic.autoclicker.presentation.theme.KineticAutoClickerTheme
import com.kinetic.autoclicker.presentation.theme.KineticBackground
import com.kinetic.autoclicker.presentation.theme.LocalAppStrings
import com.kinetic.autoclicker.presentation.theme.LocalKineticPalette
import com.kinetic.autoclicker.presentation.viewmodel.MainViewModel
import com.kinetic.autoclicker.service.ClickAccessibilityService
import com.kinetic.autoclicker.service.FloatingHudService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(KineticBackground.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(KineticBackground.toArgb())
        )

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val globalSettings = uiState.engineState.globalSettings

            KineticAutoClickerTheme(settings = globalSettings) {
                val palette = LocalKineticPalette.current
                val strings = LocalAppStrings.current
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(palette.isLight, palette.background) {
                    val barColor = palette.background.toArgb()
                    if (palette.isLight) {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.light(barColor, barColor),
                            navigationBarStyle = SystemBarStyle.light(barColor, barColor)
                        )
                    } else {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.dark(barColor),
                            navigationBarStyle = SystemBarStyle.dark(barColor)
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            MainUiEffect.OpenAccessibilitySettings -> {
                                openAccessibilitySettings()
                            }
                            MainUiEffect.OpenOverlaySettings -> {
                                openOverlayPermissionSettings()
                            }
                            MainUiEffect.StartFloatingHudService -> {
                                if (Settings.canDrawOverlays(this@MainActivity)) {
                                    FloatingHudService.startService(this@MainActivity)
                                } else {
                                    openOverlayPermissionSettings()
                                }
                            }
                            MainUiEffect.StopFloatingHudService -> {
                                FloatingHudService.stopService(this@MainActivity)
                            }
                            is MainUiEffect.ShowMessage -> {
                                snackbarHostState.showSnackbar(effect.message)
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = palette.surface,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            KineticTopBar(
                                title = when (uiState.selectedTab) {
                                    NavigationTab.AUTOMATE -> strings.dashboardTitle
                                    NavigationTab.TARGETS -> strings.activeOverlayTitle
                                    NavigationTab.SCRIPTS -> strings.scriptEditorTitle
                                    NavigationTab.SETTINGS -> strings.engineSettingsTitle
                                }
                            )
                        },
                        bottomBar = {
                            KineticBottomNavigationBar(
                                selectedTab = uiState.selectedTab,
                                onSelectTab = { tab ->
                                    viewModel.onIntent(MainUiIntent.SelectTab(tab))
                                }
                            )
                        }
                    ) { innerPadding ->
                        when (uiState.selectedTab) {
                            NavigationTab.AUTOMATE -> DashboardScreen(
                                uiState = uiState,
                                onIntent = viewModel::onIntent,
                                contentPadding = innerPadding
                            )
                            NavigationTab.TARGETS -> TargetsOverlayScreen(
                                uiState = uiState,
                                onIntent = viewModel::onIntent,
                                contentPadding = innerPadding
                            )
                            NavigationTab.SCRIPTS -> ScriptsScreen(
                                uiState = uiState,
                                onIntent = viewModel::onIntent,
                                contentPadding = innerPadding
                            )
                            NavigationTab.SETTINGS -> SettingsScreen(
                                uiState = uiState,
                                onIntent = viewModel::onIntent,
                                contentPadding = innerPadding
                            )
                        }
                    }

                    val engineState = uiState.engineState
                    if (engineState.isClickSettingsDialogVisible && !engineState.isOverlayVisible) {
                        val editingTarget = engineState.editingTarget
                        val intervalVal = editingTarget?.intervalValue ?: engineState.globalSettings.intervalValue
                        val timeUnitVal = editingTarget?.timeUnit ?: engineState.globalSettings.timeUnit
                        val stopCondVal = editingTarget?.stopCondition ?: engineState.globalSettings.stopCondition

                        ClickSettingsModalOverlay(
                            initialIntervalValue = intervalVal,
                            initialTimeUnit = timeUnitVal,
                            initialStopCondition = stopCondVal,
                            initialActionType = editingTarget?.actionType ?: ActionType.TAP,
                            initialSwipeDurationMs = editingTarget?.swipeDurationMs ?: engineState.globalSettings.swipeDurationMs,
                            initialHoldBeforeSwipeMs = editingTarget?.holdBeforeSwipeMs ?: 80L,
                            targetSubtitle = editingTarget?.let { "#${it.orderIndex} • ${it.label}" },
                            onDeleteTarget = editingTarget?.let { target ->
                                {
                                    viewModel.onIntent(MainUiIntent.RemoveTargetById(target.id))
                                }
                            },
                            onDismiss = {
                                viewModel.onIntent(MainUiIntent.DismissClickSettings)
                            },
                            onSaveAndApply = { newInterval, newUnit, newStop, newActionType, newSwipeDuration, newHoldBeforeSwipe ->
                                viewModel.onIntent(
                                    MainUiIntent.SaveClickSettings(
                                        intervalValue = newInterval,
                                        timeUnit = newUnit,
                                        stopCondition = newStop,
                                        actionType = newActionType,
                                        swipeDurationMs = newSwipeDuration,
                                        holdBeforeSwipeMs = newHoldBeforeSwipe
                                    )
                                )
                            }
                        )
                    }

                    if (engineState.isAddTargetDialogVisible && !engineState.isOverlayVisible) {
                        AddTargetTypeModalOverlay(
                            onSelectTap = {
                                viewModel.onIntent(MainUiIntent.AddTarget(ActionType.TAP))
                            },
                            onSelectSwipe = {
                                viewModel.onIntent(MainUiIntent.AddTarget(ActionType.SWIPE))
                            },
                            onDismiss = {
                                viewModel.onIntent(MainUiIntent.DismissAddTargetChooser)
                            }
                        )
                    }

                    if (engineState.isStartChooserDialogVisible && !engineState.isOverlayVisible) {
                        AddTargetTypeModalOverlay(
                            title = strings.startOverlayChooserTitle,
                            subtitle = strings.startOverlayChooserSubtitle,
                            onSelectTap = {
                                viewModel.onIntent(MainUiIntent.StartOverlayWithTargetType(ActionType.TAP))
                            },
                            onSelectSwipe = {
                                viewModel.onIntent(MainUiIntent.StartOverlayWithTargetType(ActionType.SWIPE))
                            },
                            onDismiss = {
                                viewModel.onIntent(MainUiIntent.DismissStartChooser)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSystemPermissionStates()
    }

    private fun refreshSystemPermissionStates() {
        val isAccessibilityEnabled = ClickAccessibilityService.isConnected() ||
            isAccessibilityServiceEnabledInSettings(this, ClickAccessibilityService::class.java)
        val isOverlayGranted = Settings.canDrawOverlays(this)

        viewModel.onIntent(
            MainUiIntent.RefreshPermissions(
                isAccessibilityEnabled = isAccessibilityEnabled,
                isOverlayGranted = isOverlayGranted
            )
        )
    }

    private fun openAccessibilitySettings() {
        runCatching {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun openOverlayPermissionSettings() {
        runCatching {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    companion object {
        fun isAccessibilityServiceEnabledInSettings(
            context: Context,
            serviceClass: Class<*>
        ): Boolean {
            val expectedComponentName = ComponentName(context, serviceClass)
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return enabledServicesSetting.split(':')
                .mapNotNull { ComponentName.unflattenFromString(it) }
                .any { it == expectedComponentName }
        }
    }
}

@Composable
private fun KineticTopBar(title: String) {
    val palette = LocalKineticPalette.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface.copy(alpha = 0.92f))
            .statusBarsPadding()
            .height(58.dp)
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = palette.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun KineticBottomNavigationBar(
    selectedTab: NavigationTab,
    onSelectTab: (NavigationTab) -> Unit
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface.copy(alpha = 0.94f))
            .navigationBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp)
        ) {
            BottomNavItem(
                label = strings.navAutomate,
                icon = Icons.Rounded.SmartToy,
                selected = selectedTab == NavigationTab.AUTOMATE,
                onClick = { onSelectTab(NavigationTab.AUTOMATE) }
            )
            BottomNavItem(
                label = strings.navTargets,
                icon = Icons.Rounded.AdsClick,
                selected = selectedTab == NavigationTab.TARGETS,
                onClick = { onSelectTab(NavigationTab.TARGETS) }
            )
            BottomNavItem(
                label = strings.navScripts,
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                selected = selectedTab == NavigationTab.SCRIPTS,
                onClick = { onSelectTab(NavigationTab.SCRIPTS) }
            )
            BottomNavItem(
                label = strings.navSettings,
                icon = Icons.Rounded.Settings,
                selected = selectedTab == NavigationTab.SETTINGS,
                onClick = { onSelectTab(NavigationTab.SETTINGS) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalKineticPalette.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(76.dp)
            .height(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(CircleShape)
                .background(
                    if (selected) palette.primaryContainer.copy(alpha = 0.22f) else Color.Transparent
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) palette.primary else palette.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) palette.primary else palette.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
