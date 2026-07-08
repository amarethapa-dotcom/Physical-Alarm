package com.gmail.amarethapa.physicalalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gmail.amarethapa.physicalalarm.data.room.AlarmEntity
import com.gmail.amarethapa.physicalalarm.data.room.AlarmRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        // Verify we actually received the correct boot action signal
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED
            || intent?.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {

            Log.d("BootReceiver", "Device reboot detected! Rescheduling alarms...")

            context?.let { ctx ->
                // 1. Tell the OS to keep this receiver alive for an async background job
                val pendingResult = goAsync()

                // 2. Launch a coroutine on a background thread pool (IO)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // 3. Initialize the database instance and fetch the absolute latest snapshot
                        val database = AlarmRoomDatabase.getDatabase(ctx)
                        val alarmDao = database.alarmDao()

                        // .first() reads the current database state once and drops the active connection pipeline
                        val allAlarms = alarmDao.getAllAlarmsFlow().first()

                        // 4. Loop through and reschedule only the alarms that were actively turned on
                        val alarmManager =
                            ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                        for (alarm in allAlarms) {
                            if (alarm.isEnabled) {
                                rescheduleAlarm(ctx, alarmManager, alarm)
                            }
                        }
                        Log.d("BootReceiver", "Successfully rescheduled active alarms.")
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "Failed to reschedule alarms", e)
                    } finally {
                        // 5. Must always call finish() to allow the OS to cleanly release this receiver resource
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun rescheduleAlarm(context: Context, alarmManager: AlarmManager, alarm: AlarmEntity) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Parse time details
        val timeParts = alarm.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].substring(0, 2).toInt()
        val isAm = alarm.time.contains("AM")

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

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}