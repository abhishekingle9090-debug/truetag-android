package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Base spacing scale: 4dp grid
object Spacing {
    val unit: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val cardPadding: Dp = 18.dp
    val screenPadding: Dp = 20.dp
    val sectionSpacing: Dp = 32.dp
}

private val ColorTagRedContainer = Color(0xFFF8E7E5)
private val ColorDarkRedContainer = Color(0xFF3B1F1C)

private val DarkColorScheme = darkColorScheme(
    primary = BrightTagRed,
    onPrimary = DeepSurface,
    primaryContainer = ColorDarkRedContainer,
    onPrimaryContainer = OffWhiteInk,
    secondary = LightInkNavy,
    onSecondary = OffWhiteInk,
    secondaryContainer = DeepSurface,
    onSecondaryContainer = OffWhiteInk,
    tertiary = BrightForestGreen,
    onTertiary = DeepSurface,
    background = DeepCharcoal,
    onBackground = OffWhiteInk,
    surface = DeepSurface,
    onSurface = OffWhiteInk,
    surfaceVariant = Color(0xFF28231C),
    onSurfaceVariant = MutedWarmGray,
    outline = DarkDivider,
    outlineVariant = DarkDivider,
    error = ActionRed,
    onError = OffWhiteInk
)

private val LightColorScheme = lightColorScheme(
    primary = TagRed,
    onPrimary = PaperSurface,
    primaryContainer = ColorTagRedContainer,
    onPrimaryContainer = NearBlackInk,
    secondary = InkNavy,
    onSecondary = PaperSurface,
    secondaryContainer = Color(0xFFE9EDF5),
    onSecondaryContainer = InkNavy,
    tertiary = ForestGreen,
    onTertiary = PaperSurface,
    background = PaperCream,
    onBackground = NearBlackInk,
    surface = PaperSurface,
    onSurface = NearBlackInk,
    surfaceVariant = Color(0xFFF1EADE),
    onSurfaceVariant = WarmGray,
    outline = ReceiptDivider,
    outlineVariant = ReceiptDivider,
    error = ActionRed,
    onError = PaperSurface
)

// TrueTag Shape Language:
// Swing tag card: 20dp corners
// Secondary cards / settings: 16dp corners
// Buttons: 14dp corners
// Chips: 10dp corners
val TrueTagShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun TrueTagTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = TrueTagShapes,
        content = content
    )
}
