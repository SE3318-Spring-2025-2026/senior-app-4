package com.spms.backend.service;

import com.spms.backend.model.Notification;
import com.spms.backend.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Koordinatöre sistem uyarısı bildirim kaydı oluşturur (D10).
     * alertType örn: "formation_deadline_missed", "advisor_deadline_missed"
     */
    public Notification createSystemAlert(Long toUserId,
                                          String message,
                                          String alertType,
                                          String metadata) {
        Notification n = new Notification();
        n.setType("system_alert");
        n.setMessage(message);
        n.setStatus("pending");
        n.setToUserId(toUserId);
        n.setMetadata(metadata);
        n.setCreatedAt(Instant.now());
        return notificationRepository.save(n);
    }

    /**
     * Belirli bir kullanıcıya ait tüm sistem uyarılarını getirir.
     */
    public List<Notification> getSystemAlerts(Long userId) {
        return notificationRepository
                .findByToUserIdAndTypeOrderByCreatedAtDesc(userId, "system_alert");
    }

    /**
     * Belirli bir bildirim zaten var mı kontrol et (idempotency).
     */
    public boolean systemAlertExists(Long toUserId, String alertType) {
        return notificationRepository
                .existsByToUserIdAndTypeAndStatus(toUserId, alertType, "pending");
    }
}
