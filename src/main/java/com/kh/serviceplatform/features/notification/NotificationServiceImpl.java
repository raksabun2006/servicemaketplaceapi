package com.kh.serviceplatform.features.notification;

import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.notification.dto.NotificationResponse;
import com.kh.serviceplatform.features.notification.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public void sendNotification(User recipient, NotificationType type, String title, String message, UUID referenceId, String referenceType) {
        if (recipient == null) {
            return;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type != null ? type : NotificationType.SYSTEM)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .read(false)
                .build();

        notificationRepository.save(notification);
        log.debug("Sent notification to user {}: {}", recipient.getId(), title);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(UUID userId, Boolean unreadOnly, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable != null ? pageable.getPageNumber() : 0,
                pageable != null ? pageable.getPageSize() : 20,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository.findByRecipientIdAndReadFalse(userId, sortedPageable)
                    .map(notificationMapper::toResponse);
        }

        return notificationRepository.findByRecipientId(userId, sortedPageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this notification");
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toResponse(saved);
    }

    @Override
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByRecipientId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }
}
