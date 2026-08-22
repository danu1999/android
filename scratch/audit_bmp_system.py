import requests

BASE_URL = "https://www.zedmz.cloud"
headers = {
    "Authorization": "Bearer danusijon@gmail.com",
    "Content-Type": "application/json",
    "x-client-version": "2.19.76"
}

endpoints = [
    ("/api/rt/bmp/dashboard", "BMP Dashboard Summary"),
    ("/api/rt/bmp/settings", "BMP Settings"),
    ("/api/rt/bmp/employees", "Employees Master"),
    ("/api/rt/bmp/drivers", "Drivers Master"),
    ("/api/rt/bmp/recruitment/invitations", "Job Invitations"),
    ("/api/rt/bmp/recruitment/applicants", "Job Applicants"),
    ("/api/rt/bmp/clients", "Clients / Customers Master"),
    ("/api/rt/bmp/master-products", "Finished Goods / Master Products"),
    ("/api/rt/bmp/invoices", "Invoices & Sales"),
    ("/api/rt/bmp/surat-jalan", "Delivery Notes (Surat Jalan)"),
    ("/api/rt/bmp/raw-materials", "Raw Materials / Bahan Baku"),
    ("/api/rt/bmp/machines", "Factory Production Machines"),
    ("/api/rt/bmp/production-batches", "Production Batches / SPK"),
    ("/api/rt/bmp/financial-analysis", "Financial Analysis & Profit/Loss"),
]

print("=== AUDIT RELASI API & DATABASE POSBAH ===")
for ep, name in endpoints:
    try:
        res = requests.get(f"{BASE_URL}{ep}", headers=headers, timeout=10)
        status = res.status_code
        data = res.json() if status == 200 else None
        count = len(data) if isinstance(data, list) else (len(data.keys()) if isinstance(data, dict) else 0)
        print(f"[{status}] {name:<35} -> OK ({count} items)")
    except Exception as e:
        print(f"[ERR] {name:<35} -> {e}")

print("\n=== AUDIT COMPLETED ===")
