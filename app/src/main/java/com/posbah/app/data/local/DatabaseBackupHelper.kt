package com.posbah.app.data.local

import android.content.Context
import android.util.Log
import java.io.File

object DatabaseBackupHelper {
    private const val TAG = "DatabaseBackupHelper"

    /**
     * Membuat salinan database lokal (posbah.db, posbah.db-wal, posbah.db-shm) sebagai cadangan aman.
     */
    fun backupDatabase(context: Context) {
        try {
            val dbFile = context.getDatabasePath(PosBahDatabase.DB_NAME)
            if (!dbFile.exists()) {
                Log.d(TAG, "Database belum dibuat. Melewati backup.")
                return
            }

            val backupFile = File(dbFile.path + ".bak")
            val dbWalFile = File(dbFile.path + "-wal")
            val backupWalFile = File(dbFile.path + "-wal.bak")
            val dbShmFile = File(dbFile.path + "-shm")
            val backupShmFile = File(dbFile.path + "-shm.bak")

            // Salin file utama db
            dbFile.copyTo(backupFile, overwrite = true)
            Log.i(TAG, "Backup file utama database berhasil dibuat.")

            // Salin file WAL jika ada
            if (dbWalFile.exists()) {
                dbWalFile.copyTo(backupWalFile, overwrite = true)
                Log.d(TAG, "Backup file WAL berhasil dibuat.")
            } else if (backupWalFile.exists()) {
                backupWalFile.delete()
            }

            // Salin file SHM jika ada
            if (dbShmFile.exists()) {
                dbShmFile.copyTo(backupShmFile, overwrite = true)
                Log.d(TAG, "Backup file SHM berhasil dibuat.")
            } else if (backupShmFile.exists()) {
                backupShmFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuat backup database: ${e.message}", e)
        }
    }

    /**
     * Memulihkan database lokal dari file cadangan jika terjadi kegagalan inisialisasi / migrasi destruktif.
     */
    fun restoreDatabase(context: Context): Boolean {
        try {
            val dbFile = context.getDatabasePath(PosBahDatabase.DB_NAME)
            val backupFile = File(dbFile.path + ".bak")
            if (!backupFile.exists()) {
                Log.w(TAG, "File backup tidak ditemukan. Tidak bisa memulihkan database.")
                return false
            }

            val dbWalFile = File(dbFile.path + "-wal")
            val backupWalFile = File(dbFile.path + "-wal.bak")
            val dbShmFile = File(dbFile.path + "-shm")
            val backupShmFile = File(dbFile.path + "-shm.bak")

            // Hapus file-file database saat ini terlebih dahulu jika ada
            if (dbFile.exists()) dbFile.delete()
            if (dbWalFile.exists()) dbWalFile.delete()
            if (dbShmFile.exists()) dbShmFile.delete()

            // Salin dari cadangan
            backupFile.copyTo(dbFile, overwrite = true)
            Log.i(TAG, "File utama database berhasil dipulihkan.")

            if (backupWalFile.exists()) {
                backupWalFile.copyTo(dbWalFile, overwrite = true)
                Log.d(TAG, "File WAL berhasil dipulihkan.")
            }

            if (backupShmFile.exists()) {
                backupShmFile.copyTo(dbShmFile, overwrite = true)
                Log.d(TAG, "File SHM berhasil dipulihkan.")
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memulihkan database dari backup: ${e.message}", e)
            return false
        }
    }
}
