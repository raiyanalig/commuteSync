package com.commutesync.notification.listener;

import com.commutesync.booking.event.BookingCancelledEvent;
import com.commutesync.booking.event.BookingConfirmedEvent;
import com.commutesync.notification.domain.NotificationReferenceType;
import com.commutesync.notification.domain.NotificationType;
import com.commutesync.notification.service.NotificationService;
import com.commutesync.trip.event.TripAssignedEvent;
import com.commutesync.trip.event.TripCancelledEvent;
import com.commutesync.trip.event.TripCompletedEvent;
import com.commutesync.trip.event.TripStartedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Translates business domain events into persisted notifications. Runs after the
 * publishing transaction commits, so we never notify about rolled-back operations.
 */
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        if (isBlank(event.recipientEmail())) {
            return;
        }
        notificationService.create(event.recipientEmail(), NotificationType.BOOKING_CONFIRMED,
                "Booking confirmed",
                "Your booking #" + event.bookingId() + " has been confirmed.",
                NotificationReferenceType.BOOKING, event.bookingId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        if (isBlank(event.recipientEmail())) {
            return;
        }
        notificationService.create(event.recipientEmail(), NotificationType.BOOKING_CANCELLED,
                "Booking cancelled",
                "Your booking #" + event.bookingId() + " has been cancelled.",
                NotificationReferenceType.BOOKING, event.bookingId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripAssigned(TripAssignedEvent event) {
        if (isBlank(event.driverEmail())) {
            return;
        }
        notificationService.create(event.driverEmail(), NotificationType.TRIP_ASSIGNED,
                "Trip assigned",
                "You have been assigned to trip #" + event.tripId() + ".",
                NotificationReferenceType.TRIP, event.tripId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripStarted(TripStartedEvent event) {
        if (isBlank(event.driverEmail())) {
            return;
        }
        notificationService.create(event.driverEmail(), NotificationType.TRIP_STARTED,
                "Trip started",
                "Trip #" + event.tripId() + " has started.",
                NotificationReferenceType.TRIP, event.tripId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripCompleted(TripCompletedEvent event) {
        if (isBlank(event.driverEmail())) {
            return;
        }
        notificationService.create(event.driverEmail(), NotificationType.TRIP_COMPLETED,
                "Trip completed",
                "Trip #" + event.tripId() + " has been completed.",
                NotificationReferenceType.TRIP, event.tripId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripCancelled(TripCancelledEvent event) {
        if (isBlank(event.driverEmail())) {
            return;
        }
        String suffix = isBlank(event.reason()) ? "" : " Reason: " + event.reason();
        notificationService.create(event.driverEmail(), NotificationType.TRIP_CANCELLED,
                "Trip cancelled",
                "Trip #" + event.tripId() + " has been cancelled." + suffix,
                NotificationReferenceType.TRIP, event.tripId());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
