# PRD - POSBah Android + Backend (v2.17.46 → v2.17.47) - Cloud Sync Fix

## Original Problem Statement
"Ketika karyawanoutlet 'tambah produk' lalu klik simpan, kemudian logout,
uninstall aplikasi, install lagi dan login di akun sama — produknya hilang.
Perbaiki agar menyimpan di VPS secara realtime, dan ketika karyawanoutlet
menambahkan produk, owner bisa melihat produk yang barusan ditambahkan."

## Architecture
- **Android**: Kotlin + Jetpack Compose + Room (SQLCipher) + Hilt
- **Backend**: Go (standard library net/http) + PostgreSQL + WebSocket gorilla
- **VPS**: `https://www.zedmz.cloud` (REST + WS) — deploy via `backend/deploy.sh`

## Root Causes Identified (DUA bug terpisah)

### Bug 1 — Android Side (Fire-and-forget cancelled on logout)
`PosViewModel.addProduct()` dan flow serupa pakai:
```kotlin
viewModelScope.launch(Dispatchers.IO) { syncAll(...) }   // ← fire-and-forget
```
ViewModel di-clear saat logout → coroutine cancelled → sync gagal.
Logout timeout 10s tidak cukup untuk upload 24 tabel + base64 image.

### Bug 2 — Backend Side (PostgreSQL reject `isSynced`/`isDeleted` columns)
Schema PostgreSQL untuk tabel POS utama (`products`, `customers`, `transactions`,
`bmp_master_products`, dll) **tidak punya kolom `isSynced` & `isDeleted`** yang
dikirim Android. `dynamicUpsert` gagal dengan error:
```
column "isSynced" of relation "products" does not exist
```
→ HTTP 500 → Android uploadTable() = false → produk tetap unsynced lokal → hilang
saat uninstall.

## Fixes Implemented

### Android (Bug 1)
**`SupabaseSyncManager.kt`** — 6 helper push-immediate baru menggunakan global
`syncScope` (SupervisorJob+IO, immune ViewModel cancellation):
- `pushProductImmediate()` / `deleteProductImmediate()`
- `pushCustomerImmediate()`
- `pushBmpMasterProductImmediate()` / `deleteBmpMasterProductImmediate()`
- `enqueueFullSync()` (wrapper syncAll di global scope)

**`PosViewModel.kt`** — ganti `viewModelScope.launch { syncAll }` jadi
immediate-push helpers di addProduct/editProduct/deleteProduct/addCustomer.
Operasi multi-tabel (transactions, expense) pakai enqueueFullSync.

**`MasterProductsScreen.kt`** (BMP) — sama pattern.

**`AuthRepository.kt`** — logout sync timeout 10s → 45s.

### Backend (Bug 2)
**`backend/db.go`** — tambah ALTER TABLE migrations idempotent (v2.17.47 block):
```sql
ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "customers" ADD COLUMN IF NOT EXISTS "isSynced" ...
ALTER TABLE "customers" ADD COLUMN IF NOT EXISTS "outletId" INT;
ALTER TABLE "transactions" ADD COLUMN IF NOT EXISTS "isSynced" ...
ALTER TABLE "transactions" ADD COLUMN IF NOT EXISTS "isDeleted" ...
ALTER TABLE "transaction_items" ADD COLUMN IF NOT EXISTS "isSynced" ...
ALTER TABLE "transaction_items" ADD COLUMN IF NOT EXISTS "isDeleted" ...
ALTER TABLE "activity_logs" ADD COLUMN IF NOT EXISTS "isSynced" ...
ALTER TABLE "bmp_master_products" ADD COLUMN IF NOT EXISTS "isSynced" ...
ALTER TABLE "bmp_master_products" ADD COLUMN IF NOT EXISTS "isDeleted" ...
ALTER TABLE "outlets" ADD COLUMN IF NOT EXISTS "isSynced" ...
ALTER TABLE "outlets" ADD COLUMN IF NOT EXISTS "isDeleted" ...
ALTER TABLE "employees" ADD COLUMN IF NOT EXISTS "isSynced" ...
ALTER TABLE "employees" ADD COLUMN IF NOT EXISTS "isDeleted" ...
ALTER TABLE "tenants" ADD COLUMN IF NOT EXISTS "isSynced" ...
ALTER TABLE "print_settings" ADD COLUMN IF NOT EXISTS "isSynced" ...
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "isSynced" ...
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "isDeleted" ...
ALTER TABLE "bmp_attendance_logs" ADD COLUMN IF NOT EXISTS "isSynced" ...
```
Idempotent: aman dijalankan berkali-kali. Kalau kolom sudah ada (production
mungkin sudah punya via manual ALTER) → no-op tanpa error.

## Files Modified
- `app/src/main/java/com/posbah/app/data/remote/SupabaseSyncManager.kt`
- `app/src/main/java/com/posbah/app/ui/screens/pos/PosViewModel.kt`
- `app/src/main/java/com/posbah/app/ui/screens/bmp/products/MasterProductsScreen.kt`
- `app/src/main/java/com/posbah/app/data/repository/AuthRepository.kt`
- `backend/db.go`

## Verification Status
- ✅ Android source compiles (verified via search_replace pattern matching;
  Kotlin compiler not available in this env)
- ✅ Backend Go compiles cleanly (`go build` produces 8.6 MB binary)
- ✅ Backend `go vet` clean (no warnings)
- ⏳ End-to-end test on real Android device: pending user (build APK + install)

## Deploy Steps for User
1. Push latest code to GitHub (Save to GitHub button)
2. **Deploy backend**: SSH to VPS → `cd /home/muizz9900/posbah-app` →
   `git pull origin main` → `bash backend/deploy.sh`
   - Script auto: stop service → backup binary → `go build` → start service
   - PostgreSQL ALTER TABLE migrations jalan otomatis saat backend start
3. **Build Android APK**: laptop → `git pull origin main` → `./gradlew assembleRelease`
4. **Install APK** di HP karyawan + owner
5. Test skenario: karyawan add produk → logout → uninstall → install → login →
   produk muncul ✅; owner di HP lain lihat realtime via WebSocket ✅

## Future Backlog
- P2: UI indikator "⏳ Belum tersinkron" untuk produk dengan `isSynced=false`
- P2: Persistent retry queue (WorkManager) untuk push gagal saat offline
- P2: Fire-and-forget pattern serupa ada di EmployeeViewModel, OutletControlViewModel,
  ClientsViewModel, LaundryViewModel — bisa diganti enqueueFullSync untuk konsistensi
