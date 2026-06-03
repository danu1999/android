$ErrorActionPreference = "Stop"

# 1. Backup images if exists
$publicDir = "bmp-go/golang-backend/public"
$backupImages = "bmp-go/golang-backend/temp_images_backup"
Write-Host "Backing up images if exists..." -ForegroundColor Cyan
if (Test-Path "$publicDir/images") {
    if (Test-Path $backupImages) { Remove-Item -Recurse -Force $backupImages }
    Copy-Item -Path "$publicDir/images" -Destination $backupImages -Recurse
}

# 2. Build React BMP Frontend
Write-Host "Building React BMP Frontend..." -ForegroundColor Cyan
Set-Location bmp-go/golang-frontend
cmd /c "npm run build"
Set-Location ../..

# 3. Restore images
Write-Host "Restoring images..." -ForegroundColor Cyan
if (Test-Path $backupImages) {
    Copy-Item -Path "$backupImages/*" -Destination "$publicDir/images" -Recurse
    Remove-Item -Recurse -Force $backupImages
}

# 3. Compile Go backend for Linux
Write-Host "Compiling Go backend for Linux (amd64)..." -ForegroundColor Cyan
Set-Location bmp-go/golang-backend
$env:GOOS = "linux"
$env:GOARCH = "amd64"
cmd /c "go build -o invoice-bmp-go main.go"
Set-Location ../..

# 4. Deploy files to VPS
$pemPath = "C:\Users\danus\Documents\muizz.pem"
$ip = "103.93.163.227"
$user = "muizz9900"
$vpsPath = "/home/muizz9900/bmp-backend"

Write-Host "Uploading Go binary to VPS..." -ForegroundColor Cyan
scp -o StrictHostKeyChecking=no -i $pemPath bmp-go/golang-backend/invoice-bmp-go "${user}@${ip}:${vpsPath}/invoice-bmp-go.new"

Write-Host "Uploading template files to VPS..." -ForegroundColor Cyan
scp -o StrictHostKeyChecking=no -i $pemPath -r bmp-go/golang-backend/templates/* "${user}@${ip}:${vpsPath}/templates/"

Write-Host "Uploading public static assets to VPS..." -ForegroundColor Cyan
# Zip the public folder to upload efficiently
if (Test-Path bmp-go/golang-backend/public.zip) { Remove-Item bmp-go/golang-backend/public.zip }
Compress-Archive -Path bmp-go/golang-backend/public/* -DestinationPath bmp-go/golang-backend/public.zip
scp -o StrictHostKeyChecking=no -i $pemPath bmp-go/golang-backend/public.zip "${user}@${ip}:${vpsPath}/public.zip"

Write-Host "Extracting assets, replacing binary, and restarting PM2 instances on VPS..." -ForegroundColor Cyan
$remoteCommand = "mv ${vpsPath}/invoice-bmp-go.new ${vpsPath}/invoice-bmp-go && chmod +x ${vpsPath}/invoice-bmp-go && unzip -o ${vpsPath}/public.zip -d ${vpsPath}/public && rm ${vpsPath}/public.zip && pm2 restart bmp-backend && pm2 restart bmp-backend-demo && pm2 status"
ssh -o StrictHostKeyChecking=no -i $pemPath "${user}@${ip}" "$remoteCommand"

# Clean local Linux binary and zip file
Remove-Item bmp-go/golang-backend/invoice-bmp-go
Remove-Item bmp-go/golang-backend/public.zip

Write-Host "BMP module successfully deployed to VPS!" -ForegroundColor Green
