import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Link, useLocation, Navigate } from 'react-router-dom';
import {
  Calculator, Package, LayoutDashboard, LogOut, Clock,
  DollarSign, Users, UserCog, Crown, ShieldCheck, Lock, X, Sparkles, History, Car
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
import { AuthContext, DemoContext, hasRole, DEMO_LIMITS, useAuth } from './AuthContext';
import { syncOfflineWrites } from './api';
import './index.css';

// ─── Role-based page access ────────────────────────────────────
const ROLE_ACCESS = {
  KASIR: ['/', '/katalog'],
  CASHIER: ['/', '/katalog'],
  ADMIN: ['/', '/katalog', '/dashboard', '/keuangan', '/pelanggan', '/karyawan', '/pesanan', '/supplier'],
  OWNER: ['/', '/katalog', '/dashboard', '/keuangan', '/pelanggan', '/karyawan', '/pesanan', '/supplier', '/activity-logs'],
};

const canAccess = (role, path) => {
  const allowed = ROLE_ACCESS[role] || ROLE_ACCESS['KASIR'];
  return allowed.includes(path);
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

// ─── Navigation ────────────────────────────────────────────────
const Navigation = ({ user, onLogout, appMode, setAppMode }) => {
  const location = useLocation();
  const roleStyle = ROLE_STYLE[user?.role] || ROLE_STYLE['KASIR'];

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
  const navItems = allNavItems.filter(item =>
    hasRole(effectiveRole, item.minRole) && item.showInNav
  );

  const showModeSwitcher = hasRole(user?.role, 'ADMIN') && !isExcludedName(user?.name) && !user?.isDemo;

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

  return (
    <>
      {/* Mobile Top Header */}
      <div className="mobile-header glass-panel md-hidden" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div className="brand" style={{ fontSize: '1.1rem' }}>POSBah</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {user?.name?.toLowerCase() !== 'muizz' && (
            <>
              <span style={{ fontSize: '0.75rem', color: '#1F2937', fontWeight: 600 }}>{user?.name}</span>
              <RoleBadge />
            </>
          )}
          <button onClick={onLogout} title="Keluar" style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#6B7280', padding: '4px' }}>
            <LogOut size={16} />
          </button>
        </div>
      </div>

      {/* Sidebar (Desktop) */}
      <aside className="sidebar glass-panel">
        <div className="brand hidden md-block">POSBah</div>
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
              </select>
            </div>
          )}
          {user?.name?.toLowerCase() !== 'muizz' && (
            <>
              <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#1F2937', marginBottom: '4px' }}>{user?.name}</div>
              <RoleBadge />
            </>
          )}
          <button onClick={onLogout} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', color: '#EF4444', background: 'none', border: 'none', cursor: 'pointer', padding: '8px 0 0', fontWeight: 600 }}>
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
function AppContent({ user, onLogout, appMode, setAppMode }) {
  const location = useLocation();
  const isPublicStore = location.pathname === '/toko-online';

  if (isPublicStore) return <TokoOnline />;

  // Demo: akses semua halaman (OWNER level)
  const effectiveRole = user?.isDemo ? 'OWNER' : user?.role;
  const currentPath = location.pathname;
  if (!canAccess(effectiveRole, currentPath) && currentPath !== '/toko-online') {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="app-layout">
      <Navigation user={user} onLogout={onLogout} appMode={appMode} setAppMode={setAppMode} />
      <main className="main-content">
        <Routes>
          <Route path="/" element={appMode === 'RENTAL' ? <RentalMobil /> : <Kasir />} />
          <Route path="/katalog" element={<Katalog />} />
          {canAccess(effectiveRole, '/dashboard') && <Route path="/dashboard" element={<Dashboard appMode={appMode} />} />}
          {canAccess(effectiveRole, '/keuangan') && <Route path="/keuangan" element={<Keuangan appMode={appMode} />} />}
          {canAccess(effectiveRole, '/pelanggan') && <Route path="/pelanggan" element={<Pelanggan />} />}
          {canAccess(effectiveRole, '/karyawan') && <Route path="/karyawan" element={<Karyawan />} />}
          {canAccess(effectiveRole, '/pesanan') && <Route path="/pesanan" element={<Pesanan />} />}
          {canAccess(effectiveRole, '/supplier') && <Route path="/supplier" element={<Supplier />} />}
          {canAccess(effectiveRole, '/activity-logs') && <Route path="/activity-logs" element={<LogAktivitas />} />}
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
        maxWidth: '520px', width: '100%', textAlign: 'center',
        boxShadow: '0 25px 50px rgba(0,0,0,0.3)',
        animation: 'slideUp 0.3s ease'
      }}>
        <h2 style={{ margin: '0 0 8px', fontSize: '1.4rem', fontWeight: 900, color: '#111827' }}>
          Selamat Datang di POSBah! 👋
        </h2>
        <p style={{ margin: '0 0 1.5rem', color: '#6B7280', fontSize: '0.9rem' }}>
          Silakan pilih jenis sistem POS yang ingin kamu gunakan untuk memulai operasional:
        </p>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '1.5rem' }}>
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
              Kelola kendaraan, status penyewaan, kalkulator tarif, pengembalian, biaya benkel dan margin.
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

// ─── Root App ──────────────────────────────────────────────────
function App() {
  const [isOffline, setIsOffline] = useState(() => !navigator.onLine);

  useEffect(() => {
    const handleOnline = () => {
      setIsOffline(false);
      syncOfflineWrites();
    };
    const handleOffline = () => {
      setIsOffline(true);
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
      if (parsed.isDemo && parsed.expiresAt && Date.now() > parsed.expiresAt) {
        localStorage.removeItem('posbah_user');
        return null;
      }
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

  if (!user) return <Login onLogin={handleLogin} />;

  return (
    <AuthContext.Provider value={{ user }}>
      <DemoContext.Provider value={{ showDemoBlock, isDemo: user?.isDemo === true }}>
        <BrowserRouter>
          {/* Offline/Demo Banner */}
          {isOffline ? <OfflineBanner /> : (user?.isDemo && <DemoBanner />)}

          <div style={{ paddingTop: (isOffline || user?.isDemo) ? '36px' : 0, height: '100dvh', boxSizing: 'border-box' }}>
            <AppContent user={user} onLogout={handleLogout} appMode={appMode} setAppMode={setAppMode} />
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
        </BrowserRouter>
      </DemoContext.Provider>
    </AuthContext.Provider>
  );
}

export default App;
