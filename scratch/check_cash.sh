#!/bin/bash
export PGPASSWORD="posbah_vps_pass_2026"
echo "=== Checking Cash Flow Balance in bmp_db ==="
psql -U postgres -h localhost -d bmp_db -c "SELECT SUM(CASE WHEN transaction_type = 'MASUK' THEN amount ELSE -amount END) FROM cash_flows;"
echo "=== Checking Cash Flow Balance in posbah_tenant_bahteramulyap_gmail_com ==="
psql -U postgres -h localhost -d posbah_tenant_bahteramulyap_gmail_com -c "SELECT SUM(CASE WHEN transaction_type = 'MASUK' THEN amount ELSE -amount END) FROM cash_flows;" || echo "posbah_tenant_bahteramulyap_gmail_com doesn't have cash_flows or table is empty"
