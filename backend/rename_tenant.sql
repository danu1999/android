BEGIN;

-- 1. Insert the new premium tenant copied from the old demo tenant row
INSERT INTO tenants (id, name, "ownerEmail", "businessMode", "isActive", "createdAt", "updatedAt")
SELECT 'ten_premium_mulyakus84_gmail_com_LAUNDRY', 'CV. Aku&dia Bersama (Laundry)', "ownerEmail", "businessMode", "isActive", "createdAt", "updatedAt"
FROM tenants WHERE id = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY'
ON CONFLICT (id) DO UPDATE SET name = 'CV. Aku&dia Bersama (Laundry)';

-- 2. Update local_users
UPDATE local_users SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';

-- 3. Update all tables referencing the tenant ID
UPDATE outlets SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE employees SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_clients SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_invoices SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_products SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_product_stocks SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_master_products SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_invoice_payments SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_cashflow SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_settings SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_employees SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_payrolls SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_bahan_baku SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_bahan_baku_item SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE print_settings SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE products SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE customers SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE transactions SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE activity_logs SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_device_tenants SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_stock_ledger SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';
UPDATE bmp_production_logs SET "tenantId" = 'ten_premium_mulyakus84_gmail_com_LAUNDRY' WHERE "tenantId" = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';

-- 4. Delete the old demo tenant row
DELETE FROM tenants WHERE id = 'demo_tenant_mulyakus84_gmail_com_LAUNDRY';

COMMIT;
