package com.spms.backend.repository;

import com.spms.backend.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUserId(Long userId);

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByGithubUsername(String githubUsername);

    User save(User user);
}
