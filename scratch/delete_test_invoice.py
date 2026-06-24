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

print("Deleting test invoice TEST-POST-12345...")
sql = 'DELETE FROM bmp_invoices WHERE number = $$TEST-POST-12345$$ AND "tenantId" = $$ten_premium_bahteramulyap_gmail_com$$;'
out, err, code = run_query(sql)
print(f"CODE: {code}")
print(f"OUT: {out}")
print(f"ERR: {err}")
