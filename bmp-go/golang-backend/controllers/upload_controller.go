package controllers

import (
	"context"
	"fmt"
	"os"
	"strings"

	"github.com/cloudinary/cloudinary-go/v2"
	"github.com/cloudinary/cloudinary-go/v2/api/uploader"
	"github.com/gofiber/fiber/v2"
)

// UploadFile handles image uploads directly to Cloudinary
func UploadFile(c *fiber.Ctx) error {
	folder := c.Query("folder", "uploads")
	fileHeader, err := c.FormFile("file")
	if err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Failed to get file from form"})
	}

	// Open the file
	file, err := fileHeader.Open()
	if err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to open file"})
	}
	defer file.Close()

	// Initialize Cloudinary
	cld, err := cloudinary.NewFromURL(os.Getenv("CLOUDINARY_URL"))
	if err != nil {
		// Fallback jika CLOUDINARY_URL tidak ada, rakit manual
		cloudName := os.Getenv("CLOUDINARY_CLOUD_NAME")
		apiKey := os.Getenv("CLOUDINARY_API_KEY")
		apiSecret := os.Getenv("CLOUDINARY_API_SECRET")
		cld, err = cloudinary.NewFromParams(cloudName, apiKey, apiSecret)
		if err != nil {
			return c.Status(500).JSON(fiber.Map{"success": false, "message": "Cloudinary config error: " + err.Error()})
		}
	}

	ctx := context.Background()
	uploadRes, err := cld.Upload.Upload(ctx, file, uploader.UploadParams{
		Folder:         folder,
		Transformation: "w_1200,c_limit,q_auto:best,f_auto",
	})

	if err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": fmt.Sprintf("Upload to Cloudinary failed: %v", err)})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"message": "File uploaded to Cloudinary",
		"data": fiber.Map{
			"url":           uploadRes.SecureURL,
			"public_id":     uploadRes.PublicID,
			"thumbnail_url": GetTransformationURL(uploadRes.SecureURL, "w_200,h_200,c_fill"),
		},
	})
}

// GetFile handles redirection to Cloudinary for legacy and new paths
func GetFile(c *fiber.Ctx) error {
	path := c.Params("*")
	if path == "" {
		return c.Status(404).SendString("File not found")
	}

	// Initialize Cloudinary
	cld, err := cloudinary.NewFromURL(os.Getenv("CLOUDINARY_URL"))
	if err != nil {
		cloudName := os.Getenv("CLOUDINARY_CLOUD_NAME")
		apiKey := os.Getenv("CLOUDINARY_API_KEY")
		apiSecret := os.Getenv("CLOUDINARY_API_SECRET")
		cld, err = cloudinary.NewFromParams(cloudName, apiKey, apiSecret)
		if err != nil {
			return c.Status(500).SendString("Cloudinary config error")
		}
	}

	// Generate the URL
	cloudName := cld.Config.Cloud.CloudName
	if cloudName == "" {
		cloudName = os.Getenv("CLOUDINARY_CLOUD_NAME")
	}

	if cloudName == "" {
		return c.Status(500).SendString("Cloud Name is not configured in .env")
	}

	// Hilangkan "uploads/" dari awal jika path sudah mengandung itu (opsional, tergantung struktur Cloudinary Anda)
	// Namun data Anda tadi menunjukkan public_id termasuk "uploads/..."

	finalURL := fmt.Sprintf("https://res.cloudinary.com/%s/image/upload/%s", cloudName, path)

	// Jika file tidak punya ekstensi (sering terjadi di Django), Cloudinary butuh kita menebak atau biarkan tanpa ekstensi
	// Kita coba redirect ke URL asli dulu.
	return c.Redirect(finalURL)
}

// GetTransformationURL adalah helper untuk memanipulasi URL Cloudinary untuk konversi ukuran
func GetTransformationURL(originalURL string, transformation string) string {
	if !strings.Contains(originalURL, "cloudinary.com") {
		return originalURL
	}
	// Masukkan transformasi setelah '/upload/'
	search := "/upload/"
	replacement := fmt.Sprintf("/upload/%s/", transformation)
	return strings.Replace(originalURL, search, replacement, 1)
}
