package com.project.zorvynone.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.AuthViewModel

// Premium Palette
private val PremiumGold = Color(0xFFE5C158)
private val GlassSurface = Color(0xFF1C2238).copy(alpha = 0.5f)

// Google brand colors
private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (email: String) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsState()
    val resetEmailSent by authViewModel.resetEmailSent.collectAsState()
    val isLoading = authState is AuthViewModel.AuthState.Loading
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // --- ANIMATION: Tagline Glow ---
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
            val user = (authState as AuthViewModel.AuthState.Success).user
            onLoginSuccess(user.email ?: "")
            authViewModel.resetState()
        }
    }

    LaunchedEffect(resetEmailSent) {
        if (resetEmailSent) {
            snackbarHostState.showSnackbar("Recovery blueprint sent to your inbox.")
            authViewModel.clearResetFlag()
        }
    }

    Scaffold(
        containerColor = ZorvynBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High-End Geometric Header
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                LoginGeometricArt()

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "expectr",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = "WEALTH INTELLIGENCE",
                        color = PremiumGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        modifier = Modifier.alpha(taglineAlpha) // Breathing Glow applied here
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()) {
                // FIXED: Bold greeting for premium appearance
                Text(
                    text = "Welcome back.",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black, // Maximum boldness
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Access your private vault below.",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Error Banner
                val errorMessage = (authState as? AuthViewModel.AuthState.Error)?.message
                if (errorMessage != null) {
                    Box(modifier = Modifier.fillMaxWidth().background(ZorvynRed.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).border(1.dp, ZorvynRed.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Text(text = errorMessage, color = ZorvynRed, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    placeholder = { Text("Email Address", color = TextSecondary.copy(0.4f)) },
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

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    placeholder = { Text("Password", color = TextSecondary.copy(0.4f)) },
                    isError = passwordError != null,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = TextSecondary.copy(0.4f))
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
                    singleLine = true
                )

                Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "Forgot Password?",
                        color = PremiumGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (email.contains("@")) authViewModel.sendPasswordReset(email)
                            else emailError = "Enter email for recovery"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Primary Button
                Button(
                    onClick = {
                        if (email.contains("@") && password.length >= 6) authViewModel.signInWithEmail(email, password)
                        else {
                            if (!email.contains("@")) emailError = "Invalid email"
                            if (password.length < 6) passwordError = "Required"
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumGold),
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Access Vault", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.05f))
                    Text("  SECURE HANDSHAKE  ", color = TextSecondary.copy(0.3f), fontSize = 10.sp, letterSpacing = 2.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.05f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Google SignIn
                OutlinedButton(
                    onClick = { authViewModel.signInWithGoogle(context) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) { LoginGoogleLogo() }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Continue with Google", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Footer
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("New to the ecosystem? ", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        text = "Initialize Architect",
                        color = PremiumGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.clickable { onNavigateToSignUp() }
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun LoginGeometricArt() {
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

@Composable
private fun LoginGoogleLogo() {
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