package com.commutesync.notification.event;

import com.commutesync.notification.dto.NotificationResponse;

/**
 * Published after a notification is persisted. Future delivery channels (email, push,
 * Kafka) subscribe to this without the notification service knowing about them.
 */
public record NotificationCreatedEvent(NotificationResponse notification) {
}
