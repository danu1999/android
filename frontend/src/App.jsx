import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Link, useLocation, Navigate } from 'react-router-dom';
import {
  Calculator, Package, LayoutDashboard, LogOut, Clock,
  DollarSign, Users, UserCog, Crown, ShieldCheck
} from 'lucide-react';
import Kasir from './pages/Kasir';
import Katalog from './pages/Katalog';
import Keuangan from './pages/Keuangan';
import Pelanggan from './pages/Pelanggan';
import Karyawan from './pages/Karyawan';
import TokoOnline from './pages/TokoOnline';
import Dashboard from './pages/Dashboard';
import Login from './pages/Login';
import { AuthContext, hasRole } from './AuthContext';
import './index.css';

// ─── Role-based page access ────────────────────────────────────
const ROLE_ACCESS = {
  KASIR:  ['/', '/katalog'],
  ADMIN:  ['/', '/katalog', '/dashboard', '/keuangan', '/pelanggan', '/karyawan'],
  OWNER:  ['/', '/katalog', '/dashboard', '/keuangan', '/pelanggan', '/karyawan'],
};

const canAccess = (role, path) => {
  const allowed = ROLE_ACCESS[role] || ROLE_ACCESS['KASIR'];
  return allowed.includes(path);
};

// Badge warna per role
const ROLE_STYLE = {
  KASIR:  { bg: '#EEF2FF', color: '#4F46E5', icon: <Calculator size={10} /> },
  ADMIN:  { bg: '#F0FDF4', color: '#16A34A', icon: <ShieldCheck size={10} /> },
  OWNER:  { bg: '#FEF3C7', color: '#D97706', icon: <Crown size={10} /> },
};

// ─── Navigation ────────────────────────────────────────────────
const Navigation = ({ user, onLogout }) => {
  const location = useLocation();
  const roleStyle = ROLE_STYLE[user?.role] || ROLE_STYLE['KASIR'];

  const allNavItems = [
    { path: '/',           label: 'Kasir',     icon: <Calculator size={20} />,     minRole: 'KASIR', showInNav: true  },
    { path: '/katalog',    label: 'Katalog',   icon: <Package size={20} />,        minRole: 'KASIR', showInNav: true  },
    { path: '/dashboard',  label: 'Dashboard', icon: <LayoutDashboard size={20}/>, minRole: 'ADMIN', showInNav: true  },
    // Keuangan, Pelanggan, Karyawan — bisa diakses via URL/Dashboard, tidak tampil di nav
    { path: '/keuangan',   label: 'Keuangan',  icon: <DollarSign size={20} />,     minRole: 'ADMIN', showInNav: false },
    { path: '/pelanggan',  label: 'Pelanggan', icon: <Users size={20} />,          minRole: 'ADMIN', showInNav: false },
    { path: '/karyawan',   label: 'Karyawan',  icon: <UserCog size={20} />,        minRole: 'ADMIN', showInNav: false },
  ];

  const navItems = allNavItems.filter(item => hasRole(user?.role, item.minRole) && item.showInNav);

  const RoleBadge = () => (
    <span style={{
      fontSize: '0.65rem',
      background: roleStyle.bg,
      color: roleStyle.color,
      padding: '2px 7px',
      borderRadius: '99px',
      fontWeight: 700,
      display: 'inline-flex',
      alignItems: 'center',
      gap: 3,
    }}>
      {roleStyle.icon} {user?.role}
    </span>
  );

  return (
    <>
      {/* Mobile Top Header */}
      <div className="mobile-header glass-panel md-hidden" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div className="brand" style={{ fontSize: '1.1rem' }}>POSBah</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '0.75rem', color: '#1F2937', fontWeight: 600 }}>{user?.name}</span>
          <RoleBadge />
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
        {/* User info + logout on desktop */}
        <div style={{ marginTop: 'auto', paddingTop: '1rem', borderTop: '1px solid rgba(0,0,0,0.08)' }}>
          <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#1F2937', marginBottom: '4px' }}>{user?.name}</div>
          <RoleBadge />
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
function AppContent({ user, onLogout }) {
  const location = useLocation();
  const isPublicStore = location.pathname === '/toko-online';

  if (isPublicStore) return <TokoOnline />;

  const currentPath = location.pathname;
  if (!canAccess(user?.role, currentPath) && currentPath !== '/toko-online') {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="app-layout">
      <Navigation user={user} onLogout={onLogout} />
      <main className="main-content">
        <Routes>
          <Route path="/"          element={<Kasir />} />
          <Route path="/katalog"   element={<Katalog />} />
          {canAccess(user?.role, '/dashboard')  && <Route path="/dashboard"  element={<Dashboard />} />}
          {canAccess(user?.role, '/keuangan')   && <Route path="/keuangan"   element={<Keuangan />} />}
          {canAccess(user?.role, '/pelanggan')  && <Route path="/pelanggan"  element={<Pelanggan />} />}
          {canAccess(user?.role, '/karyawan')   && <Route path="/karyawan"   element={<Karyawan />} />}
        </Routes>
      </main>
    </div>
  );
}

// ─── Root App ──────────────────────────────────────────────────
function App() {
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

  const demoDaysLeft = user?.isDemo && user?.expiresAt
    ? Math.ceil((user.expiresAt - Date.now()) / (1000 * 60 * 60 * 24))
    : null;

  const handleLogin = (userData) => {
    localStorage.setItem('posbah_user', JSON.stringify(userData));
    setUser(userData);
  };

  const handleLogout = () => {
    localStorage.removeItem('posbah_user');
    setUser(null);
  };

  if (!user) return <Login onLogin={handleLogin} />;

  return (
    <AuthContext.Provider value={{ user }}>
      <BrowserRouter>
        {demoDaysLeft !== null && (
          <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, zIndex: 9999,
            background: 'linear-gradient(90deg, #F59E0B, #EF4444)',
            color: 'white', textAlign: 'center',
            padding: '6px 16px', fontSize: '0.8rem', fontWeight: 700,
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
          }}>
            <Clock size={14} />
            Mode Demo — Sisa {demoDaysLeft} hari · Hubungi kami untuk berlangganan
          </div>
        )}
        <div style={{ paddingTop: demoDaysLeft !== null ? '32px' : 0, height: '100dvh', boxSizing: 'border-box' }}>
          <AppContent user={user} onLogout={handleLogout} />
        </div>
      </BrowserRouter>
    </AuthContext.Provider>
  );
}

export default App;
