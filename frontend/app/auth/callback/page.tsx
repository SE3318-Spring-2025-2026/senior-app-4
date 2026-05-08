"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { setAuth, decodeToken } from "@/lib/auth";

import { Suspense } from "react";

type Status = "loading" | "error";

function CallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<Status>("loading");
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    const code = searchParams.get("code");
    const state = searchParams.get("state");

    if (!code || !state) {
      setErrorMsg("Missing authorization code or state. Please try again.");
      setStatus("error");
      return;
    }

    const exchangeCode = async () => {
      try {
        const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";
        const res = await fetch(
          `${apiUrl}/auth/github/callback?code=${encodeURIComponent(code)}&state=${encodeURIComponent(state)}`
        );

        if (!res.ok) {
          const data = await res.json().catch(() => ({}));
          setErrorMsg(data.message || "Authentication failed. Please try again.");
          setStatus("error");
          return;
        }

        const data = await res.json();
        const claims = decodeToken(data.token) ?? {};

        setAuth(data.token, {
          userId: Number(claims.userId),
          name: (claims.fullName as string) || undefined,
          studentId: claims.studentId as string | undefined,
          githubUsername: claims.githubUsername as string | undefined,
          role: (claims.role as string) ?? "student",
          requiresPasswordChange: (claims.requiresPasswordChange as boolean) ?? false,
        });

        router.replace("/dashboard");
      } catch {
        setErrorMsg("Unable to connect to the server. Please try again later.");
        setStatus("error");
      }
    };

    exchangeCode();
  }, [searchParams, router]);

  if (status === "error") {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4">
        <div className="w-full max-w-sm space-y-6 text-center">
          <div className="w-14 h-14 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto">
            <svg className="w-7 h-7 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
            </svg>
          </div>
          <div className="space-y-1">
            <h2 className="text-base font-semibold text-white">Authentication Failed</h2>
            <p className="text-sm text-gray-500">{errorMsg}</p>
          </div>
          <button
            onClick={() => router.push("/auth/login")}
            className="px-5 py-2.5 rounded-xl text-sm font-medium bg-white text-gray-900 hover:bg-gray-100 active:scale-95 transition-all"
          >
            Back to Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <div className="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center">
          <svg className="w-5 h-5 text-white animate-spin" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
        </div>
        <p className="text-sm text-gray-500">Completing sign in...</p>
      </div>
    </div>
  );
}

export default function CallbackPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-gray-950 flex items-center justify-center text-white">
        <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      </div>
    }>
      <CallbackContent />
    </Suspense>
  );
}
