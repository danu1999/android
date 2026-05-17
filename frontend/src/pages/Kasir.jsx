import React, { useState, useEffect } from 'react';
import { Search, ShoppingCart, Trash2, CreditCard, QrCode, Printer, X, ChevronUp, ClipboardList, Plus, Minus } from 'lucide-react';
import api from '../api';

const getEffectivePrice = (product, quantity) => {
  if (!product.wholesaleEnabled || !product.wholesalePrices) return product.price;
  try {
    const tiers = typeof product.wholesalePrices === 'string' ? JSON.parse(product.wholesalePrices) : product.wholesalePrices;
    const applicable = tiers.filter(t => quantity >= t.minQty).sort((a, b) => b.minQty - a.minQty);
    return applicable.length > 0 ? applicable[0].price : product.price;
  } catch { return product.price; }
};

export default function Kasir() {
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
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

  useEffect(() => { fetchProducts(); }, []);

  const fetchProducts = async () => {
    try { const r = await api.get('/products'); setProducts(r.data); } catch { }
  };

  const addToCart = (p) => {
    if (p.stock < 1) { alert('Stok habis!'); return; }
    setCart(prev => {
      const ex = prev.find(i => i.product.id === p.id);
      if (ex) {
        if (ex.quantity >= p.stock) { alert('Stok tidak cukup!'); return prev; }
        return prev.map(i => i.product.id === p.id ? { ...i, quantity: i.quantity + 1 } : i);
      }
      return [...prev, { product: p, quantity: 1, discount: 0 }];
    });
  };

  const updateQty = (id, delta) => setCart(prev => prev.map(i => {
    if (i.product.id !== id) return i;
    const nq = i.quantity + delta;
    if (nq < 1) return i;
    if (nq > i.product.stock) return i;
    return { ...i, quantity: nq };
  }));

  const removeItem = (id) => setCart(prev => prev.filter(i => i.product.id !== id));

  const total = cart.reduce((s, i) => s + (getEffectivePrice(i.product, i.quantity) - i.discount) * i.quantity, 0) - globalDiscount;
  const totalItems = cart.reduce((s, i) => s + i.quantity, 0);

  const checkout = async (isQueue = false) => {
    if (!cart.length) return;
    try {
      const r = await api.post('/transactions', {
        items: cart.map(i => ({ productId: i.product.id, quantity: i.quantity, price: i.product.price, discount: i.discount })),
        total, discount: globalDiscount,
        paymentMethod: isQueue ? 'PENDING' : payMethod,
        status: isQueue ? 'PENDING' : 'COMPLETED',
        notes, customerName,
        type: isBackdate ? 'BACKDATE' : 'SALES',
        date: isBackdate ? txDate : undefined
      });
      if (!isQueue) setReceipt(r.data);
      else alert('Ditambahkan ke antrian!');
      setCart([]); setGlobalDiscount(0); setCustomerName(''); setNotes('');
      setPayModal(false); setCartOpen(false); fetchProducts();
    } catch { alert('Transaksi gagal'); }
  };

  const fetchQueue = async () => {
    try {
      const r = await api.get('/transactions');
      setQueues(r.data.filter(t => t.status === 'PENDING'));
      setQueueModal(true);
    } catch { }
  };

  const payQueue = async (id, method) => {
    try {
      await api.put(`/transactions/${id}`, { status: 'COMPLETED', paymentMethod: method });
      alert('Pembayaran berhasil!'); setQueueModal(false);
    } catch { alert('Gagal'); }
  };

  const printReceipt = (size) => {
    const w = window.open('', '_blank');
    w.document.write(`<html><head><style>body{font-family:monospace;width:${size};margin:0 auto;padding:10px;font-size:12px}.c{text-align:center}.b{font-weight:bold}.r{text-align:right}hr{border-top:1px dashed #000}</style></head><body>
      <div class="c b" style="font-size:16px">POSBah</div><div class="c">Struk Pembayaran</div><hr>
      <div>No: ${receipt?.receiptNumber}</div><div>Metode: ${receipt?.paymentMethod}</div><hr>
      <table width="100%">${receipt?.items.map(item => {
      const prod = products.find(p => p.id === item.productId);
      return `<tr><td colspan="3">${prod?.name || 'Item'}</td></tr><tr><td>${item.quantity}x</td><td>Rp${item.price}</td><td class="r">Rp${item.quantity * item.price}</td></tr>`;
    }).join('')}</table><hr>
      <table width="100%"><tr><td class="b">TOTAL</td><td class="r b">Rp${receipt?.total}</td></tr></table><hr>
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
    grid: { display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: '8px', padding: '12px 12px 160px', overflowY: 'auto', flex: 1 },
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
        <button style={S.queueBtn} onClick={fetchQueue}><ClipboardList size={16} /> Antrian</button>
      </div>

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

            {/* Badge Stok (Tetap dipertahankan di pojok kanan atas gambar karena penting untuk POS) */}
            {p.stock > 0 && p.stock <= 5 && (
              <div style={{ position: 'absolute', top: 8, right: 8, background: '#F59E0B', color: 'white', padding: '2px 8px', borderRadius: 6, fontSize: '10px', fontWeight: 700, zIndex: 10 }}>Sisa {p.stock}</div>
            )}
            {p.stock === 0 && (
              <div style={{ position: 'absolute', top: 8, right: 8, background: '#EF4444', color: 'white', padding: '2px 8px', borderRadius: 6, fontSize: '10px', fontWeight: 700, zIndex: 10 }}>HABIS</div>
            )}

            {/* Gambar Produk (Menggunakan background-image agar otomatis terpotong rapi/cover) */}
            <div
              className="card__image"
              style={p.image ? { backgroundImage: `url(${p.image})` } : { backgroundColor: '#E5E7EB' }}
            >
              {!p.image && <span style={{ fontSize: '2rem' }}>🛒</span>}
            </div>

            {/* Info Produk */}
            <div className="card__content">
              <div className="card__text">
                <p className="card__title">{p.name}</p>
              </div>

              {/* Harga & Keterangan Tambahan (Varian/Stok) */}
              <div className="card__footer">
                <div className="card__price">Rp{p.price.toLocaleString('id-ID')}</div>

                {/* Anda bisa mengganti tulisan ini menjadi "1 Varian" persis seperti di foto, 
                    tapi karena ini kasir, menampilkan sisa stok akan sangat membantu */}
                <div className="card__variant">{p.stock} {p.unit || 'pcs'}</div>
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
                const ep = getEffectivePrice(item.product, item.quantity);
                return (
                  <div key={item.product.id} style={{ borderBottom: '1px solid #F3F4F6', padding: '12px 0', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 600, fontSize: '0.9rem', color: '#1F2937' }}>{item.product.name}</div>
                        <div style={{ fontSize: '0.8rem', color: '#4F46E5', fontWeight: 700 }}>Rp {ep.toLocaleString('id-ID')}{item.discount > 0 && <span style={{ color: '#EF4444' }}> -Rp {item.discount.toLocaleString('id-ID')}</span>}</div>
                      </div>
                      <button onClick={() => removeItem(item.product.id)} style={{ background: '#FEE2E2', border: 'none', borderRadius: '8px', padding: '6px', cursor: 'pointer', color: '#EF4444', display: 'flex', alignItems: 'center' }}>
                        <Trash2 size={14} />
                      </button>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <span style={{ fontSize: '0.72rem', color: '#9CA3AF' }}>Diskon:</span>
                        <input type="number" value={item.discount} min={0}
                          onChange={e => setCart(prev => prev.map(i => i.product.id === item.product.id ? { ...i, discount: Number(e.target.value) || 0 } : i))}
                          style={{ width: '60px', padding: '4px 6px', border: '1px solid #E5E7EB', borderRadius: '7px', fontSize: '0.8rem', outline: 'none', textAlign: 'right' }} />
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0', border: '1.5px solid #E5E7EB', borderRadius: '10px', overflow: 'hidden' }}>
                        <button onClick={() => updateQty(item.product.id, -1)} style={{ padding: '6px 12px', border: 'none', background: '#F9FAFB', cursor: 'pointer', fontWeight: 800, color: '#374151', fontSize: '1rem' }}>−</button>
                        <span style={{ padding: '6px 12px', fontWeight: 700, fontSize: '0.95rem', color: '#1F2937', minWidth: '36px', textAlign: 'center' }}>{item.quantity}</span>
                        <button onClick={() => updateQty(item.product.id, 1)} style={{ padding: '6px 12px', border: 'none', background: '#F9FAFB', cursor: 'pointer', fontWeight: 800, color: '#4F46E5', fontSize: '1rem' }}>+</button>
                      </div>
                    </div>
                  </div>
                );
              })
            )}

            {/* Summary section */}
            <div style={{ paddingTop: '12px' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '12px' }}>
                <input style={S.input} placeholder="Nama / No. Meja (opsional)" value={customerName} onChange={e => setCustomerName(e.target.value)} />
                <textarea style={{ ...S.input, resize: 'none', fontFamily: 'inherit' }} rows={2} placeholder="Catatan pesanan (opsional)" value={notes} onChange={e => setNotes(e.target.value)} />
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

      {/* Receipt Modal */}
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
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button style={{ ...S.pill(true), padding: '8px 16px', fontSize: '0.8rem', flex: 1 }} onClick={() => payQueue(t.id, 'CASH')}>Cash</button>
                      <button style={{ ...S.pill(true), padding: '8px 16px', fontSize: '0.8rem', flex: 1 }} onClick={() => payQueue(t.id, 'QRIS')}>QRIS</button>
                    </div>
                  </div>
                ))
              }
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
