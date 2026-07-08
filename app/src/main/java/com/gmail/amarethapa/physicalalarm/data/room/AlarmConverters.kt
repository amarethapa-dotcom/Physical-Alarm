package com.gmail.amarethapa.physicalalarm.data.room

import androidx.room.TypeConverter

class AlarmConverters {
    @TypeConverter
    fun fromDaysSet(days: Set<Int>): String {
        return days.joinToString(separator = ",")
    }

    @TypeConverter
    fun toDaysSet(data: String): Set<Int> {
        if (data.isEmpty()) return emptySet()
        return data.split(",").map { it.toInt() }.toSet()
    }
}