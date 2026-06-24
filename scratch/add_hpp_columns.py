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

print("Adding missing columns to bmp_master_products table...")
sql_add = """
ALTER TABLE bmp_master_products 
ADD COLUMN IF NOT EXISTS "hppTotalPcs" DOUBLE PRECISION DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS "hppLusin" DOUBLE PRECISION DEFAULT 0.0;
"""
out, err, code = run_query(sql_add)
print("Code:", code)
print("Stdout:", out)
print("Stderr:", err)
