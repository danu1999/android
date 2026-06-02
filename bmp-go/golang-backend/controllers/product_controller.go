package controllers

import (
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/google/uuid"
)

// Get all master products
func GetProducts(c *fiber.Ctx) error {
	var products []models.MasterProduct
	database.DB.Where("is_demo = ?", IsDemoUser(c)).Find(&products)
	return c.JSON(fiber.Map{
		"success": true,
		"data":    products,
	})
}

// Get single master product
func GetProduct(c *fiber.Ctx) error {
	id := c.Params("id")
	var product models.MasterProduct
	
	if err := database.DB.First(&product, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{
			"success": false,
			"message": "Product not found",
		})
	}

	if product.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{
			"success": false,
			"message": "Akses ditolak.",
		})
	}
	
	return c.JSON(fiber.Map{
		"success": true,
		"data":    product,
	})
}

// Create a master product
func CreateProduct(c *fiber.Ctx) error {
	product := new(models.MasterProduct)
	
	if err := c.BodyParser(product); err != nil {
		return c.Status(400).JSON(fiber.Map{
			"success": false,
			"message": "Cannot parse JSON",
		})
	}
	
	product.IsDemo = IsDemoUser(c)
	product.DateCreated = time.Now()
	product.LastUpdated = time.Now()
	product.UniqueID = uuid.New().String()[:8]
	product.Slug = product.Title + "-" + product.UniqueID

	if err := database.DB.Create(&product).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{
			"success": false,
			"message": err.Error(),
		})
	}
	
	return c.Status(201).JSON(fiber.Map{
		"success": true,
		"data":    product,
	})
}

// Update a master product
func UpdateProduct(c *fiber.Ctx) error {
	id := c.Params("id")
	var product models.MasterProduct

	if err := database.DB.First(&product, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{
			"success": false,
			"message": "Product not found",
		})
	}

	if product.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{
			"success": false,
			"message": "Akses ditolak.",
		})
	}

	type UpdateInput struct {
		Title     string  `json:"Title"`
		Unit      string  `json:"Unit"`
		Price     float64 `json:"Price"`
		BeratGram float64 `json:"BeratGram"`
		CycleTime float64 `json:"CycleTime"`
	}
	var input UpdateInput
	if err := c.BodyParser(&input); err != nil {
		return c.Status(400).JSON(fiber.Map{
			"success": false,
			"message": "Cannot parse JSON",
		})
	}

	if err := database.DB.Model(&product).Updates(map[string]interface{}{
		"title":       input.Title,
		"unit":        input.Unit,
		"price":       input.Price,
		"berat_gram":  input.BeratGram,
		"cycle_time":  input.CycleTime,
		"last_updated": time.Now(),
	}).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{
			"success": false,
			"message": err.Error(),
		})
	}

	database.DB.First(&product, id)
	return c.JSON(fiber.Map{
		"success": true,
		"data":    product,
	})
}

// Delete a master product
func DeleteProduct(c *fiber.Ctx) error {
	id := c.Params("id")
	var product models.MasterProduct
	
	if err := database.DB.First(&product, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{
			"success": false,
			"message": "Product not found",
		})
	}

	if product.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{
			"success": false,
			"message": "Akses ditolak.",
		})
	}
	
	database.DB.Delete(&product)
	return c.JSON(fiber.Map{
		"success": true,
		"message": "Product successfully deleted",
	})
}
