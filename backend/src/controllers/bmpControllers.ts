import { Request, Response } from 'express';
import { prisma, mainPrisma } from '../index';
import crypto from 'crypto';
import path from 'path';
import fs from 'fs';
import { exec } from 'child_process';
import ejs from 'ejs';

// ─── HELPERS ─────────────────────────────────────────────────────────────────

export function formatRp(amount: number): string {
  return 'Rp' + amount.toLocaleString('id-ID', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

export function formatRpComma(amount: number): string {
  return amount.toLocaleString('id-ID', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

export function formatDate(date: any): string {
  if (!date) return '-';
  const d = new Date(date);
  if (isNaN(d.getTime())) return '-';
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
}

export async function generatePdf(html: string): Promise<Buffer> {
  const tempId = crypto.randomBytes(8).toString('hex');
  const tempHtmlPath = path.join(__dirname, `../../temp_${tempId}.html`);
  const tempPdfPath = path.join(__dirname, `../../temp_${tempId}.pdf`);

  await fs.promises.writeFile(tempHtmlPath, html, 'utf-8');

  return new Promise((resolve, reject) => {
    // wkhtmltopdf is globally installed on the VPS
    const cmd = `wkhtmltopdf --page-width 241 --page-height 279 --margin-top 5 --margin-right 10 --margin-bottom 10 --margin-left 10 "${tempHtmlPath}" "${tempPdfPath}"`;
    exec(cmd, async (error, stdout, stderr) => {
      // Clean up html
      try { await fs.promises.unlink(tempHtmlPath); } catch (_) {}

      if (error) {
        console.error('wkhtmltopdf error:', error, stderr);
        return reject(error);
      }

      try {
        const pdfBuffer = await fs.promises.readFile(tempPdfPath);
        await fs.promises.unlink(tempPdfPath);
        resolve(pdfBuffer);
      } catch (readErr) {
        reject(readErr);
      }
    });
  });
}

// ─── DASHBOARD ───────────────────────────────────────────────────────────────

export const getDashboardSummary = async (req: Request, res: Response) => {
  try {
    const totalClients = await prisma.bmpClient.count();
    const totalProducts = await prisma.bmpMasterProduct.count();
    const countInvoices = await prisma.bmpInvoice.count({
      where: { NOT: { status: 'DRAFT' } }
    });

    const cashflows = await prisma.bmpCashFlow.findMany();
    const totalKasIn = cashflows
      .filter((cf) => cf.transactionType === 'MASUK')
      .reduce((sum, cf) => sum + cf.amount, 0);
    const totalKasOut = cashflows
      .filter((cf) => cf.transactionType === 'KELUAR')
      .reduce((sum, cf) => sum + cf.amount, 0);

    const nonoData = await prisma.bmpBahanNono.findMany();
    const nonoTotalBahan = nonoData.reduce((sum, n) => sum + n.totalHarga, 0);
    const nonoTotalBayar = nonoData.reduce((sum, n) => sum + n.nominal, 0);
    const nonoSisaHutang = nonoTotalBahan - nonoTotalBayar;

    const saldoKas = totalKasIn - totalKasOut - nonoTotalBayar;

    const recentInvoicesRaw = await prisma.bmpInvoice.findMany({
      orderBy: { createdAt: 'desc' },
      take: 5,
      include: {
        client: true,
        products: true
      }
    });

    const recent = recentInvoicesRaw.map((inv) => {
      const total = inv.products.reduce((sum, p) => sum + p.quantity * p.jumlahLusin * p.price, 0);
      return {
        number: inv.number,
        client_name: inv.client?.clientName || '-',
        get_total: total,
        status: inv.status
      };
    });

    const allInvoices = await prisma.bmpInvoice.findMany({
      where: { NOT: { status: 'DRAFT' } },
      include: { products: true }
    });

    let countLunas = 0;
    let countBelum = 0;
    let countTelat = 0;
    let totalInvoicesIdr = 0;
    let totalLunasIdr = 0;
    let totalBelumIdr = 0;
    let totalTelatIdr = 0;

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    for (const inv of allInvoices) {
      const total = inv.products.reduce((sum, p) => sum + p.quantity * p.jumlahLusin * p.price, 0);
      totalInvoicesIdr += total;

      if (inv.status === 'PAID') {
        countLunas++;
        totalLunasIdr += total;
      } else {
        if (inv.dueDate && new Date(inv.dueDate) < today) {
          countTelat++;
          totalTelatIdr += total;
        } else {
          countBelum++;
          totalBelumIdr += total;
        }
      }
    }

    const simulasiSaldo = totalInvoicesIdr - nonoTotalBahan - totalKasOut;

    res.json({
      success: true,
      data: {
        total_clients: totalClients,
        total_products: totalProducts,
        count_invoices: countInvoices,
        total_kas_in: totalKasIn,
        total_kas_out: totalKasOut,
        saldo_kas: saldoKas,
        nono_total_bahan: nonoTotalBahan,
        nono_total_bayar: nonoTotalBayar,
        nono_sisa_hutang: nonoSisaHutang,
        recent_invoices: recent,
        count_lunas: countLunas,
        total_lunas_idr: totalLunasIdr,
        count_belum: countBelum,
        total_belum_idr: totalBelumIdr,
        count_telat: countTelat,
        total_telat_idr: totalTelatIdr,
        total_invoices_idr: totalInvoicesIdr,
        simulasi_saldo: simulasiSaldo
      }
    });
  } catch (error: any) {
    console.error('getDashboardSummary error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── SETTINGS ────────────────────────────────────────────────────────────────

export const getSettings = async (req: Request, res: Response) => {
  try {
    let settings = await prisma.bmpSettings.findFirst();
    if (!settings) {
      settings = await prisma.bmpSettings.create({
        data: {
          clientName: 'CV Bahtera Mulya Plastik',
          clientLogo: '',
          addressLine1: 'Jl. Arimbi, RT04 RW 01 Desa Ngrimbi',
          province: 'Jawa Timur',
          postalCode: '61476',
          phoneNumber: '08123456789',
          emailAddress: 'bahteramulyap@gmail.com',
          taxNumber: '00.000.000.0-000.000',
          listrikBulanan: 30000000,
          jumlahMesin: 5,
          jumlahKaryawan: 19,
          gajiHarian: 80000,
          hariKerjaSebulan: 26,
          biayaKarungPer1000: 2100000,
          hoursPerDay: 24,
          uniqueID: crypto.randomBytes(4).toString('hex')
        }
      });
    }
    res.json({ success: true, data: settings });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const updateSettings = async (req: Request, res: Response) => {
  try {
    const data = req.body;
    let settings = await prisma.bmpSettings.findFirst();
    if (!settings) {
      settings = await prisma.bmpSettings.create({ data });
    } else {
      settings = await prisma.bmpSettings.update({
        where: { id: settings.id },
        data
      });
    }
    res.json({ success: true, data: settings });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── REPORTS & HPP ───────────────────────────────────────────────────────────

export const getHppCalculator = async (req: Request, res: Response) => {
  try {
    const settings = await prisma.bmpSettings.findFirst();
    const masterProducts = await prisma.bmpMasterProduct.findMany();

    if (!settings) {
      return res.status(400).json({ success: false, message: 'Settings not configured yet.' });
    }

    // Hitung Biaya Operasional Tetap Bulanan
    const totalGaji = settings.jumlahKaryawan * settings.gajiHarian * settings.hariKerjaSebulan;
    const totalOperasionalBulanan = settings.listrikBulanan + totalGaji;

    // Hitung Biaya Operasional Per Detik Mesin
    const totalDetikSebulan = settings.jumlahMesin * settings.hariKerjaSebulan * settings.hoursPerDay * 3600;
    const biayaPerDetik = totalOperasionalBulanan / totalDetikSebulan;

    const products = masterProducts.map((p) => {
      const biayaMesin = p.cycleTime * biayaPerDetik;
      const rejectMultiplier = 1 + p.rejectRate / 100;

      // HPP per Pcs (beratGram * hargaBijihPlastik + biayaMesin) * rejectMultiplier
      const hppSatuan = (p.beratGram * (p.price / 1000) + biayaMesin) * rejectMultiplier;

      // Tambahkan biaya karung per pcs
      const biayaKarungPcs = settings.biayaKarungPer1000 / 1000;
      const hppTotalPcs = hppSatuan + biayaKarungPcs;

      return {
        id: p.id,
        title: p.title,
        unit: p.unit,
        beratGram: p.beratGram,
        cycleTime: p.cycleTime,
        rejectRate: p.rejectRate,
        biayaMesin,
        hppSatuan,
        hppTotalPcs,
        profitMargin: p.price - hppTotalPcs
      };
    });

    res.json({
      success: true,
      data: {
        biaya_per_detik: biayaPerDetik,
        biaya_operasional_bulanan: totalOperasionalBulanan,
        products
      }
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getPricelist = async (req: Request, res: Response) => {
  try {
    const products = await prisma.bmpMasterProduct.findMany({
      orderBy: { title: 'asc' }
    });
    res.json({ success: true, data: products });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── CLIENTS ─────────────────────────────────────────────────────────────────

export const getClients = async (req: Request, res: Response) => {
  try {
    const clients = await prisma.bmpClient.findMany({
      orderBy: { clientName: 'asc' }
    });
    res.json({ success: true, data: clients });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getClient = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const client = await prisma.bmpClient.findUnique({
      where: { id: Number(id) }
    });
    if (!client) {
      return res.status(404).json({ success: false, message: 'Client not found' });
    }
    res.json({ success: true, data: client });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const createClient = async (req: Request, res: Response) => {
  try {
    const data = req.body;
    const uniqueID = crypto.randomBytes(4).toString('hex');
    const slug = `${(data.clientName || '').toLowerCase().replace(/[^a-z0-9]/g, '-')}-${uniqueID}`;

    const client = await prisma.bmpClient.create({
      data: {
        ...data,
        uniqueID,
        slug
      }
    });
    res.status(201).json({ success: true, data: client });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const updateClient = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const data = req.body;
    // Omit fields that shouldn't be overwritten
    delete data.id;
    delete data.uniqueID;

    const client = await prisma.bmpClient.update({
      where: { id: Number(id) },
      data
    });
    res.json({ success: true, data: client });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const deleteClient = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    await prisma.bmpClient.delete({
      where: { id: Number(id) }
    });
    res.json({ success: true, message: 'Client successfully deleted' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getClientSummary = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const client = await prisma.bmpClient.findUnique({
      where: { id: Number(id) }
    });
    if (!client) {
      return res.status(404).json({ success: false, message: 'Client not found' });
    }

    const invoices = await prisma.bmpInvoice.findMany({
      where: { clientId: Number(id), NOT: { status: 'PAID' } },
      include: {
        products: true,
        payments: true
      }
    });

    let totalTunggakan = 0;
    let unpaidCount = 0;

    for (const inv of invoices) {
      const totalInv = inv.products.reduce((sum, p) => sum + p.quantity * p.jumlahLusin * p.price, 0);
      const totalPaid = inv.payments.reduce((sum, p) => sum + p.paymentAmount, 0);
      const sisa = totalInv - totalPaid;
      if (sisa > 0) {
        totalTunggakan += sisa;
        unpaidCount++;
      }
    }

    res.json({
      success: true,
      data: {
        client_name: client.clientName,
        total_tunggakan: totalTunggakan,
        unpaid_count: unpaidCount,
        saldo_borongan: client.saldoTitipan
      }
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── MASTER PRODUCTS ─────────────────────────────────────────────────────────

export const getProducts = async (req: Request, res: Response) => {
  try {
    const products = await prisma.bmpMasterProduct.findMany({
      orderBy: { title: 'asc' }
    });
    res.json({ success: true, data: products });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getProduct = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const product = await prisma.bmpMasterProduct.findUnique({
      where: { id: Number(id) }
    });
    if (!product) {
      return res.status(404).json({ success: false, message: 'Product not found' });
    }
    res.json({ success: true, data: product });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const createProduct = async (req: Request, res: Response) => {
  try {
    const data = req.body;
    const uniqueID = crypto.randomBytes(4).toString('hex');
    const slug = `${(data.title || '').toLowerCase().replace(/[^a-z0-9]/g, '-')}-${uniqueID}`;

    const product = await prisma.bmpMasterProduct.create({
      data: {
        ...data,
        uniqueID,
        slug
      }
    });
    res.status(201).json({ success: true, data: product });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const updateProduct = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const data = req.body;
    delete data.id;
    delete data.uniqueID;

    const product = await prisma.bmpMasterProduct.update({
      where: { id: Number(id) },
      data
    });
    res.json({ success: true, data: product });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const deleteProduct = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    await prisma.bmpMasterProduct.delete({
      where: { id: Number(id) }
    });
    res.json({ success: true, message: 'Product successfully deleted' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── INVOICES ────────────────────────────────────────────────────────────────

export const getInvoices = async (req: Request, res: Response) => {
  try {
    const page = Math.max(1, Number(req.query.page || 1));
    const limit = Math.min(200, Math.max(1, Number(req.query.limit || 20)));
    const offset = (page - 1) * limit;

    const search = (req.query.search as string) || '';
    const status = (req.query.status as string) || '';
    const clientIDStr = (req.query.client_id as string) || '';

    const whereClause: any = {};

    if (status && status !== 'ALL') {
      whereClause.status = status;
    }

    if (clientIDStr && clientIDStr !== 'ALL') {
      whereClause.clientId = Number(clientIDStr);
    }

    if (search) {
      whereClause.OR = [
        { number: { contains: search, mode: 'insensitive' } },
        { client: { clientName: { contains: search, mode: 'insensitive' } } }
      ];
    }

    const totalCount = await prisma.bmpInvoice.count({ where: whereClause });
    const totalPages = Math.max(1, Math.ceil(totalCount / limit));

    const invoices = await prisma.bmpInvoice.findMany({
      where: whereClause,
      include: {
        client: true,
        products: true,
        payments: true
      },
      orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
      take: limit,
      skip: offset
    });

    const result = invoices.map((inv) => {
      const total = inv.products.reduce((sum, p) => sum + p.quantity * p.jumlahLusin * p.price, 0);
      const paidAmount = inv.payments.reduce((sum, p) => sum + p.paymentAmount, 0);
      return {
        ...inv,
        Total: total,
        PaidAmount: paidAmount
      };
    });

    res.json({
      success: true,
      data: result,
      current_page: page,
      total_pages: totalPages,
      total_count: totalCount
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getInvoice = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const invoice = await prisma.bmpInvoice.findUnique({
      where: { id: Number(id) },
      include: {
        client: true,
        products: { include: { masterItem: true } },
        payments: true
      }
    });

    if (!invoice) {
      return res.status(404).json({ success: false, message: 'Invoice not found' });
    }

    res.json({
      success: true,
      data: invoice,
      products: invoice.products,
      payments: invoice.payments
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const createInvoice = async (req: Request, res: Response) => {
  try {
    const { client_id, number, title, due_date, date_created, payment_terms, notes, products } = req.body;

    const client = await prisma.bmpClient.findUnique({
      where: { id: Number(client_id) }
    });
    if (!client) {
      return res.status(400).json({ success: false, message: 'Client not found' });
    }

    const existing = await prisma.bmpInvoice.findFirst({
      where: { number }
    });
    if (existing) {
      return res.status(400).json({
        success: false,
        message: `Gagal! Nomor faktur ${number} sudah ada. Silakan Refresh halaman atau ubah manual nomor fakturnya.`
      });
    }

    const uniqueID = crypto.randomBytes(4).toString('hex');
    const slug = `${number}-${uniqueID}`;

    const parsedDueDate = due_date ? new Date(due_date) : null;
    const createdAt = date_created ? new Date(date_created) : new Date();

    const invoice = await prisma.$transaction(async (tx) => {
      const inv = await tx.bmpInvoice.create({
        data: {
          clientId: Number(client_id),
          number,
          title,
          dueDate: parsedDueDate,
          paymentTerms: payment_terms,
          notes,
          status: 'UNPAID',
          slug,
          uniqueID,
          createdAt
        }
      });

      for (const p of products) {
        const master = await tx.bmpMasterProduct.findUnique({
          where: { id: Number(p.master_item_id) }
        });
        if (!master) continue;

        const price = p.custom_price > 0 ? p.custom_price : master.price;
        const lusin = p.jumlah_lusin > 0 ? p.jumlah_lusin : 1;
        const hargaBeli = p.is_khusus ? p.harga_beli : 0;

        await tx.bmpProduct.create({
          data: {
            invoiceId: inv.id,
            masterItemID: master.id,
            title: master.title,
            unit: master.unit,
            price,
            jumlahLusin: lusin,
            quantity: p.quantity,
            isKhusus: p.is_khusus,
            hargaBeli,
            slug: `${slug}-${crypto.randomBytes(4).toString('hex')}`,
            uniqueID: crypto.randomBytes(4).toString('hex')
          }
        });

        if (p.is_khusus && hargaBeli > 0) {
          await tx.bmpCashFlow.create({
            data: {
              transactionDate: new Date(),
              transactionType: 'KELUAR',
              description: `Pembelian barang khusus untuk Faktur ${number}`,
              amount: hargaBeli
            }
          });
        }
      }

      return inv;
    });

    res.status(201).json({
      success: true,
      message: 'Invoice successfully created',
      data: invoice
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const updateInvoiceHeader = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { title, due_date, payment_terms, notes } = req.body;

    const invoice = await prisma.bmpInvoice.update({
      where: { id: Number(id) },
      data: {
        title,
        dueDate: due_date ? new Date(due_date) : null,
        paymentTerms: payment_terms,
        notes
      }
    });

    res.json({ success: true, data: invoice });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const updateInvoiceProducts = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { products } = req.body; // Array of product inputs

    const invoice = await prisma.bmpInvoice.findUnique({
      where: { id: Number(id) },
      include: { products: true }
    });
    if (!invoice) {
      return res.status(404).json({ success: false, message: 'Invoice not found' });
    }

    await prisma.$transaction(async (tx) => {
      // Hapus produk lama
      await tx.bmpProduct.deleteMany({
        where: { invoiceId: invoice.id }
      });

      for (const p of products) {
        const master = await tx.bmpMasterProduct.findUnique({
          where: { id: Number(p.master_item_id) }
        });
        if (!master) continue;

        const price = p.custom_price > 0 ? p.custom_price : master.price;
        const lusin = p.jumlah_lusin > 0 ? p.jumlah_lusin : 1;
        const hargaBeli = p.is_khusus ? p.harga_beli : 0;

        await tx.bmpProduct.create({
          data: {
            invoiceId: invoice.id,
            masterItemID: master.id,
            title: master.title,
            unit: master.unit,
            price,
            jumlahLusin: lusin,
            quantity: p.quantity,
            isKhusus: p.is_khusus,
            hargaBeli,
            slug: `${invoice.slug}-${crypto.randomBytes(4).toString('hex')}`,
            uniqueID: crypto.randomBytes(4).toString('hex')
          }
        });
      }
    });

    res.json({ success: true, message: 'Invoice products updated successfully' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const deleteInvoice = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    await prisma.bmpInvoice.delete({
      where: { id: Number(id) }
    });
    res.json({ success: true, message: 'Invoice successfully deleted' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const syncOverdueInvoices = async (req: Request, res: Response) => {
  try {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const invoices = await prisma.bmpInvoice.findMany({
      where: {
        status: { in: ['UNPAID', 'PARTIAL'] },
        dueDate: { lt: today }
      }
    });

    for (const inv of invoices) {
      await prisma.bmpInvoice.update({
        where: { id: inv.id },
        data: { status: 'OVERDUE' }
      });
    }

    res.json({ success: true, message: `Synced ${invoices.length} invoices to OVERDUE status` });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── INVOICE PAYMENTS ────────────────────────────────────────────────────────

export const paySingleInvoice = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { nominal, metode, tanggal } = req.body;

    const invoice = await prisma.bmpInvoice.findUnique({
      where: { id: Number(id) },
      include: { client: true, products: true, payments: true }
    });
    if (!invoice) {
      return res.status(404).json({ success: false, message: 'Invoice not found' });
    }

    const payDate = tanggal ? new Date(tanggal) : new Date();
    const payAmount = Number(nominal);

    await prisma.$transaction(async (tx) => {
      const payment = await tx.bmpInvoicePayment.create({
        data: {
          invoiceId: invoice.id,
          paymentDate: payDate,
          paymentAmount: payAmount,
          paymentMethod: metode || 'TRANSFER'
        }
      });

      await tx.bmpCashFlow.create({
        data: {
          transactionDate: payDate,
          transactionType: 'MASUK',
          description: `Pembayaran cicilan Faktur ${invoice.number} (${invoice.client?.clientName || '-'})`,
          amount: payAmount,
          paymentRefId: payment.id
        }
      });

      // Recalculate status
      const totalInv = invoice.products.reduce((sum, p) => sum + p.quantity * p.jumlahLusin * p.price, 0);
      const totalPaid = invoice.payments.reduce((sum, p) => sum + p.paymentAmount, 0) + payAmount;

      let newStatus = 'PARTIAL';
      if (totalPaid >= totalInv) {
        newStatus = 'PAID';
      }

      await tx.bmpInvoice.update({
        where: { id: invoice.id },
        data: { status: newStatus }
      });
    });

    res.json({ success: true, message: 'Payment recorded successfully' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const payMassal = async (req: Request, res: Response) => {
  try {
    const { client_id, nominal, metode } = req.body;

    const client = await prisma.bmpClient.findUnique({
      where: { id: Number(client_id) }
    });
    if (!client) {
      return res.status(400).json({ success: false, message: 'Client not found' });
    }

    const invoices = await prisma.bmpInvoice.findMany({
      where: { clientId: client.id, status: { in: ['UNPAID', 'PARTIAL', 'OVERDUE'] } },
      include: { products: true, payments: true },
      orderBy: { createdAt: 'asc' }
    });

    let sisaUang = Number(nominal);
    const payDate = new Date();
    const payMethod = metode || 'TRANSFER';

    await prisma.$transaction(async (tx) => {
      // 1. Catat ke CashFlow sebagai satu transaksi borongan masuk
      const cf = await tx.bmpCashFlow.create({
        data: {
          transactionDate: payDate,
          transactionType: 'MASUK',
          description: `Pembayaran Borongan dari Klien ${client.clientName}`,
          amount: nominal
        }
      });

      for (const inv of invoices) {
        if (sisaUang <= 0) break;

        const totalInv = inv.products.reduce((sum, p) => sum + p.quantity * p.jumlahLusin * p.price, 0);
        const totalPaid = inv.payments.reduce((sum, p) => sum + p.paymentAmount, 0);
        const sisaTagihan = totalInv - totalPaid;

        if (sisaTagihan <= 0) continue;

        let alokasi = sisaTagihan;
        let nextStatus = 'PAID';

        if (sisaUang < sisaTagihan) {
          alokasi = sisaUang;
          nextStatus = 'PARTIAL';
        }

        await tx.bmpInvoicePayment.create({
          data: {
            invoiceId: inv.id,
            paymentDate: payDate,
            paymentAmount: alokasi,
            paymentMethod: payMethod
          }
        });

        await tx.bmpInvoice.update({
          where: { id: inv.id },
          data: { status: nextStatus }
        });

        sisaUang -= alokasi;
      }

      // Jika ada sisa uang lebih, tambahkan ke saldo titipan klien
      if (sisaUang > 0) {
        await tx.bmpClient.update({
          where: { id: client.id },
          data: { saldoTitipan: { increment: sisaUang } }
        });
      }
    });

    res.json({ success: true, message: 'Pembayaran borongan berhasil diproses' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const editPayment = async (req: Request, res: Response) => {
  try {
    const { paymentId } = req.params;
    const { nominal, tanggal, metode } = req.body;

    const payment = await prisma.bmpInvoicePayment.findUnique({
      where: { id: Number(paymentId) },
      include: { invoice: { include: { products: true, payments: true } } }
    });
    if (!payment) {
      return res.status(404).json({ success: false, message: 'Payment not found' });
    }

    const nextAmount = Number(nominal);
    const prevAmount = payment.paymentAmount;
    const diff = nextAmount - prevAmount;

    await prisma.$transaction(async (tx) => {
      // Update Payment Record
      await tx.bmpInvoicePayment.update({
        where: { id: payment.id },
        data: {
          paymentAmount: nextAmount,
          paymentDate: tanggal ? new Date(tanggal) : payment.paymentDate,
          paymentMethod: metode || payment.paymentMethod
        }
      });

      // Update associated CashFlow
      await tx.bmpCashFlow.updateMany({
        where: { paymentRefId: payment.id },
        data: {
          amount: nextAmount,
          transactionDate: tanggal ? new Date(tanggal) : payment.paymentDate
        }
      });

      // Recompute invoice status
      const totalInv = payment.invoice.products.reduce((sum, p) => sum + p.quantity * p.jumlahLusin * p.price, 0);
      const totalPaid = payment.invoice.payments.reduce((sum, p) => sum + p.paymentAmount, 0) + diff;

      let newStatus = 'PARTIAL';
      if (totalPaid >= totalInv) {
        newStatus = 'PAID';
      } else if (totalPaid <= 0) {
        newStatus = 'UNPAID';
      }

      await tx.bmpInvoice.update({
        where: { id: payment.invoiceId },
        data: { status: newStatus }
      });
    });

    res.json({ success: true, message: 'Pembayaran berhasil diubah' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const deletePayment = async (req: Request, res: Response) => {
  try {
    const { paymentId } = req.params;

    const payment = await prisma.bmpInvoicePayment.findUnique({
      where: { id: Number(paymentId) },
      include: { invoice: { include: { products: true, payments: true } } }
    });
    if (!payment) {
      return res.status(404).json({ success: false, message: 'Payment not found' });
    }

    await prisma.$transaction(async (tx) => {
      // Delete cashflow references
      await tx.bmpCashFlow.deleteMany({
        where: { paymentRefId: payment.id }
      });

      // Delete payment
      await tx.bmpInvoicePayment.delete({
        where: { id: payment.id }
      });

      // Recompute invoice status
      const totalInv = payment.invoice.products.reduce((sum, p) => sum + p.quantity * p.jumlahLusin * p.price, 0);
      const totalPaid = payment.invoice.payments
        .filter((p) => p.id !== payment.id)
        .reduce((sum, p) => sum + p.paymentAmount, 0);

      let newStatus = 'UNPAID';
      if (totalPaid >= totalInv) {
        newStatus = 'PAID';
      } else if (totalPaid > 0) {
        newStatus = 'PARTIAL';
      }

      await tx.bmpInvoice.update({
        where: { id: payment.invoiceId },
        data: { status: newStatus }
      });
    });

    res.json({ success: true, message: 'Cicilan pembayaran berhasil dihapus' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── CASH FLOWS ──────────────────────────────────────────────────────────────

export const getCashFlows = async (req: Request, res: Response) => {
  try {
    const flows = await prisma.bmpCashFlow.findMany({
      orderBy: { transactionDate: 'desc' }
    });
    res.json({ success: true, data: flows });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const createCashFlow = async (req: Request, res: Response) => {
  try {
    const { tanggal, type, nominal, keterangan } = req.body;
    const flow = await prisma.bmpCashFlow.create({
      data: {
        transactionDate: new Date(tanggal),
        transactionType: type,
        amount: Number(nominal),
        description: keterangan
      }
    });
    res.json({ success: true, data: flow });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const updateCashFlow = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { tanggal, type, nominal, keterangan } = req.body;
    const flow = await prisma.bmpCashFlow.update({
      where: { id: Number(id) },
      data: {
        transactionDate: new Date(tanggal),
        transactionType: type,
        amount: Number(nominal),
        description: keterangan
      }
    });
    res.json({ success: true, data: flow });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const deleteCashFlow = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    await prisma.bmpCashFlow.delete({
      where: { id: Number(id) }
    });
    res.json({ success: true, message: 'CashFlow successfully deleted' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const syncKas = async (req: Request, res: Response) => {
  try {
    // Sinkronisasi/Audit kas
    // Cari semua invoice payment yang tidak tercatat di cashflow
    const payments = await prisma.bmpInvoicePayment.findMany({
      include: { cashFlows: true, invoice: { include: { client: true } } }
    });

    let syncCount = 0;
    for (const p of payments) {
      if (p.cashFlows.length === 0) {
        await prisma.bmpCashFlow.create({
          data: {
            transactionDate: p.paymentDate,
            transactionType: 'MASUK',
            description: `Pembayaran cicilan Faktur ${p.invoice.number} (${p.invoice.client?.clientName || '-'})`,
            amount: p.paymentAmount,
            paymentRefId: p.id
          }
        });
        syncCount++;
      }
    }

    res.json({ success: true, message: `Synced ${syncCount} orphan payments into CashFlow` });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── AUDIT REPORT ────────────────────────────────────────────────────────────

export const getAuditReport = async (req: Request, res: Response) => {
  try {
    const reports = await prisma.bmpCashFlow.findMany({
      orderBy: { transactionDate: 'desc' }
    });
    res.json({ success: true, data: reports });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const cleanupOrphanPayments = async (req: Request, res: Response) => {
  try {
    // We can filter orphans manually or using schema check
    const cashflows = await prisma.bmpCashFlow.findMany({
      where: { NOT: { paymentRefId: null } }
    });

    let deletedCount = 0;
    for (const cf of cashflows) {
      if (cf.paymentRefId) {
        const payExists = await prisma.bmpInvoicePayment.findUnique({
          where: { id: cf.paymentRefId }
        });
        if (!payExists) {
          await prisma.bmpCashFlow.delete({ where: { id: cf.id } });
          deletedCount++;
        }
      }
    }

    res.json({ success: true, message: `Cleaned up ${deletedCount} orphan cashflow records` });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const downloadCashFlowCSV = async (req: Request, res: Response) => {
  try {
    const flows = await prisma.bmpCashFlow.findMany({
      orderBy: { transactionDate: 'asc' }
    });

    let csv = 'Tanggal,Tipe,Keterangan,Nominal\n';
    for (const f of flows) {
      const formattedDate = new Date(f.transactionDate).toISOString().split('T')[0];
      const desc = (f.description || '').replace(/"/g, '""');
      csv += `${formattedDate},${f.transactionType},"${desc}",${f.amount}\n`;
    }

    res.setHeader('Content-Type', 'text/csv');
    res.setHeader('Content-Disposition', 'attachment; filename=laporan-kas-manufaktur.csv');
    res.send(csv);
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── BAHAN NONO ──────────────────────────────────────────────────────────────

export const getBahanNono = async (req: Request, res: Response) => {
  try {
    const nonos = await prisma.bmpBahanNono.findMany({
      include: { items: true },
      orderBy: { tanggal: 'desc' }
    });
    res.json({ success: true, data: nonos });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const createBahanNono = async (req: Request, res: Response) => {
  try {
    const { tanggal, nominal, notes, tagihan, total_harga, items } = req.body;

    const nono = await prisma.$transaction(async (tx) => {
      const bn = await tx.bmpBahanNono.create({
        data: {
          tanggal: new Date(tanggal),
          nominal: Number(nominal),
          notes,
          tagihan,
          totalHarga: Number(total_harga)
        }
      });

      for (const item of items) {
        await tx.bmpBahanNonoItem.create({
          data: {
            bahanNonoId: bn.id,
            jenisBahan: item.jenis_bahan,
            kuantitas: Number(item.kuantitas),
            unit: item.unit || 'Kg',
            rate: Number(item.rate)
          }
        });
      }

      // Record Kas Keluar otomatis
      if (nominal > 0) {
        await tx.bmpCashFlow.create({
          data: {
            transactionDate: new Date(tanggal),
            transactionType: 'KELUAR',
            description: `Pembayaran Bahan Nono - Tagihan: ${tagihan}`,
            amount: Number(nominal)
          }
        });
      }

      return bn;
    });

    res.status(201).json({ success: true, data: nono });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const updateBahanNono = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { tanggal, nominal, notes, tagihan, total_harga, items } = req.body;

    await prisma.$transaction(async (tx) => {
      // Dapatkan record lama untuk hitung selisih nominal pembayaran kas keluar
      const oldBn = await tx.bmpBahanNono.findUnique({
        where: { id: Number(id) }
      });
      const diffNominal = Number(nominal) - (oldBn?.nominal || 0);

      await tx.bmpBahanNono.update({
        where: { id: Number(id) },
        data: {
          tanggal: new Date(tanggal),
          nominal: Number(nominal),
          notes,
          tagihan,
          totalHarga: Number(total_harga)
        }
      });

      // Update items
      await tx.bmpBahanNonoItem.deleteMany({
        where: { bahanNonoId: Number(id) }
      });

      for (const item of items) {
        await tx.bmpBahanNonoItem.create({
          data: {
            bahanNonoId: Number(id),
            jenisBahan: item.jenis_bahan,
            kuantitas: Number(item.kuantitas),
            unit: item.unit || 'Kg',
            rate: Number(item.rate)
          }
        });
      }

      // Update Kas Keluar nominal
      if (diffNominal !== 0) {
        await tx.bmpCashFlow.create({
          data: {
            transactionDate: new Date(),
            transactionType: 'KELUAR',
            description: `Penyesuaian Pembayaran Bahan Nono - Tagihan: ${tagihan}`,
            amount: diffNominal
          }
        });
      }
    });

    res.json({ success: true, message: 'Bahan Nono updated successfully' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const deleteBahanNono = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    await prisma.bmpBahanNono.delete({
      where: { id: Number(id) }
    });
    res.json({ success: true, message: 'Record Bahan Nono successfully deleted' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── EMPLOYEES & PAYROLL ─────────────────────────────────────────────────────

export const getEmployees = async (req: Request, res: Response) => {
  try {
    const emps = await prisma.bmpEmployee.findMany({
      orderBy: { name: 'asc' }
    });
    res.json({ success: true, data: emps });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const createEmployee = async (req: Request, res: Response) => {
  try {
    const { name, position, salary_amount, fingerprint_pin, is_active } = req.body;
    const emp = await prisma.bmpEmployee.create({
      data: {
        name,
        position,
        salaryAmount: Number(salary_amount),
        fingerprintPIN: fingerprint_pin || null,
        isActive: is_active ?? true
      }
    });
    res.status(201).json({ success: true, data: emp });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const updateEmployee = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { name, position, salary_amount, fingerprint_pin, is_active } = req.body;
    const emp = await prisma.bmpEmployee.update({
      where: { id: Number(id) },
      data: {
        name,
        position,
        salaryAmount: Number(salary_amount),
        fingerprintPIN: fingerprint_pin || null,
        isActive: is_active ?? true
      }
    });
    res.json({ success: true, data: emp });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const deleteEmployee = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    await prisma.bmpEmployee.delete({
      where: { id: Number(id) }
    });
    res.json({ success: true, message: 'Employee successfully deleted' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const recordPayroll = async (req: Request, res: Response) => {
  try {
    const { employee_id, amount, attendance_count, daily_rate, description, payment_date } = req.body;

    const emp = await prisma.bmpEmployee.findUnique({
      where: { id: Number(employee_id) }
    });
    if (!emp) {
      return res.status(404).json({ success: false, message: 'Employee not found' });
    }

    const pay = await prisma.$transaction(async (tx) => {
      const payroll = await tx.bmpPayroll.create({
        data: {
          employeeId: emp.id,
          amount: Number(amount),
          attendanceCount: Number(attendance_count),
          dailyRate: Number(daily_rate),
          description,
          paymentDate: payment_date ? new Date(payment_date) : new Date()
        }
      });

      // Record Kas Keluar otomatis untuk Gaji Karyawan Pabrik
      await tx.bmpCashFlow.create({
        data: {
          transactionDate: payroll.paymentDate,
          transactionType: 'KELUAR',
          description: `Pembayaran gaji karyawan ${emp.name} (${description || ''})`,
          amount: Number(amount)
        }
      });

      return payroll;
    });

    res.status(201).json({ success: true, data: pay });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getPayrollHistory = async (req: Request, res: Response) => {
  try {
    const history = await prisma.bmpPayroll.findMany({
      include: { employee: true },
      orderBy: { paymentDate: 'desc' }
    });
    res.json({ success: true, data: history });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const deletePayrollHistory = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    await prisma.bmpPayroll.delete({
      where: { id: Number(id) }
    });
    res.json({ success: true, message: 'Payroll record successfully deleted' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── ATTENDANCE LOGS (ABSENSI) ───────────────────────────────────────────────

export const getAttendanceLogs = async (req: Request, res: Response) => {
  try {
    const logs = await prisma.bmpAttendanceLog.findMany({
      orderBy: { logTime: 'desc' }
    });
    res.json({ success: true, data: logs });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const createAttendanceLog = async (req: Request, res: Response) => {
  try {
    const { pin, status, time, check_out_time, alasan } = req.body;
    const log = await prisma.bmpAttendanceLog.create({
      data: {
        employeePIN: pin,
        verifyState: status === 'pulang' ? 1 : 0,
        verifyType: 1, // Fingerprint
        logTime: new Date(time),
        checkOutTime: check_out_time ? new Date(check_out_time) : null,
        workDate: new Date(time),
        alasan
      }
    });
    res.json({ success: true, data: log });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const updateAttendanceLog = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { pin, status, time, check_out_time, alasan } = req.body;
    const log = await prisma.bmpAttendanceLog.update({
      where: { id: Number(id) },
      data: {
        employeePIN: pin,
        verifyState: status === 'pulang' ? 1 : 0,
        logTime: new Date(time),
        checkOutTime: check_out_time ? new Date(check_out_time) : null,
        alasan
      }
    });
    res.json({ success: true, data: log });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const deleteAttendanceLog = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    await prisma.bmpAttendanceLog.delete({
      where: { id: Number(id) }
    });
    res.json({ success: true, message: 'Attendance log successfully deleted' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const saveAttendanceReason = async (req: Request, res: Response) => {
  try {
    const { id, alasan } = req.body;
    const log = await prisma.bmpAttendanceLog.update({
      where: { id: Number(id) },
      data: { alasan }
    });
    res.json({ success: true, data: log });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── BONUS CLAIMS ────────────────────────────────────────────────────────────

export const getBonusLogs = async (req: Request, res: Response) => {
  try {
    const logs = await prisma.bmpMachineBonusLog.findMany({
      include: { employee: true },
      orderBy: { date: 'desc' }
    });
    res.json({ success: true, data: logs });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getBonusByEmployee = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const logs = await prisma.bmpMachineBonusLog.findMany({
      where: { employeeId: Number(id) },
      orderBy: { date: 'desc' }
    });
    res.json({ success: true, data: logs });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const verifyBonusPIN = async (req: Request, res: Response) => {
  try {
    const { pin } = req.body;
    const employee = await prisma.bmpEmployee.findFirst({
      where: { fingerprintPIN: pin, isActive: true }
    });

    if (!employee) {
      return res.status(404).json({ success: false, message: 'PIN karyawan tidak ditemukan atau dinonaktifkan' });
    }

    res.json({
      success: true,
      data: {
        id: employee.id,
        name: employee.name,
        position: employee.position
      }
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const claimBonus = async (req: Request, res: Response) => {
  try {
    const { employee_id, machine, shift, count, bonus_amount } = req.body;

    const employee = await prisma.bmpEmployee.findUnique({
      where: { id: Number(employee_id) }
    });
    if (!employee) {
      return res.status(404).json({ success: false, message: 'Employee not found' });
    }

    // Hindari double claim untuk karyawan + mesin + shift + tanggal yang sama hari ini
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const existing = await prisma.bmpMachineBonusLog.findFirst({
      where: {
        employeeId: employee.id,
        machineName: machine,
        shiftType: shift,
        date: { gte: today }
      }
    });

    if (existing) {
      return res.status(400).json({
        success: false,
        message: `Karyawan ${employee.name} sudah mengklaim bonus untuk mesin ${machine} di shift ${shift} hari ini.`
      });
    }

    const log = await prisma.bmpMachineBonusLog.create({
      data: {
        employeeId: employee.id,
        machineName: machine,
        shiftType: shift,
        jumlahPerolehan: Number(count),
        bonusAmount: Number(bonus_amount),
        date: new Date()
      }
    });

    res.status(201).json({
      success: true,
      message: `Bonus sebesar Rp ${Number(bonus_amount).toLocaleString('id-ID')} berhasil dicatat untuk ${employee.name}.`,
      data: log
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getEmployeePINList = async (req: Request, res: Response) => {
  try {
    const employees = await prisma.bmpEmployee.findMany({
      where: { isActive: true, NOT: { fingerprintPIN: null } },
      select: { id: true, name: true, fingerprintPIN: true }
    });
    res.json({ success: true, data: employees });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── PDF GENERATION ──────────────────────────────────────────────────────────

export const generateInvoicePDF = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const invoice = await prisma.bmpInvoice.findUnique({
      where: { id: Number(id) },
      include: { client: true, payments: true }
    });

    if (!invoice) {
      return res.status(404).json({ success: false, message: 'Invoice not found' });
    }

    const products = await prisma.bmpProduct.findMany({
      where: { invoiceId: invoice.id }
    });

    const settings = await prisma.bmpSettings.findFirst() || { clientName: 'CV Bahtera Mulya Plastik', clientLogo: '', addressLine1: '', province: '', postalCode: '', phoneNumber: '', emailAddress: '' };

    const subtotal = products.reduce((sum, p) => sum + p.quantity * p.jumlahLusin * p.price, 0);
    const totalPaid = invoice.payments.reduce((sum, p) => sum + p.paymentAmount, 0);

    // Render HTML template using EJS
    const templatePath = path.join(__dirname, '../templates/invoice-pdf.ejs');
    const html = await ejs.renderFile(templatePath, {
      Invoice: invoice,
      Products: products,
      Settings: settings,
      Subtotal: subtotal,
      SisaTagihan: subtotal - totalPaid,
      DemoMode: false,
      LogoBase64: '', // Empty as requested to omit logo
      TTDBase64: '', // Handled inside EJS signature fallback or read local
      formatDate,
      formatRpComma
    });

    const pdfBuffer = await generatePdf(html);
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `inline; filename="invoice-${invoice.number}.pdf"`);
    res.send(pdfBuffer);
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const generateSuratJalanPDF = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const invoice = await prisma.bmpInvoice.findUnique({
      where: { id: Number(id) },
      include: { client: true }
    });

    if (!invoice) {
      return res.status(404).json({ success: false, message: 'Invoice not found' });
    }

    const products = await prisma.bmpProduct.findMany({
      where: { invoiceId: invoice.id }
    });

    const settings = await prisma.bmpSettings.findFirst() || { clientName: 'CV Bahtera Mulya Plastik', clientLogo: '', addressLine1: '', province: '', postalCode: '', phoneNumber: '', emailAddress: '' };

    const subtotal = products.reduce((sum, p) => sum + p.quantity * p.jumlahLusin * p.price, 0);

    const templatePath = path.join(__dirname, '../templates/surat-jalan-pdf.ejs');
    const html = await ejs.renderFile(templatePath, {
      Invoice: invoice,
      Products: products,
      Settings: settings,
      Subtotal: subtotal,
      DemoMode: false,
      LogoBase64: '',
      TTDBase64: '',
      formatDate,
      formatRpComma
    });

    const pdfBuffer = await generatePdf(html);
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `inline; filename="SJ-${invoice.number}.pdf"`);
    res.send(pdfBuffer);
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const generatePricelistProductPDF = async (req: Request, res: Response) => {
  try {
    const products = await prisma.bmpMasterProduct.findMany({
      orderBy: { title: 'asc' }
    });

    const settings = await prisma.bmpSettings.findFirst() || { clientName: 'CV Bahtera Mulya Plastik' };

    const templatePath = path.join(__dirname, '../templates/pricelist-pdf.ejs');
    const html = await ejs.renderFile(templatePath, {
      Products: products,
      Settings: settings,
      formatRpComma
    });

    const pdfBuffer = await generatePdf(html);
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', 'inline; filename="pricelist.pdf"');
    res.send(pdfBuffer);
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const generatePayrollPDF = async (req: Request, res: Response) => {
  try {
    // Custom payroll printable rendering
    const idsStr = req.query.ids as string;
    const ids = idsStr ? idsStr.split(',').map((id) => Number(id)) : [];
    
    const payrolls = ids.length > 0
      ? await prisma.bmpPayroll.findMany({
          where: { employeeId: { in: ids } },
          include: { employee: true },
          orderBy: { id: 'desc' },
          take: ids.length
        })
      : await prisma.bmpPayroll.findMany({
          include: { employee: true }
        });

    const now = new Date();
    const dayOfWeek = now.getDay(); // 0 is Sunday, 1 is Monday
    const offsetDays = dayOfWeek === 0 ? -6 : -(dayOfWeek - 1);
    const startOfWeek = new Date(now.setDate(now.getDate() + offsetDays));
    startOfWeek.setHours(0, 0, 0, 0);

    const slipData: any[] = [];
    for (const p of payrolls) {
      const bonusLogs = await prisma.bmpMachineBonusLog.findMany({
        where: { employeeId: p.employeeId, date: { gte: startOfWeek } }
      });

      const totalBonus = bonusLogs.reduce((sum, b) => sum + b.bonusAmount, 0);
      slipData.push({
        Payroll: p,
        BonusLogs: bonusLogs,
        TotalBonus: totalBonus,
        TotalGaji: p.amount + totalBonus
      });
    }

    const templatePath = path.join(__dirname, '../templates/payroll-pdf.ejs');
    const html = await ejs.renderFile(templatePath, {
      Employees: slipData,
      formatRpComma
    });

    const pdfBuffer = await generatePdf(html);
    res.setHeader('Content-Type', 'application/pdf');
    res.send(pdfBuffer);
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

// ─── ZKTECO ADMS FINGERPRINT MACHINE HANDLERS ─────────────────────────────────

function getWibHourAndDate(logTime: Date) {
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Jakarta',
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    second: 'numeric',
    hour12: false
  });
  
  const parts = formatter.formatToParts(logTime);
  const getVal = (type: string) => Number(parts.find(p => p.type === type)?.value);
  
  const year = getVal('year');
  const month = getVal('month') - 1; // 0-indexed in JS
  const day = getVal('day');
  let hour = getVal('hour');
  if (hour === 24) hour = 0;
  
  const workDate = new Date(Date.UTC(year, month, day, 0, 0, 0, 0));
  
  if (hour >= 0 && hour < 4) {
    workDate.setUTCDate(workDate.getUTCDate() - 1);
  }
  
  return { hour, workDate };
}

function isCheckOutWindow(logTime: Date): boolean {
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Jakarta',
    hour: 'numeric',
    hour12: false
  });
  let hour = Number(formatter.format(logTime));
  if (hour === 24) hour = 0;
  
  if (hour >= 6 && hour < 8) return true;
  if (hour >= 14 && hour < 16) return true;
  if (hour >= 22 || hour === 0) return true;
  return false;
}

function hitungKeterlambatan(logTime: Date): number {
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Jakarta',
    hour: 'numeric',
    minute: 'numeric',
    hour12: false
  });
  const parts = formatter.formatToParts(logTime);
  const getVal = (type: string) => Number(parts.find(p => p.type === type)?.value);
  
  let hour = getVal('hour');
  if (hour === 24) hour = 0;
  const minute = getVal('minute');
  
  const totalMinutes = hour * 60 + minute;
  
  // Shift Pagi: 07:00. Window: 06:01 (361) to 07:30 (450)
  if (totalMinutes >= 361 && totalMinutes <= 450) {
    const diff = totalMinutes - 420; // 07:00 is 420 mins
    return diff < 0 ? 0 : diff;
  }
  
  // Shift Sore: 15:00. Window: 14:01 (841) to 15:30 (930)
  if (totalMinutes >= 841 && totalMinutes <= 930) {
    const diff = totalMinutes - 900; // 15:00 is 900 mins
    return diff < 0 ? 0 : diff;
  }
  
  // Shift Malam: 23:00. Window: 22:01 (1321) to 23:30 (1410)
  if (totalMinutes >= 1321 && totalMinutes <= 1410) {
    const diff = totalMinutes - 1380; // 23:00 is 1380 mins
    return diff < 0 ? 0 : diff;
  }
  
  return 0;
}

export const handleCData = async (req: Request, res: Response) => {
  const sn = req.query.SN as string;
  const table = req.query.table as string;
  const options = req.query.options as string;

  console.log(`[ADMS] Received ${req.method} request on /iclock/cdata?SN=${sn}&table=${table}&options=${options}`);

  if (!sn) {
    res.setHeader('Content-Type', 'text/plain');
    return res.send('ERROR: No SN');
  }

  // 1. Initialize device or update lastActivity (GET or options=all)
  if (req.method === 'GET' || options === 'all') {
    try {
      const device = await prisma.bmpAdmsDevice.findUnique({
        where: { serialNumber: sn }
      });

      if (!device) {
        await prisma.bmpAdmsDevice.create({
          data: {
            serialNumber: sn,
            lastActivity: new Date()
          }
        });
      } else {
        await prisma.bmpAdmsDevice.update({
          where: { id: device.id },
          data: { lastActivity: new Date() }
        });
      }
    } catch (err) {
      console.error('[ADMS] Error saving/updating device status:', err);
    }

    const response = `GET OPTION FROM:${sn}\nErrorDelay=60\nDelay=30\nTransTimes=00:00;14:05\nTransInterval=1\nTransFlag=TransData AttLog OpLog\nRealtime=1\nEncrypt=0`;
    res.setHeader('Content-Type', 'text/plain');
    return res.send(response);
  }

  // 2. Receive attendance logs (POST & table=ATTLOG)
  if (req.method === 'POST' && table === 'ATTLOG') {
    const rawBody = req.body || '';
    const bodyStr = typeof rawBody === 'string' ? rawBody : (rawBody.toString ? rawBody.toString() : '');
    
    const lines = bodyStr.split('\n');
    
    for (let line of lines) {
      line = line.trim();
      if (!line) continue;

      const parts = line.split('\t');
      if (parts.length < 2) {
        console.warn(`[ADMS] Invalid line ignored: ${line}`);
        continue;
      }

      const pin = parts[0].trim();
      const timeStr = parts[1].trim();

      let verifyType = 0;
      if (parts.length >= 4) {
        verifyType = parseInt(parts[3].trim()) || 0;
      }

      const logTime = new Date();
      
      console.log(`[ADMS] Scan received for PIN=${pin} | Machine Time: ${timeStr} | Override to Server Time: ${logTime.toISOString()}`);

      try {
        const lastLog = await prisma.bmpAttendanceLog.findFirst({
          where: { employeePIN: pin },
          orderBy: { logTime: 'desc' }
        });

        let isCheckIn = true;
        let matchedLog: any = null;

        if (lastLog) {
          if (!lastLog.checkOutTime) {
            const durationMs = logTime.getTime() - lastLog.logTime.getTime();
            const durationMin = durationMs / (1000 * 60);
            const durationHr = durationMin / 60;

            if (durationMin < 2) {
              console.log(`[ADMS] Ignore double scan under 2 mins: PIN=${pin}`);
              continue;
            }

            if (durationHr <= 12) {
              isCheckIn = false;
              matchedLog = lastLog;
            } else {
              isCheckIn = true;
            }
          } else {
            const durationMs = logTime.getTime() - lastLog.checkOutTime.getTime();
            const durationMin = durationMs / (1000 * 60);

            if (durationMin < 2) {
              console.log(`[ADMS] Ignore scan right after checkout: PIN=${pin}`);
              continue;
            }
            isCheckIn = true;
          }
        } else {
          isCheckIn = true;
        }

        if (isCheckIn) {
          const { workDate } = getWibHourAndDate(logTime);
          const lateMinutes = hitungKeterlambatan(logTime);
          let alasan = '';
          if (isCheckOutWindow(logTime)) {
            alasan = 'Hanya Scan Pulang / Lupa Scan Masuk';
          }

          const attLog = await prisma.bmpAttendanceLog.create({
            data: {
              deviceSN: sn,
              employeePIN: pin,
              verifyType,
              verifyState: 0, // Check-In
              logTime,
              workDate,
              lateMinutes,
              alasan
            }
          });
          console.log(`[ADMS] Saved Check-In: ID=${attLog.id} | PIN=${pin} | WorkDate=${workDate.toISOString().split('T')[0]}`);
        } else {
          let alasan = matchedLog.alasan;
          if (alasan === 'Hanya Scan Pulang / Lupa Scan Masuk') {
            alasan = '';
          }

          const updatedLog = await prisma.bmpAttendanceLog.update({
            where: { id: matchedLog.id },
            data: {
              checkOutTime: logTime,
              verifyState: 1, // Check-Out
              alasan
            }
          });
          console.log(`[ADMS] Saved Check-Out: ID=${updatedLog.id} | PIN=${pin}`);
        }
      } catch (err) {
        console.error(`[ADMS] Error saving log for PIN=${pin}:`, err);
      }
    }

    res.setHeader('Content-Type', 'text/plain');
    return res.send('OK');
  }

  if (req.method === 'POST') {
    res.setHeader('Content-Type', 'text/plain');
    return res.send('OK');
  }

  return res.sendStatus(404);
};

const syncedDevices = new Map<string, number>();

export const handleGetRequest = async (req: Request, res: Response) => {
  const sn = req.query.SN as string;
  console.log(`[ADMS] Received ${req.method} request on /iclock/getrequest?SN=${sn}`);

  if (sn) {
    const now = Date.now();
    const lastSync = syncedDevices.get(sn) || 0;

    // Time sync max once per hour
    if (now - lastSync > 60 * 60 * 1000) {
      syncedDevices.set(sn, now);

      const formatter = new Intl.DateTimeFormat('en-US', {
        timeZone: 'Asia/Jakarta',
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
        hour12: false
      });
      const parts = formatter.formatToParts(new Date());
      const getVal = (type: string) => String(parts.find(p => p.type === type)?.value).padStart(2, '0');
      
      const year = getVal('year');
      const month = getVal('month');
      const day = getVal('day');
      let hourVal = Number(getVal('hour'));
      if (hourVal === 24) hourVal = 0;
      const hourStr = String(hourVal).padStart(2, '0');
      const minute = getVal('minute');
      const second = getVal('second');
      const dateStr = `${year}-${month}-${day} ${hourStr}:${minute}:${second}`;

      const cmdID = Math.floor(now / 1000) % 10000;
      const command = `C:${cmdID}:SET OPTIONS DateTime=${dateStr}\n`;

      console.log(`[ADMS] Time Sync command for machine ${sn}: ${command.trim()}`);
      res.setHeader('Content-Type', 'text/plain');
      return res.send(command);
    }
  }

  res.setHeader('Content-Type', 'text/plain');
  return res.send('OK');
};
