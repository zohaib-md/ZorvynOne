package com.project.zorvynone.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════
// ── CRED-STYLE FLOATING BOTTOM NAV BAR ───────────────────────
// ═══════════════════════════════════════════════════════════════

data class NavItem(
    val route: String,
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
)

val bottomNavItems = listOf(
    NavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavItem("transactions", "History", Icons.Filled.Receipt, Icons.Outlined.Receipt),
    NavItem("add_transaction", "Add", Icons.Filled.AddCircle, Icons.Outlined.AddCircle),
    NavItem("insights", "Insights", Icons.Filled.Insights, Icons.Outlined.Insights),
    NavItem("score", "Score", Icons.Filled.Speed, Icons.Outlined.Speed)
)

@Composable
fun ZorvynBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // CRED aesthetic — dark, muted, premium grey
    val pillBg = Color(0xFF1A1A1A)
    val selectedPillBg = Color(0xFF2E2E2E)
    val selectedTint = Color(0xFFE8E8E8)
    val unselectedTint = Color(0xFF5C5C5C)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(0.4f), spotColor = Color.Black.copy(0.3f))
                .background(pillBg, RoundedCornerShape(24.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route

                ZorvynNavItem(
                    item = item,
                    isSelected = isSelected,
                    selectedBg = selectedPillBg,
                    selectedTint = selectedTint,
                    unselectedTint = unselectedTint,
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.weight(if (isSelected) 2f else 1f)
                )
            }
        }
    }
}

@Composable
private fun ZorvynNavItem(
    item: NavItem,
    isSelected: Boolean,
    selectedBg: Color,
    selectedTint: Color,
    unselectedTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 22.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "iconSize"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isSelected) {
                    Modifier.background(selectedBg, RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.iconSelected else item.iconUnselected,
                contentDescription = item.label,
                tint = if (isSelected) selectedTint else unselectedTint,
                modifier = Modifier.size(iconSize)
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(250)) + expandHorizontally(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Start
                ),
                exit = fadeOut(tween(150)) + shrinkHorizontally(
                    animationSpec = tween(200),
                    shrinkTowards = Alignment.Start
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        item.label,
                        color = selectedTint,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        softWrap = false
                    )
                }
            }
        }
    }
}
