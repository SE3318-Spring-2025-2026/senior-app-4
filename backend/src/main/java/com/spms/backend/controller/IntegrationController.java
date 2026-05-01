package com.spms.backend.controller;

import com.spms.backend.dto.response.CredentialsResponse;
import com.spms.backend.service.IntegrationCredentialsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationController {

    private final IntegrationCredentialsService integrationCredentialsService;

    public IntegrationController(IntegrationCredentialsService integrationCredentialsService) {
        this.integrationCredentialsService = integrationCredentialsService;
    }

    @GetMapping("/{teamId}/credentials")
    public ResponseEntity<?> getCredentials(
            @PathVariable Long teamId,
            @RequestHeader(value = "X-Team-Token", required = false) String teamToken) {

        if (teamToken == null || teamToken.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", "Invalid or expired team token."));
        }

        if (!isValidTeamToken(teamToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", "Invalid or expired team token."));
        }

        try {
            List<CredentialsResponse> credentials = integrationCredentialsService.getCredentialsByTeamId(teamId);
            return ResponseEntity.ok(credentials);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", "Invalid or expired team token."));
        }
    }

    private boolean isValidTeamToken(String token) {
        return token != null && !token.isEmpty() && !token.equalsIgnoreCase("EXPIRED_TOKEN");
    }

    private Map<String, String> createErrorResponse(String error, String message) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("error", error);
        response.put("message", message);
        return response;
    }
}
