$ErrorActionPreference = "Stop"

# 1. Build frontend
Write-Host "Building frontend..." -ForegroundColor Cyan
Set-Location frontend
cmd /c "npm run build"
Set-Location ..

# 2. Zip dist folder
Write-Host "Zipping build..." -ForegroundColor Cyan
if (Test-Path frontend/dist.zip) {
    Remove-Item frontend/dist.zip
}
Compress-Archive -Path frontend/dist/* -DestinationPath frontend/dist.zip

# 3. Deploy to VPS
$pemPath = "C:\Users\danus\Documents\muizz.pem"
$ip = "103.93.163.227"
$user = "muizz9900"

Write-Host "Uploading dist.zip to VPS..." -ForegroundColor Cyan
scp -o StrictHostKeyChecking=no -i $pemPath frontend/dist.zip "${user}@${ip}:/home/muizz9900/dist.zip"

Write-Host "Extracting dist.zip on VPS..." -ForegroundColor Cyan
$remoteCommand = "sudo unzip -o /home/muizz9900/dist.zip -d /var/www/posbah && sudo systemctl reload nginx && echo 'Frontend successfully deployed!'"
ssh -o StrictHostKeyChecking=no -i $pemPath "${user}@${ip}" "$remoteCommand"
