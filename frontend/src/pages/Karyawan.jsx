import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, Shield, Users, Lock } from 'lucide-react';
import api from '../api';

export default function Karyawan() {
  const [isPremium, setIsPremium] = useState(localStorage.getItem('posbah_premium') === 'true');
  const [employees, setEmployees] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ id: null, name: '', role: 'CASHIER', pin: '' });

  useEffect(() => {
    if (isPremium) {
      fetchEmployees();
    }
  }, [isPremium]);

  const fetchEmployees = async () => {
    try {
      const res = await api.get('/employees');
      setEmployees(res.data);
    } catch (err) {
      console.error('Failed to fetch employees', err);
    }
  };

  const handleOpenModal = (employee = null) => {
    if (employees.length >= 10 && !employee) {
      alert('Batas maksimal 10 karyawan telah tercapai.');
      return;
    }
    if (employee) {
      setFormData(employee);
    } else {
      setFormData({ id: null, name: '', role: 'CASHIER', pin: '' });
    }
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
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
    if (window.confirm('Yakin ingin menghapus karyawan ini?')) {
      try {
        await api.delete(`/employees/${id}`);
        fetchEmployees();
      } catch (err) {
        console.error('Failed to delete', err);
      }
    }
  };

  if (!isPremium) {
    return (
      <div className="page-container flex flex-col items-center justify-center text-center" style={{ minHeight: '80vh' }}>
        <div className="glass-panel max-w-md p-8 w-full border-t-4 border-yellow-400">
           <div className="w-20 h-20 bg-yellow-100 text-yellow-600 rounded-full flex items-center justify-center mx-auto mb-6">
             <Lock size={40} />
           </div>
           <h2 className="text-2xl font-bold mb-3 text-gray-800">Akses Terkunci</h2>
           <p className="text-gray-500 mb-8 leading-relaxed">
             Fitur <b>Manajemen Karyawan (Multi-User)</b> hanya tersedia di versi berbayar. Lakukan pembayaran untuk mengelola kasir dan PIN keamanan.
           </p>
           <button 
             className="btn btn-primary w-full py-4 text-lg shadow-lg font-bold"
             onClick={() => {
               const key = prompt('Masukkan Kode Lisensi Premium Anda:');
               const isValidKey = key && key.startsWith('POSBAH-') && key.endsWith('-PRO') && key.length === 20;
               
               if (isValidKey || key === 'POSBAH-X7V9-QW2R-PRO') {
                 localStorage.setItem('posbah_premium', 'true');
                 setIsPremium(true);
                 alert('Aktivasi Berhasil! Fitur Premium telah terbuka.');
               } else if (key) {
                 alert('Kode Lisensi Tidak Valid!');
               }
             }}
           >
             Buka Kunci Akses
           </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="header-actions flex flex-col md:flex-row gap-4 justify-between items-center bg-indigo-50 p-6 rounded-2xl border border-indigo-100 mb-6">
        <div>
          <h1 className="text-2xl font-extrabold text-indigo-900 m-0">Manajemen Karyawan</h1>
          <p className="text-indigo-600 mt-1">Kelola akses dan data {employees.length}/10 Karyawan (Admin & Kasir)</p>
        </div>
        <button 
          className="btn bg-indigo-600 text-white hover:bg-indigo-700 w-full md:w-auto justify-center rounded-xl"
          onClick={() => handleOpenModal()}
          disabled={employees.length >= 10}
        >
          <Plus size={18} /> Tambah Karyawan
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {employees.length > 0 ? (
          employees.map(employee => (
            <div key={employee.id} className="bg-white rounded-xl shadow-sm p-6 flex flex-col justify-between border border-gray-100 hover:shadow-md transition-shadow">
              <div className="flex items-center gap-4 mb-4">
                <div className={`w-14 h-14 rounded-full flex items-center justify-center ${employee.role === 'ADMIN' ? 'bg-purple-100 text-purple-600' : 'bg-blue-100 text-blue-600'}`}>
                  {employee.role === 'ADMIN' ? <Shield size={28} /> : <Users size={28} />}
                </div>
                <div>
                  <h3 className="font-bold text-lg text-gray-800 m-0">{employee.name}</h3>
                  <span className={`px-2 py-1 text-xs font-bold rounded mt-1 inline-block ${employee.role === 'ADMIN' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'}`}>
                    {employee.role}
                  </span>
                </div>
              </div>
              <div className="text-sm text-gray-500 mb-4 bg-gray-50 p-2 rounded">
                <span className="font-semibold">PIN Akses:</span> •••• (Tersimpan)
              </div>
              <div className="flex justify-end gap-2 border-t border-gray-100 pt-4 mt-auto">
                <button className="btn btn-icon btn-edit" onClick={() => handleOpenModal(employee)}>
                  <Edit2 size={16} />
                </button>
                <button className="btn btn-icon btn-danger" onClick={() => handleDelete(employee.id)}>
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))
        ) : (
          <div className="col-span-full text-center p-8 text-gray-500">
            Tidak ada karyawan.
          </div>
        )}
      </div>

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel max-w-md w-full">
            <h2 className="mb-4 text-xl font-bold">{formData.id ? 'Edit Karyawan' : 'Tambah Karyawan'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-group mb-4">
                <label className="block text-sm font-semibold text-gray-700 mb-1">Nama Karyawan</label>
                <input 
                  type="text" 
                  className="w-full p-2 border border-gray-300 rounded outline-none focus:border-indigo-500"
                  value={formData.name} 
                  onChange={(e) => setFormData({...formData, name: e.target.value})} 
                  required 
                />
              </div>
              <div className="form-group mb-4">
                <label className="block text-sm font-semibold text-gray-700 mb-1">Peran (Role)</label>
                <select 
                  className="w-full p-2 border border-gray-300 rounded outline-none focus:border-indigo-500"
                  value={formData.role} 
                  onChange={(e) => setFormData({...formData, role: e.target.value})}
                >
                  <option value="CASHIER">Kasir (CASHIER)</option>
                  <option value="ADMIN">Admin (ADMIN)</option>
                </select>
              </div>
              <div className="form-group mb-6">
                <label className="block text-sm font-semibold text-gray-700 mb-1">PIN Rahasia (Untuk Login)</label>
                <input 
                  type="text" 
                  maxLength="6"
                  className="w-full p-2 border border-gray-300 rounded outline-none focus:border-indigo-500"
                  value={formData.pin} 
                  onChange={(e) => setFormData({...formData, pin: e.target.value})} 
                  required 
                  placeholder="Contoh: 123456"
                />
              </div>
              <div className="modal-actions flex justify-end gap-2">
                <button type="button" className="btn btn-secondary px-4 py-2" onClick={() => setIsModalOpen(false)}>Batal</button>
                <button type="submit" className="btn btn-primary px-4 py-2">Simpan</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
