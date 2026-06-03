import { useNavigate } from "react-router-dom";
import { ShieldAlert, ArrowLeft, Home } from "lucide-react";
const AccessDenied = () => {
  const navigate = useNavigate();
  return <div style={{
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    minHeight: "80vh",
    padding: "20px",
    background: "transparent"
  }}>
      <div className="glass-card" style={{
    maxWidth: "500px",
    width: "100%",
    padding: "40px 30px",
    borderRadius: "24px",
    background: "rgba(255, 255, 255, 0.75)",
    backdropFilter: "blur(20px)",
    border: "1px solid rgba(255, 255, 255, 0.4)",
    boxShadow: "0 20px 40px rgba(15, 23, 42, 0.08)",
    textAlign: "center",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: "24px",
    animation: "fadeIn 0.6s cubic-bezier(0.16, 1, 0.3, 1)"
  }}>
        {
    /* Shield Icon container with pulsing gradient background */
  }
        <div style={{
    position: "relative",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    width: "80px",
    height: "80px",
    borderRadius: "50%",
    background: "linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%)",
    border: "1px solid #fca5a5"
  }}>
          <ShieldAlert size={40} color="#ef4444" />
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          <h1 style={{
    fontSize: "24px",
    fontWeight: "800",
    color: "#0f172a",
    margin: 0
  }}>
            Akses Ditolak (403)
          </h1>
          <p style={{
    fontSize: "15px",
    color: "#64748b",
    lineHeight: "1.6",
    margin: 0
  }}>
            Maaf, Anda tidak memiliki izin untuk melihat atau mengubah data ini. Akun demo terisolasi sepenuhnya dari database produksi utama.
          </p>
        </div>

        <div style={{
    display: "flex",
    flexDirection: "column",
    gap: "12px",
    width: "100%"
  }}>
          <button
    onClick={() => navigate("/")}
    style={{
      display: "flex",
      justifyContent: "center",
      alignItems: "center",
      gap: "10px",
      padding: "14px 20px",
      background: "linear-gradient(135deg, #0f172a 0%, #1e293b 100%)",
      color: "#ffffff",
      border: "none",
      borderRadius: "12px",
      fontSize: "14px",
      fontWeight: "600",
      cursor: "pointer",
      transition: "all 0.2s ease",
      boxShadow: "0 4px 12px rgba(15, 23, 42, 0.15)"
    }}
    onMouseOver={(e) => {
      e.currentTarget.style.transform = "translateY(-1px)";
      e.currentTarget.style.boxShadow = "0 6px 16px rgba(15, 23, 42, 0.25)";
    }}
    onMouseOut={(e) => {
      e.currentTarget.style.transform = "translateY(0)";
      e.currentTarget.style.boxShadow = "0 4px 12px rgba(15, 23, 42, 0.15)";
    }}
  >
            <Home size={16} />
            Kembali ke Dashboard
          </button>

          <button
    onClick={() => navigate(-1)}
    style={{
      display: "flex",
      justifyContent: "center",
      alignItems: "center",
      gap: "10px",
      padding: "14px 20px",
      background: "transparent",
      color: "#475569",
      border: "1px solid #e2e8f0",
      borderRadius: "12px",
      fontSize: "14px",
      fontWeight: "600",
      cursor: "pointer",
      transition: "all 0.2s ease"
    }}
    onMouseOver={(e) => {
      e.currentTarget.style.background = "#f8fafc";
      e.currentTarget.style.color = "#0f172a";
    }}
    onMouseOut={(e) => {
      e.currentTarget.style.background = "transparent";
      e.currentTarget.style.color = "#475569";
    }}
  >
            <ArrowLeft size={16} />
            Kembali ke Halaman Sebelumnya
          </button>
        </div>
      </div>
    </div>;
};
export default AccessDenied;
