package com.commutesync.booking.event;

public record BookingConfirmedEvent(Long bookingId, Long tripId, String recipientEmail) {
}
