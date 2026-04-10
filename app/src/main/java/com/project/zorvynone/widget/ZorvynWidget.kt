package com.project.zorvynone.widget

import android.content.Context
import androidx.compose.runtime.Composable // <-- Added Missing Import
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider // <-- Added Missing Import
import com.project.zorvynone.MainActivity // Make sure this matches your package name!

class ZorvynWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Mock data for the portfolio UI
        val mockBalance = "₹28,413"
        val mockScore = "84"

        provideContent {
            WidgetUI(balance = mockBalance, score = mockScore)
        }
    }

    @Composable
    private fun WidgetUI(balance: String, score: String) {
        // Glance requires colors to be wrapped in ColorProvider
        val premiumGold = ColorProvider(Color(0xFFE5C158))
        val surfaceColor = ColorProvider(Color(0xFF1C2238))
        val textColor = ColorProvider(Color(0xFFE2E2E9))
        val mutedTextColor = ColorProvider(Color(0xFFE2E2E9).copy(alpha = 0.6f))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .cornerRadius(20.dp) // Premium rounded corners
                .padding(16.dp)
                // Clicking the widget opens the app!
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ZORVYN",
                    style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "ONE",
                    style = TextStyle(color = premiumGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }

            Spacer(GlanceModifier.height(16.dp))

            // Balance Section
            Text(
                text = "TOTAL BALANCE",
                style = TextStyle(color = mutedTextColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            )
            Text(
                text = balance,
                style = TextStyle(color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            )

            Spacer(GlanceModifier.height(16.dp))

            // Score Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ZORVYN SCORE: ",
                    style = TextStyle(color = mutedTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = score,
                    style = TextStyle(color = premiumGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}