import axios from 'axios';

const isCapacitor = !!window.Capacitor || window.location.protocol === 'capacitor:';
const isLocalDev = !isCapacitor && (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') && window.location.port !== '';

export const getBmpApiUrl = () => {
  if (import.meta.env.VITE_API_URL_BMP) {
    return import.meta.env.VITE_API_URL_BMP;
  }
  
  const isCapacitor = !!window.Capacitor || window.location.protocol === 'capacitor:';
  const isLocalDev = !isCapacitor && (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') && window.location.port !== '';
  
  if (isLocalDev) {
    return 'http://localhost:8080/api';
  }
  
  try {
    const userStr = localStorage.getItem('posbah_user');
    if (userStr) {
      const user = JSON.parse(userStr);
      if (user && user.isDemo) {
        return '/api-bmp-demo';
      }
    }
  } catch (e) {
    console.warn('Failed to dynamically check demo mode for BMP URL:', e);
  }
  
  return '/api-bmp';
};

export const API_URL = getBmpApiUrl();

const api = axios.create({
  baseURL: API_URL,
});

// Interceptor to add auth token and dynamic baseURL
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  
  // Dynamically switch baseURL on request runtime
  config.baseURL = getBmpApiUrl();
  
  return config;
});

// Interceptor to handle global errors (like 401 Unauthorized, 403 Forbidden)
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
