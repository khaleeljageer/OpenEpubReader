package com.epubreader.android

class ReaderImpl(private val readerConfig: ReaderConfig) : Reader {
    override suspend fun openBook(type: FileType) {
        when (type) {
            is FileType.Asset -> {
                readerConfig.importBookFromAsset(type.uri)
            }
            is FileType.Storage -> {

            }

            is FileType.Url -> {

            }
        }
    }

    override suspend fun deleteBook(id: String) {

    }
}