import React, { useState, useEffect } from 'react';
import api from '../api';
import { Plus, Edit2, Trash2, X, Store, MapPin, Phone, RefreshCw } from 'lucide-react';

export default function OutletManagement() {
  const [outlets, setOutlets] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  
  // Form state
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  const [phone, setPhone] = useState('');

  const fetchOutlets = async () => {
    setLoading(true);
    try {
      const res = await api.get('/outlets');
      setOutlets(res.data);
    } catch (err) {
      setError('Gagal memuat daftar outlet');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOutlets();
  }, []);

  const openCreateModal = () => {
    setEditingId(null);
    setName('');
    setAddress('');
    setPhone('');
    setError('');
    setSuccess('');
    setShowModal(true);
  };

  const openEditModal = (outlet) => {
    setEditingId(outlet.id);
    setName(outlet.name);
    setAddress(outlet.address || '');
    setPhone(outlet.phone || '');
    setError('');
    setSuccess('');
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Nama outlet wajib diisi');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      if (editingId) {
        await api.put(`/outlets/${editingId}`, { name, address, phone });
        setSuccess('Outlet berhasil diperbarui');
      } else {
        await api.post('/outlets', { name, address, phone });
        setSuccess('Outlet baru berhasil ditambahkan');
      }
      setShowModal(false);
      fetchOutlets();
    } catch (err) {
      setError(err.response?.data?.error || 'Gagal menyimpan data outlet');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Apakah Anda yakin ingin menghapus outlet "${name}"?`)) return;

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      await api.delete(`/outlets/${id}`);
      setSuccess('Outlet berhasil dihapus');
      fetchOutlets();
    } catch (err) {
      setError(err.response?.data?.error || 'Gagal menghapus outlet');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container" style={{ padding: '1.5rem', maxWidth: '1200px', margin: '0 auto', fontFamily: "'Inter', sans-serif" }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 900, color: 'var(--text-main)', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Store size={24} style={{ color: '#4F46E5' }} /> Kelola Cabang &amp; Outlet
          </h1>
          <p style={{ color: '#6B7280', fontSize: '0.85rem', margin: '4px 0 0' }}>
            Tambahkan dan kelola cabang toko Anda untuk pembagian stok dan laporan keuangan terisolasi.
          </p>
        </div>
        <button
          onClick={openCreateModal}
          style={{
            background: 'linear-gradient(135deg, #4F46E5, #4338CA)',
            color: 'white',
            border: 'none',
            borderRadius: '10px',
            padding: '10px 16px',
            fontWeight: 700,
            fontSize: '0.85rem',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(79, 70, 229, 0.25)'
          }}
        >
          <Plus size={16} /> Tambah Outlet baru
        </button>
      </div>

      {/* Alerts */}
      {error && (
        <div style={{ background: '#FEE2E2', border: '1px solid #FCA5A5', color: '#B91C1C', padding: '12px', borderRadius: '10px', fontSize: '0.85rem', fontWeight: 600, marginBottom: '1rem' }}>
          ⚠️ {error}
        </div>
      )}
      {success && (
        <div style={{ background: '#D1FAE5', border: '1px solid #6EE7B7', color: '#065F46', padding: '12px', borderRadius: '10px', fontSize: '0.85rem', fontWeight: 600, marginBottom: '1rem' }}>
          ✅ {success}
        </div>
      )}

      {/* Outlets Grid */}
      {loading && outlets.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#6B7280' }}>
          <RefreshCw size={24} className="animate-spin" style={{ margin: '0 auto 8px' }} />
          <span>Memuat data outlet...</span>
        </div>
      ) : outlets.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '4rem 2rem', borderRadius: '16px' }}>
          <Store size={48} style={{ color: '#9CA3AF', marginBottom: '1rem' }} />
          <h3 style={{ fontSize: '1.1rem', fontWeight: 800, margin: '0 0 6px', color: 'var(--text-main)' }}>Belum Ada Outlet</h3>
          <p style={{ color: '#6B7280', fontSize: '0.85rem', margin: '0 0 1.5rem', maxWidth: '360px', marginLeft: 'auto', marginRight: 'auto' }}>
            Anda belum menambahkan cabang toko. Klik tombol di atas untuk membuat outlet cabang pertama Anda.
          </p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '20px' }}>
          {outlets.map((outlet) => (
            <div
              key={outlet.id}
              className="glass-panel"
              style={{
                borderRadius: '16px',
                padding: '1.25rem',
                border: '1px solid rgba(0,0,0,0.05)',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                transition: 'all 0.2s'
              }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '10px' }}>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 800, margin: 0, color: 'var(--text-main)' }}>
                    {outlet.name}
                  </h3>
                  <span style={{ fontSize: '0.7rem', background: '#EEF2FF', color: '#4F46E5', padding: '2px 8px', borderRadius: '99px', fontWeight: 700 }}>
                    ID: {outlet.id}
                  </span>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '12px' }}>
                  <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-start', fontSize: '0.8rem', color: '#4B5563' }}>
                    <MapPin size={15} style={{ flexShrink: 0, marginTop: '2px', color: '#6B7280' }} />
                    <span>{outlet.address || 'Alamat tidak diisi'}</span>
                  </div>
                  <div style={{ display: 'flex', gap: '8px', alignItems: 'center', fontSize: '0.8rem', color: '#4B5563' }}>
                    <Phone size={14} style={{ color: '#6B7280' }} />
                    <span>{outlet.phone || 'Nomor telepon tidak diisi'}</span>
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '1.5rem', borderTop: '1px solid rgba(0,0,0,0.05)', paddingTop: '10px' }}>
                <button
                  onClick={() => openEditModal(outlet)}
                  style={{
                    background: '#F3F4F6',
                    border: 'none',
                    borderRadius: '8px',
                    padding: '6px 12px',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    color: '#374151',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    cursor: 'pointer'
                  }}
                >
                  <Edit2 size={12} /> Edit
                </button>
                <button
                  onClick={() => handleDelete(outlet.id, outlet.name)}
                  style={{
                    background: '#FEE2E2',
                    border: 'none',
                    borderRadius: '8px',
                    padding: '6px 12px',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    color: '#DC2626',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    cursor: 'pointer'
                  }}
                >
                  <Trash2 size={12} /> Hapus
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create/Edit Modal */}
      {showModal && (
        <div style={{
          position: 'fixed', inset: 0, zIndex: 9999,
          background: 'rgba(0,0,0,0.4)', backdropFilter: 'blur(4px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem'
        }}>
          <div className="glass-panel" style={{
            background: 'white', borderRadius: '20px', padding: '1.5rem',
            maxWidth: '450px', width: '100%', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h2 style={{ fontSize: '1.2rem', fontWeight: 800, margin: 0, color: '#111827' }}>
                {editingId ? 'Edit Outlet Cabang' : 'Tambah Outlet Cabang Baru'}
              </h2>
              <button onClick={() => setShowModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9CA3AF' }}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <label style={{ fontSize: '0.78rem', fontWeight: 700, color: '#475569' }}>Nama Outlet *</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Contoh: POSBah Cabang Dago"
                  style={{
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1.5px solid #E2E8F0',
                    fontSize: '0.85rem',
                    outline: 'none'
                  }}
                  required
                />
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <label style={{ fontSize: '0.78rem', fontWeight: 700, color: '#475569' }}>Nomor Telepon</label>
                <input
                  type="text"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="Contoh: 0812345678"
                  style={{
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1.5px solid #E2E8F0',
                    fontSize: '0.85rem',
                    outline: 'none'
                  }}
                />
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <label style={{ fontSize: '0.78rem', fontWeight: 700, color: '#475569' }}>Alamat Cabang</label>
                <textarea
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="Masukkan alamat lengkap cabang"
                  rows={3}
                  style={{
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1.5px solid #E2E8F0',
                    fontSize: '0.85rem',
                    outline: 'none',
                    resize: 'none'
                  }}
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                style={{
                  background: 'linear-gradient(135deg, #4F46E5, #4338CA)',
                  color: 'white',
                  border: 'none',
                  borderRadius: '10px',
                  padding: '12px',
                  fontWeight: 700,
                  fontSize: '0.88rem',
                  marginTop: '10px',
                  cursor: 'pointer',
                  boxShadow: '0 4px 12px rgba(79, 70, 229, 0.25)'
                }}
              >
                {loading ? 'Menyimpan...' : editingId ? 'Simpan Perubahan' : 'Tambah Outlet'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
