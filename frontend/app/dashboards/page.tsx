"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getUser, getToken, clearAuth } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import LeaderboardTable from "@/components/LeaderboardTable";

export default function DashboardsPage() {
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
      <Sidebar activePage="dashboards" />

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-w-0">
        <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-base font-semibold text-white">Analytics Dashboards</h1>
            <p className="text-xs text-gray-500 mt-0.5 capitalize">{user.role} · Performance metrics</p>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
            <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
            <span className="text-xs text-gray-400">Live</span>
          </div>
        </div>

        <div className="flex-1 p-8 space-y-6 overflow-y-auto">
          {/* Leaderboard Table Grid */}
          <LeaderboardTable />
        </div>
      </main>
    </div>
  );
}
