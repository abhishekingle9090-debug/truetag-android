package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

data class Particle(
    val x: Float,
    val initialY: Float,
    val speedY: Float,
    val speedX: Float,
    val rotationSpeed: Float,
    val size: Float,
    val color: Color
)

@Composable
fun ConfettiOverlay(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    if (!active) return

    val colors = listOf(
        Color(0xFF00C853),
        Color(0xFFFFD600),
        Color(0xFF00E5FF),
        Color(0xFFFF4081),
        Color(0xFF7C4DFF),
        Color(0xFFFFFFFF)
    )

    val particles = remember {
        List(60) {
            Particle(
                x = Random.nextFloat(),
                initialY = Random.nextFloat() * -0.4f,
                speedY = 0.5f + Random.nextFloat() * 0.7f,
                speedX = (Random.nextFloat() - 0.5f) * 0.2f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                size = 14f + Random.nextFloat() * 16f,
                color = colors[Random.nextInt(colors.size)]
            )
        }
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(active) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            val currentY = (p.initialY + (p.speedY * progress.value)) * h
            val currentX = (p.x + (p.speedX * progress.value)) * w
            val currentRotation = p.rotationSpeed * progress.value

            if (currentY in 0f..h) {
                rotate(degrees = currentRotation, pivot = Offset(currentX, currentY)) {
                    drawRect(
                        color = p.color.copy(alpha = (1f - progress.value * 0.4f).coerceIn(0f, 1f)),
                        topLeft = Offset(currentX, currentY),
                        size = Size(p.size, p.size * 0.6f)
                    )
                }
            }
        }
    }
}
