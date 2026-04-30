"use client";

import { Committee } from "@/lib/committee-types";

type Props = {
    committees: Committee[];
    onFilterChange?: (status: string) => void;
    onPageChange?: (page: number) => void;
    totalPages?: number;
    currentPage?: number;
};

export default function CommitteeList({
    committees,
    onFilterChange,
    onPageChange,
    totalPages = 1,
    currentPage = 1,
}: Props) {
    return (
        <div className="space-y-4">
            {/* FILTER */}
            <div>
                <label className="text-sm text-gray-300 block mb-2">
                    Status Filter
                </label>
                <select
                    onChange={(e) => onFilterChange?.(e.target.value)}
                    className="rounded-xl bg-gray-900 border border-white/10 px-4 py-2 text-white"
                >
                    <option value="ALL">All</option>
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="FORMING">FORMING</option>
                    <option value="INACTIVE">INACTIVE</option>
                    <option value="COMPLETED">COMPLETED</option>
                </select>
            </div>

            {/* LIST */}
            <div className="space-y-2">
                {committees.map((c) => (
                    <div
                        key={c.committeeId}
                        className="p-4 rounded-xl bg-white/5 border border-white/10"
                    >
                        <p className="text-white font-medium">{c.committeeName}</p>
                        <p className="text-sm text-gray-400">{c.status}</p>
                    </div>
                ))}

                {committees.length === 0 && (
                    <p className="text-gray-500 text-sm">No committees found.</p>
                )}
            </div>

            {/* PAGINATION */}
            <div className="flex gap-2">
                <button
                    onClick={() => onPageChange?.(currentPage - 1)}
                    disabled={currentPage <= 1}
                    className="px-3 py-2 rounded-lg bg-white/5 text-white disabled:opacity-40"
                >
                    Prev
                </button>

                <button
                    onClick={() => onPageChange?.(currentPage + 1)}
                    disabled={currentPage >= totalPages}
                    className="px-3 py-2 rounded-lg bg-white/5 text-white disabled:opacity-40"
                >
                    Next
                </button>
            </div>
        </div>
    );
}