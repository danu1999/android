const express = require('express');
const { createClient } = require('@supabase/supabase-js');
const cron = require('node-cron');
const path = require('path');
const fs = require('fs');

const app = express();
app.use(express.json());

// Load credentials from environment variables or use fallback values
const SUPABASE_URL = process.env.SUPABASE_URL || 'https://etustetneufkfilndimy.supabase.co';
const SUPABASE_SECRET_KEY = process.env.SUPABASE_SECRET_KEY || 'YOUR_SUPABASE_SECRET_KEY';

if (!SUPABASE_URL || !SUPABASE_SECRET_KEY) {
  console.error('ERROR: Supabase URL and Secret Key are required.');
  process.exit(1);
}

const supabase = createClient(SUPABASE_URL, SUPABASE_SECRET_KEY);

/**
 * 1. Cron Job: Runs every hour to lockout demo users older than 2 days.
 * 2 days in milliseconds: 2 * 24 * 60 * 60 * 1000 = 172,800,000 ms.
 */
cron.schedule('0 * * * *', async () => {
  console.log('[Cron] Checking for expired demo accounts...');
  try {
    const twoDaysAgoMillis = Date.now() - (2 * 24 * 60 * 60 * 1000);

    // Fetch active demo users who registered more than 2 days ago
    const { data: users, error } = await supabase
      .from('local_users')
      .select('googleSub, email, registeredAt, isActive')
      .eq('isPremium', false)
      .eq('isActive', true)
      .lt('registeredAt', twoDaysAgoMillis);

    if (error) {
      console.error('[Cron] Error fetching users:', error.message);
      return;
    }

    if (!users || users.length === 0) {
      console.log('[Cron] No expired demo users found.');
      return;
    }

    console.log(`[Cron] Found ${users.length} expired demo accounts. Locking out...`);

    for (const user of users) {
      const { error: updateError } = await supabase
        .from('local_users')
        .update({ isActive: false })
        .eq('googleSub', user.googleSub);

      if (updateError) {
        console.error(`[Cron] Failed to lockout ${user.email}:`, updateError.message);
      } else {
        console.log(`[Cron] Locked out demo user: ${user.email} (Registered at: ${new Date(Number(user.registeredAt)).toLocaleString()})`);
      }
    }
  } catch (err) {
    console.error('[Cron] Unexpected error in cron job:', err.message);
  }
});

// Endpoint: Health Check
app.get('/status', (req, res) => {
  res.json({
    status: 'running',
    timestamp: new Date().toISOString(),
    database: 'connected to Supabase'
  });
});

