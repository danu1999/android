import axios from 'axios';

const isCapacitor = !!window.Capacitor || window.location.protocol === 'capacitor:';
const isLocalDev = !isCapacitor && (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') && window.location.port !== '';

export const API_URL = import.meta.env.VITE_API_URL_BMP || (isLocalDev ? 'http://localhost:8080/api' : '/api-bmp');

const api = axios.create({
  baseURL: API_URL,
});

// Interceptor to add auth token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
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
