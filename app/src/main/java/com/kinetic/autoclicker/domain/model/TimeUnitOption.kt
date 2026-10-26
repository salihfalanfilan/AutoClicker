package com.kinetic.autoclicker.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class TimeUnitOption(
    val displayNameEn: String,
    val displayNameTr: String,
    val shortSuffixEn: String,
    val shortSuffixTr: String,
    val multiplierMs: Long
) {
    MILLISECONDS("Milliseconds", "Milisaniye", "ms", "ms", 1L),
    SECONDS("Seconds", "Saniye", "sec", "sn", 1_000L),
    MINUTES("Minutes", "Dakika", "min", "dk", 60_000L);

    val displayName: String
        get() = displayNameEn

    val shortSuffix: String
        get() = shortSuffixEn

    fun localizedName(isTurkish: Boolean): String =
        if (isTurkish) displayNameTr else displayNameEn

    fun localizedSuffix(isTurkish: Boolean): String =
        if (isTurkish) shortSuffixTr else shortSuffixEn

    fun toMilliseconds(value: Int): Long {
        val safeVal = value.coerceAtLeast(if (this == MILLISECONDS) 10 else 1)
        return safeVal.toLong() * multiplierMs
    }

    fun formatRateSummary(value: Int, isTurkish: Boolean = false): String {
        val safeVal = value.coerceAtLeast(1)
        return when (this) {
            MILLISECONDS -> {
                val perSec = 1000.0 / safeVal.toDouble()
                val formatted = if (safeVal < 100 || (1000 % safeVal != 0)) {
                    String.format(Locale.US, "%.1f", perSec)
                } else {
                    String.format(Locale.US, "%.0f", perSec)
                }
                if (isTurkish) {
                    "Saniyede $formatted tıklama"
                } else {
                    val plural = if (formatted == "1" || formatted == "1.0") "" else "s"
                    "$formatted click$plural per second"
                }
            }
            SECONDS -> {
                if (isTurkish) {
                    "Her $safeVal saniyede 1 tıklama"
                } else {
                    val plural = if (safeVal == 1) "" else "s"
                    "1 click every $safeVal second$plural"
                }
            }
            MINUTES -> {
                if (isTurkish) {
                    "Her $safeVal dakikada 1 tıklama"
                } else {
                    val plural = if (safeVal == 1) "" else "s"
                    "1 click every $safeVal minute$plural"
                }
            }
        }
    }

    fun formatTappingRhythm(value: Int, isTurkish: Boolean = false): String {
        val ms = toMilliseconds(value).coerceAtLeast(10L)
        val tapsPerSec = 1000.0 / ms.toDouble()
        return if (isTurkish) {
            val label = when {
                ms <= 150L -> "Hızlı Döngü"
                ms <= 500L -> "Dengeli Ritim"
                else -> "Zamanlı Tempo"
            }
            String.format(Locale.US, "%s • %.1f tık / sn", label, tapsPerSec)
        } else {
            val label = when {
                ms <= 150L -> "Rapid Cycle"
                ms <= 500L -> "Steady Pulse"
                else -> "Timed Cadence"
            }
            String.format(Locale.US, "%s • %.1f taps / sec", label, tapsPerSec)
        }
    }
}
