import 'dotenv/config';
import express, { Request, Response, NextFunction } from 'express';
import cors from 'cors';
import { PrismaClient } from '@prisma/client';
import { AsyncLocalStorage } from 'async_hooks';
import fs from 'fs';
import path from 'path';
import { exec } from 'child_process';

const mainPrisma = new PrismaClient();
const prismaContext = new AsyncLocalStorage<PrismaClient>();
const tenantClients = new Map<string, PrismaClient>();

function getCleanTenantDbName(tenantId: string): string {
  const cleanId = tenantId.replace(/[^a-zA-Z0-9]/g, '_').toLowerCase().trim();
  return `posbah_tenant_${cleanId}`;
}

const getTenantPrisma = (tenantId: string): PrismaClient => {
  const dbName = getCleanTenantDbName(tenantId);
  if (!tenantClients.has(dbName)) {
    const originalUrl = process.env.DATABASE_URL || '';
    let tenantDbUrl = originalUrl;
    if (originalUrl.includes('?')) {
      const parts = originalUrl.split('?');
      const base = parts[0];
      const query = parts[1];
      const lastSlash = base.lastIndexOf('/');
      tenantDbUrl = base.substring(0, lastSlash + 1) + dbName + '?' + query;
    } else {
      const lastSlash = originalUrl.lastIndexOf('/');
      tenantDbUrl = originalUrl.substring(0, lastSlash + 1) + dbName;
    }

    console.log(`Initializing new PrismaClient for tenant ${tenantId} (${dbName})`);
    const client = new PrismaClient({
      datasources: {
        db: { url: tenantDbUrl }
      }
    });
    tenantClients.set(dbName, client);
  }
  return tenantClients.get(dbName)!;
};

// Global proxy to dynamically swap prisma connection on runtime based on AsyncLocalStorage context
const prisma = new Proxy(mainPrisma, {
  get(target, prop, receiver) {
    const activePrisma = prismaContext.getStore();
    if (activePrisma) {
      return Reflect.get(activePrisma, prop, receiver);
    }
    return Reflect.get(target, prop, receiver);
  }
});

const createTenantDatabase = async (tenantId: string): Promise<string> => {
  const dbName = getCleanTenantDbName(tenantId);
  try {
    const result = await mainPrisma.$queryRawUnsafe<any[]>(
      `SELECT 1 FROM pg_database WHERE datname = $1`,
      dbName
    );
    if (result && result.length > 0) {
      console.log(`Database ${dbName} already exists.`);
      return dbName;
    }
  } catch (err) {
    console.warn(`Query check database failed for ${dbName}, trying CREATE anyway:`, err);
  }

  await mainPrisma.$executeRawUnsafe(`CREATE DATABASE ${dbName}`);
  console.log(`Database ${dbName} created successfully.`);
  return dbName;
};

const runTenantMigration = (dbName: string): Promise<string> => {
  return new Promise((resolve, reject) => {
    const originalUrl = process.env.DATABASE_URL || '';
    let tenantDbUrl = originalUrl;
    if (originalUrl.includes('?')) {
      const parts = originalUrl.split('?');
      const base = parts[0];
      const query = parts[1];
      const lastSlash = base.lastIndexOf('/');
      tenantDbUrl = base.substring(0, lastSlash + 1) + dbName + '?' + query;
    } else {
      const lastSlash = originalUrl.lastIndexOf('/');
      tenantDbUrl = originalUrl.substring(0, lastSlash + 1) + dbName;
    }

    console.log(`Running Prisma migration (db push) for ${dbName}...`);
    const cmd = process.platform === 'win32'
      ? `npx.cmd prisma db push --accept-data-loss`
      : `npx prisma db push --accept-data-loss`;

    exec(cmd, {
      env: {
        ...process.env,
        DATABASE_URL: tenantDbUrl
      }
    }, (error, stdout, stderr) => {
      if (error) {
        console.error(`Prisma migration failed for ${dbName}:`, error);
        console.error(`Stderr:`, stderr);
        reject(error);
      } else {
        console.log(`Prisma migration successful for ${dbName}.`);
        resolve(stdout);
      }
    });
  });
};

const app = express();
const port = process.env.PORT || 3001;

// ─── DAFTAR BLOKIR LANGGANAN (BLOCKED USERS) ────────────────────────
// Tambahkan nama karyawan (case-insensitive) ke dalam array ini untuk memblokir akses
// mereka dari APK secara real-time. Jika diblokir, mereka tidak akan bisa login
// dan tidak bisa mengakses data/fitur apa pun di dalam aplikasi.
const BLOCKED_USERS: string[] = []; 
// Contoh pemblokiran: const BLOCKED_USERS: string[] = ['hanafi', 'fed', 'fahri'];

// ─── DAFTAR BLOKIR EMAIL (PENYUSUP) ─────────────────────────────────
// Tambahkan email penyusup ke sini untuk mencegah registrasi & login selamanya.
// Pengecekan dilakukan di semua endpoint register dan login.
const BLOCKED_EMAILS: string[] = [
  'tester_anak@gmail.com', // PENYUSUP - diblokir 2026-06-02
];

import crypto from 'crypto';
import nodemailer from 'nodemailer';

// Hashing PIN/password using salted PBKDF2
const HASH_SALT = process.env.HASH_SALT || 'posbah_default_salt_secret';

function hashPassword(password: string): string {
  return crypto.pbkdf2Sync(password, HASH_SALT, 1000, 64, 'sha512').toString('hex');
}

function verifyPassword(password: string, hash: string): boolean {
  return hashPassword(password) === hash;
}

// ─────────────────────────────────────────────────────────────
// Email Notification Helper (Gmail SMTP via Nodemailer)
// ─────────────────────────────────────────────────────────────
const createEmailTransporter = () => nodemailer.createTransport({
  host: process.env.SMTP_HOST || 'smtp.gmail.com',
  port: parseInt(process.env.SMTP_PORT || '465'),
  secure: process.env.SMTP_SECURE === 'true',
  auth: {
    user: process.env.SMTP_USER || '',
    pass: process.env.SMTP_PASS || ''
  }
});

const sendEmail = async (to: string, subject: string, html: string): Promise<void> => {
  if (!process.env.SMTP_USER || process.env.SMTP_USER.includes('GANTI')) {
    console.warn('[EMAIL] SMTP not configured. Skipping email to:', to);
    console.warn('[EMAIL] Subject:', subject);
    return;
  }
  try {
    const transporter = createEmailTransporter();
    await transporter.sendMail({
      from: `"POSBah System" <${process.env.SMTP_USER}>`,
      to,
      subject,
      html
    });
    console.log(`[EMAIL] Sent to ${to}: ${subject}`);
  } catch (err) {
    console.error('[EMAIL] Failed to send email:', err);
  }
};

// Generate random password
const generatePassword = (): string => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789';
  return Array.from({ length: 10 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
};


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

    let empId: number;
    if (typeof employeeId === 'string' && employeeId.includes('@')) {
      const emp = await prisma.employee.findFirst({ where: { email: employeeId } });
      if (!emp) return;
      empId = emp.id;
    } else {
      empId = Number(employeeId);
    }
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
  allowedHeaders: ['Content-Type', 'Authorization', 'x-employee-id', 'x-employee-role', 'x-employee-name', 'x-app-mode', 'x-offline-sync', 'x-tenant-id']
}));
app.use(express.json());

// Multi-tenant database routing middleware
app.use((req: Request, res: Response, next: NextFunction) => {
  const tenantId = req.headers['x-tenant-id'] as string;
  
  // Rute global yang tidak menggunakan tenant database (menggunakan main/global database)
  const isGlobalPath = [
    '/',
    '/api/download-apk',
    '/api/auth/register-email',
    '/api/auth/login-email',
    '/api/auth/google-register'
  ].includes(req.path);

  if (isGlobalPath) {
    return prismaContext.run(mainPrisma, () => {
      next();
    });
  }

  if (!tenantId) {
    // Sebagai fallback agar sistem lama atau request tanpa header tetap bisa jalan ke main database
    console.warn(`Warning: Missing x-tenant-id header for path: ${req.path}`);
    return prismaContext.run(mainPrisma, () => {
      next();
    });
  }

  try {
    const tenantPrisma = getTenantPrisma(tenantId);
    prismaContext.run(tenantPrisma, () => {
      next();
    });
  } catch (err) {
    console.error(`Failed to route database for tenant: ${tenantId}`, err);
    res.status(500).json({ error: 'Gagal mengarahkan koneksi database penyewa' });
  }
});

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
  const isPublicRoute = req.path.startsWith('/api/auth/') || req.path === '/api/download-apk' || req.path === '/';

  if (!isPublicRoute && (employeeId === '9999' || employeeId === '0')) {
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

  // ─── CEK BLOKIR EMAIL PENYUSUP ──────────────────────────────────────
  // Blokir semua request dari email yang ada di BLOCKED_EMAILS
  const bodyEmail = req.body?.email as string | undefined;
  if (bodyEmail && BLOCKED_EMAILS.includes(bodyEmail.toLowerCase().trim())) {
    return res.status(403).json({
      error: 'Akses ditolak. Akun Anda telah diblokir oleh Administrator.'
    });
  }

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
      let emp;
      if (employeeId.includes('@')) {
        emp = await prisma.employee.findFirst({ where: { email: employeeId } });
      } else {
        const empId = Number(employeeId);
        if (!isNaN(empId)) {
          emp = await prisma.employee.findUnique({ where: { id: empId } });
        }
      }
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


/** Helper to resolve numeric Employee ID from x-employee-id header (which can be email) */
async function resolveEmployeeId(req: Request): Promise<number> {
  const employeeIdHeader = req.headers['x-employee-id'] as string;
  const employeeRoleHeader = req.headers['x-employee-role'] as string || 'KASIR';
  const rawName = req.headers['x-employee-name'] as string;
  const employeeNameHeader = rawName ? decodeURIComponent(rawName) : 'User';

  if (!employeeIdHeader || employeeIdHeader === '0' || employeeIdHeader === '9999') {
    let employee = await prisma.employee.findFirst({
      where: { name: 'Sistem' }
    });
    if (!employee) {
      employee = await prisma.employee.create({
        data: { name: 'Sistem', role: 'OWNER', pin: '', email: 'sistem@posbah.com' }
      });
    }
    return employee.id;
  }

  if (employeeIdHeader.includes('@')) {
    let employee = await prisma.employee.findFirst({
      where: { email: employeeIdHeader }
    });

    if (!employee) {
      employee = await prisma.employee.create({
        data: {
          name: employeeNameHeader,
          role: employeeRoleHeader,
          pin: '',
          email: employeeIdHeader
        }
      });
    } else {
      if (employee.name !== employeeNameHeader || employee.role !== employeeRoleHeader) {
        employee = await prisma.employee.update({
          where: { id: employee.id },
          data: { name: employeeNameHeader, role: employeeRoleHeader }
        });
      }
    }
    return employee.id;
  }

  const numericId = Number(employeeIdHeader);
  if (!isNaN(numericId)) {
    return numericId;
  }

  // Fallback
  let employee = await prisma.employee.findFirst();
  if (!employee) {
    employee = await prisma.employee.create({
      data: { name: 'Sistem', role: 'OWNER', pin: '', email: 'sistem@posbah.com' }
    });
  }
  return employee.id;
}

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
  next();
};

// ─────────────────────────────────────────────────────────────
// Basic sanity check
// ─────────────────────────────────────────────────────────────
app.get('/', (req, res) => {
  res.send('POSBah API is running');
});

// Memory map to store single-use download tokens
const downloadTokens = new Map<string, { tenantId: string; expiresAt: number }>();

// Route to generate a temporary download token (requires premium owner authentication headers)
app.get('/api/auth/get-apk-download-token', async (req, res) => {
  try {
    const tenantId = req.headers['x-tenant-id'] as string;
    if (!tenantId) {
      return res.status(403).json({ error: 'Akses ditolak. Tenant ID diperlukan.' });
    }

    const cleanEmail = tenantId.toLowerCase().trim();
    const premium = await mainPrisma.premiumUser.findUnique({
      where: { email: cleanEmail }
    });

    if (!premium) {
      return res.status(403).json({ error: 'Akses ditolak. Hanya akun Premium aktif yang dapat mengunduh APK.' });
    }

    // Generate single-use token
    const token = crypto.randomBytes(16).toString('hex');
    downloadTokens.set(token, { tenantId: cleanEmail, expiresAt: Date.now() + 5 * 60 * 1000 }); // 5 minutes validity

    res.json({ token });
  } catch (err) {
    console.error('Error generating download token:', err);
    res.status(500).json({ error: 'Gagal memproses token unduhan.' });
  }
});

// Route to download the latest APK (bypassed token verification for older APK client updates)
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

// Route to get the latest APK version name
app.get('/api/apk-version', (req, res) => {
  res.json({ version: '1.1.2' });
});

// Get employee limit for a tenant
app.get('/api/employees/limit', async (req, res) => {
  try {
    const tenantId = req.headers['x-tenant-id'] as string;
    let limit = 4;
    if (tenantId) {
      const tenantLimit = await mainPrisma.tenantLimit.findUnique({ where: { tenantId } });
      if (tenantLimit) {
        limit = tenantLimit.limit;
      }
    }
    res.json({ limit });
  } catch (error) {
    res.json({ limit: 4 });
  }
});

