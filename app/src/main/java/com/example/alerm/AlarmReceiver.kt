package com.example.alerm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.alerm.data.AlarmDatabase
import com.example.alerm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    private val TAG = "AlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> handleBoot(context)
            "com.example.alerm.ALARM_TRIGGER" -> handleAlarmTrigger(context, intent)
        }
    }

    private fun handleBoot(context: Context) {
        Log.d(TAG, "Boot completed — rescheduling all alarms")
        val repository = AlarmRepository(AlarmDatabase.getDatabase(context).alarmDao())
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarms = repository.getEnabledAlarms()
                AlarmScheduler.scheduleAllAlarms(context, alarms)
                // Refresh widget
                AlarmWidgetProvider.updateAllWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleAlarmTrigger(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId == -1) {
            Log.e(TAG, "Received alarm trigger with no alarm_id")
            return
        }

        Log.d(TAG, "Alarm triggered: id=$alarmId")

        val repository = AlarmRepository(AlarmDatabase.getDatabase(context).alarmDao())
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = repository.getAlarmById(alarmId)
                if (alarm == null) {
                    Log.e(TAG, "Alarm $alarmId not found in DB")
                    pendingResult.finish()
                    return@launch
                }

                if (!alarm.isEnabled) {
                    Log.d(TAG, "Alarm $alarmId is disabled, skipping")
                    pendingResult.finish()
                    return@launch
                }

                // Start foreground service to play music
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("alarm_id", alarmId)
                    putExtra("alarm_label", alarm.label)
                    putExtra("music_uri", alarm.musicUri)
                    putExtra("alarm_hour", alarm.hour)
                    putExtra("alarm_minute", alarm.minute)
                    putExtra("headphone_only", alarm.headphoneOnly)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                // Reschedule for next week if it's a repeating alarm
                if (alarm.days != 0) {
                    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                    AlarmScheduler.rescheduleAlarmForDay(context, alarm, today)
                } else {
                    // One-shot alarm: disable it
                    repository.setEnabled(alarmId, false)
                    AlarmWidgetProvider.updateAllWidgets(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
