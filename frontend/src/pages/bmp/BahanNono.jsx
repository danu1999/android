const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\BahanNono.tsx"; function _optionalChain(ops) { let lastAccessLHS = undefined; let value = ops[0]; let i = 1; while (i < ops.length) { const op = ops[i]; const fn = ops[i + 1]; i += 2; if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) { return undefined; } if (op === 'access' || op === 'optionalAccess') { lastAccessLHS = value; value = fn(value); } else if (op === 'call' || op === 'optionalCall') { value = fn((...args) => value.call(lastAccessLHS, ...args)); lastAccessLHS = undefined; } } return value; }/* eslint-disable @typescript-eslint/no-explicit-any, react-hooks/set-state-in-effect */
import React, { useEffect, useState } from 'react';
import api, { API_URL } from '../../services/apiBmp';
import { PlusCircle, ArrowUpRight, ArrowDownRight, Eye, Paperclip, X, Trash2, Edit2, Package, Info, ShoppingCart, DollarSign, AlertCircle, Calendar, ChevronLeft, ChevronRight, Filter } from 'lucide-react';

const BahanNono = () => {
    const [data, setData] = useState([]);
    const [summary, setSummary] = useState({ total_cash_in: 0, total_cash_out: 0, balance: 0 });
    const [loading, setLoading] = useState(true);
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

    // Filter states
    const [filterType, setFilterType] = useState('ALL');
    const [selectedCalendarDay, setSelectedCalendarDay] = useState(null);

    // Calendar states
    const [showCalendar, setShowCalendar] = useState(false);
    const [calendarDate, setCalendarDate] = useState(new Date());

    const [showModal, setShowModal] = useState(false);
    const [showDetailModal, setShowDetailModal] = useState(false);
    const [selectedItem, setSelectedItem] = useState(null);
    const [isEditing, setIsEditing] = useState(false);

    const [tab, setTab] = useState('IN');
    const [tanggal, setTanggal] = useState(new Date().toISOString().split('T')[0]);
    const [notes, setNotes] = useState('');
    const [tagihan, setTagihan] = useState('');
    const [uploading, setUploading] = useState(false);

    // IN State
    const [items, setItems] = useState([{ jenis_bahan: 'Super A', kuantitas: 0, unit: 'Kg', rate: 0 }]);
    const [jasaItems, setJasaItems] = useState([{ jenis_bahan: 'Titip Giling', kuantitas: 0, unit: 'Kg', rate: 0 }]);
    // OUT State
    const [nominal, setNominal] = useState(0);

    useEffect(() => {
        const handleResize = () => setIsMobile(window.innerWidth <= 768);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    const fetchData = () => {
        setLoading(true);
        api.get('/bahan-nono').then(res => {
            setData(_optionalChain([res, 'access', _2 => _2.data, 'optionalAccess', _3 => _3.data, 'optionalAccess', _4 => _4.transactions]) || []);
            setSummary(_optionalChain([res, 'access', _5 => _5.data, 'optionalAccess', _6 => _6.data]) || { total_cash_in: 0, total_cash_out: 0, balance: 0 });
            setLoading(false);
        }).catch(error => {
            console.error("Gagal ambil data:", error);
            setData([]);
            setLoading(false);
        });
    };

    useEffect(() => { fetchData(); }, []);

    const openModal = () => {
        setIsEditing(false);
        setSelectedItem(null);
        setTanggal(new Date().toISOString().split('T')[0]);
        setNotes('');
        setTagihan('');
        setItems([{ jenis_bahan: 'Super A', kuantitas: 0, unit: 'Kg', rate: 0 }]);
        setJasaItems([{ jenis_bahan: 'Titip Giling', kuantitas: 0, unit: 'Kg', rate: 0 }]);
        setNominal(0);
        setShowModal(true);
    };

    const openEdit = () => {
        if (!selectedItem) return;
        setIsEditing(true);
        setTanggal(selectedItem.Tanggal.split('T')[0]);
        setNotes(selectedItem.Notes);
        setTagihan(selectedItem.Tagihan);

        if (selectedItem.TotalHarga > 0) {
            setTab('IN');
            const bItems = selectedItem.Items.filter((i) => !i.JenisBahan.startsWith('[JASA]')).map((i) => ({
                jenis_bahan: i.JenisBahan,
                kuantitas: i.Kuantitas,
                unit: i.Unit,
                rate: i.Rate
            }));
            const jItems = selectedItem.Items.filter((i) => i.JenisBahan.startsWith('[JASA]')).map((i) => ({
                jenis_bahan: i.JenisBahan.replace('[JASA] ', ''),
                kuantitas: i.Kuantitas,
                unit: i.Unit,
                rate: i.Rate
            }));
            setItems(bItems.length > 0 ? bItems : [{ jenis_bahan: 'Super A', kuantitas: 0, unit: 'Kg', rate: 0 }]);
            setJasaItems(jItems);
        } else {
            setTab('OUT');
            setNominal(selectedItem.Nominal);
        }

        setShowDetailModal(false);
        setShowModal(true);
    };

    const compressImage = (file) => {
        return new Promise((resolve) => {
            const reader = new FileReader();
            reader.readAsDataURL(file);
            reader.onload = (event) => {
                const img = new Image();
                img.src = _optionalChain([event, 'access', _7 => _7.target, 'optionalAccess', _8 => _8.result]) ;
                img.onload = () => {
                    const canvas = document.createElement('canvas');
                    const MAX_WIDTH = 1200;
                    const MAX_HEIGHT = 1200;
                    let width = img.width;
                    let height = img.height;

                    if (width > height) {
                        if (width > MAX_WIDTH) {
                            height *= MAX_WIDTH / width;
                            width = MAX_WIDTH;
                        }
                    } else {
                        if (height > MAX_HEIGHT) {
                            width *= MAX_HEIGHT / height;
                            height = MAX_HEIGHT;
                        }
                    }

                    canvas.width = width;
                    canvas.height = height;
                    const ctx = canvas.getContext('2d');
                    _optionalChain([ctx, 'optionalAccess', _9 => _9.drawImage, 'call', _10 => _10(img, 0, 0, width, height)]);

                    canvas.toBlob((blob) => {
                        if (blob) {
                            const compressedFile = new File([blob], file.name, {
                                type: 'image/jpeg',
                                lastModified: Date.now(),
                            });
                            resolve(compressedFile);
                        } else {
                            resolve(file);
                        }
                    }, 'image/jpeg', 0.7);
                };
            };
        });
    };

    const handleFileUpload = async (e) => {
        if (!e.target.files || e.target.files.length === 0) return;

        let file = e.target.files[0];

        if (file.type.startsWith('image/')) {
            setUploading(true);
            file = await compressImage(file);
        }

        const formData = new FormData();
        formData.append('file', file);

        setUploading(true);
        try {
            const res = await api.post('/upload?folder=tagihan_nono', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
            setTagihan(res.data.data.url);
            alert("Bukti berhasil diunggah (Ukuran telah diperkecil)");
        } catch (error) {
            console.error(error);
            alert("Gagal mengunggah bukti");
        } finally {
            setUploading(false);
        }
    };

    const handleSave = async (e) => {
        e.preventDefault();
        
        const finalItems = [...items];
        jasaItems.forEach(j => {
            if (j.kuantitas > 0 || j.rate > 0) {
                finalItems.push({ ...j, jenis_bahan: `[JASA] ${j.jenis_bahan}` });
            }
        });

        const payload = tab === 'IN'
            ? { trans_type: 'IN', tanggal: tanggal + "T00:00:00Z", notes, items: finalItems, tagihan }
            : { trans_type: 'OUT', tanggal: tanggal + "T00:00:00Z", notes, nominal: Number(nominal), tagihan };

        try {
            if (isEditing && selectedItem) {
                await api.put(`/bahan-nono/${selectedItem.ID}`, payload);
            } else {
                await api.post('/bahan-nono', payload);
            }
            setShowModal(false);
            fetchData();
        } catch (error) {
            console.error(error);
            alert("Gagal menyimpan transaksi");
        }
    };

    const formatImageUrl = (url) => {
        if (!url) return '';
        if (url.startsWith('http')) return url;
        return `${API_URL}/files/${url}`;
    };

    const openDetail = (item) => {
        setSelectedItem(item);
        setShowDetailModal(true);
    };

    const handleDelete = async (id) => {
        if (window.confirm("Yakin ingin menghapus transaksi ini?")) {
            try {
                await api.delete(`/bahan-nono/${id}`);
                setShowDetailModal(false);
                fetchData();
            } catch (error) {
                console.error(error);
                alert("Gagal menghapus data");
            }
        }
    };

    // Filter Logic combining tabs & calendar selected date
    const filteredData = (data || []).filter(d => {
        let matchesType = true;
        if (filterType === 'IN') matchesType = (d ).TotalHarga > 0;
        else if (filterType === 'OUT') matchesType = (d ).Nominal > 0;
        
        let matchesDate = true;
        if (selectedCalendarDay) {
            const txDateStr = (d ).Tanggal.split('T')[0];
            matchesDate = txDateStr === selectedCalendarDay;
        }
        
        return matchesType && matchesDate;
    });

    // Calendar helper functions
    const toLocalYYYYMMDD = (d) => {
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    const getTransactionsForDate = (dateStr) => {
        return (data || []).filter(tx => {
            const txDateStr = (tx ).Tanggal.split('T')[0];
            return txDateStr === dateStr;
        });
    };

    const getDaysArray = (year, month) => {
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        const firstDayIndex = new Date(year, month, 1).getDay();
        const adjustedFirstDay = firstDayIndex === 0 ? 6 : firstDayIndex - 1;
        
        const tempDays = [];
        for (let i = 0; i < adjustedFirstDay; i++) {
            tempDays.push(null);
        }
        for (let i = 1; i <= daysInMonth; i++) {
            tempDays.push(new Date(year, month, i));
        }
        return tempDays;
    };

    const prevMonth = () => {
        setCalendarDate(new Date(calendarDate.getFullYear(), calendarDate.getMonth() - 1, 1));
    };

    const nextMonth = () => {
        setCalendarDate(new Date(calendarDate.getFullYear(), calendarDate.getMonth() + 1, 1));
    };

    const MONTH_NAMES = [
        'Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni',
        'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember'
    ];

    if (loading) return (
        React.createElement('div', { style: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 288}}
            , React.createElement('div', { style: { width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 289}})
            , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 290}}, `@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`)
            , React.createElement('div', { style: { color: '#6c757d' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 291}}, "Memuat data bahan..."  )
        )
    );

    return (
        React.createElement('div', { style: { padding: isMobile ? '12px' : '20px', background: '#f8fafc', minHeight: '100%', maxWidth: '100vw', boxSizing: 'border-box', overflowX: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 296}}
            , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 297}}, `
                .btn-primary {
                    background: linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%);
                    color: white;
                    border: none;
                    box-shadow: 0 4px 10px rgba(13, 110, 253, 0.2);
                    transition: all 0.2s ease;
                }
                .btn-primary:hover {
                    transform: translateY(-1px);
                    filter: brightness(1.05);
                }
                .btn-calendar {
                    background: #ffffff;
                    color: #334155;
                    border: 1px solid #cbd5e1;
                    transition: all 0.2s ease;
                }
                .btn-calendar:hover {
                    background: #f1f5f9;
                }
                .summary-card {
                    background: white;
                    padding: 16px;
                    borderRadius: 16px;
                    border: 1px solid #e2e8f0;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.01);
                    transition: all 0.2s;
                }
                .summary-card:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 8px 16px rgba(0,0,0,0.03);
                }
                .calendar-day-btn {
                    transition: all 0.2s ease;
                }
                .calendar-day-btn:hover {
                    background-color: #f1f5f9 !important;
                }
                .tx-card {
                    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                }
                .tx-card:hover {
                    transform: translateY(-3px);
                    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.04) !important;
                    border-color: #cbd5e1 !important;
                }
                .table-row {
                    transition: background-color 0.2s ease;
                }
                .table-row:hover {
                    background-color: #f8fafc !important;
                }
            `)

            /* Header Section */
            , React.createElement('div', { style: { display: 'flex', flexDirection: isMobile ? 'column' : 'row', justifyContent: 'space-between', alignItems: isMobile ? 'stretch' : 'center', marginBottom: '20px', gap: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 353}}
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 354}}
                    , React.createElement('h2', { style: { fontSize: isMobile ? '20px' : '24px', fontWeight: '800', margin: 0, color: '#0f172a' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 355}}, "Buku Bahan Baku"  )
                    , React.createElement('p', { style: { margin: '4px 0 0 0', color: '#64748b', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 356}}, "Pencatatan ambil bahan (Nono) & pembayaran"     )
                )

                /* Button Action Bar */
                , React.createElement('div', { style: { display: 'flex', gap: '8px', width: isMobile ? '100%' : 'auto' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 360}}
                    , React.createElement('button', {
                        onClick: () => setShowCalendar(!showCalendar),
                        className: "btn-calendar",
                        style: {
                            flex: isMobile ? 1 : 'none',
                            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
                            padding: '10px 16px', borderRadius: '10px', cursor: 'pointer',
                            fontWeight: '700', fontSize: '13px'
                        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 361}}

                        , React.createElement(Calendar, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 371}} ), " " , showCalendar ? 'Sembunyikan Kalender' : 'Buka Kalender'
                    )
                    , React.createElement('button', {
                        onClick: openModal,
                        className: "btn-primary",
                        style: {
                            flex: isMobile ? 1 : 'none',
                            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
                            padding: '10px 16px', borderRadius: '10px', cursor: 'pointer',
                            fontWeight: '700', fontSize: '13px'
                        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 373}}

                        , React.createElement(PlusCircle, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 383}} ), " Transaksi Baru"
                    )
                )
            )

            /* Collapsible Calendar Section */
            , showCalendar && (
                React.createElement('div', { style: { background: 'white', borderRadius: '18px', padding: '16px', border: '1px solid #e2e8f0', marginBottom: '16px', boxShadow: '0 4px 10px rgba(0,0,0,0.02)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 390}}
                    /* Calendar Header with Navigation */
                    , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 392}}
                        , React.createElement('h4', { style: { margin: 0, fontSize: '14px', fontWeight: '800', color: '#1e293b', display: 'flex', alignItems: 'center', gap: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 393}}
                            , React.createElement(Calendar, { size: 16, color: "#0d6efd", __self: this, __source: {fileName: _jsxFileName, lineNumber: 394}} ), " Kalender Transaksi"
                        )
                        , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 396}}
                            , React.createElement('button', { 
                                onClick: prevMonth, 
                                style: { background: '#f1f5f9', border: 'none', borderRadius: '8px', width: '28px', height: '28px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 397}}

                                , React.createElement(ChevronLeft, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 401}} )
                            )
                            , React.createElement('span', { style: { fontSize: '13px', fontWeight: '700', color: '#334155', minWidth: '110px', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 403}}
                                , MONTH_NAMES[calendarDate.getMonth()], " " , calendarDate.getFullYear()
                            )
                            , React.createElement('button', { 
                                onClick: nextMonth, 
                                style: { background: '#f1f5f9', border: 'none', borderRadius: '8px', width: '28px', height: '28px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 406}}

                                , React.createElement(ChevronRight, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 410}} )
                            )
                        )
                    )

                    /* Weekdays Header */
                    , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '4px', textAlign: 'center', marginBottom: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 416}}
                        , ['Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab', 'Min'].map(day => (
                            React.createElement('div', { key: day, style: { fontSize: '11px', fontWeight: '700', color: '#94a3b8', padding: '2px 0' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 418}}, day)
                        ))
                    )

                    /* Days Grid */
                    , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 423}}
                        , getDaysArray(calendarDate.getFullYear(), calendarDate.getMonth()).map((dayObj, idx) => {
                            if (!dayObj) {
                                return React.createElement('div', { key: `empty-${idx}`, style: { padding: '8px 0' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 426}} );
                            }
                            
                            const dateStr = toLocalYYYYMMDD(dayObj);
                            const txs = getTransactionsForDate(dateStr);
                            const hasIn = txs.some((tx) => tx.TotalHarga > 0);
                            const hasOut = txs.some((tx) => tx.Nominal > 0);
                            const isSelected = selectedCalendarDay === dateStr;
                            const isToday = toLocalYYYYMMDD(new Date()) === dateStr;
                            
                            return (
                                React.createElement('button', {
                                    key: dateStr,
                                    onClick: () => {
                                        if (isSelected) {
                                            setSelectedCalendarDay(null); // toggle off
                                        } else {
                                            setSelectedCalendarDay(dateStr);
                                        }
                                    },
                                    className: "calendar-day-btn",
                                    style: {
                                        background: isSelected ? '#ebf4ff' : 'transparent',
                                        border: isSelected ? '1.5px solid #0d6efd' : '1px solid transparent',
                                        borderRadius: '10px',
                                        padding: '4px 0',
                                        cursor: 'pointer',
                                        display: 'flex',
                                        flexDirection: 'column',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        minHeight: '40px',
                                        position: 'relative'
                                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 437}}

                                    , React.createElement('span', { style: { 
                                        fontSize: '12px', 
                                        fontWeight: isSelected || isToday ? '800' : '500',
                                        color: isSelected ? '#0d6efd' : isToday ? '#0f172a' : '#475569',
                                        background: isToday && !isSelected ? '#f1f5f9' : 'transparent',
                                        width: '22px',
                                        height: '22px',
                                        borderRadius: '50%',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center'
                                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 461}}
                                        , dayObj.getDate()
                                    )

                                    /* Dot Indicators */
                                    , React.createElement('div', { style: { display: 'flex', gap: '3px', marginTop: '2px', height: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 477}}
                                        , hasIn && React.createElement('div', { style: { width: '4px', height: '4px', borderRadius: '50%', background: '#10b981' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 478}} )
                                        , hasOut && React.createElement('div', { style: { width: '4px', height: '4px', borderRadius: '50%', background: '#ef4444' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 479}} )
                                    )
                                )
                            );
                        })
                    )
                )
            )

            /* Summary Row */
            , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(3, 1fr)', gap: '12px', marginBottom: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 489}}
                , React.createElement('div', { className: "summary-card", style: { borderLeft: '4px solid #10b981' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 490}}
                    , React.createElement('div', { style: { color: '#64748b', fontSize: '11px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 491}}, React.createElement(ShoppingCart, { size: 13, __self: this, __source: {fileName: _jsxFileName, lineNumber: 491}}), " Total Belanja"  )
                    , React.createElement('div', { style: { fontSize: '20px', fontWeight: '800', color: '#10b981' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 492}}, "Rp " , summary.total_cash_in.toLocaleString('id-ID'))
                )
                , React.createElement('div', { className: "summary-card", style: { borderLeft: '4px solid #ef4444' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 494}}
                    , React.createElement('div', { style: { color: '#64748b', fontSize: '11px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 495}}, React.createElement(DollarSign, { size: 13, __self: this, __source: {fileName: _jsxFileName, lineNumber: 495}}), " Sudah Dibayar"  )
                    , React.createElement('div', { style: { fontSize: '20px', fontWeight: '800', color: '#ef4444' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 496}}, "Rp " , summary.total_cash_out.toLocaleString('id-ID'))
                )
                , React.createElement('div', { className: "summary-card", style: { background: '#fffbeb', borderLeft: '4px solid #d97706', borderColor: '#f59e0b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 498}}
                    , React.createElement('div', { style: { color: '#b45309', fontSize: '11px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 499}}, React.createElement(Info, { size: 13, __self: this, __source: {fileName: _jsxFileName, lineNumber: 499}}), " Sisa Hutang"  )
                    , React.createElement('div', { style: { fontSize: '20px', fontWeight: '800', color: '#b45309' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 500}}, "Rp " , summary.balance.toLocaleString('id-ID'))
                )
            )

            /* Active Calendar Filter Alert Banner */
            , selectedCalendarDay && (
                React.createElement('div', { style: { background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: '12px', padding: '10px 14px', marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 506}}
                    , React.createElement('div', { style: { fontSize: '13px', color: '#1e40af', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 507}}
                        , React.createElement(Filter, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 508}} )
                        , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 509}}, "Menampilkan transaksi tanggal: "   , React.createElement('strong', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 509}}, new Date(selectedCalendarDay).toLocaleDateString('id-ID', { day: 'numeric', month: 'long', year: 'numeric' })))
                    )
                    , React.createElement('button', { 
                        onClick: () => setSelectedCalendarDay(null),
                        style: { border: 'none', background: 'none', color: '#1e40af', cursor: 'pointer', display: 'flex', padding: '2px' },
                        title: "Hapus Filter Tanggal"  , __self: this, __source: {fileName: _jsxFileName, lineNumber: 511}}

                        , React.createElement(X, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 516}} )
                    )
                )
            )

            /* Filter Tabs */
            , React.createElement('div', { style: { display: 'flex', gap: '4px', marginBottom: '16px', background: '#f1f5f9', padding: '4px', borderRadius: '10px', width: isMobile ? '100%' : 'fit-content' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 522}}
                , ['ALL', 'IN', 'OUT'].map((type) => (
                    React.createElement('button', {
                        key: type,
                        onClick: () => setFilterType(type ),
                        style: {
                            flex: isMobile ? 1 : 'none',
                            padding: '8px 16px',
                            borderRadius: '8px',
                            border: 'none',
                            cursor: 'pointer',
                            fontWeight: '700',
                            fontSize: '12px',
                            background: filterType === type ? 'white' : 'transparent',
                            color: filterType === type ? '#0d6efd' : '#64748b',
                            boxShadow: filterType === type ? '0 1px 3px rgba(0,0,0,0.05)' : 'none',
                            transition: 'all 0.15s',
                            whiteSpace: 'nowrap'
                        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 524}}

                        , type === 'ALL' ? 'Semua' : type === 'IN' ? 'Terima (IN)' : 'Bayar (OUT)'
                    )
                ))
            )

            , isMobile ? (
                /* Mobile Card List (Responsive and Compact) */
                React.createElement('div', { style: { display: 'flex', flexDirection: 'column', gap: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 549}}
                    , filteredData.map((d) => (
                        React.createElement('div', { key: d.ID, onClick: () => openDetail(d), className: "tx-card", style: { background: 'white', borderRadius: '16px', padding: '14px', border: '1px solid #e2e8f0', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 551}}
                            , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 552}}
                                , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 553}}
                                    , React.createElement('div', { style: { background: d.TotalHarga > 0 ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)', color: d.TotalHarga > 0 ? '#10b981' : '#ef4444', padding: '5px', borderRadius: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 554}}
                                        , d.TotalHarga > 0 ? React.createElement(ArrowDownRight, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 555}}) : React.createElement(ArrowUpRight, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 555}})
                                    )
                                    , React.createElement('span', { style: { fontSize: '12px', fontWeight: '700', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 557}}
                                        , new Date(d.Tanggal).toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
                                    )
                                )
                                , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 561}}
                                    , !d.Tagihan && (
                                        React.createElement('div', { style: { background: '#fef2f2', color: '#ef4444', padding: '3px', borderRadius: '50%', display: 'flex' }, title: "Bukti belum diunggah"  , __self: this, __source: {fileName: _jsxFileName, lineNumber: 563}}
                                            , React.createElement(AlertCircle, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 564}} )
                                        )
                                    )
                                    , React.createElement('span', { style: { fontSize: '13px', fontWeight: '800', color: d.TotalHarga > 0 ? '#10b981' : '#ef4444' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 567}}, "Rp "
                                         , (d.TotalHarga || d.Nominal).toLocaleString('id-ID')
                                    )
                                )
                            )
                            , React.createElement('div', { style: { color: '#475569', fontSize: '12px', borderTop: '1px solid #f1f5f9', paddingTop: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 572}}
                                , React.createElement('span', { style: { overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '75%', color: '#64748b', fontStyle: d.Notes ? 'normal' : 'italic' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 573}}
                                    , d.Notes || 'Tanpa catatan'
                                )
                                , React.createElement('span', { style: { color: '#0d6efd', fontSize: '11px', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '2px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 576}}, "Detail "
                                     , React.createElement(Eye, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 577}} )
                                )
                            )
                        )
                    ))
                )
            ) : (
                /* Desktop Table View (Polished and Tighter) */
                React.createElement('div', { style: { background: 'white', borderRadius: '18px', padding: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.01)', border: '1px solid #e2e8f0', overflow: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 585}}
                    , React.createElement('table', { style: { width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 586}}
                        , React.createElement('thead', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 587}}
                            , React.createElement('tr', { style: { color: '#64748b', borderBottom: '1px solid #f1f5f9', background: '#f8fafc' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 588}}
                                , React.createElement('th', { style: { padding: '12px 16px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 589}}, "Tanggal")
                                , React.createElement('th', { style: { padding: '12px 16px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 590}}, "Tipe")
                                , React.createElement('th', { style: { padding: '12px 16px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 591}}, "Keterangan")
                                , React.createElement('th', { style: { padding: '12px 16px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 592}}, "Detail Item" )
                                , React.createElement('th', { style: { padding: '12px 16px', fontWeight: '700', textAlign: 'right' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 593}}, "Nominal")
                                , React.createElement('th', { style: { padding: '12px 16px', fontWeight: '700', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 594}}, "Aksi")
                            )
                        )
                        , React.createElement('tbody', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 597}}
                            , filteredData.map((d) => (
                                React.createElement('tr', { key: d.ID, className: "table-row", style: { borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 599}}
                                    , React.createElement('td', { style: { padding: '12px 16px', fontWeight: '600', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 600}}
                                        , new Date(d.Tanggal).toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
                                    )
                                    , React.createElement('td', { style: { padding: '12px 16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 603}}
                                        , React.createElement('span', { style: { padding: '3px 8px', borderRadius: '6px', fontSize: '10px', fontWeight: '800', background: d.TotalHarga > 0 ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)', color: d.TotalHarga > 0 ? '#10b981' : '#ef4444' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 604}}
                                            , d.TotalHarga > 0 ? 'TERIMA (IN)' : 'BAYAR (OUT)'
                                        )
                                    )
                                    , React.createElement('td', { style: { padding: '12px 16px', color: '#64748b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '200px' }, title: d.Notes, __self: this, __source: {fileName: _jsxFileName, lineNumber: 608}}
                                        , d.Notes || '-'
                                    )
                                    , React.createElement('td', { style: { padding: '12px 16px', color: '#475569' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 611}}
                                        , React.createElement('span', { style: { fontSize: '11px', background: '#f1f5f9', padding: '2px 6px', borderRadius: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 612}}
                                            , _optionalChain([d, 'access', _11 => _11.Items, 'optionalAccess', _12 => _12.length]) > 0 ? `${d.Items.length} jenis bahan` : '-'
                                        )
                                    )
                                    , React.createElement('td', { style: { padding: '12px 16px', fontWeight: '800', color: d.TotalHarga > 0 ? '#10b981' : '#ef4444', textAlign: 'right' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 616}}
                                        , React.createElement('div', { style: { display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 617}}
                                            , !d.Tagihan && React.createElement('span', { title: "Bukti belum diunggah"  , __self: this, __source: {fileName: _jsxFileName, lineNumber: 618}}, React.createElement(AlertCircle, { size: 13, color: "#ef4444", __self: this, __source: {fileName: _jsxFileName, lineNumber: 618}} )), "Rp "
                                             , (d.TotalHarga || d.Nominal).toLocaleString('id-ID')
                                        )
                                    )
                                    , React.createElement('td', { style: { padding: '12px 16px', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 622}}
                                        , React.createElement('button', { 
                                            onClick: () => openDetail(d), 
                                            style: { background: '#ebf4ff', color: '#0d6efd', border: 'none', padding: '6px 12px', borderRadius: '8px', cursor: 'pointer', fontWeight: '700', fontSize: '11px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 623}}
, "Detail"

                                        )
                                    )
                                )
                            ))
                        )
                    )
                )
            )

            , filteredData.length === 0 && !loading && (
                React.createElement('div', { style: { textAlign: 'center', padding: '50px 20px', color: '#64748b', background: 'white', borderRadius: '18px', border: '1px solid #e2e8f0', marginTop: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 638}}
                    , React.createElement(Package, { size: 40, style: { margin: '0 auto 12px', opacity: 0.15, display: 'block' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 639}} )
                    , React.createElement('span', { style: { fontSize: '14px', fontWeight: '600' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 640}}, "Tidak ada data transaksi ditemukan"    )
                    , React.createElement('p', { style: { fontSize: '12px', color: '#94a3b8', margin: '4px 0 0 0' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 641}}, "Coba ubah filter atau tanggal kalender Anda."      )
                )
            )

            /* MODAL DETAIL */
            , showDetailModal && selectedItem && (
                React.createElement('div', { style: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.45)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1200, padding: '15px', backdropFilter: 'blur(4px)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 647}}
                    , React.createElement('div', { style: { background: 'white', borderRadius: '20px', width: '100%', maxWidth: '440px', overflow: 'hidden', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)', maxHeight: '90vh', display: 'flex', flexDirection: 'column' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 648}}
                        , React.createElement('div', { style: { background: selectedItem.TotalHarga > 0 ? '#10b981' : '#ef4444', color: 'white', padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 649}}
                            , React.createElement('h3', { style: { margin: 0, fontSize: '16px', fontWeight: '800' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 650}}, "Detail Transaksi" )
                            , React.createElement('button', { onClick: () => setShowDetailModal(false), style: { background: 'rgba(0,0,0,0.1)', border: 'none', color: 'white', borderRadius: '50%', width: '28px', height: '28px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 651}}, React.createElement(X, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 651}}))
                        )

                        , React.createElement('div', { style: { padding: '20px', overflowY: 'auto', flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 654}}
                            , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 655}}
                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 656}}
                                    , React.createElement('div', { style: { color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '3px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 657}}, "Tanggal")
                                    , React.createElement('div', { style: { fontWeight: '700', color: '#1e293b', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 658}}, new Date(selectedItem.Tanggal).toLocaleDateString('id-ID', { day: 'numeric', month: 'long', year: 'numeric' }))
                                )
                                , React.createElement('div', { style: { textAlign: 'right' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 660}}
                                    , React.createElement('div', { style: { color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '3px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 661}}, "Tipe")
                                    , React.createElement('span', { style: { padding: '3px 8px', borderRadius: '6px', fontSize: '9px', fontWeight: '800', background: selectedItem.TotalHarga > 0 ? '#dcfce7' : '#fee2e2', color: selectedItem.TotalHarga > 0 ? '#15803d' : '#b91c1c' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 662}}
                                        , selectedItem.TotalHarga > 0 ? 'TERIMA BAHAN' : 'PEMBAYARAN'
                                    )
                                )
                            )

                            , React.createElement('div', { style: { marginBottom: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 668}}
                                , React.createElement('div', { style: { color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '3px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 669}}, "Catatan")
                                , React.createElement('div', { style: { fontWeight: '600', color: '#334155', background: '#f8fafc', padding: '10px 12px', borderRadius: '10px', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 670}}, selectedItem.Notes || '-')
                            )

                            , selectedItem.Items && selectedItem.Items.filter((i) => !i.JenisBahan.startsWith('[JASA]')).length > 0 && (
                                React.createElement('div', { style: { marginBottom: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 674}}
                                    , React.createElement('div', { style: { color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 675}}, "Rincian Bahan" )
                                    , React.createElement('div', { style: { border: '1px solid #e2e8f0', borderRadius: '12px', overflow: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 676}}
                                        , selectedItem.Items.filter((i) => !i.JenisBahan.startsWith('[JASA]')).map((item, idx, arr) => (
                                            React.createElement('div', { key: item.ID, style: { display: 'flex', justifyContent: 'space-between', padding: '10px 12px', borderBottom: idx === arr.length - 1 ? 'none' : '1px solid #e2e8f0', background: idx % 2 === 0 ? 'white' : '#f8fafc', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 678}}
                                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 679}}
                                                    , React.createElement('div', { style: { fontWeight: '700', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 680}}, item.JenisBahan)
                                                    , React.createElement('div', { style: { fontSize: '10px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 681}}, item.Kuantitas, " " , item.Unit, " @ Rp "   , item.Rate.toLocaleString('id-ID'))
                                                )
                                                , React.createElement('div', { style: { fontWeight: '700', color: '#1e293b', alignSelf: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 683}}, "Rp " , (item.Kuantitas * item.Rate).toLocaleString('id-ID'))
                                            )
                                        ))
                                    )
                                )
                            )

                            , selectedItem.Items && selectedItem.Items.filter((i) => i.JenisBahan.startsWith('[JASA]')).length > 0 && (
                                React.createElement('div', { style: { marginBottom: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 691}}
                                    , React.createElement('div', { style: { color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 692}}, "Rincian Biaya Jasa"  )
                                    , React.createElement('div', { style: { border: '1px solid #e2e8f0', borderRadius: '12px', overflow: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 693}}
                                        , selectedItem.Items.filter((i) => i.JenisBahan.startsWith('[JASA]')).map((item, idx, arr) => (
                                            React.createElement('div', { key: item.ID, style: { display: 'flex', justifyContent: 'space-between', padding: '10px 12px', borderBottom: idx === arr.length - 1 ? 'none' : '1px solid #e2e8f0', background: idx % 2 === 0 ? 'white' : '#f8fafc', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 695}}
                                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 696}}
                                                    , React.createElement('div', { style: { fontWeight: '700', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 697}}, item.JenisBahan.replace('[JASA] ', ''))
                                                    , React.createElement('div', { style: { fontSize: '10px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 698}}, item.Kuantitas, " " , item.Unit, " @ Rp "   , item.Rate.toLocaleString('id-ID'))
                                                )
                                                , React.createElement('div', { style: { fontWeight: '700', color: '#1e293b', alignSelf: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 700}}, "Rp " , (item.Kuantitas * item.Rate).toLocaleString('id-ID'))
                                            )
                                        ))
                                    )
                                )
                            )

                            , React.createElement('div', { style: { background: '#f8fafc', padding: '14px', borderRadius: '14px', marginBottom: '18px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', border: '1px solid #e2e8f0' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 707}}
                                , React.createElement('span', { style: { fontWeight: '700', color: '#1e293b', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 708}}, "Total Transaksi" )
                                , React.createElement('div', { style: { fontWeight: '800', fontSize: '16px', color: selectedItem.TotalHarga > 0 ? '#10b981' : '#ef4444' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 709}}, "Rp " , (selectedItem.TotalHarga || selectedItem.Nominal).toLocaleString('id-ID'))
                            )

                            , selectedItem.Tagihan && (
                                React.createElement('button', {
                                    onClick: () => {
                                        const imgUrl = formatImageUrl(selectedItem.Tagihan);
                                        if ((window ).Capacitor) {
                                            window.open(imgUrl, '_system');
                                        } else {
                                            window.open(imgUrl, '_blank');
                                        }
                                    },
                                    style: { width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', padding: '10px', background: 'white', color: '#0d6efd', border: '1.5px solid #0d6efd', borderRadius: '10px', fontWeight: 'bold', cursor: 'pointer', marginBottom: '12px', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 713}}

                                    , React.createElement(Paperclip, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 724}} ), " Lihat Bukti / Nota"
                                )
                            )

                            , React.createElement('div', { style: { display: 'flex', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 728}}
                                , React.createElement('button', { onClick: () => handleDelete(selectedItem.ID), style: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', padding: '10px', background: '#fee2e2', color: '#b91c1c', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: 'bold', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 729}}
                                    , React.createElement(Trash2, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 730}} ), " Hapus"
                                )
                                , React.createElement('button', { onClick: openEdit, style: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', padding: '10px', background: '#f1f5f9', color: '#64748b', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: 'bold', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 732}}
                                    , React.createElement(Edit2, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 733}} ), " Edit"
                                )
                            )
                        )
                    )
                )
            )

            /* MODAL TAMBAH/EDIT */
            , showModal && (
                React.createElement('div', { style: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.45)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1200, padding: '15px', backdropFilter: 'blur(4px)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 743}}
                    , React.createElement('div', { style: { background: 'white', padding: isMobile ? '20px' : '25px', borderRadius: '20px', width: '100%', maxWidth: '480px', maxHeight: '90vh', overflowY: 'auto', position: 'relative', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 744}}
                        , React.createElement('button', { onClick: () => setShowModal(false), style: { position: 'absolute', right: '16px', top: '16px', background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 745}}, React.createElement(X, { size: 20, __self: this, __source: {fileName: _jsxFileName, lineNumber: 745}} ))

                        , React.createElement('h3', { style: { marginBottom: '20px', fontSize: '18px', fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 747}}, isEditing ? '📝 Edit Transaksi' : '📦 Transaksi Bahan Baku')

                        , React.createElement('div', { style: { display: 'flex', gap: '8px', marginBottom: '20px', background: '#f1f5f9', padding: '4px', borderRadius: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 749}}
                            , React.createElement('button', { type: "button", onClick: () => setTab('IN'), style: { flex: 1, padding: '10px', fontWeight: '800', background: tab === 'IN' ? '#10b981' : 'transparent', color: tab === 'IN' ? 'white' : '#64748b', border: 'none', borderRadius: '8px', cursor: 'pointer', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 750}}, "TERIMA BAHAN" )
                            , React.createElement('button', { type: "button", onClick: () => setTab('OUT'), style: { flex: 1, padding: '10px', fontWeight: '800', background: tab === 'OUT' ? '#ef4444' : 'transparent', color: tab === 'OUT' ? 'white' : '#64748b', border: 'none', borderRadius: '8px', cursor: 'pointer', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 751}}, "BAYAR HUTANG" )
                        )

                        , React.createElement('form', { onSubmit: handleSave, __self: this, __source: {fileName: _jsxFileName, lineNumber: 754}}
                            , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: '12px', marginBottom: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 755}}
                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 756}}
                                    , React.createElement('label', { style: { display: 'block', marginBottom: '6px', fontSize: '12px', fontWeight: '700', color: '#475569' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 757}}, "Tanggal")
                                    , React.createElement('input', { type: "date", required: true, value: tanggal, onChange: e => setTanggal(e.target.value), style: { width: '100%', padding: '10px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '13px', boxSizing: 'border-box' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 758}} )
                                )
                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 760}}
                                    , React.createElement('label', { style: { display: 'block', marginBottom: '6px', fontSize: '12px', fontWeight: '700', color: '#475569' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 761}}, "Catatan")
                                    , React.createElement('input', { type: "text", value: notes, onChange: e => setNotes(e.target.value), style: { width: '100%', padding: '10px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '13px', boxSizing: 'border-box' }, placeholder: "Contoh: Ambil 10 ball"   , __self: this, __source: {fileName: _jsxFileName, lineNumber: 762}} )
                                )
                            )

                            , React.createElement('div', { style: { marginBottom: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 766}}
                                , React.createElement('label', { style: { display: 'block', marginBottom: '6px', fontSize: '12px', fontWeight: '700', color: '#475569' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 767}}, "Bukti Foto / Nota"   )
                                , React.createElement('div', { style: { border: '2px dashed #cbd5e1', borderRadius: '10px', padding: '12px', textAlign: 'center', position: 'relative' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 768}}
                                    , React.createElement('input', { type: "file", onChange: handleFileUpload, style: { position: 'absolute', inset: 0, opacity: 0, cursor: 'pointer' }, accept: "image/*,application/pdf", __self: this, __source: {fileName: _jsxFileName, lineNumber: 769}} )
                                    , React.createElement('div', { style: { color: uploading ? '#0d6efd' : tagihan ? '#10b981' : '#64748b', fontSize: '12px', fontWeight: '600' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 770}}
                                        , uploading ? 'Sedang mengunggah...' : tagihan ? '✅ Bukti Terunggah' : 'Klik/Sentuh untuk upload Bukti Foto'
                                    )
                                )
                            )

                            , tab === 'IN' && (
                                React.createElement('div', { style: { borderTop: '1px solid #f1f5f9', paddingTop: '15px', marginTop: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 777}}
                                    , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 778}}
                                        , React.createElement('h4', { style: { margin: 0, fontSize: '13px', fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 779}}, "Daftar Bahan Masuk"  )
                                        , React.createElement('button', { type: "button", onClick: () => setItems([...items, { jenis_bahan: 'Super A', kuantitas: 0, unit: 'Kg', rate: 0 }]), style: { background: '#eff6ff', color: '#0d6efd', border: 'none', padding: '4px 10px', borderRadius: '6px', cursor: 'pointer', fontSize: '11px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 780}}, "+ Baris" )
                                    )

                                    , items.map((item, idx) => (
                                        React.createElement('div', { key: idx, style: { display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '2.2fr 1fr 1.8fr 30px', gap: '6px', marginBottom: '10px', alignItems: 'center', borderBottom: isMobile ? '1px solid #f1f5f9' : 'none', paddingBottom: isMobile ? '8px' : '0' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 784}}
                                            , React.createElement('select', {
                                                value: item.jenis_bahan,
                                                onChange: e => { const n = [...items]; n[idx].jenis_bahan = e.target.value; setItems(n); },
                                                style: { width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px', background: 'white' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 785}}

                                                , React.createElement('option', { value: "", __self: this, __source: {fileName: _jsxFileName, lineNumber: 790}}, "-- Pilih Jenis --"   )
                                                , React.createElement('option', { value: "Cerah A" , __self: this, __source: {fileName: _jsxFileName, lineNumber: 791}}, "Cerah A" )
                                                , React.createElement('option', { value: "Cerah B" , __self: this, __source: {fileName: _jsxFileName, lineNumber: 792}}, "Cerah B" )
                                                , React.createElement('option', { value: "Super A" , __self: this, __source: {fileName: _jsxFileName, lineNumber: 793}}, "Super A" )
                                                , React.createElement('option', { value: "Super B" , __self: this, __source: {fileName: _jsxFileName, lineNumber: 794}}, "Super B" )
                                                , React.createElement('option', { value: "Lainnya", __self: this, __source: {fileName: _jsxFileName, lineNumber: 795}}, "Lainnya...")
                                            )

                                            , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 2fr 30px', gap: '6px', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 798}}
                                                , React.createElement('input', { type: "number", placeholder: "Qty", value: item.kuantitas || '', onChange: e => { const n = [...items]; n[idx].kuantitas = e.target.value === '' ? ''  : Number(e.target.value); setItems(n); }, style: { width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 799}} )
                                                , React.createElement('input', { type: "number", placeholder: "Harga", value: item.rate || '', onChange: e => { const n = [...items]; n[idx].rate = e.target.value === '' ? ''  : Number(e.target.value); setItems(n); }, style: { width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 800}} )
                                                , React.createElement('button', { type: "button", onClick: () => setItems(items.filter((_, i) => i !== idx)), style: { color: '#ef4444', background: 'none', border: 'none', cursor: 'pointer', display: 'flex', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 801}}, React.createElement(X, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 801}}))
                                            )
                                        )
                                    ))
                                )
                            )

                            , tab === 'IN' && (
                                React.createElement('div', { style: { borderTop: '1px solid #f1f5f9', paddingTop: '15px', marginTop: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 809}}
                                    , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 810}}
                                        , React.createElement('h4', { style: { margin: 0, fontSize: '13px', fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 811}}, "Daftar Biaya Jasa"  )
                                        , React.createElement('button', { type: "button", onClick: () => setJasaItems([...jasaItems, { jenis_bahan: '', kuantitas: 0, unit: 'Kg', rate: 0 }]), style: { background: '#fffbeb', color: '#d97706', border: 'none', padding: '4px 10px', borderRadius: '6px', cursor: 'pointer', fontSize: '11px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 812}}, "+ Baris Jasa"  )
                                    )

                                    , jasaItems.map((item, idx) => (
                                        React.createElement('div', { key: `jasa-${idx}`, style: { display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '2.2fr 1fr 1.8fr 30px', gap: '6px', marginBottom: '10px', alignItems: 'center', borderBottom: isMobile ? '1px solid #f1f5f9' : 'none', paddingBottom: isMobile ? '8px' : '0' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 816}}
                                            , React.createElement('input', { type: "text", placeholder: "Nama Jasa (Titip Giling)"   , value: item.jenis_bahan, onChange: e => { const n = [...jasaItems]; n[idx].jenis_bahan = e.target.value; setJasaItems(n); }, style: { width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 817}} )
                                            , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 2fr 30px', gap: '6px', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 818}}
                                                , React.createElement('input', { type: "number", placeholder: "Berat", value: item.kuantitas || '', onChange: e => { const n = [...jasaItems]; n[idx].kuantitas = e.target.value === '' ? ''  : Number(e.target.value); setJasaItems(n); }, style: { width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 819}} )
                                                , React.createElement('input', { type: "number", placeholder: "Harga", value: item.rate || '', onChange: e => { const n = [...jasaItems]; n[idx].rate = e.target.value === '' ? ''  : Number(e.target.value); setJasaItems(n); }, style: { width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 820}} )
                                                , React.createElement('button', { type: "button", onClick: () => setJasaItems(jasaItems.filter((_, i) => i !== idx)), style: { color: '#ef4444', background: 'none', border: 'none', cursor: 'pointer', display: 'flex', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 821}}, React.createElement(X, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 821}}))
                                            )
                                        )
                                    ))
                                )
                            )

                            , tab === 'OUT' && (
                                React.createElement('div', { style: { borderTop: '1px solid #f1f5f9', paddingTop: '15px', marginTop: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 829}}
                                    , React.createElement('label', { style: { display: 'block', marginBottom: '6px', fontSize: '12px', fontWeight: '700', color: '#475569' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 830}}, "Nominal Pembayaran (Rp)"  )
                                    , React.createElement('input', { type: "number", required: true, value: nominal || '', onChange: e => setNominal(e.target.value === '' ? ''  : Number(e.target.value)), style: { width: '100%', padding: '10px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '15px', fontWeight: '800', color: '#ef4444' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 831}} )
                                )
                            )

                            , React.createElement('div', { style: { display: 'flex', gap: '10px', marginTop: '25px', borderTop: '1px solid #f1f5f9', paddingTop: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 835}}
                                , React.createElement('button', { type: "button", onClick: () => setShowModal(false), style: { flex: 1, padding: '12px', background: '#f1f5f9', color: '#64748b', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: '700', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 836}}, "Batal")
                                , React.createElement('button', { type: "submit", disabled: uploading, style: { flex: 2, padding: '12px', background: 'linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%)', color: 'white', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: '700', fontSize: '13px', opacity: uploading ? 0.5 : 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 837}}, isEditing ? 'Update Transaksi' : 'Simpan Transaksi')
                            )
                        )
                    )
                )
            )
        )
    );
};

export default BahanNono;