package com.project.zorvynone.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.model.SavingsGoal
import com.project.zorvynone.viewmodel.HomeViewModel
import java.util.Locale

// 1. Data Class Definition
data class ScoreHabit(
    val id: String,
    val icon: ImageVector,
    val iconTint: Color,
    val category: String,
    val title: String,
    val description: String,
    val points: Int,
    val statAmount: String,
    val statLabel: String,
    val acceptText: String,
    val rejectText: String
)

@Composable
fun scoreShimmerBrush(): Brush {
    val shimmerColors = listOf(Color.White.copy(alpha = 0.03f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onNavigateTxns: () -> Unit = {},
    onNavigateAdd: () -> Unit = {},
    onNavigateInsights: () -> Unit = {}
) {
    val baseScore by viewModel.baseScore.collectAsStateWithLifecycle()
    val premiumGold = Color(0xFFE5C158)

    fun getStatusText(s: Int): String = when { s >= 90 -> "Excellent"; s >= 74 -> "Good Standing"; s >= 50 -> "Fair"; else -> "Needs Attention" }
    fun getStatusColor(s: Int): Color = when { s >= 90 -> ZorvynGreen; s >= 74 -> premiumGold; s >= 50 -> Color(0xFFF59E0B); else -> ZorvynRed }

    val acceptedHabits by viewModel.acceptedHabitIds.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf("Spending") }

    val aiData by viewModel.scoreHabits.collectAsStateWithLifecycle()
    val defaultData by viewModel.defaultScoreHabits.collectAsStateWithLifecycle()
    val isScoreLoading by viewModel.isScoreLoading.collectAsStateWithLifecycle()
    val scoreError by viewModel.scoreError.collectAsStateWithLifecycle()

    val habits: List<ScoreHabit> = (aiData.ifEmpty { defaultData }).mapIndexed { index, data ->
        val icon = when(data.category.lowercase(Locale.ROOT)) {
            "food", "café" -> Icons.Default.Restaurant
            "shopping" -> Icons.Default.ShoppingCart
            "housing" -> Icons.Default.Home
            "transport" -> Icons.Default.DirectionsCar
            else -> Icons.Default.Receipt
        }
        val color = when(index % 3) { 0 -> ZorvynBlueText; 1 -> ZorvynRed; else -> Color(0xFFA288E3) }
        ScoreHabit("habit_$index", icon, color, data.category, data.title, data.description, data.points, data.statAmount, data.statLabel, data.acceptText, data.rejectText)
    }

    val totalBonus = habits.filter { acceptedHabits.contains(it.id) }.sumOf { h -> h.points }
    val projectedScore = (baseScore + totalBonus).coerceAtMost(100)
    val animatedScore by animateIntAsState(targetValue = projectedScore, animationSpec = tween(500), label = "score_anim")

    Scaffold(
        containerColor = ZorvynBackground,
        bottomBar = { BottomNavBar(currentRoute = "score", onHomeClick = onNavigateHome, onTxnsClick = onNavigateTxns, onAddClick = onNavigateAdd, onInsightsClick = onNavigateInsights, onScoreNavClick = {}) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.size(44.dp).background(ZorvynSurface, CircleShape).clickable { onNavigateBack() }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.height(32.dp))

            // HEADER
            Text(text = "EXPECTR HEALTH SCORE", color = TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(buildAnnotatedString { withStyle(SpanStyle(color = TextPrimary)) { append("The Score You\n") }; withStyle(SpanStyle(color = premiumGold)) { append("Control.") } }, fontSize = 42.sp, fontWeight = FontWeight.Black, lineHeight = 44.sp, letterSpacing = (-1.5).sp)
            Spacer(modifier = Modifier.height(20.dp))
            Text("Your score reacts instantly to your income, expenses and financial discipline.", color = TextSecondary.copy(alpha = 0.7f), fontSize = 15.sp, lineHeight = 22.sp)

            Spacer(modifier = Modifier.height(32.dp))

            // SCORE CARD
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ZorvynSurface), border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.15f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("YOUR SCORE", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("$baseScore", color = getStatusColor(baseScore), fontSize = 42.sp, fontWeight = FontWeight.Bold)
                        Text(getStatusText(baseScore), color = getStatusColor(baseScore), fontSize = 13.sp)
                    }
                    Box(modifier = Modifier.width(1.dp).height(80.dp).background(TextSecondary.copy(alpha = 0.15f)))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PROJECTED", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("$animatedScore", color = if (totalBonus > 0) getStatusColor(animatedScore) else TextSecondary.copy(alpha = 0.5f), fontSize = 42.sp, fontWeight = FontWeight.Bold)
                        if (totalBonus > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDropUp, null, tint = ZorvynGreen, modifier = Modifier.size(16.dp))
                                Text("+$totalBonus pts", color = ZorvynGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // TABS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = selectedTab == "Spending", onClick = { selectedTab = "Spending" }, label = { Text("Spending") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = premiumGold.copy(0.1f), selectedLabelColor = premiumGold))
                FilterChip(selected = selectedTab == "Savings", onClick = { selectedTab = "Savings" }, label = { Text("Savings") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = premiumGold.copy(0.1f), selectedLabelColor = premiumGold))
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == "Spending") {
                if (isScoreLoading) {
                    repeat(2) { ShimmerHabitCard(); Spacer(Modifier.height(16.dp)) }
                } else {
                    habits.forEach { habit ->
                        val isAccepted = acceptedHabits.contains(habit.id)
                        HabitCard(habit = habit, isAccepted = isAccepted, onAccept = { viewModel.toggleHabit(habit.id) }, onReject = { if(isAccepted) viewModel.toggleHabit(habit.id) })
                        Spacer(Modifier.height(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FIXED: ASK GEMINI AI BANNER
                Column(modifier = Modifier.fillMaxWidth().background(ZorvynSurface, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Text("YOUR PLAN", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2A2045), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isScoreLoading) {
                                // Triggers AI Score Card Refresh
                                viewModel.generateScoreHabits("")
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isScoreLoading) {
                            CircularProgressIndicator(color = Color(0xFFA288E3), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFA288E3), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isScoreLoading) "Analyzing data..." else "Ask Gemini AI", color = Color(0xFFA288E3), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Refresh for custom saving pro-tips", color = TextSecondary, fontSize = 13.sp)
                        }
                        if (!isScoreLoading) Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
                    }

                    // Error feedback
                    if (scoreError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().background(ZorvynRed.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = ZorvynRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(scoreError ?: "", color = ZorvynRed.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            } else {
                SmartVaultsContent(viewModel = viewModel)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // THE CRITICAL COMMIT BUTTON (Changes Score on Home)
            Button(
                onClick = {
                    viewModel.savePlan(totalBonus)
                    onNavigateBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = premiumGold),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Set My Plan", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── SMART VAULTS (SAVINGS TAB) ──────────────────────────────────────

@Composable
fun SmartVaultsContent(viewModel: HomeViewModel) {
    val vaults by viewModel.allVaults.collectAsStateWithLifecycle()
    val totalRoundUp by viewModel.totalRoundUpSavings.collectAsStateWithLifecycle()
    val totalSaved by viewModel.totalAllSavings.collectAsStateWithLifecycle()
    val streak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val longest by viewModel.longestStreak.collectAsStateWithLifecycle()
    val budget by viewModel.dailyBudget.collectAsStateWithLifecycle()
    val spent by viewModel.todaySpent.collectAsStateWithLifecycle()
    val underBudget by viewModel.isUnderBudgetToday.collectAsStateWithLifecycle()
    val advice by viewModel.savingsCoachAdvice.collectAsStateWithLifecycle()
    val coachLoading by viewModel.isSavingsCoachLoading.collectAsStateWithLifecycle()
    val premiumGold = Color(0xFFE5C158)
    var showCreate by remember { mutableStateOf(false) }
    var depositId by remember { mutableStateOf(-1) }
    var depositAmt by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.checkAndUpdateStreak() }

    Column {
        SavingsOverview(totalSaved, totalRoundUp)
        Spacer(Modifier.height(20.dp))
        StreakDashboard(streak, longest, viewModel.getStreakMilestone(streak), budget, spent, underBudget) { viewModel.setDailyBudget(it) }
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("YOUR VAULTS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("${vaults.size} active", color = premiumGold, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))

        if (vaults.isEmpty()) {
            EmptyVaultCard { showCreate = true }
        } else {
            vaults.forEach { v ->
                VaultCard(v, depositId == v.id, depositAmt,
                    onAmtChange = { depositAmt = it },
                    onToggle = { depositId = if (depositId == v.id) -1 else v.id; depositAmt = "" },
                    onDeposit = { depositAmt.toDoubleOrNull()?.let { a -> if (a > 0) { viewModel.depositToVault(v.id, a); depositId = -1; depositAmt = "" } } },
                    onRoundUp = { viewModel.setDefaultRoundUpVault(v.id) },
                    onDelete = { viewModel.deleteVault(v) })
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        if (showCreate) {
            CreateVaultForm(onCreate = { t, a, d, e, r -> viewModel.createVault(t, a, d, e, r); showCreate = false }, onCancel = { showCreate = false })
        } else {
            OutlinedButton(onClick = { showCreate = true }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, premiumGold.copy(0.3f))) {
                Icon(Icons.Default.Add, null, tint = premiumGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create New Vault", color = premiumGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        SavingsCoachCard(advice, coachLoading) { viewModel.generateSavingsCoachAdvice() }
    }
}

@Composable
fun SavingsOverview(totalSaved: Double, roundUp: Double) {
    val premiumGold = Color(0xFFE5C158)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F36)), border = BorderStroke(1.dp, premiumGold.copy(0.15f))) {
        Row(Modifier.fillMaxWidth().padding(20.dp), Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TOTAL SAVED", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text("₹${String.format("%,.0f", totalSaved)}", color = ZorvynGreen, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.width(1.dp).height(50.dp).background(TextSecondary.copy(0.15f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ROUND-UPS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text("₹${String.format("%,.0f", roundUp)}", color = premiumGold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StreakDashboard(streak: Int, longest: Int, milestone: String?, budget: Double, spent: Int, underBudget: Boolean, onSetBudget: (Double) -> Unit) {
    val premiumGold = Color(0xFFE5C158)
    var editing by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf("") }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ZorvynSurface), border = BorderStroke(1.dp, TextSecondary.copy(0.1f))) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("SAVINGS STREAK", color = premiumGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                if (milestone != null) Box(Modifier.background(premiumGold.copy(0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(milestone, fontSize = 12.sp, color = premiumGold, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Text("$streak", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                    Text("current", color = TextSecondary, fontSize = 12.sp)
                }
                Box(Modifier.width(1.dp).height(70.dp).background(TextSecondary.copy(0.1f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Text("$longest", color = premiumGold, fontSize = 36.sp, fontWeight = FontWeight.Black)
                    Text("longest", color = TextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("TODAY'S BUDGET", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(Modifier.clickable { editing = !editing }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = TextSecondary.copy(0.5f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("₹${budget.toInt()}", color = TextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            val prog = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1f) else 0f
            val barCol = if (underBudget) ZorvynGreen else ZorvynRed
            LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().height(8.dp), color = barCol, trackColor = Color.White.copy(0.08f), strokeCap = StrokeCap.Round)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("₹$spent spent", color = barCol, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(if (underBudget) "On track ✓" else "Over budget", color = barCol, fontSize = 12.sp)
            }
            if (editing) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                    OutlinedTextField(value = budgetInput, onValueChange = { budgetInput = it }, placeholder = { Text("New budget") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    Button(onClick = { budgetInput.toDoubleOrNull()?.let { onSetBudget(it); editing = false; budgetInput = "" } }, colors = ButtonDefaults.buttonColors(containerColor = premiumGold), shape = RoundedCornerShape(12.dp)) { Text("Set", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ── FIXED HABIT CARD ──────────────────────────────────────────

@Composable
fun HabitCard(habit: ScoreHabit, isAccepted: Boolean, onAccept: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = ZorvynSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isAccepted) ZorvynGreen.copy(alpha = 0.5f) else TextSecondary.copy(0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(40.dp).background(habit.iconTint.copy(0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(habit.icon, null, tint = habit.iconTint, modifier = Modifier.size(20.dp)) }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(habit.category, color = habit.iconTint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(habit.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(habit.description, color = TextSecondary, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().background(ZorvynBackground, RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDropUp, null, tint = ZorvynBlueText)
                    Text("pts to score", color = ZorvynBlueText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+${habit.points}", color = ZorvynBlueText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(habit.statAmount, color = TextSecondary, fontSize = 14.sp)
                    Text(habit.statLabel, color = TextSecondary.copy(0.5f), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isAccepted) ZorvynGreen.copy(0.15f) else ZorvynBackground, contentColor = if (isAccepted) ZorvynGreen else TextPrimary),
                    border = BorderStroke(1.dp, if (isAccepted) ZorvynGreen else TextSecondary.copy(0.2f)),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = habit.acceptText, textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZorvynBackground, contentColor = TextSecondary),
                    border = BorderStroke(1.dp, TextSecondary.copy(0.2f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = habit.rejectText, textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ShimmerHabitCard() {
    Box(Modifier.fillMaxWidth().height(160.dp).background(ZorvynSurface, RoundedCornerShape(20.dp)))
}

// ── VAULT ICON MAPPING ──────────────────────────────────────────────

fun vaultIcon(key: String): ImageVector = when (key) {
    "target" -> Icons.Default.GpsFixed
    "flight" -> Icons.Default.Flight
    "phone" -> Icons.Default.Smartphone
    "home" -> Icons.Default.Home
    "school" -> Icons.Default.School
    "diamond" -> Icons.Default.Diamond
    "car" -> Icons.Default.DirectionsCar
    "savings" -> Icons.Default.Savings
    else -> Icons.Default.Savings
}

// ── VAULT CARD ──────────────────────────────────────────────────────

@Composable
fun VaultCard(vault: SavingsGoal, isDepositing: Boolean, depositAmt: String, onAmtChange: (String) -> Unit, onToggle: () -> Unit, onDeposit: () -> Unit, onRoundUp: () -> Unit, onDelete: () -> Unit) {
    val premiumGold = Color(0xFFE5C158)
    val prog = if (vault.targetAmount > 0) (vault.savedAmount / vault.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val pct = (prog * 100).toInt()
    var confirmDelete by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ZorvynSurface), border = BorderStroke(1.dp, if (vault.isDefaultRoundUpVault) premiumGold.copy(0.4f) else TextSecondary.copy(0.1f))) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(Color.White.copy(0.07f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(vaultIcon(vault.iconEmoji), null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(vault.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (vault.isDefaultRoundUpVault) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Bolt, null, tint = premiumGold, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(2.dp)); Text("Round-up vault", color = premiumGold, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                Text("$pct%", color = if (pct >= 100) ZorvynGreen else premiumGold, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().height(6.dp), color = if (pct >= 100) ZorvynGreen else premiumGold, trackColor = Color.White.copy(0.08f), strokeCap = StrokeCap.Round)
            Spacer(Modifier.height(8.dp))
            Text("₹${String.format("%,.0f", vault.savedAmount)} / ₹${String.format("%,.0f", vault.targetAmount)}", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                Button(onClick = onToggle, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (isDepositing) premiumGold.copy(0.15f) else ZorvynBackground), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, premiumGold.copy(0.3f)), contentPadding = PaddingValues(8.dp)) {
                    Icon(Icons.Default.Add, null, tint = premiumGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Deposit", color = premiumGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (!vault.isDefaultRoundUpVault) {
                    OutlinedButton(onClick = onRoundUp, Modifier.weight(1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, TextSecondary.copy(0.2f)), contentPadding = PaddingValues(8.dp)) {
                        Icon(Icons.Default.Bolt, null, tint = TextSecondary, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Set Round-Up", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, null, tint = ZorvynRed.copy(0.6f), modifier = Modifier.size(18.dp))
                }
            }

            if (isDepositing) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                    OutlinedTextField(value = depositAmt, onValueChange = onAmtChange, placeholder = { Text("Amount") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    Button(onClick = onDeposit, colors = ButtonDefaults.buttonColors(containerColor = ZorvynGreen), shape = RoundedCornerShape(12.dp)) { Text("Add", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false }, confirmButton = { TextButton(onClick = { onDelete(); confirmDelete = false }) { Text("Delete", color = ZorvynRed) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }, title = { Text("Delete Vault?", color = Color.White) }, text = { Text("This will permanently remove \"${vault.title}\" and all its saved progress.", color = TextSecondary) }, containerColor = ZorvynSurface)
    }
}

@Composable
fun EmptyVaultCard(onCreate: () -> Unit) {
    val premiumGold = Color(0xFFE5C158)
    Card(Modifier.fillMaxWidth().clickable { onCreate() }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ZorvynSurface), border = BorderStroke(1.dp, premiumGold.copy(0.15f))) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).background(Color.White.copy(0.07f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountBalance, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            Spacer(Modifier.height(12.dp))
            Text("No vaults yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Create your first savings vault to start tracking goals.", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text("Tap to create →", color = premiumGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CreateVaultForm(onCreate: (String, Double, Long, String, Int) -> Unit, onCancel: () -> Unit) {
    val premiumGold = Color(0xFFE5C158)
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("target") }
    var months by remember { mutableStateOf("6") }
    var roundUp by remember { mutableStateOf("10") }
    val iconOptions = listOf("target" to Icons.Default.GpsFixed, "flight" to Icons.Default.Flight, "phone" to Icons.Default.Smartphone, "home" to Icons.Default.Home, "school" to Icons.Default.School, "diamond" to Icons.Default.Diamond, "car" to Icons.Default.DirectionsCar, "savings" to Icons.Default.Savings)

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ZorvynSurface), border = BorderStroke(1.dp, premiumGold.copy(0.2f))) {
        Column(Modifier.padding(20.dp)) {
            Text("NEW VAULT", color = premiumGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { iconOptions.forEach { (key, ic) -> Box(Modifier.size(40.dp).background(if (emoji == key) premiumGold.copy(0.15f) else Color.White.copy(0.05f), RoundedCornerShape(10.dp)).clickable { emoji = key }, contentAlignment = Alignment.Center) { Icon(ic, null, tint = if (emoji == key) Color.White else Color.White.copy(0.5f), modifier = Modifier.size(20.dp)) } } }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Vault name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target ₹") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = months, onValueChange = { months = it }, label = { Text("Months") }, modifier = Modifier.weight(0.6f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = roundUp, onValueChange = { roundUp = it }, label = { Text("Round-up rule ₹ (0=off)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                Button(onClick = {
                    val t = target.toDoubleOrNull() ?: return@Button
                    val m = months.toIntOrNull() ?: 6
                    val dl = System.currentTimeMillis() + (m.toLong() * 30 * 24 * 60 * 60 * 1000)
                    onCreate(title.ifBlank { "My Vault" }, t, dl, emoji, roundUp.toIntOrNull() ?: 10)
                }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = premiumGold), shape = RoundedCornerShape(12.dp)) { Text("Create", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun SavingsCoachCard(advice: String, isLoading: Boolean, onRefresh: () -> Unit) {
    val premiumGold = Color(0xFFE5C158)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2238)), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFA288E3), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("AI SAVINGS COACH", color = Color(0xFFA288E3), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }
                if (isLoading) CircularProgressIndicator(color = Color(0xFFA288E3), modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(16.dp))
            if (isLoading) {
                repeat(3) { Box(Modifier.fillMaxWidth().height(14.dp).padding(vertical = 4.dp).background(scoreShimmerBrush(), RoundedCornerShape(4.dp))) }
            } else {
                Text(advice, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRefresh, Modifier.fillMaxWidth().height(44.dp), enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2045)), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Refresh, null, tint = Color(0xFFA288E3), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Get Fresh Advice", color = Color(0xFFA288E3), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}