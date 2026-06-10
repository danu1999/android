package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

type LocalUser struct {
	GoogleSub    string `json:"googleSub"`
	Email        string `json:"email"`
	RegisteredAt int64  `json:"registeredAt"`
	IsActive     bool   `json:"isActive"`
}

var (
	supabaseURL       string
	supabaseSecretKey string
	port              string
)

func main() {
	// Load environment variables
	supabaseURL = os.Getenv("SUPABASE_URL")
	if supabaseURL == "" {
		supabaseURL = "https://etustetneufkfilndimy.supabase.co"
	}
	supabaseSecretKey = os.Getenv("SUPABASE_SECRET_KEY")
	if supabaseSecretKey == "" {
		supabaseSecretKey = "YOUR_SUPABASE_SECRET_KEY"
	}
	port = os.Getenv("PORT")
	if port == "" {
		port = "3000"
	}

	log.Printf("Starting PosBah Go backend...")
	log.Printf("Supabase URL: %s", supabaseURL)

	// Start background cron worker (runs check every hour)
	go startCronWorker()

	// Setup endpoints
	http.HandleFunc("/status", handleStatus)
	http.HandleFunc("/api/admin/check-demo-lockout", handleManualLockoutCheck)
	http.HandleFunc("/api/invoice/signature", handleSaveSignature)
	http.HandleFunc("/sign/", handleSignPage)
	http.HandleFunc("/api/sign/", handleSignPage)
	http.HandleFunc("/api/ai/classify", handleAiClassify)
	
	// Serve static files from TTD
	http.Handle("/api/signatures/", http.StripPrefix("/api/signatures/", http.FileServer(http.Dir("./TTD"))))

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

	for range ticker.C {
		log.Println("Triggering scheduled demo lockout check...")
		if err := checkAndLockoutDemoUsers(); err != nil {
			log.Printf("Scheduled demo lockout check failed: %v", err)
		}
	}
}

// Performs core logic of querying and deleting expired demo users
func checkAndLockoutDemoUsers() error {
	twoDaysAgoMillis := time.Now().UnixNano()/int64(time.Millisecond) - (2 * 24 * 60 * 60 * 1000)

	// Query Supabase for demo users who registered more than 2 days ago
	reqUrl := fmt.Sprintf("%s/rest/v1/local_users?isPremium=eq.false&registeredAt=lt.%d", supabaseURL, twoDaysAgoMillis)
	req, err := http.NewRequest("GET", reqUrl, nil)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("apikey", supabaseSecretKey)
	req.Header.Set("Authorization", "Bearer "+supabaseSecretKey)
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{Timeout: 15 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("http request failed: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		bodyBytes, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("unexpected status %s: %s", resp.Status, string(bodyBytes))
	}

	var users []LocalUser
	if err := json.NewDecoder(resp.Body).Decode(&users); err != nil {
		return fmt.Errorf("failed to decode response: %w", err)
	}

	if len(users) == 0 {
		log.Println("No expired demo users found.")
		return nil
	}

	log.Printf("Found %d expired demo users. Deletion in progress...", len(users))

	for _, user := range users {
		if err := deleteLocalUser(user.GoogleSub); err != nil {
			log.Printf("Failed to delete user %s: %v", user.Email, err)
		} else {
			regTime := time.Unix(user.RegisteredAt/1000, 0).Format("2006-01-02 15:04:05")
			log.Printf("Successfully deleted demo user: %s (Registered at: %s)", user.Email, regTime)
		}
	}

	return nil
}

// Deletes a user from Supabase using their GoogleSub
func deleteLocalUser(googleSub string) error {
	reqUrl := fmt.Sprintf("%s/rest/v1/local_users?googleSub=eq.%s", supabaseURL, googleSub)
	req, err := http.NewRequest("DELETE", reqUrl, nil)
	if err != nil {
		return err
	}

	req.Header.Set("apikey", supabaseSecretKey)
	req.Header.Set("Authorization", "Bearer "+supabaseSecretKey)

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		bodyBytes, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("unexpected status %s: %s", resp.Status, string(bodyBytes))
	}

	return nil
}

