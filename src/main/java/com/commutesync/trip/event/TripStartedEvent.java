package com.commutesync.trip.event;

public record TripStartedEvent(Long tripId, String driverEmail) {
}
