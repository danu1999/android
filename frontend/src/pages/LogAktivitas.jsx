import React, { useState, useEffect } from 'react';
import {
  Search, History, User, Activity, Trash2, Edit3, Plus,
  RefreshCw, DollarSign, Package, CreditCard, Shield, Clock
} from 'lucide-react';
import api from '../api';

export default function LogAktivitas() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedAction, setSelectedAction] = useState('ALL');
  const [selectedRole, setSelectedRole] = useState('ALL');

  useEffect(() => {
    fetchLogs();
  }, []);

  const fetchLogs = async () => {
    try {
      setLoading(true);
      const res = await api.get('/activity-logs');
      setLogs(res.data);
    } catch (err) {
      console.error('Failed to fetch activity logs', err);
    } finally {
      setLoading(false);
    }
  };

  const getLogIcon = (action) => {
    const act = action || '';
    if (act.startsWith('CREATE_')) return <Plus size={16} color="#059669" />;
    if (act.startsWith('UPDATE_')) return <Edit3 size={16} color="#D97706" />;
    if (act.startsWith('DELETE_')) return <Trash2 size={16} color="#DC2626" />;
    if (act === 'RESET_FINANCE') return <RefreshCw size={16} color="#DC2626" />;
    if (act === 'PAY_SALARY') return <DollarSign size={16} color="#10B981" />;
    if (act === 'RECEIVE_PO') return <Package size={16} color="#4F46E5" />;
    if (act === 'PAYMENT_CALLBACK') return <CreditCard size={16} color="#8B5CF6" />;
    return <Activity size={16} color="#6B7280" />;
  };

  const getLogColorClass = (action) => {
    const act = action || '';
    if (act.startsWith('CREATE_')) return { bg: '#ECFDF5', border: '#A7F3D0' };
    if (act.startsWith('UPDATE_')) return { bg: '#FFFBEB', border: '#FDE68A' };
    if (act.startsWith('DELETE_')) return { bg: '#FEF2F2', border: '#FCA5A5' };
    if (act === 'RESET_FINANCE') return { bg: '#FEF2F2', border: '#FCA5A5' };
    if (act === 'PAY_SALARY') return { bg: '#ECFDF5', border: '#A7F3D0' };
    if (act === 'RECEIVE_PO') return { bg: '#EEF2FF', border: '#C7D2FE' };
    if (act === 'PAYMENT_CALLBACK') return { bg: '#F5F3FF', border: '#DDD6FE' };
    return { bg: '#F9FAFB', border: '#E5E7EB' };
  };

  const getActionBadge = (action) => {
    const act = action || '';
    let label = act;
    if (act === 'CREATE_TRANSACTION') label = 'Transaksi Baru';
    else if (act === 'UPDATE_TRANSACTION') label = 'Edit Transaksi';
    else if (act === 'DELETE_TRANSACTION') label = 'Hapus Transaksi';
    else if (act === 'CREATE_PRODUCT') label = 'Tambah Produk';
    else if (act === 'UPDATE_PRODUCT') label = 'Edit Produk';
    else if (act === 'DELETE_PRODUCT') label = 'Hapus Produk';
    else if (act === 'CREATE_EMPLOYEE') label = 'Tambah Karyawan';
    else if (act === 'UPDATE_EMPLOYEE') label = 'Edit Karyawan';
    else if (act === 'DELETE_EMPLOYEE') label = 'Hapus Karyawan';
    else if (act === 'CREATE_FINANCE') label = 'Keuangan Baru';
    else if (act === 'UPDATE_FINANCE') label = 'Edit Keuangan';
    else if (act === 'DELETE_FINANCE') label = 'Hapus Keuangan';
    else if (act === 'PAY_SALARY') label = 'Bayar Gaji';
    else if (act === 'RESET_FINANCE') label = 'Reset Keuangan';
    else if (act === 'RECEIVE_PO') label = 'Terima PO';
    else if (act === 'DELETE_PO') label = 'Hapus PO';
    else if (act === 'PAYMENT_CALLBACK') label = 'Pembayaran Online';
    else if (act === 'CREATE_LAUNDRY_ORDER') label = 'Order Laundry Baru';
    else if (act === 'UPDATE_LAUNDRY_STATUS') label = 'Update Status Cucian';
    else if (act === 'UPDATE_LAUNDRY_PAYMENT') label = 'Update Bayar Laundry';
    else if (act === 'DELETE_LAUNDRY_ORDER') label = 'Hapus Order Laundry';
    else if (act === 'CREATE_LAUNDRY_SERVICE') label = 'Tambah Layanan Laundry';
    else if (act === 'UPDATE_LAUNDRY_SERVICE') label = 'Edit Layanan Laundry';
    else if (act === 'DELETE_LAUNDRY_SERVICE') label = 'Hapus Layanan Laundry';
    else if (act === 'CREATE_LAUNDRY_EXPENSE') label = 'Tambah Pengeluaran Laundry';
    else if (act === 'UPDATE_LAUNDRY_EXPENSE') label = 'Edit Pengeluaran Laundry';
    else if (act === 'DELETE_LAUNDRY_EXPENSE') label = 'Hapus Pengeluaran Laundry';

    const colors = getLogColorClass(action);

    return (
      <span style={{
        fontSize: '0.7rem',
        fontWeight: 800,
        backgroundColor: colors.bg,
        border: `1.5px solid ${colors.border}`,
        color: '#1F2937',
        padding: '3px 8px',
        borderRadius: '99px',
        display: 'inline-block',
        textTransform: 'uppercase',
        letterSpacing: '0.5px'
      }}>
        {label}
      </span>
    );
  };

  const formatDateTime = (dateStr) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('id-ID', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }) + ' WIB';
  };

  const filteredLogs = logs.filter(log => {
    const searchLower = searchQuery.toLowerCase();
    const matchesSearch = 
      log.description.toLowerCase().includes(searchLower) ||
      log.action.toLowerCase().includes(searchLower) ||
      (log.employee && log.employee.name.toLowerCase().includes(searchLower));

    const matchesAction = selectedAction === 'ALL' || log.action === selectedAction;
    const matchesRole = selectedRole === 'ALL' || (log.employee && log.employee.role === selectedRole);

    return matchesSearch && matchesAction && matchesRole;
  });

  const uniqueActions = [
    'CREATE_TRANSACTION', 'UPDATE_TRANSACTION', 'DELETE_TRANSACTION', 
    'CREATE_PRODUCT', 'UPDATE_PRODUCT', 'DELETE_PRODUCT', 
    'CREATE_FINANCE', 'UPDATE_FINANCE', 'DELETE_FINANCE', 
    'PAY_SALARY', 'RECEIVE_PO', 'PAYMENT_CALLBACK', 'RESET_FINANCE',
    'CREATE_LAUNDRY_ORDER', 'UPDATE_LAUNDRY_STATUS', 'UPDATE_LAUNDRY_PAYMENT',
    'DELETE_LAUNDRY_ORDER', 'CREATE_LAUNDRY_SERVICE', 'UPDATE_LAUNDRY_SERVICE',
    'DELETE_LAUNDRY_SERVICE', 'CREATE_LAUNDRY_EXPENSE', 'UPDATE_LAUNDRY_EXPENSE',
    'DELETE_LAUNDRY_EXPENSE'
  ];

  return (
    <div className="page-container">
      <div className="header-actions">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <History size={26} color="#4F46E5" />
          <h1 style={{ margin: 0 }}>Log Aktivitas Karyawan</h1>
        </div>
        <button className="btn btn-secondary" onClick={fetchLogs} disabled={loading} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> {loading ? 'Memuat...' : 'Refresh'}
        </button>
      </div>

      {/* Filter panel */}
      <div className="glass-panel" style={{ padding: '16px', marginBottom: '20px', display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1, minWidth: '240px', background: '#F3F4F6', borderRadius: '12px', padding: '0 12px' }}>
          <Search size={18} color="#6B7280" />
          <input
            type="text"
            placeholder="Cari log (deskripsi, nama karyawan)..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ border: 'none', background: 'transparent', outline: 'none', width: '100%', padding: '10px 0', fontSize: '0.9rem', color: '#1F2937' }}
          />
        </div>

        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          {/* Action Filter */}
          <select
            value={selectedAction}
            onChange={(e) => setSelectedAction(e.target.value)}
            style={{ padding: '10px 14px', borderRadius: '12px', border: '1.5px solid #E5E7EB', outline: 'none', background: 'white', fontWeight: 600, fontSize: '0.85rem', color: '#4B5563', cursor: 'pointer' }}
          >
            <option value="ALL">Semua Jenis Aksi</option>
            {uniqueActions.map(act => (
              <option key={act} value={act}>{act.replace(/_/g, ' ')}</option>
            ))}
          </select>

          {/* Role Filter */}
          <select
            value={selectedRole}
            onChange={(e) => setSelectedRole(e.target.value)}
            style={{ padding: '10px 14px', borderRadius: '12px', border: '1.5px solid #E5E7EB', outline: 'none', background: 'white', fontWeight: 600, fontSize: '0.85rem', color: '#4B5563', cursor: 'pointer' }}
          >
            <option value="ALL">Semua Jabatan</option>
            <option value="OWNER">Owner</option>
            <option value="ADMIN">Admin</option>
            <option value="KASIR">Kasir</option>
          </select>
        </div>
      </div>

      {/* Timeline view */}
      <div className="glass-panel" style={{ padding: '24px 20px', position: 'relative' }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#6B7280' }}>
            <RefreshCw size={24} className="animate-spin" style={{ margin: '0 auto 10px' }} />
            <div>Memuat log aktivitas...</div>
          </div>
        ) : filteredLogs.length > 0 ? (
          <div style={{ position: 'relative' }}>
            {/* Vertical Line */}
            <div style={{
              position: 'absolute',
              left: '18px',
              top: '8px',
              bottom: '8px',
              width: '2px',
              backgroundColor: '#E5E7EB',
              zIndex: 0
            }}></div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', zIndex: 1, position: 'relative' }}>
              {filteredLogs.map(log => {
                const colors = getLogColorClass(log.action);
                return (
                  <div key={log.id} style={{ display: 'flex', gap: '16px', position: 'relative' }}>
                    {/* Timeline Circle */}
                    <div style={{
                      width: '36px',
                      height: '36px',
                      borderRadius: '50%',
                      backgroundColor: colors.bg,
                      border: `2px solid ${colors.border}`,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                      boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03)',
                      zIndex: 2
                    }}>
                      {getLogIcon(log.action)}
                    </div>

                    {/* Timeline Content */}
                    <div style={{ flex: 1, background: '#F9FAFB', border: '1px solid #F3F4F6', borderRadius: '16px', padding: '14px 16px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                          <span style={{ fontWeight: 800, color: '#1F2937', fontSize: '0.95rem', display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
                            <User size={14} color="#6B7280" />
                            {log.employee?.name || 'Karyawan'}
                          </span>
                          <span style={{
                            fontSize: '0.65rem',
                            fontWeight: 700,
                            backgroundColor: log.employee?.role === 'OWNER' ? '#FEF3C7' : log.employee?.role === 'ADMIN' ? '#F0FDF4' : '#EEF2FF',
                            color: log.employee?.role === 'OWNER' ? '#D97706' : log.employee?.role === 'ADMIN' ? '#16A34A' : '#4F46E5',
                            padding: '1px 6px',
                            borderRadius: '4px',
                            textTransform: 'uppercase'
                          }}>
                            {log.employee?.role || 'KASIR'}
                          </span>
                        </div>
                        <div style={{ fontSize: '0.78rem', color: '#6B7280', display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600 }}>
                          <Clock size={12} />
                          {formatDateTime(log.createdAt)}
                        </div>
                      </div>

                      <div style={{ fontSize: '0.9rem', color: '#4B5563', lineHeight: 1.5, wordBreak: 'break-word', fontWeight: 550 }}>
                        {log.description}
                      </div>

                      <div style={{ marginTop: '2px' }}>
                        {getActionBadge(log.action)}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#9CA3AF' }}>
            <Activity size={28} style={{ margin: '0 auto 10px', opacity: 0.6 }} />
            <div>Tidak ada log aktivitas yang cocok dengan kriteria pencarian.</div>
          </div>
        )}
      </div>
    </div>
  );
}
