package com.commutesync.fleet.repository;

import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByAssignedDriverId(Long driverId);

    Optional<Vehicle> findByAssignedDriverId(Long driverId);

    List<Vehicle> findByStatusAndCapacityGreaterThanOrderByCapacityDesc(VehicleStatus status, int capacity);

    @EntityGraph(attributePaths = "assignedDriver")
    @Query("""
            SELECT v FROM Vehicle v
            WHERE (:status IS NULL OR v.status = :status)
              AND (:search IS NULL
                   OR LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Vehicle> search(@Param("status") VehicleStatus status,
                         @Param("search") String search,
                         Pageable pageable);
}
