"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { getUser, getToken, clearAuth } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import LeaderboardTable from "@/components/LeaderboardTable";
import { fetchGroupDetail, type ApiGroupDetail, type ApiGroupMember } from "@/lib/groups-api";
import { fetchFinalGradingSprints, type SprintConfig } from "@/lib/final-grading-api";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

async function fetchMe(token: string): Promise<{ groupId: number | null; userId: number }> {
  const res = await fetch(`${API_BASE}/users/me`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  if (!res.ok) return { groupId: null, userId: 0 };
  const data = await res.json();
  return { groupId: data?.data?.groupId ?? null, userId: data?.data?.userId ?? 0 };
}

function daysRemaining(endDate: string): number {
  const end = new Date(endDate);
  end.setHours(23, 59, 59, 999);
  const now = new Date();
  return Math.max(0, Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24)));
}

function sprintProgress(startDate: string, endDate: string): number {
  const start = new Date(startDate).getTime();
  const end = new Date(endDate).getTime();
  const now = Date.now();
  if (now <= start) return 0;
  if (now >= end) return 100;
  return Math.round(((now - start) / (end - start)) * 100);
}

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<ReturnType<typeof getUser>>(null);

  useEffect(() => {
    const token = getToken();
    const u = getUser();
    if (!token || !u) { router.replace("/auth/login"); return; }
    if (u.requiresPasswordChange) { router.replace("/auth/change-password"); return; }
    setUser(u);
  }, [router]);

  const handleLogout = () => { clearAuth(); router.replace("/auth/login"); };

  if (!user) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-blue-500" />
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-950 flex">
      <Sidebar activePage="dashboard" />
      <main className="flex-1 flex flex-col min-w-0">
        <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-base font-semibold text-white">Dashboard</h1>
            <p className="text-xs text-gray-500 mt-0.5 capitalize">{user.role} · Senior Project Management System</p>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
            <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
            <span className="text-xs text-gray-400">System Online</span>
          </div>
        </div>

        {user.role === "student" ? (
          <StudentDashboard userId={user.userId ?? 0} />
        ) : (
          <div className="flex-1 p-8 space-y-8 overflow-y-auto">
            <LeaderboardTable />
          </div>
        )}
      </main>
    </div>
  );
}

