package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.LocationHelper
import com.example.data.RevenueCatHelper
import com.example.data.TaxRepository
import com.example.db.UserStatsEntity
import com.example.model.ChatMessage
import com.example.model.ScannedItem
import com.example.ui.components.TestLocationBanner
import com.example.ui.screens.CartScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PaywallScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SavedTripsScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SignUpScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TaxTrackerScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.InkNavy
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.TagRed
import com.example.ui.theme.TrueGreen
import com.example.ui.theme.TrueTagTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TrueTagApplication
        val repository = app.repository

        setContent {
            val userStats by repository.userStats.collectAsStateWithLifecycle(initialValue = UserStatsEntity())
            val darkTheme = when (userStats.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            TrueTagTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()

                val cartItems by repository.cartItems.collectAsStateWithLifecycle(initialValue = emptyList())
                val savedTrips by repository.savedTrips.collectAsStateWithLifecycle(initialValue = emptyList())
                val chatMessages by repository.chatMessages.collectAsStateWithLifecycle(initialValue = emptyList())
                val testLocation by LocationHelper.testLocationFlow.collectAsStateWithLifecycle(
                    initialValue = LocationHelper.getTestLocation(this@MainActivity)
                )

                var lastScannedItem by remember { mutableStateOf<ScannedItem?>(null) }
                var pendingAddItemAfterAuth by remember { mutableStateOf<ScannedItem?>(null) }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = currentRoute in listOf("scanner", "cart", "saved_trips", "profile")

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = PaperSurface,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == "scanner",
                                    onClick = {
                                        if (currentRoute != "scanner") {
                                            navController.navigate("scanner") {
                                                popUpTo("scanner") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner") },
                                    label = { Text("Scanner") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = TagRed,
                                        selectedTextColor = TagRed,
                                        indicatorColor = TagRed.copy(alpha = 0.15f),
                                        unselectedIconColor = InkNavy,
                                        unselectedTextColor = InkNavy
                                    ),
                                    modifier = Modifier.testTag("tab_scanner")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "cart",
                                    onClick = {
                                        if (currentRoute != "cart") {
                                            navController.navigate("cart") {
                                                popUpTo("scanner") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (cartItems.isNotEmpty()) {
                                                    Badge(containerColor = TagRed) {
                                                        Text("${cartItems.size}", color = Color.White)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                                        }
                                    },
                                    label = { Text("Cart") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = TagRed,
                                        selectedTextColor = TagRed,
                                        indicatorColor = TagRed.copy(alpha = 0.15f),
                                        unselectedIconColor = InkNavy,
                                        unselectedTextColor = InkNavy
                                    ),
                                    modifier = Modifier.testTag("tab_cart")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "saved_trips",
                                    onClick = {
                                        if (currentRoute != "saved_trips") {
                                            navController.navigate("saved_trips") {
                                                popUpTo("scanner") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Trips") },
                                    label = { Text("Trips") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = TagRed,
                                        selectedTextColor = TagRed,
                                        indicatorColor = TagRed.copy(alpha = 0.15f),
                                        unselectedIconColor = InkNavy,
                                        unselectedTextColor = InkNavy
                                    ),
                                    modifier = Modifier.testTag("tab_trips")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "profile",
                                    onClick = {
                                        if (currentRoute != "profile") {
                                            navController.navigate("profile") {
                                                popUpTo("scanner") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                    label = { Text("Profile") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = TagRed,
                                        selectedTextColor = TagRed,
                                        indicatorColor = TagRed.copy(alpha = 0.15f),
                                        unselectedIconColor = InkNavy,
                                        unselectedTextColor = InkNavy
                                    ),
                                    modifier = Modifier.testTag("tab_profile")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        // Persistent Amber Banner across every screen when Test Location Mode is ON (GAP 1)
                        TestLocationBanner(
                            location = testLocation,
                            onClose = {
                                LocationHelper.disableTestLocation(this@MainActivity)
                            }
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            NavHost(
                                navController = navController,
                                startDestination = "splash",
                                enterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { 300 },
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ) + fadeIn(tween(300))
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { -300 },
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ) + fadeOut(tween(300))
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { -300 },
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ) + fadeIn(tween(300))
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { 300 },
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ) + fadeOut(tween(300))
                                }
                            ) {
                                composable("splash") {
                                    SplashScreen(
                                        onSplashFinished = {
                                            val destination = if (RevenueCatHelper.isOnboardingCompleted(this@MainActivity)) {
                                                "scanner"
                                            } else {
                                                "welcome"
                                            }
                                            navController.navigate(destination) {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                composable("onboarding") {
                                    OnboardingScreen(navController = navController)
                                }

                                composable("scanner") {
                                    ScannerScreen(
                                        cartItems = cartItems,
                                        savedTrips = savedTrips,
                                        chatMessages = chatMessages,
                                        budget = userStats.budgetAmount,
                                        onAddToCart = { item ->
                                            coroutineScope.launch {
                                                repository.addCartItem(item)
                                            }
                                        },
                                        onSaveTrip = { tripName, storeName ->
                                            coroutineScope.launch {
                                                repository.saveCurrentCartAsTrip(tripName, storeName)
                                            }
                                        },
                                        onSaveReceiptTrip = { receipt ->
                                            coroutineScope.launch {
                                                repository.saveReceiptAsTrip(
                                                    storeName = receipt.storeName,
                                                    lineItems = receipt.items,
                                                    subtotal = receipt.subtotal,
                                                    tax = receipt.taxPaid,
                                                    total = receipt.total
                                                )
                                            }
                                        },
                                        onSendChatMessage = { text ->
                                            coroutineScope.launch {
                                                repository.addChatMessage(ChatMessage(text = text, isUser = true))
                                            }
                                        },
                                        onClearChat = {
                                            coroutineScope.launch {
                                                repository.clearChat()
                                            }
                                        },
                                        onScanSuccess = { item ->
                                            lastScannedItem = item
                                            navController.navigate("result")
                                        },
                                        navController = navController
                                    )
                                }

                                composable("result") {
                                    ResultScreen(
                                        item = lastScannedItem,
                                        taxLocation = LocationHelper.getCachedLocation(this@MainActivity),
                                        isTestLocation = LocationHelper.isTestLocationActive(this@MainActivity),
                                        onAddToCart = { item ->
                                            coroutineScope.launch {
                                                repository.addCartItem(item)
                                            }
                                        },
                                        onSaveTrip = { name, store ->
                                            coroutineScope.launch {
                                                repository.saveCurrentCartAsTrip(name, store)
                                            }
                                        },
                                        navController = navController
                                    )
                                }

                                composable("cart") {
                                    CartScreen(
                                        cartItems = cartItems,
                                        savedTrips = savedTrips,
                                        chatMessages = chatMessages,
                                        budget = userStats.budgetAmount,
                                        onUpdateItemName = { id, newName ->
                                            coroutineScope.launch {
                                                repository.updateCartItemName(id, newName)
                                            }
                                        },
                                        onRemoveItem = { id ->
                                            coroutineScope.launch {
                                                repository.removeCartItem(id)
                                            }
                                        },
                                        onClearCart = {
                                            coroutineScope.launch {
                                                repository.clearCart()
                                            }
                                        },
                                        onSaveTrip = { name, store ->
                                            coroutineScope.launch {
                                                repository.saveCurrentCartAsTrip(name, store)
                                            }
                                        },
                                        onSendChatMessage = { text ->
                                            coroutineScope.launch {
                                                repository.addChatMessage(ChatMessage(text = text, isUser = true))
                                            }
                                        },
                                        onClearChat = {
                                            coroutineScope.launch {
                                                repository.clearChat()
                                            }
                                        },
                                        onOpenMoneyCoach = {
                                            navController.popBackStack()
                                        },
                                        navController = navController
                                    )
                                }

                                composable("saved_trips") {
                                    SavedTripsScreen(
                                        trips = savedTrips,
                                        chatMessages = chatMessages,
                                        onRenameTrip = { id, name, store ->
                                            coroutineScope.launch {
                                                repository.renameTrip(id, name, store)
                                            }
                                        },
                                        onDuplicateTrip = { trip ->
                                            coroutineScope.launch {
                                                repository.duplicateTrip(trip)
                                            }
                                        },
                                        onDeleteTrip = { id ->
                                            coroutineScope.launch {
                                                repository.deleteTrip(id)
                                            }
                                        },
                                        onSendChatMessage = { text ->
                                            coroutineScope.launch {
                                                repository.addChatMessage(ChatMessage(text = text, isUser = true))
                                            }
                                        },
                                        onClearChat = {
                                            coroutineScope.launch {
                                                repository.clearChat()
                                            }
                                        },
                                        navController = navController
                                    )
                                }

                                composable("tax_tracker") {
                                    TaxTrackerScreen(
                                        trips = savedTrips,
                                        chatMessages = chatMessages,
                                        onSendChatMessage = { text ->
                                            coroutineScope.launch {
                                                repository.addChatMessage(ChatMessage(text = text, isUser = true))
                                            }
                                        },
                                        onClearChat = {
                                            coroutineScope.launch {
                                                repository.clearChat()
                                            }
                                        },
                                        navController = navController
                                    )
                                }

                                composable("paywall") {
                                    PaywallScreen(navController = navController)
                                }

                                composable("profile") {
                                    ProfileScreen(
                                        userStats = userStats,
                                        currentLocation = LocationHelper.getCachedLocation(this@MainActivity),
                                        onUpdateBudget = { newBudget ->
                                            coroutineScope.launch {
                                                repository.updateBudget(newBudget)
                                            }
                                        },
                                        onUpdateTheme = { mode ->
                                            coroutineScope.launch {
                                                repository.updateThemeMode(mode)
                                            }
                                        },
                                        onToggleDemoMode = { active ->
                                            coroutineScope.launch {
                                                repository.setDemoMode(active)
                                            }
                                        },
                                        onOpenCityPicker = {
                                            navController.navigate("scanner")
                                        },
                                        onClearAllData = {
                                            coroutineScope.launch {
                                                repository.resetAllDemoData(this@MainActivity)
                                            }
                                        },
                                        navController = navController
                                    )
                                }

                                composable("welcome") {
                                    WelcomeScreen(
                                        navController = navController,
                                        onContinueAsGuest = {
                                            RevenueCatHelper.setOnboardingCompleted(this@MainActivity, true)
                                            navController.navigate("scanner") {
                                                popUpTo("welcome") { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                composable("sign_in") {
                                    SignInScreen(
                                        navController = navController,
                                        onSignInSuccess = {
                                            RevenueCatHelper.setOnboardingCompleted(this@MainActivity, true)
                                            if (pendingAddItemAfterAuth != null) {
                                                val itemToAdd = pendingAddItemAfterAuth!!
                                                pendingAddItemAfterAuth = null
                                                coroutineScope.launch {
                                                    repository.addCartItem(itemToAdd)
                                                }
                                                navController.navigate("cart") {
                                                    popUpTo("scanner")
                                                }
                                            } else {
                                                navController.navigate("scanner") {
                                                    popUpTo("scanner") { inclusive = true }
                                                }
                                            }
                                        }
                                    )
                                }

                                composable("sign_up") {
                                    SignUpScreen(
                                        navController = navController,
                                        onSignUpSuccess = {
                                            RevenueCatHelper.setOnboardingCompleted(this@MainActivity, true)
                                            if (pendingAddItemAfterAuth != null) {
                                                val itemToAdd = pendingAddItemAfterAuth!!
                                                pendingAddItemAfterAuth = null
                                                coroutineScope.launch {
                                                    repository.addCartItem(itemToAdd)
                                                }
                                                navController.navigate("cart") {
                                                    popUpTo("scanner")
                                                }
                                            } else {
                                                navController.navigate("scanner") {
                                                    popUpTo("scanner") { inclusive = true }
                                                }
                                            }
                                        }
                                    )
                                }

                                composable("forgot_password") {
                                    ForgotPasswordScreen(navController = navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
