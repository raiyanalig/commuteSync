package com.commutesync.trip.event;

public record TripCompletedEvent(Long tripId, String driverEmail) {
}
