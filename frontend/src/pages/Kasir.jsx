import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Search, ShoppingCart, Trash2, CreditCard, QrCode, Printer, X, ChevronUp, ClipboardList, Plus, Minus, Barcode, Camera } from 'lucide-react';
import api from '../api';
import { useDemoBlock, DEMO_LIMITS } from '../AuthContext';

const getEffectivePrice = (product, quantity) => {
  if (!product.wholesaleEnabled || !product.wholesalePrices) return product.price;
  try {
    const tiers = typeof product.wholesalePrices === 'string' ? JSON.parse(product.wholesalePrices) : product.wholesalePrices;
    const applicable = tiers.filter(t => quantity >= t.minQty).sort((a, b) => b.minQty - a.minQty);
    return applicable.length > 0 ? applicable[0].price : product.price;
  } catch { return product.price; }
};

// Parse variants JSON string from DB → array (add id fallback using index)
const parseVariants = (p) => {
  if (!p.variants) return [];
  try {
    const arr = typeof p.variants === 'string' ? JSON.parse(p.variants) : p.variants;
    return Array.isArray(arr) ? arr.map((v, i) => ({ id: v.id ?? i, ...v })) : [];
  } catch { return []; }
};

export default function Kasir() {
  const { showDemoBlock, isDemo } = useDemoBlock();
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [customers, setCustomers] = useState([]);
  const [customerId, setCustomerId] = useState('');
  const [queueNumber, setQueueNumber] = useState('');
  const [activeQueues, setActiveQueues] = useState([]);
  const [debtTransactionId, setDebtTransactionId] = useState(null);
  const [debtDueDate, setDebtDueDate] = useState('');
  const [cartOpen, setCartOpen] = useState(false);
  const [payModal, setPayModal] = useState(false);
  const [payMethod, setPayMethod] = useState('CASH');
  const [receipt, setReceipt] = useState(null);
  const [queueModal, setQueueModal] = useState(false);
  const [queues, setQueues] = useState([]);
  const [customerName, setCustomerName] = useState('');
  const [notes, setNotes] = useState('');
  const [globalDiscount, setGlobalDiscount] = useState(0);
  const [isBackdate, setIsBackdate] = useState(false);
  const [txDate, setTxDate] = useState('');
  const [demoTxCount, setDemoTxCount] = useState(0);
  const [variantModal, setVariantModal] = useState(null);
  const [lastCart, setLastCart] = useState([]);       // snapshot cart saat checkout untuk print
  const [queueToPrint, setQueueToPrint] = useState(null); // antrian yang baru dibayar → print

  // ── Barcode Scanner ──────────────────────────────────────────
  const barcodeBuffer = useRef('');
  const barcodeTimer = useRef(null);
  const barcodeInputRef = useRef(null);
  const [barcodeFlash, setBarcodeFlash] = useState(null); // null | 'found' | 'notfound'
  const [barcodeInputOpen, setBarcodeInputOpen] = useState(false);
  const [barcodeInputVal, setBarcodeInputVal] = useState('');

  // ── Camera Scanner (HP Android / iPhone) ──────────────────────
  const [cameraOpen, setCameraOpen] = useState(false);
  const [cameraError, setCameraError] = useState('');
  const [cameraLoading, setCameraLoading] = useState(false);
  const html5QrcodeRef = useRef(null);
  const cameraDivId = 'kasir-barcode-camera';


  const handleBarcodeScan = useCallback((code) => {
    // Cari produk berdasarkan field barcode (lokal, tanpa API call)
    const found = products.find(p => p.barcode && p.barcode === code);
    if (found) {
      addToCart(found);
      setBarcodeFlash('found');
    } else {
      setBarcodeFlash('notfound');
    }
    setTimeout(() => setBarcodeFlash(null), 1200);
  }, [products]);

  const stopCamera = useCallback(async () => {
    try {
      if (html5QrcodeRef.current) {
        await html5QrcodeRef.current.stop();
        html5QrcodeRef.current.clear();
        html5QrcodeRef.current = null;
      }
    } catch { }
    setCameraOpen(false);
    setCameraError('');
    setCameraLoading(false);
  }, []);

  const startCamera = useCallback(async () => {
    setCameraError('');
    setCameraLoading(true);
    setCameraOpen(true);
    // Tunggu div camera ter-mount di DOM
    await new Promise(r => setTimeout(r, 250));
    try {
      const { Html5Qrcode } = await import('html5-qrcode');
      const qr = new Html5Qrcode(cameraDivId);
      html5QrcodeRef.current = qr;
      await qr.start(
        { facingMode: 'environment' }, // kamera belakang HP
        { fps: 10, qrbox: { width: 250, height: 250 } },
        (decodedText) => {
          handleBarcodeScan(decodedText);
          stopCamera();
          setBarcodeInputOpen(false);
        },
        () => {} // error per-frame diabaikan
      );
      setCameraLoading(false);
    } catch (err) {
      setCameraLoading(false);
      const msg = String(err?.message || err);
      if (msg.toLowerCase().includes('permission') || msg.toLowerCase().includes('notallowed')) {
        setCameraError('Izin kamera ditolak. Buka Pengaturan browser → izinkan akses kamera untuk situs ini.');
      } else if (msg.toLowerCase().includes('notfound') || msg.toLowerCase().includes('device')) {
        setCameraError('Kamera tidak ditemukan di perangkat ini.');
      } else {
        setCameraError('Gagal membuka kamera. Pastikan browser mendukung dan HTTPS aktif.');
      }
    }
  }, [handleBarcodeScan, stopCamera]);

  // Cleanup kamera saat komponen unmount
  useEffect(() => { return () => { stopCamera(); }; }, [stopCamera]);

  // Global keyboard listener: deteksi input cepat (scanner) vs ketik manual
  useEffect(() => {
    let buffer = '';
    let lastKeyTime = 0;

    const onKeyDown = (e) => {
      const now = Date.now();
      const isModalOpen = !!variantModal || !!payModal || !!queueModal;
      // Abaikan jika user sedang mengetik di input/textarea/modal
      if (isModalOpen) return;
      const tag = document.activeElement?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return;

      if (e.key === 'Enter') {
        if (buffer.length >= 4) handleBarcodeScan(buffer);
        buffer = '';
        return;
      }

      // Hanya karakter printable
      if (e.key.length !== 1) return;

      const interval = now - lastKeyTime;
      lastKeyTime = now;

      // Karakter masuk sangat cepat (< 80ms) = scan, bukan ketik manual
      if (interval < 80 || buffer.length === 0) {
        buffer += e.key;
      } else {
        buffer = e.key; // reset jika jeda terlalu lama
      }

      // Auto-submit setelah 300ms tanpa karakter baru
      clearTimeout(barcodeTimer.current);
      barcodeTimer.current = setTimeout(() => {
        if (buffer.length >= 4) handleBarcodeScan(buffer);
        buffer = '';
      }, 300);
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [handleBarcodeScan, variantModal, payModal, queueModal]);

  useEffect(() => {
    fetchProducts();
    fetchCustomers();
    fetchActiveQueues();
    if (isDemo) {
      api.get('/transactions').then(r => setDemoTxCount(r.data?.length || 0)).catch(() => { });
    }
  }, []);

  const fetchCustomers = async () => {
    try { const r = await api.get('/customers/list'); setCustomers(r.data); } catch { }
  };

  const fetchActiveQueues = async () => {
    try {
      const r = await api.get('/queues/active');
      setActiveQueues(r.data.map(t => t.queueNumber));
    } catch { }
  };

  const resetAllQueues = async () => {
    if (!window.confirm('Yakin ingin reset/kosongkan semua nomor antrian yang dipakai?')) return;
    try {
      const r = await api.get('/queues/active');
      await Promise.all(r.data.map(t => api.put(`/transactions/${t.id}`, { queueNumber: null })));
      fetchActiveQueues();
    } catch { alert('Gagal mereset antrian'); }
  };

  const fetchProducts = async () => {
    try { const r = await api.get('/products'); setProducts(r.data); } catch { }
  };

  const addToCart = (p) => {
    const variants = parseVariants(p);
    if (variants.length > 0) {
      setVariantModal({ ...p, _variants: variants }); return;
    }
    if (p.stock < 1) { alert('Stok habis!'); return; }
    const key = String(p.id);
    setCart(prev => {
      const ex = prev.find(i => i.cartKey === key);
      if (ex) {
        if (ex.quantity >= p.stock) { alert('Stok tidak cukup!'); return prev; }
        return prev.map(i => i.cartKey === key ? { ...i, quantity: i.quantity + 1 } : i);
      }
      return [...prev, { cartKey: key, product: p, variantId: null, variantName: null, variantPrice: null, quantity: 1, discount: 0 }];
    });
  };

  const addVariantToCart = (product, variant) => {
    const stock = variant.stock !== null && variant.stock !== undefined ? variant.stock : product.stock;
    if (stock < 1) { alert('Stok varian ini habis!'); return; }
    const key = `${product.id}-v${variant.id}`;
    setCart(prev => {
      const ex = prev.find(i => i.cartKey === key);
      if (ex) {
        if (ex.quantity >= stock) { alert('Stok tidak cukup!'); return prev; }
        return prev.map(i => i.cartKey === key ? { ...i, quantity: i.quantity + 1 } : i);
      }
      return [...prev, { cartKey: key, product, variantId: variant.id, variantName: variant.name, variantPrice: variant.price || null, quantity: 1, discount: 0 }];
    });
    setVariantModal(null);
  };

  const addOriginalToCart = (product) => {
    if (product.stock < 1) { alert('Stok produk habis!'); return; }
    const key = String(product.id);
    setCart(prev => {
      const ex = prev.find(i => i.cartKey === key);
      if (ex) {
        if (ex.quantity >= product.stock) { alert('Stok tidak cukup!'); return prev; }
        return prev.map(i => i.cartKey === key ? { ...i, quantity: i.quantity + 1 } : i);
      }
      return [...prev, { cartKey: key, product, variantId: null, variantName: null, variantPrice: null, quantity: 1, discount: 0 }];
    });
    setVariantModal(null);
  };

  const updateQty = (cartKey, delta) => setCart(prev => prev.map(i => {
    if (i.cartKey !== cartKey) return i;
    const nq = i.quantity + delta;
    if (nq < 1) return i;
    const maxStock = i.variantId
      ? (parseVariants(i.product).find(v => v.id === i.variantId)?.stock ?? i.product.stock)
      : i.product.stock;
    if (nq > maxStock) return i;
    return { ...i, quantity: nq };
  }));

  const removeItem = (cartKey) => setCart(prev => prev.filter(i => i.cartKey !== cartKey));

  const getItemPrice = (item) => item.variantPrice || getEffectivePrice(item.product, item.quantity);
  const total = cart.reduce((s, i) => s + (getItemPrice(i) - i.discount) * i.quantity, 0) - globalDiscount;
  const totalItems = cart.reduce((s, i) => s + i.quantity, 0);

  const checkout = async (isQueue = false) => {
    if (!cart.length) return;
    if (isDemo) {
      showDemoBlock('Memproses transaksi hanya tersedia di akun berbayar. Data demo tidak akan tersimpan ke database. Upgrade untuk mulai berjualan!');
      return;
    }
    try {
      const r = await api.post('/transactions', {
        items: cart.map(i => ({ productId: i.product.id, variantId: i.variantId || null, quantity: i.quantity, price: i.variantPrice || i.product.price, discount: i.discount })),
        total, discount: globalDiscount,
        paymentMethod: isQueue ? 'PENDING' : payMethod,
        status: isQueue ? 'PENDING' : 'COMPLETED',
        notes,
        customerName: customers.find(c => c.id === Number(customerId))?.name || '',
        customerId: customerId ? Number(customerId) : null,
        queueNumber: queueNumber ? Number(queueNumber) : null,
        type: isBackdate ? 'BACKDATE' : 'SALES',
        date: isBackdate ? txDate : undefined
      });
      if (!isQueue) setReceipt(r.data);
      else alert('Ditambahkan ke antrian!');
      if (isDemo) setDemoTxCount(c => c + 1);

      setLastCart([...cart]); // simpan snapshot cart untuk print varian
      setCart([]); setGlobalDiscount(0); setCustomerId(''); setCustomerName(''); setQueueNumber(''); setNotes('');
      setPayModal(false); setCartOpen(false); fetchProducts(); fetchActiveQueues();
    } catch { alert('Transaksi gagal'); }
  };

  const fetchQueue = async () => {
    try {
      const r = await api.get('/queues/pending');
      setQueues(r.data);
      setQueueModal(true);
    } catch { }
  };

  const cancelQueue = async (id) => {
    if (isDemo) {
      showDemoBlock('Membatalkan antrian hanya tersedia di akun berbayar.');
      return;
    }
    if (!window.confirm('Batalkan pesanan antrian ini?')) return;
    try {
      await api.put(`/transactions/${id}`, { status: 'CANCELLED' });
      setQueues(prev => prev.filter(q => q.id !== id));
      fetchActiveQueues();
    } catch { alert('Gagal membatalkan antrian'); }
  };

  const payQueue = async (id, method) => {
    try {
      if (method === 'HUTANG') {
        if (!debtDueDate) { alert('Silakan pilih tanggal jatuh tempo!'); return; }
        const t = queues.find(q => q.id === id);

        await api.put(`/transactions/${id}`, { status: 'COMPLETED', paymentMethod: 'HUTANG' });
        await api.post('/finances', {
          type: 'RECEIVABLE',
          amount: t.total,
          description: `Hutang (Jatuh tempo: ${debtDueDate}) - ${t.customerName || t.receiptNumber}`,
          date: new Date().toISOString(),
          status: 'PENDING',
          customerId: t.customerId
        });
        alert('Pembayaran tercatat sebagai Hutang (Piutang)!');
        setDebtTransactionId(null);
        setDebtDueDate('');
      } else {
        const t = queues.find(q => q.id === id);
        await api.put(`/transactions/${id}`, { status: 'COMPLETED', paymentMethod: method });
        setQueueModal(false);
        setQueueToPrint({ ...t, paymentMethod: method }); // buka struk
      }
      if (queues.find(q => q.id === id) && !['CASH', 'QRIS'].includes(method)) setQueueModal(false);
    } catch { alert('Gagal memproses pembayaran'); }
  };

  // Cari nama varian dari produk berdasarkan harga item
  const findVariantName = (product, price) => {
    const vars = parseVariants(product);
    if (!vars.length) return null;
    const matched = vars.find(v => v.price != null && Math.abs(Number(v.price) - Number(price)) < 0.01);
    return matched?.name || null;
  };

  const RECEIPT_STYLE = (size) =>
    `body{font-family:monospace;width:${size};margin:0 auto;padding:10px;font-size:12px}` +
    `.c{text-align:center}.b{font-weight:bold}.r{text-align:right}hr{border-top:1px dashed #000}` +
    `td{vertical-align:top;padding:1px 2px}`;

  const printReceipt = (size) => {
    const w = window.open('', '_blank');
    const qLine = receipt?.queueNumber ? `<div class="b" style="font-size:14px">No. Antrian: ${receipt.queueNumber}</div>` : '';
    const cLine = receipt?.customerName ? `<div>Pelanggan: ${receipt.customerName}</div>` : '';
    const itemsHtml = (receipt?.items || []).map((item, idx) => {
      const prod = products.find(p => p.id === item.productId);
      const prodName = prod?.name || 'Item';
      const variantName = lastCart[idx]?.variantName || findVariantName(prod, item.price);
      const name = variantName ? `${prodName} <b>(${variantName})</b>` : prodName;
      const disc = item.discount > 0 ? ` <small>-Rp${Number(item.discount).toLocaleString('id-ID')}</small>` : '';
      const subtotal = item.quantity * item.price - (item.discount || 0);
      return `<tr><td colspan="3">${name}</td></tr><tr><td>${item.quantity}x</td><td>Rp${Number(item.price).toLocaleString('id-ID')}${disc}</td><td class="r">Rp${subtotal.toLocaleString('id-ID')}</td></tr>`;
    }).join('');
    const discRow = receipt?.discount > 0 ? `<tr><td colspan="2">Diskon Global</td><td class="r">-Rp${Number(receipt.discount).toLocaleString('id-ID')}</td></tr>` : '';
    w.document.write(`<html><head><style>${RECEIPT_STYLE(size)}</style></head><body>
      <div class="c b" style="font-size:16px">PISANG KEJU RAMAYANA</div><div class="c">Struk Pembayaran</div><hr>
      <div>No: ${receipt?.receiptNumber}</div><div>Metode: ${receipt?.paymentMethod}</div>${qLine}${cLine}<hr>
      <table width="100%">${itemsHtml}</table><hr>
      <table width="100%">${discRow}<tr><td class="b">TOTAL</td><td class="r b" colspan="2">Rp${Number(receipt?.total).toLocaleString('id-ID')}</td></tr></table><hr>
      <div class="c">Terima Kasih!</div>
      <script>window.print();window.onafterprint=()=>window.close()</script></body></html>`);
    w.document.close();
  };

  const printQueueReceipt = (t, size) => {
    const w = window.open('', '_blank');
    const qLine = t.queueNumber ? `<div class="b" style="font-size:14px">No. Antrian: ${t.queueNumber}</div>` : '';
    const cLine = t.customerName ? `<div>Pelanggan: ${t.customerName}</div>` : '';
    const notesLine = t.notes ? `<div>Catatan: ${t.notes}</div>` : '';
    const itemsHtml = (t.items || []).map(item => {
      const prod = item.product;
      const prodName = prod?.name || 'Item';
      const variantName = findVariantName(prod, item.price);
      const name = variantName ? `${prodName} <b>(${variantName})</b>` : prodName;
      const disc = item.discount > 0 ? ` <small>-Rp${Number(item.discount).toLocaleString('id-ID')}</small>` : '';
      const subtotal = item.quantity * item.price - (item.discount || 0);
      return `<tr><td colspan="3">${name}</td></tr><tr><td>${item.quantity}x</td><td>Rp${Number(item.price).toLocaleString('id-ID')}${disc}</td><td class="r">Rp${subtotal.toLocaleString('id-ID')}</td></tr>`;
    }).join('');
    w.document.write(`<html><head><style>${RECEIPT_STYLE(size)}</style></head><body>
      <div class="c b" style="font-size:16px">PISANG KEJU RAMAYANA</div><div class="c">Struk Pembayaran</div><hr>
      <div>No: ${t.receiptNumber}</div><div>Metode: ${t.paymentMethod}</div>${qLine}${cLine}${notesLine}<hr>
      <table width="100%">${itemsHtml}</table><hr>
      <table width="100%"><tr><td class="b">TOTAL</td><td class="r b" colspan="2">Rp${Number(t.total).toLocaleString('id-ID')}</td></tr></table><hr>
      <div class="c">Terima Kasih!</div>
      <script>window.print();window.onafterprint=()=>window.close()</script></body></html>`);
    w.document.close();
  };

  const filtered = products.filter(p => p.name.toLowerCase().includes(searchQuery.toLowerCase()));

  // ---- STYLES ----
  const S = {
    wrap: { display: 'flex', flexDirection: 'column', height: '100%', background: '#F8F9FF', position: 'relative' },
    topbar: { padding: '12px 16px', background: 'white', display: 'flex', gap: '10px', alignItems: 'center', boxShadow: '0 1px 4px rgba(0,0,0,0.06)', flexShrink: 0 },
    searchBox: { flex: 1, display: 'flex', alignItems: 'center', gap: '8px', background: '#F1F3FF', borderRadius: '12px', padding: '10px 14px' },
    searchInput: { border: 'none', background: 'transparent', outline: 'none', fontSize: '0.9rem', width: '100%', color: '#1F2937' },
    queueBtn: { padding: '10px 14px', borderRadius: '12px', border: 'none', background: '#EEF2FF', color: '#4F46E5', fontWeight: 700, fontSize: '0.82rem', cursor: 'pointer', whiteSpace: 'nowrap', display: 'flex', alignItems: 'center', gap: '6px' },
    grid: { display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', rowGap: '4px', columnGap: '16px', padding: '12px 12px 160px', overflowY: 'auto', flex: 1 },
    card: { background: 'white', borderRadius: '16px', boxShadow: '0 2px 10px rgba(0,0,0,0.05)', cursor: 'pointer', transition: 'transform 0.15s', userSelect: 'none', display: 'flex', flexDirection: 'column', overflow: 'hidden' },
    cardThumb: { width: '100%', height: '120px', background: '#EEF2FF', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', flexShrink: 0 },
    cardBody: { padding: '12px 14px', display: 'flex', flexDirection: 'column' },
    cardName: { fontWeight: 800, fontSize: '0.95rem', color: '#111827', lineHeight: 1.3, marginBottom: '8px' },
    cardPrice: { fontWeight: 800, fontSize: '0.9rem', color: '#3B82F6' },
    cardStock: { fontWeight: 600, fontSize: '0.8rem', color: '#9CA3AF' },
    // bottom bar — sits above the mobile-bottom-nav (~65px)
    bottomBar: { position: 'fixed', bottom: '65px', left: 0, right: 0, zIndex: 55 },
    backdrop: { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', backdropFilter: 'blur(3px)', zIndex: -1 },
    sheet: (open) => ({ background: 'white', borderRadius: open ? '20px 20px 0 0' : '16px 16px 0 0', boxShadow: '0 -6px 30px rgba(0,0,0,0.12)', transition: 'max-height 0.4s cubic-bezier(0.4,0,0.2,1)', maxHeight: open ? '65dvh' : '56px', overflow: 'hidden', display: 'flex', flexDirection: 'column' }),
    sheetHandle: { padding: '0 16px', borderBottom: cartOpen ? '1px solid #F3F4F6' : 'none', cursor: 'pointer', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between', minHeight: '56px', background: 'white' },
    sheetBody: { overflowY: 'auto', flex: 1, padding: '0 16px 8px' },
    input: { width: '100%', padding: '11px 14px', borderRadius: '10px', border: '1.5px solid #E5E7EB', outline: 'none', fontSize: '0.9rem', boxSizing: 'border-box', background: '#FAFAFA' },
    pill: (active) => ({ padding: '12px 24px', borderRadius: '12px', border: 'none', fontWeight: 700, fontSize: '0.9rem', cursor: 'pointer', background: active ? '#4F46E5' : '#EEF2FF', color: active ? 'white' : '#4F46E5', transition: 'all 0.2s' }),
    btnPrimary: { width: '100%', padding: '15px', borderRadius: '14px', border: 'none', background: 'linear-gradient(135deg,#6366F1,#4F46E5)', color: 'white', fontWeight: 800, fontSize: '1rem', cursor: 'pointer', boxShadow: '0 4px 14px rgba(99,102,241,0.4)' },
    btnSecondary: { width: '100%', padding: '13px', borderRadius: '14px', border: '1.5px solid #E5E7EB', background: 'white', color: '#374151', fontWeight: 700, fontSize: '0.9rem', cursor: 'pointer' },
  };

  return (
    <div style={S.wrap}>
      {/* Top Bar */}
      <div style={S.topbar}>
        <div style={S.searchBox}>
          <Search size={18} color="#9CA3AF" />
          <input style={S.searchInput} placeholder="Cari produk..." value={searchQuery} onChange={e => setSearchQuery(e.target.value)} />
          {searchQuery && <button onClick={() => setSearchQuery('')} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, color: '#9CA3AF' }}><X size={16} /></button>}
        </div>
        {/* Barcode scanner button — klik untuk buka panel input */}
        <button
          onClick={() => { setBarcodeInputOpen(o => !o); setBarcodeInputVal(''); setTimeout(() => barcodeInputRef.current?.focus(), 80); }}
          title="Klik untuk scan / ketik barcode produk"
          style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '8px 10px', borderRadius: 12, border: 'none', cursor: 'pointer', background: barcodeInputOpen ? '#4F46E5' : barcodeFlash === 'found' ? '#DCFCE7' : barcodeFlash === 'notfound' ? '#FEE2E2' : '#EEF2FF', color: barcodeInputOpen ? 'white' : barcodeFlash === 'found' ? '#16A34A' : barcodeFlash === 'notfound' ? '#DC2626' : '#6366F1', fontSize: 12, fontWeight: 700, transition: 'all 0.2s', flexShrink: 0 }}
        >
          <Barcode size={16} />
          {barcodeFlash === 'found' ? '✓ Scan!' : barcodeFlash === 'notfound' ? '✗ Tdk Ada' : barcodeInputOpen ? 'Tutup' : 'Scan'}
        </button>
        <button style={S.queueBtn} onClick={fetchQueue}><ClipboardList size={16} /> Antrian</button>
      </div>

      {/* Barcode Input Panel */}
      {barcodeInputOpen && (
        <div style={{ background: '#EEF2FF', padding: '10px 16px', display: 'flex', gap: 8, alignItems: 'center', flexShrink: 0, borderBottom: '2px solid #C7D2FE', animation: 'slideDown 0.15s ease' }}>
          <Barcode size={18} color="#4F46E5" style={{ flexShrink: 0 }} />
          <input
            ref={barcodeInputRef}
            value={barcodeInputVal}
            onChange={e => setBarcodeInputVal(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter' && barcodeInputVal.trim()) {
                handleBarcodeScan(barcodeInputVal.trim());
                setBarcodeInputVal('');
              }
            }}
            placeholder="Scan atau ketik barcode, tekan Enter..."
            style={{ flex: 1, border: '1.5px solid #C7D2FE', borderRadius: 10, padding: '9px 12px', fontSize: 14, outline: 'none', background: 'white', fontFamily: 'monospace', letterSpacing: 1 }}
            autoComplete="off"
          />
          <button
            onClick={() => { if (barcodeInputVal.trim()) { handleBarcodeScan(barcodeInputVal.trim()); setBarcodeInputVal(''); } }}
            style={{ padding: '9px 14px', borderRadius: 10, border: 'none', background: '#4F46E5', color: 'white', fontWeight: 700, fontSize: 13, cursor: 'pointer', flexShrink: 0 }}
          >
            Cari
          </button>
          {/* Tombol Kamera — Android & iPhone */}
          <button
            onClick={startCamera}
            title="Scan barcode menggunakan kamera HP"
            style={{ padding: '9px 12px', borderRadius: 10, border: 'none', background: '#10B981', color: 'white', fontWeight: 700, fontSize: 13, cursor: 'pointer', flexShrink: 0, display: 'flex', alignItems: 'center', gap: 5 }}
          >
            <Camera size={16} /> Kamera
          </button>
          <button
            onClick={() => { setBarcodeInputOpen(false); setBarcodeInputVal(''); }}
            style={{ padding: '9px', borderRadius: 10, border: 'none', background: 'white', color: '#9CA3AF', cursor: 'pointer', flexShrink: 0 }}
          >
            <X size={16} />
          </button>
        </div>
      )}

      {/* ── Modal Kamera Scanner (Android & iPhone) ─────────────── */}
      {cameraOpen && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 99999, background: 'rgba(0,0,0,0.93)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
          {/* Header */}
          <div style={{ position: 'absolute', top: 0, left: 0, right: 0, padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ color: 'white', fontWeight: 800, fontSize: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
              <Camera size={20} /> Scan Barcode
            </div>
            <button onClick={stopCamera} style={{ background: 'rgba(255,255,255,0.15)', border: 'none', borderRadius: 99, padding: '8px 16px', color: 'white', fontWeight: 700, fontSize: 13, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}>
              <X size={15} /> Tutup
            </button>
          </div>

          {/* Viewfinder */}
          <div style={{ position: 'relative', width: '100%', maxWidth: 380, padding: '0 24px' }}>
            <div id={cameraDivId} style={{ width: '100%', borderRadius: 20, overflow: 'hidden', background: '#111', minHeight: 280 }} />

            {/* Loading spinner */}
            {cameraLoading && (
              <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 12, padding: '0 24px' }}>
                <div style={{ width: 44, height: 44, border: '4px solid rgba(255,255,255,0.15)', borderTop: '4px solid #6366F1', borderRadius: '50%', animation: 'camSpin 0.8s linear infinite' }} />
                <div style={{ color: 'rgba(255,255,255,0.85)', fontSize: 14, fontWeight: 600 }}>Membuka kamera...</div>
              </div>
            )}

            {/* Sudut viewfinder */}
            {!cameraLoading && !cameraError && (
              <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', width: 220, height: 220, pointerEvents: 'none' }}>
                {[['top:0,left:0', 'borderTop,borderLeft'], ['top:0,right:0', 'borderTop,borderRight'], ['bottom:0,left:0', 'borderBottom,borderLeft'], ['bottom:0,right:0', 'borderBottom,borderRight']].map((_, i) => {
                  const positions = [{top:0,left:0},{top:0,right:0},{bottom:0,left:0},{bottom:0,right:0}];
                  const borders = [
                    {borderTop:'3px solid #6366F1',borderLeft:'3px solid #6366F1',borderRadius:'6px 0 0 0'},
                    {borderTop:'3px solid #6366F1',borderRight:'3px solid #6366F1',borderRadius:'0 6px 0 0'},
                    {borderBottom:'3px solid #6366F1',borderLeft:'3px solid #6366F1',borderRadius:'0 0 0 6px'},
                    {borderBottom:'3px solid #6366F1',borderRight:'3px solid #6366F1',borderRadius:'0 0 6px 0'},
                  ];
                  return <div key={i} style={{ position:'absolute', width:32, height:32, ...positions[i], ...borders[i] }} />;
                })}
                {/* Scan line */}
                <div style={{ position:'absolute', top:'50%', left: 4, right: 4, height: 2, background: 'linear-gradient(90deg,transparent,#6366F1,transparent)', animation: 'camScanLine 2s ease-in-out infinite' }} />
              </div>
            )}
          </div>

          {/* Error */}
          {cameraError && (
            <div style={{ background: 'rgba(254,242,242,0.95)', borderRadius: 16, padding: '18px 20px', margin: '20px 24px', maxWidth: 340, textAlign: 'center', boxShadow: '0 8px 32px rgba(0,0,0,0.3)' }}>
              <div style={{ fontSize: 28, marginBottom: 8 }}>📵</div>
              <div style={{ fontWeight: 800, color: '#DC2626', fontSize: 14, marginBottom: 8 }}>Kamera Tidak Dapat Dibuka</div>
              <div style={{ color: '#7F1D1D', fontSize: 13, lineHeight: 1.7 }}>{cameraError}</div>
              <button onClick={startCamera} style={{ marginTop: 14, padding: '10px 22px', borderRadius: 10, border: 'none', background: '#EF4444', color: 'white', fontWeight: 800, cursor: 'pointer', fontSize: 13 }}>🔄 Coba Lagi</button>
            </div>
          )}

          {!cameraError && !cameraLoading && (
            <div style={{ color: 'rgba(255,255,255,0.6)', fontSize: 13, marginTop: 20, textAlign: 'center' }}>
              Arahkan kamera ke barcode produk
            </div>
          )}
        </div>
      )}

      {/* Keyframes kamera */}
      <style>{`
        @keyframes camSpin { to { transform: rotate(360deg); } }
        @keyframes camScanLine {
          0%,100% { opacity:0; transform:translateY(-80px); }
          50% { opacity:1; transform:translateY(80px); }
        }
      `}</style>

      {/* Low stock warning */}
      {products.some(p => p.stock > 0 && p.stock <= 5) && (
        <div style={{ background: '#FEF9C3', padding: '8px 16px', fontSize: '0.78rem', color: '#92400E', fontWeight: 600, flexShrink: 0 }}>
          ⚠️ {products.filter(p => p.stock > 0 && p.stock <= 5).map(p => `${p.name} (sisa ${p.stock})`).join(' · ')}
        </div>
      )}

      {/* Product Grid */}
      <div style={S.grid}>
        {filtered.map(p => (
          <div key={p.id} className="card" onClick={() => addToCart(p)}>

            {/* Badge varian */}
            {parseVariants(p).length > 0 && (
              <div style={{ position: 'absolute', top: 8, left: 8, background: '#4F46E5', color: 'white', padding: '2px 8px', borderRadius: 6, fontSize: '10px', fontWeight: 700, zIndex: 10 }}>{parseVariants(p).length} Varian</div>
            )}
            {parseVariants(p).length === 0 && p.stock > 0 && p.stock <= 5 && (
              <div style={{ position: 'absolute', top: 8, right: 8, background: '#F59E0B', color: 'white', padding: '2px 8px', borderRadius: 6, fontSize: '10px', fontWeight: 700, zIndex: 10 }}>Sisa {p.stock}</div>
            )}
            {parseVariants(p).length === 0 && p.stock === 0 && (
              <div style={{ position: 'absolute', top: 8, right: 8, background: '#EF4444', color: 'white', padding: '2px 8px', borderRadius: 6, fontSize: '10px', fontWeight: 700, zIndex: 10 }}>HABIS</div>
            )}

            <div className="card__image" style={p.image ? { backgroundImage: `url(${p.image})` } : { backgroundColor: '#E5E7EB' }}>
              {!p.image && <span style={{ fontSize: '2rem' }}>{p.variantEnabled ? '🎨' : '🛒'}</span>}
            </div>

            <div className="card__content">
              <div className="card__text">
                <p className="card__title">{p.name}</p>
              </div>
              <div className="card__footer">
                <div className="card__price">Rp{p.price.toLocaleString('id-ID')}</div>
                <div className="card__variant">
                  {parseVariants(p).length > 0 ? 'Pilih varian ›' : `${p.stock} ${p.unit || 'pcs'}`}
                </div>
              </div>
            </div>

          </div>
        ))}
      </div>

      {/* Bottom Sheet Cart */}
      <div style={S.bottomBar}>
        {cartOpen && <div style={S.backdrop} onClick={() => setCartOpen(false)} />}
        <div style={S.sheet(cartOpen)}>
          {/* Handle */}
          <div style={S.sheetHandle} onClick={() => setCartOpen(o => !o)}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div style={{ position: 'relative' }}>
                <ShoppingCart size={22} color="#4F46E5" />
                {totalItems > 0 && (
                  <span style={{ position: 'absolute', top: '-8px', right: '-8px', background: '#EF4444', color: 'white', borderRadius: '99px', fontSize: '0.65rem', fontWeight: 800, padding: '1px 5px', minWidth: '16px', textAlign: 'center' }}>{totalItems}</span>
                )}
              </div>
              <span style={{ fontWeight: 700, fontSize: '1rem', color: '#1F2937' }}>
                {totalItems > 0 ? `${totalItems} item` : 'Buka Keranjang'}
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              {total > 0 && <span style={{ fontWeight: 800, color: '#4F46E5', fontSize: '1rem' }}>Rp {total.toLocaleString('id-ID')}</span>}
              <ChevronUp size={22} color="#9CA3AF" style={{ transform: cartOpen ? 'rotate(180deg)' : 'none', transition: 'transform 0.3s' }} />
            </div>
          </div>

          {/* Cart Body */}
          <div style={S.sheetBody}>
            {cart.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '24px 0', color: '#9CA3AF', fontSize: '0.9rem' }}>Belum ada item 🛒</div>
            ) : (
              cart.map(item => {
                const ep = getItemPrice(item);
                return (
                  <div key={item.cartKey} style={{ borderBottom: '1px solid #F3F4F6', padding: '12px 0', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 600, fontSize: '0.9rem', color: '#1F2937' }}>
                          {item.product.name}
                          {item.variantName && <span style={{ marginLeft: 6, fontSize: '0.75rem', background: '#EEF2FF', color: '#4F46E5', padding: '1px 7px', borderRadius: 99, fontWeight: 700 }}>{item.variantName}</span>}
                        </div>
                        <div style={{ fontSize: '0.8rem', color: '#4F46E5', fontWeight: 700 }}>Rp {ep.toLocaleString('id-ID')}{item.discount > 0 && <span style={{ color: '#EF4444' }}> -Rp {item.discount.toLocaleString('id-ID')}</span>}</div>
                      </div>
                      <button onClick={() => removeItem(item.cartKey)} style={{ background: '#FEE2E2', border: 'none', borderRadius: '8px', padding: '6px', cursor: 'pointer', color: '#EF4444', display: 'flex', alignItems: 'center' }}>
                        <Trash2 size={23} />
                      </button>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <span style={{ fontSize: '0.72rem', color: '#9CA3AF' }}>Diskon:</span>
                        <input type="number" value={item.discount} min={0}
                          onChange={e => setCart(prev => prev.map(i => i.cartKey === item.cartKey ? { ...i, discount: Number(e.target.value) || 0 } : i))}
                          style={{ width: '60px', padding: '4px 6px', border: '1px solid #E5E7EB', borderRadius: '7px', fontSize: '0.8rem', outline: 'none', textAlign: 'right' }} />
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0', border: '1.5px solid #E5E7EB', borderRadius: '10px', overflow: 'hidden' }}>
                        <button onClick={() => updateQty(item.cartKey, -1)} style={{ padding: '6px 12px', border: 'none', background: '#F9FAFB', cursor: 'pointer', fontWeight: 800, color: '#374151', fontSize: '1rem' }}>−</button>
                        <input
                          type="number" min={1} value={item.quantity}
                          onClick={e => e.target.select()}
                          onChange={e => {
                            const val = parseInt(e.target.value, 10);
                            if (isNaN(val) || val < 1) { setCart(prev => prev.map(i => i.cartKey === item.cartKey ? { ...i, quantity: 1 } : i)); return; }
                            const maxStock = item.variantId
                              ? (parseVariants(item.product).find(v => v.id === item.variantId)?.stock ?? item.product.stock)
                              : item.product.stock;
                            const qty = Math.min(val, maxStock);
                            setCart(prev => prev.map(i => i.cartKey === item.cartKey ? { ...i, quantity: qty } : i));
                          }}
                          style={{ width: '44px', padding: '4px 2px', fontWeight: 700, fontSize: '0.95rem', color: '#1F2937', textAlign: 'center', border: 'none', background: 'transparent', outline: 'none', MozAppearance: 'textfield' }}
                        />
                        <button onClick={() => updateQty(item.cartKey, 1)} style={{ padding: '6px 12px', border: 'none', background: '#F9FAFB', cursor: 'pointer', fontWeight: 800, color: '#4F46E5', fontSize: '1rem' }}>+</button>
                      </div>
                    </div>
                  </div>
                );
              })
            )}

            {/* Summary section */}
            <div style={{ paddingTop: '12px' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '12px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '0.85rem', fontWeight: 600, color: '#4b5563' }}>Pelanggan</label>
                  <select style={S.input} value={customerId} onChange={e => setCustomerId(e.target.value)}>
                    <option value="">-- Pelanggan Umum / Walk-in --</option>
                    {customers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <label style={{ fontSize: '0.85rem', fontWeight: 600, color: '#4b5563' }}>No. Antrian</label>
                    <button onClick={resetAllQueues} style={{ background: '#FEE2E2', color: '#EF4444', border: 'none', borderRadius: '6px', padding: '4px 8px', fontSize: '0.7rem', fontWeight: 700, cursor: 'pointer' }}>Reset Antrian</button>
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(10, 1fr)', gap: '4px' }}>
                    {Array.from({ length: 20 }, (_, i) => i + 1).map(num => {
                      const isUsed = activeQueues.includes(num);
                      const isSelected = queueNumber === num;
                      return (
                        <button
                          key={num}
                          type="button"
                          onClick={() => {
                            if (isSelected) setQueueNumber('');
                            else if (!isUsed) setQueueNumber(num);
                          }}
                          disabled={isUsed}
                          style={{
                            aspectRatio: '1/1', borderRadius: '6px', border: 'none',
                            background: isSelected ? '#4F46E5' : isUsed ? '#FEE2E2' : '#F3F4F6',
                            color: isSelected ? 'white' : isUsed ? '#EF4444' : '#374151',
                            fontWeight: 700, fontSize: '0.85rem',
                            cursor: isUsed ? 'not-allowed' : 'pointer',
                            opacity: isUsed ? 0.5 : 1, padding: 0
                          }}
                        >
                          {num}
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '0.85rem', fontWeight: 600, color: '#4b5563' }}>Catatan Pesanan</label>
                  <textarea style={{ ...S.input, resize: 'none', fontFamily: 'inherit' }} rows={2} placeholder="Masukkan catatan (opsional)" value={notes} onChange={e => setNotes(e.target.value)} />
                </div>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <span style={{ fontSize: '0.85rem', color: '#6B7280' }}>Diskon Transaksi</span>
                <input type="number" value={globalDiscount} min={0}
                  onChange={e => setGlobalDiscount(Number(e.target.value) || 0)}
                  style={{ width: '100px', padding: '6px 10px', border: '1.5px solid #E5E7EB', borderRadius: '8px', fontSize: '0.9rem', textAlign: 'right', outline: 'none' }} />
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 0', borderTop: '2px solid #EEF2FF', marginBottom: '14px' }}>
                <span style={{ fontWeight: 700, color: '#1F2937', fontSize: '1rem' }}>Total</span>
                <span style={{ fontWeight: 800, color: '#4F46E5', fontSize: '1.2rem' }}>Rp {Math.max(0, total).toLocaleString('id-ID')}</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', paddingBottom: '16px' }}>
                <button style={{ ...S.btnSecondary, opacity: cart.length ? 1 : 0.5 }} disabled={!cart.length} onClick={() => checkout(true)}>
                  🕐 Simpan sebagai Antrian
                </button>
                <button style={{ ...S.btnPrimary, opacity: cart.length ? 1 : 0.5 }} disabled={!cart.length} onClick={() => setPayModal(true)}>
                  Bayar Sekarang →
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Payment Modal */}
      {payModal && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel" style={{ maxWidth: 380 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 style={{ margin: 0, fontSize: '1.2rem' }}>Pembayaran</h2>
              <button onClick={() => setPayModal(false)} style={{ background: '#F3F4F6', border: 'none', borderRadius: '8px', padding: '6px', cursor: 'pointer' }}><X size={18} /></button>
            </div>
            <div style={{ display: 'flex', gap: '10px', marginBottom: '16px' }}>
              <button style={S.pill(payMethod === 'CASH')} onClick={() => setPayMethod('CASH')}><CreditCard size={16} style={{ marginRight: 6 }} />Cash</button>
              <button style={S.pill(payMethod === 'QRIS')} onClick={() => setPayMethod('QRIS')}><QrCode size={16} style={{ marginRight: 6 }} />QRIS</button>
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px', fontSize: '0.85rem', color: '#6B7280', cursor: 'pointer' }}>
              <input type="checkbox" checked={isBackdate} onChange={e => setIsBackdate(e.target.checked)} />
              Backdate (transaksi lampau)
            </label>
            {isBackdate && <input type="datetime-local" value={txDate} onChange={e => setTxDate(e.target.value)} style={{ ...S.input, marginBottom: '12px' }} />}
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '14px 0', borderTop: '2px solid #EEF2FF', marginBottom: '16px' }}>
              <span style={{ fontWeight: 700 }}>Total Tagihan</span>
              <span style={{ fontWeight: 800, color: '#4F46E5', fontSize: '1.1rem' }}>Rp {Math.max(0, total).toLocaleString('id-ID')}</span>
            </div>
            <button style={S.btnPrimary} onClick={() => checkout(false)}>Proses Pembayaran</button>
          </div>
        </div>
      )}

      {/* Receipt Modal — Direct Checkout */}
      {receipt && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel text-center">
            <div style={{ fontSize: '3rem', marginBottom: '8px' }}>✅</div>
            <h2 style={{ color: '#16A34A', margin: '0 0 4px' }}>Berhasil!</h2>
            <p style={{ color: '#6B7280', fontSize: '0.85rem', marginBottom: '20px' }}>{receipt.receiptNumber}</p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <button style={S.btnPrimary} onClick={() => printReceipt('58mm')}><Printer size={16} style={{ marginRight: 8 }} />Cetak Struk 58mm</button>
              <button style={S.btnPrimary} onClick={() => printReceipt('80mm')}><Printer size={16} style={{ marginRight: 8 }} />Cetak Struk 80mm</button>
              <button style={S.btnSecondary} onClick={() => setReceipt(null)}>Tutup</button>
            </div>
          </div>
        </div>
      )}

      {/* Receipt Modal — Queue Payment */}
      {queueToPrint && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel text-center">
            <div style={{ fontSize: '3rem', marginBottom: '8px' }}>✅</div>
            <h2 style={{ color: '#16A34A', margin: '0 0 4px' }}>Pembayaran Berhasil!</h2>
            <p style={{ color: '#6B7280', fontSize: '0.85rem', marginBottom: '4px' }}>
              {queueToPrint.queueNumber && <><b>Antrian #{queueToPrint.queueNumber}</b> &middot; </>}{queueToPrint.receiptNumber}
            </p>
            <p style={{ color: '#6B7280', fontSize: '0.82rem', marginBottom: '20px' }}>Metode: {queueToPrint.paymentMethod}</p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <button style={S.btnPrimary} onClick={() => printQueueReceipt(queueToPrint, '58mm')}><Printer size={16} style={{ marginRight: 8 }} />Cetak Struk 58mm</button>
              <button style={S.btnPrimary} onClick={() => printQueueReceipt(queueToPrint, '80mm')}><Printer size={16} style={{ marginRight: 8 }} />Cetak Struk 80mm</button>
              <button style={S.btnSecondary} onClick={() => setQueueToPrint(null)}>Tutup</button>
            </div>
          </div>
        </div>
      )}

      {/* Queue Modal */}
      {queueModal && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel" style={{ maxWidth: 500 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 style={{ margin: 0 }}>Daftar Antrian</h2>
              <button onClick={() => setQueueModal(false)} style={{ background: '#F3F4F6', border: 'none', borderRadius: '8px', padding: '6px', cursor: 'pointer' }}><X size={18} /></button>
            </div>
            <div style={{ maxHeight: '60vh', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {queues.length === 0
                ? <p style={{ textAlign: 'center', color: '#9CA3AF', padding: '20px 0' }}>Tidak ada antrian 🎉</p>
                : queues.map(t => (
                  <div key={t.id} style={{ background: '#F8F9FF', borderRadius: '12px', padding: '12px 14px' }}>
                    <div style={{ fontWeight: 700, marginBottom: '4px' }}>{t.customerName || t.receiptNumber}</div>
                    <div style={{ fontSize: '0.8rem', color: '#6B7280', marginBottom: '8px' }}>
                      Rp {t.total.toLocaleString('id-ID')} · {t.items?.length || 0} item
                      {t.notes && <span style={{ color: '#4F46E5' }}> · {t.notes}</span>}
                    </div>
                    {debtTransactionId === t.id ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', background: '#FFF7ED', padding: '10px', borderRadius: '8px', border: '1px solid #FED7AA' }}>
                        <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#9A3412' }}>Tanggal Jatuh Tempo:</label>
                        <input type="date" value={debtDueDate} onChange={(e) => setDebtDueDate(e.target.value)} style={{ padding: '8px', borderRadius: '6px', border: '1px solid #FDBA74', fontSize: '0.9rem', outline: 'none' }} />
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button style={{ ...S.btnSecondary, flex: 1, padding: '8px', fontSize: '0.8rem' }} onClick={() => setDebtTransactionId(null)}>Batal</button>
                          <button style={{ ...S.btnPrimary, flex: 1, padding: '8px', fontSize: '0.8rem', background: '#F59E0B', boxShadow: 'none' }} onClick={() => payQueue(t.id, 'HUTANG')}>Konfirmasi Hutang</button>
                        </div>
                      </div>
                    ) : (
                      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                        <button style={{ ...S.pill(true), padding: '8px 12px', fontSize: '0.8rem', flex: 1 }} onClick={() => payQueue(t.id, 'CASH')}>Cash</button>
                        <button style={{ ...S.pill(true), padding: '8px 12px', fontSize: '0.8rem', flex: 1 }} onClick={() => payQueue(t.id, 'QRIS')}>QRIS</button>
                        <button style={{ ...S.pill(false), padding: '8px 12px', fontSize: '0.8rem', flex: 1, color: '#EA580C', background: '#FFEDD5' }} onClick={() => setDebtTransactionId(t.id)}>Hutang</button>
                        {/* Tombol Batalkan — semua role bisa, kasir yang langsung handle pelanggan */}
                        <button
                          onClick={() => cancelQueue(t.id)}
                          title="Batalkan antrian ini"
                          style={{
                            padding: '8px 10px', borderRadius: '12px', border: 'none',
                            background: '#FEE2E2', color: '#EF4444',
                            cursor: 'pointer', display: 'flex', alignItems: 'center',
                            fontWeight: 700, flexShrink: 0
                          }}
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    )}
                  </div>
                ))
              }
            </div>
          </div>
        </div>
      )}
      {/* Modal Pilih Varian */}
      {variantModal && (
        <div onClick={() => setVariantModal(null)} style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(3px)', zIndex: 9999, display: 'flex', alignItems: 'flex-end', justifyContent: 'center' }}>
          <div onClick={e => e.stopPropagation()} style={{ background: 'white', borderRadius: '20px 20px 0 0', padding: '20px 16px 32px', width: '100%', maxWidth: '480px', maxHeight: '70vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
              <div style={{ fontWeight: 900, fontSize: '1rem', color: '#111827' }}>Pilih Varian</div>
              <button onClick={() => setVariantModal(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9CA3AF', padding: 4 }}><X size={20} /></button>
            </div>
            <div style={{ fontSize: '0.82rem', color: '#6B7280', marginBottom: 14 }}>{variantModal.name}</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {/* Opsi Original (Tanpa Varian) */}
              <button
                onClick={() => addOriginalToCart(variantModal)}
                disabled={variantModal.stock < 1}
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '13px 16px', borderRadius: 14, border: `2px dashed ${variantModal.stock < 1 ? '#F3F4F6' : '#C7D2FE'}`, background: variantModal.stock < 1 ? '#F9FAFB' : '#F5F3FF', cursor: variantModal.stock < 1 ? 'not-allowed' : 'pointer', opacity: variantModal.stock < 1 ? 0.5 : 1, width: '100%', textAlign: 'left' }}>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.95rem', color: variantModal.stock < 1 ? '#9CA3AF' : '#4F46E5' }}>Original (Tanpa Varian)</div>
                  <div style={{ fontSize: '0.75rem', color: variantModal.stock < 1 ? '#EF4444' : '#6B7280', fontWeight: 600, marginTop: 2 }}>{variantModal.stock < 1 ? 'Stok Habis' : `Stok: ${variantModal.stock}`}</div>
                </div>
                <div style={{ fontWeight: 800, fontSize: '1rem', color: '#4F46E5' }}>Rp {variantModal.price.toLocaleString('id-ID')}</div>
              </button>

              {/* Daftar Varian */}
              {(variantModal._variants || []).map(v => {
                const stock = v.stock !== null && v.stock !== undefined ? v.stock : variantModal.stock;
                const price = v.price || variantModal.price;
                const outOfStock = stock < 1;
                return (
                  <button key={v.id} onClick={() => !outOfStock && addVariantToCart(variantModal, v)} disabled={outOfStock}
                    style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '13px 16px', borderRadius: 14, border: `1.5px solid ${outOfStock ? '#F3F4F6' : '#E5E7EB'}`, background: outOfStock ? '#F9FAFB' : 'white', cursor: outOfStock ? 'not-allowed' : 'pointer', opacity: outOfStock ? 0.5 : 1, width: '100%', textAlign: 'left' }}>
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.95rem', color: outOfStock ? '#9CA3AF' : '#111827' }}>{v.name}</div>
                      <div style={{ fontSize: '0.75rem', color: outOfStock ? '#EF4444' : '#10B981', fontWeight: 600, marginTop: 2 }}>{outOfStock ? 'Stok Habis' : `Stok: ${stock}`}</div>
                    </div>
                    <div style={{ fontWeight: 800, fontSize: '1rem', color: '#4F46E5' }}>Rp {price.toLocaleString('id-ID')}</div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
