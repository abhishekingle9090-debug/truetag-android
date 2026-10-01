package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.TrueGreen
import com.example.util.HapticHelper

@Composable
fun BudgetBar(
    spent: Double,
    budget: Double,
    modifier: Modifier = Modifier
) {
    if (budget <= 0.0) return

    val context = LocalContext.current
    val fraction = (spent / budget).toFloat().coerceIn(0f, 1f)
    val isNearBudget = fraction >= 0.75f && fraction < 1.0f
    val isOverBudget = spent > budget

    // Subtle shake animation if over budget
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isOverBudget) {
        if (isOverBudget) {
            HapticHelper.playBudgetWarning(context)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -8f at 80
                    8f at 160
                    -6f at 240
                    6f at 320
                    0f at 400
                }
            )
        }
    }

    val barColor = when {
        isOverBudget -> ErrorRed
        isNearBudget -> WarningAmber
        else -> TrueGreen
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = shakeOffset.value.dp)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shopping Budget",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "$${String.format("%.2f", spent)} / $${String.format("%.2f", budget)}" +
                        if (isOverBudget) " (Over!)" else "",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress track & fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}
