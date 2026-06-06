import React, { useState, useEffect } from 'react';
import {
  Car, Plus, Edit2, Trash2, Calendar, DollarSign,
  User, ClipboardList, CheckCircle, AlertTriangle, Search, RefreshCw, Printer, Settings, X
} from 'lucide-react';
import api from '../api';
import { useDemoBlock } from '../AuthContext';

export default function RentalMobil() {
  const { showDemoBlock, isDemo } = useDemoBlock();
  
  const [receiptSettings, setReceiptSettings] = useState(() => {
    try {
      const saved = localStorage.getItem('posbah_receipt_settings_rental');
      if (saved) return JSON.parse(saved);
    } catch (e) {}
    return {
      storeName: 'POSBAH RENTAL MOBIL',
      subheader: 'Jasa Sewa Mobil Terbaik & Cepat',
      footer: "Terima kasih atas kunjungan Anda!\nHarap berkendara dengan aman.\n- POSBAH -"
    };
  });

  const [settingsModalOpen, setSettingsModalOpen] = useState(false);
  const [tempStoreName, setTempStoreName] = useState(receiptSettings.storeName);
  const [tempSubheader, setTempSubheader] = useState(receiptSettings.subheader);
  const [tempFooter, setTempFooter] = useState(receiptSettings.footer);

  useEffect(() => {
    if (settingsModalOpen) {
      setTempStoreName(receiptSettings.storeName);
      setTempSubheader(receiptSettings.subheader);
      setTempFooter(receiptSettings.footer);
    }
  }, [settingsModalOpen, receiptSettings]);

  const printTestReceipt = (size) => {
    const dummyRental = {
      id: "TEST",
      createdAt: new Date().toISOString(),
      customerName: "Pelanggan Demo",
      startDate: new Date().toISOString(),
      endDate: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString(),
      totalPrice: 500000,
      paymentMethod: "CASH",
      status: "ACTIVE",
      car: {
        name: "Toyota Avanza",
        plateNumber: "B 1234 ABC",
        type: "MPV",
        pricePerDay: 250000
      }
    };
    
    const origSettings = localStorage.getItem('posbah_receipt_settings_rental');
    localStorage.setItem('posbah_receipt_settings_rental', JSON.stringify({
      storeName: tempStoreName,
      subheader: tempSubheader,
      footer: tempFooter
    }));

    printViaSystem(dummyRental, size);

    if (origSettings) {
      localStorage.setItem('posbah_receipt_settings_rental', origSettings);
    } else {
      localStorage.removeItem('posbah_receipt_settings_rental');
    }
  };

  const [activeTab, setActiveTab] = useState('POS'); // 'POS', 'CARS', 'HISTORY'
  const [cars, setCars] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [rentals, setRentals] = useState([]);
  const [loading, setLoading] = useState(false);

  // Car Form State
  const [isCarModalOpen, setIsCarModalOpen] = useState(false);
  const [carFormData, setCarFormData] = useState({ id: null, name: '', plateNumber: '', type: 'MPV', pricePerDay: '', status: 'AVAILABLE' });

  // Rental POS State
  const [selectedCarId, setSelectedCarId] = useState('');
  const [selectedCustomerId, setSelectedCustomerId] = useState('');
  const [newCustomerName, setNewCustomerName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [identityText, setIdentityText] = useState('');
  const [compressing, setCompressing] = useState(false);
  const [viewIdentity, setViewIdentity] = useState(null);

  // Return Modal State
  const [isReturnModalOpen, setIsReturnModalOpen] = useState(false);
  const [selectedRental, setSelectedRental] = useState(null);
  const [actualReturnDate, setActualReturnDate] = useState('');
  const [lateFee, setLateFee] = useState(0);

  // Printing States
  const [printRental, setPrintRental] = useState(null);
  const [paperSize, setPaperSize] = useState('58mm');
  const [printingBluetooth, setPrintingBluetooth] = useState(false);

  const printViaSystem = (rental, size) => {
    const width = size === '58mm' ? '58mm' : '80mm';
    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      alert('Pencetakan sistem (browser) tidak didukung di APK. Silakan gunakan printer Bluetooth.');
      return;
    }
    
    const start = new Date(rental.startDate);
    const end = new Date(rental.endDate);
    const diffTime = Math.abs(end - start);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) || 1;
    const basicCost = (rental.car?.pricePerDay || 0) * diffDays;
    const grandTotal = Number(rental.totalPrice || 0) + Number(rental.status === 'RETURNED' ? (rental.lateFee || 0) : 0);

    let settings = receiptSettings;
    try {
      const saved = localStorage.getItem('posbah_receipt_settings_rental');
      if (saved) settings = JSON.parse(saved);
    } catch (e) {}

    const storeName = settings?.storeName || 'POSBAH RENTAL MOBIL';
    const subheader = settings?.subheader || 'Jasa Sewa Mobil Terbaik & Cepat';
    const footer = settings?.footer || "Terima kasih atas kunjungan Anda!\nHarap berkendara dengan aman.\n- POSBAH -";

    const content = `
      <html>
        <head>
          <title>Cetak Struk RENT-${rental.id}</title>
          <style>
            @page { size: ${width} auto; margin: 0; }
            body {
              font-family: 'Courier New', Courier, monospace;
              width: ${width};
              margin: 0;
              padding: 6px;
              font-size: 11px;
              line-height: 1.3;
              color: #000;
              background: #fff;
            }
            .c { text-align: center; }
            .r { text-align: right; }
            .b { font-weight: bold; }
            hr { border: none; border-top: 1px dashed #000; margin: 6px 0; }
            .title { font-size: 14px; font-weight: bold; margin-bottom: 2px; }
            .subtitle { font-size: 9px; margin-bottom: 4px; }
            table { width: 100%; border-collapse: collapse; margin: 4px 0; }
            td { vertical-align: top; padding: 2px 0; }
            .footer { margin-top: 10px; font-size: 9px; }
          </style>
        </head>
        <body>
          <div class="c title">${storeName}</div>
          <div class="c subtitle">${subheader}</div>
          <hr>
          <div>No. Sewa : RENT-${rental.id}</div>
          <div>Tanggal  : ${new Date(rental.createdAt || Date.now()).toLocaleString('id-ID')}</div>
          <div>Penyewa  : ${rental.customerName}</div>
          ${rental.customer?.phone ? `<div>No. HP   : ${rental.customer.phone}</div>` : ''}
          <hr>
          <div class="b">Mobil    : ${rental.car?.name}</div>
          <div>Plat No  : ${rental.car?.plateNumber || '-'}</div>
          <div>Tipe     : ${rental.car?.type || '-'}</div>
          <hr>
          <div>Mulai    : ${new Date(rental.startDate).toLocaleDateString('id-ID')}</div>
          <div>Tenggat  : ${new Date(rental.endDate).toLocaleDateString('id-ID')}</div>
          ${rental.status === 'RETURNED' && rental.actualReturnDate ? `<div>Kembali  : ${new Date(rental.actualReturnDate).toLocaleDateString('id-ID')}</div>` : ''}
          <div>Durasi   : ${diffDays} Hari</div>
          <hr>
          <table>
            <tr><td>Tarif Sewa</td><td class="r">Rp ${Number(rental.car?.pricePerDay || 0).toLocaleString('id-ID')}/hr</td></tr>
            <tr><td>Biaya Dasar</td><td class="r">Rp ${basicCost.toLocaleString('id-ID')}</td></tr>
            ${rental.lateFee > 0 ? `<tr><td>Denda Telat</td><td class="r">Rp ${Number(rental.lateFee).toLocaleString('id-ID')}</td></tr>` : ''}
          </table>
          <hr>
          <table>
            <tr class="b"><td>GRAND TOTAL</td><td class="r">Rp ${grandTotal.toLocaleString('id-ID')}</td></tr>
            <tr><td>Metode Bayar</td><td class="r">${String(rental.paymentMethod || 'CASH').toUpperCase()}</td></tr>
            <tr><td>Status Sewa</td><td class="r">${rental.status === 'ACTIVE' ? 'DISEWA (AKTIF)' : 'KEMBALI (SELESAI)'}</td></tr>
          </table>
          <hr>
          <div class="c footer">
            ${footer.replace(/\n/g, '<br>')}
          </div>
          <script>
            window.onload = function() {
              window.print();
              setTimeout(function() { window.close(); }, 500);
            };
          </script>
        </body>
      </html>
    `;
    printWindow.document.write(content);
    printWindow.document.close();
  };

  const printViaBluetooth = async (rental, size) => {
    if (!navigator.bluetooth) {
      alert('Browser Anda tidak mendukung Web Bluetooth. Silakan gunakan opsi Cetak Sistem (Browser).');
      return;
    }
    
    setPrintingBluetooth(true);
    try {
      const device = await navigator.bluetooth.requestDevice({
        acceptAllDevices: true,
        optionalServices: ['000018f0-0000-1000-8000-00805f9b34fb']
      });

      const server = await device.gatt.connect();
      
      let service;
      try {
        service = await server.getPrimaryService('000018f0-0000-1000-8000-00805f9b34fb');
      } catch (e) {
        const services = await server.getPrimaryServices();
        if (services.length > 0) {
          service = services[0];
        } else {
          throw new Error('Gagal menemukan layanan printer Bluetooth.');
        }
      }

      const characteristics = await service.getCharacteristics();
      const writeChar = characteristics.find(c => c.properties.write || c.properties.writeWithoutResponse);
      if (!writeChar) {
        throw new Error('Karakteristik menulis tidak ditemukan di printer Bluetooth.');
      }

      const charLimit = size === '58mm' ? 32 : 48;
      const encoder = new TextEncoder();
      
      const ESC = '\x1b';
      const RESET = ESC + '@';
      const CENTER = ESC + 'a\x01';
      const LEFT = ESC + 'a\x00';
      const DOUBLE_HEIGHT = ESC + '!' + '\x10';
      const NORMAL = ESC + '!' + '\x00';
      const BOLD_ON = ESC + 'E\x01';
      const BOLD_OFF = ESC + 'E\x00';
      const LINE_FEED = '\n';

      const start = new Date(rental.startDate);
      const end = new Date(rental.endDate);
      const diffTime = Math.abs(end - start);
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) || 1;
      const basicCost = (rental.car?.pricePerDay || 0) * diffDays;
      const grandTotal = Number(rental.totalPrice || 0) + Number(rental.status === 'RETURNED' ? (rental.lateFee || 0) : 0);

      const padText = (left, right) => {
        const spaceNeeded = charLimit - left.length - right.length;
        return left + ' '.repeat(spaceNeeded > 0 ? spaceNeeded : 1) + right;
      };

      let settings = receiptSettings;
      try {
        const saved = localStorage.getItem('posbah_receipt_settings_rental');
        if (saved) settings = JSON.parse(saved);
      } catch (e) {}

      const storeName = settings?.storeName || 'POSBAH RENTAL MOBIL';
      const subheader = settings?.subheader || 'Jasa Sewa Mobil Terbaik';
      const footer = settings?.footer || "Terima kasih atas kunjungan Anda!\nHarap berkendara dengan aman.\n- POSBAH -";

      let d = '';
      d += RESET;
      d += CENTER + DOUBLE_HEIGHT + BOLD_ON + storeName + NORMAL + LINE_FEED;
      d += CENTER + subheader + LINE_FEED;
      d += CENTER + '-'.repeat(charLimit) + LINE_FEED;

      d += LEFT;
      d += `No. Sewa : RENT-${rental.id}` + LINE_FEED;
      d += `Tanggal  : ${new Date(rental.createdAt || Date.now()).toLocaleString('id-ID')}` + LINE_FEED;
      d += `Penyewa  : ${rental.customerName}` + LINE_FEED;
      if (rental.customer?.phone) {
        d += `No. HP   : ${rental.customer.phone}` + LINE_FEED;
      }
      d += '-'.repeat(charLimit) + LINE_FEED;

      d += BOLD_ON + `Mobil    : ${rental.car?.name}` + BOLD_OFF + LINE_FEED;
      d += `Plat No  : ${rental.car?.plateNumber || '-'}` + LINE_FEED;
      d += `Tipe     : ${rental.car?.type || '-'}` + LINE_FEED;
      d += '-'.repeat(charLimit) + LINE_FEED;

      d += `Mulai    : ${new Date(rental.startDate).toLocaleDateString('id-ID')}` + LINE_FEED;
      d += `Tenggat  : ${new Date(rental.endDate).toLocaleDateString('id-ID')}` + LINE_FEED;
      if (rental.status === 'RETURNED' && rental.actualReturnDate) {
        d += `Kembali  : ${new Date(rental.actualReturnDate).toLocaleDateString('id-ID')}` + LINE_FEED;
      }
      d += `Durasi   : ${diffDays} Hari` + LINE_FEED;
      d += '-'.repeat(charLimit) + LINE_FEED;

      d += padText('Tarif Sewa', `Rp ${Number(rental.car?.pricePerDay || 0).toLocaleString('id-ID')}/hr`) + LINE_FEED;
      d += padText('Biaya Dasar', `Rp ${basicCost.toLocaleString('id-ID')}`) + LINE_FEED;
      if (rental.lateFee > 0) {
        d += padText('Denda Telat', `Rp ${Number(rental.lateFee).toLocaleString('id-ID')}`) + LINE_FEED;
      }
      d += '-'.repeat(charLimit) + LINE_FEED;

      d += BOLD_ON + padText('GRAND TOTAL', `Rp ${grandTotal.toLocaleString('id-ID')}`) + BOLD_OFF + LINE_FEED;
      d += padText('Metode Bayar', String(rental.paymentMethod || 'CASH').toUpperCase()) + LINE_FEED;
      d += padText('Status Sewa', rental.status === 'ACTIVE' ? 'DISEWA (AKTIF)' : 'KEMBALI (SELESAI)') + LINE_FEED;
      d += '-'.repeat(charLimit) + LINE_FEED;

      footer.split('\n').forEach(line => {
        if (line.trim()) d += CENTER + line.trim() + LINE_FEED;
      });
      d += LINE_FEED + LINE_FEED + LINE_FEED;

      const rawBytes = encoder.encode(d);
      const chunkSize = 20;
      for (let i = 0; i < rawBytes.length; i += chunkSize) {
        const chunk = rawBytes.slice(i, i + chunkSize);
        await writeChar.writeValue(chunk);
      }

      alert('Berhasil mengirim data ke printer Bluetooth!');
      device.gatt.disconnect();
    } catch (err) {
      console.error(err);
      alert(`Gagal koneksi printer Bluetooth: ${err.message || err}`);
    } finally {
      setPrintingBluetooth(false);
    }
  };

  useEffect(() => {
    fetchCars();
    fetchCustomers();
    fetchRentals();
  }, []);

  const fetchCars = async () => {
    try {
      const res = await api.get('/cars');
      setCars(res.data);
    } catch (err) {
      console.error('Failed to fetch cars', err);
    }
  };

  const fetchCustomers = async () => {
    try {
      const res = await api.get('/customers');
      setCustomers(res.data);
    } catch (err) {
      console.error('Failed to fetch customers', err);
    }
  };

  const fetchRentals = async () => {
    try {
      const res = await api.get('/rentals');
      setRentals(res.data);
    } catch (err) {
      console.error('Failed to fetch rentals', err);
    }
  };

  // Car CRUD
  const handleOpenCarModal = (car = null) => {
    if (car) {
      setCarFormData(car);
    } else {
      setCarFormData({ id: null, name: '', plateNumber: '', type: 'MPV', pricePerDay: '', status: 'AVAILABLE' });
    }
    setIsCarModalOpen(true);
  };

  const handleSaveCar = async (e) => {
    e.preventDefault();
    try {
      if (carFormData.id) {
        await api.put(`/cars/${carFormData.id}`, carFormData);
      } else {
        await api.post('/cars', carFormData);
      }
      setIsCarModalOpen(false);
      fetchCars();
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.error || 'Gagal menyimpan data mobil.');
    }
  };

  const handleDeleteCar = async (id) => {
    if (window.confirm('Yakin ingin menghapus mobil ini?')) {
      try {
        await api.delete(`/cars/${id}`);
        fetchCars();
      } catch (err) {
        alert(err.response?.data?.error || 'Gagal menghapus mobil.');
      }
    }
  };

  // Rent Calculation
  const calculateDays = () => {
    if (!startDate || !endDate) return 0;
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end - start);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays || 1; // Minimal 1 hari
  };

  const getSelectedCarPrice = () => {
    const car = cars.find(c => c.id === Number(selectedCarId));
    return car ? car.pricePerDay : 0;
  };

  const calculateTotalPrice = () => {
    return calculateDays() * getSelectedCarPrice();
  };

  const handleIdentityUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setCompressing(true);
    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;
        const maxDim = 600;

        if (width > maxDim || height > maxDim) {
          if (width > height) {
            height = Math.round((height * maxDim) / width);
            width = maxDim;
          } else {
            width = Math.round((width * maxDim) / height);
            height = maxDim;
          }
        }

        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);

        const compressedBase64 = canvas.toDataURL('image/jpeg', 0.6);
        setIdentityText(compressedBase64);
        setCompressing(false);
      };
      img.src = event.target.result;
    };
    reader.readAsDataURL(file);
  };

  // Rental Checkout
  const handleRentalCheckout = async (e) => {
    e.preventDefault();
    if (!selectedCarId) return alert('Pilih mobil terlebih dahulu!');
    let custName = newCustomerName;
    if (selectedCustomerId) {
      const cust = customers.find(c => c.id === Number(selectedCustomerId));
      if (cust) custName = cust.name;
    }
    if (!custName) return alert('Masukkan atau pilih nama pelanggan!');

    setLoading(true);
    try {
      const res = await api.post('/rentals', {
        carId: Number(selectedCarId),
        customerId: selectedCustomerId ? Number(selectedCustomerId) : null,
        customerName: custName,
        startDate,
        endDate,
        totalPrice: calculateTotalPrice(),
        paymentMethod,
        identityText: identityText || null
      });

      // Reset
      setSelectedCarId('');
      setSelectedCustomerId('');
      setNewCustomerName('');
      setStartDate('');
      setEndDate('');
      setIdentityText('');
      
      alert('Penyewaan mobil berhasil diproses!');
      fetchCars();
      fetchRentals();
      
      // Auto open print modal for the newly created transaction
      if (res.data) {
        setPrintRental(res.data);
      }
      
      setActiveTab('HISTORY');
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.error || 'Gagal checkout sewa mobil.');
    } finally {
      setLoading(false);
    }
  };

  // Return Processing
  const handleOpenReturnModal = (rental) => {
    setSelectedRental(rental);
    setActualReturnDate(new Date().toISOString().substring(0, 10));
    
    // Auto-calculate denda
    const end = new Date(rental.endDate);
    const today = new Date();
    let lateFeeVal = 0;
    if (today > end) {
      const diffTime = Math.abs(today - end);
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      lateFeeVal = diffDays * rental.car.pricePerDay * 1.5; // denda 1.5 kali tarif
    }
    setLateFee(lateFeeVal);
    setIsReturnModalOpen(true);
  };

  const handleProcessReturn = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await api.post(`/rentals/${selectedRental.id}/return`, {
        actualReturnDate,
        lateFee,
        paymentMethod
      });
      setIsReturnModalOpen(false);
      fetchCars();
      fetchRentals();
      alert('Mobil berhasil dikembalikan!');

      // Auto open print modal for return transaction (including return dates / late fee denda)
      if (res.data) {
        setPrintRental(res.data);
      }
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.error || 'Gagal memproses pengembalian.');
    } finally {
      setLoading(false);
    }
  };

  const fmt = (n) => Number(n || 0).toLocaleString('id-ID');

  return (
    <div className="page-container max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 mb-20 md:mb-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6 border-b border-gray-100 pb-5">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-indigo-50 text-indigo-600 rounded-2xl shadow-sm">
            <Car size={28} className="animate-pulse" />
          </div>
          <div>
            <h1 className="text-xl md:text-2xl font-black text-gray-800 tracking-tight m-0">POS Rental Mobil</h1>
            <p className="text-xs text-gray-400 font-semibold mt-0.5">Kelola penyewaan kendaraan dengan mudah & cepat</p>
          </div>
        </div>

        {/* Settings button */}
        <button
          onClick={() => setSettingsModalOpen(true)}
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '8px 12px',
            borderRadius: '10px',
            border: '1px solid #d1d5db',
            background: 'white',
            color: '#374151',
            cursor: 'pointer',
            fontSize: '0.85rem',
            fontWeight: 700,
            gap: '6px'
          }}
          title="Pengaturan Struk"
        >
          <Settings size={16} /> Pengaturan Struk
        </button>
      </div>

      {/* Tabs Menu */}
      <div className="bg-gray-100/80 dark:bg-gray-800/40 p-1.5 rounded-2xl flex gap-1.5 mb-6 w-full md:w-fit overflow-x-auto scrollbar-none">
        <button
          onClick={() => setActiveTab('POS')}
          className={`flex-1 md:flex-none flex items-center justify-center gap-2 py-2.5 px-5 rounded-xl text-sm font-bold transition-all duration-250 whitespace-nowrap cursor-pointer ${
            activeTab === 'POS'
              ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/15 translate-y-[-1px]'
              : 'text-gray-500 hover:text-gray-900 hover:bg-gray-200/50'
          }`}
        >
          <ClipboardList size={16} />
          Sewa Mobil (POS)
        </button>
        <button
          onClick={() => setActiveTab('CARS')}
          className={`flex-1 md:flex-none flex items-center justify-center gap-2 py-2.5 px-5 rounded-xl text-sm font-bold transition-all duration-250 whitespace-nowrap cursor-pointer ${
            activeTab === 'CARS'
              ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/15 translate-y-[-1px]'
              : 'text-gray-500 hover:text-gray-900 hover:bg-gray-200/50'
          }`}
        >
          <Car size={16} />
          Kelola Mobil
        </button>
        <button
          onClick={() => setActiveTab('HISTORY')}
          className={`flex-1 md:flex-none flex items-center justify-center gap-2 py-2.5 px-5 rounded-xl text-sm font-bold transition-all duration-250 whitespace-nowrap cursor-pointer ${
            activeTab === 'HISTORY'
              ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/15 translate-y-[-1px]'
              : 'text-gray-500 hover:text-gray-900 hover:bg-gray-200/50'
          }`}
        >
          <Calendar size={16} />
          Riwayat & Pengembalian
        </button>
      </div>

      {/* Tab 1: Rental POS */}
      {activeTab === 'POS' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Form */}
          <div className="bg-white border border-gray-100 shadow-sm p-6 rounded-2xl lg:col-span-2 flex flex-col gap-6">
            <div>
              <h2 className="text-lg font-black text-gray-800 m-0">Form Transaksi Sewa</h2>
              <p className="text-xs text-gray-400 mt-1 font-semibold">Isi informasi penyewa dan tentukan masa sewa mobil</p>
            </div>
            
            <form onSubmit={handleRentalCheckout} className="flex flex-col gap-5">
              
              {/* Select Car */}
              <div className="form-group flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-400 uppercase tracking-wider">Mobil Tersedia</label>
                <div className="relative">
                  <select
                    value={selectedCarId}
                    onChange={(e) => setSelectedCarId(e.target.value)}
                    required
                    className="w-full bg-white border border-gray-200 rounded-xl px-4 py-3 text-sm md:text-base text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-12 cursor-pointer appearance-none"
                  >
                    <option value="">-- Pilih Mobil --</option>
                    {cars.filter(c => c.status === 'AVAILABLE').map(c => (
                      <option key={c.id} value={c.id}>{c.name} ({c.plateNumber}) - Rp {fmt(c.pricePerDay)}/hari</option>
                    ))}
                  </select>
                  <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400">
                    <Car size={16} />
                  </div>
                </div>
              </div>

              {/* Select Customer */}
              <div className="form-group flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-400 uppercase tracking-wider">Pelanggan Terdaftar</label>
                <div className="relative">
                  <select
                    value={selectedCustomerId}
                    onChange={(e) => {
                      setSelectedCustomerId(e.target.value);
                      if (e.target.value) setNewCustomerName('');
                    }}
                    className="w-full bg-white border border-gray-200 rounded-xl px-4 py-3 text-sm md:text-base text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-12 cursor-pointer appearance-none"
                  >
                    <option value="">-- Pilih Pelanggan (Opsional) --</option>
                    {customers.map(cust => (
                      <option key={cust.id} value={cust.id}>{cust.name} ({cust.phone || 'No HP -'})</option>
                    ))}
                  </select>
                  <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400">
                    <User size={16} />
                  </div>
                </div>
                
                {!selectedCustomerId && (
                  <div className="mt-2">
                    <span className="text-xs text-gray-400 font-semibold">Atau ketik nama pelanggan baru:</span>
                    <input
                      type="text"
                      placeholder="Nama Pelanggan Baru..."
                      value={newCustomerName}
                      onChange={(e) => setNewCustomerName(e.target.value)}
                      required={!selectedCustomerId}
                      className="w-full bg-white border border-gray-200 rounded-xl px-4 py-3 text-sm md:text-base text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-12 mt-1.5"
                    />
                  </div>
                )}
              </div>

              {/* Period Start - End */}
              <div className="grid grid-cols-2 gap-3">
                <div className="form-group flex flex-col gap-1.5">
                  <label className="text-[10px] sm:text-xs font-bold text-gray-400 uppercase tracking-wider truncate">Mulai Sewa</label>
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    required
                    className="w-full bg-white border border-gray-200 rounded-xl px-2 sm:px-4 py-2.5 sm:py-3 text-xs sm:text-base text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-11 sm:h-12"
                  />
                </div>
                <div className="form-group flex flex-col gap-1.5">
                  <label className="text-[10px] sm:text-xs font-bold text-gray-400 uppercase tracking-wider truncate">Akhir Sewa</label>
                  <input
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    required
                    min={startDate}
                    className="w-full bg-white border border-gray-200 rounded-xl px-2 sm:px-4 py-2.5 sm:py-3 text-xs sm:text-base text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-11 sm:h-12"
                  />
                </div>
              </div>

              {/* Payment Method */}
              <div className="form-group flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-400 uppercase tracking-wider">Metode Pembayaran</label>
                <div className="grid grid-cols-3 gap-2 bg-gray-50/50 p-1.5 rounded-2xl border border-gray-100">
                  {['CASH', 'TRANSFER', 'HUTANG'].map(method => (
                    <button
                      key={method}
                      type="button"
                      onClick={() => setPaymentMethod(method)}
                      className={`py-2.5 px-3 rounded-xl text-xs font-black text-center transition-all duration-200 cursor-pointer ${
                        paymentMethod === method
                          ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-600/10'
                          : 'bg-white border border-gray-200/50 text-gray-500 hover:text-gray-800 hover:bg-gray-50'
                      }`}
                    >
                      {method}
                    </button>
                  ))}
                </div>
              </div>

              {/* Bukti Identitas */}
              <div className="form-group flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-400 uppercase tracking-wider">
                  Bukti Identitas (KTP/SIM/Passport)
                </label>
                <div className="relative border-2 border-dashed border-gray-200 hover:border-indigo-300 rounded-2xl p-6 bg-gray-50/30 hover:bg-gray-50 transition-all duration-200 cursor-pointer flex flex-col items-center justify-center gap-2 text-center group">
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleIdentityUpload}
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
                  />
                  <div className="p-3 bg-white rounded-2xl shadow-sm text-indigo-600 group-hover:scale-110 transition-transform duration-200">
                    <Plus size={20} />
                  </div>
                  <div>
                    <span className="block text-sm font-bold text-gray-700">Pilih berkas identitas</span>
                    <span className="block text-[10px] text-gray-400 font-semibold mt-0.5">Maksimal resolusi diperkecil otomatis</span>
                  </div>
                </div>
                
                {compressing && <span className="text-xs text-gray-400 animate-pulse font-semibold mt-1">Mengompresi dokumen...</span>}
                
                {identityText && (
                  <div className="mt-3 border border-emerald-100 rounded-2xl p-3 bg-emerald-50/40 flex items-center gap-3">
                    <img src={identityText} alt="Dokumen Preview" draggable="false" className="w-16 h-12 object-cover rounded-xl shadow-sm border border-emerald-100" />
                    <div className="flex-1 min-w-0">
                      <div className="text-xs font-black text-emerald-800">Sukses Diunggah!</div>
                      <div className="text-[10px] text-emerald-600 font-bold mt-0.5">{Math.round(identityText.length / 1024)} KB</div>
                    </div>
                    <button
                      type="button"
                      onClick={() => setIdentityText('')}
                      className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-600 text-xs font-bold rounded-xl border border-rose-100 transition-all cursor-pointer"
                    >
                      Hapus
                    </button>
                  </div>
                )}
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-black py-4 px-6 rounded-2xl shadow-lg shadow-indigo-600/10 active:scale-[0.98] transition-all duration-200 text-center flex items-center justify-center gap-2 h-13 cursor-pointer mt-2 disabled:bg-gray-300 disabled:shadow-none"
              >
                {loading ? 'Memproses...' : 'Proses Sewa & Cetak Struk'}
              </button>
            </form>
          </div>

          {/* Receipt Preview */}
          <div className="bg-white border border-gray-100 shadow-sm p-6 rounded-2xl relative flex flex-col gap-4 h-fit">
            {/* Top decorative line */}
            <div className="absolute top-0 left-0 right-0 h-1.5 bg-indigo-600 rounded-t-2xl"></div>
            
            <div className="flex items-center gap-2 pb-3 border-b border-gray-100">
              <ClipboardList size={18} className="text-indigo-600" />
              <h2 className="text-base font-extrabold text-gray-800 m-0">Rincian Pembayaran</h2>
            </div>
            
            {selectedCarId && startDate && endDate ? (
              <div className="flex flex-col gap-3.5">
                <div className="flex justify-between items-center py-0.5">
                  <span className="text-gray-400 text-xs font-bold uppercase tracking-wider">Mobil Pilihan</span>
                  <span className="font-extrabold text-gray-700 text-sm">
                    {cars.find(c => c.id === Number(selectedCarId))?.name}
                  </span>
                </div>
                <div className="flex justify-between items-center py-0.5">
                  <span className="text-gray-400 text-xs font-bold uppercase tracking-wider">Tarif Sewa</span>
                  <span className="font-extrabold text-gray-700 text-sm">
                    Rp {fmt(getSelectedCarPrice())} <span className="text-[10px] text-gray-400 font-semibold uppercase">/ hari</span>
                  </span>
                </div>
                <div className="flex justify-between items-center py-0.5">
                  <span className="text-gray-400 text-xs font-bold uppercase tracking-wider">Durasi Sewa</span>
                  <span className="font-extrabold text-indigo-600 text-xs bg-indigo-50 px-2.5 py-1 rounded-xl">
                    {calculateDays()} Hari
                  </span>
                </div>
                
                {/* Dashed divider */}
                <div className="border-t border-dashed border-gray-250 my-1"></div>
                
                <div className="flex justify-between items-center py-0.5">
                  <span className="font-black text-gray-800 text-sm uppercase tracking-wide">Total Tagihan</span>
                  <span className="font-black text-emerald-600 text-base md:text-lg">
                    Rp {fmt(calculateTotalPrice())}
                  </span>
                </div>
                <div className="flex justify-between items-center py-0.5">
                  <span className="text-gray-400 text-xs font-bold uppercase tracking-wider">Metode Bayar</span>
                  <span className="font-extrabold text-[10px] bg-gray-100 text-gray-600 px-2.5 py-1 rounded-lg uppercase tracking-wide">
                    {paymentMethod}
                  </span>
                </div>
              </div>
            ) : (
              <div className="text-center py-12 flex flex-col items-center justify-center gap-2">
                <Car size={32} className="text-gray-300 animate-pulse" />
                <p className="text-xs text-gray-400 font-semibold max-w-[200px] leading-relaxed">
                  Silakan pilih mobil dan tanggal sewa untuk melihat rincian tagihan.
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab 2: Manage Cars */}
      {activeTab === 'CARS' && (
        <div className="flex flex-col gap-6">
          <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-3">
            <div>
              <h2 className="text-lg font-black text-gray-800 m-0">Kelola Unit Kendaraan</h2>
              <p className="text-xs text-gray-400 mt-1 font-semibold">Tambah, perbarui, dan hapus unit armada persewaan</p>
            </div>
            <button
              className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-2.5 px-4 rounded-xl shadow-md shadow-indigo-600/10 active:scale-[0.98] transition-all text-sm flex items-center justify-center gap-1.5 cursor-pointer"
              onClick={() => handleOpenCarModal()}
            >
              <Plus size={16} /> Tambah Mobil
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {cars.map(car => (
              <div key={car.id} className="bg-white border border-gray-100 hover:border-indigo-100/50 rounded-2xl p-5 shadow-sm hover:shadow-md transition-all duration-300 flex flex-col justify-between group relative overflow-hidden">
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <div className="p-3 bg-indigo-50/80 text-indigo-600 rounded-2xl group-hover:bg-indigo-600 group-hover:text-white transition-all duration-300">
                      <Car size={22} />
                    </div>
                    <span className={`text-[10px] font-black px-2.5 py-1 rounded-full uppercase tracking-wide ${
                      car.status === 'AVAILABLE'
                        ? 'bg-emerald-50 text-emerald-600'
                        : car.status === 'RENTED'
                        ? 'bg-amber-50 text-amber-600'
                        : 'bg-rose-50 text-rose-600'
                    }`}>
                      {car.status === 'AVAILABLE' ? 'Tersedia' : car.status === 'RENTED' ? 'Disewa' : 'Servis'}
                    </span>
                  </div>
                  
                  <h3 className="text-base font-black text-gray-800 group-hover:text-indigo-600 transition-colors duration-200 m-0">{car.name}</h3>
                  <span className="inline-block text-[10px] font-bold text-gray-400 bg-gray-50 px-2 py-0.5 rounded border border-gray-100 mt-1">{car.plateNumber}</span>
                  
                  <div className="flex flex-col gap-2 mt-4 pt-4 border-t border-gray-55">
                    <div className="flex justify-between items-center text-xs">
                      <span className="text-gray-400 font-bold uppercase tracking-wider text-[10px]">Tipe</span>
                      <span className="font-extrabold text-gray-700">{car.type}</span>
                    </div>
                    <div className="flex justify-between items-center text-xs">
                      <span className="text-gray-400 font-bold uppercase tracking-wider text-[10px]">Tarif Sewa</span>
                      <span className="font-black text-emerald-600">Rp {fmt(car.pricePerDay)} <span className="text-[10px] text-gray-400 font-normal">/ Hari</span></span>
                    </div>
                  </div>
                </div>

                <div className="flex gap-2 mt-5 pt-3 border-t border-gray-50">
                  <button
                    onClick={() => handleOpenCarModal(car)}
                    className="flex-1 flex items-center justify-center gap-1.5 py-2 px-3 text-xs font-bold text-indigo-600 bg-indigo-50/50 hover:bg-indigo-50 border border-indigo-100 rounded-xl transition-all cursor-pointer"
                  >
                    <Edit2 size={13} /> Edit
                  </button>
                  <button
                    onClick={() => handleDeleteCar(car.id)}
                    className="flex-1 flex items-center justify-center gap-1.5 py-2 px-3 text-xs font-bold text-rose-600 bg-rose-50/50 hover:bg-rose-50 border border-rose-100 rounded-xl transition-all cursor-pointer"
                  >
                    <Trash2 size={13} /> Hapus
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tab 3: History & Return */}
      {activeTab === 'HISTORY' && (
        <div className="bg-white border border-gray-100 shadow-sm p-6 rounded-2xl flex flex-col gap-5">
          <div>
            <h2 className="text-lg font-black text-gray-800 m-0">Riwayat Sewa Mobil</h2>
            <p className="text-xs text-gray-400 mt-1 font-semibold">Pantau status penyewaan dan verifikasi pengembalian unit</p>
          </div>

          {/* Desktop Table View */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-gray-400 font-bold text-xs uppercase tracking-wider">
                  <th className="pb-3 pt-1 px-4">Penyewa</th>
                  <th className="pb-3 pt-1 px-4">Mobil</th>
                  <th className="pb-3 pt-1 px-4">Mulai Sewa</th>
                  <th className="pb-3 pt-1 px-4">Tenggat</th>
                  <th className="pb-3 pt-1 px-4">Total Biaya</th>
                  <th className="pb-3 pt-1 px-4">Status</th>
                  <th className="pb-3 pt-1 px-4 text-center">Aksi</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rentals.map(rental => (
                  <tr key={rental.id} className="hover:bg-gray-50/40 transition-colors">
                    <td className="py-4 px-4">
                      <div className="font-extrabold text-gray-800">{rental.customerName}</div>
                      {rental.customer?.phone && <div className="text-xs text-gray-400 mt-0.5 font-medium">{rental.customer.phone}</div>}
                      {rental.identityText ? (
                        <button
                          onClick={() => setViewIdentity(rental.identityText)}
                          className="flex items-center gap-1 mt-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-600 border border-indigo-100 rounded-lg py-1 px-2.5 text-[10px] font-black cursor-pointer transition-all"
                        >
                          👁️ Lihat Identitas
                        </button>
                      ) : (
                        <div className="text-[10px] text-gray-300 font-semibold mt-1">Tidak ada identitas</div>
                      )}
                    </td>
                    <td className="py-4 px-4">
                      <div className="font-bold text-gray-700">{rental.car?.name}</div>
                      <div className="text-xs text-gray-400 mt-0.5 font-bold">{rental.car?.plateNumber}</div>
                    </td>
                    <td className="py-4 px-4 text-gray-500 font-medium">{new Date(rental.startDate).toLocaleDateString('id-ID')}</td>
                    <td className="py-4 px-4 text-gray-500 font-medium">{new Date(rental.endDate).toLocaleDateString('id-ID')}</td>
                    <td className="py-4 px-4 font-black text-emerald-600">Rp {fmt(rental.totalPrice)}</td>
                    <td className="py-4 px-4">
                      <span className={`text-[10px] font-black px-2.5 py-1 rounded-full uppercase tracking-wide ${
                        rental.status === 'ACTIVE' ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600'
                      }`}>
                        {rental.status === 'ACTIVE' ? 'Aktif' : 'Kembali'}
                      </span>
                    </td>
                    <td className="py-4 px-4 text-center">
                      <div className="flex items-center justify-center gap-2">
                        {rental.status === 'ACTIVE' ? (
                          <button
                            onClick={() => handleOpenReturnModal(rental)}
                            className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-1.5 px-3.5 rounded-lg text-xs shadow-sm shadow-indigo-600/10 cursor-pointer transition-all active:scale-[0.97]"
                          >
                            Kembalikan Mobil
                          </button>
                        ) : (
                          <div className="text-left text-xs text-gray-500 font-medium">
                            Kembali: {new Date(rental.actualReturnDate).toLocaleDateString('id-ID')}
                            {rental.lateFee > 0 && <div className="text-rose-600 font-bold mt-0.5">Denda: Rp {fmt(rental.lateFee)}</div>}
                          </div>
                        )}
                        <button
                          onClick={() => setPrintRental(rental)}
                          className="p-2 bg-gray-50 hover:bg-gray-100 text-gray-600 border border-gray-200/50 rounded-xl cursor-pointer transition-all flex items-center justify-center"
                          title="Cetak Struk"
                        >
                          <Printer size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Card View */}
          <div className="block md:hidden space-y-4">
            {rentals.map(rental => (
              <div key={rental.id} className="bg-white border border-gray-100 shadow-sm rounded-2xl p-4 flex flex-col gap-3 relative">
                <div className="flex justify-between items-start border-b border-gray-55 pb-3">
                  <div>
                    <span className="block text-sm font-bold text-gray-800">{rental.customerName}</span>
                    {rental.customer?.phone && (
                      <span className="block text-xs text-gray-400 mt-0.5 font-medium">{rental.customer.phone}</span>
                    )}
                  </div>
                  <span className={`text-[10px] font-black px-2.5 py-1 rounded-full uppercase tracking-wide ${
                    rental.status === 'ACTIVE' ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600'
                  }`}>
                    {rental.status === 'ACTIVE' ? 'Aktif' : 'Kembali'}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-3 text-xs">
                  <div>
                    <span className="block text-gray-400 font-bold uppercase tracking-wider text-[10px]">Mobil</span>
                    <span className="block font-bold text-gray-700 mt-0.5">{rental.car?.name}</span>
                    <span className="block text-[10px] text-gray-400 font-semibold">{rental.car?.plateNumber}</span>
                  </div>
                  <div>
                    <span className="block text-gray-400 font-bold uppercase tracking-wider text-[10px]">Total Biaya</span>
                    <span className="block font-extrabold text-emerald-600 mt-0.5">Rp {fmt(rental.totalPrice)}</span>
                  </div>
                  <div>
                    <span className="block text-gray-400 font-bold uppercase tracking-wider text-[10px]">Mulai Sewa</span>
                    <span className="block font-medium text-gray-600 mt-0.5">{new Date(rental.startDate).toLocaleDateString('id-ID')}</span>
                  </div>
                  <div>
                    <span className="block text-gray-400 font-bold uppercase tracking-wider text-[10px]">Tenggat</span>
                    <span className="block font-medium text-gray-600 mt-0.5">{new Date(rental.endDate).toLocaleDateString('id-ID')}</span>
                  </div>
                </div>

                <div className="flex flex-col gap-2 border-t border-gray-50 pt-3 mt-1">
                  {rental.identityText && (
                    <button
                      onClick={() => setViewIdentity(rental.identityText)}
                      className="w-full flex items-center justify-center gap-1.5 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-600 border border-indigo-100 rounded-xl text-xs font-bold cursor-pointer transition-all"
                    >
                      👁️ Lihat Bukti Identitas
                    </button>
                  )}
                  
                  {rental.status === 'ACTIVE' ? (
                    <button
                      onClick={() => handleOpenReturnModal(rental)}
                      className="w-full flex items-center justify-center gap-1.5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold cursor-pointer transition-all shadow-sm shadow-indigo-600/10 active:scale-[0.98]"
                    >
                      Kembalikan Mobil
                    </button>
                  ) : (
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-2.5 text-center text-xs text-gray-500 font-medium leading-relaxed mb-1">
                      Kembali: {new Date(rental.actualReturnDate).toLocaleDateString('id-ID')}
                      {rental.lateFee > 0 && (
                        <div className="text-rose-600 font-bold mt-0.5">Denda: Rp {fmt(rental.lateFee)}</div>
                      )}
                    </div>
                  )}

                  <button
                    onClick={() => setPrintRental(rental)}
                    className="w-full flex items-center justify-center gap-1.5 py-2 bg-gray-50 hover:bg-gray-100 text-gray-600 border border-gray-200/50 rounded-xl text-xs font-bold cursor-pointer transition-all"
                  >
                    <Printer size={14} /> Cetak Struk (58/80mm)
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Car Modal */}
      {isCarModalOpen && (
        <div className="modal-overlay z-50 backdrop-blur-md bg-black/35 flex items-center justify-center px-4">
          <div className="bg-white rounded-2xl w-full max-w-md p-6 shadow-xl flex flex-col gap-5 relative animate-in fade-in zoom-in-95 duration-200">
            <div>
              <h2 className="text-base md:text-lg font-black text-gray-800 m-0">
                {carFormData.id ? 'Edit Unit Mobil' : 'Tambah Unit Mobil'}
              </h2>
              <p className="text-xs text-gray-400 mt-1 font-semibold">Masukkan detail informasi kendaraan dengan benar</p>
            </div>
            
            <form onSubmit={handleSaveCar} className="flex flex-col gap-4">
              <div className="form-group flex flex-col gap-1.5">
                <label className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Nama Mobil (cth. Avanza)</label>
                <input
                  type="text"
                  value={carFormData.name}
                  onChange={(e) => setCarFormData({ ...carFormData, name: e.target.value })}
                  required
                  className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-12"
                />
              </div>
              <div className="form-group flex flex-col gap-1.5">
                <label className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Plat Nomor</label>
                <input
                  type="text"
                  value={carFormData.plateNumber}
                  onChange={(e) => setCarFormData({ ...carFormData, plateNumber: e.target.value.toUpperCase() })}
                  required
                  className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-12"
                />
              </div>
              <div className="form-group flex flex-col gap-1.5">
                <label className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Tipe Mobil</label>
                <select
                  value={carFormData.type}
                  onChange={(e) => setCarFormData({ ...carFormData, type: e.target.value })}
                  className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-12 cursor-pointer"
                >
                  <option value="MPV">MPV (Avanza, Xenia)</option>
                  <option value="SUV">SUV (Pajero, Fortuner)</option>
                  <option value="Sedan">Sedan (Civic, City)</option>
                  <option value="City Car">City Car (Brio, Agya)</option>
                </select>
              </div>
              <div className="form-group flex flex-col gap-1.5">
                <label className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Tarif Sewa Per Hari</label>
                <input
                  type="number"
                  value={carFormData.pricePerDay}
                  onChange={(e) => setCarFormData({ ...carFormData, pricePerDay: e.target.value })}
                  required
                  className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-12"
                />
              </div>
              <div className="form-group flex flex-col gap-1.5">
                <label className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Status</label>
                <select
                  value={carFormData.status}
                  onChange={(e) => setCarFormData({ ...carFormData, status: e.target.value })}
                  className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-12 cursor-pointer"
                >
                  <option value="AVAILABLE">Tersedia</option>
                  <option value="MAINTENANCE">Dalam Perbaikan/Servis</option>
                </select>
              </div>

              <div className="flex gap-3 mt-4">
                <button
                  type="button"
                  className="flex-1 py-3 text-sm font-bold text-gray-500 bg-gray-100 hover:bg-gray-200 rounded-xl border border-gray-200/50 cursor-pointer transition-all"
                  onClick={() => setIsCarModalOpen(false)}
                >
                  Batal
                </button>
                <button
                  type="submit"
                  className="flex-1 py-3 text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-md shadow-indigo-600/10 cursor-pointer transition-all active:scale-[0.98]"
                >
                  Simpan
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Return Modal */}
      {isReturnModalOpen && selectedRental && (
        <div className="modal-overlay z-50 backdrop-blur-md bg-black/35 flex items-center justify-center px-4">
          <div className="bg-white rounded-2xl w-full max-w-md p-6 shadow-xl flex flex-col gap-4 relative animate-in fade-in zoom-in-95 duration-200">
            <div>
              <h2 className="text-base md:text-lg font-black text-gray-800 m-0">Kembalikan Mobil</h2>
              <p className="text-xs text-gray-400 mt-1 font-semibold">Proses pengembalian unit dan cek adanya denda</p>
            </div>
            
            <div className="text-xs text-gray-505 bg-gray-50/50 border border-gray-100 rounded-2xl p-4 flex flex-col gap-2">
              <div className="flex justify-between">
                <span className="text-gray-400 font-bold uppercase tracking-wider text-[10px]">Unit Mobil</span>
                <span className="font-extrabold text-gray-700">{selectedRental.car.name} ({selectedRental.car.plateNumber})</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400 font-bold uppercase tracking-wider text-[10px]">Penyewa</span>
                <span className="font-extrabold text-gray-700">{selectedRental.customerName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400 font-bold uppercase tracking-wider text-[10px]">Batas Sewa</span>
                <span className="font-extrabold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">{new Date(selectedRental.endDate).toLocaleDateString('id-ID')}</span>
              </div>
            </div>

            <form onSubmit={handleProcessReturn} className="flex flex-col gap-4">
              <div className="form-group flex flex-col gap-1.5">
                <label className="text-[10px] font-bold text-gray-405 uppercase tracking-wider">Tanggal Dikembalikan</label>
                <input
                  type="date"
                  value={actualReturnDate}
                  onChange={(e) => {
                    setActualReturnDate(e.target.value);
                    // Recalculate late fee
                    const end = new Date(selectedRental.endDate);
                    const actual = new Date(e.target.value);
                    if (actual > end) {
                      const diffTime = Math.abs(actual - end);
                      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
                      setLateFee(diffDays * selectedRental.car.pricePerDay * 1.5);
                    } else {
                      setLateFee(0);
                    }
                  }}
                  required
                  className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-12"
                />
              </div>

              <div className="form-group flex flex-col gap-1.5">
                <label className="text-[10px] font-bold text-gray-405 uppercase tracking-wider">Biaya Denda Telat (Rp)</label>
                <input
                  type="number"
                  value={lateFee}
                  onChange={(e) => setLateFee(e.target.value)}
                  className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-12"
                />
                {lateFee > 0 && (
                  <div className="text-[10px] text-rose-600 font-bold flex items-start gap-1.5 mt-1 leading-relaxed bg-rose-50/50 border border-rose-100/50 p-2.5 rounded-xl">
                    <div className="mt-0.5"><AlertTriangle size={12} /></div>
                    <span>Terlambat mengembalikan! Denda otomatis dihitung 1.5x tarif sewa per hari.</span>
                  </div>
                )}
              </div>

              {lateFee > 0 && (
                <div className="form-group flex flex-col gap-1.5">
                  <label className="text-[10px] font-bold text-gray-455 uppercase tracking-wider">Metode Pembayaran Denda</label>
                  <div className="grid grid-cols-2 gap-2 bg-gray-50/50 p-1 rounded-xl border border-gray-100">
                    {['CASH', 'TRANSFER'].map(method => (
                      <button
                        key={method}
                        type="button"
                        onClick={() => setPaymentMethod(method)}
                        className={`py-2 px-1.5 rounded-lg text-xs font-black text-center transition-all cursor-pointer ${
                          paymentMethod === method
                            ? 'bg-indigo-600 text-white shadow-sm'
                            : 'bg-white border border-gray-200 text-gray-500 hover:bg-gray-50'
                        }`}
                      >
                        {method}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div className="flex gap-3 mt-4">
                <button
                  type="button"
                  className="flex-1 py-3 text-sm font-bold text-gray-500 bg-gray-100 hover:bg-gray-200 rounded-xl border border-gray-200/50 cursor-pointer transition-all"
                  onClick={() => setIsReturnModalOpen(false)}
                >
                  Batal
                </button>
                <button
                  type="submit"
                  className="flex-1 py-3 text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-md shadow-indigo-600/10 cursor-pointer transition-all active:scale-[0.98]"
                >
                  Konfirmasi Selesai
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal View Identitas */}
      {viewIdentity && (
        <div className="modal-overlay z-[999] backdrop-blur-md bg-black/45 flex items-center justify-center px-4">
          <div className="bg-white rounded-2xl w-full max-w-md p-6 shadow-xl flex flex-col gap-5 relative animate-in fade-in zoom-in-95 duration-200 text-center">
            <div>
              <h3 className="text-base md:text-lg font-black text-gray-800 m-0">Dokumen Bukti Identitas</h3>
              <p className="text-xs text-gray-400 mt-1 font-semibold">Pastikan kesesuaian dokumen sebelum menyerahkan kunci</p>
            </div>
            
            <img src={viewIdentity} alt="Bukti Identitas" draggable="false" className="w-full max-h-[350px] object-contain rounded-2xl border border-gray-100 shadow-inner bg-gray-50/50" />
            
            <button
              type="button"
              onClick={() => setViewIdentity(null)}
              className="w-full py-3.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-sm font-black rounded-xl cursor-pointer transition-all active:scale-[0.98]"
            >
              Tutup
            </button>
          </div>
        </div>
      )}

      {/* Modal Cetak Struk */}
      {printRental && (
        <div className="modal-overlay z-[999] backdrop-blur-md bg-black/45 flex items-center justify-center px-4">
          <div className="bg-white rounded-2xl w-full max-w-md p-6 shadow-xl flex flex-col gap-4 relative animate-in fade-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center pb-2 border-b border-gray-100">
              <div>
                <h3 className="text-base font-black text-gray-800 m-0">Cetak Struk Transaksi</h3>
                <p className="text-[10px] text-gray-400 font-semibold mt-0.5">Pilih ukuran kertas dan metode cetak</p>
              </div>
              <button
                onClick={() => setPrintRental(null)}
                className="text-gray-400 hover:text-gray-650 font-bold text-lg cursor-pointer border-none bg-transparent"
              >
                &times;
              </button>
            </div>

            {/* Paper Size Picker */}
            <div className="flex flex-col gap-1.5">
              <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Ukuran Kertas Struk</span>
              <div className="grid grid-cols-2 gap-2 bg-gray-50/50 p-1 rounded-xl border border-gray-100">
                {['58mm', '80mm'].map(size => (
                  <button
                    key={size}
                    type="button"
                    onClick={() => setPaperSize(size)}
                    className={`py-2 text-xs font-black text-center transition-all cursor-pointer rounded-lg ${
                      paperSize === size
                        ? 'bg-indigo-600 text-white shadow-sm'
                        : 'bg-white border border-gray-200/50 text-gray-500 hover:bg-gray-50'
                    }`}
                  >
                    Thermal {size}
                  </button>
                ))}
              </div>
            </div>

            {/* Receipt Styled Preview Box */}
            <div className="flex flex-col gap-1">
              <span className="text-[10px] font-bold text-gray-450 uppercase tracking-wider">Pratinjau Kertas</span>
              <div className="bg-slate-900 text-slate-100 p-4 rounded-xl text-left overflow-y-auto" style={{ maxHeight: '180px' }}>
                <pre className="font-mono text-[9px] leading-relaxed whitespace-pre font-bold select-all">
                  {(() => {
                    const start = new Date(printRental.startDate);
                    const end = new Date(printRental.endDate);
                    const diffTime = Math.abs(end - start);
                    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) || 1;
                    const basicCost = (printRental.car?.pricePerDay || 0) * diffDays;
                    const grandTotal = Number(printRental.totalPrice || 0) + Number(printRental.status === 'RETURNED' ? (printRental.lateFee || 0) : 0);
                    const charLimit = paperSize === '58mm' ? 32 : 48;
                    const padText = (left, right) => {
                      const spaceNeeded = charLimit - left.length - right.length;
                      return left + ' '.repeat(spaceNeeded > 0 ? spaceNeeded : 1) + right;
                    };
                    
                    let settings = receiptSettings;
                    try {
                      const saved = localStorage.getItem('posbah_receipt_settings_rental');
                      if (saved) settings = JSON.parse(saved);
                    } catch (e) {}
                    const storeName = settings?.storeName || 'POSBAH RENTAL MOBIL';
                    const subheader = settings?.subheader || 'Jasa Sewa Mobil Terbaik & Cepat';
                    const footer = settings?.footer || "Terima kasih atas kunjungan Anda!\nHarap berkendara dengan aman.\n- POSBAH -";

                    let text = '';
                    text += ' '.repeat(Math.max(0, Math.floor((charLimit - storeName.length) / 2))) + storeName + '\n';
                    subheader.split('\n').forEach(line => {
                      if (line.trim()) text += ' '.repeat(Math.max(0, Math.floor((charLimit - line.trim().length) / 2))) + line.trim() + '\n';
                    });
                    text += '-'.repeat(charLimit) + '\n';
                    text += `No. Sewa : RENT-${printRental.id}\n`;
                    text += `Tanggal  : ${new Date(printRental.createdAt || Date.now()).toLocaleDateString('id-ID')}\n`;
                    text += `Penyewa  : ${printRental.customerName}\n`;
                    if (printRental.customer?.phone) {
                      text += `No. HP   : ${printRental.customer.phone}\n`;
                    }
                    text += '-'.repeat(charLimit) + '\n';
                    text += `Mobil    : ${printRental.car?.name}\n`;
                    text += `Plat No  : ${printRental.car?.plateNumber || '-'}\n`;
                    text += `Tipe     : ${printRental.car?.type || '-'}\n`;
                    text += '-'.repeat(charLimit) + '\n';
                    text += `Mulai    : ${new Date(printRental.startDate).toLocaleDateString('id-ID')}\n`;
                    text += `Tenggat  : ${new Date(printRental.endDate).toLocaleDateString('id-ID')}\n`;
                    if (printRental.status === 'RETURNED' && printRental.actualReturnDate) {
                      text += `Kembali  : ${new Date(printRental.actualReturnDate).toLocaleDateString('id-ID')}\n`;
                    }
                    text += `Durasi   : ${diffDays} Hari\n`;
                    text += '-'.repeat(charLimit) + '\n';
                    text += padText('Tarif Sewa', `Rp ${Number(printRental.car?.pricePerDay || 0).toLocaleString('id-ID')}/hr`) + '\n';
                    text += padText('Biaya Dasar', `Rp ${basicCost.toLocaleString('id-ID')}`) + '\n';
                    if (printRental.lateFee > 0) {
                      text += padText('Denda Telat', `Rp ${Number(printRental.lateFee).toLocaleString('id-ID')}`) + '\n';
                    }
                    text += '-'.repeat(charLimit) + '\n';
                    text += padText('GRAND TOTAL', `Rp ${grandTotal.toLocaleString('id-ID')}`) + '\n';
                    text += padText('Metode Bayar', String(printRental.paymentMethod || 'CASH').toUpperCase()) + '\n';
                    text += padText('Status Sewa', printRental.status === 'ACTIVE' ? 'DISEWA (AKTIF)' : 'KEMBALI (SELESAI)') + '\n';
                    text += '-'.repeat(charLimit) + '\n';
                    footer.split('\n').forEach(line => {
                      if (line.trim()) text += ' '.repeat(Math.max(0, Math.floor((charLimit - line.trim().length) / 2))) + line.trim() + '\n';
                    });
                    return text;
                  })()}
                </pre>
              </div>
            </div>

            {/* Print Triggers */}
            <div className="flex flex-col gap-2.5 mt-2">
              <button
                type="button"
                onClick={() => printViaBluetooth(printRental, paperSize)}
                disabled={printingBluetooth}
                className="w-full flex items-center justify-center gap-1.5 py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-black cursor-pointer shadow-md shadow-indigo-600/10 transition-all active:scale-[0.98] disabled:bg-gray-300 disabled:shadow-none"
              >
                <Printer size={15} />
                {printingBluetooth ? 'Menghubungkan...' : 'Hubungkan & Cetak (Bluetooth)'}
              </button>
              <button
                type="button"
                onClick={() => printViaSystem(printRental, paperSize)}
                className="w-full flex items-center justify-center gap-1.5 py-3 bg-gray-100 hover:bg-gray-250 text-gray-700 rounded-xl text-xs font-bold cursor-pointer transition-all active:scale-[0.98]"
              >
                Cetak Sistem (Browser)
              </button>
              <button
                type="button"
                onClick={() => setPrintRental(null)}
                className="w-full py-2.5 text-gray-500 hover:text-gray-800 text-xs font-bold rounded-xl border border-gray-200/50 cursor-pointer transition-all mt-1"
              >
                Tutup
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Pengaturan Struk */}
      {settingsModalOpen && (
        <div className="modal-overlay z-[999] backdrop-blur-md bg-black/45 flex items-center justify-center px-4">
          <div className="bg-white rounded-2xl w-full max-w-2xl p-6 shadow-xl flex flex-col gap-4 relative animate-in fade-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center pb-2 border-b border-gray-100">
              <div>
                <h3 className="text-base font-black text-gray-800 m-0">Pengaturan Desain Struk</h3>
                <p className="text-[10px] text-gray-400 font-semibold mt-0.5">Kustomisasi informasi struk belanja POS Rental Mobil</p>
              </div>
              <button
                onClick={() => setSettingsModalOpen(false)}
                className="text-gray-400 hover:text-gray-650 font-bold text-lg cursor-pointer border-none bg-transparent"
              >
                &times;
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Left Column: Form inputs */}
              <div className="flex flex-col gap-4">
                <div className="form-group flex flex-col gap-1.5">
                  <label className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Nama Toko / Bisnis</label>
                  <input
                    type="text"
                    value={tempStoreName}
                    onChange={(e) => setTempStoreName(e.target.value)}
                    className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2 text-sm text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-11"
                    placeholder="Contoh: POSBAH RENTAL MOBIL"
                  />
                </div>

                <div className="form-group flex flex-col gap-1.5">
                  <label className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Subheader / Keterangan</label>
                  <input
                    type="text"
                    value={tempSubheader}
                    onChange={(e) => setTempSubheader(e.target.value)}
                    className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2 text-sm text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 h-11"
                    placeholder="Contoh: Jasa Sewa Mobil Terbaik & Cepat"
                  />
                </div>

                <div className="form-group flex flex-col gap-1.5">
                  <label className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Kaki Struk (Footer)</label>
                  <textarea
                    value={tempFooter}
                    onChange={(e) => setTempFooter(e.target.value)}
                    rows={3}
                    className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2 text-sm text-gray-800 outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 transition-all duration-200 resize-none"
                    placeholder="Contoh: Terima kasih atas kunjungan Anda!"
                  />
                </div>

                <div className="flex flex-col gap-2">
                  <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Cetak Uji Coba (Test Print)</span>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => printTestReceipt('58mm')}
                      className="flex-1 flex items-center justify-center gap-1.5 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 border border-gray-200 rounded-xl text-xs font-bold cursor-pointer transition-all active:scale-[0.98]"
                    >
                      <Printer size={13} /> Cetak 58mm
                    </button>
                    <button
                      type="button"
                      onClick={() => printTestReceipt('80mm')}
                      className="flex-1 flex items-center justify-center gap-1.5 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 border border-gray-200 rounded-xl text-xs font-bold cursor-pointer transition-all active:scale-[0.98]"
                    >
                      <Printer size={13} /> Cetak 80mm
                    </button>
                  </div>
                </div>
              </div>

              {/* Right Column: Live thermal receipt mockup preview */}
              <div className="flex flex-col gap-2">
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Pratinjau Live (Preview)</span>
                <div
                  className="bg-slate-900 text-slate-100 p-4 rounded-2xl text-left overflow-y-auto font-mono text-[10px] leading-relaxed whitespace-pre flex flex-col gap-1 border border-slate-800"
                  style={{ maxHeight: '220px' }}
                >
                  <div className="text-center font-bold text-[11px] uppercase tracking-wider">{tempStoreName || 'NAMA TOKO'}</div>
                  <div className="text-center text-[9px] opacity-80">{tempSubheader || 'Subheader Struk'}</div>
                  <div className="border-t border-dashed border-slate-700 my-1"></div>
                  
                  <div className="flex justify-between">
                    <span>No: RENT-PREVIEW-999</span>
                    <span>Tunai</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Tgl: {new Date().toLocaleDateString('id-ID')}</span>
                    <span>12:00</span>
                  </div>
                  <div className="border-t border-dashed border-slate-700 my-1"></div>
                  
                  <div>Mobil    : Toyota Avanza</div>
                  <div>Plat No  : B 1234 ABC</div>
                  <div>Mulai    : {new Date().toLocaleDateString('id-ID')}</div>
                  <div>Tenggat  : {new Date(Date.now() + 2*24*60*60*1000).toLocaleDateString('id-ID')}</div>
                  <div>Durasi   : 2 Hari</div>
                  <div className="border-t border-dashed border-slate-700 my-1"></div>
                  
                  <div className="flex justify-between">
                    <span>Tarif Sewa</span>
                    <span>Rp 250.000/hr</span>
                  </div>
                  <div className="flex justify-between font-bold">
                    <span>GRAND TOTAL</span>
                    <span>Rp 500.000</span>
                  </div>
                  <div className="border-t border-dashed border-slate-700 my-1"></div>
                  
                  <div className="text-center opacity-90 whitespace-pre-line text-[9px]">{tempFooter || 'Terima kasih!'}</div>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="flex gap-3 justify-end border-t border-gray-100 pt-4 mt-2">
              <button
                type="button"
                onClick={() => setSettingsModalOpen(false)}
                className="py-2.5 px-5 text-sm font-bold text-gray-500 bg-gray-100 hover:bg-gray-200 rounded-xl border border-gray-200/50 cursor-pointer transition-all"
              >
                Batal
              </button>
              <button
                type="button"
                onClick={() => {
                  const newSettings = {
                    storeName: tempStoreName,
                    subheader: tempSubheader,
                    footer: tempFooter
                  };
                  localStorage.setItem('posbah_receipt_settings_rental', JSON.stringify(newSettings));
                  setReceiptSettings(newSettings);
                  setSettingsModalOpen(false);
                }}
                className="py-2.5 px-6 text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-md shadow-indigo-600/10 cursor-pointer transition-all active:scale-[0.98]"
              >
                Simpan Pengaturan
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
