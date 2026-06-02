import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { Cpu, Package, Settings as SettingsIcon, RotateCw } from 'lucide-react';

const HppCalculator: React.FC = () => {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

  const [sim, setSim] = useState({
    listrik: 0, gaji: 0, harga_bahan: 0, isi_karung: 50
  });

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const fetchData = async (params: any = {}) => {
    setLoading(true);
    try {
      const res = await api.get('/hpp-calculator', { params });
      const responseData = res.data.data;
      setData(responseData);
      
      // Interconnect & sync UI state from settings defaults on initial load
      if (Object.keys(params).length === 0 && responseData) {
        setSim({
          listrik: responseData.settings?.ListrikBulanan || 0,
          gaji: responseData.settings?.GajiHarian || 0,
          harga_bahan: responseData.default_harga_bahan || 8000,
          isi_karung: 50
        });
      }
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  };

  useEffect(() => { fetchData(); }, []);

  const handleSimulate = (e: React.FormEvent) => {
    e.preventDefault();
    fetchData(sim);
  };

  const formatRp = (num: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(num);

  if (loading && !data) return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }}>
      <div style={{ width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
      <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
      <div style={{ color: '#6c757d' }}>Menganalisis HPP...</div>
    </div>
  );

  return (
    <div style={{ padding: isMobile ? '15px' : '30px', background: '#f8f9fa', minHeight: '100%', maxWidth: '100vw', boxSizing: 'border-box', overflowX: 'hidden' }}>
      {/* Header */}
      <div style={{ marginBottom: '25px' }}>
        <h2 style={{ fontSize: isMobile ? '22px' : '28px', fontWeight: '800', margin: 0, color: '#1e293b', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Cpu size={isMobile ? 24 : 32} color="#0d6efd" /> Kalkulator HPP
        </h2>
        <p style={{ margin: '5px 0 0 0', color: '#64748b', fontSize: '14px' }}>Analisis modal dan margin keuntungan per produk</p>
      </div>

      <div style={{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: '25px' }}>
        {/* Sidebar Simulator */}
        <div style={{ flex: '1', minWidth: isMobile ? '100%' : '320px', maxWidth: isMobile ? '100%' : '350px' }}>
          <form onSubmit={handleSimulate} style={{ background: 'white', padding: '25px', borderRadius: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)', border: '1px solid #f1f5f9' }}>
            <h3 style={{ fontSize: '16px', fontWeight: '800', marginBottom: '20px', color: '#1e293b', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <SettingsIcon size={18} color="#64748b" /> Simulasi Parameter
            </h3>
            
            <div style={{ marginBottom: '15px' }}>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }}>Listrik (Rp/Bulan)</label>
              <input type="number" value={sim.listrik || ''} onChange={e => setSim({...sim, listrik: Number(e.target.value)})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px' }} placeholder="Masukkan listrik..." />
            </div>
            <div style={{ marginBottom: '15px' }}>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }}>Gaji Harian (Rp/Hari)</label>
              <input type="number" value={sim.gaji || ''} onChange={e => setSim({...sim, gaji: Number(e.target.value)})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px' }} placeholder="Masukkan gaji..." />
            </div>
            <div style={{ marginBottom: '15px' }}>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }}>Harga Bahan (Rp/Kg)</label>
              <input type="number" value={sim.harga_bahan || ''} onChange={e => setSim({...sim, harga_bahan: Number(e.target.value)})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px' }} placeholder="Masukkan harga bahan..." />
            </div>
            <div style={{ marginBottom: '25px' }}>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }}>Isi per Karung (Lusin)</label>
              <input type="number" value={sim.isi_karung} onChange={e => setSim({...sim, isi_karung: Number(e.target.value)})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px' }} />
            </div>
            
            <button type="submit" style={{ width: '100%', padding: '14px', background: '#0d6efd', color: 'white', border: 'none', borderRadius: '12px', cursor: 'pointer', fontWeight: 'bold', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', boxShadow: '0 4px 10px rgba(13, 110, 253, 0.2)' }}>
                <RotateCw size={18} /> Hitung Ulang
            </button>
          </form>

          {data && (
            <div style={{ background: '#1e293b', color: 'white', padding: '20px', borderRadius: '24px', marginTop: '20px', boxShadow: '0 4px 15px rgba(0,0,0,0.1)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px', fontSize: '13px' }}>
                  <span style={{ opacity: 0.7 }}>Overhead / Jam</span>
                  <span style={{ fontWeight: '700' }}>{formatRp(data.overhead_jam)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', marginBottom: '10px' }}>
                  <span style={{ opacity: 0.7 }}>Biaya Packing / Lsn</span>
                  <span style={{ fontWeight: '700' }}>{formatRp(data.packing_lsn)}</span>
              </div>
              <div style={{ borderTop: '1px solid rgba(255,255,255,0.1)', marginTop: '10px', paddingTop: '10px' }}></div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px', fontSize: '12px', opacity: 0.8 }}>
                  <span>Jam Operasional</span>
                  <span>{data.settings?.HoursPerDay || 24} Jam/Hari</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px', fontSize: '12px', opacity: 0.8 }}>
                  <span>Jumlah Mesin</span>
                  <span>{data.settings?.JumlahMesin || 0} Unit</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px', fontSize: '12px', opacity: 0.8 }}>
                  <span>Karyawan Produksi</span>
                  <span>{data.settings?.JumlahKaryawan || 0} Orang</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', opacity: 0.8 }}>
                  <span>Hari Kerja / Bulan</span>
                  <span>{data.settings?.HariKerjaSebulan || 26} Hari</span>
              </div>
            </div>
          )}
        </div>

        {/* Main Results */}
        <div style={{ flex: '1', display: 'flex', flexDirection: 'column', gap: '15px' }}>
          {isMobile ? (
             /* Mobile Cards */
             data?.hpp_results?.map((r: any, idx: number) => (
                <div key={idx} style={{ background: 'white', borderRadius: '20px', padding: '20px', boxShadow: '0 4px 6px rgba(0,0,0,0.02)', border: '1px solid #f1f5f9' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '10px' }}>
                        <h4 style={{ margin: 0, fontSize: '16px', fontWeight: '800', color: '#1e293b' }}>{r.product.Title}</h4>
                        <span style={{ background: '#ebf4ff', color: '#0d6efd', padding: '4px 10px', borderRadius: '8px', fontSize: '11px', fontWeight: '700' }}>{r.output_per_jam.toFixed(1)} Lsn/Jam</span>
                    </div>

                    <div style={{ display: 'flex', gap: '8px', marginBottom: '15px', flexWrap: 'wrap' }}>
                        <span style={{ background: '#f1f5f9', color: '#475569', padding: '2px 8px', borderRadius: '6px', fontSize: '11px' }}>
                          Cavity: {r.product.Cavity || 1}
                        </span>
                        {r.product.RejectRate > 0 && (
                          <span style={{ background: '#fee2e2', color: '#991b1b', padding: '2px 8px', borderRadius: '6px', fontSize: '11px' }}>
                            Reject: {r.product.RejectRate}%
                          </span>
                        )}
                        <span style={{ background: '#f1f5f9', color: '#475569', padding: '2px 8px', borderRadius: '6px', fontSize: '11px' }}>
                          Cycle: {r.product.CycleTime}s
                        </span>
                        <span style={{ background: '#f1f5f9', color: '#475569', padding: '2px 8px', borderRadius: '6px', fontSize: '11px' }}>
                          Berat: {r.product.BeratGram}g
                        </span>
                    </div>
                    
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '15px' }}>
                        <div style={{ background: '#f8fafc', padding: '10px', borderRadius: '12px' }}>
                            <small style={{ color: '#64748b', fontSize: '10px', display: 'block', marginBottom: '2px' }}>Modal Bahan</small>
                            <span style={{ fontWeight: '700', fontSize: '13px' }}>{formatRp(r.modal_bahan)}</span>
                        </div>
                        <div style={{ background: '#f8fafc', padding: '10px', borderRadius: '12px' }}>
                            <small style={{ color: '#64748b', fontSize: '10px', display: 'block', marginBottom: '2px' }}>Modal Mesin</small>
                            <span style={{ fontWeight: '700', fontSize: '13px' }}>{formatRp(r.modal_mesin)}</span>
                        </div>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid #f1f5f9', paddingTop: '15px' }}>
                        <div>
                            <small style={{ color: '#64748b', fontSize: '10px', display: 'block' }}>Total HPP</small>
                            <span style={{ fontWeight: '800', fontSize: '18px', color: '#1e293b' }}>{formatRp(r.total_hpp)}</span>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                            <small style={{ color: r.margin > 0 ? '#198754' : '#dc3545', fontSize: '10px', fontWeight: '800', display: 'block' }}>MARGIN</small>
                            <span style={{ fontWeight: '800', fontSize: '18px', color: r.margin > 0 ? '#198754' : '#dc3545' }}>{formatRp(r.margin)}</span>
                        </div>
                    </div>
                </div>
             ))
          ) : (
             /* Desktop Table */
             <div style={{ background: 'white', borderRadius: '24px', padding: '15px', boxShadow: '0 4px 12px rgba(0,0,0,0.03)', border: '1px solid #f1f5f9' }}>
                <table style={{ width: '100%', borderCollapse: 'separate', borderSpacing: '0 8px', textAlign: 'left' }}>
                  <thead>
                    <tr style={{ color: '#64748b', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '1px' }}>
                      <th style={{ padding: '0 20px', fontWeight: '700' }}>Produk</th>
                      <th style={{ padding: '0 20px', fontWeight: '700' }}>Output/Jam</th>
                      <th style={{ padding: '0 20px', fontWeight: '700' }}>M. Bahan</th>
                      <th style={{ padding: '0 20px', fontWeight: '700' }}>M. Mesin</th>
                      <th style={{ padding: '0 20px', fontWeight: '700' }}>Total HPP</th>
                      <th style={{ padding: '0 20px', fontWeight: '700' }}>Harga Jual</th>
                      <th style={{ padding: '0 20px', fontWeight: '700' }}>Margin</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data?.hpp_results?.map((r: any, idx: number) => (
                      <tr key={idx} style={{ background: '#ffffff' }}>
                        <td style={{ padding: '15px 20px', fontWeight: '700', borderBottom: '1px solid #f1f5f9' }}>
                          <div>{r.product.Title}</div>
                          <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 'normal', marginTop: '4px', display: 'flex', gap: '10px' }}>
                            <span>Berat: {r.product.BeratGram}g</span>
                            <span>Cycle: {r.product.CycleTime}s</span>
                            <span>Cavity: {r.product.Cavity || 1}</span>
                            {r.product.RejectRate > 0 && <span style={{ color: '#ef4444' }}>Reject: {r.product.RejectRate}%</span>}
                          </div>
                        </td>
                        <td style={{ padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }}>{r.output_per_jam.toFixed(1)} Lsn</td>
                        <td style={{ padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }}>{formatRp(r.modal_bahan)}</td>
                        <td style={{ padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }}>{formatRp(r.modal_mesin)}</td>
                        <td style={{ padding: '15px 20px', fontWeight: '800', background: '#fef9c3', borderBottom: '1px solid #f1f5f9' }}>{formatRp(r.total_hpp)}</td>
                        <td style={{ padding: '15px 20px', fontWeight: '800', color: '#0d6efd', borderBottom: '1px solid #f1f5f9' }}>{formatRp(r.harga_jual_real)}</td>
                        <td style={{ padding: '15px 20px', fontWeight: '800', color: r.margin > 0 ? '#198754' : '#dc3545', borderBottom: '1px solid #f1f5f9' }}>
                          {formatRp(r.margin)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
             </div>
          )}

          {(!data?.hpp_results || data.hpp_results.length === 0) && !loading && (
             <div style={{ textAlign: 'center', padding: '50px', background: 'white', borderRadius: '24px', color: '#64748b' }}>
                <Package size={48} style={{ margin: '0 auto 15px', opacity: 0.2 }} />
                <div>Data produk atau pengaturan tidak tersedia.</div>
             </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default HppCalculator;
