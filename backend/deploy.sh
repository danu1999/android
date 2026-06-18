#!/bin/bash
# ============================================================
# POSBah Backend — Deploy Script (dijalankan di VPS)
# Cara pakai: cd /home/muizz9900/posbah-app && bash backend/deploy.sh
# ============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_LOG="/home/muizz9900/deploy_log.txt"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$DEPLOY_LOG"
}

# ============================================================
# PERIKSA ENV FILE SEBELUM DEPLOY
# ============================================================
ENV_FILE="/home/muizz9900/.posbah-env"
if [ ! -f "$ENV_FILE" ]; then
    log "ERROR: File env tidak ditemukan: $ENV_FILE"
    log "Buat dari template: cp backend/.posbah-env.example $ENV_FILE"
    log "Lalu isi nilai yang benar dan jalankan: chmod 600 $ENV_FILE"
    exit 1
fi

# Pastikan env kritis ada nilainya
source "$ENV_FILE"
if [ -z "$ADMIN_AUTH_TOKEN" ]; then
    log "ERROR: ADMIN_AUTH_TOKEN kosong di $ENV_FILE"
    exit 1
fi
if [ -z "$DATABASE_URL" ]; then
    log "ERROR: DATABASE_URL kosong di $ENV_FILE"
    exit 1
fi

log "=== POSBah Backend Deployment ==="

# ============================================================
# STOP SERVICE
# ============================================================
log "Menghentikan service..."
sudo systemctl stop posbah-backend || true

# ============================================================
# BACKUP BINARY LAMA
# ============================================================
if [ -f "/home/muizz9900/posbah-backend" ]; then
    cp /home/muizz9900/posbah-backend /home/muizz9900/posbah-backend.bak
    log "Backup binary lama: OK"
fi

# ============================================================
# COPY BINARY BARU DAN ASET STATIS
# ============================================================
log "Menyalin binary baru..."
cp "$SCRIPT_DIR/posbah-backend" /home/muizz9900/posbah-backend
chmod +x /home/muizz9900/posbah-backend

log "Menyalin admin.html..."
cp "$SCRIPT_DIR/admin.html" /home/muizz9900/admin.html

mkdir -p /home/muizz9900/web
cp "$SCRIPT_DIR/admin.html"  /home/muizz9900/web/admin.html
cp "$SCRIPT_DIR/index.html"  /home/muizz9900/web/index.html 2>/dev/null || true
cp "$SCRIPT_DIR/app.js"      /home/muizz9900/web/app.js    2>/dev/null || true
cp "$SCRIPT_DIR/style.css"   /home/muizz9900/web/style.css 2>/dev/null || true
cp "$SCRIPT_DIR/privacy.html" /home/muizz9900/web/privacy.html 2>/dev/null || true
cp "$SCRIPT_DIR/terms.html"   /home/muizz9900/web/terms.html 2>/dev/null || true

log "Menyalin APK dan release notes..."
cp "$SCRIPT_DIR"/posbah-v*.apk /home/muizz9900/ 2>/dev/null || true
cp "$SCRIPT_DIR/release_notes.txt" /home/muizz9900/release_notes.txt 2>/dev/null || true

# ============================================================
# COPY SERVICE FILE JIKA BERUBAH + RELOAD SYSTEMD
# ============================================================
SERVICE_SRC="$SCRIPT_DIR/posbah-backend.service"
SERVICE_DEST="/etc/systemd/system/posbah-backend.service"
if ! diff -q "$SERVICE_SRC" "$SERVICE_DEST" > /dev/null 2>&1; then
    log "Service file berubah — memperbarui systemd..."
    sudo cp "$SERVICE_SRC" "$SERVICE_DEST"
    sudo systemctl daemon-reload
    log "systemd daemon-reload: OK"
fi

# ============================================================
# HAPUS APK LAMA — PERTAHANKAN 2 TERBARU
# ============================================================
log "Cleanup APK lama di /home/muizz9900/..."
APK_COUNT=$(ls /home/muizz9900/posbah-v*.apk 2>/dev/null | wc -l)
if [ "$APK_COUNT" -gt 2 ]; then
    ls -v /home/muizz9900/posbah-v*.apk | head -n -2 | while read f; do
        rm -f "$f"
        log "  Dihapus: $(basename $f)"
    done
    log "Cleanup APK selesai (total sebelumnya: $APK_COUNT, sekarang: 2)"
else
    log "APK di VPS: $APK_COUNT file — tidak perlu cleanup"
fi

# ============================================================
# START SERVICE
# ============================================================
log "Memulai service..."
sudo systemctl start posbah-backend
sleep 3
sudo systemctl status posbah-backend --no-pager | tee -a "$DEPLOY_LOG"

# ============================================================
# VERIFIKASI
# ============================================================
log "Verifikasi endpoint..."
sleep 2
curl -sf https://www.zedmz.cloud/status | python3 -m json.tool 2>/dev/null || log "WARNING: /status tidak merespons"

log ""
log "=== Deployment Selesai! ==="
log "Jika server berjalan normal, kirim blast email update via Admin Panel:"
log "  https://www.zedmz.cloud/admin"
