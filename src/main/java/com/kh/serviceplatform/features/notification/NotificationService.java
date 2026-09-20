package com.kh.serviceplatform.features.notification;

import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.notification.dto.NotificationResponse;
import com.kh.serviceplatform.features.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    void sendNotification(User recipient, NotificationType type, String title, String message, UUID referenceId, String referenceType);

    Page<NotificationResponse> getMyNotifications(UUID userId, Boolean unreadOnly, Pageable pageable);

    NotificationResponse markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);

    long getUnreadCount(UUID userId);
}
