package com.project.zorvynone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = TextSecondary)
                }
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


