package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthHelper
import com.example.model.ScannedItem
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
import kotlinx.coroutines.launch

/**
 * Bespoke Torn Paper Top Edge.
 * Emulates the perforated, ragged receipt/swing tag tear along the top boundary.
 */
@Composable
fun TornPaperTopEdge(
    modifier: Modifier = Modifier,
    paperColor: Color = PaperSurface,
    dividerColor: Color = ReceiptDivider
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val w = size.width
        val h = size.height
        val teethCount = 32
        val toothWidth = w / teethCount

        val path = Path().apply {
            moveTo(0f, h)
            for (i in 0..teethCount) {
                val x = i * toothWidth
                val y = if (i % 2 == 0) h * 0.15f else h * 0.85f
                lineTo(x, y)
            }
            lineTo(w, h)
            close()
        }

        drawPath(path = path, color = paperColor)

        // Subtle torn edge stroke
        val strokePath = Path().apply {
            moveTo(0f, h * 0.15f)
            for (i in 0..teethCount) {
                val x = i * toothWidth
                val y = if (i % 2 == 0) h * 0.15f else h * 0.85f
                lineTo(x, y)
            }
        }
        drawPath(
            path = strokePath,
            color = dividerColor,
            style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * ACCOUNT GATING INTERCEPT BOTTOM SHEET
 * Intercepts Add to Cart for guest or signed-out users (Amazon / Flipkart parity).
 * Features torn paper top edge, Google & Email actions, optional inline form, and
 * resumes the interrupted Add to Cart action automatically upon successful auth.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInInterceptBottomSheet(
    item: ScannedItem,
    onDismiss: () -> Unit,
    onAuthSuccessAndAddToCart: (ScannedItem) -> Unit,
    onNavigateToSignInFull: (ScannedItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showEmailForm by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PaperSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Torn paper top edge
            TornPaperTopEdge(
                modifier = Modifier.fillMaxWidth(),
                paperColor = PaperSurface,
                dividerColor = ReceiptDivider
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header row with title & close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "sign in to add this to your cart",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("dismiss_intercept_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = WarmGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "an account is needed to save items and trips",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = WarmGray,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Item preview summary chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PaperCream,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ReceiptDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Tag: $${String.format("%.2f", item.tagPrice)}",
                                fontFamily = IbmpPlexMono,
                                fontSize = 12.sp,
                                color = WarmGray
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL WITH TAX",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp,
                                color = WarmGray
                            )
                            Text(
                                text = item.formattedTruePrice,
                                fontFamily = IbmpPlexMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ForestGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error Banner
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.sp,
                        color = ActionRed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                // Inline Email / Password Form (when toggled)
                AnimatedVisibility(
                    visible = showEmailForm,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it; errorMessage = null },
                                singleLine = true,
                                label = { Text("Your Name", fontFamily = SpaceGrotesk) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TagRed,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("intercept_name_input")
                            )
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it; errorMessage = null },
                            singleLine = true,
                            label = { Text("Email Address", fontFamily = SpaceGrotesk) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TagRed,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("intercept_email_input")
                        )

                        OutlinedTextField(
                            value = passInput,
                            onValueChange = { passInput = it; errorMessage = null },
                            singleLine = true,
                            label = { Text("Password", fontFamily = SpaceGrotesk) },
                            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passVisible = !passVisible }) {
                                    Icon(
                                        imageVector = if (passVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = WarmGray
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TagRed,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("intercept_pass_input")
                        )

                        Button(
                            onClick = {
                                if (emailInput.isBlank() || passInput.isBlank()) {
                                    errorMessage = "Please enter email and password"
                                    return@Button
                                }
                                isLoading = true
                                coroutineScope.launch {
                                    val res = AuthHelper.upgradeAnonymousOrSignIn(
                                        email = emailInput.trim(),
                                        pass = passInput,
                                        isSignUp = isSignUpMode,
                                        name = nameInput.trim()
                                    )
                                    isLoading = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Account linked! Item added to Cart", Toast.LENGTH_SHORT).show()
                                        onAuthSuccessAndAddToCart(item)
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Sign in failed"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("intercept_submit_auth_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TagRed, contentColor = PaperSurface)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = PaperSurface, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (isSignUpMode) "create account & add to cart" else "sign in & add to cart",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // Toggle Sign In vs Register
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                                fontFamily = SpaceGrotesk,
                                fontSize = 13.sp,
                                color = WarmGray
                            )
                            Text(
                                text = if (isSignUpMode) "Sign In" else "Create Account",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = InkNavy,
                                modifier = Modifier.clickable { isSignUpMode = !isSignUpMode }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // If inline form is not showing, show standard Continue with Google & Continue with Email
                if (!showEmailForm) {
                    // Continue with Google (Primary Filled Button in Tag Red)
                    Button(
                        onClick = {
                            isLoading = true
                            coroutineScope.launch {
                                val res = AuthHelper.continueWithGoogle()
                                isLoading = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Signed in! Item added to Cart", Toast.LENGTH_SHORT).show()
                                    onAuthSuccessAndAddToCart(item)
                                } else {
                                    errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Sign in failed"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                            .testTag("intercept_google_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TagRed,
                            contentColor = PaperSurface
                        )
                    ) {
                        if (isLoading) {
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Continue with Email (Secondary Outlined Button in Ink Navy)
                    OutlinedButton(
                        onClick = {
                            showEmailForm = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, InkNavy, RoundedCornerShape(14.dp))
                            .testTag("intercept_email_button"),
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cancel button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("intercept_cancel_button")
                    ) {
                        Text(
                            text = "cancel",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = WarmGray
                        )
                    }
                }
            }
        }
    }
}

/**
 * SIGN IN REQUIRED EMPTY STATE VIEW
 * Displayed inside Cart tab, Trips tab, and Profile tab when no fully authenticated
 * user is signed in (Amazon / Flipkart pattern).
 */
@Composable
fun SignInRequiredView(
    headline: String,
    explanation: String,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Swing tag silhouette with string hole and keyhole icon
            Canvas(modifier = Modifier.size(96.dp)) {
                val w = size.width
                val h = size.height

                // Tag outline
                val tagPath = Path().apply {
                    moveTo(w * 0.38f, h * 0.16f)
                    lineTo(w * 0.62f, h * 0.16f)
                    lineTo(w * 0.84f, h * 0.36f)
                    lineTo(w * 0.84f, h * 0.86f)
                    lineTo(w * 0.16f, h * 0.86f)
                    lineTo(w * 0.16f, h * 0.36f)
                    close()
                }
                drawPath(path = tagPath, color = TagRed.copy(alpha = 0.12f))
                drawPath(
                    path = tagPath,
                    color = TagRed,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Hole cutout
                drawCircle(
                    color = TagRed,
                    radius = w * 0.055f,
                    center = Offset(w * 0.50f, h * 0.28f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Lock / Keyhole body inside tag
                val lockBody = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = w * 0.38f,
                            top = h * 0.52f,
                            right = w * 0.62f,
                            bottom = h * 0.74f,
                            radiusX = 4.dp.toPx(),
                            radiusY = 4.dp.toPx()
                        )
                    )
                }
                drawPath(path = lockBody, color = InkNavy)

                // Lock Shackle
                val shackle = Path().apply {
                    moveTo(w * 0.42f, h * 0.52f)
                    lineTo(w * 0.42f, h * 0.44f)
                    cubicTo(w * 0.42f, h * 0.36f, w * 0.58f, h * 0.36f, w * 0.58f, h * 0.44f)
                    lineTo(w * 0.58f, h * 0.52f)
                }
                drawPath(
                    path = shackle,
                    color = InkNavy,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = headline,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = explanation,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = WarmGray,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                    .testTag("signin_required_primary_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TagRed,
                    contentColor = PaperSurface
                )
            ) {
                Text(
                    text = "sign in",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
