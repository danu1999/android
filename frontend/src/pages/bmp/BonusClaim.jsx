const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\BonusClaim.tsx"; function _optionalChain(ops) { let lastAccessLHS = undefined; let value = ops[0]; let i = 1; while (i < ops.length) { const op = ops[i]; const fn = ops[i + 1]; i += 2; if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) { return undefined; } if (op === 'access' || op === 'optionalAccess') { lastAccessLHS = value; value = fn(value); } else if (op === 'call' || op === 'optionalCall') { value = fn((...args) => value.call(lastAccessLHS, ...args)); lastAccessLHS = undefined; } } return value; }import React, { useState, useEffect } from 'react';
import api from '../../services/apiBmp';

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

const BonusClaim = () => {
    const [step, setStep] = useState('pin');
    const [view, setView] = useState('login'); // toggle
    const [pin, setPin] = useState('');
    const [employee, setEmployee] = useState(null);
    const [machine, setMachine] = useState('');
    const [shift, setShift] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [result, setResult] = useState(null);
    const [pinList, setPinList] = useState([]);
    const [jumlahPerolehan, setJumlah] = useState('');

    useEffect(() => {
        api.get('/bonus/pin-list').then(res => {
            if (_optionalChain([res, 'access', _ => _.data, 'optionalAccess', _2 => _2.data])) setPinList(res.data.data);
        }).catch(() => { });
    }, []);

    const handleVerifyPIN = async () => {
        if (!pin) return setError('Masukkan PIN Anda');
        setLoading(true); setError('');
        try {
            const res = await api.post('/bonus/verify-pin', { pin });
            setEmployee(res.data.data);
            setStep('form');
        } catch (err) {
            setError(_optionalChain([err, 'access', _3 => _3.response, 'optionalAccess', _4 => _4.data, 'optionalAccess', _5 => _5.message]) || 'PIN salah atau tidak terdaftar');
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
        } catch (err) {
            setError(_optionalChain([err, 'access', _6 => _6.response, 'optionalAccess', _7 => _7.data, 'optionalAccess', _8 => _8.message]) || 'Gagal mengklaim bonus');
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
    const page = {
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

    const card = {
        width: '100%',
        maxWidth: '440px',
        background: 'rgba(255,255,255,0.05)',
        border: '1px solid rgba(255,255,255,0.1)',
        borderRadius: '16px',
        padding: '16px 14px',
        color: 'white',
        flexShrink: 0,
    };

    const inputStyle = {
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

    const btn = (color, mt = '8px') => ({
        width: '100%', padding: '11px',
        background: color, border: 'none',
        borderRadius: '10px', color: 'white',
        fontSize: '14px', fontWeight: 'bold',
        cursor: 'pointer', marginTop: mt,
        opacity: loading ? 0.7 : 1,
    });

    const btnGhost = {
        width: '100%', padding: '9px',
        background: 'transparent',
        border: '1px solid rgba(255,255,255,0.15)',
        borderRadius: '10px', color: 'rgba(255,255,255,0.55)',
        fontSize: '13px', cursor: 'pointer', marginTop: '6px',
    };

    const errBox = {
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

    const pinCell = (even) => ({
        display: 'flex', justifyContent: 'space-between',
        alignItems: 'center', padding: '5px 8px',
        background: even ? 'rgba(255,255,255,0.03)' : 'transparent',
    });
    const pinBadge = {
        background: 'rgba(59,130,246,0.25)',
        border: '1px solid rgba(59,130,246,0.45)',
        borderRadius: '5px', padding: '1px 7px',
        fontWeight: 900, fontSize: '13px',
        color: '#93c5fd', fontFamily: 'monospace',
        letterSpacing: '1px', flexShrink: 0,
    };

    return (
        React.createElement('div', { style: page, __self: this, __source: {fileName: _jsxFileName, lineNumber: 162}}

            /* ── Header + Toggle Buttons ── */
            , React.createElement('div', { style: { width: '100%', maxWidth: '440px', flexShrink: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 165}}
                , React.createElement('p', { style: { color: 'rgba(255,255,255,0.5)', fontSize: '11px', margin: '0 0 6px', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 166}}, "🏭 Klaim Bonus Mesin · CV BMP"

                )
                /* Toggle hanya muncul saat step = 'pin' */
                , step === 'pin' && (
                    React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 171}}
                        , React.createElement('button', {
                            onClick: () => { setView('login'); setError(''); },
                            style: {
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
                            }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 172}}
, "🔐 Login PIN"

                        )
                        , React.createElement('button', {
                            onClick: () => { setView('pinlist'); setError(''); },
                            style: {
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
                            }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 190}}
, "🪪 Nama & PIN"

                        )
                    )
                )
            )

            /* ── VIEW: Login PIN ── */
            , step === 'pin' && view === 'login' && (
                React.createElement('div', { style: card, __self: this, __source: {fileName: _jsxFileName, lineNumber: 214}}
                    , error && React.createElement('div', { style: errBox, __self: this, __source: {fileName: _jsxFileName, lineNumber: 215}}, "⚠️ " , error)
                    , React.createElement('p', { style: { textAlign: 'center', color: 'rgba(255,255,255,0.55)', margin: '0 0 8px', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 216}}, "Masukkan PIN fingerprint Anda"

                    )
                    , React.createElement('input', {
                        type: "password", inputMode: "numeric",
                        placeholder: "PIN", value: pin,
                        onChange: e => setPin(e.target.value),
                        onKeyDown: e => e.key === 'Enter' && handleVerifyPIN(),
                        style: inputStyle, autoFocus: true, __self: this, __source: {fileName: _jsxFileName, lineNumber: 219}}
                    )
                    , React.createElement('button', { onClick: handleVerifyPIN, disabled: loading, style: btn('linear-gradient(135deg,#3b82f6,#1d4ed8)'), __self: this, __source: {fileName: _jsxFileName, lineNumber: 226}}
                        , loading ? 'Memverifikasi...' : '🔐 Verifikasi PIN'
                    )
                )
            )

            /* ── VIEW: Daftar PIN ── */
            , step === 'pin' && view === 'pinlist' && pinList.length > 0 && (
                React.createElement('div', { style: {
                    width: '100%', maxWidth: '440px',
                    background: 'rgba(255,255,255,0.05)',
                    border: '1px solid rgba(255,255,255,0.1)',
                    borderRadius: '14px', overflow: 'hidden', flexShrink: 0,
                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 234}}
                    , React.createElement('div', { style: { background: 'rgba(124,58,237,0.4)', padding: '6px 12px', fontSize: '11px', fontWeight: 800, color: 'white' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 240}}, "🪪 Daftar PIN Karyawan"

                    )
                    , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 243}}
                        /* Kolom kiri */
                        , React.createElement('div', { style: { borderRight: '1px solid rgba(255,255,255,0.07)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 245}}
                            , col1.map((row, i) => (
                                React.createElement('div', { key: i, style: pinCell(i % 2 === 0), __self: this, __source: {fileName: _jsxFileName, lineNumber: 247}}
                                    , React.createElement('span', { style: { color: 'rgba(255,255,255,0.8)', fontSize: '11px', fontWeight: 600, textTransform: 'capitalize', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginRight: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 248}}
                                        , row.name
                                    )
                                    , React.createElement('span', { style: pinBadge, __self: this, __source: {fileName: _jsxFileName, lineNumber: 251}}, row.pin)
                                )
                            ))
                        )
                        /* Kolom kanan */
                        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 256}}
                            , col2.map((row, i) => (
                                React.createElement('div', { key: i, style: pinCell(i % 2 === 0), __self: this, __source: {fileName: _jsxFileName, lineNumber: 258}}
                                    , React.createElement('span', { style: { color: 'rgba(255,255,255,0.8)', fontSize: '11px', fontWeight: 600, textTransform: 'capitalize', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginRight: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 259}}
                                        , row.name
                                    )
                                    , React.createElement('span', { style: pinBadge, __self: this, __source: {fileName: _jsxFileName, lineNumber: 262}}, row.pin)
                                )
                            ))
                        )
                    )
                )
            )

            /* ── STEP 2: Pilih Mesin & Shift ── */
            , step === 'form' && employee && (
                React.createElement('div', { style: { ...card, overflowY: 'auto', flexShrink: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 272}}
                    , error && React.createElement('div', { style: errBox, __self: this, __source: {fileName: _jsxFileName, lineNumber: 273}}, "⚠️ " , error)

                    , React.createElement('div', { style: { background: 'rgba(34,197,94,0.12)', border: '1px solid rgba(34,197,94,0.3)', borderRadius: '8px', padding: '8px 12px', marginBottom: '10px', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 275}}
                        , React.createElement('div', { style: { fontSize: '11px', color: 'rgba(255,255,255,0.45)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 276}}, "Terverifikasi ✓" )
                        , React.createElement('div', { style: { fontWeight: 800, fontSize: '16px', textTransform: 'capitalize' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 277}}, employee.name)
                    )

                    , React.createElement('p', { style: { fontWeight: 700, margin: '0 0 6px', fontSize: '12px', color: 'rgba(255,255,255,0.55)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 280}}, "Pilih Mesin:" )
                    , React.createElement('div', { style: { display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 281}}
                        , MACHINES.map(m => (
                            React.createElement('button', { key: m.name, onClick: () => setMachine(m.name), style: {
                                padding: '9px 12px', borderRadius: '9px',
                                border: machine === m.name ? `2px solid ${m.color}` : '2px solid rgba(255,255,255,0.1)',
                                background: machine === m.name ? `${m.color}22` : 'rgba(255,255,255,0.04)',
                                color: 'white', cursor: 'pointer',
                                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                fontSize: '13px', fontWeight: machine === m.name ? 800 : 500,
                            }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 283}}
                                , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 291}}, m.name)
                                , React.createElement('span', { style: { color: machine === m.name ? m.color : 'rgba(255,255,255,0.3)', fontWeight: 800, fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 292}}, "Rp "
                                     , m.bonus.toLocaleString('id-ID')
                                )
                            )
                        ))
                    )

                    /* Input Jumlah Perolehan — muncul setelah mesin dipilih */
                    , machine && (
                        React.createElement('div', { style: { marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 301}}
                            , React.createElement('p', { style: { fontWeight: 700, margin: '0 0 6px', fontSize: '12px', color: 'rgba(255,255,255,0.55)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 302}}, "Jumlah Perolehan "
                                  , React.createElement('span', { style: { color: 'rgba(255,255,255,0.3)', fontWeight: 400 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 303}}, "(tumpukan)"), ":"
                            )
                            , React.createElement('input', {
                                type: "number",
                                inputMode: "numeric",
                                min: "1",
                                placeholder: "Contoh: 50" ,
                                value: jumlahPerolehan,
                                onChange: e => setJumlah(e.target.value.replace(/\D/g, '')),
                                style: {
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
                                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 305}}
                            )
                        )
                    )

                    , React.createElement('p', { style: { fontWeight: 700, margin: '0 0 6px', fontSize: '12px', color: 'rgba(255,255,255,0.55)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 329}}, "Pilih Shift:" )
                    , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: '6px', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 330}}
                        , SHIFTS.map(s => (
                            React.createElement('button', { key: s, onClick: () => setShift(s), style: {
                                padding: '9px 4px', borderRadius: '9px',
                                border: shift === s ? '2px solid #f59e0b' : '2px solid rgba(255,255,255,0.1)',
                                background: shift === s ? 'rgba(245,158,11,0.15)' : 'rgba(255,255,255,0.04)',
                                color: shift === s ? '#fcd34d' : 'rgba(255,255,255,0.5)',
                                cursor: 'pointer', fontWeight: shift === s ? 800 : 500, fontSize: '12px',
                            }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 332}}
                                , s === 'Pagi' ? '☀️' : (s === 'Siang' || s === 'Sore') ? '🌤️' : '🌙', React.createElement('br', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 339}} ), s
                            )
                        ))
                    )

                    , React.createElement('button', { onClick: handleClaim, disabled: loading, style: btn('linear-gradient(135deg,#22c55e,#15803d)', '0'), __self: this, __source: {fileName: _jsxFileName, lineNumber: 344}}
                        , loading ? 'Menyimpan...' : '✅ Klaim Bonus Sekarang'
                    )
                    , React.createElement('button', { onClick: reset, style: btnGhost, __self: this, __source: {fileName: _jsxFileName, lineNumber: 347}}, "← Kembali" )
                )
            )

            /* ── STEP 3: Sukses ── */
            , step === 'success' && result && (
                React.createElement('div', { style: { ...card, textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 353}}
                    , React.createElement('div', { style: { fontSize: '52px', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 354}}, "🎉")
                    , React.createElement('h3', { style: { fontSize: '18px', fontWeight: 800, margin: '0 0 4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 355}}, "Bonus Diklaim!" )
                    , React.createElement('p', { style: { color: 'rgba(255,255,255,0.45)', margin: '0 0 14px', textTransform: 'capitalize', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 356}}, result.employee)
                    , React.createElement('div', { style: { background: 'rgba(34,197,94,0.12)', border: '1px solid rgba(34,197,94,0.3)', borderRadius: '12px', padding: '14px', marginBottom: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 357}}
                        , React.createElement('div', { style: { fontSize: '12px', color: 'rgba(255,255,255,0.4)', marginBottom: '3px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 358}}, "Mesin · Shift"  )
                        , React.createElement('div', { style: { fontSize: '15px', fontWeight: 800, marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 359}}, result.machine, " · "  , result.shift)
                        , React.createElement('div', { style: { fontSize: '11px', color: 'rgba(255,255,255,0.35)', marginBottom: '3px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 360}}, "Bonus didapat" )
                        , React.createElement('div', { style: { fontSize: '30px', fontWeight: 900, color: '#4ade80' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 361}}, "Rp "
                             , Number(result.bonus).toLocaleString('id-ID')
                        )
                    )
                    , React.createElement('button', { onClick: reset, style: btn('linear-gradient(135deg,#3b82f6,#1d4ed8)', '0'), __self: this, __source: {fileName: _jsxFileName, lineNumber: 365}}, "Klaim Bonus Lain"

                    )
                )
            )

            , React.createElement('p', { style: { color: 'rgba(255,255,255,0.15)', fontSize: '10px', margin: 0, flexShrink: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 371}}, "1 PIN · 1 klaim per hari"

            )
        )
    );
};

export default BonusClaim;
