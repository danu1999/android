/**
 * Script sekali pakai: bersihkan semua data keuangan, pelanggan, transaksi,
 * lalu buat ulang karyawan dengan nama/pin/role yang sama.
 *
 * Jalankan: npx ts-node scripts/reset-data.ts
 */

import { PrismaClient } from '@prisma/client';
import * as dotenv from 'dotenv';
dotenv.config();

const prisma = new PrismaClient();

async function main() {
  console.log('🔗 Menghubungkan ke database...');

  // ── 1. Simpan data karyawan ──────────────────────────────────
  const employees = await prisma.employee.findMany({
    select: { name: true, pin: true, role: true }
  });
  console.log(`\n✅ ${employees.length} karyawan ditemukan:`);
  employees.forEach(e => console.log(`   • ${e.name} | role: ${e.role} | pin: ${e.pin}`));

  // ── 2. Hapus semua data (urutan FK penting) ──────────────────
  console.log('\n🗑  Membersihkan data...');

  const delItems   = await prisma.transactionItem.deleteMany();
  console.log(`   TransactionItem  : ${delItems.count} baris dihapus`);

  const delTx      = await prisma.transaction.deleteMany();
  console.log(`   Transaction      : ${delTx.count} baris dihapus`);

  const delFinance = await prisma.finance.deleteMany();
  console.log(`   Finance          : ${delFinance.count} baris dihapus`);

  const delCust    = await prisma.customer.deleteMany();
  console.log(`   Customer         : ${delCust.count} baris dihapus`);

  const delEmp     = await prisma.employee.deleteMany();
  console.log(`   Employee         : ${delEmp.count} baris dihapus`);

  // ── 3. Buat ulang karyawan ───────────────────────────────────
  console.log('\n👤 Membuat ulang karyawan...');
  for (const emp of employees) {
    const created = await prisma.employee.create({
      data: { name: emp.name, pin: emp.pin, role: emp.role }
    });
    console.log(`   ✓ ID ${created.id} | ${created.name} | ${created.role}`);
  }

  // ── 4. Verifikasi ────────────────────────────────────────────
  const final = await prisma.employee.findMany({ orderBy: { id: 'asc' } });
  console.log('\n📋 Hasil akhir karyawan:');
  console.table(final.map(e => ({ id: e.id, name: e.name, role: e.role, pin: e.pin })));

  console.log('\n✅ Selesai! Database sudah bersih dan karyawan sudah dibuat ulang.');
}

main()
  .catch(err => {
    console.error('❌ Error:', err.message);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());
