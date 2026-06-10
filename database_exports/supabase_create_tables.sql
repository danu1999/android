-- POSBAH SUPABASE TABLE CREATION SCHEMAS
-- Copy and paste this script into your Supabase Dashboard SQL Editor, then click "RUN".

CREATE TABLE IF NOT EXISTS local_users (
    "googleSub" TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    "displayName" TEXT,
    "photoUrl" TEXT,
    role TEXT NOT NULL DEFAULT 'OWNER',
    "tenantId" TEXT,
    whatsapp TEXT,
    "isPremium" BOOLEAN NOT NULL DEFAULT FALSE,
    "businessModeLocked" BOOLEAN NOT NULL DEFAULT FALSE,
    "registeredAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "lastLoginAt" BIGINT NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS tenants (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    "ownerEmail" TEXT NOT NULL,
    "businessMode" TEXT NOT NULL DEFAULT 'BMP',
    "isActive" BOOLEAN NOT NULL DEFAULT TRUE,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS outlets (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    name TEXT NOT NULL,
    address TEXT,
    phone TEXT,
    "isDefault" BOOLEAN NOT NULL DEFAULT FALSE,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS employees (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "outletId" BIGINT,
    name TEXT NOT NULL,
    email TEXT,
    role TEXT NOT NULL DEFAULT 'KASIR',
    "pinHash" TEXT NOT NULL,
    salary DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "isActive" BOOLEAN NOT NULL DEFAULT TRUE,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bmp_clients (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "outletId" BIGINT,
    "clientName" TEXT NOT NULL,
    "saldoTitipan" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "addressLine1" TEXT,
    "clientLogo" TEXT,
    province TEXT,
    "postalCode" TEXT,
    "phoneNumber" TEXT,
    "emailAddress" TEXT,
    "taxNumber" TEXT,
    "uniqueID" TEXT,
    slug TEXT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bmp_invoices (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "outletId" BIGINT,
    "clientId" BIGINT,
    title TEXT NOT NULL,
    number TEXT NOT NULL,
    "dueDate" BIGINT,
    "paymentTerms" TEXT NOT NULL DEFAULT '14 days',
    status TEXT NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    "totalAmount" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "paidAmount" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "uniqueID" TEXT,
    slug TEXT NOT NULL,
    "receiverSignaturePath" TEXT,
    "receiverSignatureUrl" TEXT,
    "receiverNameActual" TEXT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bmp_master_products (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    unit TEXT NOT NULL DEFAULT 'Kg',
    price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "beratGram" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "cycleTime" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    cavity INTEGER NOT NULL DEFAULT 1,
    "rejectRate" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "uniqueID" TEXT,
    slug TEXT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bmp_products (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "invoiceId" BIGINT,
    "masterItemID" BIGINT,
    title TEXT NOT NULL,
    unit TEXT NOT NULL DEFAULT 'pcs',
    price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "jumlahLusin" DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "isKhusus" BOOLEAN NOT NULL DEFAULT FALSE,
    "hargaBeli" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    currency TEXT NOT NULL DEFAULT 'Rp',
    "uniqueID" TEXT,
    slug TEXT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bmp_invoice_payments (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "invoiceId" BIGINT NOT NULL,
    "paymentDate" BIGINT NOT NULL,
    "paymentAmount" DOUBLE PRECISION NOT NULL,
    "paymentMethod" TEXT NOT NULL DEFAULT 'TRANSFER',
    notes TEXT,
    "createdAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bmp_cashflow (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "transactionDate" BIGINT NOT NULL,
    "transactionType" TEXT NOT NULL,
    description TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "paymentRefId" BIGINT,
    "createdAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bmp_settings (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL UNIQUE,
    "clientName" TEXT NOT NULL,
    "clientLogo" TEXT,
    "addressLine1" TEXT,
    province TEXT,
    "postalCode" TEXT,
    "phoneNumber" TEXT,
    "emailAddress" TEXT,
    "taxNumber" TEXT,
    "listrikBulanan" DOUBLE PRECISION NOT NULL DEFAULT 30000000.0,
    "jumlahMesin" INTEGER NOT NULL DEFAULT 5,
    "jumlahKaryawan" INTEGER NOT NULL DEFAULT 19,
    "gajiHarian" DOUBLE PRECISION NOT NULL DEFAULT 80000.0,
    "hariKerjaSebulan" INTEGER NOT NULL DEFAULT 26,
    "biayaKarungPer1000" DOUBLE PRECISION NOT NULL DEFAULT 2100000.0,
    "hoursPerDay" INTEGER NOT NULL DEFAULT 24,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bmp_employees (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    name TEXT NOT NULL,
    position TEXT,
    "salaryAmount" DOUBLE PRECISION NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT TRUE,
    "fingerprintPIN" TEXT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bmp_payrolls (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "employeeId" BIGINT NOT NULL,
    "paymentDate" BIGINT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    "attendanceCount" INTEGER NOT NULL DEFAULT 0,
    "dailyRate" DOUBLE PRECISION NOT NULL,
    description TEXT,
    "createdAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bmp_bahan_baku (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    tanggal BIGINT NOT NULL,
    "noTagihan" TEXT NOT NULL,
    "totalHarga" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    nominal DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    notes TEXT,
    "notaFotoPath" TEXT,
    "notaFotoUrl" TEXT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bmp_bahan_baku_item (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "bahanBakuId" BIGINT NOT NULL,
    "jenisBahan" TEXT NOT NULL,
    kuantitas DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    unit TEXT NOT NULL DEFAULT 'Kg',
    rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "createdAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS print_settings (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL UNIQUE,
    "jpgUseLogo" BOOLEAN NOT NULL DEFAULT TRUE,
    "jpgHeaderAlign" TEXT NOT NULL DEFAULT 'LEFT',
    "jpgUseSignature" BOOLEAN NOT NULL DEFAULT TRUE,
    "jpgSignatureSenderName" TEXT NOT NULL DEFAULT 'Admin',
    "jpgSignatureReceiverName" TEXT NOT NULL DEFAULT '',
    "jpgSignatureDrawnBase64" TEXT,
    "jpgIsColor" BOOLEAN NOT NULL DEFAULT TRUE,
    "sjUseLogo" BOOLEAN NOT NULL DEFAULT TRUE,
    "sjHeaderAlign" TEXT NOT NULL DEFAULT 'LEFT',
    "sjUseSignature" BOOLEAN NOT NULL DEFAULT TRUE,
    "sjSignatureSenderName" TEXT NOT NULL DEFAULT 'Admin',
    "sjSignatureReceiverName" TEXT NOT NULL DEFAULT '',
    "sjSignatureDrawnBase64" TEXT,
    "sjIsColor" BOOLEAN NOT NULL DEFAULT FALSE,
    "invoiceUseLogo" BOOLEAN NOT NULL DEFAULT TRUE,
    "invoiceHeaderAlign" TEXT NOT NULL DEFAULT 'LEFT',
    "invoiceUseSignature" BOOLEAN NOT NULL DEFAULT TRUE,
    "invoiceSignatureSenderName" TEXT NOT NULL DEFAULT 'Admin',
    "invoiceSignatureReceiverName" TEXT NOT NULL DEFAULT '',
    "invoiceSignatureDrawnBase64" TEXT,
    "invoiceIsColor" BOOLEAN NOT NULL DEFAULT TRUE,
    "receiptPaperWidth" TEXT NOT NULL DEFAULT 'MM80',
    "receiptUseLogo" BOOLEAN NOT NULL DEFAULT TRUE,
    "receiptHeaderAlign" TEXT NOT NULL DEFAULT 'CENTER',
    "receiptIsColor" BOOLEAN NOT NULL DEFAULT FALSE,
    "receiptShowItemPrice" BOOLEAN NOT NULL DEFAULT TRUE,
    "receiptFooterText" TEXT NOT NULL DEFAULT 'Terima kasih sudah berbelanja!',
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL,
    "bankOwnerName" TEXT NOT NULL DEFAULT '',
    "bankName" TEXT NOT NULL DEFAULT 'BCA',
    "bankAccountNumber" TEXT NOT NULL DEFAULT ''
);


CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "outletId" BIGINT,
    name TEXT NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    "costPrice" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    stock INTEGER NOT NULL DEFAULT 0,
    unit TEXT NOT NULL DEFAULT 'pcs',
    barcode TEXT,
    category TEXT NOT NULL DEFAULT 'Umum',
    "wholesaleEnabled" BOOLEAN NOT NULL DEFAULT FALSE,
    "wholesalePrices" TEXT,
    variants TEXT,
    image TEXT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    name TEXT NOT NULL,
    phone TEXT,
    address TEXT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "outletId" BIGINT,
    "employeeId" BIGINT NOT NULL,
    "customerId" BIGINT,
    "customerName" TEXT,
    "receiptNumber" TEXT NOT NULL UNIQUE,
    date BIGINT NOT NULL,
    subtotal DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "discountType" TEXT,
    "discountInput" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "discountAmt" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "paymentMethod" TEXT NOT NULL,
    "amountPaid" DOUBLE PRECISION,
    change DOUBLE PRECISION,
    status TEXT NOT NULL DEFAULT 'COMPLETED',
    type TEXT NOT NULL DEFAULT 'SALES',
    "orderStatus" TEXT,
    "dpAmount" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "deliveryDate" BIGINT,
    "queueNumber" INTEGER,
    notes TEXT,
    "createdAt" BIGINT NOT NULL,
    "updatedAt" BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS transaction_items (
    id BIGINT PRIMARY KEY,
    "transactionId" BIGINT NOT NULL,
    "productId" BIGINT NOT NULL,
    "variantId" BIGINT,
    "variantName" TEXT,
    quantity INTEGER NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    "costPrice" DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    note TEXT
);

CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGINT PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    action TEXT NOT NULL,
    description TEXT NOT NULL,
    date BIGINT NOT NULL,
    "employeeName" TEXT NOT NULL DEFAULT 'Owner',
    "appMode" TEXT NOT NULL
);
