package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.AuthHelper
import com.example.data.CategoryTaxEngine
import com.example.data.ItemCategory
import com.example.data.LocationHelper
import com.example.data.RevenueCatHelper
import com.example.model.ChatMessage
import com.example.model.ScannedItem
import com.example.model.ShoppingTrip
import com.example.ui.components.SignInRequiredView
import com.example.ui.components.SwingTagCard
import com.example.ui.components.TornPaperHeader
import com.example.ui.theme.ActionRed
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.IbmpPlexMono
import com.example.ui.theme.InkNavy
import com.example.ui.theme.PaperCream
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.ReceiptDivider
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.TagRed
import com.example.ui.theme.WarmGray
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartItems: List<ScannedItem>,
    savedTrips: List<ShoppingTrip> = emptyList(),
    chatMessages: List<ChatMessage> = emptyList(),
    budget: Double,
    onUpdateItemName: (String, String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onSaveTrip: (String, String) -> Unit,
    onSendChatMessage: (String) -> Unit = {},
    onClearChat: () -> Unit = {},
    onOpenMoneyCoach: () -> Unit = {},
    navController: NavController
) {
    val context = LocalContext.current
    var editingItem by remember { mutableStateOf<ScannedItem?>(null) }
    var showSaveTripDialog by remember { mutableStateOf(false) }
    var tripNameInput by remember { mutableStateOf("Target Run") }
    var storeNameInput by remember { mutableStateOf("Target") }

    val totalTagPrice = remember(cartItems) { cartItems.sumOf { it.tagPrice } }
    val totalTaxAmount = remember(cartItems) { cartItems.sumOf { it.taxAmount } }
    val totalRealPrice = remember(cartItems) { cartItems.sumOf { it.truePrice } }

    BackHandler {
        navController.navigate("scanner") {
            popUpTo("scanner") { inclusive = true }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cart_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "your cart",
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
                        modifier = Modifier.testTag("cart_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (AuthHelper.isFullyAuthenticated && cartItems.isNotEmpty()) {
                        // Right-side Outlined Button: "save trip" in Ink Navy
                        OutlinedButton(
                            onClick = {
                                val isPro = RevenueCatHelper.isProActive(context)
                                if (!isPro && savedTrips.size >= 3) {
                                    navController.navigate("paywall")
                                } else {
                                    showSaveTripDialog = true
                                }
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, InkNavy),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = InkNavy
                            ),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .height(38.dp)
                                .testTag("top_save_trip_button")
                        ) {
                            Text(
                                text = "save trip",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = InkNavy
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaperCream)
            )
        },
        bottomBar = {
            if (AuthHelper.isFullyAuthenticated && cartItems.isNotEmpty()) {
                // Sticky bottom summary bar with 1dp top divider
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = PaperSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Subtotal and Tax stacked in IBM Plex Mono
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "subtotal: ",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 13.sp,
                                    color = WarmGray
                                )
                                Text(
                                    text = String.format("$%.2f", totalTagPrice),
                                    fontFamily = IbmpPlexMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "tax paid: ",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 13.sp,
                                    color = WarmGray
                                )
                                Text(
                                    text = "+ " + String.format("$%.2f", totalTaxAmount),
                                    fontFamily = IbmpPlexMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = WarmGray
                                )
                            }
                        }

                        // Right: Total in forest green real price color
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL WITH TAX",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = WarmGray
                            )
                            Text(
                                text = String.format("$%.2f", totalRealPrice),
                                fontFamily = IbmpPlexMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = ForestGreen,
                                modifier = Modifier.testTag("cart_total_price_text")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (!AuthHelper.isFullyAuthenticated) {
            SignInRequiredView(
                headline = "sign in to see your cart",
                explanation = "Items you scan and add to your cart are saved to your account so you can track your spending.",
                onSignInClick = { navController.navigate("sign_in") },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else if (cartItems.isEmpty()) {
            // Empty state centered vertically with flat line illustration
            EmptyCartView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onOpenScanner = {
                    navController.navigate("scanner") {
                        popUpTo("scanner") { inclusive = true }
                    }
                }
            )
        } else {
            // Staggered entrance item list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(cartItems, key = { _, item -> item.id }) { index, item ->
                    var isVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(item.id) {
                        delay(index * 40L)
                        isVisible = true
                    }

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(250)) + slideInVertically(
                            animationSpec = tween(250, easing = FastOutSlowInEasing),
                            initialOffsetY = { 30 }
                        )
                    ) {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onRemoveItem(item.id)
                                    Toast.makeText(context, "Removed ${item.name}", Toast.LENGTH_SHORT).show()
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(ActionRed)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        ) {
                            // Swing tag card shape for each row
                            SwingTagCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cart_item_card_${item.id}"),
                                onClick = {
                                    editingItem = item
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontFamily = SpaceGrotesk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "${item.formattedTagPrice} shelf · ${item.category} · +${item.formattedTaxAmount} tax",
                                            fontFamily = IbmpPlexMono,
                                            fontSize = 12.sp,
                                            color = WarmGray
                                        )
                                    }

                                    Text(
                                        text = item.formattedTruePrice,
                                        fontFamily = IbmpPlexMono,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = ForestGreen
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Torn paper top edge bottom sheet for item editing
    if (editingItem != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val currentEditItem = editingItem!!
        var editName by remember { mutableStateOf(currentEditItem.name) }
        var editCategory by remember { mutableStateOf(currentEditItem.category) }

        ModalBottomSheet(
            onDismissRequest = { editingItem = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
            containerColor = PaperSurface,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Torn Paper Top Edge
                TornPaperHeader(
                    backgroundColor = PaperSurface,
                    toothHeight = 6.dp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "edit item",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Item Name", fontFamily = SpaceGrotesk) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TagRed,
                            cursorColor = TagRed
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text(
                            text = "CATEGORY",
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
                            ItemCategory.values().forEach { cat ->
                                val selected = cat.displayName.equals(editCategory, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) TagRed else Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Color(0xFF9E332D) else InkNavy),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clickable { editCategory = cat.displayName }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = cat.displayName,
                                            fontFamily = SpaceGrotesk,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (selected) PaperSurface else InkNavy
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onRemoveItem(currentEditItem.id)
                                editingItem = null
                                Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, ActionRed),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("delete", fontFamily = SpaceGrotesk, color = ActionRed, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                onUpdateItemName(currentEditItem.id, editName.trim())
                                currentEditItem.name = editName.trim()
                                editingItem = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TagRed),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("save", fontFamily = SpaceGrotesk, color = PaperSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Save Trip Dialog
    if (showSaveTripDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTripDialog = false },
            title = {
                Text(
                    text = "Save Shopping Trip",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Store your ${cartItems.size} items to your shopping trip history.",
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.sp,
                        color = WarmGray
                    )
                    OutlinedTextField(
                        value = tripNameInput,
                        onValueChange = { tripNameInput = it },
                        label = { Text("Trip Name (e.g. Target Run)", fontFamily = SpaceGrotesk) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = storeNameInput,
                        onValueChange = { storeNameInput = it },
                        label = { Text("Store Name (e.g. Target)", fontFamily = SpaceGrotesk) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = tripNameInput.trim().ifBlank { "Shopping Trip" }
                        val store = storeNameInput.trim().ifBlank { "Retail Store" }
                        onSaveTrip(name, store)
                        showSaveTripDialog = false
                        Toast.makeText(context, "Trip saved to Trips tab!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TagRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Trip", fontFamily = SpaceGrotesk, color = PaperSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTripDialog = false }) {
                    Text("Cancel", fontFamily = SpaceGrotesk, color = InkNavy)
                }
            },
            containerColor = PaperSurface
        )
    }
}

/**
 * Empty state centered vertically with flat line illustration of empty swing tag.
 */
@Composable
private fun EmptyCartView(
    modifier: Modifier = Modifier,
    onOpenScanner: () -> Unit
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Flat line illustration of empty swing tag
            Canvas(modifier = Modifier.size(90.dp)) {
                val w = size.width
                val h = size.height

                val tagPath = Path().apply {
                    moveTo(w * 0.40f, h * 0.16f)
                    lineTo(w * 0.60f, h * 0.16f)
                    lineTo(w * 0.82f, h * 0.34f)
                    lineTo(w * 0.82f, h * 0.84f)
                    lineTo(w * 0.18f, h * 0.84f)
                    lineTo(w * 0.18f, h * 0.34f)
                    close()
                }
                drawPath(path = tagPath, color = TagRed.copy(alpha = 0.10f))
                drawPath(
                    path = tagPath,
                    color = TagRed,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Hole
                drawCircle(
                    color = TagRed,
                    radius = w * 0.055f,
                    center = Offset(w * 0.50f, h * 0.28f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // String hanging loop
                val stringPath = Path().apply {
                    moveTo(w * 0.50f, h * 0.22f)
                    cubicTo(w * 0.50f, h * 0.05f, w * 0.30f, h * 0.05f, w * 0.30f, h * 0.15f)
                }
                drawPath(
                    path = stringPath,
                    color = InkNavy,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "no items yet",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "scan a tag to begin",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = WarmGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary filled button: open scanner
            Button(
                onClick = onOpenScanner,
                modifier = Modifier
                    .height(48.dp)
                    .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                    .testTag("open_scanner_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TagRed,
                    contentColor = PaperSurface
                )
            ) {
                Text(
                    text = "open scanner",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
