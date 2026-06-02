package controllers

import (
	"fmt"
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"log"
	"strings"

	"github.com/gofiber/fiber/v2"
)

// Helper to format Rupiah-like strings for display
func formatRupiah(val float64) string {
	return fmt.Sprintf("%.2f", val)
}

// Helper to get client name from invoice object
func getClientNameFromInvoice(inv models.Invoice) string {
	if inv.Client.ClientName != "" {
		return inv.Client.ClientName
	}
	return "Tanpa Nama / Non-Client"
}

// Helper to extract client name from description inside parentheses
func extractClientNameFromDesc(desc string) string {
	start := strings.LastIndex(desc, "(")
	end := strings.LastIndex(desc, ")")
	if start != -1 && end != -1 && end > start {
		return desc[start+1 : end]
	}
	return "Tanpa Nama / Non-Client"
}

// AuditDatabaseTerminal runs the database audit and logs the output directly to the terminal console.
func AuditDatabaseTerminal() {
	log.Println("🔍 [AUDIT] Menjalankan Audit & Deteksi Anomali Database...")

	if database.DB == nil {
		log.Println("❌ [AUDIT] Database connection is nil. Audit skipped.")
		return
	}

	// 1. Transaksi di Bawah Standar
	var underStandardFlows []models.CashFlow
	database.DB.Preload("PaymentRef.Invoice.Client").
		Where("transaction_type IN ? AND amount <= ?", []string{"MASUK", "IN"}, 500.0).
		Find(&underStandardFlows)

	log.Printf("📊 [AUDIT] 1. Transaksi di Bawah Standar (Pembayaran <= Rp 500): %d ditemukan\n", len(underStandardFlows))
	for _, f := range underStandardFlows {
		clientName := "Tanpa Nama"
		if f.PaymentRef.Invoice.Client.ClientName != "" {
			clientName = f.PaymentRef.Invoice.Client.ClientName
		} else {
			clientName = extractClientNameFromDesc(f.Description)
		}
		log.Printf("   - ID Kas: %d | Pelanggan: %s | Tanggal: %s | Nominal: Rp %s\n",
			f.ID, clientName, f.TransactionDate.Format("2006-01-02"), formatRupiah(f.Amount))
	}

	// 2. Faktur Gantung (Belum Interconnect)
	// Case A: Invoice PAID but missing payments/cashflows
	var paidInvoices []models.Invoice
	database.DB.Preload("Client").Preload("Payments").Where("status = ?", "PAID").Find(&paidInvoices)

	var hangingInvoicesCount = 0
	log.Println("📊 [AUDIT] 2. Faktur Gantung (Belum Interconnect):")
	for _, inv := range paidInvoices {
		if len(inv.Payments) == 0 {
			hangingInvoicesCount++
			log.Printf("   - [MISSING PAYMENTS] Faktur: %s | Pelanggan: %s | Status: %s | Masalah: Status 'PAID' tetapi tidak ada data pembayaran (InvoicePayment)\n",
				inv.Number, getClientNameFromInvoice(inv), inv.Status)
		} else {
			for _, pay := range inv.Payments {
				var count int64
				database.DB.Model(&models.CashFlow{}).Where("payment_ref_id = ?", pay.ID).Count(&count)
				if count == 0 {
					hangingInvoicesCount++
					log.Printf("   - [MISSING CASHFLOW] Faktur: %s | Pelanggan: %s | Pembayaran ID: %d | Masalah: Pembayaran tercatat tetapi tidak terikat ke Buku Kas Utama (CashFlow)\n",
						inv.Number, getClientNameFromInvoice(inv), pay.ID)
				}
			}
		}
	}

	// Case B: Cash Flow MASUK but not linked to any InvoicePayment
	var unconnectedCashFlows []models.CashFlow
	database.DB.Where("transaction_type IN ? AND payment_ref_id IS NULL AND (description LIKE ? OR description LIKE ? OR description LIKE ? OR description LIKE ?)",
		[]string{"MASUK", "IN"}, "%Faktur%", "%INV%", "%Invoice%", "%Pembayaran%").
		Find(&unconnectedCashFlows)

	log.Printf("   - [UNCONNECTED CASHFLOW] Uang Masuk Tanpa Referensi Faktur: %d ditemukan\n", len(unconnectedCashFlows))
	for _, f := range unconnectedCashFlows {
		log.Printf("     * ID Kas: %d | Deskripsi: %s | Tanggal: %s | Nominal: Rp %s\n",
			f.ID, f.Description, f.TransactionDate.Format("2006-01-02"), formatRupiah(f.Amount))
	}

	// 3. Mismatch Nominal
	var allInvoices []models.Invoice
	database.DB.Preload("Client").Preload("Payments").Find(&allInvoices)

	var mismatchCount = 0
	log.Println("📊 [AUDIT] 3. Mismatch Nominal (Tagihan vs Buku Kas):")
	for _, inv := range allInvoices {
		if inv.Status == "DRAFT" {
			continue
		}

		var totalBill float64
		var products []models.Product
		database.DB.Where("invoice_id = ?", inv.ID).Find(&products)
		for _, p := range products {
			totalBill += p.Quantity * p.JumlahLusin * p.Price
		}

		var totalPaid float64
		for _, p := range inv.Payments {
			totalPaid += float64(p.PaymentAmount)
		}

		var totalCashFlow float64
		var paymentIDs []uint
		for _, p := range inv.Payments {
			paymentIDs = append(paymentIDs, p.ID)
		}
		if len(paymentIDs) > 0 {
			database.DB.Model(&models.CashFlow{}).
				Select("COALESCE(SUM(amount), 0)").
				Where("payment_ref_id IN ?", paymentIDs).
				Scan(&totalCashFlow)
		}

		isMismatch := false
		reason := ""

		if inv.Status == "PAID" && totalPaid != totalBill {
			isMismatch = true
			reason = fmt.Sprintf("Status 'PAID' tetapi Total Bayar (Rp %s) != Tagihan (Rp %s)", formatRupiah(totalPaid), formatRupiah(totalBill))
		} else if totalPaid != totalCashFlow {
			isMismatch = true
			reason = fmt.Sprintf("Total Bayar (Rp %s) != Total Uang Masuk Kas (Rp %s)", formatRupiah(totalPaid), formatRupiah(totalCashFlow))
		} else if totalPaid > totalBill {
			isMismatch = true
			reason = fmt.Sprintf("Kelebihan Bayar: Total Bayar (Rp %s) melebihi Tagihan (Rp %s)", formatRupiah(totalPaid), formatRupiah(totalBill))
		}

		if isMismatch {
			mismatchCount++
			log.Printf("   - Faktur: %s | Pelanggan: %s | Status: %s | Tagihan: Rp %s | Bayar: Rp %s | Buku Kas: Rp %s | Masalah: %s\n",
				inv.Number, getClientNameFromInvoice(inv), inv.Status, formatRupiah(totalBill), formatRupiah(totalPaid), formatRupiah(totalCashFlow), reason)
		}
	}

	log.Printf("✅ [AUDIT] Audit Database selesai. Ringkasan: %d anomali nominal rendah, %d faktur gantung/tidak sinkron, %d mismatch nominal.\n",
		len(underStandardFlows), hangingInvoicesCount+len(unconnectedCashFlows), mismatchCount)
}

