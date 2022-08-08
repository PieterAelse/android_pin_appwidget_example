package com.signify.pinappwidget.example

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class SimpleWidgetProvider : AppWidgetProvider() {
    // Normally your specialized onUpdate, onReceive etc. logic will be here

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.some_1x1_widget)
            views.setTextViewText(R.id.widget_text, "I AM $appWidgetId")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}