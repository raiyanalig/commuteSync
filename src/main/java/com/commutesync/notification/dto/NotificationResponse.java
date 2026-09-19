package com.commutesync.notification.dto;

import com.commutesync.notification.domain.Notification;
import com.commutesync.notification.domain.NotificationReferenceType;
import com.commutesync.notification.domain.NotificationStatus;
import com.commutesync.notification.domain.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        String recipientEmail,
        NotificationType type,
        String title,
        String message,
        NotificationStatus status,
        NotificationReferenceType referenceType,
        Long referenceId,
        Instant readAt,
        Instant createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipientEmail(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
