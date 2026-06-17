#!/bin/bash
# Script cleanup pinHash di PostgreSQL + compile backend baru
set -e

echo "=== [1] Cleanup pinHash di database ==="
psql -d posbah -c "
SELECT COUNT(*) AS baris_dengan_pinhash FROM local_users WHERE \"pinHash\" IS NOT NULL;
" 2>/dev/null || sudo -u postgres psql -d posbah -c "
SELECT COUNT(*) AS baris_dengan_pinhash FROM local_users WHERE \"pinHash\" IS NOT NULL;
"

# NULL-kan pinHash (tidak drop column agar schema tetap kompatibel)
psql -d posbah -c "
UPDATE local_users SET \"pinHash\" = NULL WHERE \"pinHash\" IS NOT NULL;
SELECT COUNT(*) AS sisa_pinhash FROM local_users WHERE \"pinHash\" IS NOT NULL;
" 2>/dev/null || sudo -u postgres psql -d posbah -c "
UPDATE local_users SET \"pinHash\" = NULL WHERE \"pinHash\" IS NOT NULL;
SELECT COUNT(*) AS sisa_pinhash FROM local_users WHERE \"pinHash\" IS NOT NULL;
"
echo "pinHash cleanup selesai"

echo ""
echo "=== [2] Update admin.html dan backend binary ==="
cd /home/muizz9900/posbah-app
git pull --ff-only

# Stop service, copy binary baru
sudo systemctl stop posbah-backend
cp backend/posbah-backend /home/muizz9900/posbah-backend
chmod +x /home/muizz9900/posbah-backend

# Copy admin.html baru (dengan blast email UI)
cp backend/admin.html /home/muizz9900/admin.html
mkdir -p /home/muizz9900/web
cp backend/admin.html /home/muizz9900/web/admin.html

# Copy service file
sudo cp backend/posbah-backend.service /etc/systemd/system/posbah-backend.service
sudo systemctl daemon-reload

echo ""
echo "=== [3] Start backend ==="
sudo systemctl start posbah-backend
sleep 3
sudo systemctl status posbah-backend --no-pager | head -6

echo ""
echo "=== [4] Verifikasi endpoint blast email ==="
curl -sf http://localhost:3000/status
echo ""
echo "SEMUA SELESAI"
