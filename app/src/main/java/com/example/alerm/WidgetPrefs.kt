package com.example.alerm

import android.content.Context

object WidgetPrefs {
    private const val PREFS_NAME = "single_widget_prefs"
    private const val KEY_PREFIX = "alarm_id_for_widget_"

    fun setAlarmId(context: Context, widgetId: Int, alarmId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_PREFIX + widgetId, alarmId).apply()
    }

    fun getAlarmId(context: Context, widgetId: Int): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_PREFIX + widgetId, -1)

    fun remove(context: Context, widgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_PREFIX + widgetId).apply()
    }
}
