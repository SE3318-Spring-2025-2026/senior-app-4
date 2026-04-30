"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { setAuth, decodeToken } from "@/lib/auth";

export default function ProfessorLoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.SyntheticEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const res = await fetch(`/api/v1/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim(), password }),
      });

      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        setError(data.message || "Invalid email or password.");
        return;
      }


      const claims = decodeToken(data.token) ?? {};

      setAuth(data.token, {
        userId: Number(claims.userId),
        studentId: claims.studentId as string | undefined,
        githubUsername: claims.githubUsername as string | undefined,
        role: data.role ?? (claims.role as string),
        requiresPasswordChange: data.requiresPasswordChange ?? (claims.requiresPasswordChange as boolean),
      });

      if (data.requiresPasswordChange) {
        router.replace("/auth/change-password");
      } else {
        router.replace("/dashboard");
      }
    } catch {
      setError("Unable to connect to the server. Please try again later.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4">
      <div className="w-full max-w-sm space-y-8">

        {/* Logo */}
        <div className="text-center space-y-3">
          <div className="w-14 h-14 rounded-2xl bg-blue-600 flex items-center justify-center mx-auto shadow-lg shadow-blue-600/30">
            <svg className="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <div>
            <h1 className="text-xl font-semibold text-white">SPMS</h1>
            <p className="text-sm text-gray-500">Senior Project Management System</p>
          </div>
        </div>

        {/* Card */}
        <div className="bg-gray-900 border border-white/8 rounded-2xl p-8 space-y-6">
          <div className="space-y-1 text-center">
            <h2 className="text-base font-semibold text-white">Professor / Coordinator Login</h2>
            <p className="text-xs text-gray-500">Sign in with your institutional credentials.</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <label htmlFor="email" className="text-xs font-medium text-gray-400">Email Address</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => { setEmail(e.target.value); setError(null); }}
                placeholder="professor@yasar.edu.tr"
                required
                disabled={loading}
                className={[
                  "w-full px-4 py-3 rounded-xl text-sm bg-white/5 border text-white placeholder-gray-600",
                  "focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50",
                  "transition-colors disabled:opacity-50",
                  error ? "border-red-500/50 bg-red-500/5" : "border-white/10",
                ].join(" ")}
              />
            </div>

            <div className="space-y-1.5">
              <label htmlFor="password" className="text-xs font-medium text-gray-400">Password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => { setPassword(e.target.value); setError(null); }}
                placeholder="••••••••"
                required
                disabled={loading}
                className={[
                  "w-full px-4 py-3 rounded-xl text-sm bg-white/5 border text-white placeholder-gray-600",
                  "focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50",
                  "transition-colors disabled:opacity-50",
                  error ? "border-red-500/50 bg-red-500/5" : "border-white/10",
                ].join(" ")}
              />
            </div>

            {error && (
              <p className="text-xs text-red-400 flex items-center gap-1.5">
                <svg className="w-3.5 h-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                </svg>
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={loading || !email.trim() || !password}
              className={[
                "w-full flex items-center justify-center gap-2 px-4 py-3 rounded-xl",
                "text-sm font-medium transition-all duration-150",
                loading || !email.trim() || !password
                  ? "bg-white/5 text-gray-600 cursor-not-allowed border border-white/5"
                  : "bg-white text-gray-900 hover:bg-gray-100 active:scale-95 shadow-lg shadow-black/20",
              ].join(" ")}
            >
              {loading ? (
                <>
                  <svg className="w-4 h-4 animate-spin text-gray-500" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  <span className="text-gray-500">Signing in...</span>
                </>
              ) : (
                "Sign In"
              )}
            </button>
          </form>

          {/* Divider */}
          <div className="flex items-center gap-3">
            <div className="flex-1 h-px bg-white/5" />
            <span className="text-xs text-gray-600">or</span>
            <div className="flex-1 h-px bg-white/5" />
          </div>

          <button
            onClick={() => router.push("/auth/login")}
            className="w-full px-4 py-2.5 rounded-xl text-xs font-medium text-gray-500 hover:text-gray-300 hover:bg-white/5 transition-colors border border-white/5"
          >
            Sign in as Student instead
          </button>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-gray-700">
          Yaşar University · Senior Project Management System
        </p>

      </div>
    </div>
  );
}
