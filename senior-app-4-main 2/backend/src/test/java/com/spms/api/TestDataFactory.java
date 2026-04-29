package com.spms.api;

import com.spms.backend.model.User;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.repository.ValidStudentIdRepository;
import com.spms.backend.service.TokenService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for generating test data and building request payloads.
 *
 * <p>
 *     This class works directly with Spring bean references.
 *     Instead of calling API endpoints, it writes directly to the database
 *     via {@code UserRepository} and {@code ValidStudentIdRepository}.
 *     JWT tokens are minted via {@code TokenService.generateToken(User)}.
 * </p>
 *
 * <p>An atomic counter is used to generate unique values per test run,
 * preventing collisions with existing records in the database.</p>
 */
public final class TestDataFactory {

    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis());

    private TestDataFactory() {
        // Utility class — prevent instantiation
    }

    // ─── Unique Value Generators ───────────────────────────────────────

    /**
     * Generates a unique 11-digit student ID.
     * The "99" prefix makes test data easily identifiable for cleanup.
     */
    public static String uniqueStudentId() {
        long seq = COUNTER.incrementAndGet();
        return String.format("99%09d", seq % 1_000_000_000);
    }

    /**
     * Generates a unique email address.
     */
    public static String uniqueEmail() {
        return "testuser" + COUNTER.incrementAndGet() + "@spms-test.com";
    }

    /**
     * Generates a unique GitHub username.
     */
    public static String uniqueGithubUsername() {
        return "gh-test-" + COUNTER.incrementAndGet();
    }

    // ─── Database Seed Helpers (require Spring Bean references) ────────

    /**
     * Inserts a record directly into the valid_student_ids table.
     * Skips insertion if the student ID already exists.
     */
    public static void seedStudentId(ValidStudentIdRepository repo, String studentId) {
        if (!repo.existsByStudentId(studentId)) {
            repo.saveStudentId(studentId);
        }
    }

    /**
     * Creates a student user record directly in the users table.
     * Returns the existing user if one already exists with the given studentId.
     *
     * @return Persisted User entity (used with TokenService.generateToken)
     */
    public static User createStudent(UserRepository repo, String fullName,
                                     String studentId, String githubUsername) {
        return repo.findByStudentId(studentId)
                .orElseGet(() -> {
                    User user = new User();
                    user.setFullName(fullName);
                    user.setEmail(studentId + "@spms-test.com");
                    user.setStudentId(studentId);
                    user.setGithubUsername(githubUsername);
                    user.setRole("student");
                    user.setCreatedAt(Instant.now());
                    return repo.save(user);
                });
    }

    /**
     * Creates a professor user record directly in the users table.
     * Returns the existing user if one already exists with the given email.
     *
     * @return Persisted User entity (used with TokenService.generateToken)
     */
    public static User createProfessor(UserRepository repo, String fullName, String email) {
        return repo.findByEmail(email)
                .orElseGet(() -> {
                    User user = new User();
                    user.setFullName(fullName);
                    user.setEmail(email);
                    user.setRole("professor");
                    // Using a dummy hash since tests bypass login
                    user.setPasswordHash("pbkdf2$65536$dummySalt$dummyHash");
                    user.setRequiresPasswordChange(false);
                    user.setCreatedAt(Instant.now());
                    return repo.save(user);
                });
    }

    /**
     * Mints a JWT token from a User entity.
     * Uses the real TokenService.generateToken() mechanism from the backend,
     * signing with the same secret.
     *
     * @return A valid JWT token accepted by JwtAuthFilter
     */
    public static String mintToken(TokenService tokenService, User user) {
        return tokenService.generateToken(user);
    }

    // ─── JSON Request Payload Builders ─────────────────────────────────

    /**
     * Builds a payload for creating a group.
     */
    public static Map<String, Object> groupCreatePayload(String groupName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("groupName", groupName);
        return payload;
    }

    /**
     * Builds a payload for updating a group.
     */
    public static Map<String, Object> groupUpdatePayload(String groupName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("groupName", groupName);
        return payload;
    }

    /**
     * Builds a payload for adding a member to a group.
     */
    public static Map<String, Object> memberAddPayload(String studentId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("studentId", studentId);
        return payload;
    }

    /**
     * Builds a payload for student ID validation.
     */
    public static Map<String, Object> studentValidationPayload(String studentId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("studentId", studentId);
        return payload;
    }

    /**
     * Builds a payload for adding a single student ID.
     */
    public static Map<String, Object> studentIdCreatePayload(String studentId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("studentId", studentId);
        return payload;
    }

    /**
     * Builds a login payload (professor/coordinator only).
     */
    public static Map<String, Object> loginPayload(String email, String password) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        return payload;
    }

    /**
     * Builds a professor registration payload.
     */
    public static Map<String, Object> professorRegisterPayload(String fullName,
                                                                String email,
                                                                String password,
                                                                String role) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", fullName);
        payload.put("email", email);
        payload.put("password", password);
        payload.put("role", role);
        return payload;
    }
}
