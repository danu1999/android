const BASE_URL = window.location.origin;
const WS_PROTOCOL = window.location.protocol === "https:" ? "wss:" : "ws:";
const WS_URL = `${WS_PROTOCOL}//${window.location.host}/ws`;

// Global State
let products = [];
let cart = [];
let employees = [];
let attendanceLogs = [];
let qrPollInterval = null;
let wsConn = null;

// BMP (B2B Factory) Global State
let invoices = [];
let clients = [];
let bmpProducts = [];
let printSettings = null;
let currentInvoice = null;

// Auth Check & Initialize
document.addEventListener("DOMContentLoaded", () => {
    checkAuthentication();
    setupNavigation();
    setupPOSListeners();
    setupTabListeners();
    setupInvoiceListeners();
    setupMarginOutletListeners();
});

function checkAuthentication() {
    const tenantId = localStorage.getItem("tenantId");
    const email = localStorage.getItem("email");

    if (!tenantId || !email) {
        showLoginOverlay();
    } else {
        hideLoginOverlay();
        initializeDashboard();
    }
}

function showLoginOverlay() {
    document.getElementById("login-overlay").style.display = "flex";
    document.getElementById("app-container").style.display = "none";
    generateQRLogin();
}

function hideLoginOverlay() {
    document.getElementById("login-overlay").style.display = "none";
    document.getElementById("app-container").style.display = "flex";
}

// QR Code Authentication Logic
async function generateQRLogin() {
    const qrHolder = document.getElementById("qr-code-holder");
    const qrLoading = document.getElementById("qr-loading");
    const loginStatus = document.getElementById("login-status");

    qrHolder.style.display = "none";
    qrLoading.style.display = "block";
    loginStatus.innerText = "Membuat QR Code...";
    loginStatus.className = "status-badge pending";

    // Stop any existing poll interval before starting a new one
    if (qrPollInterval) {
        clearInterval(qrPollInterval);
        qrPollInterval = null;
    }

    try {
        const response = await fetch(`${BASE_URL}/api/auth/qr-session`);
        if (!response.ok) throw new Error("Gagal mengambil session ID dari server");
        
        const data = await response.json();
        const sessionId = data.sessionId;

        // Clear previous QR Code if exists
        qrHolder.innerHTML = "";

        // Generate QR Code locally using qrcodejs
        new QRCode(qrHolder, {
            text: sessionId,
            width: 200,
            height: 200,
            colorDark : "#0B0F19",
            colorLight : "#ffffff",
            correctLevel : QRCode.CorrectLevel.H
        });

        qrLoading.style.display = "none";
        qrHolder.style.display = "block";
        loginStatus.innerText = "Menunggu otorisasi dari Android...";
        loginStatus.className = "status-badge pending";

        // Start polling for auth confirmation
        startQrPolling(sessionId);

    } catch (error) {
        console.error("QR Code Error:", error);
        qrLoading.style.display = "none";
        loginStatus.innerText = "Gagal membuat QR Code. Segarkan halaman.";
        loginStatus.className = "status-badge error";
    }
}

function startQrPolling(sessionId) {
    if (qrPollInterval) clearInterval(qrPollInterval);

    // Auto-refresh QR after 4.5 minutes (sessions expire after 5 minutes)
    const qrAutoRefreshTimeout = setTimeout(() => {
        console.log("[QR] Session nearing expiry, auto-refreshing QR code...");
        clearInterval(qrPollInterval);
        generateQRLogin();
    }, 4.5 * 60 * 1000);

    qrPollInterval = setInterval(async () => {
        try {
            const response = await fetch(`${BASE_URL}/api/auth/qr-check?sessionId=${sessionId}`);
            if (!response.ok) return;

            const session = await response.json();

            if (session.status === "authorized") {
                clearInterval(qrPollInterval);
                clearTimeout(qrAutoRefreshTimeout);
                
                // Save credentials in localStorage
                localStorage.setItem("tenantId", session.tenantId || "default_tenant");
                localStorage.setItem("email", session.email || "user@posbah.com");
                localStorage.setItem("role", session.role || "OWNER");
                localStorage.setItem("isPremium", session.isPremium ? "true" : "false");
                localStorage.setItem("userName", session.name || "Pengguna POSBah");
                localStorage.setItem("businessMode", session.businessMode || "FNB");

                console.log("[QR] Login berhasil! tenantId:", session.tenantId, "email:", session.email);

                document.getElementById("login-status").innerText = "Login berhasil! Memuat dashboard...";
                document.getElementById("login-status").className = "status-badge success";

                setTimeout(() => {
                    hideLoginOverlay();
                    initializeDashboard();
                }, 800);

            } else if (session.status === "expired") {
                // Session expired, auto-regenerate
                clearInterval(qrPollInterval);
                clearTimeout(qrAutoRefreshTimeout);
                console.log("[QR] Session expired, regenerating QR...");
                generateQRLogin();
            }
            // "pending" — do nothing, keep polling

        } catch (error) {
            console.error("Polling Error:", error);
        }
    }, 2000);
}

// Initial Data Initialization
function initializeDashboard() {
    // Set UI badges
    const email = localStorage.getItem("email");
    const tenantId = localStorage.getItem("tenantId");
    const isPremium = localStorage.getItem("isPremium") === "true";
    const userName = localStorage.getItem("userName") || email;
    const businessMode = localStorage.getItem("businessMode");

    document.getElementById("user-email").innerText = email;
    document.getElementById("tenant-name-badge").innerText = `Toko: ${tenantId.toUpperCase()}`;
    document.getElementById("stat-account-mode").innerText = isPremium ? "PREMIUM ACCOUNT" : "DEMO ACCOUNT (2 HARI)";
    
    const modeBadge = document.getElementById("stat-account-mode");
    if (isPremium) {
        modeBadge.className = "stat-val text-success";
    } else {
        modeBadge.className = "stat-val text-warning";
    }

    // Set today's date
    const now = new Date();
    const dateOptions = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    const dateEl = document.getElementById("dashboard-date");
    if (dateEl) dateEl.innerText = now.toLocaleDateString("id-ID", dateOptions);

    // Handle BMP menu visibility
    const invoicesMenu = document.getElementById("invoices-menu-item");
    if (invoicesMenu) {
        if (businessMode === "BMP") {
            invoicesMenu.style.display = "block";
        } else {
            invoicesMenu.style.display = "none";
        }
    }

    // Handle Margin Outlet visibility (Owner Only)
    const role = localStorage.getItem("role") || "KASIR";
    const marginMenu = document.getElementById("margin-outlet-menu-item");
    if (marginMenu) {
        if (role.toUpperCase() === "OWNER") {
            marginMenu.style.display = "block";
        } else {
            marginMenu.style.display = "none";
        }
    }

    // Refresh layout data
    refreshData();

    // Start Websocket
    connectWebSocket();
}

