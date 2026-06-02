package database

import (
	"log"
	"os"
	"time"

	"gorm.io/driver/postgres"
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"

	// Wajib di-import agar AutoMigrate mengenali struktur tabelnya
	"invoice-bmp-go/models"
)

var DB *gorm.DB

func ConnectDB() {
	var err error

	// 1. Deteksi Mode Demo
	demoMode := os.Getenv("DEMO_MODE") == "true"

	// 2. Ambil URL Database dari environment (Railway)
	dbURL := os.Getenv("DATABASE_URL")

	// 3. Logika Cerdas: Pilih Database
	if demoMode {
		// Jika Mode Demo aktif, paksa pakai SQLite (demo.db) agar tidak merubah data asli
		DB, err = gorm.Open(sqlite.Open("demo.db"), &gorm.Config{})
		log.Println("🧪 MODE DEMO AKTIF: Menggunakan SQLite (demo.db)!")
	} else if dbURL != "" {
		// Jika ada DATABASE_URL, berarti kita di Railway -> Pakai PostgreSQL
		DB, err = gorm.Open(postgres.Open(dbURL), &gorm.Config{})
		log.Println("🚀 Berhasil koneksi ke PostgreSQL (Railway)!")
	} else {
		// Jika kosong, berarti kita di laptop lokal -> Pakai SQLite
		DB, err = gorm.Open(sqlite.Open("invoice.db"), &gorm.Config{})
		log.Println("💻 Berhasil koneksi ke SQLite (Lokal)!")
	}

	// 3. Tangani jika error
	if err != nil {
		log.Fatal("❌ Gagal terhubung ke database: ", err)
	}

	// 4. Lakukan Migrasi Tabel otomatis (MEMBUAT TABEL DI DATABASE)
	// Pastikan &models.User{} ada di urutan pertama atau di dalam sini!
	err = DB.AutoMigrate(
		&models.User{},
		&models.Client{},
		&models.Invoice{},
		&models.MasterProduct{},
		&models.Product{},
		&models.InvoicePayment{},
		&models.CashFlow{},
		&models.Employee{},
		&models.Payroll{},
		&models.BahanNono{},
		&models.BahanNonoItem{},
		&models.PembelianBarang{},
		&models.PembelianItem{},
		&models.Pembayaran{},
		&models.Settings{},
		&models.AdmsDevice{},
		&models.AttendanceLog{},
		&models.MachineBonusLog{},
	)
	if err != nil {
		log.Println("⚠️ Peringatan: Gagal melakukan migrasi tabel ->", err)
	} else {
		log.Println("✅ Database Migrated Sukses!")
		// Panggil seeder otomatis untuk mempopulasi data demo jika tabel masih kosong
		SeedDemoData(DB)
	}

	// 5. Jalankan Goroutine untuk Auto-Cleanup log mesin absensi (lebih dari 14 hari)
	go func() {
		for {
			// Hitung batas waktu (14 hari yang lalu)
			cutoffDate := time.Now().Add(-14 * 24 * time.Hour)
			
			// Hapus permanen (Unscoped) log yang lebih tua dari 14 hari
			result := DB.Unscoped().Where("log_time < ?", cutoffDate).Delete(&models.AttendanceLog{})
			
			if result.Error == nil && result.RowsAffected > 0 {
				log.Printf("🧹 Auto-Cleanup: Berhasil menghapus %d log absensi yang usianya lebih dari 14 hari.\n", result.RowsAffected)
			}

			// Tidur selama 24 jam sebelum mengecek ulang
			time.Sleep(24 * time.Hour)
		}
	}()
}
