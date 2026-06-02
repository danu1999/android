import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'https://bmp.up.railway.app/api',
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
        window.location.href = '/login';
      } else if (error.response.status === 403) {
        window.location.href = '/access-denied';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
