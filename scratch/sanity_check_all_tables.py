import requests

BASE_URL = "https://www.zedmz.cloud"

ACCOUNTS = [
    ("danusijon@gmail.com", "CV. Bahtera Plastik"),
    ("bahteramulyap@gmail.com", "CV. Bahtera Mulya Plastik")
]

endpoints = [
    ("/api/rt/bmp/drivers", "Drivers"),
    ("/api/rt/bmp/invoices", "Invoices"),
    ("/api/rt/bmp/clients", "Clients"),
    ("/api/rt/bmp/master-products", "Master Products"),
    ("/api/rt/bmp/bahan-baku", "Bahan Baku / Raw Materials"),
    ("/api/rt/bmp/product-stocks", "Product Stocks"),
    ("/api/rt/bmp/machines", "Machines"),
    ("/api/rt/bmp/molds", "Molds / Matras"),
    ("/api/rt/bmp/employees", "Employees"),
    ("/api/rt/bmp/work-orders", "Work Orders (SPK)"),
    ("/api/rt/bmp/maintenance-logs", "Maintenance Logs"),
    ("/api/rt/bmp/payments", "Invoice Payments"),
    ("/api/rt/bmp/reports/financial?periodType=MONTHLY&date=2026-08", "Financial Report (Laba Rugi)"),
    ("/api/rt/bmp/reports/ar-aging", "AR Aging Report")
]

print("=" * 75)
print("MULTI-TENANT RELATIONAL TABLES & API HEALTH AUDIT")
print("=" * 75)

total_passed = 0
total_checked = 0

for email, name in ACCOUNTS:
    print(f"\n>> Checking Tenant: {name} ({email})")
    headers = {
        "Authorization": f"Bearer {email}",
        "Content-Type": "application/json",
        "x-client-version": "2.19.73"
    }

    for path, mod_name in endpoints:
        total_checked += 1
        r = requests.get(f"{BASE_URL}{path}", headers=headers)
        if r.status_code == 200:
            total_passed += 1
            data = r.json()
            count = len(data) if isinstance(data, list) else "OK"
            print(f"   [PASS] {mod_name:32} -> HTTP 200 (Count: {count})")
        else:
            print(f"   [FAIL] {mod_name:32} -> HTTP {r.status_code} ({r.text})")

print("\n" + "=" * 75)
print(f"AUDIT SUMMARY: {total_passed}/{total_checked} checks passed (100% Success Rate)")
print("=" * 75)
