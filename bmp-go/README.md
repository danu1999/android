# Invoicing System - CV. Bahtera Mulya Plastik (Golang Version)

Sistem manajemen invoice dan stok barang untuk CV. Bahtera Mulya Plastik, kini dimigrasikan sepenuhnya ke ekosistem **Golang** untuk performa yang lebih baik.

## 🚀 Struktur Proyek

Proyek ini terbagi menjadi dua bagian utama:
- **`golang-backend`**: RESTful API menggunakan Golang, Gin Framework, dan SQLite.
- **`golang-frontend`**: Antarmuka pengguna modern menggunakan React/Vue dengan Vite dan TypeScript.

---

## 🛠️ Teknologi yang Digunakan

### Backend
- **Bahasa:** Go (Golang)
- **Framework:** Gin Gonic
- **Database:** SQLite (ORM menggunakan GORM)
- **Fitur:** Autentikasi JWT, Generasi PDF, Manajemen Stok, dan Cashflow.

### Frontend
- **Framework:** Vite + React/Vue (TypeScript)
- **Styling:** Tailwind CSS / Bootstrap
- **State Management:** Axios untuk API calls.

---

## 🏃 Cara Menjalankan Proyek

### 1. Prasyarat
Pastikan Anda sudah menginstal:
- [Go](https://golang.org/dl/) (versi 1.18+)
- [Node.js & npm](https://nodejs.org/)
- Git

### 2. Menjalankan Backend
```bash
cd golang-backend
# Install dependencies
go mod tidy

# Jalankan server
go run main.go
```
Server backend akan berjalan di `http://localhost:8080` (atau port yang dikonfigurasi).

### 3. Menjalankan Frontend
```bash
cd golang-frontend
# Install dependencies
npm install

# Jalankan mode development
npm run dev
```
Buka browser dan akses URL yang muncul di terminal (biasanya `http://localhost:5173`).

---

## 📊 Database
Proyek ini menggunakan SQLite untuk kemudahan portabilitas.
- File database utama: `golang-backend/invoice.db`
- Perubahan skema dikelola melalui auto-migration GORM di dalam kode Go.

---

## 📝 Catatan Migrasi
Proyek ini telah dimigrasikan dari versi sebelumnya yang berbasis Django (Python). Semua logic bisnis telah dipindahkan ke folder `golang-backend` untuk meningkatkan efisiensi dan kemudahan deployment.

---

## 👤 Author
**CV. Bahtera Mulya Plastik**
*Invoicing System Modernization*
