import React, { useState, useEffect } from 'react';
import { CreditCard, ArrowDownCircle, ArrowUpCircle, TrendingUp, Plus, Edit2, Trash2, Lock } from 'lucide-react';
import api from '../api';
import { useDemoBlock } from '../AuthContext';

/** Konversi Date ke string datetime-local sesuai timezone lokal browser */
const toLocalDatetime = (date = new Date()) => {
  const d = new Date(date);
  const offset = d.getTimezoneOffset() * 60000; // offset dalam ms
  return new Date(d.getTime() - offset).toISOString().slice(0, 16);
};

export default function Keuangan() {
  const { showDemoBlock, isDemo } = useDemoBlock();
  const [isPremium, setIsPremium] = useState(localStorage.getItem('posbah_premium') === 'true');

  const [activeTab, setActiveTab] = useState('REKAP');
  const [finances, setFinances] = useState([]);
  const [reports, setReports] = useState(null);
  const [products, setProducts] = useState([]);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ id: null, type: 'EXPENSE', amount: '', description: '', date: '', status: 'PENDING' });

  // REKAP, SALES, EXPENSE = bebas; PAYABLE, RECEIVABLE, MARGIN = premium
  const PREMIUM_TABS = ['PAYABLE', 'RECEIVABLE', 'MARGIN'];
  const isLockedTab = PREMIUM_TABS.includes(activeTab) && !isPremium;

  useEffect(() => {
    if (!isLockedTab) {
      if (activeTab !== 'REKAP' && activeTab !== 'MARGIN') {
        setFinances([]);
      }
      fetchData();
    }
  }, [activeTab, isPremium]);

  const fetchData = async () => {
    try {
      if (activeTab === 'REKAP') {
        const res = await api.get('/reports');
        setReports(res.data);
      } else if (activeTab === 'SALES') {
        const res = await api.get('/transactions');
        setFinances(res.data);
      } else if (activeTab === 'MARGIN') {
        const res = await api.get('/products');
        setProducts(res.data);
      } else {
        const res = await api.get('/finances');
        setFinances(res.data.filter(f => f.type === activeTab));
      }
    } catch (err) {
      console.error('Failed to fetch data', err);
    }
  };

  const handleOpenModal = (finance = null) => {
    if (finance) {
      setFormData({
        ...finance,
        date: toLocalDatetime(finance.date)
      });
    } else {
      setFormData({
        id: null,
        type: activeTab === 'REKAP' ? 'EXPENSE' : activeTab,
        amount: '',
        description: '',
        date: toLocalDatetime(), // waktu sekarang sesuai timezone lokal
        status: 'PENDING'
      });

    }
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (isDemo) { showDemoBlock('Menambah catatan keuangan hanya tersedia di akun berbayar.'); return; }

    try {
      if (formData.id) {
        await api.put(`/finances/${formData.id}`, formData);
      } else {
        await api.post('/finances', formData);
      }
      setIsModalOpen(false);
      fetchData();
    } catch (err) {
      console.error('Failed to save finance', err);
      alert('Gagal menyimpan data.');
    }
  };

  const handleDelete = async (id) => {
    if (isDemo) { showDemoBlock('Menghapus data keuangan hanya tersedia di akun berbayar.'); return; }
    if (window.confirm('Yakin ingin menghapus data ini?')) {

      try {
        await api.delete(`/finances/${id}`);
        fetchData();
      } catch (err) {
        console.error('Failed to delete', err);
      }
    }
  };

  const handleUpdateStatus = async (id, currentStatus) => {
    const newStatus = currentStatus === 'PENDING' ? 'PAID' : 'PENDING';
    try {
      await api.put(`/finances/${id}`, { status: newStatus });
      fetchData();
    } catch (err) {
      console.error('Failed to update status', err);
    }
  };

  const renderTabs = () => (
    <div className="flex gap-4 mb-6 border-b border-gray-200 pb-2" style={{ overflowX: 'auto', whiteSpace: 'nowrap', WebkitOverflowScrolling: 'touch' }}>
      <button
        className={`font-semibold pb-2 ${activeTab === 'REKAP' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
        onClick={() => setActiveTab('REKAP')}
      >
        Rekap Laporan
      </button>
      <button
        className={`font-semibold pb-2 ${activeTab === 'SALES' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
        onClick={() => setActiveTab('SALES')}
      >
        Riwayat Transaksi
      </button>
      <button
        className={`font-semibold pb-2 ${activeTab === 'EXPENSE' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
        onClick={() => setActiveTab('EXPENSE')}
      >
        Pengeluaran Usaha
      </button>
      <button
        className={`font-semibold pb-2 ${activeTab === 'MARGIN' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
        onClick={() => setActiveTab('MARGIN')}
      >
        Analisis Margin
      </button>
      <button
        className={`font-semibold pb-2 ${activeTab === 'PAYABLE' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
        onClick={() => setActiveTab('PAYABLE')}
      >
        Hutang
      </button>
      <button
        className={`font-semibold pb-2 ${activeTab === 'RECEIVABLE' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
        onClick={() => setActiveTab('RECEIVABLE')}
      >
        Piutang
      </button>
    </div>
  );

  const renderMargin = () => {
    const withCost = products.filter(p => p.costPrice > 0);

    // ── Weighted average margin (bobot = stok) ──────────────────
    const totalStockWithCost = withCost.reduce((s, p) => s + p.stock, 0);
    const avgMargin = totalStockWithCost > 0
      ? Math.round(
          withCost.reduce((s, p) => s + ((p.price - p.costPrice) / p.price) * 100 * p.stock, 0)
          / totalStockWithCost
        )
      : 0;

    const totalRevenuePotential = withCost.reduce((s, p) => s + p.price * p.stock, 0);
    const totalCostValue        = withCost.reduce((s, p) => s + p.costPrice * p.stock, 0);
    const totalPotentialProfit  = totalRevenuePotential - totalCostValue;

    const maxMargin = Math.max(...withCost.map(p => Math.round(((p.price - p.costPrice) / p.price) * 100)), 1);
    const lowMarginProducts = withCost.filter(p => ((p.price - p.costPrice) / p.price) * 100 < 10 && p.stock > 0);

    // warna kartu margin berdasarkan nilai
    const marginColor = avgMargin >= 25
      ? { border: '#10B981', text: '#065F46', bg: 'linear-gradient(135deg,#D1FAE5,#A7F3D0)' }
      : avgMargin >= 15
      ? { border: '#F59E0B', text: '#78350F', bg: 'linear-gradient(135deg,#FEF3C7,#FDE68A)' }
      : { border: '#EF4444', text: '#7F1D1D', bg: 'linear-gradient(135deg,#FEE2E2,#FECACA)' };

    const sortedProducts = [...withCost].sort((a, b) => {
      const mA = (a.price - a.costPrice) / a.price;
      const mB = (b.price - b.costPrice) / b.price;
      return mB - mA;
    });
    const allProducts = [
      ...sortedProducts,
      ...products.filter(p => p.costPrice <= 0)
    ];

    return (
      <div>
        {/* ── Warning bar produk margin rendah ─────────────────── */}
        {lowMarginProducts.length > 0 && (
          <div style={{
            display: 'flex', alignItems: 'flex-start', gap: 10,
            background: '#FFF7ED', border: '1px solid #FED7AA',
            borderRadius: 12, padding: '10px 16px', marginBottom: 20, fontSize: 13
          }}>
            <span style={{ fontSize: 18, flexShrink: 0 }}>⚠️</span>
            <div>
              <span style={{ fontWeight: 700, color: '#C2410C' }}>Margin Rendah (&lt;10%): </span>
              <span style={{ color: '#9A3412' }}>
                {lowMarginProducts.map(p => p.name).join(', ')}
              </span>
              <span style={{ color: '#EA580C' }}> — pertimbangkan kenaikan harga jual.</span>
            </div>
          </div>
        )}

        {/* ── Summary Cards ─────────────────────────────────────── */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: 14, marginBottom: 24 }}>

          {/* Rata-rata Margin (Weighted) */}
          <div style={{
            borderRadius: 16, padding: '18px 20px',
            background: marginColor.bg,
            border: `2px solid ${marginColor.border}`,
            display: 'flex', flexDirection: 'column', gap: 6
          }}>
            <div style={{ fontSize: 12, fontWeight: 700, color: marginColor.text, opacity: 0.8, textTransform: 'uppercase', letterSpacing: 1 }}>
              Rata-rata Margin ²
            </div>
            <div style={{ fontSize: 32, fontWeight: 900, color: marginColor.text, lineHeight: 1 }}>
              {avgMargin}%
            </div>
            <div style={{ fontSize: 11, color: marginColor.text, opacity: 0.7 }}>
              {avgMargin >= 25 ? '🟢 Sangat Baik' : avgMargin >= 15 ? '🟡 Cukup' : '🔴 Perlu Perhatian'}
            </div>
            <div style={{ fontSize: 10, color: marginColor.text, opacity: 0.6, marginTop: 2 }}>
              ² Tertimbang berdasarkan stok
            </div>
          </div>

          {/* Potensi Profit */}
          <div style={{ borderRadius: 16, padding: '18px 20px', background: 'linear-gradient(135deg,#EDE9FE,#DDD6FE)', border: '2px solid #7C3AED', display: 'flex', flexDirection: 'column', gap: 6 }}>
            <div style={{ fontSize: 12, fontWeight: 700, color: '#4C1D95', opacity: 0.8, textTransform: 'uppercase', letterSpacing: 1 }}>Potensi Profit Stok</div>
            <div style={{ fontSize: 22, fontWeight: 900, color: '#4C1D95', lineHeight: 1.1 }}>
              Rp {totalPotentialProfit.toLocaleString('id-ID')}
            </div>
            <div style={{ fontSize: 11, color: '#5B21B6', opacity: 0.7 }}>Jika semua stok terjual</div>
          </div>

          {/* Nilai Stok Jual */}
          <div style={{ borderRadius: 16, padding: '18px 20px', background: 'linear-gradient(135deg,#DBEAFE,#BFDBFE)', border: '2px solid #2563EB', display: 'flex', flexDirection: 'column', gap: 6 }}>
            <div style={{ fontSize: 12, fontWeight: 700, color: '#1E3A8A', opacity: 0.8, textTransform: 'uppercase', letterSpacing: 1 }}>Nilai Stok (Jual)</div>
            <div style={{ fontSize: 22, fontWeight: 900, color: '#1E3A8A', lineHeight: 1.1 }}>
              Rp {totalRevenuePotential.toLocaleString('id-ID')}
            </div>
            <div style={{ fontSize: 11, color: '#1D4ED8', opacity: 0.7 }}>Harga jual × stok</div>
          </div>

          {/* Nilai Stok Modal */}
          <div style={{ borderRadius: 16, padding: '18px 20px', background: 'linear-gradient(135deg,#FEF3C7,#FDE68A)', border: '2px solid #D97706', display: 'flex', flexDirection: 'column', gap: 6 }}>
            <div style={{ fontSize: 12, fontWeight: 700, color: '#78350F', opacity: 0.8, textTransform: 'uppercase', letterSpacing: 1 }}>Nilai Stok (Modal)</div>
            <div style={{ fontSize: 22, fontWeight: 900, color: '#78350F', lineHeight: 1.1 }}>
              Rp {totalCostValue.toLocaleString('id-ID')}
            </div>
            <div style={{ fontSize: 11, color: '#92400E', opacity: 0.7 }}>Harga modal × stok</div>
          </div>
        </div>

        {/* ── Tabel Produk ──────────────────────────────────────── */}
        <div className="glass-panel" style={{ overflow: 'hidden', borderRadius: 16 }}>
          <div style={{ padding: '14px 20px', borderBottom: '1px solid #F3F4F6', background: '#FAFAFA', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontWeight: 800, fontSize: 15, color: '#111827' }}>Detail Per Produk</span>
            <span style={{ fontSize: 12, color: '#9CA3AF' }}>Diurutkan: margin tertinggi → terendah</span>
          </div>
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table" style={{ minWidth: 680 }}>
              <thead>
                <tr>
                  <th>Nama Produk</th>
                  <th>Harga Jual</th>
                  <th>Harga Modal</th>
                  <th>Profit/item</th>
                  <th style={{ minWidth: 160 }}>Margin %</th>
                  <th>Stok</th>
                  <th>Potensi Profit</th>
                </tr>
              </thead>
              <tbody>
                {allProducts.length > 0 ? allProducts.map(p => {
                  const hasCost = p.costPrice > 0;
                  const margin = hasCost ? ((p.price - p.costPrice) / p.price) * 100 : null;
                  const marginRound = margin !== null ? Math.round(margin) : null;
                  const profitPerItem = hasCost ? p.price - p.costPrice : null;
                  const potentialProfit = profitPerItem !== null ? profitPerItem * p.stock : null;

                  const barColor = marginRound >= 20 ? '#10B981' : marginRound >= 10 ? '#F59E0B' : '#EF4444';
                  const barPct = marginRound !== null ? Math.min((marginRound / maxMargin) * 100, 100) : 0;

                  return (
                    <tr key={p.id} style={{ opacity: hasCost ? 1 : 0.5 }}>
                      <td className="font-semibold">{p.name}</td>
                      <td className="text-indigo-700 font-bold">Rp {p.price.toLocaleString('id-ID')}</td>
                      <td className="text-gray-500">
                        {hasCost ? `Rp ${p.costPrice.toLocaleString('id-ID')}` : <span style={{ color: '#D1D5DB', fontSize: 12 }}>Belum diisi</span>}
                      </td>
                      <td style={{ color: hasCost ? '#059669' : '#D1D5DB', fontWeight: 600 }}>
                        {profitPerItem !== null ? `Rp ${profitPerItem.toLocaleString('id-ID')}` : '—'}
                      </td>
                      <td>
                        {marginRound !== null ? (
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <div style={{ flex: 1, height: 6, background: '#F3F4F6', borderRadius: 99, overflow: 'hidden', minWidth: 60 }}>
                              <div style={{ width: `${barPct}%`, height: '100%', background: barColor, borderRadius: 99, transition: 'width 0.5s' }} />
                            </div>
                            <span style={{
                              background: marginRound >= 20 ? '#D1FAE5' : marginRound >= 10 ? '#FEF3C7' : '#FEE2E2',
                              color: marginRound >= 20 ? '#065F46' : marginRound >= 10 ? '#78350F' : '#7F1D1D',
                              padding: '2px 8px', borderRadius: 99, fontSize: 11, fontWeight: 800, whiteSpace: 'nowrap'
                            }}>{marginRound}%</span>
                          </div>
                        ) : <span style={{ color: '#D1D5DB', fontSize: 12 }}>—</span>}
                      </td>
                      <td>{p.stock} <span style={{ color: '#9CA3AF', fontSize: 12 }}>{p.unit}</span></td>
                      <td style={{ fontWeight: 700, color: potentialProfit > 0 ? '#059669' : '#D1D5DB' }}>
                        {potentialProfit !== null ? `Rp ${potentialProfit.toLocaleString('id-ID')}` : '—'}
                      </td>
                    </tr>
                  );
                }) : (
                  <tr><td colSpan="7" className="text-center p-4">Belum ada data produk.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    );
  };

  const renderRekap = () => {
    if (!reports) return <p>Loading...</p>;

    // Filter transaksi berdasarkan tanggal jika filter aktif
    const filterInfo = dateFrom || dateTo
      ? `Periode: ${dateFrom ? new Date(dateFrom).toLocaleDateString('id-ID') : '—'} s/d ${dateTo ? new Date(dateTo).toLocaleDateString('id-ID') : 'sekarang'}`
      : 'Semua waktu';

    return (
      <div>
        {/* Filter Tanggal */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20, flexWrap: 'wrap', background: '#F8FAFC', padding: '12px 16px', borderRadius: 12, border: '1px solid #E2E8F0' }}>
          <span style={{ fontWeight: 700, fontSize: 13, color: '#475569' }}>📅 Filter Periode:</span>
          <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} style={{ padding: '6px 10px', border: '1px solid #E2E8F0', borderRadius: 8, fontSize: 13 }} />
          <span style={{ color: '#94A3B8' }}>s/d</span>
          <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} style={{ padding: '6px 10px', border: '1px solid #E2E8F0', borderRadius: 8, fontSize: 13 }} />
          {(dateFrom || dateTo) && (
            <button onClick={() => { setDateFrom(''); setDateTo(''); }} style={{ padding: '6px 12px', background: '#FEE2E2', color: '#DC2626', border: 'none', borderRadius: 8, fontWeight: 700, cursor: 'pointer', fontSize: 12 }}>✕ Reset</button>
          )}
          <span style={{ fontSize: 12, color: '#64748B', marginLeft: 'auto' }}>{filterInfo}</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
          <div className="glass-panel p-6 flex items-center gap-4 border-l-4 border-blue-500">
            <TrendingUp size={40} className="text-blue-500" />
            <div>
              <div className="text-gray-500 text-sm font-semibold">Total Penjualan</div>
              <div className="text-2xl font-bold">Rp {reports.totalSales.toLocaleString('id-ID')}</div>
            </div>
          </div>
          <div className="glass-panel p-6 flex items-center gap-4 border-l-4 border-red-500">
            <ArrowDownCircle size={40} className="text-red-500" />
            <div>
              <div className="text-gray-500 text-sm font-semibold">Total Pengeluaran</div>
              <div className="text-2xl font-bold">Rp {reports.totalExpenses.toLocaleString('id-ID')}</div>
            </div>
          </div>
          <div className="glass-panel p-6 flex items-center gap-4 border-l-4 border-green-500">
            <CreditCard size={40} className="text-green-500" />
            <div>
              <div className="text-gray-500 text-sm font-semibold">Pendapatan Bersih (Net)</div>
              <div className="text-2xl font-bold">Rp {reports.netIncome.toLocaleString('id-ID')}</div>
            </div>
          </div>
          <div className="glass-panel p-6 flex items-center gap-4 border-l-4 border-yellow-500">
            <ArrowUpCircle size={40} className="text-yellow-500" />
            <div>
              <div className="text-gray-500 text-sm font-semibold">Piutang Tertunda</div>
              <div className="text-2xl font-bold">Rp {reports.pendingReceivables.toLocaleString('id-ID')}</div>
            </div>
          </div>
        </div>
      </div>
    );
  };

  const renderTable = () => (
    <div className="glass-panel table-container">
      <table className="data-table">
        <thead>
          <tr>
            <th>Tanggal</th>
            <th>{activeTab === 'SALES' ? 'ID Transaksi / Tipe' : 'Deskripsi'}</th>
            <th>{activeTab === 'SALES' ? 'Total / Diskon' : 'Jumlah (Rp)'}</th>
            {activeTab !== 'EXPENSE' && <th>Status / Info</th>}
          </tr>
        </thead>
        <tbody>
          {finances.length > 0 ? (
            finances.map((item) => (
              <tr
                key={item.id}
                onClick={() => activeTab !== 'SALES' && handleOpenModal(item)}
                style={{
                  cursor: activeTab !== 'SALES' ? 'pointer' : 'default',
                  transition: 'background 0.15s'
                }}
                className={activeTab !== 'SALES' ? 'hover-row' : ''}
              >
                <td>{new Date(item.date).toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}</td>

                {activeTab === 'SALES' ? (
                  <>
                    <td>
                      <div className="font-bold">{item.receiptNumber || `#${item.id}`}</div>
                      <div className="text-xs text-gray-500">{item.type} • {item.items?.length || 0} item</div>
                    </td>
                    <td>
                      <div className="font-bold text-gray-800">Rp {Number(item.total || 0).toLocaleString('id-ID')}</div>
                      {item.discount > 0 && <div className="text-xs text-red-500">Diskon: Rp {Number(item.discount || 0).toLocaleString('id-ID')}</div>}
                    </td>
                    <td>
                      <span className="px-2 py-1 bg-blue-50 text-blue-700 text-xs rounded-full font-bold">
                        {item.paymentMethod}
                      </span>
                    </td>
                  </>
                ) : (
                  <>
                    <td>{item.description}</td>
                    <td className="font-semibold text-gray-700">Rp {Number(item.amount || 0).toLocaleString('id-ID')}</td>
                    {activeTab !== 'EXPENSE' && (
                      <td onClick={e => e.stopPropagation()}>
                        <button
                          className={`px-3 py-1 text-xs rounded-full font-bold ${item.status === 'PAID' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}`}
                          onClick={() => handleUpdateStatus(item.id, item.status)}
                        >
                          {item.status}
                        </button>
                      </td>
                    )}
                  </>
                )}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="5" className="text-center p-4">Tidak ada data.</td>
            </tr>
          )}
        </tbody>
      </table>
      {activeTab !== 'SALES' && (
        <div style={{ padding: '8px 16px', borderTop: '1px solid #F3F4F6', background: '#FAFAFA', fontSize: 12, color: '#9CA3AF', display: 'flex', alignItems: 'center', gap: 6 }}>
          <span>💡</span> Klik baris untuk edit
        </div>
      )}
    </div>
  );

  const handleExportExcel = () => {
    let csvContent = "data:text/csv;charset=utf-8,";
    if (activeTab === 'REKAP') {
      csvContent += "Laporan Keuangan POSBah\n\n";
      csvContent += "Kategori,Jumlah (Rp)\n";
      csvContent += "Total Penjualan," + reports.totalSales + "\n";
      csvContent += "Total Pengeluaran," + reports.totalExpenses + "\n";
      csvContent += "Pendapatan Bersih," + reports.netIncome + "\n";
      csvContent += "Piutang Tertunda," + reports.pendingReceivables + "\n";
    } else if (activeTab === 'SALES') {
      csvContent += "Tanggal,No Struk,Tipe,Total (Rp),Diskon (Rp),Metode Bayar\n";
      finances.forEach(item => {
        const row = [
          new Date(item.date).toLocaleDateString('id-ID'),
          item.receiptNumber || `#${item.id}`,
          item.type,
          item.total || 0,
          item.discount || 0,
          item.paymentMethod || '-'
        ].join(",");
        csvContent += row + "\n";
      });
    } else {
      csvContent += "Tanggal,Deskripsi,Jumlah (Rp),Status\n";
      finances.forEach(item => {
        const row = [
          new Date(item.date).toLocaleDateString('id-ID'),
          `"${item.description || ''}"`,
          Number(item.amount || 0),
          item.status || '-'
        ].join(",");
        csvContent += row + "\n";
      });
    }
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `Laporan_${activeTab}_POSBah.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleExportPDF = () => {
    const printWindow = window.open('', '_blank');
    let content = `
      <html>
        <head>
          <title>Laporan Keuangan POSBah</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 20px; color: #333; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
            th { background-color: #f8f9fa; }
            .header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #eee; padding-bottom: 10px; }
            .summary-grid { display: flex; flex-wrap: wrap; gap: 15px; }
            .summary-box { border: 1px solid #ddd; padding: 15px; width: calc(50% - 20px); border-radius: 8px; }
            .title { font-size: 24px; font-weight: bold; margin-bottom: 5px; }
          </style>
        </head>
        <body>
          <div class="header">
            <div class="title">POSBah - Laporan ${activeTab}</div>
            <div>Tanggal Cetak: ${new Date().toLocaleString('id-ID')}</div>
          </div>
    `;

    if (activeTab === 'REKAP') {
      content += `
        <div class="summary-grid">
          <div class="summary-box"><strong>Total Penjualan:</strong><br><br> Rp ${reports.totalSales.toLocaleString('id-ID')}</div>
          <div class="summary-box"><strong>Total Pengeluaran:</strong><br><br> Rp ${reports.totalExpenses.toLocaleString('id-ID')}</div>
          <div class="summary-box"><strong>Pendapatan Bersih:</strong><br><br> Rp ${reports.netIncome.toLocaleString('id-ID')}</div>
          <div class="summary-box"><strong>Piutang Tertunda:</strong><br><br> Rp ${reports.pendingReceivables.toLocaleString('id-ID')}</div>
        </div>
      `;
    } else if (activeTab === 'SALES') {
      content += `
        <table>
          <thead>
            <tr>
              <th>Tanggal</th><th>No Struk</th><th>Tipe</th><th>Total (Rp)</th><th>Metode Bayar</th>
            </tr>
          </thead>
          <tbody>
            ${finances.map(item => `
              <tr>
                <td>${new Date(item.date).toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}</td>
                <td>${item.receiptNumber || '#' + item.id}</td>
                <td>${item.type}</td>
                <td>Rp ${Number(item.total || 0).toLocaleString('id-ID')}</td>
                <td>${item.paymentMethod || '-'}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;
      content += `
        <table>
          <thead>
            <tr><th>Tanggal</th><th>Deskripsi</th><th>Jumlah (Rp)</th><th>Status</th></tr>
          </thead>
          <tbody>
            ${finances.map(item => `
              <tr>
                <td>${new Date(item.date).toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}</td>
                <td>${item.description || '-'}</td>
                <td>Rp ${Number(item.amount || 0).toLocaleString('id-ID')}</td>
                <td>${item.status || '-'}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;
    }

    content += `
        </body>
      </html>
    `;

    printWindow.document.write(content);
    printWindow.document.close();
    printWindow.focus();
    setTimeout(() => {
      printWindow.print();
      printWindow.close();
    }, 250);
  };

  // Paywall hanya untuk tab premium (PAYABLE, RECEIVABLE, EXPENSE)
  const renderPaywall = () => (
    <div className="flex flex-col items-center justify-center text-center py-16">
      <div className="glass-panel max-w-md p-8 w-full border-t-4 border-yellow-400">
        <div className="w-20 h-20 bg-yellow-100 text-yellow-600 rounded-full flex items-center justify-center mx-auto mb-6">
          <Lock size={40} />
        </div>
        <h2 className="text-2xl font-bold mb-3 text-gray-800">Fitur Premium</h2>
        <p className="text-gray-500 mb-8 leading-relaxed">
          Fitur <b>Manajemen {activeTab === 'PAYABLE' ? 'Hutang' : activeTab === 'RECEIVABLE' ? 'Piutang' : 'Pengeluaran'}</b> hanya tersedia untuk versi berbayar.
        </p>
        <button
          className="btn btn-primary w-full py-4 text-lg shadow-lg font-bold"
          onClick={() => {
            const key = prompt('Masukkan Kode Lisensi Premium Anda:');
            const isValidKey = key && key.startsWith('POSBAH-') && key.endsWith('-PRO') && key.length === 20;
            if (isValidKey || key === 'POSBAH-X7V9-QW2R-PRO') {
              localStorage.setItem('posbah_premium', 'true');
              setIsPremium(true);
              alert('Aktivasi Berhasil! Semua fitur Premium telah terbuka.');
            } else if (key) {
              alert('Kode Lisensi Tidak Valid!');
            }
          }}
        >
          Buka Kunci Akses
        </button>
      </div>
    </div>
  );

  return (
    <div className="page-container">
      <div className="header-actions">
        <h1>Keuangan & Laporan</h1>
        <div className="flex gap-2 flex-wrap">
          {isPremium ? (
            <>
              <button className="btn btn-secondary" onClick={handleExportExcel}>
                📊 Excel (CSV)
              </button>
              <button className="btn btn-secondary" onClick={handleExportPDF}>
                🖨️ Cetak PDF
              </button>
            </>
          ) : (
            <button className="btn btn-secondary" style={{ opacity: 0.5, cursor: 'not-allowed', display: 'flex', alignItems: 'center', gap: 6 }} title="Fitur Premium" disabled>
              🔒 Export (Premium)
            </button>
          )}
          {activeTab !== 'REKAP' && activeTab !== 'SALES' && activeTab !== 'MARGIN' && !isLockedTab && (
            <button className="btn btn-primary" onClick={() => handleOpenModal()}>
              <Plus size={18} /> Tambah {activeTab === 'EXPENSE' ? 'Pengeluaran' : activeTab === 'PAYABLE' ? 'Hutang' : 'Piutang'}
            </button>
          )}
        </div>
      </div>

      {renderTabs()}

      {isLockedTab
        ? renderPaywall()
        : activeTab === 'REKAP' ? renderRekap()
          : activeTab === 'MARGIN' ? renderMargin()
            : renderTable()
      }

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel">
            <h2>{formData.id ? 'Edit' : 'Tambah'} {activeTab === 'EXPENSE' ? 'Pengeluaran Usaha' : activeTab === 'PAYABLE' ? 'Hutang' : 'Piutang'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-group">
                <label>Tipe</label>
                <select name="type" value={formData.type} onChange={(e) => setFormData({ ...formData, type: e.target.value })} required disabled={activeTab !== 'REKAP' && activeTab !== 'EXPENSE' && activeTab !== 'PAYABLE' && activeTab !== 'RECEIVABLE'}>
                  <option value="EXPENSE">Pengeluaran Usaha</option>
                  <option value="PAYABLE">Hutang</option>
                  <option value="RECEIVABLE">Piutang</option>
                </select>
              </div>
              <div className="form-group">
                <label>Tanggal</label>
                <input type="datetime-local" name="date" value={formData.date} onChange={(e) => setFormData({ ...formData, date: e.target.value })} required />
              </div>
              <div className="form-group">
                <label>Deskripsi</label>
                <input type="text" name="description" value={formData.description} onChange={(e) => setFormData({ ...formData, description: e.target.value })} required placeholder="Contoh: Beli Token Listrik" />
              </div>
              <div className="form-group">
                <label>Jumlah (Rp)</label>
                <input type="number" name="amount" value={formData.amount} onChange={(e) => setFormData({ ...formData, amount: e.target.value })} required />
              </div>
              {formData.type !== 'EXPENSE' && (
                <div className="form-group">
                  <label>Status</label>
                  <select name="status" value={formData.status} onChange={(e) => setFormData({ ...formData, status: e.target.value })}>
                    <option value="PENDING">PENDING (Belum Lunas)</option>
                    <option value="PAID">PAID (Lunas)</option>
                  </select>
                </div>
              )}
              <div className="modal-actions" style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                {/* Tombol Hapus hanya muncul saat Edit (bukan Tambah baru) */}
                {formData.id && (
                  <button
                    type="button"
                    onClick={() => { handleDelete(formData.id); }}
                    style={{
                      padding: '10px 16px', borderRadius: 10, border: 'none',
                      background: '#FEE2E2', color: '#DC2626',
                      fontWeight: 700, fontSize: '0.85rem', cursor: 'pointer',
                      display: 'flex', alignItems: 'center', gap: 6, marginRight: 'auto'
                    }}
                  >
                    <Trash2 size={15} /> Hapus
                  </button>
                )}
                <button type="button" className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>Batal</button>
                <button type="submit" className="btn btn-primary">Simpan</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
