import React, { useState, useEffect } from 'react';
import api from '../services/api';

const MACHINES = [
    { name: 'Baskom Panda', bonus: 5000, color: '#3b82f6' },
    { name: 'Baskom Mawar', bonus: 5000, color: '#22c55e' },
    { name: 'Baskom Jago', bonus: 7000, color: '#ef4444' },
    { name: 'Baskom Smile 12', bonus: 7000, color: '#f97316' },
    { name: 'Bak Kuping', bonus: 8000, color: '#a855f7' },
    { name: 'Wakul Telor', bonus: 5000, color: '#eab308' },
    { name: 'Baskom Durian', bonus: 5000, color: '#84cc16' },
    { name: 'Wakul Moris', bonus: 5000, color: '#06b6d4' },
    { name: 'Bahtera TM', bonus: 5000, color: '#ec4899' },
    { name: 'BMP', bonus: 5000, color: '#64748b' },
];
const SHIFTS = ['Pagi', 'Sore', 'Malam'];

const BonusClaim: React.FC = () => {
    const [step, setStep] = useState<'pin' | 'form' | 'success'>('pin');
    const [view, setView] = useState<'login' | 'pinlist'>('login'); // toggle
    const [pin, setPin] = useState('');
    const [employee, setEmployee] = useState<{ id: number; name: string } | null>(null);
    const [machine, setMachine] = useState('');
    const [shift, setShift] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [result, setResult] = useState<any>(null);
    const [pinList, setPinList] = useState<{ pin: string; name: string }[]>([]);
    const [jumlahPerolehan, setJumlah] = useState('');

    useEffect(() => {
        api.get('/bonus/pin-list').then(res => {
            if (res.data?.data) setPinList(res.data.data);
        }).catch(() => { });
    }, []);

    const handleVerifyPIN = async () => {
        if (!pin) return setError('Masukkan PIN Anda');
        setLoading(true); setError('');
        try {
            const res = await api.post('/bonus/verify-pin', { pin });
            setEmployee(res.data.data);
            setStep('form');
        } catch (err: any) {
            setError(err.response?.data?.message || 'PIN salah atau tidak terdaftar');
        } finally { setLoading(false); }
    };

    const handleClaim = async () => {
        if (!machine) return setError('Pilih mesin terlebih dahulu');
        if (!shift) return setError('Pilih shift terlebih dahulu');
        if (!jumlahPerolehan || Number(jumlahPerolehan) <= 0) return setError('Masukkan jumlah perolehan');
        setLoading(true); setError('');
        try {
            const res = await api.post('/bonus/claim', {
                pin,
                machine_name: machine,
                shift_type: shift,
                jumlah_perolehan: parseInt(jumlahPerolehan),
            });
            setResult(res.data.data);
            setStep('success');
        } catch (err: any) {
            setError(err.response?.data?.message || 'Gagal mengklaim bonus');
        } finally { setLoading(false); }
    };

    const reset = () => {
        setStep('pin'); setView('login');
        setPin(''); setEmployee(null);
        setMachine(''); setShift('');
        setError(''); setResult(null);
        setJumlah('');
    };

    /* ─── Styles ─────────────────────────────────────────── */
    const page: React.CSSProperties = {
        height: '100dvh',
        overflow: 'hidden',
        background: 'linear-gradient(160deg, #0f172a 0%, #1e293b 100%)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        padding: '10px 12px',
        fontFamily: "'Segoe UI', Arial, sans-serif",
        boxSizing: 'border-box',
        width: '100%',
        gap: '8px',
    };

    const card: React.CSSProperties = {
        width: '100%',
        maxWidth: '440px',
        background: 'rgba(255,255,255,0.05)',
        border: '1px solid rgba(255,255,255,0.1)',
        borderRadius: '16px',
        padding: '16px 14px',
        color: 'white',
        flexShrink: 0,
    };

    const inputStyle: React.CSSProperties = {
        width: '100%',
        padding: '12px',
        fontSize: '22px',
        fontWeight: 'bold',
        textAlign: 'center',
        letterSpacing: '8px',
        background: 'rgba(255,255,255,0.08)',
        border: '2px solid rgba(255,255,255,0.15)',
        borderRadius: '10px',
        color: 'white',
        outline: 'none',
        boxSizing: 'border-box',
    };

    const btn = (color: string, mt = '8px'): React.CSSProperties => ({
        width: '100%', padding: '11px',
        background: color, border: 'none',
        borderRadius: '10px', color: 'white',
        fontSize: '14px', fontWeight: 'bold',
        cursor: 'pointer', marginTop: mt,
        opacity: loading ? 0.7 : 1,
    });

    const btnGhost: React.CSSProperties = {
        width: '100%', padding: '9px',
        background: 'transparent',
        border: '1px solid rgba(255,255,255,0.15)',
        borderRadius: '10px', color: 'rgba(255,255,255,0.55)',
        fontSize: '13px', cursor: 'pointer', marginTop: '6px',
    };

    const errBox: React.CSSProperties = {
        background: 'rgba(239,68,68,0.15)',
        border: '1px solid rgba(239,68,68,0.35)',
        borderRadius: '8px', padding: '7px 10px',
        marginBottom: '8px', fontSize: '12px',
        color: '#fca5a5', textAlign: 'center',
    };

    /* ─── PIN list split 2 kolom ─────────────────────────── */
    const half = Math.ceil(pinList.length / 2);
    const col1 = pinList.slice(0, half);
    const col2 = pinList.slice(half);

    const pinCell = (even: boolean): React.CSSProperties => ({
        display: 'flex', justifyContent: 'space-between',
        alignItems: 'center', padding: '5px 8px',
        background: even ? 'rgba(255,255,255,0.03)' : 'transparent',
    });
    const pinBadge: React.CSSProperties = {
        background: 'rgba(59,130,246,0.25)',
        border: '1px solid rgba(59,130,246,0.45)',
        borderRadius: '5px', padding: '1px 7px',
        fontWeight: 900, fontSize: '13px',
        color: '#93c5fd', fontFamily: 'monospace',
        letterSpacing: '1px', flexShrink: 0,
    };

    return (
        <div style={page}>

            {/* ── Header + Toggle Buttons ── */}
            <div style={{ width: '100%', maxWidth: '440px', flexShrink: 0 }}>
                <p style={{ color: 'rgba(255,255,255,0.5)', fontSize: '11px', margin: '0 0 6px', textAlign: 'center' }}>
                    🏭 Klaim Bonus Mesin · CV BMP
                </p>
                {/* Toggle hanya muncul saat step = 'pin' */}
                {step === 'pin' && (
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                        <button
                            onClick={() => { setView('login'); setError(''); }}
                            style={{
                                padding: '10px',
                                borderRadius: '10px',
                                border: 'none',
                                background: view === 'login'
                                    ? 'linear-gradient(135deg, #3b82f6, #1d4ed8)'
                                    : 'rgba(255,255,255,0.08)',
                                color: 'white',
                                fontWeight: view === 'login' ? 800 : 500,
                                fontSize: '13px',
                                cursor: 'pointer',
                                transition: 'all 0.2s',
                            }}
                        >
                            🔐 Login PIN
                        </button>
                        <button
                            onClick={() => { setView('pinlist'); setError(''); }}
                            style={{
                                padding: '10px',
                                borderRadius: '10px',
                                border: 'none',
                                background: view === 'pinlist'
                                    ? 'linear-gradient(135deg, #7c3aed, #5b21b6)'
                                    : 'rgba(255,255,255,0.08)',
                                color: 'white',
                                fontWeight: view === 'pinlist' ? 800 : 500,
                                fontSize: '13px',
                                cursor: 'pointer',
                                transition: 'all 0.2s',
                            }}
                        >
                            🪪 Nama &amp; PIN
                        </button>
                    </div>
                )}
            </div>

            {/* ── VIEW: Login PIN ── */}
            {step === 'pin' && view === 'login' && (
                <div style={card}>
                    {error && <div style={errBox}>⚠️ {error}</div>}
                    <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.55)', margin: '0 0 8px', fontSize: '12px' }}>
                        Masukkan PIN fingerprint Anda
                    </p>
                    <input
                        type="password" inputMode="numeric"
                        placeholder="PIN" value={pin}
                        onChange={e => setPin(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleVerifyPIN()}
                        style={inputStyle} autoFocus
                    />
                    <button onClick={handleVerifyPIN} disabled={loading} style={btn('linear-gradient(135deg,#3b82f6,#1d4ed8)')}>
                        {loading ? 'Memverifikasi...' : '🔐 Verifikasi PIN'}
                    </button>
                </div>
            )}

            {/* ── VIEW: Daftar PIN ── */}
            {step === 'pin' && view === 'pinlist' && pinList.length > 0 && (
                <div style={{
                    width: '100%', maxWidth: '440px',
                    background: 'rgba(255,255,255,0.05)',
                    border: '1px solid rgba(255,255,255,0.1)',
                    borderRadius: '14px', overflow: 'hidden', flexShrink: 0,
                }}>
                    <div style={{ background: 'rgba(124,58,237,0.4)', padding: '6px 12px', fontSize: '11px', fontWeight: 800, color: 'white' }}>
                        🪪 Daftar PIN Karyawan
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr' }}>
                        {/* Kolom kiri */}
                        <div style={{ borderRight: '1px solid rgba(255,255,255,0.07)' }}>
                            {col1.map((row, i) => (
                                <div key={i} style={pinCell(i % 2 === 0)}>
                                    <span style={{ color: 'rgba(255,255,255,0.8)', fontSize: '11px', fontWeight: 600, textTransform: 'capitalize', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginRight: '4px' }}>
                                        {row.name}
                                    </span>
                                    <span style={pinBadge}>{row.pin}</span>
                                </div>
                            ))}
                        </div>
                        {/* Kolom kanan */}
                        <div>
                            {col2.map((row, i) => (
                                <div key={i} style={pinCell(i % 2 === 0)}>
                                    <span style={{ color: 'rgba(255,255,255,0.8)', fontSize: '11px', fontWeight: 600, textTransform: 'capitalize', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginRight: '4px' }}>
                                        {row.name}
                                    </span>
                                    <span style={pinBadge}>{row.pin}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {/* ── STEP 2: Pilih Mesin & Shift ── */}
            {step === 'form' && employee && (
                <div style={{ ...card, overflowY: 'auto', flexShrink: 1 }}>
                    {error && <div style={errBox}>⚠️ {error}</div>}

                    <div style={{ background: 'rgba(34,197,94,0.12)', border: '1px solid rgba(34,197,94,0.3)', borderRadius: '8px', padding: '8px 12px', marginBottom: '10px', textAlign: 'center' }}>
                        <div style={{ fontSize: '11px', color: 'rgba(255,255,255,0.45)' }}>Terverifikasi ✓</div>
                        <div style={{ fontWeight: 800, fontSize: '16px', textTransform: 'capitalize' }}>{employee.name}</div>
                    </div>

                    <p style={{ fontWeight: 700, margin: '0 0 6px', fontSize: '12px', color: 'rgba(255,255,255,0.55)' }}>Pilih Mesin:</p>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '10px' }}>
                        {MACHINES.map(m => (
                            <button key={m.name} onClick={() => setMachine(m.name)} style={{
                                padding: '9px 12px', borderRadius: '9px',
                                border: machine === m.name ? `2px solid ${m.color}` : '2px solid rgba(255,255,255,0.1)',
                                background: machine === m.name ? `${m.color}22` : 'rgba(255,255,255,0.04)',
                                color: 'white', cursor: 'pointer',
                                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                fontSize: '13px', fontWeight: machine === m.name ? 800 : 500,
                            }}>
                                <span>{m.name}</span>
                                <span style={{ color: machine === m.name ? m.color : 'rgba(255,255,255,0.3)', fontWeight: 800, fontSize: '12px' }}>
                                    Rp {m.bonus.toLocaleString('id-ID')}
                                </span>
                            </button>
                        ))}
                    </div>

                    {/* Input Jumlah Perolehan — muncul setelah mesin dipilih */}
                    {machine && (
                        <div style={{ marginBottom: '10px' }}>
                            <p style={{ fontWeight: 700, margin: '0 0 6px', fontSize: '12px', color: 'rgba(255,255,255,0.55)' }}>
                                Jumlah Perolehan <span style={{ color: 'rgba(255,255,255,0.3)', fontWeight: 400 }}>(tumpukan)</span>:
                            </p>
                            <input
                                type="number"
                                inputMode="numeric"
                                min="1"
                                placeholder="Contoh: 50"
                                value={jumlahPerolehan}
                                onChange={e => setJumlah(e.target.value.replace(/\D/g, ''))}
                                style={{
                                    width: '100%',
                                    padding: '11px 14px',
                                    fontSize: '18px',
                                    fontWeight: 'bold',
                                    textAlign: 'center',
                                    background: 'rgba(255,255,255,0.08)',
                                    border: '2px solid rgba(255,255,255,0.2)',
                                    borderRadius: '10px',
                                    color: 'white',
                                    outline: 'none',
                                    boxSizing: 'border-box',
                                }}
                            />
                        </div>
                    )}

                    <p style={{ fontWeight: 700, margin: '0 0 6px', fontSize: '12px', color: 'rgba(255,255,255,0.55)' }}>Pilih Shift:</p>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: '6px', marginBottom: '10px' }}>
                        {SHIFTS.map(s => (
                            <button key={s} onClick={() => setShift(s)} style={{
                                padding: '9px 4px', borderRadius: '9px',
                                border: shift === s ? '2px solid #f59e0b' : '2px solid rgba(255,255,255,0.1)',
                                background: shift === s ? 'rgba(245,158,11,0.15)' : 'rgba(255,255,255,0.04)',
                                color: shift === s ? '#fcd34d' : 'rgba(255,255,255,0.5)',
                                cursor: 'pointer', fontWeight: shift === s ? 800 : 500, fontSize: '12px',
                            }}>
                                {s === 'Pagi' ? '☀️' : (s === 'Siang' || s === 'Sore') ? '🌤️' : '🌙'}<br />{s}
                            </button>
                        ))}
                    </div>

                    <button onClick={handleClaim} disabled={loading} style={btn('linear-gradient(135deg,#22c55e,#15803d)', '0')}>
                        {loading ? 'Menyimpan...' : '✅ Klaim Bonus Sekarang'}
                    </button>
                    <button onClick={reset} style={btnGhost}>← Kembali</button>
                </div>
            )}

            {/* ── STEP 3: Sukses ── */}
            {step === 'success' && result && (
                <div style={{ ...card, textAlign: 'center' }}>
                    <div style={{ fontSize: '52px', marginBottom: '10px' }}>🎉</div>
                    <h3 style={{ fontSize: '18px', fontWeight: 800, margin: '0 0 4px' }}>Bonus Diklaim!</h3>
                    <p style={{ color: 'rgba(255,255,255,0.45)', margin: '0 0 14px', textTransform: 'capitalize', fontSize: '13px' }}>{result.employee}</p>
                    <div style={{ background: 'rgba(34,197,94,0.12)', border: '1px solid rgba(34,197,94,0.3)', borderRadius: '12px', padding: '14px', marginBottom: '14px' }}>
                        <div style={{ fontSize: '12px', color: 'rgba(255,255,255,0.4)', marginBottom: '3px' }}>Mesin · Shift</div>
                        <div style={{ fontSize: '15px', fontWeight: 800, marginBottom: '8px' }}>{result.machine} · {result.shift}</div>
                        <div style={{ fontSize: '11px', color: 'rgba(255,255,255,0.35)', marginBottom: '3px' }}>Bonus didapat</div>
                        <div style={{ fontSize: '30px', fontWeight: 900, color: '#4ade80' }}>
                            Rp {Number(result.bonus).toLocaleString('id-ID')}
                        </div>
                    </div>
                    <button onClick={reset} style={btn('linear-gradient(135deg,#3b82f6,#1d4ed8)', '0')}>
                        Klaim Bonus Lain
                    </button>
                </div>
            )}

            <p style={{ color: 'rgba(255,255,255,0.15)', fontSize: '10px', margin: 0, flexShrink: 0 }}>
                1 PIN · 1 klaim per hari
            </p>
        </div>
    );
};

export default BonusClaim;
