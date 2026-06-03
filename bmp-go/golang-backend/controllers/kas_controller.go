package controllers

import (
	"bytes"
	"encoding/csv"
	"fmt"
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"log"
	"strings"
	"time"

	"github.com/gofiber/fiber/v2"
)

// GetCashFlows retrieves cashflows with demo isolation.
// Demo users see only cashflows whose linked invoice belongs to a demo client.
// Production users see cashflows without any demo-client link.
func GetCashFlows(c *fiber.Ctx) error {
	limit := 2000 // Default limit
	isDemo := IsDemoUser(c)

	var flows []models.CashFlow
	database.DB.Where("is_demo = ?", isDemo).
		Order("cash_flows.transaction_date desc, cash_flows.id desc").
		Limit(limit).Find(&flows)

	return c.JSON(fiber.Map{
		"success": true,
		"data":    flows,
	})
}


// CreateCashFlow creates a new cashflow entry in the database.
func CreateCashFlow(c *fiber.Ctx) error {
	type CashFlowInput struct {
		TransactionDate string  `json:"transaction_date"`
		TransactionType string  `json:"transaction_type"`
		Description     string  `json:"description"`
		Amount          float64 `json:"amount"`
	}

	input := new(CashFlowInput)
	if err := c.BodyParser(input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input"})
	}

	if input.Amount <= 0 {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Amount must be greater than 0"})
	}

	// Parse TransactionDate more robustly
	parsedDate, err := time.Parse(time.RFC3339, input.TransactionDate)
	if err != nil {
		parsedDate, err = time.Parse("2006-01-02", input.TransactionDate)
		if err != nil {
			parsedDate, err = time.Parse("02/01/2006", input.TransactionDate)
			if err != nil {
				parsedDate = time.Now()
			}
		}
	}

	flow := models.CashFlow{
		TransactionDate: parsedDate,
		TransactionType: input.TransactionType,
		Description:     input.Description,
		Amount:          input.Amount,
		IsDemo:          IsDemoUser(c),
		DateCreated:     time.Now(),
	}

	if err := database.DB.Create(&flow).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to record cash flow"})
	}

	return c.Status(201).JSON(fiber.Map{
		"success": true,
		"message": "Transaksi berhasil dicatat",
		"data":    flow,
	})
}

// UpdateCashFlow updates a cashflow entry and handles updating any related invoice payments and invoice statuses.
func UpdateCashFlow(c *fiber.Ctx) error {
	id := c.Params("id")
	var flow models.CashFlow

	if err := database.DB.First(&flow, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "CashFlow not found"})
	}

	if flow.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	type CashFlowUpdateInput struct {
		Amount          *float64   `json:"amount"`
		Description     *string    `json:"description"`
		TransactionType *string    `json:"transaction_type"`
		TransactionDate *string    `json:"transaction_date"`
	}

	input := new(CashFlowUpdateInput)
	if err := c.BodyParser(input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input"})
	}

	tx := database.DB.Begin()

	if input.Amount != nil {
		if *input.Amount <= 0 {
			tx.Rollback()
			return c.Status(400).JSON(fiber.Map{"success": false, "message": "Amount must be greater than 0"})
		}
		flow.Amount = *input.Amount
	}

	if input.Description != nil {
		flow.Description = *input.Description
	}

	if input.TransactionType != nil {
		flow.TransactionType = *input.TransactionType
	}

	if input.TransactionDate != nil {
		parsedDate, err := time.Parse(time.RFC3339, *input.TransactionDate)
		if err != nil {
			parsedDate, err = time.Parse("2006-01-02", *input.TransactionDate)
			if err != nil {
				parsedDate, err = time.Parse("02/01/2006", *input.TransactionDate)
				if err != nil {
					parsedDate = time.Now()
				}
			}
		}
		flow.TransactionDate = parsedDate
	}

	if err := tx.Save(&flow).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to update CashFlow"})
	}

	// Sync with InvoicePayment if payment_ref_id is present
	if flow.PaymentRefID != nil {
		var payment models.InvoicePayment
		if err := tx.First(&payment, *flow.PaymentRefID).Error; err == nil {
			if input.Amount != nil {
				payment.PaymentAmount = int(*input.Amount)
			}
			if input.TransactionDate != nil {
				payment.PaymentDate = flow.TransactionDate
			}
			if err := tx.Save(&payment).Error; err != nil {
				tx.Rollback()
				return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to update Invoice Payment reference"})
			}

			// Recalculate invoice status (total vs paid) — menggunakan rumus IsKhusus
			var invoice models.Invoice
			if err := tx.First(&invoice, payment.InvoiceID).Error; err == nil {
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
				if err := tx.Save(&invoice).Error; err != nil {
					tx.Rollback()
					return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to update Invoice status"})
				}
			}
		}
	}

	if err := tx.Commit().Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan perubahan update kas"})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"message": "Data kas berhasil diperbarui",
		"data":    flow,
	})
}