// Request employee limit expansion
app.post('/api/request-expansion', requireOwner, async (req, res) => {
  try {
    const tenantId = req.headers['x-tenant-id'] as string;
    const { ownerName, ownerEmail } = req.body;
    if (!tenantId || !ownerEmail) {
      return res.status(400).json({ error: 'Tenant ID dan email owner wajib dikirim' });
    }

    const appBaseUrl = process.env.APP_BASE_URL || 'https://www.zedmz.cloud';
    const confirmLink = `${appBaseUrl}/api/confirm-expansion?tenantId=${encodeURIComponent(tenantId)}&email=${encodeURIComponent(ownerEmail)}`;
    const rejectLink = `${appBaseUrl}/api/reject-expansion?tenantId=${encodeURIComponent(tenantId)}&email=${encodeURIComponent(ownerEmail)}`;

    const subject = `Permintaan Ekspansi Karyawan POSBah - ${ownerName}`;
    const html = `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E5E7EB; border-radius: 10px;">
        <h2 style="color: #4F46E5; margin-top: 0;">Permintaan Ekspansi Kapasitas Karyawan</h2>
        <p>Owner dengan data berikut meminta peningkatan batas kapasitas karyawan dari 4 menjadi 7:</p>
        <table style="width: 100%; border-collapse: collapse; margin: 15px 0;">
          <tr>
            <td style="padding: 8px; border-bottom: 1px solid #E5E7EB; font-weight: bold; width: 140px;">Nama Owner:</td>
            <td style="padding: 8px; border-bottom: 1px solid #E5E7EB;">${ownerName}</td>
          </tr>
          <tr>
            <td style="padding: 8px; border-bottom: 1px solid #E5E7EB; font-weight: bold;">Email Owner:</td>
            <td style="padding: 8px; border-bottom: 1px solid #E5E7EB;">${ownerEmail}</td>
          </tr>
          <tr>
            <td style="padding: 8px; border-bottom: 1px solid #E5E7EB; font-weight: bold;">Tenant ID:</td>
            <td style="padding: 8px; border-bottom: 1px solid #E5E7EB; color: #4F46E5; font-family: monospace;">${tenantId}</td>
          </tr>
        </table>
        <p>Silakan klik salah satu tombol di bawah untuk memproses:</p>
        <div style="margin-top: 25px; margin-bottom: 20px;">
          <a href="${confirmLink}" style="background: linear-gradient(135deg, #10B981, #059669); color: white; padding: 12px 24px; text-decoration: none; border-radius: 8px; font-weight: bold; margin-right: 15px; display: inline-block; box-shadow: 0 4px 10px rgba(16,185,129,0.3);">Konfirmasi (Sudah Bayar)</a>
          <a href="${rejectLink}" style="background: linear-gradient(135deg, #EF4444, #DC2626); color: white; padding: 12px 24px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block; box-shadow: 0 4px 10px rgba(239,68,68,0.3);">Belum (Belum Bayar)</a>
        </div>
        <p style="font-size: 11px; color: #6B7280; border-top: 1px solid #E5E7EB; padding-top: 15px; margin-top: 25px;">Sistem Otomatis POSBah</p>
      </div>
    `;

    await sendEmail('muhammadmuizz8@gmail.com', subject, html);
    res.json({ success: true, message: 'Permintaan ekspansi telah dikirim melalui email.' });
  } catch (err: any) {
    console.error('Request expansion failed:', err);
    res.status(500).json({ error: 'Gagal mengirim permintaan ekspansi: ' + err.message });
  }
});

// Confirm limit expansion to 7
app.get('/api/confirm-expansion', async (req, res) => {
  try {
    const tenantId = req.query.tenantId as string;
    const email = req.query.email as string;
    if (!tenantId) {
      return res.status(400).send('Tenant ID tidak valid');
    }

    await mainPrisma.tenantLimit.upsert({
      where: { tenantId },
      update: { limit: 7 },
      create: { tenantId, limit: 7 }
    });

    const subject = 'Konfirmasi Ekspansi Karyawan POSBah Berhasil';
    const html = `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E5E7EB; border-radius: 10px;">
        <h2 style="color: #10B981; margin-top: 0;">✅ Ekspansi Karyawan POSBah Aktif!</h2>
        <p>Halo,</p>
        <p>Pembayaran Anda telah kami konfirmasi. Batas penambahan karyawan untuk toko Anda telah berhasil ditingkatkan menjadi <b>7 karyawan</b>.</p>
        <p>Perubahan ini berlaku otomatis di aplikasi web dan APK Anda. Silakan muat ulang (refresh) halaman atau buka kembali aplikasi POSBah Anda untuk mulai menambahkan karyawan baru.</p>
        <br/>
        <p>Terima kasih,</p>
        <p><b>Tim POSBah</b></p>
      </div>
    `;

    if (email) {
      await sendEmail(email, subject, html);
    }

    res.send(`
      <div style="font-family: Arial, sans-serif; text-align: center; margin-top: 100px;">
        <h2 style="color: #10B981;">✅ Konfirmasi Berhasil</h2>
        <p>Tenant <b>${tenantId}</b> telah berhasil ditingkatkan ke batas <b>7 karyawan</b>.</p>
        <p>Email pemberitahuan telah dikirimkan ke owner di <b>${email}</b>.</p>
      </div>
    `);
  } catch (err: any) {
    console.error('Confirm expansion failed:', err);
    res.status(500).send('Gagal melakukan konfirmasi: ' + err.message);
  }
});

// Reject limit expansion (send payment reminder)
app.get('/api/reject-expansion', async (req, res) => {
  try {
    const tenantId = req.query.tenantId as string;
    const email = req.query.email as string;
    if (!email) {
      return res.status(400).send('Email owner tidak valid');
    }

    const subject = 'Informasi Pembayaran Ekspansi Karyawan POSBah';
    const html = `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E5E7EB; border-radius: 10px;">
        <h2 style="color: #EF4444; margin-top: 0;">Pemberitahuan Pembayaran Ekspansi</h2>
        <p>Halo,</p>
        <p>Mohon melakukan pembayaran sebesar <b>Rp 25.000 (25K)</b> untuk melakukan ekspansi cabang (penambahan kapasitas menjadi 7 karyawan).</p>
        <p>Silakan kirimkan pembayaran Anda ke rekening/QRIS Admin POSBah yang biasa digunakan. Setelah melakukan pembayaran, harap hubungi Admin agar limit penambahan karyawan Anda segera diaktifkan.</p>
        <br/>
        <p>Terima kasih,</p>
        <p><b>Tim POSBah</b></p>
      </div>
    `;

    await sendEmail(email, subject, html);

    res.send(`
      <div style="font-family: Arial, sans-serif; text-align: center; margin-top: 100px;">
        <h2 style="color: #EF4444;">✉️ Email Pembayaran Terkirim</h2>
        <p>Email pemberitahuan tagihan 25K telah dikirim ke owner di <b>${email}</b>.</p>
      </div>
    `);
  } catch (err: any) {
    console.error('Reject expansion failed:', err);
    res.status(500).send('Gagal memproses penolakan: ' + err.message);
  }
});

// ─────────────────────────────────────────────────────────────
// Auth - Login with name + PIN
// ─────────────────────────────────────────────────────────────
app.post('/api/auth/login', async (req, res) => {
  res.status(400).json({
    error: 'Metode login menggunakan Nama & PIN sudah dinonaktifkan karena telah digabungkan ke Email & Password. Silakan gunakan versi terbaru.'
  });
});

// ─────────────────────────────────────────────────────────────
// Auth - Google Login Registration for Demo Trial (2-Day Limit)
// ─────────────────────────────────────────────────────────────
app.post('/api/auth/google-register', async (req, res) => {
  try {
    const { email, name, businessMode, whatsapp } = req.body;
    if (!email) return res.status(400).json({ error: 'Email wajib diisi' });

    const cleanEmail = email.toLowerCase().trim();
    const cleanName = name || cleanEmail.split('@')[0];
    const mode = businessMode || 'FNB';

    let googleUser = await prisma.googleUser.findUnique({
      where: { email: cleanEmail }
    });

    if (googleUser) {
      if (!googleUser.isConfirmed && googleUser.demoExpiresAt && new Date() > googleUser.demoExpiresAt) {
        return res.status(403).json({
          error: 'Masa demo gratis 2 hari Anda telah kedaluwarsa dan tidak dibayar. Email ini tidak dapat digunakan untuk demo lagi. Silakan hubungi Admin POSBah untuk peningkatan ke Akun Premium.',
          code: 'DEMO_EXPIRED'
        });
      }
    }

    let isNewUser = false;
    if (!googleUser) {
      isNewUser = true;
      const token = crypto.randomBytes(32).toString('hex');
      const demoExpiresAt = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000); // 2 hari

      googleUser = await prisma.googleUser.create({
        data: {
          id: cleanEmail,
          email: cleanEmail,
          userName: cleanName,
          businessMode: mode,
          confirmToken: token,
          demoExpiresAt,
          whatsapp: whatsapp || null
        }
      });

      // Provision tenant database
      try {
        const dbName = await createTenantDatabase(cleanEmail);
        await runTenantMigration(dbName);
      } catch (err) {
        console.error(`Failed to initialize database for google user ${cleanEmail}:`, err);
        await prisma.googleUser.delete({ where: { email: cleanEmail } });
        return res.status(500).json({ error: 'Gagal menginisialisasi database penyewa baru' });
      }

      // Kirim email notifikasi ke owner
      const baseUrl = process.env.APP_BASE_URL || 'https://103.93.163.227';
      const confirmUrl = `${baseUrl}/api/admin/confirm-demo?token=${token}`;
      const modeLabels: Record<string, string> = {
        FNB: '🍹 Retail & F&B (Kasir, Stok, Diskon)',
        RENTAL: '🚗 Rental Mobil',
        LAUNDRY: '🧺 Laundry',
        BMP: '🏭 Manufaktur & Invoice (BMP)'
      };
      const modeLabel = modeLabels[mode] || mode;
      const ownerEmail = process.env.ADMIN_BILLING_EMAIL || 'muhammadmuizz8@gmail.com';
      const now = new Date().toLocaleString('id-ID', { timeZone: 'Asia/Jakarta' });

      if (ownerEmail) {
        await sendEmail(
          ownerEmail,
          `🆕 Demo Baru - ${cleanName} memilih paket ${modeLabel}`,
          `
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; background: #f8f9fa; border-radius: 12px;">
            <div style="background: linear-gradient(135deg, #1e1b4b, #4c1d95); padding: 30px; border-radius: 12px; margin-bottom: 20px; text-align: center;">
              <h1 style="color: white; margin: 0; font-size: 24px;">🆕 User Demo Baru!</h1>
            </div>
            <div style="background: white; padding: 25px; border-radius: 12px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
              <h2 style="color: #1e293b; margin-top: 0;">Detail User</h2>
              <table style="width: 100%; border-collapse: collapse;">
                <tr><td style="padding: 8px; color: #64748b; width: 120px;">Nama</td><td style="padding: 8px; font-weight: bold; color: #1e293b;">${cleanName}</td></tr>
                <tr style="background: #f8fafc;"><td style="padding: 8px; color: #64748b;">Email</td><td style="padding: 8px; font-weight: bold; color: #1e293b;">${cleanEmail}</td></tr>
                <tr><td style="padding: 8px; color: #64748b;">Paket</td><td style="padding: 8px; font-weight: bold; color: #1e293b;">${modeLabel}</td></tr>
                <tr style="background: #f8fafc;"><td style="padding: 8px; color: #64748b;">Waktu Daftar</td><td style="padding: 8px; font-weight: bold; color: #1e293b;">${now} WIB</td></tr>
              </table>
            </div>
            <div style="text-align: center; margin-bottom: 20px;">
              <a href="${confirmUrl}" style="display: inline-block; padding: 16px 32px; background: linear-gradient(135deg, #10b981, #059669); color: white; text-decoration: none; border-radius: 12px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 12px rgba(16,185,129,0.3);">✅ Konfirmasi Pembayaran</a>
            </div>
            <p style="color: #94a3b8; font-size: 12px; text-align: center;">Jika tidak dikonfirmasi, demo akan otomatis berakhir dalam 2 hari.</p>
          </div>
          `
        );
      }
    }

    // Hitung expiresAt: jika sudah dikonfirmasi, unlimited (99 tahun); jika belum, dari demoExpiresAt
    const expiresAt = googleUser.isConfirmed
      ? new Date(Date.now() + 99 * 365 * 24 * 60 * 60 * 1000).toISOString()
      : (googleUser.demoExpiresAt?.toISOString() || new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString());

    res.json({
      email: googleUser.email,
      registeredAt: googleUser.registeredAt.toISOString(),
      expiresAt,
      isConfirmed: googleUser.isConfirmed,
      isNewUser
    });
  } catch (error) {
    console.error('Failed in google-register:', error);
    res.status(500).json({ error: 'Gagal memproses pendaftaran Google' });
  }
});

