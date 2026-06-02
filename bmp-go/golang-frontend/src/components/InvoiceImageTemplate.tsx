import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../contexts/AuthContext';

interface Product {
    ID: number;
    Title: string;
    Quantity: number;
    JumlahLusin: number;
    Price: number;
    Unit: string;
}

interface Payment {
    ID: number;
    PaymentDate: string;
    PaymentMethod: string;
    PaymentAmount: number;
}

interface Client {
    ID: number;
    ClientName: string;
    AddressLine1: string;
    Province?: string; // ✅ FIX: Mendaftarkan kolom Province agar dikenali React
}

interface InvoiceData {
    ID: number;
    Number: string;
    Status: string;
    DateCreated: string;
    DueDate: string;
    Client: Client;
    Products: Product[];
    Payments: Payment[];
}

interface InvoiceSettings {
    ClientName: string;
    AddressLine1: string;
    PhoneNumber: string;
    EmailAddress: string;
}

interface InvoiceImageTemplateProps {
    inv: InvoiceData | null;
    settings: InvoiceSettings | null;
}

const formatRp = (num: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(num);

export const InvoiceImageTemplate: React.FC<InvoiceImageTemplateProps> = ({ inv, settings }) => {
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
            const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            const payload = JSON.parse(jsonPayload);
            return payload.is_demo === true;
        } catch (e) {
            return false;
        }
    };

    const isDemo = checkIsDemo();

    const total = inv.Products?.reduce((acc: number, p: Product) => acc + (p.Quantity * p.JumlahLusin * p.Price), 0) || 0;
    const paid = inv.Payments?.reduce((acc: number, p: Payment) => acc + p.PaymentAmount, 0) || 0;
    const sisa = total - paid;

    const apiUrl = import.meta.env.VITE_API_URL || 'https://bmp.up.railway.app/api';

    const [logoBase64, setLogoBase64] = useState('');
    const [signatureBase64, setSignatureBase64] = useState('');

    useEffect(() => {
        fetch(`${apiUrl}/images/logo.jpg`)
            .then(r => r.ok ? r.json() : null)
            .then(json => json?.data && setLogoBase64(json.data))
            .catch(() => { });
        fetch(`${apiUrl}/images/signature.jpeg`)
            .then(r => r.ok ? r.json() : null)
            .then(json => json?.data && setSignatureBase64(json.data))
            .catch(() => { });
    }, [apiUrl]);

    // ✅ FIX: Logika penggabungan Alamat dan Provinsi yang cerdas
    const getFullAddress = () => {
        if (!inv.Client) return '-';
        const addressParts = [inv.Client.AddressLine1, inv.Client.Province].filter(val => val && val.trim() !== '');
        return addressParts.length > 0 ? addressParts.join(', ') : '-';
    };

    return (
        <div id={`faktur-canvas-${inv.ID}`} style={{
            width: '800px', backgroundColor: '#ffffff', padding: '30px',
            fontFamily: 'Arial, sans-serif', color: '#333',
            position: 'absolute', top: '-9999px', left: '-9999px', zIndex: -9999,
            border: '1px solid #eee', boxShadow: '0 0 10px rgba(0, 0, 0, 0.15)',
            boxSizing: 'border-box'
        }}>
            <div style={{ borderBottom: '2px solid #212529', paddingBottom: '15px', marginBottom: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                    {isDemo ? (
                        <div style={{ width: '80px', height: '80px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', fontSize: '20px', border: '1px dashed #ccc', boxSizing: 'border-box' }}>
                            Logo
                        </div>
                    ) : (
                        logoBase64 && <img src={logoBase64} alt="Logo BMP" style={{ maxHeight: '80px', objectFit: 'contain' }} />
                    )}
                    <div style={{ textAlign: 'left' }}>
                        <h2 style={{ margin: 0, marginTop: '-8px', color: '#0d6efd', fontWeight: 'bold', fontSize: '22px' }}>
                            {isDemo ? 'BMP - BINTANG MAKMUR PLASTINDO' : (settings?.ClientName || 'CV. BAHTERA MULYA PLASTIK')}
                        </h2>
                        <p style={{ margin: 0, color: '#6c757d', fontSize: '14px' }}>
                            {isDemo ? 'Jl. Industri Raya No. 45, Semarang' : (settings?.AddressLine1 || 'Jl. Arimbi, RT04 RW 01 Desa Ngrimbi')}
                        </p>
                        <p style={{ margin: '2px 0', fontSize: '13px' }}>
                            {isDemo ? '024-7654321' : (settings?.PhoneNumber || '0889-8608-4722')} &nbsp;|&nbsp; {isDemo ? 'info@bintangmakmurplastindo.com' : (settings?.EmailAddress || 'bahteramulyap@gmail.com')}
                        </p>
                    </div>
                </div>
                <div style={{ textAlign: 'right', position: 'relative' }}>
                    <h1 style={{ margin: 0, fontWeight: 'bold', color: '#212529', fontSize: '32px' }}>NOTA</h1>
                    <p style={{ margin: 0, fontSize: '16px', fontWeight: 'bold', color: '#6c757d' }}>{inv.Number}</p>
                    <p style={{ margin: '2px 0', fontSize: '16px', fontWeight: 'bold', color: '#6c757d' }}>
                        TGL: {new Date(inv.DateCreated).toLocaleDateString('id-ID', { day: '2-digit', month: 'long', year: 'numeric' })}
                    </p>
                </div>
            </div>

            <div style={{ marginBottom: '20px', fontWeight: 'bold', textTransform: 'uppercase', fontSize: '14px' }}>
                KEPADA: <br />
                {inv.Client ? (
                    <>
                        {inv.Client.ClientName}<br />
                        {/* ✅ FIX: Memanggil fungsi getFullAddress di sini */}
                        <span style={{ fontWeight: 'normal', fontSize: '12px', textTransform: 'capitalize' }}>
                            {getFullAddress()}
                        </span>
                    </>
                ) : (
                    <span style={{ color: 'red' }}>(Pelanggan Belum Dipilih)</span>
                )}
            </div>

            <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: '20px' }}>
                <thead>
                    <tr>
                        <th style={{ backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }}>#</th>
                        <th style={{ backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }}>ITEM</th>
                        <th style={{ backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }}>SATUAN</th>
                        <th style={{ backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }}>KUANTITAS</th>
                        <th style={{ backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }}>HARGA</th>
                        <th style={{ backgroundColor: '#3b5998', color: 'white', padding: '10px', textAlign: 'left', textTransform: 'uppercase', fontSize: '12px' }}>TOTAL</th>
                    </tr>
                </thead>
                <tbody>
                    {inv.Products?.map((item: Product, idx: number) => {
                        if (!item.Title) return null;
                        return (
                            <tr key={item.ID || idx}>
                                <td style={{ padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }}>{idx + 1}</td>
                                <td style={{ padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }}><strong>{item.Title}</strong></td>
                                <td style={{ padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }}>{item.Unit === '-' || !item.Unit || item.Unit.toLowerCase() === 'lusin' ? `${item.JumlahLusin} lusin` : item.Unit}</td>
                                <td style={{ padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }}>{item.Quantity}</td>
                                <td style={{ padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }}>{formatRp(item.Price).replace(',00', '')}</td>
                                <td style={{ padding: '10px', borderBottom: '1px solid #eee', fontSize: '13px' }}>{formatRp(item.Quantity * item.JumlahLusin * item.Price).replace(',00', '')}</td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>

            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <div style={{ float: 'left', width: '40%', fontSize: '12px', marginTop: '30px', borderTop: '1px solid #eee', paddingTop: '10px' }}>
                    {inv.Status !== 'PAID' && (
                        <>
                            <strong>INFO PEMBAYARAN</strong><br />
                            Pembayaran melalui rekening:<br />
                            An: {isDemo ? 'Zedmz' : 'Dedi Santoso'}<br />
                            BCA: {isDemo ? '1234567890 (Simulasi)' : '0184517724'} <br />BRI: {isDemo ? '0987654321 (Simulasi)' : '119801008053502'}
                        </>
                    )}
                </div>

                <div style={{ float: 'right', width: '55%', marginTop: '20px' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <tbody>
                            <tr>
                                <td style={{ textAlign: 'left', fontWeight: 'bold', padding: '3px', border: 'none', fontSize: '13px' }}>SUBTOTAL</td>
                                <td style={{ textAlign: 'right', padding: '3px', border: 'none', fontSize: '13px' }}>{formatRp(total).replace(',00', '')}</td>
                            </tr>
                            <tr style={{ borderBottom: '2px solid #333' }}>
                                <td style={{ textAlign: 'left', fontWeight: 'bold', padding: '3px', border: 'none', fontSize: '13px' }}>TOTAL TAGIHAN</td>
                                <td style={{ textAlign: 'right', padding: '3px', border: 'none', fontSize: '13px' }}><strong>{formatRp(total).replace(',00', '')}</strong></td>
                            </tr>

                            {sisa < total ? (
                                <>
                                    <tr>
                                        <td colSpan={2} style={{ paddingTop: '10px', fontSize: '11px', fontWeight: 'bold', color: '#3b5998', borderBottom: '1px solid #eee' }}>RIWAYAT PEMBAYARAN:</td>
                                    </tr>
                                    {inv.Payments?.map((pay: Payment, idx: number) => (
                                        <tr key={pay.ID || idx}>
                                            <td style={{ textAlign: 'left', color: '#17a2b8', fontStyle: 'italic', fontSize: '11px', padding: '3px', border: 'none' }}>
                                                - {new Date(pay.PaymentDate).toLocaleDateString('id-ID', { day: '2-digit', month: '2-digit', year: 'numeric' }).replace(/\./g, '/')} {pay.PaymentMethod ? `(${pay.PaymentMethod})` : ''}
                                            </td>
                                            <td style={{ textAlign: 'right', color: '#17a2b8', fontStyle: 'italic', padding: '3px', border: 'none', fontSize: '11px' }}>
                                                - {formatRp(pay.PaymentAmount).replace(',00', '')}
                                            </td>
                                        </tr>
                                    ))}
                                    <tr style={{ backgroundColor: '#fff5f5' }}>
                                        <td style={{ textAlign: 'left', fontWeight: 'bold', color: '#e74c3c', padding: '8px 5px', borderTop: '1px solid #e74c3c', fontSize: '13px' }}>SISA TERUTANG</td>
                                        <td style={{ textAlign: 'right', fontWeight: 'bold', color: '#e74c3c', padding: '8px 5px', borderTop: '1px solid #e74c3c', fontSize: '13px' }}>
                                            {formatRp(sisa).replace(',00', '')}
                                        </td>
                                    </tr>
                                </>
                            ) : inv.Status !== 'PAID' ? (
                                <tr>
                                    <td style={{ textAlign: 'left', fontWeight: 'bold', color: '#e74c3c', padding: '8px 5px', border: 'none', fontSize: '13px' }}>SALDO TERUTANG</td>
                                    <td style={{ textAlign: 'right', fontWeight: 'bold', color: '#e74c3c', padding: '8px 5px', border: 'none', fontSize: '13px' }}>
                                        {formatRp(total).replace(',00', '')}
                                    </td>
                                </tr>
                            ) : null}

                            <tr>
                                <td style={{ textAlign: 'left', fontWeight: 'bold', color: '#3b5998', padding: '15px 5px 5px 5px', verticalAlign: 'middle', border: 'none', fontSize: '13px' }}>STATUS</td>
                                <td style={{ textAlign: 'right', padding: '15px 5px 5px 5px', border: 'none' }}>
                                    {inv.Status === 'PAID' ? (
                                        <>
                                            <div style={{
                                                display: 'inline-block', color: '#198754', border: '4px solid #198754',
                                                borderRadius: '8px', padding: '5px 15px', fontSize: '20px', fontWeight: 900,
                                                textTransform: 'uppercase', letterSpacing: '2px', transform: 'rotate(-10deg)',
                                                opacity: 0.85, marginTop: '5px'
                                            }}>LUNAS</div>
                                            <div style={{ fontSize: '10px', color: '#198754', marginTop: '3px', fontWeight: 'bold' }}>
                                                Lunas Pada: {inv.Payments && inv.Payments.length > 0
                                                    ? new Date(inv.Payments[inv.Payments.length - 1].PaymentDate).toLocaleDateString('id-ID', { day: '2-digit', month: '2-digit', year: 'numeric' }).replace(/\./g, '/')
                                                    : (inv.DueDate ? new Date(inv.DueDate).toLocaleDateString('id-ID', { day: '2-digit', month: '2-digit', year: 'numeric' }).replace(/\./g, '/') : '-')}
                                            </div>
                                        </>
                                    ) : inv.Status === 'PARTIAL' ? (
                                        <div style={{ color: '#17a2b8', fontWeight: 'bold', fontSize: '12px', textTransform: 'uppercase', border: '1px solid #17a2b8', padding: '3px 8px', display: 'inline-block', borderRadius: '4px' }}>
                                            BELUM LUNAS (CICIL)
                                        </div>
                                    ) : (
                                        <div style={{ color: '#3b5998', fontWeight: 'bold', textTransform: 'uppercase', fontSize: '13px' }}>BELUM DIBAYAR</div>
                                    )}
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div style={{ clear: 'both' }}></div>

            <div style={{ float: 'right', textAlign: 'center', marginTop: '50px', width: '150px', fontSize: '13px' }}>
                <p style={{ margin: 0, fontWeight: 'bold', fontSize: '14px' }}>Hormat Kami,</p>
                <div style={{ height: '80px', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '5px 0' }}>
                    {isDemo ? (
                        <span style={{ fontStyle: 'italic', fontSize: '16px', color: '#555', fontWeight: 'bold', borderBottom: '1px solid #212529', display: 'inline-block', width: '100%', paddingBottom: '5px' }}>signature</span>
                    ) : (
                        signatureBase64 && <img src={signatureBase64} alt="TTD" style={{ maxHeight: '70px', borderBottom: '1px solid #212529' }} />
                    )}
                </div>
                {new Date(inv.DateCreated).toLocaleDateString('id-ID', { day: '2-digit', month: 'long', year: 'numeric' })}
            </div>
        </div>
    );
};