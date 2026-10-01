package com.example.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Motion tokens for TrueTag (Section A2)
 * - Standard duration: 250ms
 * - Spring for anything physical: dampingRatio 0.75, stiffness 300
 * - Screen transitions: shared-axis X, 300ms
 * - List items: staggered slide-in from right, 40ms delay per item
 * - Buttons: scale 0.97 on press, ripple, haptic
 */
object MotionTokens {
    const val DURATION_FAST = 150
    const val DURATION_STANDARD = 250
    const val DURATION_SCREEN = 300
    const val STAGGER_DELAY_MS = 40

    val StandardTween: AnimationSpec<Float> = tween(
        durationMillis = DURATION_STANDARD,
        easing = FastOutSlowInEasing
    )

    val PhysicalSpring: AnimationSpec<Float> = spring(
        dampingRatio = 0.75f,
        stiffness = 300f
    )
}

/**
 * Reusable modifier to scale down to 0.97 on button press with physical spring (Section A2)
 */
fun Modifier.pressScaleEffect(
    pressedScale: Float = 0.97f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 300f
        ),
        label = "press_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}
