package com.spms.backend.dto.response;

public record CommitteePaginationInfo(
        int currentPage,
        int totalPages,
        long totalElements,
        int pageSize) {}
