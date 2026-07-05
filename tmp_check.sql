BEGIN;

CREATE TABLE IF NOT EXISTS "raw_materials" (
    "id" SERIAL PRIMARY KEY,
    "tenantId" VARCHAR(100) NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    "stock" DOUBLE PRECISION DEFAULT 0,
    "unit" VARCHAR(50) DEFAULT 'pcs',
    "updatedAt" BIGINT
);

CREATE TABLE IF NOT EXISTS "product_recipes" (
    "id" SERIAL PRIMARY KEY,
    "productId" INT NOT NULL,
    "rawMaterialId" INT NOT NULL,
    "quantityNeeded" DOUBLE PRECISION NOT NULL,
    FOREIGN KEY ("productId") REFERENCES "products"("id") ON DELETE CASCADE,
    FOREIGN KEY ("rawMaterialId") REFERENCES "raw_materials"("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "product_modifiers" (
    "id" SERIAL PRIMARY KEY,
    "productId" INT NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    "price" DOUBLE PRECISION DEFAULT 0,
    "costPrice" DOUBLE PRECISION DEFAULT 0,
    "rawMaterialId" INT,
    FOREIGN KEY ("productId") REFERENCES "products"("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "cashier_shifts" (
    "id" SERIAL PRIMARY KEY,
    "tenantId" VARCHAR(100) NOT NULL,
    "employeeId" INT NOT NULL,
    "openedAt" BIGINT NOT NULL,
    "closedAt" BIGINT,
    "startCash" DOUBLE PRECISION NOT NULL,
    "expectedEndCash" DOUBLE PRECISION DEFAULT 0,
    "actualEndCash" DOUBLE PRECISION DEFAULT 0,
    "status" VARCHAR(50) DEFAULT 'OPEN'
);

COMMIT;
