const { PrismaClient } = require('@prisma/client');
const crypto = require('crypto');

const prisma = new PrismaClient();
const HASH_SALT = process.env.HASH_SALT || 'posbah_default_salt_secret';

function hashPassword(password) {
  return crypto.pbkdf2Sync(password, HASH_SALT, 1000, 64, 'sha512').toString('hex');
}

const users = [
  { email: 'hanafi@gmail.com', name: 'Hanafi', role: 'OWNER', tenantId: 'bahteramulyap@gmail.com' },
  { email: 'muizz@gmail.com', name: 'Muizz', role: 'OWNER', tenantId: 'bahteramulyap@gmail.com' },
  { email: 'fahri@gmail.com', name: 'Fahri', role: 'OWNER', tenantId: 'bahteramulyap@gmail.com' },
  { email: 'fed@gmail.com', name: 'Fed', role: 'OWNER', tenantId: 'bahteramulyap@gmail.com' },
];

const password = 'Bahtera1!';
const hashedPassword = hashPassword(password);

async function main() {
  console.log('Seeding premium users mapped to bahteramulyap@gmail.com...');
  for (const u of users) {
    const data = {
      id: u.email,
      email: u.email,
      passwordHash: hashedPassword,
      name: u.name,
      role: u.role,
      tenantId: u.tenantId,
    };
    await prisma.premiumUser.upsert({
      where: { email: u.email },
      update: {
        passwordHash: hashedPassword,
        name: u.name,
        role: u.role,
        tenantId: u.tenantId,
      },
      create: data,
    });
    console.log(`Upserted user: ${u.email} successfully.`);
  }
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
