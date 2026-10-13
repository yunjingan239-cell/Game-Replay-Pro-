package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GameReplayColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = CyberBlack,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = ElectricCyan,
    
    secondary = CyberPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF32145A),
    onSecondaryContainer = Color(0xFFE8D4FF),
    
    tertiary = NeonGreen,
    onTertiary = CyberBlack,
    tertiaryContainer = Color(0xFF00391A),
    onTertiaryContainer = NeonGreen,
    
    background = CyberBlack,
    onBackground = TextPrimary,
    
    surface = CyberSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceCard,
    onSurfaceVariant = TextSecondary,
    
    outline = CyberBorder,
    outlineVariant = CyberBorderGlow,
    
    error = RadiantCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GameReplayColorScheme,
        typography = Typography,
        content = content
    )
}
