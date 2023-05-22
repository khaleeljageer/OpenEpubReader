/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.epubreader.android

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.epubreader.android.data.BookRepository
import com.epubreader.android.db.BookDatabase
import com.epubreader.android.reader.ReaderRepository
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import timber.log.Timber
import java.io.File
import java.util.Properties

open class ReadiumApplication : Application() {

    val Context.navigatorPreferences: DataStore<Preferences>
            by preferencesDataStore(name = "navigator-preferences")

    lateinit var readium: Readium
        private set

    lateinit var storageDir: File

    lateinit var bookRepository: BookRepository
        private set

    lateinit var readerRepository: Deferred<ReaderRepository>
        private set

    private val coroutineScope: CoroutineScope =
        MainScope()

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        Timber.plant(Timber.DebugTree())

        readium = Readium(this)

        storageDir = computeStorageDir()

        /*
         * Initializing repositories
         */
        bookRepository =
            BookDatabase.getDatabase(this).booksDao()
                .let { BookRepository(it) }

        readerRepository =
            coroutineScope.async {
                ReaderRepository(
                    this@ReadiumApplication,
                    readium,
                    bookRepository,
                    navigatorPreferences
                )
            }
    }

    private fun computeStorageDir(): File {
        val properties = Properties()
        val inputStream = assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir =
            properties.getProperty("useExternalFileDir", "false")!!.toBoolean()

        return File(
            if (useExternalFileDir) getExternalFilesDir(null)?.path + "/"
            else filesDir?.path + "/"
        )
    }
}

val Context.resolver: ContentResolver
    get() = applicationContext.contentResolver
