import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Search, AlertTriangle, Eye, Tag } from 'lucide-react';
import api from '../api';
import { useAuth, useIsAdmin, useDemoBlock, DEMO_LIMITS } from '../AuthContext';


const EMPTY_VARIANT = { name: '', stock: '', price: '', costPrice: '' };

const EMPTY_FORM = {
  id: null, name: '', price: '', costPrice: '',
  stock: '', unit: 'pcs', image: '', barcode: '',
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
  const { user } = useAuth();
  const isAdmin = useIsAdmin();
  const { showDemoBlock, isDemo } = useDemoBlock();
  const [products, setProducts] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isViewOnly, setIsViewOnly] = useState(false); // modal view-only untuk kasir
  const [formData, setFormData] = useState(EMPTY_FORM);
  const LOW_STOCK_THRESHOLD = 5;

  // Produk demo — identik dengan Kasir & Keuangan
  const DEMO_PRODUCTS = [
    { id: 'p301', name: 'Pisang Keju Cokelat', price: 15000, costPrice: 9000, stock: 120, unit: 'pcs', wholesaleEnabled: false, wholesalePrices: null, variantEnabled: false, variants: null, barcode: null, image: '/demo/pisang-keju-coklat.png' },
    { id: 'p302', name: 'Pisang Keju Stroberi', price: 15000, costPrice: 9500, stock: 85,  unit: 'pcs', wholesaleEnabled: false, wholesalePrices: null, variantEnabled: false, variants: null, barcode: null, image: '/demo/pisang-keju-stroberi.png' },
    { id: 'p303', name: 'Pisang Keju Premium',  price: 20000, costPrice: 11000, stock: 50, unit: 'pcs', wholesaleEnabled: false, wholesalePrices: null, variantEnabled: true,
      variants: JSON.stringify([
        { id: 1, name: 'Keju Melimpah', price: 25000, costPrice: 13000, stock: 30 },
        { id: 2, name: 'Milo Almond',   price: 28000, costPrice: 15000, stock: 20 },
      ]), barcode: null, image: '/demo/pisang-keju-premium.png' },
    { id: 'p304', name: 'Jus Alpukat',  price: 18000, costPrice: 10000, stock: 60,  unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variantEnabled: false, variants: null, barcode: null, image: '/demo/jus-alpukat.png' },
    { id: 'p305', name: 'Jus Mangga',   price: 15000, costPrice: 8000,  stock: 75,  unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variantEnabled: false, variants: null, barcode: null, image: '/demo/jus-mangga.png' },
    { id: 'p306', name: 'Es Teh Manis', price: 8000,  costPrice: 3000,  stock: 200, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variantEnabled: false, variants: null, barcode: null, image: null },
  ];

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
    <div className="page-container" style={{ background: '#F8FAFC', minHeight: '100vh', padding: '16px 16px 32px' }}>
      <style>{`
        .katalog-card {
          background: #ffffff;
          border-radius: 20px;
          border: 1.5px solid rgba(241, 245, 249, 0.9);
          box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.03), 0 8px 10px -6px rgba(15, 23, 42, 0.03);
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          cursor: pointer;
          position: relative;
          display: flex;
          flex-direction: column;
          overflow: hidden;
        }
        .katalog-card:hover {
          transform: translateY(-6px);
          box-shadow: 0 20px 30px -10px rgba(99, 102, 241, 0.12), 0 10px 15px -3px rgba(99, 102, 241, 0.05);
          border-color: rgba(99, 102, 241, 0.35);
        }
        .katalog-card-img-container {
          width: 100%;
          aspect-ratio: 1/1;
          background: linear-gradient(135deg, #F8FAFC 0%, #F1F5F9 100%);
          display: flex;
          align-items: center;
          justify-content: center;
          position: relative;
          overflow: hidden;
        }
        .katalog-card-img {
          width: 100%;
          height: 100%;
          object-fit: contain;
          padding: 12px;
          transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
        }
        .katalog-card:hover .katalog-card-img {
          transform: scale(1.08);
        }
        .katalog-search-container {
          display: flex;
          align-items: center;
          gap: 12px;
          background: #ffffff;
          border: 1.5px solid #E2E8F0;
          border-radius: 16px;
          padding: 12px 16px;
          margin-bottom: 20px;
          box-shadow: 0 4px 15px rgba(0, 0, 0, 0.015);
          transition: all 0.2s ease;
        }
        .katalog-search-container:focus-within {
          border-color: #6366F1;
          box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.15);
        }
        .katalog-stat-pill {
          padding: 8px 16px;
          border-radius: 14px;
          white-space: nowrap;
          flex-shrink: 0;
          font-weight: 700;
          display: flex;
          align-items: center;
          gap: 6px;
          box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
          transition: all 0.2s ease;
        }
        .katalog-stat-pill:hover {
          transform: translateY(-2px);
        }
        .katalog-input {
          width: 100%;
          padding: 12px 14px;
          border: 1.5px solid #E2E8F0;
          border-radius: 12px;
          font-size: 14px;
          outline: none;
          background: #F8FAFC;
          transition: all 0.2s ease;
          box-sizing: border-box;
        }
        .katalog-input:focus {
          border-color: #6366F1;
          background: #ffffff;
          box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
        }
        .katalog-modal-overlay {
          position: fixed;
          inset: 0;
          z-index: 1000;
          background: rgba(15, 23, 42, 0.45);
          backdrop-filter: blur(8px);
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 16px;
          animation: fadeIn 0.25s ease-out;
        }
        .katalog-modal-content {
          background: #ffffff;
          border-radius: 24px;
          border: 1px solid rgba(255, 255, 255, 0.7);
          box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.15);
          max-height: 90vh;
          overflow-y: auto;
          max-width: 520px;
          width: 100%;
          padding: 24px;
          box-sizing: border-box;
          animation: slideUp 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
        }
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes slideUp {
          from { transform: translateY(20px); opacity: 0; }
          to { transform: translateY(0); opacity: 1; }
        }
      `}</style>

      {/* ── Header ── */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20, gap: 12 }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '1.4rem', fontWeight: 900, color: '#0F172A', letterSpacing: '-0.025em' }}>Katalog Produk</h1>
          {!isAdmin ? (
            <p style={{ margin: '4px 0 0', fontSize: '0.8rem', color: '#64748B', display: 'flex', alignItems: 'center', gap: 4 }}>
              <span>👁️ Mode lihat saja — Hubungi Admin untuk ubah produk</span>
            </p>
          ) : (
            <p style={{ margin: '4px 0 0', fontSize: '0.8rem', color: '#64748B' }}>
              Kelola stok barang, grosir, dan varian produk usaha Anda
            </p>
          )}
        </div>
        {isAdmin && (
          <button
            onClick={() => handleOpenModal()}
            style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '10px 18px', background: 'linear-gradient(135deg, #4F46E5 0%, #3730A3 100%)', color: 'white', border: 'none', borderRadius: 14, fontWeight: 800, fontSize: 13, cursor: 'pointer', whiteSpace: 'nowrap', boxShadow: '0 8px 20px -6px rgba(79, 70, 229, 0.45)', flexShrink: 0, transition: 'all 0.2s' }}
          >
            <Plus size={16} /> Tambah Baru
          </button>
        )}
      </div>

      {/* ── Alert Banners ── */}
      {products.filter(p => p.stock <= LOW_STOCK_THRESHOLD && p.stock > 0).length > 0 && (
        <div style={{ background: '#FEF3C7', border: '1.5px solid #FDE68A', borderRadius: 16, padding: '12px 16px', marginBottom: 12, display: 'flex', alignItems: 'flex-start', gap: 10, boxShadow: '0 4px 15px rgba(217, 119, 6, 0.04)' }}>
          <AlertTriangle size={18} color="#D97706" style={{ flexShrink: 0, marginTop: 1 }} />
          <div>
            <span style={{ fontWeight: 800, color: '#92400E', fontSize: 13 }}>⚠️ Stok Menipis: </span>
            <span style={{ color: '#B45309', fontSize: 13, fontWeight: 600 }}>
              {products.filter(p => p.stock <= LOW_STOCK_THRESHOLD && p.stock > 0).map(p => `${p.name} (${p.stock})`).join(', ')}
            </span>
          </div>
        </div>
      )}
      {products.filter(p => p.stock === 0).length > 0 && (
        <div style={{ background: '#FEE2E2', border: '1.5px solid #FCA5A5', borderRadius: 16, padding: '12px 16px', marginBottom: 12, display: 'flex', alignItems: 'flex-start', gap: 10, boxShadow: '0 4px 15px rgba(220, 38, 38, 0.04)' }}>
          <AlertTriangle size={18} color="#DC2626" style={{ flexShrink: 0, marginTop: 1 }} />
          <div>
            <span style={{ fontWeight: 800, color: '#991B1B', fontSize: 13 }}>🚫 Stok Habis: </span>
            <span style={{ color: '#B91C1C', fontSize: 13, fontWeight: 600 }}>
              {products.filter(p => p.stock === 0).map(p => p.name).join(', ')}
            </span>
          </div>
        </div>
      )}

      {/* ── Search ── */}
      <div className="katalog-search-container">
        <Search size={18} color="#64748B" />
        <input
          type="text"
          placeholder="Cari produk berdasarkan nama..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          style={{ border: 'none', outline: 'none', width: '100%', fontSize: 14, color: '#0F172A', background: 'transparent' }}
        />
        {searchQuery && (
          <button onClick={() => setSearchQuery('')} style={{ background: '#F1F5F9', border: 'none', cursor: 'pointer', color: '#64748B', padding: '4px 8px', borderRadius: 99, fontSize: 11, fontWeight: 700 }}>Batal</button>
        )}
      </div>

      {/* ── Stats Bar ── */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 20, overflowX: 'auto', paddingBottom: 6, WebkitOverflowScrolling: 'touch' }}>
        {[
          { label: 'Semua Produk', value: products.length, color: '#4F46E5', bg: '#EEF2FF', border: '#C7D2FE' },
          { label: 'Stok Habis', value: products.filter(p => p.stock === 0).length, color: '#DC2626', bg: '#FEE2E2', border: '#FCA5A5' },
          { label: 'Menipis', value: products.filter(p => p.stock > 0 && p.stock <= LOW_STOCK_THRESHOLD).length, color: '#D97706', bg: '#FEF3C7', border: '#FDE68A' },
          { label: 'Varian', value: products.filter(p => p.variants).length, color: '#7C3AED', bg: '#EDE9FE', border: '#DDD6FE' },
        ].map(s => (
          <div key={s.label} className="katalog-stat-pill" style={{ background: s.bg, color: s.color, border: `1.5px solid ${s.border}` }}>
            <span style={{ fontSize: 16, fontWeight: 900 }}>{s.value}</span>
            <span style={{ fontSize: 11, opacity: 0.85, fontWeight: 700 }}>{s.label}</span>
          </div>
        ))}
      </div>

      {/* ── Product Cards ── */}
      {filteredProducts.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '64px 16px', background: '#ffffff', borderRadius: 24, border: '1.5px dashed #E2E8F0' }}>
          <div style={{ fontSize: 48, marginBottom: 12 }}>📦</div>
          <div style={{ fontWeight: 800, fontSize: 15, color: '#0F172A' }}>Tidak Ada Produk</div>
          <div style={{ fontSize: 13, color: '#64748B', marginTop: 4 }}>Silakan tambah produk baru atau ubah kata kunci pencarian.</div>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(clamp(130px, 42vw, 240px), 1fr))', gap: 14 }}>
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
                className="katalog-card"
                style={{
                  border: isOutStock ? '1.5px solid #FCA5A5' : isLowStock ? '1.5px solid #FDE68A' : '1.5px solid rgba(241, 245, 249, 0.9)'
                }}
              >
                {/* Image */}
                <div className="katalog-card-img-container">
                  {product.image ? (
                    <img src={product.image} alt={product.name} className="katalog-card-img" draggable="false" />
                  ) : (
                    <div style={{ fontSize: 36, opacity: 0.25 }}>📦</div>
                  )}

                  {/* Top Badges */}
                  {hasVariant && (
                    <div style={{ position: 'absolute', top: 8, left: 8, background: 'rgba(124, 58, 237, 0.9)', backdropFilter: 'blur(4px)', color: 'white', fontSize: 9, fontWeight: 900, padding: '3px 8px', borderRadius: 99, boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
                      🎨 {(() => { try { return JSON.parse(product.variants).length; } catch { return '?'; } })()} VARIAN
                    </div>
                  )}
                  {hasWholesale && !hasVariant && (
                    <div style={{ position: 'absolute', top: 8, left: 8, background: 'rgba(194, 65, 12, 0.9)', backdropFilter: 'blur(4px)', color: 'white', fontSize: 9, fontWeight: 900, padding: '3px 8px', borderRadius: 99, boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
                      🏷️ GROSIR
                    </div>
                  )}
                  {isOutStock && (
                    <div style={{ position: 'absolute', inset: 0, background: 'rgba(15, 23, 42, 0.45)', display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(2px)' }}>
                      <span style={{ background: '#EF4444', color: 'white', fontWeight: 900, fontSize: 10, padding: '5px 12px', borderRadius: 99, letterSpacing: '0.05em', boxShadow: '0 4px 12px rgba(239, 68, 68, 0.3)' }}>HABIS</span>
                    </div>
                  )}
                </div>

                {/* Body */}
                <div style={{ padding: '12px 14px 14px', flex: 1, display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <div style={{ fontWeight: 800, fontSize: 13, color: '#1E293B', lineHeight: 1.3, minHeight: 34, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                    {product.name}
                  </div>
                  
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 4, flexWrap: 'wrap', gap: 4 }}>
                    <div style={{ fontWeight: 900, fontSize: 15, color: '#4F46E5', letterSpacing: '-0.02em' }}>
                      Rp {product.price.toLocaleString('id-ID')}
                    </div>
                    {margin !== null && (
                      <span style={{ fontSize: 10, color: '#10B981', fontWeight: 800, background: '#ECFDF5', padding: '1px 6px', borderRadius: 6 }}>+{margin}%</span>
                    )}
                  </div>

                  {/* Stock + Unit row */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 8 }}>
                    <span style={{
                      fontSize: 10, fontWeight: 800, padding: '3px 8px', borderRadius: 8,
                      background: isOutStock ? '#FEE2E2' : isLowStock ? '#FEF3C7' : '#DCFCE7',
                      color: isOutStock ? '#DC2626' : isLowStock ? '#D97706' : '#16A34A',
                      letterSpacing: '0.01em'
                    }}>
                      {isOutStock ? 'Habis' : `${product.stock} ${product.unit || 'pcs'}`}
                    </span>
                    {!isOutStock && isLowStock && (
                      <span style={{ fontSize: 10, color: '#D97706', fontWeight: 800, animation: 'pulse 2s infinite' }}>⚠️ Tipis</span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── Modal Add / Edit ── */}
      {isModalOpen && (
        <div className="katalog-modal-overlay">
          <div className="katalog-modal-content">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h2 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 900, color: '#0F172A', display: 'flex', alignItems: 'center', gap: 8 }}>
                {isViewOnly ? <Eye size={20} color="#64748B" /> : null}
                {isViewOnly ? 'Detail Produk' : formData.id ? 'Edit Produk' : 'Tambah Produk'}
              </h2>
              {isViewOnly && (
                <span style={{ fontSize: '0.75rem', background: '#F1F5F9', color: '#64748B', padding: '4px 10px', borderRadius: 99, fontWeight: 700 }}>Hanya Lihat</span>
              )}
            </div>

            <form onSubmit={isViewOnly ? (e) => { e.preventDefault(); setIsModalOpen(false); } : handleSave} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              
              {/* Gambar / Camera */}
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, background: '#F8FAFC', borderRadius: 16, padding: '16px 12px', border: '1px solid #E2E8F0' }}>
                {formData.image ? (
                  <div style={{ position: 'relative', width: 110, height: 110, background: '#ffffff', borderRadius: 14, overflow: 'hidden', border: '1.5px solid #E2E8F0', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <img src={formData.image} alt="Preview" draggable="false" style={{ width: '100%', height: '100%', objectFit: 'contain', padding: 4 }} />
                    {!isViewOnly && (
                      <button type="button" onClick={() => setFormData({ ...formData, image: '' })} style={{ position: 'absolute', top: 4, right: 4, background: '#EF4444', color: 'white', border: 'none', borderRadius: '50%', width: 22, height: 22, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 900 }}>✕</button>
                    )}
                  </div>
                ) : (
                  <div style={{ width: 110, height: 110, backgroundColor: '#E2E8F0', borderRadius: 14, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', color: '#64748B', gap: 4, border: '1.5px dashed #CBD5E1' }}>
                    <span style={{ fontSize: 24 }}>📦</span>
                    <span style={{ fontSize: 10, fontWeight: 700 }}>No Image</span>
                  </div>
                )}
                {!isViewOnly && (
                  <label style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', background: '#ffffff', border: '1.5px solid #CBD5E1', borderRadius: 10, fontSize: 12, fontWeight: 800, color: '#334155', cursor: 'pointer', boxShadow: '0 2px 4px rgba(0,0,0,0.02)', transition: 'all 0.2s' }}>
                    📸 Ambil / Pilih Foto
                    <input type="file" accept="image/*" onChange={handleImageUpload} style={{ display: 'none' }} capture="environment" />
                  </label>
                )}
              </div>

              {/* Nama */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                <label style={{ fontSize: 12, fontWeight: 800, color: '#334155' }}>Nama Produk</label>
                <input type="text" name="name" value={formData.name} onChange={handleInputChange} required disabled={isViewOnly} className="katalog-input" placeholder="Masukkan nama produk..." />
              </div>

              {/* Harga Jual & Harga Modal */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <label style={{ fontSize: 12, fontWeight: 800, color: '#334155' }}>💰 Harga Jual Rp</label>
                  <input type="number" name="price" value={formData.price} onChange={handleInputChange} required placeholder="0" disabled={isViewOnly} className="katalog-input" />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <label style={{ fontSize: 12, fontWeight: 800, color: '#334155' }}>📦 Harga Modal Rp</label>
                  <input type="number" name="costPrice" value={formData.costPrice} onChange={handleInputChange} placeholder="0" disabled={isViewOnly} className="katalog-input" />
                </div>
              </div>

              {/* Margin Info Badge */}
              {formData.price && formData.costPrice && Number(formData.costPrice) > 0 && (
                <div style={{ background: '#ECFDF5', border: '1.5px solid #A7F3D0', borderRadius: 14, padding: '10px 14px', fontSize: 12, color: '#065F46', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span>Keuntungan bersih per item:</span>
                  <strong style={{ fontSize: 13 }}>
                    Rp {(Number(formData.price) - Number(formData.costPrice)).toLocaleString('id-ID')} ({Math.round(((Number(formData.price) - Number(formData.costPrice)) / Number(formData.price)) * 100)}%)
                  </strong>
                </div>
              )}

              {/* Stok & Satuan */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <label style={{ fontSize: 12, fontWeight: 800, color: '#334155' }}>Stok</label>
                  <input type="number" name="stock" value={formData.stock} onChange={handleInputChange} required disabled={isViewOnly} className="katalog-input" />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
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
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                <label style={{ fontSize: 12, fontWeight: 800, color: '#334155' }}>🔍 Barcode (opsional — untuk Scanner)</label>
                <input
                  type="text"
                  name="barcode"
                  value={formData.barcode || ''}
                  onChange={handleInputChange}
                  placeholder="Scan atau ketik kode barcode produk..."
                  disabled={isViewOnly}
                  className="katalog-input"
                />
              </div>

              {/* Harga Grosir */}
              <div style={{ background: '#FFF7ED', border: '1.5px solid #FDE68A', borderRadius: 16, padding: '14px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontWeight: 800, color: '#C2410C', fontSize: 13 }}>
                  <input
                    type="checkbox"
                    checked={formData.wholesaleEnabled}
                    onChange={(e) => !isViewOnly && setFormData({ ...formData, wholesaleEnabled: e.target.checked })}
                    disabled={isViewOnly}
                    style={{ width: 18, height: 18, accentColor: '#C2410C' }}
                  />
                  🏷️ Aktifkan Harga Grosir
                </label>
                {formData.wholesaleEnabled && (
                  <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 8 }}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                      <span style={{ fontSize: 11, fontWeight: 800, color: '#92400E' }}>Min. Qty (Beli)</span>
                      <span style={{ fontSize: 11, fontWeight: 800, color: '#92400E' }}>Harga Grosir Rp / pcs</span>
                    </div>
                    {formData.wholesalePrices.map((tier, i) => (
                      <div key={i} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                        <input type="number" placeholder={`Min qty ${i + 1}`} value={tier.minQty} onChange={(e) => !isViewOnly && handleWholesaleChange(i, 'minQty', e.target.value)} disabled={isViewOnly} style={{ padding: '8px 10px', border: '1.5px solid #FDE68A', borderRadius: 10, fontSize: 12, outline: 'none', background: '#ffffff' }} />
                        <input type="number" placeholder="Harga grosir" value={tier.price} onChange={(e) => !isViewOnly && handleWholesaleChange(i, 'price', e.target.value)} disabled={isViewOnly} style={{ padding: '8px 10px', border: '1.5px solid #FDE68A', borderRadius: 10, fontSize: 12, outline: 'none', background: '#ffffff' }} />
                      </div>
                    ))}
                    <p style={{ fontSize: 10, color: '#B45309', margin: '4px 0 0', opacity: 0.85 }}>* Kosongkan baris tingkatan yang tidak digunakan.</p>
                  </div>
                )}
              </div>

              {/* Varian Produk */}
              <div style={{ background: '#F5F3FF', border: '1.5px solid #DDD6FE', borderRadius: 16, padding: '14px' }}>
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
                    style={{ width: 18, height: 18, accentColor: '#6D28D9' }}
                  />
                  🎨 Aktifkan Varian Produk
                </label>
                {formData.variantEnabled && (
                  <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                      {formData.variants.map((v, i) => (
                        <div key={i} style={{ background: '#ffffff', border: '1.5px solid #DDD6FE', borderRadius: 12, padding: '12px', position: 'relative', display: 'flex', flexDirection: 'column', gap: 8 }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <span style={{ fontSize: 11, fontWeight: 900, color: '#7C3AED', background: '#F3E8FF', padding: '3px 8px', borderRadius: 99 }}>VARIAN {i + 1}</span>
                            {!isViewOnly && (
                              <button
                                type="button"
                                onClick={() => {
                                  const upd = formData.variants.filter((_, idx) => idx !== i);
                                  setFormData({ ...formData, variants: upd.length ? upd : [{ ...EMPTY_VARIANT }] });
                                }}
                                style={{ background: '#FEE2E2', color: '#DC2626', border: 'none', borderRadius: 8, padding: '4px 10px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4, fontSize: 11, fontWeight: 800 }}
                              >
                                <Trash2 size={12} /> Hapus
                              </button>
                            )}
                          </div>

                          <input
                            type="text"
                            placeholder="Nama varian (mis. Merah-L, 500ml, Size 40...)"
                            value={v.name}
                            disabled={isViewOnly}
                            onChange={e => {
                              const upd = [...formData.variants];
                              upd[i] = { ...upd[i], name: e.target.value };
                              setFormData({ ...formData, variants: upd });
                            }}
                            style={{ width: '100%', padding: '8px 12px', border: '1.5px solid #DDD6FE', borderRadius: 10, fontSize: 13, outline: 'none', boxSizing: 'border-box' }}
                          />

                          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                            <div>
                              <div style={{ fontSize: 10, fontWeight: 800, color: '#6D28D9', marginBottom: 4 }}>Stok (opsional)</div>
                              <input
                                type="number"
                                placeholder="mis. 10"
                                value={v.stock ?? ''}
                                disabled={isViewOnly}
                                onChange={e => {
                                  const upd = [...formData.variants];
                                  upd[i] = { ...upd[i], stock: e.target.value };
                                  setFormData({ ...formData, variants: upd });
                                }}
                                style={{ width: '100%', padding: '8px 10px', border: '1.5px solid #DDD6FE', borderRadius: 10, fontSize: 12, outline: 'none', boxSizing: 'border-box' }}
                              />
                            </div>
                            <div>
                              <div style={{ fontSize: 10, fontWeight: 800, color: '#6D28D9', marginBottom: 4 }}>Harga Jual Rp (opsional)</div>
                              <input
                                type="number"
                                placeholder="Induk produk"
                                value={v.price ?? ''}
                                disabled={isViewOnly}
                                onChange={e => {
                                  const upd = [...formData.variants];
                                  upd[i] = { ...upd[i], price: e.target.value };
                                  setFormData({ ...formData, variants: upd });
                                }}
                                style={{ width: '100%', padding: '8px 10px', border: '1.5px solid #DDD6FE', borderRadius: 10, fontSize: 12, outline: 'none', boxSizing: 'border-box' }}
                              />
                            </div>
                            <div style={{ gridColumn: '1 / -1' }}>
                              <div style={{ fontSize: 10, fontWeight: 800, color: '#6D28D9', marginBottom: 4 }}>📦 Harga Modal Rp (opsional)</div>
                              <input
                                type="number"
                                placeholder="Induk produk"
                                value={v.costPrice ?? ''}
                                disabled={isViewOnly}
                                onChange={e => {
                                  const upd = [...formData.variants];
                                  upd[i] = { ...upd[i], costPrice: e.target.value };
                                  setFormData({ ...formData, variants: upd });
                                }}
                                style={{ width: '100%', padding: '8px 10px', border: '1.5px solid #DDD6FE', borderRadius: 10, fontSize: 12, outline: 'none', boxSizing: 'border-box' }}
                              />
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                    {!isViewOnly && (
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, variants: [...formData.variants, { ...EMPTY_VARIANT }] })}
                        style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 4, width: '100%', justifyContent: 'center', background: '#EDE9FE', color: '#6D28D9', border: '1.5px dashed #A78BFA', borderRadius: 12, padding: '10px 14px', fontWeight: 800, fontSize: 12, cursor: 'pointer' }}
                      >
                        <Plus size={14} /> Tambah Varian Baru
                      </button>
                    )}
                    <p style={{ fontSize: 10, color: '#7C3AED', margin: '4px 0 0', opacity: 0.85, textAlign: 'center' }}>
                      * Kosongkan harga/modal varian untuk menyamakan dengan data produk utama.
                    </p>
                  </div>
                )}
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 12, gap: 10 }}>
                {isAdmin && formData.id && !isViewOnly ? (
                  <button type="button" onClick={() => handleDelete(formData.id)} style={{ display: 'flex', alignItems: 'center', gap: 6, background: '#FEE2E2', color: '#DC2626', border: 'none', borderRadius: 12, padding: '10px 16px', fontWeight: 800, fontSize: 13, cursor: 'pointer' }}>
                    <Trash2 size={15} /> Hapus Produk
                  </button>
                ) : <div />}
                
                <div style={{ display: 'flex', gap: 8 }}>
                  <button type="button" onClick={() => setIsModalOpen(false)} style={{ background: '#F1F5F9', color: '#475569', border: 'none', borderRadius: 12, padding: '10px 18px', fontWeight: 800, fontSize: 13, cursor: 'pointer' }}>Batal</button>
                  {!isViewOnly && (
                    <button type="submit" style={{ background: 'linear-gradient(135deg, #4F46E5 0%, #3730A3 100%)', color: 'white', border: 'none', borderRadius: 12, padding: '10px 22px', fontWeight: 800, fontSize: 13, cursor: 'pointer', boxShadow: '0 4px 12px rgba(79, 70, 229, 0.25)' }}>Simpan</button>
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
