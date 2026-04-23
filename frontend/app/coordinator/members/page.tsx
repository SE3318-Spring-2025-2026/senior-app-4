"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { getToken, getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import AppTopbar from "@/components/AppTopbar";
import { useNotifications } from "@/components/NotificationProvider";
import apiClient from "@/lib/client";

type Student = {
    id: number;
    fullName: string;
    email: string;
    studentId: string;
    groupId?: number | null;
    groupName?: string | null;
};

type Group = {
    id: number;
    groupName: string;
};

export default function CoordinatorMembersPage() {
    const router = useRouter();
    const [role, setRole] = useState<string | null>(null);

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
        setRole(user.role);
    }, [router]);

    if (role === null) return (
        <div className="min-h-screen bg-gray-950 flex items-center justify-center">
            <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
        </div>
    );

    if (role !== "coordinator") return <AccessDenied />;
    return <DashboardLayout />;
}

function AccessDenied() {
    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-950">
            <div className="text-center space-y-4">
                <div className="w-16 h-16 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto">
                    <svg className="w-7 h-7 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                    </svg>
                </div>
                <h1 className="text-lg font-semibold text-white">Access Restricted</h1>
                <p className="text-sm text-gray-500">Only Coordinators and Admins can access this page.</p>
            </div>
        </div>
    );
}

