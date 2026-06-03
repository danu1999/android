package controllers

import (
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"strconv"
	"strings"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/google/uuid"
)

// InvoiceWithTotal is the enriched invoice response with computed totals.
type InvoiceWithTotal struct {
	models.Invoice
	Total      float64 `json:"Total"`
	PaidAmount float64 `json:"PaidAmount"`
}

// productTotalRow holds raw aggregated product totals per invoice from a single SQL query.
type productTotalRow struct {
	InvoiceID  uint
	TotalBill  float64
}

// paymentTotalRow holds raw aggregated payment totals per invoice from a single SQL query.
type paymentTotalRow struct {
	InvoiceID   uint
	TotalPaid   float64
}

// GetInvoices retrieves paginated invoices with totals computed via bulk SQL (no N+1).
// Query params: page (default 1), limit (default 20), search, status, client_id.
func GetInvoices(c *fiber.Ctx) error {
	// --- Parse pagination params ---
	page, _ := strconv.Atoi(c.Query("page", "1"))
	limit, _ := strconv.Atoi(c.Query("limit", "20"))
	if page < 1 {
		page = 1
	}
	if limit < 1 || limit > 200 {
		limit = 20
	}
	offset := (page - 1) * limit

	// --- Parse filters ---
	search := c.Query("search", "")
	status := c.Query("status", "")
	clientIDStr := c.Query("client_id", "")

	// --- Build query ---
	isDemo := IsDemoUser(c)
	query := database.DB.Model(&models.Invoice{}).
		Joins("LEFT JOIN clients ON clients.id = invoices.client_id").
		Where("invoices.is_demo = ?", isDemo)

	if status != "" && status != "ALL" {
		query = query.Where("invoices.status = ?", status)
	}

	if clientIDStr != "" && clientIDStr != "ALL" {
		clientID, err := strconv.Atoi(clientIDStr)
		if err == nil {
			query = query.Where("invoices.client_id = ?", clientID)
		}
	}

	if search != "" {
		query = query.
			Where("LOWER(invoices.number) LIKE ? OR LOWER(clients.client_name) LIKE ?", "%"+strings.ToLower(search)+"%", "%"+strings.ToLower(search)+"%")
	}

	// --- Count total rows for pagination metadata ---
	var totalCount int64
	query.Count(&totalCount)
	totalPages := int((totalCount + int64(limit) - 1) / int64(limit))
	if totalPages < 1 {
		totalPages = 1
	}

	// --- Fetch paginated invoices with Client preloaded (single JOIN query) ---
	var invoices []models.Invoice
	query.
		Preload("Client").
		Order("invoices.date_created desc, invoices.id desc").
		Limit(limit).
		Offset(offset).
		Find(&invoices)

	if len(invoices) == 0 {
		return c.JSON(fiber.Map{
			"success":      true,
			"data":         []interface{}{},
			"current_page": page,
			"total_pages":  totalPages,
			"total_count":  totalCount,
		})
	}

	// --- Collect invoice IDs from this page ---
	invoiceIDs := make([]uint, len(invoices))
	for i, inv := range invoices {
		invoiceIDs[i] = inv.ID
	}

	// --- Bulk aggregate product totals (1 SQL, no loop) ---
	var productTotals []productTotalRow
	database.DB.Raw(`
		SELECT invoice_id,
		       COALESCE(SUM(quantity * jumlah_lusin * price), 0) AS total_bill
		FROM products
		WHERE invoice_id IN (?) AND deleted_at IS NULL
		GROUP BY invoice_id
	`, invoiceIDs).Scan(&productTotals)

	// --- Bulk aggregate payment totals (1 SQL, no loop) ---
	var paymentTotals []paymentTotalRow
	database.DB.Raw(`
		SELECT invoice_id,
		       COALESCE(SUM(payment_amount), 0) AS total_paid
		FROM invoice_payments
		WHERE invoice_id IN (?) AND deleted_at IS NULL
		GROUP BY invoice_id
	`, invoiceIDs).Scan(&paymentTotals)

	// --- Build lookup maps for O(1) access ---
	billMap := make(map[uint]float64, len(productTotals))
	for _, row := range productTotals {
		billMap[row.InvoiceID] = row.TotalBill
	}
	paidMap := make(map[uint]float64, len(paymentTotals))
	for _, row := range paymentTotals {
		paidMap[row.InvoiceID] = row.TotalPaid
	}

	// --- Assemble final result (zero additional DB queries) ---
	result := make([]InvoiceWithTotal, 0, len(invoices))
	for _, inv := range invoices {
		result = append(result, InvoiceWithTotal{
			Invoice:    inv,
			Total:      billMap[inv.ID],
			PaidAmount: paidMap[inv.ID],
		})
	}

	return c.JSON(fiber.Map{
		"success":      true,
		"data":         result,
		"current_page": page,
		"total_pages":  totalPages,
		"total_count":  totalCount,
	})
}

