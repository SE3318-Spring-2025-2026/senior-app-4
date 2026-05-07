"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  type Annotation,
  type CreateAnnotationPayload,
  type SubmissionId,
  createAnnotation,
  deleteAnnotation,
  fetchAnnotations,
  updateAnnotation,
} from "@/lib/submissions-api";
import { toast } from "sonner";

const SOFT_GRADES = ["A", "B", "C", "D", "F"] as const;

interface AnnotatedDocumentViewerProps {
  submissionId: SubmissionId;
  htmlContent: string;
  criteria?: { id: number; name: string }[];
  readOnly?: boolean;
}

interface SelectionPopup {
  x: number;
  y: number;
  selectedText: string;
  startOffset: number;
  endOffset: number;
}

export default function AnnotatedDocumentViewer({
  submissionId,
  htmlContent,
  criteria = [],
  readOnly = false,
}: AnnotatedDocumentViewerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [annotations, setAnnotations] = useState<Annotation[]>([]);
  const [loading, setLoading] = useState(true);
  const [popup, setPopup] = useState<SelectionPopup | null>(null);
  const [editingAnnotation, setEditingAnnotation] = useState<Annotation | null>(null);
  const [form, setForm] = useState({ criterionId: "", comment: "", grade: "" });

  const loadAnnotations = useCallback(async () => {
    try {
      const data = await fetchAnnotations(submissionId);
      setAnnotations(data);
    } catch {
      // Non-critical: don't block the document view
    } finally {
      setLoading(false);
    }
  }, [submissionId]);

  useEffect(() => {
    loadAnnotations();
  }, [loadAnnotations]);

  // Close popup on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      const target = e.target as HTMLElement;
      if (!target.closest("[data-annotation-popup]")) {
        setPopup(null);
        setEditingAnnotation(null);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const handleMouseUp = useCallback(() => {
    if (readOnly) return;

    const selection = window.getSelection();
    if (!selection || selection.isCollapsed || !selection.toString().trim()) return;

    const range = selection.getRangeAt(0);
    const container = containerRef.current;
    if (!container || !container.contains(range.commonAncestorContainer)) return;

    // Calculate offsets relative to the container's text content
    const preRange = document.createRange();
    preRange.setStart(container, 0);
    preRange.setEnd(range.startContainer, range.startOffset);
    const startOffset = preRange.toString().length;
    const endOffset = startOffset + selection.toString().length;

    const rect = range.getBoundingClientRect();
    const containerRect = container.getBoundingClientRect();

    setForm({ criterionId: "", comment: "", grade: "" });
    setEditingAnnotation(null);
    setPopup({
      x: rect.left - containerRect.left + rect.width / 2,
      y: rect.bottom - containerRect.top + 8,
      selectedText: selection.toString().trim(),
      startOffset,
      endOffset,
    });
  }, [readOnly]);

  const handleSaveAnnotation = async () => {
    if (!popup) return;

    const payload: CreateAnnotationPayload = {
      selectedText: popup.selectedText,
      startOffset: popup.startOffset,
      endOffset: popup.endOffset,
      criterionId: form.criterionId ? Number(form.criterionId) : null,
      comment: form.comment || undefined,
      grade: form.grade || undefined,
    };

    try {
      const saved = await createAnnotation(submissionId, payload);
      setAnnotations((prev) => [...prev, saved]);
      setPopup(null);
      toast.success("Annotation saved.");
    } catch {
      toast.error("Failed to save annotation.");
    }
  };

  const handleUpdateAnnotation = async () => {
    if (!editingAnnotation) return;

    try {
      const updated = await updateAnnotation(submissionId, editingAnnotation.id, {
        criterionId: form.criterionId ? Number(form.criterionId) : null,
        comment: form.comment || undefined,
        grade: form.grade || undefined,
      });
      setAnnotations((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
      setEditingAnnotation(null);
      toast.success("Annotation updated.");
    } catch {
      toast.error("Failed to update annotation.");
    }
  };

  const handleDeleteAnnotation = async (annotationId: number) => {
    try {
      await deleteAnnotation(submissionId, annotationId);
      setAnnotations((prev) => prev.filter((a) => a.id !== annotationId));
      setEditingAnnotation(null);
      toast.success("Annotation removed.");
    } catch {
      toast.error("Failed to delete annotation.");
    }
  };

  const openEditPopup = (annotation: Annotation) => {
    setEditingAnnotation(annotation);
    setPopup(null);
    setForm({
      criterionId: annotation.criterionId?.toString() ?? "",
      comment: annotation.comment ?? "",
      grade: annotation.grade ?? "",
    });
  };

  // Highlight annotated text inside the rendered HTML
  const renderedContent = applyHighlights(htmlContent, annotations);

  return (
    <div className="relative space-y-4">
      {/* Document */}
      <div className="rounded-xl border border-white/10 bg-gray-950 overflow-hidden">
        <div className="border-b border-white/8 px-5 py-3 flex items-center justify-between">
          <p className="text-sm font-semibold text-white">Document</p>
          {!readOnly && (
            <p className="text-xs text-gray-500">Select any text to add an annotation</p>
          )}
        </div>

        <div
          ref={containerRef}
          onMouseUp={handleMouseUp}
          className="p-6 prose prose-invert prose-sm max-w-none select-text [&_mark]:bg-yellow-400/25 [&_mark]:text-inherit [&_mark]:rounded [&_mark]:cursor-pointer [&_mark]:px-0.5"
          dangerouslySetInnerHTML={{ __html: renderedContent }}
        />
      </div>

      {/* Selection popup — new annotation */}
      {popup && !editingAnnotation && (
        <div
          data-annotation-popup
          style={{ left: popup.x, top: popup.y }}
          className="absolute z-50 w-80 rounded-xl border border-white/15 bg-gray-900 shadow-2xl shadow-black/60 p-4 space-y-3"
        >
          <div>
            <p className="text-xs font-semibold text-white mb-1">Selected text</p>
            <p className="text-xs text-gray-400 line-clamp-3 italic">"{popup.selectedText}"</p>
          </div>

          <AnnotationForm
            form={form}
            onChange={setForm}
            criteria={criteria}
          />

          <div className="flex gap-2 pt-1">
            <button
              type="button"
              onClick={handleSaveAnnotation}
              className="flex-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-500 transition-colors"
            >
              Save annotation
            </button>
            <button
              type="button"
              onClick={() => setPopup(null)}
              className="rounded-lg border border-white/10 px-3 py-1.5 text-xs font-medium text-gray-400 hover:text-white transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Edit annotation panel */}
      {editingAnnotation && (
        <div
          data-annotation-popup
          className="rounded-xl border border-blue-500/20 bg-gray-900 p-4 space-y-3"
        >
          <div className="flex items-center justify-between">
            <p className="text-sm font-semibold text-white">Edit annotation</p>
            <button
              type="button"
              onClick={() => setEditingAnnotation(null)}
              className="text-gray-500 hover:text-white text-xs"
            >
              ✕
            </button>
          </div>
          <p className="text-xs text-gray-400 italic line-clamp-2">"{editingAnnotation.selectedText}"</p>

          <AnnotationForm
            form={form}
            onChange={setForm}
            criteria={criteria}
          />

          <div className="flex gap-2 pt-1">
            <button
              type="button"
              onClick={handleUpdateAnnotation}
              className="flex-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-500 transition-colors"
            >
              Update
            </button>
            <button
              type="button"
              onClick={() => handleDeleteAnnotation(editingAnnotation.id)}
              className="rounded-lg border border-red-500/30 px-3 py-1.5 text-xs font-medium text-red-400 hover:bg-red-500/10 transition-colors"
            >
              Delete
            </button>
          </div>
        </div>
      )}

      {/* Annotations list */}
      {!loading && annotations.length > 0 && (
        <div className="rounded-xl border border-white/8 bg-gray-950 overflow-hidden">
          <div className="border-b border-white/5 px-5 py-3">
            <p className="text-sm font-semibold text-white">
              Annotations
              <span className="ml-2 rounded-full bg-blue-500/15 px-2 py-0.5 text-xs text-blue-300">
                {annotations.length}
              </span>
            </p>
          </div>
          <div className="divide-y divide-white/5">
            {annotations.map((ann) => {
              const criterion = criteria.find((c) => c.id === ann.criterionId);
              return (
                <div key={ann.id} className="px-5 py-3 space-y-1">
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-xs text-gray-300 italic line-clamp-2">"{ann.selectedText}"</p>
                    {!readOnly && (
                      <button
                        type="button"
                        onClick={() => openEditPopup(ann)}
                        className="shrink-0 rounded px-2 py-0.5 text-xs text-gray-500 hover:bg-white/8 hover:text-white transition-colors"
                      >
                        Edit
                      </button>
                    )}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {criterion && (
                      <span className="rounded-full border border-purple-500/30 bg-purple-500/10 px-2 py-0.5 text-xs text-purple-300">
                        {criterion.name}
                      </span>
                    )}
                    {ann.grade && (
                      <span className="rounded-full border border-blue-500/30 bg-blue-500/10 px-2 py-0.5 text-xs font-semibold text-blue-300">
                        {ann.grade}
                      </span>
                    )}
                  </div>
                  {ann.comment && (
                    <p className="text-xs text-gray-400">{ann.comment}</p>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

function AnnotationForm({
  form,
  onChange,
  criteria,
}: {
  form: { criterionId: string; comment: string; grade: string };
  onChange: (f: { criterionId: string; comment: string; grade: string }) => void;
  criteria: { id: number; name: string }[];
}) {
  return (
    <div className="space-y-2">
      {criteria.length > 0 && (
        <div>
          <label className="text-xs text-gray-400 mb-1 block">Grading criterion</label>
          <select
            value={form.criterionId}
            onChange={(e) => onChange({ ...form, criterionId: e.target.value })}
            className="w-full rounded-lg border border-white/10 bg-gray-800 px-3 py-1.5 text-xs text-white outline-none"
          >
            <option value="">— None —</option>
            {criteria.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </div>
      )}

      <div>
        <label className="text-xs text-gray-400 mb-1 block">Grade</label>
        <select
          value={form.grade}
          onChange={(e) => onChange({ ...form, grade: e.target.value })}
          className="w-full rounded-lg border border-white/10 bg-gray-800 px-3 py-1.5 text-xs text-white outline-none"
        >
          <option value="">— None —</option>
          {SOFT_GRADES.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>
      </div>

      <div>
        <label className="text-xs text-gray-400 mb-1 block">Comment</label>
        <textarea
          value={form.comment}
          onChange={(e) => onChange({ ...form, comment: e.target.value })}
          rows={2}
          placeholder="Add a note about this section..."
          className="w-full rounded-lg border border-white/10 bg-gray-800 px-3 py-1.5 text-xs text-white placeholder-gray-600 outline-none resize-none focus:border-blue-500/50"
        />
      </div>
    </div>
  );
}

/**
 * Wraps annotated text ranges in <mark> tags inside the HTML string.
 * Works on the plain-text character offsets stored in the annotation.
 */
function applyHighlights(html: string, annotations: Annotation[]): string {
  if (annotations.length === 0) return html;

  // Build a plain-text version to locate offsets, then map back to HTML positions
  const temp = document.createElement("div");
  temp.innerHTML = html;
  const plainText = temp.textContent ?? "";

  // Sort by startOffset descending so we can splice without shifting indices
  const sorted = [...annotations].sort((a, b) => b.startOffset - a.startOffset);

  let result = html;
  for (const ann of sorted) {
    const chunk = plainText.slice(ann.startOffset, ann.endOffset);
    if (!chunk) continue;
    // Escape for use in regex
    const escaped = chunk.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    try {
      result = result.replace(
        new RegExp(escaped),
        `<mark data-annotation-id="${ann.id}" title="${ann.comment ?? ""}">${chunk}</mark>`,
      );
    } catch {
      // Skip if regex fails for unusual characters
    }
  }
  return result;
}
