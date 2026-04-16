import axios from 'axios';
import { toast } from 'sonner';

// Create API Client with Base URL
const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

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
          // Redirect to /login
          window.location.href = '/login';
          break;
        case 403:
          toast.error("You don't have permission");
          break;
        case 404:
          toast.error("Resource not found");
          break;
        case 400: {
          // Extract specific API error message if available, fallback otherwise
          const errorMessage = responseData?.message || responseData?.error || "Bad Request";
          toast.error(errorMessage);
          break;
        }
        case 500:
          toast.error("Server error, please try again");
          break;
        default:
          toast.error(responseData?.message || "An unexpected error occurred");
      }
    } else if (error.request) {
      toast.error("Network error. Please check your connection.");
    } else {
      toast.error(error.message || "An unexpected error occurred");
    }

    return Promise.reject(error);
  }
);

export default apiClient;
