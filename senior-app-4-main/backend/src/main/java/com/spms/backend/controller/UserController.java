package com.spms.backend.controller;

import com.spms.backend.dto.request.StudentUserCreateRequest;
import com.spms.backend.dto.request.UserCreateDirectRequest;
import com.spms.backend.dto.request.UserUpdateRequest;
import com.spms.backend.dto.response.DeleteResponse;
import com.spms.backend.dto.response.UserCreateResponse;
import com.spms.backend.dto.response.UserListResponse;
import com.spms.backend.dto.response.UserResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.User;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.StudentRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final StudentRegistrationService studentRegistrationService;
    private final UserRepository userRepository;

    public UserController(StudentRegistrationService studentRegistrationService,
                          UserRepository userRepository) {
        this.studentRegistrationService = studentRegistrationService;
        this.userRepository = userRepository;
    }

    // ── POST /api/v1/users/register/student ──
    @PostMapping("/register/student")
    public ResponseEntity<UserCreateResponse> registerStudent(
            @Valid @RequestBody StudentUserCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentRegistrationService.registerStudent(request));
    }

    // ── GET /api/v1/users?role=student ──
    @GetMapping
    public ResponseEntity<UserListResponse> getAllUsers(
            @RequestParam(required = false) String role
    ) {
        List<User> users;
        if (StringUtils.hasText(role)) {
            users = userRepository.findAllByRole(role);
        } else {
            users = userRepository.findAll();
        }

        List<UserResponse.UserData> data = users.stream()
                .map(u -> new UserResponse.UserData(
                        u.getUserId(), u.getFullName(), u.getEmail(),
                        u.getStudentId(), u.getGithubUsername(), u.getRole(),
                        u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new UserListResponse(
                "Users retrieved successfully.", data.size(), data
        ));
    }

    // ── POST /api/v1/users ──
    @PostMapping
    public ResponseEntity<UserCreateResponse> createUser(
            @Valid @RequestBody UserCreateDirectRequest request
    ) {
        // Rol kontrolü (Gelen değeri lowercase yapıyoruz)
        String role = request.role() != null ? request.role().toLowerCase(java.util.Locale.ROOT) : "";
        if (!"student".equals(role) && !"professor".equals(role) && !"coordinator".equals(role)) {
            throw new BadRequestException("role must be student, professor, or coordinator.");
        }

        // Email duplicate kontrolü
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new com.spms.backend.exception.DuplicateUserException(
                    "A user already exists for the given email.");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setStudentId(request.studentId());
        user.setGithubUsername(request.githubUsername());
        user.setRole(request.role());
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new UserCreateResponse("User created successfully.",
                        saved.getUserId(), saved.getStudentId(),
                        saved.getGithubUsername(), saved.getRole())
        );
    }

    // ── GET /api/v1/users/{userId} ──
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("User not found."));
        return ResponseEntity.ok(UserResponse.from("User retrieved successfully.", user));
    }

    // ── PUT /api/v1/users/{userId} ──
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @RequestBody UserUpdateRequest request
    ) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("User not found."));

        if (StringUtils.hasText(request.fullName())) user.setFullName(request.fullName());
        if (StringUtils.hasText(request.email())) user.setEmail(request.email());
        if (StringUtils.hasText(request.githubUsername())) user.setGithubUsername(request.githubUsername());
        if (StringUtils.hasText(request.role())) user.setRole(request.role());

        User updated = userRepository.save(user);
        return ResponseEntity.ok(UserResponse.from("User updated successfully.", updated));
    }

    // ── DELETE /api/v1/users/{userId} ──
    @DeleteMapping("/{userId}")
    public ResponseEntity<DeleteResponse> deleteUser(@PathVariable Long userId) {
        boolean deleted = userRepository.deleteByUserId(userId);
        if (!deleted) {
            throw new BadRequestException("User not found.");
        }
        return ResponseEntity.ok(new DeleteResponse("Resource deleted successfully."));
    }
}
