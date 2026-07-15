package com.gmail.amarethapa.physicalalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.gmail.amarethapa.physicalalarm.ui.DismissAlarmActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        Log.d("AlarmReceiver", "Alarm broadcast received! Starting background service...")

        context?.let { ctx ->

            // 1. Start background music playback
            val serviceIntent = Intent(ctx, AlarmService::class.java)
            ContextCompat.startForegroundService(ctx, serviceIntent)

            // 2. Launch the full screen overlay display interface
            val overlayIntent = Intent(ctx, DismissAlarmActivity::class.java).apply {
                // Critical flags: Tells Android to launch this as a standalone independent window layer
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            ctx.startActivity(overlayIntent)
        }
    }
}