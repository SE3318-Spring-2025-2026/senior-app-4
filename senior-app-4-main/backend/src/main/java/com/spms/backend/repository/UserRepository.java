package com.spms.backend.repository;

import com.spms.backend.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUserId(Long userId);

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByGithubUsername(String githubUsername);

    List<User> findAll();

    List<User> findAllByRole(String role);

    User save(User user);

    boolean deleteByUserId(Long userId);
}
