package com.example.alerm.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val dao: AlarmDao) {

    val allAlarms: Flow<List<Alarm>> = dao.getAllAlarms()

    suspend fun getAlarmById(id: Int): Alarm? = dao.getAlarmById(id)

    suspend fun getEnabledAlarms(): List<Alarm> = dao.getEnabledAlarms()

    suspend fun insertAlarm(alarm: Alarm): Long = dao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: Alarm) = dao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: Alarm) = dao.deleteAlarm(alarm)

    suspend fun deleteAlarmById(id: Int) = dao.deleteAlarmById(id)

    suspend fun setEnabled(id: Int, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun getAlarmCount(): Int = dao.getAlarmCount()
}
