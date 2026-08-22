import os
import sys
import math
import requests

BASE_URL = "https://www.zedmz.cloud"
AUTH_TOKEN = "Bearer BahteraMigrate123!"
APK_PATH = r"c:\Users\danus\Documents\antigravity\emergent\app\build\outputs\apk\release\posbah-v2.19.76.apk"
APK_FILENAME = "posbah-v2.19.76.apk"
VERSION = "2.19.76"
VERSION_CODE = 183

if not os.path.exists(APK_PATH):
    print(f"Error: APK file not found at {APK_PATH}")
    sys.exit(1)

apk_size = os.path.getsize(APK_PATH)
print(f"Uploading APK: {APK_FILENAME} ({apk_size / (1024*1024):.2f} MB)")

CHUNK_SIZE = 900 * 1024
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

release_notes_plain = """Pembaruan v2.19.76:
1. Ketentuan Upah Transparan: Formulir pendaftaran resmi kini mencantumkan ketentuan upah masa pelatihan (2 minggu pertama 50K/hari) dan karyawan reguler (70K/hari) dengan persetujuan wajib pelamar.
2. Tes Singkat Kesiapan Kerja: Ditambahkan soal skrining dinamis sesuai posisi (Operator, Driver, Kuli/Gudang) dengan penilaian skor otomatis (0 - 100).
3. Badge Skor Tes & Status Upah di POSBah: Owner/HR dapat langsung melihat nilai skor tes dan kepatuhan ketentuan upah langsung di tab Calon Karyawan.
4. Nilai Tawaran Gaji Awal Default Rp 50.000/hari saat menerima calon karyawan baru."""

config_payload = {
    "version": VERSION,
    "description": release_notes_plain,
    "downloadUrl": f"{BASE_URL}/api/download-apk"
}

print(f"\nUpdating APK Config to v{VERSION} (Code: {VERSION_CODE})...")
headers_json = {
    "Authorization": AUTH_TOKEN,
    "Content-Type": "application/json"
}
res_config = requests.post(f"{BASE_URL}/api/admin/apk-config", headers=headers_json, json=config_payload)
print(f"APK Config Response ({res_config.status_code}): {res_config.text}")

# Blast Update Email
blast_payload = {
    "versionName": VERSION,
    "versionCode": VERSION_CODE,
    "releaseNotes": release_notes_plain,
    "downloadUrl": f"{BASE_URL}/api/download-apk"
}

print(f"\nBlasting Update Email to all users...")
res_blast = requests.post(f"{BASE_URL}/api/admin/blast-update-email", headers=headers_json, json=blast_payload)
print(f"Blast Email Response ({res_blast.status_code}): {res_blast.text}")

print("\n=== All Tasks Completed Successfully! ===")
