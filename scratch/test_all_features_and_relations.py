import requests
import json

BASE_URL = "https://www.zedmz.cloud"

ACCOUNTS = [
    {
        "email": "danusijon@gmail.com",
        "tenantId": "ten_premium_danusijon_gmail_com_BMP",
        "name": "CV. Bahtera Plastik (danusijon@gmail.com)"
    },
    {
        "email": "bahteramulyap@gmail.com",
        "tenantId": "ten_premium_bahteramulyap_gmail_com",
        "name": "CV. Bahtera Mulya Plastik (bahteramulyap@gmail.com)"
    }
]

print("=" * 75)
print("[TEST] RELATIONAL DATABASE & API INTEGRATION AUDIT")
print("=" * 75)

for acc in ACCOUNTS:
    email = acc["email"]
    tenant_id = acc["tenantId"]
    name = acc["name"]
    
    headers = {
        "Authorization": f"Bearer {email}",
        "Content-Type": "application/json",
        "x-client-version": "2.19.73"
    }

    print(f"\n=======================================================")
    print(f"Testing Account: {name}")
    print(f"Tenant ID: {tenant_id}")
    print(f"=======================================================")

    # 1. GET /api/rt/bmp/drivers
    r = requests.get(f"{BASE_URL}/api/rt/bmp/drivers", headers=headers)
    print(f"1. GET /api/rt/bmp/drivers -> Status: {r.status_code}")
    if r.status_code == 200:
        drivers = r.json()
        print(f"   [OK] Drivers fetched: {len(drivers)} drivers found.")
        for d in drivers[:2]:
            print(f"        Driver: {d.get('name')} | HP: {d.get('phone')} | Plat: {d.get('plateNumber')} | KTP Doc: {bool(d.get('ktpImageUrl'))}")
    else:
        print(f"   [ERROR] Failed to fetch drivers: {r.text}")

    # 2. POST /api/rt/bmp/drivers (Create test driver)
    driver_payload = {
        "name": "Sopir Uji Relasi",
        "phone": "08123456789",
        "plateNumber": "L 9999 AA",
        "truckType": "Colt Diesel Double",
        "ktpImageUrl": "https://www.zedmz.cloud/drivers/sample_ktp.jpg",
        "truckImageUrl": "https://www.zedmz.cloud/drivers/sample_truck.jpg",
        "stnkImageUrl": "https://www.zedmz.cloud/drivers/sample_stnk.jpg",
        "notes": "Driver pengujian relasi schema"
    }
    r = requests.post(f"{BASE_URL}/api/rt/bmp/drivers", headers=headers, json=driver_payload)
    print(f"2. POST /api/rt/bmp/drivers -> Status: {r.status_code}")
    driver_id = None
    if r.status_code in (200, 201):
        res_data = r.json()
        driver_id = res_data.get("id")
        print(f"   [OK] Success created driver ID: {driver_id}")
    else:
        print(f"   [ERROR] Failed: {r.text}")

    # 3. PUT /api/rt/bmp/drivers/:id
    if driver_id:
        driver_payload["name"] = "Sopir Uji Relasi Updated"
        r = requests.put(f"{BASE_URL}/api/rt/bmp/drivers/{driver_id}", headers=headers, json=driver_payload)
        print(f"3. PUT /api/rt/bmp/drivers/{driver_id} -> Status: {r.status_code}")
        if r.status_code == 200:
            print(f"   [OK] Driver updated successfully!")
        else:
            print(f"   [ERROR] Failed: {r.text}")

    # 4. GET /api/rt/bmp/invoices
    r = requests.get(f"{BASE_URL}/api/rt/bmp/invoices", headers=headers)
    print(f"4. GET /api/rt/bmp/invoices -> Status: {r.status_code}")
    test_invoice = None
    if r.status_code == 200:
        invoices = r.json()
        print(f"   [OK] Invoices fetched: {len(invoices)} invoices found.")
        if invoices:
            test_invoice = invoices[0]
            print(f"        Sample Invoice #{test_invoice.get('number')} -> Total: Rp {test_invoice.get('totalAmount'):,.0f} | Paid: Rp {test_invoice.get('paidAmount'):,.0f} | Driver: {test_invoice.get('driverName')} | Ongkir: {test_invoice.get('ongkirSopir')} | Kuli: {test_invoice.get('biayaKuli')} | Status: {test_invoice.get('deliveryStatus')}")
    else:
        print(f"   [ERROR] Failed: {r.text}")

    # 5. PUT /api/rt/bmp/invoices/:id (Update Delivery Details)
    if test_invoice:
        inv_id = test_invoice.get("id")
        delivery_update = dict(test_invoice)
        delivery_update["driverId"] = driver_id or 1
        delivery_update["driverName"] = "Pak Bambang Pengiriman"
        delivery_update["driverPhone"] = "081987654321"
        delivery_update["plateNumber"] = "W 5678 CD"
        delivery_update["ongkirSopir"] = 125000.0
        delivery_update["biayaKuli"] = 40000.0
        delivery_update["deliveryStatus"] = "ON_DELIVERY"
        
        r = requests.put(f"{BASE_URL}/api/rt/bmp/invoices/{inv_id}", headers=headers, json=delivery_update)
        print(f"5. PUT /api/rt/bmp/invoices/{inv_id} (Update Delivery) -> Status: {r.status_code}")
        if r.status_code == 200:
            print(f"   [OK] Invoice delivery info updated successfully!")
        else:
            print(f"   [ERROR] Failed: {r.text}")

    # 6. GET /api/rt/bmp/reports/financial (Check OPEX delivery & kuli integration)
    r = requests.get(f"{BASE_URL}/api/rt/bmp/reports/financial?periodType=MONTHLY&date=2026-08", headers=headers)
    print(f"6. GET /api/rt/bmp/reports/financial -> Status: {r.status_code}")
    if r.status_code == 200:
        rep = r.json()
        print(f"   [OK] Financial Report parsed:")
        print(f"        • Omzet: Rp {rep.get('omzet', 0):,.0f}")
        print(f"        • Laba Kotor: Rp {rep.get('labaKotor', 0):,.0f}")
        print(f"        • Beban Gaji Karyawan: Rp {rep.get('gajiKaryawan', 0):,.0f}")
        print(f"        • Beban Pemeliharaan: Rp {rep.get('biayaMaintenance', 0):,.0f}")
        print(f"        • Beban Pengiriman & Kuli: Rp {rep.get('biayaPengirimanDanKuli', 0):,.0f}")
        print(f"        • Total Beban OPEX: Rp {rep.get('totalBebanOperasional', 0):,.0f}")
        print(f"        • Laba Bersih: Rp {rep.get('labaBersih', 0):,.0f}")
    else:
        print(f"   [ERROR] Failed: {r.text}")

    # 7. Clean up test driver
    if driver_id:
        r = requests.delete(f"{BASE_URL}/api/rt/bmp/drivers/{driver_id}", headers=headers)
        print(f"7. DELETE /api/rt/bmp/drivers/{driver_id} -> Status: {r.status_code}")
        if r.status_code == 200:
            print("   [OK] Test driver cleaned up successfully!")
        else:
            print(f"   [ERROR] Failed: {r.text}")

print("\n" + "=" * 75)
print("[OK] AUDIT TEST FINISHED")
print("=" * 75)
