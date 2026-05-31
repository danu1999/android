const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\CashFlow.tsx"; function _optionalChain(ops) { let lastAccessLHS = undefined; let value = ops[0]; let i = 1; while (i < ops.length) { const op = ops[i]; const fn = ops[i + 1]; i += 2; if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) { return undefined; } if (op === 'access' || op === 'optionalAccess') { lastAccessLHS = value; value = fn(value); } else if (op === 'call' || op === 'optionalCall') { value = fn((...args) => value.call(lastAccessLHS, ...args)); lastAccessLHS = undefined; } } return value; }/* eslint-disable @typescript-eslint/no-explicit-any, react-hooks/set-state-in-effect */
import React, { useEffect, useState } from 'react';
import api from '../../services/apiBmp';
// ✅ FIX: Menambahkan ikon Download dari lucide-react
import { PlusCircle, Search, Download } from 'lucide-react';









const CashFlow = () => {
    const [flows, setFlows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

    // Filter state
    const [filterType, setFilterType] = useState('ALL');
    const [searchTerm, setSearchTerm] = useState('');

    const [showModal, setShowModal] = useState(false);
    const [formData, setFormData] = useState({ date: new Date().toISOString().split('T')[0], type: 'MASUK', desc: '', amount: 0 });

    // Click-to-edit state
    const [editingId, setEditingId] = useState(null);
    const [editAmount, setEditAmount] = useState(0);

    useEffect(() => {
        const handleResize = () => setIsMobile(window.innerWidth <= 768);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    const fetchFlows = () => {
        setLoading(true);
        api.get('/kas').then(res => {
            setFlows(_optionalChain([res, 'access', _ => _.data, 'optionalAccess', _2 => _2.data]) || []);
            setLoading(false);
        }).catch((error) => {
            console.error("Gagal fetch kas:", error);
            setFlows([]);
            setLoading(false);
        });
    };

    useEffect(() => { fetchFlows(); }, []);

    const handleSave = async (e) => {
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

    const handleSaveEdit = async (id) => {
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

    const handleDelete = async (id) => {
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

    const handleRowDoubleClick = (e, id) => {
        const target = e.target ;
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
                    const detail = (res.data.details || []).map((d) =>
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
            _optionalChain([link, 'access', _3 => _3.parentNode, 'optionalAccess', _4 => _4.removeChild, 'call', _5 => _5(link)]);
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
        React.createElement('div', { style: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 187}}
            , React.createElement('div', { style: { width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #198754', borderRadius: '50%', animation: 'spin 1s linear infinite' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 188}})
            , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 189}}, `@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`)
            , React.createElement('div', { style: { color: '#6c757d' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 190}}, "Memuat data kas..."  )
        )
    );

    return (
        React.createElement('div', { style: { padding: isMobile ? '10px' : '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 195}}
            , React.createElement('div', { style: { display: 'flex', flexDirection: isMobile ? 'column' : 'row', justifyContent: 'space-between', marginBottom: '20px', gap: isMobile ? '15px' : '0' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 196}}
                , React.createElement('h2', { style: { fontSize: isMobile ? '20px' : '24px', fontWeight: 'bold', margin: 0, color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 197}}, "Buku Kas Umum"  )
                , React.createElement('div', { style: { display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 198}}

                    , React.createElement('button', { onClick: handleSync, style: { background: '#dc3545', color: 'white', border: 'none', padding: '10px 16px', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold', fontSize: isMobile ? '12px' : '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 200}}, "Sync Kas (Darurat)"

                    )

                    , React.createElement('button', {
                        onClick: handleCleanupOrphan,
                        style: {
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
                        },
                        title: "Hapus InvoicePayment yang tidak punya referensi di Kas (data ghost)"         , __self: this, __source: {fileName: _jsxFileName, lineNumber: 204}}
, "🧹 Bersihkan Data Ghost"

                    )

                    /* ✅ FIX: Tombol Download CSV diletakkan di sini */
                    , React.createElement('button', { onClick: handleDownloadCSV, style: { display: 'flex', alignItems: 'center', gap: '8px', background: '#198754', color: 'white', border: 'none', padding: '10px 16px', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold', fontSize: isMobile ? '12px' : '14px', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 225}}
                        , React.createElement(Download, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 226}} ), " Download CSV"
                    )

                    , React.createElement('button', { onClick: () => { setFormData({ date: new Date().toISOString().split('T')[0], type: 'MASUK', desc: '', amount: 0 }); setShowModal(true); }, style: { display: 'flex', alignItems: 'center', gap: '8px', background: '#0d6efd', color: 'white', border: 'none', padding: '10px 16px', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold', fontSize: isMobile ? '12px' : '14px', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 229}}
                        , React.createElement(PlusCircle, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 230}} ), " Transaksi Manual"
                    )

                )
            )

            , React.createElement('div', { style: { display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: isMobile ? '10px' : '20px', marginBottom: '25px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 236}}
                , React.createElement('div', { style: { flex: 1, background: 'white', padding: isMobile ? '15px' : '20px', borderRadius: '15px', borderLeft: '6px solid #198754', boxShadow: '0 4px 6px rgba(0,0,0,0.02)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 237}}
                    , React.createElement('div', { style: { color: '#64748b', fontSize: '13px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 238}}, "Kas Masuk" )
                    , React.createElement('div', { style: { fontSize: isMobile ? '20px' : '24px', fontWeight: '800', color: '#198754' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 239}}, "Rp " , totalIn.toLocaleString('id-ID'))
                )
                , React.createElement('div', { style: { flex: 1, background: 'white', padding: isMobile ? '15px' : '20px', borderRadius: '15px', borderLeft: '6px solid #dc3545', boxShadow: '0 4px 6px rgba(0,0,0,0.02)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 241}}
                    , React.createElement('div', { style: { color: '#64748b', fontSize: '13px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 242}}, "Kas Keluar" )
                    , React.createElement('div', { style: { fontSize: isMobile ? '20px' : '24px', fontWeight: '800', color: '#dc3545' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 243}}, "Rp " , totalOut.toLocaleString('id-ID'))
                )
                , React.createElement('div', { style: { flex: 1, background: 'linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%)', padding: isMobile ? '15px' : '20px', borderRadius: '15px', borderLeft: '6px solid #0d6efd', boxShadow: '0 4px 6px rgba(0,0,0,0.02)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 245}}
                    , React.createElement('div', { style: { color: '#1e3a8a', fontSize: '13px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 246}}, "Saldo Kas" )
                    , React.createElement('div', { style: { fontSize: isMobile ? '20px' : '24px', fontWeight: '800', color: '#1e3a8a' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 247}}, "Rp " , (totalIn - totalOut).toLocaleString('id-ID'))
                )
            )

            , React.createElement('div', { style: { background: 'white', padding: '15px', borderRadius: '15px', boxShadow: '0 2px 8px rgba(0,0,0,0.02)', marginBottom: '20px', position: 'relative' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 251}}
                , React.createElement(Search, { size: 18, style: { position: 'absolute', left: '27px', top: '28px', color: '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 252}} )
                , React.createElement('input', {
                    type: "text",
                    placeholder: "Cari keterangan transaksi..."  ,
                    value: searchTerm,
                    onChange: e => setSearchTerm(e.target.value),
                    style: { width: '100%', padding: '12px 12px 12px 40px', borderRadius: '10px', border: '1px solid #e2e8f0', outline: 'none', fontSize: '14px', boxSizing: 'border-box' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 253}}
                )
            )

            /* Modern Filter Tabs */
            , React.createElement('div', { style: { display: 'flex', gap: '5px', marginBottom: '20px', background: '#f1f5f9', padding: '5px', borderRadius: '12px', width: isMobile ? '100%' : 'fit-content', overflowX: 'auto' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 263}}
                , React.createElement('button', {
                    onClick: () => setFilterType('ALL'),
                    style: {
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
                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 264}}
, "Semua Kas"

                )
                , React.createElement('button', {
                    onClick: () => setFilterType('MASUK'),
                    style: {
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
                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 283}}
, "Pemasukan"

                )
                , React.createElement('button', {
                    onClick: () => setFilterType('KELUAR'),
                    style: {
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
                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 302}}
, "Pengeluaran"

                )
            )

            /* Instruction Tip */
            , React.createElement('div', { style: { fontSize: '13px', color: '#475569', marginBottom: '15px', display: 'flex', alignItems: 'center', gap: '8px', background: '#f8fafc', padding: '10px 15px', borderRadius: '10px', border: '1px solid #e2e8f0' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 324}}
                , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 325}}, "💡")
                , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 326}}, React.createElement('strong', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 326}}, "Tips:"), " Klik angka nominal untuk mengubah/mengedit nominal secara langsung. Klik 2x (double-click) pada baris transaksi untuk menghapus data."                 )
            )

            , React.createElement('div', { style: { background: 'white', borderRadius: '20px', padding: '10px', boxShadow: '0 4px 6px rgba(0,0,0,0.05)', border: '1px solid #f1f5f9', overflowX: 'auto' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 329}}
                , React.createElement('table', { style: { width: '100%', borderCollapse: 'separate', borderSpacing: '0 8px', textAlign: 'left', minWidth: isMobile ? '300px' : 'auto' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 330}}
                    , React.createElement('thead', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 331}}
                    , React.createElement('tr', { style: { color: '#64748b', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '1px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 332}}
                        , React.createElement('th', { style: { padding: isMobile ? '0 5px' : '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 333}}, "Tanggal")
                        , React.createElement('th', { style: { padding: isMobile ? '0 5px' : '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 334}}, "Info")
                        , React.createElement('th', { style: { padding: isMobile ? '0 5px' : '0 20px', fontWeight: '700', textAlign: 'right' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 335}}, "Nominal")
                    )
                    )
                    , React.createElement('tbody', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 338}}
                    , filteredFlows.map(f => (
                        React.createElement('tr', { 
                            key: f.ID, 
                            onDoubleClick: (e) => handleRowDoubleClick(e, f.ID),
                            style: { 
                                background: '#ffffff',
                                transition: 'background-color 0.2s',
                            },
                            onMouseEnter: (e) => { e.currentTarget.style.backgroundColor = '#f8fafc'; },
                            onMouseLeave: (e) => { e.currentTarget.style.backgroundColor = '#ffffff'; }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 340}}

                            , React.createElement('td', { 
                                title: "Double-click untuk menghapus"  ,
                                style: { padding: isMobile ? '10px 5px' : '15px 20px', fontWeight: '600', color: '#334155', borderBottom: '1px solid #f1f5f9', fontSize: isMobile ? '11px' : '14px', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 350}}

                                , new Date(f.TransactionDate).toLocaleDateString('id-ID', {day: 'numeric', month: 'short'})
                            )
                            , React.createElement('td', { 
                                title: "Double-click untuk menghapus"  ,
                                style: { padding: isMobile ? '10px 5px' : '15px 20px', borderBottom: '1px solid #f1f5f9', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 356}}

                                , React.createElement('div', { style: { fontWeight: '700', color: '#1e293b', fontSize: isMobile ? '12px' : '14px', whiteSpace: 'normal' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 360}}, f.Description)
                                , React.createElement('span', { style: { padding: '2px 6px', borderRadius: '4px', fontSize: '10px', fontWeight: '800', background: f.TransactionType === 'MASUK' ? '#dcfce7' : '#fee2e2', color: f.TransactionType === 'MASUK' ? '#15803d' : '#b91c1c' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 361}}
                                    , f.TransactionType
                                )
                            )
                            , React.createElement('td', { 
                                className: "nominal-cell",
                                style: { padding: isMobile ? '10px 5px' : '15px 20px', borderBottom: '1px solid #f1f5f9', textAlign: 'right' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 365}}

                                , editingId === f.ID ? (
                                    React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '5px', justifyContent: 'flex-end' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 370}}
                                        , React.createElement('input', {
                                            type: "number",
                                            value: editAmount,
                                            onChange: e => setEditAmount(Number(e.target.value)),
                                            style: { 
                                                width: isMobile ? '85px' : '130px', 
                                                padding: '6px 10px', 
                                                borderRadius: '6px', 
                                                border: '1px solid #cbd5e1', 
                                                fontSize: isMobile ? '12px' : '14px', 
                                                fontWeight: '800',
                                                outline: 'none',
                                                textAlign: 'right'
                                            },
                                            autoFocus: true,
                                            onKeyDown: (e) => {
                                                if (e.key === 'Enter') handleSaveEdit(f.ID);
                                                if (e.key === 'Escape') setEditingId(null);
                                            }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 371}}
                                        )
                                        , React.createElement('button', { 
                                            onClick: () => handleSaveEdit(f.ID), 
                                            style: { background: '#10b981', color: 'white', border: 'none', padding: '6px 10px', borderRadius: '6px', cursor: 'pointer', fontSize: '11px', fontWeight: 'bold' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 391}}
, "Simpan"

                                        )
                                        , React.createElement('button', { 
                                            onClick: () => setEditingId(null), 
                                            style: { background: '#64748b', color: 'white', border: 'none', padding: '6px 10px', borderRadius: '6px', cursor: 'pointer', fontSize: '11px', fontWeight: 'bold' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 397}}
, "Batal"

                                        )
                                    )
                                ) : (
                                    React.createElement('div', { 
                                        onClick: () => { setEditingId(f.ID); setEditAmount(f.Amount); },
                                        title: "Klik untuk mengubah nominal"   ,
                                        style: { 
                                            fontWeight: '800', 
                                            color: f.TransactionType === 'MASUK' ? '#15803d' : '#dc3545', 
                                            fontSize: isMobile ? '12px' : '14px',
                                            cursor: 'pointer',
                                            display: 'inline-block',
                                            padding: '4px 8px',
                                            borderRadius: '6px',
                                            transition: 'background-color 0.2s',
                                        },
                                        onMouseEnter: (e) => { e.currentTarget.style.backgroundColor = '#e2e8f0'; },
                                        onMouseLeave: (e) => { e.currentTarget.style.backgroundColor = 'transparent'; }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 405}}

                                        , f.TransactionType === 'KELUAR' ? '-' : ''
                                        , f.Amount.toLocaleString('id-ID')
                                        , React.createElement('span', { style: { fontSize: '10px', color: '#94a3b8', marginLeft: '5px', fontWeight: 'normal' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 423}}, "✏️")
                                    )
                                )
                            )
                        )
                    ))
                    , filteredFlows.length === 0 && (
                        React.createElement('tr', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 430}}
                            , React.createElement('td', { colSpan: 3, style: { textAlign: 'center', padding: '30px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 431}}, "Belum ada catatan kas."   )
                        )
                    )
                    )
                )
            )

            , showModal && (
                React.createElement('div', { style: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1200, padding: '15px', backdropFilter: 'blur(4px)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 439}}
                    , React.createElement('div', { style: { background: 'white', padding: isMobile ? '20px' : '30px', borderRadius: '24px', width: '100%', maxWidth: '400px', boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 440}}
                        , React.createElement('h3', { style: { marginBottom: '25px', fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 441}}, "Catat Kas Manual"  )
                        , React.createElement('form', { onSubmit: handleSave, __self: this, __source: {fileName: _jsxFileName, lineNumber: 442}}
                            , React.createElement('div', { style: { marginBottom: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 443}}
                                , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 444}}, "Tanggal")
                                , React.createElement('input', { type: "date", required: true, value: formData.date, onChange: e => setFormData({...formData, date: e.target.value}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 445}} )
                            )
                            , React.createElement('div', { style: { marginBottom: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 447}}
                                , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 448}}, "Tipe Transaksi" )
                                , React.createElement('select', { value: formData.type, onChange: e => setFormData({...formData, type: e.target.value}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', background: 'white' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 449}}
                                    , React.createElement('option', { value: "MASUK", __self: this, __source: {fileName: _jsxFileName, lineNumber: 450}}, "Kas Masuk" )
                                    , React.createElement('option', { value: "KELUAR", __self: this, __source: {fileName: _jsxFileName, lineNumber: 451}}, "Kas Keluar" )
                                )
                            )
                            , React.createElement('div', { style: { marginBottom: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 454}}
                                , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 455}}, "Keterangan")
                                , React.createElement('input', { type: "text", required: true, value: formData.desc, onChange: e => setFormData({...formData, desc: e.target.value}), placeholder: "Contoh: Beli token listrik"   , style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 456}} )
                            )
                            , React.createElement('div', { style: { marginBottom: '25px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 458}}
                                , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 459}}, "Nominal (Rp)" )
                                , React.createElement('input', { type: "number", required: true, value: formData.amount, onChange: e => setFormData({...formData, amount: e.target.value === '' ? ''  : Number(e.target.value)}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '16px', fontWeight: '800' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 460}} )
                            )
                            , React.createElement('div', { style: { display: 'flex', gap: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 462}}
                                , React.createElement('button', { type: "button", onClick: () => setShowModal(false), style: { flex: 1, padding: '14px', background: '#f1f5f9', color: '#64748b', border: 'none', borderRadius: '12px', cursor: 'pointer', fontWeight: 'bold' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 463}}, "Batal")
                                , React.createElement('button', { type: "submit", style: { flex: 2, padding: '14px', background: '#0d6efd', color: 'white', border: 'none', borderRadius: '12px', cursor: 'pointer', fontWeight: 'bold' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 464}}, "Simpan Transaksi" )
                            )
                        )
                    )
                )
            )
        )
    );
};

export default CashFlow;