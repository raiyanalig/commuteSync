package com.commutesync.schedule.domain;

import com.commutesync.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalTime;

@Entity
@Table(name = "shifts", uniqueConstraints =
        @UniqueConstraint(name = "uk_shifts_code", columnNames = "shift_code"))
public class Shift extends BaseEntity {

    @Column(name = "shift_code", nullable = false, length = 20)
    private String shiftCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftStatus status = ShiftStatus.ACTIVE;

    /**
     * A shift is a reusable daily template (wall-clock times, no date).
     * If end is before start the shift runs past midnight, e.g. 22:00 -> 06:00.
     */
    public boolean crossesMidnight() {
        return startTime != null && endTime != null && endTime.isBefore(startTime);
    }

    public String getShiftCode() {
        return shiftCode;
    }

    public void setShiftCode(String shiftCode) {
        this.shiftCode = shiftCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public void setStatus(ShiftStatus status) {
        this.status = status;
    }
}
