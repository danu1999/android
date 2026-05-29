import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Search, AlertTriangle, Eye, ChevronRight, Package, Tag } from 'lucide-react';
import api from '../api';
import { useAuth, useIsAdmin, useDemoBlock } from '../AuthContext';

const EMPTY_VARIANT = { name: '', stock: '', price: '', costPrice: '' };

const EMPTY_FORM = {
  id: null, name: '', price: '', costPrice: '',
  stock: '', unit: 'pcs', image: '', barcode: '',
  category: 'Umum',
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

const CATEGORIES = [
  { key: 'Semua', label: 'Semua', emoji: '🏪' },
  { key: 'Minuman', label: 'Minuman', emoji: '🥤' },
  { key: 'Makanan', label: 'Makanan', emoji: '🍽️' },
  { key: 'Laundry', label: 'Laundry', emoji: '🧺' },
  { key: 'Umum', label: 'Umum', emoji: '📦' },
];

export default function Katalog() {
  const { user } = useAuth();
  const isAdmin = useIsAdmin();
  const { showDemoBlock, isDemo } = useDemoBlock();
  const [products, setProducts] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState('Semua');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isViewOnly, setIsViewOnly] = useState(false);
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
      setFormData({ ...product, wholesalePrices, variants, variantEnabled, category: product.category || 'Umum' });
    } else {
      setFormData({ ...EMPTY_FORM });
    }
    setIsViewOnly(viewOnly);
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      const filledWholesale = formData.wholesalePrices.filter(
        w => w.minQty !== '' && w.price !== ''
      ).map(w => ({ minQty: Number(w.minQty), price: Number(w.price) }));

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
        category: formData.category || 'Umum',
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

  const filteredProducts = products.filter(p => {
    const matchSearch = p.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchCategory = activeCategory === 'Semua' || (p.category || 'Umum') === activeCategory;
    return matchSearch && matchCategory;
  });

  const getMargin = (price, cost) => {
    if (!cost || cost === 0) return null;
    return Math.round(((price - cost) / price) * 100);
  };

  const fmt = (n) => Number(n).toLocaleString('id-ID');

  return (
    <div className="page-container" style={{ background: '#F8FAFC', minHeight: '100vh', padding: '0 0 32px' }}>
      <style>{`
        /* ── Catalog Styles ── */
        .katalog-search-container {
          display: flex;
          align-items: center;
          gap: 10px;
          background: #ffffff;
          border: 1.5px solid #E2E8F0;
          border-radius: 14px;
          padding: 10px 14px;
          box-shadow: 0 2px 8px rgba(0,0,0,0.02);
          transition: all 0.2s ease;
        }
        .katalog-search-container:focus-within {
          border-color: #6366F1;
          box-shadow: 0 0 0 3px rgba(99,102,241,0.12);
        }
        .katalog-input {
          width: 100%;
          padding: 11px 13px;
          border: 1.5px solid #E2E8F0;
          border-radius: 12px;
          font-size: 14px;
          outline: none;
          background: #F8FAFC;
          transition: all 0.2s ease;
          box-sizing: border-box;
          color: #0F172A;
        }
        .katalog-input:focus {
          border-color: #6366F1;
          background: #ffffff;
          box-shadow: 0 0 0 3px rgba(99,102,241,0.1);
        }
        .katalog-modal-overlay {
          position: fixed;
          inset: 0;
          z-index: 1000;
          background: rgba(15,23,42,0.45);
          backdrop-filter: blur(8px);
          display: flex;
          align-items: flex-end;
          justify-content: center;
          padding: 0;
          animation: fadeInOverlay 0.2s ease-out;
        }
        @media (min-width: 640px) {
          .katalog-modal-overlay { align-items: center; padding: 16px; }
          .katalog-modal-content { border-radius: 24px !important; max-height: 90vh !important; }
        }
        .katalog-modal-content {
          background: #ffffff;
          border-radius: 24px 24px 0 0;
          border: 1px solid rgba(255,255,255,0.7);
          box-shadow: 0 -8px 40px rgba(15,23,42,0.18);
          max-height: 92dvh;
          overflow-y: auto;
          max-width: 560px;
          width: 100%;
          padding: 0 20px 32px;
          box-sizing: border-box;
          animation: slideUpSheet 0.3s cubic-bezier(0.34,1.56,0.64,1);
        }
        .modal-drag-bar {
          width: 40px;
          height: 4px;
          background: #E2E8F0;
          border-radius: 99px;
          margin: 12px auto 16px;
        }
        .cat-tab {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          padding: 7px 14px;
          border-radius: 99px;
          font-size: 12px;
          font-weight: 700;
          white-space: nowrap;
          border: none;
          cursor: pointer;
          transition: all 0.18s ease;
          background: #F1F5F9;
          color: #64748B;
        }
        .cat-tab.active {
          background: #4F46E5;
          color: #ffffff;
          box-shadow: 0 4px 12px rgba(79,70,229,0.35);
        }
        .cat-tab:not(.active):hover {
          background: #E2E8F0;
          color: #334155;
        }
        .product-list-row {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 12px 16px;
          border-bottom: 1px solid #F1F5F9;
          cursor: pointer;
          transition: background 0.15s ease;
          background: #ffffff;
        }
        .product-list-row:last-child { border-bottom: none; }
        .product-list-row:hover { background: #F8FAFC; }
        .product-list-row:active { background: #EEF2FF; }
        .product-thumb {
          width: 52px;
          height: 52px;
          border-radius: 12px;
          object-fit: cover;
          background: #F1F5F9;
          flex-shrink: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          overflow: hidden;
        }
        .stock-badge {
          display: inline-flex;
          align-items: center;
          gap: 3px;
          padding: 2px 8px;
          border-radius: 6px;
          font-size: 10px;
          font-weight: 800;
        }
        @keyframes fadeInOverlay {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes slideUpSheet {
          from { transform: translateY(24px); opacity: 0; }
          to { transform: translateY(0); opacity: 1; }
        }
      `}</style>

      {/* ── Sticky Top Bar ── */}
      <div style={{ position: 'sticky', top: 0, zIndex: 30, background: '#F8FAFC', padding: '16px 16px 0', borderBottom: '1px solid #F1F5F9' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div>
            <h1 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 900, color: '#0F172A', letterSpacing: '-0.025em' }}>
              Katalog Produk
            </h1>
            <p style={{ margin: '2px 0 0', fontSize: '0.75rem', color: '#94A3B8', fontWeight: 500 }}>
              {!isAdmin ? '👁️ Mode lihat saja' : `${products.length} produk · ${products.filter(p => p.stock === 0).length} habis`}
            </p>
          </div>
          {isAdmin && (
            <button
              onClick={() => handleOpenModal()}
              style={{
                display: 'flex', alignItems: 'center', gap: 5, padding: '9px 16px',
                background: 'linear-gradient(135deg,#4F46E5 0%,#3730A3 100%)',
                color: 'white', border: 'none', borderRadius: 12, fontWeight: 800, fontSize: 13,
                cursor: 'pointer', boxShadow: '0 4px 14px rgba(79,70,229,0.38)', flexShrink: 0,
                transition: 'all 0.2s'
              }}
            >
              <Plus size={15} /> Tambah
            </button>
          )}
        </div>

        {/* Search Bar */}
        <div className="katalog-search-container" style={{ marginBottom: 10 }}>
          <Search size={16} color="#94A3B8" style={{ flexShrink: 0 }} />
          <input
            type="text"
            placeholder="Cari nama produk..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ border: 'none', outline: 'none', width: '100%', fontSize: 14, color: '#0F172A', background: 'transparent' }}
          />
          {searchQuery && (
            <button onClick={() => setSearchQuery('')} style={{ background: '#F1F5F9', border: 'none', cursor: 'pointer', color: '#64748B', padding: '3px 8px', borderRadius: 99, fontSize: 11, fontWeight: 700, flexShrink: 0 }}>✕</button>
          )}
        </div>

        {/* Quick Filter Category Tabs */}
        <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 12, WebkitOverflowScrolling: 'touch', scrollbarWidth: 'none' }}>
          {CATEGORIES.map(cat => (
            <button
              key={cat.key}
              className={`cat-tab${activeCategory === cat.key ? ' active' : ''}`}
              onClick={() => setActiveCategory(cat.key)}
            >
              <span>{cat.emoji}</span>
              {cat.label}
              {cat.key !== 'Semua' && (
                <span style={{
                  fontSize: 10, fontWeight: 900, opacity: 0.75, marginLeft: 2
                }}>
                  {products.filter(p => (p.category || 'Umum') === cat.key).length}
                </span>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* ── Low Stock Alerts ── */}
      <div style={{ padding: '8px 16px 0' }}>
        {products.filter(p => p.stock <= LOW_STOCK_THRESHOLD && p.stock > 0).length > 0 && (
          <div style={{ background: '#FFFBEB', border: '1.5px solid #FDE68A', borderRadius: 12, padding: '10px 14px', marginBottom: 8, display: 'flex', alignItems: 'flex-start', gap: 8 }}>
            <AlertTriangle size={15} color="#D97706" style={{ flexShrink: 0, marginTop: 1 }} />
            <div style={{ fontSize: 12 }}>
              <span style={{ fontWeight: 800, color: '#92400E' }}>Stok Menipis: </span>
              <span style={{ color: '#B45309', fontWeight: 600 }}>
                {products.filter(p => p.stock <= LOW_STOCK_THRESHOLD && p.stock > 0).map(p => `${p.name} (${p.stock})`).join(' · ')}
              </span>
            </div>
          </div>
        )}
        {products.filter(p => p.stock === 0).length > 0 && (
          <div style={{ background: '#FEF2F2', border: '1.5px solid #FCA5A5', borderRadius: 12, padding: '10px 14px', marginBottom: 8, display: 'flex', alignItems: 'flex-start', gap: 8 }}>
            <AlertTriangle size={15} color="#DC2626" style={{ flexShrink: 0, marginTop: 1 }} />
            <div style={{ fontSize: 12 }}>
              <span style={{ fontWeight: 800, color: '#991B1B' }}>Stok Habis: </span>
              <span style={{ color: '#B91C1C', fontWeight: 600 }}>
                {products.filter(p => p.stock === 0).map(p => p.name).join(' · ')}
              </span>
            </div>
          </div>
        )}
      </div>

      {/* ── Product List ── */}
      {filteredProducts.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '48px 24px', margin: '16px', background: '#ffffff', borderRadius: 20, border: '1.5px dashed #E2E8F0' }}>
          <div style={{ fontSize: 44, marginBottom: 10 }}>📦</div>
          <div style={{ fontWeight: 800, fontSize: 15, color: '#0F172A' }}>Tidak Ada Produk</div>
          <div style={{ fontSize: 13, color: '#94A3B8', marginTop: 4 }}>
            {searchQuery || activeCategory !== 'Semua' ? 'Coba ubah filter atau kata kunci pencarian.' : 'Silakan tambah produk baru.'}
          </div>
        </div>
      ) : (
        <div style={{ background: '#ffffff', borderRadius: 20, margin: '12px 16px', overflow: 'hidden', border: '1px solid #F1F5F9', boxShadow: '0 2px 12px rgba(15,23,42,0.04)' }}>
          {/* List count header */}
          <div style={{ padding: '10px 16px', borderBottom: '1px solid #F1F5F9', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#FAFAFA' }}>
            <span style={{ fontSize: 11, fontWeight: 800, color: '#94A3B8', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
              {filteredProducts.length} Produk{activeCategory !== 'Semua' ? ` · ${activeCategory}` : ''}
            </span>
            {searchQuery && (
              <span style={{ fontSize: 11, color: '#6366F1', fontWeight: 700 }}>
                Hasil: "{searchQuery}"
              </span>
            )}
          </div>

          {filteredProducts.map((product) => {
            const margin = getMargin(product.price, product.costPrice);
            const isLowStock = product.stock > 0 && product.stock <= LOW_STOCK_THRESHOLD;
            const isOutStock = product.stock === 0;
            const hasVariant = !!product.variants;
            const hasWholesale = !!product.wholesaleEnabled;

            return (
              <div
                key={product.id}
                className="product-list-row"
                onClick={() => handleOpenModal(product, !isAdmin)}
              >
                {/* Thumbnail */}
                <div className="product-thumb">
                  {product.image ? (
                    <img src={product.image} alt={product.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} draggable="false" />
                  ) : (
                    <span style={{ fontSize: 24, opacity: 0.3 }}>📦</span>
                  )}
                </div>

                {/* Info */}
                <div style={{ flex: 1, minWidth: 0 }}>
                  {/* Name row */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 3 }}>
                    <span style={{ fontSize: 13, fontWeight: 700, color: '#1E293B', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '60vw' }}>
                      {product.name}
                    </span>
                    {hasVariant && (
                      <span style={{ fontSize: 9, fontWeight: 900, background: '#F3E8FF', color: '#7C3AED', padding: '2px 6px', borderRadius: 6, flexShrink: 0 }}>
                        {(() => { try { return JSON.parse(product.variants).length; } catch { return '?'; } })()} VAR
                      </span>
                    )}
                    {hasWholesale && !hasVariant && (
                      <span style={{ fontSize: 9, fontWeight: 900, background: '#FFF7ED', color: '#C2410C', padding: '2px 6px', borderRadius: 6, flexShrink: 0 }}>GROSIR</span>
                    )}
                  </div>

                  {/* Stock badge */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
                    <span className="stock-badge" style={{
                      background: isOutStock ? '#FEE2E2' : isLowStock ? '#FEF3C7' : '#DCFCE7',
                      color: isOutStock ? '#DC2626' : isLowStock ? '#D97706' : '#16A34A',
                    }}>
                      {isOutStock ? '🚫 HABIS' : isLowStock ? `⚠️ Sisa ${product.stock} ${product.unit || 'pcs'}` : `✓ ${product.stock} ${product.unit || 'pcs'}`}
                    </span>
                  </div>

                  {/* Price row */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 13, fontWeight: 900, color: '#4F46E5' }}>
                      Rp {fmt(product.price)}
                    </span>
                    {product.costPrice > 0 && (
                      <span style={{ fontSize: 11, color: '#94A3B8', fontWeight: 500 }}>
                        Modal Rp {fmt(product.costPrice)}
                      </span>
                    )}
                    {margin !== null && (
                      <span style={{ fontSize: 10, fontWeight: 800, color: '#10B981', background: '#ECFDF5', padding: '1px 6px', borderRadius: 5 }}>
                        +{margin}%
                      </span>
                    )}
                  </div>
                </div>

                {/* Arrow */}
                <ChevronRight size={16} color="#CBD5E1" style={{ flexShrink: 0 }} />
              </div>
            );
          })}
        </div>
      )}

      {/* ── Modal Add / Edit ── */}
      {isModalOpen && (
        <div className="katalog-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) setIsModalOpen(false); }}>
          <div className="katalog-modal-content">
            <div className="modal-drag-bar" />

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
              <h2 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 900, color: '#0F172A', display: 'flex', alignItems: 'center', gap: 8 }}>
                {isViewOnly ? <Eye size={18} color="#64748B" /> : <Package size={18} color="#4F46E5" />}
                {isViewOnly ? 'Detail Produk' : formData.id ? 'Edit Produk' : 'Tambah Produk Baru'}
              </h2>
              <button
                onClick={() => setIsModalOpen(false)}
                style={{ background: '#F1F5F9', border: 'none', borderRadius: 99, width: 30, height: 30, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, color: '#475569', fontWeight: 900 }}
              >✕</button>
            </div>

            <form onSubmit={isViewOnly ? (e) => { e.preventDefault(); setIsModalOpen(false); } : handleSave} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>

              {/* Gambar */}
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, background: '#F8FAFC', borderRadius: 14, padding: '14px 12px', border: '1px solid #E2E8F0' }}>
                {formData.image ? (
                  <div style={{ position: 'relative', width: 100, height: 100, background: '#ffffff', borderRadius: 12, overflow: 'hidden', border: '1.5px solid #E2E8F0' }}>
                    <img src={formData.image} alt="Preview" draggable="false" style={{ width: '100%', height: '100%', objectFit: 'contain', padding: 4 }} />
                    {!isViewOnly && (
                      <button type="button" onClick={() => setFormData({ ...formData, image: '' })} style={{ position: 'absolute', top: 4, right: 4, background: '#EF4444', color: 'white', border: 'none', borderRadius: '50%', width: 20, height: 20, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, fontWeight: 900 }}>✕</button>
                    )}
                  </div>
                ) : (
                  <div style={{ width: 100, height: 100, backgroundColor: '#E2E8F0', borderRadius: 12, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', color: '#64748B', gap: 4, border: '1.5px dashed #CBD5E1' }}>
                    <span style={{ fontSize: 22 }}>📦</span>
                    <span style={{ fontSize: 10, fontWeight: 700 }}>No Image</span>
                  </div>
                )}
                {!isViewOnly && (
                  <label style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '7px 14px', background: '#ffffff', border: '1.5px solid #CBD5E1', borderRadius: 10, fontSize: 12, fontWeight: 800, color: '#334155', cursor: 'pointer' }}>
                    📸 Ambil / Pilih Foto
                    <input type="file" accept="image/*" onChange={handleImageUpload} style={{ display: 'none' }} capture="environment" />
                  </label>
                )}
              </div>

              {/* Nama Produk */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                <label style={{ fontSize: 12, fontWeight: 800, color: '#334155' }}>Nama Produk *</label>
                <input type="text" name="name" value={formData.name} onChange={handleInputChange} required disabled={isViewOnly} className="katalog-input" placeholder="Masukkan nama produk..." />
              </div>

              {/* Kategori */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                <label style={{ fontSize: 12, fontWeight: 800, color: '#334155', display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Tag size={13} color="#6366F1" /> Kategori Produk
                </label>
                <select
                  name="category"
                  value={formData.category || 'Umum'}
                  onChange={handleInputChange}
                  disabled={isViewOnly}
                  className="katalog-input"
                  style={{ width: '100%', fontWeight: 600 }}
                >
                  <option value="Minuman">🥤 Minuman</option>
                  <option value="Makanan">🍽️ Makanan</option>
                  <option value="Laundry">🧺 Laundry</option>
                  <option value="Umum">📦 Umum / Lainnya</option>
                </select>
              </div>

              {/* Harga Jual & Harga Modal */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                  <label style={{ fontSize: 12, fontWeight: 800, color: '#334155' }}>💰 Harga Jual Rp *</label>
                  <input type="number" name="price" value={formData.price} onChange={handleInputChange} required placeholder="0" disabled={isViewOnly} className="katalog-input" />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                  <label style={{ fontSize: 12, fontWeight: 800, color: '#334155' }}>📦 Harga Modal Rp</label>
                  <input type="number" name="costPrice" value={formData.costPrice} onChange={handleInputChange} placeholder="0" disabled={isViewOnly} className="katalog-input" />
                </div>
              </div>

              {/* Margin Info */}
              {formData.price && formData.costPrice && Number(formData.costPrice) > 0 && (
                <div style={{ background: '#ECFDF5', border: '1.5px solid #A7F3D0', borderRadius: 12, padding: '9px 12px', fontSize: 12, color: '#065F46', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span>Keuntungan per item:</span>
                  <strong style={{ fontSize: 13 }}>
                    Rp {fmt(Number(formData.price) - Number(formData.costPrice))} ({Math.round(((Number(formData.price) - Number(formData.costPrice)) / Number(formData.price)) * 100)}%)
                  </strong>
                </div>
              )}

              {/* Stok & Satuan */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                  <label style={{ fontSize: 12, fontWeight: 800, color: '#334155' }}>Stok *</label>
                  <input type="number" name="stock" value={formData.stock} onChange={handleInputChange} required disabled={isViewOnly} className="katalog-input" />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                  <label style={{ fontSize: 12, fontWeight: 800, color: '#334155' }}>Satuan</label>
                  <select name="unit" value={formData.unit || 'pcs'} onChange={handleInputChange} disabled={isViewOnly} className="katalog-input" style={{ width: '100%' }}>
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

              {/* Barcode */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                <label style={{ fontSize: 12, fontWeight: 800, color: '#334155' }}>🔍 Barcode (opsional)</label>
                <input type="text" name="barcode" value={formData.barcode || ''} onChange={handleInputChange} placeholder="Scan atau ketik kode barcode..." disabled={isViewOnly} className="katalog-input" />
              </div>

              {/* Harga Grosir */}
              <div style={{ background: '#FFF7ED', border: '1.5px solid #FED7AA', borderRadius: 14, padding: '12px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontWeight: 800, color: '#C2410C', fontSize: 13 }}>
                  <input
                    type="checkbox"
                    checked={formData.wholesaleEnabled}
                    onChange={(e) => !isViewOnly && setFormData({ ...formData, wholesaleEnabled: e.target.checked })}
                    disabled={isViewOnly}
                    style={{ width: 17, height: 17, accentColor: '#C2410C' }}
                  />
                  🏷️ Aktifkan Harga Grosir
                </label>
                {formData.wholesaleEnabled && (
                  <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 6 }}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                      <span style={{ fontSize: 10, fontWeight: 800, color: '#92400E' }}>Min. Qty</span>
                      <span style={{ fontSize: 10, fontWeight: 800, color: '#92400E' }}>Harga Grosir Rp</span>
                    </div>
                    {formData.wholesalePrices.map((tier, i) => (
                      <div key={i} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                        <input type="number" placeholder={`Min qty ${i + 1}`} value={tier.minQty} onChange={(e) => !isViewOnly && handleWholesaleChange(i, 'minQty', e.target.value)} disabled={isViewOnly} style={{ padding: '7px 10px', border: '1.5px solid #FED7AA', borderRadius: 9, fontSize: 12, outline: 'none', background: '#ffffff' }} />
                        <input type="number" placeholder="Harga grosir" value={tier.price} onChange={(e) => !isViewOnly && handleWholesaleChange(i, 'price', e.target.value)} disabled={isViewOnly} style={{ padding: '7px 10px', border: '1.5px solid #FED7AA', borderRadius: 9, fontSize: 12, outline: 'none', background: '#ffffff' }} />
                      </div>
                    ))}
                    <p style={{ fontSize: 10, color: '#B45309', margin: '2px 0 0', opacity: 0.85 }}>* Kosongkan baris yang tidak digunakan.</p>
                  </div>
                )}
              </div>

              {/* Varian Produk */}
              <div style={{ background: '#F5F3FF', border: '1.5px solid #DDD6FE', borderRadius: 14, padding: '12px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontWeight: 800, color: '#6D28D9', fontSize: 13 }}>
                  <input
                    type="checkbox"
                    checked={formData.variantEnabled}
                    onChange={(e) => !isViewOnly && setFormData({
                      ...formData,
                      variantEnabled: e.target.checked,
                      variants: e.target.checked ? (formData.variants?.length ? formData.variants : [{ ...EMPTY_VARIANT }]) : [{ ...EMPTY_VARIANT }]
                    })}
                    disabled={isViewOnly}
                    style={{ width: 17, height: 17, accentColor: '#6D28D9' }}
                  />
                  🎨 Aktifkan Varian Produk
                </label>
                {formData.variantEnabled && (
                  <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {formData.variants.map((v, i) => (
                      <div key={i} style={{ background: '#ffffff', border: '1.5px solid #DDD6FE', borderRadius: 10, padding: '10px', position: 'relative', display: 'flex', flexDirection: 'column', gap: 7 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span style={{ fontSize: 10, fontWeight: 900, color: '#7C3AED', background: '#F3E8FF', padding: '2px 7px', borderRadius: 99 }}>VARIAN {i + 1}</span>
                          {!isViewOnly && (
                            <button type="button" onClick={() => { const upd = formData.variants.filter((_, idx) => idx !== i); setFormData({ ...formData, variants: upd.length ? upd : [{ ...EMPTY_VARIANT }] }); }} style={{ background: '#FEE2E2', color: '#DC2626', border: 'none', borderRadius: 7, padding: '3px 9px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 3, fontSize: 11, fontWeight: 800 }}>
                              <Trash2 size={11} /> Hapus
                            </button>
                          )}
                        </div>
                        <input type="text" placeholder="Nama varian (mis. Merah-L, 500ml...)" value={v.name} disabled={isViewOnly} onChange={e => { const upd = [...formData.variants]; upd[i] = { ...upd[i], name: e.target.value }; setFormData({ ...formData, variants: upd }); }} style={{ width: '100%', padding: '7px 11px', border: '1.5px solid #DDD6FE', borderRadius: 9, fontSize: 12, outline: 'none', boxSizing: 'border-box' }} />
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 7 }}>
                          <div>
                            <div style={{ fontSize: 10, fontWeight: 800, color: '#6D28D9', marginBottom: 3 }}>Stok</div>
                            <input type="number" placeholder="mis. 10" value={v.stock ?? ''} disabled={isViewOnly} onChange={e => { const upd = [...formData.variants]; upd[i] = { ...upd[i], stock: e.target.value }; setFormData({ ...formData, variants: upd }); }} style={{ width: '100%', padding: '7px 9px', border: '1.5px solid #DDD6FE', borderRadius: 9, fontSize: 12, outline: 'none', boxSizing: 'border-box' }} />
                          </div>
                          <div>
                            <div style={{ fontSize: 10, fontWeight: 800, color: '#6D28D9', marginBottom: 3 }}>Harga Jual Rp</div>
                            <input type="number" placeholder="Induk produk" value={v.price ?? ''} disabled={isViewOnly} onChange={e => { const upd = [...formData.variants]; upd[i] = { ...upd[i], price: e.target.value }; setFormData({ ...formData, variants: upd }); }} style={{ width: '100%', padding: '7px 9px', border: '1.5px solid #DDD6FE', borderRadius: 9, fontSize: 12, outline: 'none', boxSizing: 'border-box' }} />
                          </div>
                          <div style={{ gridColumn: '1 / -1' }}>
                            <div style={{ fontSize: 10, fontWeight: 800, color: '#6D28D9', marginBottom: 3 }}>Harga Modal Rp</div>
                            <input type="number" placeholder="Induk produk" value={v.costPrice ?? ''} disabled={isViewOnly} onChange={e => { const upd = [...formData.variants]; upd[i] = { ...upd[i], costPrice: e.target.value }; setFormData({ ...formData, variants: upd }); }} style={{ width: '100%', padding: '7px 9px', border: '1.5px solid #DDD6FE', borderRadius: 9, fontSize: 12, outline: 'none', boxSizing: 'border-box' }} />
                          </div>
                        </div>
                      </div>
                    ))}
                    {!isViewOnly && (
                      <button type="button" onClick={() => setFormData({ ...formData, variants: [...formData.variants, { ...EMPTY_VARIANT }] })} style={{ display: 'flex', alignItems: 'center', gap: 5, width: '100%', justifyContent: 'center', background: '#EDE9FE', color: '#6D28D9', border: '1.5px dashed #A78BFA', borderRadius: 10, padding: '9px', fontWeight: 800, fontSize: 12, cursor: 'pointer' }}>
                        <Plus size={13} /> Tambah Varian
                      </button>
                    )}
                  </div>
                )}
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4, gap: 10 }}>
                {isAdmin && formData.id && !isViewOnly ? (
                  <button type="button" onClick={() => handleDelete(formData.id)} style={{ display: 'flex', alignItems: 'center', gap: 5, background: '#FEE2E2', color: '#DC2626', border: 'none', borderRadius: 11, padding: '10px 14px', fontWeight: 800, fontSize: 13, cursor: 'pointer' }}>
                    <Trash2 size={14} /> Hapus
                  </button>
                ) : <div />}
                <div style={{ display: 'flex', gap: 8 }}>
                  <button type="button" onClick={() => setIsModalOpen(false)} style={{ background: '#F1F5F9', color: '#475569', border: 'none', borderRadius: 11, padding: '10px 18px', fontWeight: 800, fontSize: 13, cursor: 'pointer' }}>Batal</button>
                  {!isViewOnly && (
                    <button type="submit" style={{ background: 'linear-gradient(135deg,#4F46E5 0%,#3730A3 100%)', color: 'white', border: 'none', borderRadius: 11, padding: '10px 22px', fontWeight: 800, fontSize: 13, cursor: 'pointer', boxShadow: '0 4px 12px rgba(79,70,229,0.28)' }}>Simpan</button>
                  )}
                </div>
              </div>

            </form>
          </div>
        </div>
      )}
    </div>
  );
}
