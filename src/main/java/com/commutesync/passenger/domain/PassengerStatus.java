package com.commutesync.passenger.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum PassengerStatus {
    EXPECTED,
    CONFIRMED,
    BOARDED,
    CANCELLED;

    private static final Map<PassengerStatus, Set<PassengerStatus>> ALLOWED_TRANSITIONS = Map.of(
            EXPECTED, EnumSet.of(CONFIRMED, CANCELLED),
            CONFIRMED, EnumSet.of(BOARDED, CANCELLED),
            BOARDED, EnumSet.noneOf(PassengerStatus.class),
            CANCELLED, EnumSet.noneOf(PassengerStatus.class)
    );

    public boolean canTransitionTo(PassengerStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(PassengerStatus.class)).contains(target);
    }

    public boolean isTerminal() {
        return this == BOARDED || this == CANCELLED;
    }

    public boolean isActive() {
        return this != CANCELLED;
    }
}
