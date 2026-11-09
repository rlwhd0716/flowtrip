package com.github.flowtrip.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand tokens live here so a later visual design can replace the palette centrally.
private val LightColors = lightColorScheme(
    primary = Color(0xFF176B59), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCEDE0), onPrimaryContainer = Color(0xFF092F27),
    background = Color(0xFFF5F7F5), surface = Color(0xFFFCFDFB),
    onBackground = Color(0xFF192D27), onSurface = Color(0xFF192D27),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8ED5BB), onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF225043), onPrimaryContainer = Color(0xFFCCEDE0),
    background = Color(0xFF101A17), surface = Color(0xFF18231F),
    onBackground = Color(0xFFE1ECE5), onSurface = Color(0xFFE1ECE5),
)
private val FlowTypography = Typography(
    headlineLarge = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
)
private val FlowShapes = Shapes(
    small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp),
)

@Composable
fun FlowTripTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FlowTypography,
        shapes = FlowShapes,
        content = content,
    )
}
