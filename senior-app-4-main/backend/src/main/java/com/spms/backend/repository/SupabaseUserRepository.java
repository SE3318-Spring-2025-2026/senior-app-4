package com.spms.backend.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.config.SupabaseProperties;
import com.spms.backend.model.User;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class SupabaseUserRepository implements UserRepository {

    private static final String TABLE_PATH = "/rest/v1/users";

    private final RestClient restClient;
    private final SupabaseProperties supabaseProperties;
    private final ObjectMapper objectMapper;

    public SupabaseUserRepository(RestClient.Builder restClientBuilder,
                                  SupabaseProperties supabaseProperties,
                                  ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.supabaseProperties = supabaseProperties;
        this.objectMapper = objectMapper;
    }

    // ═══════════════════════════════════════════════════════════════
    //  FIND METOTLARI
    // ═══════════════════════════════════════════════════════════════

    @Override
    public Optional<User> findByEmail(String email) {
        if (!StringUtils.hasText(email)) return Optional.empty();
        return findOneByColumn("email", email.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public Optional<User> findByStudentId(String studentId) {
        if (!StringUtils.hasText(studentId)) return Optional.empty();
        return findOneByColumn("student_id", studentId.trim());
    }

    @Override
    public Optional<User> findByUserId(Long userId) {
        if (userId == null) return Optional.empty();
        return findOneByColumn("user_id", userId.toString());
    }

    @Override
    public Optional<User> findByGithubUsername(String githubUsername) {
        if (!StringUtils.hasText(githubUsername)) return Optional.empty();
        return findOneByColumn("github_username", githubUsername.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public List<User> findAll() {
        String url = tableUrl() + "?select=*";
        String json = doGet(url);
        return parseJsonArray(json).stream()
                .map(this::mapToUser)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findAllByRole(String role) {
        String url = tableUrl() + "?role=eq." + role + "&select=*";
        String json = doGet(url);
        return parseJsonArray(json).stream()
                .map(this::mapToUser)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    //  SAVE METODU
    // ═══════════════════════════════════════════════════════════════

    @Override
    public User save(User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (user.getUserId() == null) {
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(Instant.now());
            }
            return insertUser(user);
        } else {
            return updateUser(user);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DELETE METODU
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean deleteByUserId(Long userId) {
        if (userId == null) return false;

        String url = tableUrl() + "?user_id=eq." + userId;

        restClient.delete()
                .uri(url)
                .header("apikey", supabaseProperties.getServiceKey())
                .header("Authorization", "Bearer " + supabaseProperties.getServiceKey())
                .retrieve()
                .toBodilessEntity();

        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    //  YARDIMCI METOTLAR
    // ═══════════════════════════════════════════════════════════════

    private String tableUrl() {
        return supabaseProperties.getUrl() + TABLE_PATH;
    }

    private String doGet(String url) {
        return restClient.get()
                .uri(url)
                .header("apikey", supabaseProperties.getServiceKey())
                .header("Authorization", "Bearer " + supabaseProperties.getServiceKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
    }

    private Optional<User> findOneByColumn(String columnName, String value) {
        String url = tableUrl() + "?" + columnName + "=eq." + value + "&select=*";
        String json = doGet(url);
        List<Map<String, Object>> rows = parseJsonArray(json);
        if (rows == null || rows.isEmpty()) return Optional.empty();
        return Optional.of(mapToUser(rows.get(0)));
    }

    private User insertUser(User user) {
        Map<String, Object> body = userToMap(user);

        String json = restClient.post()
                .uri(tableUrl())
                .header("apikey", supabaseProperties.getServiceKey())
                .header("Authorization", "Bearer " + supabaseProperties.getServiceKey())
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(body))
                .retrieve()
                .body(String.class);

        List<Map<String, Object>> rows = parseJsonArray(json);
        if (rows == null || rows.isEmpty()) {
            throw new RuntimeException("Supabase INSERT did not return data.");
        }
        return mapToUser(rows.get(0));
    }

    private User updateUser(User user) {
        Map<String, Object> body = userToMap(user);
        String url = tableUrl() + "?user_id=eq." + user.getUserId();

        String json = restClient.patch()
                .uri(url)
                .header("apikey", supabaseProperties.getServiceKey())
                .header("Authorization", "Bearer " + supabaseProperties.getServiceKey())
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(body))
                .retrieve()
                .body(String.class);

        List<Map<String, Object>> rows = parseJsonArray(json);
        if (rows == null || rows.isEmpty()) {
            throw new RuntimeException("Supabase UPDATE did not return data.");
        }
        return mapToUser(rows.get(0));
    }

    // ═══════════════════════════════════════════════════════════════
    //  JSON ↔ Java DÖNÜŞÜM
    // ═══════════════════════════════════════════════════════════════

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (user.getUserId() != null) map.put("user_id", user.getUserId());
        if (user.getFullName() != null) map.put("full_name", user.getFullName());
        if (user.getEmail() != null) map.put("email", user.getEmail());
        if (user.getPasswordHash() != null) map.put("password_hash", user.getPasswordHash());
        if (user.getStudentId() != null) map.put("student_id", user.getStudentId());
        if (user.getGithubUsername() != null) map.put("github_username", user.getGithubUsername());
        if (user.getRole() != null) map.put("role", user.getRole());
        map.put("requires_password_change", user.isRequiresPasswordChange());
        if (user.getCreatedAt() != null) map.put("created_at", user.getCreatedAt().toString());
        return map;
    }

    private User mapToUser(Map<String, Object> row) {
        User user = new User();
        if (row.get("user_id") != null) user.setUserId(((Number) row.get("user_id")).longValue());
        user.setFullName((String) row.get("full_name"));
        user.setEmail((String) row.get("email"));
        user.setPasswordHash((String) row.get("password_hash"));
        user.setStudentId((String) row.get("student_id"));
        user.setGithubUsername((String) row.get("github_username"));
        user.setRole((String) row.get("role"));
        if (row.get("requires_password_change") != null) {
            user.setRequiresPasswordChange((Boolean) row.get("requires_password_change"));
        }
        if (row.get("created_at") != null) {
            user.setCreatedAt(Instant.parse(row.get("created_at").toString()));
        }
        return user;
    }

    private List<Map<String, Object>> parseJsonArray(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse Supabase response: " + json, e);
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON.", e);
        }
    }
}
