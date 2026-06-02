package controllers

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"html/template"
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"log"
	"net/http"
	"os"
	"strings"
	"time"

	"github.com/SebastiaanKlippert/go-wkhtmltopdf"
	"github.com/gofiber/fiber/v2"
	"github.com/golang-jwt/jwt/v5"
	"github.com/skip2/go-qrcode"
)

// Helper Base64 functions
func getLogoBase64(_ string) template.URL {
	// Dikosongkan agar PDF tidak pernah menampilkan Logo
	return ""
}

func getTTDBase64() template.URL {
	// MENCARI TANDA TANGAN DI RUMAH BARU (public/images)
	paths := []string{
		"./public/images/signature.jpeg",    // Lokal (jika run dari root)
		"../public/images/signature.jpeg",   // Lokal (jika run dari dalam folder)
		"/app/public/images/signature.jpeg", // Lokasi pasti di Railway
		"./static/signature.jpeg",           // Fallback (cadangan terakhir)
	}

	for _, path := range paths {
		data, err := os.ReadFile(path)
		if err == nil {
			return template.URL("data:image/jpeg;base64," + base64.StdEncoding.EncodeToString(data))
		}
	}

	log.Println("❌ ERROR PDF: File signature.jpeg tidak ditemukan di semua path!")
	return ""
}

// GetImageBase64 melayani gambar (logo/signature) sebagai data URI base64
// agar html2canvas bisa memuatnya tanpa masalah CORS
func GetImageBase64(c *fiber.Ctx) error {
	filename := c.Params("filename")

	if filename != "logo.jpg" && filename != "signature.jpeg" {
		return c.Status(fiber.StatusNotFound).JSON(fiber.Map{"error": "not found"})
	}

	paths := []string{
		"./public/images/" + filename,
		"../public/images/" + filename,
		"/app/public/images/" + filename,
		"./static/" + filename,
	}

	for _, path := range paths {
		data, err := os.ReadFile(path)
		if err == nil {
			mimeType := http.DetectContentType(data)
			encoded := base64.StdEncoding.EncodeToString(data)
			return c.JSON(fiber.Map{
				"data": "data:" + mimeType + ";base64," + encoded,
			})
		}
	}

	return c.Status(fiber.StatusNotFound).JSON(fiber.Map{"error": "file not found"})
}

func getQRBase64(text string) template.URL {
	png, err := qrcode.Encode(text, qrcode.Medium, 256)
	if err == nil {
		return template.URL("data:image/png;base64," + base64.StdEncoding.EncodeToString(png))
	}
	return ""
}

func isDemoUser(c *fiber.Ctx) bool {
	if os.Getenv("DEMO_MODE") == "true" {
		return true
	}
	userToken, ok := c.Locals("user").(*jwt.Token)
	if ok {
		claims, ok := userToken.Claims.(jwt.MapClaims)
		if ok {
			if isDemo, ok := claims["is_demo"].(bool); ok && isDemo {
				return true
			}
		}
	}
	return false
}

type PDFData struct {
	Invoice     models.Invoice
	Products    []models.Product
	Settings    models.Settings
	LogoBase64  template.URL
	TTDBase64   template.URL
	QRBase64    template.URL
	Subtotal    float64
	SisaTagihan float64
	DemoMode    bool
}

type PayrollPDFData struct {
	Employees []PayrollSlipData
}

type PayrollSlipData struct {
	Payroll     models.Payroll
	BonusLogs   []models.MachineBonusLog
	TotalBonus  float64
	TotalGaji   float64
}

func formatRp(amount float64) string {
	s := fmt.Sprintf("%.0f", amount)
	var result []string
	for i := len(s) - 1; i >= 0; i-- {
		result = append([]string{string(s[i])}, result...)
		if (len(s)-i)%3 == 0 && i != 0 {
			result = append([]string{"."}, result...)
		}
	}
	return "Rp" + strings.Join(result, "")
}

func formatRpComma(amount float64) string {
	s := fmt.Sprintf("%.0f", amount)
	var result []string
	for i := len(s) - 1; i >= 0; i-- {
		result = append([]string{string(s[i])}, result...)
		if (len(s)-i)%3 == 0 && i != 0 {
			result = append([]string{"."}, result...)
		}
	}
	return strings.Join(result, "")
}

