package com.project.zorvynone.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: HomeViewModel, onNavigateHome: () -> Unit, onNavigateTxns: () -> Unit, onNavigateAdd: () -> Unit) {

    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpenses.collectAsStateWithLifecycle()

    val expensesByCategory = transactions
        .filter { !it.isIncome }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Unified color palette so the Donut Chart and Bars match perfectly
    val categoryColors = listOf(ZorvynRed, ZorvynBlueAction, Color(0xFFD4A055), Color(0xFFA288E3), ZorvynGreen)

    val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

    Scaffold(
        containerColor = ZorvynBackground,
        topBar = {
            TopAppBar(
                title = { Text("Insights", color = TextPrimary, fontWeight = FontWeight.Bold) },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(ZorvynSurface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(currentMonth, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ZorvynBackground)
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "insights",
                onHomeClick = onNavigateHome,
                onTxnsClick = onNavigateTxns,
                onAddClick = onNavigateAdd
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp)) // Added a bit more breathing room at the top

            // Saved vs Spent Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InsightSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "SAVED",
                    amount = totalIncome - totalExpense,
                    subtitle = if (totalIncome > 0) "On track" else "Add income",
                    color = ZorvynGreen
                )
                InsightSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "SPENT",
                    amount = totalExpense,
                    subtitle = "This month",
                    color = ZorvynRed
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Donut Chart Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ZorvynSurface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Spending by Category", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    if (expensesByCategory.isEmpty()) {
                        Text("No expenses yet this month.", color = TextSecondary.copy(alpha = 0.6f), fontSize = 14.sp)
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            // Custom Canvas Donut Chart
                            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                                DonutChart(expenses = expensesByCategory, totalExpense = totalExpense, colors = categoryColors)

                                // Center Text
                                val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
                                Text(
                                    text = "₹${formatter.format(totalExpense / 1000)}K",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            // Custom Legend
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                expensesByCategory.take(4).forEachIndexed { index, (category, amount) ->
                                    val percent = ((amount.toFloat() / totalExpense) * 100).toInt()
                                    LegendItem(color = categoryColors[index % categoryColors.size], label = category, percent = "$percent%")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Top Spending Categories
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ZorvynSurface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Top Spending Categories", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    if (expensesByCategory.isEmpty()) {
                        Text("Add some transactions to see your spending breakdown.", color = TextSecondary.copy(alpha = 0.6f), fontSize = 14.sp)
                    } else {
                        // Loop through real data!
                        expensesByCategory.take(3).forEachIndexed { index, (category, amount) ->
                            val progress = if (totalExpense > 0) amount.toFloat() / totalExpense.toFloat() else 0f

                            ProgressBarItem(
                                title = category,
                                progress = progress,
                                color = categoryColors[index % categoryColors.size] // Match donut chart colors
                            )

                            if (index < expensesByCategory.take(3).size - 1) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InsightSummaryCard(modifier: Modifier = Modifier, title: String, amount: Int, subtitle: String, color: Color) {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    Box(
        modifier = modifier
            .background(ZorvynSurface, RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("₹${formatter.format(amount)}", color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, color = color.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, percent: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = TextSecondary, fontSize = 14.sp)
        }
        Text(percent, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ProgressBarItem(title: String, progress: Float, color: Color) {
    var animatedProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(progress) { animatedProgress = progress }

    val progressAnim by animateFloatAsState(targetValue = animatedProgress, animationSpec = tween(1000), label = "")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = TextSecondary, fontSize = 14.sp)
            Text("${(progress * 100).toInt()}%", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progressAnim },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = TextSecondary.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun DonutChart(expenses: List<Pair<String, Int>>, totalExpense: Int, colors: List<Color>) {
    var animationPlayed by remember { mutableStateOf(false) }
    val sweepAngleAnim by animateFloatAsState(
        targetValue = if (animationPlayed) 360f else 0f,
        animationSpec = tween(1500),
        label = "donut_anim"
    )

    LaunchedEffect(true) { animationPlayed = true }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 24f
        var startAngle = -90f

        expenses.take(4).forEachIndexed { index, (_, amount) ->
            val sweepAngle = (amount.toFloat() / totalExpense) * sweepAngleAnim
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}