package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InsightTier
import com.example.model.ScannedItem
import com.example.model.TaxLocation
import com.example.ui.theme.PriceAccent
import com.example.ui.theme.TaxMuted
import com.example.ui.theme.TrueGreen
import kotlinx.coroutines.delay

@Composable
fun ResultBottomSheet(
    visible: Boolean,
    item: ScannedItem?,
    taxLocation: TaxLocation?,
    errorMessage: String? = null,
    isTestLocation: Boolean = false,
    isFromGallery: Boolean = false,
    onAddToCart: (ScannedItem) -> Unit,
    onScanNext: () -> Unit,
    onSaveTrip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = 300f
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 200)
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                // Top drag handle & dismiss button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item != null && errorMessage == null) {
                            IconButton(
                                onClick = onSaveTrip,
                                modifier = Modifier.testTag("bookmark_trip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = "Save Trip",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("dismiss_sheet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // FAILURE STATE: No price detected or missing required data
                if (errorMessage != null || item == null || taxLocation == null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("ocr_failure_state"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "No price detected. Try a clearer photo of the tag.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ensure the sticker price is centered and clearly readable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onScanNext,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("try_again_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TrueGreen),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Try again", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    return@Surface
                }

                // SUCCESS STATE: Actual extracted price, actual tax rate, actual real price
                var showLine1 by remember(item.id) { mutableStateOf(false) }
                var showLine2 by remember(item.id) { mutableStateOf(false) }
                var showLine3 by remember(item.id) { mutableStateOf(false) }
                var showInsight by remember(item.id) { mutableStateOf(false) }

                LaunchedEffect(item.id) {
                    showLine1 = false
                    showLine2 = false
                    showLine3 = false
                    showInsight = false

                    delay(50)
                    showLine1 = true
                    delay(100)
                    showLine2 = true
                    delay(100)
                    showLine3 = true
                    delay(100)
                    showInsight = true
                }

                val alphaLine1 by animateFloatAsState(targetValue = if (showLine1) 1f else 0f, animationSpec = tween(200), label = "line1")
                val alphaLine2 by animateFloatAsState(targetValue = if (showLine2) 1f else 0f, animationSpec = tween(200), label = "line2")
                val alphaLine3 by animateFloatAsState(targetValue = if (showLine3) 1f else 0f, animationSpec = tween(200), label = "line3")
                val alphaInsight by animateFloatAsState(targetValue = if (showInsight) 1f else 0f, animationSpec = tween(200), label = "insight")

                if (taxLocation != null) {
                    Text(
                        text = taxLocation.displayChip,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Line 1: Tag price (large, bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alphaLine1),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isFromGallery) {
                            Text(
                                text = "From uploaded photo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TrueGreen
                            )
                        } else {
                            Text(
                                text = "Shelf Tag Price",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = item.formattedTagPrice,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Line 2: + $X.XX tax (muted, with tax rate shown)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alphaLine2)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ Estimated Tax (${taxLocation?.ratePercentageFormatted ?: "0%"})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TaxMuted
                        )
                        Text(
                            text = "+ ${item.formattedTaxAmount}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TaxMuted
                        )
                    }
                    if (isTestLocation) {
                        Text(
                            text = "Tax rate from test location.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE8A33D),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Line 3: = $X.XX real price (biggest, accent color)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alphaLine3),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Real Checkout Price",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "What register actually rings up",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "= ${item.formattedTruePrice}",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PriceAccent
                    )
                }

                // Smart Price Insights Card
                if (item.insight != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(alphaInsight)
                            .testTag("price_insight_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (item.insight.tier) {
                                InsightTier.GOOD_DEAL -> TrueGreen.copy(alpha = 0.12f)
                                InsightTier.HIGH_PRICE -> MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                                InsightTier.FAIR_PRICE -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.insight.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.insight.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons: "Scan Next" and "Add to Cart"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onScanNext,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("scan_next_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Scan Next", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onAddToCart(item) },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .testTag("add_to_cart_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrueGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Add to Cart", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
