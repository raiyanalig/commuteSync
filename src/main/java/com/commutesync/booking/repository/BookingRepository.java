package com.commutesync.booking.repository;

import com.commutesync.booking.domain.Booking;
import com.commutesync.booking.domain.BookingStatus;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    interface TripSeatCount {
        Long getTripId();

        long getConfirmedBookings();
    }

    Optional<Booking> findByTripIdAndEmployeeId(Long tripId, Long employeeId);

    long countByTripIdAndStatus(Long tripId, BookingStatus status);

    List<Booking> findByTripIdAndStatus(Long tripId, BookingStatus status);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.trip.schedule.id = :scheduleId AND b.status = :status
            """)
    long countConfirmedByScheduleId(@Param("scheduleId") Long scheduleId,
                                    @Param("status") BookingStatus status);

    @EntityGraph(attributePaths = {"trip", "trip.route", "trip.vehicle", "employee"})
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"trip", "trip.route", "trip.vehicle", "employee"})
    @Query("SELECT b FROM Booking b WHERE b.employee.id = :employeeId")
    Page<Booking> findByEmployeeIdWithDetails(@Param("employeeId") Long employeeId, Pageable pageable);

    @EntityGraph(attributePaths = {"trip", "trip.route", "trip.vehicle", "employee"})
    @Query("""
            SELECT b FROM Booking b
            WHERE (:tripId IS NULL OR b.trip.id = :tripId)
              AND (:employeeId IS NULL OR b.employee.id = :employeeId)
              AND (:status IS NULL OR b.status = :status)
            """)
    Page<Booking> search(@Param("tripId") Long tripId,
                         @Param("employeeId") Long employeeId,
                         @Param("status") BookingStatus status,
                         Pageable pageable);

    /**
     * Trips that are open for booking and still have at least one free seat.
     * The correlated subquery compares confirmed bookings against the vehicle capacity.
     */
    @Query(value = """
            SELECT t FROM Trip t
            JOIN FETCH t.route
            JOIN FETCH t.vehicle
            WHERE t.vehicle IS NOT NULL
              AND t.status IN :statuses
              AND (:serviceDate IS NULL OR t.serviceDate = :serviceDate)
              AND (:routeId IS NULL OR t.route.id = :routeId)
              AND (SELECT COUNT(b) FROM Booking b
                   WHERE b.trip = t AND b.status = :confirmed) < t.vehicle.capacity
            """,
            countQuery = """
            SELECT COUNT(t) FROM Trip t
            JOIN t.vehicle v
            WHERE t.vehicle IS NOT NULL
              AND t.status IN :statuses
              AND (:serviceDate IS NULL OR t.serviceDate = :serviceDate)
              AND (:routeId IS NULL OR t.route.id = :routeId)
              AND (SELECT COUNT(b) FROM Booking b
                   WHERE b.trip = t AND b.status = :confirmed) < v.capacity
            """)
    Page<Trip> findAvailableForBooking(@Param("statuses") Collection<TripStatus> statuses,
                                       @Param("confirmed") BookingStatus confirmed,
                                       @Param("serviceDate") LocalDate serviceDate,
                                       @Param("routeId") Long routeId,
                                       Pageable pageable);

    @Query("""
            SELECT b.trip.id AS tripId, COUNT(b) AS confirmedBookings
            FROM Booking b
            WHERE b.trip.id IN :tripIds AND b.status = :status
            GROUP BY b.trip.id
            """)
    List<TripSeatCount> countConfirmedByTripIds(@Param("tripIds") Collection<Long> tripIds,
                                                @Param("status") BookingStatus status);
}
