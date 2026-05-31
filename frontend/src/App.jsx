import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Link, useLocation, Navigate, useNavigate } from 'react-router-dom';
import {
  Calculator, Package, LayoutDashboard, LogOut, Clock,
  DollarSign, Users, UserCog, Crown, ShieldCheck, Lock, X, Sparkles, History, Car, WifiOff,
  Sun, Moon, FileText, RefreshCw, List, Cpu, Settings as SettingsIcon
} from 'lucide-react';
import Kasir from './pages/Kasir';
import Katalog from './pages/Katalog';
import Keuangan from './pages/Keuangan';
import Pelanggan from './pages/Pelanggan';
import Karyawan from './pages/Karyawan';
import TokoOnline from './pages/TokoOnline';
import Dashboard from './pages/Dashboard';
import Pesanan from './pages/Pesanan';
import Supplier from './pages/Supplier';
import Login from './pages/Login';
import LogAktivitas from './pages/LogAktivitas';
import RentalMobil from './pages/RentalMobil';
import KasirLaundry from './pages/KasirLaundry';
import LayananLaundry from './pages/LayananLaundry';
import OrderLaundry from './pages/OrderLaundry';
import StrukLaundry from './pages/StrukLaundry';
import { AuthContext, DemoContext, hasRole, DEMO_LIMITS, useAuth } from './AuthContext';
import { syncOfflineWrites } from './api';
import { App as CapApp } from '@capacitor/app';

// BMP Imports
import BmpDashboard from './pages/bmp/Dashboard';
import BmpClients from './pages/bmp/Clients';
import BmpProducts from './pages/bmp/Products';
import BmpSettings from './pages/bmp/Settings';
import BmpCashFlow from './pages/bmp/CashFlow';
import BmpInvoices from './pages/bmp/Invoices';
import BmpCreateInvoice from './pages/bmp/CreateInvoice';
import BmpBahanNono from './pages/bmp/BahanNono';
import BmpHppCalculator from './pages/bmp/HppCalculator';
import BmpPricelist from './pages/bmp/Pricelist';
import BmpEmployees from './pages/bmp/Employees';
import BmpPayroll from './pages/bmp/Payroll';
import BmpAccessDenied from './pages/bmp/AccessDenied';
import BmpLogin from './pages/bmp/Login';
import BmpBonusClaim from './pages/bmp/BonusClaim';
import { AuthProvider as BmpAuthProvider, AuthContext as BmpAuthContext } from './contexts/BmpAuthContext';

import './index.css';

// ─── Role-based page access ────────────────────────────────────
const ROLE_ACCESS = {
  KASIR: ['/', '/katalog', '/orders-laundry'],
  CASHIER: ['/', '/katalog', '/orders-laundry'],
  ADMIN: ['/', '/katalog', '/dashboard', '/keuangan', '/pelanggan', '/karyawan', '/pesanan', '/supplier', '/orders-laundry', '/layanan-laundry'],
  OWNER: ['/', '/katalog', '/dashboard', '/keuangan', '/pelanggan', '/karyawan', '/pesanan', '/supplier', '/activity-logs', '/orders-laundry', '/layanan-laundry'],
};

const canAccess = (role, path) => {
  const allowed = ROLE_ACCESS[role] || ROLE_ACCESS['KASIR'];
  if (path.startsWith('/struk-laundry/')) return true;
  return allowed.includes(path);
};

const BmpProtectedRoute = ({ children }) => {
  const { token } = React.useContext(BmpAuthContext);
  if (!token) {
    return <Navigate to="/bmp-login" replace />;
  }
  return children;
};

// ─── Demo Upgrade Modal ────────────────────────────────────────
function DemoBlockModal({ message, onClose }) {
  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 99999,
      background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(6px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem'
    }}>
      <div style={{
        background: 'white', borderRadius: '24px', padding: '2rem',
        maxWidth: '360px', width: '100%', textAlign: 'center',
        boxShadow: '0 25px 50px rgba(0,0,0,0.3)',
        animation: 'slideUp 0.25s ease'
      }}>
        {/* Icon */}
        <div style={{
          width: '72px', height: '72px', borderRadius: '50%',
          background: 'linear-gradient(135deg, #F59E0B, #EF4444)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          margin: '0 auto 1.25rem', boxShadow: '0 8px 24px rgba(245,158,11,0.35)'
        }}>
          <Lock size={32} color="white" />
        </div>

        <h2 style={{ margin: '0 0 8px', fontSize: '1.3rem', fontWeight: 800, color: '#111827' }}>
          Fitur Premium
        </h2>
        <p style={{ margin: '0 0 1.5rem', color: '#6B7280', fontSize: '0.9rem', lineHeight: 1.6 }}>
          {message || 'Fitur ini hanya tersedia di akun berbayar. Upgrade sekarang untuk akses penuh!'}
        </p>

        {/* CTA */}
        <a
          href="https://wa.me/6282245077959?text=Halo%2C%20saya%20ingin%20berlangganan%20POSBah"
          target="_blank"
          rel="noreferrer"
          style={{
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
            width: '100%', padding: '14px', borderRadius: '14px', border: 'none',
            background: 'linear-gradient(135deg, #10B981, #059669)',
            color: 'white', fontWeight: 800, fontSize: '1rem', cursor: 'pointer',
            textDecoration: 'none', marginBottom: '10px',
            boxShadow: '0 4px 14px rgba(16,185,129,0.4)'
          }}
        >
          <Sparkles size={18} /> Hubungi Kami untuk Upgrade
        </a>
        <button
          onClick={onClose}
          style={{
            width: '100%', padding: '12px', borderRadius: '14px',
            border: '1.5px solid #E5E7EB', background: 'white',
            color: '#6B7280', fontWeight: 700, fontSize: '0.9rem', cursor: 'pointer'
          }}
        >
          Kembali ke Demo
        </button>

        <p style={{ marginTop: '1rem', fontSize: '0.75rem', color: '#9CA3AF' }}>
          ✅ Kamu masih bisa menjelajahi semua fitur dalam mode demo
        </p>
      </div>
    </div>
  );
}

