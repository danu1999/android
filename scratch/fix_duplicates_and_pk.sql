-- 1. Hapus baris duplikat di bmp_master_products
DELETE FROM bmp_master_products a
USING bmp_master_products b
WHERE a.ctid < b.ctid
  AND a.id = b.id
  AND a."tenantId" = b."tenantId";

-- 2. Buat primary key constraint yang benar
ALTER TABLE bmp_master_products DROP CONSTRAINT IF EXISTS bmp_master_products_pkey;
ALTER TABLE bmp_master_products ADD PRIMARY KEY (id, "tenantId");
