import React, { useEffect, useState } from 'react';
import api from '../../services/apiBmp';
import { Cpu, Package, Settings as SettingsIcon, RotateCw, Plus, Trash2, RefreshCw } from 'lucide-react';

const HppCalculator = () => {
  const [dbProducts, setDbProducts] = useState([]);
  const [settings, setSettings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const [selectedAddProduct, setSelectedAddProduct] = useState('');

  // Global simulator parameters
  const [sim, setSim] = useState({
    listrik: 0, // Listrik per 1 mesin (Rp/Bulan)
    gaji: 0,
    harga_bahan: 0,
    isi_karung: 50
  });

  // Active calculator rows
  const [rows, setRows] = useState([]);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await api.get('/hpp-calculator');
      const responseData = res.data.data;
      if (responseData) {
        setSettings(responseData.settings);
        
        // Default Listrik per 1 mesin = Total Listrik / Jumlah Mesin
        const numMachines = responseData.settings?.JumlahMesin || 5;
        const defaultListrikPerMesin = (responseData.settings?.ListrikBulanan || 30000000) / numMachines;
        const defaultGaji = responseData.settings?.GajiHarian || 80000;
        const defaultHargaBahan = responseData.default_harga_bahan || 8000;
        
        setSim({
          listrik: defaultListrikPerMesin,
          gaji: defaultGaji,
          harga_bahan: defaultHargaBahan,
          isi_karung: 50
        });

        const rawProducts = responseData.hpp_results?.map(r => r.product) || [];
        setDbProducts(rawProducts);

        // Initialize active rows with database products
        const initialRows = responseData.hpp_results?.map(r => ({
          id: `db-${r.product.ID}`,
          dbId: r.product.ID,
          title: r.product.Title,
          cycleTime: r.product.CycleTime || 0,
          beratGram: r.product.BeratGram || 0,
          cavity: r.product.Cavity || 1,
          rejectRate: r.product.RejectRate || 0,
          hargaJual: r.harga_jual_real || r.product.Price || 0,
          isCustom: false
        })) || [];

        setRows(initialRows);
      }
    } catch (err) {
      console.error("Gagal mengambil data kalkulator HPP:", err);
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleReset = () => {
    fetchData();
  };

  const handleAddProduct = (productId) => {
    if (!productId) return;
    const prod = dbProducts.find(p => p.ID === Number(productId));
    if (!prod) return;

    const newRow = {
      id: `db-${prod.ID}-${Date.now()}`,
      dbId: prod.ID,
      title: prod.Title,
      cycleTime: prod.CycleTime || 0,
      beratGram: prod.BeratGram || 0,
      cavity: prod.Cavity || 1,
      rejectRate: prod.RejectRate || 0,
      hargaJual: prod.Price || 0,
      isCustom: false
    };

    setRows([...rows, newRow]);
    setSelectedAddProduct('');
  };

  const handleAddCustomProduct = () => {
    const newRow = {
      id: `custom-${Date.now()}`,
      dbId: null,
      title: 'Produk Kustom Baru',
      cycleTime: 15,
      beratGram: 50,
      cavity: 1,
      rejectRate: 2,
      hargaJual: 15000,
      isCustom: true
    };
    setRows([...rows, newRow]);
  };

  const handleRemoveRow = (rowId) => {
    setRows(rows.filter(r => r.id !== rowId));
  };

  const handleEditCell = (rowId, field, value) => {
    setRows(rows.map(r => {
      if (r.id === rowId) {
        return {
          ...r,
          [field]: field === 'title' ? value : Number(value)
        };
      }
      return r;
    }));
  };

  // Calculations
  const hariKerja = settings?.HariKerjaSebulan || 26;
  const hoursPerDay = settings?.HoursPerDay || 24;
  const totalJamBulan = hariKerja * hoursPerDay;

  // Listrik Jam per Mesin (direct hourly cost for 1 machine based on manual input)
  const listrikJamMesin = totalJamBulan > 0 ? (sim.listrik / totalJamBulan) : 0;

  // Gaji Jam per Mesin
  const jumlahKaryawan = settings?.JumlahKaryawan || 19;
  const jumlahMesin = settings?.JumlahMesin || 5;
  const gajiJamMesin = totalJamBulan > 0
    ? (((jumlahKaryawan * sim.gaji * hariKerja) / jumlahMesin) / totalJamBulan)
    : 0;

  const overheadJam = listrikJamMesin + gajiJamMesin;

  const packingLsn = sim.isi_karung > 0
    ? ((settings?.BiayaKarungPer1000 || 2100000) / 1000) / sim.isi_karung
    : 0;

  const calculateRowData = (r) => {
    const cycleTime = r.cycleTime || 0;
    const cavity = r.cavity || 1;
    const beratGram = r.beratGram || 0;
    const rejectRate = r.rejectRate || 0;
    const hargaJual = r.hargaJual || 0;

    const outputPerJam = cycleTime > 0 ? (((3600 / cycleTime) * cavity) / 12) : 0;
    const modalBahan = ((beratGram * 12) / 1000) * sim.harga_bahan * (1 + (rejectRate / 100));
    const modalMesin = outputPerJam > 0 ? (overheadJam / outputPerJam) : 0;
    const totalHpp = modalBahan + modalMesin + packingLsn;
    const margin = hargaJual - totalHpp;
    const marginPercent = totalHpp > 0 ? (margin / totalHpp) * 100 : 0;

    return {
      outputPerJam,
      modalBahan,
      modalMesin,
      totalHpp,
      margin,
      marginPercent
    };
  };

  const formatRp = (num) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(num);

  if (loading && !settings) return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }}>
      <div style={{ width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
      <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
      <div style={{ color: '#6c757d' }}>Memuat kalkulator HPP...</div>
    </div>
  );

  return (
    <div style={{ padding: isMobile ? '15px' : '30px', background: '#f8f9fa', minHeight: '100%', maxWidth: '100vw', boxSizing: 'border-box', overflowX: 'hidden', fontFamily: "'Inter', sans-serif" }}>
      {/* Header */}
      <div style={{ marginBottom: '25px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '15px' }}>
        <div>
          <h2 style={{ fontSize: isMobile ? '22px' : '28px', fontWeight: '800', margin: 0, color: '#1e293b', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Cpu size={isMobile ? 24 : 32} color="#0d6efd" /> Kalkulator HPP
          </h2>
          <p style={{ margin: '5px 0 0 0', color: '#64748b', fontSize: '14px' }}>Analisis modal dan margin keuntungan per produk secara fleksibel & interaktif</p>
        </div>
        <button onClick={handleReset} style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 16px', background: 'white', border: '1px solid #e2e8f0', borderRadius: '12px', color: '#64748b', fontSize: '14px', fontWeight: '600', cursor: 'pointer', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', transition: 'all 0.2s' }}>
          <RefreshCw size={16} /> Reset Default
        </button>
      </div>

      <div style={{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: '25px' }}>
        {/* Sidebar Simulator */}
        <div style={{ flex: '1', minWidth: isMobile ? '100%' : '320px', maxWidth: isMobile ? '100%' : '350px' }}>
          <div style={{ background: 'white', padding: '25px', borderRadius: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)', border: '1px solid #f1f5f9', position: 'sticky', top: '20px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: '800', marginBottom: '20px', color: '#1e293b', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <SettingsIcon size={18} color="#64748b" /> Parameter Pabrik & Bahan
            </h3>

            <div style={{ marginBottom: '15px' }}>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }}>Listrik 1 Mesin (Rp/Bulan)</label>
              <input type="number" value={sim.listrik || ''} onChange={e => setSim({...sim, listrik: Number(e.target.value)})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px', boxSizing: 'border-box' }} placeholder="Nominal listrik 1 mesin..." />
            </div>
            <div style={{ marginBottom: '15px' }}>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }}>Gaji Harian Karyawan (Rp/Hari)</label>
              <input type="number" value={sim.gaji || ''} onChange={e => setSim({...sim, gaji: Number(e.target.value)})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px', boxSizing: 'border-box' }} placeholder="Gaji harian..." />
            </div>
            <div style={{ marginBottom: '15px' }}>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }}>Harga Bahan Baku (Rp/Kg)</label>
              <input type="number" value={sim.harga_bahan || ''} onChange={e => setSim({...sim, harga_bahan: Number(e.target.value)})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px', boxSizing: 'border-box' }} placeholder="Harga bahan..." />
            </div>
            <div style={{ marginBottom: '25px' }}>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }}>Isi per Karung (Lusin)</label>
              <input type="number" value={sim.isi_karung || ''} onChange={e => setSim({...sim, isi_karung: Number(e.target.value)})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px', boxSizing: 'border-box' }} placeholder="Isi per karung..." />
            </div>

            <div style={{ background: '#1e293b', color: 'white', padding: '15px', borderRadius: '16px', fontSize: '13px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                <span style={{ opacity: 0.7 }}>Overhead Pabrik / Jam</span>
                <span style={{ fontWeight: '700' }}>{formatRp(overheadJam)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                <span style={{ opacity: 0.7 }}>Biaya Karung / Lusin</span>
                <span style={{ fontWeight: '700' }}>{formatRp(packingLsn)}</span>
              </div>
              <div style={{ borderTop: '1px solid rgba(255,255,255,0.1)', margin: '8px 0', paddingTop: '8px' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px', fontSize: '11px', opacity: 0.8 }}>
                <span>Hari Kerja / Bulan</span>
                <span>{hariKerja} Hari</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px', fontSize: '11px', opacity: 0.8 }}>
                <span>Jam Kerja / Hari</span>
                <span>{hoursPerDay} Jam</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', opacity: 0.8 }}>
                <span>Mesin (Untuk Distribusi Gaji)</span>
                <span>{jumlahMesin} Unit</span>
              </div>
            </div>
          </div>
        </div>

        {/* Main Results */}
        <div style={{ flex: '1', display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Action Bar */}
          <div style={{ background: 'white', padding: '16px 20px', borderRadius: '20px', boxShadow: '0 4px 15px rgba(0,0,0,0.03)', border: '1px solid #f1f5f9', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '15px', flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: 1, minWidth: '250px' }}>
              <select value={selectedAddProduct} onChange={e => setSelectedAddProduct(e.target.value)} style={{ flex: 1, padding: '10px', borderRadius: '10px', border: '1px solid #e2e8f0', fontSize: '14px', outline: 'none', background: 'white' }}>
                <option value="">-- Pilih Produk Database --</option>
                {dbProducts.map(p => (
                  <option key={p.ID} value={p.ID}>{p.Title}</option>
                ))}
              </select>
              <button onClick={() => handleAddProduct(selectedAddProduct)} disabled={!selectedAddProduct} style={{ padding: '10px 16px', background: selectedAddProduct ? '#0d6efd' : '#e2e8f0', color: selectedAddProduct ? 'white' : '#94a3b8', border: 'none', borderRadius: '10px', cursor: selectedAddProduct ? 'pointer' : 'not-allowed', fontWeight: 'bold', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Plus size={16} /> Tambah
              </button>
            </div>
            <button onClick={handleAddCustomProduct} style={{ padding: '10px 16px', background: '#10b981', color: 'white', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: 'bold', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '6px', boxShadow: '0 2px 5px rgba(16,185,129,0.2)' }}>
              <Plus size={16} /> Tambah Produk Kustom
            </button>
          </div>

          {/* List View / Table */}
          {isMobile ? (
            /* Mobile Card List */
            <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
              {rows.map((r) => {
                const calc = calculateRowData(r);
                return (
                  <div key={r.id} style={{ background: 'white', borderRadius: '20px', padding: '20px', boxShadow: '0 4px 15px rgba(0,0,0,0.03)', border: '1px solid #f1f5f9' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px', borderBottom: '1px solid #f1f5f9', paddingBottom: '10px' }}>
                      <input type="text" value={r.title} onChange={e => handleEditCell(r.id, 'title', e.target.value)} style={{ fontWeight: '800', fontSize: '16px', color: '#1e293b', border: 'none', borderBottom: '1px dashed #cbd5e1', outline: 'none', width: '70%', background: 'transparent' }} />
                      <button onClick={() => handleRemoveRow(r.id)} style={{ border: 'none', background: '#fee2e2', color: '#ef4444', width: '32px', height: '32px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
                        <Trash2 size={16} />
                      </button>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '15px' }}>
                      <div>
                        <label style={{ display: 'block', fontSize: '10px', color: '#64748b', fontWeight: '700', marginBottom: '4px' }}>Cycle Time (dtk)</label>
                        <input type="number" value={r.cycleTime} onChange={e => handleEditCell(r.id, 'cycleTime', e.target.value)} style={{ padding: '8px', border: '1px solid #e2e8f0', borderRadius: '8px', fontSize: '13px', width: '100%', boxSizing: 'border-box' }} />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '10px', color: '#64748b', fontWeight: '700', marginBottom: '4px' }}>Berat (gr)</label>
                        <input type="number" value={r.beratGram} onChange={e => handleEditCell(r.id, 'beratGram', e.target.value)} style={{ padding: '8px', border: '1px solid #e2e8f0', borderRadius: '8px', fontSize: '13px', width: '100%', boxSizing: 'border-box' }} />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '10px', color: '#64748b', fontWeight: '700', marginBottom: '4px' }}>Cavity</label>
                        <input type="number" value={r.cavity} onChange={e => handleEditCell(r.id, 'cavity', e.target.value)} style={{ padding: '8px', border: '1px solid #e2e8f0', borderRadius: '8px', fontSize: '13px', width: '100%', boxSizing: 'border-box' }} />
                      </div>
                      <div>
                        <label style={{ display: 'block', fontSize: '10px', color: '#64748b', fontWeight: '700', marginBottom: '4px' }}>Reject Rate (%)</label>
                        <input type="number" value={r.rejectRate} onChange={e => handleEditCell(r.id, 'rejectRate', e.target.value)} style={{ padding: '8px', border: '1px solid #e2e8f0', borderRadius: '8px', fontSize: '13px', width: '100%', boxSizing: 'border-box' }} />
                      </div>
                      <div style={{ gridColumn: 'span 2' }}>
                        <label style={{ display: 'block', fontSize: '10px', color: '#64748b', fontWeight: '700', marginBottom: '4px' }}>Harga Jual Real (Rp/Lsn)</label>
                        <input type="number" value={r.hargaJual} onChange={e => handleEditCell(r.id, 'hargaJual', e.target.value)} style={{ padding: '8px', border: '1px solid #e2e8f0', borderRadius: '8px', fontSize: '13px', width: '100%', boxSizing: 'border-box', fontWeight: 'bold', color: '#0d6efd' }} />
                      </div>
                    </div>

                    <div style={{ background: '#f8fafc', padding: '12px', borderRadius: '12px', fontSize: '12px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ color: '#64748b' }}>Output / Jam:</span>
                        <span style={{ fontWeight: '600' }}>{calc.outputPerJam.toFixed(1)} Lsn</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ color: '#64748b' }}>Modal Bahan / Lsn:</span>
                        <span style={{ fontWeight: '600' }}>{formatRp(calc.modalBahan)}</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ color: '#64748b' }}>Modal Mesin / Lsn:</span>
                        <span style={{ fontWeight: '600' }}>{formatRp(calc.modalMesin)}</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid #e2e8f0', paddingTop: '6px', marginTop: '4px', fontWeight: 'bold' }}>
                        <span>Total HPP:</span>
                        <span style={{ color: '#1e293b' }}>{formatRp(calc.totalHpp)}</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold' }}>
                        <span>Margin:</span>
                        <span style={{ color: calc.margin > 0 ? '#10b981' : '#ef4444' }}>
                          {formatRp(calc.margin)} ({calc.marginPercent.toFixed(1)}%)
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            /* Desktop Table View */
            <div style={{ background: 'white', borderRadius: '24px', padding: '20px', boxShadow: '0 4px 20px rgba(0,0,0,0.03)', border: '1px solid #f1f5f9', overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: '850px' }}>
                <thead>
                  <tr style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.5px', borderBottom: '2px solid #f1f5f9' }}>
                    <th style={{ padding: '12px 10px', fontWeight: '700' }}>Nama Produk</th>
                    <th style={{ padding: '12px 10px', fontWeight: '700', width: '80px' }}>Cycle (dtk)</th>
                    <th style={{ padding: '12px 10px', fontWeight: '700', width: '80px' }}>Berat (gr)</th>
                    <th style={{ padding: '12px 10px', fontWeight: '700', width: '70px' }}>Cavity</th>
                    <th style={{ padding: '12px 10px', fontWeight: '700', width: '70px' }}>Reject (%)</th>
                    <th style={{ padding: '12px 10px', fontWeight: '700', width: '100px' }}>Out/Jam</th>
                    <th style={{ padding: '12px 10px', fontWeight: '700' }}>M. Bahan</th>
                    <th style={{ padding: '12px 10px', fontWeight: '700' }}>M. Mesin</th>
                    <th style={{ padding: '12px 10px', fontWeight: '700' }}>Total HPP</th>
                    <th style={{ padding: '12px 10px', fontWeight: '700', width: '110px' }}>Harga Jual</th>
                    <th style={{ padding: '12px 10px', fontWeight: '700' }}>Margin</th>
                    <th style={{ padding: '12px 10px', fontWeight: '700', width: '50px' }}>Aksi</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => {
                    const calc = calculateRowData(r);
                    return (
                      <tr key={r.id} style={{ borderBottom: '1px solid #f1f5f9', transition: 'all 0.15s' }}>
                        <td style={{ padding: '12px 10px', fontWeight: '700' }}>
                          <input type="text" value={r.title} onChange={e => handleEditCell(r.id, 'title', e.target.value)} style={{ padding: '6px 8px', border: '1px solid transparent', background: 'transparent', borderRadius: '6px', fontSize: '13px', width: '100%', boxSizing: 'border-box', fontWeight: '700', outline: 'none' }} onFocus={e => e.target.style.border = '1px solid #e2e8f0'} onBlur={e => e.target.style.border = '1px solid transparent'} />
                        </td>
                        <td style={{ padding: '12px 10px' }}>
                          <input type="number" value={r.cycleTime} onChange={e => handleEditCell(r.id, 'cycleTime', e.target.value)} style={{ padding: '6px 8px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '13px', width: '100%', boxSizing: 'border-box', outline: 'none' }} />
                        </td>
                        <td style={{ padding: '12px 10px' }}>
                          <input type="number" value={r.beratGram} onChange={e => handleEditCell(r.id, 'beratGram', e.target.value)} style={{ padding: '6px 8px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '13px', width: '100%', boxSizing: 'border-box', outline: 'none' }} />
                        </td>
                        <td style={{ padding: '12px 10px' }}>
                          <input type="number" value={r.cavity} onChange={e => handleEditCell(r.id, 'cavity', e.target.value)} style={{ padding: '6px 8px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '13px', width: '100%', boxSizing: 'border-box', outline: 'none' }} />
                        </td>
                        <td style={{ padding: '12px 10px' }}>
                          <input type="number" value={r.rejectRate} onChange={e => handleEditCell(r.id, 'rejectRate', e.target.value)} style={{ padding: '6px 8px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '13px', width: '100%', boxSizing: 'border-box', outline: 'none' }} />
                        </td>
                        <td style={{ padding: '12px 10px', fontSize: '13px', color: '#475569', fontWeight: '600' }}>
                          {calc.outputPerJam.toFixed(1)} Lsn
                        </td>
                        <td style={{ padding: '12px 10px', fontSize: '13px', color: '#1e293b' }}>
                          {formatRp(calc.modalBahan)}
                        </td>
                        <td style={{ padding: '12px 10px', fontSize: '13px', color: '#1e293b' }}>
                          {formatRp(calc.modalMesin)}
                        </td>
                        <td style={{ padding: '12px 10px', fontWeight: '800', background: '#fef9c3', fontSize: '13px' }}>
                          {formatRp(calc.totalHpp)}
                        </td>
                        <td style={{ padding: '12px 10px' }}>
                          <input type="number" value={r.hargaJual} onChange={e => handleEditCell(r.id, 'hargaJual', e.target.value)} style={{ padding: '6px 8px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '13px', width: '100%', boxSizing: 'border-box', fontWeight: '800', color: '#0d6efd', outline: 'none' }} />
                        </td>
                        <td style={{ padding: '12px 10px', fontWeight: '800', color: calc.margin > 0 ? '#10b981' : '#ef4444', fontSize: '13px' }}>
                          <div>{formatRp(calc.margin)}</div>
                          <div style={{ fontSize: '10px', opacity: 0.8, fontWeight: 'normal' }}>{calc.marginPercent.toFixed(1)}%</div>
                        </td>
                        <td style={{ padding: '12px 10px' }}>
                          <button onClick={() => handleRemoveRow(r.id)} style={{ border: 'none', background: 'transparent', color: '#94a3b8', cursor: 'pointer', padding: '6px', borderRadius: '6px', transition: 'all 0.2s' }} onMouseEnter={e => e.currentTarget.style.color = '#ef4444'} onMouseLeave={e => e.currentTarget.style.color = '#94a3b8'}>
                            <Trash2 size={16} />
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {rows.length === 0 && !loading && (
            <div style={{ textAlign: 'center', padding: '50px', background: 'white', borderRadius: '24px', color: '#64748b', border: '1px solid #f1f5f9', boxShadow: '0 4px 15px rgba(0,0,0,0.02)' }}>
              <Package size={48} style={{ margin: '0 auto 15px', opacity: 0.2 }} />
              <div>Belum ada produk di kalkulator. Silakan tambah produk di atas.</div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default HppCalculator;
