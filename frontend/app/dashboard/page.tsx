"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { getUser, getToken, clearAuth } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import SystemLogsTable from "@/components/audit-logs/SystemLogsTable";

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<ReturnType<typeof getUser>>(null);

  useEffect(() => {
    const token = getToken();
    const u = getUser();

    if (!token || !u) {
      router.replace("/auth/login");
      return;
    }

    if (u.requiresPasswordChange) {
      router.replace("/auth/change-password");
      return;
    }

    setUser(u);
  }, [router]);

  const handleLogout = () => {
    clearAuth();
    router.replace("/auth/login");
  };

  if (!user) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-950 flex">
      <Sidebar activePage="dashboard" />

      {/* Main */}
      <main className="flex-1 flex flex-col min-w-0">
        <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-base font-semibold text-white">Dashboard</h1>
            <p className="text-xs text-gray-500 mt-0.5 capitalize">{user.role} · Senior Project Management System</p>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
            <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
            <span className="text-xs text-gray-400">Online</span>
          </div>
        </div>

        {user.role === "professor" ? (
          <div className="flex-1 p-8 space-y-6 overflow-y-auto">
            <SystemLogsTable />
          </div>
        ) : (
          <div className="flex-1 p-8 flex items-center justify-center">
            <div className="text-center space-y-3">
              <div className="w-16 h-16 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center mx-auto">
                <svg className="w-7 h-7 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                </svg>
              </div>
              <h2 className="text-lg font-semibold text-white">Welcome to SPMS</h2>
              <p className="text-sm text-gray-500">Use the sidebar to navigate.</p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
