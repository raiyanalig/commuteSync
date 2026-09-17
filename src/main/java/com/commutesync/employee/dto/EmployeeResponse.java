package com.commutesync.employee.dto;

import com.commutesync.employee.domain.Employee;
import com.commutesync.employee.domain.EmployeeStatus;
import java.time.Instant;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String fullName,
        String email,
        String phone,
        String pickupAddress,
        String pickupPoint,
        Long shiftId,
        EmployeeStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getPickupAddress(),
                employee.getPickupPoint(),
                employee.getShiftId(),
                employee.getStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
