package com.posbah.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entrypoint. Hilt initializes the singleton component here.
 * Heavy work (key derivation, integrity check) is deferred to the first
 * splash screen viewmodel to keep app cold-start fast.
 */
@HiltAndroidApp
class PosBahApp : Application() {
    override fun onCreate() {
        super.onCreate()
        cleanLegacyDatabases()
    }

    private fun cleanLegacyDatabases() {
        try {
            val dbs = databaseList()
            if (!dbs.isNullOrEmpty()) {
                dbs.forEach { dbName ->
                    val deleted = deleteDatabase(dbName)
                    android.util.Log.i("PosBahApp", "Deleted legacy database $dbName: $deleted")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PosBahApp", "Failed to clean legacy databases", e)
        }
    }
}
