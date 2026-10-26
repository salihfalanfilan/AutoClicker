package com.kinetic.autoclicker.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ActionType(val label: String, val labelTr: String) {
    TAP("Point Tap", "Nokta Tıklama"),
    SWIPE("Swipe Gesture", "Kaydırma Hareketi"),
    LONG_PRESS("Long Press", "Basılı Tutma");

    fun localizedLabel(isTurkish: Boolean): String = if (isTurkish) labelTr else label
}

@Serializable
enum class AutomationMode(val title: String, val badge: String) {
    SINGLE_TARGET("Single Target Mode", "Point Tap"),
    MULTI_TARGET("Multi Target Mode", "Sequence")
}

@Serializable
enum class HudDisplayMode {
    HIDDEN,
    EXPANDED_BAR,
    EDGE_PILL
}
