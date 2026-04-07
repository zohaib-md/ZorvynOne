package com.project.zorvynone.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.project.zorvynone.model.IconType
import com.project.zorvynone.model.Transaction
import com.project.zorvynone.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// --- NEW GLOBAL SHIMMER BRUSH ---
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
    onScoreNavClick: () -> Unit = {}
) {
    val balance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val income by viewModel.totalIncome.collectAsStateWithLifecycle()
    val expense by viewModel.totalExpenses.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val currentScore by viewModel.currentScore.collectAsStateWithLifecycle()

    val aiInsights by viewModel.aiInsights.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    val isVoiceLoading by viewModel.isVoiceLoading.collectAsStateWithLifecycle()

    // --- NEW: NATIVE SPEECH RECOGNIZER LAUNCHER ---
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                // ⚠️ PASTE YOUR API KEY HERE ⚠️
                viewModel.processVoiceTransaction(spokenText, "AIzaSyBv5gW1DjAbe0mj65vJlns0pS9sCbNRFEs")
            }
        }
    }

    Scaffold(
        containerColor = ZorvynBackground,
        // 1. Move it back to the right side (Material Design Standard)
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                // 2. Add a slight upward offset so it floats elegantly above the nav bar
                modifier = Modifier.offset(y = (-16).dp),
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a transaction (e.g., 'I spent ₹450 on a cab')")
                    }
                    speechLauncher.launch(intent)
                },
                containerColor = Color(0xFF2A2045),
                contentColor = Color(0xFFA288E3),
                shape = RoundedCornerShape(50)
            ) {
                if (isVoiceLoading) {
                    CircularProgressIndicator(color = Color(0xFFA288E3), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Processing...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Assistant", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Voice AI", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp)
                    // (I also shortened the text slightly to "Voice AI" so it doesn't block too much screen space!)
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
        // ... The rest of your HomeScreen code stays exactly the same ...->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ZorvynOneBranding()
            Spacer(modifier = Modifier.height(24.dp))
            PremiumHomeHeader()
            Spacer(modifier = Modifier.height(32.dp))

            PremiumBalanceCard(balance = balance, income = income, expense = expense)
            Spacer(modifier = Modifier.height(24.dp))

            PremiumCredScoreCard(score = currentScore, onClick = onScoreClick)
            Spacer(modifier = Modifier.height(32.dp))

            PremiumAiInsightsSection(
                insights = aiInsights,
                isLoading = isAiLoading,
                onGenerateClick = {
                    viewModel.generateInsights("AIzaSyBv5gW1DjAbe0mj65vJlns0pS9sCbNRFEs")
                }
            )
            Spacer(modifier = Modifier.height(32.dp))

            RecentTransactionsSection(transactions = transactions, onDelete = { txn -> viewModel.deleteTransaction(txn) })
            Spacer(modifier = Modifier.height(80.dp)) // Added extra padding so the list isn't hidden behind the FAB
        }
    }
}

