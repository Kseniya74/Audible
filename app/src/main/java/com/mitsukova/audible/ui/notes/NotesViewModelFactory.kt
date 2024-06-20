package com.mitsukova.audible.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mitsukova.audible.data.repository.notes.NotesRepositoryImpl

class NotesViewModelFactory (
    private val notesRepository: NotesRepositoryImpl
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            return NotesViewModel(notesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}