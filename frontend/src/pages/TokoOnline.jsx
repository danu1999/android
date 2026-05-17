import React, { useState, useEffect } from 'react';
import { ShoppingBag, Search, X, ZoomIn } from 'lucide-react';
import api from '../api';

export default function TokoOnline() {
  const [products, setProducts] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [cart, setCart] = useState([]);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [selectedImage, setSelectedImage] = useState(null);

  const [lastUpdated, setLastUpdated] = useState(null);

  const fetchProducts = async () => {
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
    // Auto-refresh setiap 30 detik agar stok selalu real-time
    const interval = setInterval(fetchProducts, 30000);
    return () => clearInterval(interval);
  }, []);

  const filteredProducts = products.filter(p =>
    p.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const addToCart = (product) => {
    if (product.stock < 1) {
      alert('Maaf, stok produk habis.');
      return;
    }
    const existing = cart.find(item => item.product.id === product.id);
    if (existing) {
      setCart(cart.map(item =>
        item.product.id === product.id
          ? { ...item, quantity: item.quantity + 1 }
          : item
      ));
    } else {
      setCart([...cart, { product, quantity: 1 }]);
    }
  };

  const removeFromCart = (productId) => {
    setCart(cart.filter(item => item.product.id !== productId));
  };

  const updateQuantity = (productId, delta) => {
    setCart(cart.map(item => {
      if (item.product.id === productId) {
        const newQty = item.quantity + delta;
        return newQty > 0 ? { ...item, quantity: newQty } : item;
      }
      return item;
    }));
  };

  const cartTotal = cart.reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
  const cartItemsCount = cart.reduce((sum, item) => sum + item.quantity, 0);

  const checkoutWhatsApp = () => {
    if (cart.length === 0) return;
    let message = "Halo, saya ingin memesan dari katalog online:\n\n";
    cart.forEach((item, index) => {
      message += `${index + 1}. ${item.product.name}\n   ${item.quantity} x Rp ${item.product.price.toLocaleString('id-ID')} = Rp ${(item.quantity * item.product.price).toLocaleString('id-ID')}\n`;
    });
    message += `\n*Total Belanja: Rp ${cartTotal.toLocaleString('id-ID')}*\n\nTerima kasih.`;
    const waNumber = "6281234567890";
    const encodedMessage = encodeURIComponent(message);
    window.open(`https://wa.me/${waNumber}?text=${encodedMessage}`, '_blank');
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
              Stok diperbarui {lastUpdated.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' })} · Auto-refresh 30s
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
                {/* Stock badge */}
                <div style={{ marginBottom: 8 }}>
                  {product.stock === 0 ? (
                    <span style={{ fontSize: 11, fontWeight: 700, background: '#FEE2E2', color: '#DC2626', padding: '2px 8px', borderRadius: 99 }}>🚫 Stok Habis</span>
                  ) : product.stock <= 5 ? (
                    <span style={{ fontSize: 11, fontWeight: 700, background: '#FEF3C7', color: '#D97706', padding: '2px 8px', borderRadius: 99 }}>⚠️ Sisa {product.stock} {product.unit||'pcs'}</span>
                  ) : (
                    <span style={{ fontSize: 11, fontWeight: 700, background: '#DCFCE7', color: '#16A34A', padding: '2px 8px', borderRadius: 99 }}>✓ Tersedia {product.stock} {product.unit||'pcs'}</span>
                  )}
                </div>
                <button
                  onClick={() => addToCart(product)}
                  disabled={product.stock < 1}
                  style={{
                    width: '100%',
                    padding: '7px 0',
                    borderRadius: 10,
                    border: 'none',
                    fontWeight: 700,
                    fontSize: 13,
                    cursor: product.stock < 1 ? 'not-allowed' : 'pointer',
                    background: product.stock < 1 ? '#F1F5F9' : '#EEF2FF',
                    color: product.stock < 1 ? '#94A3B8' : '#4F46E5',
                    transition: 'background 0.15s',
                    marginTop: 'auto',
                  }}
                >
                  + Keranjang
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
                  <div key={item.product.id} style={{
                    display: 'flex', alignItems: 'center', gap: 12,
                    borderBottom: '1px solid #F1F5F9', paddingBottom: 16, marginBottom: 16,
                  }}>
                    {item.product.image
                      ? <img src={item.product.image} style={{ width: 52, height: 52, objectFit: 'contain', borderRadius: 10, border: '1px solid #E2E8F0' }} alt={item.product.name} />
                      : <div style={{ width: 52, height: 52, background: '#F1F5F9', borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><ShoppingBag size={18} color="#CBD5E1" /></div>
                    }
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 700, fontSize: 13, color: '#1E293B' }}>{item.product.name}</div>
                      <div style={{ color: '#4F46E5', fontWeight: 800, fontSize: 13 }}>Rp {item.product.price.toLocaleString('id-ID')}</div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, background: '#F8FAFC', borderRadius: 10, padding: '4px 8px' }}>
                      <button onClick={() => updateQuantity(item.product.id, -1)} style={{ width: 24, height: 24, border: '1px solid #E2E8F0', borderRadius: 6, background: '#fff', cursor: 'pointer', fontWeight: 700 }}>-</button>
                      <span style={{ fontWeight: 800, minWidth: 20, textAlign: 'center', fontSize: 14 }}>{item.quantity}</span>
                      <button onClick={() => updateQuantity(item.product.id, 1)} style={{ width: 24, height: 24, border: '1px solid #E2E8F0', borderRadius: 6, background: '#fff', cursor: 'pointer', fontWeight: 700 }}>+</button>
                    </div>
                    <button onClick={() => removeFromCart(item.product.id)} style={{ background: 'none', border: 'none', color: '#EF4444', cursor: 'pointer', fontSize: 12, fontWeight: 600 }}>
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

      <style>{`
        @keyframes fadeIn { from { opacity: 0 } to { opacity: 1 } }
        @keyframes slideIn { from { transform: translateX(100%) } to { transform: translateX(0) } }
        @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
      `}</style>
    </div>
  );
}
