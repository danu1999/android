#!/bin/bash
# Deploy script for PosBah Backend
# Run this on your VPS: bash deploy.sh

set -e

echo "=== PosBah Backend Deployment ==="

# Stop current service
sudo systemctl stop posbah-backend

# Backup current binary
cp /home/muizz9900/posbah-backend /home/muizz9900/posbah-backend.bak

# Copy new binary and static assets
cp ./posbah-backend /home/muizz9900/posbah-backend
chmod +x /home/muizz9900/posbah-backend
cp ./admin.html /home/muizz9900/admin.html
mkdir -p /home/muizz9900/web
cp ./web/admin.html /home/muizz9900/web/admin.html 2>/dev/null || true
cp ./posbah-v*.apk /home/muizz9900/ 2>/dev/null || true
cp ./release_notes.txt /home/muizz9900/release_notes.txt 2>/dev/null || true

# Start service
sudo systemctl start posbah-backend

# Check status
sleep 2
sudo systemctl status posbah-backend --no-pager

echo ""
echo "=== Testing QR Endpoints ==="
echo "1. Testing qr-session..."
curl -s https://www.zedmz.cloud/api/auth/qr-session | python3 -m json.tool

echo ""
echo "2. Testing qr-check (should say 'expired' for dummy id)..."
curl -s "https://www.zedmz.cloud/api/auth/qr-check?sessionId=dummy" | python3 -m json.tool

echo ""
echo "=== Deployment Complete! ==="
