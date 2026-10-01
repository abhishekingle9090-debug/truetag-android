package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.AuthHelper
import com.example.data.RevenueCatHelper
import com.example.ui.components.TrueTagLogoMark
import com.example.ui.theme.IbmpPlexMono
import com.example.ui.theme.InkNavy
import com.example.ui.theme.MarkerYellow
import com.example.ui.theme.PaperCream
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.ReceiptDivider
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.TagRed
import com.example.ui.theme.WarmGray
import com.revenuecat.purchases.models.StoreProduct
import kotlinx.coroutines.launch

enum class SubscriptionPlan {
    MONTHLY,
    YEARLY
}

@Composable
fun PaywallScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var storeProduct by remember { mutableStateOf<StoreProduct?>(null) }
    var isLoadingProduct by remember { mutableStateOf(true) }
    var isPurchasing by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.YEARLY) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAuthRequiredDialog by remember { mutableStateOf(false) }

    BackHandler {
        navController.popBackStack()
    }

    LaunchedEffect(Unit) {
        if (!AuthHelper.isFullyAuthenticated) {
            showAuthRequiredDialog = true
        }
        val prod = RevenueCatHelper.fetchProProduct()
        storeProduct = prod
        isLoadingProduct = false
    }

    // Dynamic pricing from RevenueCat with authentic fallbacks
    val monthlyPrice = remember(storeProduct) {
        storeProduct?.price?.formatted ?: "$2.99 / mo"
    }
    val yearlyPrice = "$19.99 / yr"

    // Hero Logo Breathing Scale Animation (looping 1.0f to 1.08f)
    val infiniteTransition = rememberInfiniteTransition(label = "hero_breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperCream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("paywall_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close 'x' top left on a plain background
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PaperSurface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .testTag("paywall_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = InkNavy,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hero Section: Logo mark with breathing animation
            Box(
                modifier = Modifier
                    .scale(breathingScale)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                TrueTagLogoMark(size = 76.dp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Headline: "go pro" in display type treatment at ~32sp
            Text(
                text = "go pro",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtext
            Text(
                text = "unlimited scans, saved trips, and a monthly tax tracker",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = WarmGray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Feature List: 4 rows each with checkmark icon in tag red
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PaperSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PaywallFeatureRow(text = "unlimited scans")
                    PaywallFeatureRow(text = "saved shopping trips")
                    PaywallFeatureRow(text = "monthly tax paid tracker")
                    PaywallFeatureRow(text = "priority support")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Plan Selector: Two cards side by side (Monthly and Yearly)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Monthly Card
                val isMonthly = selectedPlan == SubscriptionPlan.MONTHLY
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PaperSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isMonthly) 2.dp else 1.dp,
                        color = if (isMonthly) TagRed else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedPlan = SubscriptionPlan.MONTHLY }
                        .testTag("plan_monthly_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MONTHLY",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp,
                            color = WarmGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = monthlyPrice,
                            fontFamily = IbmpPlexMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "billed monthly",
                            fontFamily = SpaceGrotesk,
                            fontSize = 11.sp,
                            color = WarmGray
                        )
                    }
                }

                // Yearly Card
                val isYearly = selectedPlan == SubscriptionPlan.YEARLY
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PaperSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isYearly) 2.dp else 1.dp,
                        color = if (isYearly) TagRed else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedPlan = SubscriptionPlan.YEARLY }
                        .testTag("plan_yearly_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Marker Yellow Highlight Badge: "save 40 percent"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MarkerYellow,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "SAVE 40 PERCENT",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.4.sp,
                                color = InkNavy,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "YEARLY",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp,
                            color = WarmGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = yearlyPrice,
                            fontFamily = IbmpPlexMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$1.66 / month",
                            fontFamily = SpaceGrotesk,
                            fontSize = 11.sp,
                            color = WarmGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Button: "start pro" in full width Tag Red (14dp radius, 1dp darker red border)
            Button(
                onClick = {
                    if (!AuthHelper.isFullyAuthenticated) {
                        showAuthRequiredDialog = true
                        return@Button
                    }
                    val activity = context as? Activity
                    if (activity != null) {
                        isPurchasing = true
                        coroutineScope.launch {
                            val success = RevenueCatHelper.purchasePro(activity, storeProduct)
                            isPurchasing = false
                            if (success) {
                                Toast.makeText(context, "Welcome to TrueTag Pro!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                    }
                },
                enabled = !isPurchasing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                    .testTag("start_pro_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TagRed,
                    contentColor = PaperSurface
                )
            ) {
                if (isPurchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = PaperSurface
                    )
                } else {
                    Text(
                        text = "start pro",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption: "cancel anytime"
            Text(
                text = "cancel anytime",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = WarmGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Row: Two small text links ("restore purchases" and "terms and privacy")
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "restore purchases",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.Underline,
                    color = InkNavy,
                    modifier = Modifier
                        .clickable {
                            val activity = context as? Activity
                            if (activity != null) {
                                isRestoring = true
                                coroutineScope.launch {
                                    val restored = RevenueCatHelper.restorePurchases(activity)
                                    isRestoring = false
                                    if (restored) {
                                        Toast.makeText(context, "Purchases restored successfully!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(context, "No active subscription found.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        .testTag("restore_purchases_link")
                )

                Text(
                    text = "terms and privacy",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.Underline,
                    color = InkNavy,
                    modifier = Modifier
                        .clickable { showTermsDialog = true }
                        .testTag("terms_privacy_link")
                )
            }
        }
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text(
                    text = "Terms & Privacy Policy",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TrueTag subscriptions are billed via RevenueCat Test Store. Subscriptions renew automatically unless cancelled before the end of the billing period.",
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "We do not store your camera images or photos on any remote server; all OCR processing takes place entirely on device via ML Kit.",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.sp,
                        color = WarmGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TagRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK", fontFamily = SpaceGrotesk, color = PaperSurface)
                }
            },
            containerColor = PaperSurface
        )
    }

    if (showAuthRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showAuthRequiredDialog = false },
            title = {
                Text(
                    text = "Sign In Required",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "An account is required to purchase TrueTag Pro so your pro membership is permanently saved and tied to your identity.",
                    fontFamily = SpaceGrotesk,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = WarmGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAuthRequiredDialog = false
                        navController.navigate("sign_in")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TagRed, contentColor = PaperSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("paywall_auth_signin_button")
                ) {
                    Text("Sign In", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAuthRequiredDialog = false },
                    modifier = Modifier.testTag("paywall_auth_cancel_button")
                ) {
                    Text("Cancel", fontFamily = SpaceGrotesk, color = WarmGray)
                }
            },
            containerColor = PaperSurface
        )
    }
}

@Composable
private fun PaywallFeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(TagRed.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = TagRed,
                modifier = Modifier.size(14.dp)
            )
        }

        Text(
            text = text,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