func formatDate(t interface{}) string {
	switch v := t.(type) {
	case time.Time:
		if v.IsZero() {
			return "-"
		}
		return v.Format("02/01/2006")
	case *time.Time:
		if v == nil || v.IsZero() {
			return "-"
		}
		return v.Format("02/01/2006")
	default:
		return "-"
	}
}

func generatePDFBytes(tmplName string, data PDFData) ([]byte, error) {
	funcMap := template.FuncMap{
		"add":        func(a, b int) int { return a + b },
		"formatRp":   formatRp,
		"intcomma":   formatRpComma,
		"formatDate": formatDate,
		"mul3":       func(a, b, c float64) float64 { return a * b * c },
		"float64": func(v interface{}) float64 {
			switch i := v.(type) {
			case int:
				return float64(i)
			case int64:
				return float64(i)
			case float64:
				return i
			default:
				return 0
			}
		},
	}

	tmpl, err := template.New(tmplName).Funcs(funcMap).ParseFiles("templates/" + tmplName)
	if err != nil {
		return nil, err
	}

	var htmlBuf bytes.Buffer
	if err := tmpl.ExecuteTemplate(&htmlBuf, tmplName, data); err != nil {
		return nil, err
	}

	pdfg, err := wkhtmltopdf.NewPDFGenerator()
	if err != nil {
		return nil, err
	}

	// Remove PageSize when using custom width/height
	pdfg.PageWidth.Set(241)   // 9.5 inch
	pdfg.PageHeight.Set(279)  // 11 inch
	pdfg.MarginTop.Set(5)     // 0.5 cm
	pdfg.MarginRight.Set(10)  // 1 cm
	pdfg.MarginBottom.Set(10) // 1 cm
	pdfg.MarginLeft.Set(10)   // 1 cm

	page := wkhtmltopdf.NewPageReader(bytes.NewReader(htmlBuf.Bytes()))
	pdfg.AddPage(page)

	if err := pdfg.Create(); err != nil {
		return nil, err
	}

	return pdfg.Bytes(), nil
}

func GenerateInvoicePDF(c *fiber.Ctx) error {
	id := c.Params("id")
	var invoice models.Invoice
	if err := database.DB.Preload("Client").Preload("Payments").First(&invoice, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Invoice not found"})
	}

	isDemo := IsDemoUser(c)
	if invoice.IsDemo != isDemo {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	var products []models.Product
	database.DB.Where("invoice_id = ?", invoice.ID).Find(&products)

	var settings models.Settings
	database.DB.Where("is_demo = ?", isDemo).First(&settings)

	subtotal := 0.0
	for _, p := range products {
		subtotal += p.Quantity * p.JumlahLusin * p.Price
	}

	totalPaid := 0.0
	for _, p := range invoice.Payments {
		totalPaid += float64(p.PaymentAmount)
	}

	data := PDFData{
		Invoice:     invoice,
		Products:    products,
		Settings:    settings,
		LogoBase64:  getLogoBase64(settings.ClientLogo),
		TTDBase64:   getTTDBase64(),
		QRBase64:    getQRBase64("INV:" + invoice.Number),
		Subtotal:    subtotal,
		SisaTagihan: subtotal - totalPaid,
		DemoMode:    isDemoUser(c),
	}

	pdfBytes, err := generatePDFBytes("invoice-pdf.html", data)
	if err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "PDF Error: " + err.Error()})
	}

	c.Set("Content-Type", "application/pdf")
	c.Set("Content-Disposition", `inline; filename="invoice-`+invoice.Number+`.pdf"`)
	return c.Send(pdfBytes)
}

