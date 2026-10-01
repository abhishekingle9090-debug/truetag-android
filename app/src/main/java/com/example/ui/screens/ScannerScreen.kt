package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.AuthHelper
import com.example.data.GpsLocationState
import com.example.data.LocationHelper
import com.example.data.PriceInsightRepository
import com.example.data.ReceiptParser
import com.example.data.RevenueCatHelper
import com.example.data.TaxRepository
import com.example.model.ChatMessage
import com.example.model.ReceiptScanResult
import com.example.model.ScannedItem
import com.example.model.ShoppingTrip
import com.example.model.TaxLocation
import com.example.ui.components.CameraCaptureController
import com.example.ui.components.CameraPreviewView
import com.example.ui.components.CityPickerDialog
import com.example.ui.components.LocationDetailSheet
import com.example.ui.components.MoneyCoachSheet
import com.example.ui.components.ReceiptResultSheet
import com.example.ui.components.ResultBottomSheet
import com.example.ui.components.ScanOverlay
import com.example.ui.components.TestLocationPickerDialog
import com.example.ui.components.TornPaperTopEdge
import com.example.ui.components.rememberCameraCaptureController
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.InkNavy
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.ReceiptDivider
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.TagRed
import com.example.ui.theme.TrueGreen
import com.example.ui.theme.WarmGray
import com.example.util.HapticHelper
import com.example.util.OcrPriceExtractor
import com.example.util.ValidationResult
import com.example.util.ExtractionResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Rebuilt scanner screen with explicit user-triggered camera capture controls:
 * 1. Large 72dp shutter button at bottom center.
 * 2. 48dp gallery upload button to the left of the shutter.
 * 3. 48dp flash/torch toggle to the right of the shutter.
 * 4. Thin 80% scan frame overlay with corner brackets.
 * 5. Strict on-demand user-triggered processing only (zero continuous auto-scanning).
 * 6. Single unified OCR pipeline for both gallery and camera with zero default fallbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    cartItems: List<ScannedItem>,
    savedTrips: List<ShoppingTrip>,
    chatMessages: List<ChatMessage>,
    budget: Double,
    onAddToCart: (ScannedItem) -> Unit,
    onSaveTrip: (String, String) -> Unit,
    onSaveReceiptTrip: (ReceiptScanResult) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onScanSuccess: (ScannedItem) -> Unit = {},
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cameraController = rememberCameraCaptureController()

    // Location & GPS State
    var permissionSignal by remember { mutableLongStateOf(0L) }
    val gpsState by remember(permissionSignal) {
        LocationHelper.getLocationUpdates(context, permissionSignal)
    }.collectAsState(initial = GpsLocationState.Idle)
    val testModeRevision by LocationHelper.testModeRevision.collectAsState()

    val activeTaxLocation: TaxLocation? = remember(gpsState, testModeRevision, permissionSignal) {
        LocationHelper.getCachedLocation(context)
    }

    var showPermissionRationale by remember {
        mutableStateOf(!LocationHelper.hasLocationPermission(context))
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            showPermissionRationale = false
            permissionSignal = System.currentTimeMillis()
        }
    }

    // Camera Permission
    var hasCameraPermission by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (fine) showPermissionRationale = false

        val cam = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasCameraPermission = cam
        if (!cam) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Controls State
    var isTorchOn by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isResultFromGallery by remember { mutableStateOf(false) }

    // Dialog & Sheet States
    var showLocationDetailSheet by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    var showTestLocationDialog by remember { mutableStateOf(false) }
    var showMoneyCoachSheet by remember { mutableStateOf(false) }
    var showResultSheet by remember { mutableStateOf(false) }
    var showReceiptResultSheet by remember { mutableStateOf(false) }
    var showManualZipDialog by remember { mutableStateOf(false) }
    var manualZipInput by remember { mutableStateOf("") }

    var currentScannedItem by remember { mutableStateOf<ScannedItem?>(null) }
    var currentReceiptResult by remember { mutableStateOf<ReceiptScanResult?>(null) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }
    var showScanLimitAuthIntercept by remember { mutableStateOf(false) }

    val totalRealCartPrice = remember(cartItems) { cartItems.sumOf { it.truePrice } }

    /**
     * Unified OCR processing function called by both camera capture and gallery upload.
     * Operates purely on real pixels from bitmap. Zero default fallbacks.
     */
    val processBitmapForOcr: (Bitmap, Int, Boolean) -> Unit = { bitmap, rotationDegrees, fromGallery ->
        val isPro = RevenueCatHelper.isProActive(context)
        val dailyScans = RevenueCatHelper.getDailyScanCount(context)
        if (!isPro && dailyScans >= RevenueCatHelper.FREE_SCANS_LIMIT) {
            if (!AuthHelper.isFullyAuthenticated) {
                showScanLimitAuthIntercept = true
            } else {
                navController.navigate("paywall")
            }
        } else {
            isProcessing = true
            isResultFromGallery = fromGallery

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)

                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            isProcessing = false
                            // PART 1: 3-pass validation
                            val validation = OcrPriceExtractor.validateVisionText(visionText, bitmap.height)
                            if (validation is ValidationResult.NotAPriceTag) {
                                currentScannedItem = null
                                scanErrorMessage = validation.reason
                                showResultSheet = true
                                return@addOnSuccessListener
                            }

                            // PART 2: Extraction only after validation
                            val extractionResult = OcrPriceExtractor.extractBestPriceResult(
                                visionText = visionText,
                                imageWidth = bitmap.width,
                                imageHeight = bitmap.height
                            )

                            when (extractionResult) {
                                is ExtractionResult.Failure -> {
                                    currentScannedItem = null
                                    scanErrorMessage = extractionResult.reason
                                    showResultSheet = true
                                }
                                is ExtractionResult.Success -> {
                                    val extracted = extractionResult.extracted
                                    val currentLoc = activeTaxLocation
                                    if (currentLoc != null) {
                                        val insight = PriceInsightRepository.evaluate(extracted.itemName, extracted.price)
                                        val item = ScannedItem(
                                            name = extracted.itemName,
                                            tagPrice = extracted.price,
                                            taxRate = currentLoc.combinedRate,
                                            cityName = currentLoc.displayCityState,
                                            insight = insight
                                        )
                                        currentScannedItem = item
                                        scanErrorMessage = null
                                        showResultSheet = false
                                        HapticHelper.playScanSuccess(context)
                                        RevenueCatHelper.incrementDailyScanCount(context)
                                        // PART 3: On success, navigate to the Result screen
                                        onScanSuccess(item)
                                    } else {
                                        currentScannedItem = null
                                        scanErrorMessage = "Location unavailable. Enable GPS or set a Test Location in Settings."
                                        showResultSheet = true
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            isProcessing = false
                            currentScannedItem = null
                            scanErrorMessage = "No price detected. Try a clearer photo of the tag."
                            showResultSheet = true
                        }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        currentScannedItem = null
                        scanErrorMessage = "No price detected. Try a clearer photo of the tag."
                        showResultSheet = true
                    }
                }
            }
        }
    }

    // Photo picker launcher (PickVisualMedia)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            coroutineScope.launch(Dispatchers.IO) {
                val bitmap = loadGalleryBitmap(context, uri)
                if (bitmap != null) {
                    processBitmapForOcr(bitmap, 0, true)
                } else {
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        currentScannedItem = null
                        scanErrorMessage = "No price detected. Try a clearer photo of the tag."
                        showResultSheet = true
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // NON-US LOCATION GUARD SCREEN (Bypassed when Test Location Mode is active)
        if (gpsState is GpsLocationState.NonUsCountry && !LocationHelper.isTestLocationActive(context)) {
            val nonUs = gpsState as GpsLocationState.NonUsCountry
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TrueGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "United States Only",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "TrueTag currently supports US statutory sales tax schedules. Detected country: ${nonUs.countryName.uppercase()}.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = { showTestLocationDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TrueGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("enable_test_location_from_guard")
                    ) {
                        Text("Enable Test Location Mode", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showTestLocationDialog) {
                TestLocationPickerDialog(
                    currentTestLocation = LocationHelper.getTestLocation(context),
                    initialKeepForNextLaunch = LocationHelper.isKeepForNextLaunch(context),
                    onSelectLocation = { loc, keep ->
                        LocationHelper.setTestLocation(context, loc, keep)
                        permissionSignal = System.currentTimeMillis()
                        showTestLocationDialog = false
                    },
                    onDismiss = { showTestLocationDialog = false }
                )
            }
            return
        }

        // LOCATION PERMISSION RATIONALE SCREEN
        if (showPermissionRationale && !LocationHelper.hasLocationPermission(context) && LocationHelper.getManualOverride(context) == null && !LocationHelper.isTestLocationActive(context)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = TrueGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Real Location Access",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "In the United States, sales tax varies across 13,000+ local jurisdictions. TrueTag uses GPS or Test Location to look up the exact statutory rate.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            showPermissionRationale = false
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrueGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Enable Real GPS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            showPermissionRationale = false
                            showManualZipDialog = true
                        }
                    ) {
                        Text("Enter US ZIP Code Manually", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            return
        }

        // Camera Live Viewfinder (Viewfinder only, single frame captured via shutter)
        if (hasCameraPermission) {
            CameraPreviewView(
                controller = cameraController,
                isTorchOn = isTorchOn,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allow camera access to capture price tags with on-device OCR.",
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = TrueGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Camera Access", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Thin scan frame overlay in center (80% width with corner brackets, visual guide only)
        ScanOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // Top Translucent Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141416).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TrueTag",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    // Real Location Status Chip
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (activeTaxLocation != null) {
                                    showLocationDetailSheet = true
                                } else {
                                    showManualZipDialog = true
                                }
                            }
                            .testTag("location_chip"),
                        color = Color(0xFF1F1F24),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkOutline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeTaxLocation != null) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TrueGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${activeTaxLocation.city} · ${activeTaxLocation.ratePercentageFormatted}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrueGreen
                                )
                            } else {
                                Text(
                                    text = "Location unavailable. Enable GPS or set a Test Location in Settings.",
                                    fontSize = 11.sp,
                                    color = ErrorRed
                                )
                            }
                        }
                    }

                    // Profile icon
                    IconButton(
                        onClick = { navController.navigate("profile") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                    }
                }
            }
        }

        // Quick Access Cart Pill (Upper Right)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141416).copy(alpha = 0.90f),
            border = BorderStroke(1.dp, DarkOutline),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 66.dp, end = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { navController.navigate("cart") }
                .testTag("top_cart_shortcut")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgedBox(
                    badge = {
                        if (cartItems.isNotEmpty()) {
                            Badge(containerColor = TrueGreen) {
                                Text("${cartItems.size}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("$%.2f", totalRealCartPrice),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Floating AI Money Coach Bubble
        FloatingActionButton(
            onClick = { showMoneyCoachSheet = true },
            containerColor = Color(0xFF1A1A1D),
            contentColor = TrueGreen,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .testTag("floating_money_coach_fab")
        ) {
            Icon(imageVector = Icons.Default.SmartToy, contentDescription = "Money Coach AI", modifier = Modifier.size(24.dp))
        }

        // Camera Bottom Controls Row:
        // Left: 48dp Gallery button with "Upload" label in 12sp
        // Center: 72dp Shutter button (circular white ring + solid white inner circle)
        // Right: 48dp Flash toggle (bolt icon, cycles torch on/off)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 36.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("gallery_upload_button")
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E1E22).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Upload photo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Upload",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Large Shutter Button (72dp circular white ring with solid white inner circle)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isProcessing) {
                            isProcessing = true
                            cameraController.takePicture(
                                onSuccess = { bitmap, rotation ->
                                    processBitmapForOcr(bitmap, rotation, false)
                                },
                                onError = { e ->
                                    isProcessing = false
                                    currentScannedItem = null
                                    scanErrorMessage = "No price detected. Try a clearer photo of the tag."
                                    showResultSheet = true
                                }
                            )
                        }
                    }
                    .testTag("camera_shutter_button"),
                contentAlignment = Alignment.Center
            ) {
                // Circular white ring
                Canvas(modifier = Modifier.size(72.dp)) {
                    drawCircle(
                        color = Color.White,
                        radius = (size.minDimension - 4.dp.toPx()) / 2f,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
                // Solid white inner circle
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(56.dp)
                ) {}
            }

            // Flash Toggle Button (48dp bolt icon, cycles torch on/off)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        isTorchOn = !isTorchOn
                        cameraController.toggleTorch(isTorchOn)
                    }
                    .testTag("flash_toggle_button")
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isTorchOn) Color.White else Color(0xFF1E1E22).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, if (isTorchOn) Color.White else Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.Bolt else Icons.Default.FlashOff,
                            contentDescription = "Flash toggle",
                            tint = if (isTorchOn) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isTorchOn) "On" else "Flash",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // On-device Image Processing Loading Overlay
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E22),
                    border = BorderStroke(1.dp, DarkOutline)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = TrueGreen, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Analyzing tag on-device…",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Result Bottom Sheet (Shows actual extracted price, actual tax, or explicit failure state)
        ResultBottomSheet(
            visible = showResultSheet,
            item = currentScannedItem,
            taxLocation = activeTaxLocation,
            errorMessage = scanErrorMessage,
            isTestLocation = LocationHelper.isTestLocationActive(context),
            isFromGallery = isResultFromGallery,
            onAddToCart = { item ->
                HapticHelper.playAddToCart(context)
                onAddToCart(item)
                Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                showResultSheet = false
            },
            onScanNext = {
                showResultSheet = false
                currentScannedItem = null
                scanErrorMessage = null
            },
            onSaveTrip = {
                val locCity = activeTaxLocation?.city ?: "Local Store"
                onSaveTrip("Trip - $locCity", "Retail Store")
                Toast.makeText(context, "Trip saved to history", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showResultSheet = false
                currentScannedItem = null
                scanErrorMessage = null
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Location Detail Sheet
        if (showLocationDetailSheet && activeTaxLocation != null) {
            LocationDetailSheet(
                taxLocation = activeTaxLocation,
                isGpsEnabled = LocationHelper.isGpsProviderEnabled(context),
                isManualOverride = LocationHelper.getManualOverride(context) != null,
                onOpenCityPicker = {
                    showLocationDetailSheet = false
                    showCityPicker = true
                },
                onResetToGps = {
                    LocationHelper.setManualOverride(context, null)
                    locationPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
                onDismiss = { showLocationDetailSheet = false }
            )
        }

        // Manual US Zip Code Dialog
        if (showManualZipDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showManualZipDialog = false },
                title = { Text("US Location Lookup", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Enter a 5-digit US ZIP code to lookup municipal and state sales tax rates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = manualZipInput,
                            onValueChange = { if (it.length <= 5) manualZipInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("5-Digit US ZIP") },
                            placeholder = { Text("e.g. 90210, 10001, 98101") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val matched = TaxRepository.findByZip(manualZipInput)
                            if (matched != null) {
                                LocationHelper.setManualOverride(context, matched)
                                showManualZipDialog = false
                                Toast.makeText(context, "Set to ${matched.city}, ${matched.state}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "ZIP code not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrueGreen)
                    ) {
                        Text("Set Location", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualZipDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Money Coach Sheet
        if (showMoneyCoachSheet && activeTaxLocation != null) {
            MoneyCoachSheet(
                chatMessages = chatMessages,
                cartItems = cartItems,
                taxLocation = activeTaxLocation,
                budget = budget,
                recentTrips = savedTrips,
                onSendMessage = onSendChatMessage,
                onClearChat = onClearChat,
                onDismiss = { showMoneyCoachSheet = false }
            )
        }

        // Free scan limit authentication intercept bottom sheet
        if (showScanLimitAuthIntercept) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showScanLimitAuthIntercept = false },
                sheetState = sheetState,
                containerColor = PaperSurface,
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    TornPaperTopEdge(paperColor = PaperSurface, dividerColor = ReceiptDivider)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "sign in to get TrueTag Pro",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You've reached your free daily scans. Sign in to your account to upgrade to TrueTag Pro with unlimited scans.",
                            fontFamily = SpaceGrotesk,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            color = WarmGray
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = AuthHelper.continueWithGoogle()
                                    showScanLimitAuthIntercept = false
                                    if (res.isSuccess) {
                                        navController.navigate("paywall")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                                .testTag("scan_limit_google_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TagRed, contentColor = PaperSurface)
                        ) {
                            Text("continue with Google", fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                showScanLimitAuthIntercept = false
                                navController.navigate("sign_in")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .border(1.dp, InkNavy, RoundedCornerShape(14.dp))
                                .testTag("scan_limit_email_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = InkNavy)
                        ) {
                            Text("continue with email", fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InkNavy)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showScanLimitAuthIntercept = false }) {
                            Text("cancel", fontFamily = SpaceGrotesk, color = WarmGray)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Loads image from Uri into Bitmap without resizing below 1024px on long edge,
 * adjusting for EXIF rotation.
 */
private fun loadGalleryBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val resolver = context.contentResolver
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        val w = options.outWidth
        val h = options.outHeight
        if (w <= 0 || h <= 0) return null

        val maxEdge = maxOf(w, h)
        var sampleSize = 1
        // Requirement 3: Do not resize below 1024 pixels on the long edge
        while (maxEdge / (sampleSize * 2) >= 1024) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val originalBitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val orientation = try {
            resolver.openInputStream(uri)?.use { stream ->
                val exif = android.media.ExifInterface(stream)
                exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
            } ?: android.media.ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            android.media.ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return originalBitmap
        }
        Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
    } catch (e: Exception) {
        null
    }
}
