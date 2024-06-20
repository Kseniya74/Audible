package com.mitsukova.audible.data.repository.favourites

import com.mitsukova.audible.data.model.CustomBook

interface BookActionListener {
    fun addToFavourites(customBook: CustomBook)
    fun deleteBook(customBook: CustomBook, position: Int)
    fun removeFromFavourites(customBook: CustomBook, position: Int)
}
