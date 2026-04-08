package com.spms.backend.service;

import com.spms.backend.dto.internal.StudentRegistrationData;
import com.spms.backend.dto.request.StudentUserCreateRequest;
import com.spms.backend.dto.response.UserCreateResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.DuplicateUserException;
import com.spms.backend.model.User;
import com.spms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class StudentRegistrationService {

    private static final String STUDENT_ROLE = "student";

    private final UserRepository userRepository;

    public StudentRegistrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserCreateResponse registerStudent(StudentUserCreateRequest request) {
        String studentId = requireText(request.studentId(), "studentId is required.");
        String githubUsername = requireText(request.githubUsername(), "githubUsername is required.");

        // accessToken is accepted to keep the current API contract, but it is not persisted in Issue #7.
        requireText(request.accessToken(), "accessToken is required.");

        if (userRepository.findByStudentId(studentId).isPresent()) {
            throw new DuplicateUserException("A student user already exists for the given studentId.");
        }

        if (userRepository.findByGithubUsername(githubUsername).isPresent()) {
            throw new DuplicateUserException("A student user already exists for the given githubUsername.");
        }

        User savedUser = userRepository.save(buildStudentUser(studentId, githubUsername));
        return toCreateResponse(savedUser);
    }

    public User findOrCreateFromCallback(StudentRegistrationData registrationData) {
        String studentId = requireText(registrationData.studentId(), "Validated studentId is required in state.");
        String githubUsername = requireText(registrationData.githubUsername(), "githubUsername is required.");

        Optional<User> existingByStudentId = userRepository.findByStudentId(studentId);
        if (existingByStudentId.isPresent()) {
            return reconcileExistingStudent(existingByStudentId.get(), githubUsername);
        }

        Optional<User> existingByGithubUsername = userRepository.findByGithubUsername(githubUsername);
        if (existingByGithubUsername.isPresent()) {
            throw new BadRequestException("The fetched GitHub username is already linked to another student.");
        }

        return userRepository.save(buildStudentUser(studentId, githubUsername));
    }

    private User reconcileExistingStudent(User existingUser, String githubUsername) {
        if (!StringUtils.hasText(existingUser.getGithubUsername())) {
            Optional<User> conflictingGithubUser = userRepository.findByGithubUsername(githubUsername);
            if (conflictingGithubUser.isPresent()
                    && !Objects.equals(conflictingGithubUser.get().getUserId(), existingUser.getUserId())) {
                throw new BadRequestException("The fetched GitHub username is already linked to another student.");
            }

            existingUser.setGithubUsername(githubUsername);
            return userRepository.save(existingUser);
        }

        if (existingUser.getGithubUsername().trim().equalsIgnoreCase(githubUsername)) {
            return existingUser;
        }

        throw new BadRequestException("The validated studentId is already linked to a different GitHub username.");
    }

    private User buildStudentUser(String studentId, String githubUsername) {
        User user = new User();
        user.setStudentId(studentId);
        user.setGithubUsername(githubUsername);
        user.setRole(STUDENT_ROLE);
        user.setCreatedAt(Instant.now());
        return user;
    }

    private UserCreateResponse toCreateResponse(User savedUser) {
        return new UserCreateResponse(
                "User created successfully.",
                savedUser.getUserId(),
                savedUser.getStudentId(),
                savedUser.getGithubUsername(),
                savedUser.getRole()
        );
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }
}
