import express from 'express';
import cors from 'cors';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();
const app = express();
const port = process.env.PORT || 3001;

app.use(cors());
app.use(express.json());

// Basic sanity check
app.get('/', (req, res) => {
  res.send('POSBah API is running');
});

// Products
app.get('/api/products', async (req, res) => {
  const products = await prisma.product.findMany();
  res.json(products);
});

app.post('/api/products', async (req, res) => {
  try {
    const { name, price, stock, image } = req.body;
    const product = await prisma.product.create({
      data: { name, price: Number(price), stock: Number(stock), image }
    });
    res.json(product);
  } catch (error) {
    res.status(400).json({ error: 'Failed to create product' });
  }
});

app.put('/api/products/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { name, price, stock, image } = req.body;
    const product = await prisma.product.update({
      where: { id: Number(id) },
      data: { name, price: Number(price), stock: Number(stock), image }
    });
    res.json(product);
  } catch (error) {
    res.status(400).json({ error: 'Failed to update product' });
  }
});

app.delete('/api/products/:id', async (req, res) => {
  try {
    const { id } = req.params;
    await prisma.product.delete({ where: { id: Number(id) } });
    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Failed to delete product' });
  }
});

app.get('/api/transactions', async (req, res) => {
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

app.post('/api/transactions', async (req, res) => {
  try {
    const { items, total, discount, paymentMethod, type, customerId, date } = req.body;
    
    // items = [{ productId, quantity, price, discount }]
    // Find admin for demo (in reality, from auth token)
    let employee = await prisma.employee.findFirst();
    if (!employee) {
      employee = await prisma.employee.create({
        data: { name: 'Admin Default', role: 'ADMIN', pin: '1234' }
      });
    }

    const transaction = await prisma.transaction.create({
      data: {
        receiptNumber: `INV-${Date.now()}`,
        total: Number(total),
        discount: Number(discount || 0),
        paymentMethod,
        type: type || 'SALES',
        date: date ? new Date(date) : new Date(),
        employeeId: employee!.id,
        customerId: customerId || null,
        items: {
          create: items.map((item: any) => ({
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
      await prisma.product.update({
        where: { id: item.productId },
        data: { stock: { decrement: item.quantity } }
      });
    }

    res.json(transaction);
  } catch (error) {
    console.error(error);
    res.status(400).json({ error: 'Failed to process transaction' });
  }
});

// Seed Initial Data (for demo)
app.post('/api/seed', async (req, res) => {
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

// Customers
app.get('/api/customers', async (req, res) => {
  try {
    const customers = await prisma.customer.findMany({
      orderBy: { name: 'asc' }
    });
    res.json(customers);
  } catch (error) {
    res.status(400).json({ error: 'Failed to fetch customers' });
  }
});

app.post('/api/customers', async (req, res) => {
  try {
    const { name, phone, address } = req.body;
    const customer = await prisma.customer.create({
      data: { name, phone, address }
    });
    res.json(customer);
  } catch (error) {
    res.status(400).json({ error: 'Failed to create customer' });
  }
});

app.put('/api/customers/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { name, phone, address } = req.body;
    const customer = await prisma.customer.update({
      where: { id: Number(id) },
      data: { name, phone, address }
    });
    res.json(customer);
  } catch (error) {
    res.status(400).json({ error: 'Failed to update customer' });
  }
});

app.delete('/api/customers/:id', async (req, res) => {
  try {
    const { id } = req.params;
    await prisma.customer.delete({ where: { id: Number(id) } });
    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Failed to delete customer' });
  }
});

// Employees
app.get('/api/employees', async (req, res) => {
  try {
    const employees = await prisma.employee.findMany({
      orderBy: { name: 'asc' }
    });
    res.json(employees);
  } catch (error) {
    res.status(400).json({ error: 'Failed to fetch employees' });
  }
});

app.post('/api/employees', async (req, res) => {
  try {
    const count = await prisma.employee.count();
    if (count >= 10) {
      return res.status(400).json({ error: 'Maksimal 10 karyawan telah tercapai' });
    }
    
    const { name, role, pin } = req.body;
    const employee = await prisma.employee.create({
      data: { name, role: role || 'CASHIER', pin }
    });
    res.json(employee);
  } catch (error) {
    res.status(400).json({ error: 'Failed to create employee' });
  }
});

app.put('/api/employees/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { name, role, pin } = req.body;
    const employee = await prisma.employee.update({
      where: { id: Number(id) },
      data: { name, role, pin }
    });
    res.json(employee);
  } catch (error) {
    res.status(400).json({ error: 'Failed to update employee' });
  }
});

app.delete('/api/employees/:id', async (req, res) => {
  try {
    const { id } = req.params;
    await prisma.employee.delete({ where: { id: Number(id) } });
    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Failed to delete employee' });
  }
});

// Finance (Hutang, Piutang, Pengeluaran Usaha)
app.get('/api/finances', async (req, res) => {
  try {
    const finances = await prisma.finance.findMany({
      include: { customer: true },
      orderBy: { date: 'desc' }
    });
    res.json(finances);
  } catch (error) {
    res.status(400).json({ error: 'Failed to fetch finances' });
  }
});

app.post('/api/finances', async (req, res) => {
  try {
    const { type, amount, description, date, status, customerId } = req.body;
    const finance = await prisma.finance.create({
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
  } catch (error) {
    res.status(400).json({ error: 'Failed to create finance record' });
  }
});

app.put('/api/finances/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { type, amount, description, date, status, customerId } = req.body;
    const finance = await prisma.finance.update({
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
  } catch (error) {
    res.status(400).json({ error: 'Failed to update finance record' });
  }
});

app.delete('/api/finances/:id', async (req, res) => {
  try {
    const { id } = req.params;
    await prisma.finance.delete({ where: { id: Number(id) } });
    res.json({ success: true });
  } catch (error) {
    res.status(400).json({ error: 'Failed to delete finance record' });
  }
});

// Rekap Laporan
app.get('/api/reports', async (req, res) => {
  try {
    const totalSales = await prisma.transaction.aggregate({
      where: { type: 'SALES' },
      _sum: { total: true }
    });
    
    const expenses = await prisma.finance.aggregate({
      where: { type: 'EXPENSE' },
      _sum: { amount: true }
    });
    
    const receivables = await prisma.finance.aggregate({
      where: { type: 'RECEIVABLE', status: 'PENDING' },
      _sum: { amount: true }
    });
    
    const payables = await prisma.finance.aggregate({
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
  } catch (error) {
    console.error('Error generating report:', error);
    res.status(400).json({ error: 'Failed to generate report' });
  }
});

app.listen(port, () => {
  console.log(`Server is running on port ${port}`);
});
