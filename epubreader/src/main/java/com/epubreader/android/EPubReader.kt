package com.epubreader.android

import android.app.Application
import android.content.Context

object EPubReader {
    private var readerConfig: ReaderConfig? = null

    private fun init(context: Context) {
        this.readerConfig = ReaderConfig(context.applicationContext as Application)
    }

    fun getReader(): Reader {
        readerConfig ?: throw IllegalStateException("Readium config not initialized")
        return ReaderImpl(readerConfig!!)
    }
}