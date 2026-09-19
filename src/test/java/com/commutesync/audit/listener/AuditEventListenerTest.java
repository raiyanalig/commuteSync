package com.commutesync.audit.listener;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.commutesync.audit.domain.AuditAction;
import com.commutesync.audit.service.AuditService;
import com.commutesync.booking.event.BookingCancelledEvent;
import com.commutesync.booking.event.BookingConfirmedEvent;
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
class AuditEventListenerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditEventListener listener;

    @Test
    void bookingConfirmedIsAudited() {
        listener.onBookingConfirmed(new BookingConfirmedEvent(100L, 1L, "raiyan@example.com"));

        verify(auditService).record(eq(AuditAction.BOOKING_CREATED), eq("BOOKING"), eq(100L), contains("1"));
    }

    @Test
    void bookingCancelledIsAudited() {
        listener.onBookingCancelled(new BookingCancelledEvent(100L, 1L, "raiyan@example.com"));

        verify(auditService).record(eq(AuditAction.BOOKING_CANCELLED), eq("BOOKING"), eq(100L), contains("1"));
    }

    @Test
    void tripAssignedIsAudited() {
        listener.onTripAssigned(new TripAssignedEvent(7L, "driver@example.com"));

        verify(auditService).record(eq(AuditAction.TRIP_ASSIGNED), eq("TRIP"), eq(7L), contains("driver@example.com"));
    }

    @Test
    void tripStartedIsAudited() {
        listener.onTripStarted(new TripStartedEvent(7L, "driver@example.com"));

        verify(auditService).record(eq(AuditAction.TRIP_STARTED), eq("TRIP"), eq(7L), contains("started"));
    }

    @Test
    void tripCompletedIsAudited() {
        listener.onTripCompleted(new TripCompletedEvent(7L, "driver@example.com"));

        verify(auditService).record(eq(AuditAction.TRIP_COMPLETED), eq("TRIP"), eq(7L), contains("completed"));
    }

    @Test
    void tripCancelledIncludesReason() {
        listener.onTripCancelled(new TripCancelledEvent(7L, "driver@example.com", "Vehicle breakdown"));

        verify(auditService).record(eq(AuditAction.TRIP_CANCELLED), eq("TRIP"), eq(7L), contains("Vehicle breakdown"));
    }
}
