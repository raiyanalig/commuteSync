package com.commutesync.employee.dto;

import com.commutesync.employee.domain.EmployeeStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(

        @NotBlank(message = "Employee code is required")
        @Pattern(regexp = "^[A-Za-z0-9-]{2,20}$",
                message = "Employee code must be 2-20 characters (letters, digits, hyphen)")
        String employeeCode,

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                message = "Phone must be 7-15 digits with an optional leading +")
        String phone,

        @NotBlank(message = "Pickup address is required")
        @Size(max = 255, message = "Pickup address must be at most 255 characters")
        String pickupAddress,

        @Size(max = 150, message = "Pickup point must be at most 150 characters")
        String pickupPoint,

        @Positive(message = "Shift id must be positive")
        Long shiftId,

        @NotNull(message = "Status is required")
        EmployeeStatus status
) {
}