// Fetch all database tables
async function refreshData() {
    const tenantId = localStorage.getItem("tenantId");
    const email = localStorage.getItem("email");
    const businessMode = localStorage.getItem("businessMode");
    const headers = {
        "x-tenant-id": tenantId || "",
        "x-user-email": email || ""
    };

    try {
        // 1. Fetch transactions to calculate revenue & total
        const resTx = await fetch(`${BASE_URL}/api/sync/transactions?tenantId=eq.${tenantId}`, { headers });
        const transactions = resTx.ok ? await resTx.json() : [];

        // 2. Fetch products
        const resProducts = await fetch(`${BASE_URL}/api/sync/products?tenantId=eq.${tenantId}`, { headers });
        products = resProducts.ok ? await resProducts.json() : [];

        // 3. Fetch raw materials (bmp_master_products)
        const resBahan = await fetch(`${BASE_URL}/api/sync/bmp_master_products?tenantId=eq.${tenantId}`, { headers });
        const bahanBaku = resBahan.ok ? await resBahan.json() : [];

        // 4. Fetch employees
        const resEmp = await fetch(`${BASE_URL}/api/sync/bmp_employees?tenantId=eq.${tenantId}`, { headers });
        employees = resEmp.ok ? await resEmp.json() : [];

        // 5. Fetch attendance logs
        const resAttendance = await fetch(`${BASE_URL}/api/sync/bmp_attendance_logs`, { headers });
        attendanceLogs = resAttendance.ok ? await resAttendance.json() : [];

        // Fetch Print Settings
        try {
            const resPrint = await fetch(`${BASE_URL}/api/sync/print_settings?tenantId=eq.${tenantId}`, { headers });
            if (resPrint.ok) {
                const printData = await resPrint.json();
                printSettings = printData.length > 0 ? printData[0] : null;
            }
        } catch (pe) {
            console.error("Gagal menarik print settings:", pe);
        }

        // Fetch BMP specific data if mode is BMP
        if (businessMode === "BMP") {
            try {
                const resInvoices = await fetch(`${BASE_URL}/api/sync/bmp_invoices?tenantId=eq.${tenantId}`, { headers });
                invoices = resInvoices.ok ? await resInvoices.json() : [];

                const resClients = await fetch(`${BASE_URL}/api/sync/bmp_clients?tenantId=eq.${tenantId}`, { headers });
                clients = resClients.ok ? await resClients.json() : [];

                const resBmpProducts = await fetch(`${BASE_URL}/api/sync/bmp_products?tenantId=eq.${tenantId}`, { headers });
                bmpProducts = resBmpProducts.ok ? await resBmpProducts.json() : [];

                renderInvoicesList();
            } catch (be) {
                console.error("Gagal menarik data BMP:", be);
            }
        }

        // Render everything
        renderDashboardStats(transactions);
        renderPOSProducts();
        renderProductsCatalog(products);
        renderBahanBakuCatalog(bahanBaku);
        renderAttendanceLogs();

    } catch (error) {
        console.error("Gagal melakukan refresh data:", error);
    }
}

// Render Dashboard Data
function renderDashboardStats(transactions) {
    // 1. Calculate today's revenue (OMZET)
    let today = new Date().toDateString();
    let todayRevenue = 0;
    let todayTxCount = 0;

    transactions.forEach(tx => {
        let txDate = new Date(Number(tx.date)).toDateString();
        if (txDate === today) {
            todayRevenue += tx.total || 0;
            todayTxCount++;
        }
    });

    document.getElementById("stat-revenue").innerText = formatRupiah(todayRevenue);
    document.getElementById("stat-transactions").innerText = todayTxCount;

    // Render recent transactions list
    const tbody = document.getElementById("recent-transactions-list");
    tbody.innerHTML = "";

    if (transactions.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center">Belum ada transaksi</td></tr>`;
        return;
    }

    // Sort descending by date
    transactions.sort((a, b) => b.date - a.date);

    transactions.slice(0, 5).forEach(tx => {
        const tr = document.createElement("tr");
        const formattedDate = new Date(Number(tx.date)).toLocaleTimeString("id-ID", {hour: '2-digit', minute:'2-digit'});
        tr.innerHTML = `
            <td>${formattedDate}</td>
            <td><strong>${tx.receiptNumber}</strong></td>
            <td><span class="badge success">${tx.paymentMethod}</span></td>
            <td>${formatRupiah(tx.total)}</td>
            <td><button class="btn" onclick="printOfflineReceipt('${tx.receiptNumber}')"><i class="fa-solid fa-print"></i></button></td>
        `;
        tbody.appendChild(tr);
    });
}

// Render Products Grid on Cashier Screen
function renderPOSProducts(filterCategory = "ALL", searchQuery = "") {
    const grid = document.getElementById("pos-products-grid");
    grid.innerHTML = "";

    let filtered = products;

    if (filterCategory !== "ALL") {
        filtered = filtered.filter(p => p.category === filterCategory);
    }

    if (searchQuery.trim() !== "") {
        const query = searchQuery.toLowerCase();
        filtered = filtered.filter(p => p.name.toLowerCase().includes(query) || (p.barcode && p.barcode.toLowerCase().includes(query)));
    }

    if (filtered.length === 0) {
        grid.innerHTML = `<div class="empty-cart-message"><i class="fa-solid fa-box-open"></i><p>Produk tidak ditemukan</p></div>`;
        return;
    }

    filtered.forEach(p => {
        const card = document.createElement("div");
        card.className = "product-card";
        card.innerHTML = `
            <div>
                <img src="${p.image || 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&q=80&w=300'}" alt="${p.name}">
                <h4>${p.name}</h4>
                <div class="price">${formatRupiah(p.price)}</div>
            </div>
            <div class="stock">Stok: ${p.stock} ${p.unit}</div>
        `;
        card.addEventListener("click", () => addToCart(p));
        grid.appendChild(card);
    });
}

