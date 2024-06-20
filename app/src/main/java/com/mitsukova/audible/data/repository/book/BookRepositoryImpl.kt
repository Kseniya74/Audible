package com.mitsukova.audible.data.repository.book

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mitsukova.audible.data.database.AudibleDatabase
import com.mitsukova.audible.data.database.BookDao
import com.mitsukova.audible.data.database.FavouriteBookDao
import com.mitsukova.audible.data.database.ReadProgressDao
import com.mitsukova.audible.data.model.BookEntity
import com.mitsukova.audible.data.model.CustomBook
import com.mitsukova.audible.data.model.FavouriteBookEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class BookRepositoryImpl(application: Application) : BookRepository {
    private val bookDao: BookDao
    private val favouriteBookDao: FavouriteBookDao
    private val readProgressDao: ReadProgressDao
    private val context = application.applicationContext

    init {
        val db = AudibleDatabase.getDatabase(application)
        bookDao = db.bookDao()
        favouriteBookDao = db.favouriteBookDao()
        readProgressDao = db.readProgressDao()
    }

    override fun getBooks(): LiveData<List<CustomBook>> {
        val liveData = MutableLiveData<List<CustomBook>>()
        CoroutineScope(Dispatchers.IO).launch {
            val bookEntities = bookDao.getAllBooks()
            val customBooks = bookEntities.mapNotNull { bookEntity ->
                val book = parseEpubFile(bookEntity.filePath)
                if (book != null) {
                    val progress =
                        readProgressDao.getProgressByBookId(bookEntity.id)?.progressPercentage ?: 0
                    CustomBook(book, bookEntity.filePath, bookEntity.isFavourite, progress)
                } else {
                    null
                }
            }
            withContext(Dispatchers.Main) {
                liveData.postValue(customBooks)
            }
        }
        return liveData
    }

    override fun uploadBook(filePath: String) {
        if (filePath != null) {
            Log.d("uploadBook", "filePath: $filePath")
            CoroutineScope(Dispatchers.IO).launch {
                val book = parseEpubFile(filePath)
                if (book != null) {
                    val existingBook = bookDao.getBook(filePath)
                    if (existingBook == null) {
                        val customBook = CustomBook(book, filePath, false, 0)
                        val coverImageData = customBook.epubBook.coverImage?.data

                        if (coverImageData != null) {
                            val bookEntity = BookEntity(
                                filePath = filePath,
                                coverImage = coverImageData
                            )
                            bookDao.insertBook(bookEntity)
                        }
                    }
                }
            }
        }
    }

    override suspend fun getFilePathFromUri(uri: Uri): String? {
        var filePath: String? = null
        try {
            withContext(Dispatchers.IO) {
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                if (documentFile != null && documentFile.isFile) {
                    val fileName = documentFile.name
                    val file = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName
                    )
                    filePath = file.absolutePath
                    if (!file.exists()) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        inputStream?.use { input ->
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Error getting file path from URI: ${e.message}")
        }
        return filePath
    }

    override fun deleteBook(customBook: CustomBook) {
        CoroutineScope(Dispatchers.IO).launch {
            val bookEntity = bookDao.getBook(customBook.filePath)
            if (bookEntity.isFavourite)
                favouriteBookDao.deleteFavouriteBook(favouriteBookDao.getFavouriteBook(customBook.filePath))

            if (bookEntity != null) {
                bookDao.deleteBook(bookEntity)
            }
        }
    }

    override fun addToFavourites(customBook: CustomBook) {
        CoroutineScope(Dispatchers.IO).launch {
            val existingFavouriteBookEntity = favouriteBookDao.getFavouriteBook(customBook.filePath)
            if (existingFavouriteBookEntity == null) {
                val favouriteBookEntity = customBook.epubBook.coverImage?.data?.let {
                    FavouriteBookEntity(
                        filePath = customBook.filePath,
                        coverImage = it
                    )
                }
                if (favouriteBookEntity != null) {
                    favouriteBookDao.insertFavouriteBook(favouriteBookEntity)
                }

                val bookEntity = bookDao.getBook(customBook.filePath)
                bookEntity?.isFavourite = true
                bookDao.updateBook(bookEntity)
            }
        }
    }

    override fun removeFromFavourites(customBook: CustomBook) {
        CoroutineScope(Dispatchers.IO).launch {
            val existingFavouriteBookEntity = favouriteBookDao.getFavouriteBook(customBook.filePath)
            if (existingFavouriteBookEntity != null) {
                favouriteBookDao.deleteFavouriteBook(existingFavouriteBookEntity)

                val bookEntity = bookDao.getBook(customBook.filePath)
                bookEntity?.isFavourite = false
                bookDao.updateBook(bookEntity)
            }
        }
    }

    override fun parseEpubFile(filePath: String): Book? {
        try {
            val bookStream: InputStream = FileInputStream(filePath)
            return EpubReader().readEpub(bookStream)
        } catch (e: Exception) {
            Log.e("BookParsing", "Error parsing EPUB file: ${e.message}")
        }
        return null
    }

    override fun loadReadingProgress(filePath: String): LiveData<Int?> {
        val liveData = MutableLiveData<Int?>()
        CoroutineScope(Dispatchers.IO).launch {
            val bookEntity = bookDao.getBook(filePath)
            if (bookEntity != null) {
                val bookId = bookEntity.id
                val progress =
                    bookId.let { readProgressDao.getProgressByBookId(it)?.progressPercentage }
                liveData.postValue(progress)
            }
        }
        return liveData
    }
}