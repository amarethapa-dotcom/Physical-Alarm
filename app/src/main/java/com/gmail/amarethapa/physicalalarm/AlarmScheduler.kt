package com.gmail.amarethapa.physicalalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.gmail.amarethapa.physicalalarm.data.room.AlarmEntity
import java.util.Calendar

object AlarmScheduler {

    fun updateAlarmSchedule(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // We loop through all 7 calendar tracking fields
        for (dayOfWeek in 1..7) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("ALARM_ID", alarm.id)
            }

            // A unique identifier math signature isolates this specific weekday channel
            val uniqueRequestCode = (alarm.id * 10) + dayOfWeek

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueRequestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // If the entire alarm is toggled off, or this specific weekday is unchecked, cancel it
            if (!alarm.isEnabled || !alarm.repeatDays.contains(dayOfWeek)) {
                alarmManager.cancel(pendingIntent)
                continue
            }

            val triggerTime = calculateNextTriggerTime(alarm, dayOfWeek)

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /**
     * Re-schedules a single alarm for its next occurrence after it fires.
     * Called from AlarmReceiver after the alarm triggers.
     */
    fun scheduleNextOccurrence(context: Context, alarm: AlarmEntity) {
        if (!alarm.isEnabled || alarm.repeatDays.isEmpty()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (dayOfWeek in alarm.repeatDays) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("ALARM_ID", alarm.id)
            }

            val uniqueRequestCode = (alarm.id * 10) + dayOfWeek

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueRequestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Always schedule for next week since the alarm just fired
            val triggerTime = calculateNextTriggerTime(alarm, dayOfWeek)

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /**
     * Calculates the next trigger time for a given alarm on a specific day of the week.
     * Uses explicit day offset calculation to avoid locale-dependent Calendar.set(DAY_OF_WEEK) issues.
     */
    private fun calculateNextTriggerTime(alarm: AlarmEntity, targetDayOfWeek: Int): Long {
        val now = Calendar.getInstance()

        val alarmTime = Calendar.getInstance().apply {
            set(Calendar.AM_PM, if (alarm.isAm) Calendar.AM else Calendar.PM)
            set(Calendar.HOUR, if (alarm.hour == 12) 0 else alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Calculate how many days from today until the target day
        val todayDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        var daysUntilTarget = targetDayOfWeek - todayDayOfWeek
        if (daysUntilTarget < 0) {
            daysUntilTarget += 7
        }

        // If target day is today but the time has already passed, push to next week
        if (daysUntilTarget == 0 && alarmTime.timeInMillis <= now.timeInMillis) {
            daysUntilTarget = 7
        }

        alarmTime.add(Calendar.DAY_OF_YEAR, daysUntilTarget)

        return alarmTime.timeInMillis
    }
}