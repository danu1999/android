SELECT id, title, "tenantId", COUNT(*) FROM bmp_master_products GROUP BY id, title, "tenantId" HAVING COUNT(*) > 1 LIMIT 5;
