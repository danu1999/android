const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\CreateInvoice.tsx"; function _nullishCoalesce(lhs, rhsFn) { if (lhs != null) { return lhs; } else { return rhsFn(); } } function _optionalChain(ops) { let lastAccessLHS = undefined; let value = ops[0]; let i = 1; while (i < ops.length) { const op = ops[i]; const fn = ops[i + 1]; i += 2; if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) { return undefined; } if (op === 'access' || op === 'optionalAccess') { lastAccessLHS = value; value = fn(value); } else if (op === 'call' || op === 'optionalCall') { value = fn((...args) => value.call(lastAccessLHS, ...args)); lastAccessLHS = undefined; } } return value; }/* eslint-disable react-hooks/set-state-in-effect */
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/apiBmp';
import { PlusCircle, Trash2, Save, Calendar } from 'lucide-react';
































const CreateInvoice = () => {
    const navigate = useNavigate();
    const [clients, setClients] = useState([]);
    const [masterProducts, setMasterProducts] = useState([]);

    const [invoice, setInvoice] = useState({
        client_id: 0,
        number: '',
        title: 'Faktur Penjualan',
        payment_terms: '14 days',
        notes: '',
        due_date: '',
        date_created: '',
        status: 'UNPAID'
    });

    const [products, setProducts] = useState([{ master_item_id: 0, quantity: 1, jumlah_lusin: 1, custom_price: 0, is_khusus: false, harga_beli: 0 }]);
    const [productSearch, setProductSearch] = useState(['']);
    const [showDropdown, setShowDropdown] = useState([false]);

    // Efek Pertama: Set tanggal awal saat halaman dibuka
    useEffect(() => {
        const now = new Date();
        const yy = now.toISOString().slice(2, 4);
        const mm = now.toISOString().slice(5, 7);
        const prefix = `BMP-${yy}${mm}-`;

        Promise.all([
            api.get('/clients'),
            api.get('/products'),
            api.get('/invoices'),
        ]).then(([clientsRes, productsRes, invoicesRes]) => {
            setClients(clientsRes.data.data);
            setMasterProducts(productsRes.data.data);

            const invoices = invoicesRes.data.data || [];
            let maxSeq = 0;
            invoices.forEach(inv => {
                if (inv.Number && inv.Number.startsWith(prefix)) {
                    const seq = parseInt(inv.Number.slice(prefix.length), 10);
                    if (!isNaN(seq) && seq > maxSeq) maxSeq = seq;
                }
            });

            setInvoice(prev => ({
                ...prev,
                number: `${prefix}${String(maxSeq + 1).padStart(3, '0')}`,
                due_date: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
                date_created: now.toISOString().split('T')[0],
            }));
        });
    }, []);

    // Efek Kedua: Logika Smart Auto-Due Date berdasarkan Nama Pelanggan
    useEffect(() => {
        if (invoice.client_id !== 0 && clients.length > 0) {
            const selectedClient = clients.find(c => c.ID === invoice.client_id);
            if (selectedClient) {
                let daysToAdd = 14; // Default standar 2 minggu
                const name = selectedClient.ClientName.toLowerCase();

                // Aturan khusus sesuai daftar Bos Muizz
                if (name.includes("ion") || name.includes("katiran")) {
                    daysToAdd = 7; // 1 Minggu
                } else if (name.includes("huda")) {
                    daysToAdd = 21; // 3 Minggu
                } else if (name.includes("ali") || name.includes("wiranto") || name.includes("yeyen") || name.includes("zahid")) {
                    daysToAdd = 30; // 1 Bulan
                } else if (name.includes("kolis") || name.includes("kosiin") || name.includes("malvin")) {
                    daysToAdd = 14; // 2 Minggu
                }

                const calculatedDate = new Date(Date.now() + daysToAdd * 24 * 60 * 60 * 1000)
                    .toISOString().split('T')[0];

                // Update tanggal jatuh tempo di state
                setInvoice(prev => ({ ...prev, due_date: calculatedDate }));
            }
        }
    }, [invoice.client_id, clients]);

    const handleProductChange = (index, field, value) => {
        const newProducts = [...products];

        // Konversi nilai input sesuai tipe data field
        // Jika value adalah string kosong (pengguna menghapus input), set ke 0.
        // Jika input valid, baru parsing ke angka.
        const typedValue = typeof value === 'string' && ['master_item_id', 'quantity', 'jumlah_lusin', 'custom_price', 'harga_beli'].includes(field)
            ? (value === '' ? '' : Number(value))
            : value;

        newProducts[index] = { ...newProducts[index], [field]: typedValue };

        // Auto-fill price if master_item_id changes
        if (field === 'master_item_id') {
            const master = masterProducts.find(mp => mp.ID === Number(value));
            if (master) {
                newProducts[index].custom_price = master.Price;
            }
        }

        // Auto-populate HPP suggestion when is_khusus is checked
        if (field === 'is_khusus' && value === true) {
            const p = newProducts[index];
            const master = masterProducts.find(mp => mp.ID === p.master_item_id);
            const masterPrice = master ? master.Price : 0;
            const computedModal = Math.max(0, (masterPrice * Number(p.jumlah_lusin) * Number(p.quantity)) - (Number(p.custom_price) * Number(p.jumlah_lusin) * Number(p.quantity)));
            p.harga_beli = computedModal;
        }

        setProducts(newProducts);
    };

    const total = React.useMemo(() => {
        return products.reduce((acc, p) => {
            return acc + (Number(p.quantity) * Number(p.jumlah_lusin) * Number(p.custom_price));
        }, 0);
    }, [products]);

    const handleSave = async (e) => {
        e.preventDefault();
        if (invoice.client_id === 0) return alert("Pilih Klien!");

        // Validasi dan konversi nilai 0 ke number sebelum kirim ke backend
        const validProducts = products
            .filter(p => p.master_item_id !== 0 && Number(p.quantity) > 0)
            .map(p => {
                return {
                    ...p,
                    quantity: Number(p.quantity),
                    jumlah_lusin: Number(p.jumlah_lusin),
                    custom_price: Number(p.custom_price),
                    harga_beli: p.is_khusus ? Number(p.harga_beli) : 0
                };
            });

        if (validProducts.length === 0) return alert("Tambahkan minimal 1 barang!");

        try {
            await api.post('/invoices', {
                ...invoice,
                due_date: invoice.due_date + "T00:00:00Z",
                date_created: invoice.date_created ? invoice.date_created + "T00:00:00Z" : new Date().toISOString(),
                products: validProducts
            });
            alert("Faktur berhasil dibuat!");
            navigate('/invoices');
        } catch (error) {
            const msg = _optionalChain([error, 'access', _2 => _2.response, 'optionalAccess', _3 => _3.data, 'optionalAccess', _4 => _4.message]) || "Gagal menyimpan faktur";
            alert(msg);
        }
    };

    return (
        React.createElement('div', { className: "legacy-page", style: { padding: '20px', maxWidth: '1000px', margin: '0 auto' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 192}}
            , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 193}}
                , React.createElement('h2', { style: { fontSize: '24px', fontWeight: 'bold' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 194}}, "Buat Faktur Baru"  )
                , React.createElement('button', { onClick: () => navigate('/invoices'), style: { background: '#6c757d', color: 'white', border: 'none', padding: '8px 20px', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 195}}, "Kembali ke Riwayat"

                )
            )

            , React.createElement('form', { onSubmit: handleSave, __self: this, __source: {fileName: _jsxFileName, lineNumber: 200}}
                , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 201}}
                    /* Card 1: Pengaturan Faktur */
                    , React.createElement('div', { style: { background: 'white', padding: '20px', borderRadius: '12px', borderLeft: '5px solid #0d6efd', boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 203}}
                        , React.createElement('h5', { style: { color: '#0d6efd', fontWeight: 'bold', marginBottom: '15px', display: 'flex', alignItems: 'center', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 204}}
                            , React.createElement(Calendar, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 205}} ), " 1. Pengaturan Faktur"
                        )
                        , React.createElement('div', { style: { marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 207}}
                            , React.createElement('label', { style: { display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 208}}, "Nomor Faktur" )
                            , React.createElement('input', { required: true, type: "text", value: invoice.number, onChange: e => setInvoice({...invoice, number: e.target.value}), style: { width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 209}} )
                        )
                        , React.createElement('div', { style: { marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 211}}
                            , React.createElement('label', { style: { display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 212}}, "Jatuh Tempo" )
                            , React.createElement('input', { required: true, type: "date", value: invoice.due_date, onChange: e => setInvoice({...invoice, due_date: e.target.value}), style: { width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 213}} )
                        )
                        , React.createElement('div', { style: { marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 215}}
                            , React.createElement('label', { style: { display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 216}}, "Tanggal Dibuat" )
                            , React.createElement('input', { type: "date", value: invoice.date_created, onChange: e => setInvoice({...invoice, date_created: e.target.value}), style: { width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 217}} )
                        )
                        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 219}}
                            , React.createElement('label', { style: { display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 220}}, "Status Faktur" )
                            , React.createElement('select', { value: invoice.status, onChange: e => setInvoice({...invoice, status: e.target.value}), style: { width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px', background: 'white' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 221}}
                                , React.createElement('option', { value: "UNPAID", __self: this, __source: {fileName: _jsxFileName, lineNumber: 222}}, "Belum Bayar" )
                                , React.createElement('option', { value: "PAID", __self: this, __source: {fileName: _jsxFileName, lineNumber: 223}}, "Lunas")
                            )
                        )
                    )

                    /* Card 2: Pilih Pelanggan */
                    , React.createElement('div', { style: { background: 'white', padding: '20px', borderRadius: '12px', borderLeft: '5px solid #198754', boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 229}}
                        , React.createElement('h5', { style: { color: '#198754', fontWeight: 'bold', marginBottom: '15px', display: 'flex', alignItems: 'center', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 230}}
                            , React.createElement(PlusCircle, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 231}} ), " 2. Pilih Pelanggan"
                        )
                        , React.createElement('div', { style: { marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 233}}
                            , React.createElement('label', { style: { display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 234}}, "Pelanggan")
                            , React.createElement('select', { required: true, value: invoice.client_id, onChange: e => setInvoice({...invoice, client_id: Number(e.target.value)}), style: { width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px', background: 'white' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 235}}
                                , React.createElement('option', { value: 0, __self: this, __source: {fileName: _jsxFileName, lineNumber: 236}}, "-- Pilih Klien --"   )
                                , clients.map(c => React.createElement('option', { key: c.ID, value: c.ID, __self: this, __source: {fileName: _jsxFileName, lineNumber: 237}}, c.ClientName))
                            )
                        )
                        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 240}}
                            , React.createElement('label', { style: { display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 241}}, "Catatan / Keterangan"  )
                            , React.createElement('textarea', { value: invoice.notes, onChange: e => setInvoice({...invoice, notes: e.target.value}), style: { width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px', height: '85px' }, placeholder: "Contoh: Barang titipan abah..."   , __self: this, __source: {fileName: _jsxFileName, lineNumber: 242}})
                        )
                    )
                )

                /* Card 3: Daftar Barang */
                , React.createElement('div', { style: { background: 'white', padding: '25px', borderRadius: '12px', boxShadow: '0 2px 10px rgba(0,0,0,0.05)', marginBottom: '30px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 248}}
                    , React.createElement('h5', { style: { fontWeight: 'bold', marginBottom: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 249}}, "📦 3. Masukkan Barang"   )

                    , React.createElement('div', { style: { display: 'flex', gap: '10px', marginBottom: '10px', fontSize: '12px', fontWeight: 'bold', color: '#666', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 251}}
                        , React.createElement('div', { style: { flex: 3, textAlign: 'left' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 252}}, "PILIH BARANG" )
                        , React.createElement('div', { style: { flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 253}}, "LUSIN")
                        , React.createElement('div', { style: { flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 254}}, "QTY (FISIK)" )
                        , React.createElement('div', { style: { flex: 1.5 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 255}}, "HARGA SATUAN" )
                        , React.createElement('div', { style: { flex: 1.5 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 256}}, "SUBTOTAL")
                        , React.createElement('div', { style: { width: '30px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 257}})
                    )

                    , products.map((p, i) => (
                        React.createElement('div', { key: i, style: { marginBottom: '15px', padding: '15px', border: '1px solid #f1f5f9', borderRadius: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 261}}
                            , React.createElement('div', { style: { display: 'flex', gap: '10px', marginBottom: '12px', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 262}}
                                , React.createElement('div', { style: { flex: 3, position: 'relative' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 263}}
                                    , React.createElement('input', {
                                        type: "text",
                                        value: _nullishCoalesce(productSearch[i], () => ( '')),
                                        onChange: e => {
                                            const s = [...productSearch]; s[i] = e.target.value; setProductSearch(s);
                                            const d = [...showDropdown]; d[i] = true; setShowDropdown(d);
                                            if (p.master_item_id !== 0) handleProductChange(i, 'master_item_id', 0);
                                        },
                                        onFocus: () => { const d = [...showDropdown]; d[i] = true; setShowDropdown(d); },
                                        onBlur: () => setTimeout(() => { const d = [...showDropdown]; d[i] = false; setShowDropdown(d); }, 150),
                                        placeholder: "Ketik nama barang..."  ,
                                        style: { width: '100%', padding: '10px', border: p.master_item_id ? '1px solid #198754' : '1px solid #dee2e6', borderRadius: '6px', boxSizing: 'border-box' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 264}}
                                    )
                                    , showDropdown[i] && (
                                        React.createElement('div', { style: { position: 'absolute', top: '100%', left: 0, right: 0, background: 'white', border: '1px solid #dee2e6', borderRadius: '8px', zIndex: 200, maxHeight: '220px', overflowY: 'auto', boxShadow: '0 8px 20px rgba(0,0,0,0.12)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 278}}
                                            , masterProducts
                                                .filter(mp => mp.Title.toLowerCase().includes((_nullishCoalesce(productSearch[i], () => ( ''))).toLowerCase()))
                                                .map(mp => (
                                                    React.createElement('div', {
                                                        key: mp.ID,
                                                        onMouseDown: e => {
                                                            e.preventDefault();
                                                            handleProductChange(i, 'master_item_id', mp.ID);
                                                            const s = [...productSearch]; s[i] = mp.Title; setProductSearch(s);
                                                            const d = [...showDropdown]; d[i] = false; setShowDropdown(d);
                                                        },
                                                        style: { padding: '10px 14px', cursor: 'pointer', borderBottom: '1px solid #f1f5f9' },
                                                        onMouseEnter: e => (e.currentTarget.style.background = '#f1f5f9'),
                                                        onMouseLeave: e => (e.currentTarget.style.background = 'white'), __self: this, __source: {fileName: _jsxFileName, lineNumber: 282}}

                                                        , React.createElement('div', { style: { fontWeight: '600', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 294}}, mp.Title)
                                                        , React.createElement('div', { style: { fontSize: '12px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 295}}, "Rp " , mp.Price.toLocaleString('id-ID'))
                                                    )
                                                ))
                                            , masterProducts.filter(mp => mp.Title.toLowerCase().includes((_nullishCoalesce(productSearch[i], () => ( ''))).toLowerCase())).length === 0 && (
                                                React.createElement('div', { style: { padding: '12px', color: '#94a3b8', textAlign: 'center', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 299}}, "Barang tidak ditemukan"  )
                                            )
                                        )
                                    )
                                )
                                , React.createElement('div', { style: { flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 304}}
                                    , React.createElement('input', { type: "number", step: "any", required: true, value: p.jumlah_lusin === 0 ? '' : p.jumlah_lusin, onChange: e => handleProductChange(i, 'jumlah_lusin', e.target.value), style: { width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 305}} )
                                )
                                , React.createElement('div', { style: { flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 307}}
                                    , React.createElement('input', { type: "number", step: "any", required: true, value: p.quantity === 0 ? '' : p.quantity, onChange: e => handleProductChange(i, 'quantity', e.target.value), style: { width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 308}} )
                                )
                                , React.createElement('div', { style: { flex: 1.5 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 310}}
                                    , React.createElement('input', { type: "number", value: p.custom_price === 0 ? '' : p.custom_price, onChange: e => handleProductChange(i, 'custom_price', e.target.value), style: { width: '100%', padding: '10px', border: '1px solid #ffc107', borderRadius: '6px', fontWeight: 'bold', textAlign: 'right' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 311}} )
                                )
                                , React.createElement('div', { style: { flex: 1.5 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 313}}
                                    , React.createElement('input', { type: "text", readOnly: true, value: `Rp ${(Number(p.quantity) * Number(p.jumlah_lusin) * Number(p.custom_price)).toLocaleString('id-ID')}`, style: { width: '100%', padding: '10px', border: 'none', borderRadius: '6px', background: '#f8f9fa', textAlign: 'right', fontWeight: 'bold', color: '#198754' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 314}} )
                                )
                                , React.createElement('div', { style: { width: '30px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 316}}
                                    , React.createElement('button', { type: "button", onClick: () => {
                                        setProducts(products.filter((_, idx) => idx !== i));
                                        setProductSearch(productSearch.filter((_, idx) => idx !== i));
                                        setShowDropdown(showDropdown.filter((_, idx) => idx !== i));
                                    }, style: { background: 'transparent', color: '#dc3545', border: 'none', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 317}}
                                        , React.createElement(Trash2, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 322}})
                                    )
                                )
                            )

                            /* Opsi Produk Khusus */
                            , React.createElement('div', { style: { display: 'flex', flexDirection: 'column', gap: '10px', padding: '15px', background: '#fff9db', borderRadius: '8px', border: '1px dashed #fcc419' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 328}}
                                , React.createElement('label', { style: { display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '13px', fontWeight: 'bold' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 329}}
                                    , React.createElement('input', { type: "checkbox", checked: p.is_khusus, onChange: e => handleProductChange(i, 'is_khusus', e.target.checked), style: { width: '18px', height: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 330}} ), "🌟 Tandai Barang Khusus (Ada Modal Beli Cash)"

                                )
                                , p.is_khusus && (() => {
                                    const master = masterProducts.find(mp => mp.ID === p.master_item_id);
                                    const masterPrice = master ? master.Price : 0;
                                    const computedModal = Math.max(0, (masterPrice * Number(p.jumlah_lusin) * Number(p.quantity)) - (Number(p.custom_price) * Number(p.jumlah_lusin) * Number(p.quantity)));
                                    return (
                                        React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '15px', flexWrap: 'wrap' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 338}}
                                            , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 339}}
                                                , React.createElement('span', { style: { fontSize: '13px', fontWeight: 'bold' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 340}}, "Harga Beli (Modal):"  )
                                                , React.createElement('input', { type: "number", placeholder: "Contoh: 50000" , value: p.harga_beli || '', onChange: e => handleProductChange(i, 'harga_beli', e.target.value), style: { padding: '6px 12px', border: '1px solid #fab005', borderRadius: '6px', width: '150px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 341}} )
                                            )
                                            , React.createElement('span', { style: { fontSize: '12px', color: '#856404' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 343}}, "*(Saran HPP: Rp "
                                                   , computedModal.toLocaleString('id-ID'), "). Otomatis tercatat di KAS KELUAR"
                                            )
                                        )
                                    );
                                })()
                            )
                        )
                    ))

                    , React.createElement('button', { type: "button", onClick: () => {
                        setProducts([...products, { master_item_id: 0, quantity: 1, jumlah_lusin: 1, custom_price: 0, is_khusus: false, harga_beli: 0 }]);
                        setProductSearch([...productSearch, '']);
                        setShowDropdown([...showDropdown, false]);
                    }, style: { background: '#e9ecef', color: '#0d6efd', border: '1px dashed #0d6efd', padding: '10px', borderRadius: '8px', cursor: 'pointer', width: '100%', fontWeight: 'bold', marginTop: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 353}}, "+ TAMBAH BARIS BARANG LAINNYA"

                    )
                )

                , React.createElement('div', { style: { background: '#f8f9fa', padding: '20px', borderRadius: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', border: '1px solid #dee2e6' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 362}}
                    , React.createElement('div', { style: { fontSize: '18px', fontWeight: 'bold' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 363}}, "TOTAL AKHIR: "
                          , React.createElement('span', { style: { color: '#198754', fontSize: '24px', marginLeft: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 364}}, "Rp " , total.toLocaleString('id-ID'))
                    )
                    , React.createElement('button', { type: "submit", style: { display: 'flex', alignItems: 'center', gap: '8px', background: '#198754', color: 'white', border: 'none', padding: '15px 40px', borderRadius: '10px', cursor: 'pointer', fontSize: '18px', fontWeight: 'bold', boxShadow: '0 4px 10px rgba(25, 135, 84, 0.3)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 366}}
                        , React.createElement(Save, { size: 22, __self: this, __source: {fileName: _jsxFileName, lineNumber: 367}} ), " SIMPAN SEMUA DATA FAKTUR"
                    )
                )
            )
        )
    );
};

export default CreateInvoice;