import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ChevronLeft, Search, RefreshCw, Printer, Trash2,
  CheckCircle, Clock, AlertTriangle, Send, MoreVertical, Edit2
} from 'lucide-react';
import api from '../api';
import { useAuth, useIsAdmin } from '../AuthContext';

export default function OrderLaundry() {
  const navigate = useNavigate();
  const isAdmin = useIsAdmin();
  const { user } = useAuth();

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cari, setCari] = useState('');
  const [filterBayar, setFilterBayar] = useState('Semua');
  const [filterTipe, setFilterTipe] = useState('Semua');

  const loadOrders = async () => {
    setLoading(true);
    try {
      const res = await api.get('/laundry/orders', {
        params: { cari, filterBayar, filterTipe }
      });
      setOrders(res.data);
    } catch (err) {
      console.error('Failed to load laundry orders:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders();
  }, [filterBayar, filterTipe]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    loadOrders();
  };

  const handleTogglePayment = async (orderId) => {
    try {
      const res = await api.put(`/laundry/orders/pay/${orderId}`);
      setOrders(prev => prev.map(o => o.id === orderId ? res.data : o));
    } catch (err) {
      alert(err.response?.data?.error || 'Gagal mengubah status pembayaran');
    }
  };

  const handleUpdateStatus = async (orderId, newStatus) => {
    try {
      const res = await api.put(`/laundry/orders/status/${orderId}`, { status: newStatus });
      setOrders(prev => prev.map(o => o.id === orderId ? res.data : o));
    } catch (err) {
      alert(err.response?.data?.error || 'Gagal memperbarui status');
    }
  };

  const handleDeleteOrder = async (orderId) => {
    if (!window.confirm('Apakah Anda yakin ingin menghapus pesanan laundry ini?')) return;
    try {
      await api.delete(`/laundry/orders/${orderId}`);
      setOrders(prev => prev.filter(o => o.id !== orderId));
    } catch (err) {
      alert(err.response?.data?.error || 'Gagal menghapus pesanan');
    }
  };

  const sendWhatsApp = (order) => {
    if (!order.noHp || order.noHp === '-') {
      alert('Pelanggan tidak memiliki nomor HP/WhatsApp yang valid.');
      return;
    }

    const cleanHp = order.noHp.replace(/[^0-9]/g, '');
    const formatHp = cleanHp.startsWith('0') ? '62' + cleanHp.slice(1) : cleanHp;
    
    let message = '';
    if (order.status === 'Selesai') {
      message = `Halo Kak ${order.namaPelanggan}, cucian Kakak dengan nomor nota ${order.receiptNumber} sudah selesai di-proses dan SIAP DIAMBIL di Kapas Laundry. \n\nDetail tagihan: Rp ${order.totalHarga.toLocaleString('id-ID')} (${order.statusBayar}).\n\nTerima kasih banyak! 😊🙏`;
    } else {
      message = `Halo Kak ${order.namaPelanggan}, ini adalah rincian nota pesanan laundry Kakak di Kapas Laundry:\n\nNo. Nota: ${order.receiptNumber}\nLayanan: ${order.jenisLayanan}\nTotal Biaya: Rp ${order.totalHarga.toLocaleString('id-ID')} (${order.statusBayar})\nStatus Proses: ${order.status}.\n\nTerima kasih telah mempercayai kami! 😊🙏`;
    }

    const waUrl = `https://api.whatsapp.com/send?phone=${formatHp}&text=${encodeURIComponent(message)}`;
    window.open(waUrl, '_blank');
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'Diambil': return { bg: '#E1F5FE', text: '#0288D1' };
      case 'Selesai': return { bg: '#E8F5E9', text: '#2E7D32' };
      case 'Proses': return { bg: '#FFF3E0', text: '#EF6C00' };
      default: return { bg: '#ECEFF1', text: '#455A64' }; // Menunggu
    }
  };

  const getBayarColor = (statusBayar) => {
    return statusBayar === 'Lunas' 
      ? { bg: '#E8F5E9', text: '#2E7D32', border: '1px solid #A5D6A7' }
      : { bg: '#FFEBEE', text: '#C62828', border: '1px solid #EF9A9A' };
  };

  return (
    <div style={{ padding: '1rem', maxWidth: '1000px', margin: '0 auto', fontFamily: "'Inter', sans-serif" }}>
      
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button 
            onClick={() => navigate('/')} 
            style={{ background: 'white', border: '1px solid #E5E7EB', padding: '8px', borderRadius: '12px', cursor: 'pointer', display: 'flex', alignItems: 'center' }}
          >
            <ChevronLeft size={20} color="#374151" />
          </button>
          <h1 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#111827', margin: 0 }}>Riwayat Order Laundry</h1>
        </div>
        <button 
          onClick={() => navigate('/baru-laundry')}
          style={{
            background: 'linear-gradient(135deg, #EF4444, #DC2626)',
            color: 'white', border: 'none', borderRadius: '12px',
            padding: '10px 18px', fontWeight: 800, fontSize: '0.85rem', cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(239,68,68,0.25)'
          }}
        >
          ➕ Transaksi Baru
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="glass-panel" style={{ background: 'white', borderRadius: '18px', padding: '1.25rem', border: '1px solid #E5E7EB', marginBottom: '1.5rem' }}>
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginBottom: '12px' }}>
          <div style={{ flex: 1, minWidth: '240px', position: 'relative', display: 'flex', alignItems: 'center' }}>
            <Search size={18} color="#9CA3AF" style={{ position: 'absolute', left: '12px' }} />
            <input
              type="text"
              placeholder="Cari nama pelanggan..."
              value={cari}
              onChange={e => setCari(e.target.value)}
              style={{ width: '100%', padding: '10px 12px 10px 38px', borderRadius: '10px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.88rem' }}
            />
          </div>
          <button 
            type="submit"
            style={{
              background: '#374151', color: 'white', border: 'none', borderRadius: '10px',
              padding: '10px 20px', fontWeight: 700, fontSize: '0.88rem', cursor: 'pointer'
            }}
          >
            Cari
          </button>
        </form>

        <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', borderTop: '1px solid #F3F4F6', paddingTop: '12px' }}>
          <div>
            <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#6B7280', marginRight: '8px' }}>Status Bayar:</span>
            <select
              value={filterBayar}
              onChange={e => setFilterBayar(e.target.value)}
              style={{ padding: '6px 10px', borderRadius: '8px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.8rem', fontWeight: 600 }}
            >
              <option value="Semua">Semua</option>
              <option value="Lunas">Lunas</option>
              <option value="Belum Lunas">Belum Lunas</option>
            </select>
          </div>

          <div>
            <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#6B7280', marginRight: '8px' }}>Tipe Order:</span>
            <select
              value={filterTipe}
              onChange={e => setFilterTipe(e.target.value)}
              style={{ padding: '6px 10px', borderRadius: '8px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.8rem', fontWeight: 600 }}
            >
              <option value="Semua">Semua</option>
              <option value="Laundry">Jasa Laundry</option>
              <option value="Plastik">Barang Pendukung</option>
            </select>
          </div>
        </div>
      </div>

      {/* Orders List */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#9CA3AF' }}>Memuat data pesanan...</div>
      ) : orders.length > 0 ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {orders.map(order => {
            const statusColor = getStatusColor(order.status);
            const bayarColor = getBayarColor(order.statusBayar);
            const dateObj = new Date(order.tanggalMasuk);
            const formatTanggal = dateObj.toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
            
            return (
              <div 
                key={order.id}
                style={{
                  background: 'white', border: '1px solid #E5E7EB', borderRadius: '18px',
                  padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '12px',
                  boxShadow: '0 2px 10px rgba(0,0,0,0.02)'
                }}
              >
                {/* Row 1: Receipt & Date & Actions */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #F3F4F6', paddingBottom: '8px' }}>
                  <div>
                    <span style={{ fontSize: '0.8rem', fontWeight: 800, color: '#374151' }}>{order.receiptNumber}</span>
                    <span style={{ fontSize: '0.75rem', color: '#9CA3AF', marginLeft: '8px' }}>{formatTanggal} WIB</span>
                  </div>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button
                      onClick={() => navigate(`/struk-laundry/${order.id}`)}
                      title="Cetak Struk"
                      style={{ background: '#F3F4F6', border: 'none', borderRadius: '8px', padding: '6px 10px', cursor: 'pointer', color: '#4F46E5', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.75rem', fontWeight: 700 }}
                    >
                      <Printer size={13} /> Cetak
                    </button>
                    <button
                      onClick={() => sendWhatsApp(order)}
                      title="Kirim WA"
                      style={{ background: '#E8F5E9', border: 'none', borderRadius: '8px', padding: '6px 10px', cursor: 'pointer', color: '#2E7D32', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.75rem', fontWeight: 700 }}
                    >
                      <Send size={13} /> WA
                    </button>
                    {isAdmin && (
                      <button
                        onClick={() => handleDeleteOrder(order.id)}
                        title="Hapus"
                        style={{ background: '#FFEBEE', border: 'none', borderRadius: '8px', padding: '6px 10px', cursor: 'pointer', color: '#C62828' }}
                      >
                        <Trash2 size={13} />
                      </button>
                    )}
                  </div>
                </div>

                {/* Row 2: Customer Name, Details & Price */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: '16px', alignItems: 'start' }}>
                  <div>
                    <div style={{ fontWeight: 900, fontSize: '0.95rem', color: '#111827' }}>
                      {order.namaPelanggan} {order.noHp && order.noHp !== '-' && <span style={{ fontWeight: 400, color: '#6B7280', fontSize: '0.8rem' }}>({order.noHp})</span>}
                    </div>
                    <div style={{ fontSize: '0.8rem', color: '#4B5563', marginTop: '6px', whiteSpace: 'pre-line', lineHeight: 1.4, background: '#F9FAFB', padding: '8px 12px', borderRadius: '10px', border: '1px solid #F3F4F6' }}>
                      {order.jenisLaundry}
                      {order.lokasi && <div style={{ marginTop: '4px', fontSize: '0.75rem', color: '#6B7280', display: 'flex', alignItems: 'center', gap: '3px' }}>📍 Alamat: {order.lokasi}</div>}
                    </div>
                  </div>

                  <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', gap: '6px', alignItems: 'flex-end' }}>
                    <div style={{ fontSize: '0.75rem', color: '#9CA3AF' }}>Total Biaya</div>
                    <div style={{ fontWeight: 900, fontSize: '1.2rem', color: '#EF4444' }}>
                      Rp {order.totalHarga.toLocaleString('id-ID')}
                    </div>
                    
                    {/* Clickable Payment Status Button */}
                    <button
                      onClick={() => handleTogglePayment(order.id)}
                      style={{
                        background: bayarColor.bg, color: bayarColor.text, border: bayarColor.border,
                        borderRadius: '20px', padding: '4px 10px', fontSize: '0.75rem', fontWeight: 800,
                        cursor: 'pointer', transition: 'all 0.15s'
                      }}
                      onMouseEnter={e => e.currentTarget.style.filter = 'brightness(0.95)'}
                      onMouseLeave={e => e.currentTarget.style.filter = 'none'}
                    >
                      {order.statusBayar === 'Lunas' ? '✅ Lunas' : '❌ Belum Lunas'}
                    </button>
                  </div>
                </div>

                {/* Row 3: Status Flow Buttons */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', background: '#F9FAFB', padding: '8px 12px', borderRadius: '12px', border: '1px solid #E5E7EB', marginTop: '4px', flexWrap: 'wrap' }}>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginRight: '6px' }}>Status Proses:</span>
                  <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                    {['Menunggu', 'Proses', 'Selesai', 'Diambil'].map(st => {
                      const isActive = order.status === st;
                      return (
                        <button
                          key={st}
                          onClick={() => handleUpdateStatus(order.id, st)}
                          style={{
                            border: 'none', borderRadius: '8px', padding: '6px 12px',
                            fontSize: '0.75rem', fontWeight: isActive ? 800 : 600,
                            cursor: 'pointer',
                            background: isActive ? statusColor.bg : '#E5E7EB',
                            color: isActive ? statusColor.text : '#4B5563',
                            transition: 'all 0.15s'
                          }}
                        >
                          {st === 'Menunggu' && '⏳ '}
                          {st === 'Proses' && '🔄 '}
                          {st === 'Selesai' && '✨ '}
                          {st === 'Diambil' && '🚚 '}
                          {st}
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div style={{ textAlign: 'center', padding: '4rem', background: 'white', borderRadius: '20px', border: '1px solid #E5E7EB', color: '#9CA3AF' }}>
          Belum ada data pesanan laundry yang masuk.
        </div>
      )}

    </div>
  );
}
