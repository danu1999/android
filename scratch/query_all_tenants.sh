#!/bin/bash
source ~/.posbah-env
psql "$DATABASE_URL" -c "
SELECT 
    t.id AS tenant_id, 
    t.name AS tenant_name,
    t.\"businessMode\" AS mode,
    (SELECT COUNT(*) FROM products p WHERE p.\"tenantId\" = t.id) AS products_count,
    (SELECT COUNT(*) FROM customers c WHERE c.\"tenantId\" = t.id) AS customers_count,
    (SELECT COUNT(*) FROM transactions tx WHERE tx.\"tenantId\" = t.id) AS transactions_count,
    (SELECT COUNT(*) FROM bmp_invoices bi WHERE bi.\"tenantId\" = t.id) AS invoices_count,
    (SELECT COUNT(*) FROM bmp_cashflow bc WHERE bc.\"tenantId\" = t.id) AS cashflow_count,
    (SELECT COUNT(*) FROM employees e WHERE e.\"tenantId\" = t.id) AS employees_count
FROM tenants t
ORDER BY transactions_count DESC, invoices_count DESC;
"
