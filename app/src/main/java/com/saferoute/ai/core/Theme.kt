package com.saferoute.ai.core

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================
// SAFEROUTE AI COLORS
// ============================================================

val Background = Color(0xFF090D11)
val SurfaceColor = Color(0xFF141A20)
val SurfaceLight = Color(0xFF1B222A)
val Border = Color(0xFF252E37)

val Green = Color(0xFF59E6B0)
val GreenDark = Color(0xFF173229)

val TextPrimary = Color(0xFFF5F7F8)
val TextSecondary = Color(0xFF929AA3)
val TextTertiary = Color(0xFF69727C)

val Red = Color(0xFFFF6B6B)

// ============================================================
// THEME
// ============================================================

@Composable
fun SafeRouteTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme =
            androidx.compose.material3.darkColorScheme(
                primary = Green,
                background = Background,
                surface = SurfaceColor,
                onBackground = TextPrimary,
                onSurface = TextPrimary
            ),
        content = content
    )
}