import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { API_URL } from '../../services/apiBmp';
import { PlusCircle, FileText, DollarSign, Image as ImageIcon, AlertCircle, Edit, Trash2, Search, Calendar, X, Download } from "lucide-react";
import html2canvas from "html2canvas";
import { InvoiceImageTemplate } from '../../components/InvoiceImageTemplate';
import { Share } from "@capacitor/share";
import { Filesystem, Directory } from "@capacitor/filesystem";
const Invoices = () => {
  const [invoices, setInvoices] = useState([]);
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [settings, setSettings] = useState(null);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const LIMIT = 20;
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterClient, setFilterClient] = useState("ALL");
  const [searchTerm, setSearchTerm] = useState("");
  const [clientSummary, setClientSummary] = useState(null);
  const [fullInvoiceData, setFullInvoiceData] = useState(null);
  const [previewImage, setPreviewImage] = useState(null);
  const [previewInvoiceNumber, setPreviewInvoiceNumber] = useState("");
  const [previewClientPhone, setPreviewClientPhone] = useState("");
  const navigate = useNavigate();
  const [showMassal, setShowMassal] = useState(false);
  const [massalData, setMassalData] = useState({ client_id: 0, nominal: 0, metode: "TRANSFER" });
  const [showSingle, setShowSingle] = useState(false);
  const [singleData, setSingleData] = useState({ id: 0, nominal: 0, metode: "TRANSFER", tanggal: (/* @__PURE__ */ new Date()).toISOString().split("T")[0] });
  const [masterProducts, setMasterProducts] = useState([]);
  const [showEdit, setShowEdit] = useState(false);
  const [editData, setEditData] = useState({ id: 0, products: [] });
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [payModalInvoice, setPayModalInvoice] = useState(null);
  const [payModalPayments, setPayModalPayments] = useState([]);
  const [payModalLoading, setPayModalLoading] = useState(false);
  const [payModalEditId, setPayModalEditId] = useState(null);
  const [payModalEditForm, setPayModalEditForm] = useState({ nominal: 0, tanggal: "", metode: "TRANSFER" });
  const [payModalAddVisible, setPayModalAddVisible] = useState(false);
  const [payModalAddForm, setPayModalAddForm] = useState({ nominal: 0, tanggal: (/* @__PURE__ */ new Date()).toISOString().split("T")[0], metode: "TRANSFER" });
  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);
  const fetchData = async (page = 1, status = filterStatus, client = filterClient, search = searchTerm) => {
    setLoading(true);
    try {
      const statusParam = status !== "ALL" ? status : "";
      const clientParam = client !== "ALL" ? client : "";
      const searchParam = encodeURIComponent(search);
      const invRes = await api.get(`/invoices?page=${page}&limit=${LIMIT}&status=${statusParam}&client_id=${clientParam}&search=${searchParam}`);
      setInvoices(invRes.data?.data || []);
      setCurrentPage(invRes.data?.current_page || 1);
      setTotalPages(invRes.data?.total_pages || 1);
      setTotalCount(invRes.data?.total_count || 0);
      const cliRes = await api.get("/clients");
      setClients(cliRes.data?.data || []);
      const setRes = await api.get("/settings");
      setSettings(setRes.data?.data || null);
      const mpRes = await api.get("/products");
      setMasterProducts(mpRes.data?.data || []);
    } catch (err) {
      console.error("Gagal fetch data utama:", err);
      setInvoices([]);
      setClients([]);
      setMasterProducts([]);
    }
    setLoading(false);
  };
  useEffect(() => {
    const timer = setTimeout(() => {
      setCurrentPage(1);
      fetchData(1, filterStatus, filterClient, searchTerm);
    }, 300);
    return () => clearTimeout(timer);
  }, [filterStatus, filterClient, searchTerm]);
  useEffect(() => {
    if (filterClient !== "ALL") {
      api.get(`/clients/${filterClient}/summary`).then((res) => {
        setClientSummary(res.data?.data || null);
      }).catch((err) => {
        console.error("Gagal fetch summary client:", err);
        setClientSummary(null);
      });
    } else {
      setClientSummary(null);
    }
  }, [filterClient]);
  const handleMassal = async (e) => {
    e.preventDefault();
    if (massalData.client_id === 0) return alert("Pilih pelanggan!");
    try {
      await api.post("/invoices/pay-massal", { ...massalData, nominal: Number(massalData.nominal) });
      alert("Pembayaran borongan berhasil!");
      setShowMassal(false);
      fetchData(currentPage, filterStatus, filterClient, searchTerm);
      if (filterClient !== "ALL") {
        api.get(`/clients/${filterClient}/summary`).then((res) => setClientSummary(res.data?.data || null));
      }
    } catch {
      alert("Gagal melakukan pembayaran borongan");
    }
  };
  const handleSingle = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/invoices/${singleData.id}/pay`, {
        nominal: Number(singleData.nominal),
        metode: singleData.metode,
        tanggal: singleData.tanggal + "T00:00:00Z"
      });
      alert("Pembayaran berhasil!");
      setShowSingle(false);
      fetchData(currentPage, filterStatus, filterClient, searchTerm);
    } catch {
      alert("Gagal melakukan pembayaran");
    }
  };
  const handleDelete = async (id) => {
    if (window.confirm("Yakin ingin menghapus faktur ini?")) {
      try {
        await api.delete(`/invoices/${id}`);
        fetchData(currentPage, filterStatus, filterClient, searchTerm);
      } catch {
        alert("Gagal menghapus faktur");
      }
    }
  };
  const openEditModal = async (id) => {
    try {
      const res = await api.get(`/invoices/${id}`);
      const invProducts = (res.data?.products || []).map((p) => ({
        master_item_id: p.MasterItemID,
        quantity: p.Quantity,
        jumlah_lusin: p.JumlahLusin,
        custom_price: p.Price,
        is_khusus: p.IsKhusus || false,
        harga_beli: p.HargaBeli || 0
      }));
      setEditData({ id, products: invProducts });
      setShowEdit(true);
    } catch {
      alert("Gagal memuat data produk faktur");
    }
  };
  const handleEdit = async (e) => {
    e.preventDefault();
    if (editData.products.length === 0) return alert("Minimal 1 produk!");
    const computedProducts = editData.products.map((p) => {
      return {
        ...p,
        harga_beli: p.is_khusus ? Number(p.harga_beli) : 0
      };
    });
    try {
      await api.put(`/invoices/${editData.id}/products`, { products: computedProducts });
      alert("Produk faktur berhasil diupdate!");
      setShowEdit(false);
      fetchData(currentPage, filterStatus, filterClient, searchTerm);
    } catch {
      alert("Gagal update produk faktur");
    }
  };
  const openPaymentModal = async (inv) => {
    setPayModalInvoice(inv);
    setShowPaymentModal(true);
    setPayModalLoading(true);
    setPayModalEditId(null);
    setPayModalAddVisible(false);
    setPayModalAddForm({ nominal: 0, tanggal: (/* @__PURE__ */ new Date()).toISOString().split("T")[0], metode: "TRANSFER" });
    try {
      const res = await api.get(`/invoices/${inv.ID}`);
      setPayModalPayments(res.data?.payments || []);
    } catch {
      alert("Gagal memuat riwayat pembayaran");
    }
    setPayModalLoading(false);
  };
  const handlePayModalEdit = async (paymentId) => {
    try {
      await api.put(`/invoices/payments/${paymentId}`, {
        nominal: Number(payModalEditForm.nominal),
        tanggal: payModalEditForm.tanggal ? payModalEditForm.tanggal + "T00:00:00Z" : void 0,
        metode: payModalEditForm.metode
      });
      setPayModalEditId(null);
      const res = await api.get(`/invoices/${payModalInvoice.ID}`);
      setPayModalPayments(res.data?.payments || []);
      fetchData(currentPage, filterStatus, filterClient, searchTerm);
    } catch {
      alert("Gagal memperbarui pembayaran");
    }
  };
  const handlePayModalDelete = async (paymentId) => {
    if (!window.confirm("Yakin ingin menghapus cicilan ini? Data di Kas juga akan ikut terhapus.")) return;
    try {
      await api.delete(`/invoices/payments/${paymentId}`);
      const res = await api.get(`/invoices/${payModalInvoice.ID}`);
      setPayModalPayments(res.data?.payments || []);
      fetchData(currentPage, filterStatus, filterClient, searchTerm);
    } catch {
      alert("Gagal menghapus cicilan");
    }
  };
  const handlePayModalAdd = async (e) => {
    e.preventDefault();
    if (!payModalAddForm.nominal || Number(payModalAddForm.nominal) <= 0) return alert("Masukkan nominal yang valid");
    try {
      await api.post(`/invoices/${payModalInvoice.ID}/pay`, {
        nominal: Number(payModalAddForm.nominal),
        metode: payModalAddForm.metode,
        tanggal: payModalAddForm.tanggal + "T00:00:00Z"
      });
      setPayModalAddVisible(false);
      setPayModalAddForm({ nominal: 0, tanggal: (/* @__PURE__ */ new Date()).toISOString().split("T")[0], metode: "TRANSFER" });
      const res = await api.get(`/invoices/${payModalInvoice.ID}`);
      setPayModalPayments(res.data?.payments || []);
      fetchData(currentPage, filterStatus, filterClient, searchTerm);
    } catch {
      alert("Gagal menambahkan pembayaran");
    }
  };
  const downloadJPG = async (id, number) => {
    try {
      const res = await api.get(`/invoices/${id}`);
      if (!res.data?.data) {
        alert("Data faktur tidak ditemukan");
        return;
      }
      const currentInv = {
        ...res.data.data,
        Products: res.data?.products || [],
        Payments: res.data?.payments || []
      };
      setFullInvoiceData(currentInv);
      setPreviewInvoiceNumber(number);
      setPreviewClientPhone(res.data?.data?.Client?.PhoneNumber || "");
      setTimeout(() => {
        const element = document.getElementById(`faktur-canvas-${id}`);
        if (element) {
          element.style.display = "block";
          element.style.top = "0";
          element.style.left = "0";
          html2canvas(element, {
            scale: 2,
            useCORS: true,
            backgroundColor: "#ffffff"
          }).then((canvas) => {
            element.style.display = "none";
            element.style.top = "-9999px";
            element.style.left = "-9999px";
            const dataUrl = canvas.toDataURL("image/jpeg", 0.9);
            setPreviewImage(dataUrl);
          });
        }
      }, 4e3);
    } catch {
      alert("Gagal memuat data detail untuk JPG");
    }
  };
  const handleSaveImage = async () => {
    if (!previewImage) return;
    const filename = `Faktur-${previewInvoiceNumber || "BMP"}.jpg`;
    try {
      const isCapacitor = (!!window.Capacitor && window.Capacitor.getPlatform && window.Capacitor.getPlatform() !== 'web') || window.location.protocol === 'capacitor:';
      if (isCapacitor) {
        const base64Data = previewImage.split(",")[1];
        const savedFile = await Filesystem.writeFile({
          path: filename,
          data: base64Data,
          directory: Directory.Cache
        });
        await Share.share({
          title: filename,
          text: `Simpan atau bagikan ${filename}`,
          url: savedFile.uri,
          dialogTitle: "Simpan Gambar Faktur"
        });
      } else {
        const link = document.createElement("a");
        link.download = filename;
        link.href = previewImage;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      }
    } catch (err) {
      console.error("Save image error:", err);
      alert("Gagal menyimpan gambar. Coba long-press pada gambar untuk menyimpan manual.");
    }
  };
  const handleShareWhatsApp = async () => {
    if (!previewImage) return;
    const filename = `Faktur-${previewInvoiceNumber || "BMP"}.jpg`;
    const invoiceText = `Faktur BMP No. ${previewInvoiceNumber}`;
    try {
      const isCapacitor = (!!window.Capacitor && window.Capacitor.getPlatform && window.Capacitor.getPlatform() !== 'web') || window.location.protocol === 'capacitor:';
      if (isCapacitor) {
        const base64Data = previewImage.split(",")[1];
        const savedFile = await Filesystem.writeFile({
          path: filename,
          data: base64Data,
          directory: Directory.Cache
        });
        await Share.share({
          title: invoiceText,
          text: invoiceText,
          url: savedFile.uri,
          dialogTitle: "Kirim Faktur via WhatsApp"
        });
      } else if (navigator.share && navigator.canShare) {
        const response = await fetch(previewImage);
        const blob = await response.blob();
        const file = new File([blob], filename, { type: "image/jpeg" });
        if (navigator.canShare({ files: [file] })) {
          await navigator.share({ files: [file], title: invoiceText, text: invoiceText });
        } else {
          openWhatsAppLink();
        }
      } else {
        openWhatsAppLink();
      }
    } catch (err) {
      if (err?.name !== "AbortError") {
        console.error("Share error:", err);
        openWhatsAppLink();
      }
    }
  };
  const openWhatsAppLink = () => {
    const text = encodeURIComponent(`Berikut adalah faktur BMP No. ${previewInvoiceNumber}. Mohon konfirmasi pembayarannya. Terima kasih.`);
    let phone = (previewClientPhone || "").replace(/[^0-9]/g, "");
    if (phone.startsWith("0")) phone = "62" + phone.slice(1);
    const waUrl = phone ? `https://api.whatsapp.com/send?phone=${phone}&text=${text}` : `https://api.whatsapp.com/send?text=${text}`;
    const target = window.Capacitor ? "_system" : "_blank";
    window.open(waUrl, target);
  };
  const downloadPDF = (id, type) => {
    const token = localStorage.getItem("token");
    const apiUrl = API_URL;
    const absoluteApiUrl = apiUrl.startsWith("http") ? apiUrl : `${window.location.origin}${apiUrl}`;
    const pdfUrl = `${absoluteApiUrl}/invoices/${id}/${type}?token=${token}`;
    if (window.Capacitor) {
      window.open(pdfUrl, "_system");
    } else {
      fetch(`${apiUrl}/invoices/${id}/${type}`, {
        headers: { "Authorization": `Bearer ${token}` }
      }).then((res) => res.blob()).then((blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `${type}-${id}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
      });
    }
  };
  const filteredInvoices = invoices || [];
  const PaginationNav = () => <div style={{
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginTop: "20px",
    padding: "12px 16px",
    background: "white",
    borderRadius: "14px",
    boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
    border: "1px solid #f1f5f9",
    flexWrap: "wrap",
    gap: "10px"
  }}>
            <div style={{ fontSize: "13px", color: "#64748b", fontWeight: "600" }}>
                Menampilkan <strong>{invoices.length}</strong> dari <strong>{totalCount}</strong> faktur
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                <button
    onClick={() => {
      setCurrentPage((p) => Math.max(1, p - 1));
      fetchData(Math.max(1, currentPage - 1));
    }}
    disabled={currentPage <= 1}
    style={{
      padding: "8px 18px",
      borderRadius: "10px",
      border: "1px solid #e2e8f0",
      background: currentPage <= 1 ? "#f1f5f9" : "white",
      color: currentPage <= 1 ? "#94a3b8" : "#1e293b",
      fontWeight: "700",
      cursor: currentPage <= 1 ? "not-allowed" : "pointer",
      fontSize: "13px",
      transition: "all 0.15s"
    }}
  >
                    ← Sebelumnya
                </button>
                <div style={{
    display: "flex",
    gap: "5px",
    alignItems: "center"
  }}>
                    {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
    let pageNum;
    if (totalPages <= 7) {
      pageNum = i + 1;
    } else if (currentPage <= 4) {
      pageNum = i + 1;
    } else if (currentPage >= totalPages - 3) {
      pageNum = totalPages - 6 + i;
    } else {
      pageNum = currentPage - 3 + i;
    }
    return <button
      key={pageNum}
      onClick={() => {
        setCurrentPage(pageNum);
        fetchData(pageNum);
      }}
      style={{
        width: "36px",
        height: "36px",
        borderRadius: "8px",
        border: "none",
        background: currentPage === pageNum ? "linear-gradient(135deg, #dc3545, #b91c1c)" : "#f8fafc",
        color: currentPage === pageNum ? "white" : "#64748b",
        fontWeight: "700",
        cursor: "pointer",
        fontSize: "13px",
        boxShadow: currentPage === pageNum ? "0 2px 8px rgba(220,53,69,0.3)" : "none",
        transition: "all 0.15s"
      }}
    >
                                {pageNum}
                            </button>;
  })}
                </div>
                <button
    onClick={() => {
      setCurrentPage((p) => Math.min(totalPages, p + 1));
      fetchData(Math.min(totalPages, currentPage + 1));
    }}
    disabled={currentPage >= totalPages}
    style={{
      padding: "8px 18px",
      borderRadius: "10px",
      border: "1px solid #e2e8f0",
      background: currentPage >= totalPages ? "#f1f5f9" : "white",
      color: currentPage >= totalPages ? "#94a3b8" : "#1e293b",
      fontWeight: "700",
      cursor: currentPage >= totalPages ? "not-allowed" : "pointer",
      fontSize: "13px",
      transition: "all 0.15s"
    }}
  >
                    Selanjutnya →
                </button>
            </div>
        </div>;
  if (loading) return <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "80vh", flexDirection: "column", gap: "15px" }}>
            <div style={{ width: "30px", height: "30px", border: "3px solid #f3f3f3", borderTop: "3px solid #dc3545", borderRadius: "50%", animation: "spin 1s linear infinite" }} />
            <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
            <div style={{ color: "#6c757d" }}>Memuat riwayat faktur...</div>
        </div>;
  return <div className="legacy-page" style={{ padding: isMobile ? "12px" : "30px", background: "#f8f9fa", minHeight: "100%", maxWidth: "100vw", boxSizing: "border-box", overflowX: "hidden" }}>
            <InvoiceImageTemplate inv={fullInvoiceData} settings={settings} />

            {previewImage && <div style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "rgba(15, 23, 42, 0.85)", display: "flex", justifyContent: "center", alignItems: "center", zIndex: 2e3, padding: "15px", flexDirection: "column", gap: "15px", backdropFilter: "blur(6px)" }}>
                    <div style={{ background: "white", padding: "20px", borderRadius: "24px", width: "100%", maxWidth: "460px", boxSizing: "border-box", position: "relative", display: "flex", flexDirection: "column", alignItems: "center", boxShadow: "0 25px 60px rgba(0,0,0,0.3)" }}>
                        <button onClick={() => setPreviewImage(null)} style={{ position: "absolute", right: "15px", top: "15px", border: "none", background: "#f1f5f9", color: "#64748b", cursor: "pointer", borderRadius: "50%", width: "32px", height: "32px", display: "flex", alignItems: "center", justifyContent: "center" }}><X size={18} /></button>
                        <h4 style={{ margin: "0 0 4px 0", fontWeight: "800", color: "#1e293b", fontSize: "17px" }}>Gambar Faktur</h4>
                        <p style={{ margin: "0 0 14px 0", fontSize: "12px", color: "#94a3b8", textAlign: "center" }}>Faktur No. <strong style={{ color: "#0d6efd" }}>{previewInvoiceNumber}</strong></p>
                        <div style={{ width: "100%", borderRadius: "14px", overflow: "hidden", border: "1px solid #e2e8f0", boxShadow: "0 4px 16px rgba(0,0,0,0.08)" }}>
                            <img src={previewImage} alt="Faktur" style={{ width: "100%", maxHeight: "55vh", objectFit: "contain", display: "block" }} />
                        </div>
                        <div style={{ display: "flex", flexDirection: "column", width: "100%", gap: "10px", marginTop: "16px" }}>
                            {
    /* Download / Save Button */
  }
                            <button
    onClick={handleSaveImage}
    style={{
      width: "100%",
      padding: "13px",
      background: "linear-gradient(135deg, #4f46e5, #7c3aed)",
      color: "white",
      border: "none",
      borderRadius: "12px",
      fontWeight: "bold",
      cursor: "pointer",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      gap: "8px",
      fontSize: "14px",
      boxShadow: "0 4px 12px rgba(79,70,229,0.3)"
    }}
  >
                                💾 Simpan / Unduh Gambar
                            </button>

                            {
    /* WhatsApp Share Button — always shown */
  }
                            <button
    onClick={handleShareWhatsApp}
    style={{
      width: "100%",
      padding: "13px",
      background: "linear-gradient(135deg, #25D366, #128C7E)",
      color: "white",
      border: "none",
      borderRadius: "12px",
      fontWeight: "bold",
      cursor: "pointer",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      gap: "8px",
      fontSize: "14px",
      boxShadow: "0 4px 12px rgba(37,211,102,0.3)"
    }}
  >
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" /></svg>
                                Kirim ke WhatsApp
                            </button>
                        </div>
                        <p style={{ margin: "12px 0 0 0", fontSize: "11px", color: "#cbd5e1", textAlign: "center" }}>Atau sentuh dan tahan (long-press) gambar di atas untuk menyimpan langsung</p>
                    </div>
                </div>}

            {
    /* Header Section */
  }
            <div style={{ display: "flex", flexDirection: isMobile ? "column" : "row", justifyContent: "space-between", alignItems: isMobile ? "stretch" : "center", marginBottom: "25px", gap: "15px" }}>
                <div>
                    <h2 style={{ fontSize: isMobile ? "22px" : "28px", fontWeight: "800", margin: 0, color: "#1e293b" }}>Riwayat Faktur</h2>
                    <p style={{ margin: "5px 0 0 0", color: "#64748b", fontSize: "14px" }}>Manajemen penagihan dan pembayaran pelanggan</p>
                </div>
                <button
    onClick={() => navigate("/invoices/create")}
    style={{
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      gap: "8px",
      background: "linear-gradient(135deg, #dc3545 0%, #b91c1c 100%)",
      color: "white",
      border: "none",
      padding: "12px 24px",
      borderRadius: "12px",
      cursor: "pointer",
      fontWeight: "bold",
      boxShadow: "0 4px 10px rgba(220, 53, 69, 0.2)",
      transition: "all 0.2s"
    }}
  >
                    <PlusCircle size={20} /> Buat Faktur Baru
                </button>
            </div>

            {
    /* Summary Section (Only when client filtered) */
  }
            {filterClient !== "ALL" && clientSummary && <div style={{ display: "grid", gridTemplateColumns: isMobile ? "1fr" : "1fr 1fr", gap: "15px", marginBottom: "25px" }}>
                    <div style={{ background: "white", padding: "20px", borderRadius: "18px", borderLeft: "6px solid #0d6efd", boxShadow: "0 4px 12px rgba(0,0,0,0.02)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <div>
                            <div style={{ color: "#64748b", fontSize: "11px", fontWeight: "700", textTransform: "uppercase", marginBottom: "5px" }}>Total Tunggakan</div>
                            <div style={{ fontSize: isMobile ? "20px" : "24px", fontWeight: "800", color: "#1e293b" }}>Rp {clientSummary.total_tunggakan.toLocaleString("id-ID")}</div>
                            <div style={{ fontSize: "11px", color: "#64748b", marginTop: "5px" }}>{clientSummary.unpaid_count} Faktur belum lunas</div>
                        </div>
                        <button onClick={() => {
    setMassalData({ ...massalData, client_id: Number(filterClient) });
    setShowMassal(true);
  }} style={{ background: "#0d6efd", color: "white", border: "none", padding: "10px 15px", borderRadius: "10px", fontWeight: "bold", cursor: "pointer", fontSize: "12px" }}>
                            Bayar Cepat
                        </button>
                    </div>
                    <div style={{ background: "white", padding: "20px", borderRadius: "18px", borderLeft: "6px solid #198754", boxShadow: "0 4px 12px rgba(0,0,0,0.02)" }}>
                        <div style={{ color: "#64748b", fontSize: "11px", fontWeight: "700", textTransform: "uppercase", marginBottom: "5px" }}>Saldo Titipan</div>
                        <div style={{ fontSize: isMobile ? "20px" : "24px", fontWeight: "800", color: "#198754" }}>Rp {clientSummary.saldo_borongan.toLocaleString("id-ID")}</div>
                        <div style={{ fontSize: "11px", color: "#64748b", marginTop: "5px" }}>Digunakan otomatis saat bayar faktur</div>
                    </div>
                </div>}

            {
    /* Filters & Search */
  }
            <div style={{ background: "white", padding: "15px", borderRadius: "18px", boxShadow: "0 2px 8px rgba(0,0,0,0.02)", marginBottom: "25px", display: "flex", flexDirection: "column", gap: "15px" }}>
                <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
                    <div style={{ flex: 1, minWidth: isMobile ? "100%" : "200px", position: "relative" }}>
                        <Search size={18} style={{ position: "absolute", left: "12px", top: "12px", color: "#94a3b8" }} />
                        <input
    type="text"
    placeholder="Cari No. Faktur atau Pelanggan..."
    value={searchTerm}
    onChange={(e) => setSearchTerm(e.target.value)}
    style={{ width: "100%", padding: "12px 12px 12px 40px", borderRadius: "10px", border: "1px solid #e2e8f0", outline: "none", fontSize: "14px" }}
  />
                    </div>
                    <div style={{ flex: 1, minWidth: isMobile ? "100%" : "180px" }}>
                        <select value={filterClient} onChange={(e) => setFilterClient(e.target.value)} style={{ width: "100%", padding: "12px", borderRadius: "10px", border: "1px solid #e2e8f0", outline: "none", fontSize: "14px", background: "white" }}>
                            <option value="ALL">Semua Pelanggan</option>
                            {(clients || []).map((c) => <option key={c.ID} value={c.ID}>{c.ClientName}</option>)}
                        </select>
                    </div>
                    <div style={{ flex: 1, minWidth: isMobile ? "100%" : "150px" }}>
                        <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)} style={{ width: "100%", padding: "12px", borderRadius: "10px", border: "1px solid #e2e8f0", outline: "none", fontSize: "14px", background: "white" }}>
                            <option value="ALL">Semua Status</option>
                            <option value="PAID">LUNAS</option>
                            <option value="UNPAID">BELUM BAYAR</option>
                            <option value="PARTIAL">SEBAGIAN</option>
                            <option value="OVERDUE">JATUH TEMPO</option>
                        </select>
                    </div>
                </div>
            </div>

            {isMobile ? (
    /* Mobile List View */
    <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                    {filteredInvoices.map((inv) => <div key={inv.ID} style={{ background: "white", borderRadius: "18px", padding: "18px", boxShadow: "0 4px 6px rgba(0,0,0,0.03)", border: "1px solid #f1f5f9" }}>
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "12px" }}>
                                <div>
                                    <div style={{ fontWeight: "800", color: "#0d6efd", fontSize: "15px" }}>{inv.Number}</div>
                                    <div style={{ fontWeight: "700", color: "#1e293b", fontSize: "14px", marginTop: "2px" }}>{inv.Client?.ClientName}</div>
                                </div>
                                <div style={{ textAlign: "right" }}>
                                    <div style={{ fontWeight: "800", fontSize: "16px", color: "#1e293b" }}>Rp {inv.Total.toLocaleString("id-ID")}</div>
                                    {inv.Status === "PARTIAL" && inv.PaidAmount > 0 && <div style={{ fontSize: "11px", marginTop: "3px" }}>
                                            <span style={{ color: "#198754" }}>✓ Rp {inv.PaidAmount.toLocaleString("id-ID")}</span>
                                            <br />
                                            <span style={{ color: "#dc3545", fontWeight: "700" }}>Sisa Rp {(inv.Total - inv.PaidAmount).toLocaleString("id-ID")}</span>
                                        </div>}
                                    <span style={{
      fontSize: "9px",
      fontWeight: "900",
      padding: "2px 8px",
      borderRadius: "6px",
      textTransform: "uppercase",
      display: "inline-block",
      marginTop: "5px",
      background: inv.Status === "PAID" ? "#dcfce7" : inv.Status === "PARTIAL" ? "#fef9c3" : inv.Status === "OVERDUE" ? "#fee2e2" : "#f1f5f9",
      color: inv.Status === "PAID" ? "#15803d" : inv.Status === "PARTIAL" ? "#854d0e" : inv.Status === "OVERDUE" ? "#b91c1c" : "#64748b"
    }}>
                        {inv.Status === "OVERDUE" ? "Jatuh Tempo" : inv.Status === "PARTIAL" ? "CICIL" : inv.Status}
                    </span>
                                </div>
                            </div>

                            <div style={{ display: "flex", gap: "15px", marginBottom: "15px", color: "#64748b", fontSize: "11px", background: "#f8fafc", padding: "10px", borderRadius: "10px" }}>
                                <div style={{ display: "flex", alignItems: "center", gap: "5px" }}><Calendar size={12} /> {new Date(inv.DateCreated).toLocaleDateString("id-ID")}</div>
                                {inv.DueDate && <div style={{ display: "flex", alignItems: "center", gap: "5px", color: "#dc3545" }}><AlertCircle size={12} /> Tempo: {new Date(inv.DueDate).toLocaleDateString("id-ID")}</div>}
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "repeat(5, 1fr)", gap: "8px" }}>
                                <button onClick={() => openPaymentModal(inv)} style={{ background: inv.Status === "PAID" ? "#4f46e5" : "#198754", color: "white", border: "none", padding: "10px", borderRadius: "10px", cursor: "pointer", display: "flex", justifyContent: "center" }} title={inv.Status === "PAID" ? "Lihat/Edit Pembayaran" : "Bayar / Edit Cicilan"}><DollarSign size={16} /></button>
                                <button onClick={() => openEditModal(inv.ID)} style={{ background: "#f1f5f9", color: "#64748b", border: "none", padding: "10px", borderRadius: "10px", cursor: "pointer", display: "flex", justifyContent: "center" }} title="Edit"><Edit size={16} /></button>
                                <button onClick={() => downloadJPG(inv.ID, inv.Number)} style={{ background: "#f1f5f9", color: "#64748b", border: "none", padding: "10px", borderRadius: "10px", cursor: "pointer", display: "flex", justifyContent: "center" }} title="JPG"><ImageIcon size={16} /></button>
                                <button onClick={() => downloadPDF(inv.ID, "pdf")} style={{ background: "#f1f5f9", color: "#64748b", border: "none", padding: "10px", borderRadius: "10px", cursor: "pointer", display: "flex", justifyContent: "center" }} title="PDF"><Download size={16} /></button>
                                <button onClick={() => downloadPDF(inv.ID, "surat-jalan")} style={{ background: "#f1f5f9", color: "#64748b", border: "none", padding: "10px", borderRadius: "10px", cursor: "pointer", display: "flex", justifyContent: "center" }} title="Surat Jalan"><FileText size={16} /></button>
                                <button onClick={() => handleDelete(inv.ID)} style={{ background: "#fee2e2", color: "#dc3545", border: "none", padding: "10px", borderRadius: "10px", cursor: "pointer", display: "flex", justifyContent: "center" }} title="Hapus"><Trash2 size={16} /></button>
                            </div>
                        </div>)}
                </div>
  ) : (
    /* Desktop Table View */
    <div style={{ background: "white", borderRadius: "20px", padding: "10px", boxShadow: "0 4px 12px rgba(0,0,0,0.04)", border: "1px solid #f1f5f9" }}>
                    <table style={{ width: "100%", borderCollapse: "separate", borderSpacing: "0 8px", textAlign: "left" }}>
                        <thead>
                        <tr style={{ color: "#64748b", fontSize: "11px", textTransform: "uppercase", letterSpacing: "1px" }}>
                            <th style={{ padding: "0 20px", fontWeight: "700" }}>No Faktur</th>
                            <th style={{ padding: "0 20px", fontWeight: "700" }}>Pelanggan</th>
                            <th style={{ padding: "0 20px", fontWeight: "700" }}>Tanggal & Tempo</th>
                            <th style={{ padding: "0 20px", fontWeight: "700" }}>Total Tagihan</th>
                            <th style={{ padding: "0 20px", fontWeight: "700", textAlign: "center" }}>Status</th>
                            <th style={{ padding: "0 20px", fontWeight: "700", textAlign: "center" }}>Aksi</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredInvoices.map((inv) => <tr key={inv.ID} style={{ background: "#ffffff" }}>
                                <td style={{ padding: "15px 20px", fontWeight: "700", color: "#0d6efd", borderBottom: "1px solid #f1f5f9" }}>{inv.Number}</td>
                                <td style={{ padding: "15px 20px", fontWeight: "600", color: "#334155", borderBottom: "1px solid #f1f5f9" }}>{inv.Client?.ClientName || "-"}</td>
                                <td style={{ padding: "15px 20px", fontSize: "12px", color: "#64748b", borderBottom: "1px solid #f1f5f9" }}>
                                    <div style={{ display: "flex", alignItems: "center", gap: "5px" }}><Calendar size={12} /> {new Date(inv.DateCreated).toLocaleDateString("id-ID")}</div>
                                    {inv.DueDate && <div style={{ display: "flex", alignItems: "center", gap: "5px", color: "#dc3545", marginTop: "3px" }}><AlertCircle size={12} /> {new Date(inv.DueDate).toLocaleDateString("id-ID")}</div>}
                                </td>
                                <td style={{ padding: "15px 20px", borderBottom: "1px solid #f1f5f9" }}>
                                    <div style={{ fontWeight: "800", color: "#1e293b" }}>Rp {inv.Total.toLocaleString("id-ID")}</div>
                                    {inv.Status === "PARTIAL" && inv.PaidAmount > 0 && <div style={{ marginTop: "4px", fontSize: "11px" }}>
                                            <span style={{ color: "#198754" }}>✓ Dibayar: Rp {inv.PaidAmount.toLocaleString("id-ID")}</span>
                                            <br />
                                            <span style={{ color: "#dc3545", fontWeight: "700" }}>Sisa: Rp {(inv.Total - inv.PaidAmount).toLocaleString("id-ID")}</span>
                                        </div>}
                                </td>
                                <td style={{ padding: "15px 20px", borderBottom: "1px solid #f1f5f9", textAlign: "center" }}>
                    <span style={{
      padding: "5px 12px",
      borderRadius: "8px",
      fontSize: "10px",
      fontWeight: "900",
      background: inv.Status === "PAID" ? "#dcfce7" : inv.Status === "PARTIAL" ? "#fef9c3" : inv.Status === "OVERDUE" ? "#fee2e2" : "#f1f5f9",
      color: inv.Status === "PAID" ? "#15803d" : inv.Status === "PARTIAL" ? "#854d0e" : inv.Status === "OVERDUE" ? "#b91c1c" : "#64748b"
    }}>
                        {inv.Status === "PARTIAL" ? "CICIL" : inv.Status}
                    </span>
                                </td>
                                <td style={{ padding: "15px 20px", borderBottom: "1px solid #f1f5f9" }}>
                                    <div style={{ display: "flex", gap: "6px", justifyContent: "center" }}>
                                        <button onClick={() => openPaymentModal(inv)} style={{ background: inv.Status === "PAID" ? "#4f46e5" : "#198754", color: "white", border: "none", padding: "8px", borderRadius: "8px", cursor: "pointer" }} title={inv.Status === "PAID" ? "Lihat/Edit Pembayaran" : "Bayar / Edit Cicilan"}><DollarSign size={14} /></button>
                                        <button onClick={() => openEditModal(inv.ID)} style={{ background: "#f1f5f9", color: "#64748b", border: "none", padding: "8px", borderRadius: "8px", cursor: "pointer" }} title="Edit"><Edit size={14} /></button>
                                        <button onClick={() => downloadJPG(inv.ID, inv.Number)} style={{ background: "#f1f5f9", color: "#64748b", border: "none", padding: "8px", borderRadius: "8px", cursor: "pointer" }}><ImageIcon size={14} /></button>
                                        <button onClick={() => downloadPDF(inv.ID, "pdf")} style={{ background: "#f1f5f9", color: "#64748b", border: "none", padding: "8px", borderRadius: "8px", cursor: "pointer" }}><Download size={14} /></button>
                                        <button onClick={() => downloadPDF(inv.ID, "surat-jalan")} style={{ background: "#f1f5f9", color: "#64748b", border: "none", padding: "8px", borderRadius: "8px", cursor: "pointer" }}><FileText size={14} /></button>
                                        <button onClick={() => handleDelete(inv.ID)} style={{ background: "#fee2e2", color: "#dc3545", border: "none", padding: "8px", borderRadius: "8px", cursor: "pointer" }}><Trash2 size={14} /></button>


                                    </div>
                                </td>
                            </tr>)}
                        </tbody>
                    </table>
                </div>
  )}

            {filteredInvoices.length === 0 && !loading && <div style={{ textAlign: "center", padding: "50px", background: "white", borderRadius: "24px", color: "#64748b" }}>
                    <FileText size={48} style={{ margin: "0 auto 15px", opacity: 0.2 }} />
                    <div>Belum ada faktur yang ditemukan.</div>
                </div>}

            {
    /* Pagination Navigation */
  }
            {totalCount > 0 && <PaginationNav />}

            {
    /* MODALS (Simplified for brevity, but made responsive) */
  }
            {showMassal && <div style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "rgba(15, 23, 42, 0.7)", display: "flex", justifyContent: "center", alignItems: "center", zIndex: 1200, padding: "15px", backdropFilter: "blur(4px)" }}>
                    <div style={{ background: "white", padding: "30px", borderRadius: "24px", width: "100%", maxWidth: "400px", position: "relative" }}>
                        <button onClick={() => setShowMassal(false)} style={{ position: "absolute", right: "20px", top: "20px", border: "none", background: "none", color: "#94a3b8", cursor: "pointer" }}><X size={24} /></button>
                        <h3 style={{ marginBottom: "25px", fontWeight: "800", color: "#1e293b" }}>Pembayaran Cepat</h3>
                        <form onSubmit={handleMassal}>
                            <div style={{ marginBottom: "18px" }}>
                                <label style={{ display: "block", marginBottom: "8px", fontSize: "13px", fontWeight: "600" }}>Klien</label>
                                <select disabled value={massalData.client_id} style={{ width: "100%", padding: "12px", border: "1px solid #e2e8f0", borderRadius: "12px", background: "#f8fafc" }}>
                                    {clients.map((c) => <option key={c.ID} value={c.ID}>{c.ClientName}</option>)}
                                </select>
                            </div>
                            <div style={{ marginBottom: "25px" }}>
                                <label style={{ display: "block", marginBottom: "8px", fontSize: "13px", fontWeight: "600" }}>Nominal Uang (Rp)</label>
                                <input type="number" required value={massalData.nominal} onChange={(e) => setMassalData({ ...massalData, nominal: Number(e.target.value) })} style={{ width: "100%", padding: "12px", border: "1px solid #e2e8f0", borderRadius: "12px", outline: "none", fontSize: "16px", fontWeight: "800" }} />
                            </div>
                            <button type="submit" style={{ width: "100%", padding: "14px", background: "#0d6efd", color: "white", border: "none", borderRadius: "12px", fontWeight: "bold", cursor: "pointer" }}>Proses Pembayaran</button>
                        </form>
                    </div>
                </div>}

            {
    /* Payment Detail Modal — Edit/Hapus/Tambah Cicilan */
  }
            {showPaymentModal && payModalInvoice && <div style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "rgba(15, 23, 42, 0.8)", display: "flex", justifyContent: "center", alignItems: "center", zIndex: 1200, padding: "15px", backdropFilter: "blur(6px)" }}>
                    <div style={{ background: "white", borderRadius: "24px", width: "100%", maxWidth: "520px", maxHeight: "90vh", overflowY: "auto", position: "relative", boxShadow: "0 30px 80px rgba(0,0,0,0.25)" }}>
                        {
    /* Header */
  }
                        <div style={{ padding: "24px 24px 0", borderBottom: "1px solid #f1f5f9", paddingBottom: "16px", position: "sticky", top: 0, background: "white", borderRadius: "24px 24px 0 0", zIndex: 1 }}>
                            <button onClick={() => {
    setShowPaymentModal(false);
    setPayModalEditId(null);
  }} style={{ position: "absolute", right: "20px", top: "20px", border: "none", background: "#f1f5f9", color: "#64748b", cursor: "pointer", borderRadius: "50%", width: "32px", height: "32px", display: "flex", alignItems: "center", justifyContent: "center" }}><X size={18} /></button>
                            <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "4px" }}>
                                <div style={{ background: "linear-gradient(135deg, #4f46e5, #7c3aed)", borderRadius: "10px", padding: "8px", display: "flex" }}><DollarSign size={18} color="white" /></div>
                                <div>
                                    <div style={{ fontWeight: "800", fontSize: "17px", color: "#1e293b" }}>Riwayat &amp; Edit Pembayaran</div>
                                    <div style={{ fontSize: "12px", color: "#64748b" }}>Faktur <strong style={{ color: "#0d6efd" }}>{payModalInvoice.Number}</strong> · {payModalInvoice.Client?.ClientName}</div>
                                </div>
                            </div>
                            {
    /* Summary bar */
  }
                            <div style={{ display: "flex", gap: "10px", marginTop: "12px", flexWrap: "wrap" }}>
                                <div style={{ flex: 1, minWidth: "120px", background: "#f8fafc", borderRadius: "10px", padding: "10px 14px" }}>
                                    <div style={{ fontSize: "10px", fontWeight: "700", color: "#94a3b8", textTransform: "uppercase" }}>Total Faktur</div>
                                    <div style={{ fontSize: "15px", fontWeight: "800", color: "#1e293b" }}>Rp {(payModalInvoice.Total || 0).toLocaleString("id-ID")}</div>
                                </div>
                                <div style={{ flex: 1, minWidth: "120px", background: "#f0fdf4", borderRadius: "10px", padding: "10px 14px" }}>
                                    <div style={{ fontSize: "10px", fontWeight: "700", color: "#16a34a", textTransform: "uppercase" }}>Dibayar</div>
                                    <div style={{ fontSize: "15px", fontWeight: "800", color: "#15803d" }}>Rp {payModalPayments.reduce((s, p) => s + p.PaymentAmount, 0).toLocaleString("id-ID")}</div>
                                </div>
                                <div style={{ flex: 1, minWidth: "120px", background: payModalInvoice.Total - payModalPayments.reduce((s, p) => s + p.PaymentAmount, 0) <= 0 ? "#f0fdf4" : "#fef2f2", borderRadius: "10px", padding: "10px 14px" }}>
                                    <div style={{ fontSize: "10px", fontWeight: "700", color: payModalInvoice.Total - payModalPayments.reduce((s, p) => s + p.PaymentAmount, 0) <= 0 ? "#16a34a" : "#dc2626", textTransform: "uppercase" }}>Sisa</div>
                                    <div style={{ fontSize: "15px", fontWeight: "800", color: payModalInvoice.Total - payModalPayments.reduce((s, p) => s + p.PaymentAmount, 0) <= 0 ? "#15803d" : "#b91c1c" }}>
                                        {payModalInvoice.Total - payModalPayments.reduce((s, p) => s + p.PaymentAmount, 0) <= 0 ? "\u2713 LUNAS" : `Rp ${(payModalInvoice.Total - payModalPayments.reduce((s, p) => s + p.PaymentAmount, 0)).toLocaleString("id-ID")}`}
                                    </div>
                                </div>
                            </div>
                        </div>

                        {
    /* Payment list */
  }
                        <div style={{ padding: "16px 24px" }}>
                            <div style={{ fontSize: "11px", fontWeight: "700", color: "#94a3b8", textTransform: "uppercase", letterSpacing: "1px", marginBottom: "12px" }}>Riwayat Cicilan</div>
                            {payModalLoading ? <div style={{ textAlign: "center", padding: "30px", color: "#94a3b8" }}>Memuat...</div> : payModalPayments.length === 0 ? <div style={{ textAlign: "center", padding: "30px", color: "#94a3b8", fontSize: "14px" }}>Belum ada catatan pembayaran.</div> : payModalPayments.map((pay) => <div key={pay.ID} style={{ marginBottom: "10px", borderRadius: "14px", border: "1px solid #e2e8f0", overflow: "hidden" }}>
                                        {
    /* Row utama */
  }
                                        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "12px 14px", background: payModalEditId === pay.ID ? "#f8f5ff" : "#fafafa", gap: "10px", flexWrap: "wrap" }}>
                                            <div style={{ flex: 1, minWidth: "120px" }}>
                                                <div style={{ fontWeight: "800", fontSize: "15px", color: "#1e293b" }}>Rp {Number(pay.PaymentAmount).toLocaleString("id-ID")}</div>
                                                <div style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>
                                                    {new Date(pay.PaymentDate).toLocaleDateString("id-ID", { day: "2-digit", month: "short", year: "numeric" })} · {pay.PaymentMethod}
                                                </div>
                                            </div>
                                            <div style={{ display: "flex", gap: "6px" }}>
                                                <button
    onClick={() => {
      if (payModalEditId === pay.ID) {
        setPayModalEditId(null);
      } else {
        setPayModalEditId(pay.ID);
        setPayModalEditForm({
          nominal: pay.PaymentAmount,
          tanggal: new Date(pay.PaymentDate).toISOString().split("T")[0],
          metode: pay.PaymentMethod
        });
      }
    }}
    style={{ background: payModalEditId === pay.ID ? "#ede9fe" : "#f1f5f9", color: payModalEditId === pay.ID ? "#7c3aed" : "#64748b", border: "none", padding: "7px 12px", borderRadius: "8px", cursor: "pointer", fontSize: "12px", fontWeight: "700", display: "flex", alignItems: "center", gap: "4px" }}
  >
                                                    ✏️ {payModalEditId === pay.ID ? "Batal" : "Edit"}
                                                </button>
                                                <button
    onClick={() => handlePayModalDelete(pay.ID)}
    style={{ background: "#fee2e2", color: "#dc2626", border: "none", padding: "7px 12px", borderRadius: "8px", cursor: "pointer", fontSize: "12px", fontWeight: "700", display: "flex", alignItems: "center", gap: "4px" }}
  >
                                                    🗑️ Hapus
                                                </button>
                                            </div>
                                        </div>
                                        {
    /* Edit form inline */
  }
                                        {payModalEditId === pay.ID && <div style={{ padding: "14px", background: "#f5f3ff", borderTop: "1px solid #e2e8f0" }}>
                                                <div style={{ display: "grid", gridTemplateColumns: isMobile ? "1fr" : "1fr 1fr 1fr", gap: "10px", marginBottom: "12px" }}>
                                                    <div>
                                                        <label style={{ display: "block", fontSize: "10px", fontWeight: "700", color: "#7c3aed", marginBottom: "4px", textTransform: "uppercase" }}>Nominal Baru (Rp)</label>
                                                        <input
    type="number"
    value={payModalEditForm.nominal === 0 ? "" : payModalEditForm.nominal}
    onChange={(e) => setPayModalEditForm({ ...payModalEditForm, nominal: e.target.value === "" ? 0 : Number(e.target.value) })}
    style={{ width: "100%", padding: "10px", border: "2px solid #7c3aed", borderRadius: "8px", fontSize: "14px", fontWeight: "700", outline: "none", boxSizing: "border-box" }}
  />
                                                    </div>
                                                    <div>
                                                        <label style={{ display: "block", fontSize: "10px", fontWeight: "700", color: "#7c3aed", marginBottom: "4px", textTransform: "uppercase" }}>Tanggal</label>
                                                        <input
    type="date"
    value={payModalEditForm.tanggal}
    onChange={(e) => setPayModalEditForm({ ...payModalEditForm, tanggal: e.target.value })}
    style={{ width: "100%", padding: "10px", border: "2px solid #e2e8f0", borderRadius: "8px", outline: "none", boxSizing: "border-box" }}
  />
                                                    </div>
                                                    <div>
                                                        <label style={{ display: "block", fontSize: "10px", fontWeight: "700", color: "#7c3aed", marginBottom: "4px", textTransform: "uppercase" }}>Metode</label>
                                                        <select
    value={payModalEditForm.metode}
    onChange={(e) => setPayModalEditForm({ ...payModalEditForm, metode: e.target.value })}
    style={{ width: "100%", padding: "10px", border: "2px solid #e2e8f0", borderRadius: "8px", outline: "none", boxSizing: "border-box", background: "white" }}
  >
                                                            <option value="TRANSFER">TRANSFER</option>
                                                            <option value="CASH">CASH</option>
                                                            <option value="DEBIT">DEBIT</option>
                                                        </select>
                                                    </div>
                                                </div>
                                                <div style={{ background: "#ede9fe", borderRadius: "8px", padding: "8px 12px", fontSize: "11px", color: "#5b21b6", marginBottom: "12px", lineHeight: "1.5" }}>
                                                    ⚠️ <strong>Perubahan ini akan otomatis memperbarui data di Kas Keuangan</strong> dan menghitung ulang status faktur (LUNAS / CICIL / BELUM BAYAR).
                                                </div>
                                                <button
    onClick={() => handlePayModalEdit(pay.ID)}
    style={{ width: "100%", padding: "11px", background: "linear-gradient(135deg, #4f46e5, #7c3aed)", color: "white", border: "none", borderRadius: "10px", fontWeight: "700", cursor: "pointer", fontSize: "14px" }}
  >
                                                    ✅ Simpan Perubahan
                                                </button>
                                            </div>}
                                    </div>)}

                            {
    /* Tambah Cicilan Baru */
  }
                            <div style={{ marginTop: "16px" }}>
                                {!payModalAddVisible ? <button
    onClick={() => setPayModalAddVisible(true)}
    style={{ width: "100%", padding: "12px", background: "white", color: "#198754", border: "2px dashed #198754", borderRadius: "12px", cursor: "pointer", fontWeight: "700", fontSize: "14px" }}
  >
                                        + Tambah Cicilan Baru
                                    </button> : <div style={{ background: "#f0fdf4", borderRadius: "14px", border: "1px solid #bbf7d0", padding: "16px" }}>
                                        <div style={{ fontSize: "12px", fontWeight: "700", color: "#15803d", marginBottom: "12px", textTransform: "uppercase", letterSpacing: "0.5px" }}>➕ Tambah Pembayaran Baru</div>
                                        <form onSubmit={handlePayModalAdd}>
                                            <div style={{ display: "grid", gridTemplateColumns: isMobile ? "1fr" : "1fr 1fr 1fr", gap: "10px", marginBottom: "12px" }}>
                                                <div>
                                                    <label style={{ display: "block", fontSize: "10px", fontWeight: "700", color: "#15803d", marginBottom: "4px", textTransform: "uppercase" }}>Nominal (Rp)</label>
                                                    <input
    type="number"
    required
    value={payModalAddForm.nominal === 0 ? "" : payModalAddForm.nominal}
    onChange={(e) => setPayModalAddForm({ ...payModalAddForm, nominal: e.target.value === "" ? 0 : Number(e.target.value) })}
    style={{ width: "100%", padding: "10px", border: "2px solid #16a34a", borderRadius: "8px", fontSize: "14px", fontWeight: "700", outline: "none", boxSizing: "border-box" }}
  />
                                                </div>
                                                <div>
                                                    <label style={{ display: "block", fontSize: "10px", fontWeight: "700", color: "#15803d", marginBottom: "4px", textTransform: "uppercase" }}>Tanggal</label>
                                                    <input
    type="date"
    required
    value={payModalAddForm.tanggal}
    onChange={(e) => setPayModalAddForm({ ...payModalAddForm, tanggal: e.target.value })}
    style={{ width: "100%", padding: "10px", border: "2px solid #e2e8f0", borderRadius: "8px", outline: "none", boxSizing: "border-box" }}
  />
                                                </div>
                                                <div>
                                                    <label style={{ display: "block", fontSize: "10px", fontWeight: "700", color: "#15803d", marginBottom: "4px", textTransform: "uppercase" }}>Metode</label>
                                                    <select
    value={payModalAddForm.metode}
    onChange={(e) => setPayModalAddForm({ ...payModalAddForm, metode: e.target.value })}
    style={{ width: "100%", padding: "10px", border: "2px solid #e2e8f0", borderRadius: "8px", outline: "none", boxSizing: "border-box", background: "white" }}
  >
                                                        <option value="TRANSFER">TRANSFER</option>
                                                        <option value="CASH">CASH</option>
                                                        <option value="DEBIT">DEBIT</option>
                                                    </select>
                                                </div>
                                            </div>
                                            <div style={{ display: "flex", gap: "10px" }}>
                                                <button type="button" onClick={() => setPayModalAddVisible(false)} style={{ flex: 1, padding: "11px", background: "#f1f5f9", color: "#64748b", border: "none", borderRadius: "10px", fontWeight: "700", cursor: "pointer" }}>Batal</button>
                                                <button type="submit" style={{ flex: 2, padding: "11px", background: "linear-gradient(135deg, #16a34a, #15803d)", color: "white", border: "none", borderRadius: "10px", fontWeight: "700", cursor: "pointer" }}>✅ Simpan Cicilan</button>
                                            </div>
                                        </form>
                                    </div>}
                            </div>
                        </div>

                        {
    /* Footer */
  }
                        <div style={{ padding: "16px 24px", borderTop: "1px solid #f1f5f9" }}>
                            <button onClick={() => {
    setShowPaymentModal(false);
    setPayModalEditId(null);
  }} style={{ width: "100%", padding: "13px", background: "#f8fafc", color: "#64748b", border: "1px solid #e2e8f0", borderRadius: "12px", fontWeight: "700", cursor: "pointer", fontSize: "14px" }}>Tutup</button>
                        </div>
                    </div>
                </div>}

            {showEdit && <div style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "rgba(15, 23, 42, 0.7)", display: "flex", justifyContent: "center", alignItems: "center", zIndex: 1200, padding: "10px", backdropFilter: "blur(4px)" }}>
                    <div style={{ background: "white", padding: isMobile ? "20px" : "30px", borderRadius: "24px", width: "100%", maxWidth: "650px", maxHeight: "90vh", overflowY: "auto", position: "relative" }}>
                        <button onClick={() => setShowEdit(false)} style={{ position: "absolute", right: "20px", top: "20px", border: "none", background: "none", color: "#94a3b8", cursor: "pointer" }}><X size={24} /></button>
                        <h3 style={{ marginBottom: "25px", fontWeight: "800", color: "#1e293b" }}>Edit Produk Faktur</h3>
                        <form onSubmit={handleEdit}>
                            {editData.products.map((p, i) => <div key={i} style={{ display: "grid", gridTemplateColumns: isMobile ? "1fr" : "2fr 1fr 1fr 1.5fr 1.5fr 40px", gap: "10px", marginBottom: "15px", alignItems: "end", background: "#f8fafc", padding: "15px", borderRadius: "15px" }}>
                                    <div>
                                        <label style={{ display: "block", fontSize: "11px", fontWeight: "700", marginBottom: "5px" }}>Barang</label>
                                        <select required value={p.master_item_id} onChange={(e) => {
    const val = Number(e.target.value);
    const master = (masterProducts || []).find((m) => m.ID === val);
    const newProducts = [...editData.products];
    newProducts[i].master_item_id = val;
    if (master) newProducts[i].custom_price = master.Price;
    setEditData({ ...editData, products: newProducts });
  }} style={{ width: "100%", padding: "10px", border: "1px solid #e2e8f0", borderRadius: "10px", fontSize: "13px" }}>
                                            <option value={0}>-- Pilih --</option>
                                            {(masterProducts || []).map((mp) => <option key={mp.ID} value={mp.ID}>{mp.Title}</option>)}
                                        </select>
                                    </div>
                                    <div style={{ display: "flex", gap: "8px", gridColumn: isMobile ? "span 1" : "span 2" }}>
                                        <div style={{ flex: 1 }}>
                                            <label style={{ display: "block", fontSize: "11px", fontWeight: "700", marginBottom: "5px" }}>Lusin</label>
                                            <input type="number" step="any" required value={p.jumlah_lusin} onChange={(e) => {
    const newProducts = [...editData.products];
    newProducts[i].jumlah_lusin = Number(e.target.value);
    setEditData({ ...editData, products: newProducts });
  }} style={{ width: "100%", padding: "10px", border: "1px solid #e2e8f0", borderRadius: "10px" }} />
                                        </div>
                                        <div style={{ flex: 1 }}>
                                            <label style={{ display: "block", fontSize: "11px", fontWeight: "700", marginBottom: "5px" }}>Qty</label>
                                            <input type="number" step="any" required value={p.quantity} onChange={(e) => {
    const newProducts = [...editData.products];
    newProducts[i].quantity = Number(e.target.value);
    setEditData({ ...editData, products: newProducts });
  }} style={{ width: "100%", padding: "10px", border: "1px solid #e2e8f0", borderRadius: "10px" }} />
                                        </div>
                                    </div>
                                    <div style={{ flex: 1 }}>
                                        <label style={{ display: "block", fontSize: "11px", fontWeight: "700", marginBottom: "5px" }}>Harga Jual</label>
                                        <input type="number" required value={p.custom_price} onChange={(e) => {
    const newProducts = [...editData.products];
    newProducts[i].custom_price = Number(e.target.value);
    setEditData({ ...editData, products: newProducts });
  }} style={{ width: "100%", padding: "10px", border: "1px solid #e2e8f0", borderRadius: "10px" }} />
                                    </div>
                                    <div style={{ flex: 1 }}>
                                        <label style={{ display: "flex", alignItems: "center", fontSize: "11px", fontWeight: "700", marginBottom: "5px", gap: "5px" }}>
                                            <input type="checkbox" checked={p.is_khusus} onChange={(e) => {
    const newProducts = [...editData.products];
    newProducts[i].is_khusus = e.target.checked;
    if (e.target.checked) {
      const master = (masterProducts || []).find((m) => m.ID === p.master_item_id);
      const masterPrice = master ? master.Price : 0;
      const computedModal = Math.max(0, masterPrice * Number(p.jumlah_lusin) * Number(p.quantity) - Number(p.custom_price) * Number(p.jumlah_lusin) * Number(p.quantity));
      newProducts[i].harga_beli = computedModal;
    } else {
      newProducts[i].harga_beli = 0;
    }
    setEditData({ ...editData, products: newProducts });
  }} />
                                            Khusus
                                        </label>
                                        {p.is_khusus && <div>
                                                <input type="number" placeholder="Modal Beli" required value={p.harga_beli || ""} onChange={(e) => {
    const newProducts = [...editData.products];
    newProducts[i].harga_beli = Number(e.target.value);
    setEditData({ ...editData, products: newProducts });
  }} style={{ width: "100%", padding: "8px", border: "1px solid #fab005", borderRadius: "8px", marginTop: "5px", fontSize: "12px" }} />
                                                {(() => {
    const master = (masterProducts || []).find((m) => m.ID === p.master_item_id);
    const masterPrice = master ? master.Price : 0;
    const computedModal = Math.max(0, masterPrice * Number(p.jumlah_lusin) * Number(p.quantity) - Number(p.custom_price) * Number(p.jumlah_lusin) * Number(p.quantity));
    return <div style={{ fontSize: "10px", color: "#856404", marginTop: "4px" }}>
                                                            Saran: Rp {computedModal.toLocaleString("id-ID")}
                                                        </div>;
  })()}
                                            </div>}
                                    </div>
                                    <button type="button" onClick={() => {
    const newProducts = editData.products.filter((_, idx) => idx !== i);
    setEditData({ ...editData, products: newProducts });
  }} style={{ color: "#dc3545", border: "none", background: "none", padding: "10px", cursor: "pointer", display: "flex", justifyContent: "center" }}><Trash2 size={20} /></button>
                                </div>)}
                            <button type="button" onClick={() => setEditData({ ...editData, products: [...editData.products, { master_item_id: 0, quantity: 1, jumlah_lusin: 1, custom_price: 0, is_khusus: false, harga_beli: 0 }] })} style={{ background: "white", color: "#0d6efd", border: "2px dashed #0d6efd", padding: "12px", borderRadius: "12px", cursor: "pointer", width: "100%", fontWeight: "bold", marginBottom: "25px" }}>
                                + Tambah Item Barang
                            </button>

                            <div style={{ display: "flex", gap: "12px" }}>
                                <button type="button" onClick={() => setShowEdit(false)} style={{ flex: 1, padding: "14px", background: "#f1f5f9", color: "#64748b", border: "none", borderRadius: "12px", fontWeight: "bold", cursor: "pointer" }}>Batal</button>
                                <button type="submit" style={{ flex: 2, padding: "14px", background: "#0d6efd", color: "white", border: "none", borderRadius: "12px", fontWeight: "bold", cursor: "pointer" }}>Simpan Perubahan</button>
                            </div>
                        </form>
                    </div>
                </div>}
        </div>;
};
export default Invoices;
