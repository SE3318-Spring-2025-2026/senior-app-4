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
import java.util.Locale;

@Service
public class ProfessorRegistrationService {

    private static final String PROFESSOR_ROLE = "professor";
    private static final String COORDINATOR_ROLE = "coordinator";

    private final UserRepository userRepository;
    private final PasswordHashingService passwordHashingService;

    public ProfessorRegistrationService(UserRepository userRepository,
                                        PasswordHashingService passwordHashingService) {
        this.userRepository = userRepository;
        this.passwordHashingService = passwordHashingService;
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
        // Artık PasswordHashingService kullanıyor (eskiden private hashPassword vardı)
        user.setPasswordHash(passwordHashingService.hashPassword(password));
        user.setRole(role);
        // Profesör geçici şifre ile kaydedilir, ilk girişte değiştirmesi gerekir
        user.setRequiresPasswordChange(true);
        user.setCreatedAt(Instant.now());
        return user;
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
        String role = requireText(value, "role is required.").toLowerCase(Locale.ROOT);
        if (!PROFESSOR_ROLE.equals(role) && !COORDINATOR_ROLE.equals(role)) {
            throw new BadRequestException("role must be either professor or coordinator.");
        }
        return role;
    }
}

