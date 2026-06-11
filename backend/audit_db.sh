#!/bin/bash
echo "=== Jumlah baris per tabel di PostgreSQL (database: posbah) ==="
sudo -u postgres psql -d posbah -t -c "
SELECT 
  tablename,
  (xpath('/row/cnt/text()', query_to_xml('SELECT count(*) as cnt FROM ' || quote_ident(tablename), false, true, '')))[1]::text::int as row_count
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY tablename;
"

echo ""
echo "=== ENV Backend yang berjalan ==="
cat /proc/$(systemctl show posbah-backend -p MainPID --value)/environ 2>/dev/null | tr '\0' '\n' | grep -E 'SUPABASE|DATABASE|PORT' || echo "Tidak bisa baca env"

echo ""
echo "=== Endpoint /api/sync/ apa saja yang ada ==="
curl -s http://localhost:3000/api/sync/transactions?tenantId=eq.bahteramulyap_gmail_com 2>/dev/null | head -5 || echo "Tidak ada endpoint sync"