// Get invoice by ID
func GetInvoice(c *fiber.Ctx) error {
	id := c.Params("id")
	var invoice models.Invoice

	if err := database.DB.Preload("Client").First(&invoice, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{
			"success": false,
			"message": "Invoice not found",
		})
	}

	// Isolasi demo: cegah cross-access antara demo dan production
	if invoice.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{
			"success": false,
			"message": "Akses ditolak.",
		})
	}

	var products []models.Product
	database.DB.Preload("MasterItem").Where("invoice_id = ?", invoice.ID).Find(&products)

	var payments []models.InvoicePayment
	database.DB.Where("invoice_id = ?", invoice.ID).Find(&payments)

	return c.JSON(fiber.Map{
		"success":  true,
		"data":     invoice,
		"products": products,
		"payments": payments,
	})
}

// Create an invoice with products
func CreateInvoice(c *fiber.Ctx) error {
	type ProductInput struct {
		MasterItemID uint    `json:"master_item_id"`
		Quantity     float64 `json:"quantity"`
		JumlahLusin  float64 `json:"jumlah_lusin"`
		CustomPrice  float64 `json:"custom_price"`
		IsKhusus     bool    `json:"is_khusus"`
		HargaBeli    float64 `json:"harga_beli"`
	}

	type InvoiceInput struct {
		ClientID     uint           `json:"client_id"`
		Number       string         `json:"number"`
		Title        string         `json:"title"`
		DueDate      string         `json:"due_date"`
		DateCreated  string         `json:"date_created"`
		PaymentTerms string         `json:"payment_terms"`
		Notes        string         `json:"notes"`
		Products     []ProductInput `json:"products"`
	}

	input := new(InvoiceInput)
	if err := c.BodyParser(input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input data"})
	}

	// Verifikasi Client ID memiliki demo status yang sama dengan user session
	var client models.Client
	if err := database.DB.First(&client, input.ClientID).Error; err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Client not found"})
	}
	if client.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	// Parse DueDate more robustly
	var parsedDueDate *time.Time
	if input.DueDate != "" {
		pd, err := time.Parse(time.RFC3339, input.DueDate)
		if err != nil {
			pd, err = time.Parse("2006-01-02", input.DueDate)
			if err != nil {
				pd, err = time.Parse("02/01/2006", input.DueDate)
			}
		}
		if err == nil {
			parsedDueDate = &pd
		}
	}

	// Parse DateCreated
	createdAt := time.Now()
	if input.DateCreated != "" {
		pc, err := time.Parse(time.RFC3339, input.DateCreated)
		if err != nil {
			pc, err = time.Parse("2006-01-02", input.DateCreated)
		}
		if err == nil {
			createdAt = pc
		}
	}

	// CEK DUPLIKAT NOMOR FAKTUR
	var existing models.Invoice
	if err := database.DB.Where("number = ?", input.Number).First(&existing).Error; err == nil {
		return c.Status(400).JSON(fiber.Map{
			"success": false,
			"message": "Gagal! Nomor faktur " + input.Number + " sudah ada. Silakan Refresh halaman atau ubah manual nomor fakturnya.",
		})
	}

	uniqueID := uuid.New().String()[:8]
	slug := input.Number + "-" + uniqueID

	invoice := models.Invoice{
		ClientID:     &input.ClientID,
		Number:       input.Number,
		Title:        input.Title,
		DueDate:      parsedDueDate,
		PaymentTerms: input.PaymentTerms,
		Notes:        input.Notes,
		Status:       "UNPAID",
		IsDemo:       IsDemoUser(c),
		DateCreated:  createdAt,
		LastUpdated:  time.Now(),
		UniqueID:     uniqueID,
		Slug:         slug,
	}

	tx := database.DB.Begin()

	if err := tx.Create(&invoice).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to create invoice"})
	}

	for _, pInput := range input.Products {
		var master models.MasterProduct
		if err := tx.First(&master, pInput.MasterItemID).Error; err != nil {
			continue
		}

		price := master.Price
		if pInput.CustomPrice > 0 {
			price = pInput.CustomPrice
		}

		lusin := pInput.JumlahLusin
		if lusin <= 0 {
			lusin = 1
		}

		hargaBeli := 0.0
		if pInput.IsKhusus {
			hargaBeli = pInput.HargaBeli
		}

		product := models.Product{
			InvoiceID:    &invoice.ID,
			MasterItemID: &master.ID,
			Title:        master.Title,
			Unit:         master.Unit,
			Price:        price,
			JumlahLusin:  lusin,
			Quantity:     pInput.Quantity,
			IsKhusus:     pInput.IsKhusus,
			HargaBeli:    hargaBeli,
			DateCreated:  time.Now(),
			LastUpdated:  time.Now(),
			UniqueID:     uuid.New().String()[:8],
			Slug:         slug + "-" + uuid.New().String()[:8],
		}
		if err := tx.Create(&product).Error; err != nil {
			tx.Rollback()
			return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to create product items"})
		}

		// Jika barang khusus, catat Kas Keluar otomatis
		if pInput.IsKhusus && hargaBeli > 0 {
			cashFlow := models.CashFlow{
				TransactionDate: time.Now(),
				TransactionType: "KELUAR",
				Description:     "Pembelian barang khusus untuk Faktur " + invoice.Number,
				Amount:          hargaBeli,
				IsDemo:          IsDemoUser(c),
				DateCreated:     time.Now(),
			}
			if err := tx.Create(&cashFlow).Error; err != nil {
				tx.Rollback()
				return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to create cashflow for special product"})
			}
		}
	}

	if err := tx.Commit().Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan faktur"})
	}

	return c.Status(201).JSON(fiber.Map{
		"success": true,
		"message": "Invoice successfully created",
		"data":    invoice,
	})
}

