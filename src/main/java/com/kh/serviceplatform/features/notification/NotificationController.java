package com.kh.serviceplatform.features.notification;

import com.kh.serviceplatform.common.security.SecurityUtils;
import com.kh.serviceplatform.features.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Notifications", description = "Endpoints for retrieving and managing user in-app notifications")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user notifications", description = "Browse notifications for the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth"))
    public Page<NotificationResponse> getMyNotifications(
            @RequestParam(required = false) Boolean unreadOnly,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return notificationService.getMyNotifications(currentUserId, unreadOnly, pageable);
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark notification as read", description = "Mark a single notification as read",
            security = @SecurityRequirement(name = "bearerAuth"))
    public NotificationResponse markAsRead(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return notificationService.markAsRead(currentUserId, id);
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public NotificationResponse markAsReadPut(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return notificationService.markAsRead(currentUserId, id);
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read", description = "Mark all notifications for authenticated user as read",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> markAllAsReadPut() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread notification count", description = "Retrieve total count of unread notifications",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        long unreadCount = notificationService.getUnreadCount(currentUserId);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }
}
