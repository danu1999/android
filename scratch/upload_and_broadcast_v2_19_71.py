import os
import sys
import math
import time
import requests

BASE_URL = "https://www.zedmz.cloud"
AUTH_TOKEN = "Bearer BahteraMigrate123!"
APK_PATH = r"c:\Users\danus\Documents\antigravity\emergent\app\build\outputs\apk\release\posbah-v2.19.71.apk"
APK_FILENAME = "posbah-v2.19.71.apk"
VERSION = "2.19.71"

if not os.path.exists(APK_PATH):
    print(f"Error: APK file not found at {APK_PATH}")
    sys.exit(1)

apk_size = os.path.getsize(APK_PATH)
print(f"Uploading APK: {APK_FILENAME} ({apk_size / (1024*1024):.2f} MB)")

CHUNK_SIZE = 900 * 1024  # 900 KB per chunk (below 1MB Nginx limit)
total_chunks = math.ceil(apk_size / CHUNK_SIZE)
print(f"Total chunks: {total_chunks}")

headers = {
    "Authorization": AUTH_TOKEN
}

with open(APK_PATH, "rb") as f:
    for i in range(total_chunks):
        chunk_data = f.read(CHUNK_SIZE)
        files = {
            "file": (f"chunk_{i}.part", chunk_data, "application/octet-stream")
        }
        data = {
            "filename": APK_FILENAME,
            "chunkIndex": str(i),
            "totalChunks": str(total_chunks)
        }
        
        print(f"Sending chunk {i + 1}/{total_chunks} ({len(chunk_data)} bytes)...")
        res = requests.post(f"{BASE_URL}/api/admin/upload-apk-chunk", headers=headers, data=data, files=files, timeout=30)
        
        if res.status_code != 200:
            print(f"Failed to upload chunk {i + 1}: {res.status_code} - {res.text}")
            sys.exit(1)

print("\n=== APK Upload Complete! ===")

# Trigger Deploy on VPS
print("\nTriggering server auto-deploy...")
deploy_res = requests.post(f"{BASE_URL}/api/admin/deploy", headers=headers, timeout=15)
print(f"Deploy Response ({deploy_res.status_code}):", deploy_res.text)

print("\nWaiting 20 seconds for server to git pull & restart...")
time.sleep(20)

desc = """POSBah v2.19.71 - Analisis Keuangan Komprehensif (Laba Bersih, Cetak PDF Resmi, Kirim Ringkasan WA & Pembersihan HPP Karung)

Halo Rekan POSBah! Versi 2.19.71 menghadirkan penyempurnaan menyeluruh pada modul Analisis Keuangan & Manufaktur:
1. [Fitur Baru] Laporan Laba Rugi Komprehensif: Menghitung Laba Bersih (Net Profit), Net Margin %, dan Titik Impas (BEP) dengan mengintegrasikan Beban Gaji Karyawan, Biaya Pemeliharaan Mesin/Matras, dan Biaya Operasional Pabrik.
2. [Fitur Baru] Cetak & Ekspor PDF Resmi: Menghasilkan dokumen resmi ber-Kop Surat perusahaan (CV. Bahtera Plastik / CV. Bahtera Mulya Plastik) lengkap dengan tabel laba rugi dan lembar pengesahan tanda tangan.
3. [Fitur Baru] Kirim Ringkasan Laporan via WhatsApp: Tombol aksi cepat untuk membagikan ikhtisar omzet, laba kotor, beban operasional, dan laba bersih langsung ke WhatsApp Owner/Manajemen.
4. [Fix & Peningkatan] Ekspor Excel 4 Tabel: Format file Excel (CSV) dilengkapi pemisah titik koma regional Indonesia dengan 4 tabel data akuntansi terpadu dan FileProvider yang aman di Android 10-15.
5. [Penyempurnaan HPP] Pembersihan Biaya Karung Otomatis: Menghapus biaya karung statis di pengaturan dan beralih ke pencatatan riil manual pada menu Bahan Baku, Perlengkapan & APD agar HPP produk 100% murni dan akurat.
6. [Fitur Baru] Kirim WA SPK ke Operator: Tombol kirim instruksi Surat Perintah Kerja langsung ke nomor WhatsApp karyawan kerja.

Silakan klik tombol "Unduh APK Sekarang" di layar untuk memperbarui aplikasi Anda ya!"""

config_payload = {
    "version": VERSION,
    "downloadUrl": "/api/download-apk",
    "description": desc
}

for attempt in range(5):
    try:
        res = requests.post(f"{BASE_URL}/api/admin/apk-config", headers=headers, json=config_payload, timeout=10)
        print(f"Server APK Version check (attempt {attempt+1}): {res.status_code} {res.text}")
        if res.status_code == 200:
            print("\nVersion updated successfully on server!")
            break
    except Exception as e:
        print(f"Waiting for server... ({e})")
    time.sleep(5)

print("\nTriggering broadcast update email...")
res_email = requests.post(f"{BASE_URL}/api/admin/blast-update-email", headers=headers, timeout=30)
print("Blast Email Response ({}): {}".format(res_email.status_code, res_email.text))

