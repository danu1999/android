import subprocess
import time

def run_query_stdin(sql):
    ssh_cmd = [
        "ssh", "-i", "C:\\Users\\danus\\Documents\\muizz.pem",
        "-o", "StrictHostKeyChecking=no",
        "muizz9900@zedmz.cloud",
        "psql postgres://postgres:Bahtera1!@localhost:5432/posbah?sslmode=disable"
    ]
    res = subprocess.run(ssh_cmd, input=sql, capture_output=True, text=True)
    return res.stdout, res.stderr, res.returncode

desc = """POSBah v2.17.54 - Perbaikan Pengaturan Cetak JPG & Pemutakhiran Sinkronisasi Real-Time

Halo Kak! POSBah v2.17.54 hadir dengan perbaikan konfigurasi cetak dan peningkatan konektivitas real-time ke VPS:
1. Perbaikan Bug Pengaturan Cetak: Memperbaiki kendala kegagalan simpan data pengaturan cetak (JPG, Surat Jalan, Invoice PDF, dan rekening bank) sehingga data terupdate dengan andal dan tidak berubah kembali.
2. Informasi Pembayaran pada JPG: Menambahkan input informasi bank/e-wallet beserta nama pemilik langsung pada tab pengaturan Cetak JPG agar memudahkan pencantuman detail transfer di dokumen gambar yang diekspor.
3. Sinkronisasi Lintas Layer & Interkoneksi: Mengoptimalkan integritas data cetak luring real-time dari database VPS PostgreSQL untuk semua perangkat kasir secara instan.

Silakan klik tombol "Unduh APK Sekarang" di layar untuk memperbarui aplikasi Anda ya!"""

# Escape single quotes for SQL
desc_escaped = desc.replace("'", "''")
now_ms = int(time.time() * 1000)

sql = f"""UPDATE apk_config 
SET version = '2.17.54', 
    description = '{desc_escaped}', 
    "updatedAt" = {now_ms} 
WHERE id = 1;"""

out, err, code = run_query_stdin(sql)
print("Update Output:")
print(out)
if err:
    print("Error:")
    print(err)

out_verify, err_verify, code_verify = run_query_stdin("SELECT * FROM apk_config;")
print("Verification:")
print(out_verify)
