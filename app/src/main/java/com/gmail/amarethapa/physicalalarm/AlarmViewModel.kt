package com.gmail.amarethapa.physicalalarm

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.amarethapa.physicalalarm.data.models.Alarm
import com.gmail.amarethapa.physicalalarm.data.room.AlarmEntity
import com.gmail.amarethapa.physicalalarm.data.room.AlarmRoomDatabase
import com.gmail.amarethapa.physicalalarm.data.room.toExternal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 1. A production-ready ViewModel representation
class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    // A custom property getter. Every time you type 'context',
    // it simply reads live from the parent class parameter.
    private val context: Context get() = getApplication<Application>().applicationContext

    private val alarmDao = AlarmRoomDatabase.getDatabase(context).alarmDao()

    // 1. Transform the Room cold Flow into a hot production UI StateFlow pipeline
    val alarms: StateFlow<List<Alarm>> = alarmDao.getAllAlarmsFlow()
        .map { entities -> entities.map { it.toExternal() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveNewAlarm(
        hour: Int,
        minute: Int,
        isAm: Boolean,
        repeatDays: Set<Int>,
        isEnabled: Boolean
    ) {
        viewModelScope.launch {
            val newAlarm = AlarmEntity(
                hour = hour, minute = minute, isAm = isAm,
                repeatDays = repeatDays,
                isEnabled = isEnabled
            )

            val generatedId = alarmDao.insertOrUpdateAlarm(newAlarm).toInt()
            val scheduledAlarm = newAlarm.copy(id = generatedId)
            AlarmScheduler.updateAlarmSchedule(context, scheduledAlarm)
        }
    }

    fun toggleAlarm(alarmId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            val entity = alarmDao.getAlarmById(alarmId)
            entity?.let {
                val updatedAlarm = it.copy(isEnabled = isEnabled)
                alarmDao.insertOrUpdateAlarm(updatedAlarm)
                AlarmScheduler.updateAlarmSchedule(context, updatedAlarm)
            }
        }
    }

    fun deleteAlarm(alarmId: Int) {
        viewModelScope.launch {
            val entity = alarmDao.getAlarmById(alarmId)
            entity?.let {
                // 1. Cancel the OS AlarmManager window first
                AlarmScheduler.updateAlarmSchedule(context, it.copy(isEnabled = false))
                // 2. Remove from database
                alarmDao.deleteAlarm(it)
            }
        }
    }

    // Call this function when the user presses a "Dismiss Alarm" button in your Compose UI
    fun dismissActiveRingtone() {
        val serviceIntent = Intent(context, AlarmService::class.java)
        context.stopService(serviceIntent)
    }
}