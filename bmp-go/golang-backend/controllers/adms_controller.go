package controllers

import (
	"fmt"
	"log"
	"strings"
	"time"

	"invoice-bmp-go/database"
	"invoice-bmp-go/models"

	"github.com/gofiber/fiber/v2"
)

// SyncedDevices menyimpan kapan terakhir kali mesin di-sync jamnya
var SyncedDevices = make(map[string]time.Time)

// wibLocation mengembalikan timezone Asia/Jakarta (WIB, UTC+7).
// Jika OS tidak memiliki data zoneinfo, fallback ke fixed offset UTC+7.
func wibLocation() *time.Location {
	loc, err := time.LoadLocation("Asia/Jakarta")
	if err != nil {
		loc = time.FixedZone("WIB", 7*3600)
	}
	return loc
}

// workDateWIB menentukan "tanggal kerja" berdasarkan jam WIB.
//
// ATURAN SHIFT MALAM (Cross-Midnight):
//   - Jika jam fingerprint (WIB) berada di antara rentang 00:00 hingga 11:59 WIB,
//     maka secara otomatis dikurangi 1 hari (masuk ke WorkDate hari sebelumnya).
//   - Jika di atas jam 12:00 WIB, tetap hari yang sama.
//   - Semua ketergantungan terhadap nilai 'state' mesin dihapus.
func workDateWIB(logTime time.Time) time.Time {
	wib := wibLocation()
	tWIB := logTime.In(wib)
	h := tWIB.Hour()

	workDate := time.Date(tWIB.Year(), tWIB.Month(), tWIB.Day(), 0, 0, 0, 0, time.UTC)

	if h >= 0 && h < 12 {
		workDate = workDate.AddDate(0, 0, -1)
	}
	return workDate
}

// hitungKeterlambatan menghitung menit keterlambatan masuk.
//
// Aturan shift (jam masuk standar WIB):
//   - Shift Pagi  : 07:00 (jam 7–14)
//   - Shift Sore  : 15:00 (jam 14–21)
//   - Shift Malam : 23:00 (jam 21–06 hari berikutnya → jam < 12 ditangani via WorkDate)
//
// Jika fingerprint lebih awal dari jam standar → LateMinutes = 0.
// Tidak ada validasi durasi minimum 8 jam.
func hitungKeterlambatan(logTime time.Time) int {
	wib := wibLocation()
	t := logTime.In(wib)
	h := t.Hour()
	m := t.Minute()
	totalMinutes := h*60 + m

	var standarMasuk time.Time
	
	// Shift Pagi: 06:01 (361 menit) s.d. 07:30 (450 menit)
	if totalMinutes >= 361 && totalMinutes <= 450 {
		standarMasuk = time.Date(t.Year(), t.Month(), t.Day(), 7, 0, 0, 0, wib)
		diff := int(logTime.Sub(standarMasuk).Minutes())
		if diff < 0 {
			return 0
		}
		return diff
	}
	
	// Shift Sore: 14:01 (841 menit) s.d. 15:30 (930 menit)
	if totalMinutes >= 841 && totalMinutes <= 930 {
		standarMasuk = time.Date(t.Year(), t.Month(), t.Day(), 15, 0, 0, 0, wib)
		diff := int(logTime.Sub(standarMasuk).Minutes())
		if diff < 0 {
			return 0
		}
		return diff
	}
	
	// Shift Malam: 22:01 (1321 menit) s.d. 23:30 (1410 menit)
	if totalMinutes >= 1321 && totalMinutes <= 1410 {
		standarMasuk = time.Date(t.Year(), t.Month(), t.Day(), 23, 0, 0, 0, wib)
		diff := int(logTime.Sub(standarMasuk).Minutes())
		if diff < 0 {
			return 0
		}
		return diff
	}

	// Default jika di luar shift window
	return 0
}

