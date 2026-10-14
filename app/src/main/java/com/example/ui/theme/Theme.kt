package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CleanLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = PureWhite,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,

    secondary = PrimaryBlue,
    onSecondary = PureWhite,
    secondaryContainer = PrimaryBlueLight,
    onSecondaryContainer = PrimaryBlueDark,

    tertiary = SoftGreen,
    onTertiary = PureWhite,
    tertiaryContainer = SoftGreenBg,
    onTertiaryContainer = SoftGreen,

    background = AppBackground,
    onBackground = TextTitle,

    surface = PureWhite,
    onSurface = TextTitle,
    surfaceVariant = AppBackground,
    onSurfaceVariant = TextBody,

    outline = BorderSubtle,
    outlineVariant = BorderMedium,

    error = SoftRed,
    onError = PureWhite,
    errorContainer = SoftRedBg,
    onErrorContainer = SoftRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CleanLightColorScheme,
        typography = Typography,
        content = content
    )
}
