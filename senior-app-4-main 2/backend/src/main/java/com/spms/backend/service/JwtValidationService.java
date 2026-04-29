package com.spms.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.config.TokenProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtValidationService {

    private final TokenProperties tokenProperties;
    private final ObjectMapper objectMapper;

    public JwtValidationService(TokenProperties tokenProperties, ObjectMapper objectMapper) {
        this.tokenProperties = tokenProperties;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> validateAndParse(String token) {
        if (token == null || token.isBlank()) return null;

        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;

        try {
            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expectedSignature, parts[2])) return null;

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, new TypeReference<>() {});

            Number exp = (Number) payload.get("exp");
            if (exp == null || Instant.now().getEpochSecond() > exp.longValue()) return null;

            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    tokenProperties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Token signing failed.", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) return false;
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) result |= aBytes[i] ^ bBytes[i];
        return result == 0;
    }
}
