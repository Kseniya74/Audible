package com.mitsukova.audible.data.repository.bookDetail

import com.mitsukova.audible.data.model.AudioSettingsEntity
import com.mitsukova.audible.data.model.BookSettingsEntity
import com.mitsukova.audible.data.model.Page
import nl.siegmann.epublib.domain.Resource

interface BookDetailRepository {
    suspend fun getAudioSettings(): AudioSettingsEntity?
    suspend fun saveAudioSettings(audioSettings: AudioSettingsEntity)
    suspend fun updateAudioSettings(audioSettings: AudioSettingsEntity)
    fun setTotalPages(totalPages: Int): Int
    suspend fun loadReadingProgress(filePath: String): Int?
    suspend fun saveReadingProgress(filePath: String, currentPage: Int)
    fun calculateProgressPercentage(currentChapter: Int, totalChapters: Int): Int
    fun loadBook(epubFilePath: String): List<Page>
    fun getResourceContent(resource: Resource): String
    suspend fun getBookSettings(): BookSettingsEntity
    suspend fun deleteAudioSettings()
}