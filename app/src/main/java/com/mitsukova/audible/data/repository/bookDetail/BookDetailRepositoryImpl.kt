package com.mitsukova.audible.data.repository.bookDetail

import android.util.Log
import com.mitsukova.audible.data.database.AudibleDatabase
import com.mitsukova.audible.data.model.AudioSettingsEntity
import com.mitsukova.audible.data.model.BookSettingsEntity
import com.mitsukova.audible.data.model.Page
import com.mitsukova.audible.data.model.ReadProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

class BookDetailRepositoryImpl(private val db: AudibleDatabase) : BookDetailRepository {

    private var totalPages: Int = 0

    override fun setTotalPages(totalPages: Int): Int {
        this.totalPages = totalPages
        return totalPages
    }

    override suspend fun loadReadingProgress(filePath: String): Int? {
        return withContext(Dispatchers.IO) {
            val readProgressDao = db.readProgressDao()
            val bookDao = filePath?.let { db.bookDao().getBook(it) }
            val bookId = bookDao?.id

            if (bookId != null) {
                val savedProgress = readProgressDao.getProgressByBookId(bookId)

                savedProgress?.currentPage
            } else {
                null
            }
        }
    }

    override suspend fun saveReadingProgress(filePath: String, currentPage: Int) {
        withContext(Dispatchers.IO) {
            val readProgressDao = db.readProgressDao()

            val bookDao = filePath?.let { db.bookDao().getBook(it) }
            val bookId = bookDao?.id

            val progress = readProgressDao.getProgressByBookId(bookId)

            if (progress != null) {
                progress.currentPage = currentPage
                progress.progressPercentage =
                    calculateProgressPercentage(currentPage, setTotalPages(totalPages))
                readProgressDao.updateProgress(progress)
            } else {
                val newProgress = bookId?.let {
                    ReadProgressEntity(
                        bookId = it,
                        currentPage = currentPage,
                        progressPercentage = calculateProgressPercentage(
                            currentPage,
                            setTotalPages(totalPages)
                        )
                    )
                }
                if (newProgress != null) {
                    readProgressDao.insertProgress(newProgress)
                }
            }
        }
    }

    override fun calculateProgressPercentage(currentChapter: Int, totalChapters: Int): Int {
        return ((currentChapter.toFloat() + 1) / totalChapters.toFloat() * 100).toInt()
    }

//    override fun loadBook(epubFilePath: String): List<String> {
//        val pages = mutableListOf<String>()
//        try {
//            val epubInputStream = FileInputStream(epubFilePath)
//            val epubReader = EpubReader()
//            val book: Book = epubReader.readEpub(epubInputStream)
//
//            for (tocReference in book.tableOfContents.tocReferences) {
//                val resource = tocReference.resource
//                val content = getResourceContent(resource)
//
//                val pageSize = 5000
//                var startIndex = 0
//
//                while (startIndex < content.length) {
//                    val endIndex = (startIndex + pageSize).coerceAtMost(content.length)
//                    val pageText = content.substring(startIndex, endIndex)
//                    pages.add(pageText)
//                    startIndex = endIndex
//                }
//            }
//            epubInputStream.close()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//        return pages
//    }

    override fun loadBook(epubFilePath: String): List<Page> {
        val pages = mutableListOf<Page>()
        try {
            val epubInputStream = FileInputStream(epubFilePath)
            val epubReader = EpubReader()
            val book: Book = epubReader.readEpub(epubInputStream)

            for (tocReference in book.tableOfContents.tocReferences) {
                val resource = tocReference.resource
                val content = getResourceContent(resource)
                val href = resource.href

                val pageSize = 5000 // Размер страницы по символам
                var startIndex = 0

                while (startIndex < content.length) {
                    var endIndex = findEndOfPage(content, startIndex, pageSize)
                    if (endIndex == -1) {
                        endIndex = (startIndex + pageSize).coerceAtMost(content.length)
                    }

                    val pageText = content.substring(startIndex, endIndex)
                    pages.add(Page(pageText, href))
                    Log.d("loadBook", "Added page with href: $href")
                    startIndex = endIndex
                }
            }
            epubInputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return pages
    }

    private fun findEndOfPage(content: String, startIndex: Int, pageSize: Int): Int {
        val endParagraph = content.indexOf("\n\n", startIndex + pageSize)
        return if (endParagraph != -1) {
            endParagraph
        } else {
            val endSentence = content.indexOf(".", startIndex + pageSize)
            if (endSentence != -1) {
                endSentence
            } else {
                -1
            }
        }
    }

    override fun getResourceContent(resource: Resource): String {
        val inputStream = resource.inputStream
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        val content = reader.readText()
        reader.close()
        return content
        //return content.replace(Regex("<[^>]*>"), "")
    }

    override suspend fun getBookSettings(): BookSettingsEntity {
        return withContext(Dispatchers.IO) {
            val settingsDao = db.bookSettingsDao()
            val currentSettings = settingsDao.getBookSettings()
            currentSettings
        }
    }

    override suspend fun getAudioSettings(): AudioSettingsEntity? {
        return withContext(Dispatchers.IO) {
            val audioSettingsDao = db.audioSettingsDao()
            audioSettingsDao.getAudioSettings()
        }
    }

    override suspend fun saveAudioSettings(audioSettings: AudioSettingsEntity) {
        return withContext(Dispatchers.IO) {
            val audioSettingsDao = db.audioSettingsDao()
            audioSettingsDao.insertAudioSettings(audioSettings)
        }
    }

    override suspend fun updateAudioSettings(audioSettings: AudioSettingsEntity) {
        return withContext(Dispatchers.IO) {
            val audioSettingsDao = db.audioSettingsDao()
            audioSettingsDao.updateAudioSettings(audioSettings)
        }
    }

    override suspend fun deleteAudioSettings() {
        val audioSettingsDao = db.audioSettingsDao()
        audioSettingsDao.deleteAll()
    }
}


