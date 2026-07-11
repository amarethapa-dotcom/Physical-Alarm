package com.gmail.amarethapa.physicalalarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Checks if the app can schedule exact alarms (Required for Android 12+)
 */
fun Context.hasExactAlarmPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}

/**
 * Checks if the app can post notifications (Required for Android 13+)
 */
fun Context.hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

/**
 * Checks if the app has Activity Recognition permission (Required for Android 10+)
 */
fun Context.hasActivityRecognitionPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

/**
 * Checks if the app can use full screen intents (Required for Android 14+)
 */
fun Context.hasFullScreenIntentPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.canUseFullScreenIntent()
    } else {
        true
    }
}

fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:$packageName".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}

fun Context.openFullScreenIntentSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = "package:$packageName".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}

fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:$packageName".toUri()
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
}

@Composable
fun PermissionGuardWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasExactAlarm by remember { mutableStateOf(context.hasExactAlarmPermission()) }
    var hasNotification by remember { mutableStateOf(context.hasNotificationPermission()) }
    var hasActivityRec by remember { mutableStateOf(context.hasActivityRecognitionPermission()) }
    var hasFullScreenIntent by remember { mutableStateOf(context.hasFullScreenIntentPermission()) }

    // Launcher for runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotification = results[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotification
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasActivityRec = results[Manifest.permission.ACTIVITY_RECOGNITION] ?: hasActivityRec
        }
    }

    // Refresh permissions when returning to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasExactAlarm = context.hasExactAlarmPermission()
                hasNotification = context.hasNotificationPermission()
                hasActivityRec = context.hasActivityRecognitionPermission()
                hasFullScreenIntent = context.hasFullScreenIntentPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Initial check/request
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotification) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityRec) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    when {
        !hasExactAlarm -> {
            PermissionPrompt(
                title = "Exact Alarm Required",
                description = "To wake you up exactly on time, please allow this app to set precise alarms in system settings.",
                buttonText = "Grant Exact Alarm Permission",
                onClick = { context.openExactAlarmSettings() }
            )
        }

        !hasNotification -> {
            PermissionPrompt(
                title = "Notifications Required",
                description = "We need to show notifications so you can see and dismiss your alarms.",
                buttonText = "Grant Notification Permission",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    } else {
                        context.openAppSettings()
                    }
                }
            )
        }

        !hasActivityRec -> {
            PermissionPrompt(
                title = "Motion Detection Required",
                description = "To detect your steps and turn off the alarm, we need Activity Recognition permission.",
                buttonText = "Grant Motion Permission",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))
                    }
                }
            )
        }

        !hasFullScreenIntent -> {
            PermissionPrompt(
                title = "Full Screen Alarms Required",
                description = "To show the alarm screen while your phone is locked, please allow full-screen intents.",
                buttonText = "Grant Full Screen Permission",
                onClick = { context.openFullScreenIntentSettings() }
            )
        }

        else -> {
            content()
        }
    }
}

@Composable
fun PermissionPrompt(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            modifier = Modifier.padding(vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Button(onClick = onClick) {
            Text(buttonText)
        }
    }
}
