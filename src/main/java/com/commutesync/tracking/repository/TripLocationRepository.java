package com.commutesync.tracking.repository;

import com.commutesync.tracking.domain.TripLocation;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripLocationRepository extends JpaRepository<TripLocation, Long> {

    Optional<TripLocation> findFirstByTripIdOrderByRecordedAtDescIdDesc(Long tripId);

    Page<TripLocation> findByTripIdOrderByRecordedAtDesc(Long tripId, Pageable pageable);
}
