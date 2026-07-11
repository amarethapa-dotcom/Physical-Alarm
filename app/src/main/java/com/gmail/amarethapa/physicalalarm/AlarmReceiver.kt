package com.gmail.amarethapa.physicalalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gmail.amarethapa.physicalalarm.data.room.AlarmRoomDatabase
import com.gmail.amarethapa.physicalalarm.ui.DismissAlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val FULL_SCREEN_CHANNEL_ID = "alarm_fullscreen_channel"
        private const val FULL_SCREEN_NOTIFICATION_ID = 2
    }

    override fun onReceive(context: Context?, intent: Intent?) {

        Log.d("AlarmReceiver", "Alarm broadcast received! Starting foreground service...")

        context?.let { ctx ->

            // 1. Start the alarm service as a foreground service (required for Android 8+)
            val serviceIntent = Intent(ctx, AlarmService::class.java)
            ContextCompat.startForegroundService(ctx, serviceIntent)

            // 2. Launch the dismiss screen via full-screen intent notification
            //    (direct startActivity from a BroadcastReceiver is blocked on Android 12+)
            showFullScreenAlarmNotification(ctx)

            // 3. Re-schedule the alarm for its next occurrence (one-shot alarms don't repeat)
            val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
            if (alarmId != -1) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AlarmRoomDatabase.getDatabase(ctx).alarmDao()
                        val alarm = dao.getAlarmById(alarmId)
                        alarm?.let {
                            AlarmScheduler.scheduleNextOccurrence(ctx, it)
                        }
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver", "Failed to reschedule alarm", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun showFullScreenAlarmNotification(context: Context) {
        // Create notification channel for full-screen alarm alerts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FULL_SCREEN_CHANNEL_ID,
                "Alarm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen alarm alert notifications"
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Build the full-screen intent that launches DismissAlarmActivity
        val dismissIntent = Intent(context, DismissAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FULL_SCREEN_CHANNEL_ID)
            .setContentTitle("Alarm is ringing!")
            .setContentText("Tap to dismiss")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FULL_SCREEN_NOTIFICATION_ID, notification)
    }
}