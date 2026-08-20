import requests
import json

BASE_URL = "https://www.zedmz.cloud"
AUTH_TOKEN = "Bearer BahteraMigrate123!"
headers = {
    "Authorization": AUTH_TOKEN,
    "Content-Type": "application/json",
    "x-client-version": "2.19.73"
}

TENANTS = [
    "ten_premium_danusijon_gmail_com_BMP",
    "ten_premium_bahteramulyap_gmail_com"
]

print("=" * 70)
print("[TEST] TESTING API ENDPOINTS & DATABASE RELATIONS")
print("=" * 70)

for tenant_id in TENANTS:
    print(f"\n--- Testing Tenant: {tenant_id} ---")
    
    # 1. Test GET /api/rt/bmp/drivers
    r = requests.get(f"{BASE_URL}/api/rt/bmp/drivers?tenantId={tenant_id}", headers=headers)
    print(f"1. GET /api/rt/bmp/drivers -> Status {r.status_code}")
    if r.status_code == 200:
        drivers = r.json()
        print(f"   Success! Found {len(drivers)} drivers.")
    else:
        print(f"   FAILED: {r.text}")

    # 2. Test POST /api/rt/bmp/drivers (Create test driver)
    driver_payload = {
        "tenantId": tenant_id,
        "name": "Test Driver Unit",
        "phone": "081299998888",
        "plateNumber": "B 9999 TST",
        "truckType": "Engkel Box",
        "ktpImageUrl": "",
        "truckImageUrl": "",
        "stnkImageUrl": "",
        "notes": "Driver pengujian relasi sistem"
    }
    r = requests.post(f"{BASE_URL}/api/rt/bmp/drivers", headers=headers, json=driver_payload)
    print(f"2. POST /api/rt/bmp/drivers -> Status {r.status_code}")
    driver_id = None
    if r.status_code in (200, 201):
        res_data = r.json()
        driver_id = res_data.get("id")
        print(f"   Success created driver ID: {driver_id}")
    else:
        print(f"   FAILED: {r.text}")

    # 3. Test PUT /api/rt/bmp/drivers/:id
    if driver_id:
        driver_payload["name"] = "Test Driver Unit Updated"
        r = requests.put(f"{BASE_URL}/api/rt/bmp/drivers/{driver_id}", headers=headers, json=driver_payload)
        print(f"3. PUT /api/rt/bmp/drivers/{driver_id} -> Status {r.status_code}")
        if r.status_code != 200:
            print(f"   FAILED: {r.text}")
        else:
            print(f"   Success updated driver!")

    # 4. Test GET /api/rt/bmp/invoices
    r = requests.get(f"{BASE_URL}/api/rt/bmp/invoices?tenantId={tenant_id}", headers=headers)
    print(f"4. GET /api/rt/bmp/invoices -> Status {r.status_code}")
    test_invoice_id = None
    if r.status_code == 200:
        invoices = r.json()
        print(f"   Success! Found {len(invoices)} invoices.")
        if invoices:
            test_invoice_id = invoices[0].get("id")
            first_inv = invoices[0]
            print(f"   Sample Invoice #{first_inv.get('number')} -> Driver: {first_inv.get('driverName')}, Ongkir: {first_inv.get('ongkirSopir')}, Kuli: {first_inv.get('biayaKuli')}, Status: {first_inv.get('deliveryStatus')}")
    else:
        print(f"   FAILED: {r.text}")

    # 5. Test Update Delivery on an Invoice
    if test_invoice_id:
        delivery_payload = {
            "driverId": driver_id or 1,
            "driverName": "Pak Supir Relasi",
            "driverPhone": "081234567890",
            "plateNumber": "L 1234 AA",
            "ongkirSopir": 150000.0,
            "biayaKuli": 50000.0,
            "deliveryStatus": "ON_DELIVERY"
        }
        r = requests.put(f"{BASE_URL}/api/rt/bmp/invoices/{test_invoice_id}", headers=headers, json=delivery_payload)
        print(f"5. PUT /api/rt/bmp/invoices/{test_invoice_id} (Update Delivery) -> Status {r.status_code}")
        if r.status_code == 200:
            print(f"   Success updated invoice delivery info!")
        else:
            print(f"   FAILED: {r.text}")

    # 6. Test GET Financial Report (Check OPEX integration)
    r = requests.get(f"{BASE_URL}/api/rt/bmp/financial-report?tenantId={tenant_id}&periodType=MONTHLY&date=2026-08", headers=headers)
    print(f"6. GET /api/rt/bmp/financial-report -> Status {r.status_code}")
    if r.status_code == 200:
        rep = r.json()
        print(f"   Success! Omzet: {rep.get('omzet')}, Laba Kotor: {rep.get('labaKotor')}, Pengiriman&Kuli: {rep.get('biayaPengirimanDanKuli')}, Total OPEX: {rep.get('totalBebanOperasional')}, Laba Bersih: {rep.get('labaBersih')}")
    else:
        print(f"   FAILED: {r.text}")

    # 7. Clean up test driver
    if driver_id:
        r = requests.delete(f"{BASE_URL}/api/rt/bmp/drivers/{driver_id}", headers=headers)
        print(f"7. DELETE /api/rt/bmp/drivers/{driver_id} -> Status {r.status_code}")
        if r.status_code == 200:
            print("   Success deleted test driver!")
        else:
            print(f"   FAILED: {r.text}")

print("\n" + "=" * 70)
print("[OK] COMPLETED AUDIT")
print("=" * 70)
