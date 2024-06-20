package com.mitsukova.audible.data.repository.favourites

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mitsukova.audible.data.database.AudibleDatabase
import com.mitsukova.audible.data.model.FavouriteBookEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavouriteRepositoryImpl(application: Application) : FavouriteRepository {
    private val favouriteBookDao = AudibleDatabase.getDatabase(application).favouriteBookDao()
    private val bookDao = AudibleDatabase.getDatabase(application).bookDao()
    private val readProgressDao = AudibleDatabase.getDatabase(application).readProgressDao()

    override fun getFavouriteBooks(): LiveData<List<FavouriteBookEntity>> {
        val liveData = MutableLiveData<List<FavouriteBookEntity>>()
        CoroutineScope(Dispatchers.IO).launch {
            val FavouriteBookEntity = favouriteBookDao.getAllFavouriteBooks()
                liveData.postValue(FavouriteBookEntity)
            }
        return liveData
    }

    override fun removeFromFavourites(filePath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val favouriteBookEntity = favouriteBookDao.getFavouriteBook(filePath)
            favouriteBookDao.deleteFavouriteBook(favouriteBookEntity)
            val bookEntity = bookDao.getBook(filePath)
            bookEntity.isFavourite = false
            bookDao.updateBook(bookEntity)
        }
    }

    override fun loadReadingProgress(filePath: String): LiveData<Int?> {
        val liveData = MutableLiveData<Int?>()
        CoroutineScope(Dispatchers.IO).launch {
            val bookEntity = bookDao.getBook(filePath)
            if (bookEntity != null) {
                val bookId = bookEntity.id
                val progress =
                    bookId.let { readProgressDao.getProgressByBookId(it)?.progressPercentage }
                liveData.postValue(progress)
            }
        }
        return liveData
    }
}
