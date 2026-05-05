package com.spms.backend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.config.OpenAiProperties;
import com.spms.backend.exception.P7ApiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Thin HTTP client for OpenAI Chat Completions API.
 * Uses structured JSON output (response_format: json_object) for deterministic parsing.
 * DFD 7.4 (review verification) and 7.5 (implementation validation).
 */
@Component
public class OpenAiValidationClient {

    private static final String COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

    private final RestClient restClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    public OpenAiValidationClient(RestClient.Builder restClientBuilder,
                                  OpenAiProperties openAiProperties,
                                  ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 7.4 — AI Review Verification.
     * Returns parsed map with: score, reviewQuality, hasChangeRequests,
     * hasSubstantiveComments, reviewerCount, aiFeedback.
     */
    public Map<String, Object> verifyReview(String model, String reviewsJson) {
        String systemPrompt = """
                You are an expert code review auditor. Evaluate whether a genuine, substantive code review occurred on a pull request.
                Return ONLY a valid JSON object with these fields:
                - score (integer 0-100): quality score
                - reviewQuality (string): one of INSUFFICIENT, MINIMAL, SUFFICIENT, THOROUGH
                - hasChangeRequests (boolean): true if reviewers requested code changes
                - hasSubstantiveComments (boolean): true if comments go beyond "LGTM"
                - reviewerCount (integer): number of distinct reviewers
                - aiFeedback (string): one or two sentence explanation
                """;
        String userMessage = "PR reviews data (JSON):\n" + reviewsJson;
        return callOpenAi(model, systemPrompt, userMessage);
    }

    /**
     * 7.5 — AI Implementation Validation.
     * Returns parsed map with: score, isValid, coverageAreas, missingRequirements,
     * aiFeedback, filesAnalyzed, diffTruncated.
     */
    public Map<String, Object> validateImplementation(String model, String issueDescription, String diffText,
                                                      int filesAnalyzed, boolean diffTruncated) {
        String systemPrompt = """
                You are a senior software engineer validating whether code changes match a given issue description.
                Return ONLY a valid JSON object with these fields:
                - score (integer 0-100): implementation match score
                - isValid (boolean): true if implementation adequately addresses the issue
                - coverageAreas (array of objects {requirement: string, covered: boolean})
                - missingRequirements (array of strings)
                - aiFeedback (string): one or two sentence explanation
                - filesAnalyzed (integer): number of files analyzed
                - diffTruncated (boolean): whether the diff was truncated
                """;
        String userMessage = String.format(
                "Issue description:\n%s\n\nChanged files diff (%d files analyzed, truncated=%b):\n%s",
                issueDescription, filesAnalyzed, diffTruncated, diffText);
        return callOpenAi(model, systemPrompt, userMessage);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callOpenAi(String model, String systemPrompt, String userMessage) {
        return callOpenAiWithRetry(model, systemPrompt, userMessage, 1);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callOpenAiWithRetry(String model, String systemPrompt, String userMessage,
                                                     int retriesLeft) {
        Map<String, Object> body = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(COMPLETIONS_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body((Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response == null) {
                throw new IllegalStateException("Empty response from OpenAI");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("No choices in OpenAI response");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            return objectMapper.readValue(content, (Class<Map<String, Object>>) (Class<?>) Map.class);

        } catch (RestClientException ex) {
            if (retriesLeft > 0) return callOpenAiWithRetry(model, systemPrompt, userMessage, retriesLeft - 1);
            throw new P7ApiException(HttpStatus.BAD_GATEWAY, "OPENAI_ERROR",
                    "OpenAI API call failed after retry");
        } catch (Exception ex) {
            if (retriesLeft > 0) return callOpenAiWithRetry(model, systemPrompt, userMessage, retriesLeft - 1);
            throw new P7ApiException(HttpStatus.BAD_GATEWAY, "OPENAI_ERROR",
                    "Failed to parse OpenAI response after retry");
        }
    }
}
