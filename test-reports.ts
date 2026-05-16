import { PrismaClient } from '@prisma/client';
const prisma = new PrismaClient();
async function main() {
  try {
    const totalSales = await prisma.transaction.aggregate({
      where: { type: 'SALES' },
      _sum: { total: true }
    });
    console.log('Total sales:', totalSales);
  } catch (err) {
    console.error('Error:', err);
  }
}
main();
