import React, { useState, useEffect } from 'react';
import { CreditCard, ArrowDownCircle, ArrowUpCircle, TrendingUp, Plus, Edit2, Trash2, Lock } from 'lucide-react';
import api from '../api';

export default function Keuangan() {
  const [isPremium, setIsPremium] = useState(localStorage.getItem('posbah_premium') === 'true');
  const [activeTab, setActiveTab] = useState('REKAP'); // REKAP, PAYABLE, RECEIVABLE, EXPENSE
  const [finances, setFinances] = useState([]);
  const [reports, setReports] = useState(null);
  
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ id: null, type: 'EXPENSE', amount: '', description: '', date: '', status: 'PENDING' });

  useEffect(() => {
    if (isPremium) {
      fetchData();
    }
  }, [activeTab, isPremium]);

  const fetchData = async () => {
    try {
      if (activeTab === 'REKAP') {
        const res = await api.get('/reports');
        setReports(res.data);
      } else if (activeTab === 'SALES') {
        const res = await api.get('/transactions');
        setFinances(res.data);
      } else {
        const res = await api.get('/finances');
        setFinances(res.data.filter(f => f.type === activeTab));
      }
    } catch (err) {
      console.error('Failed to fetch data', err);
    }
  };

  const handleOpenModal = (finance = null) => {
    if (finance) {
      setFormData({
        ...finance,
        date: new Date(finance.date).toISOString().slice(0, 16)
      });
    } else {
      setFormData({ 
        id: null, 
        type: activeTab === 'REKAP' ? 'EXPENSE' : activeTab, 
        amount: '', 
        description: '', 
        date: new Date().toISOString().slice(0, 16),
        status: 'PENDING' 
      });
    }
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      if (formData.id) {
        await api.put(`/finances/${formData.id}`, formData);
      } else {
        await api.post('/finances', formData);
      }
      setIsModalOpen(false);
      fetchData();
    } catch (err) {
      console.error('Failed to save finance', err);
      alert('Gagal menyimpan data.');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Yakin ingin menghapus data ini?')) {
      try {
        await api.delete(`/finances/${id}`);
        fetchData();
      } catch (err) {
        console.error('Failed to delete', err);
      }
    }
  };

  const handleUpdateStatus = async (id, currentStatus) => {
    const newStatus = currentStatus === 'PENDING' ? 'PAID' : 'PENDING';
    try {
      await api.put(`/finances/${id}`, { status: newStatus });
      fetchData();
    } catch (err) {
      console.error('Failed to update status', err);
    }
  };

  const renderTabs = () => (
    <div className="flex gap-4 mb-6 border-b border-gray-200 pb-2" style={{ overflowX: 'auto', whiteSpace: 'nowrap', WebkitOverflowScrolling: 'touch' }}>
      <button 
        className={`font-semibold pb-2 ${activeTab === 'REKAP' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
        onClick={() => setActiveTab('REKAP')}
      >
        Rekap Laporan
      </button>
      <button 
        className={`font-semibold pb-2 ${activeTab === 'SALES' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
        onClick={() => setActiveTab('SALES')}
      >
        Riwayat Transaksi
      </button>
      <button 
        className={`font-semibold pb-2 ${activeTab === 'PAYABLE' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
        onClick={() => setActiveTab('PAYABLE')}
      >
        Hutang (Payable)
      </button>
      <button 
        className={`font-semibold pb-2 ${activeTab === 'RECEIVABLE' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
        onClick={() => setActiveTab('RECEIVABLE')}
      >
        Piutang (Receivable)
      </button>
      <button 
        className={`font-semibold pb-2 ${activeTab === 'EXPENSE' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
        onClick={() => setActiveTab('EXPENSE')}
      >
        Pengeluaran Usaha
      </button>
    </div>
  );

  const renderRekap = () => {
    if (!reports) return <p>Loading...</p>;
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
        <div className="glass-panel p-6 flex items-center gap-4 border-l-4 border-blue-500">
          <TrendingUp size={40} className="text-blue-500" />
          <div>
            <div className="text-gray-500 text-sm font-semibold">Total Penjualan</div>
            <div className="text-2xl font-bold">Rp {reports.totalSales.toLocaleString('id-ID')}</div>
          </div>
        </div>
        <div className="glass-panel p-6 flex items-center gap-4 border-l-4 border-red-500">
          <ArrowDownCircle size={40} className="text-red-500" />
          <div>
            <div className="text-gray-500 text-sm font-semibold">Total Pengeluaran</div>
            <div className="text-2xl font-bold">Rp {reports.totalExpenses.toLocaleString('id-ID')}</div>
          </div>
        </div>
        <div className="glass-panel p-6 flex items-center gap-4 border-l-4 border-green-500">
          <CreditCard size={40} className="text-green-500" />
          <div>
            <div className="text-gray-500 text-sm font-semibold">Pendapatan Bersih (Net)</div>
            <div className="text-2xl font-bold">Rp {reports.netIncome.toLocaleString('id-ID')}</div>
          </div>
        </div>
        <div className="glass-panel p-6 flex items-center gap-4 border-l-4 border-yellow-500">
          <ArrowUpCircle size={40} className="text-yellow-500" />
          <div>
            <div className="text-gray-500 text-sm font-semibold">Piutang Tertunda</div>
            <div className="text-2xl font-bold">Rp {reports.pendingReceivables.toLocaleString('id-ID')}</div>
          </div>
        </div>
      </div>
    );
  };

  const renderTable = () => (
    <div className="glass-panel table-container">
      <table className="data-table">
        <thead>
          <tr>
            <th>Tanggal</th>
            <th>{activeTab === 'SALES' ? 'ID Transaksi / Tipe' : 'Deskripsi'}</th>
            <th>{activeTab === 'SALES' ? 'Total / Diskon' : 'Jumlah (Rp)'}</th>
            {activeTab !== 'EXPENSE' && <th>Status / Info</th>}
            {activeTab !== 'SALES' && <th className="text-right">Aksi</th>}
          </tr>
        </thead>
        <tbody>
          {finances.length > 0 ? (
            finances.map((item) => (
              <tr key={item.id}>
                <td>{new Date(item.date).toLocaleDateString('id-ID', {day: 'numeric', month: 'short', year:'numeric'})}</td>
                
                {activeTab === 'SALES' ? (
                  <>
                    <td>
                      <div className="font-bold">{item.receiptNumber || `#${item.id}`}</div>
                      <div className="text-xs text-gray-500">{item.type} • {item.items?.length || 0} item</div>
                    </td>
                    <td>
                      <div className="font-bold text-gray-800">Rp {item.total.toLocaleString('id-ID')}</div>
                      {item.discount > 0 && <div className="text-xs text-red-500">Diskon: Rp {item.discount.toLocaleString('id-ID')}</div>}
                    </td>
                    <td>
                      <span className="px-2 py-1 bg-blue-50 text-blue-700 text-xs rounded-full font-bold">
                        {item.paymentMethod}
                      </span>
                    </td>
                  </>
                ) : (
                  <>
                    <td>{item.description}</td>
                    <td className="font-semibold text-gray-700">Rp {Number(item.amount || 0).toLocaleString('id-ID')}</td>
                    {activeTab !== 'EXPENSE' && (
                      <td>
                        <button 
                          className={`px-3 py-1 text-xs rounded-full font-bold ${item.status === 'PAID' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}`}
                          onClick={() => handleUpdateStatus(item.id, item.status)}
                        >
                          {item.status}
                        </button>
                      </td>
                    )}
                    <td className="text-right action-btns">
                      <button className="btn btn-icon btn-edit" onClick={() => handleOpenModal(item)}>
                        <Edit2 size={16} />
                      </button>
                      <button className="btn btn-icon btn-danger" onClick={() => handleDelete(item.id)}>
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </>
                )}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="5" className="text-center p-4">Tidak ada data.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );

  const handleExportExcel = () => {
    let csvContent = "data:text/csv;charset=utf-8,";
    if (activeTab === 'REKAP') {
      csvContent += "Laporan Keuangan POSBah\n\n";
      csvContent += "Kategori,Jumlah (Rp)\n";
      csvContent += "Total Penjualan," + reports.totalSales + "\n";
      csvContent += "Total Pengeluaran," + reports.totalExpenses + "\n";
      csvContent += "Pendapatan Bersih," + reports.netIncome + "\n";
      csvContent += "Piutang Tertunda," + reports.pendingReceivables + "\n";
    } else if (activeTab === 'SALES') {
      csvContent += "Tanggal,No Struk,Tipe,Total (Rp),Diskon (Rp),Metode Bayar\n";
      finances.forEach(item => {
        const row = [
          new Date(item.date).toLocaleDateString('id-ID'),
          item.receiptNumber || `#${item.id}`,
          item.type,
          item.total || 0,
          item.discount || 0,
          item.paymentMethod || '-'
        ].join(",");
        csvContent += row + "\n";
      });
    } else {
      csvContent += "Tanggal,Deskripsi,Jumlah (Rp),Status\n";
      finances.forEach(item => {
        const row = [
          new Date(item.date).toLocaleDateString('id-ID'),
          `"${item.description || ''}"`,
          Number(item.amount || 0),
          item.status || '-'
        ].join(",");
        csvContent += row + "\n";
      });
    }
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `Laporan_${activeTab}_POSBah.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleExportPDF = () => {
    const printWindow = window.open('', '_blank');
    let content = `
      <html>
        <head>
          <title>Laporan Keuangan POSBah</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 20px; color: #333; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
            th { background-color: #f8f9fa; }
            .header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #eee; padding-bottom: 10px; }
            .summary-grid { display: flex; flex-wrap: wrap; gap: 15px; }
            .summary-box { border: 1px solid #ddd; padding: 15px; width: calc(50% - 20px); border-radius: 8px; }
            .title { font-size: 24px; font-weight: bold; margin-bottom: 5px; }
          </style>
        </head>
        <body>
          <div class="header">
            <div class="title">POSBah - Laporan ${activeTab}</div>
            <div>Tanggal Cetak: ${new Date().toLocaleString('id-ID')}</div>
          </div>
    `;

    if (activeTab === 'REKAP') {
      content += `
        <div class="summary-grid">
          <div class="summary-box"><strong>Total Penjualan:</strong><br><br> Rp ${reports.totalSales.toLocaleString('id-ID')}</div>
          <div class="summary-box"><strong>Total Pengeluaran:</strong><br><br> Rp ${reports.totalExpenses.toLocaleString('id-ID')}</div>
          <div class="summary-box"><strong>Pendapatan Bersih:</strong><br><br> Rp ${reports.netIncome.toLocaleString('id-ID')}</div>
          <div class="summary-box"><strong>Piutang Tertunda:</strong><br><br> Rp ${reports.pendingReceivables.toLocaleString('id-ID')}</div>
        </div>
      `;
    } else if (activeTab === 'SALES') {
      content += `
        <table>
          <thead>
            <tr>
              <th>Tanggal</th><th>No Struk</th><th>Tipe</th><th>Total (Rp)</th><th>Metode Bayar</th>
            </tr>
          </thead>
          <tbody>
            ${finances.map(item => `
              <tr>
                <td>${new Date(item.date).toLocaleDateString('id-ID', {day: 'numeric', month: 'short', year:'numeric'})}</td>
                <td>${item.receiptNumber || '#' + item.id}</td>
                <td>${item.type}</td>
                <td>Rp ${Number(item.total || 0).toLocaleString('id-ID')}</td>
                <td>${item.paymentMethod || '-'}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;
      content += `
        <table>
          <thead>
            <tr><th>Tanggal</th><th>Deskripsi</th><th>Jumlah (Rp)</th><th>Status</th></tr>
          </thead>
          <tbody>
            ${finances.map(item => `
              <tr>
                <td>${new Date(item.date).toLocaleDateString('id-ID', {day: 'numeric', month: 'short', year:'numeric'})}</td>
                <td>${item.description || '-'}</td>
                <td>Rp ${Number(item.amount || 0).toLocaleString('id-ID')}</td>
                <td>${item.status || '-'}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;
    }

    content += `
        </body>
      </html>
    `;

    printWindow.document.write(content);
    printWindow.document.close();
    printWindow.focus();
    setTimeout(() => {
      printWindow.print();
      printWindow.close();
    }, 250);
  };

  if (!isPremium) {
    return (
      <div className="page-container flex flex-col items-center justify-center text-center" style={{ minHeight: '80vh' }}>
        <div className="glass-panel max-w-md p-8 w-full border-t-4 border-yellow-400">
           <div className="w-20 h-20 bg-yellow-100 text-yellow-600 rounded-full flex items-center justify-center mx-auto mb-6">
             <Lock size={40} />
           </div>
           <h2 className="text-2xl font-bold mb-3 text-gray-800">Akses Terkunci</h2>
           <p className="text-gray-500 mb-8 leading-relaxed">
             Fitur <b>Keuangan & Rekap Laporan Premium</b> hanya tersedia untuk versi berbayar. Lakukan pembayaran untuk membuka kunci semua fitur finansial canggih ini.
           </p>
           <button 
             className="btn btn-primary w-full py-4 text-lg shadow-lg font-bold"
             onClick={() => {
               const key = prompt('Masukkan Kode Lisensi Premium Anda:');
               // Validasi kunci lebih rumit: harus berformat POSBAH-XXXX-XXXX-PRO
               const isValidKey = key && key.startsWith('POSBAH-') && key.endsWith('-PRO') && key.length === 20;
               
               if (isValidKey || key === 'POSBAH-X7V9-QW2R-PRO') {
                 localStorage.setItem('posbah_premium', 'true');
                 setIsPremium(true);
                 alert('Aktivasi Berhasil! Fitur Premium telah terbuka.');
               } else if (key) {
                 alert('Kode Lisensi Tidak Valid!');
               }
             }}
           >
             Buka Kunci Akses
           </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="header-actions">
        <h1>Keuangan & Laporan</h1>
        <div className="flex gap-2 flex-wrap">
          <button className="btn btn-secondary" onClick={handleExportExcel}>
            Excel (CSV)
          </button>
          <button className="btn btn-secondary" onClick={handleExportPDF}>
            Cetak PDF
          </button>
          {activeTab !== 'REKAP' && (
            <button className="btn btn-primary" onClick={() => handleOpenModal()}>
              <Plus size={18} /> Tambah Data
            </button>
          )}
        </div>
      </div>

      {renderTabs()}

      {activeTab === 'REKAP' ? renderRekap() : renderTable()}

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel">
            <h2>{formData.id ? 'Edit Data' : 'Tambah Data'} {activeTab}</h2>
            <form onSubmit={handleSave}>
              <div className="form-group">
                <label>Tipe</label>
                <select name="type" value={formData.type} onChange={(e) => setFormData({...formData, type: e.target.value})} required disabled={activeTab !== 'REKAP'}>
                  <option value="EXPENSE">Pengeluaran Usaha</option>
                  <option value="PAYABLE">Hutang (Payable)</option>
                  <option value="RECEIVABLE">Piutang (Receivable)</option>
                </select>
              </div>
              <div className="form-group">
                <label>Tanggal</label>
                <input type="datetime-local" name="date" value={formData.date} onChange={(e) => setFormData({...formData, date: e.target.value})} required />
              </div>
              <div className="form-group">
                <label>Deskripsi</label>
                <input type="text" name="description" value={formData.description} onChange={(e) => setFormData({...formData, description: e.target.value})} required placeholder="Contoh: Beli Token Listrik" />
              </div>
              <div className="form-group">
                <label>Jumlah (Rp)</label>
                <input type="number" name="amount" value={formData.amount} onChange={(e) => setFormData({...formData, amount: e.target.value})} required />
              </div>
              {formData.type !== 'EXPENSE' && (
                <div className="form-group">
                  <label>Status</label>
                  <select name="status" value={formData.status} onChange={(e) => setFormData({...formData, status: e.target.value})}>
                    <option value="PENDING">PENDING (Belum Lunas)</option>
                    <option value="PAID">PAID (Lunas)</option>
                  </select>
                </div>
              )}
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>Batal</button>
                <button type="submit" className="btn btn-primary">Simpan</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
