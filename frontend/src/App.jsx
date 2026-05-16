import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Link, useLocation } from 'react-router-dom';
import { Calculator, Package, Wallet, Users, Contact, Globe, Menu, X } from 'lucide-react';
import Kasir from './pages/Kasir';
import Katalog from './pages/Katalog';
import Keuangan from './pages/Keuangan';
import Pelanggan from './pages/Pelanggan';
import Karyawan from './pages/Karyawan';
import TokoOnline from './pages/TokoOnline';
import './index.css';

const Navigation = () => {
  const location = useLocation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // Close mobile menu on route change
  useEffect(() => {
    setIsMobileMenuOpen(false);
  }, [location.pathname]);

  const navItems = [
    { path: '/', label: 'Kasir', icon: <Calculator size={20} /> },
    { path: '/katalog', label: 'Katalog', icon: <Package size={20} /> },
    { path: '/keuangan', label: 'Keuangan', icon: <Wallet size={20} /> },
    { path: '/pelanggan', label: 'Pelanggan', icon: <Contact size={20} /> },
    { path: '/karyawan', label: 'Karyawan', icon: <Users size={20} /> },
    { path: '/toko-online', label: 'Online', icon: <Globe size={20} /> },
  ];

  return (
    <>
      {/* Mobile Top Header */}
      <div className="mobile-header glass-panel md-hidden">
        <div className="brand text-center w-full">POSBah</div>
      </div>

      {/* Sidebar (Desktop) & Mobile Overlay Menu */}
      <aside className={`sidebar glass-panel ${isMobileMenuOpen ? 'mobile-open' : ''}`}>
        <div className="brand hidden md-block">POSBah</div>
        <nav>
          {navItems.map(item => (
            <Link 
              key={item.path} 
              to={item.path}
              className={location.pathname === item.path ? 'active' : ''}
            >
              {item.icon} <span>{item.label}</span>
            </Link>
          ))}
        </nav>
      </aside>

      {/* Mobile Bottom Nav (Optional UI enhancement for phones) */}
      <nav className="mobile-bottom-nav glass-panel md-hidden">
        {navItems.map(item => (
          <Link 
            key={item.path} 
            to={item.path}
            className={location.pathname === item.path ? 'active' : ''}
          >
            {item.icon}
            <span className="text-xs">{item.label}</span>
          </Link>
        ))}
      </nav>
    </>
  );
};

function AppContent() {
  const location = useLocation();
  const isPublicStore = location.pathname === '/toko-online';

  if (isPublicStore) {
    return <TokoOnline />;
  }

  return (
    <div className="app-layout">
      <Navigation />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<Kasir />} />
          <Route path="/katalog" element={<Katalog />} />
          <Route path="/keuangan" element={<Keuangan />} />
          <Route path="/pelanggan" element={<Pelanggan />} />
          <Route path="/karyawan" element={<Karyawan />} />
        </Routes>
      </main>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}

export default App;
