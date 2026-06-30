package main

// handlers_rt.go — Real-time API handlers for Full Online mode
// Replaces SupabaseSyncManager. All endpoints require Authorization: Bearer <token>.
// All queries filter by "tenantId" for tenant isolation.
// Target: GET < 150ms, POST/PUT < 300ms.

import (
	"database/sql"
	"encoding/csv"
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

func handleRtBmpFinancialReport(w http.ResponseWriter, r *http.Request) {
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

	// 1. Query Omzet
	var omzet float64
	err := db.QueryRow(`
		SELECT COALESCE(SUM("totalAmount"), 0) 
		FROM bmp_invoices 
		WHERE "tenantId"=$1 AND "createdAt" >= $2 AND "createdAt" < $3 AND "isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&omzet)
	if err != nil { jsonErr(w, 500, err.Error()); return }

	// 2. Query COGS (HPP Terjual)
	var cogs float64
	err = db.QueryRow(`
		SELECT COALESCE(SUM(bp.quantity * COALESCE(mp."hppTotalPcs", 0)), 0)
		FROM bmp_products bp
		JOIN bmp_invoices bi ON bp."invoiceId" = bi.id
		LEFT JOIN bmp_master_products mp ON bp."masterItemID" = mp.id
		WHERE bi."tenantId"=$1 AND bi."createdAt" >= $2 AND bi."createdAt" < $3 
		  AND bi."isDeleted"=FALSE AND bp."isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&cogs)
	if err != nil { jsonErr(w, 500, err.Error()); return }

	// 3. Query Direct Materials Cost (Actual Consumption)
	var directMaterials float64
	err = db.QueryRow(`
		SELECT COALESCE(SUM(pl."rawMaterialUsedKg" * COALESCE(rates.avg_rate, mp.price)), 0)
		FROM bmp_production_logs pl
		JOIN bmp_master_products mp ON pl."masterProductId" = mp.id AND pl."tenantId" = mp."tenantId"
		LEFT JOIN (
			SELECT bbi."jenisBahan", AVG(bbi.rate) as avg_rate
			FROM bmp_bahan_baku_item bbi
			JOIN bmp_bahan_baku bb ON bbi."bahanBakuId" = bb.id AND bbi."tenantId" = bb."tenantId"
			WHERE bb."tenantId" = $1 AND bb."isDeleted" = FALSE AND bbi."isDeleted" = FALSE
			GROUP BY bbi."jenisBahan"
		) rates ON mp."jenisBahanBaku" = rates."jenisBahan"
		WHERE pl."tenantId" = $1 AND pl."productionDate" >= $2 AND pl."productionDate" < $3 AND pl."isDeleted" = FALSE
	`, tenantId, startMs, endMs).Scan(&directMaterials)
	if err != nil { jsonErr(w, 500, err.Error()); return }

	// 4. Query Direct Labor Cost
	var directLabor float64
	err = db.QueryRow(`
		SELECT COALESCE(
			(SELECT SUM(bp.amount)
			 FROM bmp_payrolls bp
			 JOIN bmp_employees be ON bp."employeeId" = be.id AND bp."tenantId" = be."tenantId"
			 WHERE bp."tenantId" = $1 AND bp."paymentDate" >= $2 AND bp."paymentDate" < $3
			   AND be."employeeType" = 'DIRECT_LABOR'), 0)
		+ COALESCE(
			(SELECT SUM(amount)
			 FROM bmp_cashflow
			 WHERE "tenantId" = $1 AND "transactionType" = 'KELUAR'
			   AND "transactionDate" >= $2 AND "transactionDate" < $3 AND "isDeleted" = FALSE
			   AND "costType" = 'DIRECT_LABOR'), 0)
	`, tenantId, startMs, endMs).Scan(&directLabor)
	if err != nil { jsonErr(w, 500, err.Error()); return }

	// 5. Query Factory Overhead (FOH)
	var foh float64
	err = db.QueryRow(`
		SELECT COALESCE(
			(SELECT SUM(amount)
			 FROM bmp_cashflow
			 WHERE "tenantId" = $1 AND "transactionType" = 'KELUAR'
			   AND "transactionDate" >= $2 AND "transactionDate" < $3 AND "isDeleted" = FALSE
			   AND "costType" = 'FACTORY_OVERHEAD'), 0)
		+ COALESCE(
			(SELECT SUM(bp.amount)
			 FROM bmp_payrolls bp
			 JOIN bmp_employees be ON bp."employeeId" = be.id AND bp."tenantId" = be."tenantId"
			 WHERE bp."tenantId" = $1 AND bp."paymentDate" >= $2 AND bp."paymentDate" < $3
			   AND be."employeeType" = 'INDIRECT_LABOR'), 0)
	`, tenantId, startMs, endMs).Scan(&foh)
	if err != nil { jsonErr(w, 500, err.Error()); return }

	// 6. Query OPEX (Operating Expenses)
	var opex float64
	err = db.QueryRow(`
		SELECT COALESCE(
			(SELECT SUM(amount)
			 FROM bmp_cashflow
			 WHERE "tenantId" = $1 AND "transactionType" = 'KELUAR'
			   AND "transactionDate" >= $2 AND "transactionDate" < $3 AND "isDeleted" = FALSE
			   AND COALESCE("costType", 'OPERATING_EXPENSE') NOT IN ('FACTORY_OVERHEAD', 'DIRECT_LABOR')), 0)
		+ COALESCE(
			(SELECT SUM(bp.amount)
			 FROM bmp_payrolls bp
			 JOIN bmp_employees be ON bp."employeeId" = be.id AND bp."tenantId" = be."tenantId"
			 WHERE bp."tenantId" = $1 AND bp."paymentDate" >= $2 AND bp."paymentDate" < $3
			   AND COALESCE(be."employeeType", 'OPERATING_EXPENSE') NOT IN ('DIRECT_LABOR', 'INDIRECT_LABOR')), 0)
	`, tenantId, startMs, endMs).Scan(&opex)
	if err != nil { jsonErr(w, 500, err.Error()); return }

	var depreciation float64
	if periodType == "MONTHLY" {
		_ = db.QueryRow(`
			SELECT COALESCE(SUM(amount), 0.0) 
			FROM bmp_monthly_depreciation 
			WHERE "tenantId"=$1 AND period=$2
		`, tenantId, dateStr).Scan(&depreciation)
	} else if periodType == "QUARTERLY" {
		parts := strings.Split(dateStr, "-Q")
		year := parts[0]
		quarter, _ := strconv.Atoi(parts[1])
		var months []string
		for m := (quarter-1)*3 + 1; m <= quarter*3; m++ {
			months = append(months, fmt.Sprintf("'%s-%02d'", year, m))
		}
		qStr := fmt.Sprintf(`
			SELECT COALESCE(SUM(amount), 0.0) 
			FROM bmp_monthly_depreciation 
			WHERE "tenantId"=$1 AND period IN (%s)
		`, strings.Join(months, ","))
		_ = db.QueryRow(qStr, tenantId).Scan(&depreciation)
	} else { // ANNUALLY
		_ = db.QueryRow(`
			SELECT COALESCE(SUM(amount), 0.0) 
			FROM bmp_monthly_depreciation 
			WHERE "tenantId"=$1 AND period LIKE $2
		`, tenantId, dateStr+"-%").Scan(&depreciation)
	}

	foh = foh + depreciation
	cogm := directMaterials + directLabor + foh
	labaKotor := omzet - cogs
	labaBersih := labaKotor - opex

	// 7. Calculate BEP
	var bep float64
	if omzet > 0 && (omzet-cogs) > 0 {
		marginRatio := (omzet - cogs) / omzet
		bep = opex / marginRatio
	}

	cogsPercentage := 0.0
	marginPercentage := 0.0
	if omzet > 0 {
		cogsPercentage = (cogs / omzet) * 100.0
		marginPercentage = (labaKotor / omzet) * 100.0
	}

	// 5. Query Top Products
	type TopProduct struct {
		Name     string  `json:"name"`
		QtySold  float64 `json:"qtySold"`
		Revenue  float64 `json:"revenue"`
	}
	topProducts := []TopProduct{}
	rows, err := db.Query(`
		SELECT COALESCE(mp.title, bp.title), SUM(bp.quantity) as qty, SUM(bp.quantity * bp.price) as rev
		FROM bmp_products bp
		JOIN bmp_invoices bi ON bp."invoiceId" = bi.id
		LEFT JOIN bmp_master_products mp ON bp."masterItemID" = mp.id
		WHERE bi."tenantId"=$1 AND bi."createdAt" >= $2 AND bi."createdAt" < $3 
		  AND bi."isDeleted"=FALSE AND bp."isDeleted"=FALSE
		GROUP BY COALESCE(mp.title, bp.title)
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

	response := map[string]interface{}{
		"period":           dateStr,
		"omzet":            omzet,
		"cogs":             cogs,
		"labaKotor":        labaKotor,
		"opex":             opex,
		"labaBersih":       labaBersih,
		"bep":              bep,
		"cogsPercentage":   cogsPercentage,
		"marginPercentage": marginPercentage,
		"topProducts":      topProducts,
		"directMaterials":  directMaterials,
		"directLabor":      directLabor,
		"foh":              foh,
		"cogm":             cogm,
		"depreciation":     depreciation,
	}

	jsonOK(w, response)
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
	var omzet, cogs, opex float64
	_ = db.QueryRow(`
		SELECT COALESCE(SUM("totalAmount"), 0) FROM bmp_invoices 
		WHERE "tenantId"=$1 AND "createdAt" >= $2 AND "createdAt" < $3 AND "isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&omzet)

	_ = db.QueryRow(`
		SELECT COALESCE(SUM(bp.quantity * COALESCE(mp."hppTotalPcs", 0)), 0)
		FROM bmp_products bp
		JOIN bmp_invoices bi ON bp."invoiceId" = bi.id
		LEFT JOIN bmp_master_products mp ON bp."masterItemID" = mp.id
		WHERE bi."tenantId"=$1 AND bi."createdAt" >= $2 AND bi."createdAt" < $3 
		  AND bi."isDeleted"=FALSE AND bp."isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&cogs)

	_ = db.QueryRow(`
		SELECT COALESCE(SUM("amount"), 0) FROM bmp_cashflow 
		WHERE "tenantId"=$1 AND "transactionType"='KELUAR' 
		  AND "transactionDate" >= $2 AND "transactionDate" < $3 AND "isDeleted"=FALSE
	`, tenantId, startMs, endMs).Scan(&opex)

	labaKotor := omzet - cogs
	labaBersih := labaKotor - opex

	// Set CSV Headers
	w.Header().Set("Content-Type", "text/csv; charset=utf-8")
	filename := fmt.Sprintf("Laporan_Keuangan_POSBah_%s.csv", dateStr)
	w.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=%s", filename))

	writer := csv.NewWriter(w)
	writer.Comma = ';' // Titik koma agar Excel regional Indonesia langsung memisah kolom

	// BOM untuk UTF-8
	_, _ = w.Write([]byte{0xEF, 0xBB, 0xBF})

	// 1. Header Ringkasan Keuangan
	_ = writer.Write([]string{"LAPORAN KEUANGAN POSBAH (MANUFAKTUR)"})
	_ = writer.Write([]string{"Periode", dateStr})
	_ = writer.Write([]string{"Tipe Laporan", periodType})
	_ = writer.Write([]string{""})

	_ = writer.Write([]string{"IKHTISAR LABA RUGI"})
	_ = writer.Write([]string{"Pos Keuangan", "Nominal (Rupiah)"})
	_ = writer.Write([]string{"OMZET (Pendapatan Kotor)", fmt.Sprintf("%.2f", omzet)})
	_ = writer.Write([]string{"HARGA POKOK PENJUALAN (COGS / HPP)", fmt.Sprintf("%.2f", cogs)})
	_ = writer.Write([]string{"LABA KOTOR", fmt.Sprintf("%.2f", labaKotor)})
	_ = writer.Write([]string{"BEBAN OPERASIONAL (OPEX)", fmt.Sprintf("%.2f", opex)})
	_ = writer.Write([]string{"LABA BERSIH", fmt.Sprintf("%.2f", labaBersih)})
	_ = writer.Write([]string{""})
	_ = writer.Write([]string{""})

	// 2. Jurnal Penjualan
	_ = writer.Write([]string{"DETAIL JURNAL PENJUALAN (INVOICES)"})
	_ = writer.Write([]string{"ID", "Nomor Invoice", "Nama Pelanggan", "Tanggal Faktur", "Jatuh Tempo", "Total Tagihan", "Telah Dibayar", "Status"})
	
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
				_ = writer.Write([]string{
					strconv.FormatInt(id, 10),
					number,
					clientName,
					createdDate,
					dueDateStr,
					fmt.Sprintf("%.2f", totalAmt),
					fmt.Sprintf("%.2f", paidAmt),
					status,
				})
			}
		}
	}
	_ = writer.Write([]string{""})
	_ = writer.Write([]string{""})

	// 3. Buku Kas Keluar
	_ = writer.Write([]string{"DETAIL PENGELUARAN OPERASIONAL (CASHFLOW KELUAR)"})
	_ = writer.Write([]string{"ID", "Tanggal Transaksi", "Deskripsi Pengeluaran", "Jumlah (Rupiah)"})

	rowsCF, errCF := db.Query(`
		SELECT id, "transactionDate", description, amount
		FROM bmp_cashflow
		WHERE "tenantId"=$1 AND "transactionType"='KELUAR' AND "transactionDate" >= $2 AND "transactionDate" < $3 AND "isDeleted"=FALSE
		ORDER BY id ASC
	`, tenantId, startMs, endMs)
	
	if errCF == nil {
		defer rowsCF.Close()
		for rowsCF.Next() {
			var id int64
			var transDate int64
			var desc string
			var amount float64
			if errS := rowsCF.Scan(&id, &transDate, &desc, &amount); errS == nil {
				dateStrFormatted := time.Unix(transDate/1000, 0).In(loc).Format("2006-01-02 15:04")
				_ = writer.Write([]string{
					strconv.FormatInt(id, 10),
					dateStrFormatted,
					desc,
					fmt.Sprintf("%.2f", amount),
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

func handleRtBmpDepreciation(w http.ResponseWriter, r *http.Request) {
	tenantId, ok := extractTenantId(r)
	if !ok { jsonErr(w, 401, "unauthorized"); return }

	if r.Method == http.MethodGet {
		period := r.URL.Query().Get("period")
		if period == "" {
			jsonErr(w, 400, "period is required")
			return
		}
		var amount float64
		err := db.QueryRow(`
			SELECT COALESCE(amount, 0.0) 
			FROM bmp_monthly_depreciation 
			WHERE "tenantId"=$1 AND period=$2
		`, tenantId, period).Scan(&amount)
		if err != nil {
			jsonOK(w, map[string]interface{}{"amount": 0.0})
			return
		}
		jsonOK(w, map[string]interface{}{"amount": amount})
		return
	}

	if r.Method == http.MethodPost {
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			jsonErr(w, 400, "invalid body")
			return
		}
		period, _ := body["period"].(string)
		amountVal, _ := body["amount"].(float64)
		if period == "" {
			jsonErr(w, 400, "period is required")
			return
		}

		_, err := db.Exec(`
			INSERT INTO bmp_monthly_depreciation ("tenantId", "period", "amount", "updatedAt")
			VALUES ($1, $2, $3, $4)
			ON CONFLICT ("tenantId", "period") DO UPDATE SET "amount"=$3, "updatedAt"=$4
		`, tenantId, period, amountVal, time.Now().UnixNano()/1e6)
		if err != nil {
			jsonErr(w, 500, err.Error())
			return
		}
		jsonOK(w, map[string]interface{}{"success": true})
		return
	}

	jsonErr(w, 405, "method not allowed")
}
