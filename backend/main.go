package main

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"crypto/sha1"
	"crypto/sha512"
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"hash"
	"io"
	"log"
	"math"
	"net"
	"net/http"
	"net/smtp"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strconv"
	"sort"
	"strings"
	"sync"
	"time"
)

type LocalUser struct {
	GoogleSub    string `json:"googleSub"`
	Email        string `json:"email"`
	RegisteredAt int64  `json:"registeredAt"`
	IsActive     bool   `json:"isActive"`
}

var (
	port              string
	adminAuthToken    string
	adminSessions     = make(map[string]string) // sessionToken -> adminEmail
	adminSessionsMu   sync.Mutex
	// paymentTokens: one-time tokens for confirm-payment-action links in admin emails
	// key = token (UUID), value = "action:sub:email" validated once then deleted
	paymentTokens   = make(map[string]string)
	paymentTokensMu sync.Mutex
)

func main() {
	// Load environment variables
	port = os.Getenv("PORT")
	if port == "" {
		port = "3000"
	}

	adminAuthToken = os.Getenv("ADMIN_AUTH_TOKEN")
	if adminAuthToken == "" {
		log.Fatal("FATAL: ADMIN_AUTH_TOKEN environment variable is required. Set it before starting the server.")
	}

	log.Printf("Starting PosBah Go backend...")

	// Initialize local PostgreSQL database
	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		log.Fatal("FATAL: DATABASE_URL environment variable is required. Set it before starting the server.")
	}
	if err := initDatabase(dbURL); err != nil {
		log.Printf("Warning: local database initialization failed: %v", err)
	}
	// Run initial synchronization of local users and tenants
	syncDatabaseUsersAndTenants()
	// Detect current APK version automatically on startup
	autoDetectApkVersion()

	// Start background cron worker (runs check every hour)
	go startCronWorker()

	// Setup endpoints
	http.Handle("/", http.FileServer(http.Dir("./web")))
	http.HandleFunc("/admin", handleAdminPage)
	http.HandleFunc("/admin/", handleAdminPage)
	http.HandleFunc("/api/admin/login", handleAdminLogin)
	http.HandleFunc("/api/admin/check-login", handleAdminCheckLogin)
	http.HandleFunc("/api/admin/logout", handleAdminLogout)
	http.HandleFunc("/api/admin/users", handleAdminGetUsers)
	http.HandleFunc("/api/admin/toggle-block", handleAdminToggleBlock)
	http.HandleFunc("/api/admin/confirm-payment", handleAdminConfirmPayment)
	http.HandleFunc("/api/admin/apk-config", handleAdminApkConfig)
	http.HandleFunc("/api/admin/diagnose", handleAdminDiagnose)
	http.HandleFunc("/status", handleStatus)
	http.HandleFunc("/api/admin/deploy", handleAdminDeploy)
	http.HandleFunc("/api/admin/deploy-log", handleAdminDeployLog)
	http.HandleFunc("/api/admin/delete-user", handleAdminDeleteUser)
	http.HandleFunc("/api/admin/blast-update-email", handleAdminBlastUpdateEmail)
	http.HandleFunc("/api/admin/check-demo-lockout", handleManualLockoutCheck)
	http.HandleFunc("/api/admin/demo-users", handleGetDemoUsers)
	http.HandleFunc("/api/admin/approve-user", handleApproveUser)
	http.HandleFunc("/api/admin/reject-user", handleRejectUser)
	http.HandleFunc("/api/admin/approve-demo", handleApproveDemo)
	http.HandleFunc("/api/admin/reject-demo", handleRejectDemo)
	http.HandleFunc("/api/admin/inspect-tenant", handleInspectTenant)
	http.HandleFunc("/api/admin/confirm-payment-page", handleConfirmPaymentPage)
	http.HandleFunc("/api/admin/confirm-payment-action", handleConfirmPaymentAction)
	http.HandleFunc("/api/auth/qr-session", handleQrSession)
	http.HandleFunc("/api/auth/qr-authorize", handleQrAuthorize)
	http.HandleFunc("/api/auth/qr-confirm", handleQrConfirm)
	http.HandleFunc("/api/auth/qr-check", handleQrCheck)
	http.HandleFunc("/ws", handleWS)
	http.HandleFunc("/api/invoice/signature", handleSaveSignature)
	http.HandleFunc("/api/invoice/signature-status", handleSignatureStatus)
	http.HandleFunc("/api/invoice/delete-signature", handleDeleteSignature)
	http.HandleFunc("/sign/", handleSignPage)
	http.HandleFunc("/api/sign/", handleSignPage)
	http.HandleFunc("/store/", handleStorePage)
	http.HandleFunc("/api/store/", handleStorePage)
	http.HandleFunc("/api/ai/classify", handleAiClassify)

	// APK download & version endpoints
	http.HandleFunc("/api/auth/get-apk-download-token", handleGetApkDownloadToken)
	http.HandleFunc("/api/download-apk", handleDownloadApk)
	http.HandleFunc("/api/dowload-apk", handleDownloadApk) // Typo compatibility
	http.HandleFunc("/api/apk-version", handleApkVersion)

	// ADMS Fingerprint machine endpoints
	http.HandleFunc("/iclock/cdata", handleCData)
	http.HandleFunc("/iclock/getrequest", handleGetRequest)

	// Sync API for Android client
	http.HandleFunc("/api/sync/", handleSyncRoute)
	http.HandleFunc("/api/employee/confirm", handleEmployeeConfirm)

	// Auth Rejoin endpoints for deleted users
	http.HandleFunc("/api/auth/check-deleted", handleCheckDeleted)
	http.HandleFunc("/api/auth/request-rejoin", handleRequestRejoin)
	http.HandleFunc("/api/auth/confirm-rejoin", handleConfirmRejoin)
	http.HandleFunc("/api/auth/approve-rejoin", handleApproveRejoin)
	http.HandleFunc("/api/auth/complete-rejoin", handleCompleteRejoin)
	
	// Serve static files from TTD
	http.Handle("/api/signatures/", http.StripPrefix("/api/signatures/", http.FileServer(http.Dir("./TTD"))))

	// Reports API
	http.HandleFunc("/api/reports/outlet-margin", handleOutletMarginReport)

	log.Printf("Server listening on port %s", port)
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}

// Background cron worker running every hour
func startCronWorker() {
	log.Println("Background cron worker started. Checking every 1 hour...")
	ticker := time.NewTicker(1 * time.Hour)
	defer ticker.Stop()

	// Run first check immediately on startup
	if err := checkAndLockoutDemoUsers(); err != nil {
		log.Printf("Initial demo lockout check failed: %v", err)
	}
	runDatabaseInterconnectSyncAutomation()
	autoDetectApkVersion()

	for range ticker.C {
		log.Println("Triggering scheduled demo lockout check...")
		if err := checkAndLockoutDemoUsers(); err != nil {
			log.Printf("Scheduled demo lockout check failed: %v", err)
		}
		runDatabaseInterconnectSyncAutomation()
		autoDetectApkVersion()
	}
}

// Performs core logic of querying and deleting expired demo users
func checkAndLockoutDemoUsers() error {
	if db == nil {
		return fmt.Errorf("local database not initialized")
	}
	go checkAndNotifyAdminOfNewDemoUsers()
	syncDatabaseUsersAndTenants()
	nowMillis := time.Now().UnixNano() / int64(time.Millisecond)
	twoDaysAgoMillis := nowMillis - (2 * 24 * 60 * 60 * 1000)
	fiveDaysAgoMillis := nowMillis - (5 * 24 * 60 * 60 * 1000)

	// --- 1. Day 5 Deletion / Purge ---
	rows5, err5 := db.Query(`SELECT "googleSub", "email" FROM "local_users" WHERE "isPremium" = FALSE AND "registeredAt" < $1 AND "email" NOT LIKE '%@posbah.com'`, fiveDaysAgoMillis)
	if err5 == nil {
		type UserToPurge struct {
			GoogleSub string
			Email     string
		}
		var toPurge []UserToPurge
		for rows5.Next() {
			var p UserToPurge
			if err := rows5.Scan(&p.GoogleSub, &p.Email); err == nil {
				toPurge = append(toPurge, p)
			}
		}
		rows5.Close()

		for _, p := range toPurge {
			log.Printf("[Cron] Purging expired demo user after 5 days: %s (sub: %s)", p.Email, p.GoogleSub)
			if err := deleteLocalUser(p.GoogleSub); err != nil {
				log.Printf("[Cron] Error purging user %s: %v", p.Email, err)
			} else {
				log.Printf("[Cron] Successfully purged expired demo user: %s", p.Email)
				wsMsg := map[string]interface{}{
					"type":      "user_status_changed",
					"googleSub": p.GoogleSub,
					"status":    "deleted",
				}
				if msgBytes, err := json.Marshal(wsMsg); err == nil {
					broadcastWSMessage(string(msgBytes))
				}
			}
		}
	} else {
		log.Printf("[Cron] Error querying 5-day expired demo users: %v", err5)
	}

	// --- 2. Day 2 Warning & Lockout ---
	rows2, err2 := db.Query(`SELECT "googleSub", "email", "registeredAt" FROM "local_users" WHERE "isPremium" = FALSE AND "registeredAt" < $1 AND "demoDay2Notified" = FALSE AND "email" NOT LIKE '%@posbah.com'`, twoDaysAgoMillis)
	if err2 != nil {
		return fmt.Errorf("failed to query 2-day expired demo users: %w", err2)
	}
	defer rows2.Close()

	var users2 []LocalUser
	for rows2.Next() {
		var u LocalUser
		var registeredAt int64
		if err := rows2.Scan(&u.GoogleSub, &u.Email, &registeredAt); err != nil {
			log.Printf("Failed to scan user: %v", err)
			continue
		}
		u.RegisteredAt = registeredAt
		users2 = append(users2, u)
	}

	for _, user := range users2 {
		log.Printf("[Cron] Locking out and warning demo user: %s", user.Email)
		
		// Send Day 2 warning email to demouser
		subject := "[POSBah] Masa Uji Coba Demo POSBah Segera Berakhir"
		body := `
			<div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 500px; margin: 0 auto; padding: 30px; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff; color: #1e293b; box-shadow: 0 4px 6px rgba(0,0,0,0.05);">
				<h2 style="color: #dc2626; font-size: 20px; font-weight: 700; margin-bottom: 16px; text-align: center;">Uji Coba Demo Berakhir</h2>
				<p>Halo,</p>
				<p>Masa uji coba gratis (demo) 2 hari Anda di <strong>POSBah</strong> telah berakhir, dan akun Anda telah ditangguhkan.</p>
				<p>Untuk menghindari kehilangan data, akun dan seluruh data transaksi Anda akan dihapus secara permanen dalam waktu <strong>3 hari</strong> (total 5 hari sejak pendaftaran).</p>
				<p>Silakan lakukan pembayaran dan upgrade ke Premium sekarang juga agar data Anda tidak terhapus.</p>
				<div style="text-align: center; margin: 24px 0;">
					<span style="display: inline-block; background-color: #2563eb; color: #ffffff; padding: 14px 28px; font-weight: 600; font-size: 15px; border-radius: 10px; text-decoration: none;">Hubungi Admin untuk Upgrade</span>
				</div>
				<p style="font-size: 13px; color: #64748b; line-height: 1.5;">Catatan: Data yang telah dihapus tidak dapat dipulihkan kembali. Terima kasih atas pengertiannya.</p>
			</div>
		`

		errMail := sendEmail(user.Email, subject, body)
		if errMail != nil {
			log.Printf("[Cron] Warning: Could not send day 2 email to %s: %v", user.Email, errMail)
		}

		// Update user to inactive and mark day 2 notified
		_, err := db.Exec(`UPDATE "local_users" SET "isActive" = FALSE, "demoDay2Notified" = TRUE, "updatedAt" = $1 WHERE "googleSub" = $2`,
			nowMillis, user.GoogleSub)
		if err != nil {
			log.Printf("Failed to lockout expired demo user %s: %v", user.Email, err)
		} else {
			regTime := time.Unix(user.RegisteredAt/1000, 0).Format("2006-01-02 15:04:05")
			log.Printf("Successfully locked out demo user and set day 2 notified: %s (Registered at: %s)", user.Email, regTime)
			
			// Broadcast WS message
			wsMsg := map[string]interface{}{
				"type":      "user_status_changed",
				"googleSub": user.GoogleSub,
				"status":    "blocked",
			}
			if msgBytes, err := json.Marshal(wsMsg); err == nil {
				broadcastWSMessage(string(msgBytes))
			}
		}
	}
	return nil
}

// Purges all data referencing the tenantId across all tables
func purgeTenantData(tenantID string) {
	if tenantID == "" {
		return
	}
	log.Printf("Purging all data for tenant ID: %s", tenantID)

	// Delete from child tables first
	_, _ = db.Exec(`DELETE FROM "transaction_items" WHERE "transactionId" IN (SELECT "id" FROM "transactions" WHERE "tenantId" = $1)`, tenantID)
	_, _ = db.Exec(`DELETE FROM "bmp_products" WHERE "invoiceId" IN (SELECT "id" FROM "bmp_invoices" WHERE "tenantId" = $1)`, tenantID)
	_, _ = db.Exec(`DELETE FROM "bmp_invoice_payments" WHERE "invoiceId" IN (SELECT "id" FROM "bmp_invoices" WHERE "tenantId" = $1)`, tenantID)
	_, _ = db.Exec(`DELETE FROM "bmp_bahan_baku_item" WHERE "bahanBakuId" IN (SELECT "id" FROM "bmp_bahan_baku" WHERE "tenantId" = $1)`, tenantID)

	tables := []string{
		"bmp_payrolls",
		"bmp_clients",
		"bmp_invoices",
		"bmp_products",
		"bmp_master_products",
		"bmp_invoice_payments",
		"bmp_cashflow",
		"bmp_settings",
		"bmp_employees",
		"bmp_bahan_baku",
		"bmp_bahan_baku_item",
		"print_settings",
		"products",
		"customers",
		"transactions",
		"activity_logs",
		"employees",
		"outlets",
		"bmp_device_tenants",
	}

	for _, table := range tables {
		_, err := db.Exec(fmt.Sprintf(`DELETE FROM "%s" WHERE "tenantId" = $1`, table), tenantID)
		if err != nil {
			log.Printf("Error purging table %s for tenant %s: %v", table, tenantID, err)
		}
	}
	_, err := db.Exec(`DELETE FROM "tenants" WHERE "id" = $1`, tenantID)
	if err != nil {
		log.Printf("Error purging tenant %s from tenants table: %v", tenantID, err)
	}
}

// Deletes a user from local database using their GoogleSub and purges associated tenant data
func deleteLocalUser(googleSub string) error {
	if db == nil {
		return fmt.Errorf("local database not initialized")
	}

	var email string
	var tenantId sql.NullString
	// Search by googleSub or email for robust interconnected purging
	err := db.QueryRow(`SELECT "email", "tenantId" FROM "local_users" WHERE "googleSub" = $1 OR TRIM(LOWER("email")) = $2`, googleSub, strings.TrimSpace(strings.ToLower(googleSub))).Scan(&email, &tenantId)
	if err == nil {
		cleanEmail := strings.ReplaceAll(strings.ReplaceAll(email, ".", "_"), "@", "_")
		demoTenantId := "demo_tenant_" + cleanEmail
		premiumTenantId := "ten_premium_" + cleanEmail

		purgeTenantData(demoTenantId)
		purgeTenantData(premiumTenantId)
		if tenantId.Valid && tenantId.String != "" {
			purgeTenantData(tenantId.String)
		}

		// Purge any tenants starting with demo_tenant_<cleanEmail> or ten_premium_<cleanEmail>
		rows, errRows := db.Query(`SELECT "id" FROM "tenants" WHERE "ownerEmail" = $1 OR "id" LIKE $2 OR "id" LIKE $3`, 
			email, "demo_tenant_" + cleanEmail + "%", "ten_premium_" + cleanEmail + "%")
		if errRows == nil {
			defer rows.Close()
			for rows.Next() {
				var tid string
				if errScan := rows.Scan(&tid); errScan == nil {
					purgeTenantData(tid)
				}
			}
		}

		// Also purge from deleted_users table to allow them to register again
		_, _ = db.Exec(`DELETE FROM "deleted_users" WHERE TRIM(LOWER("email")) = $1`, strings.TrimSpace(strings.ToLower(email)))
	}

	_, err = db.Exec(`DELETE FROM "local_users" WHERE "googleSub" = $1 OR TRIM(LOWER("email")) = $2`, googleSub, strings.TrimSpace(strings.ToLower(googleSub)))
	return err
}

// Handler: GET /status
func handleStatus(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	dbStatus := "connected to PostgreSQL"
	if db == nil {
		dbStatus = "disconnected (db connection pool not initialized)"
	} else if err := db.Ping(); err != nil {
		dbStatus = fmt.Sprintf("disconnected error: %v", err)
	}

	w.Header().Set("Content-Type", "application/json")
	response := map[string]string{
		"status":    "running",
		"timestamp": time.Now().UTC().Format(time.RFC3339),
		"database":  dbStatus,
		"language":  "Go 1.21",
	}
	json.NewEncoder(w).Encode(response)
}

func handleAdminDeploy(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	if !isAdminAuthenticated(r) {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "Pembaruan otomatis dimulai di background. Server akan menarik kode baru, mengompilasi, dan me-restart layanan secara otomatis.",
	})

	go runAutoDeploy()
}

func getAdminEmail(r *http.Request) string {
	authHeader := r.Header.Get("Authorization")
	if authHeader == adminAuthToken {
		return "muhammadmuizz8@gmail.com"
	}
	token := getBearerToken(r)
	if token != "" {
		adminSessionsMu.Lock()
		email, exists := adminSessions[token]
		adminSessionsMu.Unlock()
		if exists {
			return email
		}
	}
	return ""
}

// generateUUID creates a cryptographically random UUID v4 string.
func generateUUID() string {
	b := make([]byte, 16)
	_, err := rand.Read(b)
	if err != nil {
		// fallback to timestamp if rand fails
		return fmt.Sprintf("%d", time.Now().UnixNano())
	}
	b[6] = (b[6] & 0x0f) | 0x40 // version 4
	b[8] = (b[8] & 0x3f) | 0x80 // variant 10
	return fmt.Sprintf("%08x-%04x-%04x-%04x-%12x",
		b[0:4], b[4:6], b[6:8], b[8:10], b[10:])
}

// handleAdminBlastUpdateEmail mengirim email notifikasi update APK ke semua user aktif.
// Pesan diambil dari release_notes.txt (section terbaru) atau dari request body.
func handleAdminBlastUpdateEmail(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	if !isAdminAuthenticated(r) {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	var req struct {
		Subject string `json:"subject"`
		Message string `json:"message"`
	}
	json.NewDecoder(r.Body).Decode(&req)

	// Fallback: ambil section pertama dari release_notes.txt
	if req.Message == "" {
		for _, path := range []string{"/home/muizz9900/release_notes.txt", "./release_notes.txt"} {
			data, err := os.ReadFile(path)
			if err == nil {
				content := string(data)
				if idx := strings.Index(content, "\n---\n"); idx > 0 {
					content = content[:idx]
				}
				req.Message = strings.TrimSpace(content)
				break
			}
		}
	}

	if req.Subject == "" {
		req.Subject = "Pembaruan POSBah - Versi Terbaru Tersedia!"
	}

	if req.Message == "" {
		http.Error(w, "Tidak ada pesan yang bisa dikirim. Periksa release_notes.txt di VPS.", http.StatusBadRequest)
		return
	}

	// Ambil semua email user aktif
	rows, err := db.Query(`SELECT "email", "displayName" FROM "local_users" WHERE "isActive" = TRUE AND "email" != '' ORDER BY "registeredAt" DESC`)
	if err != nil {
		http.Error(w, fmt.Sprintf("Gagal ambil daftar user: %v", err), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	type userInfo struct {
		Email string
		Name  string
	}
	var users []userInfo
	for rows.Next() {
		var u userInfo
		if err := rows.Scan(&u.Email, &u.Name); err == nil && u.Email != "" {
			users = append(users, u)
		}
	}

	if len(users) == 0 {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": true,
			"message": "Tidak ada user aktif ditemukan untuk dikirimi email.",
			"total":   0,
		})
		return
	}

	msg := req.Message
	subj := req.Subject

	// Kirim secara asinkron (background) agar response tidak timeout
	go func() {
		sent, failed := 0, 0
		for _, u := range users {
			name := u.Name
			if name == "" {
				name = "Pengguna POSBah"
			}
			body := fmt.Sprintf("Halo %s,\n\n%s\n\nUnduh pembaruan: https://www.zedmz.cloud/api/download-apk\n\nTerima kasih,\nTim POSBah", name, msg)
			if err := sendEmail(u.Email, subj, body); err != nil {
				log.Printf("[BlastEmail] Gagal kirim ke %s: %v", u.Email, err)
				failed++
			} else {
				log.Printf("[BlastEmail] Terkirim ke: %s", u.Email)
				sent++
			}
		}
		log.Printf("[BlastEmail] Selesai. Terkirim: %d, Gagal: %d dari total %d user", sent, failed, len(users))
	}()

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": fmt.Sprintf("Email sedang dikirim ke %d user aktif secara background. Pantau log server untuk hasilnya.", len(users)),
		"total":   len(users),
	})
}

