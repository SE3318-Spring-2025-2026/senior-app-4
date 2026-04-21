package com.spms.backend.dto.response;

public record MemberResponseDto(
    Long userId,
    String studentId,
    String fullName,
    String role
) {}