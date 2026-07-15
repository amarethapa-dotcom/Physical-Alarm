package com.gmail.amarethapa.physicalalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmService : Service(), SensorEventListener {

    companion object {
        const val ACTION_STEP_UPDATE = "com.gmail.amarethapa.physicalalarm.STEP_UPDATE"
        const val ACTION_ALARM_DISMISSED = "com.gmail.amarethapa.physicalalarm.ALARM_DISMISSED"
        const val EXTRA_REMAINING_STEPS = "remaining_steps"
    }

    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "alarm_channel"

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private lateinit var sensorManager: SensorManager
    private var stepDetectorSensor: Sensor? = null

    private var stepsWalked = 0
    private val TARGET_STEPS = 20 // Number of steps required to turn off alarm

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Plays alarm sound and vibrates when the physical alarm triggers."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startServiceAsForeground() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Physical Alarm Ringing")
            .setContentText("Walk 20 steps to dismiss this alarm.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize the hardware sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        // Start listening immediately when the alarm triggers
        stepDetectorSensor?.let {
            sensorManager.registerListener(
                this, it,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AlarmService", "Alarm service started - Ringing!")
        startServiceAsForeground()

        // 1. Initialize Audio Playback (Using default alarm sound)
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, alarmUri)

            // Critical: Force audio through the Alarm stream so it respects alarm volume settings
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            isLooping = true
            prepare()
            start()
        }

        // 2. Initialize Hardware Vibration (Modern API aware check)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Vibrate repeatedly in a 1-second on / 1-second off pattern
        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    pattern,
                    0
                )
            ) // 0 means repeat indefinitely
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }

        // START_STICKY tells the OS to recreate this service if it gets killed due to low memory
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AlarmService", "Alarm service stopped - Cleaning up.")

        // 3. Clean up resources to prevent hardware battery drain/leaks
        mediaPlayer?.stop()
        mediaPlayer?.release()
        vibrator?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            stepsWalked++

            // Broadcast the current step count to update your UI screen
            updateUiWithRemainingSteps(TARGET_STEPS - stepsWalked)

            if (stepsWalked >= TARGET_STEPS) {
                stopAlarmAndDestroyService()
            }
        }
    }

    private fun updateUiWithRemainingSteps(remainingSteps: Int) {
        val intent = Intent(ACTION_STEP_UPDATE).apply {
            putExtra(EXTRA_REMAINING_STEPS, remainingSteps)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun stopAlarmAndDestroyService() {
        sensorManager.unregisterListener(this)
        sendBroadcast(Intent(ACTION_ALARM_DISMISSED).apply { setPackage(packageName) })
        stopSelf()
    }
}