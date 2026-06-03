import { useEffect, useState } from "react";
import api from '../../services/apiBmp';
import { Save, Building2, Factory, Zap, Users, Wallet, Calendar, Box, Package, RotateCw } from "lucide-react";
const Settings = () => {
  const [settings, setSettings] = useState({
    ClientName: "",
    AddressLine1: "",
    PhoneNumber: "",
    EmailAddress: "",
    ListrikBulanan: 3e7,
    JumlahMesin: 5,
    JumlahKaryawan: 19,
    GajiHarian: 8e4,
    HariKerjaSebulan: 26,
    HoursPerDay: 24,
    BiayaKarungPer1000: 21e5,
    ClientLogo: ""
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener("resize", handleResize);
    api.get("/settings").then((res) => {
      if (res.data.data) {
        setSettings(res.data.data);
      }
      setLoading(false);
    }).catch(() => {
      setLoading(false);
    });
    return () => window.removeEventListener("resize", handleResize);
  }, []);
  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await api.put("/settings", settings);
      alert("\u2705 Pengaturan berhasil disimpan!");
    } catch (err) {
      alert("\u274C Gagal menyimpan pengaturan.");
    } finally {
      setSaving(false);
    }
  };
  if (loading) return <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "80vh", flexDirection: "column", gap: "15px" }}>
      <div style={{ width: "30px", height: "30px", border: "3px solid #f3f3f3", borderTop: "3px solid #0d6efd", borderRadius: "50%", animation: "spin 1s linear infinite" }} />
      <div style={{ color: "#6c757d" }}>Memuat pengaturan...</div>
    </div>;
  const SectionTitle = ({ icon: Icon, title }) => <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "20px", paddingBottom: "10px", borderBottom: "1px solid #f1f5f9" }}>
        <div style={{ background: "#ebf4ff", color: "#0d6efd", padding: "8px", borderRadius: "10px" }}>
            <Icon size={20} />
        </div>
        <h3 style={{ margin: 0, fontSize: "18px", fontWeight: "800", color: "#1e293b" }}>{title}</h3>
    </div>;
  return <div style={{ padding: isMobile ? "15px" : "40px", background: "#f8f9fa", minHeight: "100%", maxWidth: "100vw", boxSizing: "border-box" }}>
      <div style={{ maxWidth: "900px", margin: "0 auto" }}>
          <div style={{ marginBottom: "30px" }}>
            <h2 style={{ fontSize: isMobile ? "24px" : "32px", fontWeight: "800", margin: 0, color: "#1e293b" }}>Konfigurasi Pabrik</h2>
            <p style={{ margin: "5px 0 0 0", color: "#64748b", fontSize: "14px" }}>Sesuaikan profil perusahaan dan parameter perhitungan HPP</p>
          </div>

          <form onSubmit={handleSave}>
            {
    /* Profile Section */
  }
            <div style={{ background: "white", padding: isMobile ? "20px" : "35px", borderRadius: "24px", boxShadow: "0 4px 20px rgba(0,0,0,0.04)", border: "1px solid #f1f5f9", marginBottom: "25px" }}>
                <SectionTitle icon={Building2} title="Profil Perusahaan" />
                
                <div style={{ display: "grid", gridTemplateColumns: isMobile ? "1fr" : "1fr 1fr", gap: "20px" }}>
                    <div style={{ gridColumn: isMobile ? "auto" : "span 2" }}>
                        <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#64748b", marginBottom: "8px" }}>Nama Perusahaan (Kop Surat)</label>
                        <input type="text" value={settings.ClientName} onChange={(e) => setSettings({ ...settings, ClientName: e.target.value })} style={{ width: "100%", padding: "12px", border: "1px solid #e2e8f0", borderRadius: "12px", outline: "none" }} placeholder="CV. Contoh Makmur" />
                    </div>
                    <div>
                        <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#64748b", marginBottom: "8px" }}>Nomor Telepon</label>
                        <input type="text" value={settings.PhoneNumber} onChange={(e) => setSettings({ ...settings, PhoneNumber: e.target.value })} style={{ width: "100%", padding: "12px", border: "1px solid #e2e8f0", borderRadius: "12px", outline: "none" }} placeholder="0812..." />
                    </div>
                    <div>
                        <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#64748b", marginBottom: "8px" }}>Email Bisnis</label>
                        <input type="email" value={settings.EmailAddress} onChange={(e) => setSettings({ ...settings, EmailAddress: e.target.value })} style={{ width: "100%", padding: "12px", border: "1px solid #e2e8f0", borderRadius: "12px", outline: "none" }} placeholder="admin@perusahaan.com" />
                    </div>
                    <div style={{ gridColumn: isMobile ? "auto" : "span 2" }}>
                        <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#64748b", marginBottom: "8px" }}>Alamat Lengkap</label>
                        <textarea value={settings.AddressLine1} onChange={(e) => setSettings({ ...settings, AddressLine1: e.target.value })} style={{ width: "100%", padding: "12px", border: "1px solid #e2e8f0", borderRadius: "12px", outline: "none", minHeight: "80px", fontFamily: "inherit" }} placeholder="Jl. Raya Nomor 123..." />
                    </div>
                </div>
            </div>

            {
    /* HPP Parameters Section */
  }
            <div style={{ background: "white", padding: isMobile ? "20px" : "35px", borderRadius: "24px", boxShadow: "0 4px 20px rgba(0,0,0,0.04)", border: "1px solid #f1f5f9", marginBottom: "35px" }}>
                <SectionTitle icon={Factory} title="Parameter Produksi (HPP)" />
                
                <div style={{ display: "grid", gridTemplateColumns: isMobile ? "1fr" : "1fr 1fr", gap: "20px" }}>
                    <div style={{ background: "#f8fafc", padding: "15px", borderRadius: "16px" }}>
                        <label style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "13px", fontWeight: "700", color: "#475569", marginBottom: "10px" }}><Zap size={14} /> Listrik / Bulan</label>
                        <input type="number" value={settings.ListrikBulanan} onChange={(e) => setSettings({ ...settings, ListrikBulanan: Number(e.target.value) })} style={{ width: "100%", padding: "10px", border: "1px solid #e2e8f0", borderRadius: "10px", outline: "none", fontWeight: "700" }} />
                    </div>
                    <div style={{ background: "#f8fafc", padding: "15px", borderRadius: "16px" }}>
                        <label style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "13px", fontWeight: "700", color: "#475569", marginBottom: "10px" }}><Calendar size={14} /> Hari Kerja / Bulan</label>
                        <input type="number" value={settings.HariKerjaSebulan} onChange={(e) => setSettings({ ...settings, HariKerjaSebulan: Number(e.target.value) })} style={{ width: "100%", padding: "10px", border: "1px solid #e2e8f0", borderRadius: "10px", outline: "none", fontWeight: "700" }} />
                    </div>
                    <div style={{ background: "#f8fafc", padding: "15px", borderRadius: "16px" }}>
                        <label style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "13px", fontWeight: "700", color: "#475569", marginBottom: "10px" }}><Users size={14} /> Jumlah Karyawan</label>
                        <input type="number" value={settings.JumlahKaryawan} onChange={(e) => setSettings({ ...settings, JumlahKaryawan: Number(e.target.value) })} style={{ width: "100%", padding: "10px", border: "1px solid #e2e8f0", borderRadius: "10px", outline: "none", fontWeight: "700" }} />
                    </div>
                    <div style={{ background: "#f8fafc", padding: "15px", borderRadius: "16px" }}>
                        <label style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "13px", fontWeight: "700", color: "#475569", marginBottom: "10px" }}><Wallet size={14} /> Gaji Harian (Rp)</label>
                        <input type="number" value={settings.GajiHarian} onChange={(e) => setSettings({ ...settings, GajiHarian: Number(e.target.value) })} style={{ width: "100%", padding: "10px", border: "1px solid #e2e8f0", borderRadius: "10px", outline: "none", fontWeight: "700" }} />
                    </div>
                    <div style={{ background: "#f8fafc", padding: "15px", borderRadius: "16px" }}>
                        <label style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "13px", fontWeight: "700", color: "#475569", marginBottom: "10px" }}><Box size={14} /> Jumlah Mesin</label>
                        <input type="number" value={settings.JumlahMesin} onChange={(e) => setSettings({ ...settings, JumlahMesin: Number(e.target.value) })} style={{ width: "100%", padding: "10px", border: "1px solid #e2e8f0", borderRadius: "10px", outline: "none", fontWeight: "700" }} />
                    </div>
                    <div style={{ background: "#f8fafc", padding: "15px", borderRadius: "16px" }}>
                        <label style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "13px", fontWeight: "700", color: "#475569", marginBottom: "10px" }}><Zap size={14} /> Jam Operasional / Hari</label>
                        <input type="number" min="1" max="24" value={settings.HoursPerDay || 24} onChange={(e) => setSettings({ ...settings, HoursPerDay: Number(e.target.value) })} style={{ width: "100%", padding: "10px", border: "1px solid #e2e8f0", borderRadius: "10px", outline: "none", fontWeight: "700" }} />
                    </div>
                    <div style={{ background: "#f8fafc", padding: "15px", borderRadius: "16px" }}>
                        <label style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "13px", fontWeight: "700", color: "#475569", marginBottom: "10px" }}><Package size={14} /> Biaya Karung / 1000</label>
                        <input type="number" value={settings.BiayaKarungPer1000} onChange={(e) => setSettings({ ...settings, BiayaKarungPer1000: Number(e.target.value) })} style={{ width: "100%", padding: "10px", border: "1px solid #e2e8f0", borderRadius: "10px", outline: "none", fontWeight: "700" }} />
                    </div>
                </div>
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: "50px" }}>
              <button
    type="submit"
    disabled={saving}
    style={{
      display: "flex",
      alignItems: "center",
      gap: "10px",
      background: "linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%)",
      color: "white",
      border: "none",
      padding: "16px 30px",
      borderRadius: "15px",
      cursor: "pointer",
      fontSize: "16px",
      fontWeight: "800",
      boxShadow: "0 8px 15px rgba(13, 110, 253, 0.25)",
      transition: "all 0.2s",
      width: isMobile ? "100%" : "auto",
      justifyContent: "center",
      opacity: saving ? 0.7 : 1
    }}
  >
                {saving ? <RotateCw size={20} className="animate-spin" /> : <Save size={20} />} 
                Simpan Semua Perubahan
              </button>
            </div>
          </form>
      </div>
      <style>{`
        .animate-spin { animation: spin 1s linear infinite; }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>;
};
export default Settings;
