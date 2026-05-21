package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val MyDarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF047857),
    onPrimaryContainer = Color(0xFFD1FAE5),
    
    secondary = SlateBase,
    onSecondary = WhiteIce,
    secondaryContainer = SlateDeep,
    onSecondaryContainer = GrayMuted,

    tertiary = ElectricPurple,
    onTertiary = Color.White,
    
    background = ObsidianDark,
    onBackground = WhiteIce,
    surface = SlateDeep,
    onSurface = WhiteIce,
    surfaceVariant = SlateBase,
    onSurfaceVariant = WhiteIce,
    
    error = CrimsonRed,
    onError = Color.White
)

private val MyLightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    
    secondary = Color(0xFFE2E8F0),
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF475569),

    tertiary = ElectricPurple,
    onTertiary = Color.White,
    
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF1E293B),
    
    error = CrimsonRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Let's support user selection or standard system toggle!
    dynamicColor: Boolean = false, // Disable default Android dynamic color to preserve SpendWise bespoke brand identity!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        MyDarkColorScheme
    } else {
        MyLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
