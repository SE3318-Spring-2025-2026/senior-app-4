"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getUser, getToken } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";

interface AdvisorRequest {
  notificationId: number;
  groupId: number;
  groupName: string;
  requestedAt: string;
}

export default function AdvisorRequestsPage() {
  const router = useRouter();
  const [user, setUser] = useState<ReturnType<typeof getUser>>(null);
  const [requests, setRequests] = useState<AdvisorRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = getToken();
    const u = getUser();

    if (!token || !u || u.role !== "professor") {
      router.replace("/auth/login");
      return;
    }

    setUser(u);
    fetchRequests(token);
  }, [router]);

  const fetchRequests = async (token: string) => {
    setLoading(true);
    try {
      const res = await fetch("http://localhost:8080/api/v1/professors/advisor-requests", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        throw new Error("Failed to fetch requests");
      }
      
      const data = await res.json();
      setRequests(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDecision = async (groupId: number, status: "approved" | "rejected") => {
    const token = getToken();
    try {
      const res = await fetch("http://localhost:8080/api/v1/professors/advisor-requests", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ groupId, status }),
      });

      if (!res.ok) {
        throw new Error("Failed to process decision");
      }

      // Automatically remove from the local list
      setRequests((prev) => prev.filter((r) => r.groupId !== groupId));
      alert(`Request has been ${status}.`);
    } catch (err: any) {
      alert("Error: " + err.message);
    }
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
      <Sidebar activePage="advisor-requests" />

      <main className="flex-1 flex flex-col min-w-0">
        <div className="border-b border-white/5 px-8 py-4">
          <h1 className="text-base font-semibold text-white">Advisor Requests</h1>
          <p className="text-xs text-gray-500 mt-0.5">Manage group requests for advisorship</p>
        </div>

        <div className="flex-1 p-8 overflow-auto">
          {loading ? (
            <div className="flex justify-center mt-10">
              <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
            </div>
          ) : error ? (
            <div className="text-red-500 text-sm">{error}</div>
          ) : requests.length === 0 ? (
            <div className="text-center space-y-3 mt-10">
              <p className="text-sm text-gray-500">No pending advisor requests.</p>
            </div>
          ) : (
            <div className="grid gap-4 max-w-3xl">
              {requests.map((req) => (
                <div key={req.notificationId} className="bg-white/5 border border-white/10 rounded-xl p-5 flex items-center justify-between">
                  <div>
                    <h3 className="text-sm font-medium text-white">{req.groupName}</h3>
                    <p className="text-xs text-gray-400 mt-1">Requested on: {new Date(req.requestedAt).toLocaleDateString()}</p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleDecision(req.groupId, "approved")}
                      className="px-3 py-1.5 text-xs font-medium bg-blue-600 hover:bg-blue-500 text-white rounded-lg transition-colors"
                    >
                      Approve
                    </button>
                    <button
                      onClick={() => handleDecision(req.groupId, "rejected")}
                      className="px-3 py-1.5 text-xs font-medium bg-red-600/20 hover:bg-red-600/30 text-red-400 rounded-lg transition-colors border border-red-500/20"
                    >
                      Reject
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
