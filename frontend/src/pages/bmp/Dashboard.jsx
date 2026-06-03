const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\Dashboard.tsx";import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/apiBmp';
import { Zap, Users, Package, DollarSign, Clock, CheckCircle, ArrowRight, ShieldAlert, BookOpen, PlusCircle, Download } from 'lucide-react';






















const formatRp = (num) => new Intl.NumberFormat('id-ID').format(num);

const Dashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const [latestVer, setLatestVer] = useState("1.1.0");
  const navigate = useNavigate();

  const isCapacitor = (!!window.Capacitor && window.Capacitor.getPlatform && window.Capacitor.getPlatform() !== 'web') || window.location.protocol === 'capacitor:';

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    
    api.get('/dashboard').then((res) => {
      setData(res.data.data);
      setLoading(false);
    }).catch((err) => {
      console.error(err);
      setLoading(false);
    });

    const fetchVer = async () => {
      try {
        const base = isCapacitor ? 'https://www.zedmz.cloud' : '';
        const res = await fetch(`${base}/api/apk-version`);
        const val = await res.json();
        if (val && val.version) {
          setLatestVer(val.version);
        }
      } catch (err) {
        console.warn("Gagal memproses versi APK di BMP Dashboard:", err);
      }
    };
    fetchVer();

    return () => window.removeEventListener('resize', handleResize);
  }, []);

  if (loading) return (
    React.createElement('div', { style: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 51}}
      , React.createElement('div', { style: { width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 52}})
      , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 53}}, `@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`)
      , React.createElement('div', { style: { color: '#6c757d', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 54}}, "Memuat dashboard..." )
    )
  );

  if (!data) return React.createElement('div', { style: { padding: '20px', textAlign: 'center', color: '#dc3545' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 58}}, "Gagal memuat data."  );

  return (
    React.createElement('div', { style: { 
      padding: isMobile ? '12px' : '20px', 
      background: '#f8fafc', 
      minHeight: '100%',
      maxWidth: '100vw',
      boxSizing: 'border-box',
      overflowX: 'hidden'
    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 61}}
      , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 69}}, `
        .card-premium {
          background: #ffffff;
          border-radius: 16px;
          border: 1px solid #e2e8f0;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02), 0 1px 2px rgba(0, 0, 0, 0.04);
          transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
        }
        .card-premium:hover {
          transform: translateY(-2px);
          box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.04), 0 4px 6px -2px rgba(0, 0, 0, 0.04) !important;
          border-color: #cbd5e1 !important;
        }
        .action-btn {
          transition: all 0.2s ease;
        }
        .action-btn:hover {
          transform: translateY(-1px);
          filter: brightness(1.05);
        }
        .financial-grid {
          display: grid;
          grid-template-columns: 1.2fr 1fr;
          gap: 16px;
          margin-bottom: 16px;
        }
        .stats-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 12px;
          margin-bottom: 16px;
        }
        .detail-grid {
          display: grid;
          grid-template-columns: 1fr 1.2fr;
          gap: 16px;
          margin-bottom: 16px;
        }
        .quick-actions {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 10px;
          margin-top: 10px;
        }
        @media (max-width: 992px) {
          .financial-grid {
            grid-template-columns: 1fr;
          }
          .detail-grid {
            grid-template-columns: 1fr;
          }
        }
        @media (max-width: 768px) {
          .stats-grid {
            grid-template-columns: repeat(2, 1fr);
          }
          .quick-actions {
            grid-template-columns: 1fr;
            gap: 8px;
          }
        }
      `)

      /* Header & Quick Toolbar Section */
      , React.createElement('div', { style: { display: 'flex', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 133}}
        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 134}}
          , React.createElement('h1', { style: { fontSize: isMobile ? '20px' : '24px', margin: 0, fontWeight: '800', color: '#0f172a' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 135}}, "Panel Utama" )
          , React.createElement('p', { style: { margin: '2px 0 0', color: '#64748b', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 136}}, "Dashboard Ringkasan Operasional & Keuangan"    )
        )

        /* Quick Toolbar */
        , React.createElement('div', { style: { display: 'flex', gap: '8px', flexWrap: 'wrap', width: isMobile ? '100%' : 'auto' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 140}}
          , React.createElement('button', { 
            onClick: () => navigate('/invoices/create'), 
            className: "action-btn",
            style: { 
              background: 'linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%)', 
              color: 'white', border: 'none', padding: '10px 16px', borderRadius: '10px', 
              fontWeight: '700', cursor: 'pointer', fontSize: '13px',
              display: 'flex', alignItems: 'center', gap: '6px', justifyContent: 'center', flex: isMobile ? 1 : 'none'
            }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 141}}

            , React.createElement(PlusCircle, { size: 15, __self: this, __source: {fileName: _jsxFileName, lineNumber: 151}} ), " BUAT FAKTUR"
          )
          , React.createElement('button', { 
            onClick: () => navigate('/kas'), 
            className: "action-btn",
            style: { 
              background: '#ffffff', border: '1px solid #cbd5e1', color: '#334155', padding: '10px 16px', borderRadius: '10px', 
              fontWeight: '700', cursor: 'pointer', fontSize: '13px',
              display: 'flex', alignItems: 'center', gap: '6px', justifyContent: 'center', flex: isMobile ? 1 : 'none'
            }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 153}}

            , React.createElement(DollarSign, { size: 15, __self: this, __source: {fileName: _jsxFileName, lineNumber: 162}} ), " KAS KEUANGAN"
          )
        )
      )



      /* Financial Summary Section */
      , React.createElement('div', { className: "financial-grid", __self: this, __source: {fileName: _jsxFileName, lineNumber: 168}}

        /* Real Cash Card */
        , React.createElement('div', { style: { 
          background: 'linear-gradient(135deg, #10b981 0%, #047857 100%)', 
          color: 'white', borderRadius: '18px', padding: '18px', 
          boxShadow: '0 4px 15px rgba(16, 185, 129, 0.1)', position: 'relative', overflow: 'hidden',
          display: 'flex', flexDirection: 'column', justifyContent: 'space-between'
        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 171}}
          , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 177}}
            , React.createElement('div', { style: { textTransform: 'uppercase', fontWeight: '700', opacity: 0.85, display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', letterSpacing: '0.5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 178}}
              , React.createElement(DollarSign, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 179}} ), " SALDO KAS RIIL"
            )
            , React.createElement('h2', { style: { fontSize: isMobile ? '26px' : '32px', fontWeight: '800', margin: '6px 0 12px 0', letterSpacing: '-0.5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 181}}, "Rp "
               , formatRp(data.saldo_kas)
            )
          )

          , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', borderTop: '1px solid rgba(255,255,255,0.18)', paddingTop: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 186}}
            , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 187}}
              , React.createElement('small', { style: { opacity: 0.75, display: 'block', fontSize: '9px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 188}}, "TOTAL MASUK" )
              , React.createElement('span', { style: { fontWeight: '800', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 189}}, "Rp " , formatRp(data.total_kas_in))
            )
            , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 191}}
              , React.createElement('small', { style: { opacity: 0.75, display: 'block', fontSize: '9px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 192}}, "TOTAL KELUAR (+NONO)"  )
              , React.createElement('span', { style: { fontWeight: '800', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 193}}, "Rp " , formatRp(data.total_kas_out + data.nono_total_bayar))
            )
          )
        )

        /* Clean Estimation Card */
        , React.createElement('div', { style: { 
          background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)', 
          color: 'white', borderRadius: '18px', padding: '18px', 
          boxShadow: '0 4px 15px rgba(15, 23, 42, 0.1)', position: 'relative', overflow: 'hidden',
          display: 'flex', flexDirection: 'column', justifyContent: 'space-between'
        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 199}}
          , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 205}}
            , React.createElement('div', { style: { textTransform: 'uppercase', fontWeight: '700', color: '#f59e0b', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', letterSpacing: '0.5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 206}}
              , React.createElement(Zap, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 207}} ), " ESTIMASI BERSIH AKHIR"
            )
            , React.createElement('h2', { style: { fontSize: isMobile ? '26px' : '32px', fontWeight: '800', color: '#f59e0b', margin: '6px 0 10px 0', letterSpacing: '-0.5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 209}}, "Rp "
               , formatRp(data.simulasi_saldo)
            )
          )
          , React.createElement('div', { style: { borderTop: '1px solid rgba(255,255,255,0.12)', paddingTop: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 213}}
            , React.createElement('p', { style: { fontSize: '11px', opacity: 0.75, margin: 0, lineHeight: '1.4' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 214}}, "Estimasi sisa kas riil aman setelah dikurangi sisa hutang Nono serta tagihan berjalan selesai dilunasi."

            )
          )
        )
      )

      /* Mini Stats 4-Column Row (Desktop) / 2x2 Grid (Mobile) */
      , React.createElement('div', { className: "stats-grid", __self: this, __source: {fileName: _jsxFileName, lineNumber: 222}}

        /* Combined Clients & Products Stats Box */
        , React.createElement('div', { className: "card-premium", style: { padding: '14px 16px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 225}}
          , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 226}}
            , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 227}}
              , React.createElement('div', { style: { color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 228}}, "Pelanggan")
              , React.createElement('div', { style: { fontSize: '16px', fontWeight: '800', color: '#1e293b', marginTop: '2px', display: 'flex', alignItems: 'center', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 229}}
                , React.createElement(Users, { size: 14, color: "#0d6efd", __self: this, __source: {fileName: _jsxFileName, lineNumber: 230}} ), " " , data.total_clients
              )
            )
            , React.createElement('div', { style: { borderLeft: '1px solid #cbd5e1', height: '30px', margin: '0 8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 233}})
            , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 234}}
              , React.createElement('div', { style: { color: '#64748b', fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 235}}, "Produk")
              , React.createElement('div', { style: { fontSize: '16px', fontWeight: '800', color: '#1e293b', marginTop: '2px', display: 'flex', alignItems: 'center', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 236}}
                , React.createElement(Package, { size: 14, color: "#10b981", __self: this, __source: {fileName: _jsxFileName, lineNumber: 237}} ), " " , data.total_products
              )
            )
          )
        )

        /* Unpaid Invoices */
        , React.createElement('div', { className: "card-premium", style: { padding: '12px 14px', position: 'relative' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 244}}
          , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 245}}
            , React.createElement(Clock, { size: 15, color: "#f59e0b", __self: this, __source: {fileName: _jsxFileName, lineNumber: 246}} )
            , React.createElement('span', { style: { color: '#64748b', fontWeight: '700', fontSize: '11px', textTransform: 'uppercase' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 247}}, "Berjalan")
          )
          , React.createElement('h3', { style: { fontWeight: '800', color: '#1e293b', margin: 0, fontSize: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 249}}
            , data.count_belum, " " , React.createElement('span', { style: { fontSize: '11px', fontWeight: '500', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 250}}, "Nota")
          )
          , React.createElement('div', { style: { fontSize: '12px', color: '#d97706', marginTop: '2px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 252}}, "Rp " , formatRp(data.total_belum_idr))
        )

        /* Overdue Invoices */
        , React.createElement('div', { className: "card-premium", style: { padding: '12px 14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 256}}
          , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 257}}
            , React.createElement(ShieldAlert, { size: 15, color: "#ef4444", __self: this, __source: {fileName: _jsxFileName, lineNumber: 258}} )
            , React.createElement('span', { style: { color: '#64748b', fontWeight: '700', fontSize: '11px', textTransform: 'uppercase' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 259}}, "Telat Bayar" )
          )
          , React.createElement('h3', { style: { fontWeight: '800', color: '#ef4444', margin: 0, fontSize: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 261}}
            , data.count_telat, " " , React.createElement('span', { style: { fontSize: '11px', fontWeight: '500', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 262}}, "Nota")
          )
          , React.createElement('div', { style: { fontSize: '12px', color: '#ef4444', marginTop: '2px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 264}}, "Rp " , formatRp(data.total_telat_idr))
        )

        /* Paid Invoices */
        , React.createElement('div', { className: "card-premium", style: { padding: '12px 14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 268}}
          , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 269}}
            , React.createElement(CheckCircle, { size: 15, color: "#10b981", __self: this, __source: {fileName: _jsxFileName, lineNumber: 270}} )
            , React.createElement('span', { style: { color: '#64748b', fontWeight: '700', fontSize: '11px', textTransform: 'uppercase' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 271}}, "Nota Lunas" )
          )
          , React.createElement('h3', { style: { fontWeight: '800', color: '#10b981', margin: 0, fontSize: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 273}}
            , data.count_lunas, " " , React.createElement('span', { style: { fontSize: '11px', fontWeight: '500', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 274}}, "Nota")
          )
          , React.createElement('div', { style: { fontSize: '12px', color: '#10b981', marginTop: '2px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 276}}, "Rp " , formatRp(data.total_lunas_idr))
        )

      )

      /* Details Grid Section */
      , React.createElement('div', { className: "detail-grid", __self: this, __source: {fileName: _jsxFileName, lineNumber: 282}}

        /* Nono Debt Card */
        , React.createElement('div', { className: "card-premium", style: { padding: '18px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 285}}
          , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 286}}
            , React.createElement('h5', { style: { fontWeight: '800', color: '#1e293b', margin: '0 0 12px 0', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 287}}
              , React.createElement(Package, { color: "#ef4444", size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 288}} ), " Hutang Bahan Nono"
            )
            , React.createElement('p', { style: { fontSize: '12px', color: '#64748b', margin: '0 0 12px 0', lineHeight: '1.4' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 290}}, "Rekapitulasi sisa kewajiban pembayaran bahan baku yang diambil dari supplier Mas Nono."

            )
          )

          , React.createElement('div', { style: { display: 'flex', flexDirection: 'column', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 295}}
            , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', padding: '10px 12px', borderRadius: '8px', background: '#f8fafc', border: '1px solid #e2e8f0' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 296}}
              , React.createElement('span', { style: { color: '#64748b', fontSize: '12px', fontWeight: '500' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 297}}, "Total Pengambilan" )
              , React.createElement('span', { style: { fontWeight: '700', fontSize: '12px', color: '#334155' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 298}}, "Rp " , formatRp(data.nono_total_bahan))
            )

            , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', padding: '12px', borderRadius: '10px', background: '#fef2f2', border: '1px solid #fecaca' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 301}}
              , React.createElement('span', { style: { color: '#991b1b', fontWeight: '800', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 302}}, "Sisa Hutang Bahan"  )
              , React.createElement('span', { style: { fontWeight: '800', color: '#991b1b', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 303}}, "Rp " , formatRp(data.nono_sisa_hutang))
            )
          )
        )

        /* Recent Invoices Card */
        , React.createElement('div', { className: "card-premium", style: { padding: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 309}}
          , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 310}}
            , React.createElement('h5', { style: { fontWeight: '800', color: '#1e293b', margin: 0, fontSize: '15px', display: 'flex', alignItems: 'center', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 311}}
              , React.createElement(BookOpen, { color: "#0d6efd", size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 312}} ), " Faktur Penjualan Terbaru"
            )
            , React.createElement('button', { 
              onClick: () => navigate('/invoices'), 
              style: { 
                background: 'transparent', color: '#0d6efd', border: 'none', 
                fontWeight: '700', fontSize: '11px', cursor: 'pointer', 
                display: 'flex', alignItems: 'center', gap: '2px' 
              }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 314}}
