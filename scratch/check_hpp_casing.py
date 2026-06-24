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

sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'bmp_master_products' AND column_name ILIKE '%hpp%';"
out, err, code = run_query(sql)
print("Columns matching hpp:")
print(out)