func GenerateSuratJalanPDF(c *fiber.Ctx) error {
	id := c.Params("id")
	var invoice models.Invoice
	if err := database.DB.Preload("Client").First(&invoice, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Invoice not found"})
	}

	isDemo := IsDemoUser(c)
	if invoice.IsDemo != isDemo {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	var products []models.Product
	database.DB.Where("invoice_id = ?", invoice.ID).Find(&products)

	var settings models.Settings
	database.DB.Where("is_demo = ?", isDemo).First(&settings)

	subtotal := 0.0
	for _, p := range products {
		subtotal += p.Quantity * p.JumlahLusin * p.Price
	}

	data := PDFData{
		Invoice:    invoice,
		Products:   products,
		Settings:   settings,
		LogoBase64: getLogoBase64(settings.ClientLogo),
		TTDBase64:  getTTDBase64(),
		QRBase64:   getQRBase64("SJ:" + invoice.Number),
		Subtotal:   subtotal,
		DemoMode:   isDemoUser(c),
	}

	pdfBytes, err := generatePDFBytes("surat-jalan-pdf.html", data)
	if err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "PDF Error: " + err.Error()})
	}

	c.Set("Content-Type", "application/pdf")
	c.Set("Content-Disposition", `inline; filename="SJ-`+invoice.Number+`.pdf"`)
	return c.Send(pdfBytes)
}

func GeneratePayrollPDF(c *fiber.Ctx) error {
	tokenString := c.Query("token")
	if tokenString == "" {
		return c.Status(401).JSON(fiber.Map{"success": false, "message": "Missing token"})
	}

	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		return JwtSecret, nil
	})
	if err != nil {
		return c.Status(401).JSON(fiber.Map{"success": false, "message": "Invalid token"})
	}

	var isDemo bool
	if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
		isDemo, _ = claims["is_demo"].(bool)
	}

	idsStr := c.Query("ids")
	var payrolls []models.Payroll

	if idsStr != "" {
		ids := strings.Split(idsStr, ",")
		database.DB.Preload("Employee").Where("employee_id IN ? AND is_demo = ?", ids, isDemo).Order("id desc").Limit(len(ids)).Find(&payrolls)
	} else {
		database.DB.Preload("Employee").Where("is_demo = ?", isDemo).Find(&payrolls)
	}

	// Hitung range minggu ini untuk fetch bonus
	now := time.Now()
	dayOfWeek := int(now.Weekday())
	if dayOfWeek == 0 {
		dayOfWeek = 7
	}
	startOfWeek := now.AddDate(0, 0, -(dayOfWeek - 1))
	startOfWeek = time.Date(startOfWeek.Year(), startOfWeek.Month(), startOfWeek.Day(), 0, 0, 0, 0, startOfWeek.Location())

	// Build slip data dengan bonus
	var slipData []PayrollSlipData
	for _, p := range payrolls {
		var bonusLogs []models.MachineBonusLog
		database.DB.Where("employee_id = ? AND date >= ? AND is_demo = ?", p.EmployeeID, startOfWeek, isDemo).Find(&bonusLogs)

		totalBonus := 0.0
		for _, b := range bonusLogs {
			totalBonus += b.BonusAmount
		}

		slipData = append(slipData, PayrollSlipData{
			Payroll:    p,
			BonusLogs:  bonusLogs,
			TotalBonus: totalBonus,
			TotalGaji:  p.Amount + totalBonus,
		})
	}

	funcMap := template.FuncMap{
		"formatRp": formatRp,
	}

	tmpl, err := template.New("payroll-pdf.html").Funcs(funcMap).ParseFiles("templates/payroll-pdf.html")
	if err != nil {
		return c.Status(500).JSON(fiber.Map{"error": err.Error()})
	}

	var htmlBuf bytes.Buffer
	if err := tmpl.Execute(&htmlBuf, PayrollPDFData{Employees: slipData}); err != nil {
		return c.Status(500).JSON(fiber.Map{"error": err.Error()})
	}

	pdfg, _ := wkhtmltopdf.NewPDFGenerator()
	pdfg.PageWidth.Set(241)
	pdfg.PageHeight.Set(279)
	pdfg.AddPage(wkhtmltopdf.NewPageReader(bytes.NewReader(htmlBuf.Bytes())))

	if err := pdfg.Create(); err != nil {
		return c.Status(500).JSON(fiber.Map{"error": err.Error()})
	}

	c.Set("Content-Type", "application/pdf")
	return c.Send(pdfg.Bytes())
}
