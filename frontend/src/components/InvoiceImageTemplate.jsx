import { API_URL } from '../services/apiBmp';
const _jsxFileName = "temp_comp\\InvoiceImageTemplate.tsx"; function _optionalChain(ops) { let lastAccessLHS = undefined; let value = ops[0]; let i = 1; while (i < ops.length) { const op = ops[i]; const fn = ops[i + 1]; i += 2; if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) { return undefined; } if (op === 'access' || op === 'optionalAccess') { lastAccessLHS = value; value = fn(value); } else if (op === 'call' || op === 'optionalCall') { value = fn((...args) => value.call(lastAccessLHS, ...args)); lastAccessLHS = undefined; } } return value; } import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../contexts/BmpAuthContext';




const formatRp = (num) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(num);

export const InvoiceImageTemplate = ({ inv, settings }) => {
    if (!inv) return null;

    const { token } = useContext(AuthContext);

    // Decode token to see if is_demo claim is true
    const checkIsDemo = () => {
        if (import.meta.env.VITE_DEMO_MODE === 'true') {
            return true;
        }
        if (!token) return false;
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function (c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            const payload = JSON.parse(jsonPayload);
            return payload.is_demo === true;
        } catch (e) {
            return false;
        }
    };

    const isDemo = checkIsDemo();

    const total = _optionalChain([inv, 'access', _ => _.Products, 'optionalAccess', _2 => _2.reduce, 'call', _3 => _3((acc, p) => acc + (p.Quantity * p.JumlahLusin * p.Price), 0)]) || 0;
    const paid = _optionalChain([inv, 'access', _4 => _4.Payments, 'optionalAccess', _5 => _5.reduce, 'call', _6 => _6((acc, p) => acc + p.PaymentAmount, 0)]) || 0;
    const sisa = total - paid;

    const apiUrl = API_URL;

    const [logoBase64, setLogoBase64] = useState('');
    const [signatureBase64, setSignatureBase64] = useState('');

    useEffect(() => {
        fetch(`${apiUrl}/images/logo.jpg`)
            .then(r => r.ok ? r.json() : null)
            .then(json => _optionalChain([json, 'optionalAccess', _7 => _7.data]) && setLogoBase64(json.data))
            .catch(() => { });
        fetch(`${apiUrl}/images/signature.jpeg`)
            .then(r => r.ok ? r.json() : null)
            .then(json => _optionalChain([json, 'optionalAccess', _8 => _8.data]) && setSignatureBase64(json.data))
            .catch(() => { });
    }, [apiUrl]);

    // ✅ FIX: Logika penggabungan Alamat dan Provinsi yang cerdas
    const getFullAddress = () => {
        if (!inv.Client) return '-';
        const addressParts = [inv.Client.AddressLine1, inv.Client.Province].filter(val => val && val.trim() !== '');
        return addressParts.length > 0 ? addressParts.join(', ') : '-';
    };

    return (
        React.createElement('div', {
            id: `faktur-canvas-${inv.ID}`, style: {
                width: '800px', backgroundColor: '#ffffff', padding: '30px',
                fontFamily: 'Arial, sans-serif', color: '#333',
                position: 'absolute', top: '-9999px', left: '-9999px', zIndex: -9999,
                border: '1px solid #eee', boxShadow: '0 0 10px rgba(0, 0, 0, 0.15)',
                boxSizing: 'border-box'
            }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 106 }
        }
            , React.createElement('div', { style: { borderBottom: '2px solid #212529', paddingBottom: '15px', marginBottom: '20px', display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 113 } }
                , React.createElement('div', { style: { display: 'flex', alignItems: 'flex-start', gap: '30px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 114 } }
                    , isDemo ? (
                        React.createElement('div', { style: { width: '180px', height: '100px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', fontSize: '20px', border: '1px dashed #ccc', boxSizing: 'border-box' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 116 } }, "Logo"

                        )
                    ) : (
                        logoBase64 && React.createElement('img', {
                            src: logoBase64, alt: "Logo BMP", style: { maxHeight: '170px', maxWidth: '270px', width: 'auto', height: 'auto', objectFit: 'contain', display: 'block' }
                            , __self: this, __source: { fileName: _jsxFileName, lineNumber: 120 }
                        })
                    )
                    , React.createElement('div', { style: { textAlign: 'left', paddingTop: '30px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 122 } }
                        , React.createElement('h2', { style: { margin: 0, color: '#0d6efd', fontWeight: 'bold', fontSize: '22px', lineHeight: 1 }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 123 } }
                            , isDemo ? 'BMP - BINTANG MAKMUR PLASTINDO' : (_optionalChain([settings, 'optionalAccess', _9 => _9.ClientName]) || 'CV. BAHTERA MULYA PLASTIK')
                        )
                        , React.createElement('p', { style: { margin: 0, color: '#6c757d', fontSize: '14px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 126 } }
                            , isDemo ? 'Jl. Industri Raya No. 45, Semarang' : (_optionalChain([settings, 'optionalAccess', _10 => _10.AddressLine1]) || 'Jl. Arimbi, RT04 RW 01 Desa Ngrimbi')
                        )
                        , React.createElement('p', { style: { margin: '2px 0', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 129 } }
                            , isDemo ? '024-7654321' : (_optionalChain([settings, 'optionalAccess', _11 => _11.PhoneNumber]) || '0889-8608-4722'), "  |   ", isDemo ? 'info@bintangmakmurplastindo.com' : (_optionalChain([settings, 'optionalAccess', _12 => _12.EmailAddress]) || 'bahteramulyap@gmail.com')
                        )
                    )
                )
                , React.createElement('div', { style: { textAlign: 'right', position: 'relative', paddingTop: '30px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 134 } }
                    , React.createElement('h1', { style: { margin: 0, fontWeight: 'bold', color: '#212529', fontSize: '18px', letterSpacing: '2px', lineHeight: 1 }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 135 } }, "NOTA")
                    , React.createElement('p', { style: { margin: 0, fontSize: '14px', fontWeight: 'bold', color: '#6c757d' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 136 } }, inv.Number)
                    , React.createElement('p', { style: { margin: '2px 0', fontSize: '14px', fontWeight: 'bold', color: '#6c757d' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 137 } }, "TGL: "
                        , new Date(inv.DateCreated).toLocaleDateString('id-ID', { day: '2-digit', month: 'long', year: 'numeric' })
                    )
                    , React.createElement('p', { style: { margin: '2px 0', fontSize: '16px', fontWeight: 'bold', color: '#6c757d' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 137 } }, "Jatuh Tempo: "
                        , inv.DueDate ? new Date(inv.DueDate).toLocaleDateString('id-ID', { day: '2-digit', month: '2-digit', year: 'numeric' }).replace(/\./g, '/') : '-'
                    )
                )
            )

            , React.createElement('div', { style: { marginBottom: '20px', fontWeight: 'bold', textTransform: 'uppercase', fontSize: '14px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 143 } }, "KEPADA: "
                , React.createElement('br', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 144 } })
                , inv.Client ? (
                    React.createElement(React.Fragment, null
                        , inv.Client.ClientName, React.createElement('br', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 147 } })
                        /* ✅ FIX: Memanggil fungsi getFullAddress di sini */
                        , React.createElement('span', { style: { fontWeight: 'normal', fontSize: '12px', textTransform: 'capitalize' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 149 } }
                            , getFullAddress()
                        )
                    )
                ) : (
                    React.createElement('span', { style: { color: 'red' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 154 } }, "(Pelanggan Belum Dipilih)")
                )
            )

            , React.createElement('table', { style: { width: '100%', borderCollapse: 'collapse', marginBottom: '20px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 158 } }
                , React.createElement('thead', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 159 } }
                    , React.createElement('tr', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 160 } }
                        , React.createElement('th', { style: { backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 161 } }, "#")
                        , React.createElement('th', { style: { backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 162 } }, "ITEM")
                        , React.createElement('th', { style: { backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 163 } }, "SATUAN")
                        , React.createElement('th', { style: { backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 164 } }, "KUANTITAS")
                        , React.createElement('th', { style: { backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 165 } }, "HARGA")
                        , React.createElement('th', { style: { backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 166 } }, "TOTAL")
                    )
                )
                , React.createElement('tbody', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 169 } }
                    , _optionalChain([inv, 'access', _13 => _13.Products, 'optionalAccess', _14 => _14.map, 'call', _15 => _15((item, idx) => {
                        if (!item.Title) return null;
                        return (
                            React.createElement('tr', { key: item.ID || idx, __self: this, __source: { fileName: _jsxFileName, lineNumber: 173 } }
                                , React.createElement('td', { style: { padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 174 } }, idx + 1)
                                , React.createElement('td', { style: { padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 175 } }, React.createElement('strong', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 175 } }, item.Title))
                                , React.createElement('td', { style: { padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 176 } }, item.Unit === '-' || !item.Unit || item.Unit.toLowerCase() === 'lusin' ? `${item.JumlahLusin} lusin` : item.Unit)
                                , React.createElement('td', { style: { padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 177 } }, item.Quantity)
                                , React.createElement('td', { style: { padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 178 } }, formatRp(item.Price).replace(',00', ''))
                                , React.createElement('td', { style: { padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 179 } }, formatRp(item.Quantity * item.JumlahLusin * item.Price).replace(',00', ''))
                            )
                        );
                    })])
                )
            )

            , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 186 } }
                , React.createElement('div', { style: { float: 'left', width: '40%', fontSize: '12px', marginTop: '30px', borderTop: '1px solid #eee', paddingTop: '10px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 187 } }
                    , inv.Status !== 'PAID' && (
                        React.createElement(React.Fragment, null
                            , React.createElement('strong', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 190 } }, "INFO PEMBAYARAN"), React.createElement('br', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 190 } }), "Pembayaran melalui rekening:"
                            , React.createElement('br', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 191 } }), "An: "
                            , isDemo ? 'Zedmz' : 'Dedi Santoso', React.createElement('br', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 192 } }), "BCA: "
                            , isDemo ? '1234567890 (Simulasi)' : '0184517724', " ", React.createElement('br', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 193 } }), "BRI: ", isDemo ? '0987654321 (Simulasi)' : '119801008053502'
                        )
                    )
                )

                , React.createElement('div', { style: { float: 'right', width: '55%', marginTop: '20px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 198 } }
                    , React.createElement('table', { style: { width: '100%', borderCollapse: 'collapse' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 199 } }
                        , React.createElement('tbody', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 200 } }
                            , React.createElement('tr', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 201 } }
                                , React.createElement('td', { style: { textAlign: 'left', fontWeight: 'bold', padding: '3px', border: 'none', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 202 } }, "SUBTOTAL")
                                , React.createElement('td', { style: { textAlign: 'right', padding: '3px', border: 'none', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 203 } }, formatRp(total).replace(',00', ''))
                            )
                            , React.createElement('tr', { style: { borderBottom: '2px solid #333' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 205 } }
                                , React.createElement('td', { style: { textAlign: 'left', fontWeight: 'bold', padding: '3px', border: 'none', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 206 } }, "TOTAL TAGIHAN")
                                , React.createElement('td', { style: { textAlign: 'right', padding: '3px', border: 'none', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 207 } }, React.createElement('strong', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 207 } }, formatRp(total).replace(',00', '')))
                            )

                            , sisa < total ? (
                                React.createElement(React.Fragment, null
                                    , React.createElement('tr', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 212 } }
                                        , React.createElement('td', { colSpan: 2, style: { paddingTop: '10px', fontSize: '11px', fontWeight: 'bold', color: '#3b5998', borderBottom: '1px solid #eee' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 213 } }, "RIWAYAT PEMBAYARAN:")
                                    )
                                    , _optionalChain([inv, 'access', _16 => _16.Payments, 'optionalAccess', _17 => _17.map, 'call', _18 => _18((pay, idx) => (
                                        React.createElement('tr', { key: pay.ID || idx, __self: this, __source: { fileName: _jsxFileName, lineNumber: 216 } }
                                            , React.createElement('td', { style: { textAlign: 'left', color: '#17a2b8', fontStyle: 'italic', fontSize: '11px', padding: '3px', border: 'none' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 217 } }, "- "
                                                , new Date(pay.PaymentDate).toLocaleDateString('id-ID', { day: '2-digit', month: '2-digit', year: 'numeric' }).replace(/\./g, '/'), " ", pay.PaymentMethod ? `(${pay.PaymentMethod})` : ''
                                            )
                                            , React.createElement('td', { style: { textAlign: 'right', color: '#17a2b8', fontStyle: 'italic', padding: '3px', border: 'none', fontSize: '11px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 220 } }, "- "
                                                , formatRp(pay.PaymentAmount).replace(',00', '')
                                            )
                                        )
                                    ))])
                                    , React.createElement('tr', { style: { backgroundColor: '#fff5f5' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 225 } }
                                        , React.createElement('td', { style: { textAlign: 'left', fontWeight: 'bold', color: '#e74c3c', padding: '8px 5px', borderTop: '1px solid #e74c3c', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 226 } }, "SISA TERUTANG")
                                        , React.createElement('td', { style: { textAlign: 'right', fontWeight: 'bold', color: '#e74c3c', padding: '8px 5px', borderTop: '1px solid #e74c3c', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 227 } }
                                            , formatRp(sisa).replace(',00', '')
                                        )
                                    )
                                )
                            ) : inv.Status !== 'PAID' ? (
                                React.createElement('tr', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 233 } }
                                    , React.createElement('td', { style: { textAlign: 'left', fontWeight: 'bold', color: '#e74c3c', padding: '8px 5px', border: 'none', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 234 } }, "SALDO TERUTANG")
                                    , React.createElement('td', { style: { textAlign: 'right', fontWeight: 'bold', color: '#e74c3c', padding: '8px 5px', border: 'none', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 235 } }
                                        , formatRp(total).replace(',00', '')
                                    )
                                )
                            ) : null

                            , React.createElement('tr', { __self: this, __source: { fileName: _jsxFileName, lineNumber: 241 } }
                                , React.createElement('td', { style: { textAlign: 'left', fontWeight: 'bold', color: '#3b5998', padding: '15px 5px 5px 5px', verticalAlign: 'middle', border: 'none', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 242 } }, "STATUS")
                                , React.createElement('td', { style: { textAlign: 'right', padding: '15px 5px 5px 5px', border: 'none' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 243 } }
                                    , inv.Status === 'PAID' ? (
                                        React.createElement(React.Fragment, null
                                            , React.createElement('div', {
                                                style: {
                                                    display: 'inline-block', color: '#198754', border: '4px solid #198754',
                                                    borderRadius: '8px', padding: '5px 15px', fontSize: '20px', fontWeight: 900,
                                                    textTransform: 'uppercase', letterSpacing: '2px', transform: 'rotate(-10deg)',
                                                    opacity: 0.85, marginTop: '5px'
                                                }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 246 }
                                            }, "LUNAS")
                                            , React.createElement('div', { style: { fontSize: '10px', color: '#198754', marginTop: '3px', fontWeight: 'bold' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 252 } }, "Lunas Pada: "
                                                , inv.Payments && inv.Payments.length > 0
                                                    ? new Date(inv.Payments[inv.Payments.length - 1].PaymentDate).toLocaleDateString('id-ID', { day: '2-digit', month: '2-digit', year: 'numeric' }).replace(/\./g, '/')
                                                    : (inv.DueDate ? new Date(inv.DueDate).toLocaleDateString('id-ID', { day: '2-digit', month: '2-digit', year: 'numeric' }).replace(/\./g, '/') : '-')
                                            )
                                        )
                                    ) : inv.Status === 'PARTIAL' ? (
                                        React.createElement('div', { style: { color: '#17a2b8', fontWeight: 'bold', fontSize: '12px', textTransform: 'uppercase', border: '1px solid #17a2b8', padding: '3px 8px', display: 'inline-block', borderRadius: '4px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 259 } }, "BELUM LUNAS (CICIL)"

                                        )
                                    ) : (
                                        React.createElement('div', { style: { color: '#3b5998', fontWeight: 'bold', textTransform: 'uppercase', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 263 } }, "BELUM DIBAYAR")
                                    )
                                )
                            )
                        )
                    )
                )
            )

            , React.createElement('div', { style: { clear: 'both' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 272 } })

            , React.createElement('div', { style: { float: 'right', textAlign: 'center', marginTop: '50px', width: '150px', fontSize: '13px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 274 } }
                , React.createElement('p', { style: { margin: 0, fontWeight: 'bold', fontSize: '14px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 275 } }, "Hormat Kami,")
                , React.createElement('div', { style: { height: '80px', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '5px 0' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 276 } }
                    , isDemo ? (
                        React.createElement('span', { style: { fontStyle: 'italic', fontSize: '16px', color: '#555', fontWeight: 'bold', borderBottom: '1px solid #212529', display: 'inline-block', width: '100%', paddingBottom: '5px' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 278 } }, "signature")
                    ) : (
                        signatureBase64 && React.createElement('img', { src: signatureBase64, alt: "TTD", style: { maxHeight: '70px', borderBottom: '1px solid #212529' }, __self: this, __source: { fileName: _jsxFileName, lineNumber: 280 } })
                    )
                )
                , new Date(inv.DateCreated).toLocaleDateString('id-ID', { day: '2-digit', month: 'long', year: 'numeric' })
            )
        )
    );
};