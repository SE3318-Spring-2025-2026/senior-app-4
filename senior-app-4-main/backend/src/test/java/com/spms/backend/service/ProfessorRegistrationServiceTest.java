package com.spms.backend.service;

import com.spms.backend.dto.request.ProfessorRegisterRequest;
import com.spms.backend.dto.response.ProfessorRegisterResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.DuplicateUserException;
import com.spms.backend.model.User;
import com.spms.backend.repository.InMemoryUserRepository;
import com.spms.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessorRegistrationServiceTest {

    private UserRepository userRepository;
    private ProfessorRegistrationService professorRegistrationService;
    private PasswordHashingService passwordHashingService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        passwordHashingService = new PasswordHashingService();
        professorRegistrationService = new ProfessorRegistrationService(
                userRepository, passwordHashingService);
    }

    @Test
    void registerProfessorCreatesNewCoordinatorUserWithHashedPassword() {
        ProfessorRegisterResponse response = professorRegistrationService.registerProfessor(
                new ProfessorRegisterRequest(
                        "Dr. Jane Doe",
                        "jane.doe@university.edu",
                        "SecurePass123!",
                        "coordinator"
                )
        );

        User savedUser = userRepository.findByEmail("JANE.DOE@UNIVERSITY.EDU").orElseThrow();

        assertNotNull(response.userId());
        assertEquals("coordinator", response.role());
        assertNotNull(savedUser.getPasswordHash());
        assertNotEquals("SecurePass123!", savedUser.getPasswordHash());
        assertTrue(passwordHashingService.matchesPassword("SecurePass123!", savedUser.getPasswordHash()));
    }

    @Test
    void registerProfessorRejectsInvalidRole() {
        assertThrows(
                BadRequestException.class,
                () -> professorRegistrationService.registerProfessor(
                        new ProfessorRegisterRequest(
                                "Dr. Jane Doe",
                                "jane.doe@university.edu",
                                "SecurePass123!",
                                "advisor"
                        )
                )
        );
    }

    @Test
    void registerProfessorRejectsDuplicateEmailCaseInsensitively() {
        professorRegistrationService.registerProfessor(
                new ProfessorRegisterRequest(
                        "Dr. Jane Doe",
                        "jane.doe@university.edu",
                        "SecurePass123!",
                        "professor"
                )
        );

        assertThrows(
                DuplicateUserException.class,
                () -> professorRegistrationService.registerProfessor(
                        new ProfessorRegisterRequest(
                                "Dr. Jane Doe",
                                "JANE.DOE@UNIVERSITY.EDU",
                                "AnotherSecurePass123!",
                                "coordinator"
                        )
                )
        );
    }
}
