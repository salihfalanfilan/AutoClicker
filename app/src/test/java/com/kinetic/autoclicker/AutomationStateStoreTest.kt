package com.kinetic.autoclicker

import androidx.compose.ui.graphics.luminance
import com.kinetic.autoclicker.domain.engine.AutomationStateStore
import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.domain.model.AppLanguage
import com.kinetic.autoclicker.domain.model.AutomationMode
import com.kinetic.autoclicker.domain.model.EngineSettings
import com.kinetic.autoclicker.domain.model.HudDisplayMode
import com.kinetic.autoclicker.domain.model.StopCondition
import com.kinetic.autoclicker.domain.model.StopConditionType
import com.kinetic.autoclicker.domain.model.ThemeMode
import com.kinetic.autoclicker.domain.model.TimeUnitOption
import com.kinetic.autoclicker.presentation.theme.buildKineticPalette
import com.kinetic.autoclicker.presentation.theme.hexToComposeColor
import com.kinetic.autoclicker.service.GestureExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AutomationStateStoreTest {

    @Test
    fun `buildKineticPalette constructs valid sRGB colors without crashing on luminance`() {
        val darkPalette = buildKineticPalette(
            isLight = false,
            isAmoled = false,
            primaryHex = 0xFF4BE277L,
            secondaryHex = 0xFF3B82F6L
        )
        assertTrue(darkPalette.primary.luminance() > 0f)
        assertTrue(darkPalette.secondary.luminance() > 0f)

        val amoledPalette = buildKineticPalette(
            isLight = false,
            isAmoled = true,
            primaryHex = 0xFFFF2E93L,
            secondaryHex = 0xFF00E5FFL
        )
        assertEquals(0f, amoledPalette.background.luminance(), 0.0001f)
        assertTrue(amoledPalette.primary.luminance() > 0f)

        val lightPalette = buildKineticPalette(
            isLight = true,
            isAmoled = false,
            primaryHex = 0xFF22C55EL,
            secondaryHex = 0xFF0566D9L
        )
        assertTrue(lightPalette.background.luminance() > 0.8f)
        assertEquals(hexToComposeColor(0xFF22C55EL), lightPalette.primary)
    }

    @Test
    fun `system language resolves Turkish for tr and az and English for other locales`() {
        assertTrue(AppLanguage.SYSTEM.resolveIsTurkish(Locale("tr", "TR")))
        assertTrue(AppLanguage.SYSTEM.resolveIsTurkish(Locale("az", "AZ")))
        assertFalse(AppLanguage.SYSTEM.resolveIsTurkish(Locale("en", "US")))
        assertFalse(AppLanguage.SYSTEM.resolveIsTurkish(Locale("de", "DE")))
        assertFalse(AppLanguage.SYSTEM.resolveIsTurkish(Locale("fr", "FR")))

        assertTrue(AppLanguage.TR.resolveIsTurkish(Locale("en", "US")))
        assertFalse(AppLanguage.EN.resolveIsTurkish(Locale("tr", "TR")))
    }

    @Test
    fun `default theme is SYSTEM and custom colors and AMOLED theme apply cleanly`() {
        val defaults = EngineSettings()
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
        assertEquals(AppLanguage.SYSTEM, defaults.appLanguage)

        val store = AutomationStateStore()
        val customSettings = defaults.copy(
            appLanguage = AppLanguage.TR,
            themeMode = ThemeMode.AMOLED,
            primaryAccentHex = 0xFFFF2E93L,
            secondaryAccentHex = 0xFF00E5FFL
        )
        store.applyGlobalSettings(customSettings)

        val active = store.state.value.globalSettings
        assertEquals(ThemeMode.AMOLED, active.themeMode)
        assertEquals(AppLanguage.TR, active.appLanguage)
        assertEquals(0xFFFF2E93L, active.primaryAccentHex)
        assertEquals(0xFF00E5FFL, active.secondaryAccentHex)
    }

    @Test
    fun `timeUnitOption converts and formats rate summary accurately in EN and TR`() {
        assertEquals(100L, TimeUnitOption.MILLISECONDS.toMilliseconds(100))
        assertEquals("10 clicks per second", TimeUnitOption.MILLISECONDS.formatRateSummary(100, isTurkish = false))
        assertEquals("Saniyede 10 tıklama", TimeUnitOption.MILLISECONDS.formatRateSummary(100, isTurkish = true))
        assertEquals("8.3 clicks per second", TimeUnitOption.MILLISECONDS.formatRateSummary(120, isTurkish = false))
        assertEquals("1 click every 5 seconds", TimeUnitOption.SECONDS.formatRateSummary(5, isTurkish = false))
        assertEquals("Her 5 saniyede 1 tıklama", TimeUnitOption.SECONDS.formatRateSummary(5, isTurkish = true))
    }

    @Test
    fun `gestureExecutor normalizes zero or negative duration to at least 10ms`() {
        assertEquals(10L, GestureExecutor.normalizeStrokeDurationMs(0L))
        assertEquals(10L, GestureExecutor.normalizeStrokeDurationMs(-50L))
        assertEquals(45L, GestureExecutor.normalizeStrokeDurationMs(45L))
    }

    @Test
    fun `screen rotation remaps target coordinates proportionally`() {
        val store = AutomationStateStore()
        store.onScreenConfigurationChanged(1000, 2000)

        val targetId = store.state.value.targets.first().id
        store.updateTargetPosition(targetId, newX = 500f, newY = 1000f)

        val updated = store.state.value.targets.first()
        assertEquals(0.5f, updated.normalizedX, 0.001f)
        assertEquals(0.5f, updated.normalizedY, 0.001f)

        // Rotate to landscape (2000 x 1000)
        store.onScreenConfigurationChanged(2000, 1000)
        val rotated = store.state.value.targets.first()
        assertEquals(1000f, rotated.x, 0.5f)
        assertEquals(500f, rotated.y, 0.5f)
    }

    @Test
    fun `emergencyStop immediately halts running automation`() {
        val store = AutomationStateStore()
        store.setRunning(true)
        assertTrue(store.state.value.isRunning)

        store.emergencyStop("Emergency Stop: Volume key pressed")
        assertFalse(store.state.value.isRunning)
    }

    @Test
    fun `stateStore manages overlay modes, target pins, and click settings`() {
        val store = AutomationStateStore()

        store.enableOverlayMode(AutomationMode.SINGLE_TARGET)
        assertTrue(store.state.value.isOverlayVisible)
        assertEquals(HudDisplayMode.EXPANDED_BAR, store.state.value.hudDisplayMode)
        assertEquals(1, store.state.value.targets.size)

        store.addTarget(ActionType.TAP)
        assertEquals(AutomationMode.MULTI_TARGET, store.state.value.activeMode)
        assertEquals(2, store.state.value.targets.size)
        assertEquals(2, store.state.value.targets.last().orderIndex)

        store.setHudDisplayMode(HudDisplayMode.EDGE_PILL)
        assertEquals(HudDisplayMode.EDGE_PILL, store.state.value.hudDisplayMode)

        store.openClickSettingsDialog(null)
        assertTrue(store.state.value.isClickSettingsDialogVisible)

        store.saveClickSettings(
            intervalValue = 250,
            timeUnit = TimeUnitOption.MILLISECONDS,
            stopCondition = StopCondition(StopConditionType.CYCLE_COUNT, timerMinutes = 5, cycleCount = 100)
        )
        assertFalse(store.state.value.isClickSettingsDialogVisible)
        assertEquals(250, store.state.value.globalSettings.intervalValue)
        assertEquals(StopConditionType.CYCLE_COUNT, store.state.value.globalSettings.stopCondition.type)

        store.disableOverlayAndStop()
        assertFalse(store.state.value.isOverlayVisible)
        assertFalse(store.state.value.isRunning)
        assertEquals(HudDisplayMode.HIDDEN, store.state.value.hudDisplayMode)
    }

    @Test
    fun `target position is preserved on enableOverlayMode and locked while running`() {
        val store = AutomationStateStore()
        val firstId = store.state.value.targets.first().id

        // User moves target #1 to (220f, 340f) before starting overlay
        store.updateTargetPosition(firstId, newX = 220f, newY = 340f)
        assertEquals(220f, store.state.value.targets.first().x, 0.01f)
        assertEquals(340f, store.state.value.targets.first().y, 0.01f)

        // Enabling overlay mode must NOT reset the user's target position
        store.enableOverlayMode()
        assertEquals(220f, store.state.value.targets.first().x, 0.01f)
        assertEquals(340f, store.state.value.targets.first().y, 0.01f)

        // While running, simulated clicks must NOT move the target position
        store.setRunning(true)
        store.updateTargetPosition(firstId, newX = 800f, newY = 900f)
        assertEquals(220f, store.state.value.targets.first().x, 0.01f)
        assertEquals(340f, store.state.value.targets.first().y, 0.01f)
    }

    @Test
    fun `swipe target supports independent start and end point dragging and add target dialog`() {
        val store = AutomationStateStore()

        store.openAddTargetDialog()
        assertTrue(store.state.value.isAddTargetDialogVisible)

        store.addTarget(ActionType.SWIPE)
        assertFalse(store.state.value.isAddTargetDialogVisible)
        assertEquals(2, store.state.value.targets.size)

        val swipeTarget = store.state.value.targets.last()
        assertEquals(ActionType.SWIPE, swipeTarget.actionType)

        // Move start point only
        store.updateTargetPosition(swipeTarget.id, newX = 300f, newY = 600f)
        val afterStartMove = store.state.value.targets.last()
        assertEquals(300f, afterStartMove.x, 0.01f)
        assertEquals(600f, afterStartMove.y, 0.01f)

        // Move end point independently
        store.updateSwipeEndPosition(swipeTarget.id, endX = 750f, endY = 250f)
        val afterEndMove = store.state.value.targets.last()
        assertEquals(300f, afterEndMove.x, 0.01f)
        assertEquals(600f, afterEndMove.y, 0.01f)
        assertEquals(750f, afterEndMove.endX ?: 0f, 0.01f)
        assertEquals(250f, afterEndMove.endY ?: 0f, 0.01f)
    }

    @Test
    fun `start overlay chooser toggles dialog and initializes requested TAP or SWIPE target`() {
        val store = AutomationStateStore()

        store.openStartChooserDialog()
        assertTrue(store.state.value.isStartChooserDialogVisible)

        store.dismissStartChooserDialog()
        assertFalse(store.state.value.isStartChooserDialogVisible)

        // Starting overlay with SWIPE must initialize single swipe target with valid start & end
        store.startOverlayWithInitialTarget(ActionType.SWIPE)
        assertTrue(store.state.value.isOverlayVisible)
        assertEquals(HudDisplayMode.EXPANDED_BAR, store.state.value.hudDisplayMode)
        assertFalse(store.state.value.isStartChooserDialogVisible)
        assertEquals(1, store.state.value.targets.size)

        val initialSwipe = store.state.value.targets.first()
        assertEquals(ActionType.SWIPE, initialSwipe.actionType)
        assertTrue((initialSwipe.endX ?: 0f) > 0f)
        assertTrue((initialSwipe.endY ?: 0f) > 0f)
        assertTrue(initialSwipe.y != initialSwipe.endY)

        // Starting overlay with TAP must initialize single tap target
        store.startOverlayWithInitialTarget(ActionType.TAP)
        assertEquals(1, store.state.value.targets.size)
        val initialTap = store.state.value.targets.first()
        assertEquals(ActionType.TAP, initialTap.actionType)
    }
}
