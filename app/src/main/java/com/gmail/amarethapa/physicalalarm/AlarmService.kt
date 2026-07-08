package com.gmail.amarethapa.physicalalarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AlarmService", "Alarm service started - Ringing!")

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
}