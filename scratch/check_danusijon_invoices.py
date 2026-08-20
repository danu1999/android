import requests
import json
from datetime import datetime

headers = {'Authorization': 'Bearer danusijon@gmail.com', 'x-client-version': '2.19.72'}
invoices = requests.get('https://www.zedmz.cloud/api/rt/bmp/invoices', headers=headers).json()
payments = requests.get('https://www.zedmz.cloud/api/rt/bmp/payments', headers=headers).json()
clients = requests.get('https://www.zedmz.cloud/api/rt/bmp/clients', headers=headers).json()

client_map = {c['id']: c.get('clientName', c.get('name', '-')) for c in clients}

print(f"=== REKAP LENGKAP INVOICE & PEMBAYARAN AKUN DANUSIJON@GMAIL.COM ===")
print(f"Total Invoices Terdaftar: {len(invoices)}")
print(f"Total Baris Pembayaran di bmp_invoice_payments: {len(payments)}\n")

for inv in invoices:
    inv_id = inv.get('id')
    inv_num = inv.get('number')
    c_id = inv.get('clientId')
    c_name = client_map.get(c_id, f"Client #{c_id}")
    tot = float(inv.get('totalAmount', 0))
    paid = float(inv.get('paidAmount', 0))
    status = inv.get('status')
    created_ms = inv.get('createdAt', 0)
    created_str = datetime.fromtimestamp(created_ms / 1000).strftime('%Y-%m-%d %H:%M') if created_ms else '-'
    
    inv_pays = [p for p in payments if p.get('invoiceId') == inv_id and not p.get('isDeleted', False)]
    sum_pays = sum(float(p.get('paymentAmount', 0)) for p in inv_pays)
    sisa = tot - sum_pays
    
    print(f"Invoice ID: {inv_id} | Nomor: #{inv_num} | Klien: {c_name} | Tanggal: {created_str}")
    print(f"  - Total Tagihan  : Rp {tot:,.0f}")
    print(f"  - Total Dibayar  : Rp {paid:,.0f} (Header) vs Rp {sum_pays:,.0f} (Detail Payments)")
    print(f"  - Sisa Tagihan   : Rp {sisa:,.0f}")
    print(f"  - Status Invoice : {status}")
    print(f"  - Riwayat Pembayaran ({len(inv_pays)} entri):")
    if inv_pays:
        for p in inv_pays:
            p_date_ms = p.get('paymentDate', 0)
            p_date_str = datetime.fromtimestamp(p_date_ms / 1000).strftime('%Y-%m-%d %H:%M') if p_date_ms else '-'
            p_amt = float(p.get('paymentAmount', 0))
            p_mth = p.get('paymentMethod', '-')
            p_notes = p.get('notes', '-')
            print(f"      * Payment ID {p.get('id')}: Rp {p_amt:,.0f} [{p_mth}] - {p_date_str} - Catatan: {p_notes}")
    else:
        print("      * (Belum ada pembayaran / Invoice ini UNPAID / Belum bayar DP)")
    
    # Check discrepancy
    if abs(paid - sum_pays) > 0.01:
        print(f"  [PERINGATAN]: Mismatch terdeteksi! Header {paid:,.0f} != Detail {sum_pays:,.0f}")
    elif status == "PAID" and len(inv_pays) == 0 and tot > 0:
        print(f"  [PERINGATAN]: Status PAID tapi 0 baris pembayaran!")
    else:
        print(f"  [STATUS RELASI]: 100% SINKRON & VALID")
    print("=" * 70)
