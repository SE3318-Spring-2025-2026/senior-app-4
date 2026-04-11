"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { getToken, getUser, clearAuth } from "@/lib/auth";

export default function AdminProfessorRegisterPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    fullName: "",
    email: "",
    password: "",
    role: "PROFESSOR",
  });
  const [message, setMessage] = useState({ type: "", text: "" });
  const [isLoading, setIsLoading] = useState(false);
  const [userRole, setUserRole] = useState<string | null>(null);

  useEffect(() => {
    const token = getToken();
    const user = getUser();
    if (!token || !user) { router.replace("/auth/login"); return; }
    if (user.requiresPasswordChange) { router.replace("/auth/change-password"); return; }
    if (user.role !== "coordinator" && user.role !== "professor") {
      router.replace("/dashboard");
      return;
    }
    setUserRole(user.role);
  }, [router]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.SyntheticEvent) => {
    e.preventDefault();
    setMessage({ type: "", text: "" });

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      setMessage({ type: "error", text: "Please enter a valid email address." });
      return;
    }

    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[^\n]{8,}$/;
    if (!passwordRegex.test(formData.password)) {
      setMessage({ type: "error", text: "Password must be at least 8 characters, including an uppercase letter, a lowercase letter, and a number." });
      return;
    }

    setIsLoading(true);
    try {
      const token = getToken();
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/professors/register`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { "Authorization": `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(formData),
      });

      const data = await response.json().catch(() => ({}));

      if (response.ok) {
        setMessage({ type: "success", text: "Personnel successfully registered!" });
        setFormData({ fullName: "", email: "", password: "", role: "PROFESSOR" });
      } else {
        setMessage({ type: "error", text: data.message || "Registration failed. The email might already be registered." });
      }
    } catch {
      setMessage({ type: "error", text: "Could not connect to the server." });
    } finally {
      setIsLoading(false);
    }
  };

  if (!userRole) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-950 flex">
      {/* Sidebar */}
      <aside className="w-64 border-r border-white/5 flex flex-col shrink-0">
        <div className="px-5 py-5 border-b border-white/5">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center shrink-0">
              <svg className="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <div>
              <p className="text-sm font-semibold text-white">SPMS</p>
              <p className="text-xs text-gray-500 capitalize">{userRole} Panel</p>
            </div>
          </div>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1">
          <p className="text-xs font-medium text-gray-600 px-3 mb-2 uppercase tracking-widest">Navigation</p>

          {userRole === "coordinator" && (
            <Link href="/coordinator/student-ids" className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-gray-400 hover:text-white hover:bg-white/5 transition-all text-sm font-medium">
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 9h3.75M15 12h3.75M15 15h3.75M4.5 19.5h15a2.25 2.25 0 002.25-2.25V6.75A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25v10.5A2.25 2.25 0 004.5 19.5zm6-10.125a1.875 1.875 0 11-3.75 0 1.875 1.875 0 013.75 0zm1.294 6.336a6.721 6.721 0 01-3.17.789 6.721 6.721 0 01-3.168-.789 3.376 3.376 0 016.338 0z" />
              </svg>
              Student IDs
            </Link>
          )}

          <Link href="/admin/register" className="flex items-center gap-3 px-3 py-2.5 rounded-lg bg-white/8 text-white transition-all text-sm font-medium">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M19 7.5v3m0 0v3m0-3h3m-3 0h-3m-2.25-4.125a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zM4 19.235v-.11a6.375 6.375 0 0112.75 0v.109A12.318 12.318 0 0110.374 21c-2.331 0-4.512-.645-6.374-1.766z" />
            </svg>
            Register Personnel
          </Link>
        </nav>

        <div className="px-3 py-4 border-t border-white/5">
          <div className="flex items-center gap-3 px-3 py-2 mb-2">
            <div className="w-8 h-8 rounded-full bg-blue-600/20 border border-blue-500/30 flex items-center justify-center shrink-0">
              <span className="text-xs font-semibold text-blue-400 uppercase">{userRole?.slice(0, 2)}</span>
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-white truncate capitalize">{userRole}</p>
            </div>
          </div>
          <button
            onClick={() => { clearAuth(); router.replace("/auth/login"); }}
            className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-xs text-gray-500 hover:text-red-400 hover:bg-red-500/5 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
            </svg>
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 flex flex-col min-w-0">
        <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-base font-semibold text-white">Register Personnel</h1>
            <p className="text-xs text-gray-500 mt-0.5">Add a new professor or coordinator to the system</p>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
            <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
            <span className="text-xs text-gray-400">Online</span>
          </div>
        </div>

        <div className="flex-1 p-8 flex items-start justify-center">
          <div className="w-full max-w-md">
            <div className="bg-gray-900 border border-white/8 rounded-2xl p-8 space-y-6">
              <div className="space-y-1 text-center">
                <h2 className="text-base font-semibold text-white">New Personnel</h2>
                <span className="inline-block px-3 py-1 bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs font-medium rounded-full">
                  Coordinator Access Only
                </span>
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
                <div className="space-y-1.5">
                  <label htmlFor="fullName" className="text-xs font-medium text-gray-400">Full Name</label>
                  <input
                    id="fullName"
                    type="text"
                    name="fullName"
                    required
                    placeholder="Prof. Dr. John Doe"
                    value={formData.fullName}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-xl text-sm bg-white/5 border border-white/10 text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-colors"
                  />
                </div>

                <div className="space-y-1.5">
                  <label htmlFor="email" className="text-xs font-medium text-gray-400">Email Address</label>
                  <input
                    id="email"
                    type="email"
                    name="email"
                    required
                    placeholder="example@yasar.edu.tr"
                    value={formData.email}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-xl text-sm bg-white/5 border border-white/10 text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-colors"
                  />
                </div>

                <div className="space-y-1.5">
                  <label htmlFor="password" className="text-xs font-medium text-gray-400">Temporary Password</label>
                  <input
                    id="password"
                    type="password"
                    name="password"
                    required
                    placeholder="••••••••"
                    value={formData.password}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-xl text-sm bg-white/5 border border-white/10 text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-colors"
                  />
                  <p className="text-xs text-gray-600">Min 8 chars, 1 uppercase, 1 lowercase, 1 number.</p>
                </div>

                <div className="space-y-1.5">
                  <label htmlFor="role" className="text-xs font-medium text-gray-400">Role</label>
                  <select
                    id="role"
                    name="role"
                    value={formData.role}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-xl text-sm bg-gray-800 border border-white/10 text-white focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-colors"
                  >
                    <option value="PROFESSOR">Professor</option>
                    <option value="COORDINATOR">Coordinator</option>
                  </select>
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
                      <span className="text-gray-500">Registering...</span>
                    </>
                  ) : (
                    "Register"
                  )}
                </button>
              </form>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}