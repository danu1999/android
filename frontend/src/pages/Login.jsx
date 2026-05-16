import React, { useState } from 'react';
import api from '../api';

export default function Login({ onLogin }) {
  const [name, setName] = useState('');
  const [pin, setPin] = useState('');
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

  const handleDemo = () => {
    const expiresAt = Date.now() + 3 * 24 * 60 * 60 * 1000; // 3 days
    onLogin({ id: 0, name: 'Demo User', role: 'OWNER', isDemo: true, expiresAt });
  };

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
          <div style={{
            width: '64px', height: '64px',
            background: 'linear-gradient(135deg, #818cf8, #6366f1)',
            borderRadius: '16px',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '28px',
            marginBottom: '1rem',
            boxShadow: '0 8px 24px rgba(99,102,241,0.4)'
          }}>🏪</div>
          <h1 style={{ color: 'white', fontWeight: 800, fontSize: '1.75rem', margin: 0 }}>POSBah</h1>
          <p style={{ color: 'rgba(255,255,255,0.55)', margin: '4px 0 0', fontSize: '0.875rem' }}>Masuk ke akun Anda</p>
        </div>

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
              onMouseDown={e => { if (d !== '') e.currentTarget.style.transform = 'scale(0.93)'; }}
              onMouseUp={e => { e.currentTarget.style.transform = 'scale(1)'; }}
              onTouchStart={e => { if (d !== '') e.currentTarget.style.transform = 'scale(0.93)'; }}
              onTouchEnd={e => { e.currentTarget.style.transform = 'scale(1)'; }}
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

        {/* Demo separator */}
        <div style={{ display:'flex', alignItems:'center', gap:'10px', margin:'16px 0 0' }}>
          <div style={{ flex:1, height:'1px', background:'rgba(255,255,255,0.15)' }} />
          <span style={{ color:'rgba(255,255,255,0.35)', fontSize:'0.75rem' }}>atau</span>
          <div style={{ flex:1, height:'1px', background:'rgba(255,255,255,0.15)' }} />
        </div>

        {/* Demo Button */}
        <button
          onClick={handleDemo}
          style={{
            width: '100%',
            padding: '14px',
            borderRadius: '14px',
            border: '1.5px solid rgba(255,255,255,0.2)',
            background: 'rgba(255,255,255,0.06)',
            color: 'rgba(255,255,255,0.7)',
            fontSize: '0.9rem',
            fontWeight: 600,
            cursor: 'pointer',
            marginTop: '10px',
            transition: 'all 0.2s',
            letterSpacing: '0.01em',
          }}
          onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.12)'}
          onMouseLeave={e => e.currentTarget.style.background = 'rgba(255,255,255,0.06)'}
        >
          🎯 Coba Demo Gratis (3 Hari)
        </button>

        <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.3)', fontSize: '0.75rem', marginTop: '1.5rem', marginBottom: 0 }}>
          Hubungi admin jika lupa PIN
        </p>
      </div>
    </div>
  );
}
