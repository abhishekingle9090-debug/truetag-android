package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TagRed

/**
 * Scan frame at 80 percent screen width using four short corner brackets in tag red
 * with a slow opacity pulse animation.
 */
@Composable
fun ScanOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_bracket_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bracket_alpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameWidth = size.width * 0.80f
            val frameHeight = frameWidth * 0.58f
            val left = (size.width - frameWidth) / 2f
            val top = (size.height - frameHeight) / 2f
            val cornerRadius = 14.dp.toPx()
            val bracketLength = 26.dp.toPx()
            val bracketStroke = 3.5.dp.toPx()

            // Subtle darkened surround outside guide window
            drawRect(color = Color.Black.copy(alpha = 0.35f))

            // Transparent hole inside guide frame
            val cutoutPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = left,
                        top = top,
                        right = left + frameWidth,
                        bottom = top + frameHeight,
                        radiusX = cornerRadius,
                        radiusY = cornerRadius
                    )
                )
            }
            drawPath(
                path = cutoutPath,
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )

            // Four short corner brackets in Tag Red with slow opacity pulse
            val bracketColor = TagRed.copy(alpha = pulseAlpha)

            // Top-Left bracket
            val tlPath = Path().apply {
                moveTo(left, top + bracketLength)
                lineTo(left, top + cornerRadius)
                quadraticTo(left, top, left + cornerRadius, top)
                lineTo(left + bracketLength, top)
            }
            drawPath(tlPath, bracketColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))

            // Top-Right bracket
            val trPath = Path().apply {
                moveTo(left + frameWidth - bracketLength, top)
                lineTo(left + frameWidth - cornerRadius, top)
                quadraticTo(left + frameWidth, top, left + frameWidth, top + cornerRadius)
                lineTo(left + frameWidth, top + bracketLength)
            }
            drawPath(trPath, bracketColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))

            // Bottom-Left bracket
            val blPath = Path().apply {
                moveTo(left, top + frameHeight - bracketLength)
                lineTo(left, top + frameHeight - cornerRadius)
                quadraticTo(left, top + frameHeight, left + cornerRadius, top + frameHeight)
                lineTo(left + bracketLength, top + frameHeight)
            }
            drawPath(blPath, bracketColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))

            // Bottom-Right bracket
            val brPath = Path().apply {
                moveTo(left + frameWidth - bracketLength, top + frameHeight)
                lineTo(left + frameWidth - cornerRadius, top + frameHeight)
                quadraticTo(left + frameWidth, top + frameHeight, left + frameWidth - cornerRadius, top + frameHeight)
                lineTo(left + frameWidth, top + frameHeight - bracketLength)
            }
            drawPath(brPath, bracketColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))
        }
    }
}