// ─────────────────────────────────────────────────────────────
// Auth - Email Trial Registration for Demo (Alternative to Google OAuth)
// ─────────────────────────────────────────────────────────────
app.post('/api/auth/email-register-demo', async (req, res) => {
  try {
    const { email, name, password, businessMode, whatsapp } = req.body;
    if (!email || !password) return res.status(400).json({ error: 'Email dan password wajib diisi' });

    const cleanEmail = email.toLowerCase().trim();
    const cleanName = name || cleanEmail.split('@')[0];
    const mode = businessMode || 'FNB';

    // Cek apakah email sudah terdaftar di PremiumUser atau GoogleUser
    const existingPremium = await prisma.premiumUser.findUnique({ where: { email: cleanEmail } });
    if (existingPremium) return res.status(400).json({ error: 'Email ini sudah terdaftar sebagai akun Premium' });

    let googleUser = await prisma.googleUser.findUnique({
      where: { email: cleanEmail }
    });

    if (googleUser) {
      if (!googleUser.isConfirmed && googleUser.demoExpiresAt && new Date() > googleUser.demoExpiresAt) {
        return res.status(403).json({
          error: 'Masa demo gratis 2 hari Anda telah kedaluwarsa dan tidak dibayar. Email ini tidak dapat digunakan untuk demo lagi. Silakan hubungi Admin POSBah untuk peningkatan ke Akun Premium.',
          code: 'DEMO_EXPIRED'
        });
      }
      return res.status(400).json({ error: 'Email ini sudah terdaftar sebagai akun demo' });
    }

    const token = crypto.randomBytes(32).toString('hex');
    const demoExpiresAt = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000); // 2 hari
    const hashedPassword = hashPassword(password);

    googleUser = await prisma.googleUser.create({
      data: {
        id: cleanEmail,
        email: cleanEmail,
        userName: cleanName,
        businessMode: mode,
        confirmToken: token,
        demoExpiresAt,
        passwordHash: hashedPassword,
        whatsapp: whatsapp || null
      }
    });

    // Provision tenant database
    try {
      const dbName = await createTenantDatabase(cleanEmail);
      await runTenantMigration(dbName);
    } catch (err) {
      console.error(`Failed to initialize database for demo email user ${cleanEmail}:`, err);
      await prisma.googleUser.delete({ where: { email: cleanEmail } });
      return res.status(500).json({ error: 'Gagal menginisialisasi database penyewa baru' });
    }

    // Kirim email notifikasi ke owner
    const baseUrl = process.env.APP_BASE_URL || 'https://103.93.163.227';
    const confirmUrl = `${baseUrl}/api/admin/confirm-demo?token=${token}`;
    const modeLabels: Record<string, string> = {
      FNB: '🍹 Retail & F&B (Kasir, Stok, Diskon)',
      RENTAL: '🚗 Rental Mobil',
      LAUNDRY: '🧺 Laundry',
      BMP: '🏭 Manufaktur & Invoice (BMP)'
    };
    const modeLabel = modeLabels[mode] || mode;
    const ownerEmail = process.env.ADMIN_BILLING_EMAIL || 'muhammadmuizz8@gmail.com';
    const now = new Date().toLocaleString('id-ID', { timeZone: 'Asia/Jakarta' });

    if (ownerEmail) {
      await sendEmail(
        ownerEmail,
        `🆕 Demo Baru (Email) - ${cleanName} memilih paket ${modeLabel}`,
        `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; background: #f8f9fa; border-radius: 12px;">
          <div style="background: linear-gradient(135deg, #1e1b4b, #4c1d95); padding: 30px; border-radius: 12px; margin-bottom: 20px; text-align: center;">
            <h1 style="color: white; margin: 0; font-size: 24px;">🆕 User Demo Baru (Email)!</h1>
          </div>
          <div style="background: white; padding: 25px; border-radius: 12px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
            <h2 style="color: #1e293b; margin-top: 0;">Detail User</h2>
            <table style="width: 100%; border-collapse: collapse;">
              <tr><td style="padding: 8px; color: #64748b; width: 120px;">Nama</td><td style="padding: 8px; font-weight: bold; color: #1e293b;">${cleanName}</td></tr>
              <tr style="background: #f8fafc;"><td style="padding: 8px; color: #64748b;">Email</td><td style="padding: 8px; font-weight: bold; color: #1e293b;">${cleanEmail}</td></tr>
              <tr><td style="padding: 8px; color: #64748b;">Paket</td><td style="padding: 8px; font-weight: bold; color: #1e293b;">${modeLabel}</td></tr>
              <tr style="background: #f8fafc;"><td style="padding: 8px; color: #64748b;">Waktu Daftar</td><td style="padding: 8px; font-weight: bold; color: #1e293b;">${now} WIB</td></tr>
            </table>
          </div>
          <div style="text-align: center; margin-bottom: 20px;">
            <a href="${confirmUrl}" style="display: inline-block; padding: 16px 32px; background: linear-gradient(135deg, #10b981, #059669); color: white; text-decoration: none; border-radius: 12px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 12px rgba(16,185,129,0.3);">✅ Konfirmasi Pembayaran</a>
          </div>
          <p style="color: #94a3b8; font-size: 12px; text-align: center;">Jika tidak dikonfirmasi, demo akan otomatis berakhir dalam 2 hari.</p>
        </div>
        `
      );
    }

    const expiresAt = googleUser.demoExpiresAt?.toISOString() || new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString();

    res.json({
      email: googleUser.email,
      registeredAt: googleUser.registeredAt.toISOString(),
      expiresAt,
      isConfirmed: false,
      isNewUser: true
    });
  } catch (error) {
    console.error('Failed in email-register-demo:', error);
    res.status(500).json({ error: 'Gagal memproses pendaftaran demo' });
  }
});

// ─────────────────────────────────────────────────────────────
// Admin - Halaman Konfirmasi Demo (GET: tampilkan form HTML)
// ─────────────────────────────────────────────────────────────
app.get('/api/admin/confirm-demo', async (req, res) => {
  try {
    const { token } = req.query as { token: string };
    if (!token) return res.status(400).send('<h1>Token tidak valid</h1>');

    const user = await prisma.googleUser.findUnique({
      where: { confirmToken: token }
    });

    if (!user) return res.status(404).send('<h1 style="font-family:Arial">Link tidak valid atau sudah digunakan.</h1>');

    const modeLabels: Record<string, string> = {
      FNB: '🍹 Retail & F&B',
      RENTAL: '🚗 Rental Mobil',
      LAUNDRY: '🧺 Laundry',
      BMP: '🏭 Manufaktur & Invoice (BMP)'
    };
    const modeLabel = modeLabels[user.businessMode] || user.businessMode;
    const registeredAt = user.registeredAt.toLocaleString('id-ID', { timeZone: 'Asia/Jakarta' });

    if (user.isConfirmed) {
      return res.send(`
        <!DOCTYPE html><html lang="id"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Sudah Dikonfirmasi - POSBah</title>
        <style>body{font-family:Arial,sans-serif;background:#f0fdf4;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;}
        .card{background:white;padding:40px;border-radius:16px;box-shadow:0 4px 24px rgba(0,0,0,0.1);max-width:500px;width:90%;text-align:center;}
        .icon{font-size:64px;margin-bottom:16px;}.title{font-size:24px;font-weight:bold;color:#15803d;margin:0 0 8px;}.sub{color:#64748b;}</style></head>
        <body><div class="card"><div class="icon">✅</div>
        <h1 class="title">Sudah Dikonfirmasi</h1>
        <p class="sub">User <strong>${user.email}</strong> sudah pernah dikonfirmasi pada ${user.confirmedAt?.toLocaleString('id-ID', { timeZone: 'Asia/Jakarta' })} WIB.</p>
        </div></body></html>
      `);
    }

    res.send(`
      <!DOCTYPE html><html lang="id"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
      <title>Konfirmasi Demo - POSBah Admin</title>
      <style>
        *{box-sizing:border-box;} body{font-family:Arial,sans-serif;background:linear-gradient(135deg,#1e1b4b,#4c1d95);min-height:100vh;display:flex;align-items:center;justify-content:center;margin:0;padding:20px;}
        .card{background:white;border-radius:20px;padding:40px;max-width:520px;width:100%;box-shadow:0 20px 60px rgba(0,0,0,0.3);}
        h1{color:#1e293b;font-size:22px;margin:0 0 24px;text-align:center;}
        .info{background:#f8fafc;border-radius:12px;padding:20px;margin-bottom:24px;}
        .info-row{display:flex;padding:8px 0;border-bottom:1px solid #f1f5f9;}
        .info-row:last-child{border:none;}
        .info-label{color:#64748b;width:130px;font-size:14px;}
        .info-value{font-weight:bold;color:#1e293b;font-size:14px;}
        .btn{display:block;width:100%;padding:16px;border:none;border-radius:12px;font-size:16px;font-weight:bold;cursor:pointer;margin-bottom:12px;transition:all 0.2s;}
        .btn-approve{background:linear-gradient(135deg,#10b981,#059669);color:white;box-shadow:0 4px 12px rgba(16,185,129,0.3);}
        .btn-approve:hover{transform:translateY(-2px);box-shadow:0 6px 16px rgba(16,185,129,0.4);}
        .btn-reject{background:#f1f5f9;color:#64748b;}
        .btn-reject:hover{background:#e2e8f0;}
        .msg{display:none;padding:12px;border-radius:8px;text-align:center;margin-top:16px;font-weight:bold;}
        .msg.success{background:#dcfce7;color:#15803d;}
        .msg.error{background:#fee2e2;color:#dc2626;}
      </style></head><body>
      <div class="card">
        <h1>🔐 Konfirmasi Pembayaran Demo</h1>
        <div class="info">
          <div class="info-row"><span class="info-label">Nama</span><span class="info-value">${user.userName || user.email}</span></div>
          <div class="info-row"><span class="info-label">Email</span><span class="info-value">${user.email}</span></div>
          <div class="info-row"><span class="info-label">Paket</span><span class="info-value">${modeLabel}</span></div>
          <div class="info-row"><span class="info-label">Daftar</span><span class="info-value">${registeredAt} WIB</span></div>
        </div>
        <button class="btn btn-approve" onclick="doAction('approve')">✅ Konfirmasi — User Ini Sudah Bayar</button>
        <button class="btn btn-reject" onclick="doAction('reject')">❌ Belum Bayar — Biarkan Demo 2 Hari</button>
        <div class="msg" id="msg"></div>
      </div>
      <script>
        async function doAction(action) {
          const msgEl = document.getElementById('msg');
          msgEl.style.display='none';
          const res = await fetch('/api/admin/confirm-demo', {
            method: 'POST',
            headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ token: '${token}', action })
          });
          const data = await res.json();
          msgEl.style.display='block';
          if(data.success) {
            msgEl.className='msg success';
            msgEl.textContent = action==='approve' ? '✅ Berhasil! Email aktivasi telah dikirim ke user.' : '✅ Oke, demo dibiarkan berjalan 2 hari.';
          } else {
            msgEl.className='msg error';
            msgEl.textContent = data.error || 'Gagal memproses';
          }
        }
      </script></body></html>
    `);
  } catch (error) {
    console.error('Error in confirm-demo GET:', error);
    res.status(500).send('<h1>Terjadi kesalahan server</h1>');
  }
});

