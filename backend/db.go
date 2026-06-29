package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
	"time"

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
		`CREATE TABLE IF NOT EXISTS "tenant_sync_status" (
			"tenantId" VARCHAR(100) PRIMARY KEY,
			"lastUpdated" BIGINT NOT NULL
		);`,
		`CREATE TABLE IF NOT EXISTS "deleted_users" (
			"email" VARCHAR(255) PRIMARY KEY,
			"status" VARCHAR(50) NOT NULL DEFAULT 'DELETED',
			"updatedAt" BIGINT NOT NULL
		);`,
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
			"isActive" BOOLEAN DEFAULT TRUE,
			"demoEmailSent" BOOLEAN DEFAULT FALSE,
			"demoDay2Notified" BOOLEAN DEFAULT FALSE,
			"apkVersion" VARCHAR(50) DEFAULT '2.5.0'
		);`,
		`ALTER TABLE "local_users" ADD COLUMN IF NOT EXISTS "demoEmailSent" BOOLEAN DEFAULT FALSE;`,
		`ALTER TABLE "local_users" ADD COLUMN IF NOT EXISTS "demoDay2Notified" BOOLEAN DEFAULT FALSE;`,
		`ALTER TABLE "local_users" ADD COLUMN IF NOT EXISTS "apkVersion" VARCHAR(50) DEFAULT '2.5.0';`,
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
			"phone" VARCHAR(50),
			"salary" DOUBLE PRECISION DEFAULT 0,
			"isActive" BOOLEAN DEFAULT TRUE,
			"payPeriod" VARCHAR(50) DEFAULT 'MONTHLY',
			"lastPaidAt" BIGINT,
			"emailVerified" BOOLEAN DEFAULT FALSE,
			"passwordChangeCount" INT DEFAULT 0,
			"lastPasswordChangeDate" BIGINT DEFAULT 0,
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
			"updatedAt" BIGINT,
			"isDeleted" BOOLEAN DEFAULT FALSE
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
			"updatedAt" BIGINT,
			"isDeleted" BOOLEAN DEFAULT FALSE
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_products" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"invoiceId" INT,
			"masterItemID" INT,
			"title" VARCHAR(255) NOT NULL,
			"description" TEXT,
			"unit" VARCHAR(50) DEFAULT 'pcs',
			"price" DOUBLE PRECISION DEFAULT 0,
			"jumlahLusin" DOUBLE PRECISION DEFAULT 1,
			"quantity" DOUBLE PRECISION DEFAULT 0,
			"isKhusus" BOOLEAN DEFAULT FALSE,
			"hargaBeli" DOUBLE PRECISION DEFAULT 0,
			"currency" VARCHAR(10) DEFAULT 'Rp',
			"uniqueID" VARCHAR(100),
			"slug" VARCHAR(255),
			"isSynced" BOOLEAN NOT NULL DEFAULT TRUE,
			"isDeleted" BOOLEAN NOT NULL DEFAULT FALSE,
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_master_products" (
			"id" INT,
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
			"jenisBahanBaku" VARCHAR(100) DEFAULT '',
			"image" TEXT,
			"createdAt" BIGINT,
			"updatedAt" BIGINT,
			PRIMARY KEY ("id", "tenantId")
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_invoice_payments" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"invoiceId" INT NOT NULL,
			"paymentDate" BIGINT NOT NULL,
			"paymentAmount" DOUBLE PRECISION NOT NULL,
			"paymentMethod" VARCHAR(50) DEFAULT 'TRANSFER',
			"notes" TEXT,
			"createdAt" BIGINT,
			"isDeleted" BOOLEAN DEFAULT FALSE
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_cashflow" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"transactionDate" BIGINT NOT NULL,
			"transactionType" VARCHAR(50) NOT NULL,
			"description" TEXT NOT NULL,
			"amount" DOUBLE PRECISION DEFAULT 0,
			"costType" VARCHAR(50) DEFAULT 'OPERATING_EXPENSE',
			"paymentRefId" INT,
			"createdAt" BIGINT,
			"isDeleted" BOOLEAN DEFAULT FALSE
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
			"employeeId" INT,
			"createdAt" BIGINT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_payrolls" (
			"id" VARCHAR(100) NOT NULL,
			"tenantId" VARCHAR(100) NOT NULL,
			"employeeId" INT NOT NULL,
			"paymentDate" BIGINT NOT NULL,
			"amount" DOUBLE PRECISION NOT NULL,
			"attendanceCount" INT DEFAULT 0,
			"dailyRate" DOUBLE PRECISION NOT NULL,
			"description" TEXT,
			"createdAt" BIGINT,
			PRIMARY KEY ("id", "tenantId")
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
			"updatedAt" BIGINT,
			"isDeleted" BOOLEAN DEFAULT FALSE
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_bahan_baku_item" (
			"id" INT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"bahanBakuId" INT NOT NULL,
			"jenisBahan" VARCHAR(150) NOT NULL,
			"kuantitas" DOUBLE PRECISION DEFAULT 0,
			"unit" VARCHAR(50) DEFAULT 'Kg',
			"rate" DOUBLE PRECISION DEFAULT 0,
			"createdAt" BIGINT,
			"isDeleted" BOOLEAN DEFAULT FALSE
		);`,
		`CREATE TABLE IF NOT EXISTS "print_settings" (
			"id" BIGINT PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"moduleKey" VARCHAR(50) DEFAULT 'BMP',
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
			"jpgTemplateType" VARCHAR(50) DEFAULT 'MODERN',
			"sjTemplateType" VARCHAR(50) DEFAULT 'MODERN',
			"invoiceTemplateType" VARCHAR(50) DEFAULT 'MODERN',
			"bankOwnerName" VARCHAR(100) DEFAULT '',
			"bankName" VARCHAR(50) DEFAULT 'BCA',
			"bankAccountNumber" VARCHAR(100) DEFAULT '',
			"logoPath" TEXT,
			"createdAt" BIGINT,
			"updatedAt" BIGINT,
			CONSTRAINT "print_settings_tenantId_moduleKey_key" UNIQUE ("tenantId", "moduleKey")
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
		`CREATE TABLE IF NOT EXISTS "system_admins" (
			"email" VARCHAR(255) PRIMARY KEY,
			"passwordHash" VARCHAR(255) NOT NULL,
			"createdAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "apk_config" (
			"id" INT PRIMARY KEY,
			"version" VARCHAR(50) NOT NULL,
			"description" TEXT,
			"downloadUrl" TEXT,
			"updatedAt" BIGINT
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_product_stocks" (
			"id" INT,
			"tenantId" VARCHAR(100) NOT NULL,
			"masterProductId" INT NOT NULL,
			"quantity" DOUBLE PRECISION DEFAULT 0,
			"minStockAlert" DOUBLE PRECISION DEFAULT 0,
			"isSynced" BOOLEAN DEFAULT TRUE,
			"isDeleted" BOOLEAN DEFAULT FALSE,
			"updatedAt" BIGINT,
			PRIMARY KEY ("id", "tenantId"),
			CONSTRAINT "bmp_product_stocks_masterProductId_tenantId_key" UNIQUE ("masterProductId", "tenantId")
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_stock_ledger" (
			"id" INT,
			"tenantId" VARCHAR(100) NOT NULL,
			"masterProductId" INT NOT NULL,
			"referenceId" INT NOT NULL,
			"mutationType" VARCHAR(50) NOT NULL,
			"quantityChange" DOUBLE PRECISION NOT NULL,
			"finalStock" DOUBLE PRECISION NOT NULL,
			"notes" TEXT,
			"isSynced" BOOLEAN DEFAULT TRUE,
			"isDeleted" BOOLEAN DEFAULT FALSE,
			"createdAt" BIGINT,
			PRIMARY KEY ("id", "tenantId")
		);`,
		`CREATE TABLE IF NOT EXISTS "bmp_production_logs" (
			"id" INT,
			"tenantId" VARCHAR(100) NOT NULL,
			"masterProductId" INT NOT NULL,
			"quantityProduced" DOUBLE PRECISION NOT NULL,
			"quantityRejected" DOUBLE PRECISION NOT NULL,
			"rawMaterialUsedKg" DOUBLE PRECISION NOT NULL,
			"operatorName" VARCHAR(255),
			"productionDate" BIGINT,
			"isSynced" BOOLEAN DEFAULT TRUE,
			"isDeleted" BOOLEAN DEFAULT FALSE,
			"createdAt" BIGINT,
			PRIMARY KEY ("id", "tenantId")
		);`,
	}

	for _, q := range queries {
		if _, err := db.Exec(q); err != nil {
			return fmt.Errorf("error executing migration query: %w\nQuery: %s", err, q)
		}
	}

	// Interconnect hanafiariful@gmail.com and fahrup22@gmail.com legacy to premium
	migrationQueries := []string{
		`INSERT INTO "tenants" ("id", "name", "ownerEmail", "businessMode", "isActive", "createdAt", "updatedAt")
		VALUES ('ten_premium_hanafiariful_gmail_com', 'PISANG KEJU RAMAYANA', 'hanafiariful@gmail.com', 'FNB', true, 1685642632000, 1685642632000)
		ON CONFLICT ("id") DO UPDATE SET "ownerEmail" = EXCLUDED."ownerEmail", "businessMode" = EXCLUDED."businessMode";`,

		`INSERT INTO "local_users" ("googleSub", "email", "displayName", "role", "tenantId", "isPremium", "isActive", "registeredAt", "updatedAt")
		VALUES ('hanafiariful@gmail.com', 'hanafiariful@gmail.com', 'PISANG KEJU RAMAYANA', 'OWNER', 'ten_premium_hanafiariful_gmail_com', true, true, 1685642632000, 1685642632000)
		ON CONFLICT ("googleSub") DO UPDATE SET "tenantId" = EXCLUDED."tenantId", "isPremium" = EXCLUDED."isPremium", "isActive" = EXCLUDED."isActive";`,

		`INSERT INTO "local_users" ("googleSub", "email", "displayName", "role", "tenantId", "isPremium", "isActive", "registeredAt", "updatedAt")
		VALUES ('fahrup22@gmail.com', 'fahrup22@gmail.com', 'FahriP', 'ADMIN', 'ten_premium_hanafiariful_gmail_com', true, true, 1685642632000, 1685642632000)
		ON CONFLICT ("googleSub") DO UPDATE SET "tenantId" = EXCLUDED."tenantId", "isPremium" = EXCLUDED."isPremium", "isActive" = EXCLUDED."isActive";`,

		`INSERT INTO "employees" ("id", "tenantId", "name", "email", "role", "pinHash", "isActive", "createdAt", "updatedAt")
		VALUES (10001, 'ten_premium_hanafiariful_gmail_com', 'PISANG KEJU RAMAYANA', 'hanafiariful@gmail.com', 'OWNER', '20710a82f8d6b458af10d49fbb1f985ac8aaf696e6b32e776d4f4ebbc30d08565e2bb5e1902ace18297d8db47ad35e49c086669125b1d6ac867c0d2d7e265e50', true, 1685642632000, 1685642632000)
		ON CONFLICT ("id", "tenantId") DO UPDATE SET "role" = EXCLUDED."role", "pinHash" = EXCLUDED."pinHash", "isActive" = EXCLUDED."isActive";`,

		`INSERT INTO "employees" ("id", "tenantId", "name", "email", "role", "pinHash", "isActive", "createdAt", "updatedAt")
		VALUES (10002, 'ten_premium_hanafiariful_gmail_com', 'FahriP', 'fahrup22@gmail.com', 'ADMIN', '63e71711d1481b6da8b756e114aa2ac71a704929c0accf46f419706a5c1416ae1a312899ae84d3d8e33d255811e98fd4d17e59371a08e2f9c21c01d1b1c13a8d', true, 1685642632000, 1685642632000)
		ON CONFLICT ("id", "tenantId") DO UPDATE SET "role" = EXCLUDED."role", "pinHash" = EXCLUDED."pinHash", "isActive" = EXCLUDED."isActive";`,

		`UPDATE "bmp_payrolls" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "bmp_clients" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "bmp_invoices" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "bmp_master_products" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "bmp_cashflow" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "bmp_settings" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "bmp_employees" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "bmp_bahan_baku" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "print_settings" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "products" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "customers" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "transactions" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "activity_logs" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "employees" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "outlets" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "bmp_device_tenants" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "bmp_products" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "bmp_invoice_payments" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,
		`UPDATE "bmp_bahan_baku_item" SET "tenantId" = 'ten_premium_hanafiariful_gmail_com' WHERE "tenantId" = 'hanafiariful@gmail.com';`,

		// Interconnect bahteramulyap@gmail.com and syerlirahma7@gmail.com legacy to premium
		`INSERT INTO "tenants" ("id", "name", "ownerEmail", "businessMode", "isActive", "createdAt", "updatedAt")
		VALUES ('ten_premium_bahteramulyap_gmail_com', 'CV. BAHTERA MULYA PLASTIK', 'bahteramulyap@gmail.com', 'BMP', true, 1685642632000, 1685642632000)
		ON CONFLICT ("id") DO UPDATE SET "ownerEmail" = EXCLUDED."ownerEmail", "businessMode" = EXCLUDED."businessMode";`,

		`INSERT INTO "local_users" ("googleSub", "email", "displayName", "role", "tenantId", "isPremium", "isActive", "registeredAt", "updatedAt")
		VALUES ('bahteramulyap@gmail.com', 'bahteramulyap@gmail.com', 'CV. BAHTERA MULYA PLASTIK', 'OWNER', 'ten_premium_bahteramulyap_gmail_com', true, true, 1685642632000, 1685642632000)
		ON CONFLICT ("googleSub") DO UPDATE SET "tenantId" = EXCLUDED."tenantId", "isPremium" = EXCLUDED."isPremium", "isActive" = EXCLUDED."isActive";`,

		`INSERT INTO "employees" ("id", "tenantId", "name", "email", "role", "pinHash", "isActive", "createdAt", "updatedAt")
		VALUES (20001, 'ten_premium_bahteramulyap_gmail_com', 'CV. BAHTERA MULYA PLASTIK', 'bahteramulyap@gmail.com', 'OWNER', '8a0ff1f8926195dfde55af7e68c028591602dacc30dc3c7caef27a949ca45142b25514004cf4540c46eca830100d06517c6facc0faf77fc57140e9df5fe5ffc7', true, 1685642632000, 1685642632000)
		ON CONFLICT ("id", "tenantId") DO UPDATE SET "role" = EXCLUDED."role", "pinHash" = EXCLUDED."pinHash", "isActive" = EXCLUDED."isActive";`,

		// PlayStore Review Premium Account Insertion
		`INSERT INTO "tenants" ("id", "name", "ownerEmail", "businessMode", "isActive", "createdAt", "updatedAt")
		VALUES ('ten_premium_playstoretest_gmail_com', 'PlayStore Review (Premium)', 'playstoretest@gmail.com', 'FNB', true, 1685642632000, 1685642632000)
		ON CONFLICT ("id") DO UPDATE SET "ownerEmail" = EXCLUDED."ownerEmail", "businessMode" = EXCLUDED."businessMode";`,

		`INSERT INTO "local_users" ("googleSub", "email", "displayName", "role", "tenantId", "isPremium", "isActive", "registeredAt", "updatedAt")
		VALUES ('playstoretest@gmail.com', 'playstoretest@gmail.com', 'PlayStore Test', 'OWNER', 'ten_premium_playstoretest_gmail_com', true, true, 1685642632000, 1685642632000)
		ON CONFLICT ("googleSub") DO UPDATE SET "tenantId" = EXCLUDED."tenantId", "isPremium" = EXCLUDED."isPremium", "isActive" = EXCLUDED."isActive";`,

		`INSERT INTO "employees" ("id", "tenantId", "name", "email", "role", "pinHash", "isActive", "createdAt", "updatedAt")
		VALUES (10005, 'ten_premium_playstoretest_gmail_com', 'PlayStore Reviewer', 'playstoretest@gmail.com', 'OWNER', 'f93226ab6fd88288603a9ea14137015f3667f84ea23e34c32fad092883b3994546a681e423e9c1d087a5ea6f7238dcf8d3b7a27b93d2315addfde043c01cbf1a', true, 1685642632000, 1685642632000)
		ON CONFLICT ("id", "tenantId") DO UPDATE SET "role" = EXCLUDED."role", "pinHash" = EXCLUDED."pinHash", "isActive" = EXCLUDED."isActive";`,

		`UPDATE "bmp_payrolls" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "bmp_clients" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "bmp_invoices" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "bmp_master_products" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "bmp_cashflow" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "bmp_settings" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "bmp_employees" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "bmp_bahan_baku" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "print_settings" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "products" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "customers" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "transactions" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "activity_logs" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "employees" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "outlets" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "bmp_device_tenants" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "bmp_products" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "bmp_invoice_payments" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,
		`UPDATE "bmp_bahan_baku_item" SET "tenantId" = 'ten_premium_bahteramulyap_gmail_com' WHERE "tenantId" = 'bahteramulyap@gmail.com';`,

		// print_settings schema updates
		`ALTER TABLE "print_settings" ADD COLUMN IF NOT EXISTS "moduleKey" VARCHAR(50) DEFAULT 'BMP';`,
		`ALTER TABLE "print_settings" ADD COLUMN IF NOT EXISTS "logoPath" TEXT;`,
		`ALTER TABLE "print_settings" ADD COLUMN IF NOT EXISTS "jpgTemplateType" VARCHAR(50) DEFAULT 'MODERN';`,
		`ALTER TABLE "print_settings" ADD COLUMN IF NOT EXISTS "sjTemplateType" VARCHAR(50) DEFAULT 'MODERN';`,
		`ALTER TABLE "print_settings" ADD COLUMN IF NOT EXISTS "invoiceTemplateType" VARCHAR(50) DEFAULT 'MODERN';`,
		`ALTER TABLE "print_settings" DROP CONSTRAINT IF EXISTS "print_settings_tenantId_key";`,
		`ALTER TABLE "print_settings" DROP CONSTRAINT IF EXISTS "print_settings_tenantId_moduleKey_key";`,
		`ALTER TABLE "print_settings" ADD CONSTRAINT "print_settings_tenantId_moduleKey_key" UNIQUE ("tenantId", "moduleKey");`,

		// products schema updates for minStockAlert
		`ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "minStockAlert" INT DEFAULT 0;`,

		// v2.17.47: Sinkronisasi POS realtime — Android client mengirim kolom isSynced/isDeleted
		// untuk setiap row upsert. Tabel berikut sebelumnya menolak payload karena kolom belum ada,
		// sehingga produk yang ditambah karyawan outlet tidak pernah ter-upload ke VPS.
		// ADD COLUMN IF NOT EXISTS aman & idempotent: tidak ada efek jika kolom sudah ada.
		`ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "customers" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "customers" ADD COLUMN IF NOT EXISTS "outletId" INT;`,
		`ALTER TABLE "transactions" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "transactions" ADD COLUMN IF NOT EXISTS "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "transaction_items" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "transaction_items" ADD COLUMN IF NOT EXISTS "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "activity_logs" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "bmp_master_products" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "bmp_master_products" ADD COLUMN IF NOT EXISTS "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "outlets" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "outlets" ADD COLUMN IF NOT EXISTS "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "employees" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "employees" ADD COLUMN IF NOT EXISTS "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "tenants" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "print_settings" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "employeeId" INT;`,
		`ALTER TABLE "bmp_attendance_logs" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,

		// bmp tables isSynced schema updates
		`ALTER TABLE "bmp_clients" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "bmp_invoices" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "bmp_invoice_payments" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "bmp_cashflow" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "bmp_payrolls" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "bmp_payrolls" ALTER COLUMN "id" TYPE VARCHAR(100);`,
		`ALTER TABLE "bmp_bahan_baku" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT FALSE;`,
		`ALTER TABLE "bmp_bahan_baku_item" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT FALSE;`,

		// Drop single PKs and create composite PKs (id, tenantId) for multi-tenant tables
		`UPDATE "outlets" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "outlets" DROP CONSTRAINT IF EXISTS "outlets_pkey";`,
		`ALTER TABLE "outlets" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "employees" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "employees" DROP CONSTRAINT IF EXISTS "employees_pkey";`,
		`ALTER TABLE "employees" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "bmp_clients" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "bmp_clients" DROP CONSTRAINT IF EXISTS "bmp_clients_pkey";`,
		`ALTER TABLE "bmp_clients" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "bmp_invoices" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "bmp_invoices" DROP CONSTRAINT IF EXISTS "bmp_invoices_pkey";`,
		`ALTER TABLE "bmp_invoices" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "bmp_products" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "bmp_products" DROP CONSTRAINT IF EXISTS "bmp_products_pkey";`,
		`ALTER TABLE "bmp_products" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "bmp_master_products" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "bmp_master_products" DROP CONSTRAINT IF EXISTS "bmp_master_products_pkey";`,
		`ALTER TABLE "bmp_master_products" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "bmp_invoice_payments" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "bmp_invoice_payments" DROP CONSTRAINT IF EXISTS "bmp_invoice_payments_pkey";`,
		`ALTER TABLE "bmp_invoice_payments" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "bmp_cashflow" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "bmp_cashflow" DROP CONSTRAINT IF EXISTS "bmp_cashflow_pkey";`,
		`ALTER TABLE "bmp_cashflow" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "bmp_settings" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "bmp_settings" DROP CONSTRAINT IF EXISTS "bmp_settings_pkey";`,
		`ALTER TABLE "bmp_settings" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "bmp_employees" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "bmp_employees" DROP CONSTRAINT IF EXISTS "bmp_employees_pkey";`,
		`ALTER TABLE "bmp_employees" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "bmp_payrolls" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "bmp_payrolls" DROP CONSTRAINT IF EXISTS "bmp_payrolls_pkey";`,
		`ALTER TABLE "bmp_payrolls" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "bmp_bahan_baku" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "bmp_bahan_baku" DROP CONSTRAINT IF EXISTS "bmp_bahan_baku_pkey";`,
		`ALTER TABLE "bmp_bahan_baku" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "bmp_bahan_baku_item" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "bmp_bahan_baku_item" DROP CONSTRAINT IF EXISTS "bmp_bahan_baku_item_pkey";`,
		`ALTER TABLE "bmp_bahan_baku_item" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "products" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "products" DROP CONSTRAINT IF EXISTS "products_pkey";`,
		`ALTER TABLE "products" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "customers" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "customers" DROP CONSTRAINT IF EXISTS "customers_pkey";`,
		`ALTER TABLE "customers" ADD PRIMARY KEY ("id", "tenantId");`,

		`UPDATE "transactions" SET "tenantId" = 'demo_tenant' WHERE "tenantId" IS NULL OR "tenantId" = '';`,
		`ALTER TABLE "transactions" DROP CONSTRAINT IF EXISTS "transactions_pkey";`,
		`ALTER TABLE "transactions" ADD PRIMARY KEY ("id", "tenantId");`,

		`ALTER TABLE "employees" ADD COLUMN IF NOT EXISTS "phone" VARCHAR(50);`,
		`ALTER TABLE "employees" ADD COLUMN IF NOT EXISTS "passwordChangeCount" INT DEFAULT 0;`,
		`ALTER TABLE "employees" ADD COLUMN IF NOT EXISTS "lastPasswordChangeDate" BIGINT DEFAULT 0;`,

		// v2.3.0: Hapus duplikasi karyawan dengan email typo "alfarisirosi04"
		// (email aktif yang benar adalah "alfarisirosi40@gmail.com")
		`DELETE FROM "employees" WHERE TRIM(LOWER("email")) = 'alfarisirosi04@gmail.com';`,

		// v2.3.0: Tambah index outletId pada transactions untuk query margin per outlet lebih cepat
		`CREATE INDEX IF NOT EXISTS "idx_transactions_outlet" ON "transactions" ("outletId", "tenantId");`,
		`CREATE INDEX IF NOT EXISTS "idx_transaction_items_tx" ON "transaction_items" ("transactionId");`,
		// v2.17.3: Tambah kolom jenisBahanBaku pada master produk & composite PK master products jika belum
		`ALTER TABLE "bmp_master_products" ADD COLUMN IF NOT EXISTS "jenisBahanBaku" VARCHAR(100) DEFAULT '';`,
		`ALTER TABLE "bmp_master_products" DROP CONSTRAINT IF EXISTS "bmp_master_products_pkey";`,
		`ALTER TABLE "bmp_master_products" ADD PRIMARY KEY ("id", "tenantId");`,

		// v2.17.9: Tambah kolom image pada bmp_master_products
		`ALTER TABLE "bmp_master_products" ADD COLUMN IF NOT EXISTS "image" TEXT;`,

		// v2.17.58: Tambah kolom supplier pada bmp_bahan_baku untuk relasi pemasok
		`ALTER TABLE "bmp_bahan_baku" ADD COLUMN IF NOT EXISTS "supplier" VARCHAR(255);`,

		// v2.17.6: Ubah primary key print_settings menjadi composite (id, tenantId) untuk mencegah duplikasi key error
		`ALTER TABLE "print_settings" DROP CONSTRAINT IF EXISTS "print_settings_pkey";`,
		`ALTER TABLE "print_settings" ADD PRIMARY KEY ("id", "tenantId");`,

		// v2.17.7: Set isPremium = TRUE untuk mulyakus84@gmail.com di database server
		`UPDATE "local_users" SET "isPremium" = TRUE WHERE TRIM(LOWER("email")) = 'mulyakus84@gmail.com';`,

		// v2.17.8: Rename demo Laundry tenant to premium for mulyakus84@gmail.com
		`INSERT INTO "tenants" ("id", "name", "ownerEmail", "businessMode", "isActive", "createdAt", "updatedAt")
		 SELECT 'ten_premium_mulyakus84_gmail_com_LAUNDRY', 'CV. Aku&dia Bersama (Laundry)', "ownerEmail", "businessMode", "isActive", "createdAt", "updatedAt"
		 FROM "tenants" WHERE "id" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY'
		 ON CONFLICT ("id") DO UPDATE SET "name" = EXCLUDED."name";`,
		`UPDATE "local_users" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "outlets" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "employees" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_clients" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_invoices" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_products" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_product_stocks" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_master_products" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_invoice_payments" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_cashflow" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_settings" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_employees" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_payrolls" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_bahan_baku" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_bahan_baku_item" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "print_settings" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "products" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "customers" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "transactions" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "activity_logs" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_device_tenants" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_stock_ledger" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`UPDATE "bmp_production_logs" SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,
		`DELETE FROM "tenants" WHERE "id" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';`,

		// Real-time optimization performance indexes
		`CREATE INDEX IF NOT EXISTS idx_products_tenant_outlet   ON "products"       ("tenantId", "outletId");`,
		`CREATE INDEX IF NOT EXISTS idx_customers_tenant_outlet  ON "customers"       ("tenantId", "outletId");`,
		`CREATE INDEX IF NOT EXISTS idx_transactions_tenant_date ON "transactions"    ("tenantId", "date" DESC);`,
		`CREATE INDEX IF NOT EXISTS idx_bmp_invoices_tenant_date ON "bmp_invoices"   ("tenantId", "createdAt" DESC);`,
		`CREATE INDEX IF NOT EXISTS idx_bmp_clients_tenant       ON "bmp_clients"    ("tenantId");`,
		`CREATE INDEX IF NOT EXISTS idx_bmp_cashflow_tenant_date ON "bmp_cashflow"   ("tenantId", "transactionDate" DESC);`,
		`CREATE INDEX IF NOT EXISTS idx_bmp_products_invoice     ON "bmp_products"   ("invoiceId", "tenantId");`,
		`CREATE INDEX IF NOT EXISTS idx_bmp_payrolls_employee    ON "bmp_payrolls"   ("employeeId", "tenantId");`,
		`CREATE INDEX IF NOT EXISTS idx_bmp_bahan_tenant_date    ON "bmp_bahan_baku" ("tenantId", "tanggal" DESC);`,
		`CREATE INDEX IF NOT EXISTS idx_employees_tenant_outlet  ON "employees"      ("tenantId", "outletId");`,
	}

	for _, q := range migrationQueries {
		_, _ = db.Exec(q)
	}

	// Seed default system admin: muhammadmuizz8@gmail.com
	defaultAdminHash := hashPassword("AdminBahtera123!")
	_, _ = db.Exec(`INSERT INTO "system_admins" ("email", "passwordHash", "createdAt")
		VALUES ('muhammadmuizz8@gmail.com', $1, $2)
		ON CONFLICT ("email") DO UPDATE SET "passwordHash" = EXCLUDED."passwordHash";`, defaultAdminHash, time.Now().UnixNano()/int64(time.Millisecond))


	// Seed default apk_config — v2.17.56
	_, _ = db.Exec(`INSERT INTO "apk_config" ("id", "version", "description", "downloadUrl", "updatedAt")
		VALUES (1, '2.17.56', 'Pembaruan Keamanan & Validasi Versi Aplikasi: 1. Validasi Versi Global: Peningkatan validasi x-client-version secara terpusat pada seluruh API endpoint VPS POSBah (POS, Invoice, & Manufaktur) untuk mencegah penggunaan aplikasi versi lama. 2. Penanganan Error yang Lebih Baik: Menolak otentikasi login pada aplikasi versi lama secara aman dengan menyajikan instruksi pembaruan langsung.', '/api/download-apk', $1)
		ON CONFLICT ("id") DO UPDATE SET "version" = EXCLUDED."version", "description" = EXCLUDED."description", "updatedAt" = EXCLUDED."updatedAt";`, time.Now().UnixNano()/int64(time.Millisecond))
	
	// Protect syerlirahma7@gmail.com: mark as ACTIVE in deleted_users so she is never purged again
	_, _ = db.Exec(`INSERT INTO "deleted_users" ("email", "status", "updatedAt")
		VALUES ('syerlirahma7@gmail.com', 'ACTIVE', $1)
		ON CONFLICT ("email") DO UPDATE SET "status" = 'ACTIVE', "updatedAt" = EXCLUDED."updatedAt";`,
		time.Now().UnixNano()/int64(time.Millisecond))

	// Protect bahteramulyap@gmail.com: mark as ACTIVE in deleted_users so she is never blocked or purged
	_, _ = db.Exec(`INSERT INTO "deleted_users" ("email", "status", "updatedAt")
		VALUES ('bahteramulyap@gmail.com', 'ACTIVE', $1)
		ON CONFLICT ("email") DO UPDATE SET "status" = 'ACTIVE', "updatedAt" = EXCLUDED."updatedAt";`,
		time.Now().UnixNano()/int64(time.Millisecond))

	// Ensure bahteramulyap@gmail.com is active in local_users
	_, _ = db.Exec(`UPDATE "local_users" SET "isActive" = TRUE, "updatedAt" = $1 WHERE TRIM(LOWER("email")) = 'bahteramulyap@gmail.com'`,
		time.Now().UnixNano()/int64(time.Millisecond))

	// Ensure bahteramulyap@gmail.com is active in employees
	_, _ = db.Exec(`UPDATE "employees" SET "isActive" = TRUE, "updatedAt" = $1 WHERE TRIM(LOWER("email")) = 'bahteramulyap@gmail.com'`,
		time.Now().UnixNano()/int64(time.Millisecond))

	// Deduplicate employees table to resolve duplicate/conflict rows (keeping correct ones)
	_, errDeduplicate := db.Exec(`
		WITH duplicate_employees AS (
			SELECT id, "tenantId",
			       ROW_NUMBER() OVER (
			         PARTITION BY TRIM(LOWER(email))
			         ORDER BY 
			           CASE WHEN "isActive" = TRUE THEN 1 ELSE 2 END,
			           CASE WHEN "pinHash" IS NOT NULL AND "pinHash" <> '' THEN 1 ELSE 2 END,
			           id DESC
			       ) as rn
			FROM "employees"
			WHERE email IS NOT NULL AND email <> ''
		)
		DELETE FROM "employees"
		WHERE (id, "tenantId") IN (
			SELECT id, "tenantId" 
			FROM duplicate_employees 
			WHERE rn > 1
		)
	`)
	if errDeduplicate != nil {
		log.Printf("[Deduplicate] Warning: failed to deduplicate employees: %v", errDeduplicate)
	}
	// Migration: tambah outletId ke tabel BMP & activity_logs untuk isolasi per-outlet
	outletIdMigrations := []string{
		`ALTER TABLE "bmp_cashflow"      ADD COLUMN IF NOT EXISTS "outletId" BIGINT;`,
		`ALTER TABLE "bmp_payrolls"      ADD COLUMN IF NOT EXISTS "outletId" BIGINT;`,
		`ALTER TABLE "bmp_bahan_baku"    ADD COLUMN IF NOT EXISTS "outletId" BIGINT;`,
		`ALTER TABLE "bmp_product_stocks" ADD COLUMN IF NOT EXISTS "outletId" BIGINT;`,
		`ALTER TABLE "activity_logs"     ADD COLUMN IF NOT EXISTS "outletId" BIGINT;`,
		`ALTER TABLE "bmp_products"      ADD COLUMN IF NOT EXISTS "description" TEXT;`,
		`ALTER TABLE "bmp_products"      ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN NOT NULL DEFAULT TRUE;`,
		`ALTER TABLE "bmp_products"      ADD COLUMN IF NOT EXISTS "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE;`,
	}
	for _, q := range outletIdMigrations {
		if _, err := db.Exec(q); err != nil {
			log.Printf("[migration] outletId warning: %v", err)
		}
	}

	// Migration: tambah kolom varian, grosir, dan hpp breakdown ke tabel products & transaction_items
	fnbProductMigrations := []string{
		`ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "wholesalePrice" DOUBLE PRECISION DEFAULT 0.0;`,
		`ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "minWholesaleQty" INT DEFAULT 0;`,
		`ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "variants" TEXT;`,
		`ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "costPriceBreakdown" TEXT;`,
		`ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "defaultDailyTarget" INT DEFAULT 0;`,
		`ALTER TABLE "transaction_items" ADD COLUMN IF NOT EXISTS "variant" VARCHAR(100);`,
	}
	for _, q := range fnbProductMigrations {
		if _, err := db.Exec(q); err != nil {
			log.Printf("[migration] fnb products warning: %v", err)
		}
	}

	// Migration: fix products.id sequence default — tanpa ini INSERT baru tanpa id akan gagal
	// (null value in column "id" violates not-null constraint)
	// Idempotent: CREATE SEQUENCE IF NOT EXISTS & ALTER COLUMN SET DEFAULT aman dijalankan berkali-kali
	productIdSeqMigrations := []string{
		`CREATE SEQUENCE IF NOT EXISTS "Product_id_seq" START 1;`,
		`ALTER TABLE "products" ALTER COLUMN id SET DEFAULT nextval('"Product_id_seq"');`,
		`SELECT setval('"Product_id_seq"', COALESCE((SELECT MAX(id) FROM "products"), 0) + 1, false);`,
	}
	for _, q := range productIdSeqMigrations {
		if _, err := db.Exec(q); err != nil {
			log.Printf("[migration] products id sequence warning: %v", err)
		}
	}

	// Migration: tambah kolom relasi ke transaction_items untuk HPP akuntansi yang benar
	// - productName : snapshot nama produk saat transaksi (historical data integrity)
	// - hppBreakdown: snapshot JSON komponen HPP saat transaksi (audit trail COGS)
	// - costPrice   : sudah ada, tapi sekarang akan terisi dari Android client (bug fix)
	// Prinsip akuntansi: Historical Cost — COGS harus mencerminkan biaya pada saat transaksi
	txItemRelationMigrations := []string{
		`ALTER TABLE "transaction_items" ADD COLUMN IF NOT EXISTS "productName" VARCHAR(255);`,
		`ALTER TABLE "transaction_items" ADD COLUMN IF NOT EXISTS "hppBreakdown" TEXT;`,
	}
	for _, q := range txItemRelationMigrations {
		if _, err := db.Exec(q); err != nil {
			log.Printf("[migration] transaction_items relation warning: %v", err)
		}
	}

	// Migration: tambah tabel product_daily_targets untuk target penjualan per produk per outlet
	targetMigrations := []string{
		`CREATE TABLE IF NOT EXISTS "product_daily_targets" (
			"id" BIGSERIAL PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"outletId" BIGINT NOT NULL,
			"productId" BIGINT NOT NULL,
			"targetDate" DATE NOT NULL,
			"targetQty" INT DEFAULT 0,
			"achievedQty" INT DEFAULT 0,
			"createdAt" BIGINT NOT NULL,
			"updatedAt" BIGINT NOT NULL,
			CONSTRAINT "uniq_target_date" UNIQUE ("tenantId", "outletId", "productId", "targetDate")
		);`,
	}
	for _, q := range targetMigrations {
		if _, err := db.Exec(q); err != nil {
			log.Printf("[migration] target warning: %v", err)
		}
	}

	// Migration: tambah kolom rawMaterialId ke tabel bmp_production_logs
	manufakturMigrations := []string{
		`ALTER TABLE "bmp_production_logs" ADD COLUMN IF NOT EXISTS "rawMaterialId" INT DEFAULT 0;`,
		`ALTER TABLE "bmp_cashflow" ADD COLUMN IF NOT EXISTS "amount" DOUBLE PRECISION DEFAULT 0;`,
		`ALTER TABLE "bmp_cashflow" ADD COLUMN IF NOT EXISTS "costType" VARCHAR(50) DEFAULT 'OPERATING_EXPENSE';`,
		`ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "employeeType" VARCHAR(50) DEFAULT 'OPERATING_EXPENSE';`,
		`CREATE TABLE IF NOT EXISTS "bmp_raw_material_stocks" (
			"id" SERIAL PRIMARY KEY,
			"tenantId" VARCHAR(100) NOT NULL,
			"jenisBahan" VARCHAR(150) NOT NULL,
			"stockInitial" DOUBLE PRECISION DEFAULT 0.0,
			"stockEntered" DOUBLE PRECISION DEFAULT 0.0,
			"stockConsumed" DOUBLE PRECISION DEFAULT 0.0,
			"stockFinal" DOUBLE PRECISION DEFAULT 0.0,
			"period" VARCHAR(50) NOT NULL,
			"updatedAt" BIGINT,
			CONSTRAINT "uniq_rm_stock_period" UNIQUE ("tenantId", "jenisBahan", "period")
		);`,
	}
	for _, q := range manufakturMigrations {
		if _, err := db.Exec(q); err != nil {
			log.Printf("[migration] manufaktur rawMaterialId/accounting warning: %v", err)
		}
	}


	// Backfill: isi kolom description pada bmp_products lama yang NULL
	// dengan description dari bmp_master_products berdasarkan masterItemID.
	// Query ini idempotent (WHERE description IS NULL) dan aman dijalankan berkali-kali.
	_, _ = db.Exec(`
		UPDATE "bmp_products" bp
		SET "description" = mp."description"
		FROM "bmp_master_products" mp
		WHERE bp."masterItemID" = mp."id"
		  AND bp."tenantId" = mp."tenantId"
		  AND bp."description" IS NULL
		  AND mp."description" IS NOT NULL
	`)
	// ── Database Indexing ──────────────────────────────────────────────────────
	// Semua index menggunakan IF NOT EXISTS (aman dijalankan berkali-kali).
	// Catatan: CONCURRENTLY tidak bisa dijalankan melalui db.Exec (butuh autocommit).
	// Index sudah terpasang langsung di VPS via psql. Block ini menjadi fallback
	// untuk database baru / fresh install.
	// Berdasarkan analisis pola WHERE clause di handlers_rt.go dan main.go.
	indexMigrations := []string{
		// ── Kelompok 1: Inti POS (Prioritas Tertinggi) ────────────────────────
		// transactions: filter utama per tenant + date sort
		`CREATE INDEX IF NOT EXISTS "idx_tx_tenant_date"
			ON "transactions" ("tenantId", "date" DESC)
			WHERE "isDeleted" = FALSE`,
		// transactions: filter per tenant + outlet (support multi-outlet)
		`CREATE INDEX IF NOT EXISTS "idx_tx_tenant_outlet_date"
			ON "transactions" ("tenantId", "outletId", "date" DESC)
			WHERE "isDeleted" = FALSE`,
		// transaction_items: join per transactionId (detail struk & margin)
		`CREATE INDEX IF NOT EXISTS "idx_tx_items_txid"
			ON "transaction_items" ("transactionId")`,
		// transaction_items: agregasi COGS per produk (Margin Analysis)
		`CREATE INDEX IF NOT EXISTS "idx_tx_items_prodid"
			ON "transaction_items" ("productId")`,
		// products: katalog produk per tenant+outlet
		`CREATE INDEX IF NOT EXISTS "idx_products_tenant_outlet"
			ON "products" ("tenantId", "outletId")
			WHERE "isDeleted" = FALSE`,

		// ── Kelompok 2: Multi-Tenant Core ──────────────────────────────────────
		`CREATE INDEX IF NOT EXISTS "idx_customers_tenant"
			ON "customers" ("tenantId")
			WHERE "isDeleted" = FALSE`,
		`CREATE INDEX IF NOT EXISTS "idx_employees_tenant"
			ON "employees" ("tenantId")
			WHERE "isActive" = TRUE`,
		`CREATE INDEX IF NOT EXISTS "idx_outlets_tenant"
			ON "outlets" ("tenantId")`,

		// ── Kelompok 3: BMP Module ─────────────────────────────────────────────
		`CREATE INDEX IF NOT EXISTS "idx_bmp_invoices_tenant_date"
			ON "bmp_invoices" ("tenantId", "createdAt" DESC)
			WHERE "isDeleted" = FALSE`,
		`CREATE INDEX IF NOT EXISTS "idx_bmp_cashflow_tenant_date"
			ON "bmp_cashflow" ("tenantId", "transactionDate" DESC)
			WHERE "isDeleted" = FALSE`,
		`CREATE INDEX IF NOT EXISTS "idx_bmp_bb_tenant"
			ON "bmp_bahan_baku" ("tenantId")
			WHERE "isDeleted" = FALSE`,
		`CREATE INDEX IF NOT EXISTS "idx_bmp_bbi_bahanid"
			ON "bmp_bahan_baku_item" ("bahanBakuId")
			WHERE "isDeleted" = FALSE`,
		`CREATE INDEX IF NOT EXISTS "idx_bmp_prodlogs_tenant_date"
			ON "bmp_production_logs" ("tenantId", "productionDate" DESC)
			WHERE "isDeleted" = FALSE`,

		// ── Kelompok 4: Auth — kritis untuk kecepatan login ───────────────────
		`CREATE INDEX IF NOT EXISTS "idx_users_email"
			ON "local_users" (LOWER("email"))
			WHERE "isActive" = TRUE`,
		`CREATE INDEX IF NOT EXISTS "idx_users_googlesub"
			ON "local_users" ("googleSub")
			WHERE "isActive" = TRUE`,

		// ── Kelompok 5: Target Penjualan Harian ───────────────────────────────
		`CREATE INDEX IF NOT EXISTS "idx_targets_tenant_outlet_date"
			ON "product_daily_targets" ("tenantId", "outletId", "targetDate")`,

		// ── Kelompok 6: Activity Logs (kolom: date, bukan createdAt) ───────────
		`CREATE INDEX IF NOT EXISTS "idx_actlogs_tenant_date"
			ON "activity_logs" ("tenantId", date DESC)`,
	}
	for _, q := range indexMigrations {
		if _, err := db.Exec(q); err != nil {
			// IF NOT EXISTS membuat ini idempotent — error hanya terjadi jika
			// ada perbedaan definisi, bukan duplikat
			log.Printf("[migration] index warning (non-fatal): %v", err)
		}
	}

	log.Println("Database schemas verified / migrated successfully.")
	return nil
}

