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

print("--- FIXING BMP_PAYROLLS ID COLUMN ---")
out, err, code = run_query('ALTER TABLE bmp_payrolls ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;')
print(f"CODE: {code}")
print(f"OUT: {out}")
print(f"ERR: {err}")