// DeleteCashFlow deletes a cashflow entry and updates its related InvoicePayment and Invoice.
func DeleteCashFlow(c *fiber.Ctx) error {
	id := c.Params("id")
	var flow models.CashFlow

	// 1. Ambil data kas terlebih dahulu sebelum dihapus (GET data kas)
	if err := database.DB.Preload("PaymentRef").First(&flow, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "CashFlow not found"})
	}

	if flow.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	// Ambil informasi nominal, FakturID, dan jenis transaksi
	nominal := flow.Amount
	var invoiceID uint
	isPaymentLinked := false
	jenisTransaksi := "Manual/Biasa"

	if flow.PaymentRefID != nil && flow.PaymentRef.ID != 0 {
		invoiceID = flow.PaymentRef.InvoiceID
		isPaymentLinked = true
		if strings.Contains(flow.PaymentRef.PaymentMethod, "Borongan") {
			jenisTransaksi = "Pembayaran Borongan"
		} else {
			jenisTransaksi = "Pembayaran Faktur Biasa"
		}
	}

	log.Printf("[DELETE KAS] Memproses Hapus Kas ID: %d | Nominal: %.2f | Jenis: %s | Terikat Faktur ID: %d\n",
		flow.ID, nominal, jenisTransaksi, invoiceID)

	// 3. Jalankan dalam Database Transaction (Tx) secara Atomik
	tx := database.DB.Begin()
	if tx.Error != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal memulai transaksi database"})
	}

	// Langkah A: Hapus data kas keuangan tersebut
	if err := tx.Delete(&flow).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to delete CashFlow"})
	}

	// Langkah B & C: Jika data kas memiliki keterikatan dengan FakturID, update nominal & hitung ulang status
	if isPaymentLinked {
		var payment models.InvoicePayment
		if err := tx.First(&payment, *flow.PaymentRefID).Error; err == nil {
			// Hapus data pembayaran (mengurangi nominal total_terbayar karena data payment hilang dari sum)
			if err := tx.Delete(&payment).Error; err != nil {
				tx.Rollback()
				return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to delete Invoice Payment reference"})
			}

			// Langkah C: Hitung ulang status faktur tersebut
			var invoice models.Invoice
			if err := tx.First(&invoice, invoiceID).Error; err == nil {
				var products []models.Product
				tx.Where("invoice_id = ?", invoice.ID).Find(&products)
				
				// Hitung total tagihan (menggunakan rumus is_khusus yang disinkronkan)
				totalBill := 0.0
				for _, p := range products {
					totalBill += p.Quantity * p.JumlahLusin * p.Price
				}

				// Hitung sisa total pembayaran yang tersisa setelah penghapusan
				// Eksplisit exclude payment yang baru dihapus dari kalkulasi
				var remainingPayments []models.InvoicePayment
				tx.Where("invoice_id = ? AND deleted_at IS NULL AND id != ?", invoice.ID, payment.ID).Find(&remainingPayments)

				
				totalPaid := 0.0
				for _, p := range remainingPayments {
					totalPaid += float64(p.PaymentAmount)
				}

				// Tentukan status baru secara otomatis
				sisaTagihan := totalBill - totalPaid
				if sisaTagihan <= 0 {
					invoice.Status = "PAID"
				} else if totalPaid > 0 {
					invoice.Status = "PARTIAL"
				} else {
					invoice.Status = "UNPAID"
				}

				log.Printf("[DELETE KAS] Recalculate Faktur ID: %d | Total Tagihan: %.2f | Sisa Terbayar: %.2f | Status Baru: %s\n",
					invoice.ID, totalBill, totalPaid, invoice.Status)

				// Simpan perubahan status faktur
				if err := tx.Save(&invoice).Error; err != nil {
					tx.Rollback()
					return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to update Invoice status after deletion"})
				}
			}
		}
	}

	if err := tx.Commit().Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan perubahan transaksi"})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"message": "Data riwayat kas berhasil dihapus dan status faktur disinkronkan",
	})
}

