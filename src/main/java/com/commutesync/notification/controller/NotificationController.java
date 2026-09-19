package com.commutesync.notification.controller;

import com.commutesync.common.api.PageResponse;
import com.commutesync.notification.domain.NotificationStatus;
import com.commutesync.notification.dto.CreateNotificationRequest;
import com.commutesync.notification.dto.NotificationResponse;
import com.commutesync.notification.dto.UnreadCountResponse;
import com.commutesync.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    @Operation(summary = "List the authenticated user's notifications")
    public PageResponse<NotificationResponse> getMyNotifications(
            Authentication authentication,
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return notificationService.getNotifications(authentication.getName(), status, pageable);
    }

    @GetMapping("/me/unread-count")
    @Operation(summary = "Count the authenticated user's unread notifications")
    public UnreadCountResponse getUnreadCount(Authentication authentication) {
        return new UnreadCountResponse(notificationService.countUnread(authentication.getName()));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public NotificationResponse markAsRead(@PathVariable Long id, Authentication authentication) {
        return notificationService.markAsRead(id, authentication.getName());
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all of the authenticated user's notifications as read")
    public UnreadCountResponse markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
        return new UnreadCountResponse(0);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a notification manually (ADMIN only)")
    public NotificationResponse create(@Valid @RequestBody CreateNotificationRequest request) {
        return notificationService.create(request);
    }
}
