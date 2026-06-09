# POSBah — Full Native Android (Kotlin + Jetpack Compose)

Versi `2.0.0` ini adalah **penulisan ulang lengkap** dari aplikasi POSBah ke
arsitektur **full native Android**, menggantikan stack Capacitor + Node.js + PostgreSQL
sebelumnya. Tujuan utama: **keamanan tinggi, smooth, fast, offline-first**, siap di
deploy ke Play Store.

> Modul yang diimplementasikan pada iterasi ini: **BMP (Invoice & Manufaktur)**.
> Modul lain (Kasir F&B, Rental Mobil, Laundry) akan dimigrasikan pada iterasi berikutnya.

---

## Tech Stack

| Layer | Pilihan |
|---|---|
| Bahasa | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.10.01), Material 3 |
| Arsitektur | MVVM + Repository + StateFlow |
| DI | Dagger Hilt 2.52 |
| Database | Room 2.6.1 + **SQLCipher 4.5.4** (terenkripsi) |
| Auth | Google Sign-In via Credential Manager + Auth0 JWT |
| Security | Android Keystore, EncryptedSharedPreferences, Biometric, Play Integrity |
| Build | AGP 8.7.3, Gradle 8.10.2, JDK 17 |

---

## Stack Keamanan (Anti pencurian data & replikasi)

1. **Database terenkripsi at-rest** — Room di atas SQLCipher. Passphrase 32 byte
   di-generate sekali per device, di-enkripsi dengan AES-GCM 256 di Android
   Keystore (hardware-backed bila tersedia), lalu disimpan di
   `EncryptedSharedPreferences`. **DB tidak bisa dibuka di device lain.**
2. **Session terenkripsi** — `EncryptedSharedPreferences` (AES-256 GCM + AES-SIV).
3. **PIN karyawan** — PBKDF2-HMAC-SHA256, 120.000 iterasi, per-record salt,
   constant-time compare.
4. **Biometric lock** — `BiometricPrompt` (Strong + DEVICE_CREDENTIAL fallback)
   setiap kali aplikasi resume.
5. **Anti-root / anti-emulator / anti-Frida** — `DeviceIntegrityGuard` memeriksa
   path `su`, build tags, Magisk mount, Frida library, Xposed packages. Aplikasi
   **menolak start** bila terdeteksi.
6. **Play Integrity API** — verdict token diambil opportunistic untuk audit
   integritas APK & install dari Play.
7. **Anti-screenshot** — `FLAG_SECURE` aktif di semua activity.
8. **Auto-Backup dinonaktifkan** — DB dan secure prefs di-exclude dari Google
   Backup dan Device Transfer.
9. **R8/ProGuard obfuscation** — kode rilis di-shrink, di-rename
   (`repackageclasses`), debug logs dihapus.
10. **Lockout** — 5 kali PIN gagal → lock 5 menit, disimpan terenkripsi.

---

## Struktur Project

```
android/
├── build.gradle.kts           (Kotlin DSL, version catalog plugin refs)
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml     (version catalog — semua dependency versions)
│   └── wrapper/
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/               (strings.xml, colors.xml, themes.xml, splash, dll)
        └── java/com/posbah/app/
            ├── PosBahApp.kt              # Application (Hilt entrypoint)
            ├── MainActivity.kt           # Single-activity, FragmentActivity, FLAG_SECURE
            ├── auth/                     # Google Sign-In (Credential Manager)
            ├── data/
            │   ├── local/                # Room database + entities + DAOs
            │   └── repository/           # Repository layer
            ├── di/                       # Hilt modules
            ├── security/                 # Keystore, Biometric, Integrity, PinHasher
            ├── ui/
            │   ├── theme/                # Material 3 theme (Charcoal + Saffron)
            │   ├── components/           # Shared UI (PrimaryButton, TopBar, dll)
            │   ├── navigation/           # Screen routes
            │   ├── PosBahRoot.kt         # NavHost
            │   └── screens/
            │       ├── splash/
            │       ├── login/
            │       ├── lock/             # Biometric unlock
            │       ├── tenant/           # Multi-tenant picker
            │       └── bmp/              # BMP module
            │           ├── dashboard/
            │           ├── clients/
            │           ├── invoices/
            │           ├── products/
            │           ├── payments/
            │           ├── cashflow/
            │           ├── employees/    # juga PayrollScreen
            │           └── settings/     # juga Outlet management
            └── util/                     # Formatters (Rp, tanggal)
```

---

## Cara Build

### Prasyarat
- **Android Studio** Koala/Jellyfish (atau lebih baru)
- **JDK 17**
- **Android SDK 36** (compile + target)
- Akun Google Play Console (untuk SHA-1 fingerprint final)

### Build Debug APK
```bash
cd android
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK / AAB
1. Generate keystore (sekali):
   ```bash
   keytool -genkey -v -keystore android/keystore/release.jks \
     -alias posbah -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Set environment variables:
   ```bash
   export POSBAH_KEYSTORE_PASSWORD=...
   export POSBAH_KEY_ALIAS=posbah
   export POSBAH_KEY_PASSWORD=...
   ```
3. Build:
   ```bash
   ./gradlew assembleRelease        # APK
   ./gradlew bundleRelease          # AAB (untuk Play Store)
   ```
Output:
- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

---

## Konfigurasi Google Sign-In (Sudah Ter-set)

- **Package**: `com.posbah.app`
- **Web Client ID**: `119416648055-06ujev0c1onnv8fs67no28dqhlca93fm.apps.googleusercontent.com`
  (di-inject via `buildConfigField` di `app/build.gradle.kts`)
- **SHA-1 Fingerprint** (yang harus didaftarkan di Google Cloud Console untuk
  package Anda):
  - Debug key: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`
  - Release key: dari `release.jks` Anda (atau Play App Signing dari Play Console)

---

## Alur Aplikasi

1. **Splash** → cek device integrity, route ke Login atau Lock.
2. **Login** → Google SSO (Credential Manager) atau PIN karyawan.
3. **Tenant Picker** → bila user memiliki lebih dari 1 tenant.
4. **BMP Dashboard** → entry point modul, menampilkan agregat & navigasi ke modul.
5. **Modul BMP**:
   - Klien (CRUD)
   - Invoice (CRUD + multi-line items + payment tracking)
   - Master Produk (CRUD)
   - Pembayaran (view + auto-recorded dari invoice)
   - Arus Kas (CRUD masuk/keluar + auto-link dari payment)
   - Karyawan (CRUD + bayar gaji)
   - Penggajian (riwayat payroll)
   - Pengaturan (profil + multi-outlet management)

---

## Multi-Tenant & Multi-Outlet

- Setiap data carry `tenantId` (string) — pemisahan logis di level query.
- Tenant aktif disimpan di encrypted session prefs.
- Setiap tenant punya 0+ outlet; ID outlet aktif juga di session.
- Saat login Google pertama kali, default tenant + outlet utama dibuat
  otomatis.
- Pengguna bisa membuat tenant tambahan dari Tenant Picker.

---

## Roadmap (Iterasi Berikutnya)

- Modul Kasir F&B (transaksi, barcode scan, struk thermal)
- Modul Rental Mobil
- Modul Laundry
- Export PDF invoice (native, tanpa server)
- Print Bluetooth thermal printer
- Backup terenkripsi ke Google Drive (opsional, user-controlled)
- Sync multi-device (opsional, jika dibutuhkan)
