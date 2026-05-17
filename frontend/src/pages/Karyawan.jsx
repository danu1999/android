import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, Shield, Users, Crown, Eye, UserCog } from 'lucide-react';
import api from '../api';
import { useAuth, useIsOwner, useIsAdmin, useDemoBlock } from '../AuthContext';

// Role config: gradient, icon, badge
const ROLE_CONFIG = {
  OWNER: {
    grad:    'linear-gradient(135deg,#F59E0B,#D97706)',
    gradSoft:'linear-gradient(135deg,#FEF3C7,#FDE68A)',
    border:  '#FCD34D',
    text:    '#78350F',
    icon:    <Crown size={24} />,
    badge:   { bg: '#FEF3C7', color: '#92400E' },
    label:   '👑 Owner',
  },
  ADMIN: {
    grad:    'linear-gradient(135deg,#6D28D9,#4F46E5)',
    gradSoft:'linear-gradient(135deg,#EDE9FE,#E0E7FF)',
    border:  '#A78BFA',
    text:    '#3730A3',
    icon:    <Shield size={24} />,
    badge:   { bg: '#EDE9FE', color: '#4C1D95' },
    label:   '🛡️ Admin',
  },
  KASIR: {
    grad:    'linear-gradient(135deg,#3B82F6,#2563EB)',
    gradSoft:'linear-gradient(135deg,#EFF6FF,#DBEAFE)',
    border:  '#93C5FD',
    text:    '#1E3A8A',
    icon:    <Users size={24} />,
    badge:   { bg: '#DBEAFE', color: '#1E40AF' },
    label:   '🧾 Kasir',
  },
};

const getRoleCfg = (role) => ROLE_CONFIG[role] || ROLE_CONFIG['KASIR'];