@Composable
fun ZorvynOneBranding() {
    val premiumGold = Color(0xFFE5C158)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(buildAnnotatedString {
            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Black)) { append("ZORVYN") }
            withStyle(SpanStyle(color = premiumGold, fontWeight = FontWeight.Black)) { append("ONE") }
        }, fontSize = 20.sp, letterSpacing = 2.sp)
        Box(modifier = Modifier.size(40.dp).background(Color(0xFF1C2238), CircleShape).border(1.dp, TextSecondary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
            Text("U", color = premiumGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun PremiumHomeHeader() {
    val premiumGold = Color(0xFFE5C158)
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 5..11 -> "GOOD MORNING"
        in 12..16 -> "GOOD AFTERNOON"
        in 17..20 -> "GOOD EVENING"
        else -> "GOOD NIGHT"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.border(1.dp, premiumGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).background(premiumGold.copy(alpha = 0.05f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(text = "• $greeting", color = premiumGold, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(buildAnnotatedString {
            withStyle(SpanStyle(color = TextPrimary)) { append("Your money,\n") }
            withStyle(SpanStyle(color = premiumGold)) { append("fully in focus.") }
        }, fontSize = 36.sp, fontWeight = FontWeight.Black, lineHeight = 40.sp, letterSpacing = (-1.5).sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Every rupee tracked. Every habit scored.\n", color = TextSecondary.copy(alpha = 0.7f), fontSize = 15.sp, lineHeight = 22.sp)
        Text("Your financial story, told clearly.", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PremiumBalanceCard(balance: Int, income: Int, expense: Int) {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    val cardBg = Color(0xFF1C2238)
    val cardBorder = Color(0xFF2A314A)
    val savingsRate = if (income > 0) ((income - expense).toFloat() / income.toFloat() * 100).toInt() else 0
    val isPositive = savingsRate >= 0
    val displayRate = abs(savingsRate)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = cardBg), border = BorderStroke(1.dp, cardBorder)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("TOTAL BALANCE", color = TextSecondary.copy(alpha = 0.8f), fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("₹${formatter.format(balance)}", color = TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                Text(".00", color = TextSecondary.copy(alpha = 0.6f), fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (income > 0) {
                val pillColor = if (isPositive) ZorvynGreen else ZorvynRed
                val icon = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
                val sign = if (isPositive) "+" else "-"

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.border(1.dp, pillColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).background(pillColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = pillColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$sign$displayRate%", color = pillColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(if (isPositive) "saved this month" else "overspent this month", color = TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            } else {
                Text("Add income to track your savings rate", color = TextSecondary.copy(alpha = 0.5f), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PremiumStatBox(modifier = Modifier.weight(1f), title = "INCOME", amount = "₹${formatter.format(income)}", isIncome = true)
                PremiumStatBox(modifier = Modifier.weight(1f), title = "EXPENSES", amount = "₹${formatter.format(expense)}", isIncome = false)
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
    val statusText = when { score >= 90 -> "Excellent"; score >= 74 -> "Good Standing"; score >= 50 -> "Fair"; else -> "Needs Attention" }
    val statusColor = when { score >= 90 -> ZorvynGreen; score >= 74 -> premiumGold; score >= 50 -> Color(0xFFF59E0B); else -> ZorvynRed }
    val rankMock = when { score >= 90 -> "Top 2% of all users"; score >= 80 -> "Top 15% of all users"; score >= 70 -> "Top 40% of all users"; else -> "Below average" }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ZorvynSurface), border = BorderStroke(1.dp, statusColor.copy(alpha = 0.15f))) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                val animatedProgress by animateFloatAsState(targetValue = score / 100f, animationSpec = tween(1000), label = "progress")
                CircularProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxSize(), color = premiumGold, trackColor = TextSecondary.copy(alpha = 0.1f), strokeWidth = 5.dp, strokeCap = StrokeCap.Round)
                Text("$score", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("ZORVYN SCORE", color = TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
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
                    // NEW: SHIMMER EFFECT FOR AI INSIGHTS
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
            val tint = if (txn.isIncome) ZorvynGreen else ZorvynRed
            val sign = if (txn.isIncome) "+" else "-"
            val formattedAmt = NumberFormat.getNumberInstance(Locale("en", "IN")).format(txn.amount)
            TransactionItem(icon = iconVec, iconTint = tint, title = txn.title, subtitle = txn.subtitle, amount = "$sign₹$formattedAmt", isIncome = txn.isIncome)
        }
    })
}

@Composable
fun TransactionItem(icon: ImageVector, iconTint: Color, title: String, subtitle: String, amount: String, isIncome: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(iconTint.copy(alpha = 0.1f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp)) }
            Spacer(modifier = Modifier.width(16.dp))
            Column { Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(2.dp)); Text(subtitle, color = TextSecondary.copy(alpha = 0.8f), fontSize = 13.sp) }
        }
        Text(amount, color = if (isIncome) ZorvynGreen else ZorvynRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BottomNavBar(currentRoute: String = "home", onHomeClick: () -> Unit = {}, onTxnsClick: () -> Unit = {}, onAddClick: () -> Unit = {}, onInsightsClick: () -> Unit = {}, onScoreNavClick: () -> Unit = {}) {
    val premiumGold = Color(0xFFE5C158)
    NavigationBar(containerColor = ZorvynBackground, contentColor = TextSecondary, tonalElevation = 0.dp, modifier = Modifier.border(width = 1.dp, color = ZorvynSurface, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
        NavigationBarItem(icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("HOME", fontSize = 10.sp, fontWeight = FontWeight.Bold) }, selected = currentRoute == "home", onClick = onHomeClick, colors = NavigationBarItemDefaults.colors(selectedIconColor = premiumGold, selectedTextColor = premiumGold, indicatorColor = ZorvynBackground))
        NavigationBarItem(icon = { Icon(Icons.Default.List, contentDescription = "Txns") }, label = { Text("TXNS", fontSize = 10.sp, fontWeight = FontWeight.Medium) }, selected = currentRoute == "txns", onClick = onTxnsClick, colors = NavigationBarItemDefaults.colors(selectedIconColor = premiumGold, selectedTextColor = premiumGold, indicatorColor = ZorvynBackground))
        NavigationBarItem(icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = "Add") }, label = { Text("ADD", fontSize = 10.sp, fontWeight = FontWeight.Medium) }, selected = currentRoute == "add", onClick = onAddClick, colors = NavigationBarItemDefaults.colors(selectedIconColor = premiumGold, selectedTextColor = premiumGold, indicatorColor = ZorvynBackground))
        NavigationBarItem(icon = { Icon(Icons.Default.ShowChart, contentDescription = "Insights") }, label = { Text("INSIGHTS", fontSize = 10.sp, fontWeight = FontWeight.Medium) }, selected = currentRoute == "insights", onClick = onInsightsClick, colors = NavigationBarItemDefaults.colors(selectedIconColor = premiumGold, selectedTextColor = premiumGold, indicatorColor = ZorvynBackground))
        NavigationBarItem(icon = { Icon(Icons.Default.TrackChanges, contentDescription = "Score") }, label = { Text("SCORE", fontSize = 10.sp, fontWeight = FontWeight.Medium) }, selected = currentRoute == "score", onClick = onScoreNavClick, colors = NavigationBarItemDefaults.colors(selectedIconColor = premiumGold, selectedTextColor = premiumGold, indicatorColor = ZorvynBackground))
    }
}