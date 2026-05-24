import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  try {
    console.log('Starting customer data migration...');

    // 1. Get all customer IDs that have rental records
    const rentals = await prisma.rental.findMany({
      select: { customerId: true }
    });

    const rentalCustomerIds = Array.from(
      new Set(rentals.map(r => r.customerId).filter((id): id is number => id !== null && id !== undefined))
    );
    console.log(`Found rental customer IDs: ${rentalCustomerIds.join(', ')}`);

    // 2. Prepend '[RENTAL]' to their addresses if they don't already have it
    for (const customerId of rentalCustomerIds) {
      const customer = await prisma.customer.findUnique({ where: { id: customerId } });
      if (customer) {
        let currentAddress = customer.address || '';
        if (!currentAddress.startsWith('[RENTAL]')) {
          const newAddress = `[RENTAL] ${currentAddress}`.trim();
          await prisma.customer.update({
            where: { id: customerId },
            data: { address: newAddress }
          });
          console.log(`Updated customer "${customer.name}" (ID: ${customerId}) address to: "${newAddress}"`);
        } else {
          console.log(`Customer "${customer.name}" (ID: ${customerId}) is already tagged.`);
        }
      }
    }

    console.log('Migration completed successfully!');
  } catch (err) {
    console.error('Migration failed:', err);
  } finally {
    await prisma.$disconnect();
  }
}

main();
