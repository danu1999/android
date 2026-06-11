package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"strings"

	_ "github.com/lib/pq"
)

var db *sql.DB

func initDatabase(dbURL string) error {
	var err error
	db, err = sql.Open("postgres", dbURL)
	if err != nil {
		return fmt.Errorf("failed to open database: %w", err)
	}

	if err = db.Ping(); err != nil {
		return fmt.Errorf("failed to ping database: %w", err)
	}

	log.Println("Successfully connected to PostgreSQL database.")
	return initSchema()
}

func initSchema() error {
	queries := []string{
		`CREATE TABLE IF NOT EXISTS "local_users" (
			"googleSub" VARCHAR(255) PRIMARY KEY,
			"email" VARCHAR(255) UNIQUE NOT NULL,
			"displayName" VARCHAR(255),
			"photoUrl" VARCHAR(255),
			"role" VARCHAR(50) DEFAULT 'OWNER',
			"tenantId" VARCHAR(100),
			"whatsapp" VARCHAR(50),
			"isPremium" BOOLEAN DEFAULT FALSE,
			"businessModeLocked" BOOLEAN DEFAULT FALSE,
			"registeredAt" BIGINT,
			"updatedAt" BIGINT,
			"lastLoginAt" BIGINT,
			"isActive" BOOLEAN DEFAULT TRUE
		);`,
		`CREATE TABLE IF NOT EXISTS "tenants" (
			"id" VARCHAR(100) PRIMARY KEY,
			"name" VARCHAR(255) NOT NULL,
			"ownerEmail" VARCHAR(255),
			"businessMode" VARCHAR(50) DEFAULT 'FNB',
			"isActive" BOOLEAN DEFAULT TRUE,
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "outlets" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"name" VARCHAR(255) NOT NULL,
			"address" TEXT,
			"phone" VARCHAR(50),
			"isDefault" BOOLEAN DEFAULT FALSE,
			"isOpen" BOOLEAN DEFAULT TRUE,
			"currentEmployee" VARCHAR(150),
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "employees" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"outletId" INT,
			"name" VARCHAR(255) NOT NULL,
			"email" VARCHAR(255),
			"role" VARCHAR(50) DEFAULT 'KASIR',
			"pinHash" VARCHAR(255) NOT NULL,
			"salary" DOUBLE PRECISION DEFAULT 0,
			"isActive" BOOLEAN DEFAULT TRUE,
			"payPeriod" VARCHAR(50) DEFAULT 'MONTHLY',
			"lastPaidAt" BIGINT,
			"emailVerified" BOOLEAN DEFAULT FALSE,
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_clients" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"outletId" INT,
			"clientName" VARCHAR(255) NOT NULL,
			"saldoTitipan" DOUBLE PRECISION DEFAULT 0,
			"addressLine1" TEXT,
			"clientLogo" TEXT,
			"province" VARCHAR(100),
			"postalCode" VARCHAR(20),
			"phoneNumber" VARCHAR(50),
			"emailAddress" VARCHAR(150),
			"taxNumber" VARCHAR(50),
			"uniqueID" VARCHAR(100),
			"slug" VARCHAR(255),
			"receiverSignatureUrl" TEXT,
			"receiverNameActual" VARCHAR(255),
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_invoices" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"outletId" INT,
			"clientId" INT,
			"title" VARCHAR(255) NOT NULL,
			"number" VARCHAR(100) NOT NULL,
			"dueDate" BIGINT,
			"paymentTerms" VARCHAR(100) DEFAULT '14 days',
			"status" VARCHAR(50) DEFAULT 'DRAFT',
			"notes" TEXT,
			"totalAmount" DOUBLE PRECISION DEFAULT 0,
			"paidAmount" DOUBLE PRECISION DEFAULT 0,
			"uniqueID" VARCHAR(100),
			"slug" VARCHAR(255) UNIQUE NOT NULL,
			"receiverSignaturePath" TEXT,
			"receiverSignatureUrl" TEXT,
			"receiverNameActual" VARCHAR(255),
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_products" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"invoiceId" INT,
			"masterItemID" INT,
			"title" VARCHAR(255) NOT NULL,
			"unit" VARCHAR(50) DEFAULT 'pcs',
			"price" DOUBLE PRECISION DEFAULT 0,
			"jumlahLusin" DOUBLE PRECISION DEFAULT 1,
			"quantity" DOUBLE PRECISION DEFAULT 0,
			"isKhusus" BOOLEAN DEFAULT FALSE,
			"hargaBeli" DOUBLE PRECISION DEFAULT 0,
			"currency" VARCHAR(10) DEFAULT 'Rp',
			"uniqueID" VARCHAR(100),
			"slug" VARCHAR(255),
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_master_products" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"title" VARCHAR(255) NOT NULL,
			"description" TEXT,
			"unit" VARCHAR(50) DEFAULT 'Kg',
			"price" DOUBLE PRECISION DEFAULT 0,
			"beratGram" DOUBLE PRECISION DEFAULT 0,
			"cycleTime" DOUBLE PRECISION DEFAULT 0,
			"cavity" INT DEFAULT 1,
			"rejectRate" DOUBLE PRECISION DEFAULT 0,
			"uniqueID" VARCHAR(100),
			"slug" VARCHAR(255),
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_invoice_payments" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"invoiceId" INT NOT NULL,
			"paymentDate" BIGINT NOT NULL,
			"paymentAmount" DOUBLE PRECISION NOT NULL,
			"paymentMethod" VARCHAR(50) DEFAULT 'TRANSFER',
			"notes" TEXT,
			"createdAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_cashflow" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"transactionDate" BIGINT NOT NULL,
			"transactionType" VARCHAR(50) NOT NULL,
			"description" TEXT NOT NULL,
			"amount" DOUBLE PRECISION DEFAULT 0,
			"paymentRefId" INT,
			"createdAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_settings" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"clientName" VARCHAR(255) NOT NULL,
			"clientLogo" TEXT,
			"addressLine1" TEXT,
			"province" VARCHAR(100),
			"postalCode" VARCHAR(20),
			"phoneNumber" VARCHAR(50),
			"emailAddress" VARCHAR(150),
			"taxNumber" VARCHAR(50),
			"listrikBulanan" DOUBLE PRECISION DEFAULT 30000000,
			"jumlahMesin" INT DEFAULT 5,
			"jumlahKaryawan" INT DEFAULT 19,
			"gajiHarian" DOUBLE PRECISION DEFAULT 80000,
			"hariKerjaSebulan" INT DEFAULT 26,
			"biayaKarungPer1000" DOUBLE PRECISION DEFAULT 2100000,
			"hoursPerDay" INT DEFAULT 24,
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_employees" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"name" VARCHAR(255) NOT NULL,
			"position" VARCHAR(100),
			"salaryAmount" DOUBLE PRECISION DEFAULT 0,
			"isActive" BOOLEAN DEFAULT TRUE,
			"fingerprintPIN" VARCHAR(50),
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_payrolls" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"employeeId" INT NOT NULL,
			"paymentDate" BIGINT NOT NULL,
			"amount" DOUBLE PRECISION NOT NULL,
			"attendanceCount" INT DEFAULT 0,
			"dailyRate" DOUBLE PRECISION NOT NULL,
			"description" TEXT,
			"createdAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_bahan_baku" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"tanggal" BIGINT NOT NULL,
			"noTagihan" VARCHAR(100) NOT NULL,
			"totalHarga" DOUBLE PRECISION DEFAULT 0,
			"nominal" DOUBLE PRECISION DEFAULT 0,
			"notes" TEXT,
			"notaFotoPath" TEXT,
			"notaFotoUrl" TEXT,
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_bahan_baku_item" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"bahanBakuId" INT NOT NULL,
			"jenisBahan" VARCHAR(150) NOT NULL,
			"kuantitas" DOUBLE PRECISION DEFAULT 0,
			"unit" VARCHAR(50) DEFAULT 'Kg',
			"rate" DOUBLE PRECISION DEFAULT 0,
			"createdAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "print_settings" (
			"id" BIGINT PRIMARY KEY,
			"tenantId" VARCHAR(100) UNIQUE NOT NULL,
			"jpgUseLogo" BOOLEAN DEFAULT TRUE,
			"jpgHeaderAlign" VARCHAR(50) DEFAULT 'LEFT',
			"jpgUseSignature" BOOLEAN DEFAULT TRUE,
			"jpgSignatureSenderName" VARCHAR(100) DEFAULT 'Admin',
			"jpgSignatureReceiverName" VARCHAR(100) DEFAULT '',
			"jpgSignatureDrawnBase64" TEXT,
			"jpgIsColor" BOOLEAN DEFAULT TRUE,
			"sjUseLogo" BOOLEAN DEFAULT TRUE,
			"sjHeaderAlign" VARCHAR(50) DEFAULT 'LEFT',
			"sjUseSignature" BOOLEAN DEFAULT TRUE,
			"sjSignatureSenderName" VARCHAR(100) DEFAULT 'Admin',
			"sjSignatureReceiverName" VARCHAR(100) DEFAULT '',
			"sjSignatureDrawnBase64" TEXT,
			"sjIsColor" BOOLEAN DEFAULT FALSE,
			"invoiceUseLogo" BOOLEAN DEFAULT TRUE,
			"invoiceHeaderAlign" VARCHAR(50) DEFAULT 'LEFT',
			"invoiceUseSignature" BOOLEAN DEFAULT TRUE,
			"invoiceSignatureSenderName" VARCHAR(100) DEFAULT 'Admin',
			"invoiceSignatureReceiverName" VARCHAR(100) DEFAULT '',
			"invoiceSignatureDrawnBase64" TEXT,
			"invoiceIsColor" BOOLEAN DEFAULT TRUE,
			"receiptPaperWidth" VARCHAR(50) DEFAULT 'MM80',
			"receiptUseLogo" BOOLEAN DEFAULT TRUE,
			"receiptHeaderAlign" VARCHAR(50) DEFAULT 'CENTER',
			"receiptIsColor" BOOLEAN DEFAULT FALSE,
			"receiptShowItemPrice" BOOLEAN DEFAULT TRUE,
			"receiptFooterText" VARCHAR(255) DEFAULT 'Terima kasih sudah berbelanja!',
			"bankOwnerName" VARCHAR(100) DEFAULT '',
			"bankName" VARCHAR(50) DEFAULT 'BCA',
			"bankAccountNumber" VARCHAR(100) DEFAULT '',
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "products" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"outletId" INT,
			"name" VARCHAR(255) NOT NULL,
			"price" DOUBLE PRECISION NOT NULL,
			"costPrice" DOUBLE PRECISION DEFAULT 0,
			"stock" INT DEFAULT 0,
			"unit" VARCHAR(50) DEFAULT 'pcs',
			"barcode" VARCHAR(100),
			"category" VARCHAR(100) DEFAULT 'Umum',
			"wholesaleEnabled" BOOLEAN DEFAULT FALSE,
			"wholesalePrices" TEXT,
			"variants" TEXT,
			"image" TEXT,
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "customers" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"name" VARCHAR(255) NOT NULL,
			"phone" VARCHAR(50),
			"address" TEXT,
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "transactions" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"outletId" INT,
			"employeeId" INT NOT NULL,
			"customerId" INT,
			"customerName" VARCHAR(255),
			"receiptNumber" VARCHAR(100) UNIQUE NOT NULL,
			"date" BIGINT NOT NULL,
			"subtotal" DOUBLE PRECISION DEFAULT 0,
			"discountType" VARCHAR(50),
			"discountInput" DOUBLE PRECISION DEFAULT 0,
			"discountAmt" DOUBLE PRECISION DEFAULT 0,
			"total" DOUBLE PRECISION NOT NULL,
			"discount" DOUBLE PRECISION DEFAULT 0,
			"paymentMethod" VARCHAR(50) NOT NULL,
			"amountPaid" DOUBLE PRECISION,
			"change" DOUBLE PRECISION,
			"status" VARCHAR(50) DEFAULT 'COMPLETED',
			"type" VARCHAR(50) DEFAULT 'SALES',
			"orderStatus" VARCHAR(50),
			"dpAmount" DOUBLE PRECISION DEFAULT 0,
			"deliveryDate" BIGINT,
			"queueNumber" INT,
			"notes" TEXT,
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "transaction_items" (
			"id" INT PRIMARY KEY,
			"transactionId" INT NOT NULL,
			"productId" INT NOT NULL,
			"variantId" INT,
			"variantName" VARCHAR(255),
			"quantity" INT NOT NULL,
			"price" DOUBLE PRECISION NOT NULL,
			"costPrice" DOUBLE PRECISION DEFAULT 0,
			"discount" DOUBLE PRECISION DEFAULT 0,
			"note" TEXT
		);`,
		`CREATE TABLE IF NOT EXISTS "activity_logs" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"action" VARCHAR(100) NOT NULL,
			"description" TEXT NOT NULL,
			"date" BIGINT NOT NULL,
			"employeeName" VARCHAR(255) NOT NULL,
			"appMode" VARCHAR(50) DEFAULT 'FNB'
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_adms_devices" (
			"id" SERIAL PRIMARY KEY,
			"serialNumber" VARCHAR(100) UNIQUE NOT NULL,
			"alias" VARCHAR(100),
			"lastActivity" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
			"createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_attendance_logs" (
			"id" SERIAL PRIMARY KEY,
			"deviceSN" VARCHAR(100),
			"employeePIN" VARCHAR(50),
			"verifyType" INT DEFAULT 0,
			"verifyState" INT DEFAULT 0,
			"logTime" TIMESTAMP NOT NULL,
			"checkOutTime" TIMESTAMP,
			"workDate" TIMESTAMP NOT NULL,
			"lateMinutes" INT DEFAULT 0,
			"alasan" VARCHAR(255),
			"createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_device_tenants" (
			"id" SERIAL PRIMARY KEY,
			"serialNumber" VARCHAR(100) UNIQUE NOT NULL,
			"tenantId" VARCHAR(100) NOT NULL,
			"createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
		);`,
	}

	for _, q := range queries {
		if _, err := db.Exec(q); err != nil {
			return fmt.Errorf("error executing migration query: %w\nQuery: %s", err, q)
		}
	}

	log.Println("Database schemas verified / migrated successfully.")
	return nil
}

