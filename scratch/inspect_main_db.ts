import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient({
  datasources: {
    db: {
      url: "postgresql://postgres:Bahtera1!@localhost:5432/posbah"
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
