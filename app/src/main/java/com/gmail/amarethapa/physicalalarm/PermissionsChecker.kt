package com.gmail.amarethapa.physicalalarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

fun Context.hasExactAlarmPermission(): Boolean {
    // Exact alarm permissions were introduced in Android 12 (API 31)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else {
        // Older versions of Android grant this automatically by default
        true
    }
}

fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            // Target your exact application ID package cleanly
            data = "package:$packageName".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}

@Composable
fun PermissionGuardWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Track whether the permission is currently granted
    var isPermissionGranted by remember { mutableStateOf(context.hasExactAlarmPermission()) }

    // Use a Side Effect listener to re-verify the permission status
    // whenever the user comes back to the application from Settings
    LaunchedEffect(Unit) {
        // This runs once when mounting, but you can trigger check updates on lifecycle events
        isPermissionGranted = context.hasExactAlarmPermission()
    }

    if (isPermissionGranted) {
        // If everything is fine, render the actual alarm application layout code seamlessly
        content()
    } else {
        // If blocked, render a prominent full-screen warning prompt instead
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Exact Alarm Permission Required",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "To wake you up exactly on time, Android requires explicit permission to schedule precise system events. Please enable it in the system settings.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Button(
                onClick = { context.openExactAlarmSettings() }
            ) {
                Text("Open Settings")
            }
        }
    }
}