package com.jskhaleel.openepubreader.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.streamer.Streamer
import org.readium.r2.streamer.server.Server
import java.io.File
import java.io.IOException
import java.net.URL

/**
 * Created by Khaleel Jageer on 18/05/22.
 */
@HiltViewModel
class BookViewModel : ViewModel() {
    private val _links = mutableStateOf<List<Link>>(emptyList())
    val links by _links

    var baseUrl = ""

    fun loadBook(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val streamer = Streamer(context)
            val server = Server(8080, context = context)

            if (!server.isAlive) {
                try {
                    server.start()
                }catch (e: Exception) {
                    Log.d("Khaleel", "Exception : $e")
                }
            }

            val file: File = getFileFromAssets(context, "1946.epub")
            val asset = FileAsset(file)
            val publication: Publication = streamer.open(
                asset = asset, credentials = null, allowUserInteraction = true, sender = context
            ).getOrThrow()

            val links: List<Link> = publication.readingOrder
            Log.d("Khaleel", "links : $links")

            val url: URL? = server.addPublication(publication, null)
            Log.d("Khaleel", "links : $url")
            baseUrl = url.toString()
            _links.value = links
        }
    }
}

@Throws(IOException::class)
fun getFileFromAssets(context: Context, fileName: String): File = File(context.cacheDir, fileName)
    .also {
        if (!it.exists()) {
            it.outputStream().use { cache ->
                context.assets.open(fileName).use { inputStream ->
                    inputStream.copyTo(cache)
                }
            }
        }
    }


fun Link.withBaseUrl(baseUrl: String): Link {
    // Already an absolute URL?
    if (Uri.parse(href).scheme != null) {
        return this
    }

    check(!baseUrl.endsWith("/"))
    check(href.startsWith("/"))
    return copy(href = baseUrl + href)
}