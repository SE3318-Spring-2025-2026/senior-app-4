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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "User Store")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final String USER_NOT_FOUND = "User not found.";

    private final StudentRegistrationService studentRegistrationService;
    private final UserRepository userRepository;

    public UserController(StudentRegistrationService studentRegistrationService,
                          UserRepository userRepository) {
        this.studentRegistrationService = studentRegistrationService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get the currently authenticated user's profile")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("jwt_userId");
        if (userIdAttr == null) {
            throw new BadRequestException("Could not determine user from token.");
        }
        Long userId = ((Number) userIdAttr).longValue();
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(USER_NOT_FOUND));
        return ResponseEntity.ok(UserResponse.from("Profile retrieved successfully.", user));
    }

    @Operation(summary = "Register a student user after GitHub OAuth")
    @PostMapping("/register/student")
    public ResponseEntity<UserCreateResponse> registerStudent(
            @Valid @RequestBody StudentUserCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentRegistrationService.registerStudent(request));
    }

    @Operation(summary = "List all users, optionally filtered by role")
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

    @Operation(summary = "Create a user directly (admin use)")
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

    @Operation(summary = "Get a user by ID")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(USER_NOT_FOUND));
        return ResponseEntity.ok(UserResponse.from("User retrieved successfully.", user));
    }

    @Operation(summary = "Update a user by ID")
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @RequestBody UserUpdateRequest request
    ) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(USER_NOT_FOUND));

        if (StringUtils.hasText(request.fullName())) user.setFullName(request.fullName());
        if (StringUtils.hasText(request.email())) user.setEmail(request.email());
        if (StringUtils.hasText(request.githubUsername())) user.setGithubUsername(request.githubUsername());
        if (StringUtils.hasText(request.role())) user.setRole(request.role());

        User updated = userRepository.save(user);
        return ResponseEntity.ok(UserResponse.from("User updated successfully.", updated));
    }

    @Operation(summary = "Delete a user by ID")
    @DeleteMapping("/{userId}")
    public ResponseEntity<DeleteResponse> deleteUser(@PathVariable Long userId) {
        boolean deleted = userRepository.deleteByUserId(userId);
        if (!deleted) {
            throw new BadRequestException(USER_NOT_FOUND);
        }
        return ResponseEntity.ok(new DeleteResponse("Resource deleted successfully."));
    }
}