// Badge warna per role
const ROLE_STYLE = {
  KASIR: { bg: '#EEF2FF', color: '#4F46E5', icon: <Calculator size={10} /> },
  CASHIER: { bg: '#EEF2FF', color: '#4F46E5', icon: <Calculator size={10} /> },
  ADMIN: { bg: '#F0FDF4', color: '#16A34A', icon: <ShieldCheck size={10} /> },
  OWNER: { bg: '#FEF3C7', color: '#D97706', icon: <Crown size={10} /> },
};

const ThemeToggle = ({ theme, toggleTheme }) => {
  return (
    <button
      onClick={toggleTheme}
      style={{
        background: 'none',
        border: 'none',
        cursor: 'pointer',
        padding: '6px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: '50%',
        color: theme === 'dark' ? '#F59E0B' : '#4F46E5',
        transition: 'all 0.2s ease',
      }}
      className="theme-toggle-btn"
      title={theme === 'dark' ? 'Mode Terang' : 'Mode Gelap'}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'scale(1.1) rotate(15deg)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'scale(1) rotate(0deg)';
      }}
    >
      {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  );
};

// ─── Navigation ────────────────────────────────────────────────
const Navigation = ({ user, onLogout, appMode, setAppMode, theme, toggleTheme }) => {
  const location = useLocation();
  const roleStyle = ROLE_STYLE[user?.role] || ROLE_STYLE['KASIR'];
  const { logout: bmpLogout } = React.useContext(BmpAuthContext);

  const isExcludedName = (name) => {
    if (!name) return false;
    return ['hanafi', 'fed', 'fahri'].includes(name.toLowerCase());
  };

  const allNavItems = appMode === 'RENTAL' ? [
    { path: '/', label: 'Kasir Rental', icon: <Car size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/dashboard', label: 'Dashboard', icon: <LayoutDashboard size={20} />, minRole: 'ADMIN', showInNav: true },
    { path: '/keuangan', label: 'Keuangan', icon: <DollarSign size={20} />, minRole: 'ADMIN', showInNav: false },
    { path: '/pelanggan', label: 'Pelanggan', icon: <Users size={20} />, minRole: 'ADMIN', showInNav: false },
    { path: '/karyawan', label: 'Karyawan', icon: <UserCog size={20} />, minRole: 'ADMIN', showInNav: false },
    { path: '/activity-logs', label: 'Log Aktivitas', icon: <History size={20} />, minRole: 'ADMIN', showInNav: false },
  ] : appMode === 'LAUNDRY' ? [
    { path: '/', label: 'Kasir Laundry', icon: <Calculator size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/orders-laundry', label: 'Riwayat Order', icon: <Clock size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/layanan-laundry', label: 'Tarif Layanan', icon: <Package size={20} />, minRole: 'ADMIN', showInNav: true },
    { path: '/dashboard', label: 'Dashboard', icon: <LayoutDashboard size={20} />, minRole: 'ADMIN', showInNav: true },
    { path: '/keuangan', label: 'Keuangan', icon: <DollarSign size={20} />, minRole: 'ADMIN', showInNav: false },
    { path: '/pelanggan', label: 'Pelanggan', icon: <Users size={20} />, minRole: 'ADMIN', showInNav: false },
    { path: '/karyawan', label: 'Karyawan', icon: <UserCog size={20} />, minRole: 'ADMIN', showInNav: false },
    { path: '/activity-logs', label: 'Log Aktivitas', icon: <History size={20} />, minRole: 'ADMIN', showInNav: false },
  ] : appMode === 'BMP' ? [
    { path: '/', label: 'Panel Utama', icon: <LayoutDashboard size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/invoices', label: 'Daftar Faktur', icon: <FileText size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/products', label: 'Master Barang', icon: <Package size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/clients', label: 'Data Pelanggan', icon: <Users size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/bahan-nono', label: 'Bahan Mas Nono', icon: <Package size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/kas', label: 'Kas Keuangan', icon: <RefreshCw size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/pricelist', label: 'Pricelist Harga', icon: <List size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/hpp-calculator', label: 'Kalkulator HPP', icon: <Cpu size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/employees', label: 'Data Karyawan', icon: <Users size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/payroll', label: 'Sistem Penggajian', icon: <DollarSign size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/settings', label: 'Pengaturan', icon: <SettingsIcon size={20} />, minRole: 'KASIR', showInNav: true },
  ] : [
    { path: '/', label: 'Kasir', icon: <Calculator size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/katalog', label: 'Katalog', icon: <Package size={20} />, minRole: 'KASIR', showInNav: true },
    { path: '/dashboard', label: 'Dashboard', icon: <LayoutDashboard size={20} />, minRole: 'ADMIN', showInNav: true },
    { path: '/keuangan', label: 'Keuangan', icon: <DollarSign size={20} />, minRole: 'ADMIN', showInNav: false },
    { path: '/pelanggan', label: 'Pelanggan', icon: <Users size={20} />, minRole: 'ADMIN', showInNav: false },
    { path: '/karyawan', label: 'Karyawan', icon: <UserCog size={20} />, minRole: 'ADMIN', showInNav: false },
    { path: '/pesanan', label: 'Pesanan', icon: <Clock size={20} />, minRole: 'ADMIN', showInNav: false },
    { path: '/supplier', label: 'Supplier', icon: <UserCog size={20} />, minRole: 'ADMIN', showInNav: false },
    { path: '/activity-logs', label: 'Log Aktivitas', icon: <History size={20} />, minRole: 'ADMIN', showInNav: false },
  ];

  // Demo user diperlakukan sebagai OWNER untuk navigasi (akses semua halaman)
  const effectiveRole = user?.isDemo ? 'OWNER' : user?.role;
  const navItems = appMode === 'BMP'
    ? allNavItems.filter(item => item.showInNav)
    : allNavItems.filter(item => hasRole(effectiveRole, item.minRole) && item.showInNav);

  const showModeSwitcher = !!user && hasRole(user?.role, 'ADMIN') && !isExcludedName(user?.name) && !user?.isDemo;

  const handleModeChange = (newMode) => {
    localStorage.setItem('posbah_app_mode', newMode);
    setAppMode(newMode);
  };

  const RoleBadge = () => {
    return (
      <span style={{
        fontSize: '0.65rem',
        background: user?.isDemo
          ? 'linear-gradient(90deg,#7C3AED,#4F46E5)'
          : roleStyle.bg,
        color: user?.isDemo ? 'white' : roleStyle.color,
        padding: '2px 7px', borderRadius: '99px', fontWeight: 700,
        display: 'inline-flex', alignItems: 'center', gap: 3,
      }}>
        {user?.isDemo
          ? '⚡ ULTRA DEMO'
          : <>{roleStyle.icon} {user?.role}</>}
      </span>
    );
  };

  const handleLogoutClick = () => {
    if (appMode === 'BMP') {
      bmpLogout();
    }
    onLogout();
  };

  return (
    <>
      {/* Mobile Top Header */}
      <div className="mobile-header glass-panel md-hidden" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div className="brand" style={{ fontSize: '1.1rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <img src="/logo.png" alt="Logo" style={{ width: '24px', height: '24px', borderRadius: '6px', objectFit: 'cover' }} />
          {appMode === 'BMP' ? 'BAHTERA MULYA' : 'POSBah'}
          <ThemeToggle theme={theme} toggleTheme={toggleTheme} />
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {user?.name?.toLowerCase() !== 'muizz' && user?.name && (
            <>
              <span style={{ fontSize: '0.75rem', color: '#1F2937', fontWeight: 600 }}>{user?.name}</span>
              <RoleBadge />
            </>
          )}
          {appMode === 'BMP' && !user && (
            <span style={{ fontSize: '0.75rem', color: '#1F2937', fontWeight: 600 }}>BMP Admin</span>
          )}
          <button onClick={handleLogoutClick} title="Keluar" style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#6B7280', padding: '4px' }}>
            <LogOut size={16} />
          </button>
        </div>
      </div>

      {/* Sidebar (Desktop) */}
      <aside className="sidebar glass-panel">
        <div className="brand hidden md-block" style={{ display: 'flex', alignItems: 'center', gap: '10px', justifyContent: 'space-between', width: '100%' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <img src="/logo.png" alt="Logo" style={{ width: '32px', height: '32px', borderRadius: '8px', objectFit: 'cover' }} />
            {appMode === 'BMP' ? 'BAHTERA MULYA' : 'POSBah'}
          </div>
          <ThemeToggle theme={theme} toggleTheme={toggleTheme} />
        </div>
        <nav>
          {navItems.map(item => (
            <Link key={item.path} to={item.path} className={location.pathname === item.path ? 'active' : ''}>
              {item.icon} <span>{item.label}</span>
            </Link>
          ))}
        </nav>
        <div style={{ marginTop: 'auto', paddingTop: '1rem', borderTop: '1px solid rgba(0,0,0,0.08)' }}>
          {showModeSwitcher && (
            <div style={{ marginBottom: '12px' }}>
              <div style={{ fontSize: '0.7rem', color: '#6B7280', fontWeight: 800, marginBottom: '4px', textTransform: 'uppercase' }}>Mode POS</div>
              <select
                value={appMode}
                onChange={(e) => handleModeChange(e.target.value)}
                style={{ width: '100%', padding: '6px 10px', borderRadius: '8px', border: '1.5px solid #E5E7EB', fontSize: '0.8rem', fontWeight: 800, background: 'white', color: '#374151', cursor: 'pointer' }}
              >
                <option value="FNB">🍹 UMKM & Jus</option>
                <option value="RENTAL">🚗 Rental Mobil</option>
                <option value="LAUNDRY">🧺 POS Laundry</option>
                <option value="BMP">🏭 Invoice &amp; Manufaktur</option>
              </select>
            </div>
          )}
          {user?.name?.toLowerCase() !== 'muizz' && user?.name && (
            <>
              <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#1F2937', marginBottom: '4px' }}>{user?.name}</div>
              <RoleBadge />
            </>
          )}
          {appMode === 'BMP' && !user && (
            <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#1F2937', marginBottom: '4px' }}>BMP Admin</div>
          )}
          <button onClick={handleLogoutClick} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', color: '#EF4444', background: 'none', border: 'none', cursor: 'pointer', padding: '8px 0 0', fontWeight: 600 }}>
            <LogOut size={14} /> Keluar
          </button>
        </div>
      </aside>

      {/* Mobile Bottom Nav */}
      <nav className="mobile-bottom-nav glass-panel md-hidden">
        {navItems.map(item => (
          <Link key={item.path} to={item.path} className={location.pathname === item.path ? 'active' : ''}>
            {item.icon}
            <span className="text-xs">{item.label}</span>
          </Link>
        ))}
      </nav>
    </>
  );
};

// ─── App Content ───────────────────────────────────────────────
function AppContent({ user, onLogout, appMode, setAppMode, theme, toggleTheme }) {
  const location = useLocation();
  const isPublicStore = location.pathname === '/toko-online';

  if (isPublicStore) return <TokoOnline />;

  // Demo: akses semua halaman (OWNER level)
  const effectiveRole = user?.isDemo ? 'OWNER' : user?.role;
  const currentPath = location.pathname;

  const isBmpPath = [
    '/invoices', '/invoices/create', '/products', '/clients', '/bahan-nono',
    '/kas', '/pricelist', '/hpp-calculator', '/employees', '/payroll',
    '/settings', '/bonus', '/bmp-login'
  ].some(p => location.pathname === p || location.pathname.startsWith(p + '/'));

  if (!isBmpPath && !canAccess(effectiveRole, currentPath) && currentPath !== '/toko-online') {
    return <Navigate to="/" replace />;
  }

  const isTargetUser = user?.name && ['hanafi', 'fed', 'fahri'].includes(user.name.toLowerCase().trim());

  return (
    <div className="app-layout select-none">
      <Navigation user={user} onLogout={onLogout} appMode={appMode} setAppMode={setAppMode} theme={theme} toggleTheme={toggleTheme} />
      <main className="main-content">
        {isTargetUser && (
          <div style={{
            background: theme === 'dark' ? '#7c2d12' : '#fef3c7',
            border: theme === 'dark' ? '1px solid #9a3412' : '1px solid #fde68a',
            borderRadius: '8px',
            padding: '8px 12px',
            marginBottom: '12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '8px',
            fontSize: '0.75rem',
            fontFamily: "'Inter', sans-serif"
          }} className="trial-warning-banner">
            <div style={{ color: theme === 'dark' ? '#ffedd5' : '#78350f', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ fontSize: '1rem' }}>⚠️</span>
              <span>Trial berakhir 1 Juni. Segera bayar agar akses Web & APK tidak terputus.</span>
            </div>
            <a
              href={`https://wa.me/6282245077959?text=Halo%20Admin%20POSBah%2C%20saya%20(${encodeURIComponent(user.name)})%20ingin%20melakukan%20pembayaran%20langganan%20POSBah`}
              target="_blank"
              rel="noreferrer"
              style={{
                color: theme === 'dark' ? '#fdba74' : '#b45309',
                fontWeight: 800,
                textDecoration: 'underline',
                whiteSpace: 'nowrap'
              }}
            >
              Bayar Sekarang
            </a>
          </div>
        )}
        <Routes>
          <Route path="/" element={
            appMode === 'RENTAL' ? <RentalMobil /> :
              appMode === 'LAUNDRY' ? <KasirLaundry /> :
                appMode === 'BMP' ? <BmpProtectedRoute><BmpDashboard /></BmpProtectedRoute> :
                  <Kasir />
          } />
          <Route path="/katalog" element={<Katalog />} />
          <Route path="/orders-laundry" element={<OrderLaundry />} />
          <Route path="/layanan-laundry" element={<LayananLaundry />} />
          <Route path="/struk-laundry/:id" element={<StrukLaundry />} />
          {canAccess(effectiveRole, '/dashboard') && <Route path="/dashboard" element={<Dashboard appMode={appMode} />} />}
          {canAccess(effectiveRole, '/keuangan') && <Route path="/keuangan" element={<Keuangan appMode={appMode} />} />}
          {canAccess(effectiveRole, '/pelanggan') && <Route path="/pelanggan" element={<Pelanggan />} />}
          {canAccess(effectiveRole, '/karyawan') && <Route path="/karyawan" element={<Karyawan />} />}
          {canAccess(effectiveRole, '/pesanan') && <Route path="/pesanan" element={<Pesanan />} />}
          {canAccess(effectiveRole, '/supplier') && <Route path="/supplier" element={<Supplier />} />}
          {canAccess(effectiveRole, '/activity-logs') && <Route path="/activity-logs" element={<LogAktivitas />} />}

          {/* BMP Routes */}
          <Route path="/bmp-login" element={<BmpLogin />} />
          <Route path="/bonus" element={<BmpBonusClaim />} />
          <Route path="/invoices" element={<BmpProtectedRoute><BmpInvoices /></BmpProtectedRoute>} />
          <Route path="/invoices/create" element={<BmpProtectedRoute><BmpCreateInvoice /></BmpProtectedRoute>} />
          <Route path="/products" element={<BmpProtectedRoute><BmpProducts /></BmpProtectedRoute>} />
          <Route path="/clients" element={<BmpProtectedRoute><BmpClients /></BmpProtectedRoute>} />
          <Route path="/bahan-nono" element={<BmpProtectedRoute><BmpBahanNono /></BmpProtectedRoute>} />
          <Route path="/kas" element={<BmpProtectedRoute><BmpCashFlow /></BmpProtectedRoute>} />
          <Route path="/pricelist" element={<BmpProtectedRoute><BmpPricelist /></BmpProtectedRoute>} />
          <Route path="/hpp-calculator" element={<BmpProtectedRoute><BmpHppCalculator /></BmpProtectedRoute>} />
          <Route path="/employees" element={<BmpProtectedRoute><BmpEmployees /></BmpProtectedRoute>} />
          <Route path="/payroll" element={<BmpProtectedRoute><BmpPayroll /></BmpProtectedRoute>} />
          <Route path="/settings" element={<BmpProtectedRoute><BmpSettings /></BmpProtectedRoute>} />
          <Route path="/access-denied" element={<BmpProtectedRoute><BmpAccessDenied /></BmpProtectedRoute>} />
        </Routes>
      </main>
    </div>
  );
}

// ─── Demo Banner ───────────────────────────────────────────────
function DemoBanner() {
  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, zIndex: 9999,
      background: 'linear-gradient(90deg, #7C3AED, #4F46E5)',
      color: 'white', padding: '0 16px',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      minHeight: '36px', gap: '8px', fontSize: '0.78rem', fontWeight: 700,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
        <span>⚡ Mode Ultra (Simulasi)</span>
        <span style={{ fontWeight: 400, opacity: 0.9 }}>·</span>
        <span style={{ 0.9: undefined, fontWeight: 600 }}>Data Anda sepenuhnya terisolasi dari database produksi</span>
      </div>
      <div style={{ fontSize: '0.7rem', opacity: 0.8 }}>
        Demo Mode &amp; Offline Simulation
      </div>
    </div>
  );
}

// Onboarding Business Mode Selection Modal
function BusinessModeModal({ onSelectMode }) {
  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 99999,
      background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(8px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem'
    }}>
      <div style={{
        background: 'white', borderRadius: '24px', padding: '2rem',
        maxWidth: '680px', width: '100%', textAlign: 'center',
        boxShadow: '0 25px 50px rgba(0,0,0,0.3)',
        animation: 'slideUp 0.3s ease'
      }}>
        <h2 style={{ margin: '0 0 8px', fontSize: '1.4rem', fontWeight: 900, color: '#111827' }}>
          Selamat Datang di POSBah! 👋
        </h2>
        <p style={{ margin: '0 0 1.5rem', color: '#6B7280', fontSize: '0.9rem' }}>
          Silakan pilih jenis sistem POS yang ingin kamu gunakan untuk memulai operasional:
        </p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 140px), 1fr))', gap: '16px', marginBottom: '1.5rem' }}>
          {/* Card 1: UMKM F&B */}
          <div
            onClick={() => onSelectMode('FNB')}
            style={{
              border: '2px solid #E5E7EB', borderRadius: '18px', padding: '1.5rem 1rem',
              cursor: 'pointer', transition: 'all 0.2s', display: 'flex', flexDirection: 'column',
              alignItems: 'center', gap: '10px'
            }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = '#10B981'; e.currentTarget.style.transform = 'translateY(-2px)'; }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = '#E5E7EB'; e.currentTarget.style.transform = ''; }}
          >
            <div style={{ fontSize: '2.5rem' }}>🍹</div>
            <div style={{ fontWeight: 800, fontSize: '0.95rem', color: '#111827' }}>POS UMKM & Retail</div>
            <div style={{ fontSize: '0.75rem', color: '#6B7280', lineHeight: 1.4 }}>
              Cocok untuk Kedai Jus, Pisang Keju, Toko Kelontong, & Kuliner lainnya.
            </div>
          </div>

          {/* Card 2: Rental Mobil */}
          <div
            onClick={() => onSelectMode('RENTAL')}
            style={{
              border: '2px solid #E5E7EB', borderRadius: '18px', padding: '1.5rem 1rem',
              cursor: 'pointer', transition: 'all 0.2s', display: 'flex', flexDirection: 'column',
              alignItems: 'center', gap: '10px'
            }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = '#4F46E5'; e.currentTarget.style.transform = 'translateY(-2px)'; }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = '#E5E7EB'; e.currentTarget.style.transform = ''; }}
          >
            <div style={{ fontSize: '2.5rem' }}>🚗</div>
            <div style={{ fontWeight: 800, fontSize: '0.95rem', color: '#111827' }}>POS Rental Mobil</div>
            <div style={{ fontSize: '0.75rem', color: '#6B7280', lineHeight: 1.4 }}>
              Kelola kendaraan, status penyewaan, kalkulator tarif, pengembalian, biaya bengkel dan margin.
            </div>
          </div>

          {/* Card 3: POS Laundry */}
          <div
            onClick={() => onSelectMode('LAUNDRY')}
            style={{
              border: '2px solid #E5E7EB', borderRadius: '18px', padding: '1.5rem 1rem',
              cursor: 'pointer', transition: 'all 0.2s', display: 'flex', flexDirection: 'column',
              alignItems: 'center', gap: '10px'
            }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = '#3B82F6'; e.currentTarget.style.transform = 'translateY(-2px)'; }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = '#E5E7EB'; e.currentTarget.style.transform = ''; }}
          >
            <div style={{ fontSize: '2.5rem' }}>🧺</div>
            <div style={{ fontWeight: 800, fontSize: '0.95rem', color: '#111827' }}>POS Laundry</div>
            <div style={{ fontSize: '0.75rem', color: '#6B7280', lineHeight: 1.4 }}>
              Kelola cucian kiloan & satuan, status proses, timbangan, pembayaran lunas/belum lunas, & notifikasi WA.
            </div>
          </div>

          {/* Card 4: Invoice & Manufaktur (BMP) */}
          <div
            onClick={() => onSelectMode('BMP')}
            style={{
              border: '2px solid #E5E7EB', borderRadius: '18px', padding: '1.5rem 1rem',
              cursor: 'pointer', transition: 'all 0.2s', display: 'flex', flexDirection: 'column',
              alignItems: 'center', gap: '10px'
            }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = '#3B82F6'; e.currentTarget.style.transform = 'translateY(-2px)'; }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = '#E5E7EB'; e.currentTarget.style.transform = ''; }}
          >
            <div style={{ fontSize: '2.5rem' }}>🏭</div>
            <div style={{ fontWeight: 800, fontSize: '0.95rem', color: '#111827' }}>Invoice &amp; Manufaktur</div>
            <div style={{ fontSize: '0.75rem', color: '#6B7280', lineHeight: 1.4 }}>
              Kelola faktur, master barang, data pelanggan, bahan Baku, kas keuangan, kalkulator HPP Plastik, &amp; payroll karyawan pabrik.
            </div>
          </div>
        </div>

        <p style={{ fontSize: '0.75rem', color: '#9CA3AF', margin: 0 }}>
          pilih sesuai kebutuhan kamu
        </p>
      </div>
    </div>
  );
}

