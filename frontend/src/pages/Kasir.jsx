import React, { useState, useEffect, useRef } from 'react';
import { Search, ShoppingCart, Trash2, CreditCard, QrCode, Printer } from 'lucide-react';
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
      if (existing.quantity >= product.stock) {
        alert('Stok tidak cukup!');
        return;
      }
      setCart(cart.map(item => 
        item.product.id === product.id ? { ...item, quantity: item.quantity + 1 } : item
      ));
    } else {
      if (product.stock < 1) {
        alert('Stok habis!');
        return;
      }
      setCart([...cart, { product, quantity: 1, discount: 0 }]);
    }
  };

  const removeFromCart = (productId) => {
    setCart(cart.filter(item => item.product.id !== productId));
  };

  const updateQuantity = (productId, delta) => {
    setCart(cart.map(item => {
      if (item.product.id === productId) {
        const newQty = item.quantity + delta;
        if (newQty > 0 && newQty <= item.product.stock) {
          return { ...item, quantity: newQty };
        }
      }
      return item;
    }));
  };

  const [globalDiscount, setGlobalDiscount] = useState(0);

  const totalAmount = cart.reduce((sum, item) => sum + ((item.product.price - item.discount) * item.quantity), 0) - globalDiscount;

  const updateItemDiscount = (productId, discount) => {
    setCart(cart.map(item => {
      if (item.product.id === productId) {
        return { ...item, discount: Number(discount) || 0 };
      }
      return item;
    }));
  };

  const handleCheckout = async () => {
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
        paymentMethod: paymentMethod,
        type: isBackdate ? 'BACKDATE' : 'SALES',
        date: isBackdate ? transactionDate : undefined
      };
      
      const res = await api.post('/transactions', transactionData);
      setReceiptData(res.data);
      setCart([]);
      setGlobalDiscount(0);
      setIsPaymentModalOpen(false);
      fetchProducts(); // Refresh stock
    } catch (err) {
      console.error('Checkout failed', err);
      alert('Gagal memproses transaksi');
    }
  };

  const printReceipt = (size) => {
    // Generate simple receipt
    const printWindow = window.open('', '_blank');
    const width = size === '58mm' ? '58mm' : '80mm';
    
    printWindow.document.write(`
      <html>
        <head>
          <style>
            body { font-family: monospace; width: ${width}; margin: 0 auto; padding: 10px; font-size: 12px; }
            .text-center { text-align: center; }
            .bold { font-weight: bold; }
            table { width: 100%; font-size: 12px; border-collapse: collapse; margin-top: 10px; }
            th, td { text-align: left; padding: 4px 0; }
            .right { text-align: right; }
            .divider { border-top: 1px dashed #000; margin: 5px 0; }
          </style>
        </head>
        <body>
          <div class="text-center bold" style="font-size: 16px;">POSBah</div>
          <div class="text-center">Struk Pembayaran</div>
          <div class="divider"></div>
          <div>No: ${receiptData?.receiptNumber}</div>
          <div>Metode: ${receiptData?.paymentMethod}</div>
          <div class="divider"></div>
          <table>
            ${receiptData?.items.map(item => {
              const prod = products.find(p => p.id === item.productId);
              return `
                <tr>
                  <td colspan="3">${prod?.name || 'Item'}</td>
                </tr>
                <tr>
                  <td>${item.quantity} x</td>
                  <td>Rp ${item.price}</td>
                  <td class="right">Rp ${item.quantity * item.price}</td>
                </tr>
                ${item.discount > 0 ? `
                <tr>
                  <td colspan="2">Diskon Item</td>
                  <td class="right">-Rp ${item.discount * item.quantity}</td>
                </tr>` : ''}
              `;
            }).join('')}
          </table>
          <div class="divider"></div>
          <table>
            ${receiptData?.discount > 0 ? `
            <tr>
              <td>Diskon Trx</td>
              <td class="right">-Rp ${receiptData.discount}</td>
            </tr>` : ''}
            <tr>
              <td class="bold">TOTAL</td>
              <td class="right bold">Rp ${receiptData?.total}</td>
            </tr>
          </table>
          <div class="divider"></div>
          <div class="text-center">Terima Kasih</div>
          <script>
            window.print();
            window.onafterprint = function() { window.close(); }
          </script>
        </body>
      </html>
    `);
    printWindow.document.close();
  };

  const filteredProducts = products.filter(p => 
    p.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="kasir-layout flex-col md:flex-row h-auto md:h-full">
      {/* Left: Product Grid */}
      <div className="kasir-main">
        <div className="glass-panel search-bar mb-4">
          <Search size={20} className="text-gray-400" />
          <input 
            type="text" 
            placeholder="Cari produk..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <div className="product-grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
          {filteredProducts.map(product => (
            <div 
              key={product.id} 
              className="product-card glass-panel flex flex-col"
              onClick={() => addToCart(product)}
            >
              <div className="product-image h-24 md:h-32">
                {product.image ? (
                  <img src={product.image} alt={product.name} />
                ) : (
                  <div className="image-placeholder">No Image</div>
                )}
              </div>
              <div className="product-info p-2 md:p-4 flex-1 flex flex-col">
                <div className="product-name text-sm md:text-base">{product.name}</div>
                <div className="product-price text-sm md:text-base">Rp {product.price.toLocaleString('id-ID')}</div>
                <div className="product-stock text-xs mt-auto">Stok: {product.stock}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Right: Cart */}
      <div className="kasir-sidebar glass-panel w-full md:w-80 lg:w-96 mt-4 md:mt-0 max-h-screen overflow-y-auto">
        <div className="cart-header">
          <h2><ShoppingCart size={20} /> Keranjang</h2>
          {cart.length > 0 && (
            <button className="btn-icon btn-danger" onClick={() => setCart([])} title="Kosongkan">
              <Trash2 size={16} />
            </button>
          )}
        </div>

        <div className="cart-items flex-1 overflow-y-auto">
          {cart.length === 0 ? (
            <div className="empty-cart text-center text-gray-500 mt-8">
              Keranjang kosong
            </div>
          ) : (
            cart.map(item => (
              <div key={item.product.id} className="cart-item border-b pb-2 mb-2">
                <div className="cart-item-info">
                  <div className="cart-item-name">{item.product.name}</div>
                  <div className="cart-item-price">Rp {item.product.price.toLocaleString('id-ID')}</div>
                </div>
                <div className="flex justify-between items-center mt-2">
                  <div className="flex items-center gap-1">
                    <span className="text-xs text-gray-500">Diskon (Rp):</span>
                    <input 
                      type="number" 
                      className="w-16 p-1 text-xs border rounded outline-none" 
                      value={item.discount}
                      onChange={(e) => updateItemDiscount(item.product.id, e.target.value)}
                    />
                  </div>
                  <div className="cart-item-actions">
                    <button onClick={() => updateQuantity(item.product.id, -1)}>-</button>
                    <span>{item.quantity}</span>
                    <button onClick={() => updateQuantity(item.product.id, 1)}>+</button>
                    <button className="btn-icon text-red-500" onClick={() => removeFromCart(item.product.id)}>
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="cart-summary mt-auto">
          <div className="flex justify-between items-center mb-2 text-sm">
            <span>Diskon Transaksi (Rp):</span>
            <input 
              type="number" 
              className="w-24 p-1 border rounded outline-none text-right" 
              value={globalDiscount}
              onChange={(e) => setGlobalDiscount(Number(e.target.value) || 0)}
            />
          </div>
          <div className="summary-row font-bold text-xl pt-2 border-t">
            <span>Total</span>
            <span>Rp {totalAmount > 0 ? totalAmount.toLocaleString('id-ID') : 0}</span>
          </div>
          <button 
            className="btn btn-primary w-full mt-4 justify-center py-3 text-lg"
            disabled={cart.length === 0}
            onClick={() => setIsPaymentModalOpen(true)}
          >
            Bayar
          </button>
        </div>
      </div>

      {/* Payment Modal */}
      {isPaymentModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel text-center">
            <h2>Pilih Metode Pembayaran</h2>
            
            <div className="text-left mt-4 mb-2 p-3 border rounded bg-gray-50 flex flex-col gap-2">
              <label className="flex items-center gap-2 cursor-pointer">
                <input 
                  type="checkbox" 
                  checked={isBackdate} 
                  onChange={(e) => setIsBackdate(e.target.checked)} 
                />
                <span className="font-semibold text-gray-700">Gunakan Backdate Kasir (Transaksi Lampau)</span>
              </label>
              
              {isBackdate && (
                <div className="mt-2">
                  <label className="block text-sm text-gray-600 mb-1">Pilih Tanggal Transaksi</label>
                  <input 
                    type="datetime-local" 
                    className="p-2 border rounded w-full"
                    value={transactionDate}
                    onChange={(e) => setTransactionDate(e.target.value)}
                  />
                </div>
              )}
            </div>

            <div className="payment-options mt-4 flex justify-center gap-4">
              <button 
                className={`btn ${paymentMethod === 'CASH' ? 'btn-primary' : 'btn-secondary'} flex-col p-4 w-32`}
                onClick={() => setPaymentMethod('CASH')}
              >
                <CreditCard size={32} className="mb-2" />
                CASH
              </button>
              <button 
                className={`btn ${paymentMethod === 'QRIS' ? 'btn-primary' : 'btn-secondary'} flex-col p-4 w-32`}
                onClick={() => setPaymentMethod('QRIS')}
              >
                <QrCode size={32} className="mb-2" />
                QRIS
              </button>
            </div>
            
            {paymentMethod === 'QRIS' && (
              <div className="mt-4 p-4 border rounded-lg bg-white inline-block">
                {/* Dummy QR Code */}
                <div style={{width: 150, height: 150, background: '#ccc', margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
                  QRIS Mockup
                </div>
              </div>
            )}

            <div className="summary-row mt-6 text-xl font-bold">
              <span>Total Tagihan:</span>
              <span>Rp {totalAmount.toLocaleString('id-ID')}</span>
            </div>

            <div className="modal-actions justify-center mt-6">
              <button className="btn btn-secondary" onClick={() => setIsPaymentModalOpen(false)}>Batal</button>
              <button className="btn btn-primary px-8" onClick={handleCheckout}>Proses Pembayaran</button>
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
              <button className="btn btn-primary justify-center" onClick={() => printReceipt('58mm')}>
                <Printer size={18} /> Cetak Struk (58mm)
              </button>
              <button className="btn btn-primary justify-center" onClick={() => printReceipt('80mm')}>
                <Printer size={18} /> Cetak Struk (80mm)
              </button>
              <button className="btn btn-secondary justify-center mt-2" onClick={() => setReceiptData(null)}>
                Tutup
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
