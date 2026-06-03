package controllers

import (
	"fmt"
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"strings"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/google/uuid"
)

// Get all clients
// Demo users only see clients tagged with is_demo=true; production users see is_demo=false.
func GetClients(c *fiber.Ctx) error {
	var clients []models.Client
	database.DB.Where("is_demo = ?", IsDemoUser(c)).Find(&clients)
	return c.JSON(fiber.Map{
		"success": true,
		"data":    clients,
	})
}

// Get a single client by ID
func GetClient(c *fiber.Ctx) error {
	id := c.Params("id")
	var client models.Client

	if err := database.DB.First(&client, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{
			"success": false,
			"message": "Client not found",
		})
	}

	// Isolasi demo: pastikan user hanya bisa akses data yang sesuai
	if client.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{
			"success": false,
			"message": "Akses ditolak.",
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    client,
	})
}

// Create a new client
func CreateClient(c *fiber.Ctx) error {
	client := new(models.Client)

	if err := c.BodyParser(client); err != nil {
		return c.Status(400).JSON(fiber.Map{
			"success": false,
			"message": "Cannot parse JSON",
		})
	}

	// Set default values similar to Django models.py save() method
	client.DateCreated = time.Now()
	client.LastUpdated = time.Now()
	client.UniqueID = uuid.New().String()[:8] // simulate split("-")[4] logic briefly
	client.IsDemo = IsDemoUser(c)

	// 👇 FIX SLUG: Buat slug otomatis dari Nama Klien + Unique ID agar tidak ada yang kembar
	if client.Slug == "" {
		baseSlug := strings.ReplaceAll(strings.ToLower(client.ClientName), " ", "-")
		client.Slug = fmt.Sprintf("%s-%s", baseSlug, client.UniqueID)
	}

	if err := database.DB.Create(&client).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{
			"success": false,
			"message": err.Error(),
		})
	}

	return c.Status(201).JSON(fiber.Map{
		"success": true,
		"data":    client,
	})
}

// Update a client
func UpdateClient(c *fiber.Ctx) error {
	id := c.Params("id")
	var client models.Client

	if err := database.DB.First(&client, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{
			"success": false,
			"message": "Client not found",
		})
	}

	// Isolasi demo: pastikan user hanya bisa akses data yang sesuai
	if client.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{
			"success": false,
			"message": "Akses ditolak.",
		})
	}

	if err := c.BodyParser(&client); err != nil {
		return c.Status(400).JSON(fiber.Map{
			"success": false,
			"message": "Cannot parse JSON",
		})
	}

	// Selalu pin IsDemo ke nilai asli — cegah BodyParser menimpa field ini
	// (user tidak boleh memindahkan data demo ke produksi atau sebaliknya)
	client.IsDemo = IsDemoUser(c)
	client.LastUpdated = time.Now()

	database.DB.Save(&client)
	return c.JSON(fiber.Map{
		"success": true,
		"data":    client,
	})
}

// Delete a client
func DeleteClient(c *fiber.Ctx) error {
	id := c.Params("id")
	var client models.Client

	if err := database.DB.First(&client, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{
			"success": false,
			"message": "Client not found",
		})
	}

	// Isolasi demo: pastikan user hanya bisa akses data yang sesuai
	if client.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{
			"success": false,
			"message": "Akses ditolak.",
		})
	}

	database.DB.Delete(&client)
	return c.JSON(fiber.Map{
		"success": true,
		"message": "Client successfully deleted",
	})
}

// GetClientSummary calculates total arrears and deposit balance for a client
func GetClientSummary(c *fiber.Ctx) error {
	id := c.Params("id")
	var client models.Client
	if err := database.DB.First(&client, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Client not found"})
	}

	// Isolasi demo
	if client.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak."})
	}

	var invoices []models.Invoice
	database.DB.Where("client_id = ? AND status != ?", client.ID, "PAID").Find(&invoices)

	totalTunggakan := 0.0
	unpaidCount := 0

	for _, inv := range invoices {
		// Calculate total invoice
		var products []models.Product
		database.DB.Where("invoice_id = ?", inv.ID).Find(&products)
		totalInv := 0.0
		for _, p := range products {
			if p.IsKhusus {
				totalInv += p.Quantity * p.JumlahLusin * p.HargaBeli
			} else {
				totalInv += p.Quantity * p.JumlahLusin * p.Price
			}
		}

		// Calculate total paid
		var payments []models.InvoicePayment
		database.DB.Where("invoice_id = ?", inv.ID).Find(&payments)
		totalPaid := 0.0
		for _, p := range payments {
			totalPaid += float64(p.PaymentAmount)
		}

		sisa := totalInv - totalPaid
		if sisa > 0 {
			totalTunggakan += sisa
			unpaidCount++
		}
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data": fiber.Map{
			"client_name":     client.ClientName,
			"total_tunggakan": totalTunggakan,
			"unpaid_count":    unpaidCount,
			"saldo_borongan":  client.SaldoTitipan,
		},
	})
}
