import { toast } from "sonner";

export class ApiError extends Error {
  status: number;
  data: any;

  constructor(status: number, data: any, message: string) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

/**
 * A wrapper around native fetch that intercepts and handles global API errors
 * precisely according to Issue 321 Acceptance Criteria.
 */
export const apiClient = async (endpoint: string, options: RequestInit = {}) => {
  const url = endpoint.startsWith('http') 
    ? endpoint 
    : `${process.env.NEXT_PUBLIC_API_URL || '/api/v1'}${endpoint}`;

  try {
    const response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });

    if (!response.ok) {
      let data: any = {};
      try {
        data = await response.json();
      } catch (e) {
        // Response might not be JSON
      }

      const status = response.status;
      const apiMessage = data?.message || data?.error || "Unknown error";

      // Global Interceptor Handling
      switch (status) {
        case 401:
          toast.error("Session expired. Please log in again.");
          if (typeof window !== 'undefined') {
            window.location.href = '/login';
          }
          break;
        case 403:
          toast.error("You don't have permission");
          break;
        case 404:
          toast.error("Resource not found");
          break;
        case 409:
          toast.error(`Conflict: ${apiMessage}`);
          break;
        case 400:
          // Fallback for form validations etc (handled by UI components, but intercepted if needed)
          toast.error(apiMessage);
          break;
        case 500:
          toast.error("Server error. Please try again later.", {
            action: {
              label: 'Retry',
              onClick: () => window.location.reload()
            }
          });
          break;
        default:
          toast.error(apiMessage || "An unexpected error occurred");
      }

      throw new ApiError(status, data, apiMessage);
    }

    // For 204 No Content, return null
    if (response.status === 204) {
      return null;
    }

    return await response.json();
  } catch (error: any) {
    // If error is ApiError, we already handled the toast in the switch statement
    if (error instanceof ApiError) {
      throw error;
    }

    // Network errors (fetch throws a TypeError for network failures)
    if (error.name === 'TypeError' && error.message === 'Failed to fetch') {
      toast.error("Unable to connect. Check your connection.", {
        action: {
          label: 'Retry',
          onClick: () => window.location.reload()
        }
      });
    } else {
      // Fallback for other JS errors
      toast.error(error.message || "An unexpected error occurred");
    }

    throw error;
  }
};
