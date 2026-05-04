package com.project.zorvynone.ui.screens

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.firebase.auth.FirebaseAuth
import com.project.zorvynone.model.IconType
import com.project.zorvynone.model.Transaction
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.HomeViewModel
import com.project.zorvynone.KommunicateHelper
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

// --- GLOBAL SHIMMER BRUSH ---
@Composable
fun premiumShimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.03f),
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.03f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerAnim"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onScoreClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onTxnsClick: () -> Unit = {},
    onInsightsClick: () -> Unit = {},
    onScoreNavClick: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onChatClick: () -> Unit = {}
) {
    val balance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val income by viewModel.totalIncome.collectAsStateWithLifecycle()
    val expense by viewModel.totalExpenses.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val currentScore by viewModel.currentScore.collectAsStateWithLifecycle()

    val aiInsights by viewModel.aiInsights.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiError by viewModel.aiInsightsError.collectAsStateWithLifecycle()

    val isVoiceLoading by viewModel.isVoiceLoading.collectAsStateWithLifecycle()
    val isScannerLoading by viewModel.isScannerLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.processVoiceTransaction(spokenText)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text
                        if (extractedText.isNotBlank()) {
                            viewModel.processReceiptText(extractedText)
                        }
                    }
                    .addOnFailureListener { e -> e.printStackTrace() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- PREMIUM SPEED DIAL FAB STATE ---
    var isFabExpanded by remember { mutableStateOf(false) }

    // Main FAB rotation animation
    val fabRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 135f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "fab_rotation"
    )

    // Pulsing glow for the main FAB
    val infiniteTransition = rememberInfiniteTransition(label = "fab_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Speed dial option animations (staggered)
    val option1Offset by animateFloatAsState(
        targetValue = if (isFabExpanded) 0f else 80f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "opt1_offset"
    )
    val option1Alpha by animateFloatAsState(
        targetValue = if (isFabExpanded) 1f else 0f,
        animationSpec = tween(if (isFabExpanded) 200 else 100),
        label = "opt1_alpha"
    )
    val option2Offset by animateFloatAsState(
        targetValue = if (isFabExpanded) 0f else 60f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "opt2_offset"
    )
    val option2Alpha by animateFloatAsState(
        targetValue = if (isFabExpanded) 1f else 0f,
        animationSpec = tween(if (isFabExpanded) 300 else 100, delayMillis = if (isFabExpanded) 50 else 0),
        label = "opt2_alpha"
    )

    // Colors
    val fabGold = Color(0xFFE5C158)
    val fabDark = Color(0xFF141518)
    val fabSurface = Color(0xFF1C2238)

    Scaffold(
        containerColor = ZorvynBackground,
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            Column(
                modifier = Modifier
                    .offset(y = (-16).dp)
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // --- OPTION 2: Ask Experia (top) ---
                Row(
                    modifier = Modifier
                        .graphicsLayer(
                            alpha = option2Alpha,
                            translationY = option2Offset
                        )
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isFabExpanded) {
                        Box(
                            modifier = Modifier
                                .shadow(6.dp, RoundedCornerShape(8.dp))
                                .background(fabSurface, RoundedCornerShape(8.dp))
                                .border(0.5.dp, fabGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Ask Experia",
                                color = fabGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    FloatingActionButton(
                        onClick = {
                            isFabExpanded = false
                            onChatClick()
                        },
                        containerColor = fabDark,
                        contentColor = fabGold,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, fabGold.copy(alpha = 0.4f), CircleShape),
                        elevation = FloatingActionButtonDefaults.elevation(6.dp, 10.dp)
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = "Ask Experia", modifier = Modifier.size(20.dp))
                    }
                }

                // --- OPTION 1: Voice AI (closer to main FAB) ---
                Row(
                    modifier = Modifier
                        .graphicsLayer(
                            alpha = option1Alpha,
                            translationY = option1Offset
                        )
                        .padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isFabExpanded) {
                        Box(
                            modifier = Modifier
                                .shadow(6.dp, RoundedCornerShape(8.dp))
                                .background(fabSurface, RoundedCornerShape(8.dp))
                                .border(0.5.dp, fabGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (isVoiceLoading) "Processing..." else "Voice AI",
                                color = fabGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    FloatingActionButton(
                        onClick = {
                            isFabExpanded = false
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a transaction")
                            }
                            speechLauncher.launch(intent)
                        },
                        containerColor = fabDark,
                        contentColor = fabGold,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, fabGold.copy(alpha = 0.4f), CircleShape),
                        elevation = FloatingActionButtonDefaults.elevation(6.dp, 10.dp)
                    ) {
                        if (isVoiceLoading) {
                            CircularProgressIndicator(color = fabGold, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Mic, contentDescription = "Voice AI", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // --- MAIN FAB (Speed Dial Trigger) ---
                Box(contentAlignment = Alignment.Center) {
                    // Pulsing glow ring (only when collapsed)
                    if (!isFabExpanded) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(pulseScale)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            fabGold.copy(alpha = pulseAlpha),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                    }
                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        containerColor = fabDark,
                        contentColor = fabGold,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        fabGold.copy(alpha = 0.8f),
                                        fabGold.copy(alpha = 0.2f),
                                        fabGold.copy(alpha = 0.6f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        elevation = FloatingActionButtonDefaults.elevation(10.dp, 14.dp)
                    ) {
                        Icon(
                            imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.AutoAwesome,
                            contentDescription = if (isFabExpanded) "Close" else "AI Assistant",
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer(rotationZ = fabRotation)
                        )
                    }
                }
            }
        },

        bottomBar = {
            BottomNavBar(
                currentRoute = "home",
                onHomeClick = {},
                onAddClick = onAddClick,
                onTxnsClick = onTxnsClick,
                onInsightsClick = onInsightsClick,
                onScoreNavClick = onScoreNavClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ExpectrBranding(onSignOut = onSignOut)
            Spacer(modifier = Modifier.height(24.dp))
            PremiumHomeHeader()
            Spacer(modifier = Modifier.height(32.dp))

            TitaniumDebitCard(balance = balance, score = currentScore)
            Spacer(modifier = Modifier.height(24.dp))

            PremiumCredScoreCard(score = currentScore, onClick = onScoreClick)
            Spacer(modifier = Modifier.height(32.dp))

            PremiumAiInsightsSection(
                insights = aiInsights,
                isLoading = isAiLoading,
                errorMessage = aiError,
                onGenerateClick = {
                    viewModel.generateInsights()
                }
            )
            Spacer(modifier = Modifier.height(32.dp))

            PremiumScannerCard(
                isLoading = isScannerLoading,
                onClick = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                }
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Experia chatbot is now a FAB — no card here

            RecentTransactionsSection(transactions = transactions, onDelete = { txn -> viewModel.deleteTransaction(txn) })
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ExpectrBranding(onSignOut: () -> Unit = {}) {
    val premiumGold = Color(0xFFE5C158)
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val displayName = firebaseUser?.displayName ?: ""
    val userEmail = firebaseUser?.email ?: ""
    val initial = displayName.firstOrNull()?.uppercase()
        ?: userEmail.firstOrNull()?.uppercase()
        ?: "U"

    var showSidebar by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "expectr",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-1).sp
        )

        // Profile avatar — opens sidebar
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF1C2238), CircleShape)
                .border(1.dp, TextSecondary.copy(alpha = 0.2f), CircleShape)
                .clickable { showSidebar = true },
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }

    // --- Premium Gliding Sidebar ---
    PremiumSidebar(
        isOpen = showSidebar,
        onDismiss = { showSidebar = false },
        displayName = displayName,
        userEmail = userEmail,
        initial = initial,
        onSignOut = {
            showSidebar = false
            onSignOut()
        }
    )
}

@Composable
fun PremiumSidebar(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    displayName: String,
    userEmail: String,
    initial: String,
    onSignOut: () -> Unit
) {
    val premiumGold = Color(0xFFE5C158)
    val sidebarWidth = 300.dp

    // Slide animation — spring physics
    val offsetX by animateDpAsState(
        targetValue = if (isOpen) 0.dp else sidebarWidth + 20.dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sidebar_slide"
    )

    // Overlay dim animation
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isOpen) 0.55f else 0f,
        animationSpec = tween(300),
        label = "overlay_dim"
    )

    if (isOpen || offsetX < sidebarWidth + 15.dp) {
        Box(modifier = Modifier.fillMaxSize()) {
            // --- Dim overlay ---
            if (overlayAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = overlayAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDismiss() }
                )
            }

            // --- Sidebar panel ---
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(sidebarWidth)
                    .align(Alignment.CenterEnd)
                    .offset(x = offsetX)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF111418), Color(0xFF0C0E12))
                        ),
                        RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                premiumGold.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = 60.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // --- Close button ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // --- Profile section ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Large avatar
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                premiumGold.copy(alpha = 0.2f),
                                                Color(0xFF1C2238)
                                            )
                                        ),
                                        CircleShape
                                    )
                                    .border(
                                        2.dp,
                                        Brush.linearGradient(
                                            colors = listOf(
                                                premiumGold.copy(alpha = 0.5f),
                                                premiumGold.copy(alpha = 0.1f)
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    initial,
                                    color = premiumGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (displayName.isNotBlank()) {
                                Text(
                                    displayName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                userEmail,
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        // --- Divider ---
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    }

                    // --- Sign out at bottom ---
                    Column {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        Spacer(modifier = Modifier.height(16.dp))
                        SidebarMenuItem(
                            icon = Icons.Default.Logout,
                            label = "Sign Out",
                            tint = ZorvynRed,
                            onClick = onSignOut
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "expectr v1.0",
                            color = TextSecondary.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color = TextSecondary.copy(alpha = 0.8f),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PremiumHomeHeader() {
    val premiumGold = Color(0xFFE5C158)

    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greetingWord = when (currentHour) {
        in 5..11 -> "good morning"
        in 12..16 -> "good afternoon"
        in 17..20 -> "good evening"
        else -> "good night"
    }

    // Personalize with Firebase user name
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val firstName = firebaseUser?.displayName
        ?.split(" ")?.firstOrNull()?.replaceFirstChar { it.uppercase() }
        ?: "there"

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- Personalized greeting — Playfair Display Bold ---
        Text(
            text = "Hi $firstName, $greetingWord",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlayfairDisplay,
            letterSpacing = (-0.3).sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- Hero headline — Playfair Display Bold ---
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TextPrimary)) { append("Your money,\n") }
                withStyle(SpanStyle(color = premiumGold)) { append("fully in focus.") }
            },
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlayfairDisplay,
            lineHeight = 42.sp,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- Tagline ---
        Text(
            "Every rupee tracked. Every habit scored.",
            color = TextSecondary.copy(alpha = 0.6f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Your financial story, told clearly.",
            color = TextSecondary.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// --- TITANIUM DEBIT CARD ---

private val CardGold = Color(0xFFE5C158)
private val ChipGold = Color(0xFFD4AF37)

@Composable
fun TitaniumDebitCard(balance: Int, score: Int) {
    val animatedBalance by animateIntAsState(
        targetValue = balance,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "balanceAnim"
    )

    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Card Holder"

    // Press-down scale animation
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_press"
    )

    // Metallic gradient
    val titaniumGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF2A2D34), Color(0xFF1A1C22), Color(0xFF141518)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f) // ISO 7810 credit card ratio
            .scale(scale)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black,
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .background(titaniumGradient, RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP ROW: Chip + NFC + Brand ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // EMV Chip
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 30.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        ChipGold,
                                        Color(0xFFC9A036),
                                        ChipGold.copy(alpha = 0.8f)
                                    )
                                ),
                                RoundedCornerShape(6.dp)
                            )
                            .border(0.5.dp, ChipGold.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    ) {
                        // Chip lines
                        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                            val w = size.width
                            val h = size.height
                            val lineColor = Color(0xFFAA8820).copy(alpha = 0.5f)
                            // Horizontal lines
                            drawLine(lineColor, Offset(0f, h * 0.33f), Offset(w, h * 0.33f), strokeWidth = 0.8f)
                            drawLine(lineColor, Offset(0f, h * 0.66f), Offset(w, h * 0.66f), strokeWidth = 0.8f)
                            // Vertical center
                            drawLine(lineColor, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokeWidth = 0.8f)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // NFC icon
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "NFC",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Brand name
                Text(
                    text = "EXPECTR BLACK",
                    color = CardGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
            }

            // --- MIDDLE ROW: Vault Balance ---
            Column {
                Text(
                    text = "VAULT BALANCE",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 24.sp, color = Color.White.copy(alpha = 0.7f))) {
                            append("₹")
                        }
                        withStyle(SpanStyle(fontSize = 34.sp, color = Color.White, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)) {
                            append(formatter.format(animatedBalance))
                        }
                        withStyle(SpanStyle(fontSize = 18.sp, color = Color.White.copy(alpha = 0.4f), fontFamily = FontFamily.Monospace)) {
                            append(".00")
                        }
                    }
                )
            }

            // --- BOTTOM ROW: Architect + Health ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ARCHITECT",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = userName.uppercase(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "HEALTH",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$score/100",
                        color = CardGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumStatBox(modifier: Modifier = Modifier, title: String, amount: String, isIncome: Boolean) {
    val color = if (isIncome) ZorvynGreen else ZorvynRed
    val bgColor = if (isIncome) Color(0xFF193230) else Color(0xFF3A1D27)
    val icon = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Box(modifier = modifier.background(bgColor, RoundedCornerShape(16.dp)).border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(color.copy(alpha = 0.2f), CircleShape).padding(4.dp)) { Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp)) }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = TextSecondary.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(amount, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumCredScoreCard(score: Int, onClick: () -> Unit = {}) {
    val premiumGold = Color(0xFFE5C158)
    val statusText = when {
        score >= 90 -> "Excellent"
        score >= 74 -> "Good Standing"
        score >= 50 -> "Fair"
        else -> "Needs Attention"
    }

    val statusColor = when {
        score >= 90 -> ZorvynGreen
        score >= 74 -> premiumGold
        score >= 50 -> Color(0xFFF59E0B)
        else -> ZorvynRed
    }

    val rankMock = when {
        score >= 90 -> "Elite Financial Discipline"
        score >= 80 -> "High Discipline Tier"
        score >= 70 -> "Stable Saving Habit"
        score >= 50 -> "Developing Foundation"
        else -> "High Risk Profile"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ZorvynSurface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.15f))
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                val animatedProgress by animateFloatAsState(targetValue = score / 100f, animationSpec = tween(1000), label = "progress")
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = premiumGold,
                    trackColor = TextSecondary.copy(alpha = 0.1f),
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round
                )
                Text("$score", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                // UPDATED NAME
                Text("EXPECTR SCORE", color = TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$score", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" / 100", color = TextSecondary.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(statusText, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(rankMock, color = TextSecondary.copy(alpha = 0.5f), fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "View Details", tint = TextSecondary.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun PremiumAiInsightsSection(
    insights: List<String>,
    isLoading: Boolean,
    errorMessage: String? = null,
    onGenerateClick: () -> Unit
) {
    val geminiPurple = Color(0xFFA288E3)
    val geminiBg = Color(0xFF2A2045)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            HorizontalDivider(modifier = Modifier.width(24.dp), color = TextSecondary.copy(alpha = 0.2f))
            Text("  POWERED BY GEMINI  ", color = TextSecondary.copy(alpha = 0.4f), fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.weight(1f), color = TextSecondary.copy(alpha = 0.2f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = TextPrimary)) { append("AI ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = geminiPurple)) { append("Insights") }
            }, fontSize = 20.sp)

            Surface(onClick = onGenerateClick, enabled = !isLoading, shape = RoundedCornerShape(12.dp), color = geminiBg, border = BorderStroke(1.dp, geminiPurple.copy(alpha = 0.3f))) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isLoading) { CircularProgressIndicator(color = geminiPurple, modifier = Modifier.size(12.dp), strokeWidth = 2.dp) }
                    else { Icon(Icons.Default.Refresh, contentDescription = null, tint = geminiPurple, modifier = Modifier.size(14.dp)) }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh", color = geminiPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("★", color = geminiPurple, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = ZorvynSurface), border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.1f)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (isLoading) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        repeat(3) { index ->
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.padding(top = 5.dp).size(8.dp).background(premiumShimmerBrush(), CircleShape))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.fillMaxWidth().height(14.dp).background(premiumShimmerBrush(), RoundedCornerShape(4.dp)))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).background(premiumShimmerBrush(), RoundedCornerShape(4.dp)))
                                }
                            }
                            if (index < 2) { HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp)) }
                        }
                    }
                } else if (errorMessage != null) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(64.dp).background(Color(0xFF3A1D27).copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(48.dp).background(Color(0xFF3A1D27), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ZorvynRed, modifier = Modifier.size(24.dp)) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Something went wrong", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(errorMessage, color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tap Refresh to try again", color = geminiPurple.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (insights.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(64.dp).background(geminiBg.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(48.dp).background(geminiBg, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = geminiPurple, modifier = Modifier.size(24.dp)) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No insights yet", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tap Refresh to let Gemini AI analyse your\nspending patterns", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                    }
                } else {
                    val colors = listOf(ZorvynGreen, ZorvynRed, Color(0xFFE5C158))
                    insights.take(3).forEachIndexed { index, insightText ->
                        PremiumInsightItem(color = colors[index % colors.size], text = insightText)
                        if (index < insights.size - 1 && index < 2) { HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumScannerCard(isLoading: Boolean, onClick: () -> Unit) {
    val premiumGold = Color(0xFFE5C158)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isLoading) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2238)),
        border = BorderStroke(1.dp, premiumGold.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(premiumGold.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = premiumGold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Scan", tint = premiumGold)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Magic Receipt Scanner", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (isLoading) "Extracting data with ML Kit..." else "Upload an image to auto-fill", color = TextSecondary.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
            if (!isLoading) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun PremiumInsightItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.padding(top = 5.dp).size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = TextSecondary.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun RecentTransactionsSection(transactions: List<Transaction>, onDelete: (Transaction) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Swipe left to delete 👈", color = TextSecondary.copy(alpha = 0.4f), fontSize = 11.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Text("All", color = ZorvynBlueText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ZorvynBlueText, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp).background(ZorvynSurface, RoundedCornerShape(20.dp)).border(1.dp, TextSecondary.copy(alpha = 0.1f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No transactions yet", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        } else {
            transactions.forEach { txn ->
                key(txn.hashCode()) { SwipeableTransactionItem(txn = txn, onDelete = { onDelete(txn) }); Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionItem(txn: Transaction, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { dismissValue -> if (dismissValue == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false })
    SwipeToDismissBox(state = dismissState, enableDismissFromStartToEnd = false, backgroundContent = {
        val color by animateColorAsState(targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) ZorvynRed else Color.Transparent, label = "")
        Box(modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(16.dp)).padding(end = 24.dp), contentAlignment = Alignment.CenterEnd) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White) }
    }, content = {
        Box(modifier = Modifier.background(ZorvynBackground)) {
            val iconVec = when(txn.iconType) {
                IconType.WALLET -> Icons.Default.AccountBalanceWallet
                IconType.CAFE -> Icons.Default.LocalCafe
                IconType.SHOPPING -> Icons.Default.ShoppingCart
                IconType.FOOD -> Icons.Default.Restaurant
                IconType.HOUSING -> Icons.Default.Home
                IconType.TRANSPORT -> Icons.Default.DirectionsCar
                IconType.SALARY -> Icons.Default.Payments
                IconType.DEFAULT -> Icons.Default.Receipt
            }
            val sign = if (txn.isIncome) "+" else "-"
            val formattedAmt = NumberFormat.getNumberInstance(Locale("en", "IN")).format(txn.amount)
            TransactionItem(icon = iconVec, title = txn.title, subtitle = txn.subtitle, amount = "$sign₹$formattedAmt", isIncome = txn.isIncome)
        }
    })
}

@Composable
fun TransactionItem(icon: ImageVector, title: String, subtitle: String, amount: String, isIncome: Boolean) {
    // Premium icon styling — neutral surface with white icon, CRED-style
    val iconSurface = Color(0xFF0D0F12)
    val iconBorder = Color.White.copy(alpha = 0.08f)
    val iconTintColor = Color.White.copy(alpha = 0.85f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Premium icon container — dark surface, subtle border, white icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, iconBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Only the amount stays colored
        Text(
            text = amount,
            color = if (isIncome) ZorvynGreen else ZorvynRed,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- EXPERIA CHATBOT CARD ---
@Composable
fun ExperiaChatCard(onClick: () -> Unit) {
    val experiaPurple = Color(0xFFA288E3)
    val experiaBg = Color(0xFF2A2045)

    val infiniteTransition = rememberInfiniteTransition(label = "experia_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "experia_glow_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = experiaBg),
        border = BorderStroke(1.dp, experiaPurple.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Animated glow icon container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    experiaPurple.copy(alpha = glowAlpha * 0.4f),
                                    experiaPurple.copy(alpha = 0.1f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Ask Experia",
                        tint = experiaPurple,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Ask Experia",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your AI financial assistant",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .background(experiaPurple.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "CHAT",
                    color = experiaPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(
    currentRoute: String = "home",
    onHomeClick: () -> Unit = {},
    onTxnsClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onInsightsClick: () -> Unit = {},
    onScoreNavClick: () -> Unit = {}
) {
    val premiumGold = Color(0xFFE5C158)

    data class NavItem(
        val route: String,
        val icon: ImageVector,
        val label: String,
        val onClick: () -> Unit
    )

    val items = listOf(
        NavItem("home", Icons.Default.Home, "HOME", onHomeClick),
        NavItem("txns", Icons.Default.Receipt, "TXNS", onTxnsClick),
        NavItem("add", Icons.Default.AddCircleOutline, "ADD", onAddClick),
        NavItem("insights", Icons.Default.BarChart, "INSIGHTS", onInsightsClick),
        NavItem("score", Icons.Default.TrackChanges, "SCORE", onScoreNavClick)
    )

    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0C10))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
        ) {
            val tabWidth = maxWidth / items.size

            // --- Gliding capsule indicator ---
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = 300f
                ),
                label = "capsule_glide"
            )

            // Glow behind capsule
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .height(52.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                premiumGold.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    )
            )

            // Capsule surface
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .height(52.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1E26),
                                Color(0xFF14171D)
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                premiumGold.copy(alpha = 0.3f),
                                premiumGold.copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            // --- Tab items ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex

                    // Icon floats up when selected
                    val iconOffsetY by animateDpAsState(
                        targetValue = if (isSelected) (-2).dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "icon_lift_$index"
                    )

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 0.85f,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "icon_scale_$index"
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) premiumGold else Color.White.copy(alpha = 0.35f),
                        animationSpec = tween(220),
                        label = "icon_color_$index"
                    )

                    val labelAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(if (isSelected) 200 else 100),
                        label = "label_alpha_$index"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { item.onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.offset(y = iconOffsetY)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(21.dp)
                                    .scale(iconScale)
                            )
                            // Micro label under selected icon
                            if (labelAlpha > 0.01f) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.label,
                                    color = premiumGold.copy(alpha = labelAlpha),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp,
                                    modifier = Modifier.graphicsLayer(alpha = labelAlpha)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}