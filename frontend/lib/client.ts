import axios from 'axios';
import { showToast } from '../components/toast/ToastContext';
import { getToken } from './auth';

// Create API Client with Base URL
const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add Authorization Header to Requests
apiClient.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Configure Global Error Handling Interceptor
apiClient.interceptors.response.use(
  (response) => {
    return response; // Pass through successful responses
  },
  (error: any) => {
    if (error.response) {
      const status = error.response.status;
      const responseData = error.response.data;

      switch (status) {
        case 401:
          // Redirect to /auth/login
          window.location.href = '/auth/login';
          break;
        case 403:
          showToast("You don't have permission", 'error');
          break;
        case 404:
          showToast("Resource not found", 'error');
          break;
        case 400: {
          // Extract specific API error message if available, fallback otherwise
          const errorMessage = responseData?.message || responseData?.error || "Bad Request";
          showToast(errorMessage, 'error');
          break;
        }
        case 500:
          showToast("Server error, please try again", 'error');
          break;
        default:
          showToast(responseData?.message || "An unexpected error occurred", 'error');
      }
    } else if (error.request) {
      showToast("Network error. Please check your connection.", 'error');
    } else {
      showToast(error.message || "An unexpected error occurred", 'error');
    }

    return Promise.reject(error);
  }
);

export default apiClient;
