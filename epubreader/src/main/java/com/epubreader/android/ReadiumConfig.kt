package com.epubreader.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import com.epubreader.android.data.BookRepository
import com.epubreader.android.data.ReaderRepository
import com.epubreader.android.reader.ReaderActivity
import com.epubreader.android.reader.Readium
import com.epubreader.android.utils.LocalReaderError
import com.epubreader.android.utils.ReaderResult
import com.epubreader.android.utils.extensions.computeStorageDir
import com.epubreader.android.utils.extensions.copyToTempFile
import com.epubreader.android.utils.extensions.moveTo
import com.google.android.material.color.DynamicColors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.shared.extensions.mediaType
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadiumConfig @Inject constructor(
    private val context: Context
) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReadiumConfigEntryPoint {
        fun provideReadium(): Readium
        fun provideCoroutineScope(): CoroutineScope
        fun getBookRepository(): BookRepository
        fun getReaderRepository(): ReaderRepository
    }

    private val hiltEntryPoint = EntryPointAccessors.fromApplication(
        context,
        ReadiumConfigEntryPoint::class.java
    )

    private val storageDir = context.computeStorageDir()

    private fun getReadium(): Readium {
        return hiltEntryPoint.provideReadium()
    }

    private fun getBookRepository(): BookRepository {
        return hiltEntryPoint.getBookRepository()
    }

    private fun getReaderRepository(): ReaderRepository {
        return hiltEntryPoint.getReaderRepository()
    }

    private fun provideCoroutineScope(): CoroutineScope {
        return hiltEntryPoint.provideCoroutineScope()
    }


    init {
        DynamicColors.applyToActivitiesIfAvailable(context.applicationContext as Application)
        Timber.plant(Timber.DebugTree())
    }

    suspend fun importBookFromAsset(fileName: String): ReaderResult<Long> {
        val tempFile = (context.applicationContext as Application)
            .assets.open(fileName)
            .copyToTempFile(storageDir)
        return if (tempFile != null) {
            importPublication(tempFile)
        } else ReaderResult.Failure(LocalReaderError("Files seems to be corrupted"))
    }

    suspend fun importBookFromUri(uri: Uri): ReaderResult<Long> {
        val tempFile = uri.copyToTempFile(context.applicationContext, storageDir)
        return if (tempFile != null) {
            importPublication(tempFile)
        } else ReaderResult.Failure(LocalReaderError("Files seems to be corrupted"))
    }

    private suspend fun importPublication(
        sourceFile: File
    ): ReaderResult<Long> {
        val sourceMediaType = sourceFile.mediaType()
        val publicationAsset: FileAsset =
            if (sourceMediaType != MediaType.LCP_LICENSE_DOCUMENT)
                FileAsset(sourceFile, sourceMediaType)
            else {
                getReadium().lcpService
                    .flatMap { it.acquirePublication(sourceFile) }
                    .fold(
                        {
                            val mediaType =
                                MediaType.of(fileExtension = File(it.suggestedFilename).extension)
                            FileAsset(it.localFile, mediaType)
                        },
                        {
                            tryOrNull { sourceFile.delete() }
                            Timber.d(it)
                            return ReaderResult.Failure(LocalReaderError("ImportPublicationFailed ${it.message}"))
                        }
                    )
            }

        val mediaType = publicationAsset.mediaType()
        val fileName = "${UUID.randomUUID()}.${mediaType.fileExtension}"
        val libraryAsset = FileAsset(File(storageDir, fileName), mediaType)

        try {
            publicationAsset.file.moveTo(libraryAsset.file)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrNull { publicationAsset.file.delete() }
            return ReaderResult.Failure(LocalReaderError("UnableToMovePublication"))
        }

        return getPublicationId(libraryAsset)
    }

    private suspend fun getPublicationId(libraryAsset: FileAsset): ReaderResult<Long> {
        getReadium().streamer.open(libraryAsset, allowUserInteraction = false)
            .fold(
                onSuccess = { publication ->
                    addPublicationToDatabase(
                        libraryAsset.file.path,
                        libraryAsset.mediaType(),
                        publication
                    ).let { id ->
                        return if (id != -1L) {
                            ReaderResult.Success(id)
                        } else {
                            ReaderResult.Failure(LocalReaderError("ImportDatabaseFailed"))
                        }
                    }
                },
                onFailure = {
                    tryOrNull { libraryAsset.file.delete() }
                    Timber.d(it)
                    return ReaderResult.Failure(
                        LocalReaderError(
                            "ImportPublicationFailed ${it.message}"
                        )
                    )
                }
            )
    }

    suspend fun openBook(bookId: Long, activity: Activity): ReaderResult<Unit> {
        getReaderRepository().open(bookId, activity).fold(
            onSuccess = {
                launchReaderActivity(context = activity)
                return ReaderResult.Success(Unit)
            },
            onFailure = {
                return ReaderResult.Failure(LocalReaderError(it.message ?: "Unknown error"))
            }
        )
    }

    private fun launchReaderActivity(context: Context) {
        val intent = Intent(context, ReaderActivity::class.java)
        intent.putExtra("book_id", 1)
        context.startActivity(intent)
    }

    private suspend fun addPublicationToDatabase(
        href: String,
        mediaType: MediaType,
        publication: Publication
    ): Long {
        val id = getBookRepository().insertBook(href, mediaType, publication)
        storeCoverImage(publication, id.toString())
        return id
    }

    private fun storeCoverImage(
        publication: Publication,
        imageName: String,
    ) {
        provideCoroutineScope().launch(Dispatchers.IO) {
            val coverImageDir = File(storageDir, "covers/")
            if (!coverImageDir.exists()) {
                coverImageDir.mkdirs()
            }
            val coverImageFile = File(storageDir, "covers/$imageName.png")

            val bitmap: Bitmap? = publication.cover()

            val resized = bitmap?.let { Bitmap.createScaledBitmap(it, 120, 200, true) }
            val fos = FileOutputStream(coverImageFile)
            resized?.compress(Bitmap.CompressFormat.PNG, 80, fos)
            fos.flush()
            fos.close()
        }
    }
}
