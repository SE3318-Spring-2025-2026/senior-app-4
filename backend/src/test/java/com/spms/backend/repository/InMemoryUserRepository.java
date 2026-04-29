package com.spms.backend.repository;

import com.spms.backend.model.User;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

/**
 * TEST İÇİN IN-MEMORY REPOSITORY
 */
public class InMemoryUserRepository extends AbstractStubJpaRepository<User, Long> implements UserRepository {

    private final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, Long> emailIndex = new ConcurrentHashMap<>();
    private final Map<String, Long> studentIdIndex = new ConcurrentHashMap<>();
    private final Map<String, Long> githubUsernameIndex = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1L);

    @Override
    public Optional<User> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) return Optional.empty();
        Long userId = emailIndex.get(normalizedEmail);
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(usersById.get(userId)).map(User::new);
    }

    @Override
    public Optional<User> findByStudentId(String studentId) {
        String normalized = normalizeStudentId(studentId);
        if (normalized == null) return Optional.empty();
        Long userId = studentIdIndex.get(normalized);
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(usersById.get(userId)).map(User::new);
    }

    @Override
    public Optional<User> findByUserId(Long userId) {
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(usersById.get(userId)).map(User::new);
    }

    @Override
    public Optional<User> findById(Long id) {
        return findByUserId(id);
    }

    @Override
    public Optional<User> findByGithubUsername(String githubUsername) {
        String normalized = normalizeGithubUsername(githubUsername);
        if (normalized == null) return Optional.empty();
        Long userId = githubUsernameIndex.get(normalized);
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(usersById.get(userId)).map(User::new);
    }

    @Override
    public synchronized User save(User user) {
        Objects.requireNonNull(user, "user must not be null");
        User userToStore = new User(user);
        if (userToStore.getUserId() == null) {
            userToStore.setUserId(idSequence.getAndIncrement());
            if (userToStore.getCreatedAt() == null) userToStore.setCreatedAt(Instant.now());
        } else {
            User existing = usersById.get(userToStore.getUserId());
            if (existing != null) {
                removeEmailIndex(existing.getEmail());
                removeStudentIdIndex(existing.getStudentId());
                removeGithubUsernameIndex(existing.getGithubUsername());
                if (userToStore.getCreatedAt() == null) userToStore.setCreatedAt(existing.getCreatedAt());
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
        return usersById.values().stream().map(User::new).toList();
    }

    @Override
    public List<User> findAllByRoleIgnoreCase(String role) {
        return usersById.values().stream()
                .filter(u -> role.equalsIgnoreCase(u.getRole()))
                .map(User::new)
                .toList();
    }

    @Override
    public synchronized boolean deleteByUserId(Long userId) {
        User removed = usersById.remove(userId);
        if (removed == null) return false;
        removeEmailIndex(removed.getEmail());
        removeStudentIdIndex(removed.getStudentId());
        removeGithubUsernameIndex(removed.getGithubUsername());
        return true;
    }

    @Override
    public void deleteById(Long id) { deleteByUserId(id); }

    @Override
    public void delete(User user) { if (user != null) deleteByUserId(user.getUserId()); }

    @Override
    public void deleteAll() { usersById.clear(); emailIndex.clear(); studentIdIndex.clear(); githubUsernameIndex.clear(); }

    @Override
    public long count() { return usersById.size(); }

    private void putEmailIndex(String v, Long id) { String n = normalizeEmail(v); if (n != null) emailIndex.put(n, id); }
    private void putStudentIdIndex(String v, Long id) { String n = normalizeStudentId(v); if (n != null) studentIdIndex.put(n, id); }
    private void putGithubUsernameIndex(String v, Long id) { String n = normalizeGithubUsername(v); if (n != null) githubUsernameIndex.put(n, id); }
    private void removeEmailIndex(String v) { String n = normalizeEmail(v); if (n != null) emailIndex.remove(n); }
    private void removeStudentIdIndex(String v) { String n = normalizeStudentId(v); if (n != null) studentIdIndex.remove(n); }
    private void removeGithubUsernameIndex(String v) { String n = normalizeGithubUsername(v); if (n != null) githubUsernameIndex.remove(n); }

    private String normalizeEmail(String v) { return StringUtils.hasText(v) ? v.trim().toLowerCase(Locale.ROOT) : null; }
    private String normalizeStudentId(String v) { return StringUtils.hasText(v) ? v.trim() : null; }
    private String normalizeGithubUsername(String v) { return StringUtils.hasText(v) ? v.trim().toLowerCase(Locale.ROOT) : null; }
}
