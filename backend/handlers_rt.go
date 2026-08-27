package main

// handlers_rt.go — Real-time API handlers for Full Online mode
// Replaces SupabaseSyncManager. All endpoints require Authorization: Bearer <token>.
// All queries filter by "tenantId" for tenant isolation.
// Target: GET < 150ms, POST/PUT < 300ms.

import (
	"crypto/rand"
	"database/sql"
	_ "embed"
	"encoding/csv"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"time"
)

//go:embed job_application_web.html
var jobAppWebHTML []byte


// ── Shared helpers ────────────────────────────────────────────────────────────

func extractTenantId(r *http.Request) (string, bool) {
	authHeader := r.Header.Get("Authorization")
	if !strings.HasPrefix(authHeader, "Bearer ") {
		return "", false
	}
	token := strings.TrimPrefix(authHeader, "Bearer ")
	if token == "" {
		return "", false
	}
	var tenantId string
	// Try 1: googleSub (numeric sub ID from Google SSO atau email token sesi lokal)
	err := db.QueryRow(`SELECT "tenantId" FROM "local_users" WHERE "googleSub" = $1 AND "isActive" = TRUE LIMIT 1`, token).Scan(&tenantId)
	if err != nil {
		// Try 2: email — untuk user premium statis (login email+password)
		err2 := db.QueryRow(`SELECT "tenantId" FROM "local_users" WHERE "email" = $1 AND "isActive" = TRUE LIMIT 1`, token).Scan(&tenantId)
		if err2 != nil {
			// Try 3: check employees / bmp_employees table — untuk admin/supervisor login (token = "emp:<id>")
			if strings.HasPrefix(token, "emp:") {
				empIdStr := strings.TrimPrefix(token, "emp:")
				err3 := db.QueryRow(`SELECT "tenantId" FROM "employees" WHERE id = $1 AND "isActive" = TRUE LIMIT 1`, empIdStr).Scan(&tenantId)
				if err3 != nil {
					// Fallback: check bmp_employees table
					err4 := db.QueryRow(`SELECT "tenantId" FROM "bmp_employees" WHERE id = $1 AND "isActive" = TRUE LIMIT 1`, empIdStr).Scan(&tenantId)
					if err4 != nil {
						return "", false
					}
				}
			} else {
				return "", false
			}
		}
	}
	return tenantId, tenantId != ""
}

func isOwnerToken(token string) bool {
	if strings.HasPrefix(token, "emp:") {
		return false
	}
	var count int
	_ = db.QueryRow(`
		SELECT COUNT(1) FROM "local_users" 
		WHERE ("googleSub" = $1 OR "email" = $1) 
		  AND "isActive" = TRUE`, token).Scan(&count)
	return count > 0
}

func checkOwnerOnly(w http.ResponseWriter, r *http.Request) bool {
	authHeader := r.Header.Get("Authorization")
	token := strings.TrimPrefix(authHeader, "Bearer ")
	if !isOwnerToken(token) {
		jsonErr(w, 403, "forbidden: owner access only")
		return false
	}
	return true
}

func isManagerOrOwnerToken(token string) bool {
	if !strings.HasPrefix(token, "emp:") {
		var count int
		_ = db.QueryRow(`
			SELECT COUNT(1) FROM "local_users" 
			WHERE ("googleSub" = $1 OR "email" = $1) 
			  AND "isActive" = TRUE`, token).Scan(&count)
		return count > 0
	}
	empIdStr := strings.TrimPrefix(token, "emp:")
	var role string
	err := db.QueryRow(`SELECT "role" FROM "employees" WHERE id = $1 AND "isActive" = TRUE LIMIT 1`, empIdStr).Scan(&role)
	if err == nil {
		roleUpper := strings.ToUpper(strings.TrimSpace(role))
		return roleUpper == "ADMIN" || roleUpper == "SUPERVISOR" || roleUpper == "OWNER"
	}
	err2 := db.QueryRow(`SELECT COALESCE("role", "position", '') FROM "bmp_employees" WHERE id = $1 AND "isActive" = TRUE LIMIT 1`, empIdStr).Scan(&role)
	if err2 == nil {
		roleUpper := strings.ToUpper(strings.TrimSpace(role))
		return roleUpper == "ADMIN" || roleUpper == "SUPERVISOR" || roleUpper == "OWNER"
	}
	return false
}

func checkManagerOrOwner(w http.ResponseWriter, r *http.Request) bool {
	authHeader := r.Header.Get("Authorization")
	token := strings.TrimPrefix(authHeader, "Bearer ")
	if !isManagerOrOwnerToken(token) {
		jsonErr(w, 403, "forbidden: manager or owner access only")
		return false
	}
	return true
}

func jsonOK(w http.ResponseWriter, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(data)
}

func jsonErr(w http.ResponseWriter, code int, msg string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	json.NewEncoder(w).Encode(map[string]string{"error": msg})
}

func nowMillis() int64 {
	return time.Now().UnixNano() / int64(time.Millisecond)
}

// deductRawMaterialsForTransaction: kurangi stok raw_materials berdasarkan resep (product_recipes)
// untuk setiap item yang terjual pada transaksi COMPLETED. Dijalankan secara goroutine (async).
func deductRawMaterialsForTransaction(transactionId int64, tenantId string) {
	// Ambil semua item transaksi beserta produknya
	rows, err := db.Query(`
		SELECT ti."productId", ti."quantity"
		FROM transaction_items ti
		WHERE ti."transactionId" = $1
	`, transactionId)
	if err != nil {
		log.Printf("[RecipeInventory] error fetching tx items for tx %d: %v", transactionId, err)
		return
	}
	defer rows.Close()

	type txItem struct {
		productId int64
		quantity  int64
	}
	var items []txItem
	for rows.Next() {
		var it txItem
		rows.Scan(&it.productId, &it.quantity)
		items = append(items, it)
	}

	// Untuk setiap item, kurangi stok bahan baku berdasarkan resep
	for _, item := range items {
		recipeRows, err := db.Query(`
			SELECT pr."rawMaterialId", pr."quantityNeeded"
			FROM product_recipes pr
			WHERE pr."productId" = $1 AND pr."tenantId" = $2
		`, item.productId, tenantId)
		if err != nil {
			continue
		}
		for recipeRows.Next() {
			var rawMatId int64
			var qtyNeeded float64
			recipeRows.Scan(&rawMatId, &qtyNeeded)
			totalDeduction := qtyNeeded * float64(item.quantity)
			_, _ = db.Exec(`
				UPDATE raw_materials SET
					"stock" = GREATEST(0, "stock" - $1),
					"updatedAt" = $2
				WHERE id = $3 AND "tenantId" = $4 AND "isDeleted" = FALSE
			`, totalDeduction, nowMillis(), rawMatId, tenantId)
		}
		recipeRows.Close()
	}
	log.Printf("[RecipeInventory] deducted raw materials for transaction %d", transactionId)
}

