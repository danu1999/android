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

tables = [
    "outlets",
    "employees",
    "products",
    "customers",
    "transactions",
    "transaction_items",
    "bmp_clients",
    "bmp_invoices",
    "bmp_products",
    "bmp_master_products",
    "bmp_cashflow",
    "bmp_invoice_payments",
    "bmp_employees",
    "bmp_payrolls",
    "bmp_bahan_baku",
    "bmp_bahan_baku_item",
    "bmp_production_logs",
    "bmp_product_stocks",
    "bmp_stock_ledger"
]

print("Starting sequence fix on VPS database...")
for table in tables:
    print(f"\n--- Fixing table: {table} ---")
    
    # 1. Create sequence if not exists
    seq_name = f"{table}_id_seq"
    create_seq_sql = f"CREATE SEQUENCE IF NOT EXISTS {seq_name};"
    out, err, code = run_query(create_seq_sql)
    if code != 0:
        print(f"Error creating sequence for {table}: {err}")
        continue
        
    # 2. Set default value to nextval of sequence
    alter_col_sql = f"ALTER TABLE \"{table}\" ALTER COLUMN id SET DEFAULT nextval($${seq_name}$$);"
    out, err, code = run_query(alter_col_sql)
    if code != 0:
        print(f"Error altering column for {table}: {err}")
        continue
        
    # 3. Associate sequence with column
    own_seq_sql = f"ALTER SEQUENCE {seq_name} OWNED BY \"{table}\".id;"
    out, err, code = run_query(own_seq_sql)
    if code != 0:
        print(f"Error setting sequence owner for {table}: {err}")
        continue
        
    # 4. Sync sequence value with MAX(id) + 1
    # We do a subquery select max(id) and setval
    sync_val_sql = f"SELECT setval($${seq_name}$$, COALESCE((SELECT MAX(id) FROM \"{table}\"), 0) + 1, false);"
    out, err, code = run_query(sync_val_sql)
    if code != 0:
        print(f"Error syncing sequence value for {table}: {err}")
        continue
        
    print(f"Successfully configured autoincrement sequence for {table}!")

print("\nAll sequences verified and updated successfully!")
