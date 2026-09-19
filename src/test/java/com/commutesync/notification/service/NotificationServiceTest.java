package com.commutesync.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final String EMAIL = "raiyan@example.com";

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createPersistsUnreadNotificationAndPublishesEvent() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        NotificationResponse response = notificationService.create(EMAIL, NotificationType.BOOKING_CONFIRMED,
                "Booking confirmed", "Your booking is confirmed.", NotificationReferenceType.BOOKING, 100L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(NotificationStatus.UNREAD);
        assertThat(response.readAt()).isNull();
        assertThat(response.referenceId()).isEqualTo(100L);
        verify(eventPublisher).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void createFromRequestMapsFields() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.create(new CreateNotificationRequest(
                EMAIL, NotificationType.TRIP_ASSIGNED, "Trip assigned", "You have a trip.",
                NotificationReferenceType.TRIP, 5L));

        assertThat(response.type()).isEqualTo(NotificationType.TRIP_ASSIGNED);
        assertThat(response.referenceType()).isEqualTo(NotificationReferenceType.TRIP);
    }

    @Test
    void getNotificationsReturnsPage() {
        when(notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(eq(EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification(1L, NotificationStatus.UNREAD))));

        PageResponse<NotificationResponse> response =
                notificationService.getNotifications(EMAIL, null, PageRequest.of(0, 20));

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void getNotificationsFiltersByStatus() {
        when(notificationRepository.findByRecipientEmailAndStatusOrderByCreatedAtDesc(
                eq(EMAIL), eq(NotificationStatus.UNREAD), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification(1L, NotificationStatus.UNREAD))));

        PageResponse<NotificationResponse> response =
                notificationService.getNotifications(EMAIL, NotificationStatus.UNREAD, PageRequest.of(0, 20));

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void countUnreadReturnsCount() {
        when(notificationRepository.countByRecipientEmailAndStatus(EMAIL, NotificationStatus.UNREAD))
                .thenReturn(3L);

        assertThat(notificationService.countUnread(EMAIL)).isEqualTo(3L);
    }

    @Test
    void markAsReadMarksNotificationRead() {
        Notification notification = notification(1L, NotificationStatus.UNREAD);
        when(notificationRepository.findByIdAndRecipientEmail(1L, EMAIL)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(1L, EMAIL);

        assertThat(response.status()).isEqualTo(NotificationStatus.READ);
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void markAsReadThrowsWhenNotOwned() {
        when(notificationRepository.findByIdAndRecipientEmail(99L, EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllAsReadDelegatesToRepository() {
        when(notificationRepository.markAllRead(eq(EMAIL), any(Instant.class))).thenReturn(2);

        assertThat(notificationService.markAllAsRead(EMAIL)).isEqualTo(2);
    }

    private Notification notification(Long id, NotificationStatus status) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setRecipientEmail(EMAIL);
        notification.setType(NotificationType.BOOKING_CONFIRMED);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setStatus(status);
        notification.setReferenceType(NotificationReferenceType.BOOKING);
        notification.setReferenceId(100L);
        return notification;
    }
}
