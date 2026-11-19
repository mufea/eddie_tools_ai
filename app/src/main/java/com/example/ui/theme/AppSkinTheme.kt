package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * AppSkinTheme definition with distinct visual identities and color palettes.
 */
enum class AppSkinTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val primaryColor: Color,
    val surfaceColor: Color,
    val isDark: Boolean
) {
    DEFAULT_SLATE(
        id = "slate",
        title = "Slate Clean",
        subtitle = "Minimalist putih & slate elegan",
        iconEmoji = "⚪",
        primaryColor = Color(0xFF0F172A),
        surfaceColor = Color(0xFFFFFFFF),
        isDark = false
    ),
    MIDNIGHT_CYBER(
        id = "midnight_cyber",
        title = "Midnight Cyber",
        subtitle = "Gelap pekat dengan aksen cyan neon",
        iconEmoji = "🌌",
        primaryColor = Color(0xFF06B6D4),
        surfaceColor = Color(0xFF0F172A),
        isDark = true
    ),
    EMERALD_MINT(
        id = "emerald_mint",
        title = "Emerald Forest",
        subtitle = "Hijau zamrud segar & menyejukkan",
        iconEmoji = "🍃",
        primaryColor = Color(0xFF059669),
        surfaceColor = Color(0xFFF0FDF4),
        isDark = false
    ),
    ROYAL_AMETHYST(
        id = "royal_amethyst",
        title = "Royal Amethyst",
        subtitle = "Ungu pekat mewah & berkarakter",
        iconEmoji = "🔮",
        primaryColor = Color(0xFF7C3AED),
        surfaceColor = Color(0xFFFAF5FF),
        isDark = false
    ),
    SUNSET_EMBER(
        id = "sunset_ember",
        title = "Sunset Amber",
        subtitle = "Hangat senja oranye & coral",
        iconEmoji = "🌅",
        primaryColor = Color(0xFFEA580C),
        surfaceColor = Color(0xFFFFF7ED),
        isDark = false
    ),
    AMOLED_NOIR(
        id = "amoled_noir",
        title = "AMOLED Pitch Black",
        subtitle = "Hitam murni hemat baterai & aksen perak",
        iconEmoji = "🖤",
        primaryColor = Color(0xFFE2E8F0),
        surfaceColor = Color(0xFF000000),
        isDark = true
    );

    companion object {
        fun fromId(id: String): AppSkinTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT_SLATE
        }
    }
}

/**
 * Generate Material3 ColorScheme based on the selected AppSkinTheme.
 */
fun getSkinColorScheme(skin: AppSkinTheme): ColorScheme {
    return when (skin) {
        AppSkinTheme.DEFAULT_SLATE -> lightColorScheme(
            primary = Color(0xFF0F172A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF1F5F9),
            onPrimaryContainer = Color(0xFF0F172A),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF64748B),
            outline = Color(0xFFE2E8F0)
        )
        AppSkinTheme.MIDNIGHT_CYBER -> darkColorScheme(
            primary = Color(0xFF06B6D4),
            onPrimary = Color(0xFF083344),
            primaryContainer = Color(0xFF164E63),
            onPrimaryContainer = Color(0xFFCFFAFE),
            background = Color(0xFF0B0F19),
            onBackground = Color(0xFFF1F5F9),
            surface = Color(0xFF111827),
            onSurface = Color(0xFFF9FAFB),
            surfaceVariant = Color(0xFF1F2937),
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = Color(0xFF374151)
        )
        AppSkinTheme.EMERALD_MINT -> lightColorScheme(
            primary = Color(0xFF059669),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD1FAE5),
            onPrimaryContainer = Color(0xFF064E3B),
            background = Color(0xFFF9FCF9),
            onBackground = Color(0xFF064E3B),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF134E4A),
            surfaceVariant = Color(0xFFE6F4EA),
            onSurfaceVariant = Color(0xFF047857),
            outline = Color(0xFFA7F3D0)
        )
        AppSkinTheme.ROYAL_AMETHYST -> lightColorScheme(
            primary = Color(0xFF7C3AED),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFEDE9FE),
            onPrimaryContainer = Color(0xFF4C1D95),
            background = Color(0xFFFAF8FF),
            onBackground = Color(0xFF2E1065),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF2E1065),
            surfaceVariant = Color(0xFFF3E8FF),
            onSurfaceVariant = Color(0xFF6D28D9),
            outline = Color(0xFFDDD6FE)
        )
        AppSkinTheme.SUNSET_EMBER -> lightColorScheme(
            primary = Color(0xFFEA580C),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFEDD5),
            onPrimaryContainer = Color(0xFF7C2D12),
            background = Color(0xFFFFFDFB),
            onBackground = Color(0xFF431407),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF431407),
            surfaceVariant = Color(0xFFFFEAD5),
            onSurfaceVariant = Color(0xFFC2410C),
            outline = Color(0xFFFED7AA)
        )
        AppSkinTheme.AMOLED_NOIR -> darkColorScheme(
            primary = Color(0xFFFFFFFF),
            onPrimary = Color(0xFF000000),
            primaryContainer = Color(0xFF1E293B),
            onPrimaryContainer = Color(0xFFF8FAFC),
            background = Color(0xFF000000),
            onBackground = Color(0xFFEDEDED),
            surface = Color(0xFF0A0A0A),
            onSurface = Color(0xFFF5F5F5),
            surfaceVariant = Color(0xFF171717),
            onSurfaceVariant = Color(0xFFA3A3A3),
            outline = Color(0xFF262626)
        )
    }
}
