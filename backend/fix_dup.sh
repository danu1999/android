#!/bin/bash
echo "=== Cek duplikat employees ==="
sudo -u postgres psql posbah -c "SELECT id, name, email, role, \"outletId\", \"createdAt\" FROM employees WHERE email = 'fahrup22@gmail.com' ORDER BY id;"

echo ""
echo "=== Cek local_users fahrup22 ==="
sudo -u postgres psql posbah -c "SELECT \"googleSub\", email, role, \"tenantId\", \"isActive\" FROM local_users WHERE email = 'fahrup22@gmail.com';"

echo ""
echo "=== Hapus duplikat: keep id=10002 FahriP, hapus id=10004 fahri ==="
sudo -u postgres psql posbah -c "DELETE FROM employees WHERE id = 10004 AND \"tenantId\" = 'ten_premium_hanafiariful_gmail_com';"

echo ""
echo "=== Tambah unique constraint supaya tidak duplikat lagi ==="
sudo -u postgres psql posbah -c "ALTER TABLE employees ADD CONSTRAINT IF NOT EXISTS emp_email_tenant_unique UNIQUE (email, \"tenantId\") WHERE NOT \"isDeleted\";" 2>/dev/null || echo "Constraint sudah ada atau gagal (tidak apa)"

echo ""
echo "=== Verifikasi setelah hapus ==="
sudo -u postgres psql posbah -c "SELECT id, name, email, role, \"outletId\", \"isActive\" FROM employees WHERE \"tenantId\" = 'ten_premium_hanafiariful_gmail_com' ORDER BY id;"
