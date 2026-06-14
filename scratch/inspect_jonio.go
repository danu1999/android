package main

import (
	"database/sql"
	"fmt"
	"log"

	_ "github.com/lib/pq"
)

func main() {
	dbURL := "postgres://postgres:Bahtera1!@localhost:5432/posbah?sslmode=disable"
	db, err := sql.Open("postgres", dbURL)
	if err != nil {
		log.Fatalf("Gagal konek: %v", err)
	}
	defer db.Close()

	// 1. Check local_users
	fmt.Println("--- local_users ---")
	rows, err := db.Query(`SELECT "googleSub", "email", "role", "tenantId", "isActive" FROM "local_users" WHERE "email" = 'jonio9012@gmail.com'`)
	if err == nil {
		defer rows.Close()
		for rows.Next() {
			var googleSub, email, role, tenantId sql.NullString
			var isActive bool
			rows.Scan(&googleSub, &email, &role, &tenantId, &isActive)
			fmt.Printf("googleSub: %s | email: %s | role: %s | tenantId: %s | isActive: %t\n",
				googleSub.String, email.String, role.String, tenantId.String, isActive)
		}
	} else {
		fmt.Println("Error local_users:", err)
	}

	// 2. Check employees
	fmt.Println("\n--- employees ---")
	rowsE, err := db.Query(`SELECT "id", "tenantId", "outletId", "name", "email", "role", "pinHash", "isActive" FROM "employees" WHERE "email" = 'jonio9012@gmail.com'`)
	if err == nil {
		defer rowsE.Close()
		for rowsE.Next() {
			var id int64
			var tenantId, name, email, role, pinHash sql.NullString
			var outletId sql.NullInt64
			var isActive bool
			rowsE.Scan(&id, &tenantId, &outletId, &name, &email, &role, &pinHash, &isActive)
			fmt.Printf("id: %d | tenantId: %s | outletId: %v | name: %s | email: %s | role: %s | pinHash: %s | isActive: %t\n",
				id, tenantId.String, outletId, name.String, email.String, role.String, pinHash.String, isActive)
		}
	} else {
		fmt.Println("Error employees:", err)
	}
}
