package com.mitsukova.audible.ui.book

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mitsukova.audible.data.model.CustomBook
import com.mitsukova.audible.data.repository.book.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book

class BookViewModel(application: Application,  private val bookRepository: BookRepository) : ViewModel() {

    private val _bookData = MutableLiveData<List<CustomBook>>()
    val bookData: LiveData<List<CustomBook>> get() = _bookData

    fun uploadBook(filePath: String) {
        bookRepository.uploadBook(filePath)
    }

    fun refreshBooks() {
        bookRepository.getBooks().observeForever { books ->
            _bookData.postValue(books)
        }
    }

    fun deleteBook(customBook: CustomBook) {
        bookRepository.deleteBook(customBook)
    }

    fun addToFavourites(customBook: CustomBook) {
        bookRepository.addToFavourites(customBook)
    }

    suspend fun getFilePathFromUri(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            bookRepository.getFilePathFromUri(uri)
        }
    }

    fun parseEpubFile(filePath: String): Book? {
        return bookRepository.parseEpubFile(filePath)
    }

    fun loadReadingProgress(filePath: String): LiveData<Int?> {
        return bookRepository.loadReadingProgress(filePath)
    }

    fun removeFromFavourites(customBook: CustomBook) {
        return bookRepository.removeFromFavourites(customBook)
    }
}