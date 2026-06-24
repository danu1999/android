import subprocess

def run_vps_bash(script_content):
    ssh_cmd = [
        "ssh", "-i", "C:\\Users\\danus\\Documents\\muizz.pem",
        "-o", "StrictHostKeyChecking=no",
        "muizz9900@zedmz.cloud",
        "bash"
    ]
    res = subprocess.run(ssh_cmd, input=script_content, capture_output=True, text=True)
    return res.stdout, res.stderr, res.returncode

vps_script = """
psql postgres://postgres:Bahtera1!@localhost:5432/posbah?sslmode=disable << 'SQL'
-- 1. Hapus baris duplikat di bmp_master_products
DELETE FROM bmp_master_products a
USING bmp_master_products b
WHERE a.ctid < b.ctid
  AND a.id = b.id
  AND a."tenantId" = b."tenantId";

-- 2. Buat primary key constraint yang benar
ALTER TABLE bmp_master_products DROP CONSTRAINT IF EXISTS bmp_master_products_pkey;
ALTER TABLE bmp_master_products ADD PRIMARY KEY (id, "tenantId");
SQL
"""

print("Executing Primary Key configuration on VPS...")
out, err, code = run_vps_bash(vps_script)
print("Code:", code)
print("Stdout:")
print(out)
print("Stderr:")
print(err)
