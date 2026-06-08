import axios from 'axios';

export const getBmpApiUrl = () => {
  const isCapacitor = (!!window.Capacitor && window.Capacitor.getPlatform && window.Capacitor.getPlatform() !== 'web') || window.location.protocol === 'capacitor:';
  const isLocalDev = !isCapacitor && (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') && window.location.port !== '';
  
  if (isLocalDev) {
    return 'http://localhost:3001/api/bmp';
  }
  
  const base = isCapacitor ? 'https://www.zedmz.cloud' : '';
  return `${base}/api/bmp`;
};

export const API_URL = getBmpApiUrl();

const api = axios.create({
  baseURL: API_URL,
});

// Interceptor to add auth token, dynamic headers, and dynamic baseURL
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  
  try {
    const stored = localStorage.getItem('posbah_user');
    if (stored) {
      const user = JSON.parse(stored);
      if (user?.id !== undefined && user?.id !== null) config.headers['x-employee-id'] = String(user.id);
      if (user?.role) config.headers['x-employee-role'] = user.role;
      if (user?.name) config.headers['x-employee-name'] = encodeURIComponent(user.name);
    }
    const appMode = localStorage.getItem('posbah_app_mode') || 'FNB';
    config.headers['x-app-mode'] = appMode;

    const tenantId = localStorage.getItem('posbah_tenant_id');
    if (tenantId) {
      config.headers['x-tenant-id'] = tenantId;
    }

    const activeOutletId = localStorage.getItem('posbah_active_outlet_id');
    if (activeOutletId) {
      config.headers['x-outlet-id'] = activeOutletId;
    }
  } catch (_) {}
  
  config.baseURL = getBmpApiUrl();
  return config;
});

// Interceptor to handle global errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      if (error.response.status === 401) {
        localStorage.removeItem('token');
        window.location.href = '/';
      } else if (error.response.status === 403) {
        window.location.href = '/';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
