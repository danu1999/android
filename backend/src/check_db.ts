import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  try {
    console.log('=== LATEST 10 RENTAL RECORDS ===');
    const rentals = await prisma.rental.findMany({
      take: 10,
      orderBy: { id: 'desc' },
      include: { car: true, customer: true }
    });
    console.dir(rentals, { depth: null });

    console.log('\n=== LATEST 10 FINANCE RECORDS ===');
    const finances = await prisma.finance.findMany({
      take: 10,
      orderBy: { id: 'desc' }
    });
    console.dir(finances, { depth: null });

    console.log('\n=== LATEST 10 CUSTOMER RECORDS ===');
    const customers = await prisma.customer.findMany({
      take: 10,
      orderBy: { id: 'desc' }
    });
    console.dir(customers, { depth: null });
  } catch (err) {
    console.error(err);
  } finally {
    await prisma.$disconnect();
  }
}

main();
