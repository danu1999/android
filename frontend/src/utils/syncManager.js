import { getQueuedTransactions, removeQueuedTransaction } from './offlineDb';
import api from '../api';

let isSyncing = false;

export async function syncOfflineTransactions() {
  if (isSyncing) return;
  if (!navigator.onLine) return;

  try {
    const queue = await getQueuedTransactions();
    if (queue.length === 0) return;

    isSyncing = true;
    console.log(`[SyncManager] Syncing ${queue.length} offline transactions...`);

    // Dispatch status to show sync indicator in UI
    window.dispatchEvent(new CustomEvent('posbah_sync_status', { 
      detail: { status: 'syncing', count: queue.length } 
    }));

    let successCount = 0;
    let errorCount = 0;

    for (const item of queue) {
      try {
        const { type, payload } = item;
        
        if (type === 'SALES') {
          // Kirim transaksi retail/FNB ke server
          await api.post('/transactions', payload, {
            headers: { 'x-offline-sync': 'true' }
          });
        } else if (type === 'LAUNDRY') {
          // Kirim order laundry ke server
          await api.post('/laundry/orders', payload, {
            headers: { 'x-offline-sync': 'true' }
          });
        }

        // Hapus dari antrean offline setelah berhasil disinkronkan
        await removeQueuedTransaction(item.id);
        successCount++;
      } catch (err) {
        console.error('[SyncManager] Error syncing transaction:', item, err);
        
        // Jika error client (400 Bad Request, dll), abaikan untuk mencegah antrean macet
        if (err.response && err.response.status >= 400 && err.response.status < 500) {
          await removeQueuedTransaction(item.id);
          errorCount++;
        } else {
          // Jika network error atau server down, hentikan loop sync dan coba lagi nanti
          break;
        }
      }
    }

    isSyncing = false;
    
    // Kirim event selesai sinkronisasi untuk refresh data di halaman dashboard/kasir
    window.dispatchEvent(new CustomEvent('posbah_sync_status', { 
      detail: { 
        status: 'idle', 
        successCount, 
        errorCount,
        hasRemaining: false
      } 
    }));

    if (successCount > 0 || errorCount > 0) {
      window.dispatchEvent(new CustomEvent('posbah_show_toast', {
        detail: {
          message: errorCount === 0
            ? `✅ Berhasil sinkronisasi ${successCount} transaksi offline.`
            : `⚠️ Sinkronisasi selesai: ${successCount} sukses, ${errorCount} gagal.`,
          type: errorCount === 0 ? 'success' : 'warning'
        }
      }));
    }
  } catch (error) {
    console.error('[SyncManager] Failed in sync loop:', error);
    isSyncing = false;
  }
}

// Menginisialisasi listener koneksi
export function initSyncManager() {
  // Listen event online browser
  window.addEventListener('online', () => {
    console.log('[SyncManager] Koneksi kembali aktif. Memulai sinkronisasi otomatis...');
    syncOfflineTransactions();
  });

  // Interval check setiap 30 detik
  setInterval(() => {
    if (navigator.onLine) {
      syncOfflineTransactions();
    }
  }, 30000);

  // Jalankan sinkronisasi pertama kali jika online
  if (navigator.onLine) {
    syncOfflineTransactions();
  }
}
