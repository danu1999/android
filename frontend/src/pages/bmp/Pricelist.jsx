const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\Pricelist.tsx";import React, { useEffect, useState } from 'react';
import api, { API_URL } from '../../services/apiBmp';
import { ArrowUp, ArrowDown, List, User, TrendingUp } from 'lucide-react';

const Pricelist = () => {
  const [data, setData] = useState([]);
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterClient, setFilterClient] = useState(0);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const cliRes = await api.get('/clients');
      setClients(cliRes.data.data);

      const params = filterClient > 0 ? { client: filterClient } : {};
      const res = await api.get('/pricelist', { params });
      setData(res.data.data);
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  };

  useEffect(() => { fetchData(); }, [filterClient]);

  const formatRp = (num) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(num);

  if (loading && data.length === 0) return (
    React.createElement('div', { style: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 38}}
      , React.createElement('div', { style: { width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 39}})
      , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 40}}, `@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`)
      , React.createElement('div', { style: { color: '#6c757d' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 41}}, "Memuat riwayat harga..."  )
    )
  );

  return (
    React.createElement('div', { style: { padding: isMobile ? '15px' : '30px', background: '#f8f9fa', minHeight: '100%', maxWidth: '100vw', boxSizing: 'border-box', overflowX: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 46}}
      /* Header */
      , React.createElement('div', { style: { display: 'flex', flexDirection: isMobile ? 'column' : 'row', justifyContent: 'space-between', alignItems: isMobile ? 'stretch' : 'center', marginBottom: '25px', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 48}}
        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 49}}
          , React.createElement('h2', { style: { fontSize: isMobile ? '22px' : '28px', fontWeight: '800', margin: 0, color: '#1e293b', display: 'flex', alignItems: 'center', gap: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 50}}
            , React.createElement(List, { size: isMobile ? 24 : 32, color: "#0d6efd", __self: this, __source: {fileName: _jsxFileName, lineNumber: 51}} ), " Pantau Harga"
          )
          , React.createElement('p', { style: { margin: '5px 0 0 0', color: '#64748b', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 53}}, "Histori perubahan harga jual per pelanggan"     )
        )

        , React.createElement('div', { style: { display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 56}}
          /* Tombol Export PDF Produk */
          , React.createElement('button', {
            onClick: () => {
              const token = localStorage.getItem('token');
              const apiUrl = API_URL;
              const absoluteApiUrl = apiUrl.startsWith('http') ? apiUrl : `${window.location.origin}${apiUrl}`;
              const pdfUrl = `${absoluteApiUrl}/pricelist/pdf?token=${token}`;

              if ((window ).Capacitor) {
                window.open(pdfUrl, '_system');
              } else {
                window.open(pdfUrl, '_blank');
              }
            },
            style: {
              display: 'flex', alignItems: 'center', gap: '8px',
              background: 'linear-gradient(135deg, #dc3545, #b02a37)',
              color: 'white', border: 'none', borderRadius: '12px',
              padding: '12px 18px', fontWeight: '700', fontSize: '14px',
              cursor: 'pointer', whiteSpace: 'nowrap',
              boxShadow: '0 4px 12px rgba(220,53,69,0.3)'
            }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 58}}
, "📄 Export PDF Produk"

          )

          /* Filter Pelanggan */
          , React.createElement('div', { style: { position: 'relative', minWidth: isMobile ? '100%' : '220px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 84}}
            , React.createElement(User, { size: 18, style: { position: 'absolute', left: '12px', top: '12px', color: '#94a3b8', zIndex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 85}} )
            , React.createElement('select', {
              value: filterClient,
              onChange: e => setFilterClient(Number(e.target.value)),
              style: { width: '100%', padding: '12px 12px 12px 40px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', background: 'white', fontSize: '14px', appearance: 'none', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 86}}

              , React.createElement('option', { value: 0, __self: this, __source: {fileName: _jsxFileName, lineNumber: 91}}, "Semua Pelanggan" )
              , clients.map(c => React.createElement('option', { key: c.ID, value: c.ID, __self: this, __source: {fileName: _jsxFileName, lineNumber: 92}}, c.ClientName))
            )
          )
        )
      )

      , isMobile ? (
        /* Mobile Card View */
        React.createElement('div', { style: { display: 'flex', flexDirection: 'column', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 100}}
          , data && data.map((d, idx) => (
            React.createElement('div', { key: idx, style: { background: 'white', borderRadius: '20px', padding: '20px', boxShadow: '0 4px 6px rgba(0,0,0,0.02)', border: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 102}}
              , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 103}}
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 104}}
                  , React.createElement('h3', { style: { margin: 0, fontSize: '16px', fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 105}}, d.item)
                  , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '5px', color: '#64748b', fontSize: '12px', marginTop: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 106}}
                    , React.createElement(User, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 107}}), " " , d.client
                  )
                )
                , React.createElement('div', { style: { textAlign: 'right' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 110}}
                    , React.createElement('div', { style: { fontSize: '16px', fontWeight: '800', color: '#0d6efd' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 111}}, formatRp(d.terbaru.Harga))
                    , React.createElement('div', { style: { fontSize: '10px', color: '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 112}}, new Date(d.terbaru.Tanggal).toLocaleDateString('id-ID'))
                )
              )

              , React.createElement('div', { style: { background: '#f8fafc', borderRadius: '12px', padding: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 116}}
                , React.createElement('div', { style: { fontSize: '11px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 117}}, "Faktur: "
                     , React.createElement('span', { style: { fontWeight: '700', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 118}}, d.terbaru.Faktur)
                )
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 120}}
                    , d.status === 'NAIK' && React.createElement('span', { style: { color: '#dc3545', fontWeight: '800', fontSize: '11px', display: 'flex', alignItems: 'center', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 121}}, React.createElement(ArrowUp, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 121}}), " +" , formatRp(d.selisih))
                    , d.status === 'TURUN' && React.createElement('span', { style: { color: '#198754', fontWeight: '800', fontSize: '11px', display: 'flex', alignItems: 'center', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 122}}, React.createElement(ArrowDown, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 122}}), " -" , formatRp(d.selisih))
                    , d.status === 'TETAP' && React.createElement('span', { style: { color: '#64748b', fontWeight: '800', fontSize: '11px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 123}}, "Stabil")
                )
              )
            )
          ))
        )
      ) : (
        /* Desktop Table View */
        React.createElement('div', { style: { background: 'white', borderRadius: '24px', padding: '15px', boxShadow: '0 4px 12px rgba(0,0,0,0.03)', border: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 131}}
          , React.createElement('table', { style: { width: '100%', borderCollapse: 'separate', borderSpacing: '0 8px', textAlign: 'left' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 132}}
            , React.createElement('thead', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 133}}
              , React.createElement('tr', { style: { color: '#64748b', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '1px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 134}}
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 135}}, "Pelanggan")
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 136}}, "Barang")
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 137}}, "Harga Terbaru" )
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 138}}, "Faktur")
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 139}}, "Harga Sebelumnya" )
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 140}}, "Status")
              )
            )
            , React.createElement('tbody', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 143}}
              , data && data.map((d, idx) => (
                React.createElement('tr', { key: idx, style: { background: '#ffffff' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 145}}
                  , React.createElement('td', { style: { padding: '15px 20px', fontWeight: '700', color: '#1e293b', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 146}}, d.client)
                  , React.createElement('td', { style: { padding: '15px 20px', fontWeight: '600', color: '#334155', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 147}}, d.item)
                  , React.createElement('td', { style: { padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 148}}
                    , React.createElement('div', { style: { fontWeight: '800', color: '#0d6efd' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 149}}, formatRp(d.terbaru.Harga))
                    , React.createElement('div', { style: { fontSize: '10px', color: '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 150}}, new Date(d.terbaru.Tanggal).toLocaleDateString('id-ID'))
                  )
                  , React.createElement('td', { style: { padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 152}}
                    , React.createElement('span', { style: { background: '#f1f5f9', padding: '4px 10px', borderRadius: '8px', fontSize: '12px', fontWeight: '700', color: '#475569' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 153}}
                      , d.terbaru.Faktur
                    )
                  )
                  , React.createElement('td', { style: { padding: '15px 20px', color: '#94a3b8', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 157}}
                    , d.sebelumnya ? (
                      React.createElement(React.Fragment, null
                        , React.createElement('div', { style: { fontWeight: '600', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 160}}, formatRp(d.sebelumnya.Harga))
                        , React.createElement('div', { style: { fontSize: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 161}}, new Date(d.sebelumnya.Tanggal).toLocaleDateString('id-ID'))
                      )
                    ) : '-'
                  )
                  , React.createElement('td', { style: { padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 165}}
                    , d.status === 'NAIK' && React.createElement('span', { style: { color: '#dc3545', fontWeight: '800', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 166}}, React.createElement(ArrowUp, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 166}}), " " , formatRp(d.selisih))
                    , d.status === 'TURUN' && React.createElement('span', { style: { color: '#198754', fontWeight: '800', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 167}}, React.createElement(ArrowDown, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 167}}), " " , formatRp(d.selisih))
                    , d.status === 'TETAP' && React.createElement('span', { style: { color: '#64748b', fontWeight: '800', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 168}}, "Tetap")
                  )
                )
              ))
            )
          )
        )
      )

      , (!data || data.length === 0) && !loading && (
        React.createElement('div', { style: { textAlign: 'center', padding: '50px', background: 'white', borderRadius: '24px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 178}}
          , React.createElement(TrendingUp, { size: 48, style: { margin: '0 auto 15px', opacity: 0.2 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 179}} )
          , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 180}}, "Tidak ada riwayat harga ditemukan."    )
        )
      )
    )
  );
};

export default Pricelist;
