package com.project.zorvynone.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.zorvynone.model.IconType
import com.project.zorvynone.model.Transaction
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: HomeViewModel, onNavigateHome: () -> Unit, onNavigateAdd: () -> Unit) {

    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredTransactions = transactions.filter { txn ->
        val matchesSearch = txn.title.contains(searchQuery, ignoreCase = true) ||
                txn.category.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Income" -> txn.isIncome
            "Expense" -> !txn.isIncome
            else -> true
        }
        matchesSearch && matchesFilter
    }

    val groupedTransactions = filteredTransactions.groupBy { txn ->
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(txn.date)).uppercase(Locale.ROOT)
    }

    // Summary stats
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val netFlow = totalIncome - totalExpense

    val premiumGold = Color(0xFFE5C158)

    Scaffold(
        containerColor = ZorvynBackground,
        bottomBar = {
            BottomNavBar(
                currentRoute = "txns",
                onHomeClick = onNavigateHome,
                onAddClick = onNavigateAdd
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── HERO HEADER ──
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Small muted label — acts as a "kicker" above the hero word
                    Text(
                        text = "EVERY RUPEE,",
                        color = premiumGold.copy(0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // Hero word — massive PlayfairDisplay Black
                    Text(
                        text = "Accounted.",
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = PlayfairDisplay,
                        letterSpacing = (-1.5).sp,
                        lineHeight = 46.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Gold fade rule
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(premiumGold, premiumGold.copy(0f))
                                ),
                                RoundedCornerShape(1.dp)
                            )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live data status line
                    val statusText = when {
                        transactions.isEmpty() -> "No transactions recorded yet."
                        netFlow >= 0 -> "${transactions.size} transactions · ₹${fmt.format(netFlow.toInt())} net positive"
                        else -> "${transactions.size} transactions · ₹${fmt.format((-netFlow).toInt())} net spent"
                    }
                    Text(
                        text = statusText,
                        color = TextSecondary.copy(0.55f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ── SUMMARY CARD ──
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .drawBehind {
                            if (size.width > 0f && size.height > 0f) drawRect(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E1036), Color(0xFF2D1B69).copy(0.6f)),
                                    Offset.Zero, Offset(size.width, size.height)
                                )
                            )
                        }
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Income
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ZorvynGreen.copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingUp, null, tint = ZorvynGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("₹${fmt.format(totalIncome)}", color = ZorvynGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Income", color = ZorvynGreen.copy(0.5f), fontSize = 11.sp)
                        }
                        // Net
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val netColor = if (netFlow >= 0) premiumGold else ZorvynRed
                            Box(
                                modifier = Modifier.size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(netColor.copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SwapVert, null, tint = netColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val netSign = if (netFlow >= 0) "+" else "-"
                            Text("${netSign}₹${fmt.format(kotlin.math.abs(netFlow))}", color = netColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Net Flow", color = netColor.copy(0.5f), fontSize = 11.sp)
                        }
                        // Expense
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ZorvynRed.copy(0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingDown, null, tint = ZorvynRed, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("₹${fmt.format(totalExpense)}", color = ZorvynRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Expense", color = ZorvynRed.copy(0.5f), fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── SEARCH BAR ──
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or category...", color = TextSecondary.copy(0.4f), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary.copy(0.5f)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary.copy(0.5f),
                                modifier = Modifier.size(20.dp).clickable { searchQuery = "" }
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = ZorvynSurface,
                        focusedContainerColor = ZorvynSurface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = premiumGold.copy(0.4f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = premiumGold
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── FILTER CHIPS ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Triple("All", Icons.Default.GridView, TextSecondary),
                        Triple("Income", Icons.Default.TrendingUp, ZorvynGreen),
                        Triple("Expense", Icons.Default.TrendingDown, ZorvynRed)
                    ).forEach { (label, icon, color) ->
                        val isSelected = selectedFilter == label
                        val chipBg = if (isSelected) color.copy(0.12f) else Color.Transparent
                        val chipBorder = if (isSelected) color.copy(0.3f) else TextSecondary.copy(0.1f)
                        val chipText = if (isSelected) color else TextSecondary.copy(0.5f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(14.dp))
                                .clickable { selectedFilter = label },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = chipText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label, color = chipText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── TRANSACTION COUNT ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filteredTransactions.size} transaction${if (filteredTransactions.size != 1) "s" else ""}",
                        color = TextSecondary.copy(0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            "Clear all",
                            color = premiumGold.copy(0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                searchQuery = ""
                                selectedFilter = "All"
                            }
                        )
                    }
                }
            }

            // ── EMPTY STATE OR GROUPED LIST ──
            if (filteredTransactions.isEmpty()) {
                item {
                    KineticEmptyState(modifier = Modifier.fillMaxWidth().height(400.dp))
                }
            } else {
                groupedTransactions.forEach { (monthYear, txns) ->
                    // Month header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(6.dp)
                                    .background(premiumGold.copy(0.5f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = monthYear,
                                color = TextSecondary.copy(0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            HorizontalDivider(color = TextSecondary.copy(0.06f), modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "${txns.size}",
                                color = TextSecondary.copy(0.3f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Transaction items with swipe-to-delete
                    items(txns, key = { it.id }) { txn ->
                        PremiumSwipeableItem(
                            txn = txn,
                            onDelete = { viewModel.deleteTransaction(txn) }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ── PREMIUM SWIPEABLE TRANSACTION ITEM ──────────────────────
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumSwipeableItem(txn: Transaction, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) ZorvynRed else Color.Transparent,
                label = "swipe_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .background(color, RoundedCornerShape(18.dp))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
            }
        },
        content = {
            Box(modifier = Modifier.background(ZorvynBackground)) {
                PremiumTransactionCard(txn = txn)
            }
        }
    )
}

@Composable
private fun PremiumTransactionCard(txn: Transaction) {
    val iconVec = when (txn.iconType) {
        IconType.WALLET -> Icons.Default.AccountBalanceWallet
        IconType.CAFE -> Icons.Default.LocalCafe
        IconType.SHOPPING -> Icons.Default.ShoppingCart
        IconType.FOOD -> Icons.Default.Restaurant
        IconType.HOUSING -> Icons.Default.Home
        IconType.TRANSPORT -> Icons.Default.DirectionsCar
        IconType.SALARY -> Icons.Default.Payments
        IconType.DEFAULT -> Icons.Default.Receipt
    }

    val iconColor = when (txn.iconType) {
        IconType.WALLET -> Color(0xFFA78BFA)
        IconType.CAFE -> Color(0xFFFBBF24)
        IconType.SHOPPING -> Color(0xFF60A5FA)
        IconType.FOOD -> Color(0xFFF472B6)
        IconType.HOUSING -> Color(0xFF5EEAD4)
        IconType.TRANSPORT -> Color(0xFF34D399)
        IconType.SALARY -> Color(0xFF4ADE80)
        IconType.DEFAULT -> Color(0xFF94A3B8)
    }

    val tint = if (txn.isIncome) ZorvynGreen else ZorvynRed
    val sign = if (txn.isIncome) "+" else "-"
    val formattedAmt = NumberFormat.getNumberInstance(Locale("en", "IN")).format(txn.amount)
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(txn.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ZorvynSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored icon badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(0.1f))
                    .border(1.dp, iconColor.copy(0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = iconVec, contentDescription = txn.title, tint = iconColor, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title + category + date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(4.dp).background(iconColor.copy(0.5f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = txn.category,
                        color = TextSecondary.copy(0.5f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("·", color = TextSecondary.copy(0.3f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateStr,
                        color = TextSecondary.copy(0.3f),
                        fontSize = 11.sp
                    )
                }
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign₹$formattedAmt",
                    color = tint,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = if (txn.isIncome) "credit" else "debit",
                    color = tint.copy(0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// ── KINETIC EMPTY STATE ─────────────────────────────────────
// ═══════════════════════════════════════════════════════════════

private val EmptyGold = Color(0xFFE5C158)
private val EmptyAmber = Color(0xFFD4A24E)

@Composable
private fun KineticEmptyState(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "kinetic")

    val levitationOffset by infiniteTransition.animateFloat(
        initialValue = -15f, targetValue = 15f,
        animationSpec = infiniteRepeatable(animation = tween(2400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "levitate"
    )
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing)),
        label = "outer_ring"
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(5000, easing = LinearEasing)),
        label = "inner_ring"
    )
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = LinearEasing)),
        label = "shimmer"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "glow"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = levitationOffset.dp)
        ) {
            Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val outerRadius = size.minDimension / 2f - 8f
                    val innerRadius = outerRadius - 22f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(EmptyGold.copy(alpha = glowPulse), EmptyAmber.copy(alpha = glowPulse * 0.4f), Color.Transparent),
                            center = Offset(cx, cy), radius = outerRadius * 1.3f
                        ),
                        radius = outerRadius * 1.3f, center = Offset(cx, cy)
                    )
                    rotate(degrees = outerRotation, pivot = Offset(cx, cy)) {
                        drawArc(color = EmptyGold.copy(alpha = 0.35f), startAngle = 0f, sweepAngle = 280f, useCenter = false, topLeft = Offset(cx - outerRadius, cy - outerRadius), size = Size(outerRadius * 2, outerRadius * 2), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
                    }
                    rotate(degrees = innerRotation, pivot = Offset(cx, cy)) {
                        val dashCount = 24; val dashSweep = 8f; val gapSweep = (360f / dashCount) - dashSweep
                        for (i in 0 until dashCount) {
                            drawArc(color = EmptyAmber.copy(alpha = 0.2f), startAngle = i * (dashSweep + gapSweep), sweepAngle = dashSweep, useCenter = false, topLeft = Offset(cx - innerRadius, cy - innerRadius), size = Size(innerRadius * 2, innerRadius * 2), style = Stroke(width = 1.5f, cap = StrokeCap.Round))
                        }
                    }
                }
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add transaction", tint = EmptyGold.copy(alpha = 0.8f), modifier = Modifier.size(52.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Initiate your first sequence.",
                style = TextStyle(
                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    brush = Brush.linearGradient(
                        colors = listOf(EmptyGold.copy(alpha = 0.4f), EmptyGold, Color.White, EmptyGold, EmptyGold.copy(alpha = 0.4f)),
                        start = Offset(shimmerOffset * 600f, 0f), end = Offset(shimmerOffset * 600f + 300f, 0f)
                    ),
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tap the Add button below to begin\nmapping your wealth intelligence.",
                color = TextSecondary.copy(alpha = 0.5f), fontSize = 13.sp,
                textAlign = TextAlign.Center, lineHeight = 20.sp
            )
        }
    }
}
