#!/bin/bash
# ============================================================
# POSBah VPS Database Backup Script
# Keep daily pg_dump backups for the last 7 days
# ============================================================

BACKUP_DIR="/home/muizz9900/backups"
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +"%Y-%m-%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/posbah_backup_$TIMESTAMP.dump"

ENV_FILE="/home/muizz9900/.posbah-env"
if [ -f "$ENV_FILE" ]; then
    source "$ENV_FILE"
fi

if [ -z "$DATABASE_URL" ]; then
    DATABASE_URL="postgres://postgres:Bahtera1!@localhost:5432/posbah?sslmode=disable"
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Memulai backup database POSBah..."

# Run pg_dump in custom binary format (-F c)
if pg_dump "$DATABASE_URL" -F c -b -v -f "$BACKUP_FILE"; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup sukses: $(basename "$BACKUP_FILE")"
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: Backup database gagal!"
    exit 1
fi

# Rotate backups: delete backups older than 7 days
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Melakukan pembersihan backup lama (lebih dari 7 hari)..."
find "$BACKUP_DIR" -name "posbah_backup_*.dump" -type f -mtime +7 -exec rm -f {} \; -print | while read f; do
    echo "  Dihapus: $(basename "$f")"
done

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Proses backup selesai."
