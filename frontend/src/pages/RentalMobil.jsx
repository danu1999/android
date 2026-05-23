import React, { useState, useEffect } from 'react';
import {
  Car, Plus, Edit2, Trash2, Calendar, DollarSign,
  User, ClipboardList, CheckCircle, AlertTriangle, Search, RefreshCw
} from 'lucide-react';
import api from '../api';
import { useDemoBlock } from '../AuthContext';

export default function RentalMobil() {
  const { showDemoBlock, isDemo } = useDemoBlock();
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

  // Return Modal State
  const [isReturnModalOpen, setIsReturnModalOpen] = useState(false);
  const [selectedRental, setSelectedRental] = useState(null);
  const [actualReturnDate, setActualReturnDate] = useState('');
  const [lateFee, setLateFee] = useState(0);

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
      await api.post('/rentals', {
        carId: Number(selectedCarId),
        customerId: selectedCustomerId ? Number(selectedCustomerId) : null,
        customerName: custName,
        startDate,
        endDate,
        totalPrice: calculateTotalPrice(),
        paymentMethod
      });

      // Reset
      setSelectedCarId('');
      setSelectedCustomerId('');
      setNewCustomerName('');
      setStartDate('');
      setEndDate('');
      
      alert('Penyewaan mobil berhasil diproses!');
      fetchCars();
      fetchRentals();
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
      await api.post(`/rentals/${selectedRental.id}/return`, {
        actualReturnDate,
        lateFee,
        paymentMethod
      });
      setIsReturnModalOpen(false);
      fetchCars();
      fetchRentals();
      alert('Mobil berhasil dikembalikan!');
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.error || 'Gagal memproses pengembalian.');
    } finally {
      setLoading(false);
    }
  };

  const fmt = (n) => Number(n || 0).toLocaleString('id-ID');

  return (
    <div className="page-container">
      {/* Header */}
      <div className="header-actions">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Car size={26} color="#4F46E5" />
          <h1 style={{ margin: 0 }}>POS Rental Mobil</h1>
        </div>
      </div>

      {/* Tabs Menu */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', borderBottom: '1px solid #E5E7EB', paddingBottom: '10px' }}>
        <button
          onClick={() => setActiveTab('POS')}
          className={`btn ${activeTab === 'POS' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ padding: '8px 16px', borderRadius: '10px', fontWeight: 700 }}
        >
          Sewa Mobil (POS)
        </button>
        <button
          onClick={() => setActiveTab('CARS')}
          className={`btn ${activeTab === 'CARS' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ padding: '8px 16px', borderRadius: '10px', fontWeight: 700 }}
        >
          Kelola Mobil
        </button>
        <button
          onClick={() => setActiveTab('HISTORY')}
          className={`btn ${activeTab === 'HISTORY' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ padding: '8px 16px', borderRadius: '10px', fontWeight: 700 }}
        >
          Riwayat & Pengembalian
        </button>
      </div>

      {/* Tab 1: Rental POS */}
      {activeTab === 'POS' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Form */}
          <div className="glass-panel p-6 lg:col-span-2" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#1F2937', margin: '0 0 10px' }}>Form Transaksi Sewa</h2>
            <form onSubmit={handleRentalCheckout} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              
              {/* Select Car */}
              <div className="form-group">
                <label style={{ fontWeight: 700, fontSize: '0.85rem', color: '#4B5563', marginBottom: '6px', display: 'block' }}>Mobil Tersedia</label>
                <select
                  value={selectedCarId}
                  onChange={(e) => setSelectedCarId(e.target.value)}
                  required
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '12px', border: '1.5px solid #E5E7EB', outline: 'none' }}
                >
                  <option value="">-- Pilih Mobil --</option>
                  {cars.filter(c => c.status === 'AVAILABLE').map(c => (
                    <option key={c.id} value={c.id}>{c.name} ({c.plateNumber}) - Rp {fmt(c.pricePerDay)}/hari</option>
                  ))}
                </select>
              </div>

              {/* Select Customer */}
              <div className="form-group">
                <label style={{ fontWeight: 700, fontSize: '0.85rem', color: '#4B5563', marginBottom: '6px', display: 'block' }}>Pelanggan Terdaftar</label>
                <select
                  value={selectedCustomerId}
                  onChange={(e) => {
                    setSelectedCustomerId(e.target.value);
                    if (e.target.value) setNewCustomerName('');
                  }}
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '12px', border: '1.5px solid #E5E7EB', outline: 'none', marginBottom: '8px' }}
                >
                  <option value="">-- Pilih Pelanggan (Opsional) --</option>
                  {customers.map(cust => (
                    <option key={cust.id} value={cust.id}>{cust.name} ({cust.phone || 'No HP -'})</option>
                  ))}
                </select>
                
                {!selectedCustomerId && (
                  <div>
                    <span style={{ fontSize: '0.8rem', color: '#6B7280' }}>Atau ketik nama pelanggan baru:</span>
                    <input
                      type="text"
                      placeholder="Nama Pelanggan Baru..."
                      value={newCustomerName}
                      onChange={(e) => setNewCustomerName(e.target.value)}
                      required={!selectedCustomerId}
                      style={{ width: '100%', padding: '10px 14px', borderRadius: '12px', border: '1.5px solid #E5E7EB', outline: 'none', marginTop: '6px' }}
                    />
                  </div>
                )}
              </div>

              {/* Period Start - End */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="form-group">
                  <label style={{ fontWeight: 700, fontSize: '0.85rem', color: '#4B5563', marginBottom: '6px', display: 'block' }}>Tanggal Mulai Sewa</label>
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    required
                    style={{ width: '100%', padding: '10px 14px', borderRadius: '12px', border: '1.5px solid #E5E7EB', outline: 'none' }}
                  />
                </div>
                <div className="form-group">
                  <label style={{ fontWeight: 700, fontSize: '0.85rem', color: '#4B5563', marginBottom: '6px', display: 'block' }}>Tanggal Akhir Sewa</label>
                  <input
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    required
                    min={startDate}
                    style={{ width: '100%', padding: '10px 14px', borderRadius: '12px', border: '1.5px solid #E5E7EB', outline: 'none' }}
                  />
                </div>
              </div>

              {/* Payment Method */}
              <div className="form-group">
                <label style={{ fontWeight: 700, fontSize: '0.85rem', color: '#4B5563', marginBottom: '6px', display: 'block' }}>Metode Pembayaran</label>
                <div style={{ display: 'flex', gap: '8px' }}>
                  {['CASH', 'TRANSFER', 'QRIS', 'HUTANG'].map(method => (
                    <button
                      key={method}
                      type="button"
                      onClick={() => setPaymentMethod(method)}
                      className={`btn ${paymentMethod === method ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ flex: 1, padding: '10px', borderRadius: '10px', fontSize: '0.8rem', fontWeight: 800 }}
                    >
                      {method}
                    </button>
                  ))}
                </div>
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading}
                style={{ width: '100%', padding: '14px', borderRadius: '12px', fontWeight: 800, fontSize: '1rem', marginTop: '10px' }}
              >
                {loading ? 'Memproses...' : 'Proses Sewa & Cetak Struk'}
              </button>
            </form>
          </div>

          {/* Receipt Preview */}
          <div className="glass-panel p-6" style={{ background: '#F9FAFB', border: '1px dashed #D1D5DB' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#1F2937', margin: '0 0 16px', borderBottom: '1px solid #E5E7EB', paddingBottom: '8px' }}>
              Rincian Pembayaran
            </h2>
            {selectedCarId && startDate && endDate ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#6B7280', fontSize: '0.9rem' }}>Mobil:</span>
                  <span style={{ fontWeight: 700, color: '#1F2937' }}>
                    {cars.find(c => c.id === Number(selectedCarId))?.name}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#6B7280', fontSize: '0.9rem' }}>Tarif/Hari:</span>
                  <span style={{ fontWeight: 700, color: '#1F2937' }}>
                    Rp {fmt(getSelectedCarPrice())}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#6B7280', fontSize: '0.9rem' }}>Durasi Sewa:</span>
                  <span style={{ fontWeight: 700, color: '#4F46E5' }}>{calculateDays()} Hari</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid #E5E7EB', paddingTop: '10px', marginTop: '10px' }}>
                  <span style={{ fontWeight: 800, color: '#1F2937' }}>Total Tagihan:</span>
                  <span style={{ fontWeight: 900, color: '#059669', fontSize: '1.2rem' }}>
                    Rp {fmt(calculateTotalPrice())}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#6B7280', fontSize: '0.9rem' }}>Metode Bayar:</span>
                  <span style={{ fontWeight: 800, color: '#4B5563' }}>{paymentMethod}</span>
                </div>
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '40px 0', color: '#9CA3AF', fontSize: '0.85rem' }}>
                Silakan pilih mobil dan tanggal sewa untuk melihat rincian tagihan.
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab 2: Manage Cars */}
      {activeTab === 'CARS' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#1F2937', margin: 0 }}>Kelola Unit Kendaraan</h2>
            <button className="btn btn-primary" onClick={() => handleOpenCarModal()} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Plus size={16} /> Tambah Mobil
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {cars.map(car => (
              <div key={car.id} className="glass-panel p-5 flex flex-col justify-between" style={{ position: 'relative' }}>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                    <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: '#111827', margin: 0 }}>{car.name}</h3>
                    <span style={{
                      fontSize: '0.65rem',
                      fontWeight: 800,
                      backgroundColor: car.status === 'AVAILABLE' ? '#ECFDF5' : car.status === 'RENTED' ? '#FEF3C7' : '#FEF2F2',
                      color: car.status === 'AVAILABLE' ? '#059669' : car.status === 'RENTED' ? '#D97706' : '#DC2626',
                      padding: '3px 8px',
                      borderRadius: '99px',
                      textTransform: 'uppercase'
                    }}>
                      {car.status === 'AVAILABLE' ? 'Tersedia' : car.status === 'RENTED' ? 'Disewa' : 'Servis'}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.85rem', color: '#4B5563', fontWeight: 700, marginBottom: '12px' }}>{car.plateNumber}</div>
                  
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#6B7280', marginBottom: '4px' }}>
                    <span>Tipe:</span>
                    <span style={{ fontWeight: 750, color: '#1F2937' }}>{car.type}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#6B7280', marginBottom: '12px' }}>
                    <span>Tarif Sewa:</span>
                    <span style={{ fontWeight: 900, color: '#059669' }}>Rp {fmt(car.pricePerDay)} / Hari</span>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '8px', borderTop: '1px solid #F3F4F6', paddingTop: '12px', marginTop: '12px' }}>
                  <button className="btn btn-secondary" onClick={() => handleOpenCarModal(car)} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4, padding: '8px' }}>
                    <Edit2 size={14} /> Edit
                  </button>
                  <button className="btn btn-secondary" onClick={() => handleDeleteCar(car.id)} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4, color: '#DC2626', borderColor: '#FCA5A5', padding: '8px' }}>
                    <Trash2 size={14} /> Hapus
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tab 3: History & Return */}
      {activeTab === 'HISTORY' && (
        <div className="glass-panel p-6">
          <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#1F2937', margin: '0 0 16px' }}>Riwayat Sewa Mobil</h2>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #E5E7EB', color: '#4B5563', fontWeight: 700 }}>
                  <th style={{ padding: '12px 8px' }}>Penyewa</th>
                  <th style={{ padding: '12px 8px' }}>Mobil</th>
                  <th style={{ padding: '12px 8px' }}>Mulai Sewa</th>
                  <th style={{ padding: '12px 8px' }}>Tenggat</th>
                  <th style={{ padding: '12px 8px' }}>Total Biaya</th>
                  <th style={{ padding: '12px 8px' }}>Status</th>
                  <th style={{ padding: '12px 8px', textAlign: 'center' }}>Aksi</th>
                </tr>
              </thead>
              <tbody>
                {rentals.map(rental => (
                  <tr key={rental.id} style={{ borderBottom: '1px solid #F3F4F6', color: '#1F2937' }}>
                    <td style={{ padding: '12px 8px', fontWeight: 750 }}>
                      {rental.customerName}
                      {rental.customer?.phone && <div style={{ fontSize: '0.75rem', color: '#6B7280', fontWeight: 400 }}>{rental.customer.phone}</div>}
                    </td>
                    <td style={{ padding: '12px 8px' }}>
                      {rental.car?.name}
                      <div style={{ fontSize: '0.75rem', color: '#4B5563', fontWeight: 600 }}>{rental.car?.plateNumber}</div>
                    </td>
                    <td style={{ padding: '12px 8px' }}>{new Date(rental.startDate).toLocaleDateString('id-ID')}</td>
                    <td style={{ padding: '12px 8px' }}>{new Date(rental.endDate).toLocaleDateString('id-ID')}</td>
                    <td style={{ padding: '12px 8px', fontWeight: 800, color: '#059669' }}>Rp {fmt(rental.totalPrice)}</td>
                    <td style={{ padding: '12px 8px' }}>
                      <span style={{
                        fontSize: '0.65rem',
                        fontWeight: 800,
                        backgroundColor: rental.status === 'ACTIVE' ? '#FEF3C7' : '#ECFDF5',
                        color: rental.status === 'ACTIVE' ? '#D97706' : '#059669',
                        padding: '3px 8px',
                        borderRadius: '99px',
                        textTransform: 'uppercase'
                      }}>
                        {rental.status === 'ACTIVE' ? 'Aktif' : 'Kembali'}
                      </span>
                    </td>
                    <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                      {rental.status === 'ACTIVE' ? (
                        <button
                          onClick={() => handleOpenReturnModal(rental)}
                          className="btn btn-primary"
                          style={{ padding: '6px 12px', fontSize: '0.78rem', borderRadius: '8px', fontWeight: 800 }}
                        >
                          Kembalikan Mobil
                        </button>
                      ) : (
                        <div style={{ fontSize: '0.75rem', color: '#6B7280' }}>
                          Kembali: {new Date(rental.actualReturnDate).toLocaleDateString('id-ID')}
                          {rental.lateFee > 0 && <div style={{ color: '#DC2626', fontWeight: 700 }}>Denda: Rp {fmt(rental.lateFee)}</div>}
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Car Modal */}
      {isCarModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel" style={{ maxWidth: '400px' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#1F2937', margin: '0 0 16px' }}>
              {carFormData.id ? 'Edit Unit Mobil' : 'Tambah Unit Mobil'}
            </h2>
            <form onSubmit={handleSaveCar} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div className="form-group">
                <label>Nama Mobil (cth. Avanza)</label>
                <input
                  type="text"
                  value={carFormData.name}
                  onChange={(e) => setCarFormData({ ...carFormData, name: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label>Plat Nomor</label>
                <input
                  type="text"
                  value={carFormData.plateNumber}
                  onChange={(e) => setCarFormData({ ...carFormData, plateNumber: e.target.value.toUpperCase() })}
                  required
                />
              </div>
              <div className="form-group">
                <label>Tipe Mobil</label>
                <select
                  value={carFormData.type}
                  onChange={(e) => setCarFormData({ ...carFormData, type: e.target.value })}
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '12px', border: '1.5px solid #E5E7EB', outline: 'none' }}
                >
                  <option value="MPV">MPV (Avanza, Xenia)</option>
                  <option value="SUV">SUV (Pajero, Fortuner)</option>
                  <option value="Sedan">Sedan (Civic, City)</option>
                  <option value="City Car">City Car (Brio, Agya)</option>
                </select>
              </div>
              <div className="form-group">
                <label>Tarif Sewa Per Hari</label>
                <input
                  type="number"
                  value={carFormData.pricePerDay}
                  onChange={(e) => setCarFormData({ ...carFormData, pricePerDay: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label>Status</label>
                <select
                  value={carFormData.status}
                  onChange={(e) => setCarFormData({ ...carFormData, status: e.target.value })}
                  style={{ width: '100%', padding: '10px 14px', borderRadius: '12px', border: '1.5px solid #E5E7EB', outline: 'none' }}
                >
                  <option value="AVAILABLE">Tersedia</option>
                  <option value="MAINTENANCE">Dalam Perbaikan/Servis</option>
                </select>
              </div>

              <div className="modal-actions" style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsCarModalOpen(false)} style={{ flex: 1 }}>Batal</button>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Simpan</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Return Modal */}
      {isReturnModalOpen && selectedRental && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel" style={{ maxWidth: '400px' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#1F2937', margin: '0 0 12px' }}>
              Kembalikan Mobil
            </h2>
            <div style={{ fontSize: '0.85rem', color: '#4B5563', background: '#F3F4F6', borderRadius: '12px', padding: '10px', marginBottom: '14px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                <span>Unit Mobil:</span>
                <span style={{ fontWeight: 700 }}>{selectedRental.car.name} ({selectedRental.car.plateNumber})</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                <span>Penyewa:</span>
                <span style={{ fontWeight: 700 }}>{selectedRental.customerName}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Batas Sewa:</span>
                <span style={{ fontWeight: 700 }}>{new Date(selectedRental.endDate).toLocaleDateString('id-ID')}</span>
              </div>
            </div>

            <form onSubmit={handleProcessReturn} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div className="form-group">
                <label>Tanggal Dikembalikan</label>
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
                />
              </div>

              <div className="form-group">
                <label>Biaya Denda Telat (Rp)</label>
                <input
                  type="number"
                  value={lateFee}
                  onChange={(e) => setLateFee(e.target.value)}
                  style={{ width: '100%' }}
                />
                {lateFee > 0 && (
                  <div style={{ fontSize: '0.78rem', color: '#DC2626', display: 'flex', alignItems: 'center', gap: 4, marginTop: '4px', fontWeight: 600 }}>
                    <AlertTriangle size={12} />
                    Terlambat mengembalikan! Denda otomatis dihitung 1.5x tarif sewa per hari.
                  </div>
                )}
              </div>

              {lateFee > 0 && (
                <div className="form-group">
                  <label>Metode Pembayaran Denda</label>
                  <div style={{ display: 'flex', gap: '6px' }}>
                    {['CASH', 'TRANSFER', 'QRIS'].map(method => (
                      <button
                        key={method}
                        type="button"
                        onClick={() => setPaymentMethod(method)}
                        className={`btn ${paymentMethod === method ? 'btn-primary' : 'btn-secondary'}`}
                        style={{ flex: 1, padding: '8px', fontSize: '0.75rem', fontWeight: 800, borderRadius: '8px' }}
                      >
                        {method}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div className="modal-actions" style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsReturnModalOpen(false)} style={{ flex: 1 }}>Batal</button>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Konfirmasi Selesai</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
