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
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Thin HTTP client for OpenAI Chat Completions API.
 * Makes a single call per invocation — retry logic lives in the orchestrator
 * so each attempt can be independently logged to D9.
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
     * 7.4 — AI Review Verification. Single attempt; caller handles retry.
     */
    public OpenAiCallResult verifyReview(String model, String reviewsJson) {
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
        return callOpenAi(model, systemPrompt, "PR reviews data (JSON):\n" + reviewsJson);
    }

    /**
     * 7.5 — AI Implementation Validation. Single attempt; caller handles retry.
     */
    public OpenAiCallResult validateImplementation(String model, String issueDescription, String diffText,
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
    private OpenAiCallResult callOpenAi(String model, String systemPrompt, String userMessage) {
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
            Map<String, Object> parsed = objectMapper.readValue(
                    content, (Class<Map<String, Object>>) (Class<?>) Map.class);

            return new OpenAiCallResult(parsed, 200, extractTokenCount(response));

        } catch (RestClientResponseException ex) {
            throw new P7ApiException(HttpStatus.BAD_GATEWAY, "OPENAI_ERROR",
                    "OpenAI API error: HTTP " + ex.getStatusCode().value());
        } catch (RestClientException ex) {
            throw new P7ApiException(HttpStatus.BAD_GATEWAY, "OPENAI_ERROR",
                    "OpenAI API call failed");
        } catch (P7ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new P7ApiException(HttpStatus.BAD_GATEWAY, "OPENAI_ERROR",
                    "Failed to parse OpenAI response");
        }
    }

    @SuppressWarnings("unchecked")
    private int extractTokenCount(Map<String, Object> response) {
        try {
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            if (usage != null) {
                Object tokens = usage.get("completion_tokens");
                if (tokens instanceof Number n) return n.intValue();
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
