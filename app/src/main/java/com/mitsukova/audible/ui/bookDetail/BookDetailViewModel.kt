package com.mitsukova.audible.ui.bookDetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitsukova.audible.data.model.AudioSettingsEntity
import com.mitsukova.audible.data.model.BookSettingsEntity
import com.mitsukova.audible.data.model.Page
import com.mitsukova.audible.data.repository.bookDetail.BookDetailRepository
import kotlinx.coroutines.launch

class BookDetailViewModel(private val repository: BookDetailRepository) : ViewModel() {

    val pages = MutableLiveData<List<Page>>()
    private val _bookSettingsEntity = MutableLiveData<BookSettingsEntity>()
    val bookSettingsEntity: LiveData<BookSettingsEntity> = _bookSettingsEntity
    private val _audioSettings = MutableLiveData<AudioSettingsEntity?>()
    val audioSettings: MutableLiveData<AudioSettingsEntity?> = _audioSettings

    init {
        viewModelScope.launch {
            _bookSettingsEntity.value = getBookSettings()
            _audioSettings.value = getAudioSettings()
        }
    }

    fun loadBook(filePath: String) {
        val pagesData = repository.loadBook(filePath)
        pages.postValue(pagesData)
    }

    fun saveReadingProgress(filePath: String, currentPage: Int) {
        viewModelScope.launch {
            repository.saveReadingProgress(filePath, currentPage)
        }
    }

    suspend fun loadReadingProgress(filePath: String): Int? {
        return repository.loadReadingProgress(filePath)
    }

    private suspend fun getBookSettings(): BookSettingsEntity {
        return repository.getBookSettings()
    }

    fun setTotalPages(totalPages: Int) : Int {
        return repository.setTotalPages(totalPages = totalPages)
    }

    suspend fun getAudioSettings(): AudioSettingsEntity? {
        return repository.getAudioSettings()
    }

    fun saveAudioSettings(voice: String, speed: String) {
        viewModelScope.launch {
            val audioSettings = AudioSettingsEntity(
                voice = voice,
                speed = speed)
            repository.saveAudioSettings(audioSettings)
        }
    }
}


