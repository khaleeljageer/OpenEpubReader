package com.jskhaleel.openepubreader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epubreader.android.ReadiumApplication
import com.epubreader.android.reader.ReaderViewModel
import com.epubreader.android.utils.EventChannel
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    val channel = EventChannel(Channel<ReaderViewModel.Event>(Channel.BUFFERED), viewModelScope)
    val books = app.bookRepository.books()

    private val app get() = getApplication<ReadiumApplication>()
    fun openBook(context: Context) {

    }

}