package com.example.alerm

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.alerm.data.Alarm
import com.example.alerm.data.AlarmDatabase
import com.example.alerm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmSingleWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.example.alerm.SINGLE_WIDGET_TOGGLE"
        const val EXTRA_WIDGET_ID = "single_widget_id"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, alarm: Alarm?) {
            val views = RemoteViews(context.packageName, R.layout.widget_alarm_single)

            if (alarm == null) {
                views.setTextViewText(R.id.swTime, "--:--")
                views.setTextViewText(R.id.swLabel, "未設定")
                views.setTextViewText(R.id.swStatus, "---")
            } else {
                views.setTextViewText(R.id.swTime, alarm.formattedTime())
                views.setTextViewText(R.id.swLabel, alarm.label.ifBlank { "Alarm" })
                views.setTextViewText(R.id.swStatus, if (alarm.isEnabled) "ON" else "OFF")

                val toggleIntent = Intent(context, AlarmSingleWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE
                    putExtra(EXTRA_WIDGET_ID, widgetId)
                }
                val pi = PendingIntent.getBroadcast(
                    context, widgetId, toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.swStatus, pi)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, AlarmSingleWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                val repo = AlarmRepository(AlarmDatabase.getDatabase(context).alarmDao())
                val alarms = repo.allAlarms.first()
                for (id in ids) {
                    val alarmId = WidgetPrefs.getAlarmId(context, id)
                    val alarm = alarms.find { it.id == alarmId }
                    updateWidget(context, manager, id, alarm)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            val repo = AlarmRepository(AlarmDatabase.getDatabase(context).alarmDao())
            val alarms = repo.allAlarms.first()
            for (id in appWidgetIds) {
                val alarmId = WidgetPrefs.getAlarmId(context, id)
                val alarm = alarms.find { it.id == alarmId }
                updateWidget(context, appWidgetManager, id, alarm)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
            if (widgetId != -1) handleToggle(context, widgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPrefs.remove(context, it) }
    }

    private fun handleToggle(context: Context, widgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val alarmId = WidgetPrefs.getAlarmId(context, widgetId)
            if (alarmId == -1) return@launch
            val repo = AlarmRepository(AlarmDatabase.getDatabase(context).alarmDao())
            val alarm = repo.getAlarmById(alarmId) ?: return@launch
            val newEnabled = !alarm.isEnabled
            repo.setEnabled(alarmId, newEnabled)
            val updated = alarm.copy(isEnabled = newEnabled)
            if (newEnabled) AlarmScheduler.scheduleAlarm(context, updated)
            else AlarmScheduler.cancelAlarm(context, updated)
            refreshAll(context)
            AlarmWidgetProvider.updateAllWidgets(context)
        }
    }
}
