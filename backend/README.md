# PosBah Backend Server (Golang Edition)

Layanan backend API super cepat dan hemat memori berbasis **Golang** untuk VPS Anda yang terhubung dengan database **Supabase Cloud**. Server ini menjalankan:
1. **Background Cron Worker (Goroutine Ticker)**: Mengecek database setiap jam untuk mengunci (`isActive = false`) akun demo user yang berumur lebih dari 2 hari.
2. **REST API Endpoints**: Menyediakan endpoint status dan pemicu manual untuk administrasi sistem.

---

## Prasyarat Server (VPS)

1. OS: **Ubuntu 24.04 LTS** atau **Ubuntu 22.04 LTS** (direkomendasikan).
2. **Golang** (Go 1.21 atau lebih baru).
3. **PM2** (atau Systemd) untuk menjaga binary tetap hidup di background.

---

## Langkah Instalasi & Build di VPS

### 1. Update OS & Install Golang
Jalankan perintah berikut di terminal VPS Anda:
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install golang-go -y
```

### 2. Copy Folder Code ke VPS
Gunakan Git, SCP, atau SFTP untuk mengunggah folder `backend` ini ke VPS Anda. Misalnya ke folder `/var/www/posbah-backend`.

### 3. Build Go Binary
Masuk ke folder `backend` di VPS dan lakukan kompilasi:
```bash
cd /var/www/posbah-backend
go build -o posbah-backend main.go
```
Perintah ini akan menghasilkan file binary mandiri bernama `posbah-backend` yang berukuran sangat kecil dan cepat.

### 4. Jalankan Service di Background (Pakai PM2)
PM2 dapat memonitor file binary hasil kompilasi:
1. Pastikan Anda memiliki PM2 (jika belum, jalankan `sudo apt install npm -y && sudo npm install -g pm2`).
2. Jalankan binary dengan parameter environment variable Supabase Anda:
   ```bash
   SUPABASE_URL="https://etustetneufkfilndimy.supabase.co" \
   SUPABASE_SECRET_KEY="YOUR_SUPABASE_SECRET_KEY" \
   PORT=3000 \
   pm2 start ./posbah-backend --name "posbah-api"
   ```

Cek status dan log untuk memastikan server berjalan normal:
```bash
pm2 status
pm2 logs posbah-api
```

### 5. Setup Auto-start PM2
Agar otomatis hidup ketika VPS restart:
```bash
pm2 startup
# (Jalankan perintah yang disarankan di terminal Anda)
pm2 save
```

---

## Endpoint Dokumentasi

* **GET `/status`**: Cek status server.
* **POST `/api/admin/check-demo-lockout`**: Jalankan pembersihan demo user secara manual (mempercepat proses testing lockout 2 hari).
