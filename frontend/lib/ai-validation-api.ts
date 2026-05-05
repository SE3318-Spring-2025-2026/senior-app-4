// TODO(parallel: #298, @you, 2026-05-05): GET/PUT /ai-validation/config frontend page
//   Affects: app/coordinator/ai-validation/config/page.tsx
//   Coordinate before editing: check with team

import { API_BASE, buildHeaders, parseError } from "@/lib/api-utils";

// ── Types ──────────────────────────────────────────────────────────────────

/** Spec: ValidationConfigResponse.data */
export interface ValidationConfigData {
  reviewWeight: number;
  implementationWeight: number;
  openaiModel: string;
  maxDiffLines: number;
  excludedFilePatterns: string[];
}

/** Spec: ValidationConfigResponse envelope */
export interface ValidationConfigResponse {
  status: string;
  data: ValidationConfigData;
}

/** Spec: ValidationConfigRequest (required: reviewWeight, implementationWeight) */
export interface ValidationConfigRequest {
  reviewWeight: number;
  implementationWeight: number;
  openaiModel?: string;
  maxDiffLines?: number;
  excludedFilePatterns?: string[];
}

// ── API functions ──────────────────────────────────────────────────────────

/**
 * GET /api/v1/ai-validation/config
 * Coordinator only. Returns the current AI validation configuration.
 */
export async function fetchValidationConfig(): Promise<ValidationConfigResponse> {
  const res = await fetch(`${API_BASE}/ai-validation/config`, {
    headers: buildHeaders(),
  });

  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }

  return res.json();
}

/**
 * PUT /api/v1/ai-validation/config
 * Coordinator only. Updates and returns the new AI validation configuration.
 *
 * Business rules enforced server-side:
 * - reviewWeight + implementationWeight must equal 100 → INVALID_WEIGHTS
 * - maxDiffLines (if provided) must be ≥ 1 → INVALID_MAX_DIFF_LINES
 * - openaiModel (if provided) must be one of gpt-4o, gpt-4o-mini, gpt-4-turbo → INVALID_OPENAI_MODEL
 */
export async function updateValidationConfig(
  payload: ValidationConfigRequest
): Promise<ValidationConfigResponse> {
  const res = await fetch(`${API_BASE}/ai-validation/config`, {
    method: "PUT",
    headers: buildHeaders(),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const msg = await parseError(res);
    throw new Error(msg);
  }

  return res.json();
}
