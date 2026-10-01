package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.AuthHelper
import com.example.ui.components.StampBadge
import com.example.ui.components.TrueTagLogoLockup
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
import kotlinx.coroutines.launch

// ============================================================================
// WELCOME SCREEN
// Duotone vector illustration (phone scanning tag), Space Grotesk bold 28sp headline,
// Tag Red primary Google button, Ink Navy outlined Email button, Guest link.
// ============================================================================

@Composable
fun WelcomeScreen(
    navController: NavController,
    onContinueAsGuest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSigningInGuest by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            TrueTagLogoLockup(markSize = 36.dp, textSize = 24)

            Spacer(modifier = Modifier.height(36.dp))

            // Duotone Vector Illustration: Hand holding phone over retail swing tag
            WelcomeDuotoneIllustration(
                modifier = Modifier
                    .size(240.dp, 200.dp)
                    .testTag("welcome_illustration")
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "know the real price before checkout",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "point your camera at any tag, see the true price with tax, instantly",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = WarmGray
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp, top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Primary Filled Button (Tag Red, 14dp radius, 1dp darker red border)
            Button(
                onClick = {
                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        isSigningInGuest = true
                        coroutineScope.launch {
                            val res = AuthHelper.signInWithGoogle(activity)
                            isSigningInGuest = false
                            if (res.isSuccess) {
                                com.example.data.RevenueCatHelper.setOnboardingCompleted(context, true)
                                onContinueAsGuest()
                            } else {
                                val ex = res.exceptionOrNull()
                                if (ex !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                                    Toast.makeText(context, ex?.localizedMessage ?: "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                    .testTag("welcome_google_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TagRed,
                    contentColor = PaperSurface
                )
            ) {
                if (isSigningInGuest) {
                    CircularProgressIndicator(color = PaperSurface, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "continue with Google",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            // Secondary Outlined Button (Ink Navy with transparent fill)
            OutlinedButton(
                onClick = { navController.navigate("sign_in") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, InkNavy, RoundedCornerShape(14.dp))
                    .testTag("welcome_email_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = InkNavy
                )
            ) {
                Text(
                    text = "continue with email",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = InkNavy
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Guest Mode
            Text(
                text = "continue as guest",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                textDecoration = TextDecoration.Underline,
                color = InkNavy,
                modifier = Modifier
                    .clickable {
                        coroutineScope.launch {
                            AuthHelper.signInAnonymously()
                            onContinueAsGuest()
                        }
                    }
                    .padding(8.dp)
                    .testTag("continue_as_guest_button")
            )
        }
    }
}

/**
 * Bespoke Duotone Flat Illustration: Hand holding phone over a retail swing tag.
 * Pure vector canvas in Tag Red & Ink Navy. Zero photo imagery.
 */
@Composable
private fun WelcomeDuotoneIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ground shadow/surface
        drawRoundRect(
            color = ReceiptDivider,
            topLeft = Offset(w * 0.15f, h * 0.88f),
            size = Size(w * 0.70f, h * 0.04f),
            cornerRadius = CornerRadius(h * 0.02f, h * 0.02f)
        )

        // Swing tag on shelf (Tag Red #C1443C)
        val tagLeft = w * 0.30f
        val tagTop = h * 0.44f
        val tagW = w * 0.40f
        val tagH = h * 0.46f

        val tagPath = Path().apply {
            moveTo(tagLeft + tagW * 0.35f, tagTop)
            lineTo(tagLeft + tagW * 0.65f, tagTop)
            lineTo(tagLeft + tagW, tagTop + tagH * 0.30f)
            lineTo(tagLeft + tagW, tagTop + tagH)
            lineTo(tagLeft, tagTop + tagH)
            lineTo(tagLeft, tagTop + tagH * 0.30f)
            close()
        }
        drawPath(tagPath, TagRed)

        // Tag String Hole
        drawCircle(
            color = PaperCream,
            radius = tagW * 0.08f,
            center = Offset(tagLeft + tagW * 0.50f, tagTop + tagH * 0.18f)
        )

        // Tag Price text placeholder lines
        drawLine(
            color = PaperSurface,
            start = Offset(tagLeft + tagW * 0.25f, tagTop + tagH * 0.48f),
            end = Offset(tagLeft + tagW * 0.75f, tagTop + tagH * 0.48f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = PaperSurface.copy(alpha = 0.8f),
            start = Offset(tagLeft + tagW * 0.30f, tagTop + tagH * 0.64f),
            end = Offset(tagLeft + tagW * 0.70f, tagTop + tagH * 0.64f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Hand holding phone (Ink Navy #1F2A44)
        val phoneLeft = w * 0.18f
        val phoneTop = h * 0.10f
        val phoneW = w * 0.36f
        val phoneH = h * 0.62f

        // Phone body
        drawRoundRect(
            color = InkNavy,
            topLeft = Offset(phoneLeft, phoneTop),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )

        // Phone screen viewfinder
        drawRoundRect(
            color = PaperCream,
            topLeft = Offset(phoneLeft + 4.dp.toPx(), phoneTop + 6.dp.toPx()),
            size = Size(phoneW - 8.dp.toPx(), phoneH - 12.dp.toPx()),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )

        // Scan brackets on phone screen (Tag Red)
        val bW = phoneW * 0.60f
        val bH = phoneH * 0.42f
        val bLeft = phoneLeft + (phoneW - bW) / 2
        val bTop = phoneTop + (phoneH - bH) / 2

        drawRoundRect(
            color = TagRed,
            topLeft = Offset(bLeft, bTop),
            size = Size(bW, bH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

// ============================================================================
// SIGN IN SCREEN
// Swing tag styled label chip, Monospace password, Sign In button.
// ============================================================================

@Composable
fun SignInScreen(
    navController: NavController,
    onSignInSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Back arrow in circular 36dp tap target
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .size(36.dp)
                .clickable { navController.popBackStack() }
                .testTag("signin_back_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "welcome back",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email field with Swing Tag style label chip
        SwingTagInputLabel(label = "EMAIL ADDRESS")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            singleLine = true,
            placeholder = { Text("you@example.com", fontFamily = SpaceGrotesk, color = WarmGray) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = TagRed,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signin_email_input")
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Password field with Swing Tag style label chip
        SwingTagInputLabel(label = "PASSWORD")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = IbmpPlexMono),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = WarmGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = TagRed,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signin_password_input")
        )

        // Forgot password link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "forgot password?",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = InkNavy,
                modifier = Modifier
                    .clickable { navController.navigate("forgot_password") }
                    .testTag("forgot_password_link")
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = errorMessage!!,
                fontFamily = SpaceGrotesk,
                fontSize = 13.sp,
                color = ActionRed
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Primary Button: Sign In
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter your email and password"
                    return@Button
                }
                isLoading = true
                coroutineScope.launch {
                    val result = AuthHelper.signInWithEmail(email.trim(), password)
                    isLoading = false
                    if (result.isSuccess) {
                        onSignInSuccess()
                    } else {
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Invalid credentials. Try again."
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TagRed, contentColor = PaperSurface),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                .testTag("signin_submit_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = PaperSurface, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = "sign in",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Secondary Action: Continue with Google
        OutlinedButton(
            onClick = {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    isLoading = true
                    coroutineScope.launch {
                        val result = AuthHelper.signInWithGoogle(activity)
                        isLoading = false
                        if (result.isSuccess) {
                            onSignInSuccess()
                        } else {
                            val ex = result.exceptionOrNull()
                            if (ex !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                                errorMessage = ex?.localizedMessage ?: "Google Sign-In failed"
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = InkNavy),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, InkNavy, RoundedCornerShape(14.dp))
                .testTag("signin_google_button")
        ) {
            Text(
                text = "continue with Google",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = InkNavy
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom link: do not have an account, sign up
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "do not have an account? ",
                fontFamily = SpaceGrotesk,
                fontSize = 14.sp,
                color = WarmGray
            )
            Text(
                text = "sign up",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = InkNavy,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { navController.navigate("sign_up") }
                    .testTag("goto_signup_link")
            )
        }
    }
}

// ============================================================================
// SIGN UP SCREEN
// Name, Email, Password with 3-segment tag-shaped strength meter, Custom Checkbox.
// ============================================================================

@Composable
fun SignUpScreen(
    navController: NavController,
    onSignUpSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val passwordStrength = remember(password) {
        when {
            password.length >= 8 && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> 3
            password.length >= 6 -> 2
            password.isNotBlank() -> 1
            else -> 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .size(36.dp)
                .clickable { navController.popBackStack() }
                .testTag("signup_back_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "create your account",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Name
        SwingTagInputLabel(label = "FULL NAME")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorMessage = null },
            singleLine = true,
            placeholder = { Text("e.g. Jane Doe", fontFamily = SpaceGrotesk, color = WarmGray) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = TagRed,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_name_input")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Email
        SwingTagInputLabel(label = "EMAIL ADDRESS")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            singleLine = true,
            placeholder = { Text("you@example.com", fontFamily = SpaceGrotesk, color = WarmGray) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = TagRed,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_email_input")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Password
        SwingTagInputLabel(label = "PASSWORD")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = IbmpPlexMono),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = WarmGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = TagRed,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_password_input")
        )

        // Password Strength Meter: Three small tag-shaped segments filling Red -> Yellow -> Green
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..3).forEach { index ->
                val active = passwordStrength >= index
                val segColor = when {
                    !active -> MaterialTheme.colorScheme.outline
                    passwordStrength == 1 -> TagRed
                    passwordStrength == 2 -> MarkerYellow
                    else -> ForestGreen
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(segColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Confirm Password
        SwingTagInputLabel(label = "CONFIRM PASSWORD")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorMessage = null },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = IbmpPlexMono),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = TagRed,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_confirm_password_input")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Hand-drawn style terms checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { agreedToTerms = !agreedToTerms },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hand-drawn square box with check
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (agreedToTerms) TagRed else MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, if (agreedToTerms) TagRed else MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                    .testTag("terms_checkbox"),
                contentAlignment = Alignment.Center
            ) {
                if (agreedToTerms) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        val stroke = 2.dp.toPx()
                        val p = Path().apply {
                            moveTo(size.width * 0.15f, size.height * 0.50f)
                            lineTo(size.width * 0.42f, size.height * 0.78f)
                            lineTo(size.width * 0.85f, size.height * 0.22f)
                        }
                        drawPath(p, PaperSurface, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "I agree to the Terms of Service and Privacy Policy",
                fontFamily = SpaceGrotesk,
                fontSize = 13.sp,
                color = WarmGray
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = errorMessage!!,
                fontFamily = SpaceGrotesk,
                fontSize = 13.sp,
                color = ActionRed
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Create Account Button
        Button(
            onClick = {
                if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    errorMessage = "Please fill in all fields"
                    return@Button
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    errorMessage = "Please enter a valid email address"
                    return@Button
                }
                if (password.length < 8) {
                    errorMessage = "Password must be at least 8 characters long"
                    return@Button
                }
                if (password != confirmPassword) {
                    errorMessage = "Passwords do not match"
                    return@Button
                }
                if (!agreedToTerms) {
                    errorMessage = "Please accept the terms and privacy policy"
                    return@Button
                }
                isLoading = true
                coroutineScope.launch {
                    val result = AuthHelper.signUpWithEmail(email.trim(), password, name.trim(), confirmPassword)
                    isLoading = false
                    if (result.isSuccess) {
                        onSignUpSuccess()
                    } else {
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Could not create account"
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TagRed, contentColor = PaperSurface),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                .testTag("signup_submit_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = PaperSurface, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = "create account",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "already have an account? ",
                fontFamily = SpaceGrotesk,
                fontSize = 14.sp,
                color = WarmGray
            )
            Text(
                text = "sign in",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = InkNavy,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { navController.popBackStack() }
                    .testTag("goto_signin_link")
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ============================================================================
// FORGOT PASSWORD SCREEN
// Email field, send reset link, stamp animation confirmation state.
// ============================================================================

@Composable
fun ForgotPasswordScreen(
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .size(36.dp)
                .clickable { navController.popBackStack() }
                .testTag("forgot_back_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "reset your password",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "enter your email address and we will send you a link to reset your password",
            fontFamily = SpaceGrotesk,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = WarmGray
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isSubmitted) {
            SwingTagInputLabel(label = "EMAIL ADDRESS")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                singleLine = true,
                placeholder = { Text("you@example.com", fontFamily = SpaceGrotesk, color = WarmGray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = TagRed,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("forgot_email_input")
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = errorMessage!!, color = ActionRed, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (email.isBlank()) {
                        errorMessage = "Please enter your email"
                        return@Button
                    }
                    isLoading = true
                    coroutineScope.launch {
                        val res = AuthHelper.sendPasswordReset(email.trim())
                        isLoading = false
                        if (res.isSuccess) {
                            isSubmitted = true
                        } else {
                            errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Could not send reset email"
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TagRed, contentColor = PaperSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                    .testTag("forgot_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = PaperSurface, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("send reset link", fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        } else {
            // Confirmation state with stamp animation!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StampBadge(
                    text = "EMAIL SENT",
                    containerColor = ForestGreen.copy(alpha = 0.15f),
                    contentColor = ForestGreen
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "check your email for a reset link",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "We have sent a link to $email. Tap the link in your inbox to reset your password.",
                    fontFamily = SpaceGrotesk,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = WarmGray
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("back to sign in", fontFamily = SpaceGrotesk, color = InkNavy)
                }
            }
        }
    }
}

/**
 * Small Swing Tag styled chip for field labels
 */
@Composable
private fun SwingTagInputLabel(label: String) {
    Surface(
        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 4.dp, bottomEnd = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.padding(bottom = 2.dp)
    ) {
        val outlineColor = MaterialTheme.colorScheme.outline
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(color = outlineColor, radius = 2.5.dp.toPx())
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