// AdmsCdata menangani inisialisasi mesin dan pengiriman log absensi
func AdmsCdata(c *fiber.Ctx) error {
	sn := c.Query("SN")

	log.Printf("[ADMS] Received %s request on /iclock/cdata?SN=%s&table=%s&options=%s\n",
		c.Method(), sn, c.Query("table"), c.Query("options"))

	if sn == "" {
		return c.SendString("ERROR: No SN")
	}

	// ── Inisialisasi awal mesin (GET atau POST?options=all) ──────────────────
	if c.Method() == "GET" || c.Query("options") == "all" {
		var device models.AdmsDevice
		result := database.DB.Where("serial_number = ?", sn).First(&device)
		if result.Error != nil {
			device = models.AdmsDevice{SerialNumber: sn, LastActivity: time.Now()}
			database.DB.Create(&device)
		} else {
			device.LastActivity = time.Now()
			database.DB.Save(&device)
		}

		response := fmt.Sprintf(
			"GET OPTION FROM:%s\nErrorDelay=60\nDelay=30\nTransTimes=00:00;14:05\nTransInterval=1\nTransFlag=TransData AttLog OpLog\nRealtime=1\nEncrypt=0",
			sn,
		)
		c.Set("Content-Type", "text/plain")
		return c.SendString(response)
	}

	// ── Terima log absensi (POST &table=ATTLOG) ──────────────────────────────
	if c.Method() == "POST" && c.Query("table") == "ATTLOG" {
		body := string(c.Body())
		lines := strings.Split(body, "\n")

		// Timezone WIB disiapkan sekali untuk semua baris dalam request ini
		loc := wibLocation()

		// Muat device sekali untuk memeriksa status IsDemo
		var device models.AdmsDevice
		database.DB.Where("serial_number = ?", sn).First(&device)

		for _, line := range lines {
			line = strings.TrimSpace(line)
			if line == "" {
				continue
			}

			// Format payload mesin ZKTeco / Solution:
			// PIN\tTime\tState\tVerifyType
			// Contoh: 5\t2026-05-22 07:51:27\t0\t1
			parts := strings.Split(line, "\t")
			if len(parts) < 2 {
				log.Printf("[ADMS] Baris tidak valid, diabaikan: %q", line)
				continue
			}

			pin := strings.TrimSpace(parts[0])
			timeStr := strings.TrimSpace(parts[1])

			state := 0
			verifyType := 0
			if len(parts) >= 3 {
				fmt.Sscanf(strings.TrimSpace(parts[2]), "%d", &state)
			}
			if len(parts) >= 4 {
				fmt.Sscanf(strings.TrimSpace(parts[3]), "%d", &verifyType)
			}

			// Karena jam mesin sering error/tidak sinkron, kita override logTime menggunakan waktu server saat ini (WIB).
			// Waktu asli dari mesin tetap dicatat di log untuk kebutuhan debugging.
			logTime := time.Now().In(loc)
			log.Printf("[ADMS] 🕒 Scan diterima untuk PIN=%s | Jam mesin: %q | Di-override ke jam server WIB: %s",
				pin, timeStr, logTime.Format("2006-01-02 15:04:05"),
			)

			// ── Tentukan WorkDate (tanggal kerja riil, aman untuk shift malam) ──
			workDate := workDateWIB(logTime)

			// ── Cek apakah sudah ada log absensi untuk PIN dan WorkDate ini ──
			var existingLog models.AttendanceLog
			err := database.DB.Where("employee_pin = ? AND work_date = ? AND is_demo = ?", pin, workDate, device.IsDemo).First(&existingLog).Error

			if err == nil {
				// 📋 LOG DI HARI YANG SAMA DITEMUKAN → INI ADALAH CHECK-OUT
				
				// Validasi ketat retransmit & multi-scan
				// a. Jika logTime baru <= logTime Check-In yang sudah ada, abaikan
				if logTime.Before(existingLog.LogTime) || logTime.Equal(existingLog.LogTime) {
					log.Printf("[ADMS] ⚠️ Abaikan retransmit/data check-in lama: PIN=%s | logTime=%s WIB <= existing.LogTime=%s WIB",
						pin, logTime.In(loc).Format("2006-01-02 15:04:05"), existingLog.LogTime.In(loc).Format("2006-01-02 15:04:05"),
					)
					continue
				}

				// b. Jika check_out_time sudah terisi, update hanya jika logTime baru > check_out_time lama
				if existingLog.CheckOutTime != nil {
					if !logTime.After(*existingLog.CheckOutTime) {
						log.Printf("[ADMS] ⚠️ Abaikan check-out lama / retransmit: PIN=%s | logTime=%s WIB <= existing.CheckOutTime=%s WIB",
							pin, logTime.In(loc).Format("2006-01-02 15:04:05"), existingLog.CheckOutTime.In(loc).Format("2006-01-02 15:04:05"),
						)
						continue
					}
				}

				// Update check-out time dengan waktu terbaru, dan ubah VerifyState ke 1 (Check-Out)
				existingLog.CheckOutTime = &logTime
				existingLog.VerifyState = 1
				if err := database.DB.Save(&existingLog).Error; err != nil {
					log.Printf("[ADMS] ❌ Gagal update log check-out PIN=%s: %v", pin, err)
					continue
				}
				log.Printf("[ADMS] 💾 Terupdate (Check-Out): ID=%d | PIN=%s | Pulang=%s WIB | WorkDate=%s",
					existingLog.ID, pin, logTime.In(loc).Format("15:04:05"), workDate.Format("2006-01-02"),
				)
			} else {
				// 📋 LOG BELUM ADA → INI ADALAH CHECK-IN
				// Hitung keterlambatan berdasarkan rentang window shift ketat
				lateMinutes := hitungKeterlambatan(logTime)

				attLog := models.AttendanceLog{
					DeviceSN:    sn,
					EmployeePIN: pin,
					VerifyType:  verifyType,
					VerifyState: 0, // Check-In
					LogTime:     logTime,
					WorkDate:    workDate,
					LateMinutes: lateMinutes,
					IsDemo:      device.IsDemo,
				}
				if err := database.DB.Create(&attLog).Error; err != nil {
					log.Printf("[ADMS] ❌ Gagal simpan log check-in PIN=%s: %v", pin, err)
					continue
				}
				log.Printf("[ADMS] 💾 Tersimpan (Check-In): ID=%d | PIN=%s | Masuk=%s WIB | WorkDate=%s | Late=%dmnt",
					attLog.ID, pin, logTime.In(loc).Format("15:04:05"), workDate.Format("2006-01-02"), lateMinutes,
				)
			}
		}

		c.Set("Content-Type", "text/plain")
		return c.SendString("OK")
	}

	// Table lain (OpLog, UserInfo, dll) — cukup balas OK
	if c.Method() == "POST" {
		c.Set("Content-Type", "text/plain")
		return c.SendString("OK")
	}

	return c.SendStatus(fiber.StatusNotFound)
}

// AdmsGetRequest menangani request komando dari mesin
func AdmsGetRequest(c *fiber.Ctx) error {
	sn := c.Query("SN")
	log.Printf("[ADMS] Received %s request on /iclock/getrequest?SN=%s\n", c.Method(), sn)

	if sn != "" {
		// Sinkronisasi jam mesin ke jam server maksimal 1x per jam
		lastSync, exists := SyncedDevices[sn]
		if !exists || time.Since(lastSync) > 1*time.Hour {
			SyncedDevices[sn] = time.Now()

			loc := wibLocation()
			nowWIB := time.Now().In(loc)
			cmdID := nowWIB.Unix() % 10000
			command := fmt.Sprintf("C:%d:SET OPTIONS DateTime=%s\n", cmdID, nowWIB.Format("2006-01-02 15:04:05"))

			log.Printf("⏳ Time Sync → mesin %s: %s", sn, strings.TrimSpace(command))
			c.Set("Content-Type", "text/plain")
			return c.SendString(command)
		}
	}

	c.Set("Content-Type", "text/plain")
	return c.SendString("OK")
}
