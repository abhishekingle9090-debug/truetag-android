package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.AuthHelper
import com.example.model.ChatMessage
import com.example.model.ShoppingTrip
import com.example.ui.components.SignInRequiredView
import com.example.ui.components.SwingTagCard
import com.example.ui.theme.ActionRed
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.IbmpPlexMono
import com.example.ui.theme.InkNavy
import com.example.ui.theme.PaperCream
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.TagRed
import com.example.ui.theme.WarmGray
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TripSortOption {
    DATE_DESC,
    STORE_ASC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedTripsScreen(
    trips: List<ShoppingTrip>,
    chatMessages: List<ChatMessage> = emptyList(),
    onRenameTrip: (String, String, String) -> Unit,
    onDuplicateTrip: (ShoppingTrip) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onOpenTripDetail: (ShoppingTrip) -> Unit = {},
    onSendChatMessage: (String) -> Unit = {},
    onClearChat: () -> Unit = {},
    navController: NavController
) {
    val context = LocalContext.current
    var renamingTrip by remember { mutableStateOf<ShoppingTrip?>(null) }
    var renameName by remember { mutableStateOf("") }
    var renameStore by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(TripSortOption.DATE_DESC) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val sortedTrips = remember(trips, sortOption) {
        when (sortOption) {
            TripSortOption.DATE_DESC -> trips.sortedByDescending { it.date }
            TripSortOption.STORE_ASC -> trips.sortedBy { it.storeName.lowercase() }
        }
    }

    BackHandler {
        navController.navigate("scanner") {
            popUpTo("scanner") { inclusive = true }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("saved_trips_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "saved trips",
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
                        modifier = Modifier.testTag("saved_trips_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showFilterMenu = true },
                            modifier = Modifier.testTag("filter_trips_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Date", fontFamily = SpaceGrotesk) },
                                onClick = {
                                    sortOption = TripSortOption.DATE_DESC
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Store", fontFamily = SpaceGrotesk) },
                                onClick = {
                                    sortOption = TripSortOption.STORE_ASC
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaperCream)
            )
        }
    ) { innerPadding ->
        if (!AuthHelper.isFullyAuthenticated) {
            SignInRequiredView(
                headline = "sign in to see your trips",
                explanation = "Your shopping trips, receipts, and tax records are saved securely with your TrueTag account.",
                onSignInClick = { navController.navigate("sign_in") },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else if (sortedTrips.isEmpty()) {
            EmptySavedTripsView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onGoToCart = {
                    navController.navigate("cart") {
                        popUpTo("scanner") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(sortedTrips, key = { _, trip -> trip.id }) { index, trip ->
                    var isVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(trip.id) {
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
                        TripCardRow(
                            trip = trip,
                            onClick = { onOpenTripDetail(trip) },
                            onRename = {
                                renameName = trip.name
                                renameStore = trip.storeName
                                renamingTrip = trip
                            },
                            onDuplicate = {
                                onDuplicateTrip(trip)
                                Toast.makeText(context, "Trip duplicated", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                onDeleteTrip(trip.id)
                                Toast.makeText(context, "Trip deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Rename Dialog
    if (renamingTrip != null) {
        AlertDialog(
            onDismissRequest = { renamingTrip = null },
            title = {
                Text(
                    text = "Rename Trip",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = renameName,
                        onValueChange = { renameName = it },
                        label = { Text("Trip Name", fontFamily = SpaceGrotesk) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = renameStore,
                        onValueChange = { renameStore = it },
                        label = { Text("Store Name", fontFamily = SpaceGrotesk) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = renamingTrip
                        if (t != null) {
                            onRenameTrip(t.id, renameName.trim(), renameStore.trim())
                        }
                        renamingTrip = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TagRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", fontFamily = SpaceGrotesk, color = PaperSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingTrip = null }) {
                    Text("Cancel", fontFamily = SpaceGrotesk, color = InkNavy)
                }
            },
            containerColor = PaperSurface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripCardRow(
    trip: ShoppingTrip,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(trip.date))
    val displayName = if (trip.storeName.isNotBlank()) trip.storeName else (if (trip.name.isNotBlank()) trip.name else "unnamed trip")

    Box {
        // Swing tag shape card
        SwingTagCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .testTag("trip_card_${trip.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: store name or unnamed trip bold, with date and item count as caption beneath
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$formattedDate · ${trip.items.size} ${if (trip.items.size == 1) "item" else "items"}",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.sp,
                        color = WarmGray
                    )
                }

                // Right: total in IBM Plex Mono bold, tax amount as smaller secondary line beneath
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = trip.formattedTotalTruePrice,
                        fontFamily = IbmpPlexMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ForestGreen
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "tax: ${trip.formattedTotalTaxAmount}",
                        fontFamily = IbmpPlexMono,
                        fontSize = 11.sp,
                        color = WarmGray
                    )
                }
            }
        }

        // Long Press Context Menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Rename", fontFamily = SpaceGrotesk) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = InkNavy) },
                onClick = {
                    showMenu = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text("Duplicate", fontFamily = SpaceGrotesk) },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = InkNavy) },
                onClick = {
                    showMenu = false
                    onDuplicate()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", fontFamily = SpaceGrotesk, color = ActionRed) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ActionRed) },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}

/**
 * Empty state with illustration, message "no saved trips yet",
 * subtext "save a cart to keep a record of your shop", button "go to cart".
 */
@Composable
private fun EmptySavedTripsView(
    modifier: Modifier = Modifier,
    onGoToCart: () -> Unit
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
            // Receipt / Tag flat line illustration
            Canvas(modifier = Modifier.size(90.dp)) {
                val w = size.width
                val h = size.height

                // Receipt silhouette
                val receipt = Path().apply {
                    moveTo(w * 0.24f, h * 0.12f)
                    lineTo(w * 0.76f, h * 0.12f)
                    lineTo(w * 0.76f, h * 0.84f)
                    lineTo(w * 0.66f, h * 0.78f)
                    lineTo(w * 0.56f, h * 0.84f)
                    lineTo(w * 0.46f, h * 0.78f)
                    lineTo(w * 0.36f, h * 0.84f)
                    lineTo(w * 0.24f, h * 0.78f)
                    close()
                }
                drawPath(path = receipt, color = TagRed.copy(alpha = 0.10f))
                drawPath(path = receipt, color = TagRed, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

                // Receipt lines
                drawLine(InkNavy, Offset(w * 0.35f, h * 0.30f), Offset(w * 0.65f, h * 0.30f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(InkNavy, Offset(w * 0.35f, h * 0.45f), Offset(w * 0.65f, h * 0.45f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(InkNavy, Offset(w * 0.35f, h * 0.60f), Offset(w * 0.55f, h * 0.60f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "no saved trips yet",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "save a cart to keep a record of your shop",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = WarmGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onGoToCart,
                modifier = Modifier
                    .height(48.dp)
                    .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                    .testTag("go_to_cart_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TagRed,
                    contentColor = PaperSurface
                )
            ) {
                Text(
                    text = "go to cart",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
