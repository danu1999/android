import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'
import { initSyncManager } from './utils/syncManager'

// Global Client Error Catchers
window.onerror = function (message, source, lineno, colno, error) {
  try {
    const user = JSON.parse(localStorage.getItem('posbah_user') || 'null');
    const isCapacitor = (!!window.Capacitor && window.Capacitor.getPlatform && window.Capacitor.getPlatform() !== 'web') || window.location.protocol === 'capacitor:';
    const base = isCapacitor ? 'https://www.zedmz.cloud' : '';

    fetch(`${base}/api/logs/client-error`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        errorMsg: String(message),
        stack: error ? error.stack : `${source}:${lineno}:${colno}`,
        url: window.location.href,
        userAgent: navigator.userAgent,
        userEmail: user ? user.email : null
      })
    }).catch(e => console.warn('[Reporter] Gagal kirim error log:', e));
  } catch (e) {
    console.error('[Reporter] Error in logger event:', e);
  }
};

window.addEventListener('unhandledrejection', function (event) {
  try {
    const message = event.reason ? (event.reason.message || String(event.reason)) : 'Unhandled rejection';
    const stack = event.reason ? event.reason.stack : null;
    const user = JSON.parse(localStorage.getItem('posbah_user') || 'null');
    const isCapacitor = (!!window.Capacitor && window.Capacitor.getPlatform && window.Capacitor.getPlatform() !== 'web') || window.location.protocol === 'capacitor:';
    const base = isCapacitor ? 'https://www.zedmz.cloud' : '';

    fetch(`${base}/api/logs/client-error`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        errorMsg: message,
        stack: stack,
        url: window.location.href,
        userAgent: navigator.userAgent,
        userEmail: user ? user.email : null
      })
    }).catch(e => console.warn('[Reporter] Gagal kirim rejection log:', e));
  } catch (e) {
    console.error('[Reporter] Error in rejection logger event:', e);
  }
});

initSyncManager();

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