function OfflineBanner() {
  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, zIndex: 99999,
      background: 'linear-gradient(90deg, #EF4444, #B91C1C)',
      color: 'white', padding: '0 16px',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      minHeight: '36px', gap: '8px', fontSize: '0.78rem', fontWeight: 700,
      boxShadow: '0 2px 10px rgba(0,0,0,0.2)'
    }}>
      <span>⚠️ Koneksi Terputus (Mode Offline) · Transaksi &amp; data disimpan secara lokal.</span>
    </div>
  );
}

function OfflineOverlay({ onRetry, onContinueOffline }) {
  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 99999,
      background: 'linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #1e152a 100%)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem',
      fontFamily: "'Inter', sans-serif"
    }}>
      <div style={{
        background: 'rgba(255, 255, 255, 0.03)',
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '28px',
        padding: '2.5rem 2rem',
        maxWidth: '400px',
        width: '100%',
        textAlign: 'center',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
        animation: 'slideUp 0.3s ease'
      }}>
        <div style={{
          width: '72px', height: '72px', borderRadius: '50%',
          background: 'linear-gradient(135deg, rgba(239, 68, 68, 0.15), rgba(239, 68, 68, 0.05))',
          border: '1.5px solid rgba(239, 68, 68, 0.25)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          margin: '0 auto 1.25rem',
          boxShadow: '0 8px 32px rgba(239, 68, 68, 0.1)'
        }}>
          <WifiOff size={32} color="#f87171" />
        </div>

        <h2 style={{ margin: '0 0 10px', fontSize: '1.35rem', fontWeight: 900, color: '#f8fafc', letterSpacing: '-0.025em' }}>
          Koneksi Terputus
        </h2>

        <p style={{ margin: '0 0 1.75rem', color: '#94a3b8', fontSize: '0.85rem', lineHeight: 1.6 }}>
          Koneksi internet Anda terputus. Anda tetap dapat menggunakan fitur Kasir dalam Mode Offline secara terbatas.
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <button
            onClick={onRetry}
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
              width: '100%', padding: '14px', borderRadius: '14px', border: 'none',
              background: 'linear-gradient(135deg, #6366f1, #4f46e5)',
              color: 'white', fontWeight: 800, fontSize: '0.9rem', cursor: 'pointer',
              boxShadow: '0 4px 18px rgba(99, 102, 241, 0.3)'
            }}
          >
            Coba Hubungkan Kembali
          </button>

          <button
            onClick={onContinueOffline}
            style={{
              width: '100%', padding: '12px', borderRadius: '14px',
              border: '1.5px solid rgba(255, 255, 255, 0.08)',
              background: 'rgba(255, 255, 255, 0.02)',
              color: '#94a3b8', fontWeight: 700, fontSize: '0.85rem', cursor: 'pointer'
            }}
          >
            Lanjutkan Mode Offline
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Exit Modal Component ──────────────────────────────────────
function ExitModal({ onClose }) {
  const handleExit = () => {
    try {
      CapApp.exitApp();
    } catch {
      window.close();
    }
  };

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 999999,
      background: 'rgba(15,10,60,0.5)', backdropFilter: 'blur(8px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem',
      fontFamily: "'Inter', sans-serif"
    }}>
      <div className="glass-panel" style={{
        background: 'white',
        borderRadius: '24px',
        padding: '2rem 1.5rem',
        maxWidth: '320px',
        width: '100%',
        textAlign: 'center',
        boxShadow: '0 20px 40px rgba(0,0,0,0.15)',
        border: '1.5px solid #EEF2FF',
      }}>
        <div style={{ fontSize: '3rem', marginBottom: '12px' }}>🚪</div>
        <h2 style={{ margin: '0 0 10px', fontSize: '1.3rem', fontWeight: 800, color: '#1F2937' }}>Keluar Aplikasi?</h2>
        <p style={{ margin: '0 0 24px', color: '#6B7280', fontSize: '0.88rem', lineHeight: 1.5 }}>Apakah Anda yakin ingin keluar dari aplikasi POSBah?</p>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={handleExit}
            style={{
              flex: 1, padding: '12px', borderRadius: '12px', border: 'none',
              background: 'linear-gradient(135deg,#EF4444,#DC2626)',
              color: 'white', fontWeight: 800, fontSize: '0.9rem', cursor: 'pointer',
              boxShadow: '0 4px 12px rgba(239,68,68,0.2)'
            }}
          >
            Keluar
          </button>
          <button
            onClick={onClose}
            style={{
              flex: 1, padding: '12px', borderRadius: '12px',
              border: '1.5px solid #E5E7EB', background: 'white',
              color: '#374151', fontWeight: 800, fontSize: '0.9rem', cursor: 'pointer'
            }}
          >
            Tidak
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Back Button Handler Component ─────────────────────────────
function BackButtonHandler({ setShowExitModal, user }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { token: bmpToken } = React.useContext(BmpAuthContext);

  useEffect(() => {
    let handler;
    const init = async () => {
      try {
        handler = await CapApp.addListener('backButton', () => {
          // Jika di home page, login page, atau tidak terautentikasi, tampilkan exit modal
          const isAuthenticated = !!user || !!bmpToken;
          if (location.pathname === '/' || location.pathname === '/login' || location.pathname === '/bmp-login' || !isAuthenticated) {
            setShowExitModal(true);
          } else {
            // Kembali ke halaman sebelumnya di history
            navigate(-1);
          }
        });
      } catch (e) {
        console.warn('CapApp backButton listener error:', e);
      }
    };
    init();
    return () => {
      if (handler) {
        handler.remove();
      }
    };
  }, [location.pathname, navigate, setShowExitModal, user, bmpToken]);

  return null;
}

