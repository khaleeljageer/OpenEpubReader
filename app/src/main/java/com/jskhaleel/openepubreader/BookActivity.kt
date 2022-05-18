package com.jskhaleel.openepubreader

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.jskhaleel.openepubreader.ui.theme.OpenEpubReaderTheme
import com.jskhaleel.openepubreader.viewmodel.BookViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by Khaleel Jageer on 14/05/22.
 */
@AndroidEntryPoint
class BookActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenEpubReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    BookPage()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun BookPage(bookViewModel: BookViewModel = hiltViewModel()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            bookViewModel.loadBook(LocalContext.current)
            when (bookViewModel.links.isEmpty()) {
                true -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(55.dp)
                            .width(55.dp)
                    )
                }
                else -> {
                    val baseUrl = bookViewModel.baseUrl
                    val mUrl = baseUrl + bookViewModel.links[3].href
                    AndroidView(factory = {
                        WebView(it).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            this.webViewClient = WebViewClient()
                            this.isVerticalScrollBarEnabled = false
                            this.isHorizontalScrollBarEnabled = false
                            this.settings.apply {
                                this.javaScriptEnabled = true

                                this.builtInZoomControls = true
                            }
                            this.setPadding(0, 0, 0, 0)
                            this.isHapticFeedbackEnabled = false
                            this.isLongClickable = false

                            loadUrl(mUrl)
                        }
                    }, update = {
                        it.loadUrl(mUrl)
                    }, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}