<#
.SYNOPSIS
    POSBah Build & Deploy Automation v2 - Full Pipeline

.DESCRIPTION
    Otomasi penuh rilis POSBah:
    1. Baca versi dari build.gradle.kts secara otomatis
    2. Build APK Android (debug/release)
    3. Commit semua perubahan kode ke git + push ke GitHub
    4. Cross-compile backend Go untuk Linux (opsional, butuh Go terinstall)
    5. Upload APK ke VPS via SCP langsung (karena APK tidak di git)
    6. Trigger auto-deploy via Admin Panel API
    
    Untuk rilis berikutnya cukup jalankan: .\build_and_deploy.ps1

.PARAMETER BuildType
    "debug" (default, cepat) atau "release" (produksi, butuh keystore)

.PARAMETER SkipBuild
    Lewati gradle build, cari APK yang sudah ada

.PARAMETER SkipGitPush
    Lewati commit + push git

.PARAMETER SkipGoCompile
    Lewati cross-compile Go backend (gunakan binary yang sudah ada)

.PARAMETER SkipApkUpload
    Lewati upload APK ke VPS via SCP

.PARAMETER SkipDeploy
    Lewati trigger admin panel deploy

.PARAMETER VpsHost
    SSH host VPS (default: muizz9900@zedmz.cloud)

.PARAMETER VpsApkDir
    Direktori tujuan APK di VPS (default: /home/muizz9900)