// GetAuditReport is the Fiber controller function that performs database audit and returns a JSON report.
func GetAuditReport(c *fiber.Ctx) error {
	if database.DB == nil {
		return c.Status(500).JSON(fiber.Map{
			"success": false,
			"message": "Database connection is not initialized",
		})
	}

	isDemo := IsDemoUser(c)

	// 1. Transaksi di Bawah Standar
	var underStandardFlows []models.CashFlow
	database.DB.Preload("PaymentRef.Invoice.Client").
		Where("transaction_type IN ? AND amount <= ? AND is_demo = ?", []string{"MASUK", "IN"}, 500.0, isDemo).
		Find(&underStandardFlows)

	underStandardResults := []map[string]interface{}{}
	for _, f := range underStandardFlows {
		clientName := "Tanpa Nama"
		if f.PaymentRef.Invoice.Client.ClientName != "" {
			clientName = f.PaymentRef.Invoice.Client.ClientName
		} else {
			clientName = extractClientNameFromDesc(f.Description)
		}
		underStandardResults = append(underStandardResults, map[string]interface{}{
			"id":               f.ID,
			"client_name":      clientName,
			"transaction_date": f.TransactionDate,
			"amount":           f.Amount,
			"description":      f.Description,
		})
	}

	// 2. Faktur Gantung
	var paidInvoices []models.Invoice
	database.DB.Preload("Client").Preload("Payments").Where("status = ? AND is_demo = ?", "PAID", isDemo).Find(&paidInvoices)

	hangingInvoices := []map[string]interface{}{}
	for _, inv := range paidInvoices {
		if len(inv.Payments) == 0 {
			hangingInvoices = append(hangingInvoices, map[string]interface{}{
				"invoice_id":  inv.ID,
				"number":      inv.Number,
				"client_name": getClientNameFromInvoice(inv),
				"status":      inv.Status,
				"reason":      "Status 'PAID' (Lunas) tetapi tidak ada record pembayaran (InvoicePayment)",
			})
		} else {
			for _, pay := range inv.Payments {
				var count int64
				database.DB.Model(&models.CashFlow{}).Where("payment_ref_id = ? AND is_demo = ?", pay.ID, isDemo).Count(&count)
				if count == 0 {
					hangingInvoices = append(hangingInvoices, map[string]interface{}{
						"invoice_id":  inv.ID,
						"number":      inv.Number,
						"client_name": getClientNameFromInvoice(inv),
						"status":      inv.Status,
						"reason":      fmt.Sprintf("Pembayaran ID %d ada tetapi tidak terhubung ke tabel Buku Kas Utama (missing CashFlow)", pay.ID),
					})
				}
			}
		}
	}

	var unconnectedCashFlows []models.CashFlow
	database.DB.Where("transaction_type IN ? AND payment_ref_id IS NULL AND is_demo = ? AND (description LIKE ? OR description LIKE ? OR description LIKE ? OR description LIKE ?)",
		[]string{"MASUK", "IN"}, isDemo, "%Faktur%", "%INV%", "%Invoice%", "%Pembayaran%").
		Find(&unconnectedCashFlows)

	unconnectedResults := []map[string]interface{}{}
	for _, f := range unconnectedCashFlows {
		unconnectedResults = append(unconnectedResults, map[string]interface{}{
			"cashflow_id":      f.ID,
			"description":      f.Description,
			"transaction_date": f.TransactionDate,
			"amount":           f.Amount,
			"reason":           "Uang masuk terindikasi pembayaran faktur tetapi tidak terhubung ke Faktur manapun (payment_ref_id kosong)",
		})
	}

	// 3. Mismatch Nominal
	var allInvoices []models.Invoice
	database.DB.Preload("Client").Preload("Payments").Where("is_demo = ?", isDemo).Find(&allInvoices)

	mismatchedInvoices := []map[string]interface{}{}
	for _, inv := range allInvoices {
		if inv.Status == "DRAFT" {
			continue
		}

		var totalBill float64
		var products []models.Product
		database.DB.Where("invoice_id = ?", inv.ID).Find(&products)
		for _, p := range products {
			totalBill += p.Quantity * p.JumlahLusin * p.Price
		}

		var totalPaid float64
		for _, p := range inv.Payments {
			totalPaid += float64(p.PaymentAmount)
		}

		var totalCashFlow float64
		var paymentIDs []uint
		for _, p := range inv.Payments {
			paymentIDs = append(paymentIDs, p.ID)
		}
		if len(paymentIDs) > 0 {
			database.DB.Model(&models.CashFlow{}).
				Select("COALESCE(SUM(amount), 0)").
				Where("payment_ref_id IN ? AND is_demo = ?", paymentIDs, isDemo).
				Scan(&totalCashFlow)
		}

		isMismatch := false
		reason := ""

		if inv.Status == "PAID" && totalPaid != totalBill {
			isMismatch = true
			reason = fmt.Sprintf("Status 'PAID' tetapi Total Bayar (Rp %s) != Tagihan (Rp %s)", formatRupiah(totalPaid), formatRupiah(totalBill))
		} else if totalPaid != totalCashFlow {
			isMismatch = true
			reason = fmt.Sprintf("Total Bayar (Rp %s) != Total Uang Masuk Buku Kas (Rp %s)", formatRupiah(totalPaid), formatRupiah(totalCashFlow))
		} else if totalPaid > totalBill {
			isMismatch = true
			reason = fmt.Sprintf("Kelebihan Bayar: Total Bayar (Rp %s) melebihi Tagihan (Rp %s)", formatRupiah(totalPaid), formatRupiah(totalBill))
		}

		if isMismatch {
			mismatchedInvoices = append(mismatchedInvoices, map[string]interface{}{
				"invoice_id":     inv.ID,
				"number":         inv.Number,
				"client_name":    getClientNameFromInvoice(inv),
				"status":         inv.Status,
				"total_bill":     totalBill,
				"total_paid":     totalPaid,
				"total_cashflow": totalCashFlow,
				"reason":         reason,
			})
		}
	}

	// Trigger the terminal output as well so they can see it in real-time
	AuditDatabaseTerminal()

	return c.JSON(fiber.Map{
		"success": true,
		"data": fiber.Map{
			"under_standard_transactions": underStandardResults,
			"hanging_invoices":            hangingInvoices,
			"unconnected_cashflows":       unconnectedResults,
			"mismatched_invoices":         mismatchedInvoices,
		},
	})
}

