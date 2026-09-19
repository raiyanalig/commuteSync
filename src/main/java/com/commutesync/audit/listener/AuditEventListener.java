package com.commutesync.audit.listener;

import com.commutesync.audit.domain.AuditAction;
import com.commutesync.audit.service.AuditService;
import com.commutesync.booking.event.BookingCancelledEvent;
import com.commutesync.booking.event.BookingConfirmedEvent;
import com.commutesync.trip.event.TripAssignedEvent;
import com.commutesync.trip.event.TripCancelledEvent;
import com.commutesync.trip.event.TripCompletedEvent;
import com.commutesync.trip.event.TripStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Builds the audit trail from the existing booking and trip domain events. Running
 * after commit guarantees we only audit actions that actually took effect.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditService auditService;

    public AuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.debug("Auditing booking confirmation for booking {}", event.bookingId());
        auditService.record(AuditAction.BOOKING_CREATED, "BOOKING", event.bookingId(),
                "Booking confirmed for trip " + event.tripId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.debug("Auditing booking cancellation for booking {}", event.bookingId());
        auditService.record(AuditAction.BOOKING_CANCELLED, "BOOKING", event.bookingId(),
                "Booking cancelled for trip " + event.tripId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripAssigned(TripAssignedEvent event) {
        log.debug("Auditing trip assignment for trip {}", event.tripId());
        auditService.record(AuditAction.TRIP_ASSIGNED, "TRIP", event.tripId(),
                "Driver " + event.driverEmail() + " assigned to trip");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripStarted(TripStartedEvent event) {
        log.debug("Auditing trip start for trip {}", event.tripId());
        auditService.record(AuditAction.TRIP_STARTED, "TRIP", event.tripId(), "Trip started");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripCompleted(TripCompletedEvent event) {
        log.debug("Auditing trip completion for trip {}", event.tripId());
        auditService.record(AuditAction.TRIP_COMPLETED, "TRIP", event.tripId(), "Trip completed");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripCancelled(TripCancelledEvent event) {
        log.debug("Auditing trip cancellation for trip {}", event.tripId());
        String details = "Trip cancelled"
                + (event.reason() == null || event.reason().isBlank() ? "" : ": " + event.reason());
        auditService.record(AuditAction.TRIP_CANCELLED, "TRIP", event.tripId(), details);
    }
}
