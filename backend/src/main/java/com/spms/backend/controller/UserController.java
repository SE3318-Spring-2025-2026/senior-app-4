package com.spms.backend.controller;

import com.spms.backend.dto.request.StudentUserCreateRequest;
import com.spms.backend.dto.response.UserCreateResponse;
import com.spms.backend.service.StudentRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.spms.backend.dto.request.StudentValidationRequest;
import com.spms.backend.dto.response.StudentValidationResponse;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final StudentRegistrationService studentRegistrationService;

    public UserController(StudentRegistrationService studentRegistrationService) {
        this.studentRegistrationService = studentRegistrationService;
    }

    @PostMapping("/register/student")
    public ResponseEntity<UserCreateResponse> registerStudent(@Valid @RequestBody StudentUserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentRegistrationService.registerStudent(request));
    }

    @PostMapping("/students/validate")
public ResponseEntity<StudentValidationResponse> validateStudent(
        @RequestBody StudentValidationRequest request
) {
    boolean exists = studentRegistrationService.validateStudent(request.getStudentId());

    if (exists) {
        return ResponseEntity.ok(new StudentValidationResponse(true));
    }

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(new StudentValidationResponse(false));
}

}