// PayMassal handles Borongan Payments for clients
func PayMassal(c *fiber.Ctx) error {
	type PaymentInput struct {
		ClientID uint    `json:"client_id"`
		Nominal  float64 `json:"nominal"`
		Metode   string  `json:"metode"`
	}

	input := new(PaymentInput)
	if err := c.BodyParser(input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input data"})
	}

	var client models.Client
	if err := database.DB.First(&client, input.ClientID).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Client not found"})
	}

	if client.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	var invoices []models.Invoice
	database.DB.Where("client_id = ? AND status IN ? AND is_demo = ?", client.ID, []string{"UNPAID", "PARTIAL", "OVERDUE"}, IsDemoUser(c)).
		Order("date_created asc").Find(&invoices)

	sisaUang := input.Nominal
	tanggalBayar := time.Now()
	metode := input.Metode
	if metode == "" {
		metode = "TRANSFER"
	}

	tx := database.DB.Begin()

	for _, inv := range invoices {
		if sisaUang <= 0 {
			break
		}

		// Calculate Total Tagihan
		var products []models.Product
		tx.Where("invoice_id = ?", inv.ID).Find(&products)
		total := 0.0
		for _, p := range products {
			total += p.Quantity * p.JumlahLusin * p.Price
		}

		// Calculate Total Paid
		var payments []models.InvoicePayment
		tx.Where("invoice_id = ?", inv.ID).Find(&payments)
		paid := 0.0
		for _, p := range payments {
			paid += float64(p.PaymentAmount)
		}

		sisaTagihan := total - paid
		if sisaTagihan <= 0 {
			continue
		}

		bayarIni := sisaTagihan
		if sisaUang < sisaTagihan {
			bayarIni = sisaUang
		}

		// Create Payment History
		payment := models.InvoicePayment{
			InvoiceID:     inv.ID,
			PaymentDate:   tanggalBayar,
			PaymentMethod: "Borongan " + metode,
			PaymentAmount: int(bayarIni),
			DateCreated:   tanggalBayar,
		}
		tx.Create(&payment)

		// Create CashFlow Entry
		cashFlow := models.CashFlow{
			TransactionDate: tanggalBayar,
			TransactionType: "MASUK",
			Description:     "Pembayaran Borongan Faktur " + inv.Number + " (" + client.ClientName + ")",
			Amount:          bayarIni,
			PaymentRefID:    &payment.ID,
			IsDemo:          IsDemoUser(c),
			DateCreated:     tanggalBayar,
		}
		tx.Create(&cashFlow)

		sisaUang -= bayarIni

		if bayarIni >= sisaTagihan {
			inv.Status = "PAID"
		} else {
			inv.Status = "PARTIAL"
		}
		tx.Save(&inv)
	}

	// Sisa uang masuk deposit klien (Saldo Titipan)
	if sisaUang > 0 {
		client.SaldoTitipan += sisaUang
		tx.Save(&client)
	}

	if err := tx.Commit().Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan pembayaran borongan"})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"message": "Pembayaran massal berhasil diproses dan dibagikan ke faktur",
	})
}

