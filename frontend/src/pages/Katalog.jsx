import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, Search } from 'lucide-react';
import api from '../api';

export default function Katalog() {
  const [products, setProducts] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ id: null, name: '', price: '', stock: '', unit: 'pcs', image: '' });

  const fetchProducts = async () => {
    try {
      const res = await api.get('/products');
      setProducts(res.data);
    } catch (err) {
      console.error('Failed to fetch products', err);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, []);

  const handleInputChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
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
        setFormData({ ...formData, image: compressedBase64 });
      };
    };
  };

  const handleOpenModal = (product = null) => {
    if (product) {
      setFormData(product);
    } else {
      setFormData({ id: null, name: '', price: '', stock: '', unit: 'pcs', image: '' });
    }
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      if (formData.id) {
        await api.put(`/products/${formData.id}`, formData);
      } else {
        await api.post('/products', formData);
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
              <th>Harga</th>
              <th>Stok</th>
              <th>Satuan</th>
              <th className="text-right">Aksi</th>
            </tr>
          </thead>
          <tbody>
            {filteredProducts.length > 0 ? (
              filteredProducts.map((product) => (
                <tr key={product.id}>
                  <td>
                    {product.image ? (
                      <img src={product.image} alt={product.name} style={{ width: '40px', height: '40px', objectFit: 'cover', borderRadius: '4px' }} />
                    ) : (
                      <div style={{ width: '40px', height: '40px', backgroundColor: '#e5e7eb', borderRadius: '4px' }}></div>
                    )}
                  </td>
                  <td>{product.name}</td>
                  <td>Rp {product.price.toLocaleString('id-ID')}</td>
                  <td>{product.stock}</td>
                  <td>
                    <span style={{ background: '#EEF2FF', color: '#4F46E5', padding: '2px 8px', borderRadius: 99, fontSize: 12, fontWeight: 700 }}>
                      {product.unit || 'pcs'}
                    </span>
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
              ))
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
          <div className="modal-content glass-panel" style={{ maxHeight: '90vh', overflowY: 'auto' }}>
            <h2>{formData.id ? 'Edit Produk' : 'Tambah Produk'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-group" style={{ textAlign: 'center' }}>
                {formData.image ? (
                  <img src={formData.image} alt="Preview" style={{ width: '100px', height: '100px', objectFit: 'cover', borderRadius: '8px', marginBottom: '10px' }} />
                ) : (
                  <div style={{ width: '100px', height: '100px', backgroundColor: '#e5e7eb', borderRadius: '8px', margin: '0 auto 10px auto', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#9ca3af' }}>No Image</div>
                )}
                <label className="btn btn-secondary" style={{ cursor: 'pointer', display: 'inline-block' }}>
                  Pilih Gambar / Kamera
                  <input type="file" accept="image/*" onChange={handleImageUpload} style={{ display: 'none' }} />
                </label>
              </div>
              <div className="form-group">
                <label>Nama Produk</label>
                <input type="text" name="name" value={formData.name} onChange={handleInputChange} required />
              </div>
              <div className="form-group">
                <label>Harga (Rp)</label>
                <input type="number" name="price" value={formData.price} onChange={handleInputChange} required />
              </div>
              <div className="form-group">
                <label>Stok</label>
                <input type="number" name="stock" value={formData.stock} onChange={handleInputChange} required />
              </div>
              <div className="form-group">
                <label>Satuan</label>
                <select name="unit" value={formData.unit || 'pcs'} onChange={handleInputChange} style={{ width: '100%', padding: '10px 12px', border: '1px solid #E2E8F0', borderRadius: 8, fontSize: 14, outline: 'none', background: '#fff' }}>
                  <option value="pcs">Pcs (Satuan)</option>
                  <option value="Kg">Kg (Kilogram)</option>
                  <option value="Gram">Gram</option>
                  <option value="Liter">Liter</option>
                  <option value="Lusin">Lusin</option>
                  <option value="Pack">Pack</option>
                  <option value="Box">Box</option>
                  <option value="Meter">Meter</option>
                </select>
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
