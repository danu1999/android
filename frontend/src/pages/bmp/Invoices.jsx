const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\Invoices.tsx"; function _optionalChain(ops) { let lastAccessLHS = undefined; let value = ops[0]; let i = 1; while (i < ops.length) { const op = ops[i]; const fn = ops[i + 1]; i += 2; if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) { return undefined; } if (op === 'access' || op === 'optionalAccess') { lastAccessLHS = value; value = fn(value); } else if (op === 'call' || op === 'optionalCall') { value = fn((...args) => value.call(lastAccessLHS, ...args)); lastAccessLHS = undefined; } } return value; }/* eslint-disable @typescript-eslint/no-explicit-any, react-hooks/set-state-in-effect */
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { API_URL } from '../../services/apiBmp';
import { PlusCircle, FileText, DollarSign, Image as ImageIcon, AlertCircle, Edit, Trash2, Search, Calendar, X, Download } from 'lucide-react';
import html2canvas from 'html2canvas';
import { InvoiceImageTemplate } from '../../components/InvoiceImageTemplate';
import { Share } from '@capacitor/share';
import { Filesystem, Directory } from '@capacitor/filesystem';
























const Invoices = () => {
    const [invoices, setInvoices] = useState([]);
    const [clients, setClients] = useState([]);
    const [loading, setLoading] = useState(true);
    const [settings, setSettings] = useState(null);
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

    // Pagination state
    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [totalCount, setTotalCount] = useState(0);
    const LIMIT = 20;

    const [filterStatus, setFilterStatus] = useState('ALL');
    const [filterClient, setFilterClient] = useState('ALL');
    const [searchTerm, setSearchTerm] = useState('');
    const [clientSummary, setClientSummary] = useState(null);
    const [fullInvoiceData, setFullInvoiceData] = useState(null);
    const [previewImage, setPreviewImage] = useState(null);
    const [previewInvoiceNumber, setPreviewInvoiceNumber] = useState('');
    const [previewClientPhone, setPreviewClientPhone] = useState('');

    const navigate = useNavigate();

    const [showMassal, setShowMassal] = useState(false);
    const [massalData, setMassalData] = useState({ client_id: 0, nominal: 0, metode: 'TRANSFER' });

    const [showSingle, setShowSingle] = useState(false);
    const [singleData, setSingleData] = useState({ id: 0, nominal: 0, metode: 'TRANSFER', tanggal: new Date().toISOString().split('T')[0] });

    // === Payment Modal (cicilan) ===
    const [showPaymentModal, setShowPaymentModal] = useState(false);
    const [activeInvoice, setActiveInvoice] = useState(null);
    const [loadingPaymentDetail, setLoadingPaymentDetail] = useState(false);
    const [payForm, setPayForm] = useState({ nominal: 0, tanggal: new Date().toISOString().split('T')[0], metode: 'TRANSFER' });
    const [editPayId, setEditPayId] = useState(null);
    const [editPayForm, setEditPayForm] = useState({ nominal: 0, tanggal: new Date().toISOString().split('T')[0], metode: 'TRANSFER' });

    const [masterProducts, setMasterProducts] = useState([]);
    const [showEdit, setShowEdit] = useState(false);
    const [editData, setEditData] = useState({ id: 0, products: [] });

    useEffect(() => {
        const handleResize = () => setIsMobile(window.innerWidth <= 768);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    const fetchData = async (page = 1, status = filterStatus, client = filterClient, search = searchTerm) => {
        setLoading(true);
        try {
            const statusParam = status !== 'ALL' ? status : '';
            const clientParam = client !== 'ALL' ? client : '';
            const searchParam = encodeURIComponent(search);
            const invRes = await api.get(`/invoices?page=${page}&limit=${LIMIT}&status=${statusParam}&client_id=${clientParam}&search=${searchParam}`);
            setInvoices(_optionalChain([invRes, 'access', _2 => _2.data, 'optionalAccess', _3 => _3.data]) || []);
            setCurrentPage(_optionalChain([invRes, 'access', _4 => _4.data, 'optionalAccess', _5 => _5.current_page]) || 1);
            setTotalPages(_optionalChain([invRes, 'access', _6 => _6.data, 'optionalAccess', _7 => _7.total_pages]) || 1);
            setTotalCount(_optionalChain([invRes, 'access', _8 => _8.data, 'optionalAccess', _9 => _9.total_count]) || 0);

            const cliRes = await api.get('/clients');
            setClients(_optionalChain([cliRes, 'access', _10 => _10.data, 'optionalAccess', _11 => _11.data]) || []);

            const setRes = await api.get('/settings');
            setSettings(_optionalChain([setRes, 'access', _12 => _12.data, 'optionalAccess', _13 => _13.data]) || null);

            const mpRes = await api.get('/products');
            setMasterProducts(_optionalChain([mpRes, 'access', _14 => _14.data, 'optionalAccess', _15 => _15.data]) || []);
        } catch (err) {
            console.error("Gagal fetch data utama:", err);
            setInvoices([]);
            setClients([]);
            setMasterProducts([]);
        }
        setLoading(false);
    };

    useEffect(() => {
        const timer = setTimeout(() => {
            setCurrentPage(1);
            fetchData(1, filterStatus, filterClient, searchTerm);
        }, 300);
        
        return () => clearTimeout(timer);
    }, [filterStatus, filterClient, searchTerm]);

    useEffect(() => {
        if (filterClient !== 'ALL') {
            api.get(`/clients/${filterClient}/summary`)
                .then(res => {
                    setClientSummary(_optionalChain([res, 'access', _16 => _16.data, 'optionalAccess', _17 => _17.data]) || null);
                })
                .catch((err) => {
                    console.error("Gagal fetch summary client:", err);
                    setClientSummary(null);
                });
        } else {
            setClientSummary(null);
        }
    }, [filterClient]);

    const handleMassal = async (e) => {
        e.preventDefault();
        if (massalData.client_id === 0) return alert("Pilih pelanggan!");
        try {
            await api.post('/invoices/pay-massal', { ...massalData, nominal: Number(massalData.nominal) });
            alert("Pembayaran borongan berhasil!");
            setShowMassal(false);
            fetchData(currentPage, filterStatus, filterClient, searchTerm);
            if (filterClient !== 'ALL') {
                api.get(`/clients/${filterClient}/summary`).then(res => setClientSummary(_optionalChain([res, 'access', _18 => _18.data, 'optionalAccess', _19 => _19.data]) || null));
            }
        } catch (e2) {
            alert("Gagal melakukan pembayaran borongan");
        }
    };

    const handleSingle = async (e) => {
        e.preventDefault();
        try {
            await api.post(`/invoices/${singleData.id}/pay`, {
                nominal: Number(singleData.nominal),
                metode: singleData.metode,
                tanggal: singleData.tanggal + "T00:00:00Z"
            });
            alert("Pembayaran berhasil!");
            setShowSingle(false);
            fetchData(currentPage, filterStatus, filterClient, searchTerm);
        } catch (e3) {
            alert("Gagal melakukan pembayaran");
        }
    };

    // Buka modal cicilan: fetch detail faktur termasuk riwayat pembayaran
    const openPaymentModal = async (id) => {
        setLoadingPaymentDetail(true);
        setActiveInvoice(null);
        setShowPaymentModal(true);
        try {
            const res = await api.get(`/invoices/${id}`);
            const inv = res.data.data;
            const payments = res.data.payments || [];
            const products = res.data.products || [];
            const total = products.reduce((sum, p) => sum + (p.Quantity || 0) * (p.JumlahLusin || 1) * (p.Price || 0), 0);
            const paid = payments.reduce((sum, p) => sum + (p.PaymentAmount || 0), 0);
            const invoiceData = { ...inv, Total: total, PaidAmount: paid, payments };
            setActiveInvoice(invoiceData);
            setPayForm(f => ({ ...f, nominal: Math.max(0, total - paid), tanggal: new Date().toISOString().split('T')[0] }));
        } catch (err) {
            alert('Gagal memuat detail faktur');
            setShowPaymentModal(false);
        }
        setLoadingPaymentDetail(false);
    };

    // Submit cicilan baru
    const handlePaySubmit = async (e) => {
        e.preventDefault();
        if (!activeInvoice || payForm.nominal <= 0) return alert('Nominal tidak valid');
        try {
            await api.post(`/invoices/${activeInvoice.ID}/pay`, {
                nominal: Number(payForm.nominal),
                metode: payForm.metode,
                tanggal: payForm.tanggal + 'T00:00:00Z'
            });
            // Refresh data modal
            const res = await api.get(`/invoices/${activeInvoice.ID}`);
            const inv = res.data.data;
            const payments = res.data.payments || [];
            const products = res.data.products || [];
            const total = products.reduce((sum, p) => sum + (p.Quantity || 0) * (p.JumlahLusin || 1) * (p.Price || 0), 0);
            const paid = payments.reduce((sum, p) => sum + (p.PaymentAmount || 0), 0);
            setActiveInvoice({ ...inv, Total: total, PaidAmount: paid, payments });
            setPayForm(f => ({ ...f, nominal: Math.max(0, total - paid) }));
            // Refresh daftar faktur
            fetchData(currentPage, filterStatus, filterClient, searchTerm);
        } catch (err) {
            alert('Gagal menyimpan pembayaran');
        }
    };

    // Edit cicilan: PUT /api/invoices/payments/:paymentId — cascade ke CashFlow + update status faktur
    const handleEditPay = async (paymentId) => {
        if (editPayForm.nominal <= 0) return alert('Nominal tidak valid');
        try {
            await api.put(`/invoices/payments/${paymentId}`, {
                nominal: Number(editPayForm.nominal),
                tanggal: editPayForm.tanggal + 'T00:00:00Z',
                metode: editPayForm.metode
            });
            setEditPayId(null);
            // Refresh modal
            const res = await api.get(`/invoices/${activeInvoice.ID}`);
            const inv = res.data.data;
            const payments = res.data.payments || [];
            const products = res.data.products || [];
            const total = products.reduce((sum, p) => sum + (p.Quantity || 0) * (p.JumlahLusin || 1) * (p.Price || 0), 0);
            const paid = payments.reduce((sum, p) => sum + (p.PaymentAmount || 0), 0);
            setActiveInvoice({ ...inv, Total: total, PaidAmount: paid, payments });
            setPayForm(f => ({ ...f, nominal: Math.max(0, total - paid) }));
            fetchData(currentPage, filterStatus, filterClient, searchTerm);
        } catch (err) {
            alert('Gagal mengubah pembayaran');
        }
    };

    // Hapus cicilan: DELETE /api/invoices/payments/:paymentId — cascade ke CashFlow + update status faktur
    const handleDeletePay = async (paymentId, urutan) => {
        if (!window.confirm(`Yakin ingin menghapus Bayar ke-${urutan}? Data kas keuangan terkait juga akan terhapus.`)) return;
        try {
            await api.delete(`/invoices/payments/${paymentId}`);
            // Refresh modal
            const res = await api.get(`/invoices/${activeInvoice.ID}`);
            const inv = res.data.data;
            const payments = res.data.payments || [];
            const products = res.data.products || [];
            const total = products.reduce((sum, p) => sum + (p.Quantity || 0) * (p.JumlahLusin || 1) * (p.Price || 0), 0);
            const paid = payments.reduce((sum, p) => sum + (p.PaymentAmount || 0), 0);
            setActiveInvoice({ ...inv, Total: total, PaidAmount: paid, payments });
            setPayForm(f => ({ ...f, nominal: Math.max(0, total - paid) }));
            fetchData(currentPage, filterStatus, filterClient, searchTerm);
        } catch (err) {
            alert('Gagal menghapus pembayaran');
        }
    };

    const handleDelete = async (id) => {
        if (window.confirm("Yakin ingin menghapus faktur ini?")) {
            try {
                await api.delete(`/invoices/${id}`);
                fetchData(currentPage, filterStatus, filterClient, searchTerm);
            } catch (e4) {
                alert("Gagal menghapus faktur");
            }
        }
    };

    const openEditModal = async (id) => {
        try {
            const res = await api.get(`/invoices/${id}`);
            const invProducts = (_optionalChain([res, 'access', _20 => _20.data, 'optionalAccess', _21 => _21.products]) || []).map((p) => ({
                master_item_id: p.MasterItemID,
                quantity: p.Quantity,
                jumlah_lusin: p.JumlahLusin,
                custom_price: p.Price,
                is_khusus: p.IsKhusus || false,
                harga_beli: p.HargaBeli || 0
            }));
            setEditData({ id, products: invProducts });
            setShowEdit(true);
        } catch (e5) {
            alert("Gagal memuat data produk faktur");
        }
    };

    const handleEdit = async (e) => {
        e.preventDefault();
        if (editData.products.length === 0) return alert("Minimal 1 produk!");

        const computedProducts = editData.products.map(p => {
            return {
                ...p,
                harga_beli: p.is_khusus ? Number(p.harga_beli) : 0
            };
        });

        try {
            await api.put(`/invoices/${editData.id}/products`, { products: computedProducts });
            alert("Produk faktur berhasil diupdate!");
            setShowEdit(false);
            fetchData(currentPage, filterStatus, filterClient, searchTerm);
        } catch (e6) {
            alert("Gagal update produk faktur");
        }
    };

    const downloadJPG = async (id, number) => {
        try {
            const res = await api.get(`/invoices/${id}`);
            if (!_optionalChain([res, 'access', _22 => _22.data, 'optionalAccess', _23 => _23.data])) {
                alert("Data faktur tidak ditemukan");
                return;
            }
            const currentInv = {
                ...res.data.data,
                Products: _optionalChain([res, 'access', _24 => _24.data, 'optionalAccess', _25 => _25.products]) || [],
                Payments: _optionalChain([res, 'access', _26 => _26.data, 'optionalAccess', _27 => _27.payments]) || []
            };
            setFullInvoiceData(currentInv);
            setPreviewInvoiceNumber(number);
            setPreviewClientPhone(_optionalChain([res, 'access', _28 => _28.data, 'optionalAccess', _29 => _29.data, 'optionalAccess', _30 => _30.Client, 'optionalAccess', _31 => _31.PhoneNumber]) || '');

            setTimeout(() => {
                const element = document.getElementById(`faktur-canvas-${id}`);
                if (element) {
                    element.style.display = 'block';
                    element.style.top = '0';
                    element.style.left = '0';

                    html2canvas(element, {
                        scale: 2,
                        useCORS: true,
                        backgroundColor: "#ffffff",
                        logging: false
                    }).then(canvas => {
                        element.style.display = 'none';
                        element.style.top = '-9999px';
                        element.style.left = '-9999px';

                        const dataUrl = canvas.toDataURL('image/jpeg', 0.9);
                        // Always show preview modal (works on both web and Capacitor)
                        setPreviewImage(dataUrl);
                    });
                }
            }, 4000);
        } catch (e7) {
            alert("Gagal memuat data detail untuk JPG");
        }
    };

    const handleSaveImage = async () => {
        if (!previewImage) return;
        const filename = `Faktur-${previewInvoiceNumber || 'BMP'}.jpg`;
        try {
            if ((window ).Capacitor) {
                // Native: Save to Documents folder via Filesystem plugin
                const base64Data = previewImage.split(',')[1];
                await Filesystem.writeFile({
                    path: filename,
                    data: base64Data,
                    directory: Directory.Documents,
                });
                alert(`Gambar berhasil disimpan ke folder Dokumen: ${filename}`);
            } else {
                // Web browser: trigger download
                const link = document.createElement('a');
                link.download = filename;
                link.href = previewImage;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
            }
        } catch (err) {
            console.error('Save image error:', err);
            alert('Gagal menyimpan gambar. Coba long-press pada gambar untuk menyimpan manual.');
        }
    };

    const handleShareWhatsApp = async () => {
        if (!previewImage) return;
        const filename = `Faktur-${previewInvoiceNumber || 'BMP'}.jpg`;
        const invoiceText = `Faktur BMP No. ${previewInvoiceNumber}`;
        try {
            if ((window ).Capacitor) {
                // Native Capacitor: Save to cache then share natively (opens WA share sheet)
                const base64Data = previewImage.split(',')[1];
                const savedFile = await Filesystem.writeFile({
                    path: filename,
                    data: base64Data,
                    directory: Directory.Cache,
                });
                await Share.share({
                    title: invoiceText,
                    text: invoiceText,
                    url: savedFile.uri,
                    dialogTitle: 'Kirim Faktur via WhatsApp',
                });
            } else if (navigator.share && navigator.canShare) {
                // Mobile browser with Web Share API + file support
                const response = await fetch(previewImage);
                const blob = await response.blob();
                const file = new File([blob], filename, { type: 'image/jpeg' });
                if (navigator.canShare({ files: [file] })) {
                    await navigator.share({ files: [file], title: invoiceText, text: invoiceText });
                } else {
                    // Fallback: open WhatsApp Web link
                    openWhatsAppLink();
                }
            } else {
                // Desktop fallback: open WhatsApp Web with message
                openWhatsAppLink();
            }
        } catch (err) {
            // User cancelled is not an error
            if (_optionalChain([err, 'optionalAccess', _32 => _32.name]) !== 'AbortError') {
                console.error('Share error:', err);
                openWhatsAppLink();
            }
        }
    };

    const openWhatsAppLink = () => {
        const text = encodeURIComponent(`Berikut adalah faktur BMP No. ${previewInvoiceNumber}. Mohon konfirmasi pembayarannya. Terima kasih.`);
        let phone = (previewClientPhone || '').replace(/[^0-9]/g, '');
        if (phone.startsWith('0')) phone = '62' + phone.slice(1);
        const waUrl = phone
            ? `https://api.whatsapp.com/send?phone=${phone}&text=${text}`
            : `https://api.whatsapp.com/send?text=${text}`;
        window.open(waUrl, '_blank');
    };

    const downloadPDF = (id, type) => {
        const token = localStorage.getItem('token');
        const apiUrl = API_URL;
        const absoluteApiUrl = apiUrl.startsWith('http') ? apiUrl : `${window.location.origin}${apiUrl}`;
        const pdfUrl = `${absoluteApiUrl}/invoices/${id}/${type}?token=${token}`;

        if ((window ).Capacitor) {
            window.open(pdfUrl, '_system');
        } else {
            fetch(`${apiUrl}/invoices/${id}/${type}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            })
                .then(res => res.blob())
                .then(blob => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = `${type}-${id}.pdf`;
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);
                });
        }
    };

    const filteredInvoices = invoices || [];

    // Komponen navigasi pagination
    const PaginationNav = () => (
        React.createElement('div', { style: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginTop: '20px',
            padding: '12px 16px',
            background: 'white',
            borderRadius: '14px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
            border: '1px solid #f1f5f9',
            flexWrap: 'wrap',
            gap: '10px'
        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 362}}
            , React.createElement('div', { style: { fontSize: '13px', color: '#64748b', fontWeight: '600' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 375}}, "Menampilkan "
                 , React.createElement('strong', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 376}}, invoices.length), " dari "  , React.createElement('strong', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 376}}, totalCount), " faktur"
            )
            , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 378}}
                , React.createElement('button', {
                    onClick: () => { setCurrentPage(p => Math.max(1, p - 1)); fetchData(Math.max(1, currentPage - 1)); },
                    disabled: currentPage <= 1,
                    style: {
                        padding: '8px 18px', borderRadius: '10px', border: '1px solid #e2e8f0',
                        background: currentPage <= 1 ? '#f1f5f9' : 'white',
                        color: currentPage <= 1 ? '#94a3b8' : '#1e293b',
                        fontWeight: '700', cursor: currentPage <= 1 ? 'not-allowed' : 'pointer',
                        fontSize: '13px', transition: 'all 0.15s'
                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 379}}
, "← Sebelumnya"

                )
                , React.createElement('div', { style: {
                    display: 'flex', gap: '5px', alignItems: 'center'
                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 392}}
                    , Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
                        let pageNum;
                        if (totalPages <= 7) {
                            pageNum = i + 1;
                        } else if (currentPage <= 4) {
                            pageNum = i + 1;
                        } else if (currentPage >= totalPages - 3) {
                            pageNum = totalPages - 6 + i;
                        } else {
                            pageNum = currentPage - 3 + i;
                        }
                        return (
                            React.createElement('button', {
                                key: pageNum,
                                onClick: () => { setCurrentPage(pageNum); fetchData(pageNum); },
                                style: {
                                    width: '36px', height: '36px', borderRadius: '8px', border: 'none',
                                    background: currentPage === pageNum ? 'linear-gradient(135deg, #dc3545, #b91c1c)' : '#f8fafc',
                                    color: currentPage === pageNum ? 'white' : '#64748b',
                                    fontWeight: '700', cursor: 'pointer', fontSize: '13px',
                                    boxShadow: currentPage === pageNum ? '0 2px 8px rgba(220,53,69,0.3)' : 'none',
                                    transition: 'all 0.15s'
                                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 407}}

                                , pageNum
                            )
                        );
                    })
                )
                , React.createElement('button', {
                    onClick: () => { setCurrentPage(p => Math.min(totalPages, p + 1)); fetchData(Math.min(totalPages, currentPage + 1)); },
                    disabled: currentPage >= totalPages,
                    style: {
                        padding: '8px 18px', borderRadius: '10px', border: '1px solid #e2e8f0',
                        background: currentPage >= totalPages ? '#f1f5f9' : 'white',
                        color: currentPage >= totalPages ? '#94a3b8' : '#1e293b',
                        fontWeight: '700', cursor: currentPage >= totalPages ? 'not-allowed' : 'pointer',
                        fontSize: '13px', transition: 'all 0.15s'
                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 424}}
, "Selanjutnya →"

                )
            )
        )
    );

    if (loading) return (
        React.createElement('div', { style: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 442}}
            , React.createElement('div', { style: { width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #dc3545', borderRadius: '50%', animation: 'spin 1s linear infinite' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 443}})
            , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 444}}, `@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`)
            , React.createElement('div', { style: { color: '#6c757d' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 445}}, "Memuat riwayat faktur..."  )
        )
    );

    return (
        React.createElement('div', { className: "legacy-page", style: { padding: isMobile ? '12px' : '30px', background: '#f8f9fa', minHeight: '100%', maxWidth: '100vw', boxSizing: 'border-box', overflowX: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 450}}
            , React.createElement(InvoiceImageTemplate, { inv: fullInvoiceData, settings: settings, __self: this, __source: {fileName: _jsxFileName, lineNumber: 451}} )

            , previewImage && (
                React.createElement('div', { style: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.85)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 2000, padding: '15px', flexDirection: 'column', gap: '15px', backdropFilter: 'blur(6px)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 454}}
                    , React.createElement('div', { style: { background: 'white', padding: '20px', borderRadius: '24px', width: '100%', maxWidth: '460px', boxSizing: 'border-box', position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: '0 25px 60px rgba(0,0,0,0.3)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 455}}
                        , React.createElement('button', { onClick: () => setPreviewImage(null), style: { position: 'absolute', right: '15px', top: '15px', border: 'none', background: '#f1f5f9', color: '#64748b', cursor: 'pointer', borderRadius: '50%', width: '32px', height: '32px', display: 'flex', alignItems: 'center', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 456}}, React.createElement(X, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 456}}))
                        , React.createElement('h4', { style: { margin: '0 0 4px 0', fontWeight: '800', color: '#1e293b', fontSize: '17px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 457}}, "Gambar Faktur" )
                        , React.createElement('p', { style: { margin: '0 0 14px 0', fontSize: '12px', color: '#94a3b8', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 458}}, "Faktur No. "  , React.createElement('strong', { style: {color:'#0d6efd'}, __self: this, __source: {fileName: _jsxFileName, lineNumber: 458}}, previewInvoiceNumber))
                        , React.createElement('div', { style: { width: '100%', borderRadius: '14px', overflow: 'hidden', border: '1px solid #e2e8f0', boxShadow: '0 4px 16px rgba(0,0,0,0.08)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 459}}
                            , React.createElement('img', { src: previewImage, alt: "Faktur", style: { width: '100%', maxHeight: '55vh', objectFit: 'contain', display: 'block' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 460}} )
                        )
                        , React.createElement('div', { style: { display: 'flex', flexDirection: 'column', width: '100%', gap: '10px', marginTop: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 462}}
                            /* Download / Save Button */
                            , React.createElement('button', {
                                onClick: handleSaveImage,
                                style: {
                                    width: '100%',
                                    padding: '13px',
                                    background: 'linear-gradient(135deg, #4f46e5, #7c3aed)',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '12px',
                                    fontWeight: 'bold',
                                    cursor: 'pointer',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    gap: '8px',
                                    fontSize: '14px',
                                    boxShadow: '0 4px 12px rgba(79,70,229,0.3)'
                                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 464}}
, "💾 Simpan / Unduh Gambar"

                            )

                            /* WhatsApp Share Button — always shown */
                            , React.createElement('button', {
                                onClick: handleShareWhatsApp,
                                style: {
                                    width: '100%',
                                    padding: '13px',
                                    background: 'linear-gradient(135deg, #25D366, #128C7E)',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '12px',
                                    fontWeight: 'bold',
                                    cursor: 'pointer',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    gap: '8px',
                                    fontSize: '14px',
                                    boxShadow: '0 4px 12px rgba(37,211,102,0.3)'
                                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 487}}

                                , React.createElement('svg', { width: "18", height: "18", viewBox: "0 0 24 24"   , fill: "currentColor", __self: this, __source: {fileName: _jsxFileName, lineNumber: 506}}, React.createElement('path', { d: "M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"                                                                  , __self: this, __source: {fileName: _jsxFileName, lineNumber: 506}})), "Kirim ke WhatsApp"

                            )
                        )
                        , React.createElement('p', { style: { margin: '12px 0 0 0', fontSize: '11px', color: '#cbd5e1', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 510}}, "Atau sentuh dan tahan (long-press) gambar di atas untuk menyimpan langsung"          )
                    )
                )
            )

            /* Header Section */
            , React.createElement('div', { style: { display: 'flex', flexDirection: isMobile ? 'column' : 'row', justifyContent: 'space-between', alignItems: isMobile ? 'stretch' : 'center', marginBottom: '25px', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 516}}
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 517}}
                    , React.createElement('h2', { style: { fontSize: isMobile ? '22px' : '28px', fontWeight: '800', margin: 0, color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 518}}, "Riwayat Faktur" )
                    , React.createElement('p', { style: { margin: '5px 0 0 0', color: '#64748b', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 519}}, "Manajemen penagihan dan pembayaran pelanggan"    )
                )
                , React.createElement('button', {
                    onClick: () => navigate('/invoices/create'),
                    style: {
                        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
                        background: 'linear-gradient(135deg, #dc3545 0%, #b91c1c 100%)', color: 'white',
                        border: 'none', padding: '12px 24px', borderRadius: '12px', cursor: 'pointer',
                        fontWeight: 'bold', boxShadow: '0 4px 10px rgba(220, 53, 69, 0.2)', transition: 'all 0.2s'
                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 521}}

                    , React.createElement(PlusCircle, { size: 20, __self: this, __source: {fileName: _jsxFileName, lineNumber: 530}} ), " Buat Faktur Baru"
                )
            )

            /* Summary Section (Only when client filtered) */
            , filterClient !== 'ALL' && clientSummary && (
                React.createElement('div', { style: { display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: '15px', marginBottom: '25px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 536}}
                    , React.createElement('div', { style: { background: 'white', padding: '20px', borderRadius: '18px', borderLeft: '6px solid #0d6efd', boxShadow: '0 4px 12px rgba(0,0,0,0.02)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 537}}
                        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 538}}
                            , React.createElement('div', { style: { color: '#64748b', fontSize: '11px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 539}}, "Total Tunggakan" )
                            , React.createElement('div', { style: { fontSize: isMobile ? '20px' : '24px', fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 540}}, "Rp " , clientSummary.total_tunggakan.toLocaleString('id-ID'))
                            , React.createElement('div', { style: { fontSize: '11px', color: '#64748b', marginTop: '5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 541}}, clientSummary.unpaid_count, " Faktur belum lunas"   )
                        )
                        , React.createElement('button', { onClick: () => { setMassalData({ ...massalData, client_id: Number(filterClient) }); setShowMassal(true); }, style: { background: '#0d6efd', color: 'white', border: 'none', padding: '10px 15px', borderRadius: '10px', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 543}}, "Bayar Cepat"

                        )
                    )
                    , React.createElement('div', { style: { background: 'white', padding: '20px', borderRadius: '18px', borderLeft: '6px solid #198754', boxShadow: '0 4px 12px rgba(0,0,0,0.02)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 547}}
                        , React.createElement('div', { style: { color: '#64748b', fontSize: '11px', fontWeight: '700', textTransform: 'uppercase', marginBottom: '5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 548}}, "Saldo Titipan" )
                        , React.createElement('div', { style: { fontSize: isMobile ? '20px' : '24px', fontWeight: '800', color: '#198754' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 549}}, "Rp " , clientSummary.saldo_borongan.toLocaleString('id-ID'))
                        , React.createElement('div', { style: { fontSize: '11px', color: '#64748b', marginTop: '5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 550}}, "Digunakan otomatis saat bayar faktur"    )
                    )
                )
            )

            /* Filters & Search */
            , React.createElement('div', { style: { background: 'white', padding: '15px', borderRadius: '18px', boxShadow: '0 2px 8px rgba(0,0,0,0.02)', marginBottom: '25px', display: 'flex', flexDirection: 'column', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 556}}
                , React.createElement('div', { style: { display: 'flex', gap: '10px', flexWrap: 'wrap' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 557}}
                    , React.createElement('div', { style: { flex: 1, minWidth: isMobile ? '100%' : '200px', position: 'relative' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 558}}
                        , React.createElement(Search, { size: 18, style: { position: 'absolute', left: '12px', top: '12px', color: '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 559}} )
                        , React.createElement('input', {
                            type: "text",
                            placeholder: "Cari No. Faktur atau Pelanggan..."    ,
                            value: searchTerm,
                            onChange: e => setSearchTerm(e.target.value),
                            style: { width: '100%', padding: '12px 12px 12px 40px', borderRadius: '10px', border: '1px solid #e2e8f0', outline: 'none', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 560}}
                        )
                    )
                    , React.createElement('div', { style: { flex: 1, minWidth: isMobile ? '100%' : '180px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 568}}
                        , React.createElement('select', { value: filterClient, onChange: e => setFilterClient(e.target.value), style: { width: '100%', padding: '12px', borderRadius: '10px', border: '1px solid #e2e8f0', outline: 'none', fontSize: '14px', background: 'white' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 569}}
                            , React.createElement('option', { value: "ALL", __self: this, __source: {fileName: _jsxFileName, lineNumber: 570}}, "Semua Pelanggan" )
                            , (clients || []).map(c => React.createElement('option', { key: c.ID, value: c.ID, __self: this, __source: {fileName: _jsxFileName, lineNumber: 571}}, c.ClientName))
                        )
                    )
                    , React.createElement('div', { style: { flex: 1, minWidth: isMobile ? '100%' : '150px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 574}}
                        , React.createElement('select', { value: filterStatus, onChange: e => setFilterStatus(e.target.value), style: { width: '100%', padding: '12px', borderRadius: '10px', border: '1px solid #e2e8f0', outline: 'none', fontSize: '14px', background: 'white' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 575}}
                            , React.createElement('option', { value: "ALL", __self: this, __source: {fileName: _jsxFileName, lineNumber: 576}}, "Semua Status" )
                            , React.createElement('option', { value: "PAID", __self: this, __source: {fileName: _jsxFileName, lineNumber: 577}}, "LUNAS")
                            , React.createElement('option', { value: "UNPAID", __self: this, __source: {fileName: _jsxFileName, lineNumber: 578}}, "BELUM BAYAR" )
                            , React.createElement('option', { value: "PARTIAL", __self: this, __source: {fileName: _jsxFileName, lineNumber: 579}}, "SEBAGIAN")
                            , React.createElement('option', { value: "OVERDUE", __self: this, __source: {fileName: _jsxFileName, lineNumber: 580}}, "JATUH TEMPO" )
                        )
                    )
                )
            )

            , isMobile ? (
                /* Mobile List View */
                React.createElement('div', { style: { display: 'flex', flexDirection: 'column', gap: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 588}}
                    , filteredInvoices.map(inv => (
                        React.createElement('div', { key: inv.ID, style: { background: 'white', borderRadius: '18px', padding: '18px', boxShadow: '0 4px 6px rgba(0,0,0,0.03)', border: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 590}}
                            , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 591}}
                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 592}}
                                    , React.createElement('div', { style: { fontWeight: '800', color: '#0d6efd', fontSize: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 593}}, inv.Number)
                                    , React.createElement('div', { style: { fontWeight: '700', color: '#1e293b', fontSize: '14px', marginTop: '2px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 594}}, _optionalChain([inv, 'access', _33 => _33.Client, 'optionalAccess', _34 => _34.ClientName]))
                                )
                                , React.createElement('div', { style: { textAlign: 'right' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 596}}
                                    , React.createElement('div', { style: { fontWeight: '800', fontSize: '16px', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 597}}, "Rp " , inv.Total.toLocaleString('id-ID'))
                                    , inv.Status === 'PARTIAL' && inv.PaidAmount > 0 && (
                                        React.createElement('div', { style: { fontSize: '11px', marginTop: '3px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 599}}
                                            , React.createElement('span', { style: { color: '#198754' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 600}}, "✓ Rp "  , inv.PaidAmount.toLocaleString('id-ID'))
                                            , React.createElement('br', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 601}})
                                            , React.createElement('span', { style: { color: '#dc3545', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 602}}, "Sisa Rp "  , (inv.Total - inv.PaidAmount).toLocaleString('id-ID'))
                                        )
                                    )
                                    , React.createElement('span', { style: {
                                        fontSize: '9px', fontWeight: '900', padding: '2px 8px', borderRadius: '6px', textTransform: 'uppercase', display: 'inline-block', marginTop: '5px',
                                        background: inv.Status === 'PAID' ? '#dcfce7' : inv.Status === 'PARTIAL' ? '#fef9c3' : inv.Status === 'OVERDUE' ? '#fee2e2' : '#f1f5f9',
                                        color: inv.Status === 'PAID' ? '#15803d' : inv.Status === 'PARTIAL' ? '#854d0e' : inv.Status === 'OVERDUE' ? '#b91c1c' : '#64748b'
                                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 605}}
                        , inv.Status === 'OVERDUE' ? 'Jatuh Tempo' : inv.Status === 'PARTIAL' ? 'CICIL' : inv.Status
                    )
                                )
                            )

                            , React.createElement('div', { style: { display: 'flex', gap: '15px', marginBottom: '15px', color: '#64748b', fontSize: '11px', background: '#f8fafc', padding: '10px', borderRadius: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 615}}
                                , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 616}}, React.createElement(Calendar, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 616}}), " " , new Date(inv.DateCreated).toLocaleDateString('id-ID'))
                                , inv.DueDate && React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '5px', color: '#dc3545' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 617}}, React.createElement(AlertCircle, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 617}}), " Tempo: "  , new Date(inv.DueDate).toLocaleDateString('id-ID'))
                            )

                            , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 620}}
                                , inv.Status !== 'PAID' && (
                                    React.createElement('button', { onClick: () => openPaymentModal(inv.ID), style: { background: 'linear-gradient(135deg, #16a34a, #15803d)', color: 'white', border: 'none', padding: '10px', borderRadius: '10px', cursor: 'pointer', display: 'flex', justifyContent: 'center', boxShadow: '0 2px 8px rgba(22,163,74,0.3)' }, title: "Bayar Cicilan" }, React.createElement(DollarSign, { size: 16 }))
                                )
                                , React.createElement('button', { onClick: () => openEditModal(inv.ID), style: { background: '#f1f5f9', color: '#64748b', border: 'none', padding: '10px', borderRadius: '10px', cursor: 'pointer', display: 'flex', justifyContent: 'center' }, title: "Edit", __self: this, __source: {fileName: _jsxFileName, lineNumber: 624}}, React.createElement(Edit, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 624}}))
                                , React.createElement('button', { onClick: () => downloadJPG(inv.ID, inv.Number), style: { background: '#f1f5f9', color: '#64748b', border: 'none', padding: '10px', borderRadius: '10px', cursor: 'pointer', display: 'flex', justifyContent: 'center' }, title: "JPG", __self: this, __source: {fileName: _jsxFileName, lineNumber: 625}}, React.createElement(ImageIcon, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 625}}))
                                , React.createElement('button', { onClick: () => downloadPDF(inv.ID, 'pdf'), style: { background: '#f1f5f9', color: '#64748b', border: 'none', padding: '10px', borderRadius: '10px', cursor: 'pointer', display: 'flex', justifyContent: 'center' }, title: "PDF", __self: this, __source: {fileName: _jsxFileName, lineNumber: 626}}, React.createElement(Download, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 626}}))
                                , React.createElement('button', { onClick: () => downloadPDF(inv.ID, 'surat-jalan'), style: { background: '#f1f5f9', color: '#64748b', border: 'none', padding: '10px', borderRadius: '10px', cursor: 'pointer', display: 'flex', justifyContent: 'center' }, title: "Surat Jalan" , __self: this, __source: {fileName: _jsxFileName, lineNumber: 627}}, React.createElement(FileText, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 627}}))
                                , React.createElement('button', { onClick: () => handleDelete(inv.ID), style: { background: '#fee2e2', color: '#dc3545', border: 'none', padding: '10px', borderRadius: '10px', cursor: 'pointer', display: 'flex', justifyContent: 'center' }, title: "Hapus", __self: this, __source: {fileName: _jsxFileName, lineNumber: 628}}, React.createElement(Trash2, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 628}}))
                            )
                        )
                    ))
                )
            ) : (
                /* Desktop Table View */
                React.createElement('div', { style: { background: 'white', borderRadius: '20px', padding: '10px', boxShadow: '0 4px 12px rgba(0,0,0,0.04)', border: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 635}}
                    , React.createElement('table', { style: { width: '100%', borderCollapse: 'separate', borderSpacing: '0 8px', textAlign: 'left' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 636}}
                        , React.createElement('thead', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 637}}
                        , React.createElement('tr', { style: { color: '#64748b', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '1px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 638}}
                            , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 639}}, "No Faktur" )
                            , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 640}}, "Pelanggan")
                            , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 641}}, "Tanggal & Tempo"  )
                            , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 642}}, "Total Tagihan" )
                            , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 643}}, "Status")
                            , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 644}}, "Aksi")
                        )
                        )
                        , React.createElement('tbody', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 647}}
                        , filteredInvoices.map(inv => (
                            React.createElement('tr', { key: inv.ID, style: { background: '#ffffff' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 649}}
                                , React.createElement('td', { style: { padding: '15px 20px', fontWeight: '700', color: '#0d6efd', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 650}}, inv.Number)
                                , React.createElement('td', { style: { padding: '15px 20px', fontWeight: '600', color: '#334155', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 651}}, _optionalChain([inv, 'access', _35 => _35.Client, 'optionalAccess', _36 => _36.ClientName]) || '-')
                                , React.createElement('td', { style: { padding: '15px 20px', fontSize: '12px', color: '#64748b', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 652}}
                                    , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 653}}, React.createElement(Calendar, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 653}}), " " , new Date(inv.DateCreated).toLocaleDateString('id-ID'))
                                    , inv.DueDate && React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '5px', color: '#dc3545', marginTop: '3px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 654}}, React.createElement(AlertCircle, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 654}}), " " , new Date(inv.DueDate).toLocaleDateString('id-ID'))
                                )
                                , React.createElement('td', { style: { padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 656}}
                                    , React.createElement('div', { style: { fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 657}}, "Rp " , inv.Total.toLocaleString('id-ID'))
                                    , inv.Status === 'PARTIAL' && inv.PaidAmount > 0 && (
                                        React.createElement('div', { style: { marginTop: '4px', fontSize: '11px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 659}}
                                            , React.createElement('span', { style: { color: '#198754' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 660}}, "✓ Dibayar: Rp "   , inv.PaidAmount.toLocaleString('id-ID'))
                                            , React.createElement('br', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 661}} )
                                            , React.createElement('span', { style: { color: '#dc3545', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 662}}, "Sisa: Rp "  , (inv.Total - inv.PaidAmount).toLocaleString('id-ID'))
                                        )
                                    )
                                )
                                , React.createElement('td', { style: { padding: '15px 20px', borderBottom: '1px solid #f1f5f9', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 666}}
                    , React.createElement('span', { style: {
                        padding: '5px 12px', borderRadius: '8px', fontSize: '10px', fontWeight: '900',
                        background: inv.Status === 'PAID' ? '#dcfce7' : inv.Status === 'PARTIAL' ? '#fef9c3' : inv.Status === 'OVERDUE' ? '#fee2e2' : '#f1f5f9',
                        color: inv.Status === 'PAID' ? '#15803d' : inv.Status === 'PARTIAL' ? '#854d0e' : inv.Status === 'OVERDUE' ? '#b91c1c' : '#64748b'
                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 667}}
                        , inv.Status === 'PARTIAL' ? 'CICIL' : inv.Status
                    )
                                )
                                , React.createElement('td', { style: { padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 675}}
                                    , React.createElement('div', { style: { display: 'flex', gap: '6px', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 676}}
                                        , inv.Status !== 'PAID' && (
                                            React.createElement('button', { onClick: () => openPaymentModal(inv.ID), style: { background: 'linear-gradient(135deg, #16a34a, #15803d)', color: 'white', border: 'none', padding: '8px', borderRadius: '8px', cursor: 'pointer', boxShadow: '0 2px 6px rgba(22,163,74,0.3)' }, title: "Bayar Cicilan" }, React.createElement(DollarSign, { size: 14 }))
                                        )
                                        , React.createElement('button', { onClick: () => openEditModal(inv.ID), style: { background: '#f1f5f9', color: '#64748b', border: 'none', padding: '8px', borderRadius: '8px', cursor: 'pointer' }, title: "Edit", __self: this, __source: {fileName: _jsxFileName, lineNumber: 680}}, React.createElement(Edit, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 680}}))
                                        , React.createElement('button', { onClick: () => downloadJPG(inv.ID, inv.Number), style: { background: '#f1f5f9', color: '#64748b', border: 'none', padding: '8px', borderRadius: '8px', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 681}}, React.createElement(ImageIcon, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 681}}))
                                        , React.createElement('button', { onClick: () => downloadPDF(inv.ID, 'pdf'), style: { background: '#f1f5f9', color: '#64748b', border: 'none', padding: '8px', borderRadius: '8px', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 682}}, React.createElement(Download, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 682}}))
                                        , React.createElement('button', { onClick: () => downloadPDF(inv.ID, 'surat-jalan'), style: { background: '#f1f5f9', color: '#64748b', border: 'none', padding: '8px', borderRadius: '8px', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 683}}, React.createElement(FileText, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 683}}))
                                        , React.createElement('button', { onClick: () => handleDelete(inv.ID), style: { background: '#fee2e2', color: '#dc3545', border: 'none', padding: '8px', borderRadius: '8px', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 684}}, React.createElement(Trash2, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 684}}))


                                    )
                                )
                            )
                        ))
                        )
                    )
                )
            )

            , filteredInvoices.length === 0 && !loading && (
                React.createElement('div', { style: { textAlign: 'center', padding: '50px', background: 'white', borderRadius: '24px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 697}}
                    , React.createElement(FileText, { size: 48, style: { margin: '0 auto 15px', opacity: 0.2 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 698}} )
                    , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 699}}, "Belum ada faktur yang ditemukan."    )
                )
            )

            /* Pagination Navigation */
            , totalCount > 0 && React.createElement(PaginationNav, {__self: this, __source: {fileName: _jsxFileName, lineNumber: 704}} )

            /* MODALS (Simplified for brevity, but made responsive) */
            , showMassal && (
                React.createElement('div', { style: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1200, padding: '15px', backdropFilter: 'blur(4px)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 708}}
                    , React.createElement('div', { style: { background: 'white', padding: '30px', borderRadius: '24px', width: '100%', maxWidth: '400px', position: 'relative' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 709}}
                        , React.createElement('button', { onClick: () => setShowMassal(false), style: { position: 'absolute', right: '20px', top: '20px', border: 'none', background: 'none', color: '#94a3b8', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 710}}, React.createElement(X, { size: 24, __self: this, __source: {fileName: _jsxFileName, lineNumber: 710}}))
                        , React.createElement('h3', { style: { marginBottom: '25px', fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 711}}, "Pembayaran Cepat" )
                        , React.createElement('form', { onSubmit: handleMassal, __self: this, __source: {fileName: _jsxFileName, lineNumber: 712}}
                            , React.createElement('div', { style: { marginBottom: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 713}}
                                , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 714}}, "Klien")
                                , React.createElement('select', { disabled: true, value: massalData.client_id, style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', background: '#f8fafc' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 715}}
                                    , clients.map(c => React.createElement('option', { key: c.ID, value: c.ID, __self: this, __source: {fileName: _jsxFileName, lineNumber: 716}}, c.ClientName))
                                )
                            )
                            , React.createElement('div', { style: { marginBottom: '25px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 719}}
                                , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 720}}, "Nominal Uang (Rp)"  )
                                , React.createElement('input', { type: "number", required: true, value: massalData.nominal, onChange: e => setMassalData({...massalData, nominal: Number(e.target.value)}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '16px', fontWeight: '800' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 721}} )
                            )
                            , React.createElement('button', { type: "submit", style: { width: '100%', padding: '14px', background: '#0d6efd', color: 'white', border: 'none', borderRadius: '12px', fontWeight: 'bold', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 723}}, "Proses Pembayaran" )
                        )
                    )
                )
            )

            , showPaymentModal && (
                React.createElement('div', { style: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.78)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1200, padding: '15px', backdropFilter: 'blur(5px)' } }
                    , loadingPaymentDetail ? (
                        React.createElement('div', { style: { background: 'white', borderRadius: '24px', padding: '40px', textAlign: 'center', minWidth: '260px' } }
                            , React.createElement('div', { style: { width: '28px', height: '28px', border: '3px solid #f3f3f3', borderTop: '3px solid #198754', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 14px' } })
                            , React.createElement('div', { style: { color: '#64748b', fontSize: '14px', fontWeight: '600' } }, 'Memuat data faktur...')
                        )
                    ) : activeInvoice && (
                        React.createElement('div', { style: { background: 'white', padding: isMobile ? '20px' : '28px', borderRadius: '24px', width: '100%', maxWidth: '520px', maxHeight: '90vh', overflowY: 'auto', position: 'relative', boxShadow: '0 25px 60px rgba(0,0,0,0.25)' } }
                            , React.createElement('button', { onClick: () => setShowPaymentModal(false), style: { position: 'absolute', right: '16px', top: '16px', border: 'none', background: '#f1f5f9', color: '#64748b', cursor: 'pointer', borderRadius: '50%', width: '32px', height: '32px', display: 'flex', alignItems: 'center', justifyContent: 'center' } }
                                , React.createElement(X, { size: 18 })
                            )
                            , React.createElement('h3', { style: { margin: '0 0 2px 0', fontWeight: '800', color: '#1e293b', paddingRight: '40px', fontSize: '18px' } }, 'Pembayaran Faktur')
                            , React.createElement('div', { style: { fontSize: '13px', color: '#0d6efd', fontWeight: '700', marginBottom: '18px' } }, activeInvoice.Number)

                            , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '20px' } }
                                , React.createElement('div', { style: { background: '#f8fafc', borderRadius: '14px', padding: '14px', borderLeft: '4px solid #0d6efd' } }
                                    , React.createElement('div', { style: { fontSize: '10px', color: '#64748b', fontWeight: '700', textTransform: 'uppercase', marginBottom: '4px', letterSpacing: '0.5px' } }, 'Total Tagihan')
                                    , React.createElement('div', { style: { fontSize: '15px', fontWeight: '800', color: '#1e293b' } }, 'Rp ' + activeInvoice.Total.toLocaleString('id-ID'))
                                    , activeInvoice.PaidAmount > 0 && React.createElement('div', { style: { fontSize: '11px', color: '#198754', marginTop: '4px', fontWeight: '600' } }, '✓ Dibayar: Rp ' + activeInvoice.PaidAmount.toLocaleString('id-ID'))
                                )
                                , React.createElement('div', { style: { background: activeInvoice.Status === 'PAID' ? '#f0fdf4' : '#fff5f5', borderRadius: '14px', padding: '14px', borderLeft: '4px solid ' + (activeInvoice.Status === 'PAID' ? '#16a34a' : '#dc3545') } }
                                    , React.createElement('div', { style: { fontSize: '10px', color: '#64748b', fontWeight: '700', textTransform: 'uppercase', marginBottom: '4px', letterSpacing: '0.5px' } }, activeInvoice.Status === 'PAID' ? 'Status' : 'Sisa Tagihan')
                                    , React.createElement('div', { style: { fontSize: '15px', fontWeight: '800', color: activeInvoice.Status === 'PAID' ? '#16a34a' : '#dc3545' } }
                                        , activeInvoice.Status === 'PAID' ? '✅ LUNAS' : ('Rp ' + (activeInvoice.Total - activeInvoice.PaidAmount).toLocaleString('id-ID'))
                                    )
                                )
                            )

                            , React.createElement('div', { style: { marginBottom: '20px' } }
                                , React.createElement('div', { style: { fontSize: '11px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '10px' } }, 'Riwayat Pembayaran')
                                , activeInvoice.payments && activeInvoice.payments.length > 0 ? (
                                    React.createElement('div', { style: { display: 'flex', flexDirection: 'column', gap: '8px' } }
                                        , activeInvoice.payments.map((pay, idx) =>
                                            editPayId === pay.ID ? (
                                                React.createElement('div', { key: pay.ID || idx, style: { background: '#fffbeb', borderRadius: '12px', padding: '14px', border: '2px solid #fbbf24' } }
                                                    , React.createElement('div', { style: { fontSize: '11px', color: '#92400e', fontWeight: '700', marginBottom: '10px' } }, 'Edit Bayar ke-' + (idx + 1))
                                                    , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: '8px', marginBottom: '8px' } }
                                                        , React.createElement('div', {}
                                                            , React.createElement('label', { style: { fontSize: '11px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '4px' } }, 'Tanggal')
                                                            , React.createElement('input', { type: 'date', value: editPayForm.tanggal, onChange: e => setEditPayForm({ ...editPayForm, tanggal: e.target.value }), style: { width: '100%', padding: '8px', border: '1px solid #fbbf24', borderRadius: '8px', fontSize: '13px', boxSizing: 'border-box' } })
                                                        )
                                                        , React.createElement('div', {}
                                                            , React.createElement('label', { style: { fontSize: '11px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '4px' } }, 'Metode')
                                                            , React.createElement('select', { value: editPayForm.metode, onChange: e => setEditPayForm({ ...editPayForm, metode: e.target.value }), style: { width: '100%', padding: '8px', border: '1px solid #fbbf24', borderRadius: '8px', fontSize: '13px', background: 'white', boxSizing: 'border-box' } }
                                                                , React.createElement('option', { value: 'TRANSFER' }, 'Transfer Bank')
                                                                , React.createElement('option', { value: 'TUNAI' }, 'Tunai / Cash')
                                                                , React.createElement('option', { value: 'CEK' }, 'Cek / Giro')
                                                            )
                                                        )
                                                    )
                                                    , React.createElement('div', { style: { marginBottom: '10px' } }
                                                        , React.createElement('label', { style: { fontSize: '11px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '4px' } }, 'Nominal (Rp)')
                                                        , React.createElement('input', { type: 'number', min: 1, value: editPayForm.nominal === 0 ? '' : editPayForm.nominal, onChange: e => setEditPayForm({ ...editPayForm, nominal: e.target.value === '' ? 0 : Number(e.target.value) }), style: { width: '100%', padding: '8px', border: '1px solid #fbbf24', borderRadius: '8px', fontSize: '15px', fontWeight: '800', boxSizing: 'border-box' } })
                                                    )
                                                    , React.createElement('div', { style: { display: 'flex', gap: '8px' } }
                                                        , React.createElement('button', { onClick: () => handleEditPay(pay.ID), style: { flex: 1, padding: '9px', background: 'linear-gradient(135deg, #f59e0b, #d97706)', color: 'white', border: 'none', borderRadius: '8px', fontWeight: '700', cursor: 'pointer', fontSize: '13px' } }, '💾 Simpan Perubahan')
                                                        , React.createElement('button', { onClick: () => setEditPayId(null), style: { padding: '9px 14px', background: '#f1f5f9', color: '#64748b', border: 'none', borderRadius: '8px', fontWeight: '600', cursor: 'pointer', fontSize: '13px' } }, 'Batal')
                                                    )
                                                )
                                            ) : (
                                                React.createElement('div', { key: pay.ID || idx, style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#f8fafc', borderRadius: '12px', padding: '12px 15px', border: '1px solid #e2e8f0' } }
                                                    , React.createElement('div', {}
                                                        , React.createElement('div', { style: { fontSize: '11px', color: '#94a3b8', fontWeight: '700', marginBottom: '2px' } }, 'Bayar ke-' + (idx + 1))
                                                        , React.createElement('div', { style: { fontSize: '12px', color: '#64748b' } }, new Date(pay.PaymentDate).toLocaleDateString('id-ID', { day: '2-digit', month: '2-digit', year: 'numeric' }) + (pay.PaymentMethod ? ' · ' + pay.PaymentMethod : ''))
                                                    )
                                                    , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' } }
                                                        , React.createElement('div', { style: { fontWeight: '800', color: '#198754', fontSize: '15px', marginRight: '4px' } }, '+Rp ' + (pay.PaymentAmount || 0).toLocaleString('id-ID'))
                                                        , React.createElement('button', { onClick: () => { setEditPayId(pay.ID); setEditPayForm({ nominal: pay.PaymentAmount || 0, tanggal: new Date(pay.PaymentDate).toISOString().split('T')[0], metode: pay.PaymentMethod || 'TRANSFER' }); }, style: { background: '#fef3c7', color: '#d97706', border: 'none', borderRadius: '8px', width: '30px', height: '30px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '14px', flexShrink: 0 }, title: 'Edit Pembayaran' }, '✏️')
                                                        , React.createElement('button', { onClick: () => handleDeletePay(pay.ID, idx + 1), style: { background: '#fee2e2', color: '#dc3545', border: 'none', borderRadius: '8px', width: '30px', height: '30px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '14px', flexShrink: 0 }, title: 'Hapus Pembayaran' }, '🗑️')
                                                    )
                                                )
                                            )
                                        )
                                    )
                                        , activeInvoice.Status === 'PAID' && React.createElement('div', { style: { display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px', background: '#f0fdf4', borderRadius: '12px', padding: '14px', border: '2px solid #86efac', marginTop: '4px' } }
                                            , React.createElement('span', { style: { fontSize: '15px', fontWeight: '800', color: '#16a34a' } }, '✅ LUNAS pada ' + new Date(activeInvoice.payments[activeInvoice.payments.length - 1].PaymentDate).toLocaleDateString('id-ID'))
                                        )
                                ) : (
                                    React.createElement('div', { style: { textAlign: 'center', padding: '20px', background: '#f8fafc', borderRadius: '12px', color: '#94a3b8', fontSize: '13px' } }, 'Belum ada pembayaran')
                                )
                            )

                            , activeInvoice.Status !== 'PAID' && (
                                React.createElement('div', {}
                                    , React.createElement('div', { style: { fontSize: '11px', fontWeight: '700', color: '#198754', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '12px', background: '#f0fdf4', padding: '8px 12px', borderRadius: '8px', display: 'inline-block' } }
                                        , '💳 Input Pembayaran ke-' + ((activeInvoice.payments ? activeInvoice.payments.length : 0) + 1)
                                    )
                                    , React.createElement('form', { onSubmit: handlePaySubmit }
                                        , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: '12px', marginBottom: '12px' } }
                                            , React.createElement('div', {}
                                                , React.createElement('label', { style: { display: 'block', marginBottom: '6px', fontSize: '12px', fontWeight: '600', color: '#374151' } }, 'Tanggal Bayar')
                                                , React.createElement('input', { type: 'date', required: true, value: payForm.tanggal, onChange: e => setPayForm({ ...payForm, tanggal: e.target.value }), style: { width: '100%', padding: '10px 12px', border: '1px solid #e2e8f0', borderRadius: '10px', fontSize: '14px', boxSizing: 'border-box' } })
                                            )
                                            , React.createElement('div', {}
                                                , React.createElement('label', { style: { display: 'block', marginBottom: '6px', fontSize: '12px', fontWeight: '600', color: '#374151' } }, 'Metode')
                                                , React.createElement('select', { value: payForm.metode, onChange: e => setPayForm({ ...payForm, metode: e.target.value }), style: { width: '100%', padding: '10px 12px', border: '1px solid #e2e8f0', borderRadius: '10px', fontSize: '14px', background: 'white', boxSizing: 'border-box' } }
                                                    , React.createElement('option', { value: 'TRANSFER' }, 'Transfer Bank')
                                                    , React.createElement('option', { value: 'TUNAI' }, 'Tunai / Cash')
                                                    , React.createElement('option', { value: 'CEK' }, 'Cek / Giro')
                                                )
                                            )
                                        )
                                        , React.createElement('div', { style: { marginBottom: '10px' } }
                                            , React.createElement('label', { style: { display: 'block', marginBottom: '6px', fontSize: '12px', fontWeight: '600', color: '#374151' } }, 'Nominal (Rp)')
                                            , React.createElement('input', { type: 'number', required: true, min: 1, value: payForm.nominal === 0 ? '' : payForm.nominal, onChange: e => setPayForm({ ...payForm, nominal: e.target.value === '' ? 0 : Number(e.target.value) }), placeholder: '0', style: { width: '100%', padding: '12px', border: '2px solid #e2e8f0', borderRadius: '10px', fontSize: '18px', fontWeight: '800', color: '#1e293b', boxSizing: 'border-box', outline: 'none' } })
                                        )
                                        , React.createElement('div', { style: { background: '#fffbeb', border: '1px solid #fde68a', borderRadius: '10px', padding: '10px 14px', marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' } }
                                            , React.createElement('span', { style: { fontSize: '12px', color: '#92400e', fontWeight: '600' } }, 'Sisa tagihan:')
                                            , React.createElement('span', { style: { fontSize: '13px', fontWeight: '800', color: '#b45309' } }, 'Rp ' + (activeInvoice.Total - activeInvoice.PaidAmount).toLocaleString('id-ID'))
                                        )
                                        , React.createElement('button', { type: 'submit', style: { width: '100%', padding: '14px', background: 'linear-gradient(135deg, #16a34a, #15803d)', color: 'white', border: 'none', borderRadius: '12px', fontWeight: '800', cursor: 'pointer', fontSize: '15px', boxShadow: '0 4px 12px rgba(22,163,74,0.3)' } }
                                            , '💰 Simpan Pembayaran ke-' + ((activeInvoice.payments ? activeInvoice.payments.length : 0) + 1)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )

            , showEdit && (
                React.createElement('div', { style: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1200, padding: '10px', backdropFilter: 'blur(4px)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 750}}
                    , React.createElement('div', { style: { background: 'white', padding: isMobile ? '20px' : '30px', borderRadius: '24px', width: '100%', maxWidth: '650px', maxHeight: '90vh', overflowY: 'auto', position: 'relative' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 751}}
                        , React.createElement('button', { onClick: () => setShowEdit(false), style: { position: 'absolute', right: '20px', top: '20px', border: 'none', background: 'none', color: '#94a3b8', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 752}}, React.createElement(X, { size: 24, __self: this, __source: {fileName: _jsxFileName, lineNumber: 752}}))
                        , React.createElement('h3', { style: { marginBottom: '25px', fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 753}}, "Edit Produk Faktur"  )
                        , React.createElement('form', { onSubmit: handleEdit, __self: this, __source: {fileName: _jsxFileName, lineNumber: 754}}
                            , editData.products.map((p, i) => (
                                React.createElement('div', { key: i, style: { display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '2fr 1fr 1fr 1.5fr 1.5fr 40px', gap: '10px', marginBottom: '15px', alignItems: 'end', background: '#f8fafc', padding: '15px', borderRadius: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 756}}
                                    , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 757}}
                                        , React.createElement('label', { style: { display: 'block', fontSize: '11px', fontWeight: '700', marginBottom: '5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 758}}, "Barang")
                                        , React.createElement('select', { required: true, value: p.master_item_id, onChange: e => {
                                            const val = Number(e.target.value);
                                            const master = (masterProducts || []).find(m => m.ID === val);
                                            const newProducts = [...editData.products];
                                            newProducts[i].master_item_id = val;
                                            if (master) newProducts[i].custom_price = master.Price;
                                            setEditData({ ...editData, products: newProducts });
                                        }, style: { width: '100%', padding: '10px', border: '1px solid #e2e8f0', borderRadius: '10px', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 759}}
                                            , React.createElement('option', { value: 0, __self: this, __source: {fileName: _jsxFileName, lineNumber: 767}}, "-- Pilih --"  )
                                            , (masterProducts || []).map(mp => React.createElement('option', { key: mp.ID, value: mp.ID, __self: this, __source: {fileName: _jsxFileName, lineNumber: 768}}, mp.Title))
                                        )
                                    )
                                    , React.createElement('div', { style: { display: 'flex', gap: '8px', gridColumn: isMobile ? 'span 1' : 'span 2' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 771}}
                                        , React.createElement('div', { style: { flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 772}}
                                            , React.createElement('label', { style: { display: 'block', fontSize: '11px', fontWeight: '700', marginBottom: '5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 773}}, "Lusin")
                                            , React.createElement('input', { type: "number", step: "any", required: true, value: p.jumlah_lusin, onChange: e => {
                                                const newProducts = [...editData.products];
                                                newProducts[i].jumlah_lusin = Number(e.target.value);
                                                setEditData({ ...editData, products: newProducts });
                                            }, style: { width: '100%', padding: '10px', border: '1px solid #e2e8f0', borderRadius: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 774}} )
                                        )
                                        , React.createElement('div', { style: { flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 780}}
                                            , React.createElement('label', { style: { display: 'block', fontSize: '11px', fontWeight: '700', marginBottom: '5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 781}}, "Qty")
                                            , React.createElement('input', { type: "number", step: "any", required: true, value: p.quantity, onChange: e => {
                                                const newProducts = [...editData.products];
                                                newProducts[i].quantity = Number(e.target.value);
                                                setEditData({ ...editData, products: newProducts });
                                            }, style: { width: '100%', padding: '10px', border: '1px solid #e2e8f0', borderRadius: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 782}} )
                                        )
                                    )
                                    , React.createElement('div', { style: { flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 789}}
                                        , React.createElement('label', { style: { display: 'block', fontSize: '11px', fontWeight: '700', marginBottom: '5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 790}}, "Harga Jual" )
                                        , React.createElement('input', { type: "number", required: true, value: p.custom_price, onChange: e => {
                                            const newProducts = [...editData.products];
                                            newProducts[i].custom_price = Number(e.target.value);
                                            setEditData({ ...editData, products: newProducts });
                                        }, style: { width: '100%', padding: '10px', border: '1px solid #e2e8f0', borderRadius: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 791}} )
                                    )
                                    , React.createElement('div', { style: { flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 797}}
                                        , React.createElement('label', { style: { display: 'flex', alignItems: 'center', fontSize: '11px', fontWeight: '700', marginBottom: '5px', gap: '5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 798}}
                                            , React.createElement('input', { type: "checkbox", checked: p.is_khusus, onChange: e => {
                                                const newProducts = [...editData.products];
                                                newProducts[i].is_khusus = e.target.checked;
                                                if (e.target.checked) {
                                                    const master = (masterProducts || []).find(m => m.ID === p.master_item_id);
                                                    const masterPrice = master ? master.Price : 0;
                                                    const computedModal = Math.max(0, (masterPrice * Number(p.jumlah_lusin) * Number(p.quantity)) - (Number(p.custom_price) * Number(p.jumlah_lusin) * Number(p.quantity)));
                                                    newProducts[i].harga_beli = computedModal;
                                                } else {
                                                    newProducts[i].harga_beli = 0;
                                                }
                                                setEditData({ ...editData, products: newProducts });
                                            }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 799}} ), "Khusus"

                                        )
                                        , p.is_khusus && (
                                            React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 815}}
                                                , React.createElement('input', { type: "number", placeholder: "Modal Beli" , required: true, value: p.harga_beli || '', onChange: e => {
                                                    const newProducts = [...editData.products];
                                                    newProducts[i].harga_beli = Number(e.target.value);
                                                    setEditData({ ...editData, products: newProducts });
                                                }, style: { width: '100%', padding: '8px', border: '1px solid #fab005', borderRadius: '8px', marginTop: '5px', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 816}} )
                                                , (() => {
                                                    const master = (masterProducts || []).find(m => m.ID === p.master_item_id);
                                                    const masterPrice = master ? master.Price : 0;
                                                    const computedModal = Math.max(0, (masterPrice * Number(p.jumlah_lusin) * Number(p.quantity)) - (Number(p.custom_price) * Number(p.jumlah_lusin) * Number(p.quantity)));
                                                    return (
                                                        React.createElement('div', { style: { fontSize: '10px', color: '#856404', marginTop: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 826}}, "Saran: Rp "
                                                              , computedModal.toLocaleString('id-ID')
                                                        )
                                                    );
                                                })()
                                            )
                                        )
                                    )
                                    , React.createElement('button', { type: "button", onClick: () => {
                                        const newProducts = editData.products.filter((_, idx) => idx !== i);
                                        setEditData({ ...editData, products: newProducts });
                                    }, style: { color: '#dc3545', border: 'none', background: 'none', padding: '10px', cursor: 'pointer', display: 'flex', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 834}}, React.createElement(Trash2, { size: 20, __self: this, __source: {fileName: _jsxFileName, lineNumber: 837}}))
                                )
                            ))
                            , React.createElement('button', { type: "button", onClick: () => setEditData({ ...editData, products: [...editData.products, { master_item_id: 0, quantity: 1, jumlah_lusin: 1, custom_price: 0, is_khusus: false, harga_beli: 0 }] }), style: { background: 'white', color: '#0d6efd', border: '2px dashed #0d6efd', padding: '12px', borderRadius: '12px', cursor: 'pointer', width: '100%', fontWeight: 'bold', marginBottom: '25px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 840}}, "+ Tambah Item Barang"

                            )

                            , React.createElement('div', { style: { display: 'flex', gap: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 844}}
                                , React.createElement('button', { type: "button", onClick: () => setShowEdit(false), style: { flex: 1, padding: '14px', background: '#f1f5f9', color: '#64748b', border: 'none', borderRadius: '12px', fontWeight: 'bold', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 845}}, "Batal")
                                , React.createElement('button', { type: "submit", style: { flex: 2, padding: '14px', background: '#0d6efd', color: 'white', border: 'none', borderRadius: '12px', fontWeight: 'bold', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 846}}, "Simpan Perubahan" )
                            )
                        )
                    )
                )
            )
        )
    );
};

export default Invoices;