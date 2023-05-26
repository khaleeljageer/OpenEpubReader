package com.epubreader.android

interface Reader {
    suspend fun openBook(id: String)
    suspend fun deleteBook(id: String)
}