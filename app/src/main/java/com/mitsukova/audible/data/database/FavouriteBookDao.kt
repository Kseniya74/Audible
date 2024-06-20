package com.mitsukova.audible.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mitsukova.audible.data.model.FavouriteBookEntity

@Dao
interface FavouriteBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insertFavouriteBook(favouriteBook: FavouriteBookEntity)

    @Query("SELECT * FROM FavouriteBooks")
     fun getAllFavouriteBooks(): List<FavouriteBookEntity>

    @Query("SELECT * FROM FavouriteBooks WHERE filePath = :filePath")
    fun getFavouriteBook(filePath: String): FavouriteBookEntity

    @Delete
    fun deleteFavouriteBook(favouriteBook: FavouriteBookEntity)
}