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

export default function Keuangan({ appMode: propAppMode }) {
  const appMode = propAppMode || localStorage.getItem('posbah_app_mode') || 'FNB';
  const { showDemoBlock, isDemo } = useDemoBlock();
  const { user } = useAuth();

  // Owner hanya boleh edit data Gaji (deskripsi diawali '[Gaji]')
  const isOwner = user?.role === 'OWNER';
  const isGajiRecord = (item) => item?.description?.startsWith('[Gaji]');
  const canEdit = (item) => !isOwner || isGajiRecord(item);

  const [isPremium, setIsPremium] = useState(true);

  useEffect(() => {
    if (isDemo) {
      setIsPremium(true);
    }
  }, [isDemo]);

  const [activeTab, setActiveTab] = useState('REKAP');
  const [finances, setFinances] = useState([]);
  const [reports, setReports] = useState(null);
  const [products, setProducts] = useState([]);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  const filteredFinances = finances.filter(item => {
    const itemDateStr = item.date || item.createdAt || item.startDate;
    if (!itemDateStr) return true;
    const itemDate = new Date(itemDateStr);
    const checkDate = new Date(itemDate.getFullYear(), itemDate.getMonth(), itemDate.getDate()).getTime();
    
    if (dateFrom) {
      const fromDate = new Date(dateFrom);
      const checkFrom = new Date(fromDate.getFullYear(), fromDate.getMonth(), fromDate.getDate()).getTime();
      if (checkDate < checkFrom) return false;
    }
    if (dateTo) {
      const toDate = new Date(dateTo);
      const checkTo = new Date(toDate.getFullYear(), toDate.getMonth(), toDate.getDate()).getTime();
      if (checkDate > checkTo) return false;
    }
    return true;
  });

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
  }, [activeTab, isPremium, dateFrom, dateTo]);

  const fetchData = async () => {
    try {
      if (activeTab === 'REKAP') {
        const res = await api.get('/reports', { params: { from: dateFrom, to: dateTo } });
        setReports(res.data);
      } else if (activeTab === 'SALES') {
        const endpoint = appMode === 'RENTAL' ? '/rentals' : appMode === 'LAUNDRY' ? '/laundry/orders' : '/transactions';
        const res = await api.get(endpoint);
        setFinances(res.data);
      } else if (activeTab === 'MARGIN') {
        if (appMode === 'LAUNDRY') {
          setProducts([]);
        } else {
          const res = await api.get('/products');
          setProducts(res.data);
        }
      } else if (appMode === 'LAUNDRY') {
        if (activeTab === 'EXPENSE') {
          const res = await api.get('/laundry/expenses');
          const mapped = res.data.map(e => ({
            id: e.id,
            type: 'EXPENSE',
            amount: e.nominal,
            description: e.keterangan || e.kategori,
            date: e.tanggal || e.createdAt,
            category: e.kategori
          }));
          setFinances(mapped);
        } else if (activeTab === 'RECEIVABLE') {
          const res = await api.get('/laundry/orders');
          const mapped = res.data.filter(o => o.statusBayar === 'Belum Lunas').map(o => ({
            id: o.id,
            type: 'RECEIVABLE',
            amount: o.totalHarga,
            description: `Order ${o.receiptNumber} - Pelanggan: ${o.namaPelanggan}`,
            date: o.tanggalMasuk || o.createdAt,
            status: 'PENDING',
            isLaundryOrder: true
          }));
          setFinances(mapped);
        } else {
          setFinances([]);
        }
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
    try {
      const res = await api.get('/reports', { params: { from: dateFrom, to: dateTo } });
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
    if (isOwner && !isGajiRecord(formData)) {
      alert('Role Owner hanya dapat mengedit data Gaji Karyawan.');
      return;
    }
    try {
      if (appMode === 'LAUNDRY') {
        if (formData.type === 'EXPENSE') {
          const payload = {
            nominal: Number(formData.amount),
            keterangan: formData.description,
            kategori: formData.category || formData.description.split(' ')[0] || 'Operasional',
            tanggal: new Date(formData.date).toISOString()
          };
          if (formData.id) {
            await api.put(`/laundry/expenses/${formData.id}`, payload);
          } else {
            await api.post('/laundry/expenses', payload);
          }
        } else {
          alert('Piutang Laundry diubah dari menu Riwayat Order (toggle pembayaran)');
          return;
        }
      } else {
        if (formData.id) {
          await api.put(`/finances/${formData.id}`, formData);
        } else {
          await api.post('/finances', formData);
        }
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
    if (isOwner && !isGajiRecord(item)) {
      alert('Role Owner hanya dapat menghapus data Gaji Karyawan.');
      return;
    }
    if (!window.confirm('Yakin ingin menghapus data ini? Rekap laporan akan ikut diperbarui.')) return;
    try {
      if (appMode === 'LAUNDRY') {
        if (activeTab === 'EXPENSE') {
          await api.delete(`/laundry/expenses/${id}`);
        } else {
          alert('Order laundry tidak dapat dihapus dari sini. Silakan hapus dari menu Riwayat Order.');
          return;
        }
      } else {
        await api.delete(`/finances/${id}`);
      }
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
    if (appMode === 'LAUNDRY') {
      if (item && item.isLaundryOrder) {
        try {
          await api.put(`/laundry/orders/pay/${id}`);
          fetchData();
          fetchReports();
        } catch (err) {
          console.error('Failed to update laundry status', err);
        }
      }
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

  const renderTabs = () => {
    const tabs = [
      { id: 'REKAP', label: '📊 Rekap' },
      { id: 'SALES', label: appMode === 'RENTAL' ? '🚗 Sewa Mobil' : appMode === 'LAUNDRY' ? '🧺 Order Laundry' : '🛒 Transaksi' },
      { id: 'EXPENSE', label: '💸 Pengeluaran' },
      ...(appMode === 'RENTAL' || appMode === 'LAUNDRY' ? [] : [{ id: 'MARGIN', label: '📈 Margin' }]),
      ...(appMode === 'LAUNDRY' ? [] : [{ id: 'PAYABLE', label: '🔴 Hutang' }]),
      { id: 'RECEIVABLE', label: '🟢 Piutang' },
    ];
    return (
      <div style={{ display: 'flex', gap: 8, overflowX: 'auto', WebkitOverflowScrolling: 'touch', paddingBottom: 4, marginBottom: 16, scrollbarWidth: 'none' }}>
        {tabs.map(tab => (
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
  };

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
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(clamp(130px, 45vw, 260px), 1fr))', gap: 14, marginBottom: 24 }}>

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

    const r = reports;
    const mainCards = [
      { icon: '📈', label: 'Total Penjualan', value: r.totalSales || 0, color: '#3B82F6', bg: '#EFF6FF', border: '#BFDBFE' },
      { icon: '💸', label: 'Total Pengeluaran', value: r.totalExpenses || 0, color: '#EF4444', bg: '#FEF2F2', border: '#FECACA' },
      { icon: '💰', label: 'Pendapatan Bersih', value: r.netIncome || 0, color: (r.netIncome || 0) >= 0 ? '#10B981' : '#EF4444', bg: (r.netIncome || 0) >= 0 ? '#F0FDF4' : '#FEF2F2', border: (r.netIncome || 0) >= 0 ? '#A7F3D0' : '#FECACA' },
      { icon: '⏳', label: 'Piutang Pending', value: r.pendingReceivables || 0, color: '#F59E0B', bg: '#FFFBEB', border: '#FDE68A' },
    ];

    const extraCards = [];
    if (r.grossProfit !== undefined && appMode !== 'RENTAL') {
      extraCards.push({ icon: '📊', label: 'Laba Kotor (Gross Profit)', value: r.grossProfit || 0, color: '#7C3AED', bg: '#F5F3FF', border: '#DDD6FE' });
    }
    if (r.totalPayable !== undefined) {
      extraCards.push({ icon: '🔴', label: 'Hutang Pending', value: r.totalPayable || 0, color: '#DC2626', bg: '#FEF2F2', border: '#FECACA' });
    }
    if (r.todaySales !== undefined) {
      extraCards.push({ icon: '🗓️', label: 'Penjualan Hari Ini', value: r.todaySales || 0, color: '#0EA5E9', bg: '#F0F9FF', border: '#BAE6FD' });
    }
    if (r.transactionCount !== undefined) {
      extraCards.push({ icon: '🧾', label: 'Jumlah Transaksi', value: r.transactionCount || 0, color: '#64748B', bg: '#F8FAFC', border: '#E2E8F0', isCount: true });
    }

    return (
      <div>
        {/* Kartu Utama 2×2 */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(clamp(130px, 45vw, 240px), 1fr))', gap: 12, marginBottom: 12 }}>
          {mainCards.map(s => (
            <div key={s.label} style={{ background: s.bg, border: `1.5px solid ${s.border}`, borderRadius: 16, padding: '14px 14px' }}>
              <div style={{ fontSize: 20, marginBottom: 4 }}>{s.icon}</div>
              <div style={{ fontSize: 11, fontWeight: 700, color: s.color, opacity: 0.8, marginBottom: 4 }}>{s.label}</div>
              <div style={{ fontSize: 16, fontWeight: 900, color: s.color, lineHeight: 1.2 }}>Rp {(s.value).toLocaleString('id-ID')}</div>
            </div>
          ))}
        </div>

        {/* Kartu Tambahan */}
        {extraCards.length > 0 && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 10, marginBottom: 4 }}>
            {extraCards.map(s => (
              <div key={s.label} style={{ background: s.bg, border: `1.5px solid ${s.border}`, borderRadius: 14, padding: '12px 14px' }}>
                <div style={{ fontSize: 18, marginBottom: 4 }}>{s.icon}</div>
                <div style={{ fontSize: 10, fontWeight: 700, color: s.color, opacity: 0.8, marginBottom: 4 }}>{s.label}</div>
                <div style={{ fontSize: 15, fontWeight: 900, color: s.color, lineHeight: 1.2 }}>
                  {s.isCount ? s.value : `Rp ${(s.value).toLocaleString('id-ID')}`}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  };

  const renderTable = () => (
    <div className="glass-panel table-container">
      <table className="data-table">
        <thead>
          <tr>
            <th>Tanggal</th>
            <th>{activeTab === 'SALES' ? (appMode === 'RENTAL' ? 'Mobil & Pelanggan' : appMode === 'LAUNDRY' ? 'Layanan & Pelanggan' : 'Nama Transaksi') : 'Deskripsi'}</th>
            <th>{activeTab === 'SALES' ? (appMode === 'RENTAL' ? 'Durasi / Biaya' : appMode === 'LAUNDRY' ? 'Detail / Biaya' : 'Total / Diskon') : 'Jumlah (Rp)'}</th>
            {activeTab !== 'EXPENSE' && <th>Status / Info</th>}
          </tr>
        </thead>
        <tbody>
          {filteredFinances.length > 0 ? (
            filteredFinances.map((item) => (
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
                <td>{new Date(item.date || item.createdAt || item.startDate || item.tanggalMasuk).toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}</td>

                {activeTab === 'SALES' ? (
                  appMode === 'RENTAL' ? (
                    <>
                      <td>
                        <div className="font-bold">{item.car?.name} ({item.car?.plateNumber})</div>
                        <div className="text-xs text-gray-600" style={{ marginTop: 2, fontStyle: 'italic' }}>
                          Pelanggan: {item.customerName}
                        </div>
                      </td>
                      <td>
                        <div className="font-bold text-gray-800">Rp {Number(item.totalPrice || 0).toLocaleString('id-ID')}</div>
                        <div className="text-xs text-gray-500" style={{ marginTop: 2 }}>
                          {new Date(item.startDate).toLocaleDateString('id-ID')} s.d. {new Date(item.endDate).toLocaleDateString('id-ID')}
                        </div>
                      </td>
                      <td>
                        <span className={`px-2 py-1 text-xs rounded-full font-bold ${item.status === 'RETURNED' ? 'bg-green-100 text-green-700' : 'bg-blue-100 text-blue-700'}`}>
                          {item.status === 'RETURNED' ? 'Selesai (Mobil Kembali)' : 'Aktif (Disewa)'}
                        </span>
                      </td>
                    </>
                  ) : appMode === 'LAUNDRY' ? (
                    <>
                      <td>
                        <div className="font-bold">{item.receiptNumber || `#${item.id}`}</div>
                        <div className="text-xs text-gray-600" style={{ marginTop: 2, fontStyle: 'italic' }}>
                          Pelanggan: {item.namaPelanggan} ({item.noHp})
                        </div>
                      </td>
                      <td>
                        <div className="font-bold text-gray-800">Rp {Number(item.totalHarga || 0).toLocaleString('id-ID')}</div>
                        <div className="text-xs text-gray-500" style={{ marginTop: 2, maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={item.jenisLayanan}>
                          {item.jenisLayanan}
                        </div>
                      </td>
                      <td>
                        <span className={`px-2 py-1 text-xs rounded-full font-bold ${item.statusBayar === 'Lunas' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                          {item.statusBayar}
                        </span>
                        <span className="px-2 py-1 bg-blue-50 text-blue-700 text-xs rounded-full font-bold ml-2">
                          {item.status}
                        </span>
                      </td>
                    </>
                  ) : (
                    <>
                      <td>
                        <div className="font-bold">{item.receiptNumber || `#${item.id}`}</div>
                        <div className="text-xs text-gray-600" style={{ marginTop: 2, fontStyle: 'italic' }}>
                          {item.items?.map(it => {
                            const name = it.product?.name || 'Produk';
                            const variant = it.variantName ? ` [${it.variantName}]` : '';
                            return `${name}${variant} (${it.quantity} ${it.product?.unit || 'pcs'})`;
                          }).join(', ') || 'Tidak ada detail item'}
                        </div>
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
                  )
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
      if (appMode === 'RENTAL') {
        csvContent += "Tanggal,Mobil,Pelanggan,Mulai,Selesai,Total Biaya (Rp),Status\n";
        filteredFinances.forEach(item => {
          const row = [
            new Date(item.createdAt || item.startDate).toLocaleDateString('id-ID'),
            `"${item.car?.name || ''} (${item.car?.plateNumber || ''})"`,
            `"${item.customerName || ''}"`,
            new Date(item.startDate).toLocaleDateString('id-ID'),
            new Date(item.endDate).toLocaleDateString('id-ID'),
            item.totalPrice || 0,
            item.status || '-'
          ].join(",");
          csvContent += row + "\n";
        });
      } else if (appMode === 'LAUNDRY') {
        csvContent += "Tanggal,No Struk,Nama Pelanggan,No HP,Layanan,Total (Rp),Status Bayar,Status Cucian\n";
        filteredFinances.forEach(item => {
          const row = [
            new Date(item.tanggalMasuk || item.createdAt).toLocaleDateString('id-ID'),
            item.receiptNumber || `#${item.id}`,
            `"${item.namaPelanggan || ''}"`,
            `"${item.noHp || ''}"`,
            `"${item.jenisLayanan || ''}"`,
            item.totalHarga || 0,
            item.statusBayar || '',
            item.status || ''
          ].join(",");
          csvContent += row + "\n";
        });
      } else {
        csvContent += "Tanggal,No Struk,Tipe,Total (Rp),Diskon (Rp),Metode Bayar\n";
        filteredFinances.forEach(item => {
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
      }
    } else {
      csvContent += "Tanggal,Deskripsi,Jumlah (Rp),Status\n";
      filteredFinances.forEach(item => {
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
            <div class="title">POSBah - Laporan ${activeTab === 'SALES' && appMode === 'RENTAL' ? 'Sewa Mobil' : activeTab}</div>
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
      if (appMode === 'RENTAL') {
        content += `
          <table>
            <thead>
              <tr>
                <th>Tanggal</th><th>Mobil</th><th>Pelanggan</th><th>Durasi</th><th>Total Biaya (Rp)</th><th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${filteredFinances.map(item => `
                <tr>
                  <td>${new Date(item.createdAt || item.startDate).toLocaleDateString('id-ID')}</td>
                  <td>${item.car?.name || ''} (${item.car?.plateNumber || ''})</td>
                  <td>${item.customerName}</td>
                  <td>${new Date(item.startDate).toLocaleDateString('id-ID')} - ${new Date(item.endDate).toLocaleDateString('id-ID')}</td>
                  <td>Rp ${Number(item.totalPrice || 0).toLocaleString('id-ID')}</td>
                  <td>${item.status === 'RETURNED' ? 'Selesai' : 'Aktif'}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        `;
      } else if (appMode === 'LAUNDRY') {
        content += `
          <table>
            <thead>
              <tr>
                <th>Tanggal</th><th>No Struk</th><th>Pelanggan</th><th>Layanan</th><th>Total (Rp)</th><th>Bayar</th><th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${filteredFinances.map(item => `
                <tr>
                  <td>${new Date(item.tanggalMasuk || item.createdAt).toLocaleDateString('id-ID')}</td>
                  <td>${item.receiptNumber || '#' + item.id}</td>
                  <td>${item.namaPelanggan} (${item.noHp})</td>
                  <td>${item.jenisLayanan}</td>
                  <td>Rp ${Number(item.totalHarga || 0).toLocaleString('id-ID')}</td>
                  <td>${item.statusBayar}</td>
                  <td>${item.status}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        `;
      } else {
        content += `
          <table>
            <thead>
              <tr>
                <th>Tanggal</th><th>No Struk</th><th>Tipe</th><th>Total (Rp)</th><th>Metode Bayar</th>
              </tr>
            </thead>
            <tbody>
              ${filteredFinances.map(item => `
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
      }
    } else {
      content += `
        <table>
          <thead>
            <tr><th>Tanggal</th><th>Deskripsi</th><th>Jumlah (Rp)</th><th>Status</th></tr>
          </thead>
          <tbody>
            ${filteredFinances.map(item => `
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

      {/* Filter Tanggal (Berlaku untuk semua tab kecuali MARGIN) */}
      {activeTab !== 'MARGIN' && (
        <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap', background: '#F8FAFC', padding: '12px', borderRadius: 14, border: '1px solid #E2E8F0' }}>
          <span style={{ fontWeight: 700, fontSize: 13, color: '#475569', width: '100%' }}>📅 Filter Periode</span>
          <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} style={{ flex: 1, minWidth: 130, padding: '8px 10px', border: '1px solid #E2E8F0', borderRadius: 10, fontSize: 13 }} />
          <span style={{ alignSelf: 'center', color: '#94A3B8' }}>–</span>
          <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} style={{ flex: 1, minWidth: 130, padding: '8px 10px', border: '1px solid #E2E8F0', borderRadius: 10, fontSize: 13 }} />
          {(dateFrom || dateTo) && <button onClick={() => { setDateFrom(''); setDateTo(''); }} style={{ padding: '8px 12px', background: '#FEE2E2', color: '#DC2626', border: 'none', borderRadius: 10, fontWeight: 700, cursor: 'pointer', fontSize: 12 }}>✕ Reset</button>}
        </div>
      )}

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
