package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// Phosphor / Lucide Style Bespoke Line Icons (20-22dp outline)
// No stock Material Symbols filled icons!
// ============================================================================

/**
 * Camera Outline Icon for Scan Tab
 */
@Composable
fun IconCameraOutline(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.75.dp.toPx()
        val w = this.size.width
        val h = this.size.height

        // Camera body with rounded corners
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, h * 0.30f),
            size = Size(w * 0.76f, h * 0.54f),
            cornerRadius = CornerRadius(w * 0.12f, h * 0.12f),
            style = Stroke(width = stroke)
        )

        // Top turret
        val turret = Path().apply {
            moveTo(w * 0.36f, h * 0.30f)
            lineTo(w * 0.42f, h * 0.18f)
            lineTo(w * 0.58f, h * 0.18f)
            lineTo(w * 0.64f, h * 0.30f)
        }
        drawPath(turret, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Center lens circle
        drawCircle(
            color = tint,
            radius = w * 0.16f,
            center = Offset(w * 0.50f, h * 0.57f),
            style = Stroke(width = stroke)
        )
    }
}

/**
 * Retail Swing Tag Outline Icon for Cart Tab (custom line drawing instead of cart glyph)
 */
@Composable
fun IconTagOutline(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.75.dp.toPx()
        val w = this.size.width
        val h = this.size.height

        // Tilted swing tag path
        val tag = Path().apply {
            moveTo(w * 0.44f, h * 0.14f)
            lineTo(w * 0.82f, h * 0.52f)
            lineTo(w * 0.56f, h * 0.86f)
            lineTo(w * 0.16f, h * 0.46f)
            lineTo(w * 0.16f, h * 0.14f)
            close()
        }
        drawPath(tag, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // String hole
        drawCircle(
            color = tint,
            radius = w * 0.055f,
            center = Offset(w * 0.30f, h * 0.28f),
            style = Stroke(width = stroke)
        )
    }
}

/**
 * Thermal Receipt Outline Icon for Trips Tab
 */
@Composable
fun IconReceiptOutline(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.75.dp.toPx()
        val w = this.size.width
        val h = this.size.height

        // Receipt body with torn/zigzag bottom
        val receipt = Path().apply {
            moveTo(w * 0.22f, h * 0.12f)
            lineTo(w * 0.78f, h * 0.12f)
            lineTo(w * 0.78f, h * 0.84f)
            // Zigzag bottom
            lineTo(w * 0.68f, h * 0.78f)
            lineTo(w * 0.58f, h * 0.84f)
            lineTo(w * 0.48f, h * 0.78f)
            lineTo(w * 0.38f, h * 0.84f)
            lineTo(w * 0.22f, h * 0.78f)
            close()
        }
        drawPath(receipt, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Internal printed lines
        drawLine(tint, Offset(w * 0.34f, h * 0.30f), Offset(w * 0.66f, h * 0.30f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.34f, h * 0.44f), Offset(w * 0.66f, h * 0.44f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.34f, h * 0.58f), Offset(w * 0.54f, h * 0.58f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/**
 * Circle / Person Outline Icon for Profile Tab
 */
@Composable
fun IconProfileOutline(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.75.dp.toPx()
        val w = this.size.width
        val h = this.size.height

        // Head circle
        drawCircle(
            color = tint,
            radius = w * 0.20f,
            center = Offset(w * 0.50f, h * 0.34f),
            style = Stroke(width = stroke)
        )

        // Shoulders arc
        val shoulders = Path().apply {
            moveTo(w * 0.18f, h * 0.84f)
            quadraticBezierTo(w * 0.18f, h * 0.62f, w * 0.50f, h * 0.62f)
            quadraticBezierTo(w * 0.82f, h * 0.62f, w * 0.82f, h * 0.84f)
        }
        drawPath(shoulders, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

/**
 * Flash Bolt Outline Icon
 */
@Composable
fun IconBoltOutline(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.75.dp.toPx()
        val w = this.size.width
        val h = this.size.height

        val bolt = Path().apply {
            moveTo(w * 0.55f, h * 0.10f)
            lineTo(w * 0.25f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.90f)
            lineTo(w * 0.75f, h * 0.44f)
            lineTo(w * 0.50f, h * 0.44f)
            close()
        }
        drawPath(bolt, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Gallery / Image Outline Icon
 */
@Composable
fun IconGalleryOutline(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.75.dp.toPx()
        val w = this.size.width
        val h = this.size.height

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.14f, h * 0.14f),
            size = Size(w * 0.72f, h * 0.72f),
            cornerRadius = CornerRadius(w * 0.12f, h * 0.12f),
            style = Stroke(width = stroke)
        )

        // Mountains path
        val mountains = Path().apply {
            moveTo(w * 0.20f, h * 0.72f)
            lineTo(w * 0.42f, h * 0.48f)
            lineTo(w * 0.56f, h * 0.62f)
            lineTo(w * 0.70f, h * 0.44f)
            lineTo(w * 0.80f, h * 0.72f)
        }
        drawPath(mountains, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Sun dot
        drawCircle(
            color = tint,
            radius = w * 0.06f,
            center = Offset(w * 0.36f, h * 0.32f),
            style = Stroke(width = stroke)
        )
    }
}

/**
 * Checkmark Line Icon
 */
@Composable
fun IconCheckLine(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 2.dp.toPx()
        val w = this.size.width
        val h = this.size.height

        val check = Path().apply {
            moveTo(w * 0.22f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.72f)
            lineTo(w * 0.78f, h * 0.28f)
        }
        drawPath(check, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
