package controllers

import (
	"bytes"
	"html/template"
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"sort"
	"time"

	"github.com/SebastiaanKlippert/go-wkhtmltopdf"
	"github.com/gofiber/fiber/v2"
	"github.com/golang-jwt/jwt/v5"
)

func GetHppCalculator(c *fiber.Ctx) error {
	isDemo := IsDemoUser(c)
	var settings models.Settings
	database.DB.Where("is_demo = ?", isDemo).First(&settings)

	var products []models.MasterProduct
	database.DB.Where("is_demo = ?", isDemo).Find(&products)

	var latestBahan models.BahanNonoItem
	database.DB.Joins("JOIN bahan_nonos ON bahan_nonos.id = bahan_nono_items.bahan_nono_id").
		Where("rate > 0 AND bahan_nonos.is_demo = ?", isDemo).
		Order("bahan_nono_items.id desc").First(&latestBahan)
	defaultHargaBahan := latestBahan.Rate
	if defaultHargaBahan == 0 {
		defaultHargaBahan = 8000
	}

	simListrik := c.QueryFloat("listrik", settings.ListrikBulanan)
	simGaji := c.QueryFloat("gaji", settings.GajiHarian)
	simBahan := c.QueryFloat("harga_bahan", defaultHargaBahan)
	simIsiKarung := c.QueryFloat("isi_karung", 50)

	jumlahMesin := float64(settings.JumlahMesin)
	if jumlahMesin == 0 {
		jumlahMesin = 1
	}
	jumlahKaryawan := float64(settings.JumlahKaryawan)
	if jumlahKaryawan == 0 {
		jumlahKaryawan = 1
	}
	hariKerja := float64(settings.HariKerjaSebulan)
	if hariKerja == 0 {
		hariKerja = 26
	}
	hoursPerDay := float64(settings.HoursPerDay)
	if hoursPerDay == 0 {
		hoursPerDay = 24
	}

	totalJamBulan := hariKerja * hoursPerDay
	listrikJamMesin, gajiJamMesin := 0.0, 0.0
	if totalJamBulan > 0 {
		listrikJamMesin = (simListrik / jumlahMesin) / totalJamBulan
		gajiJamMesin = ((jumlahKaryawan * simGaji * hariKerja) / jumlahMesin) / totalJamBulan
	}
	overheadJam := listrikJamMesin + gajiJamMesin
	packingLsn := 0.0
	if simIsiKarung > 0 {
		packingLsn = (settings.BiayaKarungPer1000 / 1000) / simIsiKarung
	}

	var results []fiber.Map
	for _, p := range products {
		if p.CycleTime <= 0 || p.BeratGram <= 0 {
			continue
		}

		cavity := float64(p.Cavity)
		if cavity == 0 {
			cavity = 1
		}

		lusinJam := ((3600 / p.CycleTime) * cavity) / 12
		modalMesin := 0.0
		if lusinJam > 0 {
			modalMesin = overheadJam / lusinJam
		}
		modalBahan := ((p.BeratGram * 12) / 1000) * simBahan * (1 + (p.RejectRate / 100))
		totalHpp := modalBahan + modalMesin + packingLsn

		var lastSale models.Product
		database.DB.Joins("JOIN invoices ON invoices.id = products.invoice_id").
			Where("products.master_item_id = ? AND invoices.status != ? AND invoices.is_demo = ?", p.ID, "DRAFT", isDemo).
			Order("products.id desc").First(&lastSale)

		hargaJualReal := lastSale.Price
		if hargaJualReal == 0 {
			hargaJualReal = p.Price
		}

		margin := 0.0
		if hargaJualReal > 0 {
			margin = hargaJualReal - totalHpp
		}

		results = append(results, fiber.Map{
			"product": p, "output_per_jam": lusinJam, "modal_bahan": modalBahan,
			"modal_mesin": modalMesin, "total_hpp": totalHpp, "harga_jual_real": hargaJualReal, "margin": margin,
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data": fiber.Map{
			"hpp_results":          results,
			"overhead_jam":         overheadJam,
			"packing_lsn":          packingLsn,
			"settings":             settings,
			"default_harga_bahan":  defaultHargaBahan,
		},
	})
}

