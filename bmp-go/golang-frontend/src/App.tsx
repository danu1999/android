import React, { useContext, useState, useRef, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, NavLink, useNavigate } from 'react-router-dom';
import { AuthProvider, AuthContext } from './contexts/AuthContext';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Clients from './pages/Clients';
import Products from './pages/Products';
import Settings from './pages/Settings';
import CashFlow from './pages/CashFlow';
import Invoices from './pages/Invoices';
import CreateInvoice from './pages/CreateInvoice';
import BahanNono from './pages/BahanNono';
import HppCalculator from './pages/HppCalculator';
import Pricelist from './pages/Pricelist';
import BonusClaim from './pages/BonusClaim';
import { User, Key, Settings as SettingsIcon, LogOut, ChevronDown, Home, FileText, ShoppingCart, Users, Package, RefreshCw, List, Cpu, Menu, X, DollarSign } from 'lucide-react';
import Employees from './pages/Employees';
import Payroll from './pages/Payroll';
import AccessDenied from './pages/AccessDenied';

const isDemoToken = (token: string | null): boolean => {
  if (!token) return false;
  try {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload));
    return !!decoded.is_demo;
  } catch (e) {
    return false;
  }
};

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { token } = useContext(AuthContext);
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

const Layout = ({ children }: { children: React.ReactNode }) => {
  const { logout, token } = useContext(AuthContext);
  const isDemo = isDemoToken(token);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const sidebarRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const handleResize = () => {
      const mobile = window.innerWidth <= 768;
      setIsMobile(mobile);
      if (!mobile) setSidebarOpen(false);
    };
    window.addEventListener('resize', handleResize);
    
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
      if (isMobile && sidebarOpen && sidebarRef.current && !sidebarRef.current.contains(event.target as Node)) {
        setSidebarOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      window.removeEventListener('resize', handleResize);
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isMobile, sidebarOpen]);

  const navStyle = ({ isActive }: { isActive: boolean }) => ({
    color: isActive ? '#fff' : 'rgba(255, 255, 255, 0.7)',
    textDecoration: 'none',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px 18px',
    borderRadius: '12px',
    background: isActive ? 'linear-gradient(135deg, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0.05) 100%)' : 'transparent',
    boxShadow: isActive ? '0 4px 15px rgba(0,0,0,0.1)' : 'none',
    border: isActive ? '1px solid rgba(255,255,255,0.1)' : '1px solid transparent',
    marginBottom: '6px',
    fontWeight: isActive ? '600' : '400',
    fontSize: '14px',
    transition: 'all 0.3s ease',
    backdropFilter: isActive ? 'blur(10px)' : 'none'
  });

  const closeSidebar = () => {
    if (isMobile) setSidebarOpen(false);
  };

  return (
    <div className="app-container" style={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100vw', overflow: 'hidden', background: '#f0f4f8' }}>
      {isDemo && (
        <div style={{
          background: 'linear-gradient(90deg, #f59e0b 0%, #d97706 100%)',
          color: '#ffffff',
          textAlign: 'center',
          padding: '8px 20px',
          fontSize: '14px',
          fontWeight: '600',
          boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
          zIndex: 1200,
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          gap: '8px',
          borderBottom: '1px solid rgba(255,255,255,0.1)'
        }}>
          <span style={{ fontSize: '16px' }}>⚠️</span>
          <span>Anda sedang menggunakan Akun Demo. Semua aktivitas di sini terisolasi dan aman.</span>
        </div>
      )}
      {/* Top Header - Glassmorphism */}
      <header className="glass-header" style={{ 
        display: 'flex', justifyContent: 'space-between', alignItems: 'center', 
        padding: '0 20px', height: '70px', 
        zIndex: 1100 
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          {isMobile && (
            <button 
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="icon-btn"
            >
              {sidebarOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          )}
          <div className="brand-logo" style={{ fontSize: isMobile ? '18px' : '22px', fontWeight: '800', whiteSpace: 'nowrap', background: 'linear-gradient(135deg, #0d6efd 0%, #0dcaf0 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            BAHTERA MULYA
          </div>
        </div>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          <div style={{ position: 'relative' }} ref={dropdownRef}>
            <button 
              onClick={() => setDropdownOpen(!dropdownOpen)} 
              className="user-dropdown-btn"
            >
              <div className="avatar-circle">
                <User size={18} />
              </div>
              {!isMobile && <span style={{ fontWeight: '600', fontSize: '14px', color: '#1e293b' }}>Administrator</span>}
              <ChevronDown size={14} color="#64748b" />
            </button>

            {dropdownOpen && (
              <div className="glass-dropdown">
                <div style={{ padding: '15px', borderBottom: '1px solid rgba(0,0,0,0.05)', fontSize: '12px', color: '#64748b', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Key size={14} /> Akses: Super Admin
                </div>
                <button 
                  onClick={() => { setDropdownOpen(false); navigate('/settings'); }}
                  className="dropdown-item"
                >
                  <SettingsIcon size={16} /> Profil Perusahaan
                </button>
                <button 
                  onClick={() => { setDropdownOpen(false); logout(); }}
                  className="dropdown-item text-danger"
                >
                  <LogOut size={16} /> Keluar Sistem
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden', position: 'relative' }}>
        {/* Sidebar */}
        <div 
          ref={sidebarRef}
          className={`modern-sidebar ${isMobile ? (sidebarOpen ? 'open' : 'closed') : ''}`}
        >
          <div className="sidebar-scroll">
            <nav style={{ flex: 1, padding: '20px' }}>
              <div className="nav-section-title">Menu Utama</div>
              <NavLink to="/" style={navStyle} onClick={closeSidebar}><Home size={18}/> Panel Utama</NavLink>
              <NavLink to="/invoices" style={navStyle} onClick={closeSidebar}><FileText size={18}/> Daftar Faktur</NavLink>
              <NavLink to="/products" style={navStyle} onClick={closeSidebar}><ShoppingCart size={18}/> Master Barang</NavLink>
              <NavLink to="/clients" style={navStyle} onClick={closeSidebar}><Users size={18}/> Data Pelanggan</NavLink>
              <NavLink to="/bahan-nono" style={navStyle} onClick={closeSidebar}><Package size={18}/> Bahan Baku</NavLink>
              <NavLink to="/kas" style={navStyle} onClick={closeSidebar}><RefreshCw size={18}/> Kas Keuangan</NavLink>
              <NavLink to="/pricelist" style={navStyle} onClick={closeSidebar}><List size={18}/> Pricelist Harga</NavLink>
              <NavLink to="/hpp-calculator" style={navStyle} onClick={closeSidebar}><Cpu size={18}/> Kalkulator HPP</NavLink>
              
              <div className="nav-section-title mt-4">SDM & Payroll</div>
              <NavLink to="/employees" style={navStyle} onClick={closeSidebar}><Users size={18}/> Data Karyawan</NavLink>
              <NavLink to="/payroll" style={navStyle} onClick={closeSidebar}><DollarSign size={18}/> Sistem Penggajian</NavLink>

              <div className="nav-section-title mt-4">Sistem</div>
              <NavLink to="/settings" style={navStyle} onClick={closeSidebar}><SettingsIcon size={18}/> Pengaturan</NavLink>
            </nav>
          </div>
          
          <div className="sidebar-footer">
            <div style={{ color: 'rgba(255, 255, 255, 0.4)', fontSize: '11px', textAlign: 'center', marginBottom: '8px', letterSpacing: '0.5px' }}>
              Versi 1.2.0
            </div>
            <button onClick={logout} className="logout-btn">
              <LogOut size={18} /> Keluar
            </button>
          </div>
        </div>

        {/* Overlay for mobile sidebar */}
        {isMobile && sidebarOpen && (
          <div className="sidebar-overlay" onClick={() => setSidebarOpen(false)} />
        )}

        {/* Main Content Area */}
        <div className="main-content-area" style={{ flex: 1, overflowY: 'auto', width: '100%', order: isMobile ? 1 : 2, position: 'relative' }}>
          <div className="content-wrapper">
             {children}
          </div>
        </div>
      </div>
    </div>
  );
};

const AppRoutes = () => {
  const { token } = useContext(AuthContext);
  
  return (
    <Router>
      <Routes>
        {/* Halaman /bonus selalu dapat diakses secara publik dan mandiri (tanpa Layout admin) */}
        <Route path="/bonus" element={<BonusClaim />} />
        
        {token ? (
          <Route path="*" element={
            <Layout>
              <Routes>
                <Route path="/" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
                <Route path="/clients" element={<ProtectedRoute><Clients /></ProtectedRoute>} />
                <Route path="/products" element={<ProtectedRoute><Products /></ProtectedRoute>} />
                <Route path="/settings" element={<ProtectedRoute><Settings /></ProtectedRoute>} />
                <Route path="/kas" element={<ProtectedRoute><CashFlow /></ProtectedRoute>} />
                <Route path="/invoices" element={<ProtectedRoute><Invoices /></ProtectedRoute>} />
                <Route path="/invoices/create" element={<ProtectedRoute><CreateInvoice /></ProtectedRoute>} />
                <Route path="/bahan-nono" element={<ProtectedRoute><BahanNono /></ProtectedRoute>} />
                <Route path="/hpp-calculator" element={<ProtectedRoute><HppCalculator /></ProtectedRoute>} />
                <Route path="/pricelist" element={<ProtectedRoute><Pricelist /></ProtectedRoute>} />
                <Route path="/employees" element={<ProtectedRoute><Employees /></ProtectedRoute>} />
                <Route path="/payroll" element={<ProtectedRoute><Payroll /></ProtectedRoute>} />
                <Route path="/access-denied" element={<ProtectedRoute><AccessDenied /></ProtectedRoute>} />
                <Route path="/login" element={<Navigate to="/" replace />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Layout>
          } />
        ) : (
          <Route path="*" element={
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
          } />
        )}
      </Routes>
    </Router>
  );
};

const App: React.FC = () => {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
};

export default App;