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

desc = """POSBah v2.17.55 - Pembaruan Ukuran Cetak Nota Traditional & Perbaikan Info Rekening

Halo Kak! POSBah v2.17.55 hadir dengan pembaruan cetak nota tradisional dan peningkatan kestabilan data:
1. Pembaruan Ukuran Cetak Tradisional (Continuous Form): Ukuran cetak Invoice dan Surat Jalan untuk kertas traditional / continuous form kini telah disesuaikan menjadi 9.5" x 11" (240mm x 279mm). Tata letak, logo, nama toko, serta tanda tangan penerima/pengirim kini diatur otomatis agar tampil lebih rapi, proporsional, dan tidak saling menumpuk.
2. Perbaikan Info Rekening & Kolom Tanda Tangan: Mengatasi kendala data Info Pembayaran (rekening bank) dan memunculkan kotak kolom tanda tangan putus-putus untuk tanda tangan fisik jika dicetak kosong secara luring dari website, lengkap dengan kalimat sertifikasi keabsahan cetak luring oleh POSBah.
3. Kestabilan Cetak Offline & Sinkronisasi Tanda Tangan: Mengoptimalkan pemuatan tanda tangan penerima di aplikasi HP agar tetap muncul secara offline/luring menggunakan penyimpanan lokal HP. Sedangkan untuk cetak melalui website/browser, tanda tangan akan selalu dimuat langsung secara online dari server VPS utama agar data tanda tangan pelanggan yang tersimpan terpusat selalu sinkron.

Silakan klik tombol "Unduh APK Sekarang" di layar untuk memperbarui aplikasi Anda ya!"""

# Escape single quotes for SQL
desc_escaped = desc.replace("'", "''")
now_ms = int(time.time() * 1000)

sql = f"""UPDATE apk_config 
SET version = '2.17.55', 
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
