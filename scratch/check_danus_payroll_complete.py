import urllib.request
import urllib.parse
import json

BASE_URL = "https://www.zedmz.cloud/api/sync"
EMAIL = "danusijon@gmail.com"
TENANT_ID = "ten_premium_danusijon_gmail_com_BMP"

def api_get(endpoint):
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "x-client-version": "web",
        "x-user-email": EMAIL,
        "x-tenant-id": TENANT_ID
    }
    url = f"{BASE_URL}{endpoint}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req) as resp:
            data = resp.read().decode('utf-8')
            return json.loads(data)
    except Exception as e:
        print(f"Error fetching {endpoint}: {e}")
        return None

print("=" * 70)
print(f"CHECKING DATA FOR: {EMAIL} (Tenant: {TENANT_ID})")
print("=" * 70)

# 1. Tenants
tenants = api_get(f"/tenants?id=eq.{urllib.parse.quote(TENANT_ID)}")
print("\n--- TENANT INFO ---")
print(json.dumps(tenants, indent=2))

# 2. Outlets
outlets = api_get(f"/outlets?tenantId=eq.{urllib.parse.quote(TENANT_ID)}")
print("\n--- OUTLETS ---")
print(json.dumps(outlets, indent=2))

# 3. Employees (POS/FNB standard)
employees = api_get(f"/employees?tenantId=eq.{urllib.parse.quote(TENANT_ID)}")
print("\n--- EMPLOYEES (Standard) ---")
print(json.dumps(employees, indent=2))

# 4. BMP Employees
bmp_employees = api_get(f"/bmp_employees?tenantId=eq.{urllib.parse.quote(TENANT_ID)}")
print("\n--- BMP EMPLOYEES ---")
print(json.dumps(bmp_employees, indent=2))

# 5. BMP Payrolls
bmp_payrolls = api_get(f"/bmp_payrolls?tenantId=eq.{urllib.parse.quote(TENANT_ID)}")
print("\n--- BMP PAYROLLS (Riwayat Pembayaran Gaji Karyawan) ---")
print(json.dumps(bmp_payrolls, indent=2))

# 6. BMP Attendance Logs
bmp_attendance = api_get(f"/bmp_attendance_logs?tenantId=eq.{urllib.parse.quote(TENANT_ID)}")
print("\n--- BMP ATTENDANCE LOGS ---")
print(json.dumps(bmp_attendance, indent=2))

# 7. BMP Cashflow
bmp_cashflow = api_get(f"/bmp_cashflow?tenantId=eq.{urllib.parse.quote(TENANT_ID)}")
print("\n--- BMP CASHFLOW (Arus Kas / Pengeluaran Gaji) ---")
print(json.dumps(bmp_cashflow, indent=2))

# 8. Activity Logs
activity_logs = api_get(f"/activity_logs?tenantId=eq.{urllib.parse.quote(TENANT_ID)}")
print("\n--- ACTIVITY LOGS ---")
print(json.dumps(activity_logs, indent=2))
