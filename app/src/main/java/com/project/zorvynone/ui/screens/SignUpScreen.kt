package com.project.zorvynone.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.AuthViewModel
import com.project.zorvynone.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Premium Palette
private val PremiumGold = Color(0xFFE5C158)
private val GlassSurface = Color(0xFF1C2238).copy(alpha = 0.5f)

// Google brand colors for the logo
private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: (username: String, email: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsState()
    val isLoading = authState is AuthViewModel.AuthState.Loading
    val context = LocalContext.current

    // --- ROCKET LAUNCH STATE ---
    var isLaunching by remember { mutableStateOf(false) }

    // Staggered entrance starts immediately (no typing delay)
    var startStagger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(300); startStagger = true }

    // Pulse Animation for Tagline
    val infiniteTransition = rememberInfiniteTransition(label = "tagline_pulse")
    val taglineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Success) {
            // Trigger rocket before finishing
            isLaunching = true
            delay(2500) // Let the rocket fly for a moment
            val user = (authState as AuthViewModel.AuthState.Success).user
            onRegisterSuccess(user.displayName ?: username, user.email ?: email)
            authViewModel.resetState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = ZorvynBackground) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- TOP NAVIGATION ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(GlassSurface, RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                            .clickable { onNavigateToLogin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // --- BRAND HEADER ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SignUpGeometricArt()

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "expectr",
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.5).sp
                        )
                        Text(
                            text = "CREATE ACCOUNT",
                            color = PremiumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            modifier = Modifier.alpha(taglineAlpha)
                        )
                    }
                }

                // --- FORM SECTIONS ---
                StaggeredEntrance(visible = startStagger, delayMillis = 0) {
                    Column(modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()) {
                        Text(
                            text = "Create Account",
                            color = TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Sign up to get started.",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                StaggeredEntrance(visible = startStagger, delayMillis = 200) {
                    Column(modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()) {
                        val errorMessage = (authState as? AuthViewModel.AuthState.Error)?.message
                        if (errorMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ZorvynRed.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .border(1.dp, ZorvynRed.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Text(text = errorMessage, color = ZorvynRed, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; usernameError = null },
                            placeholder = { Text("Full Name", color = TextSecondary.copy(alpha = 0.4f)) },
                            isError = usernameError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = GlassSurface,
                                focusedContainerColor = GlassSurface,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                                focusedBorderColor = PremiumGold,
                                errorBorderColor = ZorvynRed,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = PremiumGold
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; emailError = null },
                            placeholder = { Text("Email Address", color = TextSecondary.copy(alpha = 0.4f)) },
                            isError = emailError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = GlassSurface,
                                focusedContainerColor = GlassSurface,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                                focusedBorderColor = PremiumGold,
                                errorBorderColor = ZorvynRed,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = PremiumGold
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; passwordError = null },
                            placeholder = { Text("Password (6+ characters)", color = TextSecondary.copy(alpha = 0.4f)) },
                            isError = passwordError != null,
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = TextSecondary.copy(alpha = 0.4f)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = GlassSurface,
                                focusedContainerColor = GlassSurface,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                                focusedBorderColor = PremiumGold,
                                errorBorderColor = ZorvynRed,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = PremiumGold
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }
                }

                StaggeredEntrance(visible = startStagger, delayMillis = 400) {
                    Column(modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(32.dp))

                        // --- SWIPE TO SIGN UP SLIDER ---
                        SwipeToSignUpButton(
                            isLoading = isLoading,
                            onSwipeComplete = {
                                var valid = true
                                if (username.isBlank()) { usernameError = "Name required"; valid = false }
                                if (email.isBlank() || !email.contains("@")) { emailError = "Invalid email"; valid = false }
                                if (password.length < 6) { passwordError = "Too short"; valid = false }
                                if (valid) {
                                    authViewModel.signUpWithEmail(username, email, password)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.05f))
                            Text("  OR  ", color = TextSecondary.copy(0.3f), fontSize = 10.sp, letterSpacing = 2.sp)
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.05f))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedButton(
                            onClick = { authViewModel.signInWithGoogle(context) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                                SignUpGoogleLogo()
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Continue with Google", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("Already have an account? ", color = TextSecondary, fontSize = 14.sp)
                            Text(
                                text = "Sign In",
                                color = PremiumGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.clickable { onNavigateToLogin() }
                            )
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }

        // --- THE ROCKET OVERLAY (LOTTIE) ---
        if (isLaunching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable(enabled = false) {}, // Intercept clicks
                contentAlignment = Alignment.Center
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.rocket_launch))
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = 1,
                    restartOnPlay = false
                )

                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(300.dp)
                )

                if (progress >= 0.8f) {
                    Text(
                        text = "Account Created!",
                        color = PremiumGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 280.dp)
                    )
                }
            }
        }
    }
}

// --- HELPER COMPONENTS ---

@Composable
private fun StaggeredEntrance(
    visible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .alpha(alpha)
            .offset { IntOffset(0, offsetY.dp.toPx().roundToInt()) }
    ) {
        content()
    }
}

