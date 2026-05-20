import React, { useState, useEffect } from 'react';
import { PackageCheck, Plus, X, ChevronRight, Clock, CheckCircle, Wallet, Trash2 } from 'lucide-react';
import api from '../api';
import { useDemoBlock, useAuth } from '../AuthContext';

const fmt = (n) => Number(n || 0).toLocaleString('id-ID');

const STATUS_CONFIG = {
  BOOKED:    { label: '📋 Dipesan', color: '#F59E0B', bg: '#FEF3C7', border: '#FDE68A' },
  DP_PAID:   { label: '💳 DP Dibayar', color: '#3B82F6', bg: '#EFF6FF', border: '#BFDBFE' },
  COMPLETED: { label: '✅ Lunas', color: '#10B981', bg: '#ECFDF5', border: '#A7F3D0' },
};

const DEMO_ORDERS = [
  { id: 'd1', receiptNumber: 'PO-DEMO-001', customerName: 'Catering Bu Dewi (Demo)', total: 1500000, dpAmount: 500000, orderStatus: 'DP_PAID', deliveryDate: new Date(Date.now() + 2 * 86400000).toISOString(), notes: 'Nasi kotak 100 box, antar jam 10 pagi', items: [{ product: { name: 'Nasi Kotak Spesial (Demo)' }, quantity: 100, price: 15000 }] },
  { id: 'd2', receiptNumber: 'PO-DEMO-002', customerName: 'Arisan RT 05 (Demo)', total: 750000, dpAmount: 0, orderStatus: 'BOOKED', deliveryDate: new Date(Date.now() + 5 * 86400000).toISOString(), notes: 'Kue ulang tahun 2 tier', items: [{ product: { name: 'Kue Ulang Tahun (Demo)' }, quantity: 2, price: 375000 }] },
];

