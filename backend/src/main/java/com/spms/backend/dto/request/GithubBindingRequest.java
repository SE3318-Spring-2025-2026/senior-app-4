package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GithubBindingRequest(
        @NotBlank(message = "GitHub PAT cannot be empty") 
        String githubPat,
        
        @NotBlank(message = "Organization name cannot be empty") 
        String organizationName
) {}