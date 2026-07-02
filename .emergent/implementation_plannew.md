# Implementation Plan — v2.19.21: Absensi Harian Mesin (3 Shift), Pelacakan Matras & Integrasi Penggajian Presisi

> [!IMPORTANT]
> ✅ **Backup database VPS sudah dibuat**: `posbah_backup_2026-07-02_002520.dump` (2 Juli 2026, 00:25 WIB)

Dokumen ini merancang perubahan komprehensif untuk modul produksi harian, mesin, matras (cetakan), absensi kerja per shift, serta integrasi data absensi ke penggajian karyawan dan perhitungan HPP presisi.

---

## 📌 Alur Relasi Keuangan Baru (Interkoneksi End-to-End)

```
┌─────────────────────────────────┐
│     Halaman Log Produksi        │
│   (Shift Pagi/Sore/Malam)       │
├─────────────────────────────────┤
│ Catat produksi & reject         │
│ Catat absensi operator di mesin │
└───────────────┬─────────────────┘
                │
                │ 1. Kirim absensi & pemakaian
                ▼
┌─────────────────────────────────┐       ┌─────────────────────────────────┐
│     Database & Backend          │       │        Halaman Matras           │
├─────────────────────────────────┤       ├─────────────────────────────────┤
│ Simpan log & hitung jam kerja   │──────►│ Akumulasi otomatis usage_count  │
└───────────────┬─────────────────┘       │ ⚠️ Notifikasi Servis jika >= 90% │
                │                         └───────────────┬─────────────────┘
                │ 2. Tarik rekap absensi                  │
                ▼                                         │ 4. Penyusutan Matras
┌─────────────────────────────────┐                       │    (Harga / Lifetime)
│       Halaman Penggajian        │                       ▼
├─────────────────────────────────┤       ┌─────────────────────────────────┐
│ "Isi Otomatis" total hari kerja │       │      Kalkulasi HPP Presisi      │
│ & upah dari absensi produksi    │──────►│ 1. Alokasikan Gaji Operator riil│
└───────────────┬─────────────────┘       │    spesifik ke mesin terkait    │
                │                         │ 2. Tambah Penyusutan Matras     │
                │ 3. Catat BKK            │    per pcs produk secara riil   │
                ▼                         └─────────────────────────────────┘
┌─────────────────────────────────┐
│           Arus Kas              │
├─────────────────────────────────┤
│ Catat pengeluaran DIRECT_LABOR  │
└─────────────────────────────────┘
```

---

## 🗄️ Rencana Perubahan Database (PostgreSQL)

Jalankan query migrasi berikut di server VPS:

```sql
-- 1. Hubungkan mesin ke matras
ALTER TABLE "bmp_machines" ADD COLUMN IF NOT EXISTS "mold_id" INT DEFAULT NULL;

-- 2. Lacak pemakaian matras
ALTER TABLE "bmp_molds" ADD COLUMN IF NOT EXISTS "usage_count" INT DEFAULT 0;

-- 3. Simpan data absensi operator di log produksi
ALTER TABLE "bmp_production_logs" ADD COLUMN IF NOT EXISTS "workers_attendance" TEXT DEFAULT NULL;

-- 4. Simpan nama shift (PAGI, SORE, MALAM)
ALTER TABLE "bmp_production_logs" ADD COLUMN IF NOT EXISTS "shift_name" VARCHAR(50) DEFAULT 'PAGI';
```

---

## 🖥️ Rencana Perubahan Backend Go (Server API)

### 1. Otomatisasi Peningkatan Pemakaian Matras
Di `triggerProductionLogCompletion` (`handlers_rt.go`), tambahkan logika:
- Cari `mold_id` yang terpasang pada `machine_id` log produksi terkait.
- Jika ditemukan, hitung total shots pemakaian baru: `total_shots = quantityProduced + quantityRejected`.
- Jalankan query untuk meningkatkan pemakaian:
  ```sql
  UPDATE bmp_molds SET usage_count = COALESCE(usage_count, 0) + $1 WHERE id=$2
  ```

### 2. Formulasi HPP Presisi Gaji Operator per Mesin & Penyusutan Matras (`updateAndCalculateCOGS`)
Perbarui cara penghitungan komponen **Biaya Tenaga Kerja (Labor Cost)** dan **Penyusutan Cetakan (Mold Depreciation)** dalam HPP:
- **Alokasi Tenaga Kerja per Mesin (Gaji Operator)**:
  - Hitung rate upah per jam operator: `HourlyRate = Total Gaji Karyawan di periode berjalan / Total Jam Kerja Nyata dari Absensi Produksi`.
  - Hitung biaya operator per mesin: `MachineLaborCost = SUM(Jam Kerja di Mesin * HourlyRate Operator)`.
  - Biaya operator per unit produk = `(Cycle Time / Total Jam Operasional Mesin) * MachineLaborCost`.
