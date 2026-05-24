import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  try {
    const appMode = 'RENTAL';
    const from = '';
    const to = '';

    const financeBaseWhere = appMode === 'RENTAL' ? {
      OR: [
        { description: { startsWith: '[RENTAL]' } },
        { description: { startsWith: 'Sewa Mobil' } },
        { description: { startsWith: 'Denda Telat Sewa Mobil' } }
      ]
    } : {
      AND: [
        { description: { not: { startsWith: '[RENTAL]' } } },
        { description: { not: { startsWith: 'Sewa Mobil' } } },
        { description: { not: { startsWith: 'Denda Telat' } } }
      ]
    };

    const expenseWhere: any = { type: 'EXPENSE', ...financeBaseWhere };
    const receivableWhere: any = { type: 'RECEIVABLE', status: 'PENDING', ...financeBaseWhere };
    const payableWhere: any = { type: 'PAYABLE', status: 'PENDING', ...financeBaseWhere };

    const dateFilter: any = {};
    let dateFilterActive = false;
    if (from || to) {
      if (from) dateFilter.gte = new Date(from as string);
      if (to) {
        const toDate = new Date(to as string);
        toDate.setHours(23, 59, 59, 999);
        dateFilter.lte = toDate;
      }
      dateFilterActive = true;
      expenseWhere.date = dateFilter;
      receivableWhere.date = dateFilter;
      payableWhere.date = dateFilter;
    }

    let totalSalesVal = 0;
    let todaySalesVal = 0;

    const tzOffset = 7 * 60 * 60 * 1000;
    const localNow = new Date(Date.now() + tzOffset);
    const startOfToday = new Date(Date.UTC(localNow.getUTCFullYear(), localNow.getUTCMonth(), localNow.getUTCDate(), 0, 0, 0, 0) - tzOffset);
    const endOfToday = new Date(Date.UTC(localNow.getUTCFullYear(), localNow.getUTCMonth(), localNow.getUTCDate(), 23, 59, 59, 999) - tzOffset);

    console.log('Today range UTC:', startOfToday.toISOString(), 'to', endOfToday.toISOString());

    if (appMode === 'RENTAL') {
      const rentalWhere: any = {};
      if (dateFilterActive) {
        rentalWhere.createdAt = dateFilter;
      }
      const totalRentals = await prisma.rental.aggregate({
        where: rentalWhere,
        _sum: { totalPrice: true }
      });
      totalSalesVal = totalRentals._sum.totalPrice || 0;

      const todayRentals = await prisma.rental.aggregate({
        where: {
          createdAt: { gte: startOfToday, lte: endOfToday }
        },
        _sum: { totalPrice: true }
      });
      todaySalesVal = todayRentals._sum.totalPrice || 0;
    }

    const expenses = await prisma.finance.aggregate({
      where: expenseWhere,
      _sum: { amount: true }
    });
    const receivables = await prisma.finance.aggregate({
      where: receivableWhere,
      _sum: { amount: true }
    });
    const payables = await prisma.finance.aggregate({
      where: payableWhere,
      _sum: { amount: true }
    });

    console.log({
      totalSalesVal,
      todaySalesVal,
      expensesSum: expenses._sum.amount || 0,
      receivablesSum: receivables._sum.amount || 0,
      payablesSum: payables._sum.amount || 0,
      netIncome: totalSalesVal - (expenses._sum.amount || 0)
    });
  } catch (err) {
    console.error(err);
  } finally {
    await prisma.$disconnect();
  }
}

main();
