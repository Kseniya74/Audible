package com.mitsukova.audible.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mitsukova.audible.data.repository.favourites.FavouriteRepositoryImpl

class FavouritesViewModelFactory(private val repository: FavouriteRepositoryImpl) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavouritesViewModel::class.java)) {
            return FavouritesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}