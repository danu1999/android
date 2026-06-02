/* eslint-disable @typescript-eslint/no-explicit-any, react-hooks/set-state-in-effect */
import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { PlusCircle, ArrowUpRight, ArrowDownRight, Eye, Paperclip, X, Trash2, Edit2, Package, Info, ShoppingCart, DollarSign, AlertCircle, Calendar, ChevronLeft, ChevronRight, Filter } from 'lucide-react';

const BahanNono: React.FC = () => {
    const [data, setData] = useState<Record<string, unknown>[]>([]);
    const [summary, setSummary] = useState({ total_cash_in: 0, total_cash_out: 0, balance: 0 });
    const [loading, setLoading] = useState(true);
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

    // Filter states
    const [filterType, setFilterType] = useState<'ALL' | 'IN' | 'OUT'>('ALL');
    const [selectedCalendarDay, setSelectedCalendarDay] = useState<string | null>(null);

    // Calendar states
    const [showCalendar, setShowCalendar] = useState(false);
    const [calendarDate, setCalendarDate] = useState(new Date());

    const [showModal, setShowModal] = useState(false);
    const [showDetailModal, setShowDetailModal] = useState(false);
    const [selectedItem, setSelectedItem] = useState<Record<string, any> | null>(null);
    const [isEditing, setIsEditing] = useState(false);

    const [tab, setTab] = useState<'IN' | 'OUT'>('IN');
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
            setData(res.data?.data?.transactions || []);
            setSummary(res.data?.data || { total_cash_in: 0, total_cash_out: 0, balance: 0 });
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
            const bItems = selectedItem.Items.filter((i: any) => !i.JenisBahan.startsWith('[JASA]')).map((i: any) => ({
                jenis_bahan: i.JenisBahan,
                kuantitas: i.Kuantitas,
                unit: i.Unit,
                rate: i.Rate
            }));
            const jItems = selectedItem.Items.filter((i: any) => i.JenisBahan.startsWith('[JASA]')).map((i: any) => ({
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

    const compressImage = (file: File): Promise<File> => {
        return new Promise((resolve) => {
            const reader = new FileReader();
            reader.readAsDataURL(file);
            reader.onload = (event) => {
                const img = new Image();
                img.src = event.target?.result as string;
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
                    ctx?.drawImage(img, 0, 0, width, height);

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

    const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        if (!e.target.files || e.target.files.length === 0) return;

        let file: any = e.target.files[0];

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

    const handleSave = async (e: React.FormEvent) => {
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

    const formatImageUrl = (url: string) => {
        if (!url) return '';
        if (url.startsWith('http')) return url;
        return `${import.meta.env.VITE_API_URL || 'https://bmp.up.railway.app/api'}/files/${url}`;
    };

    const openDetail = (item: any) => {
        setSelectedItem(item);
        setShowDetailModal(true);
    };

    const handleDelete = async (id: number) => {
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
        if (filterType === 'IN') matchesType = (d as any).TotalHarga > 0;
        else if (filterType === 'OUT') matchesType = (d as any).Nominal > 0;
        
        let matchesDate = true;
        if (selectedCalendarDay) {
            const txDateStr = (d as any).Tanggal.split('T')[0];
            matchesDate = txDateStr === selectedCalendarDay;
        }
        
        return matchesType && matchesDate;
    });

    // Calendar helper functions
    const toLocalYYYYMMDD = (d: Date) => {
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    const getTransactionsForDate = (dateStr: string) => {
        return (data || []).filter(tx => {
            const txDateStr = (tx as any).Tanggal.split('T')[0];
            return txDateStr === dateStr;
        });
    };

    const getDaysArray = (year: number, month: number) => {
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
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }}>
            <div style={{ width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
            <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
            <div style={{ color: '#6c757d' }}>Memuat data bahan...</div>
        </div>
    );

    return (
        <div style={{ padding: isMobile ? '12px' : '20px', background: '#f8fafc', minHeight: '100%', maxWidth: '100vw', boxSizing: 'border-box', overflowX: 'hidden' }}>
            <style>{`
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
            `}</style>

            {/* Header Section */}
            <div style={{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', justifyContent: 'space-between', alignItems: isMobile ? 'stretch' : 'center', marginBottom: '20px', gap: '12px' }}>
                <div>
                    <h2 style={{ fontSize: isMobile ? '20px' : '24px', fontWeight: '800', margin: 0, color: '#0f172a' }}>Buku Bahan Baku</h2>
                    <p style={{ margin: '4px 0 0 0', color: '#64748b', fontSize: '13px' }}>Pencatatan ambil bahan (Nono) & pembayaran</p>
                </div>
                
                {/* Button Action Bar */}
                <div style={{ display: 'flex', gap: '8px', width: isMobile ? '100%' : 'auto' }}>
                    <button
                        onClick={() => setShowCalendar(!showCalendar)}
                        className="btn-calendar"
                        style={{
                            flex: isMobile ? 1 : 'none',
                            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
                            padding: '10px 16px', borderRadius: '10px', cursor: 'pointer',
                            fontWeight: '700', fontSize: '13px'
                        }}
                    >
                        <Calendar size={16} /> {showCalendar ? 'Sembunyikan Kalender' : 'Buka Kalender'}
                    </button>
                    <button
                        onClick={openModal}
                        className="btn-primary"
                        style={{
                            flex: isMobile ? 1 : 'none',
                            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
                            padding: '10px 16px', borderRadius: '10px', cursor: 'pointer',
                            fontWeight: '700', fontSize: '13px'
                        }}
                    >
                        <PlusCircle size={16} /> Transaksi Baru
                    </button>
                </div>
            </div>

            {/* Collapsible Calendar Section */}
            {showCalendar && (
                <div style={{ background: 'white', borderRadius: '18px', padding: '16px', border: '1px solid #e2e8f0', marginBottom: '16px', boxShadow: '0 4px 10px rgba(0,0,0,0.02)' }}>
                    {/* Calendar Header with Navigation */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                        <h4 style={{ margin: 0, fontSize: '14px', fontWeight: '800', color: '#1e293b', display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <Calendar size={16} color="#0d6efd" /> Kalender Transaksi
                        </h4>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <button 
                                onClick={prevMonth} 
                                style={{ background: '#f1f5f9', border: 'none', borderRadius: '8px', width: '28px', height: '28px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}
                            >
                                <ChevronLeft size={16} />
                            </button>
                            <span style={{ fontSize: '13px', fontWeight: '700', color: '#334155', minWidth: '110px', textAlign: 'center' }}>
                                {MONTH_NAMES[calendarDate.getMonth()]} {calendarDate.getFullYear()}
                            </span>
                            <button 
                                onClick={nextMonth} 
                                style={{ background: '#f1f5f9', border: 'none', borderRadius: '8px', width: '28px', height: '28px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}
                            >
                                <ChevronRight size={16} />
                            </button>
                        </div>
                    </div>

                    {/* Weekdays Header */}
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '4px', textAlign: 'center', marginBottom: '6px' }}>
                        {['Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab', 'Min'].map(day => (
                            <div key={day} style={{ fontSize: '11px', fontWeight: '700', color: '#94a3b8', padding: '2px 0' }}>{day}</div>
                        ))}
                    </div>

                    {/* Days Grid */}
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '4px' }}>
                        {getDaysArray(calendarDate.getFullYear(), calendarDate.getMonth()).map((dayObj, idx) => {
                            if (!dayObj) {
                                return <div key={`empty-${idx}`} style={{ padding: '8px 0' }} />;
                            }
                            
                            const dateStr = toLocalYYYYMMDD(dayObj);
                            const txs = getTransactionsForDate(dateStr);
                            const hasIn = txs.some((tx: any) => tx.TotalHarga > 0);
                            const hasOut = txs.some((tx: any) => tx.Nominal > 0);
                            const isSelected = selectedCalendarDay === dateStr;
                            const isToday = toLocalYYYYMMDD(new Date()) === dateStr;
                            
                            return (
                                <button
                                    key={dateStr}
                                    onClick={() => {
                                        if (isSelected) {
                                            setSelectedCalendarDay(null); // toggle off
                                        } else {
                                            setSelectedCalendarDay(dateStr);
                                        }
                                    }}
                                    className="calendar-day-btn"
                                    style={{
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
                                    }}
                                >
                                    <span style={{ 
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
                                    }}>
                                        {dayObj.getDate()}
                                    </span>
                                    
                                    {/* Dot Indicators */}
                                    <div style={{ display: 'flex', gap: '3px', marginTop: '2px', height: '4px' }}>
                                        {hasIn && <div style={{ width: '4px', height: '4px', borderRadius: '50%', background: '#10b981' }} />}
                                        {hasOut && <div style={{ width: '4px', height: '4px', borderRadius: '50%', background: '#ef4444' }} />}
                                    </div>
                                </button>
                            );
                        })}
                    </div>
                </div>
            )}

            {/* Summary Row */}
            <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(3, 1fr)', gap: '12px', marginBottom: '16px' }}>
                <div className="summary-card" style={{ borderLeft: '4px solid #10b981' }}>
                    <div style={{ color: '#64748b', fontSize: '11px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}><ShoppingCart size={13}/> Total Belanja</div>
                    <div style={{ fontSize: '20px', fontWeight: '800', color: '#10b981' }}>Rp {summary.total_cash_in.toLocaleString('id-ID')}</div>
                </div>
                <div className="summary-card" style={{ borderLeft: '4px solid #ef4444' }}>
                    <div style={{ color: '#64748b', fontSize: '11px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}><DollarSign size={13}/> Sudah Dibayar</div>
                    <div style={{ fontSize: '20px', fontWeight: '800', color: '#ef4444' }}>Rp {summary.total_cash_out.toLocaleString('id-ID')}</div>
                </div>
                <div className="summary-card" style={{ background: '#fffbeb', borderLeft: '4px solid #d97706', borderColor: '#f59e0b' }}>
                    <div style={{ color: '#b45309', fontSize: '11px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}><Info size={13}/> Sisa Hutang</div>
                    <div style={{ fontSize: '20px', fontWeight: '800', color: '#b45309' }}>Rp {summary.balance.toLocaleString('id-ID')}</div>
                </div>
            </div>

            {/* Active Calendar Filter Alert Banner */}
            {selectedCalendarDay && (
                <div style={{ background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: '12px', padding: '10px 14px', marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Filter size={14} /> 
                        <span>Menampilkan transaksi tanggal: <strong>{new Date(selectedCalendarDay).toLocaleDateString('id-ID', { day: 'numeric', month: 'long', year: 'numeric' })}</strong></span>
                    </div>
                    <button 
                        onClick={() => setSelectedCalendarDay(null)}
                        style={{ border: 'none', background: 'none', color: '#1e40af', cursor: 'pointer', display: 'flex', padding: '2px' }}
                        title="Hapus Filter Tanggal"
                    >
                        <X size={16} />
                    </button>
                </div>
            )}

            {/* Filter Tabs */}
            <div style={{ display: 'flex', gap: '4px', marginBottom: '16px', background: '#f1f5f9', padding: '4px', borderRadius: '10px', width: isMobile ? '100%' : 'fit-content' }}>
                {['ALL', 'IN', 'OUT'].map((type) => (
                    <button
                        key={type}
                        onClick={() => setFilterType(type as any)}
                        style={{
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
                        }}
                    >
                        {type === 'ALL' ? 'Semua' : type === 'IN' ? 'Terima (IN)' : 'Bayar (OUT)'}
                    </button>
                ))}
            </div>

            {isMobile ? (
                /* Mobile Card List (Responsive and Compact) */
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {filteredData.map((d: any) => (
                        <div key={d.ID} onClick={() => openDetail(d)} className="tx-card" style={{ background: 'white', borderRadius: '16px', padding: '14px', border: '1px solid #e2e8f0', cursor: 'pointer' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                    <div style={{ background: d.TotalHarga > 0 ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)', color: d.TotalHarga > 0 ? '#10b981' : '#ef4444', padding: '5px', borderRadius: '8px' }}>
                                        {d.TotalHarga > 0 ? <ArrowDownRight size={14}/> : <ArrowUpRight size={14}/>}
                                    </div>
                                    <span style={{ fontSize: '12px', fontWeight: '700', color: '#64748b' }}>
                                        {new Date(d.Tanggal).toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })}
                                    </span>
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                    {!d.Tagihan && (
                                        <div style={{ background: '#fef2f2', color: '#ef4444', padding: '3px', borderRadius: '50%', display: 'flex' }} title="Bukti belum diunggah">
                                            <AlertCircle size={12} />
                                        </div>
                                    )}
                                    <span style={{ fontSize: '13px', fontWeight: '800', color: d.TotalHarga > 0 ? '#10b981' : '#ef4444' }}>
                                        Rp {(d.TotalHarga || d.Nominal).toLocaleString('id-ID')}
                                    </span>
                                </div>
                            </div>
                            <div style={{ color: '#475569', fontSize: '12px', borderTop: '1px solid #f1f5f9', paddingTop: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '75%', color: '#64748b', fontStyle: d.Notes ? 'normal' : 'italic' }}>
                                    {d.Notes || 'Tanpa catatan'}
                                </span>
                                <span style={{ color: '#0d6efd', fontSize: '11px', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '2px' }}>
                                    Detail <Eye size={12} />
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                /* Desktop Table View (Polished and Tighter) */
                <div style={{ background: 'white', borderRadius: '18px', padding: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.01)', border: '1px solid #e2e8f0', overflow: 'hidden' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
                        <thead>
                            <tr style={{ color: '#64748b', borderBottom: '1px solid #f1f5f9', background: '#f8fafc' }}>
                                <th style={{ padding: '12px 16px', fontWeight: '700' }}>Tanggal</th>
                                <th style={{ padding: '12px 16px', fontWeight: '700' }}>Tipe</th>
                                <th style={{ padding: '12px 16px', fontWeight: '700' }}>Keterangan</th>
                                <th style={{ padding: '12px 16px', fontWeight: '700' }}>Detail Item</th>
                                <th style={{ padding: '12px 16px', fontWeight: '700', textAlign: 'right' }}>Nominal</th>
                                <th style={{ padding: '12px 16px', fontWeight: '700', textAlign: 'center' }}>Aksi</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredData.map((d: any) => (
                                <tr key={d.ID} className="table-row" style={{ borderBottom: '1px solid #f1f5f9' }}>
                                    <td style={{ padding: '12px 16px', fontWeight: '600', color: '#1e293b' }}>
                                        {new Date(d.Tanggal).toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })}
                                    </td>
                                    <td style={{ padding: '12px 16px' }}>
                                        <span style={{ padding: '3px 8px', borderRadius: '6px', fontSize: '10px', fontWeight: '800', background: d.TotalHarga > 0 ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)', color: d.TotalHarga > 0 ? '#10b981' : '#ef4444' }}>
                                            {d.TotalHarga > 0 ? 'TERIMA (IN)' : 'BAYAR (OUT)'}
                                        </span>
                                    </td>
                                    <td style={{ padding: '12px 16px', color: '#64748b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '200px' }} title={d.Notes}>
                                        {d.Notes || '-'}
                                    </td>
                                    <td style={{ padding: '12px 16px', color: '#475569' }}>
                                        <span style={{ fontSize: '11px', background: '#f1f5f9', padding: '2px 6px', borderRadius: '4px' }}>
                                            {d.Items?.length > 0 ? `${d.Items.length} jenis bahan` : '-'}
                                        </span>
                                    </td>
                                    <td style={{ padding: '12px 16px', fontWeight: '800', color: d.TotalHarga > 0 ? '#10b981' : '#ef4444', textAlign: 'right' }}>
                                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '6px' }}>
                                            {!d.Tagihan && <span title="Bukti belum diunggah"><AlertCircle size={13} color="#ef4444" /></span>}
                                            Rp {(d.TotalHarga || d.Nominal).toLocaleString('id-ID')}
                                        </div>
                                    </td>
                                    <td style={{ padding: '12px 16px', textAlign: 'center' }}>
                                        <button 
                                            onClick={() => openDetail(d)} 
                                            style={{ background: '#ebf4ff', color: '#0d6efd', border: 'none', padding: '6px 12px', borderRadius: '8px', cursor: 'pointer', fontWeight: '700', fontSize: '11px' }}
                                        >
                                            Detail
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {filteredData.length === 0 && !loading && (
                <div style={{ textAlign: 'center', padding: '50px 20px', color: '#64748b', background: 'white', borderRadius: '18px', border: '1px solid #e2e8f0', marginTop: '10px' }}>
                    <Package size={40} style={{ margin: '0 auto 12px', opacity: 0.15, display: 'block' }} />
                    <span style={{ fontSize: '14px', fontWeight: '600' }}>Tidak ada data transaksi ditemukan</span>
                    <p style={{ fontSize: '12px', color: '#94a3b8', margin: '4px 0 0 0' }}>Coba ubah filter atau tanggal kalender Anda.</p>
                </div>
            )}

            {/* MODAL DETAIL */}
            {showDetailModal && selectedItem && (
                <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.45)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1200, padding: '15px', backdropFilter: 'blur(4px)' }}>
                    <div style={{ background: 'white', borderRadius: '20px', width: '100%', maxWidth: '440px', overflow: 'hidden', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)', maxHeight: '90vh', display: 'flex', flexDirection: 'column' }}>
                        <div style={{ background: selectedItem.TotalHarga > 0 ? '#10b981' : '#ef4444', color: 'white', padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: '800' }}>Detail Transaksi</h3>
                            <button onClick={() => setShowDetailModal(false)} style={{ background: 'rgba(0,0,0,0.1)', border: 'none', color: 'white', borderRadius: '50%', width: '28px', height: '28px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}><X size={16}/></button>
                        </div>

                        <div style={{ padding: '20px', overflowY: 'auto', flex: 1 }}>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '18px' }}>
                                <div>
                                    <div style={{ color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '3px' }}>Tanggal</div>
                                    <div style={{ fontWeight: '700', color: '#1e293b', fontSize: '13px' }}>{new Date(selectedItem.Tanggal).toLocaleDateString('id-ID', { day: 'numeric', month: 'long', year: 'numeric' })}</div>
                                </div>
                                <div style={{ textAlign: 'right' }}>
                                    <div style={{ color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '3px' }}>Tipe</div>
                                    <span style={{ padding: '3px 8px', borderRadius: '6px', fontSize: '9px', fontWeight: '800', background: selectedItem.TotalHarga > 0 ? '#dcfce7' : '#fee2e2', color: selectedItem.TotalHarga > 0 ? '#15803d' : '#b91c1c' }}>
                                        {selectedItem.TotalHarga > 0 ? 'TERIMA BAHAN' : 'PEMBAYARAN'}
                                    </span>
                                </div>
                            </div>

                            <div style={{ marginBottom: '18px' }}>
                                <div style={{ color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '3px' }}>Catatan</div>
                                <div style={{ fontWeight: '600', color: '#334155', background: '#f8fafc', padding: '10px 12px', borderRadius: '10px', fontSize: '12px' }}>{selectedItem.Notes || '-'}</div>
                            </div>

                            {selectedItem.Items && selectedItem.Items.filter((i: any) => !i.JenisBahan.startsWith('[JASA]')).length > 0 && (
                                <div style={{ marginBottom: '18px' }}>
                                    <div style={{ color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '6px' }}>Rincian Bahan</div>
                                    <div style={{ border: '1px solid #e2e8f0', borderRadius: '12px', overflow: 'hidden' }}>
                                        {selectedItem.Items.filter((i: any) => !i.JenisBahan.startsWith('[JASA]')).map((item: any, idx: number, arr: any[]) => (
                                            <div key={item.ID} style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 12px', borderBottom: idx === arr.length - 1 ? 'none' : '1px solid #e2e8f0', background: idx % 2 === 0 ? 'white' : '#f8fafc', fontSize: '12px' }}>
                                                <div>
                                                    <div style={{ fontWeight: '700', color: '#1e293b' }}>{item.JenisBahan}</div>
                                                    <div style={{ fontSize: '10px', color: '#64748b' }}>{item.Kuantitas} {item.Unit} @ Rp {item.Rate.toLocaleString('id-ID')}</div>
                                                </div>
                                                <div style={{ fontWeight: '700', color: '#1e293b', alignSelf: 'center' }}>Rp {(item.Kuantitas * item.Rate).toLocaleString('id-ID')}</div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {selectedItem.Items && selectedItem.Items.filter((i: any) => i.JenisBahan.startsWith('[JASA]')).length > 0 && (
                                <div style={{ marginBottom: '18px' }}>
                                    <div style={{ color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '6px' }}>Rincian Biaya Jasa</div>
                                    <div style={{ border: '1px solid #e2e8f0', borderRadius: '12px', overflow: 'hidden' }}>
                                        {selectedItem.Items.filter((i: any) => i.JenisBahan.startsWith('[JASA]')).map((item: any, idx: number, arr: any[]) => (
                                            <div key={item.ID} style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 12px', borderBottom: idx === arr.length - 1 ? 'none' : '1px solid #e2e8f0', background: idx % 2 === 0 ? 'white' : '#f8fafc', fontSize: '12px' }}>
                                                <div>
                                                    <div style={{ fontWeight: '700', color: '#1e293b' }}>{item.JenisBahan.replace('[JASA] ', '')}</div>
                                                    <div style={{ fontSize: '10px', color: '#64748b' }}>{item.Kuantitas} {item.Unit} @ Rp {item.Rate.toLocaleString('id-ID')}</div>
                                                </div>
                                                <div style={{ fontWeight: '700', color: '#1e293b', alignSelf: 'center' }}>Rp {(item.Kuantitas * item.Rate).toLocaleString('id-ID')}</div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}

                            <div style={{ background: '#f8fafc', padding: '14px', borderRadius: '14px', marginBottom: '18px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', border: '1px solid #e2e8f0' }}>
                                <span style={{ fontWeight: '700', color: '#1e293b', fontSize: '13px' }}>Total Transaksi</span>
                                <div style={{ fontWeight: '800', fontSize: '16px', color: selectedItem.TotalHarga > 0 ? '#10b981' : '#ef4444' }}>Rp {(selectedItem.TotalHarga || selectedItem.Nominal).toLocaleString('id-ID')}</div>
                            </div>

                            {selectedItem.Tagihan && (
                                <button
                                    onClick={() => {
                                        const imgUrl = formatImageUrl(selectedItem.Tagihan);
                                        if ((window as any).Capacitor) {
                                            window.open(imgUrl, '_system');
                                        } else {
                                            window.open(imgUrl, '_blank');
                                        }
                                    }}
                                    style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', padding: '10px', background: 'white', color: '#0d6efd', border: '1.5px solid #0d6efd', borderRadius: '10px', fontWeight: 'bold', cursor: 'pointer', marginBottom: '12px', fontSize: '12px' }}
                                >
                                    <Paperclip size={16} /> Lihat Bukti / Nota
                                </button>
                            )}

                            <div style={{ display: 'flex', gap: '8px' }}>
                                <button onClick={() => handleDelete(selectedItem.ID)} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', padding: '10px', background: '#fee2e2', color: '#b91c1c', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: 'bold', fontSize: '12px' }}>
                                    <Trash2 size={14} /> Hapus
                                </button>
                                <button onClick={openEdit} style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', padding: '10px', background: '#f1f5f9', color: '#64748b', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: 'bold', fontSize: '12px' }}>
                                    <Edit2 size={14} /> Edit
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* MODAL TAMBAH/EDIT */}
            {showModal && (
                <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.45)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1200, padding: '15px', backdropFilter: 'blur(4px)' }}>
                    <div style={{ background: 'white', padding: isMobile ? '20px' : '25px', borderRadius: '20px', width: '100%', maxWidth: '480px', maxHeight: '90vh', overflowY: 'auto', position: 'relative', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)' }}>
                        <button onClick={() => setShowModal(false)} style={{ position: 'absolute', right: '16px', top: '16px', background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer' }}><X size={20} /></button>

                        <h3 style={{ marginBottom: '20px', fontSize: '18px', fontWeight: '800', color: '#1e293b' }}>{isEditing ? '📝 Edit Transaksi' : '📦 Transaksi Bahan Baku'}</h3>

                        <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', background: '#f1f5f9', padding: '4px', borderRadius: '12px' }}>
                            <button type="button" onClick={() => setTab('IN')} style={{ flex: 1, padding: '10px', fontWeight: '800', background: tab === 'IN' ? '#10b981' : 'transparent', color: tab === 'IN' ? 'white' : '#64748b', border: 'none', borderRadius: '8px', cursor: 'pointer', fontSize: '12px' }}>TERIMA BAHAN</button>
                            <button type="button" onClick={() => setTab('OUT')} style={{ flex: 1, padding: '10px', fontWeight: '800', background: tab === 'OUT' ? '#ef4444' : 'transparent', color: tab === 'OUT' ? 'white' : '#64748b', border: 'none', borderRadius: '8px', cursor: 'pointer', fontSize: '12px' }}>BAYAR HUTANG</button>
                        </div>

                        <form onSubmit={handleSave}>
                            <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: '12px', marginBottom: '14px' }}>
                                <div>
                                    <label style={{ display: 'block', marginBottom: '6px', fontSize: '12px', fontWeight: '700', color: '#475569' }}>Tanggal</label>
                                    <input type="date" required value={tanggal} onChange={e => setTanggal(e.target.value)} style={{ width: '100%', padding: '10px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '13px', boxSizing: 'border-box' }} />
                                </div>
                                <div>
                                    <label style={{ display: 'block', marginBottom: '6px', fontSize: '12px', fontWeight: '700', color: '#475569' }}>Catatan</label>
                                    <input type="text" value={notes} onChange={e => setNotes(e.target.value)} style={{ width: '100%', padding: '10px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '13px', boxSizing: 'border-box' }} placeholder="Contoh: Ambil 10 ball" />
                                </div>
                            </div>

                            <div style={{ marginBottom: '16px' }}>
                                <label style={{ display: 'block', marginBottom: '6px', fontSize: '12px', fontWeight: '700', color: '#475569' }}>Bukti Foto / Nota</label>
                                <div style={{ border: '2px dashed #cbd5e1', borderRadius: '10px', padding: '12px', textAlign: 'center', position: 'relative' }}>
                                    <input type="file" onChange={handleFileUpload} style={{ position: 'absolute', inset: 0, opacity: 0, cursor: 'pointer' }} accept="image/*,application/pdf" />
                                    <div style={{ color: uploading ? '#0d6efd' : tagihan ? '#10b981' : '#64748b', fontSize: '12px', fontWeight: '600' }}>
                                        {uploading ? 'Sedang mengunggah...' : tagihan ? '✅ Bukti Terunggah' : 'Klik/Sentuh untuk upload Bukti Foto'}
                                    </div>
                                </div>
                            </div>

                            {tab === 'IN' && (
                                <div style={{ borderTop: '1px solid #f1f5f9', paddingTop: '15px', marginTop: '15px' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                                        <h4 style={{ margin: 0, fontSize: '13px', fontWeight: '800', color: '#1e293b' }}>Daftar Bahan Masuk</h4>
                                        <button type="button" onClick={() => setItems([...items, { jenis_bahan: 'Super A', kuantitas: 0, unit: 'Kg', rate: 0 }])} style={{ background: '#eff6ff', color: '#0d6efd', border: 'none', padding: '4px 10px', borderRadius: '6px', cursor: 'pointer', fontSize: '11px', fontWeight: '700' }}>+ Baris</button>
                                    </div>

                                    {items.map((item, idx) => (
                                        <div key={idx} style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '2.2fr 1fr 1.8fr 30px', gap: '6px', marginBottom: '10px', alignItems: 'center', borderBottom: isMobile ? '1px solid #f1f5f9' : 'none', paddingBottom: isMobile ? '8px' : '0' }}>
                                            <select
                                                value={item.jenis_bahan}
                                                onChange={e => { const n = [...items]; n[idx].jenis_bahan = e.target.value; setItems(n); }}
                                                style={{ width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px', background: 'white' }}
                                            >
                                                <option value="">-- Pilih Jenis --</option>
                                                <option value="Cerah A">Cerah A</option>
                                                <option value="Cerah B">Cerah B</option>
                                                <option value="Super A">Super A</option>
                                                <option value="Super B">Super B</option>
                                                <option value="Lainnya">Lainnya...</option>
                                            </select>

                                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr 30px', gap: '6px', alignItems: 'center' }}>
                                                <input type="number" placeholder="Qty" value={item.kuantitas || ''} onChange={e => { const n = [...items]; n[idx].kuantitas = e.target.value === '' ? '' as any : Number(e.target.value); setItems(n); }} style={{ width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px' }} />
                                                <input type="number" placeholder="Harga" value={item.rate || ''} onChange={e => { const n = [...items]; n[idx].rate = e.target.value === '' ? '' as any : Number(e.target.value); setItems(n); }} style={{ width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px' }} />
                                                <button type="button" onClick={() => setItems(items.filter((_, i) => i !== idx))} style={{ color: '#ef4444', background: 'none', border: 'none', cursor: 'pointer', display: 'flex', justifyContent: 'center' }}><X size={16}/></button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}

                            {tab === 'IN' && (
                                <div style={{ borderTop: '1px solid #f1f5f9', paddingTop: '15px', marginTop: '15px' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                                        <h4 style={{ margin: 0, fontSize: '13px', fontWeight: '800', color: '#1e293b' }}>Daftar Biaya Jasa</h4>
                                        <button type="button" onClick={() => setJasaItems([...jasaItems, { jenis_bahan: '', kuantitas: 0, unit: 'Kg', rate: 0 }])} style={{ background: '#fffbeb', color: '#d97706', border: 'none', padding: '4px 10px', borderRadius: '6px', cursor: 'pointer', fontSize: '11px', fontWeight: '700' }}>+ Baris Jasa</button>
                                    </div>

                                    {jasaItems.map((item, idx) => (
                                        <div key={`jasa-${idx}`} style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '2.2fr 1fr 1.8fr 30px', gap: '6px', marginBottom: '10px', alignItems: 'center', borderBottom: isMobile ? '1px solid #f1f5f9' : 'none', paddingBottom: isMobile ? '8px' : '0' }}>
                                            <input type="text" placeholder="Nama Jasa (Titip Giling)" value={item.jenis_bahan} onChange={e => { const n = [...jasaItems]; n[idx].jenis_bahan = e.target.value; setJasaItems(n); }} style={{ width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px' }} />
                                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr 30px', gap: '6px', alignItems: 'center' }}>
                                                <input type="number" placeholder="Berat" value={item.kuantitas || ''} onChange={e => { const n = [...jasaItems]; n[idx].kuantitas = e.target.value === '' ? '' as any : Number(e.target.value); setJasaItems(n); }} style={{ width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px' }} />
                                                <input type="number" placeholder="Harga" value={item.rate || ''} onChange={e => { const n = [...jasaItems]; n[idx].rate = e.target.value === '' ? '' as any : Number(e.target.value); setJasaItems(n); }} style={{ width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '8px', outline: 'none', fontSize: '12px' }} />
                                                <button type="button" onClick={() => setJasaItems(jasaItems.filter((_, i) => i !== idx))} style={{ color: '#ef4444', background: 'none', border: 'none', cursor: 'pointer', display: 'flex', justifyContent: 'center' }}><X size={16}/></button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}

                            {tab === 'OUT' && (
                                <div style={{ borderTop: '1px solid #f1f5f9', paddingTop: '15px', marginTop: '15px' }}>
                                    <label style={{ display: 'block', marginBottom: '6px', fontSize: '12px', fontWeight: '700', color: '#475569' }}>Nominal Pembayaran (Rp)</label>
                                    <input type="number" required value={nominal || ''} onChange={e => setNominal(e.target.value === '' ? '' as any : Number(e.target.value))} style={{ width: '100%', padding: '10px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '15px', fontWeight: '800', color: '#ef4444' }} />
                                </div>
                            )}

                            <div style={{ display: 'flex', gap: '10px', marginTop: '25px', borderTop: '1px solid #f1f5f9', paddingTop: '15px' }}>
                                <button type="button" onClick={() => setShowModal(false)} style={{ flex: 1, padding: '12px', background: '#f1f5f9', color: '#64748b', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: '700', fontSize: '13px' }}>Batal</button>
                                <button type="submit" disabled={uploading} style={{ flex: 2, padding: '12px', background: 'linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%)', color: 'white', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: '700', fontSize: '13px', opacity: uploading ? 0.5 : 1 }}>{isEditing ? 'Update Transaksi' : 'Simpan Transaksi'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default BahanNono;