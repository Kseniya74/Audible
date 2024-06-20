package com.mitsukova.audible.ui.notes

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitsukova.audible.data.model.NotesEntity
import com.mitsukova.audible.data.repository.notes.NotesRepository
import kotlinx.coroutines.launch

class NotesViewModel(private val notesRepository: NotesRepository) : ViewModel() {
    private val _notes = MutableLiveData<List<NotesEntity>?>()
    val notes: MutableLiveData<List<NotesEntity>?> = _notes

    suspend fun getNotesByBookId(filePath: String) {
        viewModelScope.launch {
            val result = notesRepository.getNotesByBookId(filePath)
            _notes.postValue(result)
        }
    }

    suspend fun deleteNote(note: NotesEntity) {
        viewModelScope.launch {
            notesRepository.deleteNote(note)
        }
    }
}