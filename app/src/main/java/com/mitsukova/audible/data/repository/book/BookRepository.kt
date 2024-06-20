package com.mitsukova.audible.data.repository.book

import android.net.Uri
import androidx.lifecycle.LiveData
import com.mitsukova.audible.data.model.CustomBook
import nl.siegmann.epublib.domain.Book

interface BookRepository {
    fun getBooks(): LiveData<List<CustomBook>>
    fun uploadBook(filePath: String)
    suspend fun getFilePathFromUri(uri: Uri): String?
    fun deleteBook(customBook: CustomBook)
    fun addToFavourites(customBook: CustomBook)
    fun removeFromFavourites(customBook: CustomBook)
    fun parseEpubFile(filePath: String): Book?
    fun loadReadingProgress(filePath: String): LiveData<Int?>
}