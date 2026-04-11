package com.spms.backend.repository;

import com.spms.backend.model.User;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * TEST İÇİN IN-MEMORY REPOSITORY
 *
 * Bu sınıf eski SupabaseUserRepository'nin ConcurrentHashMap mantığını korur.
 * Sadece test'lerde kullanılır — üretim ortamında SupabaseUserRepository kullanılır.
 *
 * Neden var?
 * - Unit testler Supabase'e bağlanmamalı (internet bağımlılığı olmamalı)
 * - Testler hızlı çalışmalı (HTTP isteği beklemeden)
 * - Her test öncesi temiz bir veritabanı ile başlamalı
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, Long> emailIndex = new ConcurrentHashMap<>();
    private final Map<String, Long> studentIdIndex = new ConcurrentHashMap<>();
    private final Map<String, Long> githubUsernameIndex = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1L);

    @Override
    public Optional<User> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return Optional.empty();
        }

        Long userId = emailIndex.get(normalizedEmail);
        if (userId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(usersById.get(userId)).map(User::new);
    }

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
    public Optional<User> findByUserId(Long userId) {
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
                removeEmailIndex(existingUser.getEmail());
                removeStudentIdIndex(existingUser.getStudentId());
                removeGithubUsernameIndex(existingUser.getGithubUsername());
                if (userToStore.getCreatedAt() == null) {
                    userToStore.setCreatedAt(existingUser.getCreatedAt());
                }
            } else if (userToStore.getCreatedAt() == null) {
                userToStore.setCreatedAt(Instant.now());
            }
        }

        usersById.put(userToStore.getUserId(), new User(userToStore));
        putEmailIndex(userToStore.getEmail(), userToStore.getUserId());
        putStudentIdIndex(userToStore.getStudentId(), userToStore.getUserId());
        putGithubUsernameIndex(userToStore.getGithubUsername(), userToStore.getUserId());
        return new User(userToStore);
    }

    @Override
    public List<User> findAll() {
        return usersById.values().stream()
                .map(User::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findAllByRole(String role) {
        return usersById.values().stream()
                .filter(u -> role.equalsIgnoreCase(u.getRole()))
                .map(User::new)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized boolean deleteByUserId(Long userId) {
        User removed = usersById.remove(userId);
        if (removed == null) {
            return false;
        }
        removeEmailIndex(removed.getEmail());
        removeStudentIdIndex(removed.getStudentId());
        removeGithubUsernameIndex(removed.getGithubUsername());
        return true;
    }

    private void putEmailIndex(String rawValue, Long userId) {
        String normalizedValue = normalizeEmail(rawValue);
        if (normalizedValue != null) {
            emailIndex.put(normalizedValue, userId);
        }
    }

    private void putStudentIdIndex(String rawValue, Long userId) {
        String normalizedValue = normalizeStudentId(rawValue);
        if (normalizedValue != null) {
            studentIdIndex.put(normalizedValue, userId);
        }
    }

    private void putGithubUsernameIndex(String rawValue, Long userId) {
        String normalizedValue = normalizeGithubUsername(rawValue);
        if (normalizedValue != null) {
            githubUsernameIndex.put(normalizedValue, userId);
        }
    }

    private void removeEmailIndex(String rawValue) {
        String normalizedValue = normalizeEmail(rawValue);
        if (normalizedValue != null) {
            emailIndex.remove(normalizedValue);
        }
    }

    private void removeStudentIdIndex(String rawValue) {
        String normalizedValue = normalizeStudentId(rawValue);
        if (normalizedValue != null) {
            studentIdIndex.remove(normalizedValue);
        }
    }

    private void removeGithubUsernameIndex(String rawValue) {
        String normalizedValue = normalizeGithubUsername(rawValue);
        if (normalizedValue != null) {
            githubUsernameIndex.remove(normalizedValue);
        }
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
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
        return githubUsername.trim().toLowerCase(Locale.ROOT);
    }
}
