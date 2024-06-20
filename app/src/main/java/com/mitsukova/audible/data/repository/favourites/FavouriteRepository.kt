package com.mitsukova.audible.data.repository.favourites

import androidx.lifecycle.LiveData
import com.mitsukova.audible.data.model.FavouriteBookEntity

interface FavouriteRepository {
    fun getFavouriteBooks(): LiveData<List<FavouriteBookEntity>>
    fun removeFromFavourites(filePath: String)
    fun loadReadingProgress(filePath: String): LiveData<Int?>
}