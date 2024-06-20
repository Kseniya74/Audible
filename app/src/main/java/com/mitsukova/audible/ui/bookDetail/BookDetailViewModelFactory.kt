package com.mitsukova.audible.ui.bookDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mitsukova.audible.data.repository.bookDetail.BookDetailRepositoryImpl

class BookDetailViewModelFactory(
    private val bookDetailRepository: BookDetailRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookDetailViewModel::class.java)) {
            return BookDetailViewModel(bookDetailRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}