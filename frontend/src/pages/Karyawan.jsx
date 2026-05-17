import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, Shield, Users, Crown, Eye } from 'lucide-react';
import api from '../api';
import { useAuth, useIsOwner, useIsAdmin, useDemoBlock } from '../AuthContext';


export default function Karyawan() {
  const { user } = useAuth();
  const isOwner = useIsOwner();
  const isAdmin = useIsAdmin();
  const { showDemoBlock, isDemo } = useDemoBlock();

  const [employees, setEmployees] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isViewOnly, setIsViewOnly] = useState(false);
  const [formData, setFormData] = useState({ id: null, name: '', role: 'KASIR', pin: '' });

  useEffect(() => {
    fetchEmployees();
  }, []);

  const fetchEmployees = async () => {
    try {
      const res = await api.get('/employees');
      setEmployees(res.data);
    } catch (err) {
      console.error('Failed to fetch employees', err);
    }
  };

  const handleOpenModal = (employee = null, viewOnly = false) => {
    if (employees.length >= 10 && !employee) {
      alert('Batas maksimal 10 karyawan telah tercapai.');
      return;
    }
    if (employee) {
      setFormData(employee);
    } else {
      setFormData({ id: null, name: '', role: 'KASIR', pin: '' });
    }
    setIsViewOnly(viewOnly);
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (isDemo) { showDemoBlock('Mengelola karyawan hanya tersedia di akun berbayar.'); return; }
    if (!isOwner) return;

    try {
      if (formData.id) {
        await api.put(`/employees/${formData.id}`, formData);
      } else {
        await api.post('/employees', formData);
      }
      setIsModalOpen(false);
      fetchEmployees();
    } catch (err) {
      console.error('Failed to save employee', err);
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

  const getRoleStyle = (role) => {
    if (role === 'OWNER') return { bg: 'bg-yellow-100', color: 'text-yellow-700', icon: <Crown size={26} />, iconBg: 'bg-yellow-100 text-yellow-600' };
    if (role === 'ADMIN')  return { bg: 'bg-purple-100', color: 'text-purple-700', icon: <Shield size={26} />, iconBg: 'bg-purple-100 text-purple-600' };
    return { bg: 'bg-blue-100', color: 'text-blue-700', icon: <Users size={26} />, iconBg: 'bg-blue-100 text-blue-600' };
  };

  // Roles yang bisa dipilih berdasarkan siapa yang login
  const availableRoles = isOwner
    ? ['KASIR', 'ADMIN', 'OWNER']
    : ['KASIR'];

  return (
    <div className="page-container">
      <div className="header-actions flex flex-col md:flex-row gap-4 justify-between items-center bg-indigo-50 p-6 rounded-2xl border border-indigo-100 mb-6">
        <div>
          <h1 className="text-2xl font-extrabold text-indigo-900 m-0">Manajemen Karyawan</h1>
          <p className="text-indigo-600 mt-1">
            {employees.length}/10 Karyawan terdaftar
            {!isOwner && (
              <span style={{ marginLeft: 8, fontSize: '0.75rem', background: '#FEF3C7', color: '#D97706', padding: '2px 8px', borderRadius: 99, fontWeight: 700 }}>
                👁️ Hanya OWNER yang dapat mengelola karyawan
              </span>
            )}
          </p>
        </div>
        {isOwner && (
          <button
            className="btn bg-indigo-600 text-white hover:bg-indigo-700 w-full md:w-auto justify-center rounded-xl"
            onClick={() => handleOpenModal()}
            disabled={employees.length >= 10}
          >
            <Plus size={18} /> Tambah Karyawan
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {employees.length > 0 ? (
          employees.map(employee => {
            const rs = getRoleStyle(employee.role);
            const isSelf = employee.id === user?.id;
            return (
              <div key={employee.id} className="bg-white rounded-xl shadow-sm p-6 flex flex-col justify-between border border-gray-100 hover:shadow-md transition-shadow">
                <div className="flex items-center gap-4 mb-4">
                  <div className={`w-14 h-14 rounded-full flex items-center justify-center ${rs.iconBg}`}>
                    {rs.icon}
                  </div>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <h3 className="font-bold text-lg text-gray-800 m-0">{employee.name}</h3>
                      {isSelf && <span style={{ fontSize: '0.65rem', background: '#EEF2FF', color: '#4F46E5', padding: '1px 6px', borderRadius: 99, fontWeight: 700 }}>Saya</span>}
                    </div>
                    <span className={`px-2 py-1 text-xs font-bold rounded mt-1 inline-block ${rs.bg} ${rs.color}`}>
                      {employee.role}
                    </span>
                  </div>
                </div>
                <div className="text-sm text-gray-500 mb-4 bg-gray-50 p-2 rounded">
                  <span className="font-semibold">PIN Akses:</span> •••• (Tersimpan)
                </div>
                <div className="flex justify-end gap-2 border-t border-gray-100 pt-4 mt-auto">
                  {isOwner ? (
                    <>
                      <button className="btn btn-icon btn-edit" onClick={() => handleOpenModal(employee, false)} title="Edit">
                        <Edit2 size={16} />
                      </button>
                      {!isSelf && (
                        <button className="btn btn-icon btn-danger" onClick={() => handleDelete(employee.id)} title="Hapus">
                          <Trash2 size={16} />
                        </button>
                      )}
                    </>
                  ) : (
                    <button className="btn btn-icon" style={{ background: '#F3F4F6', color: '#6B7280' }} onClick={() => handleOpenModal(employee, true)} title="Lihat Detail">
                      <Eye size={16} />
                    </button>
                  )}
                </div>
              </div>
            );
          })
        ) : (
          <div className="col-span-full text-center p-8 text-gray-500">
            Belum ada karyawan terdaftar.
          </div>
        )}
      </div>

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel max-w-md w-full">
            <h2 className="mb-4 text-xl font-bold" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {isViewOnly ? <Eye size={20} color="#6B7280" /> : null}
              {isViewOnly ? 'Detail Karyawan' : formData.id ? 'Edit Karyawan' : 'Tambah Karyawan'}
              {isViewOnly && <span style={{ fontSize: '0.7rem', background: '#F3F4F6', color: '#6B7280', padding: '2px 8px', borderRadius: 99, fontWeight: 600 }}>Hanya Lihat</span>}
            </h2>
            <form onSubmit={isViewOnly ? (e) => { e.preventDefault(); setIsModalOpen(false); } : handleSave}>
              <div className="form-group mb-4">
                <label className="block text-sm font-semibold text-gray-700 mb-1">Nama Karyawan</label>
                <input
                  type="text"
                  className="w-full p-2 border border-gray-300 rounded outline-none focus:border-indigo-500"
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  required
                  disabled={isViewOnly}
                />
              </div>
              <div className="form-group mb-4">
                <label className="block text-sm font-semibold text-gray-700 mb-1">Peran (Role)</label>
                <select
                  className="w-full p-2 border border-gray-300 rounded outline-none focus:border-indigo-500"
                  value={formData.role}
                  onChange={(e) => setFormData({...formData, role: e.target.value})}
                  disabled={isViewOnly}
                >
                  {availableRoles.map(r => (
                    <option key={r} value={r}>
                      {r === 'KASIR' ? '🧾 Kasir' : r === 'ADMIN' ? '🛡️ Admin' : '👑 Owner'}
                    </option>
                  ))}
                </select>
                {isOwner && (
                  <div style={{ marginTop: 6, fontSize: '0.75rem', color: '#6B7280' }}>
                    <strong>KASIR</strong>: Hanya transaksi &amp; lihat katalog ·{' '}
                    <strong>ADMIN</strong>: Kelola produk, keuangan, pelanggan ·{' '}
                    <strong>OWNER</strong>: Akses penuh + kelola karyawan
                  </div>
                )}
              </div>
              {!isViewOnly && (
                <div className="form-group mb-6">
                  <label className="block text-sm font-semibold text-gray-700 mb-1">
                    PIN Rahasia {formData.id ? '(kosongkan jika tidak diubah)' : '(untuk Login)'}
                  </label>
                  <input
                    type="text"
                    maxLength="6"
                    className="w-full p-2 border border-gray-300 rounded outline-none focus:border-indigo-500"
                    value={formData.pin}
                    onChange={(e) => setFormData({...formData, pin: e.target.value})}
                    required={!formData.id}
                    placeholder="Contoh: 123456"
                  />
                </div>
              )}
              <div className="modal-actions flex justify-end gap-2">
                <button type="button" className="btn btn-secondary px-4 py-2" onClick={() => setIsModalOpen(false)}>
                  {isViewOnly ? 'Tutup' : 'Batal'}
                </button>
                {isOwner && !isViewOnly && (
                  <button type="submit" className="btn btn-primary px-4 py-2">Simpan</button>
                )}
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
