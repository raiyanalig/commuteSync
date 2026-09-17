package com.commutesync.fleet.repository;

import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.DriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("""
            SELECT d FROM Driver d
            WHERE (:status IS NULL OR d.status = :status)
              AND (:available IS NULL OR d.available = :available)
              AND (:search IS NULL
                   OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(d.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Driver> search(@Param("status") DriverStatus status,
                        @Param("available") Boolean available,
                        @Param("search") String search,
                        Pageable pageable);
}
