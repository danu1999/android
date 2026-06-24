BEGIN;

-- Migrate outlets
INSERT INTO outlets (id, "tenantId", name, address, phone, "isDefault", "isOpen", "createdAt", "updatedAt", "isSynced", "isDeleted")
SELECT
  id,
  'ten_premium_bahteramulyap_gmail_com',
  name,
  address,
  phone,
  false,
  true,
  COALESCE(EXTRACT(EPOCH FROM "createdAt")::bigint*1000, 0),
  COALESCE(EXTRACT(EPOCH FROM "updatedAt")::bigint*1000, 0),
  true,
  false
FROM "Outlet"
ON CONFLICT (id, "tenantId") DO UPDATE SET
  name = EXCLUDED.name,
  address = EXCLUDED.address,
  phone = EXCLUDED.phone;

-- Migrate employees
INSERT INTO employees (id, "tenantId", "outletId", name, email, role, "pinHash", phone, salary, "isActive", "payPeriod", "emailVerified", "createdAt", "updatedAt", "isSynced", "isDeleted")
SELECT
  id,
  'ten_premium_bahteramulyap_gmail_com',
  "outletId",
  name,
  email,
  role,
  pin,
  '',
  salary,
  true,
  'MONTHLY',
  false,
  COALESCE(EXTRACT(EPOCH FROM "createdAt")::bigint*1000, 0),
  COALESCE(EXTRACT(EPOCH FROM "updatedAt")::bigint*1000, 0),
  true,
  false
FROM "Employee"
ON CONFLICT (id, "tenantId") DO UPDATE SET
  "outletId" = EXCLUDED."outletId",
  name = EXCLUDED.name,
  email = EXCLUDED.email,
  role = EXCLUDED.role,
  "pinHash" = EXCLUDED."pinHash",
  salary = EXCLUDED.salary;

-- Migrate products
INSERT INTO products (id, "tenantId", "outletId", name, price, "costPrice", stock, unit, barcode, category, "wholesaleEnabled", "wholesalePrices", variants, image, "createdAt", "updatedAt", "isSynced", "isDeleted")
SELECT
  id,
  'ten_premium_bahteramulyap_gmail_com',
  "outletId",
  name,
  price,
  "costPrice",
  stock,
  unit,
  barcode,
  category,
  "wholesaleEnabled",
  "wholesalePrices",
  variants,
  image,
  COALESCE(EXTRACT(EPOCH FROM "createdAt")::bigint*1000, 0),
  COALESCE(EXTRACT(EPOCH FROM "updatedAt")::bigint*1000, 0),
  true,
  false
FROM "Product"
ON CONFLICT (id, "tenantId") DO UPDATE SET
  "outletId" = EXCLUDED."outletId",
  name = EXCLUDED.name,
  price = EXCLUDED.price,
  "costPrice" = EXCLUDED."costPrice",
  stock = EXCLUDED.stock,
  unit = EXCLUDED.unit,
  barcode = EXCLUDED.barcode,
  category = EXCLUDED.category,
  "wholesaleEnabled" = EXCLUDED."wholesaleEnabled",
  "wholesalePrices" = EXCLUDED."wholesalePrices",
  variants = EXCLUDED.variants,
  image = EXCLUDED.image;

-- Migrate customers
INSERT INTO customers (id, "tenantId", name, phone, address, "createdAt", "updatedAt", "isSynced")
SELECT
  id,
  'ten_premium_bahteramulyap_gmail_com',
  name,
  phone,
  address,
  COALESCE(EXTRACT(EPOCH FROM "createdAt")::bigint*1000, 0),
  COALESCE(EXTRACT(EPOCH FROM "updatedAt")::bigint*1000, 0),
  true
FROM "Customer"
ON CONFLICT (id, "tenantId") DO UPDATE SET
  name = EXCLUDED.name,
  phone = EXCLUDED.phone,
  address = EXCLUDED.address;

-- Migrate transactions
INSERT INTO transactions (id, "tenantId", "outletId", "employeeId", "customerId", "customerName", "receiptNumber", date, subtotal, "discountType", "discountInput", "discountAmt", total, discount, "paymentMethod", "amountPaid", change, status, type, "orderStatus", "dpAmount", "deliveryDate", "queueNumber", notes, "createdAt", "updatedAt", "isSynced", "isDeleted")
SELECT
  id,
  'ten_premium_bahteramulyap_gmail_com',
  "outletId",
  "employeeId",
  "customerId",
  "customerName",
  "receiptNumber",
  COALESCE(EXTRACT(EPOCH FROM date)::bigint*1000, 0),
  subtotal,
  "discountType",
  "discountInput",
  "discountAmt",
  total,
  discount,
  "paymentMethod",
  "amountPaid",
  change,
  status,
  type,
  "orderStatus",
  "dpAmount",
  COALESCE(EXTRACT(EPOCH FROM "deliveryDate")::bigint*1000, 0),
  "queueNumber",
  notes,
  COALESCE(EXTRACT(EPOCH FROM "createdAt")::bigint*1000, 0),
  COALESCE(EXTRACT(EPOCH FROM "updatedAt")::bigint*1000, 0),
  true,
  false
FROM "Transaction"
ON CONFLICT (id, "tenantId") DO UPDATE SET
  "outletId" = EXCLUDED."outletId",
  "employeeId" = EXCLUDED."employeeId",
  "customerId" = EXCLUDED."customerId",
  "customerName" = EXCLUDED."customerName",
  "receiptNumber" = EXCLUDED."receiptNumber",
  date = EXCLUDED.date,
  subtotal = EXCLUDED.subtotal,
  total = EXCLUDED.total,
  "paymentMethod" = EXCLUDED."paymentMethod",
  status = EXCLUDED.status;

-- Migrate transaction items
INSERT INTO transaction_items (id, "transactionId", "productId", "variantId", "variantName", quantity, price, "costPrice", discount, note, "isSynced", "isDeleted")
SELECT
  id,
  "transactionId",
  "productId",
  "variantId",
  "variantName",
  quantity,
  price,
  "costPrice",
  discount,
  note,
  true,
  false
FROM "TransactionItem"
ON CONFLICT (id) DO UPDATE SET
  "transactionId" = EXCLUDED."transactionId",
  "productId" = EXCLUDED."productId",
  quantity = EXCLUDED.quantity,
  price = EXCLUDED.price;

-- Migrate activity logs
INSERT INTO activity_logs (id, "tenantId", action, description, date, "employeeName", "appMode")
SELECT
  a.id,
  'ten_premium_bahteramulyap_gmail_com',
  a.action,
  a.description,
  COALESCE(EXTRACT(EPOCH FROM a.date)::bigint*1000, 0),
  COALESCE(e.name, 'Admin'),
  a."appMode"
FROM "ActivityLog" a
LEFT JOIN "Employee" e ON a."employeeId" = e.id
ON CONFLICT (id) DO UPDATE SET
  "tenantId" = EXCLUDED."tenantId",
  action = EXCLUDED.action,
  description = EXCLUDED.description;

COMMIT;
