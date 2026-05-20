import React, { useState, useEffect } from 'react';
import { ShoppingBag, Search, X, ZoomIn } from 'lucide-react';
import api from '../api';
import { useDemoBlock } from '../AuthContext';

const parseVariants = (p) => {
  if (!p.variants) return [];
  try {
    const arr = typeof p.variants === 'string' ? JSON.parse(p.variants) : p.variants;
    return Array.isArray(arr) ? arr.map((v, i) => ({ id: v.id ?? i, ...v })) : [];
  } catch { return []; }
};

export default function TokoOnline() {
  const [products, setProducts] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [cart, setCart] = useState([]);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [selectedImage, setSelectedImage] = useState(null);
  const [variantModal, setVariantModal] = useState(null);
  const [waQueueModal, setWaQueueModal] = useState(false);
  const [waQueueNum, setWaQueueNum] = useState('');

  const [lastUpdated, setLastUpdated] = useState(null);
  const [storeWANumber, setStoreWANumber] = useState('6285746135996');
  const [storeName, setStoreName] = useState('Toko');

  // Form checkout WA
  const [buyerName, setBuyerName] = useState('');
  const [buyerAddress, setBuyerAddress] = useState('');
  const [deliveryType, setDeliveryType] = useState('pickup'); // pickup | delivery

  const { isDemo } = useDemoBlock();

  // Produk demo — identik dengan Kasir, Katalog, Keuangan
  const DEMO_PRODUCTS = [
    { id: 'p301', name: 'Pisang Keju Cokelat', price: 15000, costPrice: 9000, stock: 120, unit: 'pcs', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: null, image: '/demo/pisang-keju-coklat.png' },
    { id: 'p302', name: 'Pisang Keju Stroberi', price: 15000, costPrice: 9500, stock: 85,  unit: 'pcs', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: null, image: '/demo/pisang-keju-stroberi.png' },
    { id: 'p303', name: 'Pisang Keju Premium',  price: 20000, costPrice: 11000, stock: 50, unit: 'pcs', wholesaleEnabled: false, wholesalePrices: null,
      variants: JSON.stringify([
        { id: 1, name: 'Keju Melimpah', price: 25000, costPrice: 13000, stock: 30 },
        { id: 2, name: 'Milo Almond',   price: 28000, costPrice: 15000, stock: 20 },
      ]), barcode: null, image: '/demo/pisang-keju-premium.png' },
    { id: 'p304', name: 'Jus Alpukat',  price: 18000, costPrice: 10000, stock: 60,  unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: null, image: '/demo/jus-alpukat.png' },
    { id: 'p305', name: 'Jus Mangga',   price: 15000, costPrice: 8000,  stock: 75,  unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: null, image: '/demo/jus-mangga.png' },
    { id: 'p306', name: 'Es Teh Manis', price: 8000,  costPrice: 3000,  stock: 200, unit: 'cup', wholesaleEnabled: false, wholesalePrices: null, variants: null, barcode: null, image: null },
  ];

  const fetchProducts = async () => {
    if (isDemo) { setProducts(DEMO_PRODUCTS); setLastUpdated(new Date()); return; }
    try {
      const res = await api.get('/products');
      setProducts(res.data);
      setLastUpdated(new Date());
    } catch (err) {
      console.error('Failed to fetch products', err);
    }
  };

  useEffect(() => {
    fetchProducts();

    if (isDemo) return; // demo tidak perlu listener re-fetch

    // Refresh hanya saat tab kembali aktif/difokus (hemat server)
    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') fetchProducts();
    };
    const onFocus = () => fetchProducts();

    document.addEventListener('visibilitychange', onVisibilityChange);
    window.addEventListener('focus', onFocus);

    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange);
      window.removeEventListener('focus', onFocus);
    };
  }, []);

  const filteredProducts = products.filter(p =>
    p.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const addToCart = (product) => {
    const variants = parseVariants(product);
    if (variants.length > 0) { setVariantModal({ ...product, _variants: variants }); return; }
    if (product.stock < 1) { alert('Maaf, stok produk habis.'); return; }
    const key = String(product.id);
    setCart(prev => {
      const ex = prev.find(i => i.cartKey === key);
      if (ex) return prev.map(i => i.cartKey === key ? { ...i, quantity: i.quantity + 1 } : i);
      return [...prev, { cartKey: key, product, variantId: null, variantName: null, variantPrice: null, quantity: 1 }];
    });
  };

  const addVariantToCart = (product, variant) => {
    const stock = variant.stock !== null && variant.stock !== undefined ? variant.stock : product.stock;
    if (stock < 1) { alert('Stok varian ini habis!'); return; }
    const key = `${product.id}-v${variant.id}`;
    setCart(prev => {
      const ex = prev.find(i => i.cartKey === key);
      if (ex) return prev.map(i => i.cartKey === key ? { ...i, quantity: i.quantity + 1 } : i);
      return [...prev, { cartKey: key, product, variantId: variant.id, variantName: variant.name, variantPrice: variant.price || null, quantity: 1 }];
    });
    setVariantModal(null);
  };

  const addOriginalToCart = (product) => {
    if (product.stock < 1) { alert('Stok produk habis!'); return; }
    const key = String(product.id);
    setCart(prev => {
      const ex = prev.find(i => i.cartKey === key);
      if (ex) return prev.map(i => i.cartKey === key ? { ...i, quantity: i.quantity + 1 } : i);
      return [...prev, { cartKey: key, product, variantId: null, variantName: null, variantPrice: null, quantity: 1 }];
    });
    setVariantModal(null);
  };

  const removeFromCart = (cartKey) => setCart(cart.filter(i => i.cartKey !== cartKey));

  const updateQuantity = (cartKey, delta) => {
    setCart(cart.map(item => {
      if (item.cartKey !== cartKey) return item;
      const newQty = item.quantity + delta;
      return newQty > 0 ? { ...item, quantity: newQty } : item;
    }));
  };

  const getItemPrice = (item) => item.variantPrice || item.product.price;
  const cartTotal = cart.reduce((sum, item) => sum + getItemPrice(item) * item.quantity, 0);
  const cartItemsCount = cart.reduce((sum, item) => sum + item.quantity, 0);

  const sendWhatsApp = () => {
    const nama = buyerName.trim() || 'Pelanggan';
    let message = `Halo *${storeName}*, saya *${nama}* ingin memesan:\n\n`;
    cart.forEach((item, index) => {
      const price = getItemPrice(item);
      const variantStr = item.variantName ? ` (${item.variantName})` : '';
      message += `${index + 1}. *${item.product.name}${variantStr}*\n   ${item.quantity} x Rp ${price.toLocaleString('id-ID')} = *Rp ${(item.quantity * price).toLocaleString('id-ID')}*\n`;
    });
    message += `\n💰 *Total: Rp ${cartTotal.toLocaleString('id-ID')}*\n`;
    if (deliveryType === 'delivery') {
      message += `\n🚚 *Pengiriman ke:* ${buyerAddress || '-'}`;
    } else {
      message += `\n🏪 *Ambil di tempat*`;
    }
    message += `\n\nMohon konfirmasi ketersediaan. Terima kasih 🙏`;
    window.open(`https://wa.me/${storeWANumber.replace(/\D/g, '')}?text=${encodeURIComponent(message)}`, '_blank');
    setWaQueueModal(false); setWaQueueNum(''); setBuyerName(''); setBuyerAddress('');
  };

  const checkoutWhatsApp = () => {
    if (cart.length === 0) return;
    setWaQueueModal(true);
  };

  return (
    <div style={{ padding: '0', background: '#F1F5F9', minHeight: '100vh' }}>

      {/* Header */}
      <header style={{
        background: '#fff',
        boxShadow: '0 1px 8px rgba(0,0,0,0.07)',
        padding: '12px 20px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        position: 'sticky',
        top: 0,
        zIndex: 40,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#4F46E5', fontWeight: 800, fontSize: 18 }}>
          <ShoppingBag size={22} />
          <span>POSBah Online</span>
        </div>
        <button
          onClick={() => setIsCartOpen(true)}
          style={{
            position: 'relative',
            background: '#4F46E5',
            color: '#fff',
            border: 'none',
            borderRadius: 12,
            padding: '8px 16px',
            fontWeight: 700,
            fontSize: 14,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: 6,
          }}
        >
          <ShoppingBag size={16} />
          Keranjang
          {cartItemsCount > 0 && (
            <span style={{
              position: 'absolute', top: -8, right: -8,
              background: '#EF4444', color: '#fff',
              width: 20, height: 20, borderRadius: '50%',
              fontSize: 11, fontWeight: 800,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              {cartItemsCount}
            </span>
          )}
        </button>
      </header>

      <div style={{ maxWidth: 960, margin: '0 auto', padding: '24px 16px 60px' }}>

        {/* Title */}
        <div style={{ textAlign: 'center', marginBottom: 28 }}>
          <h1 style={{ fontSize: 26, fontWeight: 900, color: '#1E293B', marginBottom: 8 }}>Katalog Digital Kami</h1>
          <p style={{ color: '#64748B', fontSize: 14 }}>Pilih produk favorit Anda dan pesan langsung via WhatsApp.</p>
          {lastUpdated && (
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, marginTop: 6, background: '#F0FDF4', border: '1px solid #86EFAC', borderRadius: 99, padding: '3px 12px', fontSize: 11, color: '#16A34A', fontWeight: 600 }}>
              <span style={{ width: 7, height: 7, borderRadius: '50%', background: '#22C55E', display: 'inline-block', animation: 'pulse 2s infinite' }} />
              Stok diperbarui {lastUpdated.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' })} · Refresh saat tab aktif
            </div>
          )}
        </div>

        {/* Search */}
        <div style={{
          background: '#fff',
          borderRadius: 16,
          border: '1px solid #E2E8F0',
          padding: '8px 14px',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          maxWidth: 480,
          margin: '0 auto 28px',
          boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
        }}>
          <Search size={18} color="#94A3B8" />
          <input
            type="text"
            placeholder="Cari produk..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ border: 'none', outline: 'none', width: '100%', fontSize: 14, color: '#334155', background: 'transparent' }}
          />
        </div>

        {/* Product Grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))',
          gap: 16,
        }}>
          {filteredProducts.length > 0 ? filteredProducts.map(product => (
            <div key={product.id} style={{
              background: '#fff',
              borderRadius: 16,
              boxShadow: '0 2px 8px rgba(0,0,0,0.07)',
              overflow: 'hidden',
              border: '1px solid #E2E8F0',
              display: 'flex',
              flexDirection: 'column',
              transition: 'transform 0.15s, box-shadow 0.15s',
            }}
              onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-3px)'; e.currentTarget.style.boxShadow = '0 8px 20px rgba(0,0,0,0.12)'; }}
              onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 2px 8px rgba(0,0,0,0.07)'; }}
            >
              {/* Image Box */}
              <div
                onClick={() => product.image && setSelectedImage(product.image)}
                style={{
                  width: '100%',
                  aspectRatio: '1 / 1',
                  background: '#F8FAFC',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  position: 'relative',
                  cursor: product.image ? 'zoom-in' : 'default',
                  overflow: 'hidden',
                }}
              >
                {product.image ? (
                  <>
                    <img
                      src={product.image}
                      alt={product.name}
                      style={{ width: '100%', height: '100%', objectFit: 'contain', padding: 8 }}
                    />
                    {/* Zoom hint overlay */}
                    <div style={{
                      position: 'absolute', inset: 0,
                      background: 'rgba(79,70,229,0)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      transition: 'background 0.2s',
                    }}
                      className="img-hover-overlay"
                    >
                      <ZoomIn size={28} color="white" style={{ opacity: 0, transition: 'opacity 0.2s' }} className="zoom-icon" />
                    </div>
                  </>
                ) : (
                  <ShoppingBag size={40} color="#CBD5E1" />
                )}
                {product.stock < 1 && (
                  <div style={{
                    position: 'absolute', top: 8, right: 8,
                    background: '#EF4444', color: '#fff',
                    fontSize: 10, fontWeight: 700,
                    padding: '2px 8px', borderRadius: 99,
                  }}>
                    Habis
                  </div>
                )}
              </div>

              {/* Product Info */}
              <div style={{ padding: '10px 12px 12px', flex: 1, display: 'flex', flexDirection: 'column' }}>
                <div style={{ fontWeight: 700, fontSize: 13, color: '#1E293B', marginBottom: 4, lineHeight: 1.3 }}>
                  {product.name}
                </div>
                <div style={{ color: '#4F46E5', fontWeight: 800, fontSize: 14, marginBottom: 6 }}>
                  Rp {product.price.toLocaleString('id-ID')}
                </div>
                {/* Variant / Stock badge */}
                <div style={{ marginBottom: 8 }}>
                  {(() => {
                    const vars = parseVariants(product);
                    if (vars.length > 0) return <span style={{ fontSize: 11, fontWeight: 700, background: '#EEF2FF', color: '#4F46E5', padding: '2px 8px', borderRadius: 99 }}>🎨 {vars.length} Varian</span>;
                    if (product.stock === 0) return <span style={{ fontSize: 11, fontWeight: 700, background: '#FEE2E2', color: '#DC2626', padding: '2px 8px', borderRadius: 99 }}>🚫 Stok Habis</span>;
                    if (product.stock <= 5) return <span style={{ fontSize: 11, fontWeight: 700, background: '#FEF3C7', color: '#D97706', padding: '2px 8px', borderRadius: 99 }}>⚠️ Sisa {product.stock} {product.unit||'pcs'}</span>;
                    return <span style={{ fontSize: 11, fontWeight: 700, background: '#DCFCE7', color: '#16A34A', padding: '2px 8px', borderRadius: 99 }}>✓ Tersedia {product.stock} {product.unit||'pcs'}</span>;
                  })()}
                </div>
                <button
                  onClick={() => addToCart(product)}
                  disabled={product.stock < 1 && parseVariants(product).length === 0}
                  style={{
                    width: '100%', padding: '7px 0', borderRadius: 10, border: 'none',
                    fontWeight: 700, fontSize: 13,
                    cursor: (product.stock < 1 && parseVariants(product).length === 0) ? 'not-allowed' : 'pointer',
                    background: (product.stock < 1 && parseVariants(product).length === 0) ? '#F1F5F9' : '#EEF2FF',
                    color: (product.stock < 1 && parseVariants(product).length === 0) ? '#94A3B8' : '#4F46E5',
                    transition: 'background 0.15s', marginTop: 'auto',
                  }}
                >
                  {parseVariants(product).length > 0 ? 'Pilih Varian ›' : '+ Keranjang'}
                </button>
              </div>
            </div>
          )) : (
            <div style={{ gridColumn: '1/-1', textAlign: 'center', padding: '40px 0', color: '#94A3B8' }}>
              Produk tidak ditemukan.
            </div>
          )}
        </div>
      </div>

      {/* === IMAGE ZOOM MODAL === */}
      {selectedImage && (
        <div
          onClick={() => setSelectedImage(null)}
          style={{
            position: 'fixed', inset: 0,
            background: 'rgba(0,0,0,0.85)',
            zIndex: 9999,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 16,
            backdropFilter: 'blur(4px)',
            cursor: 'zoom-out',
            animation: 'fadeIn 0.2s ease',
          }}
        >
          <img
            src={selectedImage}
            alt="Zoom"
            onClick={(e) => e.stopPropagation()}
            style={{
              maxWidth: '90vw',
              maxHeight: '85vh',
              objectFit: 'contain',
              borderRadius: 16,
              boxShadow: '0 24px 60px rgba(0,0,0,0.5)',
              cursor: 'default',
            }}
          />
          <button
            onClick={() => setSelectedImage(null)}
            style={{
              position: 'absolute', top: 16, right: 16,
              background: 'rgba(255,255,255,0.15)',
              border: 'none',
              borderRadius: '50%',
              width: 40, height: 40,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer',
              color: '#fff',
              backdropFilter: 'blur(8px)',
              transition: 'background 0.2s',
            }}
          >
            <X size={20} />
          </button>
        </div>
      )}

      {/* === CART SIDEBAR === */}
      {isCartOpen && (
        <div style={{
          position: 'fixed', inset: 0,
          background: 'rgba(0,0,0,0.5)',
          zIndex: 50,
          display: 'flex',
          justifyContent: 'flex-end',
        }}>
          <div style={{
            background: '#fff',
            width: '100%',
            maxWidth: 400,
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            boxShadow: '-4px 0 24px rgba(0,0,0,0.15)',
            animation: 'slideIn 0.25s ease',
          }}>
            <div style={{
              padding: '16px 20px',
              borderBottom: '1px solid #E2E8F0',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              background: '#EEF2FF',
            }}>
              <h2 style={{ fontWeight: 800, color: '#3730A3', display: 'flex', alignItems: 'center', gap: 8, margin: 0 }}>
                <ShoppingBag size={20} /> Keranjang Belanja
              </h2>
              <button onClick={() => setIsCartOpen(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748B' }}>
                <X size={22} />
              </button>
            </div>

            <div style={{ flex: 1, overflowY: 'auto', padding: 16 }}>
              {cart.length === 0 ? (
                <div style={{ textAlign: 'center', color: '#94A3B8', marginTop: 60 }}>Keranjang masih kosong.</div>
              ) : (
                cart.map(item => (
                  <div key={item.cartKey} style={{
                    display: 'flex', alignItems: 'center', gap: 12,
                    borderBottom: '1px solid #F1F5F9', paddingBottom: 16, marginBottom: 16,
                  }}>
                    {item.product.image
                      ? <img src={item.product.image} style={{ width: 52, height: 52, objectFit: 'contain', borderRadius: 10, border: '1px solid #E2E8F0' }} alt={item.product.name} />
                      : <div style={{ width: 52, height: 52, background: '#F1F5F9', borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><ShoppingBag size={18} color="#CBD5E1" /></div>
                    }
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 700, fontSize: 13, color: '#1E293B' }}>
                        {item.product.name}
                        {item.variantName && <span style={{ marginLeft: 5, fontSize: 11, background: '#EEF2FF', color: '#4F46E5', padding: '1px 6px', borderRadius: 99, fontWeight: 700 }}>{item.variantName}</span>}
                      </div>
                      <div style={{ color: '#4F46E5', fontWeight: 800, fontSize: 13 }}>Rp {getItemPrice(item).toLocaleString('id-ID')}</div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, background: '#F8FAFC', borderRadius: 10, padding: '4px 8px' }}>
                      <button onClick={() => updateQuantity(item.cartKey, -1)} style={{ width: 24, height: 24, border: '1px solid #E2E8F0', borderRadius: 6, background: '#fff', cursor: 'pointer', fontWeight: 700 }}>-</button>
                      <span style={{ fontWeight: 800, minWidth: 20, textAlign: 'center', fontSize: 14 }}>{item.quantity}</span>
                      <button onClick={() => updateQuantity(item.cartKey, 1)} style={{ width: 24, height: 24, border: '1px solid #E2E8F0', borderRadius: 6, background: '#fff', cursor: 'pointer', fontWeight: 700 }}>+</button>
                    </div>
                    <button onClick={() => removeFromCart(item.cartKey)} style={{ background: 'none', border: 'none', color: '#EF4444', cursor: 'pointer', fontSize: 12, fontWeight: 600 }}>
                      <X size={16} />
                    </button>
                  </div>
                ))
              )}
            </div>

            <div style={{ padding: '16px 20px', borderTop: '1px solid #E2E8F0', background: '#F8FAFC' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 14 }}>
                <span style={{ color: '#64748B', fontWeight: 600 }}>Total Belanja:</span>
                <span style={{ fontWeight: 900, fontSize: 18, color: '#1E293B' }}>Rp {cartTotal.toLocaleString('id-ID')}</span>
              </div>
              <button
                onClick={checkoutWhatsApp}
                disabled={cart.length === 0}
                style={{
                  width: '100%', padding: '13px 0',
                  borderRadius: 14, border: 'none',
                  fontWeight: 800, fontSize: 15,
                  cursor: cart.length === 0 ? 'not-allowed' : 'pointer',
                  background: cart.length === 0 ? '#CBD5E1' : '#22C55E',
                  color: '#fff',
                  boxShadow: cart.length > 0 ? '0 4px 14px rgba(34,197,94,0.35)' : 'none',
                  transition: 'background 0.2s',
                }}
              >
                {cart.length > 0 ? '🛒 Pesan via WhatsApp' : 'Keranjang Kosong'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Variant Modal */}
      {variantModal && (
        <div onClick={() => setVariantModal(null)} style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(3px)', zIndex: 9999, display: 'flex', alignItems: 'flex-end', justifyContent: 'center' }}>
          <div onClick={e => e.stopPropagation()} style={{ background: 'white', borderRadius: '20px 20px 0 0', padding: '20px 16px 32px', width: '100%', maxWidth: '480px', maxHeight: '70vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
              <div style={{ fontWeight: 900, fontSize: '1rem', color: '#111827' }}>Pilih Varian</div>
              <button onClick={() => setVariantModal(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9CA3AF' }}><X size={20} /></button>
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

      {/* WA Checkout Modal */}
      {waQueueModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.55)', backdropFilter: 'blur(4px)', zIndex: 9999, display: 'flex', alignItems: 'flex-end', justifyContent: 'center', padding: 0 }}>
          <div style={{ background: 'white', borderRadius: '24px 24px 0 0', padding: '24px 20px 32px', width: '100%', maxWidth: 480, boxShadow: '0 -8px 40px rgba(0,0,0,0.15)' }}>
            <div style={{ width: 40, height: 4, background: '#E5E7EB', borderRadius: 99, margin: '0 auto 20px' }} />
            <h3 style={{ margin: '0 0 4px', color: '#1E293B', fontWeight: 900, fontSize: '1.1rem' }}>🛒 Konfirmasi Pesanan</h3>
            <p style={{ margin: '0 0 14px', fontSize: 13, color: '#64748B' }}>Isi nama Anda, pesanan dikirim via WhatsApp</p>

            {/* Toggle Ambil Sendiri / Dikirim — hanya tampil untuk user demo */}
            {isDemo && (
              <>
                <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
                  <button onClick={() => setDeliveryType('pickup')} style={{ flex: 1, padding: '10px 0', borderRadius: 12, border: `2px solid ${deliveryType === 'pickup' ? '#4F46E5' : '#E5E7EB'}`, background: deliveryType === 'pickup' ? '#EEF2FF' : 'white', color: deliveryType === 'pickup' ? '#4F46E5' : '#64748B', fontWeight: 700, fontSize: 13, cursor: 'pointer' }}>🏪 Ambil Sendiri</button>
                  <button onClick={() => setDeliveryType('delivery')} style={{ flex: 1, padding: '10px 0', borderRadius: 12, border: `2px solid ${deliveryType === 'delivery' ? '#4F46E5' : '#E5E7EB'}`, background: deliveryType === 'delivery' ? '#EEF2FF' : 'white', color: deliveryType === 'delivery' ? '#4F46E5' : '#64748B', fontWeight: 700, fontSize: 13, cursor: 'pointer' }}>🚚 Dikirim</button>
                </div>
              </>
            )}

            <input
              type="text" placeholder="Nama Anda (opsional)"
              value={buyerName}
              onChange={e => setBuyerName(e.target.value)}
              style={{ width: '100%', padding: '11px 14px', borderRadius: 12, border: '1.5px solid #E2E8F0', fontSize: 14, outline: 'none', boxSizing: 'border-box', marginBottom: isDemo && deliveryType === 'delivery' ? 10 : 14 }}
            />

            {/* Alamat pengiriman — hanya untuk demo + pilih Dikirim */}
            {isDemo && deliveryType === 'delivery' && (
              <textarea
                placeholder="Alamat pengiriman lengkap..."
                value={buyerAddress}
                onChange={e => setBuyerAddress(e.target.value)}
                rows={2}
                style={{ width: '100%', padding: '11px 14px', borderRadius: 12, border: '1.5px solid #E2E8F0', fontSize: 14, outline: 'none', boxSizing: 'border-box', resize: 'vertical', marginBottom: 14 }}
              />
            )}

            <div style={{ background: '#F8FAFC', borderRadius: 12, padding: '12px 14px', marginBottom: 14 }}>
              <div style={{ fontWeight: 700, fontSize: 13, color: '#1E293B', marginBottom: 4 }}>Ringkasan ({cart.length} item)</div>
              {cart.map(item => <div key={item.cartKey} style={{ fontSize: 12, color: '#64748B', display: 'flex', justifyContent: 'space-between' }}><span>{item.product.name}{item.variantName ? ` (${item.variantName})` : ''} ×{item.quantity}</span><span>Rp {(getItemPrice(item) * item.quantity).toLocaleString('id-ID')}</span></div>)}
              <div style={{ fontWeight: 900, color: '#4F46E5', fontSize: 14, borderTop: '1px solid #E5E7EB', marginTop: 6, paddingTop: 6, display: 'flex', justifyContent: 'space-between' }}><span>Total</span><span>Rp {cartTotal.toLocaleString('id-ID')}</span></div>
            </div>

            <button onClick={sendWhatsApp} style={{ width: '100%', padding: '14px 0', borderRadius: 14, border: 'none', background: 'linear-gradient(135deg,#22C55E,#16A34A)', color: 'white', fontWeight: 800, fontSize: 15, cursor: 'pointer', marginBottom: 8, boxShadow: '0 4px 14px rgba(34,197,94,0.35)' }}>💬 Kirim Pesanan via WhatsApp</button>
            <button onClick={() => { setWaQueueModal(false); setWaQueueNum(''); setBuyerName(''); setBuyerAddress(''); setDeliveryType('pickup'); }} style={{ width: '100%', padding: '12px 0', borderRadius: 14, border: '1.5px solid #E2E8F0', background: 'white', color: '#64748B', fontWeight: 700, fontSize: 14, cursor: 'pointer' }}>Batal</button>
          </div>
        </div>
      )}

      <style>{`
        @keyframes fadeIn { from { opacity: 0 } to { opacity: 1 } }
        @keyframes slideIn { from { transform: translateX(100%) } to { transform: translateX(0) } }
        @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
      `}</style>
    </div>
  );
}
