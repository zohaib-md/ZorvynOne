package com.project.zorvynone.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ZorvynWidgetReceiver : GlanceAppWidgetReceiver() {
    // This connects the Android OS to your Compose UI
    override val glanceAppWidget: GlanceAppWidget = ZorvynWidget()
}