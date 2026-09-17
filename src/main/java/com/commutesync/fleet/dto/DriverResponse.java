package com.commutesync.fleet.dto;

import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.DriverStatus;
import java.time.Instant;
import java.time.LocalDate;

public record DriverResponse(
        Long id,
        String fullName,
        String phone,
        String email,
        String licenseNumber,
        LocalDate licenseExpiryDate,
        DriverStatus status,
        boolean available,
        boolean assignable,
        Instant createdAt,
        Instant updatedAt
) {

    public static DriverResponse from(Driver driver) {
        return new DriverResponse(
                driver.getId(),
                driver.getFullName(),
                driver.getPhone(),
                driver.getEmail(),
                driver.getLicenseNumber(),
                driver.getLicenseExpiryDate(),
                driver.getStatus(),
                driver.isAvailable(),
                driver.isAssignable(),
                driver.getCreatedAt(),
                driver.getUpdatedAt()
        );
    }
}
