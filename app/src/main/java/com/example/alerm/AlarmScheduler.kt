package com.example.alerm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.alerm.data.Alarm
import java.util.Calendar

object AlarmScheduler {

    private const val REQUEST_CODE_BASE = 10000

    /**
     * Schedule all enabled days for a given alarm.
     * Each day gets its own PendingIntent keyed by (alarmId * 10 + dayIndex).
     */
    fun scheduleAlarm(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (alarm.days == 0) {
            // One-time alarm: schedule for next occurrence of hour:minute
            val triggerTime = nextOccurrenceOneShotMs(alarm.hour, alarm.minute)
            val pi = buildPendingIntent(context, alarm, requestCode = alarm.id * 10)
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pi),
                pi
            )
        } else {
            // Repeating: schedule for each enabled day of the week
            Alarm.DAY_BITS.forEachIndexed { index, bit ->
                if (alarm.hasDayEnabled(bit)) {
                    val calDay = bitToCalendarDay(bit)
                    val triggerTime = nextOccurrenceForDay(calDay, alarm.hour, alarm.minute)
                    val pi = buildPendingIntent(context, alarm, requestCode = alarm.id * 10 + index + 1)
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerTime, pi),
                        pi
                    )
                } else {
                    // Cancel any previously scheduled alarm for this day
                    val pi = buildPendingIntentNoCreate(context, alarm, requestCode = alarm.id * 10 + index + 1)
                    pi?.let { alarmManager.cancel(it) }
                }
            }
        }
    }

    fun cancelAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel one-shot
        val piOneShot = buildPendingIntentNoCreate(context, alarm, requestCode = alarm.id * 10)
        piOneShot?.let { alarmManager.cancel(it) }

        // Cancel all day-specific
        Alarm.DAY_BITS.forEachIndexed { index, _ ->
            val pi = buildPendingIntentNoCreate(context, alarm, requestCode = alarm.id * 10 + index + 1)
            pi?.let { alarmManager.cancel(it) }
        }
    }

    /**
     * Reschedule the next occurrence for a weekly alarm after it fires.
     * Called from AlarmReceiver to set up the same day next week.
     */
    fun rescheduleAlarmForDay(context: Context, alarm: Alarm, calendarDay: Int) {
        if (!alarm.isEnabled || !alarm.hasDayEnabled(Alarm.calendarDayToBit(calendarDay))) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val dayIndex = calendarDayToIndex(calendarDay)
        val triggerTime = nextOccurrenceForDay(calendarDay, alarm.hour, alarm.minute, mustBeFuture = true)
        val pi = buildPendingIntent(context, alarm, requestCode = alarm.id * 10 + dayIndex + 1)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerTime, pi),
            pi
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildPendingIntent(context: Context, alarm: Alarm, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.alerm.ALARM_TRIGGER"
            putExtra("alarm_id", alarm.id)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildPendingIntentNoCreate(context: Context, alarm: Alarm, requestCode: Int): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.alerm.ALARM_TRIGGER"
            putExtra("alarm_id", alarm.id)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Returns the next millisecond timestamp for [hour]:[minute] on a specific
     * Calendar day-of-week. If today is that day but the time already passed,
     * returns next week.
     */
    private fun nextOccurrenceForDay(
        calendarDay: Int,
        hour: Int,
        minute: Int,
        mustBeFuture: Boolean = false
    ): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, calendarDay)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If target is in the past (or we need strictly future), add 7 days
        if (target.timeInMillis <= now.timeInMillis || mustBeFuture) {
            target.add(Calendar.DAY_OF_YEAR, 7)
        }
        return target.timeInMillis
    }

    /** For a one-shot alarm with no day set, schedule for the next occurrence of hour:minute */
    private fun nextOccurrenceOneShotMs(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun bitToCalendarDay(bit: Int): Int = when (bit) {
        Alarm.MON -> Calendar.MONDAY
        Alarm.TUE -> Calendar.TUESDAY
        Alarm.WED -> Calendar.WEDNESDAY
        Alarm.THU -> Calendar.THURSDAY
        Alarm.FRI -> Calendar.FRIDAY
        Alarm.SAT -> Calendar.SATURDAY
        Alarm.SUN -> Calendar.SUNDAY
        else -> Calendar.MONDAY
    }

    private fun calendarDayToIndex(calDay: Int): Int = when (calDay) {
        Calendar.MONDAY    -> 0
        Calendar.TUESDAY   -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY  -> 3
        Calendar.FRIDAY    -> 4
        Calendar.SATURDAY  -> 5
        Calendar.SUNDAY    -> 6
        else -> 0
    }

    fun scheduleAllAlarms(context: Context, alarms: List<Alarm>) {
        alarms.forEach { scheduleAlarm(context, it) }
    }
}
