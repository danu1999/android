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
            console.log('=== LATEST 10 RENTAL RECORDS ===');
            const rentals = yield prisma.rental.findMany({
                take: 10,
                orderBy: { id: 'desc' },
                include: { car: true, customer: true }
            });
            console.dir(rentals, { depth: null });
            console.log('\n=== LATEST 10 FINANCE RECORDS ===');
            const finances = yield prisma.finance.findMany({
                take: 10,
                orderBy: { id: 'desc' }
            });
            console.dir(finances, { depth: null });
            console.log('\n=== LATEST 10 CUSTOMER RECORDS ===');
            const customers = yield prisma.customer.findMany({
                take: 10,
                orderBy: { id: 'desc' }
            });
            console.dir(customers, { depth: null });
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