.PARAMETER AdminUrl
    URL Admin Panel (default: https://www.zedmz.cloud)

.PARAMETER AdminPassword
    Password Admin Panel (jika kosong, diminta via prompt)

.EXAMPLE
    # Rilis penuh (build debug + upload + deploy)
    .\build_and_deploy.ps1

    # Rilis release (butuh keystore tersedia)
    .\build_and_deploy.ps1 -BuildType release

    # Hanya upload APK yang sudah ada + deploy (tanpa rebuild)
    .\build_and_deploy.ps1 -SkipBuild -SkipGitPush

    # Hanya push kode + deploy, tanpa APK
    .\build_and_deploy.ps1 -SkipBuild -SkipApkUpload

    # Dry run: hanya build, tidak push/upload/deploy
    .\build_and_deploy.ps1 -SkipGitPush -SkipApkUpload -SkipDeploy
#>

param(
    [ValidateSet("debug", "release")]
    [string]$BuildType = "debug",

    [switch]$SkipBuild,
    [switch]$SkipGitPush,
    [switch]$SkipGoCompile,
    [switch]$SkipApkUpload,
    [switch]$SkipDeploy,

    [string]$VpsHost    = "muizz9900@zedmz.cloud",
    [string]$VpsPemKey  = "C:\Users\danus\Documents\muizz.pem",
    [string]$VpsApkDir  = "/home/muizz9900",
    [string]$AdminUrl   = "https://www.zedmz.cloud",
    [string]$AdminEmail = "muhammadmuizz8@gmail.com",
    [string]$AdminPassword = ""
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir  = Join-Path $ProjectRoot "backend"
$AppDir      = Join-Path $ProjectRoot "app"
$GradleW     = Join-Path $ProjectRoot "gradlew.bat"

# Set JAVA_HOME otomatis jika belum terkonfigurasi
if (-not $env:JAVA_HOME) {
    $fallbackJava = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path $fallbackJava) {
        $env:JAVA_HOME = $fallbackJava
    }
}

# ============================================================
# UTILS
# ============================================================
function Write-Step([string]$msg) {
    Write-Host ""
    Write-Host "===[ $msg ]===" -ForegroundColor Cyan
}
function Write-OK([string]$msg)   { Write-Host "  [OK]  $msg" -ForegroundColor Green }
function Write-Fail([string]$msg) { Write-Host "  [ERR] $msg" -ForegroundColor Red }
function Write-Info([string]$msg) { Write-Host "  [..] $msg"  -ForegroundColor Yellow }

# ============================================================
# STEP 0: BACA VERSI DARI build.gradle.kts
# ============================================================
Write-Step "Membaca versi"
$gradleFile = Join-Path $AppDir "build.gradle.kts"
$gradleContent = Get-Content $gradleFile -Raw
if ($gradleContent -match 'versionCode\s*=\s*(\d+)')    { $versionCode = $Matches[1] }
if ($gradleContent -match 'versionName\s*=\s*"([^"]+)"') { $versionName = $Matches[1] }
if (-not $versionName) {
    Write-Fail "Tidak bisa baca versionName dari build.gradle.kts"
    exit 1
}

$apkSuffix   = if ($BuildType -eq "release") { "" } else { "-debug" }
$apkFileName = "posbah-v$versionName$apkSuffix.apk"
$apkLocalPath = Join-Path $AppDir "build\outputs\apk\$BuildType\$apkFileName"

Write-OK "Versi: $versionName (versionCode: $versionCode)"
Write-OK "APK target: $apkFileName"

# ============================================================
# STEP 1: BUILD APK ANDROID
# ============================================================
if (-not $SkipBuild) {
    Write-Step "Build APK Android ($BuildType)"
    if (-not (Test-Path $GradleW)) {
        Write-Fail "gradlew.bat tidak ditemukan: $GradleW"
        exit 1
    }
    $task = if ($BuildType -eq "release") { "assembleRelease" } else { "assembleDebug" }
    Write-Info "Menjalankan: gradlew $task"
    Push-Location $ProjectRoot
    try {
        cmd /c "gradlew.bat $task 2>&1"
        if ($LASTEXITCODE -ne 0) { Write-Fail "Build gagal"; exit 1 }
    } finally { Pop-Location }
    Write-OK "Build selesai"
} else {
    Write-Info "Build dilewati (-SkipBuild)"
}

# Verifikasi APK ada jika akan di-upload
if (-not $SkipApkUpload) {
    if (-not (Test-Path $apkLocalPath)) {
        # Coba fallback nama lama
        $fallback = Join-Path $AppDir "build\outputs\apk\$BuildType\app-$BuildType.apk"
        if (Test-Path $fallback) {
            $apkLocalPath = $fallback
            Write-Info "APK ditemukan dengan nama lama: $fallback"
        } else {
            Write-Fail "APK tidak ditemukan: $apkLocalPath"
            Write-Info "Jalankan tanpa -SkipBuild, atau periksa hasil build"
            exit 1
        }
    }
    $apkSizeMB = '{0:N1}' -f ((Get-Item $apkLocalPath).Length / 1MB)
    Write-OK "APK ditemukan: $apkLocalPath ($apkSizeMB MB)"
}

# ============================================================
# STEP 2: CROSS-COMPILE BACKEND GO UNTUK LINUX
# ============================================================
if (-not $SkipGoCompile) {
    Write-Step "Cross-compile backend Go untuk Linux/amd64"
    $goExe = Get-Command go -ErrorAction SilentlyContinue
    if (-not $goExe) {
        Write-Info "Go tidak terinstall - melewati cross-compile (gunakan binary lama)"
    } else {
        Push-Location $BackendDir
        try {
            $env:GOOS   = "linux"
            $env:GOARCH = "amd64"
            $env:CGO_ENABLED = "0"
            Write-Info "GOOS=linux GOARCH=amd64 go build -o posbah-backend"
            go build -ldflags="-s -w" -o posbah-backend .
            if ($LASTEXITCODE -ne 0) {
                Write-Fail "Go build gagal"
            } else {
                Write-OK "Binary Linux berhasil dikompilasi: backend/posbah-backend"
            }
        } finally {
            Remove-Item env:GOOS   -ErrorAction SilentlyContinue
            Remove-Item env:GOARCH -ErrorAction SilentlyContinue
            Remove-Item env:CGO_ENABLED -ErrorAction SilentlyContinue
            Pop-Location
        }
    }
} else {
    Write-Info "Kompilasi Go dilewati (-SkipGoCompile)"
}

# ============================================================
# STEP 3: COMMIT + PUSH KE GITHUB
# ============================================================
if (-not $SkipGitPush) {
    Write-Step "Commit & Push ke GitHub"
    Push-Location $ProjectRoot
    try {
        git add -A
        $hasChanges = (git status --porcelain).Trim()
        if ($hasChanges) {
            $commitMsg = "release: v$versionName (versionCode $versionCode)"
            git commit -m $commitMsg
            Write-OK "Commit: $commitMsg"
        } else {
            Write-Info "Tidak ada perubahan baru untuk di-commit"
        }
        git push
        if ($LASTEXITCODE -ne 0) { Write-Fail "Git push gagal"; exit 1 }
        Write-OK "Push ke GitHub berhasil"
    } finally { Pop-Location }
} else {
    Write-Info "Git push dilewati (-SkipGitPush)"
}

# ============================================================
# STEP 4: UPLOAD APK KE VPS VIA SCP
# ============================================================
if (-not $SkipApkUpload) {
    Write-Step "Upload APK ke VPS via SCP"

    # Cek apakah ssh/scp tersedia
    $scpExe = Get-Command scp -ErrorAction SilentlyContinue
    if (-not $scpExe) {
        Write-Fail "scp tidak ditemukan. Install OpenSSH atau gunakan PuTTY/WinSCP."
        Write-Info "Upload manual: scp `"$apkLocalPath`" ${VpsHost}:${VpsApkDir}/${apkFileName}"
    } else {
        $scpArgs = @("-i", $VpsPemKey, "-o", "StrictHostKeyChecking=no")
        $vpsTarget = "${VpsHost}:${VpsApkDir}/${apkFileName}"
        Write-Info "Upload: $apkLocalPath -> $vpsTarget"
        scp @scpArgs "$apkLocalPath" $vpsTarget
        if ($LASTEXITCODE -ne 0) {
            Write-Fail "SCP upload gagal. Coba upload manual:"
            Write-Info "  scp `"$apkLocalPath`" $vpsTarget"
        } else {
            Write-OK "APK berhasil diupload ke VPS"
        }
    }
} else {
    Write-Info "Upload APK dilewati (-SkipApkUpload)"
}

