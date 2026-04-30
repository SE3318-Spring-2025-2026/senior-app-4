"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import Sidebar from "@/components/Sidebar";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import { fetchCommittees, Committee } from "@/lib/committees-api";
import CreateCommitteeForm from "@/components/committees/CreateCommitteeForm";

export default function CoordinatorCommitteesPage() {
    const authStatus = useAuthGuard("coordinator");

    if (authStatus === "loading") return (
        <div className="min-h-screen bg-gray-950 flex items-center justify-center">
            <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
        </div>
    );

    if (authStatus === "denied") return <AccessDenied />;
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
    const [committees, setCommittees] = useState<Committee[]>([]);
    const [loading, setLoading] = useState(true);
    const [showCreateModal, setShowCreateModal] = useState(false);

    const loadCommittees = async () => {
        setLoading(true);
        try {
            const data = await fetchCommittees();
            console.log("Fetched committees data:", data);
            setCommittees(Array.isArray(data) ? data : (data as any)?.data || []);
        } catch (error) {
            console.error("Failed to fetch committees", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCommittees();
    }, []);

    const handleCreateSuccess = () => {
        setShowCreateModal(false);
        loadCommittees();
    };

    return (
        <div className="min-h-screen bg-gray-950 flex">
            {/* Sidebar with activePage="system-alerts" just to keep it selected or we can add a new one, but let's assume we can pass "committees" */}
            <Sidebar activePage="committees" />

            <main className="flex-1 flex flex-col min-w-0 relative">
                <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-base font-semibold text-white">Committees</h1>
                        <p className="text-xs text-gray-500 mt-0.5">Manage evaluation committees and assignments</p>
                    </div>
                    <button
                        onClick={() => setShowCreateModal(true)}
                        className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-500 transition-colors"
                    >
                        Create Committee
                    </button>
                </div>

                <div className="flex-1 p-8">
                    <div className="max-w-5xl mx-auto space-y-6">
                        {loading ? (
                            <div className="text-center py-10">
                                <div className="inline-block w-8 h-8 border-4 border-white/20 border-t-blue-500 rounded-full animate-spin"></div>
                                <p className="text-gray-400 mt-3">Loading committees...</p>
                            </div>
                        ) : committees.length === 0 ? (
                            <div className="text-center py-10 bg-gray-900 border border-dashed border-white/10 rounded-2xl">
                                <p className="text-gray-400 mb-4">No committees have been created yet.</p>
                                <button
                                    onClick={() => setShowCreateModal(true)}
                                    className="bg-blue-600/10 text-blue-400 border border-blue-500/20 px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-500/20 transition-colors"
                                >
                                    Create First Committee
                                </button>
                            </div>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                {Array.isArray(committees) && committees.map((committee) => (
                                    <Link 
                                        href={`/coordinator/committees/${committee.committeeId}`} 
                                        key={committee.committeeId}
                                        className="bg-gray-900 border border-white/10 p-6 rounded-2xl hover:border-blue-500/50 transition-colors group relative overflow-hidden"
                                    >
                                        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-500 to-purple-500 opacity-0 group-hover:opacity-100 transition-opacity"></div>
                                        <div className="flex justify-between items-start mb-4">
                                            <h3 className="text-lg font-semibold text-white">{committee.committeeName}</h3>
                                            <span className={`px-2 py-1 rounded-md text-xs font-medium border ${
                                                committee.status === 'ACTIVE' 
                                                ? 'bg-green-500/10 text-green-400 border-green-500/20' 
                                                : 'bg-gray-500/10 text-gray-400 border-gray-500/20'
                                            }`}>
                                                {committee.status}
                                            </span>
                                        </div>
                                        <p className="text-sm text-gray-400 line-clamp-2 mb-4 h-10">
                                            {committee.description || "No description provided."}
                                        </p>
                                        <div className="flex justify-between items-center text-xs text-gray-500 border-t border-white/5 pt-4">
                                            <span>{committee.advisors?.length || 0} Members Assigned</span>
                                            <span className="text-blue-400 group-hover:underline">View Details &rarr;</span>
                                        </div>
                                    </Link>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {/* Create Modal */}
                {showCreateModal && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
                        <div className="bg-gray-900 border border-white/10 w-full max-w-md rounded-2xl shadow-2xl p-6">
                            <h2 className="text-xl font-bold text-white mb-6">Create New Committee</h2>
                            <CreateCommitteeForm 
                                onSuccess={handleCreateSuccess} 
                                onCancel={() => setShowCreateModal(false)} 
                            />
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}
