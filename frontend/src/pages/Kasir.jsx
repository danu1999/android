import React, { useState, useEffect, useRef } from 'react';
import { Search, ShoppingCart, Trash2, CreditCard, QrCode, Printer, X, ChevronUp, ClipboardList } from 'lucide-react';
import api from '../api';

export default function Kasir() {
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [receiptData, setReceiptData] = useState(null);
  const [isBackdate, setIsBackdate] = useState(false);
  const [transactionDate, setTransactionDate] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [notes, setNotes] = useState('');
  const [isQueueModalOpen, setIsQueueModalOpen] = useState(false);
  const [queuedTransactions, setQueuedTransactions] = useState([]);
  const [isMobileCartOpen, setIsMobileCartOpen] = useState(false);

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    try {
      const res = await api.get('/products');
      setProducts(res.data);
    } catch (err) {
      console.error('Failed to fetch products', err);
    }
  };

  const addToCart = (product) => {
    const existing = cart.find(item => item.product.id === product.id);
    if (existing) {
      if (existing.quantity >= product.stock) { alert('Stok tidak cukup!'); return; }
      setCart(cart.map(item => item.product.id === product.id ? { ...item, quantity: item.quantity + 1 } : item));
    } else {
      if (product.stock < 1) { alert('Stok habis!'); return; }
      setCart([...cart, { product, quantity: 1, discount: 0 }]);
    }
  };

  const removeFromCart = (productId) => setCart(cart.filter(item => item.product.id !== productId));

  const updateQuantity = (productId, delta) => {
    setCart(cart.map(item => {
      if (item.product.id === productId) {
        const newQty = item.quantity + delta;
        if (newQty > 0 && newQty <= item.product.stock) return { ...item, quantity: newQty };
      }
      return item;
    }));
  };

  const getEffectivePrice = (product, quantity) => {
    if (!product.wholesaleEnabled || !product.wholesalePrices) return product.price;
    try {
      const tiers = typeof product.wholesalePrices === 'string' ? JSON.parse(product.wholesalePrices) : product.wholesalePrices;
      const applicable = tiers.filter(t => quantity >= t.minQty).sort((a, b) => b.minQty - a.minQty);
      return applicable.length > 0 ? applicable[0].price : product.price;
    } catch (_) { return product.price; }
  };

  const [globalDiscount, setGlobalDiscount] = useState(0);
  const totalAmount = cart.reduce((sum, item) => {
    const effectivePrice = getEffectivePrice(item.product, item.quantity);
    return sum + ((effectivePrice - item.discount) * item.quantity);
  }, 0) - globalDiscount;

  const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);

  const updateItemDiscount = (productId, discount) => {
    setCart(cart.map(item => item.product.id === productId ? { ...item, discount: Number(discount) || 0 } : item));
  };

  const handleCheckout = async (isQueue = false) => {
    try {
      const transactionData = {
        items: cart.map(item => ({
          productId: item.product.id,
          quantity: item.quantity,
          price: item.product.price,
          discount: item.discount
        })),
        total: totalAmount,
        discount: globalDiscount,
        paymentMethod: isQueue ? 'PENDING' : paymentMethod,
        status: isQueue ? 'PENDING' : 'COMPLETED',
        notes,
        customerName,
        type: isBackdate ? 'BACKDATE' : 'SALES',
        date: isBackdate ? transactionDate : undefined
      };
      const res = await api.post('/transactions', transactionData);
      if (!isQueue) {
        setReceiptData(res.data);
      } else {
        alert('Berhasil ditambahkan ke antrian!');
      }
      setCart([]);
      setGlobalDiscount(0);
      setCustomerName('');
      setNotes('');
      setIsPaymentModalOpen(false);
      setIsMobileCartOpen(false);
      fetchProducts();
    } catch (err) {
      console.error('Checkout failed', err);
      alert('Gagal memproses transaksi');
    }
  };

  const fetchQueue = async () => {
    try {
      const res = await api.get('/transactions');
      setQueuedTransactions(res.data.filter(t => t.status === 'PENDING'));
      setIsQueueModalOpen(true);
    } catch(err) { console.error('Failed to fetch queue', err); }
  };

  const payQueue = async (id, method) => {
    try {
      await api.put(`/transactions/${id}`, { status: 'COMPLETED', paymentMethod: method });
      alert('Pembayaran antrian berhasil!');
      setIsQueueModalOpen(false);
    } catch(err) { alert('Gagal memproses pembayaran antrian'); }
  };

  const printReceipt = (size) => {
    const printWindow = window.open('', '_blank');
    const width = size === '58mm' ? '58mm' : '80mm';
    printWindow.document.write(`
      <html><head><style>
        body { font-family: monospace; width: ${width}; margin: 0 auto; padding: 10px; font-size: 12px; }
        .text-center { text-align: center; } .bold { font-weight: bold; }
        table { width: 100%; font-size: 12px; border-collapse: collapse; margin-top: 10px; }
        th, td { text-align: left; padding: 4px 0; } .right { text-align: right; }
        .divider { border-top: 1px dashed #000; margin: 5px 0; }
      </style></head><body>
        <div class="text-center bold" style="font-size: 16px;">POSBah</div>
        <div class="text-center">Struk Pembayaran</div>
        <div class="divider"></div>
        <div>No: ${receiptData?.receiptNumber}</div>
        <div>Metode: ${receiptData?.paymentMethod}</div>
        <div class="divider"></div>
        <table>${receiptData?.items.map(item => {
          const prod = products.find(p => p.id === item.productId);
          return `<tr><td colspan="3">${prod?.name || 'Item'}</td></tr>
            <tr><td>${item.quantity} x</td><td>Rp ${item.price}</td><td class="right">Rp ${item.quantity * item.price}</td></tr>
            ${item.discount > 0 ? `<tr><td colspan="2">Diskon Item</td><td class="right">-Rp ${item.discount * item.quantity}</td></tr>` : ''}`;
        }).join('')}</table>
        <div class="divider"></div>
        <table>
          ${receiptData?.discount > 0 ? `<tr><td>Diskon Trx</td><td class="right">-Rp ${receiptData.discount}</td></tr>` : ''}
          <tr><td class="bold">TOTAL</td><td class="right bold">Rp ${receiptData?.total}</td></tr>
        </table>
        <div class="divider"></div>
        <div class="text-center">Terima Kasih</div>
        <script>window.print(); window.onafterprint = function() { window.close(); }</script>
      </body></html>
    `);
    printWindow.document.close();
  };

  const filteredProducts = products.filter(p => p.name.toLowerCase().includes(searchQuery.toLowerCase()));

  // Shared cart content (reused in both desktop sidebar and mobile sheet)
  const CartContent = () => (
    <>
      <div className="cart-items" style={{ flex: 1, overflowY: 'auto', maxHeight: isMobileCartOpen ? '40vh' : undefined }}>
        {cart.length === 0 ? (
          <div className="text-center text-gray-500 mt-4 text-sm">Keranjang kosong</div>
        ) : (
          cart.map(item => (
            <div key={item.product.id} className="cart-item border-b pb-2 mb-2">
              <div className="cart-item-info">
                <div className="cart-item-name text-sm">{item.product.name}</div>
                <div className="cart-item-price text-sm">Rp {item.product.price.toLocaleString('id-ID')}</div>
              </div>
              <div className="flex justify-between items-center mt-1">
                <div className="flex items-center gap-1">
                  <span className="text-xs text-gray-500">Diskon:</span>
                  <input type="number" className="w-16 p-1 text-xs border rounded outline-none" value={item.discount} onChange={(e) => updateItemDiscount(item.product.id, e.target.value)} />
                </div>
                <div className="cart-item-actions">
                  <button onClick={() => updateQuantity(item.product.id, -1)}>-</button>
                  <span>{item.quantity}</span>
                  <button onClick={() => updateQuantity(item.product.id, 1)}>+</button>
                  <button className="btn-icon text-red-500" onClick={() => removeFromCart(item.product.id)}><Trash2 size={14} /></button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      <div className="cart-summary" style={{ marginTop: 'auto' }}>
        <div className="flex flex-col gap-2 mb-3 border-t pt-3">
          <input type="text" placeholder="Nama / No. Meja (opsional)" className="p-2 border rounded outline-none text-sm w-full" value={customerName} onChange={e => setCustomerName(e.target.value)} />
          <textarea placeholder="Catatan Pesanan (opsional)" className="p-2 border rounded outline-none text-xs w-full" rows={2} value={notes} onChange={e => setNotes(e.target.value)} />
        </div>
        <div className="flex justify-between items-center mb-2 text-sm">
          <span>Diskon Transaksi:</span>
          <input type="number" className="w-24 p-1 border rounded outline-none text-right" value={globalDiscount} onChange={(e) => setGlobalDiscount(Number(e.target.value) || 0)} />
        </div>
        <div className="summary-row font-bold text-xl pt-2 border-t">
          <span>Total</span>
          <span>Rp {totalAmount > 0 ? totalAmount.toLocaleString('id-ID') : 0}</span>
        </div>
        <div className="flex flex-col gap-2 mt-3">
          <button className="btn btn-secondary w-full justify-center py-2 text-sm" disabled={cart.length === 0} onClick={() => handleCheckout(true)}>
            Tambahkan Antrian (Bayar Nanti)
          </button>
          <button className="btn btn-primary w-full justify-center py-3 text-base" disabled={cart.length === 0} onClick={() => setIsPaymentModalOpen(true)}>
            Bayar Sekarang
          </button>
        </div>
      </div>
    </>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', position: 'relative' }}>

      {/* ===== DESKTOP LAYOUT ===== */}
      <div className="kasir-layout" style={{ flex: 1, overflow: 'hidden' }}>
        {/* Left: Products */}
        <div className="kasir-main">
          {/* Top bar */}
          <div style={{ display: 'flex', gap: '8px', marginBottom: '1rem' }}>
            <div className="glass-panel search-bar" style={{ flex: 1, marginBottom: 0 }}>
              <Search size={20} className="text-gray-400" />
              <input type="text" placeholder="Cari produk..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />
            </div>
            <button className="btn btn-secondary" style={{ whiteSpace: 'nowrap' }} onClick={fetchQueue}>
              <ClipboardList size={18} /> Antrian
            </button>
          </div>

          {/* Low Stock Alert */}
          {products.filter(p => p.stock > 0 && p.stock <= 5).length > 0 && (
            <div style={{ background: '#FEF9C3', border: '1px solid #FDE047', borderRadius: 10, padding: '8px 14px', marginBottom: 10, fontSize: 13, color: '#854D0E', fontWeight: 600 }}>
              ⚠️ Stok menipis: {products.filter(p => p.stock > 0 && p.stock <= 5).map(p => `${p.name} (${p.stock})`).join(' · ')}
            </div>
          )}

          {/* Product Grid — with extra bottom padding on mobile for the floating bar */}
          <div className="product-grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4" style={{ paddingBottom: '5rem' }}>
            {filteredProducts.map(product => (
              <div key={product.id} className="product-card glass-panel flex flex-col" onClick={() => addToCart(product)} style={{ position: 'relative' }}>
                {product.stock > 0 && product.stock <= 5 && (
                  <div style={{ position: 'absolute', top: 6, right: 6, background: '#F59E0B', color: '#fff', fontSize: 10, fontWeight: 800, padding: '2px 6px', borderRadius: 99, zIndex: 1 }}>
                    Sisa {product.stock}
                  </div>
                )}
                <div className="product-image h-24 md:h-32">
                  {product.image ? (
                    <img src={product.image} alt={product.name} className="w-full h-full object-contain p-2" />
                  ) : (
                    <div className="image-placeholder">No Image</div>
                  )}
                </div>
                <div className="product-info p-2 md:p-4 flex-1 flex flex-col">
                  <div className="product-name text-sm md:text-base">{product.name}</div>
                  <div className="product-price text-sm md:text-base">Rp {product.price.toLocaleString('id-ID')}</div>
                  <div className="product-stock text-xs mt-auto">Stok: {product.stock} {product.unit || 'pcs'}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right: Desktop Sidebar Cart */}
        <div className="kasir-sidebar glass-panel" style={{ overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
          <div className="cart-header">
            <h2><ShoppingCart size={20} /> Keranjang</h2>
            {cart.length > 0 && (
              <button className="btn-icon btn-danger" onClick={() => setCart([])} title="Kosongkan"><Trash2 size={16} /></button>
            )}
          </div>
          <CartContent />
        </div>
      </div>

      {/* ===== MOBILE FLOATING CART BAR (bottom) ===== */}
      <div
        className="md:hidden"
        style={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          zIndex: 60,
        }}
      >
        {/* Backdrop when sheet is open */}
        {isMobileCartOpen && (
          <div
            onClick={() => setIsMobileCartOpen(false)}
            style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', zIndex: -1, backdropFilter: 'blur(2px)' }}
          />
        )}

        {/* Bottom Sheet */}
        <div
          style={{
            background: 'white',
            borderRadius: isMobileCartOpen ? '20px 20px 0 0' : '16px 16px 0 0',
            boxShadow: '0 -4px 30px rgba(0,0,0,0.15)',
            padding: '0 1rem 1rem',
            transition: 'max-height 0.35s cubic-bezier(0.4,0,0.2,1)',
            maxHeight: isMobileCartOpen ? '85dvh' : '64px',
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          {/* Handle / toggle bar */}
          <button
            onClick={() => setIsMobileCartOpen(!isMobileCartOpen)}
            style={{
              width: '100%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '12px 4px',
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <ShoppingCart size={22} color="#4F46E5" />
              <span style={{ fontWeight: 700, fontSize: '1rem', color: '#1F2937' }}>
                Keranjang {totalItems > 0 ? `(${totalItems})` : ''}
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              {totalAmount > 0 && (
                <span style={{ fontWeight: 700, color: '#4F46E5', fontSize: '0.95rem' }}>
                  Rp {totalAmount.toLocaleString('id-ID')}
                </span>
              )}
              <ChevronUp size={22} color="#6B7280" style={{ transform: isMobileCartOpen ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.3s' }} />
            </div>
          </button>

          {/* Cart content inside sheet */}
          <div style={{ overflowY: 'auto', flex: 1, paddingBottom: '1rem' }}>
            <CartContent />
          </div>
        </div>
      </div>

      {/* ===== MODALS ===== */}

      {/* Payment Modal */}
      {isPaymentModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel text-center">
            <h2>Pilih Metode Pembayaran</h2>
            <div className="text-left mt-4 mb-2 p-3 border rounded bg-gray-50 flex flex-col gap-2">
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" checked={isBackdate} onChange={(e) => setIsBackdate(e.target.checked)} />
                <span className="font-semibold text-gray-700">Gunakan Backdate Kasir</span>
              </label>
              {isBackdate && (
                <div className="mt-2">
                  <label className="block text-sm text-gray-600 mb-1">Pilih Tanggal Transaksi</label>
                  <input type="datetime-local" className="p-2 border rounded w-full" value={transactionDate} onChange={(e) => setTransactionDate(e.target.value)} />
                </div>
              )}
            </div>
            <div className="payment-options mt-4 flex justify-center gap-4">
              <button className={`btn ${paymentMethod === 'CASH' ? 'btn-primary' : 'btn-secondary'} flex-col p-4 w-32`} onClick={() => setPaymentMethod('CASH')}>
                <CreditCard size={32} className="mb-2" />CASH
              </button>
              <button className={`btn ${paymentMethod === 'QRIS' ? 'btn-primary' : 'btn-secondary'} flex-col p-4 w-32`} onClick={() => setPaymentMethod('QRIS')}>
                <QrCode size={32} className="mb-2" />QRIS
              </button>
            </div>
            {paymentMethod === 'QRIS' && (
              <div className="mt-4 p-4 border rounded-lg bg-white inline-block">
                <div style={{width: 150, height: 150, background: '#ccc', margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>QRIS Mockup</div>
              </div>
            )}
            <div className="summary-row mt-6 text-xl font-bold">
              <span>Total Tagihan:</span>
              <span>Rp {totalAmount.toLocaleString('id-ID')}</span>
            </div>
            <div className="modal-actions justify-center mt-6">
              <button className="btn btn-secondary" onClick={() => setIsPaymentModalOpen(false)}>Batal</button>
              <button className="btn btn-primary px-8" onClick={() => handleCheckout(false)}>Proses Pembayaran</button>
            </div>
          </div>
        </div>
      )}

      {/* Receipt Modal */}
      {receiptData && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel text-center">
            <h2 className="text-green-600 mb-2">Pembayaran Berhasil!</h2>
            <p>No. Transaksi: {receiptData.receiptNumber}</p>
            <div className="mt-6 flex flex-col gap-3">
              <button className="btn btn-primary justify-center" onClick={() => printReceipt('58mm')}><Printer size={18} /> Cetak Struk (58mm)</button>
              <button className="btn btn-primary justify-center" onClick={() => printReceipt('80mm')}><Printer size={18} /> Cetak Struk (80mm)</button>
              <button className="btn btn-secondary justify-center mt-2" onClick={() => setReceiptData(null)}>Tutup</button>
            </div>
          </div>
        </div>
      )}

      {/* Queue Modal */}
      {isQueueModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel" style={{ maxWidth: 600 }}>
            <div className="flex justify-between items-center mb-4">
              <h2>Daftar Antrian</h2>
              <button className="btn btn-secondary" onClick={() => setIsQueueModalOpen(false)}>Tutup</button>
            </div>
            <div className="max-h-96 overflow-y-auto pr-2">
              {queuedTransactions.length === 0 ? (
                <p className="text-center text-gray-500 py-4">Tidak ada antrian.</p>
              ) : (
                queuedTransactions.map(trx => (
                  <div key={trx.id} className="border p-3 rounded mb-2 flex justify-between items-center bg-gray-50">
                    <div>
                      <div className="font-bold">{trx.customerName || trx.receiptNumber}</div>
                      <div className="text-xs text-gray-600">Total: Rp {trx.total.toLocaleString('id-ID')} ({trx.items.length} item)</div>
                      {trx.notes && <div className="text-xs text-blue-600 mt-1">Catatan: {trx.notes}</div>}
                    </div>
                    <div className="flex flex-col md:flex-row gap-2">
                      <button className="btn btn-primary text-xs" onClick={() => payQueue(trx.id, 'CASH')}>Bayar CASH</button>
                      <button className="btn btn-primary text-xs" onClick={() => payQueue(trx.id, 'QRIS')}>Bayar QRIS</button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