# ============================================================
# STEP 5: TRIGGER AUTO-DEPLOY VIA ADMIN PANEL API
# ============================================================
if (-not $SkipDeploy) {
    Write-Step "Trigger deploy di VPS via Admin Panel"

    if (-not $AdminPassword) {
        $secPwd = Read-Host "Password Admin Panel ($AdminUrl/admin)" -AsSecureString
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPwd)
        $AdminPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
    }

    try {
        # Login
        Write-Info "Login ke admin panel..."
        $loginResp = Invoke-RestMethod `
            -Uri "$AdminUrl/api/admin/login" `
            -Method POST `
            -Body (@{ email = $AdminEmail; password = $AdminPassword } | ConvertTo-Json) `
            -ContentType "application/json" `
            -SessionVariable "sess"

        if (-not $loginResp.success) {
            Write-Fail "Login gagal: $($loginResp.message)"
            Write-Info "Deploy manual via: $AdminUrl/admin"
        } else {
            Write-OK "Login berhasil"

            # Trigger deploy
            Write-Info "Mengirim perintah deploy..."
            $headers = @{
                "Authorization" = "Bearer $($loginResp.token)"
            }
            $deployResp = Invoke-RestMethod `
                -Uri "$AdminUrl/api/admin/deploy" `
                -Method POST `
                -Headers $headers `
                -ContentType "application/json"

            if ($deployResp.success) {
                Write-OK "Deploy dipicu! Server sedang: git pull -> compile -> restart"
                Write-Info "Monitor: $AdminUrl/admin (tab Deploy Log)"
                Write-Info ""
                Write-Info "Setelah server restart (~30 detik), kirim blast email update"
                Write-Info "via Admin Panel -> Kirim Notifikasi ke Semua User"
            } else {
                Write-Fail "Deploy API gagal: $($deployResp.message)"
            }
        }
    } catch {
        Write-Fail "Koneksi ke admin panel gagal: $_"
        Write-Info "Deploy manual: buka $AdminUrl/admin dan klik tombol Deploy"
    }
} else {
    Write-Info "Deploy dilewati (-SkipDeploy)"
}

# ============================================================
# RINGKASAN
# ============================================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Rilis POSBah v$versionName selesai!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Checklist pasca-deploy:" -ForegroundColor White
Write-Host "  [ ] Cek server berjalan: curl $AdminUrl/status" -ForegroundColor Gray
Write-Host "  [ ] Cek versi APK terdaftar: curl $AdminUrl/api/apk-version" -ForegroundColor Gray
Write-Host "  [ ] Kirim blast email via Admin Panel: $AdminUrl/admin" -ForegroundColor Gray
Write-Host ""

# ============================================================
# CATATAN VPS: PASTIKAN .posbah-env SUDAH ADA
# ============================================================
Write-Host "PENTING - Sebelum deploy pertama:" -ForegroundColor Yellow
Write-Host "  Di VPS, buat file env:" -ForegroundColor Gray
Write-Host "    cp ~/posbah-app/backend/.posbah-env.example ~/.posbah-env" -ForegroundColor Gray
Write-Host "    nano ~/.posbah-env   # isi ADMIN_AUTH_TOKEN dan DATABASE_URL" -ForegroundColor Gray
Write-Host "    chmod 600 ~/.posbah-env" -ForegroundColor Gray
Write-Host "    sudo systemctl daemon-reload" -ForegroundColor Gray
Write-Host "    sudo systemctl restart posbah-backend" -ForegroundColor Gray
