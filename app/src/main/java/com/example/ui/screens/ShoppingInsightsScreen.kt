package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.RevenueCatHelper
import com.example.model.ScannedItem
import com.example.model.ShoppingTrip
import com.example.ui.components.SwingTagCard
import com.example.ui.theme.ActionRed
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.IbmpPlexMono
import com.example.ui.theme.InkNavy
import com.example.ui.theme.MarkerYellow
import com.example.ui.theme.PaperCream
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.ReceiptDivider
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.TagRed
import com.example.ui.theme.WarmGray
import java.util.Locale

@Composable
fun ShoppingInsightsScreen(
    navController: NavController,
    cartItems: List<ScannedItem>,
    savedTrips: List<ShoppingTrip>,
    budget: Double
) {
    val context = LocalContext.current
    val isPro = remember(context) { RevenueCatHelper.isProActive(context) }

    BackHandler {
        navController.popBackStack()
    }

    // Aggregated Metrics
    val totalSpend = remember(cartItems, savedTrips) {
        if (cartItems.isNotEmpty()) cartItems.sumOf { it.truePrice }
        else savedTrips.firstOrNull()?.totalTruePrice ?: 0.0
    }
    val totalTax = remember(cartItems, savedTrips) {
        if (cartItems.isNotEmpty()) cartItems.sumOf { it.taxAmount }
        else savedTrips.firstOrNull()?.totalTaxAmount ?: 0.0
    }
    val itemCount = remember(cartItems, savedTrips) {
        if (cartItems.isNotEmpty()) cartItems.size
        else savedTrips.firstOrNull()?.items?.size ?: 0
    }
    val avgItemPrice = remember(totalSpend, itemCount) {
        if (itemCount > 0) totalSpend / itemCount else 0.0
    }

    val budgetUsedPercent = remember(totalSpend, budget) {
        if (budget > 0) ((totalSpend / budget) * 100.0).coerceAtLeast(0.0) else 0.0
    }
    val remainingBudget = remember(totalSpend, budget) {
        budget - totalSpend
    }
    val isOverBudget = remainingBudget < 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperCream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("shopping_insights_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PaperSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { navController.popBackStack() }
                        .testTag("insights_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = InkNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "shopping insights",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isPro) {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "TrueTag Shopping Insights:\nTotal: $${String.format(Locale.US, "%.2f", totalSpend)}\nTax: $${String.format(Locale.US, "%.2f", totalTax)}\nItems: $itemCount\nAverage: $${String.format(Locale.US, "%.2f", avgItemPrice)}\nBudget: $${String.format(Locale.US, "%.2f", budget)}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Shopping Insights"))
                        },
                        modifier = Modifier.testTag("insights_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TagRed)
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!isPro) {
                // =============================================================
                // LOCKED PRO STATE
                // =============================================================
                SwingTagCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(TagRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TagRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "TrueTag Pro Feature",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Unlock deep shopping trip analytics, category breakdowns, smart budget alerts, and tax-saving summaries with TrueTag Pro.",
                            fontFamily = SpaceGrotesk,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            color = WarmGray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { navController.navigate("paywall") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TagRed, contentColor = PaperSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                                .testTag("insights_upgrade_button")
                        ) {
                            Text("unlock TrueTag Pro", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            } else {
                // =============================================================
                // UNLOCKED PRO DASHBOARD
                // =============================================================

                // 1. Hero Summary Card (Swing Tag style)
                SwingTagCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRIP SUMMARY",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = WarmGray
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MarkerYellow
                            ) {
                                Text(
                                    text = "PRO ACTIVE",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = InkNavy,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL CHECKOUT",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = WarmGray
                                )
                                Text(
                                    text = String.format(Locale.US, "$%.2f", totalSpend),
                                    fontFamily = IbmpPlexMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                    color = ForestGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "SALES TAX",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = WarmGray
                                )
                                Text(
                                    text = String.format(Locale.US, "$%.2f", totalTax),
                                    fontFamily = IbmpPlexMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = TagRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "ITEMS", fontFamily = SpaceGrotesk, fontSize = 11.sp, color = WarmGray)
                                Text(text = "$itemCount", fontFamily = IbmpPlexMono, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "AVERAGE / ITEM", fontFamily = SpaceGrotesk, fontSize = 11.sp, color = WarmGray)
                                Text(text = String.format(Locale.US, "$%.2f", avgItemPrice), fontFamily = IbmpPlexMono, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "EFFECTIVE TAX", fontFamily = SpaceGrotesk, fontSize = 11.sp, color = WarmGray)
                                val effRate = if (totalSpend > 0) (totalTax / totalSpend) * 100.0 else 0.0
                                Text(text = String.format(Locale.US, "%.1f%%", effRate), fontFamily = IbmpPlexMono, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Budget Analytics Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaperSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BUDGET HEALTH",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.US, "%.0f%% used", budgetUsedPercent),
                                fontFamily = IbmpPlexMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isOverBudget) ActionRed else ForestGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { (budgetUsedPercent / 100f).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isOverBudget) ActionRed else ForestGreen,
                            trackColor = PaperCream
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Budget", fontFamily = SpaceGrotesk, fontSize = 11.sp, color = WarmGray)
                                Text(String.format(Locale.US, "$%.2f", budget), fontFamily = IbmpPlexMono, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isOverBudget) "Budget exceeded by" else "Remaining",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 11.sp,
                                    color = if (isOverBudget) ActionRed else ForestGreen
                                )
                                Text(
                                    text = String.format(Locale.US, "$%.2f", Math.abs(remainingBudget)),
                                    fontFamily = IbmpPlexMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isOverBudget) ActionRed else ForestGreen
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Smart Tax & Price Trends
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaperSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Smart Tax Insights",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (totalTax == 0.0) {
                                "All items in this trip qualify for zero sales tax or were purchased in a tax-free jurisdiction."
                            } else {
                                "Your estimated tax rate reflects real-time state, county, and city sales tax ordinances. Tax exemptions on groceries and clothing are automatically applied where enacted by state law."
                            },
                            fontFamily = SpaceGrotesk,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = WarmGray
                        )
                    }
                }
            }
        }
    }
}