func hasTenantIdColumn(tableName string) bool {
	switch tableName {
	case "outlets", "employees", "bmp_clients", "bmp_invoices", "bmp_products",
		"bmp_master_products", "bmp_invoice_payments", "bmp_cashflow", "bmp_settings",
		"bmp_employees", "bmp_payrolls", "bmp_bahan_baku", "bmp_bahan_baku_item",
		"products", "customers", "transactions", "bmp_product_stocks", "bmp_stock_ledger",
		"bmp_production_logs":
		return true
	}
	return false
}

func isPKColumn(tableName string, colName string) bool {
	if tableName == "local_users" && colName == "googleSub" {
		return true
	}
	if tableName == "print_settings" && (colName == "tenantId" || colName == "moduleKey") {
		return true
	}
	if hasTenantIdColumn(tableName) && (colName == "id" || colName == "tenantId") {
		return true
	}
	if colName == "id" {
		return true
	}
	return false
}

func dynamicUpsert(tableName string, rows []map[string]interface{}) error {
	if len(rows) == 0 {
		return nil
	}

	for _, row := range rows {
		var columns []string
		var placeholders []string
		var values []interface{}
		var updateParts []string

		idx := 1
		for k, v := range row {
			if !isValidColumnName(k) {
				return fmt.Errorf("invalid column name in upsert row payload: %s", k)
			}
			columns = append(columns, fmt.Sprintf(`"%s"`, k))
			placeholders = append(placeholders, fmt.Sprintf("$%d", idx))

			switch val := v.(type) {
			case map[string]interface{}, []interface{}:
				jsonBytes, _ := json.Marshal(val)
				values = append(values, string(jsonBytes))
			default:
				values = append(values, v)
			}

			if !isPKColumn(tableName, k) {
				if tableName == "employees" && k == "email" {
					// Exclude email column from conflict updates for employees table
				} else if (tableName == "bmp_invoices" || tableName == "bmp_clients") &&
					(k == "receiverSignatureUrl" || k == "receiverSignaturePath" || k == "receiverNameActual") {
					// Protect signature columns from being overwritten with NULL/empty during sync upsert
					updateParts = append(updateParts, fmt.Sprintf(`"%s" = COALESCE(NULLIF(EXCLUDED."%s", ''), "%s"."%s")`, k, k, tableName, k))
				} else {
					updateParts = append(updateParts, fmt.Sprintf(`"%s" = EXCLUDED."%s"`, k, k))
				}
			}
			idx++
		}

		conflictTarget := `"id"`
		if tableName == "local_users" {
			conflictTarget = `"googleSub"`
		} else if tableName == "print_settings" {
			conflictTarget = `"tenantId", "moduleKey"`
		} else if hasTenantIdColumn(tableName) {
			conflictTarget = `"id", "tenantId"`
		}

		query := fmt.Sprintf(
			`INSERT INTO "%s" (%s) VALUES (%s)`,
			tableName,
			strings.Join(columns, ", "),
			strings.Join(placeholders, ", "),
		)

		if len(updateParts) > 0 {
			query += fmt.Sprintf(` ON CONFLICT (%s) DO UPDATE SET %s`, conflictTarget, strings.Join(updateParts, ", "))
		} else {
			query += fmt.Sprintf(` ON CONFLICT (%s) DO NOTHING`, conflictTarget)
		}

		_, err := db.Exec(query, values...)
		if err != nil {
			log.Printf("Error upserting into %s: %v. Query: %s", tableName, err, query)
			return err
		}
	}
	return nil
}