// Endpoint: Manually Trigger Demo Check (helpful for admin/testing)
app.post('/api/admin/check-demo-lockout', async (req, res) => {
  try {
    const twoDaysAgoMillis = Date.now() - (2 * 24 * 60 * 60 * 1000);

    const { data: users, error } = await supabase
      .from('local_users')
      .select('googleSub, email, registeredAt, isActive')
      .eq('isPremium', false)
      .eq('isActive', true)
      .lt('registeredAt', twoDaysAgoMillis);

    if (error) {
      return res.status(500).json({ error: error.message });
    }

    const lockedOutUsers = [];
    for (const user of users) {
      const { error: updateError } = await supabase
        .from('local_users')
        .update({ isActive: false })
        .eq('googleSub', user.googleSub);

      if (!updateError) {
        lockedOutUsers.push(user.email);
      }
    }

    res.json({
      message: 'Demo lockout check completed manually.',
      checkedCount: users.length,
      lockedOut: lockedOutUsers
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Serve signature files statically
app.use('/api/signatures', express.static(path.join(process.cwd(), 'TTD')));

// Endpoint: Simpan Tanda Tangan Penerima ke Supabase (Lokal VPS)
app.post('/api/invoice/signature', async (req, res) => {
  const { invoiceId, signatureUrl, signatureBase64, receiverName } = req.body;
  if (!invoiceId || !receiverName || (!signatureUrl && !signatureBase64)) {
    return res.status(400).json({ error: 'Missing required parameters: invoiceId, receiverName, and signatureUrl or signatureBase64' });
  }

  try {
    let savedUrl = signatureUrl;

    if (signatureBase64) {
      // Buat folder TTD jika belum ada
      const ttdDir = path.join(process.cwd(), 'TTD');
      if (!fs.existsSync(ttdDir)) {
        fs.mkdirSync(ttdDir, { recursive: true });
      }

      // Dekode gambar base64
      const matches = signatureBase64.match(/^data:([A-Za-z-+\/]+);base64,(.+)$/);
      let buffer;
      if (matches && matches.length === 3) {
        buffer = Buffer.from(matches[2], 'base64');
      } else {
        buffer = Buffer.from(signatureBase64, 'base64');
      }

      // Bersihkan nama penerima untuk nama file yang aman
      const cleanName = receiverName.toLowerCase().replace(/[^a-z0-9]/g, '_').replace(/_+/g, '_').replace(/^_+|_+$/g, '');
      const fileName = `sig_${invoiceId}_${cleanName}.png`;
      const filePath = path.join(ttdDir, fileName);

      fs.writeFileSync(filePath, buffer);
      savedUrl = `https://www.zedmz.cloud/api/signatures/${fileName}`;
    }

    const { data, error } = await supabase
      .from('bmp_invoices')
      .update({
        receiverSignatureUrl: savedUrl,
        receiverNameActual: receiverName,
        updatedAt: Date.now()
      })
      .eq('id', invoiceId);

    if (error) {
      console.error('[Signature] Supabase update error:', error.message);
      return res.status(500).json({ error: error.message });
    }

    console.log(`[Signature] Successfully saved receiver signature for invoice ${invoiceId} (URL: ${savedUrl})`);
    res.json({ message: 'Signature saved successfully', signatureUrl: savedUrl });
  } catch (err) {
    console.error('[Signature] Unexpected error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// Endpoint: AI Command zero-shot classifier
app.post('/api/ai/classify', async (req, res) => {
  const { statement } = req.body;
  if (!statement) {
    return res.status(400).json({ error: 'Missing parameter: statement' });
  }

  const { exec } = require('child_process');
  const detectorPath = path.join(process.cwd(), 'ai_detector.py');
  
  // Escaping single quotes in statement for shell safety
  const safeStatement = statement.replace(/'/g, "'\\''");

  exec(`python3 "${detectorPath}" "${safeStatement}"`, (error, stdout, stderr) => {
    if (error) {
      console.error('[AI] Execution error:', error);
      return res.status(500).json({ error: 'Failed to execute AI classifier', details: error.message });
    }

    try {
      const result = JSON.parse(stdout.trim());
      res.json(result);
    } catch (e) {
      res.json({ category: 'UNKNOWN', confidence: 0.0, raw: stdout.trim() });
    }
  });
});

// Endpoint: Layani halaman Web Tanda Tangan Penerima
app.get('/sign/:token', (req, res) => {
  res.setHeader('Content-Type', 'text/html');
  res.send(SIGNATURE_HTML);
});

app.get('/api/sign/:token', (req, res) => {
  res.setHeader('Content-Type', 'text/html');
  res.send(SIGNATURE_HTML);
});

const SIGNATURE_HTML = `<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tanda Terima Digital - POSBah</title>
    <!-- Google Fonts Inter -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #3B82F6;
            --primary-hover: #2563EB;
            --bg-dark: #0F172A;
            --card-bg: rgba(30, 41, 59, 0.7);
            --border: rgba(255, 255, 255, 0.1);
            --text-main: #F8FAFC;
            --text-muted: #94A3B8;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Inter', sans-serif;
        }

        body {
            background-color: var(--bg-dark);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 16px;
            background-image: radial-gradient(circle at top right, rgba(59, 130, 246, 0.1), transparent),
                              radial-gradient(circle at bottom left, rgba(16, 185, 129, 0.05), transparent);
        }

        .container {
            width: 100%;
            max-width: 450px;
            background: var(--card-bg);
            backdrop-filter: blur(16px);
            border: 1px solid var(--border);
            border-radius: 24px;
            padding: 24px;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
            text-align: center;
        }

        .header {
            margin-bottom: 24px;
        }

        .logo-text {
            font-size: 24px;
            font-weight: 800;
            background: linear-gradient(to right, #3B82F6, #10B981);
            -webkit-background-clip: text;
            background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 4px;
        }

        .subtitle {
            font-size: 14px;
            color: var(--text-muted);
        }

        .form-group {
            text-align: left;
            margin-bottom: 20px;
        }

        label {
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--text-muted);
            margin-bottom: 8px;
            display: block;
        }

        input[type="text"] {
            width: 100%;
            padding: 12px 16px;
            background: rgba(15, 23, 42, 0.6);
            border: 1px solid var(--border);
            border-radius: 12px;
            color: var(--text-main);
            font-size: 15px;
            outline: none;
            transition: border-color 0.2s;
        }

        input[type="text"]:focus {
            border-color: var(--primary);
        }

        .canvas-container {
            position: relative;
            background: #FFFFFF;
            border-radius: 16px;
            overflow: hidden;
            margin-bottom: 20px;
            border: 1px solid var(--border);
        }

        canvas {
            display: block;
            width: 100%;
            height: 200px;
            cursor: crosshair;
        }

        .canvas-actions {
            display: flex;
            justify-content: space-between;
            margin-top: 12px;
        }

        .btn-clear {
            background: transparent;
            color: #EF4444;
            border: 1px solid rgba(239, 68, 68, 0.3);
            padding: 8px 16px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 500;
            cursor: pointer;
            transition: background 0.2s;
        }

        .btn-clear:hover {
            background: rgba(239, 68, 68, 0.1);
        }

        .btn-submit {
            width: 100%;
            background: var(--primary);
            color: white;
            border: none;
            padding: 14px;
            border-radius: 12px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.2s, transform 0.1s;
        }

        .btn-submit:hover {
            background: var(--primary-hover);
        }

        .btn-submit:active {
            transform: scale(0.98);
        }

        .btn-submit:disabled {
            background: rgba(59, 130, 246, 0.4);
            cursor: not-allowed;
            transform: none;
        }

        .status-message {
            margin-top: 16px;
            font-size: 13px;
            color: #10B981;
            display: none;
        }

        .status-message.error {
            color: #EF4444;
        }

        .expiry-banner {
            font-size: 12px;
            color: #F59E0B;
            background: rgba(245, 158, 11, 0.1);
            border: 1px solid rgba(245, 158, 11, 0.2);
            padding: 8px 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            display: inline-block;
        }

        .expiry-banner.expired {
            color: #EF4444;
            background: rgba(239, 68, 68, 0.1);
            border: 1px solid rgba(239, 68, 68, 0.2);
        }

        /* Overlay loading */
        .loading-overlay {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(15, 23, 42, 0.8);
            z-index: 1000;
            justify-content: center;
            align-items: center;
            flex-direction: column;
        }

        .spinner {
            width: 40px;
            height: 40px;
            border: 4px solid rgba(255, 255, 255, 0.1);
            border-left-color: var(--primary);
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin-bottom: 12px;
        }

        @keyframes spin {
            to { transform: rotate(360deg); }
        }
    </style>
</head>
<body>

    <div class="container">
        <div class="header">
            <h1 class="logo-text">POSBah</h1>
            <p class="subtitle">Konfirmasi Tanda Terima Barang</p>
        </div>

        <div id="expiry-tag" class="expiry-banner">
            Mengecek kevalidan link...
        </div>

        <div id="form-container" style="display: none;">
            <div class="form-group">
                <label for="receiver-name">Nama Terang Penerima</label>
                <input type="text" id="receiver-name" placeholder="Masukkan nama Anda..." autocomplete="off">
            </div>

            <label>Tanda Tangan di Bawah</label>
            <div class="canvas-container">
                <canvas id="sig-canvas"></canvas>
            </div>
            
            <div style="margin-bottom: 20px; text-align: left;">
                <button type="button" class="btn-clear" id="clear-btn">Ulangi Corengan</button>
            </div>

            <button type="button" class="btn-submit" id="submit-btn" disabled>Kirim Bukti Tanda Tangan</button>
        </div>

        <div id="status-tag" class="status-message"></div>
    </div>

    <!-- Loading screen overlay -->
    <div class="loading-overlay" id="loader">
        <div class="spinner"></div>
        <p id="loader-text">Memproses tanda tangan...</p>
    </div>

    <script>
        // ─── Cloudinary Config ───
        const CLOUDINARY_CLOUD_NAME = "dkkbizenf";
        const CLOUDINARY_PRESET = "nota_bahan_baku";
        const CLOUDINARY_FOLDER = "posbah/nota_penerima";
        // ───────────────────────────────────────────────────────

        const canvas = document.getElementById("sig-canvas");
        const ctx = canvas.getContext("2d");
        const clearBtn = document.getElementById("clear-btn");
        const submitBtn = document.getElementById("submit-btn");
        const receiverNameInput = document.getElementById("receiver-name");
        const formContainer = document.getElementById("form-container");
        const statusTag = document.getElementById("status-tag");
        const expiryTag = document.getElementById("expiry-tag");
        const loader = document.getElementById("loader");
        const loaderText = document.getElementById("loader-text");

        let isDrawing = false;
        let drawnSomething = false;
        let invoiceId = null;
        let tokenStr = null;

        // Setup responsivitas canvas
        function resizeCanvas() {
            const rect = canvas.getBoundingClientRect();
            canvas.width = rect.width;
            canvas.height = 200;
            
            // Default styling canvas
            ctx.strokeStyle = "#000000";
            ctx.lineWidth = 4;
            ctx.lineCap = "round";
            ctx.lineJoin = "round";
            clearCanvas();
        }

        window.addEventListener("resize", resizeCanvas);

        // Bersihkan canvas
        function clearCanvas() {
            ctx.fillStyle = "#FFFFFF";
            ctx.fillRect(0, 0, canvas.width, canvas.height);
            drawnSomething = false;
            validateForm();
        }

        clearBtn.addEventListener("click", clearCanvas);

        // Deteksi input gambar (Touch & Mouse)
        function getPos(e) {
            const rect = canvas.getBoundingClientRect();
            const clientX = e.touches ? e.touches[0].clientX : e.clientX;
            const clientY = e.touches ? e.touches[0].clientY : e.clientY;
            return {
                x: clientX - rect.left,
                y: clientY - rect.top
            };
        }

        // Touch start
        function startDrawing(e) {
            e.preventDefault();
            isDrawing = true;
            const pos = getPos(e);
            ctx.beginPath();
            ctx.moveTo(pos.x, pos.y);
        }

        // Touch move
        function draw(e) {
            if (!isDrawing) return;
            e.preventDefault();
            const pos = getPos(e);
            ctx.lineTo(pos.x, pos.y);
            ctx.stroke();
            drawnSomething = true;
            validateForm();
        }

        // Touch end
        function stopDrawing() {
            isDrawing = false;
        }

        canvas.addEventListener("mousedown", startDrawing);
        canvas.addEventListener("mousemove", draw);
        canvas.addEventListener("mouseup", stopDrawing);
        canvas.addEventListener("mouseleave", stopDrawing);

        canvas.addEventListener("touchstart", startDrawing);
        canvas.addEventListener("touchmove", draw);
        canvas.addEventListener("touchend", stopDrawing);

        // Form validation
        function validateForm() {
            const name = receiverNameInput.value.trim();
            submitBtn.disabled = !(name.length > 1 && drawnSomething);
        }

        receiverNameInput.addEventListener("input", validateForm);

        // Parser Token URL
        function parseUrlToken() {
            // Cek dulu apakah ada query parameter 'token'
            const urlParams = new URLSearchParams(window.location.search);
            let token = urlParams.get("token");
            
            // Jika tidak ada, baru fallback ke path parameter (segmen terakhir URL)
            if (!token) {
                const pathParts = window.location.pathname.split("/");
                token = pathParts[pathParts.length - 1];
            }

            if (!token || token === "signature_receiver_web.html" || token === "sign" || token === "") {
                showExpiryError("Token tidak ditemukan di URL. Minta pengirim membagikan ulang link.");
                return;
            }

            tokenStr = token;
            decodeAndVerifyToken(token);
        }

        // Dekode Token & Verifikasi Kadaluarsa
        function decodeAndVerifyToken(tokenEncoded) {
            try {
                // Decode base64 URL safe
                let base64 = tokenEncoded.replace(/-/g, "+").replace(/_/g, "/");
                while (base64.length % 4) {
                    base64 += "=";
                }
                let tokenRaw = atob(base64);
                const parts = tokenRaw.split(":");
                if (parts.length < 2) {
                    showExpiryError("Format token tidak valid.");
                    return;
                }

                invoiceId = parts[0];
                const expiry = parseInt(parts[1]);
                
                // Cek kadaluarsa
                const now = Date.now();
                if (now > expiry) {
                    showExpiryError("Link ini telah kadaluarsa. Minta pengirim membuat link baru.");
                    return;
                }

                // Sukses — Link Valid
                const timeLeftSec = Math.round((expiry - now) / 1000);
                startCountdown(timeLeftSec);
                
                formContainer.style.display = "block";
                resizeCanvas();

            } catch (e) {
                showExpiryError("Gagal mendekripsi token. Pastikan link yang Anda gunakan valid.");
            }
        }

        function showExpiryError(msg) {
            expiryTag.className = "expiry-banner expired";
            expiryTag.innerText = "⚠️ LINK KADALUARSA ATAU ERROR";
            formContainer.style.display = "none";
            showStatus(msg, true);
        }

        function startCountdown(durationSeconds) {
            let sec = durationSeconds;
            expiryTag.innerText = "⏳ Link aktif selama " + sec + " detik lagi";
            const timer = setInterval(function() {
                sec--;
                if (sec <= 0) {
                    clearInterval(timer);
                    showExpiryError("Waktu habis! Link kadaluarsa.");
                } else {
                    expiryTag.innerText = "⏳ Link aktif selama " + sec + " detik lagi";
                }
            }, 1000);
        }

        function showStatus(msg, isError) {
            statusTag.style.display = "block";
            statusTag.innerText = msg;
            if (isError) {
                statusTag.className = "status-message error";
            } else {
                statusTag.className = "status-message";
            }
        }

        // Kirim tanda tangan dalam format Base64 langsung ke server VPS
        submitBtn.addEventListener("click", async function() {
            const name = receiverNameInput.value.trim();
            if (!name || !drawnSomething || !invoiceId) return;

            showLoader("Menyimpan tanda tangan...");

            try {
                // 1. Ekspor canvas ke Base64 (data URL)
                const signatureBase64 = canvas.toDataURL("image/png");

                // 2. Kirim ke API server backend
                const apiRes = await fetch("/api/invoice/signature", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        invoiceId: invoiceId,
                        token: tokenStr,
                        signatureBase64: signatureBase64,
                        receiverName: name
                    })
                });

                hideLoader();

                if (apiRes.ok || apiRes.status === 404) {
                    formContainer.style.display = "none";
                    expiryTag.style.display = "none";
                    showStatus("Tanda terima berhasil dikirim! Terima kasih, " + name + ".");
                } else {
                    const errText = await apiRes.text();
                    throw new Error(errText || "Gagal memproses tanda tangan di database server.");
                }

            } catch (e) {
                hideLoader();
                showStatus("Gagal mengirim: " + e.message, true);
            }
        });

        function showLoader(text) {
            loaderText.innerText = text;
            loader.style.display = "flex";
        }

        function hideLoader() {
            loader.style.display = "none";
        }

        // Jalankan parser saat halaman di-load
        window.onload = parseUrlToken;
    </script>
</body>
</html>
`;

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`PosBah Backend Server is listening on port ${PORT}`);
});
