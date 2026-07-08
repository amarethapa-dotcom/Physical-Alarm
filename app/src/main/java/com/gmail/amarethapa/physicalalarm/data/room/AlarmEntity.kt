package com.gmail.amarethapa.physicalalarm.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms_table")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // 0 handles auto-increment instantiation fallback safely
    val time: String,
    val days: String,
    val isEnabled: Boolean
)