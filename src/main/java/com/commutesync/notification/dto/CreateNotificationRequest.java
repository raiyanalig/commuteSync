package com.commutesync.notification.dto;

import com.commutesync.notification.domain.NotificationReferenceType;
import com.commutesync.notification.domain.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(

        @NotBlank(message = "Recipient email is required")
        @Email(message = "Recipient email must be valid")
        @Size(max = 150, message = "Recipient email must be at most 150 characters")
        String recipientEmail,

        @NotNull(message = "Notification type is required")
        NotificationType type,

        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message must be at most 500 characters")
        String message,

        NotificationReferenceType referenceType,

        @Positive(message = "Reference id must be positive")
        Long referenceId
) {
}
