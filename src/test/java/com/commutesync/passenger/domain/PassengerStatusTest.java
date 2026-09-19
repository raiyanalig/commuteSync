package com.commutesync.passenger.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PassengerStatusTest {

    @Test
    void expectedMovesToConfirmedOrCancelled() {
        assertThat(PassengerStatus.EXPECTED.canTransitionTo(PassengerStatus.CONFIRMED)).isTrue();
        assertThat(PassengerStatus.EXPECTED.canTransitionTo(PassengerStatus.CANCELLED)).isTrue();
        assertThat(PassengerStatus.EXPECTED.canTransitionTo(PassengerStatus.BOARDED)).isFalse();
    }

    @Test
    void confirmedMovesToBoardedOrCancelled() {
        assertThat(PassengerStatus.CONFIRMED.canTransitionTo(PassengerStatus.BOARDED)).isTrue();
        assertThat(PassengerStatus.CONFIRMED.canTransitionTo(PassengerStatus.CANCELLED)).isTrue();
        assertThat(PassengerStatus.CONFIRMED.canTransitionTo(PassengerStatus.EXPECTED)).isFalse();
    }

    @Test
    void boardedAndCancelledAreTerminal() {
        assertThat(PassengerStatus.BOARDED.isTerminal()).isTrue();
        assertThat(PassengerStatus.CANCELLED.isTerminal()).isTrue();

        for (PassengerStatus target : PassengerStatus.values()) {
            assertThat(PassengerStatus.BOARDED.canTransitionTo(target)).isFalse();
            assertThat(PassengerStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void cancelledIsNotActive() {
        assertThat(PassengerStatus.CANCELLED.isActive()).isFalse();
        assertThat(PassengerStatus.EXPECTED.isActive()).isTrue();
        assertThat(PassengerStatus.BOARDED.isActive()).isTrue();
    }
}
