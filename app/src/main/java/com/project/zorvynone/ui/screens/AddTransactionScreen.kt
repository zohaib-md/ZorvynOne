package com.project.zorvynone.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.project.zorvynone.R
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import android.content.Intent
import android.speech.RecognizerIntent
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
    val premiumGold = Color(0xFFE5C158)

    // --- Receipt Scanner ---
    val isScannerLoading by viewModel.isScannerLoading.collectAsStateWithLifecycle()
    val isVoiceLoading by viewModel.isVoiceLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text
                        if (extractedText.isNotBlank()) {
                            viewModel.processReceiptText(extractedText)
                            onNavigateBack()
                        }
                    }
                    .addOnFailureListener { e -> e.printStackTrace() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Voice AI ---
    val speechLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.processVoiceTransaction(spokenText)
                onNavigateBack()
            }
        }
    }

    Scaffold(
        containerColor = ZorvynBackground,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ── Premium header ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ZorvynSurface, CircleShape)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Add Transaction",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlayfairDisplay,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(premiumGold, premiumGold.copy(0f))
                                ),
                                RoundedCornerShape(1.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Log a payment, income or scan a receipt.",
                        color = TextSecondary.copy(0.55f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── AI INPUT TOOLS (Split Pill) ──
            val scannerLottie by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.document_ocr_scan))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ZorvynSurface),
                border = BorderStroke(1.dp, premiumGold.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    // ─── Left: Receipt Scanner ───
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !isScannerLoading) {
                                photoPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                            }
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(premiumGold.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isScannerLoading) {
                                CircularProgressIndicator(color = premiumGold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                LottieAnimation(
                                    composition = scannerLottie,
                                    iterations = LottieConstants.IterateForever,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Scan Receipt",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (isScannerLoading) "Scanning..." else "Photo → AI fill",
                            color = TextSecondary.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }

                    // ─── Divider ───
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .padding(vertical = 20.dp)
                            .background(premiumGold.copy(alpha = 0.12f))
                    )

                    // ─── Right: Voice AI ───
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !isVoiceLoading) {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your expense e.g. '200 on coffee'")
                                }
                                speechLauncher.launch(intent)
                            }
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(premiumGold.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isVoiceLoading) {
                                CircularProgressIndicator(color = premiumGold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                val recordingLottie by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.recording))
                                LottieAnimation(
                                    composition = recordingLottie,
                                    iterations = LottieConstants.IterateForever,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Voice AI",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (isVoiceLoading) "Processing..." else "Speak → AI fill",
                            color = TextSecondary.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Income / Expense Toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Expense button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .drawBehind {
                            if (size.width > 0f && size.height > 0f) {
                                if (!isIncome) {
                                    drawRect(Brush.linearGradient(
                                        listOf(Color(0xFF7F1D1D), Color(0xFFEF4444), Color(0xFFFF6B6B)),
                                        Offset.Zero, Offset(size.width, size.height)
                                    ))
                                } else {
                                    drawRect(androidx.compose.ui.graphics.SolidColor(ZorvynSurface))
                                }
                            }
                        }
                        .clickable { isIncome = false },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (!isIncome) Color.White else TextSecondary.copy(0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Expense",
                            color = if (!isIncome) Color.White else TextSecondary.copy(0.5f),
                            fontWeight = if (!isIncome) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
                // Income button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .drawBehind {
                            if (size.width > 0f && size.height > 0f) {
                                if (isIncome) {
                                    drawRect(Brush.linearGradient(
                                        listOf(Color(0xFF14532D), Color(0xFF22C55E), Color(0xFF4ADE80)),
                                        Offset.Zero, Offset(size.width, size.height)
                                    ))
                                } else {
                                    drawRect(androidx.compose.ui.graphics.SolidColor(ZorvynSurface))
                                }
                            }
                        }
                        .clickable { isIncome = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isIncome) Color.White else TextSecondary.copy(0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Income",
                            color = if (isIncome) Color.White else TextSecondary.copy(0.5f),
                            fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
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

            // Each category: Triple(name, fallbackIcon, lottieResOrNull)
            data class CategoryDef(val name: String, val icon: ImageVector, val lottieRes: Int? = null)

            val categories = if (isIncome) {
                listOf(
                    CategoryDef("Salary", Icons.Default.Payments, R.raw.money_wallet),
                    CategoryDef("Freelance", Icons.Default.Computer, R.raw.freelancers_life),
                    CategoryDef("Gift", Icons.Default.CardGiftcard, R.raw.angpao_animation)
                )
            } else {
                listOf(
                    CategoryDef("Food", Icons.Default.Restaurant, R.raw.food_carousel),
                    CategoryDef("Café", Icons.Default.LocalCafe, R.raw.cafe),
                    CategoryDef("Shopping", Icons.Default.ShoppingCart, R.raw.shopping_cart),
                    CategoryDef("Housing", Icons.Default.Home, R.raw.property_market),
                    CategoryDef("Transport", Icons.Default.DirectionsCar, R.raw.girl_on_scooter),
                    CategoryDef("Bills", Icons.Default.Receipt, R.raw.receipt)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categories.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { cat ->
                            CategoryItem(
                                name = cat.name, icon = cat.icon, lottieRes = cat.lottieRes,
                                isSelected = selectedCategory == cat.name,
                                onClick = { selectedCategory = cat.name }, modifier = Modifier.weight(1f)
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
fun CategoryItem(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lottieRes: Int? = null
) {
    val backgroundColor = if (isSelected) ZorvynBlueAction.copy(alpha = 0.2f) else ZorvynSurface
    val contentColor = if (isSelected) ZorvynBlueText else TextSecondary
    val borderColor = if (isSelected) ZorvynBlueText else Color.Transparent

    Box(
        modifier = modifier
            .height(88.dp)
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (lottieRes != null) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRes))
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Icon(imageVector = icon, contentDescription = name, tint = contentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = name, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}