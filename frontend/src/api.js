import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:3001/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Inject user ID & role ke setiap request agar backend bisa validasi akses
api.interceptors.request.use((config) => {
  try {
    const stored = localStorage.getItem('posbah_user');
    if (stored) {
      const user = JSON.parse(stored);
      if (user?.id !== undefined && user?.id !== null) config.headers['x-employee-id'] = String(user.id);
      if (user?.role) config.headers['x-employee-role'] = user.role;
    }
  } catch (_) {}
  return config;
});

export default api;
