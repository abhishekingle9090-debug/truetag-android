package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PriceAccent
import com.example.ui.theme.TrueGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val onComplete = {
        RevenueCatHelper.setOnboardingCompleted(context, true)
        navController.navigate("scanner") {
            popUpTo("onboarding") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar with Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TrueTag",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TrueGreen
                )

                if (pagerState.currentPage < 2) {
                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.testTag("skip_onboarding_button")
                    ) {
                        Text(
                            text = "Skip",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // Swipeable Cards Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("onboarding_pager")
            ) { page ->
                when (page) {
                    0 -> OnboardingCardOne()
                    1 -> OnboardingCardTwo()
                    2 -> OnboardingCardThree()
                }
            }

            // Bottom Navigation Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) TrueGreen else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                // Action Button: "Get Started" or "Next"
                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_primary_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrueGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage == 2) "Get Started" else "Next",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingCardOne() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Soft Illustration 1: Shelf tag vs Register receipt
        Card(
            modifier = Modifier
                .size(260.dp)
                .testTag("illustration_shelf_vs_register"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Shelf Price Tag (Left)
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.36f, h * 0.42f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                // Tag border
                drawRoundRect(
                    color = Color(0xFFD6DFD9),
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.36f, h * 0.42f),
                    style = Stroke(2f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                // Tag hole
                drawCircle(
                    color = Color(0xFFE0E0E0),
                    radius = 8f,
                    center = Offset(w * 0.30f, h * 0.28f)
                )

                // Register Screen / Receipt (Right)
                drawRoundRect(
                    color = TrueGreen.copy(alpha = 0.12f),
                    topLeft = Offset(w * 0.52f, h * 0.18f),
                    size = Size(w * 0.38f, h * 0.54f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawRoundRect(
                    color = TrueGreen,
                    topLeft = Offset(w * 0.52f, h * 0.18f),
                    size = Size(w * 0.38f, h * 0.54f),
                    style = Stroke(3f),
                    cornerRadius = CornerRadius(18f, 18f)
                )

                // Arrow from shelf tag to register
                val arrowPath = Path().apply {
                    moveTo(w * 0.43f, h * 0.45f)
                    lineTo(w * 0.49f, h * 0.45f)
                }
                drawPath(arrowPath, color = TrueGreen, style = Stroke(4f, cap = StrokeCap.Round))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Shelf says $10.\nRegister says $10.85.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "In the United States, sales tax is added at checkout, so the sticker price is never the real price.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun OnboardingCardTwo() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Soft Illustration 2: City pins with different tax rates
        Card(
            modifier = Modifier
                .size(260.dp)
                .testTag("illustration_city_taxes"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Skyline silhouettes
                val skylinePath = Path().apply {
                    moveTo(w * 0.1f, h * 0.72f)
                    lineTo(w * 0.25f, h * 0.72f)
                    lineTo(w * 0.25f, h * 0.45f)
                    lineTo(w * 0.38f, h * 0.45f)
                    lineTo(w * 0.38f, h * 0.35f)
                    lineTo(w * 0.50f, h * 0.35f)
                    lineTo(w * 0.50f, h * 0.52f)
                    lineTo(w * 0.65f, h * 0.52f)
                    lineTo(w * 0.65f, h * 0.40f)
                    lineTo(w * 0.75f, h * 0.40f)
                    lineTo(w * 0.75f, h * 0.60f)
                    lineTo(w * 0.90f, h * 0.60f)
                    lineTo(w * 0.90f, h * 0.72f)
                    close()
                }
                drawPath(skylinePath, color = TrueGreen.copy(alpha = 0.15f))

                // Pin 1 (Local store pin)
                drawCircle(color = TrueGreen, radius = 14f, center = Offset(w * 0.44f, h * 0.28f))
                drawLine(
                    color = TrueGreen,
                    start = Offset(w * 0.44f, h * 0.28f),
                    end = Offset(w * 0.44f, h * 0.38f),
                    strokeWidth = 3f
                )

                // Pin 2 (Nearby district)
                drawCircle(color = TrueGreen.copy(alpha = 0.7f), radius = 11f, center = Offset(w * 0.22f, h * 0.38f))

                // Pin 3 (Tax-free state)
                drawCircle(color = TrueGreen.copy(alpha = 0.4f), radius = 11f, center = Offset(w * 0.72f, h * 0.34f))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Because in the US, tax is added at checkout — and it changes by city.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "State, county, and municipal taxes combine into wildly different rates across every ZIP code.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun OnboardingCardThree() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Soft Illustration 3: Optical scan reticle with green checkmark
        Card(
            modifier = Modifier
                .size(260.dp)
                .testTag("illustration_camera_scan"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Scan frame
                drawRoundRect(
                    color = TrueGreen,
                    topLeft = Offset(w * 0.2f, h * 0.26f),
                    size = Size(w * 0.6f, h * 0.48f),
                    style = Stroke(3.5f),
                    cornerRadius = CornerRadius(20f, 20f)
                )

                // Laser scan line
                drawLine(
                    color = TrueGreen.copy(alpha = 0.7f),
                    start = Offset(w * 0.24f, h * 0.50f),
                    end = Offset(w * 0.76f, h * 0.50f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                // Green badge with checkmark
                drawCircle(
                    color = TrueGreen,
                    radius = 24f,
                    center = Offset(w * 0.5f, h * 0.50f)
                )
                val checkPath = Path().apply {
                    moveTo(w * 0.46f, h * 0.50f)
                    lineTo(w * 0.49f, h * 0.53f)
                    lineTo(w * 0.54f, h * 0.46f)
                }
                drawPath(checkPath, color = Color.White, style = Stroke(4f, cap = StrokeCap.Round))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Point your camera.\nSee the real price.\nNo surprises.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Instant on-device OCR calculates combined sales tax from bundled offline data in milliseconds.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}