func dynamicUpsertTx(tx *sql.Tx, tableName string, rows []map[string]interface{}) error {
	if len(rows) == 0 {
		return nil
	}

	for _, row := range rows {
		var columns []string
		var placeholders []string
		var values []interface{}
		var updateParts []string

		idx := 1
		for k, v := range row {
			if !isValidColumnName(k) {
				return fmt.Errorf("invalid column name in upsert row payload: %s", k)
			}
			columns = append(columns, fmt.Sprintf(`"%s"`, k))
			placeholders = append(placeholders, fmt.Sprintf("$%d", idx))

			switch val := v.(type) {
			case map[string]interface{}, []interface{}:
				jsonBytes, _ := json.Marshal(val)
				values = append(values, string(jsonBytes))
			default:
				values = append(values, v)
			}

			if !isPKColumn(tableName, k) {
				if tableName == "employees" && k == "email" {
					// Exclude email column from conflict updates for employees table
				} else if (tableName == "bmp_invoices" || tableName == "bmp_clients") &&
					(k == "receiverSignatureUrl" || k == "receiverSignaturePath" || k == "receiverNameActual") {
					// Protect signature columns from being overwritten with NULL/empty during sync upsert
					updateParts = append(updateParts, fmt.Sprintf(`"%s" = COALESCE(NULLIF(EXCLUDED."%s", ''), "%s"."%s")`, k, k, tableName, k))
				} else {
					updateParts = append(updateParts, fmt.Sprintf(`"%s" = EXCLUDED."%s"`, k, k))
				}
			}
			idx++
		}

		conflictTarget := `"id"`
		if tableName == "local_users" {
			conflictTarget = `"googleSub"`
		} else if tableName == "print_settings" {
			conflictTarget = `"tenantId", "moduleKey"`
		} else if hasTenantIdColumn(tableName) {
			conflictTarget = `"id", "tenantId"`
		}

		query := fmt.Sprintf(
			`INSERT INTO "%s" (%s) VALUES (%s)`,
			tableName,
			strings.Join(columns, ", "),
			strings.Join(placeholders, ", "),
		)

		if len(updateParts) > 0 {
			query += fmt.Sprintf(` ON CONFLICT (%s) DO UPDATE SET %s`, conflictTarget, strings.Join(updateParts, ", "))
		} else {
			query += fmt.Sprintf(` ON CONFLICT (%s) DO NOTHING`, conflictTarget)
		}

		_, err := tx.Exec(query, values...)
		if err != nil {
			log.Printf("Error upserting into %s (tx): %v. Query: %s", tableName, err, query)
			return err
		}
	}
	return nil
}

