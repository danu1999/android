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
	// Layani file statis (CSS, JS, logo, dll) dari folder "public"
	// Daftarkan folder static agar bisa diakses oleh frontend (JPG)
	app.Static("/static", "../static") // Untuk lingkungan lokal
	app.Static("/static", "./static")  // Untuk lingkungan Railway
	app.Static("/", "./public")

	// ==========================================
	// 2. SETUP API ROUTES
	// ==========================================
	routes.SetupRoutes(app)

	// ==========================================
	// 3. FALLBACK SPA (HARUS PALING BAWAH)
	// ==========================================
	// Menangani rute di browser seperti /kas, /login, dll agar memuat index.html.
	app.Get("/*", func(c *fiber.Ctx) error {
		return c.SendFile("./public/index.html")
	})

	log.Println("Server is starting...")
	port := os.Getenv("PORT")
	if port == "" {
		port = "3000"
	}
	log.Fatal(app.Listen(":" + port))
}
