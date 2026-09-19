package com.commutesync.trip.repository;

import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("""
            SELECT t FROM Trip t
            LEFT JOIN FETCH t.schedule
            LEFT JOIN FETCH t.route
            LEFT JOIN FETCH t.driver
            LEFT JOIN FETCH t.vehicle
            WHERE t.id = :id
            """)
    Optional<Trip> findWithDetailsById(@Param("id") Long id);

    /**
     * Acquires a row-level write lock (SELECT ... FOR UPDATE) so concurrent bookings
     * for the same trip are serialized and the last seat cannot be double-booked.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT t FROM Trip t
            LEFT JOIN FETCH t.schedule s
            LEFT JOIN FETCH t.route r
            LEFT JOIN FETCH t.driver d
            LEFT JOIN FETCH t.vehicle v
            WHERE (:serviceDate IS NULL OR t.serviceDate = :serviceDate)
              AND (:status IS NULL OR t.status = :status)
              AND (:routeId IS NULL OR r.id = :routeId)
              AND (:driverId IS NULL OR d.id = :driverId)
              AND (:vehicleId IS NULL OR v.id = :vehicleId)
            """)
    Page<Trip> search(@Param("serviceDate") LocalDate serviceDate,
                      @Param("status") TripStatus status,
                      @Param("routeId") Long routeId,
                      @Param("driverId") Long driverId,
                      @Param("vehicleId") Long vehicleId,
                      Pageable pageable);
}
