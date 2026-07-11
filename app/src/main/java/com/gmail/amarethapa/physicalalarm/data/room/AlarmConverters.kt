package com.gmail.amarethapa.physicalalarm.data.room

import androidx.room.TypeConverter

class AlarmConverters {
    @TypeConverter
    fun fromDaysSet(days: Set<Int>): String {
        return days.joinToString(separator = ",")
    }

    @TypeConverter
    fun toDaysSet(data: String): Set<Int> {
        if (data.isBlank()) return emptySet()
        return try {
            data.split(",")
                .filter { it.isNotBlank() }
                .map { it.trim().toInt() }
                .toSet()
        } catch (e: NumberFormatException) {
            // Corrupted data fallback — return empty set rather than crash
            emptySet()
        }
    }
}