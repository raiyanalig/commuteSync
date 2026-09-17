package com.commutesync.schedule.dto;

import com.commutesync.employee.domain.Employee;

public record EmployeeShiftResponse(
        Long id,
        String employeeCode,
        String fullName,
        String email,
        Long shiftId
) {

    public static EmployeeShiftResponse from(Employee employee) {
        return new EmployeeShiftResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getShiftId()
        );
    }
}
