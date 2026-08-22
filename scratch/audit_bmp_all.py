import requests

BASE_URL = "https://www.zedmz.cloud"
headers = {
    "Authorization": "Bearer danusijon@gmail.com",
    "Content-Type": "application/json",
    "x-client-version": "2.19.76"
}

endpoints = [
    ("/api/rt/bmp/settings", "Pengaturan Pabrik (Settings)"),
    ("/api/rt/bmp/clients", "Master Klien / Pelanggan"),
    ("/api/rt/bmp/master-products", "Master Produk Jadi"),
    ("/api/rt/bmp/invoices", "Invoice & Penjualan"),
    ("/api/rt/bmp/products", "Item Invoice / Produk Detail"),
    ("/api/rt/bmp/payments", "Pembayaran Invoice"),
    ("/api/rt/bmp/employees", "Master Karyawan"),
    ("/api/rt/bmp/payrolls", "Gaji & Payroll"),
    ("/api/rt/bmp/drivers", "Master Sopir / Armada"),
    ("/api/rt/bmp/recruitment/invitations", "Undangan Rekrutmen"),
    ("/api/rt/bmp/recruitment/applicants", "Calon Karyawan (E-Recruitment)"),
    ("/api/rt/bmp/bahan-baku", "Bahan Baku Masuk"),
    ("/api/rt/bmp/bahan-baku-items", "Item Bahan Baku"),
    ("/api/rt/bmp/suppliers", "Master Supplier"),
    ("/api/rt/bmp/machines", "Master Mesin Pabrik"),
    ("/api/rt/bmp/production-logs", "Log Produksi Mesin"),
    ("/api/rt/bmp/production-batches", "Batch Produksi / SPK"),
    ("/api/rt/bmp/product-stocks", "Stok Produk Jadi"),
    ("/api/rt/bmp/stock-ledger", "Kartu Stok / Ledger"),
    ("/api/rt/bmp/surat-jalan", "Surat Jalan / Delivery Order"),
    ("/api/rt/bmp/attendance", "Presensi & Absensi Karyawan"),
    ("/api/rt/bmp/financial-accounts", "Bagan Akun Keuangan (COA)"),
    ("/api/rt/bmp/journal-entries", "Jurnal Keuangan Umum"),
    ("/api/rt/bmp/expenses", "Pengeluaran Operasional Pabrik"),
]

print("=== AUDIT KONEKSI & RELASI DATABASE POSBAH ===")
ok_count = 0
for ep, name in endpoints:
    try:
        res = requests.get(f"{BASE_URL}{ep}", headers=headers, timeout=10)
        status = res.status_code
        data = res.json() if status == 200 else None
        count = len(data) if isinstance(data, list) else (len(data.keys()) if isinstance(data, dict) else 0)
        if status == 200:
            ok_count += 1
            print(f"[OK 200] {name:<36} -> {count:>3} records")
        else:
            print(f"[{status}]    {name:<36} -> {res.text[:60]}")
    except Exception as e:
        print(f"[ERR]    {name:<36} -> {e}")

print(f"\nHasil: {ok_count} / {len(endpoints)} endpoint aktif & terhubung ke PostgreSQL.")
