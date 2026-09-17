package com.commutesync.fleet.dto;

import com.commutesync.fleet.domain.Driver;

public record DriverSummary(
        Long id,
        String fullName,
        String phone,
        String licenseNumber
) {

    public static DriverSummary from(Driver driver) {
        return new DriverSummary(
                driver.getId(),
                driver.getFullName(),
                driver.getPhone(),
                driver.getLicenseNumber()
        );
    }
}
