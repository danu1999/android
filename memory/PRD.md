# PRD - POSBah Android (v2.17.46) - Cloud Sync Fix

## Original Problem Statement
"Repo Android: ketika karyawanoutlet 'tambah produk' lalu klik simpan, kemudian
logout, uninstall, install ulang dan login di akun sama — produknya hilang.
Perbaiki agar menyimpan di VPS secara realtime, dan ketika karyawanoutlet
menambahkan produk, owner bisa melihat produk yang barusan ditambahkan."

## Architecture
- Android Kotlin + Jetpack Compose + Room (SQLCipher) + Hilt
- VPS: Go + PostgreSQL + REST (`https://www.zedmz.cloud/api/sync/*`) + WebSocket
- Multi-tenant via `tenantId`; karyawan dikunci ke `outletId`
- Realtime: WebSocket `sync_trigger` → trigger `pullAll` di semua device aktif

## User Personas
- **Owner** (Google Sign-In) — multi-outlet, bisa switch outlet & lihat semua
- **Karyawan Outlet** (Email/Password) — terkunci ke outlet tertentu

## Root Cause yang Diperbaiki

`PosViewModel.addProduct()`, `editProduct()`, `deleteProduct()`, `addCustomer()`,
dll. semuanya menggunakan pola fire-and-forget:

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    SupabaseSyncManager.syncAll(...)   // ✗ ikut dibatalkan saat ViewModel di-clear
}
```

Saat user klik Simpan lalu LANGSUNG logout:
1. `productRepository.upsert(p)` simpan lokal `isSynced=false`
2. `viewModelScope.launch { syncAll }` BARU saja mulai
3. `logout()` → ViewModel di-clear → coroutine dibatalkan
4. `logout()`'s own `syncAll` punya timeout **hanya 10 detik** — tidak cukup
   untuk mengunggah 24 tabel dengan base64 image produk yang besar
5. `db.clearAllTables()` wipe lokal
6. Uninstall → install ulang → `pullAll` → tabel `products` di VPS kosong → produk hilang

## Fix Implemented (Jan 2026)

### 1. `SupabaseSyncManager.kt` — Tambah 6 helper baru di global `syncScope`:
- `pushProductImmediate()` — push 1 produk POS langsung ke `products`
- `deleteProductImmediate()` — DELETE 1 produk di VPS + hardDelete lokal
- `pushCustomerImmediate()` — push 1 customer POS
- `pushBmpMasterProductImmediate()` — push 1 master produk BMP
- `deleteBmpMasterProductImmediate()` — DELETE 1 master produk BMP
- `enqueueFullSync()` — wrapper `syncAll()` di global scope (untuk operasi multi-tabel)

Semua helper menggunakan `syncScope = CoroutineScope(IO + SupervisorJob())` (singleton
object) sehingga **tidak ikut dibatalkan** ketika ViewModel di-clear pada logout/navigate.

### 2. `PosViewModel.kt` — Ganti fire-and-forget dengan immediate push:
- `addProduct` / `editProduct` → `pushProductImmediate(newId)`
- `deleteProduct` → `deleteProductImmediate(productId)`
- `addCustomer` → `pushCustomerImmediate(newCustomerId)`
- `settlePiutang`, `deleteTransaction`, `editTransaction`, `addExpense`,
  `cancelQueue` → `enqueueFullSync()` (perubahan multi-tabel)

### 3. `MasterProductsScreen.kt` (BMP) — Same pattern:
- `save` → `pushBmpMasterProductImmediate(savedId)`
- `delete` → `deleteBmpMasterProductImmediate(id)`

### 4. `AuthRepository.kt` — Logout timeout 10s → 45s:
Safety net agar `syncAll` saat logout punya cukup waktu untuk mengunggah data
besar (mis. produk dengan image base64 yang banyak) sebelum `clearAllTables()`.

## How Realtime Owner View Works (sudah ada, tidak perlu diubah)
1. Karyawan klik Simpan → `pushProductImmediate` upload ke VPS
2. VPS PostgreSQL menerima → broadcast `sync_trigger` ke WebSocket client lain
   dengan `tenantId` sama
3. App Owner (di HP berbeda) menerima `sync_trigger` di `WebSocketSyncClient.onMessage`
4. Trigger `pullAll` + `syncAll` → produk baru masuk ke local Room
5. UI Compose otomatis recompose via `StateFlow` → produk tampil tanpa refresh manual

## Backlog / Future
- P2: Tampilkan UI indikator "Belum tersinkron" untuk produk dengan `isSynced=false`
- P2: Retry queue persistent (WorkManager) untuk produk yang gagal push saat offline
- P3: Compression on-server untuk image base64 (sekarang langsung disimpan apa adanya)

## Testing Status
- Static analysis: ✓ (file diedit dengan search_replace, syntax preserved)
- Build verification: tidak tersedia di environment (gradle/kotlinc not installed)
- Action item user: Build APK dengan `./gradlew assembleDebug` di mesin lokal, lalu
  test skenario asli: tambah produk → logout → uninstall → install → login →
  verify produk muncul.
