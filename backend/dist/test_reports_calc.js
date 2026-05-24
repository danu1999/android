"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const client_1 = require("@prisma/client");
const prisma = new client_1.PrismaClient();
function main() {
    return __awaiter(this, void 0, void 0, function* () {
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
            const expenseWhere = Object.assign({ type: 'EXPENSE' }, financeBaseWhere);
            const receivableWhere = Object.assign({ type: 'RECEIVABLE', status: 'PENDING' }, financeBaseWhere);
            const payableWhere = Object.assign({ type: 'PAYABLE', status: 'PENDING' }, financeBaseWhere);
            const dateFilter = {};
            let dateFilterActive = false;
            if (from || to) {
                if (from)
                    dateFilter.gte = new Date(from);
                if (to) {
                    const toDate = new Date(to);
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
                const rentalWhere = {};
                if (dateFilterActive) {
                    rentalWhere.createdAt = dateFilter;
                }
                const totalRentals = yield prisma.rental.aggregate({
                    where: rentalWhere,
                    _sum: { totalPrice: true }
                });
                totalSalesVal = totalRentals._sum.totalPrice || 0;
                const todayRentals = yield prisma.rental.aggregate({
                    where: {
                        createdAt: { gte: startOfToday, lte: endOfToday }
                    },
                    _sum: { totalPrice: true }
                });
                todaySalesVal = todayRentals._sum.totalPrice || 0;
            }
            const expenses = yield prisma.finance.aggregate({
                where: expenseWhere,
                _sum: { amount: true }
            });
            const receivables = yield prisma.finance.aggregate({
                where: receivableWhere,
                _sum: { amount: true }
            });
            const payables = yield prisma.finance.aggregate({
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
        }
        catch (err) {
            console.error(err);
        }
        finally {
            yield prisma.$disconnect();
        }
    });
}
main();
