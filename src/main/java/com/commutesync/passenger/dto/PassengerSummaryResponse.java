package com.commutesync.passenger.dto;

public record PassengerSummaryResponse(
        Long tripId,
        int vehicleCapacity,
        int activePassengers,
        int confirmedPassengers,
        int boardedPassengers,
        int availableSeats
) {
}
