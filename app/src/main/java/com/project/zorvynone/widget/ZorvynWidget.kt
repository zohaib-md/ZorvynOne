package com.project.zorvynone.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.Image
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.project.zorvynone.MainActivity
import com.project.zorvynone.R
import com.project.zorvynone.model.AppDatabase
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Locale

class ZorvynWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.transactionDao()

        // 1. Fetch Real Data
        val totalIncome = dao.getTotalIncome().first() ?: 0
        val totalExpense = dao.getTotalExpenses().first() ?: 0
        val balance = totalIncome - totalExpense

        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }

        provideContent {
            WidgetUI(balance = formatter.format(balance))
        }
    }

    @Composable
    private fun WidgetUI(balance: String) {
        val premiumGold = ColorProvider(Color(0xFFE5C158))
        val surfaceColor = ColorProvider(Color(0xFF1C2238))
        val textColor = ColorProvider(Color(0xFFFFFFFF))
        val mutedText = ColorProvider(Color(0xFFA0A5B5))

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .cornerRadius(24.dp)
                .padding(20.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: Focused on Balance
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "expectr",
                    style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(GlanceModifier.height(12.dp))
                Text(
                    text = "TOTAL BALANCE",
                    style = TextStyle(color = mutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = balance,
                    style = TextStyle(color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                )
            }

            // RIGHT SIDE: The Quick Add Action
            Column(
                horizontalAlignment = Alignment.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(56.dp)
                        .background(premiumGold)
                        .cornerRadius(28.dp)
                        .clickable(
                            actionStartActivity<MainActivity>(
                                actionParametersOf(androidx.glance.action.ActionParameters.Key<String>("nav") to "add")
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_add_rounded),
                        contentDescription = "Add",
                        modifier = GlanceModifier.size(28.dp)
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = "QUICK ADD",
                    style = TextStyle(color = premiumGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}