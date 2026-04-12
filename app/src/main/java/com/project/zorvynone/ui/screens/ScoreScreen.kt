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
                }
            } else {
                SavingsEngineDashboard(viewModel = viewModel)
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

// ── AI ACHIEVEMENT NAVIGATOR (SAVINGS TAB) ──────────────────────────────

@Composable
fun SavingsEngineDashboard(viewModel: HomeViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isGeneratingPlan by viewModel.isScoreLoading.collectAsStateWithLifecycle()
    val progress by viewModel.realGoalCoverage.collectAsStateWithLifecycle()
    val briefing by viewModel.aiMissionBriefing.collectAsStateWithLifecycle()
    val premiumGold = Color(0xFFE5C158)

    var isEditing by remember { mutableStateOf(false) }

    if (profile == null || isEditing) {
        SetupAchievementUI(onSave = { s, f, n, t, m ->
            viewModel.updateProfile(s, f, n, t, m)
            // Triggers AI Mission Briefing using True Surplus
            viewModel.generateAchievementPlan("")
            isEditing = false
        })
    } else {
        Column {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2238)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("AI ACHIEVEMENT NAVIGATOR", color = premiumGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        if (isGeneratingPlan) CircularProgressIndicator(color = premiumGold, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(text = profile?.goalName?.uppercase() ?: "YOUR GOAL", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(text = "Target: ₹${String.format("%,.0f", profile?.targetAmount)} • Timeline: ${profile?.targetMonths} Months", color = Color.White.copy(0.6f), fontSize = 13.sp)

                    Spacer(Modifier.height(24.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = premiumGold, trackColor = Color.White.copy(0.1f), strokeCap = StrokeCap.Round)

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(progress * 100).toInt()}% Net Savings Coverage", color = premiumGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("₹${String.format("%,.0f", profile?.targetAmount)}", color = Color.White.copy(0.3f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(Modifier.fillMaxWidth().background(ZorvynSurface, RoundedCornerShape(20.dp)).padding(20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = premiumGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("MISSION BRIEFING", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (isGeneratingPlan) {
                        repeat(3) { Box(Modifier.fillMaxWidth().height(14.dp).padding(vertical = 4.dp).background(scoreShimmerBrush(), RoundedCornerShape(4.dp))) }
                    } else {
                        Text(text = briefing, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().background(ZorvynSurface, RoundedCornerShape(20.dp)).padding(16.dp).clickable { isEditing = true }) {
                Text("RECONFIGURE ARCHITECT", color = premiumGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun SetupAchievementUI(onSave: (Double, Double, String, Double, Int) -> Unit) {
    var salary by remember { mutableStateOf("") }; var fixed by remember { mutableStateOf("") }
    var goalName by remember { mutableStateOf("") }; var target by remember { mutableStateOf("") }; var months by remember { mutableStateOf("5") }

    Column(Modifier.fillMaxWidth().background(ZorvynSurface, RoundedCornerShape(24.dp)).padding(24.dp)) {
        Text("Savings Architect", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Define your path to achievement.", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = goalName, onValueChange = { goalName = it }, label = { Text("Goal (e.g. iPhone)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target (₹)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = months, onValueChange = { months = it }, label = { Text("Months") }, modifier = Modifier.weight(0.6f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Monthly Salary (₹)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = fixed, onValueChange = { fixed = it }, label = { Text("Fixed Bills (₹)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onSave(salary.toDoubleOrNull() ?: 0.0, fixed.toDoubleOrNull() ?: 0.0, goalName, target.toDoubleOrNull() ?: 0.0, months.toIntOrNull() ?: 1) }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5C158)), shape = RoundedCornerShape(12.dp)) {
            Text("Launch Navigator", color = Color.Black, fontWeight = FontWeight.Bold)
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