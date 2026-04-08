package com.spms.backend.service;

import com.spms.backend.dto.internal.StudentRegistrationData;
import com.spms.backend.dto.request.StudentUserCreateRequest;
import com.spms.backend.dto.response.UserCreateResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.DuplicateUserException;
import com.spms.backend.model.User;
import com.spms.backend.repository.SupabaseUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentRegistrationServiceTest {

    private SupabaseUserRepository userRepository;
    private StudentRegistrationService studentRegistrationService;

    @BeforeEach
    void setUp() {
        userRepository = new SupabaseUserRepository();
        studentRegistrationService = new StudentRegistrationService(userRepository);
    }

    @Test
    void registerStudentCreatesNewUser() {
        UserCreateResponse response = studentRegistrationService.registerStudent(
                new StudentUserCreateRequest("11070001000", "furkangncr", "gho_valid")
        );

        assertNotNull(response.userId());
        assertEquals("student", response.role());
        assertTrue(userRepository.findByStudentId("11070001000").isPresent());
    }

    @Test
    void registerStudentRejectsDuplicateStudentId() {
        studentRegistrationService.registerStudent(
                new StudentUserCreateRequest("11070001000", "furkangncr", "gho_valid")
        );

        assertThrows(
                DuplicateUserException.class,
                () -> studentRegistrationService.registerStudent(
                        new StudentUserCreateRequest("11070001000", "anotheruser", "gho_other")
                )
        );
    }

    @Test
    void registerStudentRejectsDuplicateGithubUsername() {
        studentRegistrationService.registerStudent(
                new StudentUserCreateRequest("11070001000", "furkangncr", "gho_valid")
        );

        assertThrows(
                DuplicateUserException.class,
                () -> studentRegistrationService.registerStudent(
                        new StudentUserCreateRequest("11070001001", "furkangncr", "gho_other")
                )
        );
    }

    @Test
    void findOrCreateFromCallbackCreatesNewUserWhenMissing() {
        User user = studentRegistrationService.findOrCreateFromCallback(
                new StudentRegistrationData("11070001000", "furkangncr", "gho_valid")
        );

        assertNotNull(user.getUserId());
        assertEquals("11070001000", user.getStudentId());
        assertEquals("furkangncr", user.getGithubUsername());
        assertEquals("student", user.getRole());
    }

    @Test
    void findOrCreateFromCallbackReusesExistingStudent() {
        User createdUser = studentRegistrationService.findOrCreateFromCallback(
                new StudentRegistrationData("11070001000", "furkangncr", "gho_valid")
        );

        User reusedUser = studentRegistrationService.findOrCreateFromCallback(
                new StudentRegistrationData("11070001000", "furkangncr", "gho_valid")
        );

        assertEquals(createdUser.getUserId(), reusedUser.getUserId());
    }

    @Test
    void findOrCreateFromCallbackRejectsGithubUsernameConflict() {
        studentRegistrationService.findOrCreateFromCallback(
                new StudentRegistrationData("11070001000", "furkangncr", "gho_valid")
        );

        assertThrows(
                BadRequestException.class,
                () -> studentRegistrationService.findOrCreateFromCallback(
                        new StudentRegistrationData("11070001001", "furkangncr", "gho_other")
                )
        );
    }
}
