import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Plus, Edit2, Trash2, Save, X, Tag } from 'lucide-react';
import api from '../api';
import { useIsAdmin } from '../AuthContext';

export default function LayananLaundry() {
  const navigate = useNavigate();
  const isAdmin = useIsAdmin();

  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form States (for Create/Edit)
  const [editingId, setEditingId] = useState(null);
  const [kategori, setKategori] = useState('');
  const [proses, setProses] = useState('');
  const [nama, setNama] = useState('');
  const [harga, setHarga] = useState('');
  const [satuan, setSatuan] = useState('Kg');
  const [waktu, setWaktu] = useState('');
  const [icon, setIcon] = useState('🧺');

  const [showAddForm, setShowAddForm] = useState(false);

  const loadServices = async () => {
    setLoading(true);
    try {
      const res = await api.get('/laundry/services');
      setServices(res.data);
    } catch (err) {
      console.error('Failed to load laundry services:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadServices();
  }, []);

  const handleAdd = async (e) => {
    e.preventDefault();
    if (!kategori || !nama || !harga || !waktu) {
      alert('Semua kolom wajib diisi!');
      return;
    }

    const payload = { kategori, proses, nama, harga: Number(harga), satuan, waktu, icon };
    try {
      const res = await api.post('/laundry/services', payload);
      setServices(prev => [res.data, ...prev]);
      resetForm();
      setShowAddForm(false);
    } catch (err) {
      alert(err.response?.data?.error || 'Gagal menambahkan layanan');
    }
  };

  const handleEditInit = (item) => {
    setEditingId(item.id);
    setKategori(item.kategori);
    setProses(item.proses);
    setNama(item.nama);
    setHarga(item.harga);
    setSatuan(item.satuan);
    setWaktu(item.waktu);
    setIcon(item.icon);
    setShowAddForm(false);
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    const payload = { kategori, proses, nama, harga: Number(harga), satuan, waktu, icon };
    try {
      const res = await api.put(`/laundry/services/${editingId}`, payload);
      setServices(prev => prev.map(s => s.id === editingId ? res.data : s));
      resetForm();
    } catch (err) {
      alert(err.response?.data?.error || 'Gagal memperbarui layanan');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Apakah Anda yakin ingin menghapus layanan ini?')) return;
    try {
      await api.delete(`/laundry/services/${id}`);
      setServices(prev => prev.filter(s => s.id !== id));
    } catch (err) {
      alert(err.response?.data?.error || 'Gagal menghapus layanan');
    }
  };

  const resetForm = () => {
    setEditingId(null);
    setKategori('');
    setProses('');
    setNama('');
    setHarga('');
    setSatuan('Kg');
    setWaktu('');
    setIcon('🧺');
  };

  return (
    <div style={{ padding: '1rem', maxWidth: '800px', margin: '0 auto', fontFamily: "'Inter', sans-serif" }}>
      
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button 
            onClick={() => navigate('/')} 
            style={{ background: 'white', border: '1px solid #E5E7EB', padding: '8px', borderRadius: '12px', cursor: 'pointer', display: 'flex', alignItems: 'center' }}
          >
            <ChevronLeft size={20} color="#374151" />
          </button>
          <h1 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#111827', margin: 0 }}>Pengaturan Paket Laundry</h1>
        </div>
        {isAdmin && !showAddForm && !editingId && (
          <button 
            onClick={() => setShowAddForm(true)}
            style={{
              background: 'linear-gradient(135deg, #EF4444, #DC2626)',
              color: 'white', border: 'none', borderRadius: '12px',
              padding: '10px 18px', fontWeight: 800, fontSize: '0.85rem', cursor: 'pointer',
              boxShadow: '0 4px 12px rgba(239,68,68,0.25)'
            }}
          >
            ➕ Tambah Paket
          </button>
        )}
      </div>

      {/* Add or Edit Form */}
      {(showAddForm || editingId) && (
        <div className="glass-panel" style={{ background: 'white', borderRadius: '18px', padding: '1.25rem', border: '1px solid #E5E7EB', marginBottom: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ fontSize: '1rem', fontWeight: 800, color: '#111827', margin: 0 }}>
              {editingId ? 'Edit Paket Layanan' : 'Tambah Paket Layanan Baru'}
            </h3>
            <button 
              onClick={resetForm}
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#6B7280' }}
            >
              <X size={18} />
            </button>
          </div>

          <form onSubmit={editingId ? handleUpdate : handleAdd} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginBottom: '6px' }}>Kategori Layanan</label>
                <input
                  type="text"
                  required
                  placeholder="Contoh: Kiloan Setrika, Jas, Karpet"
                  value={kategori}
                  onChange={e => setKategori(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '10px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.9rem' }}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginBottom: '6px' }}>Nama Paket</label>
                <input
                  type="text"
                  required
                  placeholder="Contoh: Reguler, Ekspress, VIP"
                  value={nama}
                  onChange={e => setNama(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '10px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.9rem' }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginBottom: '6px' }}>Proses Pencucian (Keterangan)</label>
              <input
                type="text"
                placeholder="Contoh: Cuci >> Kering >> Setrika"
                value={proses}
                onChange={e => setProses(e.target.value)}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '10px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.9rem' }}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(110px, 1fr))', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginBottom: '6px' }}>Harga Tarif (Rp)</label>
                <input
                  type="number"
                  required
                  placeholder="8000"
                  value={harga}
                  onChange={e => setHarga(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '10px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.9rem' }}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginBottom: '6px' }}>Satuan</label>
                <select
                  value={satuan}
                  onChange={e => setSatuan(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '10px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.9rem', background: 'white' }}
                >
                  <option value="Kg">Kg (Kiloan)</option>
                  <option value="Pcs">Pcs (Satuan)</option>
                  <option value="Meter">Meter</option>
                  <option value="Porsi">Porsi / Box</option>
                </select>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginBottom: '6px' }}>Estimasi Waktu</label>
                <input
                  type="text"
                  required
                  placeholder="Contoh: 3 Hari, 24 Jam, Instan"
                  value={waktu}
                  onChange={e => setWaktu(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '10px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '0.9rem' }}
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '12px', alignItems: 'center' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#6B7280', marginBottom: '6px' }}>Emoji Ikon</label>
                <input
                  type="text"
                  maxLength={2}
                  value={icon}
                  onChange={e => setIcon(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '10px', border: '1px solid #D1D5DB', outline: 'none', fontSize: '1rem', textAlign: 'center' }}
                />
              </div>
              <div style={{ paddingTop: '20px', display: 'flex', gap: '8px' }}>
                <button
                  type="submit"
                  style={{
                    flex: 1, background: '#10B981', color: 'white', border: 'none',
                    borderRadius: '10px', padding: '10px', fontWeight: 700, fontSize: '0.88rem',
                    cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px'
                  }}
                >
                  <Save size={16} /> Simpan
                </button>
                <button
                  type="button"
                  onClick={resetForm}
                  style={{
                    background: '#F3F4F6', color: '#374151', border: '1px solid #D1D5DB',
                    borderRadius: '10px', padding: '10px 16px', fontWeight: 700, fontSize: '0.88rem',
                    cursor: 'pointer'
                  }}
                >
                  Batal
                </button>
              </div>
            </div>
          </form>
        </div>
      )}

      {/* Services List Grid */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#9CA3AF' }}>Memuat daftar layanan...</div>
      ) : services.length > 0 ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {services.map(item => (
            <div
              key={item.id}
              style={{
                background: 'white', border: '1px solid #E5E7EB', borderRadius: '16px',
                padding: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                boxShadow: '0 2px 8px rgba(0,0,0,0.02)'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                <div style={{ fontSize: '1.75rem', background: '#F9FAFB', width: '50px', height: '50px', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {item.icon || '🧺'}
                </div>
                <div>
                  <div style={{ fontWeight: 800, fontSize: '0.95rem', color: '#111827' }}>
                    {item.kategori} - {item.nama}
                  </div>
                  <div style={{ fontSize: '0.78rem', color: '#6B7280', marginTop: '2px' }}>
                    {item.proses || 'Proses standar'}
                  </div>
                  <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginTop: '4px' }}>
                    <span style={{ fontSize: '0.88rem', fontWeight: 900, color: '#EF4444' }}>
                      Rp {item.harga.toLocaleString('id-ID')} / {item.satuan}
                    </span>
                    <span style={{ fontSize: '0.75rem', background: '#F3F4F6', color: '#6B7280', padding: '2px 8px', borderRadius: '20px', fontWeight: 700 }}>
                      🕒 {item.waktu}
                    </span>
                  </div>
                </div>
              </div>

              {isAdmin && (
                <div style={{ display: 'flex', gap: '6px' }}>
                  <button
                    onClick={() => handleEditInit(item)}
                    style={{ background: '#F3F4F6', border: 'none', borderRadius: '8px', padding: '8px', cursor: 'pointer', color: '#4B5563' }}
                    title="Edit"
                  >
                    <Edit2 size={14} />
                  </button>
                  <button
                    onClick={() => handleDelete(item.id)}
                    style={{ background: '#FFEBEE', border: 'none', borderRadius: '8px', padding: '8px', cursor: 'pointer', color: '#C62828' }}
                    title="Hapus"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      ) : (
        <div style={{ textAlign: 'center', padding: '4rem', background: 'white', borderRadius: '18px', border: '1px solid #E5E7EB', color: '#9CA3AF' }}>
          Belum ada paket layanan laundry yang dibuat.
        </div>
      )}

    </div>
  );
}
