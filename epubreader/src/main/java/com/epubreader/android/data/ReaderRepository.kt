package com.epubreader.android.data

import android.app.Activity
import com.epubreader.android.reader.ReaderInitData
import org.readium.r2.shared.util.Try

interface ReaderRepository {
    suspend fun open(bookId: Long, activity: Activity): Try<Unit, Exception>
    fun close(bookId: Long)
    fun getReaderInit(bookId: Long): ReaderInitData
}