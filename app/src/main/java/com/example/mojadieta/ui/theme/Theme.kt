package com.example.mojadieta.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Mint700 = Color(0xFF15805D)
val Mint600 = Color(0xFF1F9D74)
val Mint100 = Color(0xFFCDF3E3)
val Mint50 = Color(0xFFE9F8F1)
val AppBackground = Color(0xFFF5FBF8)

val ProteinColor = Color(0xFF4F7FE0)
val CarbsColor = Color(0xFFF0A232)
val FatColor = Color(0xFFE0645F)
val WaterColor = Color(0xFF2E9FD8)
val WaterContainer = Color(0xFFDDF0FA)

private val LightColors = lightColorScheme(
    primary = Mint600,
    onPrimary = Color.White,
    primaryContainer = Mint100,
    onPrimaryContainer = Color(0xFF00382A),
    secondary = Color(0xFF4A8C7A),
    onSecondary = Color.White,
    secondaryContainer = Mint50,
    onSecondaryContainer = Color(0xFF0C3A2E),
    tertiary = Color(0xFFE08A3C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6CF),
    onTertiaryContainer = Color(0xFF4A2600),
    background = AppBackground,
    onBackground = Color(0xFF17201C),
    surface = AppBackground,
    onSurface = Color(0xFF17201C),
    surfaceVariant = Color(0xFFE3EFE9),
    onSurfaceVariant = Color(0xFF55635D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCFEFD),
    surfaceContainer = Color(0xFFF0F7F3),
    surfaceContainerHigh = Color(0xFFEAF3EE),
    surfaceContainerHighest = Color(0xFFE3EEE8),
    outline = Color(0xFF8A9A93),
    outlineVariant = Color(0xFFD3E1DA),
    error = Color(0xFFC62828),
    onError = Color.White
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MojaDietaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
