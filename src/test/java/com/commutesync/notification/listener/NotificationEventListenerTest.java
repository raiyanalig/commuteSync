package com.commutesync.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.commutesync.booking.event.BookingCancelledEvent;
import com.commutesync.booking.event.BookingConfirmedEvent;
import com.commutesync.notification.domain.NotificationReferenceType;
import com.commutesync.notification.domain.NotificationType;
import com.commutesync.notification.service.NotificationService;
import com.commutesync.trip.event.TripAssignedEvent;
import com.commutesync.trip.event.TripCancelledEvent;
import com.commutesync.trip.event.TripCompletedEvent;
import com.commutesync.trip.event.TripStartedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void bookingConfirmedCreatesNotification() {
        listener.onBookingConfirmed(new BookingConfirmedEvent(100L, 1L, "raiyan@example.com"));

        verify(notificationService).create(eq("raiyan@example.com"), eq(NotificationType.BOOKING_CONFIRMED),
                anyString(), contains("100"), eq(NotificationReferenceType.BOOKING), eq(100L));
    }

    @Test
    void bookingConfirmedSkipsBlankRecipient() {
        listener.onBookingConfirmed(new BookingConfirmedEvent(100L, 1L, null));

        verify(notificationService, never()).create(anyString(), any(), anyString(), anyString(), any(), anyLong());
    }

    @Test
    void bookingCancelledCreatesNotification() {
        listener.onBookingCancelled(new BookingCancelledEvent(100L, 1L, "raiyan@example.com"));

        verify(notificationService).create(eq("raiyan@example.com"), eq(NotificationType.BOOKING_CANCELLED),
                anyString(), contains("100"), eq(NotificationReferenceType.BOOKING), eq(100L));
    }

    @Test
    void tripAssignedCreatesNotification() {
        listener.onTripAssigned(new TripAssignedEvent(7L, "driver@example.com"));

        verify(notificationService).create(eq("driver@example.com"), eq(NotificationType.TRIP_ASSIGNED),
                anyString(), contains("7"), eq(NotificationReferenceType.TRIP), eq(7L));
    }

    @Test
    void tripAssignedSkipsWhenNoDriver() {
        listener.onTripAssigned(new TripAssignedEvent(7L, null));

        verify(notificationService, never()).create(anyString(), any(), anyString(), anyString(), any(), anyLong());
    }

    @Test
    void tripStartedCreatesNotification() {
        listener.onTripStarted(new TripStartedEvent(7L, "driver@example.com"));

        verify(notificationService).create(eq("driver@example.com"), eq(NotificationType.TRIP_STARTED),
                anyString(), anyString(), eq(NotificationReferenceType.TRIP), eq(7L));
    }

    @Test
    void tripCompletedCreatesNotification() {
        listener.onTripCompleted(new TripCompletedEvent(7L, "driver@example.com"));

        verify(notificationService).create(eq("driver@example.com"), eq(NotificationType.TRIP_COMPLETED),
                anyString(), anyString(), eq(NotificationReferenceType.TRIP), eq(7L));
    }

    @Test
    void tripCancelledIncludesReason() {
        listener.onTripCancelled(new TripCancelledEvent(7L, "driver@example.com", "Vehicle breakdown"));

        verify(notificationService).create(eq("driver@example.com"), eq(NotificationType.TRIP_CANCELLED),
                anyString(), contains("Vehicle breakdown"), eq(NotificationReferenceType.TRIP), eq(7L));
    }
}
