package io.nekohasekai.sagernet.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class OneBoxWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.OneBox.app.widget.ACTION_TOGGLE"

        fun updateWidgets(context: Context) {
            OneBoxWidgetHelper.updateAllWidgets(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE) {
            OneBoxWidgetHelper.handleToggle(context)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        OneBoxWidgetHelper.updateAllWidgets(context)
    }
}
