import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Printer } from 'lucide-react';
import api from '../api';

export default function StrukLaundry() {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOrder = async () => {
      try {
        const res = await api.get(`/laundry/orders/${id}`);
        setOrder(res.data);
      } catch (err) {
        console.error('Failed to fetch laundry order:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchOrder();
  }, [id]);

  const handlePrint = () => {
    window.print();
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '3rem', fontFamily: 'sans-serif' }}>Memuat struk...</div>;
  }

  if (!order) {
    return (
      <div style={{ textAlign: 'center', padding: '3rem', fontFamily: 'sans-serif' }}>
        <h3>Struk tidak ditemukan</h3>
        <button onClick={() => navigate('/orders-laundry')} style={{ marginTop: '1rem', padding: '8px 16px' }}>Kembali</button>
      </div>
    );
  }

  const formatRupiah = (val) => {
    return "Rp " + Number(val || 0).toLocaleString('id-ID');
  };

  const dateObj = new Date(order.tanggalMasuk);
  const formatTanggal = dateObj.toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });

  return (
    <div style={{ background: '#f3f4f6', minHeight: '100vh', padding: '2rem 1rem', fontFamily: "'Courier New', Courier, monospace" }}>
      
      {/* Navigation and Print Controls (Hidden on Print) */}
      <style>{`
        @media print {
          .no-print {
            display: none !important;
          }
          body {
            background: white !important;
            padding: 0 !important;
          }
          .receipt-card {
            border: none !important;
            box-shadow: none !important;
            padding: 0 !important;
            width: 100% !important;
            max-width: 100% !important;
            background: white !important;
          }
        }
      `}</style>
      
      <div className="no-print" style={{ maxWidth: '400px', margin: '0 auto 1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <button
          onClick={() => navigate('/orders-laundry')}
          style={{
            background: 'white', border: '1px solid #d1d5db', padding: '8px 12px',
            borderRadius: '10px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px',
            fontFamily: 'sans-serif', fontSize: '0.85rem', fontWeight: 700, color: '#374151'
          }}
        >
          <ChevronLeft size={16} /> Kembali
        </button>
        <button
          onClick={handlePrint}
          style={{
            background: 'linear-gradient(135deg, #4F46E5, #3730A3)', color: 'white',
            border: 'none', padding: '8px 16px', borderRadius: '10px', cursor: 'pointer',
            display: 'flex', alignItems: 'center', gap: '6px', fontFamily: 'sans-serif',
            fontSize: '0.85rem', fontWeight: 700, boxShadow: '0 4px 12px rgba(79,70,229,0.3)'
          }}
        >
          <Printer size={16} /> Cetak Struk
        </button>
      </div>

      {/* Thermal Receipt Card */}
      <div 
        className="receipt-card"
        style={{
          background: 'white', border: '1px solid #e5e7eb', borderRadius: '8px',
          padding: '1.5rem', maxWidth: '380px', margin: '0 auto',
          boxShadow: '0 10px 15px -3px rgba(0,0,0,0.05)', color: '#111827'
        }}
      >
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <h2 style={{ fontSize: '1.3rem', fontWeight: 900, margin: '0 0 6px', letterSpacing: '1px' }}>KAPAS LAUNDRY</h2>
          <p style={{ fontSize: '0.75rem', margin: '0 0 4px' }}>Ruko Buah Batu Regency Blok B-3</p>
          <p style={{ fontSize: '0.75rem', margin: '0 0 4px' }}>Telp/WA: 0822-4507-7959</p>
          <p style={{ fontSize: '0.75rem', margin: '0' }}>----------------------------------</p>
        </div>

        {/* Info Order */}
        <div style={{ fontSize: '0.78rem', display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>No. Nota :</span>
            <span style={{ fontWeight: 'bold' }}>{order.receiptNumber}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>Tanggal  :</span>
            <span>{formatTanggal}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>Pelanggan:</span>
            <span style={{ fontWeight: 'bold' }}>{order.namaPelanggan}</span>
          </div>
          {order.noHp && order.noHp !== '-' && (
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>WA       :</span>
              <span>{order.noHp}</span>
            </div>
          )}
          {order.lokasi && (
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>Alamat   :</span>
              <span>{order.lokasi}</span>
            </div>
          )}
          {order.employee?.name && (
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>Kasir    :</span>
              <span>{order.employee.name}</span>
            </div>
          )}
          <p style={{ fontSize: '0.75rem', margin: '6px 0 0', textAlign: 'center' }}>==================================</p>
        </div>

        {/* Items List */}
        <div style={{ fontSize: '0.78rem', marginBottom: '1rem' }}>
          <div style={{ fontWeight: 'bold', marginBottom: '6px', textTransform: 'uppercase' }}>Rincian Cucian:</div>
          <div style={{ whiteSpace: 'pre-line', lineHeight: 1.5, background: '#f9fafb', padding: '8px', borderRadius: '4px', border: '1px dashed #d1d5db' }}>
            {order.jenisLaundry}
          </div>
        </div>

        {/* Special Item Counts (If Any) */}
        {(order.selimut > 0 || order.sprei > 0 || order.boneka > 0 || order.korden > 0) && (
          <div style={{ fontSize: '0.75rem', marginBottom: '1rem' }}>
            <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>Detail Item:</div>
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              {order.selimut > 0 && <span>🛏️ Selimut: {order.selimut}</span>}
              {order.sprei > 0 && <span>🛌 Sprei: {order.sprei}</span>}
              {order.boneka > 0 && <span>🧸 Boneka: {order.boneka}</span>}
              {order.korden > 0 && <span>🖼️ Korden: {order.korden}</span>}
            </div>
            <p style={{ fontSize: '0.75rem', margin: '8px 0 0', textAlign: 'center' }}>----------------------------------</p>
          </div>
        )}

        {/* Total & Status */}
        <div style={{ fontSize: '0.85rem', display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold' }}>
            <span>TOTAL TAGIHAN:</span>
            <span style={{ fontSize: '1rem' }}>{formatRupiah(order.totalHarga)}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold' }}>
            <span>STATUS BAYAR :</span>
            <span style={{ color: order.statusBayar === 'Lunas' ? '#2E7D32' : '#C62828' }}>
              {order.statusBayar.toUpperCase()}
            </span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>STATUS PROSES:</span>
            <span style={{ fontWeight: 'bold' }}>{order.status.toUpperCase()}</span>
          </div>
        </div>

        {/* Footer Note */}
        <div style={{ textAlign: 'center', fontSize: '0.7rem', lineHeight: 1.4, marginTop: '1.5rem', borderTop: '1px dashed #d1d5db', paddingTop: '10px' }}>
          <p style={{ margin: '0 0 4px', fontWeight: 'bold' }}>Syarat &amp; Ketentuan:</p>
          <p style={{ margin: '0 0 4px' }}>1. Cucian tidak diambil dalam 30 hari di luar tanggung jawab kami.</p>
          <p style={{ margin: '0 0 4px' }}>2. Klaim kerusakan/kehilangan maks 24 jam setelah diambil disertai struk asli.</p>
          <p style={{ margin: '8px 0 0', fontWeight: 'bold', fontSize: '0.75rem' }}>Terima kasih atas kunjungan Anda!</p>
        </div>

      </div>

    </div>
  );
}
