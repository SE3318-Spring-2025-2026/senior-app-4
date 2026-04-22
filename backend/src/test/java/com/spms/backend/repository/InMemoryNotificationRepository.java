package com.spms.backend.repository;

import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InMemoryNotificationRepository implements NotificationRepository {

    private final Map<Long, Notification> notifications = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public <S extends Notification> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        notifications.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Page<Notification> findByToUser_UserId(Long toUserId, Pageable pageable) {
        List<Notification> userNotifs = notifications.values().stream()
                .filter(n -> n.getToUser().getUserId().equals(toUserId))
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), userNotifs.size());
        if (start > userNotifs.size()) return new PageImpl<>(Collections.emptyList(), pageable, userNotifs.size());
        return new PageImpl<>(userNotifs.subList(start, end), pageable, userNotifs.size());
    }

    @Override
    public Optional<Notification> findByGroupIdAndTypeAndStatus(Long groupId, NotificationType type, NotificationStatus status) {
        return notifications.values().stream()
                .filter(n -> n.getGroupId() != null && n.getGroupId().equals(groupId) 
                        && n.getType() == type 
                        && n.getStatus() == status)
                .findFirst();
    }

    @Override
    public Optional<Notification> findByGroupIdAndToUser_UserIdAndTypeAndStatus(Long groupId, Long toUserId, NotificationType type, NotificationStatus status) {
        return notifications.values().stream()
                .filter(n -> n.getGroupId() != null && n.getGroupId().equals(groupId) 
                        && n.getToUser().getUserId().equals(toUserId) 
                        && n.getType() == type 
                        && n.getStatus() == status)
                .findFirst();
    }

    @Override
    public void deleteByToUser_UserId(Long userId) {
        notifications.values().removeIf(n -> n.getToUser().getUserId().equals(userId));
    }

    @Override
    public List<Notification> findByToUser_UserIdAndTypeOrderByCreatedAtDesc(Long toUserId, NotificationType type) {
        return notifications.values().stream()
                .filter(n -> n.getToUser().getUserId().equals(toUserId) && n.getType() == type)
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByToUser_UserIdAndTypeAndStatusAndMessageContaining(Long toUserId, NotificationType type, NotificationStatus status, String messageSnippet) {
        return notifications.values().stream()
                .anyMatch(n -> n.getToUser().getUserId().equals(toUserId) 
                        && n.getType() == type 
                        && n.getStatus() == status 
                        && n.getMessage() != null && n.getMessage().contains(messageSnippet));
    }

    @Override
    public List<Notification> findByToUser_UserIdAndTypeAndStatus(Long toUserId, NotificationType type, NotificationStatus status) {
        return notifications.values().stream()
                .filter(n -> n.getToUser().getUserId().equals(toUserId) && n.getType() == type && n.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findByGroupIdAndTypeAndStatusAndToUser_UserIdNot(Long groupId, NotificationType type, NotificationStatus status, Long excludedUserId) {
        return notifications.values().stream()
                .filter(n -> n.getGroupId() != null && n.getGroupId().equals(groupId) 
                        && n.getType() == type 
                        && n.getStatus() == status 
                        && !n.getToUser().getUserId().equals(excludedUserId))
                .collect(Collectors.toList());
    }

    // --- Unimplemented Boilerplate ---

    @Override public List<Notification> findAll() { return new ArrayList<>(notifications.values()); }
    @Override public List<Notification> findAll(Sort sort) { return findAll(); }
    @Override public Page<Notification> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public List<Notification> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public long count() { return notifications.size(); }
    @Override public void deleteById(Long id) { notifications.remove(id); }
    @Override public void delete(Notification entity) { deleteById(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(Iterable<? extends Notification> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { notifications.clear(); }
    @Override public boolean existsById(Long id) { return notifications.containsKey(id); }
    @Override public Optional<Notification> findById(Long id) { return Optional.ofNullable(notifications.get(id)); }
    @Override public <S extends Notification> List<S> saveAll(Iterable<S> entities) { entities.forEach(this::save); return (List<S>) entities; }
    @Override public void flush() {}
    @Override public <S extends Notification> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Notification> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<Notification> entities) { deleteAll(entities); }
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public void deleteAllInBatch() { deleteAll(); }
    @Override public Notification getOne(Long id) { return findById(id).orElse(null); }
    @Override public Notification getById(Long id) { return getOne(id); }
    @Override public Notification getReferenceById(Long id) { return getOne(id); }
    @Override public <S extends Notification> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Notification> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Notification> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
    @Override public <S extends Notification> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public <S extends Notification> long count(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Notification> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Notification, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
}
