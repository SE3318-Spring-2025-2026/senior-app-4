import axios from 'axios';
import { showToast } from '../components/toast/ToastContext';

// Create API Client with Base URL
const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Configure Global Error Handling Interceptor (Issue 321)
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
          showToast("Session expired. Please log in again.", 'error');
          window.location.href = '/login';
          break;
        case 403:
          showToast("You don't have permission", 'error');
          break;
        case 404:
          showToast("Resource not found", 'error');
          break;
        case 409:
          showToast(`Conflict: ${responseData?.message || "Data conflict occurred"}`, 'error');
          break;
        case 500:
          // 500 triggers an error toast with action (Retry functionality can be intercepted by UI)
          showToast("Server error. Please try again later.", 'error');
          break;
        case 400: {
          // Validation error fallback
          const errorMessage = responseData?.message || responseData?.error || "Bad Request";
          showToast(errorMessage, 'warning');
          break;
        }
        default:
          showToast(responseData?.message || "An unexpected error occurred", 'error');
      }
    } else if (error.request) {
      showToast("Unable to connect. Check your connection.", 'error');
    } else {
      showToast(error.message || "An unexpected error occurred", 'error');
    }

    return Promise.reject(error);
  }
);

export default apiClient;
