package controllers

import (
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"time"

	"github.com/gofiber/fiber/v2"
)

func GetDashboardSummary(c *fiber.Ctx) error {
	isDemo := IsDemoUser(c)

	var totalClients int64
	database.DB.Model(&models.Client{}).Where("is_demo = ?", isDemo).Count(&totalClients)

	var totalProducts int64
	database.DB.Model(&models.MasterProduct{}).Where("is_demo = ?", isDemo).Count(&totalProducts)

	var countInvoices int64
	database.DB.Model(&models.Invoice{}).
		Where("invoices.status != ? AND invoices.is_demo = ?", "DRAFT", isDemo).
		Count(&countInvoices)

	// Hitung Kas
	type SumResult struct {
		Total float64
	}

	var totalKasIn SumResult
	var totalKasOut SumResult

	database.DB.Model(&models.CashFlow{}).
		Select("COALESCE(sum(amount), 0) as total").
		Where("transaction_type = ? AND is_demo = ?", "MASUK", isDemo).
		Scan(&totalKasIn)

	if isDemo {
		database.DB.Model(&models.CashFlow{}).
			Select("COALESCE(sum(amount), 0) as total").
			Where("transaction_type = ? AND is_demo = ?", "KELUAR", isDemo).
			Scan(&totalKasOut)
	} else {
		database.DB.Model(&models.CashFlow{}).
			Select("COALESCE(sum(amount), 0) as total").
			Where("transaction_type = ? AND is_demo = ? AND description NOT LIKE ?", "KELUAR", isDemo, "%Nono%").
			Scan(&totalKasOut)
	}

	// Hitung Bahan Nono
	nonoTotalBahan := 0.0
	nonoTotalBayar := 0.0
	var nonoData []models.BahanNono
	database.DB.Where("is_demo = ?", isDemo).Find(&nonoData)
	for _, n := range nonoData {
		nonoTotalBahan += n.TotalHarga
		nonoTotalBayar += n.Nominal
	}
	nonoSisaHutang := nonoTotalBahan - nonoTotalBayar

	// Rumus Pemotongan Aman
	saldoKas := totalKasIn.Total - totalKasOut.Total - nonoTotalBayar

	// Recent invoices
	var recentInvoices []models.Invoice
	database.DB.Preload("Client").
		Where("invoices.is_demo = ?", isDemo).
		Order("invoices.id desc").Limit(5).Find(&recentInvoices)

	type RecentInv struct {
		Number     string  `json:"number"`
		ClientName string  `json:"client_name"`
		Total      float64 `json:"get_total"`
		Status     string  `json:"status"`
	}
	var recent []RecentInv
	for _, inv := range recentInvoices {
		var products []models.Product
		database.DB.Where("invoice_id = ?", inv.ID).Find(&products)
		total := 0.0
		for _, p := range products {
			total += p.Quantity * p.JumlahLusin * p.Price
		}

		cName := "-"
		if inv.Client.ID != 0 {
			cName = inv.Client.ClientName
		}
		recent = append(recent, RecentInv{
			Number:     inv.Number,
			ClientName: cName,
			Total:      total,
			Status:     inv.Status,
		})
	}

	// Invoices Statuses
	var allInvoices []models.Invoice
	database.DB.
		Where("invoices.status != ? AND invoices.is_demo = ?", "DRAFT", isDemo).
		Find(&allInvoices)

	var countLunas, countBelum, countTelat int64
	var totalInvoicesIdr, totalLunasIdr, totalBelumIdr, totalTelatIdr float64

	today := time.Now().Truncate(24 * time.Hour)

	for _, inv := range allInvoices {
		var products []models.Product
		database.DB.Where("invoice_id = ?", inv.ID).Find(&products)
		total := 0.0
		for _, p := range products {
			total += p.Quantity * p.JumlahLusin * p.Price
		}

		totalInvoicesIdr += total

		if inv.Status == "PAID" {
			countLunas++
			totalLunasIdr += total
		} else {
			if inv.DueDate != nil && inv.DueDate.Before(today) {
				countTelat++
				totalTelatIdr += total
			} else {
				countBelum++
				totalBelumIdr += total
			}
		}
	}

	simulasiSaldo := totalInvoicesIdr - nonoTotalBahan - totalKasOut.Total

	return c.JSON(fiber.Map{
		"success": true,
		"data": fiber.Map{
			"total_clients":      totalClients,
			"total_products":     totalProducts,
			"count_invoices":     countInvoices,
			"total_kas_in":       totalKasIn.Total,
			"total_kas_out":      totalKasOut.Total,
			"saldo_kas":          saldoKas,
			"nono_total_bahan":   nonoTotalBahan,
			"nono_total_bayar":   nonoTotalBayar,
			"nono_sisa_hutang":   nonoSisaHutang,
			"recent_invoices":    recent,
			"count_lunas":        countLunas,
			"total_lunas_idr":    totalLunasIdr,
			"count_belum":        countBelum,
			"total_belum_idr":    totalBelumIdr,
			"count_telat":        countTelat,
			"total_telat_idr":    totalTelatIdr,
			"total_invoices_idr": totalInvoicesIdr,
			"simulasi_saldo":     simulasiSaldo,
		},
	})
}