- **Penyusutan Matras per Pcs**:
  - Ambil data matras terhubung untuk produk tersebut.
  - Jika matras terdaftar, hitung nilai penyusutan matras per pcs:
    $$\text{Penyusutan Matras/Pcs} = \frac{\text{Harga Beli Matras (purchase\_price)}}{\text{Batas Umur (expected\_shots\_lifetime)}}$$
  - Tambahkan nilai penyusutan ini secara langsung ke total HPP produk terkait:
    $$\text{HPP Total} = \text{Bahan Baku} + \text{Biaya Mesin (Listrik & Depresiasi Mesin)} + \text{Gaji Operator Riil Mesin} + \text{Penyusutan Matras/Pcs}$$

---

## 📱 Rencana Perubahan Android (Data & UI Layer)

### 1. Perubahan Entitas Data ✅ SELESAI (v2.19.21–v2.19.24)
- **`BmpMachineEntity`**: `operatorSalaryMonthly` & `hoursCapacityMonthly` dihapus dari UI (v2.19.21). `electricityCostDaily` dihapus dari UI (v2.19.24). Field tetap ada di entity sebagai backward-compat. Tambahkan `moldId: Long? = null` ✅.
- **`BmpMoldEntity`**: `usageCount: Int = 0` sudah ditambahkan ✅.
- **`BmpProductionLogEntity`**: `workersAttendance: String?` dan `shiftName: String` sudah ditambahkan ✅.
- **Model `OperatorAttendanceEntry`** (multi-operator per mesin) sudah ada di `BmpProductionLogScreen.kt` ✅.

### 2. Perubahan UI Halaman Mesin & Matras (`MachineMoldManagementScreen.kt`) ✅ SELESAI (v2.19.24)
- **Master Mesin**:
  - ✅ Hapus input field "Gaji Operator / Bulan" (v2.19.24)
  - ✅ Hapus input field "Biaya Listrik Nominal Harian" (v2.19.24)
  - ✅ Tambah dropdown pemilih matras terpasang pada mesin
  - ✅ Banner peringatan kuning jika mesin aktif tapi matras kosong
- **Master Matras/Cetakan**:
  - ✅ Progress bar pemakaian (`usageCount / expectedShotsLifetime`)
  - ✅ Tampilkan Amortisasi Per Shot (Harga Beli / Batas Umur)
  - ✅ Badge ⚠️ "Perlu Servis" jika `usageCount >= 90%`

### 3. Perubahan UI Halaman Log Produksi Harian (`BmpProductionLogScreen.kt`) ✅ SELESAI (v2.19.21–v2.19.23)
- ✅ **Selector 3 Shift** (Pagi 07:00–15:00 / Sore 15:00–23:00 / Malam 23:00–07:00) — default otomatis jam lokal
- ✅ **Campuran Pewarna** — pilih warna 1, 2, 3, 4 + link batch bahan baku pewarna, rasio campuran
- ✅ **Multi-Operator per Mesin** — min 1, bisa tambah bebas, input jam masuk/pulang per operator
- ✅ **History Log** — tampilkan tag Shift di kartu riwayat produksi

### 4. Perubahan UI Halaman Penggajian (`EmployeesScreen.kt`) ✅ SELESAI (v2.19.21)
- ✅ Tombol **"Isi Otomatis dari Absensi Produksi"** — mengisi total hari kerja dari log absensi
- ✅ Tombol Edit & Hapus payroll sinkron ke Arus Kas

---

## ✅ Status Verifikasi

### A. Database & Backend
- [x] Kolom `mold_id`, `usage_count`, `workers_attendance`, `shift_name` terbentuk di VPS (db.go migration)
- [x] Backend auto-increment `usage_count` matras saat log produksi disimpan (v2.19.24, `handlers_rt.go`)
- [x] Penyusutan matras per pcs masuk ke kalkulasi HPP (`updateAndCalculateCOGS`)
- [x] Go backend v2.19.24 build sukses (tidak ada error compile)

### B. Android UI — Halaman Mesin & Matras
- [x] Field "Gaji Operator / Bulan" sudah hilang dari form mesin (v2.19.24)
- [x] Field "Biaya Listrik Nominal Harian" sudah hilang dari form & kartu mesin (v2.19.24)
- [x] Banner kuning ⚠️ muncul jika mesin aktif tanpa matras
- [x] Badge ⚠️ & progress bar matras muncul jika usage >= 90%

### C. Android UI — Halaman Log Produksi
- [x] Selector 3 Shift muncul (Pagi/Sore/Malam) — default otomatis sesuai jam lokal
- [x] Absensi multi-operator per mesin berfungsi (min 1, tidak bisa dikurangi)
- [x] Campuran pewarna dengan pilihan batch bahan baku
- [x] History card menampilkan info shift

### D. Android UI — Halaman Penggajian
- [x] Tombol "Isi Otomatis dari Absensi Produksi" tersedia dan berfungsi
- [x] Edit & Hapus payroll sinkron ke Arus Kas (BKK DIRECT_LABOR)

### E. Relasi Keuangan (v2.19.24)
- [ ] PERLU VERIFIKASI: Setelah deploy backend — tambah log produksi → cek `usage_count` bertambah
- [ ] PERLU VERIFIKASI: Cek HPP di produk → komponen penyusutan matras masuk
- [ ] PERLU VERIFIKASI: Bayar gaji → Arus Kas → ada entri BKK DIRECT_LABOR
