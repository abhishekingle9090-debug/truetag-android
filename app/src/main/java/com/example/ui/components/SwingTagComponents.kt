package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IbmpPlexMono
import com.example.ui.theme.InkNavy
import com.example.ui.theme.PaperCream
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.ReceiptDivider
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.TagRed
import kotlin.math.abs

// ============================================================================
// SWING TAG SILHOUETTE SHAPE
// Rounded rectangle (20dp) with a 10dp circular string hole cutout in top-left
// inset 14dp from top and left edges.
// ============================================================================

val SwingTagShape = GenericShape { size, _ ->
    val cornerRadius = 20f * 2.75f // approx in px or adaptive
    val holeRadius = 5f * 2.75f
    val holeCenterX = 14f * 2.75f
    val holeCenterY = 14f * 2.75f

    // Outer rounded rect
    addRoundRect(
        RoundRect(
            rect = Rect(0f, 0f, size.width, size.height),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
    )
}

/**
 * Primary Swing Tag Card with authentic punched string hole and subtle inner shadow.
 */
@Composable
fun SwingTagCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    hasHoleCutout: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(clickableModifier)
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        // Tag content
        Box(modifier = Modifier.padding(18.dp)) {
            content()
        }

        // Punched string hole at top-left corner inset 14dp
        if (hasHoleCutout) {
            Canvas(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 14.dp)
            ) {
                val radius = 5.dp.toPx()
                val center = Offset(radius, radius)

                // Hole cutout (paper cream or background showing through)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.12f),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = borderColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                // Eyelet ring grommet highlight
                drawCircle(
                    color = borderColor.copy(alpha = 0.7f),
                    radius = radius + 2.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                )
            }
        }
    }
}

// ============================================================================
// RECEIPT DASHED DIVIDER
// Evoking a retail receipt tear line: 4dp dash, 3dp gap pattern.
// ============================================================================

@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline,
    dashWidth: Dp = 4.dp,
    gapWidth: Dp = 3.dp,
    thickness: Dp = 1.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
    ) {
        val pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashWidth.toPx(), gapWidth.toPx()), 0f
        )
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = thickness.toPx(),
            pathEffect = pathEffect
        )
    }
}

// ============================================================================
// TORN PAPER TOP EDGE (FOR BOTTOM SHEETS & RECEIPTS)
// Jagged zigzag path across top 6dp of the sheet before content begins.
// ============================================================================

val TornPaperTopShape = GenericShape { size, _ ->
    val toothWidth = 8f * 2.75f
    val toothHeight = 6f * 2.75f
    val count = (size.width / toothWidth).toInt()

    moveTo(0f, toothHeight)
    var currentX = 0f
    var up = false
    while (currentX < size.width) {
        currentX += toothWidth / 2f
        val y = if (up) 0f else toothHeight
        lineTo(currentX.coerceAtMost(size.width), y)
        up = !up
    }
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun TornPaperHeader(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    toothWidth: Dp = 8.dp,
    toothHeight: Dp = 6.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(toothHeight)
    ) {
        val w = toothWidth.toPx()
        val h = toothHeight.toPx()
        val path = Path().apply {
            moveTo(0f, h)
            var x = 0f
            var up = true
            while (x < size.width) {
                x += w / 2f
                val y = if (up) 0f else h
                lineTo(x.coerceAtMost(size.width), y)
                up = !up
            }
            lineTo(size.width, h)
            close()
        }
        drawPath(path = path, color = backgroundColor)
    }
}

// ============================================================================
// TRUETAG LOGO MARK & WORDMARK LOCKUP
// Geometric price tag silhouette + integrated checkmark in Tag Red & Ink Navy.
// Wordmark: "True" in Ink Navy, "Tag" in Tag Red set in Space Grotesk Bold.
// ============================================================================

@Composable
fun TrueTagLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tagColor: Color = TagRed,
    checkColor: Color = InkNavy,
    holeColor: Color = PaperCream
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Swing tag path
        val tagPath = Path().apply {
            moveTo(w * 0.42f, h * 0.16f)
            lineTo(w * 0.58f, h * 0.16f)
            lineTo(w * 0.78f, h * 0.32f)
            lineTo(w * 0.78f, h * 0.82f)
            lineTo(w * 0.22f, h * 0.82f)
            lineTo(w * 0.22f, h * 0.32f)
            close()
        }
        drawPath(path = tagPath, color = tagColor)

        // Punched string hole
        drawCircle(
            color = holeColor,
            radius = w * 0.055f,
            center = Offset(w * 0.50f, h * 0.26f)
        )

        // Integrated checkmark
        val checkPath = Path().apply {
            moveTo(w * 0.38f, h * 0.54f)
            lineTo(w * 0.48f, h * 0.65f)
            lineTo(w * 0.64f, h * 0.46f)
        }
        drawPath(
            path = checkPath,
            color = checkColor,
            style = Stroke(
                width = w * 0.08f,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun TrueTagLogoLockup(
    modifier: Modifier = Modifier,
    markSize: Dp = 32.dp,
    textSize: Int = 22
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrueTagLogoMark(size = markSize)
        Spacer(modifier = Modifier.width(10.dp))
        Row {
            Text(
                text = "True",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = textSize.sp,
                color = InkNavy
            )
            Text(
                text = "Tag",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = textSize.sp,
                color = TagRed
            )
        }
    }
}

// ============================================================================
// STAMP ANIMATION FOR CONFIDENCE BADGE
// Scaled at 1.4, rotated -8 deg -> 1.0, 0 deg with sharp spring!
// ============================================================================

@Composable
fun StampBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1.4f) }
    val rotation = remember { Animatable(-8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(text) {
        scale.snapTo(1.4f)
        rotation.snapTo(-8f)
        alpha.snapTo(0f)

        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        rotation.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, tween(120))
    }

    Surface(
        modifier = modifier
            .scale(scale.value)
            .rotate(rotation.value)
            .border(1.5.dp, contentColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Text(
            text = text.uppercase(),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

// ============================================================================
// ODOMETER MONOSPACE PRICE NUMBER
// Animated vertical rolling digits for numbers & prices
// ============================================================================

@Composable
fun OdometerPriceText(
    amount: Double,
    modifier: Modifier = Modifier,
    prefix: String = "$",
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: Int = 18,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val formatted = String.format("%.2f", amount)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = prefix,
            fontFamily = SpaceGrotesk,
            fontWeight = fontWeight,
            fontSize = fontSize.sp,
            color = color
        )
        formatted.forEach { char ->
            Text(
                text = char.toString(),
                fontFamily = IbmpPlexMono,
                fontWeight = fontWeight,
                fontSize = fontSize.sp,
                color = color
            )
        }
    }
}
