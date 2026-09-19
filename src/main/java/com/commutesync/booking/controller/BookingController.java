package com.commutesync.booking.controller;

import com.commutesync.booking.domain.BookingStatus;
import com.commutesync.booking.dto.AvailableTripResponse;
import com.commutesync.booking.dto.BookingResponse;
import com.commutesync.booking.dto.CancelBookingRequest;
import com.commutesync.booking.dto.CreateBookingRequest;
import com.commutesync.booking.service.BookingService;
import com.commutesync.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Employee trip booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/available-trips")
    @Operation(summary = "List trips that are open for booking and have free seats")
    public PageResponse<AvailableTripResponse> getAvailableTrips(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate serviceDate,
            @RequestParam(required = false) Long routeId,
            @PageableDefault(size = 10, sort = "serviceDate") Pageable pageable) {
        return bookingService.getAvailableTrips(serviceDate, routeId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a trip for the authenticated employee")
    public BookingResponse book(@Valid @RequestBody CreateBookingRequest request,
                                Authentication authentication) {
        return bookingService.book(request.tripId(), authentication.getName());
    }

    @GetMapping("/me")
    @Operation(summary = "List the authenticated employee's bookings (history)")
    public PageResponse<BookingResponse> getMyBookings(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "bookedAt") Pageable pageable) {
        return bookingService.getMyBookings(authentication.getName(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a booking by id (owner or admin)")
    public BookingResponse getBooking(@PathVariable Long id, Authentication authentication) {
        return bookingService.getBooking(id, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking (owner or admin)")
    public BookingResponse cancel(@PathVariable Long id,
                                  @Valid @RequestBody(required = false) CancelBookingRequest request,
                                  Authentication authentication) {
        return bookingService.cancel(id, authentication.getName(), isAdmin(authentication),
                request == null ? null : request.reason());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all bookings (ADMIN only)")
    public PageResponse<BookingResponse> getAllBookings(
            @RequestParam(required = false) Long tripId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 10, sort = "bookedAt") Pageable pageable) {
        return bookingService.getAllBookings(tripId, employeeId, status, pageable);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