export default function Karyawan() {
  const { user }  = useAuth();
  const isOwner   = useIsOwner();
  const isAdmin   = useIsAdmin();
  const { showDemoBlock, isDemo } = useDemoBlock();

  const [employees,    setEmployees]    = useState([]);
  const [isModalOpen,  setIsModalOpen]  = useState(false);
  const [isViewOnly,   setIsViewOnly]   = useState(false);
  const [formData,     setFormData]     = useState({ id: null, name: '', role: 'KASIR', pin: '' });

  useEffect(() => { fetchEmployees(); }, []);

  const fetchEmployees = async () => {
    try { const res = await api.get('/employees'); setEmployees(res.data); }
    catch (err) { console.error('Failed to fetch employees', err); }
  };

  const handleOpenModal = (employee = null, viewOnly = false) => {
    if (employees.length >= 10 && !employee) {
      alert('Batas maksimal 10 karyawan telah tercapai.'); return;
    }
    setFormData(employee ? employee : { id: null, name: '', role: 'KASIR', pin: '' });
    setIsViewOnly(viewOnly);
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (isDemo) { showDemoBlock('Mengelola karyawan hanya tersedia di akun berbayar.'); return; }
    if (!isOwner) return;
    try {
      if (formData.id) await api.put(`/employees/${formData.id}`, formData);
      else             await api.post('/employees', formData);
      setIsModalOpen(false);
      fetchEmployees();
    } catch (err) {
      alert(err.response?.data?.error || 'Gagal menyimpan karyawan.');
    }
  };

  const handleDelete = async (id) => {
    if (isDemo) { showDemoBlock('Menghapus karyawan hanya tersedia di akun berbayar.'); return; }
    if (!isOwner) return;
    if (id === user?.id) { alert('Tidak dapat menghapus akun sendiri.'); return; }
    if (window.confirm('Yakin ingin menghapus karyawan ini?')) {
      try {
        await api.delete(`/employees/${id}`);
        setIsModalOpen(false);
        fetchEmployees();
      } catch (err) {
        alert(err.response?.data?.error || 'Gagal menghapus karyawan.');
      }
    }
  };

  const availableRoles = isOwner ? ['KASIR', 'ADMIN', 'OWNER'] : ['KASIR'];

  // Summary counts
  const ownerCount = employees.filter(e => e.role === 'OWNER').length;
  const adminCount = employees.filter(e => e.role === 'ADMIN').length;
  const kasirCount = employees.filter(e => e.role === 'KASIR').length;

  return (
    <div className="page-container">

      {/* ── Header ─────────────────────────────────────────────── */}
      <div style={{
        background: 'linear-gradient(135deg,#4F46E5,#7C3AED)',
        borderRadius: 20, padding: '18px 24px', marginBottom: 20,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        flexWrap: 'wrap', gap: 12,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <div style={{
            width: 48, height: 48, borderRadius: 14,
            background: 'rgba(255,255,255,0.2)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <UserCog size={26} color="white" />
          </div>
          <div>
            <h1 style={{ margin: 0, color: 'white', fontSize: '1.3rem', fontWeight: 900 }}>Manajemen Karyawan</h1>
            <div style={{ color: 'rgba(255,255,255,0.75)', fontSize: 12, marginTop: 2 }}>
              {employees.length}/10 karyawan terdaftar
              {!isOwner && (
                <span style={{ marginLeft: 8, background: 'rgba(255,255,255,0.2)', borderRadius: 99, padding: '1px 8px', fontSize: 11 }}>
                  👁️ Hanya OWNER yang bisa mengelola
                </span>
              )}
            </div>
          </div>
        </div>
        {isOwner && (
          <button
            onClick={() => handleOpenModal()}
            disabled={employees.length >= 10}
            style={{
              display: 'flex', alignItems: 'center', gap: 8,
              background: employees.length >= 10 ? 'rgba(255,255,255,0.1)' : 'white',
              color: employees.length >= 10 ? 'rgba(255,255,255,0.4)' : '#4F46E5',
              border: 'none', borderRadius: 12, padding: '10px 18px',
              fontWeight: 800, fontSize: 14, cursor: employees.length >= 10 ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s',
            }}
          >
            <Plus size={16} /> Tambah Karyawan
          </button>
        )}
      </div>

      {/* ── Summary bar ────────────────────────────────────────── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 10, marginBottom: 20 }}>
        {[
          { role: 'OWNER', count: ownerCount },
          { role: 'ADMIN', count: adminCount },
          { role: 'KASIR', count: kasirCount },
        ].map(({ role, count }) => {
          const cfg = getRoleCfg(role);
          return (
            <div key={role} style={{
              background: cfg.gradSoft, border: `1.5px solid ${cfg.border}`,
              borderRadius: 14, padding: '10px 14px',
              display: 'flex', alignItems: 'center', gap: 10,
            }}>
              <div style={{
                background: cfg.grad, borderRadius: 10, padding: 8,
                display: 'flex', color: 'white', flexShrink: 0,
              }}>
                {role === 'OWNER' ? <Crown size={16} /> : role === 'ADMIN' ? <Shield size={16} /> : <Users size={16} />}
              </div>
              <div>
                <div style={{ fontSize: 20, fontWeight: 900, color: cfg.text, lineHeight: 1 }}>{count}</div>
                <div style={{ fontSize: 10, fontWeight: 700, color: cfg.text, opacity: 0.7 }}>{role}</div>
              </div>
            </div>
          );
        })}
      </div>

      {/* ── Cards grid ─────────────────────────────────────────── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px,1fr))', gap: 14 }}>
        {employees.length > 0 ? employees.map(emp => {
          const cfg    = getRoleCfg(emp.role);
          const isSelf = emp.id === user?.id;
          return (
            <div
              key={emp.id}
              style={{
                background: 'white',
                border: `1.5px solid ${cfg.border}`,
                borderRadius: 18, overflow: 'hidden',
                boxShadow: '0 2px 12px rgba(0,0,0,0.06)',
                transition: 'transform 0.18s, box-shadow 0.18s',
                cursor: 'pointer',
              }}
              onMouseEnter={e => {
                e.currentTarget.style.transform = 'translateY(-3px)';
                e.currentTarget.style.boxShadow = '0 8px 24px rgba(0,0,0,0.13)';
              }}
              onMouseLeave={e => {
                e.currentTarget.style.transform = '';
                e.currentTarget.style.boxShadow = '0 2px 12px rgba(0,0,0,0.06)';
              }}
            >
              {/* Gradient top strip */}
              <div style={{ height: 6, background: cfg.grad }} />

              {/* Card body */}
              <div style={{ padding: '16px 18px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
                  {/* Avatar */}
                  <div style={{
                    width: 52, height: 52, borderRadius: 14,
                    background: cfg.grad, flexShrink: 0,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white',
                    boxShadow: `0 4px 12px ${cfg.border}`,
                  }}>
                    {cfg.icon}
                  </div>

                  {/* Name + role */}
                  <div style={{ minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                      <span style={{ fontWeight: 800, fontSize: 15, color: '#111827', wordBreak: 'break-word' }}>
                        {emp.name}
                      </span>
                      {isSelf && (
                        <span style={{ fontSize: 10, background: '#EEF2FF', color: '#4F46E5', padding: '1px 7px', borderRadius: 99, fontWeight: 700, flexShrink: 0 }}>
                          Saya
                        </span>
                      )}
                    </div>
                    <span style={{
                      display: 'inline-block', marginTop: 4, fontSize: 11, fontWeight: 800,
                      background: cfg.badge.bg, color: cfg.badge.color,
                      padding: '2px 10px', borderRadius: 99,
                    }}>
                      {cfg.label}
                    </span>
                  </div>
                </div>

                {/* PIN row */}
                <div style={{
                  background: '#F8FAFC', borderRadius: 10,
                  padding: '7px 12px', fontSize: 12, color: '#6B7280',
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  marginBottom: 12, border: '1px solid #F1F5F9',
                }}>
                  <span style={{ fontWeight: 600 }}>PIN Akses</span>
                  <span style={{ letterSpacing: 4, fontWeight: 800, color: '#9CA3AF' }}>••••</span>
                </div>

                {/* Actions */}
                <div style={{ display: 'flex', gap: 8 }}>
                  {isOwner ? (
                    <>
                      <button
                        onClick={() => handleOpenModal(emp, false)}
                        style={{
                          flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
                          background: cfg.gradSoft, border: `1px solid ${cfg.border}`,
                          color: cfg.text, borderRadius: 10, padding: '8px',
                          fontWeight: 700, fontSize: 12, cursor: 'pointer',
                          transition: 'all 0.15s',
                        }}
                      >
                        <Edit2 size={13} /> Edit
                      </button>
                      {!isSelf && (
                        <button
                          onClick={() => handleDelete(emp.id)}
                          style={{
                            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
                            background: '#FEF2F2', border: '1px solid #FECACA',
                            color: '#DC2626', borderRadius: 10, padding: '8px 12px',
                            fontWeight: 700, fontSize: 12, cursor: 'pointer',
                            transition: 'all 0.15s',
                          }}
                        >
                          <Trash2 size={13} />
                        </button>
                      )}
                    </>
                  ) : (
                    <button
                      onClick={() => handleOpenModal(emp, true)}
                      style={{
                        flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
                        background: '#F9FAFB', border: '1px solid #E5E7EB',
                        color: '#6B7280', borderRadius: 10, padding: '8px',
                        fontWeight: 600, fontSize: 12, cursor: 'pointer',
                      }}
                    >
                      <Eye size={13} /> Lihat Detail
                    </button>
                  )}
                </div>
              </div>
            </div>
          );
        }) : (
          <div style={{
            gridColumn: '1/-1', textAlign: 'center', padding: '48px 16px',
            color: '#9CA3AF', fontSize: 14,
          }}>
            <Users size={40} style={{ marginBottom: 12, opacity: 0.3 }} />
            <div>Belum ada karyawan terdaftar.</div>
          </div>
        )}
      </div>

      {/* ── Modal ──────────────────────────────────────────────── */}
      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel" style={{ maxWidth: 420, width: '100%' }}>

            {/* Modal header strip */}
            <div style={{
              background: getRoleCfg(formData.role).grad,
              margin: '-2rem -2rem 1.5rem',
              padding: '18px 24px', borderRadius: '16px 16px 0 0',
              display: 'flex', alignItems: 'center', gap: 12, color: 'white',
            }}>
              <div style={{ background: 'rgba(255,255,255,0.2)', borderRadius: 10, padding: 8, display: 'flex' }}>
                {isViewOnly ? <Eye size={20} /> : formData.id ? <Edit2 size={20} /> : <Plus size={20} />}
              </div>
              <div>
                <div style={{ fontWeight: 800, fontSize: 16 }}>
                  {isViewOnly ? 'Detail Karyawan' : formData.id ? 'Edit Karyawan' : 'Tambah Karyawan'}
                </div>
                {isViewOnly && <div style={{ fontSize: 11, opacity: 0.8 }}>Mode hanya lihat</div>}
              </div>
            </div>

            <form onSubmit={isViewOnly ? (e) => { e.preventDefault(); setIsModalOpen(false); } : handleSave}>
              {/* Nama */}
              <div className="form-group">
                <label style={{ fontWeight: 700, color: '#374151' }}>Nama Karyawan</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={e => setFormData({ ...formData, name: e.target.value })}
                  required disabled={isViewOnly}
                  style={{ border: `1.5px solid ${getRoleCfg(formData.role).border}` }}
                />
              </div>

              {/* Role */}
              <div className="form-group">
                <label style={{ fontWeight: 700, color: '#374151' }}>Peran (Role)</label>
                <select
                  value={formData.role}
                  onChange={e => setFormData({ ...formData, role: e.target.value })}
                  disabled={isViewOnly}
                  style={{ border: `1.5px solid ${getRoleCfg(formData.role).border}` }}
                >
                  {availableRoles.map(r => (
                    <option key={r} value={r}>
                      {r === 'KASIR' ? '🧾 Kasir — Transaksi & lihat katalog'
                        : r === 'ADMIN' ? '🛡️ Admin — Kelola produk, keuangan, pelanggan'
                        : '👑 Owner — Akses penuh + kelola karyawan'}
                    </option>
                  ))}
                </select>
              </div>

              {/* PIN */}
              {!isViewOnly && (
                <div className="form-group">
                  <label style={{ fontWeight: 700, color: '#374151' }}>
                    PIN Rahasia {formData.id ? '(kosongkan jika tidak diubah)' : '(untuk Login)'}
                  </label>
                  <input
                    type="text"
                    maxLength="6"
                    value={formData.pin}
                    onChange={e => setFormData({ ...formData, pin: e.target.value })}
                    required={!formData.id}
                    placeholder="Contoh: 123456"
                    style={{ border: `1.5px solid ${getRoleCfg(formData.role).border}`, letterSpacing: 4, fontWeight: 700 }}
                  />
                </div>
              )}

              {/* Modal actions */}
              <div className="modal-actions" style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                {isOwner && !isViewOnly && formData.id && (
                  <button
                    type="button"
                    onClick={() => handleDelete(formData.id)}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 6,
                      background: '#FEE2E2', color: '#DC2626',
                      border: 'none', borderRadius: 10, padding: '10px 14px',
                      fontWeight: 700, fontSize: 13, cursor: 'pointer', marginRight: 'auto',
                    }}
                  >
                    <Trash2 size={14} /> Hapus
                  </button>
                )}
                <button type="button" className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>
                  {isViewOnly ? 'Tutup' : 'Batal'}
                </button>
                {isOwner && !isViewOnly && (
                  <button
                    type="submit"
                    style={{
                      display: 'flex', alignItems: 'center', gap: 6,
                      background: getRoleCfg(formData.role).grad,
                      color: 'white', border: 'none', borderRadius: 10,
                      padding: '10px 20px', fontWeight: 800, fontSize: 14, cursor: 'pointer',
                    }}
                  >
                    Simpan
                  </button>
                )}
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
