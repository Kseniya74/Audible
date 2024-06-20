package com.mitsukova.audible.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mitsukova.audible.data.model.BookEntity

@Dao
interface BookDao {
    @Insert
    fun insertBook(bookEntity: BookEntity)

    @Query("SELECT * FROM books")
    fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE filePath = :filePath")
    fun getBook(filePath: String): BookEntity

    @Delete
    fun deleteBook(bookEntity: BookEntity)

    @Update
    fun updateBook(book: BookEntity)
}