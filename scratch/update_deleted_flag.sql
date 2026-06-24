UPDATE bmp_clients SET "isDeleted" = FALSE WHERE "isDeleted" IS NULL;
UPDATE bmp_invoices SET "isDeleted" = FALSE WHERE "isDeleted" IS NULL;
UPDATE bmp_cashflow SET "isDeleted" = FALSE WHERE "isDeleted" IS NULL;
UPDATE bmp_bahan_baku SET "isDeleted" = FALSE WHERE "isDeleted" IS NULL;
UPDATE bmp_bahan_baku_item SET "isDeleted" = FALSE WHERE "isDeleted" IS NULL;
