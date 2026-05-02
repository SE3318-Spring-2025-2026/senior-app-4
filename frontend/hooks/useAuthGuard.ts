"use client";
import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { getToken, getUser } from "@/lib/auth";

export type AuthGuardStatus = "loading" | "authorized" | "denied";

export function useAuthGuard(requiredRole: string): AuthGuardStatus {
    const router = useRouter();
    const [status, setStatus] = useState<AuthGuardStatus>("loading");

    useEffect(() => {
        const token = getToken();
        const user = getUser();
        if (!token || !user) {
            router.replace("/auth/login");
            return;
        }
        if (user.requiresPasswordChange) {
            router.replace("/auth/change-password");
            return;
        }
        setStatus(user.role === requiredRole ? "authorized" : "denied");
    }, [router, requiredRole]);

    return status;
}