// SyncKas cleans up Nono and resyncs invoice payments
func SyncKas(c *fiber.Ctx) error {
	isDemo := IsDemoUser(c)
	database.DB.Where("description LIKE ? AND is_demo = ?", "%Nono%", isDemo).Delete(&models.CashFlow{})

	syncedCount := 0
	today := time.Now()

	var payments []models.InvoicePayment
	database.DB.Preload("Invoice").Preload("Invoice.Client").
		Joins("JOIN invoices ON invoices.id = invoice_payments.invoice_id").
		Where("invoices.is_demo = ?", isDemo).
		Find(&payments)

	for _, p := range payments {
		var count int64
		database.DB.Model(&models.CashFlow{}).Where("payment_ref_id = ? AND is_demo = ?", p.ID, isDemo).Count(&count)

		if count == 0 {
			clientName := "Tanpa Nama"
			if p.Invoice.Client.ID != 0 {
				clientName = p.Invoice.Client.ClientName
			}

			cf := models.CashFlow{
				TransactionDate: p.PaymentDate,
				TransactionType: "MASUK",
				Description:     "Pembayaran Faktur " + p.Invoice.Number + " (" + clientName + ")",
				Amount:          float64(p.PaymentAmount),
				PaymentRefID:    &p.ID,
				IsDemo:          isDemo,
				DateCreated:     today,
			}
			database.DB.Create(&cf)
			syncedCount++
		}
	}

	return c.JSON(fiber.Map{
		"success":      true,
		"message":      "Pembersihan Sukses! Data kas disinkronisasi",
		"synced_count": syncedCount,
	})
}

// DownloadCashFlowCSV mengekspor data Kas Keluar Masuk menjadi file .csv
func DownloadCashFlowCSV(c *fiber.Ctx) error {
	var cashFlows []models.CashFlow
	isDemo := IsDemoUser(c)

	// Ambil semua data kas, urutkan dari tanggal terbaru ke terlama
	if err := database.DB.Where("is_demo = ?", isDemo).Order("transaction_date desc, id desc").Find(&cashFlows).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal mengambil data kas"})
	}

	// Siapkan penampung untuk menulis file CSV
	b := &bytes.Buffer{}
	w := csv.NewWriter(b)

	// 1. Tulis Header (Judul Kolom) di baris pertama
	headers := []string{"Tanggal", "Tipe Transaksi", "Keterangan", "Nominal"}
	if err := w.Write(headers); err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menulis header CSV"})
	}

	// 2. Looping data kas dan tulis ke baris-baris berikutnya
	for _, kas := range cashFlows {
		tanggal := kas.TransactionDate.Format("2006-01-02")
		tipe := kas.TransactionType
		keterangan := kas.Description
		// Cetak nominal murni angka (tanpa Rp/koma) agar bisa di-SUM di Excel
		nominal := fmt.Sprintf("%.0f", kas.Amount)

		row := []string{tanggal, tipe, keterangan, nominal}
		if err := w.Write(row); err != nil {
			return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menulis baris data CSV"})
		}
	}

	// Pastikan semua data masuk ke buffer
	w.Flush()
	if err := w.Error(); err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal memproses file CSV"})
	}

	// 3. Atur Header HTTP agar browser mengerti ini adalah file yang harus didownload
	fileName := fmt.Sprintf("Laporan_Kas_%s.csv", time.Now().Format("20060102_150405"))
	c.Set("Content-Type", "text/csv")
	c.Set("Content-Disposition", fmt.Sprintf("attachment; filename=\"%s\"", fileName))

	// Kirim file-nya ke pengguna!
	return c.Send(b.Bytes())
}