// SyncOverdueInvoices updates invoice statuses based on due date, scoped to demo/production.
func SyncOverdueInvoices(c *fiber.Ctx) error {
	isDemo := IsDemoUser(c)
	today := time.Now().Truncate(24 * time.Hour)

	database.DB.Model(&models.Invoice{}).
		Where("status = ? AND due_date IS NOT NULL AND due_date < ? AND is_demo = ?", "UNPAID", today, isDemo).
		Update("status", "OVERDUE")

	database.DB.Model(&models.Invoice{}).
		Where("status = ? AND due_date IS NOT NULL AND due_date >= ? AND is_demo = ?", "OVERDUE", today, isDemo).
		Update("status", "UNPAID")

	return c.JSON(fiber.Map{"success": true, "message": "Invoices synced"})
}

// PaySingleInvoice handles partial or full payment for a single invoice
func PaySingleInvoice(c *fiber.Ctx) error {
	id := c.Params("id")

	type PayInput struct {
		Tanggal string  `json:"tanggal"`
		Metode  string  `json:"metode"`
		Nominal float64 `json:"nominal"`
	}

	input := new(PayInput)
	if err := c.BodyParser(input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input"})
	}

	if input.Nominal <= 0 {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Nominal must be > 0"})
	}

	// Parse Tanggal more robustly
	parsedTanggal, err := time.Parse(time.RFC3339, input.Tanggal)
	if err != nil {
		parsedTanggal, err = time.Parse("2006-01-02", input.Tanggal)
		if err != nil {
			parsedTanggal, err = time.Parse("02/01/2006", input.Tanggal)
			if err != nil {
				parsedTanggal = time.Now()
			}
		}
	}

	var inv models.Invoice
	if err := database.DB.Preload("Client").First(&inv, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Invoice not found"})
	}

	if inv.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	tx := database.DB.Begin()

	payment := models.InvoicePayment{
		InvoiceID:     inv.ID,
		PaymentDate:   parsedTanggal,
		PaymentMethod: input.Metode,
		PaymentAmount: int(input.Nominal),
		DateCreated:   time.Now(),
	}
	tx.Create(&payment)

	clientName := "Tanpa Nama"
	if inv.Client.ID != 0 {
		clientName = inv.Client.ClientName
	}

	cashFlow := models.CashFlow{
		TransactionDate: parsedTanggal,
		TransactionType: "MASUK",
		Description:     "Pembayaran Faktur " + inv.Number + " (" + clientName + ")",
		Amount:          input.Nominal,
		PaymentRefID:    &payment.ID,
		IsDemo:          IsDemoUser(c),
		DateCreated:     time.Now(),
	}
	tx.Create(&cashFlow)

	var products []models.Product
	tx.Where("invoice_id = ?", inv.ID).Find(&products)
	total := 0.0
	for _, p := range products {
		total += p.Quantity * p.JumlahLusin * p.Price
	}

	var payments []models.InvoicePayment
	tx.Where("invoice_id = ?", inv.ID).Find(&payments)
	paid := 0.0
	for _, p := range payments {
		paid += float64(p.PaymentAmount)
	}

	sisa := total - paid
	if sisa <= 0 {
		inv.Status = "PAID"
	} else {
		inv.Status = "PARTIAL"
	}
	tx.Save(&inv)
	if err := tx.Commit().Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan pembayaran faktur"})
	}

	return c.JSON(fiber.Map{"success": true, "message": "Pembayaran berhasil dicatat", "status": inv.Status})
}

