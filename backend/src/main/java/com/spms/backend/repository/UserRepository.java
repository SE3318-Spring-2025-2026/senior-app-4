package com.spms.backend.repository;

import com.spms.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(Long userId);

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByGithubUsername(String githubUsername);

    List<User> findAllByRole(String role);

    default boolean deleteByUserId(Long userId) {
        if (userId == null || findByUserId(userId).isEmpty()) return false;
        deleteById(userId);
        return true;
    }
}
