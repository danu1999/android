import { PrismaClient } from '@prisma/client';

const sourceDbUrl = process.env.SOURCE_DATABASE_URL || "postgresql://postgres:posbah_vps_pass_2026@localhost:5432/bmp_db";
const targetDbUrl = process.env.TARGET_DATABASE_URL || "postgresql://postgres:posbah_vps_pass_2026@localhost:5432/posbah_tenant_bahteramulyap_gmail_com";

console.log(`Source DB: ${sourceDbUrl}`);
console.log(`Target DB: ${targetDbUrl}`);

const sourcePrisma = new PrismaClient({ datasources: { db: { url: sourceDbUrl } } });
const targetPrisma = new PrismaClient({ datasources: { db: { url: targetDbUrl } } });

async function insertRaw(tableName: string, data: any) {
  const keys = Object.keys(data);
  const columns = keys.map(k => `"${k}"`).join(', ');
  const values = keys.map((_, i) => `$${i + 1}`).join(', ');
  
  const query = `INSERT INTO "${tableName}" (${columns}) VALUES (${values})`;
  const params = keys.map(k => {
    const val = data[k];
    if (val instanceof Date) return val;
    if (typeof val === 'bigint') return Number(val);
    return val;
  });
  
  await targetPrisma.$executeRawUnsafe(query, ...params);
}

