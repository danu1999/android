# deploy_fix.ps1 — Upload handlers_rt.go (auth fix) ke VPS dan rebuild
# Jalankan dari: C:\Users\danus\Documents\antigravity\emergent\backend\
# Perintah: powershell -File deploy_fix.ps1

$VPS_HOST = "muizz9900@www.zedmz.cloud"
$BACKEND_DIR = "/home/muizz9900/posbah-app/backend"

Write-Host "=== Deploy Fix: handlers_rt.go (auth token fallback) ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Upload handlers_rt.go yang sudah difix
Write-Host "1. Upload handlers_rt.go ke VPS..." -ForegroundColor Yellow
scp handlers_rt.go "${VPS_HOST}:${BACKEND_DIR}/handlers_rt.go"
if ($LASTEXITCODE -ne 0) {
    Write-Host "GAGAL upload handlers_rt.go" -ForegroundColor Red
    exit 1
}
Write-Host "   OK" -ForegroundColor Green

# Step 2: SSH ke VPS, rebuild, dan restart service
Write-Host "2. Rebuild + restart service di VPS..." -ForegroundColor Yellow
$remoteCmd = @"
set -e
cd /home/muizz9900/posbah-app/backend
echo '  > go build...'
go build -o /home/muizz9900/posbah-backend .
echo '  > chmod...'
chmod +x /home/muizz9900/posbah-backend
echo '  > restart service...'
sudo systemctl restart posbah-backend
sleep 2
echo '  > status:'
sudo systemctl is-active posbah-backend
echo 'DEPLOY SUKSES!'
"@

ssh "${VPS_HOST}" $remoteCmd
if ($LASTEXITCODE -ne 0) {
    Write-Host "GAGAL rebuild di VPS" -ForegroundColor Red
    exit 1
}

Write-Host "" 
Write-Host "=== Deploy Selesai! ===" -ForegroundColor Green
Write-Host "Backend baru aktif dengan fix auth token (email fallback)" -ForegroundColor Green
Write-Host ""
Write-Host "Verifikasi endpoint:" -ForegroundColor Cyan
Write-Host "  curl https://www.zedmz.cloud/status"
