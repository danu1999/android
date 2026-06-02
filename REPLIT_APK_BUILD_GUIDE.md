# POSBah — Panduan Lengkap Project & Build Android APK (Replit Integration)

> [!IMPORTANT]
> **PERINGATAN UNTUK REPLIT / REPLIT AGENT**:
> Project ini hanya perlu dibuatkan/dikompilasi menjadi **Aplikasi Android saja (APK)**.
> Anda **TIDAK PERLU** menjalankan, menginstal, atau men-deploy server backend Node.js (`/backend`), dan tidak perlu mengonfigurasi database PostgreSQL lokal di Replit.
> Aplikasi klien di Android/Web sudah terhubung langsung (interkoneksi cloud) ke server API produksi aktif di Railway (`https://posbah.up.railway.app/api`).
> Tugas utama Anda di Replit adalah mengompilasi aset frontend React di direktori `/frontend` dan menjalankan kompilasi Gradle untuk menghasilkan file **Android APK** (`.apk`) standalone seperti dijelaskan pada **Bagian 6**.

Dokumen ini menjelaskan arsitektur project, skema database, dependensi, konfigurasi Google OAuth, dan instruksi langkah demi langkah agar Replit atau Replit Agent dapat mengompilasi dan membuat Android APK dari project POSBah.

---

## 1. Ikhtisar Arsitektur Project
Project POSBah dibagi menjadi dua bagian utama:
1. **`backend`**: Node.js Express server menggunakan Prisma ORM dengan database PostgreSQL (dihosting di Railway).
2. **`frontend`**: React (Vite) client, dirancang dengan Tailwind CSS v4, yang dibungkus oleh **Capacitor 8** untuk melahirkan aplikasi native Android (APK).

Struktur Folder Utama:
```text
POSBah/
├── backend/            # Server Express, Prisma ORM, Skema Database
│   ├── prisma/
│   │   └── schema.prisma
│   └── src/
│       └── index.ts
└── frontend/           # Aplikasi React Client & Shell Capacitor
    ├── android/        # Project Android Studio bawaan Capacitor
    ├── src/
    ├── capacitor.config.json
    └── package.json
```

---

## 2. Fitur & Tiga Mode POSBah
POSBah mendukung **3 Mode POS** yang dapat diaktifkan dinamis berdasarkan mode bisnis:
- **Jus & UMKM (Retail/F&B)**: Kelola stok produk, penjualan barcode, diskon kupon, harga grosir, varian produk, nomor antrean, dan sistem kasir.
- **Rental Mobil**: Kelola ketersediaan mobil, plat nomor, status penyewaan, hitung durasi denda keterlambatan secara otomatis.
- **Laundry**: Catat cucian berdasarkan timbangan (Kg) atau satuan (Pcs), lacak status cucian (Menunggu, Proses, Selesai, Diambil), cetak struk laundry, dan kelola pengeluaran laundry.

Sistem otorisasi karyawan dibagi dalam 3 Role:
- **`KASIR`** (Melakukan transaksi kasir dan melihat katalog)
- **`ADMIN`** (Kelola katalog produk, kelola pelanggan, kelola pencatatan keuangan)
- **`OWNER`** (Akses penuh ke seluruh sistem, kelola karyawan/kasir, melihat log aktivitas)

---

## 3. Database Schema (`backend/prisma/schema.prisma`)
Database backend menggunakan database relasional PostgreSQL. Berikut adalah relasi model utamanya:
- **`Employee`**: Menyimpan data kasir/admin. PIN disimpan aman menggunakan Pbkdf2 hashing (panjang string hash 128 karakter).
- **`Customer` & `Supplier`**: Lacak piutang/hutang.
- **`Product`**: Menyimpan detail produk (stok, harga pokok modal, harga jual, JSON varian, dan wholesale price).
- **`Transaction` & `TransactionItem`**: Riwayat transaksi kasir, pre-order, dan detail barang belanja.
- **`Car` & `Rental`**: Entitas pelacak persewaan mobil.
- **`LaundryService` & `LaundryOrder`**: Entitas pelacak operasional kasir laundry.
- **`PremiumUser` & `GoogleUser`**: Autentikasi permanen di database untuk pengguna premium email dan Google SSO.

---

## 4. Konfigurasi Native Android (`frontend/android`)

Folder `frontend/android` berisi project native Gradle yang siap dikompilasi.

### SDK Target (`variables.gradle`)
- **Compile SDK Level**: 36
- **Target SDK Level**: 36
- **Min SDK Level**: 24

### Android Dependencies (`app/build.gradle`)
Menyertakan plugin Google Play Services Auth & Android Credentials untuk login Google SSO Native:
- `com.google.android.gms:play-services-auth:21.3.0`
- `androidx.credentials:credentials:1.3.0`
- `androidx.credentials:credentials-play-services-auth:1.3.0`
- `com.google.android.libraries.identity.googleid:googleid:1.1.1`

### Native Java Code (`MainActivity.java`)
MainActivity mengimplementasikan `ModifiedMainActivityForSocialLoginPlugin` untuk memotong callback native intent dan memproses otorisasi Google Credential Manager menggunakan plugin `@capgo/capacitor-social-login`.

---

## 5. Konfigurasi Google Sign-In & API URL
Agar Google Sign-In native dapat berfungsi di APK, Replit harus mengetahui kunci API dan sertifikat SHA-1:

- **Package Name**: `com.posbah.app`
- **Vite API URL**: `https://posbah.up.railway.app/api`
- **webClientId (dari Google Cloud Console)**:
  `119416648055-06ujev0c1onnv8fs67no28dqhlca93fm.apps.googleusercontent.com`
- **SHA-1 Fingerprint (Debug Key)**:
  `48:53:F9:B1:EE:33:BF:65:5B:DA:CE:33:F1:E9:09:10:57:A6:D9:2C`

---

## 6. Langkah-Langkah Build APK di Replit

Replit atau Replit Agent dapat memicu build APK dengan langkah-langkah di bawah ini. Pastikan environment workspace Replit telah terinstal Android SDK dan JDK 17+.

### Langkah 1: Instalasi Node Modules
Jalankan di root workspace project:
```bash
# Instal modul di root (jika ada)
npm install

# Masuk ke folder frontend dan instal dependensi React + Capacitor
cd frontend
npm install
```

### Langkah 2: Lakukan Build Web Frontend
Vite akan membuat kompilasi aset statis JS/CSS yang dioptimalkan di folder `dist`:
```bash
npm run build
```

### Langkah 3: Sinkronisasi Aset dengan Capacitor Shell
Perintah ini akan menyalin seluruh folder `dist` ke dalam aset Android native (`android/app/src/main/assets/public`) dan memperbarui dependensi plugin:
```bash
npx cap sync
```

### Langkah 4: Jalankan Kompilasi Gradle
Masuk ke direktori android dan gunakan Gradle Wrapper (`gradlew`) untuk mengompilasi APK debug:
```bash
cd android
./gradlew assembleDebug
```
*(Gunakan `gradlew.bat assembleDebug` jika berada di host OS Windows).*

### Langkah 5: Lokasi File Hasil Build APK
Setelah Gradle selesai melakukan kompilasi dengan sukses, file Android APK (`.apk`) dapat diunduh di lokasi:
`frontend/android/app/build/outputs/apk/debug/app-debug.apk`
