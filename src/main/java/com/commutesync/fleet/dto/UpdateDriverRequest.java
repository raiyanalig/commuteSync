package com.commutesync.fleet.dto;

import com.commutesync.fleet.domain.DriverStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateDriverRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                message = "Phone must be 7-15 digits with an optional leading +")
        String phone,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "License number is required")
        @Size(max = 50, message = "License number must be at most 50 characters")
        String licenseNumber,

        @NotNull(message = "License expiry date is required")
        LocalDate licenseExpiryDate,

        @NotNull(message = "Status is required")
        DriverStatus status,

        @NotNull(message = "Availability is required")
        Boolean available
) {
}
