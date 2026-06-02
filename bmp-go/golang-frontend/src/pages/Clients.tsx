import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { PlusCircle, Edit3, Trash2, Search, X, MapPin, Phone, Wallet, Users, User, ArrowUpRight } from 'lucide-react';

interface Client {
  ID: number;
  ClientName: string;
  Province: string;
  PhoneNumber: string;
  SaldoTitipan: number;
}

const Clients: React.FC = () => {
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({ id: 0, name: '', province: '', phone: '' });
  
  // Search state
  const [searchTerm, setSearchTerm] = useState('');

  const fetchClients = () => {
    setLoading(true);
    api.get('/clients').then((res) => {
      setClients(res.data.data || []);
      setLoading(false);
    }).catch(err => {
      console.error(err);
      setLoading(false);
    });
  };

  useEffect(() => { fetchClients(); }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload = { ClientName: formData.name, Province: formData.province, PhoneNumber: formData.phone };
    try {
      if (formData.id > 0) {
        await api.put(`/clients/${formData.id}`, payload);
      } else {
        await api.post('/clients', payload);
      }
      setShowModal(false);
      fetchClients();
    } catch (err) {
      alert("Gagal menyimpan data klien");
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm("Yakin ingin menghapus klien ini?")) {
      try {
        await api.delete(`/clients/${id}`);
        fetchClients();
      } catch (err) {
        alert("Gagal menghapus klien");
      }
    }
  };

  const openModal = (c?: Client) => {
    if (c) setFormData({ id: c.ID, name: c.ClientName, province: c.Province, phone: c.PhoneNumber });
    else setFormData({ id: 0, name: '', province: '', phone: '' });
    setShowModal(true);
  };

  // Avatar initial gradient generator
  const getAvatarGradient = (name: string) => {
    const char = name.trim().charAt(0).toUpperCase();
    const code = char.charCodeAt(0) || 0;
    const gradients = [
      'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)', // Blue
      'linear-gradient(135deg, #10b981 0%, #047857 100%)', // Green
      'linear-gradient(135deg, #f97316 0%, #c2410c 100%)', // Orange
      'linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)', // Purple
      'linear-gradient(135deg, #ec4899 0%, #be185d 100%)', // Pink
      'linear-gradient(135deg, #06b6d4 0%, #0891b2 100%)', // Cyan
      'linear-gradient(135deg, #6366f1 0%, #4338ca 100%)', // Indigo
    ];
    return gradients[code % gradients.length];
  };

  // Modern filtering logic
  const filteredClients = clients.filter(c => 
    (c.ClientName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (c.Province || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (c.PhoneNumber || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Calculations for summary metrics
  const totalClientsCount = clients.length;
  const totalDeposits = clients.reduce((acc, c) => acc + (c.SaldoTitipan || 0), 0);

  const formatRp = (num: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(num);

  return (
    <div style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto' }}>
      <style>{`
        .client-card {
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }
        .client-card:hover {
          transform: translateY(-4px);
          box-shadow: 0 12px 20px -5px rgba(0, 0, 0, 0.08) !important;
          border-color: #3b82f6 !important;
        }
        .btn-action {
          transition: all 0.2s ease;
        }
        .btn-action:hover {
          transform: scale(1.05);
        }
        .btn-add {
          transition: all 0.2s ease;
        }
        .btn-add:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(13, 110, 253, 0.25);
        }
        .input-focus:focus {
          border-color: #3b82f6 !important;
          box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15) !important;
        }
      `}</style>

      {/* Header Section */}
      <div style={{ display: 'flex', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', gap: '15px', flexWrap: 'wrap' }}>
        <div>
          <h2 style={{ fontSize: '24px', fontWeight: '800', margin: 0, color: '#0f172a' }}>Daftar Klien / Pelanggan</h2>
          <p style={{ margin: '4px 0 0', color: '#64748b', fontSize: '14px' }}>Kelola database pelanggan dan pantau saldo deposit</p>
        </div>
        <button onClick={() => openModal()} className="btn-add" style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'linear-gradient(135deg, #0d6efd 0%, #0b5ed7 100%)', color: 'white', border: 'none', padding: '12px 20px', borderRadius: '12px', fontWeight: '600', cursor: 'pointer', fontSize: '14px' }}>
          <PlusCircle size={18} /> Tambah Klien
        </button>
      </div>

      {/* Summary Cards Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '15px', marginBottom: '20px' }}>
        <div style={{ background: '#white', backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '16px', padding: '18px', display: 'flex', alignItems: 'center', gap: '15px', boxShadow: '0 2px 4px rgba(0,0,0,0.02)' }}>
          <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(59, 130, 246, 0.1)', color: '#3b82f6', display: 'flex', alignItems: 'center', justifySelf: 'center', justifyContent: 'center' }}>
            <Users size={22} />
          </div>
          <div>
            <div style={{ fontSize: '13px', color: '#64748b', fontWeight: '500' }}>Total Pelanggan</div>
            <div style={{ fontSize: '20px', fontWeight: '800', color: '#1e293b', marginTop: '2px' }}>{totalClientsCount} <span style={{ fontSize: '13px', fontWeight: '500', color: '#64748b' }}>Orang</span></div>
          </div>
        </div>

        <div style={{ background: '#white', backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '16px', padding: '18px', display: 'flex', alignItems: 'center', gap: '15px', boxShadow: '0 2px 4px rgba(0,0,0,0.02)' }}>
          <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(16, 185, 129, 0.1)', color: '#10b981', display: 'flex', alignItems: 'center', justifySelf: 'center', justifyContent: 'center' }}>
            <Wallet size={22} />
          </div>
          <div>
            <div style={{ fontSize: '13px', color: '#64748b', fontWeight: '500' }}>Total Saldo Titipan</div>
            <div style={{ fontSize: '20px', fontWeight: '800', color: '#10b981', marginTop: '2px' }}>{formatRp(totalDeposits).replace(',00', '')}</div>
          </div>
        </div>
      </div>

      {/* Modern Search Bar */}
      <div style={{ position: 'relative', marginBottom: '20px' }}>
        <div style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', display: 'flex', alignItems: 'center' }}>
          <Search size={18} />
        </div>
        <input 
          type="text" 
          placeholder="Cari pelanggan berdasarkan nama, provinsi, atau telepon..." 
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="input-focus"
          style={{ 
            width: '100%', 
            padding: '14px 14px 14px 48px', 
            borderRadius: '12px', 
            border: '1px solid #cbd5e1', 
            fontSize: '15px',
            backgroundColor: '#ffffff',
            boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
            outline: 'none',
            transition: 'all 0.2s',
            boxSizing: 'border-box'
          }} 
        />
        {searchTerm && (
          <button 
            onClick={() => setSearchTerm('')}
            style={{ position: 'absolute', right: '16px', top: '50%', transform: 'translateY(-50%)', border: 'none', background: 'none', cursor: 'pointer', color: '#64748b', display: 'flex', alignItems: 'center', padding: '4px' }}
          >
            <X size={16} />
          </button>
        )}
      </div>

      {/* Loading state */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '50px 20px', color: '#64748b' }}>
          <div style={{ width: '35px', height: '35px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 15px' }}></div>
          <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
          Memuat data pelanggan...
        </div>
      ) : (
        <>
          {/* Card-based grid list */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '20px' }}>
            {filteredClients.map(c => {
              const initial = c.ClientName ? c.ClientName.trim().charAt(0).toUpperCase() : '?';
              return (
                <div key={c.ID} className="client-card" style={{ background: '#ffffff', borderRadius: '16px', border: '1px solid #e2e8f0', padding: '18px', display: 'flex', flexDirection: 'column', boxShadow: '0 4px 6px rgba(0,0,0,0.01)' }}>
                  
                  {/* Card Header */}
                  <div style={{ display: 'flex', alignItems: 'center', marginBottom: '14px' }}>
                    <div style={{ width: '42px', height: '42px', borderRadius: '12px', background: getAvatarGradient(c.ClientName || ''), color: '#ffffff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: '800', fontSize: '16px', flexShrink: 0 }}>
                      {initial}
                    </div>
                    <div style={{ marginLeft: '12px', minWidth: 0, flex: 1 }}>
                      <h3 style={{ fontSize: '16px', fontWeight: '800', margin: 0, color: '#1e293b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={c.ClientName}>
                        {c.ClientName}
                      </h3>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#64748b', fontSize: '12px', marginTop: '2px' }}>
                        <MapPin size={12} color="#94a3b8" />
                        <span>{c.Province || 'Provinsi tidak diisi'}</span>
                      </div>
                    </div>
                  </div>

                  {/* Divider */}
                  <div style={{ height: '1px', backgroundColor: '#f1f5f9', margin: '0 0 12px' }}></div>

                  {/* Card Details */}
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '13px' }}>
                      <span style={{ color: '#64748b', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Phone size={13} color="#94a3b8" /> Telepon
                      </span>
                      {c.PhoneNumber ? (
                        <a href={`tel:${c.PhoneNumber}`} style={{ color: '#0d6efd', fontWeight: '600', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '3px' }}>
                          {c.PhoneNumber} <ArrowUpRight size={12} />
                        </a>
                      ) : (
                        <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>-</span>
                      )}
                    </div>

                    {/* Deposit Section Container */}
                    <div style={{ background: '#f8fafc', borderRadius: '12px', padding: '10px 12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '4px' }}>
                      <span style={{ fontSize: '12px', fontWeight: '600', color: '#64748b', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Wallet size={13} color="#10b981" /> Deposit / Saldo
                      </span>
                      <span style={{ fontSize: '14px', fontWeight: '800', color: c.SaldoTitipan > 0 ? '#10b981' : '#64748b' }}>
                        {formatRp(c.SaldoTitipan).replace(',00', '')}
                      </span>
                    </div>
                  </div>

                  {/* Card Footer Actions */}
                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '16px', borderTop: '1px solid #f1f5f9', paddingTop: '12px' }}>
                    <button 
                      onClick={() => openModal(c)} 
                      className="btn-action"
                      style={{ background: '#fff', border: '1px solid #ffc107', color: '#856404', padding: '8px 12px', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px', fontSize: '12px', fontWeight: '600' }}
                    >
                      <Edit3 size={14}/> Edit
                    </button>
                    <button 
                      onClick={() => handleDelete(c.ID)} 
                      className="btn-action"
                      style={{ background: '#fff', border: '1px solid #fee2e2', color: '#dc3545', padding: '8px 12px', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px', fontSize: '12px', fontWeight: '600' }}
                    >
                      <Trash2 size={14}/> Hapus
                    </button>
                  </div>

                </div>
              );
            })}
          </div>

          {/* Empty state */}
          {filteredClients.length === 0 && (
            <div style={{ textAlign: 'center', padding: '60px 20px', background: '#ffffff', borderRadius: '16px', border: '1px solid #e2e8f0', marginTop: '10px' }}>
              <div style={{ fontSize: '40px', marginBottom: '10px' }}>🔍</div>
              <h3 style={{ fontSize: '16px', fontWeight: '800', color: '#1e293b', margin: '0 0 4px' }}>Pelanggan Tidak Ditemukan</h3>
              <p style={{ color: '#64748b', fontSize: '13px', margin: 0 }}>Coba gunakan kata kunci pencarian lain atau buat klien baru.</p>
            </div>
          )}
        </>
      )}

      {/* Modern Modal with Backdrop Blur */}
      {showModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.45)', backdropFilter: 'blur(4px)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, padding: '15px' }}>
          <div style={{ background: 'white', padding: '25px', borderRadius: '20px', width: '100%', maxWidth: '420px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04)', boxSizing: 'border-box' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ fontSize: '18px', fontWeight: '800', color: '#1e293b', margin: 0 }}>
                {formData.id > 0 ? '📝 Edit Data Klien' : '👤 Tambah Klien Baru'}
              </h3>
              <button onClick={() => setShowModal(false)} style={{ border: 'none', background: 'none', color: '#94a3b8', cursor: 'pointer', padding: '4px', display: 'flex', alignItems: 'center' }}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSave}>
              <div style={{ marginBottom: '15px' }}>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600', color: '#475569' }}>Nama Klien <span style={{ color: '#ef4444' }}>*</span></label>
                <input 
                  required 
                  type="text" 
                  value={formData.name} 
                  onChange={e => setFormData({...formData, name: e.target.value})} 
                  placeholder="Contoh: Toko Berkah"
                  className="input-focus"
                  style={{ width: '100%', padding: '11px 12px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '14px', boxSizing: 'border-box', transition: 'all 0.2s' }} 
                />
              </div>

              <div style={{ marginBottom: '15px' }}>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600', color: '#475569' }}>Provinsi</label>
                <input 
                  type="text" 
                  value={formData.province} 
                  onChange={e => setFormData({...formData, province: e.target.value})} 
                  placeholder="Contoh: Jawa Timur"
                  className="input-focus"
                  style={{ width: '100%', padding: '11px 12px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '14px', boxSizing: 'border-box', transition: 'all 0.2s' }} 
                />
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600', color: '#475569' }}>Nomor Telepon</label>
                <input 
                  type="text" 
                  value={formData.phone} 
                  onChange={e => setFormData({...formData, phone: e.target.value})} 
                  placeholder="Contoh: 08123456789"
                  className="input-focus"
                  style={{ width: '100%', padding: '11px 12px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '14px', boxSizing: 'border-box', transition: 'all 0.2s' }} 
                />
              </div>

              <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', borderTop: '1px solid #f1f5f9', paddingTop: '15px' }}>
                <button type="button" onClick={() => setShowModal(false)} style={{ padding: '10px 16px', background: '#f1f5f9', color: '#475569', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: '600', fontSize: '13px' }}>Batal</button>
                <button type="submit" style={{ padding: '10px 16px', background: 'linear-gradient(135deg, #0d6efd 0%, #0b5ed7 100%)', color: 'white', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: '600', fontSize: '13px' }}>Simpan</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Clients;
