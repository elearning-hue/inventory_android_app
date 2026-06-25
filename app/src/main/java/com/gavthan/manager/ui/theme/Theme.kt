package com.gavthan.manager.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Full semantic palette used across the app (beyond Material's color roles). */
@Immutable
data class GavColors(
    val bg: Color, val surface: Color, val surface2: Color, val surface3: Color,
    val ink: Color, val muted: Color, val muted2: Color,
    val line: Color, val lineStrong: Color,
    val accent: Color, val accent600: Color, val accentInk: Color, val accentTint: Color,
    val debit: Color, val credit: Color, val warn: Color,
    val redBg: Color, val redBorder: Color,
    val greenBg: Color, val greenBorder: Color,
    val warnBg: Color, val warnBorder: Color,
    val avSup: Color,
    val isDark: Boolean,
)

private val LightGav = GavColors(
    bg = L_bg, surface = L_surface, surface2 = L_surface2, surface3 = L_surface3,
    ink = L_ink, muted = L_muted, muted2 = L_muted2,
    line = L_line, lineStrong = L_lineStrong,
    accent = L_accent, accent600 = L_accent600, accentInk = L_accentInk, accentTint = L_accentTint,
    debit = L_debit, credit = L_credit, warn = L_warn,
    redBg = L_redBg, redBorder = L_redBorder, greenBg = L_greenBg, greenBorder = L_greenBorder,
    warnBg = L_warnBg, warnBorder = L_warnBorder, avSup = L_avSup, isDark = false,
)

private val DarkGav = GavColors(
    bg = D_bg, surface = D_surface, surface2 = D_surface2, surface3 = D_surface3,
    ink = D_ink, muted = D_muted, muted2 = D_muted2,
    line = D_line, lineStrong = D_lineStrong,
    accent = D_accent, accent600 = D_accent600, accentInk = D_accentInk, accentTint = D_accentTint,
    debit = D_debit, credit = D_credit, warn = D_warn,
    redBg = D_redBg, redBorder = D_redBorder, greenBg = D_greenBg, greenBorder = D_greenBorder,
    warnBg = D_warnBg, warnBorder = D_warnBorder, avSup = D_avSup, isDark = true,
)

val LocalGavColors = staticCompositionLocalOf { LightGav }

/** Shorthand for reading the active palette inside composables: `val c = gav`. */
val gav: GavColors
    @Composable @ReadOnlyComposable get() = LocalGavColors.current

private fun schemeFor(c: GavColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.accent, onPrimary = White, primaryContainer = c.accentTint, onPrimaryContainer = c.accentInk,
        secondary = c.accent, onSecondary = White,
        background = c.bg, onBackground = c.ink,
        surface = c.surface, onSurface = c.ink,
        surfaceVariant = c.surface2, onSurfaceVariant = c.muted,
        surfaceContainer = c.surface2, surfaceContainerHigh = c.surface3,
        outline = c.lineStrong, outlineVariant = c.line,
        error = c.debit, onError = White, errorContainer = c.redBg, onErrorContainer = c.debit,
        scrim = Color(0x80000000),
    )
} else {
    lightColorScheme(
        primary = c.accent, onPrimary = White, primaryContainer = c.accentTint, onPrimaryContainer = c.accentInk,
        secondary = c.accent, onSecondary = White,
        background = c.bg, onBackground = c.ink,
        surface = c.surface, onSurface = c.ink,
        surfaceVariant = c.surface2, onSurfaceVariant = c.muted,
        surfaceContainer = c.surface2, surfaceContainerHigh = c.surface3,
        outline = c.lineStrong, outlineVariant = c.line,
        error = c.debit, onError = White, errorContainer = c.redBg, onErrorContainer = c.debit,
        scrim = Color(0x80000000),
    )
}

@Composable
fun GavthanTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkGav else LightGav
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalGavColors provides colors) {
        MaterialTheme(
            colorScheme = schemeFor(colors),
            typography = GavTypography,
            content = content,
        )
    }
}
