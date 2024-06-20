package com.mitsukova.audible.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mitsukova.audible.data.model.ReadProgressEntity

@Dao
interface ReadProgressDao {
    @Query("SELECT * FROM `readProgress` WHERE bookId = :bookId")
    fun getProgressByBookId(bookId: Long?): ReadProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProgress(progress: ReadProgressEntity)

    @Update
    fun updateProgress(progress: ReadProgressEntity)

    @Delete
    fun deleteProgress(progress: ReadProgressEntity)

    @Query("DELETE FROM readProgress")
    suspend fun deleteAll()
}