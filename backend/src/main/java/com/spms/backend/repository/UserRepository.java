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

    @org.springframework.data.jpa.repository.Query(
        "SELECT new com.spms.backend.dto.response.CoordinatorStudentResponseDto(" +
        "u.userId, u.fullName, u.email, u.studentId, g.id, g.groupName) " +
        "FROM User u " +
        "LEFT JOIN GroupMember gm ON gm.user = u " +
        "LEFT JOIN gm.group g " +
        "WHERE u.role = 'student' AND (" +
        "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "LOWER(u.studentId) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    List<com.spms.backend.dto.response.CoordinatorStudentResponseDto> searchStudentsWithGroups(@org.springframework.data.repository.query.Param("search") String search);

    default boolean deleteByUserId(Long userId) {
        if (userId == null || findByUserId(userId).isEmpty()) return false;
        deleteById(userId);
        return true;
    }
}
