package com.example.alerm

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.alerm.data.Alarm
import com.example.alerm.data.AlarmDatabase
import com.example.alerm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.example.alerm.WIDGET_TOGGLE"
        const val EXTRA_ALARM_ID = "widget_alarm_id"

        // Up to 5 alarm row IDs in the widget layout
        private val ROW_IDS = intArrayOf(
            R.id.widgetRow1, R.id.widgetRow2, R.id.widgetRow3,
            R.id.widgetRow4, R.id.widgetRow5
        )
        private val TIME_IDS = intArrayOf(
            R.id.widgetTime1, R.id.widgetTime2, R.id.widgetTime3,
            R.id.widgetTime4, R.id.widgetTime5
        )
        private val STATUS_IDS = intArrayOf(
            R.id.widgetStatus1, R.id.widgetStatus2, R.id.widgetStatus3,
            R.id.widgetStatus4, R.id.widgetStatus5
        )
        private val LABEL_IDS = intArrayOf(
            R.id.widgetLabel1, R.id.widgetLabel2, R.id.widgetLabel3,
            R.id.widgetLabel4, R.id.widgetLabel5
        )

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, AlarmWidgetProvider::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                val intent = Intent(context, AlarmWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = AlarmRepository(AlarmDatabase.getDatabase(context).alarmDao())
            val alarms = repository.allAlarms.first().take(5)
            for (widgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, widgetId, alarms)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE) {
            val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
            if (alarmId != -1) {
                handleToggle(context, alarmId)
            }
        }
    }

    private fun handleToggle(context: Context, alarmId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = AlarmRepository(AlarmDatabase.getDatabase(context).alarmDao())
            val alarm = repository.getAlarmById(alarmId) ?: return@launch
            val newEnabled = !alarm.isEnabled
            repository.setEnabled(alarmId, newEnabled)
            val updated = alarm.copy(isEnabled = newEnabled)
            if (newEnabled) {
                AlarmScheduler.scheduleAlarm(context, updated)
            } else {
                AlarmScheduler.cancelAlarm(context, updated)
            }
            updateAllWidgets(context)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        alarms: List<Alarm>
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_alarm)

        // Populate each row slot
        for (i in 0 until 5) {
            val alarm = alarms.getOrNull(i)
            if (alarm != null) {
                views.setViewVisibility(ROW_IDS[i], android.view.View.VISIBLE)
                views.setTextViewText(TIME_IDS[i], alarm.formattedTime())
                views.setTextViewText(STATUS_IDS[i], if (alarm.isEnabled) "ON" else "OFF")
                views.setTextViewText(
                    LABEL_IDS[i],
                    if (alarm.label.isBlank()) "Alarm" else alarm.label
                )

                // Toggle PendingIntent for this row
                val toggleIntent = Intent(context, AlarmWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_TOGGLE
                    putExtra(EXTRA_ALARM_ID, alarm.id)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarm.id,  // unique request code per alarm
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(ROW_IDS[i], pendingIntent)
            } else {
                views.setViewVisibility(ROW_IDS[i], android.view.View.GONE)
            }
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