func dynamicUpsert(tableName string, rows []map[string]interface{}) error {
	if len(rows) == 0 {
		return nil
	}

	pkColumn := "id"
	if tableName == "local_users" {
		pkColumn = "googleSub"
	} else if tableName == "print_settings" {
		pkColumn = "tenantId"
	}

	for _, row := range rows {
		var columns []string
		var placeholders []string
		var values []interface{}
		var updateParts []string

		idx := 1
		for k, v := range row {
			columns = append(columns, fmt.Sprintf(`"%s"`, k))
			placeholders = append(placeholders, fmt.Sprintf("$%d", idx))

			switch val := v.(type) {
			case map[string]interface{}, []interface{}:
				jsonBytes, _ := json.Marshal(val)
				values = append(values, string(jsonBytes))
			default:
				values = append(values, v)
			}

			if k != pkColumn {
				updateParts = append(updateParts, fmt.Sprintf(`"%s" = EXCLUDED."%s"`, k, k))
			}
			idx++
		}

		query := fmt.Sprintf(
			`INSERT INTO "%s" (%s) VALUES (%s)`,
			tableName,
			strings.Join(columns, ", "),
			strings.Join(placeholders, ", "),
		)

		if len(updateParts) > 0 {
			query += fmt.Sprintf(` ON CONFLICT ("%s") DO UPDATE SET %s`, pkColumn, strings.Join(updateParts, ", "))
		} else {
			query += fmt.Sprintf(` ON CONFLICT ("%s") DO NOTHING`, pkColumn)
		}

		_, err := db.Exec(query, values...)
		if err != nil {
			log.Printf("Error upserting into %s: %v. Query: %s", tableName, err, query)
			return err
		}
	}
	return nil
}
