import axios from 'axios';
import { showToast } from '../components/toast/ToastContext';

const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const status = error.response.status;
      const responseData = error.response.data;

      switch (status) {
        case 401:
          showToast('Unauthorized', 'error');
          break;
        case 403:
          showToast("You don't have permission", 'error');
          break;
        case 404:
          showToast('Resource not found', 'error');
          break;
        case 400: {
          const errorMessage =
            responseData?.message || responseData?.error || 'Bad Request';
          showToast(errorMessage, 'error');
          break;
        }
        case 500:
          showToast('Server error, please try again', 'error');
          break;
        default:
          showToast(
            responseData?.message || 'An unexpected error occurred',
            'error'
          );
      }
    } else if (error.request) {
      showToast('Network error. Please check your connection.', 'error');
    } else {
      showToast(error.message || 'An unexpected error occurred', 'error');
    }

    return Promise.reject(error);
  }
);

export default apiClient;