// Render Products Catalog table
function renderProductsCatalog(productsList) {
    const tbody = document.getElementById("catalog-products-list");
    tbody.innerHTML = "";

    if (productsList.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center">Tidak ada produk terdaftar</td></tr>`;
        return;
    }

    productsList.forEach(p => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${p.id}</td>
            <td><img class="catalog-img" src="${p.image || 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&q=80&w=100'}" alt="${p.name}"></td>
            <td><strong>${p.name}</strong></td>
            <td>${p.category || 'Umum'}</td>
            <td>${formatRupiah(p.price)}</td>
            <td>${p.stock} ${p.unit}</td>
            <td>
                <span class="badge ${p.stock > 10 ? 'success' : 'danger'}">
                    ${p.stock > 10 ? 'Stok Cukup' : 'Stok Kritis'}
                </span>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// Render Bahan Baku Catalog
function renderBahanBakuCatalog(bahanBaku) {
    const tbody = document.getElementById("catalog-bahanbaku-list");
    tbody.innerHTML = "";

    if (bahanBaku.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center">Tidak ada bahan baku terdaftar</td></tr>`;
        return;
    }

    bahanBaku.forEach(bb => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${bb.id}</td>
            <td><strong>${bb.title}</strong></td>
            <td>${bb.unit || 'Kg'}</td>
            <td>${formatRupiah(bb.price)}</td>
            <td>${bb.cycleTime} detik</td>
            <td>${bb.rejectRate}%</td>
        `;
        tbody.appendChild(tr);
    });
}

// Render Attendance Logs with Local TF-IDF logic
function renderAttendanceLogs() {
    const tbody = document.getElementById("attendance-logs-list");
    tbody.innerHTML = "";

    // Calculate active stats
    document.getElementById("stat-employee-count").innerText = employees.length;
    
    let today = new Date().toDateString();
    let todayLogs = attendanceLogs.filter(log => new Date(log.logTime).toDateString() === today);
    document.getElementById("stat-today-attendance").innerText = todayLogs.length;

    let totalLate = 0;
    todayLogs.forEach(log => totalLate += (log.lateMinutes || 0));
    document.getElementById("stat-today-late").innerText = `${totalLate} menit`;

    if (attendanceLogs.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center">Tidak ada data absensi hari ini</td></tr>`;
        return;
    }

    // Sort attendance logTime descending
    attendanceLogs.sort((a, b) => new Date(b.logTime) - new Date(a.logTime));

    attendanceLogs.slice(0, 10).forEach(log => {
        const tr = document.createElement("tr");

        // Find Employee Name
        const emp = employees.find(e => e.fingerprintPIN === log.employeePIN);
        const employeeName = emp ? emp.name : `Karyawan (${log.employeePIN})`;

        const logInTimeStr = new Date(log.logTime).toLocaleTimeString("id-ID", {hour: '2-digit', minute:'2-digit'});
        
        let logOutTimeStr = "-";
        if (log.checkOutTime) {
            logOutTimeStr = new Date(log.checkOutTime).toLocaleTimeString("id-ID", {hour: '2-digit', minute:'2-digit'});
        }

        let isCheckoutMissing = !log.checkOutTime;
        let checkInTime = new Date(log.logTime);
        let timeDiffHours = (new Date() - checkInTime) / (1000 * 60 * 60);

        let aiPredictionText = "-";
        let aiBadgeClass = "";
        
        if (isCheckoutMissing && timeDiffHours > 12) {
            aiPredictionText = "Lupa Scan Pulang (Deteksi AI)";
            aiBadgeClass = "ai-badge warning";
        } else if (log.checkOutTime) {
            aiPredictionText = "Absensi Lengkap";
            aiBadgeClass = "ai-badge";
        } else {
            aiPredictionText = "Sedang Bekerja";
            aiBadgeClass = "ai-badge";
        }

        tr.innerHTML = `
            <td><strong>${employeeName}</strong></td>
            <td>${logInTimeStr}</td>
            <td>${logOutTimeStr}</td>
            <td><span class="badge ${log.lateMinutes > 0 ? 'danger' : 'success'}">${log.lateMinutes} menit</span></td>
            <td>${log.alasan || 'Normal'}</td>
            <td><span class="${aiBadgeClass}">${aiPredictionText}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

// POS Cashier Cart Engine
function addToCart(product) {
    const existing = cart.find(item => item.id === product.id);

    if (existing) {
        existing.qty++;
    } else {
        cart.push({
            id: product.id,
            name: product.name,
            price: product.price,
            qty: 1
        });
    }

    updateCartUI();
}

function updateCartUI() {
    const container = document.getElementById("cart-items-container");
    container.innerHTML = "";

    if (cart.length === 0) {
        container.innerHTML = `
            <div class="empty-cart-message">
                <i class="fa-solid fa-basket-shopping"></i>
                <p>Keranjang kosong</p>
            </div>
        `;
        document.getElementById("btn-checkout").disabled = true;
        document.getElementById("cart-subtotal").innerText = "Rp 0";
        document.getElementById("cart-total").innerText = "Rp 0";
        return;
    }

    document.getElementById("btn-checkout").disabled = false;

    let subtotal = 0;
    cart.forEach(item => {
        subtotal += item.price * item.qty;
        
        const itemRow = document.createElement("div");
        itemRow.className = "cart-item";
        itemRow.innerHTML = `
            <div class="cart-item-info">
                <h4>${item.name}</h4>
                <p>${formatRupiah(item.price)}</p>
            </div>
            <div class="cart-item-qty">
                <button class="qty-btn" onclick="changeQty(${item.id}, -1)">-</button>
                <span>${item.qty}</span>
                <button class="qty-btn" onclick="changeQty(${item.id}, 1)">+</button>
            </div>
        `;
        container.appendChild(itemRow);
    });

    // Subtotal
    document.getElementById("cart-subtotal").innerText = formatRupiah(subtotal);

    // Calculate Total with Discount
    const discountInput = document.getElementById("cart-discount");
    let discountVal = Number(discountInput.value) || 0;
    let total = subtotal - discountVal;
    if (total < 0) total = 0;

    document.getElementById("cart-total").innerText = formatRupiah(total);
}

window.changeQty = function(id, val) {
    const item = cart.find(i => i.id === id);
    if (!item) return;

    item.qty += val;
    if (item.qty <= 0) {
        cart = cart.filter(i => i.id !== id);
    }
    updateCartUI();
};

// Checkout & Post transaction to VPS database
async function triggerCheckout() {
    const btn = document.getElementById("btn-checkout");
    btn.disabled = true;
    btn.innerText = "Memproses...";

    const tenantId = localStorage.getItem("tenantId");
    const subtotal = cart.reduce((sum, item) => sum + (item.price * item.qty), 0);
    const discount = Number(document.getElementById("cart-discount").value) || 0;
    const total = subtotal - discount;
    
    const activePayMethod = document.querySelector(".pay-method-btn.active").getAttribute("data-method");
    const timestamp = Date.now();
    const receiptNum = `SB-${timestamp.toString().slice(-6)}`;

    // Create primary transaction payload
    const txId = Math.floor(Math.random() * 1000000);
    const txPayload = [{
        id: txId,
        tenantId: tenantId,
        outletId: 1,
        employeeId: 99,
        customerName: "Pelanggan POS Web",
        receiptNumber: receiptNum,
        date: timestamp,
        subtotal: subtotal,
        total: total,
        discount: discount,
        paymentMethod: activePayMethod,
        status: "COMPLETED",
        type: "SALES",
        createdAt: timestamp,
        updatedAt: timestamp
    }];

    // Create item payloads
    const txItemsPayload = cart.map(item => ({
        id: Math.floor(Math.random() * 1000000),
        transactionId: txId,
        productId: item.id,
        quantity: item.qty,
        price: item.price,
        costPrice: 0,
        discount: 0
    }));

    try {
        // Post Transaction
        const resTx = await fetch(`${BASE_URL}/api/sync/transactions`, {
            method: "POST",
            headers: { 
                "Content-Type": "application/json",
                "x-tenant-id": tenantId || "",
                "x-user-email": localStorage.getItem("email") || ""
            },
            body: JSON.stringify(txPayload)
        });

        // Post Items
        const resItems = await fetch(`${BASE_URL}/api/sync/transaction_items`, {
            method: "POST",
            headers: { 
                "Content-Type": "application/json",
                "x-tenant-id": tenantId || "",
                "x-user-email": localStorage.getItem("email") || ""
            },
            body: JSON.stringify(txItemsPayload)
        });

        if (resTx.ok && resItems.ok) {
            alert("Transaksi Sukses!");
            
            // Generate print trigger
            const fullTx = { ...txPayload[0], items: cart };
            triggerThermalPrint(fullTx);

            // Clear Cart
            cart = [];
            document.getElementById("cart-discount").value = "0";
            updateCartUI();

            // Refresh counts
            refreshData();
        } else {
            alert("Terjadi kesalahan saat memproses transaksi di VPS database.");
        }

    } catch (error) {
        console.error("Checkout Error:", error);
        alert("Gagal terhubung ke VPS Server.");
    } finally {
        btn.disabled = false;
        btn.innerText = "Bayar & Cetak Struk (Groziie)";
    }
}

// WebSocket Integration for Real-time Receipt Triggering
function connectWebSocket() {
    console.log("Menghubungkan WebSocket ke:", WS_URL);
    wsConn = new WebSocket(WS_URL);

    wsConn.onopen = () => {
        console.log("WebSocket terhubung!");
        document.getElementById("ws-status").innerText = "WebSocket Aktif";
        document.getElementById("ws-status").parentElement.querySelector(".dot").className = "dot active";
    };

    wsConn.onmessage = (event) => {
        try {
            const payload = JSON.parse(event.data);
            console.log("[WS MESSAGE RECEIVED]:", payload);
            
            if (payload.event === "new_transaction" && payload.data) {
                const tenantId = localStorage.getItem("tenantId");
                const firstRow = payload.data[0];
                
                if (firstRow && firstRow.tenantId === tenantId) {
                    console.log("[WS] Triggering automatic thermal print for receipt:", firstRow.receiptNumber);
                    triggerThermalPrint(firstRow);
                    refreshData();
                }
            } else if (payload.event === "data_synced" && payload.data) {
                const tenantId = localStorage.getItem("tenantId");
                const firstRow = payload.data[0];
                
                if (firstRow && firstRow.tenantId === tenantId) {
                    console.log("[WS] Silent data refresh triggered for table:", payload.table);
                    refreshData();
                }
            }
        } catch (e) {
            console.error("Format WebSocket tidak valid:", e);
        }
    };

    wsConn.onclose = () => {
        console.warn("WebSocket terputus. Mencoba menghubungkan kembali dalam 5 detik...");
        document.getElementById("ws-status").innerText = "Koneksi Terputus";
        document.getElementById("ws-status").parentElement.querySelector(".dot").className = "dot";
        
        setTimeout(connectWebSocket, 5000);
    };

    wsConn.onerror = (err) => {
        console.error("WebSocket error:", err);
    };
}

// Continuous Form Thermal Print Layout Injection (Groziie TD630 layout)
function triggerThermalPrint(tx) {
    const printArea = document.getElementById("print-area");
    
    const formattedDate = new Date(Number(tx.date || Date.now())).toLocaleString("id-ID");
    const items = tx.items || [];
    
    let itemsRowsHtml = "";
    if (items.length > 0) {
        items.forEach(item => {
            const sub = item.price * (item.qty || item.quantity);
            itemsRowsHtml += `
                <tr>
                    <td>${item.name || `Produk ID: ${item.productId}`}</td>
                    <td>${item.qty || item.quantity}</td>
                    <td>${formatRupiah(item.price)}</td>
                    <td>${formatRupiah(sub)}</td>
                </tr>
            `;
        });
    } else {
        itemsRowsHtml = `<tr><td colspan="4" style="text-align:center;">Transaksi Item Terdaftar</td></tr>`;
    }

    const printHtml = `
        <div class="print-invoice-title">NOTA TRANSAKSI POSBAH</div>
        <div class="print-meta">
            <div>
                <p><strong>Nomor Struk:</strong> ${tx.receiptNumber}</p>
                <p><strong>Tanggal:</strong> ${formattedDate}</p>
                <p><strong>Kasir:</strong> POS Web Terminal</p>
            </div>
            <div style="text-align: right;">
                <p><strong>Toko:</strong> ${tx.tenantId.toUpperCase()}</p>
                <p><strong>Status:</strong> ${translateInvoiceStatus(tx.status || 'COMPLETED')}</p>
                <p><strong>Pembayaran:</strong> ${translatePaymentMethod(tx.paymentMethod)}</p>
            </div>
        </div>

        <table class="print-table">
            <thead>
                <tr>
                    <th>Barang</th>
                    <th>Jumlah</th>
                    <th>Harga</th>
                    <th>Subtotal</th>
                </tr>
            </thead>
            <tbody>
                ${itemsRowsHtml}
            </tbody>
        </table>

        <div class="print-total-section">
            <div class="print-total-row">
                <span>Subtotal:</span>
                <span>${formatRupiah(tx.subtotal)}</span>
            </div>
            <div class="print-total-row">
                <span>Diskon:</span>
                <span>${formatRupiah(tx.discount)}</span>
            </div>
            <div class="print-total-row grand">
                <span>TOTAL AKHIR:</span>
                <span>${formatRupiah(tx.total)}</span>
            </div>
        </div>

        <div class="print-footer">
            <p><strong>Terima Kasih Atas Kunjungan Anda</strong></p>
            <p>Struk dicetak otomatis menggunakan kertas continuous form Groziie TD630 (9.5" x 11")</p>
        </div>
    `;

    printArea.innerHTML = printHtml;
    window.print();
}

// Helpers
function formatRupiah(number) {
    return new Intl.NumberFormat("id-ID", {
        style: "currency",
        currency: "IDR",
        minimumFractionDigits: 0
    }).format(number);
}

function setupNavigation() {
    const items = document.querySelectorAll(".menu-item");
    const sections = document.querySelectorAll(".app-section");

    items.forEach(item => {
        item.addEventListener("click", (e) => {
            e.preventDefault();
            const targetId = item.getAttribute("data-target");

            items.forEach(i => i.classList.remove("active"));
            sections.forEach(s => s.classList.remove("active-section"));

            item.classList.add("active");
            document.getElementById(targetId).classList.add("active-section");

            if (targetId === "margin-outlet-section") {
                fetchOutletMarginReport(7);
                document.querySelectorAll(".margin-period-btn").forEach(btn => {
                    if (btn.getAttribute("data-days") === "7") {
                        btn.classList.add("active");
                    } else {
                        btn.classList.remove("active");
                    }
                });
            }
        });
    });

    // Logout
    document.getElementById("logout-btn").addEventListener("click", () => {
        if (confirm("Apakah Anda yakin ingin logout dari POSBah Web?")) {
            localStorage.clear();
            showLoginOverlay();
        }
    });
}

function setupPOSListeners() {
    // Search
    document.getElementById("product-search").addEventListener("input", (e) => {
        const activeFilter = document.querySelector(".filter-btn.active").getAttribute("data-category");
        renderPOSProducts(activeFilter, e.target.value);
    });

    // Category Buttons
    const categoryBtns = document.querySelectorAll(".filter-btn");
    categoryBtns.forEach(btn => {
        btn.addEventListener("click", () => {
            categoryBtns.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            
            const category = btn.getAttribute("data-category");
            const searchVal = document.getElementById("product-search").value;
            renderPOSProducts(category, searchVal);
        });
    });

    // Discount changes recalculates total
    document.getElementById("cart-discount").addEventListener("input", () => {
        updateCartUI();
    });

    // Checkout Action
    document.getElementById("btn-checkout").addEventListener("click", () => {
        triggerCheckout();
    });
}

function setupTabListeners() {
    const tabs = document.querySelectorAll(".tab-btn");
    const contents = document.querySelectorAll(".tab-content");

    tabs.forEach(tab => {
        tab.addEventListener("click", () => {
            tabs.forEach(t => t.classList.remove("active"));
            contents.forEach(c => c.classList.remove("active-tab-content"));

            tab.classList.add("active");
            const target = tab.getAttribute("data-tab");
            document.getElementById(target).classList.add("active-tab-content");
        });
    });
}

// ─────────────────────────────────────────────────────────────────────────────
// BMP B2B Factory Module Functions
// ─────────────────────────────────────────────────────────────────────────────

function setupInvoiceListeners() {
    const closeBtn = document.getElementById("close-invoice-modal-btn");
    if (closeBtn) {
        closeBtn.addEventListener("click", () => {
            document.getElementById("invoice-detail-overlay").style.display = "none";
        });
    }

    const editBankBtn = document.getElementById("btn-edit-bank-info");
    if (editBankBtn) {
        editBankBtn.addEventListener("click", () => {
            document.getElementById("bank-info-display-container").style.display = "none";
            document.getElementById("bank-info-edit-container").style.display = "block";
        });
    }

    const cancelBankBtn = document.getElementById("btn-cancel-bank-edit");
    if (cancelBankBtn) {
        cancelBankBtn.addEventListener("click", () => {
            document.getElementById("bank-info-edit-container").style.display = "none";
            document.getElementById("bank-info-display-container").style.display = "block";
        });
    }

    const saveBankBtn = document.getElementById("btn-save-bank-info");
    if (saveBankBtn) {
        saveBankBtn.addEventListener("click", () => {
            saveBankInfo();
        });
    }

    const deleteSigBtn = document.getElementById("btn-delete-signature");
    if (deleteSigBtn) {
        deleteSigBtn.addEventListener("click", () => {
            deleteSignature();
        });
    }

    const generateSigBtn = document.getElementById("btn-generate-signature-link");
    if (generateSigBtn) {
        generateSigBtn.addEventListener("click", () => {
            shareSignatureLink();
        });
    }

    const shareSigBtn = document.getElementById("btn-share-signature-link");
    if (shareSigBtn) {
        shareSigBtn.addEventListener("click", () => {
            shareSignatureLink();
        });
    }

    const copySigBtn = document.getElementById("btn-copy-signature-link");
    if (copySigBtn) {
        copySigBtn.addEventListener("click", () => {
            const linkInput = document.getElementById("input-signature-link");
            const linkUrl = linkInput ? linkInput.value : "";
            if (!linkUrl) return;

            const copyMsg = "link telah disalin, silahkan kirim ke whatsapp atau klien anda";
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(linkUrl).then(() => {
                    const msgEl = document.getElementById("copy-success-message");
                    if (msgEl) {
                        msgEl.innerText = copyMsg;
                        msgEl.style.display = "block";
                    }
                }).catch(err => {
                    console.error("Gagal menyalin link:", err);
                    if (linkInput) {
                        linkInput.select();
                        document.execCommand("copy");
                    }
                    const msgEl = document.getElementById("copy-success-message");
                    if (msgEl) {
                        msgEl.innerText = copyMsg;
                        msgEl.style.display = "block";
                    }
                });
            } else {
                if (linkInput) {
                    linkInput.select();
                    document.execCommand("copy");
                }
                const msgEl = document.getElementById("copy-success-message");
                if (msgEl) {
                    msgEl.innerText = copyMsg;
                    msgEl.style.display = "block";
                }
            }
        });
    }

    const printInvoiceBtn = document.getElementById("print-invoice-modal-btn");
    if (printInvoiceBtn) {
        printInvoiceBtn.addEventListener("click", () => {
            triggerInvoicePrint();
        });
    }

    const printSjBtn = document.getElementById("print-sj-modal-btn");
    if (printSjBtn) {
        printSjBtn.addEventListener("click", () => {
            triggerSjPrint();
        });
    }
}

function renderInvoicesList() {
    const tbody = document.getElementById("invoices-list-body");
    const countBadge = document.getElementById("invoices-count-badge");
    tbody.innerHTML = "";

    if (countBadge) countBadge.innerText = `Total: ${invoices.length} Invoice`;

    if (invoices.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center">Tidak ada faktur / invoice terdaftar</td></tr>`;
        return;
    }

    // Sort descending by id or createdAt
    invoices.sort((a, b) => b.id - a.id);

    invoices.forEach(inv => {
        const tr = document.createElement("tr");

        // Find client name
        const client = clients.find(c => c.id === inv.clientId);
        const clientName = client ? client.clientName : `Klien ID: ${inv.clientId}`;

        // Format Date
        const dueDateStr = inv.dueDate ? new Date(Number(inv.dueDate)).toLocaleDateString("id-ID", {year: 'numeric', month: 'short', day: 'numeric'}) : "-";

        let statusClass = "warning";
        if (inv.status === "PAID") statusClass = "success";
        else if (inv.status === "DRAFT") statusClass = "danger";

        tr.innerHTML = `
            <td><strong>${inv.number}</strong></td>
            <td>${inv.title}</td>
            <td>${clientName}</td>
            <td>${formatRupiah(inv.totalAmount)}</td>
            <td><span class="badge ${statusClass}">${translateInvoiceStatus(inv.status)}</span></td>
            <td>${dueDateStr}</td>
            <td>
                <button class="btn btn-primary" onclick="showInvoiceDetails(${inv.id})">
                    <i class="fa-solid fa-eye"></i> Detail
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

window.showInvoiceDetails = function(invoiceId) {
    const invoice = invoices.find(i => i.id === invoiceId);
    if (!invoice) return;

    currentInvoice = invoice;

    const client = clients.find(c => c.id === invoice.clientId);
    const clientName = client ? client.clientName : `Klien ID: ${invoice.clientId}`;

    document.getElementById("inv-detail-number").innerText = invoice.number || "-";
    document.getElementById("inv-detail-title").innerText = invoice.title || "-";
    document.getElementById("inv-detail-client").innerText = clientName;
    document.getElementById("inv-detail-due").innerText = invoice.dueDate ? new Date(Number(invoice.dueDate)).toLocaleDateString("id-ID", {year: 'numeric', month: 'long', day: 'numeric'}) : "-";
    document.getElementById("inv-detail-status").innerText = translateInvoiceStatus(invoice.status || "DRAFT");
    document.getElementById("inv-detail-terms").innerText = translatePaymentTerms(invoice.paymentTerms || "14 days");

    // Item list
    const itemsBody = document.getElementById("inv-detail-items-body");
    itemsBody.innerHTML = "";

    const items = bmpProducts.filter(p => p.invoiceId === invoiceId);
    if (items.length === 0) {
        itemsBody.innerHTML = `<tr><td colspan="4" class="text-center">Tidak ada item barang</td></tr>`;
    } else {
        items.forEach(item => {
            const tr = document.createElement("tr");
            const sub = item.price * item.quantity;
            tr.innerHTML = `
                <td><strong>${item.title}</strong></td>
                <td>${item.quantity} ${item.unit}</td>
                <td>${formatRupiah(item.price)}</td>
                <td>${formatRupiah(sub)}</td>
            `;
            itemsBody.appendChild(tr);
        });
    }

    // Totals
    document.getElementById("inv-detail-total").innerText = formatRupiah(invoice.totalAmount);
    document.getElementById("inv-detail-paid").innerText = formatRupiah(invoice.paidAmount);
    document.getElementById("inv-detail-remaining").innerText = formatRupiah(invoice.totalAmount - invoice.paidAmount);

    // Bank Info
    const bankDisplay = document.getElementById("bank-info-display-container");
    const bankEdit = document.getElementById("bank-info-edit-container");
    bankDisplay.style.display = "block";
    bankEdit.style.display = "none";

    if (printSettings) {
        document.getElementById("lbl-bank-name").innerText = printSettings.bankName || "-";
        document.getElementById("lbl-bank-account").innerText = printSettings.bankAccountNumber || "-";
        document.getElementById("lbl-bank-owner").innerText = printSettings.bankOwnerName || "-";

        document.getElementById("txt-bank-name").value = printSettings.bankName || "";
        document.getElementById("txt-bank-account").value = printSettings.bankAccountNumber || "";
        document.getElementById("txt-bank-owner").value = printSettings.bankOwnerName || "";
    } else {
        document.getElementById("lbl-bank-name").innerText = "-";
        document.getElementById("lbl-bank-account").innerText = "-";
        document.getElementById("lbl-bank-owner").innerText = "-";

        document.getElementById("txt-bank-name").value = "";
        document.getElementById("txt-bank-account").value = "";
        document.getElementById("txt-bank-owner").value = "";
    }

    // Signature
    const sigContainer = document.getElementById("inv-detail-signature-container");
    const noSigContainer = document.getElementById("inv-detail-no-signature-container");
    const shareBox = document.getElementById("signature-link-share-box");
    if (shareBox) shareBox.style.display = "none";
    const successMsg = document.getElementById("copy-success-message");
    if (successMsg) successMsg.style.display = "none";

    if (invoice.receiverSignatureUrl) {
        sigContainer.style.display = "block";
        noSigContainer.style.display = "none";
        document.getElementById("inv-detail-receiver-name").innerText = invoice.receiverNameActual || "Penerima";
        document.getElementById("inv-detail-signature-img").src = invoice.receiverSignatureUrl;
    } else {
        sigContainer.style.display = "none";
        noSigContainer.style.display = "block";
    }

    // Show Overlay
    document.getElementById("invoice-detail-overlay").style.display = "flex";
};

async function saveBankInfo() {
    const bankName = document.getElementById("txt-bank-name").value.trim();
    const bankAccount = document.getElementById("txt-bank-account").value.trim();
    const bankOwner = document.getElementById("txt-bank-owner").value.trim();

    if (!bankName || !bankAccount || !bankOwner) {
        alert("Harap isi semua kolom informasi rekening bank!");
        return;
    }

    const tenantId = localStorage.getItem("tenantId");
    const email = localStorage.getItem("email");

    const payload = [{
        id: printSettings && printSettings.id ? printSettings.id : Date.now(),
        tenantId: tenantId,
        bankName: bankName,
        bankAccountNumber: bankAccount,
        bankOwnerName: bankOwner,
        updatedAt: Date.now()
    }];

    try {
        const response = await fetch(`${BASE_URL}/api/sync/print_settings`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "x-tenant-id": tenantId || "",
                "x-user-email": email || ""
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert("Informasi Bank berhasil disimpan!");
            printSettings = payload[0];

            document.getElementById("lbl-bank-name").innerText = bankName;
            document.getElementById("lbl-bank-account").innerText = bankAccount;
            document.getElementById("lbl-bank-owner").innerText = bankOwner;

            document.getElementById("bank-info-edit-container").style.display = "none";
            document.getElementById("bank-info-display-container").style.display = "block";
        } else {
            const err = await response.text();
            alert("Gagal menyimpan data: " + err);
        }
    } catch (e) {
        console.error("Error saving bank info:", e);
        alert("Gagal menghubungi server.");
    }
}

async function deleteSignature() {
    if (!currentInvoice) return;
    if (!confirm("Apakah Anda yakin ingin menghapus tanda tangan digital penerima untuk invoice ini?")) return;

    const tenantId = localStorage.getItem("tenantId");

    try {
        const response = await fetch(`${BASE_URL}/api/invoice/delete-signature`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                invoiceId: currentInvoice.id,
                tenantId: tenantId
            })
        });

        if (response.ok) {
            alert("Tanda tangan berhasil dihapus.");
            currentInvoice.receiverSignatureUrl = null;
            currentInvoice.receiverNameActual = null;

            document.getElementById("inv-detail-signature-container").style.display = "none";
            document.getElementById("inv-detail-no-signature-container").style.display = "block";

            refreshData();
        } else {
            const err = await response.text();
            alert("Gagal menghapus tanda tangan: " + err);
        }
    } catch (e) {
        console.error("Error deleting signature:", e);
        alert("Gagal menghubungi server.");
    }
}

async function shareSignatureLink() {
    if (!currentInvoice) return;

    const tenantId = localStorage.getItem("tenantId") || "";
    // Token expires in 10 minutes
    const expiryTimestamp = Date.now() + (10 * 60 * 1000);
    const dataToSign = `${tenantId}:${currentInvoice.id}:${expiryTimestamp}`;
    const SECRET_KEY = "PosBahSignatureSecretKey123!"; // Same as Android APK

    let signature = "";
    try {
        signature = await computeHmacSha256(dataToSign, SECRET_KEY);
    } catch (err) {
        console.error("Gagal membuat signature token:", err);
    }

    // Token format: "tenantId:invoiceId:expiry:signature"
    const tokenRaw = `${tenantId}:${currentInvoice.id}:${expiryTimestamp}:${signature}`;

    // Base64 encode URL-safe
    const tokenEncoded = btoa(tokenRaw)
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/, "");

    const linkUrl = `${window.location.origin}/api/sign/${tokenEncoded}`;

    const linkInput = document.getElementById("input-signature-link");
    if (linkInput) {
        linkInput.value = linkUrl;
    }

    const shareBox = document.getElementById("signature-link-share-box");
    if (shareBox) {
        shareBox.style.display = "block";
    }

    const msgEl = document.getElementById("copy-success-message");
    if (msgEl) {
        msgEl.style.display = "none";
    }

    // Auto-copy once for convenience when generated
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(linkUrl).then(() => {
            if (msgEl) {
                msgEl.innerText = "Link Tanda Tangan Penerima berhasil disalin ke clipboard! Aktif selama 10 menit.";
                msgEl.style.display = "block";
            }
        }).catch(err => {
            console.error("Gagal menyalin otomatis:", err);
        });
    }
}

function triggerInvoicePrint() {
    if (!currentInvoice) return;

    const printArea = document.getElementById("print-area");
    const client = clients.find(c => c.id === currentInvoice.clientId);
    const clientName = client ? client.clientName : "-";
    const clientAddress = client ? client.addressLine1 || "-" : "-";
    const clientPhone = client ? client.phoneNumber || "-" : "-";

    const items = bmpProducts.filter(p => p.invoiceId === currentInvoice.id);
    let itemsHtml = "";
    items.forEach((p, index) => {
        const subtotal = p.price * p.quantity * p.jumlahLusin;
        const satuanVal = `${p.jumlahLusin} ${p.unit.toLowerCase() === "lusin" || p.unit === "-" ? "Lusin" : p.unit}`;
        itemsHtml += `
            <tr>
                <td style="border: 1px solid #ddd; padding: 8px; text-align: center;">${index + 1}</td>
                <td style="border: 1px solid #ddd; padding: 8px;"><strong>${p.title}</strong></td>
                <td style="border: 1px solid #ddd; padding: 8px; text-align: center;">${p.quantity}</td>
                <td style="border: 1px solid #ddd; padding: 8px; text-align: center;">${satuanVal}</td>
                <td style="border: 1px solid #ddd; padding: 8px; text-align: right;">${formatRupiah(p.price)}</td>
                <td style="border: 1px solid #ddd; padding: 8px; text-align: right;">${formatRupiah(subtotal)}</td>
            </tr>
        `;
    });

    const isColor = true;
    const themeColor = "#1E3A8A";
    const accentBg = "#EFF6FF";
    const statusColor = currentInvoice.status === "PAID" ? "#10B981" : (currentInvoice.status === "PARTIAL" ? "#3B82F6" : "#6B7280");

    const bankInfoHtmlContent = (printSettings && printSettings.bankOwnerName && printSettings.bankAccountNumber) ? `
        <div style="font-size: 13px; margin-bottom: 15px; color: #000; text-align: left; margin-right: 15px;">
            <strong>Info Pembayaran :</strong><br>
            bank : ${printSettings.bankName} : ${printSettings.bankAccountNumber}<br>
            atas nama : ${printSettings.bankOwnerName}
        </div>
    ` : "";

    const receiverSig = currentInvoice.receiverSignatureUrl || "";
    const receiverName = currentInvoice.receiverNameActual || (printSettings ? printSettings.invoiceSignatureReceiverName || "" : "");
    const senderName = printSettings ? printSettings.invoiceSignatureSenderName || "Admin" : "Admin";

    const signatureHtml = `
        <table class="signature-section">
            <tr>
                <td class="signature-col" style="vertical-align: top;">
                    Penerima,<br>
                    ${receiverSig ? `<img src="${receiverSig}" style="height: 60px; width: auto; margin: 5px auto; display: block;" />` : `<div style="height: 60px;"></div>`}
                    ( <strong>${receiverName || " _____________________ "}</strong> )
                </td>
                <td class="signature-col" style="vertical-align: top;">
                    Hormat Kami,<br>
                    <div style="height: 60px;"></div>
                    ( <strong>${senderName || " _____________________ "}</strong> )
                </td>
            </tr>
        </table>
    `;

    const paperSize = document.getElementById("select-paper-size").value;
    let sizeStyle = "";
    if (paperSize === "A4") {
        sizeStyle = `
            @page {
                size: A4 portrait !important;
                margin: 20mm 15mm 20mm 15mm !important;
            }
            #print-area {
                width: 100% !important;
                height: auto !important;
                padding: 0 !important;
                box-sizing: border-box !important;
            }
        `;
    } else {
        sizeStyle = `
            @page {
                size: 9.5in 11in !important;
                margin: 0 !important;
            }
            #print-area {
                width: 9.5in !important;
                height: 11in !important;
                padding: 0.6in 0.8in !important;
                box-sizing: border-box !important;
            }
        `;
    }

    const printHtml = `
        <style>
            ${sizeStyle}
            #print-area {
                display: block !important;
                font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                color: #333;
                font-size: 13px;
                line-height: 1.5;
                text-align: left;
            }
            .header-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
            .company-name { font-size: 20px; font-weight: bold; color: ${themeColor}; }
            .doc-title { font-size: 16px; font-weight: bold; text-align: right; color: ${themeColor}; text-transform: uppercase; }
            .info-table { width: 100%; border-collapse: collapse; margin-bottom: 25px; }
            .info-col { width: 100%; vertical-align: top; }
            .info-box { text-align: left; line-height: 1.5; }
            .info-box-right { display: none; }
            .details-table { width: 100%; border-collapse: collapse; margin-bottom: 25px; }
            .details-table th {
                background-color: ${themeColor};
                color: white;
                padding: 10px;
                font-weight: bold;
                text-align: left;
            }
            .details-table td { padding: 10px; border-bottom: 1px solid #eee; }
            .totals-table { width: 100%; border-collapse: collapse; margin-top: 0; }
            .totals-table td { padding: 6px 10px; }
            .totals-table tr.grand-total { font-size: 15px; font-weight: bold; background-color: ${accentBg}; }
            .status-badge {
                font-weight: bold;
                color: inherit;
                background-color: transparent;
                font-size: inherit;
                text-transform: none;
            }
            .footer { margin-top: 50px; text-align: center; color: #777; font-size: 11px; border-top: 1px solid #eee; padding-top: 15px; }
            .signature-section { width: 100%; margin-top: 50px; border-collapse: collapse; }
            .signature-col { width: 50%; text-align: center; }
        </style>
        
        <table class="header-table">
            <tr>
                <td style="vertical-align: top; text-align: left;">
                    <span class="company-name">CV. Bahtera Mulya Plastik</span><br>
                    Sidoarjo, Jawa Timur<br>
                    Telp: 082652626237 | Email: bahteramulyap@gmail.com
                </td>
                <td style="text-align: right; vertical-align: top; line-height: 1.4; font-size: 13px; color: #000;">
                    <span class="doc-title">FAKTUR</span><br>
                    No: ${currentInvoice.number}<br>
                    Tanggal Faktur: ${new Date(Number(currentInvoice.createdAt || Date.now())).toLocaleDateString("id-ID", {year: 'numeric', month: 'long', day: 'numeric'})}<br>
                    Jatuh Tempo: ${currentInvoice.dueDate ? new Date(Number(currentInvoice.dueDate)).toLocaleDateString("id-ID", {year: 'numeric', month: 'long', day: 'numeric'}) : "-"}<br>
                    Status: <span style="font-weight: bold; color: ${currentInvoice.status === 'PAID' ? '#10B981' : '#EF4444'};">${translateInvoiceStatus(currentInvoice.status)}</span>
                </td>
            </tr>
        </table>

        <hr style="border: none; border-top: 2px solid ${themeColor}; margin-bottom: 20px;">

        <table class="info-table">
            <tr>
                <td class="info-col">
                    <div class="info-box">
                        <strong>DITAGIHKAN KEPADA:</strong><br>
                        <span style="font-size: 14px; font-weight: bold; color: #000;">${clientName}</span><br>
                        Alamat: ${clientAddress}<br>
                        Telp: ${clientPhone}
                    </div>
                </td>
            </tr>
        </table>

        <table class="details-table" style="border: 1px solid #ddd; width: 100%;">
            <thead>
                <tr>
                    <th style="width: 5%; text-align: center;">No</th>
                    <th style="width: 45%;">Nama Barang / Deskripsi</th>
                    <th style="width: 10%; text-align: center;">Jumlah</th>
                    <th style="width: 15%; text-align: center;">Satuan</th>
                    <th style="width: 12%; text-align: right;">Harga</th>
                    <th style="width: 13%; text-align: right;">Total</th>
                </tr>
            </thead>
            <tbody>
                ${itemsHtml}
            </tbody>
        </table>

        <table style="width: 100%; margin-top: 15px; border-collapse: collapse;">
            <tr>
                <td style="width: 60%; vertical-align: top; text-align: left; padding: 0;">
                    ${bankInfoHtmlContent}
                </td>
                <td style="width: 40%; vertical-align: top; padding: 0;">
                    <table class="totals-table">
                        <tr>
                            <td style="padding: 6px 10px; text-align: left;">Subtotal:</td>
                            <td style="text-align: right; padding: 6px 10px;">${formatRupiah(currentInvoice.totalAmount)}</td>
                        </tr>
                        <tr>
                            <td style="padding: 6px 10px; text-align: left;">Telah Dibayar:</td>
                            <td style="text-align: right; padding: 6px 10px; color: #10B981;">${formatRupiah(currentInvoice.paidAmount)}</td>
                        </tr>
                        <tr class="grand-total">
                            <td style="border-top: 1px solid #ccc; padding: 6px 10px; font-weight: bold; text-align: left;">Sisa Tagihan:</td>
                            <td style="text-align: right; border-top: 1px solid #ccc; padding: 6px 10px; font-weight: bold; color: #EF4444;">${formatRupiah(currentInvoice.totalAmount - currentInvoice.paidAmount)}</td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>

        ${signatureHtml}

        <div class="footer">
            Terima kasih atas kerja sama Anda.<br>
            Faktur ini dihasilkan secara luring oleh POSBah.
        </div>
    `;

    printArea.innerHTML = printHtml;
    window.print();
}

function triggerSjPrint() {
    if (!currentInvoice) return;

    const printArea = document.getElementById("print-area");
    const client = clients.find(c => c.id === currentInvoice.clientId);
    const clientName = client ? client.clientName : "-";
    const clientAddress = client ? client.addressLine1 || "-" : "-";
    const clientPhone = client ? client.phoneNumber || "-" : "-";

    const items = bmpProducts.filter(p => p.invoiceId === currentInvoice.id);
    let itemsHtml = "";
    items.forEach((p, index) => {
        const satuanVal = `${p.jumlahLusin} ${p.unit.toLowerCase() === "lusin" || p.unit === "-" ? "Lusin" : p.unit}`;
        itemsHtml += `
            <tr>
                <td style="border: 1px solid #333; padding: 8px; text-align: center;">${index + 1}</td>
                <td style="border: 1px solid #333; padding: 8px;"><strong>${p.title}</strong></td>
                <td style="border: 1px solid #333; padding: 8px; text-align: center;">${satuanVal}</td>
                <td style="border: 1px solid #333; padding: 8px; text-align: center;">${p.quantity}</td>
            </tr>
        `;
    });

    const receiverSig = currentInvoice.receiverSignatureUrl || "";
    const receiverName = currentInvoice.receiverNameActual || (printSettings ? printSettings.invoiceSignatureReceiverName || "" : "");
    const senderName = printSettings ? printSettings.invoiceSignatureSenderName || "Admin" : "Admin";

    const signatureHtml = `
        <table class="signature-section">
            <tr>
                <td class="signature-col">
                    Penerima,<br>
                    ${receiverSig ? `<img src="${receiverSig}" style="height: 60px; width: auto; margin: 5px auto; display: block;" />` : `<div style="height: 60px;"></div>`}
                    ( <strong>${receiverName || " _____________________ "}</strong> )
                </td>
                <td class="signature-col">
                    Pengirim / Sopir,<br><br>
                    <div style="height: 60px;"></div>
                    ( <strong> _____________________ </strong> )
                </td>
                <td class="signature-col" style="vertical-align: top;">
                    Hormat Kami (Gudang),<br>
                    <div style="height: 60px;"></div>
                    ( <strong>${senderName || " _____________________ "}</strong> )
                </td>
            </tr>
        </table>
    `;

    const paperSize = document.getElementById("select-paper-size").value;
    let sizeStyle = "";
    if (paperSize === "A4") {
        sizeStyle = `
            @page {
                size: A4 portrait !important;
                margin: 20mm 15mm 20mm 15mm !important;
            }
            #print-area {
                width: 100% !important;
                height: auto !important;
                padding: 0 !important;
                box-sizing: border-box !important;
            }
        `;
    } else {
        sizeStyle = `
            @page {
                size: 9.5in 11in !important;
                margin: 0 !important;
            }
            #print-area {
                width: 9.5in !important;
                height: 11in !important;
                padding: 0.6in 0.8in !important;
                box-sizing: border-box !important;
            }
        `;
    }

    const printHtml = `
        <style>
            ${sizeStyle}
            #print-area {
                display: block !important;
                font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                color: #000;
                font-size: 13px;
                line-height: 1.5;
                text-align: left;
            }
            .header-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
            .company-name { font-size: 18px; font-weight: bold; color: #000; }
            .doc-title { font-size: 22px; font-weight: bold; text-align: right; text-transform: uppercase; letter-spacing: 1px; }
            .info-table { width: 100%; border-collapse: collapse; margin-bottom: 25px; }
            .info-col { width: 50%; vertical-align: top; }
            .info-box { padding: 10px; border: none; margin-right: 10px; text-align: left; }
            .info-box-right { padding: 10px; border: none; margin-left: 10px; text-align: left; }
            .details-table { width: 100%; border-collapse: collapse; margin-bottom: 25px; }
            .details-table th {
                background-color: #333;
                color: white;
                padding: 8px;
                font-weight: bold;
                text-align: left;
                border: 1px solid #333;
            }
            .details-table td { padding: 8px; border: 1px solid #333; }
            .footer { margin-top: 30px; text-align: center; color: #555; font-size: 11px; }
            .signature-section { width: 100%; margin-top: 40px; border-collapse: collapse; }
            .signature-col { width: 33%; text-align: center; vertical-align: top; }
        </style>
        
        <table class="header-table">
            <tr>
                <td style="vertical-align: top; text-align: left;">
                    <span class="company-name">CV. Bahtera Mulya Plastik</span><br>
                    Sidoarjo, Jawa Timur<br>
                    Telp: 082652626237
                </td>
                <td style="text-align: right; vertical-align: top;">
                    <span class="doc-title">Surat Jalan</span><br>
                    No. SJ: <strong>SJ-${currentInvoice.number}</strong><br>
                    Tanggal: ${new Date().toLocaleDateString("id-ID", {year: 'numeric', month: 'long', day: 'numeric'})}
                </td>
            </tr>
        </table>

        <hr style="border: none; border-top: 2px solid #000; margin-bottom: 20px;">

        <table class="info-table">
            <tr>
                <td class="info-col" style="width: 50%;">
                    <div class="info-box">
                        <strong>Kirim Kepada:</strong><br>
                        <span style="font-size: 14px; font-weight: bold;">${clientName}</span><br>
                        Alamat: ${clientAddress}<br>
                        Telp: ${clientPhone}
                    </div>
                </td>
                <td class="info-col" style="width: 50%;">
                    <div class="info-box-right">
                        <strong>Keterangan:</strong><br>
                        Dokumen ini adalah bukti penyerahan barang secara sah.<br>
                        No. Faktur Rujukan: <strong>${currentInvoice.number}</strong>
                    </div>
                </td>
            </tr>
        </table>

        <table class="details-table" style="width: 100%;">
            <thead>
                <tr>
                    <th style="width: 8%; text-align: center;">No</th>
                    <th style="width: 52%;">Nama Barang / Deskripsi</th>
                    <th style="width: 20%; text-align: center;">Satuan</th>
                    <th style="width: 20%; text-align: center;">Jumlah</th>
                </tr>
            </thead>
            <tbody>
                ${itemsHtml}
            </tbody>
        </table>

        ${signatureHtml}

        <div class="footer">
            Surat Jalan ini sah dan diterbitkan secara luring oleh POSBah.
        </div>
    `;

    printArea.innerHTML = printHtml;
    window.print();
}

function translateInvoiceStatus(status) {
    if (!status) return "";
    switch (status.toUpperCase()) {
        case "PAID": return "Lunas";
        case "UNPAID": return "Belum Bayar";
        case "PARTIAL": return "Cicil";
        case "OVERDUE": return "Jatuh Tempo";
        case "DRAFT": return "Draft";
        case "COMPLETED": return "Selesai";
        default: return status;
    }
}

function translatePaymentTerms(terms) {
    if (!terms) return "";
    return terms.toLowerCase().replace("days", "hari").replace("day", "hari");
}

function translatePaymentMethod(method) {
    if (!method) return "";
    switch (method.toUpperCase()) {
        case "CASH": return "TUNAI";
        case "TRANSFER": return "TRANSFER";
        case "QRIS": return "QRIS";
        default: return method;
    }
}

async function computeHmacSha256(message, secret) {
    const encoder = new TextEncoder();
    const keyData = encoder.encode(secret);
    const messageData = encoder.encode(message);
    const cryptoKey = await window.crypto.subtle.importKey(
        "raw",
        keyData,
        { name: "HMAC", hash: "SHA-256" },
        false,
        ["sign"]
    );
    const signatureBuffer = await window.crypto.subtle.sign(
        "HMAC",
        cryptoKey,
        messageData
    );
    const hashArray = Array.from(new Uint8Array(signatureBuffer));
    const binary = hashArray.map(b => String.fromCharCode(b)).join('');
    return btoa(binary)
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/, "");
}

function setupMarginOutletListeners() {
    const periodBtns = document.querySelectorAll(".margin-period-btn");
    periodBtns.forEach(btn => {
        btn.addEventListener("click", () => {
            periodBtns.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            const days = Number(btn.getAttribute("data-days")) || 7;
            fetchOutletMarginReport(days);
        });
    });
}

async function fetchOutletMarginReport(days = 7) {
    const tenantId = localStorage.getItem("tenantId");
    const email = localStorage.getItem("email");
    const headers = {
        "x-tenant-id": tenantId || "",
        "x-user-email": email || ""
    };

    const tbody = document.getElementById("margin-outlet-list-body");
    tbody.innerHTML = `<tr><td colspan="7" class="text-center"><div class="spinner" style="margin: 20px auto;"></div> Memuat analisis margin...</td></tr>`;

    try {
        // Fetch outlets details
        const resOutlets = await fetch(`${BASE_URL}/api/sync/outlets?tenantId=eq.${tenantId}`, { headers });
        const outlets = resOutlets.ok ? await resOutlets.json() : [];

        // Fetch margin data
        const resMargin = await fetch(`${BASE_URL}/api/reports/outlet-margin?tenantId=${tenantId}&days=${days}`, { headers });
        if (!resMargin.ok) throw new Error("Gagal mengambil data dari API margin");
        const marginData = await resMargin.json();

        // Aggregate by outlet
        const outletMap = {};
        // Initialize map with all outlets
        outlets.forEach(o => {
            outletMap[o.id] = {
                id: o.id,
                name: o.name,
                address: o.address || "Lokasi tidak diset",
                revenue: 0,
                cost: 0,
                margin: 0
            };
        });

        // Sum data from API
        marginData.forEach(row => {
            const id = row.outletId;
            if (!outletMap[id]) {
                outletMap[id] = {
                    id: id,
                    name: row.outletName || `Outlet ${id}`,
                    address: "Lokasi tidak diset",
                    revenue: 0,
                    cost: 0,
                    margin: 0
                };
            }
            outletMap[id].revenue += row.revenue || 0;
            outletMap[id].cost += row.cost || 0;
            outletMap[id].margin += row.margin || 0;
        });

        // Convert map to array
        const aggregated = Object.values(outletMap);

        // Render summary cards
        let totalRevenue = 0;
        let totalCogs = 0;
        let totalMargin = 0;

        tbody.innerHTML = "";

        if (aggregated.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center">Belum ada outlet terdaftar</td></tr>`;
        } else {
            aggregated.forEach(row => {
                totalRevenue += row.revenue;
                totalCogs += row.cost;
                totalMargin += row.margin;

                const marginPercent = row.revenue > 0 ? ((row.margin / row.revenue) * 100).toFixed(1) : "0.0";
                
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${row.id}</td>
                    <td><strong>${row.name}</strong></td>
                    <td>${row.address}</td>
                    <td>${formatRupiah(row.revenue)}</td>
                    <td>${formatRupiah(row.cost)}</td>
                    <td style="color: ${row.margin >= 0 ? '#10B981' : '#EF4444'}; font-weight: 700;">${formatRupiah(row.margin)}</td>
                    <td><span class="badge ${row.margin >= 0 ? 'success' : 'danger'}">${marginPercent}%</span></td>
                `;
                tbody.appendChild(tr);
            });
        }

        // Update card values
        document.getElementById("margin-total-revenue").innerText = formatRupiah(totalRevenue);
        document.getElementById("margin-total-cogs").innerText = formatRupiah(totalCogs);
        
        const totalMarginPercent = totalRevenue > 0 ? ((totalMargin / totalRevenue) * 100).toFixed(1) : "0.0";
        document.getElementById("margin-total-profit").innerText = `${formatRupiah(totalMargin)} (${totalMarginPercent}%)`;

    } catch (err) {
        console.error("Gagal memuat laporan margin outlet:", err);
        tbody.innerHTML = `<tr><td colspan="7" class="text-center" style="color: var(--danger);"><i class="fa-solid fa-triangle-exclamation"></i> Gagal memuat data analisis margin: ${err.message}</td></tr>`;
    }
}
