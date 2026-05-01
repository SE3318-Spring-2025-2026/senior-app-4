package com.spms.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/github/webhook")
public class GithubWebhookController {

    @Value("${github.webhook.secret:my_super_secret_key}")
    private String githubSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/pr-data")
    public ResponseEntity<?> receivePrData(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload) {

        // 1. GERÇEK GÜVENLİK KONTROLÜ
        if (signature == null || !isValidSignature(signature, rawPayload)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid signature"));
        }

        try {
            // 2. İmzayı doğruladıktan sonra String'i Map'e çeviriyoruz
            Map<String, Object> payload = objectMapper.readValue(rawPayload, new TypeReference<>() {
            });

            String action = (String) payload.get("action");
            @SuppressWarnings("unchecked")
            Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");

            if (pr != null) {
                Boolean merged = (Boolean) pr.get("merged");

                if ("closed".equals(action) && Boolean.FALSE.equals(merged)) {
                    return ResponseEntity.ok(Map.of("processed", false, "reason", "Not merged"));
                }

                if (Boolean.TRUE.equals(merged)) {
                    return ResponseEntity.ok(Map.of("processed", true, "reason", "PR merged successfully"));
                }
            }
            return ResponseEntity.ok(Map.of("processed", true, "message", "Webhook received"));

        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid JSON payload"));
        }
    }

    /**
     * Gerçek HMAC SHA-256 Şifreleme ve Doğrulama Algoritması
     */
    private boolean isValidSignature(String signature, String rawPayload) {
        try {
            // HMAC SHA256 algoritmasını kuruyoruz
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(githubSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKeySpec);

            // Gelen ham veriyi kendi gizli anahtarımızla şifreliyoruz
            byte[] hash = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));

            // Byte dizisini Hexadecimal String'e çeviriyoruz (GitHub formatı)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            String expectedSignature = "sha256=" + hexString.toString();

            // Zamanlama saldırılarını (Timing Attack) önlemek için MessageDigest.isEqual
            // kullanılır
            return MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            return false; // Herhangi bir hata olursa (algoritma bulunamadı vs.) kapıyı kapat
        }
    }
}