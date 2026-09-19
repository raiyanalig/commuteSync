package com.commutesync.passenger.controller;

import com.commutesync.passenger.dto.AddPassengerRequest;
import com.commutesync.passenger.dto.PassengerSummaryResponse;
import com.commutesync.passenger.dto.TripPassengerResponse;
import com.commutesync.passenger.service.TripPassengerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/passengers")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Passengers", description = "Trip passenger manifest (ADMIN only)")
public class TripPassengerController {

    private final TripPassengerService tripPassengerService;

    public TripPassengerController(TripPassengerService tripPassengerService) {
        this.tripPassengerService = tripPassengerService;
    }

    @GetMapping
    @Operation(summary = "List a trip's passengers (synced from confirmed bookings)")
    public List<TripPassengerResponse> getPassengers(@PathVariable Long tripId) {
        return tripPassengerService.getPassengers(tripId);
    }

    @GetMapping("/summary")
    @Operation(summary = "Passenger count and seat availability for a trip")
    public PassengerSummaryResponse getSummary(@PathVariable Long tripId) {
        return tripPassengerService.getSummary(tripId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a passenger from a confirmed booking")
    public TripPassengerResponse addPassenger(@PathVariable Long tripId,
                                              @Valid @RequestBody AddPassengerRequest request) {
        return tripPassengerService.addPassenger(tripId, request.bookingId());
    }

    @PostMapping("/{passengerId}/confirm")
    @Operation(summary = "Confirm a passenger")
    public TripPassengerResponse confirmPassenger(@PathVariable Long tripId,
                                                  @PathVariable Long passengerId) {
        return tripPassengerService.confirmPassenger(tripId, passengerId);
    }

    @PostMapping("/{passengerId}/board")
    @Operation(summary = "Mark a passenger as boarded")
    public TripPassengerResponse boardPassenger(@PathVariable Long tripId,
                                                @PathVariable Long passengerId) {
        return tripPassengerService.boardPassenger(tripId, passengerId);
    }

    @DeleteMapping("/{passengerId}")
    @Operation(summary = "Remove a passenger (cancels the manifest entry and frees the seat)")
    public TripPassengerResponse removePassenger(@PathVariable Long tripId,
                                                 @PathVariable Long passengerId) {
        return tripPassengerService.removePassenger(tripId, passengerId);
    }
}
