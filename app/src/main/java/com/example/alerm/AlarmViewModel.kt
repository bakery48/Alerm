package com.example.alerm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alerm.data.Alarm
import com.example.alerm.data.AlarmDatabase
import com.example.alerm.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository = AlarmRepository(
        AlarmDatabase.getDatabase(application).alarmDao()
    )

    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(alarm.id, enabled)
            val updated = alarm.copy(isEnabled = enabled)
            if (enabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), updated)
            }
            AlarmWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), alarm)
            repository.deleteAlarm(alarm)
            AlarmWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    suspend fun getAlarmCount(): Int = repository.getAlarmCount()
}
