package com.gmail.amarethapa.physicalalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmBottomSheet(
    onDismissRequest: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int, isAm: Boolean, chosenDays: Set<Int>) -> Unit
) {
    // 1. Local UI state for user selection values
    var selectedHour by remember { mutableStateOf(6) }
    var selectedMinute by remember { mutableStateOf(30) }
    var isAm by remember { mutableStateOf(true) }
    var chosenDays by remember {
        mutableStateOf(
            setOf(
                2,
                3,
                4,
                5,
                6
            )
        )
    } // Default values initialized to weekdays

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Material 3 built-in standard BottomSheet container
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set New Alarm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. The Time Selector Segment (Hours : Minutes)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Hour Display Block
                TimeNumberBlock(
                    value = String.format("%02d", selectedHour),
                    onIncrement = { if (selectedHour < 12) selectedHour++ else selectedHour = 1 },
                    onDecrement = { if (selectedHour > 1) selectedHour-- else selectedHour = 12 }
                )

                Text(
                    text = ":",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Minute Display Block
                TimeNumberBlock(
                    value = String.format("%02d", selectedMinute),
                    onIncrement = {
                        if (selectedMinute < 59) selectedMinute++ else selectedMinute = 0
                    },
                    onDecrement = {
                        if (selectedMinute > 0) selectedMinute-- else selectedMinute = 59
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. AM / PM Segment Selector Selector Row
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                AmPmToggleButton(label = "AM", isSelected = isAm, onClick = { isAm = true })
                AmPmToggleButton(label = "PM", isSelected = !isAm, onClick = { isAm = false })
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Select repeat days

            Text(text = "Repeat Days", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            DayPickerGroup(
                selectedDays = chosenDays,
                onDaysChanged = { updatedSelection -> chosenDays = updatedSelection }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Base Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onTimeSelected(
                            selectedHour, selectedMinute, isAm,
                            chosenDays
                        )
                        onDismissRequest() // Closes the window layer automatically
                    }
                ) {
                    Text("Save Alarm")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DayPickerGroup(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit
) {
    // Mapping standard Calendar indexing keys to human-readable strings
    val daysOfWeekMap = listOf(
        2 to "M", // Calendar.MONDAY
        3 to "T", // Calendar.TUESDAY
        4 to "W", // Calendar.WEDNESDAY
        5 to "T", // Calendar.THURSDAY
        6 to "F", // Calendar.FRIDAY
        7 to "S", // Calendar.SATURDAY
        1 to "S"  // Calendar.SUNDAY
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        daysOfWeekMap.forEach { (dayIndex, label) ->
            val isSelected = selectedDays.contains(dayIndex)

            // Dynamic theme selection matching active token frameworks
            val backgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }

            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable {
                        // Implement an immutable structural toggle update
                        val updatedSet = if (isSelected) {
                            selectedDays - dayIndex // Remove integer element natively
                        } else {
                            selectedDays + dayIndex // Add integer element natively
                        }
                        onDaysChanged(updatedSet)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Reusable custom sub-composable widget for picking individual column numbers
@Composable
fun TimeNumberBlock(
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onIncrement) { Text("▲", fontSize = 20.sp) }

        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }

        IconButton(onClick = onDecrement) { Text("▼", fontSize = 20.sp) }
    }
}

@Composable
fun AmPmToggleButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}