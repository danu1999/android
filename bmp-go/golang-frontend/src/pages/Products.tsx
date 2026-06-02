import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { PlusCircle, Edit, Trash2, Search, ShoppingBag, Clock, Weight, X } from 'lucide-react';

interface Product {
  ID: number;
  Title: string;
  Unit: string;
  Price: number;
  BeratGram: number;
  CycleTime: number;
  Cavity: number;
  RejectRate: number;
}

const Products: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState<Product>({ ID: 0, Title: '', Unit: '', Price: 0, BeratGram: 0, CycleTime: 0, Cavity: 1, RejectRate: 0 });

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const fetchProducts = () => {
    setLoading(true);
    api.get('/products').then((res) => {
      setProducts(res.data.data);
      setLoading(false);
    }).catch(err => {
      console.error(err);
      setLoading(false);
    });
  };

  useEffect(() => { fetchProducts(); }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload = { 
        Title: formData.Title, 
        Unit: formData.Unit, 
        Price: Number(formData.Price),
        BeratGram: Number(formData.BeratGram),
        CycleTime: Number(formData.CycleTime),
        Cavity: Number(formData.Cavity) || 1,
        RejectRate: Number(formData.RejectRate) || 0
    };
    try {
      if (formData.ID > 0) {
        await api.put(`/products/${formData.ID}`, payload);
      } else {
        await api.post('/products', payload);
      }
      setShowModal(false);
      fetchProducts();
    } catch (err) {
      alert("Gagal menyimpan data barang");
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm("Yakin ingin menghapus barang ini?")) {
      try {
        await api.delete(`/products/${id}`);
        fetchProducts();
      } catch {
        alert("Gagal menghapus barang");
      }
    }
  };

  const openModal = (p?: Product) => {
    if (p) setFormData({ ...p });
    else setFormData({ ID: 0, Title: '', Unit: 'Pcs', Price: 0, BeratGram: 0, CycleTime: 0, Cavity: 1, RejectRate: 0 });
    setShowModal(true);
  };

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }}>
      <div style={{ width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
      <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
      <div style={{ color: '#6c757d' }}>Memuat daftar barang...</div>
    </div>
  );

  const filteredProducts = products.filter(p => 
    p.Title.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div style={{ padding: isMobile ? '15px' : '30px', background: '#f8f9fa', minHeight: '100%', maxWidth: '100vw', boxSizing: 'border-box', overflowX: 'hidden' }}>
      {/* Header Section */}
      <div style={{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', justifyContent: 'space-between', alignItems: isMobile ? 'stretch' : 'center', marginBottom: '25px', gap: '15px' }}>
        <div>
          <h2 style={{ fontSize: isMobile ? '22px' : '28px', fontWeight: '800', margin: 0, color: '#1e293b' }}>Katalog Barang</h2>
          <p style={{ margin: '5px 0 0 0', color: '#64748b', fontSize: '14px' }}>Manajemen stok dan harga satuan</p>
        </div>
        <button 
          onClick={() => openModal()} 
          style={{ 
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', 
            background: 'linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%)', color: 'white', 
            border: 'none', padding: '12px 24px', borderRadius: '12px', cursor: 'pointer', 
            fontWeight: 'bold', boxShadow: '0 4px 10px rgba(13, 110, 253, 0.2)', transition: 'all 0.2s'
          }}
        >
          <PlusCircle size={20} /> Tambah Barang Baru
        </button>
      </div>

      {/* Search Bar */}
      <div style={{ marginBottom: '25px', position: 'relative' }}>
        <Search size={20} style={{ position: 'absolute', left: '15px', top: '12px', color: '#94a3b8' }} />
        <input 
          type="text" 
          placeholder="Cari barang berdasarkan nama..." 
          value={searchTerm} 
          onChange={(e) => setSearchTerm(e.target.value)} 
          style={{ 
            width: '100%', padding: '14px 15px 14px 45px', borderRadius: '12px', 
            border: '1px solid #e2e8f0', outline: 'none', background: 'white', 
            fontSize: '15px', boxShadow: '0 2px 4px rgba(0,0,0,0.02)', transition: 'border 0.2s'
          }} 
          onFocus={(e) => e.target.style.borderColor = '#0d6efd'}
          onBlur={(e) => e.target.style.borderColor = '#e2e8f0'}
        />
      </div>

      {isMobile ? (
        /* Mobile Card View */
        <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '15px' }}>
          {filteredProducts.map(p => (
            <div key={p.ID} style={{ background: 'white', borderRadius: '16px', padding: '18px', boxShadow: '0 4px 6px rgba(0,0,0,0.05)', border: '1px solid #f1f5f9' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                <div>
                  <h3 style={{ margin: 0, fontSize: '17px', fontWeight: '700', color: '#1e293b' }}>{p.Title}</h3>
                  <span style={{ fontSize: '12px', color: '#64748b', background: '#f1f5f9', padding: '2px 8px', borderRadius: '6px', marginTop: '5px', display: 'inline-block' }}>{p.Unit}</span>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '16px', fontWeight: '800', color: '#0d6efd' }}>Rp {p.Price.toLocaleString('id-ID')}</div>
                </div>
              </div>
              
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', background: '#f8fafc', padding: '12px', borderRadius: '10px', marginBottom: '15px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#64748b' }}>
                  <Weight size={14} /> {p.BeratGram} g
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#64748b' }}>
                  <Clock size={14} /> {p.CycleTime} s
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#64748b' }}>
                  Cavity: {p.Cavity || 1}
                </div>
                {(p.RejectRate ?? 0) > 0 && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#dc2626', fontWeight: '600' }}>
                    Reject: {p.RejectRate}%
                  </div>
                )}
              </div>

              <div style={{ display: 'flex', gap: '10px' }}>
                <button onClick={() => openModal(p)} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', padding: '10px', background: '#fef9c3', color: '#854d0e', border: 'none', borderRadius: '8px', fontWeight: 'bold', fontSize: '13px' }}>
                  <Edit size={16} /> Edit
                </button>
                <button onClick={() => handleDelete(p.ID)} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', padding: '10px', background: '#fee2e2', color: '#b91c1c', border: 'none', borderRadius: '8px', fontWeight: 'bold', fontSize: '13px' }}>
                  <Trash2 size={16} /> Hapus
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        /* Desktop Table View */
        <div style={{ background: 'white', borderRadius: '16px', padding: '10px', boxShadow: '0 4px 6px rgba(0,0,0,0.05)', border: '1px solid #f1f5f9', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'separate', borderSpacing: '0 10px', textAlign: 'left' }}>
            <thead>
              <tr style={{ color: '#64748b', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '1px' }}>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Barang</th>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Satuan</th>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Berat</th>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Cycle Time</th>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Cavity</th>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Reject %</th>
                <th style={{ padding: '0 20px', fontWeight: '700' }}>Harga</th>
                <th style={{ padding: '0 20px', fontWeight: '700', textAlign: 'center' }}>Aksi</th>
              </tr>
            </thead>
            <tbody>
              {filteredProducts.map(p => (
                <tr key={p.ID} style={{ background: '#ffffff', transition: 'background 0.2s' }}>
                  <td style={{ padding: '15px 20px', fontWeight: '700', color: '#1e293b', borderBottom: '1px solid #f1f5f9' }}>{p.Title}</td>
                  <td style={{ padding: '15px 20px', color: '#64748b', borderBottom: '1px solid #f1f5f9' }}>{p.Unit}</td>
                  <td style={{ padding: '15px 20px', color: '#64748b', borderBottom: '1px solid #f1f5f9' }}>{p.BeratGram} g</td>
                  <td style={{ padding: '15px 20px', color: '#64748b', borderBottom: '1px solid #f1f5f9' }}>{p.CycleTime} s</td>
                  <td style={{ padding: '15px 20px', color: '#64748b', borderBottom: '1px solid #f1f5f9' }}>{p.Cavity || 1}</td>
                  <td style={{ padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }}>
                    {(p.RejectRate ?? 0) > 0
                      ? <span style={{ background: '#fee2e2', color: '#b91c1c', padding: '2px 8px', borderRadius: '6px', fontSize: '12px', fontWeight: '700' }}>{p.RejectRate}%</span>
                      : <span style={{ color: '#94a3b8', fontSize: '12px' }}>-</span>
                    }
                  </td>
                  <td style={{ padding: '15px 20px', color: '#0d6efd', fontWeight: '800', borderBottom: '1px solid #f1f5f9' }}>Rp {p.Price.toLocaleString('id-ID')}</td>
                  <td style={{ padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }}>
                    <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                      <button onClick={() => openModal(p)} style={{ background: '#fef9c3', color: '#854d0e', border: 'none', padding: '8px', borderRadius: '8px', cursor: 'pointer' }}><Edit size={16}/></button>
                      <button onClick={() => handleDelete(p.ID)} style={{ background: '#fee2e2', color: '#b91c1c', border: 'none', padding: '8px', borderRadius: '8px', cursor: 'pointer' }}><Trash2 size={16}/></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {filteredProducts.length === 0 && !loading && (
        <div style={{ textAlign: 'center', padding: '40px', color: '#64748b' }}>
          <ShoppingBag size={48} style={{ margin: '0 auto 15px', opacity: 0.3, display: 'block' }} />
          Barang tidak ditemukan.
        </div>
      )}

      {/* MODAL FORM */}
      {showModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1200, padding: '15px', backdropFilter: 'blur(4px)' }}>
          <div style={{ background: 'white', padding: isMobile ? '20px' : '30px', borderRadius: '20px', width: '100%', maxWidth: '450px', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)', position: 'relative' }}>
            <button onClick={() => setShowModal(false)} style={{ position: 'absolute', right: '15px', top: '15px', background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer' }}><X size={24} /></button>
            
            <h3 style={{ marginBottom: '25px', fontSize: '20px', fontWeight: '800', color: '#1e293b' }}>{formData.ID > 0 ? 'Update' : 'Tambah'} Data Barang</h3>
            
            <form onSubmit={handleSave}>
              <div style={{ marginBottom: '18px' }}>
                <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }}>Nama Barang</label>
                <input required type="text" value={formData.Title} onChange={e => setFormData({...formData, Title: e.target.value})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '10px', boxSizing: 'border-box', outline: 'none' }} placeholder="Contoh: Pot Hitam 10" />
              </div>
              
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '18px' }}>
                <div>
                    <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }}>Satuan</label>
                    <input type="text" value={formData.Unit} onChange={e => setFormData({...formData, Unit: e.target.value})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '10px', boxSizing: 'border-box', outline: 'none' }} placeholder="Pcs/Lusin" />
                </div>
                <div>
                    <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }}>Harga (Rp)</label>
                    <input type="number" required value={formData.Price} onChange={e => setFormData({...formData, Price: e.target.value as any})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '10px', boxSizing: 'border-box', outline: 'none' }} />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '18px' }}>
                <div>
                    <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }}>Berat (Gram)</label>
                    <input type="number" step="0.01" value={formData.BeratGram} onChange={e => setFormData({...formData, BeratGram: e.target.value as any})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '10px', boxSizing: 'border-box', outline: 'none' }} />
                </div>
                <div>
                    <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }}>Cycle Time (s)</label>
                    <input type="number" step="0.01" value={formData.CycleTime} onChange={e => setFormData({...formData, CycleTime: e.target.value as any})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '10px', boxSizing: 'border-box', outline: 'none' }} />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '18px', background: '#f0f9ff', padding: '15px', borderRadius: '12px', border: '1px solid #bae6fd' }}>
                <div>
                    <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#0369a1' }}>Cavity (cetakan/shot)</label>
                    <input type="number" min="1" value={formData.Cavity || 1} onChange={e => setFormData({...formData, Cavity: e.target.value as any})} style={{ width: '100%', padding: '12px', border: '1px solid #bae6fd', borderRadius: '10px', boxSizing: 'border-box', outline: 'none', background: 'white' }} />
                </div>
                <div>
                    <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#0369a1' }}>Reject Rate (%)</label>
                    <input type="number" min="0" max="100" step="0.1" value={formData.RejectRate || 0} onChange={e => setFormData({...formData, RejectRate: e.target.value as any})} style={{ width: '100%', padding: '12px', border: '1px solid #bae6fd', borderRadius: '10px', boxSizing: 'border-box', outline: 'none', background: 'white' }} />
                </div>
                <div style={{ gridColumn: '1/-1' }}>
                  <small style={{ color: '#0369a1', fontSize: '11px' }}>⚙️ Digunakan oleh Kalkulator HPP untuk menghitung output dan modal bahan secara akurat.</small>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px' }}>
                <button type="button" onClick={() => setShowModal(false)} style={{ flex: 1, padding: '12px', background: '#f1f5f9', color: '#64748b', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: 'bold' }}>Batal</button>
                <button type="submit" style={{ flex: 2, padding: '12px', background: '#0d6efd', color: 'white', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: 'bold' }}>Simpan Barang</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Products;
