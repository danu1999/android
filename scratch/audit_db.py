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

tenant = "ten_premium_hanafiariful_gmail_com"

print(f"--- POST-LOGIN/MIGRATION AUDIT FOR TENANT: {tenant} ---")

tables = [
    "outlets",
    "employees",
    "products",
    "customers",
    "transactions",
    "transaction_items",
    "print_settings"
]

for table in tables:
    out, err, code = run_query(f'SELECT COUNT(*), COUNT(*) FILTER (WHERE "isDeleted" = false) AS active FROM "{table}" WHERE "tenantId" = $${tenant}$$;')
    if code != 0:
        # Maybe no isDeleted column (e.g. print_settings)
        out, err, code = run_query(f'SELECT COUNT(*) FROM "{table}" WHERE "tenantId" = $${tenant}$$;')
    print(f"Table '{table}':")
    print(out)
