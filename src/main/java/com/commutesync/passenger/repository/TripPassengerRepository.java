package com.commutesync.passenger.repository;

import com.commutesync.passenger.domain.PassengerStatus;
import com.commutesync.passenger.domain.TripPassenger;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripPassengerRepository extends JpaRepository<TripPassenger, Long> {

    List<TripPassenger> findByTripIdAndStatusNotOrderBySeatNumberAsc(Long tripId, PassengerStatus status);

    List<TripPassenger> findByTripIdAndStatusNot(Long tripId, PassengerStatus status);

    Optional<TripPassenger> findByTripIdAndEmployeeId(Long tripId, Long employeeId);

    Optional<TripPassenger> findByIdAndTripId(Long id, Long tripId);
}
