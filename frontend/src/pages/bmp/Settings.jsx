const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\Settings.tsx";import React, { useEffect, useState } from 'react';
import api from '../../services/apiBmp';
import { Save, Building2, Factory, Zap, Users, Wallet, Calendar, Box, Package, RotateCw } from 'lucide-react';
















const Settings = () => {
  const [settings, setSettings] = useState({
    ClientName: '', AddressLine1: '', PhoneNumber: '', EmailAddress: '',
    ListrikBulanan: 30000000, JumlahMesin: 5, JumlahKaryawan: 19,
    GajiHarian: 80000, HariKerjaSebulan: 26, HoursPerDay: 24, BiayaKarungPer1000: 2100000,
    ClientLogo: ''
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    
    api.get('/settings').then((res) => {
      if (res.data.data) {
        setSettings(res.data.data);
      }
      setLoading(false);
    }).catch(() => {
      setLoading(false);
    });

    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await api.put('/settings', settings);
      alert('✅ Pengaturan berhasil disimpan!');
    } catch (err) {
      alert('❌ Gagal menyimpan pengaturan.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return (
    React.createElement('div', { style: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 61}}
      , React.createElement('div', { style: { width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 62}})
      , React.createElement('div', { style: { color: '#6c757d' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 63}}, "Memuat pengaturan..." )
    )
  );

  const SectionTitle = ({ icon: Icon, title }) => (
    React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px', paddingBottom: '10px', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 68}}
        , React.createElement('div', { style: { background: '#ebf4ff', color: '#0d6efd', padding: '8px', borderRadius: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 69}}
            , React.createElement(Icon, { size: 20, __self: this, __source: {fileName: _jsxFileName, lineNumber: 70}} )
        )
        , React.createElement('h3', { style: { margin: 0, fontSize: '18px', fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 72}}, title)
    )
  );

  return (
    React.createElement('div', { style: { padding: isMobile ? '15px' : '40px', background: '#f8f9fa', minHeight: '100%', maxWidth: '100vw', boxSizing: 'border-box' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 77}}
      , React.createElement('div', { style: { maxWidth: '900px', margin: '0 auto' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 78}}
          , React.createElement('div', { style: { marginBottom: '30px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 79}}
            , React.createElement('h2', { style: { fontSize: isMobile ? '24px' : '32px', fontWeight: '800', margin: 0, color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 80}}, "Konfigurasi Pabrik" )
            , React.createElement('p', { style: { margin: '5px 0 0 0', color: '#64748b', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 81}}, "Sesuaikan profil perusahaan dan parameter perhitungan HPP"      )
          )

          , React.createElement('form', { onSubmit: handleSave, __self: this, __source: {fileName: _jsxFileName, lineNumber: 84}}
            /* Profile Section */
            , React.createElement('div', { style: { background: 'white', padding: isMobile ? '20px' : '35px', borderRadius: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.04)', border: '1px solid #f1f5f9', marginBottom: '25px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 86}}
                , React.createElement(SectionTitle, { icon: Building2, title: "Profil Perusahaan" , __self: this, __source: {fileName: _jsxFileName, lineNumber: 87}} )

                , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 89}}
                    , React.createElement('div', { style: { gridColumn: isMobile ? 'auto' : 'span 2' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 90}}
                        , React.createElement('label', { style: { display: 'block', fontSize: '13px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 91}}, "Nama Perusahaan (Kop Surat)"   )
                        , React.createElement('input', { type: "text", value: settings.ClientName, onChange: e => setSettings({...settings, ClientName: e.target.value}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none' }, placeholder: "CV. Contoh Makmur"  , __self: this, __source: {fileName: _jsxFileName, lineNumber: 92}} )
                    )
                    , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 94}}
                        , React.createElement('label', { style: { display: 'block', fontSize: '13px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 95}}, "Nomor Telepon" )
                        , React.createElement('input', { type: "text", value: settings.PhoneNumber, onChange: e => setSettings({...settings, PhoneNumber: e.target.value}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none' }, placeholder: "0812...", __self: this, __source: {fileName: _jsxFileName, lineNumber: 96}} )
                    )
                    , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 98}}
                        , React.createElement('label', { style: { display: 'block', fontSize: '13px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 99}}, "Email Bisnis" )
                        , React.createElement('input', { type: "email", value: settings.EmailAddress, onChange: e => setSettings({...settings, EmailAddress: e.target.value}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none' }, placeholder: "admin@perusahaan.com", __self: this, __source: {fileName: _jsxFileName, lineNumber: 100}} )
                    )
                    , React.createElement('div', { style: { gridColumn: isMobile ? 'auto' : 'span 2' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 102}}
                        , React.createElement('label', { style: { display: 'block', fontSize: '13px', fontWeight: '700', color: '#64748b', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 103}}, "Alamat Lengkap" )
                        , React.createElement('textarea', { value: settings.AddressLine1, onChange: e => setSettings({...settings, AddressLine1: e.target.value}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '12px', outline: 'none', minHeight: '80px', fontFamily: 'inherit' }, placeholder: "Jl. Raya Nomor 123..."   , __self: this, __source: {fileName: _jsxFileName, lineNumber: 104}} )
                    )
                )
            )

            /* HPP Parameters Section */
            , React.createElement('div', { style: { background: 'white', padding: isMobile ? '20px' : '35px', borderRadius: '24px', boxShadow: '0 4px 20px rgba(0,0,0,0.04)', border: '1px solid #f1f5f9', marginBottom: '35px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 110}}
                , React.createElement(SectionTitle, { icon: Factory, title: "Parameter Produksi (HPP)"  , __self: this, __source: {fileName: _jsxFileName, lineNumber: 111}} )

                , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 113}}
                    , React.createElement('div', { style: { background: '#f8fafc', padding: '15px', borderRadius: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 114}}
                        , React.createElement('label', { style: { display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: '700', color: '#475569', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 115}}, React.createElement(Zap, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 115}}), " Listrik / Bulan"   )
                        , React.createElement('input', { type: "number", value: settings.ListrikBulanan, onChange: e => setSettings({...settings, ListrikBulanan: Number(e.target.value)}), style: { width: '100%', padding: '10px', border: '1px solid #e2e8f0', borderRadius: '10px', outline: 'none', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 116}} )
                    )
                    , React.createElement('div', { style: { background: '#f8fafc', padding: '15px', borderRadius: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 118}}
                        , React.createElement('label', { style: { display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: '700', color: '#475569', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 119}}, React.createElement(Calendar, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 119}}), " Hari Kerja / Bulan"    )
                        , React.createElement('input', { type: "number", value: settings.HariKerjaSebulan, onChange: e => setSettings({...settings, HariKerjaSebulan: Number(e.target.value)}), style: { width: '100%', padding: '10px', border: '1px solid #e2e8f0', borderRadius: '10px', outline: 'none', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 120}} )
                    )
                    , React.createElement('div', { style: { background: '#f8fafc', padding: '15px', borderRadius: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 122}}
                        , React.createElement('label', { style: { display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: '700', color: '#475569', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 123}}, React.createElement(Users, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 123}}), " Jumlah Karyawan"  )
                        , React.createElement('input', { type: "number", value: settings.JumlahKaryawan, onChange: e => setSettings({...settings, JumlahKaryawan: Number(e.target.value)}), style: { width: '100%', padding: '10px', border: '1px solid #e2e8f0', borderRadius: '10px', outline: 'none', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 124}} )
                    )
                    , React.createElement('div', { style: { background: '#f8fafc', padding: '15px', borderRadius: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 126}}
                        , React.createElement('label', { style: { display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: '700', color: '#475569', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 127}}, React.createElement(Wallet, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 127}}), " Gaji Harian (Rp)"   )
                        , React.createElement('input', { type: "number", value: settings.GajiHarian, onChange: e => setSettings({...settings, GajiHarian: Number(e.target.value)}), style: { width: '100%', padding: '10px', border: '1px solid #e2e8f0', borderRadius: '10px', outline: 'none', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 128}} )
                    )
                    , React.createElement('div', { style: { background: '#f8fafc', padding: '15px', borderRadius: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 130}}
                        , React.createElement('label', { style: { display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: '700', color: '#475569', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 131}}, React.createElement(Box, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 131}}), " Jumlah Mesin"  )
                        , React.createElement('input', { type: "number", value: settings.JumlahMesin, onChange: e => setSettings({...settings, JumlahMesin: Number(e.target.value)}), style: { width: '100%', padding: '10px', border: '1px solid #e2e8f0', borderRadius: '10px', outline: 'none', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 132}} )
                    )
                    , React.createElement('div', { style: { background: '#f8fafc', padding: '15px', borderRadius: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 134}}
                        , React.createElement('label', { style: { display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: '700', color: '#475569', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 135}}, React.createElement(Zap, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 135}}), " Jam Operasional / Hari"    )
                        , React.createElement('input', { type: "number", min: "1", max: "24", value: settings.HoursPerDay || 24, onChange: e => setSettings({...settings, HoursPerDay: Number(e.target.value)}), style: { width: '100%', padding: '10px', border: '1px solid #e2e8f0', borderRadius: '10px', outline: 'none', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 136}} )
                    )
                    , React.createElement('div', { style: { background: '#f8fafc', padding: '15px', borderRadius: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 138}}
                        , React.createElement('label', { style: { display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: '700', color: '#475569', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 139}}, React.createElement(Package, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 139}}), " Biaya Karung / 1000"    )
                        , React.createElement('input', { type: "number", value: settings.BiayaKarungPer1000, onChange: e => setSettings({...settings, BiayaKarungPer1000: Number(e.target.value)}), style: { width: '100%', padding: '10px', border: '1px solid #e2e8f0', borderRadius: '10px', outline: 'none', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 140}} )
                    )
                )
            )

            , React.createElement('div', { style: { display: 'flex', justifyContent: 'flex-end', marginBottom: '50px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 145}}
              , React.createElement('button', { 
                type: "submit", 
                disabled: saving,
                style: { 
                    display: 'flex', alignItems: 'center', gap: '10px', 
                    background: 'linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%)', 
                    color: 'white', border: 'none', padding: '16px 30px', borderRadius: '15px', 
                    cursor: 'pointer', fontSize: '16px', fontWeight: '800',
                    boxShadow: '0 8px 15px rgba(13, 110, 253, 0.25)',
                    transition: 'all 0.2s',
                    width: isMobile ? '100%' : 'auto',
                    justifyContent: 'center',
                    opacity: saving ? 0.7 : 1
                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 146}}

                , saving ? React.createElement(RotateCw, { size: 20, className: "animate-spin", __self: this, __source: {fileName: _jsxFileName, lineNumber: 161}} ) : React.createElement(Save, { size: 20, __self: this, __source: {fileName: _jsxFileName, lineNumber: 161}} ), "Simpan Semua Perubahan"

              )
            )
          )
      )
      , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 167}}, `
        .animate-spin { animation: spin 1s linear infinite; }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `)
    )
  );
};

export default Settings;
