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
const SHIFTS = [
    { label: 'Pagi', icon: '☀️', time: '07:00 – 15:00' },
    { label: 'Sore', icon: '🌤️', time: '15:00 – 23:00' },
    { label: 'Malam', icon: '🌙', time: '23:00 – 07:00' },
];

const BonusClaim: React.FC = () => {
    const [step, setStep] = useState<'pin' | 'form' | 'success'>('pin');
    const [view, setView] = useState<'login' | 'pinlist'>('login');
    const [pin, setPin] = useState('');
    const [employee, setEmployee] = useState<{ id: number; name: string } | null>(null);
    const [machine, setMachine] = useState('');
    const [shift, setShift] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [result, setResult] = useState<{ employee: string; machine: string; shift: string; bonus: number; jumlah_perolehan: number } | null>(null);
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
        } catch (err: unknown) {
            const e = err as { response?: { data?: { message?: string } } };
            setError(e.response?.data?.message || 'PIN salah atau tidak terdaftar');
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
        } catch (err: unknown) {
            const e = err as { response?: { data?: { message?: string } } };
            setError(e.response?.data?.message || 'Gagal mengklaim bonus');
        } finally { setLoading(false); }
    };

    const reset = () => {
        setStep('pin'); setView('login');
        setPin(''); setEmployee(null);
        setMachine(''); setShift('');
        setError(''); setResult(null);
        setJumlah('');
    };

    /* ── PIN half-split ── */
    const half = Math.ceil(pinList.length / 2);
    const col1 = pinList.slice(0, half);
    const col2 = pinList.slice(half);

    /* ── Styles ── */
    const s = {
        page: {
            minHeight: '100dvh',
            background: 'linear-gradient(160deg, #0f172a 0%, #1a2744 50%, #0f172a 100%)',
            display: 'flex',
            flexDirection: 'column' as const,
            alignItems: 'center',
            padding: '16px 14px 24px',
            gap: '12px',
            fontFamily: "'Inter', 'Segoe UI', Arial, sans-serif",
        },
        header: {
            width: '100%',
            maxWidth: '460px',
            textAlign: 'center' as const,
            flexShrink: 0,
        },
        logo: {
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            background: 'rgba(59,130,246,0.12)',
            border: '1px solid rgba(59,130,246,0.25)',
            borderRadius: '100px',
            padding: '6px 16px',
            marginBottom: '8px',
        },
        card: {
            width: '100%',
            maxWidth: '460px',
            background: 'rgba(255,255,255,0.04)',
            border: '1px solid rgba(255,255,255,0.09)',
            borderRadius: '20px',
            padding: '20px 18px',
            color: 'white',
            backdropFilter: 'blur(12px)',
        },
        input: {
            width: '100%',
            padding: '14px',
            fontSize: '24px',
            fontWeight: 800,
            textAlign: 'center' as const,
            letterSpacing: '10px',
            background: 'rgba(255,255,255,0.06)',
            border: '2px solid rgba(255,255,255,0.12)',
            borderRadius: '12px',
            color: 'white',
            outline: 'none',
            boxSizing: 'border-box' as const,
            fontFamily: 'monospace',
        },
        btnPrimary: (color = 'linear-gradient(135deg,#3b82f6,#1d4ed8)') => ({
            width: '100%',
            padding: '13px',
            background: color,
            border: 'none',
            borderRadius: '12px',
            color: 'white',
            fontSize: '14px',
            fontWeight: 700,
            cursor: 'pointer',
            marginTop: '10px',
            letterSpacing: '0.3px',
            transition: 'opacity 0.15s',
            opacity: loading ? 0.7 : 1,
        }),
        btnGhost: {
            width: '100%',
            padding: '11px',
            background: 'transparent',
            border: '1px solid rgba(255,255,255,0.13)',
            borderRadius: '12px',
            color: 'rgba(255,255,255,0.5)',
            fontSize: '13px',
            cursor: 'pointer',
            marginTop: '8px',
        },
        err: {
            background: 'rgba(239,68,68,0.12)',
            border: '1px solid rgba(239,68,68,0.3)',
            borderRadius: '10px',
            padding: '9px 12px',
            marginBottom: '12px',
            fontSize: '13px',
            color: '#fca5a5',
            textAlign: 'center' as const,
        },
        sectionTitle: {
            fontWeight: 700,
            fontSize: '12px',
            color: 'rgba(255,255,255,0.45)',
            textTransform: 'uppercase' as const,
            letterSpacing: '1px',
            marginBottom: '8px',
        },
    };

    return (
        <div style={s.page}>
            {/* ── Header ── */}
            <div style={s.header}>
                <div style={s.logo}>
                    <span style={{ fontSize: '16px' }}>🏆</span>
                    <span style={{ fontSize: '13px', fontWeight: 700, color: '#93c5fd' }}>
                        BMP Bonus · CV BMP
                    </span>
                </div>
                <h1 style={{ fontSize: '22px', fontWeight: 900, margin: 0, letterSpacing: '-0.5px' }}>
                    Klaim Bonus Mesin
                </h1>
                <p style={{ fontSize: '12px', color: 'rgba(255,255,255,0.35)', margin: '4px 0 0' }}>
                    Masukkan PIN fingerprint untuk melanjutkan
                </p>
            </div>

            {/* ── Toggle: Login / Daftar PIN ── */}
            {step === 'pin' && (
                <div style={{ width: '100%', maxWidth: '460px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                    {[
                        { key: 'login', label: '🔐 Login PIN', grad: 'linear-gradient(135deg,#3b82f6,#1d4ed8)' },
                        { key: 'pinlist', label: '🪪 Nama & PIN', grad: 'linear-gradient(135deg,#7c3aed,#5b21b6)' },
                    ].map(({ key, label, grad }) => (
                        <button
                            key={key}
                            onClick={() => { setView(key as 'login' | 'pinlist'); setError(''); }}
                            style={{
                                padding: '12px',
                                borderRadius: '12px',
                                border: 'none',
                                background: view === key ? grad : 'rgba(255,255,255,0.06)',
                                color: 'white',
                                fontWeight: view === key ? 800 : 500,
                                fontSize: '13px',
                                cursor: 'pointer',
                                transition: 'all 0.2s',
                            }}
                        >{label}</button>
                    ))}
                </div>
            )}

            {/* ── STEP 1a: Login PIN ── */}
            {step === 'pin' && view === 'login' && (
                <div style={s.card}>
                    {error && <div style={s.err}>⚠️ {error}</div>}
                    <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.45)', marginBottom: '12px', fontSize: '13px' }}>
                        Masukkan PIN fingerprint Anda
                    </p>
                    <input
                        type="password"
                        inputMode="numeric"
                        placeholder="• • • •"
                        value={pin}
                        onChange={e => setPin(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleVerifyPIN()}
                        style={s.input}
                        autoFocus
                    />
                    <button onClick={handleVerifyPIN} disabled={loading} style={s.btnPrimary()}>
                        {loading ? '⏳ Memverifikasi...' : '🔐 Verifikasi PIN'}
                    </button>
                </div>
            )}

            {/* ── STEP 1b: Daftar PIN ── */}
            {step === 'pin' && view === 'pinlist' && pinList.length > 0 && (
                <div style={{ ...s.card, padding: 0, overflow: 'hidden' }}>
                    <div style={{
                        background: 'linear-gradient(135deg,rgba(124,58,237,0.5),rgba(91,33,182,0.5))',
                        padding: '10px 16px', fontSize: '12px', fontWeight: 800,
                        color: 'white', letterSpacing: '0.5px',
                    }}>
                        🪪 Daftar PIN Karyawan
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr' }}>
                        {[col1, col2].map((col, ci) => (
                            <div key={ci} style={{ borderRight: ci === 0 ? '1px solid rgba(255,255,255,0.06)' : 'none' }}>
                                {col.map((row, i) => (
                                    <div key={i} style={{
                                        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                        padding: '7px 12px',
                                        background: i % 2 === 0 ? 'rgba(255,255,255,0.025)' : 'transparent',
                                    }}>
                                        <span style={{
                                            color: 'rgba(255,255,255,0.75)', fontSize: '11px', fontWeight: 600,
                                            textTransform: 'capitalize', overflow: 'hidden',
                                            textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginRight: '6px',
                                        }}>{row.name}</span>
                                        <span style={{
                                            background: 'rgba(59,130,246,0.2)', border: '1px solid rgba(59,130,246,0.4)',
                                            borderRadius: '6px', padding: '2px 8px',
                                            fontWeight: 900, fontSize: '12px', color: '#93c5fd',
                                            fontFamily: 'monospace', letterSpacing: '1px', flexShrink: 0,
                                        }}>{row.pin}</span>
                                    </div>
                                ))}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* ── STEP 2: Form Klaim ── */}
            {step === 'form' && employee && (
                <div style={{ ...s.card, width: '100%', maxWidth: '460px' }}>
                    {error && <div style={s.err}>⚠️ {error}</div>}

                    {/* Nama karyawan terverifikasi */}
                    <div style={{
                        background: 'rgba(34,197,94,0.1)',
                        border: '1px solid rgba(34,197,94,0.25)',
                        borderRadius: '12px', padding: '10px 14px',
                        marginBottom: '16px', textAlign: 'center',
                    }}>
                        <div style={{ fontSize: '11px', color: 'rgba(255,255,255,0.4)', marginBottom: '2px' }}>
                            ✅ Terverifikasi
                        </div>
                        <div style={{ fontWeight: 900, fontSize: '18px', textTransform: 'capitalize' }}>
                            {employee.name}
                        </div>
                    </div>

                    {/* Pilih Mesin */}
                    <p style={s.sectionTitle}>🏭 Pilih Mesin</p>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '14px' }}>
                        {MACHINES.map(m => (
                            <button
                                key={m.name}
                                onClick={() => setMachine(m.name)}
                                style={{
                                    padding: '10px 14px', borderRadius: '11px',
                                    border: machine === m.name ? `2px solid ${m.color}` : '2px solid rgba(255,255,255,0.08)',
                                    background: machine === m.name ? `${m.color}1a` : 'rgba(255,255,255,0.03)',
                                    color: 'white', cursor: 'pointer',
                                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                    fontSize: '13px', fontWeight: machine === m.name ? 800 : 500,
                                    transition: 'all 0.15s',
                                }}
                            >
                                <span>{m.name}</span>
                                <span style={{
                                    color: machine === m.name ? m.color : 'rgba(255,255,255,0.25)',
                                    fontWeight: 800, fontSize: '12px',
                                }}>
                                    Rp {m.bonus.toLocaleString('id-ID')}
                                </span>
                            </button>
                        ))}
                    </div>

                    {/* Jumlah Perolehan — muncul setelah mesin dipilih */}
                    {machine && (
                        <div style={{ marginBottom: '14px' }}>
                            <p style={s.sectionTitle}>📦 Jumlah Perolehan <span style={{ color: 'rgba(255,255,255,0.25)', fontWeight: 400, textTransform: 'none' }}>(tumpukan)</span></p>
                            <input
                                type="number"
                                inputMode="numeric"
                                min="1"
                                placeholder="Contoh: 50"
                                value={jumlahPerolehan}
                                onChange={e => setJumlah(e.target.value.replace(/\D/g, ''))}
                                style={{
                                    width: '100%', padding: '13px 14px',
                                    fontSize: '20px', fontWeight: 800,
                                    textAlign: 'center',
                                    background: 'rgba(255,255,255,0.06)',
                                    border: '2px solid rgba(255,255,255,0.15)',
                                    borderRadius: '12px',
                                    color: 'white', outline: 'none',
                                    boxSizing: 'border-box',
                                }}
                            />
                        </div>
                    )}

                    {/* Pilih Shift */}
                    <p style={s.sectionTitle}>⏰ Pilih Shift</p>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: '8px', marginBottom: '14px' }}>
                        {SHIFTS.map(sh => (
                            <button
                                key={sh.label}
                                onClick={() => setShift(sh.label)}
                                style={{
                                    padding: '12px 4px', borderRadius: '12px',
                                    border: shift === sh.label ? '2px solid #f59e0b' : '2px solid rgba(255,255,255,0.08)',
                                    background: shift === sh.label ? 'rgba(245,158,11,0.12)' : 'rgba(255,255,255,0.03)',
                                    color: shift === sh.label ? '#fcd34d' : 'rgba(255,255,255,0.45)',
                                    cursor: 'pointer', fontWeight: shift === sh.label ? 800 : 500,
                                    fontSize: '12px', transition: 'all 0.15s', textAlign: 'center',
                                }}
                            >
                                <div style={{ fontSize: '20px', marginBottom: '4px' }}>{sh.icon}</div>
                                <div>{sh.label}</div>
                                <div style={{ fontSize: '10px', opacity: 0.6, marginTop: '2px' }}>{sh.time}</div>
                            </button>
                        ))}
                    </div>

                    <button onClick={handleClaim} disabled={loading} style={s.btnPrimary('linear-gradient(135deg,#22c55e,#15803d)')}>
                        {loading ? '⏳ Menyimpan...' : '✅ Klaim Bonus Sekarang'}
                    </button>
                    <button onClick={reset} style={s.btnGhost}>← Kembali</button>
                </div>
            )}

            {/* ── STEP 3: Sukses ── */}
            {step === 'success' && result && (
                <div style={{ ...s.card, textAlign: 'center', maxWidth: '460px', width: '100%' }}>
                    <div style={{ fontSize: '64px', marginBottom: '12px', lineHeight: 1 }}>🎉</div>
                    <h2 style={{ fontSize: '22px', fontWeight: 900, margin: '0 0 4px' }}>Bonus Diklaim!</h2>
                    <p style={{ color: 'rgba(255,255,255,0.4)', margin: '0 0 18px', textTransform: 'capitalize', fontSize: '14px' }}>
                        {result.employee}
                    </p>

                    <div style={{
                        background: 'rgba(34,197,94,0.1)',
                        border: '1px solid rgba(34,197,94,0.25)',
                        borderRadius: '16px', padding: '18px', marginBottom: '18px',
                    }}>
                        <div style={{ fontSize: '12px', color: 'rgba(255,255,255,0.35)', marginBottom: '4px' }}>
                            Mesin · Shift · Perolehan
                        </div>
                        <div style={{ fontSize: '16px', fontWeight: 800, marginBottom: '12px' }}>
                            {result.machine} · {result.shift} · {result.jumlah_perolehan} tumpuk
                        </div>
                        <div style={{ fontSize: '11px', color: 'rgba(255,255,255,0.3)', marginBottom: '4px' }}>
                            Total Bonus
                        </div>
                        <div style={{ fontSize: '36px', fontWeight: 900, color: '#4ade80', letterSpacing: '-1px' }}>
                            Rp {Number(result.bonus).toLocaleString('id-ID')}
                        </div>
                    </div>

                    <button onClick={reset} style={s.btnPrimary()}>
                        🔄 Klaim Bonus Lain
                    </button>
                </div>
            )}

            <p style={{ color: 'rgba(255,255,255,0.12)', fontSize: '10px', margin: 0, textAlign: 'center' }}>
                CV BMP · 1 PIN · 1 klaim per hari per mesin
            </p>
        </div>
    );
};

export default BonusClaim;