// Delete an invoice
func DeleteInvoice(c *fiber.Ctx) error {
	id := c.Params("id")
	var invoice models.Invoice

	if err := database.DB.First(&invoice, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Invoice not found"})
	}

	if invoice.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	tx := database.DB.Begin()

	// 1. HAPUS KAS KELUAR (Barang Khusus): Cari dari deskripsinya
	deskripsiBarangKhusus := "%Faktur " + invoice.Number + "%"
	tx.Where("description LIKE ? AND transaction_type = ?", deskripsiBarangKhusus, "KELUAR").Delete(&models.CashFlow{})

	// 2. HAPUS KAS MASUK (Pembayaran): Cari ID Pembayaran yang nyangkut di faktur ini
	var payments []models.InvoicePayment
	tx.Where("invoice_id = ?", invoice.ID).Find(&payments)
	var paymentIDs []uint
	for _, p := range payments {
		paymentIDs = append(paymentIDs, p.ID)
	}

	// Jika ada riwayat pembayaran, hapus juga catatannya di Kas
	if len(paymentIDs) > 0 {
		if err := tx.Where("payment_ref_id IN ?", paymentIDs).Delete(&models.CashFlow{}).Error; err != nil {
			tx.Rollback()
			return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to delete related cashflow"})
		}
	}

	// 3. Hapus Produk (Barang di Faktur)
	if err := tx.Where("invoice_id = ?", invoice.ID).Delete(&models.Product{}).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to delete related products"})
	}

	// 4. Hapus Riwayat Pembayaran (InvoicePayment)
	if err := tx.Where("invoice_id = ?", invoice.ID).Delete(&models.InvoicePayment{}).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to delete related payments"})
	}

	// 5. Hapus Fakturnya Sendiri
	if err := tx.Delete(&invoice).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to delete invoice"})
	}

	if err := tx.Commit().Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menghapus faktur"})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"message": "Invoice, Payments, and CashFlows successfully deleted",
	})
}

