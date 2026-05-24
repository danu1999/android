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
            console.log('Starting customer data migration...');
            // 1. Get all customer IDs that have rental records
            const rentals = yield prisma.rental.findMany({
                select: { customerId: true }
            });
            const rentalCustomerIds = Array.from(new Set(rentals.map(r => r.customerId).filter((id) => id !== null && id !== undefined)));
            console.log(`Found rental customer IDs: ${rentalCustomerIds.join(', ')}`);
            // 2. Prepend '[RENTAL]' to their addresses if they don't already have it
            for (const customerId of rentalCustomerIds) {
                const customer = yield prisma.customer.findUnique({ where: { id: customerId } });
                if (customer) {
                    let currentAddress = customer.address || '';
                    if (!currentAddress.startsWith('[RENTAL]')) {
                        const newAddress = `[RENTAL] ${currentAddress}`.trim();
                        yield prisma.customer.update({
                            where: { id: customerId },
                            data: { address: newAddress }
                        });
                        console.log(`Updated customer "${customer.name}" (ID: ${customerId}) address to: "${newAddress}"`);
                    }
                    else {
                        console.log(`Customer "${customer.name}" (ID: ${customerId}) is already tagged.`);
                    }
                }
            }
            console.log('Migration completed successfully!');
        }
        catch (err) {
            console.error('Migration failed:', err);
        }
        finally {
            yield prisma.$disconnect();
        }
    });
}
main();
