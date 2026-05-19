import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Search, AlertTriangle, Eye, Tag } from 'lucide-react';
import api from '../api';
import { useAuth, useIsAdmin, useDemoBlock, DEMO_LIMITS } from '../AuthContext';


const EMPTY_VARIANT = { name: '', stock: '', price: '', costPrice: '' };

const EMPTY_FORM = {
  id: null, name: '', price: '', costPrice: '',
  stock: '', unit: 'pcs', image: '',
  wholesaleEnabled: false,
  wholesalePrices: [
    { minQty: '', price: '' },
    { minQty: '', price: '' },
    { minQty: '', price: '' },
    { minQty: '', price: '' },
    { minQty: '', price: '' },
  ],
  variantEnabled: false,
  variants: [{ ...EMPTY_VARIANT }],
};

export default function Katalog() {
  const isAdmin = useIsAdmin();
  const { showDemoBlock, isDemo } = useDemoBlock();
  const [products, setProducts] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isViewOnly, setIsViewOnly] = useState(false); // modal view-only untuk kasir
  const [formData, setFormData] = useState(EMPTY_FORM);
  const LOW_STOCK_THRESHOLD = 5;

  const fetchProducts = async () => {
    try {
      const res = await api.get('/products');
      setProducts(res.data);
    } catch (err) {
      console.error('Failed to fetch products', err);
    }
  };

  useEffect(() => { fetchProducts(); }, []);

  const handleInputChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleWholesaleChange = (index, field, value) => {
    const updated = [...formData.wholesalePrices];
    updated[index] = { ...updated[index], [field]: value };
    setFormData({ ...formData, wholesalePrices: updated });
  };

  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = (event) => {
      const img = new Image();
      img.src = event.target.result;
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const MAX_WIDTH = 400;
        const scaleSize = MAX_WIDTH / img.width;
        canvas.width = MAX_WIDTH;
        canvas.height = img.height * scaleSize;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        const compressedBase64 = canvas.toDataURL('image/jpeg', 0.8);
        setFormData(prev => ({ ...prev, image: compressedBase64 }));
      };
    };
  };

  const handleOpenModal = (product = null, viewOnly = false) => {
    if (product) {
      let wholesalePrices = EMPTY_FORM.wholesalePrices;
      if (product.wholesalePrices) {
        try {
          const parsed = JSON.parse(product.wholesalePrices);
          wholesalePrices = [...parsed, ...EMPTY_FORM.wholesalePrices].slice(0, 5);
        } catch (_) {}
      }
      let variants = [{ ...EMPTY_VARIANT }];
      let variantEnabled = false;
      if (product.variants) {
        try {
          const parsed = JSON.parse(product.variants);
          if (parsed && parsed.length > 0) {
            variants = parsed;
            variantEnabled = true;
          }
        } catch (_) {}
      }
      setFormData({ ...product, wholesalePrices, variants, variantEnabled });
    } else {
      setFormData({ ...EMPTY_FORM });
    }
    setIsViewOnly(viewOnly);
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (isDemo && !formData.id && products.length >= DEMO_LIMITS.PRODUCTS) {
      showDemoBlock(`Batas maksimal ${DEMO_LIMITS.PRODUCTS} produk untuk akun demo. Upgrade untuk produk tidak terbatas!`);
      return;
    }

    try {
      // Filter hanya wholesale yang terisi
      const filledWholesale = formData.wholesalePrices.filter(
        w => w.minQty !== '' && w.price !== ''
      ).map(w => ({ minQty: Number(w.minQty), price: Number(w.price) }));

      // Filter varian yang terisi (minimal nama)
      const filledVariants = formData.variantEnabled
        ? formData.variants
            .filter(v => v.name.trim() !== '')
            .map(v => ({
              name: v.name.trim(),
              stock: v.stock !== '' ? Number(v.stock) : null,
              price: v.price !== '' ? Number(v.price) : null,
              costPrice: v.costPrice !== '' ? Number(v.costPrice) : null,
            }))
        : [];

      const payload = {
        ...formData,
        wholesalePrices: formData.wholesaleEnabled && filledWholesale.length > 0 ? filledWholesale : null,
        variants: filledVariants,
      };

      if (formData.id) {
        await api.put(`/products/${formData.id}`, payload);
      } else {
        await api.post('/products', payload);
      }
      setIsModalOpen(false);
      fetchProducts();
    } catch (err) {
      console.error('Failed to save product', err);
      alert('Gagal menyimpan produk.');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Yakin ingin menghapus produk ini?')) {
      try {
        await api.delete(`/products/${id}`);
        setIsModalOpen(false);
        fetchProducts();
      } catch (err) {
        alert(err.response?.data?.error || 'Gagal menghapus produk. Produk mungkin masih digunakan dalam transaksi.');
      }
    }
  };

  const filteredProducts = products.filter(p =>
    p.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const getMargin = (price, cost) => {
    if (!cost || cost === 0) return null;
    return Math.round(((price - cost) / price) * 100);
  };

  return (
    <div className="page-container">

      {/* ── Header ── */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16, gap: 12 }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '1.3rem', fontWeight: 900, color: '#1E293B' }}>Katalog Produk</h1>
          {!isAdmin && (
            <p style={{ margin: '3px 0 0', fontSize: '0.78rem', color: '#6B7280' }}>
              👁️ Mode lihat saja — Hubungi Admin untuk mengubah produk
            </p>
          )}
        </div>
        {isAdmin && (
          <button
            onClick={() => handleOpenModal()}
            style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '10px 16px', background: 'linear-gradient(135deg,#6366F1,#4F46E5)', color: 'white', border: 'none', borderRadius: 14, fontWeight: 800, fontSize: 14, cursor: 'pointer', whiteSpace: 'nowrap', boxShadow: '0 4px 12px rgba(99,102,241,0.35)', flexShrink: 0 }}
          >
            <Plus size={17} /> Tambah
          </button>
        )}
      </div>

      {/* ── Alert Banners ── */}
      {products.filter(p => p.stock <= LOW_STOCK_THRESHOLD && p.stock > 0).length > 0 && (
        <div style={{ background: '#FEF9C3', border: '1px solid #FDE047', borderRadius: 12, padding: '10px 14px', marginBottom: 10, display: 'flex', alignItems: 'flex-start', gap: 8 }}>
          <AlertTriangle size={17} color="#CA8A04" style={{ flexShrink: 0, marginTop: 2 }} />
          <span style={{ fontWeight: 600, color: '#854D0E', fontSize: 13, lineHeight: 1.5 }}>
            ⚠️ Stok menipis: {products.filter(p => p.stock <= LOW_STOCK_THRESHOLD && p.stock > 0).map(p => `${p.name} (${p.stock})`).join(', ')}
          </span>
        </div>
      )}
      {products.filter(p => p.stock === 0).length > 0 && (
        <div style={{ background: '#FEE2E2', border: '1px solid #FCA5A5', borderRadius: 12, padding: '10px 14px', marginBottom: 10, display: 'flex', alignItems: 'flex-start', gap: 8 }}>
          <AlertTriangle size={17} color="#DC2626" style={{ flexShrink: 0, marginTop: 2 }} />
          <span style={{ fontWeight: 600, color: '#991B1B', fontSize: 13, lineHeight: 1.5 }}>
            🚫 Stok habis: {products.filter(p => p.stock === 0).map(p => p.name).join(', ')}
          </span>
        </div>
      )}

      {/* ── Search ── */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, background: 'white', border: '1.5px solid #E5E7EB', borderRadius: 14, padding: '10px 14px', marginBottom: 16, boxShadow: '0 1px 4px rgba(0,0,0,0.05)' }}>
        <Search size={18} color="#94A3B8" />
        <input
          type="text"
          placeholder="Cari produk..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          style={{ border: 'none', outline: 'none', width: '100%', fontSize: 14, color: '#1E293B', background: 'transparent' }}
        />
        {searchQuery && (
          <button onClick={() => setSearchQuery('')} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9CA3AF', padding: 0 }}>✕</button>
        )}
      </div>

      {/* ── Stats Bar ── */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, overflowX: 'auto', paddingBottom: 2 }}>
        {[
          { label: 'Total', value: products.length, color: '#4F46E5', bg: '#EEF2FF' },
          { label: 'Stok Habis', value: products.filter(p => p.stock === 0).length, color: '#DC2626', bg: '#FEE2E2' },
          { label: 'Menipis', value: products.filter(p => p.stock > 0 && p.stock <= LOW_STOCK_THRESHOLD).length, color: '#D97706', bg: '#FEF3C7' },
          { label: 'Varian', value: products.filter(p => p.variants).length, color: '#7C3AED', bg: '#EDE9FE' },
        ].map(s => (
          <div key={s.label} style={{ background: s.bg, borderRadius: 12, padding: '7px 14px', whiteSpace: 'nowrap', flexShrink: 0 }}>
            <span style={{ fontWeight: 900, fontSize: 16, color: s.color }}>{s.value}</span>
            <span style={{ fontSize: 11, color: s.color, marginLeft: 5, fontWeight: 600 }}>{s.label}</span>
          </div>
        ))}
      </div>

      {/* ── Product Cards ── */}
      {filteredProducts.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '48px 0', color: '#94A3B8' }}>
          <div style={{ fontSize: 40, marginBottom: 8 }}>📦</div>
          <div style={{ fontWeight: 700, fontSize: 15 }}>Tidak ada produk</div>
          <div style={{ fontSize: 13, marginTop: 4 }}>Coba ubah kata kunci pencarian</div>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
          {filteredProducts.map((product) => {
            const margin = getMargin(product.price, product.costPrice);
            const isLowStock = product.stock > 0 && product.stock <= LOW_STOCK_THRESHOLD;
            const isOutStock = product.stock === 0;
            const hasVariant = !!product.variants;
            const hasWholesale = !!product.wholesaleEnabled;
            return (
              <div
                key={product.id}
                onClick={() => handleOpenModal(product, !isAdmin)}
                style={{
                  background: 'white', borderRadius: 18, overflow: 'hidden',
                  boxShadow: '0 2px 10px rgba(0,0,0,0.07)',
                  border: `1.5px solid ${isOutStock ? '#FCA5A5' : isLowStock ? '#FDE68A' : '#F1F5F9'}`,
                  cursor: 'pointer', transition: 'transform 0.15s, box-shadow 0.15s',
                  position: 'relative', display: 'flex', flexDirection: 'column',
                }}
                onTouchStart={e => e.currentTarget.style.transform = 'scale(0.98)'}
                onTouchEnd={e => e.currentTarget.style.transform = 'scale(1)'}
              >
                {/* Image */}
                <div style={{ width: '100%', aspectRatio: '1/1', background: '#F8FAFC', display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative', overflow: 'hidden' }}>
                  {product.image
                    ? <img src={product.image} alt={product.name} style={{ width: '100%', height: '100%', objectFit: 'contain', padding: 8 }} />
                    : <div style={{ fontSize: 36, opacity: 0.25 }}>📦</div>
                  }
                  {/* Top badges */}
                  {hasVariant && (
                    <div style={{ position: 'absolute', top: 7, left: 7, background: '#7C3AED', color: 'white', fontSize: 10, fontWeight: 800, padding: '2px 7px', borderRadius: 99 }}>
                      🎨 {(() => { try { return JSON.parse(product.variants).length; } catch { return '?'; } })()} Varian
                    </div>
                  )}
                  {hasWholesale && !hasVariant && (
                    <div style={{ position: 'absolute', top: 7, left: 7, background: '#C2410C', color: 'white', fontSize: 10, fontWeight: 800, padding: '2px 7px', borderRadius: 99 }}>
                      🏷️ Grosir
                    </div>
                  )}
                  {isOutStock && (
                    <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.45)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <span style={{ background: '#EF4444', color: 'white', fontWeight: 900, fontSize: 12, padding: '4px 12px', borderRadius: 99 }}>HABIS</span>
                    </div>
                  )}
                </div>

                {/* Body */}
                <div style={{ padding: '10px 12px 12px', flex: 1, display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <div style={{ fontWeight: 800, fontSize: 13, color: '#1E293B', lineHeight: 1.3, marginBottom: 2 }}>{product.name}</div>
                  <div style={{ fontWeight: 900, fontSize: 15, color: '#4F46E5' }}>
                    Rp {product.price.toLocaleString('id-ID')}
                  </div>
                  {margin !== null && (
                    <div style={{ fontSize: 11, color: '#10B981', fontWeight: 700 }}>Margin {margin}%</div>
                  )}

                  {/* Stock + Unit row */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 4, flexWrap: 'wrap' }}>
                    <span style={{
                      fontSize: 11, fontWeight: 700, padding: '2px 8px', borderRadius: 99,
                      background: isOutStock ? '#FEE2E2' : isLowStock ? '#FEF3C7' : '#DCFCE7',
                      color: isOutStock ? '#DC2626' : isLowStock ? '#D97706' : '#16A34A',
                    }}>
                      {isOutStock ? '✕ Habis' : `${product.stock} ${product.unit || 'pcs'}`}
                    </span>
                    {!isOutStock && isLowStock && (
                      <span style={{ fontSize: 10, color: '#D97706', fontWeight: 700 }}>⚠️ Menipis</span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel" style={{ maxHeight: '92vh', overflowY: 'auto', maxWidth: 480, width: '100%' }}>
            <h2 style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {isViewOnly ? <Eye size={20} color="#6B7280" /> : null}
              {isViewOnly ? 'Detail Produk' : formData.id ? 'Edit Produk' : 'Tambah Produk'}
              {isViewOnly && <span style={{ fontSize: '0.7rem', background: '#F3F4F6', color: '#6B7280', padding: '2px 8px', borderRadius: 99, fontWeight: 600 }}>Hanya Lihat</span>}
            </h2>
            <form onSubmit={isViewOnly ? (e) => { e.preventDefault(); setIsModalOpen(false); } : handleSave}>

              {/* Gambar */}
              <div className="form-group" style={{ textAlign: 'center' }}>
                {formData.image ? (
                  <img src={formData.image} alt="Preview" style={{ width: '100px', height: '100px', objectFit: 'contain', borderRadius: '8px', marginBottom: '10px', border: '1px solid #e5e7eb' }} />
                ) : (
                  <div style={{ width: '100px', height: '100px', backgroundColor: '#e5e7eb', borderRadius: '8px', margin: '0 auto 10px auto', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#9ca3af' }}>No Image</div>
                )}
                <label className="btn btn-secondary" style={{ cursor: 'pointer', display: 'inline-block' }}>
                  Pilih Gambar / Kamera
                  <input type="file" accept="image/*" onChange={handleImageUpload} style={{ display: 'none' }} capture="environment" />
                </label>
              </div>

              {/* Nama */}
              <div className="form-group">
                <label>Nama Produk</label>
                <input type="text" name="name" value={formData.name} onChange={handleInputChange} required />
              </div>

              {/* Harga Jual & Harga Modal */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div className="form-group">
                  <label>💰 Harga Jual (Rp)</label>
                  <input type="number" name="price" value={formData.price} onChange={handleInputChange} required placeholder="0" />
                </div>
                <div className="form-group">
                  <label>📦 Harga Modal (Rp)</label>
                  <input type="number" name="costPrice" value={formData.costPrice} onChange={handleInputChange} placeholder="0" />
                </div>
              </div>

              {/* Margin preview */}
              {formData.price && formData.costPrice && Number(formData.costPrice) > 0 && (
                <div style={{ background: '#F0FDF4', border: '1px solid #86EFAC', borderRadius: 8, padding: '8px 14px', marginBottom: 12, fontSize: 13 }}>
                  <strong>Margin:</strong> Rp {(Number(formData.price) - Number(formData.costPrice)).toLocaleString('id-ID')}
                  {' '}
                  <strong>({Math.round(((Number(formData.price) - Number(formData.costPrice)) / Number(formData.price)) * 100)}%)</strong>
                </div>
              )}

              {/* Stok & Satuan */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div className="form-group">
                  <label>Stok</label>
                  <input type="number" name="stock" value={formData.stock} onChange={handleInputChange} required />
                </div>
                <div className="form-group">
                  <label>Satuan</label>
                  <select name="unit" value={formData.unit || 'pcs'} onChange={handleInputChange} style={{ width: '100%', padding: '10px 12px', border: '1px solid #E2E8F0', borderRadius: 8, fontSize: 14, outline: 'none', background: '#fff' }}>
                    <option value="pcs">Pcs</option>
                    <option value="Kg">Kg</option>
                    <option value="Gram">Gram</option>
                    <option value="Liter">Liter</option>
                    <option value="Lusin">Lusin</option>
                    <option value="Pack">Pack</option>
                    <option value="Box">Box</option>
                    <option value="Meter">Meter</option>
                  </select>
                </div>
              </div>

              {/* Harga Grosir Toggle */}
              <div style={{ background: '#FFF7ED', border: '1px solid #FED7AA', borderRadius: 10, padding: '12px 14px', marginBottom: 14 }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer', fontWeight: 700, color: '#C2410C' }}>
                  <input
                    type="checkbox"
                    checked={formData.wholesaleEnabled}
                    onChange={(e) => setFormData({ ...formData, wholesaleEnabled: e.target.checked })}
                    style={{ width: 18, height: 18, accentColor: '#C2410C' }}
                  />
                  🏷️ Aktifkan Harga Grosir
                </label>
                {formData.wholesaleEnabled && (
                  <div style={{ marginTop: 12 }}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 6 }}>
                      <span style={{ fontSize: 12, fontWeight: 700, color: '#78350F' }}>Min. Qty (beli)</span>
                      <span style={{ fontSize: 12, fontWeight: 700, color: '#78350F' }}>Harga per item (Rp)</span>
                    </div>
                    {formData.wholesalePrices.map((tier, i) => (
                      <div key={i} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 6 }}>
                        <input type="number" placeholder={`Min qty tier ${i + 1}`} value={tier.minQty} onChange={(e) => handleWholesaleChange(i, 'minQty', e.target.value)} style={{ padding: '8px 10px', border: '1px solid #FED7AA', borderRadius: 8, fontSize: 13, outline: 'none' }} />
                        <input type="number" placeholder="Harga grosir" value={tier.price} onChange={(e) => handleWholesaleChange(i, 'price', e.target.value)} style={{ padding: '8px 10px', border: '1px solid #FED7AA', borderRadius: 8, fontSize: 13, outline: 'none' }} />
                      </div>
                    ))}
                    <p style={{ fontSize: 11, color: '#92400E', marginTop: 4 }}>* Maksimal 5 tingkatan harga. Kosongkan baris yang tidak dipakai.</p>
                  </div>
                )}
              </div>

              {/* Varian Produk */}
              <div style={{ background: '#F5F3FF', border: '1px solid #C4B5FD', borderRadius: 10, padding: '12px 14px', marginBottom: 14 }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer', fontWeight: 700, color: '#6D28D9' }}>
                  <input
                    type="checkbox"
                    checked={formData.variantEnabled}
                    onChange={(e) => setFormData({
                      ...formData,
                      variantEnabled: e.target.checked,
                      variants: e.target.checked ? (formData.variants?.length ? formData.variants : [{ ...EMPTY_VARIANT }]) : [{ ...EMPTY_VARIANT }]
                    })}
                    style={{ width: 18, height: 18, accentColor: '#6D28D9' }}
                  />
                  🎨 Aktifkan Varian Produk
                </label>
                {formData.variantEnabled && (
                  <div style={{ marginTop: 12 }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                      {formData.variants.map((v, i) => (
                        <div key={i} style={{
                          background: 'white', border: '1px solid #C4B5FD',
                          borderRadius: 10, padding: '10px 12px', position: 'relative'
                        }}>
                          {/* Nomor + Hapus */}
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                            <span style={{ fontSize: 11, fontWeight: 700, color: '#7C3AED', background: '#EDE9FE', padding: '2px 8px', borderRadius: 99 }}>
                              Varian {i + 1}
                            </span>
                            <button
                              type="button"
                              onClick={() => {
                                const upd = formData.variants.filter((_, idx) => idx !== i);
                                setFormData({ ...formData, variants: upd.length ? upd : [{ ...EMPTY_VARIANT }] });
                              }}
                              style={{ background: '#FEE2E2', color: '#DC2626', border: 'none', borderRadius: 7, padding: '4px 8px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4, fontSize: 12, fontWeight: 600 }}
                            >
                              <Trash2 size={12} /> Hapus
                            </button>
                          </div>

                          {/* Nama Varian — full width */}
                          <input
                            type="text"
                            placeholder="Nama varian (mis. Merah-L, 500ml, Size 40...)"
                            value={v.name}
                            onChange={e => {
                              const upd = [...formData.variants];
                              upd[i] = { ...upd[i], name: e.target.value };
                              setFormData({ ...formData, variants: upd });
                            }}
                            style={{ width: '100%', padding: '8px 10px', border: '1px solid #DDD6FE', borderRadius: 8, fontSize: 13, outline: 'none', boxSizing: 'border-box', marginBottom: 8 }}
                          />

                          {/* Stok + Harga Jual + Harga Modal */}
                          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                            <div>
                              <div style={{ fontSize: 11, fontWeight: 600, color: '#7C3AED', marginBottom: 4 }}>Stok (opsional)</div>
                              <input
                                type="number"
                                placeholder="mis. 10"
                                value={v.stock}
                                onChange={e => {
                                  const upd = [...formData.variants];
                                  upd[i] = { ...upd[i], stock: e.target.value };
                                  setFormData({ ...formData, variants: upd });
                                }}
                                style={{ width: '100%', padding: '8px 10px', border: '1px solid #DDD6FE', borderRadius: 8, fontSize: 13, outline: 'none', boxSizing: 'border-box' }}
                              />
                            </div>
                            <div>
                              <div style={{ fontSize: 11, fontWeight: 600, color: '#7C3AED', marginBottom: 4 }}>Harga Jual Rp (opsional)</div>
                              <input
                                type="number"
                                placeholder="Default = harga produk"
                                value={v.price}
                                onChange={e => {
                                  const upd = [...formData.variants];
                                  upd[i] = { ...upd[i], price: e.target.value };
                                  setFormData({ ...formData, variants: upd });
                                }}
                                style={{ width: '100%', padding: '8px 10px', border: '1px solid #DDD6FE', borderRadius: 8, fontSize: 13, outline: 'none', boxSizing: 'border-box' }}
                              />
                            </div>
                            <div style={{ gridColumn: '1 / -1' }}>
                              <div style={{ fontSize: 11, fontWeight: 600, color: '#7C3AED', marginBottom: 4 }}>📦 Harga Modal Rp (opsional, untuk analisis margin)</div>
                              <input
                                type="number"
                                placeholder="Default = harga modal produk"
                                value={v.costPrice ?? ''}
                                onChange={e => {
                                  const upd = [...formData.variants];
                                  upd[i] = { ...upd[i], costPrice: e.target.value };
                                  setFormData({ ...formData, variants: upd });
                                }}
                                style={{ width: '100%', padding: '8px 10px', border: '1px solid #DDD6FE', borderRadius: 8, fontSize: 13, outline: 'none', boxSizing: 'border-box' }}
                              />
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>

                    <button
                      type="button"
                      onClick={() => setFormData({ ...formData, variants: [...formData.variants, { ...EMPTY_VARIANT }] })}
                      style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 10, width: '100%', justifyContent: 'center', background: '#EDE9FE', color: '#6D28D9', border: '1.5px dashed #A78BFA', borderRadius: 10, padding: '9px 14px', fontWeight: 700, fontSize: 13, cursor: 'pointer' }}
                    >
                      <Plus size={14} /> Tambah Varian
                    </button>
                    <p style={{ fontSize: 11, color: '#7C3AED', marginTop: 6, opacity: 0.7, textAlign: 'center' }}>
                      * Stok &amp; Harga Jual kosong → pakai nilai produk utama · Harga Modal kosong → pakai modal produk
                    </p>
                  </div>
                )}
              </div>


              <div className="modal-actions" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', marginTop: '20px' }}>
                {isAdmin && formData.id ? (
                  <button type="button" className="btn btn-danger" style={{ display: 'flex', alignItems: 'center', gap: '6px', background: '#FEE2E2', color: '#DC2626', border: 'none' }} onClick={() => handleDelete(formData.id)}>
                    <Trash2 size={16} /> Hapus
                  </button>
                ) : <div></div>}
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button type="button" className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>Tutup</button>
                  {isAdmin && <button type="submit" className="btn btn-primary">Simpan</button>}
                </div>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
