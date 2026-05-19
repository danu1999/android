/**
 * Reset HANYA data Keuangan & Laporan (Transaksi).
 * Produk, Pelanggan, dan Karyawan TIDAK diubah.
 *
 * Jalankan: npx ts-node scripts/reset-finance.ts
 */

import { PrismaClient } from '@prisma/client';
import * as dotenv from 'dotenv';
dotenv.config();

const prisma = new PrismaClient();

async function main() {
  console.log('🔗 Menghubungkan ke database...');
  console.log('⚠️  Hanya Keuangan & Transaksi yang akan dihapus.\n');

  // Hitung dulu sebelum hapus
  const txCount  = await prisma.transaction.count();
  const finCount = await prisma.finance.count();
  console.log(`📊 Data yang akan dihapus:`);
  console.log(`   • Transaksi   : ${txCount} baris`);
  console.log(`   • Keuangan    : ${finCount} baris\n`);

  // ── Hapus (urutan FK: item dulu, lalu transaksi, lalu finance) ──
  const delItems = await prisma.transactionItem.deleteMany();
  console.log(`🗑  TransactionItem  : ${delItems.count} baris dihapus`);

  const delTx = await prisma.transaction.deleteMany();
  console.log(`🗑  Transaction      : ${delTx.count} baris dihapus`);

  const delFin = await prisma.finance.deleteMany();
  console.log(`🗑  Finance          : ${delFin.count} baris dihapus`);

  // ── Verifikasi yang TIDAK dihapus ──────────────────────────────
  const prodCount = await prisma.product.count();
  const custCount = await prisma.customer.count();
  const empCount  = await prisma.employee.count();
  console.log(`\n✅ Data yang tetap aman:`);
  console.log(`   • Produk      : ${prodCount} baris`);
  console.log(`   • Pelanggan   : ${custCount} baris`);
  console.log(`   • Karyawan    : ${empCount} baris`);

  console.log('\n✅ Selesai! Keuangan & Laporan sudah direset.');
}

main()
  .catch(err => {
    console.error('❌ Error:', err.message);
    process.exit(1);
  })
  .finally(() => prisma.$disconnect());
