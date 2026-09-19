package com.commutesync.tracking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.tracking.domain.TripLocation;
import com.commutesync.tracking.dto.TrackingUpdateRequest;
import com.commutesync.tracking.dto.TripLocationResponse;
import com.commutesync.tracking.event.TripLocationRecordedEvent;
import com.commutesync.tracking.repository.TripLocationRepository;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import com.commutesync.trip.repository.TripRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private TripLocationRepository locationRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TrackingService trackingService;

    @Test
    void recordLocationSnapshotsTripStatus() {
        Trip trip = trip(1L, TripStatus.IN_PROGRESS);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(locationRepository.save(any(TripLocation.class))).thenAnswer(invocation -> {
            TripLocation saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        TripLocationResponse response = trackingService.recordLocation(1L,
                new TrackingUpdateRequest(new BigDecimal("28.613900"), new BigDecimal("77.209000"),
                        new BigDecimal("42.50"), null));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.tripId()).isEqualTo(1L);
        assertThat(response.tripStatus()).isEqualTo(TripStatus.IN_PROGRESS);
        assertThat(response.latitude()).isEqualByComparingTo("28.613900");
        assertThat(response.longitude()).isEqualByComparingTo("77.209000");
        assertThat(response.speedKph()).isEqualByComparingTo("42.50");
    }

    @Test
    void recordLocationPublishesEvent() {
        Trip trip = trip(1L, TripStatus.STARTED);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(locationRepository.save(any(TripLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        trackingService.recordLocation(1L, new TrackingUpdateRequest(
                new BigDecimal("28.6"), new BigDecimal("77.2"), null, null));

        ArgumentCaptor<TripLocationRecordedEvent> captor =
                ArgumentCaptor.forClass(TripLocationRecordedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().location().tripId()).isEqualTo(1L);
    }

    @Test
    void recordLocationDefaultsTimestampToNow() {
        Trip trip = trip(1L, TripStatus.STARTED);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(locationRepository.save(any(TripLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        TripLocationResponse response = trackingService.recordLocation(1L,
                new TrackingUpdateRequest(new BigDecimal("28.6"), new BigDecimal("77.2"), null, null));

        assertThat(response.recordedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void recordLocationUsesProvidedTimestamp() {
        Trip trip = trip(1L, TripStatus.STARTED);
        Instant provided = Instant.now().minusSeconds(30);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(locationRepository.save(any(TripLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripLocationResponse response = trackingService.recordLocation(1L,
                new TrackingUpdateRequest(new BigDecimal("28.6"), new BigDecimal("77.2"), null, provided));

        assertThat(response.recordedAt()).isEqualTo(provided);
    }

    @Test
    void recordLocationRejectsFutureTimestamp() {
        Trip trip = trip(1L, TripStatus.STARTED);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> trackingService.recordLocation(1L, new TrackingUpdateRequest(
                new BigDecimal("28.6"), new BigDecimal("77.2"), null, Instant.now().plusSeconds(3600))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("future");

        verify(locationRepository, never()).save(any(TripLocation.class));
    }

    @Test
    void recordLocationRejectsTerminalTrip() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip(1L, TripStatus.COMPLETED)));

        assertThatThrownBy(() -> trackingService.recordLocation(1L, new TrackingUpdateRequest(
                new BigDecimal("28.6"), new BigDecimal("77.2"), null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COMPLETED");

        verify(locationRepository, never()).save(any(TripLocation.class));
    }

    @Test
    void recordLocationThrowsWhenTripMissing() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.recordLocation(99L, new TrackingUpdateRequest(
                new BigDecimal("28.6"), new BigDecimal("77.2"), null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getLatestLocationReturnsLatest() {
        when(tripRepository.existsById(1L)).thenReturn(true);
        when(locationRepository.findFirstByTripIdOrderByRecordedAtDescIdDesc(1L))
                .thenReturn(Optional.of(location(10L, trip(1L, TripStatus.IN_PROGRESS))));

        TripLocationResponse response = trackingService.getLatestLocation(1L);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void getLatestLocationThrowsWhenNone() {
        when(tripRepository.existsById(1L)).thenReturn(true);
        when(locationRepository.findFirstByTripIdOrderByRecordedAtDescIdDesc(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.getLatestLocation(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getHistoryReturnsPage() {
        when(tripRepository.existsById(1L)).thenReturn(true);
        when(locationRepository.findByTripIdOrderByRecordedAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(location(10L, trip(1L, TripStatus.IN_PROGRESS)))));

        PageResponse<TripLocationResponse> response =
                trackingService.getHistory(1L, PageRequest.of(0, 20));

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void getHistoryThrowsWhenTripMissing() {
        when(tripRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> trackingService.getHistory(99L, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Trip trip(Long id, TripStatus status) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setServiceDate(LocalDate.now().plusDays(1));
        trip.setDepartureTime(LocalTime.of(9, 0));
        trip.setStatus(status);
        return trip;
    }

    private TripLocation location(Long id, Trip trip) {
        TripLocation location = new TripLocation();
        location.setId(id);
        location.setTrip(trip);
        location.setLatitude(new BigDecimal("28.613900"));
        location.setLongitude(new BigDecimal("77.209000"));
        location.setTripStatus(trip.getStatus());
        location.setRecordedAt(Instant.now());
        return location;
    }
}
