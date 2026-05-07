package com.spms.backend.repository;

import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.spms.backend.model.notification.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByToUser_UserId(Long toUserId, Pageable pageable);

    Page<Notification> findByToUser_UserIdAndStatusNot(Long toUserId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByToUser_UserIdAndReadStatus(Long toUserId, boolean readStatus, Pageable pageable);

    Page<Notification> findByToUser_UserIdAndReadStatusAndStatusNot(
            Long toUserId, boolean readStatus, NotificationStatus status, Pageable pageable);

    Optional<Notification> findByGroupIdAndTypeAndStatus(Long groupId, NotificationType type, NotificationStatus status);

    Optional<Notification> findByGroupIdAndToUser_UserIdAndTypeAndStatus(
            Long groupId, Long toUserId, NotificationType type, NotificationStatus status);

    void deleteByToUser_UserId(Long userId);

    List<Notification> findByToUser_UserIdAndTypeOrderByCreatedAtDesc(Long toUserId, NotificationType type);

    boolean existsByToUser_UserIdAndTypeAndStatusAndMessageContaining(
            Long toUserId, NotificationType type, NotificationStatus status, String messageSnippet);

    List<Notification> findByToUser_UserIdAndTypeAndStatus(Long toUserId, NotificationType type, NotificationStatus status);

    List<Notification> findByGroupIdAndTypeAndStatusAndToUser_UserIdNot(
            Long groupId, NotificationType type, NotificationStatus status, Long excludedUserId);

    List<Notification> findByToUser_UserIdAndTypeAndStatusAndGroupIdNot(
            Long toUserId, NotificationType type, NotificationStatus status, Long excludedGroupId);

    List<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type);

    List<Notification> findByGroupIdAndTypeOrderByCreatedAtDesc(Long groupId, NotificationType type);

    List<Notification> findByCommitteeIdOrderByCreatedAtDesc(Long committeeId);

    List<Notification> findByToUser_UserIdAndCommitteeIdIsNotNullOrderByCreatedAtDesc(Long userId);
}
