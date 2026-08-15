import requests
import json

BASE_URL = "https://www.zedmz.cloud"

# Test 1: Danusijon
headers_danus = {
    "Authorization": "Bearer danusijon@gmail.com",
    "Content-Type": "application/json",
    "x-client-version": "2.19.70"
}

print("=== 1. Testing GET /api/rt/bmp/work-orders for danusijon@gmail.com ===")
res_get = requests.get(f"{BASE_URL}/api/rt/bmp/work-orders", headers=headers_danus)
print(f"GET status: {res_get.status_code}")
print(f"GET body: {res_get.text}")

print("\n=== 2. Testing POST /api/rt/bmp/work-orders for danusijon@gmail.com ===")
sample_spk = {
    "spkNumber": "SPK-TEST-001",
    "masterProductId": 1,
    "targetQuantity": 500,
    "completedQuantity": 0,
    "rejectedQuantity": 0,
    "startDate": 1750000000000,
    "status": "PENDING",
    "priority": "NORMAL",
    "notes": "Test SPK dari script"
}

res_post = requests.post(f"{BASE_URL}/api/rt/bmp/work-orders", headers=headers_danus, json=sample_spk)
print(f"POST status: {res_post.status_code}")
print(f"POST body: {res_post.text}")

print("\n=== 3. Testing GET after POST ===")
res_get2 = requests.get(f"{BASE_URL}/api/rt/bmp/work-orders", headers=headers_danus)
print(f"GET 2 status: {res_get2.status_code}")
print(f"GET 2 body: {res_get2.text}")