func backupBahanBakuAndRelations() {
	if db == nil {
		log.Println("[Backup Error] Database instance is nil.")
		return
	}

	log.Println("[Backup] Checking raw material tables and running auto-backup/restore...")

	// 1. Create backup tables if they don't exist
	_, err1 := db.Exec(`CREATE TABLE IF NOT EXISTS "backup_bmp_bahan_baku" (LIKE "bmp_bahan_baku" INCLUDING ALL)`)
	_, err2 := db.Exec(`CREATE TABLE IF NOT EXISTS "backup_bmp_bahan_baku_item" (LIKE "bmp_bahan_baku_item" INCLUDING ALL)`)
	if err1 != nil {
		log.Printf("[Backup Error] Failed to create backup_bmp_bahan_baku: %v", err1)
	}
	if err2 != nil {
		log.Printf("[Backup Error] Failed to create backup_bmp_bahan_baku_item: %v", err2)
	}

	// 2. Query row counts to determine if we should restore or refresh backup
	var countMain, countBackup int
	_ = db.QueryRow(`SELECT count(*) FROM "bmp_bahan_baku"`).Scan(&countMain)
	_ = db.QueryRow(`SELECT count(*) FROM "backup_bmp_bahan_baku"`).Scan(&countBackup)

	var countItemsMain, countItemsBackup int
	_ = db.QueryRow(`SELECT count(*) FROM "bmp_bahan_baku_item"`).Scan(&countItemsMain)
	_ = db.QueryRow(`SELECT count(*) FROM "backup_bmp_bahan_baku_item"`).Scan(&countItemsBackup)

	log.Printf("[Backup] Main count: %d, Backup count: %d. Items main count: %d, Items backup count: %d.",
		countMain, countBackup, countItemsMain, countItemsBackup)

	// Case A: Main tables are completely empty, but backup tables have data -> Restore!
	if countMain == 0 && countBackup > 0 {
		log.Printf("[Backup] Main table is empty, restoring %d headers from backup table...", countBackup)
		_, err := db.Exec(`INSERT INTO "bmp_bahan_baku" SELECT * FROM "backup_bmp_bahan_baku"`)
		if err != nil {
			log.Printf("[Backup Error] Restoring headers failed: %v", err)
		} else {
			log.Println("[Backup] Restoring headers completed successfully.")
			countMain = countBackup
		}
	}

	if countItemsMain == 0 && countItemsBackup > 0 {
		log.Printf("[Backup] Main item table is empty, restoring %d items from backup table...", countItemsBackup)
		_, err := db.Exec(`INSERT INTO "bmp_bahan_baku_item" SELECT * FROM "backup_bmp_bahan_baku_item"`)
		if err != nil {
			log.Printf("[Backup Error] Restoring items failed: %v", err)
		} else {
			log.Println("[Backup] Restoring items completed successfully.")
			countItemsMain = countItemsBackup
		}
	}

	// Case B: Main tables have data -> Refresh/overwrite the backup tables with the latest state
	if countMain > 0 {
		log.Println("[Backup] Refreshing database backup tables...")
		_, err3 := db.Exec(`TRUNCATE TABLE "backup_bmp_bahan_baku" CASCADE`)
		if err3 == nil {
			_, err4 := db.Exec(`INSERT INTO "backup_bmp_bahan_baku" SELECT * FROM "bmp_bahan_baku"`)
			if err4 != nil {
				log.Printf("[Backup Error] Failed to populate backup_bmp_bahan_baku: %v", err4)
			}
		} else {
			log.Printf("[Backup Error] Failed to truncate backup_bmp_bahan_baku: %v", err3)
		}

		_, err5 := db.Exec(`TRUNCATE TABLE "backup_bmp_bahan_baku_item" CASCADE`)
		if err5 == nil {
			_, err6 := db.Exec(`INSERT INTO "backup_bmp_bahan_baku_item" SELECT * FROM "bmp_bahan_baku_item"`)
			if err6 != nil {
				log.Printf("[Backup Error] Failed to populate backup_bmp_bahan_baku_item: %v", err6)
			}
		} else {
			log.Printf("[Backup Error] Failed to truncate backup_bmp_bahan_baku_item: %v", err5)
		}
		log.Println("[Backup] Database backup tables refreshed successfully.")
	}

	// 3. Export to file backups for extra redundancy
	backupBahanBakuToJSON()
}

