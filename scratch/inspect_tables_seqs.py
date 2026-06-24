import subprocess
import json

def run_query(sql):
    ssh_cmd = [
        "ssh", "-i", "C:\\Users\\danus\\Documents\\muizz.pem",
        "-o", "StrictHostKeyChecking=no",
        "muizz9900@zedmz.cloud",
        f"psql postgres://postgres:Bahtera1!@localhost:5432/posbah?sslmode=disable -c \"{sql}\""
    ]
    res = subprocess.run(ssh_cmd, capture_output=True, text=True)
    return res.stdout, res.stderr, res.returncode

tables_and_suggested_seqs = {
    "bmp_master_products": "BmpMasterProduct_id_seq",
    "bmp_clients": "BmpClient_id_seq",
    "bmp_invoices": "BmpInvoice_id_seq",
    "bmp_cashflow": "BmpCashFlow_id_seq",
    "bmp_invoice_payments": "BmpInvoicePayment_id_seq",
    "bmp_employees": "BmpEmployee_id_seq",
    "bmp_products": "BmpProduct_id_seq",
    "bmp_settings": "BmpSettings_id_seq",
    "bmp_bahan_baku": "BmpBahanNono_id_seq",
    "bmp_bahan_baku_item": "BmpBahanNonoItem_id_seq",
    "bmp_production_logs": "BmpProductionLog_id_seq",  # Let's check if this sequence exists
    "bmp_product_stocks": "BmpProductStock_id_seq",
    "bmp_stock_ledger": "BmpStockLedger_id_seq"
}

for table, seq in tables_and_suggested_seqs.items():
    print(f"\n================ TABLE: {table} ================")
    # Get max ID
    out, err, code = run_query(f"SELECT MAX(id) FROM \\\"{table}\\\";")
    if code == 0:
        print(f"Max ID: {out.strip()}")
    else:
        print(f"Error getting max ID: {err.strip()}")

    # Check if suggested sequence exists and get its value
    out_seq, err_seq, code_seq = run_query(f"SELECT last_value, is_called FROM pg_sequences WHERE schemaname = 'public' AND sequencename = '{seq}';")
    if code_seq == 0 and out_seq.strip() and "last_value" in out_seq:
        print(f"Sequence '{seq}' info:")
        print(out_seq.strip())
    else:
        # Check general info
        out_all_seqs, _, _ = run_query(f"SELECT sequencename FROM pg_sequences WHERE schemaname = 'public' AND sequencename ILIKE '%{table.replace('bmp_', '')}%';")
        print(f"Sequence '{seq}' not found or error. Seqs matching '{table}':")
        print(out_all_seqs.strip())
