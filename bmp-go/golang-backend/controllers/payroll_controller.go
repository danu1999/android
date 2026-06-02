package controllers

import (
	"invoice-bmp-go/database"
	"invoice-bmp-go/models"
	"time"

	"github.com/gofiber/fiber/v2"
)

func GetEmployees(c *fiber.Ctx) error {
	var employees []models.Employee
	database.DB.Where("is_demo = ?", IsDemoUser(c)).Find(&employees)
	return c.JSON(fiber.Map{"success": true, "data": employees})
}

func CreateEmployee(c *fiber.Ctx) error {
	var employee models.Employee
	if err := c.BodyParser(&employee); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input"})
	}
	employee.IsDemo = IsDemoUser(c)
	database.DB.Create(&employee)
	return c.Status(201).JSON(fiber.Map{"success": true, "data": employee})
}

func UpdateEmployee(c *fiber.Ctx) error {
	id := c.Params("id")
	var employee models.Employee
	if err := database.DB.First(&employee, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Employee not found"})
	}
	if employee.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}
	if err := c.BodyParser(&employee); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input"})
	}
	database.DB.Save(&employee)
	return c.JSON(fiber.Map{"success": true, "data": employee})
}

func DeleteEmployee(c *fiber.Ctx) error {
	id := c.Params("id")
	var employee models.Employee
	if err := database.DB.First(&employee, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Employee not found"})
	}
	if employee.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}
	if err := database.DB.Delete(&employee).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to delete"})
	}
	return c.JSON(fiber.Map{"success": true, "message": "Deleted"})
}

func RecordPayroll(c *fiber.Ctx) error {
	type PayrollInput struct {
		EmployeeID uint    `json:"employee_id"`
		Amount     float64 `json:"amount"`
		Date       string  `json:"date"`
		Notes      string  `json:"notes"`
	}
	input := new(PayrollInput)
	if err := c.BodyParser(input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Invalid input"})
	}

	// Verify Employee exists and matches current mode
	var employee models.Employee
	if err := database.DB.First(&employee, input.EmployeeID).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Employee not found"})
	}
	if employee.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	tanggal, _ := time.Parse("2006-01-02", input.Date)

	payroll := models.Payroll{
		EmployeeID:  input.EmployeeID,
		PaymentDate: tanggal,
		Amount:      input.Amount,
		Description: input.Notes,
		IsDemo:      IsDemoUser(c),
	}

	tx := database.DB.Begin()
	if err := tx.Create(&payroll).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to record payroll"})
	}

	cashFlow := models.CashFlow{
		TransactionDate: tanggal,
		TransactionType: "KELUAR",
		Description:     "Pembayaran Gaji: " + input.Notes,
		Amount:          input.Amount,
		IsDemo:          IsDemoUser(c),
		DateCreated:     time.Now(),
	}
	if err := tx.Create(&cashFlow).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Failed to record cash flow"})
	}

	tx.Commit()
	return c.Status(201).JSON(fiber.Map{"success": true, "data": payroll})
}

// GetPayrollHistory mengambil semua riwayat pembayaran gaji lengkap dengan data karyawannya
func GetPayrollHistory(c *fiber.Ctx) error {
	var payrolls []models.Payroll

	// Preload("Employee") digunakan agar data nama dan posisi karyawan ikut terbawa
	// Order("payment_date desc, id desc") agar gaji yang terbaru tampil di paling atas
	if err := database.DB.Preload("Employee").Where("is_demo = ?", IsDemoUser(c)).Order("payment_date desc, id desc").Find(&payrolls).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal mengambil riwayat gaji"})
	}

	return c.JSON(fiber.Map{"success": true, "data": payrolls})
}

