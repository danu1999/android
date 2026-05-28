import React, { useState, useEffect } from 'react';
import { Truck, Plus, X, CheckCircle, Trash2, Package, ShoppingCart, Edit2 } from 'lucide-react';
import api from '../api';
import { useDemoBlock } from '../AuthContext';

const fmt = (n) => Number(n || 0).toLocaleString('id-ID');

const DEMO_SUPPLIERS = [
  { id: 's1', name: 'CV Maju Bersama (Demo)', phone: '081234567890', address: 'Jl. Raya No. 12, Bandung', notes: 'Supplier utama bahan baku', purchaseOrders: [] },
  { id: 's2', name: 'Toko Grosir Pak Haji (Demo)', phone: '081298765432', address: 'Pasar Induk Timur Blok C-7', notes: 'Sayuran & bumbu segar', purchaseOrders: [] },
];

const DEMO_POS = [
  { id: 'po1', supplier: { name: 'CV Maju Bersama (Demo)' }, date: new Date(Date.now() - 3 * 86400000).toISOString(), status: 'RECEIVED', total: 1200000, notes: 'Restok bahan baku bulan ini', items: [{ product: { name: 'Tepung Terigu (Demo)' }, quantity: 50, costPrice: 12000 }, { product: { name: 'Gula Pasir (Demo)' }, quantity: 30, costPrice: 15000 }] },
  { id: 'po2', supplier: { name: 'Toko Grosir Pak Haji (Demo)' }, date: new Date(Date.now() - 1 * 86400000).toISOString(), status: 'ORDERED', total: 450000, notes: '', items: [{ product: { name: 'Bawang Merah (Demo)' }, quantity: 20, costPrice: 22500 }] },
];

const PO_STATUS = {
  DRAFT:    { label: '📝 Draft', color: '#6B7280', bg: '#F9FAFB', border: '#E5E7EB' },
  ORDERED:  { label: '📦 Dipesan', color: '#D97706', bg: '#FEF3C7', border: '#FDE68A' },
  RECEIVED: { label: '✅ Diterima', color: '#059669', bg: '#ECFDF5', border: '#A7F3D0' },
};

