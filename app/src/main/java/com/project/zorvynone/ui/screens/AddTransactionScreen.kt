package com.project.zorvynone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Food") }
    var note by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val selectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()

    val currentBalance by viewModel.totalBalance.collectAsState()
    val formattedBalance = NumberFormat.getNumberInstance(Locale("en", "IN")).format(currentBalance)
    val goldColor = Color(0xFFFFD700)

    Scaffold(
        containerColor = ZorvynBackground,
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ZorvynBackground)
            )
        }
        // Removed the bottomBar to put the button in the scrollable content
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Income / Expense Toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilterChip(
                    selected = !isIncome,
                    onClick = { isIncome = false },
                    label = { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Expense")
                    }},
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ZorvynRed.copy(alpha = 0.15f),
                        selectedLabelColor = ZorvynRed,
                        selectedLeadingIconColor = ZorvynRed,
                        containerColor = ZorvynSurface,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = null
                )
                FilterChip(
                    selected = isIncome,
                    onClick = { isIncome = true },
                    label = { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Income")
                    }},
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ZorvynGreen.copy(alpha = 0.15f),
                        selectedLabelColor = ZorvynGreen,
                        selectedLeadingIconColor = ZorvynGreen,
                        containerColor = ZorvynSurface,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = null
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Big Amount Input Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZorvynSurface, RoundedCornerShape(24.dp))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ENTER AMOUNT", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹", color = TextPrimary, fontSize = 48.sp, fontWeight = FontWeight.Bold)

                        BasicTextField(
                            value = amountInput,
                            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 8) amountInput = it },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 48.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            cursorBrush = SolidColor(ZorvynRed),
                            modifier = Modifier.width(IntrinsicSize.Min)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Available: ₹$formattedBalance", color = TextSecondary, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category Selection Grid
            Text("CATEGORY", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            val categories = if (isIncome) {
                listOf("Salary" to Icons.Default.Payments, "Freelance" to Icons.Default.Computer, "Gift" to Icons.Default.CardGiftcard)
            } else {
                listOf(
                    "Food" to Icons.Default.Restaurant, "Café" to Icons.Default.LocalCafe, "Shopping" to Icons.Default.ShoppingCart,
                    "Housing" to Icons.Default.Home, "Transport" to Icons.Default.DirectionsCar, "Bills" to Icons.Default.Receipt
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categories.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { (name, icon) ->
                            CategoryItem(
                                name = name, icon = icon, isSelected = selectedCategory == name,
                                onClick = { selectedCategory = name }, modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details Section
            Text("DETAILS", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(selectedDateMillis)),
                onValueChange = {}, readOnly = true,
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = ZorvynSurface, focusedContainerColor = ZorvynSurface,
                    unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                enabled = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                placeholder = { Text("Add a note (optional)...", color = TextSecondary.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = ZorvynSurface, focusedContainerColor = ZorvynSurface,
                    unfocusedBorderColor = Color.Transparent, focusedBorderColor = ZorvynBlueText,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // New Golden Save Button inside the layout
            Button(
                onClick = {
                    val finalAmount = amountInput.replace(",", "").toIntOrNull() ?: 0
                    if (finalAmount > 0) {
                        viewModel.addTransaction(
                            amount = finalAmount, isIncome = isIncome, category = selectedCategory,
                            dateMillis = selectedDateMillis, note = note
                        )
                        onNavigateBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = ZorvynBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Transaction", color = ZorvynBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp)) // Padding for bottom nav
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK", color = ZorvynBlueText) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = TextSecondary) } },
            colors = DatePickerDefaults.colors(containerColor = ZorvynSurface)
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun CategoryItem(name: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = if (isSelected) ZorvynBlueAction.copy(alpha = 0.2f) else ZorvynSurface
    val contentColor = if (isSelected) ZorvynBlueText else TextSecondary
    val borderColor = if (isSelected) ZorvynBlueText else Color.Transparent

    Box(
        modifier = modifier
            .height(80.dp)
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = name, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = name, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}