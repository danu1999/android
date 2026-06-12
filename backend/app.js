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

// Auth Check & Initialize
document.addEventListener("DOMContentLoaded", () => {
    checkAuthentication();
    setupNavigation();
    setupPOSListeners();
    setupTabListeners();
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

    // Refresh layout data
    refreshData();

    // Start Websocket
    connectWebSocket();
}

// Fetch all database tables
async function refreshData() {
    const tenantId = localStorage.getItem("tenantId");
    const email = localStorage.getItem("email");
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
                <p><strong>Status:</strong> ${tx.status || 'COMPLETED'}</p>
                <p><strong>Pembayaran:</strong> ${tx.paymentMethod}</p>
            </div>
        </div>

        <table class="print-table">
            <thead>
                <tr>
                    <th>Item</th>
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