, "Semua "
               , React.createElement(ArrowRight, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 322}} )
            )
          )

          , React.createElement('div', { style: { overflowX: 'auto' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 326}}
            , React.createElement('table', { style: { width: '100%', borderCollapse: 'separate', borderSpacing: '0 6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 327}}
              , React.createElement('tbody', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 328}}
                , (data.recent_invoices || []).slice(0, 4).map((r, idx) => {
                  let badgeColor = '#64748b', badgeBg = '#f1f5f9';
                  if (r.status === 'PAID') { badgeColor = '#166534'; badgeBg = '#dcfce7'; }
                  else if (r.status === 'OVERDUE') { badgeColor = '#991b1b'; badgeBg = '#fee2e2'; }
                  
                  return (
                    React.createElement('tr', { key: idx, style: { background: '#f8fafc' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 335}}
                      , React.createElement('td', { style: { padding: '8px 10px', borderRadius: '8px 0 0 8px', border: '1px solid #e2e8f0', borderRight: 'none' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 336}}
                        , React.createElement('div', { style: { fontWeight: '800', color: '#0d6efd', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 337}}, r.number)
                        , React.createElement('div', { style: { fontSize: '11px', color: '#64748b', marginTop: '1px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '140px' }, title: r.client_name, __self: this, __source: {fileName: _jsxFileName, lineNumber: 338}}
                          , r.client_name
                        )
                      )
                      , React.createElement('td', { style: { padding: '8px 10px', textAlign: 'right', borderRadius: '0 8px 8px 0', border: '1px solid #e2e8f0', borderLeft: 'none' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 342}}
                        , React.createElement('div', { style: { fontWeight: '800', fontSize: '12px', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 343}}, "Rp " , formatRp(r.get_total))
                        , React.createElement('span', { style: { fontSize: '9px', fontWeight: '800', color: badgeColor, background: badgeBg, padding: '1px 5px', borderRadius: '4px', display: 'inline-block', marginTop: '2px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 344}}
                          , r.status
                        )
                      )
                    )
                  );
                })
                , (!data.recent_invoices || data.recent_invoices.length === 0) && (
                  React.createElement('tr', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 352}}
                    , React.createElement('td', { colSpan: 2, style: { textAlign: 'center', padding: '20px', color: '#64748b', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 353}}, "Belum ada data faktur terbaru."

                    )
                  )
                )
              )
            )
          )
        )

      )

    )
  );
};

export default Dashboard;
