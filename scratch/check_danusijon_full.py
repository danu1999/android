import subprocess

def run_vps_bash(script_content):
    ssh_cmd = [
        "ssh", "-i", "C:\\Users\\danus\\Documents\\muizz.pem",
        "-o", "StrictHostKeyChecking=no",
        "muizz9900@zedmz.cloud",
        "bash"
    ]
    res = subprocess.run(ssh_cmd, input=script_content, capture_output=True, text=True, timeout=30)
    return res.stdout, res.stderr, res.returncode

sql = r"""
psql postgres://postgres:Bahtera1!@localhost:5432/posbah?sslmode=disable << 'SQL'
\x on
SELECT '=== 1. LOCAL USERS ===' AS section;
SELECT * FROM local_users WHERE email ILIKE '%danusijon%';

SELECT '=== 2. TENANTS ===' AS section;
SELECT * FROM tenants WHERE "ownerEmail" ILIKE '%danusijon%' OR id ILIKE '%danusijon%';

SELECT '=== 3. EMPLOYEES (FNB/POS) ===' AS section;
SELECT * FROM employees WHERE "tenantId" ILIKE '%danusijon%' OR email ILIKE '%danusijon%' OR name ILIKE '%danusijon%';

SELECT '=== 4. BMP EMPLOYEES ===' AS section;
SELECT * FROM bmp_employees WHERE "tenantId" ILIKE '%danusijon%' OR name ILIKE '%danusijon%';

SELECT '=== 5. BMP PAYROLLS ===' AS section;
SELECT * FROM bmp_payrolls WHERE "tenantId" ILIKE '%danusijon%';

SELECT '=== 6. EXPENSES ===' AS section;
SELECT * FROM expenses WHERE "tenantId" ILIKE '%danusijon%';

SELECT '=== 7. BMP CASHFLOW (GAJI/PAYROLL) ===' AS section;
SELECT * FROM bmp_cashflow WHERE "tenantId" ILIKE '%danusijon%';

SELECT '=== 8. ACTIVITY LOGS ===' AS section;
SELECT * FROM activity_logs WHERE "tenantId" ILIKE '%danusijon%' OR description ILIKE '%gaji%' OR description ILIKE '%karyawan%' LIMIT 20;

SQL
"""

print("Checking VPS database for danusijon@gmail.com...")
out, err, code = run_vps_bash(sql)
print("Return code:", code)
print(out)
if err:
    print("Error:", err)
