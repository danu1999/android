import React, { useState, useEffect } from 'react';
import { CreditCard, ArrowDownCircle, ArrowUpCircle, TrendingUp, Plus, Edit2, Trash2, Lock } from 'lucide-react';
import api from '../api';
import { useDemoBlock, useAuth } from '../AuthContext';

/** Konversi Date ke string datetime-local sesuai timezone lokal browser */
const toLocalDatetime = (date = new Date()) => {
  const d = new Date(date);
  const offset = d.getTimezoneOffset() * 60000; // offset dalam ms
  return new Date(d.getTime() - offset).toISOString().slice(0, 16);
};

export default function Keuangan() {
  const { showDemoBlock, isDemo } = useDemoBlock();
  const { user } = useAuth();

  // Owner hanya boleh edit data Gaji (deskripsi diawali '[Gaji]')
  const isOwner = user?.role === 'OWNER';
  const isGajiRecord = (item) => item?.description?.startsWith('[Gaji]');
  const canEdit = (item) => !isOwner || isGajiRecord(item);

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
    if (isDemo) {
      if (activeTab === 'REKAP') {
        setReports({
          totalSales: 45280000,
          totalExpenses: 12400000,
          netIncome: 32880000,
          pendingReceivables: 1850000
        });
      } else if (activeTab === 'SALES') {
        setFinances([
          { id: 'f101', date: '2026-05-20T10:15:00.000Z', customerName: 'Budi Santoso (Demo)', total: 155000, paymentMethod: 'QRIS', status: 'COMPLETED' },
          { id: 'f102', date: '2026-05-20T09:40:00.000Z', customerName: 'Siti Rahma (Demo)', total: 75000, paymentMethod: 'CASH', status: 'COMPLETED' },
          { id: 'f103', date: '2026-05-19T17:20:00.000Z', customerName: 'Andi Wijaya (Demo)', total: 320000, paymentMethod: 'CASH', status: 'COMPLETED' },
          { id: 'f104', date: '2026-05-19T14:10:00.000Z', customerName: 'Dewi Lestari (Demo)', total: 110000, paymentMethod: 'QRIS', status: 'COMPLETED' },
          { id: 'f105', date: '2026-05-18T11:05:00.000Z', customerName: 'Rian Hidayat (Demo)', total: 45000, paymentMethod: 'CASH', status: 'COMPLETED' },
        ]);
      } else if (activeTab === 'MARGIN') {
        setProducts([
          { id: 'p301', name: 'Pisang Keju Cokelat', price: 15000, costPrice: 9000, stock: 120, variantEnabled: false, variants: null },
          { id: 'p302', name: 'Pisang Keju Stroberi', price: 15000, costPrice: 9500, stock: 85, variantEnabled: false, variants: null },
          { id: 'p303', name: 'Pisang Keju Premium', price: 20000, costPrice: 11000, stock: 50, variantEnabled: true, variants: JSON.stringify([{ name: 'Keju Melimpah', price: 25000, costPrice: 13000, stock: 30 }, { name: 'Milo Almond', price: 28000, costPrice: 15000, stock: 20 }]) },
          { id: 'p304', name: 'Jus Alpukat', price: 18000, costPrice: 10000, stock: 60, variantEnabled: false, variants: null },
          { id: 'p305', name: 'Jus Mangga', price: 15000, costPrice: 8000, stock: 75, variantEnabled: false, variants: null },
          { id: 'p306', name: 'Es Teh Manis', price: 8000, costPrice: 3000, stock: 200, variantEnabled: false, variants: null },
        ]);
      } else if (activeTab === 'EXPENSE') {
        setFinances([
          { id: 'f201', date: '2026-05-18T08:00:00.000Z', description: 'Belanja Pisang & Bahan Baku Keju (Demo)', amount: 850000, type: 'EXPENSE', status: 'PAID' },
          { id: 'f202', date: '2026-05-15T09:30:00.000Z', description: 'Beli Cup & Plastik Kemasan (Demo)', amount: 350000, type: 'EXPENSE', status: 'PAID' },
          { id: 'f203', date: '2026-05-02T10:00:00.000Z', description: 'Bayar Token Listrik Outlet (Demo)', amount: 200000, type: 'EXPENSE', status: 'PAID' },
          { id: 'f204', date: '2026-05-01T17:00:00.000Z', description: 'Uang Keamanan & Kebersihan (Demo)', amount: 50000, type: 'EXPENSE', status: 'PAID' },
        ]);
      } else if (activeTab === 'PAYABLE') {
        setFinances([
          { id: 'f401', date: '2026-05-10T12:00:00.000Z', description: 'Hutang Agen Pisang Pak Slamet (Demo)', amount: 1500000, type: 'PAYABLE', status: 'PENDING' },
          { id: 'f402', date: '2026-05-05T14:30:00.000Z', description: 'Sewa Ruko Sisa Bulan Depan (Demo)', amount: 3000000, type: 'PAYABLE', status: 'PENDING' },
        ]);
      } else if (activeTab === 'RECEIVABLE') {
        setFinances([
          { id: 'f501', date: '2026-05-18T16:00:00.000Z', description: 'Piutang Catering Ibu Ratna (Demo)', amount: 1200000, type: 'RECEIVABLE', status: 'PENDING' },
          { id: 'f502', date: '2026-05-17T11:00:00.000Z', description: 'Titipan Modal Koperasi (Demo)', amount: 650000, type: 'RECEIVABLE', status: 'PENDING' },
        ]);
      }
      return;
    }

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

  // Selalu refresh rekap setelah mutasi — agar kartu ringkasan sinkron
  const fetchReports = async () => {
    if (isDemo) return;
    try {
      const res = await api.get('/reports');
      setReports(res.data);
    } catch (err) {
      console.error('Failed to refresh reports', err);
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
    if (isOwner && !isGajiRecord(formData)) {
      alert('Role Owner hanya dapat mengedit data Gaji Karyawan.');
      return;
    }
    try {
      if (formData.id) {
        await api.put(`/finances/${formData.id}`, formData);
      } else {
        await api.post('/finances', formData);
      }
      setIsModalOpen(false);
      fetchData();
      fetchReports();
    } catch (err) {
      console.error('Failed to save finance', err);
      alert(err?.response?.data?.error || 'Gagal menyimpan data.');
    }
  };

  const handleDelete = async (id, item) => {
    if (isDemo) { showDemoBlock('Menghapus data keuangan hanya tersedia di akun berbayar.'); return; }
    if (isOwner && !isGajiRecord(item)) {
      alert('Role Owner hanya dapat menghapus data Gaji Karyawan.');
      return;
    }
    if (!window.confirm('Yakin ingin menghapus data ini? Rekap laporan akan ikut diperbarui.')) return;
    try {
      await api.delete(`/finances/${id}`);
      fetchData();
      fetchReports();
    } catch (err) {
      console.error('Failed to delete', err);
      alert(err?.response?.data?.error || 'Gagal menghapus data.');
    }
  };

  const handleUpdateStatus = async (id, currentStatus, item) => {
    if (isOwner && !isGajiRecord(item)) {
      alert('Role Owner hanya dapat mengubah status data Gaji Karyawan.');
      return;
    }
    const newStatus = currentStatus === 'PENDING' ? 'PAID' : 'PENDING';
    try {
      await api.put(`/finances/${id}`, { status: newStatus });
      fetchData();
      fetchReports();
    } catch (err) {
      console.error('Failed to update status', err);
    }
  };

  const renderTabs = () => (
    <div style={{ display: 'flex', gap: 8, overflowX: 'auto', WebkitOverflowScrolling: 'touch', paddingBottom: 4, marginBottom: 16, scrollbarWidth: 'none' }}>
      {[
        { id: 'REKAP', label: '📊 Rekap' },
        { id: 'SALES', label: '🛒 Transaksi' },
        { id: 'EXPENSE', label: '💸 Pengeluaran' },
        { id: 'MARGIN', label: '📈 Margin' },
        { id: 'PAYABLE', label: '🔴 Hutang' },
        { id: 'RECEIVABLE', label: '🟢 Piutang' },
      ].map(tab => (
        <button key={tab.id} onClick={() => setActiveTab(tab.id)} style={{
          padding: '9px 18px', borderRadius: 99, border: 'none', cursor: 'pointer', whiteSpace: 'nowrap',
          fontWeight: 700, fontSize: 13, flexShrink: 0, transition: 'all 0.2s',
          background: activeTab === tab.id ? 'linear-gradient(135deg,#6366F1,#4F46E5)' : '#F1F5F9',
          color: activeTab === tab.id ? 'white' : '#64748B',
          boxShadow: activeTab === tab.id ? '0 4px 12px rgba(99,102,241,0.3)' : 'none',
        }}>
          {tab.label}{PREMIUM_TABS.includes(tab.id) && !isPremium ? ' 🔒' : ''}
        </button>
      ))}
    </div>
  );

  const renderMargin = () => {
    const flattenedProducts = [];

    products.forEach(p => {
      let variants = [];
      try {
        if (p.variants) variants = JSON.parse(p.variants);
      } catch (e) { }

      if (!p.variantEnabled || variants.length === 0) {
        flattenedProducts.push({
          ...p,
          isVariant: false
        });
      } else {
        // Original option
        flattenedProducts.push({
          ...p,
          id: p.id + '-original',
          name: p.name + ' (Original)',
          isVariant: false,
          isOriginal: true
        });

        // Variants
        variants.forEach((v, idx) => {
          flattenedProducts.push({
            ...p,
            id: p.id + '-v' + idx,
            name: p.name + ' - ' + v.name,
            price: v.price != null && v.price !== '' ? Number(v.price) : p.price,
            costPrice: v.costPrice != null && v.costPrice !== '' ? Number(v.costPrice) : p.costPrice,
            stock: v.stock != null && v.stock !== '' ? Number(v.stock) : p.stock,
            isVariant: true,
            isOriginal: false
          });
        });
      }
    });

    const withCost = flattenedProducts.filter(p => p.costPrice > 0);

    // ── Weighted average margin (bobot = stok) ──────────────────
    const totalStockWithCost = withCost.reduce((s, p) => s + p.stock, 0);
    const avgMargin = totalStockWithCost > 0
      ? Math.round(
        withCost.reduce((s, p) => s + ((p.price - p.costPrice) / p.price) * 100 * p.stock, 0)
        / totalStockWithCost
      )
      : 0;

    const totalRevenuePotential = withCost.reduce((s, p) => s + p.price * p.stock, 0);
    const totalCostValue = withCost.reduce((s, p) => s + p.costPrice * p.stock, 0);
    const totalPotentialProfit = totalRevenuePotential - totalCostValue;

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
      ...flattenedProducts.filter(p => p.costPrice <= 0)
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

        {/* Card list per produk */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {allProducts.length === 0
            ? <div style={{ textAlign: 'center', padding: '32px', color: '#94A3B8' }}>Belum ada data produk.</div>
            : allProducts.map(p => {
              const hasCost = p.costPrice > 0;
              const margin = hasCost ? Math.round(((p.price - p.costPrice) / p.price) * 100) : null;
              const profitPerItem = hasCost ? p.price - p.costPrice : null;
              const potentialProfit = profitPerItem !== null ? profitPerItem * p.stock : null;
              const badgeBg = margin >= 20 ? '#D1FAE5' : margin >= 10 ? '#FEF3C7' : '#FEE2E2';
              const badgeColor = margin >= 20 ? '#065F46' : margin >= 10 ? '#78350F' : '#7F1D1D';
              const barColor = margin >= 20 ? '#10B981' : margin >= 10 ? '#F59E0B' : '#EF4444';
              const hasVariant = !!p.variants;
              const hasWholesale = !!p.wholesaleEnabled;
              return (
                <div key={p.id} style={{ background: 'white', borderRadius: 14, padding: '12px 14px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', border: '1.5px solid #F1F5F9', opacity: hasCost ? 1 : 0.55 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 10 }}>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                        <span style={{ fontWeight: 800, fontSize: 14, color: '#1E293B' }}>{p.name}</span>
                        {hasVariant && <span style={{ fontSize: 10, fontWeight: 800, background: '#EDE9FE', color: '#6D28D9', padding: '1px 6px', borderRadius: 99 }}>🎨 Varian</span>}
                        {hasWholesale && <span style={{ fontSize: 10, fontWeight: 800, background: '#FFF7ED', color: '#C2410C', padding: '1px 6px', borderRadius: 99 }}>🏷️ Grosir</span>}
                      </div>
                      <div style={{ fontSize: 12, color: '#6B7280', marginTop: 3 }}>
                        Jual: Rp {p.price.toLocaleString('id-ID')} · Modal: {hasCost ? `Rp ${p.costPrice.toLocaleString('id-ID')}` : <span style={{ color: '#D1D5DB' }}>Belum diisi</span>}
                      </div>
                      {profitPerItem !== null && <div style={{ fontSize: 12, color: '#059669', fontWeight: 700, marginTop: 2 }}>+Rp {profitPerItem.toLocaleString('id-ID')}/item · Stok: {p.stock} {p.unit || 'pcs'}</div>}
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4, flexShrink: 0 }}>
                      {margin !== null
                        ? <span style={{ fontSize: 13, fontWeight: 900, padding: '3px 10px', borderRadius: 99, background: badgeBg, color: badgeColor }}>{margin}%</span>
                        : <span style={{ fontSize: 12, color: '#D1D5DB' }}>—</span>}
                      {potentialProfit !== null && <div style={{ fontSize: 11, color: '#7C3AED', fontWeight: 700 }}>Rp {potentialProfit.toLocaleString('id-ID')}</div>}
                    </div>
                  </div>
                  {margin !== null && (
                    <div style={{ marginTop: 8, height: 5, background: '#F3F4F6', borderRadius: 99, overflow: 'hidden' }}>
                      <div style={{ width: `${Math.min((margin / maxMargin) * 100, 100)}%`, height: '100%', background: barColor, borderRadius: 99 }} />
                    </div>
                  )}
                </div>
              );
            })
          }
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
        <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap', background: '#F8FAFC', padding: '12px', borderRadius: 14, border: '1px solid #E2E8F0' }}>
          <span style={{ fontWeight: 700, fontSize: 13, color: '#475569', width: '100%' }}>📅 Filter Periode</span>
          <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} style={{ flex: 1, minWidth: 130, padding: '8px 10px', border: '1px solid #E2E8F0', borderRadius: 10, fontSize: 13 }} />
          <span style={{ alignSelf: 'center', color: '#94A3B8' }}>–</span>
          <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} style={{ flex: 1, minWidth: 130, padding: '8px 10px', border: '1px solid #E2E8F0', borderRadius: 10, fontSize: 13 }} />
          {(dateFrom || dateTo) && <button onClick={() => { setDateFrom(''); setDateTo(''); }} style={{ padding: '8px 12px', background: '#FEE2E2', color: '#DC2626', border: 'none', borderRadius: 10, fontWeight: 700, cursor: 'pointer', fontSize: 12 }}>✕ Reset</button>}
        </div>

        {/* Stat cards 2×2 */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 4 }}>
          {[
            { icon: '📈', label: 'Total Penjualan', value: reports.totalSales, color: '#3B82F6', bg: '#EFF6FF', border: '#BFDBFE' },
            { icon: '💸', label: 'Pengeluaran', value: reports.totalExpenses, color: '#EF4444', bg: '#FEF2F2', border: '#FECACA' },
            { icon: '💰', label: 'Pendapatan Bersih', value: reports.netIncome, color: reports.netIncome >= 0 ? '#10B981' : '#EF4444', bg: reports.netIncome >= 0 ? '#F0FDF4' : '#FEF2F2', border: reports.netIncome >= 0 ? '#A7F3D0' : '#FECACA' },
            { icon: '⏳', label: 'Piutang Pending', value: reports.pendingReceivables, color: '#F59E0B', bg: '#FFFBEB', border: '#FDE68A' },
          ].map(s => (
            <div key={s.label} style={{ background: s.bg, border: `1.5px solid ${s.border}`, borderRadius: 16, padding: '14px 14px' }}>
              <div style={{ fontSize: 20, marginBottom: 4 }}>{s.icon}</div>
              <div style={{ fontSize: 11, fontWeight: 700, color: s.color, opacity: 0.8, marginBottom: 4 }}>{s.label}</div>
              <div style={{ fontSize: 16, fontWeight: 900, color: s.color, lineHeight: 1.2 }}>Rp {s.value.toLocaleString('id-ID')}</div>
            </div>
          ))}
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
                onClick={() => activeTab !== 'SALES' && canEdit(item) && handleOpenModal(item)}
                style={{
                  cursor: activeTab !== 'SALES' && canEdit(item) ? 'pointer' : 'default',
                  transition: 'background 0.15s',
                  opacity: activeTab !== 'SALES' && isOwner && !isGajiRecord(item) ? 0.7 : 1,
                }}
                className={activeTab !== 'SALES' && canEdit(item) ? 'hover-row' : ''}
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
                          onClick={() => handleUpdateStatus(item.id, item.status, item)}
                          disabled={isOwner && !isGajiRecord(item)}
                          title={isOwner && !isGajiRecord(item) ? 'Owner hanya dapat mengubah status Gaji Karyawan' : ''}
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
          <span>💡</span>
          {isOwner
            ? 'Klik baris data Gaji untuk edit (Owner hanya dapat mengelola data Gaji Karyawan)'
            : 'Klik baris untuk edit'
          }
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
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, gap: 12 }}>
        <h1 style={{ margin: 0, fontSize: '1.3rem', fontWeight: 900, color: '#1E293B' }}>Keuangan &amp; Laporan</h1>
        <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
          {isPremium ? (
            <>
              <button title="Export CSV" onClick={handleExportExcel} style={{ padding: '8px 12px', borderRadius: 12, border: '1.5px solid #E5E7EB', background: 'white', cursor: 'pointer', fontSize: 16 }}>📊</button>
              <button title="Cetak PDF" onClick={handleExportPDF} style={{ padding: '8px 12px', borderRadius: 12, border: '1.5px solid #E5E7EB', background: 'white', cursor: 'pointer', fontSize: 16 }}>🖨️</button>
            </>
          ) : (
            <button disabled title="Export (Premium)" style={{ padding: '8px 12px', borderRadius: 12, border: '1.5px solid #E5E7EB', background: '#F9FAFB', opacity: 0.6, cursor: 'not-allowed', fontSize: 16 }}>🔒</button>
          )}
          {activeTab !== 'REKAP' && activeTab !== 'SALES' && activeTab !== 'MARGIN' && !isLockedTab && (
            <button onClick={() => handleOpenModal()} style={{ display: 'flex', alignItems: 'center', gap: 5, padding: '8px 14px', background: 'linear-gradient(135deg,#6366F1,#4F46E5)', color: 'white', border: 'none', borderRadius: 12, fontWeight: 800, fontSize: 13, cursor: 'pointer', boxShadow: '0 4px 12px rgba(99,102,241,0.3)' }}>
              <Plus size={16} /> Tambah
            </button>
          )}
        </div>
      </div>

      {isDemo && (
        <div style={{ background: '#EFF6FF', border: '1.5px solid #BFDBFE', borderRadius: 12, padding: '10px 14px', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 16 }}>💡</span>
          <span style={{ fontWeight: 600, color: '#1E40AF', fontSize: 13 }}>
            <b>Mode Demo:</b> Menampilkan data fiktif (simulasi) untuk uji coba menu Keuangan &amp; Laporan.
          </span>
        </div>
      )}

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
