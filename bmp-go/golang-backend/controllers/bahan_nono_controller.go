package controllers

import (
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"time"

	"github.com/gofiber/fiber/v2"
)

// Get all Bahan Nono transactions
func GetBahanNono(c *fiber.Ctx) error {
	var data []models.BahanNono

	// Preload items automatically
	database.DB.Preload("Items").Where("is_demo = ?", IsDemoUser(c)).Order("tanggal desc, id desc").Find(&data)

	totalCashIn := 0.0
	totalCashOut := 0.0

	for _, b := range data {
		if b.TotalHarga > 0 {
			totalCashIn += b.TotalHarga
		}
		if b.Nominal > 0 {
			totalCashOut += b.Nominal
		}
	}

	balance := totalCashIn - totalCashOut

	return c.JSON(fiber.Map{
		"success": true,
		"data": fiber.Map{
			"transactions":   data,
			"total_cash_in":  totalCashIn,
			"total_cash_out": totalCashOut,
			"balance":        balance,
		},
	})
}

// Create Cash IN or OUT for Bahan Nono
func CreateBahanNono(c *fiber.Ctx) error {
	type ItemInput struct {
		JenisBahan string  `json:"jenis_bahan"`
		Kuantitas  float64 `json:"kuantitas"`
		Unit       string  `json:"unit"`
		Rate       float64 `json:"rate"`
	}

	type NonoInput struct {
		TransType string      `json:"trans_type"` // "IN" or "OUT"
		Tanggal   string      `json:"tanggal"`    // Change to string for flexible parsing
		Notes     string      `json:"notes"`
		Nominal   float64     `json:"nominal"`
		Tagihan   string      `json:"tagihan"`
		Items     []ItemInput `json:"items"`
	}

	input := new(NonoInput)
	if err := c.BodyParser(input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input"})
	}

	// Parse Tanggal more robustly
	parsedTanggal, err := time.Parse(time.RFC3339, input.Tanggal)
	if err != nil {
		// Try parsing from YYYY-MM-DD
		parsedTanggal, err = time.Parse("2006-01-02", input.Tanggal)
		if err != nil {
			// Try parsing from DD/MM/YYYY (common in Indonesia)
			parsedTanggal, err = time.Parse("02/01/2006", input.Tanggal)
			if err != nil {
				// Fallback to today if all parsing fails
				parsedTanggal = time.Now()
			}
		}
	}

	tx := database.DB.Begin()

	if input.TransType == "IN" {
		totalHarga := 0.0
		for _, item := range input.Items {
			totalHarga += item.Kuantitas * item.Rate
		}

		nono := models.BahanNono{
			Tanggal:    parsedTanggal,
			Notes:      input.Notes,
			TotalHarga: totalHarga,
			Nominal:    0,
			Tagihan:    input.Tagihan,
			IsDemo:     IsDemoUser(c),
		}
		if err := tx.Create(&nono).Error; err != nil {
			tx.Rollback()
			return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan data IN"})
		}

		for _, item := range input.Items {
			if item.Kuantitas > 0 {
				nItem := models.BahanNonoItem{
					BahanNonoID: nono.ID,
					JenisBahan:  item.JenisBahan,
					Kuantitas:   item.Kuantitas,
					Unit:        item.Unit,
					Rate:        item.Rate,
				}
				tx.Create(&nItem)
			}
		}

		tx.Commit()
		return c.JSON(fiber.Map{"success": true, "message": "Data bahan IN tersimpan", "data": nono})

	} else if input.TransType == "OUT" {
		nono := models.BahanNono{
			Tanggal:    parsedTanggal,
			Notes:      input.Notes,
			Nominal:    input.Nominal,
			TotalHarga: 0,
			Tagihan:    input.Tagihan,
			IsDemo:     IsDemoUser(c),
		}
		if err := tx.Create(&nono).Error; err != nil {
			tx.Rollback()
			return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan data OUT"})
		}

		tx.Commit()
		return c.JSON(fiber.Map{"success": true, "message": "Pembayaran bahan OUT tersimpan", "data": nono})
	}

	tx.Rollback()
	return c.Status(400).JSON(fiber.Map{"success": false, "message": "TransType must be IN or OUT"})
}

