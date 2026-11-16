package com.cleber.baixatrix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BrandOrange = Color(0xFFFF9F43)
val BrandPink = Color(0xFFE1306C)
val BrandPurple = Color(0xFF8A3FFC)

/** Assinatura visual do app: laranja do Kwai → rosa do Instagram → roxo. */
val BrandGradient = Brush.linearGradient(
    colors = listOf(BrandOrange, BrandPink, BrandPurple)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFF4D7D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B1222),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = BrandOrange,
    onSecondary = Color(0xFF2A1604),
    background = Color(0xFF0C0A0F),
    onBackground = Color(0xFFF5EFF8),
    surface = Color(0xFF151219),
    onSurface = Color(0xFFF5EFF8),
    surfaceVariant = Color(0xFF1F1B26),
    onSurfaceVariant = Color(0xFFB7ACC0),
    surfaceContainer = Color(0xFF1A1620),
    outline = Color(0xFF3C3444),
    outlineVariant = Color(0xFF2A2430),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF43141A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFC2185B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E3),
    onPrimaryContainer = Color(0xFF3B0718),
    secondary = Color(0xFFE8590C),
    onSecondary = Color.White,
    background = Color(0xFFFDF8FB),
    onBackground = Color(0xFF1B1218),
    surface = Color.White,
    onSurface = Color(0xFF1B1218),
    surfaceVariant = Color(0xFFF2E9EF),
    onSurfaceVariant = Color(0xFF6B5B66),
    surfaceContainer = Color(0xFFF7F0F4),
    outline = Color(0xFFD8C9D3),
    outlineVariant = Color(0xFFEADFE6),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410E0B)
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BaixaTrixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
