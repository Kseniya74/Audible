package com.mitsukova.audible.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mitsukova.audible.data.model.BookSettingsEntity

@Dao
interface BookSettingsDao {
    @Query("SELECT * FROM bookSettings WHERE id = 0")
    suspend fun getBookSettings(): BookSettingsEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookSettings(bookSettingsEntity: BookSettingsEntity)

    @Update
    suspend fun updateBookSettings(bookSettingsEntity: BookSettingsEntity)

    @Query("DELETE FROM bookSettings")
    suspend fun deleteAll()
}