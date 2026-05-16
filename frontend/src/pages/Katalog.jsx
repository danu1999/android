import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, Search, ChevronDown, ChevronUp } from 'lucide-react';
import api from '../api';

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
  ]
};

export default function Katalog() {
  const [products, setProducts] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState(EMPTY_FORM);

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

  const handleOpenModal = (product = null) => {
    if (product) {
      // Parse wholesalePrices dari JSON string jika ada
      let wholesalePrices = EMPTY_FORM.wholesalePrices;
      if (product.wholesalePrices) {
        try {
          const parsed = JSON.parse(product.wholesalePrices);
          // Isi 5 slot, sisanya kosong
          wholesalePrices = [...parsed, ...EMPTY_FORM.wholesalePrices].slice(0, 5);
        } catch (_) {}
      }
      setFormData({ ...product, wholesalePrices });
    } else {
      setFormData({ ...EMPTY_FORM });
    }
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      // Filter hanya wholesale yang terisi
      const filledWholesale = formData.wholesalePrices.filter(
        w => w.minQty !== '' && w.price !== ''
      ).map(w => ({ minQty: Number(w.minQty), price: Number(w.price) }));

      const payload = {
        ...formData,
        wholesalePrices: formData.wholesaleEnabled && filledWholesale.length > 0 ? filledWholesale : null
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
        <h1>Katalog Produk</h1>
        <button className="btn btn-primary" onClick={() => handleOpenModal()}>
          <Plus size={18} /> Tambah Produk
        </button>
      </div>

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
              <th>Harga Modal</th>
              <th>Margin</th>
              <th>Stok</th>
              <th>Satuan</th>
              <th>Grosir</th>
              <th className="text-right">Aksi</th>
            </tr>
          </thead>
          <tbody>
            {filteredProducts.length > 0 ? (
              filteredProducts.map((product) => {
                const margin = getMargin(product.price, product.costPrice);
                return (
                  <tr key={product.id}>
                    <td>
                      {product.image ? (
                        <img src={product.image} alt={product.name} style={{ width: '40px', height: '40px', objectFit: 'contain', borderRadius: '4px' }} />
                      ) : (
                        <div style={{ width: '40px', height: '40px', backgroundColor: '#e5e7eb', borderRadius: '4px' }}></div>
                      )}
                    </td>
                    <td className="font-semibold">{product.name}</td>
                    <td className="font-bold text-indigo-700">Rp {product.price.toLocaleString('id-ID')}</td>
                    <td className="text-gray-500">
                      {product.costPrice > 0 ? `Rp ${product.costPrice.toLocaleString('id-ID')}` : '-'}
                    </td>
                    <td>
                      {margin !== null ? (
                        <span style={{
                          background: margin >= 20 ? '#DCFCE7' : margin >= 10 ? '#FEF9C3' : '#FEE2E2',
                          color: margin >= 20 ? '#166534' : margin >= 10 ? '#854D0E' : '#991B1B',
                          padding: '2px 8px', borderRadius: 99, fontSize: 12, fontWeight: 700
                        }}>
                          {margin}%
                        </span>
                      ) : '-'}
                    </td>
                    <td>{product.stock}</td>
                    <td>
                      <span style={{ background: '#EEF2FF', color: '#4F46E5', padding: '2px 8px', borderRadius: 99, fontSize: 12, fontWeight: 700 }}>
                        {product.unit || 'pcs'}
                      </span>
                    </td>
                    <td>
                      {product.wholesaleEnabled ? (
                        <span style={{ background: '#FFF7ED', color: '#C2410C', padding: '2px 8px', borderRadius: 99, fontSize: 12, fontWeight: 700 }}>
                          ✓ Aktif
                        </span>
                      ) : (
                        <span style={{ color: '#9CA3AF', fontSize: 12 }}>—</span>
                      )}
                    </td>
                    <td className="text-right action-btns">
                      <button className="btn btn-icon btn-edit" onClick={() => handleOpenModal(product)}>
                        <Edit2 size={16} />
                      </button>
                      <button className="btn btn-icon btn-danger" onClick={() => handleDelete(product.id)}>
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan="9" className="text-center p-4">Tidak ada data produk.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel" style={{ maxHeight: '92vh', overflowY: 'auto', maxWidth: 480, width: '100%' }}>
            <h2>{formData.id ? 'Edit Produk' : 'Tambah Produk'}</h2>
            <form onSubmit={handleSave}>

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
                        <input
                          type="number"
                          placeholder={`Min qty tier ${i + 1}`}
                          value={tier.minQty}
                          onChange={(e) => handleWholesaleChange(i, 'minQty', e.target.value)}
                          style={{ padding: '8px 10px', border: '1px solid #FED7AA', borderRadius: 8, fontSize: 13, outline: 'none' }}
                        />
                        <input
                          type="number"
                          placeholder="Harga grosir"
                          value={tier.price}
                          onChange={(e) => handleWholesaleChange(i, 'price', e.target.value)}
                          style={{ padding: '8px 10px', border: '1px solid #FED7AA', borderRadius: 8, fontSize: 13, outline: 'none' }}
                        />
                      </div>
                    ))}
                    <p style={{ fontSize: 11, color: '#92400E', marginTop: 4 }}>
                      * Maksimal 5 tingkatan harga. Kosongkan baris yang tidak dipakai.
                    </p>
                  </div>
                )}
              </div>

              <div className="modal-actions">
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
