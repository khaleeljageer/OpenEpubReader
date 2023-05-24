package com.jskhaleel.openepubreader

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.epubreader.android.ReadiumConfig
import com.epubreader.android.utils.onFailure
import com.epubreader.android.utils.onSuccess
import com.jskhaleel.openepubreader.ui.theme.OpenEpubReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

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
                scope.launch(Dispatchers.IO) {
                    val config = ReadiumConfig(context)
                    config
                        .importBookFromAsset("1946.epub")
                        .onSuccess {
                            Log.d("Khaleel", "onSuccess $it")
                            config.openBook(it, context as Activity)
                        }
                        .onFailure {
                            Log.d("Khaleel", "onFailure ${it.message}")
                        }
                }
            },
        ) {
            Text(text = "Open book")
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


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OpenEpubReaderTheme {
        MainPage()
    }
}