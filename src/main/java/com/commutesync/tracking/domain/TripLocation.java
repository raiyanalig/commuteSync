package com.commutesync.tracking.domain;

import com.commutesync.common.persistence.BaseEntity;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single location ping for a trip. It also snapshots the trip status at the moment
 * of the ping, so history can be replayed ("where was the bus while IN_PROGRESS").
 */
@Entity
@Table(name = "trip_locations")
public class TripLocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "speed_kph", precision = 5, scale = 2)
    private BigDecimal speedKph;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_status", nullable = false, length = 20)
    private TripStatus tripStatus;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getSpeedKph() {
        return speedKph;
    }

    public void setSpeedKph(BigDecimal speedKph) {
        this.speedKph = speedKph;
    }

    public TripStatus getTripStatus() {
        return tripStatus;
    }

    public void setTripStatus(TripStatus tripStatus) {
        this.tripStatus = tripStatus;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
