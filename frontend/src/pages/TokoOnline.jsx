import React, { useState, useEffect } from 'react';
import { ShoppingBag, Search, ExternalLink } from 'lucide-react';
import api from '../api';

export default function TokoOnline() {
  const [products, setProducts] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');

  const [cart, setCart] = useState([]);
  const [isCartOpen, setIsCartOpen] = useState(false);

  useEffect(() => {
    // In a real app, this would be a public endpoint without auth needed
    const fetchProducts = async () => {
      try {
        const res = await api.get('/products');
        setProducts(res.data);
      } catch (err) {
        console.error('Failed to fetch products', err);
      }
    };
    fetchProducts();
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
    
    // Ganti nomor WhatsApp di bawah ini dengan nomor toko yang sebenarnya
    const waNumber = "6281234567890"; 
    const encodedMessage = encodeURIComponent(message);
    window.open(`https://wa.me/${waNumber}?text=${encodedMessage}`, '_blank');
  };

  return (
    <div className="page-container" style={{ padding: '0', background: '#F9FAFB', minHeight: '100vh' }}>
      {/* Public Header */}
      <header className="bg-white shadow-sm px-6 py-4 flex justify-between items-center mb-6 sticky top-0 z-10">
        <div className="flex items-center gap-2 text-indigo-600 font-bold text-xl">
          <ShoppingBag size={24} />
          <span>POSBah Online</span>
        </div>
        <button 
          className="btn btn-primary text-sm flex items-center gap-2 relative"
          onClick={() => setIsCartOpen(true)}
        >
          <ShoppingBag size={18} /> Keranjang
          {cartItemsCount > 0 && (
            <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs font-bold w-5 h-5 flex items-center justify-center rounded-full">
              {cartItemsCount}
            </span>
          )}
        </button>
      </header>

      <div className="max-w-5xl mx-auto px-4 pb-12">
        <div className="text-center mb-10">
          <h1 className="text-3xl font-extrabold text-gray-900 mb-4">Katalog Digital Kami</h1>
          <p className="text-gray-500 max-w-2xl mx-auto">
            Selamat datang di toko online kami. Jelajahi produk-produk unggulan kami dan pesan dengan mudah melalui WhatsApp.
          </p>
        </div>

        <div className="bg-white rounded-2xl shadow-sm p-2 flex items-center gap-2 mb-8 max-w-lg mx-auto border border-gray-100">
          <Search className="text-gray-400 ml-2" size={20} />
          <input 
            type="text" 
            placeholder="Cari produk yang Anda inginkan..." 
            className="w-full p-2 outline-none text-gray-700 bg-transparent"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6">
          {filteredProducts.length > 0 ? (
            filteredProducts.map(product => (
              <div key={product.id} className="bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow overflow-hidden border border-gray-100 flex flex-col">
                <div className="h-40 md:h-48 bg-gray-100 flex items-center justify-center relative">
                  {product.image ? (
                    <img src={product.image} alt={product.name} className="w-full h-full object-contain p-2" />
                  ) : (
                    <ShoppingBag size={40} className="text-gray-300" />
                  )}
                  {product.stock < 1 && (
                    <div className="absolute top-2 right-2 bg-red-500 text-white text-xs font-bold px-2 py-1 rounded">
                      Habis
                    </div>
                  )}
                </div>
                <div className="p-4 flex-1 flex flex-col">
                  <h3 className="font-semibold text-gray-800 mb-1 line-clamp-2">{product.name}</h3>
                  <div className="text-indigo-600 font-bold mt-auto">
                    Rp {product.price.toLocaleString('id-ID')}
                  </div>
                  <button 
                    className={`mt-3 w-full py-2 rounded-lg text-sm font-semibold flex items-center justify-center gap-1 transition-colors ${
                      product.stock < 1 
                        ? 'bg-gray-100 text-gray-400 cursor-not-allowed' 
                        : 'bg-indigo-50 text-indigo-700 hover:bg-indigo-100'
                    }`}
                    onClick={() => addToCart(product)}
                    disabled={product.stock < 1}
                  >
                    + Keranjang
                  </button>
                </div>
              </div>
            ))
          ) : (
            <div className="col-span-full text-center py-12 text-gray-500">
              Produk tidak ditemukan.
            </div>
          )}
        </div>
      </div>

      {/* Cart Modal Overlay */}
      {isCartOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex justify-end">
          <div className="bg-white w-full max-w-md h-full shadow-2xl flex flex-col transform transition-transform duration-300">
            <div className="p-4 border-b flex justify-between items-center bg-indigo-50">
              <h2 className="text-lg font-bold text-indigo-800 flex items-center gap-2">
                <ShoppingBag size={20} /> Keranjang Belanja
              </h2>
              <button 
                className="text-gray-500 hover:text-gray-800 p-2"
                onClick={() => setIsCartOpen(false)}
              >
                Tutup
              </button>
            </div>
            
            <div className="flex-1 overflow-y-auto p-4">
              {cart.length === 0 ? (
                <div className="text-center text-gray-500 mt-10">
                  Keranjang Anda masih kosong.
                </div>
              ) : (
                <div className="flex flex-col gap-4">
                  {cart.map(item => (
                    <div key={item.product.id} className="flex justify-between items-center border-b pb-4">
                      <div className="flex gap-3 items-center w-full">
                        {item.product.image ? (
                          <img src={item.product.image} className="w-12 h-12 object-cover rounded-md" />
                        ) : (
                          <div className="w-12 h-12 bg-gray-200 rounded-md flex items-center justify-center"><ShoppingBag size={16} className="text-gray-400" /></div>
                        )}
                        <div className="flex-1">
                          <h4 className="font-semibold text-sm line-clamp-1">{item.product.name}</h4>
                          <div className="text-indigo-600 font-bold text-sm">Rp {item.product.price.toLocaleString('id-ID')}</div>
                        </div>
                        <div className="flex items-center gap-2 bg-gray-100 rounded-lg p-1">
                          <button className="w-6 h-6 flex items-center justify-center bg-white rounded shadow-sm text-gray-600" onClick={() => updateQuantity(item.product.id, -1)}>-</button>
                          <span className="text-sm font-bold w-4 text-center">{item.quantity}</span>
                          <button className="w-6 h-6 flex items-center justify-center bg-white rounded shadow-sm text-gray-600" onClick={() => updateQuantity(item.product.id, 1)}>+</button>
                        </div>
                        <button className="text-red-500 hover:text-red-700 ml-2 p-2" onClick={() => removeFromCart(item.product.id)}>
                          Hapus
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="p-4 border-t bg-gray-50">
              <div className="flex justify-between items-center mb-4">
                <span className="text-gray-600 font-semibold">Total Belanja:</span>
                <span className="text-xl font-bold text-gray-900">Rp {cartTotal.toLocaleString('id-ID')}</span>
              </div>
              <button 
                className={`w-full py-3 rounded-xl font-bold text-white flex justify-center items-center gap-2 ${cart.length === 0 ? 'bg-gray-400 cursor-not-allowed' : 'bg-green-500 hover:bg-green-600 shadow-lg'}`}
                onClick={checkoutWhatsApp}
                disabled={cart.length === 0}
              >
                {cart.length > 0 ? 'Pesan via WhatsApp Sekarang' : 'Keranjang Kosong'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
