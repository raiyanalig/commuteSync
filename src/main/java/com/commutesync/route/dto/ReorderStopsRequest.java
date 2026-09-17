package com.commutesync.route.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderStopsRequest(

        @NotEmpty(message = "Stop ids are required")
        List<@NotNull(message = "Stop id must not be null") Long> stopIds
) {
}
