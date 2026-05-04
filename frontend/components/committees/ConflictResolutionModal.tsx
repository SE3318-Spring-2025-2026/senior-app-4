"use client";

import React from "react";
import { AlertTriangle, CalendarCheck } from "lucide-react";

interface ConflictResolutionModalProps {
    isOpen: boolean;
    conflictData: {
        conflictingGroup?: string;
        conflictingDate?: string;
        suggestedDates?: string[];
    } | null;
    onClose: () => void;
    onSelectAlternative: (date: string) => void;
}

export const ConflictResolutionModal = ({ isOpen, conflictData, onClose, onSelectAlternative }: ConflictResolutionModalProps) => {
    if (!isOpen || !conflictData) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
            <div className="w-full max-w-md bg-gray-900 border border-red-500/30 rounded-2xl p-6 shadow-2xl">
                <div className="flex items-center gap-3 text-red-400 mb-4">
                    <AlertTriangle size={24} />
                    <h2 className="text-lg font-bold">Schedule Conflict Detected</h2>
                </div>
                
                <p className="text-sm text-gray-300 mb-4">
                    The proposed time overlaps with <span className="text-white font-semibold">{conflictData.conflictingGroup || "another group"}</span>'s 
                    exam on <span className="text-white font-semibold">{conflictData.conflictingDate}</span>.
                </p>

                {conflictData.suggestedDates && conflictData.suggestedDates.length > 0 && (
                    <div className="mb-6">
                        <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Suggested Alternatives:</h3>
                        <div className="space-y-2">
                            {conflictData.suggestedDates.map((date, index) => (
                                <button
                                    key={index}
                                    onClick={() => onSelectAlternative(date)}
                                    className="w-full flex items-center justify-between p-3 bg-white/5 hover:bg-purple-500/20 border border-white/10 hover:border-purple-500/50 rounded-xl transition-all text-sm text-white"
                                >
                                    <span>{new Date(date).toLocaleString()}</span>
                                    <CalendarCheck size={16} className="text-purple-400" />
                                </button>
                            ))}
                        </div>
                    </div>
                )}

                <button onClick={onClose} className="w-full py-2.5 text-sm font-medium text-gray-400 hover:text-white transition-colors">
                    Cancel and Change Manually
                </button>
            </div>
        </div>
    );
};