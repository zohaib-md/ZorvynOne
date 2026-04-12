package com.project.zorvynone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.google.firebase.auth.FirebaseAuth
import com.project.zorvynone.model.AppDatabase
import com.project.zorvynone.model.AuthPrefs
import com.project.zorvynone.ui.screens.*
import com.project.zorvynone.ui.theme.ZorvynBackground
import com.project.zorvynone.ui.theme.ZorvynOneTheme
import com.project.zorvynone.viewmodel.AuthViewModel
import com.project.zorvynone.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install the official Splash API
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 2. Auth & Deep Link Check — Firebase is now source of truth
        val authPrefs = AuthPrefs(applicationContext)
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val navTarget = intent.getStringExtra("nav")
        val initialRoute = when {
            firebaseUser == null -> "login"
            navTarget == "add" -> "add_transaction"
            else -> "home"
        }

        // Sync AuthPrefs with Firebase state (for widget compat)
        if (firebaseUser != null) {
            authPrefs.isLoggedIn = true
            authPrefs.email = firebaseUser.email ?: ""
            authPrefs.username = firebaseUser.displayName ?: ""
        }

        val db = AppDatabase.getDatabase(applicationContext)
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
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
                    val homeViewModel: HomeViewModel = viewModel(factory = factory)
                    val authViewModel: AuthViewModel = viewModel()

                    if (showMainContent) {
                        AppNavigation(
                            homeViewModel = homeViewModel,
                            authViewModel = authViewModel,
                            startRoute = initialRoute,
                            authPrefs = authPrefs
                        )
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

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

    // Animations Logic
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )
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
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring"
    )

    // Particle Logic
    val p1y by infiniteTransition.animateFloat(0f, -18f, infiniteRepeatable(tween(2200), RepeatMode.Reverse))
    val p2y by infiniteTransition.animateFloat(0f, -12f, infiniteRepeatable(tween(1700, 300), RepeatMode.Reverse))
    val p3y by infiniteTransition.animateFloat(0f, -22f, infiniteRepeatable(tween(2600, 700), RepeatMode.Reverse))
    val p4y by infiniteTransition.animateFloat(0f, -10f, infiniteRepeatable(tween(1900, 500), RepeatMode.Reverse))
    val p5y by infiniteTransition.animateFloat(0f, -16f, infiniteRepeatable(tween(2400, 1000), RepeatMode.Reverse))
    val p6y by infiniteTransition.animateFloat(0f, -20f, infiniteRepeatable(tween(2000, 200), RepeatMode.Reverse))

    val scanLine by infiniteTransition.animateFloat(
        initialValue = -400f, targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "scan"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200)
        onAnimFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(Color(0xFF1C2238), Color(0xFF07090F)), radius = 2000f)),
        contentAlignment = Alignment.Center
    ) {
        // LAYER 1: Background Glow
        Box(modifier = Modifier.size(420.dp).graphicsLayer(alpha = glowPulse)
            .background(Brush.radialGradient(colors = listOf(Color(0xFFA288E3).copy(0.25f), Color(0xFF5B8DEF).copy(0.08f), Color.Transparent))))

        // LAYER 2 & 3: Rotating Rings
        Canvas(modifier = Modifier.size(260.dp).graphicsLayer(rotationZ = ringRotation, alpha = if (startAnimation) 0.18f else 0f)) {
            drawCircle(Color(0xFFA288E3), style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 18f), 0f)))
        }
        Canvas(modifier = Modifier.size(200.dp).graphicsLayer(rotationZ = -ringRotation * 0.6f, alpha = if (startAnimation) 0.12f else 0f)) {
            drawCircle(Color(0xFFE5C158), style = Stroke(0.7f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 24f), 0f)))
        }

        // LAYER 4: Floating Particles
        if (startAnimation) {
            val particles = listOf(
                Pair((-90).dp to (-60).dp, p1y), Pair(80.dp to (-80).dp, p2y), Pair((-110).dp to 30.dp, p3y),
                Pair(100.dp to 50.dp, p4y), Pair((-40).dp to 90.dp, p5y), Pair(60.dp to (-30).dp, p6y)
            )
            particles.forEach { (offsets, dy) ->
                Box(modifier = Modifier.offset(x = offsets.first, y = offsets.second + dy.dp).size(3.dp).background(Color(0xFFA288E3).copy(0.4f), CircleShape))
            }
        }

        // LAYER 5: Scan Line
        Canvas(modifier = Modifier.size(320.dp, 200.dp).graphicsLayer(alpha = 0.06f)) {
            drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White, Color.Transparent)), Offset(scanLine - 100f, 0f), Offset(scanLine + 100f, size.height), 1.5f)
        }

        // LAYER 6: Corner Accents
        if (startAnimation) {
            val cornerAlpha by animateFloatAsState(0.3f, tween(1200, 400), label = "corner")
            Canvas(modifier = Modifier.size(280.dp).graphicsLayer(alpha = cornerAlpha)) {
                val len = 28f; val col = Color(0xFFE5C158)
                drawLine(col, Offset(0f, 0f), Offset(len, 0f), 1f) // Top-Left
                drawLine(col, Offset(0f, 0f), Offset(0f, len), 1f)
                drawLine(col, Offset(size.width, 0f), Offset(size.width - len, 0f), 1f) // Top-Right
                drawLine(col, Offset(size.width, size.height), Offset(size.width, size.height - len), 1f) // Bottom-Right
            }
        }

        // LAYER 7: Branding
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale).graphicsLayer(alpha = alpha)) {
            Box {
                Text("expectr", color = Color.White.copy(0.1f), fontSize = 54.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-2).sp)
                Text("expectr", fontSize = 54.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-2).sp,
                    style = androidx.compose.ui.text.TextStyle(brush = Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(0.8f), Color.Transparent),
                        start = Offset(shimmerOffset - 200f, shimmerOffset - 200f), end = Offset(shimmerOffset, shimmerOffset))))
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedVisibility(visible = startAnimation, enter = fadeIn(tween(1000, 500)) + expandVertically(tween(1000, 500))) {
                Text("FINANCIAL INTELLIGENCE", color = Color(0xFFE5C158).copy(0.7f), fontSize = 11.sp, letterSpacing = 5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AppNavigation(
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    startRoute: String,
    authPrefs: AuthPrefs
) {
    val navController = rememberNavController()
    val navTo = { route: String ->
        if (navController.currentBackStackEntry?.destination?.route != route) {
            navController.navigate(route) {
                navController.graph.startDestinationRoute?.let { popUpTo(it) { saveState = true } }
                launchSingleTop = true; restoreState = true
            }
        }
    }

    NavHost(navController = navController, startDestination = startRoute) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { email ->
                    // Sync AuthPrefs for widget + offline compat
                    authPrefs.isLoggedIn = true
                    authPrefs.email = email
                    authPrefs.username = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }
        composable("signup") {
            SignUpScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = { username, email ->
                    // Sync AuthPrefs for widget + offline compat
                    authPrefs.isLoggedIn = true
                    authPrefs.username = username
                    authPrefs.email = email
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onScoreClick = { navController.navigate("score") },
                onAddClick = { navTo("add_transaction") },
                onTxnsClick = { navTo("transactions") },
                onInsightsClick = { navTo("insights") },
                onScoreNavClick = { navTo("score") },
                onSignOut = {
                    authViewModel.signOut()
                    authPrefs.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("transactions") { TransactionsScreen(homeViewModel, onNavigateHome = { navTo("home") }, onNavigateAdd = { navTo("add_transaction") }) }
        composable("insights") { InsightsScreen(homeViewModel, onNavigateHome = { navTo("home") }, onNavigateTxns = { navTo("transactions") }, onNavigateAdd = { navTo("add_transaction") }) }
        composable("score") { ScoreScreen(homeViewModel, onNavigateBack = { navController.popBackStack() }, onNavigateHome = { navTo("home") }, onNavigateTxns = { navTo("transactions") }, onNavigateAdd = { navTo("add_transaction") }, onNavigateInsights = { navTo("insights") }) }
        composable("add_transaction") { AddTransactionScreen(homeViewModel, onNavigateBack = { navController.popBackStack() }) }
    }
}