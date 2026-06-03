package routes

import (
	"invoice-bmp-go/controllers"
	"invoice-bmp-go/middleware"

	"github.com/gofiber/fiber/v2"
)

func SetupRoutes(app *fiber.App) {
	api := app.Group("/api")

	// Auth Route (Public - Bebas Akses)
	api.Post("/login", controllers.Login)
	api.Get("/files/*", controllers.GetFile)
	api.Get("/images/:filename", controllers.GetImageBase64)

	// Bonus Routes (Public - Diakses karyawan via HP)
	api.Post("/bonus/verify-pin", controllers.VerifyBonusPIN)
	api.Post("/bonus/claim", controllers.ClaimBonus)
	api.Get("/bonus/pin-list", controllers.GetEmployeePINList)

	// ADMS Routes (Public - Dipanggil oleh Mesin Fingerprint)
	// Mesin ZKTeco/Solution biasanya memanggil endpoint tanpa prefix /api
	app.Get("/iclock/cdata", controllers.AdmsCdata)
	app.Post("/iclock/cdata", controllers.AdmsCdata)
	app.Get("/iclock/getrequest", controllers.AdmsGetRequest)

	// ==========================================
	// Protected Routes Middleware (Gembok JWT)
	// Semua rute di bawah ini wajib pakai Token
	// ==========================================
	api.Use(middleware.Protected())

	// ==========================================
	// Demo Isolation Middleware (Lapisan 2)
	// Blokir semua POST/PUT/DELETE dari demouser
	// ke data produksi secara global.
	// Controller individual juga punya pengecekan
	// is_demo pada setiap operasi data.
	// ==========================================
	api.Use(middleware.RestrictDemo())

	// Payroll history — blokir demouser TOTAL (data gaji sensitif produksi)
	api.Get("/payroll/history", middleware.BlockForDemo(), controllers.GetPayrollHistory)
	api.Delete("/payroll/history/:id", middleware.BlockForDemo(), controllers.DeletePayrollHistory)

	// Upload Route — blokir demouser (jangan boros quota Cloudinary produksi)
	api.Post("/upload", middleware.BlockForDemo(), controllers.UploadFile)

	// Dashboard Routes
	api.Get("/dashboard", controllers.GetDashboardSummary)

	// Settings Route
	api.Get("/settings", controllers.GetSettings)
	api.Put("/settings", controllers.UpdateSettings)

	// Reports & Pricelist
	api.Get("/hpp-calculator", controllers.GetHppCalculator)
	api.Get("/pricelist", controllers.GetPricelist)
	api.Get("/pricelist/pdf", controllers.GeneratePricelistProductPDF)

	// Client Routes
	clientRoute := api.Group("/clients")
	clientRoute.Get("/", controllers.GetClients)
	clientRoute.Get("/:id/summary", controllers.GetClientSummary)
	clientRoute.Get("/:id", controllers.GetClient)
	clientRoute.Post("/", controllers.CreateClient)
	clientRoute.Put("/:id", controllers.UpdateClient)
	clientRoute.Delete("/:id", controllers.DeleteClient)

	// Master Product Routes
	productRoute := api.Group("/products")
	productRoute.Get("/", controllers.GetProducts)
	productRoute.Get("/:id", controllers.GetProduct)
	productRoute.Post("/", controllers.CreateProduct)
	productRoute.Put("/:id", controllers.UpdateProduct)
	productRoute.Delete("/:id", controllers.DeleteProduct)

	// Invoice Routes
	invoiceRoute := api.Group("/invoices")
	invoiceRoute.Get("/", controllers.GetInvoices)
	invoiceRoute.Post("/sync-overdue", controllers.SyncOverdueInvoices)
	invoiceRoute.Post("/pay-massal", controllers.PayMassal)
	invoiceRoute.Get("/:id/pdf", controllers.GenerateInvoicePDF)
	invoiceRoute.Get("/:id/surat-jalan", controllers.GenerateSuratJalanPDF)
	invoiceRoute.Get("/:id", controllers.GetInvoice)
	invoiceRoute.Post("/", controllers.CreateInvoice)
	invoiceRoute.Post("/:id/pay", controllers.PaySingleInvoice)
	invoiceRoute.Put("/payments/:paymentId", controllers.EditPayment)
	invoiceRoute.Delete("/payments/:paymentId", controllers.DeletePayment)
	invoiceRoute.Put("/:id/products", controllers.UpdateInvoiceProducts)
	invoiceRoute.Delete("/:id", controllers.DeleteInvoice)

	// Kas (CashFlow) Routes
	// audit & cleanup — blokir demouser TOTAL (hanya untuk admin produksi)
	api.Get("/kas/audit", middleware.BlockForDemo(), controllers.GetAuditReport)
	api.Post("/kas/cleanup-orphan-payments", middleware.BlockForDemo(), controllers.CleanupOrphanPayments)
	api.Get("/kas/export/csv", middleware.BlockForDemo(), controllers.DownloadCashFlowCSV)
	kasRoute := api.Group("/kas")
	kasRoute.Get("/", controllers.GetCashFlows)
	kasRoute.Post("/sync", controllers.SyncKas)
	kasRoute.Post("/", controllers.CreateCashFlow)
	kasRoute.Put("/:id", controllers.UpdateCashFlow)
	kasRoute.Delete("/:id", controllers.DeleteCashFlow)

	// Bahan Nono Routes
	nonoRoute := api.Group("/bahan-nono")
	nonoRoute.Get("/", controllers.GetBahanNono)
	nonoRoute.Post("/", controllers.CreateBahanNono)
	nonoRoute.Put("/:id", controllers.UpdateBahanNono)
	nonoRoute.Delete("/:id", controllers.DeleteBahanNono)

	// Payroll Routes
	payrollRoute := api.Group("/payroll")
	payrollRoute.Get("/employees", controllers.GetEmployees)
	payrollRoute.Post("/employees", controllers.CreateEmployee)
	payrollRoute.Put("/employees/:id", controllers.UpdateEmployee)
	payrollRoute.Delete("/employees/:id", controllers.DeleteEmployee)
	payrollRoute.Post("/pay", controllers.RecordPayroll)
	payrollRoute.Get("/pdf", controllers.GeneratePayrollPDF)
	payrollRoute.Get("/attendance-logs", controllers.GetAttendanceLogs)
	payrollRoute.Post("/attendance-logs", controllers.CreateAttendanceLog)
	payrollRoute.Delete("/attendance-logs/:id", controllers.DeleteAttendanceLog)
	payrollRoute.Put("/attendance-logs/:id", controllers.UpdateAttendanceLog)
	payrollRoute.Post("/attendance-logs/reason", controllers.SaveAttendanceReason)

	// Bonus Admin Routes (Protected)
	// blokir demouser dari melihat log bonus dan data bonus karyawan produksi
	bonusRoute := api.Group("/bonus")
	bonusRoute.Get("/logs", middleware.BlockForDemo(), controllers.GetBonusLogs)
	bonusRoute.Get("/employee/:id", middleware.BlockForDemo(), controllers.GetBonusByEmployee)
}