// Handler: GET /status
func handleStatus(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	response := map[string]string{
		"status":    "running",
		"timestamp": time.Now().UTC().Format(time.RFC3339),
		"database":  "connected to Supabase",
		"language":  "Go 1.21",
	}
	json.NewEncoder(w).Encode(response)
}

// Handler: POST /api/admin/check-demo-lockout
func handleManualLockoutCheck(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	twoDaysAgoMillis := time.Now().UnixNano()/int64(time.Millisecond) - (2 * 24 * 60 * 60 * 1000)

	// Fetch affected users first to report them
	reqUrl := fmt.Sprintf("%s/rest/v1/local_users?isPremium=eq.false&registeredAt=lt.%d", supabaseURL, twoDaysAgoMillis)
	req, err := http.NewRequest("GET", reqUrl, nil)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	req.Header.Set("apikey", supabaseSecretKey)
	req.Header.Set("Authorization", "Bearer "+supabaseSecretKey)
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{Timeout: 15 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer resp.Body.Close()

	var users []LocalUser
	json.NewDecoder(resp.Body).Decode(&users)

	deletedEmails := []string{}
	for _, user := range users {
		if err := deleteLocalUser(user.GoogleSub); err == nil {
			deletedEmails = append(deletedEmails, user.Email)
		}
	}

	w.Header().Set("Content-Type", "application/json")
	response := map[string]interface{}{
		"message":      "Manual demo deletion check completed successfully.",
		"checkedCount": len(users),
		"deleted":      deletedEmails,
	}
	json.NewEncoder(w).Encode(response)
}

// SignatureRequest menerima invoiceId sebagai string atau number dari JS
type SignatureRequest struct {
	InvoiceIdRaw    json.RawMessage `json:"invoiceId"`
	InvoiceId       int64           `json:"-"` // hasil parse dari InvoiceIdRaw
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

	// Update Supabase bmp_invoices table
	reqUrl := fmt.Sprintf("%s/rest/v1/bmp_invoices?id=eq.%d", supabaseURL, reqData.InvoiceId)
	
	updateFields := map[string]interface{}{
		"receiverSignatureUrl": savedUrl,
		"receiverNameActual":   reqData.ReceiverName,
		"updatedAt":            time.Now().UnixNano() / int64(time.Millisecond),
	}
	
	bodyBytes, err := json.Marshal(updateFields)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	req, err := http.NewRequest("PATCH", reqUrl, bytes.NewBuffer(bodyBytes))
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	req.Header.Set("apikey", supabaseSecretKey)
	req.Header.Set("Authorization", "Bearer "+supabaseSecretKey)
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		http.Error(w, "Supabase connection error: "+err.Error(), http.StatusInternalServerError)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		respBody, _ := io.ReadAll(resp.Body)
		http.Error(w, fmt.Sprintf("Supabase error [%d]: %s", resp.StatusCode, string(respBody)), resp.StatusCode)
		return
	}

	log.Printf("[Signature] Successfully saved receiver signature for invoice %d (URL: %s)", reqData.InvoiceId, savedUrl)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"message":      "Signature saved successfully",
		"signatureUrl": savedUrl,
	})
}

type AiClassifyRequest struct {
	Statement string `json:"statement"`
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

	// Panggil python script ai_detector.py
	detectorPath := "./ai_detector.py"
	cmd := exec.Command("python3", detectorPath, reqData.Statement)
	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &stderr

	if err := cmd.Run(); err != nil {
		http.Error(w, fmt.Sprintf("Failed to run AI classifier: %v (details: %s)", err, stderr.String()), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(out.Bytes())
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

        let isDrawing = false;
        let drawnSomething = false;
        let invoiceId = null;
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

                invoiceId = parts[0];
                const expiry = parseInt(parts[1]);
                
                // Cek kadaluarsa
                const now = Date.now();
                if (now > expiry) {
                    showExpiryError("Link ini telah kadaluarsa (melebihi batas 3 menit). Minta pengirim membuat link baru.");
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
                        token: tokenStr,
                        signatureBase64: signatureBase64,
                        receiverName: name
                    })
                });

                hideLoader();

                if (apiRes.ok || apiRes.status === 404) {
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

