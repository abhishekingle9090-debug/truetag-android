package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.InkNavy
import com.example.ui.theme.LightInkNavy
import com.example.ui.theme.PaperCream
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.TagRed
import com.example.ui.theme.WarmGray
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) DeepCharcoal else PaperCream
    val navyColor = if (isDark) LightInkNavy else InkNavy

    // Animation: Logo mark drawing on with checkmark stroke appearing last (900ms total)
    val tagProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Tag outline draws on (0..500ms)
        tagProgress.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        // Checkmark stroke draws on last (500..900ms)
        checkProgress.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        // Fade in wordmark and caption
        contentAlpha.animateTo(1f, animationSpec = tween(300))
        // Hold 1.5 seconds then route
        delay(1500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Drawing Logo Mark
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Tag outline path
                    val tagPath = Path().apply {
                        moveTo(w * 0.40f, h * 0.16f)
                        lineTo(w * 0.60f, h * 0.16f)
                        lineTo(w * 0.82f, h * 0.32f)
                        lineTo(w * 0.82f, h * 0.82f)
                        lineTo(w * 0.18f, h * 0.82f)
                        lineTo(w * 0.18f, h * 0.32f)
                        close()
                    }

                    if (tagProgress.value > 0f) {
                        val measure = PathMeasure()
                        measure.setPath(tagPath, false)
                        val len = measure.length
                        val segment = Path()
                        measure.getSegment(0f, len * tagProgress.value, segment, true)

                        drawPath(
                            path = segment,
                            color = TagRed,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Eyelet Hole
                    if (tagProgress.value >= 0.7f) {
                        drawCircle(
                            color = TagRed,
                            radius = w * 0.05f,
                            center = Offset(w * 0.50f, h * 0.28f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Checkmark stroke appearing last
                    if (checkProgress.value > 0f) {
                        val checkPath = Path().apply {
                            moveTo(w * 0.36f, h * 0.54f)
                            lineTo(w * 0.47f, h * 0.66f)
                            lineTo(w * 0.64f, h * 0.46f)
                        }
                        val checkMeasure = PathMeasure()
                        checkMeasure.setPath(checkPath, false)
                        val checkLen = checkMeasure.length
                        val checkSegment = Path()
                        checkMeasure.getSegment(0f, checkLen * checkProgress.value, checkSegment, true)

                        drawPath(
                            path = checkSegment,
                            color = navyColor,
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Two-tone Wordmark: "True" in Ink Navy, "Tag" in Tag Red in Space Grotesk Bold
            Row {
                Text(
                    text = "True",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = navyColor
                )
                Text(
                    text = "Tag",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = TagRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Small caption type: "know the real price before checkout"
            Text(
                text = "know the real price before checkout",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                letterSpacing = 0.4.sp,
                color = WarmGray
            )
        }
    }
}
