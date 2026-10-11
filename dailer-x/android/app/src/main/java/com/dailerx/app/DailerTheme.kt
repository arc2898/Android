package com.dailerx.app

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Dailer X uses a deliberately narrow gold / white / charcoal identity. */
internal object DailerPalette {
    val Gold = Color(0xFFD3B165)
    val GoldDeep = Color(0xFFB18A36)
    val GoldContainerLight = Color(0xFFF0E3BE)
    val GoldContainerDark = Color(0xFF3C321D)
    val Ivory = Color(0xFFF7F4EB)
    val Paper = Color(0xFFFFFEFA)
    val Charcoal = Color(0xFF201F1B)
    val Onyx = Color(0xFF11110F)
    val Ash = Color(0xFF777267)
    val LineLight = Color(0xFFC9C1AE)
    val LineDark = Color(0xFF595447)
}

internal fun dailerColorScheme(dark: Boolean, background: Color): ColorScheme = if (dark) {
    darkColorScheme(
        primary = DailerPalette.Gold,
        onPrimary = DailerPalette.Charcoal,
        primaryContainer = DailerPalette.GoldContainerDark,
        onPrimaryContainer = DailerPalette.Ivory,
        inversePrimary = DailerPalette.GoldDeep,
        secondary = DailerPalette.Gold,
        onSecondary = DailerPalette.Charcoal,
        secondaryContainer = DailerPalette.GoldContainerDark,
        onSecondaryContainer = DailerPalette.Ivory,
        tertiary = DailerPalette.Gold,
        onTertiary = DailerPalette.Charcoal,
        tertiaryContainer = DailerPalette.GoldContainerDark,
        onTertiaryContainer = DailerPalette.Ivory,
        background = background,
        onBackground = DailerPalette.Ivory,
        surface = Color(0xF0201F1B),
        onSurface = DailerPalette.Ivory,
        surfaceVariant = Color(0xFF292821),
        onSurfaceVariant = Color(0xFFC7C0B1),
        surfaceTint = DailerPalette.Gold,
        inverseSurface = DailerPalette.Ivory,
        inverseOnSurface = DailerPalette.Charcoal,
        outline = DailerPalette.LineDark,
        outlineVariant = Color(0xFF3D3A32),
        error = DailerPalette.Gold,
        onError = DailerPalette.Charcoal,
        errorContainer = DailerPalette.GoldContainerDark,
        onErrorContainer = DailerPalette.Ivory,
        scrim = DailerPalette.Onyx
    )
} else {
    lightColorScheme(
        primary = DailerPalette.GoldDeep,
        onPrimary = DailerPalette.Charcoal,
        primaryContainer = DailerPalette.GoldContainerLight,
        onPrimaryContainer = DailerPalette.Charcoal,
        inversePrimary = DailerPalette.Gold,
        secondary = DailerPalette.GoldDeep,
        onSecondary = DailerPalette.Charcoal,
        secondaryContainer = DailerPalette.GoldContainerLight,
        onSecondaryContainer = DailerPalette.Charcoal,
        tertiary = DailerPalette.GoldDeep,
        onTertiary = DailerPalette.Charcoal,
        tertiaryContainer = DailerPalette.GoldContainerLight,
        onTertiaryContainer = DailerPalette.Charcoal,
        background = background,
        onBackground = DailerPalette.Charcoal,
        surface = Color(0xF9FFFEFA),
        onSurface = DailerPalette.Charcoal,
        surfaceVariant = Color(0xFFF0EDE4),
        onSurfaceVariant = Color(0xFF625E55),
        surfaceTint = DailerPalette.GoldDeep,
        inverseSurface = DailerPalette.Charcoal,
        inverseOnSurface = DailerPalette.Ivory,
        outline = DailerPalette.LineLight,
        outlineVariant = Color(0xFFE2DCCF),
        error = DailerPalette.GoldDeep,
        onError = DailerPalette.Charcoal,
        errorContainer = DailerPalette.GoldContainerLight,
        onErrorContainer = DailerPalette.Charcoal,
        scrim = DailerPalette.Charcoal
    )
}
