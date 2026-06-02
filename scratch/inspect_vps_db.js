const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();
async function main() {
  const limits = await prisma.tenantLimit.findMany();
  console.log("Tenant Limits on VPS:", limits);
  const premium = await prisma.premiumUser.findMany();
  console.log("Premium Users on VPS:", premium);
}
main().catch(console.error).finally(() => prisma.$disconnect());
