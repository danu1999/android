-- POSBAH SUPABASE RLS ENABLE & TENANT ISOLATION POLICIES
-- Copy and paste this script into your Supabase Dashboard SQL Editor, then click "RUN".

-- 1. Enable Row Level Security (RLS) on all tables
ALTER TABLE IF EXISTS local_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS outlets ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS employees ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS bmp_clients ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS bmp_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS bmp_master_products ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS bmp_products ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS bmp_invoice_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS bmp_cashflow ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS bmp_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS bmp_employees ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS bmp_payrolls ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS bmp_bahan_baku ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS bmp_bahan_baku_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS print_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS products ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS transaction_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS activity_logs ENABLE ROW LEVEL SECURITY;

-- 2. Drop existing policies to prevent conflicts
DROP POLICY IF EXISTS tenant_isolation_policy ON local_users;
DROP POLICY IF EXISTS tenant_isolation_policy ON tenants;
DROP POLICY IF EXISTS tenant_isolation_policy ON outlets;
DROP POLICY IF EXISTS tenant_isolation_policy ON employees;
DROP POLICY IF EXISTS tenant_isolation_policy ON bmp_clients;
DROP POLICY IF EXISTS tenant_isolation_policy ON bmp_invoices;
DROP POLICY IF EXISTS tenant_isolation_policy ON bmp_master_products;
DROP POLICY IF EXISTS tenant_isolation_policy ON bmp_products;
DROP POLICY IF EXISTS tenant_isolation_policy ON bmp_invoice_payments;
DROP POLICY IF EXISTS tenant_isolation_policy ON bmp_cashflow;
DROP POLICY IF EXISTS tenant_isolation_policy ON bmp_settings;
DROP POLICY IF EXISTS tenant_isolation_policy ON bmp_employees;
DROP POLICY IF EXISTS tenant_isolation_policy ON bmp_payrolls;
DROP POLICY IF EXISTS tenant_isolation_policy ON bmp_bahan_baku;
DROP POLICY IF EXISTS tenant_isolation_policy ON bmp_bahan_baku_item;
DROP POLICY IF EXISTS tenant_isolation_policy ON print_settings;
DROP POLICY IF EXISTS tenant_isolation_policy ON products;
DROP POLICY IF EXISTS tenant_isolation_policy ON customers;
DROP POLICY IF EXISTS tenant_isolation_policy ON transactions;
DROP POLICY IF EXISTS tenant_isolation_policy ON transaction_items;
DROP POLICY IF EXISTS tenant_isolation_policy ON activity_logs;

-- 3. Create Tenant Isolation Policies using custom header x-tenant-id
CREATE POLICY tenant_isolation_policy ON local_users FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON tenants FOR ALL USING (id = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK (id = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON outlets FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON employees FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON bmp_clients FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON bmp_invoices FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON bmp_master_products FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON bmp_products FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON bmp_invoice_payments FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON bmp_cashflow FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON bmp_settings FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON bmp_employees FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON bmp_payrolls FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON bmp_bahan_baku FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON bmp_bahan_baku_item FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON print_settings FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON products FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON customers FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON transactions FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));
CREATE POLICY tenant_isolation_policy ON activity_logs FOR ALL USING ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')) WITH CHECK ("tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', ''));

-- Special Policy for transaction_items (Joins transactions to verify tenantId)
CREATE POLICY tenant_isolation_policy ON transaction_items FOR ALL 
USING (EXISTS (
    SELECT 1 FROM transactions 
    WHERE transactions.id = transaction_items."transactionId" 
      AND transactions."tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')
))
WITH CHECK (EXISTS (
    SELECT 1 FROM transactions 
    WHERE transactions.id = transaction_items."transactionId" 
      AND transactions."tenantId" = coalesce(current_setting('request.headers', true)::json->>'x-tenant-id', '')
));
