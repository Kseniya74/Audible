package com.mitsukova.audible.data.repository.notes

import com.mitsukova.audible.data.model.NotesEntity

interface NotesRepository {
    suspend fun getNotesByBookId(filePath: String): List<NotesEntity>
    suspend fun deleteNote(note: NotesEntity)
}