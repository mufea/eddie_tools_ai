package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Professional Elegant Theme Colors (Light Theme)
val LightPrimary = Color(0xFF0F172A)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnBackground = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF64748B)
val LightBorder = Color(0xFFE2E8F0)
val LightUserBubbleBg = Color(0xFF0F172A)
val LightUserBubbleText = Color(0xFFF8FAFC)

// Professional Elegant Theme Colors (Dark Theme)
val DarkPrimary = Color(0xFFF8FAFC)
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)
val DarkOnBackground = Color(0xFFF8FAFC)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkBorder = Color(0xFF334155)
val DarkUserBubbleBg = Color(0xFF38BDF8) // High contrast blue/cyan accent for user on dark theme
val DarkUserBubbleText = Color(0xFF0F172A)

// Legacy variable names for backwards-compat if referenced elsewhere (will map to Light by default, overridden by themed colors)
val AppPrimary = LightPrimary
val AppBackground = LightBackground
val AppSurface = LightSurface
val AppSurfaceVariant = LightSurfaceVariant
val AppOnBackground = LightOnBackground
val AppOnSurfaceVariant = LightOnSurfaceVariant
val AppUserBubbleBg = LightUserBubbleBg
val AppUserBubbleText = LightUserBubbleText
val AppBorder = LightBorder
