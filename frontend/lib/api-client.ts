import axios from 'axios';
import { getToken, clearAuth } from '@/lib/auth';
import { toast } from 'sonner';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:5000/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';

    if (status === 401) {
      clearAuth();
      window.location.href = '/auth/login';
    } else if (status === 403) {
      window.location.href = '/auth/access-denied';
    } else if (status === 404 && url.includes('/active-sprint')) {
      console.warn('No Active Sprint Registered');
    } else if (status >= 500) {
      toast.error('Server Failure Occurred');
    }

    return Promise.reject(error);
  }
);

export default apiClient;