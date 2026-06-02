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
    const premiumUsers = await prisma.premiumUser.findMany();
    console.log("Premium Users:", premiumUsers);

    const googleUsers = await prisma.googleUser.findMany();
    console.log("Google Users:", googleUsers);
  } catch (err) {
    console.error("Error connecting or querying database:", err);
  } finally {
    await prisma.$disconnect();
  }
}

main();
