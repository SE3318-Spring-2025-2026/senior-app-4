"use client";

import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: string[];
}

export default function ProtectedRoute({
  children,
  allowedRoles,
}: ProtectedRouteProps) {
  const { data: session, status } = useSession();
  const router = useRouter();

  useEffect(() => {
    // We are waiting for the session to load.
    if (status === "loading") return;

    // If you are not logged in, redirect to login
    if (status === "unauthenticated") {
      router.push("/auth/login");
      return;
    }

    // Role control — Check if allowedRoles has been granted
    if (allowedRoles && session?.user) {
      const userRole = (session.user as { role?: string }).role;
      if (!userRole || !allowedRoles.includes(userRole)) {
        router.push("/unauthorized");
      }
    }
  }, [status, session, router, allowedRoles]);

  // Show spinner while loading
  if (status === "loading") {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center">
            <svg
              className="w-5 h-5 text-white animate-spin"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
              />
            </svg>
          </div>
          <p className="text-sm text-gray-500">Loading session...</p>
        </div>
      </div>
    );
  }

  // Show nothing if logged in or unauthorized
  if (status === "unauthenticated") return null;

  return <>{children}</>;
}