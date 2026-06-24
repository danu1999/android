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

sql = """
SELECT table_name, constraint_name, constraint_type 
FROM information_schema.table_constraints 
WHERE table_name LIKE 'bmp_%' AND constraint_type = 'PRIMARY KEY'
ORDER BY table_name;
"""
out, err, code = run_query(sql)
print("BMP Tables with PRIMARY KEY:")
print(out)
if err:
    print("Error:", err)
