package com.commutesync.route.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStopRequest(

        @NotBlank(message = "Stop name is required")
        @Size(max = 150, message = "Stop name must be at most 150 characters")
        String name,

        @Size(max = 255, message = "Address must be at most 255 characters")
        String address
) {
}
