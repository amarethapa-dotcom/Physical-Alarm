package com.gmail.amarethapa.physicalalarm.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlarmEntity::class], version = 1, exportSchema = false)
abstract class AlarmRoomDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmRoomDatabase? = null

        // Thread-safe singleton constructor logic to prevent multi-instance allocation leaks
        fun getDatabase(context: Context): AlarmRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmRoomDatabase::class.java,
                    "alarm_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}