// ── Raw Materials (Stok Bahan Baku) ──────────────────────────────────────────
func handleRtRawMaterials(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		outletId := r.URL.Query().Get("outletId")
		var rows *sql.Rows
		var err error
		if outletId != "" {
			rows, err = db.Query(`SELECT * FROM raw_materials WHERE "tenantId"=$1 AND ("outletId"=$2 OR "outletId" IS NULL) AND "isDeleted"=FALSE ORDER BY name ASC`, tenantId, outletId)
		} else {
			rows, err = db.Query(`SELECT * FROM raw_materials WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY name ASC`, tenantId)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["createdAt"] = nowMillis(); body["updatedAt"] = nowMillis()
		id, err := insertRow("raw_materials", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtRawMaterialsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/raw-materials/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["updatedAt"] = nowMillis()
		updateRow("raw_materials", id, tenantId, body)
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE raw_materials SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── Product Recipes (Mapping Resep ke Bahan Baku) ────────────────────────────
func handleRtProductRecipes(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		productId := r.URL.Query().Get("productId")
		var rows *sql.Rows
		var err error
		if productId != "" {
			rows, err = db.Query(`
				SELECT pr.*, rm.name AS "rawMaterialName", rm."recipeUnit", rm."purchaseUnit", rm."conversionRate"
				FROM product_recipes pr
				LEFT JOIN raw_materials rm ON rm.id = pr."rawMaterialId"
				WHERE pr."tenantId"=$1 AND pr."productId"=$2`, tenantId, productId)
		} else {
			rows, err = db.Query(`SELECT * FROM product_recipes WHERE "tenantId"=$1`, tenantId)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["createdAt"] = nowMillis(); body["updatedAt"] = nowMillis()
		id, err := insertRow("product_recipes", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtProductRecipesById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/product-recipes/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["updatedAt"] = nowMillis()
		updateRow("product_recipes", id, tenantId, body)
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`DELETE FROM product_recipes WHERE id=$1 AND "tenantId"=$2`, id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── Product Modifiers (Topping/Kustomisasi per Varian) ───────────────────────
func handleRtProductModifiers(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		productId := r.URL.Query().Get("productId")
		var rows *sql.Rows
		var err error
		if productId != "" {
			rows, err = db.Query(`SELECT * FROM product_modifiers WHERE "tenantId"=$1 AND "productId"=$2 AND "isDeleted"=FALSE ORDER BY name ASC`, tenantId, productId)
		} else {
			rows, err = db.Query(`SELECT * FROM product_modifiers WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY name ASC`, tenantId)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["createdAt"] = nowMillis(); body["updatedAt"] = nowMillis()
		id, err := insertRow("product_modifiers", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtProductModifiersById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/product-modifiers/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["updatedAt"] = nowMillis()
		updateRow("product_modifiers", id, tenantId, body)
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE product_modifiers SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── Transaction Item Modifiers ────────────────────────────────────────────────
func handleRtTxItemModifiers(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		txItemId := r.URL.Query().Get("transactionItemId")
		var rows *sql.Rows
		var err error
		if txItemId != "" {
			rows, err = db.Query(`SELECT * FROM transaction_item_modifiers WHERE "tenantId"=$1 AND "transactionItemId"=$2`, tenantId, txItemId)
		} else {
			rows, err = db.Query(`SELECT * FROM transaction_item_modifiers WHERE "tenantId"=$1`, tenantId)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body []map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			// fallback: try single object
			var single map[string]interface{}
			json.NewDecoder(r.Body).Decode(&single)
			single["tenantId"] = tenantId
			id, err2 := insertRow("transaction_item_modifiers", single)
			if err2 != nil { jsonErr(w, 500, err2.Error()); return }
			jsonOK(w, map[string]interface{}{"id": id, "ok": true})
			return
		}
		for _, item := range body {
			item["tenantId"] = tenantId
			insertRow("transaction_item_modifiers", item)
		}
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── Cashier Shifts (Buka/Tutup Shift Kasir) ───────────────────────────────────
func handleRtCashierShifts(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		status := r.URL.Query().Get("status")
		employeeId := r.URL.Query().Get("employeeId")
		var rows *sql.Rows
		var err error
		if employeeId != "" && status != "" {
			rows, err = db.Query(`SELECT * FROM cashier_shifts WHERE "tenantId"=$1 AND "employeeId"=$2 AND "status"=$3 ORDER BY "openedAt" DESC LIMIT 1`, tenantId, employeeId, status)
		} else if employeeId != "" {
			rows, err = db.Query(`SELECT * FROM cashier_shifts WHERE "tenantId"=$1 AND "employeeId"=$2 ORDER BY "openedAt" DESC LIMIT 20`, tenantId, employeeId)
		} else {
			rows, err = db.Query(`SELECT * FROM cashier_shifts WHERE "tenantId"=$1 ORDER BY "openedAt" DESC LIMIT 50`, tenantId)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		// Buka shift baru
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId
		body["openedAt"] = nowMillis()
		body["status"] = "OPEN"
		body["createdAt"] = nowMillis(); body["updatedAt"] = nowMillis()
		id, err := insertRow("cashier_shifts", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtCashierShiftsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/cashier-shifts/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		// Tutup shift atau update ekspektasi kas akhir
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["updatedAt"] = nowMillis()
		// Hitung selisih kas jika tutup shift
		if body["status"] == "CLOSED" {
			body["closedAt"] = nowMillis()
			actualEnd, _ := body["actualEndCash"].(float64)
			expectedEnd, _ := body["expectedEndCash"].(float64)
			body["cashDifference"] = actualEnd - expectedEnd
		}
		updateRow("cashier_shifts", id, tenantId, body)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// insertRow: dynamic INSERT INTO "table" RETURNING id
func insertRow(table string, data map[string]interface{}) (int64, error) {
	cols := make([]string, 0, len(data))
	vals := make([]interface{}, 0, len(data))
	placeholders := make([]string, 0, len(data))
	i := 1
	for k, v := range data {
		cols = append(cols, `"`+k+`"`)
		vals = append(vals, v)
		placeholders = append(placeholders, "$"+strconv.Itoa(i))
		i++
	}
	q := `INSERT INTO "` + table + `" (` + strings.Join(cols, ",") + `) VALUES (` + strings.Join(placeholders, ",") + `) RETURNING id`
	var id int64
	err := db.QueryRow(q, vals...).Scan(&id)
	return id, err
}

// updateRow: dynamic UPDATE "table" SET ... WHERE id=$N AND "tenantId"=$M
func updateRow(table string, id int64, tenantId string, data map[string]interface{}) error {
	setParts := make([]string, 0)
	vals := make([]interface{}, 0)
	i := 1
	for k, v := range data {
		if k == "id" || k == "tenantId" {
			continue
		}
		setParts = append(setParts, `"`+k+`"=$`+strconv.Itoa(i))
		vals = append(vals, v)
		i++
	}
	if len(setParts) == 0 {
		return nil
	}
	vals = append(vals, id, tenantId)
	q := `UPDATE "` + table + `" SET ` + strings.Join(setParts, ",") + ` WHERE id=$` + strconv.Itoa(i) + ` AND "tenantId"=$` + strconv.Itoa(i+1)
	_, err := db.Exec(q, vals...)
	return err
}

// rowsToJSON converts *sql.Rows to []map[string]interface{} for JSON response
func rowsToJSON(rows *sql.Rows) []map[string]interface{} {
	cols, _ := rows.Columns()
	var result []map[string]interface{}
	for rows.Next() {
		scanArgs := make([]interface{}, len(cols))
		values := make([]interface{}, len(cols))
		for i := range values {
			scanArgs[i] = &values[i]
		}
		if err := rows.Scan(scanArgs...); err != nil {
			continue
		}
		rowMap := make(map[string]interface{})
		for i, col := range cols {
			val := values[i]
			if b, ok := val.([]byte); ok {
				rowMap[col] = string(b)
			} else {
				rowMap[col] = val
			}
		}
		result = append(result, rowMap)
	}
	if result == nil {
		return []map[string]interface{}{}
	}
	return result
}

// ── Migration API ─────────────────────────────────────────────────────────────

// POST /api/migration/verify-table
// Body: {"tableName":"bmp_invoices","expectedCount":213}
// Response: {"match":true,"serverCount":213,"clientCount":213}
func handleMigrationVerifyTable(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		jsonErr(w, 405, "POST required"); return
	}
	tenantId, ok := extractTenantId(r)
	if !ok {
		jsonErr(w, 401, "unauthorized"); return
	}
	var req struct {
		TableName     string `json:"tableName"`
		ExpectedCount int    `json:"expectedCount"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonErr(w, 400, "invalid json"); return
	}
	allowedTables := map[string]bool{
		"local_users": true, "tenants": true, "outlets": true, "employees": true,
		"products": true, "customers": true,
		"bmp_clients": true, "bmp_master_products": true, "bmp_settings": true,
		"bmp_employees": true, "print_settings": true,
		"transactions": true, "bmp_invoices": true, "bmp_bahan_baku": true,
		"transaction_items": true, "bmp_products": true, "bmp_invoice_payments": true,
		"bmp_bahan_baku_item": true, "bmp_product_stocks": true,
		"bmp_stock_ledger": true, "bmp_production_logs": true, "activity_logs": true,
	}
	if !allowedTables[req.TableName] {
		jsonErr(w, 400, "unknown table: "+req.TableName); return
	}
	noTenantFilter := map[string]bool{"local_users": true, "tenants": true}
	var serverCount int
	var err error
	if noTenantFilter[req.TableName] {
		err = db.QueryRow(`SELECT COUNT(*) FROM "` + req.TableName + `"`).Scan(&serverCount)
	} else if req.TableName == "transaction_items" {
		err = db.QueryRow(`SELECT COUNT(*) FROM "transaction_items" ti JOIN "transactions" t ON t.id = ti."transactionId" WHERE t."tenantId" = $1`, tenantId).Scan(&serverCount)
	} else {
		err = db.QueryRow(`SELECT COUNT(*) FROM "`+req.TableName+`" WHERE "tenantId" = $1`, tenantId).Scan(&serverCount)
	}
	if err != nil {
		jsonErr(w, 500, "db error: "+err.Error()); return
	}
	jsonOK(w, map[string]interface{}{
		"match":       serverCount == req.ExpectedCount,
		"serverCount": serverCount,
		"clientCount": req.ExpectedCount,
		"tableName":   req.TableName,
	})
}

// GET /api/migration/check-readiness
// Checks if Grup A tables exist on VPS before migration starts.
func handleMigrationCheckReadiness(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		jsonErr(w, 405, "GET required"); return
	}
	tenantId, ok := extractTenantId(r)
	if !ok {
		jsonErr(w, 401, "unauthorized"); return
	}
	existing := map[string]int{}
	var missing []string

	var n int
	db.QueryRow(`SELECT COUNT(*) FROM "local_users"`).Scan(&n)
	existing["local_users"] = n
	if n == 0 { missing = append(missing, "local_users") }

	n = 0
	db.QueryRow(`SELECT COUNT(*) FROM "tenants" WHERE id=$1`, tenantId).Scan(&n)
	existing["tenants"] = n
	if n == 0 { missing = append(missing, "tenants") }

	n = 0
	db.QueryRow(`SELECT COUNT(*) FROM "outlets" WHERE "tenantId"=$1`, tenantId).Scan(&n)
	existing["outlets"] = n
	if n == 0 { missing = append(missing, "outlets") }

	n = 0
	db.QueryRow(`SELECT COUNT(*) FROM "employees" WHERE "tenantId"=$1`, tenantId).Scan(&n)
	existing["employees"] = n
	if n == 0 { missing = append(missing, "employees") }

	jsonOK(w, map[string]interface{}{
		"ready":          len(missing) == 0,
		"missingTables":  missing,
		"existingCounts": existing,
		"tenantId":       tenantId,
	})
}

// ── PIN Login ─────────────────────────────────────────────────────────────────

// POST /api/auth/pin-login  Body: {"tenantId":"...","email":"...","pin":"1234"}
func handlePinLogin(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		jsonErr(w, 405, "POST required"); return
	}
	var req struct {
		TenantId string `json:"tenantId"`
		Email    string `json:"email"`
	}
	json.NewDecoder(r.Body).Decode(&req)
	var pinHash, name, role string
	var id int64
	err := db.QueryRow(`SELECT id, "pinHash", name, role FROM employees WHERE "tenantId"=$1 AND email=$2 AND "isActive"=TRUE LIMIT 1`,
		req.TenantId, req.Email).Scan(&id, &pinHash, &name, &role)
	if err != nil {
		jsonErr(w, 401, "employee not found"); return
	}
	jsonOK(w, map[string]interface{}{"id": id, "name": name, "role": role, "pinHash": pinHash})
}

// ── Core POS — products ───────────────────────────────────────────────────────

func handleRtProducts(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	outletId := r.URL.Query().Get("outletId")
	switch r.Method {
	case http.MethodGet:
		var rows *sql.Rows
		var err error
		if outletId != "" {
			rows, err = db.Query(`SELECT * FROM products WHERE "tenantId"=$1 AND ("outletId"=$2 OR "outletId" IS NULL) AND "isDeleted"=FALSE ORDER BY name ASC`, tenantId, outletId)
		} else {
			rows, err = db.Query(`SELECT * FROM products WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY name ASC`, tenantId)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		if _, ok := body["createdAt"]; !ok { body["createdAt"] = nowMillis() }
		id, err := insertRow("products", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtProductsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/products/")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil { jsonErr(w, 400, "invalid id"); return }
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["updatedAt"] = nowMillis()
		updateRow("products", id, tenantId, body)
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE products SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── Core POS — customers ──────────────────────────────────────────────────────

func handleRtCustomers(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, err := db.Query(`SELECT * FROM customers WHERE "tenantId"=$1 ORDER BY name ASC`, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		if _, ok := body["createdAt"]; !ok { body["createdAt"] = nowMillis() }
		id, err := insertRow("customers", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtCustomersById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/customers/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodDelete:
		db.Exec(`DELETE FROM customers WHERE id=$1 AND "tenantId"=$2`, id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── Core POS — transactions ───────────────────────────────────────────────────

func handleRtTransactions(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		outletId := r.URL.Query().Get("outletId")
		limit := r.URL.Query().Get("limit")
		if limit == "" { limit = "500" }
		var rows *sql.Rows
		var err error
		if outletId != "" {
			rows, err = db.Query(`SELECT * FROM transactions WHERE "tenantId"=$1 AND ("outletId"=$2 OR "outletId" IS NULL) AND "isDeleted"=FALSE ORDER BY date DESC LIMIT $3`, tenantId, outletId, limit)
		} else {
			rows, err = db.Query(`SELECT * FROM transactions WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY date DESC LIMIT $2`, tenantId, limit)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		if _, ok := body["createdAt"]; !ok { body["createdAt"] = nowMillis() }
		id, err := insertRow("transactions", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		// Auto-deduct raw material stock based on product recipes (Recipe-Based Inventory)
		// Hanya dilakukan untuk transaksi COMPLETED tipe SALES
		status, _ := body["status"].(string)
		txType, _ := body["type"].(string)
		if status == "COMPLETED" && (txType == "SALES" || txType == "") {
			go deductRawMaterialsForTransaction(id, tenantId)
		}

		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}


func handleRtTransactionsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/transactions/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["updatedAt"] = nowMillis()
		updateRow("transactions", id, tenantId, body)
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE transactions SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtTransactionItems(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		txId := r.URL.Query().Get("transactionId")
		if txId == "" { jsonErr(w, 400, "transactionId required"); return }
		rows, err := db.Query(`SELECT ti.* FROM transaction_items ti JOIN transactions t ON t.id=ti."transactionId" WHERE ti."transactionId"=$1 AND t."tenantId"=$2`, txId, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body []map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		for _, item := range body { insertRow("transaction_items", item) }
		jsonOK(w, map[string]interface{}{"ok": true, "count": len(body)})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── Core — employees & outlets ────────────────────────────────────────────────

func handleRtEmployees(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM employees WHERE "tenantId"=$1 AND "isActive"=TRUE ORDER BY name ASC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		if _, ok := body["createdAt"]; !ok { body["createdAt"] = nowMillis() }
		id, err := insertRow("employees", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtEmployeesById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/employees/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["updatedAt"] = nowMillis()
		updateRow("employees", id, tenantId, body); jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE employees SET "isActive"=FALSE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleBmpPinLogin(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		jsonErr(w, 405, "POST required"); return
	}
	var req struct {
		TenantId string `json:"tenantId"`
		Email    string `json:"email"`
	}
	json.NewDecoder(r.Body).Decode(&req)
	var pinHash, name, role string
	var id int64
	err := db.QueryRow(`
		SELECT be.id, e."pinHash", be.name, be.role 
		FROM bmp_employees be 
		JOIN employees e ON be."employeeId" = e.id 
		WHERE be."tenantId"=$1 AND e.email=$2 AND be."isActive"=TRUE LIMIT 1`,
		req.TenantId, req.Email).Scan(&id, &pinHash, &name, &role)
	if err != nil {
		jsonErr(w, 401, "employee not found"); return
	}
	jsonOK(w, map[string]interface{}{"id": id, "name": name, "role": role, "pinHash": pinHash})
}

func handleRtOutlets(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM outlets WHERE "tenantId"=$1 ORDER BY "isDefault" DESC, name ASC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId
		id, err := insertRow("outlets", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtOutletsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/outlets/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		updateRow("outlets", id, tenantId, body); jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`DELETE FROM outlets WHERE id=$1 AND "tenantId"=$2`, id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — clients ─────────────────────────────────────────────────────────────

func handleRtBmpClients(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_clients WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "clientName" ASC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		if _, ok := body["createdAt"]; !ok { body["createdAt"] = nowMillis() }
		id, err := insertRow("bmp_clients", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpClientsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/clients/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["updatedAt"] = nowMillis()
		updateRow("bmp_clients", id, tenantId, body); jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE bmp_clients SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — invoices ────────────────────────────────────────────────────────────

func syncRtBmpInvoicePaid(tenantId string, invoiceId int64) {
	if invoiceId <= 0 {
		return
	}
	var totalPaid float64
	_ = db.QueryRow(`
		SELECT COALESCE(SUM("paymentAmount"), 0.0)
		FROM bmp_invoice_payments
		WHERE "tenantId"=$1 AND "invoiceId"=$2 AND "isDeleted"=FALSE
	`, tenantId, invoiceId).Scan(&totalPaid)

	var totalAmount float64
	var dueDate sql.NullInt64
	err := db.QueryRow(`
		SELECT "totalAmount", "dueDate"
		FROM bmp_invoices
		WHERE "tenantId"=$1 AND id=$2
	`, tenantId, invoiceId).Scan(&totalAmount, &dueDate)
	if err != nil {
		return
	}

	status := "UNPAID"
	now := nowMillis()
	if totalPaid >= totalAmount-0.01 && totalAmount > 0 {
		status = "PAID"
	} else if totalPaid > 0 {
		if dueDate.Valid && now > dueDate.Int64 {
			status = "OVERDUE"
		} else {
			status = "PARTIAL"
		}
	} else {
		if dueDate.Valid && now > dueDate.Int64 {
			status = "OVERDUE"
		} else {
			status = "UNPAID"
		}
	}

	_, _ = db.Exec(`
		UPDATE bmp_invoices
		SET "paidAmount"=$1, "status"=$2, "updatedAt"=$3
		WHERE "tenantId"=$4 AND id=$5
	`, totalPaid, status, now, tenantId, invoiceId)
}

func handleRtBmpInvoices(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_invoices WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "createdAt" DESC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		if _, ok := body["createdAt"]; !ok { body["createdAt"] = nowMillis() }
		if _, ok := body["title"]; !ok { body["title"] = "Invoice Baru" }
		if _, ok := body["slug"]; !ok {
			numberStr := ""
			if num, ok := body["number"].(string); ok {
				numberStr = num
			}
			body["slug"] = fmt.Sprintf("inv-%s-%d", numberStr, nowMillis())
		}
		
		paidAmountInput := 0.0
		if paidVal, ok := body["paidAmount"]; ok {
			if num, ok := paidVal.(float64); ok {
				paidAmountInput = num
			}
		}

		id, err := insertRow("bmp_invoices", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		// Jika ada uang muka / DP saat pembuatan invoice, otomatis catat payment record pertama
		if paidAmountInput > 0 {
			payBody := map[string]interface{}{
				"tenantId":      tenantId,
				"invoiceId":     id,
				"paymentDate":   body["createdAt"],
				"paymentAmount": paidAmountInput,
				"paymentMethod": "CASH",
				"notes":         "Uang Muka (DP) saat pembuatan invoice",
				"isDeleted":     false,
				"isSynced":      true,
			}
			_, _ = insertRow("bmp_invoice_payments", payBody)
			syncRtBmpInvoicePaid(tenantId, id)
		}

		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpInvoicesById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/invoices/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["updatedAt"] = nowMillis()
		updateRow("bmp_invoices", id, tenantId, body)
		syncRtBmpInvoicePaid(tenantId, id)
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE bmp_invoices SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — invoice products, master products, cashflow, payments ───────────────

func handleRtBmpProducts(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		invoiceId := r.URL.Query().Get("invoiceId")
		var rows *sql.Rows
		var err error
		if invoiceId != "" {
			rows, err = db.Query(`SELECT bp.* FROM bmp_products bp JOIN bmp_invoices bi ON bi.id=bp."invoiceId" WHERE bi."tenantId"=$1 AND bp."invoiceId"=$2 AND bp."isDeleted"=FALSE ORDER BY bp.id ASC`, tenantId, invoiceId)
		} else {
			rows, err = db.Query(`SELECT bp.* FROM bmp_products bp JOIN bmp_invoices bi ON bi.id=bp."invoiceId" WHERE bi."tenantId"=$1 AND bp."isDeleted"=FALSE ORDER BY bp.id ASC`, tenantId)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		if nameVal, ok := body["name"]; ok {
			body["title"] = nameVal
			delete(body, "name")
		}
		if _, ok := body["title"]; !ok { body["title"] = "" }
		id, err := insertRow("bmp_products", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpProductsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/products/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["updatedAt"] = nowMillis()
		updateRow("bmp_products", id, tenantId, body); jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE bmp_products SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpMasterProducts(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_master_products WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY title ASC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		id, err := insertRow("bmp_master_products", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpMasterProductsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/master-products/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["updatedAt"] = nowMillis()
		updateRow("bmp_master_products", id, tenantId, body); jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE bmp_master_products SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// handleRtBmpCashflow dan handleRtBmpCashflowById dihapus (Jalur 2 removed — v2.20.0)

func handleRtBmpPayments(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_invoice_payments WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "paymentDate" DESC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId
		if _, ok := body["paymentDate"]; !ok { body["paymentDate"] = nowMillis() }
		id, err := insertRow("bmp_invoice_payments", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		var invoiceId int64
		if invIdVal, ok := body["invoiceId"]; ok {
			if num, ok := invIdVal.(float64); ok {
				invoiceId = int64(num)
			}
		}
		if invoiceId > 0 {
			syncRtBmpInvoicePaid(tenantId, invoiceId)
		}

		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpPaymentsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/payments/")
	id, _ := strconv.ParseInt(idStr, 10, 64)

	var invoiceId int64
	_ = db.QueryRow(`SELECT "invoiceId" FROM bmp_invoice_payments WHERE id=$1 AND "tenantId"=$2`, id, tenantId).Scan(&invoiceId)

	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["updatedAt"] = nowMillis()
		updateRow("bmp_invoice_payments", id, tenantId, body)
		if invoiceId > 0 {
			syncRtBmpInvoicePaid(tenantId, invoiceId)
		}
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_invoice_payments SET "isDeleted"=TRUE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		if invoiceId > 0 {
			syncRtBmpInvoicePaid(tenantId, invoiceId)
		}
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — drivers & fleet ───────────────────────────────────────────────────

func handleRtBmpDrivers(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, err := db.Query(`SELECT * FROM bmp_drivers WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY name ASC`, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["tenantId"] = tenantId
		now := nowMillis()
		if _, ok := body["createdAt"]; !ok { body["createdAt"] = now }
		body["updatedAt"] = now
		id, err := insertRow("bmp_drivers", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpDriversById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/drivers/")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil { jsonErr(w, 400, "invalid id"); return }

	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["updatedAt"] = nowMillis()
		updateRow("bmp_drivers", id, tenantId, body)
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_drivers SET "isDeleted"=TRUE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — employees & payrolls ────────────────────────────────────────────────

func handleRtBmpEmployees(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if !checkManagerOrOwner(w, r) { return }
	switch r.Method {
	case http.MethodGet:
		rows, err := db.Query(`SELECT * FROM bmp_employees WHERE "tenantId"=$1 AND "isActive"=TRUE AND "isDeleted"=FALSE ORDER BY name ASC`, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["tenantId"] = tenantId
		now := nowMillis()
		if _, ok := body["createdAt"]; !ok {
			body["createdAt"] = now
		}
		body["updatedAt"] = now
		body["isDeleted"] = false
		if _, ok := body["isActive"]; !ok {
			body["isActive"] = true
		}
		if rVal, ok := body["role"]; ok && rVal != nil {
			if _, hasPos := body["position"]; !hasPos || body["position"] == nil {
				body["position"] = rVal
			}
		} else if pVal, ok := body["position"]; ok && pVal != nil {
			body["role"] = pVal
		}
		if sVal, ok := body["salaryAmount"]; ok && sVal != nil {
			body["salary"] = sVal
		} else if sVal, ok := body["salary"]; ok && sVal != nil {
			body["salaryAmount"] = sVal
		}
		id, err := insertRow("bmp_employees", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpEmployeesById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if !checkManagerOrOwner(w, r) { return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/employees/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["updatedAt"] = nowMillis()
		if rVal, ok := body["role"]; ok && rVal != nil {
			body["position"] = rVal
		} else if pVal, ok := body["position"]; ok && pVal != nil {
			body["role"] = pVal
		}
		if sVal, ok := body["salaryAmount"]; ok && sVal != nil {
			body["salary"] = sVal
		} else if sVal, ok := body["salary"]; ok && sVal != nil {
			body["salaryAmount"] = sVal
		}
		err := updateRow("bmp_employees", id, tenantId, body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_employees SET "isActive"=FALSE, "isDeleted"=TRUE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpPayrolls(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if !checkManagerOrOwner(w, r) { return }
	switch r.Method {
	case http.MethodGet:
		rows, err := db.Query(`SELECT * FROM bmp_payrolls WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "paymentDate" DESC`, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["tenantId"] = tenantId
		now := nowMillis()
		if _, ok := body["paymentDate"]; !ok {
			body["paymentDate"] = now
		}
		if _, ok := body["createdAt"]; !ok {
			body["createdAt"] = now
		}
		body["updatedAt"] = now
		body["isDeleted"] = false
		body["isSynced"] = true

		id, err := insertRow("bmp_payrolls", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		// Update lastPaidAt pada bmp_employees jika employeeId disertakan
		if empIdVal, ok := body["employeeId"]; ok {
			var empId int64
			switch v := empIdVal.(type) {
			case float64:
				empId = int64(v)
			case int64:
				empId = v
			case int:
				empId = int64(v)
			case string:
				empId, _ = strconv.ParseInt(v, 10, 64)
			}
			if empId > 0 {
				var payDate int64 = now
				if pd, ok := body["paymentDate"].(float64); ok {
					payDate = int64(pd)
				}
				_, _ = db.Exec(`UPDATE bmp_employees SET "lastPaidAt"=$1, "updatedAt"=$2 WHERE id=$3 AND "tenantId"=$4`, payDate, now, empId, tenantId)
			}
		}

		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpPayrollsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if !checkManagerOrOwner(w, r) { return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/payrolls/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_payrolls SET "isDeleted"=TRUE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}



// ── BMP — bahan baku & items ──────────────────────────────────────────────────

func handleRtBmpBahanBaku(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_bahan_baku WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY tanggal DESC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		id, err := insertRow("bmp_bahan_baku", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		// Trigger HPP recalculation for the period
		txDateBb := nowMillis()
		if tgl, ok := body["tanggal"].(float64); ok {
			txDateBb = int64(tgl)
		}
		startMsBb, endMsBb, dateStrBb := getPeriodRangeFromMs(txDateBb)
		_, _ = updateAndCalculateCOGS(tenantId, startMsBb, endMsBb, dateStrBb, "MONTHLY")

		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpBahanBakuById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/bahan-baku/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["updatedAt"] = nowMillis()
		err := updateRow("bmp_bahan_baku", id, tenantId, body)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		// Trigger HPP recalculation for the period
		txDate := nowMillis()
		if tgl, ok := body["tanggal"].(float64); ok {
			txDate = int64(tgl)
		}
		startMs, endMs, dateStr := getPeriodRangeFromMs(txDate)
		_, _ = updateAndCalculateCOGS(tenantId, startMs, endMs, dateStr, "MONTHLY")

		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_bahan_baku SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpBahanBakuItems(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		bahanBakuId := r.URL.Query().Get("bahanBakuId")
		var rows *sql.Rows
		var err error
		if bahanBakuId != "" {
			rows, err = db.Query(`SELECT bbi.* FROM bmp_bahan_baku_item bbi JOIN bmp_bahan_baku bb ON bb.id=bbi."bahanBakuId" WHERE bb."tenantId"=$1 AND bbi."bahanBakuId"=$2 AND bbi."isDeleted"=FALSE ORDER BY bbi.id ASC`, tenantId, bahanBakuId)
		} else {
			rows, err = db.Query(`SELECT bbi.* FROM bmp_bahan_baku_item bbi JOIN bmp_bahan_baku bb ON bb.id=bbi."bahanBakuId" WHERE bb."tenantId"=$1 AND bbi."isDeleted"=FALSE ORDER BY bbi.id ASC`, tenantId)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body []map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		for _, item := range body { item["tenantId"] = tenantId; insertRow("bmp_bahan_baku_item", item) }
		jsonOK(w, map[string]interface{}{"ok": true, "count": len(body)})
	case http.MethodDelete:
		bahanBakuId := r.URL.Query().Get("bahanBakuId")
		if bahanBakuId == "" {
			jsonErr(w, 400, "missing bahanBakuId")
			return
		}
		_, err := db.Exec(`DELETE FROM "bmp_bahan_baku_item" WHERE "bahanBakuId"=$1 AND "tenantId"=$2`, bahanBakuId, tenantId)
		if err != nil {
			jsonErr(w, 500, err.Error())
			return
		}
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — production, stocks, ledger ─────────────────────────────────────────

func handleRtBmpProductionLogs(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_production_logs WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "productionDate" DESC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid body")
			return
		}
		body["tenantId"] = tenantId
		
		statusVal, ok := body["status"].(string)
		if !ok || statusVal == "" {
			body["status"] = "COMPLETED" // Default
			statusVal = "COMPLETED"
		}
		
		// v2.19.17: resolve is_machine_active from the referenced machine
		// (pre-populate before insert so insertRow picks it up)
		if machineIdRaw, ok2 := body["machine_id"]; ok2 && machineIdRaw != nil {
			var machineActive bool
			errM := db.QueryRow(`SELECT COALESCE("is_active", TRUE) FROM bmp_machines WHERE id=$1 AND "tenantId"=$2`, machineIdRaw, tenantId).Scan(&machineActive)
			if errM == nil {
				body["is_machine_active"] = machineActive
			}
		}

		id, err := insertRow("bmp_production_logs", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		
		if statusVal == "COMPLETED" {
			errTrigger := triggerProductionLogCompletion(tenantId, id)
			if errTrigger != nil {
				log.Printf("[Warning] Gagal memicu penyelesaian log produksi: %v", errTrigger)
			}
		}
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpProductionLogsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/production-logs/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid body")
			return
		}
		err := updateRow("bmp_production_logs", id, tenantId, body)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		statusVal, _ := body["status"].(string)
		if statusVal == "COMPLETED" {
			errTrigger := triggerProductionLogCompletion(tenantId, id)
			if errTrigger != nil {
				log.Printf("[Warning] Gagal memicu penyelesaian log produksi: %v", errTrigger)
			}
		}
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_production_logs SET "isDeleted"=TRUE WHERE id=$1 AND "tenantId"=$2`, id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		// Cascade soft-delete ke bmp_production_materials
		_, _ = db.Exec(`UPDATE bmp_production_materials SET "isDeleted"=TRUE WHERE "productionLogId"=$1 AND "tenantId"=$2`, id, tenantId)

		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpProductStocks(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, err := db.Query(`
			SELECT id, "tenantId", "masterProductId" AS "masterItemId", "quantity" AS "currentStock", "minStockAlert", "isSynced", "isDeleted", "updatedAt", "outletId" 
			FROM bmp_product_stocks 
			WHERE "tenantId"=$1 AND "isDeleted"=FALSE`, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid body")
			return
		}
		
		masterItemIdNum, _ := body["masterItemId"].(float64)
		masterItemId := int64(masterItemIdNum)
		currentStockNum, _ := body["currentStock"].(float64)
		
		var id int64
		err := db.QueryRow(`
			INSERT INTO "bmp_product_stocks" ("tenantId", "masterProductId", "quantity", "updatedAt") 
			VALUES ($1, $2, $3, $4) 
			ON CONFLICT ("masterProductId", "tenantId") 
			DO UPDATE SET "quantity" = EXCLUDED."quantity", "updatedAt" = EXCLUDED."updatedAt" 
			RETURNING id`, 
			tenantId, masterItemId, currentStockNum, nowMillis()).Scan(&id)
		
		if err != nil { 
			jsonErr(w, 500, err.Error())
			return 
		}
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpStockLedger(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, err := db.Query(`
			SELECT id, "tenantId", "masterProductId" AS "masterItemId", "referenceId", "mutationType", "quantityChange" AS "change", "finalStock" AS "stockAfter", "notes", "isSynced", "isDeleted", "createdAt" 
			FROM bmp_stock_ledger 
			WHERE "tenantId"=$1 AND "isDeleted"=FALSE 
			ORDER BY "createdAt" DESC`, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid body")
			return
		}
		
		masterItemIdNum, _ := body["masterItemId"].(float64)
		masterItemId := int64(masterItemIdNum)
		changeNum, _ := body["change"].(float64)
		stockAfterNum, _ := body["stockAfter"].(float64)
		mutationType, _ := body["mutationType"].(string)
		notes, _ := body["notes"].(string)
		
		var referenceId int64
		if refVal, ok := body["referenceId"]; ok && refVal != nil {
			if refNum, ok := refVal.(float64); ok {
				referenceId = int64(refNum)
			}
		}
		
		var id int64
		err := db.QueryRow(`
			INSERT INTO "bmp_stock_ledger" 
			("tenantId", "masterProductId", "referenceId", "mutationType", "quantityChange", "finalStock", "notes", "createdAt") 
			VALUES ($1, $2, $3, $4, $5, $6, $7, $8) 
			RETURNING id`, 
			tenantId, masterItemId, referenceId, mutationType, changeNum, stockAfterNum, notes, nowMillis()).Scan(&id)
			
		if err != nil { 
			jsonErr(w, 500, err.Error())
			return 
		}
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — settings & print settings ──────────────────────────────────────────

func handleRtBmpSettings(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_settings WHERE "tenantId"=$1 LIMIT 1`, tenantId)
		defer rows.Close()
		result := rowsToJSON(rows)
		if len(result) > 0 {
			res := result[0]
			// Map DB keys to app keys
			if val, ok := res["clientName"]; ok { res["companyName"] = val }
			if val, ok := res["clientLogo"]; ok { res["logoUrl"] = val }
			if val, ok := res["addressLine1"]; ok { res["address"] = val }
			if val, ok := res["phoneNumber"]; ok { res["phone"] = val }
			if val, ok := res["emailAddress"]; ok { res["email"] = val }
			if val, ok := res["taxNumber"]; ok { res["npwp"] = val }
			jsonOK(w, res)
		} else {
			jsonOK(w, nil)
		}
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid JSON: " + err.Error())
			return
		}
		body["tenantId"] = tenantId
		body["updatedAt"] = nowMillis()

		// Map app keys to DB keys
		if val, ok := body["companyName"]; ok { body["clientName"] = val; delete(body, "companyName") }
		if val, ok := body["logoUrl"]; ok { body["clientLogo"] = val; delete(body, "logoUrl") }
		if val, ok := body["address"]; ok { body["addressLine1"] = val; delete(body, "address") }
		if val, ok := body["phone"]; ok { body["phoneNumber"] = val; delete(body, "phone") }
		if val, ok := body["email"]; ok { body["emailAddress"] = val; delete(body, "email") }
		if val, ok := body["npwp"]; ok { body["taxNumber"] = val; delete(body, "npwp") }
		delete(body, "bankInfo")
		delete(body, "invoicePrefix")


		db.Exec(`DELETE FROM bmp_settings WHERE "tenantId"=$1`, tenantId)
		_, err := insertRow("bmp_settings", body)
		if err != nil {
			jsonErr(w, 500, "failed to save settings: " + err.Error())
			return
		}
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtPrintSettings(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	moduleKey := r.URL.Query().Get("moduleKey")
	switch r.Method {
	case http.MethodGet:
		var rows *sql.Rows
		if moduleKey != "" {
			rows, _ = db.Query(`SELECT * FROM print_settings WHERE "tenantId"=$1 AND "moduleKey"=$2 LIMIT 1`, tenantId, moduleKey)
		} else {
			rows, _ = db.Query(`SELECT * FROM print_settings WHERE "tenantId"=$1`, tenantId)
		}
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		mk, _ := body["moduleKey"].(string)
		db.Exec(`DELETE FROM print_settings WHERE "tenantId"=$1 AND "moduleKey"=$2`, tenantId, mk)
		insertRow("print_settings", body)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtProductTargets(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	outletId := r.URL.Query().Get("outletId")
	dateStr := r.URL.Query().Get("targetDate")
	switch r.Method {
	case http.MethodGet:
		var rows *sql.Rows
		var err error
		if outletId != "" && dateStr != "" {
			rows, err = db.Query(`SELECT * FROM product_daily_targets WHERE "tenantId"=$1 AND "outletId"=$2 AND "targetDate"=$3 ORDER BY id ASC`, tenantId, outletId, dateStr)
		} else if outletId != "" {
			rows, err = db.Query(`SELECT * FROM product_daily_targets WHERE "tenantId"=$1 AND "outletId"=$2 ORDER BY "targetDate" DESC, id ASC`, tenantId, outletId)
		} else {
			rows, err = db.Query(`SELECT * FROM product_daily_targets WHERE "tenantId"=$1 ORDER BY "targetDate" DESC, id ASC`, tenantId)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		if _, ok := body["createdAt"]; !ok { body["createdAt"] = nowMillis() }
		id, err := insertRow("product_daily_targets", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtProductTargetsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/product-targets/")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil { jsonErr(w, 400, "invalid id"); return }
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["updatedAt"] = nowMillis()
		updateRow("product_daily_targets", id, tenantId, body)
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`DELETE FROM product_daily_targets WHERE id=$1 AND "tenantId"=$2`, id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// handleRtBmpFinancialReport — v2.20.0: Jalur 1 Only (HPP dari master_products × qty terjual)
func handleRtBmpFinancialReport(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }

	if r.Method != http.MethodGet {
		jsonErr(w, 405, "method not allowed")
		return
	}

	periodType := r.URL.Query().Get("periodType") // MONTHLY, QUARTERLY, ANNUALLY
	dateStr := strings.TrimSpace(r.URL.Query().Get("date")) // "2026-06", "2026-Q1", "2026"
	if dateStr == "" {
		jsonErr(w, 400, "date is required")
		return
	}

	var startMs, endMs int64
	loc, _ := time.LoadLocation("Asia/Jakarta")

	if periodType == "MONTHLY" {
		t, err := time.ParseInLocation("2006-01", dateStr, loc)
		if err != nil {
			jsonErr(w, 400, "invalid monthly date format, use YYYY-MM")
			return
		}
		startMs = t.UnixNano() / 1e6
		endMs = t.AddDate(0, 1, 0).UnixNano() / 1e6
	} else if periodType == "QUARTERLY" {
		parts := strings.Split(dateStr, "-Q")
		if len(parts) != 2 {
			jsonErr(w, 400, "invalid quarterly date format, use YYYY-QX")
			return
		}
		year, _ := strconv.Atoi(parts[0])
		quarter, _ := strconv.Atoi(parts[1])
		if year < 1900 || quarter < 1 || quarter > 4 {
			jsonErr(w, 400, "invalid quarter or year")
			return
		}
		monthOffset := (quarter - 1) * 3
		t := time.Date(year, time.Month(monthOffset+1), 1, 0, 0, 0, 0, loc)
		startMs = t.UnixNano() / 1e6
		endMs = t.AddDate(0, 3, 0).UnixNano() / 1e6
	} else if periodType == "ANNUALLY" {
		t, err := time.ParseInLocation("2006", dateStr, loc)
		if err != nil {
			jsonErr(w, 400, "invalid annual date format, use YYYY")
			return
		}
		startMs = t.UnixNano() / 1e6
		endMs = t.AddDate(1, 0, 0).UnixNano() / 1e6
	} else {
		jsonErr(w, 400, "invalid periodType, use MONTHLY, QUARTERLY, or ANNUALLY")
		return
	}

	// 1. Omzet & Kas Masuk dari invoices
	var omzet, totalPaid float64
	err := db.QueryRow(`
		SELECT COALESCE(SUM("totalAmount"), 0), COALESCE(SUM("paidAmount"), 0)
		FROM bmp_invoices
		WHERE "tenantId"=$1 AND "createdAt" >= $2 AND "createdAt" < $3 AND "isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&omzet, &totalPaid)
	if err != nil { jsonErr(w, 500, err.Error()); return }
	totalUnpaid := omzet - totalPaid
	if totalUnpaid < 0 { totalUnpaid = 0 }

	// 2. COGS = HPP/unit (dari bmp_master_products.hppTotalPcs, dihitung app via Jalur 1) × qty terjual
	cogs, err := updateAndCalculateCOGS(tenantId, startMs, endMs, dateStr, periodType)
	if err != nil { jsonErr(w, 500, err.Error()); return }

	labaKotor := omzet - cogs

	// 3. Beban Operasional (OPEX)
	// a. Beban Gaji Karyawan (bmp_payrolls)
	var gajiKaryawan float64
	_ = db.QueryRow(`
		SELECT COALESCE(SUM(amount), 0)
		FROM bmp_payrolls
		WHERE "tenantId"=$1 AND "paymentDate" >= $2 AND "paymentDate" < $3
	`, tenantId, startMs, endMs).Scan(&gajiKaryawan)

	// b. Biaya Pemeliharaan Mesin & Matras (bmp_maintenance_logs)
	var biayaMaintenance float64
	_ = db.QueryRow(`
		SELECT COALESCE(SUM(cost), 0)
		FROM bmp_maintenance_logs
		WHERE "tenantId"=$1 AND "maintenanceDate" >= $2 AND "maintenanceDate" < $3 AND "isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&biayaMaintenance)

	// c. Biaya Pengiriman & Upah Kuli (bmp_invoices)
	var biayaPengirimanDanKuli float64
	_ = db.QueryRow(`
		SELECT COALESCE(SUM(COALESCE("ongkirSopir", 0) + COALESCE("biayaKuli", 0)), 0)
		FROM bmp_invoices
		WHERE "tenantId"=$1 AND "createdAt" >= $2 AND "createdAt" < $3 AND "isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&biayaPengirimanDanKuli)

	// d. Beban Operasional Lainnya (bmp_cashflow + bmp_bahan_baku dengan category 'OPERASIONAL', 'LISTRIK', 'PERLENGKAPAN')
	var biayaOperasionalLain float64
	_ = db.QueryRow(`
		SELECT COALESCE(SUM("totalHarga"), 0)
		FROM bmp_bahan_baku
		WHERE "tenantId"=$1 AND "tanggal" >= $2 AND "tanggal" < $3 
		  AND ("category" = 'OPERASIONAL' OR "category" = 'LISTRIK' OR "category" = 'PERLENGKAPAN') AND "isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&biayaOperasionalLain)

	totalBebanOperasional := gajiKaryawan + biayaMaintenance + biayaPengirimanDanKuli + biayaOperasionalLain
	labaBersih := labaKotor - totalBebanOperasional

	cogsPercentage := 0.0
	marginPercentage := 0.0
	netMarginPercentage := 0.0
	if omzet > 0 {
		cogsPercentage = (cogs / omzet) * 100.0
		marginPercentage = (labaKotor / omzet) * 100.0
		netMarginPercentage = (labaBersih / omzet) * 100.0
	}

	bepNominal := 0.0
	if marginPercentage > 0 {
		bepNominal = totalBebanOperasional / (marginPercentage / 100.0)
	}

	// 4. Top Products
	type TopProduct struct {
		Name    string  `json:"name"`
		QtySold float64 `json:"qtySold"`
		Revenue float64 `json:"revenue"`
	}
	topProducts := []TopProduct{}
	rows, err := db.Query(`
		SELECT COALESCE(mp.title, bp.title, '-'), SUM(COALESCE(bp.quantity, 0)) as qty, SUM(COALESCE(bp.quantity, 0) * COALESCE(bp.price, 0)) as rev
		FROM bmp_products bp
		JOIN bmp_invoices bi ON bp."invoiceId" = bi.id
		LEFT JOIN bmp_master_products mp ON bp."masterItemID" = mp.id
		WHERE bi."tenantId"=$1 AND bi."createdAt" >= $2 AND bi."createdAt" < $3
		  AND bi."isDeleted"=FALSE AND bp."isDeleted"=FALSE
		GROUP BY COALESCE(mp.title, bp.title, '-')
		ORDER BY qty DESC
		LIMIT 5
	`, tenantId, startMs, endMs)
	if err == nil {
		defer rows.Close()
		for rows.Next() {
			var tp TopProduct
			if errS := rows.Scan(&tp.Name, &tp.QtySold, &tp.Revenue); errS == nil {
				topProducts = append(topProducts, tp)
			}
		}
	}

	// 5. Warnings
	warnings := []string{}
	var missingBomCount int
	_ = db.QueryRow(`
		SELECT COUNT(DISTINCT bp."masterItemID")
		FROM bmp_products bp
		JOIN bmp_invoices bi ON bp."invoiceId" = bi.id
		LEFT JOIN bmp_product_ingredients pin ON bp."masterItemID" = pin."productId" AND bp."tenantId" = pin."tenantId"
		WHERE bi."tenantId"=$1 AND bi."createdAt" >= $2 AND bi."createdAt" < $3
		  AND bi."isDeleted"=FALSE AND bp."isDeleted"=FALSE AND pin.id IS NULL
	`, tenantId, startMs, endMs).Scan(&missingBomCount)
	if missingBomCount > 0 {
		warnings = append(warnings, "Resep / BOM belum dilengkapi untuk sebagian produk yang terjual.")
	}

	jsonOK(w, map[string]interface{}{
		"period":                dateStr,
		"omzet":                 omzet,
		"totalPaid":             totalPaid,
		"totalUnpaid":           totalUnpaid,
		"cogs":                  cogs,
		"labaKotor":             labaKotor,
		"gajiKaryawan":           gajiKaryawan,
		"biayaMaintenance":       biayaMaintenance,
		"biayaPengirimanDanKuli": biayaPengirimanDanKuli,
		"biayaOperasionalLain":   biayaOperasionalLain,
		"totalBebanOperasional":  totalBebanOperasional,
		"labaBersih":            labaBersih,
		"cogsPercentage":        cogsPercentage,
		"marginPercentage":      marginPercentage,
		"netMarginPercentage":   netMarginPercentage,
		"bepNominal":            bepNominal,
		"topProducts":           topProducts,
		"warnings":              warnings,
	})
}

func handleRtBmpExportReport(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }

	if r.Method != http.MethodGet {
		jsonErr(w, 405, "method not allowed")
		return
	}

	periodType := r.URL.Query().Get("periodType") // MONTHLY, QUARTERLY, ANNUALLY
	dateStr := r.URL.Query().Get("date")          // "2026-06", "2026-Q1", "2026"
	if dateStr == "" {
		jsonErr(w, 400, "date is required")
		return
	}

	var startMs, endMs int64
	loc, _ := time.LoadLocation("Asia/Jakarta")

	if periodType == "MONTHLY" {
		t, _ := time.ParseInLocation("2006-01", dateStr, loc)
		startMs = t.UnixNano() / 1e6
		endMs = t.AddDate(0, 1, 0).UnixNano() / 1e6
	} else if periodType == "QUARTERLY" {
		parts := strings.Split(dateStr, "-Q")
		year, _ := strconv.Atoi(parts[0])
		quarter, _ := strconv.Atoi(parts[1])
		monthOffset := (quarter - 1) * 3
		t := time.Date(year, time.Month(monthOffset+1), 1, 0, 0, 0, 0, loc)
		startMs = t.UnixNano() / 1e6
		endMs = t.AddDate(0, 3, 0).UnixNano() / 1e6
	} else { // ANNUALLY
		t, _ := time.ParseInLocation("2006", dateStr, loc)
		startMs = t.UnixNano() / 1e6
		endMs = t.AddDate(1, 0, 0).UnixNano() / 1e6
	}

	// Fetch Summary Metrics
	var omzet, totalPaid float64
	_ = db.QueryRow(`
		SELECT COALESCE(SUM("totalAmount"), 0), COALESCE(SUM("paidAmount"), 0) FROM bmp_invoices 
		WHERE "tenantId"=$1 AND "createdAt" >= $2 AND "createdAt" < $3 AND "isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&omzet, &totalPaid)
	totalUnpaid := omzet - totalPaid
	if totalUnpaid < 0 { totalUnpaid = 0 }

	cogs, _ := updateAndCalculateCOGS(tenantId, startMs, endMs, dateStr, periodType)
	labaKotor := omzet - cogs

	var gajiKaryawan float64
	_ = db.QueryRow(`SELECT COALESCE(SUM(amount), 0) FROM bmp_payrolls WHERE "tenantId"=$1 AND "paymentDate" >= $2 AND "paymentDate" < $3`, tenantId, startMs, endMs).Scan(&gajiKaryawan)

	var biayaMaintenance float64
	_ = db.QueryRow(`SELECT COALESCE(SUM(cost), 0) FROM bmp_maintenance_logs WHERE "tenantId"=$1 AND "maintenanceDate" >= $2 AND "maintenanceDate" < $3 AND "isDeleted"=FALSE`, tenantId, startMs, endMs).Scan(&biayaMaintenance)

	var biayaOperasionalLain float64
	_ = db.QueryRow(`
		SELECT COALESCE(SUM("totalHarga"), 0) 
		FROM bmp_bahan_baku 
		WHERE "tenantId"=$1 AND "tanggal" >= $2 AND "tanggal" < $3 
		  AND ("category" = 'OPERASIONAL' OR "category" = 'LISTRIK' OR "category" = 'PERLENGKAPAN') AND "isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&biayaOperasionalLain)

	totalBebanOperasional := gajiKaryawan + biayaMaintenance + biayaOperasionalLain
	labaBersih := labaKotor - totalBebanOperasional

	// Ambil Nama Perusahaan
	var companyName string
	_ = db.QueryRow(`SELECT COALESCE("clientName", 'POSBah Manufaktur') FROM bmp_settings WHERE "tenantId"=$1 LIMIT 1`, tenantId).Scan(&companyName)
	if companyName == "" { companyName = "POSBah Invoice & Manufaktur" }

	// Set CSV Headers
	w.Header().Set("Content-Type", "text/csv; charset=utf-8")
	filename := fmt.Sprintf("Laporan_Keuangan_%s_%s.csv", strings.ReplaceAll(companyName, " ", "_"), dateStr)
	w.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=%s", filename))

	writer := csv.NewWriter(w)
	writer.Comma = ';' // Titik koma agar Excel regional Indonesia langsung memisah kolom

	// BOM untuk UTF-8
	_, _ = w.Write([]byte{0xEF, 0xBB, 0xBF})

	// 1. Header Ringkasan Keuangan
	_ = writer.Write([]string{"LAPORAN KEUANGAN & LABA RUGI KOMPREHENSIF"})
	_ = writer.Write([]string{"Perusahaan", companyName})
	_ = writer.Write([]string{"Periode", dateStr})
	_ = writer.Write([]string{"Tipe Laporan", periodType})
	_ = writer.Write([]string{"Tanggal Cetak", time.Now().In(loc).Format("2006-01-02 15:04:05")})
	_ = writer.Write([]string{""})

	_ = writer.Write([]string{"IKHTISAR LABA RUGI"})
	_ = writer.Write([]string{"Pos Keuangan", "Nominal (Rupiah)", "Keterangan"})
	_ = writer.Write([]string{"OMZET PENJUALAN (Faktur Diterbitkan)", fmt.Sprintf("%.2f", omzet), "Total seluruh tagihan penjualan"})
	_ = writer.Write([]string{"- Kas Riil Diterima (Cash In)", fmt.Sprintf("%.2f", totalPaid), "Telah cair ke rekening / kas"})
	_ = writer.Write([]string{"- Sisa Piutang Usaha (AR)", fmt.Sprintf("%.2f", totalUnpaid), "Tagihan belum lunas"})
	_ = writer.Write([]string{"HARGA POKOK PENJUALAN (COGS / HPP)", fmt.Sprintf("%.2f", cogs), "Biaya bahan baku langsung"})
	_ = writer.Write([]string{"LABA KOTOR (Gross Profit)", fmt.Sprintf("%.2f", labaKotor), "Omzet - HPP"})
	_ = writer.Write([]string{""})
	_ = writer.Write([]string{"BEBAN OPERASIONAL (OPEX)"})
	_ = writer.Write([]string{"- Beban Gaji Karyawan", fmt.Sprintf("%.2f", gajiKaryawan), "Total payroll karyawan"})
	_ = writer.Write([]string{"- Beban Pemeliharaan Mesin & Matras", fmt.Sprintf("%.2f", biayaMaintenance), "Biaya servis & perbaikan aset"})
	_ = writer.Write([]string{"- Beban Operasional Lainnya", fmt.Sprintf("%.2f", biayaOperasionalLain), "Biaya operasional pabrik"})
	_ = writer.Write([]string{"TOTAL BEBAN OPERASIONAL", fmt.Sprintf("%.2f", totalBebanOperasional), "Total beban operasional"})
	_ = writer.Write([]string{""})
	_ = writer.Write([]string{"LABA BERSIH (NET PROFIT)", fmt.Sprintf("%.2f", labaBersih), "Laba Kotor - Beban Operasional"})
	_ = writer.Write([]string{""})
	_ = writer.Write([]string{""})

	// 2. Jurnal Penjualan
	_ = writer.Write([]string{"DETAIL JURNAL PENJUALAN (INVOICES)"})
	_ = writer.Write([]string{"ID", "Nomor Invoice", "Nama Pelanggan", "Tanggal Faktur", "Jatuh Tempo", "Total Tagihan", "Telah Dibayar", "Sisa Piutang", "Status"})
	
	rowsInv, errInv := db.Query(`
		SELECT bi.id, bi.number, COALESCE(bc."clientName", '-'), bi."createdAt", bi."dueDate", bi."totalAmount", bi."paidAmount", bi.status
		FROM bmp_invoices bi
		LEFT JOIN bmp_clients bc ON bi."clientId" = bc.id
		WHERE bi."tenantId"=$1 AND bi."createdAt" >= $2 AND bi."createdAt" < $3 AND bi."isDeleted"=FALSE
		ORDER BY bi.id ASC
	`, tenantId, startMs, endMs)
	
	if errInv == nil {
		defer rowsInv.Close()
		for rowsInv.Next() {
			var id int64
			var number, clientName, status string
			var createdAt, dueDate int64
			var totalAmt, paidAmt float64
			if errS := rowsInv.Scan(&id, &number, &clientName, &createdAt, &dueDate, &totalAmt, &paidAmt, &status); errS == nil {
				createdDate := time.Unix(createdAt/1000, 0).In(loc).Format("2006-01-02 15:04")
				dueDateStr := "-"
				if dueDate > 0 {
					dueDateStr = time.Unix(dueDate/1000, 0).In(loc).Format("2006-01-02")
				}
				sisa := totalAmt - paidAmt
				if sisa < 0 { sisa = 0 }
				_ = writer.Write([]string{
					strconv.FormatInt(id, 10),
					number,
					clientName,
					createdDate,
					dueDateStr,
					fmt.Sprintf("%.2f", totalAmt),
					fmt.Sprintf("%.2f", paidAmt),
					fmt.Sprintf("%.2f", sisa),
					status,
				})
			}
		}
	}
	_ = writer.Write([]string{""})
	_ = writer.Write([]string{""})

	// 3. Detail Pembayaran Gaji Karyawan
	_ = writer.Write([]string{"DETAIL RIWAYAT PENGGAJIAN KARYAWAN (PAYROLLS)"})
	_ = writer.Write([]string{"ID Payroll", "Nama Karyawan", "Tanggal Bayar", "Jumlah Gaji", "Keterangan"})
	rowsPay, errPay := db.Query(`
		SELECT bp.id, COALESCE(be.name, 'Karyawan #' || bp."employeeId"), bp."paymentDate", bp.amount, COALESCE(bp.description, '-')
		FROM bmp_payrolls bp
		LEFT JOIN bmp_employees be ON bp."employeeId" = be.id AND bp."tenantId" = be."tenantId"
		WHERE bp."tenantId"=$1 AND bp."paymentDate" >= $2 AND bp."paymentDate" < $3
		ORDER BY bp."paymentDate" ASC
	`, tenantId, startMs, endMs)
	if errPay == nil {
		defer rowsPay.Close()
		for rowsPay.Next() {
			var pid, empName, desc string
			var pDate int64
			var amt float64
			if errS := rowsPay.Scan(&pid, &empName, &pDate, &amt, &desc); errS == nil {
				dateFormatted := time.Unix(pDate/1000, 0).In(loc).Format("2006-01-02 15:04")
				_ = writer.Write([]string{pid, empName, dateFormatted, fmt.Sprintf("%.2f", amt), desc})
			}
		}
	}
	_ = writer.Write([]string{""})
	_ = writer.Write([]string{""})

	// 4. Detail Log Pemeliharaan Mesin & Matras
	_ = writer.Write([]string{"DETAIL LOG PEMELIHARAAN MESIN & MATRAS (MAINTENANCE)"})
	_ = writer.Write([]string{"ID Log", "Jenis Aset", "Nama Aset", "Tanggal Servis", "Jenis Servis", "Biaya Servis", "Teknisi", "Catatan"})
	rowsMaint, errMaint := db.Query(`
		SELECT id, "assetType", COALESCE("assetName", '-'), "maintenanceDate", "serviceType", cost, COALESCE("technicianName", '-'), COALESCE(notes, '-')
		FROM bmp_maintenance_logs
		WHERE "tenantId"=$1 AND "maintenanceDate" >= $2 AND "maintenanceDate" < $3 AND "isDeleted"=FALSE
		ORDER BY "maintenanceDate" ASC
	`, tenantId, startMs, endMs)
	if errMaint == nil {
		defer rowsMaint.Close()
		for rowsMaint.Next() {
			var mid int64
			var aType, aName, sType, tech, notes string
			var mDate int64
			var cost float64
			if errS := rowsMaint.Scan(&mid, &aType, &aName, &mDate, &sType, &cost, &tech, &notes); errS == nil {
				dateFormatted := time.Unix(mDate/1000, 0).In(loc).Format("2006-01-02")
				_ = writer.Write([]string{
					strconv.FormatInt(mid, 10),
					aType,
					aName,
					dateFormatted,
					sType,
					fmt.Sprintf("%.2f", cost),
					tech,
					notes,
				})
			}
		}
	}

	writer.Flush()
}

func handleRtBmpSuppliers(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if r.Method != http.MethodGet {
		jsonErr(w, 405, "method not allowed")
		return
	}
	rows, err := db.Query(`
		SELECT DISTINCT supplier 
		FROM bmp_bahan_baku 
		WHERE "tenantId"=$1 AND "isDeleted"=FALSE AND supplier IS NOT NULL AND TRIM(supplier) != ''
		ORDER BY supplier ASC
	`, tenantId)
	if err != nil { jsonErr(w, 500, err.Error()); return }
	defer rows.Close()
	
	list := []string{}
	for rows.Next() {
		var s string
		if errS := rows.Scan(&s); errS == nil {
			list = append(list, s)
		}
	}
	jsonOK(w, list)
}

// isPeriodWithinAssetLife, autoCalculateDepreciation, handleRtBmpDepreciation,
// handleRtBmpAssets, handleRtBmpAssetsById dihapus (Jalur 2 removed — v2.20.0)



func handleRtBmpIngredients(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		productIdStr := r.URL.Query().Get("productId")
		if productIdStr == "" {
			jsonErr(w, 400, "productId is required")
			return
		}
		productId, _ := strconv.ParseInt(productIdStr, 10, 64)
		rows, err := db.Query(`SELECT * FROM bmp_product_ingredients WHERE "tenantId"=$1 AND "productId"=$2 ORDER BY "jenisBahan" ASC`, tenantId, productId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid body")
			return
		}
		body["tenantId"] = tenantId
		body["createdAt"] = nowMillis()
		body["updatedAt"] = nowMillis()
		id, err := insertRow("bmp_product_ingredients", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpIngredientsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/ingredients/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid body")
			return
		}
		body["updatedAt"] = nowMillis()
		err := updateRow("bmp_product_ingredients", id, tenantId, body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		_, err := db.Exec(`DELETE FROM bmp_product_ingredients WHERE id=$1 AND "tenantId"=$2`, id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpProductionMaterials(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		productionLogIdStr := r.URL.Query().Get("productionLogId")
		if productionLogIdStr == "" {
			jsonErr(w, 400, "productionLogId is required")
			return
		}
		productionLogId, _ := strconv.ParseInt(productionLogIdStr, 10, 64)
		rows, err := db.Query(`SELECT * FROM bmp_production_materials WHERE "tenantId"=$1 AND "productionLogId"=$2 AND "isDeleted"=FALSE ORDER BY "id" ASC`, tenantId, productionLogId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid body")
			return
		}
		body["tenantId"] = tenantId
		body["createdAt"] = nowMillis()
		body["updatedAt"] = nowMillis()
		body["isDeleted"] = false
		id, err := insertRow("bmp_production_materials", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpProductionMaterialsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/production-materials/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid body")
			return
		}
		body["updatedAt"] = nowMillis()
		err := updateRow("bmp_production_materials", id, tenantId, body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_production_materials SET "isDeleted"=TRUE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func getPeriodRangeFromMs(ms int64) (int64, int64, string) {
	loc, _ := time.LoadLocation("Asia/Jakarta")
	t := time.Unix(ms/1000, (ms%1000)*1e6).In(loc)
	dateStr := t.Format("2006-01")
	startT := time.Date(t.Year(), t.Month(), 1, 0, 0, 0, 0, loc)
	endT := startT.AddDate(0, 1, 0)
	return startT.UnixNano() / 1e6, endT.UnixNano() / 1e6, dateStr
}

func triggerProductionLogCompletion(tenantId string, logId int64) error {
	var masterProductId int64
	var quantityProduced float64
	var productionDate int64
	var cycleTimeActual float64
	var electricityCostActual float64
	var rawMaterialIdParam int64
	var colorMixture sql.NullString

	err := db.QueryRow(`
		SELECT "masterProductId", "quantityProduced", "productionDate",
		       COALESCE(cycle_time_actual, 0), COALESCE(electricity_cost_actual, 0),
		       COALESCE("rawMaterialId", 0), COALESCE(color_mixture, '')
		FROM bmp_production_logs 
		WHERE id=$1 AND "tenantId"=$2 AND "isDeleted"=FALSE
	`, logId, tenantId).Scan(&masterProductId, &quantityProduced, &productionDate, &cycleTimeActual, &electricityCostActual, &rawMaterialIdParam, &colorMixture)
	if err != nil {
		return err
	}

	// v2.19.17: Jika cycle_time_actual di-isi user, snapshot ke master product
	// agar COGS recalc menggunakan nilai aktual dari shift ini
	if cycleTimeActual > 0 {
		_, _ = db.Exec(`UPDATE bmp_master_products SET "cycleTime"=$1 WHERE id=$2 AND "tenantId"=$3`,
			cycleTimeActual, masterProductId, tenantId)
		log.Printf("[HPP] Cycle time updated for masterProduct %d: %.2fs (from log %d)", masterProductId, cycleTimeActual, logId)
	}

	// v2.19.24: Auto-increment mold usage_count saat log produksi disimpan
	// Hitung total shots = quantityProduced + quantityRejected (keduanya pakai cetakan)
	{
		var moldIdForLog sql.NullInt64
		var machineIdForLog sql.NullInt64
		var quantityRejectedForLog float64
		var workOrderIdForLog sql.NullInt64
		_ = db.QueryRow(`
			SELECT COALESCE(pl."mold_id", m.mold_id), pl."machineId", COALESCE(pl."quantityRejected", 0), pl."workOrderId"
			FROM bmp_production_logs pl
			LEFT JOIN bmp_machines m ON pl."machineId" = m.id AND m."isDeleted" = FALSE
			WHERE pl.id = $1 AND pl."tenantId" = $2
		`, logId, tenantId).Scan(&moldIdForLog, &machineIdForLog, &quantityRejectedForLog, &workOrderIdForLog)

		totalShots := quantityProduced + quantityRejectedForLog

		if moldIdForLog.Valid && moldIdForLog.Int64 > 0 && totalShots > 0 {
			_, _ = db.Exec(`
				UPDATE bmp_molds SET usage_count = COALESCE(usage_count, 0) + $1
				WHERE id = $2 AND "tenantId" = $3 AND "isDeleted" = FALSE
			`, totalShots, moldIdForLog.Int64, tenantId)
			log.Printf("[MOLD] usage_count +%.0f for mold_id=%d (log=%d)", totalShots, moldIdForLog.Int64, logId)
		}

		// Update SPK / Work Order progress if linked
		if workOrderIdForLog.Valid && workOrderIdForLog.Int64 > 0 {
			now := nowMillis()
			_, _ = db.Exec(`
				UPDATE bmp_work_orders 
				SET "completedQuantity" = COALESCE("completedQuantity", 0) + $1,
				    "rejectedQuantity" = COALESCE("rejectedQuantity", 0) + $2,
				    "status" = CASE 
				        WHEN COALESCE("completedQuantity", 0) + $1 >= "targetQuantity" THEN 'COMPLETED'
				        ELSE 'IN_PROGRESS'
				    END,
				    "actualCompletionDate" = CASE
				        WHEN COALESCE("completedQuantity", 0) + $1 >= "targetQuantity" THEN $3
				        ELSE "actualCompletionDate"
				    END,
				    "updatedAt" = $3
				WHERE id = $4 AND "tenantId" = $5 AND "isDeleted" = FALSE
			`, quantityProduced, quantityRejectedForLog, now, workOrderIdForLog.Int64, tenantId)
			log.Printf("[SPK] Work order %d progress updated: +%.0f completed (log=%d)", workOrderIdForLog.Int64, quantityProduced, logId)
		}

		// Update Machine operating hours
		if machineIdForLog.Valid && machineIdForLog.Int64 > 0 && cycleTimeActual > 0 && totalShots > 0 {
			hoursRun := (totalShots * cycleTimeActual) / 3600.0
			if hoursRun > 0 {
				_, _ = db.Exec(`
					UPDATE bmp_machines 
					SET "total_operating_hours" = COALESCE("total_operating_hours", 0) + $1,
					    "updatedAt" = $2
					WHERE id = $3 AND "tenantId" = $4 AND "isDeleted" = FALSE
				`, hoursRun, nowMillis(), machineIdForLog.Int64, tenantId)
			}
		}
	}

	// v2.19.22: Parse color_mixture JSON to extract raw material batches for mixtures
	type ColorMixEntry struct {
		Color         string  `json:"color"`
		Rasio         string  `json:"rasio"`
		RawMaterialId *int64  `json:"raw_material_id"`
	}
	var mixEntries []ColorMixEntry
	if colorMixture.Valid && colorMixture.String != "" {
		_ = json.Unmarshal([]byte(colorMixture.String), &mixEntries)
	}

	var validMix []ColorMixEntry
	var totalRatio float64 = 0.0
	for _, entry := range mixEntries {
		if entry.RawMaterialId != nil && *entry.RawMaterialId > 0 {
			ratio, errR := strconv.ParseFloat(entry.Rasio, 64)
			if errR == nil && ratio > 0 {
				validMix = append(validMix, entry)
				totalRatio += ratio
			}
		}
	}

	// 1. Clear any existing materials for this log
	_, _ = db.Exec(`UPDATE bmp_production_materials SET "isDeleted"=TRUE WHERE "productionLogId"=$1 AND "tenantId"=$2`, logId, tenantId)

	// 2. Query ingredients (BOM)
	rows, err := db.Query(`
		SELECT "jenisBahan", quantity 
		FROM bmp_product_ingredients 
		WHERE "productId"=$1 AND "tenantId"=$2
	`, masterProductId, tenantId)
	if err != nil {
		return err
	}
	defer rows.Close()

	for rows.Next() {
		var jenisBahan string
		var bomQty float64
		if errS := rows.Scan(&jenisBahan, &bomQty); errS == nil {
			qtyUsed := bomQty * quantityProduced
			
			if len(validMix) > 0 && totalRatio > 0 {
				// Proportional allocation based on mixture ratio & batches
				for _, mix := range validMix {
					ratio, _ := strconv.ParseFloat(mix.Rasio, 64)
					partQty := qtyUsed * (ratio / totalRatio)
					
					// Find item id matching bahanBakuId and jenisBahan
					var itemRawMaterialId int64 = 0
					_ = db.QueryRow(`
						SELECT id FROM bmp_bahan_baku_item 
						WHERE "bahanBakuId"=$1 AND "jenisBahan"=$2 AND "isDeleted"=FALSE
						LIMIT 1
					`, *mix.RawMaterialId, jenisBahan).Scan(&itemRawMaterialId)
					
					// Fallback to latest active purchase if not found in this specific batch
					if itemRawMaterialId == 0 {
						_ = db.QueryRow(`
							SELECT bbi.id 
							FROM bmp_bahan_baku_item bbi 
							JOIN bmp_bahan_baku bb ON bbi."bahanBakuId" = bb.id AND bbi."tenantId" = bb."tenantId" 
							WHERE bb."tenantId"=$1 AND bbi."jenisBahan"=$2 AND bb."isDeleted"=FALSE AND bbi."isDeleted"=FALSE 
							ORDER BY bb.tanggal DESC, bbi.id DESC 
							LIMIT 1
						`, tenantId, jenisBahan).Scan(&itemRawMaterialId)
					}
					
					if itemRawMaterialId > 0 && partQty > 0 {
						_, _ = db.Exec(`
							INSERT INTO bmp_production_materials ("tenantId", "productionLogId", "rawMaterialId", "jenisBahan", "quantityUsed", "createdAt", "updatedAt")
							VALUES ($1, $2, $3, $4, $5, $6, $6)
							ON CONFLICT ("tenantId", "productionLogId", "rawMaterialId") 
							DO UPDATE SET "quantityUsed"=$5, "isDeleted"=FALSE, "updatedAt"=$6
						`, tenantId, logId, itemRawMaterialId, jenisBahan, partQty, nowMillis())
					}
				}
			} else {
				// Default behavior: use global rawMaterialId or latest purchase fallback
				var itemRawMaterialId int64 = 0
				if rawMaterialIdParam > 0 {
					_ = db.QueryRow(`
						SELECT id FROM bmp_bahan_baku_item 
						WHERE "bahanBakuId"=$1 AND "jenisBahan"=$2 AND "isDeleted"=FALSE
						LIMIT 1
					`, rawMaterialIdParam, jenisBahan).Scan(&itemRawMaterialId)
				}
				if itemRawMaterialId == 0 {
					_ = db.QueryRow(`
						SELECT bbi.id 
						FROM bmp_bahan_baku_item bbi 
						JOIN bmp_bahan_baku bb ON bbi."bahanBakuId" = bb.id AND bbi."tenantId" = bb."tenantId" 
						WHERE bb."tenantId"=$1 AND bbi."jenisBahan"=$2 AND bb."isDeleted"=FALSE AND bbi."isDeleted"=FALSE 
						ORDER BY bb.tanggal DESC, bbi.id DESC 
						LIMIT 1
					`, tenantId, jenisBahan).Scan(&itemRawMaterialId)
				}
				
				if itemRawMaterialId > 0 && qtyUsed > 0 {
					_, _ = db.Exec(`
						INSERT INTO bmp_production_materials ("tenantId", "productionLogId", "rawMaterialId", "jenisBahan", "quantityUsed", "createdAt", "updatedAt")
						VALUES ($1, $2, $3, $4, $5, $6, $6)
						ON CONFLICT ("tenantId", "productionLogId", "rawMaterialId") 
						DO UPDATE SET "quantityUsed"=$5, "isDeleted"=FALSE, "updatedAt"=$6
					`, tenantId, logId, itemRawMaterialId, jenisBahan, qtyUsed, nowMillis())
				}
			}
		}
	}

	// 3. Recalculate HPP for the period
	startMs, endMs, dateStr := getPeriodRangeFromMs(productionDate)
	_, _ = updateAndCalculateCOGS(tenantId, startMs, endMs, dateStr, "MONTHLY")

	// 4. Fetch the computed HPP
	var hppTotal float64
	errHpp := db.QueryRow(`SELECT "hppTotalPcs" FROM bmp_master_products WHERE id=$1 AND "tenantId"=$2`, masterProductId, tenantId).Scan(&hppTotal)
	if errHpp == nil && hppTotal > 0 {
		// Snapshot HPP to invoice products
		_, _ = db.Exec(`
			UPDATE bmp_products bp 
			SET "hargaBeli" = $1 
			FROM bmp_invoices bi 
			WHERE bp."invoiceId" = bi.id AND bp."masterItemID" = $2 AND bp."tenantId" = $3 
			  AND bi."createdAt" >= $4 AND bi."createdAt" < $5 
			  AND bi."isDeleted" = FALSE AND bp."isDeleted" = FALSE
		`, hppTotal, masterProductId, tenantId, startMs, endMs)
	}

	log.Printf("[HPP] Completed for masterProduct %d: hppTotalPcs=%.2f cycleTime=%.2fs elec=%.0f", masterProductId, hppTotal, cycleTimeActual, electricityCostActual)
	return nil
}


// updateAndCalculateCOGS — v2.20.0: Jalur 1 Only
// COGS = SUM(qty_terjual × hppTotalPcs dari bmp_master_products)
// HPP per unit sudah dihitung di sisi app via bmp_settings dan disimpan ke bmp_master_products.
func updateAndCalculateCOGS(tenantId string, startMs int64, endMs int64, dateStr string, periodType string) (float64, error) {
	var totalCogs float64
	err := db.QueryRow(`
		SELECT COALESCE(SUM(COALESCE(bp.quantity, 0.0) * COALESCE(mp."hppTotalPcs", 0.0)), 0.0)
		FROM bmp_products bp
		JOIN bmp_invoices bi ON bp."invoiceId" = bi.id
		LEFT JOIN bmp_master_products mp ON bp."masterItemID" = mp.id
		WHERE bi."tenantId"=$1 AND bi."createdAt" >= $2 AND bi."createdAt" < $3
		  AND bi."isDeleted"=FALSE AND bp."isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&totalCogs)
	if err != nil {
		return 0, err
	}
	return totalCogs, nil
}

// ── BMP — machines ─────────────────────────────────────────────────────────────


func handleRtBmpMachines(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_machines WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY id ASC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		if _, ok := body["createdAt"]; !ok { body["createdAt"] = nowMillis() }
		id, err := insertRow("bmp_machines", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpMachinesById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/machines/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["updatedAt"] = nowMillis()
		updateRow("bmp_machines", id, tenantId, body); jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE bmp_machines SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — molds ────────────────────────────────────────────────────────────────

func handleRtBmpMolds(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_molds WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY id ASC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		if _, ok := body["createdAt"]; !ok { body["createdAt"] = nowMillis() }
		id, err := insertRow("bmp_molds", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpMoldsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/molds/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["updatedAt"] = nowMillis()
		updateRow("bmp_molds", id, tenantId, body); jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE bmp_molds SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — Price Tracking (v2.19.26) ─────────────────────────────────────────

// handleRtBmpClientPrices: GET harga jual semua produk ke semua klien (untuk halaman Lacak Harga)
// Response: list {clientId, clientName, masterItemID, productName, highestPrice, latestPrice, latestPurchaseDate}
func handleRtBmpClientPrices(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if r.Method != http.MethodGet { jsonErr(w, 405, "method not allowed"); return }

	rows, err := db.Query(`
		SELECT 
			i."clientId",
			c."clientName",
			p."masterItemID",
			mp.title AS "productName",
			MAX(p.price) AS "highestPrice",
			(SELECT p2.price FROM bmp_products p2 
			 JOIN bmp_invoices i2 ON p2."invoiceId" = i2.id 
			 WHERE i2."clientId" = i."clientId" AND p2."masterItemID" = p."masterItemID" 
			   AND i2."isDeleted" = FALSE AND p2."isDeleted" = FALSE 
			 ORDER BY i2."createdAt" DESC LIMIT 1) AS "latestPrice",
			MAX(i."createdAt") AS "latestPurchaseDate"
		FROM bmp_products p
		JOIN bmp_invoices i ON p."invoiceId" = i.id
		JOIN bmp_clients c ON i."clientId" = c.id
		JOIN bmp_master_products mp ON p."masterItemID" = mp.id
		WHERE i."isDeleted" = FALSE AND p."isDeleted" = FALSE AND i."tenantId" = $1
		  AND p."masterItemID" IS NOT NULL AND p."masterItemID" > 0
		GROUP BY i."clientId", c."clientName", p."masterItemID", mp.title
		ORDER BY mp.title ASC, c."clientName" ASC
	`, tenantId)
	if err != nil { jsonErr(w, 500, err.Error()); return }
	defer rows.Close()
	jsonOK(w, rowsToJSON(rows))
}

// handleRtBmpClientLatestPrices: GET harga terakhir 1 klien untuk semua produk (untuk saran harga di invoice form)
// URL: /api/rt/bmp/clients/latest-prices/{clientId}
// Response: list {masterItemID, latestPrice, purchaseDate}  — DISTINCT ON masterItemID, newest first
func handleRtBmpClientLatestPrices(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if r.Method != http.MethodGet { jsonErr(w, 405, "method not allowed"); return }

	clientIdStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/clients/latest-prices/")
	clientId, _ := strconv.ParseInt(clientIdStr, 10, 64)
	if clientId == 0 { jsonErr(w, 400, "invalid client id"); return }

	rows, err := db.Query(`
		SELECT DISTINCT ON (p."masterItemID")
			p."masterItemID",
			p.price AS "latestPrice",
			i."createdAt" AS "purchaseDate"
		FROM bmp_products p
		JOIN bmp_invoices i ON p."invoiceId" = i.id
		WHERE i."clientId" = $1 
		  AND i."tenantId" = $2 
		  AND i."isDeleted" = FALSE 
		  AND p."isDeleted" = FALSE
		  AND p."masterItemID" IS NOT NULL AND p."masterItemID" > 0
		ORDER BY p."masterItemID", i."createdAt" DESC
	`, clientId, tenantId)
	if err != nil { jsonErr(w, 500, err.Error()); return }
	defer rows.Close()
	jsonOK(w, rowsToJSON(rows))
}

// ─── Helper type converters (untuk data dari JSON body interface{}) ──────────

// toLong: konversi interface{} ke int64 (JSON numbers decoded sebagai float64)
func toLong(v interface{}) int64 {
	switch val := v.(type) {
	case float64:
		return int64(val)
	case float32:
		return int64(val)
	case int64:
		return val
	case int:
		return int64(val)
	}
	return 0
}

// toFloat: konversi interface{} ke float64
func toFloat(v interface{}) float64 {
	switch val := v.(type) {
	case float64:
		return val
	case float32:
		return float64(val)
	case int64:
		return float64(val)
	case int:
		return float64(val)
	}
	return 0.0
}

// toBool: konversi interface{} ke bool
func toBool(v interface{}) bool {
	if val, ok := v.(bool); ok {
		return val
	}
	return false
}

// ─── Telemetri Memori Perangkat ───────────────────────────────────────────────

// handleRtBmpTelemetryMemory: Menyimpan log memori perangkat dari APK ke database.
// APK mengirim data ini sesaat sebelum cetak JPG agar kita bisa memantau
// kondisi HP user secara terpusat di server.
func handleRtBmpTelemetryMemory(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if r.Method != http.MethodPost { jsonErr(w, 405, "method not allowed"); return }

	var body map[string]interface{}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		jsonErr(w, 400, "invalid body")
		return
	}

	_, err := db.Exec(`
		INSERT INTO "bmp_device_memory_logs" (
			"tenantId", "deviceModel", "androidVersion", "totalMemoryMb",
			"availableMemoryMb", "isLowMemory", "selectedScale", "invoiceNumber", "createdAt"
		) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
	`,
		tenantId,
		body["deviceModel"],
		body["androidVersion"],
		toLong(body["totalMemoryMb"]),
		toLong(body["availableMemoryMb"]),
		toBool(body["isLowMemory"]),
		toFloat(body["selectedScale"]),
		body["invoiceNumber"],
		nowMillis(),
	)
	if err != nil {
		jsonErr(w, 500, err.Error())
		return
	}
	jsonOK(w, map[string]interface{}{"ok": true})
}

// handleRtClearAllEmployees: Menghapus seluruh data karyawan (POS / Invoice) untuk tenant aktif
func handleRtClearAllEmployees(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }

	_, err := db.Exec(`DELETE FROM "employees" WHERE "tenantId" = $1`, tenantId)
	if err != nil { jsonErr(w, 500, err.Error()); return }
	_, _ = db.Exec(`DELETE FROM "local_users" WHERE "tenantId" = $1 AND "role" != 'OWNER'`, tenantId)

	jsonOK(w, map[string]interface{}{"ok": true, "message": "Semua data karyawan POS/Invoice telah dibersihkan."})
}

// handleRtClearAllBmpEmployees: Menghapus seluruh data karyawan (BMP / Manufaktur) untuk tenant aktif
func handleRtClearAllBmpEmployees(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }

	_, err := db.Exec(`DELETE FROM "bmp_employees" WHERE "tenantId" = $1`, tenantId)
	if err != nil { jsonErr(w, 500, err.Error()); return }

	jsonOK(w, map[string]interface{}{"ok": true, "message": "Semua data karyawan Manufaktur/BMP telah dibersihkan."})
}

// ── BMP — Work Orders / SPK (v2.19.58) ────────────────────────────────────────

func handleRtBmpWorkOrders(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if !checkManagerOrOwner(w, r) { return }
	switch r.Method {
	case http.MethodGet:
		rows, err := db.Query(`SELECT * FROM bmp_work_orders WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "startDate" DESC, id DESC`, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["tenantId"] = tenantId
		now := nowMillis()
		if _, ok := body["startDate"]; !ok {
			body["startDate"] = now
		}
		if _, ok := body["createdAt"]; !ok {
			body["createdAt"] = now
		}
		body["updatedAt"] = now
		if _, ok := body["status"]; !ok {
			body["status"] = "PENDING"
		}
		if _, ok := body["priority"]; !ok {
			body["priority"] = "NORMAL"
		}
		if pName, ok := body["masterProductName"].(string); (!ok || strings.TrimSpace(pName) == "") {
			if mpid, ok := body["masterProductId"]; ok {
				var pTitle string
				_ = db.QueryRow(`SELECT "title" FROM "bmp_master_products" WHERE id=$1 AND "tenantId"=$2`, mpid, tenantId).Scan(&pTitle)
				if pTitle != "" {
					body["masterProductName"] = pTitle
				}
			}
		}

		id, err := insertRow("bmp_work_orders", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpWorkOrdersById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if !checkManagerOrOwner(w, r) { return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/work-orders/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["updatedAt"] = nowMillis()
		err := updateRow("bmp_work_orders", id, tenantId, body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_work_orders SET "isDeleted"=TRUE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — Preventive Maintenance Logs (v2.19.58) ──────────────────────────────

func handleRtBmpMaintenanceLogs(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if !checkManagerOrOwner(w, r) { return }
	switch r.Method {
	case http.MethodGet:
		rows, err := db.Query(`SELECT * FROM bmp_maintenance_logs WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "maintenanceDate" DESC, id DESC`, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["tenantId"] = tenantId
		now := nowMillis()
		if _, ok := body["maintenanceDate"]; !ok {
			body["maintenanceDate"] = now
		}
		if _, ok := body["createdAt"]; !ok {
			body["createdAt"] = now
		}
		body["updatedAt"] = now
		body["isDeleted"] = false

		id, err := insertRow("bmp_maintenance_logs", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		// Reset maintenance threshold based on assetType
		assetType, _ := body["assetType"].(string)
		assetTypeUpper := strings.ToUpper(strings.TrimSpace(assetType))
		var assetId int64
		if aIdVal, ok := body["assetId"]; ok {
			switch v := aIdVal.(type) {
			case float64: assetId = int64(v)
			case int64: assetId = v
			case int: assetId = int64(v)
			case string: assetId, _ = strconv.ParseInt(v, 10, 64)
			}
		}

		if assetTypeUpper == "MOLD" && assetId > 0 {
			_, _ = db.Exec(`
				UPDATE bmp_molds 
				SET "last_maintenance_shots" = COALESCE("usage_count", 0), "updatedAt" = $1 
				WHERE id = $2 AND "tenantId" = $3
			`, now, assetId, tenantId)
		} else if assetTypeUpper == "MACHINE" && assetId > 0 {
			_, _ = db.Exec(`
				UPDATE bmp_machines 
				SET "last_maintenance_hours" = COALESCE("total_operating_hours", 0), "updatedAt" = $1 
				WHERE id = $2 AND "tenantId" = $3
			`, now, assetId, tenantId)
		}

		// If cost > 0 and recordedToCashflow, insert into bmp_bahan_baku as operational expense
		var cost float64
		if cVal, ok := body["cost"]; ok {
			switch v := cVal.(type) {
			case float64: cost = v
			case int64: cost = float64(v)
			case int: cost = float64(v)
			case string: cost, _ = strconv.ParseFloat(v, 64)
			}
		}
		recToCashflow, hasRec := body["recordedToCashflow"].(bool)
		if (!hasRec || recToCashflow) && cost > 0 {
			assetName, _ := body["assetName"].(string)
			if assetName == "" { assetName = fmt.Sprintf("%s #%d", assetTypeUpper, assetId) }
			serviceType, _ := body["serviceType"].(string)
			bbBody := map[string]interface{}{
				"tenantId":   tenantId,
				"tanggal":    body["maintenanceDate"],
				"noTagihan":  fmt.Sprintf("SRV-%d-%d", assetId, now % 100000),
				"supplier":   fmt.Sprintf("Servis %s", assetName),
				"category":   "PERLENGKAPAN",
				"totalHarga": cost,
				"nominal":    cost,
				"notes":      fmt.Sprintf("Biaya Pemeliharaan/Servis (%s): %s", serviceType, assetName),
				"createdAt":  now,
				"updatedAt":  now,
			}
			_, _ = insertRow("bmp_bahan_baku", bbBody)
		}

		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpMaintenanceLogsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if !checkManagerOrOwner(w, r) { return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/maintenance-logs/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_maintenance_logs SET "isDeleted"=TRUE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — AR Aging Report (Laporan Umur Piutang Klien) (v2.19.58) ───────────────

func handleRtBmpArAging(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }

	nowMs := nowMillis()

	// Query all unpaid / partially paid invoices
	rows, err := db.Query(`
		SELECT bi.id, bi."number", bi."title", COALESCE(bi."clientId", 0), COALESCE(bc."clientName", 'Tanpa Klien'),
		       COALESCE(bc."phoneNumber", ''), COALESCE(bi."dueDate", bi."createdAt"), bi."totalAmount",
		       COALESCE(bi."paidAmount", 0.0), bi."status", bi."createdAt"
		FROM bmp_invoices bi
		LEFT JOIN bmp_clients bc ON bi."clientId" = bc.id AND bi."tenantId" = bc."tenantId"
		WHERE bi."tenantId" = $1 AND bi."isDeleted" = FALSE
		  AND (bi."paidAmount" < bi."totalAmount" OR bi."status" NOT IN ('PAID', 'LUNAS', 'CANCELLED', 'VOID'))
		ORDER BY bi."createdAt" DESC
	`, tenantId)
	if err != nil { jsonErr(w, 500, err.Error()); return }
	defer rows.Close()

	type InvoiceAgingItem struct {
		InvoiceId      int64   `json:"invoiceId"`
		InvoiceNumber  string  `json:"invoiceNumber"`
		Title          string  `json:"title"`
		DueDate        int64   `json:"dueDate"`
		TotalAmount    float64 `json:"totalAmount"`
		PaidAmount     float64 `json:"paidAmount"`
		Remaining      float64 `json:"remaining"`
		OverdueDays    int     `json:"overdueDays"`
		Bucket         string  `json:"bucket"` // "CURRENT", "DAYS_1_30", "DAYS_31_60", "DAYS_OVER_60"
		Status         string  `json:"status"`
		CreatedAt      int64   `json:"createdAt"`
	}

	type ClientAgingGroup struct {
		ClientId       int64              `json:"clientId"`
		ClientName     string             `json:"clientName"`
		PhoneNumber    string             `json:"phoneNumber"`
		TotalReceivable float64           `json:"totalReceivable"`
		CurrentAmount  float64            `json:"currentAmount"`
		Days1To30      float64            `json:"days1To30"`
		Days31To60     float64            `json:"days31To60"`
		DaysOver60     float64            `json:"daysOver60"`
		OldestOverdueDays int             `json:"oldestOverdueDays"`
		Invoices       []InvoiceAgingItem `json:"invoices"`
	}

	clientMap := make(map[int64]*ClientAgingGroup)
	var grandTotalReceivable, grandCurrent, grandDays1To30, grandDays31To60, grandDaysOver60 float64

	for rows.Next() {
		var invId, clientId, dueDate, createdAt int64
		var invNumber, title, clientName, phone, status string
		var totalAmount, paidAmount float64

		if err := rows.Scan(&invId, &invNumber, &title, &clientId, &clientName, &phone, &dueDate, &totalAmount, &paidAmount, &status, &createdAt); err != nil {
			continue
		}

		remaining := totalAmount - paidAmount
		if remaining <= 0 { continue }

		// Calculate overdue days
		var overdueDays int = 0
		if dueDate > 0 && nowMs > dueDate {
			overdueDays = int((nowMs - dueDate) / (1000 * 60 * 60 * 24))
		}

		var bucket string
		if overdueDays <= 0 {
			bucket = "CURRENT"
			grandCurrent += remaining
		} else if overdueDays <= 30 {
			bucket = "DAYS_1_30"
			grandDays1To30 += remaining
		} else if overdueDays <= 60 {
			bucket = "DAYS_31_60"
			grandDays31To60 += remaining
		} else {
			bucket = "DAYS_OVER_60"
			grandDaysOver60 += remaining
		}
		grandTotalReceivable += remaining

		item := InvoiceAgingItem{
			InvoiceId:     invId,
			InvoiceNumber: invNumber,
			Title:         title,
			DueDate:       dueDate,
			TotalAmount:   totalAmount,
			PaidAmount:    paidAmount,
			Remaining:     remaining,
			OverdueDays:   overdueDays,
			Bucket:        bucket,
			Status:        status,
			CreatedAt:     createdAt,
		}

		grp, exists := clientMap[clientId]
		if !exists {
			grp = &ClientAgingGroup{
				ClientId:    clientId,
				ClientName:  clientName,
				PhoneNumber: phone,
				Invoices:    []InvoiceAgingItem{},
			}
			clientMap[clientId] = grp
		}

		grp.TotalReceivable += remaining
		switch bucket {
		case "CURRENT": grp.CurrentAmount += remaining
		case "DAYS_1_30": grp.Days1To30 += remaining
		case "DAYS_31_60": grp.Days31To60 += remaining
		case "DAYS_OVER_60": grp.DaysOver60 += remaining
		}
		if overdueDays > grp.OldestOverdueDays {
			grp.OldestOverdueDays = overdueDays
		}
		grp.Invoices = append(grp.Invoices, item)
	}

	var clientList []ClientAgingGroup
	for _, grp := range clientMap {
		clientList = append(clientList, *grp)
	}

	// Sort clients by TotalReceivable DESC
	sort.Slice(clientList, func(i, j int) bool {
		return clientList[i].TotalReceivable > clientList[j].TotalReceivable
	})

	jsonOK(w, map[string]interface{}{
		"totalReceivable": grandTotalReceivable,
		"currentAmount":   grandCurrent,
		"days1To30":       grandDays1To30,
		"days31To60":      grandDays31To60,
		"daysOver60":      grandDaysOver60,
		"clientCount":     len(clientList),
		"clients":         clientList,
	})
}

// ── E-Recruitment: Invitations & Applicants ─────────────────────────────────

func handleRtBmpJobInvitations(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, err := db.Query(`SELECT * FROM bmp_job_invitations WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "createdAt" DESC`, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var req struct {
			CandidateName  string `json:"candidateName"`
			CandidatePhone string `json:"candidatePhone"`
			PositionTarget string `json:"positionTarget"`
		}
		json.NewDecoder(r.Body).Decode(&req)

		tokenBytes := make([]byte, 4)
		_, _ = rand.Read(tokenBytes)
		token := fmt.Sprintf("BMP-INV-%X", tokenBytes)
		now := nowMillis()
		expiresAt := now + 7*86400*1000 // 7 days

		pos := strings.TrimSpace(req.PositionTarget)
		if pos == "" { pos = "OPERATOR" }

		body := map[string]interface{}{
			"tenantId":       tenantId,
			"token":          token,
			"candidateName":  strings.TrimSpace(req.CandidateName),
			"candidatePhone": strings.TrimSpace(req.CandidatePhone),
			"positionTarget": pos,
			"status":         "ACTIVE",
			"expiresAt":      expiresAt,
			"createdAt":      now,
			"updatedAt":      now,
			"isDeleted":      false,
		}
		id, err := insertRow("bmp_job_invitations", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		baseURL := os.Getenv("BASE_URL")
		if baseURL == "" { baseURL = "https://www.zedmz.cloud" }
		formURL := fmt.Sprintf("%s/karir/form?token=%s", baseURL, token)

		jsonOK(w, map[string]interface{}{
			"id":        id,
			"token":     token,
			"formUrl":   formURL,
			"expiresAt": expiresAt,
			"ok":        true,
		})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpJobInvitationsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/recruitment/invitations/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_job_invitations SET "isDeleted"=TRUE, "status"='CANCELLED', "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpJobApplicants(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, err := db.Query(`SELECT * FROM bmp_job_applicants WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "appliedAt" DESC`, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpJobApplicantsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/recruitment/applicants/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_job_applicants SET "isDeleted"=TRUE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpAcceptApplicant(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if r.Method != http.MethodPost { jsonErr(w, 405, "method not allowed"); return }

	var req struct {
		ApplicantId int64   `json:"applicantId"`
		SalaryOffer float64 `json:"salaryOffer"`
		Position    string  `json:"position"`
		Role        string  `json:"role"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonErr(w, 400, "invalid body")
		return
	}

	var app struct {
		FullName        string
		Nik             string
		Phone           string
		Email           string
		BirthPlaceDate  string
		Address         string
		PositionApplied string
		Education       string
		Experience      string
		KtpPhotoUrl     string
		SelfPhotoUrl    string
		SimPhotoUrl     string
		CvPdfUrl        string
		Status          string
	}
	err := db.QueryRow(`SELECT "fullName", "nik", "phone", "email", "birthPlaceDate", "address", "positionApplied", "education", "experience", "ktpPhotoUrl", "selfPhotoUrl", "simPhotoUrl", "cvPdfUrl", "status" FROM bmp_job_applicants WHERE id=$1 AND "tenantId"=$2 AND "isDeleted"=FALSE`, req.ApplicantId, tenantId).
		Scan(&app.FullName, &app.Nik, &app.Phone, &app.Email, &app.BirthPlaceDate, &app.Address, &app.PositionApplied, &app.Education, &app.Experience, &app.KtpPhotoUrl, &app.SelfPhotoUrl, &app.SimPhotoUrl, &app.CvPdfUrl, &app.Status)
	if err != nil {
		jsonErr(w, 404, "Data pelamar tidak ditemukan")
		return
	}
	if app.Status == "ACCEPTED" {
		jsonErr(w, 400, "Pelamar ini sudah pernah diterima sebelumnya")
		return
	}

	finalPos := strings.TrimSpace(req.Position)
	if finalPos == "" { finalPos = app.PositionApplied }
	finalRole := strings.TrimSpace(req.Role)
	if finalRole == "" { finalRole = finalPos }

	now := nowMillis()

	// 1. Insert ke bmp_employees
	empBody := map[string]interface{}{
		"tenantId":     tenantId,
		"name":         app.FullName,
		"phone":        app.Phone,
		"email":        app.Email,
		"role":         finalRole,
		"position":     finalPos,
		"salaryAmount": req.SalaryOffer,
		"employeeType": "OPERATING_EXPENSE",
		"nik":          app.Nik,
		"address":      app.Address,
		"ktpPhotoUrl":  app.KtpPhotoUrl,
		"selfPhotoUrl": app.SelfPhotoUrl,
		"cvPdfUrl":              app.CvPdfUrl,
		"isTraining":            true,
		"trainingTargetDays":    14,
		"trainingDaysCompleted": 0,
		"trainingStartedAt":     now,
		"isActive":              true,
		"isDeleted":             false,
		"isSynced":     true,
		"createdAt":    now,
		"updatedAt":    now,
	}
	empId, err := insertRow("bmp_employees", empBody)
	if err != nil {
		jsonErr(w, 500, "Gagal membuat data karyawan: "+err.Error())
		return
	}

	// 2. Jika posisi Sopir / Driver, otomatis insert ke bmp_drivers
	var driverId int64 = 0
	if strings.ToUpper(finalPos) == "DRIVER" || strings.Contains(strings.ToUpper(finalPos), "SOPIR") {
		drvBody := map[string]interface{}{
			"tenantId":      tenantId,
			"name":          app.FullName,
			"phone":         app.Phone,
			"plateNumber":   "",
			"truckType":     "",
			"ktpImageUrl":   app.KtpPhotoUrl,
			"simPhotoUrl":   app.SimPhotoUrl,
			"truckImageUrl": "",
			"stnkImageUrl":  "",
			"notes":         "Diterima dari E-Recruitment (NIK: " + app.Nik + ", Alamat: " + app.Address + ")",
			"isActive":      true,
			"isDeleted":     false,
			"createdAt":     now,
			"updatedAt":     now,
		}
		drvId, errDrv := insertRow("bmp_drivers", drvBody)
		if errDrv == nil {
			driverId = drvId
		}
	}

	// 3. Update status applicant menjadi ACCEPTED
	_, _ = db.Exec(`UPDATE bmp_job_applicants SET "status"='ACCEPTED', "acceptedEmployeeId"=$1, "acceptedDriverId"=$2, "salaryOffer"=$3, "updatedAt"=$4 WHERE id=$5 AND "tenantId"=$6`,
		empId, driverId, req.SalaryOffer, now, req.ApplicantId, tenantId)

	jsonOK(w, map[string]interface{}{
		"ok":                 true,
		"acceptedEmployeeId": empId,
		"acceptedDriverId":   driverId,
		"message":            "Pelamar berhasil diterima menjadi karyawan aktif",
	})
}

func handleRtBmpRejectApplicant(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	if r.Method != http.MethodPost { jsonErr(w, 405, "method not allowed"); return }

	var req struct {
		ApplicantId int64  `json:"applicantId"`
		Reason      string `json:"reason"`
	}
	json.NewDecoder(r.Body).Decode(&req)

	_, err := db.Exec(`UPDATE bmp_job_applicants SET "status"='REJECTED', "notes"=$1, "updatedAt"=$2 WHERE id=$3 AND "tenantId"=$4`,
		req.Reason, nowMillis(), req.ApplicantId, tenantId)
	if err != nil { jsonErr(w, 500, err.Error()); return }
	jsonOK(w, map[string]interface{}{"ok": true, "message": "Pelamar ditolak"})
}

// ── Public E-Recruitment Endpoints ──────────────────────────────────────────

func handleServeJobApplicationPage(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
	w.Header().Set("Pragma", "no-cache")
	w.Header().Set("Expires", "0")
	w.Header().Set("Content-Type", "text/html; charset=UTF-8")

	if len(jobAppWebHTML) > 0 {
		w.Write(jobAppWebHTML)
		return
	}

	paths := []string{
		"/home/muizz9900/job_application_web.html",
		"/home/muizz9900/android/backend/job_application_web.html",
		"./job_application_web.html",
		"./backend/job_application_web.html",
	}
	for _, p := range paths {
		if data, err := os.ReadFile(p); err == nil {
			w.Write(data)
			return
		}
	}
	http.Error(w, "Formulir lamaran sedang tidak tersedia", http.StatusNotFound)
}

func handlePublicValidateJobToken(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	token := strings.TrimSpace(r.URL.Query().Get("token"))
	if token == "" {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"valid":   false,
			"title":   "Token Tidak Ditemukan",
			"message": "Parameter token undangan tidak ditemukan pada link formulir.",
		})
		return
	}

	var inv struct {
		Id             int64
		TenantId       string
		CandidateName  string
		CandidatePhone string
		PositionTarget string
		Status         string
		ExpiresAt      int64
		IsDeleted      bool
	}
	err := db.QueryRow(`SELECT id, "tenantId", "candidateName", "candidatePhone", "positionTarget", "status", "expiresAt", "isDeleted" FROM bmp_job_invitations WHERE "token"=$1`, token).
		Scan(&inv.Id, &inv.TenantId, &inv.CandidateName, &inv.CandidatePhone, &inv.PositionTarget, &inv.Status, &inv.ExpiresAt, &inv.IsDeleted)

	if err != nil || inv.IsDeleted {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"valid":   false,
			"title":   "Link Tidak Valid",
			"message": "Link formulir ini tidak terdaftar di sistem.",
		})
		return
	}

	now := nowMillis()
	if inv.Status == "USED" {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"valid":   false,
			"title":   "Link Sudah Pernah Digunakan",
			"message": "Link formulir undangan ini sudah berhasil diisi dan tidak dapat digunakan kembali.",
		})
		return
	}
	if inv.Status == "CANCELLED" || (inv.ExpiresAt > 0 && now > inv.ExpiresAt) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"valid":   false,
			"title":   "Link Sudah Kadaluarsa",
			"message": "Masa berlaku link undangan ini telah habis. Silakan hubungi manajemen perusahaan.",
		})
		return
	}

	// Fetch Company Name from bmp_settings (clientName) or tenants table
	var companyName string
	_ = db.QueryRow(`SELECT COALESCE("clientName", '') FROM bmp_settings WHERE "tenantId"=$1 LIMIT 1`, inv.TenantId).Scan(&companyName)
	if strings.TrimSpace(companyName) == "" {
		_ = db.QueryRow(`SELECT COALESCE("name", '') FROM tenants WHERE id=$1 LIMIT 1`, inv.TenantId).Scan(&companyName)
	}
	companyName = strings.TrimSpace(companyName)
	if companyName == "" || strings.Contains(strings.ToLower(companyName), "danu sijon") {
		companyName = "CV. BAHTERA MULYA PLASTIK"
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"valid":          true,
		"companyName":    companyName,
		"candidateName":  inv.CandidateName,
		"candidatePhone": inv.CandidatePhone,
		"positionTarget": inv.PositionTarget,
	})
}

func handlePublicSubmitJobApplication(w http.ResponseWriter, r *http.Request) {
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

	if err := r.ParseMultipartForm(15 << 20); err != nil { // 15MB max
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": "Gagal membaca form: " + err.Error(),
		})
		return
	}

	token := strings.TrimSpace(r.FormValue("token"))
	if token == "" {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": "Token undangan tidak valid.",
		})
		return
	}

	// Atomic Check & Lock Token
	var inv struct {
		Id        int64
		TenantId  string
		Status    string
		ExpiresAt int64
	}
	err := db.QueryRow(`SELECT id, "tenantId", "status", "expiresAt" FROM bmp_job_invitations WHERE "token"=$1 AND "isDeleted"=FALSE`, token).
		Scan(&inv.Id, &inv.TenantId, &inv.Status, &inv.ExpiresAt)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": "Token undangan tidak ditemukan.",
		})
		return
	}

	now := nowMillis()
	if inv.Status == "USED" {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": "Link formulir ini sudah pernah digunakan.",
		})
		return
	}
	if inv.Status == "CANCELLED" || (inv.ExpiresAt > 0 && now > inv.ExpiresAt) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": "Link formulir ini sudah kadaluarsa.",
		})
		return
	}

	uploadDir := filepath.Join(".", "recruitment", inv.TenantId)
	_ = os.MkdirAll(uploadDir, 0755)

	saveFile := func(fields []string, prefix string) string {
		for _, field := range fields {
			file, header, errFile := r.FormFile(field)
			if errFile == nil {
				defer file.Close()
				ext := filepath.Ext(header.Filename)
				if ext == "" { ext = ".jpg" }
				fileName := fmt.Sprintf("%s_%d%s", prefix, time.Now().UnixNano()/1e6, ext)
				destPath := filepath.Join(uploadDir, fileName)
				bytes, errRead := io.ReadAll(file)
				if errRead == nil && len(bytes) > 0 {
					if errWrite := os.WriteFile(destPath, bytes, 0644); errWrite == nil {
						baseURL := os.Getenv("BASE_URL")
						if baseURL == "" { baseURL = "https://www.zedmz.cloud" }
						return fmt.Sprintf("%s/recruitment/%s/%s", baseURL, inv.TenantId, fileName)
					}
				}
			}
		}
		return ""
	}

	ktpUrl := saveFile([]string{"ktpFile", "ktpPhoto", "ktp"}, "ktp")
	selfUrl := saveFile([]string{"selfFile", "selfPhoto", "self"}, "self")
	simUrl := saveFile([]string{"simFile", "simPhoto", "sim"}, "sim")
	cvPdfUrl := saveFile([]string{"cvFile", "cvPdfFile", "cv", "cvPdf"}, "cv")

	testScore, _ := strconv.Atoi(r.FormValue("testScore"))
	testAnswers := strings.TrimSpace(r.FormValue("testAnswers"))
	wageAgreed := r.FormValue("wageAgreed") == "true" || r.FormValue("wageAgreed") == "1" || r.FormValue("wageAgreed") == "on" || r.FormValue("wageAgreed") == ""

	applicantBody := map[string]interface{}{
		"tenantId":        inv.TenantId,
		"invitationId":    inv.Id,
		"token":           token,
		"fullName":        strings.TrimSpace(r.FormValue("fullName")),
		"nik":             strings.TrimSpace(r.FormValue("nik")),
		"phone":           strings.TrimSpace(r.FormValue("phone")),
		"email":           strings.TrimSpace(r.FormValue("email")),
		"gender":          strings.TrimSpace(r.FormValue("gender")),
		"birthPlaceDate":  strings.TrimSpace(r.FormValue("birthPlaceDate")),
		"address":         strings.TrimSpace(r.FormValue("address")),
		"positionApplied": strings.TrimSpace(r.FormValue("positionApplied")),
		"education":       strings.TrimSpace(r.FormValue("education")),
		"experience":      strings.TrimSpace(r.FormValue("experience")),
		"ktpPhotoUrl":     ktpUrl,
		"selfPhotoUrl":    selfUrl,
		"simPhotoUrl":     simUrl,
		"cvPdfUrl":        cvPdfUrl,
		"testScore":       testScore,
		"testAnswers":     testAnswers,
		"wageAgreed":      wageAgreed,
		"status":          "PENDING",
		"appliedAt":       now,
		"updatedAt":       now,
		"isDeleted":       false,
	}

	appId, errApp := insertRow("bmp_job_applicants", applicantBody)
	if errApp != nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": "Gagal menyimpan lamaran: " + errApp.Error(),
		})
		return
	}

	// Invalidate token (mark as USED)
	_, _ = db.Exec(`UPDATE bmp_job_invitations SET "status"='USED', "usedAt"=$1, "updatedAt"=$2 WHERE id=$3`, now, now, inv.Id)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success":     true,
		"applicantId": appId,
		"message":     "Lamaran berhasil dikirimkan!",
	})
}

// ── BMP — warning letters (SP 1, SP 2, Surat Dikeluarkan / PHK) ─────────────

func handleRtBmpWarningLetters(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		employeeIdStr := r.URL.Query().Get("employeeId")
		var rows *sql.Rows
		var err error
		if employeeIdStr != "" {
			empId, _ := strconv.ParseInt(employeeIdStr, 10, 64)
			rows, err = db.Query(`SELECT * FROM bmp_warning_letters WHERE "tenantId"=$1 AND "employeeId"=$2 AND "isDeleted"=FALSE ORDER BY "issueDate" DESC, id DESC`, tenantId, empId)
		} else {
			rows, err = db.Query(`SELECT * FROM bmp_warning_letters WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "issueDate" DESC, id DESC`, tenantId)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()
		jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["tenantId"] = tenantId
		now := nowMillis()
		if _, ok := body["createdAt"]; !ok { body["createdAt"] = now }
		body["updatedAt"] = now
		id, err := insertRow("bmp_warning_letters", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		// Jika jenis surat adalah TERMINATION / Surat Dikeluarkan, otomatis nonaktifkan karyawan
		if letterType, ok := body["letterType"].(string); ok && letterType == "TERMINATION" {
			var empId int64
			if empIdVal, ok := body["employeeId"].(float64); ok {
				empId = int64(empIdVal)
			}
			if empId > 0 {
				_, _ = db.Exec(`UPDATE bmp_employees SET "isActive"=FALSE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, now, empId, tenantId)
			}
		}

		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpWarningLettersById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/warning-letters/")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil { jsonErr(w, 400, "invalid id"); return }

	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["updatedAt"] = nowMillis()
		updateRow("bmp_warning_letters", id, tenantId, body)
		jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_warning_letters SET "isDeleted"=TRUE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — PHL (Pekerja Harian Lepas / Cadangan) & Web Registration Form ───────

var phlFormWebHTML []byte

func handleServePhlFormPage(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
	w.Header().Set("Pragma", "no-cache")
	w.Header().Set("Expires", "0")
	w.Header().Set("Content-Type", "text/html; charset=UTF-8")

	if len(phlFormWebHTML) > 0 {
		w.Write(phlFormWebHTML)
		return
	}

	paths := []string{
		"/home/muizz9900/phl_form_web.html",
		"/home/muizz9900/android/backend/phl_form_web.html",
		"./phl_form_web.html",
		"./backend/phl_form_web.html",
	}
	for _, p := range paths {
		if data, err := os.ReadFile(p); err == nil {
			w.Write(data)
			return
		}
	}
	http.Error(w, "Formulir pendaftaran PHL sedang tidak tersedia", http.StatusNotFound)
}

func handlePublicValidatePhlSession(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusOK)
		return
	}

	token := strings.TrimSpace(r.URL.Query().Get("session"))
	if token == "" {
		token = strings.TrimSpace(r.URL.Query().Get("token"))
	}
	if token == "" {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"valid":   false,
			"title":   "Token Tidak Ditemukan",
			"message": "Parameter sesi pendaftaran PHL tidak ditemukan pada tautan.",
		})
		return
	}

	var session struct {
		Id              int64
		TenantId        string
		SessionCode     string
		Token           string
		Title           string
		WorkDate        int64
		ShiftName       string
		DailyWage       float64
		MaxQuota        int
		RegisteredCount int
		Status          string
		Notes           string
		IsDeleted       bool
	}
	err := db.QueryRow(`SELECT id, "tenantId", "sessionCode", "token", "title", "workDate", "shiftName", "dailyWage", "maxQuota", "registeredCount", "status", "notes", "isDeleted" FROM bmp_phl_sessions WHERE "token"=$1`, token).
		Scan(&session.Id, &session.TenantId, &session.SessionCode, &session.Token, &session.Title, &session.WorkDate, &session.ShiftName, &session.DailyWage, &session.MaxQuota, &session.RegisteredCount, &session.Status, &session.Notes, &session.IsDeleted)

	if err != nil || session.IsDeleted {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"valid":   false,
			"title":   "Jadwal Tidak Ditemukan",
			"message": "Jadwal pendaftaran PHL ini tidak terdaftar di sistem atau sudah dihapus.",
		})
		return
	}

	// Calculate actual active registered count
	var activeRegisteredCount int
	_ = db.QueryRow(`SELECT COUNT(*) FROM bmp_phl_applicants WHERE "sessionId"=$1 AND "isDeleted"=FALSE`, session.Id).Scan(&activeRegisteredCount)
	if activeRegisteredCount != session.RegisteredCount {
		session.RegisteredCount = activeRegisteredCount
		_, _ = db.Exec(`UPDATE bmp_phl_sessions SET "registeredCount"=$1 WHERE id=$2`, activeRegisteredCount, session.Id)
	}

	var companyName string
	_ = db.QueryRow(`SELECT COALESCE("clientName", '') FROM bmp_settings WHERE "tenantId"=$1 LIMIT 1`, session.TenantId).Scan(&companyName)
	if strings.TrimSpace(companyName) == "" {
		_ = db.QueryRow(`SELECT COALESCE("name", '') FROM tenants WHERE id=$1 LIMIT 1`, session.TenantId).Scan(&companyName)
	}
	companyName = strings.TrimSpace(companyName)
	if companyName == "" || strings.Contains(strings.ToLower(companyName), "danu sijon") {
		companyName = "CV. BAHTERA MULYA PLASTIK"
	}

	isFull := session.Status == "FULL" || (session.MaxQuota > 0 && session.RegisteredCount >= session.MaxQuota)
	remaining := session.MaxQuota - session.RegisteredCount
	if remaining < 0 { remaining = 0 }

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"valid":           true,
		"sessionId":       session.Id,
		"sessionCode":     session.SessionCode,
		"title":           session.Title,
		"workDate":        session.WorkDate,
		"shiftName":       session.ShiftName,
		"dailyWage":       session.DailyWage,
		"maxQuota":        session.MaxQuota,
		"registeredCount": session.RegisteredCount,
		"remainingQuota":  remaining,
		"status":          session.Status,
		"isFull":          isFull,
		"companyName":     companyName,
		"notes":           session.Notes,
		"message":         ifThen(isFull, "Maaf, pendaftaran PHL sudah penuh. Kuota telah terpenuhi.", ""),
	})
}

func handlePublicSubmitPhlApplication(w http.ResponseWriter, r *http.Request) {
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

	if err := r.ParseMultipartForm(25 << 20); err != nil { // 25MB max
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": "Gagal membaca berkas formulir: " + err.Error(),
		})
		return
	}

	token := strings.TrimSpace(r.FormValue("token"))
	if token == "" {
		token = strings.TrimSpace(r.FormValue("session"))
	}
	if token == "" {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": "Token pendaftaran tidak valid.",
		})
		return
	}

	// Atomic Check & Lock Session
	tx, errTx := db.Begin()
	if errTx != nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "Database transaction error: " + errTx.Error()})
		return
	}
	defer tx.Rollback()

	var session struct {
		Id              int64
		TenantId        string
		MaxQuota        int
		RegisteredCount int
		Status          string
	}
	err := tx.QueryRow(`SELECT id, "tenantId", "maxQuota", "registeredCount", "status" FROM bmp_phl_sessions WHERE "token"=$1 AND "isDeleted"=FALSE FOR UPDATE`, token).
		Scan(&session.Id, &session.TenantId, &session.MaxQuota, &session.RegisteredCount, &session.Status)

	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": "Jadwal pendaftaran tidak ditemukan atau sudah tidak aktif.",
		})
		return
	}

	// Double check active applicants
	var activeCount int
	_ = tx.QueryRow(`SELECT COUNT(*) FROM bmp_phl_applicants WHERE "sessionId"=$1 AND "isDeleted"=FALSE`, session.Id).Scan(&activeCount)

	if session.Status == "FULL" || session.Status == "CLOSED" || (session.MaxQuota > 0 && activeCount >= session.MaxQuota) {
		if session.Status == "OPEN" && activeCount >= session.MaxQuota {
			_, _ = tx.Exec(`UPDATE bmp_phl_sessions SET "status"='FULL', "registeredCount"=$1 WHERE id=$2`, activeCount, session.Id)
			_ = tx.Commit()
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"isFull":  true,
			"message": "Maaf, pendaftaran PHL sudah penuh. Kuota maksimal telah terpenuhi.",
		})
		return
	}

	uploadDir := filepath.Join(".", "recruitment", session.TenantId)
	_ = os.MkdirAll(uploadDir, 0755)

	saveFile := func(fields []string, prefix string) string {
		for _, field := range fields {
			file, header, errFile := r.FormFile(field)
			if errFile == nil {
				defer file.Close()
				ext := filepath.Ext(header.Filename)
				if ext == "" { ext = ".jpg" }
				fileName := fmt.Sprintf("%s_%d%s", prefix, time.Now().UnixNano()/1e6, ext)
				destPath := filepath.Join(uploadDir, fileName)
				bytes, errRead := io.ReadAll(file)
				if errRead == nil && len(bytes) > 0 {
					if errWrite := os.WriteFile(destPath, bytes, 0644); errWrite == nil {
						baseURL := os.Getenv("BASE_URL")
						if baseURL == "" { baseURL = "https://www.zedmz.cloud" }
						return fmt.Sprintf("%s/recruitment/%s/%s", baseURL, session.TenantId, fileName)
					}
				}
			}
		}
		return ""
	}

	ktpUrl := saveFile([]string{"ktpFile", "ktpPhoto", "ktp"}, "phl_ktp")
	selfUrl := saveFile([]string{"selfFile", "selfPhoto", "self"}, "phl_selfie")
	ijazahUrl := saveFile([]string{"ijazahFile", "ijazahPhoto", "ijazah"}, "phl_ijazah")
	cvPdfUrl := saveFile([]string{"cvFile", "cvPdfFile", "cv"}, "phl_cv")

	now := nowMillis()
	fullName := strings.TrimSpace(r.FormValue("fullName"))
	phone := strings.TrimSpace(r.FormValue("phone"))
	nik := strings.TrimSpace(r.FormValue("nik"))
	address := strings.TrimSpace(r.FormValue("address"))

	var appId int64
	errInsert := tx.QueryRow(`INSERT INTO bmp_phl_applicants 
		("tenantId", "sessionId", "fullName", "phone", "nik", "address", "ktpPhotoUrl", "selfPhotoUrl", "ijazahPhotoUrl", "cvPdfUrl", "status", "notes", "appliedAt", "updatedAt", "isDeleted") 
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 'REGISTERED', '', $11, $12, FALSE) RETURNING id`,
		session.TenantId, session.Id, fullName, phone, nik, address, ktpUrl, selfUrl, ijazahUrl, cvPdfUrl, now, now).Scan(&appId)

	if errInsert != nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": "Gagal menyimpan data pendaftaran: " + errInsert.Error(),
		})
		return
	}

	newCount := activeCount + 1
	newStatus := "OPEN"
	if session.MaxQuota > 0 && newCount >= session.MaxQuota {
		newStatus = "FULL"
	}
	_, errUpd := tx.Exec(`UPDATE bmp_phl_sessions SET "registeredCount"=$1, "status"=$2, "updatedAt"=$3 WHERE id=$4`, newCount, newStatus, now, session.Id)
	if errUpd != nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "Gagal mengupdate kuota sesi: " + errUpd.Error()})
		return
	}

	if errCommit := tx.Commit(); errCommit != nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "Commit error: " + errCommit.Error()})
		return
	}

	// Broadcast WS to POSBah App
	wsMsg := fmt.Sprintf(`{"type":"bmp_phl_update","sessionId":%d,"applicantId":%d,"count":%d,"status":"%s"}`, session.Id, appId, newCount, newStatus)
	broadcastWSMessage(wsMsg)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success":     true,
		"applicantId": appId,
		"message":     "Pendaftaran PHL berhasil! Data Anda telah terdaftar.",
	})
}

func handleRtBmpPhlSessions(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }

	switch r.Method {
	case http.MethodGet:
		rows, err := db.Query(`SELECT id, "tenantId", "sessionCode", "token", "title", "workDate", "shiftName", "dailyWage", "maxQuota", "registeredCount", "status", "notes", "createdAt", "updatedAt", "isDeleted" FROM bmp_phl_sessions WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "workDate" DESC, id DESC`, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()

		var sessions []map[string]interface{}
		for rows.Next() {
			var s struct {
				Id              int64
				TenantId        string
				SessionCode     string
				Token           string
				Title           string
				WorkDate        int64
				ShiftName       string
				DailyWage       float64
				MaxQuota        int
				RegisteredCount int
				Status          string
				Notes           string
				CreatedAt       int64
				UpdatedAt       int64
				IsDeleted       bool
			}
			if err := rows.Scan(&s.Id, &s.TenantId, &s.SessionCode, &s.Token, &s.Title, &s.WorkDate, &s.ShiftName, &s.DailyWage, &s.MaxQuota, &s.RegisteredCount, &s.Status, &s.Notes, &s.CreatedAt, &s.UpdatedAt, &s.IsDeleted); err == nil {
				var count int
				_ = db.QueryRow(`SELECT COUNT(*) FROM bmp_phl_applicants WHERE "sessionId"=$1 AND "isDeleted"=FALSE`, s.Id).Scan(&count)
				sessions = append(sessions, map[string]interface{}{
					"id":              s.Id,
					"tenantId":        s.TenantId,
					"sessionCode":     s.SessionCode,
					"token":           s.Token,
					"title":           s.Title,
					"workDate":        s.WorkDate,
					"shiftName":       s.ShiftName,
					"dailyWage":       s.DailyWage,
					"maxQuota":        s.MaxQuota,
					"registeredCount": count,
					"status":          s.Status,
					"notes":           s.Notes,
					"createdAt":       s.CreatedAt,
					"updatedAt":       s.UpdatedAt,
					"isDeleted":       s.IsDeleted,
				})
			}
		}
		if sessions == nil { sessions = []map[string]interface{}{} }
		jsonOK(w, sessions)

	case http.MethodPost:
		var req struct {
			Title     string  `json:"title"`
			WorkDate  int64   `json:"workDate"`
			ShiftName string  `json:"shiftName"`
			DailyWage float64 `json:"dailyWage"`
			MaxQuota  int     `json:"maxQuota"`
			Notes     string  `json:"notes"`
		}
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			jsonErr(w, 400, "invalid json: "+err.Error())
			return
		}

		tokenBytes := make([]byte, 6)
		_, _ = rand.Read(tokenBytes)
		token := fmt.Sprintf("phl_%x", tokenBytes)
		sessionCode := fmt.Sprintf("PHL-%s-%02X%02X", time.Now().Format("20060102"), tokenBytes[0], tokenBytes[1])
		now := nowMillis()

		if req.Title == "" { req.Title = "Operator Injeksi Cadangan" }
		if req.ShiftName == "" { req.ShiftName = "Pagi (07.00 - 15.00)" }
		if req.Notes == "" { req.Notes = "15 menit wajib sudah ada di lokasi, silahkan japri no. wa 082245077959 untuk lokasi" }
		if req.MaxQuota <= 0 { req.MaxQuota = 3 }
		if req.WorkDate <= 0 { req.WorkDate = now }

		body := map[string]interface{}{
			"tenantId":        tenantId,
			"sessionCode":     sessionCode,
			"token":           token,
			"title":           req.Title,
			"workDate":        req.WorkDate,
			"shiftName":       req.ShiftName,
			"dailyWage":       req.DailyWage,
			"maxQuota":        req.MaxQuota,
			"registeredCount": 0,
			"status":          "OPEN",
			"notes":           req.Notes,
			"createdAt":       now,
			"updatedAt":       now,
			"isDeleted":       false,
		}

		id, err := insertRow("bmp_phl_sessions", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		baseURL := os.Getenv("BASE_URL")
		if baseURL == "" { baseURL = "https://www.zedmz.cloud" }
		formURL := fmt.Sprintf("%s/phl/form?session=%s", baseURL, token)

		jsonOK(w, map[string]interface{}{
			"id":          id,
			"sessionCode": sessionCode,
			"token":       token,
			"formUrl":     formURL,
		})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpPhlSessionsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/phl/sessions/")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil { jsonErr(w, 400, "invalid id"); return }

	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["updatedAt"] = nowMillis()

		if maxQ, ok := body["maxQuota"].(float64); ok && maxQ > 0 {
			var regCount int
			_ = db.QueryRow(`SELECT COUNT(*) FROM bmp_phl_applicants WHERE "sessionId"=$1 AND "isDeleted"=FALSE`, id).Scan(&regCount)
			if regCount < int(maxQ) && body["status"] == "FULL" {
				body["status"] = "OPEN"
			} else if regCount >= int(maxQ) {
				body["status"] = "FULL"
			}
			body["registeredCount"] = regCount
		}

		updateRow("bmp_phl_sessions", id, tenantId, body)
		jsonOK(w, map[string]interface{}{"ok": true})

	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_phl_sessions SET "isDeleted"=TRUE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpPhlApplicants(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }

	switch r.Method {
	case http.MethodGet:
		sessionIdStr := r.URL.Query().Get("sessionId")
		var rows *sql.Rows
		var err error
		if sessionIdStr != "" {
			sId, _ := strconv.ParseInt(sessionIdStr, 10, 64)
			rows, err = db.Query(`SELECT id, "tenantId", "sessionId", "fullName", "phone", "nik", "address", "ktpPhotoUrl", "selfPhotoUrl", "ijazahPhotoUrl", "cvPdfUrl", "status", "notes", "appliedAt", "updatedAt", "isDeleted" FROM bmp_phl_applicants WHERE "tenantId"=$1 AND "sessionId"=$2 AND "isDeleted"=FALSE ORDER BY "appliedAt" DESC`, tenantId, sId)
		} else {
			rows, err = db.Query(`SELECT id, "tenantId", "sessionId", "fullName", "phone", "nik", "address", "ktpPhotoUrl", "selfPhotoUrl", "ijazahPhotoUrl", "cvPdfUrl", "status", "notes", "appliedAt", "updatedAt", "isDeleted" FROM bmp_phl_applicants WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "appliedAt" DESC`, tenantId)
		}
		if err != nil { jsonErr(w, 500, err.Error()); return }
		defer rows.Close()

		var list []map[string]interface{}
		for rows.Next() {
			var a struct {
				Id             int64
				TenantId       string
				SessionId      int64
				FullName       string
				Phone          string
				Nik            string
				Address        string
				KtpPhotoUrl    string
				SelfPhotoUrl   string
				IjazahPhotoUrl string
				CvPdfUrl       string
				Status         string
				Notes          string
				AppliedAt      int64
				UpdatedAt      int64
				IsDeleted      bool
			}
			if err := rows.Scan(&a.Id, &a.TenantId, &a.SessionId, &a.FullName, &a.Phone, &a.Nik, &a.Address, &a.KtpPhotoUrl, &a.SelfPhotoUrl, &a.IjazahPhotoUrl, &a.CvPdfUrl, &a.Status, &a.Notes, &a.AppliedAt, &a.UpdatedAt, &a.IsDeleted); err == nil {
				list = append(list, map[string]interface{}{
					"id":             a.Id,
					"tenantId":       a.TenantId,
					"sessionId":      a.SessionId,
					"fullName":       a.FullName,
					"phone":          a.Phone,
					"nik":            a.Nik,
					"address":        a.Address,
					"ktpPhotoUrl":    a.KtpPhotoUrl,
					"selfPhotoUrl":   a.SelfPhotoUrl,
					"ijazahPhotoUrl": a.IjazahPhotoUrl,
					"cvPdfUrl":       a.CvPdfUrl,
					"status":         a.Status,
					"notes":          a.Notes,
					"appliedAt":      a.AppliedAt,
					"updatedAt":      a.UpdatedAt,
					"isDeleted":      a.IsDeleted,
				})
			}
		}
		if list == nil { list = []map[string]interface{}{} }
		jsonOK(w, list)
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpPhlApplicantsById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/phl/applicants/")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil { jsonErr(w, 400, "invalid id"); return }

	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid json body")
			return
		}
		body["updatedAt"] = nowMillis()
		updateRow("bmp_phl_applicants", id, tenantId, body)
		jsonOK(w, map[string]interface{}{"ok": true})

	case http.MethodDelete:
		var sessionId int64
		_ = db.QueryRow(`SELECT "sessionId" FROM bmp_phl_applicants WHERE id=$1 AND "tenantId"=$2`, id, tenantId).Scan(&sessionId)
		_, err := db.Exec(`UPDATE bmp_phl_applicants SET "isDeleted"=TRUE, "updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }

		if sessionId > 0 {
			var activeCount int
			_ = db.QueryRow(`SELECT COUNT(*) FROM bmp_phl_applicants WHERE "sessionId"=$1 AND "isDeleted"=FALSE`, sessionId).Scan(&activeCount)
			var maxQ int
			_ = db.QueryRow(`SELECT "maxQuota" FROM bmp_phl_sessions WHERE id=$1`, sessionId).Scan(&maxQ)
			newStatus := "OPEN"
			if maxQ > 0 && activeCount >= maxQ {
				newStatus = "FULL"
			}
			_, _ = db.Exec(`UPDATE bmp_phl_sessions SET "registeredCount"=$1, "status"=$2, "updatedAt"=$3 WHERE id=$4`, activeCount, newStatus, nowMillis(), sessionId)
		}

		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func ifThen(condition bool, a, b string) string {
	if condition { return a }
	return b
}