// Update Bahan Nono transaction
func UpdateBahanNono(c *fiber.Ctx) error {
	id := c.Params("id")
	var nono models.BahanNono
	if err := database.DB.First(&nono, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Data tidak ditemukan"})
	}

	if nono.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{
			"success": false,
			"message": "Akses ditolak.",
		})
	}

	type ItemInput struct {
		JenisBahan string  `json:"jenis_bahan"`
		Kuantitas  float64 `json:"kuantitas"`
		Unit       string  `json:"unit"`
		Rate       float64 `json:"rate"`
	}

	type NonoInput struct {
		TransType string      `json:"trans_type"`
		Tanggal   string      `json:"tanggal"` // Change to string for flexible parsing
		Notes     string      `json:"notes"`
		Nominal   float64     `json:"nominal"`
		Tagihan   string      `json:"tagihan"`
		Items     []ItemInput `json:"items"`
	}

	input := new(NonoInput)
	if err := c.BodyParser(input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input"})
	}

	// Parse Tanggal more robustly
	parsedTanggal, err := time.Parse(time.RFC3339, input.Tanggal)
	if err != nil {
		// Try parsing from YYYY-MM-DD
		parsedTanggal, err = time.Parse("2006-01-02", input.Tanggal)
		if err != nil {
			// Try parsing from DD/MM/YYYY (common in Indonesia)
			parsedTanggal, err = time.Parse("02/01/2006", input.Tanggal)
			if err != nil {
				// Fallback to today if all parsing fails
				parsedTanggal = time.Now()
			}
		}
	}

	tx := database.DB.Begin()

	// Update main record
	nono.Tanggal = parsedTanggal
	nono.Notes = input.Notes
	nono.Tagihan = input.Tagihan

	if input.TransType == "IN" {
		totalHarga := 0.0
		for _, item := range input.Items {
			totalHarga += item.Kuantitas * item.Rate
		}
		nono.TotalHarga = totalHarga
		nono.Nominal = 0

		// Delete old items and recreate
		tx.Where("bahan_nono_id = ?", id).Delete(&models.BahanNonoItem{})
		for _, item := range input.Items {
			if item.Kuantitas > 0 {
				nItem := models.BahanNonoItem{
					BahanNonoID: nono.ID,
					JenisBahan:  item.JenisBahan,
					Kuantitas:   item.Kuantitas,
					Unit:        item.Unit,
					Rate:        item.Rate,
				}
				tx.Create(&nItem)
			}
		}
	} else {
		nono.Nominal = input.Nominal
		nono.TotalHarga = 0
		tx.Where("bahan_nono_id = ?", id).Delete(&models.BahanNonoItem{})
	}

	if err := tx.Save(&nono).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal update data"})
	}

	tx.Commit()
	return c.JSON(fiber.Map{"success": true, "message": "Transaksi berhasil diperbarui", "data": nono})
}

// Delete Bahan Nono transaction
func DeleteBahanNono(c *fiber.Ctx) error {
	id := c.Params("id")
	var nono models.BahanNono
	if err := database.DB.First(&nono, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Data tidak ditemukan"})
	}

	if nono.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{
			"success": false,
			"message": "Akses ditolak.",
		})
	}

	// Delete items first (though CASCADE should handle it if defined, GORM Unscoped delete handles soft-delete if present)
	database.DB.Where("bahan_nono_id = ?", id).Delete(&models.BahanNonoItem{})
	database.DB.Delete(&nono)

	return c.JSON(fiber.Map{"success": true, "message": "Transaksi berhasil dihapus"})
}
