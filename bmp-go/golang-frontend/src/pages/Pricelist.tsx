import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { ArrowUp, ArrowDown, List, User, TrendingUp } from 'lucide-react';

const Pricelist: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [clients, setClients] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterClient, setFilterClient] = useState<number>(0);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const cliRes = await api.get('/clients');
      setClients(cliRes.data.data);

      const params = filterClient > 0 ? { client: filterClient } : {};
      const res = await api.get('/pricelist', { params });
      setData(res.data.data);
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  };

  useEffect(() => { fetchData(); }, [filterClient]);

  const formatRp = (num: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(num);

  if (loading && data.length === 0) return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }}>
      <div style={{ width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
      <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
      <div style={{ color: '#6c757d' }}>Memuat riwayat harga...</div>
    </div>
  );

  return (
    <div style={{ padding: isMobile ? '15px' : '30px', background: '#f8f9fa', minHeight: '100%', maxWidth: '100vw', boxSizing: 'border-box', overflowX: 'hidden' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', justifyContent: 'space-between', alignItems: isMobile ? 'stretch' : 'center', marginBottom: '25px', gap: '15px' }}>
        <div>
          <h2 style={{ fontSize: isMobile ? '22px' : '28px', fontWeight: '800', margin: 0, color: '#1e293b', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <List size={isMobile ? 24 : 32} color="#0d6efd" /> Pantau Harga
          </h2>
          <p style={{ margin: '5px 0 0 0', color: '#64748b', fontSize: '14px' }}>Histori perubahan harga jual per pelanggan</p>
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
          {/* Tombol Export PDF Produk */}
          <button
            onClick={() => {
              const token = localStorage.getItem('token');
              const apiUrl = import.meta.env.VITE_API_URL || 'https://bmp.up.railway.app/api';
              const absoluteApiUrl = apiUrl.startsWith('http') ? apiUrl : `${window.location.origin}${apiUrl}`;
              const pdfUrl = `${absoluteApiUrl}/pricelist/pdf?token=${token}`;

              if ((window as any).Capacitor) {
                window.open(pdfUrl, '_system');
              } else {
                window.open(pdfUrl, '_blank');
              }
            }}
            style={{
              display: 'flex', alignItems: 'center', gap: '8px',
              background: 'linear-gradient(135deg, #dc3545, #b02a37)',
              color: 'white', border: 'none', borderRadius: '12px',
              padding: '12px 18px', fontWeight: '700', fontSize: '14px',
              cursor: 'pointer', whiteSpace: 'nowrap',
              boxShadow: '0 4px 12px rgba(220,53,69,0.3)'
            }}
          >
            📄 Export PDF Produk
          </button>

          {/* Filter Pelanggan */}
          <div style={{ position: 'relative', minWidth: isMobile ? '100%' : '220px' }}>
            <User size={18} style={{ position: 'absolute', left: '12px', top: '12px', color: '#94a3b8', zIndex: 1 }} />
            <select
              value={filterClient}
              onChange={e => setFilterClient(Number(e.target.value))}
              style={{ width: '100%', padding: '12px 12px 12px 40px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', background: 'white', fontSize: '14px', appearance: 'none', cursor: 'pointer' }}
            >
              <option value={0}>Semua Pelanggan</option>
              {clients.map(c => <option key={c.ID} value={c.ID}>{c.ClientName}</option>)}
            </select>
          </div>
        </div>
      </div>

      {isMobile ? (
        /* Mobile Card View */
        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
          {data && data.map((d, idx) => (
            <div key={idx} style={{ background: 'white', borderRadius: '20px', padding: '20px', boxShadow: '0 4px 6px rgba(0,0,0,0.02)', border: '1px solid #f1f5f9' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                <div>
                  <h3 style={{ margin: 0, fontSize: '16px', fontWeight: '800', color: '#1e293b' }}>{d.item}</h3>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '5px', color: '#64748b', fontSize: '12px', marginTop: '4px' }}>
                    <User size={12}/> {d.client}
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '16px', fontWeight: '800', color: '#0d6efd' }}>{formatRp(d.terbaru.Harga)}</div>
                    <div style={{ fontSize: '10px', color: '#94a3b8' }}>{new Date(d.terbaru.Tanggal).toLocaleDateString('id-ID')}</div>
                </div>
              </div>

              <div style={{ background: '#f8fafc', borderRadius: '12px', padding: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ fontSize: '11px', color: '#64748b' }}>
                    Faktur: <span style={{ fontWeight: '700', color: '#1e293b' }}>{d.terbaru.Faktur}</span>
                </div>
                <div>
                    {d.status === 'NAIK' && <span style={{ color: '#dc3545', fontWeight: '800', fontSize: '11px', display: 'flex', alignItems: 'center', gap: '4px' }}><ArrowUp size={14}/> +{formatRp(d.selisih)}</span>}
                    {d.status === 'TURUN' && <span style={{ color: '#198754', fontWeight: '800', fontSize: '11px', display: 'flex', alignItems: 'center', gap: '4px' }}><ArrowDown size={14}/> -{formatRp(d.selisih)}</span>}
                    {d.status === 'TETAP' && <span style={{ color: '#64748b', fontWeight: '800', fontSize: '11px' }}>Stabil</span>}
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        /* Desktop Table View */
        <div style={{ background: 'white', borderRadius: '24px', padding: '15px', boxShadow: '0 4px 12px rgba(0,0,0,0.03)', border: '1px solid #f1f5f9' }}>
          <table style={{ width: '100%', borderCollapse: 'separate', borderSpacing: '0 8px', textAlign: 'left' }}>
            <thead>
              <tr style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '1px' }}>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Pelanggan</th>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Barang</th>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Harga Terbaru</th>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Faktur</th>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Harga Sebelumnya</th>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {data && data.map((d, idx) => (
                <tr key={idx} style={{ background: '#ffffff' }}>
                  <td style={{ padding: '15px 20px', fontWeight: '700', color: '#1e293b', borderBottom: '1px solid #f1f5f9' }}>{d.client}</td>
                  <td style={{ padding: '15px 20px', fontWeight: '600', color: '#334155', borderBottom: '1px solid #f1f5f9' }}>{d.item}</td>
                  <td style={{ padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }}>
                    <div style={{ fontWeight: '800', color: '#0d6efd' }}>{formatRp(d.terbaru.Harga)}</div>
                    <div style={{ fontSize: '10px', color: '#94a3b8' }}>{new Date(d.terbaru.Tanggal).toLocaleDateString('id-ID')}</div>
                  </td>
                  <td style={{ padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }}>
                    <span style={{ background: '#f1f5f9', padding: '4px 10px', borderRadius: '8px', fontSize: '12px', fontWeight: '700', color: '#475569' }}>
                      {d.terbaru.Faktur}
                    </span>
                  </td>
                  <td style={{ padding: '15px 20px', color: '#94a3b8', borderBottom: '1px solid #f1f5f9' }}>
                    {d.sebelumnya ? (
                      <>
                        <div style={{ fontWeight: '600', fontSize: '13px' }}>{formatRp(d.sebelumnya.Harga)}</div>
                        <div style={{ fontSize: '10px' }}>{new Date(d.sebelumnya.Tanggal).toLocaleDateString('id-ID')}</div>
                      </>
                    ) : '-'}
                  </td>
                  <td style={{ padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }}>
                    {d.status === 'NAIK' && <span style={{ color: '#dc3545', fontWeight: '800', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '13px' }}><ArrowUp size={16}/> {formatRp(d.selisih)}</span>}
                    {d.status === 'TURUN' && <span style={{ color: '#198754', fontWeight: '800', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '13px' }}><ArrowDown size={16}/> {formatRp(d.selisih)}</span>}
                    {d.status === 'TETAP' && <span style={{ color: '#64748b', fontWeight: '800', fontSize: '13px' }}>Tetap</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {(!data || data.length === 0) && !loading && (
        <div style={{ textAlign: 'center', padding: '50px', background: 'white', borderRadius: '24px', color: '#64748b' }}>
          <TrendingUp size={48} style={{ margin: '0 auto 15px', opacity: 0.2 }} />
          <div>Tidak ada riwayat harga ditemukan.</div>
        </div>
      )}
    </div>
  );
};

export default Pricelist;
