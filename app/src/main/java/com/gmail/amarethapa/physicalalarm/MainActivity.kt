package com.gmail.amarethapa.physicalalarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gmail.amarethapa.physicalalarm.ui.AddAlarmBottomSheet
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
    alarmViewModel: AlarmViewModel = viewModel(modelClass = AlarmViewModel::class.java)
) {

    val alarmList by alarmViewModel.alarms.collectAsStateWithLifecycle()
    var alarmToDeleteId by remember { mutableStateOf<Int?>(null) }

    if (alarmToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { alarmToDeleteId = null },
            title = { Text("Delete Alarm") },
            text = { Text("Are you sure you want to delete this alarm?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        alarmToDeleteId?.let { alarmViewModel.deleteAlarm(it) }
                        alarmToDeleteId = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDeleteId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = alarmList,
            key = { alarm -> alarm.id }
        ) { alarm ->

            // 1. Correct Mobile M3 state instantiation from the official docs
            val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        alarmToDeleteId = alarm.id
                        false // Don't dismiss yet, wait for dialog confirmation
                    } else {
                        false // Ignore swiping right
                    }
                }
            )

            // Reset swipe state if dialog is cancelled
            LaunchedEffect(alarmToDeleteId) {
                if (alarmToDeleteId == null && swipeToDismissBoxState.currentValue != SwipeToDismissBoxValue.Settled) {
                    swipeToDismissBoxState.reset()
                }
            }

            // 2. Official Mobile Component Wrapper
            SwipeToDismissBox(
                state = swipeToDismissBoxState,
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = true,
                backgroundContent = {

                    val backgroundColor = lerp(
                        Color.Transparent,
                        MaterialTheme.colorScheme.errorContainer,
                        swipeToDismissBoxState.progress
                    )

                    val textColor = lerp(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.onErrorContainer,
                        swipeToDismissBoxState.progress
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(backgroundColor)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {

                        Text(
                            text = "Delete",
                            color = textColor,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                content = {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        AlarmItemRow(
                            time = alarm.time,
                            repeatDays = alarm.days,
                            initialIsChecked = alarm.isEnabled,
                            onAlarmToggle = { isToggled ->
                                alarmViewModel.toggleAlarm(alarm.id, isToggled)
                            }
                        )
                    }
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
            LargeFloatingActionButton(onClick = { showAddSheet = true }) {

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
                    onTimeSelected = { hour, minute, isAm, chosenDays ->

                        viewModel.saveNewAlarm(
                            hour, minute, isAm, chosenDays, true
                        )
                    }
                )
            }
        }
    }
}