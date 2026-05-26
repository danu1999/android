import axios, { getAdapter } from 'axios';

const isLocalDev = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') && window.location.port !== '';
const API_URL = import.meta.env.VITE_API_URL || (isLocalDev ? 'http://localhost:3001/api' : 'https://posbah.up.railway.app/api');

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Inject user ID, role, dan app mode ke setiap request agar backend bisa validasi akses & filter data
api.interceptors.request.use((config) => {
  try {
    const stored = localStorage.getItem('posbah_user');
    if (stored) {
      const user = JSON.parse(stored);
      if (user?.id !== undefined && user?.id !== null) config.headers['x-employee-id'] = String(user.id);
      if (user?.role) config.headers['x-employee-role'] = user.role;
    }
    const appMode = localStorage.getItem('posbah_app_mode') || 'FNB';
    config.headers['x-app-mode'] = appMode;
  } catch (_) {}
  return config;
});

// ─────────────────────────────────────────────────────────────
// Client-Side Simulated Database for Demo Mode
// ─────────────────────────────────────────────────────────────

const initDemoData = () => {
  const DEMO_VERSION_KEY = 'posbah_demo_version_v4';
  if (localStorage.getItem(DEMO_VERSION_KEY) !== 'true') {
    // Clear all demo-related localStorage entries to force fresh initialization
    for (let i = localStorage.length - 1; i >= 0; i--) {
      const key = localStorage.key(i);
      if (key && key.startsWith('posbah_demo_')) {
        localStorage.removeItem(key);
      }
    }
    localStorage.setItem(DEMO_VERSION_KEY, 'true');
  }

  const mode = localStorage.getItem('posbah_app_mode') || 'FNB';

  if (mode === 'RENTAL') {
    if (!localStorage.getItem(getDemoKey('posbah_demo_customers'))) {
      localStorage.setItem(getDemoKey('posbah_demo_customers'), JSON.stringify([
        { id: 501, name: 'Budi Santoso', phone: '08123456789', address: 'Jl. Merdeka No. 10, Bandung' },
        { id: 502, name: 'Siti Rahma', phone: '08567890123', address: 'Jl. Mawar No. 4, Cimahi' },
        { id: 503, name: 'Andi Wijaya', phone: '08789012345', address: 'Jl. Melati No. 15, Bandung' },
        { id: 504, name: 'Dewi Lestari', phone: '08991234567', address: 'Komp. Buah Batu Indah Reg-3' },
        { id: 505, name: 'Rian Hidayat', phone: '08215432765', address: 'Jl. Cihampelas No. 102' }
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_employees'))) {
      localStorage.setItem(getDemoKey('posbah_demo_employees'), JSON.stringify([
        { id: 1, name: 'Kasir Utama', role: 'KASIR', pin: '111111', salary: 2500000 },
        { id: 2, name: 'Admin Gudang', role: 'ADMIN', pin: '222222', salary: 3000000 },
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_cars'))) {
      localStorage.setItem(getDemoKey('posbah_demo_cars'), JSON.stringify([
        { id: 801, name: 'Toyota Avanza Veloz', plateNumber: 'D 1234 ABC', type: 'MPV', pricePerDay: 350000, status: 'AVAILABLE' },
        { id: 802, name: 'Mitsubishi Pajero Sport', plateNumber: 'D 8888 BOSS', type: 'SUV', pricePerDay: 800000, status: 'AVAILABLE' },
        { id: 803, name: 'Honda Brio RS', plateNumber: 'D 5678 XYZ', type: 'City Car', pricePerDay: 250000, status: 'RENTED' },
        { id: 804, name: 'Toyota Innova Zenix Hybrid', plateNumber: 'D 2026 HYB', type: 'MPV', pricePerDay: 650000, status: 'AVAILABLE' },
        { id: 805, name: 'Honda Civic RS Turbo', plateNumber: 'D 1999 CIV', type: 'Sedan', pricePerDay: 900000, status: 'AVAILABLE' },
        { id: 806, name: 'Daihatsu Sigra X', plateNumber: 'D 1742 SGR', type: 'Family Car', pricePerDay: 220000, status: 'AVAILABLE' },
        { id: 807, name: 'Suzuki Ertiga Hybrid', plateNumber: 'D 4321 ERT', type: 'MPV', pricePerDay: 300000, status: 'RENTED' },
        { id: 808, name: 'Hyundai Stargazer Prime', plateNumber: 'D 999 HND', type: 'MPV', pricePerDay: 400000, status: 'AVAILABLE' },
        { id: 809, name: 'Toyota Fortuner GR Sport', plateNumber: 'D 777 FTR', type: 'SUV', pricePerDay: 850000, status: 'AVAILABLE' },
        { id: 810, name: 'Honda HR-V SE', plateNumber: 'D 3020 HRV', type: 'Crossover', pricePerDay: 450000, status: 'AVAILABLE' },
        { id: 811, name: 'Toyota Alphard VIP', plateNumber: 'D 1 VIP', type: 'Luxury MPV', pricePerDay: 1800000, status: 'AVAILABLE' },
        { id: 812, name: 'Hyundai Ioniq 5 EV', plateNumber: 'D 2024 EV', type: 'Electric', pricePerDay: 750000, status: 'RENTED' },
        { id: 813, name: 'Toyota Calya G', plateNumber: 'D 1456 CAL', type: 'Family Car', pricePerDay: 200000, status: 'AVAILABLE' },
        { id: 814, name: 'Mitsubishi Xpander Ultimate', plateNumber: 'D 4321 XPD', type: 'LMPV', pricePerDay: 380000, status: 'AVAILABLE' },
        { id: 815, name: 'Wuling Air EV Lite', plateNumber: 'D 77 EV', type: 'Electric', pricePerDay: 300000, status: 'AVAILABLE' }
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_rentals'))) {
      localStorage.setItem(getDemoKey('posbah_demo_rentals'), JSON.stringify([
        { id: 901, carId: 803, car: { name: 'Honda Brio RS', plateNumber: 'D 5678 XYZ', pricePerDay: 250000 }, customerId: 501, customerName: 'Budi Santoso', startDate: new Date(Date.now() - 2 * 86400000).toISOString(), endDate: new Date(Date.now() + 1 * 86400000).toISOString(), totalPrice: 750000, status: 'ACTIVE', actualReturnDate: null, lateFee: 0, employeeId: 1 },
        { id: 902, carId: 801, car: { name: 'Toyota Avanza Veloz', plateNumber: 'D 1234 ABC', pricePerDay: 350000 }, customerId: 502, customerName: 'Siti Rahma', startDate: new Date(Date.now() - 5 * 86400000).toISOString(), endDate: new Date(Date.now() - 3 * 86400000).toISOString(), totalPrice: 700000, status: 'RETURNED', actualReturnDate: new Date(Date.now() - 3 * 86400000).toISOString(), lateFee: 0, employeeId: 1 },
        { id: 903, carId: 804, car: { name: 'Toyota Innova Zenix Hybrid', plateNumber: 'D 2026 HYB', pricePerDay: 650000 }, customerId: 503, customerName: 'Andi Wijaya', startDate: new Date(Date.now() - 4 * 86400000).toISOString(), endDate: new Date(Date.now() - 2 * 86400000).toISOString(), totalPrice: 1300000, status: 'RETURNED', actualReturnDate: new Date(Date.now() - 2 * 86400000).toISOString(), lateFee: 0, employeeId: 1 },
        { id: 904, carId: 807, car: { name: 'Suzuki Ertiga Hybrid', plateNumber: 'D 4321 ERT', pricePerDay: 300000 }, customerId: 504, customerName: 'Dewi Lestari', startDate: new Date(Date.now() - 3 * 86400000).toISOString(), endDate: new Date(Date.now() + 2 * 86400000).toISOString(), totalPrice: 1500000, status: 'ACTIVE', actualReturnDate: null, lateFee: 0, employeeId: 2 },
        { id: 905, carId: 809, car: { name: 'Toyota Fortuner GR Sport', plateNumber: 'D 777 FTR', pricePerDay: 850000 }, customerId: 505, customerName: 'Rian Hidayat', startDate: new Date(Date.now() - 6 * 86400000).toISOString(), endDate: new Date(Date.now() - 4 * 86400000).toISOString(), totalPrice: 1700000, status: 'RETURNED', actualReturnDate: new Date(Date.now() - 4 * 86400000).toISOString(), lateFee: 0, employeeId: 2 },
        { id: 906, carId: 805, car: { name: 'Honda Civic RS Turbo', plateNumber: 'D 1999 CIV', pricePerDay: 900000 }, customerId: 501, customerName: 'Budi Santoso', startDate: new Date(Date.now() - 3 * 86400000).toISOString(), endDate: new Date(Date.now() - 1 * 86400000).toISOString(), totalPrice: 1800000, status: 'RETURNED', actualReturnDate: new Date(Date.now() - 1 * 86400000).toISOString(), lateFee: 0, employeeId: 1 },
        { id: 907, carId: 811, car: { name: 'Toyota Alphard VIP', plateNumber: 'D 1 VIP', pricePerDay: 1800000 }, customerId: 503, customerName: 'Andi Wijaya', startDate: new Date(Date.now() - 4 * 86400000).toISOString(), endDate: new Date(Date.now() - 3 * 86400000).toISOString(), totalPrice: 1800000, status: 'RETURNED', actualReturnDate: new Date(Date.now() - 3 * 86400000).toISOString(), lateFee: 0, employeeId: 1 },
        { id: 908, carId: 812, car: { name: 'Hyundai Ioniq 5 EV', plateNumber: 'D 2024 EV', pricePerDay: 750000 }, customerId: 504, customerName: 'Dewi Lestari', startDate: new Date(Date.now() - 1 * 86400000).toISOString(), endDate: new Date(Date.now() + 2 * 86400000).toISOString(), totalPrice: 2250000, status: 'ACTIVE', actualReturnDate: null, lateFee: 0, employeeId: 2 },
        { id: 909, carId: 813, car: { name: 'Toyota Calya G', plateNumber: 'D 1456 CAL', pricePerDay: 200000 }, customerId: 502, customerName: 'Siti Rahma', startDate: new Date(Date.now() - 6 * 86400000).toISOString(), endDate: new Date(Date.now() - 4 * 86400000).toISOString(), totalPrice: 400000, status: 'RETURNED', actualReturnDate: new Date(Date.now() - 4 * 86400000).toISOString(), lateFee: 0, employeeId: 1 },
        { id: 910, carId: 814, car: { name: 'Mitsubishi Xpander Ultimate', plateNumber: 'D 4321 XPD', pricePerDay: 380000 }, customerId: 505, customerName: 'Rian Hidayat', startDate: new Date(Date.now() - 3 * 86400000).toISOString(), endDate: new Date(Date.now()).toISOString(), totalPrice: 1140000, status: 'RETURNED', actualReturnDate: new Date().toISOString(), lateFee: 0, employeeId: 2 }
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_finances'))) {
      localStorage.setItem(getDemoKey('posbah_demo_finances'), JSON.stringify([
        { id: 401, type: 'EXPENSE', amount: 1500000, description: 'Servis rutin Toyota Avanza (Demo)', date: new Date(Date.now() - 6 * 86400000).toISOString(), status: 'PAID' },
        { id: 402, type: 'EXPENSE', amount: 800000, description: 'Beli Ban Baru Honda Brio (Demo)', date: new Date(Date.now() - 9 * 86400000).toISOString(), status: 'PAID' },
        { id: 403, type: 'INCOME', amount: 700000, description: 'Pelunasan Sewa Toyota Avanza - Siti Rahma (Demo)', date: new Date(Date.now() - 3 * 86400000).toISOString(), status: 'PAID' },
        { id: 404, type: 'INCOME', amount: 1700000, description: 'Pelunasan Sewa Toyota Fortuner - Rian Hidayat (Demo)', date: new Date(Date.now() - 4 * 86400000).toISOString(), status: 'PAID' },
        { id: 405, type: 'INCOME', amount: 1300000, description: 'Pelunasan Sewa Toyota Innova - Andi Wijaya (Demo)', date: new Date(Date.now() - 2 * 86400000).toISOString(), status: 'PAID' },
        { id: 406, type: 'INCOME', amount: 1800000, description: 'Pelunasan Sewa Honda Civic RS - Budi Santoso (Demo)', date: new Date(Date.now() - 1 * 86400000).toISOString(), status: 'PAID' },
        { id: 407, type: 'RECEIVABLE', amount: 750000, description: 'Piutang Sewa Honda Brio RS - Budi Santoso (Demo)', date: new Date(Date.now() - 2 * 86400000).toISOString(), status: 'PENDING' },
        { id: 408, type: 'RECEIVABLE', amount: 1500000, description: 'Piutang Sewa Suzuki Ertiga - Dewi Lestari (Demo)', date: new Date(Date.now() - 3 * 86400000).toISOString(), status: 'PENDING' },
        { id: 409, type: 'EXPENSE', amount: 600000, description: 'Belanja BBM & Operasional Kantor (Demo)', date: new Date(Date.now() - 1 * 86400000).toISOString(), status: 'PAID' },
        { id: 410, type: 'EXPENSE', amount: 2500000, description: 'Gaji Bulanan Karyawan - Kasir Utama (Demo)', date: new Date().toISOString(), status: 'PAID' },
        { id: 411, type: 'EXPENSE', amount: 3000000, description: 'Gaji Bulanan Karyawan - Admin Gudang (Demo)', date: new Date().toISOString(), status: 'PAID' },
        { id: 412, type: 'INCOME', amount: 1800000, description: 'Pelunasan Sewa Toyota Alphard - Andi Wijaya (Demo)', date: new Date(Date.now() - 3 * 86400000).toISOString(), status: 'PAID' },
        { id: 413, type: 'RECEIVABLE', amount: 2250000, description: 'Piutang Sewa Hyundai Ioniq 5 - Dewi Lestari (Demo)', date: new Date(Date.now() - 1 * 86400000).toISOString(), status: 'PENDING' },
        { id: 414, type: 'INCOME', amount: 400000, description: 'Pelunasan Sewa Toyota Calya - Siti Rahma (Demo)', date: new Date(Date.now() - 4 * 86400000).toISOString(), status: 'PAID' },
        { id: 415, type: 'INCOME', amount: 1140000, description: 'Pelunasan Sewa Mitsubishi Xpander - Rian Hidayat (Demo)', date: new Date().toISOString(), status: 'PAID' }
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_logs'))) {
      localStorage.setItem(getDemoKey('posbah_demo_logs'), JSON.stringify([
        { id: 1, action: 'CREATE_RENTAL', description: 'Kasir Utama menyewakan mobil Honda Brio RS ke Budi Santoso', createdAt: new Date(Date.now() - 2 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 2, action: 'RETURN_CAR', description: 'Pengembalian mobil Toyota Avanza Veloz oleh Siti Rahma', createdAt: new Date(Date.now() - 3 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 3, action: 'RETURN_CAR', description: 'Pengembalian mobil Toyota Innova Zenix oleh Andi Wijaya', createdAt: new Date(Date.now() - 2 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 4, action: 'CREATE_RENTAL', description: 'Admin Gudang menyewakan mobil Suzuki Ertiga ke Dewi Lestari', createdAt: new Date(Date.now() - 3 * 86400000).toISOString(), employee: { name: 'Admin Gudang', role: 'ADMIN' } },
        { id: 5, action: 'RETURN_CAR', description: 'Pengembalian mobil Toyota Fortuner oleh Rian Hidayat', createdAt: new Date(Date.now() - 4 * 86400000).toISOString(), employee: { name: 'Admin Gudang', role: 'ADMIN' } },
        { id: 6, action: 'RETURN_CAR', description: 'Pengembalian mobil Honda Civic RS oleh Budi Santoso', createdAt: new Date(Date.now() - 1 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 7, action: 'CREATE_RENTAL', description: 'Kasir Utama menyewakan mobil Toyota Alphard VIP ke Andi Wijaya', createdAt: new Date(Date.now() - 4 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 8, action: 'RETURN_CAR', description: 'Pengembalian mobil Toyota Alphard VIP oleh Andi Wijaya', createdAt: new Date(Date.now() - 3 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 9, action: 'CREATE_RENTAL', description: 'Admin Gudang menyewakan mobil Hyundai Ioniq 5 EV ke Dewi Lestari', createdAt: new Date(Date.now() - 1 * 86400000).toISOString(), employee: { name: 'Admin Gudang', role: 'ADMIN' } },
        { id: 10, action: 'CREATE_RENTAL', description: 'Kasir Utama menyewakan mobil Toyota Calya G ke Siti Rahma', createdAt: new Date(Date.now() - 6 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 11, action: 'RETURN_CAR', description: 'Pengembalian mobil Toyota Calya G oleh Siti Rahma', createdAt: new Date(Date.now() - 4 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 12, action: 'CREATE_RENTAL', description: 'Admin Gudang menyewakan mobil Mitsubishi Xpander ke Rian Hidayat', createdAt: new Date(Date.now() - 3 * 86400000).toISOString(), employee: { name: 'Admin Gudang', role: 'ADMIN' } },
        { id: 13, action: 'RETURN_CAR', description: 'Pengembalian mobil Mitsubishi Xpander oleh Rian Hidayat', createdAt: new Date().toISOString(), employee: { name: 'Admin Gudang', role: 'ADMIN' } }
      ]));
    }
  } else if (mode === 'LAUNDRY') {
    if (!localStorage.getItem(getDemoKey('posbah_demo_customers'))) {
      localStorage.setItem(getDemoKey('posbah_demo_customers'), JSON.stringify([
        { id: 501, name: 'Budi Santoso', phone: '08123456789', address: 'Jl. Merdeka No. 10, Bandung' },
        { id: 502, name: 'Siti Rahma', phone: '08567890123', address: 'Jl. Mawar No. 4, Cimahi' },
        { id: 503, name: 'Andi Wijaya', phone: '08789012345', address: 'Jl. Melati No. 15, Bandung' },
        { id: 504, name: 'Dewi Lestari', phone: '08991234567', address: 'Komp. Buah Batu Indah Reg-3' },
        { id: 505, name: 'Rian Hidayat', phone: '08215432765', address: 'Jl. Cihampelas No. 102' }
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_employees'))) {
      localStorage.setItem(getDemoKey('posbah_demo_employees'), JSON.stringify([
        { id: 1, name: 'Kasir Utama', role: 'KASIR', pin: '111111', salary: 2500000 },
        { id: 2, name: 'Admin Gudang', role: 'ADMIN', pin: '222222', salary: 3000000 },
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_laundry_services'))) {
      localStorage.setItem(getDemoKey('posbah_demo_laundry_services'), JSON.stringify([
        { id: 1, kategori: 'Kiloan Setrika', proses: 'Cuci >> Kering >> Setrika', nama: 'Reguler', harga: 8000, satuan: 'Kg', waktu: '3 Hari', icon: '🧺' },
        { id: 2, kategori: 'Kiloan Setrika', proses: 'Cuci >> Kering >> Setrika', nama: 'Ekspress', harga: 12000, satuan: 'Kg', waktu: '24 Jam', icon: '⚡' },
        { id: 3, kategori: 'Kiloan Setrika', proses: 'Cuci >> Kering >> Setrika', nama: 'Kilat', harga: 16000, satuan: 'Kg', waktu: '6 Jam', icon: '🚀' },
        { id: 4, kategori: 'Cuci Selimut', proses: 'Cuci >> Kering >> Lipat', nama: 'Selimut Besar', harga: 15000, satuan: 'Pcs', waktu: '3 Hari', icon: '🛌' },
        { id: 5, kategori: 'Cuci Sprei', proses: 'Cuci >> Kering >> Lipat', nama: 'Sprei Double', harga: 12000, satuan: 'Pcs', waktu: '3 Hari', icon: '🛏️' },
        { id: 6, kategori: 'Barang Pendukung', proses: 'Pembelian Langsung', nama: 'Deterjen Wangi 50ml', harga: 3000, satuan: 'Pcs', waktu: 'Instan', icon: '🧴' },
        { id: 7, kategori: 'Barang Pendukung', proses: 'Pembelian Langsung', nama: 'Plastik Besar Ekstra', harga: 2000, satuan: 'Pcs', waktu: 'Instan', icon: '🛍️' }
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_laundry_orders'))) {
      localStorage.setItem(getDemoKey('posbah_demo_laundry_orders'), JSON.stringify([
        { id: 2001, receiptNumber: 'INV-LND-1716645600000', namaPelanggan: 'Budi Santoso', noHp: '08123456789', jenisLayanan: 'Kiloan Setrika', jenisLaundry: '- Kiloan Setrika - Reguler : 5 Kg (Rp 40.000)\n', totalHarga: 40000, statusBayar: 'Lunas', status: 'Diambil', tanggalMasuk: new Date(Date.now() - 4 * 86400000).toISOString(), selimut: 0, sprei: 0, boneka: 0, korden: 0, lokasi: 'Bandung', employeeId: 1, customerId: 501 },
        { id: 2002, receiptNumber: 'INV-LND-1716732000000', namaPelanggan: 'Siti Rahma', noHp: '08567890123', jenisLayanan: 'Cuci Selimut', jenisLaundry: '- Selimut Besar : 2 Pcs (Rp 30.000)\n', totalHarga: 30000, statusBayar: 'Belum Lunas', status: 'Proses', tanggalMasuk: new Date(Date.now() - 2 * 86400000).toISOString(), selimut: 2, sprei: 0, boneka: 0, korden: 0, lokasi: 'Cimahi', employeeId: 1, customerId: 502 },
        { id: 2003, receiptNumber: 'INV-LND-1716818400000', namaPelanggan: 'Dewi Lestari', noHp: '08991234567', jenisLayanan: 'Kiloan Setrika, Barang Pendukung', jenisLaundry: '- Kiloan Setrika - Ekspress : 3 Kg (Rp 36.000)\n- Deterjen Wangi 50ml : 2 Pcs (Rp 6.000)\n', totalHarga: 42000, statusBayar: 'Lunas', status: 'Menunggu', tanggalMasuk: new Date(Date.now() - 1 * 86400000).toISOString(), selimut: 0, sprei: 0, boneka: 0, korden: 0, lokasi: 'Komp. Buah Batu', employeeId: 2, customerId: 504 }
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_laundry_expenses'))) {
      localStorage.setItem(getDemoKey('posbah_demo_laundry_expenses'), JSON.stringify([
        { id: 3001, kategori: 'Beli Deterjen', nominal: 150000, keterangan: 'Beli deterjen liquid 5 liter (Demo)', tanggal: new Date(Date.now() - 5 * 86400000).toISOString() },
        { id: 3002, kategori: 'Listrik', nominal: 350000, keterangan: 'Listrik ruko laundry bulan ini (Demo)', tanggal: new Date(Date.now() - 1 * 86400000).toISOString() }
      ]));
    }
  } else {
    if (!localStorage.getItem(getDemoKey('posbah_demo_products'))) {
      localStorage.setItem(getDemoKey('posbah_demo_products'), JSON.stringify([
        { id: 301, name: 'Pisang Goreng Keju Cokelat', price: 15000, costPrice: 9000, stock: 120, unit: 'porsi', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456001', image: null },
        { id: 302, name: 'Pisang Goreng Keju Stroberi', price: 15000, costPrice: 9500, stock: 85, unit: 'porsi', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456002', image: null },
        { id: 303, name: 'Pisang Goreng Keju Premium', price: 20000, costPrice: 11000, stock: 50, unit: 'porsi', wholesaleEnabled: false, wholesalePrices: null, variants: JSON.stringify([{ id: 1, name: 'Keju Double', price: 23000, costPrice: 12000, stock: 30 }, { id: 2, name: 'Milo Almond', price: 28000, costPrice: 15000, stock: 20 }]), barcode: '899123456003', image: null },
        { id: 304, name: 'Pisang Bakar Caramel Extra', price: 17000, costPrice: 10000, stock: 65, unit: 'porsi', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456004', image: null },
        { id: 305, name: 'Roti Bakar Bandung Cokelat', price: 18000, costPrice: 10000, stock: 40, unit: 'porsi', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456005', image: null },
        { id: 306, name: 'Roti Bakar Bandung Keju Special', price: 22000, costPrice: 12000, stock: 35, unit: 'porsi', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456006', image: null },
        { id: 307, name: 'Jus Alpukat Mentega Super', price: 18000, costPrice: 10000, stock: 60, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456007', image: null },
        { id: 308, name: 'Jus Mangga Arumanis', price: 16000, costPrice: 8000, stock: 75, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456008', image: null },
        { id: 309, name: 'Jus Stroberi Segar', price: 16000, costPrice: 8500, stock: 55, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456009', image: null },
        { id: 310, name: 'Es Kopi Susu Gula Aren', price: 15000, costPrice: 7000, stock: 150, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456010', image: null },
        { id: 311, name: 'Es Kopi Cappuccino Creamy', price: 17000, costPrice: 8000, stock: 90, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456011', image: null },
        { id: 312, name: 'Es Matcha Latte Japan', price: 18000, costPrice: 9000, stock: 80, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456012', image: null },
        { id: 313, name: 'Es Teh Manis Jasmine', price: 7000, costPrice: 2000, stock: 300, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456013', image: null },
        { id: 314, name: 'Es Lemon Tea Segar', price: 10000, costPrice: 4000, stock: 200, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456014', image: null },
        { id: 315, name: 'French Fries / Kentang Goreng', price: 15000, costPrice: 8000, stock: 110, unit: 'porsi', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456015', image: null },
        { id: 316, name: 'Singkong Goreng Garlic Crispy', price: 12000, costPrice: 6000, stock: 95, unit: 'porsi', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: '899123456016', image: null },
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_customers'))) {
      localStorage.setItem(getDemoKey('posbah_demo_customers'), JSON.stringify([
        { id: 501, name: 'Budi Santoso', phone: '08123456789', address: 'Jl. Merdeka No. 10, Bandung' },
        { id: 502, name: 'Siti Rahma', phone: '08567890123', address: 'Jl. Mawar No. 4, Cimahi' },
        { id: 503, name: 'Andi Wijaya', phone: '08789012345', address: 'Jl. Melati No. 15, Bandung' },
        { id: 504, name: 'Dewi Lestari', phone: '08991234567', address: 'Komp. Buah Batu Indah Reg-3' },
        { id: 505, name: 'Rian Hidayat', phone: '08215432765', address: 'Jl. Cihampelas No. 102' }
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_employees'))) {
      localStorage.setItem(getDemoKey('posbah_demo_employees'), JSON.stringify([
        { id: 1, name: 'Kasir Utama', role: 'KASIR', pin: '111111', salary: 2500000 },
        { id: 2, name: 'Admin Gudang', role: 'ADMIN', pin: '222222', salary: 3000000 },
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_suppliers'))) {
      localStorage.setItem(getDemoKey('posbah_demo_suppliers'), JSON.stringify([
        { id: 601, name: 'CV Maju Bersama (Demo)', phone: '081234567890', address: 'Jl. Kopo Jaya No. 12, Bandung', notes: 'Supplier utama bahan baku buah, kopi, dan keju' },
        { id: 602, name: 'Toko Grosir Pak Haji (Demo)', phone: '081298765432', address: 'Pasar Induk Caringin Blok C-7', notes: 'Supplier sayuran segar & kemasan cup/sedotan' },
        { id: 603, name: 'PT Duta Susu Nusantara (Demo)', phone: '082265789012', address: 'Kawasan Industri Rancaekek K-15', notes: 'Distributor resmi susu UHT & kental manis' }
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_purchase_orders'))) {
      localStorage.setItem(getDemoKey('posbah_demo_purchase_orders'), JSON.stringify([
        { id: 701, supplierId: 601, supplier: { name: 'CV Maju Bersama (Demo)' }, date: new Date(Date.now() - 3 * 86400000).toISOString(), status: 'RECEIVED', total: 1200000, notes: 'Restok bahan baku pisang & keju', items: [{ productId: 301, product: { name: 'Pisang Raja (Demo)' }, quantity: 100, costPrice: 6000 }, { productId: 302, product: { name: 'Keju Cheddar Kraft (Demo)' }, quantity: 30, costPrice: 20000 }] },
        { id: 702, supplierId: 602, supplier: { name: 'Toko Grosir Pak Haji (Demo)' }, date: new Date(Date.now() - 1 * 86400000).toISOString(), status: 'ORDERED', total: 450000, notes: 'Pesanan alpukat & mangga segar', items: [{ productId: 307, product: { name: 'Alpukat Mentega (Demo)' }, quantity: 20, costPrice: 15000 }, { productId: 308, product: { name: 'Mangga Arumanis (Demo)' }, quantity: 15, costPrice: 10000 }] },
        { id: 703, supplierId: 603, supplier: { name: 'PT Duta Susu Nusantara (Demo)' }, date: new Date().toISOString(), status: 'RECEIVED', total: 850000, notes: 'Susu cair & kental manis untuk kopi/matcha', items: [{ productId: 310, product: { name: 'Susu UHT Full Cream (Demo)' }, quantity: 50, costPrice: 17000 }] }
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_transactions'))) {
      localStorage.setItem(getDemoKey('posbah_demo_transactions'), JSON.stringify([
        {
          id: 1001,
          receiptNumber: 'TX-20260520-001',
          date: new Date(Date.now() - 4 * 86400000).toISOString(),
          subtotal: 162000,
          discountType: null,
          discountInput: 0,
          discountAmt: 0,
          total: 162000,
          paymentMethod: 'QRIS',
          status: 'COMPLETED',
          type: 'SALES',
          employeeId: 1,
          customerName: 'Budi Santoso',
          items: [
            { productId: 301, quantity: 5, price: 15000, costPrice: 9000, product: { name: 'Pisang Goreng Keju Cokelat' } },
            { productId: 307, quantity: 4, price: 18000, costPrice: 10000, product: { name: 'Jus Alpukat Mentega Super' } },
            { productId: 310, quantity: 1, price: 15000, costPrice: 7000, product: { name: 'Es Kopi Susu Gula Aren' } }
          ]
        },
        {
          id: 1002,
          receiptNumber: 'TX-20260520-002',
          date: new Date(Date.now() - 4 * 86400000).toISOString(),
          subtotal: 75000,
          discountType: null,
          discountInput: 0,
          discountAmt: 0,
          total: 75000,
          paymentMethod: 'CASH',
          status: 'COMPLETED',
          type: 'SALES',
          employeeId: 1,
          customerName: 'Siti Rahma',
          items: [
            { productId: 302, quantity: 5, price: 15000, costPrice: 9500, product: { name: 'Pisang Goreng Keju Stroberi' } }
          ]
        },
        {
          id: 1003,
          receiptNumber: 'TX-20260521-001',
          date: new Date(Date.now() - 3 * 86400000).toISOString(),
          subtotal: 124000,
          discountType: null,
          discountInput: 0,
          discountAmt: 0,
          total: 124000,
          paymentMethod: 'QRIS',
          status: 'COMPLETED',
          type: 'SALES',
          employeeId: 1,
          customerName: 'Andi Wijaya',
          items: [
            { productId: 303, quantity: 3, price: 28000, costPrice: 15000, variantId: 2, variantName: 'Milo Almond', product: { name: 'Pisang Goreng Keju Premium' } },
            { productId: 314, quantity: 4, price: 10000, costPrice: 4000, product: { name: 'Es Lemon Tea Segar' } }
          ]
        },
        {
          id: 1004,
          receiptNumber: 'TX-20260521-002',
          date: new Date(Date.now() - 3 * 86400000).toISOString(),
          subtotal: 143000,
          discountType: null,
          discountInput: 0,
          discountAmt: 0,
          total: 143000,
          paymentMethod: 'CASH',
          status: 'COMPLETED',
          type: 'SALES',
          employeeId: 1,
          customerName: 'Dewi Lestari',
          items: [
            { productId: 305, quantity: 6, price: 18000, costPrice: 10000, product: { name: 'Roti Bakar Bandung Cokelat' } },
            { productId: 313, quantity: 5, price: 7000, costPrice: 2000, product: { name: 'Es Teh Manis Jasmine' } }
          ]
        },
        {
          id: 1005,
          receiptNumber: 'TX-20260522-001',
          date: new Date(Date.now() - 2 * 86400000).toISOString(),
          subtotal: 120000,
          discountType: null,
          discountInput: 0,
          discountAmt: 0,
          total: 120000,
          paymentMethod: 'QRIS',
          status: 'COMPLETED',
          type: 'SALES',
          employeeId: 2,
          customerName: 'Rian Hidayat',
          items: [
            { productId: 316, quantity: 4, price: 12000, costPrice: 6000, product: { name: 'Singkong Goreng Garlic Crispy' } },
            { productId: 312, quantity: 4, price: 18000, costPrice: 9000, product: { name: 'Es Matcha Latte Japan' } }
          ]
        },
        {
          id: 1006,
          receiptNumber: 'TX-20260522-002',
          date: new Date(Date.now() - 2 * 86400000).toISOString(),
          subtotal: 222000,
          discountType: null,
          discountInput: 0,
          discountAmt: 0,
          total: 222000,
          paymentMethod: 'CASH',
          status: 'COMPLETED',
          type: 'SALES',
          employeeId: 1,
          customerName: 'Budi Santoso',
          items: [
            { productId: 301, quantity: 8, price: 15000, costPrice: 9000, product: { name: 'Pisang Goreng Keju Cokelat' } },
            { productId: 311, quantity: 6, price: 17000, costPrice: 8000, product: { name: 'Es Kopi Cappuccino Creamy' } }
          ]
        },
        {
          id: 1007,
          receiptNumber: 'TX-20260523-001',
          date: new Date(Date.now() - 1 * 86400000).toISOString(),
          subtotal: 155000,
          discountType: null,
          discountInput: 0,
          discountAmt: 0,
          total: 155000,
          paymentMethod: 'QRIS',
          status: 'COMPLETED',
          type: 'SALES',
          employeeId: 1,
          customerName: 'Siti Rahma',
          items: [
            { productId: 315, quantity: 5, price: 15000, costPrice: 8000, product: { name: 'French Fries / Kentang Goreng' } },
            { productId: 309, quantity: 5, price: 16000, costPrice: 8500, product: { name: 'Jus Stroberi Segar' } }
          ]
        },
        {
          id: 1008,
          receiptNumber: 'TX-20260523-002',
          date: new Date(Date.now() - 1 * 86400000).toISOString(),
          subtotal: 155000,
          discountType: null,
          discountInput: 0,
          discountAmt: 0,
          total: 155000,
          paymentMethod: 'CASH',
          status: 'COMPLETED',
          type: 'SALES',
          employeeId: 2,
          customerName: 'Andi Wijaya',
          items: [
            { productId: 313, quantity: 10, price: 7000, costPrice: 2000, product: { name: 'Es Teh Manis Jasmine' } },
            { productId: 304, quantity: 5, price: 17000, costPrice: 10000, product: { name: 'Pisang Bakar Caramel Extra' } }
          ]
        },
        {
          id: 1009,
          receiptNumber: 'PO-DEMO-001',
          date: new Date(Date.now() - 1 * 86400000).toISOString(),
          subtotal: 1500000,
          discountType: null,
          discountInput: 0,
          discountAmt: 0,
          total: 1500000,
          paymentMethod: 'CASH',
          status: 'PENDING',
          type: 'PRE_ORDER',
          orderStatus: 'DP_PAID',
          dpAmount: 500000,
          deliveryDate: new Date(Date.now() + 2 * 86400000).toISOString(),
          notes: 'Nasi kotak 100 box untuk acara reuni akbar',
          employeeId: 1,
          customerName: 'Dewi Lestari',
          items: [{ productId: 301, quantity: 100, price: 15000, costPrice: 9000, product: { name: 'Nasi Kotak Spesial (Demo)' } }]
        },
        {
          id: 1010,
          receiptNumber: 'TX-20260524-001',
          date: new Date(Date.now() - 4 * 3600000).toISOString(),
          subtotal: 148000,
          discountType: null,
          discountInput: 0,
          discountAmt: 0,
          total: 148000,
          paymentMethod: 'QRIS',
          status: 'COMPLETED',
          type: 'SALES',
          employeeId: 1,
          customerName: 'Rian Hidayat',
          items: [
            { productId: 306, quantity: 4, price: 22000, costPrice: 12000, product: { name: 'Roti Bakar Bandung Keju Special' } },
            { productId: 310, quantity: 4, price: 15000, costPrice: 7000, product: { name: 'Es Kopi Susu Gula Aren' } }
          ]
        },
        {
          id: 1011,
          receiptNumber: 'TX-20260524-002',
          date: new Date(Date.now() - 2 * 3600000).toISOString(),
          subtotal: 190000,
          discountType: null,
          discountInput: 0,
          discountAmt: 0,
          total: 190000,
          paymentMethod: 'CASH',
          status: 'COMPLETED',
          type: 'SALES',
          employeeId: 2,
          customerName: 'Budi Santoso',
          items: [
            { productId: 303, quantity: 5, price: 23000, costPrice: 12000, variantId: 1, variantName: 'Keju Double', product: { name: 'Pisang Goreng Keju Premium' } },
            { productId: 308, quantity: 5, price: 15000, costPrice: 8000, product: { name: 'Jus Mangga Arumanis' } }
          ]
        }
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_finances'))) {
      localStorage.setItem(getDemoKey('posbah_demo_finances'), JSON.stringify([
        { id: 401, type: 'EXPENSE', amount: 850000, description: 'Belanja Pisang & Bahan Baku Keju (Demo)', date: new Date(Date.now() - 6 * 86400000).toISOString(), status: 'PAID' },
        { id: 402, type: 'EXPENSE', amount: 350000, description: 'Beli Cup & Plastik Kemasan (Demo)', date: new Date(Date.now() - 9 * 86400000).toISOString(), status: 'PAID' },
        { id: 403, type: 'PAYABLE', amount: 1500000, description: 'Hutang Agen Pisang Pak Slamet (Demo)', date: new Date(Date.now() - 14 * 86400000).toISOString(), status: 'PENDING' },
        { id: 404, type: 'RECEIVABLE', amount: 1200000, description: 'Piutang Catering Ibu Ratna (Demo)', date: new Date(Date.now() - 6 * 86400000).toISOString(), status: 'PENDING' },
        { id: 405, type: 'INCOME', amount: 162000, description: 'Penjualan TX-20260520-001 (Demo)', date: new Date(Date.now() - 4 * 86400000).toISOString(), status: 'PAID' },
        { id: 406, type: 'INCOME', amount: 75000, description: 'Penjualan TX-20260520-002 (Demo)', date: new Date(Date.now() - 4 * 86400000).toISOString(), status: 'PAID' },
        { id: 407, type: 'INCOME', amount: 124000, description: 'Penjualan TX-20260521-001 (Demo)', date: new Date(Date.now() - 3 * 86400000).toISOString(), status: 'PAID' },
        { id: 408, type: 'INCOME', amount: 143000, description: 'Penjualan TX-20260521-002 (Demo)', date: new Date(Date.now() - 3 * 86400000).toISOString(), status: 'PAID' },
        { id: 409, type: 'INCOME', amount: 120000, description: 'Penjualan TX-20260522-001 (Demo)', date: new Date(Date.now() - 2 * 86400000).toISOString(), status: 'PAID' },
        { id: 410, type: 'INCOME', amount: 222000, description: 'Penjualan TX-20260522-002 (Demo)', date: new Date(Date.now() - 2 * 86400000).toISOString(), status: 'PAID' },
        { id: 411, type: 'INCOME', amount: 155000, description: 'Penjualan TX-20260523-001 (Demo)', date: new Date(Date.now() - 1 * 86400000).toISOString(), status: 'PAID' },
        { id: 412, type: 'INCOME', amount: 155000, description: 'Penjualan TX-20260523-002 (Demo)', date: new Date(Date.now() - 1 * 86400000).toISOString(), status: 'PAID' },
        { id: 413, type: 'INCOME', amount: 500000, description: 'DP Pre-Order PO-DEMO-001 (Demo)', date: new Date(Date.now() - 1 * 86400000).toISOString(), status: 'PAID' },
        { id: 414, type: 'RECEIVABLE', amount: 1000000, description: 'Piutang Sisa Pelunasan PO-DEMO-001 (Demo)', date: new Date(Date.now() - 1 * 86400000).toISOString(), status: 'PENDING' },
        { id: 415, type: 'EXPENSE', amount: 1200000, description: 'Pelunasan PO Restok CV Maju Bersama (Demo)', date: new Date().toISOString(), status: 'PAID' },
        { id: 416, type: 'INCOME', amount: 148000, description: 'Penjualan TX-20260524-001 (Demo)', date: new Date().toISOString(), status: 'PAID' },
        { id: 417, type: 'INCOME', amount: 190000, description: 'Penjualan TX-20260524-002 (Demo)', date: new Date().toISOString(), status: 'PAID' },
        { id: 418, type: 'EXPENSE', amount: 2500000, description: 'Gaji Bulanan Karyawan - Kasir Utama (Demo)', date: new Date().toISOString(), status: 'PAID' },
        { id: 419, type: 'EXPENSE', amount: 3000000, description: 'Gaji Bulanan Karyawan - Admin Gudang (Demo)', date: new Date().toISOString(), status: 'PAID' },
      ]));
    }
    if (!localStorage.getItem(getDemoKey('posbah_demo_logs'))) {
      localStorage.setItem(getDemoKey('posbah_demo_logs'), JSON.stringify([
        { id: 1, action: 'CREATE_TRANSACTION', description: 'Kasir Utama membuat transaksi baru TX-20260520-001', createdAt: new Date(Date.now() - 4 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 2, action: 'CREATE_TRANSACTION', description: 'Kasir Utama membuat transaksi baru TX-20260520-002', createdAt: new Date(Date.now() - 4 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 3, action: 'CREATE_TRANSACTION', description: 'Kasir Utama membuat transaksi baru TX-20260521-001', createdAt: new Date(Date.now() - 3 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 4, action: 'CREATE_TRANSACTION', description: 'Kasir Utama membuat transaksi baru TX-20260521-002', createdAt: new Date(Date.now() - 3 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 5, action: 'CREATE_TRANSACTION', description: 'Kasir Utama membuat transaksi baru TX-20260522-001', createdAt: new Date(Date.now() - 2 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 6, action: 'CREATE_TRANSACTION', description: 'Kasir Utama membuat transaksi baru TX-20260522-002', createdAt: new Date(Date.now() - 2 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 7, action: 'RECEIVE_PO', description: 'Admin Gudang menerima barang dari PO CV Maju Bersama', createdAt: new Date(Date.now() - 3 * 86400000).toISOString(), employee: { name: 'Admin Gudang', role: 'ADMIN' } },
        { id: 8, action: 'CREATE_TRANSACTION', description: 'Kasir Utama membuat transaksi baru TX-20260523-001', createdAt: new Date(Date.now() - 1 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 9, action: 'CREATE_TRANSACTION', description: 'Kasir Utama membuat transaksi baru TX-20260523-002', createdAt: new Date(Date.now() - 1 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 10, action: 'CREATE_TRANSACTION', description: 'Kasir Utama menerima DP Pre-Order PO-DEMO-001 sebesar Rp 500.000', createdAt: new Date(Date.now() - 1 * 86400000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 11, action: 'CREATE_TRANSACTION', description: 'Kasir Utama membuat transaksi baru TX-20260524-001', createdAt: new Date(Date.now() - 4 * 3600000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 12, action: 'CREATE_TRANSACTION', description: 'Kasir Utama membuat transaksi baru TX-20260524-002', createdAt: new Date(Date.now() - 2 * 3600000).toISOString(), employee: { name: 'Kasir Utama', role: 'KASIR' } },
        { id: 13, action: 'PAY_SALARY', description: 'Owner membayarkan gaji untuk Kasir Utama sebesar Rp 2.500.000', createdAt: new Date().toISOString(), employee: { name: 'userdemo', role: 'OWNER' } },
        { id: 14, action: 'PAY_SALARY', description: 'Owner membayarkan gaji untuk Admin Gudang sebesar Rp 3.000.000', createdAt: new Date().toISOString(), employee: { name: 'userdemo', role: 'OWNER' } },
      ]));
    }
  }
};

const logDemoActivity = (action, description) => {
  let empName = 'Demo User';
  const storedUser = localStorage.getItem('posbah_user');
  if (storedUser) {
    try {
      const u = JSON.parse(storedUser);
      empName = u.name || 'Demo User';
    } catch (_) {}
  }
  const logsKey = getDemoKey('posbah_demo_logs');
  const table = JSON.parse(localStorage.getItem(logsKey) || '[]');
  table.unshift({
    id: Date.now(),
    action,
    description,
    createdAt: new Date().toISOString(),
    employee: { name: empName, role: 'OWNER' }
  });
  localStorage.setItem(logsKey, JSON.stringify(table.slice(0, 100)));
};

const getDemoKey = (key) => {
  if (key && key.startsWith('posbah_demo_')) {
    const tableName = key.replace('posbah_demo_', '');
    const mode = localStorage.getItem('posbah_app_mode') || 'FNB';
    return `posbah_demo_${mode.toLowerCase()}_${tableName}`;
  }
  return key;
};

const getTable = (key) => JSON.parse(localStorage.getItem(getDemoKey(key)) || '[]');
const saveTable = (key, data) => localStorage.setItem(getDemoKey(key), JSON.stringify(data));

// Override Axios Adapter for Demo User
const defaultAdapter = getAdapter(api.defaults.adapter || axios.defaults.adapter);
api.defaults.adapter = async function (config) {
  const storedUser = localStorage.getItem('posbah_user');
  let isDemo = false;
  if (storedUser) {
    try {
      const u = JSON.parse(storedUser);
      isDemo = u?.isDemo === true;
    } catch (_) {}
  }

  let pathname = '';
  try {
    pathname = new URL(config.url, config.baseURL || window.location.origin).pathname;
  } catch {
    pathname = config.url;
  }
  const route = pathname.replace(/^\/api/, '').replace(/^\//, '').replace(/\/$/, '');

  const isOffline = !navigator.onLine;

  // Jangan pernah intercept request login / google-register, selalu arahkan ke backend
  if (route === 'auth/login' || route === 'auth/google-register' || (!isDemo && !isOffline)) {
    return defaultAdapter(config);
  }

  if (!isDemo && isOffline) {
    return handleOfflineRequest(config, route);
  }

  // Simulasikan data locally di browser
  initDemoData();

  const method = config.method.toUpperCase();
  const parts = route.split('/');
  
  let data = null;
  let status = 200;

  if (method === 'GET') {
    if (route === 'products') {
      data = getTable('posbah_demo_products');
    } else if (route === 'customers' || route === 'customers/list') {
      data = getTable('posbah_demo_customers');
    } else if (route === 'employees') {
      data = getTable('posbah_demo_employees');
    } else if (route === 'suppliers') {
      data = getTable('posbah_demo_suppliers');
    } else if (route === 'purchase-orders') {
      data = getTable('posbah_demo_purchase_orders');
    } else if (route === 'cars') {
      data = getTable('posbah_demo_cars');
    } else if (route === 'rentals') {
      data = getTable('posbah_demo_rentals');
    } else if (route === 'transactions') {
      data = getTable('posbah_demo_transactions');
    } else if (route === 'pre-orders') {
      data = getTable('posbah_demo_transactions').filter(t => t.type === 'PRE_ORDER');
    } else if (route === 'finances') {
      data = getTable('posbah_demo_finances');
    } else if (route === 'activity-logs') {
      data = getTable('posbah_demo_logs');
    } else if (route === 'queues/active' || route === 'queues/pending') {
      data = getTable('posbah_demo_transactions').filter(t => t.status === 'PENDING' && t.queueNumber !== null);

    } else if (route === 'payroll/history') {
      data = [];
    } else if (route === 'laundry/services') {
      data = getTable('posbah_demo_laundry_services');
    } else if (route === 'laundry/orders') {
      let orders = getTable('posbah_demo_laundry_orders');
      try {
        const fullUrl = new URL(config.url, config.baseURL || window.location.origin);
        const cari = fullUrl.searchParams.get('cari');
        const filterBayar = fullUrl.searchParams.get('filterBayar');
        const filterTipe = fullUrl.searchParams.get('filterTipe');
        if (cari) {
          orders = orders.filter(o => o.namaPelanggan.toLowerCase().includes(cari.toLowerCase()));
        }
        if (filterBayar && filterBayar !== 'Semua') {
          orders = orders.filter(o => o.statusBayar === filterBayar);
        }
        if (filterTipe === 'Plastik') {
          orders = orders.filter(o => o.jenisLayanan.toLowerCase().includes('barang'));
        } else if (filterTipe === 'Laundry') {
          orders = orders.filter(o => !o.jenisLayanan.toLowerCase().includes('barang'));
        }
      } catch (_) {}
      data = orders;
    } else if (route.startsWith('laundry/orders/')) {
      const orderId = Number(parts[2]);
      data = getTable('posbah_demo_laundry_orders').find(o => o.id === orderId) || null;
    } else if (route === 'laundry/expenses') {
      data = getTable('posbah_demo_laundry_expenses');
    } else if (route === 'reports') {
      const mode = localStorage.getItem('posbah_app_mode') || 'FNB';
      // Parse date filters from query params
      let paramFrom = null;
      let paramTo = null;
      try {
        const fullUrl = new URL(config.url, config.baseURL || window.location.origin);
        const pf = fullUrl.searchParams.get('from');
        const pt = fullUrl.searchParams.get('to');
        if (pf) paramFrom = new Date(pf);
        if (pt) { paramTo = new Date(pt); paramTo.setHours(23, 59, 59, 999); }
      } catch (_) {}

      const inRange = (dateStr) => {
        if (!dateStr) return true;
        const d = new Date(dateStr);
        if (paramFrom && d < paramFrom) return false;
        if (paramTo && d > paramTo) return false;
        return true;
      };

      const fins = getTable('posbah_demo_finances');
      const filteredFins = fins.filter(f => inRange(f.date || f.createdAt));
      let totalExpenses = filteredFins.filter(f => f.type === 'EXPENSE' && f.status === 'PAID').reduce((sum, f) => sum + f.amount, 0);
      let pendingReceivables = filteredFins.filter(f => f.type === 'RECEIVABLE' && f.status === 'PENDING').reduce((sum, f) => sum + f.amount, 0);
      let totalPayable = filteredFins.filter(f => f.type === 'PAYABLE' && f.status === 'PENDING').reduce((sum, f) => sum + f.amount, 0);

      if (mode === 'LAUNDRY') {
        const exps = getTable('posbah_demo_laundry_expenses').filter(e => inRange(e.tanggal || e.createdAt));
        totalExpenses = exps.reduce((sum, e) => sum + (e.nominal || 0), 0);
        const orders = getTable('posbah_demo_laundry_orders').filter(o => inRange(o.tanggalMasuk || o.createdAt));
        pendingReceivables = orders.filter(o => o.statusBayar === 'Belum Lunas').reduce((sum, o) => sum + (o.totalHarga || 0), 0);
        totalPayable = 0;
      }

      let totalSales = 0;
      let todaySales = 0;
      let count = 0;
      let grossProfit = 0;

      if (mode === 'RENTAL') {
        const rentals = getTable('posbah_demo_rentals').filter(r => inRange(r.createdAt || r.startDate));
        totalSales = rentals.reduce((sum, r) => sum + (r.totalPrice || 0), 0);
        todaySales = rentals.filter(r => new Date(r.createdAt || r.startDate).toDateString() === new Date().toDateString()).reduce((sum, r) => sum + (r.totalPrice || 0), 0);
        count = rentals.length;
        grossProfit = totalSales;
      } else if (mode === 'LAUNDRY') {
        const orders = getTable('posbah_demo_laundry_orders').filter(o => inRange(o.tanggalMasuk || o.createdAt));
        totalSales = orders.filter(o => o.statusBayar === 'Lunas').reduce((sum, o) => sum + (o.totalHarga || 0), 0);
        todaySales = orders.filter(o => o.statusBayar === 'Lunas' && new Date(o.tanggalMasuk || o.createdAt).toDateString() === new Date().toDateString()).reduce((sum, o) => sum + (o.totalHarga || 0), 0);
        count = orders.length;
        grossProfit = totalSales;
      } else {
        const txs = getTable('posbah_demo_transactions').filter(t => t.status === 'COMPLETED' && inRange(t.date));
        totalSales = txs.reduce((sum, t) => sum + (t.total || 0), 0);
        todaySales = txs.filter(t => new Date(t.date).toDateString() === new Date().toDateString()).reduce((sum, t) => sum + (t.total || 0), 0);
        count = txs.length;
        // Gross profit = total revenue - COGS
        grossProfit = txs.reduce((sum, t) => {
          const cogs = (t.items || []).reduce((s, i) => s + ((i.costPrice || 0) * (i.quantity || 1)), 0);
          return sum + (t.total || 0) - cogs;
        }, 0);
      }
      
      data = {
        totalSales,
        totalExpenses,
        netIncome: totalSales - totalExpenses,
        grossProfit,
        pendingReceivables,
        totalPayable,
        todaySales,
        transactionCount: count
      };
    }
  } else if (method === 'POST') {
    const payload = JSON.parse(config.data || '{}');
    const newId = Date.now();
    payload.id = newId;

    if (route === 'laundry/services') {
      const table = getTable('posbah_demo_laundry_services');
      table.push(payload);
      saveTable('posbah_demo_laundry_services', table);
      data = payload;
      logDemoActivity('CREATE_LAUNDRY_SERVICE', `Menambahkan layanan laundry baru ${payload.kategori} - ${payload.nama}`);
    } else if (route === 'laundry/orders') {
      const table = getTable('posbah_demo_laundry_orders');
      payload.receiptNumber = `INV-LND-${Date.now()}`;
      payload.tanggalMasuk = new Date().toISOString();
      payload.status = 'Menunggu';
      table.push(payload);
      saveTable('posbah_demo_laundry_orders', table);
      data = payload;
      logDemoActivity('CREATE_LAUNDRY_ORDER', `Membuat pesanan laundry baru ${payload.receiptNumber} senilai Rp ${payload.totalHarga.toLocaleString('id-ID')}`);
    } else if (route === 'laundry/expenses') {
      const table = getTable('posbah_demo_laundry_expenses');
      payload.tanggal = payload.tanggal || new Date().toISOString();
      table.push(payload);
      saveTable('posbah_demo_laundry_expenses', table);
      data = payload;
      logDemoActivity('CREATE_LAUNDRY_EXPENSE', `Mencatat pengeluaran laundry baru: ${payload.kategori} senilai Rp ${payload.nominal.toLocaleString('id-ID')}`);
    } else if (route === 'products') {
      const table = getTable('posbah_demo_products');
      table.push(payload);
      saveTable('posbah_demo_products', table);
      data = payload;
      logDemoActivity('CREATE_PRODUCT', `Menambahkan produk baru ${payload.name}`);
    } else if (route === 'customers') {
      const table = getTable('posbah_demo_customers');
      table.push(payload);
      saveTable('posbah_demo_customers', table);
      data = payload;
    } else if (route === 'employees') {
      const table = getTable('posbah_demo_employees');
      table.push(payload);
      saveTable('posbah_demo_employees', table);
      data = payload;
      logDemoActivity('CREATE_EMPLOYEE', `Menambahkan karyawan baru ${payload.name}`);
    } else if (route === 'suppliers') {
      const table = getTable('posbah_demo_suppliers');
      table.push(payload);
      saveTable('posbah_demo_suppliers', table);
      data = payload;
    } else if (route === 'finances') {
      const table = getTable('posbah_demo_finances');
      table.push(payload);
      saveTable('posbah_demo_finances', table);
      data = payload;
      logDemoActivity('CREATE_FINANCE', `Mencatat keuangan baru: ${payload.description} senilai Rp ${payload.amount.toLocaleString('id-ID')}`);
    } else if (route === 'purchase-orders') {
      const table = getTable('posbah_demo_purchase_orders');
      payload.status = 'ORDERED';
      payload.date = new Date().toISOString();
      const sups = getTable('posbah_demo_suppliers');
      const sup = sups.find(s => s.id === Number(payload.supplierId));
      payload.supplier = { name: sup ? sup.name : 'Supplier' };
      const prods = getTable('posbah_demo_products');
      payload.items = (payload.items || []).map(i => {
        const pr = prods.find(p => p.id === Number(i.productId));
        return { ...i, product: { name: pr ? pr.name : 'Produk' } };
      });
      payload.total = payload.items.reduce((sum, i) => sum + (i.costPrice * i.quantity), 0);
      table.push(payload);
      saveTable('posbah_demo_purchase_orders', table);
      data = payload;
    } else if (parts[0] === 'purchase-orders' && parts[2] === 'receive') {
      const poId = Number(parts[1]);
      const table = getTable('posbah_demo_purchase_orders');
      const po = table.find(p => p.id === poId);
      if (po) {
        po.status = 'RECEIVED';
        saveTable('posbah_demo_purchase_orders', table);
        const prods = getTable('posbah_demo_products');
        (po.items || []).forEach(item => {
          const pr = prods.find(p => p.id === Number(item.productId));
          if (pr) pr.stock += item.quantity;
        });
        saveTable('posbah_demo_products', prods);
        const fins = getTable('posbah_demo_finances');
        fins.push({
          id: Date.now(),
          type: 'PAYABLE',
          amount: po.total,
          description: `Hutang pembelian barang PO #${po.id} dari ${po.supplier?.name}`,
          date: new Date().toISOString(),
          status: 'PENDING'
        });
        saveTable('posbah_demo_finances', fins);
        logDemoActivity('RECEIVE_PO', `Penerimaan barang PO #${po.id} dari ${po.supplier?.name}`);
      }
      data = { success: true };
    } else if (route === 'cars') {
      const table = getTable('posbah_demo_cars');
      table.push(payload);
      saveTable('posbah_demo_cars', table);
      data = payload;
      logDemoActivity('CREATE_CAR', `Menambahkan mobil baru ${payload.name} (${payload.plateNumber})`);
    } else if (route === 'rentals') {
      const table = getTable('posbah_demo_rentals');
      const cars = getTable('posbah_demo_cars');
      const car = cars.find(c => c.id === Number(payload.carId));
      if (car) car.status = 'RENTED';
      saveTable('posbah_demo_cars', cars);

      let finalCustomerId = payload.customerId ? Number(payload.customerId) : null;
      if (!finalCustomerId && payload.customerName) {
        const custs = getTable('posbah_demo_customers');
        const existing = custs.find(c => c.name.toLowerCase() === payload.customerName.toLowerCase());
        if (existing) {
          finalCustomerId = existing.id;
        } else {
          const newCust = {
            id: Date.now() + Math.floor(Math.random() * 1000),
            name: payload.customerName,
            phone: '',
            address: ''
          };
          custs.push(newCust);
          saveTable('posbah_demo_customers', custs);
          finalCustomerId = newCust.id;
        }
      }
      payload.customerId = finalCustomerId;

      payload.car = car || { name: 'Mobil', plateNumber: '', pricePerDay: 0 };
      payload.status = 'ACTIVE';
      payload.createdAt = new Date().toISOString();
      table.push(payload);
      saveTable('posbah_demo_rentals', table);

      // Selalu catat pemasukan rental ke keuangan
      const fins = getTable('posbah_demo_finances');
      if (payload.paymentMethod === 'CASH' || payload.paymentMethod === 'TRANSFER' || payload.paymentMethod === 'QRIS') {
        // Langsung tercatat sebagai pendapatan tunai
        fins.push({
          id: Date.now(),
          type: 'INCOME',
          amount: payload.totalPrice,
          description: `Pendapatan sewa mobil ${payload.car.name} (${payload.car.plateNumber}) - ${payload.customerName}`,
          date: new Date().toISOString(),
          status: 'PAID'
        });
      } else {
        // Kredit — catat sebagai piutang
        fins.push({
          id: Date.now(),
          type: 'RECEIVABLE',
          amount: payload.totalPrice,
          description: `Piutang sewa mobil ${payload.car.name} ke ${payload.customerName}`,
          date: new Date().toISOString(),
          status: 'PENDING',
          customerId: finalCustomerId
        });
      }
      saveTable('posbah_demo_finances', fins);

      logDemoActivity('CREATE_RENTAL', `Menyewakan mobil ${payload.car.name} (${payload.car.plateNumber}) ke ${payload.customerName}`);
      data = payload;
    } else if (parts[0] === 'rentals' && parts[2] === 'return') {
      const rentalId = Number(parts[1]);
      const table = getTable('posbah_demo_rentals');
      const rental = table.find(r => r.id === rentalId);
      if (rental) {
        rental.status = 'RETURNED';
        rental.actualReturnDate = payload.actualReturnDate || new Date().toISOString();
        rental.lateFee = payload.lateFee || 0;

        const cars = getTable('posbah_demo_cars');
        const car = cars.find(c => c.id === rental.carId);
        if (car) car.status = 'AVAILABLE';
        saveTable('posbah_demo_cars', cars);

        saveTable('posbah_demo_rentals', table);

        if (rental.lateFee > 0 && payload.paymentMethod !== 'CASH' && payload.paymentMethod !== 'TRANSFER' && payload.paymentMethod !== 'QRIS') {
          const fins = getTable('posbah_demo_finances');
          fins.push({
            id: Date.now(),
            type: 'RECEIVABLE',
            amount: rental.lateFee,
            description: `Denda telat pengembalian mobil ${rental.car?.name} oleh ${rental.customerName}`,
            date: new Date().toISOString(),
            status: 'PENDING'
          });
          saveTable('posbah_demo_finances', fins);
        }
        logDemoActivity('RETURN_CAR', `Pengembalian mobil ${rental.car?.name} oleh ${rental.customerName}. Denda: Rp ${rental.lateFee.toLocaleString('id-ID')}`);
      }
      data = { success: true };
    } else if (route === 'transactions') {
      const table = getTable('posbah_demo_transactions');
      
      const num = table.length + 1;
      payload.receiptNumber = `TX-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${String(num).padStart(3, '0')}`;
      payload.date = new Date().toISOString();
      
      const prods = getTable('posbah_demo_products');
      payload.items = (payload.items || []).map(item => {
        const pr = prods.find(p => p.id === Number(item.productId));
        if (pr) {
          pr.stock -= item.quantity;
          item.product = { name: pr.name };
        }
        return item;
      });
      saveTable('posbah_demo_products', prods);
      
      table.push(payload);
      saveTable('posbah_demo_transactions', table);
      
      if (payload.paymentMethod === 'HUTANG') {
        const fins = getTable('posbah_demo_finances');
        fins.push({
          id: Date.now(),
          type: 'RECEIVABLE',
          amount: payload.total,
          description: `Piutang (Hutang pelanggan) - ${payload.customerName || payload.receiptNumber}`,
          date: new Date().toISOString(),
          status: 'PENDING'
        });
        saveTable('posbah_demo_finances', fins);
      }
      
      logDemoActivity('CREATE_TRANSACTION', `Membuat transaksi baru ${payload.receiptNumber} senilai Rp ${payload.total.toLocaleString('id-ID')}`);
      data = payload;

    } else if (parts[0] === 'payroll' && parts[1] === 'pay') {
      // Catat pembayaran gaji sebagai pengeluaran
      const payrollPayload = JSON.parse(config.data || '{}');
      if (payrollPayload.employeeId || payrollPayload.amount) {
        const emps = getTable('posbah_demo_employees');
        const emp = emps.find(e => e.id === Number(payrollPayload.employeeId));
        const salaryAmt = payrollPayload.amount || (emp ? emp.salary : 0);
        if (salaryAmt > 0) {
          const fins = getTable('posbah_demo_finances');
          fins.push({
            id: Date.now(),
            type: 'EXPENSE',
            amount: salaryAmt,
            description: `[Gaji] Pembayaran gaji ${emp ? emp.name : 'Karyawan'} - ${new Date().toLocaleDateString('id-ID', { month: 'long', year: 'numeric' })}`,
            date: new Date().toISOString(),
            status: 'PAID'
          });
          saveTable('posbah_demo_finances', fins);
        }
      }
      data = { success: true };
      logDemoActivity('PAY_SALARY', `Membayar gaji karyawan.`);
    } else if (route === 'reset-finance') {
      saveTable('posbah_demo_transactions', []);
      saveTable('posbah_demo_finances', []);
      logDemoActivity('RESET_FINANCE', 'Mengosongkan semua data keuangan dan penjualan.');
      data = { success: true };
    }
  } else if (method === 'PUT') {
    const payload = JSON.parse(config.data || '{}');
    if (parts[0] === 'laundry') {
      if (parts[1] === 'services') {
        const id = Number(parts[2]);
        const table = getTable('posbah_demo_laundry_services');
        const idx = table.findIndex(s => s.id === id);
        if (idx !== -1) {
          table[idx] = { ...table[idx], ...payload };
          saveTable('posbah_demo_laundry_services', table);
          data = table[idx];
          logDemoActivity('UPDATE_LAUNDRY_SERVICE', `Memperbarui layanan laundry ${table[idx]?.kategori} - ${table[idx]?.nama}`);
        }
      } else if (parts[1] === 'orders') {
        if (parts[2] === 'status') {
          const id = Number(parts[3]);
          const table = getTable('posbah_demo_laundry_orders');
          const idx = table.findIndex(o => o.id === id);
          if (idx !== -1) {
            table[idx].status = payload.status;
            if (payload.status === 'Selesai' || payload.status === 'Diambil') {
              table[idx].tanggalSelesai = new Date().toISOString();
            }
            saveTable('posbah_demo_laundry_orders', table);
            data = table[idx];
            logDemoActivity('UPDATE_LAUNDRY_STATUS', `Mengubah status pesanan laundry ${table[idx].receiptNumber} menjadi ${payload.status}`);
          }
        } else if (parts[2] === 'pay') {
          const id = Number(parts[3]);
          const table = getTable('posbah_demo_laundry_orders');
          const idx = table.findIndex(o => o.id === id);
          if (idx !== -1) {
            const statusLama = table[idx].statusBayar || 'Belum Lunas';
            const statusBaru = statusLama === 'Lunas' ? 'Belum Lunas' : 'Lunas';
            table[idx].statusBayar = statusBaru;
            saveTable('posbah_demo_laundry_orders', table);
            data = table[idx];
            logDemoActivity('UPDATE_LAUNDRY_PAYMENT', `Mengubah status pembayaran pesanan laundry ${table[idx].receiptNumber} menjadi ${statusBaru}`);
          }
        } else {
          const id = Number(parts[2]);
          const table = getTable('posbah_demo_laundry_orders');
          const idx = table.findIndex(o => o.id === id);
          if (idx !== -1) {
            table[idx] = { ...table[idx], ...payload };
            saveTable('posbah_demo_laundry_orders', table);
            data = table[idx];
          }
        }
      } else if (parts[1] === 'expenses') {
        const id = Number(parts[2]);
        const table = getTable('posbah_demo_laundry_expenses');
        const idx = table.findIndex(e => e.id === id);
        if (idx !== -1) {
          table[idx] = { ...table[idx], ...payload };
          saveTable('posbah_demo_laundry_expenses', table);
          data = table[idx];
          logDemoActivity('UPDATE_LAUNDRY_EXPENSE', `Mengubah pengeluaran laundry ID ${id}`);
        }
      }
    } else if (parts[0] === 'products') {
      const table = getTable('posbah_demo_products');
      const id = Number(parts[1]);
      const idx = table.findIndex(p => p.id === id);
      if (idx !== -1) {
        table[idx] = { ...table[idx], ...payload };
        saveTable('posbah_demo_products', table);
      }
      data = table[idx];
      logDemoActivity('UPDATE_PRODUCT', `Memperbarui data produk ${table[idx]?.name}`);
    } else if (parts[0] === 'customers') {
      const table = getTable('posbah_demo_customers');
      const id = Number(parts[1]);
      const idx = table.findIndex(c => c.id === id);
      if (idx !== -1) {
        table[idx] = { ...table[idx], ...payload };
        saveTable('posbah_demo_customers', table);
      }
      data = table[idx];
    } else if (parts[0] === 'employees') {
      const table = getTable('posbah_demo_employees');
      const id = Number(parts[1]);
      const idx = table.findIndex(e => e.id === id);
      if (idx !== -1) {
        table[idx] = { ...table[idx], ...payload };
        saveTable('posbah_demo_employees', table);
      }
      data = table[idx];
      logDemoActivity('UPDATE_EMPLOYEE', `Memperbarui data karyawan ${table[idx]?.name}`);
    } else if (parts[0] === 'suppliers') {
      const table = getTable('posbah_demo_suppliers');
      const id = Number(parts[1]);
      const idx = table.findIndex(s => s.id === id);
      if (idx !== -1) {
        table[idx] = { ...table[idx], ...payload };
        saveTable('posbah_demo_suppliers', table);
      }
      data = table[idx];
    } else if (parts[0] === 'finances') {
      const table = getTable('posbah_demo_finances');
      const id = Number(parts[1]);
      const idx = table.findIndex(f => f.id === id);
      if (idx !== -1) {
        table[idx] = { ...table[idx], ...payload };
        saveTable('posbah_demo_finances', table);
      }
      data = table[idx];
      logDemoActivity('UPDATE_FINANCE', `Mengubah catatan keuangan: ${table[idx]?.description}`);
    } else if (parts[0] === 'cars') {
      const table = getTable('posbah_demo_cars');
      const id = Number(parts[1]);
      const idx = table.findIndex(c => c.id === id);
      if (idx !== -1) {
        table[idx] = { ...table[idx], ...payload };
        saveTable('posbah_demo_cars', table);
      }
      data = table[idx];
      logDemoActivity('UPDATE_CAR', `Mengubah data mobil ${table[idx]?.name}`);
    } else if (parts[0] === 'transactions') {
      const table = getTable('posbah_demo_transactions');
      const id = Number(parts[1]);
      const idx = table.findIndex(t => t.id === id);
      if (idx !== -1) {
        table[idx] = { ...table[idx], ...payload };
        saveTable('posbah_demo_transactions', table);
        
        if (payload.status === 'CANCELLED') {
          const prods = getTable('posbah_demo_products');
          (table[idx].items || []).forEach(item => {
            const pr = prods.find(p => p.id === Number(item.productId));
            if (pr) pr.stock += item.quantity;
          });
          saveTable('posbah_demo_products', prods);
          logDemoActivity('CANCEL_TRANSACTION', `Membatalkan transaksi antrean ${table[idx].receiptNumber}`);
        } else if (payload.status === 'COMPLETED') {
          logDemoActivity('UPDATE_TRANSACTION', `Menyelesaikan pembayaran transaksi antrean ${table[idx].receiptNumber}`);
        }
      }
      data = table[idx];
    }
  } else if (method === 'DELETE') {
    const id = Number(parts[1]);

    if (parts[0] === 'laundry') {
      const type = parts[1];
      const laundryId = Number(parts[2]);
      if (type === 'services') {
        const table = getTable('posbah_demo_laundry_services');
        const filtered = table.filter(s => s.id !== laundryId);
        saveTable('posbah_demo_laundry_services', filtered);
        data = { success: true };
        logDemoActivity('DELETE_LAUNDRY_SERVICE', `Menghapus layanan laundry`);
      } else if (type === 'orders') {
        const table = getTable('posbah_demo_laundry_orders');
        const filtered = table.filter(o => o.id !== laundryId);
        saveTable('posbah_demo_laundry_orders', filtered);
        data = { success: true };
        logDemoActivity('DELETE_LAUNDRY_ORDER', `Menghapus pesanan laundry`);
      } else if (type === 'expenses') {
        const table = getTable('posbah_demo_laundry_expenses');
        const filtered = table.filter(e => e.id !== laundryId);
        saveTable('posbah_demo_laundry_expenses', filtered);
        data = { success: true };
        logDemoActivity('DELETE_LAUNDRY_EXPENSE', `Menghapus pengeluaran laundry`);
      }
    } else if (parts[0] === 'products') {
      const table = getTable('posbah_demo_products');
      const item = table.find(p => p.id === id);
      const filtered = table.filter(p => p.id !== id);
      saveTable('posbah_demo_products', filtered);
      data = { success: true };
      if (item) logDemoActivity('DELETE_PRODUCT', `Menghapus produk ${item.name}`);
    } else if (parts[0] === 'customers') {
      const table = getTable('posbah_demo_customers');
      const filtered = table.filter(c => c.id !== id);
      saveTable('posbah_demo_customers', filtered);
      data = { success: true };
    } else if (parts[0] === 'employees') {
      const table = getTable('posbah_demo_employees');
      const item = table.find(e => e.id === id);
      const filtered = table.filter(e => e.id !== id);
      saveTable('posbah_demo_employees', filtered);
      data = { success: true };
      if (item) logDemoActivity('DELETE_EMPLOYEE', `Menghapus karyawan ${item.name}`);
    } else if (parts[0] === 'suppliers') {
      const table = getTable('posbah_demo_suppliers');
      const filtered = table.filter(s => s.id !== id);
      saveTable('posbah_demo_suppliers', filtered);
      data = { success: true };
    } else if (parts[0] === 'finances') {
      const table = getTable('posbah_demo_finances');
      const item = table.find(f => f.id === id);
      const filtered = table.filter(f => f.id !== id);
      saveTable('posbah_demo_finances', filtered);
      data = { success: true };
      if (item) logDemoActivity('DELETE_FINANCE', `Menghapus data keuangan: ${item.description}`);
    } else if (parts[0] === 'cars') {
      const table = getTable('posbah_demo_cars');
      const item = table.find(c => c.id === id);
      const filtered = table.filter(c => c.id !== id);
      saveTable('posbah_demo_cars', filtered);
      data = { success: true };
      if (item) logDemoActivity('DELETE_CAR', `Menghapus mobil ${item.name}`);
    }
  } else if (method === 'PATCH') {
    const payload = JSON.parse(config.data || '{}');
    const id = Number(parts[1]);

    if (parts[0] === 'pre-orders' && parts[2] === 'status') {
      const table = getTable('posbah_demo_transactions');
      const idx = table.findIndex(t => t.id === id);
      if (idx !== -1) {
        if (payload.orderStatus) table[idx].orderStatus = payload.orderStatus;
        if (payload.dpAmount !== undefined) table[idx].dpAmount = payload.dpAmount;
        if (payload.orderStatus === 'COMPLETED') {
          table[idx].status = 'COMPLETED';
          // Catat pelunasan pre-order sebagai pemasukan di keuangan
          const fins = getTable('posbah_demo_finances');
          const remainingAmt = (table[idx].total || 0) - (table[idx].dpAmount || 0);
          fins.push({
            id: Date.now(),
            type: 'INCOME',
            amount: remainingAmt > 0 ? remainingAmt : (table[idx].total || 0),
            description: `Pelunasan pre-order ${table[idx].receiptNumber} - ${table[idx].customerName || ''}`,
            date: new Date().toISOString(),
            status: 'PAID'
          });
          // Hapus piutang terkait pre-order ini jika ada
          const filteredFins = fins.filter(f => !(f.type === 'RECEIVABLE' && f.description && f.description.includes(table[idx].receiptNumber)));
          saveTable('posbah_demo_finances', filteredFins.length < fins.length ? filteredFins : fins);
          logDemoActivity('UPDATE_TRANSACTION', `Melunasi pre-order ${table[idx].receiptNumber}`);
        } else if (payload.orderStatus === 'DP_PAID') {
          // Catat DP sebagai pemasukan parsial
          const fins = getTable('posbah_demo_finances');
          fins.push({
            id: Date.now(),
            type: 'INCOME',
            amount: payload.dpAmount || table[idx].dpAmount || 0,
            description: `DP pre-order ${table[idx].receiptNumber} - ${table[idx].customerName || ''}`,
            date: new Date().toISOString(),
            status: 'PAID'
          });
          // Catat sisa sebagai piutang
          const remainingDebt = (table[idx].total || 0) - (payload.dpAmount || table[idx].dpAmount || 0);
          if (remainingDebt > 0) {
            fins.push({
              id: Date.now() + 1,
              type: 'RECEIVABLE',
              amount: remainingDebt,
              description: `Sisa piutang pre-order ${table[idx].receiptNumber} - ${table[idx].customerName || ''}`,
              date: new Date().toISOString(),
              status: 'PENDING'
            });
          }
          saveTable('posbah_demo_finances', fins);
          logDemoActivity('UPDATE_TRANSACTION', `Menerima DP untuk pre-order ${table[idx].receiptNumber}`);
        }
        saveTable('posbah_demo_transactions', table);
      }
      data = table[idx];
    }
  }

  // Fallback: jika route tidak tertangani oleh demo adapter, kembalikan data kosong
  // agar UI tidak error dan tidak ada kebocoran ke data production
  if (data === null || data === undefined) {
    if (method === 'GET') {
      data = [];
    } else {
      data = { success: true, _demo: true };
    }
  }

  return {
    data,
    status,
    statusText: 'OK',
    headers: {},
    config
  };
};

// ─────────────────────────────────────────────────────────────
// Offline Mode Helper Functions & Response Caching Interceptor
// ─────────────────────────────────────────────────────────────

function showOfflineToast(message) {
  let toast = document.getElementById('posbah-offline-toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'posbah-offline-toast';
    toast.style.position = 'fixed';
    toast.style.bottom = '24px';
    toast.style.left = '50%';
    toast.style.transform = 'translateX(-50%)';
    toast.style.background = 'rgba(30, 27, 75, 0.95)';
    toast.style.backdropFilter = 'blur(8px)';
    toast.style.border = '1px solid rgba(255, 255, 255, 0.15)';
    toast.style.color = '#FDE047';
    toast.style.padding = '12px 20px';
    toast.style.borderRadius = '16px';
    toast.style.fontSize = '0.85rem';
    toast.style.fontWeight = '600';
    toast.style.zIndex = '999999';
    toast.style.boxShadow = '0 10px 25px rgba(0, 0, 0, 0.3)';
    toast.style.display = 'flex';
    toast.style.alignItems = 'center';
    toast.style.gap = '8px';
    toast.style.transition = 'all 0.3s ease-in-out';
    document.body.appendChild(toast);
  }
  toast.innerHTML = `⚠️ <span>${message}</span>`;
  toast.style.opacity = '1';
  
  if (window.offlineToastTimeout) clearTimeout(window.offlineToastTimeout);
  window.offlineToastTimeout = setTimeout(() => {
    toast.style.opacity = '0';
  }, 4000);
}

async function handleOfflineRequest(config, route) {
  const method = config.method.toUpperCase();
  const parts = route.split('/');
  
  if (method === 'GET') {
    let data = null;
    let status = 200;
    const cacheKey = `posbah_cache_${parts[0]}`;
    const cachedData = localStorage.getItem(cacheKey);
    
    if (parts[0] === 'reports') {
      const txs = JSON.parse(localStorage.getItem('posbah_cache_transactions') || '[]').filter(t => t.status === 'COMPLETED');
      const fins = JSON.parse(localStorage.getItem('posbah_cache_finances') || '[]');
      const totalExpenses = fins.filter(f => f.type === 'EXPENSE' && f.status === 'PAID').reduce((sum, f) => sum + f.amount, 0);
      const pendingReceivables = fins.filter(f => f.type === 'RECEIVABLE' && f.status === 'PENDING').reduce((sum, f) => sum + f.amount, 0);
      
      const totalSales = txs.reduce((sum, t) => sum + (t.total || t.totalPrice || 0), 0);
      const todaySales = txs.filter(t => new Date(t.date || t.createdAt).toDateString() === new Date().toDateString()).reduce((sum, t) => sum + (t.total || t.totalPrice || 0), 0);
      
      data = {
        totalSales,
        totalExpenses,
        netIncome: totalSales - totalExpenses,
        pendingReceivables,
        todaySales,
        transactionCount: txs.length
      };
    } else if (parts[0] === 'payroll' && parts[1] === 'history') {
      data = JSON.parse(localStorage.getItem('posbah_cache_payroll_history') || '[]');
    } else if (parts[1] && !isNaN(Number(parts[1]))) {
      const id = Number(parts[1]);
      const list = JSON.parse(cachedData || '[]');
      data = list.find(item => item.id === id) || null;
      if (!data) status = 404;
    } else {
      data = JSON.parse(cachedData || '[]');
    }
    
    return {
      data,
      status,
      statusText: status === 200 ? 'OK' : 'Not Found',
      headers: {},
      config
    };
  }
  
  const payload = config.data ? JSON.parse(config.data) : {};
  const queue = JSON.parse(localStorage.getItem('posbah_offline_writes') || '[]');
  
  queue.push({
    url: config.url,
    method: config.method,
    data: payload,
    headers: config.headers,
    timestamp: Date.now()
  });
  localStorage.setItem('posbah_offline_writes', JSON.stringify(queue));
  
  const cacheKey = `posbah_cache_${parts[0]}`;
  let list = JSON.parse(localStorage.getItem(cacheKey) || '[]');
  let responseData = { success: true };
  
  if (method === 'POST') {
    const newId = Date.now();
    const newItem = { ...payload, id: newId, createdAt: new Date().toISOString() };
    
    if (parts[0] === 'transactions') {
      const num = list.length + 1;
      newItem.receiptNumber = `TX-OFFLINE-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${String(num).padStart(3, '0')}`;
      newItem.date = new Date().toISOString();
      
      const prodsKey = 'posbah_cache_products';
      const prods = JSON.parse(localStorage.getItem(prodsKey) || '[]');
      (newItem.items || []).forEach(item => {
        const pr = prods.find(p => p.id === Number(item.productId));
        if (pr) pr.stock -= item.quantity;
      });
      localStorage.setItem(prodsKey, JSON.stringify(prods));
      
      if (newItem.paymentMethod === 'HUTANG') {
        const finsKey = 'posbah_cache_finances';
        const fins = JSON.parse(localStorage.getItem(finsKey) || '[]');
        fins.push({
          id: Date.now() + 1,
          type: 'RECEIVABLE',
          amount: newItem.total,
          description: `Piutang (Hutang pelanggan) - ${newItem.customerName || newItem.receiptNumber}`,
          date: new Date().toISOString(),
          status: 'PENDING'
        });
        localStorage.setItem(finsKey, JSON.stringify(fins));
      }
    } else if (parts[0] === 'rentals') {
      newItem.status = 'ACTIVE';
      newItem.startDate = newItem.startDate || new Date().toISOString();
      const carsKey = 'posbah_cache_cars';
      const cars = JSON.parse(localStorage.getItem(carsKey) || '[]');
      const car = cars.find(c => c.id === Number(newItem.carId));
      if (car) car.status = 'RENTED';
      localStorage.setItem(carsKey, JSON.stringify(cars));
    }
    
    list.push(newItem);
    responseData = newItem;
  } else if (method === 'PUT' && parts[1]) {
    const id = Number(parts[1]);
    const idx = list.findIndex(item => item.id === id);
    if (idx !== -1) {
      list[idx] = { ...list[idx], ...payload };
      responseData = list[idx];
    }
  } else if (method === 'DELETE' && parts[1]) {
    const id = Number(parts[1]);
    list = list.filter(item => item.id !== id);
  }
  
  localStorage.setItem(cacheKey, JSON.stringify(list));
  showOfflineToast('Data disimpan secara lokal (Offline Mode). Data akan disinkronkan saat terhubung.');
  
  return {
    data: responseData,
    status: 200,
    statusText: 'OK',
    headers: {},
    config
  };
}

export async function syncOfflineWrites() {
  const queue = JSON.parse(localStorage.getItem('posbah_offline_writes') || '[]');
  if (queue.length === 0) return;
  if (window.isSyncingOffline) return;
  window.isSyncingOffline = true;
  
  console.log(`Syncing ${queue.length} offline changes...`);
  showOfflineToast(`Menyinkronkan ${queue.length} perubahan offline...`);
  
  const remaining = [];
  let successCount = 0;
  
  for (const item of queue) {
    try {
      await defaultAdapter({
        url: item.url,
        method: item.method,
        data: item.data ? JSON.stringify(item.data) : undefined,
        headers: {
          ...item.headers,
          'Content-Type': 'application/json',
          'x-offline-sync': 'true'
        }
      });
      successCount++;
    } catch (err) {
      console.error('Failed to sync offline item:', item, err);
      remaining.push(item);
    }
  }
  
  localStorage.setItem('posbah_offline_writes', JSON.stringify(remaining));
  window.isSyncingOffline = false;
  
  if (successCount > 0) {
    window.dispatchEvent(new CustomEvent('posbah_sync_complete', { detail: { count: successCount } }));
  }
}

// Intercept responses to cache successful GETs
api.interceptors.response.use(
  (response) => {
    const config = response.config;
    let pathname = '';
    try {
      pathname = new URL(config.url, config.baseURL || window.location.origin).pathname;
    } catch {
      pathname = config.url;
    }
    const route = pathname.replace(/^\/api/, '').replace(/^\//, '').replace(/\/$/, '');
    
    if (config.method.toUpperCase() === 'GET' && response.status === 200) {
      const storedUser = localStorage.getItem('posbah_user');
      let isDemo = false;
      if (storedUser) {
        try {
          const u = JSON.parse(storedUser);
          isDemo = u?.isDemo === true;
        } catch (_) {}
      }
      
      if (!isDemo && route !== 'auth/login') {
        const parts = route.split('/');
        if (parts.length === 1) {
          localStorage.setItem(`posbah_cache_${route}`, JSON.stringify(response.data));
        }
      }
    }
    return response;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default api;
