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

tables = [
    "bmp_master_products",
    "bmp_clients",
    "bmp_invoices",
    "bmp_cashflow",
    "bmp_invoice_payments",
    "bmp_employees",
    "bmp_products",
    "bmp_settings",
    "bmp_bahan_baku",
    "bmp_bahan_baku_item",
    "bmp_production_logs",
    "bmp_product_stocks",
    "bmp_stock_ledger"
]

print("Applying sequence fixes and defaults on VPS database...")
for table in tables:
    print(f"\n--- Table: {table} ---")
    seq_name = f"{table}_id_seq"
    
    # 1. Create sequence if not exists
    create_sql = f"CREATE SEQUENCE IF NOT EXISTS {seq_name};"
    out, err, code = run_query(create_sql)
    if code != 0:
        print(f"Error creating sequence for {table}: {err.strip()}")
        continue
    else:
        print(f"Sequence {seq_name} verified/created.")
        
    # 2. Set default value for id column
    alter_sql = f"ALTER TABLE \\\"{table}\\\" ALTER COLUMN id SET DEFAULT nextval('{seq_name}'::regclass);"
    out, err, code = run_query(alter_sql)
    if code != 0:
        print(f"Error setting default for {table}: {err.strip()}")
        continue
    else:
        print(f"Default set to nextval('{seq_name}'::regclass) for {table}.id")
        
    # 3. Synchronize sequence value with max ID
    sync_sql = f"SELECT setval('{seq_name}', COALESCE((SELECT MAX(id) FROM \\\"{table}\\\"), 0) + 1, false);"
    out, err, code = run_query(sync_sql)
    if code != 0:
        print(f"Error synchronizing sequence for {table}: {err.strip()}")
        continue
    else:
        print(f"Sequence {seq_name} synchronized. Result: {out.strip()}")

print("\nDone fixing all sequences!")
