import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ChevronLeft, Users, Phone, MapPin, Info,
  Plus, Minus, Check, ShoppingCart, DollarSign
} from 'lucide-react';
import api from '../api';
import { useDemoBlock } from '../AuthContext';

export default function KasirLaundry() {
  const navigate = useNavigate();
  const { isDemo } = useDemoBlock();

  const [namaPelanggan, setNamaPelanggan] = useState('');
  const [noHp, setNoHp] = useState('');
  const [lokasi, setLokasi] = useState('');
  const [statusBayar, setStatusBayar] = useState('Belum Lunas');

  const [layanan, setLayanan] = useState([]);
  const [pelangganUnik, setPelangganUnik] = useState([]);
  const [loading, setLoading] = useState(true);

  // Tab: 'Laundry' atau 'Plastik' (barang pendukung)
  const [activeTab, setActiveTab] = useState('Laundry');
  
  // Shopping Cart: { [serviceId]: qty }
  const [cart, setCart] = useState({});
  const [showContactModal, setShowContactModal] = useState(false);
  const [cariKontak, setCariKontak] = useState('');

  useEffect(() => {
    const loadData = async () => {
      try {
        const [servicesRes, customersRes] = await Promise.all([
          api.get('/laundry/services'),
          api.get('/customers/list')
        ]);
        setLayanan(servicesRes.data);
        setPelangganUnik(customersRes.data);
      } catch (err) {
        console.error('Failed to load laundry services/customers:', err);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, []);

  const handleQtyChange = (item, direction) => {
    setCart(prev => {
      const currentQty = prev[item.id] || 0;
      let newQty = currentQty + direction;
      if (newQty < 0) newQty = 0;
      
      const newCart = { ...prev };
      if (newQty > 0) {
        newCart[item.id] = newQty;
      } else {
        delete newCart[item.id];
      }
      return newCart;
    });
  };

  const getCartTotal = () => {
    return Object.keys(cart).reduce((sum, serviceId) => {
      const s = layanan.find(item => item.id === Number(serviceId));
      if (!s) return sum;
      return sum + (s.harga * cart[serviceId]);
    }, 0);
  };

  const getCartSummary = () => {
    let listKategori = new Set();
    let rincianTeks = "";

    Object.keys(cart).forEach(serviceId => {
      const s = layanan.find(item => item.id === Number(serviceId));
      if (s) {
        const qty = cart[serviceId];
        listKategori.add(s.kategori);
        rincianTeks += `- ${s.kategori} - ${s.nama} : ${qty} ${s.satuan} (Rp ${(s.harga * qty).toLocaleString('id-ID')})\n`;
      }
    });

    return {
      jenisLayanan: Array.from(listKategori).join(', '),
      jenisLaundry: rincianTeks
    };
  };

  // Hitung jumlah item selimut, sprei, boneka, korden dari cart untuk disimpan
  const getItemCounts = () => {
    let counts = { selimut: 0, sprei: 0, boneka: 0, korden: 0 };
    Object.keys(cart).forEach(serviceId => {
      const s = layanan.find(item => item.id === Number(serviceId));
      if (s) {
        const nameLower = s.kategori.toLowerCase() + ' ' + s.nama.toLowerCase();
        const qty = cart[serviceId];
        if (nameLower.includes('selimut')) counts.selimut += qty;
        else if (nameLower.includes('sprei')) counts.sprei += qty;
        else if (nameLower.includes('boneka')) counts.boneka += qty;
        else if (nameLower.includes('korden') || nameLower.includes('hordeng')) counts.korden += qty;
      }
    });
    return counts;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const totalHarga = getCartTotal();
    if (totalHarga === 0) return;

    const summary = getCartSummary();
    const counts = getItemCounts();

    const payload = {
      namaPelanggan: namaPelanggan.trim(),
      noHp: noHp.trim() || '-',
      jenisLayanan: summary.jenisLayanan,
      jenisLaundry: summary.jenisLaundry,
      totalHarga,
      statusBayar,
      lokasi: lokasi.trim() || null,
      ...counts
    };

    try {
      await api.post('/laundry/orders', payload);
      navigate('/orders-laundry');
    } catch (err) {
      alert(err.response?.data?.error || 'Gagal menyimpan pesanan laundry');
    }
  };

  const selectContact = (contact) => {
    setNamaPelanggan(contact.name);
    setNoHp(contact.phone || '');
    setShowContactModal(false);
  };

  const filteredContacts = pelangganUnik.filter(c => 
    c.name.toLowerCase().includes(cariKontak.toLowerCase())
  );

  const activeServices = layanan.filter(s => {
    const isProduct = s.kategori.toLowerCase().includes('barang');
    return activeTab === 'Plastik' ? isProduct : !isProduct;
  });

  const cartTotal = getCartTotal();

  return (
    <div style={{ padding: '1rem', maxWidth: '800px', margin: '0 auto', fontFamily: "'Inter', sans-serif", paddingBottom: '120px' }}>
      
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '1.5rem' }}>
        <button 
          onClick={() => navigate('/')} 
          style={{ background: 'white', border: '1px solid #E5E7EB', padding: '8px', borderRadius: '12px', cursor: 'pointer', display: 'flex', alignItems: 'center' }}
        >
          <ChevronLeft size={20} color="#374151" />
        </button>
        <h1 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#111827', margin: 0 }}>Buat Transaksi Laundry</h1>
      </div>

      <form onSubmit={handleSubmit}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>

          {/* Customer Card */}
          <div className="glass-panel" style={{ background: 'white', borderRadius: '18px', padding: '1.25rem', border: '1px solid #E5E7EB' }}>
            <h3 style={{ fontSize: '0.9rem', fontWeight: 800, color: '#374151', margin: '0 0 1rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Data Pelanggan
            </h3>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginBottom: '6px' }}>Nama Pelanggan</label>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <input
                    type="text"
                    required
                    placeholder="Masukkan nama pelanggan"
                    value={namaPelanggan}
                    onChange={e => setNamaPelanggan(e.target.value)}
                    style={{ flex: 1, padding: '10px 14px', borderRadius: '10px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.9rem' }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowContactModal(true)}
                    style={{
                      background: 'linear-gradient(135deg, #EF4444, #DC2626)',
                      color: 'white', border: 'none', borderRadius: '10px',
                      padding: '0 16px', fontWeight: 700, fontSize: '0.85rem',
                      display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer'
                    }}
                  >
                    <Users size={16} /> Pilih
                  </button>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginBottom: '6px' }}>Nomor WhatsApp</label>
                  <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                    <Phone size={16} color="#9CA3AF" style={{ position: 'absolute', left: '12px' }} />
                    <input
                      type="number"
                      placeholder="081234xxx"
                      value={noHp}
                      onChange={e => setNoHp(e.target.value)}
                      style={{ width: '100%', padding: '10px 12px 10px 36px', borderRadius: '10px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.9rem' }}
                    />
                  </div>
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginBottom: '6px' }}>Lokasi / Alamat</label>
                  <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                    <MapPin size={16} color="#9CA3AF" style={{ position: 'absolute', left: '12px' }} />
                    <input
                      type="text"
                      placeholder="Bandung, Ruko B4"
                      value={lokasi}
                      onChange={e => setLokasi(e.target.value)}
                      style={{ width: '100%', padding: '10px 12px 10px 36px', borderRadius: '10px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.9rem' }}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Mode Tabs */}
          <div style={{ display: 'flex', background: '#F3F4F6', borderRadius: '14px', padding: '4px' }}>
            <button
              type="button"
              onClick={() => setActiveTab('Laundry')}
              style={{
                flex: 1, padding: '10px', border: 'none', borderRadius: '10px',
                fontWeight: 700, fontSize: '0.85rem', cursor: 'pointer',
                background: activeTab === 'Laundry' ? 'white' : 'transparent',
                color: activeTab === 'Laundry' ? '#EF4444' : '#6B7280',
                boxShadow: activeTab === 'Laundry' ? '0 2px 8px rgba(0,0,0,0.06)' : 'none',
                transition: 'all 0.2s'
              }}
            >
              🧺 Order Laundry
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('Plastik')}
              style={{
                flex: 1, padding: '10px', border: 'none', borderRadius: '10px',
                fontWeight: 700, fontSize: '0.85rem', cursor: 'pointer',
                background: activeTab === 'Plastik' ? 'white' : 'transparent',
                color: activeTab === 'Plastik' ? '#EF4444' : '#6B7280',
                boxShadow: activeTab === 'Plastik' ? '0 2px 8px rgba(0,0,0,0.06)' : 'none',
                transition: 'all 0.2s'
              }}
            >
              🛍️ Barang Pendukung
            </button>
          </div>

          {/* Services List */}
          <div>
            <h3 style={{ fontSize: '0.9rem', fontWeight: 800, color: '#374151', margin: '0 0 0.75rem px-1', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Pilih Layanan & Produk
            </h3>

            {loading ? (
              <div style={{ textAlign: 'center', padding: '2rem', color: '#9CA3AF' }}>Memuat paket layanan...</div>
            ) : activeServices.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {activeServices.map(item => {
                  const qty = cart[item.id] || 0;
                  return (
                    <div 
                      key={item.id} 
                      style={{
                        background: 'white', border: '1px solid #E5E7EB', borderRadius: '14px',
                        padding: '12px 14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                        boxShadow: '0 2px 4px rgba(0,0,0,0.02)'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div style={{ fontSize: '1.5rem', background: '#F9FAFB', width: '44px', height: '44px', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          {item.icon || '🧺'}
                        </div>
                        <div>
                          <div style={{ fontWeight: 800, fontSize: '0.9rem', color: '#111827' }}>
                            {item.kategori} - {item.nama}
                          </div>
                          <div style={{ fontSize: '0.75rem', color: '#6B7280', marginTop: '2px' }}>
                            {item.proses}
                          </div>
                          <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#EF4444', marginTop: '4px' }}>
                            Rp {item.harga.toLocaleString('id-ID')} / {item.satuan} · <span style={{ color: '#9CA3AF', fontWeight: 400 }}>{item.waktu}</span>
                          </div>
                        </div>
                      </div>

                      {/* Qty Controls */}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', background: '#F9FAFB', borderRadius: '20px', padding: '4px 8px', border: '1px solid #E5E7EB' }}>
                        <button
                          type="button"
                          onClick={() => handleQtyChange(item, -1)}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#EF4444', fontSize: '1.1rem', fontWeight: 900, padding: '0 6px' }}
                        >
                          <Minus size={14} strokeWidth={3} />
                        </button>
                        <span style={{ minWidth: '24px', textAlign: 'center', fontWeight: 800, color: '#111827', fontSize: '0.9rem' }}>
                          {qty}
                        </span>
                        <button
                          type="button"
                          onClick={() => handleQtyChange(item, 1)}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#EF4444', fontSize: '1.1rem', fontWeight: 900, padding: '0 6px' }}
                        >
                          <Plus size={14} strokeWidth={3} />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '2rem', background: 'white', borderRadius: '14px', border: '1px solid #E5E7EB', color: '#6B7280', fontSize: '0.85rem' }}>
                Belum ada paket layanan/produk di kategori ini.
              </div>
            )}
          </div>

          {/* Payment Card */}
          <div className="glass-panel" style={{ background: 'white', borderRadius: '18px', padding: '1.25rem', border: '1px solid #E5E7EB' }}>
            <h3 style={{ fontSize: '0.9rem', fontWeight: 800, color: '#374151', margin: '0 0 1rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Pembayaran
            </h3>
            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginBottom: '6px' }}>Status Pembayaran</label>
              <select
                value={statusBayar}
                onChange={e => setStatusBayar(e.target.value)}
                style={{ width: '100%', padding: '10px 14px', borderRadius: '10px', border: '1px solid #D1D5DB', background: '#F9FAFB', outline: 'none', fontSize: '0.9rem', fontWeight: 600 }}
              >
                <option value="Belum Lunas">Belum Lunas (Bayar Nanti)</option>
                <option value="Lunas">Lunas (Bayar Sekarang)</option>
              </select>
            </div>
          </div>

        </div>

        {/* Bottom Cart Bar */}
        <div style={{
          position: 'fixed', bottom: 0, left: 0, right: 0,
          background: 'rgba(255,255,255,0.9)', backdropFilter: 'blur(10px)',
          borderTop: '1px solid #E5E7EB', padding: '12px 16px', zIndex: 1000,
          boxShadow: '0 -4px 20px rgba(0,0,0,0.06)'
        }}>
          <div style={{ maxWidth: '800px', margin: '0 auto', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '16px' }}>
            <div>
              <div style={{ fontSize: '0.75rem', color: '#6B7280', fontWeight: 600 }}>Total Pembayaran</div>
              <div style={{ fontSize: '1.35rem', fontWeight: 900, color: '#EF4444', marginTop: '2px' }}>
                Rp {cartTotal.toLocaleString('id-ID')}
              </div>
            </div>
            <button
              type="submit"
              disabled={cartTotal === 0}
              style={{
                flex: 1, maxWidth: '240px', background: cartTotal === 0 ? '#E5E7EB' : 'linear-gradient(135deg, #EF4444, #DC2626)',
                color: cartTotal === 0 ? '#9CA3AF' : 'white', border: 'none', borderRadius: '12px',
                padding: '14px', fontWeight: 800, fontSize: '0.95rem', cursor: cartTotal === 0 ? 'not-allowed' : 'pointer',
                boxShadow: cartTotal === 0 ? 'none' : '0 4px 14px rgba(239,68,68,0.3)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', transition: 'all 0.2s'
              }}
            >
              <ShoppingCart size={18} /> Simpan Transaksi
            </button>
          </div>
        </div>
      </form>

      {/* Customer Contact Selector Modal */}
      {showContactModal && (
        <div style={{
          position: 'fixed', inset: 0, zIndex: 9999,
          background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem'
        }}>
          <div style={{
            background: 'white', borderRadius: '20px', padding: '1.5rem',
            maxWidth: '440px', width: '100%', maxHeight: '80vh', display: 'flex', flexDirection: 'column',
            boxShadow: '0 20px 40px rgba(0,0,0,0.15)',
            animation: 'slideUp 0.2s ease'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h4 style={{ fontWeight: 800, fontSize: '1.1rem', color: '#111827', margin: 0 }}>Pilih Pelanggan</h4>
              <button 
                onClick={() => setShowContactModal(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#6B7280', fontSize: '1.25rem', fontWeight: 700 }}
              >
                ✕
              </button>
            </div>
            
            <input
              type="text"
              placeholder="🔍 Cari nama pelanggan..."
              value={cariKontak}
              onChange={e => setCariKontak(e.target.value)}
              style={{ width: '100%', padding: '10px 12px', borderRadius: '10px', border: '1px solid #D1D5DB', marginBottom: '1rem', outline: 'none', fontSize: '0.85rem' }}
            />

            <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {filteredContacts.length > 0 ? (
                filteredContacts.map(contact => (
                  <div
                    key={contact.id}
                    onClick={() => selectContact(contact)}
                    style={{
                      background: '#F9FAFB', border: '1px solid #E5E7EB', borderRadius: '12px',
                      padding: '12px', cursor: 'pointer', transition: 'all 0.15s',
                      display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                    }}
                    onMouseEnter={e => { e.currentTarget.style.background = '#FEF2F2'; e.currentTarget.style.borderColor = '#FCA5A5'; }}
                    onMouseLeave={e => { e.currentTarget.style.background = '#F9FAFB'; e.currentTarget.style.borderColor = '#E5E7EB'; }}
                  >
                    <div>
                      <div style={{ fontWeight: 800, fontSize: '0.88rem', color: '#111827' }}>{contact.name}</div>
                      <div style={{ fontSize: '0.75rem', color: '#6B7280', marginTop: '2px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        📞 {contact.phone || 'Tidak ada no WA'}
                      </div>
                    </div>
                    <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#EF4444' }}>Pilih ➔</span>
                  </div>
                ))
              ) : (
                <div style={{ textAlign: 'center', padding: '2rem', color: '#9CA3AF', fontSize: '0.8rem' }}>
                  Belum ada data pelanggan yang cocok.
                </div>
              )}
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
