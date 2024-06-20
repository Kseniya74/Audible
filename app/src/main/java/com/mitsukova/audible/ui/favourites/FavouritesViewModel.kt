package com.mitsukova.audible.ui.favourites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mitsukova.audible.data.model.FavouriteBookEntity
import com.mitsukova.audible.data.repository.favourites.FavouriteRepositoryImpl

class FavouritesViewModel(private val repository: FavouriteRepositoryImpl) : ViewModel() {
    //val favouriteBookData : LiveData<List<FavouriteBookEntity>> = repository.getFavouriteBooks()
    private val _favouriteBookData = MutableLiveData<List<FavouriteBookEntity>>()
    val favouriteBookData: LiveData<List<FavouriteBookEntity>> get() = _favouriteBookData

    fun removeFromFavourites(filePath: String) {
        repository.removeFromFavourites(filePath)
    }

    fun loadReadingProgress(filePath: String): LiveData<Int?> {
        return repository.loadReadingProgress(filePath)
    }

    fun refreshFavourites() {
        repository.getFavouriteBooks().observeForever { books ->
            _favouriteBookData.postValue(books)
        }
    }
}