// DeletePayrollHistory menghapus riwayat gaji SEKALIGUS menghapus potongannya di Kas
func DeletePayrollHistory(c *fiber.Ctx) error {
	id := c.Params("id")

	// Cari data gaji yang mau dihapus
	var payroll models.Payroll
	if err := database.DB.First(&payroll, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Riwayat gaji tidak ditemukan"})
	}

	if payroll.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	// Gunakan Transaction (tx) agar kalau satu gagal, gagal semua (aman)
	tx := database.DB.Begin()

	// 1. CARI DAN HAPUS DATA DI BUKU KAS (CASHFLOW)
	// Kita cari kas keluar yang tanggal, nominal, dan catatannya persis sama dengan gaji ini
	descKas := "Pembayaran Gaji: " + payroll.Description
	if err := tx.Where("transaction_date = ? AND amount = ? AND description = ? AND transaction_type = ?", payroll.PaymentDate, payroll.Amount, descKas, "KELUAR").Delete(&models.CashFlow{}).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menghapus data Kas Keluar"})
	}

	// 2. HAPUS DATA RIWAYAT GAJI (PAYROLL)
	if err := tx.Delete(&payroll).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menghapus riwayat gaji"})
	}

	tx.Commit() // Simpan perubahan permanen ke database
	return c.JSON(fiber.Map{"success": true, "message": "Riwayat gaji dan Kas Keluar berhasil ditarik/dihapus"})
}

// GetAttendanceLogs mengambil log absensi dari mesin
func GetAttendanceLogs(c *fiber.Ctx) error {
	var logs []models.AttendanceLog
	
	// Mengambil 500 log terakhir, diurutkan dari yang paling baru
	if err := database.DB.Where("is_demo = ?", IsDemoUser(c)).Order("log_time desc").Limit(500).Find(&logs).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal mengambil log absensi"})
	}

	return c.JSON(fiber.Map{"success": true, "data": logs})
}

// DeleteAttendanceLog menghapus log absensi secara manual
func DeleteAttendanceLog(c *fiber.Ctx) error {
	id := c.Params("id")
	
	var logItem models.AttendanceLog
	if err := database.DB.First(&logItem, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Log tidak ditemukan"})
	}
	if logItem.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	// Gunakan Unscoped() agar data benar-benar terhapus (hard delete), bukan sekadar soft delete
	if err := database.DB.Unscoped().Delete(&logItem).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menghapus log absensi"})
	}

	return c.JSON(fiber.Map{"success": true, "message": "Log absensi berhasil dihapus"})
}

// UpdateAttendanceLog memperbarui jam masuk dan/atau jam pulang secara manual oleh admin
func UpdateAttendanceLog(c *fiber.Ctx) error {
	id := c.Params("id")

	type UpdateInput struct {
		LogTime      string `json:"log_time"`       // jam masuk (format: "HH:MM")
		CheckOutTime string `json:"check_out_time"` // jam pulang (format: "HH:MM"), kosong = hapus check-out
		WorkDate     string `json:"work_date"`      // tanggal kerja (format: "YYYY-MM-DD")
	}

	var input UpdateInput
	if err := c.BodyParser(&input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Input tidak valid"})
	}

	var logItem models.AttendanceLog
	if err := database.DB.First(&logItem, id).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Log tidak ditemukan"})
	}
	if logItem.IsDemo != IsDemoUser(c) {
		return c.Status(403).JSON(fiber.Map{"success": false, "message": "Akses ditolak"})
	}

	wib := wibLocation()

	// Update WorkDate jika diberikan
	if input.WorkDate != "" {
		wd, err := time.ParseInLocation("2006-01-02", input.WorkDate, time.UTC)
		if err != nil {
			return c.Status(400).JSON(fiber.Map{"success": false, "message": "Format work_date salah (wajib YYYY-MM-DD)"})
		}
		logItem.WorkDate = wd
	}

	// Dapatkan tanggal kerja WIB sebagai base untuk set jam
	workDateWIBStr := logItem.WorkDate.UTC().Format("2006-01-02")

	// Update jam masuk jika diberikan
	if input.LogTime != "" {
		logTimeStr := workDateWIBStr + " " + input.LogTime + ":00"
		lt, err := time.ParseInLocation("2006-01-02 15:04:05", logTimeStr, wib)
		if err != nil {
			return c.Status(400).JSON(fiber.Map{"success": false, "message": "Format log_time salah (wajib HH:MM)"})
		}
		logItem.LogTime = lt
		logItem.LateMinutes = hitungKeterlambatan(lt)
		logItem.VerifyState = 0 // Reset ke check-in
	}

	// Update jam pulang jika diberikan
	if input.CheckOutTime != "" {
		// Cek apakah jam pulang < jam masuk (berarti melintas tengah malam)
		cotStr := workDateWIBStr + " " + input.CheckOutTime + ":00"
		cot, err := time.ParseInLocation("2006-01-02 15:04:05", cotStr, wib)
		if err != nil {
			return c.Status(400).JSON(fiber.Map{"success": false, "message": "Format check_out_time salah (wajib HH:MM)"})
		}
		// Jika checkout sebelum check-in, berarti melintas tengah malam — tambah 1 hari
		if cot.Before(logItem.LogTime) {
			cot = cot.AddDate(0, 0, 1)
		}
		logItem.CheckOutTime = &cot
		logItem.VerifyState = 1 // Ada check-out
	} else if input.CheckOutTime == "" && input.LogTime != "" {
		// Jika check_out_time sengaja dikosongkan, hapus check-out
		logItem.CheckOutTime = nil
		logItem.VerifyState = 0
	}

	if err := database.DB.Save(&logItem).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan perubahan log"})
	}

	return c.JSON(fiber.Map{"success": true, "message": "Log absensi berhasil diperbarui", "data": logItem})
}

