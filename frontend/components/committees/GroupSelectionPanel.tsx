"use client";

import React, { useEffect, useState } from "react";
import { Users, Plus, Loader2, ShieldAlert } from "lucide-react";
import { StudentGroup, GroupAssignment } from "@/lib/committee-types";
import { fetchAvailableGroups, assignGroupToCommittee } from "@/lib/committee-assignment-api";
import { toast } from "sonner";
import { handleApiError } from "@/lib/error-handler";

interface GroupSelectionPanelProps {
    committeeId: number;
    onGroupAssigned: (assignment: GroupAssignment) => void;
    currentGroupCount: number;     
    maxGroupsAllowed: number;      
}

export const GroupSelectionPanel = ({ 
    committeeId, 
    onGroupAssigned,
    currentGroupCount,
    maxGroupsAllowed
}: GroupSelectionPanelProps) => {
    const [groups, setGroups] = useState<StudentGroup[]>([]);
    const [loading, setLoading] = useState(true);
    const [assigningId, setAssigningId] = useState<number | null>(null);

    const isCapacityReached = currentGroupCount >= maxGroupsAllowed;

    useEffect(() => {
        
        if (isCapacityReached) {
            setLoading(false);
            return;
        }

        const loadGroups = async () => {
            try {
                setLoading(true);
                const data = await fetchAvailableGroups(committeeId);
                setGroups(data);
            } catch (error) {
                handleApiError(error, "Failed to load available groups.");
            } finally {
                setLoading(false);
            }
        };
        loadGroups();
    }, [committeeId, isCapacityReached]);

    const handleAssign = async (group: StudentGroup) => {
        if (isCapacityReached) {
            toast.error("Committee capacity has been reached.");
            return;
        }

        try {
            setAssigningId(group.groupId);
            const newAssignment = await assignGroupToCommittee(committeeId, group.groupId);
            
            setGroups((prev) => prev.filter((g) => g.groupId !== group.groupId));
            onGroupAssigned(newAssignment);
            
            toast.success(`Group ${group.groupName} assigned successfully.`);
        } catch (error) {
            handleApiError(error, "Failed to assign group.");
        } finally {
            setAssigningId(null);
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center p-8">
                <Loader2 className="h-8 w-8 animate-spin text-gray-500" />
            </div>
        );
    }

    return (
        <div className="rounded-2xl border border-white/10 bg-gray-900/70 shadow-sm backdrop-blur-sm overflow-hidden">
            <div className="p-4 border-b border-white/5 flex justify-between items-center">
                <h3 className="text-sm font-semibold flex items-center gap-2 text-white">
                    <Users size={16} className="text-blue-500" />
                    Available Groups
                </h3>
                <span className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${isCapacityReached ? 'bg-red-500/10 text-red-400 border border-red-500/20' : 'bg-blue-500/10 text-blue-400 border border-blue-500/20'}`}>
                    {currentGroupCount} / {maxGroupsAllowed} Assigned
                </span>
            </div>
            
            <div className="p-2 max-h-[300px] overflow-y-auto custom-scrollbar">
                {isCapacityReached ? (
                    <div className="flex flex-col items-center justify-center py-8 px-4 text-center">
                        <ShieldAlert className="text-red-400/50 mb-3" size={32} />
                        <p className="text-sm font-medium text-red-400">Capacity Reached</p>
                        <p className="text-xs text-gray-500 mt-1">This committee cannot accept any more groups.</p>
                    </div>
                ) : groups.length === 0 ? (
                    <p className="text-xs text-gray-400 text-center py-6 italic">
                        No groups left to assign.
                    </p>
                ) : (
                    groups.map((group) => (
                        <div
                            key={group.groupId}
                            className="flex items-center justify-between p-3 mb-1 rounded-lg hover:bg-gray-800 transition-colors border border-transparent hover:border-white/5 group"
                        >
                            <div className="min-w-0 flex-1">
                                <div className="flex items-center gap-2 mb-0.5">
                                    <span className="text-sm font-medium text-white truncate">
                                        {group.groupName}
                                    </span>
                                    <span className="shrink-0 text-[9px] px-1.5 py-0.5 bg-gray-800 text-gray-400 rounded-full border border-white/5">
                                        {group.membersCount} Members
                                    </span>
                                </div>
                                <p className="text-[11px] text-gray-500 italic truncate pr-4">
                                    {group.projectTitle}
                                </p>
                            </div>
                            
                            <button
                                onClick={() => handleAssign(group)}
                                disabled={assigningId !== null || isCapacityReached}
                                className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-blue-400 bg-blue-500/10 hover:bg-blue-500/20 rounded-lg transition-all disabled:opacity-50 shrink-0"
                            >
                                {assigningId === group.groupId ? (
                                    <Loader2 size={12} className="animate-spin" />
                                ) : (
                                    <Plus size={14} />
                                )}
                                Assign
                            </button>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};