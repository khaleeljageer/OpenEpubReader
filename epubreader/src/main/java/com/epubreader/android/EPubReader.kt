package com.epubreader.android

import android.content.Context

object EPubReader {
    private var readiumConfig: ReadiumConfig? = null

    fun init(context: Context) {
        readiumConfig = ReadiumConfig(context)
    }


    fun getReader(): Operation {
        readiumConfig ?: throw IllegalStateException("Readium config not initiated")
        return OperationImpl(readiumConfig!!)
    }
}