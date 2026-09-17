package com.commutesync.schedule.dto;

import com.commutesync.schedule.domain.ShiftStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record CreateShiftRequest(

        @NotBlank(message = "Shift code is required")
        @Pattern(regexp = "^[A-Za-z0-9-]{2,20}$",
                message = "Shift code must be 2-20 characters (letters, digits, hyphen)")
        String shiftCode,

        @NotBlank(message = "Shift name is required")
        @Size(max = 100, message = "Shift name must be at most 100 characters")
        String name,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        ShiftStatus status
) {
}
