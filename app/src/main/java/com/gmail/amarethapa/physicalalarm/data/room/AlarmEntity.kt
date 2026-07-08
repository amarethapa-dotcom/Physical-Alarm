package com.gmail.amarethapa.physicalalarm.data.room

import com.gmail.amarethapa.physicalalarm.data.models.Alarm
import java.util.Locale

@androidx.room.Entity(tableName = "alarms_table")
data class AlarmEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val isAm: Boolean,
    val repeatDays: Set<Int>, // e.g., setOf(2, 4, 6) for Mon, Wed, Fri
    val isEnabled: Boolean
)

fun AlarmEntity.toExternal(): Alarm {
    val formattedTime = String.format(Locale.getDefault(), "%d:%02d %s", hour, minute, if (isAm) "AM" else "PM")
    
    val daysList = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val formattedDays = if (repeatDays.size == 7) {
        "Every day"
    } else if (repeatDays.isEmpty()) {
        "Once"
    } else {
        repeatDays.sorted().joinToString(", ") { dayInt ->
            daysList.getOrElse(dayInt - 1) { "Unknown" }
        }
    }

    return Alarm(id, formattedTime, formattedDays, isEnabled)
}

fun List<AlarmEntity>.toExternalList() = map { it.toExternal() }