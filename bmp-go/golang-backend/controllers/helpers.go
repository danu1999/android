package controllers

import (
	"os"

	"github.com/gofiber/fiber/v2"
	"github.com/golang-jwt/jwt/v5"
)

// IsDemoUser extracts the is_demo claim from the JWT token stored in fiber context.
// Returns true if the current request is made by a demo user.
// Digunakan di PROTECTED routes (butuh JWT).
func IsDemoUser(c *fiber.Ctx) bool {
	userToken, ok := c.Locals("user").(*jwt.Token)
	if !ok {
		return false
	}
	claims, ok := userToken.Claims.(jwt.MapClaims)
	if !ok {
		return false
	}
	isDemo, _ := claims["is_demo"].(bool)
	return isDemo
}

// IsDemoEnv membaca environment variable DEMO_MODE untuk menentukan apakah server
// ini adalah instance demo (bmp-backend-demo) atau produksi (bmp-backend).
// Digunakan di PUBLIC routes yang tidak memiliki JWT (bonus kiosk, fingerprint, dll).
// bmp-backend-demo dijalankan dengan env DEMO_MODE=true,
// bmp-backend (produksi) dijalankan tanpa DEMO_MODE atau DEMO_MODE=false.
func IsDemoEnv() bool {
	return os.Getenv("DEMO_MODE") == "true"
}
