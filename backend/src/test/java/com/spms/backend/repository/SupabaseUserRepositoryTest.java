package com.spms.backend.repository;

import com.spms.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupabaseUserRepositoryTest {

    private SupabaseUserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new SupabaseUserRepository();
    }

    @Test
    void saveAssignsIdAndFindsUserByIndexes() {
        User user = new User();
        user.setStudentId("11070001000");
        user.setGithubUsername("furkangncr");
        user.setRole("student");
        user.setCreatedAt(Instant.now());

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getUserId());
        assertTrue(userRepository.findByStudentId("11070001000").isPresent());
        assertTrue(userRepository.findByGithubUsername("furkangncr").isPresent());
    }

    @Test
    void saveUpdatesGithubUsernameIndexForExistingUser() {
        User user = new User();
        user.setStudentId("11070001000");
        user.setGithubUsername("furkangncr");
        user.setRole("student");
        user.setCreatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        savedUser.setGithubUsername("furkangncr-updated");
        userRepository.save(savedUser);

        assertTrue(userRepository.findByGithubUsername("furkangncr-updated").isPresent());
        assertTrue(userRepository.findByGithubUsername("furkangncr").isEmpty());
        assertEquals("furkangncr-updated", userRepository.findByStudentId("11070001000").orElseThrow().getGithubUsername());
    }

    @Test
    void saveFindsUserByEmailCaseInsensitively() {
        User user = new User();
        user.setFullName("Dr. Jane Doe");
        user.setEmail("Jane.Doe@University.edu");
        user.setPasswordHash("hashed-password");
        user.setRole("professor");
        user.setCreatedAt(Instant.now());

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getUserId());
        assertTrue(userRepository.findByEmail("jane.doe@university.edu").isPresent());
        assertTrue(userRepository.findByEmail("JANE.DOE@UNIVERSITY.EDU").isPresent());
    }

    @Test
    void saveUpdatesEmailIndexForExistingUser() {
        User user = new User();
        user.setFullName("Dr. Jane Doe");
        user.setEmail("jane.doe@university.edu");
        user.setPasswordHash("hashed-password");
        user.setRole("coordinator");
        user.setCreatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        savedUser.setEmail("jane.updated@university.edu");
        userRepository.save(savedUser);

        assertTrue(userRepository.findByEmail("jane.updated@university.edu").isPresent());
        assertTrue(userRepository.findByEmail("jane.doe@university.edu").isEmpty());
        assertEquals(
                "jane.updated@university.edu",
                userRepository.findByEmail("jane.updated@university.edu").orElseThrow().getEmail()
        );
    }
}
