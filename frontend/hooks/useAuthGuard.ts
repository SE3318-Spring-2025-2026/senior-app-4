"use client";
import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { getToken, getUser } from "@/lib/auth";

export type AuthGuardStatus = "loading" | "authorized" | "denied";

export function useAuthGuard(requiredRole: string | string[]): AuthGuardStatus {
    const router = useRouter();
    const [status, setStatus] = useState<AuthGuardStatus>("loading");
    const requiredRoleKey = Array.isArray(requiredRole) ? requiredRole.join("|") : requiredRole;

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
        const allowedRoles = requiredRoleKey.split("|");
        queueMicrotask(() => {
            setStatus(allowedRoles.includes(user.role) ? "authorized" : "denied");
        });
    }, [router, requiredRoleKey]);

    return status;
}
