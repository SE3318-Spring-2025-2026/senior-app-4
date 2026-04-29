package com.spms.backend.controller;

import com.spms.backend.dto.request.ProfessorRegisterRequest;
import com.spms.backend.dto.response.ProfessorRegisterResponse;
import com.spms.backend.service.ProfessorRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Professor Management")
@RestController
@RequestMapping("/api/v1/professors")
public class ProfessorController {

    private final ProfessorRegistrationService professorRegistrationService;

    public ProfessorController(ProfessorRegistrationService professorRegistrationService) {
        this.professorRegistrationService = professorRegistrationService;
    }

    @Operation(summary = "Register a new professor or coordinator")
    @PostMapping("/register")
    public ResponseEntity<ProfessorRegisterResponse> registerProfessor(
            @Valid @RequestBody ProfessorRegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(professorRegistrationService.registerProfessor(request));
    }
}
