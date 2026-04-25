package com.project.zorvynone.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.zorvynone.model.IconType
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Income, Expense

    // 1. Filter the list based on Search + Chips
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

    // 2. Group the filtered list by Month and Year (e.g., "APRIL 2025")
    val groupedTransactions = filteredTransactions.groupBy { txn ->
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(txn.date)).uppercase(Locale.ROOT)
    }

    Scaffold(
        containerColor = ZorvynBackground,
        topBar = {
            TopAppBar(
                title = { Text("Transactions", color = TextPrimary, fontWeight = FontWeight.Bold) },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(ZorvynSurface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Filter", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ZorvynBackground)
            )
        },
        bottomBar = {
            // Reusing the Bottom Nav, highlighting TXNS
            BottomNavBar(
                currentRoute = "txns",
                onHomeClick = onNavigateHome,
                onAddClick = onNavigateAdd
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search transactions...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = ZorvynSurface,
                    focusedContainerColor = ZorvynSurface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = ZorvynBlueText,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("All", "Income", "Expense").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, modifier = Modifier.padding(horizontal = 8.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ZorvynSurface,
                            selectedLabelColor = TextPrimary,
                            containerColor = ZorvynBackground,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) ZorvynBlueText.copy(alpha = 0.5f) else ZorvynSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // The List itself
            if (filteredTransactions.isEmpty()) {
                KineticEmptyState(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for bottom nav
                ) {
                    groupedTransactions.forEach { (monthYear, txns) ->
                        // Month Header
                        item {
                            Text(
                                text = monthYear,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                            )
                        }

                        // Transactions for that month
                        items(txns, key = { it.id }) { txn ->
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

                            // Using the same TransactionItem look from HomeScreen
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(tint.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = iconVec, contentDescription = txn.title, tint = tint, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = txn.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text(text = txn.subtitle, color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                                Text(
                                    text = "$sign₹$formattedAmt",
                                    color = tint,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// A slightly modified BottomNavBar that highlights the correct tab


// --- KINETIC EMPTY STATE ---

private val EmptyGold = Color(0xFFE5C158)
private val EmptyAmber = Color(0xFFD4A24E)

@Composable
private fun KineticEmptyState(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "kinetic")

    // 1. Levitation
    val levitationOffset by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levitate"
    )

    // 2. Outer ring rotation (clockwise)
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "outer_ring"
    )

    // 3. Inner ring rotation (counter-clockwise, faster)
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing)
        ),
        label = "inner_ring"
    )

    // 4. Shimmer offset for text
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ),
        label = "shimmer"
    )

    // 5. Pulse for ambient glow
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = levitationOffset.dp)
        ) {
            // --- ORBITAL CANVAS ---
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val outerRadius = size.minDimension / 2f - 8f
                    val innerRadius = outerRadius - 22f

                    // Ambient glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                EmptyGold.copy(alpha = glowPulse),
                                EmptyAmber.copy(alpha = glowPulse * 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = outerRadius * 1.3f
                        ),
                        radius = outerRadius * 1.3f,
                        center = Offset(cx, cy)
                    )

                    // Outer orbital ring (280° broken arc, rotating clockwise)
                    rotate(degrees = outerRotation, pivot = Offset(cx, cy)) {
                        drawArc(
                            color = EmptyGold.copy(alpha = 0.35f),
                            startAngle = 0f,
                            sweepAngle = 280f,
                            useCenter = false,
                            topLeft = Offset(cx - outerRadius, cy - outerRadius),
                            size = Size(outerRadius * 2, outerRadius * 2),
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                        )
                    }

                    // Inner dashed ring (counter-clockwise)
                    rotate(degrees = innerRotation, pivot = Offset(cx, cy)) {
                        val dashCount = 24
                        val dashSweep = 8f
                        val gapSweep = (360f / dashCount) - dashSweep
                        for (i in 0 until dashCount) {
                            val startAngle = i * (dashSweep + gapSweep)
                            drawArc(
                                color = EmptyAmber.copy(alpha = 0.2f),
                                startAngle = startAngle,
                                sweepAngle = dashSweep,
                                useCenter = false,
                                topLeft = Offset(cx - innerRadius, cy - innerRadius),
                                size = Size(innerRadius * 2, innerRadius * 2),
                                style = Stroke(width = 1.5f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                // Central Add icon
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add transaction",
                    tint = EmptyGold.copy(alpha = 0.8f),
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- SHIMMER TEXT ---
            Text(
                text = "Initiate your first sequence.",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            EmptyGold.copy(alpha = 0.4f),
                            EmptyGold,
                            Color.White,
                            EmptyGold,
                            EmptyGold.copy(alpha = 0.4f)
                        ),
                        start = Offset(shimmerOffset * 600f, 0f),
                        end = Offset(shimmerOffset * 600f + 300f, 0f)
                    ),
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tap the Add button below to begin\nmapping your wealth intelligence.",
                color = TextSecondary.copy(alpha = 0.5f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
