"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { getToken, getUser, setAuth } from "@/lib/auth";

export default function ChangePasswordPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    email: "",
    tempPassword: "",
    newPassword: "",
    confirmNewPassword: "",
  });

  const [message, setMessage] = useState({ type: "", text: "" });
  const [isLoading, setIsLoading] = useState(false);
  const [tokenMode, setTokenMode] = useState(false); // JWT ile giriş yapıldıysa email/tempPw gizlenir

  useEffect(() => {
    const user = getUser();
    const token = getToken();
    if (token && user?.requiresPasswordChange) {
      setTokenMode(true);
    }
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.SyntheticEvent) => {
    e.preventDefault();
    setMessage({ type: "", text: "" });

    if (formData.newPassword !== formData.confirmNewPassword) {
      setMessage({ type: "error", text: "New passwords do not match!" });
      return;
    }

    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[^\n]{8,}$/;
    if (!passwordRegex.test(formData.newPassword)) {
      setMessage({
        type: "error",
        text: "Password must be at least 8 characters long, including an uppercase letter, a lowercase letter, and a number.",
      });
      return;
    }

    setIsLoading(true);

    try {
      const token = getToken();
      const headers: Record<string, string> = { "Content-Type": "application/json" };
      if (token) headers["Authorization"] = `Bearer ${token}`;

      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/auth/change-password`, {
        method: "POST",
        headers,
        body: JSON.stringify({
          email: formData.email,
          tempPassword: formData.tempPassword,
          newPassword: formData.newPassword,
        }),
      });

      const data = await res.json().catch(() => ({}));

      if (res.ok) {
        setMessage({ type: "success", text: "Password updated successfully! Redirecting..." });

        // requiresPasswordChange flag'ini temizle
        const user = getUser();
        const currentToken = getToken();
        if (user && currentToken) {
          setAuth(currentToken, { ...user, requiresPasswordChange: false });
        }

        setTimeout(() => { router.push("/dashboard"); }, 1500);
      } else {
        setMessage({ type: "error", text: data.message || "Something went wrong. Please check your temporary password." });
      }
    } catch {
      setMessage({ type: "error", text: "Server connection failed. Please try again later." });
    } finally {
      setIsLoading(false);
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
            <h2 className="text-base font-semibold text-white">Change Password</h2>
            <p className="text-xs text-gray-500">Please update your temporary password to continue.</p>
          </div>

          {message.text && (
            <div className={[
              "px-4 py-3 rounded-xl text-xs font-medium flex items-center gap-2",
              message.type === "error"
                ? "bg-red-500/10 border border-red-500/20 text-red-400"
                : "bg-green-500/10 border border-green-500/20 text-green-400",
            ].join(" ")}>
              <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                {message.type === "error" ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                )}
              </svg>
              {message.text}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {!tokenMode && (
              <>
                <div className="space-y-1.5">
                  <label htmlFor="email" className="text-xs font-medium text-gray-400">Email Address</label>
                  <input
                    id="email"
                    type="email"
                    name="email"
                    required
                    placeholder="professor@yasar.edu.tr"
                    value={formData.email}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-xl text-sm bg-white/5 border border-white/10 text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-colors"
                  />
                </div>

                <div className="space-y-1.5">
                  <label htmlFor="tempPassword" className="text-xs font-medium text-gray-400">Temporary Password</label>
                  <input
                    id="tempPassword"
                    type="password"
                    name="tempPassword"
                    required
                    placeholder="Current temporary password"
                    value={formData.tempPassword}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-xl text-sm bg-white/5 border border-white/10 text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-colors"
                  />
                </div>
              </>
            )}

            <div className="space-y-1.5">
              <label htmlFor="newPassword" className="text-xs font-medium text-gray-400">New Password</label>
              <input
                id="newPassword"
                type="password"
                name="newPassword"
                required
                placeholder="••••••••"
                value={formData.newPassword}
                onChange={handleChange}
                className="w-full px-4 py-3 rounded-xl text-sm bg-white/5 border border-white/10 text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-colors"
              />
              <p className="text-xs text-gray-600">At least 8 characters, 1 upper, 1 lower, 1 number.</p>
            </div>

            <div className="space-y-1.5">
              <label htmlFor="confirmNewPassword" className="text-xs font-medium text-gray-400">Confirm New Password</label>
              <input
                id="confirmNewPassword"
                type="password"
                name="confirmNewPassword"
                required
                placeholder="••••••••"
                value={formData.confirmNewPassword}
                onChange={handleChange}
                className="w-full px-4 py-3 rounded-xl text-sm bg-white/5 border border-white/10 text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-colors"
              />
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className={[
                "w-full flex items-center justify-center gap-2 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-150 mt-2",
                isLoading
                  ? "bg-white/5 text-gray-600 cursor-not-allowed border border-white/5"
                  : "bg-white text-gray-900 hover:bg-gray-100 active:scale-95 shadow-lg shadow-black/20",
              ].join(" ")}
            >
              {isLoading ? (
                <>
                  <svg className="w-4 h-4 animate-spin text-gray-500" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  <span className="text-gray-500">Updating...</span>
                </>
              ) : (
                "Update Password"
              )}
            </button>
          </form>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-gray-700">
          Yaşar University · Senior Project Management System
        </p>

      </div>
    </div>
  );
}