func handleAdminDeleteUser(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	if !isAdminAuthenticated(r) {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	adminEmail := getAdminEmail(r)
	if adminEmail != "muhammadmuizz8@gmail.com" {
		http.Error(w, "Forbidden: Only muhammadmuizz8@gmail.com can delete users", http.StatusForbidden)
		return
	}

	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		GoogleSub string `json:"googleSub"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.GoogleSub == "" {
		http.Error(w, "googleSub is required", http.StatusBadRequest)
		return
	}

	err := deleteLocalUser(req.GoogleSub)
	if err != nil {
		http.Error(w, "Failed to delete user: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "User and all interconnected data successfully deleted",
	})
}

func runAutoDeploy() {
	logFile, _ := os.OpenFile("/home/muizz9900/deploy_log.txt", os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0644)
	if logFile == nil {
		logFile, _ = os.OpenFile("./deploy_log.txt", os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0644)
	}

	writeLog := func(format string, v ...interface{}) {
		msg := fmt.Sprintf(format, v...)
		log.Println(msg)
		if logFile != nil {
			logFile.WriteString(time.Now().Format("2006-01-02 15:04:05") + " " + msg + "\n")
		}
	}

	if logFile != nil {
		defer logFile.Close()
	}

	writeLog("[AutoDeploy] Memulai deploy otomatis...")

	runCmd := func(name string, args []string, dir string) error {
		cmd := exec.Command(name, args...)
		cmd.Dir = dir
		output, err := cmd.CombinedOutput()
		if err != nil {
			writeLog("[AutoDeploy] Perintah %s %v gagal: %v. Output: %s", name, args, err, string(output))
			return err
		}
		writeLog("[AutoDeploy] Perintah %s %v sukses. Output: %s", name, args, string(output))
		return nil
	}

	repoDir := "/home/muizz9900/posbah-app"
	backendDir := "/home/muizz9900/posbah-app/backend"
	destBin := "/home/muizz9900/posbah-backend"
	destAdminHtml := "/home/muizz9900/admin.html"
	destWebAdminHtml := "/home/muizz9900/web/admin.html"

	// 1a. Discard any local modifications in repository on VPS to prevent conflicts during pull
	_ = runCmd("git", []string{"reset", "--hard"}, repoDir)
	_ = runCmd("git", []string{"clean", "-fd"}, repoDir)

	// Diagnostics: log git remote and test SSH connection to GitHub
	_ = runCmd("git", []string{"remote", "-v"}, repoDir)
	_ = runCmd("ssh", []string{"-T", "-o", "StrictHostKeyChecking=no", "git@github.com"}, repoDir)

	// 1b. Git pull
	if err := runCmd("git", []string{"pull"}, repoDir); err != nil {
		writeLog("[AutoDeploy] Git pull gagal: %v", err)
		return
	}

	// 2. Go build (or use pre-compiled binary if present)
	usePrecompiled := false
	srcBin := backendDir + "/posbah-backend"
	if _, err := os.Stat(srcBin); err == nil {
		writeLog("[AutoDeploy] Menemukan pre-compiled binary. Menghindari build ulang di VPS.")
		usePrecompiled = true
	}

	if !usePrecompiled {
		if err := runCmd("go", []string{"build", "-o", "posbah-backend"}, backendDir); err != nil {
			writeLog("[AutoDeploy] Go build gagal: %v", err)
			return
		}
	}

	// 3. Backup & unlink old binary
	_ = os.Remove(destBin + ".bak")
	_ = os.Rename(destBin, destBin+".bak")

	// 4. Copy new binary
	srcBin = backendDir + "/posbah-backend"
	if err := copyFile(srcBin, destBin); err != nil {
		writeLog("[AutoDeploy] Gagal menyalin binary baru: %v", err)
		return
	}
	_ = os.Chmod(destBin, 0755)

	// 5. Copy admin.html
	_ = copyFile(backendDir+"/admin.html", destAdminHtml)

	// 6. Copy web/admin.html
	_ = os.MkdirAll("/home/muizz9900/web", 0755)
	_ = copyFile(backendDir+"/web/admin.html", destWebAdminHtml)

	// 7. Copy posbah-v*.apk files
	if apks, err := filepath.Glob(backendDir + "/posbah-v*.apk"); err == nil {
		for _, apkPath := range apks {
			destApk := "/home/muizz9900/" + filepath.Base(apkPath)
			_ = copyFile(apkPath, destApk)
		}
	}

	// 7b. Cleanup APK lama di VPS — pertahankan hanya 2 versi terbaru
	cleanupOldApks("/home/muizz9900/", writeLog)

	// 8. Copy release_notes.txt
	_ = copyFile(backendDir+"/release_notes.txt", "/home/muizz9900/release_notes.txt")

	writeLog("[AutoDeploy] Pembaruan file berhasil. Keluar dari proses untuk me-restart layanan...")
	time.Sleep(1 * time.Second)
	os.Exit(0) // Systemd akan otomatis me-restart process karena Restart=always
}

func handleAdminDeployLog(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	if !isAdminAuthenticated(r) {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	logBytes, err := os.ReadFile("/home/muizz9900/deploy_log.txt")
	if err != nil {
		logBytes, err = os.ReadFile("./deploy_log.txt")
	}

	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"error":   fmt.Sprintf("Gagal membaca file log: %v", err),
		})
		return
	}

	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.Write(logBytes)
}

// cleanupOldApks hapus APK lama di folder VPS, pertahankan hanya 2 versi terbaru.
// Dipanggil otomatis setelah deploy berhasil agar VPS tidak penuh file APK lama.
func cleanupOldApks(dir string, writeLog func(string, ...interface{})) {
	pattern := filepath.Join(dir, "posbah-v*.apk")
	apks, err := filepath.Glob(pattern)
	if err != nil {
		writeLog("[AutoDeploy] Gagal scan APK di %s: %v", dir, err)
		return
	}
	if len(apks) <= 2 {
		writeLog("[AutoDeploy] APK di VPS: %d file — tidak perlu cleanup", len(apks))
		return
	}

	// Sort versi semantic descending (terbaru di index 0)
	sort.Slice(apks, func(i, j int) bool {
		return compareApkVersions(filepath.Base(apks[i]), filepath.Base(apks[j])) > 0
	})

	writeLog("[AutoDeploy] Ditemukan %d APK. Mempertahankan 2 terbaru, menghapus sisanya.", len(apks))

	// Hapus semua selain 2 terbaru
	for _, apkPath := range apks[2:] {
		if err := os.Remove(apkPath); err != nil {
			writeLog("[AutoDeploy] Gagal menghapus %s: %v", filepath.Base(apkPath), err)
		} else {
			writeLog("[AutoDeploy] Dihapus: %s", filepath.Base(apkPath))
		}
	}
}

// compareApkVersions membandingkan versi semantic dari nama file APK.
// Return > 0 jika a lebih baru dari b.
func compareApkVersions(a, b string) int {
	extractVer := func(name string) []int {
		name = strings.TrimPrefix(name, "posbah-v")
		name = strings.TrimSuffix(name, ".apk")
		parts := strings.Split(name, ".")
		nums := make([]int, len(parts))
		for i, p := range parts {
			nums[i], _ = strconv.Atoi(p)
		}
		return nums
	}
	va, vb := extractVer(a), extractVer(b)
	length := len(va)
	if len(vb) > length {
		length = len(vb)
	}
	for i := 0; i < length; i++ {
		na, nb := 0, 0
		if i < len(va) {
			na = va[i]
		}
		if i < len(vb) {
			nb = vb[i]
		}
		if na != nb {
			return na - nb
		}
	}
	return 0
}

func copyFile(src, dst string) error {
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()

	out, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer out.Close()

	_, err = io.Copy(out, in)
	if err != nil {
		return err
	}
	return out.Sync()
}

// Handler: POST /api/admin/check-demo-lockout
func handleManualLockoutCheck(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	if err := checkAndLockoutDemoUsers(); err != nil {
		http.Error(w, "Manual check failed: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "Manual demo lockout check (including Day 2 warnings and Day 5 purges) completed successfully.",
	})
}

type ApproveUserRequest struct {
	GoogleSub   string `json:"googleSub"`
	Email       string `json:"email"`
	DisplayName string `json:"displayName"`
	PinHash     string `json:"pinHash"`
}

type RejectUserRequest struct {
	GoogleSub string `json:"googleSub"`
}

type DemoUserResponse struct {
	GoogleSub    string `json:"googleSub"`
	Email        string `json:"email"`
	DisplayName  string `json:"displayName"`
	RegisteredAt int64  `json:"registeredAt"`
	IsActive     bool   `json:"isActive"`
	TenantId     string `json:"tenantId"`
}

func handleGetDemoUsers(w http.ResponseWriter, r *http.Request) {
	// CORS
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	authHeader := r.Header.Get("Authorization")
	if authHeader != adminAuthToken {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	syncDatabaseUsersAndTenants()

	rows, err := db.Query(`SELECT "googleSub", "email", COALESCE("displayName", ''), "registeredAt", "isActive", COALESCE("tenantId", '') FROM "local_users" WHERE "isPremium" = FALSE ORDER BY "registeredAt" DESC`)
	if err != nil {
		http.Error(w, "Database query failed: "+err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	users := []DemoUserResponse{}
	for rows.Next() {
		var u DemoUserResponse
		if err := rows.Scan(&u.GoogleSub, &u.Email, &u.DisplayName, &u.RegisteredAt, &u.IsActive, &u.TenantId); err != nil {
			log.Printf("Failed to scan demo user: %v", err)
			continue
		}
		users = append(users, u)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(users)
}

func handleInspectTenant(w http.ResponseWriter, r *http.Request) {
	// CORS headers
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}
	if r.Method != http.MethodGet && r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	authHeader := r.Header.Get("Authorization")
	if authHeader != adminAuthToken {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	tenantID := r.URL.Query().Get("tenantId")
	if tenantID == "" {
		http.Error(w, "tenantId parameter is required", http.StatusBadRequest)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	if r.Method == http.MethodPost && r.URL.Query().Get("purge") == "true" {
		log.Printf("[InspectTenant] Purging tenant data via client request: %s", tenantID)
		purgeTenantData(tenantID)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{"success": true, "message": "Tenant data purged successfully"})
		return
	}

	counts := make(map[string]int)
	tables := []string{
		"bmp_payrolls",
		"bmp_clients",
		"bmp_invoices",
		"bmp_master_products",
		"bmp_cashflow",
		"bmp_settings",
		"bmp_employees",
		"bmp_bahan_baku",
		"print_settings",
		"products",
		"customers",
		"transactions",
		"activity_logs",
		"employees",
		"outlets",
		"bmp_device_tenants",
	}

	for _, table := range tables {
		var count int
		query := fmt.Sprintf(`SELECT COUNT(*) FROM "%s" WHERE "tenantId" = $1`, table)
		err := db.QueryRow(query, tenantID).Scan(&count)
		if err != nil {
			// Skip or mark error
			counts[table] = -1
		} else {
			counts[table] = count
		}
	}

	// Also check if the tenant exists in tenants table
	var tenantExists bool
	err := db.QueryRow(`SELECT EXISTS(SELECT 1 FROM "tenants" WHERE "id" = $1)`, tenantID).Scan(&tenantExists)
	if err != nil {
		log.Printf("Error checking tenants existence: %v", err)
	}

	response := map[string]interface{}{
		"tenantId":     tenantID,
		"tenantExists": tenantExists,
		"counts":       counts,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func handleRejectUser(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	authHeader := r.Header.Get("Authorization")
	if authHeader != adminAuthToken {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	var reqData RejectUserRequest
	if err := json.NewDecoder(r.Body).Decode(&reqData); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	if reqData.GoogleSub == "" {
		http.Error(w, "googleSub is required", http.StatusBadRequest)
		return
	}

	err := deleteLocalUser(reqData.GoogleSub)
	if err != nil {
		http.Error(w, "Failed to delete user: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "User successfully rejected and deleted",
	})
}

func handleApproveUser(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	authHeader := r.Header.Get("Authorization")
	if authHeader != adminAuthToken {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	var reqData ApproveUserRequest
	if err := json.NewDecoder(r.Body).Decode(&reqData); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	if reqData.GoogleSub == "" || reqData.Email == "" || reqData.PinHash == "" {
		http.Error(w, "googleSub, email, and pinHash are required", http.StatusBadRequest)
		return
	}

	premiumTenantId, err := upgradeUserToPremium(reqData.GoogleSub, reqData.Email, reqData.DisplayName, reqData.PinHash)
	if err != nil {
		http.Error(w, "Failed to upgrade user: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Sync database records instantly
	syncDatabaseUsersAndTenants()

	// Broadcast WS message
	wsMsg := map[string]interface{}{
		"type":      "user_upgraded",
		"googleSub": reqData.GoogleSub,
		"email":     reqData.Email,
	}
	msgBytes, _ := json.Marshal(wsMsg)
	broadcastWSMessage(string(msgBytes))

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "User successfully approved to Premium",
		"tenantId": premiumTenantId,
	})
}

type ApproveDemoRequest struct {
	GoogleSub string `json:"googleSub"`
}

type RejectDemoRequest struct {
	GoogleSub string `json:"googleSub"`
}

func handleApproveDemo(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	authHeader := r.Header.Get("Authorization")
	if authHeader != adminAuthToken {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	var reqData ApproveDemoRequest
	if err := json.NewDecoder(r.Body).Decode(&reqData); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	if reqData.GoogleSub == "" {
		http.Error(w, "googleSub is required", http.StatusBadRequest)
		return
	}

	nowMillis := time.Now().UnixNano() / int64(time.Millisecond)

	_, err := db.Exec(`UPDATE "local_users" SET "isActive" = TRUE, "updatedAt" = $1 WHERE "googleSub" = $2`, nowMillis, reqData.GoogleSub)
	if err != nil {
		http.Error(w, "Failed to approve demo user: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "Demo user successfully approved",
	})
}

func handleRejectDemo(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	authHeader := r.Header.Get("Authorization")
	if authHeader != adminAuthToken {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	var reqData RejectDemoRequest
	if err := json.NewDecoder(r.Body).Decode(&reqData); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	if reqData.GoogleSub == "" {
		http.Error(w, "googleSub is required", http.StatusBadRequest)
		return
	}

	err := deleteLocalUser(reqData.GoogleSub)
	if err != nil {
		http.Error(w, "Failed to delete demo user: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "Demo user successfully rejected and deleted",
	})
}

// SignatureRequest menerima invoiceId sebagai string atau number dari JS
type SignatureRequest struct {
	InvoiceIdRaw    json.RawMessage `json:"invoiceId"`
	InvoiceId       int64           `json:"-"` // hasil parse dari InvoiceIdRaw
	TenantId        string          `json:"tenantId"`
	Token           string          `json:"token"`
	SignatureUrl    string          `json:"signatureUrl"`
	SignatureBase64 string          `json:"signatureBase64"`
	ReceiverName    string          `json:"receiverName"`
}

// parseInvoiceId meng-parse invoiceId yang bisa berupa number (42) atau string ("42")
func parseInvoiceId(raw json.RawMessage) (int64, error) {
	if len(raw) == 0 {
		return 0, fmt.Errorf("invoiceId is empty")
	}
	// Coba parse sebagai number langsung
	var numVal int64
	if err := json.Unmarshal(raw, &numVal); err == nil {
		return numVal, nil
	}
	// Kalau gagal, coba parse sebagai string lalu konversi
	var strVal string
	if err := json.Unmarshal(raw, &strVal); err != nil {
		return 0, fmt.Errorf("invoiceId harus berupa angka atau string angka")
	}
	return strconv.ParseInt(strings.TrimSpace(strVal), 10, 64)
}

func handleSaveSignature(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var reqData SignatureRequest
	if err := json.NewDecoder(r.Body).Decode(&reqData); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Parse invoiceId secara fleksibel (bisa dari JS string atau number)
	invoiceId, err := parseInvoiceId(reqData.InvoiceIdRaw)
	if err != nil || invoiceId <= 0 {
		http.Error(w, fmt.Sprintf("invoiceId tidak valid (raw: %s): %v", string(reqData.InvoiceIdRaw), err), http.StatusBadRequest)
		return
	}
	reqData.InvoiceId = invoiceId

	if reqData.ReceiverName == "" || (reqData.SignatureUrl == "" && reqData.SignatureBase64 == "") {
		http.Error(w, "Missing required fields: receiverName, and signatureUrl or signatureBase64", http.StatusBadRequest)
		return
	}

	savedUrl := reqData.SignatureUrl

	if reqData.SignatureBase64 != "" {
		// Buat folder TTD jika belum ada
		if err := os.MkdirAll("./TTD", 0755); err != nil {
			http.Error(w, "Failed to create TTD directory: "+err.Error(), http.StatusInternalServerError)
			return
		}

		base64Data := reqData.SignatureBase64
		if idx := strings.Index(base64Data, ","); idx != -1 {
			base64Data = base64Data[idx+1:]
		}

		decoded, err := base64.StdEncoding.DecodeString(base64Data)
		if err != nil {
			http.Error(w, "Invalid base64 signature: "+err.Error(), http.StatusBadRequest)
			return
		}

		// Generate nama file aman berdasarkan nama penerima
		cleanName := cleanFilename(reqData.ReceiverName)
		fileName := fmt.Sprintf("sig_%d_%s.png", reqData.InvoiceId, cleanName)
		filePath := filepath.Join("./TTD", fileName)

		if err := os.WriteFile(filePath, decoded, 0644); err != nil {
			http.Error(w, "Failed to save signature file: "+err.Error(), http.StatusInternalServerError)
			return
		}

		savedUrl = fmt.Sprintf("https://www.zedmz.cloud/api/signatures/%s", fileName)
	}

	// Update local database & fetch clientId/tenantId
	var clientId sql.NullInt64
	var dbTenantId string
	err = db.QueryRow(`SELECT "clientId", "tenantId" FROM "bmp_invoices" WHERE "id" = $1`, reqData.InvoiceId).Scan(&clientId, &dbTenantId)
	if err != nil {
		log.Printf("[Warning] Failed to find clientId/tenantId in local DB for invoice %d: %v", reqData.InvoiceId, err)
	}

	updatedAt := time.Now().UnixNano() / int64(time.Millisecond)

	// Update local bmp_invoices
	_, err = db.Exec(`UPDATE "bmp_invoices" SET "receiverSignatureUrl" = $1, "receiverNameActual" = $2, "updatedAt" = $3 WHERE "id" = $4`, savedUrl, reqData.ReceiverName, updatedAt, reqData.InvoiceId)
	if err != nil {
		log.Printf("[Warning] Failed to update local bmp_invoices for invoice %d: %v", reqData.InvoiceId, err)
	}

	// Update local bmp_clients if clientId is valid
	if clientId.Valid && clientId.Int64 > 0 {
		_, err = db.Exec(`UPDATE "bmp_clients" SET "receiverSignatureUrl" = $1, "receiverNameActual" = $2, "updatedAt" = $3 WHERE "id" = $4`, savedUrl, reqData.ReceiverName, updatedAt, clientId.Int64)
		if err != nil {
			log.Printf("[Warning] Failed to update local bmp_clients for client %d: %v", clientId.Int64, err)
		}
	}

	tenantId := dbTenantId
	if tenantId == "" {
		tenantId = reqData.TenantId
	}

	log.Printf("[Signature] Successfully saved receiver signature for invoice %d (URL: %s)", reqData.InvoiceId, savedUrl)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"message":      "Signature saved successfully",
		"signatureUrl": savedUrl,
	})
}

func handleDeleteSignature(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var reqData struct {
		InvoiceId int64  `json:"invoiceId"`
		TenantId  string `json:"tenantId"`
	}
	if err := json.NewDecoder(r.Body).Decode(&reqData); err != nil {
		http.Error(w, "Invalid request body: " + err.Error(), http.StatusBadRequest)
		return
	}

	if reqData.InvoiceId <= 0 {
		http.Error(w, "Invalid invoiceId", http.StatusBadRequest)
		return
	}

	// 1. Fetch clientId & tenantId from local DB
	var clientId sql.NullInt64
	var dbTenantId string
	err := db.QueryRow(`SELECT "clientId", "tenantId" FROM "bmp_invoices" WHERE "id" = $1`, reqData.InvoiceId).Scan(&clientId, &dbTenantId)
	if err != nil {
		log.Printf("[Warning] Failed to find clientId/tenantId for invoice %d: %v", reqData.InvoiceId, err)
	}

	tenantId := dbTenantId
	if tenantId == "" {
		tenantId = reqData.TenantId
	}

	updatedAt := time.Now().UnixNano() / int64(time.Millisecond)

	// 2. Clear local database
	_, err = db.Exec(`UPDATE "bmp_invoices" SET "receiverSignatureUrl" = NULL, "receiverNameActual" = NULL, "updatedAt" = $1 WHERE "id" = $2`, updatedAt, reqData.InvoiceId)
	if err != nil {
		log.Printf("[Warning] Failed to clear signature in local bmp_invoices: %v", err)
	}

	if clientId.Valid && clientId.Int64 > 0 {
		_, err = db.Exec(`UPDATE "bmp_clients" SET "receiverSignatureUrl" = NULL, "receiverNameActual" = NULL, "updatedAt" = $1 WHERE "id" = $2`, updatedAt, clientId.Int64)
		if err != nil {
			log.Printf("[Warning] Failed to clear signature in local bmp_clients: %v", err)
		}
	}

	// 3. Delete physical files in ./TTD starting with sig_<invoiceId>_
	files, err := os.ReadDir("./TTD")
	if err == nil {
		prefix := fmt.Sprintf("sig_%d_", reqData.InvoiceId)
		for _, file := range files {
			if !file.IsDir() && strings.HasPrefix(file.Name(), prefix) {
				filePath := filepath.Join("./TTD", file.Name())
				if err := os.Remove(filePath); err != nil {
					log.Printf("[Warning] Failed to delete signature file %s: %v", file.Name(), err)
				} else {
					log.Printf("[Signature] Deleted signature file %s", file.Name())
				}
			}
		}
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "Signature deleted successfully",
	})
}

// Handler: GET /api/invoice/signature-status?id=<invoiceId>
func handleSignatureStatus(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	invoiceIdStr := r.URL.Query().Get("id")
	if invoiceIdStr == "" {
		http.Error(w, "Missing invoice id", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")

	// Scan TTD folder
	files, err := os.ReadDir("./TTD")
	if err != nil {
		// TTD folder doesn't exist yet or other error, return empty array
		w.Write([]byte("[]"))
		return
	}

	prefix := fmt.Sprintf("sig_%s_", invoiceIdStr)
	for _, file := range files {
		if !file.IsDir() && strings.HasPrefix(file.Name(), prefix) && strings.HasSuffix(file.Name(), ".png") {
			fileName := file.Name()
			// Extract receiver name
			// Format: sig_<invoiceId>_<receiverName>.png
			temp := strings.TrimPrefix(fileName, prefix)
			receiverName := strings.TrimSuffix(temp, ".png")
			receiverName = strings.ReplaceAll(receiverName, "_", " ")

			savedUrl := fmt.Sprintf("https://www.zedmz.cloud/api/signatures/%s", fileName)

			response := []map[string]interface{}{
				{
					"receiverSignatureUrl": savedUrl,
					"receiverNameActual":   receiverName,
				},
			}
			json.NewEncoder(w).Encode(response)
			return
		}
	}

	// No signature found
	w.Write([]byte("[]"))
}

type AiClassifyRequest struct {
	Statement string `json:"statement"`
}

var trainingData = map[string][]string{
	"SAVE_SIGNATURE_VPS": {
		"simpan di vps aja ketika ada yang ttd untuk penerima invoice",
		"simpan tanda tangan di vps",
		"buat folder ttd di server",
		"jangan pakai cloudinary lagi, simpan lokal di vps",
		"taruh file ttd di folder vps",
		"simpan ttd di vps lokal",
		"simpan di vps aja",
		"buat folder TTD di vps",
		"simpan gambar ttd lokal di server",
	},
	"SAVE_SIGNATURE_CLOUDINARY": {
		"simpan tanda tangan di cloudinary",
		"upload ttd ke cloudinary",
		"pakai cloudinary untuk ttd",
		"simpan gambar ttd ke cloud",
		"upload ke cloudinary",
	},
	"CHANGE_DOMAIN": {
		"ubah domain web ke zedmz.cloud",
		"pakai domain www.posbah.com",
		"ganti domain server",
		"ubah settingan dns atau domain",
		"pakai https://www.zedmz.cloud",
	},
	"DETECT_APK_VERSION": {
		"deteksi versi apk userdemo dan userpremium",
		"cek versi aplikasi yang digunakan user",
		"apakah user menggunakan versi terbaru",
		"deteksi update otomatis apk",
		"sistem deteksi versi apk user",
		"cek update versi paling akhir",
		"jangan berikan banner pembaruan jika versi terbaru",
		"otomatis deteksi update versi apk",
	},
}

var nonAlphanumericRegex = regexp.MustCompile(`[^a-z0-9\s]`)

func tokenize(text string) []string {
	text = strings.ToLower(text)
	text = nonAlphanumericRegex.ReplaceAllString(text, "")
	return strings.Fields(text)
}

func getTF(tokens []string) map[string]int {
	tf := make(map[string]int)
	for _, token := range tokens {
		tf[token]++
	}
	return tf
}

var (
	vocab       = make(map[string]bool)
	idf         = make(map[string]float64)
	docs        [][]string
	docLabels   []string
	isModelInit = false
)

func initTfidfModel() {
	if isModelInit {
		return
	}
	for label, texts := range trainingData {
		for _, text := range texts {
			tokens := tokenize(text)
			for _, token := range tokens {
				vocab[token] = true
			}
			docs = append(docs, tokens)
			docLabels = append(docLabels, label)
		}
	}

	numDocs := float64(len(docs))
	for term := range vocab {
		docCount := 0
		for _, doc := range docs {
			contains := false
			for _, t := range doc {
				if t == term {
					contains = true
					break
				}
			}
			if contains {
				docCount++
			}
		}
		idf[term] = math.Log(numDocs / (1.0 + float64(docCount)))
	}
	isModelInit = true
}

func getTfidfVector(text string) map[string]float64 {
	initTfidfModel()
	tokens := tokenize(text)
	tf := getTF(tokens)
	vector := make(map[string]float64)
	for term := range vocab {
		if count, exists := tf[term]; exists {
			vector[term] = float64(count) * idf[term]
		} else {
			vector[term] = 0.0
		}
	}
	return vector
}

func cosineSimilarity(v1, v2 map[string]float64) float64 {
	dotProduct := 0.0
	sumV1 := 0.0
	sumV2 := 0.0
	for term := range vocab {
		val1 := v1[term]
		val2 := v2[term]
		dotProduct += val1 * val2
		sumV1 += val1 * val1
		sumV2 += val2 * val2
	}
	magnitudeV1 := math.Sqrt(sumV1)
	magnitudeV2 := math.Sqrt(sumV2)
	if magnitudeV1 == 0 || magnitudeV2 == 0 {
		return 0.0
	}
	return dotProduct / (magnitudeV1 * magnitudeV2)
}

func classifyGo(inputText string) (string, float64) {
	inputLower := strings.ToLower(inputText)

	// Check simple keyword rules first for absolute accuracy
	if strings.Contains(inputLower, "simpan") && (strings.Contains(inputLower, "vps") || strings.Contains(inputLower, "folder") || strings.Contains(inputLower, "lokal") || strings.Contains(inputLower, "ttd")) {
		return "SAVE_SIGNATURE_VPS", 1.0
	}
	if strings.Contains(inputLower, "cloudinary") {
		return "SAVE_SIGNATURE_CLOUDINARY", 1.0
	}
	if strings.Contains(inputLower, "domain") || strings.Contains(inputLower, "dns") || strings.Contains(inputLower, "zedmz") || strings.Contains(inputLower, "posbah.com") {
		return "CHANGE_DOMAIN", 1.0
	}
	if strings.Contains(inputLower, "apk") || strings.Contains(inputLower, "versi") || strings.Contains(inputLower, "update") || strings.Contains(inputLower, "pembaruan") {
		return "DETECT_APK_VERSION", 1.0
	}

	initTfidfModel()
	inputVector := getTfidfVector(inputText)
	bestScore := 0.0
	bestLabel := "UNKNOWN"

	for idx, doc := range docs {
		label := docLabels[idx]
		docVector := getTfidfVector(strings.Join(doc, " "))
		score := cosineSimilarity(inputVector, docVector)
		if score > bestScore {
			bestScore = score
			bestLabel = label
		}
	}

	if bestScore < 0.1 {
		return "UNKNOWN", bestScore
	}

	return bestLabel, bestScore
}

func handleAiClassify(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var reqData AiClassifyRequest
	if err := json.NewDecoder(r.Body).Decode(&reqData); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	if reqData.Statement == "" {
		http.Error(w, "Missing required field: statement", http.StatusBadRequest)
		return
	}

	label, score := classifyGo(reqData.Statement)

	w.Header().Set("Content-Type", "application/json")
	response := map[string]interface{}{
		"category":   label,
		"confidence": score,
	}
	json.NewEncoder(w).Encode(response)
}

func handleGetApkDownloadToken(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"token": "dummy-token",
	})
}

func handleDownloadApk(w http.ResponseWriter, r *http.Request) {
	autoDetectApkVersion()
	var version, description string
	if db != nil {
		_ = db.QueryRow(`SELECT "version", "description" FROM "apk_config" WHERE "id" = 1`).Scan(&version, &description)
	}
	if version == "" {
		version = "2.4.0" // Fallback
	}
	if description == "" {
		description = "Pembaruan sistem dan optimalisasi."
	}

	direct := r.URL.Query().Get("direct") == "true"
	if direct {
		apkPath := fmt.Sprintf("./posbah-v%s.apk", version)
		if _, err := os.Stat(apkPath); err != nil {
			// Try fallback to debug APK name
			apkPathDebug := fmt.Sprintf("./posbah-v%s-debug.apk", version)
			if _, errD := os.Stat(apkPathDebug); errD == nil {
				apkPath = apkPathDebug
			}
		}

		if _, err := os.Stat(apkPath); err == nil {
			filename := filepath.Base(apkPath)
			w.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=%s", filename))
			w.Header().Set("Content-Type", "application/vnd.android.package-archive")
			http.ServeFile(w, r, apkPath)
			return
		}
		// Fallback redirect to Google Drive
		http.Redirect(w, r, "https://drive.google.com/uc?export=download&id=1grCDSGp1qacBES1hcO29d_03HNPstdbM", http.StatusFound)
		return
	}

	// Serve the beautiful landing page
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	html := fmt.Sprintf(`<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dapatkan POSBah v%s</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        :root {
            --bg-gradient: linear-gradient(135deg, #0f172a 0%%, #1e1b4b 100%%);
            --primary: #f59e0b;
            --primary-hover: #d97706;
            --playstore: #10b981;
            --playstore-hover: #059669;
            --card-bg: rgba(30, 41, 59, 0.7);
            --card-border: rgba(255, 255, 255, 0.08);
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
        }
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }
        body {
            font-family: 'Outfit', sans-serif;
            background: var(--bg-gradient);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        .container {
            width: 100%%;
            max-width: 500px;
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 24px;
            padding: 40px 30px;
            text-align: center;
            backdrop-filter: blur(20px);
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
        }
        .logo {
            font-size: 32px;
            font-weight: 800;
            color: var(--primary);
            margin-bottom: 8px;
            letter-spacing: -0.5px;
        }
        .subtitle {
            font-size: 15px;
            color: var(--text-muted);
            margin-bottom: 30px;
        }
        .version-badge {
            display: inline-block;
            background: rgba(16, 185, 129, 0.15);
            color: var(--playstore);
            padding: 6px 16px;
            border-radius: 99px;
            font-weight: 600;
            font-size: 14px;
            margin-bottom: 24px;
            border: 1px solid rgba(16, 185, 129, 0.3);
        }
        .btn-playstore {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 12px;
            background: var(--playstore);
            color: #0f172a;
            text-decoration: none;
            padding: 16px 32px;
            border-radius: 16px;
            font-size: 16px;
            font-weight: 700;
            transition: all 0.3s ease;
            box-shadow: 0 4px 14px rgba(16, 185, 129, 0.3);
            margin-bottom: 16px;
        }
        .btn-playstore:hover {
            background: var(--playstore-hover);
            color: #ffffff;
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
        }
        .info-box {
            background: rgba(245, 158, 11, 0.08);
            border: 1px solid rgba(245, 158, 11, 0.2);
            border-radius: 16px;
            padding: 16px;
            text-align: left;
            font-size: 13px;
            color: #fbbf24;
            line-height: 1.6;
            margin-bottom: 24px;
        }
        .info-box i {
            margin-right: 6px;
        }
        .release-notes-box {
            text-align: left;
            background: rgba(15, 23, 42, 0.6);
            border: 1px solid var(--card-border);
            border-radius: 16px;
            padding: 20px;
            margin-top: 20px;
        }
        .release-notes-title {
            font-size: 14px;
            font-weight: 700;
            margin-bottom: 12px;
            color: var(--text-main);
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .release-notes-content {
            font-size: 13px;
            color: var(--text-muted);
            line-height: 1.6;
            white-space: pre-wrap;
        }
        .btn-legacy {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            background: transparent;
            color: var(--text-muted);
            text-decoration: none;
            padding: 10px 20px;
            border-radius: 12px;
            font-size: 13px;
            font-weight: 600;
            transition: all 0.3s ease;
            border: 1px dashed var(--card-border);
            margin-top: 24px;
        }
        .btn-legacy:hover {
            color: var(--primary);
            border-color: var(--primary);
            transform: translateY(-1px);
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">POSBah</div>
        <div class="subtitle">Aplikasi Android POS & Invoice Native</div>
        
        <div class="version-badge">Distribusi Resmi Google Play</div>
        
        <a href="https://play.google.com/apps/testing/com.posbah.app" target="_blank" class="btn-playstore">
            <i class="fa-brands fa-google-play"></i> Gabung & Unduh di Play Store
        </a>
        
        <div class="info-box">
            <i class="fa-solid fa-triangle-exclamation"></i>
            <strong>Penting untuk Penguji:</strong> Agar dapat mengunduh aplikasi di Google Play Store, alamat email Anda wajib didaftarkan terlebih dahulu sebagai <strong>Penguji (Tester)</strong> pada Closed Testing Google Play Console oleh Admin.
        </div>
        
        <div class="release-notes-box">
            <div class="release-notes-title">
                <i class="fa-solid fa-list-check" style="color: var(--primary);"></i> Catatan Pembaruan v%s:
            </div>
            <div class="release-notes-content">%s</div>
        </div>

        <a href="/api/download-apk?direct=true" class="btn-legacy">
            <i class="fa-solid fa-download"></i> Unduh APK Manual (Metode Lama)
        </a>
    </div>
</body>
</html>`, version, version, description)
	w.Write([]byte(html))
}
func handleApkVersion(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	if db == nil {
		json.NewEncoder(w).Encode(map[string]string{
			"version":     "2.4.0",
			"description": "Database not initialized. Fallback version.",
		})
		return
	}
	autoDetectApkVersion()
	var version, description, downloadUrl string
	err := db.QueryRow(`SELECT "version", "description", "downloadUrl" FROM "apk_config" WHERE "id" = 1`).Scan(&version, &description, &downloadUrl)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]string{
			"version":     "2.4.0",
			"description": "Query failed. Fallback version.",
		})
		return
	}
	json.NewEncoder(w).Encode(map[string]string{
		"version":     version,
		"description": description,
		"downloadUrl": downloadUrl,
	})
}

func cleanFilename(name string) string {
	var builder strings.Builder
	for _, ch := range strings.ToLower(name) {
		if (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') {
			builder.WriteRune(ch)
		} else if ch == ' ' || ch == '_' || ch == '-' {
			builder.WriteRune('_')
		}
	}
	res := builder.String()
	for strings.Contains(res, "__") {
		res = strings.ReplaceAll(res, "__", "_")
	}
	return strings.Trim(res, "_")
}

func handleSignPage(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Content-Type", "text/html")
	w.Write([]byte(signatureHtmlPage))
}

const signatureHtmlPage = `<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tanda Terima Digital - POSBah</title>
    <!-- Google Fonts Inter -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #3B82F6;
            --primary-hover: #2563EB;
            --bg-dark: #0F172A;
            --card-bg: rgba(30, 41, 59, 0.7);
            --border: rgba(255, 255, 255, 0.1);
            --text-main: #F8FAFC;
            --text-muted: #94A3B8;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Inter', sans-serif;
        }

        body {
            background-color: var(--bg-dark);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 16px;
            background-image: radial-gradient(circle at top right, rgba(59, 130, 246, 0.1), transparent),
                              radial-gradient(circle at bottom left, rgba(16, 185, 129, 0.05), transparent);
        }

        .container {
            width: 100%;
            max-width: 450px;
            background: var(--card-bg);
            backdrop-filter: blur(16px);
            border: 1px solid var(--border);
            border-radius: 24px;
            padding: 24px;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
            text-align: center;
        }

        .header {
            margin-bottom: 24px;
        }

        .logo-text {
            font-size: 24px;
            font-weight: 800;
            background: linear-gradient(to right, #3B82F6, #10B981);
            -webkit-background-clip: text;
            background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 4px;
        }

        .subtitle {
            font-size: 14px;
            color: var(--text-muted);
        }

        .form-group {
            text-align: left;
            margin-bottom: 20px;
        }

        label {
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--text-muted);
            margin-bottom: 8px;
            display: block;
        }

        input[type="text"] {
            width: 100%;
            padding: 12px 16px;
            background: rgba(15, 23, 42, 0.6);
            border: 1px solid var(--border);
            border-radius: 12px;
            color: var(--text-main);
            font-size: 15px;
            outline: none;
            transition: border-color 0.2s;
        }

        input[type="text"]:focus {
            border-color: var(--primary);
        }

        .canvas-container {
            position: relative;
            background: #FFFFFF;
            border-radius: 16px;
            overflow: hidden;
            margin-bottom: 20px;
            border: 1px solid var(--border);
        }

        canvas {
            display: block;
            width: 100%;
            height: 200px;
            cursor: crosshair;
        }

        .canvas-actions {
            display: flex;
            justify-content: space-between;
            margin-top: 12px;
        }

        .btn-clear {
            background: transparent;
            color: #EF4444;
            border: 1px solid rgba(239, 68, 68, 0.3);
            padding: 8px 16px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 500;
            cursor: pointer;
            transition: background 0.2s;
        }

        .btn-clear:hover {
            background: rgba(239, 68, 68, 0.1);
        }

        .btn-submit {
            width: 100%;
            background: var(--primary);
            color: white;
            border: none;
            padding: 14px;
            border-radius: 12px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.2s, transform 0.1s;
        }

        .btn-submit:hover {
            background: var(--primary-hover);
        }

        .btn-submit:active {
            transform: scale(0.98);
        }

        .btn-submit:disabled {
            background: rgba(59, 130, 246, 0.4);
            cursor: not-allowed;
            transform: none;
        }

        .status-message {
            margin-top: 16px;
            font-size: 13px;
            color: #10B981;
            display: none;
        }

        .status-message.error {
            color: #EF4444;
        }

        .expiry-banner {
            font-size: 12px;
            color: #F59E0B;
            background: rgba(245, 158, 11, 0.1);
            border: 1px solid rgba(245, 158, 11, 0.2);
            padding: 8px 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            display: inline-block;
        }

        .expiry-banner.expired {
            color: #EF4444;
            background: rgba(239, 68, 68, 0.1);
            border: 1px solid rgba(239, 68, 68, 0.2);
        }

        .statement-box {
            background: rgba(59, 130, 246, 0.1);
            border: 1px solid rgba(59, 130, 246, 0.3);
            border-left: 4px solid var(--primary);
            border-radius: 10px;
            padding: 12px 16px;
            margin-bottom: 20px;
            font-size: 13px;
            line-height: 1.6;
            color: #E2E8F0;
            text-align: left;
        }

        .instruction-box {
            font-size: 13px;
            font-weight: 600;
            color: #F59E0B;
            margin-bottom: 8px;
            text-align: left;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .btn-tc {
            width: 100%;
            background: rgba(59, 130, 246, 0.1);
            color: #3B82F6;
            border: 1px solid rgba(59, 130, 246, 0.3);
            padding: 12px;
            border-radius: 12px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
            margin-bottom: 24px;
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 8px;
        }

        .btn-tc:hover {
            background: rgba(59, 130, 246, 0.2);
            border-color: #3B82F6;
            color: #60A5FA;
        }

        /* Terms and Conditions Modal */
        .tc-modal {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(15, 23, 42, 0.85);
            backdrop-filter: blur(8px);
            z-index: 2000;
            justify-content: center;
            align-items: center;
            padding: 16px;
        }

        .tc-modal-content {
            background: #1E293B;
            border: 1px solid var(--border);
            border-radius: 24px;
            width: 100%;
            max-width: 420px;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
            animation: modalFadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
            display: flex;
            flex-direction: column;
            max-height: 85vh;
        }

        @keyframes modalFadeIn {
            from { transform: scale(0.95); opacity: 0; }
            to { transform: scale(1); opacity: 1; }
        }

        .tc-modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 18px 20px;
            border-bottom: 1px solid var(--border);
        }

        .tc-modal-header h2 {
            font-size: 18px;
            font-weight: 700;
            background: linear-gradient(to right, #3B82F6, #10B981);
            -webkit-background-clip: text;
            background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .tc-close {
            background: transparent;
            border: none;
            color: var(--text-muted);
            font-size: 28px;
            line-height: 1;
            cursor: pointer;
            transition: color 0.2s;
            padding: 0 4px;
        }

        .tc-close:hover {
            color: #EF4444;
        }

        .tc-modal-body {
            padding: 20px;
            overflow-y: auto;
            text-align: left;
            font-size: 13px;
            line-height: 1.6;
            color: #E2E8F0;
        }

        .tc-modal-body p {
            margin-bottom: 12px;
        }

        .tc-modal-body strong {
            color: #3B82F6;
            display: block;
            margin-top: 12px;
            margin-bottom: 6px;
        }

        .tc-modal-body strong:first-child {
            margin-top: 0;
        }

        .tc-modal-footer {
            padding: 16px 20px;
            border-top: 1px solid var(--border);
        }

        /* Overlay loading */
        .loading-overlay {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(15, 23, 42, 0.8);
            z-index: 1000;
            justify-content: center;
            align-items: center;
            flex-direction: column;
        }

        .spinner {
            width: 40px;
            height: 40px;
            border: 4px solid rgba(255, 255, 255, 0.1);
            border-left-color: var(--primary);
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin-bottom: 12px;
        }

        @keyframes spin {
            to { transform: rotate(360deg); }
        }
    </style>
</head>
<body>

    <div class="container">
        <div class="header">
            <h1 class="logo-text">POSBah</h1>
            <p class="subtitle">Konfirmasi Tanda Terima Barang</p>
        </div>

        <div id="expiry-tag" class="expiry-banner">
            Mengecek kevalidan link...
        </div>

        <div id="form-container" style="display: none;">
            <div class="instruction-box">
                <span>⚠️</span>
                <span>Pahami Terlebih Dahulu Syarat & Ketentuan:</span>
            </div>
            <button type="button" class="btn-tc" id="tc-btn">📜 Syarat & Ketentuan Transaksi</button>

            <div class="form-group">
                <label for="receiver-name">Nama Terang Penerima</label>
                <input type="text" id="receiver-name" placeholder="Masukkan nama Anda..." autocomplete="off">
            </div>

            <label>Tanda Tangan di Bawah</label>
            <div class="canvas-container">
                <canvas id="sig-canvas"></canvas>
            </div>
            
            <div style="margin-bottom: 20px; text-align: left;">
                <button type="button" class="btn-clear" id="clear-btn">Ulangi Corengan</button>
            </div>

            <button type="button" class="btn-submit" id="submit-btn" disabled>Kirim Bukti Tanda Tangan</button>
        </div>

        <div id="status-tag" class="status-message"></div>
    </div>

    <!-- Modal Syarat & Ketentuan -->
    <div class="tc-modal" id="tc-modal">
        <div class="tc-modal-content">
            <div class="tc-modal-header">
                <h2>Syarat & Ketentuan Transaksi</h2>
                <button type="button" class="tc-close" id="tc-close-btn">&times;</button>
            </div>
            <div class="tc-modal-body">
                <strong>PERNYATAAN HUKUM & PERSETUJUAN TTE</strong>
                <p>Dengan menandatangani dokumen ini, saya menyatakan secara sadar bahwa yang bertanda tangan di bawah ini adalah saya sendiri secara sah mewakili Klien, memiliki wewenang penuh untuk menerima barang, serta setuju bahwa tanda tangan digital ini akan disimpan di sistem basis data dan digunakan kembali sebagai bukti tanda terima yang sah dan mengikat secara hukum pada invoice dan surat jalan di kemudian hari secara luring maupun daring.</p>

                <strong>1. KEABSAHAN PENERIMAAN BARANG</strong>
                <p>Dengan membubuhkan tanda tangan elektronik ini, Penerima (atas nama Klien) menyatakan telah memeriksa dan menerima produk dalam keadaan lengkap, baik, dan sesuai dengan pesanan. Segala klaim atas kerusakan atau kekurangan barang setelah penandatanganan ini wajib dilaporkan maksimal 1x24 jam.</p>
                
                <strong>2. KETENTUAN JATUH TEMPO (TEMPO)</strong>
                <p>Untuk transaksi tempo, Klien berkewajiban melunasi seluruh pembayaran sebelum tanggal jatuh tempo yang tertera pada Invoice. Hak kepemilikan atas produk tetap berada sepenuhnya pada Penjual dan baru beralih setelah Invoice dilunasi secara penuh.</p>
                
                <strong>3. LEGALITAS TANDA TANGAN ELEKTRONIK (TTE)</strong>
                <p>Tanda tangan elektronik ini merupakan alat bukti digital yang sah, mengikat, dan memiliki kekuatan hukum yang setara dengan tanda tangan basah di bawah Pasal 11 UU ITE. Klien menyatakan persetujuan atas penyimpanan dan penggunaan kembali tanda tangan ini pada dokumen invoice dan surat jalan berikutnya.</p>
            </div>
            <div class="tc-modal-footer">
                <button type="button" class="btn-submit" id="tc-agree-btn">Saya Mengerti & Setuju</button>
            </div>
        </div>
    </div>

    <!-- Loading screen overlay -->
    <div class="loading-overlay" id="loader">
        <div class="spinner"></div>
        <p id="loader-text">Memproses tanda tangan...</p>
    </div>

    <script>
        // ─── Cloudinary Config ───
        const CLOUDINARY_CLOUD_NAME = "dkkbizenf";
        const CLOUDINARY_PRESET = "nota_bahan_baku";
        const CLOUDINARY_FOLDER = "posbah/nota_penerima";
        // ───────────────────────────────────────────────────────

        const canvas = document.getElementById("sig-canvas");
        const ctx = canvas.getContext("2d");
        const clearBtn = document.getElementById("clear-btn");
        const submitBtn = document.getElementById("submit-btn");
        const receiverNameInput = document.getElementById("receiver-name");
        const formContainer = document.getElementById("form-container");
        const statusTag = document.getElementById("status-tag");
        const expiryTag = document.getElementById("expiry-tag");
        const loader = document.getElementById("loader");
        const loaderText = document.getElementById("loader-text");

        // Syarat & Ketentuan Modal DOM Elements
        const tcBtn = document.getElementById("tc-btn");
        const tcModal = document.getElementById("tc-modal");
        const tcCloseBtn = document.getElementById("tc-close-btn");
        const tcAgreeBtn = document.getElementById("tc-agree-btn");

        let isDrawing = false;
        let drawnSomething = false;
        let invoiceId = null;
        let tenantId = "";
        let tokenStr = null;

        // Setup responsivitas canvas
        function resizeCanvas() {
            const rect = canvas.getBoundingClientRect();
            canvas.width = rect.width;
            canvas.height = 200;
            
            // Default styling canvas
            ctx.strokeStyle = "#000000";
            ctx.lineWidth = 4;
            ctx.lineCap = "round";
            ctx.lineJoin = "round";
            clearCanvas();
        }

        window.addEventListener("resize", resizeCanvas);

        // Bersihkan canvas
        function clearCanvas() {
            ctx.fillStyle = "#FFFFFF";
            ctx.fillRect(0, 0, canvas.width, canvas.height);
            drawnSomething = false;
            validateForm();
        }

        clearBtn.addEventListener("click", clearCanvas);

        // Deteksi input gambar (Touch & Mouse)
        function getPos(e) {
            const rect = canvas.getBoundingClientRect();
            const clientX = e.touches ? e.touches[0].clientX : e.clientX;
            const clientY = e.touches ? e.touches[0].clientY : e.clientY;
            return {
                x: clientX - rect.left,
                y: clientY - rect.top
            };
        }

        // Touch start
        function startDrawing(e) {
            e.preventDefault();
            isDrawing = true;
            const pos = getPos(e);
            ctx.beginPath();
            ctx.moveTo(pos.x, pos.y);
        }

        // Touch move
        function draw(e) {
            if (!isDrawing) return;
            e.preventDefault();
            const pos = getPos(e);
            ctx.lineTo(pos.x, pos.y);
            ctx.stroke();
            drawnSomething = true;
            validateForm();
        }

        // Touch end
        function stopDrawing() {
            isDrawing = false;
        }

        canvas.addEventListener("mousedown", startDrawing);
        canvas.addEventListener("mousemove", draw);
        canvas.addEventListener("mouseup", stopDrawing);
        canvas.addEventListener("mouseleave", stopDrawing);

        canvas.addEventListener("touchstart", startDrawing);
        canvas.addEventListener("touchmove", draw);
        canvas.addEventListener("touchend", stopDrawing);

        // Form validation
        function validateForm() {
            const name = receiverNameInput.value.trim();
            submitBtn.disabled = !(name.length > 1 && drawnSomething);
        }

        receiverNameInput.addEventListener("input", validateForm);

        // Syarat & Ketentuan Modal Handlers
        tcBtn.addEventListener("click", function() {
            tcModal.style.display = "flex";
        });

        function closeTcModal() {
            tcModal.style.display = "none";
        }

        tcCloseBtn.addEventListener("click", closeTcModal);
        tcAgreeBtn.addEventListener("click", closeTcModal);

        window.addEventListener("click", function(event) {
            if (event.target === tcModal) {
                closeTcModal();
            }
        });

        // Parser Token URL
        function parseUrlToken() {
            // Cek dulu apakah ada query parameter 'token'
            const urlParams = new URLSearchParams(window.location.search);
            let token = urlParams.get("token");
            
            // Jika tidak ada, baru fallback ke path parameter (segmen terakhir URL)
            if (!token) {
                const pathParts = window.location.pathname.split("/");
                token = pathParts[pathParts.length - 1];
            }

            if (!token || token === "signature_receiver_web.html" || token === "sign" || token === "") {
                showExpiryError("Token tidak ditemukan di URL. Minta pengirim membagikan ulang link.");
                return;
            }

            tokenStr = token;
            decodeAndVerifyToken(token);
        }

        // Dekode Token & Verifikasi Kadaluarsa
        function decodeAndVerifyToken(tokenEncoded) {
            try {
                // Decode base64 URL safe
                let base64 = tokenEncoded.replace(/-/g, "+").replace(/_/g, "/");
                while (base64.length % 4) {
                    base64 += "=";
                }
                let tokenRaw = atob(base64);
                const parts = tokenRaw.split(":");
                if (parts.length < 2) {
                    showExpiryError("Format token tidak valid.");
                    return;
                }

                let expiry = 0;
                if (parts.length >= 4) {
                    tenantId = parts[0];
                    invoiceId = parts[1];
                    expiry = parseInt(parts[2]);
                } else {
                    tenantId = "";
                    invoiceId = parts[0];
                    expiry = parseInt(parts[1]);
                }
                
                // Cek kadaluarsa
                const now = Date.now();
                if (now > expiry) {
                    showExpiryError("Link ini telah kadaluarsa. Minta pengirim membuat link baru.");
                    return;
                }

                // Sukses — Link Valid
                const timeLeftSec = Math.round((expiry - now) / 1000);
                startCountdown(timeLeftSec);
                
                formContainer.style.display = "block";
                resizeCanvas();

            } catch (e) {
                showExpiryError("Gagal mendekripsi token. Pastikan link yang Anda gunakan valid.");
            }
        }

        function showExpiryError(msg) {
            expiryTag.className = "expiry-banner expired";
            expiryTag.innerText = "⚠️ LINK KADALUARSA ATAU ERROR";
            formContainer.style.display = "none";
            showStatus(msg, true);
        }

        function startCountdown(durationSeconds) {
            let sec = durationSeconds;
            expiryTag.innerText = "⏳ Link aktif selama " + sec + " detik lagi";
            const timer = setInterval(function() {
                sec--;
                if (sec <= 0) {
                    clearInterval(timer);
                    showExpiryError("Waktu habis! Link kadaluarsa.");
                } else {
                    expiryTag.innerText = "⏳ Link aktif selama " + sec + " detik lagi";
                }
            }, 1000);
        }

        function showStatus(msg, isError) {
            statusTag.style.display = "block";
            statusTag.innerText = msg;
            if (isError) {
                statusTag.className = "status-message error";
            } else {
                statusTag.className = "status-message";
            }
        }

        // Kirim tanda tangan dalam format Base64 langsung ke server VPS
        submitBtn.addEventListener("click", async function() {
            const name = receiverNameInput.value.trim();
            if (!name || !drawnSomething || !invoiceId) return;

            showLoader("Menyimpan tanda tangan...");

            try {
                // 1. Ekspor canvas ke Base64 (data URL)
                const signatureBase64 = canvas.toDataURL("image/png");

                // 2. Kirim ke API server backend
                const apiRes = await fetch("/api/invoice/signature", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        invoiceId: invoiceId,
                        tenantId: tenantId,
                        token: tokenStr,
                        signatureBase64: signatureBase64,
                        receiverName: name
                    })
                });

                hideLoader();

                if (apiRes.ok) {
                    formContainer.style.display = "none";
                    expiryTag.style.display = "none";
                    showStatus("Tanda terima berhasil dikirim! Terima kasih, " + name + ".");
                } else {
                    const errText = await apiRes.text();
                    throw new Error(errText || "Gagal memproses tanda tangan di database server.");
                }

            } catch (e) {
                hideLoader();
                showStatus("Gagal mengirim: " + e.message, true);
            }
        });

        function showLoader(text) {
            loaderText.innerText = text;
            loader.style.display = "flex";
        }

        function hideLoader() {
            loader.style.display = "none";
        }

        // Jalankan parser saat halaman di-load
        window.onload = parseUrlToken;
    </script>
</body>
</html>
`

type StoreProduct struct {
	ID       string  `json:"id"`
	Name     string  `json:"name"`
	Category string  `json:"category"`
	Price    float64 `json:"price"`
	Stock    int     `json:"stock"`
	Image    string  `json:"image"`
}

func handleStorePage(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	path := r.URL.Path
	var tokenEncoded string
	if strings.HasPrefix(path, "/store/") {
		tokenEncoded = strings.TrimPrefix(path, "/store/")
	} else if strings.HasPrefix(path, "/api/store/") {
		tokenEncoded = strings.TrimPrefix(path, "/api/store/")
	}

	tokenEncoded = strings.TrimSpace(tokenEncoded)
	if tokenEncoded == "" {
		http.Error(w, "Token required", http.StatusBadRequest)
		return
	}

	tenantId, err := validateStoreToken(tokenEncoded)
	if err != nil || tenantId == "" {
		w.Header().Set("Content-Type", "text/html; charset=utf-8")
		w.WriteHeader(http.StatusNotFound)
		w.Write([]byte(renderStoreErrorPage("Link Toko Online Tidak Valid", "Link toko online ini tidak valid, salah format, atau telah kedaluwarsa.")))
		return
	}

	products, err := loadTenantProducts(tenantId)
	if err != nil {
		log.Printf("Error loading products for tenant %s: %v", tenantId, err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	businessName := "Toko Online"
	_ = db.QueryRow(`SELECT "name" FROM "tenants" WHERE "id" = $1`, tenantId).Scan(&businessName)

	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Write([]byte(renderStoreCatalogPage(businessName, products)))
}

func validateStoreToken(tokenEncoded string) (string, error) {
	decodedBytes, err := base64.RawURLEncoding.DecodeString(tokenEncoded)
	if err != nil {
		decodedBytes, err = base64.URLEncoding.DecodeString(tokenEncoded)
		if err != nil {
			return "", err
		}
	}

	tokenRaw := string(decodedBytes)
	parts := strings.Split(tokenRaw, ":")
	secretKey := "PosBahStoreSecretKey123!"

	if len(parts) == 3 {
		tenantId := parts[0]
		expiryStr := parts[1]
		signature := parts[2]

		dataToSign := tenantId + ":" + expiryStr
		expectedSig := computeStoreHmacSha256(dataToSign, secretKey)
		if expectedSig == signature {
			return tenantId, nil
		}
	} else if len(parts) == 2 {
		tenantId := parts[0]
		signature := parts[1]

		expectedSig := computeStoreHmacSha256(tenantId, secretKey)
		if expectedSig == signature {
			return tenantId, nil
		}
	}

	return "", fmt.Errorf("invalid token structure")
}

func computeStoreHmacSha256(data string, secret string) string {
	h := hmac.New(sha256.New, []byte(secret))
	h.Write([]byte(data))
	return base64.RawURLEncoding.EncodeToString(h.Sum(nil))
}

func loadTenantProducts(tenantId string) ([]StoreProduct, error) {
	rows, err := db.Query(`SELECT "id", "name", COALESCE("category", ''), COALESCE("price", 0), COALESCE("stock", 0), COALESCE("image", '') FROM "products" WHERE "tenantId" = $1 ORDER BY "name" ASC`, tenantId)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var products []StoreProduct
	for rows.Next() {
		var p StoreProduct
		if err := rows.Scan(&p.ID, &p.Name, &p.Category, &p.Price, &p.Stock, &p.Image); err != nil {
			return nil, err
		}
		products = append(products, p)
	}
	return products, nil
}

func renderStoreCatalogPage(businessName string, products []StoreProduct) string {
	productsJSON, _ := json.Marshal(products)

	return fmt.Sprintf(`<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Katalog Toko Online - %s</title>
    <!-- Google Fonts Outfit & Inter -->
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;700;800&family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
    <!-- FontAwesome for icons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        :root {
            --primary: #3B82F6;
            --primary-hover: #2563EB;
            --bg-dark: #0F172A;
            --card-bg: rgba(30, 41, 59, 0.7);
            --border: rgba(255, 255, 255, 0.08);
            --text-main: #F8FAFC;
            --text-muted: #94A3B8;
            --success: #10B981;
            --error: #EF4444;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            background-color: var(--bg-dark);
            color: var(--text-main);
            font-family: 'Inter', sans-serif;
            min-height: 100vh;
            background-image: radial-gradient(circle at top right, rgba(59, 130, 246, 0.08), transparent),
                              radial-gradient(circle at bottom left, rgba(16, 185, 129, 0.03), transparent);
            padding-bottom: 60px;
        }

        header {
            background: rgba(15, 23, 42, 0.6);
            backdrop-filter: blur(12px);
            border-bottom: 1px solid var(--border);
            padding: 20px 16px;
            position: sticky;
            top: 0;
            z-index: 100;
        }

        .header-container {
            max-width: 1000px;
            margin: 0 auto;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .logo-section h1 {
            font-family: 'Outfit', sans-serif;
            font-size: 24px;
            font-weight: 800;
            background: linear-gradient(135deg, #60A5FA, #3B82F6);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .logo-section p {
            font-size: 12px;
            color: var(--text-muted);
            margin-top: 2px;
        }

        .main-container {
            max-width: 1000px;
            margin: 30px auto;
            padding: 0 16px;
        }

        .search-filter-section {
            display: flex;
            flex-direction: column;
            gap: 16px;
            margin-bottom: 30px;
        }

        .search-bar-wrapper {
            position: relative;
            width: 100%%;
        }

        .search-bar-wrapper i {
            position: absolute;
            left: 16px;
            top: 50%%;
            transform: translateY(-50%%);
            color: var(--text-muted);
            font-size: 16px;
        }

        .search-input {
            width: 100%%;
            background: rgba(30, 41, 59, 0.5);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 14px 16px 14px 48px;
            color: var(--text-main);
            font-size: 15px;
            outline: none;
            transition: all 0.3s;
            font-family: inherit;
        }

        .search-input:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
            background: rgba(30, 41, 59, 0.8);
        }

        .categories-wrapper {
            display: flex;
            gap: 8px;
            overflow-x: auto;
            padding-bottom: 4px;
            scrollbar-width: none;
        }
        .categories-wrapper::-webkit-scrollbar {
            display: none;
        }

        .category-chip {
            background: rgba(30, 41, 59, 0.5);
            border: 1px solid var(--border);
            border-radius: 20px;
            padding: 8px 16px;
            font-size: 13px;
            font-weight: 500;
            color: var(--text-muted);
            cursor: pointer;
            white-space: nowrap;
            transition: all 0.2s;
        }

        .category-chip.active, .category-chip:hover {
            background: var(--primary);
            color: white;
            border-color: var(--primary);
        }

        .products-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
            gap: 20px;
        }

        .product-card {
            background: var(--card-bg);
            backdrop-filter: blur(12px);
            border: 1px solid var(--border);
            border-radius: 16px;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            transition: transform 0.3s, box-shadow 0.3s;
        }

        .product-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 10px 20px rgba(0, 0, 0, 0.3);
            border-color: rgba(59, 130, 246, 0.2);
        }

        .image-container {
            width: 100%%;
            height: 180px;
            background: rgba(15, 23, 42, 0.4);
            display: flex;
            align-items: center;
            justify-content: center;
            position: relative;
            overflow: hidden;
            border-bottom: 1px solid var(--border);
        }

        .product-image {
            width: 100%%;
            height: 100%%;
            object-fit: cover;
        }

        .image-placeholder {
            font-size: 40px;
            color: rgba(96, 165, 250, 0.2);
            font-weight: bold;
            font-family: 'Outfit', sans-serif;
            text-transform: uppercase;
        }

        .product-info {
            padding: 16px;
            display: flex;
            flex-direction: column;
            flex-grow: 1;
        }

        .product-category {
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: var(--text-muted);
            margin-bottom: 6px;
            font-weight: 600;
        }

        .product-name {
            font-size: 15px;
            font-weight: 700;
            color: var(--text-main);
            margin-bottom: 8px;
            line-height: 1.4;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            height: 42px;
        }

        .product-footer {
            margin-top: auto;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .product-price {
            font-family: 'Outfit', sans-serif;
            font-size: 18px;
            font-weight: 800;
            color: var(--primary);
        }

        .stock-badge {
            font-size: 11px;
            font-weight: 600;
            padding: 4px 8px;
            border-radius: 6px;
            width: fit-content;
        }

        .stock-badge.in-stock {
            background: rgba(16, 185, 129, 0.1);
            color: var(--success);
        }

        .stock-badge.out-of-stock {
            background: rgba(239, 68, 68, 0.1);
            color: var(--error);
        }

        .empty-state {
            grid-column: 1 / -1;
            text-align: center;
            padding: 60px 20px;
            color: var(--text-muted);
        }

        .empty-state i {
            font-size: 48px;
            color: rgba(255, 255, 255, 0.1);
            margin-bottom: 16px;
        }

        .empty-state h3 {
            font-size: 18px;
            color: var(--text-main);
            margin-bottom: 8px;
        }

        footer {
            text-align: center;
            padding: 40px 16px 20px;
            color: var(--text-muted);
            font-size: 12px;
            border-top: 1px solid var(--border);
            margin-top: 60px;
        }

        footer strong {
            color: var(--text-main);
        }
    </style>
</head>
<body>

    <header>
        <div class="header-container">
            <div class="logo-section">
                <h1>%s</h1>
                <p><i class="fa-solid fa-store"></i> Katalog Produk Online Resmi</p>
            </div>
        </div>
    </header>

    <div class="main-container">
        <div class="search-filter-section">
            <div class="search-bar-wrapper">
                <i class="fa-solid fa-magnifying-glass"></i>
                <input type="text" id="search-input" class="search-input" placeholder="Cari nama produk...">
            </div>
            <div class="categories-wrapper" id="categories-container">
                <div class="category-chip active" data-category="ALL">Semua Produk</div>
            </div>
        </div>

        <div class="products-grid" id="products-grid">
            <!-- Dynamic products will be rendered here -->
        </div>
    </div>

    <footer>
        <p>&copy; 2026 <strong>%s</strong>. Seluruh Hak Cipta Dilindungi.</p>
        <p style="margin-top: 6px; opacity: 0.7;">Powered by POSBah Cashier System</p>
    </footer>

    <script>
        const products = %s;
        
        // Extract unique categories
        const categories = new Set();
        products.forEach(p => {
            if (p.category && p.category.trim() !== "") {
                categories.add(p.category.trim());
            }
        });

        // Render category chips
        const categoriesContainer = document.getElementById("categories-container");
        categories.forEach(cat => {
            const chip = document.createElement("div");
            chip.className = "category-chip";
            chip.textContent = cat;
            chip.setAttribute("data-category", cat);
            categoriesContainer.appendChild(chip);
        });

        let activeCategory = "ALL";
        let searchQuery = "";

        function formatRupiah(value) {
            return "Rp " + new Intl.NumberFormat("id-ID").format(value);
        }

        function renderProducts() {
            const grid = document.getElementById("products-grid");
            grid.innerHTML = "";

            const filtered = products.filter(p => {
                const matchesSearch = p.name.toLowerCase().includes(searchQuery.toLowerCase());
                const matchesCategory = activeCategory === "ALL" || p.category === activeCategory;
                return matchesSearch && matchesCategory;
            });

            if (filtered.length === 0) {
                grid.innerHTML = '<div class="empty-state"><i class="fa-solid fa-box-open"></i><h3>Produk Tidak Ditemukan</h3><p>Silakan coba kata kunci pencarian atau kategori lainnya.</p></div>';
                return;
            }

            filtered.forEach(p => {
                const card = document.createElement("div");
                card.className = "product-card";

                let imageHtml = "";
                if (p.image && p.image.trim() !== "") {
                    let imgSrc = p.image;
                    if (!imgSrc.startsWith("data:")) {
                        imgSrc = "data:image/png;base64," + imgSrc;
                    }
                    imageHtml = '<img src="' + imgSrc + '" class="product-image" alt="' + p.name + '" onerror="this.style.display=\'none\'; this.nextElementSibling.style.display=\'flex\';"><div class="image-placeholder" style="display:none;">' + p.name.substring(0, 1) + '</div>';
                } else {
                    imageHtml = '<div class="image-placeholder">' + p.name.substring(0, 1) + '</div>';
                }

                const stockClass = p.stock > 0 ? "in-stock" : "out-of-stock";
                const stockText = p.stock > 0 ? "Stok Tersedia" : "Stok Habis";

                card.innerHTML = '<div class="image-container">' + imageHtml + '</div><div class="product-info"><div class="product-category">' + (p.category || 'Lainnya') + '</div><div class="product-name">' + p.name + '</div><div class="product-footer"><div class="product-price">' + formatRupiah(p.price) + '</div><span class="stock-badge ' + stockClass + '">' + stockText + '</span></div></div>';
                grid.appendChild(card);
            });
        }

        // Setup search event
        document.getElementById("search-input").addEventListener("input", (e) => {
            searchQuery = e.target.value;
            renderProducts();
        });

        // Setup category chips events
        document.addEventListener("click", (e) => {
            if (e.target.classList.contains("category-chip")) {
                document.querySelectorAll(".category-chip").forEach(c => c.classList.remove("active"));
                e.target.classList.add("active");
                activeCategory = e.target.getAttribute("data-category");
                renderProducts();
            }
        });

        // Initial render
        renderProducts();
    </script>
</body>
</html>`, businessName, businessName, businessName, productsJSON)
}

func renderStoreErrorPage(title string, message string) string {
	return fmt.Sprintf(`<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>%s - POSBah</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@600;800&family=Inter:wght@400;500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        body {
            background-color: #0F172A;
            color: #F8FAFC;
            font-family: 'Inter', sans-serif;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 16px;
            margin: 0;
            background-image: radial-gradient(circle at top right, rgba(239, 68, 68, 0.08), transparent);
        }
        .container {
            width: 100%%;
            max-width: 400px;
            background: rgba(30, 41, 59, 0.7);
            backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 20px;
            padding: 32px;
            text-align: center;
        }
        .icon {
            font-size: 48px;
            color: #EF4444;
            margin-bottom: 20px;
        }
        h1 {
            font-family: 'Outfit', sans-serif;
            font-size: 20px;
            font-weight: 800;
            margin-bottom: 12px;
        }
        p {
            font-size: 14px;
            color: #94A3B8;
            line-height: 1.6;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="icon"><i class="fa-solid fa-triangle-exclamation"></i></div>
        <h1>%s</h1>
        <p>%s</p>
    </div>
</body>
</html>`, title, title, message)
}

// handleCData handles ADMS device initialization and attendance uploads
func handleCData(w http.ResponseWriter, r *http.Request) {
	sn := r.URL.Query().Get("SN")
	table := r.URL.Query().Get("table")
	options := r.URL.Query().Get("options")

	log.Printf("[ADMS] Received %s request on /iclock/cdata?SN=%s&table=%s&options=%s", r.Method, sn, table, options)

	if sn == "" {
		w.Header().Set("Content-Type", "text/plain")
		w.Write([]byte("ERROR: No SN"))
		return
	}

	if r.Method == http.MethodGet || options == "all" {
		var exists bool
		err := db.QueryRow(`SELECT EXISTS(SELECT 1 FROM "bmp_adms_devices" WHERE "serialNumber" = $1)`, sn).Scan(&exists)
		if err == nil {
			if !exists {
				_, err = db.Exec(`INSERT INTO "bmp_adms_devices" ("serialNumber", "lastActivity") VALUES ($1, NOW())`, sn)
			} else {
				_, err = db.Exec(`UPDATE "bmp_adms_devices" SET "lastActivity" = NOW() WHERE "serialNumber" = $1`, sn)
			}
			if err != nil {
				log.Printf("[ADMS] Error saving/updating device status: %v", err)
			}
		} else {
			log.Printf("[ADMS] Error checking device existence: %v", err)
		}

		response := fmt.Sprintf("GET OPTION FROM:%s\nErrorDelay=60\nDelay=30\nTransTimes=00:00;14:05\nTransInterval=1\nTransFlag=TransData AttLog OpLog\nRealtime=1\nEncrypt=0", sn)
		w.Header().Set("Content-Type", "text/plain")
		w.Write([]byte(response))
		return
	}

	if r.Method == http.MethodPost && table == "ATTLOG" {
		bodyBytes, err := io.ReadAll(r.Body)
		if err != nil {
			http.Error(w, "Read body error", http.StatusBadRequest)
			return
		}

		err = handleCDataPost(sn, string(bodyBytes))
		if err != nil {
			log.Printf("[ADMS] CData POST error: %v", err)
		}

		w.Header().Set("Content-Type", "text/plain")
		w.Write([]byte("OK"))
		return
	}

	if r.Method == http.MethodPost {
		w.Header().Set("Content-Type", "text/plain")
		w.Write([]byte("OK"))
		return
	}

	w.WriteHeader(http.StatusNotFound)
}

// handleGetRequest handles command polling from ZKTeco devices
func handleGetRequest(w http.ResponseWriter, r *http.Request) {
	sn := r.URL.Query().Get("SN")
	log.Printf("[ADMS] Received %s request on /iclock/getrequest?SN=%s", r.Method, sn)

	// In a real ADMS configuration, we can send options updates to ZKTeco machine
	// We just reply OK for default keepalive
	w.Header().Set("Content-Type", "text/plain")
	w.Write([]byte("OK"))
}

var allowedSyncTables = map[string]bool{
	"local_users":          true,
	"tenants":              true,
	"outlets":              true,
	"employees":            true,
	"products":             true,
	"customers":            true,
	"transactions":         true,
	"transaction_items":    true,
	"activity_logs":        true,
	"bmp_clients":          true,
	"bmp_invoices":         true,
	"bmp_products":         true,
	"bmp_master_products":  true,
	"bmp_invoice_payments": true,
	"bmp_cashflow":         true,
	"bmp_settings":         true,
	"bmp_employees":        true,
	"bmp_payrolls":         true,
	"bmp_bahan_baku":       true,
	"bmp_bahan_baku_item":  true,
	"print_settings":       true,
	"bmp_attendance_logs":  true,
	"bmp_device_tenants":   true,
}

var columnNameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]+$`)

func isValidColumnName(name string) bool {
	return columnNameRegex.MatchString(name)
}

// validateTenantAccess checks whether the user has authorization for the requested tenant.
// For public GET requests to login tables (local_users, employees, tenants), it allows the request
// if the filter restricts query results to safe columns (googleSub, email, or id).
func validateTenantAccess(r *http.Request) error {
	parts := strings.Split(r.URL.Path, "/")
	if len(parts) < 4 {
		return fmt.Errorf("invalid path")
	}
	tableName := parts[3]

	if !allowedSyncTables[tableName] {
		return fmt.Errorf("forbidden: table %s is not allowed for synchronization", tableName)
	}

	tenantID := r.Header.Get("x-tenant-id")
	userEmail := r.Header.Get("x-user-email")

	// 1. Allow login/auth-related GET requests without headers IF they query by secure columns
	if r.Method == http.MethodGet && (tenantID == "" || userEmail == "") {
		if tableName == "local_users" || tableName == "employees" || tableName == "tenants" {
			hasSecureFilter := false
			for k, vList := range r.URL.Query() {
				if len(vList) > 0 && strings.HasPrefix(vList[0], "eq.") {
					if k == "googleSub" || k == "email" || k == "id" {
						hasSecureFilter = true
						break
					}
				}
			}
			if hasSecureFilter {
				return nil
			}
		}
		return fmt.Errorf("unauthorized: missing x-tenant-id or x-user-email headers")
	}

	if tenantID == "" || userEmail == "" {
		return fmt.Errorf("unauthorized: missing x-tenant-id or x-user-email headers")
	}

	// 2. Allow registration/rejoin POST requests to local_users and tenants 
	// for new users or approved rejoining users.
	if r.Method == http.MethodPost && (tableName == "local_users" || tableName == "tenants") {
		var status string
		err := db.QueryRow(`SELECT "status" FROM "deleted_users" WHERE TRIM(LOWER("email")) = $1`, strings.TrimSpace(strings.ToLower(userEmail))).Scan(&status)
		if err == nil {
			// Email exists in deleted_users. Only allow if status is ACTIVE (approved to rejoin)
			if status != "ACTIVE" {
				return fmt.Errorf("forbidden: user %s is deleted or pending rejoin approval (status: %s)", userEmail, status)
			}
		} else if err != sql.ErrNoRows {
			return fmt.Errorf("database error: %v", err)
		}
		// Approved to rejoin or completely new user: allowed to initialize user/tenant record
		return nil
	}

	// 3. Verify if user is active and belongs to tenantId (either as OWNER or Employee)
	var isAllowed bool

	// Check local_users (owner)
	var count int
	err := db.QueryRow(`SELECT COUNT(*) FROM "local_users" WHERE "email" = $1 AND "tenantId" = $2 AND "isActive" = TRUE`, userEmail, tenantID).Scan(&count)
	if err == nil && count > 0 {
		isAllowed = true
	}

	// Check employees
	if !isAllowed {
		err = db.QueryRow(`SELECT COUNT(*) FROM "employees" WHERE "email" = $1 AND "tenantId" = $2 AND "isActive" = TRUE`, userEmail, tenantID).Scan(&count)
		if err == nil && count > 0 {
			isAllowed = true
		}
	}

	if !isAllowed {
		return fmt.Errorf("forbidden: user %s has no active membership in tenant %s", userEmail, tenantID)
	}

	// 3. Enforce query-level tenant parameter isolation for GET/PATCH/DELETE
	if r.Method == http.MethodGet || r.Method == http.MethodPatch || r.Method == http.MethodDelete {
		for k, vList := range r.URL.Query() {
			if len(vList) > 0 && strings.HasPrefix(vList[0], "eq.") {
				val := strings.TrimPrefix(vList[0], "eq.")
				if k == "tenantId" && val != tenantID {
					return fmt.Errorf("forbidden: query tenantId %s does not match authorized tenantId %s", val, tenantID)
				}
				if k == "id" && tableName == "tenants" && val != tenantID {
					return fmt.Errorf("forbidden: query tenant id %s does not match authorized tenant %s", val, tenantID)
				}
			}
		}
	}

	return nil
}

func getLatestVersionFromDb() string {
	if db == nil {
		return "2.5.0"
	}
	var version string
	err := db.QueryRow(`SELECT "version" FROM "apk_config" WHERE "id" = 1`).Scan(&version)
	if err != nil || version == "" {
		return "2.5.0"
	}
	return version
}

// handleSyncRoute acts as a gateway for both GET (query) and POST (upsert) requests on /api/sync/
func handleSyncRoute(w http.ResponseWriter, r *http.Request) {
	clientVersion := strings.TrimSpace(r.Header.Get("x-client-version"))
	latestVersion := getLatestVersionFromDb()
	if clientVersion == "" || compareVersions(clientVersion, latestVersion) < 0 {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusUpgradeRequired)
		json.NewEncoder(w).Encode(map[string]string{
			"error":         "Upgrade Required",
			"message":       "Pembaruan wajib untuk kelancaran sinkronisasi data transaksi dan peningkatan keamanan sistem POSBah Anda. Silakan unduh versi terbaru untuk melanjutkan.",
			"latestVersion": latestVersion,
		})
		return
	}

	if err := validateTenantAccess(r); err != nil {
		if strings.HasPrefix(err.Error(), "unauthorized") {
			http.Error(w, err.Error(), http.StatusUnauthorized)
		} else {
			http.Error(w, err.Error(), http.StatusForbidden)
		}
		return
	}

	if r.Method == http.MethodGet {
		handleSyncQuery(w, r)
		return
	}
	if r.Method == http.MethodPost {
		handleSyncTable(w, r)
		return
	}
	if r.Method == http.MethodPatch {
		handleSyncPatch(w, r)
		return
	}
	if r.Method == http.MethodDelete {
		handleSyncDelete(w, r)
		return
	}
	http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
}

func handleSyncPatch(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	if len(parts) < 4 {
		http.Error(w, "Missing table name", http.StatusBadRequest)
		return
	}
	tableName := parts[3]

	var updateData map[string]interface{}
	if err := json.NewDecoder(r.Body).Decode(&updateData); err != nil {
		http.Error(w, "Invalid JSON body: "+err.Error(), http.StatusBadRequest)
		return
	}

	var whereClauses []string
	var args []interface{}
	idx := 1

	for k, vList := range r.URL.Query() {
		if len(vList) == 0 {
			continue
		}
		val := vList[0]
		if strings.HasPrefix(val, "eq.") {
			realVal := strings.TrimPrefix(val, "eq.")
			if !isValidColumnName(k) {
				http.Error(w, "Bad Request: invalid query parameter key", http.StatusBadRequest)
				return
			}
			whereClauses = append(whereClauses, fmt.Sprintf(`"%s" = $%d`, k, idx))
			args = append(args, parseQueryParamValue(tableName, k, realVal))
			idx++
		}
	}

	tenantID := r.Header.Get("x-tenant-id")
	if tenantID != "" {
		if tableName == "transaction_items" {
			var txIDVal string
			for k, vList := range r.URL.Query() {
				if k == "transactionId" && len(vList) > 0 && strings.HasPrefix(vList[0], "eq.") {
					txIDVal = strings.TrimPrefix(vList[0], "eq.")
					break
				}
			}
			if txIDVal != "" {
				var count int
				err := db.QueryRow(`SELECT COUNT(*) FROM "transactions" WHERE "id" = $1 AND "tenantId" = $2`, txIDVal, tenantID).Scan(&count)
				if err != nil || count == 0 {
					http.Error(w, "Forbidden: transaction does not belong to your tenant", http.StatusForbidden)
					return
				}
			} else {
				var itemIDVal string
				for k, vList := range r.URL.Query() {
					if k == "id" && len(vList) > 0 && strings.HasPrefix(vList[0], "eq.") {
						itemIDVal = strings.TrimPrefix(vList[0], "eq.")
						break
					}
				}
				if itemIDVal != "" {
					var count int
					err := db.QueryRow(`SELECT COUNT(*) FROM "transaction_items" ti 
						INNER JOIN "transactions" t ON ti."transactionId" = t."id" 
						WHERE ti."id" = $1 AND t."tenantId" = $2`, itemIDVal, tenantID).Scan(&count)
					if err != nil || count == 0 {
						http.Error(w, "Forbidden: transaction item does not belong to your tenant", http.StatusForbidden)
						return
					}
				} else {
					http.Error(w, "Bad Request: transactionId or id filter required for transaction_items", http.StatusBadRequest)
					return
				}
			}
		} else if tableName == "tenants" {
			hasIdFilter := false
			for k := range r.URL.Query() {
				if k == "id" {
					hasIdFilter = true
					break
				}
			}
			if !hasIdFilter {
				whereClauses = append(whereClauses, fmt.Sprintf(`"id" = $%d`, idx))
				args = append(args, tenantID)
				idx++
			}
		} else {
			hasTenantIdFilter := false
			for k := range r.URL.Query() {
				if k == "tenantId" {
					hasTenantIdFilter = true
					break
				}
			}
			if !hasTenantIdFilter {
				whereClauses = append(whereClauses, fmt.Sprintf(`"tenantId" = $%d`, idx))
				args = append(args, tenantID)
				idx++
			}
		}
	}

	if len(whereClauses) == 0 {
		http.Error(w, "WHERE clause is required for PATCH", http.StatusBadRequest)
		return
	}

	var setClauses []string
	for k, v := range updateData {
		if !isValidColumnName(k) {
			http.Error(w, "Bad Request: invalid column name in update payload", http.StatusBadRequest)
			return
		}
		setClauses = append(setClauses, fmt.Sprintf(`"%s" = $%d`, k, idx))
		switch val := v.(type) {
		case map[string]interface{}, []interface{}:
			jsonBytes, _ := json.Marshal(val)
			args = append(args, string(jsonBytes))
		default:
			args = append(args, v)
		}
		idx++
	}

	query := fmt.Sprintf(`UPDATE "%s" SET %s WHERE %s`, tableName, strings.Join(setClauses, ", "), strings.Join(whereClauses, " AND "))

	_, err := db.Exec(query, args...)
	if err != nil {
		http.Error(w, "Update failed: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write([]byte(`{"success":true}`))
}

func handleSyncDelete(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	if len(parts) < 4 {
		http.Error(w, "Missing table name", http.StatusBadRequest)
		return
	}
	tableName := parts[3]

	var whereClauses []string
	var args []interface{}
	idx := 1

	for k, vList := range r.URL.Query() {
		if len(vList) == 0 {
			continue
		}
		val := vList[0]
		if strings.HasPrefix(val, "eq.") {
			realVal := strings.TrimPrefix(val, "eq.")
			if !isValidColumnName(k) {
				http.Error(w, "Bad Request: invalid query parameter key", http.StatusBadRequest)
				return
			}
			whereClauses = append(whereClauses, fmt.Sprintf(`"%s" = $%d`, k, idx))
			args = append(args, parseQueryParamValue(tableName, k, realVal))
			idx++
		}
	}

	tenantID := r.Header.Get("x-tenant-id")
	if tenantID != "" {
		if tableName == "transaction_items" {
			var txIDVal string
			for k, vList := range r.URL.Query() {
				if k == "transactionId" && len(vList) > 0 && strings.HasPrefix(vList[0], "eq.") {
					txIDVal = strings.TrimPrefix(vList[0], "eq.")
					break
				}
			}
			if txIDVal != "" {
				var count int
				err := db.QueryRow(`SELECT COUNT(*) FROM "transactions" WHERE "id" = $1 AND "tenantId" = $2`, txIDVal, tenantID).Scan(&count)
				if err != nil || count == 0 {
					http.Error(w, "Forbidden: transaction does not belong to your tenant", http.StatusForbidden)
					return
				}
			} else {
				var itemIDVal string
				for k, vList := range r.URL.Query() {
					if k == "id" && len(vList) > 0 && strings.HasPrefix(vList[0], "eq.") {
						itemIDVal = strings.TrimPrefix(vList[0], "eq.")
						break
					}
				}
				if itemIDVal != "" {
					var count int
					err := db.QueryRow(`SELECT COUNT(*) FROM "transaction_items" ti 
						INNER JOIN "transactions" t ON ti."transactionId" = t."id" 
						WHERE ti."id" = $1 AND t."tenantId" = $2`, itemIDVal, tenantID).Scan(&count)
					if err != nil || count == 0 {
						http.Error(w, "Forbidden: transaction item does not belong to your tenant", http.StatusForbidden)
						return
					}
				} else {
					http.Error(w, "Bad Request: transactionId or id filter required for transaction_items", http.StatusBadRequest)
					return
				}
			}
		} else if tableName == "tenants" {
			hasIdFilter := false
			for k := range r.URL.Query() {
				if k == "id" {
					hasIdFilter = true
					break
				}
			}
			if !hasIdFilter {
				whereClauses = append(whereClauses, fmt.Sprintf(`"id" = $%d`, idx))
				args = append(args, tenantID)
				idx++
			}
		} else {
			hasTenantIdFilter := false
			for k := range r.URL.Query() {
				if k == "tenantId" {
					hasTenantIdFilter = true
					break
				}
			}
			if !hasTenantIdFilter {
				whereClauses = append(whereClauses, fmt.Sprintf(`"tenantId" = $%d`, idx))
				args = append(args, tenantID)
				idx++
			}
		}
	}

	if len(whereClauses) == 0 {
		http.Error(w, "WHERE clause is required for DELETE", http.StatusBadRequest)
		return
	}

	query := fmt.Sprintf(`DELETE FROM "%s" WHERE %s`, tableName, strings.Join(whereClauses, " AND "))

	_, err := db.Exec(query, args...)
	if err != nil {
		http.Error(w, "Delete failed: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write([]byte(`{"success":true}`))
}

func handleSyncQuery(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	if len(parts) < 4 {
		http.Error(w, "Missing table name", http.StatusBadRequest)
		return
	}
	tableName := parts[3]

	var whereClauses []string
	var args []interface{}
	idx := 1

	for k, vList := range r.URL.Query() {
		if len(vList) == 0 {
			continue
		}
		val := vList[0]
		if strings.HasPrefix(val, "eq.") {
			realVal := strings.TrimPrefix(val, "eq.")
			if !isValidColumnName(k) {
				http.Error(w, "Bad Request: invalid query parameter key", http.StatusBadRequest)
				return
			}
			whereClauses = append(whereClauses, fmt.Sprintf(`"%s" = $%d`, k, idx))
			args = append(args, parseQueryParamValue(tableName, k, realVal))
			idx++
		}
	}

	tenantID := r.Header.Get("x-tenant-id")
	userEmail := r.Header.Get("x-user-email")
	if tenantID != "" {
		if tableName == "transaction_items" {
			var txIDVal string
			for k, vList := range r.URL.Query() {
				if k == "transactionId" && len(vList) > 0 && strings.HasPrefix(vList[0], "eq.") {
					txIDVal = strings.TrimPrefix(vList[0], "eq.")
					break
				}
			}
			if txIDVal != "" {
				var count int
				err := db.QueryRow(`SELECT COUNT(*) FROM "transactions" WHERE "id" = $1 AND "tenantId" = $2`, txIDVal, tenantID).Scan(&count)
				if err != nil || count == 0 {
					http.Error(w, "Forbidden: transaction does not belong to your tenant", http.StatusForbidden)
					return
				}
			} else {
				http.Error(w, "Bad Request: transactionId filter required for transaction_items", http.StatusBadRequest)
				return
			}
		} else if tableName == "tenants" {
			hasIdFilter := false
			hasOwnerEmailFilter := false
			for k, vList := range r.URL.Query() {
				if k == "id" {
					hasIdFilter = true
				}
				if k == "ownerEmail" && len(vList) > 0 {
					hasOwnerEmailFilter = true
					queriedEmail := strings.TrimPrefix(vList[0], "eq.")
					if strings.TrimSpace(strings.ToLower(queriedEmail)) != strings.TrimSpace(strings.ToLower(userEmail)) {
						http.Error(w, "Forbidden: you can only query tenants that you own", http.StatusForbidden)
						return
					}
				}
			}
			if !hasIdFilter && !hasOwnerEmailFilter {
				whereClauses = append(whereClauses, fmt.Sprintf(`"id" = $%d`, idx))
				args = append(args, tenantID)
				idx++
			}
		} else if tableName == "bmp_attendance_logs" {
			whereClauses = append(whereClauses, fmt.Sprintf(`"deviceSN" IN (SELECT "serialNumber" FROM "bmp_device_tenants" WHERE "tenantId" = $%d)`, idx))
			args = append(args, tenantID)
			idx++
		} else {
			hasTenantIdFilter := false
			for k := range r.URL.Query() {
				if k == "tenantId" {
					hasTenantIdFilter = true
					break
				}
			}
			if !hasTenantIdFilter {
				whereClauses = append(whereClauses, fmt.Sprintf(`"tenantId" = $%d`, idx))
				args = append(args, tenantID)
				idx++
			}
		}
	}

	query := fmt.Sprintf(`SELECT * FROM "%s"`, tableName)
	if len(whereClauses) > 0 {
		query += " WHERE " + strings.Join(whereClauses, " AND ")
	}

	rows, err := db.Query(query, args...)
	if err != nil {
		http.Error(w, "Database error: "+err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	cols, err := rows.Columns()
	if err != nil {
		http.Error(w, "Columns error: "+err.Error(), http.StatusInternalServerError)
		return
	}

	var result []map[string]interface{}
	for rows.Next() {
		columns := make([]interface{}, len(cols))
		columnPointers := make([]interface{}, len(cols))
		for i := range columns {
			columnPointers[i] = &columns[i]
		}

		if err := rows.Scan(columnPointers...); err != nil {
			http.Error(w, "Scan error: "+err.Error(), http.StatusInternalServerError)
			return
		}

		rowMap := make(map[string]interface{})
		for i, colName := range cols {
			val := columns[i]
			b, ok := val.([]byte)
			if ok {
				var temp map[string]interface{}
				var tempArr []interface{}
				if json.Unmarshal(b, &temp) == nil {
					rowMap[colName] = temp
				} else if json.Unmarshal(b, &tempArr) == nil {
					rowMap[colName] = tempArr
				} else {
					rowMap[colName] = string(b)
				}
			} else {
				rowMap[colName] = val
			}
		}
		result = append(result, rowMap)
	}

	w.Header().Set("Content-Type", "application/json")
	if len(result) == 0 {
		w.Write([]byte("[]"))
		return
	}
	json.NewEncoder(w).Encode(result)
}

func isPremiumEmail(email string) bool {
	email = strings.TrimSpace(strings.ToLower(email))
	staticPremium := map[string]bool{
		"muhammadmuizz8@gmail.com": true,
		"bahteramulyap@gmail.com":    true,
		"hanafiariful@gmail.com":    true,
		"fahrup22@gmail.com":       true,
		"alfarisirosi40@gmail.com": true,
		"mulyakus84@gmail.com":     true,
	}
	if staticPremium[email] {
		return true
	}

	if db == nil {
		return false
	}
	var isPremium bool
	err := db.QueryRow(`SELECT "isPremium" FROM "local_users" WHERE TRIM(LOWER("email")) = $1`, email).Scan(&isPremium)
	if err == nil && isPremium {
		return true
	}
	return false
}

func handleSyncTable(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	if len(parts) < 4 {
		http.Error(w, "Missing table name", http.StatusBadRequest)
		return
	}
	tableName := parts[3]

	var rows []map[string]interface{}
	if err := json.NewDecoder(r.Body).Decode(&rows); err != nil {
		http.Error(w, "Invalid JSON body: "+err.Error(), http.StatusBadRequest)
		return
	}

	if tableName == "local_users" {
		userEmail := r.Header.Get("x-user-email")
		for _, row := range rows {
			emailVal, _ := row["email"].(string)
			if strings.TrimSpace(strings.ToLower(emailVal)) != strings.TrimSpace(strings.ToLower(userEmail)) {
				http.Error(w, "Forbidden: cannot sync local_user record for a different email address", http.StatusForbidden)
				return
			}
			if emailVal != "" && isPremiumEmail(emailVal) {
				row["isPremium"] = true
			}

			// Clean transition for googleSub when rejoining:
			// If existing user has googleSub = email, but incoming has a different (numeric) googleSub,
			// update the existing user's googleSub in the database first to avoid unique constraint on email.
			googleSubVal, _ := row["googleSub"].(string)
			if emailVal != "" && googleSubVal != "" {
				var existingSub string
				err := db.QueryRow(`SELECT "googleSub" FROM "local_users" WHERE TRIM(LOWER("email")) = $1`, strings.TrimSpace(strings.ToLower(emailVal))).Scan(&existingSub)
				if err == nil && existingSub != googleSubVal {
					log.Printf("[SyncLocalUsers] Updating googleSub for %s from %s to %s due to rejoin transition", emailVal, existingSub, googleSubVal)
					_, updateErr := db.Exec(`UPDATE "local_users" SET "googleSub" = $1 WHERE "googleSub" = $2`, googleSubVal, existingSub)
					if updateErr != nil {
						log.Printf("[SyncLocalUsers] Failed to update googleSub during transition: %v", updateErr)
					}
				}
			}
		}
	}

	tenantID := r.Header.Get("x-tenant-id")
	if tenantID != "" {
		if tableName == "transaction_items" {
			for _, row := range rows {
				txID, ok := row["transactionId"]
				if ok {
					var txIDVal int64
					switch v := txID.(type) {
					case float64:
						txIDVal = int64(v)
					case int64:
						txIDVal = v
					case int:
						txIDVal = int64(v)
					default:
						parsed, _ := strconv.ParseInt(fmt.Sprintf("%v", v), 10, 64)
						txIDVal = parsed
					}
					var count int
					err := db.QueryRow(`SELECT COUNT(*) FROM "transactions" WHERE "id" = $1 AND "tenantId" = $2`, txIDVal, tenantID).Scan(&count)
					if err != nil || count == 0 {
						http.Error(w, fmt.Sprintf("Forbidden: transaction %d does not belong to tenant %s", txIDVal, tenantID), http.StatusForbidden)
						return
					}
				}
			}
		} else if tableName == "tenants" {
			userEmail := r.Header.Get("x-user-email")
			for _, row := range rows {
				ownerEmailVal, _ := row["ownerEmail"].(string)
				if ownerEmailVal != "" && strings.TrimSpace(strings.ToLower(ownerEmailVal)) != strings.TrimSpace(strings.ToLower(userEmail)) {
					http.Error(w, "Forbidden: cannot sync tenant owned by a different email address", http.StatusForbidden)
					return
				}
				idVal, hasId := row["id"]
				if hasId {
					if fmt.Sprintf("%v", idVal) != tenantID {
						http.Error(w, fmt.Sprintf("Forbidden: tenant id %v does not match authorized tenant %s", idVal, tenantID), http.StatusForbidden)
						return
					}
				} else {
					row["id"] = tenantID
				}
			}
		} else {
			for _, row := range rows {
				rowTenantID, hasTenant := row["tenantId"]
				if hasTenant {
					if fmt.Sprintf("%v", rowTenantID) != tenantID {
						http.Error(w, fmt.Sprintf("Forbidden: row tenantId %v does not match authorized tenant %s", rowTenantID, tenantID), http.StatusForbidden)
						return
					}
				} else {
					row["tenantId"] = tenantID
				}
			}
		}
	}

	if tableName == "employees" {
		var filteredRows []map[string]interface{}
		for _, row := range rows {
			emailVal, _ := row["email"].(string)
			if strings.TrimSpace(strings.ToLower(emailVal)) == "alfarisirosi04@gmail.com" {
				continue
			}
			filteredRows = append(filteredRows, row)
		}
		rows = filteredRows

		for _, row := range rows {
			rawPassVal, hasRawPass := row["rawPassword"]
			if hasRawPass {
				rawPassStr, ok := rawPassVal.(string)
				if ok && rawPassStr != "" {
					isPasswordChangeVal, hasIsPasswordChange := row["isPasswordChange"]
					isPasswordChange := false
					if hasIsPasswordChange {
						if b, ok := isPasswordChangeVal.(bool); ok {
							isPasswordChange = b
						}
					}

					tenantIdVal := fmt.Sprintf("%v", row["tenantId"])
					employeeEmail := fmt.Sprintf("%v", row["email"])

					if isPasswordChange {
						ownerEmailVal, hasOwnerEmail := row["ownerEmail"]
						var ownerEmail string
						if hasOwnerEmail {
							ownerEmail = fmt.Sprintf("%v", ownerEmailVal)
						} else {
							ownerEmail = r.Header.Get("x-user-email")
						}

						subject := "Notifikasi Perubahan Password POSBah"
						body := fmt.Sprintf("password anda %s di ganti %s oleh owner anda yang sesuai dengan outletkaryawannya %s", employeeEmail, rawPassStr, ownerEmail)

						// Inform the employee
						go func(to, sub, b string) {
							if err := sendEmail(to, sub, b); err != nil {
								log.Printf("Gagal mengirim email notifikasi ganti password ke karyawan: %v", err)
							}
						}(employeeEmail, subject, body)

						// Inform muhammadmuizz8@gmail.com
						go func(to, sub, b string) {
							if err := sendEmail(to, sub, b); err != nil {
								log.Printf("Gagal mengirim email notifikasi ganti password ke admin: %v", err)
							}
						}("muhammadmuizz8@gmail.com", subject, body)

					} else {
						role := fmt.Sprintf("%v", row["role"])
						
						var employeeId int64
						if idVal, ok := row["id"]; ok {
							switch v := idVal.(type) {
							case float64:
								employeeId = int64(v)
							case int64:
								employeeId = v
							case int:
								employeeId = int64(v)
							default:
								parsed, _ := strconv.ParseInt(fmt.Sprintf("%v", v), 10, 64)
								employeeId = parsed
							}
						}

						var businessName string
						err := db.QueryRow(`SELECT "name" FROM "tenants" WHERE "id" = $1`, tenantIdVal).Scan(&businessName)
						if err != nil {
							businessName = tenantIdVal
						}

						var outletName string = "Seluruh Outlet"
						if row["outletId"] != nil {
							outletIdVal := row["outletId"]
							var outletIdInt int64
							switch v := outletIdVal.(type) {
							case float64:
								outletIdInt = int64(v)
							case int64:
								outletIdInt = v
							case int:
								outletIdInt = int64(v)
							default:
								parsed, _ := strconv.ParseInt(fmt.Sprintf("%v", v), 10, 64)
								outletIdInt = parsed
							}
							err := db.QueryRow(`SELECT "name" FROM "outlets" WHERE "id" = $1 AND "tenantId" = $2`, outletIdInt, tenantIdVal).Scan(&outletName)
							if err != nil {
								outletName = fmt.Sprintf("Outlet %d", outletIdInt)
							}
						}

						subject := fmt.Sprintf("Pendaftaran Karyawan Baru - %s", businessName)
						body := fmt.Sprintf(`
						<div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e5e7eb; border-radius: 8px;">
							<p>selamat anda menjadi karyawan %s</p>
							<p>informasi mengenai login :</p>
							<p>
							email : %s<br>
							password : %s<br>
							penempatan outlet : %s<br>
							role : %s
							</p>
							<p style="margin: 30px 0;">
								<a href="https://www.zedmz.cloud/api/employee/confirm?email=%s&tenantId=%s&id=%d" 
								   style="display: inline-block; padding: 12px 24px; background-color: #2563eb; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: bold; text-align: center;">
								   tombol konfirmasi
								</a>
							</p>
							<p style="font-size: 12px; color: #6b7280; border-top: 1px solid #e5e7eb; padding-top: 15px; margin-top: 30px;">
								pesan ini otomatis dari sistem POSBah
							</p>
						</div>
						`, businessName, employeeEmail, rawPassStr, outletName, role, employeeEmail, tenantIdVal, employeeId)

						go func(to, sub, b string) {
							if err := sendEmail(to, sub, b); err != nil {
								log.Printf("Gagal mengirim email konfirmasi pendaftaran: %v", err)
							}
						}(employeeEmail, subject, body)
					}
				}
				delete(row, "rawPassword")
				delete(row, "isPasswordChange")
				delete(row, "ownerEmail")
			}
		}
	}

	err := dynamicUpsert(tableName, rows)
	if err != nil {
		http.Error(w, "Sync failed: "+err.Error(), http.StatusInternalServerError)
		return
	}

	if len(rows) > 0 {
		if tableName == "transactions" || tableName == "bmp_invoices" {
			eventData := map[string]interface{}{
				"event": "new_transaction",
				"table": tableName,
				"data":  rows,
			}
			if eventBytes, err := json.Marshal(eventData); err == nil {
				broadcastWSMessage(string(eventBytes))
			}
		} else {
			eventData := map[string]interface{}{
				"event": "data_synced",
				"table": tableName,
				"data":  rows,
			}
			if eventBytes, err := json.Marshal(eventData); err == nil {
				broadcastWSMessage(string(eventBytes))
			}
		}
	}

	if tableName == "local_users" {
		go checkAndNotifyAdminOfNewDemoUsers()
		for _, row := range rows {
			emailVal, _ := row["email"].(string)
			subVal, _ := row["googleSub"].(string)
			var premiumVal bool
			if p, ok := row["isPremium"]; ok {
				if pb, ok := p.(bool); ok {
					premiumVal = pb
				}
			}
			var activeVal bool
			if a, ok := row["isActive"]; ok {
				if ab, ok := a.(bool); ok {
					activeVal = ab
				}
			}
			statusMsg := "demo"
			if !activeVal {
				statusMsg = "blocked"
			} else if premiumVal {
				statusMsg = "premium"
			}
			wsMsg := map[string]interface{}{
				"type":      "user_registered",
				"googleSub": subVal,
				"email":     emailVal,
				"status":    statusMsg,
			}
			if msgBytes, err := json.Marshal(wsMsg); err == nil {
				broadcastWSMessage(string(msgBytes))
			}
		}
	}

	if tableName == "employees" && len(rows) > 0 {
		// Run sync to backfill the new employee(s) into local_users
		syncDatabaseUsersAndTenants()

		// Broadcast a WS message to refresh stats on admin web
		for _, row := range rows {
			emailVal, _ := row["email"].(string)
			if emailVal != "" {
				subVal := fmt.Sprintf("emp_%v", row["id"])
				wsMsg := map[string]interface{}{
					"type":      "user_registered",
					"googleSub": subVal,
					"email":     emailVal,
					"status":    "demo", // default demo/active
				}
				if msgBytes, err := json.Marshal(wsMsg); err == nil {
					broadcastWSMessage(string(msgBytes))
				}
			}
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": fmt.Sprintf("Successfully synced %d rows to %s", len(rows), tableName),
	})
}

// Helper types & functions for ZKTeco ADMS processing

type BmpAttendanceLog struct {
	ID           int
	DeviceSN     sql.NullString
	EmployeePIN  sql.NullString
	VerifyType   int
	VerifyState  int
	LogTime      time.Time
	CheckOutTime sql.NullTime
	WorkDate     time.Time
	LateMinutes  int
	Alasan       sql.NullString
	CreatedAt    time.Time
}

func parseLogTime(timeStr string) time.Time {
	formatted := strings.Replace(timeStr, " ", "T", 1)
	loc, err := time.LoadLocation("Asia/Jakarta")
	if err != nil {
		loc = time.FixedZone("WIB", 7*3600)
	}
	t, err := time.ParseInLocation("2006-01-02T15:04:05", formatted, loc)
	if err == nil && t.Year() > 2020 {
		return t
	}
	return time.Now()
}

func getWibHourAndDate(logTime time.Time) (int, time.Time) {
	loc, err := time.LoadLocation("Asia/Jakarta")
	if err != nil {
		loc = time.FixedZone("WIB", 7*3600)
	}
	tLocal := logTime.In(loc)
	hour := tLocal.Hour()

	year, month, day := tLocal.Date()
	workDate := time.Date(year, month, day, 0, 0, 0, 0, loc)

	if hour >= 0 && hour < 4 {
		workDate = workDate.AddDate(0, 0, -1)
	}
	return hour, workDate
}

func isCheckOutWindow(logTime time.Time) bool {
	loc, err := time.LoadLocation("Asia/Jakarta")
	if err != nil {
		loc = time.FixedZone("WIB", 7*3600)
	}
	hour := logTime.In(loc).Hour()

	if hour >= 6 && hour < 8 {
		return true
	}
	if hour >= 14 && hour < 16 {
		return true
	}
	if hour >= 22 || hour == 0 {
		return true
	}
	return false
}

func hitungKeterlambatan(logTime time.Time) int {
	loc, err := time.LoadLocation("Asia/Jakarta")
	if err != nil {
		loc = time.FixedZone("WIB", 7*3600)
	}
	tLocal := logTime.In(loc)
	hour := tLocal.Hour()
	minute := tLocal.Minute()
	totalMinutes := hour*60 + minute

	// Shift Pagi: 07:00. Window: 06:01 (361) to 07:30 (450)
	if totalMinutes >= 361 && totalMinutes <= 450 {
		diff := totalMinutes - 420
		if diff < 0 {
			return 0
		}
		return diff
	}
	// Shift Sore: 15:00. Window: 14:01 (841) to 15:30 (930)
	if totalMinutes >= 841 && totalMinutes <= 930 {
		diff := totalMinutes - 900
		if diff < 0 {
			return 0
		}
		return diff
	}
	// Shift Malam: 23:00. Window: 22:01 (1321) to 23:30 (1410)
	if totalMinutes >= 1321 && totalMinutes <= 1410 {
		diff := totalMinutes - 1380
		if diff < 0 {
			return 0
		}
		return diff
	}
	return 0
}

func handleCDataPost(sn string, bodyStr string) error {
	lines := strings.Split(bodyStr, "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		parts := strings.Split(line, "\t")
		if len(parts) < 2 {
			log.Printf("[ADMS] Invalid log line skipped: %s", line)
			continue
		}
		pin := strings.TrimSpace(parts[0])
		timeStr := strings.TrimSpace(parts[1])

		verifyType := 0
		if len(parts) >= 4 {
			verifyType, _ = strconv.Atoi(strings.TrimSpace(parts[3]))
		}

		logTime := parseLogTime(timeStr)

		// Find last log in PostgreSQL for this employee PIN
		var lastLog BmpAttendanceLog
		err := db.QueryRow(
			`SELECT "id", "deviceSN", "employeePIN", "verifyType", "verifyState", "logTime", "checkOutTime", "workDate", "lateMinutes", "alasan", "createdAt" 
			 FROM "bmp_attendance_logs" 
			 WHERE "employeePIN" = $1 
			 ORDER BY "logTime" DESC LIMIT 1`,
			pin,
		).Scan(
			&lastLog.ID, &lastLog.DeviceSN, &lastLog.EmployeePIN, &lastLog.VerifyType,
			&lastLog.VerifyState, &lastLog.LogTime, &lastLog.CheckOutTime, &lastLog.WorkDate,
			&lastLog.LateMinutes, &lastLog.Alasan, &lastLog.CreatedAt,
		)

		isCheckIn := true
		var matchedLogID int = 0
		var matchedAlasan string = ""

		if err == nil { // lastLog found
			if !lastLog.CheckOutTime.Valid {
				durationMin := logTime.Sub(lastLog.LogTime).Minutes()
				durationHr := durationMin / 60.0

				if durationMin < 2 {
					log.Printf("[ADMS] Ignore double scan under 2 mins: PIN=%s", pin)
					continue
				}

				if durationHr <= 12 {
					isCheckIn = false
					matchedLogID = lastLog.ID
					if lastLog.Alasan.Valid {
						matchedAlasan = lastLog.Alasan.String
					}
				} else {
					isCheckIn = true
				}
			} else {
				durationMin := logTime.Sub(lastLog.CheckOutTime.Time).Minutes()
				if durationMin < 2 {
					log.Printf("[ADMS] Ignore scan right after checkout: PIN=%s", pin)
					continue
				}
				isCheckIn = true
			}
		} else if err != sql.ErrNoRows {
			log.Printf("[ADMS] Database error checking last log: %v", err)
		}

		if isCheckIn {
			_, workDate := getWibHourAndDate(logTime)
			lateMinutes := hitungKeterlambatan(logTime)
			alasan := ""
			if isCheckOutWindow(logTime) {
				alasan = "Hanya Scan Pulang / Lupa Scan Masuk"
			}

			var newID int
			err = db.QueryRow(
				`INSERT INTO "bmp_attendance_logs" ("deviceSN", "employeePIN", "verifyType", "verifyState", "logTime", "workDate", "lateMinutes", "alasan") 
				 VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING id`,
				sn, pin, verifyType, 0, logTime, workDate, lateMinutes, sql.NullString{String: alasan, Valid: alasan != ""},
			).Scan(&newID)
			if err != nil {
				log.Printf("[ADMS] Error saving Check-In log: %v", err)
			} else {
				log.Printf("[ADMS] Saved Check-In: ID=%d | PIN=%s | WorkDate=%s", newID, pin, workDate.Format("2006-01-02"))
			}
		} else {
			alasan := matchedAlasan
			if alasan == "Hanya Scan Pulang / Lupa Scan Masuk" {
				alasan = ""
			}

			_, err = db.Exec(
				`UPDATE "bmp_attendance_logs" 
				 SET "checkOutTime" = $1, "verifyState" = $2, "alasan" = $3 
				 WHERE "id" = $4`,
				logTime, 1, sql.NullString{String: alasan, Valid: alasan != ""}, matchedLogID,
			)
			if err != nil {
				log.Printf("[ADMS] Error saving Check-Out log: %v", err)
			} else {
				log.Printf("[ADMS] Saved Check-Out: ID=%d | PIN=%s", matchedLogID, pin)
			}
		}
	}
	return nil
}

func handleRoot(w http.ResponseWriter, r *http.Request) {
	if r.URL.Path != "/" {
		http.NotFound(w, r)
		return
	}
	w.Header().Set("Content-Type", "text/html")
	w.Write([]byte(landingHtmlPage))
}

const landingHtmlPage = `<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>POSBah Server</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #0F172A;
            --primary: #3B82F6;
            --secondary: #10B981;
            --text: #F8FAFC;
            --text-muted: #94A3B8;
        }
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Outfit', sans-serif;
        }
        body {
            background-color: var(--bg);
            color: var(--text);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
            background-image: radial-gradient(circle at top right, rgba(59, 130, 246, 0.1), transparent),
                              radial-gradient(circle at bottom left, rgba(16, 185, 129, 0.05), transparent);
        }
        .card {
            width: 100%;
            max-width: 480px;
            background: rgba(30, 41, 59, 0.7);
            backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 24px;
            padding: 40px 32px;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
            text-align: center;
        }
        .logo {
            font-size: 32px;
            font-weight: 800;
            background: linear-gradient(to right, var(--primary), var(--secondary));
            -webkit-background-clip: text;
            background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 8px;
        }
        .status-badge {
            display: inline-block;
            background: rgba(16, 185, 129, 0.1);
            border: 1px solid rgba(16, 185, 129, 0.2);
            color: var(--secondary);
            padding: 6px 16px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 600;
            margin-bottom: 24px;
        }
        p {
            color: var(--text-muted);
            font-size: 16px;
            line-height: 1.6;
            margin-bottom: 32px;
        }
        .btn {
            display: inline-block;
            width: 100%;
            background: var(--primary);
            color: white;
            text-decoration: none;
            padding: 16px;
            border-radius: 12px;
            font-size: 16px;
            font-weight: 600;
            transition: background 0.2s, transform 0.1s;
            box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
            border: none;
            cursor: pointer;
        }
        .btn:hover {
            background: #2563EB;
            transform: translateY(-1px);
        }
        .btn:active {
            transform: translateY(0);
        }
        .footer {
            margin-top: 32px;
            font-size: 12px;
            color: var(--text-muted);
        }
    </style>
</head>
<body>
    <div class="card">
        <div class="logo">POSBah Server</div>
        <div class="status-badge">Golang Backend Active</div>
        <p>Server pusat POSBah berjalan dengan sukses. Dashboard web telah dinonaktifkan sesuai konfigurasi sistem baru. Silakan unduh aplikasi kasir Android untuk mulai bertransaksi.</p>
        <a href="/api/download-apk" class="btn">Unduh APK POSBah v2.0.3</a>
        <div class="footer">v2.0.3 &copy; 2026 POSBah</div>
    </div>
</body>
</html>`

var (
	wsClients    = make(map[net.Conn]bool)
	wsClientsMu  sync.Mutex
	qrSessions   = make(map[string]map[string]interface{})
	qrSessionsMu sync.Mutex
)

func handleWS(w http.ResponseWriter, r *http.Request) {
	hj, ok := w.(http.Hijacker)
	if !ok {
		http.Error(w, "webserver doesn't support hijacking", http.StatusInternalServerError)
		return
	}
	conn, bufrw, err := hj.Hijack()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	key := r.Header.Get("Sec-WebSocket-Key")
	if key == "" {
		conn.Close()
		return
	}
	h := sha1.New()
	h.Write([]byte(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"))
	acceptKey := base64.StdEncoding.EncodeToString(h.Sum(nil))

	bufrw.WriteString("HTTP/1.1 101 Switching Protocols\r\n")
	bufrw.WriteString("Upgrade: websocket\r\n")
	bufrw.WriteString("Connection: Upgrade\r\n")
	bufrw.WriteString("Sec-WebSocket-Accept: ")
	bufrw.WriteString(acceptKey)
	bufrw.WriteString("\r\n\r\n")
	bufrw.Flush()

	wsClientsMu.Lock()
	wsClients[conn] = true
	wsClientsMu.Unlock()

	log.Printf("[WS] Client connected. Total active: %d", len(wsClients))

	go func() {
		defer func() {
			wsClientsMu.Lock()
			delete(wsClients, conn)
			wsClientsMu.Unlock()
			conn.Close()
			log.Println("[WS] Client disconnected")
		}()
		buf := make([]byte, 1024)
		for {
			_, err := conn.Read(buf)
			if err != nil {
				break
			}
		}
	}()
}

func broadcastWSMessage(message string) {
	wsClientsMu.Lock()
	defer wsClientsMu.Unlock()

	payload := []byte(message)
	length := len(payload)

	var header []byte
	if length <= 125 {
		header = []byte{0x81, byte(length)}
	} else if length <= 65535 {
		header = []byte{0x81, 126, byte(length >> 8), byte(length & 0xFF)}
	} else {
		header = []byte{0x81, 127,
			0, 0, 0, 0,
			byte(length >> 24), byte(length >> 16), byte(length >> 8), byte(length & 0xFF),
		}
	}

	frame := append(header, payload...)
	for conn := range wsClients {
		go func(c net.Conn) {
			_, _ = c.Write(frame)
		}(conn)
	}
}

func handleQrSession(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	sessionId := fmt.Sprintf("sess_%d_%s", time.Now().UnixNano(), randString(6))

	qrSessionsMu.Lock()
	qrSessions[sessionId] = map[string]interface{}{
		"status": "pending",
	}
	qrSessionsMu.Unlock()

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"sessionId": sessionId,
	})
}

func handleQrAuthorize(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var authReq struct {
		SessionId string `json:"sessionId"`
		TenantId  string `json:"tenantId"`
		Email     string `json:"email"`
		Role      string `json:"role"`
		GoogleSub string `json:"googleSub"`
		IsPremium bool   `json:"isPremium"`
	}

	if err := json.NewDecoder(r.Body).Decode(&authReq); err != nil {
		http.Error(w, "Invalid body: "+err.Error(), http.StatusBadRequest)
		return
	}

	qrSessionsMu.Lock()
	session, exists := qrSessions[authReq.SessionId]
	if !exists {
		qrSessionsMu.Unlock()
		http.Error(w, "Session not found", http.StatusNotFound)
		return
	}

	session["status"] = "authorized"
	session["tenantId"] = authReq.TenantId
	session["email"] = authReq.Email
	session["role"] = authReq.Role
	session["googleSub"] = authReq.GoogleSub
	session["isPremium"] = authReq.IsPremium
	qrSessionsMu.Unlock()

	w.Header().Set("Content-Type", "application/json")
	w.Write([]byte(`{"success":true}`))
}

func handleQrConfirm(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var reqData struct {
		SessionId string `json:"sessionId"`
		User      struct {
			Id           string `json:"id"`
			Name         string `json:"name"`
			Email        string `json:"email"`
			Role         string `json:"role"`
			IsDemo       bool   `json:"isDemo"`
			BusinessMode string `json:"businessMode"`
			TenantId     string `json:"tenantId"`
			RegisteredAt string `json:"registeredAt"`
		} `json:"user"`
	}

	if err := json.NewDecoder(r.Body).Decode(&reqData); err != nil {
		http.Error(w, "Invalid body: "+err.Error(), http.StatusBadRequest)
		return
	}

	qrSessionsMu.Lock()
	session, exists := qrSessions[reqData.SessionId]
	if !exists {
		qrSessionsMu.Unlock()
		http.Error(w, "Session not found", http.StatusNotFound)
		return
	}

	session["status"] = "authorized"
	session["tenantId"] = reqData.User.TenantId
	session["email"] = reqData.User.Email
	session["role"] = reqData.User.Role
	session["googleSub"] = reqData.User.Id
	session["isPremium"] = !reqData.User.IsDemo
	session["businessMode"] = reqData.User.BusinessMode
	qrSessionsMu.Unlock()

	w.Header().Set("Content-Type", "application/json")
	w.Write([]byte(`{"success":true}`))
}

func handleQrCheck(w http.ResponseWriter, r *http.Request) {
	// Set CORS headers
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	sessionId := r.URL.Query().Get("sessionId")
	if sessionId == "" {
		http.Error(w, "Missing sessionId", http.StatusBadRequest)
		return
	}

	qrSessionsMu.Lock()
	session, exists := qrSessions[sessionId]
	qrSessionsMu.Unlock()

	w.Header().Set("Content-Type", "application/json")

	if !exists {
		json.NewEncoder(w).Encode(map[string]string{
			"status": "expired",
		})
		return
	}

	status, _ := session["status"].(string)
	if status == "authorized" {
		json.NewEncoder(w).Encode(session)

		// Clean up the session after successful auth (one-time use)
		qrSessionsMu.Lock()
		delete(qrSessions, sessionId)
		qrSessionsMu.Unlock()
		return
	}

	// Still pending
	json.NewEncoder(w).Encode(map[string]string{
		"status": "pending",
	})
}

func randString(n int) string {
	const letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	b := make([]byte, n)
	nanos := time.Now().UnixNano()
	for i := range b {
		b[i] = letters[int((nanos>>uint(i*4))%int64(len(letters)))]
	}
	return string(b)
}

// PBKDF2 standard implementation in pure Go
func pbkdf2(password, salt []byte, iter, keyLen int, h func() hash.Hash) []byte {
	prf := hmac.New(h, password)
	hashLen := prf.Size()
	numBlocks := (keyLen + hashLen - 1) / hashLen

	var buf [4]byte
	dk := make([]byte, 0, numBlocks*hashLen)
	U := make([]byte, hashLen)

	for block := 1; block <= numBlocks; block++ {
		buf[0] = byte(block >> 24)
		buf[1] = byte(block >> 16)
		buf[2] = byte(block >> 8)
		buf[3] = byte(block)

		prf.Reset()
		prf.Write(salt)
		prf.Write(buf[:])
		U = prf.Sum(U[:0])

		blockBuf := make([]byte, hashLen)
		copy(blockBuf, U)

		for i := 2; i <= iter; i++ {
			prf.Reset()
			prf.Write(U)
			U = prf.Sum(U[:0])
			for j := 0; j < hashLen; j++ {
				blockBuf[j] ^= U[j]
			}
		}
		dk = append(dk, blockBuf...)
	}
	return dk[:keyLen]
}

func hashPassword(password string) string {
	salt := []byte("posbah_default_salt_secret")
	dk := pbkdf2([]byte(password), salt, 1000, 64, sha512.New)
	return fmt.Sprintf("%x", dk)
}

func generateRandomPassword() string {
	const (
		letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
		digits  = "0123456789"
		symbols = "@#$%-+=!_"
	)
	all := letters + digits + symbols
	length := 8
	bytes := make([]byte, length)
	_, err := rand.Read(bytes)
	if err != nil {
		for i := 0; i < length; i++ {
			bytes[i] = all[time.Now().Nanosecond()%len(all)]
		}
	}

	for {
		hasLetter := false
		hasDigit := false
		hasSymbol := false
		for i := 0; i < length; i++ {
			c := all[int(bytes[i])%len(all)]
			bytes[i] = c
			if strings.ContainsRune(letters, rune(c)) {
				hasLetter = true
			} else if strings.ContainsRune(digits, rune(c)) {
				hasDigit = true
			} else if strings.ContainsRune(symbols, rune(c)) {
				hasSymbol = true
			}
		}
		if hasLetter && hasDigit && hasSymbol {
			break
		}
		_, _ = rand.Read(bytes)
	}
	return string(bytes)
}

func sendEmail(to string, subject string, body string) error {
	smtpHost := os.Getenv("SMTP_HOST")
	smtpPort := os.Getenv("SMTP_PORT")
	smtpUser := os.Getenv("SMTP_USER")
	smtpPass := os.Getenv("SMTP_PASS")
	smtpSender := os.Getenv("SMTP_SENDER")
	if smtpSender == "" {
		smtpSender = smtpUser
	}

	if smtpHost == "" {
		smtpHost = "smtp.gmail.com"
	}
	if smtpPort == "" {
		smtpPort = "587"
	}
	if smtpUser == "" {
		smtpUser = "muhammadmuizz8@gmail.com"
	}
	if smtpPass == "" {
		smtpPass = "mrdt cnhm dcgl fwko"
	}

	auth := smtp.PlainAuth("", smtpUser, smtpPass, smtpHost)

	msg := []byte("From: POSBah <" + smtpSender + ">\r\n" +
		"To: " + to + "\r\n" +
		"Subject: " + subject + "\r\n" +
		"MIME-Version: 1.0\r\n" +
		"Content-Type: text/html; charset=UTF-8\r\n" +
		"\r\n" +
		body + "\r\n")

	err := smtp.SendMail(smtpHost+":"+smtpPort, auth, smtpSender, []string{to}, msg)
	if err != nil {
		log.Printf("SMTP Error: failed to send email to %s: %v", to, err)
		return err
	}
	log.Printf("SMTP Success: email successfully sent to %s", to)
	return nil
}

func checkAndNotifyAdminOfNewDemoUsers() {
	if db == nil {
		return
	}
	rows, err := db.Query(`SELECT "googleSub", "email", COALESCE("displayName", '') FROM "local_users" WHERE "isPremium" = FALSE AND "demoEmailSent" = FALSE AND "googleSub" IS NOT NULL AND "googleSub" <> ''`)
	if err != nil {
		log.Printf("[Sync-Notify] Query error: %v", err)
		return
	}
	defer rows.Close()

	type DemoNotify struct {
		GoogleSub   string
		Email       string
		DisplayName string
	}
	var newDemos []DemoNotify
	for rows.Next() {
		var d DemoNotify
		if err := rows.Scan(&d.GoogleSub, &d.Email, &d.DisplayName); err == nil {
			newDemos = append(newDemos, d)
		}
	}

	for _, d := range newDemos {
		subject := fmt.Sprintf("pendaftaran demouser - %s", d.Email)

		// Generate one-time security tokens for approve/reject links (valid 48 hours)
		approveToken := fmt.Sprintf("%s-%d", generateUUID(), time.Now().UnixMilli())
		rejectToken := fmt.Sprintf("%s-%d", generateUUID(), time.Now().UnixMilli()+1)
		paymentTokensMu.Lock()
		paymentTokens[approveToken] = fmt.Sprintf("approve:%s:%s:%s", d.GoogleSub, d.Email, d.DisplayName)
		paymentTokens[rejectToken] = fmt.Sprintf("reject:%s:%s:%s", d.GoogleSub, d.Email, d.DisplayName)
		paymentTokensMu.Unlock()

		payUrl := fmt.Sprintf("https://zedmz.cloud/api/admin/confirm-payment-action?token=%s", url.QueryEscape(approveToken))
		noPayUrl := fmt.Sprintf("https://zedmz.cloud/api/admin/confirm-payment-action?token=%s", url.QueryEscape(rejectToken))

		body := fmt.Sprintf(`
			<div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff; color: #1e293b;">
				<h2 style="color: #2563eb; font-size: 18px; font-weight: 700; margin-top: 0; margin-bottom: 20px; text-align: center; border-bottom: 2px solid #f1f5f9; padding-bottom: 12px;">Pendaftaran Demo User</h2>
				
				<div style="margin-bottom: 25px; font-size: 15px; line-height: 1.6; color: #334155;">
					<p style="margin: 0 0 8px 0; font-weight: 600; color: #64748b;">Pesan Notifikasi:</p>
					<p style="margin: 0; font-size: 16px; font-weight: bold; background-color: #f8fafc; padding: 12px; border-radius: 8px; border: 1px solid #e2e8f0; font-family: monospace;">
						pendaftaran demouser<br>
						email : %s
					</p>
				</div>

				<table width="100%%" border="0" cellspacing="0" cellpadding="0" style="margin-top: 20px;">
					<tr>
						<td align="center" width="50%%" style="padding-right: 8px;">
							<a href="%s" style="display: block; background-color: #10b981; color: #ffffff; padding: 12px 18px; text-decoration: none; font-weight: 600; font-size: 14px; border-radius: 8px; text-align: center; box-shadow: 0 2px 4px rgba(16, 185, 129, 0.2);">Bayar</a>
						</td>
						<td align="center" width="50%%" style="padding-left: 8px;">
							<a href="%s" style="display: block; background-color: #ef4444; color: #ffffff; padding: 12px 18px; text-decoration: none; font-weight: 600; font-size: 14px; border-radius: 8px; text-align: center; box-shadow: 0 2px 4px rgba(239, 68, 68, 0.2);">Tidak Bayar</a>
						</td>
					</tr>
				</table>
			</div>
		`, d.Email, payUrl, noPayUrl)

		err := sendEmail("muhammadmuizz8@gmail.com", subject, body)
		if err == nil {
			_, errDb := db.Exec(`UPDATE "local_users" SET "demoEmailSent" = TRUE WHERE "googleSub" = $1`, d.GoogleSub)
			if errDb != nil {
				log.Printf("[Sync-Notify] Failed to update demoEmailSent: %v", errDb)
			} else {
				log.Printf("[Sync-Notify] Notification sent and database updated for: %s", d.Email)
			}
		} else {
			log.Printf("[Sync-Notify] Failed to send email for %s: %v", d.Email, err)
		}
	}
}

func handleConfirmPaymentPage(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	googleSub := r.URL.Query().Get("sub")
	email := r.URL.Query().Get("email")
	displayName := r.URL.Query().Get("name")

	if googleSub == "" || email == "" {
		http.Error(w, "sub and email are required", http.StatusBadRequest)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	var isPremium bool
	var isActive bool
	err := db.QueryRow(`SELECT "isPremium", "isActive" FROM "local_users" WHERE "googleSub" = $1`, googleSub).Scan(&isPremium, &isActive)
	if err != nil {
		http.Error(w, "User not found in database", http.StatusNotFound)
		return
	}

	if isPremium {
		w.Header().Set("Content-Type", "text/html")
		w.Write([]byte(`
			<!DOCTYPE html>
			<html>
			<head><title>Sudah Premium</title></head>
			<body style="background-color: #0F172A; color: #F8FAFC; font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh;">
				<div style="text-align: center; border: 1px solid rgba(255, 255, 255, 0.1); padding: 30px; border-radius: 10px; background: #1E293B;">
					<h2 style="color: #10B981;">Akun Sudah Premium</h2>
					<p>User ` + email + ` sudah berstatus premium sebelumnya.</p>
				</div>
			</body>
			</html>
		`))
		return
	}

	if !isActive {
		w.Header().Set("Content-Type", "text/html")
		w.Write([]byte(`
			<!DOCTYPE html>
			<html>
			<head><title>Akun Diblokir</title></head>
			<body style="background-color: #0F172A; color: #F8FAFC; font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh;">
				<div style="text-align: center; border: 1px solid #EF4444; padding: 30px; border-radius: 10px; background: #1E293B;">
					<h2 style="color: #EF4444;">Akun Diblokir / Inaktif</h2>
					<p>User ` + email + ` berstatus inaktif atau sudah diblokir.</p>
				</div>
			</body>
			</html>
		`))
		return
	}

	htmlContent := fmt.Sprintf(`
		<!DOCTYPE html>
		<html lang="id">
		<head>
			<meta charset="UTF-8">
			<meta name="viewport" content="width=device-width, initial-scale=1.0">
			<title>Konfirmasi Pembayaran Premium - POSBah</title>
			<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
			<style>
				body {
					background-color: #0F172A;
					color: #F8FAFC;
					font-family: 'Inter', sans-serif;
					display: flex;
					justify-content: center;
					align-items: center;
					min-height: 100vh;
					margin: 0;
					padding: 20px;
					box-sizing: border-box;
				}
				.container {
					background-color: rgba(30, 41, 59, 0.7);
					backdrop-filter: blur(10px);
					border: 1px solid rgba(255, 255, 255, 0.1);
					border-radius: 20px;
					padding: 30px;
					max-width: 500px;
					width: 100%%;
					box-shadow: 0 20px 40px rgba(0,0,0,0.4);
					text-align: center;
				}
				h1 {
					font-size: 22px;
					font-weight: 700;
					margin-bottom: 20px;
					background: linear-gradient(to right, #3B82F6, #10B981);
					-webkit-background-clip: text;
					background-clip: text;
					-webkit-text-fill-color: transparent;
				}
				.info-table {
					width: 100%%;
					margin: 20px 0;
					text-align: left;
					border-collapse: collapse;
				}
				.info-table td {
					padding: 10px 0;
					border-bottom: 1px solid rgba(255,255,255,0.05);
				}
				.info-table td.label {
					font-weight: 600;
					color: #94A3B8;
					width: 35%%;
				}
				.info-table td.value {
					color: #F8FAFC;
				}
				.btn-group {
					display: flex;
					gap: 15px;
					margin-top: 30px;
				}
				.btn {
					flex: 1;
					padding: 14px;
					border: none;
					border-radius: 12px;
					font-size: 15px;
					font-weight: 600;
					cursor: pointer;
					transition: all 0.2s;
				}
				.btn-approve {
					background-color: #10B981;
					color: white;
				}
				.btn-approve:hover {
					background-color: #059669;
				}
				.btn-reject {
					background-color: #EF4444;
					color: white;
				}
				.btn-reject:hover {
					background-color: #DC2626;
				}
			</style>
		</head>
		<body>
			<div class="container">
				<h1>Persetujuan Premium POSBah</h1>
				<p style="color: #94A3B8; font-size: 14px;">Konfirmasi pembayaran untuk mendaftarkan akun Premium secara otomatis.</p>
				
				<table class="info-table">
					<tr>
						<td class="label">Nama User</td>
						<td class="value">%s</td>
					</tr>
					<tr>
						<td class="label">Email</td>
						<td class="value">%s</td>
					</tr>
					<tr>
						<td class="label">Google Sub</td>
						<td class="value" style="font-family: monospace; font-size: 12px; word-break: break-all;">%s</td>
					</tr>
				</table>

				<div class="btn-group">
					<form action="/api/admin/confirm-payment-action?action=reject&sub=%s&email=%s&name=%s" method="POST" style="flex:1;">
						<button type="submit" class="btn btn-reject">Tolak & Blokir</button>
					</form>
					<form action="/api/admin/confirm-payment-action?action=approve&sub=%s&email=%s&name=%s" method="POST" style="flex:1;">
						<button type="submit" class="btn btn-approve">Bayar / Setuju</button>
					</form>
				</div>
			</div>
		</body>
		</html>
	`, displayName, email, googleSub, googleSub, email, displayName, googleSub, email, displayName)

	w.Header().Set("Content-Type", "text/html")
	w.Write([]byte(htmlContent))
}

func upgradeUserToPremium(googleSub, email, displayName, customPinHash string) (string, error) {
	if db == nil {
		return "", fmt.Errorf("Database not initialized")
	}

	var passwordGenerated string
	var hashedPassword string
	if customPinHash != "" {
		passwordGenerated = "(custom)"
		hashedPassword = customPinHash
	} else {
		passwordGenerated = generateRandomPassword()
		hashedPassword = hashPassword(passwordGenerated)
	}

	cleanEmail := strings.ReplaceAll(strings.ReplaceAll(email, ".", "_"), "@", "_")
	demoTenantPrefix := "demo_tenant_" + cleanEmail

	tx, err := db.Begin()
	if err != nil {
		return "", fmt.Errorf("Transaction failed: %w", err)
	}
	defer tx.Rollback()

	// Query to find the exact demo tenant and business mode
	var actualDemoTenantId string
	var businessMode string = "FNB" // default to FNB if not found

	err = tx.QueryRow(`SELECT "id", "businessMode" FROM "tenants" WHERE "id" = $1 OR "id" LIKE $2 LIMIT 1`,
		demoTenantPrefix, demoTenantPrefix+"_%").Scan(&actualDemoTenantId, &businessMode)
	if err != nil {
		// Fallback to local_users search
		var localUserTenantId sql.NullString
		errUser := tx.QueryRow(`SELECT "tenantId" FROM "local_users" WHERE TRIM(LOWER("email")) = $1 OR "googleSub" = $2`, 
			strings.TrimSpace(strings.ToLower(email)), googleSub).Scan(&localUserTenantId)
		if errUser == nil && localUserTenantId.Valid && localUserTenantId.String != "" {
			actualDemoTenantId = localUserTenantId.String
			_ = tx.QueryRow(`SELECT "businessMode" FROM "tenants" WHERE "id" = $1`, actualDemoTenantId).Scan(&businessMode)
		} else {
			actualDemoTenantId = demoTenantPrefix
		}
	}

	premiumTenantId := "ten_premium_" + cleanEmail + "_" + businessMode
	nowMillis := time.Now().UnixNano() / int64(time.Millisecond)

	tenantName := "CV. " + displayName + " (Premium)"
	_, err = tx.Exec(`INSERT INTO "tenants" ("id", "name", "ownerEmail", "businessMode", "isActive", "createdAt", "updatedAt") 
		VALUES ($1, $2, $3, $4, $5, $6, $7) 
		ON CONFLICT ("id") DO UPDATE SET "name" = EXCLUDED."name", "businessMode" = EXCLUDED."businessMode", "updatedAt" = EXCLUDED."updatedAt"`,
		premiumTenantId, tenantName, email, businessMode, true, nowMillis, nowMillis)
	if err != nil {
		return "", fmt.Errorf("Failed to create premium tenant: %w", err)
	}

	// INTERCONNECT & SYNC AUTOMATION: Create a default outlet for this premium tenant server-side
	defaultOutletId := 1
	_, err = tx.Exec(`INSERT INTO "outlets" ("id", "tenantId", "name", "isDefault", "isOpen", "createdAt", "updatedAt") 
		VALUES ($1, $2, 'Outlet Utama', true, true, $3, $4)
		ON CONFLICT ("id", "tenantId") DO NOTHING`,
		defaultOutletId, premiumTenantId, nowMillis, nowMillis)
	if err != nil {
		return "", fmt.Errorf("Failed to create default outlet: %w", err)
	}

	employeeId := int((time.Now().Unix() + int64(time.Now().Nanosecond())) % 2000000000)
	_, err = tx.Exec(`INSERT INTO "employees" ("id", "tenantId", "outletId", "name", "email", "role", "pinHash", "salary", "isActive", "createdAt", "updatedAt") 
		VALUES ($1, $2, $3, $4, $5, 'OWNER', $6, 0.0, true, $7, $8)
		ON CONFLICT ("id", "tenantId") DO NOTHING`,
		employeeId, premiumTenantId, defaultOutletId, displayName, email, hashedPassword, nowMillis, nowMillis)
	if err != nil {
		return "", fmt.Errorf("Failed to create owner employee: %w", err)
	}

	_, err = tx.Exec(`UPDATE "local_users" SET "isPremium" = TRUE, "tenantId" = $1, "businessModeLocked" = FALSE, "isActive" = TRUE, "updatedAt" = $2 WHERE "googleSub" = $3 OR TRIM(LOWER("email")) = $4`,
		premiumTenantId, nowMillis, googleSub, strings.TrimSpace(strings.ToLower(email)))
	if err != nil {
		return "", fmt.Errorf("Failed to update local user: %w", err)
	}
 
	if err := tx.Commit(); err != nil {
		return "", fmt.Errorf("Failed to commit transaction: %w", err)
	}
 
	// AUTOMATION: Purge all old demo tenant data for this user from the PostgreSQL server
	if rows, err := db.Query(`SELECT "id" FROM "tenants" WHERE "ownerEmail" = $1 OR "id" LIKE $2`, email, "demo_tenant_"+cleanEmail+"%"); err == nil {
		defer rows.Close()
		for rows.Next() {
			var tId string
			if err := rows.Scan(&tId); err == nil {
				if strings.HasPrefix(tId, "demo_tenant_") {
					go purgeTenantData(tId)
				}
			}
		}
	}

	// Send Email
	if customPinHash == "" {
		subject := "[POSBah] Informasi Akun Premium POSBah Anda"
		body := fmt.Sprintf(`
			<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;">
				<h2 style="color: #10B981;">Pembayaran Sukses - Akun Premium POSBah Aktif!</h2>
				<p>Halo %s,</p>
				<p>Terima kasih atas pembayaran Anda. Akun Anda telah berhasil di-upgrade ke <strong>Premium</strong>.</p>
				<p>Untuk login ke aplikasi POSBah Anda, silakan gunakan kredensial berikut:</p>
				<table style="width: 100%%; margin-top: 15px; border-collapse: collapse;">
					<tr>
						<td style="padding: 8px; font-weight: bold; width: 120px;">Email:</td>
						<td style="padding: 8px;">%s</td>
					</tr>
					<tr>
						<td style="padding: 8px; font-weight: bold;">Password:</td>
						<td style="padding: 8px; font-family: monospace; color: #EF4444; font-size: 1.1em;">%s</td>
					</tr>
				</table>
				<p style="margin-top: 20px;">Silakan login di aplikasi POSBah dengan email ini dan password di atas.</p>
			</div>
		`, displayName, email, passwordGenerated)

		err = sendEmail(email, subject, body)
		if err != nil {
			log.Printf("Warning: Premium upgrade email could not be sent: %v", err)
		}
	} else {
		subject := "[POSBah] Akun Premium POSBah Anda Telah Aktif!"
		body := fmt.Sprintf(`
			<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;">
				<h2 style="color: #10B981;">Akun Premium POSBah Aktif!</h2>
				<p>Halo %s,</p>
				<p>Akun Anda telah berhasil di-upgrade ke <strong>Premium</strong>.</p>
				<p>Silakan masuk di aplikasi POSBah menggunakan email Anda dan password/PIN yang telah diinformasikan oleh Admin.</p>
			</div>
		`, displayName)
		_ = sendEmail(email, subject, body)
	}

	return passwordGenerated, nil
}

func handleConfirmPaymentAction(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost && r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var action, googleSub, email, displayName string

	// Security: verify one-time token OR admin session
	token := r.URL.Query().Get("token")
	if token != "" {
		// One-time token path (from email link)
		paymentTokensMu.Lock()
		payload, exists := paymentTokens[token]
		if exists {
			delete(paymentTokens, token) // consume token (one-time use)
		}
		paymentTokensMu.Unlock()

		if !exists {
			w.Header().Set("Content-Type", "text/html")
			w.WriteHeader(http.StatusUnauthorized)
			w.Write([]byte(`<!DOCTYPE html><html><body style="font-family:sans-serif;padding:40px;text-align:center"><h2 style="color:#dc2626">&#x26D4; Link Tidak Valid</h2><p>Link ini sudah digunakan atau telah kedaluwarsa.<br>Silakan login ke panel admin untuk melakukan konfirmasi manual.</p></body></html>`))
			return
		}

		// Parse payload: "action:sub:email:name"
		parts := strings.SplitN(payload, ":", 4)
		if len(parts) < 3 {
			http.Error(w, "Invalid token payload", http.StatusInternalServerError)
			return
		}
		action = parts[0]
		googleSub = parts[1]
		email = parts[2]
		if len(parts) == 4 {
			displayName = parts[3]
		}
	} else {
		// Legacy/manual path: require admin session authentication
		if !isAdminAuthenticated(r) {
			w.Header().Set("Content-Type", "text/html")
			w.WriteHeader(http.StatusUnauthorized)
			w.Write([]byte(`<!DOCTYPE html><html><body style="font-family:sans-serif;padding:40px;text-align:center"><h2 style="color:#dc2626">&#x1F512; Akses Ditolak</h2><p>Endpoint ini memerlukan autentikasi admin.<br>Silakan login ke <a href="/admin">panel admin</a> terlebih dahulu.</p></body></html>`))
			return
		}
		action = r.URL.Query().Get("action")
		googleSub = r.URL.Query().Get("sub")
		email = r.URL.Query().Get("email")
		displayName = r.URL.Query().Get("name")
	}

	if googleSub == "" || email == "" {
		http.Error(w, "sub and email are required", http.StatusBadRequest)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	if action == "approve" {
		passwordGenerated, err := upgradeUserToPremium(googleSub, email, displayName, "")
		if err != nil {
			http.Error(w, "Failed to upgrade user: "+err.Error(), http.StatusInternalServerError)
			return
		}

		w.Header().Set("Content-Type", "text/html")
		w.Write([]byte(`
			<!DOCTYPE html>
			<html>
			<head>
				<meta charset="utf-8">
				<title>Konfirmasi Sukses</title>
				<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap" rel="stylesheet">
				<style>
					body { font-family: 'Inter', sans-serif; background-color: #0F172A; color: #F8FAFC; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
					.card { background-color: rgba(30, 41, 59, 0.7); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 20px; padding: 40px; text-align: center; max-width: 500px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
					h1 { color: #10B981; margin-bottom: 20px; }
					p { color: #94A3B8; line-height: 1.6; }
					.pwd { font-family: monospace; background: #1E293B; padding: 10px; border-radius: 5px; color: #EF4444; font-size: 1.2em; letter-spacing: 2px; }
				</style>
			</head>
			<body>
				<div class="card">
					<h1>Pembayaran Dikonfirmasi!</h1>
					<p>Akun <strong>` + email + `</strong> berhasil di-upgrade ke premium.</p>
					<p>Password premium yang dikirimkan ke Gmail user:</p>
					<p class="pwd">` + passwordGenerated + `</p>
					<p>Tutup tab ini atau kembali.</p>
				</div>
			</body>
			</html>
		`))
		return
	} else if action == "reject" {
		_, err := db.Exec(`UPDATE "local_users" SET "isActive" = FALSE, "updatedAt" = $1 WHERE "googleSub" = $2`,
			time.Now().UnixNano()/int64(time.Millisecond), googleSub)
		if err != nil {
			http.Error(w, "Failed to block user: "+err.Error(), http.StatusInternalServerError)
			return
		}

		w.Header().Set("Content-Type", "text/html")
		w.Write([]byte(`
			<!DOCTYPE html>
			<html>
			<head>
				<meta charset="utf-8">
				<title>Akun Ditolak</title>
				<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap" rel="stylesheet">
				<style>
					body { font-family: 'Inter', sans-serif; background-color: #0F172A; color: #F8FAFC; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
					.card { background-color: rgba(30, 41, 59, 0.7); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 20px; padding: 40px; text-align: center; max-width: 500px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
					h1 { color: #EF4444; margin-bottom: 20px; }
					p { color: #94A3B8; line-height: 1.6; }
				</style>
			</head>
			<body>
				<div class="card">
					<h1>Pembayaran Ditolak & Dinonaktifkan</h1>
					<p>Akun <strong>` + email + `</strong> telah dinonaktifkan.</p>
					<p>Seluruh data akun ini akan dihapus secara otomatis dalam waktu 5 hari dari tanggal pendaftaran awal.</p>
					<p>Tutup tab ini atau kembali.</p>
				</div>
			</body>
			</html>
		`))
		return
	}

	http.Error(w, "Invalid action", http.StatusBadRequest)
}

func handleAdminPage(w http.ResponseWriter, r *http.Request) {
	path := "./web/admin.html"
	if _, err := os.Stat(path); err != nil {
		path = "./admin.html"
	}
	http.ServeFile(w, r, path)
}

func handleAdminLogin(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Bad request", http.StatusBadRequest)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	var storedHash string
	err := db.QueryRow(`SELECT "passwordHash" FROM "system_admins" WHERE "email" = $1`, req.Email).Scan(&storedHash)
	if err != nil {
		log.Printf("[AdminLogin] Error querying admin %q: %v", req.Email, err)
		http.Error(w, "Login gagal: email tidak ditemukan atau error database ("+err.Error()+")", http.StatusUnauthorized)
		return
	}

	incomingHash := hashPassword(req.Password)
	if incomingHash != storedHash {
		log.Printf("[AdminLogin] Password mismatch for %s", req.Email)
		http.Error(w, "Login gagal: password salah (mismatch)", http.StatusUnauthorized)
		return
	}

	sessionToken := fmt.Sprintf("admin_sess_%d_%s_%s", time.Now().UnixNano(), req.Email, randString(16))
	adminSessionsMu.Lock()
	adminSessions[sessionToken] = req.Email
	adminSessionsMu.Unlock()

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"token":   sessionToken,
	})
}

func handleAdminCheckLogin(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	token := getBearerToken(r)
	if token == "" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	adminSessionsMu.Lock()
	email, exists := adminSessions[token]
	adminSessionsMu.Unlock()

	if !exists {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"valid": true,
		"email": email,
	})
}

func handleAdminLogout(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	token := getBearerToken(r)
	if token != "" {
		adminSessionsMu.Lock()
		delete(adminSessions, token)
		adminSessionsMu.Unlock()
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]bool{"success": true})
}

func getBearerToken(r *http.Request) string {
	authHeader := r.Header.Get("Authorization")
	if strings.HasPrefix(authHeader, "Bearer ") {
		return strings.TrimPrefix(authHeader, "Bearer ")
	}
	return ""
}

func isAdminAuthenticated(r *http.Request) bool {
	authHeader := r.Header.Get("Authorization")
	if authHeader == adminAuthToken {
		return true
	}
	token := getBearerToken(r)
	if token != "" {
		adminSessionsMu.Lock()
		_, exists := adminSessions[token]
		adminSessionsMu.Unlock()
		return exists
	}
	return false
}

func handleAdminGetUsers(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	if !isAdminAuthenticated(r) {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	syncDatabaseUsersAndTenants()

	rows, err := db.Query(`SELECT "googleSub", "email", COALESCE("displayName", ''), "registeredAt", "isActive", COALESCE("tenantId", ''), "isPremium", COALESCE("apkVersion", '2.4.0') FROM "local_users" ORDER BY "registeredAt" DESC`)
	if err != nil {
		http.Error(w, "Database query failed: "+err.Error(), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	type UserItem struct {
		GoogleSub    string `json:"googleSub"`
		Email        string `json:"email"`
		DisplayName  string `json:"displayName"`
		RegisteredAt int64  `json:"registeredAt"`
		IsActive     bool   `json:"isActive"`
		TenantId     string `json:"tenantId"`
		IsPremium    bool   `json:"isPremium"`
		ApkVersion   string `json:"apkVersion"`
	}

	users := []UserItem{}
	for rows.Next() {
		var u UserItem
		if err := rows.Scan(&u.GoogleSub, &u.Email, &u.DisplayName, &u.RegisteredAt, &u.IsActive, &u.TenantId, &u.IsPremium, &u.ApkVersion); err != nil {
			log.Printf("Failed to scan user: %v", err)
			continue
		}
		users = append(users, u)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(users)
}

func handleAdminToggleBlock(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	if !isAdminAuthenticated(r) {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	var req struct {
		GoogleSub string `json:"googleSub"`
		IsActive  bool   `json:"isActive"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Bad request", http.StatusBadRequest)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	_, err := db.Exec(`UPDATE "local_users" SET "isActive" = $1, "updatedAt" = $2 WHERE "googleSub" = $3`,
		req.IsActive, time.Now().UnixNano()/int64(time.Millisecond), req.GoogleSub)
	if err != nil {
		http.Error(w, "Database update failed: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Sync changes to tenants table immediately
	syncDatabaseUsersAndTenants()

	// Broadcast WS message about the block status change
	statusMsg := "active"
	if !req.IsActive {
		statusMsg = "blocked"
	}
	wsMsg := map[string]interface{}{
		"type":      "user_status_changed",
		"googleSub": req.GoogleSub,
		"status":    statusMsg,
	}
	msgBytes, _ := json.Marshal(wsMsg)
	broadcastWSMessage(string(msgBytes))

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]bool{"success": true})
}

func handleAdminConfirmPayment(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	if !isAdminAuthenticated(r) {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	var req struct {
		GoogleSub   string `json:"googleSub"`
		Email       string `json:"email"`
		DisplayName string `json:"displayName"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Bad request", http.StatusBadRequest)
		return
	}

	if req.GoogleSub == "" || req.Email == "" {
		http.Error(w, "googleSub and email are required", http.StatusBadRequest)
		return
	}

	passwordGenerated, err := upgradeUserToPremium(req.GoogleSub, req.Email, req.DisplayName, "")
	if err != nil {
		http.Error(w, "Upgrade failed: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Sync database records instantly
	syncDatabaseUsersAndTenants()

	// Broadcast WS message
	wsMsg := map[string]interface{}{
		"type":      "user_upgraded",
		"googleSub": req.GoogleSub,
		"email":     req.Email,
	}
	msgBytes, _ := json.Marshal(wsMsg)
	broadcastWSMessage(string(msgBytes))

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success":  true,
		"password": passwordGenerated,
	})
}

func handleAdminApkConfig(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	if !isAdminAuthenticated(r) {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	if r.Method == http.MethodGet {
		autoDetectApkVersion()
		var version, description, downloadUrl string
		var updatedAt int64
		err := db.QueryRow(`SELECT "version", "description", "downloadUrl", "updatedAt" FROM "apk_config" WHERE "id" = 1`).Scan(&version, &description, &downloadUrl, &updatedAt)
		if err != nil {
			http.Error(w, "Query failed: "+err.Error(), http.StatusInternalServerError)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"version":     version,
			"description": description,
			"downloadUrl": downloadUrl,
			"updatedAt":   updatedAt,
		})
		return
	}

	if r.Method == http.MethodPost {
		var req struct {
			Version     string `json:"version"`
			Description string `json:"description"`
			DownloadUrl string `json:"downloadUrl"`
		}
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, "Bad request", http.StatusBadRequest)
			return
		}

		if req.Version == "" {
			http.Error(w, "version is required", http.StatusBadRequest)
			return
		}

		// Otomatis potong deskripsi ke versi terbaru saja jika ada separator ---
		if strings.Contains(req.Description, "\n---\n") || strings.Contains(req.Description, "\n---") {
			req.Description = extractLatestReleaseNote(req.Description)
		}

		_, err := db.Exec(`UPDATE "apk_config" SET "version" = $1, "description" = $2, "downloadUrl" = $3, "updatedAt" = $4 WHERE "id" = 1`,
			req.Version, req.Description, req.DownloadUrl, time.Now().UnixNano()/int64(time.Millisecond))
		if err != nil {
			http.Error(w, "Update failed: "+err.Error(), http.StatusInternalServerError)
			return
		}

		// Broadcast update notification
		wsMsg := map[string]interface{}{
			"type":        "apk_update",
			"version":     req.Version,
			"description": req.Description,
		}
		msgBytes, _ := json.Marshal(wsMsg)
		broadcastWSMessage(string(msgBytes))

		// Send Gmail notification to all premium users and employees
		sendApkUpdateEmailToAllPremiumUsersAndEmployees(req.Version, req.Description)

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]bool{"success": true})
		return
	}
}

func compareVersions(v1, v2 string) int {
	v1 = strings.TrimSuffix(v1, "-debug")
	v2 = strings.TrimSuffix(v2, "-debug")
	parts1 := strings.Split(v1, ".")
	parts2 := strings.Split(v2, ".")
	for i := 0; i < len(parts1) && i < len(parts2); i++ {
		n1, _ := strconv.Atoi(parts1[i])
		n2, _ := strconv.Atoi(parts2[i])
		if n1 < n2 {
			return -1
		}
		if n1 > n2 {
			return 1
		}
	}
	if len(parts1) < len(parts2) {
		return -1
	}
	if len(parts1) > len(parts2) {
		return 1
	}
	return 0
}

// extractLatestReleaseNote mengambil hanya section versi terbaru dari release_notes.txt.
// Section dipisah oleh baris "---". Hanya section pertama (terbaru) yang dikembalikan.
// Ini mencegah deskripsi terlalu panjang di banner pembaruan Android.
func extractLatestReleaseNote(content string) string {
	content = strings.TrimSpace(content)
	// Hapus separator di awal jika ada
	content = strings.TrimPrefix(content, "---")
	content = strings.TrimSpace(content)
	// Ambil hanya section pertama (sebelum separator --- berikutnya)
	for _, sep := range []string{"\n---\n", "\n\n---\n", "\n---\r\n"} {
		if idx := strings.Index(content, sep); idx > 0 {
			content = content[:idx]
			break
		}
	}
	return strings.TrimSpace(content)
}

func autoDetectApkVersion() {
	if db == nil {
		return
	}

	files, err := filepath.Glob("./posbah-v*.apk")
	if err != nil || len(files) == 0 {
		return
	}

	var latestVersion string
	for _, f := range files {
		filename := filepath.Base(f)
		version := strings.TrimPrefix(filename, "posbah-v")
		version = strings.TrimSuffix(version, ".apk")
		version = strings.TrimSuffix(version, "-debug")
		if latestVersion == "" || compareVersions(version, latestVersion) > 0 {
			latestVersion = version
		}
	}

	if latestVersion == "" {
		return
	}

	var currentVersion string
	err = db.QueryRow(`SELECT "version" FROM "apk_config" WHERE "id" = 1`).Scan(&currentVersion)
	if err != nil {
		// Insert default if select failed
		_, _ = db.Exec(`INSERT INTO "apk_config" ("id", "version", "description", "downloadUrl", "updatedAt") VALUES (1, $1, $2, $3, $4) ON CONFLICT DO NOTHING`,
			latestVersion, "Versi baru terdeteksi otomatis.", "/api/download-apk", time.Now().UnixNano()/int64(time.Millisecond))
		return
	}

	if latestVersion != currentVersion {
		description := ""
		notesBytes, err := os.ReadFile("./release_notes.txt")
		if err == nil {
			// Hanya ambil section versi terbaru, bukan seluruh riwayat
			description = extractLatestReleaseNote(string(notesBytes))
		}
		if description == "" {
			description = fmt.Sprintf("Pembaruan ke versi %s terdeteksi otomatis. Silakan unduh pembaruan.", latestVersion)
		}

		_, err = db.Exec(`UPDATE "apk_config" SET "version" = $1, "description" = $2, "updatedAt" = $3 WHERE "id" = 1`,
			latestVersion, description, time.Now().UnixNano()/int64(time.Millisecond))
		if err == nil {
			log.Printf("[AutoUpdate] Detected and updated new APK version: %s", latestVersion)
			// Broadcast update notification
			wsMsg := map[string]interface{}{
				"type":        "apk_update",
				"version":     latestVersion,
				"description": description,
			}
			msgBytes, _ := json.Marshal(wsMsg)
			broadcastWSMessage(string(msgBytes))

			// Send Gmail notification to all premium users and employees
			sendApkUpdateEmailToAllPremiumUsersAndEmployees(latestVersion, description)
		}
	}
}

func sendApkUpdateEmailToAllPremiumUsersAndEmployees(version, description string) {
	if db == nil {
		log.Println("[ApkUpdateEmail] Database not initialized. Skipping email broadcast.")
		return
	}

	query := `
		SELECT DISTINCT "email" FROM "local_users" WHERE "isPremium" = TRUE AND "isActive" = TRUE AND "email" IS NOT NULL AND "email" != ''
		UNION
		SELECT DISTINCT e."email"
		FROM "employees" e
		JOIN "local_users" u ON e."tenantId" = u."tenantId"
		WHERE u."isPremium" = TRUE AND u."isActive" = TRUE AND e."email" IS NOT NULL AND e."email" != '' AND e."isActive" = TRUE
	`

	rows, err := db.Query(query)
	if err != nil {
		log.Printf("[ApkUpdateEmail] Failed to query premium emails: %v", err)
		return
	}
	defer rows.Close()

	var emails []string
	for rows.Next() {
		var email string
		if err := rows.Scan(&email); err == nil {
			email = strings.TrimSpace(strings.ToLower(email))
			if email != "" {
				emails = append(emails, email)
			}
		}
	}

	if len(emails) == 0 {
		log.Println("[ApkUpdateEmail] No premium users or employees found to email.")
		return
	}

	log.Printf("[ApkUpdateEmail] Broadcasting update to %d premium emails...", len(emails))

	subject := fmt.Sprintf("[POSBah] Informasi Pembaruan Aplikasi Versi %s", version)
	body := fmt.Sprintf(`
		<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid rgba(0, 0, 0, 0.08); border-radius: 16px; background-color: #FAFAFA;">
			<div style="text-align: center; margin-bottom: 25px; padding-bottom: 20px; border-bottom: 1px solid #EAEAEA;">
				<h2 style="color: #6366F1; margin: 0; font-size: 24px;">POSBah Rilis Pembaruan!</h2>
				<p style="color: #6B7280; margin: 5px 0 0 0; font-size: 14px;">Aplikasi Kasir Premium Anda Menjadi Lebih Baik</p>
			</div>
			
			<div style="margin-bottom: 25px;">
				<p style="font-size: 15px; color: #374151; line-height: 1.6; margin: 0 0 15px 0;">Halo Rekan POSBah Premium,</p>
				<p style="font-size: 15px; color: #374151; line-height: 1.6; margin: 0 0 15px 0;">
					Kami telah merilis pembaruan aplikasi <strong>POSBah Versi %s</strong> yang menyertakan berbagai perbaikan sistem dan peningkatan kinerja.
				</p>
				
				<div style="background-color: #EEF2F6; border-left: 4px solid #6366F1; padding: 15px; border-radius: 8px; margin: 20px 0;">
					<h4 style="margin: 0 0 10px 0; color: #1E293B; font-size: 14px; font-weight: 600;">Catatan Rilis (Release Notes):</h4>
					<p style="margin: 0; color: #4B5563; font-size: 13px; line-height: 1.6; white-space: pre-line;">%s</p>
				</div>
			</div>

			<div style="text-align: center; margin: 30px 0;">
				<a href="https://www.zedmz.cloud/api/download-apk" style="display: inline-block; background-color: #6366F1; color: white; text-decoration: none; padding: 14px 28px; border-radius: 10px; font-size: 15px; font-weight: 600;">Unduh APK Terbaru</a>
			</div>
			
			<div style="border-top: 1px solid #EAEAEA; padding-top: 20px; text-align: center; color: #9CA3AF; font-size: 11px;">
				<p style="margin: 0;">Email ini dikirimkan secara otomatis kepada pengguna akun premium dan karyawan outlet POSBah yang terdaftar.</p>
				<p style="margin: 5px 0 0 0;">&copy; 2026 POSBah. All rights reserved.</p>
			</div>
		</div>
	`, version, description)

	go func() {
		for _, email := range emails {
			err := sendEmail(email, subject, body)
			if err != nil {
				log.Printf("[ApkUpdateEmail] Failed to send email to %s: %v", email, err)
			} else {
				log.Printf("[ApkUpdateEmail] Email successfully sent to %s", email)
			}
			time.Sleep(100 * time.Millisecond)
		}
	}()
}

func handleAdminDiagnose(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	if db == nil {
		http.Error(w, "Database not initialized", http.StatusInternalServerError)
		return
	}

	targetEmail := r.URL.Query().Get("email")
	if targetEmail == "" {
		targetEmail = ""
	}

	syncDatabaseUsersAndTenants()

	// Count users
	var totalUsers, premiumUsers, activeDemo, blockedUsers int
	db.QueryRow(`SELECT count(*) FROM "local_users"`).Scan(&totalUsers)
	db.QueryRow(`SELECT count(*) FROM "local_users" WHERE "isPremium" = TRUE`).Scan(&premiumUsers)
	db.QueryRow(`SELECT count(*) FROM "local_users" WHERE "isPremium" = FALSE AND "isActive" = TRUE`).Scan(&activeDemo)
	db.QueryRow(`SELECT count(*) FROM "local_users" WHERE "isActive" = FALSE`).Scan(&blockedUsers)

	// Search targetEmail in local_users
	var luFound bool
	var luSub, luEmail, luName, luTenant string
	var luPremium, luActive bool
	err := db.QueryRow(`SELECT "googleSub", "email", COALESCE("displayName", ''), "tenantId", "isPremium", "isActive" FROM "local_users" WHERE "email" = $1`, targetEmail).Scan(&luSub, &luEmail, &luName, &luTenant, &luPremium, &luActive)
	if err == nil {
		luFound = true
	}

	// Search targetEmail in employees
	var empFound bool
	var empId int
	var empTenant, empName, empEmail, empRole string
	var empActive bool
	err = db.QueryRow(`SELECT "id", "tenantId", "name", "email", "role", "isActive" FROM "employees" WHERE "email" = $1`, targetEmail).Scan(&empId, &empTenant, &empName, &empEmail, &empRole, &empActive)
	if err == nil {
		empFound = true
	}

	// Get all table names in db
	rows, err := db.Query(`SELECT table_name FROM information_schema.tables WHERE table_schema='public'`)
	var tables []string
	if err == nil {
		for rows.Next() {
			var t string
			if err := rows.Scan(&t); err == nil {
				tables = append(tables, t)
			}
		}
		rows.Close()
	}

	// Dynamic email scan for targetEmail across all tables and columns containing 'email'
	type EmailCol struct {
		TableName  string
		ColumnName string
	}
	var emailCols []EmailCol
	rowsCols, err := db.Query(`
		SELECT table_name, column_name 
		FROM information_schema.columns 
		WHERE table_schema='public' AND column_name ILIKE '%email%'
	`)
	if err == nil {
		for rowsCols.Next() {
			var ec EmailCol
			if err := rowsCols.Scan(&ec.TableName, &ec.ColumnName); err == nil {
				emailCols = append(emailCols, ec)
			}
		}
		rowsCols.Close()
	}

	type MatchInfo struct {
		Table  string                 `json:"table"`
		Column string                 `json:"column"`
		Record map[string]interface{} `json:"record"`
	}
	var matches []MatchInfo
	for _, ec := range emailCols {
		queryStr := fmt.Sprintf(`SELECT row_to_json(t) FROM "%s" t WHERE t."%s" = $1 LIMIT 1`, ec.TableName, ec.ColumnName)
		var rowJSON string
		err := db.QueryRow(queryStr, targetEmail).Scan(&rowJSON)
		if err == nil {
			var parsed map[string]interface{}
			if err := json.Unmarshal([]byte(rowJSON), &parsed); err == nil {
				matches = append(matches, MatchInfo{
					Table:  ec.TableName,
					Column: ec.ColumnName,
					Record: parsed,
				})
			}
		}
	}

	tenantCounts := make(map[string]int)
	if luFound && luTenant != "" {
		tenantTables := []string{
			"outlets", "employees", "products", "customers", "transactions", 
			"bmp_invoices", "bmp_products", "bmp_master_products", "bmp_clients", 
			"bmp_cashflow", "bmp_payrolls", "bmp_bahan_baku", "print_settings", 
			"activity_logs",
		}
		for _, tbl := range tenantTables {
			var count int
			queryStr := fmt.Sprintf(`SELECT count(*) FROM "%s" WHERE "tenantId" = $1`, tbl)
			err := db.QueryRow(queryStr, luTenant).Scan(&count)
			if err == nil {
				tenantCounts[tbl] = count
			}
		}
	}

	premiumEmailsRows, err := db.Query(`
		SELECT DISTINCT "email" FROM "local_users" WHERE "isPremium" = TRUE AND "isActive" = TRUE AND "email" IS NOT NULL AND "email" != ''
		UNION
		SELECT DISTINCT e."email"
		FROM "employees" e
		JOIN "local_users" u ON e."tenantId" = u."tenantId"
		WHERE u."isPremium" = TRUE AND u."isActive" = TRUE AND e."email" IS NOT NULL AND e."email" != '' AND e."isActive" = TRUE
	`)
	var premiumEmails []string
	if err == nil {
		for premiumEmailsRows.Next() {
			var pe string
			if err := premiumEmailsRows.Scan(&pe); err == nil {
				pe = strings.TrimSpace(strings.ToLower(pe))
				if pe != "" {
					premiumEmails = append(premiumEmails, pe)
				}
			}
		}
		premiumEmailsRows.Close()
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"premium_emails":    premiumEmails,
		"tenant_row_counts": tenantCounts,
		"stats": map[string]int{
			"total":   totalUsers,
			"premium": premiumUsers,
			"demo":    activeDemo,
			"blocked": blockedUsers,
		},
		"tables": tables,
		"target_email": targetEmail,
		"email_local_user": map[string]interface{}{
			"found":       luFound,
			"googleSub":   luSub,
			"email":       luEmail,
			"displayName": luName,
			"tenantId":    luTenant,
			"isPremium":   luPremium,
			"isActive":    luActive,
		},
		"email_employee": map[string]interface{}{
			"found":     empFound,
			"id":        empId,
			"tenantId":  empTenant,
			"name":      empName,
			"email":     empEmail,
			"role":      empRole,
			"isActive":  empActive,
		},
		"email_matches": matches,
		"smtp_config": map[string]interface{}{
			"host_configured":   os.Getenv("SMTP_HOST") != "",
			"port_configured":   os.Getenv("SMTP_PORT") != "",
			"user_configured":   os.Getenv("SMTP_USER") != "",
			"pass_configured":   os.Getenv("SMTP_PASS") != "",
			"sender_configured": os.Getenv("SMTP_SENDER") != "",
			"host":              os.Getenv("SMTP_HOST"),
			"port":              os.Getenv("SMTP_PORT"),
			"user":              os.Getenv("SMTP_USER"),
			"sender":            os.Getenv("SMTP_SENDER"),
		},
	})
}

func syncDatabaseUsersAndTenants() {
	if db == nil {
		return
	}

	// 1. Backfill missing local_users from tenants
	_, err := db.Exec(`
		INSERT INTO "local_users" ("googleSub", "email", "displayName", "role", "tenantId", "isPremium", "isActive", "registeredAt", "updatedAt")
		SELECT 
			t."ownerEmail" AS "googleSub", 
			t."ownerEmail" AS "email", 
			regexp_replace(regexp_replace(t."name", '^Demo - ', ''), ' \(Premium\)$', '') AS "displayName",
			'OWNER' AS "role",
			t."id" AS "tenantId",
			CASE WHEN t."id" LIKE 'ten_premium_%' THEN TRUE ELSE FALSE END AS "isPremium",
			t."isActive" AS "isActive",
			t."createdAt" AS "registeredAt",
			t."updatedAt" AS "updatedAt"
		FROM "tenants" t
		WHERE NOT EXISTS (
			SELECT 1 FROM "local_users" u WHERE u."tenantId" = t."id" OR u."email" = t."ownerEmail"
		) AND t."ownerEmail" IS NOT NULL AND t."ownerEmail" <> ''
		ON CONFLICT DO NOTHING
	`)
	if err != nil {
		log.Printf("Sync error: failed to backfill local_users: %v", err)
	}

	// 2. Backfill missing tenants from local_users (only when tenantId is not null and not empty)
	_, err = db.Exec(`
		INSERT INTO "tenants" ("id", "name", "ownerEmail", "businessMode", "isActive", "createdAt", "updatedAt")
		SELECT 
			u."tenantId" AS "id",
			CASE WHEN u."isPremium" = TRUE THEN 'CV. ' || COALESCE(NULLIF(u."displayName", ''), u."email") || ' (Premium)'
				 ELSE 'Demo - ' || COALESCE(NULLIF(u."displayName", ''), u."email")
			END AS "name",
			u."email" AS "ownerEmail",
			'FNB' AS "businessMode",
			u."isActive" AS "isActive",
			u."registeredAt" AS "createdAt",
			u."updatedAt" AS "updatedAt"
		FROM "local_users" u
		WHERE NOT EXISTS (
			SELECT 1 FROM "tenants" t WHERE t."id" = u."tenantId" OR t."ownerEmail" = u."email"
		) AND u."email" IS NOT NULL AND u."email" <> '' AND u."tenantId" IS NOT NULL AND u."tenantId" <> ''
		ON CONFLICT ("id") DO NOTHING
	`)
	if err != nil {
		log.Printf("Sync error: failed to backfill tenants: %v", err)
	}

	// 3. Keep isActive synchronized from tenants to local_users (e.g. if updated via tenant sync)
	_, err = db.Exec(`
		UPDATE "local_users" u
		SET "isActive" = t."isActive", "updatedAt" = $1
		FROM "tenants" t
		WHERE (u."tenantId" = t."id" OR u."email" = t."ownerEmail") AND u."isActive" <> t."isActive"
	`, time.Now().UnixNano()/int64(time.Millisecond))
	if err != nil {
		log.Printf("Sync error: failed to sync isActive tenants->local_users: %v", err)
	}

	// 4. Keep isActive synchronized from local_users to tenants (e.g. if blocked from admin panel)
	_, err = db.Exec(`
		UPDATE "tenants" t
		SET "isActive" = u."isActive", "updatedAt" = $1
		FROM "local_users" u
		WHERE (u."tenantId" = t."id" OR u."email" = t."ownerEmail") AND t."isActive" <> u."isActive"
	`, time.Now().UnixNano()/int64(time.Millisecond))
	if err != nil {
		log.Printf("Sync error: failed to sync isActive local_users->tenants: %v", err)
	}

	// 5. Backfill missing local_users from employees
	_, err = db.Exec(`
		INSERT INTO "local_users" ("googleSub", "email", "displayName", "role", "tenantId", "isPremium", "isActive", "registeredAt", "updatedAt")
		SELECT DISTINCT ON (LOWER(e."email"))
			'emp_' || e."id" AS "googleSub", 
			e."email" AS "email", 
			e."name" AS "displayName",
			e."role" AS "role",
			e."tenantId" AS "tenantId",
			COALESCE((SELECT t."id" LIKE 'ten_premium_%' FROM "tenants" t WHERE t."id" = e."tenantId" LIMIT 1), FALSE) AS "isPremium",
			e."isActive" AS "isActive",
			e."createdAt" AS "registeredAt",
			e."updatedAt" AS "updatedAt"
		FROM "employees" e
		WHERE NOT EXISTS (
			SELECT 1 FROM "local_users" u WHERE LOWER(u."email") = LOWER(e."email")
		) AND e."email" IS NOT NULL AND e."email" <> ''
		ORDER BY LOWER(e."email"), e."id"
		ON CONFLICT DO NOTHING
	`)
	if err != nil {
		log.Printf("Sync error: failed to backfill local_users from employees: %v", err)
	}

	// 6. Fix isPremium flag: any user whose tenantId starts with 'ten_premium_' MUST be isPremium=TRUE
	_, err = db.Exec(`
		UPDATE "local_users"
		SET "isPremium" = TRUE, "updatedAt" = $1
		WHERE "tenantId" LIKE 'ten_premium_%' AND "isPremium" = FALSE
	`, time.Now().UnixNano()/int64(time.Millisecond))
	if err != nil {
		log.Printf("Sync error: failed to fix isPremium for premium tenants: %v", err)
	}

	// 7. Protect active premium users: if a premium user is in deleted_users with a blocking status,
	// update them to ACTIVE so they are never purged or blocked.
	_, err = db.Exec(`
		UPDATE "deleted_users" du
		SET "status" = 'ACTIVE', "updatedAt" = $1
		FROM "local_users" u
		WHERE TRIM(LOWER(du."email")) = TRIM(LOWER(u."email"))
		  AND u."isPremium" = TRUE
		  AND du."status" NOT IN ('ACTIVE')
	`, time.Now().UnixNano()/int64(time.Millisecond))
	if err != nil {
		log.Printf("Sync error: failed to protect premium users in deleted_users: %v", err)
	}

	// 8. Ensure all premium users have isActive=TRUE (they paid — must never be blocked by demo cron)
	_, err = db.Exec(`
		UPDATE "local_users"
		SET "isActive" = TRUE, "updatedAt" = $1
		WHERE "isPremium" = TRUE AND "isActive" = FALSE
	`, time.Now().UnixNano()/int64(time.Millisecond))
	if err != nil {
		log.Printf("Sync error: failed to re-activate blocked premium users: %v", err)
	} else {
		log.Printf("[Sync] Premium user protection checks completed.")
	}

	// Trigger automated interconnect and consistency checks
	runDatabaseInterconnectSyncAutomation()
}

func handleEmployeeConfirm(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	email := r.URL.Query().Get("email")
	tenantID := r.URL.Query().Get("tenantId")
	idStr := r.URL.Query().Get("id")

	if email == "" || tenantID == "" || idStr == "" {
		http.Error(w, "Missing query parameters", http.StatusBadRequest)
		return
	}

	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		http.Error(w, "Invalid ID parameter", http.StatusBadRequest)
		return
	}

	// 1. Update the emailVerified = true in database
	res, err := db.Exec(`UPDATE "employees" SET "emailVerified" = TRUE WHERE "id" = $1 AND "tenantId" = $2 AND "email" = $3`, id, tenantID, email)
	if err != nil {
		http.Error(w, "Failed to confirm email: "+err.Error(), http.StatusInternalServerError)
		return
	}
	rowsAffected, _ := res.RowsAffected()
	if rowsAffected == 0 {
		http.Error(w, "Employee not found or already verified", http.StatusNotFound)
		return
	}

	// 2. Query business name (tenants)
	var businessName string
	err = db.QueryRow(`SELECT "name" FROM "tenants" WHERE "id" = $1`, tenantID).Scan(&businessName)
	if err != nil {
		businessName = tenantID
	}

	// 3. Send second confirmation email
	// "maka email : muhammadmuizz8@gmail.com akan mengirimkan sebuah informasi bahwa email outlet karyawan telah resmi menjadi outlet karyawan di [namabisnis] dan email outlet karyawan tidak bisa diganti permanen"
	subject := "Konfirmasi Akun Karyawan Resmi - POSBah"
	body := fmt.Sprintf(`
	<div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e5e7eb; border-radius: 8px;">
		<h2 style="color: #10b981;">Konfirmasi Berhasil</h2>
		<p>Email <strong>%s</strong> telah resmi menjadi outlet karyawan di <strong>%s</strong>.</p>
		<p>Aturan Sistem: Email outlet karyawan tidak bisa diganti secara permanen.</p>
		<p style="font-size: 12px; color: #6b7280; border-top: 1px solid #e5e7eb; padding-top: 15px; margin-top: 30px;">
			pesan ini otomatis dari sistem POSBah
		</p>
	</div>
	`, email, businessName)

	go func(to, sub, b string) {
		if err := sendEmail(to, sub, b); err != nil {
			log.Printf("Gagal mengirim email konfirmasi resmi: %v", err)
		}
	}(email, subject, body)

	// 4. Return beautiful HTML page
	w.Header().Set("Content-Type", "text/html; charset=UTF-8")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(fmt.Sprintf(`
	<!DOCTYPE html>
	<html>
	<head>
		<meta charset="utf-8">
		<title>Konfirmasi Berhasil</title>
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<style>
			body {
				font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
				background: linear-gradient(135deg, #0f172a 0%%, #1e1b4b 100%%);
				color: #f8fafc;
				display: flex;
				justify-content: center;
				align-items: center;
				height: 100vh;
				margin: 0;
			}
			.card {
				background: rgba(30, 41, 59, 0.7);
				backdrop-filter: blur(12px);
				border: 1px solid rgba(255, 255, 255, 0.1);
				padding: 40px;
				border-radius: 16px;
				text-align: center;
				box-shadow: 0 10px 25px rgba(0,0,0,0.3);
				max-width: 400px;
				width: 100%%;
			}
			.icon {
				font-size: 48px;
				color: #10b981;
				margin-bottom: 20px;
			}
			h1 {
				margin-top: 0;
				font-size: 24px;
				font-weight: 700;
			}
			p {
				color: #94a3b8;
				font-size: 16px;
				line-height: 1.5;
			}
			.footer {
				margin-top: 30px;
				font-size: 12px;
				color: #64748b;
			}
		</style>
	</head>
	<body>
		<div class="card">
			<div class="icon">✓</div>
			<h1>Konfirmasi Sukses!</h1>
			<p>Email <strong>%s</strong> telah resmi dikonfirmasi menjadi outlet karyawan di <strong>%s</strong>.</p>
			<p>Anda sekarang dapat login ke aplikasi POSBah.</p>
			<div class="footer">pesan ini otomatis dari sistem POSBah</div>
		</div>
	</body>
	</html>
	`, email, businessName)))
}

func handleCheckDeleted(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	email := strings.TrimSpace(strings.ToLower(r.URL.Query().Get("email")))
	if email == "" {
		http.Error(w, "Missing email parameter", http.StatusBadRequest)
		return
	}

	var status string
	err := db.QueryRow(`SELECT "status" FROM "deleted_users" WHERE TRIM(LOWER("email")) = $1`, email).Scan(&status)
	w.Header().Set("Content-Type", "application/json")
	if err == sql.ErrNoRows {
		w.Write([]byte(`{"deleted":false}`))
		return
	} else if err != nil {
		http.Error(w, "Database error: "+err.Error(), http.StatusInternalServerError)
		return
	}

	if status == "ACTIVE" {
		w.Write([]byte(`{"deleted":false}`))
	} else {
		w.Write([]byte(fmt.Sprintf(`{"deleted":true,"status":"%s"}`, status)))
	}
}

func handleRequestRejoin(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	email := strings.TrimSpace(strings.ToLower(r.URL.Query().Get("email")))
	if email == "" {
		http.Error(w, "Missing email parameter", http.StatusBadRequest)
		return
	}

	var status string
	err := db.QueryRow(`SELECT "status" FROM "deleted_users" WHERE TRIM(LOWER("email")) = $1`, email).Scan(&status)
	if err == sql.ErrNoRows {
		_, _ = db.Exec(`INSERT INTO "deleted_users" ("email", "status", "updatedAt") VALUES ($1, 'DELETED', $2)`, email, time.Now().UnixNano()/1e6)
		status = "DELETED"
	} else if err != nil {
		http.Error(w, "Database error: "+err.Error(), http.StatusInternalServerError)
		return
	}

	_, err = db.Exec(`UPDATE "deleted_users" SET "status" = 'PENDING_USER_CONFIRM', "updatedAt" = $1 WHERE TRIM(LOWER("email")) = $2`, time.Now().UnixNano()/1e6, email)
	if err != nil {
		http.Error(w, "Database error: "+err.Error(), http.StatusInternalServerError)
		return
	}

	subject := "Apakah kamu ingin mencoba menggunakan POSBah lagi ?"
	body := fmt.Sprintf(`
	<!DOCTYPE html>
	<html>
	<head>
		<meta charset="utf-8">
		<style>
			body { font-family: sans-serif; background-color: #f8fafc; color: #1e293b; padding: 30px; margin: 0; }
			.card { background: white; border: 1px solid #e2e8f0; border-radius: 12px; padding: 32px; max-width: 480px; margin: 0 auto; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
			h2 { color: #f97316; margin-top: 0; }
			p { font-size: 16px; line-height: 1.6; color: #475569; }
			.btn { display: inline-block; background-color: #f97316; color: white !important; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px; margin-top: 20px; text-align: center; }
			.footer { font-size: 12px; color: #94a3b8; margin-top: 30px; border-top: 1px solid #e2e8f0; padding-top: 15px; }
		</style>
	</head>
	<body>
		<div class="card">
			<h2>Uji Coba POSBah</h2>
			<p>Apakah kamu ingin mencoba menggunakan POSBah lagi?</p>
			<a class="btn" href="https://www.zedmz.cloud/api/auth/confirm-rejoin?email=%s">Ya, Saya Ingin Mencoba Lagi</a>
			<div class="footer">Pesan ini dikirim secara otomatis oleh sistem POSBah.</div>
		</div>
	</body>
	</html>
	`, email)

	if err := sendEmail(email, subject, body); err != nil {
		http.Error(w, "Failed to send email: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write([]byte(`{"success":true}`))
}

func handleConfirmRejoin(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	email := strings.TrimSpace(strings.ToLower(r.URL.Query().Get("email")))
	if email == "" {
		http.Error(w, "Missing email parameter", http.StatusBadRequest)
		return
	}

	_, err := db.Exec(`UPDATE "deleted_users" SET "status" = 'PENDING_ADMIN_APPROVE', "updatedAt" = $1 WHERE TRIM(LOWER("email")) = $2`, time.Now().UnixNano()/1e6, email)
	if err != nil {
		http.Error(w, "Database error: "+err.Error(), http.StatusInternalServerError)
		return
	}

	adminEmail := "muhammadmuizz8@gmail.com"
	subject := "Persetujuan Rejoin POSBah"
	body := fmt.Sprintf(`
	<!DOCTYPE html>
	<html>
	<head>
		<meta charset="utf-8">
		<style>
			body { font-family: sans-serif; background-color: #f8fafc; color: #1e293b; padding: 30px; margin: 0; }
			.card { background: white; border: 1px solid #e2e8f0; border-radius: 12px; padding: 32px; max-width: 480px; margin: 0 auto; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
			h2 { color: #f97316; margin-top: 0; }
			p { font-size: 16px; line-height: 1.6; color: #475569; }
			.btn { display: inline-block; background-color: #10b981; color: white !important; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px; margin-top: 20px; text-align: center; }
			.footer { font-size: 12px; color: #94a3b8; margin-top: 30px; border-top: 1px solid #e2e8f0; padding-top: 15px; }
		</style>
	</head>
	<body>
		<div class="card">
			<h2>Persetujuan Rejoin POSBah</h2>
			<p>Apakah kamu menyetujui bahwa email : <strong>%s</strong> untuk login kembali?</p>
			<a class="btn" href="https://www.zedmz.cloud/api/auth/approve-rejoin?email=%s">Iya</a>
			<div class="footer">Pesan ini dikirim secara otomatis oleh sistem POSBah.</div>
		</div>
	</body>
	</html>
	`, email, email)

	if err := sendEmail(adminEmail, subject, body); err != nil {
		http.Error(w, "Failed to send email to admin: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Write([]byte(fmt.Sprintf(`
	<!DOCTYPE html>
	<html>
	<head>
		<meta charset="utf-8">
		<title>Permintaan Terkirim</title>
		<style>
			body { font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #0f172a; color: white; }
			.card { background: rgba(30, 41, 59, 0.7); backdrop-filter: blur(12px); border: 1px solid rgba(255, 255, 255, 0.1); padding: 40px; border-radius: 16px; text-align: center; box-shadow: 0 10px 25px rgba(0,0,0,0.3); max-width: 400px; width: 100%%; }
			.icon { font-size: 48px; color: #f97316; margin-bottom: 20px; }
			h1 { font-size: 24px; font-weight: 700; margin-top: 0; }
			p { color: #94a3b8; font-size: 16px; line-height: 1.5; }
			.footer { margin-top: 30px; font-size: 12px; color: #64748b; }
		</style>
	</head>
	<body>
		<div class="card">
			<div class="icon">✓</div>
			<h1>Permintaan Terkirim</h1>
			<p>Terima kasih. Permintaan rejoin untuk email <strong>%s</strong> telah diteruskan kepada Admin untuk disetujui.</p>
			<div class="footer">pesan ini otomatis dari sistem POSBah</div>
		</div>
	</body>
	</html>
	`, email)))
}

func handleApproveRejoin(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	email := strings.TrimSpace(strings.ToLower(r.URL.Query().Get("email")))
	if email == "" {
		http.Error(w, "Missing email parameter", http.StatusBadRequest)
		return
	}

	_, err := db.Exec(`UPDATE "deleted_users" SET "status" = 'REJOINED', "updatedAt" = $1 WHERE TRIM(LOWER("email")) = $2`, time.Now().UnixNano()/1e6, email)
	if err != nil {
		http.Error(w, "Database error: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Automatically pre-register the user in local_users on approve-rejoin
	// so they immediately appear in the Web Admin Panel.
	cleanEmail := strings.TrimSpace(strings.ToLower(email))
	defaultDisplayName := strings.Split(cleanEmail, "@")[0]
	if len(defaultDisplayName) > 0 {
		defaultDisplayName = strings.ToUpper(defaultDisplayName[0:1]) + defaultDisplayName[1:]
	}

	// Safely insert or update local user without multiple ON CONFLICT syntax error in PostgreSQL
	var exists bool
	err = db.QueryRow(`SELECT EXISTS(SELECT 1 FROM "local_users" WHERE TRIM(LOWER("email")) = $1 OR "googleSub" = $1)`, cleanEmail).Scan(&exists)
	if err == nil && exists {
		_, err = db.Exec(`UPDATE "local_users" SET "isActive" = TRUE, "isPremium" = FALSE, "updatedAt" = $1 WHERE TRIM(LOWER("email")) = $2 OR "googleSub" = $2`, time.Now().UnixNano()/1e6, cleanEmail)
	} else {
		_, err = db.Exec(`
			INSERT INTO "local_users" ("googleSub", "email", "displayName", "role", "tenantId", "isPremium", "isActive", "registeredAt", "updatedAt")
			VALUES ($1, $1, $2, 'OWNER', NULL, FALSE, TRUE, $3, $3)
			ON CONFLICT ("email") DO UPDATE 
			SET "isActive" = TRUE, "isPremium" = FALSE, "updatedAt" = EXCLUDED."updatedAt"`,
			cleanEmail, defaultDisplayName, time.Now().UnixNano()/1e6)
	}
	if err != nil {
		log.Printf("[RejoinAuto] Failed to insert/update local_user: %v", err)
	}

	// Trigger sync count calculation to update stats counters
	syncDatabaseUsersAndTenants()

	// Broadcast WebSocket message to refresh stats on admin web panel immediately
	wsMsg := map[string]interface{}{
		"type":      "user_registered",
		"googleSub": cleanEmail,
		"email":     cleanEmail,
		"status":    "demo",
	}
	if msgBytes, err := json.Marshal(wsMsg); err == nil {
		broadcastWSMessage(string(msgBytes))
	}

	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Write([]byte(fmt.Sprintf(`
	<!DOCTYPE html>
	<html>
	<head>
		<meta charset="utf-8">
		<title>Persetujuan Berhasil</title>
		<style>
			body { font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #0f172a; color: white; }
			.card { background: rgba(30, 41, 59, 0.7); backdrop-filter: blur(12px); border: 1px solid rgba(255, 255, 255, 0.1); padding: 40px; border-radius: 16px; text-align: center; box-shadow: 0 10px 25px rgba(0,0,0,0.3); max-width: 400px; width: 100%%; }
			.icon { font-size: 48px; color: #10b981; margin-bottom: 20px; }
			h1 { font-size: 24px; font-weight: 700; margin-top: 0; }
			p { color: #94a3b8; font-size: 16px; line-height: 1.5; }
			.footer { margin-top: 30px; font-size: 12px; color: #64748b; }
		</style>
	</head>
	<body>
		<div class="card">
			<div class="icon">✓</div>
			<h1>Persetujuan Berhasil</h1>
			<p>Email <strong>%s</strong> telah disetujui untuk login kembali ke POSBah dengan data 0.</p>
			<div class="footer">pesan ini otomatis dari sistem POSBah</div>
		</div>
	</body>
	</html>
	`, email)))
}

func handleCompleteRejoin(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	email := strings.TrimSpace(strings.ToLower(r.URL.Query().Get("email")))
	if email == "" {
		http.Error(w, "Missing email parameter", http.StatusBadRequest)
		return
	}

	_, err := db.Exec(`UPDATE "deleted_users" SET "status" = 'ACTIVE', "updatedAt" = $1 WHERE TRIM(LOWER("email")) = $2`, time.Now().UnixNano()/1e6, email)
	if err != nil {
		http.Error(w, "Database error: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write([]byte(`{"success":true}`))
}

// runDatabaseInterconnectSyncAutomation checks database states for consistency and cleans up orphan tenants periodically.
func runDatabaseInterconnectSyncAutomation() {
	if db == nil {
		return
	}
	log.Println("[Automation] Running database interconnect and synchronization check...")

	// 1. Synchronize deleted_users and local_users
	// If user is in deleted_users with status NOT IN ('REJOINED', 'ACTIVE'), make sure they are inactive
	rows, err := db.Query(`SELECT "email", "status" FROM "deleted_users"`)
	if err == nil {
		defer rows.Close()
		for rows.Next() {
			var email, status string
			if err := rows.Scan(&email, &status); err == nil {
				cleanEmail := strings.TrimSpace(strings.ToLower(email))
				if status == "DELETED" || status == "PENDING_USER_CONFIRM" || status == "PENDING_ADMIN_APPROVE" {
					// Ensure they are blocked/inactive in local_users
					_, err := db.Exec(`UPDATE "local_users" SET "isActive" = FALSE, "updatedAt" = $1 WHERE TRIM(LOWER("email")) = $2`, time.Now().UnixNano()/1e6, cleanEmail)
					if err != nil {
						log.Printf("[Automation] Failed to set inactive for %s: %v", cleanEmail, err)
					}
				} else if status == "REJOINED" || status == "ACTIVE" {
					// Ensure they are active in local_users
					_, err := db.Exec(`UPDATE "local_users" SET "isActive" = TRUE, "updatedAt" = $1 WHERE TRIM(LOWER("email")) = $2`, time.Now().UnixNano()/1e6, cleanEmail)
					if err != nil {
						log.Printf("[Automation] Failed to set active for %s: %v", cleanEmail, err)
					}
				}
			}
		}
	}

	// 2. Clean up duplicate/orphan temporary tenants
	// If a user has a specific tenant ID (e.g. ending in _FNB, _RENTAL, _LAUNDRY, _BMP) in local_users,
	// and there is also an orphan tenant (ID = demo_tenant_cleanEmail) with no local_users referencing it,
	// we should purge the orphan tenant.
	rowsLU, err := db.Query(`SELECT "email", "tenantId" FROM "local_users" WHERE "tenantId" IS NOT NULL AND "tenantId" <> ''`)
	if err == nil {
		defer rowsLU.Close()
		for rowsLU.Next() {
			var email, tenantId string
			if err := rowsLU.Scan(&email, &tenantId); err == nil {
				cleanEmail := strings.TrimSpace(strings.ToLower(email))
				emailKey := strings.ReplaceAll(strings.ReplaceAll(cleanEmail, ".", "_"), "@", "_")
				fakeTenantId := "demo_tenant_" + emailKey
				// If the actual tenantId is different from fakeTenantId but contains emailKey (e.g., FNB, BMP etc.)
				if tenantId != fakeTenantId && strings.Contains(tenantId, emailKey) {
					// Check if there is an orphan tenant with fakeTenantId
					var count int
					db.QueryRow(`SELECT count(*) FROM "tenants" WHERE "id" = $1`, fakeTenantId).Scan(&count)
					if count > 0 {
						// Check if any other user is using fakeTenantId
						var refCount int
						db.QueryRow(`SELECT count(*) FROM "local_users" WHERE "tenantId" = $1`, fakeTenantId).Scan(&refCount)
						if refCount == 0 {
							log.Printf("[Automation] Purging orphan temporary tenant: %s", fakeTenantId)
							purgeTenantData(fakeTenantId)
						}
					}
				}
			}
		}
	}
}

// ─────────────────────────────────────────────────────────────────────────────
// GET /api/reports/outlet-margin?tenantId=xxx&days=7
// Mengembalikan margin keuntungan per outlet per hari (Pendekatan A).
// Response: [ { outletId, outletName, date, revenue, cost, margin } ]
// ─────────────────────────────────────────────────────────────────────────────
func handleOutletMarginReport(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Access-Control-Allow-Origin", "*")

	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	tenantId := strings.TrimSpace(r.URL.Query().Get("tenantId"))
	if tenantId == "" {
		http.Error(w, `{"error":"tenantId is required"}`, http.StatusBadRequest)
		return
	}

	daysStr := r.URL.Query().Get("days")
	days := 7
	if d, err := strconv.Atoi(daysStr); err == nil && d > 0 && d <= 90 {
		days = d
	}

	if db == nil {
		http.Error(w, `{"error":"database not available"}`, http.StatusServiceUnavailable)
		return
	}

	// Hitung batas waktu
	nowMs := time.Now().UnixNano() / int64(time.Millisecond)
	fromMs := nowMs - int64(days)*24*60*60*1000

	// Query margin per outlet per hari dari tabel transactions + transaction_items
	query := `
		SELECT
			o."id"          AS outlet_id,
			o."name"        AS outlet_name,
			TO_CHAR(TO_TIMESTAMP(t."date" / 1000), 'YYYY-MM-DD') AS day_str,
			COALESCE(SUM(t."total"), 0)  AS revenue,
			COALESCE(SUM(
				(SELECT COALESCE(SUM(ti."costPrice" * ti."quantity"), 0)
				 FROM "transaction_items" ti
				 WHERE ti."transactionId" = t."id")
			), 0) AS cost
		FROM "outlets" o
		LEFT JOIN "transactions" t
			ON t."outletId" = o."id"
			AND t."tenantId" = $1
			AND t."status" = 'COMPLETED'
			AND t."date" >= $2
		WHERE o."tenantId" = $1
		GROUP BY o."id", o."name", day_str
		ORDER BY o."id", day_str;
	`

	rows, err := db.Query(query, tenantId, fromMs)
	if err != nil {
		log.Printf("[OutletMargin] query error: %v", err)
		http.Error(w, `{"error":"query failed"}`, http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	type OutletMarginRow struct {
		OutletId   int64   `json:"outletId"`
		OutletName string  `json:"outletName"`
		DayStr     string  `json:"date"`
		Revenue    float64 `json:"revenue"`
		Cost       float64 `json:"cost"`
		Margin     float64 `json:"margin"`
	}

	var result []OutletMarginRow
	for rows.Next() {
		var row OutletMarginRow
		var dayStr *string
		if err := rows.Scan(&row.OutletId, &row.OutletName, &dayStr, &row.Revenue, &row.Cost); err != nil {
			continue
		}
		if dayStr != nil {
			row.DayStr = *dayStr
		} else {
			row.DayStr = ""
		}
		row.Margin = row.Revenue - row.Cost
		result = append(result, row)
	}

	if result == nil {
		result = []OutletMarginRow{}
	}

	json.NewEncoder(w).Encode(result)
}

func parseQueryParamValue(tableName, colName, rawVal string) interface{} {
	colLower := strings.ToLower(colName)
	tableLower := strings.ToLower(tableName)

	if colLower == "tenantid" {
		return rawVal
	}
	if tableLower == "tenants" && colLower == "id" {
		return rawVal
	}

	if colLower == "id" || strings.HasSuffix(colLower, "id") {
		if valInt, err := strconv.ParseInt(rawVal, 10, 64); err == nil {
			return valInt
		}
	}

	return rawVal
}