// ─────────────────────────────────────────────────────────────
// Admin - Proses Konfirmasi Demo (POST: approve / reject)
// ─────────────────────────────────────────────────────────────
app.post('/api/admin/confirm-demo', async (req, res) => {
  try {
    const { token, action } = req.body;
    if (!token || !action) return res.status(400).json({ error: 'Token dan action wajib diisi' });

    const user = await prisma.googleUser.findUnique({
      where: { confirmToken: token }
    });

    if (!user) return res.status(404).json({ error: 'Token tidak valid atau sudah digunakan' });
    if (user.isConfirmed) return res.status(400).json({ error: 'User ini sudah pernah dikonfirmasi' });

    if (action === 'reject') {
      // Biarkan demo 2 hari berjalan normal, hapus token saja
      await prisma.googleUser.update({
        where: { email: user.email },
        data: { confirmToken: null }
      });
      return res.json({ success: true, message: 'Demo dibiarkan berjalan 2 hari' });
    }

    if (action === 'approve') {
      // 1. Tandai sebagai confirmed
      await prisma.googleUser.update({
        where: { email: user.email },
        data: {
          isConfirmed: true,
          confirmedAt: new Date(),
          confirmToken: null // Hapus token setelah dipakai
        }
      });

      // 2. Tentukan password hash
      let hashedPassword = user.passwordHash;
      let tempPassword = '';
      if (!hashedPassword) {
        tempPassword = generatePassword();
        hashedPassword = hashPassword(tempPassword);
      }

      // 3. Buat/update PremiumUser agar bisa login email
      await prisma.premiumUser.upsert({
        where: { email: user.email },
        update: { 
          passwordHash: hashedPassword,
          whatsapp: user.whatsapp || null
        },
        create: {
          id: user.email,
          email: user.email,
          passwordHash: hashedPassword,
          name: user.userName || user.email.split('@')[0],
          role: 'OWNER',
          tenantId: user.email,
          whatsapp: user.whatsapp || null
        }
      });

      // 4. Update user di tenant BMP db (jika ada)
      try {
        const tenantPrisma = getTenantPrisma(user.email);
        const existingBmpUser = await tenantPrisma.$queryRaw<any[]>`
          SELECT id FROM users WHERE username = ${user.email} LIMIT 1
        `;
        if (existingBmpUser && existingBmpUser.length > 0) {
          // Gunakan bcrypt hash yang kompatibel dengan Go backend
          // Go backend menggunakan bcrypt, jadi kita simpan plaintext dan biarkan Go yang hash
          // Untuk sederhananya, kita tidak ubah password BMP di sini
        }
      } catch (err) {
        console.warn('Could not update BMP user password:', err);
      }

      // 5. Kirim email ke user dengan credentials
      const baseUrl = process.env.APP_BASE_URL || 'https://103.93.163.227';
      await sendEmail(
        user.email,
        '✅ Akun POSBah Anda Telah Diaktifkan!',
        `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; background: #f8f9fa; border-radius: 12px;">
          <div style="background: linear-gradient(135deg, #1e1b4b, #4c1d95); padding: 30px; border-radius: 12px; margin-bottom: 20px; text-align: center;">
            <h1 style="color: white; margin: 0; font-size: 24px;">🎉 Selamat! Akun Anda Aktif</h1>
          </div>
          <div style="background: white; padding: 25px; border-radius: 12px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
            <p style="color: #1e293b;">Halo <strong>${user.userName || user.email}</strong>,</p>
            <p style="color: #475569;">Pembayaran Anda telah dikonfirmasi. Akun POSBah Anda kini aktif dan bisa digunakan tanpa batas waktu.</p>
            <div style="background: #f8fafc; border-left: 4px solid #10b981; padding: 16px; border-radius: 8px; margin: 20px 0;">
              <p style="margin: 0 0 8px; color: #64748b; font-size: 13px;">KREDENSIAL LOGIN</p>
              <p style="margin: 0 0 4px; color: #1e293b;"><strong>Email:</strong> ${user.email}</p>
              ${tempPassword ? `
              <p style="margin: 0 0 4px; color: #1e293b;"><strong>Password Sementara:</strong> <code style="background: #e2e8f0; padding: 2px 8px; border-radius: 4px; font-size: 16px;">${tempPassword}</code></p>
              ` : `
              <p style="margin: 0 0 4px; color: #1e293b;"><strong>Password:</strong> Gunakan password yang Anda daftarkan saat registrasi demo.</p>
              `}
            </div>
            ${tempPassword ? '<p style="color: #94a3b8; font-size: 13px;">⚠️ Harap simpan password ini. Anda dapat mengganti password setelah login.</p>' : ''}
          </div>
          <div style="text-align: center;">
            <a href="${baseUrl}" style="display: inline-block; padding: 14px 28px; background: linear-gradient(135deg, #10b981, #059669); color: white; text-decoration: none; border-radius: 12px; font-weight: bold;">🔐 Login Sekarang</a>
          </div>
        </div>
        `
      );

      return res.json({ success: true, message: 'Akun diaktifkan dan email dikirim ke user' });
    }

    res.status(400).json({ error: 'Action tidak valid. Gunakan: approve atau reject' });
  } catch (error) {
    console.error('Error in confirm-demo POST:', error);
    res.status(500).json({ error: 'Terjadi kesalahan server' });
  }
});

// ─────────────────────────────────────────────────────────────
// Auth - Premium Email Registration
// ─────────────────────────────────────────────────────────────
app.post('/api/auth/register-email', async (req, res) => {
  try {
    const { email, password, name, role, whatsapp } = req.body;
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
        role: role || 'OWNER',
        whatsapp: whatsapp || null
      }
    });

    // Provision tenant database
    try {
      const dbName = await createTenantDatabase(cleanEmail);
      await runTenantMigration(dbName);
    } catch (err) {
      console.error(`Failed to initialize database for premium user ${cleanEmail}:`, err);
      // Rollback creation in main db
      await prisma.premiumUser.delete({ where: { email: cleanEmail } });
      return res.status(500).json({ error: 'Gagal menginisialisasi database penyewa baru' });
    }

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

    if (user) {
      const oldSha256 = crypto.createHash('sha256').update(password).digest('hex');
      const isMatch = verifyPassword(password, user.passwordHash) || user.passwordHash === oldSha256 || user.passwordHash === password;

      if (!isMatch) return res.status(401).json({ error: 'Email atau password salah' });

      // Cari businessMode dari GoogleUser (jika terdaftar sebelumnya sebagai demo, atau dari tenantId jika karyawan)
      const targetEmailForMode = user.tenantId || cleanEmail;
      const googleUser = await prisma.googleUser.findUnique({
        where: { email: targetEmailForMode }
      });
      const businessMode = googleUser ? googleUser.businessMode : undefined;

      return res.json({
        id: cleanEmail,
        name: user.name,
        email: cleanEmail,
        role: user.role,
        isDemo: false,
        businessMode, // Sertakan di response agar frontend langsung memetakan mode bisnis
        tenantId: user.tenantId || cleanEmail,
        registeredAt: user.registeredAt.toISOString()
      });
    }

    // Jika tidak ditemukan di PremiumUser, coba cari di GoogleUser (untuk demo email)
    const demoUser = await prisma.googleUser.findUnique({
      where: { email: cleanEmail }
    });

    if (!demoUser || !demoUser.passwordHash) {
      return res.status(401).json({ error: 'Email atau password salah' });
    }

    const isDemoMatch = verifyPassword(password, demoUser.passwordHash);
    if (!isDemoMatch) return res.status(401).json({ error: 'Email atau password salah' });

    if (!demoUser.isConfirmed && demoUser.demoExpiresAt && new Date() > demoUser.demoExpiresAt) {
      return res.status(403).json({
        error: 'Masa demo gratis 2 hari Anda telah kedaluwarsa dan tidak dibayar. Email ini tidak dapat digunakan untuk demo lagi. Silakan hubungi Admin POSBah untuk peningkatan ke Akun Premium.',
        code: 'DEMO_EXPIRED'
      });
    }

    // Hitung expiresAt: jika sudah dikonfirmasi, unlimited; jika belum, dari demoExpiresAt
    const expiresAt = demoUser.isConfirmed
      ? new Date(Date.now() + 99 * 365 * 24 * 60 * 60 * 1000).toISOString()
      : (demoUser.demoExpiresAt?.toISOString() || new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString());

    res.json({
      id: cleanEmail,
      name: demoUser.userName || cleanEmail.split('@')[0],
      email: cleanEmail,
      role: 'OWNER',
      isDemo: true,
      businessMode: demoUser.businessMode,
      tenantId: cleanEmail,
      registeredAt: demoUser.registeredAt.toISOString(),
      expiresAt
    });
  } catch (error) {
    console.error('Failed in login-email:', error);
    res.status(500).json({ error: 'Gagal melakukan login email' });
  }
});

