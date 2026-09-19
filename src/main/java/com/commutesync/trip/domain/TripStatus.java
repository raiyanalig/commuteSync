package com.commutesync.trip.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum TripStatus {
    SCHEDULED,
    ASSIGNED,
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    private static final Map<TripStatus, Set<TripStatus>> ALLOWED_TRANSITIONS = Map.of(
            SCHEDULED, EnumSet.of(ASSIGNED, CANCELLED),
            ASSIGNED, EnumSet.of(STARTED, CANCELLED),
            STARTED, EnumSet.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, EnumSet.of(COMPLETED, CANCELLED),
            COMPLETED, EnumSet.noneOf(TripStatus.class),
            CANCELLED, EnumSet.noneOf(TripStatus.class)
    );

    public boolean canTransitionTo(TripStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(TripStatus.class)).contains(target);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
