package com.project.zorvynone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.zorvynone.model.AppDatabase
import com.project.zorvynone.ui.screens.AddTransactionScreen
import com.project.zorvynone.ui.screens.HomeScreen
import com.project.zorvynone.ui.screens.InsightsScreen
import com.project.zorvynone.ui.screens.ScoreScreen
import com.project.zorvynone.ui.screens.TransactionsScreen
import com.project.zorvynone.ui.theme.ZorvynBackground
import com.project.zorvynone.ui.theme.ZorvynOneTheme
import com.project.zorvynone.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.Dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install the official Splash API (Must be before super.onCreate)
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(db.transactionDao()) as T
            }
        }

        setContent {
            ZorvynOneTheme {
                var showMainContent by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ZorvynBackground
                ) {
                    val viewModel: HomeViewModel = viewModel(factory = factory)

                    // 2. The Premium Entrance Transition
                    if (showMainContent) {
                        AppNavigation(viewModel = viewModel)
                    } else {
                        ExpectrAnimatedSplash(onAnimFinished = { showMainContent = true })
                    }
                }
            }
        }
    }
}

@Composable
fun ExpectrAnimatedSplash(onAnimFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    // --- Shimmer (unchanged logic) ---
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // --- Scale & alpha (unchanged logic) ---
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1.1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessVeryLow),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = "alpha"
    )

    // --- NEW: Pulsing central glow ---
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // --- NEW: Slow rotating outer ring ---
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    // --- NEW: Particle floats (6 particles, each with own Y offset) ---
    val p1y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -18f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p1"
    )
    val p2y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(1700, delayMillis = 300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p2"
    )
    val p3y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -22f,
        animationSpec = infiniteRepeatable(tween(2600, delayMillis = 700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p3"
    )
    val p4y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(1900, delayMillis = 500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p4"
    )
    val p5y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -16f,
        animationSpec = infiniteRepeatable(tween(2400, delayMillis = 1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p5"
    )
    val p6y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -20f,
        animationSpec = infiniteRepeatable(tween(2000, delayMillis = 200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p6"
    )

    // --- NEW: Diagonal scan line ---
    val scanLine by infiniteTransition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200)
        onAnimFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1C2238), Color(0xFF07090F)),
                    center = Offset.Unspecified,
                    radius = 2000f
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // ── LAYER 1: Deep background glow (existing, enhanced pulse) ──────────
        Box(
            modifier = Modifier
                .size(420.dp)
                .graphicsLayer(alpha = glowPulse)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFA288E3).copy(alpha = 0.25f),
                            Color(0xFF5B8DEF).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // ── LAYER 2: Rotating dashed-look ring ────────────────────────────────
        Canvas(
            modifier = Modifier
                .size(260.dp)
                .graphicsLayer(
                    rotationZ = ringRotation,
                    alpha = if (startAnimation) 0.18f else 0f
                )
        ) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(12f, 18f), 0f
                )
            )
            drawCircle(
                color = Color(0xFFA288E3),
                radius = size.minDimension / 2f,
                style = stroke
            )
        }

        // ── LAYER 3: Second counter-rotating ring (smaller, gold) ─────────────
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer(
                    rotationZ = -ringRotation * 0.6f,
                    alpha = if (startAnimation) 0.12f else 0f
                )
        ) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 0.7f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(6f, 24f), 0f
                )
            )
            drawCircle(
                color = Color(0xFFE5C158),
                radius = size.minDimension / 2f,
                style = stroke
            )
        }

        // ── LAYER 4: Floating particles ───────────────────────────────────────
        if (startAnimation) {
            // Particle data: (x offset dp, y base dp, size dp, color alpha, yAnim)
            data class Particle(val x: Dp, val yBase: Dp, val size: Dp, val colorAlpha: Float, val dy: Float)

            val particles = listOf(
                Particle((-90).dp, (-60).dp, 3.dp, 0.5f, p1y),
                Particle(80.dp, (-80).dp, 2.dp, 0.35f, p2y),
                Particle((-110).dp, 30.dp, 2.dp, 0.4f, p3y),
                Particle(100.dp, 50.dp, 3.5.dp, 0.3f, p4y),
                Particle((-40).dp, 90.dp, 2.dp, 0.45f, p5y),
                Particle(60.dp, (-30).dp, 2.5.dp, 0.4f, p6y)
            )

            particles.forEach { p ->
                Box(
                    modifier = Modifier
                        .offset(x = p.x, y = p.yBase + p.dy.dp)
                        .size(p.size)
                        .background(
                            color = Color(0xFFA288E3).copy(alpha = p.colorAlpha),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }

            // Two gold accent particles
            Box(
                modifier = Modifier
                    .offset(x = (-70).dp, y = 70.dp + p2y.dp)
                    .size(2.dp)
                    .background(
                        color = Color(0xFFE5C158).copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .offset(x = 85.dp, y = (-55).dp + p4y.dp)
                    .size(2.dp)
                    .background(
                        color = Color(0xFFE5C158).copy(alpha = 0.45f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }

        // ── LAYER 5: Diagonal scan line ───────────────────────────────────────
        Canvas(
            modifier = Modifier
                .size(320.dp, 200.dp)
                .graphicsLayer(alpha = 0.06f)
        ) {
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White,
                        Color.Transparent
                    )
                ),
                start = Offset(scanLine - 100f, 0f),
                end = Offset(scanLine + 100f, size.height),
                strokeWidth = 1.5f
            )
        }

        // ── LAYER 6: Four corner accent lines ─────────────────────────────────
        if (startAnimation) {
            val cornerAlpha by animateFloatAsState(
                targetValue = 0.3f,
                animationSpec = tween(1200, delayMillis = 400),
                label = "cornerAlpha"
            )
            Canvas(modifier = Modifier.size(280.dp).graphicsLayer(alpha = cornerAlpha)) {
                val w = size.width
                val h = size.height
                val len = 28f
                val strokeW = 1f
                val col = Color(0xFFE5C158)
                // Top-left
                drawLine(col, Offset(0f, 0f), Offset(len, 0f), strokeW)
                drawLine(col, Offset(0f, 0f), Offset(0f, len), strokeW)
                // Top-right
                drawLine(col, Offset(w, 0f), Offset(w - len, 0f), strokeW)
                drawLine(col, Offset(w, 0f), Offset(w, len), strokeW)
                // Bottom-left
                drawLine(col, Offset(0f, h), Offset(len, h), strokeW)
                drawLine(col, Offset(0f, h), Offset(0f, h - len), strokeW)
                // Bottom-right
                drawLine(col, Offset(w, h), Offset(w - len, h), strokeW)
                drawLine(col, Offset(w, h), Offset(w, h - len), strokeW)
            }
        }

        // ── LAYER 7: The original logo + tagline (UNCHANGED) ──────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale).graphicsLayer(alpha = alpha)
        ) {
            Box {
                Text(
                    text = "expectr",
                    color = Color.White.copy(alpha = 0.1f),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "expectr",
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (-2).sp,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.8f),
                                Color.Transparent
                            ),
                            start = Offset(shimmerOffset - 200f, shimmerOffset - 200f),
                            end = Offset(shimmerOffset, shimmerOffset)
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 500)) +
                        expandVertically(animationSpec = tween(1000, delayMillis = 500))
            ) {
                Text(
                    text = "FINANCIAL INTELLIGENCE",
                    color = Color(0xFFE5C158).copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    letterSpacing = 5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: HomeViewModel) {
    val navController = rememberNavController()

    // Helper for Bottom Nav logic
    val navTo = { route: String ->
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != route) {
            navController.navigate(route) {
                navController.graph.startDestinationRoute?.let { startRoute ->
                    popUpTo(startRoute) { saveState = true }
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onScoreClick = { navController.navigate("score") },
                onAddClick = { navTo("add_transaction") },
                onTxnsClick = { navTo("transactions") },
                onInsightsClick = { navTo("insights") },
                onScoreNavClick = { navTo("score") }
            )
        }

        composable("transactions") {
            TransactionsScreen(
                viewModel = viewModel,
                onNavigateHome = { navTo("home") },
                onNavigateAdd = { navTo("add_transaction") }
            )
        }

        composable("insights") {
            InsightsScreen(
                viewModel = viewModel,
                onNavigateHome = { navTo("home") },
                onNavigateTxns = { navTo("transactions") },
                onNavigateAdd = { navTo("add_transaction") }
            )
        }

        composable("score") {
            ScoreScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = { navTo("home") },
                onNavigateTxns = { navTo("transactions") },
                onNavigateAdd = { navTo("add_transaction") },
                onNavigateInsights = { navTo("insights") }
            )
        }

        composable("add_transaction") {
            AddTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}