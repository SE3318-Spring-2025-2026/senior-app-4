package com.spms.backend.dto.response;

import java.util.List;

public record UserListResponse(
        String message,
        int count,
        List<UserResponse.UserData> data
) {}
