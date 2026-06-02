import axios from 'axios';

// API Bonus — tidak memerlukan JWT token (endpoint public)
const api = axios.create({
  baseURL: 'https://bmp.up.railway.app/api',
  timeout: 15000,
});

export default api;
