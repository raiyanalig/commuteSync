package com.commutesync.booking.event;

public record BookingCancelledEvent(Long bookingId, Long tripId, String recipientEmail) {
}
