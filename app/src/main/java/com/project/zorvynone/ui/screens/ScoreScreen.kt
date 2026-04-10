package com.project.zorvynone.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.HomeViewModel

data class ScoreHabit(
    val id: String, val icon: ImageVector, val iconTint: Color,
    val category: String, val title: String, val description: String,
    val points: Int, val statAmount: String, val statLabel: String,
    val acceptText: String, val rejectText: String
)

@Composable
fun scoreShimmerBrush(): Brush {
    val shimmerColors = listOf(Color.White.copy(alpha = 0.03f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f))
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(initialValue = 0f, targetValue = 2000f, animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "shimmerAnim")
    return Brush.linearGradient(colors = shimmerColors, start = Offset.Zero, end = Offset(x = translateAnim, y = translateAnim))
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

    val getStatusText: (Int) -> String = { s -> when { s >= 90 -> "Excellent"; s >= 74 -> "Good Standing"; s >= 50 -> "Fair"; else -> "Needs Attention" } }
    val getStatusColor: (Int) -> Color = { s -> when { s >= 90 -> ZorvynGreen; s >= 74 -> premiumGold; s >= 50 -> Color(0xFFF59E0B); else -> ZorvynRed } }

    val acceptedHabits by viewModel.acceptedHabitIds.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf("Spending") }

    val aiData by viewModel.scoreHabits.collectAsStateWithLifecycle()
    val defaultData by viewModel.defaultScoreHabits.collectAsStateWithLifecycle()
    val isScoreLoading by viewModel.isScoreLoading.collectAsStateWithLifecycle()

    val activeData = aiData.ifEmpty { defaultData }

    val habits = if (activeData.isNotEmpty()) {
        activeData.mapIndexed { index, data ->
            val icon = when(data.category.lowercase(java.util.Locale.ROOT)) { "food", "café" -> Icons.Default.Restaurant; "shopping" -> Icons.Default.ShoppingCart; "housing" -> Icons.Default.Home; "transport" -> Icons.Default.DirectionsCar; else -> Icons.Default.Receipt }
            val color = when(index % 3) { 0 -> ZorvynBlueText; 1 -> ZorvynRed; else -> Color(0xFFA288E3) }
            ScoreHabit(id = "habit_$index", icon = icon, iconTint = color, category = data.category, title = data.title, description = data.description, points = data.points, statAmount = data.statAmount, statLabel = data.statLabel, acceptText = data.acceptText, rejectText = data.rejectText)
        }
    } else {
        listOf(ScoreHabit("dummy", Icons.Default.AccountBalanceWallet, ZorvynBlueText, "NO DATA YET", "Add expenses to get insights", "We need some transaction data first.", 0, "₹0", "spent", "Okay", "Skip"))
    }

    val totalBonus = habits.filter { acceptedHabits.contains(it.id) }.sumOf { it.points }
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

            Column(modifier = Modifier.fillMaxWidth()) {
                // REBRANDED HEADER
                Text(text = "EXPECTR HEALTH SCORE", color = TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(buildAnnotatedString { withStyle(SpanStyle(color = TextPrimary)) { append("The Score You\n") }; withStyle(SpanStyle(color = premiumGold)) { append("Control.") } }, fontSize = 42.sp, fontWeight = FontWeight.Black, lineHeight = 44.sp, letterSpacing = (-1.5).sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            // UPDATED COPY
            Text(text = "Unlike traditional bureau scores, your expectr Score reacts instantly to your income, expenses and financial discipline.", color = TextSecondary.copy(alpha = 0.7f), fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(32.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ZorvynSurface), border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.15f))) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("YOUR\nSCORE", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("$baseScore", color = getStatusColor(baseScore), fontSize = 42.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(getStatusText(baseScore), color = getStatusColor(baseScore), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Box(modifier = Modifier.width(1.dp).height(100.dp).background(TextSecondary.copy(alpha = 0.15f)))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("IF YOU DO\nTHIS", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("$animatedScore", color = if (totalBonus > 0) getStatusColor(animatedScore) else TextSecondary.copy(alpha = 0.5f), fontSize = 42.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (totalBonus > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(ZorvynGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Icon(Icons.Default.ArrowDropUp, contentDescription = null, tint = ZorvynGreen, modifier = Modifier.size(16.dp))
                                    Text("+$totalBonus pts", color = ZorvynGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            } else { Text("No change", color = TextSecondary.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
                        Column(modifier = Modifier.weight(1f)) { Text("CURRENT", color = premiumGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Spacer(modifier = Modifier.height(6.dp)); Box(modifier = Modifier.height(2.dp).fillMaxWidth(0.85f).background(premiumGold)) }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) { Text("PROJECTED", color = TextSecondary.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Spacer(modifier = Modifier.height(6.dp)); Box(modifier = Modifier.height(2.dp).fillMaxWidth(0.85f).background(TextSecondary.copy(alpha = 0.2f))) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = selectedTab == "Spending", onClick = { selectedTab = "Spending" }, label = { Text("Spending", modifier = Modifier.padding(horizontal = 8.dp)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = premiumGold.copy(alpha = 0.15f), selectedLabelColor = premiumGold, containerColor = ZorvynSurface, labelColor = TextSecondary), border = null, shape = RoundedCornerShape(12.dp))
                FilterChip(selected = selectedTab == "Savings", onClick = { selectedTab = "Savings" }, label = { Text("Savings", modifier = Modifier.padding(horizontal = 8.dp)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = premiumGold.copy(alpha = 0.15f), selectedLabelColor = premiumGold, containerColor = ZorvynSurface, labelColor = TextSecondary), border = null, shape = RoundedCornerShape(12.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == "Spending") {
                if (isScoreLoading) {
                    repeat(2) {
                        ShimmerHabitCard()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    habits.forEach { habit ->
                        val isAccepted = acceptedHabits.contains(habit.id)
                        HabitCard(habit = habit, isAccepted = isAccepted, onAccept = { viewModel.toggleHabit(habit.id) }, onReject = { if(isAccepted) viewModel.toggleHabit(habit.id) })
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth().background(ZorvynSurface, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Text("YOUR PLAN", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tap below to generate a personalized AI plan.", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2045), RoundedCornerShape(12.dp)).clickable(enabled = !isScoreLoading) { viewModel.generateScoreHabits("AIzaSyC8Ed1CGHWwlRw0oEvedgRwIGL0_ijjcE8") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFFA288E3), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ask Gemini AI", color = Color(0xFFA288E3), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Refresh with real-time spending data", color = TextSecondary, fontSize = 13.sp)
                        }
                        if (isScoreLoading) CircularProgressIndicator(color = Color(0xFFA288E3), modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().background(ZorvynSurface, RoundedCornerShape(20.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = premiumGold, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Savings Engine", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI-driven savings goals and compounding projections are coming in Version 2.0!\n\nFocus on reducing your spending habits first.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.savePlan(totalBonus); onNavigateBack() }, colors = ButtonDefaults.buttonColors(containerColor = premiumGold.copy(alpha = if (acceptedHabits.isNotEmpty()) 1f else 0.5f)), modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), enabled = acceptedHabits.isNotEmpty()) {
                Icon(Icons.Default.Check, contentDescription = null, tint = ZorvynBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set My Plan", color = ZorvynBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ShimmerHabitCard() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ZorvynSurface), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(40.dp).background(scoreShimmerBrush(), RoundedCornerShape(12.dp)))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.width(60.dp).height(12.dp).background(scoreShimmerBrush(), RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(scoreShimmerBrush(), RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).background(scoreShimmerBrush(), RoundedCornerShape(4.dp)))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth().height(48.dp).background(scoreShimmerBrush(), RoundedCornerShape(12.dp)))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f).height(48.dp).background(scoreShimmerBrush(), RoundedCornerShape(12.dp)))
                Box(modifier = Modifier.weight(1f).height(48.dp).background(scoreShimmerBrush(), RoundedCornerShape(12.dp)))
            }
        }
    }
}

@Composable
fun HabitCard(habit: ScoreHabit, isAccepted: Boolean, onAccept: () -> Unit, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.cardColors(containerColor = ZorvynSurface), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, if (isAccepted) ZorvynGreen.copy(alpha = 0.5f) else TextSecondary.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(40.dp).background(habit.iconTint.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(habit.icon, contentDescription = null, tint = habit.iconTint, modifier = Modifier.size(20.dp)) }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(habit.category, color = habit.iconTint, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Text(habit.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(habit.description, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth().background(ZorvynBackground, RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDropUp, contentDescription = null, tint = ZorvynBlueText)
                    Column { Text("pts to", color = ZorvynBlueText, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text("score", color = ZorvynBlueText, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+${habit.points}", color = ZorvynBlueText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(TextSecondary.copy(alpha = 0.2f)))
                Column(horizontalAlignment = Alignment.End) { Text(habit.statAmount, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium); Text(habit.statLabel, color = TextSecondary.copy(alpha = 0.5f), fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = if (isAccepted) ZorvynGreen.copy(alpha = 0.15f) else ZorvynBackground, contentColor = if (isAccepted) ZorvynGreen else TextPrimary), border = BorderStroke(1.dp, if (isAccepted) ZorvynGreen else TextSecondary.copy(alpha = 0.2f)), modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                    if (isAccepted) { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)) }
                    Text(text = habit.acceptText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 16.sp, maxLines = 2)
                }
                Button(onClick = onReject, colors = ButtonDefaults.buttonColors(containerColor = ZorvynBackground, contentColor = TextSecondary), border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.2f)), modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                    Text(text = habit.rejectText, fontWeight = FontWeight.Medium, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 16.sp, maxLines = 2)
                }
            }
        }
    }
}