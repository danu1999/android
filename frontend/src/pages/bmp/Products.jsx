const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\Products.tsx"; function _nullishCoalesce(lhs, rhsFn) { if (lhs != null) { return lhs; } else { return rhsFn(); } }import React, { useEffect, useState } from 'react';
import api from '../../services/apiBmp';
import { PlusCircle, Edit, Trash2, Search, ShoppingBag, Clock, Weight, X } from 'lucide-react';












const Products = () => {
  const [products, setProducts] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({ ID: 0, Title: '', Unit: '', Price: 0, BeratGram: 0, CycleTime: 0, Cavity: 1, RejectRate: 0 });

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const fetchProducts = () => {
    setLoading(true);
    api.get('/products').then((res) => {
      setProducts(res.data.data);
      setLoading(false);
    }).catch(err => {
      console.error(err);
      setLoading(false);
    });
  };

  useEffect(() => { fetchProducts(); }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    const payload = { 
        Title: formData.Title, 
        Unit: formData.Unit, 
        Price: Number(formData.Price),
        BeratGram: Number(formData.BeratGram),
        CycleTime: Number(formData.CycleTime),
        Cavity: Number(formData.Cavity) || 1,
        RejectRate: Number(formData.RejectRate) || 0
    };
    try {
      if (formData.ID > 0) {
        await api.put(`/products/${formData.ID}`, payload);
      } else {
        await api.post('/products', payload);
      }
      setShowModal(false);
      fetchProducts();
    } catch (err) {
      alert("Gagal menyimpan data barang");
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm("Yakin ingin menghapus barang ini?")) {
      try {
        await api.delete(`/products/${id}`);
        fetchProducts();
      } catch (e2) {
        alert("Gagal menghapus barang");
      }
    }
  };

  const openModal = (p) => {
    if (p) setFormData({ ...p });
    else setFormData({ ID: 0, Title: '', Unit: 'Pcs', Price: 0, BeratGram: 0, CycleTime: 0, Cavity: 1, RejectRate: 0 });
    setShowModal(true);
  };

  if (loading) return (
    React.createElement('div', { style: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh', flexDirection: 'column', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 85}}
      , React.createElement('div', { style: { width: '30px', height: '30px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 86}})
      , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 87}}, `@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`)
      , React.createElement('div', { style: { color: '#6c757d' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 88}}, "Memuat daftar barang..."  )
    )
  );

  const filteredProducts = products.filter(p => 
    p.Title.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    React.createElement('div', { style: { padding: isMobile ? '15px' : '30px', background: '#f8f9fa', minHeight: '100%', maxWidth: '100vw', boxSizing: 'border-box', overflowX: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 97}}
      /* Header Section */
      , React.createElement('div', { style: { display: 'flex', flexDirection: isMobile ? 'column' : 'row', justifyContent: 'space-between', alignItems: isMobile ? 'stretch' : 'center', marginBottom: '25px', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 99}}
        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 100}}
          , React.createElement('h2', { style: { fontSize: isMobile ? '22px' : '28px', fontWeight: '800', margin: 0, color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 101}}, "Katalog Barang" )
          , React.createElement('p', { style: { margin: '5px 0 0 0', color: '#64748b', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 102}}, "Manajemen stok dan harga satuan"    )
        )
        , React.createElement('button', { 
          onClick: () => openModal(), 
          style: { 
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', 
            background: 'linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%)', color: 'white', 
            border: 'none', padding: '12px 24px', borderRadius: '12px', cursor: 'pointer', 
            fontWeight: 'bold', boxShadow: '0 4px 10px rgba(13, 110, 253, 0.2)', transition: 'all 0.2s'
          }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 104}}

          , React.createElement(PlusCircle, { size: 20, __self: this, __source: {fileName: _jsxFileName, lineNumber: 113}} ), " Tambah Barang Baru"
        )
      )

      /* Search Bar */
      , React.createElement('div', { style: { marginBottom: '25px', position: 'relative' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 118}}
        , React.createElement(Search, { size: 20, style: { position: 'absolute', left: '15px', top: '12px', color: '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 119}} )
        , React.createElement('input', { 
          type: "text", 
          placeholder: "Cari barang berdasarkan nama..."   , 
          value: searchTerm, 
          onChange: (e) => setSearchTerm(e.target.value), 
          style: { 
            width: '100%', padding: '14px 15px 14px 45px', borderRadius: '12px', 
            border: '1px solid #e2e8f0', outline: 'none', background: 'white', 
            fontSize: '15px', boxShadow: '0 2px 4px rgba(0,0,0,0.02)', transition: 'border 0.2s'
          }, 
          onFocus: (e) => e.target.style.borderColor = '#0d6efd',
          onBlur: (e) => e.target.style.borderColor = '#e2e8f0', __self: this, __source: {fileName: _jsxFileName, lineNumber: 120}}
        )
      )

      , isMobile ? (
        /* Mobile Card View */
        React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr', gap: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 137}}
          , filteredProducts.map(p => (
            React.createElement('div', { key: p.ID, style: { background: 'white', borderRadius: '16px', padding: '18px', boxShadow: '0 4px 6px rgba(0,0,0,0.05)', border: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 139}}
              , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 140}}
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 141}}
                  , React.createElement('h3', { style: { margin: 0, fontSize: '17px', fontWeight: '700', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 142}}, p.Title)
                  , React.createElement('span', { style: { fontSize: '12px', color: '#64748b', background: '#f1f5f9', padding: '2px 8px', borderRadius: '6px', marginTop: '5px', display: 'inline-block' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 143}}, p.Unit)
                )
                , React.createElement('div', { style: { textAlign: 'right' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 145}}
                  , React.createElement('div', { style: { fontSize: '16px', fontWeight: '800', color: '#0d6efd' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 146}}, "Rp " , p.Price.toLocaleString('id-ID'))
                )
              )

              , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', background: '#f8fafc', padding: '12px', borderRadius: '10px', marginBottom: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 150}}
                , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 151}}
                  , React.createElement(Weight, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 152}} ), " " , p.BeratGram, " g"
                )
                , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 154}}
                  , React.createElement(Clock, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 155}} ), " " , p.CycleTime, " s"
                )
                , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 157}}, "Cavity: "
                   , p.Cavity || 1
                )
                , (_nullishCoalesce(p.RejectRate, () => ( 0))) > 0 && (
                  React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#dc2626', fontWeight: '600' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 161}}, "Reject: "
                     , p.RejectRate, "%"
                  )
                )
              )

              , React.createElement('div', { style: { display: 'flex', gap: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 167}}
                , React.createElement('button', { onClick: () => openModal(p), style: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', padding: '10px', background: '#fef9c3', color: '#854d0e', border: 'none', borderRadius: '8px', fontWeight: 'bold', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 168}}
                  , React.createElement(Edit, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 169}} ), " Edit"
                )
                , React.createElement('button', { onClick: () => handleDelete(p.ID), style: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', padding: '10px', background: '#fee2e2', color: '#b91c1c', border: 'none', borderRadius: '8px', fontWeight: 'bold', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 171}}
                  , React.createElement(Trash2, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 172}} ), " Hapus"
                )
              )
            )
          ))
        )
      ) : (
        /* Desktop Table View */
        React.createElement('div', { style: { background: 'white', borderRadius: '16px', padding: '10px', boxShadow: '0 4px 6px rgba(0,0,0,0.05)', border: '1px solid #f1f5f9', overflow: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 180}}
          , React.createElement('table', { style: { width: '100%', borderCollapse: 'separate', borderSpacing: '0 10px', textAlign: 'left' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 181}}
            , React.createElement('thead', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 182}}
              , React.createElement('tr', { style: { color: '#64748b', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '1px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 183}}
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 184}}, "Barang")
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 185}}, "Satuan")
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 186}}, "Berat")
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 187}}, "Cycle Time" )
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 188}}, "Cavity")
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 189}}, "Reject %" )
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 190}}, "Harga")
                , React.createElement('th', { style: { padding: '0 20px', fontWeight: '700', textAlign: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 191}}, "Aksi")
              )
            )
            , React.createElement('tbody', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 194}}
              , filteredProducts.map(p => (
                React.createElement('tr', { key: p.ID, style: { background: '#ffffff', transition: 'background 0.2s' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 196}}
                  , React.createElement('td', { style: { padding: '15px 20px', fontWeight: '700', color: '#1e293b', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 197}}, p.Title)
                  , React.createElement('td', { style: { padding: '15px 20px', color: '#64748b', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 198}}, p.Unit)
                  , React.createElement('td', { style: { padding: '15px 20px', color: '#64748b', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 199}}, p.BeratGram, " g" )
                  , React.createElement('td', { style: { padding: '15px 20px', color: '#64748b', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 200}}, p.CycleTime, " s" )
                  , React.createElement('td', { style: { padding: '15px 20px', color: '#64748b', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 201}}, p.Cavity || 1)
                  , React.createElement('td', { style: { padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 202}}
                    , (_nullishCoalesce(p.RejectRate, () => ( 0))) > 0
                      ? React.createElement('span', { style: { background: '#fee2e2', color: '#b91c1c', padding: '2px 8px', borderRadius: '6px', fontSize: '12px', fontWeight: '700' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 204}}, p.RejectRate, "%")
                      : React.createElement('span', { style: { color: '#94a3b8', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 205}}, "-")
                    
                  )
                  , React.createElement('td', { style: { padding: '15px 20px', color: '#0d6efd', fontWeight: '800', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 208}}, "Rp " , p.Price.toLocaleString('id-ID'))
                  , React.createElement('td', { style: { padding: '15px 20px', borderBottom: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 209}}
                    , React.createElement('div', { style: { display: 'flex', gap: '8px', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 210}}
                      , React.createElement('button', { onClick: () => openModal(p), style: { background: '#fef9c3', color: '#854d0e', border: 'none', padding: '8px', borderRadius: '8px', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 211}}, React.createElement(Edit, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 211}}))
                      , React.createElement('button', { onClick: () => handleDelete(p.ID), style: { background: '#fee2e2', color: '#b91c1c', border: 'none', padding: '8px', borderRadius: '8px', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 212}}, React.createElement(Trash2, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 212}}))
                    )
                  )
                )
              ))
            )
          )
        )
      )

      , filteredProducts.length === 0 && !loading && (
        React.createElement('div', { style: { textAlign: 'center', padding: '40px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 223}}
          , React.createElement(ShoppingBag, { size: 48, style: { margin: '0 auto 15px', opacity: 0.3, display: 'block' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 224}} ), "Barang tidak ditemukan."

        )
      )

      /* MODAL FORM */
      , showModal && (
        React.createElement('div', { style: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1200, padding: '15px', backdropFilter: 'blur(4px)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 231}}
          , React.createElement('div', { style: { background: 'white', padding: isMobile ? '20px' : '30px', borderRadius: '20px', width: '100%', maxWidth: '450px', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)', position: 'relative' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 232}}
            , React.createElement('button', { onClick: () => setShowModal(false), style: { position: 'absolute', right: '15px', top: '15px', background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 233}}, React.createElement(X, { size: 24, __self: this, __source: {fileName: _jsxFileName, lineNumber: 233}} ))

            , React.createElement('h3', { style: { marginBottom: '25px', fontSize: '20px', fontWeight: '800', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 235}}, formData.ID > 0 ? 'Update' : 'Tambah', " Data Barang"  )

            , React.createElement('form', { onSubmit: handleSave, __self: this, __source: {fileName: _jsxFileName, lineNumber: 237}}
              , React.createElement('div', { style: { marginBottom: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 238}}
                , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 239}}, "Nama Barang" )
                , React.createElement('input', { required: true, type: "text", value: formData.Title, onChange: e => setFormData({...formData, Title: e.target.value}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '10px', boxSizing: 'border-box', outline: 'none' }, placeholder: "Contoh: Pot Hitam 10"   , __self: this, __source: {fileName: _jsxFileName, lineNumber: 240}} )
              )

              , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 243}}
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 244}}
                    , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 245}}, "Satuan")
                    , React.createElement('input', { type: "text", value: formData.Unit, onChange: e => setFormData({...formData, Unit: e.target.value}), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '10px', boxSizing: 'border-box', outline: 'none' }, placeholder: "Pcs/Lusin", __self: this, __source: {fileName: _jsxFileName, lineNumber: 246}} )
                )
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 248}}
                    , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 249}}, "Harga (Rp)" )
                    , React.createElement('input', { type: "number", required: true, value: formData.Price, onChange: e => setFormData({...formData, Price: e.target.value }), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '10px', boxSizing: 'border-box', outline: 'none' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 250}} )
                )
              )

              , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '18px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 254}}
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 255}}
                    , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 256}}, "Berat (Gram)" )
                    , React.createElement('input', { type: "number", step: "0.01", value: formData.BeratGram, onChange: e => setFormData({...formData, BeratGram: e.target.value }), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '10px', boxSizing: 'border-box', outline: 'none' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 257}} )
                )
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 259}}
                    , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 260}}, "Cycle Time (s)"  )
                    , React.createElement('input', { type: "number", step: "0.01", value: formData.CycleTime, onChange: e => setFormData({...formData, CycleTime: e.target.value }), style: { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '10px', boxSizing: 'border-box', outline: 'none' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 261}} )
                )
              )

              , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '18px', background: '#f0f9ff', padding: '15px', borderRadius: '12px', border: '1px solid #bae6fd' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 265}}
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 266}}
                    , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#0369a1' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 267}}, "Cavity (cetakan/shot)" )
                    , React.createElement('input', { type: "number", min: "1", value: formData.Cavity || 1, onChange: e => setFormData({...formData, Cavity: e.target.value }), style: { width: '100%', padding: '12px', border: '1px solid #bae6fd', borderRadius: '10px', boxSizing: 'border-box', outline: 'none', background: 'white' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 268}} )
                )
                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 270}}
                    , React.createElement('label', { style: { display: 'block', marginBottom: '8px', fontSize: '13px', fontWeight: '600', color: '#0369a1' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 271}}, "Reject Rate (%)"  )
                    , React.createElement('input', { type: "number", min: "0", max: "100", step: "0.1", value: formData.RejectRate || 0, onChange: e => setFormData({...formData, RejectRate: e.target.value }), style: { width: '100%', padding: '12px', border: '1px solid #bae6fd', borderRadius: '10px', boxSizing: 'border-box', outline: 'none', background: 'white' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 272}} )
                )
                , React.createElement('div', { style: { gridColumn: '1/-1' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 274}}
                  , React.createElement('small', { style: { color: '#0369a1', fontSize: '11px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 275}}, "⚙️ Digunakan oleh Kalkulator HPP untuk menghitung output dan modal bahan secara akurat."            )
                )
              )

              , React.createElement('div', { style: { display: 'flex', gap: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 279}}
                , React.createElement('button', { type: "button", onClick: () => setShowModal(false), style: { flex: 1, padding: '12px', background: '#f1f5f9', color: '#64748b', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: 'bold' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 280}}, "Batal")
                , React.createElement('button', { type: "submit", style: { flex: 2, padding: '12px', background: '#0d6efd', color: 'white', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: 'bold' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 281}}, "Simpan Barang" )
              )
            )
          )
        )
      )
    )
  );
};

export default Products;
