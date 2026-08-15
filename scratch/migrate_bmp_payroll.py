import subprocess

def run_vps_bash(script_content):
    ssh_cmd = [
        "ssh", "-i", "C:\\Users\\danus\\Documents\\muizz.pem",
        "-o", "StrictHostKeyChecking=no",
        "muizz9900@zedmz.cloud",
        "bash"
    ]
    res = subprocess.run(ssh_cmd, input=script_content, capture_output=True, text=True)
    return res.stdout, res.stderr, res.returncode

vps_script = r"""
psql postgres://postgres:Bahtera1!@localhost:5432/posbah?sslmode=disable << 'SQL'
-- 1. Upgrade bmp_employees columns
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "phone" VARCHAR(50);
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "email" VARCHAR(255);
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "lastPaidAt" BIGINT;
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "outletId" INT;
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "role" VARCHAR(50) DEFAULT 'KARYAWAN';
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "position" VARCHAR(100);
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "salaryAmount" DOUBLE PRECISION DEFAULT 0;
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "employeeType" VARCHAR(50) DEFAULT 'OPERATING_EXPENSE';
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "fingerprintPIN" VARCHAR(50);
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "employeeId" INT;
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "isActive" BOOLEAN DEFAULT TRUE;
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "isDeleted" BOOLEAN DEFAULT FALSE;
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN DEFAULT TRUE;
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "createdAt" BIGINT;
ALTER TABLE "bmp_employees" ADD COLUMN IF NOT EXISTS "updatedAt" BIGINT;

CREATE SEQUENCE IF NOT EXISTS "bmp_employees_id_seq" START 1;
ALTER TABLE "bmp_employees" ALTER COLUMN id SET DEFAULT nextval('"bmp_employees_id_seq"');
SELECT setval('"bmp_employees_id_seq"', COALESCE((SELECT MAX(id) FROM "bmp_employees"), 0) + 1, false);

-- 2. Create / ensure bmp_payrolls table
CREATE TABLE IF NOT EXISTS "bmp_payrolls" (
    "id" BIGSERIAL PRIMARY KEY,
    "tenantId" VARCHAR(100) NOT NULL,
    "employeeId" BIGINT NOT NULL,
    "employeeName" VARCHAR(255),
    "paymentDate" BIGINT NOT NULL,
    "amount" DOUBLE PRECISION NOT NULL,
    "attendanceCount" INT DEFAULT 0,
    "dailyRate" DOUBLE PRECISION NOT NULL,
    "description" TEXT,
    "paymentMethod" VARCHAR(50) DEFAULT 'TRANSFER',
    "isDeleted" BOOLEAN DEFAULT FALSE,
    "isSynced" BOOLEAN DEFAULT TRUE,
    "createdAt" BIGINT,
    "updatedAt" BIGINT
);

ALTER TABLE "bmp_payrolls" ADD COLUMN IF NOT EXISTS "employeeName" VARCHAR(255);
ALTER TABLE "bmp_payrolls" ADD COLUMN IF NOT EXISTS "paymentMethod" VARCHAR(50) DEFAULT 'TRANSFER';
ALTER TABLE "bmp_payrolls" ADD COLUMN IF NOT EXISTS "isDeleted" BOOLEAN DEFAULT FALSE;
ALTER TABLE "bmp_payrolls" ADD COLUMN IF NOT EXISTS "isSynced" BOOLEAN DEFAULT TRUE;
ALTER TABLE "bmp_payrolls" ADD COLUMN IF NOT EXISTS "createdAt" BIGINT;
ALTER TABLE "bmp_payrolls" ADD COLUMN IF NOT EXISTS "updatedAt" BIGINT;

CREATE INDEX IF NOT EXISTS "idx_bmp_payrolls_tenant_date" ON "bmp_payrolls" ("tenantId", "paymentDate" DESC);
CREATE INDEX IF NOT EXISTS "idx_bmp_payrolls_tenant_emp" ON "bmp_payrolls" ("tenantId", "employeeId");

\d bmp_employees
\d bmp_payrolls
SQL
"""

print("Executing migrations on VPS PostgreSQL...")
out, err, code = run_vps_bash(vps_script)
print("Code:", code)
print("Stdout:")
print(out)
if err:
    print("Stderr:", err)
