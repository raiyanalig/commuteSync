package com.commutesync.trip.event;

public record TripAssignedEvent(Long tripId, String driverEmail) {
}
