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

            // Calculate the exact upcoming occurrence for this targeted day of the week
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.AM_PM, if (alarm.isAm) Calendar.AM else Calendar.PM)
                set(Calendar.HOUR, if (alarm.hour == 12) 0 else alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Explicitly force the target day of the week configuration
                set(Calendar.DAY_OF_WEEK, dayOfWeek)

                // Critical adjustment: If the targeted day calculation resolves to a time
                // that already ticked past earlier *this week*, push it exactly 7 days into the future.
                if (before(Calendar.getInstance())) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}