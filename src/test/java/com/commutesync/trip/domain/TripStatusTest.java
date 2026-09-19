package com.commutesync.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TripStatusTest {

    @Test
    void scheduledMovesToAssignedOrCancelled() {
        assertThat(TripStatus.SCHEDULED.canTransitionTo(TripStatus.ASSIGNED)).isTrue();
        assertThat(TripStatus.SCHEDULED.canTransitionTo(TripStatus.CANCELLED)).isTrue();
        assertThat(TripStatus.SCHEDULED.canTransitionTo(TripStatus.STARTED)).isFalse();
        assertThat(TripStatus.SCHEDULED.canTransitionTo(TripStatus.COMPLETED)).isFalse();
    }

    @Test
    void assignedMovesToStartedOrCancelled() {
        assertThat(TripStatus.ASSIGNED.canTransitionTo(TripStatus.STARTED)).isTrue();
        assertThat(TripStatus.ASSIGNED.canTransitionTo(TripStatus.CANCELLED)).isTrue();
        assertThat(TripStatus.ASSIGNED.canTransitionTo(TripStatus.IN_PROGRESS)).isFalse();
        assertThat(TripStatus.ASSIGNED.canTransitionTo(TripStatus.COMPLETED)).isFalse();
    }

    @Test
    void startedMovesToInProgressOrCancelled() {
        assertThat(TripStatus.STARTED.canTransitionTo(TripStatus.IN_PROGRESS)).isTrue();
        assertThat(TripStatus.STARTED.canTransitionTo(TripStatus.CANCELLED)).isTrue();
        assertThat(TripStatus.STARTED.canTransitionTo(TripStatus.COMPLETED)).isFalse();
    }

    @Test
    void inProgressMovesToCompletedOrCancelled() {
        assertThat(TripStatus.IN_PROGRESS.canTransitionTo(TripStatus.COMPLETED)).isTrue();
        assertThat(TripStatus.IN_PROGRESS.canTransitionTo(TripStatus.CANCELLED)).isTrue();
        assertThat(TripStatus.IN_PROGRESS.canTransitionTo(TripStatus.STARTED)).isFalse();
    }

    @Test
    void completedAndCancelledAreTerminal() {
        assertThat(TripStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(TripStatus.CANCELLED.isTerminal()).isTrue();

        for (TripStatus target : TripStatus.values()) {
            assertThat(TripStatus.COMPLETED.canTransitionTo(target)).isFalse();
            assertThat(TripStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void cannotSkipStates() {
        assertThat(TripStatus.SCHEDULED.canTransitionTo(TripStatus.IN_PROGRESS)).isFalse();
        assertThat(TripStatus.SCHEDULED.canTransitionTo(TripStatus.COMPLETED)).isFalse();
        assertThat(TripStatus.ASSIGNED.canTransitionTo(TripStatus.COMPLETED)).isFalse();
        assertThat(TripStatus.STARTED.canTransitionTo(TripStatus.COMPLETED)).isFalse();
    }
}
