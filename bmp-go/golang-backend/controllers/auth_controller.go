package controllers

import (
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
)

var JwtSecret = []byte("super-secKret-key-change-me")

func Login(c *fiber.Ctx) error {
	type LoginInput struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}

	input := new(LoginInput)
	if err := c.BodyParser(input); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{"success": false, "message": "Invalid input"})
	}

	// Intercept login demouser
	if input.Username == "demouser" && input.Password == "demouser123" {
		claims := jwt.MapClaims{
			"user_id": uint(9999),
			"is_demo": true,
			"exp":     time.Now().Add(time.Hour * 72).Unix(),
		}
		token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
		t, err := token.SignedString(JwtSecret)
		if err != nil {
			return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{"success": false, "message": "Could not login"})
		}
		return c.JSON(fiber.Map{
			"success": true,
			"token":   t,
		})
	}

	var user models.User
	if err := database.DB.Where("username = ?", input.Username).First(&user).Error; err != nil {
		return c.Status(fiber.StatusUnauthorized).JSON(fiber.Map{"success": false, "message": "Invalid credentials"})
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(input.Password)); err != nil {
		return c.Status(fiber.StatusUnauthorized).JSON(fiber.Map{"success": false, "message": "Invalid credentials"})
	}

	// Create JWT token
	claims := jwt.MapClaims{
		"user_id": user.ID,
		"exp":     time.Now().Add(time.Hour * 72).Unix(),
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	t, err := token.SignedString(JwtSecret)
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{"success": false, "message": "Could not login"})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"token":   t,
	})
}

// CreateAdminUser creates default production admin users and seeds demo data.
func CreateAdminUser() {
	var adminCount int64
	database.DB.Model(&models.User{}).Where("username = ?", "admin").Count(&adminCount)
	if adminCount == 0 {
		hashedPassword, _ := bcrypt.GenerateFromPassword([]byte("admin123"), 10)
		admin := models.User{
			Username: "admin",
			Password: string(hashedPassword),
		}
		database.DB.Create(&admin)
	}

	var dediCount int64
	database.DB.Model(&models.User{}).Where("username = ?", "dedi").Count(&dediCount)
	if dediCount == 0 {
		hashedPassword, _ := bcrypt.GenerateFromPassword([]byte("Bahtera1!"), 10)
		dedi := models.User{
			Username: "dedi",
			Password: string(hashedPassword),
		}
		database.DB.Create(&dedi)
	}

	var muizzCount int64
	database.DB.Model(&models.User{}).Where("username = ?", "muizz").Count(&muizzCount)
	if muizzCount == 0 {
		hashedPassword, _ := bcrypt.GenerateFromPassword([]byte("Muizz9001!"), 10)
		muizz := models.User{
			Username: "muizz",
			Password: string(hashedPassword),
		}
		database.DB.Create(&muizz)
	}

	// Seed isolated demo data
	seedDemoClients()
}

