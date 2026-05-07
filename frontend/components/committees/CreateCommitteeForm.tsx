"use client";

import { useState } from "react";
import { toast } from "sonner";
import { createCommittee } from "@/lib/committees-api";

interface CreateCommitteeFormProps {
    onSuccess: () => void;
    onCancel: () => void;
}

export default function CreateCommitteeForm({ onSuccess, onCancel }: CreateCommitteeFormProps) {
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        
        if (!name.trim()) {
            toast.error("Committee name is required");
            return;
        }

        setLoading(true);
        try {
            await createCommittee({ committeeName: name, description, status: "FORMING" });
            toast.success("Committee created successfully");
            onSuccess();
        } catch (error: unknown) {
            console.error("Failed to create committee", error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Committee Name</label>
                <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none"
                    placeholder="e.g. 2026 Senior Project Committee"
                />
            </div>
            
            <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Description (Optional)</label>
                <textarea
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none"
                    placeholder="Brief description of the committee"
                    rows={3}
                />
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-white/10">
                <button
                    type="button"
                    onClick={onCancel}
                    className="px-4 py-2 text-sm font-medium text-gray-400 hover:text-white transition-colors"
                    disabled={loading}
                >
                    Cancel
                </button>
                <button
                    type="submit"
                    disabled={loading || !name.trim()}
                    className="px-4 py-2 text-sm font-medium bg-blue-600 text-white rounded-lg hover:bg-blue-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {loading ? "Creating..." : "Create Committee"}
                </button>
            </div>
        </form>
    );
}
