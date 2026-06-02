package database

import (
	"invoice-bmp-go/models"
	"log"
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

func SeedDemoData(db *gorm.DB) {
	// Pengecekan apakah database sudah terisi (berdasarkan Client)
	var clientCount int64
	db.Model(&models.Client{}).Count(&clientCount)
	if clientCount > 0 {
		log.Println("💡 Seeder: Database sudah memiliki data, melewati proses seeding.")
		return
	}

	log.Println("🌱 Seeder: Memulai proses seeding data simulasi manufaktur plastik...")

	// 1. Seed Settings Pabrik
	settings := models.Settings{
		ClientName:         "BMP - Bintang Makmur Plastindo",
		ClientLogo:         "",
		AddressLine1:       "Jl. Industri Raya No. 45, Kawasan Industri Candi",
		Province:           "Jawa Tengah",
		PostalCode:         "50181",
		PhoneNumber:        "024-7654321",
		EmailAddress:       "info@bintangmakmurplastindo.com",
		TaxNumber:          "01.234.567.8-901.000",
		ListrikBulanan:     32500000,
		JumlahMesin:        6,
		JumlahKaryawan:     15,
		GajiHarian:         85000,
		HariKerjaSebulan:   26,
		BiayaKarungPer1000: 2150000,
		UniqueID:           uuid.New().String()[:8],
		Slug:               "bmp-bintang-makmur-plastindo",
		DateCreated:        time.Now(),
		LastUpdated:        time.Now(),
	}
	db.Create(&settings)

	// 2. Seed Clients
	clients := []models.Client{
		{
			ClientName:   "PT Sinar Agung Plastindo",
			AddressLine1: "Kawasan Industri Jatake Blok C/12, Tangerang",
			Province:     "Banten",
			PostalCode:   "15137",
			PhoneNumber:  "021-5901122",
			EmailAddress: "purchasing@sinaragung.co.id",
			TaxNumber:    "31.889.771.2-411.000",
			UniqueID:     uuid.New().String()[:8],
			Slug:         "pt-sinar-agung-plastindo-" + uuid.New().String()[:4],
			DateCreated:  time.Now().Add(-30 * 24 * time.Hour),
			LastUpdated:  time.Now(),
			SaldoTitipan: 2500000,
		},
		{
			ClientName:   "Toko Maju Jaya Plastik",
			AddressLine1: "Jl. Astana Anyar No. 142, Bandung",
			Province:     "Jawa Barat",
			PostalCode:   "40241",
			PhoneNumber:  "022-4209988",
			EmailAddress: "majujepl@gmail.com",
			TaxNumber:    "",
			UniqueID:     uuid.New().String()[:8],
			Slug:         "toko-maju-jaya-plastik-" + uuid.New().String()[:4],
			DateCreated:  time.Now().Add(-20 * 24 * time.Hour),
			LastUpdated:  time.Now(),
			SaldoTitipan: 0,
		},
		{
			ClientName:   "UD Plastik Makmur Sejahtera",
			AddressLine1: "Jl. Kenjeran No. 288, Surabaya",
			Province:     "Jawa Timur",
			PostalCode:   "60142",
			PhoneNumber:  "031-3814455",
			EmailAddress: "makmursej.ud@yahoo.com",
			TaxNumber:    "",
			UniqueID:     uuid.New().String()[:8],
			Slug:         "ud-plastik-makmur-sejahtera-" + uuid.New().String()[:4],
			DateCreated:  time.Now().Add(-15 * 24 * time.Hour),
			LastUpdated:  time.Now(),
			SaldoTitipan: 0,
		},
	}
	for i := range clients {
		db.Create(&clients[i])
	}

	// 3. Seed Master Products
	masterProducts := []models.MasterProduct{
		{
			Title:       "Kantong HDPE Bening 30x50 (Tebal 0.03)",
			Description: "Kantong plastik high-density polyethylene bening kualitas premium untuk kemasan berat.",
			Unit:        "Kg",
			Price:       23500,
			BeratGram:   14.5,
			CycleTime:   3.2,
			UniqueID:     uuid.New().String()[:8],
			Slug:         "kantong-hdpe-bening-30x50",
			DateCreated:  time.Now().Add(-40 * 24 * time.Hour),
			LastUpdated:  time.Now(),
		},
		{
			Title:       "Sedotan Hitam Steril 6mm (Isi 500)",
			Description: "Sedotan plastik steril dibungkus kertas per pcs, warna hitam pekat higienis.",
			Unit:        "Pack",
			Price:       9000,
			BeratGram:   1.2,
			CycleTime:   0.6,
			UniqueID:     uuid.New().String()[:8],
			Slug:         "sedotan-hitam-steril-6mm",
			DateCreated:  time.Now().Add(-40 * 24 * time.Hour),
			LastUpdated:  time.Now(),
		},
		{
			Title:       "Plastik Cor PE Lebar 3m (Double Sheet)",
			Description: "Plastik cor PE daur ulang kualitas baik untuk proyek konstruksi dan pengecoran.",
			Unit:        "Roll",
			Price:       380000,
			BeratGram:   16500,
			CycleTime:   40,
			UniqueID:     uuid.New().String()[:8],
			Slug:         "plastik-cor-pe-lebar-3m",
			DateCreated:  time.Now().Add(-40 * 24 * time.Hour),
			LastUpdated:  time.Now(),
		},
		{
			Title:       "Cup Plastik 16oz PP Tebal 7gr",
			Description: "Gelas cup plastik bahan Polypropylene (PP) tebal tahan panas untuk minuman boba/kopi.",
			Unit:        "Karton",
			Price:       135000,
			BeratGram:   7.0,
			CycleTime:   1.4,
			UniqueID:     uuid.New().String()[:8],
			Slug:         "cup-plastik-16oz-pp-tebal",
			DateCreated:  time.Now().Add(-40 * 24 * time.Hour),
			LastUpdated:  time.Now(),
		},
	}
	for i := range masterProducts {
		db.Create(&masterProducts[i])
	}

	// 4. Seed Employees
	employees := []models.Employee{
		{
			Name:           "Budi Santoso",
			Position:       "Operator Extruder 1",
			SalaryAmount:   95000,
			FingerprintPIN: "1001",
			IsActive:       true,
		},
		{
			Name:           "Joko Susilo",
			Position:       "Helper Gulung & Potong",
			SalaryAmount:   80000,
			FingerprintPIN: "1002",
			IsActive:       true,
		},
		{
			Name:           "Siti Aminah",
			Position:       "Checker & Packing",
			SalaryAmount:   85000,
			FingerprintPIN: "1003",
			IsActive:       true,
		},
	}
	for i := range employees {
		db.Create(&employees[i])
	}

	// 5. Seed Invoices, Products, Payments, and Cashflow
	// --- INVOICE 1: LUNAS (PT Sinar Agung) ---
	due1 := time.Now().Add(-10 * 24 * time.Hour)
	inv1 := models.Invoice{
		Title:        "Pembelian Plastik Cor Proyek Tangerang",
		Number:       "INV/2026/05/001",
		DueDate:      &due1,
		PaymentTerms: "COD",
		Status:       "PAID",
		Notes:        "Barang dikirim menggunakan truk pabrik. Pembayaran tunai saat barang sampai.",
		ClientID:     &clients[0].ID,
		UniqueID:     uuid.New().String()[:8],
		Slug:         "inv-2026-05-001",
		DateCreated:  time.Now().Add(-15 * 24 * time.Hour),
		LastUpdated:  time.Now(),
	}
	db.Create(&inv1)

	// Produk invoice 1
	prod1 := models.Product{
		MasterItemID: &masterProducts[2].ID,
		Title:        masterProducts[2].Title,
		Unit:         masterProducts[2].Unit,
		Price:        masterProducts[2].Price,
		JumlahLusin:  1,
		Quantity:     20, // 20 Roll Plastik Cor
		UniqueID:     uuid.New().String()[:8],
		Slug:         "prod-inv1-1",
		InvoiceID:    &inv1.ID,
		DateCreated:  time.Now().Add(-15 * 24 * time.Hour),
		LastUpdated:  time.Now(),
	}
	db.Create(&prod1)

	subtotal1 := prod1.Quantity * prod1.JumlahLusin * prod1.Price // 7.600.000

	pay1 := models.InvoicePayment{
		InvoiceID:     inv1.ID,
		PaymentDate:   time.Now().Add(-14 * 24 * time.Hour),
		PaymentAmount: int(subtotal1),
		PaymentMethod: "TRANSFER_BCA",
		DateCreated:   time.Now().Add(-14 * 24 * time.Hour),
	}
	db.Create(&pay1)

	cf1 := models.CashFlow{
		TransactionDate: pay1.PaymentDate,
		TransactionType: "IN",
		Description:     "Pembayaran Invoice " + inv1.Number + " - PT Sinar Agung Plastindo",
		Amount:          subtotal1,
		PaymentRefID:    &pay1.ID,
		DateCreated:     pay1.DateCreated,
	}
	db.Create(&cf1)

	// --- INVOICE 2: TERKIRIM / PIUTANG SEBAGIAN (Toko Maju Jaya) ---
	due2 := time.Now().Add(5 * 24 * time.Hour)
	inv2 := models.Invoice{
		Title:        "Pemesanan Cup PP & Sedotan Grosir",
		Number:       "INV/2026/05/002",
		DueDate:      &due2,
		PaymentTerms: "14 days",
		Status:       "SENT",
		Notes:        "Pembayaran DP Rp 5.000.000 telah diterima. Sisa dibayar setelah barang tiba.",
		ClientID:     &clients[1].ID,
		UniqueID:     uuid.New().String()[:8],
		Slug:         "inv-2026-05-002",
		DateCreated:  time.Now().Add(-7 * 24 * time.Hour),
		LastUpdated:  time.Now(),
	}
	db.Create(&inv2)

	// Produk invoice 2
	prod2a := models.Product{
		MasterItemID: &masterProducts[3].ID,
		Title:        masterProducts[3].Title,
		Unit:         masterProducts[3].Unit,
		Price:        masterProducts[3].Price,
		JumlahLusin:  1,
		Quantity:     50, // 50 Karton Cup PP = 6.750.000
		UniqueID:     uuid.New().String()[:8],
		Slug:         "prod-inv2-1",
		InvoiceID:    &inv2.ID,
		DateCreated:  time.Now().Add(-7 * 24 * time.Hour),
		LastUpdated:  time.Now(),
	}
	db.Create(&prod2a)

	prod2b := models.Product{
		MasterItemID: &masterProducts[1].ID,
		Title:        masterProducts[1].Title,
		Unit:         masterProducts[1].Unit,
		Price:        masterProducts[1].Price,
		JumlahLusin:  1,
		Quantity:     150, // 150 Pack Sedotan = 1.350.000
		UniqueID:     uuid.New().String()[:8],
		Slug:         "prod-inv2-2",
		InvoiceID:    &inv2.ID,
		DateCreated:  time.Now().Add(-7 * 24 * time.Hour),
		LastUpdated:  time.Now(),
	}
	db.Create(&prod2b)

	// Total Inv 2 = 8.100.000. Bayar DP 5.000.000
	pay2 := models.InvoicePayment{
		InvoiceID:     inv2.ID,
		PaymentDate:   time.Now().Add(-6 * 24 * time.Hour),
		PaymentAmount: 5000000,
		PaymentMethod: "TRANSFER_BCA",
		DateCreated:   time.Now().Add(-6 * 24 * time.Hour),
	}
	db.Create(&pay2)

	cf2 := models.CashFlow{
		TransactionDate: pay2.PaymentDate,
		TransactionType: "IN",
		Description:     "Pembayaran Uang Muka Invoice " + inv2.Number + " - Toko Maju Jaya",
		Amount:          5000000,
		PaymentRefID:    &pay2.ID,
		DateCreated:     pay2.DateCreated,
	}
	db.Create(&cf2)

	// --- INVOICE 3: BELUM BAYAR / TOTAL PIUTANG (UD Plastik Makmur) ---
	due3 := time.Now().Add(-2 * 24 * time.Hour) // Overdue
	inv3 := models.Invoice{
		Title:        "Supply Kantong HDPE Bening Bulanan",
		Number:       "INV/2026/05/003",
		DueDate:      &due3,
		PaymentTerms: "14 days",
		Status:       "SENT",
		Notes:        "Piutang jatuh tempo. Mohon ditagih secepatnya.",
		ClientID:     &clients[2].ID,
		UniqueID:     uuid.New().String()[:8],
		Slug:         "inv-2026-05-003",
		DateCreated:  time.Now().Add(-16 * 24 * time.Hour),
		LastUpdated:  time.Now(),
	}
	db.Create(&inv3)

	prod3 := models.Product{
		MasterItemID: &masterProducts[0].ID,
		Title:        masterProducts[0].Title,
		Unit:         masterProducts[0].Unit,
		Price:        masterProducts[0].Price,
		JumlahLusin:  1,
		Quantity:     400, // 400 Kg HDPE = 9.400.000
		UniqueID:     uuid.New().String()[:8],
		Slug:         "prod-inv3-1",
		InvoiceID:    &inv3.ID,
		DateCreated:  time.Now().Add(-16 * 24 * time.Hour),
		LastUpdated:  time.Now(),
	}
	db.Create(&prod3)

	// --- INVOICE 4: DRAFT (PT Sinar Agung) ---
	inv4 := models.Invoice{
		Title:        "Draft Estimasi Cup PP Khusus Cetak Sablon",
		Number:       "INV/2026/05/004",
		DueDate:      nil,
		PaymentTerms: "30 days",
		Status:       "DRAFT",
		Notes:        "Masih menunggu approval spek desain sablon dari klien.",
		ClientID:     &clients[0].ID,
		UniqueID:     uuid.New().String()[:8],
		Slug:         "inv-2026-05-004",
		DateCreated:  time.Now(),
		LastUpdated:  time.Now(),
	}
	db.Create(&inv4)

	prod4 := models.Product{
		MasterItemID: &masterProducts[3].ID,
		Title:        masterProducts[3].Title + " (Custom Sablon)",
		Unit:         masterProducts[3].Unit,
		Price:        145000, // Lebih mahal karena sablon
		JumlahLusin:  1,
		Quantity:     100, // 100 Karton = 14.500.000
		UniqueID:     uuid.New().String()[:8],
		Slug:         "prod-inv4-1",
		InvoiceID:    &inv4.ID,
		DateCreated:  time.Now(),
		LastUpdated:  time.Now(),
	}
	db.Create(&prod4)

	// 6. Seed Pengeluaran Kas (CashFlow OUT)
	expenses := []models.CashFlow{
		{
			TransactionDate: time.Now().Add(-20 * 24 * time.Hour),
			TransactionType: "OUT",
			Description:     "Pembayaran Listrik Pabrik PLN (Periode April)",
			Amount:          31200000,
			DateCreated:     time.Now().Add(-20 * 24 * time.Hour),
		},
		{
			TransactionDate: time.Now().Add(-12 * 24 * time.Hour),
			TransactionType: "OUT",
			Description:     "Pembelian Karung Kemasan 50kg (3.000 Lembar)",
			Amount:          6450000,
			DateCreated:     time.Now().Add(-12 * 24 * time.Hour),
		},
		{
			TransactionDate: time.Now().Add(-5 * 24 * time.Hour),
			TransactionType: "OUT",
			Description:     "Pembelian Sparepart Oli & V-Belt Mesin Extruder 2",
			Amount:          1850000,
			DateCreated:     time.Now().Add(-5 * 24 * time.Hour),
		},
	}
	for i := range expenses {
		db.Create(&expenses[i])
	}

	// 7. Seed Pembelian Bahan Baku (Bahan Nono)
	bahanNono := models.BahanNono{
		Tanggal:     time.Now().Add(-18 * 24 * time.Hour),
		Nominal:     17500000,
		Notes:       "Bahan baku biji plastik masuk gudang utama.",
		Tagihan:     "INV-SUPPLIER-POLYTAMA-889",
		TotalHarga:  17500000,
		DateCreated: time.Now().Add(-18 * 24 * time.Hour),
	}
	db.Create(&bahanNono)

	bahanNonoItems := []models.BahanNonoItem{
		{
			BahanNonoID: bahanNono.ID,
			JenisBahan:  "Biji Plastik PP Ori (Grade A)",
			Kuantitas:   500, // 500 kg
			Unit:        "Kg",
			Rate:        22000, // Total = 11.000.000
		},
		{
			BahanNonoID: bahanNono.ID,
			JenisBahan:  "Biji Plastik PP Recycled (Super)",
			Kuantitas:   500,
			Unit:        "Kg",
			Rate:        13000, // Total = 6.500.000
		},
	}
	for i := range bahanNonoItems {
		db.Create(&bahanNonoItems[i])
	}

	// 8. Seed Payroll Karyawan
	payrollHistory := []models.Payroll{
		{
			EmployeeID:      employees[0].ID,
			PaymentDate:     time.Now().Add(-4 * 24 * time.Hour),
			Amount:          2470000, // 26 Hari x 95000
			AttendanceCount: 26,
			DailyRate:       95000,
			Description:     "Gaji Bulanan Operator Extruder 1 - Budi",
		},
		{
			EmployeeID:      employees[1].ID,
			PaymentDate:     time.Now().Add(-4 * 24 * time.Hour),
			Amount:          2000000, // 25 Hari x 80000
			AttendanceCount: 25,
			DailyRate:       80000,
			Description:     "Gaji Bulanan Helper - Joko (1 Hari Absen)",
		},
		{
			EmployeeID:      employees[2].ID,
			PaymentDate:     time.Now().Add(-4 * 24 * time.Hour),
			Amount:          2210000, // 26 Hari x 85000
			AttendanceCount: 26,
			DailyRate:       85000,
			Description:     "Gaji Bulanan Checker - Siti Aminah",
		},
	}
	for i := range payrollHistory {
		db.Create(&payrollHistory[i])

		// Catat pengeluaran kas untuk gaji karyawan
		db.Create(&models.CashFlow{
			TransactionDate: payrollHistory[i].PaymentDate,
			TransactionType: "OUT",
			Description:     payrollHistory[i].Description,
			Amount:          payrollHistory[i].Amount,
			DateCreated:     payrollHistory[i].PaymentDate,
		})
	}

	// 9. Seed Machine Bonus Log
	bonusLogs := []models.MachineBonusLog{
		{
			EmployeeID:      employees[0].ID,
			MachineName:     "Extruder Blow 1",
			ShiftType:       "SIANG",
			BonusAmount:     125000,
			JumlahPerolehan: 850,
			Date:            time.Now().Add(-2 * 24 * time.Hour),
		},
		{
			EmployeeID:      employees[1].ID,
			MachineName:     "Extruder Blow 1",
			ShiftType:       "SIANG",
			BonusAmount:     75000,
			JumlahPerolehan: 850,
			Date:            time.Now().Add(-2 * 24 * time.Hour),
		},
	}
	for i := range bonusLogs {
		db.Create(&bonusLogs[i])
	}

	log.Println("✅ Seeder: Seeding data simulasi manufaktur plastik selesai dilakukan!")
}
