package controllers

import (
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"time"

	"github.com/gofiber/fiber/v2"
)

// Daftar mesin dan bonus per shift
var machineBonus = map[string]float64{
	"Baskom Panda":    5000,
	"Baskom Mawar":    5000,
	"Baskom Jago":     7000,
	"Baskom Smile 12": 7000,
	"Bak Kuping":      8000,
	"Wakul Telor":     5000,
	"Baskom Durian":   5000,
	"Wakul Moris":     5000,
	"Bahtera TM":      5000,
	"BMP":             5000,
}

// VerifyPIN - Cek PIN fingerprint dan kembalikan data karyawan
func VerifyBonusPIN(c *fiber.Ctx) error {
	type Input struct {
		PIN string `json:"pin"`
	}
	var input Input
	if err := c.BodyParser(&input); err != nil || input.PIN == "" {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "PIN tidak boleh kosong"})
	}

	var employee models.Employee
	if err := database.DB.Where("fingerprint_pin = ?", input.PIN).First(&employee).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "PIN tidak ditemukan. Cek kembali PIN Anda."})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data": fiber.Map{
			"id":   employee.ID,
			"name": employee.Name,
		},
	})
}

// ClaimBonus - Karyawan klaim bonus mesin
func ClaimBonus(c *fiber.Ctx) error {
	type Input struct {
		PIN             string `json:"pin"`
		MachineName     string `json:"machine_name"`
		ShiftType       string `json:"shift_type"`
		JumlahPerolehan int    `json:"jumlah_perolehan"`
	}
	var input Input
	if err := c.BodyParser(&input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Input tidak valid"})
	}

	// 1. Verifikasi PIN
	var employee models.Employee
	if err := database.DB.Where("fingerprint_pin = ?", input.PIN).First(&employee).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "PIN tidak ditemukan"})
	}

	// 2. Cek mesin valid
	bonus, ok := machineBonus[input.MachineName]
	if !ok {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Nama mesin tidak valid"})
	}

	// 3. Cek shift valid
	validShifts := map[string]bool{"Pagi": true, "Siang": true, "Sore": true, "Malam": true}
	if !validShifts[input.ShiftType] {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Shift tidak valid"})
	}

	// 4. Cek duplikat: 1 karyawan hanya boleh 1 klaim per hari (apapun mesinnya)
	today := time.Now().Format("2006-01-02")
	var existing models.MachineBonusLog
	if err := database.DB.Where(
		"employee_id = ? AND date = ?",
		employee.ID, today,
	).First(&existing).Error; err == nil {
		return c.Status(409).JSON(fiber.Map{
			"success": false,
			"message": "PIN ini sudah digunakan untuk klaim bonus lereno !! (" + existing.MachineName + ") hari ini. 1 PIN hanya bisa 1x klaim per hari.",
		})
	}

	// 5. Simpan bonus
	tanggal, _ := time.Parse("2006-01-02", today)
	log := models.MachineBonusLog{
		EmployeeID:      employee.ID,
		MachineName:     input.MachineName,
		ShiftType:       input.ShiftType,
		BonusAmount:     bonus,
		JumlahPerolehan: input.JumlahPerolehan,
		Date:            tanggal,
		IsDemo:          employee.IsDemo,
	}
	if err := database.DB.Create(&log).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan bonus"})
	}

	return c.Status(201).JSON(fiber.Map{
		"success": true,
		"message": "Bonus berhasil diklaim!",
		"data": fiber.Map{
			"employee": employee.Name,
			"machine":  input.MachineName,
			"shift":    input.ShiftType,
			"bonus":    bonus,
		},
	})
}

// GetBonusLogs - Admin lihat semua log bonus (bisa filter minggu ini)
func GetBonusLogs(c *fiber.Ctx) error {
	var logs []models.MachineBonusLog

	// Filter minggu ini secara default
	now := time.Now()
	dayOfWeek := int(now.Weekday())
	if dayOfWeek == 0 {
		dayOfWeek = 7
	}
	startOfWeek := now.AddDate(0, 0, -(dayOfWeek - 1))
	startOfWeek = time.Date(startOfWeek.Year(), startOfWeek.Month(), startOfWeek.Day(), 0, 0, 0, 0, startOfWeek.Location())

	isDemo := IsDemoUser(c)
	if err := database.DB.Preload("Employee").Where("is_demo = ? AND date >= ?", isDemo, startOfWeek).Order("date desc, id desc").Find(&logs).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal mengambil data bonus"})
	}

	// Hitung total per karyawan
	totals := map[uint]float64{}
	for _, l := range logs {
		totals[l.EmployeeID] += l.BonusAmount
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    logs,
		"totals":  totals,
	})
}

// GetBonusByEmployee - Ambil total bonus karyawan tertentu minggu ini (untuk slip gaji)
func GetBonusByEmployee(c *fiber.Ctx) error {
	employeeID := c.Params("id")
	isDemo := IsDemoUser(c)

	var employee models.Employee
	if err := database.DB.First(&employee, employeeID).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Karyawan tidak ditemukan"})
	}
	if employee.IsDemo != isDemo {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	now := time.Now()
	dayOfWeek := int(now.Weekday())
	if dayOfWeek == 0 {
		dayOfWeek = 7
	}
	startOfWeek := now.AddDate(0, 0, -(dayOfWeek - 1))
	startOfWeek = time.Date(startOfWeek.Year(), startOfWeek.Month(), startOfWeek.Day(), 0, 0, 0, 0, startOfWeek.Location())

	var logs []models.MachineBonusLog
	database.DB.Where("employee_id = ? AND date >= ? AND is_demo = ?", employeeID, startOfWeek, isDemo).Find(&logs)

	total := 0.0
	for _, l := range logs {
		total += l.BonusAmount
	}

	return c.JSON(fiber.Map{
		"success": true,
		"data":    logs,
		"total":   total,
	})
}

// GetEmployeePINList - Tampilkan daftar PIN + Nama karyawan (untuk referensi di halaman bonus)
func GetEmployeePINList(c *fiber.Ctx) error {
	var employees []models.Employee
	if err := database.DB.
		Where("deleted_at IS NULL AND fingerprint_pin IS NOT NULL AND fingerprint_pin != '' AND LOWER(name) != 'muizz' AND is_demo = ?", IsDemoUser(c)).
		Order("name asc").
		Find(&employees).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false})
	}

	type PINRow struct {
		PIN  string `json:"pin"`
		Name string `json:"name"`
	}
	var rows []PINRow
	for _, e := range employees {
		rows = append(rows, PINRow{PIN: e.FingerprintPIN, Name: e.Name})
	}
	return c.JSON(fiber.Map{"success": true, "data": rows, "total": len(rows)})
}
