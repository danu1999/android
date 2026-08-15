import urllib.request
import urllib.parse
import json
import ssl

ssl_ctx = ssl.create_default_context()
ssl_ctx.check_hostname = False
ssl_ctx.verify_mode = ssl.CERT_NONE

BASE_URL = "https://zedmz.cloud"
TOKEN = "danusijon@gmail.com"

def make_req(path, method="GET", body=None):
    url = f"{BASE_URL}{path}"
    headers = {
        "Authorization": f"Bearer {TOKEN}",
        "x-client-version": "2.20.0"
    }
    data = None
    if body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode("utf-8")
    
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, context=ssl_ctx) as resp:
            content = resp.read().decode("utf-8")
            return resp.status, json.loads(content) if content else {}
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8")
        return e.code, err_body
    except Exception as e:
        return 0, str(e)

print("=== Testing BMP Employee & Payroll API Flow ===")

# 1. Fetch current employees
status, data = make_req("/api/rt/bmp/employees")
print(f"1. GET /api/rt/bmp/employees -> Status: {status}")
print("Data:", data)

# 2. Create an employee
new_emp = {
    "name": "Budi Santoso (Supervisor)",
    "role": "SUPERVISOR",
    "position": "SUPERVISOR",
    "salaryAmount": 120000.0,
    "salary": 120000.0,
    "employeeType": "OPERATING_EXPENSE",
    "phone": "081234567890",
    "email": "budi.santoso@example.com",
    "fingerprintPIN": "1234",
    "isActive": True
}
status, create_resp = make_req("/api/rt/bmp/employees", method="POST", body=new_emp)
print(f"\n2. POST /api/rt/bmp/employees -> Status: {status}")
print("Response:", create_resp)

# 3. Fetch employees again to verify persistence
status, emp_list = make_req("/api/rt/bmp/employees")
print(f"\n3. GET /api/rt/bmp/employees -> Status: {status}")
print("Employees Count:", len(emp_list) if isinstance(emp_list, list) else 0)
print("Employees:", emp_list)

# 4. If employee was created, test Pay Salary
if isinstance(emp_list, list) and len(emp_list) > 0:
    emp = emp_list[0]
    emp_id = emp.get("id")
    print(f"\n4. Testing Pay Salary for Employee ID {emp_id} ({emp.get('name')})...")
    payroll_payload = {
        "employeeId": emp_id,
        "employeeName": emp.get("name"),
        "amount": 25 * 120000.0,
        "attendanceCount": 25,
        "dailyRate": 120000.0,
        "description": "Gaji Bulan Agustus 2026",
        "paymentMethod": "TRANSFER"
    }
    status, pay_resp = make_req("/api/rt/bmp/payrolls", method="POST", body=payroll_payload)
    print(f"POST /api/rt/bmp/payrolls -> Status: {status}")
    print("Response:", pay_resp)

    # 5. Verify payrolls list
    status, payrolls = make_req("/api/rt/bmp/payrolls")
    print(f"\n5. GET /api/rt/bmp/payrolls -> Status: {status}")
    print("Payrolls:", payrolls)

    # 6. Verify employee lastPaidAt
    status, emp_list_after = make_req("/api/rt/bmp/employees")
    print(f"\n6. GET /api/rt/bmp/employees after payroll -> Status: {status}")
    print("Employees after payroll:", emp_list_after)
