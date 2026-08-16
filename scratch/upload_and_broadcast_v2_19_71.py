import os
import requests
import json
import time

BASE_URL = "https://www.zedmz.cloud"
AUTH_HEADER = {"Authorization": "Bearer BahteraMigrate123!"}
APK_PATH = r"c:\Users\danus\Documents\antigravity\emergent\app\build\outputs\apk\release\posbah-v2.19.71.apk"
CHUNK_SIZE = 1024 * 900  # 900 KB chunks

def upload_apk():
    file_size = os.path.getsize(APK_PATH)
    file_name = os.path.basename(APK_PATH)
    total_chunks = (file_size + CHUNK_SIZE - 1) // CHUNK_SIZE
    
    print(f"Uploading APK: {file_name} ({file_size / (1024*1024):.2f} MB)")
    print(f"Total chunks: {total_chunks}")
    
    with open(APK_PATH, "rb") as f:
        for chunk_idx in range(total_chunks):
            data = f.read(CHUNK_SIZE)
            headers = {
                **AUTH_HEADER,
                "X-Chunk-Index": str(chunk_idx),
                "X-Total-Chunks": str(total_chunks),
                "X-File-Name": file_name,
                "Content-Type": "application/octet-stream"
            }
            print(f"Sending chunk {chunk_idx + 1}/{total_chunks} ({len(data)} bytes)...")
            res = requests.post(f"{BASE_URL}/api/admin/upload-apk-chunk", headers=headers, data=data, timeout=60)
            if res.status_code != 200:
                print(f"Failed chunk {chunk_idx + 1}: {res.status_code} {res.text}")
                return False
            time.sleep(0.3)
            
    print("\n=== APK Upload Complete! ===")
    return True

def trigger_deploy_and_update_version():
    print("\nTriggering server auto-deploy...")
    res_dep = requests.post(f"{BASE_URL}/api/admin/deploy", headers=AUTH_HEADER, timeout=30)
    print("Deploy Response ({}): {}".format(res_dep.status_code, res_dep.text))
    
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
        "version": "2.19.71",
        "downloadUrl": "/api/download-apk",
        "description": desc
    }
    
    for attempt in range(5):
        try:
            res = requests.post(f"{BASE_URL}/api/admin/apk-config", headers=AUTH_HEADER, json=config_payload, timeout=10)
            print(f"Server APK Version check (attempt {attempt+1}): {res.status_code} {res.text}")
            if res.status_code == 200:
                print("\nVersion updated successfully on server!")
                break
        except Exception as e:
            print(f"Waiting for server... ({e})")
        time.sleep(5)
        
    print("\nTriggering broadcast update email...")
    res_email = requests.post(f"{BASE_URL}/api/admin/blast-update-email", headers=AUTH_HEADER, timeout=30)
    print("Blast Email Response ({}): {}".format(res_email.status_code, res_email.text))

if __name__ == "__main__":
    if upload_apk():
        trigger_deploy_and_update_version()
