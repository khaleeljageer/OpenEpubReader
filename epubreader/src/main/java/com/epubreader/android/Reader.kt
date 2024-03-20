package com.epubreader.android

interface Reader {
    suspend fun openBook(type: FileType)
    suspend fun deleteBook(id: String)
}

sealed interface FileType {
    data class Url(val url: String) : FileType
    data class Storage(val uri: String) : FileType
    data class Asset(val uri: String) : FileType
}