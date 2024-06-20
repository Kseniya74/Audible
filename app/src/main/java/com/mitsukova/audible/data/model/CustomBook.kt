package com.mitsukova.audible.data.model

import nl.siegmann.epublib.domain.Book

data class CustomBook(val epubBook: Book, val filePath: String, var isFavourite: Boolean, var progress: Int) {


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomBook) return false
        return filePath == other.filePath
    }

    override fun hashCode(): Int {
        return filePath.hashCode()
    }
}
