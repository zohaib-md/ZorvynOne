package com.project.zorvynone.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.zorvynone.model.IconType
import com.project.zorvynone.model.Transaction
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SpendOrSkipScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val premiumGold = Color(0xFFE5C158)
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    // Filter expenses only — last 30 days
    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
    val expenses = remember(transactions) {
        transactions.filter { !it.isIncome && it.date >= thirtyDaysAgo }
    }

    // Swipe state
    var currentIndex by remember { mutableIntStateOf(0) }
    val worthItList = remember { mutableStateListOf<Transaction>() }
    val regretList = remember { mutableStateListOf<Transaction>() }
    var showReport by remember { mutableStateOf(false) }

    val isFinished = currentIndex >= expenses.size || expenses.isEmpty()

    Scaffold(containerColor = ZorvynBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(ZorvynSurface, CircleShape)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("SPEND OR SKIP", color = premiumGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Review your spending", color = TextSecondary, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (expenses.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, null, tint = TextSecondary.copy(0.3f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No expenses to review", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add some transactions first!", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else if (showReport || isFinished) {
                // Regret Report
                RegretReport(
                    worthItList = worthItList,
                    regretList = regretList,
                    onPlayAgain = {
                        currentIndex = 0
                        worthItList.clear()
                        regretList.clear()
                        showReport = false
                    },
                    onDone = onNavigateBack
                )
            } else {
                // Progress
                val progress = currentIndex.toFloat() / expenses.size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${currentIndex + 1} / ${expenses.size}",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${regretList.size}", color = ZorvynRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(" regrets", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${worthItList.size}", color = ZorvynGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(" worth it", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = premiumGold,
                    trackColor = ZorvynSurface
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Swipe hints
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("← Regret", color = ZorvynRed.copy(0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Worth it →", color = ZorvynGreen.copy(0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card stack
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Next card peek (behind)
                    if (currentIndex + 1 < expenses.size) {
                        ExpenseSwipeCard(
                            transaction = expenses[currentIndex + 1],
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .scale(0.92f)
                                .graphicsLayer(alpha = 0.4f)
                        )
                    }

                    // Current card
                    SwipeableCard(
                        transaction = expenses[currentIndex],
                        onSwipeLeft = {
                            regretList.add(expenses[currentIndex])
                            currentIndex++
                            if (currentIndex >= expenses.size) showReport = true
                        },
                        onSwipeRight = {
                            worthItList.add(expenses[currentIndex])
                            currentIndex++
                            if (currentIndex >= expenses.size) showReport = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Regret button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(ZorvynRed.copy(0.1f), CircleShape)
                            .border(1.dp, ZorvynRed.copy(0.3f), CircleShape)
                            .clickable {
                                regretList.add(expenses[currentIndex])
                                currentIndex++
                                if (currentIndex >= expenses.size) showReport = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = ZorvynRed, modifier = Modifier.size(28.dp))
                    }

                    // Worth it button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(ZorvynGreen.copy(0.1f), CircleShape)
                            .border(1.dp, ZorvynGreen.copy(0.3f), CircleShape)
                            .clickable {
                                worthItList.add(expenses[currentIndex])
                                currentIndex++
                                if (currentIndex >= expenses.size) showReport = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = ZorvynGreen, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SwipeableCard(
    transaction: Transaction,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val swipeThreshold = 300f

    val rotation = (offsetX.value / 30f).coerceIn(-15f, 15f)
    val tintAlpha = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 0.4f)
    val isRight = offsetX.value > 0

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer(rotationZ = rotation)
            .pointerInput(transaction.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            when {
                                offsetX.value > swipeThreshold -> {
                                    offsetX.animateTo(1500f, tween(300))
                                    onSwipeRight()
                                    offsetX.snapTo(0f)
                                }
                                offsetX.value < -swipeThreshold -> {
                                    offsetX.animateTo(-1500f, tween(300))
                                    onSwipeLeft()
                                    offsetX.snapTo(0f)
                                }
                                else -> {
                                    offsetX.animateTo(0f, spring(dampingRatio = 0.6f))
                                }
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                        }
                    }
                )
            }
    ) {
        ExpenseSwipeCard(
            transaction = transaction,
            modifier = Modifier.fillMaxWidth(0.95f),
            overlayColor = if (tintAlpha > 0.05f) {
                if (isRight) ZorvynGreen.copy(alpha = tintAlpha) else ZorvynRed.copy(alpha = tintAlpha)
            } else null,
            overlayLabel = if (tintAlpha > 0.1f) {
                if (isRight) "WORTH IT" else "REGRET"
            } else null,
            overlayLabelColor = if (isRight) ZorvynGreen else ZorvynRed
        )
    }
}

@Composable
private fun ExpenseSwipeCard(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    overlayColor: Color? = null,
    overlayLabel: String? = null,
    overlayLabelColor: Color = Color.White
) {
    val premiumGold = Color(0xFFE5C158)
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(transaction.date))

    val icon = when (transaction.iconType) {
        IconType.WALLET -> Icons.Default.AccountBalanceWallet
        IconType.CAFE -> Icons.Default.LocalCafe
        IconType.SHOPPING -> Icons.Default.ShoppingCart
        IconType.FOOD -> Icons.Default.Restaurant
        IconType.HOUSING -> Icons.Default.Home
        IconType.TRANSPORT -> Icons.Default.DirectionsCar
        IconType.SALARY -> Icons.Default.Payments
        IconType.DEFAULT -> Icons.Default.Receipt
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f))
    ) {
        Box {
            // Overlay tint on drag
            if (overlayColor != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(overlayColor, RoundedCornerShape(24.dp))
                )
            }

            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Swipe label
                if (overlayLabel != null) {
                    Box(
                        modifier = Modifier
                            .border(2.dp, overlayLabelColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            overlayLabel,
                            color = overlayLabelColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Category icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White.copy(0.06f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Amount
                Text(
                    "₹${fmt.format(transaction.amount)}",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    transaction.title,
                    color = Color.White.copy(0.9f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Category + date
                Text(
                    transaction.category.replaceFirstChar { it.uppercase() },
                    color = premiumGold.copy(0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    dateStr,
                    color = TextSecondary.copy(0.5f),
                    fontSize = 12.sp
                )

                if (!transaction.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "\"${transaction.note}\"",
                        color = TextSecondary.copy(0.6f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun RegretReport(
    worthItList: List<Transaction>,
    regretList: List<Transaction>,
    onPlayAgain: () -> Unit,
    onDone: () -> Unit
) {
    val premiumGold = Color(0xFFE5C158)
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))
    val total = worthItList.sumOf { it.amount } + regretList.sumOf { it.amount }
    val regretAmount = regretList.sumOf { it.amount }
    val worthItAmount = worthItList.sumOf { it.amount }
    val regretPercent = if (total > 0) ((regretAmount.toFloat() / total) * 100).toInt() else 0
    val worthItPercent = 100 - regretPercent

    // Animated score
    val animatedRegretPercent by animateIntAsState(
        targetValue = regretPercent,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "regret_anim"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero stat
        Text("YOUR REGRET REPORT", color = premiumGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

        Spacer(modifier = Modifier.height(32.dp))

        // Regret score circle
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ZorvynRed.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .border(
                    3.dp,
                    Brush.linearGradient(
                        listOf(ZorvynRed.copy(0.6f), premiumGold.copy(0.3f))
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${animatedRegretPercent}%",
                    color = if (regretPercent > 50) ZorvynRed else premiumGold,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black
                )
                Text("regret", color = TextSecondary, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Stats cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Regret card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ZorvynRed.copy(0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZorvynRed.copy(0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Close, null, tint = ZorvynRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("₹${fmt.format(regretAmount)}", color = ZorvynRed, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${regretList.size} items", color = TextSecondary, fontSize = 12.sp)
                    Text("Regretted", color = ZorvynRed.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Worth it card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ZorvynGreen.copy(0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZorvynGreen.copy(0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Check, null, tint = ZorvynGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("₹${fmt.format(worthItAmount)}", color = ZorvynGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${worthItList.size} items", color = TextSecondary, fontSize = 12.sp)
                    Text("Worth it", color = ZorvynGreen.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Top regrets
        if (regretList.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ZorvynSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary.copy(0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TOP REGRETS", color = ZorvynRed.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    regretList.sortedByDescending { it.amount }.take(3).forEach { txn ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(txn.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text("-₹${fmt.format(txn.amount)}", color = ZorvynRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onPlayAgain,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, premiumGold.copy(0.3f))
            ) {
                Icon(Icons.Default.Refresh, null, tint = premiumGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Again", color = premiumGold, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onDone,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = premiumGold)
            ) {
                Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
