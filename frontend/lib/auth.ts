const TOKEN_KEY = "spms_token";
const USER_KEY = "spms_user";

export interface AuthUser {
  userId?: number;
  studentId?: string;
  githubUsername?: string;
  role: string;
  requiresPasswordChange: boolean;
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function getUser(): AuthUser | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function setAuth(token: string, user: AuthUser): void {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function isAuthenticated(): boolean {
  return !!getToken();
}

/** JWT payload'ını decode eder (imza doğrulaması yapmaz — sadece frontend için) */
export function decodeToken(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split(".")[1];
    const decoded = atob(payload.replaceAll("-", "+").replaceAll("_", "/"));
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}