async function main() {
  try {
    console.log('=== Starting BMP Data Migration ===');

    // Helper: Truncate existing BMP tables in target
    console.log('Truncating target tables...');
    await targetPrisma.$executeRawUnsafe(`TRUNCATE TABLE 
      "BmpClient", "BmpInvoice", "BmpSettings", "BmpMasterProduct", "BmpProduct", 
      "BmpInvoicePayment", "BmpCashFlow", "BmpBahanNono", "BmpBahanNonoItem", 
      "BmpPembayaran", "BmpEmployee", "BmpPayroll", "BmpAdmsDevice", 
      "BmpAttendanceLog", "BmpMachineBonusLog" CASCADE;`);

    // 1. Clients
    console.log('Migrating BmpClient...');
    const clients: any[] = await sourcePrisma.$queryRawUnsafe(`SELECT * FROM clients WHERE deleted_at IS NULL AND is_demo = false;`);
    for (const c of clients) {
      await insertRaw('BmpClient', {
        id: Number(c.id),
        saldoTitipan: Number(c.saldo_titipan) || 0,
        clientName: c.client_name,
        addressLine1: c.address_line1,
        clientLogo: c.client_logo,
        province: c.province,
        postalCode: c.postal_code,
        phoneNumber: c.phone_number,
        emailAddress: c.email_address,
        taxNumber: c.tax_number,
        uniqueID: c.unique_id,
        slug: c.slug,
        createdAt: new Date(c.created_at || c.date_created || Date.now()),
        updatedAt: new Date(c.updated_at || c.last_updated || Date.now())
      });
    }

    // 2. Invoices
    console.log('Migrating BmpInvoice...');
    const invoices: any[] = await sourcePrisma.$queryRawUnsafe(`SELECT * FROM invoices WHERE deleted_at IS NULL AND is_demo = false;`);
    for (const inv of invoices) {
      await insertRaw('BmpInvoice', {
        id: Number(inv.id),
        title: inv.title || "Invoice",
        number: inv.number,
        dueDate: inv.due_date ? new Date(inv.due_date) : null,
        paymentTerms: inv.payment_terms || "14 days",
        status: inv.status || "DRAFT",
        notes: inv.notes,
        clientId: inv.client_id ? Number(inv.client_id) : null,
        uniqueID: inv.unique_id,
        slug: inv.slug,
        createdAt: new Date(inv.created_at || inv.date_created || Date.now()),
        updatedAt: new Date(inv.updated_at || inv.last_updated || Date.now())
      });
    }

    // 3. Settings
    console.log('Migrating BmpSettings...');
    const settings: any[] = await sourcePrisma.$queryRawUnsafe(`SELECT * FROM settings WHERE deleted_at IS NULL AND is_demo = false;`);
    for (const s of settings) {
      await insertRaw('BmpSettings', {
        id: Number(s.id),
        clientName: s.client_name,
        clientLogo: s.client_logo,
        addressLine1: s.address_line1,
        province: s.province,
        postalCode: s.postal_code,
        phoneNumber: s.phone_number,
        emailAddress: s.email_address,
        taxNumber: s.tax_number,
        listrikBulanan: Number(s.listrik_bulanan) || 30000000,
        jumlahMesin: s.jumlah_mesin || 5,
        jumlahKaryawan: s.jumlah_karyawan || 19,
        gajiHarian: Number(s.gaji_harian) || 80000,
        hariKerjaSebulan: s.hari_kerja_sebulan || 26,
        biayaKarungPer1000: Number(s.biaya_karung_per1000) || 2100000,
        hoursPerDay: s.hours_per_day || 24,
        uniqueID: s.unique_id,
        slug: s.slug,
        createdAt: new Date(s.created_at || s.date_created || Date.now()),
        updatedAt: new Date(s.updated_at || s.last_updated || Date.now())
      });
    }

    // 4. Master Products
    console.log('Migrating BmpMasterProduct...');
    const masterProducts: any[] = await sourcePrisma.$queryRawUnsafe(`SELECT * FROM master_products WHERE deleted_at IS NULL AND is_demo = false;`);
    for (const mp of masterProducts) {
      await insertRaw('BmpMasterProduct', {
        id: Number(mp.id),
        title: mp.title,
        description: mp.description,
        unit: mp.unit || "Kg",
        price: Number(mp.price) || 0,
        beratGram: Number(mp.berat_gram) || 0,
        cycleTime: Number(mp.cycle_time) || 0,
        cavity: mp.cavity || 1,
        rejectRate: Number(mp.reject_rate) || 0,
        uniqueID: mp.unique_id,
        slug: mp.slug,
        createdAt: new Date(mp.created_at || mp.date_created || Date.now()),
        updatedAt: new Date(mp.updated_at || mp.last_updated || Date.now())
      });
    }

    // 5. Products
    console.log('Migrating BmpProduct...');
    const products: any[] = await sourcePrisma.$queryRawUnsafe(`
      SELECT p.* FROM products p 
      INNER JOIN invoices i ON p.invoice_id = i.id 
      WHERE p.deleted_at IS NULL AND i.is_demo = false AND i.deleted_at IS NULL;
    `);
    for (const p of products) {
      await insertRaw('BmpProduct', {
        id: Number(p.id),
        masterItemID: p.master_item_id ? Number(p.master_item_id) : null,
        title: p.title || "Product",
        unit: p.unit || "pcs",
        price: Number(p.price) || 0,
        jumlahLusin: Number(p.jumlah_lusin) || 1,
        quantity: Number(p.quantity) || 0,
        isKhusus: p.is_khusus || false,
        hargaBeli: Number(p.harga_beli) || 0,
        currency: p.currency || "Rp",
        invoiceId: p.invoice_id ? Number(p.invoice_id) : null,
        uniqueID: p.unique_id,
        slug: p.slug,
        createdAt: new Date(p.created_at || p.date_created || Date.now()),
        updatedAt: new Date(p.updated_at || p.last_updated || Date.now())
      });
    }

    // 6. Invoice Payments
    console.log('Migrating BmpInvoicePayment...');
    const payments: any[] = await sourcePrisma.$queryRawUnsafe(`
      SELECT ip.* FROM invoice_payments ip 
      INNER JOIN invoices i ON ip.invoice_id = i.id 
      WHERE ip.deleted_at IS NULL AND i.is_demo = false AND i.deleted_at IS NULL;
    `);
    for (const pay of payments) {
      await insertRaw('BmpInvoicePayment', {
        id: Number(pay.id),
        invoiceId: Number(pay.invoice_id),
        paymentDate: new Date(pay.payment_date || Date.now()),
        paymentAmount: Number(pay.payment_amount) || 0,
        paymentMethod: pay.payment_method || "TRANSFER",
        createdAt: new Date(pay.created_at || pay.date_created || Date.now())
      });
    }

    // 7. Cash Flows
    console.log('Migrating BmpCashFlow...');
    const cashflows: any[] = await sourcePrisma.$queryRawUnsafe(`SELECT * FROM cash_flows WHERE deleted_at IS NULL AND is_demo = false;`);
    for (const cf of cashflows) {
      await insertRaw('BmpCashFlow', {
        id: Number(cf.id),
        transactionDate: new Date(cf.transaction_date || Date.now()),
        transactionType: cf.transaction_type,
        description: cf.description,
        amount: Number(cf.amount) || 0,
        paymentRefId: cf.payment_ref_id ? Number(cf.payment_ref_id) : null,
        createdAt: new Date(cf.created_at || cf.date_created || Date.now())
      });
    }

    // 8. Bahan Nonos
    console.log('Migrating BmpBahanNono...');
    const nonos: any[] = await sourcePrisma.$queryRawUnsafe(`SELECT * FROM bahan_nonos WHERE deleted_at IS NULL AND is_demo = false;`);
    for (const n of nonos) {
      await insertRaw('BmpBahanNono', {
        id: Number(n.id),
        tanggal: new Date(n.tanggal || Date.now()),
        nominal: Number(n.nominal) || 0,
        notes: n.notes,
        tagihan: n.tagihan || "",
        totalHarga: Number(n.total_harga) || 0,
        createdAt: new Date(n.created_at || n.date_created || Date.now())
      });
    }

    // 9. Bahan Nono Items
    console.log('Migrating BmpBahanNonoItem...');
    const nonoItems: any[] = await sourcePrisma.$queryRawUnsafe(`
      SELECT ni.* FROM bahan_nono_items ni 
      INNER JOIN bahan_nonos n ON ni.bahan_nono_id = n.id 
      WHERE ni.deleted_at IS NULL AND n.is_demo = false AND n.deleted_at IS NULL;
    `);
    for (const ni of nonoItems) {
      await insertRaw('BmpBahanNonoItem', {
        id: Number(ni.id),
        bahanNonoId: Number(ni.bahan_nono_id),
        jenisBahan: ni.jenis_bahan || "",
        kuantitas: Number(ni.kuantitas) || 0,
        unit: ni.unit || "Kg",
        rate: Number(ni.rate) || 0
      });
    }

    // 10. Pembayarans
    console.log('Migrating BmpPembayaran...');
    const pembayarans: any[] = await sourcePrisma.$queryRawUnsafe(`
      SELECT p.* FROM pembayarans p 
      INNER JOIN invoices i ON p.invoice_id = i.id 
      WHERE p.deleted_at IS NULL AND i.is_demo = false AND i.deleted_at IS NULL;
    `);
    for (const p of pembayarans) {
      await insertRaw('BmpPembayaran', {
        id: Number(p.id),
        invoiceId: Number(p.invoice_id),
        tanggalBayar: new Date(p.tanggal_bayar || Date.now()),
        jumlahBayar: Number(p.jumlah_bayar) || 0,
        keterangan: p.keterangan
      });
    }

    // 11. Employees
    console.log('Migrating BmpEmployee...');
    const employees: any[] = await sourcePrisma.$queryRawUnsafe(`SELECT * FROM employees WHERE deleted_at IS NULL AND is_demo = false;`);
    for (const emp of employees) {
      await insertRaw('BmpEmployee', {
        id: Number(emp.id),
        name: emp.name,
        position: emp.position,
        salaryAmount: Number(emp.salary_amount) || 0,
        isActive: emp.is_active !== false,
        fingerprintPIN: emp.fingerprint_pin,
        createdAt: new Date(emp.created_at || Date.now()),
        updatedAt: new Date(emp.updated_at || Date.now())
      });
    }

    // 12. Payrolls
    console.log('Migrating BmpPayroll...');
    const payrolls: any[] = await sourcePrisma.$queryRawUnsafe(`SELECT * FROM payrolls WHERE deleted_at IS NULL AND is_demo = false;`);
    for (const pr of payrolls) {
      await insertRaw('BmpPayroll', {
        id: Number(pr.id),
        employeeId: Number(pr.employee_id),
        paymentDate: new Date(pr.payment_date || Date.now()),
        amount: Number(pr.amount) || 0,
        attendanceCount: pr.attendance_count || 0,
        dailyRate: Number(pr.daily_rate) || 0,
        description: pr.description,
        createdAt: new Date(pr.created_at || Date.now())
      });
    }

    // 13. ADMS Devices
    console.log('Migrating BmpAdmsDevice...');
    const devices: any[] = await sourcePrisma.$queryRawUnsafe(`SELECT * FROM adms_devices WHERE deleted_at IS NULL AND is_demo = false;`);
    for (const d of devices) {
      await insertRaw('BmpAdmsDevice', {
        id: Number(d.id),
        serialNumber: d.serial_number,
        alias: d.alias,
        lastActivity: new Date(d.last_activity || Date.now()),
        createdAt: new Date(d.created_at || Date.now())
      });
    }

    // 14. Attendance Logs
    console.log('Migrating BmpAttendanceLog...');
    const logs: any[] = await sourcePrisma.$queryRawUnsafe(`SELECT * FROM attendance_logs WHERE deleted_at IS NULL AND is_demo = false;`);
    for (const l of logs) {
      await insertRaw('BmpAttendanceLog', {
        id: Number(l.id),
        deviceSN: l.device_sn,
        employeePIN: l.employee_pin,
        verifyType: l.verify_type || 0,
        verifyState: l.verify_state || 0,
        logTime: new Date(l.log_time || Date.now()),
        checkOutTime: l.check_out_time ? new Date(l.check_out_time) : null,
        workDate: new Date(l.work_date || Date.now()),
        lateMinutes: l.late_minutes || 0,
        alasan: l.alasan,
        createdAt: new Date(l.created_at || Date.now())
      });
    }

    // 15. Machine Bonus Logs
    console.log('Migrating BmpMachineBonusLog...');
    const bonuslogs: any[] = await sourcePrisma.$queryRawUnsafe(`SELECT * FROM machine_bonus_logs WHERE deleted_at IS NULL AND is_demo = false;`);
    for (const bl of bonuslogs) {
      await insertRaw('BmpMachineBonusLog', {
        id: Number(bl.id),
        employeeId: Number(bl.employee_id),
        machineName: bl.machine_name || "",
        shiftType: bl.shift_type || "",
        bonusAmount: Number(bl.bonus_amount) || 0,
        jumlahPerolehan: bl.jumlah_perolehan || 0,
        date: new Date(bl.date || Date.now()),
        createdAt: new Date(bl.created_at || Date.now())
      });
    }

    // Sequence resets for auto-increment keys
    console.log('Resetting database sequences...');
    const tables = [
      "BmpClient", "BmpInvoice", "BmpSettings", "BmpMasterProduct", "BmpProduct", 
      "BmpInvoicePayment", "BmpCashFlow", "BmpBahanNono", "BmpBahanNonoItem", 
      "BmpPembayaran", "BmpEmployee", "BmpPayroll", "BmpAdmsDevice", 
      "BmpAttendanceLog", "BmpMachineBonusLog"
    ];
    for (const table of tables) {
      await targetPrisma.$executeRawUnsafe(`
        SELECT setval('"${table}_id_seq"', COALESCE((SELECT MAX(id) FROM "${table}"), 1));
      `);
    }

    console.log('=== BMP Data Migration Completed Successfully! ===');
  } catch (err) {
    console.error('Migration failed with error:', err);
  } finally {
    await sourcePrisma.$disconnect();
    await targetPrisma.$disconnect();
  }
}

main();