func backupBahanBakuToJSON() {
	// Query header rows
	rows1, err := db.Query(`SELECT * FROM "bmp_bahan_baku"`)
	if err != nil {
		log.Printf("[Backup Error] Query bmp_bahan_baku failed: %v", err)
		return
	}
	defer rows1.Close()
	headerJSON := rowsToJSON(rows1)

	// Query item rows
	rows2, err := db.Query(`SELECT * FROM "bmp_bahan_baku_item"`)
	if err != nil {
		log.Printf("[Backup Error] Query bmp_bahan_baku_item failed: %v", err)
		return
	}
	defer rows2.Close()
	itemsJSON := rowsToJSON(rows2)

	// Create backup map
	backupData := map[string]interface{}{
		"timestamp": time.Now().UnixMilli(),
		"headers":   headerJSON,
		"items":     itemsJSON,
	}

	// Serialize
	dataBytes, err := json.MarshalIndent(backupData, "", "  ")
	if err != nil {
		log.Printf("[Backup Error] JSON marshal failed: %v", err)
		return
	}

	// Write to file
	backupDir := "/home/muizz9900/backups"
	_ = os.MkdirAll(backupDir, 0755)
	backupFile := filepath.Join(backupDir, "bahanbaku_backup.json")
	err = os.WriteFile(backupFile, dataBytes, 0644)
	if err != nil {
		// Fallback to current dir
		backupFile = "./bahanbaku_backup.json"
		_ = os.WriteFile(backupFile, dataBytes, 0644)
	}
	log.Printf("[Backup Success] Exported raw material data to file: %s", backupFile)
}
