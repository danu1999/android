import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Wallet, Contact, Users, Globe,
  TrendingUp, ShoppingBag, Package, AlertTriangle,
  ArrowRight, BarChart2, Clock
} from 'lucide-react';
import api from '../api';
import { useAuth, useIsAdmin, useIsOwner } from '../AuthContext';
import { useDemoBlock } from '../AuthContext';

export default function Dashboard() {
  const { user } = useAuth();
  const isAdmin = useIsAdmin();
  const isOwner = useIsOwner();
  const { isDemo } = useDemoBlock();

  const [report, setReport] = useState(null);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);

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
      if (isDemo) {
        setReport(DEMO_REPORT);
        setProducts(DEMO_PRODUCTS);
        setLoading(false);
        return;
      }
      try {
        const [rRes, pRes] = await Promise.all([
          api.get('/reports'),
          api.get('/products'),
        ]);
        setReport(rRes.data);
        setProducts(pRes.data);
      } catch (_) { }
      finally { setLoading(false); }
    };
    load();
  }, []);

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
    { path: '/toko-online', label: 'Toko Online', icon: <Globe size={22} />, grad: 'linear-gradient(135deg,#8B5CF6,#6D28D9)', show: true },
  ].filter(m => m.show);

  return (
    <div style={{
      height: '100%',
      display: 'flex',
      flexDirection: 'column',
      gap: 14,
      padding: '1rem',
      boxSizing: 'border-box',
      overflow: 'hidden',
    }}>

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
      <div style={{ display: 'flex', gap: 14, flex: 1, minHeight: 0 }}>

        {/* Stats column */}
        {isAdmin && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, flex: 1, minWidth: 0 }}>

            {/* Row 1: Penjualan Hari Ini & Total Penjualan */}
            <div style={{ display: 'flex', gap: 10, flex: 1 }}>
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

        </div>
      </div>

      {/* ── Menu shortcut grid ─────────────────────────────────── */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: `repeat(${menus.length}, 1fr)`,
        gap: 10, flexShrink: 0,
      }}>
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
