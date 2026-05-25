package com.project.zorvynone.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.HomeViewModel
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

data class SplitPerson(
    val id: Int,
    val name: String,
    val customAmount: String = "",
    val percentage: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val premiumGold = Color(0xFFE5C158)
    val indigo = Color(0xFF6366F1)
    val context = LocalContext.current
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))

    var billAmount by remember { mutableStateOf("") }
    var tipPercent by remember { mutableStateOf("0") }
    var splitMode by remember { mutableStateOf("equal") }
    var people by remember {
        mutableStateOf(
            listOf(
                SplitPerson(1, "You"),
                SplitPerson(2, "")
            )
        )
    }
    var nextId by remember { mutableIntStateOf(3) }

    // AI Smart Split
    var showSmartSplitSheet by remember { mutableStateOf(false) }
    var smartSplitInput by remember { mutableStateOf("") }

    // Share card
    var showShareSheet by remember { mutableStateOf(false) }
    var upiId by remember { mutableStateOf("") }
    val isSmartSplitLoading by viewModel.isSmartSplitLoading.collectAsStateWithLifecycle()
    val smartSplitResult by viewModel.smartSplitResult.collectAsStateWithLifecycle()

    // Process AI result
    LaunchedEffect(smartSplitResult) {
        smartSplitResult?.let { jsonStr ->
            try {
                val json = JSONObject(jsonStr)
                val arr = json.getJSONArray("people")
                val newPeople = mutableListOf<SplitPerson>()
                var totalAmount = 0.0
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val amt = obj.getDouble("amount")
                    totalAmount += amt
                    newPeople.add(SplitPerson(i + 1, obj.getString("name"), customAmount = amt.toInt().toString()))
                }
                people = newPeople
                nextId = newPeople.size + 1
                splitMode = "custom"
                billAmount = totalAmount.toInt().toString()
                showSmartSplitSheet = false
                smartSplitInput = ""
                viewModel.clearSmartSplitResult()
            } catch (_: Exception) {}
        }
    }

    val bill = billAmount.toDoubleOrNull() ?: 0.0
    val tip = tipPercent.toDoubleOrNull() ?: 0.0
    val tipAmount = bill * (tip / 100)
    val totalWithTip = bill + tipAmount

    val perPersonAmounts = remember(totalWithTip, people, splitMode) {
        when (splitMode) {
            "equal" -> {
                val each = if (people.isNotEmpty()) totalWithTip / people.size else 0.0
                people.associate { it.id to each }
            }
            "percentage" -> {
                people.associate { p ->
                    val pct = p.percentage.toDoubleOrNull() ?: 0.0
                    p.id to (totalWithTip * pct / 100)
                }
            }
            "custom" -> {
                people.associate { p ->
                    p.id to (p.customAmount.toDoubleOrNull() ?: 0.0)
                }
            }
            else -> emptyMap()
        }
    }

    val totalAllocated = perPersonAmounts.values.sum()
    val remaining = totalWithTip - totalAllocated

    Scaffold(containerColor = ZorvynBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── HEADER ──
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
                    Text("SPLIT THE BILL", color = indigo, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Fair and easy", color = TextSecondary, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── BILL AMOUNT HERO CARD ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12151C)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(indigo.copy(0.25f), premiumGold.copy(0.1f)))
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "TOTAL BILL",
                        color = TextSecondary.copy(0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Big bill input with visible cursor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "₹",
                            color = premiumGold,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        BasicTextField(
                            value = billAmount,
                            onValueChange = { billAmount = it },
                            textStyle = LocalTextStyle.current.copy(
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            cursorBrush = SolidColor(Color.Transparent),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (billAmount.isEmpty()) {
                                        Text(
                                            "0",
                                            color = TextSecondary.copy(0.15f),
                                            fontSize = 48.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    // ── TIP SELECTOR ──
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color.White.copy(0.04f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolunteerActivism, null, tint = TextSecondary.copy(0.4f), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Tip", color = TextSecondary.copy(0.5f), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("0", "5", "10", "15", "20").forEach { t ->
                            val isSelected = tipPercent == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) premiumGold.copy(0.15f) else Color.White.copy(0.03f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .then(
                                        if (isSelected) Modifier.border(1.dp, premiumGold.copy(0.4f), RoundedCornerShape(10.dp))
                                        else Modifier
                                    )
                                    .clickable { tipPercent = t }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$t%",
                                    color = if (isSelected) premiumGold else TextSecondary.copy(0.5f),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Tip summary
                    if (tipAmount > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(0.03f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Tip", color = TextSecondary.copy(0.5f), fontSize = 11.sp)
                                Text("₹${fmt.format(tipAmount.roundToInt())}", color = premiumGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total", color = TextSecondary.copy(0.5f), fontSize = 11.sp)
                                Text("₹${fmt.format(totalWithTip.roundToInt())}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── AI SMART SPLIT BUTTON ──
            Button(
                onClick = { showSmartSplitSheet = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4231E7))
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Smart Split ✨", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── SPLIT MODE ──
            Text("SPLIT MODE", color = TextSecondary.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZorvynSurface, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("equal" to "Equal", "percentage" to "By %", "custom" to "Custom").forEach { (mode, label) ->
                    val isSelected = splitMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) indigo.copy(0.15f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .then(
                                if (isSelected) Modifier.border(1.dp, indigo.copy(0.3f), RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .clickable { splitMode = mode }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (isSelected) indigo else TextSecondary.copy(0.5f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── PEOPLE ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PEOPLE", color = TextSecondary.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Box(
                    modifier = Modifier
                        .background(indigo.copy(0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("${people.size}", color = indigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // People cards
            people.forEachIndexed { index, person ->
                val amount = perPersonAmounts[person.id] ?: 0.0
                val avatarColors = listOf(
                    premiumGold,
                    indigo,
                    Color(0xFFA288E3),
                    Color(0xFF34D399),
                    ZorvynRed,
                    Color(0xFFF59E0B)
                )
                val avatarColor = avatarColors[index % avatarColors.size]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ZorvynSurface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (index == 0) premiumGold.copy(0.12f) else TextSecondary.copy(0.06f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(avatarColor.copy(0.12f), CircleShape)
                                    .border(1.dp, avatarColor.copy(0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (person.name.firstOrNull()?.uppercase() ?: "${index + 1}"),
                                    color = avatarColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Name
                            Column(modifier = Modifier.weight(1f)) {
                                if (index == 0) {
                                    Text("You", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    if (index == 0) {
                                        Text("Organizer", color = premiumGold.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = person.name,
                                        onValueChange = { newName: String ->
                                            people = people.toMutableList().also { list ->
                                                list[index] = person.copy(name = newName)
                                            }
                                        },
                                        placeholder = { Text("Enter name", color = TextSecondary.copy(0.3f), fontSize = 14.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            cursorColor = indigo
                                        )
                                    )
                                }
                            }

                            // Amount
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "₹${fmt.format(amount.roundToInt())}",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (splitMode == "equal" && people.isNotEmpty()) {
                                    Text(
                                        "1/${people.size} share",
                                        color = TextSecondary.copy(0.4f),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Delete
                            if (index > 0) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(ZorvynRed.copy(0.08f), CircleShape)
                                        .clickable {
                                            people = people.toMutableList().also { it.removeAt(index) }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, null, tint = ZorvynRed.copy(0.6f), modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Inline input for percentage/custom
                        if (splitMode == "percentage" || splitMode == "custom") {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color.White.copy(0.04f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (splitMode == "percentage") "Share %" else "Amount ₹",
                                    color = TextSecondary.copy(0.4f),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = if (splitMode == "percentage") person.percentage else person.customAmount,
                                    onValueChange = { v: String ->
                                        people = people.toMutableList().also { list ->
                                            list[index] = if (splitMode == "percentage")
                                                person.copy(percentage = v)
                                            else
                                                person.copy(customAmount = v)
                                        }
                                    },
                                    placeholder = { Text(if (splitMode == "percentage") "0%" else "₹0", color = TextSecondary.copy(0.2f)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(100.dp),
                                    textStyle = LocalTextStyle.current.copy(
                                        color = indigo,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = indigo.copy(0.3f),
                                        unfocusedBorderColor = TextSecondary.copy(0.1f),
                                        cursorColor = indigo
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Add person
            OutlinedButton(
                onClick = {
                    people = people + SplitPerson(nextId, "")
                    nextId++
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, indigo.copy(0.2f))
            ) {
                Icon(Icons.Default.PersonAdd, null, tint = indigo, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Person", color = indigo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Balance indicator
            if (splitMode != "equal" && bill > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                val isBalanced = kotlin.math.abs(remaining) < 1.0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isBalanced) ZorvynGreen.copy(0.06f) else ZorvynRed.copy(0.06f),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            if (isBalanced) ZorvynGreen.copy(0.15f) else ZorvynRed.copy(0.15f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isBalanced) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            null,
                            tint = if (isBalanced) ZorvynGreen else ZorvynRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isBalanced) "Perfectly split" else "Remaining to allocate",
                            color = if (isBalanced) ZorvynGreen else ZorvynRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (!isBalanced) {
                        Text(
                            "₹${fmt.format(remaining.roundToInt())}",
                            color = ZorvynRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── UPI ID INPUT ──
            if (bill > 0) {
                Text("UPI ID (for QR on share card)", color = TextSecondary.copy(0.4f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    placeholder = { Text("yourname@upi", color = TextSecondary.copy(0.2f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = indigo.copy(0.4f),
                        unfocusedBorderColor = TextSecondary.copy(0.1f),
                        cursorColor = indigo
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── ACTION BUTTONS ──
            if (bill > 0) {
                // Premium share card
                Button(
                    onClick = { showShareSheet = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = indigo)
                ) {
                    Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Share Premium Split Card", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Copy text
                OutlinedButton(
                    onClick = {
                        val shareText = buildShareText(bill, tipAmount, totalWithTip, people, perPersonAmounts, fmt)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Bill Split", shareText))
                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary.copy(0.15f))
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = TextSecondary.copy(0.7f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy as Text", color = TextSecondary.copy(0.7f), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ── AI Smart Split Bottom Sheet ──
    if (showSmartSplitSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSmartSplitSheet = false },
            containerColor = ZorvynSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text("AI Smart Split ✨", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Describe what everyone ordered in plain English", color = TextSecondary.copy(0.6f), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = smartSplitInput,
                    onValueChange = { smartSplitInput = it },
                    placeholder = { Text("e.g. Rahul had pizza ₹350, Priya had pasta ₹280, I had coffee ₹150", color = TextSecondary.copy(0.3f)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4231E7),
                        unfocusedBorderColor = TextSecondary.copy(0.15f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Color(0xFF4231E7)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { if (smartSplitInput.isNotBlank()) viewModel.parseSmartSplit(smartSplitInput) },
                    enabled = smartSplitInput.isNotBlank() && !isSmartSplitLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4231E7))
                ) {
                    if (isSmartSplitLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Gemini is thinking...", color = Color.White, fontSize = 14.sp)
                    } else {
                        Text("Split with AI →", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ── PREMIUM SHARE CARD BOTTOM SHEET ──
    if (showShareSheet && bill > 0) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            containerColor = ZorvynSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PREVIEW", color = TextSecondary.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(12.dp))

                // The premium card
                SplitReceiptCard(
                    bill = bill,
                    tipAmount = tipAmount,
                    totalWithTip = totalWithTip,
                    people = people,
                    perPersonAmounts = perPersonAmounts,
                    upiId = upiId
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Share as image
                Button(
                    onClick = {
                        val bitmap = renderSplitCardBitmap(
                            bill = bill,
                            tipAmount = tipAmount,
                            totalWithTip = totalWithTip,
                            people = people,
                            perPersonAmounts = perPersonAmounts,
                            upiId = upiId
                        )
                        shareCardAsImage(context, bitmap)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = indigo)
                ) {
                    Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share as Image", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Share as text fallback
                OutlinedButton(
                    onClick = {
                        val shareText = buildShareText(bill, tipAmount, totalWithTip, people, perPersonAmounts, fmt)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share split"))
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary.copy(0.15f))
                ) {
                    Text("Share as Text", color = TextSecondary.copy(0.7f), fontSize = 13.sp)
                }
            }
        }
    }
}

private fun buildShareText(
    bill: Double,
    tip: Double,
    total: Double,
    people: List<SplitPerson>,
    amounts: Map<Int, Double>,
    fmt: NumberFormat
): String {
    val sb = StringBuilder()
    sb.appendLine("💰 Bill Split — via expectr")
    sb.appendLine("─────────────────")
    sb.appendLine("Bill: ₹${fmt.format(bill.roundToInt())}")
    if (tip > 0) sb.appendLine("Tip: ₹${fmt.format(tip.roundToInt())}")
    sb.appendLine("Total: ₹${fmt.format(total.roundToInt())}")
    sb.appendLine("─────────────────")
    people.forEach { p ->
        val name = p.name.ifBlank { "Person ${p.id}" }
        val amt = amounts[p.id] ?: 0.0
        sb.appendLine("• $name → ₹${fmt.format(amt.roundToInt())}")
    }
    sb.appendLine("─────────────────")
    sb.appendLine("Split with expectr 🚀")
    return sb.toString()
}
