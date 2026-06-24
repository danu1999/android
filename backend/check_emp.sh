#!/bin/bash
echo "=== Karyawan Hanafi ==="
sudo -u postgres psql posbah -c "SELECT id, name, email, role, \"outletId\", \"isActive\", (\"pinHash\" IS NOT NULL) as has_pin FROM employees WHERE \"tenantId\" = 'ten_premium_hanafiariful_gmail_com' ORDER BY role;"

echo ""
echo "=== Cek kolom employees ==="
sudo -u postgres psql posbah -c "\d employees"
