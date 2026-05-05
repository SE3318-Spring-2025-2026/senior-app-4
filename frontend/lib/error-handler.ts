import { toast } from "sonner";

export const ERROR_MESSAGES = {
    DUPLICATE: "This person is already assigned to this role.",
    FORBIDDEN: "You do not have permission to perform this action.",
    NOT_FOUND: "The requested resource was not found.",
    NETWORK: "Network error. Please check your connection.",
    GENERIC: "An unexpected error occurred. Please try again.",
} as const;

export function getErrorMessage(err: any, context?: string): string {
    const status = err.response?.status || err.status;
    const prefix = context ? `${context}: ` : "";

    if (status === 409) return `${prefix}${ERROR_MESSAGES.DUPLICATE}`;
    if (status === 403) return `${prefix}${ERROR_MESSAGES.FORBIDDEN}`;
    if (status === 404) return `${prefix}${ERROR_MESSAGES.NOT_FOUND}`;
    
    return `${prefix}${err.message || ERROR_MESSAGES.GENERIC}`;
}

export function handleApiError(err: any, context?: string) {
    const message = getErrorMessage(err, context);
    toast.error(message);
    if (process.env.NODE_ENV === "development") {
        console.error(`[API Error] ${context}:`, err);
    }
}