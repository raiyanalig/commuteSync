package com.commutesync.tracking.event;

import com.commutesync.tracking.dto.TripLocationResponse;

/**
 * Published after a location ping is persisted. No listener is required today; a
 * WebSocket handler (or Kafka producer later) can subscribe without touching the
 * tracking service.
 */
public record TripLocationRecordedEvent(TripLocationResponse location) {
}