// ─── Root App ──────────────────────────────────────────────────
function App() {
  const [isOffline, setIsOffline] = useState(() => !navigator.onLine);
  const [dismissOffline, setDismissOffline] = useState(false);
  const [showExitModal, setShowExitModal] = useState(false);

  const [theme, setTheme] = useState(() => {
    try {
      const stored = localStorage.getItem('posbah_theme');
      if (stored) return stored;
    } catch (e) { }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  const toggleTheme = () => {
    setTheme(prev => (prev === 'dark' ? 'light' : 'dark'));
  };

  useEffect(() => {
    try {
      if (theme === 'dark') {
        document.body.classList.add('dark-theme');
      } else {
        document.body.classList.remove('dark-theme');
      }
      localStorage.setItem('posbah_theme', theme);
    } catch (e) { }
  }, [theme]);

  useEffect(() => {
    const handleOnline = () => {
      setIsOffline(false);
      setDismissOffline(false);
      syncOfflineWrites();
    };
    const handleOffline = () => {
      setIsOffline(true);
      setDismissOffline(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Initial check on startup
    if (navigator.onLine) {
      syncOfflineWrites();
    }

    const handleSyncComplete = (e) => {
      alert(`Koneksi terhubung kembali! Berhasil menyinkronkan ${e.detail.count} data transaksi/perubahan offline.`);
      window.location.reload();
    };

    window.addEventListener('posbah_sync_complete', handleSyncComplete);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('posbah_sync_complete', handleSyncComplete);
    };
  }, []);

  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('posbah_user');
      if (!stored) return null;
      const parsed = JSON.parse(stored);
      return parsed;
    } catch { return null; }
  });

  const isExcludedName = (name) => {
    if (!name) return false;
    return ['hanafi', 'fed', 'fahri'].includes(name.toLowerCase());
  };

  const [appMode, setAppMode] = useState(() => {
    return localStorage.getItem('posbah_app_mode') || 'FNB';
  });

  const [showModePrompt, setShowModePrompt] = useState(false);

  // Demo block modal state
  const [demoBlockMsg, setDemoBlockMsg] = useState(null);
  const showDemoBlock = (message) => setDemoBlockMsg(message || true);

  useEffect(() => {
    if (user) {
      if (isExcludedName(user.name)) {
        localStorage.setItem('posbah_app_mode', 'FNB');
        setAppMode('FNB');
        setShowModePrompt(false);
      } else if (!localStorage.getItem('posbah_app_mode')) {
        setShowModePrompt(true);
      }
    }
  }, [user]);

  const handleSelectMode = (mode) => {
    localStorage.setItem('posbah_app_mode', mode);
    setAppMode(mode);
    setShowModePrompt(false);
  };

  const handleLogin = (userData) => {
    localStorage.setItem('posbah_user', JSON.stringify(userData));
    setUser(userData);
  };

  const handleLogout = () => {
    localStorage.removeItem('posbah_user');
    localStorage.removeItem('posbah_app_mode');
    setUser(null);
  };

  useEffect(() => {
    // Disable right click (context menu)
    const handleContextMenu = (e) => {
      e.preventDefault();
    };

    // Disable keyboard shortcuts (F12, Ctrl+Shift+I, Ctrl+Shift+J, Ctrl+Shift+C, Ctrl+U)
    const handleKeyDown = (e) => {
      if (
        e.keyCode === 123 || // F12
        e.key === 'F12' ||
        (e.ctrlKey && e.shiftKey && (e.key === 'I' || e.key === 'i' || e.keyCode === 73)) || // Ctrl+Shift+I
        (e.ctrlKey && e.shiftKey && (e.key === 'J' || e.key === 'j' || e.keyCode === 74)) || // Ctrl+Shift+J
        (e.ctrlKey && e.shiftKey && (e.key === 'C' || e.key === 'c' || e.keyCode === 67)) || // Ctrl+Shift+C
        (e.ctrlKey && (e.key === 'U' || e.key === 'u' || e.keyCode === 85)) // Ctrl+U
      ) {
        e.preventDefault();
        return false;
      }
    };

    window.addEventListener('contextmenu', handleContextMenu);
    window.addEventListener('keydown', handleKeyDown);

    // DevTools Detection & Auto-Defense Loop
    const devToolsInterval = setInterval(() => {
      const startTime = performance.now();
      debugger;
      const endTime = performance.now();

      // If DevTools is open, the pause on debugger statement will cause a significant delay
      if (endTime - startTime > 100) {
        if (user?.isDemo) {
          // Force logout for demo accounts
          handleLogout();
          window.location.href = 'about:blank';
        }
      }
    }, 500);

    return () => {
      window.removeEventListener('contextmenu', handleContextMenu);
      window.removeEventListener('keydown', handleKeyDown);
      clearInterval(devToolsInterval);
    };
  }, [user]);

  return (
    <AuthContext.Provider value={{ user }}>
      <BmpAuthProvider>
        <DemoContext.Provider value={{ showDemoBlock, isDemo: user?.isDemo === true }}>
          <BrowserRouter>
            <BackButtonHandler setShowExitModal={setShowExitModal} user={user} />
            <MainRouterContent
              user={user}
              handleLogin={handleLogin}
              handleLogout={handleLogout}
              appMode={appMode}
              setAppMode={setAppMode}
              theme={theme}
              toggleTheme={toggleTheme}
              showExitModal={showExitModal}
              setShowExitModal={setShowExitModal}
              showModePrompt={showModePrompt}
              handleSelectMode={handleSelectMode}
              demoBlockMsg={demoBlockMsg}
              setDemoBlockMsg={setDemoBlockMsg}
              isOffline={isOffline}
              dismissOffline={dismissOffline}
              setIsOffline={setIsOffline}
              setDismissOffline={setDismissOffline}
            />
          </BrowserRouter>
        </DemoContext.Provider>
      </BmpAuthProvider>
    </AuthContext.Provider>
  );
}

