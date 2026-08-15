import subprocess

def run_sql(query):
    print("=" * 60)
    print("QUERY:", query)
    print("=" * 60)
    ssh_cmd = [
        "ssh", "-i", "C:\\Users\\danus\\Documents\\muizz.pem",
        "-o", "StrictHostKeyChecking=no",
        "muizz9900@zedmz.cloud",
        f'psql postgres://postgres:Bahtera1!@localhost:5432/posbah?sslmode=disable -c "{query}"'
    ]
    res = subprocess.run(ssh_cmd, capture_output=True, text=True)
    print("STDOUT:")
    print(res.stdout)
    if res.stderr:
        print("STDERR:")
        print(res.stderr)

if __name__ == "__main__":
    run_sql("SELECT * FROM local_users WHERE email ILIKE '%danusijon%';")
    run_sql("SELECT * FROM tenants WHERE \"ownerEmail\" ILIKE '%danusijon%' OR id ILIKE '%danusijon%';")
    run_sql("SELECT * FROM employees WHERE email ILIKE '%danusijon%' OR name ILIKE '%danusijon%';")
    run_sql("SELECT * FROM bmp_employees WHERE name ILIKE '%danusijon%';")
