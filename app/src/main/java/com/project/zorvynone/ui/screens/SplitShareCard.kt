package com.project.zorvynone.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.project.zorvynone.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════
// ── PREMIUM SPLIT RECEIPT CARD ────────────────────────────────
// ═══════════════════════════════════════════════════════════════

@Composable
fun SplitReceiptCard(
    bill: Double,
    tipAmount: Double,
    totalWithTip: Double,
    people: List<SplitPerson>,
    perPersonAmounts: Map<Int, Double>,
    upiId: String,
    modifier: Modifier = Modifier
) {
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))
    val indigo = Color(0xFF6366F1)
    val purple = Color(0xFF8B5CF6)
    val premiumGold = Color(0xFFE5C158)
    val cardBg = Color(0xFF0F1118)
    val rowBg = Color(0xFF161A24)

    val avatarColors = listOf(premiumGold, indigo, Color(0xFFA288E3), Color(0xFF34D399), Color(0xFFF59E0B), Color(0xFFEF4444))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(24.dp))
            .border(1.dp, indigo.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        // ── Header gradient ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(indigo, purple)),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "SPLIT RECEIPT",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "ZorvynOne",
                    color = Color.White.copy(0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            // ── Bill breakdown ──
            BillRow("Bill Amount", "₹${fmt.format(bill.roundToInt())}", Color.White)
            if (tipAmount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                BillRow("Tip", "₹${fmt.format(tipAmount.roundToInt())}", Color.White.copy(0.6f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color.White.copy(0.06f))
            Spacer(modifier = Modifier.height(6.dp))
            BillRow("Grand Total", "₹${fmt.format(totalWithTip.roundToInt())}", premiumGold, bold = true)

            Spacer(modifier = Modifier.height(20.dp))

            // ── People section ──
            Text(
                "SPLIT BETWEEN",
                color = Color.White.copy(0.35f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            people.forEachIndexed { index, person ->
                val amount = perPersonAmounts[person.id] ?: 0.0
                val avatarColor = avatarColors[index % avatarColors.size]
                val name = person.name.ifBlank { "Person ${person.id}" }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .background(rowBg, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(avatarColor.copy(0.15f), CircleShape)
                            .border(1.dp, avatarColor.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name.first().uppercase(),
                            color = avatarColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        if (index == 0) {
                            Text("Organizer", color = premiumGold.copy(0.5f), fontSize = 10.sp)
                        }
                    }

                    Text(
                        "₹${fmt.format(amount.roundToInt())}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── UPI QR Section ──
            if (upiId.isNotBlank() && people.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(0.06f))
                Spacer(modifier = Modifier.height(16.dp))

                val organizerName = people.firstOrNull()?.name?.ifBlank { "User" } ?: "User"
                val firstPersonAmount = perPersonAmounts[people.first().id] ?: 0.0
                val upiString = "upi://pay?pa=$upiId&pn=$organizerName&am=${firstPersonAmount.roundToInt()}&cu=INR"

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "SCAN TO PAY",
                        color = Color.White.copy(0.35f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val qrBitmap = remember(upiString) { generateQrBitmap(upiString, 200) }
                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "UPI QR Code",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                            )
                        }
                    } else {
                        // Fallback — show UPI ID text
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode2, null, tint = indigo, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(upiId, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Pay $organizerName · $upiId",
                        color = Color.White.copy(0.4f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Footer branding ──
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(0.04f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Split with ZorvynOne 🚀",
                color = Color.White.copy(0.25f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, valueColor: Color, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color.White.copy(if (bold) 0.7f else 0.45f),
            fontSize = if (bold) 14.sp else 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value,
            color = valueColor,
            fontSize = if (bold) 16.sp else 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// ── QR CODE GENERATION ────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════

fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val darkPaint = Paint().apply { color = android.graphics.Color.parseColor("#1A1A2E") }
        val lightPaint = Paint().apply { color = android.graphics.Color.WHITE }

        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), lightPaint)

        val moduleSize = size.toFloat() / bitMatrix.width
        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                if (bitMatrix.get(x, y)) {
                    val rect = RectF(
                        x * moduleSize, y * moduleSize,
                        (x + 1) * moduleSize, (y + 1) * moduleSize
                    )
                    canvas.drawRoundRect(rect, moduleSize * 0.3f, moduleSize * 0.3f, darkPaint)
                }
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

// ═══════════════════════════════════════════════════════════════
// ── BITMAP CAPTURE & SHARE ────────────────────────────────────
// ═══════════════════════════════════════════════════════════════

fun shareCardAsImage(context: Context, bitmap: Bitmap) {
    try {
        val dir = File(context.cacheDir, "shared_images")
        dir.mkdirs()
        val file = File(dir, "zorvyn_split_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share split receipt"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ═══════════════════════════════════════════════════════════════
// ── CANVAS BITMAP RENDERER ────────────────────────────────────
// ═══════════════════════════════════════════════════════════════

fun renderSplitCardBitmap(
    bill: Double,
    tipAmount: Double,
    totalWithTip: Double,
    people: List<SplitPerson>,
    perPersonAmounts: Map<Int, Double>,
    upiId: String
): Bitmap {
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))
    val w = 900
    val density = 2.5f

    // Colors
    val bgColor = android.graphics.Color.parseColor("#0F1118")
    val rowBgColor = android.graphics.Color.parseColor("#161A24")
    val indigoColor = android.graphics.Color.parseColor("#6366F1")
    val purpleColor = android.graphics.Color.parseColor("#8B5CF6")
    val goldColor = android.graphics.Color.parseColor("#E5C158")
    val whiteColor = android.graphics.Color.WHITE
    val dimWhite = android.graphics.Color.parseColor("#73FFFFFF")
    val subtleWhite = android.graphics.Color.parseColor("#40FFFFFF")
    val avatarColorInts = intArrayOf(
        android.graphics.Color.parseColor("#E5C158"),
        android.graphics.Color.parseColor("#6366F1"),
        android.graphics.Color.parseColor("#A288E3"),
        android.graphics.Color.parseColor("#34D399"),
        android.graphics.Color.parseColor("#F59E0B"),
        android.graphics.Color.parseColor("#EF4444")
    )

    // Paints
    val bgPaint = Paint().apply { color = bgColor; isAntiAlias = true }
    val headerPaint = Paint().apply { color = indigoColor; isAntiAlias = true }
    val titlePaint = Paint().apply { color = whiteColor; textSize = 16f * density; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true; letterSpacing = 0.15f }
    val subtitlePaint = Paint().apply { color = dimWhite; textSize = 10f * density; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    val labelPaint = Paint().apply { color = subtleWhite; textSize = 13f * density; isAntiAlias = true }
    val valuePaint = Paint().apply { color = whiteColor; textSize = 13f * density; textAlign = Paint.Align.RIGHT; isAntiAlias = true }
    val boldLabelPaint = Paint().apply { color = dimWhite; textSize = 14f * density; isFakeBoldText = true; isAntiAlias = true }
    val boldValuePaint = Paint().apply { color = goldColor; textSize = 16f * density; isFakeBoldText = true; textAlign = Paint.Align.RIGHT; isAntiAlias = true }
    val sectionPaint = Paint().apply { color = subtleWhite; textSize = 10f * density; isFakeBoldText = true; letterSpacing = 0.1f; isAntiAlias = true }
    val namePaint = Paint().apply { color = whiteColor; textSize = 14f * density; isFakeBoldText = true; isAntiAlias = true }
    val amountPaint = Paint().apply { color = whiteColor; textSize = 16f * density; isFakeBoldText = true; textAlign = Paint.Align.RIGHT; isAntiAlias = true }
    val tagPaint = Paint().apply { color = android.graphics.Color.parseColor("#80E5C158"); textSize = 10f * density; isAntiAlias = true }
    val footerPaint = Paint().apply { color = android.graphics.Color.parseColor("#40FFFFFF"); textSize = 11f * density; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    val rowBgPaint = Paint().apply { color = rowBgColor; isAntiAlias = true }
    val dividerPaint = Paint().apply { color = android.graphics.Color.parseColor("#10FFFFFF"); strokeWidth = 1f * density; isAntiAlias = true }
    val avatarTextPaint = Paint().apply { textSize = 14f * density; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true }

    val pad = 20f * density
    val headerH = 60f * density
    val personRowH = 58f * density
    val qrSize = 160f * density

    // Calculate height
    var totalH = headerH + pad // header
    totalH += 30f * density // bill row
    if (tipAmount > 0) totalH += 22f * density
    totalH += 30f * density // divider + grand total
    totalH += 30f * density // "SPLIT BETWEEN"
    totalH += people.size * (personRowH + 8f * density) // person rows
    if (upiId.isNotBlank()) totalH += qrSize + 60f * density // QR section
    totalH += 50f * density // footer
    totalH += pad * 2

    val bitmap = Bitmap.createBitmap(w, totalH.toInt(), Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)

    // Background
    c.drawRoundRect(RectF(0f, 0f, w.toFloat(), totalH), 24f * density, 24f * density, bgPaint)

    // Header gradient
    val headerShader = android.graphics.LinearGradient(0f, 0f, w.toFloat(), 0f, indigoColor, purpleColor, android.graphics.Shader.TileMode.CLAMP)
    headerPaint.shader = headerShader
    c.drawRoundRect(RectF(0f, 0f, w.toFloat(), headerH), 24f * density, 24f * density, headerPaint)
    c.drawRect(RectF(0f, headerH - 24f * density, w.toFloat(), headerH), headerPaint)
    c.drawText("SPLIT RECEIPT", w / 2f, headerH / 2f - 2f * density, titlePaint)
    c.drawText("ZorvynOne", w / 2f, headerH / 2f + 16f * density, subtitlePaint)

    var y = headerH + pad

    // Bill Amount
    c.drawText("Bill Amount", pad, y, labelPaint)
    c.drawText("₹${fmt.format(bill.roundToInt())}", w - pad, y, valuePaint)
    y += 22f * density

    // Tip
    if (tipAmount > 0) {
        c.drawText("Tip", pad, y, labelPaint)
        c.drawText("₹${fmt.format(tipAmount.roundToInt())}", w - pad, y, valuePaint)
        y += 22f * density
    }

    // Divider
    c.drawLine(pad, y, w - pad, y, dividerPaint)
    y += 12f * density

    // Grand Total
    c.drawText("Grand Total", pad, y, boldLabelPaint)
    c.drawText("₹${fmt.format(totalWithTip.roundToInt())}", w - pad, y, boldValuePaint)
    y += 30f * density

    // SPLIT BETWEEN
    c.drawText("SPLIT BETWEEN", pad, y, sectionPaint)
    y += 18f * density

    // Person rows
    people.forEachIndexed { index, person ->
        val amount = perPersonAmounts[person.id] ?: 0.0
        val name = person.name.ifBlank { "Person ${person.id}" }
        val avatarColor = avatarColorInts[index % avatarColorInts.size]

        // Row background
        val rowTop = y
        val rowBottom = y + personRowH
        c.drawRoundRect(RectF(pad, rowTop, w - pad, rowBottom), 14f * density, 14f * density, rowBgPaint)

        // Avatar circle
        val avatarCx = pad + 30f * density
        val avatarCy = rowTop + personRowH / 2f
        val avatarR = 17f * density
        val avatarBgPaint = Paint().apply { color = avatarColor; alpha = 40; isAntiAlias = true }
        c.drawCircle(avatarCx, avatarCy, avatarR, avatarBgPaint)
        avatarTextPaint.color = avatarColor
        c.drawText(name.first().uppercase(), avatarCx, avatarCy + 5f * density, avatarTextPaint)

        // Name
        val nameX = avatarCx + avatarR + 12f * density
        c.drawText(name, nameX, avatarCy - 2f * density, namePaint)
        if (index == 0) {
            c.drawText("Organizer", nameX, avatarCy + 14f * density, tagPaint)
        }

        // Amount
        c.drawText("₹${fmt.format(amount.roundToInt())}", w - pad - 14f * density, avatarCy + 5f * density, amountPaint)

        y = rowBottom + 8f * density
    }

    // UPI QR
    if (upiId.isNotBlank() && people.isNotEmpty()) {
        y += 10f * density
        c.drawLine(pad, y, w - pad, y, dividerPaint)
        y += 16f * density

        val scanPaint = Paint().apply { color = subtleWhite; textSize = 10f * density; isFakeBoldText = true; textAlign = Paint.Align.CENTER; letterSpacing = 0.1f; isAntiAlias = true }
        c.drawText("SCAN TO PAY", w / 2f, y, scanPaint)
        y += 14f * density

        val organizerName = people.firstOrNull()?.name?.ifBlank { "User" } ?: "User"
        val firstAmount = perPersonAmounts[people.first().id] ?: 0.0
        val upiString = "upi://pay?pa=$upiId&pn=$organizerName&am=${firstAmount.roundToInt()}&cu=INR"
        val qrBitmap = generateQrBitmap(upiString, (qrSize * 0.85f).toInt())

        if (qrBitmap != null) {
            val qrLeft = (w - qrSize) / 2f
            // White rounded background
            val qrBgPaint = Paint().apply { color = whiteColor; isAntiAlias = true }
            c.drawRoundRect(RectF(qrLeft, y, qrLeft + qrSize, y + qrSize), 16f * density, 16f * density, qrBgPaint)
            // Draw QR
            val qrPad = 10f * density
            c.drawBitmap(qrBitmap, null, RectF(qrLeft + qrPad, y + qrPad, qrLeft + qrSize - qrPad, y + qrSize - qrPad), null)
            y += qrSize + 8f * density
        }

        val payPaint = Paint().apply { color = subtleWhite; textSize = 11f * density; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        c.drawText("Pay $organizerName · $upiId", w / 2f, y, payPaint)
        y += 20f * density
    }

    // Footer
    y += 4f * density
    c.drawLine(pad, y, w - pad, y, dividerPaint)
    y += 18f * density
    c.drawText("Split with ZorvynOne 🚀", w / 2f, y, footerPaint)

    return bitmap
}
