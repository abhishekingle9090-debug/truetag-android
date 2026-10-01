package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.AuthHelper
import com.example.data.CategoryTaxEngine
import com.example.data.ItemCategory
import com.example.model.ScannedItem
import com.example.model.TaxLocation
import com.example.ui.components.DashedDivider
import com.example.ui.components.SignInInterceptBottomSheet
import com.example.ui.components.StampBadge
import com.example.ui.components.SwingTagCard
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
import com.example.util.HapticHelper
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    item: ScannedItem?,
    taxLocation: TaxLocation?,
    isTestLocation: Boolean = false,
    isFromGallery: Boolean = false,
    onAddToCart: (ScannedItem) -> Unit,
    onSaveTrip: (String, String) -> Unit = { _, _ -> },
    onRequireSignIn: ((ScannedItem) -> Unit)? = null,
    navController: NavController
) {
    val context = LocalContext.current

    BackHandler {
        navController.navigate("scanner") {
            popUpTo("scanner") { inclusive = true }
        }
    }

    // FAILURE STATE: If no valid item was parsed
    if (item == null) {
        ResultFailureScreen(onTryAgain = {
            navController.navigate("scanner") {
                popUpTo("scanner") { inclusive = true }
            }
        })
        return
    }

    // Editable item details
    var currentItemName by remember(item.id) { mutableStateOf(item.name) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf(currentItemName) }
    var isBookmarked by remember { mutableStateOf(false) }
    var addedToCart by remember { mutableStateOf(false) }
    var showAboutDataDialog by remember { mutableStateOf(false) }
    var showSignInInterceptSheet by remember { mutableStateOf(false) }

    // Category Selector: General, Groceries, Clothing
    var selectedCategory by remember(item.id) {
        val initial = when (item.category.lowercase()) {
            "groceries", "grocery", "food" -> ItemCategory.GROCERIES
            "clothing", "apparel", "clothes" -> ItemCategory.CLOTHING
            else -> ItemCategory.GENERAL
        }
        mutableStateOf(initial)
    }

    // Live Category-Aware Tax Calculation
    val taxCalculation = remember(item.tagPrice, taxLocation, selectedCategory) {
        CategoryTaxEngine.calculate(item.tagPrice, taxLocation, selectedCategory)
    }

    // Neighboring Border Savings Tip
    val borderTip = remember(item.tagPrice, taxLocation, selectedCategory) {
        CategoryTaxEngine.getBorderSavingsTip(item.tagPrice, taxLocation, selectedCategory)
    }

    // Receipt Print-Up Slide Transition
    var hasAnimatedEntrance by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) {
        hasAnimatedEntrance = true
    }

    // Confidence tier
    val confidenceScore = item.insight?.tier?.ordinal ?: 1
    val (confidenceText, confidenceBg, confidenceFg) = when {
        item.taxRate >= 0.0 && item.tagPrice > 0 -> Triple("Verified Match", ForestGreen.copy(alpha = 0.15f), ForestGreen)
        confidenceScore == 0 -> Triple("High Confidence", ForestGreen.copy(alpha = 0.15f), ForestGreen)
        confidenceScore == 1 -> Triple("Standard Read", MarkerYellow.copy(alpha = 0.25f), InkNavy)
        else -> Triple("Manual Check", TagRed.copy(alpha = 0.15f), TagRed)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("result_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "result",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("scanner") {
                                popUpTo("scanner") { inclusive = true }
                            }
                        },
                        modifier = Modifier.testTag("result_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Scanner",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isBookmarked = !isBookmarked
                            if (isBookmarked) {
                                onSaveTrip(currentItemName, taxLocation?.city ?: "Retail Store")
                                Toast.makeText(context, "Saved to Trips", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("result_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark Trip",
                            tint = if (isBookmarked) TagRed else MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaperCream
                )
            )
        },
        bottomBar = {
            // Stacked Buttons: Primary filled Tag Red, Secondary outlined Ink Navy
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.background,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Filled Tag Red
                    Button(
                        onClick = {
                            val updatedItem = item.copy(
                                name = currentItemName,
                                category = selectedCategory.displayName,
                                taxRate = taxCalculation.taxRate
                            )
                            if (!AuthHelper.isFullyAuthenticated) {
                                showSignInInterceptSheet = true
                            } else {
                                onAddToCart(updatedItem)
                                addedToCart = true
                                HapticHelper.playScanSuccess(context)
                                Toast.makeText(context, "Added $currentItemName to Cart", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !addedToCart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                            .testTag("add_to_cart_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TagRed,
                            contentColor = PaperSurface,
                            disabledContainerColor = TagRed.copy(alpha = 0.5f),
                            disabledContentColor = PaperSurface.copy(alpha = 0.8f)
                        )
                    ) {
                        Text(
                            text = if (addedToCart) "added to cart ✓" else "add to cart",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Secondary Outlined Ink Navy
                    OutlinedButton(
                        onClick = {
                            navController.navigate("scanner") {
                                popUpTo("scanner") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(1.dp, InkNavy, RoundedCornerShape(14.dp))
                            .testTag("scan_next_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = InkNavy
                        )
                    ) {
                        Text(
                            text = "scan next",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = InkNavy
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source Caption (Test Location / Gallery)
            if (isTestLocation || isFromGallery) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MarkerYellow.copy(alpha = 0.20f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MarkerYellow.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = InkNavy,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTestLocation) "Source: Test Location Mode (${taxLocation?.displayCityState ?: "Demo"})" else "Source: Uploaded Gallery Image",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = InkNavy
                        )
                    }
                }
            }

            // Thumbnail (64dp square 12dp radius) + Editable Item Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 64dp square thumbnail
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(36.dp)) {
                        val w = size.width
                        val h = size.height
                        // Small tag graphic
                        val path = Path().apply {
                            moveTo(w * 0.45f, h * 0.15f)
                            lineTo(w * 0.55f, h * 0.15f)
                            lineTo(w * 0.85f, h * 0.35f)
                            lineTo(w * 0.85f, h * 0.85f)
                            lineTo(w * 0.15f, h * 0.85f)
                            lineTo(w * 0.15f, h * 0.35f)
                            close()
                        }
                        drawPath(path = path, color = TagRed)
                        drawCircle(color = PaperCream, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.28f))
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Item Name with Pencil
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ITEM DESCRIPTION",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = WarmGray
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                editNameInput = currentItemName
                                showEditNameDialog = true
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = currentItemName,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Item Name",
                            tint = InkNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Category Selector Chips: General, Groceries, Clothing (10dp radius)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ITEM CATEGORY",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = WarmGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ItemCategory.values().forEach { category ->
                        val isSelected = category == selectedCategory
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TagRed else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF9E332D) else InkNavy
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable {
                                    selectedCategory = category
                                }
                                .testTag("category_chip_${category.name.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = category.displayName,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) PaperSurface else InkNavy
                                )
                            }
                        }
                    }
                }
            }

            // Neighboring Border Savings Tip (Highlight Yellow Tint)
            if (borderTip != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MarkerYellow.copy(alpha = 0.20f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MarkerYellow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("border_savings_tip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = InkNavy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = borderTip.message,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Main Swing Tag Price Breakdown Card (Punched Hole Cutout)
            SwingTagCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("price_breakdown_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Row 1: Tag Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shelf Tag Price",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = WarmGray
                        )
                        Text(
                            text = String.format("$%.2f", item.tagPrice),
                            fontFamily = IbmpPlexMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("tag_price_text")
                        )
                    }

                    DashedDivider()

                    // Row 2: Tax Rate for Category & State
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${taxLocation?.state ?: "US"} Sales Tax (${selectedCategory.displayName})",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = WarmGray
                            )
                            Text(
                                text = taxCalculation.ruleExplanation,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = WarmGray.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = taxCalculation.ratePercentageFormatted,
                            fontFamily = IbmpPlexMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DashedDivider()

                    // Row 3: Tax Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ Tax Amount",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = WarmGray
                        )
                        Text(
                            text = "+ " + String.format("$%.2f", taxCalculation.taxAmount),
                            fontFamily = IbmpPlexMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DashedDivider()

                    // Row 4: Real Price (Display Type Scale: 44sp Space Grotesk Integer + IBM Plex Mono Decimals)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Real Checkout Price",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "What register actually rings up",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = WarmGray
                            )
                        }

                        // Split dollar & cents for bespoke typography hierarchy
                        val totalStr = String.format("%.2f", taxCalculation.realPrice)
                        val parts = totalStr.split(".")
                        val integerPart = parts.getOrNull(0) ?: "0"
                        val decimalPart = parts.getOrNull(1) ?: "00"

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.testTag("real_price_text")
                        ) {
                            Text(
                                text = "$$integerPart",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 44.sp,
                                lineHeight = 46.sp,
                                color = ForestGreen
                            )
                            Text(
                                text = ".$decimalPart",
                                fontFamily = IbmpPlexMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                lineHeight = 32.sp,
                                color = ForestGreen,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }
            }

            // Confidence Badge with Stamp Animation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StampBadge(
                    text = confidenceText,
                    containerColor = confidenceBg,
                    contentColor = confidenceFg,
                    modifier = Modifier.testTag("confidence_badge")
                )

                Text(
                    text = "Tax rate based on 2026 state data",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = WarmGray
                )
            }

            // About the Data Info Row Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAboutDataDialog = true }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "State averages and category rules are simplified · ",
                    fontFamily = SpaceGrotesk,
                    fontSize = 11.sp,
                    color = WarmGray
                )
                Text(
                    text = "About the data",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline,
                    color = InkNavy
                )
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Edit Item Description",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = editNameInput,
                    onValueChange = { editNameInput = it },
                    singleLine = true,
                    label = { Text("Product name", fontFamily = SpaceGrotesk) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TagRed,
                        cursorColor = TagRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_name_dialog_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameInput.isNotBlank()) {
                            currentItemName = editNameInput.trim()
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TagRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", fontFamily = SpaceGrotesk, color = PaperSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", fontFamily = SpaceGrotesk, color = InkNavy)
                }
            },
            containerColor = PaperSurface
        )
    }

    // About the Data Dialog
    if (showAboutDataDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDataDialog = false },
            title = {
                Text(
                    text = "About State Sales Tax Data",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = CategoryTaxEngine.ABOUT_THE_DATA_EXPLAINER,
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Groceries: Most states exempt unprepared food; selected states levy reduced municipal or state rates.\n• Clothing: Exempt outright in PA, NJ, MN, VT, and under thresholds in NY, MA, RI.\n• Border Tips: Computed via straight-line coordinate distance to tax-free or lower-tax neighboring jurisdictions.",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = WarmGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDataDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TagRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Got it", fontFamily = SpaceGrotesk, color = PaperSurface)
                }
            },
            containerColor = PaperSurface
        )
    }

    // Intercept Bottom Sheet for account gating
    if (showSignInInterceptSheet) {
        val updatedItem = item.copy(
            name = currentItemName,
            category = selectedCategory.displayName,
            taxRate = taxCalculation.taxRate
        )
        SignInInterceptBottomSheet(
            item = updatedItem,
            onDismiss = { showSignInInterceptSheet = false },
            onAuthSuccessAndAddToCart = { authenticatedItem ->
                showSignInInterceptSheet = false
                addedToCart = true
                onAddToCart(authenticatedItem)
                navController.navigate("cart") {
                    popUpTo("scanner")
                }
            },
            onNavigateToSignInFull = {
                showSignInInterceptSheet = false
                navController.navigate("sign_in")
            }
        )
    }
}

