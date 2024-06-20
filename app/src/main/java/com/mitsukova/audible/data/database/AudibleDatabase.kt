package com.mitsukova.audible.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mitsukova.audible.data.model.AudioSettingsEntity
import com.mitsukova.audible.data.model.BookEntity
import com.mitsukova.audible.data.model.BookSettingsEntity
import com.mitsukova.audible.data.model.FavouriteBookEntity
import com.mitsukova.audible.data.model.NotesEntity
import com.mitsukova.audible.data.model.ReadProgressEntity

@Database(entities = [BookEntity::class, FavouriteBookEntity::class, ReadProgressEntity::class,
    BookSettingsEntity::class, AudioSettingsEntity::class, NotesEntity::class],
    version = 15, exportSchema = false)
abstract class AudibleDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun favouriteBookDao(): FavouriteBookDao
    abstract fun readProgressDao(): ReadProgressDao
    abstract fun bookSettingsDao(): BookSettingsDao
    abstract fun audioSettingsDao(): AudioSettingsDao
    abstract fun notesDao(): NotesDao

    companion object {
        @Volatile
        private var INSTANCE: AudibleDatabase? = null

        fun getDatabase(context: Context): AudibleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = createDatabase(context)
                INSTANCE = instance
                instance
            }
        }

        private fun createDatabase(context: Context): AudibleDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AudibleDatabase::class.java,
                "audible-database"
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15
            ).build()
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favouriteBooks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`filePath` TEXT NOT NULL, `coverImage` BLOB NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `books` ADD COLUMN `isFavourite` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `readProgress` (`bookId` INTEGER PRIMARY KEY, " +
                            "`currentPage` INTEGER NOT NULL, " +
                            "`progressPercentage` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `readProgress_new` (`bookId` INTEGER, `currentPage` INTEGER NOT NULL, " +
                            "`progressPercentage` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`bookId`), " +
                            "FOREIGN KEY(`bookId`) " +
                            "REFERENCES `books`(`id`) ON DELETE CASCADE)"
                )
                // Копирование данных из старой таблицы в новую
                database.execSQL(
                    "INSERT INTO `readProgress_new` ( `bookId`, `currentPage`, `progressPercentage`) " +
                            "SELECT `bookId`, `currentPage`, `progressPercentage` " +
                            "FROM `readProgress`"
                )
                // Удаление старой таблицы
                database.execSQL("DROP TABLE `readProgress`")
                // Переименование новой таблицы в readProgress
                database.execSQL("ALTER TABLE `readProgress_new` RENAME TO `readProgress`")
            }
        }

        private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `readProgress_new` (`bookId` INTEGER NOT NULL," +
                        "`currentPage` INTEGER NOT NULL, " +
                        "`progressPercentage` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`bookId`, `currentPage`), " +
                        "FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON DELETE CASCADE)")
                database.execSQL("INSERT INTO `readProgress_new` (`bookId`, `currentPage`, `progressPercentage`)" +
                        " SELECT `bookId`, `currentPage`, `progressPercentage`" +
                        " FROM `readProgress`")
                database.execSQL("DROP TABLE `readProgress`")
                database.execSQL("ALTER TABLE `readProgress_new` RENAME TO `readProgress`")
            }
        }

        private val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE readProgress_new (progressId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "bookId INTEGER NOT NULL, " +
                        "currentPage INTEGER NOT NULL, " +
                        "progressPercentage INTEGER NOT NULL, " +
                        "FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE)")
                database.execSQL("INSERT INTO readProgress_new (bookId, currentPage, progressPercentage) " +
                        "SELECT bookId, currentPage, progressPercentage FROM readProgress")
                database.execSQL("DROP TABLE readProgress")
                database.execSQL("ALTER TABLE readProgress_new RENAME TO readProgress")
            }
        }

        private val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bookSettings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`fontSize` INTEGER NOT NULL, " +
                            "`lineSpacing` FLOAT NOT NULL," +
                            "`backgroundColor` INT NOT NULL," +
                            "`textColor` INT NOT NULL," +
                            "`autoScrollEnabled` INT NOT NULL," +
                            "`autoScrollSpeed` FLOAT NOT NULL)"
                )
            }
        }

        private val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
            CREATE TABLE new_book_settings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                fontSize INTEGER NOT NULL,
                lineSpacing FLOAT NOT NULL,
                backgroundColor TEXT NOT NULL,
                textColor TEXT NOT NULL,
                autoScrollEnabled INT NOT NULL,
                autoScrollSpeed FLOAT NOT NULL
            )
        """)

                // Копируем данные, преобразовывая цвета из `Int` в шестнадцатеричный формат
                database.execSQL("""
            INSERT INTO new_book_settings (id, fontSize, lineSpacing, backgroundColor, textColor, autoScrollEnabled, autoScrollSpeed)
            SELECT id, fontSize, lineSpacing,
                printf("#%06X", backgroundColor & 0xFFFFFF) AS backgroundColor,
                printf("#%06X", textColor & 0xFFFFFF) AS textColor,
                autoScrollEnabled, autoScrollSpeed
            FROM bookSettings
        """)

                // Удаляем старую таблицу
                database.execSQL("DROP TABLE bookSettings")

                // Переименовываем новую таблицу
                database.execSQL("ALTER TABLE new_book_settings RENAME TO bookSettings")
            }
        }

        private val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`bookId` INTEGER NOT NULL, " +
                            "`text` TEXT NOT NULL, " +
                            "FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE)"
                )
            }
        }

        private val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bookmarks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`bookId` INTEGER NOT NULL, " +
                            "`chapterTitle` TEXT NOT NULL, " +
                            "FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE)"
                )

                database.execSQL("DROP TABLE IF EXISTS `notes`")
            }
        }

        private val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `audioSettings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`voice` TEXT NOT NULL, " +
                            "`speed` TEXT NOT NULL)"
                )
            }
        }

        private val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `new_audioSettings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`voice` TEXT NOT NULL, " +
                            "`speed` TEXT NOT NULL)"
                )
                database.execSQL("DROP TABLE audioSettings")

                // Переименовываем новую таблицу
                database.execSQL("ALTER TABLE new_audioSettings RENAME TO audioSettings")
            }
        }

        private val MIGRATION_13_14: Migration = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`bookId` INTEGER NOT NULL, " +
                            "`text` TEXT NOT NULL, " +
                            "FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE)"
                )
            }
        }

        private val MIGRATION_14_15: Migration = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE bookmarks")
            }
        }
    }
}