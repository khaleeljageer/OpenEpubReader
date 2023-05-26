package com.epubreader.android

interface Operation {
    suspend fun open(id: String)
}