// SaveAttendanceReason menyimpan alasan ketidakhadiran/error mesin
func SaveAttendanceReason(c *fiber.Ctx) error {
	type ReasonInput struct {
		Date        string `json:"date"`         // Format: YYYY-MM-DD (tanggal kerja / WorkDate)
		EmployeePIN string `json:"employee_pin"` // PIN Mesin
		Alasan      string `json:"alasan"`       // Sakit / Mesin Error / dll
	}

	var input ReasonInput
	if err := c.BodyParser(&input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Input tidak valid"})
	}

	// Verify Employee belongs to the correct demo mode
	var emp models.Employee
	if err := database.DB.Where("fingerprint_pin = ? AND is_demo = ?", input.EmployeePIN, IsDemoUser(c)).First(&emp).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Karyawan dengan PIN tersebut tidak ditemukan"})
	}

	// Parsing WorkDate (UTC) — ini adalah "tanggal kerja riil" bebas timezone shift
	workDate, err := time.ParseInLocation("2006-01-02", input.Date, time.UTC)
	if err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Format tanggal salah (wajib YYYY-MM-DD)"})
	}

	// Batas waktu lokal (WIB) untuk fallback query log_time
	wib := wibLocation()
	localStart, _ := time.ParseInLocation("2006-01-02", input.Date, wib)
	localEnd := localStart.Add(24 * time.Hour)

	// ── Cari log berdasarkan WorkDate (mendukung shift malam cross-midnight) ──
	// Prioritas 1: Gunakan kolom work_date jika sudah ada di tabel
	// Prioritas 2: Fallback ke rentang log_time 24 jam (data lama sebelum migrasi)
	var logs []models.AttendanceLog
	err = database.DB.Where(
		"employee_pin = ? AND is_demo = ? AND (work_date = ? OR (work_date IS NULL AND log_time >= ? AND log_time < ?))",
		input.EmployeePIN, IsDemoUser(c),
		workDate,
		localStart, localEnd,
	).Find(&logs).Error
	if err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal mencari log absensi"})
	}

	if len(logs) > 0 {
		// Update alasan di semua log pada hari kerja tersebut
		for _, logItem := range logs {
			if input.Alasan == "" && logItem.VerifyType == 0 && logItem.VerifyState == 0 && logItem.DeviceSN == "" {
				// Hapus dummy log jika alasan dihapus/dikosongkan
				database.DB.Unscoped().Delete(&logItem)
			} else {
				logItem.Alasan = input.Alasan
				if err := database.DB.Save(&logItem).Error; err != nil {
					return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan alasan"})
				}
			}
		}
	} else if input.Alasan != "" {
		// Buat log dummy untuk karyawan tidak hadir.
		// LogTime = jam 07:00 WIB pada tanggal kerja tersebut (netral, tidak ada makna shift).
		// WorkDate diisi agar future query via work_date berjalan benar.
		dummyLogTime := time.Date(workDate.Year(), workDate.Month(), workDate.Day(), 7, 0, 0, 0, wib)
		newLog := models.AttendanceLog{
			EmployeePIN: input.EmployeePIN,
			LogTime:     dummyLogTime,
			WorkDate:    workDate, // ← krusial: set WorkDate agar konsisten
			LateMinutes: 0,
			Alasan:      input.Alasan,
			VerifyType:  0,
			VerifyState: 0,
			IsDemo:      IsDemoUser(c),
		}
		if err := database.DB.Create(&newLog).Error; err != nil {
			return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal membuat log baru"})
		}
	}

	return c.JSON(fiber.Map{"success": true, "message": "Alasan berhasil disimpan"})
}

