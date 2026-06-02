/* eslint-disable @typescript-eslint/no-explicit-any, react-hooks/set-state-in-effect */
import React, { useEffect, useState } from 'react';
import api from '../services/api';
// ✅ FIX: Menambahkan ikon Download dari lucide-react
import { PlusCircle, Search, Download } from 'lucide-react';

interface CashFlow {
    ID: number;
    TransactionDate: string;
    TransactionType: string;
    Description: string;
    Amount: number;
}

const CashFlow: React.FC = () => {
    const [flows, setFlows] = useState<CashFlow[]>([]);
    const [loading, setLoading] = useState(true);
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

    // Filter state
    const [filterType, setFilterType] = useState<'ALL' | 'MASUK' | 'KELUAR'>('ALL');
    const [searchTerm, setSearchTerm] = useState('');

    const [showModal, setShowModal] = useState(false);
    const [formData, setFormData] = useState({ date: new Date().toISOString().split('T')[0], type: 'MASUK', desc: '', amount: 0 });

    // Click-to-edit state
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editAmount, setEditAmount] = useState<number>(0);

    useEffect(() => {
        const handleResize = () => setIsMobile(window.innerWidth <= 768);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    const fetchFlows = () => {
        setLoading(true);
        api.get('/kas').then(res => {
            setFlows(res.data?.data || []);
            setLoading(false);
        }).catch((error) => {
            console.error("Gagal fetch kas:", error);
            setFlows([]);
            setLoading(false);
        });
    };

    useEffect(() => { fetchFlows(); }, []);

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const dateStr = formData.date;
            const formattedDate = dateStr.includes('T') ? dateStr : `${dateStr}T00:00:00+07:00`;

            await api.post('/kas', {
                transaction_date: formattedDate,
                transaction_type: formData.type,
                description: formData.desc,
                amount: Number(formData.amount)
            });
            setShowModal(false);
            fetchFlows();
        } catch (error) {
            console.error(error);
            alert("Gagal mencatat transaksi. Cek console log.");
        }
    };

    const handleSaveEdit = async (id: number) => {
        if (editAmount <= 0) {
            alert("Nominal harus lebih besar dari 0");
            return;
        }
        try {
            await api.put(`/kas/${id}`, { amount: Number(editAmount) });
            setEditingId(null);
            fetchFlows();
        } catch (error) {
            console.error("Gagal memperbarui nominal kas:", error);
            alert("Gagal memperbarui nominal kas");
        }
    };

    const handleDelete = async (id: number) => {
        if (window.confirm("Apakah Anda yakin ingin menghapus transaksi kas ini? Data pembayaran faktur terkait juga akan disesuaikan secara otomatis.")) {
            try {
                await api.delete(`/kas/${id}`);
                fetchFlows();
            } catch (error) {
                console.error(error);
                alert("Gagal menghapus kas");
            }
        }
    };

    const handleRowDoubleClick = (e: React.MouseEvent, id: number) => {
        const target = e.target as HTMLElement;
        if (
            target.closest('.nominal-cell') || 
            target.tagName === 'INPUT' || 
            target.tagName === 'BUTTON'
        ) {
            return;
        }
        handleDelete(id);
    };

    const handleSync = async () => {
        if (window.confirm("Sinkronisasi akan menghapus data kas Nono dan mereset ulang seluruh data pembayaran faktur. Lanjutkan?")) {
            try {
                const res = await api.post('/kas/sync');
                alert(res.data.message + ` (${res.data.synced_count} disinkronisasi)`);
                fetchFlows();
            } catch (error) {
                console.error(error);
                alert("Gagal sinkronisasi");
            }
        }
    };

    // Membersihkan InvoicePayment yang tidak punya referensi kas (data ghost / orphan)
    const handleCleanupOrphan = async () => {
        if (window.confirm("⚠️ Fungsi ini akan menghapus semua data pembayaran faktur yang tidak punya referensi di kas keuangan (data ghost).\n\nData yang terdampak akan dihitung ulang statusnya secara otomatis.\n\nLanjutkan?")) {
            try {
                const res = await api.post('/kas/cleanup-orphan-payments');
                if (res.data.cleaned > 0) {
                    const detail = (res.data.details || []).map((d: any) =>
                        `  • Payment ID ${d.payment_id} | Faktur ID ${d.invoice_id} | Rp ${d.payment_amount.toLocaleString('id-ID')}`
                    ).join('\n');
                    alert(`✅ Berhasil! ${res.data.cleaned} data ghost dihapus dan status faktur telah diperbarui.\n\nDetail:\n${detail}`);
                } else {
                    alert(`✅ ${res.data.message}`);
                }
                fetchFlows();
            } catch (error) {
                console.error(error);
                alert("Gagal membersihkan data ghost. Cek console untuk detail.");
            }
        }
    };


    // ✅ FIX: Fungsi download yang membawa Kunci Token (JWT)
    const handleDownloadCSV = async () => {
        try {
            // 1. Panggil API pakai Axios agar token Authorization ikut terbawa
            const response = await api.get('/kas/export/csv', {
                responseType: 'blob', // 👈 PENTING: Beritahu Axios bahwa ini adalah File, bukan JSON
            });

            // 2. Buat URL sementara dari file yang didapat
            const url = window.URL.createObjectURL(new Blob([response.data]));

            // 3. Buat tombol link tak kasat mata
            const link = document.createElement('a');
            link.href = url;

            // 4. Set nama file (opsional, karena dari backend sebenarnya sudah ada)
            link.setAttribute('download', `Laporan_Kas_${new Date().toISOString().slice(0, 10)}.csv`);
            document.body.appendChild(link);

            // 5. Klik otomatis tombolnya lalu hancurkan lagi
            link.click();
            link.parentNode?.removeChild(link);
            window.URL.revokeObjectURL(url);

        } catch (error) {
            console.error("Gagal mendownload CSV:", error);
            alert("Gagal mendownload file. Sesi mungkin kadaluarsa, coba login ulang.");
        }
    };

    // Filtering logic
    const filteredFlows = (flows || []).filter(f => {
        const matchType = filterType === 'ALL' || f.TransactionType === filterType;
        const matchSearch = (f.Description || '').toLowerCase().includes(searchTerm.toLowerCase());
        return matchType && matchSearch;
    });

    // Menghitung total berdasarkan data yang terfilter
    const totalIn = (filteredFlows || []).filter(f => f.TransactionType === 'MASUK').reduce((a, b) => a + b.Amount, 0);
    const totalOut = (filteredFlows || []).filter(f => f.TransactionType === 'KELUAR').reduce((a, b) => a + b.Amount, 0);

    if (loading) return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }}>
            <div style={{ width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #198754', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
            <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
            <div style={{ color: '#6c757d' }}>Memuat data kas...</div>
        </div>
    );

    return (
        <div style={{ padding: isMobile ? '10px' : '20px' }}>
            <div style={{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', justifyContent: 'space-between', marginBottom: '20px', gap: isMobile ? '15px' : '0' }}>
                <h2 style={{ fontSize: isMobile ? '20px' : '24px', fontWeight: 'bold', margin: 0, color: '#1e293b' }}>Buku Kas Umum</h2>
                <div style={{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: '10px' }}>

                    <button onClick={handleSync} style={{ background: '#dc3545', color: 'white', border: 'none', padding: '10px 16px', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold', fontSize: isMobile ? '12px' : '14px' }}>
                        Sync Kas (Darurat)
                    </button>

                    <button
                        onClick={handleCleanupOrphan}
                        style={{
                            background: '#f59e0b',
                            color: 'white',
                            border: 'none',
                            padding: '10px 16px',
                            borderRadius: '8px',
                            cursor: 'pointer',
                            fontWeight: 'bold',
                            fontSize: isMobile ? '12px' : '14px',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '6px'
                        }}
                        title="Hapus InvoicePayment yang tidak punya referensi di Kas (data ghost)"
                    >
                        🧹 Bersihkan Data Ghost
                    </button>

                    {/* ✅ FIX: Tombol Download CSV diletakkan di sini */}
                    <button onClick={handleDownloadCSV} style={{ display: 'flex', alignItems: 'center', gap: '8px', background: '#198754', color: 'white', border: 'none', padding: '10px 16px', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold', fontSize: isMobile ? '12px' : '14px', justifyContent: 'center' }}>
                        <Download size={18} /> Download CSV
                    </button>

                    <button onClick={() => { setFormData({ date: new Date().toISOString().split('T')[0], type: 'MASUK', desc: '', amount: 0 }); setShowModal(true); }} style={{ display: 'flex', alignItems: 'center', gap: '8px', background: '#0d6efd', color: 'white', border: 'none', padding: '10px 16px', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold', fontSize: isMobile ? '12px' : '14px', justifyContent: 'center' }}>
                        <PlusCircle size={18} /> Transaksi Manual
                    </button>

                </div>
            </div>

            <div style={{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: isMobile ? '10px' : '20px', marginBottom: '25px' }}>
                <div style={{ flex: 1, background: 'white', padding: isMobile ? '15px' : '20px', borderRadius: '15px', borderLeft: '6px solid #198754', boxShadow: '0 4px 6px rgba(0,0,0,0.02)' }}>
                    <div style={{ color: '#64748b', fontSize: '13px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '8px' }}>Kas Masuk</div>
                    <div style={{ fontSize: isMobile ? '20px' : '24px', fontWeight: '800', color: '#198754' }}>Rp {totalIn.toLocaleString('id-ID')}</div>
                </div>
                <div style={{ flex: 1, background: 'white', padding: isMobile ? '15px' : '20px', borderRadius: '15px', borderLeft: '6px solid #dc3545', boxShadow: '0 4px 6px rgba(0,0,0,0.02)' }}>
                    <div style={{ color: '#64748b', fontSize: '13px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '8px' }}>Kas Keluar</div>
                    <div style={{ fontSize: isMobile ? '20px' : '24px', fontWeight: '800', color: '#dc3545' }}>Rp {totalOut.toLocaleString('id-ID')}</div>
                </div>
                <div style={{ flex: 1, background: 'linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%)', padding: isMobile ? '15px' : '20px', borderRadius: '15px', borderLeft: '6px solid #0d6efd', boxShadow: '0 4px 6px rgba(0,0,0,0.02)' }}>
                    <div style={{ color: '#1e3a8a', fontSize: '13px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '8px' }}>Saldo Kas</div>
                    <div style={{ fontSize: isMobile ? '20px' : '24px', fontWeight: '800', color: '#1e3a8a' }}>Rp {(totalIn - totalOut).toLocaleString('id-ID')}</div>
                </div>
            </div>

            <div style={{ background: 'white', padding: '15px', borderRadius: '15px', boxShadow: '0 2px 8px rgba(0,0,0,0.02)', marginBottom: '20px', position: 'relative' }}>
                <Search size={18} style={{ position: 'absolute', left: '27px', top: '28px', color: '#94a3b8' }} />
                <input
                    type="text"
                    placeholder="Cari keterangan transaksi..."
                    value={searchTerm}
                    onChange={e => setSearchTerm(e.target.value)}
                    style={{ width: '100%', padding: '12px 12px 12px 40px', borderRadius: '10px', border: '1px solid #e2e8f0', outline: 'none', fontSize: '14px', boxSizing: 'border-box' }}
                />
            </div>

            {/* Modern Filter Tabs */}
            <div style={{ display: 'flex', gap: '5px', marginBottom: '20px', background: '#f1f5f9', padding: '5px', borderRadius: '12px', width: isMobile ? '100%' : 'fit-content', overflowX: 'auto' }}>
                <button
                    onClick={() => setFilterType('ALL')}
                    style={{
                        flex: isMobile ? 1 : 'none',
                        padding: '10px 20px',
                        borderRadius: '10px',
                        border: 'none',
                        cursor: 'pointer',
                        fontWeight: '700',
                        fontSize: '13px',
                        background: filterType === 'ALL' ? 'white' : 'transparent',
                        color: filterType === 'ALL' ? '#0d6efd' : '#64748b',
                        boxShadow: filterType === 'ALL' ? '0 2px 8px rgba(0,0,0,0.05)' : 'none',
                        transition: 'all 0.2s',
                        whiteSpace: 'nowrap'
                    }}
                >
                    Semua Kas
                </button>
                <button
                    onClick={() => setFilterType('MASUK')}
                    style={{
                        flex: isMobile ? 1 : 'none',
                        padding: '10px 20px',
                        borderRadius: '10px',
                        border: 'none',
                        cursor: 'pointer',
                        fontWeight: '700',
                        fontSize: '13px',
                        background: filterType === 'MASUK' ? 'white' : 'transparent',
                        color: filterType === 'MASUK' ? '#198754' : '#64748b',
                        boxShadow: filterType === 'MASUK' ? '0 2px 8px rgba(0,0,0,0.05)' : 'none',
                        transition: 'all 0.2s',
                        whiteSpace: 'nowrap'
                    }}
                >
                    Pemasukan
                </button>
                <button
                    onClick={() => setFilterType('KELUAR')}
                    style={{
                        flex: isMobile ? 1 : 'none',
                        padding: '10px 20px',
                        borderRadius: '10px',
                        border: 'none',
                        cursor: 'pointer',
                        fontWeight: '700',
                        fontSize: '13px',
                        background: filterType === 'KELUAR' ? 'white' : 'transparent',
                        color: filterType === 'KELUAR' ? '#dc3545' : '#64748b',
                        boxShadow: filterType === 'KELUAR' ? '0 2px 8px rgba(0,0,0,0.05)' : 'none',
                        transition: 'all 0.2s',
                        whiteSpace: 'nowrap'
                    }}
                >
                    Pengeluaran
                </button>
            </div>

            {/* Instruction Tip */}
            <div style={{ fontSize: '13px', color: '#475569', marginBottom: '15px', display: 'flex', alignItems: 'center', gap: '8px', background: '#f8fafc', padding: '10px 15px', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                <span>💡</span>
                <span><strong>Tips:</strong> Klik angka nominal untuk mengubah/mengedit nominal secara langsung. Klik 2x (double-click) pada baris transaksi untuk menghapus data.</span>
            </div>

            <div style={{ background: 'white', borderRadius: '20px', padding: '10px', boxShadow: '0 4px 6px rgba(0,0,0,0.05)', border: '1px solid #f1f5f9', overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'separate', borderSpacing: '0 8px', textAlign: 'left', minWidth: isMobile ? '300px' : 'auto' }}>
                    <thead>
                    <tr style={{ color: '#64748b', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '1px' }}>
                        <th style={{ padding: isMobile ? '0 5px' : '0 20px', fontWeight: '700' }}>Tanggal</th>
                        <th style={{ padding: isMobile ? '0 5px' : '0 20px', fontWeight: '700' }}>Info</th>
                        <th style={{ padding: isMobile ? '0 5px' : '0 20px', fontWeight: '700', textAlign: 'right' }}>Nominal</th>
                    </tr>
                    </thead>
                    <tbody>
                    {filteredFlows.map(f => (
                        <tr 
                            key={f.ID} 
                            onDoubleClick={(e) => handleRowDoubleClick(e, f.ID)}
                            style={{ 
                                background: '#ffffff',
                                transition: 'background-color 0.2s',
                            }}
                            onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#f8fafc'; }}
                            onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = '#ffffff'; }}
                        >
                            <td 
                                title="Double-click untuk menghapus"
                                style={{ padding: isMobile ? '10px 5px' : '15px 20px', fontWeight: '600', color: '#334155', borderBottom: '1px solid #f1f5f9', fontSize: isMobile ? '11px' : '14px', cursor: 'pointer' }}
                            >
                                {new Date(f.TransactionDate).toLocaleDateString('id-ID', {day: 'numeric', month: 'short'})}
                            </td>
                            <td 
                                title="Double-click untuk menghapus"
                                style={{ padding: isMobile ? '10px 5px' : '15px 20px', borderBottom: '1px solid #f1f5f9', cursor: 'pointer' }}
                            >
                                <div style={{ fontWeight: '700', color: '#1e293b', fontSize: isMobile ? '12px' : '14px', whiteSpace: 'normal' }}>{f.Description}</div>
                                <span style={{ padding: '2px 6px', borderRadius: '4px', fontSize: '10px', fontWeight: '800', background: f.TransactionType === 'MASUK' ? '#dcfce7' : '#fee2e2', color: f.TransactionType === 'MASUK' ? '#15803d' : '#b91c1c' }}>
                                    {f.TransactionType}
                                </span>
                            </td>
                            <td 
                                className="nominal-cell"
                                style={{ padding: isMobile ? '10px 5px' : '15px 20px', borderBottom: '1px solid #f1f5f9', textAlign: 'right' }}
                            >
                                {editingId === f.ID ? (
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '5px', justifyContent: 'flex-end' }}>
                                        <input
                                            type="number"
                                            value={editAmount}
                                            onChange={e => setEditAmount(Number(e.target.value))}
                                            style={{ 
                                                width: isMobile ? '85px' : '130px', 
                                                padding: '6px 10px', 
                                                borderRadius: '6px', 
                                                border: '1px solid #cbd5e1', 
                                                fontSize: isMobile ? '12px' : '14px', 
                                                fontWeight: '800',
                                                outline: 'none',
                                                textAlign: 'right'
                                            }}
                                            autoFocus
                                            onKeyDown={(e) => {
                                                if (e.key === 'Enter') handleSaveEdit(f.ID);
                                                if (e.key === 'Escape') setEditingId(null);
                                            }}
                                        />
                                        <button 
                                            onClick={() => handleSaveEdit(f.ID)} 
                                            style={{ background: '#10b981', color: 'white', border: 'none', padding: '6px 10px', borderRadius: '6px', cursor: 'pointer', fontSize: '11px', fontWeight: 'bold' }}
                                        >
                                            Simpan
                                        </button>
                                        <button 
                                            onClick={() => setEditingId(null)} 
                                            style={{ background: '#64748b', color: 'white', border: 'none', padding: '6px 10px', borderRadius: '6px', cursor: 'pointer', fontSize: '11px', fontWeight: 'bold' }}
                                        >
                                            Batal
                                        </button>
                                    </div>
                                ) : (
                                    <div 
                                        onClick={() => { setEditingId(f.ID); setEditAmount(f.Amount); }}
                                        title="Klik untuk mengubah nominal"
                                        style={{ 
                                            fontWeight: '800', 
                                            color: f.TransactionType === 'MASUK' ? '#15803d' : '#dc3545', 
                                            fontSize: isMobile ? '12px' : '14px',
                                            cursor: 'pointer',
                                            display: 'inline-block',
                                            padding: '4px 8px',
                                            borderRadius: '6px',
                                            transition: 'background-color 0.2s',
                                        }}
                                        onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#e2e8f0'; }}
                                        onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'transparent'; }}
                                    >
                                        {f.TransactionType === 'KELUAR' ? '-' : ''}
                                        {f.Amount.toLocaleString('id-ID')}
                                        <span style={{ fontSize: '10px', color: '#94a3b8', marginLeft: '5px', fontWeight: 'normal' }}>✏️</span>
                                    </div>
                                )}
                            </td>
                        </tr>
                    ))}
                    {filteredFlows.length === 0 && (
                        <tr>
                            <td colSpan={3} style={{ textAlign: 'center', padding: '30px', color: '#64748b' }}>Belum ada catatan kas.</td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>

            {showModal && (
                <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1200, padding: '15px', backdropFilter: 'blur(4px)' }}>
                    <div style={{ background: 'white', padding: isMobile ? '20px' : '30px', borderRadius: '24px', width: '100%', maxWidth: '400px', boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)' }}>
                        <h3 style={{ marginBottom: '25px', fontWeight: '800', color: '#1e293b' }}>Catat Kas Manual</h3>
                        <form onSubmit={handleSave}>
                            <div style={{ marginBottom: '18px' }}>
                                <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }}>Tanggal</label>
                                <input type="date" required value={formData.date} onChange={e => setFormData({...formData, date: e.target.value})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none' }} />
                            </div>
                            <div style={{ marginBottom: '18px' }}>
                                <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }}>Tipe Transaksi</label>
                                <select value={formData.type} onChange={e => setFormData({...formData, type: e.target.value})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', background: 'white' }}>
                                    <option value="MASUK">Kas Masuk</option>
                                    <option value="KELUAR">Kas Keluar</option>
                                </select>
                            </div>
                            <div style={{ marginBottom: '18px' }}>
                                <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }}>Keterangan</label>
                                <input type="text" required value={formData.desc} onChange={e => setFormData({...formData, desc: e.target.value})} placeholder="Contoh: Beli token listrik" style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none' }} />
                            </div>
                            <div style={{ marginBottom: '25px' }}>
                                <label style={{ display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }}>Nominal (Rp)</label>
                                <input type="number" required value={formData.amount} onChange={e => setFormData({...formData, amount: e.target.value === '' ? '' as any : Number(e.target.value)})} style={{ width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '16px', fontWeight: '800' }} />
                            </div>
                            <div style={{ display: 'flex', gap: '12px' }}>
                                <button type="button" onClick={() => setShowModal(false)} style={{ flex: 1, padding: '14px', background: '#f1f5f9', color: '#64748b', border: 'none', borderRadius: '12px', cursor: 'pointer', fontWeight: 'bold' }}>Batal</button>
                                <button type="submit" style={{ flex: 2, padding: '14px', background: '#0d6efd', color: 'white', border: 'none', borderRadius: '12px', cursor: 'pointer', fontWeight: 'bold' }}>Simpan Transaksi</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CashFlow;