// ─── Main Router Content Component ─────────────────────────────
function MainRouterContent({
  user, handleLogin, handleLogout,
  appMode, setAppMode,
  theme, toggleTheme,
  showExitModal, setShowExitModal,
  showModePrompt, handleSelectMode,
  demoBlockMsg, setDemoBlockMsg,
  isOffline, dismissOffline, setIsOffline, setDismissOffline
}) {
  const { token: bmpToken } = React.useContext(BmpAuthContext);
  const location = useLocation();

  const isBmpMode = appMode === 'BMP';
  const isAuthenticated = !!user || (!!bmpToken && isBmpMode);

  // Exclude public paths from login check
  const isPublicRoute = location.pathname === '/bmp-login' || location.pathname === '/bonus' || location.pathname === '/toko-online';

  const isTrialExpired = user?.isDemo && user?.expiresAt && Date.now() > user?.expiresAt;

  if (isPublicRoute) {
    return (
      <Routes>
        <Route path="/bmp-login" element={<BmpLogin />} />
        <Route path="/bonus" element={<BmpBonusClaim />} />
        <Route path="/toko-online" element={<TokoOnline />} />
        <Route path="*" element={<Navigate to="/bmp-login" replace />} />
      </Routes>
    );
  }

  if (!isAuthenticated) {
    if (isBmpMode) {
      return (
        <Routes>
          <Route path="/bmp-login" element={<BmpLogin />} />
          <Route path="*" element={<Navigate to="/bmp-login" replace />} />
        </Routes>
      );
    } else {
      return (
        <>
          <Login onLogin={handleLogin} />
          {showExitModal && <ExitModal onClose={() => setShowExitModal(false)} />}
        </>
      );
    }
  }

  if (isTrialExpired) {
    return (
      <>
        <div style={{
          position: 'fixed', inset: 0, zIndex: 99999,
          background: 'linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4c1d95 100%)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem',
          fontFamily: "'Inter', sans-serif"
        }}>
          <div style={{
            background: 'white', borderRadius: '24px', padding: '2.5rem 2rem',
            maxWidth: '400px', width: '100%', textAlign: 'center',
            boxShadow: '0 25px 50px rgba(0,0,0,0.3)',
            animation: 'slideUp 0.25s ease'
          }}>
            {/* Icon */}
            <div style={{
              width: '72px', height: '72px', borderRadius: '50%',
              background: 'linear-gradient(135deg, #EF4444, #B91C1C)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              margin: '0 auto 1.25rem', boxShadow: '0 8px 24px rgba(239,68,68,0.35)'
            }}>
              <Lock size={32} color="white" />
            </div>

            <h2 style={{ margin: '0 0 10px', fontSize: '1.4rem', fontWeight: 900, color: '#111827' }}>
              Masa Percobaan Habis ⏳
            </h2>
            <p style={{ margin: '0 0 1.5rem', color: '#4B5563', fontSize: '0.9rem', lineHeight: 1.6 }}>
              Masa percobaan 3 hari untuk akun Google <strong style={{ color: '#4F46E5' }}>{user.email}</strong> telah berakhir. Silakan hubungi kami untuk berlangganan POSBah agar dapat melanjutkan akses penuh.
            </p>

            {/* CTA */}
            <a
              href={`https://wa.me/6282245077959?text=Halo%2C%20saya%20ingin%20berlangganan%20POSBah%20setelah%20menggunakan%20trial%20Google%20dengan%20email%20${encodeURIComponent(user.email)}`}
              target="_blank"
              rel="noreferrer"
              style={{
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
                width: '100%', padding: '14px', borderRadius: '14px', border: 'none',
                background: 'linear-gradient(135deg, #10B981, #059669)',
                color: 'white', fontWeight: 800, fontSize: '0.95rem', cursor: 'pointer',
                textDecoration: 'none', marginBottom: '12px', boxSizing: 'border-box',
                boxShadow: '0 4px 14px rgba(16,185,129,0.4)'
              }}
            >
              <Sparkles size={18} /> Hubungi WhatsApp (Berlangganan)
            </a>

            <button
              onClick={handleLogout}
              style={{
                width: '100%', padding: '12px', borderRadius: '14px',
                border: '1.5px solid #E5E7EB', background: 'white',
                color: '#6B7280', fontWeight: 700, fontSize: '0.9rem', cursor: 'pointer'
              }}
            >
              Keluar / Ganti Akun
            </button>
          </div>
        </div>
        {showExitModal && <ExitModal onClose={() => setShowExitModal(false)} />}
      </>
    );
  }

  return (
    <>
      {/* Offline/Demo Banner */}
      {isOffline ? <OfflineBanner /> : (user?.isDemo && <DemoBanner />)}

      {isOffline && !dismissOffline && (
        <OfflineOverlay
          onRetry={() => {
            if (navigator.onLine) {
              setIsOffline(false);
              setDismissOffline(false);
            } else {
              alert('Koneksi internet masih belum tersedia.');
            }
          }}
          onContinueOffline={() => setDismissOffline(true)}
        />
      )}

      <div style={{ paddingTop: (isOffline || user?.isDemo) ? '36px' : 0, height: '100vh', boxSizing: 'border-box' }}>
        <AppContent user={user} onLogout={handleLogout} appMode={appMode} setAppMode={setAppMode} theme={theme} toggleTheme={toggleTheme} />
      </div>

      {/* Onboarding Mode Selection Modal */}
      {showModePrompt && (
        <BusinessModeModal onSelectMode={handleSelectMode} />
      )}

      {/* Demo Block Modal */}
      {demoBlockMsg && (
        <DemoBlockModal
          message={typeof demoBlockMsg === 'string' ? demoBlockMsg : undefined}
          onClose={() => setDemoBlockMsg(null)}
        />
      )}

      {/* Exit Modal */}
      {showExitModal && (
        <ExitModal onClose={() => setShowExitModal(false)} />
      )}
    </>
  );
}

export default App;
