package com.spms.backend.service.impl;

import com.spms.backend.exception.BadRequestException;
import com.spms.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Primary
@Service
public class SupabaseFileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final long MAX_SIZE_BYTES = 20 * 1024 * 1024; // 20 MB

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.storage.bucket:submissions}")
    private String bucket;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("File exceeds the 20 MB size limit.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("File type not allowed. Accepted: pdf, doc, docx.");
        }

        String storedName = UUID.randomUUID() + "." + extension;
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + storedName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.setContentType(contentTypeFor(extension));

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file: " + e.getMessage(), e);
        }

        HttpEntity<byte[]> request = new HttpEntity<>(bytes, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Supabase Storage rejected the upload: " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to Supabase Storage: " + e.getMessage(), e);
        }

        // Return the public URL for the stored file
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + storedName;
    }

    private MediaType contentTypeFor(String extension) {
        return switch (extension.toLowerCase()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "doc" -> MediaType.parseMediaType("application/msword");
            case "docx" -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
