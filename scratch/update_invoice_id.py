import subprocess

def run_query(sql):
    ssh_cmd = [
        "ssh", "-i", "C:\\Users\\danus\\Documents\\muizz.pem",
        "-o", "StrictHostKeyChecking=no",
        "muizz9900@zedmz.cloud",
        f"psql postgres://postgres:Bahtera1!@localhost:5432/posbah?sslmode=disable -c '{sql}'"
    ]
    res = subprocess.run(ssh_cmd, capture_output=True, text=True)
    return res.stdout, res.stderr, res.returncode

print("Updating INV-37488418 ID from 0 to 368...")
sql = 'UPDATE bmp_invoices SET id = 368 WHERE id = 0 AND "tenantId" = $$ten_premium_bahteramulyap_gmail_com$$;'
out, err, code = run_query(sql)
print(f"CODE: {code}")
print(f"OUT: {out}")
print(f"ERR: {err}")

print("\nVerifying the update...")
verify_sql = 'SELECT id, number, "totalAmount", status FROM bmp_invoices WHERE number = $$INV-37488418$$ AND "tenantId" = $$ten_premium_bahteramulyap_gmail_com$$;'
out, err, code = run_query(verify_sql)
print(f"CODE: {code}")
print(f"OUT: {out}")
print(f"ERR: {err}")
