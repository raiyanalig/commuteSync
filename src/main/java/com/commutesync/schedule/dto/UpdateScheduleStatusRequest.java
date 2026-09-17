package com.commutesync.schedule.dto;

import com.commutesync.schedule.domain.ScheduleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateScheduleStatusRequest(

        @NotNull(message = "Status is required")
        ScheduleStatus status
) {
}
