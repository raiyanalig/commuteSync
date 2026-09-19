package com.commutesync.notification.service;

import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.notification.domain.Notification;
import com.commutesync.notification.domain.NotificationReferenceType;
import com.commutesync.notification.domain.NotificationStatus;
import com.commutesync.notification.domain.NotificationType;
import com.commutesync.notification.dto.CreateNotificationRequest;
import com.commutesync.notification.dto.NotificationResponse;
import com.commutesync.notification.event.NotificationCreatedEvent;
import com.commutesync.notification.repository.NotificationRepository;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationService(NotificationRepository notificationRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Single write path for all notifications. Event listeners and the admin API both
     * call this, so future channels only need to listen to {@link NotificationCreatedEvent}.
     */
    @Transactional
    public NotificationResponse create(String recipientEmail,
                                       NotificationType type,
                                       String title,
                                       String message,
                                       NotificationReferenceType referenceType,
                                       Long referenceId) {
        Notification notification = new Notification();
        notification.setRecipientEmail(recipientEmail);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = NotificationResponse.from(saved);
        eventPublisher.publishEvent(new NotificationCreatedEvent(response));
        return response;
    }

    @Transactional
    public NotificationResponse create(CreateNotificationRequest request) {
        return create(request.recipientEmail(), request.type(), request.title(), request.message(),
                request.referenceType(), request.referenceId());
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(String recipientEmail,
                                                               NotificationStatus status,
                                                               Pageable pageable) {
        Page<Notification> page = status == null
                ? notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(recipientEmail, pageable)
                : notificationRepository.findByRecipientEmailAndStatusOrderByCreatedAtDesc(
                        recipientEmail, status, pageable);
        return PageResponse.from(page.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long countUnread(String recipientEmail) {
        return notificationRepository.countByRecipientEmailAndStatus(recipientEmail, NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, String recipientEmail) {
        Notification notification = notificationRepository.findByIdAndRecipientEmail(id, recipientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        notification.markRead();
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllAsRead(String recipientEmail) {
        return notificationRepository.markAllRead(recipientEmail, Instant.now());
    }
}
