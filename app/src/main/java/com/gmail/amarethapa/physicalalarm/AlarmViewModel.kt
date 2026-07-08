package com.gmail.amarethapa.physicalalarm

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.amarethapa.physicalalarm.data.models.Alarm
import com.gmail.amarethapa.physicalalarm.data.room.AlarmEntity
import com.gmail.amarethapa.physicalalarm.data.room.AlarmRoomDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

// 1. A production-ready ViewModel representation
class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    // A custom property getter. Every time you type 'context',
    // it simply reads live from the parent class parameter.
    private val context: Context get() = getApplication<Application>().applicationContext

    // This is mutable internally. Only the ViewModel can pump updates into it.
    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())

    private val alarmDao = AlarmRoomDatabase.getDatabase(context).alarmDao()

    // 1. Transform the Room cold Flow into a hot production UI StateFlow pipeline
    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.getAllAlarmsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Drop subscription to save battery if screen closes
            initialValue = emptyList()
        )

    init {
        loadAlarms()
    }

    fun saveNewAlarm(
        timeStr: String, daysStr: String, hour: Int, minute: Int, isAm: Boolean,
        isEnabled: Boolean
    ) {

        viewModelScope.launch {

            val newAlarm = AlarmEntity(time = timeStr, days = daysStr, isEnabled = true)

            alarmDao.insertOrUpdateAlarm(newAlarm)

            scheduleSystemAlarm(newAlarm.id, hour, minute, isAm, isEnabled)
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            // Update the SQLite record state natively using data copy logic
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            alarmDao.insertOrUpdateAlarm(updatedAlarm)

            // Extract values and trigger your AlarmManager execution rules...
            val timeParts = alarm.time.split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].substring(0, 2).toInt()
            val isAm = alarm.time.contains("AM")

            scheduleSystemAlarm(alarm.id, hour, minute, isAm, isEnabled)
        }
    }

    private fun loadAlarms() {
        // Simulating loading data from a repository or database
        _alarms.value = listOf(
            Alarm(1, "06:30 AM", "Mon, Tue, Wed, Thu, Fri", true),
            Alarm(2, "09:00 AM", "Sat, Sun", false),
            Alarm(3, "10:15 PM", "Every day", true)
        )
    }

    fun toggleAlarm(alarmId: Int, isEnabled: Boolean) {
        // Find the alarm details from your list state
        val alarm = _alarms.value.find { it.id == alarmId } ?: return

        // Update local list state
        _alarms.update { list ->
            list.map { if (it.id == alarmId) it.copy(isEnabled = isEnabled) else it }
        }

        // Parse your time format safely to integer blocks for the system scheduler
        // Example assumes "06:30 AM" format split logic
        val timeParts = alarm.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].substring(0, 2).toInt()
        val isAm = alarm.time.contains("AM")

        // 3. Trigger your system execution layer safely
        scheduleSystemAlarm(alarmId, hour, minute, isAm, isEnabled)
    }

    fun scheduleSystemAlarm(
        alarmId: Int,
        hour: Int,
        minute: Int,
        isAm: Boolean,
        isEnabled: Boolean
    ) {
        // 1. Fetch the System Service using Kotlin's type-safe casting
        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        // 2. Create the Intent directed explicitly at our receiver class
        val intent = Intent(
            context,
            AlarmReceiver::class.java
        ).apply {
            putExtra("ALARM_ID", alarmId)
        }

        // 3. Wrap it in a PendingIntent so the OS can execute it on our behalf
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId, // Unique ID prevents alarms from overwriting each other
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (isEnabled) {
            // 4. Calculate the exact time target using a Calendar instance

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()

                // 1. Explicitly set the AM or PM phase (0 for AM, 1 for PM)
                set(Calendar.AM_PM, if (isAm) Calendar.AM else Calendar.PM)

                // 2. Set the 12-hour value (0-11).
                // Note: If hour is 12, Calendar.HOUR expects 0 (midnight/noon)
                set(Calendar.HOUR, if (hour == 12) 0 else hour)

                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If the time already passed today, roll it over to tomorrow
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            // 5. Tell the OS to wake up the phone precisely, even in low-power Doze mode
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            // 6. If the user toggled the alarm off, cancel it completely out of the OS pool
            alarmManager.cancel(pendingIntent)
        }
    }

    // Call this function when the user presses a "Dismiss Alarm" button in your Compose UI
    fun dismissActiveRingtone() {
        val serviceIntent = Intent(context, AlarmService::class.java)
        context.stopService(serviceIntent)
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            // 1. Cancel the OS AlarmManager window first so it stops ringing in the background
            scheduleSystemAlarm(alarm.id, 0, 0, false, isEnabled = false)

            // 2. Remove the row permanently from your Room SQLite table
            alarmDao.deleteAlarm(alarm)
        }
    }
}