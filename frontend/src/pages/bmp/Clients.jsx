const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\Clients.tsx";import React, { useEffect, useState } from 'react';
import api from '../../services/apiBmp';
import { PlusCircle, Edit3, Trash2, Search, X, MapPin, Phone, Wallet, Users, ArrowUpRight } from 'lucide-react';









const Clients = () => {
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({ id: 0, name: '', province: '', phone: '' });
  
  // Search state
  const [searchTerm, setSearchTerm] = useState('');

  const fetchClients = () => {
    setLoading(true);
    api.get('/clients').then((res) => {
      setClients(res.data.data || []);
      setLoading(false);
    }).catch(err => {
      console.error(err);
      setLoading(false);
    });
  };

  useEffect(() => { fetchClients(); }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    const payload = { ClientName: formData.name, Province: formData.province, PhoneNumber: formData.phone };
    try {
      if (formData.id > 0) {
        await api.put(`/clients/${formData.id}`, payload);
      } else {
        await api.post('/clients', payload);
      }
      setShowModal(false);
      fetchClients();
    } catch (err) {
      alert("Gagal menyimpan data klien");
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm("Yakin ingin menghapus klien ini?")) {
      try {
        await api.delete(`/clients/${id}`);
        fetchClients();
      } catch (err) {
        alert("Gagal menghapus klien");
      }
    }
  };

  const openModal = (c) => {
    if (c) setFormData({ id: c.ID, name: c.ClientName, province: c.Province, phone: c.PhoneNumber });
    else setFormData({ id: 0, name: '', province: '', phone: '' });
    setShowModal(true);
  };

  // Avatar initial gradient generator
  const getAvatarGradient = (name) => {
    const char = name.trim().charAt(0).toUpperCase();
    const code = char.charCodeAt(0) || 0;
    const gradients = [
      'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)', // Blue
      'linear-gradient(135deg, #10b981 0%, #047857 100%)', // Green
      'linear-gradient(135deg, #f97316 0%, #c2410c 100%)', // Orange
      'linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)', // Purple
      'linear-gradient(135deg, #ec4899 0%, #be185d 100%)', // Pink
      'linear-gradient(135deg, #06b6d4 0%, #0891b2 100%)', // Cyan
      'linear-gradient(135deg, #6366f1 0%, #4338ca 100%)', // Indigo
    ];
    return gradients[code % gradients.length];
  };

  // Modern filtering logic
  const filteredClients = clients.filter(c => 
    (c.ClientName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (c.Province || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (c.PhoneNumber || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Calculations for summary metrics
  const totalClientsCount = clients.length;
  const totalDeposits = clients.reduce((acc, c) => acc + (c.SaldoTitipan || 0), 0);

  const formatRp = (num) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(num);

  return (
    React.createElement('div', { style: { padding: '20px', maxWidth: '1200px', margin: '0 auto' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 98}}
      , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 99}}, `
        .client-card {
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }
        .client-card:hover {
          transform: translateY(-4px);
          box-shadow: 0 12px 20px -5px rgba(0, 0, 0, 0.08) !important;
          border-color: #3b82f6 !important;
        }
        .btn-action {
          transition: all 0.2s ease;
        }
        .btn-action:hover {
          transform: scale(1.05);
        }
        .btn-add {
          transition: all 0.2s ease;
        }
        .btn-add:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(13, 110, 253, 0.25);
        }
        .input-focus:focus {
          border-color: #3b82f6 !important;
          box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15) !important;
        }
      `)

      /* Header Section */
      , React.createElement('div', { style: { display: 'flex', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', gap: '15px', flexWrap: 'wrap' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 128}}
        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 129}}
          , React.createElement('h2', { style: { fontSize: '24px', fontWeight: '800', margin: 0, color: '#0f172a' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 130}}, "Daftar Klien / Pelanggan"   )
          , React.createElement('p', { style: { margin: '4px 0 0', color: '#64748b', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 131}}, "Kelola database pelanggan dan pantau saldo deposit"      )
        )
        , React.createElement('button', { onClick: () => openModal(), className: "btn-add", style: { display: 'flex', alignItems: 'center', gap: '8px', background: 'linear-gradient(135deg, #0d6efd 0%, #0b5ed7 100%)', color: 'white', border: 'none', padding: '12px 20px', borderRadius: '12px', fontWeight: '600', cursor: 'pointer', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 133}}
          , React.createElement(PlusCircle, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 134}} ), " Tambah Klien"
        )
      )

      /* Summary Cards Row */
      , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '15px', marginBottom: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 139}}
        , React.createElement('div', { style: { background: '#white', backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '16px', padding: '18px', display: 'flex', alignItems: 'center', gap: '15px', boxShadow: '0 2px 4px rgba(0,0,0,0.02)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 140}}
          , React.createElement('div', { style: { width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(59, 130, 246, 0.1)', color: '#3b82f6', display: 'flex', alignItems: 'center', justifySelf: 'center', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 141}}
            , React.createElement(Users, { size: 22, __self: this, __source: {fileName: _jsxFileName, lineNumber: 142}} )
          )
          , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 144}}
            , React.createElement('div', { style: { fontSize: '13px', color: '#64748b', fontWeight: '500' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 145}}, "Total Pelanggan" )
            , React.createElement('div', { style: { fontSize: '20px', fontWeight: '800', color: '#1e293b', marginTop: '2px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 146}}, totalClientsCount, " " , React.createElement('span', { style: { fontSize: '13px', fontWeight: '500', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 146}}, "Orang"))
          )
        )

        , React.createElement('div', { style: { background: '#white', backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '16px', padding: '18px', display: 'flex', alignItems: 'center', gap: '15px', boxShadow: '0 2px 4px rgba(0,0,0,0.02)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 150}}
          , React.createElement('div', { style: { width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(16, 185, 129, 0.1)', color: '#10b981', display: 'flex', alignItems: 'center', justifySelf: 'center', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 151}}
            , React.createElement(Wallet, { size: 22, __self: this, __source: {fileName: _jsxFileName, lineNumber: 152}} )
          )
          , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 154}}
            , React.createElement('div', { style: { fontSize: '13px', color: '#64748b', fontWeight: '500' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 155}}, "Total Saldo Titipan"  )
            , React.createElement('div', { style: { fontSize: '20px', fontWeight: '800', color: '#10b981', marginTop: '2px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 156}}, formatRp(totalDeposits).replace(',00', ''))
          )
        )
      )

      /* Modern Search Bar */
      , React.createElement('div', { style: { position: 'relative', marginBottom: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 162}}
        , React.createElement('div', { style: { position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', display: 'flex', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 163}}
          , React.createElement(Search, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 164}} )
        )
        , React.createElement('input', { 
          type: "text", 
          placeholder: "Cari pelanggan berdasarkan nama, provinsi, atau telepon..."      , 
          value: searchTerm,
          onChange: (e) => setSearchTerm(e.target.value),
          className: "input-focus",
          style: { 
            width: '100%', 
            padding: '14px 14px 14px 48px', 
            borderRadius: '12px', 
            border: '1px solid #cbd5e1', 
            fontSize: '15px',
            backgroundColor: '#ffffff',
            boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
            outline: 'none',
            transition: 'all 0.2s',
            boxSizing: 'border-box'
          }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 166}} 
        )
        , searchTerm && (
          React.createElement('button', { 
            onClick: () => setSearchTerm(''),
            style: { position: 'absolute', right: '16px', top: '50%', transform: 'translateY(-50%)', border: 'none', background: 'none', cursor: 'pointer', color: '#64748b', display: 'flex', alignItems: 'center', padding: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 186}}

            , React.createElement(X, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 190}} )
          )
        )
      )

      /* Loading state */
      , loading ? (
        React.createElement('div', { style: { textAlign: 'center', padding: '50px 20px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 197}}
          , React.createElement('div', { style: { width: '35px', height: '35px', border: '3px solid #f3f3f3', borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 198}})
          , React.createElement('style', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 199}}, `@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`), "Memuat data pelanggan..."

        )
      ) : (
        React.createElement(React.Fragment, null
          /* Card-based grid list */
          , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 205}}
            , filteredClients.map(c => {
              const initial = c.ClientName ? c.ClientName.trim().charAt(0).toUpperCase() : '?';
              return (
                React.createElement('div', { key: c.ID, className: "client-card", style: { background: '#ffffff', borderRadius: '16px', border: '1px solid #e2e8f0', padding: '18px', display: 'flex', flexDirection: 'column', boxShadow: '0 4px 6px rgba(0,0,0,0.01)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 209}}

                  /* Card Header */
                  , React.createElement('div', { style: { display: 'flex', alignItems: 'center', marginBottom: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 212}}
                    , React.createElement('div', { style: { width: '42px', height: '42px', borderRadius: '12px', background: getAvatarGradient(c.ClientName || ''), color: '#ffffff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: '800', fontSize: '16px', flexShrink: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 213}}
                      , initial
                    )
                    , React.createElement('div', { style: { marginLeft: '12px', minWidth: 0, flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 216}}
                      , React.createElement('h3', { style: { fontSize: '16px', fontWeight: '800', margin: 0, color: '#1e293b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }, title: c.ClientName, __self: this, __source: {fileName: _jsxFileName, lineNumber: 217}}
                        , c.ClientName
                      )
                      , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '4px', color: '#64748b', fontSize: '12px', marginTop: '2px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 220}}
                        , React.createElement(MapPin, { size: 12, color: "#94a3b8", __self: this, __source: {fileName: _jsxFileName, lineNumber: 221}} )
                        , React.createElement('span', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 222}}, c.Province || 'Provinsi tidak diisi')
                      )
                    )
                  )

                  /* Divider */
                  , React.createElement('div', { style: { height: '1px', backgroundColor: '#f1f5f9', margin: '0 0 12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 228}})

                  /* Card Details */
                  , React.createElement('div', { style: { display: 'flex', flexDirection: 'column', gap: '8px', flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 231}}
                    , React.createElement('div', { style: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 232}}
                      , React.createElement('span', { style: { color: '#64748b', display: 'flex', alignItems: 'center', gap: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 233}}
                        , React.createElement(Phone, { size: 13, color: "#94a3b8", __self: this, __source: {fileName: _jsxFileName, lineNumber: 234}} ), " Telepon"
                      )
                      , c.PhoneNumber ? (
                        React.createElement('a', { href: `tel:${c.PhoneNumber}`, style: { color: '#0d6efd', fontWeight: '600', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '3px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 237}}
                          , c.PhoneNumber, " " , React.createElement(ArrowUpRight, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 238}} )
                        )
                      ) : (
                        React.createElement('span', { style: { color: '#94a3b8', fontStyle: 'italic' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 241}}, "-")
                      )
                    )

                    /* Deposit Section Container */
                    , React.createElement('div', { style: { background: '#f8fafc', borderRadius: '12px', padding: '10px 12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 246}}
                      , React.createElement('span', { style: { fontSize: '12px', fontWeight: '600', color: '#64748b', display: 'flex', alignItems: 'center', gap: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 247}}
                        , React.createElement(Wallet, { size: 13, color: "#10b981", __self: this, __source: {fileName: _jsxFileName, lineNumber: 248}} ), " Deposit / Saldo"
                      )
                      , React.createElement('span', { style: { fontSize: '14px', fontWeight: '800', color: c.SaldoTitipan > 0 ? '#10b981' : '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 250}}
                        , formatRp(c.SaldoTitipan).replace(',00', '')
                      )
                    )
                  )

                  /* Card Footer Actions */
                  , React.createElement('div', { style: { display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '16px', borderTop: '1px solid #f1f5f9', paddingTop: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 257}}
                    , React.createElement('button', { 
                      onClick: () => openModal(c), 
                      className: "btn-action",
                      style: { background: '#fff', border: '1px solid #ffc107', color: '#856404', padding: '8px 12px', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px', fontSize: '12px', fontWeight: '600' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 258}}

                      , React.createElement(Edit3, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 263}}), " Edit"
                    )
                    , React.createElement('button', { 
                      onClick: () => handleDelete(c.ID), 
                      className: "btn-action",
                      style: { background: '#fff', border: '1px solid #fee2e2', color: '#dc3545', padding: '8px 12px', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px', fontSize: '12px', fontWeight: '600' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 265}}

                      , React.createElement(Trash2, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 270}}), " Hapus"
                    )
                  )

                )
              );
            })
          )

          /* Empty state */
          , filteredClients.length === 0 && (
            React.createElement('div', { style: { textAlign: 'center', padding: '60px 20px', background: '#ffffff', borderRadius: '16px', border: '1px solid #e2e8f0', marginTop: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 281}}
              , React.createElement('div', { style: { fontSize: '40px', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 282}}, "🔍")
              , React.createElement('h3', { style: { fontSize: '16px', fontWeight: '800', color: '#1e293b', margin: '0 0 4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 283}}, "Pelanggan Tidak Ditemukan"  )
              , React.createElement('p', { style: { color: '#64748b', fontSize: '13px', margin: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 284}}, "Coba gunakan kata kunci pencarian lain atau buat klien baru."         )
            )
          )
        )
      )

      /* Modern Modal with Backdrop Blur */
      , showModal && (
        React.createElement('div', { style: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.45)', backdropFilter: 'blur(4px)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, padding: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 292}}
          , React.createElement('div', { style: { background: 'white', padding: '25px', borderRadius: '20px', width: '100%', maxWidth: '420px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04)', boxSizing: 'border-box' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 293}}
            , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 294}}
              , React.createElement('h3', { style: { fontSize: '18px', fontWeight: '800', color: '#1e293b', margin: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 295}}
                , formData.id > 0 ? '📝 Edit Data Klien' : '👤 Tambah Klien Baru'
              )
              , React.createElement('button', { onClick: () => setShowModal(false), style: { border: 'none', background: 'none', color: '#94a3b8', cursor: 'pointer', padding: '4px', display: 'flex', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 298}}
                , React.createElement(X, { size: 20, __self: this, __source: {fileName: _jsxFileName, lineNumber: 299}} )
              )
            )

            , React.createElement('form', { onSubmit: handleSave, __self: this, __source: {fileName: _jsxFileName, lineNumber: 303}}
              , React.createElement('div', { style: { marginBottom: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 304}}
                , React.createElement('label', { style: { display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600', color: '#475569' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 305}}, "Nama Klien "  , React.createElement('span', { style: { color: '#ef4444' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 305}}, "*"))
                , React.createElement('input', { 
                  required: true, 
                  type: "text", 
                  value: formData.name, 
                  onChange: e => setFormData({...formData, name: e.target.value}), 
                  placeholder: "Contoh: Toko Berkah"  ,
                  className: "input-focus",
                  style: { width: '100%', padding: '11px 12px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '14px', boxSizing: 'border-box', transition: 'all 0.2s' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 306}} 
                )
              )

              , React.createElement('div', { style: { marginBottom: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 317}}
                , React.createElement('label', { style: { display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600', color: '#475569' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 318}}, "Provinsi")
                , React.createElement('input', { 
                  type: "text", 
                  value: formData.province, 
                  onChange: e => setFormData({...formData, province: e.target.value}), 
                  placeholder: "Contoh: Jawa Timur"  ,
                  className: "input-focus",
                  style: { width: '100%', padding: '11px 12px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '14px', boxSizing: 'border-box', transition: 'all 0.2s' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 319}} 
                )
              )

              , React.createElement('div', { style: { marginBottom: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 329}}
                , React.createElement('label', { style: { display: 'block', marginBottom: '6px', fontSize: '13px', fontWeight: '600', color: '#475569' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 330}}, "Nomor Telepon" )
                , React.createElement('input', { 
                  type: "text", 
                  value: formData.phone, 
                  onChange: e => setFormData({...formData, phone: e.target.value}), 
                  placeholder: "Contoh: 08123456789" ,
                  className: "input-focus",
                  style: { width: '100%', padding: '11px 12px', border: '1px solid #cbd5e1', borderRadius: '10px', outline: 'none', fontSize: '14px', boxSizing: 'border-box', transition: 'all 0.2s' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 331}} 
                )
              )

              , React.createElement('div', { style: { display: 'flex', gap: '10px', justifyContent: 'flex-end', borderTop: '1px solid #f1f5f9', paddingTop: '15px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 341}}
                , React.createElement('button', { type: "button", onClick: () => setShowModal(false), style: { padding: '10px 16px', background: '#f1f5f9', color: '#475569', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: '600', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 342}}, "Batal")
                , React.createElement('button', { type: "submit", style: { padding: '10px 16px', background: 'linear-gradient(135deg, #0d6efd 0%, #0b5ed7 100%)', color: 'white', border: 'none', borderRadius: '10px', cursor: 'pointer', fontWeight: '600', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 343}}, "Simpan")
              )
            )
          )
        )
      )
    )
  );
};

export default Clients;
