package com.gmail.amarethapa.physicalalarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gmail.amarethapa.physicalalarm.ui.theme.PhysicalAlarmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhysicalAlarmTheme {
                MainAppEntry()
            }
        }
    }
}

@Composable
fun MainAppEntry() {
    PermissionGuardWrapper {
        // This entire dashboard UI layer is fully protected now!
        // It won't compile or run unless exact alarm permissions are verified active.
        MainAlarmDashboardScreen()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PhysicalAlarmTheme {
        Greeting("Hello Android")
    }
}

@Composable
fun AlarmItemRow(
    time: String,
    repeatDays: String,
    initialIsChecked: Boolean,
    onAlarmToggle: (Boolean) -> Unit // A Kotlin lambda callback
) {
    // 1. Local UI State tracking if the switch is physically turned on/off
    var isEnabled by remember { mutableStateOf(initialIsChecked) }

    val textColor = if (isEnabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, // Pushes Text to left, Switch to right
        verticalAlignment = Alignment.CenterVertically    // Aligns them vertically centered
    ) {
        // 2. The Text Group (Stacked vertically)
        Column {
            Text(
                color = textColor,
                text = time,
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(4.dp)) // Simple spacing block instead of margin
            Text(
                color = textColor,
                text = repeatDays,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // 3. The Interactive Switch
        Switch(
            checked = isEnabled,
            onCheckedChange = { newValue ->
                isEnabled = newValue       // Updates local state to redraw the switch
                onAlarmToggle(newValue)    // Fires event upward to trigger your Android system alarm logic
            }
        )
    }
}

@Composable
fun AlarmListScreen(
    modifier: Modifier = Modifier,
    alarmViewModel: AlarmViewModel = viewModel(modelClass = AlarmViewModel::class.java)
) {

    val alarmList by alarmViewModel.alarms.collectAsStateWithLifecycle()

    // The equivalent of a RecyclerView
    LazyColumn(modifier = modifier) {
        // 'items' loops through your data collection and builds rows on demand
        items(alarmList) { alarm ->
            AlarmItemRow(
                time = alarm.time,
                repeatDays = alarm.days,
                initialIsChecked = alarm.isEnabled,
                onAlarmToggle = { isToggled ->
                    // We will connect this to our background system later!
                    println("Alarm ${alarm.id} changed to: $isToggled")
                }
            )
        }
    }
}

@Composable
fun MainAlarmDashboardScreen(viewModel: AlarmViewModel = viewModel()) {
    // 1. Flag state controlling if the modal layer should be drawn down or up
    var showAddSheet by remember { mutableStateOf(false) }

    val alarmList by viewModel.alarms.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Text("+", fontSize = 24.sp)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // Renders list view
            AlarmListScreen(alarmViewModel = viewModel)

            // 2. Declarative visibility: If true, the bottom sheet automatically renders on screen
            if (showAddSheet) {
                AddAlarmBottomSheet(
                    onDismissRequest = { showAddSheet = false },
                    onTimeSelected = { hour, minute, isAm ->

                        // Pass this directly to your ViewModel to construct and add a new alarm object
                        val timeString =
                            String.format("%02d:%02d %s", hour, minute, if (isAm) "AM" else "PM")

                        viewModel.saveNewAlarm(
                            timeString,
                            "Mon, Tue, Wed, Thu, Fri",
                            hour, minute, isAm, isEnabled = true
                        )
                    }
                )
            }
        }
    }
}