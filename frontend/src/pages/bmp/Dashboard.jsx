import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from '../../services/apiBmp';
import { Zap, Users, Package, DollarSign, Clock, CheckCircle, ArrowRight, ShieldAlert, BookOpen, PlusCircle } from "lucide-react";
const formatRp = (num) => new Intl.NumberFormat("id-ID").format(num);
const Dashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const navigate = useNavigate();
  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener("resize", handleResize);
    api.get("/dashboard").then((res) => {
      setData(res.data.data);
      setLoading(false);
    }).catch((err) => {
      console.error(err);
      setLoading(false);
    });
    return () => window.removeEventListener("resize", handleResize);
  }, []);
  if (loading) return <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "80vh", flexDirection: "column", gap: "15px" }}>
      <div style={{ width: "30px", height: "30px", border: "3px solid #f3f3f3", borderTop: "3px solid #0d6efd", borderRadius: "50%", animation: "spin 1s linear infinite" }} />
      <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
      <div style={{ color: "#6c757d", fontSize: "14px" }}>Memuat dashboard...</div>
    </div>;
  if (!data) return <div style={{ padding: "20px", textAlign: "center", color: "#dc3545" }}>Gagal memuat data.</div>;
  return <div style={{
    padding: isMobile ? "12px" : "20px",
    background: "#f8fafc",
    minHeight: "100%",
    maxWidth: "100vw",
    boxSizing: "border-box",
    overflowX: "hidden"
  }}>
      <style>{`
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
      `}</style>

      {
    /* Header & Quick Toolbar Section */
  }
      <div style={{ display: "flex", flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: "16px", flexWrap: "wrap", gap: "15px" }}>
        <div>
          <h1 style={{ fontSize: isMobile ? "20px" : "24px", margin: 0, fontWeight: "800", color: "#0f172a" }}>Panel Utama</h1>
          <p style={{ margin: "2px 0 0", color: "#64748b", fontSize: "13px" }}>Dashboard Ringkasan Operasional & Keuangan</p>
        </div>

        {
    /* Quick Toolbar */
  }
        <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", width: isMobile ? "100%" : "auto" }}>
          <button
    onClick={() => navigate("/invoices/create")}
    className="action-btn"
    style={{
      background: "linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%)",
      color: "white",
      border: "none",
      padding: "10px 16px",
      borderRadius: "10px",
      fontWeight: "700",
      cursor: "pointer",
      fontSize: "13px",
      display: "flex",
      alignItems: "center",
      gap: "6px",
      justifyContent: "center",
      flex: isMobile ? 1 : "none"
    }}
  >
            <PlusCircle size={15} /> BUAT FAKTUR
          </button>
          <button
    onClick={() => navigate("/kas")}
    className="action-btn"
    style={{
      background: "#ffffff",
      border: "1px solid #cbd5e1",
      color: "#334155",
      padding: "10px 16px",
      borderRadius: "10px",
      fontWeight: "700",
      cursor: "pointer",
      fontSize: "13px",
      display: "flex",
      alignItems: "center",
      gap: "6px",
      justifyContent: "center",
      flex: isMobile ? 1 : "none"
    }}
  >
            <DollarSign size={15} /> KAS KEUANGAN
          </button>
        </div>
      </div>

      {
    /* Financial Summary Section */
  }
      <div className="financial-grid">
        
        {
    /* Real Cash Card */
  }
        <div style={{
    background: "linear-gradient(135deg, #10b981 0%, #047857 100%)",
    color: "white",
    borderRadius: "18px",
    padding: "18px",
    boxShadow: "0 4px 15px rgba(16, 185, 129, 0.1)",
    position: "relative",
    overflow: "hidden",
    display: "flex",
    flexDirection: "column",
    justifyContent: "space-between"
  }}>
          <div>
            <div style={{ textTransform: "uppercase", fontWeight: "700", opacity: 0.85, display: "flex", alignItems: "center", gap: "6px", fontSize: "11px", letterSpacing: "0.5px" }}>
              <DollarSign size={14} /> SALDO KAS RIIL
            </div>
            <h2 style={{ fontSize: isMobile ? "26px" : "32px", fontWeight: "800", margin: "6px 0 12px 0", letterSpacing: "-0.5px" }}>
              Rp {formatRp(data.saldo_kas)}
            </h2>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px", borderTop: "1px solid rgba(255,255,255,0.18)", paddingTop: "10px" }}>
            <div>
              <small style={{ opacity: 0.75, display: "block", fontSize: "9px", fontWeight: "700" }}>TOTAL MASUK</small>
              <span style={{ fontWeight: "800", fontSize: "13px" }}>Rp {formatRp(data.total_kas_in)}</span>
            </div>
            <div>
              <small style={{ opacity: 0.75, display: "block", fontSize: "9px", fontWeight: "700" }}>TOTAL KELUAR (+BAHAN BAKU)</small>
              <span style={{ fontWeight: "800", fontSize: "13px" }}>Rp {formatRp(data.total_kas_out + data.nono_total_bayar)}</span>
            </div>
          </div>
        </div>

        {
    /* Clean Estimation Card */
  }
        <div style={{
    background: "linear-gradient(135deg, #1e293b 0%, #0f172a 100%)",
    color: "white",
    borderRadius: "18px",
    padding: "18px",
    boxShadow: "0 4px 15px rgba(15, 23, 42, 0.1)",
    position: "relative",
    overflow: "hidden",
    display: "flex",
    flexDirection: "column",
    justifyContent: "space-between"
  }}>
          <div>
            <div style={{ textTransform: "uppercase", fontWeight: "700", color: "#f59e0b", display: "flex", alignItems: "center", gap: "6px", fontSize: "11px", letterSpacing: "0.5px" }}>
              <Zap size={14} /> ESTIMASI BERSIH AKHIR
            </div>
            <h2 style={{ fontSize: isMobile ? "26px" : "32px", fontWeight: "800", color: "#f59e0b", margin: "6px 0 10px 0", letterSpacing: "-0.5px" }}>
              Rp {formatRp(data.simulasi_saldo)}
            </h2>
          </div>
          <div style={{ borderTop: "1px solid rgba(255,255,255,0.12)", paddingTop: "8px" }}>
            <p style={{ fontSize: "11px", opacity: 0.75, margin: 0, lineHeight: "1.4" }}>
              Estimasi sisa kas riil aman setelah dikurangi sisa hutang Bahan Baku serta tagihan berjalan selesai dilunasi.
            </p>
          </div>
        </div>
      </div>

      {
    /* Mini Stats 4-Column Row (Desktop) / 2x2 Grid (Mobile) */
  }
      <div className="stats-grid">
        
        {
    /* Combined Clients & Products Stats Box */
  }
        <div className="card-premium" style={{ padding: "14px 16px", display: "flex", flexDirection: "column", justifyContent: "center" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div>
              <div style={{ color: "#64748b", fontSize: "10px", fontWeight: "700", textTransform: "uppercase", letterSpacing: "0.5px" }}>Pelanggan</div>
              <div style={{ fontSize: "16px", fontWeight: "800", color: "#1e293b", marginTop: "2px", display: "flex", alignItems: "center", gap: "4px" }}>
                <Users size={14} color="#0d6efd" /> {data.total_clients}
              </div>
            </div>
            <div style={{ borderLeft: "1px solid #cbd5e1", height: "30px", margin: "0 8px" }} />
            <div>
              <div style={{ color: "#64748b", fontSize: "10px", fontWeight: "700", textTransform: "uppercase", letterSpacing: "0.5px" }}>Produk</div>
              <div style={{ fontSize: "16px", fontWeight: "800", color: "#1e293b", marginTop: "2px", display: "flex", alignItems: "center", gap: "4px" }}>
                <Package size={14} color="#10b981" /> {data.total_products}
              </div>
            </div>
          </div>
        </div>

        {
    /* Unpaid Invoices */
  }
        <div className="card-premium" style={{ padding: "12px 14px", position: "relative" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "4px" }}>
            <Clock size={15} color="#f59e0b" />
            <span style={{ color: "#64748b", fontWeight: "700", fontSize: "11px", textTransform: "uppercase" }}>Berjalan</span>
          </div>
          <h3 style={{ fontWeight: "800", color: "#1e293b", margin: 0, fontSize: "16px" }}>
            {data.count_belum} <span style={{ fontSize: "11px", fontWeight: "500", color: "#64748b" }}>Nota</span>
          </h3>
          <div style={{ fontSize: "12px", color: "#d97706", marginTop: "2px", fontWeight: "700" }}>Rp {formatRp(data.total_belum_idr)}</div>
        </div>

        {
    /* Overdue Invoices */
  }
        <div className="card-premium" style={{ padding: "12px 14px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "4px" }}>
            <ShieldAlert size={15} color="#ef4444" />
            <span style={{ color: "#64748b", fontWeight: "700", fontSize: "11px", textTransform: "uppercase" }}>Telat Bayar</span>
          </div>
          <h3 style={{ fontWeight: "800", color: "#ef4444", margin: 0, fontSize: "16px" }}>
            {data.count_telat} <span style={{ fontSize: "11px", fontWeight: "500", color: "#64748b" }}>Nota</span>
          </h3>
          <div style={{ fontSize: "12px", color: "#ef4444", marginTop: "2px", fontWeight: "700" }}>Rp {formatRp(data.total_telat_idr)}</div>
        </div>

        {
    /* Paid Invoices */
  }
        <div className="card-premium" style={{ padding: "12px 14px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "4px" }}>
            <CheckCircle size={15} color="#10b981" />
            <span style={{ color: "#64748b", fontWeight: "700", fontSize: "11px", textTransform: "uppercase" }}>Nota Lunas</span>
          </div>
          <h3 style={{ fontWeight: "800", color: "#10b981", margin: 0, fontSize: "16px" }}>
            {data.count_lunas} <span style={{ fontSize: "11px", fontWeight: "500", color: "#64748b" }}>Nota</span>
          </h3>
          <div style={{ fontSize: "12px", color: "#10b981", marginTop: "2px", fontWeight: "700" }}>Rp {formatRp(data.total_lunas_idr)}</div>
        </div>

      </div>

      {
    /* Details Grid Section */
  }
      <div className="detail-grid">
        
        {
    /* Nono Debt Card */
  }
        <div className="card-premium" style={{ padding: "18px", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
          <div>
            <h5 style={{ fontWeight: "800", color: "#1e293b", margin: "0 0 12px 0", display: "flex", alignItems: "center", gap: "8px", fontSize: "15px" }}>
              <Package color="#ef4444" size={18} /> Hutang Bahan Baku
            </h5>
            <p style={{ fontSize: "12px", color: "#64748b", margin: "0 0 12px 0", lineHeight: "1.4" }}>
              Rekapitulasi sisa kewajiban pembayaran bahan baku yang diambil dari supplier Bahan Baku.
            </p>
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", padding: "10px 12px", borderRadius: "8px", background: "#f8fafc", border: "1px solid #e2e8f0" }}>
              <span style={{ color: "#64748b", fontSize: "12px", fontWeight: "500" }}>Total Pengambilan</span>
              <span style={{ fontWeight: "700", fontSize: "12px", color: "#334155" }}>Rp {formatRp(data.nono_total_bahan)}</span>
            </div>
            
            <div style={{ display: "flex", justifyContent: "space-between", padding: "12px", borderRadius: "10px", background: "#fef2f2", border: "1px solid #fecaca" }}>
              <span style={{ color: "#991b1b", fontWeight: "800", fontSize: "13px" }}>Sisa Hutang Bahan</span>
              <span style={{ fontWeight: "800", color: "#991b1b", fontSize: "14px" }}>Rp {formatRp(data.nono_sisa_hutang)}</span>
            </div>
          </div>
        </div>

        {
    /* Recent Invoices Card */
  }
        <div className="card-premium" style={{ padding: "18px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
            <h5 style={{ fontWeight: "800", color: "#1e293b", margin: 0, fontSize: "15px", display: "flex", alignItems: "center", gap: "8px" }}>
              <BookOpen color="#0d6efd" size={18} /> Faktur Penjualan Terbaru
            </h5>
            <button
    onClick={() => navigate("/invoices")}
    style={{
      background: "transparent",
      color: "#0d6efd",
      border: "none",
      fontWeight: "700",
      fontSize: "11px",
      cursor: "pointer",
      display: "flex",
      alignItems: "center",
      gap: "2px"
    }}
  >
              Semua <ArrowRight size={12} />
            </button>
          </div>

          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "separate", borderSpacing: "0 6px" }}>
              <tbody>
                {(data.recent_invoices || []).slice(0, 4).map((r, idx) => {
    let badgeColor = "#64748b", badgeBg = "#f1f5f9";
    if (r.status === "PAID") {
      badgeColor = "#166534";
      badgeBg = "#dcfce7";
    } else if (r.status === "OVERDUE") {
      badgeColor = "#991b1b";
      badgeBg = "#fee2e2";
    }
    return <tr key={idx} style={{ background: "#f8fafc" }}>
                      <td style={{ padding: "8px 10px", borderRadius: "8px 0 0 8px", border: "1px solid #e2e8f0", borderRight: "none" }}>
                        <div style={{ fontWeight: "800", color: "#0d6efd", fontSize: "12px" }}>{r.number}</div>
                        <div style={{ fontSize: "11px", color: "#64748b", marginTop: "1px", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: "140px" }} title={r.client_name}>
                          {r.client_name}
                        </div>
                      </td>
                      <td style={{ padding: "8px 10px", textAlign: "right", borderRadius: "0 8px 8px 0", border: "1px solid #e2e8f0", borderLeft: "none" }}>
                        <div style={{ fontWeight: "800", fontSize: "12px", color: "#1e293b" }}>Rp {formatRp(r.get_total)}</div>
                        <span style={{ fontSize: "9px", fontWeight: "800", color: badgeColor, background: badgeBg, padding: "1px 5px", borderRadius: "4px", display: "inline-block", marginTop: "2px" }}>
                          {r.status}
                        </span>
                      </td>
                    </tr>;
  })}
                {(!data.recent_invoices || data.recent_invoices.length === 0) && <tr>
                    <td colSpan={2} style={{ textAlign: "center", padding: "20px", color: "#64748b", fontSize: "12px" }}>
                      Belum ada data faktur terbaru.
                    </td>
                  </tr>}
              </tbody>
            </table>
          </div>
        </div>

      </div>

    </div>;
};
export default Dashboard;
