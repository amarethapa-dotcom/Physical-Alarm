package com.gmail.amarethapa.physicalalarm.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms_table ORDER BY id DESC")
    fun getAllAlarmsFlow(): Flow<List<AlarmEntity>> // Automatically pushes reactive live updates!

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAlarm(alarmEntity: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarmEntity: AlarmEntity)
}