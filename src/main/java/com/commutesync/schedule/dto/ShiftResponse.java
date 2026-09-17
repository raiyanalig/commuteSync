package com.commutesync.schedule.dto;

import com.commutesync.schedule.domain.Shift;
import com.commutesync.schedule.domain.ShiftStatus;
import java.time.Instant;
import java.time.LocalTime;

public record ShiftResponse(
        Long id,
        String shiftCode,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        boolean crossesMidnight,
        ShiftStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ShiftResponse from(Shift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getShiftCode(),
                shift.getName(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.crossesMidnight(),
                shift.getStatus(),
                shift.getCreatedAt(),
                shift.getUpdatedAt()
        );
    }
}
