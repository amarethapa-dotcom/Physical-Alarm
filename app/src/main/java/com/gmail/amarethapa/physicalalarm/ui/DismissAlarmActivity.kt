package com.gmail.amarethapa.physicalalarm.ui

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gmail.amarethapa.physicalalarm.AlarmService
import com.gmail.amarethapa.physicalalarm.ui.theme.PhysicalAlarmTheme
import kotlinx.coroutines.delay

class DismissAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Force the system hardware to turn the screen on and bypass lock restrictions
        turnScreenOnAndShowOnLockScreen()

        setContent {
            PhysicalAlarmTheme {
                var remainingSteps by remember { mutableStateOf(20) } // Default TARGET_STEPS
                val context = LocalContext.current

                // Long-press state for emergency dismiss
                var isLongPressing by remember { mutableStateOf(false) }
                var holdProgress by remember { mutableFloatStateOf(0f) }
                val holdDurationMs = 3000L // 3 seconds

                // Animate progress while long-pressing
                LaunchedEffect(isLongPressing) {
                    if (isLongPressing) {
                        holdProgress = 0f
                        val startTime = System.currentTimeMillis()
                        while (isLongPressing && holdProgress < 1f) {
                            delay(50)
                            val elapsed = System.currentTimeMillis() - startTime
                            holdProgress = (elapsed.toFloat() / holdDurationMs).coerceAtMost(1f)
                            if (holdProgress >= 1f) {
                                // Long-press completed - dismiss
                                stopService(Intent(this@DismissAlarmActivity, AlarmService::class.java))
                                finish()
                            }
                        }
                    } else {
                        holdProgress = 0f
                    }
                }

                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            when (intent?.action) {
                                AlarmService.ACTION_STEP_UPDATE -> {
                                    remainingSteps = intent.getIntExtra(AlarmService.EXTRA_REMAINING_STEPS, 20)
                                }
                                AlarmService.ACTION_ALARM_DISMISSED -> {
                                    finish()
                                }
                            }
                        }
                    }
                    val filter = IntentFilter().apply {
                        addAction(AlarmService.ACTION_STEP_UPDATE)
                        addAction(AlarmService.ACTION_ALARM_DISMISSED)
                    }
                    ContextCompat.registerReceiver(
                        context,
                        receiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    onDispose {
                        context.unregisterReceiver(receiver)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.errorContainer // Eye-catching alert background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ALARM IS RINGING!",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Walk to turn it off",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "$remainingSteps",
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "STEPS REMAINING",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(64.dp))

                        // Emergency dismiss - requires 3-second long-press to prevent accidental bypass
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isLongPressing = true
                                            tryAwaitRelease()
                                            isLongPressing = false
                                        }
                                    )
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(
                                    alpha = 0.3f + (0.7f * holdProgress)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (holdProgress > 0f && holdProgress < 1f)
                                        "HOLD TO DISMISS... ${((1f - holdProgress) * 3).toInt() + 1}s"
                                    else
                                        "HOLD TO EMERGENCY DISMISS",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onError,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun turnScreenOnAndShowOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Dismiss the standard keyguard lock programmatically if it isn't password protected
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }
}