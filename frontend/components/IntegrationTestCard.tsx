"use client";

import { useState } from "react";
import { IntegrationTestResult } from "@/lib/github-types";

type Props = {
    groupId: number;
    mockResult?: IntegrationTestResult;
};

export default function IntegrationTestCard({ groupId, mockResult }: Props) {
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<IntegrationTestResult | null>(null);

    async function handleTest() {
        setLoading(true);

        try {
            await new Promise((r) => setTimeout(r, 1000));

            if (mockResult) {
                setResult(mockResult);
            }
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
            <div className="mb-4 flex items-center justify-between">
                <h3 className="text-sm text-gray-400">Connection Test</h3>

                <button
                    onClick={handleTest}
                    disabled={loading}
                    className="rounded-lg bg-blue-600 px-4 py-2 text-sm transition hover:bg-blue-500 disabled:opacity-50"
                >
                    {loading ? "Testing..." : "Test Connection"}
                </button>
            </div>

            {!result && !loading && (
                <p className="mt-2 text-sm text-gray-500">
                    Click "Test Connection" to verify GitHub and JIRA integrations.
                </p>
            )}

            {result && (
                <div className="mt-4 space-y-3 text-sm">
                    <div className="flex items-center justify-between">
                        <span className="text-gray-300">GitHub</span>
                        <span
                            className={
                                result.github.connected ? "text-green-400" : "text-red-400"
                            }
                        >
                            {result.github.connected ? "✔ Connected" : "✖ Failed"}
                        </span>
                    </div>

                    <div className="text-xs text-gray-500">{result.github.message}</div>

                    <div className="mt-3 flex items-center justify-between">
                        <span className="text-gray-300">JIRA</span>
                        <span
                            className={
                                result.jira.connected ? "text-green-400" : "text-red-400"
                            }
                        >
                            {result.jira.connected ? "✔ Connected" : "✖ Failed"}
                        </span>
                    </div>

                    <div className="text-xs text-gray-500">{result.jira.message}</div>
                </div>
            )}
        </div>
    );
}