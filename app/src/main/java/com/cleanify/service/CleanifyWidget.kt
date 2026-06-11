package com.cleanify.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.cleanify.R

class CleanifyWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_cleanify_layout)

            views.setOnClickPendingIntent(R.id.action_clean, buildDeepLinkIntent(
                context, "app://com.cleanify/empty_cleaner", R.id.action_clean
            ))
            views.setOnClickPendingIntent(R.id.action_duplicates, buildDeepLinkIntent(
                context, "app://com.cleanify/duplicates_graph", R.id.action_duplicates
            ))
            views.setOnClickPendingIntent(R.id.action_recycle_bin, buildDeepLinkIntent(
                context, "app://com.cleanify/recycle_bin", R.id.action_recycle_bin
            ))

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun buildDeepLinkIntent(
        context: Context,
        uri: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
