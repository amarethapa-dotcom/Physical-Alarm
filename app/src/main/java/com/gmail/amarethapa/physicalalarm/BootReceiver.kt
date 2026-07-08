package com.gmail.amarethapa.physicalalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gmail.amarethapa.physicalalarm.data.room.AlarmRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

                        for (alarm in allAlarms) {
                            if (alarm.isEnabled) {
                                // Use the shared scheduler utility
                                AlarmScheduler.updateAlarmSchedule(ctx, alarm)
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
}