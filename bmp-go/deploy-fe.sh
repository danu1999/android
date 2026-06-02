#!/bin/bash

# Script untuk membuild frontend dan menyalin hasil ke backend
# Jalankan dari root project: ./deploy-fe.sh

set -e # Berhenti jika ada error

echo "🚀 Memulai proses build frontend..."

# Masuk ke folder frontend
cd golang-frontend

# Build frontend
npm run build

echo "✅ Build selesai. Menyalin file ke backend..."

# Backup folder images agar tidak terhapus
if [ -d "../golang-backend/public/images" ]; then
    cp -r ../golang-backend/public/images /tmp/bmp-images-backup
fi

# Hapus isi folder public backend
rm -rf ../golang-backend/public/*

# Copy hasil build baru ke folder public backend
cp -r dist/* ../golang-backend/public/

# Restore folder images
if [ -d "/tmp/bmp-images-backup" ]; then
    cp -r /tmp/bmp-images-backup ../golang-backend/public/images
    rm -rf /tmp/bmp-images-backup
fi

echo "🎉 Selesai! File frontend telah diperbarui di folder public backend."
