import subprocess

def run_query(sql):
    ssh_cmd = [
        "ssh", "-i", "C:\\Users\\danus\\Documents\\muizz.pem",
        "-o", "StrictHostKeyChecking=no",
        "muizz9900@zedmz.cloud",
        f"psql postgres://postgres:Bahtera1!@localhost:5432/posbah?sslmode=disable -c \"{sql}\""
    ]
    res = subprocess.run(ssh_cmd, capture_output=True, text=True)
    return res.stdout, res.stderr, res.returncode

sql = 'SELECT id, "tenantId", COUNT(*) FROM bmp_master_products GROUP BY id, "tenantId" HAVING COUNT(*) > 1;'
out, err, code = run_query(sql)
print("Duplicates on bmp_master_products:")
print(out)
if err:
    print("Error:", err)