// Update invoice products
func UpdateInvoiceProducts(c *fiber.Ctx) error {
	id := c.Params("id")
	var invoice models.Invoice

	if err := database.DB.First(&invoice, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Invoice not found"})
	}

	if invoice.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	type ProductInput struct {
		MasterItemID uint    `json:"master_item_id"`
		Quantity     float64 `json:"quantity"`
		JumlahLusin  float64 `json:"jumlah_lusin"`
		CustomPrice  float64 `json:"custom_price"`
		IsKhusus     bool    `json:"is_khusus"`
		HargaBeli    float64 `json:"harga_beli"`
	}

	type UpdateInput struct {
		Products []ProductInput `json:"products"`
	}

	input := new(UpdateInput)
	if err := c.BodyParser(input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input data"})
	}

	tx := database.DB.Begin()

	// Clear old cashflow records for special products of this invoice
	deskripsiBarangKhusus := "%Faktur " + invoice.Number + "%"
	if err := tx.Where("description LIKE ? AND transaction_type = ?", deskripsiBarangKhusus, "KELUAR").Delete(&models.CashFlow{}).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to clear old special product cashflows"})
	}

	// Clear old products
	if err := tx.Where("invoice_id = ?", invoice.ID).Delete(&models.Product{}).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to clear old products"})
	}

	// Insert new products
	for _, pInput := range input.Products {
		var master models.MasterProduct
		if err := tx.First(&master, pInput.MasterItemID).Error; err != nil {
			continue
		}

		price := master.Price
		if pInput.CustomPrice > 0 {
			price = pInput.CustomPrice
		}

		lusin := pInput.JumlahLusin
		if lusin <= 0 {
			lusin = 1
		}

		hargaBeli := 0.0
		if pInput.IsKhusus {
			hargaBeli = pInput.HargaBeli
		}

		product := models.Product{
			InvoiceID:    &invoice.ID,
			MasterItemID: &master.ID,
			Title:        master.Title,
			Unit:         master.Unit,
			Price:        price,
			JumlahLusin:  lusin,
			Quantity:     pInput.Quantity,
			IsKhusus:     pInput.IsKhusus,
			HargaBeli:    hargaBeli,
			DateCreated:  time.Now(),
			LastUpdated:  time.Now(),
			UniqueID:     uuid.New().String()[:8],
			Slug:         invoice.Slug + "-" + uuid.New().String()[:8],
		}
		if err := tx.Create(&product).Error; err != nil {
			tx.Rollback()
			return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to save new product"})
		}

		// Jika barang khusus, catat Kas Keluar otomatis
		if pInput.IsKhusus && hargaBeli > 0 {
			cashFlow := models.CashFlow{
				TransactionDate: time.Now(),
				TransactionType: "KELUAR",
				Description:     "Pembelian barang khusus (Update) untuk Faktur " + invoice.Number,
				Amount:          hargaBeli,
				IsDemo:          IsDemoUser(c),
				DateCreated:     time.Now(),
			}
			if err := tx.Create(&cashFlow).Error; err != nil {
				tx.Rollback()
				return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to create cashflow for special product"})
			}
		}
	}

	// Recalculate invoice status (total vs paid)
	var products []models.Product
	tx.Where("invoice_id = ?", invoice.ID).Find(&products)
	total := 0.0
	for _, p := range products {
		total += p.Quantity * p.JumlahLusin * p.Price
	}

	var payments []models.InvoicePayment
	tx.Where("invoice_id = ?", invoice.ID).Find(&payments)
	paid := 0.0
	for _, p := range payments {
		paid += float64(p.PaymentAmount)
	}

	sisa := total - paid
	if sisa <= 0 {
		invoice.Status = "PAID"
	} else if paid > 0 {
		invoice.Status = "PARTIAL"
	} else {
		invoice.Status = "UNPAID"
	}
	tx.Save(&invoice)

	if err := tx.Commit().Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan update produk faktur"})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"message": "Invoice products successfully updated",
	})
}

