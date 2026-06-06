import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Printer, Settings, X } from 'lucide-react';
import api from '../api';

export default function StrukLaundry() {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  const [receiptSettings, setReceiptSettings] = useState(() => {
    try {
      const saved = localStorage.getItem('posbah_receipt_settings_laundry');
      if (saved) {
        const parsed = JSON.parse(saved);
        return {
          paperSize: '80mm',
          ...parsed
        };
      }
    } catch (e) {}
    return {
      storeName: 'KAPAS LAUNDRY',
      subheader: 'Ruko Buah Batu Regency Blok B-3\nTelp/WA: 0822-4507-7959',
      footer: "Syarat & Ketentuan:\n1. Cucian tidak diambil dalam 30 hari di luar tanggung jawab kami.\n2. Klaim kerusakan/kehilangan maks 24 jam setelah diambil disertai struk asli.\n\nTerima kasih atas kunjungan Anda!",
      paperSize: '80mm'
    };
  });

  const [settingsModalOpen, setSettingsModalOpen] = useState(false);
  const [tempStoreName, setTempStoreName] = useState(receiptSettings.storeName);
  const [tempSubheader, setTempSubheader] = useState(receiptSettings.subheader);
  const [tempFooter, setTempFooter] = useState(receiptSettings.footer);
  const [tempPaperSize, setTempPaperSize] = useState(receiptSettings.paperSize || '80mm');

  useEffect(() => {
    if (settingsModalOpen) {
      setTempStoreName(receiptSettings.storeName);
      setTempSubheader(receiptSettings.subheader);
      setTempFooter(receiptSettings.footer);
      setTempPaperSize(receiptSettings.paperSize || '80mm');
    }
  }, [settingsModalOpen, receiptSettings]);

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
    const w = window.open('', '_blank');
    if (!w) {
      alert('Pencetakan sistem (browser) tidak didukung di APK. Silakan gunakan printer Bluetooth.');
      return;
    }
    w.document.write(buildLaundryReceiptHTML(order, receiptSettings.paperSize || '80mm'));
    w.document.close();
  };

  const buildLaundryReceiptHTML = (o, size) => {
    let settings = receiptSettings;
    try {
      const saved = localStorage.getItem('posbah_receipt_settings_laundry');
      if (saved) settings = JSON.parse(saved);
    } catch (e) {}

    const storeName = settings?.storeName || 'KAPAS LAUNDRY';
    const subheader = settings?.subheader || 'Ruko Buah Batu Regency Blok B-3\nTelp/WA: 0822-4507-7959';
    const footer = settings?.footer || "Syarat & Ketentuan:\n1. Cucian tidak diambil dalam 30 hari di luar tanggung jawab kami.\n2. Klaim kerusakan/kehilangan maks 24 jam setelah diambil disertai struk asli.\n\nTerima kasih atas kunjungan Anda!";

    const formatRupiah = (val) => {
      return "Rp " + Number(val || 0).toLocaleString('id-ID');
    };
    
    const dateObj = new Date(o.tanggalMasuk || Date.now());
    const formatTanggal = dateObj.toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });

    const sizeWidth = size === '58mm' ? '58mm' : '80mm';
    const paddingVal = size === '58mm' ? '5px' : '15px';

    const waLine = o.noHp && o.noHp !== '-' ? `<tr><td>WA</td><td>: ${o.noHp}</td></tr>` : '';
    const alamatLine = o.lokasi ? `<tr><td>Alamat</td><td>: ${o.lokasi}</td></tr>` : '';
    const kasirLine = o.employee?.name ? `<tr><td>Kasir</td><td>: ${o.employee.name}</td></tr>` : '';

    let itemsDetails = '';
    if (o.selimut > 0 || o.sprei > 0 || o.boneka > 0 || o.korden > 0) {
      itemsDetails = `
        <div class="b">Detail Item:</div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;font-size:11px;margin-bottom:6px">
          ${o.selimut > 0 ? `<span>🛏️ Selimut: ${o.selimut}</span>` : ''}
          ${o.sprei > 0 ? `<span>🛌 Sprei: ${o.sprei}</span>` : ''}
          ${o.boneka > 0 ? `<span>🧸 Boneka: ${o.boneka}</span>` : ''}
          ${o.korden > 0 ? `<span>🖼️ Korden: ${o.korden}</span>` : ''}
        </div>
        <hr>
      `;
    }

    return `
      <html>
        <head>
          <style>
            @page { margin: 0; }
            body {
              font-family: monospace;
              width: ${sizeWidth};
              margin: 0 auto;
              padding: ${paddingVal};
              font-size: 12px;
              color: #000;
              background: #fff;
            }
            .c { text-align: center; }
            .b { font-weight: bold; }
            .r { text-align: right; }
            hr { border-top: 1px dashed #000; border-bottom: none; border-left: none; border-right: none; margin: 8px 0; }
            table { width: 100%; border-collapse: collapse; }
            td { vertical-align: top; padding: 2px 0; }
            .title { font-size: 16px; font-weight: bold; margin-bottom: 4px; }
            .subtitle { font-size: 11px; margin-bottom: 4px; }
            .footer { font-size: 10px; margin-top: 10px; }
          </style>
        </head>
        <body>
          <div class="c title">${storeName}</div>
          <div class="c subtitle">${subheader.replace(/\n/g, '<br>')}</div>
          <hr>
          <table>
            <tr><td>No. Nota</td><td>: ${o.receiptNumber}</td></tr>
            <tr><td>Tanggal</td><td>: ${formatTanggal}</td></tr>
            <tr><td>Pelanggan</td><td>: ${o.namaPelanggan}</td></tr>
            ${waLine}
            ${alamatLine}
            ${kasirLine}
          </table>
          <hr>
          <div class="b" style="margin-bottom:4px">Rincian Cucian:</div>
          <div style="white-space:pre-line;line-height:1.4;padding:4px;border:1px dashed #000;margin-bottom:6px">
            ${o.jenisLaundry}
          </div>
          <hr>
          ${itemsDetails}
          <table>
            <tr class="b"><td>TOTAL TAGIHAN</td><td class="r" style="font-size:14px">${formatRupiah(o.totalHarga)}</td></tr>
            <tr class="b"><td>STATUS BAYAR</td><td class="r">${String(o.statusBayar).toUpperCase()}</td></tr>
            <tr class="b"><td>STATUS PROSES</td><td class="r">${String(o.status).toUpperCase()}</td></tr>
          </table>
          <hr>
          <div class="c footer">${footer.replace(/\n/g, '<br>')}</div>
          <script>
            window.onload = function() {
              window.print();
              window.onafterprint = function() {
                window.close();
              }
            }
          </script>
        </body>
      </html>
    `;
  };

  const printTestReceipt = (size) => {
    const dummyOrder = {
      receiptNumber: "N-TEST-LAUNDRY",
      tanggalMasuk: new Date().toISOString(),
      namaPelanggan: "Pelanggan Demo",
      noHp: "0812-3456-7890",
      lokasi: "Jl. Clean & Fresh No. 123",
      jenisLaundry: "Cuci Komplit Reguler - 5 Kg @ Rp7.000\nCuci Setrika Sprei - 1 Pcs @ Rp15.000",
      selimut: 1,
      sprei: 1,
      boneka: 0,
      korden: 0,
      totalHarga: 50000,
      statusBayar: "Lunas",
      status: "Selesai",
      employee: {
        name: "Kasir Laundry"
      }
    };

    const w = window.open('', '_blank');
    if (!w) {
      alert('Pencetakan sistem (browser) tidak didukung di APK. Silakan gunakan printer Bluetooth.');
      return;
    }
    w.document.write(buildLaundryReceiptHTML(dummyOrder, size));
    w.document.close();
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
          @page {
            margin: 0;
          }
          body {
            background: white !important;
            padding: 0.6cm !important;
            margin: 0 !important;
          }
          .no-print {
            display: none !important;
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
      
      <div className="no-print" style={{ maxWidth: '400px', margin: '0 auto 1.5rem', display: 'flex', gap: '8px', justifyContent: 'space-between', alignItems: 'center' }}>
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

        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            onClick={() => setSettingsModalOpen(true)}
            style={{
              background: 'white', border: '1px solid #d1d5db', padding: '8px 12px',
              borderRadius: '10px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px',
              fontFamily: 'sans-serif', fontSize: '0.85rem', fontWeight: 700, color: '#374151'
            }}
          >
            <Settings size={16} /> Edit Struk
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
          <h2 style={{ fontSize: '1.3rem', fontWeight: 900, margin: '0 0 6px', letterSpacing: '1px' }}>{receiptSettings.storeName}</h2>
          {receiptSettings.subheader.split('\n').map((line, idx) => (
            <p key={idx} style={{ fontSize: '0.75rem', margin: '0 0 4px' }}>{line}</p>
          ))}
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
          <div style={{ whiteSpace: 'pre-wrap' }}>{receiptSettings.footer}</div>
        </div>

      </div>

      {/* Modal Pengaturan Struk */}
      {settingsModalOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 999,
            background: 'rgba(0, 0, 0, 0.45)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '1rem',
            fontFamily: 'sans-serif'
          }}
        >
          <div
            style={{
              background: 'white',
              borderRadius: '16px',
              width: '100%',
              maxWidth: '650px',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
              display: 'flex',
              flexDirection: 'column',
              gap: '16px',
              position: 'relative'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #F1F5F9', paddingBottom: '12px' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 800, color: '#1E293B' }}>Pengaturan Desain Struk</h3>
                <p style={{ margin: '4px 0 0', fontSize: '0.78rem', color: '#64748B', fontWeight: 500 }}>Kustomisasi informasi struk belanja POS Laundry</p>
              </div>
              <button
                type="button"
                onClick={() => setSettingsModalOpen(false)}
                style={{
                  border: 'none',
                  background: 'transparent',
                  fontSize: '1.5rem',
                  color: '#94A3B8',
                  cursor: 'pointer',
                  fontWeight: 'bold'
                }}
              >
                &times;
              </button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
              {/* Left Column: Form inputs */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Nama Toko / Bisnis</label>
                  <input
                    type="text"
                    value={tempStoreName}
                    onChange={(e) => setTempStoreName(e.target.value)}
                    style={{
                      width: '100%',
                      boxSizing: 'border-box',
                      background: 'white',
                      border: '1px solid #CBD5E1',
                      borderRadius: '8px',
                      padding: '10px 12px',
                      fontSize: '0.85rem',
                      color: '#1E293B',
                      outline: 'none'
                    }}
                    placeholder="Contoh: KAPAS LAUNDRY"
                  />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Subheader / Alamat & Telp</label>
                  <textarea
                    value={tempSubheader}
                    onChange={(e) => setTempSubheader(e.target.value)}
                    rows={2}
                    style={{
                      width: '100%',
                      boxSizing: 'border-box',
                      background: 'white',
                      border: '1px solid #CBD5E1',
                      borderRadius: '8px',
                      padding: '10px 12px',
                      fontSize: '0.85rem',
                      color: '#1E293B',
                      outline: 'none',
                      resize: 'none'
                    }}
                    placeholder="Contoh: Ruko Buah Batu Regency Blok B-3\nTelp: 0822-4507-7959"
                  />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Kaki Struk (Footer)</label>
                  <textarea
                    value={tempFooter}
                    onChange={(e) => setTempFooter(e.target.value)}
                    rows={4}
                    style={{
                      width: '100%',
                      boxSizing: 'border-box',
                      background: 'white',
                      border: '1px solid #CBD5E1',
                      borderRadius: '8px',
                      padding: '10px 12px',
                      fontSize: '0.85rem',
                      color: '#1E293B',
                      outline: 'none',
                      resize: 'none'
                    }}
                    placeholder="Syarat & ketentuan..."
                  />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Ukuran Kertas Utama</label>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    {['58mm', '80mm'].map(size => (
                      <button
                        key={size}
                        type="button"
                        onClick={() => setTempPaperSize(size)}
                        style={{
                          flex: 1,
                          padding: '10px 12px',
                          background: tempPaperSize === size ? 'linear-gradient(135deg, #4F46E5, #3730A3)' : '#FFFFFF',
                          border: tempPaperSize === size ? 'none' : '1px solid #CBD5E1',
                          borderRadius: '8px',
                          color: tempPaperSize === size ? '#FFFFFF' : '#334155',
                          fontWeight: 700,
                          fontSize: '0.8rem',
                          cursor: 'pointer'
                        }}
                      >
                        Thermal {size}
                      </button>
                    ))}
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Cetak Uji Coba (Test Print)</span>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button
                      type="button"
                      onClick={() => printTestReceipt('58mm')}
                      style={{
                        flex: 1,
                        padding: '10px 12px',
                        background: '#FFFFFF',
                        border: '1px solid #CBD5E1',
                        borderRadius: '8px',
                        color: '#334155',
                        fontWeight: 700,
                        fontSize: '0.8rem',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '6px'
                      }}
                    >
                      <Printer size={14} /> Cetak 58mm
                    </button>
                    <button
                      type="button"
                      onClick={() => printTestReceipt('80mm')}
                      style={{
                        flex: 1,
                        padding: '10px 12px',
                        background: '#FFFFFF',
                        border: '1px solid #CBD5E1',
                        borderRadius: '8px',
                        color: '#334155',
                        fontWeight: 700,
                        fontSize: '0.8rem',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '6px'
                      }}
                    >
                      <Printer size={14} /> Cetak 80mm
                    </button>
                  </div>
                </div>
              </div>

              {/* Right Column: Live thermal receipt mockup preview */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Pratinjau Live (Preview)</span>
                <div
                  style={{
                    background: '#F8FAFC',
                    border: '1px dashed #CBD5E1',
                    borderRadius: '12px',
                    padding: '12px',
                    fontFamily: 'monospace',
                    fontSize: '10px',
                    color: '#0F172A',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'stretch',
                    lineHeight: '1.4',
                    maxHeight: '260px',
                    overflowY: 'auto'
                  }}
                >
                  <div style={{ textAlign: 'center', fontWeight: 'bold', fontSize: '11px', marginBottom: '2px', wordBreak: 'break-word' }}>
                    {tempStoreName || 'NAMA TOKO'}
                  </div>
                  <div style={{ textAlign: 'center', marginBottom: '6px', opacity: 0.8, wordBreak: 'break-word', fontSize: '9px', whiteSpace: 'pre-wrap' }}>
                    {tempSubheader || 'Alamat & Kontak'}
                  </div>
                  <div style={{ borderTop: '1px dashed #94A3B8', margin: '4px 0' }}></div>
                  
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span>No. Nota: N-PREVIEW-999</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span>Tanggal : {new Date().toLocaleDateString('id-ID')}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                    <span>Pelanggan: Pelanggan Demo</span>
                  </div>
                  <div style={{ borderTop: '1px dashed #94A3B8', margin: '4px 0' }}></div>
                  
                  <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>Rincian Cucian:</div>
                  <div style={{ padding: '4px', border: '1px dashed #94A3B8', marginBottom: '6px', whiteSpace: 'pre-wrap' }}>
                    Cuci Komplit Reguler - 5 Kg @ Rp7.000
                  </div>
                  
                  <div style={{ borderTop: '1px dashed #94A3B8', margin: '4px 0' }}></div>
                  
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold' }}>
                    <span>TOTAL TAGIHAN</span>
                    <span>Rp 35.000</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span>STATUS BAYAR</span>
                    <span>LUNAS</span>
                  </div>
                  <div style={{ borderTop: '1px dashed #94A3B8', margin: '4px 0' }}></div>
                  
                  <div style={{ textAlign: 'center', marginTop: '6px', whiteSpace: 'pre-wrap', wordBreak: 'break-word', opacity: 0.9, fontSize: '9px' }}>
                    {tempFooter || 'Terima Kasih!'}
                  </div>
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', borderTop: '1px solid #F1F5F9', paddingTop: '16px', marginTop: '4px' }}>
              <button
                type="button"
                onClick={() => setSettingsModalOpen(false)}
                style={{
                  padding: '8px 16px',
                  background: '#F1F5F9',
                  border: 'none',
                  borderRadius: '8px',
                  color: '#475569',
                  fontWeight: 700,
                  fontSize: '0.85rem',
                  cursor: 'pointer'
                }}
              >
                Batal
              </button>
              <button
                type="button"
                onClick={() => {
                  const newSettings = {
                    storeName: tempStoreName,
                    subheader: tempSubheader,
                    footer: tempFooter,
                    paperSize: tempPaperSize
                  };
                  localStorage.setItem('posbah_receipt_settings_laundry', JSON.stringify(newSettings));
                  setReceiptSettings(newSettings);
                  setSettingsModalOpen(false);
                }}
                style={{
                  padding: '8px 20px',
                  background: 'linear-gradient(135deg, #4F46E5, #3730A3)',
                  border: 'none',
                  borderRadius: '8px',
                  color: '#FFFFFF',
                  fontWeight: 700,
                  fontSize: '0.85rem',
                  cursor: 'pointer',
                  boxShadow: '0 4px 12px rgba(79, 70, 229, 0.15)'
                }}
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
