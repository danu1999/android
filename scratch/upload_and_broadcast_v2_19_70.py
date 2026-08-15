import os
import sys
import math
import time
import requests

BASE_URL = "https://www.zedmz.cloud"
AUTH_TOKEN = "Bearer BahteraMigrate123!"
APK_PATH = r"c:\Users\danus\Documents\antigravity\emergent\app\build\outputs\apk\release\posbah-v2.19.70.apk"
APK_FILENAME = "posbah-v2.19.70.apk"
VERSION = "2.19.70"

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

# Check APK Version endpoint
for attempt in range(5):
    try:
        ver_res = requests.get(f"{BASE_URL}/api/apk-version", timeout=10)
        print(f"Server APK Version check (attempt {attempt + 1}):", ver_res.status_code, ver_res.text)
        if ver_res.status_code == 200 and VERSION in ver_res.text:
            print("Version updated successfully on server!")
            break
    except Exception as e:
        print(f"Attempt {attempt + 1} error:", e)
    time.sleep(5)

# Trigger email blast to all users
print("\nTriggering broadcast update email...")
blast_res = requests.post(f"{BASE_URL}/api/admin/blast-update-email", headers=headers, timeout=15)
print(f"Blast Email Response ({blast_res.status_code}):", blast_res.text)
