import React, { useState, useEffect } from 'react';
import api from '../api';
import { SocialLogin } from '@capgo/capacitor-social-login';

export default function Login({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleEmailLogin = async () => {
    if (!email.trim()) { setError('Masukkan email Anda'); return; }
    if (!password) { setError('Masukkan password Anda'); return; }
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/auth/login-email', { email: email.trim(), password });
      
      // Auto login to BMP in background (use fetch directly to avoid baseURL concatenation)
      try {
        const isLocalDev = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');
        const bmpLoginUrl = import.meta.env.VITE_API_URL_BMP
          ? `${import.meta.env.VITE_API_URL_BMP}/login`
          : (isLocalDev ? 'http://localhost:8080/api/login' : '/api-bmp/login');

        const bmpRes = await fetch(bmpLoginUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: email.trim(), password })
        });
        const bmpData = await bmpRes.json();
        if (bmpData && bmpData.success && bmpData.token) {
          localStorage.setItem('token', bmpData.token);
        }
      } catch (bmpErr) {
        console.warn('Background BMP login failed:', bmpErr);
      }

      onLogin(res.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Login email gagal, periksa akun Anda');
      setPassword('');
    } finally {
      setLoading(false);
    }
  };

  const handleNativeGoogleLogin = async () => {
    setLoading(true);
    setError('');
    try {
      // Inisialisasi plugin dengan webClientId
      await SocialLogin.initialize({
        google: {
          webClientId: import.meta.env.VITE_GOOGLE_CLIENT_ID || '276837280353-d83c8eo0nfo5v1ij1dr4okjjt443mbn0.apps.googleusercontent.com',
        }
      });

      const result = await SocialLogin.login({
        provider: 'google',
        options: {
          scopes: ['profile', 'email'],
        }
      });

      if (result?.result?.profile) {
        const profile = result.result.profile;
        await handleGoogleLoginResponse(null, profile);
      } else {
        setError('Login Google gagal, coba lagi');
      }
    } catch (err) {
      console.warn('Native Google login error:', err);
      if (err?.message?.includes('cancel') || err?.code === 'CANCELED') {
        setError('Login Google dibatalkan');
      } else {
        setError('Login Google gagal: ' + (err?.message || 'Coba lagi'));
      }
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

  const handleGoogleLoginResponse = async (response, nativeProfile = null) => {
    let emailVal, name, picture, sub;
    if (nativeProfile) {
      emailVal = nativeProfile.email;
      name = nativeProfile.name || nativeProfile.givenName;
      picture = nativeProfile.imageUrl || nativeProfile.picture;
      sub = nativeProfile.id || nativeProfile.sub;
    } else {
      const payload = parseJwt(response.credential);
      if (!payload || !payload.email) {
        setError('Gagal membaca profil Google');
        return;
      }
      emailVal = payload.email;
      name = payload.name;
      picture = payload.picture;
      sub = payload.sub;
    }

    if (emailVal) {
      try {
        const regRes = await api.post('/auth/google-register', { email: emailVal });
        const { registeredAt } = regRes.data;
        const regTime = new Date(registeredAt).getTime();
        const expiresAt = regTime + 3 * 24 * 60 * 60 * 1000;

        onLogin({
          id: sub,
          name: name || emailVal.split('@')[0],
          email: emailVal,
          picture: picture,
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
        
        let registeredAt = localUsers[emailVal];
        if (!registeredAt) {
          registeredAt = new Date().toISOString();
          localUsers[emailVal] = registeredAt;
          localStorage.setItem('posbah_google_users', JSON.stringify(localUsers));
        }

        const regTime = new Date(registeredAt).getTime();
        const expiresAt = regTime + 3 * 24 * 60 * 60 * 1000;

        onLogin({
          id: sub,
          name: name || emailVal.split('@')[0],
          email: emailVal,
          picture: picture,
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
  }, []);

  return (
    <div style={{
      minHeight: '100vh',
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
          <img src="/logo.png" alt="Logo" style={{
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
        <div>
          {/* Email Input */}
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', color: 'rgba(255,255,255,0.7)', fontSize: '0.8rem', fontWeight: 600, marginBottom: '8px', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
              Email
            </label>
            <input
              type="text"
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
            {loading ? 'Memproses...' : 'Masuk ke POSBah'}
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', margin: '24px 0 16px' }}>
            <div style={{ flex: 1, height: '1px', background: 'rgba(255,255,255,0.15)' }} />
            <span style={{ color: 'rgba(255,255,255,0.35)', fontSize: '0.75rem', fontWeight: 600 }}>SISTEM LAIN</span>
            <div style={{ flex: 1, height: '1px', background: 'rgba(255,255,255,0.15)' }} />
          </div>

          <button
            onClick={() => {
              localStorage.setItem('posbah_app_mode', 'BMP');
              window.location.reload();
            }}
            style={{
              width: '100%',
              padding: '16px',
              borderRadius: '16px',
              border: '1px solid rgba(59, 130, 246, 0.3)',
              background: 'rgba(59, 130, 246, 0.1)',
              color: '#93c5fd',
              fontSize: '0.95rem',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '12px',
              transition: 'all 0.2s',
              boxShadow: '0 4px 12px rgba(59,130,246,0.1)'
            }}
            onMouseEnter={e => {
              e.currentTarget.style.background = 'rgba(59, 130, 246, 0.2)';
              e.currentTarget.style.transform = 'translateY(-2px)';
            }}
            onMouseLeave={e => {
              e.currentTarget.style.background = 'rgba(59, 130, 246, 0.1)';
              e.currentTarget.style.transform = 'translateY(0)';
            }}
          >
            <span style={{ fontSize: '1.3rem' }}>🏭</span>
            <div style={{ textAlign: 'left' }}>
              <div style={{ fontWeight: 800 }}>Masuk ke Invoice & Manufaktur (BMP)</div>
              <div style={{ fontSize: '0.75rem', color: 'rgba(147,197,253,0.7)', fontWeight: 500 }}>Sistem Pabrik Bahtera Mulya</div>
            </div>
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', margin: '24px 0 16px' }}>
            <div style={{ flex: 1, height: '1px', background: 'rgba(255,255,255,0.15)' }} />
            <span style={{ color: 'rgba(255,255,255,0.35)', fontSize: '0.75rem', fontWeight: 600 }}>ATAU COBA DEMO</span>
            <div style={{ flex: 1, height: '1px', background: 'rgba(255,255,255,0.15)' }} />
          </div>

          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '10px', width: '100%' }}>
            {window.Capacitor || !window.google ? (
              <button
                onClick={handleNativeGoogleLogin}
                disabled={loading}
                style={{
                  width: '316px',
                  padding: '12px 16px',
                  borderRadius: '24px',
                  border: '1px solid rgba(255,255,255,0.15)',
                  background: 'white',
                  color: '#3c4043',
                  fontSize: '0.875rem',
                  fontWeight: 700,
                  cursor: loading ? 'not-allowed' : 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
                  transition: 'background-color 0.218s, border-color 0.218s, box-shadow 0.218s',
                  fontFamily: "'Roboto', arial, sans-serif"
                }}
                onMouseEnter={e => {
                  e.currentTarget.style.backgroundColor = '#f8f9fa';
                  e.currentTarget.style.boxShadow = '0 1px 3px rgba(60,64,67,0.3), 0 4px 8px 3px rgba(60,64,67,0.15)';
                }}
                onMouseLeave={e => {
                  e.currentTarget.style.backgroundColor = 'white';
                  e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.08)';
                }}
              >
                <svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" style={{ width: '18px', height: '18px', marginRight: '10px', display: 'block' }}>
                  <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"></path>
                  <path fill="#4285F4" d="M46.5 24c0-1.55-.15-3.24-.47-4.77H24v9.03h12.75c-.55 2.87-2.22 5.37-4.72 7.03l7.3 5.66C43.5 36.63 46.5 30.9 46.5 24z"></path>
                  <path fill="#FBBC05" d="M10.54 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.98-6.19z"></path>
                  <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.3-5.66c-2.03 1.36-4.63 2.17-8.59 2.17-6.26 0-11.57-4.22-13.46-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"></path>
                </svg>
                Masuk dengan Google (Demo)
              </button>
            ) : (
              <div id="google-signin-btn" />
            )}
          </div>

          <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.3)', fontSize: '0.75rem', marginTop: '1.5rem', marginBottom: 0 }}>
            Hubungi admin jika Anda mengalami kendala masuk
          </p>
        </div>
      </div>
    </div>
  );
}
