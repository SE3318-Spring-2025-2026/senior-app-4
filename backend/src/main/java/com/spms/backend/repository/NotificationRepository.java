package com.spms.backend.repository;

import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.spms.backend.model.notification.Notification;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    Page<Notification> findByToUser_UserId(Long toUserId, Pageable pageable);

    Optional<Notification> findByGroupIdAndTypeAndStatus(Long groupId, NotificationType type, NotificationStatus status);

    Optional<Notification> findByGroupIdAndToUser_UserIdAndTypeAndStatus(
            Long groupId, Long toUserId, NotificationType type, NotificationStatus status);

    void deleteByToUser_UserId(Long userId);
    java.util.List<Notification> findByToUser_UserIdAndTypeOrderByCreatedAtDesc(Long toUserId, NotificationType type);
    boolean existsByToUser_UserIdAndTypeAndStatusAndMessageContaining(Long toUserId, NotificationType type, NotificationStatus status, String messageSnippet);
}
