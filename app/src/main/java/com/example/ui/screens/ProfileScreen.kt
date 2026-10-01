package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.AuthHelper
import com.example.data.CategoryTaxEngine
import com.example.data.LocationHelper
import com.example.data.RevenueCatHelper
import com.example.db.UserStatsEntity
import com.example.model.ShoppingTrip
import com.example.model.TaxLocation
import com.example.ui.components.DashedDivider
import com.example.ui.components.OdometerPriceText
import com.example.ui.components.SignInRequiredView
import com.example.ui.components.SwingTagCard
import com.example.ui.components.TestLocationPickerDialog
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userStats: UserStatsEntity,
    currentLocation: TaxLocation?,
    savedTrips: List<ShoppingTrip> = emptyList(),
    onUpdateBudget: (Double) -> Unit,
    onUpdateTheme: (String) -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onOpenCityPicker: () -> Unit,
    onClearAllData: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val currentUser = AuthHelper.getCurrentUser()
    val isGuest = currentUser == null || currentUser.isAnonymous

    // Dialog & Sheet States
    var showBudgetSheet by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(userStats.budgetAmount.toInt().toString()) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTestLocationPicker by remember { mutableStateOf(false) }
    var testModeEnabled by remember { mutableStateOf(LocationHelper.isTestLocationActive(context)) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showAboutDataDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }

    // Tax Paid This Month: calculated by summing tax of items across trips in current calendar month
    val taxPaidThisMonth = remember(savedTrips) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        savedTrips.filter { trip ->
            val tripCal = Calendar.getInstance().apply { timeInMillis = trip.date }
            tripCal.get(Calendar.MONTH) == currentMonth && tripCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.totalTaxAmount }
    }

    BackHandler {
        navController.navigate("scanner") {
            popUpTo("scanner") { inclusive = true }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "profile",
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
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaperCream)
            )
        }
    ) { innerPadding ->
        if (!AuthHelper.isFullyAuthenticated) {
            SignInRequiredView(
                headline = "sign in to manage your account",
                explanation = "Create an account or sign in to access your stats, budget settings, and TrueTag Pro membership.",
                onSignInClick = { navController.navigate("sign_in") },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // =================================================================
            // 1. ACCOUNT CARD IN SWING TAG SHAPE
            // =================================================================
            item {
                SwingTagCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_account_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 56dp Avatar Circle with Ink Navy Fill
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(InkNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = if (!isGuest && !currentUser?.displayName.isNullOrBlank()) {
                                currentUser?.displayName!!.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                            } else if (!isGuest && !currentUser?.email.isNullOrBlank()) {
                                currentUser?.email!!.take(2).uppercase()
                            } else {
                                "TT"
                            }
                            Text(
                                text = initials,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = PaperSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        if (isGuest) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "sign in to save your trips",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Guest Session Active",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 12.sp,
                                    color = WarmGray
                                )
                            }
                            Button(
                                onClick = { navController.navigate("sign_in") },
                                colors = ButtonDefaults.buttonColors(containerColor = TagRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Sign In", fontFamily = SpaceGrotesk, fontSize = 12.sp, color = PaperSurface)
                            }
                        } else {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "TrueTag Member",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentUser?.email ?: "account@truetag.app",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 13.sp,
                                    color = WarmGray
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 2. STATS CARD: 4 COLUMNS WITH ROLLING ODOMETER EFFECT
            // =================================================================
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PaperSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_stats_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Col 1: Total Scans
                        StatColumn(
                            value = "${userStats.totalScans}",
                            label = "TOTAL SCANS"
                        )

                        // Divider
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(ReceiptDivider))

                        // Col 2: Total Saved
                        StatColumn(
                            value = "$${String.format("%.0f", userStats.totalTaxSavedOrTracked)}",
                            label = "TOTAL SAVED"
                        )

                        // Divider
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(ReceiptDivider))

                        // Col 3: Streak
                        StatColumn(
                            value = "${userStats.streakDays}d",
                            label = "STREAK"
                        )

                        // Divider
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(ReceiptDivider))

                        // Col 4: Tax Paid This Month
                        StatColumn(
                            value = "$${String.format("%.2f", taxPaidThisMonth)}",
                            label = "TAX THIS MO"
                        )
                    }
                }
            }

            // =================================================================
            // 3. SETTINGS SECTION (PLAIN ROUNDED RECTANGLE CARDS 16DP)
            // =================================================================
            item {
                Text(
                    text = "SETTINGS",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = WarmGray
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PaperSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Permanent TrueTag Pro Row (Amazon Prime / Flipkart Plus persistent membership entry)
                        val isPro = RevenueCatHelper.isProActive(context)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("paywall") }
                                .padding(16.dp)
                                .testTag("truetag_pro_row"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "TrueTag Pro",
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isPro) ForestGreen.copy(alpha = 0.15f) else MarkerYellow,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPro) ForestGreen else MarkerYellow)
                                    ) {
                                        Text(
                                            text = if (isPro) "ACTIVE" else "PRO",
                                            fontFamily = SpaceGrotesk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp,
                                            color = if (isPro) ForestGreen else InkNavy,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPro) "Unlimited scans & advanced tax insights active" else "Unlimited shelf scans · Zero ads · Offline rates",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 12.sp,
                                    color = if (isPro) ForestGreen else WarmGray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Pro Paywall",
                                tint = if (isPro) ForestGreen else TagRed
                            )
                        }

                        DashedDivider()

                        // Row 1: Test Location Mode with Sliding Tag Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showTestLocationPicker = true
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Test Location Mode",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val activeLoc = LocationHelper.getTestLocation(context)
                                Text(
                                    text = if (activeLoc != null) "${activeLoc.city}, ${activeLoc.state} (${activeLoc.ratePercentageFormatted})" else "Simulate any US city or ZIP code",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 12.sp,
                                    color = if (activeLoc != null) ForestGreen else WarmGray
                                )
                            }

                            // Sliding Tag Switch
                            TagSlidingSwitch(
                                checked = testModeEnabled,
                                onCheckedChange = { checked ->
                                    testModeEnabled = checked
                                    if (checked) {
                                        showTestLocationPicker = true
                                    } else {
                                        LocationHelper.disableTestLocation(context)
                                    }
                                }
                            )
                        }

                        DashedDivider()

                        // Row 2: Theme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThemeDialog = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Theme",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = userStats.themeMode.lowercase().replaceFirstChar { it.uppercase() },
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 12.sp,
                                    color = WarmGray
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = WarmGray)
                        }

                        DashedDivider()

                        // Row 3: Budget
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBudgetSheet = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Budget",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$${userStats.budgetAmount.toInt()}",
                                    fontFamily = IbmpPlexMono,
                                    fontSize = 12.sp,
                                    color = WarmGray
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = WarmGray)
                        }

                        DashedDivider()

                        // Row 4: Notifications Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Price & Trip Alerts",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (notificationsEnabled) "Enabled" else "Muted",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 12.sp,
                                    color = WarmGray
                                )
                            }
                            TagSlidingSwitch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it }
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 4. DATA SECTION (PLAIN TEXT ROWS)
            // =================================================================
            item {
                Text(
                    text = "DATA",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = WarmGray
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PaperSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Reset Demo Data",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetDialog = true }
                                .padding(16.dp)
                                .testTag("reset_demo_data_row")
                        )

                        DashedDivider()

                        Text(
                            text = "Clear All Trips",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onClearAllData()
                                    Toast.makeText(context, "All trips cleared", Toast.LENGTH_SHORT).show()
                                }
                                .padding(16.dp)
                                .testTag("clear_all_trips_row")
                        )

                        DashedDivider()

                        Text(
                            text = "Sign Out",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ActionRed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AuthHelper.signOut()
                                    navController.navigate("welcome") {
                                        popUpTo("scanner") { inclusive = true }
                                    }
                                }
                                .padding(16.dp)
                                .testTag("sign_out_row")
                        )
                    }
                }
            }

            // =================================================================
            // 5. ABOUT SECTION
            // =================================================================
            item {
                Text(
                    text = "ABOUT",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = WarmGray
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PaperSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // About Test Mode
                        Column {
                            Text(
                                text = "About Test Mode",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Test Location lets you demo TrueTag from anywhere by simulating a US ZIP code.",
                                fontFamily = SpaceGrotesk,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = WarmGray
                            )
                        }

                        DashedDivider()

                        // About the Data
                        Column(modifier = Modifier.clickable { showAboutDataDialog = true }) {
                            Text(
                                text = "About the Data",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Sourced from Tax Foundation & TaxJar 2026 published data with category exemptions.",
                                fontFamily = SpaceGrotesk,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = WarmGray
                            )
                        }

                        DashedDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showPolicyDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Privacy Policy & Terms", fontFamily = SpaceGrotesk, fontSize = 14.sp, color = InkNavy)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = WarmGray)
                        }

                        DashedDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Version", fontFamily = SpaceGrotesk, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("1.0.0 (Build 2026)", fontFamily = IbmpPlexMono, fontSize = 13.sp, color = WarmGray)
                        }

                        DashedDivider()

                        Text("Open Source Licenses", fontFamily = SpaceGrotesk, fontSize = 14.sp, color = InkNavy)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

    // Test Location Picker Dialog
    if (showTestLocationPicker) {
        TestLocationPickerDialog(
            currentTestLocation = LocationHelper.getTestLocation(context),
            initialKeepForNextLaunch = true,
            onSelectLocation = { location, _ ->
                LocationHelper.setTestLocation(context, location)
                testModeEnabled = true
                showTestLocationPicker = false
                Toast.makeText(context, "Test location set to ${location.displayCityState}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showTestLocationPicker = false }
        )
    }

    // Budget Bottom Sheet
    if (showBudgetSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showBudgetSheet = false },
            sheetState = sheetState,
            containerColor = PaperSurface
        ) {
            Column(modifier = Modifier.padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Set Monthly Budget", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Monthly budget amount ($)", fontFamily = SpaceGrotesk) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val parsed = budgetInput.toDoubleOrNull() ?: 100.0
                        onUpdateBudget(parsed)
                        showBudgetSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TagRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Save Budget", fontFamily = SpaceGrotesk, color = PaperSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("SYSTEM" to "System Default", "LIGHT" to "Warm Paper Cream (Light)", "DARK" to "Deep Warm Charcoal (Dark)").forEach { (mode, name) ->
                        Text(
                            text = name,
                            fontFamily = SpaceGrotesk,
                            fontWeight = if (userStats.themeMode == mode) FontWeight.Bold else FontWeight.Normal,
                            color = if (userStats.themeMode == mode) TagRed else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().clickable {
                                onUpdateTheme(mode)
                                showThemeDialog = false
                            }.padding(vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            containerColor = PaperSurface
        )
    }

    // Reset Demo Data Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Demo Data?", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold) },
            text = { Text("This will reset your local scans, shopping cart, trips, and test location override.", fontFamily = SpaceGrotesk) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        LocationHelper.disableTestLocation(context)
                        testModeEnabled = false
                        showResetDialog = false
                        Toast.makeText(context, "Demo data reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActionRed)
                ) {
                    Text("Reset", fontFamily = SpaceGrotesk, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", fontFamily = SpaceGrotesk, color = InkNavy)
                }
            },
            containerColor = PaperSurface
        )
    }

    // About Data Dialog
    if (showAboutDataDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDataDialog = false },
            title = { Text("About State Tax Data", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = CategoryTaxEngine.ABOUT_THE_DATA_EXPLAINER,
                    fontFamily = SpaceGrotesk,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(onClick = { showAboutDataDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = TagRed)) {
                    Text("Done", fontFamily = SpaceGrotesk, color = PaperSurface)
                }
            },
            containerColor = PaperSurface
        )
    }

    // Privacy Policy Dialog
    if (showPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            title = { Text("Privacy Policy & Terms", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "TrueTag processes price tags entirely on-device using ML Kit without uploading images. Location queries resolve state and local tax jurisdictions locally or via device GPS coordinates.",
                    fontFamily = SpaceGrotesk,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(onClick = { showPolicyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = TagRed)) {
                    Text("Close", fontFamily = SpaceGrotesk, color = PaperSurface)
                }
            },
            containerColor = PaperSurface
        )
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = IbmpPlexMono,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
            color = WarmGray
        )
    }
}

/**
 * Custom sliding switch styled as a small swing tag sliding horizontally rather than default Material switch.
 */
@Composable
private fun TagSlidingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 24f else 2f,
        animationSpec = tween(180),
        label = "switch_anim"
    )

    Box(
        modifier = Modifier
            .size(width = 50.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) TagRed else ReceiptDivider)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PaperSurface)
                .border(1.dp, if (checked) Color(0xFF9E332D) else WarmGray.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Tiny eyelet hole in the sliding tag thumb
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (checked) TagRed else WarmGray)
            )
        }
    }
}
