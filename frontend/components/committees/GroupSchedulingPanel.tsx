"use client";

import React, { useState } from "react";
import { Clock, Calendar as CalendarIcon, Loader2, AlertCircle } from "lucide-react";
import { GroupAssignment, AvailabilitySlot } from "@/lib/committee-types";
import { scheduleGroupPresentation } from "@/lib/committee-assignment-api";
import { isOverlapping } from "@/lib/date-utils";
import { toast } from "sonner";
import { handleApiError } from "@/lib/error-handler";

interface GroupSchedulingPanelProps {
    committeeId: number;
    assignedGroups: GroupAssignment[];
    availabilities: AvailabilitySlot[];
    onScheduleUpdated: (updatedAssignment: GroupAssignment) => void;
}

export const GroupSchedulingPanel = ({
    committeeId,
    assignedGroups,
    availabilities,
    onScheduleUpdated
}: GroupSchedulingPanelProps) => {
    const [selectedAssignmentId, setSelectedAssignmentId] = useState<number | null>(null);
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [loading, setLoading] = useState(false);

    const unscheduledGroups = assignedGroups.filter(
        (g) => g.assignmentStatus !== "SCHEDULED" && !g.scheduledSlotId
    );

    const checkConflict = (start: string, end: string) => {
        return availabilities.some(slot =>
            slot.status === "UNAVAILABLE" &&
            isOverlapping(start, end, slot.startDateTime, slot.endDateTime)
        );
    };

    const handleSchedule = async (assignmentId: number) => {
        if (!startDate || !endDate) {
            toast.error("Please select both start and end times.");
            return;
        }

        if (new Date(startDate) >= new Date(endDate)) {
            toast.error("End time must be after start time.");
            return;
        }

        const durationMs = new Date(endDate).getTime() - new Date(startDate).getTime();
        const durationMinutes = durationMs / (1000 * 60);
        if (durationMinutes < 30) {
            toast.error("Presentation duration must be at least 30 minutes.");
            return;
        }

        if (checkConflict(startDate, endDate)) {
            toast.error("Conflict detected! A committee member is unavailable at this time.");
            return;
        }

        try {
            setLoading(true);
            const updated = await scheduleGroupPresentation(committeeId, assignmentId, startDate, endDate);
            onScheduleUpdated(updated);
            toast.success("Presentation scheduled successfully.");
            
            setSelectedAssignmentId(null);
            setStartDate("");
            setEndDate("");
        } catch (error) {
            handleApiError(error, "Failed to schedule presentation.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="rounded-2xl border border-white/10 bg-gray-900/70 shadow-sm backdrop-blur-sm p-4">
            <h3 className="text-sm font-semibold flex items-center gap-2 text-white mb-4">
                <Clock size={16} className="text-purple-500" />
                Schedule Presentations
            </h3>

            {unscheduledGroups.length === 0 ? (
                <p className="text-xs text-gray-400 italic">No assigned groups available for scheduling.</p>
            ) : (
                <div className="space-y-4">
                    <div>
                        <label className="block text-xs text-gray-400 mb-1">Select Group</label>
                        <select 
                            className="w-full bg-gray-800 border border-white/10 rounded-lg p-2 text-sm text-white focus:outline-none focus:border-purple-500"
                            value={selectedAssignmentId || ""}
                            onChange={(e) => setSelectedAssignmentId(Number(e.target.value))}
                        >
                            <option value="">-- Choose a group --</option>
                            {unscheduledGroups.map(group => (
                                <option key={group.assignmentId} value={group.assignmentId}>
                                    {group.groupName}
                                </option>
                            ))}
                        </select>
                    </div>

                    {selectedAssignmentId && (
                        <div className="space-y-3 bg-white/5 p-3 rounded-lg border border-white/5">
                            <div>
                                <label className="block text-xs text-gray-400 mb-1">Start Time</label>
                                <input 
                                    type="datetime-local" 
                                    className="w-full bg-gray-800 border border-white/10 rounded-lg p-2 text-sm text-white [color-scheme:dark]"
                                    value={startDate}
                                    onChange={(e) => setStartDate(e.target.value)}
                                />
                            </div>
                            <div>
                                <label className="block text-xs text-gray-400 mb-1">End Time</label>
                                <input 
                                    type="datetime-local" 
                                    className="w-full bg-gray-800 border border-white/10 rounded-lg p-2 text-sm text-white [color-scheme:dark]"
                                    value={endDate}
                                    onChange={(e) => setEndDate(e.target.value)}
                                />
                            </div>
                            
                            {startDate && endDate && checkConflict(startDate, endDate) && (
                                <div className="flex items-center gap-2 text-xs text-red-400 bg-red-500/10 p-2 rounded border border-red-500/20">
                                    <AlertCircle size={14} />
                                    <span>Time slot conflicts with a member's schedule.</span>
                                </div>
                            )}

                            <button
                                onClick={() => handleSchedule(selectedAssignmentId)}
                                disabled={loading || !startDate || !endDate || checkConflict(startDate, endDate)}
                                className="w-full mt-2 flex justify-center items-center gap-2 py-2 bg-purple-600 hover:bg-purple-700 disabled:opacity-50 disabled:hover:bg-purple-600 text-white text-sm font-medium rounded-lg transition-colors"
                            >
                                {loading ? <Loader2 size={16} className="animate-spin" /> : <CalendarIcon size={16} />}
                                Save Schedule
                            </button>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};