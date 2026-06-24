package com.posbah.app.data.local

// ─────────────────────────────────────────────────────────────────────────────
// LocalDataSeeder.kt — Full Online mode STUB
// LocalDataSeeder tidak lagi berfungsi dalam full-online mode.
// Semua seeding dilakukan di VPS, bukan lokal.
// File ini dipertahankan agar kompilasi tidak gagal.
// ─────────────────────────────────────────────────────────────────────────────

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataSeeder @Inject constructor() {

    /** No-op: seeding dilakukan di VPS */
    suspend fun seedDefaultSettings(tenantId: String) {
        android.util.Log.i("LocalDataSeeder", "seedDefaultSettings() — no-op in full-online mode.")
    }

    /** No-op: tidak ada SQL dump lokal */
    suspend fun seedFromSqlDump(context: Context, tenantId: String, outletId: Long?) {
        android.util.Log.i("LocalDataSeeder", "seedFromSqlDump() — no-op in full-online mode.")
    }

    /** No-op: seeding kendaraan dilakukan di VPS */
    suspend fun seedDefaultVehicles(tenantId: String, outletId: Long?) {
        android.util.Log.i("LocalDataSeeder", "seedDefaultVehicles() — no-op in full-online mode.")
    }

    /** No-op: seeding laundry dilakukan di VPS */
    suspend fun seedDefaultLaundryServices(tenantId: String, outletId: Long?) {
        android.util.Log.i("LocalDataSeeder", "seedDefaultLaundryServices() — no-op in full-online mode.")
    }
}
