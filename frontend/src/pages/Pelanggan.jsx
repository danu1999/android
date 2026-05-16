import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, Search, User } from 'lucide-react';
import api from '../api';

export default function Pelanggan() {
  const [customers, setCustomers] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ id: null, name: '', phone: '', address: '' });

  useEffect(() => {
    fetchCustomers();
  }, []);

  const fetchCustomers = async () => {
    try {
      const res = await api.get('/customers');
      setCustomers(res.data);
    } catch (err) {
      console.error('Failed to fetch customers', err);
    }
  };

  const handleOpenModal = (customer = null) => {
    if (customer) {
      setFormData(customer);
    } else {
      setFormData({ id: null, name: '', phone: '', address: '' });
    }
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      if (formData.id) {
        await api.put(`/customers/${formData.id}`, formData);
      } else {
        await api.post('/customers', formData);
      }
      setIsModalOpen(false);
      fetchCustomers();
    } catch (err) {
      console.error('Failed to save customer', err);
      alert('Gagal menyimpan pelanggan.');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Yakin ingin menghapus pelanggan ini?')) {
      try {
        await api.delete(`/customers/${id}`);
        fetchCustomers();
      } catch (err) {
        console.error('Failed to delete', err);
      }
    }
  };

  const filteredCustomers = customers.filter(c => 
    c.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
    (c.phone && c.phone.includes(searchQuery))
  );

  return (
    <div className="page-container">
      <div className="header-actions">
        <h1>Kelola Pelanggan</h1>
        <button className="btn btn-primary" onClick={() => handleOpenModal()}>
          <Plus size={18} /> Tambah Pelanggan
        </button>
      </div>

      <div className="glass-panel search-bar">
        <Search size={20} className="text-gray-400" />
        <input 
          type="text" 
          placeholder="Cari pelanggan (Nama / No HP)..." 
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredCustomers.length > 0 ? (
          filteredCustomers.map(customer => (
            <div key={customer.id} className="glass-panel p-6 flex flex-col justify-between">
              <div>
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-12 h-12 bg-indigo-100 text-indigo-600 rounded-full flex items-center justify-center">
                    <User size={24} />
                  </div>
                  <div>
                    <h3 className="font-bold text-lg m-0">{customer.name}</h3>
                    <div className="text-gray-500 text-sm">{customer.phone || 'No HP tidak tersedia'}</div>
                  </div>
                </div>
                <div className="text-gray-600 text-sm mb-4">
                  {customer.address || 'Alamat tidak tersedia'}
                </div>
              </div>
              <div className="flex justify-end gap-2 border-t border-gray-100 pt-4 mt-auto">
                <button className="btn btn-icon btn-edit" onClick={() => handleOpenModal(customer)}>
                  <Edit2 size={16} />
                </button>
                <button className="btn btn-icon btn-danger" onClick={() => handleDelete(customer.id)}>
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))
        ) : (
          <div className="col-span-full text-center p-8 text-gray-500">
            Tidak ada pelanggan.
          </div>
        )}
      </div>

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel">
            <h2>{formData.id ? 'Edit Pelanggan' : 'Tambah Pelanggan'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-group">
                <label>Nama Pelanggan</label>
                <input type="text" name="name" value={formData.name} onChange={(e) => setFormData({...formData, name: e.target.value})} required />
              </div>
              <div className="form-group">
                <label>No. HP / WhatsApp</label>
                <input type="text" name="phone" value={formData.phone} onChange={(e) => setFormData({...formData, phone: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Alamat</label>
                <input type="text" name="address" value={formData.address} onChange={(e) => setFormData({...formData, address: e.target.value})} />
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