/**
 * Failure state replaces the price card with a flat line illustration of a torn or blank tag.
 */
@Composable
private fun ResultFailureScreen(onTryAgain: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperCream)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Flat line illustration of torn or blank tag
            Canvas(modifier = Modifier.size(100.dp)) {
                val w = size.width
                val h = size.height

                // Torn tag outline
                val tagPath = Path().apply {
                    moveTo(w * 0.35f, h * 0.15f)
                    lineTo(w * 0.65f, h * 0.15f)
                    lineTo(w * 0.85f, h * 0.35f)
                    lineTo(w * 0.85f, h * 0.70f)
                    // Torn bottom jagged line
                    lineTo(w * 0.75f, h * 0.65f)
                    lineTo(w * 0.65f, h * 0.75f)
                    lineTo(w * 0.50f, h * 0.68f)
                    lineTo(w * 0.35f, h * 0.75f)
                    lineTo(w * 0.15f, h * 0.68f)
                    lineTo(w * 0.15f, h * 0.35f)
                    close()
                }
                drawPath(path = tagPath, color = TagRed.copy(alpha = 0.15f))
                drawPath(path = tagPath, color = TagRed, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

                // Hole
                drawCircle(color = PaperCream, radius = 5.dp.toPx(), center = Offset(w * 0.5f, h * 0.28f))
                drawCircle(color = TagRed, radius = 5.dp.toPx(), center = Offset(w * 0.5f, h * 0.28f), style = Stroke(width = 2.dp.toPx()))

                // Blank question mark in tag body
                val questionPath = Path().apply {
                    moveTo(w * 0.42f, h * 0.42f)
                    cubicTo(w * 0.42f, h * 0.36f, w * 0.58f, h * 0.36f, w * 0.58f, h * 0.45f)
                    lineTo(w * 0.50f, h * 0.52f)
                }
                drawPath(path = questionPath, color = InkNavy, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(color = InkNavy, radius = 2.dp.toPx(), center = Offset(w * 0.50f, h * 0.58f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "this does not look like a price tag",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "point the camera at a shelf price or upload a photo of one",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = WarmGray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onTryAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                    .testTag("try_again_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TagRed,
                    contentColor = PaperSurface
                )
            ) {
                Text(
                    text = "try again",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
