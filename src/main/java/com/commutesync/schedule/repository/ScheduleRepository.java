package com.commutesync.schedule.repository;

import com.commutesync.schedule.domain.Schedule;
import com.commutesync.schedule.domain.ScheduleStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    boolean existsByShiftId(Long shiftId);

    boolean existsByServiceDateAndShiftIdAndRouteIdAndDepartureTime(
            LocalDate serviceDate, Long shiftId, Long routeId, LocalTime departureTime);

    @EntityGraph(attributePaths = {"shift", "route", "vehicle", "driver"})
    @Query("SELECT s FROM Schedule s WHERE s.id = :id")
    Optional<Schedule> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"shift", "route", "vehicle", "driver"})
    @Query("""
            SELECT s FROM Schedule s
            WHERE (:serviceDate IS NULL OR s.serviceDate = :serviceDate)
              AND (:status IS NULL OR s.status = :status)
              AND (:routeId IS NULL OR s.route.id = :routeId)
            """)
    Page<Schedule> search(@Param("serviceDate") LocalDate serviceDate,
                          @Param("status") ScheduleStatus status,
                          @Param("routeId") Long routeId,
                          Pageable pageable);
}
