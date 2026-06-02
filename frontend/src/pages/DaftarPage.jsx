import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { SocialLogin } from '@capgo/capacitor-social-login';
import api from '../api';

const isCapacitor = (!!window.Capacitor && window.Capacitor.getPlatform && window.Capacitor.getPlatform() !== 'web') || window.location.protocol === 'capacitor:';

const PACKAGES = [
  {
    id: 'FNB',
    icon: '🍹',
    title: 'Retail & F&B',
    subtitle: 'Warung, Jualan Online, UMKM',
    color: '#f59e0b',
    gradient: 'linear-gradient(135deg, #f59e0b, #d97706)',
    shadow: 'rgba(245,158,11,0.3)',
    features: ['Kasir & Barcode Scanner', 'Manajemen Stok Produk', 'Diskon & Kupon Promo', 'Harga Grosir & Varian', 'Nomor Antrean Otomatis', 'Laporan Keuangan Harian']
  },
  {
    id: 'RENTAL',
    icon: '🚗',
    title: 'Rental Mobil',
    subtitle: 'Manajemen Armada Kendaraan',
    color: '#3b82f6',
    gradient: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
    shadow: 'rgba(59,130,246,0.3)',
    features: ['Manajemen Armada Mobil', 'Status Sewa Real-Time', 'Hitung Denda Keterlambatan', 'Data Penyewa & Identitas', 'Laporan Pendapatan Sewa', 'Riwayat Penyewaan Lengkap']
  },
  {
    id: 'LAUNDRY',
    icon: '🧺',
    title: 'Laundry',
    subtitle: 'Usaha Cuci & Setrika',
    color: '#8b5cf6',
    gradient: 'linear-gradient(135deg, #8b5cf6, #6d28d9)',
    shadow: 'rgba(139,92,246,0.3)',
    features: ['Order per Kg atau Satuan', 'Tracking Status Cucian', 'Notifikasi Selesai', 'Cetak Struk Laundry', 'Kelola Pengeluaran', 'Laporan Omzet Bulanan']
  },
  {
    id: 'BMP',
    icon: '🏭',
    title: 'Manufaktur & Invoice',
    subtitle: 'Pabrik & Invoice B2B',
    color: '#10b981',
    gradient: 'linear-gradient(135deg, #10b981, #059669)',
    shadow: 'rgba(16,185,129,0.3)',
    features: ['Faktur & Invoice Klien', 'Buku Kas Keuangan', 'Kalkulator HPP Produksi', 'Pricelist per Pelanggan', 'Gaji & Absensi Karyawan', 'Laporan Produksi & Bonus']
  }
];

