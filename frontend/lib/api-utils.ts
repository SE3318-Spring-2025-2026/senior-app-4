import { getToken } from "@/lib/auth";

export const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

export async function parseError(res: Response): Promise<string> {
    try {
        const data = await res.json();
        return data?.message || data?.error || "Request failed.";
    } catch {
        return "Request failed.";
    }
}

export function buildHeaders(): Record<string, string> {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    const token = getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;
    return headers;
}
