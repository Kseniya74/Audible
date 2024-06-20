package com.mitsukova.audible.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val coverImage: ByteArray,
    var isFavourite: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BookEntity

        if (id != other.id) return false
        if (filePath != other.filePath) return false
        if (!coverImage.contentEquals(other.coverImage)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + coverImage.contentHashCode()
        return result
    }
}

@Entity(tableName = "favouriteBooks")
data class FavouriteBookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val coverImage: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FavouriteBookEntity

        if (id != other.id) return false
        if (filePath != other.filePath) return false
        if (!coverImage.contentEquals(other.coverImage)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + coverImage.contentHashCode()
        return result
    }
}


@Entity(
    tableName = "readProgress",
    foreignKeys = [ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)]
)
data class ReadProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val progressId: Long = 0,
    val bookId: Long,
    var currentPage: Int,
    var progressPercentage: Int
)

@Entity(tableName = "bookSettings")
data class BookSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val fontSize: Int,
    val lineSpacing: Float,
    val backgroundColor: String,
    val textColor: String,
    val autoScrollEnabled: Boolean,
    val autoScrollSpeed: Float
)

@Entity(tableName = "notes",
    foreignKeys = [ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)]
)
data class NotesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val text: String
)

@Entity(tableName = "audioSettings")
data class AudioSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val voice: String,
    val speed: String
)