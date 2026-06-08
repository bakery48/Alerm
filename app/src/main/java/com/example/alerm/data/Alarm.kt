package com.example.alerm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Days bitmask constants:
 * Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
 */
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    /** Bitmask of days: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64 */
    val days: Int = 0,
    val isEnabled: Boolean = true,
    val musicUri: String? = null
) {
    companion object {
        const val MON = 1
        const val TUE = 2
        const val WED = 4
        const val THU = 8
        const val FRI = 16
        const val SAT = 32
        const val SUN = 64

        val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val DAY_BITS = listOf(MON, TUE, WED, THU, FRI, SAT, SUN)

        /** Map from Java Calendar.DAY_OF_WEEK (1=Sun..7=Sat) to bitmask */
        fun calendarDayToBit(calDay: Int): Int = when (calDay) {
            java.util.Calendar.SUNDAY    -> SUN
            java.util.Calendar.MONDAY    -> MON
            java.util.Calendar.TUESDAY   -> TUE
            java.util.Calendar.WEDNESDAY -> WED
            java.util.Calendar.THURSDAY  -> THU
            java.util.Calendar.FRIDAY    -> FRI
            java.util.Calendar.SATURDAY  -> SAT
            else -> 0
        }
    }

    fun hasDayEnabled(bit: Int) = (days and bit) != 0

    fun activeDaysLabel(): String {
        if (days == 0) return "One-time"
        val all = DAY_BITS.zip(DAY_LABELS)
        val active = all.filter { (bit, _) -> (days and bit) != 0 }.map { it.second }
        if (active.size == 7) return "Every day"
        if (active == listOf("Mon", "Tue", "Wed", "Thu", "Fri")) return "Weekdays"
        if (active == listOf("Sat", "Sun")) return "Weekend"
        return active.joinToString(", ")
    }

    fun formattedTime(): String = "%02d:%02d".format(hour, minute)
}
