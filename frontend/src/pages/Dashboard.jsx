import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Wallet, Contact, Users, Globe,
  TrendingUp, ShoppingBag, Package, AlertTriangle,
  ArrowRight, BarChart2, Clock, History, Download
} from 'lucide-react';
import api from '../api';
import { useAuth, useIsAdmin, useIsOwner } from '../AuthContext';
import { useDemoBlock } from '../AuthContext';
import { App as CapApp } from '@capacitor/app';

export default function Dashboard({ appMode }) {
  const { user } = useAuth();
  const isAdmin = useIsAdmin();
  const isOwner = useIsOwner();
  const { isDemo } = useDemoBlock();

  const [report, setReport] = useState(null);
  const [products, setProducts] = useState([]);
  const [cars, setCars] = useState([]);
  const [rentals, setRentals] = useState([]);
  const [laundryOrders, setLaundryOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hasUpdate, setHasUpdate] = useState(false);
  const [latestVer, setLatestVer] = useState("1.1.0");

  useEffect(() => {
    const checkVersion = async () => {
      try {
        let currentVer = "1.1.0";
        try {
          const info = await CapApp.getInfo();
          if (info && info.version) {
            currentVer = info.version;
          }
        } catch (err) {
          console.warn("CapApp.getInfo tidak tersedia di browser:", err);
        }

        const res = await api.get('/apk-version');
        const serverVer = res.data?.version;
        if (serverVer) {
          setLatestVer(serverVer);
          if (currentVer !== serverVer) {
            setHasUpdate(true);
          } else {
            setHasUpdate(false);
          }
        }
      } catch (err) {
        console.warn("Gagal memproses pengecekan versi APK:", err);
      }
    };
    checkVersion();
  }, []);

  const DEMO_PRODUCTS = [
    { id: 'p301', name: 'Pisang Keju Cokelat', stock: 120, price: 15000 },
    { id: 'p302', name: 'Pisang Keju Stroberi', stock: 85, price: 15000 },
    { id: 'p303', name: 'Pisang Keju Premium', stock: 50, price: 20000 },
    { id: 'p304', name: 'Jus Alpukat', stock: 60, price: 18000 },
    { id: 'p305', name: 'Jus Mangga', stock: 75, price: 15000 },
    { id: 'p306', name: 'Es Teh Manis', stock: 4, price: 8000 }, // stok menipis
  ];

  const DEMO_REPORT = {
    totalSales: 45280000,
    totalExpenses: 12400000,
    netIncome: 32880000,
    pendingReceivables: 1850000,
    todaySales: 705000,
    transactionCount: 5,
  };

  useEffect(() => {
    const load = async () => {
      try {
        if (appMode === 'RENTAL') {
          const [rRes, cRes, rentRes] = await Promise.all([
            api.get('/reports'),
            api.get('/cars'),
            api.get('/rentals'),
          ]);
          setReport(rRes.data);
          setCars(cRes.data);
          setRentals(rentRes.data);
        } else if (appMode === 'LAUNDRY') {
          const [rRes, lRes] = await Promise.all([
            api.get('/reports'),
            api.get('/laundry/orders'),
          ]);
          setReport(rRes.data);
          setLaundryOrders(lRes.data);
        } else {
          const [rRes, pRes] = await Promise.all([
            api.get('/reports'),
            api.get('/products'),
          ]);
          setReport(rRes.data);
          setProducts(pRes.data);
        }
      } catch (_) { }
      finally { setLoading(false); }
    };
    load();
  }, [appMode]);

  const now = new Date();
  const greeting = now.getHours() < 12 ? 'Selamat Pagi' : now.getHours() < 17 ? 'Selamat Siang' : 'Selamat Malam';
  const dateStr = now.toLocaleDateString('id-ID', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

  const lowStock = products.filter(p => p.stock > 0 && p.stock <= 5);
  const outStock = products.filter(p => p.stock === 0);

  const fmt = (n) => Number(n || 0).toLocaleString('id-ID');

  // Menu cards
  const menus = [
    { path: '/keuangan', label: 'Keuangan', icon: <Wallet size={22} />, grad: 'linear-gradient(135deg,#10B981,#059669)', show: isAdmin },
    { path: '/pelanggan', label: 'Pelanggan', icon: <Contact size={22} />, grad: 'linear-gradient(135deg,#3B82F6,#2563EB)', show: isAdmin },
    { path: '/karyawan', label: 'Karyawan', icon: <Users size={22} />, grad: 'linear-gradient(135deg,#F59E0B,#D97706)', show: isAdmin },
    { path: '/activity-logs', label: 'Log Aktivitas', icon: <History size={22} />, grad: 'linear-gradient(135deg,#EC4899,#BE185D)', show: isAdmin },
    { path: '/toko-online', label: 'Toko Online', icon: <Globe size={22} />, grad: 'linear-gradient(135deg,#8B5CF6,#6D28D9)', show: appMode === 'FNB' },
    { path: '/orders-laundry', label: 'Riwayat Order', icon: <Clock size={22} />, grad: 'linear-gradient(135deg,#EC4899,#BE185D)', show: appMode === 'LAUNDRY' },
    { path: '/layanan-laundry', label: 'Tarif Layanan', icon: <Package size={22} />, grad: 'linear-gradient(135deg,#8B5CF6,#6D28D9)', show: appMode === 'LAUNDRY' && isAdmin },
  ].filter(m => m.show);

  return (
    <div className="db-container">

      {/* ── Header row ─────────────────────────────────────────── */}
      <div style={{
        background: 'linear-gradient(135deg,#4F46E5,#7C3AED)',
        borderRadius: 18, padding: '14px 20px',
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        color: 'white', flexShrink: 0,
      }}>
        <div>
          <div style={{ fontSize: 11, opacity: 0.8, marginBottom: 2 }}>{dateStr}</div>
          <div style={{ fontSize: 18, fontWeight: 800 }}>{greeting}, {user?.name}! 👋</div>
          <div style={{ fontSize: 11, opacity: 0.75, marginTop: 2 }}>
            <Clock size={11} style={{ display: 'inline', marginRight: 4 }} />
            {now.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' })} WIB
          </div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 10, opacity: 0.7, marginBottom: 4 }}>Role Aktif</div>
          <div style={{
            background: 'rgba(255,255,255,0.2)', borderRadius: 99,
            padding: '4px 12px', fontSize: 12, fontWeight: 800, letterSpacing: 1
          }}>{user?.role}</div>
        </div>
      </div>



      {/* ── Middle: stats + alerts ─────────────────────────────── */}
      <div className="db-middle">

        {/* Stats column */}
        {isAdmin && (
          <div className="db-stats-col">

            {/* Row 1: Penjualan Hari Ini & Total Penjualan */}
            <div className="db-row-1">
              {/* Penjualan Hari Ini */}
              <div style={{
                background: 'linear-gradient(135deg,#ECFDF5,#D1FAE5)',
                border: '1px solid #6EE7B7', borderRadius: 14, padding: '12px 14px',
                display: 'flex', alignItems: 'center', gap: 10, flex: 1, minWidth: 0
              }}>
                <div style={{ background: '#10B981', borderRadius: 10, padding: 8, display: 'flex', flexShrink: 0 }}>
                  <TrendingUp size={16} color="white" />
                </div>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontSize: 9, color: '#065F46', fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.5 }}>Hari Ini</div>
                  <div style={{ fontSize: 15, fontWeight: 900, color: '#065F46', lineHeight: 1.1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {loading ? '—' : `Rp ${fmt(report?.todaySales)}`}
                  </div>
                </div>
              </div>

              {/* Total Penjualan */}
              <div style={{
                background: 'linear-gradient(135deg,#ECFDF5,#D1FAE5)',
                border: '1px solid #6EE7B7', borderRadius: 14, padding: '12px 14px',
                display: 'flex', alignItems: 'center', gap: 10, flex: 1, minWidth: 0
              }}>
                <div style={{ background: '#059669', borderRadius: 10, padding: 8, display: 'flex', flexShrink: 0 }}>
                  <TrendingUp size={16} color="white" />
                </div>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontSize: 9, color: '#065F46', fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.5 }}>Total Penjualan</div>
                  <div style={{ fontSize: 15, fontWeight: 900, color: '#065F46', lineHeight: 1.1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {loading ? '—' : `Rp ${fmt(report?.totalSales)}`}
                  </div>
                </div>
              </div>
            </div>

            {/* Pendapatan Bersih */}
            <div style={{
              background: 'linear-gradient(135deg,#EEF2FF,#E0E7FF)',
              border: '1px solid #A5B4FC', borderRadius: 14, padding: '12px 16px',
              display: 'flex', alignItems: 'center', gap: 12, flex: 1
            }}>
              <div style={{ background: '#4F46E5', borderRadius: 12, padding: 10, display: 'flex' }}>
                <BarChart2 size={20} color="white" />
              </div>
              <div>
                <div style={{ fontSize: 10, color: '#3730A3', fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1 }}>Pendapatan Bersih</div>
                <div style={{ fontSize: 18, fontWeight: 900, color: '#3730A3', lineHeight: 1.1 }}>
                  {loading ? '—' : `Rp ${fmt(report?.netIncome)}`}
                </div>
              </div>
            </div>

            {/* Piutang */}
            <div style={{
              background: 'linear-gradient(135deg,#FEF3C7,#FDE68A)',
              border: '1px solid #FCD34D', borderRadius: 14, padding: '12px 16px',
              display: 'flex', alignItems: 'center', gap: 12, flex: 1
            }}>
              <div style={{ background: '#F59E0B', borderRadius: 12, padding: 10, display: 'flex' }}>
                <ShoppingBag size={20} color="white" />
              </div>
              <div>
                <div style={{ fontSize: 10, color: '#78350F', fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1 }}>Piutang Tertunda</div>
                <div style={{ fontSize: 18, fontWeight: 900, color: '#78350F', lineHeight: 1.1 }}>
                  {loading ? '—' : `Rp ${fmt(report?.pendingReceivables)}`}
                </div>
              </div>
            </div>

          </div>
        )}

        {/* Alerts + Stok column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, flex: 1, minWidth: 0 }}>

          {appMode === 'RENTAL' ? (
            <>
              {/* Mobil availability summary */}
              <div style={{
                background: 'white', border: '1px solid #E5E7EB',
                borderRadius: 14, padding: '12px 16px', flex: 1, overflow: 'hidden'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
                  <Package size={16} color="#6B7280" />
                  <span style={{ fontSize: 12, fontWeight: 700, color: '#374151' }}>Status Armada Mobil</span>
                </div>
                <div style={{ display: 'flex', gap: 10 }}>
                  <div style={{ flex: 1, background: '#EEF2FF', borderRadius: 10, padding: '8px 10px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 900, color: '#4F46E5' }}>{cars.length}</div>
                    <div style={{ fontSize: 10, color: '#4338CA', fontWeight: 600 }}>Total Mobil</div>
                  </div>
                  <div style={{ flex: 1, background: '#F0FDF4', borderRadius: 10, padding: '8px 10px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 900, color: '#16A34A' }}>{cars.filter(c => c.status === 'AVAILABLE').length}</div>
                    <div style={{ fontSize: 10, color: '#15803D', fontWeight: 600 }}>Tersedia</div>
                  </div>
                  <div style={{ flex: 1, background: '#FFFBEB', borderRadius: 10, padding: '8px 10px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 900, color: '#D97706' }}>{cars.filter(c => c.status === 'RENTED').length}</div>
                    <div style={{ fontSize: 10, color: '#92400E', fontWeight: 600 }}>Disewa</div>
                  </div>
                </div>
              </div>

              {/* Active rentals summary */}
              <div style={{
                background: '#F5F3FF', border: '1px solid #DDD6FE',
                borderRadius: 14, padding: '16px 18px', flex: 1, overflow: 'hidden',
                display: 'flex', flexDirection: 'column', justifyContent: 'center'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                  <Clock size={18} color="#8B5CF6" />
                  <span style={{ fontSize: 13, fontWeight: 800, color: '#6D28D9' }}>Penyewaan Aktif</span>
                </div>
                <div style={{ fontSize: 13, color: '#5B21B6', lineHeight: 1.6 }}>
                  {rentals.filter(r => r.status === 'ACTIVE').length > 0 ? (
                    <div>🚗 <b>{rentals.filter(r => r.status === 'ACTIVE').length} mobil</b> sedang aktif disewa saat ini.</div>
                  ) : (
                    <div>✅ Semua armada terparkir rapi di garasi.</div>
                  )}
                </div>
              </div>
            </>
          ) : appMode === 'LAUNDRY' ? (
            <>
              {/* Laundry status summary */}
              <div style={{
                background: 'white', border: '1px solid #E5E7EB',
                borderRadius: 14, padding: '12px 16px', flex: 1, overflow: 'hidden'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
                  <Package size={16} color="#6B7280" />
                  <span style={{ fontSize: 12, fontWeight: 700, color: '#374151' }}>Status Antrean Laundry</span>
                </div>
                <div style={{ display: 'flex', gap: 10 }}>
                  <div style={{ flex: 1, background: '#EEF2FF', borderRadius: 10, padding: '8px 10px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 900, color: '#4F46E5' }}>{laundryOrders.length}</div>
                    <div style={{ fontSize: 10, color: '#4338CA', fontWeight: 600 }}>Total Order</div>
                  </div>
                  <div style={{ flex: 1, background: '#FFFBEB', borderRadius: 10, padding: '8px 10px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 900, color: '#D97706' }}>{laundryOrders.filter(o => o.status === 'Menunggu' || o.status === 'Proses').length}</div>
                    <div style={{ fontSize: 10, color: '#92400E', fontWeight: 600 }}>Antre / Proses</div>
                  </div>
                  <div style={{ flex: 1, background: '#F0FDF4', borderRadius: 10, padding: '8px 10px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 900, color: '#16A34A' }}>{laundryOrders.filter(o => o.status === 'Selesai').length}</div>
                    <div style={{ fontSize: 10, color: '#15803D', fontWeight: 600 }}>Siap Diambil</div>
                  </div>
                </div>
              </div>

              {/* Active orders detailed summary */}
              <div style={{
                background: '#EFF6FF', border: '1px solid #BFDBFE',
                borderRadius: 14, padding: '16px 18px', flex: 1, overflow: 'hidden',
                display: 'flex', flexDirection: 'column', justifyContent: 'center'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                  <Clock size={18} color="#3B82F6" />
                  <span style={{ fontSize: 13, fontWeight: 800, color: '#1D4ED8' }}>Cucian Diproses</span>
                </div>
                <div style={{ fontSize: 13, color: '#1E40AF', lineHeight: 1.6 }}>
                  {laundryOrders.filter(o => o.status === 'Proses').length > 0 ? (
                    <div>🧺 <b>{laundryOrders.filter(o => o.status === 'Proses').length} order</b> sedang dicuci/dikeringkan.</div>
                  ) : (
                    <div>✅ Semua cucian selesai dikerjakan.</div>
                  )}
                </div>
              </div>
            </>
          ) : (
            <>
              {/* Stok summary */}
              <div style={{
                background: 'white', border: '1px solid #E5E7EB',
                borderRadius: 14, padding: '12px 16px', flex: 1, overflow: 'hidden'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
                  <Package size={16} color="#6B7280" />
                  <span style={{ fontSize: 12, fontWeight: 700, color: '#374151' }}>Status Stok</span>
                </div>
                <div style={{ display: 'flex', gap: 10 }}>
                  <div style={{ flex: 1, background: '#F0FDF4', borderRadius: 10, padding: '8px 10px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 900, color: '#16A34A' }}>{products.length}</div>
                    <div style={{ fontSize: 10, color: '#15803D', fontWeight: 600 }}>Total Produk</div>
                  </div>
                  <div style={{ flex: 1, background: '#FFFBEB', borderRadius: 10, padding: '8px 10px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 900, color: '#D97706' }}>{lowStock.length}</div>
                    <div style={{ fontSize: 10, color: '#92400E', fontWeight: 600 }}>Stok Menipis</div>
                  </div>
                  <div style={{ flex: 1, background: '#FEF2F2', borderRadius: 10, padding: '8px 10px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 900, color: '#DC2626' }}>{outStock.length}</div>
                    <div style={{ fontSize: 10, color: '#991B1B', fontWeight: 600 }}>Stok Habis</div>
                  </div>
                </div>
              </div>

              {/* Alert produk */}
              {(lowStock.length > 0 || outStock.length > 0) ? (
                <div style={{
                  background: '#FFFBEB', border: '1px solid #FCD34D',
                  borderRadius: 14, padding: '16px 18px', flex: 1, overflow: 'hidden',
                  display: 'flex', flexDirection: 'column', justifyContent: 'center'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                    <AlertTriangle size={18} color="#D97706" />
                    <span style={{ fontSize: 13, fontWeight: 800, color: '#92400E' }}>Perlu Perhatian</span>
                  </div>
                  <div style={{ fontSize: 13, color: '#78350F', lineHeight: 1.6, overflow: 'hidden' }}>
                    {outStock.length > 0 && (
                      <div style={{ marginBottom: 4 }}>🚫 <b>Habis:</b> {outStock.slice(0, 3).map(p => p.name).join(', ')}{outStock.length > 3 ? ` +${outStock.length - 3} lagi` : ''}</div>
                    )}
                    {lowStock.length > 0 && (
                      <div>⚠️ <b>Menipis:</b> {lowStock.slice(0, 3).map(p => `${p.name} (${p.stock})`).join(', ')}{lowStock.length > 3 ? ` +${lowStock.length - 3} lagi` : ''}</div>
                    )}
                  </div>
                </div>
              ) : (
                <div style={{
                  background: '#F0FDF4', border: '1px solid #86EFAC',
                  borderRadius: 14, padding: '20px 16px', flex: 1,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexDirection: 'column', gap: 8
                }}>
                  <div style={{ fontSize: 36 }}>✅</div>
                  <div style={{ fontSize: 14, fontWeight: 800, color: '#166534', textAlign: 'center' }}>Semua stok aman</div>
                </div>
              )}
            </>
          )}

        </div>
      </div>

      {/* ── Menu shortcut grid ─────────────────────────────────── */}
      <div className="db-shortcut-grid">
        {menus.map((m, i) => (
          <Link
            key={i}
            to={m.path}
            style={{
              background: m.grad,
              borderRadius: 14, padding: '12px 10px',
              display: 'flex', flexDirection: 'column',
              alignItems: 'center', gap: 6,
              textDecoration: 'none', color: 'white',
              boxShadow: '0 4px 12px rgba(0,0,0,0.12)',
              transition: 'transform 0.15s, box-shadow 0.15s',
            }}
            onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-2px)'; e.currentTarget.style.boxShadow = '0 8px 20px rgba(0,0,0,0.18)'; }}
            onMouseLeave={e => { e.currentTarget.style.transform = ''; e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.12)'; }}
          >
            <div style={{ background: 'rgba(255,255,255,0.2)', borderRadius: 10, padding: 8, display: 'flex' }}>
              {m.icon}
            </div>
            <div style={{ fontSize: 11, fontWeight: 700, textAlign: 'center' }}>{m.label}</div>
            <ArrowRight size={12} style={{ opacity: 0.7 }} />
          </Link>
        ))}
      </div>

    </div>
  );
}
