package com.kh.serviceplatform.features.notification;

import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.auth.User;
import com.kh.serviceplatform.features.auth.UserRepository;
import com.kh.serviceplatform.features.notification.dto.NotificationResponse;
import com.kh.serviceplatform.features.notification.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationMapper mapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User recipient;
    private Notification notification;

    @BeforeEach
    void setUp() {
        recipient = User.builder()
                .id(UUID.randomUUID())
                .fullName("Sok San")
                .email("sok@example.com")
                .build();

        notification = Notification.builder()
                .id(UUID.randomUUID())
                .recipient(recipient)
                .type(NotificationType.NEW_OFFER)
                .title("New Offer")
                .message("You received an offer")
                .read(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void shouldSendNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.sendNotification(
                recipient, NotificationType.NEW_OFFER, "New Offer", "You received an offer", UUID.randomUUID(), "SERVICE_REQUEST"
        );

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void shouldMarkNotificationAsRead() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toResponse(notification)).thenReturn(mock(NotificationResponse.class));

        NotificationResponse response = notificationService.markAsRead(recipient.getId(), notification.getId());

        assertNotNull(response);
        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void shouldPreventOtherUserFromMarkingNotificationRead() {
        UUID otherUserId = UUID.randomUUID();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThrows(ForbiddenException.class, () ->
                notificationService.markAsRead(otherUserId, notification.getId()));
    }

    @Test
    void shouldMarkAllAsRead() {
        notificationService.markAllAsRead(recipient.getId());
        verify(notificationRepository).markAllAsReadByRecipientId(recipient.getId());
    }

    @Test
    void shouldGetUnreadCount() {
        when(notificationRepository.countByRecipientIdAndReadFalse(recipient.getId())).thenReturn(3L);

        long count = notificationService.getUnreadCount(recipient.getId());

        assertEquals(3L, count);
    }
}
