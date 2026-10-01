package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// TrueTag Color System — Physical Retail Swing Tag & Thermal Receipt Identity
// NO pure white #FFFFFF and NO pure black #000000 anywhere.
// Warmth throughout all neutrals drifting toward cream and ink.
// ============================================================================

// Light Mode Palette
val PaperCream = Color(0xFFF7F3EA)       // Background: warm paper cream
val PaperSurface = Color(0xFFFFFBF2)     // Surface (cards): warm off white
val NearBlackInk = Color(0xFF201C16)     // Primary text: warm near black ink
val WarmGray = Color(0xFF6B6258)         // Secondary text: warm gray
val TagRed = Color(0xFFC1443C)           // Primary accent: tag red, muted brick terracotta
val InkNavy = Color(0xFF1F2A44)          // Secondary accent: ink navy
val MarkerYellow = Color(0xFFE8B23D)     // Highlight accent: marker yellow (badges/highlights only)
val ForestGreen = Color(0xFF2F5D50)      // Real price color: deep forest green
val ActionRed = Color(0xFFB23A2E)        // Error and delete actions
val ReceiptDivider = Color(0xFFE4DCC8)   // Border and divider

// Dark Mode Palette
val DeepCharcoal = Color(0xFF14120F)     // Background: deep warm charcoal
val DeepSurface = Color(0xFF201C17)      // Surface: deep warm surface
val OffWhiteInk = Color(0xFFF5F0E6)      // Primary text: warm off white
val MutedWarmGray = Color(0xFFA39B8C)    // Secondary text: warm gray
val BrightTagRed = Color(0xFFE2604F)     // Primary accent: brighter tag red for contrast
val LightInkNavy = Color(0xFF4A5A80)     // Secondary accent: lighter ink navy
val BrightForestGreen = Color(0xFF4C8C79)// Real price color: brighter forest green
val DarkDivider = Color(0xFF2E2A22)      // Border and divider

// Compatibility Aliases for components
val TrueGreen = ForestGreen
val TrueGreenDark = ForestGreen
val TrueGreenContainer = Color(0xFFEBF2EE)
val TrueGreenContainerDark = Color(0xFF1E2E28)

val DarkBackground = DeepCharcoal
val DarkSurface = DeepSurface
val DarkSurfaceVariant = Color(0xFF28231D)
val DarkSurfaceElevated = Color(0xFF2D2721)
val DarkOnBackground = OffWhiteInk
val DarkOnSurface = OffWhiteInk
val DarkOnSurfaceVariant = MutedWarmGray
val DarkOutline = DarkDivider

val LightBackground = PaperCream
val LightSurface = PaperSurface
val LightSurfaceVariant = Color(0xFFF2ECE0)
val LightSurfaceElevated = PaperSurface
val LightOnBackground = NearBlackInk
val LightOnSurface = NearBlackInk
val LightOnSurfaceVariant = WarmGray
val LightOutline = ReceiptDivider

val PriceAccent = ForestGreen
val TaxMuted = WarmGray
val ErrorRed = ActionRed
val WarningAmber = MarkerYellow
val SuccessGreen = ForestGreen
val GoldPro = MarkerYellow
val GoldProDark = MarkerYellow
