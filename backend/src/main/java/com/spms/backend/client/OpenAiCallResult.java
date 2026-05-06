package com.spms.backend.client;

import java.util.Map;

/**
 * Result of a single OpenAI Chat Completions call.
 * Carries the parsed JSON payload alongside metadata needed for D9 audit logging.
 */
public record OpenAiCallResult(Map<String, Object> parsed, int httpStatus, int tokenCount) {}
