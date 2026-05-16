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
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const client_1 = require("@prisma/client");
const prisma = new client_1.PrismaClient();
const app = (0, express_1.default)();
const port = process.env.PORT || 3001;
app.use((0, cors_1.default)());
app.use(express_1.default.json());
// Basic sanity check
app.get('/', (req, res) => {
    res.send('POSBah API is running');
});
// Products
app.get('/api/products', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    const products = yield prisma.product.findMany();
    res.json(products);
}));
app.post('/api/products', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { name, price, stock, image } = req.body;
        const product = yield prisma.product.create({
            data: { name, price: Number(price), stock: Number(stock), image }
        });
        res.json(product);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to create product' });
    }
}));
app.put('/api/products/:id', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const { name, price, stock, image } = req.body;
        const product = yield prisma.product.update({
            where: { id: Number(id) },
            data: { name, price: Number(price), stock: Number(stock), image }
        });
        res.json(product);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to update product' });
    }
}));
app.delete('/api/products/:id', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        yield prisma.product.delete({ where: { id: Number(id) } });
        res.json({ success: true });
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to delete product' });
    }
}));
app.get('/api/transactions', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
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
app.post('/api/transactions', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { items, total, discount, paymentMethod, type, customerId, date } = req.body;
        // items = [{ productId, quantity, price, discount }]
        // Find admin for demo (in reality, from auth token)
        const employee = yield prisma.employee.findFirst();
        const transaction = yield prisma.transaction.create({
            data: {
                receiptNumber: `INV-${Date.now()}`,
                total: Number(total),
                discount: Number(discount || 0),
                paymentMethod,
                type: type || 'SALES',
                date: date ? new Date(date) : new Date(),
                employeeId: employee.id,
                customerId: customerId || null,
                items: {
                    create: items.map((item) => ({
                        productId: item.productId,
                        quantity: item.quantity,
                        price: item.price,
                        discount: item.discount || 0,
                    }))
                }
            },
            include: { items: true }
        });
        // Update stock
        for (const item of items) {
            yield prisma.product.update({
                where: { id: item.productId },
                data: { stock: { decrement: item.quantity } }
            });
        }
        res.json(transaction);
    }
    catch (error) {
        console.error(error);
        res.status(400).json({ error: 'Failed to process transaction' });
    }
}));
// Seed Initial Data (for demo)
app.post('/api/seed', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
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
// Customers
app.get('/api/customers', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const customers = yield prisma.customer.findMany({
            orderBy: { name: 'asc' }
        });
        res.json(customers);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to fetch customers' });
    }
}));
app.post('/api/customers', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { name, phone, address } = req.body;
        const customer = yield prisma.customer.create({
            data: { name, phone, address }
        });
        res.json(customer);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to create customer' });
    }
}));
app.put('/api/customers/:id', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
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
app.delete('/api/customers/:id', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        yield prisma.customer.delete({ where: { id: Number(id) } });
        res.json({ success: true });
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to delete customer' });
    }
}));
// Employees
app.get('/api/employees', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const employees = yield prisma.employee.findMany({
            orderBy: { name: 'asc' }
        });
        res.json(employees);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to fetch employees' });
    }
}));
app.post('/api/employees', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const count = yield prisma.employee.count();
        if (count >= 10) {
            return res.status(400).json({ error: 'Maksimal 10 karyawan telah tercapai' });
        }
        const { name, role, pin } = req.body;
        const employee = yield prisma.employee.create({
            data: { name, role: role || 'CASHIER', pin }
        });
        res.json(employee);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to create employee' });
    }
}));
app.put('/api/employees/:id', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const { name, role, pin } = req.body;
        const employee = yield prisma.employee.update({
            where: { id: Number(id) },
            data: { name, role, pin }
        });
        res.json(employee);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to update employee' });
    }
}));
app.delete('/api/employees/:id', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        yield prisma.employee.delete({ where: { id: Number(id) } });
        res.json({ success: true });
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to delete employee' });
    }
}));
// Finance (Hutang, Piutang, Pengeluaran Usaha)
app.get('/api/finances', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
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
app.post('/api/finances', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { type, amount, description, date, status, customerId } = req.body;
        const finance = yield prisma.finance.create({
            data: {
                type, // PAYABLE, RECEIVABLE, EXPENSE
                amount: Number(amount),
                description,
                date: date ? new Date(date) : new Date(),
                status: status || 'PENDING',
                customerId: customerId ? Number(customerId) : null,
            }
        });
        res.json(finance);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to create finance record' });
    }
}));
app.put('/api/finances/:id', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        const { type, amount, description, date, status, customerId } = req.body;
        const finance = yield prisma.finance.update({
            where: { id: Number(id) },
            data: {
                type,
                amount: Number(amount),
                description,
                date: date ? new Date(date) : undefined,
                status,
                customerId: customerId ? Number(customerId) : null,
            }
        });
        res.json(finance);
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to update finance record' });
    }
}));
app.delete('/api/finances/:id', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.params;
        yield prisma.finance.delete({ where: { id: Number(id) } });
        res.json({ success: true });
    }
    catch (error) {
        res.status(400).json({ error: 'Failed to delete finance record' });
    }
}));
// Rekap Laporan
app.get('/api/reports', (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const totalSales = yield prisma.transaction.aggregate({
            where: { type: 'SALES' },
            _sum: { total: true }
        });
        const expenses = yield prisma.finance.aggregate({
            where: { type: 'EXPENSE' },
            _sum: { amount: true }
        });
        const receivables = yield prisma.finance.aggregate({
            where: { type: 'RECEIVABLE', status: 'PENDING' },
            _sum: { amount: true }
        });
        const payables = yield prisma.finance.aggregate({
            where: { type: 'PAYABLE', status: 'PENDING' },
            _sum: { amount: true }
        });
        res.json({
            totalSales: totalSales._sum.total || 0,
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
app.listen(port, () => {
    console.log(`Server is running on port ${port}`);
});
