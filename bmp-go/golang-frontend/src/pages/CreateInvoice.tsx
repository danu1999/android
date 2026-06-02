/* eslint-disable react-hooks/set-state-in-effect */
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { PlusCircle, Trash2, Save, Calendar } from 'lucide-react';

interface Client {
    ID: number;
    ClientName: string;
}

interface MasterProduct {
    ID: number;
    Title: string;
    Price: number;
}

interface InvoiceProduct {
    master_item_id: number;
    quantity: number;
    jumlah_lusin: number;
    custom_price: number;
    is_khusus: boolean;
    harga_beli: number;
}

interface InvoiceState {
    client_id: number;
    number: string;
    title: string;
    payment_terms: string;
    notes: string;
    due_date: string;
    date_created: string;
    status: string;
}

const CreateInvoice: React.FC = () => {
    const navigate = useNavigate();
    const [clients, setClients] = useState<Client[]>([]);
    const [masterProducts, setMasterProducts] = useState<MasterProduct[]>([]);

    const [invoice, setInvoice] = useState<InvoiceState>({
        client_id: 0,
        number: '',
        title: 'Faktur Penjualan',
        payment_terms: '14 days',
        notes: '',
        due_date: '',
        date_created: '',
        status: 'UNPAID'
    });

    const [products, setProducts] = useState<InvoiceProduct[]>([{ master_item_id: 0, quantity: 1, jumlah_lusin: 1, custom_price: 0, is_khusus: false, harga_beli: 0 }]);
    const [productSearch, setProductSearch] = useState<string[]>(['']);
    const [showDropdown, setShowDropdown] = useState<boolean[]>([false]);

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

            const invoices: { Number: string }[] = invoicesRes.data.data || [];
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

    const handleProductChange = (index: number, field: keyof InvoiceProduct, value: string | number | boolean) => {
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

    const handleSave = async (e: React.FormEvent) => {
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
        } catch (error: any) {
            const msg = error.response?.data?.message || "Gagal menyimpan faktur";
            alert(msg);
        }
    };

    return (
        <div className="legacy-page" style={{ padding: '20px', maxWidth: '1000px', margin: '0 auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h2 style={{ fontSize: '24px', fontWeight: 'bold' }}>Buat Faktur Baru</h2>
                <button onClick={() => navigate('/invoices')} style={{ background: '#6c757d', color: 'white', border: 'none', padding: '8px 20px', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold' }}>
                    Kembali ke Riwayat
                </button>
            </div>

            <form onSubmit={handleSave}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '20px' }}>
                    {/* Card 1: Pengaturan Faktur */}
                    <div style={{ background: 'white', padding: '20px', borderRadius: '12px', borderLeft: '5px solid #0d6efd', boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }}>
                        <h5 style={{ color: '#0d6efd', fontWeight: 'bold', marginBottom: '15px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <Calendar size={18} /> 1. Pengaturan Faktur
                        </h5>
                        <div style={{ marginBottom: '12px' }}>
                            <label style={{ display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }}>Nomor Faktur</label>
                            <input required type="text" value={invoice.number} onChange={e => setInvoice({...invoice, number: e.target.value})} style={{ width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px' }} />
                        </div>
                        <div style={{ marginBottom: '12px' }}>
                            <label style={{ display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }}>Jatuh Tempo</label>
                            <input required type="date" value={invoice.due_date} onChange={e => setInvoice({...invoice, due_date: e.target.value})} style={{ width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px' }} />
                        </div>
                        <div style={{ marginBottom: '12px' }}>
                            <label style={{ display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }}>Tanggal Dibuat</label>
                            <input type="date" value={invoice.date_created} onChange={e => setInvoice({...invoice, date_created: e.target.value})} style={{ width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px' }} />
                        </div>
                        <div>
                            <label style={{ display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }}>Status Faktur</label>
                            <select value={invoice.status} onChange={e => setInvoice({...invoice, status: e.target.value})} style={{ width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px', background: 'white' }}>
                                <option value="UNPAID">Belum Bayar</option>
                                <option value="PAID">Lunas</option>
                            </select>
                        </div>
                    </div>

                    {/* Card 2: Pilih Pelanggan */}
                    <div style={{ background: 'white', padding: '20px', borderRadius: '12px', borderLeft: '5px solid #198754', boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }}>
                        <h5 style={{ color: '#198754', fontWeight: 'bold', marginBottom: '15px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <PlusCircle size={18} /> 2. Pilih Pelanggan
                        </h5>
                        <div style={{ marginBottom: '12px' }}>
                            <label style={{ display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }}>Pelanggan</label>
                            <select required value={invoice.client_id} onChange={e => setInvoice({...invoice, client_id: Number(e.target.value)})} style={{ width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px', background: 'white' }}>
                                <option value={0}>-- Pilih Klien --</option>
                                {clients.map(c => <option key={c.ID} value={c.ID}>{c.ClientName}</option>)}
                            </select>
                        </div>
                        <div>
                            <label style={{ display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }}>Catatan / Keterangan</label>
                            <textarea value={invoice.notes} onChange={e => setInvoice({...invoice, notes: e.target.value})} style={{ width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px', height: '85px' }} placeholder="Contoh: Barang titipan abah..."></textarea>
                        </div>
                    </div>
                </div>

                {/* Card 3: Daftar Barang */}
                <div style={{ background: 'white', padding: '25px', borderRadius: '12px', boxShadow: '0 2px 10px rgba(0,0,0,0.05)', marginBottom: '30px' }}>
                    <h5 style={{ fontWeight: 'bold', marginBottom: '20px' }}>📦 3. Masukkan Barang</h5>

                    <div style={{ display: 'flex', gap: '10px', marginBottom: '10px', fontSize: '12px', fontWeight: 'bold', color: '#666', textAlign: 'center' }}>
                        <div style={{ flex: 3, textAlign: 'left' }}>PILIH BARANG</div>
                        <div style={{ flex: 1 }}>LUSIN</div>
                        <div style={{ flex: 1 }}>QTY (FISIK)</div>
                        <div style={{ flex: 1.5 }}>HARGA SATUAN</div>
                        <div style={{ flex: 1.5 }}>SUBTOTAL</div>
                        <div style={{ width: '30px' }}></div>
                    </div>

                    {products.map((p, i) => (
                        <div key={i} style={{ marginBottom: '15px', padding: '15px', border: '1px solid #f1f5f9', borderRadius: '12px' }}>
                            <div style={{ display: 'flex', gap: '10px', marginBottom: '12px', alignItems: 'center' }}>
                                <div style={{ flex: 3, position: 'relative' }}>
                                    <input
                                        type="text"
                                        value={productSearch[i] ?? ''}
                                        onChange={e => {
                                            const s = [...productSearch]; s[i] = e.target.value; setProductSearch(s);
                                            const d = [...showDropdown]; d[i] = true; setShowDropdown(d);
                                            if (p.master_item_id !== 0) handleProductChange(i, 'master_item_id', 0);
                                        }}
                                        onFocus={() => { const d = [...showDropdown]; d[i] = true; setShowDropdown(d); }}
                                        onBlur={() => setTimeout(() => { const d = [...showDropdown]; d[i] = false; setShowDropdown(d); }, 150)}
                                        placeholder="Ketik nama barang..."
                                        style={{ width: '100%', padding: '10px', border: p.master_item_id ? '1px solid #198754' : '1px solid #dee2e6', borderRadius: '6px', boxSizing: 'border-box' }}
                                    />
                                    {showDropdown[i] && (
                                        <div style={{ position: 'absolute', top: '100%', left: 0, right: 0, background: 'white', border: '1px solid #dee2e6', borderRadius: '8px', zIndex: 200, maxHeight: '220px', overflowY: 'auto', boxShadow: '0 8px 20px rgba(0,0,0,0.12)' }}>
                                            {masterProducts
                                                .filter(mp => mp.Title.toLowerCase().includes((productSearch[i] ?? '').toLowerCase()))
                                                .map(mp => (
                                                    <div
                                                        key={mp.ID}
                                                        onMouseDown={e => {
                                                            e.preventDefault();
                                                            handleProductChange(i, 'master_item_id', mp.ID);
                                                            const s = [...productSearch]; s[i] = mp.Title; setProductSearch(s);
                                                            const d = [...showDropdown]; d[i] = false; setShowDropdown(d);
                                                        }}
                                                        style={{ padding: '10px 14px', cursor: 'pointer', borderBottom: '1px solid #f1f5f9' }}
                                                        onMouseEnter={e => (e.currentTarget.style.background = '#f1f5f9')}
                                                        onMouseLeave={e => (e.currentTarget.style.background = 'white')}
                                                    >
                                                        <div style={{ fontWeight: '600', fontSize: '13px' }}>{mp.Title}</div>
                                                        <div style={{ fontSize: '12px', color: '#64748b' }}>Rp {mp.Price.toLocaleString('id-ID')}</div>
                                                    </div>
                                                ))}
                                            {masterProducts.filter(mp => mp.Title.toLowerCase().includes((productSearch[i] ?? '').toLowerCase())).length === 0 && (
                                                <div style={{ padding: '12px', color: '#94a3b8', textAlign: 'center', fontSize: '13px' }}>Barang tidak ditemukan</div>
                                            )}
                                        </div>
                                    )}
                                </div>
                                <div style={{ flex: 1 }}>
                                    <input type="number" step="any" required value={p.jumlah_lusin === 0 ? '' : p.jumlah_lusin} onChange={e => handleProductChange(i, 'jumlah_lusin', e.target.value)} style={{ width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px', textAlign: 'center' }} />
                                </div>
                                <div style={{ flex: 1 }}>
                                    <input type="number" step="any" required value={p.quantity === 0 ? '' : p.quantity} onChange={e => handleProductChange(i, 'quantity', e.target.value)} style={{ width: '100%', padding: '10px', border: '1px solid #dee2e6', borderRadius: '6px', textAlign: 'center' }} />
                                </div>
                                <div style={{ flex: 1.5 }}>
                                    <input type="number" value={p.custom_price === 0 ? '' : p.custom_price} onChange={e => handleProductChange(i, 'custom_price', e.target.value)} style={{ width: '100%', padding: '10px', border: '1px solid #ffc107', borderRadius: '6px', fontWeight: 'bold', textAlign: 'right' }} />
                                </div>
                                <div style={{ flex: 1.5 }}>
                                    <input type="text" readOnly value={`Rp ${(Number(p.quantity) * Number(p.jumlah_lusin) * Number(p.custom_price)).toLocaleString('id-ID')}`} style={{ width: '100%', padding: '10px', border: 'none', borderRadius: '6px', background: '#f8f9fa', textAlign: 'right', fontWeight: 'bold', color: '#198754' }} />
                                </div>
                                <div style={{ width: '30px' }}>
                                    <button type="button" onClick={() => {
                                        setProducts(products.filter((_, idx) => idx !== i));
                                        setProductSearch(productSearch.filter((_, idx) => idx !== i));
                                        setShowDropdown(showDropdown.filter((_, idx) => idx !== i));
                                    }} style={{ background: 'transparent', color: '#dc3545', border: 'none', cursor: 'pointer' }}>
                                        <Trash2 size={18}/>
                                    </button>
                                </div>
                            </div>

                            {/* Opsi Produk Khusus */}
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', padding: '15px', background: '#fff9db', borderRadius: '8px', border: '1px dashed #fcc419' }}>
                                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '13px', fontWeight: 'bold' }}>
                                    <input type="checkbox" checked={p.is_khusus} onChange={e => handleProductChange(i, 'is_khusus', e.target.checked)} style={{ width: '18px', height: '18px' }} />
                                    🌟 Tandai Barang Khusus (Ada Modal Beli Cash)
                                </label>
                                {p.is_khusus && (() => {
                                    const master = masterProducts.find(mp => mp.ID === p.master_item_id);
                                    const masterPrice = master ? master.Price : 0;
                                    const computedModal = Math.max(0, (masterPrice * Number(p.jumlah_lusin) * Number(p.quantity)) - (Number(p.custom_price) * Number(p.jumlah_lusin) * Number(p.quantity)));
                                    return (
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '15px', flexWrap: 'wrap' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                <span style={{ fontSize: '13px', fontWeight: 'bold' }}>Harga Beli (Modal):</span>
                                                <input type="number" placeholder="Contoh: 50000" value={p.harga_beli || ''} onChange={e => handleProductChange(i, 'harga_beli', e.target.value)} style={{ padding: '6px 12px', border: '1px solid #fab005', borderRadius: '6px', width: '150px' }} />
                                            </div>
                                            <span style={{ fontSize: '12px', color: '#856404' }}>
                                                *(Saran HPP: Rp {computedModal.toLocaleString('id-ID')}). Otomatis tercatat di KAS KELUAR
                                            </span>
                                        </div>
                                    );
                                })()}
                            </div>
                        </div>
                    ))}

                    <button type="button" onClick={() => {
                        setProducts([...products, { master_item_id: 0, quantity: 1, jumlah_lusin: 1, custom_price: 0, is_khusus: false, harga_beli: 0 }]);
                        setProductSearch([...productSearch, '']);
                        setShowDropdown([...showDropdown, false]);
                    }} style={{ background: '#e9ecef', color: '#0d6efd', border: '1px dashed #0d6efd', padding: '10px', borderRadius: '8px', cursor: 'pointer', width: '100%', fontWeight: 'bold', marginTop: '10px' }}>
                        + TAMBAH BARIS BARANG LAINNYA
                    </button>
                </div>

                <div style={{ background: '#f8f9fa', padding: '20px', borderRadius: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', border: '1px solid #dee2e6' }}>
                    <div style={{ fontSize: '18px', fontWeight: 'bold' }}>
                        TOTAL AKHIR: <span style={{ color: '#198754', fontSize: '24px', marginLeft: '10px' }}>Rp {total.toLocaleString('id-ID')}</span>
                    </div>
                    <button type="submit" style={{ display: 'flex', alignItems: 'center', gap: '8px', background: '#198754', color: 'white', border: 'none', padding: '15px 40px', borderRadius: '10px', cursor: 'pointer', fontSize: '18px', fontWeight: 'bold', boxShadow: '0 4px 10px rgba(25, 135, 84, 0.3)' }}>
                        <Save size={22} /> SIMPAN SEMUA DATA FAKTUR
                    </button>
                </div>
            </form>
        </div>
    );
};

export default CreateInvoice;