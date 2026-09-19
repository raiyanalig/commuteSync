package com.commutesync.trip.event;

public record TripCancelledEvent(Long tripId, String driverEmail, String reason) {
}
