package com.project.zorvynone.ui.screens

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

// Google brand colors
private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)
private val AccentBlue = Color(0xFF2563EB)

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

    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.Success -> {
                val user = (authState as AuthViewModel.AuthState.Success).user
                onLoginSuccess(user.email ?: "")
                authViewModel.resetState()
            }
            is AuthViewModel.AuthState.Error -> {
                // Error is shown inline
            }
            else -> {}
        }
    }

    // Handle password reset confirmation
    LaunchedEffect(resetEmailSent) {
        if (resetEmailSent) {
            snackbarHostState.showSnackbar("Password reset email sent! Check your inbox.")
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
                .verticalScroll(rememberScrollState())
        ) {
            // Geometric abstract art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                GeometricArt()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google G logo + Title
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    GoogleLogo()
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Login",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Welcome back to expectr",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Form
            Column(modifier = Modifier.padding(horizontal = 32.dp)) {

                // Error banner
                val errorMessage = (authState as? AuthViewModel.AuthState.Error)?.message
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                ZorvynRed.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                ZorvynRed.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = ZorvynRed,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                        if (authState is AuthViewModel.AuthState.Error) authViewModel.resetState()
                    },
                    placeholder = {
                        Text("Email", color = TextSecondary.copy(alpha = 0.5f))
                    },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it, color = ZorvynRed) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = ZorvynSurface,
                        focusedContainerColor = ZorvynSurface,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedBorderColor = AccentBlue,
                        errorBorderColor = ZorvynRed,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentBlue
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                        if (authState is AuthViewModel.AuthState.Error) authViewModel.resetState()
                    },
                    placeholder = {
                        Text("Password", color = TextSecondary.copy(alpha = 0.5f))
                    },
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it, color = ZorvynRed) } },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                                tint = TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = ZorvynSurface,
                        focusedContainerColor = ZorvynSurface,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedBorderColor = AccentBlue,
                        errorBorderColor = ZorvynRed,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentBlue
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Forgot Password
                Text(
                    text = "Forgot Password?",
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable {
                            if (email.isNotBlank() && email.contains("@")) {
                                authViewModel.sendPasswordReset(email)
                            } else {
                                emailError = "Enter your email first"
                            }
                        }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Sign In button
                Button(
                    onClick = {
                        // Validate
                        var valid = true
                        if (email.isBlank() || !email.contains("@")) {
                            emailError = "Enter a valid email"
                            valid = false
                        }
                        if (password.length < 6) {
                            passwordError = "Password must be at least 6 characters"
                            valid = false
                        }
                        if (valid) {
                            authViewModel.signInWithEmail(email, password)
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Sign In",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = TextSecondary.copy(alpha = 0.2f)
                    )
                    Text(
                        "  or continue with  ",
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = TextSecondary.copy(alpha = 0.2f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Google Sign-In Button
                OutlinedButton(
                    onClick = { authViewModel.signInWithGoogle(context) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ZorvynSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        GoogleLogo()
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Continue with Google",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sign up prompt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Don't have an account? ",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        "Sign Up",
                        color = AccentBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToSignUp() }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// ── Geometric Abstract Art (matching the design's overlapping shapes) ────────

@Composable
fun GeometricArt() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Large rounded rectangle (tilted)
        rotate(degrees = -8f, pivot = Offset(w * 0.55f, h * 0.4f)) {
            drawRoundRect(
                color = Color(0xFF1C2238),
                topLeft = Offset(w * 0.25f, h * 0.1f),
                size = Size(w * 0.45f, h * 0.55f),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 2f)
            )
            drawRoundRect(
                color = Color(0xFF1C2238).copy(alpha = 0.4f),
                topLeft = Offset(w * 0.25f, h * 0.1f),
                size = Size(w * 0.45f, h * 0.55f),
                cornerRadius = CornerRadius(24f, 24f)
            )
        }

        // Smaller overlapping square
        rotate(degrees = 5f, pivot = Offset(w * 0.35f, h * 0.35f)) {
            drawRoundRect(
                color = Color(0xFF252D42),
                topLeft = Offset(w * 0.12f, h * 0.15f),
                size = Size(w * 0.35f, h * 0.45f),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(width = 1.5f)
            )
            drawRoundRect(
                color = Color(0xFF252D42).copy(alpha = 0.25f),
                topLeft = Offset(w * 0.12f, h * 0.15f),
                size = Size(w * 0.35f, h * 0.45f),
                cornerRadius = CornerRadius(20f, 20f)
            )
        }

        // Gold/brown circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8B7355), Color(0xFF5C4D3A)),
                center = Offset(w * 0.22f, h * 0.68f),
                radius = w * 0.1f
            ),
            radius = w * 0.08f,
            center = Offset(w * 0.22f, h * 0.68f)
        )

        // Subtle accent line
        drawRoundRect(
            color = Color(0xFF2A3450).copy(alpha = 0.5f),
            topLeft = Offset(w * 0.6f, h * 0.45f),
            size = Size(w * 0.3f, h * 0.35f),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 1f)
        )
    }
}

// ── Google "G" Logo ──────────────────────────────────────────────────────────

@Composable
fun GoogleLogo() {
    Canvas(modifier = Modifier.size(48.dp)) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val strokeW = r * 0.38f

        // Blue arc (right / top-right)
        drawArc(
            color = GoogleBlue,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - r + strokeW / 2, cy - r + strokeW / 2),
            size = Size((r - strokeW / 2) * 2, (r - strokeW / 2) * 2),
            style = Stroke(width = strokeW)
        )
        // Green arc (bottom-right)
        drawArc(
            color = GoogleGreen,
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - r + strokeW / 2, cy - r + strokeW / 2),
            size = Size((r - strokeW / 2) * 2, (r - strokeW / 2) * 2),
            style = Stroke(width = strokeW)
        )
        // Yellow arc (bottom-left)
        drawArc(
            color = GoogleYellow,
            startAngle = 135f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(cx - r + strokeW / 2, cy - r + strokeW / 2),
            size = Size((r - strokeW / 2) * 2, (r - strokeW / 2) * 2),
            style = Stroke(width = strokeW)
        )
        // Red arc (top-left / top)
        drawArc(
            color = GoogleRed,
            startAngle = 205f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(cx - r + strokeW / 2, cy - r + strokeW / 2),
            size = Size((r - strokeW / 2) * 2, (r - strokeW / 2) * 2),
            style = Stroke(width = strokeW)
        )
        // Horizontal bar of the G
        drawRect(
            color = GoogleBlue,
            topLeft = Offset(cx, cy - strokeW / 2),
            size = Size(r * 0.55f, strokeW)
        )
    }
}