function DashboardLayout() {
    const [search, setSearch] = useState("");
    const [students, setStudents] = useState<Student[]>([]);
    const [groups, setGroups] = useState<Group[]>([]);
    const [loading, setLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState<number | null>(null);
    const [selectedGroups, setSelectedGroups] = useState<Record<number, string>>({});
    const { unreadOrPendingCount } = useNotifications();

    useEffect(() => {
        apiClient.get('/groups?size=1000')
            .then(res => {
                if (res.data && res.data.content) {
                    setGroups(res.data.content.map((g: any) => ({ id: g.id, groupName: g.groupName })));
                }
            })
            .catch(() => console.error("Failed to load groups"));
    }, []);

    const handleSearch = async () => {
        if (!search.trim()) {
            setStudents([]);
            return;
        }

        setLoading(true);
        try {
            const res = await apiClient.get(`/coordinator/students/search?q=${encodeURIComponent(search)}`);
            setStudents(res.data || []);
        } catch (error) {
            console.error(error);
            toast.error("An error occurred while searching.");
        } finally {
            setLoading(false);
        }
    };

    const handleAssign = async (studentId: number) => {
        const groupId = selectedGroups[studentId];
        if (!groupId) {
            toast.error("Please select a group first.");
            return;
        }

        setActionLoading(studentId);
        try {
            await apiClient.post(`/groups/${groupId}/members`, { studentId });
            toast.success("Student successfully assigned to group.");
            
            // Auto refresh state
            setStudents(prev => prev.map(s => {
                if (s.id === studentId) {
                    const groupName = groups.find(g => g.id === Number(groupId))?.groupName;
                    return { ...s, groupId: Number(groupId), groupName: groupName };
                }
                return s;
            }));
            
        } catch (error) {
            console.error(error);
        } finally {
            setActionLoading(null);
        }
    };

    const handleRemove = async (studentId: number, groupId: number) => {
        if (!confirm("Are you sure you want to remove this student from their group?")) return;

        setActionLoading(studentId);
        try {
            await apiClient.delete(`/groups/${groupId}/members/${studentId}`);
            toast.success("Student successfully removed from group.");
            
            // Auto refresh state
            setStudents(prev => prev.map(s => {
                if (s.id === studentId) {
                    return { ...s, groupId: null, groupName: null };
                }
                return s;
            }));
            
        } catch (error) {
            console.error(error);
        } finally {
            setActionLoading(null);
        }
    };

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="members" />

            <main className="flex-1 flex flex-col min-w-0">
                <AppTopbar title="Member Management" notificationCount={unreadOrPendingCount} />

                <div className="flex-1 p-8">
                    <div className="max-w-5xl mx-auto space-y-6">
                        
                        <div className="flex items-center justify-between mb-8">
                            <div>
                                <h1 className="text-3xl font-bold text-white">Student Placement</h1>
                                <p className="mt-2 text-gray-400">Search students and manually manage their group assignments.</p>
                            </div>
                        </div>

                        {/* Search Card */}
                        <div className="bg-gradient-to-br from-gray-900 to-gray-950 border border-white/10 rounded-3xl p-8 shadow-2xl shadow-black/40">
                            <h2 className="text-xl font-semibold text-white mb-6">Find Student</h2>
                            <div className="flex gap-4">
                                <div className="relative flex-1">
                                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                                        <svg className="w-5 h-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                        </svg>
                                    </div>
                                    <input
                                        type="text"
                                        placeholder="Enter name, email, or student ID..."
                                        value={search}
                                        onChange={(e) => setSearch(e.target.value)}
                                        onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                                        className="w-full bg-black/40 border border-white/10 text-white rounded-2xl pl-12 pr-4 py-4 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all"
                                    />
                                </div>
                                <button
                                    onClick={handleSearch}
                                    className="bg-blue-600 text-white px-8 py-4 rounded-2xl font-semibold hover:bg-blue-500 transition-all shadow-lg shadow-blue-600/20 active:scale-95 flex items-center gap-2"
                                >
                                    Search
                                </button>
                            </div>
                        </div>

                        {/* Loading State */}
                        {loading && (
                            <div className="text-center py-16">
                                <div className="inline-block w-10 h-10 border-4 border-white/10 border-t-blue-500 rounded-full animate-spin"></div>
                                <p className="text-gray-400 mt-4 text-sm font-medium tracking-wide">Searching database...</p>
                            </div>
                        )}

                        {/* Empty State */}
                        {!loading && search && students.length === 0 && (
                            <div className="text-center py-20 bg-gray-900/50 border border-dashed border-white/10 rounded-3xl">
                                <div className="w-16 h-16 bg-gray-800 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <svg className="w-8 h-8 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                    </svg>
                                </div>
                                <h3 className="text-lg font-medium text-white mb-1">No Matches Found</h3>
                                <p className="text-gray-400 text-sm">We couldn't find any students matching "{search}".</p>
                            </div>
                        )}

                        {/* Results Grid */}
                        {!loading && students.length > 0 && (
                            <div className="grid grid-cols-1 gap-4">
                                {students.map((student) => (
                                    <div key={student.id} className="bg-gray-900 border border-white/5 hover:border-white/10 rounded-2xl p-5 flex items-center justify-between transition-all group">
                                        <div className="flex items-center gap-5">
                                            <div className="w-12 h-12 rounded-full bg-blue-500/10 border border-blue-500/20 flex items-center justify-center shrink-0">
                                                <span className="text-blue-400 font-semibold text-lg">
                                                    {(student.fullName || 'S')[0].toUpperCase()}
                                                </span>
                                            </div>
                                            <div>
                                                <h3 className="text-white font-semibold text-lg">{student.fullName || 'Unknown Student'}</h3>
                                                <div className="flex items-center gap-3 mt-1">
                                                    <span className="text-sm text-gray-400">{student.studentId || 'No ID'}</span>
                                                    <span className="w-1 h-1 rounded-full bg-gray-700"></span>
                                                    <span className="text-sm text-gray-500">{student.email || 'No email'}</span>
                                                </div>
                                            </div>
                                        </div>

                                        <div className="flex items-center gap-8">
                                            <div className="text-right">
                                                <p className="text-xs text-gray-500 uppercase tracking-wider mb-1">Current Status</p>
                                                {student.groupId ? (
                                                    <span className="inline-flex items-center px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-sm font-medium">
                                                        {student.groupName || `Group #${student.groupId}`}
                                                    </span>
                                                ) : (
                                                    <span className="inline-flex items-center px-3 py-1 rounded-full bg-yellow-500/10 text-yellow-400 border border-yellow-500/20 text-sm font-medium">
                                                        Unassigned
                                                    </span>
                                                )}
                                            </div>

                                            <div className="w-px h-12 bg-white/5"></div>

                                            <div className="min-w-[200px] flex justify-end">
                                                {student.groupId ? (
                                                    <button
                                                        onClick={() => handleRemove(student.id, student.groupId!)}
                                                        disabled={actionLoading === student.id}
                                                        className="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 hover:text-red-300 transition-colors disabled:opacity-50 font-medium"
                                                    >
                                                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                        </svg>
                                                        {actionLoading === student.id ? 'Removing...' : 'Remove'}
                                                    </button>
                                                ) : (
                                                    <div className="flex items-center gap-3">
                                                        <select
                                                            value={selectedGroups[student.id] || ""}
                                                            onChange={(e) => setSelectedGroups(prev => ({ ...prev, [student.id]: e.target.value }))}
                                                            className="bg-black/50 border border-white/10 text-sm text-white rounded-xl px-3 py-2.5 focus:ring-1 focus:ring-blue-500 outline-none w-40"
                                                        >
                                                            <option value="">Select Group...</option>
                                                            {groups.map(g => (
                                                                <option key={g.id} value={g.id}>{g.groupName}</option>
                                                            ))}
                                                        </select>
                                                        <button
                                                            onClick={() => handleAssign(student.id)}
                                                            disabled={actionLoading === student.id || !selectedGroups[student.id]}
                                                            className="bg-blue-600 text-white px-4 py-2.5 rounded-xl text-sm font-medium hover:bg-blue-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                                        >
                                                            {actionLoading === student.id ? 'Assigning...' : 'Assign'}
                                                        </button>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}

                    </div>
                </div>
            </main>
        </div>
    );
}
