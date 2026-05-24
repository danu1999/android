import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Search, ShoppingCart, Trash2, CreditCard, QrCode, Printer, X, ChevronUp, ClipboardList, Plus, Minus, Barcode, Camera, ShoppingBag } from 'lucide-react';
import api from '../api';
import { useAuth, useDemoBlock, DEMO_LIMITS } from '../AuthContext';
import ProductCard from '../components/ProductCard';

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
  const { user } = useAuth();

  // Akun yang tidak diizinkan menggunakan fitur barcode scanner
  const SCAN_BLOCKED = ['hanafi', 'fahri', 'fed'];
  const canScan = !SCAN_BLOCKED.includes((user?.name || '').toLowerCase().trim());

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
  const [isBackdate, setIsBackdate] = useState(false);
  const [txDate, setTxDate] = useState('');
  const [demoTxCount, setDemoTxCount] = useState(0);
  const [variantModal, setVariantModal] = useState(null);
  const [lastCart, setLastCart] = useState([]);
  const [queueToPrint, setQueueToPrint] = useState(null);

  // Printing States
  const [printingBluetooth, setPrintingBluetooth] = useState(false);
  const [paperSize, setPaperSize] = useState('58mm');

  // ---- Midtrans QRIS States ----
  const [midtransActiveTx, setMidtransActiveTx] = useState(null);
  const [midtransQrUrl, setMidtransQrUrl] = useState(null);
  const [midtransOrderId, setMidtransOrderId] = useState(null);
  const [midtransStatus, setMidtransStatus] = useState('PENDING');
  const [isChargingMidtrans, setIsChargingMidtrans] = useState(false);

  // ── Smart Discount ────────────────────────────────────────────
  const [discountType, setDiscountType] = useState('percent');   // 'percent' | 'nominal'
  const [discountInput, setDiscountInput] = useState('');         // string input user
  const [amountPaid, setAmountPaid] = useState('');              // uang yang dibayar (untuk kembalian)

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
        () => { } // error per-frame diabaikan
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

  // Load Midtrans Snap dynamically based on backend config
  useEffect(() => {
    const initMidtransSnap = async () => {
      try {
        const response = await api.get('/midtrans/config');
        const { clientKey, isProduction } = response.data;
        const snapScriptUrl = isProduction
          ? 'https://app.midtrans.com/snap/snap.js'
          : 'https://app.sandbox.midtrans.com/snap/snap.js';

        const existingScript = document.querySelector(`script[src="${snapScriptUrl}"]`);
        if (!existingScript) {
          const script = document.createElement('script');
          script.src = snapScriptUrl;
          script.setAttribute('data-client-key', clientKey);
          script.async = true;
          document.body.appendChild(script);
        }
      } catch (err) {
        console.error('Gagal memuat konfigurasi Midtrans Snap:', err);
      }
    };
    initMidtransSnap();
  }, []);

  // Midtrans Payment Status Polling
  useEffect(() => {
    let intervalId;
    if (midtransActiveTx && midtransOrderId && midtransStatus === 'PENDING') {
      const checkStatus = async () => {
        try {
          const response = await api.get(`/midtrans/status/${midtransOrderId}`);
          if (response.data.status === 'SUCCESS') {
            setMidtransStatus('SUCCESS');
            const targetMethod = midtransActiveTx.paymentMethod === 'MIDTRANS_SNAP' ? 'MIDTRANS_SNAP' : 'QRIS';
            if (midtransActiveTx.queueNumber) {
              setQueueToPrint({ ...midtransActiveTx, paymentMethod: targetMethod, status: 'COMPLETED' });
            } else {
              setReceipt({ ...midtransActiveTx, paymentMethod: targetMethod, status: 'COMPLETED' });
            }
            setTimeout(() => {
              setMidtransActiveTx(null);
              setMidtransQrUrl(null);
              setMidtransOrderId(null);
            }, 1000);
            fetchProducts();
            fetchActiveQueues();
          } else if (response.data.status === 'CANCELLED') {
            setMidtransStatus('CANCELLED');
            alert('Transaksi kedaluwarsa atau dibatalkan.');
            setTimeout(() => {
              setMidtransActiveTx(null);
              setMidtransQrUrl(null);
              setMidtransOrderId(null);
            }, 2000);
            fetchProducts();
            fetchActiveQueues();
          }
        } catch (err) {
          console.error('Gagal mengecek status Midtrans:', err);
        }
      };

      intervalId = setInterval(checkStatus, 3000);
    }

    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, [midtransActiveTx, midtransOrderId, midtransStatus]);

  const startMidtransPayment = async (tx) => {
    try {
      setIsChargingMidtrans(true);
      setMidtransActiveTx(tx);
      setMidtransStatus('PENDING');
      setPayModal(false);
      setQueueModal(false);

      const response = await api.post('/midtrans/charge', {
        transactionId: tx.id
      });

      setMidtransQrUrl(response.data.qrUrl);
      setMidtransOrderId(response.data.orderId);
      setIsChargingMidtrans(false);
    } catch (error) {
      console.error(error);
      alert('Gagal mendapatkan QRIS Midtrans: ' + (error.response?.data?.error || error.message));
      setIsChargingMidtrans(false);
      setMidtransActiveTx(null);
    }
  };

  const startMidtransSnapPayment = async (tx) => {
    try {
      setIsChargingMidtrans(true);
      setMidtransActiveTx(tx);
      setMidtransStatus('PENDING');
      setPayModal(false);
      setQueueModal(false);

      const response = await api.post('/midtrans/snap-token', {
        transactionId: tx.id
      });

      const { token, orderId } = response.data;
      setMidtransOrderId(orderId);
      setIsChargingMidtrans(false);

      if (window.snap) {
        window.snap.pay(token, {
          onSuccess: async function (result) {
            console.log('Snap Success:', result);
            setMidtransStatus('SUCCESS');
            if (tx.queueNumber) {
              setQueueToPrint({ ...tx, paymentMethod: 'MIDTRANS_SNAP', status: 'COMPLETED' });
            } else {
              setReceipt({ ...tx, paymentMethod: 'MIDTRANS_SNAP', status: 'COMPLETED' });
            }
            setTimeout(() => {
              setMidtransActiveTx(null);
              setMidtransOrderId(null);
            }, 1000);
            fetchProducts();
            fetchActiveQueues();
          },
          onPending: function (result) {
            console.log('Snap Pending:', result);
            alert('Pembayaran tertunda. Silakan selesaikan pembayaran Anda.');
          },
          onError: function (result) {
            console.error('Snap Error:', result);
            alert('Pembayaran gagal: ' + result.status_message);
            api.put(`/transactions/${tx.id}`, { status: 'CANCELLED' }).catch(() => {});
            setMidtransActiveTx(null);
            setMidtransOrderId(null);
            fetchProducts();
            fetchActiveQueues();
          },
          onClose: function () {
            console.log('Customer closed the popup without finishing the payment');
            if (window.confirm('Tutup pembayaran? Transaksi akan tetap tersimpan sebagai PENDING. Pilih OK untuk membatalkan transaksi ini sepenuhnya.')) {
              api.put(`/transactions/${tx.id}`, { status: 'CANCELLED' }).then(() => {
                alert('Transaksi dibatalkan.');
                setMidtransActiveTx(null);
                setMidtransOrderId(null);
                fetchProducts();
                fetchActiveQueues();
              }).catch((err) => {
                console.error(err);
              });
            }
          }
        });
      } else {
        alert('SDK Midtrans Snap tidak termuat dengan benar.');
        setMidtransActiveTx(null);
        setMidtransOrderId(null);
      }
    } catch (error) {
      console.error(error);
      alert('Gagal memproses Snap Midtrans: ' + (error.response?.data?.error || error.message));
      setIsChargingMidtrans(false);
      setMidtransActiveTx(null);
    }
  };

  const cancelMidtransPayment = async () => {
    if (!midtransActiveTx) return;
    if (window.confirm('Apakah Anda yakin ingin membatalkan transaksi Midtrans ini?')) {
      try {
        await api.put(`/transactions/${midtransActiveTx.id}`, { status: 'CANCELLED' });
        setMidtransStatus('CANCELLED');
        alert('Transaksi berhasil dibatalkan dan stok dikembalikan.');
        setMidtransActiveTx(null);
        setMidtransQrUrl(null);
        setMidtransOrderId(null);
        fetchProducts();
        fetchActiveQueues();
      } catch (error) {
        console.error('Gagal membatalkan transaksi:', error);
        alert('Gagal membatalkan transaksi.');
      }
    }
  };

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

  // Produk demo — sama dengan data di Keuangan & Laporan
  const DEMO_PRODUCTS = [
    { id: 'p301', name: 'Pisang Keju Cokelat', price: 15000, costPrice: 9000, stock: 120, unit: 'pcs', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: null, image: '/demo/pisang-keju-coklat.png' },
    { id: 'p302', name: 'Pisang Keju Stroberi', price: 15000, costPrice: 9500, stock: 85,  unit: 'pcs', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: null, image: '/demo/pisang-keju-stroberi.png' },
    { id: 'p303', name: 'Pisang Keju Premium',  price: 20000, costPrice: 11000, stock: 50, unit: 'pcs', wholesaleEnabled: false, wholesalePrices: null,
      variants: JSON.stringify([
        { id: 1, name: 'Keju Melimpah', price: 25000, costPrice: 13000, stock: 30 },
        { id: 2, name: 'Milo Almond',   price: 28000, costPrice: 15000, stock: 20 },
      ]), barcode: null, image: '/demo/pisang-keju-premium.png' },
    { id: 'p304', name: 'Jus Alpukat',          price: 18000, costPrice: 10000, stock: 60, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: null, image: '/demo/jus-alpukat.png' },
    { id: 'p305', name: 'Jus Mangga',            price: 15000, costPrice: 8000,  stock: 75, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: null, image: '/demo/jus-mangga.png' },
    { id: 'p306', name: 'Es Teh Manis',          price: 8000,  costPrice: 3000,  stock: 200,unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: null, image: null },
  ];

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
  const subtotal = cart.reduce((s, i) => s + (getItemPrice(i) - i.discount) * i.quantity, 0);

  // Smart Discount
  const discountInputNum = parseFloat(discountInput) || 0;
  const discountAmt = (() => {
    if (!discountInputNum) return 0;
    if (discountType === 'percent') return Math.round(subtotal * Math.min(discountInputNum, 100) / 100);
    return Math.min(discountInputNum, subtotal); // nominal tidak boleh melebihi subtotal
  })();
  const discountLabel = discountType === 'percent' ? `Diskon ${discountInputNum}%` : 'Diskon';
  const total = subtotal - discountAmt;
  const totalItems = cart.reduce((s, i) => s + i.quantity, 0);
  const change = amountPaid ? Math.max(0, parseFloat(amountPaid) - total) : 0;

  const checkout = async (isQueue = false) => {
    if (!cart.length) return;
    try {
      const r = await api.post('/transactions', {
        items: cart.map(i => ({
          productId: i.product.id,
          variantId: i.variantId || null,
          variantName: i.variantName || null,
          quantity: i.quantity,
          price: i.variantPrice || i.product.price,
          costPrice: i.product.costPrice || 0,
          discount: i.discount,
          note: i.note || null,
        })),
        subtotal,
        total,
        discountType: discountInputNum > 0 ? discountType : null,
        discountInput: discountInputNum,
        discountAmt,
        discount: discountAmt,       // legacy compat
        amountPaid: amountPaid ? parseFloat(amountPaid) : null,
        change: amountPaid ? change : null,
        paymentMethod: isQueue ? 'PENDING' : payMethod,
        status: (isQueue || (['QRIS_MIDTRANS', 'MIDTRANS_SNAP'].includes(payMethod) && !isQueue)) ? 'PENDING' : 'COMPLETED',
        notes,
        customerName: customers.find(c => c.id === Number(customerId))?.name || '',
        customerId: customerId ? Number(customerId) : null,
        queueNumber: queueNumber ? Number(queueNumber) : null,
        type: isBackdate ? 'BACKDATE' : 'SALES',
        date: isBackdate ? txDate : undefined
      });
      if (payMethod === 'QRIS_MIDTRANS' && !isQueue) {
        startMidtransPayment(r.data);
      } else if (payMethod === 'MIDTRANS_SNAP' && !isQueue) {
        startMidtransSnapPayment(r.data);
      } else if (!isQueue) {
        setReceipt(r.data);
      } else {
        alert('Ditambahkan ke antrian!');
      }
      setLastCart([...cart]);
      setCart([]); setDiscountInput(''); setAmountPaid('');
      setCustomerId(''); setCustomerName(''); setQueueNumber(''); setNotes('');
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
      } else if (method === 'QRIS_MIDTRANS') {
        const t = queues.find(q => q.id === id);
        await api.put(`/transactions/${id}`, { paymentMethod: 'QRIS_MIDTRANS' });
        startMidtransPayment(t);
      } else if (method === 'MIDTRANS_SNAP') {
        const t = queues.find(q => q.id === id);
        await api.put(`/transactions/${id}`, { paymentMethod: 'MIDTRANS_SNAP' });
        startMidtransSnapPayment(t);
      } else {
        const t = queues.find(q => q.id === id);
        await api.put(`/transactions/${id}`, { status: 'COMPLETED', paymentMethod: method });
        setQueueModal(false);
        setQueueToPrint({ ...t, paymentMethod: method }); // buka struk
      }
      if (queues.find(q => q.id === id) && !['CASH', 'QRIS', 'QRIS_MIDTRANS', 'MIDTRANS_SNAP'].includes(method)) setQueueModal(false);
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
    `td{vertical-align:top;padding:1px 2px}.indent{padding-left:8px;color:#555;font-size:11px}`;

  const buildReceiptHTML = (t, cartSnapshot, size) => {
    const qLine = t.queueNumber ? `<div class="b c" style="font-size:16px;border:2px solid #000;padding:4px;margin:4px 0">No. Antrian: #${t.queueNumber}</div>` : '';
    const cLine = t.customerName ? `<div>Pelanggan: ${t.customerName}</div>` : '';
    const notesLine = t.notes ? `<div>Catatan: ${t.notes}</div>` : '';
    const itemsHtml = (t.items || cartSnapshot || []).map((item, idx) => {
      const prod = products.find(p => p.id === (item.productId || item.product?.id)) || item.product;
      const prodName = item.productName || prod?.name || 'Item';
      const varName = item.variantName || (cartSnapshot?.[idx]?.variantName) || findVariantName(prod, item.price);
      const nameRow = varName ? `${prodName} <b>(${varName})</b>` : prodName;
      const noteRow = item.note ? `<tr><td colspan="3" class="indent">&#8627; ${item.note}</td></tr>` : '';
      const lineTotal = item.quantity * item.price - (item.discount || 0);
      return `<tr><td colspan="3">${nameRow}</td></tr>${noteRow}<tr><td>${item.quantity}x</td><td>Rp${Number(item.price).toLocaleString('id-ID')}</td><td class="r">Rp${lineTotal.toLocaleString('id-ID')}</td></tr>`;
    }).join('');
    const sub = t.subtotal || t.total;
    const dAmt = t.discountAmt || t.discount || 0;
    const dLabel = t.discountType === 'percent' ? `Diskon (${t.discountInput}%)` : dAmt > 0 ? 'Diskon' : '';
    const discRow = dAmt > 0 ? `<tr><td colspan="2">${dLabel}</td><td class="r">-Rp${Number(dAmt).toLocaleString('id-ID')}</td></tr>` : '';
    const subRow = dAmt > 0 ? `<tr><td colspan="2">Subtotal</td><td class="r">Rp${Number(sub).toLocaleString('id-ID')}</td></tr>` : '';
    const paidRow = t.amountPaid > 0 ? `<tr><td colspan="2">Tunai</td><td class="r">Rp${Number(t.amountPaid).toLocaleString('id-ID')}</td></tr><tr><td colspan="2">Kembali</td><td class="r">Rp${Number(t.change || 0).toLocaleString('id-ID')}</td></tr>` : '';
    const isTargetUser = ['hanafi', 'fed', 'fahri'].includes(user?.name?.toLowerCase().trim());
    const brandRow = isTargetUser ? '<div class="c" style="font-size:10px;margin-top:6px;opacity:0.7">POSBah</div>' : '';
    return `<html><head><style>${RECEIPT_STYLE(size)}</style></head><body>
      <div class="c b" style="font-size:16px">PISANG KEJU RAMAYANA</div>
      <div class="c">Struk Pembayaran</div><hr>
      <div>No: ${t.receiptNumber}</div><div>Metode: ${t.paymentMethod}</div>${cLine}${notesLine}<hr>
      ${qLine}
      <table width="100%">${itemsHtml}</table><hr>
      <table width="100%">${subRow}${discRow}<tr><td colspan="2" class="b">TOTAL</td><td class="r b">Rp${Number(t.total).toLocaleString('id-ID')}</td></tr>${paidRow}</table><hr>
      <div class="c">Terima Kasih! 🙏</div>
      ${brandRow}
      <script>window.print();window.onafterprint=()=>window.close()</script></body></html>`;
  };

  const printReceipt = (size) => {
    const w = window.open('', '_blank');
    w.document.write(buildReceiptHTML(receipt, lastCart, size));
    w.document.close();
  };

  const printQueueReceipt = (t, size) => {
    const w = window.open('', '_blank');
    w.document.write(buildReceiptHTML(t, null, size));
    w.document.close();
  };

  const printViaBluetooth = async (t, cartSnapshot, size) => {
    if (!navigator.bluetooth) {
      alert('Browser Anda tidak mendukung Web Bluetooth. Silakan gunakan opsi Cetak Sistem (Browser).');
      return;
    }
    
    setPrintingBluetooth(true);
    try {
      const device = await navigator.bluetooth.requestDevice({
        acceptAllDevices: true,
        optionalServices: ['000018f0-0000-1000-8000-00805f9b34fb']
      });

      const server = await device.gatt.connect();
      
      let service;
      try {
        service = await server.getPrimaryService('000018f0-0000-1000-8000-00805f9b34fb');
      } catch (e) {
        const services = await server.getPrimaryServices();
        if (services.length > 0) {
          service = services[0];
        } else {
          throw new Error('Gagal menemukan layanan printer Bluetooth.');
        }
      }

      const characteristics = await service.getCharacteristics();
      const writeChar = characteristics.find(c => c.properties.write || c.properties.writeWithoutResponse);
      if (!writeChar) {
        throw new Error('Karakteristik menulis tidak ditemukan di printer Bluetooth.');
      }

      const charLimit = size === '58mm' ? 32 : 48;
      const encoder = new TextEncoder();
      
      const ESC = '\x1b';
      const RESET = ESC + '@';
      const CENTER = ESC + 'a\x01';
      const LEFT = ESC + 'a\x00';
      const DOUBLE_HEIGHT = ESC + '!' + '\x10';
      const NORMAL = ESC + '!' + '\x00';
      const BOLD_ON = ESC + 'E\x01';
      const BOLD_OFF = ESC + 'E\x00';
      const LINE_FEED = '\n';

      const padText = (left, right) => {
        const spaceNeeded = charLimit - left.length - right.length;
        return left + ' '.repeat(spaceNeeded > 0 ? spaceNeeded : 1) + right;
      };

      let d = '';
      d += RESET;
      d += CENTER + DOUBLE_HEIGHT + BOLD_ON + 'PISANG KEJU RAMAYANA' + NORMAL + LINE_FEED;
      d += CENTER + 'Struk Pembayaran UMKM' + LINE_FEED;
      d += CENTER + '-'.repeat(charLimit) + LINE_FEED;

      d += LEFT;
      if (t.queueNumber) {
        d += BOLD_ON + CENTER + `No. Antrian: #${t.queueNumber}` + BOLD_OFF + LEFT + LINE_FEED;
        d += CENTER + '-'.repeat(charLimit) + LEFT + LINE_FEED;
      }
      d += `No. Trans: ${t.receiptNumber}` + LINE_FEED;
      d += `Tanggal  : ${new Date(t.createdAt || Date.now()).toLocaleString('id-ID')}` + LINE_FEED;
      if (t.customerName) {
        d += `Pelanggan: ${t.customerName}` + LINE_FEED;
      }
      d += `Metode   : ${t.paymentMethod}` + LINE_FEED;
      if (t.notes) {
        d += `Catatan  : ${t.notes}` + LINE_FEED;
      }
      d += '-'.repeat(charLimit) + LINE_FEED;

      const items = t.items || cartSnapshot || [];
      items.forEach((item) => {
        const prod = products.find(p => p.id === (item.productId || item.product?.id)) || item.product;
        const prodName = item.productName || prod?.name || 'Item';
        const varName = item.variantName || findVariantName(prod, item.price);
        const nameRow = varName ? `${prodName} (${varName})` : prodName;
        d += `${nameRow}` + LINE_FEED;
        
        if (item.note) {
          d += `  * ${item.note}` + LINE_FEED;
        }

        const lineTotal = item.quantity * item.price - (item.discount || 0);
        const priceDetail = `${item.quantity}x Rp ${Number(item.price).toLocaleString('id-ID')}`;
        const totalStr = `Rp ${lineTotal.toLocaleString('id-ID')}`;
        d += padText(priceDetail, totalStr) + LINE_FEED;
      });
      d += '-'.repeat(charLimit) + LINE_FEED;

      const sub = t.subtotal || t.total;
      const dAmt = t.discountAmt || t.discount || 0;
      const dLabel = t.discountType === 'percent' ? `Diskon (${t.discountInput}%)` : dAmt > 0 ? 'Diskon' : '';

      if (dAmt > 0) {
        d += padText('Subtotal', `Rp ${Number(sub).toLocaleString('id-ID')}`) + LINE_FEED;
        d += padText(dLabel, `-Rp ${Number(dAmt).toLocaleString('id-ID')}`) + LINE_FEED;
      }

      d += BOLD_ON + padText('TOTAL', `Rp ${Number(t.total).toLocaleString('id-ID')}`) + BOLD_OFF + LINE_FEED;

      if (t.amountPaid > 0) {
        d += padText('Tunai', `Rp ${Number(t.amountPaid).toLocaleString('id-ID')}`) + LINE_FEED;
        d += padText('Kembali', `Rp ${Number(t.change || 0).toLocaleString('id-ID')}`) + LINE_FEED;
      }
      d += '-'.repeat(charLimit) + LINE_FEED;

      d += CENTER + 'Terima kasih atas pembelian Anda! 🙏' + LINE_FEED;
      d += CENTER + '- POSBAH -' + LINE_FEED;
      const isTargetUser = ['hanafi', 'fed', 'fahri'].includes(user?.name?.toLowerCase().trim());
      if (isTargetUser) {
        d += CENTER + 'POSBah' + LINE_FEED;
      }
      d += LINE_FEED + LINE_FEED + LINE_FEED;

      const rawBytes = encoder.encode(d);
      const chunkSize = 20;
      for (let i = 0; i < rawBytes.length; i += chunkSize) {
        const chunk = rawBytes.slice(i, i + chunkSize);
        await writeChar.writeValue(chunk);
      }

      alert('Berhasil mengirim data ke printer Bluetooth!');
      device.gatt.disconnect();
    } catch (err) {
      console.error(err);
      alert(`Gagal koneksi printer Bluetooth: ${err.message || err}`);
    } finally {
      setPrintingBluetooth(false);
    }
  };

  const filtered = products.filter(p => p.name.toLowerCase().includes(searchQuery.toLowerCase()));

  // ---- STYLES ----
  const S = {
    wrap: { display: 'flex', flexDirection: 'column', height: '100%', background: '#F0F4FF', position: 'relative' },
    topbar: { padding: '10px 12px', background: 'linear-gradient(135deg,#4F46E5,#6366F1)', display: 'flex', gap: '8px', alignItems: 'center', flexShrink: 0 },
    searchBox: { flex: 1, display: 'flex', alignItems: 'center', gap: '8px', background: 'rgba(255,255,255,0.18)', borderRadius: '12px', padding: '9px 12px', border: '1px solid rgba(255,255,255,0.25)' },
    searchInput: { border: 'none', background: 'transparent', outline: 'none', fontSize: '0.88rem', width: '100%', color: 'white' },
    queueBtn: { padding: '9px 12px', borderRadius: '10px', border: 'none', background: 'rgba(255,255,255,0.18)', color: 'white', fontWeight: 700, fontSize: '0.78rem', cursor: 'pointer', whiteSpace: 'nowrap', display: 'flex', alignItems: 'center', gap: '5px', border: '1px solid rgba(255,255,255,0.25)' },
    grid: { display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 10, padding: '10px 10px 230px', overflowY: 'auto', flex: 1 },
    card: { background: 'white', borderRadius: '14px', boxShadow: '0 1px 6px rgba(79,70,229,0.08)', cursor: 'pointer', userSelect: 'none', overflow: 'hidden', WebkitTapHighlightColor: 'transparent' },
    cardThumb: { width: '100%', background: '#F0F4FF', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', flexShrink: 0, position: 'relative' },
    cardBody: { padding: '8px 10px 10px' },
    cardName: { fontWeight: 700, fontSize: '0.8rem', color: '#1E293B', lineHeight: 1.3, marginBottom: 3, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' },
    cardPrice: { fontWeight: 800, fontSize: '0.85rem', color: '#4F46E5' },
    cardStock: { fontWeight: 600, fontSize: '0.7rem', color: '#9CA3AF', marginTop: 2 },
    bottomBar: { position: 'fixed', bottom: '65px', left: 0, right: 0, zIndex: 55 },
    backdrop: { position: 'fixed', inset: 0, background: 'rgba(15,10,60,0.45)', backdropFilter: 'blur(4px)', zIndex: -1 },
    sheet: (open) => ({ background: 'white', borderRadius: open ? '22px 22px 0 0' : '18px 18px 0 0', boxShadow: '0 -8px 40px rgba(79,70,229,0.15)', transition: 'max-height 0.4s cubic-bezier(0.4,0,0.2,1)', maxHeight: open ? '65dvh' : '58px', overflow: 'hidden', display: 'flex', flexDirection: 'column' }),
    sheetHandle: { padding: '0 16px', borderBottom: cartOpen ? '1px solid #F0F4FF' : 'none', cursor: 'pointer', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between', minHeight: '58px', background: 'white' },
    sheetBody: { overflowY: 'auto', flex: 1, padding: '0 16px 8px' },
    input: { width: '100%', padding: '11px 14px', borderRadius: '10px', border: '1.5px solid #E5E7EB', outline: 'none', fontSize: '0.9rem', boxSizing: 'border-box', background: '#FAFAFA' },
    pill: (active) => ({ padding: '12px 24px', borderRadius: '12px', border: 'none', fontWeight: 700, fontSize: '0.9rem', cursor: 'pointer', background: active ? '#4F46E5' : '#EEF2FF', color: active ? 'white' : '#4F46E5', transition: 'all 0.2s' }),
    btnPrimary: { width: '100%', padding: '15px', borderRadius: '14px', border: 'none', background: 'linear-gradient(135deg,#6366F1,#4F46E5)', color: 'white', fontWeight: 800, fontSize: '1rem', cursor: 'pointer', boxShadow: '0 4px 14px rgba(99,102,241,0.4)' },
    btnSecondary: { width: '100%', padding: '13px', borderRadius: '14px', border: '1.5px solid #E5E7EB', background: 'white', color: '#374151', fontWeight: 700, fontSize: '0.9rem', cursor: 'pointer' },
  };

  return (
    <div style={S.wrap} className="kasir-page">
      {/* Top Bar */}
      <div style={S.topbar}>
        <div style={S.searchBox}>
          <Search size={16} color="rgba(255,255,255,0.7)" />
          <input style={{ ...S.searchInput, '::placeholder': { color: 'rgba(255,255,255,0.6)' } }} placeholder="Cari produk..." value={searchQuery} onChange={e => setSearchQuery(e.target.value)} />
          {searchQuery && <button onClick={() => setSearchQuery('')} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, color: 'rgba(255,255,255,0.7)' }}><X size={15} /></button>}
        </div>
        {canScan && (
          <button
            onClick={() => { setBarcodeInputOpen(o => !o); setBarcodeInputVal(''); setTimeout(() => barcodeInputRef.current?.focus(), 80); }}
            style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '9px 10px', borderRadius: 10, border: '1px solid rgba(255,255,255,0.25)', cursor: 'pointer', background: barcodeFlash === 'found' ? '#10B981' : barcodeFlash === 'notfound' ? '#EF4444' : 'rgba(255,255,255,0.18)', color: 'white', fontSize: 11, fontWeight: 700, transition: 'all 0.2s', flexShrink: 0 }}
          >
            <Barcode size={15} />
            {barcodeFlash === 'found' ? '✓' : barcodeFlash === 'notfound' ? '✗' : 'Scan'}
          </button>
        )}
        <button style={S.queueBtn} onClick={fetchQueue}><ClipboardList size={15} /></button>
      </div>

      {/* Barcode Input Panel — hanya tampil jika canScan */}
      {canScan && barcodeInputOpen && (
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
                  const positions = [{ top: 0, left: 0 }, { top: 0, right: 0 }, { bottom: 0, left: 0 }, { bottom: 0, right: 0 }];
                  const borders = [
                    { borderTop: '3px solid #6366F1', borderLeft: '3px solid #6366F1', borderRadius: '6px 0 0 0' },
                    { borderTop: '3px solid #6366F1', borderRight: '3px solid #6366F1', borderRadius: '0 6px 0 0' },
                    { borderBottom: '3px solid #6366F1', borderLeft: '3px solid #6366F1', borderRadius: '0 0 0 6px' },
                    { borderBottom: '3px solid #6366F1', borderRight: '3px solid #6366F1', borderRadius: '0 0 6px 0' },
                  ];
                  return <div key={i} style={{ position: 'absolute', width: 32, height: 32, ...positions[i], ...borders[i] }} />;
                })}
                {/* Scan line */}
                <div style={{ position: 'absolute', top: '50%', left: 4, right: 4, height: 2, background: 'linear-gradient(90deg,transparent,#6366F1,transparent)', animation: 'camScanLine 2s ease-in-out infinite' }} />
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

      <style>{`
        @keyframes camSpin { to { transform: rotate(360deg); } }
        @keyframes camScanLine {
          0%,100% { opacity:0; transform:translateY(-80px); }
          50% { opacity:1; transform:translateY(80px); }
        }
        input[placeholder] { color: rgba(255,255,255,0.6) !important; }
        ::placeholder { color: rgba(255,255,255,0.6) !important; }
        .kasir-search::placeholder { color: rgba(255,255,255,0.7) !important; }
      `}</style>

      {/* Low stock warning */}
      {products.some(p => p.stock > 0 && p.stock <= 5) && (
        <div style={{ background: '#FEF9C3', padding: '8px 16px', fontSize: '0.78rem', color: '#92400E', fontWeight: 600, flexShrink: 0 }}>
          ⚠️ {products.filter(p => p.stock > 0 && p.stock <= 5).map(p => `${p.name} (sisa ${p.stock})`).join(' · ')}
        </div>
      )}

      {/* Product Grid */}
      <div style={S.grid} className="product-grid">
        {filtered.length > 0 ? filtered.map(p => {
          const variants = parseVariants(p);
          const isOut = variants.length === 0 && p.stock === 0;
          const isLow = variants.length === 0 && p.stock > 0 && p.stock <= 5;
          
          // Safe check to verify if the product really has a valid image string
          const hasImage = p.image && typeof p.image === 'string' && p.image.trim() !== '' && p.image !== 'null' && p.image !== 'undefined';

          const qty = cart
            .filter(i => i.product?.id === p.id || i.product?.id === String(p.id))
            .reduce((s, i) => s + i.quantity, 0);

          return (
            <ProductCard
              key={p.id}
              product={p}
              quantity={qty}
              onAdd={addToCart}
              onVariant={(prod, vars) => setVariantModal({ ...prod, _variants: vars })}
            />
          );
        }) : (
          <div style={{ gridColumn: '1/-1', textAlign: 'center', padding: '40px 0', color: '#94A3B8' }}>
            Produk tidak ditemukan.
          </div>
        )}
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

              {/* Smart Discount */}
              <div style={{ background: '#F8F9FF', borderRadius: 12, padding: '12px 14px', marginBottom: 10, border: '1px solid #E8ECFF' }}>
                <div style={{ fontSize: 13, fontWeight: 700, color: '#374151', marginBottom: 8 }}>🏷️ Diskon</div>
                <div style={{ display: 'flex', gap: 6, marginBottom: 8 }}>
                  <button onClick={() => { setDiscountType('percent'); setDiscountInput(''); }}
                    style={{ flex: 1, padding: '7px 0', borderRadius: 8, border: 'none', fontWeight: 700, fontSize: 12, cursor: 'pointer', background: discountType === 'percent' ? '#4F46E5' : '#EEF2FF', color: discountType === 'percent' ? 'white' : '#4F46E5' }}>
                    % Persen
                  </button>
                  <button onClick={() => { setDiscountType('nominal'); setDiscountInput(''); }}
                    style={{ flex: 1, padding: '7px 0', borderRadius: 8, border: 'none', fontWeight: 700, fontSize: 12, cursor: 'pointer', background: discountType === 'nominal' ? '#4F46E5' : '#EEF2FF', color: discountType === 'nominal' ? 'white' : '#4F46E5' }}>
                    Rp Nominal
                  </button>
                </div>
                <input type="number" value={discountInput} min={0} max={discountType === 'percent' ? 100 : subtotal}
                  onChange={e => setDiscountInput(e.target.value)}
                  placeholder={discountType === 'percent' ? 'Contoh: 10 (untuk 10%)' : 'Contoh: 5000'}
                  style={{ width: '100%', padding: '8px 12px', border: '1.5px solid #C7D2FE', borderRadius: 8, fontSize: 13, outline: 'none', boxSizing: 'border-box', background: 'white' }} />
                {discountAmt > 0 && (
                  <div style={{ marginTop: 6, fontSize: 12, color: '#10B981', fontWeight: 600 }}>✓ Hemat Rp {discountAmt.toLocaleString('id-ID')}</div>
                )}
              </div>

              {/* Ringkasan Harga */}
              <div style={{ background: '#F8F9FF', borderRadius: 12, padding: '10px 14px', marginBottom: 14 }}>
                {discountAmt > 0 && (
                  <>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, color: '#6B7280', marginBottom: 4 }}>
                      <span>Subtotal</span><span>Rp {subtotal.toLocaleString('id-ID')}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, color: '#EF4444', marginBottom: 4 }}>
                      <span>{discountLabel}</span><span>-Rp {discountAmt.toLocaleString('id-ID')}</span>
                    </div>
                  </>
                )}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: discountAmt > 0 ? 8 : 0, borderTop: discountAmt > 0 ? '1px dashed #C7D2FE' : 'none' }}>
                  <span style={{ fontWeight: 800, color: '#1F2937', fontSize: '1rem' }}>Total</span>
                  <span style={{ fontWeight: 800, color: '#4F46E5', fontSize: '1.2rem' }}>Rp {Math.max(0, total).toLocaleString('id-ID')}</span>
                </div>
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
              <h2 style={{ margin: 0, fontSize: '1.2rem' }}>💳 Pembayaran</h2>
              <button onClick={() => setPayModal(false)} style={{ background: '#F3F4F6', border: 'none', borderRadius: '8px', padding: '6px', cursor: 'pointer' }}><X size={18} /></button>
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '12px' }}>
              <button style={{ ...S.pill(payMethod === 'CASH'), padding: '10px 14px', fontSize: '0.85rem' }} onClick={() => setPayMethod('CASH')}><CreditCard size={14} style={{ marginRight: 4 }} />Cash</button>
              <button style={{ ...S.pill(payMethod === 'QRIS'), padding: '10px 14px', fontSize: '0.85rem' }} onClick={() => setPayMethod('QRIS')}><QrCode size={14} style={{ marginRight: 4 }} />QRIS Manual</button>
              <button style={{ ...S.pill(payMethod === 'MIDTRANS_SNAP'), padding: '10px 14px', fontSize: '0.85rem' }} onClick={() => setPayMethod('MIDTRANS_SNAP')}><QrCode size={14} style={{ marginRight: 4 }} />QRIS Midtrans</button>
              <button style={{ ...S.pill(payMethod === 'TRANSFER'), padding: '10px 14px', fontSize: '0.85rem' }} onClick={() => setPayMethod('TRANSFER')}>🏦 TF</button>
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px', fontSize: '0.85rem', color: '#6B7280', cursor: 'pointer' }}>
              <input type="checkbox" checked={isBackdate} onChange={e => setIsBackdate(e.target.checked)} />
              Backdate (transaksi lampau)
            </label>
            {isBackdate && <input type="datetime-local" value={txDate} onChange={e => setTxDate(e.target.value)} style={{ ...S.input, marginBottom: '12px' }} />}
            {/* Summary */}
            <div style={{ background: '#F8F9FF', borderRadius: 12, padding: '12px 14px', marginBottom: 12 }}>
              {discountAmt > 0 && (
                <>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, color: '#6B7280', marginBottom: 3 }}>
                    <span>Subtotal</span><span>Rp {subtotal.toLocaleString('id-ID')}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, color: '#EF4444', marginBottom: 3 }}>
                    <span>{discountLabel}</span><span>-Rp {discountAmt.toLocaleString('id-ID')}</span>
                  </div>
                </>
              )}
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: discountAmt > 0 ? 6 : 0, borderTop: discountAmt > 0 ? '1px dashed #C7D2FE' : 'none' }}>
                <span style={{ fontWeight: 700 }}>Total Tagihan</span>
                <span style={{ fontWeight: 800, color: '#4F46E5', fontSize: '1.1rem' }}>Rp {Math.max(0, total).toLocaleString('id-ID')}</span>
              </div>
            </div>
            {/* Uang Dibayar (untuk Cash) */}
            {payMethod === 'CASH' && (
              <div style={{ marginBottom: 12 }}>
                <label style={{ fontSize: 13, fontWeight: 600, color: '#374151', display: 'block', marginBottom: 4 }}>Uang Dibayar</label>
                <input type="number" value={amountPaid} onChange={e => setAmountPaid(e.target.value)}
                  placeholder={`Min. Rp ${total.toLocaleString('id-ID')}`}
                  style={{ ...S.input, textAlign: 'right' }} />
                {parseFloat(amountPaid) >= total && amountPaid && (
                  <div style={{ marginTop: 6, fontSize: 13, fontWeight: 700, color: '#10B981' }}>Kembalian: Rp {change.toLocaleString('id-ID')}</div>
                )}
                {parseFloat(amountPaid) < total && amountPaid && (
                  <div style={{ marginTop: 6, fontSize: 12, color: '#EF4444' }}>⚠️ Uang kurang Rp {(total - parseFloat(amountPaid)).toLocaleString('id-ID')}</div>
                )}
              </div>
            )}
            <button style={S.btnPrimary} onClick={() => checkout(false)}>Proses Pembayaran</button>
          </div>
        </div>
      )}

      {/* Midtrans QRIS Modal */}
      {((midtransActiveTx && midtransActiveTx.paymentMethod === 'QRIS_MIDTRANS') || isChargingMidtrans) && (
        <div className="modal-overlay" style={{ zIndex: 110 }}>
          <div className="modal-content glass-panel text-center" style={{ maxWidth: 400, padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 800, color: '#1E293B' }}>QRIS Midtrans</h3>
              {midtransStatus === 'PENDING' && !isChargingMidtrans && (
                <button onClick={cancelMidtransPayment} style={{ background: '#FEE2E2', border: 'none', borderRadius: '8px', padding: '6px', cursor: 'pointer', color: '#EF4444' }}><X size={18} /></button>
              )}
            </div>

            {isChargingMidtrans ? (
              <div style={{ padding: '40px 0' }}>
                <div className="spinner" style={{ border: '4px solid #F3F4F6', borderTop: '4px solid #4F46E5', borderRadius: '50%', width: 40, height: 40, animation: 'spin 1s linear infinite', margin: '0 auto 16px' }} />
                <p style={{ color: '#4B5563', fontWeight: 600 }}>Menghubungkan ke Midtrans...</p>
              </div>
            ) : (
              <div>
                <div style={{ background: '#F3F4F6', borderRadius: '12px', padding: '10px 14px', marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ textAlign: 'left' }}>
                    <span style={{ fontSize: '0.75rem', color: '#6B7280', display: 'block' }}>Invoice</span>
                    <strong style={{ fontSize: '0.85rem', color: '#374151' }}>{midtransActiveTx?.receiptNumber}</strong>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <span style={{ fontSize: '0.75rem', color: '#6B7280', display: 'block' }}>Total Tagihan</span>
                    <strong style={{ fontSize: '1.1rem', color: '#4F46E5', fontWeight: 800 }}>Rp {midtransActiveTx?.total?.toLocaleString('id-ID')}</strong>
                  </div>
                </div>

                {midtransStatus === 'PENDING' && (
                  <>
                    <div style={{ background: 'white', border: '1px solid #E5E7EB', borderRadius: '16px', padding: '12px', display: 'inline-block', marginBottom: '16px', boxShadow: '0 4px 10px rgba(0,0,0,0.05)' }}>
                      {midtransQrUrl ? (
                        <img src={midtransQrUrl} alt="QRIS Midtrans" style={{ width: '220px', height: '220px', display: 'block' }} />
                      ) : (
                        <div style={{ width: '220px', height: '220px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#F9FAFB' }}>
                          <span style={{ color: '#9CA3AF', fontSize: '0.85rem' }}>Gagal memuat QR Code</span>
                        </div>
                      )}
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginBottom: '16px' }}>
                      <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#F59E0B', animation: 'pulse 1.5s infinite' }} />
                      <span style={{ fontSize: '0.85rem', color: '#D97706', fontWeight: 700 }}>Menunggu Pembayaran...</span>
                    </div>

                    <p style={{ fontSize: '0.8rem', color: '#6B7280', margin: '0 0 20px', lineHeight: '1.4' }}>
                      Pindai QRIS di atas dengan GoPay, OVO, Dana, LinkAja, ShopeePay, atau Mobile Banking Anda untuk menyelesaikan pembayaran.
                    </p>

                    <div style={{ display: 'flex', gap: '10px' }}>
                      <button style={{ ...S.btnPrimary, flex: 1, padding: '10px 14px', fontSize: '0.85rem' }} onClick={() => {
                        api.get(`/midtrans/status/${midtransOrderId}`).then(res => {
                          if (res.data.status === 'SUCCESS') {
                            setMidtransStatus('SUCCESS');
                            if (midtransActiveTx.queueNumber) {
                              setQueueToPrint({ ...midtransActiveTx, paymentMethod: 'QRIS', status: 'COMPLETED' });
                            } else {
                              setReceipt({ ...midtransActiveTx, paymentMethod: 'QRIS', status: 'COMPLETED' });
                            }
                            setTimeout(() => {
                              setMidtransActiveTx(null);
                              setMidtransQrUrl(null);
                              setMidtransOrderId(null);
                            }, 1000);
                            fetchProducts();
                            fetchActiveQueues();
                          } else {
                            alert('Pembayaran belum terdeteksi. Silakan coba beberapa saat lagi.');
                          }
                        }).catch(() => {
                          alert('Gagal mengecek status pembayaran.');
                        });
                      }}>Cek Status</button>
                      
                      <button style={{ ...S.btnSecondary, flex: 1, padding: '10px 14px', fontSize: '0.85rem', borderColor: '#EF4444', color: '#EF4444' }} onClick={cancelMidtransPayment}>Batal</button>
                    </div>
                  </>
                )}

                {midtransStatus === 'SUCCESS' && (
                  <div style={{ padding: '20px 0' }}>
                    <div style={{ fontSize: '3.5rem', marginBottom: '8px' }}>✅</div>
                    <h3 style={{ color: '#10B981', margin: '0 0 8px' }}>Pembayaran Berhasil!</h3>
                    <p style={{ color: '#6B7280', fontSize: '0.85rem' }}>Transaksi selesai. Struk sedang disiapkan.</p>
                  </div>
                )}

                {midtransStatus === 'CANCELLED' && (
                  <div style={{ padding: '20px 0' }}>
                    <div style={{ fontSize: '3.5rem', marginBottom: '8px' }}>❌</div>
                    <h3 style={{ color: '#EF4444', margin: '0 0 8px' }}>Transaksi Dibatalkan</h3>
                    <p style={{ color: '#6B7280', fontSize: '0.85rem' }}>Stok telah dikembalikan.</p>
                    <button style={{ ...S.btnSecondary, marginTop: '16px' }} onClick={() => { setMidtransActiveTx(null); setMidtransQrUrl(null); setMidtransOrderId(null); }}>Tutup</button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Receipt Modal — Direct Checkout */}
      {receipt && (
        <div className="modal-overlay z-50 backdrop-blur-md bg-black/35 flex items-center justify-center px-4">
          <div className="bg-white rounded-2xl w-full max-w-sm p-6 shadow-xl flex flex-col gap-4 text-center animate-in fade-in zoom-in-95 duration-200">
            <div style={{ fontSize: '3rem', marginBottom: '-5px' }}>✅</div>
            <h2 style={{ color: '#16A34A', margin: '0 0 2px', fontSize: '1.25rem', fontWeight: 900 }}>Pembayaran Berhasil!</h2>
            <p style={{ color: '#6B7280', fontSize: '0.85rem', margin: '0 0 10px', fontWeight: 700 }}>{receipt.receiptNumber}</p>
            
            {/* Paper Size Picker */}
            <div className="flex flex-col gap-1.5 text-left">
              <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Ukuran Kertas Struk</span>
              <div className="grid grid-cols-2 gap-2 bg-gray-50/50 p-1 rounded-xl border border-gray-100">
                {['58mm', '80mm'].map(size => (
                  <button
                    key={size}
                    type="button"
                    onClick={() => setPaperSize(size)}
                    className={`py-2 text-xs font-black text-center transition-all cursor-pointer rounded-lg border-none ${
                      paperSize === size
                        ? 'bg-indigo-600 text-white shadow-sm'
                        : 'bg-white border border-gray-200/50 text-gray-550 hover:bg-gray-50'
                    }`}
                  >
                    Thermal {size}
                  </button>
                ))}
              </div>
            </div>

            {/* Print Triggers */}
            <div className="flex flex-col gap-2.5 mt-2">
              <button
                type="button"
                onClick={() => printViaBluetooth(receipt, lastCart, paperSize)}
                disabled={printingBluetooth}
                className="w-full flex items-center justify-center gap-1.5 py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-black cursor-pointer shadow-md shadow-indigo-600/10 transition-all border-none active:scale-[0.98] disabled:bg-gray-300 disabled:shadow-none"
              >
                <Printer size={15} />
                {printingBluetooth ? 'Menghubungkan...' : 'Hubungkan & Cetak (Bluetooth)'}
              </button>
              <button
                type="button"
                onClick={() => printReceipt(paperSize)}
                className="w-full flex items-center justify-center gap-1.5 py-3 bg-gray-100 hover:bg-gray-250 text-gray-700 rounded-xl text-xs font-bold cursor-pointer transition-all border-none active:scale-[0.98]"
              >
                Cetak Sistem (Browser)
              </button>
              <button
                type="button"
                onClick={() => setReceipt(null)}
                className="w-full py-2.5 text-gray-500 hover:text-gray-800 text-xs font-bold rounded-xl border border-gray-200/50 bg-white cursor-pointer transition-all mt-1"
              >
                Tutup
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Receipt Modal — Queue Payment */}
      {queueToPrint && (
        <div className="modal-overlay z-50 backdrop-blur-md bg-black/35 flex items-center justify-center px-4">
          <div className="bg-white rounded-2xl w-full max-w-sm p-6 shadow-xl flex flex-col gap-4 text-center animate-in fade-in zoom-in-95 duration-200">
            <div style={{ fontSize: '3rem', marginBottom: '-5px' }}>✅</div>
            <h2 style={{ color: '#16A34A', margin: '0 0 2px', fontSize: '1.25rem', fontWeight: 900 }}>Pembayaran Antrian Sukses!</h2>
            <p style={{ color: '#6B7280', fontSize: '0.85rem', margin: '0 0 2px', fontWeight: 700 }}>
              {queueToPrint.queueNumber && <><b>Antrian #{queueToPrint.queueNumber}</b> &middot; </>}{queueToPrint.receiptNumber}
            </p>
            <p style={{ color: '#6B7280', fontSize: '0.78rem', margin: '0 0 10px', fontWeight: 600 }}>Metode: {queueToPrint.paymentMethod}</p>

            {/* Paper Size Picker */}
            <div className="flex flex-col gap-1.5 text-left">
              <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Ukuran Kertas Struk</span>
              <div className="grid grid-cols-2 gap-2 bg-gray-50/50 p-1 rounded-xl border border-gray-100">
                {['58mm', '80mm'].map(size => (
                  <button
                    key={size}
                    type="button"
                    onClick={() => setPaperSize(size)}
                    className={`py-2 text-xs font-black text-center transition-all cursor-pointer rounded-lg border-none ${
                      paperSize === size
                        ? 'bg-indigo-600 text-white shadow-sm'
                        : 'bg-white border border-gray-200/50 text-gray-550 hover:bg-gray-50'
                    }`}
                  >
                    Thermal {size}
                  </button>
                ))}
              </div>
            </div>

            {/* Print Triggers */}
            <div className="flex flex-col gap-2.5 mt-2">
              <button
                type="button"
                onClick={() => printViaBluetooth(queueToPrint, null, paperSize)}
                disabled={printingBluetooth}
                className="w-full flex items-center justify-center gap-1.5 py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-black cursor-pointer shadow-md shadow-indigo-600/10 transition-all border-none active:scale-[0.98] disabled:bg-gray-300 disabled:shadow-none"
              >
                <Printer size={15} />
                {printingBluetooth ? 'Menghubungkan...' : 'Hubungkan & Cetak (Bluetooth)'}
              </button>
              <button
                type="button"
                onClick={() => printQueueReceipt(queueToPrint, paperSize)}
                className="w-full flex items-center justify-center gap-1.5 py-3 bg-gray-100 hover:bg-gray-250 text-gray-700 rounded-xl text-xs font-bold cursor-pointer transition-all border-none active:scale-[0.98]"
              >
                Cetak Sistem (Browser)
              </button>
              <button
                type="button"
                onClick={() => setQueueToPrint(null)}
                className="w-full py-2.5 text-gray-500 hover:text-gray-800 text-xs font-bold rounded-xl border border-gray-200/50 bg-white cursor-pointer transition-all mt-1"
              >
                Tutup
              </button>
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
                        <button style={{ ...S.pill(true), padding: '8px 12px', fontSize: '0.8rem', flex: 1 }} onClick={() => payQueue(t.id, 'QRIS')}>QRIS Manual</button>
                        <button style={{ ...S.pill(true), padding: '8px 12px', fontSize: '0.8rem', flex: 1, background: '#4F46E5', color: 'white' }} onClick={() => payQueue(t.id, 'MIDTRANS_SNAP')}>QRIS Midtrans</button>
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
