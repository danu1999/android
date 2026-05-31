const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\HppCalculator.tsx"; function _optionalChain(ops) { let lastAccessLHS = undefined; let value = ops[0]; let i = 1; while (i < ops.length) { const op = ops[i]; const fn = ops[i + 1]; i += 2; if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) { return undefined; } if (op === 'access' || op === 'optionalAccess') { lastAccessLHS = value; value = fn(value); } else if (op === 'call' || op === 'optionalCall') { value = fn((...args) => value.call(lastAccessLHS, ...args)); lastAccessLHS = undefined; } } return value; }import React, { useEffect, useState } from 'react';
import api from '../../services/apiBmp';
import { Cpu, Package, Settings as SettingsIcon, RotateCw } from 'lucide-react';

const HppCalculator = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

  const [sim, setSim] = useState({
    listrik: 0, gaji: 0, harga_bahan: 0, isi_karung: 50
  });

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const fetchData = async (params = {}) => {
    setLoading(true);
    try {
      const res = await api.get('/hpp-calculator', { params });
      const responseData = res.data.data;
      setData(responseData);
      
      // Interconnect & sync UI state from settings defaults on initial load
      if (Object.keys(params).length === 0 && responseData) {
        setSim({
          listrik: _optionalChain([responseData, 'access', _ => _.settings, 'optionalAccess', _2 => _2.ListrikBulanan]) || 0,
          gaji: _optionalChain([responseData, 'access', _3 => _3.settings, 'optionalAccess', _4 => _4.GajiHarian]) || 0,
          harga_bahan: responseData.default_harga_bahan || 8000,
          isi_karung: 50
        });
      }
    } catch (err) {
      console.error(err);
    }
    setLoading(false);
  };

  useEffect(() => { fetchData(); }, []);

  const handleSimulate = (e) => {
    e.preventDefault();
    fetchData(sim);
  };

  const formatRp = (num) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(num);

  if (loading && !data) return (
    React.createElement('div', { style: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 52}}
      , React.createElement('div', { style: { width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 53}})
      , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 54}}, `@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`)
      , React.createElement('div', { style: { color: '#6c757d' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 55}}, "Menganalisis HPP..." )
    )
  );

  return (
    React.createElement('div', { style: { padding: isMobile ? '15px' : '30px', background: '#f8f9fa', minHeight: '100%', maxWidth: '100vw', boxSizing: 'border-box', overflowX: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 60}}
      /* Header */
      , React.createElement('div', { style: { marginBottom: '25px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 62}}
        , React.createElement('h2', { style: { fontSize: isMobile ? '22px' : '28px', fontWeight: '800', margin: 0, color: '#1e293b', display: 'flex', alignItems: 'center', gap: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 63}}
          , React.createElement(Cpu, { size: isMobile ? 24 : 32, color: "#0d6efd", __self: this, __source: {fileName: _jsxFileName, lineNumber: 64}} ), " Kalkulator HPP"
        )
        , React.createElement('p', { style: { margin: '5px 0 0 0', color: '#64748b', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 66}}, "Analisis modal dan margin keuntungan per produk"      )
      )

      , React.createElement('div', { style: { display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: '25px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 69}}
        /* Sidebar Simulator */
        , React.createElement('div', { style: { flex: '1', minWidth: isMobile ? '100%' : '320px', maxWidth: isMobile ? '100%' : '350px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 71}}
          , React.createElement('form', { onSubmit: handleSimulate, style: { background: 'white', padding: '25px', borderRadius: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.05)', border: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 72}}
            , React.createElement('h3', { style: { fontSize: '16px', fontWeight: '800', marginBottom: '20px', color: '#1e293b', display: 'flex', alignItems: 'center', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 73}}
                , React.createElement(SettingsIcon, { size: 18, color: "#64748b", __self: this, __source: {fileName: _jsxFileName, lineNumber: 74}} ), " Simulasi Parameter"
            )

            , React.createElement('div', { style: { marginBottom: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 77}}
              , React.createElement('label', { style: { display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 78}}, "Listrik (Rp/Bulan)" )
              , React.createElement('input', { type: "number", value: sim.listrik || '', onChange: e => setSim({...sim, listrik: Number(e.target.value)}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px' }, placeholder: "Masukkan listrik..." , __self: this, __source: {fileName: _jsxFileName, lineNumber: 79}} )
            )
            , React.createElement('div', { style: { marginBottom: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 81}}
              , React.createElement('label', { style: { display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 82}}, "Gaji Harian (Rp/Hari)"  )
              , React.createElement('input', { type: "number", value: sim.gaji || '', onChange: e => setSim({...sim, gaji: Number(e.target.value)}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px' }, placeholder: "Masukkan gaji..." , __self: this, __source: {fileName: _jsxFileName, lineNumber: 83}} )
            )
            , React.createElement('div', { style: { marginBottom: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 85}}
              , React.createElement('label', { style: { display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 86}}, "Harga Bahan (Rp/Kg)"  )
              , React.createElement('input', { type: "number", value: sim.harga_bahan || '', onChange: e => setSim({...sim, harga_bahan: Number(e.target.value)}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px' }, placeholder: "Masukkan harga bahan..."  , __self: this, __source: {fileName: _jsxFileName, lineNumber: 87}} )
            )
            , React.createElement('div', { style: { marginBottom: '25px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 89}}
              , React.createElement('label', { style: { display: 'block', fontSize: '12px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 90}}, "Isi per Karung (Lusin)"   )
              , React.createElement('input', { type: "number", value: sim.isi_karung, onChange: e => setSim({...sim, isi_karung: Number(e.target.value)}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 91}} )
            )

            , React.createElement('button', { type: "submit", style: { width: '100%', padding: '14px', background: '#0d6efd', color: 'white', border: 'none', borderRadius: '12px', cursor: 'pointer', fontWeight: 'bold', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', boxShadow: '0 4px 10px rgba(13, 110, 253, 0.2)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 94}}
                , React.createElement(RotateCw, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 95}} ), " Hitung Ulang"
            )
          )

          , data && (
            React.createElement('div', { style: { background: '#1e293b', color: 'white', padding: '20px', borderRadius: '24px', marginTop: '20px', boxShadow: '0 4px 15px rgba(0,0,0,0.1)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 100}}
              , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', marginBottom: '10px', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 101}}
                  , React.createElement('span', { style: { opacity: 0.7 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 102}}, "Overhead / Jam"  )
                  , React.createElement('span', { style: { fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 103}}, formatRp(data.overhead_jam))
              )
              , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', fontSize: '13px', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 105}}
                  , React.createElement('span', { style: { opacity: 0.7 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 106}}, "Biaya Packing / Lsn"   )
                  , React.createElement('span', { style: { fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 107}}, formatRp(data.packing_lsn))
              )
              , React.createElement('div', { style: { borderTop: '1px solid rgba(255,255,255,0.1)', marginTop: '10px', paddingTop: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 109}})
              , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', marginBottom: '10px', fontSize: '12px', opacity: 0.8 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 110}}
                  , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 111}}, "Jam Operasional" )
                  , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 112}}, _optionalChain([data, 'access', _5 => _5.settings, 'optionalAccess', _6 => _6.HoursPerDay]) || 24, " Jam/Hari" )
              )
              , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', marginBottom: '10px', fontSize: '12px', opacity: 0.8 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 114}}
                  , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 115}}, "Jumlah Mesin" )
                  , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 116}}, _optionalChain([data, 'access', _7 => _7.settings, 'optionalAccess', _8 => _8.JumlahMesin]) || 0, " Unit" )
              )
              , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', marginBottom: '10px', fontSize: '12px', opacity: 0.8 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 118}}
                  , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 119}}, "Karyawan Produksi" )
                  , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 120}}, _optionalChain([data, 'access', _9 => _9.settings, 'optionalAccess', _10 => _10.JumlahKaryawan]) || 0, " Orang" )
              )
              , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', fontSize: '12px', opacity: 0.8 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 122}}
                  , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 123}}, "Hari Kerja / Bulan"   )
                  , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 124}}, _optionalChain([data, 'access', _11 => _11.settings, 'optionalAccess', _12 => _12.HariKerjaSebulan]) || 26, " Hari" )
              )
            )
          )
        )

        /* Main Results */
        , React.createElement('div', { style: { flex: '1', display: 'flex', flexDirection: 'column', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 131}}
          , isMobile ? (
             /* Mobile Cards */
             _optionalChain([data, 'optionalAccess', _13 => _13.hpp_results, 'optionalAccess', _14 => _14.map, 'call', _15 => _15((r, idx) => (
                React.createElement('div', { key: idx, style: { background: 'white', borderRadius: '20px', padding: '20px', boxShadow: '0 4px 6px rgba(0,0,0,0.02)', border: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 135}}
                    , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 136}}
                        , React.createElement('h4', { style: { margin: 0, fontSize: '16px', fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 137}}, r.product.Title)
                        , React.createElement('span', { style: { background: '#ebf4ff', color: '#0d6efd', padding: '4px 10px', borderRadius: '8px', fontSize: '11px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 138}}, r.output_per_jam.toFixed(1), " Lsn/Jam" )
                    )

                    , React.createElement('div', { style: { display: 'flex', gap: '8px', marginBottom: '15px', flexWrap: 'wrap' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 141}}
                        , React.createElement('span', { style: { background: '#f1f5f9', color: '#475569', padding: '2px 8px', borderRadius: '6px', fontSize: '11px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 142}}, "Cavity: "
                           , r.product.Cavity || 1
                        )
                        , r.product.RejectRate > 0 && (
                          React.createElement('span', { style: { background: '#fee2e2', color: '#991b1b', padding: '2px 8px', borderRadius: '6px', fontSize: '11px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 146}}, "Reject: "
                             , r.product.RejectRate, "%"
                          )
                        )
                        , React.createElement('span', { style: { background: '#f1f5f9', color: '#475569', padding: '2px 8px', borderRadius: '6px', fontSize: '11px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 150}}, "Cycle: "
                           , r.product.CycleTime, "s"
                        )
                        , React.createElement('span', { style: { background: '#f1f5f9', color: '#475569', padding: '2px 8px', borderRadius: '6px', fontSize: '11px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 153}}, "Berat: "
                           , r.product.BeratGram, "g"
                        )
                    )

                    , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 158}}
                        , React.createElement('div', { style: { background: '#f8fafc', padding: '10px', borderRadius: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 159}}
                            , React.createElement('small', { style: { color: '#64748b', fontSize: '10px', display: 'block', marginBottom: '2px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 160}}, "Modal Bahan" )
                            , React.createElement('span', { style: { fontWeight: '700', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 161}}, formatRp(r.modal_bahan))
                        )
                        , React.createElement('div', { style: { background: '#f8fafc', padding: '10px', borderRadius: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 163}}
                            , React.createElement('small', { style: { color: '#64748b', fontSize: '10px', display: 'block', marginBottom: '2px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 164}}, "Modal Mesin" )
                            , React.createElement('span', { style: { fontWeight: '700', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 165}}, formatRp(r.modal_mesin))
                        )
                    )

                    , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid #f1f5f9', paddingTop: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 169}}
                        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 170}}
                            , React.createElement('small', { style: { color: '#64748b', fontSize: '10px', display: 'block' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 171}}, "Total HPP" )
                            , React.createElement('span', { style: { fontWeight: '800', fontSize: '18px', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 172}}, formatRp(r.total_hpp))
                        )
                        , React.createElement('div', { style: { textAlign: 'right' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 174}}
                            , React.createElement('small', { style: { color: r.margin > 0 ? '#198754' : '#dc3545', fontSize: '10px', fontWeight: '800', display: 'block' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 175}}, "MARGIN")
                            , React.createElement('span', { style: { fontWeight: '800', fontSize: '18px', color: r.margin > 0 ? '#198754' : '#dc3545' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 176}}, formatRp(r.margin))
                        )
                    )
                )
             ))])
          ) : (
             /* Desktop Table */
             React.createElement('div', { style: { background: 'white', borderRadius: '24px', padding: '15px', boxShadow: '0 4px 12px rgba(0,0,0,0.03)', border: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 183}}
                , React.createElement('table', { style: { width: '100%', borderCollapse: 'separate', borderSpacing: '0 8px', textAlign: 'left' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 184}}
                  , React.createElement('thead', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 185}}
                    , React.createElement('tr', { style: { color: '#64748b', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '1px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 186}}
                      , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 187}}, "Produk")
                      , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 188}}, "Output/Jam")
                      , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 189}}, "M. Bahan" )
                      , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 190}}, "M. Mesin" )
                      , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 191}}, "Total HPP" )
                      , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 192}}, "Harga Jual" )
                      , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 193}}, "Margin")
                    )
                  )
                  , React.createElement('tbody', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 196}}
                    , _optionalChain([data, 'optionalAccess', _16 => _16.hpp_results, 'optionalAccess', _17 => _17.map, 'call', _18 => _18((r, idx) => (
                      React.createElement('tr', { key: idx, style: { background: '#ffffff' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 198}}
                        , React.createElement('td', { style: { padding: '15px 20px', fontWeight: '700', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 199}}
                          , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 200}}, r.product.Title)
                          , React.createElement('div', { style: { fontSize: '11px', color: '#64748b', fontWeight: 'normal', marginTop: '4px', display: 'flex', gap: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 201}}
                            , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 202}}, "Berat: " , r.product.BeratGram, "g")
                            , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 203}}, "Cycle: " , r.product.CycleTime, "s")
                            , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 204}}, "Cavity: " , r.product.Cavity || 1)
                            , r.product.RejectRate > 0 && React.createElement('span', { style: { color: '#ef4444' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 205}}, "Reject: " , r.product.RejectRate, "%")
                          )
                        )
                        , React.createElement('td', { style: { padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 208}}, r.output_per_jam.toFixed(1), " Lsn" )
                        , React.createElement('td', { style: { padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 209}}, formatRp(r.modal_bahan))
                        , React.createElement('td', { style: { padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 210}}, formatRp(r.modal_mesin))
                        , React.createElement('td', { style: { padding: '15px 20px', fontWeight: '800', background: '#fef9c3', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 211}}, formatRp(r.total_hpp))
                        , React.createElement('td', { style: { padding: '15px 20px', fontWeight: '800', color: '#0d6efd', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 212}}, formatRp(r.harga_jual_real))
                        , React.createElement('td', { style: { padding: '15px 20px', fontWeight: '800', color: r.margin > 0 ? '#198754' : '#dc3545', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 213}}
                          , formatRp(r.margin)
                        )
                      )
                    ))])
                  )
                )
             )
          )

          , (!_optionalChain([data, 'optionalAccess', _19 => _19.hpp_results]) || data.hpp_results.length === 0) && !loading && (
             React.createElement('div', { style: { textAlign: 'center', padding: '50px', background: 'white', borderRadius: '24px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 224}}
                , React.createElement(Package, { size: 48, style: { margin: '0 auto 15px', opacity: 0.2 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 225}} )
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 226}}, "Data produk atau pengaturan tidak tersedia."     )
             )
          )
        )
      )
    )
  );
};

export default HppCalculator;
