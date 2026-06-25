package main

// handlers_rt.go — Real-time API handlers for Full Online mode
// Replaces SupabaseSyncManager. All endpoints require Authorization: Bearer <token>.
// All queries filter by "tenantId" for tenant isolation.
// Target: GET < 150ms, POST/PUT < 300ms.

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"
)

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
	// Try 1: sessionToken (for future JWT support)
	err := db.QueryRow(`SELECT "tenantId" FROM "local_users" WHERE "sessionToken" = $1 AND "isActive" = TRUE LIMIT 1`, token).Scan(&tenantId)
	if err != nil {
		// Try 2: googleSub (numeric sub ID from Google SSO)
		err2 := db.QueryRow(`SELECT "tenantId" FROM "local_users" WHERE "googleSub" = $1 AND "isActive" = TRUE LIMIT 1`, token).Scan(&tenantId)
		if err2 != nil {
			// Try 3: email — for premium static users (email+password login) whose token = email
			err3 := db.QueryRow(`SELECT "tenantId" FROM "local_users" WHERE "email" = $1 AND "isActive" = TRUE LIMIT 1`, token).Scan(&tenantId)
			if err3 != nil {
				// Try 4: check employees table — for kasir/admin login where token = "emp:<id>"
				if strings.HasPrefix(token, "emp:") {
					empIdStr := strings.TrimPrefix(token, "emp:")
					err4 := db.QueryRow(`SELECT "tenantId" FROM "employees" WHERE id = $1 AND "isActive" = TRUE LIMIT 1`, empIdStr).Scan(&tenantId)
					if err4 != nil {
						return "", false
					}
				} else {
					return "", false
				}
			}
		}
	}
	return tenantId, tenantId != ""
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
		"bmp_cashflow": true, "bmp_payrolls": true,
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
		id, err := insertRow("bmp_invoices", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
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
		updateRow("bmp_invoices", id, tenantId, body); jsonOK(w, map[string]interface{}{"ok": true})
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
		rows, _ := db.Query(`SELECT bp.* FROM bmp_products bp JOIN bmp_invoices bi ON bi.id=bp."invoiceId" WHERE bi."tenantId"=$1 AND bp."invoiceId"=$2 AND bp."isDeleted"=FALSE ORDER BY bp.id ASC`, tenantId, invoiceId)
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

func handleRtBmpCashflow(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_cashflow WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "transactionDate" DESC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId
		id, err := insertRow("bmp_cashflow", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpCashflowById(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/cashflow/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		updateRow("bmp_cashflow", id, tenantId, body); jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE bmp_cashflow SET "isDeleted"=TRUE WHERE id=$1 AND "tenantId"=$2`, id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

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
		id, err := insertRow("bmp_invoice_payments", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
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
	switch r.Method {
	case http.MethodDelete:
		db.Exec(`UPDATE bmp_invoice_payments SET "isDeleted"=TRUE WHERE id=$1 AND "tenantId"=$2`, id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

// ── BMP — employees & payrolls ────────────────────────────────────────────────

func handleRtBmpEmployees(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_employees WHERE "tenantId"=$1 AND "isActive"=TRUE ORDER BY name ASC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
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
	idStr := strings.TrimPrefix(r.URL.Path, "/api/rt/bmp/employees/")
	id, _ := strconv.ParseInt(idStr, 10, 64)
	switch r.Method {
	case http.MethodPut:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["updatedAt"] = nowMillis()
		updateRow("bmp_employees", id, tenantId, body); jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE bmp_employees SET "isActive"=FALSE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
		jsonOK(w, map[string]interface{}{"ok": true})
	default:
		jsonErr(w, 405, "method not allowed")
	}
}

func handleRtBmpPayrolls(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }
	switch r.Method {
	case http.MethodGet:
		rows, _ := db.Query(`SELECT * FROM bmp_payrolls WHERE "tenantId"=$1 ORDER BY "paymentDate" DESC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId
		id, err := insertRow("bmp_payrolls", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
		jsonOK(w, map[string]interface{}{"id": id, "ok": true})
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
		updateRow("bmp_bahan_baku", id, tenantId, body); jsonOK(w, map[string]interface{}{"ok": true})
	case http.MethodDelete:
		db.Exec(`UPDATE bmp_bahan_baku SET "isDeleted"=TRUE,"updatedAt"=$1 WHERE id=$2 AND "tenantId"=$3`, nowMillis(), id, tenantId)
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
		rows, _ := db.Query(`SELECT bbi.* FROM bmp_bahan_baku_item bbi JOIN bmp_bahan_baku bb ON bb.id=bbi."bahanBakuId" WHERE bb."tenantId"=$1 AND bbi."bahanBakuId"=$2 AND bbi."isDeleted"=FALSE ORDER BY bbi.id ASC`, tenantId, bahanBakuId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body []map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body)
		for _, item := range body { item["tenantId"] = tenantId; insertRow("bmp_bahan_baku_item", item) }
		jsonOK(w, map[string]interface{}{"ok": true, "count": len(body)})
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
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId
		id, err := insertRow("bmp_production_logs", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
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
	case http.MethodDelete:
		_, err := db.Exec(`UPDATE bmp_production_logs SET "isDeleted"=TRUE WHERE id=$1 AND "tenantId"=$2`, id, tenantId)
		if err != nil { jsonErr(w, 500, err.Error()); return }
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
		rows, _ := db.Query(`SELECT * FROM bmp_product_stocks WHERE "tenantId"=$1 AND "isDeleted"=FALSE`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		id, err := insertRow("bmp_product_stocks", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
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
		rows, _ := db.Query(`SELECT * FROM bmp_stock_ledger WHERE "tenantId"=$1 AND "isDeleted"=FALSE ORDER BY "createdAt" DESC`, tenantId)
		defer rows.Close(); jsonOK(w, rowsToJSON(rows))
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId
		id, err := insertRow("bmp_stock_ledger", body)
		if err != nil { jsonErr(w, 500, err.Error()); return }
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
		if len(result) > 0 { jsonOK(w, result[0]) } else { jsonOK(w, nil) }
	case http.MethodPost:
		var body map[string]interface{}
		json.NewDecoder(r.Body).Decode(&body); body["tenantId"] = tenantId; body["updatedAt"] = nowMillis()
		db.Exec(`DELETE FROM bmp_settings WHERE "tenantId"=$1`, tenantId)
		insertRow("bmp_settings", body)
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