// EditPayment updates nominal, tanggal, and metode of a single InvoicePayment.
// Cascades to the linked CashFlow entry and recalculates Invoice status.
func EditPayment(c *fiber.Ctx) error {
	paymentID := c.Params("paymentId")

	type EditInput struct {
		Nominal float64 `json:"nominal"`
		Tanggal string  `json:"tanggal"`
		Metode  string  `json:"metode"`
	}

	input := new(EditInput)
	if err := c.BodyParser(input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input"})
	}
	if input.Nominal <= 0 {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Nominal must be > 0"})
	}

	var payment models.InvoicePayment
	if err := database.DB.First(&payment, paymentID).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Payment not found"})
	}

	var inv models.Invoice
	if err := database.DB.First(&inv, payment.InvoiceID).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Invoice not found"})
	}
	if inv.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	// Parse tanggal
	parsedTanggal := payment.PaymentDate
	if input.Tanggal != "" {
		if t, err := time.Parse(time.RFC3339, input.Tanggal); err == nil {
			parsedTanggal = t
		} else if t, err := time.Parse("2006-01-02", input.Tanggal); err == nil {
			parsedTanggal = t
		}
	}

	tx := database.DB.Begin()

	// Update payment record
	payment.PaymentAmount = int(input.Nominal)
	payment.PaymentDate = parsedTanggal
	if input.Metode != "" {
		payment.PaymentMethod = input.Metode
	}
	if err := tx.Save(&payment).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal update pembayaran"})
	}

	// Cascade to linked CashFlow
	tx.Model(&models.CashFlow{}).
		Where("payment_ref_id = ?", payment.ID).
		Updates(map[string]interface{}{
			"amount":           float64(payment.PaymentAmount),
			"transaction_date": parsedTanggal,
		})

	// Recalculate invoice status
	var products []models.Product
	tx.Where("invoice_id = ? AND deleted_at IS NULL", inv.ID).Find(&products)
	total := 0.0
	for _, p := range products {
		total += p.Quantity * p.JumlahLusin * p.Price
	}

	var payments []models.InvoicePayment
	tx.Where("invoice_id = ? AND deleted_at IS NULL", inv.ID).Find(&payments)
	paid := 0.0
	for _, p := range payments {
		paid += float64(p.PaymentAmount)
	}

	sisa := total - paid
	if sisa <= 0 {
		inv.Status = "PAID"
	} else if paid > 0 {
		inv.Status = "PARTIAL"
	} else {
		inv.Status = "UNPAID"
	}
	tx.Save(&inv)

	if err := tx.Commit().Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan perubahan"})
	}

	return c.JSON(fiber.Map{"success": true, "message": "Pembayaran berhasil diperbarui", "status": inv.Status})
}

// DeletePayment removes a single InvoicePayment and its linked CashFlow,
// then recalculates Invoice status.
func DeletePayment(c *fiber.Ctx) error {
	paymentID := c.Params("paymentId")

	var payment models.InvoicePayment
	if err := database.DB.First(&payment, paymentID).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Payment not found"})
	}

	var inv models.Invoice
	if err := database.DB.First(&inv, payment.InvoiceID).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Invoice not found"})
	}
	if inv.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	tx := database.DB.Begin()

	// Delete linked CashFlow
	if err := tx.Where("payment_ref_id = ?", payment.ID).Delete(&models.CashFlow{}).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal hapus cashflow terkait"})
	}

	// Delete payment
	if err := tx.Delete(&payment).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal hapus pembayaran"})
	}

	// Recalculate invoice status
	var products []models.Product
	tx.Where("invoice_id = ? AND deleted_at IS NULL", inv.ID).Find(&products)
	total := 0.0
	for _, p := range products {
		total += p.Quantity * p.JumlahLusin * p.Price
	}

	var payments []models.InvoicePayment
	tx.Where("invoice_id = ? AND deleted_at IS NULL", inv.ID).Find(&payments)
	paid := 0.0
	for _, p := range payments {
		paid += float64(p.PaymentAmount)
	}

	sisa := total - paid
	if sisa <= 0 {
		inv.Status = "PAID"
	} else if paid > 0 {
		inv.Status = "PARTIAL"
	} else {
		inv.Status = "UNPAID"
	}
	tx.Save(&inv)

	if err := tx.Commit().Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menghapus pembayaran"})
	}

	return c.JSON(fiber.Map{"success": true, "message": "Pembayaran berhasil dihapus", "status": inv.Status})
}
