import { useEffect, useState } from "react";
import api from '../../services/apiBmp';
import { Calendar, DollarSign, FileText, User, Trash2, Search, X, Briefcase, TrendingDown } from "lucide-react";
const Employees = () => {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const fetchHistory = async () => {
    setLoading(true);
    try {
      const res = await api.get("/payroll/history");
      setHistory(res.data.data || []);
    } catch (err) {
      console.error("Gagal memuat riwayat gaji", err);
    }
    setLoading(false);
  };
  useEffect(() => {
    fetchHistory();
  }, []);
  const handleDelete = async (id) => {
    if (!window.confirm("Yakin ingin menghapus riwayat gaji ini? (Data Kas Keluar juga akan otomatis ditarik/dihapus lho!)")) return;
    try {
      await api.delete(`/payroll/history/${id}`);
      alert("Riwayat gaji dan data Kas Keluar berhasil dihapus bersih!");
      fetchHistory();
    } catch (err) {
      console.error(err);
      alert("Gagal menghapus data.");
    }
  };
  const formatRp = (num) => new Intl.NumberFormat("id-ID", { style: "currency", currency: "IDR", minimumFractionDigits: 0 }).format(num);
  const filteredHistory = history.filter((item) => {
    const name = (item.Employee?.Name || "").toLowerCase();
    const position = (item.Employee?.Position || "").toLowerCase();
    const notes = (item.Description || item.Notes || "").toLowerCase();
    const query = searchTerm.toLowerCase();
    return name.includes(query) || position.includes(query) || notes.includes(query);
  });
  const totalPaymentsCount = filteredHistory.length;
  const totalSalaryPaid = filteredHistory.reduce((acc, item) => acc + (item.Amount || 0), 0);
  return <div style={{ padding: "20px", maxWidth: "1200px", margin: "0 auto" }}>
            <style>{`
                .history-card {
                    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                }
                .history-card:hover {
                    transform: translateY(-4px);
                    box-shadow: 0 12px 20px -5px rgba(0, 0, 0, 0.08) !important;
                    border-color: #3b82f6 !important;
                }
                .btn-delete {
                    transition: all 0.2s ease;
                }
                .btn-delete:hover {
                    transform: scale(1.05);
                    background-color: #fee2e2 !important;
                    color: #dc3545 !important;
                }
                .input-focus:focus {
                    border-color: #3b82f6 !important;
                    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15) !important;
                }
            `}</style>

            {
    /* Header Section */
  }
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px", flexWrap: "wrap", gap: "15px" }}>
                <div>
                    <h2 style={{ margin: 0, color: "#0f172a", fontSize: "24px", fontWeight: "800" }}>Riwayat Penggajian</h2>
                    <p style={{ margin: "4px 0 0", color: "#64748b", fontSize: "14px" }}>Catatan pembayaran gaji karyawan & otomatisasi potongan kas</p>
                </div>
            </div>

            {
    /* Stats Cards Row */
  }
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "15px", marginBottom: "20px" }}>
                <div style={{ background: "#ffffff", border: "1px solid #e2e8f0", borderRadius: "16px", padding: "18px", display: "flex", alignItems: "center", gap: "15px", boxShadow: "0 2px 4px rgba(0,0,0,0.02)" }}>
                    <div style={{ width: "48px", height: "48px", borderRadius: "12px", background: "rgba(59, 130, 246, 0.1)", color: "#3b82f6", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                        <DollarSign size={22} />
                    </div>
                    <div>
                        <div style={{ fontSize: "13px", color: "#64748b", fontWeight: "500" }}>Total Transaksi Gaji</div>
                        <div style={{ fontSize: "20px", fontWeight: "800", color: "#1e293b", marginTop: "2px" }}>{totalPaymentsCount} <span style={{ fontSize: "13px", fontWeight: "500", color: "#64748b" }}>Kali</span></div>
                    </div>
                </div>

                <div style={{ background: "#ffffff", border: "1px solid #e2e8f0", borderRadius: "16px", padding: "18px", display: "flex", alignItems: "center", gap: "15px", boxShadow: "0 2px 4px rgba(0,0,0,0.02)" }}>
                    <div style={{ width: "48px", height: "48px", borderRadius: "12px", background: "rgba(239, 68, 68, 0.1)", color: "#ef4444", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                        <TrendingDown size={22} />
                    </div>
                    <div>
                        <div style={{ fontSize: "13px", color: "#64748b", fontWeight: "500" }}>Total Gaji Dibayarkan</div>
                        <div style={{ fontSize: "20px", fontWeight: "800", color: "#ef4444", marginTop: "2px" }}>{formatRp(totalSalaryPaid).replace(",00", "")}</div>
                    </div>
                </div>
            </div>

            {
    /* Search Bar */
  }
            <div style={{ position: "relative", marginBottom: "20px" }}>
                <div style={{ position: "absolute", left: "16px", top: "50%", transform: "translateY(-50%)", color: "#64748b", display: "flex", alignItems: "center" }}>
                    <Search size={18} />
                </div>
                <input
    type="text"
    placeholder="Cari riwayat berdasarkan nama karyawan, posisi, atau catatan..."
    value={searchTerm}
    onChange={(e) => setSearchTerm(e.target.value)}
    className="input-focus"
    style={{
      width: "100%",
      padding: "14px 14px 14px 48px",
      borderRadius: "12px",
      border: "1px solid #cbd5e1",
      fontSize: "15px",
      backgroundColor: "#ffffff",
      boxShadow: "0 2px 8px rgba(0,0,0,0.03)",
      outline: "none",
      transition: "all 0.2s",
      boxSizing: "border-box"
    }}
  />
                {searchTerm && <button
    onClick={() => setSearchTerm("")}
    style={{ position: "absolute", right: "16px", top: "50%", transform: "translateY(-50%)", border: "none", background: "none", cursor: "pointer", color: "#64748b", display: "flex", alignItems: "center", padding: "4px" }}
  >
                        <X size={16} />
                    </button>}
            </div>

            {
    /* Content Container */
  }
            <div style={{ background: "transparent", minHeight: "200px" }}>
                {loading ? <div style={{ textAlign: "center", padding: "50px 20px", color: "#64748b" }}>
                        <div style={{ width: "35px", height: "35px", border: "3px solid #f3f3f3", borderTop: "3px solid #0d6efd", borderRadius: "50%", animation: "spin 1s linear infinite", margin: "0 auto 15px" }} />
                        <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
                        Memuat riwayat...
                    </div> : <>
                        {
    /* Card Grid */
  }
                        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))", gap: "20px" }}>
                            {filteredHistory.length > 0 ? filteredHistory.map((item, idx) => {
    const payDate = new Date(item.PaymentDate || item.CreatedAt);
    const formattedDate = payDate.toLocaleDateString("id-ID", { day: "2-digit", month: "long", year: "numeric" });
    return <div key={item.ID || idx} className="history-card" style={{ background: "#ffffff", borderRadius: "16px", border: "1px solid #e2e8f0", padding: "18px", display: "flex", flexDirection: "column", boxShadow: "0 4px 6px rgba(0,0,0,0.01)" }}>
                                        
                                        {
      /* Header Card: Tanggal & Badge Gaji */
    }
                                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
                                            <div style={{ display: "flex", alignItems: "center", gap: "6px", color: "#64748b", fontSize: "12px", fontWeight: "500" }}>
                                                <Calendar size={13} color="#0d6efd" />
                                                {formattedDate}
                                            </div>
                                            <div style={{ fontSize: "11px", background: "rgba(239, 68, 68, 0.1)", color: "#ef4444", padding: "3px 8px", borderRadius: "8px", fontWeight: "700" }}>
                                                Gaji Keluar
                                            </div>
                                        </div>

                                        {
      /* Info Karyawan */
    }
                                        <div style={{ display: "flex", alignItems: "center", marginBottom: "12px" }}>
                                            <div style={{ width: "38px", height: "38px", borderRadius: "10px", background: "linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%)", color: "#475569", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                                                <User size={18} />
                                            </div>
                                            <div style={{ marginLeft: "12px", minWidth: 0, flex: 1 }}>
                                                <h3 style={{ fontSize: "15px", fontWeight: "800", margin: 0, color: "#1e293b", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                                                    {item.Employee?.Name || "Tidak diketahui"}
                                                </h3>
                                                <div style={{ display: "flex", alignItems: "center", gap: "4px", color: "#64748b", fontSize: "12px", marginTop: "1px" }}>
                                                    <Briefcase size={12} color="#94a3b8" />
                                                    <span>{item.Employee?.Position || "-"}</span>
                                                </div>
                                            </div>
                                        </div>

                                        {
      /* Divider */
    }
                                        <div style={{ height: "1px", backgroundColor: "#f1f5f9", margin: "0 0 12px" }} />

                                        {
      /* Info Tambahan: Catatan & Nominal */
    }
                                        <div style={{ display: "flex", flexDirection: "column", gap: "8px", flex: 1 }}>
                                            <div style={{ display: "flex", gap: "6px", fontSize: "12px", color: "#64748b", background: "#f8fafc", padding: "10px 12px", borderRadius: "10px", fontStyle: "italic" }}>
                                                <FileText size={14} color="#94a3b8" style={{ marginTop: "2px", flexShrink: 0 }} />
                                                <span style={{ wordBreak: "break-word" }}>{item.Description || item.Notes || "Tidak ada catatan"}</span>
                                            </div>

                                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "6px" }}>
                                                <span style={{ fontSize: "12px", fontWeight: "600", color: "#94a3b8" }}>Total Dibayar</span>
                                                <span style={{ fontSize: "16px", fontWeight: "900", color: "#ef4444" }}>
                                                    - {formatRp(item.Amount).replace(",00", "")}
                                                </span>
                                            </div>
                                        </div>

                                        {
      /* Footer Actions */
    }
                                        <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "14px", borderTop: "1px solid #f1f5f9", paddingTop: "10px" }}>
                                            <button
      onClick={() => handleDelete(item.ID)}
      className="btn-delete"
      style={{ background: "#fee2e2", color: "#dc3545", border: "none", padding: "8px", borderRadius: "8px", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}
      title="Hapus Riwayat & Potongan Kas"
    >
                                                <Trash2 size={16} />
                                            </button>
                                        </div>

                                    </div>;
  }) : <div style={{ gridColumn: "1 / -1", textAlign: "center", padding: "60px 20px", background: "#ffffff", borderRadius: "16px", border: "1px solid #e2e8f0" }}>
                                    <div style={{ fontSize: "40px", marginBottom: "10px" }}>🔍</div>
                                    <h3 style={{ fontSize: "16px", fontWeight: "800", color: "#1e293b", margin: "0 0 4px" }}>Riwayat Tidak Ditemukan</h3>
                                    <p style={{ color: "#64748b", fontSize: "13px", margin: 0 }}>Coba gunakan kata kunci pencarian lain.</p>
                                </div>}
                        </div>
                    </>}
            </div>
        </div>;
};
export default Employees;
