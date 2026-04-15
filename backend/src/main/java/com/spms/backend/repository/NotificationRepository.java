package com.spms.backend.repository;

import com.spms.backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByToUserIdAndTypeOrderByCreatedAtDesc(Long toUserId, String type);

    boolean existsByToUserIdAndTypeAndStatus(Long toUserId, String type, String status);

    List<Notification> findByToUserIdOrderByCreatedAtDesc(Long toUserId);
}
