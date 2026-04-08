package com.spms.backend.service;

import com.spms.backend.dto.request.ProfessorRegisterRequest;
import com.spms.backend.dto.response.ProfessorRegisterResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.DuplicateUserException;
import com.spms.backend.model.User;
import com.spms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@Service
public class ProfessorRegistrationService {

    private static final String PROFESSOR_ROLE = "professor";
    private static final String COORDINATOR_ROLE = "coordinator";
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int HASH_ITERATIONS = 65_536;
    private static final int HASH_KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;

    public ProfessorRegistrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ProfessorRegisterResponse registerProfessor(ProfessorRegisterRequest request) {
        String fullName = requireText(request.fullName(), "fullName is required.");
        String email = normalizeEmail(request.email());
        String password = requirePassword(request.password());
        String role = validateRole(request.role());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateUserException("A user already exists for the given email.");
        }

        User savedUser = userRepository.save(buildProfessorUser(fullName, email, password, role));
        return new ProfessorRegisterResponse(
                "Professor registered successfully.",
                savedUser.getUserId(),
                savedUser.getRole()
        );
    }

    private User buildProfessorUser(String fullName, String email, String password, String role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(hashPassword(password));
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        return user;
    }

    boolean matchesPassword(String rawPassword, String passwordHash) {
        String[] parts = passwordHash.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }

        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
        byte[] candidateHash = hash(rawPassword, salt, iterations);
        return MessageDigest.isEqual(expectedHash, candidateHash);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String requirePassword(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("password is required.");
        }
        return value;
    }

    private String normalizeEmail(String value) {
        return requireText(value, "email is required.").toLowerCase(Locale.ROOT);
    }

    private String validateRole(String value) {
        String role = requireText(value, "role is required.");
        if (!PROFESSOR_ROLE.equals(role) && !COORDINATOR_ROLE.equals(role)) {
            throw new BadRequestException("role must be either professor or coordinator.");
        }
        return role;
    }

    private String hashPassword(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = hash(password, salt, HASH_ITERATIONS);
        return "pbkdf2$" + HASH_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private byte[] hash(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_KEY_LENGTH);
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            return secretKeyFactory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to hash password.", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