// CleanupOrphanPayments menghapus semua InvoicePayment yang tidak terhubung ke CashFlow manapun,
// lalu menghitung ulang status faktur yang terdampak secara atomik.
func CleanupOrphanPayments(c *fiber.Ctx) error {
	if database.DB == nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Database tidak tersedia"})
	}

	isDemo := IsDemoUser(c)

	// 1. Temukan semua InvoicePayment yang tidak punya referensi CashFlow
	var allPayments []models.InvoicePayment
	database.DB.
		Joins("JOIN invoices ON invoices.id = invoice_payments.invoice_id").
		Where("invoices.is_demo = ?", isDemo).
		Find(&allPayments)

	type OrphanInfo struct {
		PaymentID     uint    `json:"payment_id"`
		InvoiceID     uint    `json:"invoice_id"`
		PaymentAmount int     `json:"payment_amount"`
		PaymentMethod string  `json:"payment_method"`
	}

	var orphanPayments []models.InvoicePayment
	var orphanInfos []OrphanInfo

	for _, p := range allPayments {
		var count int64
		database.DB.Model(&models.CashFlow{}).Where("payment_ref_id = ? AND is_demo = ?", p.ID, isDemo).Count(&count)
		if count == 0 {
			orphanPayments = append(orphanPayments, p)
			orphanInfos = append(orphanInfos, OrphanInfo{
				PaymentID:     p.ID,
				InvoiceID:     p.InvoiceID,
				PaymentAmount: p.PaymentAmount,
				PaymentMethod: p.PaymentMethod,
			})
		}
	}

	if len(orphanPayments) == 0 {
		return c.JSON(fiber.Map{
			"success": true,
			"message": "Tidak ada data pembayaran orphan yang ditemukan. Semua data sudah sinkron.",
			"cleaned": 0,
			"details": []interface{}{},
		})
	}

	log.Printf("[CLEANUP] Ditemukan %d InvoicePayment orphan (tanpa referensi kas). Memulai proses cleanup...\n", len(orphanPayments))

	// 2. Kumpulkan InvoiceID unik yang terdampak untuk recalculate nanti
	affectedInvoiceIDs := map[uint]bool{}
	for _, p := range orphanPayments {
		affectedInvoiceIDs[p.InvoiceID] = true
	}

	// 3. Jalankan dalam transaksi atomik
	tx := database.DB.Begin()
	if tx.Error != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal memulai transaksi database"})
	}

	// Hapus semua orphan payments
	for _, p := range orphanPayments {
		if err := tx.Delete(&p).Error; err != nil {
			tx.Rollback()
			log.Printf("[CLEANUP] ❌ Gagal menghapus InvoicePayment ID %d: %v\n", p.ID, err)
			return c.Status(500).JSON(fiber.Map{
				"success": false,
				"message": fmt.Sprintf("Gagal menghapus pembayaran orphan ID %d", p.ID),
			})
		}
		log.Printf("[CLEANUP] ✅ Hapus InvoicePayment ID %d | Invoice %d | Rp %d\n", p.ID, p.InvoiceID, p.PaymentAmount)
	}

	// 4. Recalculate status faktur yang terdampak
	for invoiceID := range affectedInvoiceIDs {
		var invoice models.Invoice
		if err := tx.First(&invoice, invoiceID).Error; err != nil {
			log.Printf("[CLEANUP] ⚠️ Invoice ID %d tidak ditemukan, skip recalculate.\n", invoiceID)
			continue
		}

		// Hitung total tagihan
		var products []models.Product
		tx.Where("invoice_id = ?", invoiceID).Find(&products)
		totalBill := 0.0
		for _, p := range products {
			totalBill += p.Quantity * p.JumlahLusin * p.Price
		}

		// Hitung sisa pembayaran yang valid (sudah exclude orphan yang baru dihapus)
		var remainingPayments []models.InvoicePayment
		tx.Where("invoice_id = ?", invoiceID).Find(&remainingPayments)
		totalPaid := 0.0
		for _, p := range remainingPayments {
			totalPaid += float64(p.PaymentAmount)
		}

		// Update status faktur
		sisaTagihan := totalBill - totalPaid
		if sisaTagihan <= 0 {
			invoice.Status = "PAID"
		} else if totalPaid > 0 {
			invoice.Status = "PARTIAL"
		} else {
			invoice.Status = "UNPAID"
		}

		log.Printf("[CLEANUP] 🔄 Recalculate Faktur ID %d | Tagihan: %.2f | Terbayar: %.2f | Status Baru: %s\n",
			invoiceID, totalBill, totalPaid, invoice.Status)

		if err := tx.Save(&invoice).Error; err != nil {
			tx.Rollback()
			return c.Status(500).JSON(fiber.Map{
				"success": false,
				"message": fmt.Sprintf("Gagal update status faktur ID %d", invoiceID),
			})
		}
	}

	if err := tx.Commit().Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal commit transaksi cleanup"})
	}

	log.Printf("[CLEANUP] ✅ Cleanup selesai! %d InvoicePayment orphan berhasil dihapus.\n", len(orphanPayments))

	return c.JSON(fiber.Map{
		"success": true,
		"message": fmt.Sprintf("Berhasil! %d data pembayaran orphan dihapus dan status faktur terkait sudah diperbarui.", len(orphanPayments)),
		"cleaned": len(orphanPayments),
		"details": orphanInfos,
	})
}
