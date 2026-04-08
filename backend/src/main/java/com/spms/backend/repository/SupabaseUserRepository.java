package com.spms.backend.repository;

import com.spms.backend.model.User;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class SupabaseUserRepository implements UserRepository {

    /*
     * TEMPORARY STUB:
     * This class keeps the Supabase-oriented repository boundary required by the project,
     * but uses in-memory storage until the real Supabase schema and connectivity are ready.
     *
     * TODO:
     * Replace the map-based storage with real Supabase CRUD operations once the table design
     * and credentials are finalized by the team.
     */
    private final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, Long> studentIdIndex = new ConcurrentHashMap<>();
    private final Map<String, Long> githubUsernameIndex = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1L);

    @Override
    public Optional<User> findByStudentId(String studentId) {
        String normalizedStudentId = normalizeStudentId(studentId);
        if (normalizedStudentId == null) {
            return Optional.empty();
        }

        Long userId = studentIdIndex.get(normalizedStudentId);
        if (userId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(usersById.get(userId)).map(User::new);
    }

    @Override
    public Optional<User> findByGithubUsername(String githubUsername) {
        String normalizedGithubUsername = normalizeGithubUsername(githubUsername);
        if (normalizedGithubUsername == null) {
            return Optional.empty();
        }

        Long userId = githubUsernameIndex.get(normalizedGithubUsername);
        if (userId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(usersById.get(userId)).map(User::new);
    }

    @Override
    public synchronized User save(User user) {
        Objects.requireNonNull(user, "user must not be null");

        User userToStore = new User(user);
        if (userToStore.getUserId() == null) {
            userToStore.setUserId(idSequence.getAndIncrement());
            if (userToStore.getCreatedAt() == null) {
                userToStore.setCreatedAt(Instant.now());
            }
        } else {
            User existingUser = usersById.get(userToStore.getUserId());
            if (existingUser != null) {
                removeIndex(studentIdIndex, existingUser.getStudentId());
                removeIndex(githubUsernameIndex, existingUser.getGithubUsername());
                if (userToStore.getCreatedAt() == null) {
                    userToStore.setCreatedAt(existingUser.getCreatedAt());
                }
            } else if (userToStore.getCreatedAt() == null) {
                userToStore.setCreatedAt(Instant.now());
            }
        }

        usersById.put(userToStore.getUserId(), new User(userToStore));
        putIndex(studentIdIndex, userToStore.getStudentId(), userToStore.getUserId());
        putIndex(githubUsernameIndex, userToStore.getGithubUsername(), userToStore.getUserId());
        return new User(userToStore);
    }

    private void putIndex(Map<String, Long> index, String rawValue, Long userId) {
        String normalizedValue = index == githubUsernameIndex
                ? normalizeGithubUsername(rawValue)
                : normalizeStudentId(rawValue);

        if (normalizedValue != null) {
            index.put(normalizedValue, userId);
        }
    }

    private void removeIndex(Map<String, Long> index, String rawValue) {
        String normalizedValue = index == githubUsernameIndex
                ? normalizeGithubUsername(rawValue)
                : normalizeStudentId(rawValue);

        if (normalizedValue != null) {
            index.remove(normalizedValue);
        }
    }

    private String normalizeStudentId(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            return null;
        }
        return studentId.trim();
    }

    private String normalizeGithubUsername(String githubUsername) {
        if (!StringUtils.hasText(githubUsername)) {
            return null;
        }
        return githubUsername.trim().toLowerCase();
    }
}
