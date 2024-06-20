package com.mitsukova.audible.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mitsukova.audible.data.model.NotesEntity

@Dao
interface NotesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NotesEntity)

    @Query("SELECT * FROM notes WHERE bookId = :bookId")
    suspend fun getNoteByBookId(bookId: Long): List<NotesEntity>

    @Query("SELECT * FROM notes WHERE bookId = :bookId AND text = :text")
    suspend fun getNoteByTitle(bookId: Long, text: String): NotesEntity?

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)
}