import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Search, AlertTriangle, Eye, Tag } from 'lucide-react';
import api from '../api';
import { useAuth, useIsAdmin, useDemoBlock, DEMO_LIMITS } from '../AuthContext';


const EMPTY_VARIANT = { name: '', stock: '', price: '' };

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
    if (isDemo && products.length <= DEMO_LIMITS.PRODUCTS) {
      // Izinkan hapus di demo agar tidak stuck
    } else if (isDemo) {
      showDemoBlock('Menghapus produk hanya tersedia di akun berbayar.');
      return;
    }
    if (window.confirm('Yakin ingin menghapus produk ini?')) {

      try {
        await api.delete(`/products/${id}`);
        setIsModalOpen(false);
        fetchProducts();
      } catch (err) {
        console.error('Failed to delete product', err);
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
      <div className="header-actions">
        <div>
          <h1>Katalog Produk</h1>
          {!isAdmin && (
            <p style={{ margin: '2px 0 0', fontSize: '0.8rem', color: '#6B7280' }}>
              👁️ Mode lihat saja — Hubungi Admin untuk mengubah produk
            </p>
          )}
        </div>
        {isAdmin && (
          <button className="btn btn-primary" onClick={() => handleOpenModal()}>
            <Plus size={18} /> Tambah Produk
          </button>
        )}
      </div>

      {/* Low Stock Alert Banner */}
      {products.filter(p => p.stock <= LOW_STOCK_THRESHOLD && p.stock > 0).length > 0 && (
        <div style={{ background: '#FEF9C3', border: '1px solid #FDE047', borderRadius: 12, padding: '10px 16px', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10 }}>
          <AlertTriangle size={20} color="#CA8A04" />
          <span style={{ fontWeight: 600, color: '#854D0E', fontSize: 14 }}>
            ⚠️ {products.filter(p => p.stock <= LOW_STOCK_THRESHOLD && p.stock > 0).length} produk stok menipis (≤ {LOW_STOCK_THRESHOLD} {' '}item):
            {' '}{products.filter(p => p.stock <= LOW_STOCK_THRESHOLD && p.stock > 0).map(p => `${p.name} (${p.stock})`).join(', ')}
          </span>
        </div>
      )}
      {products.filter(p => p.stock === 0).length > 0 && (
        <div style={{ background: '#FEE2E2', border: '1px solid #FCA5A5', borderRadius: 12, padding: '10px 16px', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 10 }}>
          <AlertTriangle size={20} color="#DC2626" />
          <span style={{ fontWeight: 600, color: '#991B1B', fontSize: 14 }}>
            🚫 Stok habis: {products.filter(p => p.stock === 0).map(p => p.name).join(', ')}
          </span>
        </div>
      )}

      <div className="glass-panel search-bar">
        <Search size={20} className="text-gray-400" />
        <input
          type="text"
          placeholder="Cari produk (Nama)..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>

      <div className="glass-panel table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Gambar</th>
              <th>Nama Produk</th>
              <th>Harga Jual</th>
              <th>Stok</th>
              <th>Satuan</th>
              <th>Varian / Grosir</th>
            </tr>
          </thead>
          <tbody>
            {filteredProducts.length > 0 ? (
              filteredProducts.map((product) => {
                const margin = getMargin(product.price, product.costPrice);
                return (
                  <tr
                    key={product.id}
                    onClick={() => handleOpenModal(product, !isAdmin)}
                    style={{ cursor: 'pointer' }}
                    className="hover:bg-gray-50 transition-colors"
                  >
                    <td>
                      {product.image ? (
                        <img src={product.image} alt={product.name} style={{ width: '40px', height: '40px', objectFit: 'contain', borderRadius: '4px' }} />
                      ) : (
                        <div style={{ width: '40px', height: '40px', backgroundColor: '#e5e7eb', borderRadius: '4px' }}></div>
                      )}
                    </td>
                    <td className="font-semibold">{product.name}</td>
                    <td className="font-bold text-indigo-700">Rp {product.price.toLocaleString('id-ID')}</td>
                    <td>{product.stock}</td>
                    <td>
                      <span style={{ background: '#EEF2FF', color: '#4F46E5', padding: '2px 8px', borderRadius: 99, fontSize: 12, fontWeight: 700 }}>
                        {product.unit || 'pcs'}
                      </span>
                    </td>
                    <td>
                      {product.variants ? (
                        <span style={{ background: '#EDE9FE', color: '#6D28D9', padding: '2px 8px', borderRadius: 99, fontSize: 12, fontWeight: 700 }}>
                          🎨 {JSON.parse(product.variants).length} Varian
                        </span>
                      ) : product.wholesaleEnabled ? (
                        <span style={{ background: '#FFF7ED', color: '#C2410C', padding: '2px 8px', borderRadius: 99, fontSize: 12, fontWeight: 700 }}>
                          ✓ Grosir
                        </span>
                      ) : (
                        <span style={{ color: '#9CA3AF', fontSize: 12 }}>—</span>
                      )}
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan="6" className="text-center p-4">Tidak ada data produk.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

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

                          {/* Stok + Harga — 2 kolom */}
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
                              <div style={{ fontSize: 11, fontWeight: 600, color: '#7C3AED', marginBottom: 4 }}>Harga Rp (opsional)</div>
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
                      * Stok &amp; Harga kosong → menggunakan nilai produk utama
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
