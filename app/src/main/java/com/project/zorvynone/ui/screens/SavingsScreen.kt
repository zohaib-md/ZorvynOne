package com.project.zorvynone.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.project.zorvynone.R
import com.project.zorvynone.model.SavingsGoal
import com.project.zorvynone.ui.theme.*
import com.project.zorvynone.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════
// ── SMART VAULTS — PREMIUM SAVINGS SCREEN ────────────────────
// ═══════════════════════════════════════════════════════════════

private val premiumGold = Color(0xFFE5C158)
private val emerald = Color(0xFF34D399)
private val deepPurple = Color(0xFF7C3AED)
private val vibrantPurple = Color(0xFFA78BFA)
private val deepTeal = Color(0xFF0D9488)
private val vibrantTeal = Color(0xFF5EEAD4)
private val rosePink = Color(0xFFFB7185)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(viewModel: HomeViewModel) {
    val vaults by viewModel.allVaults.collectAsStateWithLifecycle()
    val totalSaved by viewModel.totalAllSavings.collectAsStateWithLifecycle()
    val totalRoundUp by viewModel.totalRoundUpSavings.collectAsStateWithLifecycle()
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))

    var showCreateSheet by remember { mutableStateOf(false) }
    var depositTargetVault by remember { mutableStateOf<SavingsGoal?>(null) }
    var detailVault by remember { mutableStateOf<SavingsGoal?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(ZorvynBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp)
        ) {
            // ── HEADER ──
            item {
                VaultsHeader(totalSaved = totalSaved, vaultCount = vaults.size)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── HERO STATS — vibrant gradient cards ──
            item {
                HeroStatsSection(totalSaved = totalSaved, totalRoundUp = totalRoundUp, vaultCount = vaults.size, fmt = fmt)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── VAULT CARDS ──
            if (vaults.isEmpty()) {
                item { EmptyVaultsState(onCreateClick = { showCreateSheet = true }) }
            } else {
                items(vaults, key = { it.id }) { vault ->
                    PremiumVaultCard(
                        vault = vault, fmt = fmt,
                        onDeposit = { depositTargetVault = vault },
                        onTapDetail = { detailVault = vault },
                        onSetRoundUp = { viewModel.setDefaultRoundUpVault(vault.id) },
                        onDelete = { viewModel.deleteVault(vault) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showCreateSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 90.dp),
            containerColor = premiumGold, contentColor = ZorvynBackground, shape = CircleShape
        ) { Icon(Icons.Default.Add, "Create Vault", modifier = Modifier.size(26.dp)) }
    }

    // Sheets
    if (showCreateSheet) {
        CreateVaultSheet(onDismiss = { showCreateSheet = false },
            onCreate = { t, a, d, e, r -> viewModel.createVault(t, a, d, e, r); showCreateSheet = false })
    }
    depositTargetVault?.let { v ->
        QuickDepositSheet(v, { depositTargetVault = null }, { viewModel.depositToVault(v.id, it); depositTargetVault = null })
    }
    detailVault?.let { v ->
        VaultDetailSheet(v, viewModel) { detailVault = null }
    }
}

// ═══════════════════════════════════════════════════════════════
// ── HEADER ───────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════
@Composable
private fun VaultsHeader(totalSaved: Double = 0.0, vaultCount: Int = 0) {
    val premiumGold = Color(0xFFE5C158)
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))

    val statusLine = when {
        vaultCount == 0 -> "Ring-fence money for a goal. Each vault is a separate savings pocket."
        totalSaved > 0 -> "₹${fmt.format(totalSaved.toLong())} set aside in $vaultCount goal${if (vaultCount > 1) "s" else ""}. Tap a vault to top up."
        else -> "$vaultCount savings pocket${if (vaultCount > 1) "s" else ""} active. Start adding to reach your goals."
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Vaults",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlayfairDisplay,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Thin gold accent rule
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            statusLine,
            color = TextSecondary.copy(0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 19.sp
        )
    }
}



// ═══════════════════════════════════════════════════════════════
// ── HERO STATS — VIBRANT GRADIENT CARDS ──────────────────────
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HeroStatsSection(totalSaved: Double, totalRoundUp: Double, vaultCount: Int, fmt: NumberFormat) {
    // Big total saved card — deep purple gradient
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .drawBehind { if (size.width > 0f && size.height > 0f) drawRect(Brush.linearGradient(listOf(Color(0xFF1E1036), deepPurple.copy(0.6f), Color(0xFF2D1B69)), Offset.Zero, Offset(size.width, size.height))) }
            .padding(22.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(vibrantPurple.copy(0.06f))
        )
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(vibrantPurple.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text("💰", fontSize = 18.sp) }
                Spacer(modifier = Modifier.width(10.dp))
                Text("TOTAL SAVED", color = vibrantPurple.copy(0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("₹${fmt.format(totalSaved.roundToInt())}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("across $vaultCount vault${if (vaultCount != 1) "s" else ""}", color = vibrantPurple.copy(0.6f), fontSize = 12.sp)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Round-ups + Active vaults — two smaller cards
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Round-ups — teal gradient
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .drawBehind { if (size.width > 0f && size.height > 0f) drawRect(Brush.linearGradient(listOf(Color(0xFF0A2520), deepTeal.copy(0.5f)), Offset.Zero, Offset(size.width, size.height))) }
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ROUND-UPS", color = vibrantTeal.copy(0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("₹${fmt.format(totalRoundUp.roundToInt())}", color = vibrantTeal, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                Text("auto-saved", color = vibrantTeal.copy(0.4f), fontSize = 11.sp)
            }
        }

        // Active vaults — rose/pink gradient
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .drawBehind { if (size.width > 0f && size.height > 0f) drawRect(Brush.linearGradient(listOf(Color(0xFF2A0F1A), rosePink.copy(0.4f)), Offset.Zero, Offset(size.width, size.height))) }
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏦", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ACTIVE", color = rosePink.copy(0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("$vaultCount", color = rosePink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("in progress", color = rosePink.copy(0.4f), fontSize = 11.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ── PREMIUM VAULT CARD ───────────────────────────────────────
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumVaultCard(
    vault: SavingsGoal, fmt: NumberFormat,
    onDeposit: () -> Unit, onTapDetail: () -> Unit,
    onSetRoundUp: () -> Unit, onDelete: () -> Unit
) {
    val progress = if (vault.targetAmount > 0) (vault.savedAmount / vault.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val progressPercent = (progress * 100).roundToInt()
    val daysLeft = TimeUnit.MILLISECONDS.toDays(vault.deadline - System.currentTimeMillis()).coerceAtLeast(0)
    val isComplete = progress >= 1f

    // Each vault gets a unique gradient palette based on its ID
    val palette = vaultPalette(vault.id)

    val animatedProgress by animateFloatAsState(
        targetValue = progress, animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "p"
    )
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .drawBehind { if (size.width > 0f && size.height > 0f) drawRect(Brush.linearGradient(listOf(palette.bgDark, palette.bgLight), Offset.Zero, Offset(size.width, size.height))) }
            .clickable { onTapDetail() }
    ) {
        Box(
            modifier = Modifier.size(120.dp).align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-20).dp)
                .clip(CircleShape).background(palette.accent.copy(0.05f))
        )

        Column(modifier = Modifier.padding(20.dp)) {
            // Top: Emoji badge + Info + Big Arc
            Row(verticalAlignment = Alignment.Top) {
                // Left: emoji in a frosted circle
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                        .background(palette.accent.copy(0.1f))
                        .border(1.dp, palette.accent.copy(0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(vault.iconEmoji, fontSize = 24.sp) }

                Spacer(modifier = Modifier.width(14.dp))

                // Center: title + amount
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(vault.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
                        if (vault.isDefaultRoundUpVault) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(premiumGold.copy(0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) { Text("⚡", fontSize = 11.sp) }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "₹${fmt.format(vault.savedAmount.roundToInt())} of ₹${fmt.format(vault.targetAmount.roundToInt())}",
                        color = palette.accent.copy(0.5f), fontSize = 13.sp
                    )
                }

                // Right: big progress ring OR trophy for completed
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                    if (isComplete) {
                        // Trophy celebration for 100%!
                        val trophyComp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.trophy))
                        LottieAnimation(
                            composition = trophyComp,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(60.dp)
                        )
                    } else {
                        Canvas(modifier = Modifier.size(64.dp)) {
                            val sw = 6.dp.toPx()
                            val arcSize = Size(size.width - sw, size.height - sw)
                            val arcOff = Offset(sw / 2, sw / 2)
                            drawArc(color = palette.accent.copy(0.1f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(sw, cap = StrokeCap.Round), topLeft = arcOff, size = arcSize)
                            drawArc(color = palette.accent, startAngle = -90f, sweepAngle = animatedProgress * 360f, useCenter = false, style = Stroke(sw, cap = StrokeCap.Round), topLeft = arcOff, size = arcSize)
                        }
                        Text("$progressPercent%", color = palette.accent, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(palette.accent.copy(0.08f))) {
                if (animatedProgress > 0f) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).clip(RoundedCornerShape(3.dp))
                        .drawBehind { if (size.width > 0f && size.height > 0f) drawRect(Brush.horizontalGradient(listOf(palette.accent.copy(0.5f), palette.accent))) })
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (isComplete) "🎉 Goal reached! Congratulations!" else if (daysLeft > 0) "${daysLeft} days remaining" else "Overdue",
                color = if (isComplete) emerald else palette.accent.copy(0.4f), fontSize = 11.sp, fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Deposit
                Box(
                    modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp))
                        .background(palette.accent.copy(0.12f))
                        .clickable { onDeposit() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deposit", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // History
                Box(
                    modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.04f))
                        .clickable { onTapDetail() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("History", color = Color.White.copy(0.5f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // More
                Box {
                    Box(
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.04f))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.MoreVert, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(18.dp)) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (!vault.isDefaultRoundUpVault) {
                            DropdownMenuItem(text = { Text("Set as round-up vault ⚡") }, onClick = { onSetRoundUp(); showMenu = false })
                        }
                        DropdownMenuItem(text = { Text("Delete vault", color = ZorvynRed) }, onClick = { onDelete(); showMenu = false })
                    }
                }
            }
        }
    }
}

// Unique gradient palette per vault
private data class VaultPalette(val bgDark: Color, val bgLight: Color, val accent: Color)
private fun vaultPalette(id: Int): VaultPalette = when (id % 4) {
    0 -> VaultPalette(Color(0xFF1E1036), Color(0xFF2D1B69).copy(0.7f), Color(0xFFA78BFA))  // Purple
    1 -> VaultPalette(Color(0xFF0A2520), Color(0xFF134E4A).copy(0.7f), Color(0xFF5EEAD4))  // Teal
    2 -> VaultPalette(Color(0xFF2A1507), Color(0xFF78350F).copy(0.5f), Color(0xFFFBBF24))  // Amber
    else -> VaultPalette(Color(0xFF1A0F1E), Color(0xFF5B2150).copy(0.6f), Color(0xFFF472B6))  // Pink
}

// ═══════════════════════════════════════════════════════════════
// ── EMPTY STATE ──────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EmptyVaultsState(onCreateClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .drawBehind { if (size.width > 0f && size.height > 0f) drawRect(Brush.linearGradient(listOf(Color(0xFF1E1036), deepPurple.copy(0.4f)), Offset.Zero, Offset(size.width, size.height))) }
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated Lottie target
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val sw = 5.dp.toPx()
                    drawArc(color = vibrantPurple.copy(0.15f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(sw, cap = StrokeCap.Round), topLeft = Offset(sw, sw), size = Size(size.width - sw * 2, size.height - sw * 2))
                    drawArc(color = vibrantPurple, startAngle = -90f, sweepAngle = 120f, useCenter = false, style = Stroke(sw, cap = StrokeCap.Round), topLeft = Offset(sw, sw), size = Size(size.width - sw * 2, size.height - sw * 2))
                }
                val targetComp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.reaching_target))
                LottieAnimation(
                    composition = targetComp,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("No vaults yet", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Create your first savings vault and\nwatch your wealth grow.", color = vibrantPurple.copy(0.5f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCreateClick, colors = ButtonDefaults.buttonColors(containerColor = premiumGold), shape = RoundedCornerShape(14.dp), modifier = Modifier.height(48.dp)) {
                Icon(Icons.Default.Add, null, tint = ZorvynBackground, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Vault", color = ZorvynBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ── CREATE VAULT SHEET ───────────────────────────────────────
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateVaultSheet(onDismiss: () -> Unit, onCreate: (String, Double, Long, String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎯") }
    var roundUpRule by remember { mutableIntStateOf(10) }
    val defaultDeadline = remember { Calendar.getInstance().apply { add(Calendar.MONTH, 6) }.timeInMillis }

    val emojis = listOf("🎯", "📱", "✈️", "🏠", "🎓", "💍", "🚗", "🎮", "👟", "💰", "🏖️", "🎵")
    val roundUpOptions = listOf(0, 10, 50, 100)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF0F0F14),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(0.3f)) }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) { append("New ") }
                withStyle(SpanStyle(color = vibrantPurple)) { append("vault") }
            }, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Text("Set a goal. Start building.", color = TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("VAULT NAME")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(value = title, onValueChange = { title = it },
                placeholder = { Text("e.g. Dream Vacation", color = TextSecondary.copy(0.4f)) },
                colors = sheetFieldColors(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)

            Spacer(modifier = Modifier.height(18.dp))
            SectionLabel("TARGET AMOUNT")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(value = targetAmount, onValueChange = { targetAmount = it.filter { c -> c.isDigit() } },
                placeholder = { Text("₹1,00,000", color = TextSecondary.copy(0.4f)) },
                leadingIcon = { Text("₹", color = premiumGold, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = sheetFieldColors(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)

            Spacer(modifier = Modifier.height(18.dp))
            SectionLabel("ICON")
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                emojis.forEach { emoji ->
                    val isChosen = emoji == selectedEmoji
                    Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (isChosen) vibrantPurple.copy(0.15f) else Color(0xFF1A1A22))
                        .border(1.5.dp, if (isChosen) vibrantPurple else TextSecondary.copy(0.1f), RoundedCornerShape(12.dp))
                        .clickable { selectedEmoji = emoji }, contentAlignment = Alignment.Center
                    ) { Text(emoji, fontSize = 20.sp) }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            SectionLabel("ROUND-UP RULE")
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                roundUpOptions.forEach { rule ->
                    val isChosen = rule == roundUpRule; val label = if (rule == 0) "Off" else "₹$rule"
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (isChosen) vibrantPurple.copy(0.15f) else Color(0xFF1A1A22))
                        .border(1.5.dp, if (isChosen) vibrantPurple else TextSecondary.copy(0.1f), RoundedCornerShape(10.dp))
                        .clickable { roundUpRule = rule }.padding(horizontal = 18.dp, vertical = 10.dp)
                    ) { Text(label, color = if (isChosen) vibrantPurple else TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            val canCreate = title.isNotBlank() && (targetAmount.toDoubleOrNull() ?: 0.0) > 0
            Button(onClick = { onCreate(title, targetAmount.toDouble(), defaultDeadline, selectedEmoji, roundUpRule) }, enabled = canCreate,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = vibrantPurple, disabledContainerColor = vibrantPurple.copy(0.25f)),
                shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Savings, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Vault", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ── QUICK DEPOSIT SHEET ──────────────────────────────────────
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickDepositSheet(vault: SavingsGoal, onDismiss: () -> Unit, onDeposit: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    val quickAmounts = listOf(100, 500, 1000, 5000)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF0F0F14),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(0.3f)) }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) { append("Deposit to ") }
                withStyle(SpanStyle(color = vibrantPurple)) { append(vault.title) }
            }, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } },
                placeholder = { Text("Enter amount", color = TextSecondary.copy(0.4f)) },
                leadingIcon = { Text("₹", color = premiumGold, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = sheetFieldColors(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)
            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                quickAmounts.forEach { q ->
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1A22))
                        .border(1.dp, TextSecondary.copy(0.1f), RoundedCornerShape(10.dp))
                        .clickable { amount = q.toString() }.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) { Text("₹$q", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                }
            }
            Spacer(modifier = Modifier.height(22.dp))

            Button(onClick = { amount.toDoubleOrNull()?.let { onDeposit(it) } }, enabled = (amount.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = vibrantPurple, disabledContainerColor = vibrantPurple.copy(0.25f)),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Deposit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ── VAULT DETAIL / HISTORY SHEET ─────────────────────────────
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultDetailSheet(vault: SavingsGoal, viewModel: HomeViewModel, onDismiss: () -> Unit) {
    val deposits by viewModel.getDepositsForGoal(vault.id).collectAsStateWithLifecycle(emptyList())
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))
    val progress = if (vault.targetAmount > 0) (vault.savedAmount / vault.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val palette = vaultPalette(vault.id)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF0F0F14),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(0.3f)) }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(palette.accent.copy(0.1f)),
                    contentAlignment = Alignment.Center) { Text(vault.iconEmoji, fontSize = 24.sp) }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(vault.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    Text("${(progress * 100).roundToInt()}% complete", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))

            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(palette.accent.copy(0.08f))) {
                if (progress > 0f) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(4.dp))
                        .drawBehind { if (size.width > 0f && size.height > 0f) drawRect(Brush.horizontalGradient(listOf(palette.accent.copy(0.5f), palette.accent))) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text("₹${fmt.format(vault.savedAmount.roundToInt())}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(" / ₹${fmt.format(vault.targetAmount.roundToInt())}", color = TextSecondary, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(22.dp))
            SectionLabel("DEPOSIT HISTORY")
            Spacer(modifier = Modifier.height(12.dp))

            if (deposits.isEmpty()) {
                Text("No deposits yet. Tap deposit to start!", color = TextSecondary.copy(0.5f), fontSize = 13.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    deposits.take(20).forEach { deposit ->
                        val (icon, sourceLabel, sourceColor) = when (deposit.source) {
                            "round_up" -> Triple("⚡", "Round-up", emerald)
                            "sweep" -> Triple("🔄", "Sweep", Color(0xFF60A5FA))
                            else -> Triple("💰", "Manual", premiumGold)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A22)).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(sourceColor.copy(0.1f)),
                                contentAlignment = Alignment.Center) { Text(icon, fontSize = 16.sp) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sourceLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                val dateStr = java.text.SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(java.util.Date(deposit.timestamp))
                                Text(dateStr, color = TextSecondary.copy(0.5f), fontSize = 11.sp)
                            }
                            Text("+₹${fmt.format(deposit.amount.roundToInt())}", color = sourceColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ── SHARED UTILITIES ─────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun sheetFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = Color(0xFF1A1A22), focusedContainerColor = Color(0xFF1A1A22),
    unfocusedBorderColor = TextSecondary.copy(0.1f), focusedBorderColor = vibrantPurple,
    cursorColor = vibrantPurple, focusedTextColor = Color.White, unfocusedTextColor = Color.White
)