export default function Pesanan() {
  const { showDemoBlock, isDemo } = useDemoBlock();
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modal, setModal] = useState(null); // null | 'create' | order object
  const [dpModal, setDpModal] = useState(null);
  const [form, setForm] = useState({ customerName: '', notes: '', deliveryDate: '', dpAmount: 0, items: [] });
  const [newItem, setNewItem] = useState({ productId: '', quantity: 1, price: '' });

  const fetchOrders = async () => {
    if (isDemo) { setOrders(DEMO_ORDERS); return; }
    try {
      setLoading(true);
      const r = await api.get('/pre-orders');
      setOrders(r.data);
    } catch { } finally { setLoading(false); }
  };

  const fetchCustomers = async () => {
    try { const r = await api.get('/customers/list'); setCustomers(r.data); } catch { }
  };

  const fetchProducts = async () => {
    try { const r = await api.get('/products'); setProducts(r.data); } catch { }
  };

  useEffect(() => { fetchOrders(); fetchCustomers(); fetchProducts(); }, []);

  const openCreate = () => {
    if (isDemo) { showDemoBlock('Membuat Pre-Order hanya tersedia di akun berbayar.'); return; }
    setForm({ customerName: '', notes: '', deliveryDate: '', dpAmount: 0, items: [] });
    setModal('create');
  };

  const addItem = () => {
    if (!newItem.productId || !newItem.price) return;
    const product = products.find(p => p.id === Number(newItem.productId));
    if (!product) return;
    setForm(f => ({
      ...f,
      items: [...f.items, { productId: Number(newItem.productId), productName: product.name, quantity: Number(newItem.quantity), price: Number(newItem.price) }]
    }));
    setNewItem({ productId: '', quantity: 1, price: '' });
  };

  const removeItem = (idx) => setForm(f => ({ ...f, items: f.items.filter((_, i) => i !== idx) }));

  const formTotal = form.items.reduce((s, i) => s + i.price * i.quantity, 0);

  const handleCreate = async () => {
    if (!form.items.length) return alert('Tambahkan minimal 1 item pesanan!');
    if (!form.customerName) return alert('Nama pemesan wajib diisi!');
    try {
      await api.post('/transactions', {
        items: form.items.map(i => ({ productId: i.productId, quantity: i.quantity, price: i.price, discount: 0 })),
        total: formTotal,
        discount: 0,
        paymentMethod: 'CASH',
        status: 'PENDING',
        type: 'PRE_ORDER',
        orderStatus: 'BOOKED',
        dpAmount: Number(form.dpAmount || 0),
        deliveryDate: form.deliveryDate || null,
        customerName: form.customerName,
        notes: form.notes,
      });
      setModal(null);
      fetchOrders();
    } catch (err) { alert(err.response?.data?.error || 'Gagal membuat pesanan'); }
  };

  const updateStatus = async (id, orderStatus) => {
    if (isDemo) { showDemoBlock('Update status hanya tersedia di akun berbayar.'); return; }
    try {
      await api.patch(`/pre-orders/${id}/status`, { orderStatus });
      fetchOrders();
    } catch { alert('Gagal update status'); }
  };

  const updateDP = async (id, dpAmount) => {
    if (isDemo) { showDemoBlock('Update DP hanya tersedia di akun berbayar.'); return; }
    try {
      await api.patch(`/pre-orders/${id}/status`, { dpAmount: Number(dpAmount) });
      setDpModal(null);
      fetchOrders();
    } catch { alert('Gagal update DP'); }
  };

  const formatDate = (d) => {
    if (!d) return '–';
    return new Date(d).toLocaleDateString('id-ID', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
  };

  const today = new Date().setHours(0, 0, 0, 0);
  const todayOrders = orders.filter(o => o.orderStatus !== 'COMPLETED' && o.deliveryDate && new Date(o.deliveryDate).setHours(0,0,0,0) === today);

  return (
    <div className="page-container">
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, gap: 12 }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '1.3rem', fontWeight: 900, color: '#1E293B' }}>📅 Pre-Order & Pesanan</h1>
          <div style={{ fontSize: 12, color: '#6B7280', marginTop: 2 }}>Kelola pesanan katering, kue, & pre-order khusus</div>
        </div>
        <button onClick={openCreate} style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '10px 16px', background: 'linear-gradient(135deg,#6366F1,#4F46E5)', color: 'white', border: 'none', borderRadius: 12, fontWeight: 800, fontSize: 13, cursor: 'pointer', boxShadow: '0 4px 12px rgba(99,102,241,0.3)', flexShrink: 0 }}>
          <Plus size={16} /> Pesanan Baru
        </button>
      </div>

      {/* Demo banner */}
      {isDemo && (
        <div style={{ background: '#EFF6FF', border: '1.5px solid #BFDBFE', borderRadius: 12, padding: '10px 14px', marginBottom: 16, fontSize: 13, color: '#1E40AF', fontWeight: 600 }}>
          💡 <b>Mode Demo</b>: Menampilkan contoh pre-order fiktif. Upgrade untuk membuat pesanan nyata.
        </div>
      )}

      {/* Today alert */}
      {todayOrders.length > 0 && (
        <div style={{ background: '#FEF3C7', border: '1.5px solid #FDE68A', borderRadius: 14, padding: '12px 16px', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10 }}>
          <Clock size={18} color="#D97706" />
          <div>
            <div style={{ fontWeight: 800, fontSize: 13, color: '#92400E' }}>⚡ Pengiriman Hari Ini ({todayOrders.length} pesanan)</div>
            <div style={{ fontSize: 12, color: '#78350F' }}>{todayOrders.map(o => o.customerName).join(', ')}</div>
          </div>
        </div>
      )}

      {/* Order list */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: 40, color: '#94A3B8' }}>Memuat...</div>
      ) : orders.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <PackageCheck size={48} color="#E5E7EB" style={{ marginBottom: 12 }} />
          <div style={{ color: '#94A3B8', fontWeight: 600, fontSize: 15 }}>Belum ada pre-order</div>
          <div style={{ color: '#CBD5E1', fontSize: 13 }}>Klik "Pesanan Baru" untuk membuat pesanan katering atau pre-order khusus</div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {orders.map(order => {
            const cfg = STATUS_CONFIG[order.orderStatus] || STATUS_CONFIG.BOOKED;
            const sisa = order.total - (order.dpAmount || 0);
            const isToday = order.deliveryDate && new Date(order.deliveryDate).setHours(0,0,0,0) === today;
            return (
              <div key={order.id} style={{ background: 'white', borderRadius: 18, padding: '16px 18px', boxShadow: `0 2px 12px rgba(0,0,0,0.06)`, border: isToday ? '2px solid #FDE68A' : '1.5px solid #F1F5F9' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                  <div>
                    <div style={{ fontWeight: 900, fontSize: '1rem', color: '#111827' }}>{order.customerName}</div>
                    <div style={{ fontSize: 12, color: '#94A3B8', marginTop: 2 }}>{order.receiptNumber}</div>
                  </div>
                  <span style={{ padding: '4px 12px', borderRadius: 99, fontSize: 12, fontWeight: 700, background: cfg.bg, color: cfg.color, border: `1px solid ${cfg.border}`, flexShrink: 0 }}>{cfg.label}</span>
                </div>

                {/* Delivery date */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
                  <Clock size={14} color={isToday ? '#D97706' : '#94A3B8'} />
                  <span style={{ fontSize: 13, color: isToday ? '#92400E' : '#6B7280', fontWeight: isToday ? 700 : 500 }}>
                    {isToday ? '⚡ Hari ini — ' : ''}{formatDate(order.deliveryDate)}
                  </span>
                </div>

                {/* Items */}
                {order.items?.slice(0, 2).map((item, i) => (
                  <div key={i} style={{ fontSize: 12, color: '#64748B', display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
                    <span>{item.product?.name} × {item.quantity}</span>
                    <span>Rp {fmt(item.price * item.quantity)}</span>
                  </div>
                ))}
                {order.items?.length > 2 && <div style={{ fontSize: 11, color: '#94A3B8' }}>+{order.items.length - 2} item lagi...</div>}

                {/* Keuangan */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginTop: 12, padding: '10px 0', borderTop: '1px solid #F1F5F9' }}>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 10, color: '#94A3B8', fontWeight: 600 }}>TOTAL</div>
                    <div style={{ fontSize: 13, fontWeight: 800, color: '#111827' }}>Rp {fmt(order.total)}</div>
                  </div>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 10, color: '#94A3B8', fontWeight: 600 }}>DP</div>
                    <div style={{ fontSize: 13, fontWeight: 800, color: '#3B82F6' }}>Rp {fmt(order.dpAmount)}</div>
                  </div>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 10, color: '#94A3B8', fontWeight: 600 }}>SISA</div>
                    <div style={{ fontSize: 13, fontWeight: 800, color: sisa > 0 ? '#EF4444' : '#10B981' }}>Rp {fmt(sisa)}</div>
                  </div>
                </div>

                {/* Notes */}
                {order.notes && <div style={{ fontSize: 12, color: '#6B7280', marginTop: 6, fontStyle: 'italic', background: '#F8FAFC', padding: '6px 10px', borderRadius: 8 }}>"{order.notes}"</div>}

                {/* Actions */}
                {order.orderStatus !== 'COMPLETED' && (
                  <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                    <button onClick={() => setDpModal(order)} style={{ flex: 1, padding: '9px 0', borderRadius: 10, border: '1.5px solid #BFDBFE', background: '#EFF6FF', color: '#1D4ED8', fontWeight: 700, fontSize: 12, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 5 }}>
                      <Wallet size={14} /> Update DP
                    </button>
                    {order.orderStatus === 'BOOKED' && (
                      <button onClick={() => updateStatus(order.id, 'DP_PAID')} style={{ flex: 1, padding: '9px 0', borderRadius: 10, border: 'none', background: '#3B82F6', color: 'white', fontWeight: 700, fontSize: 12, cursor: 'pointer' }}>
                        DP Masuk
                      </button>
                    )}
                    <button onClick={() => updateStatus(order.id, 'COMPLETED')} style={{ flex: 1, padding: '9px 0', borderRadius: 10, border: 'none', background: 'linear-gradient(135deg,#10B981,#059669)', color: 'white', fontWeight: 700, fontSize: 12, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 5 }}>
                      <CheckCircle size={14} /> Lunas
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* DP Update Modal */}
      {dpModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
          <div style={{ background: 'white', borderRadius: 20, padding: '24px 20px', width: '100%', maxWidth: 360 }}>
            <div style={{ fontWeight: 900, fontSize: '1rem', marginBottom: 4 }}>💳 Update DP</div>
            <div style={{ fontSize: 13, color: '#64748B', marginBottom: 16 }}>{dpModal.customerName} — Total: Rp {fmt(dpModal.total)}</div>
            <label style={{ fontWeight: 700, fontSize: 13 }}>Nominal DP Baru (Rp)</label>
            <input type="number" defaultValue={dpModal.dpAmount || 0} id="dp-input" style={{ width: '100%', padding: '11px 14px', borderRadius: 10, border: '1.5px solid #E5E7EB', fontSize: 15, outline: 'none', boxSizing: 'border-box', marginTop: 6, marginBottom: 14 }} />
            <button onClick={() => updateDP(dpModal.id, document.getElementById('dp-input').value)} style={{ width: '100%', padding: '12px', borderRadius: 12, border: 'none', background: 'linear-gradient(135deg,#3B82F6,#1D4ED8)', color: 'white', fontWeight: 800, fontSize: 14, cursor: 'pointer', marginBottom: 8 }}>Simpan</button>
            <button onClick={() => setDpModal(null)} style={{ width: '100%', padding: '11px', borderRadius: 12, border: '1.5px solid #E5E7EB', background: 'white', color: '#64748B', fontWeight: 700, fontSize: 13, cursor: 'pointer' }}>Batal</button>
          </div>
        </div>
      )}

      {/* Create Modal */}
      {modal === 'create' && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
          <div style={{ background: 'white', borderRadius: 20, padding: '24px 20px', width: '100%', maxWidth: 440, maxHeight: '90dvh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
              <div style={{ fontWeight: 900, fontSize: '1.1rem' }}>📋 Pesanan Baru</div>
              <button onClick={() => setModal(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#94A3B8' }}><X size={20} /></button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div>
                <label style={{ fontWeight: 700, fontSize: 13, display: 'block', marginBottom: 4 }}>Nama Pemesan *</label>
                <input value={form.customerName} onChange={e => setForm(f => ({ ...f, customerName: e.target.value }))} placeholder="Nama pelanggan / instansi" style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E5E7EB', fontSize: 14, outline: 'none', boxSizing: 'border-box' }} />
              </div>
              <div>
                <label style={{ fontWeight: 700, fontSize: 13, display: 'block', marginBottom: 4 }}>Tanggal Pengiriman</label>
                <input type="datetime-local" value={form.deliveryDate} onChange={e => setForm(f => ({ ...f, deliveryDate: e.target.value }))} style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E5E7EB', fontSize: 14, outline: 'none', boxSizing: 'border-box' }} />
              </div>
              <div>
                <label style={{ fontWeight: 700, fontSize: 13, display: 'block', marginBottom: 4 }}>Catatan</label>
                <textarea value={form.notes} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} rows={2} placeholder="Instruksi khusus, jumlah porsi, dll" style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E5E7EB', fontSize: 14, outline: 'none', boxSizing: 'border-box', resize: 'vertical' }} />
              </div>
              <div>
                <label style={{ fontWeight: 700, fontSize: 13, display: 'block', marginBottom: 4 }}>DP Awal (Rp)</label>
                <input type="number" value={form.dpAmount} onChange={e => setForm(f => ({ ...f, dpAmount: e.target.value }))} placeholder="0" style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E5E7EB', fontSize: 14, outline: 'none', boxSizing: 'border-box' }} />
              </div>

              <div style={{ background: '#F8FAFC', borderRadius: 14, padding: '14px' }}>
                <div style={{ fontWeight: 800, fontSize: 13, marginBottom: 10 }}>🛒 Item Pesanan</div>
                {form.items.map((item, i) => (
                  <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 13, marginBottom: 6, background: 'white', padding: '8px 10px', borderRadius: 8 }}>
                    <span>{item.productName} × {item.quantity}</span>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <span style={{ color: '#4F46E5', fontWeight: 700 }}>Rp {fmt(item.price * item.quantity)}</span>
                      <button onClick={() => removeItem(i)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#EF4444', padding: 0 }}><Trash2 size={14} /></button>
                    </div>
                  </div>
                ))}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr auto auto', gap: 6, marginTop: 8 }}>
                  <select value={newItem.productId} onChange={e => { const p = products.find(pr => pr.id === Number(e.target.value)); setNewItem(ni => ({ ...ni, productId: e.target.value, price: p?.price || '' })); }} style={{ padding: '8px 10px', borderRadius: 8, border: '1.5px solid #E5E7EB', fontSize: 13, outline: 'none' }}>
                    <option value="">Pilih produk...</option>
                    {products.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                  </select>
                  <input type="number" min="1" value={newItem.quantity} onChange={e => setNewItem(ni => ({ ...ni, quantity: e.target.value }))} style={{ width: 52, padding: '8px 6px', borderRadius: 8, border: '1.5px solid #E5E7EB', fontSize: 13, outline: 'none', textAlign: 'center' }} />
                  <button onClick={addItem} style={{ padding: '8px 12px', borderRadius: 8, border: 'none', background: '#4F46E5', color: 'white', fontWeight: 700, fontSize: 13, cursor: 'pointer' }}>+ Add</button>
                </div>
              </div>

              {form.items.length > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 900, fontSize: 15, color: '#111827', padding: '8px 0' }}>
                  <span>Total Pesanan</span><span style={{ color: '#4F46E5' }}>Rp {fmt(formTotal)}</span>
                </div>
              )}
            </div>

            <button onClick={handleCreate} style={{ width: '100%', padding: '13px', borderRadius: 14, border: 'none', background: 'linear-gradient(135deg,#6366F1,#4F46E5)', color: 'white', fontWeight: 800, fontSize: 14, cursor: 'pointer', marginTop: 16, boxShadow: '0 4px 12px rgba(99,102,241,0.3)' }}>
              Simpan Pesanan
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
