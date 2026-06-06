import React, { useState, useEffect } from 'react';
import api from '../api';
import { SocialLogin } from '@capgo/capacitor-social-login';
import { Download } from 'lucide-react';

export default function Login({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [latestVer, setLatestVer] = useState("1.1.0");

  const isCapacitor = (!!window.Capacitor && window.Capacitor.getPlatform && window.Capacitor.getPlatform() !== 'web') || window.location.protocol === 'capacitor:';

  useEffect(() => {
    const fetchVer = async () => {
      try {
        const res = await api.get('/apk-version');
        if (res.data && res.data.version) {
          setLatestVer(res.data.version);
        }
      } catch (err) {
        console.warn("Gagal mengambil versi APK di Login page:", err);
      }
    };
    fetchVer();
  }, []);

  const handleEmailLogin = async () => {
    if (!email.trim()) { setError('Masukkan email Anda'); return; }
    if (!password) { setError('Masukkan password Anda'); return; }
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/auth/login-email', { email: email.trim(), password });
      
      // Auto login to BMP in background (use fetch directly to avoid baseURL concatenation)
      try {
        const isCapacitor = (!!window.Capacitor && window.Capacitor.getPlatform && window.Capacitor.getPlatform() !== 'web') || window.location.protocol === 'capacitor:';
        const isLocalDev = !isCapacitor && (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') && window.location.port !== '';
        
        let bmpLoginUrl = '';
        if (import.meta.env.VITE_API_URL_BMP) {
          bmpLoginUrl = `${import.meta.env.VITE_API_URL_BMP}/login`;
        } else if (isLocalDev) {
          bmpLoginUrl = 'http://localhost:8080/api/login';
        } else {
          const base = isCapacitor ? 'https://www.zedmz.cloud' : '';
          bmpLoginUrl = `${base}/api-bmp/login`;
        }

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
      await SocialLogin.initialize({
        google: {
          webClientId: import.meta.env.VITE_GOOGLE_CLIENT_ID || '119416648055-06ujev0c1onnv8fs67no28dqhlca93fm.apps.googleusercontent.com',
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
    let retryCount = 0;
    const initGoogleGSI = () => {
      if (window.google) {
        google.accounts.id.initialize({
          client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID || "119416648055-06ujev0c1onnv8fs67no28dqhlca93fm.apps.googleusercontent.com",
          callback: handleGoogleLoginResponse
        });
        const btnElem = document.getElementById("google-signin-btn");
        if (btnElem) {
          google.accounts.id.renderButton(
            btnElem,
            { 
              theme: "outline", 
              size: "large", 
              width: "316", 
              text: "continue_with",
              shape: "pill"
            }
          );
        }
      } else if (retryCount < 20) {
        retryCount++;
        setTimeout(initGoogleGSI, 200);
      }
    };

    initGoogleGSI();
  }, []);

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4c1d95 100%)',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '2rem 1rem',
      fontFamily: "'Inter', sans-serif",
      gap: '20px'
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
            <span style={{ color: 'rgba(255,255,255,0.35)', fontSize: '0.75rem', fontWeight: 600 }}>ATAU COBA DEMO</span>
            <div style={{ flex: 1, height: '1px', background: 'rgba(255,255,255,0.15)' }} />
          </div>

          <button
            onClick={() => window.location.href = '/daftar'}
            style={{
              width: '100%',
              padding: '14px',
              borderRadius: '14px',
              border: '1px solid rgba(99,102,241,0.3)',
              background: 'rgba(99,102,241,0.1)',
              color: '#a5b4fc',
              fontSize: '0.95rem',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px',
              transition: 'all 0.2s',
            }}
            onMouseEnter={e => {
              e.currentTarget.style.background = 'rgba(99,102,241,0.2)';
              e.currentTarget.style.transform = 'translateY(-2px)';
            }}
            onMouseLeave={e => {
              e.currentTarget.style.background = 'rgba(99,102,241,0.1)';
              e.currentTarget.style.transform = 'translateY(0)';
            }}
          >
            <span style={{ fontSize: '1.2rem' }}>🚀</span>
            <div style={{ textAlign: 'left' }}>
              <div style={{ fontWeight: 800 }}>Coba Demo Gratis 2 Hari</div>
              <div style={{ fontSize: '0.72rem', color: 'rgba(165,180,252,0.7)', fontWeight: 500 }}>Pilih paket bisnis Anda terlebih dahulu</div>
            </div>
          </button>

          <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.3)', fontSize: '0.75rem', marginTop: '1.5rem', marginBottom: 0 }}>
            Hubungi admin jika Anda mengalami kendala masuk
          </p>

        </div>
      </div>

      {/* Website APK Download Banner */}
      {!isCapacitor && (
        <div style={{
          background: 'rgba(255, 255, 255, 0.08)',
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          border: '1px solid rgba(255, 255, 255, 0.15)',
          borderRadius: '20px',
          padding: '16px 20px',
          maxWidth: '380px',
          width: '100%',
          boxSizing: 'border-box',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px',
          boxShadow: '0 15px 30px rgba(0, 0, 0, 0.2)',
          color: 'white',
          animation: 'fadeIn 0.5s ease'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
            <div style={{
              background: 'linear-gradient(135deg, #10B981, #059669)',
              borderRadius: '12px',
              padding: '10px',
              display: 'flex',
              color: 'white',
              boxShadow: '0 4px 14px rgba(16, 185, 129, 0.3)'
            }}>
              <Download size={20} />
            </div>
            <div>
              <h4 style={{ margin: '0 0 2px', fontSize: '0.85rem', fontWeight: 800, color: '#f8fafc' }}>
                Unduh POSBah Android (v{latestVer}) 📱
              </h4>
              <p style={{ margin: 0, fontSize: '0.72rem', color: '#a7f3d0', fontWeight: 500, lineHeight: 1.4 }}>
                Cetak struk bluetooth &amp; transaksi offline lebih cepat.
              </p>
              <p style={{ margin: '4px 0 0', fontSize: '0.62rem', color: '#fca5a5', fontWeight: 700, lineHeight: 1.3 }}>
                ⚠️ Update ini penting untuk pengusaha manufaktur. POS UMKM, laundry, Rental tidak perlu update.
              </p>
            </div>
          </div>
          <button
            onClick={() => alert('Untuk keamanan, silakan masuk (login) ke akun POSBah Anda terlebih dahulu. Menu unduhan APK tersedia di dalam menu samping (sidebar) dashboard setelah Anda masuk sebagai akun Premium.')}
            style={{
              background: 'linear-gradient(135deg, #10B981, #059669)',
              color: 'white',
              padding: '10px 14px',
              borderRadius: '10px',
              fontSize: '0.75rem',
              fontWeight: 800,
              border: 'none',
              cursor: 'pointer',
              boxShadow: '0 4px 14px rgba(16, 185, 129, 0.35)',
              whiteSpace: 'nowrap',
              display: 'inline-flex',
              alignItems: 'center',
              transition: 'transform 0.15s'
            }}
            onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-1px)'; }}
            onMouseLeave={e => { e.currentTarget.style.transform = ''; }}
          >
            Unduh APK
          </button>
        </div>
      )}
    </div>
  );
}
