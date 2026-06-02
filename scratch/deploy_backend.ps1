$ErrorActionPreference = "Stop"

# 1. Build backend
Write-Host "Building backend..." -ForegroundColor Cyan
Set-Location backend
cmd /c "npm run build"
Set-Location ..

# 2. Deploy to VPS
$pemPath = "C:\Users\danus\Documents\muizz.pem"
$ip = "103.93.163.227"
$user = "muizz9900"

Write-Host "Uploading dist/index.js to VPS..." -ForegroundColor Cyan
scp -o StrictHostKeyChecking=no -i $pemPath backend/dist/index.js "${user}@${ip}:/home/muizz9900/posbah-backend/dist/index.js"

Write-Host "Uploading BMP PDF templates to VPS..." -ForegroundColor Cyan
scp -o StrictHostKeyChecking=no -i $pemPath -r bmp-go/golang-backend/templates/* "${user}@${ip}:/home/muizz9900/bmp-backend/templates/"

Write-Host "Restarting backend via PM2 on VPS..." -ForegroundColor Cyan
ssh -o StrictHostKeyChecking=no -i $pemPath "${user}@${ip}" "pm2 restart posbah-backend && pm2 status"

Write-Host "Backend successfully deployed!" -ForegroundColor Green
