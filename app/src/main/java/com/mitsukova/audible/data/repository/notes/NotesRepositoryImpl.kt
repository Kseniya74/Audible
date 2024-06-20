package com.mitsukova.audible.data.repository.notes

import com.mitsukova.audible.data.database.AudibleDatabase
import com.mitsukova.audible.data.model.NotesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotesRepositoryImpl(private val db: AudibleDatabase) : NotesRepository {
    override suspend fun getNotesByBookId(filePath: String): List<NotesEntity> {
        return withContext(Dispatchers.IO) {
            val notesDao = db.notesDao()
            val bookDao = filePath.let { db.bookDao().getBook(it) }
            val bookId = bookDao.id

            notesDao.getNoteByBookId(bookId)
        }
    }

    override suspend fun deleteNote(note: NotesEntity) {
        withContext(Dispatchers.IO) {
            val notesDao = db.notesDao()
            notesDao.deleteNote(note.id)
        }
    }
}