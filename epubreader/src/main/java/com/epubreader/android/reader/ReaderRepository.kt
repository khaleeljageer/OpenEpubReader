/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.epubreader.android.reader

import android.app.Activity
import android.app.Application
import androidx.datastore.core.DataStore
import com.epubreader.android.Readium
import com.epubreader.android.data.BookRepository
import com.epubreader.android.reader.preferences.EpubPreferencesManagerFactory
import org.json.JSONObject
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.Try
import java.io.File
import androidx.datastore.preferences.core.Preferences as JetpackPreferences

/**
 * Open and store publications in order for them to be listened or read.
 *
 * Ensure you call [open] before any attempt to start a [ReaderActivity].
 * Pass the method result to the activity to enable it to know which current publication it must
 * retrieve from this rep+ository - media or visual.
 */
@OptIn(ExperimentalReadiumApi::class)
class ReaderRepository(
    private val application: Application,
    private val readium: Readium,
    private val bookRepository: BookRepository,
    private val preferencesDataStore: DataStore<JetpackPreferences>,
) {
    object CancellationException : Exception()

    private val repository: MutableMap<Long, ReaderInitData> =
        mutableMapOf()

    operator fun get(bookId: Long): ReaderInitData? =
        repository[bookId]

    suspend fun open(bookId: Long, activity: Activity): Try<Unit, Exception> {
        return try {
            openThrowing(bookId, activity)
            Try.success(Unit)
        } catch (e: Exception) {
            Try.failure(e)
        }
    }

    private suspend fun openThrowing(bookId: Long, activity: Activity) {
        if (bookId in repository.keys) {
            return
        }

        val book = bookRepository.get(bookId)
            ?: throw Exception("Cannot find book in database.")

        val file = File(book.href)
        require(file.exists())
        val asset = FileAsset(file)

        val publication =
            readium.streamer.open(asset, allowUserInteraction = true, sender = activity)
                .getOrThrow()

        // The publication is protected with a DRM and not unlocked.
        if (publication.isRestricted) {
            throw publication.protectionError
                ?: CancellationException
        }

        val initialLocator = book.progression?.let { Locator.fromJSON(JSONObject(it)) }

        val readerInitData = when {
            publication.conformsTo(Publication.Profile.EPUB) ->
                openEpub(bookId, publication, initialLocator)

            else ->
                throw Exception("Publication is not supported.")
        }

        repository[bookId] = readerInitData
    }

    private suspend fun openEpub(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?
    ): EpubReaderInitData {

        val preferencesManager = EpubPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val navigatorFactory = EpubNavigatorFactory(publication)

        return EpubReaderInitData(
            bookId, publication, initialLocator,
            preferencesManager, navigatorFactory
        )
    }

    fun close(bookId: Long) {
        when (val initData = repository.remove(bookId)) {
            is VisualReaderInitData -> {
                initData.publication.close()
            }

            null, is DummyReaderInitData -> {
                // Do nothing
            }
        }
    }
}
