const DB_NAME = 'posbah_offline_db';
const DB_VERSION = 1;

export function initDb() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onerror = (event) => {
      console.error('IndexedDB open error:', event.target.error);
      reject(event.target.error);
    };

    request.onsuccess = (event) => {
      resolve(event.target.result);
    };

    request.onupgradeneeded = (event) => {
      const db = event.target.result;
      
      // Store untuk caching produk
      if (!db.objectStoreNames.contains('products')) {
        db.createObjectStore('products', { keyPath: 'id' });
      }

      // Store untuk caching pelanggan
      if (!db.objectStoreNames.contains('customers')) {
        db.createObjectStore('customers', { keyPath: 'id' });
      }

      // Store untuk antrean transaksi offline (FNB POS maupun Laundry)
      if (!db.objectStoreNames.contains('transactions_queue')) {
        db.createObjectStore('transactions_queue', { keyPath: 'id', autoIncrement: true });
      }
    };
  });
}

// ─────────────────────────────────────────────────────────────
// PRODUK
// ─────────────────────────────────────────────────────────────
export async function cacheProducts(products) {
  const db = await initDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(['products'], 'readwrite');
    const store = transaction.objectStore('products');

    const clearRequest = store.clear();
    clearRequest.onsuccess = () => {
      if (!products || products.length === 0) {
        resolve();
        return;
      }

      let completed = 0;
      let failed = false;

      products.forEach((product) => {
        const req = store.put(product);
        req.onsuccess = () => {
          completed++;
          if (completed === products.length && !failed) {
            resolve();
          }
        };
        req.onerror = (e) => {
          failed = true;
          reject(e.target.error);
        };
      });
    };

    clearRequest.onerror = (e) => {
      reject(e.target.error);
    };
  });
}

export async function getProducts() {
  const db = await initDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(['products'], 'readonly');
    const store = transaction.objectStore('products');
    const request = store.getAll();

    request.onsuccess = () => {
      resolve(request.result);
    };

    request.onerror = (e) => {
      reject(e.target.error);
    };
  });
}

// ─────────────────────────────────────────────────────────────
// PELANGGAN
// ─────────────────────────────────────────────────────────────
export async function cacheCustomers(customers) {
  const db = await initDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(['customers'], 'readwrite');
    const store = transaction.objectStore('customers');

    const clearRequest = store.clear();
    clearRequest.onsuccess = () => {
      if (!customers || customers.length === 0) {
        resolve();
        return;
      }

      let completed = 0;
      let failed = false;

      customers.forEach((customer) => {
        const req = store.put(customer);
        req.onsuccess = () => {
          completed++;
          if (completed === customers.length && !failed) {
            resolve();
          }
        };
        req.onerror = (e) => {
          failed = true;
          reject(e.target.error);
        };
      });
    };

    clearRequest.onerror = (e) => {
      reject(e.target.error);
    };
  });
}

export async function getCustomers() {
  const db = await initDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(['customers'], 'readonly');
    const store = transaction.objectStore('customers');
    const request = store.getAll();

    request.onsuccess = () => {
      resolve(request.result);
    };

    request.onerror = (e) => {
      reject(e.target.error);
    };
  });
}

// ─────────────────────────────────────────────────────────────
// ANTRIAN TRANSAKSI OFFLINE
// ─────────────────────────────────────────────────────────────
export async function queueTransaction(type, payload) {
  const db = await initDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(['transactions_queue'], 'readwrite');
    const store = transaction.objectStore('transactions_queue');
    
    const item = {
      type, // 'SALES' atau 'LAUNDRY'
      payload,
      timestamp: Date.now()
    };

    const request = store.add(item);

    request.onsuccess = () => {
      resolve(request.result); // Mengembalikan auto-incremented ID antrean
    };

    request.onerror = (e) => {
      reject(e.target.error);
    };
  });
}

export async function getQueuedTransactions() {
  const db = await initDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(['transactions_queue'], 'readonly');
    const store = transaction.objectStore('transactions_queue');
    const request = store.getAll();

    request.onsuccess = () => {
      resolve(request.result);
    };

    request.onerror = (e) => {
      reject(e.target.error);
    };
  });
}

export async function removeQueuedTransaction(id) {
  const db = await initDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(['transactions_queue'], 'readwrite');
    const store = transaction.objectStore('transactions_queue');
    const request = store.delete(id);

    request.onsuccess = () => {
      resolve();
    };

    request.onerror = (e) => {
      reject(e.target.error);
    };
  });
}
