package com.commutesync.tracking.service;

import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.tracking.domain.TripLocation;
import com.commutesync.tracking.dto.TrackingUpdateRequest;
import com.commutesync.tracking.dto.TripLocationResponse;
import com.commutesync.tracking.event.TripLocationRecordedEvent;
import com.commutesync.tracking.repository.TripLocationRepository;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.repository.TripRepository;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackingService {

    private static final long MAX_FUTURE_SKEW_SECONDS = 60;

    private final TripLocationRepository locationRepository;
    private final TripRepository tripRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TrackingService(TripLocationRepository locationRepository,
                           TripRepository tripRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.locationRepository = locationRepository;
        this.tripRepository = tripRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TripLocationResponse recordLocation(Long tripId, TrackingUpdateRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
        if (trip.getStatus().isTerminal()) {
            throw new BusinessException("Cannot record tracking for a " + trip.getStatus() + " trip");
        }

        TripLocation location = new TripLocation();
        location.setTrip(trip);
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setSpeedKph(request.speedKph());
        location.setTripStatus(trip.getStatus());
        location.setRecordedAt(resolveRecordedAt(request.recordedAt()));

        TripLocation saved = locationRepository.save(location);
        TripLocationResponse response = TripLocationResponse.from(saved);

        // Extension point: a WebSocket/Kafka listener can consume this event later.
        eventPublisher.publishEvent(new TripLocationRecordedEvent(response));

        return response;
    }

    @Transactional(readOnly = true)
    public TripLocationResponse getLatestLocation(Long tripId) {
        ensureTripExists(tripId);
        return locationRepository.findFirstByTripIdOrderByRecordedAtDescIdDesc(tripId)
                .map(TripLocationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking for trip", tripId));
    }

    @Transactional(readOnly = true)
    public PageResponse<TripLocationResponse> getHistory(Long tripId, Pageable pageable) {
        ensureTripExists(tripId);
        Page<TripLocation> page = locationRepository.findByTripIdOrderByRecordedAtDesc(tripId, pageable);
        return PageResponse.from(page.map(TripLocationResponse::from));
    }

    private Instant resolveRecordedAt(Instant recordedAt) {
        Instant now = Instant.now();
        if (recordedAt == null) {
            return now;
        }
        if (recordedAt.isAfter(now.plusSeconds(MAX_FUTURE_SKEW_SECONDS))) {
            throw new BusinessException("recordedAt cannot be in the future");
        }
        return recordedAt;
    }

    private void ensureTripExists(Long tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("Trip", tripId);
        }
    }
}
