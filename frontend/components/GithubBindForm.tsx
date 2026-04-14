"use client";

import { useState } from "react";

type Props = {
    onBind?: (githubPat: string, organizationName: string) => Promise<void> | void;
};

export default function GithubBindForm({ onBind }: Props) {
    const [githubPat, setGithubPat] = useState("");
    const [organizationName, setOrganizationName] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();
        setError("");

        const trimmedPat = githubPat.trim();
        const trimmedOrg = organizationName.trim();

        if (!trimmedPat) {
            setError("Personal Access Token is required.");
            return;
        }

        if (!trimmedOrg) {
            setError("Organization name is required.");
            return;
        }

        setLoading(true);

        try {
            await onBind?.(trimmedPat, trimmedOrg);

            setGithubPat("");
            setOrganizationName("");
        } catch {
            setError("Failed to bind GitHub organization.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
            <div className="mb-5">
                <h3 className="text-lg font-semibold text-white">Bind GitHub Organization</h3>
                <p className="mt-1 text-sm text-gray-400">
                    Enter a Personal Access Token and your GitHub organization name.
                </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                    <label
                        htmlFor="githubPat"
                        className="mb-2 block text-sm font-medium text-gray-300"
                    >
                        Personal Access Token
                    </label>
                    <input
                        id="githubPat"
                        type="password"
                        value={githubPat}
                        onChange={(e) => setGithubPat(e.target.value)}
                        placeholder="Enter GitHub PAT"
                        disabled={loading}
                        className="w-full rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white placeholder:text-gray-500 outline-none transition focus:border-blue-500/40 focus:ring-2 focus:ring-blue-500/20 disabled:opacity-60"
                    />
                </div>

                <div>
                    <label
                        htmlFor="organizationName"
                        className="mb-2 block text-sm font-medium text-gray-300"
                    >
                        Organization Name
                    </label>
                    <input
                        id="organizationName"
                        type="text"
                        value={organizationName}
                        onChange={(e) => setOrganizationName(e.target.value)}
                        placeholder="Enter organization name"
                        disabled={loading}
                        className="w-full rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white placeholder:text-gray-500 outline-none transition focus:border-blue-500/40 focus:ring-2 focus:ring-blue-500/20 disabled:opacity-60"
                    />
                </div>

                {error && (
                    <div className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-300">
                        {error}
                    </div>
                )}

                <button
                    type="submit"
                    disabled={loading}
                    className="inline-flex items-center justify-center rounded-xl bg-blue-600 px-4 py-3 text-sm font-medium text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {loading ? "Binding..." : "Bind GitHub"}
                </button>
            </form>
        </div>
    );
}