"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { getToken, getUser } from "@/lib/auth";
import Sidebar from "@/components/Sidebar";
import apiClient from "@/lib/client";

// Types based on likely API responses
type Student = {
    id: number;
    fullName: string;
    email: string;
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

    useEffect(() => {
        // Fetch all groups for the assignment dropdown
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
            // Using /users endpoint since it's commonly available, filtering for students
            const res = await apiClient.get(`/users?role=STUDENT&search=${encodeURIComponent(search)}`);
            // Adapt to the actual response structure. Assume it's an array or has a content array.
            const data = Array.isArray(res.data) ? res.data : res.data.content || [];
            
            // For robust UI, let's also fetch students directly if /users fails or returns empty
            if (data.length === 0) {
                const altRes = await apiClient.get(`/students?search=${encodeURIComponent(search)}`).catch(() => null);
                if (altRes && Array.isArray(altRes.data)) {
                    setStudents(altRes.data);
                    return;
                }
            }
            setStudents(data);
        } catch (error) {
            console.error(error);
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
            handleSearch(); // Refresh list
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
            handleSearch(); // Refresh list
        } catch (error) {
            console.error(error);
        } finally {
            setActionLoading(null);
        }
    };

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="student-ids" />

            <main className="flex-1 flex flex-col min-w-0">
                <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-base font-semibold text-white">Member Management</h1>
                        <p className="text-xs text-gray-500 mt-0.5">Manually adjust student group memberships</p>
                    </div>
                </div>

                <div className="flex-1 p-8">
                    <div className="max-w-4xl mx-auto space-y-6">
                        
                        <div className="bg-gray-900 border border-white/10 rounded-2xl p-6 shadow-xl shadow-black/20">
                            <h2 className="text-lg font-semibold text-white mb-4">Search Students</h2>
                            <div className="flex gap-3">
                                <input
                                    type="text"
                                    placeholder="Search by name, email, or ID..."
                                    value={search}
                                    onChange={(e) => setSearch(e.target.value)}
                                    onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                                    className="flex-1 bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none"
                                />
                                <button
                                    onClick={handleSearch}
                                    className="bg-blue-600 text-white px-6 py-3 rounded-xl font-medium hover:bg-blue-500 transition-colors"
                                >
                                    Search
                                </button>
                            </div>
                        </div>

                        {loading && (
                            <div className="text-center py-10">
                                <div className="inline-block w-8 h-8 border-4 border-white/20 border-t-blue-500 rounded-full animate-spin"></div>
                                <p className="text-gray-400 mt-3">Searching students...</p>
                            </div>
                        )}

                        {!loading && students.length > 0 && (
                            <div className="bg-gray-900 border border-white/10 rounded-2xl overflow-hidden shadow-xl shadow-black/20">
                                <table className="w-full text-left border-collapse">
                                    <thead>
                                        <tr className="bg-white/5 border-b border-white/10">
                                            <th className="px-6 py-4 text-xs font-semibold text-gray-400 uppercase tracking-wider">Student Name</th>
                                            <th className="px-6 py-4 text-xs font-semibold text-gray-400 uppercase tracking-wider">Email / ID</th>
                                            <th className="px-6 py-4 text-xs font-semibold text-gray-400 uppercase tracking-wider">Current Group</th>
                                            <th className="px-6 py-4 text-xs font-semibold text-gray-400 uppercase tracking-wider text-right">Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-white/5">
                                        {students.map((student) => (
                                            <tr key={student.id} className="hover:bg-white/5 transition-colors">
                                                <td className="px-6 py-4">
                                                    <div className="text-sm font-medium text-white">{student.fullName || `Student #${student.id}`}</div>
                                                </td>
                                                <td className="px-6 py-4">
                                                    <div className="text-sm text-gray-400">{student.email || 'N/A'}</div>
                                                </td>
                                                <td className="px-6 py-4">
                                                    {student.groupId ? (
                                                        <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-blue-500/10 text-blue-400 border border-blue-500/20 text-xs font-medium">
                                                            {student.groupName || `Group #${student.groupId}`}
                                                        </span>
                                                    ) : (
                                                        <span className="text-sm text-gray-500 italic">No group</span>
                                                    )}
                                                </td>
                                                <td className="px-6 py-4 text-right">
                                                    {student.groupId ? (
                                                        <button
                                                            onClick={() => handleRemove(student.id, student.groupId!)}
                                                            disabled={actionLoading === student.id}
                                                            className="text-sm bg-red-500/10 text-red-400 border border-red-500/20 px-3 py-1.5 rounded-lg hover:bg-red-500/20 transition-colors disabled:opacity-50"
                                                        >
                                                            {actionLoading === student.id ? 'Removing...' : 'Remove'}
                                                        </button>
                                                    ) : (
                                                        <div className="flex items-center justify-end gap-2">
                                                            <select
                                                                value={selectedGroups[student.id] || ""}
                                                                onChange={(e) => setSelectedGroups(prev => ({ ...prev, [student.id]: e.target.value }))}
                                                                className="bg-black/40 border border-white/10 text-sm text-white rounded-lg px-2 py-1.5 focus:ring-1 focus:ring-blue-500 outline-none max-w-[150px]"
                                                            >
                                                                <option value="">Select Group...</option>
                                                                {groups.map(g => (
                                                                    <option key={g.id} value={g.id}>{g.groupName}</option>
                                                                ))}
                                                            </select>
                                                            <button
                                                                onClick={() => handleAssign(student.id)}
                                                                disabled={actionLoading === student.id || !selectedGroups[student.id]}
                                                                className="text-sm bg-blue-600 text-white px-3 py-1.5 rounded-lg hover:bg-blue-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                                            >
                                                                {actionLoading === student.id ? 'Assigning...' : 'Assign'}
                                                            </button>
                                                        </div>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        {!loading && search && students.length === 0 && (
                            <div className="text-center py-10 bg-gray-900 border border-dashed border-white/10 rounded-2xl">
                                <p className="text-gray-400">No students found matching your search.</p>
                            </div>
                        )}

                    </div>
                </div>
            </main>
        </div>
    );
}