// CreateAttendanceLog membuat log absensi baru secara manual oleh admin
func CreateAttendanceLog(c *fiber.Ctx) error {
	type CreateInput struct {
		EmployeePIN  string `json:"employee_pin"`
		LogTime      string `json:"log_time"`       // jam masuk (format: "HH:MM")
		CheckOutTime string `json:"check_out_time"` // jam pulang (format: "HH:MM")
		WorkDate     string `json:"work_date"`      // tanggal kerja (format: "YYYY-MM-DD")
	}

	var input CreateInput
	if err := c.BodyParser(&input); err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Input tidak valid"})
	}

	if input.EmployeePIN == "" || input.WorkDate == "" {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "PIN Karyawan dan Tanggal Kerja wajib diisi"})
	}

	// Verify Employee belongs to the correct demo mode
	var emp models.Employee
	if err := database.DB.Where("fingerprint_pin = ? AND is_demo = ?", input.EmployeePIN, IsDemoUser(c)).First(&emp).Error; err != nil {
		return c.Status(404).JSON(fiber.Map{"success": false, "message": "Karyawan dengan PIN tersebut tidak ditemukan"})
	}

	wib := wibLocation()

	wd, err := time.ParseInLocation("2006-01-02", input.WorkDate, time.UTC)
	if err != nil {
		return c.Status(400).JSON(fiber.Map{"success": false, "message": "Format work_date salah (wajib YYYY-MM-DD)"})
	}

	// Buat log item baru
	var logItem models.AttendanceLog
	logItem.EmployeePIN = input.EmployeePIN
	logItem.WorkDate = wd
	logItem.IsDemo = IsDemoUser(c)

	// Set LogTime jika ada
	if input.LogTime != "" {
		logTimeStr := input.WorkDate + " " + input.LogTime + ":00"
		lt, err := time.ParseInLocation("2006-01-02 15:04:05", logTimeStr, wib)
		if err != nil {
			return c.Status(400).JSON(fiber.Map{"success": false, "message": "Format log_time salah (wajib HH:MM)"})
		}
		logItem.LogTime = lt
		logItem.LateMinutes = hitungKeterlambatan(lt)
		logItem.VerifyState = 0
	} else {
		// Default log time if not provided (e.g. 07:00 WIB)
		logItem.LogTime = time.Date(wd.Year(), wd.Month(), wd.Day(), 7, 0, 0, 0, wib)
		logItem.VerifyState = 0
	}

	// Set CheckOutTime jika ada
	if input.CheckOutTime != "" {
		cotStr := input.WorkDate + " " + input.CheckOutTime + ":00"
		cot, err := time.ParseInLocation("2006-01-02 15:04:05", cotStr, wib)
		if err != nil {
			return c.Status(400).JSON(fiber.Map{"success": false, "message": "Format check_out_time salah (wajib HH:MM)"})
		}
		if cot.Before(logItem.LogTime) {
			cot = cot.AddDate(0, 0, 1)
		}
		logItem.CheckOutTime = &cot
		logItem.VerifyState = 1
	}

	if err := database.DB.Create(&logItem).Error; err != nil {
		return c.Status(500).JSON(fiber.Map{"success": false, "message": "Gagal menyimpan log baru"})
	}

	return c.JSON(fiber.Map{"success": true, "message": "Log absensi berhasil dibuat", "data": logItem})
}

