package com.project.zorvynone.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ZorvynColorScheme = darkColorScheme(
    background = ZorvynBackground,
    surface = ZorvynSurface,
    surfaceVariant = ZorvynSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    primary = ZorvynGreen,
    error = ZorvynRed
)

@Composable
fun ZorvynOneTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ZorvynBackground.toArgb()
            window.navigationBarColor = ZorvynBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = ZorvynColorScheme,
        typography = Typography, // Uses default from Type.kt
        content = content
    )
}