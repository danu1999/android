const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\Employees.tsx"; function _optionalChain(ops) { let lastAccessLHS = undefined; let value = ops[0]; let i = 1; while (i < ops.length) { const op = ops[i]; const fn = ops[i + 1]; i += 2; if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) { return undefined; } if (op === 'access' || op === 'optionalAccess') { lastAccessLHS = value; value = fn(value); } else if (op === 'call' || op === 'optionalCall') { value = fn((...args) => value.call(lastAccessLHS, ...args)); lastAccessLHS = undefined; } } return value; }import React, { useEffect, useState } from 'react';
import api from '../../services/apiBmp';
import { Calendar, DollarSign, FileText, User, Trash2, Search, X, Briefcase, TrendingDown } from 'lucide-react';

const Employees = () => {
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');

    const fetchHistory = async () => {
        setLoading(true);
        try {
            const res = await api.get('/payroll/history');
            setHistory(res.data.data || []);
        } catch (err) {
            console.error("Gagal memuat riwayat gaji", err);
        }
        setLoading(false);
    };

    useEffect(() => {
        fetchHistory();
    }, []);

    const handleDelete = async (id) => {
        if (!window.confirm("Yakin ingin menghapus riwayat gaji ini? (Data Kas Keluar juga akan otomatis ditarik/dihapus lho!)")) return;

        try {
            await api.delete(`/payroll/history/${id}`);
            alert("Riwayat gaji dan data Kas Keluar berhasil dihapus bersih!");
            fetchHistory(); // Refresh list setelah berhasil dihapus
        } catch (err) {
            console.error(err);
            alert("Gagal menghapus data.");
        }
    };

    const formatRp = (num) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(num);

    // Search filtering
    const filteredHistory = history.filter(item => {
        const name = (_optionalChain([item, 'access', _ => _.Employee, 'optionalAccess', _2 => _2.Name]) || '').toLowerCase();
        const position = (_optionalChain([item, 'access', _3 => _3.Employee, 'optionalAccess', _4 => _4.Position]) || '').toLowerCase();
        const notes = (item.Description || item.Notes || '').toLowerCase();
        const query = searchTerm.toLowerCase();
        return name.includes(query) || position.includes(query) || notes.includes(query);
    });

    // Summary calculations
    const totalPaymentsCount = filteredHistory.length;
    const totalSalaryPaid = filteredHistory.reduce((acc, item) => acc + (item.Amount || 0), 0);

    return (
        React.createElement('div', { style: { padding: '20px', maxWidth: '1200px', margin: '0 auto' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 54}}
            , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 55}}, `
                .history-card {
                    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                }
                .history-card:hover {
                    transform: translateY(-4px);
                    box-shadow: 0 12px 20px -5px rgba(0, 0, 0, 0.08) !important;
                    border-color: #3b82f6 !important;
                }
                .btn-delete {
                    transition: all 0.2s ease;
                }
                .btn-delete:hover {
                    transform: scale(1.05);
                    background-color: #fee2e2 !important;
                    color: #dc3545 !important;
                }
                .input-focus:focus {
                    border-color: #3b82f6 !important;
                    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15) !important;
                }
            `)

            /* Header Section */
            , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 79}}
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 80}}
                    , React.createElement('h2', { style: { margin: 0, color: '#0f172a', fontSize: '24px', fontWeight: '800' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 81}}, "Riwayat Penggajian" )
                    , React.createElement('p', { style: { margin: '4px 0 0', color: '#64748b', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 82}}, "Catatan pembayaran gaji karyawan & otomatisasi potongan kas"       )
                )
            )

            /* Stats Cards Row */
            , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '15px', marginBottom: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 87}}
                , React.createElement('div', { style: { background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '16px', padding: '18px', display: 'flex', alignItems: 'center', gap: '15px', boxShadow: '0 2px 4px rgba(0,0,0,0.02)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 88}}
                    , React.createElement('div', { style: { width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(59, 130, 246, 0.1)', color: '#3b82f6', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 89}}
                        , React.createElement(DollarSign, { size: 22, __self: this, __source: {fileName: _jsxFileName, lineNumber: 90}} )
                    )
                    , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 92}}
                        , React.createElement('div', { style: { fontSize: '13px', color: '#64748b', fontWeight: '500' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 93}}, "Total Transaksi Gaji"  )
                        , React.createElement('div', { style: { fontSize: '20px', fontWeight: '800', color: '#1e293b', marginTop: '2px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 94}}, totalPaymentsCount, " " , React.createElement('span', { style: { fontSize: '13px', fontWeight: '500', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 94}}, "Kali"))
                    )
                )

                , React.createElement('div', { style: { background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '16px', padding: '18px', display: 'flex', alignItems: 'center', gap: '15px', boxShadow: '0 2px 4px rgba(0,0,0,0.02)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 98}}
                    , React.createElement('div', { style: { width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 99}}
                        , React.createElement(TrendingDown, { size: 22, __self: this, __source: {fileName: _jsxFileName, lineNumber: 100}} )
                    )
                    , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 102}}
                        , React.createElement('div', { style: { fontSize: '13px', color: '#64748b', fontWeight: '500' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 103}}, "Total Gaji Dibayarkan"  )
                        , React.createElement('div', { style: { fontSize: '20px', fontWeight: '800', color: '#ef4444', marginTop: '2px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 104}}, formatRp(totalSalaryPaid).replace(',00', ''))
                    )
                )
            )

            /* Search Bar */
            , React.createElement('div', { style: { position: 'relative', marginBottom: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 110}}
                , React.createElement('div', { style: { position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', display: 'flex', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 111}}
                    , React.createElement(Search, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 112}} )
                )
                , React.createElement('input', { 
                    type: "text", 
                    placeholder: "Cari riwayat berdasarkan nama karyawan, posisi, atau catatan..."       , 
                    value: searchTerm,
                    onChange: (e) => setSearchTerm(e.target.value),
                    className: "input-focus",
                    style: { 
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
                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 114}} 
                )
                , searchTerm && (
                    React.createElement('button', { 
                        onClick: () => setSearchTerm(''),
                        style: { position: 'absolute', right: '16px', top: '50%', transform: 'translateY(-50%)', border: 'none', background: 'none', cursor: 'pointer', color: '#64748b', display: 'flex', alignItems: 'center', padding: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 134}}

                        , React.createElement(X, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 138}} )
                    )
                )
            )

            /* Content Container */
            , React.createElement('div', { style: { background: 'transparent', minHeight: '200px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 144}}
                , loading ? (
                    React.createElement('div', { style: { textAlign: 'center', padding: '50px 20px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 146}}
                        , React.createElement('div', { style: { width: '35px', height: '35px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 147}})
                        , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 148}}, `@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`), "Memuat riwayat..."

                    )
                ) : (
                    React.createElement(React.Fragment, null
                        /* Card Grid */
                        , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 154}}
                            , filteredHistory.length > 0 ? filteredHistory.map((item, idx) => {
                                const payDate = new Date(item.PaymentDate || item.CreatedAt);
                                const formattedDate = payDate.toLocaleDateString('id-ID', { day: '2-digit', month: 'long', year: 'numeric' });
                                
                                return (
                                    React.createElement('div', { key: item.ID || idx, className: "history-card", style: { background: '#ffffff', borderRadius: '16px', border: '1px solid #e2e8f0', padding: '18px', display: 'flex', flexDirection: 'column', boxShadow: '0 4px 6px rgba(0,0,0,0.01)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 160}}

                                        /* Header Card: Tanggal & Badge Gaji */
                                        , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 163}}
                                            , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', color: '#64748b', fontSize: '12px', fontWeight: '500' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 164}}
                                                , React.createElement(Calendar, { size: 13, color: "#0d6efd", __self: this, __source: {fileName: _jsxFileName, lineNumber: 165}} )
                                                , formattedDate
                                            )
                                            , React.createElement('div', { style: { fontSize: '11px', background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', padding: '3px 8px', borderRadius: '8px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 168}}, "Gaji Keluar"

                                            )
                                        )

                                        /* Info Karyawan */
                                        , React.createElement('div', { style: { display: 'flex', alignItems: 'center', marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 174}}
                                            , React.createElement('div', { style: { width: '38px', height: '38px', borderRadius: '10px', background: 'linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%)', color: '#475569', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 175}}
                                                , React.createElement(User, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 176}} )
                                            )
                                            , React.createElement('div', { style: { marginLeft: '12px', minWidth: 0, flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 178}}
                                                , React.createElement('h3', { style: { fontSize: '15px', fontWeight: '800', margin: 0, color: '#1e293b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 179}}
                                                    , _optionalChain([item, 'access', _5 => _5.Employee, 'optionalAccess', _6 => _6.Name]) || 'Tidak diketahui'
                                                )
                                                , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '4px', color: '#64748b', fontSize: '12px', marginTop: '1px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 182}}
                                                    , React.createElement(Briefcase, { size: 12, color: "#94a3b8", __self: this, __source: {fileName: _jsxFileName, lineNumber: 183}} )
                                                    , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 184}}, _optionalChain([item, 'access', _7 => _7.Employee, 'optionalAccess', _8 => _8.Position]) || '-')
                                                )
                                            )
                                        )

                                        /* Divider */
                                        , React.createElement('div', { style: { height: '1px', backgroundColor: '#f1f5f9', margin: '0 0 12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 190}})

                                        /* Info Tambahan: Catatan & Nominal */
                                        , React.createElement('div', { style: { display: 'flex', flexDirection: 'column', gap: '8px', flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 193}}
                                            , React.createElement('div', { style: { display: 'flex', gap: '6px', fontSize: '12px', color: '#64748b', background: '#f8fafc', padding: '10px 12px', borderRadius: '10px', fontStyle: 'italic' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 194}}
                                                , React.createElement(FileText, { size: 14, color: "#94a3b8", style: { marginTop: '2px', flexShrink: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 195}} )
                                                , React.createElement('span', { style: { wordBreak: 'break-word' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 196}}, item.Description || item.Notes || 'Tidak ada catatan')
                                            )

                                            , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 199}}
                                                , React.createElement('span', { style: { fontSize: '12px', fontWeight: '600', color: '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 200}}, "Total Dibayar" )
                                                , React.createElement('span', { style: { fontSize: '16px', fontWeight: '900', color: '#ef4444' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 201}}, "- "
                                                     , formatRp(item.Amount).replace(',00', '')
                                                )
                                            )
                                        )

                                        /* Footer Actions */
                                        , React.createElement('div', { style: { display: 'flex', justifyContent: 'flex-end', marginTop: '14px', borderTop: '1px solid #f1f5f9', paddingTop: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 208}}
                                            , React.createElement('button', { 
                                                onClick: () => handleDelete(item.ID), 
                                                className: "btn-delete",
                                                style: { background: '#fee2e2', color: '#dc3545', border: 'none', padding: '8px', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' },
                                                title: "Hapus Riwayat & Potongan Kas"    , __self: this, __source: {fileName: _jsxFileName, lineNumber: 209}}

                                                , React.createElement(Trash2, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 215}})
                                            )
                                        )

                                    )
                                );
                            }) : (
                                React.createElement('div', { style: { gridColumn: '1 / -1', textAlign: 'center', padding: '60px 20px', background: '#ffffff', borderRadius: '16px', border: '1px solid #e2e8f0' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 222}}
                                    , React.createElement('div', { style: { fontSize: '40px', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 223}}, "🔍")
                                    , React.createElement('h3', { style: { fontSize: '16px', fontWeight: '800', color: '#1e293b', margin: '0 0 4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 224}}, "Riwayat Tidak Ditemukan"  )
                                    , React.createElement('p', { style: { color: '#64748b', fontSize: '13px', margin: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 225}}, "Coba gunakan kata kunci pencarian lain."     )
                                )
                            )
                        )
                    )
                )
            )
        )
    );
};

export default Employees;