function StudentDashboard({ userId: _userId }: { userId: number }) {
  const [group, setGroup] = useState<ApiGroupDetail | null>(null);
  const [activeSprint, setActiveSprint] = useState<SprintConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [noGroup, setNoGroup] = useState(false);

  const load = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    setLoading(true);
    try {
      // Round-trip 1: get groupId
      const { groupId } = await fetchMe(token);
      if (!groupId) { setNoGroup(true); setLoading(false); return; }

      // Round-trip 2: parallel — group detail + sprints
      const [groupDetail, sprints] = await Promise.all([
        fetchGroupDetail(groupId),
        fetchFinalGradingSprints(),
      ]);

      setGroup(groupDetail);

      const active = sprints.find(s => s.status?.toLowerCase() === "active") ?? null;
      setActiveSprint(active);
    } catch {
      setNoGroup(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-blue-500" />
      </div>
    );
  }

  if (noGroup || !group) {
    return (
      <div className="flex-1 p-8 flex items-center justify-center">
        <div className="text-center space-y-3">
          <div className="w-16 h-16 rounded-2xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center mx-auto">
            <svg className="w-7 h-7 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197" />
            </svg>
          </div>
          <h2 className="text-lg font-semibold text-white">No Group Yet</h2>
          <p className="text-sm text-gray-500">Join or create a group to see your dashboard.</p>
        </div>
      </div>
    );
  }

  const days = activeSprint ? daysRemaining(activeSprint.endDate) : 0;
  const progress = activeSprint ? sprintProgress(activeSprint.startDate, activeSprint.endDate) : 0;
  const allMembers: ApiGroupMember[] = group.members ?? [];

  return (
    <div className="flex-1 overflow-y-auto p-8">
      <div className="max-w-4xl space-y-5">

        {/* Group card */}
        <div className="rounded-2xl border border-white/8 bg-gray-900 p-6">
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="h-12 w-12 rounded-xl bg-blue-600/20 border border-blue-500/20 flex items-center justify-center shrink-0">
                <span className="text-lg font-bold text-blue-300">
                  {group.groupName.charAt(0).toUpperCase()}
                </span>
              </div>
              <div>
                <h2 className="text-base font-semibold text-white">{group.groupName}</h2>
                <div className="flex items-center gap-2 mt-0.5">
                  <span className={`inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full font-medium ${
                    group.status === "ADVISED"
                      ? "bg-green-500/15 text-green-400"
                      : group.status === "FORMING"
                      ? "bg-amber-500/15 text-amber-400"
                      : "bg-blue-500/15 text-blue-400"
                  }`}>
                    {group.status}
                  </span>
                  {group.advisorName && (
                    <span className="text-xs text-gray-500">· Advisor: <span className="text-gray-300">{group.advisorName}</span></span>
                  )}
                  {!group.advisorName && (
                    <span className="text-xs text-gray-600">· No advisor assigned</span>
                  )}
                </div>
              </div>
            </div>
            <span className="text-xs text-gray-600 shrink-0">ID: {group.id}</span>
          </div>

          {/* Members */}
          {allMembers.length > 0 && (
            <div className="mt-5 pt-5 border-t border-white/5">
              <p className="text-xs font-medium uppercase tracking-widest text-gray-500 mb-3">
                Members ({allMembers.length})
              </p>
              <div className="flex flex-wrap gap-2">
                {allMembers.map((m) => {
                  const isLeader = m.userId === group.leaderId;
                  return (
                    <div
                      key={m.userId}
                      className={`flex items-center gap-2 rounded-lg px-3 py-2 border text-sm ${
                        isLeader
                          ? "border-blue-500/20 bg-blue-500/10"
                          : "border-white/8 bg-white/[0.03]"
                      }`}
                    >
                      <div className={`h-6 w-6 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${
                        isLeader ? "bg-blue-500/30 text-blue-300" : "bg-white/10 text-gray-400"
                      }`}>
                        {m.fullName?.charAt(0).toUpperCase() ?? "?"}
                      </div>
                      <span className={isLeader ? "text-blue-200" : "text-gray-300"}>
                        {m.fullName}
                      </span>
                      {isLeader && (
                        <span className="text-xs text-blue-400 font-medium">Leader</span>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* Active sprint card */}
        {activeSprint ? (
          <div className="rounded-2xl border border-green-500/20 bg-green-500/5 p-6">
            <div className="flex items-start justify-between gap-4 mb-4">
              <div className="flex items-center gap-3">
                <div className="h-9 w-9 rounded-xl bg-green-500/15 flex items-center justify-center shrink-0">
                  <svg className="w-5 h-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z" />
                  </svg>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-widest text-green-500">Active Sprint</p>
                  <h3 className="text-base font-semibold text-white mt-0.5">{activeSprint.sprintName}</h3>
                </div>
              </div>
              <div className="text-right shrink-0">
                <p className="text-2xl font-bold text-white">{days}</p>
                <p className="text-xs text-gray-500">{days === 1 ? "day left" : "days left"}</p>
              </div>
            </div>

            {/* Progress bar */}
            <div className="mb-3">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-xs text-gray-500">{activeSprint.startDate}</span>
                <span className="text-xs text-gray-400 font-medium">{progress}%</span>
                <span className="text-xs text-gray-500">{activeSprint.endDate}</span>
              </div>
              <div className="h-1.5 rounded-full bg-white/8 overflow-hidden">
                <div
                  className="h-full rounded-full bg-green-500 transition-all"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>

            <div className="flex items-center gap-4 mt-3">
              {activeSprint.requiredStoryPoints != null && (
                <div className="flex items-center gap-1.5 text-xs text-gray-400">
                  <svg className="w-3.5 h-3.5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 12h16.5m-16.5 3.75h16.5M3.75 19.5h16.5M5.625 4.5h12.75a1.875 1.875 0 010 3.75H5.625a1.875 1.875 0 010-3.75z" />
                  </svg>
                  Target: <span className="font-medium text-white">{activeSprint.requiredStoryPoints} SP</span>
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="rounded-2xl border border-dashed border-white/10 bg-gray-900 p-6 flex items-center gap-4">
            <div className="h-9 w-9 rounded-xl bg-white/5 flex items-center justify-center shrink-0">
              <svg className="w-5 h-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z" />
              </svg>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-400">No Active Sprint</p>
              <p className="text-xs text-gray-600 mt-0.5">Your coordinator hasn't started a sprint yet.</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
