import React, { useState, useEffect } from 'react';
import api from '../api';

export default function Login({ onLogin }) {
  const [loginMethod, setLoginMethod] = useState(null); // 'PIN' | 'EMAIL' | 'GOOGLE'
  const [name, setName] = useState('');
  const [pin, setPin] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handlePinPress = (digit) => {
    if (pin.length < 6) setPin(prev => prev + digit);
  };

  const handlePinDelete = () => setPin(prev => prev.slice(0, -1));
  const handlePinClear = () => setPin('');

  const handleLogin = async () => {
    if (!name.trim()) { setError('Masukkan nama kamu terlebih dahulu'); return; }
    if (!pin) { setError('Masukkan PIN kamu'); return; }
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/auth/login', { name: name.trim(), pin });
      onLogin(res.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Login gagal, coba lagi');
      setPin('');
    } finally {
      setLoading(false);
    }
  };

  const handleEmailLogin = async () => {
    if (!email.trim()) { setError('Masukkan email Anda'); return; }
    if (!password) { setError('Masukkan password Anda'); return; }
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/auth/login-email', { email: email.trim(), password });
      onLogin(res.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Login email gagal, periksa akun Anda');
      setPassword('');
    } finally {
      setLoading(false);
    }
  };

  const parseJwt = (token) => {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));
      return JSON.parse(jsonPayload);
    } catch (e) {
      return null;
    }
  };

  const handleGoogleLoginResponse = async (response) => {
    const payload = parseJwt(response.credential);
    if (payload && payload.email) {
      try {
        const regRes = await api.post('/auth/google-register', { email: payload.email });
        const { registeredAt } = regRes.data;
        const regTime = new Date(registeredAt).getTime();
        const expiresAt = regTime + 3 * 24 * 60 * 60 * 1000; // 3 days since registration

        onLogin({
          id: payload.sub,
          name: payload.name || payload.email.split('@')[0],
          email: payload.email,
          picture: payload.picture,
          role: 'OWNER',
          isDemo: true,
          registeredAt,
          expiresAt
        });
      } catch (err) {
        console.warn('Google register backend error, falling back to local registration:', err);
        
        let localUsers = {};
        try {
          localUsers = JSON.parse(localStorage.getItem('posbah_google_users') || '{}');
        } catch (_) {}
        
        let registeredAt = localUsers[payload.email];
        if (!registeredAt) {
          registeredAt = new Date().toISOString();
          localUsers[payload.email] = registeredAt;
          localStorage.setItem('posbah_google_users', JSON.stringify(localUsers));
        }

        const regTime = new Date(registeredAt).getTime();
        const expiresAt = regTime + 3 * 24 * 60 * 60 * 1000; // 3 days since registration

        onLogin({
          id: payload.sub,
          name: payload.name || payload.email.split('@')[0],
          email: payload.email,
          picture: payload.picture,
          role: 'OWNER',
          isDemo: true,
          registeredAt,
          expiresAt
        });
      }
    } else {
      setError('Gagal membaca profil Google');
    }
  };

  useEffect(() => {
    if (loginMethod === 'GOOGLE') {
      /* global google */
      const initGoogleGSI = () => {
        if (window.google) {
          google.accounts.id.initialize({
            client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID || "YOUR_GOOGLE_CLIENT_ID_PLACEHOLDER.apps.googleusercontent.com",
            callback: handleGoogleLoginResponse
          });
          google.accounts.id.renderButton(
            document.getElementById("google-signin-btn"),
            { 
              theme: "outline", 
              size: "large", 
              width: "316", 
              text: "continue_with",
              shape: "pill"
            }
          );
        }
      };

      const timer = setTimeout(initGoogleGSI, 300);
      return () => clearTimeout(timer);
    }
  }, [loginMethod]);

  const numpad = ['1','2','3','4','5','6','7','8','9','','0','⌫'];

  return (
    <div style={{
      minHeight: '100dvh',
      background: 'linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4c1d95 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '1rem',
      fontFamily: "'Inter', sans-serif"
    }}>
      <div style={{
        background: 'rgba(255,255,255,0.08)',
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        borderRadius: '24px',
        border: '1px solid rgba(255,255,255,0.15)',
        padding: '2.5rem 2rem',
        width: '100%',
        maxWidth: '380px',
        boxShadow: '0 25px 50px rgba(0,0,0,0.4)',
      }}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <img src="/logo.jpg" alt="Logo" style={{
            width: '64px', height: '64px',
            objectFit: 'cover',
            borderRadius: '16px',
            display: 'inline-flex',
            marginBottom: '1rem',
            boxShadow: '0 8px 24px rgba(99,102,241,0.4)'
          }} />
          <h1 style={{ color: 'white', fontWeight: 800, fontSize: '1.75rem', margin: 0 }}>POSBah</h1>
          <p style={{ color: 'rgba(255,255,255,0.55)', margin: '4px 0 0', fontSize: '0.875rem' }}>Masuk ke akun Anda</p>
        </div>

        {/* Konten Form Dinamis */}
        {loginMethod === null ? (
          <div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <button
                onClick={() => { setLoginMethod('PIN'); setError(''); }}
                style={{
                  width: '100%',
                  padding: '16px',
                  borderRadius: '16px',
                  border: '1px solid rgba(255,255,255,0.12)',
                  background: 'rgba(255,255,255,0.06)',
                  color: 'white',
                  fontSize: '0.95rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  transition: 'all 0.2s',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
                }}
                onMouseEnter={e => {
                  e.currentTarget.style.background = 'rgba(255,255,255,0.12)';
                  e.currentTarget.style.transform = 'translateY(-2px)';
                }}
                onMouseLeave={e => {
                  e.currentTarget.style.background = 'rgba(255,255,255,0.06)';
                  e.currentTarget.style.transform = 'translateY(0)';
                }}
              >
                <span style={{ fontSize: '1.3rem' }}>🔑</span>
                <div style={{ textAlign: 'left' }}>
                  <div style={{ fontWeight: 800 }}>Masuk dengan PIN & Nama</div>
                  <div style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.5)', fontWeight: 500 }}>Kasir, Admin, & Owner</div>
                </div>
              </button>

              <button
                onClick={() => { setLoginMethod('EMAIL'); setError(''); }}
                style={{
                  width: '100%',
                  padding: '16px',
                  borderRadius: '16px',
                  border: '1px solid rgba(255,255,255,0.12)',
                  background: 'rgba(255,255,255,0.06)',
                  color: 'white',
                  fontSize: '0.95rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  transition: 'all 0.2s',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
                }}
                onMouseEnter={e => {
                  e.currentTarget.style.background = 'rgba(255,255,255,0.12)';
                  e.currentTarget.style.transform = 'translateY(-2px)';
                }}
                onMouseLeave={e => {
                  e.currentTarget.style.background = 'rgba(255,255,255,0.06)';
                  e.currentTarget.style.transform = 'translateY(0)';
                }}
              >
                <span style={{ fontSize: '1.3rem' }}>✉️</span>
                <div style={{ textAlign: 'left' }}>
                  <div style={{ fontWeight: 800 }}>Masuk dengan Email & Password</div>
                  <div style={{ fontSize: '0.75rem', color: '#a7f3d0', fontWeight: 600 }}>⭐ Akun Premium / Aktif</div>
                </div>
              </button>

              <button
                onClick={() => { setLoginMethod('GOOGLE'); setError(''); }}
                style={{
                  width: '100%',
                  padding: '16px',
                  borderRadius: '16px',
                  border: '1px solid rgba(255,255,255,0.12)',
                  background: 'rgba(255,255,255,0.06)',
                  color: 'white',
                  fontSize: '0.95rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  transition: 'all 0.2s',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
                }}
                onMouseEnter={e => {
                  e.currentTarget.style.background = 'rgba(255,255,255,0.12)';
                  e.currentTarget.style.transform = 'translateY(-2px)';
                }}
                onMouseLeave={e => {
                  e.currentTarget.style.background = 'rgba(255,255,255,0.06)';
                  e.currentTarget.style.transform = 'translateY(0)';
                }}
              >
                <span style={{ fontSize: '1.3rem' }}>⚡</span>
                <div style={{ textAlign: 'left' }}>
                  <div style={{ fontWeight: 800 }}>Masuk dengan Google (Demo)</div>
                  <div style={{ fontSize: '0.75rem', color: '#c7d2fe', fontWeight: 600 }}>Coba Gratis Mode Simulasi</div>
                </div>
              </button>
            </div>
            <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.3)', fontSize: '0.75rem', marginTop: '2rem', marginBottom: 0 }}>
              Hubungi admin jika Anda mengalami kendala masuk
            </p>
          </div>
        ) : loginMethod === 'PIN' ? (
          <div>
            {/* Name Input */}
            <div style={{ marginBottom: '1.25rem' }}>
              <label style={{ display: 'block', color: 'rgba(255,255,255,0.7)', fontSize: '0.8rem', fontWeight: 600, marginBottom: '8px', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
                Nama
              </label>
              <input
                type="text"
                placeholder="Masukkan nama Anda"
                value={name}
                onChange={e => { setName(e.target.value); setError(''); }}
                style={{
                  width: '100%',
                  padding: '14px 16px',
                  borderRadius: '12px',
                  border: '1px solid rgba(255,255,255,0.2)',
                  background: 'rgba(255,255,255,0.1)',
                  color: 'white',
                  fontSize: '1rem',
                  outline: 'none',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            {/* PIN Display */}
            <div style={{ marginBottom: '1.25rem' }}>
              <label style={{ display: 'block', color: 'rgba(255,255,255,0.7)', fontSize: '0.8rem', fontWeight: 600, marginBottom: '8px', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
                PIN
              </label>
              <div style={{ display: 'flex', justifyContent: 'center', gap: '10px' }}>
                {[0,1,2,3,4,5].map(i => (
                  <div key={i} style={{
                    width: '44px', height: '44px',
                    borderRadius: '50%',
                    border: '2px solid rgba(255,255,255,0.3)',
                    background: pin.length > i ? '#818cf8' : 'rgba(255,255,255,0.08)',
                    boxShadow: pin.length > i ? '0 0 12px rgba(129,140,248,0.6)' : 'none',
                    transition: 'all 0.2s',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    {pin.length > i && <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: 'white', display: 'block' }} />}
                  </div>
                ))}
              </div>
            </div>

            {/* Numpad */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px', marginBottom: '1.25rem' }}>
              {numpad.map((d, i) => (
                <button
                  key={i}
                  onClick={() => {
                    if (d === '⌫') handlePinDelete();
                    else if (d !== '') handlePinPress(d);
                  }}
                  disabled={d === ''}
                  style={{
                    padding: '16px',
                    borderRadius: '12px',
                    border: 'none',
                    background: d === '⌫' ? 'rgba(239,68,68,0.2)' : d === '' ? 'transparent' : 'rgba(255,255,255,0.1)',
                    color: d === '⌫' ? '#fca5a5' : 'white',
                    fontSize: d === '⌫' ? '1.1rem' : '1.25rem',
                    fontWeight: 700,
                    cursor: d === '' ? 'default' : 'pointer',
                    transition: 'all 0.15s',
                    visibility: d === '' ? 'hidden' : 'visible',
                  }}
                >
                  {d}
                </button>
              ))}
            </div>

            {/* Error */}
            {error && (
              <div style={{
                background: 'rgba(239,68,68,0.15)',
                border: '1px solid rgba(239,68,68,0.3)',
                borderRadius: '10px',
                padding: '10px 14px',
                color: '#fca5a5',
                fontSize: '0.85rem',
                marginBottom: '1rem',
                textAlign: 'center',
              }}>
                {error}
              </div>
            )}

            {/* Login Button */}
            <button
              onClick={handleLogin}
              disabled={loading || !pin || !name}
              style={{
                width: '100%',
                padding: '16px',
                borderRadius: '14px',
                border: 'none',
                background: loading || !pin || !name
                  ? 'rgba(255,255,255,0.1)'
                  : 'linear-gradient(135deg, #818cf8, #6366f1)',
                color: loading || !pin || !name ? 'rgba(255,255,255,0.4)' : 'white',
                fontSize: '1rem',
                fontWeight: 700,
                cursor: loading || !pin || !name ? 'not-allowed' : 'pointer',
                boxShadow: loading || !pin || !name ? 'none' : '0 8px 20px rgba(99,102,241,0.35)',
                transition: 'all 0.2s',
                letterSpacing: '0.02em',
              }}
            >
              {loading ? 'Memproses...' : 'Masuk'}
            </button>

            <button
              onClick={() => { setLoginMethod(null); setError(''); }}
              style={{
                width: '100%',
                padding: '12px',
                borderRadius: '14px',
                border: '1.5px solid rgba(255,255,255,0.15)',
                background: 'transparent',
                color: 'rgba(255,255,255,0.7)',
                fontSize: '0.875rem',
                fontWeight: 700,
                cursor: 'pointer',
                marginTop: '12px',
                transition: 'all 0.2s',
              }}
              onMouseEnter={e => {
                e.currentTarget.style.background = 'rgba(255,255,255,0.05)';
                e.currentTarget.style.color = 'white';
              }}
              onMouseLeave={e => {
                e.currentTarget.style.background = 'transparent';
                e.currentTarget.style.color = 'rgba(255,255,255,0.7)';
              }}
            >
              ← Kembali ke Pilihan
            </button>
          </div>
        ) : loginMethod === 'EMAIL' ? (
          <div>
            {/* Email Input */}
            <div style={{ marginBottom: '1.25rem' }}>
              <label style={{ display: 'block', color: 'rgba(255,255,255,0.7)', fontSize: '0.8rem', fontWeight: 600, marginBottom: '8px', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
                Email
              </label>
              <input
                type="email"
                placeholder="Masukkan email Anda"
                value={email}
                onChange={e => { setEmail(e.target.value); setError(''); }}
                style={{
                  width: '100%',
                  padding: '14px 16px',
                  borderRadius: '12px',
                  border: '1px solid rgba(255,255,255,0.2)',
                  background: 'rgba(255,255,255,0.1)',
                  color: 'white',
                  fontSize: '1rem',
                  outline: 'none',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            {/* Password Input */}
            <div style={{ marginBottom: '1.25rem' }}>
              <label style={{ display: 'block', color: 'rgba(255,255,255,0.7)', fontSize: '0.8rem', fontWeight: 600, marginBottom: '8px', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
                Password
              </label>
              <input
                type="password"
                placeholder="Masukkan password Anda"
                value={password}
                onChange={e => { setPassword(e.target.value); setError(''); }}
                style={{
                  width: '100%',
                  padding: '14px 16px',
                  borderRadius: '12px',
                  border: '1px solid rgba(255,255,255,0.2)',
                  background: 'rgba(255,255,255,0.1)',
                  color: 'white',
                  fontSize: '1rem',
                  outline: 'none',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            {/* Error */}
            {error && (
              <div style={{
                background: 'rgba(239,68,68,0.15)',
                border: '1px solid rgba(239,68,68,0.3)',
                borderRadius: '10px',
                padding: '10px 14px',
                color: '#fca5a5',
                fontSize: '0.85rem',
                marginBottom: '1rem',
                textAlign: 'center',
              }}>
                {error}
              </div>
            )}

            {/* Login Button */}
            <button
              onClick={handleEmailLogin}
              disabled={loading || !email || !password}
              style={{
                width: '100%',
                padding: '16px',
                borderRadius: '14px',
                border: 'none',
                background: loading || !email || !password
                  ? 'rgba(255,255,255,0.1)'
                  : 'linear-gradient(135deg, #10B981, #059669)',
                color: loading || !email || !password ? 'rgba(255,255,255,0.4)' : 'white',
                fontSize: '1rem',
                fontWeight: 700,
                cursor: loading || !email || !password ? 'not-allowed' : 'pointer',
                boxShadow: loading || !email || !password ? 'none' : '0 8px 20px rgba(16,185,129,0.35)',
                transition: 'all 0.2s',
                letterSpacing: '0.02em',
              }}
            >
              {loading ? 'Memproses...' : 'Masuk Akun Premium'}
            </button>

            <button
              onClick={() => { setLoginMethod(null); setError(''); }}
              style={{
                width: '100%',
                padding: '12px',
                borderRadius: '14px',
                border: '1.5px solid rgba(255, 255, 255, 0.15)',
                background: 'transparent',
                color: 'rgba(255,255,255,0.7)',
                fontSize: '0.875rem',
                fontWeight: 700,
                cursor: 'pointer',
                marginTop: '12px',
                transition: 'all 0.2s',
              }}
              onMouseEnter={e => {
                e.currentTarget.style.background = 'rgba(255,255,255,0.05)';
                e.currentTarget.style.color = 'white';
              }}
              onMouseLeave={e => {
                e.currentTarget.style.background = 'transparent';
                e.currentTarget.style.color = 'rgba(255,255,255,0.7)';
              }}
            >
              ← Kembali ke Pilihan
            </button>
          </div>
        ) : (
          <div>
            {/* Google Sign-In for Demo */}
            <div style={{ textAlign: 'center', marginTop: '14px', marginBottom: '20px' }}>
              <div style={{ color: 'rgba(255,255,255,0.75)', fontSize: '0.85rem', lineHeight: 1.5, marginBottom: '20px' }}>
                Gunakan akun Google Anda untuk mencoba demo gratis selama 3 hari. Semua fitur simulasi langsung aktif!
              </div>
              <div id="google-signin-btn" style={{ display: 'flex', justifyContent: 'center' }} />
            </div>

            {/* Error */}
            {error && (
              <div style={{
                background: 'rgba(239,68,68,0.15)',
                border: '1px solid rgba(239,68,68,0.3)',
                borderRadius: '10px',
                padding: '10px 14px',
                color: '#fca5a5',
                fontSize: '0.85rem',
                marginBottom: '1rem',
                textAlign: 'center',
              }}>
                {error}
              </div>
            )}

            <button
              onClick={() => { setLoginMethod(null); setError(''); }}
              style={{
                width: '100%',
                padding: '12px',
                borderRadius: '14px',
                border: '1.5px solid rgba(255, 255, 255, 0.15)',
                background: 'transparent',
                color: 'rgba(255,255,255,0.7)',
                fontSize: '0.875rem',
                fontWeight: 700,
                cursor: 'pointer',
                marginTop: '12px',
                transition: 'all 0.2s',
              }}
              onMouseEnter={e => {
                e.currentTarget.style.background = 'rgba(255,255,255,0.05)';
                e.currentTarget.style.color = 'white';
              }}
              onMouseLeave={e => {
                e.currentTarget.style.background = 'transparent';
                e.currentTarget.style.color = 'rgba(255,255,255,0.7)';
              }}
            >
              ← Kembali ke Pilihan
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