export default function Supplier() {
  const { showDemoBlock, isDemo } = useDemoBlock();
  const [tab, setTab] = useState('supplier'); // 'supplier' | 'po'
  const [suppliers, setSuppliers] = useState([]);
  const [pos, setPos] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [suppModal, setSuppModal] = useState(null); // null | 'create' | supplier obj
  const [poModal, setPoModal] = useState(null); // null | 'create'
  const [suppForm, setSuppForm] = useState({ name: '', phone: '', address: '', notes: '' });
  const [poForm, setPoForm] = useState({ supplierId: '', notes: '', items: [] });
  const [newPOItem, setNewPOItem] = useState({ productId: '', quantity: 1, costPrice: '' });

  const fetchSuppliers = async () => {
    try { const r = await api.get('/suppliers'); setSuppliers(r.data); } catch { }
  };

  const fetchPOs = async () => {
    try { const r = await api.get('/purchase-orders'); setPos(r.data); } catch { }
  };

  const fetchProducts = async () => {
    try { const r = await api.get('/products'); setProducts(r.data); } catch { }
  };

  useEffect(() => { fetchSuppliers(); fetchPOs(); fetchProducts(); }, []);

  // ── Supplier CRUD ───────────────────────────────────────────
  const openCreateSupplier = () => {
    setSuppForm({ name: '', phone: '', address: '', notes: '' });
    setSuppModal('create');
  };

  const openEditSupplier = (s) => {
    setSuppForm({ name: s.name, phone: s.phone || '', address: s.address || '', notes: s.notes || '' });
    setSuppModal(s);
  };

  const saveSupplier = async () => {
    if (!suppForm.name) return alert('Nama supplier wajib diisi');
    try {
      if (suppModal === 'create') {
        await api.post('/suppliers', suppForm);
      } else {
        await api.put(`/suppliers/${suppModal.id}`, suppForm);
      }
      setSuppModal(null); fetchSuppliers();
    } catch (err) { alert(err.response?.data?.error || 'Gagal simpan supplier'); }
  };

  const deleteSupplier = async (id) => {
    if (!window.confirm('Yakin hapus supplier ini?')) return;
    try { await api.delete(`/suppliers/${id}`); fetchSuppliers(); } catch { alert('Gagal hapus'); }
  };

  // ── Purchase Order ──────────────────────────────────────────
  const openCreatePO = () => {
    setPoForm({ supplierId: '', notes: '', items: [] });
    setNewPOItem({ productId: '', quantity: 1, costPrice: '' });
    setPoModal('create');
  };

  const addPOItem = () => {
    if (!newPOItem.productId || !newPOItem.costPrice) return;
    const product = products.find(p => p.id === Number(newPOItem.productId));
    if (!product) return;
    setPoForm(f => ({
      ...f,
      items: [...f.items, { productId: Number(newPOItem.productId), productName: product.name, quantity: Number(newPOItem.quantity), costPrice: Number(newPOItem.costPrice) }]
    }));
    setNewPOItem({ productId: '', quantity: 1, costPrice: '' });
  };

  const removePOItem = (idx) => setPoForm(f => ({ ...f, items: f.items.filter((_, i) => i !== idx) }));

  const poFormTotal = poForm.items.reduce((s, i) => s + i.costPrice * i.quantity, 0);

  const savePO = async () => {
    if (!poForm.supplierId) return alert('Pilih supplier terlebih dahulu');
    if (!poForm.items.length) return alert('Tambahkan minimal 1 item');
    try {
      await api.post('/purchase-orders', {
        supplierId: Number(poForm.supplierId),
        notes: poForm.notes,
        items: poForm.items.map(i => ({ productId: i.productId, quantity: i.quantity, costPrice: i.costPrice }))
      });
      setPoModal(null); fetchPOs();
    } catch (err) { alert(err.response?.data?.error || 'Gagal buat PO'); }
  };

  const receivePO = async (po) => {
    if (!window.confirm(`Konfirmasi terima barang dari ${po.supplier?.name}?\n\nStok produk akan otomatis bertambah dan hutang ke supplier akan dicatat ke Keuangan.`)) return;
    try {
      await api.post(`/purchase-orders/${po.id}/receive`);
      fetchPOs(); fetchProducts();
      alert('✅ Barang diterima! Stok produk telah diperbarui dan hutang telah dicatat ke Keuangan.');
    } catch (err) { alert(err.response?.data?.error || 'Gagal konfirmasi'); }
  };

  return (
    <div className="page-container">
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '1.3rem', fontWeight: 900, color: '#1E293B' }}>🏭 Supplier & Pembelian</h1>
          <div style={{ fontSize: 12, color: '#6B7280', marginTop: 2 }}>Kelola supplier & purchase order (PO) barang</div>
        </div>
        <button
          onClick={tab === 'supplier' ? openCreateSupplier : openCreatePO}
          style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '10px 16px', background: 'linear-gradient(135deg,#6366F1,#4F46E5)', color: 'white', border: 'none', borderRadius: 12, fontWeight: 800, fontSize: 13, cursor: 'pointer', boxShadow: '0 4px 12px rgba(99,102,241,0.3)', flexShrink: 0 }}
        >
          <Plus size={16} /> {tab === 'supplier' ? 'Tambah Supplier' : 'Buat PO'}
        </button>
      </div>

      {/* Demo banner */}
      {isDemo && (
        <div style={{ background: '#EFF6FF', border: '1.5px solid #BFDBFE', borderRadius: 12, padding: '10px 14px', marginBottom: 16, fontSize: 13, color: '#1E40AF', fontWeight: 600 }}>
          💡 <b>Mode Demo</b>: Menampilkan data supplier & PO fiktif.
        </div>
      )}

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4, marginBottom: 20, scrollbarWidth: 'none' }}>
        {[{ id: 'supplier', label: '🏭 Supplier' }, { id: 'po', label: '📦 Purchase Order' }].map(t => (
          <button key={t.id} onClick={() => setTab(t.id)} style={{ padding: '9px 18px', borderRadius: 99, border: 'none', cursor: 'pointer', fontWeight: 700, fontSize: 13, background: tab === t.id ? 'linear-gradient(135deg,#6366F1,#4F46E5)' : '#F1F5F9', color: tab === t.id ? 'white' : '#64748B', boxShadow: tab === t.id ? '0 4px 12px rgba(99,102,241,0.3)' : 'none' }}>
            {t.label}
          </button>
        ))}
      </div>

      {/* Supplier list */}
      {tab === 'supplier' && (
        suppliers.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 48 }}>
            <Truck size={48} color="#E5E7EB" style={{ marginBottom: 12 }} />
            <div style={{ color: '#94A3B8', fontWeight: 600 }}>Belum ada supplier</div>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {suppliers.map(s => (
              <div key={s.id} style={{ background: 'white', borderRadius: 16, padding: '14px 16px', boxShadow: '0 2px 10px rgba(0,0,0,0.05)', border: '1.5px solid #F1F5F9', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 900, fontSize: '0.95rem', color: '#111827' }}>{s.name}</div>
                  {s.phone && <div style={{ fontSize: 12, color: '#64748B', marginTop: 2 }}>📞 {s.phone}</div>}
                  {s.address && <div style={{ fontSize: 12, color: '#64748B' }}>📍 {s.address}</div>}
                  {s.notes && <div style={{ fontSize: 11, color: '#94A3B8', marginTop: 4, fontStyle: 'italic' }}>"{s.notes}"</div>}
                </div>
                <div style={{ display: 'flex', gap: 6, flexShrink: 0, marginLeft: 12 }}>
                  <button onClick={() => openEditSupplier(s)} style={{ padding: '7px 10px', borderRadius: 8, border: '1.5px solid #E5E7EB', background: 'white', color: '#4F46E5', cursor: 'pointer' }}><Edit2 size={14} /></button>
                  <button onClick={() => deleteSupplier(s.id)} style={{ padding: '7px 10px', borderRadius: 8, border: '1.5px solid #FEE2E2', background: '#FEF2F2', color: '#EF4444', cursor: 'pointer' }}><Trash2 size={14} /></button>
                </div>
              </div>
            ))}
          </div>
        )
      )}

      {/* PO list */}
      {tab === 'po' && (
        pos.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 48 }}>
            <Package size={48} color="#E5E7EB" style={{ marginBottom: 12 }} />
            <div style={{ color: '#94A3B8', fontWeight: 600 }}>Belum ada Purchase Order</div>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {pos.map(po => {
              const cfg = PO_STATUS[po.status] || PO_STATUS.DRAFT;
              return (
                <div key={po.id} style={{ background: 'white', borderRadius: 18, padding: '16px 18px', boxShadow: '0 2px 12px rgba(0,0,0,0.06)', border: '1.5px solid #F1F5F9' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                    <div>
                      <div style={{ fontWeight: 900, fontSize: '0.95rem', color: '#111827' }}>{po.supplier?.name}</div>
                      <div style={{ fontSize: 12, color: '#94A3B8' }}>{new Date(po.date).toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}</div>
                    </div>
                    <span style={{ padding: '4px 12px', borderRadius: 99, fontSize: 12, fontWeight: 700, background: cfg.bg, color: cfg.color, border: `1px solid ${cfg.border}` }}>{cfg.label}</span>
                  </div>

                  {po.items?.map((item, i) => (
                    <div key={i} style={{ fontSize: 12, color: '#64748B', display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
                      <span>{item.product?.name} × {item.quantity}</span>
                      <span>Rp {fmt(item.costPrice * item.quantity)}</span>
                    </div>
                  ))}

                  <div style={{ fontWeight: 800, color: '#4F46E5', fontSize: 14, borderTop: '1px solid #F1F5F9', marginTop: 8, paddingTop: 8, display: 'flex', justifyContent: 'space-between' }}>
                    <span>Total</span><span>Rp {fmt(po.total)}</span>
                  </div>

                  {po.notes && <div style={{ fontSize: 11, color: '#94A3B8', fontStyle: 'italic', marginTop: 6 }}>"{po.notes}"</div>}

                  {po.status !== 'RECEIVED' && (
                    <button onClick={() => receivePO(po)} style={{ width: '100%', marginTop: 12, padding: '11px', borderRadius: 12, border: 'none', background: 'linear-gradient(135deg,#10B981,#059669)', color: 'white', fontWeight: 800, fontSize: 13, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
                      <CheckCircle size={15} /> Konfirmasi Terima Barang
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )
      )}

      {/* Supplier Modal */}
      {suppModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
          <div style={{ background: 'white', borderRadius: 20, padding: '24px 20px', width: '100%', maxWidth: 400 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
              <div style={{ fontWeight: 900, fontSize: '1rem' }}>{suppModal === 'create' ? '➕ Tambah Supplier' : '✏️ Edit Supplier'}</div>
              <button onClick={() => setSuppModal(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#94A3B8' }}><X size={20} /></button>
            </div>
            {[{ label: 'Nama Supplier *', key: 'name', placeholder: 'CV Maju Jaya' }, { label: 'No. HP / WhatsApp', key: 'phone', placeholder: '08123456789' }, { label: 'Alamat', key: 'address', placeholder: 'Jl. Pasar No. 12...' }, { label: 'Catatan', key: 'notes', placeholder: 'Supplier bahan baku, dll' }].map(f => (
              <div key={f.key} style={{ marginBottom: 12 }}>
                <label style={{ fontWeight: 700, fontSize: 13, display: 'block', marginBottom: 4 }}>{f.label}</label>
                <input value={suppForm[f.key]} onChange={e => setSuppForm(sf => ({ ...sf, [f.key]: e.target.value }))} placeholder={f.placeholder} style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E5E7EB', fontSize: 14, outline: 'none', boxSizing: 'border-box' }} />
              </div>
            ))}
            <button onClick={saveSupplier} style={{ width: '100%', padding: '12px', borderRadius: 12, border: 'none', background: 'linear-gradient(135deg,#6366F1,#4F46E5)', color: 'white', fontWeight: 800, fontSize: 14, cursor: 'pointer', marginTop: 4 }}>Simpan</button>
          </div>
        </div>
      )}

      {/* PO Create Modal */}
      {poModal === 'create' && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
          <div style={{ background: 'white', borderRadius: 20, padding: '24px 20px', width: '100%', maxWidth: 440, maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
              <div style={{ fontWeight: 900, fontSize: '1rem' }}>📦 Buat Purchase Order</div>
              <button onClick={() => setPoModal(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#94A3B8' }}><X size={20} /></button>
            </div>

            <div style={{ marginBottom: 12 }}>
              <label style={{ fontWeight: 700, fontSize: 13, display: 'block', marginBottom: 4 }}>Supplier *</label>
              <select value={poForm.supplierId} onChange={e => setPoForm(f => ({ ...f, supplierId: e.target.value }))} style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E5E7EB', fontSize: 14, outline: 'none' }}>
                <option value="">Pilih supplier...</option>
                {suppliers.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <div style={{ marginBottom: 12 }}>
              <label style={{ fontWeight: 700, fontSize: 13, display: 'block', marginBottom: 4 }}>Catatan</label>
              <textarea value={poForm.notes} onChange={e => setPoForm(f => ({ ...f, notes: e.target.value }))} rows={2} placeholder="Restok bahan baku, dll" style={{ width: '100%', padding: '10px 12px', borderRadius: 10, border: '1.5px solid #E5E7EB', fontSize: 14, outline: 'none', boxSizing: 'border-box', resize: 'vertical' }} />
            </div>

            <div style={{ background: '#F8FAFC', borderRadius: 14, padding: 14 }}>
              <div style={{ fontWeight: 800, fontSize: 13, marginBottom: 10 }}>🧾 Item PO</div>
              {poForm.items.map((item, i) => (
                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 13, marginBottom: 6, background: 'white', padding: '8px 10px', borderRadius: 8 }}>
                  <span>{item.productName} × {item.quantity}</span>
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <span style={{ color: '#4F46E5', fontWeight: 700 }}>Rp {fmt(item.costPrice * item.quantity)}</span>
                    <button onClick={() => removePOItem(i)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#EF4444', padding: 0 }}><Trash2 size={14} /></button>
                  </div>
                </div>
              ))}
              <div className="po-item-grid">
                <select value={newPOItem.productId} onChange={e => { const p = products.find(pr => pr.id === Number(e.target.value)); setNewPOItem(ni => ({ ...ni, productId: e.target.value, costPrice: p?.costPrice || '' })); }} style={{ padding: '8px', borderRadius: 8, border: '1.5px solid #E5E7EB', fontSize: 12, outline: 'none' }}>
                  <option value="">Produk...</option>
                  {products.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                </select>
                <input type="number" min="1" value={newPOItem.quantity} onChange={e => setNewPOItem(ni => ({ ...ni, quantity: e.target.value }))} style={{ padding: '8px 4px', borderRadius: 8, border: '1.5px solid #E5E7EB', fontSize: 12, outline: 'none', textAlign: 'center', width: '100%', boxSizing: 'border-box' }} />
                <input type="number" value={newPOItem.costPrice} onChange={e => setNewPOItem(ni => ({ ...ni, costPrice: e.target.value }))} placeholder="Harga beli" style={{ padding: '8px', borderRadius: 8, border: '1.5px solid #E5E7EB', fontSize: 12, outline: 'none', width: '100%', boxSizing: 'border-box' }} />
                <button onClick={addPOItem} style={{ padding: '8px 10px', borderRadius: 8, border: 'none', background: '#4F46E5', color: 'white', fontWeight: 700, fontSize: 12, cursor: 'pointer', whiteSpace: 'nowrap' }}>+ Add</button>
              </div>
            </div>

            {poForm.items.length > 0 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 900, fontSize: 15, color: '#111827', padding: '10px 0' }}>
                <span>Total PO</span><span style={{ color: '#4F46E5' }}>Rp {fmt(poFormTotal)}</span>
              </div>
            )}

            <button onClick={savePO} style={{ width: '100%', padding: '13px', borderRadius: 14, border: 'none', background: 'linear-gradient(135deg,#6366F1,#4F46E5)', color: 'white', fontWeight: 800, fontSize: 14, cursor: 'pointer', marginTop: 10, boxShadow: '0 4px 12px rgba(99,102,241,0.3)' }}>
              Simpan Purchase Order
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
