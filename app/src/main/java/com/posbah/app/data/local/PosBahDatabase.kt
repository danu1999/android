package com.posbah.app.data.local

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosBahDatabase @Inject constructor() {
    suspend fun clearAllTables() {
        android.util.Log.i("PosBahDatabase", "clearAllTables() no-op")
    }
    fun close() {
        android.util.Log.i("PosBahDatabase", "close() no-op")
    }
    suspend fun <T> withTransaction(block: suspend () -> T): T = block()
}
