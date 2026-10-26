package com.kinetic.autoclicker.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class AppLanguage {
    SYSTEM,
    TR,
    EN;

    /**
     * Varsayılan sistem dili Türkçe ("tr") veya Azerbaycanca ("az") ise Türkçe (true),
     * başka bir dilse İngilizce (false) döndürür.
     */
    fun resolveIsTurkish(defaultLocale: Locale = Locale.getDefault()): Boolean {
        return when (this) {
            TR -> true
            EN -> false
            SYSTEM -> {
                val lang = defaultLocale.language.lowercase(Locale.ROOT)
                lang == "tr" || lang == "az" || lang == "aze" || lang == "tur"
            }
        }
    }
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

data class ColorPresetItem(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val primaryHex: Long,
    val secondaryHex: Long
)

object BuiltInColorPresets {
    val presets = listOf(
        ColorPresetItem(
            id = "kinetic_modern",
            nameTr = "Modern Mavi & Mor (Orijinal)",
            nameEn = "Modern Blue & Purple (Original)",
            primaryHex = 0xFF299EECL,
            secondaryHex = 0xFF9449DBL
        ),
        ColorPresetItem(
            id = "soft_sage",
            nameTr = "Adaçayı Yeşili",
            nameEn = "Soft Sage",
            primaryHex = 0xFFCEE3D4L,
            secondaryHex = 0xFF3B82F6L
        ),
        ColorPresetItem(
            id = "cyber_magenta",
            nameTr = "Siber Macenta",
            nameEn = "Cyber Magenta",
            primaryHex = 0xFFFF2E93L,
            secondaryHex = 0xFF00E5FFL
        ),
        ColorPresetItem(
            id = "electric_cyan",
            nameTr = "Elektrik Siyan",
            nameEn = "Electric Cyan",
            primaryHex = 0xFF00E5FFL,
            secondaryHex = 0xFF8B5CF6L
        ),
        ColorPresetItem(
            id = "solar_amber",
            nameTr = "Solar Kehribar",
            nameEn = "Solar Amber",
            primaryHex = 0xFFFFB300L,
            secondaryHex = 0xFFEF4444L
        ),
        ColorPresetItem(
            id = "royal_violet",
            nameTr = "Kraliyet Moru",
            nameEn = "Royal Violet",
            primaryHex = 0xFFA855F7L,
            secondaryHex = 0xFF38BDF8L
        ),
        ColorPresetItem(
            id = "crimson_pulse",
            nameTr = "Kızıl Nabız",
            nameEn = "Crimson Pulse",
            primaryHex = 0xFFF43F5EL,
            secondaryHex = 0xFFF59E0BL
        )
    )
}
