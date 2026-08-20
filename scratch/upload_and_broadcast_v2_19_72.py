import os
import sys
import math
import time
import requests

BASE_URL = "https://www.zedmz.cloud"
AUTH_TOKEN = "Bearer BahteraMigrate123!"
APK_PATH = r"c:\Users\danus\Documents\antigravity\emergent\app\build\outputs\apk\release\posbah-v2.19.72.apk"
APK_FILENAME = "posbah-v2.19.72.apk"
VERSION = "2.19.72"
VERSION_CODE = 179

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

# Update APK Config
print("\nUpdating APK Config in Server...")
with open(r"c:\Users\danus\Documents\antigravity\emergent\backend\release_notes.txt", "r", encoding="utf-8") as f:
    release_notes = f.read()

config_payload = {
    "version": VERSION,
    "description": release_notes,
    "downloadUrl": f"{BASE_URL}/static/{APK_FILENAME}"
}

cfg_res = requests.post(f"{BASE_URL}/api/admin/apk-config", headers=headers, json=config_payload, timeout=15)
print(f"APK Config Response ({cfg_res.status_code}):", cfg_res.text)

# Blast Update Email to all users
print("\nBlasting Update Email to all users...")
blast_res = requests.post(f"{BASE_URL}/api/admin/blast-update-email", headers=headers, timeout=60)
print(f"Blast Email Response ({blast_res.status_code}):", blast_res.text)
