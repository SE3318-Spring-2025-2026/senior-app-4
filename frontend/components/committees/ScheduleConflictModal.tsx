"use client";

import { useEffect, useState } from "react";

export interface ScheduleConflictData {
  message: string;
  conflictingGroup?: string;
  conflictingDate?: string;
  suggestedAlternativeDates?: string[];
}

export default function ScheduleConflictModal({
  isOpen,
  onClose,
  conflictData,
}: {
  isOpen: boolean;
  onClose: () => void;
  conflictData: ScheduleConflictData | null;
}) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  
  if (!mounted || !isOpen || !conflictData) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="bg-gray-900 border border-white/10 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
        <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3 bg-red-500/10">
          <div className="w-8 h-8 rounded-full bg-red-500/20 flex items-center justify-center shrink-0">
            <svg className="w-4 h-4 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h2 className="text-sm font-semibold text-white">Schedule Conflict</h2>
        </div>
        <div className="p-6 space-y-4">
          <p className="text-sm text-gray-300 leading-relaxed">{conflictData.message}</p>
          
          {(conflictData.conflictingGroup || conflictData.conflictingDate) && (
            <div className="bg-white/5 border border-white/10 rounded-lg p-3 space-y-1">
              {conflictData.conflictingGroup && (
                <p className="text-xs text-gray-400"><span className="text-gray-500">Conflicting Group:</span> {conflictData.conflictingGroup}</p>
              )}
              {conflictData.conflictingDate && (
                <p className="text-xs text-gray-400"><span className="text-gray-500">Date/Time:</span> {new Date(conflictData.conflictingDate).toLocaleString()}</p>
              )}
            </div>
          )}

          {conflictData.suggestedAlternativeDates && conflictData.suggestedAlternativeDates.length > 0 && (
            <div className="space-y-2">
              <p className="text-xs font-semibold text-blue-400 uppercase tracking-wider">Suggested Alternatives</p>
              <div className="flex flex-wrap gap-2">
                {conflictData.suggestedAlternativeDates.map((d, i) => (
                  <span key={i} className="text-xs bg-blue-500/10 text-blue-300 border border-blue-500/20 px-2 py-1 rounded-md">
                    {new Date(d).toLocaleString()}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
        <div className="px-6 py-4 border-t border-white/5 bg-white/5 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-white/10 hover:bg-white/15 text-white rounded-lg text-sm font-medium transition-colors"
          >
            Acknowledge
          </button>
        </div>
      </div>
    </div>
  );
}