export default function DaftarPage({ onLogin }) {
  const navigate = useNavigate();
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState('choose'); // 'choose' | 'google-login' | 'confirm'
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [googleProfile, setGoogleProfile] = useState(null);

  useEffect(() => {
    if (step === 'confirm' && !googleProfile) {
      window.location.href = 'https://www.zedmz.cloud';
    }
  }, [step, googleProfile]);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  useEffect(() => {
    /* global google */
    if (step !== 'google-login' || !selected) return;
    let retryCount = 0;
    const initGSI = () => {
      if (window.google) {
        google.accounts.id.initialize({
          client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID || '119416648055-06ujev0c1onnv8fs67no28dqhlca93fm.apps.googleusercontent.com',
          callback: handleGoogleResponse
        });
        const btn = document.getElementById('google-demo-btn');
        if (btn) {
          google.accounts.id.renderButton(btn, {
            theme: 'outline', size: 'large', width: '316', text: 'continue_with', shape: 'pill'
          });
        }
      } else if (retryCount < 20) {
        retryCount++;
        setTimeout(initGSI, 200);
      }
    };
    initGSI();
  }, [step, selected]);

  const parseJwt = (token) => {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(decodeURIComponent(window.atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')));
    } catch { return null; }
  };

  const doEmailRegister = async (e) => {
    e.preventDefault();
    if (!googleProfile) {
      window.location.href = 'https://www.zedmz.cloud';
      return;
    }
    if (!email || !password) return alert('Email dan password wajib diisi');
    setLoading(true);
    try {
      const res = await api.post('/auth/email-register-demo', {
        email: googleProfile.email,
        name: googleProfile.name,
        password: password,
        businessMode: selected
      });
      const { registeredAt, expiresAt } = res.data;

      onLogin({
        id: googleProfile.email,
        name: googleProfile.name || googleProfile.email.split('@')[0],
        email: googleProfile.email,
        role: 'OWNER',
        isDemo: true,
        businessMode: selected,
        registeredAt,
        expiresAt
      });

      // Set app mode berdasarkan paket
      const modeMap = { FNB: 'FNB', RENTAL: 'RENTAL', LAUNDRY: 'LAUNDRY', BMP: 'BMP' };
      localStorage.setItem('posbah_app_mode', modeMap[selected] || 'FNB');

      navigate('/');
    } catch (err) {
      console.error('email-register-demo error:', err);
      alert(err.response?.data?.error || 'Gagal mendaftar demo. Coba lagi.');
    } finally {
      setLoading(false);
    }
  };

  const doGoogleRegister = async (emailVal, nameVal) => {
    setLoading(true);
    try {
      const res = await api.post('/auth/google-register', {
        email: emailVal,
        name: nameVal,
        businessMode: selected
      });
      const { registeredAt, expiresAt, isNewUser } = res.data;

      onLogin({
        id: emailVal,
        name: nameVal || emailVal.split('@')[0],
        email: emailVal,
        role: 'OWNER',
        isDemo: true,
        businessMode: selected,
        registeredAt,
        expiresAt
      });

      // Set app mode berdasarkan paket
      const modeMap = { FNB: 'FNB', RENTAL: 'RENTAL', LAUNDRY: 'LAUNDRY', BMP: 'BMP' };
      localStorage.setItem('posbah_app_mode', modeMap[selected] || 'FNB');

      navigate('/');
    } catch (err) {
      console.error('google-register error:', err);
      alert('Gagal mendaftar. Coba lagi.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleResponse = async (response) => {
    const payload = parseJwt(response.credential);
    if (!payload?.email) return alert('Gagal membaca profil Google');
    setGoogleProfile({ email: payload.email, name: payload.name });
    setEmail(payload.email);
    setName(payload.name || payload.email.split('@')[0]);
    setStep('confirm');
  };

  const handleNativeGoogle = async () => {
    setLoading(true);
    try {
      await SocialLogin.initialize({
        google: { webClientId: import.meta.env.VITE_GOOGLE_CLIENT_ID || '119416648055-06ujev0c1onnv8fs67no28dqhlca93fm.apps.googleusercontent.com' }
      });
      const result = await SocialLogin.login({ provider: 'google', options: { scopes: ['profile', 'email'] } });
      const profile = result?.result?.profile;
      if (profile?.email) {
        setGoogleProfile({ email: profile.email, name: profile.name || profile.givenName });
        setEmail(profile.email);
        setName(profile.name || profile.givenName || profile.email.split('@')[0]);
        setStep('confirm');
      } else {
        alert('Login Google gagal');
      }
    } catch (err) {
      console.warn('Native Google error:', err);
      alert('Login Google gagal: ' + (err?.message || 'Coba lagi'));
    } finally {
      setLoading(false);
    }
  };

  const pkg = PACKAGES.find(p => p.id === selected);

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0f0c29 0%, #1a1547 50%, #24243e 100%)',
      fontFamily: "'Inter', sans-serif",
      padding: isMobile ? '20px 16px' : '40px 24px'
    }}>
      {/* Header */}
      <div style={{ textAlign: 'center', marginBottom: isMobile ? '28px' : '40px' }}>
        <img src="/logo.png" alt="POSBah" style={{ width: '56px', height: '56px', borderRadius: '14px', objectFit: 'cover', marginBottom: '12px', boxShadow: '0 8px 24px rgba(99,102,241,0.4)' }} />
        <h1 style={{ color: 'white', fontWeight: 800, fontSize: isMobile ? '24px' : '32px', margin: '0 0 8px' }}>Coba POSBah Gratis</h1>
        <p style={{ color: 'rgba(255,255,255,0.55)', fontSize: '15px', margin: 0 }}>
          {step === 'choose' ? 'Pilih paket yang sesuai dengan bisnis Anda' : `Konfirmasi paket dan masuk dengan Google`}
        </p>
        {(step === 'confirm' || step === 'google-login') && (
          <button onClick={() => { setGoogleProfile(null); setStep('choose'); }} style={{ marginTop: '12px', background: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.15)', color: 'rgba(255,255,255,0.7)', padding: '6px 16px', borderRadius: '20px', cursor: 'pointer', fontSize: '13px' }}>
            ← Ganti Paket
          </button>
        )}
      </div>

      {/* Step 1: Pilih Paket */}
      {step === 'choose' && (
        <div style={{ maxWidth: '960px', margin: '0 auto' }}>
          <div style={{
            display: 'grid',
            gridTemplateColumns: isMobile ? '1fr' : 'repeat(2, 1fr)',
            gap: '20px',
            marginBottom: '32px'
          }}>
            {PACKAGES.map((pkg) => (
              <div
                key={pkg.id}
                onClick={() => setSelected(pkg.id)}
                style={{
                  background: selected === pkg.id
                    ? `linear-gradient(135deg, ${pkg.color}22, ${pkg.color}11)`
                    : 'rgba(255,255,255,0.05)',
                  border: `2px solid ${selected === pkg.id ? pkg.color : 'rgba(255,255,255,0.1)'}`,
                  borderRadius: '20px',
                  padding: '24px',
                  cursor: 'pointer',
                  transition: 'all 0.25s',
                  transform: selected === pkg.id ? 'scale(1.02)' : 'scale(1)',
                  boxShadow: selected === pkg.id ? `0 8px 32px ${pkg.shadow}` : 'none',
                  backdropFilter: 'blur(10px)'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginBottom: '16px' }}>
                  <div style={{
                    width: '52px', height: '52px', borderRadius: '14px',
                    background: pkg.gradient, display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '24px', boxShadow: `0 4px 12px ${pkg.shadow}`
                  }}>
                    {pkg.icon}
                  </div>
                  <div>
                    <div style={{ color: 'white', fontWeight: 800, fontSize: '18px' }}>{pkg.title}</div>
                    <div style={{ color: 'rgba(255,255,255,0.5)', fontSize: '13px' }}>{pkg.subtitle}</div>
                  </div>
                  {selected === pkg.id && (
                    <div style={{ marginLeft: 'auto', background: pkg.color, color: 'white', borderRadius: '50%', width: '26px', height: '26px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', fontSize: '14px' }}>✓</div>
                  )}
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px' }}>
                  {pkg.features.map((f, i) => (
                    <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'rgba(255,255,255,0.7)', fontSize: '12px' }}>
                      <span style={{ color: pkg.color, fontWeight: 'bold' }}>✓</span> {f}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>

          <div style={{ textAlign: 'center' }}>
            <button
              onClick={() => selected && setStep('google-login')}
              disabled={!selected}
              style={{
                padding: '16px 48px', borderRadius: '14px', border: 'none', fontSize: '17px', fontWeight: 800, cursor: selected ? 'pointer' : 'not-allowed',
                background: selected ? (PACKAGES.find(p => p.id === selected)?.gradient || 'rgba(255,255,255,0.1)') : 'rgba(255,255,255,0.1)',
                color: selected ? 'white' : 'rgba(255,255,255,0.3)',
                boxShadow: selected ? `0 8px 24px ${PACKAGES.find(p => p.id === selected)?.shadow}` : 'none',
                transition: 'all 0.25s', transform: selected ? 'translateY(0)' : 'none'
              }}
              onMouseEnter={e => selected && (e.currentTarget.style.transform = 'translateY(-2px)')}
              onMouseLeave={e => e.currentTarget.style.transform = 'translateY(0)'}
            >
              Lanjutkan →
            </button>

            <div style={{ marginTop: '20px' }}>
              <button onClick={() => navigate('/')} style={{ background: 'none', border: 'none', color: 'rgba(255,255,255,0.4)', cursor: 'pointer', fontSize: '14px' }}>
                Sudah punya akun? <span style={{ color: 'rgba(255,255,255,0.7)', textDecoration: 'underline' }}>Login di sini</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Step 2: Wajib Login Google */}
      {step === 'google-login' && pkg && (
        <div style={{ maxWidth: '420px', margin: '0 auto' }}>
          <div style={{
            background: 'rgba(255,255,255,0.07)',
            backdropFilter: 'blur(20px)',
            borderRadius: '24px',
            border: `1px solid ${pkg.color}44`,
            padding: '32px',
            boxShadow: `0 20px 60px rgba(0,0,0,0.4), 0 0 0 1px ${pkg.color}22`,
            textAlign: 'center'
          }}>
            {/* Selected Package Badge */}
            <div style={{
              display: 'inline-flex', alignItems: 'center', gap: '10px',
              background: `${pkg.color}22`, border: `1px solid ${pkg.color}44`,
              borderRadius: '12px', padding: '10px 16px', marginBottom: '24px'
            }}>
              <span style={{ fontSize: '20px' }}>{pkg.icon}</span>
              <div style={{ textAlign: 'left' }}>
                <div style={{ color: 'white', fontWeight: 700, fontSize: '15px' }}>{pkg.title}</div>
                <div style={{ color: 'rgba(255,255,255,0.5)', fontSize: '12px' }}>{pkg.subtitle}</div>
              </div>
            </div>

            <h2 style={{ color: 'white', fontWeight: 800, margin: '0 0 8px', fontSize: '20px' }}>Wajib Login Google</h2>
            <p style={{ color: 'rgba(255,255,255,0.5)', fontSize: '13px', margin: '0 0 24px', lineHeight: 1.5 }}>
              Untuk memverifikasi identitas Anda dan mencegah penyalahgunaan demo, Anda wajib masuk menggunakan Google terlebih dahulu.
            </p>

            {loading ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', padding: '20px', color: 'rgba(255,255,255,0.7)' }}>
                <div style={{ width: '20px', height: '20px', border: '2px solid rgba(255,255,255,0.2)', borderTop: '2px solid white', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
                Memproses...
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '16px' }}>
                {isCapacitor ? (
                  <button
                    onClick={handleNativeGoogle}
                    style={{
                      width: '100%',
                      padding: '14px',
                      borderRadius: '14px',
                      border: 'none',
                      background: 'white',
                      color: '#1f2937',
                      fontSize: '0.95rem',
                      fontWeight: 700,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '10px',
                      transition: 'all 0.2s',
                      boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
                    }}
                  >
                    <svg viewBox="0 0 24 24" width="18" height="18" style={{ marginRight: '6px' }}>
                      <path fill="#EA4335" d="M12.24 10.285V14.4h6.887c-.648 2.41-2.519 4.114-5.136 4.114-3.51 0-6.357-2.847-6.357-6.357s2.847-6.357 6.357-6.357c1.6 0 3.056.592 4.185 1.565l3.204-3.203C19.262 2.21 15.96 1 12.24 1 5.92 1 1 5.92 1 12.24s4.92 11.24 11.24 11.24c5.98 0 11.24-4.32 11.24-11.24 0-.74-.08-1.46-.22-2.155H12.24z"/>
                    </svg>
                    Masuk dengan Google
                  </button>
                ) : (
                  <div id="google-demo-btn" style={{ minHeight: '44px', display: 'flex', justifyContent: 'center' }} />
                )}
              </div>
            )}

            <div style={{ marginTop: '24px', padding: '12px', background: 'rgba(255,255,255,0.05)', borderRadius: '10px' }}>
              <p style={{ color: 'rgba(255,255,255,0.4)', fontSize: '12px', margin: 0, lineHeight: 1.5 }}>
                🔒 Kami menjaga privasi Anda. POSBah hanya meminta akses ke profil dasar dan email untuk verifikasi keamanan akun demo.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Step 3: Konfirmasi & Input Password */}
      {step === 'confirm' && pkg && (
        <div style={{ maxWidth: '420px', margin: '0 auto' }}>
          <div style={{
            background: 'rgba(255,255,255,0.07)',
            backdropFilter: 'blur(20px)',
            borderRadius: '24px',
            border: `1px solid ${pkg.color}44`,
            padding: '32px',
            boxShadow: `0 20px 60px rgba(0,0,0,0.4), 0 0 0 1px ${pkg.color}22`,
            textAlign: 'center'
          }}>
            {/* Selected Package Badge */}
            <div style={{
              display: 'inline-flex', alignItems: 'center', gap: '10px',
              background: `${pkg.color}22`, border: `1px solid ${pkg.color}44`,
              borderRadius: '12px', padding: '10px 16px', marginBottom: '24px'
            }}>
              <span style={{ fontSize: '20px' }}>{pkg.icon}</span>
              <div style={{ textAlign: 'left' }}>
                <div style={{ color: 'white', fontWeight: 700, fontSize: '15px' }}>{pkg.title}</div>
                <div style={{ color: 'rgba(255,255,255,0.5)', fontSize: '12px' }}>{pkg.subtitle}</div>
              </div>
            </div>

            <h2 style={{ color: 'white', fontWeight: 800, margin: '0 0 8px', fontSize: '20px' }}>Registrasi Demo 2 Hari</h2>
            <p style={{ color: 'rgba(255,255,255,0.5)', fontSize: '13px', margin: '0 0 20px', lineHeight: 1.5 }}>
              Lengkapi pembuatan akun demo Anda dengan menentukan password baru di bawah ini.
            </p>

            {loading ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', padding: '20px', color: 'rgba(255,255,255,0.7)' }}>
                <div style={{ width: '20px', height: '20px', border: '2px solid rgba(255,255,255,0.2)', borderTop: '2px solid white', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
                <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
                Memproses...
              </div>
            ) : (
              <form onSubmit={doEmailRegister} style={{ textAlign: 'left' }}>
                <div style={{ marginBottom: '14px' }}>
                  <label style={{ display: 'block', color: 'rgba(255,255,255,0.7)', fontSize: '12px', fontWeight: 600, marginBottom: '5px' }}>Nama Lengkap / Bisnis (dari Google)</label>
                  <input
                    type="text"
                    value={name}
                    readOnly
                    disabled
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      borderRadius: '10px',
                      border: '1px solid rgba(255,255,255,0.1)',
                      background: 'rgba(255,255,255,0.03)',
                      color: 'rgba(255,255,255,0.5)',
                      fontSize: '13px',
                      outline: 'none',
                      cursor: 'not-allowed'
                    }}
                    required
                  />
                </div>

                <div style={{ marginBottom: '14px' }}>
                  <label style={{ display: 'block', color: 'rgba(255,255,255,0.7)', fontSize: '12px', fontWeight: 600, marginBottom: '5px' }}>Alamat Email (dari Google)</label>
                  <input
                    type="email"
                    value={email}
                    readOnly
                    disabled
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      borderRadius: '10px',
                      border: '1px solid rgba(255,255,255,0.1)',
                      background: 'rgba(255,255,255,0.03)',
                      color: 'rgba(255,255,255,0.5)',
                      fontSize: '13px',
                      outline: 'none',
                      cursor: 'not-allowed'
                    }}
                    required
                  />
                </div>

                <div style={{ marginBottom: '20px' }}>
                  <label style={{ display: 'block', color: 'rgba(255,255,255,0.7)', fontSize: '12px', fontWeight: 600, marginBottom: '5px' }}>Password Baru</label>
                  <input
                    type="password"
                    placeholder="Buat password (min. 6 karakter)"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      borderRadius: '10px',
                      border: '1px solid rgba(255,255,255,0.15)',
                      background: 'rgba(255,255,255,0.05)',
                      color: 'white',
                      fontSize: '13px',
                      outline: 'none',
                      transition: 'all 0.2s'
                    }}
                    minLength={6}
                    required
                  />
                </div>

                <button
                  type="submit"
                  style={{
                    width: '100%',
                    padding: '12px',
                    borderRadius: '10px',
                    border: 'none',
                    fontSize: '14px',
                    fontWeight: 800,
                    cursor: 'pointer',
                    background: pkg.gradient,
                    color: 'white',
                    boxShadow: `0 6px 16px ${pkg.shadow}`,
                    transition: 'all 0.25s',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px'
                  }}
                >
                  Mulai Demo {pkg.title} 🚀
                </button>
              </form>
            )}

            <div style={{ marginTop: '20px', padding: '12px', background: 'rgba(255,255,255,0.05)', borderRadius: '10px' }}>
              <p style={{ color: 'rgba(255,255,255,0.4)', fontSize: '12px', margin: 0, lineHeight: 1.5 }}>
                🔒 Demo gratis 2 hari. Setelah konfirmasi pembayaran oleh admin, akun Anda akan diaktifkan penuh tanpa batas waktu.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
