package controllers

import (
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/google/uuid"
)

func GetSettings(c *fiber.Ctx) error {
	isDemo := IsDemoUser(c)
	var settings models.Settings
	if err := database.DB.Where("is_demo = ?", isDemo).First(&settings).Error; err != nil {
		// Auto-create default settings for demo/production mode
		clientName := "BMP - Bintang Makmur Plastindo"
		if isDemo {
			clientName = "BMP - Bintang Makmur Plastindo (DEMO)"
		}
		settings = models.Settings{
			ClientName:         clientName,
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
			IsDemo:             isDemo,
			DateCreated:        time.Now(),
			LastUpdated:        time.Now(),
		}
		if err := database.DB.Create(&settings).Error; err != nil {
			return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to create default settings"})
		}
	}
	return c.JSON(fiber.Map{"success": true, "data": settings})
}

func UpdateSettings(c *fiber.Ctx) error {
	isDemo := IsDemoUser(c)
	var settings models.Settings
	if err := database.DB.Where("is_demo = ?", isDemo).First(&settings).Error; err != nil {
		settings = models.Settings{
			DateCreated: time.Now(),
			IsDemo:      isDemo,
			UniqueID:    uuid.New().String()[:8],
			Slug:        "bmp-bintang-makmur-plastindo",
		}
	}

	if err := c.BodyParser(&settings); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input"})
	}

	// Force correct demo flag and update time
	settings.IsDemo = isDemo
	settings.LastUpdated = time.Now()
	database.DB.Save(&settings)

	return c.JSON(fiber.Map{"success": true, "message": "Settings updated", "data": settings})
}
