package com.project.zorvynone.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
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

private val premiumGold = Color(0xFFE5C158)

@Composable
fun SpendOrSkipScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
    val expenses = remember(transactions) {
        transactions.filter { !it.isIncome && it.date >= thirtyDaysAgo }
    }

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
            Spacer(modifier = Modifier.height(20.dp))

            // ── Header ────────────────────────────────────────────────────────
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
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = TextSecondary, modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "SPEND OR SKIP",
                        color = premiumGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "Was it worth it?",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── States ────────────────────────────────────────────────────────
            if (expenses.isEmpty()) {
                EmptySpendState()
            } else if (showReport || isFinished) {
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
                // ── Progress dots ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${currentIndex + 1} of ${expenses.size}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // regret pill
                        Box(
                            modifier = Modifier
                                .background(ZorvynRed.copy(0.12f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Close, null, tint = ZorvynRed, modifier = Modifier.size(12.dp))
                                Text("${regretList.size}", color = ZorvynRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        // worth it pill
                        Box(
                            modifier = Modifier
                                .background(ZorvynGreen.copy(0.12f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Check, null, tint = ZorvynGreen, modifier = Modifier.size(12.dp))
                                Text("${worthItList.size}", color = ZorvynGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Segmented progress bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(expenses.size) { i ->
                        val color = when {
                            i < currentIndex && i < regretList.size + worthItList.size -> {
                                val allDecided = (regretList + worthItList).sortedBy { expenses.indexOf(it) }
                                if (i < allDecided.size && regretList.contains(allDecided[i])) ZorvynRed
                                else ZorvynGreen
                            }
                            i == currentIndex -> premiumGold
                            else -> ZorvynSurface
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Swipe direction hints
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.KeyboardArrowLeft, null, tint = ZorvynRed.copy(0.6f), modifier = Modifier.size(20.dp))
                        Text("Regret", color = ZorvynRed.copy(0.6f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Worth it", color = ZorvynGreen.copy(0.6f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = ZorvynGreen.copy(0.6f), modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Card stack ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Back card peek
                    if (currentIndex + 1 < expenses.size) {
                        ExpenseSwipeCard(
                            transaction = expenses[currentIndex + 1],
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .scale(0.93f)
                                .graphicsLayer(alpha = 0.35f)
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

                Spacer(modifier = Modifier.height(28.dp))

                // ── Action buttons ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Regret button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .drawBehind {
                                    if (size.width > 0f) drawCircle(
                                        Brush.radialGradient(
                                            listOf(Color(0xFF7F1D1D), Color(0xFFEF4444)),
                                            Offset(size.width / 2, size.height / 2), size.width / 2
                                        )
                                    )
                                }
                                .clickable {
                                    regretList.add(expenses[currentIndex])
                                    currentIndex++
                                    if (currentIndex >= expenses.size) showReport = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Regret", color = ZorvynRed.copy(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Skip button (center)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(ZorvynSurface, CircleShape)
                            .border(1.dp, TextSecondary.copy(0.15f), CircleShape)
                            .clickable {
                                currentIndex++
                                if (currentIndex >= expenses.size) showReport = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SkipNext, null, tint = TextSecondary.copy(0.5f), modifier = Modifier.size(20.dp))
                    }

                    // Worth it button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .drawBehind {
                                    if (size.width > 0f) drawCircle(
                                        Brush.radialGradient(
                                            listOf(Color(0xFF14532D), Color(0xFF22C55E)),
                                            Offset(size.width / 2, size.height / 2), size.width / 2
                                        )
                                    )
                                }
                                .clickable {
                                    worthItList.add(expenses[currentIndex])
                                    currentIndex++
                                    if (currentIndex >= expenses.size) showReport = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Worth it", color = ZorvynGreen.copy(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

// ── Swipeable card wrapper ─────────────────────────────────────────────────────

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
    val tintAlpha = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 0.5f)
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
                                else -> offsetX.animateTo(0f, spring(dampingRatio = 0.6f))
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        coroutineScope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
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
            overlayLabel = if (tintAlpha > 0.12f) {
                if (isRight) "WORTH IT ✓" else "REGRET ✗"
            } else null,
            overlayLabelColor = if (isRight) ZorvynGreen else ZorvynRed
        )
    }
}

// ── Expense card ───────────────────────────────────────────────────────────────

@Composable
private fun ExpenseSwipeCard(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    overlayColor: Color? = null,
    overlayLabel: String? = null,
    overlayLabelColor: Color = Color.White
) {
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.date))

    val (icon, categoryColor) = when (transaction.iconType) {
        IconType.WALLET -> Pair(Icons.Default.AccountBalanceWallet, Color(0xFF6366F1))
        IconType.CAFE   -> Pair(Icons.Default.LocalCafe, Color(0xFFF59E0B))
        IconType.SHOPPING -> Pair(Icons.Default.ShoppingCart, Color(0xFFEC4899))
        IconType.FOOD   -> Pair(Icons.Default.Restaurant, Color(0xFFEF4444))
        IconType.HOUSING -> Pair(Icons.Default.Home, Color(0xFF8B5CF6))
        IconType.TRANSPORT -> Pair(Icons.Default.DirectionsCar, Color(0xFF3B82F6))
        IconType.SALARY -> Pair(Icons.Default.Payments, Color(0xFF10B981))
        IconType.DEFAULT -> Pair(Icons.Default.Receipt, Color(0xFF6B7280))
    }

    Box(modifier = modifier) {
        // Card background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF13161F))
                .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(28.dp))
        ) {
            Column {
                // ── Gradient top section ──────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .drawBehind {
                            if (size.width > 0f && size.height > 0f) {
                                drawRect(
                                    Brush.linearGradient(
                                        listOf(
                                            categoryColor.copy(0.25f),
                                            categoryColor.copy(0.08f),
                                            Color.Transparent
                                        ),
                                        Offset.Zero,
                                        Offset(size.width, size.height)
                                    )
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Glowing icon ring
                        Box(contentAlignment = Alignment.Center) {
                            // Outer glow
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(categoryColor.copy(0.2f), Color.Transparent)
                                        ),
                                        CircleShape
                                    )
                            )
                            // Icon container
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(categoryColor.copy(0.18f))
                                    .border(1.5.dp, categoryColor.copy(0.35f), RoundedCornerShape(22.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = categoryColor, modifier = Modifier.size(34.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category label
                        Box(
                            modifier = Modifier
                                .background(categoryColor.copy(0.15f), RoundedCornerShape(20.dp))
                                .border(1.dp, categoryColor.copy(0.25f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                transaction.category.replaceFirstChar { it.uppercase() },
                                color = categoryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }

                // ── Bottom info section ───────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Amount — big hero text
                    Text(
                        "₹${fmt.format(transaction.amount)}",
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.5).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Title
                    Text(
                        transaction.title,
                        color = Color.White.copy(0.85f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday, null,
                            tint = TextSecondary.copy(0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(dateStr, color = TextSecondary.copy(0.5f), fontSize = 12.sp)
                    }

                    if (!transaction.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(0.04f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                "\"${transaction.note}\"",
                                color = TextSecondary.copy(0.7f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // ── Swipe overlay ─────────────────────────────────────────────────
        if (overlayColor != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(overlayColor)
            )
        }
        if (overlayLabel != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .border(2.5.dp, overlayLabelColor, RoundedCornerShape(10.dp))
                    .background(overlayLabelColor.copy(0.1f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    overlayLabel,
                    color = overlayLabelColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

// ── Empty state ─────────────────────────────────────────────────────────────────

@Composable
private fun EmptySpendState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(ZorvynSurface, RoundedCornerShape(26.dp))
                    .border(1.dp, TextSecondary.copy(0.1f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Receipt, null, tint = TextSecondary.copy(0.3f), modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("No expenses to review", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Add some transactions first!", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

// ── Regret report ───────────────────────────────────────────────────────────────

@Composable
private fun RegretReport(
    worthItList: List<Transaction>,
    regretList: List<Transaction>,
    onPlayAgain: () -> Unit,
    onDone: () -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))
    val total = worthItList.sumOf { it.amount } + regretList.sumOf { it.amount }
    val regretAmount = regretList.sumOf { it.amount }
    val worthItAmount = worthItList.sumOf { it.amount }
    val regretPercent = if (total > 0) ((regretAmount.toFloat() / total) * 100).toInt() else 0

    val animatedRegretPercent by animateIntAsState(
        targetValue = regretPercent,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "regret_anim"
    )

    val scoreColor = when {
        regretPercent >= 60 -> ZorvynRed
        regretPercent >= 30 -> premiumGold
        else -> ZorvynGreen
    }
    val verdict = when {
        regretPercent >= 60 -> "Spending regrets 😬"
        regretPercent >= 30 -> "Mixed feelings 🤔"
        else -> "Great spending! 🎉"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Label
        Text(
            "YOUR REGRET REPORT",
            color = premiumGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Score circle ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(180.dp)
                .drawBehind {
                    // outer glow ring
                    drawCircle(
                        Brush.radialGradient(
                            listOf(scoreColor.copy(0.15f), Color.Transparent),
                            Offset(size.width / 2, size.height / 2), size.width / 2
                        )
                    )
                    // border arc
                    drawCircle(
                        color = scoreColor.copy(0.35f),
                        style = Stroke(width = 2.5f)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${animatedRegretPercent}%",
                    color = scoreColor,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp
                )
                Text("regret", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(verdict, color = TextSecondary.copy(0.7f), fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Stats row ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Regret card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .drawBehind {
                        if (size.width > 0f) drawRect(
                            Brush.linearGradient(
                                listOf(Color(0xFF3B0A0A), ZorvynRed.copy(0.08f)),
                                Offset.Zero, Offset(size.width, size.height)
                            )
                        )
                    }
                    .border(1.dp, ZorvynRed.copy(0.2f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ZorvynRed.copy(0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = ZorvynRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("₹${fmt.format(regretAmount)}", color = ZorvynRed, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("${regretList.size} items", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("REGRETTED", color = ZorvynRed.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            // Worth it card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .drawBehind {
                        if (size.width > 0f) drawRect(
                            Brush.linearGradient(
                                listOf(Color(0xFF052E16), ZorvynGreen.copy(0.08f)),
                                Offset.Zero, Offset(size.width, size.height)
                            )
                        )
                    }
                    .border(1.dp, ZorvynGreen.copy(0.2f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ZorvynGreen.copy(0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, null, tint = ZorvynGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("₹${fmt.format(worthItAmount)}", color = ZorvynGreen, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("${worthItList.size} items", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("WORTH IT", color = ZorvynGreen.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        // ── Top regrets ─────────────────────────────────────────────────
        if (regretList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ZorvynSurface)
                    .border(1.dp, ZorvynRed.copy(0.12f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.TrendingDown, null, tint = ZorvynRed.copy(0.7f), modifier = Modifier.size(16.dp))
                        Text("TOP REGRETS", color = ZorvynRed.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    regretList.sortedByDescending { it.amount }.take(3).forEachIndexed { i, txn ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${i + 1}",
                                    color = TextSecondary.copy(0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(txn.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("-₹${fmt.format(txn.amount)}", color = ZorvynRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        if (i < minOf(regretList.size, 3) - 1) {
                            HorizontalDivider(color = TextSecondary.copy(0.06f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Action buttons ───────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onPlayAgain,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, premiumGold.copy(0.3f))
            ) {
                Icon(Icons.Default.Refresh, null, tint = premiumGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Again", color = premiumGold, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onDone,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = premiumGold)
            ) {
                Text("Done", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
