package com.spms.backend.controller;

import com.spms.backend.dto.request.StudentUserCreateRequest;
import com.spms.backend.dto.request.UserCreateDirectRequest;
import com.spms.backend.dto.request.UserUpdateRequest;
import com.spms.backend.dto.response.DeleteResponse;
import com.spms.backend.dto.response.UserCreateResponse;
import com.spms.backend.dto.response.UserListResponse;
import com.spms.backend.dto.response.UserResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.User;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.StudentRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag(name = "User Store")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final String USER_NOT_FOUND = "User not found.";

    private final StudentRegistrationService studentRegistrationService;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;

    public UserController(StudentRegistrationService studentRegistrationService,
                          UserRepository userRepository,
                          GroupMemberRepository groupMemberRepository,
                          GroupRepository groupRepository) {
        this.studentRegistrationService = studentRegistrationService;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupRepository = groupRepository;
    }

    @Operation(summary = "Get the currently authenticated user's profile")
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<UserResponse> getMe(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("jwt_userId");
        if (userIdAttr == null) {
            throw new BadRequestException("Could not determine user from token.");
        }
        Long userId = ((Number) userIdAttr).longValue();
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(USER_NOT_FOUND));

        Optional<GroupMember> membership = groupMemberRepository.findTopByUser_UserId(userId);
        Long groupId = membership.map(m -> m.getGroup().getId()).orElse(null);
        String groupName = membership.map(m -> m.getGroup().getGroupName()).orElse(null);
        log.info("[/me] userId={} membership={}", userId, groupId);

        if (groupId == null) {
            Optional<Group> ledGroup = groupRepository.findByLeader_UserId(userId);
            groupId = ledGroup.map(Group::getId).orElse(null);
            groupName = ledGroup.map(Group::getGroupName).orElse(null);
            log.info("[/me] userId={} leaderOf={}", userId, groupId);
        }

        UserResponse.UserData userData = new UserResponse.UserData(
                user.getUserId(), user.getFullName(), user.getEmail(),
                user.getStudentId(), user.getGithubUsername(), user.getRole(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                groupId, groupName
        );
        return ResponseEntity.ok(new UserResponse("Profile retrieved successfully.", userData));
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
    @Transactional(readOnly = true)
    public ResponseEntity<UserListResponse> getAllUsers(
            @RequestParam(required = false) String role
    ) {
        List<User> users;
        if (StringUtils.hasText(role)) {
            users = userRepository.findAllByRoleIgnoreCase(role);
        } else {
            users = userRepository.findAll();
        }

        Map<Long, GroupMember> membershipMap = groupMemberRepository.findAllWithUserAndGroup()
                .stream()
                .collect(Collectors.toMap(
                        gm -> gm.getUser().getUserId(),
                        gm -> gm,
                        (a, b) -> a
                ));

        List<UserResponse.UserData> data = users.stream()
                .map(u -> {
                    GroupMember membership = membershipMap.get(u.getUserId());
                    Long groupId = membership != null ? membership.getGroup().getId() : null;
                    String groupName = membership != null ? membership.getGroup().getGroupName() : null;
                    return new UserResponse.UserData(
                            u.getUserId(), u.getFullName(), u.getEmail(),
                            u.getStudentId(), u.getGithubUsername(), u.getRole(),
                            u.getCreatedAt() != null ? u.getCreatedAt().toString() : null,
                            groupId, groupName
                    );
                })
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
