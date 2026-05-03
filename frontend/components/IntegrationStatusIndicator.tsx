"use client";

import { useState, useEffect } from "react";
import { fetchIntegrationStatus, GlobalIntegrationStatusApiResponse } from "@/lib/integrations-api";

export function useIntegrationStatus() {
    const [statusData, setStatusData] = useState<GlobalIntegrationStatusApiResponse["data"] | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let isMounted = true;
        async function loadStatus() {
            setLoading(true);
            try {
                const result = await fetchIntegrationStatus();
                if (isMounted) {
                    setStatusData(result.data);
                }
            } catch (error) {
                if (isMounted) {
                    setStatusData({
                        connected: false,
                        lastSynced: null,
                        message: "Failed to fetch status"
                    });
                }
            } finally {
                if (isMounted) {
                    setLoading(false);
                }
            }
        }

        loadStatus();
        return () => {
            isMounted = false;
        };
    }, []);

    return { statusData, loading };
}

export default function IntegrationStatusIndicator() {
    const { statusData, loading } = useIntegrationStatus();

    if (loading) {
        return (
            <div className="flex items-center gap-3 bg-gray-900/80 border border-white/10 rounded-2xl p-4 shadow-lg w-full sm:w-auto sm:min-w-[320px] animate-pulse">
                <div className="w-3 h-3 rounded-full bg-gray-700"></div>
                <div className="space-y-2 flex-1">
                    <div className="h-4 bg-gray-700 rounded w-1/2"></div>
                    <div className="h-3 bg-gray-700 rounded w-1/3"></div>
                </div>
            </div>
        );
    }

    const isConnected = statusData?.connected === true;

    const formatTimeAgo = (dateString?: string | null) => {
        if (!dateString) return "Never";
        const date = new Date(dateString);
        const diffMs = Date.now() - date.getTime();
        const diffMins = Math.floor(diffMs / 60000);
        
        if (diffMins < 1) return "Just now";
        if (diffMins < 60) return `${diffMins} mins ago`;
        
        const diffHours = Math.floor(diffMins / 60);
        if (diffHours < 24) return `${diffHours} hours ago`;
        
        return date.toLocaleDateString();
    };

    return (
        <div className="flex items-center justify-between gap-6 bg-gray-900/80 border border-white/10 rounded-2xl p-4 shadow-lg w-full sm:w-auto sm:min-w-[320px]">
            <div className="flex items-center gap-3">
                <div className="relative flex items-center justify-center">
                    {isConnected && (
                        <div className="absolute inset-0 bg-green-500 rounded-full blur-sm opacity-50 animate-pulse"></div>
                    )}
                    <div
                        className={`w-3 h-3 rounded-full z-10 ${
                            isConnected ? "bg-green-500" : "bg-red-500"
                        }`}
                    ></div>
                </div>
                <div>
                    <p className="text-sm font-medium text-white">
                        {isConnected ? "Connected" : "No Connection / Disconnected"}
                    </p>
                    <p className="text-xs text-gray-400">
                        Global Integration Status
                    </p>
                </div>
            </div>
            
            <div className="text-right">
                <p className="text-xs text-gray-500 mb-0.5">Last Synced</p>
                <p className="text-xs font-medium text-gray-300">
                    {formatTimeAgo(statusData?.lastSynced)}
                </p>
            </div>
        </div>
    );
}
