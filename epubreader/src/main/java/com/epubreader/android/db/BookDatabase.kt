/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.epubreader.android.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.epubreader.android.domain.model.Book
import com.epubreader.android.domain.model.Bookmark
import com.epubreader.android.domain.model.Catalog
import com.epubreader.android.domain.model.Highlight
import com.epubreader.android.domain.model.HighlightConverters

@Database(
    entities = [Book::class, Bookmark::class, Highlight::class, Catalog::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(HighlightConverters::class)
abstract class BookDatabase : RoomDatabase() {

    abstract fun booksDao(): BooksDao

    abstract fun catalogDao(): CatalogDao

    companion object {
        @Volatile
        private var INSTANCE: BookDatabase? = null

        fun getDatabase(context: Context): BookDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookDatabase::class.java,
                    "books_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
