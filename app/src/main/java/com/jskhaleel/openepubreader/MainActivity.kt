package com.jskhaleel.openepubreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.epubreader.android.reader.ReaderActivity
import com.epubreader.android.utils.extensions.copyToTempFile
import com.jskhaleel.openepubreader.ui.theme.OpenEpubReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenEpubReaderTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainPage()
                }
            }
        }
    }
}


@Composable
fun MainPage(

) {
    val context = LocalContext.current
    val application = LocalContext.current.applicationContext as TestApp
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                val intent = Intent(context, ReaderActivity::class.java)

                scope.launch {
                    importPublication(
                        application = application,
                        uri = Uri.parse("file:///android_asset/1946.epub")
                    )
                }
                intent.putExtra("book_id")
                context.startActivity(intent)
            },
        ) {
            Text(text = "Open book")
        }
    }
}

suspend fun importPublication(application: TestApp, uri: Uri) {
    uri.copyToTempFile(application, application.storageDir)
        ?.let {
            importPublication(it)
        }
}

private suspend fun importPublication(
    sourceFile: File
) {
    val sourceMediaType = sourceFile.mediaType()
    val publicationAsset: FileAsset =
        if (sourceMediaType != MediaType.LCP_LICENSE_DOCUMENT)
            FileAsset(sourceFile, sourceMediaType)
        else {
            app.readium.lcpService
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
                        channel.send(Event.ImportPublicationFailed(it.message))
                        return
                    }
                )
        }

    val mediaType = publicationAsset.mediaType()
    val fileName = "${UUID.randomUUID()}.${mediaType.fileExtension}"
    val libraryAsset = FileAsset(File(app.storageDir, fileName), mediaType)

    try {
        publicationAsset.file.moveTo(libraryAsset.file)
    } catch (e: Exception) {
        Timber.d(e)
        tryOrNull { publicationAsset.file.delete() }
        channel.send(Event.UnableToMovePublication)
        return
    }

    app.readium.streamer.open(libraryAsset, allowUserInteraction = false)
        .onSuccess {
            addPublicationToDatabase(libraryAsset.file.path, libraryAsset.mediaType(), it).let { id ->

                if (id != -1L)
                    channel.send(Event.ImportPublicationSuccess)
                else
                    channel.send(Event.ImportDatabaseFailed)
            }
        }
        .onFailure {
            tryOrNull { libraryAsset.file.delete() }
            Timber.d(it)
            channel.send(Event.ImportPublicationFailed(it.getUserMessage(app)))
        }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OpenEpubReaderTheme {
        MainPage()
    }
}