app.get('/api/auth/me', async (req, res) => {
  try {
    const employeeIdHeader = req.headers['x-employee-id'] as string;
    if (!employeeIdHeader) return res.status(401).json({ error: 'Unauthorized' });

    let employee: any = null;
    if (employeeIdHeader.includes('@')) {
      employee = await prisma.employee.findFirst({
        where: { email: employeeIdHeader },
        include: { outlet: true }
      });
    } else {
      const numericId = Number(employeeIdHeader);
      if (!isNaN(numericId)) {
        employee = await prisma.employee.findUnique({
          where: { id: numericId },
          include: { outlet: true }
        });
      }
    }

    if (!employee) return res.status(404).json({ error: 'Employee not found' });
    res.json(employee);
  } catch (error) {
    console.error('Failed fetching auth me profile:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─────────────────────────────────────────────────────────────
// Outlets (READ: KASIR+ | WRITE: OWNER only)
// ─────────────────────────────────────────────────────────────

app.get('/api/outlets', async (req, res) => {
  try {
    const outlets = await prisma.outlet.findMany({
      include: {
        _count: {
          select: {
            employees: true,
            products: true,
            transactions: true
          }
        }
      },
      orderBy: { name: 'asc' }
    });
    res.json(outlets);
  } catch (error) {
    console.error('Failed to fetch outlets:', error);
    res.status(400).json({ error: 'Failed to fetch outlets' });
  }
});

app.post('/api/outlets', requireOwner, async (req, res) => {
  try {
    const { name, address, phone } = req.body;
    if (!name) return res.status(400).json({ error: 'Nama outlet wajib diisi' });
    const outlet = await prisma.outlet.create({
      data: { name, address, phone }
    });
    const employeeIdHeader = req.headers['x-employee-id'] as string;
    logActivity(
      employeeIdHeader,
      'CREATE_OUTLET',
      `Membuat outlet baru: ${name}`
    );
    res.json(outlet);
  } catch (error) {
    console.error('Failed to create outlet:', error);
    res.status(400).json({ error: 'Failed to create outlet' });
  }
});

app.put('/api/outlets/:id', requireOwner, async (req, res) => {
  try {
    const { id } = req.params;
    const { name, address, phone } = req.body;
    if (!name) return res.status(400).json({ error: 'Nama outlet wajib diisi' });
    const outlet = await prisma.outlet.update({
      where: { id: Number(id) },
      data: { name, address, phone }
    });
    const employeeIdHeader = req.headers['x-employee-id'] as string;
    logActivity(
      employeeIdHeader,
      'UPDATE_OUTLET',
      `Mengubah outlet: ${name}`
    );
    res.json(outlet);
  } catch (error) {
    console.error('Failed to update outlet:', error);
    res.status(400).json({ error: 'Failed to update outlet' });
  }
});

app.delete('/api/outlets/:id', requireOwner, async (req, res) => {
  try {
    const { id } = req.params;
    const outletId = Number(id);

    // Cek apakah outlet masih digunakan oleh produk atau karyawan
    const hasEmployees = await prisma.employee.findFirst({ where: { outletId } });
    if (hasEmployees) {
      return res.status(400).json({ error: 'Outlet tidak dapat dihapus karena masih memiliki karyawan.' });
    }

    const hasProducts = await prisma.product.findFirst({ where: { outletId } });
    if (hasProducts) {
      return res.status(400).json({ error: 'Outlet tidak dapat dihapus karena masih memiliki produk.' });
    }

    const outlet = await prisma.outlet.findUnique({ where: { id: outletId } });
    await prisma.outlet.delete({ where: { id: outletId } });
    
    const employeeIdHeader = req.headers['x-employee-id'] as string;
    logActivity(
      employeeIdHeader,
      'DELETE_OUTLET',
      `Menghapus outlet: ${outlet?.name || outletId}`
    );
    res.json({ success: true });
  } catch (error) {
    console.error('Failed to delete outlet:', error);
    res.status(400).json({ error: 'Failed to delete outlet' });
  }
});

// ─────────────────────────────────────────────────────────────
// Queue endpoints — Bisa diakses KASIR (semua role)
// ─────────────────────────────────────────────────────────────

// Antrian aktif (PENDING + ada queueNumber) — untuk cek slot yang terpakai
app.get('/api/queues/active', async (req, res) => {
  try {
    const outletIdHeader = req.headers['x-outlet-id'];
    const outletId = outletIdHeader ? Number(outletIdHeader) : undefined;
    const where: any = { status: 'PENDING', queueNumber: { not: null } };
    if (outletId && !isNaN(outletId)) {
      where.outletId = outletId;
    }
    const queues = await prisma.transaction.findMany({
      where,
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
    const outletIdHeader = req.headers['x-outlet-id'];
    const outletId = outletIdHeader ? Number(outletIdHeader) : undefined;
    const where: any = { status: 'PENDING' };
    if (outletId && !isNaN(outletId)) {
      where.outletId = outletId;
    }
    const queues = await prisma.transaction.findMany({
      where,
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
  try {
    const outletIdHeader = req.headers['x-outlet-id'];
    const outletId = outletIdHeader ? Number(outletIdHeader) : undefined;
    const where: any = {};
    if (outletId && !isNaN(outletId)) {
      where.outletId = outletId;
    }
    const products = await prisma.product.findMany({ where });
    res.json(products);
  } catch (error) {
    res.status(400).json({ error: 'Failed to fetch products' });
  }
});

// Lookup produk by barcode (untuk scanner kasir)
app.get('/api/products/barcode/:code', async (req, res) => {
  try {
    const outletIdHeader = req.headers['x-outlet-id'];
    const outletId = outletIdHeader ? Number(outletIdHeader) : undefined;
    const where: any = { barcode: req.params.code };
    if (outletId && !isNaN(outletId)) {
      where.outletId = outletId;
    }
    const product = await (prisma.product as any).findFirst({
      where
    });
    if (!product) return res.status(404).json({ error: 'Produk tidak ditemukan' });
    res.json(product);
  } catch (error) {
    res.status(400).json({ error: 'Gagal mencari produk' });
  }
});

app.post('/api/products', requireAdmin, async (req, res) => {
  try {
    const outletIdHeader = req.headers['x-outlet-id'];
    const outletId = outletIdHeader ? Number(outletIdHeader) : undefined;
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
        ...(barcode ? { barcode } : {}),
        ...(outletId && !isNaN(outletId) ? { outletId } : {})
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
    const { name, price, costPrice, stock, unit, barcode, category, wholesaleEnabled, wholesalePrices, variants, image, outletId } = req.body;
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
        ...(barcode !== undefined ? { barcode: barcode || null } : {}),
        ...(outletId !== undefined ? { outletId: outletId ? Number(outletId) : null } : {})
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
    const outletIdHeader = req.headers['x-outlet-id'];
    const outletId = outletIdHeader ? Number(outletIdHeader) : undefined;
    const where: any = {};
    if (outletId && !isNaN(outletId)) {
      where.outletId = outletId;
    }
    const transactions = await prisma.transaction.findMany({
      where,
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

    const employeeId = await resolveEmployeeId(req);
    const outletIdHeader = req.headers['x-outlet-id'];
    const outletId = outletIdHeader ? Number(outletIdHeader) : undefined;

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
          ...(outletId && !isNaN(outletId) ? { outletId } : {}),
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
    const tenantId = req.headers['x-tenant-id'] as string;
    if (tenantId) {
      await syncTenantEmployees(tenantId);
    }

    const employees = await prisma.employee.findMany({
      where: {
        name: {
          not: 'muizz',
          mode: 'insensitive'
        }
      },
      include: { outlet: true },
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
    if (employeeIdHeader === '0' || employeeIdHeader === '9999') {
      return res.status(403).json({ error: 'Akun demo tidak dapat menambah karyawan' });
    }
    const tenantId = req.headers['x-tenant-id'] as string;
    let maxLimit = 4;
    if (tenantId) {
      const tenantLimit = await mainPrisma.tenantLimit.findUnique({ where: { tenantId } });
      if (tenantLimit) {
        maxLimit = tenantLimit.limit;
      }
    }

    const count = await prisma.employee.count();
    if (count >= maxLimit) {
      return res.status(400).json({ 
        error: `Batas maksimal ${maxLimit} karyawan telah tercapai. Silakan lakukan ekspansi kapasitas karyawan.`,
        limitReached: true,
        maxLimit
      });
    }
    const { name, email, role, pin, salary, outletId } = req.body;
    if (!email) {
      return res.status(400).json({ error: 'Email karyawan wajib diisi' });
    }

    const cleanEmail = email.toLowerCase().trim();

    // Cek keunikan email secara global
    const existingPremium = await mainPrisma.premiumUser.findUnique({ where: { email: cleanEmail } });
    if (existingPremium) {
      return res.status(400).json({ error: 'Email ini sudah terdaftar di sistem' });
    }
    const existingGoogle = await mainPrisma.googleUser.findUnique({ where: { email: cleanEmail } });
    if (existingGoogle) {
      return res.status(400).json({ error: 'Email ini sudah terdaftar sebagai akun demo' });
    }

    // Cek keunikan email di database penyewa lokal
    const existingEmployee = await prisma.employee.findFirst({ where: { email: cleanEmail } });
    if (existingEmployee) {
      return res.status(400).json({ error: 'Email ini sudah terdaftar sebagai karyawan di toko Anda' });
    }

    // OWNER bisa tambah OWNER/ADMIN/KASIR; ADMIN hanya bisa tambah KASIR
    const requesterRole = req.headers['x-employee-role'] as string;
    if (requesterRole !== 'OWNER' && role === 'OWNER') {
      return res.status(403).json({ error: 'Hanya OWNER yang dapat membuat akun OWNER' });
    }

    const hashedPin = pin && pin.length === 128 ? pin : hashPassword(pin || '');

    // 1. Simpan di database penyewa
    const employee = await prisma.employee.create({
      data: {
        name,
        email: cleanEmail,
        role: role || 'KASIR',
        pin: hashedPin,
        salary: role === 'OWNER' ? 0 : Number(salary || 0), // Gaji owner selalu 0
        ...(outletId ? { outletId: Number(outletId) } : {})
      }
    });

    // 2. Simpan di database global PremiumUser agar bisa login menggunakan email & password
    await mainPrisma.premiumUser.create({
      data: {
        id: cleanEmail,
        email: cleanEmail,
        passwordHash: hashedPin,
        name,
        role: role || 'KASIR',
        tenantId: tenantId
      }
    });

    // 3. Dapatkan Nama Bisnis Owner dari database global
    let businessName = 'POSBah';
    const ownerUser = await mainPrisma.premiumUser.findUnique({ where: { email: tenantId } });
    if (ownerUser) {
      businessName = ownerUser.name;
    } else {
      const googleOwner = await mainPrisma.googleUser.findUnique({ where: { email: tenantId } });
      if (googleOwner) {
        businessName = googleOwner.userName || tenantId;
      }
    }

    // 4. Kirim Gmail selamat bergabung otomatis ke karyawan
    const subject = `Selamat bergabung ke: ${businessName}`;
    const emailHtml = `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E5E7EB; border-radius: 10px;">
        <h2 style="color: #4F46E5; margin-top: 0; font-size: 1.4rem;">🎉 Selamat Bergabung!</h2>
        <p>Halo <b>${name}</b>,</p>
        <p>Anda telah terdaftar sebagai karyawan di <b>${businessName}</b>. Mulai hari ini, berikut adalah data kredensial login Anda:</p>
        <table style="width: 100%; border-collapse: collapse; margin: 15px 0; background: #F9FAFB; border-radius: 8px; border: 1px solid #E5E7EB;">
          <tr>
            <td style="padding: 10px; border-bottom: 1px solid #E5E7EB; font-weight: bold; width: 140px; color: #374151;">Email:</td>
            <td style="padding: 10px; border-bottom: 1px solid #E5E7EB; color: #111827; font-family: monospace; font-size: 14px;">${cleanEmail}</td>
          </tr>
          <tr>
            <td style="padding: 10px; font-weight: bold; color: #374151;">Password:</td>
            <td style="padding: 10px; color: #111827; font-family: monospace; font-size: 14px; font-weight: bold;">${pin}</td>
          </tr>
        </table>
        <p style="margin-top: 20px; font-size: 13px; color: #4B5563;">
          ⚠️ <b>Penting:</b> Jika Anda ingin mengganti password atau lupa password, silakan hubungi Owner Anda di email: 
          <a href="mailto:${tenantId}" style="color: #4F46E5; text-decoration: underline; font-weight: 600;">${tenantId}</a>
        </p>
        <div style="margin-top: 30px; text-align: center;">
          <a href="${process.env.APP_BASE_URL || 'https://www.zedmz.cloud'}" style="background: linear-gradient(135deg, #4F46E5, #4338CA); color: white; padding: 12px 24px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 14px; display: inline-block; box-shadow: 0 4px 10px rgba(79, 70, 229, 0.3);">Masuk ke POSBah</a>
        </div>
        <p style="font-size: 11px; color: #9CA3AF; border-top: 1px solid #E5E7EB; padding-top: 15px; margin-top: 35px; text-align: center;">Sistem Otomatis POSBah</p>
      </div>
    `;

    try {
      await sendEmail(cleanEmail, subject, emailHtml);
    } catch (mailErr) {
      console.error('Failed to send employee welcome email:', mailErr);
    }

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
    if (employeeIdHeader === '0' || employeeIdHeader === '9999') {
      return res.status(403).json({ error: 'Akun demo tidak dapat mengubah karyawan' });
    }
    const { name, role, pin, salary, outletId } = req.body;

    const requesterRole = req.headers['x-employee-role'] as string;
    if (requesterRole !== 'OWNER' && role === 'OWNER') {
      return res.status(403).json({ error: 'Hanya OWNER yang dapat mengubah role menjadi OWNER' });
    }

    const existingEmployee = await prisma.employee.findUnique({ where: { id: Number(id) } });
    if (!existingEmployee) {
      return res.status(404).json({ error: 'Karyawan tidak ditemukan' });
    }

    const updateData: any = { name, role };
    if (pin && pin.trim() !== '') {
      updateData.pin = pin.length === 128 ? pin : hashPassword(pin);
    }
    if (salary !== undefined) {
      updateData.salary = role === 'OWNER' ? 0 : Number(salary || 0); // Gaji owner selalu 0
    }
    if (outletId !== undefined) {
      updateData.outletId = outletId ? Number(outletId) : null;
    }

    // 1. Update di database penyewa lokal
    const employee = await prisma.employee.update({
      where: { id: Number(id) },
      data: updateData
    });

    // 2. Sinkronkan perubahan ke PremiumUser global
    if (existingEmployee.email) {
      const premiumUpdateData: any = { name };
      if (role !== undefined) premiumUpdateData.role = role;
      if (pin && pin.trim() !== '') {
        premiumUpdateData.passwordHash = updateData.pin;
      }
      
      await mainPrisma.premiumUser.updateMany({
        where: { email: existingEmployee.email.toLowerCase().trim() },
        data: premiumUpdateData
      });
    }

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
    if (employeeIdHeader === '0' || employeeIdHeader === '9999') {
      return res.status(403).json({ error: 'Akun demo tidak dapat menghapus karyawan' });
    }
    const currentEmployeeId = await resolveEmployeeId(req);
    if (Number(id) === currentEmployeeId) {
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
    if (!emp) {
      return res.status(404).json({ error: 'Karyawan tidak ditemukan' });
    }

    // 1. Hapus dari database penyewa lokal
    await prisma.employee.delete({ where: { id: Number(id) } });

    // 2. Hapus dari database global PremiumUser jika ada emailnya
    if (emp.email) {
      await mainPrisma.premiumUser.deleteMany({
        where: { email: emp.email.toLowerCase().trim() }
      });
    }

    logActivity(
      req.headers['x-employee-id'],
      'DELETE_EMPLOYEE',
      `Menghapus karyawan: ${emp.name}`
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
    const outletIdHeader = req.headers['x-outlet-id'];
    const outletId = outletIdHeader ? Number(outletIdHeader) : undefined;
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

    if (outletId && !isNaN(outletId)) {
      whereClause.outletId = outletId;
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

    const employeeId = await resolveEmployeeId(req);
    const outletIdHeader = req.headers['x-outlet-id'];
    const outletId = outletIdHeader ? Number(outletIdHeader) : undefined;

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
        customerId: customerId ? Number(customerId) : null,
        ...(outletId && !isNaN(outletId) ? { outletId } : {})
      }
    });

    logActivity(
      req.headers['x-employee-id'],
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

    const outletIdHeader = req.headers['x-outlet-id'];
    const outletId = outletIdHeader ? Number(outletIdHeader) : undefined;

    if (appMode === 'LAUNDRY') {
      const orderWhere: any = { statusBayar: 'Lunas' };
      const pendingWhere: any = { statusBayar: 'Belum Lunas' };
      const expenseWhere: any = {};

      if (dateFilterActive) {
        orderWhere.tanggalMasuk = dateFilter;
        pendingWhere.tanggalMasuk = dateFilter;
        expenseWhere.tanggal = dateFilter;
      }

      if (outletId && !isNaN(outletId)) {
        orderWhere.outletId = outletId;
        pendingWhere.outletId = outletId;
      }

      const totalSales = await (prisma as any).laundryOrder.aggregate({
        where: orderWhere,
        _sum: { totalHarga: true }
      });
      const totalSalesVal = totalSales._sum.totalHarga || 0;

      const todaySalesWhere: any = {
        statusBayar: 'Lunas',
        tanggalMasuk: { gte: startOfToday, lte: endOfToday }
      };
      if (outletId && !isNaN(outletId)) {
        todaySalesWhere.outletId = outletId;
      }

      const todaySales = await (prisma as any).laundryOrder.aggregate({
        where: todaySalesWhere,
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
      if (outletId && !isNaN(outletId)) {
        salesWhere.outletId = outletId;
      }
      const totalSales = await prisma.transaction.aggregate({
        where: salesWhere,
        _sum: { total: true }
      });
      totalSalesVal = totalSales._sum.total || 0;

      const todaySalesWhere: any = {
        type: 'SALES',
        status: { not: 'CANCELLED' },
        date: { gte: startOfToday, lte: endOfToday }
      };
      if (outletId && !isNaN(outletId)) {
        todaySalesWhere.outletId = outletId;
      }

      const todaySales = await prisma.transaction.aggregate({
        where: todaySalesWhere,
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
    const empId = await resolveEmployeeId(req);

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
    const empId = await resolveEmployeeId(req);

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

const syncTenantEmployees = async (tenantId: string): Promise<void> => {
  try {
    const cleanTenantId = tenantId.toLowerCase().trim();
    const dbName = getCleanTenantDbName(cleanTenantId);
    
    const dbExists = await mainPrisma.$queryRawUnsafe<any[]>(
      `SELECT 1 FROM pg_database WHERE datname = $1`,
      dbName
    );
    if (!dbExists || dbExists.length === 0) return;

    const tenantPrisma = getTenantPrisma(cleanTenantId);
    const employees = await tenantPrisma.employee.findMany({
      where: {
        email: { not: null, notIn: [''] }
      }
    });

    for (const emp of employees) {
      if (!emp.email) continue;
      const cleanEmpEmail = emp.email.toLowerCase().trim();
      if (cleanEmpEmail === cleanTenantId) continue;

      const existingPremium = await mainPrisma.premiumUser.findUnique({
        where: { email: cleanEmpEmail }
      });

      if (!existingPremium) {
        await mainPrisma.premiumUser.create({
          data: {
            id: cleanEmpEmail,
            email: cleanEmpEmail,
            passwordHash: emp.pin,
            name: emp.name,
            role: emp.role || 'KASIR',
            tenantId: cleanTenantId
          }
        });
        console.log(`[SYNC] Restored missing PremiumUser for employee: ${emp.name} (${cleanEmpEmail}) under owner ${cleanTenantId}`);
      } else {
        if (
          existingPremium.name !== emp.name ||
          existingPremium.role !== emp.role ||
          existingPremium.passwordHash !== emp.pin ||
          existingPremium.tenantId !== cleanTenantId
        ) {
          await mainPrisma.premiumUser.update({
            where: { email: cleanEmpEmail },
            data: {
              name: emp.name,
              role: emp.role || 'KASIR',
              passwordHash: emp.pin,
              tenantId: cleanTenantId
            }
          });
          console.log(`[SYNC] Updated PremiumUser data for employee: ${emp.name} (${cleanEmpEmail})`);
        }
      }
    }
  } catch (err: any) {
    console.error(`[SYNC] Failed to sync employees for tenant ${tenantId}:`, err?.message || err);
  }
};

const syncAllEmployeesToPremiumUsers = async (): Promise<void> => {
  try {
    console.log('[SYNC] Starting synchronization of all employees to PremiumUser...');
    const premiumOwners = await mainPrisma.premiumUser.findMany({
      where: { role: 'OWNER', tenantId: null }
    });
    const googleUsers = await mainPrisma.googleUser.findMany();
    
    const ownerEmails = new Set<string>();
    premiumOwners.forEach(o => ownerEmails.add(o.email.toLowerCase().trim()));
    googleUsers.forEach(g => ownerEmails.add(g.email.toLowerCase().trim()));
    
    let totalSynced = 0;
    
    for (const ownerEmail of ownerEmails) {
      const dbName = getCleanTenantDbName(ownerEmail);
      const dbExists = await mainPrisma.$queryRawUnsafe<any[]>(
        `SELECT 1 FROM pg_database WHERE datname = $1`,
        dbName
      );
      if (!dbExists || dbExists.length === 0) continue;
      
      const tenantPrisma = getTenantPrisma(ownerEmail);
      const employees = await tenantPrisma.employee.findMany({
        where: {
          email: { not: null, notIn: [''] }
        }
      });
      
      for (const emp of employees) {
        if (!emp.email) continue;
        const cleanEmpEmail = emp.email.toLowerCase().trim();
        if (cleanEmpEmail === ownerEmail) continue;
        
        const existingPremium = await mainPrisma.premiumUser.findUnique({
          where: { email: cleanEmpEmail }
        });
        
        if (!existingPremium) {
          await mainPrisma.premiumUser.create({
            data: {
              id: cleanEmpEmail,
              email: cleanEmpEmail,
              passwordHash: emp.pin,
              name: emp.name,
              role: emp.role || 'KASIR',
              tenantId: ownerEmail
            }
          });
          console.log(`[SYNC] Restored missing PremiumUser for employee: ${emp.name} (${cleanEmpEmail}) under owner ${ownerEmail}`);
          totalSynced++;
        } else {
          if (
            existingPremium.name !== emp.name ||
            existingPremium.role !== emp.role ||
            existingPremium.passwordHash !== emp.pin ||
            existingPremium.tenantId !== ownerEmail
          ) {
            await mainPrisma.premiumUser.update({
              where: { email: cleanEmpEmail },
              data: {
                name: emp.name,
                role: emp.role || 'KASIR',
                passwordHash: emp.pin,
                tenantId: ownerEmail
              }
            });
            console.log(`[SYNC] Updated PremiumUser data for employee: ${emp.name} (${cleanEmpEmail})`);
            totalSynced++;
          }
        }
      }
    }
    console.log(`[SYNC] Synchronization finished. Synced/Updated ${totalSynced} employee records.`);
  } catch (err: any) {
    console.error('[SYNC] Failed to run employee synchronization:', err?.message || err);
  }
};

const runStartupMigrations = async () => {
  try {
    await autoCreateMuizz();
    await migratePlaintextPins();
    await migrateActivityLogs();
    await syncAllEmployeesToPremiumUsers();
    console.log('All startup migrations checked/executed successfully.');
  } catch (error) {
    console.error('Startup migrations error:', error);
  }
};
runStartupMigrations();

// ─────────────────────────────────────────────────────────────
// Auto Cleanup: Hapus Akun Demo yang Kedaluwarsa Otomatis
// Berjalan saat startup + setiap 24 jam (via setInterval)
// ─────────────────────────────────────────────────────────────
const cleanupExpiredDemoUsers = async (): Promise<void> => {
  const now = new Date();
  console.log(`[CLEANUP] Running expired demo cleanup at ${now.toISOString()}`);

  try {
    // Cari semua GoogleUser yang BELUM dikonfirmasi DAN masa demo sudah lewat
    const expiredUsers = await mainPrisma.googleUser.findMany({
      where: {
        isConfirmed: false,
        demoExpiresAt: {
          lt: now  // Sudah lewat dari sekarang
        }
      },
      select: {
        email: true,
        userName: true,
        businessMode: true,
        demoExpiresAt: true,
        registeredAt: true
      }
    });

    if (expiredUsers.length === 0) {
      console.log('[CLEANUP] Tidak ada akun demo kedaluwarsa. Selesai.');
      return;
    }

    console.log(`[CLEANUP] Ditemukan ${expiredUsers.length} akun demo kedaluwarsa.`);
    const results: { email: string; status: string; reason?: string }[] = [];

    for (const user of expiredUsers) {
      const userEmail = user.email;
      let dbDropped = false;
      let userDeleted = false;

      // 1. Drop tenant database
      try {
        const dbName = getCleanTenantDbName(userEmail);

        // Tutup koneksi Prisma tenant jika ada
        if (tenantClients.has(dbName)) {
          const client = tenantClients.get(dbName)!;
          await client.$disconnect();
          tenantClients.delete(dbName);
        }

        // Putuskan semua koneksi aktif ke database ini sebelum DROP
        await mainPrisma.$executeRawUnsafe(`
          SELECT pg_terminate_backend(pg_stat_activity.pid)
          FROM pg_stat_activity
          WHERE pg_stat_activity.datname = '${dbName}'
            AND pid <> pg_backend_pid()
        `);

        // Cek apakah database memang ada
        const dbExists = await mainPrisma.$queryRawUnsafe<any[]>(
          `SELECT 1 FROM pg_database WHERE datname = $1`,
          dbName
        );

        if (dbExists && dbExists.length > 0) {
          await mainPrisma.$executeRawUnsafe(`DROP DATABASE "${dbName}"`);
          console.log(`[CLEANUP] ✅ Dropped database: ${dbName}`);
        } else {
          console.log(`[CLEANUP] ⚠️  Database ${dbName} tidak ditemukan (mungkin sudah dihapus).`);
        }
        dbDropped = true;
      } catch (err: any) {
        console.error(`[CLEANUP] ❌ Gagal drop database untuk ${userEmail}:`, err?.message || err);
      }

      // 2. Hapus GoogleUser dari main database
      try {
        await mainPrisma.googleUser.delete({ where: { email: userEmail } });
        userDeleted = true;
        console.log(`[CLEANUP] ✅ Deleted GoogleUser: ${userEmail}`);
      } catch (err: any) {
        console.error(`[CLEANUP] ❌ Gagal hapus GoogleUser ${userEmail}:`, err?.message || err);
      }

      // 3. Hapus PremiumUser jika ada yang terbuat dari demo ini (edge case)
      try {
        const premiumExists = await mainPrisma.premiumUser.findUnique({ where: { email: userEmail } });
        if (premiumExists && !premiumExists.tenantId) {
          // Hanya hapus jika belum punya tenantId (artinya belum dikonfirmasi betulan)
          await mainPrisma.premiumUser.delete({ where: { email: userEmail } });
          console.log(`[CLEANUP] ✅ Deleted stale PremiumUser: ${userEmail}`);
        }
      } catch (err: any) {
        // Tidak apa-apa jika tidak ada
      }

      results.push({
        email: userEmail,
        status: dbDropped && userDeleted ? 'CLEANED' : dbDropped ? 'DB_ONLY' : userDeleted ? 'USER_ONLY' : 'FAILED',
        reason: `Expired: ${user.demoExpiresAt?.toISOString()}`
      });
    }

    const cleaned = results.filter(r => r.status === 'CLEANED').length;
    const failed = results.filter(r => r.status === 'FAILED').length;
    console.log(`[CLEANUP] Selesai. ${cleaned} berhasil dibersihkan, ${failed} gagal.`);

    // Notifikasi ke admin jika ada yang dibersihkan
    const ownerEmail = process.env.ADMIN_BILLING_EMAIL || 'muhammadmuizz8@gmail.com';
    if (ownerEmail && results.length > 0) {
      const rows = results.map(r =>
        `<tr style="background:${r.status === 'CLEANED' ? '#f0fdf4' : '#fef2f2'}">
          <td style="padding:8px;border-bottom:1px solid #f1f5f9;">${r.email}</td>
          <td style="padding:8px;border-bottom:1px solid #f1f5f9;">${r.status}</td>
          <td style="padding:8px;border-bottom:1px solid #f1f5f9;color:#64748b;font-size:12px;">${r.reason}</td>
        </tr>`
      ).join('');

      await sendEmail(
        ownerEmail,
        `🧹 Auto-Cleanup: ${results.length} Akun Demo Kedaluwarsa Dibersihkan`,
        `
        <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px;background:#f8f9fa;border-radius:12px;">
          <div style="background:linear-gradient(135deg,#1e1b4b,#4c1d95);padding:24px;border-radius:12px;margin-bottom:20px;text-align:center;">
            <h1 style="color:white;margin:0;font-size:20px;">🧹 Laporan Auto-Cleanup Demo</h1>
            <p style="color:rgba(255,255,255,0.8);margin:8px 0 0;font-size:14px;">${now.toLocaleString('id-ID', { timeZone: 'Asia/Jakarta' })} WIB</p>
          </div>
          <div style="background:white;padding:20px;border-radius:12px;margin-bottom:16px;">
            <p style="color:#475569;margin:0 0 16px;">Berikut daftar akun demo yang telah otomatis dibersihkan karena tidak membayar setelah 2 hari:</p>
            <table style="width:100%;border-collapse:collapse;">
              <thead>
                <tr style="background:#f8fafc;">
                  <th style="padding:8px;text-align:left;color:#64748b;font-size:12px;border-bottom:2px solid #e2e8f0;">Email</th>
                  <th style="padding:8px;text-align:left;color:#64748b;font-size:12px;border-bottom:2px solid #e2e8f0;">Status</th>
                  <th style="padding:8px;text-align:left;color:#64748b;font-size:12px;border-bottom:2px solid #e2e8f0;">Keterangan</th>
                </tr>
              </thead>
              <tbody>${rows}</tbody>
            </table>
          </div>
          <p style="color:#94a3b8;font-size:12px;text-align:center;">Auto-cleanup berjalan otomatis setiap 24 jam. Tidak ada tindakan diperlukan.</p>
        </div>
        `
      ).catch(err => console.warn('[CLEANUP] Gagal kirim email laporan:', err));
    }
  } catch (err: any) {
    console.error('[CLEANUP] Fatal error during cleanup:', err?.message || err);
  }
};

// ─── Endpoint Internal: Trigger Cleanup Manual ───────────────
// GET /api/admin/cleanup-expired?secret=CLEANUP_SECRET
// Bisa dipanggil dari cron VPS atau secara manual
app.get('/api/admin/cleanup-expired', async (req, res) => {
  const { secret } = req.query as { secret: string };
  const expectedSecret = process.env.CLEANUP_SECRET || 'posbah_cleanup_secret_2026';

  if (secret !== expectedSecret) {
    return res.status(403).json({ error: 'Akses ditolak. Secret tidak valid.' });
  }

  try {
    await cleanupExpiredDemoUsers();
    res.json({ success: true, message: 'Cleanup selesai dijalankan. Cek log server untuk detail.' });
  } catch (err: any) {
    res.status(500).json({ error: 'Cleanup gagal', detail: err?.message });
  }
});

// ─── Jalankan Cleanup Saat Startup (dengan delay 30 detik) ───
setTimeout(() => {
  cleanupExpiredDemoUsers().catch(err =>
    console.error('[CLEANUP] Startup cleanup error:', err)
  );
}, 30 * 1000);

// ─── Jalankan Cleanup Setiap 24 Jam ──────────────────────────
const CLEANUP_INTERVAL_MS = 24 * 60 * 60 * 1000; // 24 jam
setInterval(() => {
  cleanupExpiredDemoUsers().catch(err =>
    console.error('[CLEANUP] Scheduled cleanup error:', err)
  );
}, CLEANUP_INTERVAL_MS);
console.log('[CLEANUP] Auto-cleanup dijadwalkan setiap 24 jam.');

// ═════════════════════════════════════════════════════════════════
// SISTEM LAPORAN BULANAN OWNER (Tanggal 7 setiap bulan)
// ═════════════════════════════════════════════════════════════════

const ADMIN_SECRET = process.env.CLEANUP_SECRET || 'posbah_cleanup_secret_2026';
const ADMIN_BASE_URL = process.env.APP_BASE_URL || 'https://www.zedmz.cloud';

// ─── Helper: Label paket bisnis ───────────────────────────────
const MODE_LABELS: Record<string, string> = {
  FNB: '🍹 Retail & F&B',
  RENTAL: '🚗 Rental Mobil',
  LAUNDRY: '🧺 Laundry',
  BMP: '🏭 Manufaktur (BMP)'
};

// ─── Eksekusi penghapusan owner yang sudah dijadwalkan ─────────
const executePendingOwnerDeletions = async (): Promise<void> => {
  const now = new Date();
  try {
    const pendingUsers = await mainPrisma.premiumUser.findMany({
      where: {
        deletionScheduledAt: { lt: now }
      },
      select: {
        email: true,
        name: true,
        tenantId: true
      }
    });

    if (pendingUsers.length === 0) return;

    console.log(`[BILLING] Ditemukan ${pendingUsers.length} akun owner untuk dihapus (jadwal terpenuhi).`);
    // Laporan eksekusi hapus HANYA ke admin utama
    const ownerEmail = process.env.ADMIN_BILLING_EMAIL || 'muhammadmuizz8@gmail.com';
    const deletedList: string[] = [];

    for (const user of pendingUsers) {
      const userEmail = user.email;
      try {
        // 1. Tutup dan drop tenant database
        const dbName = getCleanTenantDbName(userEmail);
        if (tenantClients.has(dbName)) {
          await tenantClients.get(dbName)!.$disconnect();
          tenantClients.delete(dbName);
        }
        await mainPrisma.$executeRawUnsafe(`
          SELECT pg_terminate_backend(pid) FROM pg_stat_activity
          WHERE datname = '${dbName}' AND pid <> pg_backend_pid()
        `);
        const dbCheck = await mainPrisma.$queryRawUnsafe<any[]>(
          `SELECT 1 FROM pg_database WHERE datname = $1`, dbName
        );
        if (dbCheck && dbCheck.length > 0) {
          await mainPrisma.$executeRawUnsafe(`DROP DATABASE "${dbName}"`);
          console.log(`[BILLING] ✅ Dropped: ${dbName}`);
        }

        // 2. Hapus PremiumUser
        await mainPrisma.premiumUser.delete({ where: { email: userEmail } });

        // 3. Hapus GoogleUser jika ada
        try {
          await mainPrisma.googleUser.delete({ where: { email: userEmail } });
        } catch { /* tidak masalah jika tidak ada */ }

        deletedList.push(`${user.name} (${userEmail})`);
        console.log(`[BILLING] ✅ Deleted owner account: ${userEmail}`);
      } catch (err: any) {
        console.error(`[BILLING] ❌ Gagal hapus ${userEmail}:`, err?.message);
      }
    }

    // Notifikasi ke admin
    if (ownerEmail && deletedList.length > 0) {
      const rows = deletedList.map(d => `<li style="padding:4px 0;">${d}</li>`).join('');
      await sendEmail(
        ownerEmail,
        `🗑️ Eksekusi Hapus Owner: ${deletedList.length} Akun Dihapus`,
        `<div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px;background:#f8f9fa;border-radius:12px;">
          <div style="background:linear-gradient(135deg,#7f1d1d,#dc2626);padding:24px;border-radius:12px;margin-bottom:20px;text-align:center;">
            <h1 style="color:white;margin:0;font-size:20px;">🗑️ Eksekusi Penghapusan Owner</h1>
            <p style="color:rgba(255,255,255,0.8);margin:8px 0 0;font-size:14px;">${now.toLocaleString('id-ID',{timeZone:'Asia/Jakarta'})} WIB</p>
          </div>
          <div style="background:white;padding:20px;border-radius:12px;">
            <p style="color:#475569;">Akun berikut telah dihapus permanen beserta seluruh data (produk, karyawan, transaksi, dll):</p>
            <ul style="color:#1e293b;line-height:1.8;">${rows}</ul>
          </div>
        </div>`
      ).catch(console.warn);
    }
  } catch (err: any) {
    console.error('[BILLING] Fatal error executePendingOwnerDeletions:', err?.message);
  }
};

// ─── Kirim Laporan Bulanan Owner ───────────────────────────────
const sendMonthlyOwnerReport = async (): Promise<void> => {
  // Laporan billing HANYA ke admin utama, bukan ke semua OWNER_EMAIL
  const billingEmail = process.env.ADMIN_BILLING_EMAIL || 'muhammadmuizz8@gmail.com';
  if (!billingEmail) {
    console.warn('[BILLING] ADMIN_BILLING_EMAIL tidak diset, laporan tidak dikirim.');
    return;
  }

  const now = new Date();
  const bulanTahun = now.toLocaleString('id-ID', { month: 'long', year: 'numeric', timeZone: 'Asia/Jakarta' });
  console.log(`[BILLING] Mengirim laporan bulanan owner untuk ${bulanTahun}...`);

  try {
    // Ambil semua PremiumUser (owner)
    const allOwners = await mainPrisma.premiumUser.findMany({
      where: { role: 'OWNER', tenantId: null },
      orderBy: { registeredAt: 'asc' }
    });

    // Ambil businessMode dari GoogleUser
    const googleUsers = await mainPrisma.googleUser.findMany({
      select: { email: true, businessMode: true }
    });
    const modeMap = new Map(googleUsers.map(g => [g.email, g.businessMode]));

    if (allOwners.length === 0) {
      console.log('[BILLING] Tidak ada owner premium. Laporan tidak dikirim.');
      return;
    }

    const rows = allOwners.map((owner, idx) => {
      const mode = modeMap.get(owner.email) || 'FNB';
      const modeLabel = MODE_LABELS[mode] || mode;
      const registeredAt = owner.registeredAt.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' });
      const lastPaid = owner.lastPaymentConfirmedAt
        ? owner.lastPaymentConfirmedAt.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
        : '<span style="color:#dc2626;font-weight:bold;">Belum pernah</span>';
      const deletionWarning = owner.deletionScheduledAt
        ? `<span style="color:#dc2626;font-weight:bold;">⚠️ Dihapus ${owner.deletionScheduledAt.toLocaleDateString('id-ID',{day:'2-digit',month:'short',year:'numeric'})}</span>`
        : '<span style="color:#16a34a;">✅ Aktif</span>';

      const paidUrl   = `${ADMIN_BASE_URL}/api/admin/owner-paid?secret=${ADMIN_SECRET}&email=${encodeURIComponent(owner.email)}`;
      const deleteUrl = `${ADMIN_BASE_URL}/api/admin/owner-delete?secret=${ADMIN_SECRET}&email=${encodeURIComponent(owner.email)}`;

      const rowBg = idx % 2 === 0 ? '#ffffff' : '#f8fafc';
      return `
        <tr style="background:${rowBg};">
          <td style="padding:10px 8px;border-bottom:1px solid #f1f5f9;font-weight:bold;color:#1e293b;">${idx + 1}</td>
          <td style="padding:10px 8px;border-bottom:1px solid #f1f5f9;color:#1e293b;">${owner.name}</td>
          <td style="padding:10px 8px;border-bottom:1px solid #f1f5f9;color:#475569;font-size:13px;">${owner.email}</td>
          <td style="padding:10px 8px;border-bottom:1px solid #f1f5f9;color:#475569;font-size:13px;">${owner.whatsapp || '-'}</td>
          <td style="padding:10px 8px;border-bottom:1px solid #f1f5f9;font-size:12px;">${modeLabel}</td>
          <td style="padding:10px 8px;border-bottom:1px solid #f1f5f9;font-size:12px;color:#64748b;">${registeredAt}</td>
          <td style="padding:10px 8px;border-bottom:1px solid #f1f5f9;font-size:12px;">${lastPaid}</td>
          <td style="padding:10px 8px;border-bottom:1px solid #f1f5f9;font-size:12px;">${deletionWarning}</td>
          <td style="padding:10px 8px;border-bottom:1px solid #f1f5f9;white-space:nowrap;">
            <a href="${paidUrl}"
               style="display:inline-block;padding:6px 12px;background:linear-gradient(135deg,#10b981,#059669);color:white;text-decoration:none;border-radius:6px;font-size:12px;font-weight:bold;margin-right:4px;">
              ✅ Sudah Bayar
            </a>
            <a href="${deleteUrl}"
               style="display:inline-block;padding:6px 12px;background:linear-gradient(135deg,#ef4444,#dc2626);color:white;text-decoration:none;border-radius:6px;font-size:12px;font-weight:bold;">
              🗑️ Hapus
            </a>
          </td>
        </tr>`;
    }).join('');

    await sendEmail(
      billingEmail,
      `📋 Laporan Bulanan Owner POSBah — ${bulanTahun} (${allOwners.length} Owner)`,
      // Email ini HANYA dikirim ke admin (muhammadmuizz8@gmail.com), bukan ke owner
      `<div style="font-family:Arial,sans-serif;max-width:900px;margin:auto;padding:20px;background:#f8f9fa;border-radius:12px;">
        <div style="background:linear-gradient(135deg,#1e1b4b,#4c1d95);padding:28px;border-radius:12px;margin-bottom:20px;text-align:center;">
          <h1 style="color:white;margin:0;font-size:22px;">📋 Laporan Bulanan Owner POSBah</h1>
          <p style="color:rgba(255,255,255,0.8);margin:8px 0 0;font-size:14px;">${bulanTahun} · ${allOwners.length} Owner Terdaftar</p>
        </div>

        <div style="background:white;padding:20px;border-radius:12px;margin-bottom:16px;overflow-x:auto;">
          <p style="color:#475569;margin:0 0 16px;font-size:14px;">
            Klik <strong style="color:#10b981;">✅ Sudah Bayar</strong> untuk mengkonfirmasi pembayaran bulan ini.<br>
            Klik <strong style="color:#dc2626;">🗑️ Hapus</strong> untuk menjadwalkan penghapusan akun dalam <strong>7 hari</strong> (owner akan diberitahu via email).
          </p>
          <table style="width:100%;border-collapse:collapse;min-width:700px;">
            <thead>
              <tr style="background:linear-gradient(135deg,#1e1b4b,#4c1d95);">
                <th style="padding:10px 8px;text-align:left;color:white;font-size:12px;">#</th>
                <th style="padding:10px 8px;text-align:left;color:white;font-size:12px;">Nama</th>
                <th style="padding:10px 8px;text-align:left;color:white;font-size:12px;">Email</th>
                <th style="padding:10px 8px;text-align:left;color:white;font-size:12px;">WhatsApp</th>
                <th style="padding:10px 8px;text-align:left;color:white;font-size:12px;">Paket</th>
                <th style="padding:10px 8px;text-align:left;color:white;font-size:12px;">Tgl Daftar</th>
                <th style="padding:10px 8px;text-align:left;color:white;font-size:12px;">Terakhir Bayar</th>
                <th style="padding:10px 8px;text-align:left;color:white;font-size:12px;">Status</th>
                <th style="padding:10px 8px;text-align:left;color:white;font-size:12px;">Aksi</th>
              </tr>
            </thead>
            <tbody>${rows}</tbody>
          </table>
        </div>

        <div style="background:#fffbeb;border:1px solid #fcd34d;border-radius:8px;padding:14px;margin-bottom:12px;">
          <p style="margin:0;color:#92400e;font-size:13px;">
            ⚠️ <strong>Perhatian:</strong> Tombol Hapus akan menjadwalkan penghapusan 7 hari ke depan.
            Owner akan menerima email peringatan. Untuk membatalkan, klik Sudah Bayar sebelum tanggal eksekusi.
          </p>
        </div>

        <p style="color:#94a3b8;font-size:11px;text-align:center;">
          Email ini dikirim otomatis setiap tanggal 7 oleh sistem POSBah.
          <a href="${ADMIN_BASE_URL}/api/admin/send-monthly-report?secret=${ADMIN_SECRET}" style="color:#6366f1;">Kirim ulang laporan</a>
        </p>
      </div>`
    );

    console.log(`[BILLING] ✅ Laporan bulanan berhasil dikirim ke ${billingEmail}`);
  } catch (err: any) {
    console.error('[BILLING] ❌ Gagal kirim laporan bulanan:', err?.message);
  }
};

// ─── Endpoint: Owner Sudah Bayar ──────────────────────────────
app.get('/api/admin/owner-paid', async (req, res) => {
  const { secret, email } = req.query as { secret: string; email: string };
  if (secret !== ADMIN_SECRET) return res.status(403).send('<h1>Akses ditolak</h1>');
  if (!email) return res.status(400).send('<h1>Email wajib diisi</h1>');

  try {
    const user = await mainPrisma.premiumUser.findUnique({ where: { email: decodeURIComponent(email) } });
    if (!user || user.role !== 'OWNER' || user.tenantId !== null) return res.status(404).send('<h1>Owner tidak ditemukan</h1>');

    await mainPrisma.premiumUser.update({
      where: { email: decodeURIComponent(email) },
      data: {
        lastPaymentConfirmedAt: new Date(),
        deletionScheduledAt: null  // Batalkan hapus jika ada
      }
    });

    // Kirim notifikasi ke owner
    const ownerName = user.name || user.email;
    const now = new Date().toLocaleString('id-ID', { timeZone: 'Asia/Jakarta' });
    await sendEmail(
      user.email,
      '✅ Pembayaran POSBah Anda Dikonfirmasi',
      `<div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px;background:#f8f9fa;border-radius:12px;">
        <div style="background:linear-gradient(135deg,#064e3b,#10b981);padding:28px;border-radius:12px;margin-bottom:20px;text-align:center;">
          <h1 style="color:white;margin:0;font-size:22px;">✅ Pembayaran Dikonfirmasi</h1>
        </div>
        <div style="background:white;padding:24px;border-radius:12px;">
          <p style="color:#1e293b;">Halo <strong>${ownerName}</strong>,</p>
          <p style="color:#475569;">Pembayaran langganan POSBah Anda telah dikonfirmasi pada <strong>${now} WIB</strong>.</p>
          <p style="color:#475569;">Akun Anda tetap aktif dan dapat digunakan seperti biasa. Terima kasih!</p>
          <div style="text-align:center;margin-top:20px;">
            <a href="${ADMIN_BASE_URL}" style="display:inline-block;padding:12px 24px;background:linear-gradient(135deg,#10b981,#059669);color:white;text-decoration:none;border-radius:10px;font-weight:bold;">🔐 Buka Aplikasi</a>
          </div>
        </div>
      </div>`
    ).catch(console.warn);

    res.send(`<!DOCTYPE html><html lang="id"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
      <title>Pembayaran Dikonfirmasi</title>
      <style>body{font-family:Arial,sans-serif;background:linear-gradient(135deg,#064e3b,#10b981);min-height:100vh;display:flex;align-items:center;justify-content:center;margin:0;}
      .card{background:white;border-radius:20px;padding:40px;max-width:480px;width:90%;text-align:center;box-shadow:0 20px 60px rgba(0,0,0,0.3);}
      .icon{font-size:64px;margin-bottom:16px;}.title{font-size:22px;font-weight:bold;color:#15803d;}.sub{color:#64748b;margin-top:8px;}</style></head>
      <body><div class="card">
        <div class="icon">✅</div>
        <h1 class="title">Pembayaran Dikonfirmasi!</h1>
        <p class="sub"><strong>${user.name}</strong><br>${user.email}</p>
        <p class="sub">Status bayar diperbarui. Email notifikasi sudah dikirim ke owner.</p>
      </div></body></html>`);
  } catch (err: any) {
    res.status(500).send(`<h1>Error: ${err?.message}</h1>`);
  }
});

// ─── Endpoint: Jadwalkan Hapus Owner ──────────────────────────
app.get('/api/admin/owner-delete', async (req, res) => {
  const { secret, email } = req.query as { secret: string; email: string };
  if (secret !== ADMIN_SECRET) return res.status(403).send('<h1>Akses ditolak</h1>');
  if (!email) return res.status(400).send('<h1>Email wajib diisi</h1>');

  try {
    const cleanEmail = decodeURIComponent(email);
    const user = await mainPrisma.premiumUser.findUnique({ where: { email: cleanEmail } });
    if (!user || user.role !== 'OWNER' || user.tenantId !== null) return res.status(404).send('<h1>Owner tidak ditemukan</h1>');

    // Sudah dijadwalkan?
    if (user.deletionScheduledAt && user.deletionScheduledAt > new Date()) {
      const tglHapus = user.deletionScheduledAt.toLocaleString('id-ID', { timeZone: 'Asia/Jakarta' });
      return res.send(`<!DOCTYPE html><html lang="id"><head><meta charset="UTF-8"><title>Sudah Dijadwalkan</title>
        <style>body{font-family:Arial,sans-serif;background:#fef2f2;min-height:100vh;display:flex;align-items:center;justify-content:center;margin:0;}
        .card{background:white;border-radius:20px;padding:40px;max-width:480px;width:90%;text-align:center;}</style></head>
        <body><div class="card">
          <div style="font-size:48px;">⚠️</div>
          <h1 style="color:#dc2626;">Sudah Dijadwalkan</h1>
          <p>${cleanEmail} sudah dijadwalkan dihapus pada:<br><strong>${tglHapus} WIB</strong></p>
          <a href="${ADMIN_BASE_URL}/api/admin/owner-paid?secret=${ADMIN_SECRET}&email=${encodeURIComponent(cleanEmail)}"
             style="display:inline-block;margin-top:16px;padding:12px 24px;background:#10b981;color:white;text-decoration:none;border-radius:10px;font-weight:bold;">
            Batalkan &amp; Konfirmasi Bayar
          </a>
        </div></body></html>`);
    }

    const deletionDate = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000); // +7 hari
    await mainPrisma.premiumUser.update({
      where: { email: cleanEmail },
      data: { deletionScheduledAt: deletionDate }
    });

    const tglHapus = deletionDate.toLocaleString('id-ID', { timeZone: 'Asia/Jakarta' });
    const ownerName = user.name || cleanEmail;

    // Kirim email peringatan ke owner
    const cancelUrl = `${ADMIN_BASE_URL}/api/admin/owner-cancel-delete?secret=${ADMIN_SECRET}&email=${encodeURIComponent(cleanEmail)}`;
    await sendEmail(
      cleanEmail,
      '⚠️ Akun POSBah Anda Akan Dihapus — Harap Segera Hubungi Admin',
      `<div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px;background:#f8f9fa;border-radius:12px;">
        <div style="background:linear-gradient(135deg,#7f1d1d,#dc2626);padding:28px;border-radius:12px;margin-bottom:20px;text-align:center;">
          <h1 style="color:white;margin:0;font-size:20px;">⚠️ Peringatan: Akun Akan Dihapus</h1>
        </div>
        <div style="background:white;padding:24px;border-radius:12px;">
          <p style="color:#1e293b;">Halo <strong>${ownerName}</strong>,</p>
          <p style="color:#475569;">Akun POSBah Anda (<strong>${cleanEmail}</strong>) dijadwalkan untuk dihapus permanen pada:</p>
          <div style="background:#fef2f2;border:2px solid #dc2626;border-radius:10px;padding:16px;text-align:center;margin:16px 0;">
            <span style="font-size:20px;font-weight:bold;color:#dc2626;">${tglHapus} WIB</span>
          </div>
          <p style="color:#475569;">⚠️ <strong>Semua data Anda akan dihapus permanen</strong> termasuk: data karyawan, produk, transaksi, pelanggan, dan semua riwayat usaha.</p>
          <p style="color:#475569;">Jika ini kesalahan atau Anda ingin melanjutkan langganan, segera hubungi admin:</p>
          <div style="background:#f8fafc;border-radius:8px;padding:16px;margin:16px 0;">
            <p style="margin:4px 0;color:#1e293b;">📞 WhatsApp: <strong>0812-3456-7890</strong></p>
            <p style="margin:4px 0;color:#1e293b;">📧 Email: <strong>muhammadmuizz8@gmail.com</strong></p>
          </div>
          <div style="text-align:center;margin-top:20px;">
            <a href="https://wa.me/628123456789?text=Halo Admin, saya ${encodeURIComponent(ownerName)} ingin melanjutkan langganan POSBah. Email: ${encodeURIComponent(cleanEmail)}"
               style="display:inline-block;padding:12px 24px;background:#25d366;color:white;text-decoration:none;border-radius:10px;font-weight:bold;">
              💬 Hubungi via WhatsApp
            </a>
          </div>
        </div>
      </div>`
    ).catch(console.warn);

    // Notifikasi ke admin utama saja
    const ownerEmailAdmin = process.env.ADMIN_BILLING_EMAIL || 'muhammadmuizz8@gmail.com';
    if (ownerEmailAdmin) {
      await sendEmail(
        ownerEmailAdmin,
        `🗑️ Penghapusan Dijadwalkan: ${ownerName} (${cleanEmail})`,
        `<div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px;background:#f8f9fa;border-radius:12px;">
          <p>Akun <strong>${cleanEmail}</strong> dijadwalkan dihapus pada <strong>${tglHapus} WIB</strong>.</p>
          <p>Email peringatan sudah dikirim ke owner.</p>
          <a href="${ADMIN_BASE_URL}/api/admin/owner-paid?secret=${ADMIN_SECRET}&email=${encodeURIComponent(cleanEmail)}"
             style="display:inline-block;padding:10px 20px;background:#10b981;color:white;text-decoration:none;border-radius:8px;font-weight:bold;">
            Batalkan &amp; Konfirmasi Bayar
          </a>
        </div>`
      ).catch(console.warn);
    }

    res.send(`<!DOCTYPE html><html lang="id"><head><meta charset="UTF-8"><title>Penghapusan Dijadwalkan</title>
      <style>body{font-family:Arial,sans-serif;background:linear-gradient(135deg,#7f1d1d,#dc2626);min-height:100vh;display:flex;align-items:center;justify-content:center;margin:0;}
      .card{background:white;border-radius:20px;padding:40px;max-width:500px;width:90%;text-align:center;box-shadow:0 20px 60px rgba(0,0,0,0.3);}
      .icon{font-size:64px;margin-bottom:12px;}.title{font-size:20px;font-weight:bold;color:#dc2626;}.sub{color:#64748b;margin:8px 0;}
      .date{background:#fef2f2;border:2px solid #dc2626;border-radius:10px;padding:12px;margin:16px 0;font-size:18px;font-weight:bold;color:#dc2626;}
      .btn{display:inline-block;margin-top:16px;padding:12px 24px;background:#10b981;color:white;text-decoration:none;border-radius:10px;font-weight:bold;font-size:14px;}
      </style></head>
      <body><div class="card">
        <div class="icon">🗑️</div>
        <h1 class="title">Penghapusan Dijadwalkan</h1>
        <p class="sub"><strong>${user.name}</strong><br>${cleanEmail}</p>
        <div class="date">Dihapus: ${tglHapus} WIB</div>
        <p class="sub" style="font-size:13px;">Email peringatan sudah dikirim ke owner.<br>Klik di bawah jika ingin membatalkan.</p>
        <a class="btn" href="${ADMIN_BASE_URL}/api/admin/owner-paid?secret=${ADMIN_SECRET}&email=${encodeURIComponent(cleanEmail)}">
          ✅ Batalkan &amp; Konfirmasi Bayar
        </a>
      </div></body></html>`);
  } catch (err: any) {
    res.status(500).send(`<h1>Error: ${err?.message}</h1>`);
  }
});

// ─── Endpoint: Batalkan Hapus Owner ───────────────────────────
app.get('/api/admin/owner-cancel-delete', async (req, res) => {
  const { secret, email } = req.query as { secret: string; email: string };
  if (secret !== ADMIN_SECRET) return res.status(403).send('<h1>Akses ditolak</h1>');
  if (!email) return res.status(400).send('<h1>Email wajib diisi</h1>');

  try {
    const cleanEmail = decodeURIComponent(email);
    const user = await mainPrisma.premiumUser.findUnique({ where: { email: cleanEmail } });
    if (!user || user.role !== 'OWNER' || user.tenantId !== null) return res.status(404).send('<h1>Owner tidak ditemukan</h1>');
    await mainPrisma.premiumUser.update({
      where: { email: cleanEmail },
      data: { deletionScheduledAt: null }
    });
    res.send(`<!DOCTYPE html><html lang="id"><head><meta charset="UTF-8"><title>Hapus Dibatalkan</title>
      <style>body{font-family:Arial,sans-serif;background:linear-gradient(135deg,#064e3b,#10b981);min-height:100vh;display:flex;align-items:center;justify-content:center;margin:0;}
      .card{background:white;border-radius:20px;padding:40px;max-width:480px;width:90%;text-align:center;}</style></head>
      <body><div class="card">
        <div style="font-size:64px;">✅</div>
        <h1 style="color:#15803d;">Penghapusan Dibatalkan</h1>
        <p style="color:#64748b;">${cleanEmail}</p>
        <p style="color:#475569;">Jadwal hapus sudah dibatalkan. Akun tetap aktif.</p>
      </div></body></html>`);
  } catch (err: any) {
    res.status(500).send(`<h1>Error: ${err?.message}</h1>`);
  }
});

// ─── Endpoint: Trigger Manual Laporan Bulanan ─────────────────
app.get('/api/admin/send-monthly-report', async (req, res) => {
  const { secret } = req.query as { secret: string };
  if (secret !== ADMIN_SECRET) return res.status(403).json({ error: 'Akses ditolak' });
  try {
    await sendMonthlyOwnerReport();
    res.json({ success: true, message: 'Laporan bulanan berhasil dikirim.' });
  } catch (err: any) {
    res.status(500).json({ error: 'Gagal kirim laporan', detail: err?.message });
  }
});

// ─── Cron: Tanggal 7 Setiap Bulan ─────────────────────────────
let lastReportMonth = -1; // -1 = belum pernah kirim session ini

const checkAndSendMonthlyReport = async () => {
  const now = new Date();
  const jakartaTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Jakarta' }));
  const currentDay   = jakartaTime.getDate();
  const currentMonth = jakartaTime.getMonth(); // 0-11

  if (currentDay === 7 && currentMonth !== lastReportMonth) {
    lastReportMonth = currentMonth;
    console.log('[BILLING] Tanggal 7 terdeteksi! Mengirim laporan bulanan...');
    await sendMonthlyOwnerReport().catch(err =>
      console.error('[BILLING] Error kirim laporan bulanan:', err)
    );
    // Juga jalankan eksekusi hapus yang sudah jatuh tempo
    await executePendingOwnerDeletions().catch(err =>
      console.error('[BILLING] Error eksekusi pending deletions:', err)
    );
  }
};

// Cek setiap jam
setInterval(checkAndSendMonthlyReport, 60 * 60 * 1000);
console.log('[BILLING] Laporan bulanan dijadwalkan setiap tanggal 7.');

// Eksekusi pending deletions juga ikut cek harian (digabung cleanup yang sudah ada)
// Jalankan sekali saat startup dengan delay 60 detik
setTimeout(() => {
  executePendingOwnerDeletions().catch(err =>
    console.error('[BILLING] Startup pending deletion check error:', err)
  );
}, 60 * 1000);

app.listen(port, () => {
  console.log(`Server is running on port ${port}`);
});
