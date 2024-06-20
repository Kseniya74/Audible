package com.mitsukova.audible.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mitsukova.audible.data.model.AudioSettingsEntity

@Dao
interface AudioSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioSettings(audioSettings: AudioSettingsEntity)

    @Query("SELECT * FROM audioSettings WHERE id = 0")
    suspend fun getAudioSettings(): AudioSettingsEntity

    @Update
    suspend fun updateAudioSettings(audioSettings: AudioSettingsEntity)

    @Query("DELETE FROM audioSettings")
    suspend fun deleteAll()
}