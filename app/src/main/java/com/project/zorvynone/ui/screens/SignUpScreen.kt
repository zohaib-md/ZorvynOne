package com.project.zorvynone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.AuthViewModel

private val AccentBlue = Color(0xFF2563EB)
private val FieldBg = Color(0xFF1A1F2E)
private val FieldBorder = Color(0xFF2A3040)

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

    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.Success -> {
                val user = (authState as AuthViewModel.AuthState.Success).user
                onRegisterSuccess(
                    user.displayName ?: username,
                    user.email ?: email
                )
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(containerColor = ZorvynBackground) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Back button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(ZorvynSurface, RoundedCornerShape(14.dp))
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

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            Text(
                text = "Create an\naccount",
                color = TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 42.sp,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sign up with",
                color = TextSecondary,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Google Sign-In button (full width)
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
                Text("Continue with Google", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider with text
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = TextSecondary.copy(alpha = 0.2f)
                )
                Text(
                    "  or fill in details  ",
                    color = TextSecondary.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = TextSecondary.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            // USERNAME field
            Text(
                "USERNAME",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    usernameError = null
                    if (authState is AuthViewModel.AuthState.Error) authViewModel.resetState()
                },
                placeholder = { Text("username", color = TextSecondary.copy(alpha = 0.4f)) },
                isError = usernameError != null,
                supportingText = usernameError?.let { { Text(it, color = ZorvynRed) } },
                leadingIcon = {
                    Icon(
                        Icons.Default.PersonOutline,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FieldBg,
                    focusedContainerColor = FieldBg,
                    unfocusedBorderColor = FieldBorder,
                    focusedBorderColor = AccentBlue,
                    errorBorderColor = ZorvynRed,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // EMAIL field
            Text(
                "EMAIL",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                    if (authState is AuthViewModel.AuthState.Error) authViewModel.resetState()
                },
                placeholder = { Text("example@mail.com", color = TextSecondary.copy(alpha = 0.4f)) },
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it, color = ZorvynRed) } },
                leadingIcon = {
                    Icon(
                        Icons.Default.MailOutline,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FieldBg,
                    focusedContainerColor = FieldBg,
                    unfocusedBorderColor = FieldBorder,
                    focusedBorderColor = AccentBlue,
                    errorBorderColor = ZorvynRed,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // PASSWORD field
            Text(
                "PASSWORD",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                    if (authState is AuthViewModel.AuthState.Error) authViewModel.resetState()
                },
                placeholder = { Text("••••••••", color = TextSecondary.copy(alpha = 0.4f)) },
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it, color = ZorvynRed) } },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                },
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
                    unfocusedContainerColor = FieldBg,
                    focusedContainerColor = FieldBg,
                    unfocusedBorderColor = FieldBorder,
                    focusedBorderColor = AccentBlue,
                    errorBorderColor = ZorvynRed,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Register button
            Button(
                onClick = {
                    // Validate
                    var valid = true
                    if (username.isBlank()) {
                        usernameError = "Username is required"
                        valid = false
                    }
                    if (email.isBlank() || !email.contains("@")) {
                        emailError = "Enter a valid email"
                        valid = false
                    }
                    if (password.length < 6) {
                        passwordError = "Password must be at least 6 characters"
                        valid = false
                    }
                    if (valid) {
                        authViewModel.signUpWithEmail(username, email, password)
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
                        "Register",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Login prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Already have an account? ",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    "Login",
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