@Composable
private fun SignUpGeometricArt() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        rotate(degrees = -15f, pivot = Offset(w * 0.5f, h * 0.5f)) {
            drawRoundRect(color = Color(0xFF1C2238).copy(alpha = 0.3f), topLeft = Offset(w * 0.2f, h * 0.1f), size = Size(w * 0.6f, h * 0.8f), cornerRadius = CornerRadius(40f, 40f), style = Stroke(width = 1f))
        }
        drawCircle(brush = Brush.radialGradient(colors = listOf(PremiumGold.copy(0.15f), Color.Transparent), center = Offset(w * 0.8f, h * 0.2f), radius = w * 0.4f), radius = w * 0.4f, center = Offset(w * 0.8f, h * 0.2f))
        rotate(degrees = 10f, pivot = Offset(w * 0.3f, h * 0.7f)) {
            drawRoundRect(color = Color(0xFF252D42).copy(alpha = 0.2f), topLeft = Offset(w * 0.1f, h * 0.4f), size = Size(w * 0.4f, h * 0.4f), cornerRadius = CornerRadius(30f, 30f))
        }
    }
}

// --- SWIPE TO SIGN UP SLIDER ---

@Composable
private fun SwipeToSignUpButton(
    isLoading: Boolean,
    onSwipeComplete: () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val handleSizeDp = 52.dp
    val trackHeight = 62.dp
    val handleSizePx = with(density) { handleSizeDp.toPx() }

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val maxDragPx = (trackWidthPx - handleSizePx).coerceAtLeast(0f)

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var hasTriggered by remember { mutableStateOf(false) }
    val swipeProgress = if (maxDragPx > 0f) (dragOffset / maxDragPx).coerceIn(0f, 1f) else 0f

    // Animate snap-back
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "drag_snap"
    )

    // Shimmer for hint text
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by shimmerTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    if (isLoading) {
        // Loading state: show a centered spinner in the track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .background(
                    PremiumGold.copy(alpha = 0.15f),
                    RoundedCornerShape(31.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = PremiumGold,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "CREATING ACCOUNT...",
                    color = PremiumGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    } else {
        // Swipe track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PremiumGold.copy(alpha = 0.08f),
                            PremiumGold.copy(alpha = 0.15f)
                        )
                    ),
                    RoundedCornerShape(31.dp)
                )
                .border(1.dp, PremiumGold.copy(alpha = 0.15f), RoundedCornerShape(31.dp))
                .onSizeChanged { trackWidthPx = it.width.toFloat() },
            contentAlignment = Alignment.CenterStart
        ) {
            // Gold fill behind the drag
            Box(
                modifier = Modifier
                    .width(with(density) { (animatedOffset + handleSizePx).toDp() })
                    .fillMaxHeight()
                    .background(
                        PremiumGold.copy(alpha = 0.12f * swipeProgress),
                        RoundedCornerShape(31.dp)
                    )
            )

            // Hint text (fades as you drag)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "SWIPE TO SIGN UP  →",
                    color = PremiumGold.copy(alpha = shimmerAlpha * (1f - swipeProgress)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            // Draggable handle
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                    .padding(4.dp)
                    .size(handleSizeDp)
                    .clip(CircleShape)
                    .background(PremiumGold)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            if (!hasTriggered) {
                                dragOffset = (dragOffset + delta).coerceIn(0f, maxDragPx)
                                // Haptic when crossing threshold
                                if (swipeProgress >= 0.85f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        },
                        onDragStopped = {
                            if (swipeProgress >= 0.85f && !hasTriggered) {
                                hasTriggered = true
                                dragOffset = maxDragPx
                                onSwipeComplete()
                                delay(500)
                                hasTriggered = false
                                dragOffset = 0f
                            } else {
                                dragOffset = 0f
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Swipe to sign up",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SignUpGoogleLogo() {
    Canvas(modifier = Modifier.size(48.dp)) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val strokeW = r * 0.38f
        drawArc(color = GoogleBlue, startAngle = -45f, sweepAngle = 90f, useCenter = false, topLeft = Offset(cx - r + strokeW / 2, cy - r + strokeW / 2), size = Size((r - strokeW / 2) * 2, (r - strokeW / 2) * 2), style = Stroke(width = strokeW))
        drawArc(color = GoogleGreen, startAngle = 45f, sweepAngle = 90f, useCenter = false, topLeft = Offset(cx - r + strokeW / 2, cy - r + strokeW / 2), size = Size((r - strokeW / 2) * 2, (r - strokeW / 2) * 2), style = Stroke(width = strokeW))
        drawArc(color = GoogleYellow, startAngle = 135f, sweepAngle = 70f, useCenter = false, topLeft = Offset(cx - r + strokeW / 2, cy - r + strokeW / 2), size = Size((r - strokeW / 2) * 2, (r - strokeW / 2) * 2), style = Stroke(width = strokeW))
        drawArc(color = GoogleRed, startAngle = 205f, sweepAngle = 110f, useCenter = false, topLeft = Offset(cx - r + strokeW / 2, cy - r + strokeW / 2), size = Size((r - strokeW / 2) * 2, (r - strokeW / 2) * 2), style = Stroke(width = strokeW))
        drawRect(color = GoogleBlue, topLeft = Offset(cx, cy - strokeW / 2), size = Size(r * 0.55f, strokeW))
    }
}