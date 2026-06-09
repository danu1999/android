package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strconv"
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

// Performs core logic of querying and locking out expired demo users
func checkAndLockoutDemoUsers() error {
	twoDaysAgoMillis := time.Now().UnixNano()/int64(time.Millisecond) - (2 * 24 * 60 * 60 * 1000)

	// Query Supabase for active demo users who registered more than 2 days ago
	reqUrl := fmt.Sprintf("%s/rest/v1/local_users?isPremium=eq.false&isActive=eq.true&registeredAt=lt.%d", supabaseURL, twoDaysAgoMillis)
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

	log.Printf("Found %d expired demo users. Lockout in progress...", len(users))

	for _, user := range users {
		if err := updateLocalUserActiveStatus(user.GoogleSub, false); err != nil {
			log.Printf("Failed to lockout user %s: %v", user.Email, err)
		} else {
			regTime := time.Unix(user.RegisteredAt/1000, 0).Format("2006-01-02 15:04:05")
			log.Printf("Successfully locked out demo user: %s (Registered at: %s)", user.Email, regTime)
		}
	}

	return nil
}

// Updates the isActive field in Supabase for a specific googleSub
func updateLocalUserActiveStatus(googleSub string, active bool) error {
	reqUrl := fmt.Sprintf("%s/rest/v1/local_users?googleSub=eq.%s", supabaseURL, googleSub)
	
	bodyData := map[string]bool{"isActive": active}
	jsonBody, err := json.Marshal(bodyData)
	if err != nil {
		return err
	}

	req, err := http.NewRequest("PATCH", reqUrl, bytes.NewBuffer(jsonBody))
	if err != nil {
		return err
	}

	req.Header.Set("apikey", supabaseSecretKey)
	req.Header.Set("Authorization", "Bearer "+supabaseSecretKey)
	req.Header.Set("Content-Type", "application/json")

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
	reqUrl := fmt.Sprintf("%s/rest/v1/local_users?isPremium=eq.false&isActive=eq.true&registeredAt=lt.%d", supabaseURL, twoDaysAgoMillis)
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

	lockedOutEmails := []string{}
	for _, user := range users {
		if err := updateLocalUserActiveStatus(user.GoogleSub, false); err == nil {
			lockedOutEmails = append(lockedOutEmails, user.Email)
		}
	}

	w.Header().Set("Content-Type", "application/json")
	response := map[string]interface{}{
		"message":      "Manual demo lockout check completed successfully.",
		"checkedCount": len(users),
		"lockedOut":    lockedOutEmails,
	}
	json.NewEncoder(w).Encode(response)
}
