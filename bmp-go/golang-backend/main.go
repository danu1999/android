package main

import (
	"log"
	"os"

	"invoice-bmp-go/controllers"
	"invoice-bmp-go/database"
	"invoice-bmp-go/routes"

	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/middleware/cors"
	"github.com/gofiber/fiber/v2/middleware/logger"
	"github.com/joho/godotenv"
)

func main() {
	// Load .env file
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found, using system environment variables")
	}

	// Initialize Database connection and auto migration
	database.ConnectDB()

	// Create default admin user if not exists
	controllers.CreateAdminUser()

	// Run database audit on startup to print initial anomalies
	controllers.AuditDatabaseTerminal()

	// Initialize Fiber app
	app := fiber.New()

	// Enable Logger to see all incoming requests
	app.Use(logger.New())

	// Enable CORS for all origins
	app.Use(cors.New(cors.Config{
		AllowOrigins: "*",
		AllowHeaders: "Origin, Content-Type, Accept, Authorization",
		AllowMethods: "GET, POST, PUT, DELETE, OPTIONS",
	}))

	// ==========================================
	// 1. FRONTEND UTAMA (HARUS DI ATAS API)
	// ==========================================
	// Layani assets (JS/CSS dengan content-hash) dengan cache panjang
	app.Static("/assets", "./public/assets", fiber.Static{
		MaxAge: 31536000, // 1 tahun — aman karena nama file pakai hash
	})
	// Layani file statis lainnya (gambar, favicon, dll) tanpa cache
	app.Static("/", "./public", fiber.Static{
		MaxAge:   0,
		Browse:   false,
	})

	// ==========================================
	// 2. SETUP API ROUTES
	// ==========================================
	routes.SetupRoutes(app)

	// ==========================================
	// 3. FALLBACK SPA (HARUS PALING BAWAH)
	// ==========================================
	// Menangani rute di browser seperti /kas, /login, dll agar memuat index.html.
	app.Get("/*", func(c *fiber.Ctx) error {
		// Jangan cache index.html — paksa browser ambil versi terbaru
		c.Set("Cache-Control", "no-cache, no-store, must-revalidate")
		c.Set("Pragma", "no-cache")
		c.Set("Expires", "0")
		return c.SendFile("./public/index.html")
	})

	log.Println("Server is starting...")
	port := os.Getenv("PORT")
	if port == "" {
		port = "3000"
	}
	log.Fatal(app.Listen(":" + port))
}
