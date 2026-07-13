package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.R
import java.util.Locale

object WidgetUtils {
    fun formatElapsedTime(diffMs: Long, lang: String): String {
        if (diffMs <= 0) return "0h 0m"
        val totalMins = diffMs / 60000
        val totalHours = totalMins / 60
        val days = totalHours / 24
        val hours = totalHours % 24
        val mins = totalMins % 60

        val dSuffix = when (lang) {
            "it" -> "g"
            "es" -> "d"
            "fr" -> "j"
            else -> "d"
        }

        return if (days > 0) {
            "${days}${dSuffix} ${hours}h"
        } else {
            "${hours}h ${mins}m"
        }
    }

    fun getLang(context: Context): String {
        val prefs = context.getSharedPreferences("stop_stay_healthy_prefs", Context.MODE_PRIVATE)
        val selected = prefs.getString("language", "system") ?: "system"
        if (selected != "system") return selected
        val systemLang = Locale.getDefault().language
        return if (systemLang in listOf("en", "it", "es", "fr")) systemLang else "en"
    }

    fun triggerUpdate(context: Context) {
        val intentFasting = android.content.Intent(context, FastingWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                android.content.ComponentName(context, FastingWidgetProvider::class.java)
            )
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intentFasting)

        val intentSmoking = android.content.Intent(context, SmokingWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                android.content.ComponentName(context, SmokingWidgetProvider::class.java)
            )
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intentSmoking)

        val intentAlcohol = android.content.Intent(context, AlcoholWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                android.content.ComponentName(context, AlcoholWidgetProvider::class.java)
            )
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intentAlcohol)

        val intentSugar = android.content.Intent(context, SugarWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                android.content.ComponentName(context, SugarWidgetProvider::class.java)
            )
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intentSugar)
    }
}

class FastingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("stop_stay_healthy_prefs", Context.MODE_PRIVATE)
        val startTime = prefs.getLong("fasting_start_time", -1L)
        val lang = WidgetUtils.getLang(context)

        val timeStr = if (startTime == -1L) {
            when (lang) {
                "it" -> "Inattivo"
                "es" -> "Inactivo"
                "fr" -> "Inactif"
                else -> "Inactive"
            }
        } else {
            val diff = System.currentTimeMillis() - startTime
            WidgetUtils.formatElapsedTime(diff, lang)
        }

        val title = when (lang) {
            "it" -> "Digiuno Intermittente"
            "es" -> "Ayuno Intermitente"
            "fr" -> "Jeûne Intermittent"
            else -> "Intermittent Fasting"
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_fasting)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_time, timeStr)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class SmokingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("stop_stay_healthy_prefs", Context.MODE_PRIVATE)
        val startTime = prefs.getLong("smoking_start_time", -1L)
        val enabled = prefs.getBoolean("smoking_enabled", false)
        val lang = WidgetUtils.getLang(context)

        val timeStr = if (!enabled || startTime == -1L) {
            when (lang) {
                "it" -> "Disattivato"
                "es" -> "Desactivado"
                "fr" -> "Désactivé"
                else -> "Disabled"
            }
        } else {
            val diff = System.currentTimeMillis() - startTime
            WidgetUtils.formatElapsedTime(diff, lang)
        }

        val title = when (lang) {
            "it" -> "Senza Fumo"
            "es" -> "Sin Humo"
            "fr" -> "Sans Tabac"
            else -> "Smoke Free"
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_smoking)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_time, timeStr)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class AlcoholWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("stop_stay_healthy_prefs", Context.MODE_PRIVATE)
        val startTime = prefs.getLong("alcohol_start_time", -1L)
        val enabled = prefs.getBoolean("alcohol_enabled", false)
        val lang = WidgetUtils.getLang(context)

        val timeStr = if (!enabled || startTime == -1L) {
            when (lang) {
                "it" -> "Disattivato"
                "es" -> "Desactivado"
                "fr" -> "Désactivé"
                else -> "Disabled"
            }
        } else {
            val diff = System.currentTimeMillis() - startTime
            WidgetUtils.formatElapsedTime(diff, lang)
        }

        val title = when (lang) {
            "it" -> "Senza Alcol"
            "es" -> "Sin Alcohol"
            "fr" -> "Sans Alcool"
            else -> "Alcohol Free"
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_alcohol)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_time, timeStr)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class SugarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("stop_stay_healthy_prefs", Context.MODE_PRIVATE)
        val startTime = prefs.getLong("sugar_start_time", -1L)
        val enabled = prefs.getBoolean("sugar_enabled", false)
        val lang = WidgetUtils.getLang(context)

        val timeStr = if (!enabled || startTime == -1L) {
            when (lang) {
                "it" -> "Disattivato"
                "es" -> "Desactivado"
                "fr" -> "Désactivé"
                else -> "Disabled"
            }
        } else {
            val diff = System.currentTimeMillis() - startTime
            WidgetUtils.formatElapsedTime(diff, lang)
        }

        val title = when (lang) {
            "it" -> "Senza Zucchero"
            "es" -> "Sin Azúcar"
            "fr" -> "Sans Sucre"
            else -> "Sugar Free"
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_sugar)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_time, timeStr)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