func GetPricelist(c *fiber.Ctx) error {
	clientId := c.QueryInt("client", 0)
	isDemo := IsDemoUser(c)
	query := database.DB.Preload("Invoice").Preload("Invoice.Client").
		Joins("JOIN invoices ON invoices.id = products.invoice_id").
		Where("invoices.status != ? AND invoices.client_id IS NOT NULL AND invoices.is_demo = ?", "DRAFT", isDemo)

	if clientId > 0 {
		query = query.Where("invoices.client_id = ?", clientId)
	}

	var items []models.Product
	query.Order("invoices.date_created desc").Find(&items)

	type HistoryItem struct {
		Tanggal string
		Harga   float64
		Qty     float64
		Lusin   float64
		Faktur  string
	}
	type PriceData struct {
		Client  string
		Item    string
		History []HistoryItem
	}

	dataHarga := make(map[string]*PriceData)
	for _, item := range items {
		if item.Invoice.ClientID == nil {
			continue
		}
		clientName := item.Invoice.Client.ClientName
		itemName := item.Title
		key := clientName + "__" + itemName

		if _, exists := dataHarga[key]; !exists {
			dataHarga[key] = &PriceData{Client: clientName, Item: itemName}
		}

		dataHarga[key].History = append(dataHarga[key].History, HistoryItem{
			Tanggal: item.Invoice.DateCreated.Format("2006-01-02"), Harga: item.Price, Qty: item.Quantity, Lusin: item.JumlahLusin, Faktur: item.Invoice.Number,
		})
	}

	var finalResult []fiber.Map
	for _, data := range dataHarga {
		if len(data.History) == 0 {
			continue
		}
		current := data.History[0]
		status, selisih := "TETAP", 0.0
		var prev *HistoryItem
		if len(data.History) > 1 {
			prev = &data.History[1]
			selisih = current.Harga - prev.Harga
			if selisih > 0 {
				status = "NAIK"
			} else if selisih < 0 {
				status = "TURUN"
				selisih = -selisih
			}
		}
		finalResult = append(finalResult, fiber.Map{"client": data.Client, "item": data.Item, "terbaru": current, "sebelumnya": prev, "status": status, "selisih": selisih})
	}

	sort.Slice(finalResult, func(i, j int) bool {
		if finalResult[i]["status"] == "TETAP" && finalResult[j]["status"] != "TETAP" {
			return false
		}
		if finalResult[i]["status"] != "TETAP" && finalResult[j]["status"] == "TETAP" {
			return true
		}
		return finalResult[i]["client"].(string) < finalResult[j]["client"].(string)
	})

	return c.JSON(fiber.Map{"success": true, "data": finalResult})
}

// GeneratePricelistProductPDF — Export PDF 1 produk 1 harga terbaru
func GeneratePricelistProductPDF(c *fiber.Ctx) error {
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

	var masterProducts []models.MasterProduct
	database.DB.Where("deleted_at IS NULL AND is_demo = ?", isDemo).Order("title asc").Find(&masterProducts)

	type ProductPriceRow struct {
		No      int
		Nama    string
		Satuan  string
		Harga   string
		Tanggal string
		Faktur  string
	}

	var rows []ProductPriceRow
	for i, mp := range masterProducts {
		var lastProduct models.Product
		errQ := database.DB.
			Joins("JOIN invoices ON invoices.id = products.invoice_id").
			Where("products.master_item_id = ? AND invoices.status != ? AND invoices.deleted_at IS NULL AND invoices.is_demo = ?", mp.ID, "DRAFT", isDemo).
			Order("products.id desc").
			First(&lastProduct).Error

		harga := "-"
		tanggal := "-"
		faktur := "-"

		if errQ == nil && lastProduct.Price > 0 {
			harga = formatRp(lastProduct.Price)
			var inv models.Invoice
			database.DB.First(&inv, lastProduct.InvoiceID)
			tanggal = inv.DateCreated.Format("02/01/2006")
			faktur = inv.Number
		} else if mp.Price > 0 {
			harga = formatRp(mp.Price)
		}

		satuan := mp.Unit
		if satuan == "" || satuan == "-" {
			satuan = "Lusin"
		}

		rows = append(rows, ProductPriceRow{
			No:      i + 1,
			Nama:    mp.Title,
			Satuan:  satuan,
			Harga:   harga,
			Tanggal: tanggal,
			Faktur:  faktur,
		})
	}

	type PricelistPDFData struct {
		Rows         []ProductPriceRow
		TanggalCetak string
	}

	data := PricelistPDFData{
		Rows:         rows,
		TanggalCetak: time.Now().Format("02/01/2006"),
	}

	tmpl, err := template.New("pricelist-pdf.html").ParseFiles("templates/pricelist-pdf.html")
	if err != nil {
		return c.Status(500).JSON(fiber.Map{"error": "Template error: " + err.Error()})
	}

	var htmlBuf bytes.Buffer
	if err := tmpl.Execute(&htmlBuf, data); err != nil {
		return c.Status(500).JSON(fiber.Map{"error": "Render error: " + err.Error()})
	}

	pdfg, _ := wkhtmltopdf.NewPDFGenerator()
	pdfg.PageWidth.Set(241)
	pdfg.PageHeight.Set(279)
	pdfg.MarginTop.Set(10)
	pdfg.MarginBottom.Set(10)
	pdfg.MarginLeft.Set(10)
	pdfg.MarginRight.Set(10)
	pdfg.AddPage(wkhtmltopdf.NewPageReader(bytes.NewReader(htmlBuf.Bytes())))

	if err := pdfg.Create(); err != nil {
		return c.Status(500).JSON(fiber.Map{"error": "PDF error: " + err.Error()})
	}

	c.Set("Content-Type", "application/pdf")
	c.Set("Content-Disposition", `inline; filename="Pricelist-Produk.pdf"`)
	return c.Send(pdfg.Bytes())
}
