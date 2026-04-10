package com.spms.backend.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.config.SupabaseProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class SupabaseValidStudentIdRepository implements ValidStudentIdRepository {

    private static final String TABLE_PATH = "/rest/v1/valid_student_ids";

    private final RestClient restClient;
    private final SupabaseProperties supabaseProperties;
    private final ObjectMapper objectMapper;

    public SupabaseValidStudentIdRepository(RestClient.Builder restClientBuilder,
                                            SupabaseProperties supabaseProperties,
                                            ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.supabaseProperties = supabaseProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean existsByStudentId(String studentId) {
        if (!StringUtils.hasText(studentId)) return false;
        String url = tableUrl() + "?student_id=eq." + studentId.trim() + "&select=student_id";
        String json = doGet(url);
        List<Map<String, Object>> rows = parseJsonArray(json);
        return rows != null && !rows.isEmpty();
    }

    @Override
    public void save(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            throw new IllegalArgumentException("studentId must not be blank");
        }
        String body = "{\"student_id\":\"" + studentId.trim() + "\",\"status\":\"valid\"}";
        restClient.post()
                .uri(tableUrl())
                .header("apikey", supabaseProperties.getServiceKey())
                .header("Authorization", "Bearer " + supabaseProperties.getServiceKey())
                .header("Prefer", "return=minimal")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public Map<String, String> findByStudentId(String studentId) {
        if (!StringUtils.hasText(studentId)) return null;
        String url = tableUrl() + "?student_id=eq." + studentId.trim() + "&select=*";
        String json = doGet(url);
        List<Map<String, Object>> rows = parseJsonArray(json);
        if (rows == null || rows.isEmpty()) return null;
        Map<String, Object> row = rows.get(0);
        return Map.of(
                "studentId", String.valueOf(row.get("student_id")),
                "status", String.valueOf(row.get("status"))
        );
    }

    @Override
    public List<Map<String, String>> findAll() {
        String url = tableUrl() + "?select=*";
        String json = doGet(url);
        return parseJsonArray(json).stream()
                .map(row -> Map.of(
                        "studentId", String.valueOf(row.get("student_id")),
                        "status", String.valueOf(row.get("status"))
                ))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteByStudentId(String studentId) {
        if (!StringUtils.hasText(studentId)) return false;
        String url = tableUrl() + "?student_id=eq." + studentId.trim();
        restClient.delete()
                .uri(url)
                .header("apikey", supabaseProperties.getServiceKey())
                .header("Authorization", "Bearer " + supabaseProperties.getServiceKey())
                .retrieve()
                .toBodilessEntity();
        return true;
    }

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

    private List<Map<String, Object>> parseJsonArray(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse Supabase response: " + json, e);
        }
    }
}
