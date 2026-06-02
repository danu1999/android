const { PrismaClient } = require('@prisma/client');

const prisma = new PrismaClient({
  datasources: {
    db: {
      url: "postgresql://postgres:sDfvYsAJwYuQroTQUHraQeDDJeSeQlFm@yamanote.proxy.rlwy.net:58793/railway"
    }
  }
});

async function main() {
  try {
    console.log("Connecting to main database...");
    const limits = await prisma.tenantLimit.findMany();
    console.log("Tenant Limits:", limits);
  } catch (err) {
    console.error("Error querying TenantLimit:", err);
  } finally {
    await prisma.$disconnect();
  }
}

main();