// seedDemoClients creates sample demo clients + invoices if none exist yet.
// All demo data has is_demo=true and will ONLY be visible to demouser sessions.
func seedDemoClients() {
	var demoCount int64
	database.DB.Model(&models.Client{}).Where("is_demo = ?", true).Count(&demoCount)
	if demoCount > 0 {
		return // sudah ada data demo, lewati
	}

	// --- 1 Demo Settings ---
	demoSettings := models.Settings{
		ClientName:         "BMP - Bintang Makmur Plastindo (DEMO)",
		ClientLogo:         "",
		AddressLine1:       "Jl. Industri Demo No. 45, Kawasan Industri Candi",
		Province:           "Jawa Tengah",
		PostalCode:         "50181",
		PhoneNumber:        "024-7654321",
		EmailAddress:       "demo-info@bintangmakmurplastindo.com",
		TaxNumber:          "01.234.567.8-901.000",
		ListrikBulanan:     32500000,
		JumlahMesin:        6,
		JumlahKaryawan:     15,
		GajiHarian:         85000,
		HariKerjaSebulan:   26,
		BiayaKarungPer1000: 2150000,
		UniqueID:           uuid.New().String()[:8],
		Slug:               "bmp-bintang-makmur-plastindo-demo",
		IsDemo:             true,
		DateCreated:        time.Now(),
		LastUpdated:        time.Now(),
	}
	database.DB.Create(&demoSettings)

	// --- 3 Demo Master Products ---
	demoMPs := []models.MasterProduct{
		{
			Title:       "[DEMO] Kantong HDPE Bening 30x50 (Tebal 0.03)",
			Description: "Kantong plastik high-density polyethylene bening kualitas premium (Demo).",
			Unit:        "Kg",
			Price:       23500,
			BeratGram:   14.5,
			CycleTime:   3.2,
			IsDemo:      true,
			UniqueID:     uuid.New().String()[:8],
			Slug:         "demo-kantong-hdpe-bening-30x50",
			DateCreated:  time.Now(),
			LastUpdated:  time.Now(),
		},
		{
			Title:       "[DEMO] Sedotan Hitam Steril 6mm (Isi 500)",
			Description: "Sedotan plastik steril dibungkus kertas per pcs (Demo).",
			Unit:        "Pack",
			Price:       9000,
			BeratGram:   1.2,
			CycleTime:   0.6,
			IsDemo:      true,
			UniqueID:     uuid.New().String()[:8],
			Slug:         "demo-sedotan-hitam-steril-6mm",
			DateCreated:  time.Now(),
			LastUpdated:  time.Now(),
		},
		{
			Title:       "[DEMO] Cup Plastik 16oz PP Tebal 7gr",
			Description: "Gelas cup plastik bahan Polypropylene (Demo).",
			Unit:        "Karton",
			Price:       135000,
			BeratGram:   7.0,
			CycleTime:   1.4,
			IsDemo:      true,
			UniqueID:     uuid.New().String()[:8],
			Slug:         "demo-cup-plastik-16oz-pp-tebal",
			DateCreated:  time.Now(),
			LastUpdated:  time.Now(),
		},
	}
	for i := range demoMPs {
		database.DB.Create(&demoMPs[i])
	}

	// --- 3 Demo Employees ---
	demoEmployees := []models.Employee{
		{
			Name:           "[DEMO] Budi Santoso",
			Position:       "Operator Extruder 1",
			SalaryAmount:   95000,
			FingerprintPIN: "9001",
			IsActive:       true,
			IsDemo:         true,
		},
		{
			Name:           "[DEMO] Joko Susilo",
			Position:       "Helper Gulung & Potong",
			SalaryAmount:   80000,
			FingerprintPIN: "9002",
			IsActive:       true,
			IsDemo:         true,
		},
		{
			Name:           "[DEMO] Siti Aminah",
			Position:       "Checker & Packing",
			SalaryAmount:   85000,
			FingerprintPIN: "9003",
			IsActive:       true,
			IsDemo:         true,
		},
	}
	for i := range demoEmployees {
		database.DB.Create(&demoEmployees[i])
	}

	// --- 3 Demo Clients ---
	demoClients := []models.Client{
		{
			ClientName:   "[DEMO] PT Plastik Nusantara",
			AddressLine1: "Jl. Industri Demo No. 1, Tangerang",
			Province:     "Banten",
			PostalCode:   "15000",
			PhoneNumber:  "021-0000001",
			EmailAddress: "demo1@example.com",
			IsDemo:       true,
			UniqueID:     uuid.New().String()[:8],
			Slug:         "demo-pt-plastik-nusantara-" + uuid.New().String()[:4],
			DateCreated:  time.Now().Add(-30 * 24 * time.Hour),
			LastUpdated:  time.Now(),
		},
		{
			ClientName:   "[DEMO] Toko Plastik Sejahtera",
			AddressLine1: "Jl. Pasar Demo No. 22, Bandung",
			Province:     "Jawa Barat",
			PostalCode:   "40001",
			PhoneNumber:  "022-0000002",
			EmailAddress: "demo2@example.com",
			IsDemo:       true,
			UniqueID:     uuid.New().String()[:8],
			Slug:         "demo-toko-plastik-sejahtera-" + uuid.New().String()[:4],
			DateCreated:  time.Now().Add(-20 * 24 * time.Hour),
			LastUpdated:  time.Now(),
		},
		{
			ClientName:   "[DEMO] UD Karya Plastik Maju",
			AddressLine1: "Jl. Raya Demo No. 99, Surabaya",
			Province:     "Jawa Timur",
			PostalCode:   "60001",
			PhoneNumber:  "031-0000003",
			EmailAddress: "demo3@example.com",
			IsDemo:       true,
			UniqueID:     uuid.New().String()[:8],
			Slug:         "demo-ud-karya-plastik-maju-" + uuid.New().String()[:4],
			DateCreated:  time.Now().Add(-10 * 24 * time.Hour),
			LastUpdated:  time.Now(),
		},
	}
	for i := range demoClients {
		database.DB.Create(&demoClients[i])
	}

	// --- Demo Invoices ---
	due1 := time.Now().Add(7 * 24 * time.Hour)
	uniqueID1 := uuid.New().String()[:8]
	demoInv1 := models.Invoice{
		Title:        "Demo - Pembelian Kantong Plastik HDPE",
		Number:       "DEMO-INV-001",
		DueDate:      &due1,
		PaymentTerms: "14 days",
		Status:       "UNPAID",
		Notes:        "Ini adalah faktur contoh untuk akun demo. Data ini tidak mempengaruhi data produksi.",
		ClientID:     &demoClients[0].ID,
		UniqueID:     uniqueID1,
		Slug:         "DEMO-INV-001-" + uniqueID1,
		IsDemo:       true,
		DateCreated:  time.Now().Add(-5 * 24 * time.Hour),
		LastUpdated:  time.Now(),
	}
	database.DB.Create(&demoInv1)

	demoProd1 := models.Product{
		MasterItemID: &demoMPs[0].ID,
		Title:        demoMPs[0].Title,
		Unit:         demoMPs[0].Unit,
		Price:        demoMPs[0].Price,
		JumlahLusin:  1,
		Quantity:     50,
		IsKhusus:     false,
		InvoiceID:    &demoInv1.ID,
		UniqueID:     uuid.New().String()[:8],
		Slug:         demoInv1.Slug + "-p1",
		DateCreated:  time.Now(),
		LastUpdated:  time.Now(),
	}
	database.DB.Create(&demoProd1)

	// Second demo invoice - PAID
	due2 := time.Now().Add(-5 * 24 * time.Hour)
	uniqueID2 := uuid.New().String()[:8]
	demoInv2 := models.Invoice{
		Title:        "Demo - Pengiriman Cup PP Reguler",
		Number:       "DEMO-INV-002",
		DueDate:      &due2,
		PaymentTerms: "COD",
		Status:       "PAID",
		Notes:        "Faktur demo sudah lunas.",
		ClientID:     &demoClients[1].ID,
		UniqueID:     uniqueID2,
		Slug:         "DEMO-INV-002-" + uniqueID2,
		IsDemo:       true,
		DateCreated:  time.Now().Add(-15 * 24 * time.Hour),
		LastUpdated:  time.Now(),
	}
	database.DB.Create(&demoInv2)

	demoProd2 := models.Product{
		MasterItemID: &demoMPs[2].ID,
		Title:        demoMPs[2].Title,
		Unit:         demoMPs[2].Unit,
		Price:        demoMPs[2].Price,
		JumlahLusin:  1,
		Quantity:     100,
		IsKhusus:     false,
		InvoiceID:    &demoInv2.ID,
		UniqueID:     uuid.New().String()[:8],
		Slug:         demoInv2.Slug + "-p1",
		DateCreated:  time.Now(),
		LastUpdated:  time.Now(),
	}
	database.DB.Create(&demoProd2)

	// Payment for demo invoice 2
	demoPayment := models.InvoicePayment{
		InvoiceID:     demoInv2.ID,
		PaymentDate:   time.Now().Add(-14 * 24 * time.Hour),
		PaymentAmount: int(demoMPs[2].Price * 100),
		PaymentMethod: "TRANSFER",
		DateCreated:   time.Now().Add(-14 * 24 * time.Hour),
	}
	database.DB.Create(&demoPayment)

	// Cashflow for demo payment
	demoCF := models.CashFlow{
		TransactionDate: demoPayment.PaymentDate,
		TransactionType: "MASUK",
		Description:     "[DEMO] Pembayaran Faktur " + demoInv2.Number + " (" + demoClients[1].ClientName + ")",
		Amount:          float64(demoPayment.PaymentAmount),
		PaymentRefID:    &demoPayment.ID,
		IsDemo:          true,
		DateCreated:     demoPayment.DateCreated,
	}
	database.DB.Create(&demoCF)
}
