CREATE TABLE trips (
    id                  BIGSERIAL PRIMARY KEY,
    schedule_id         BIGINT,
    route_id            BIGINT      NOT NULL,
    driver_id           BIGINT,
    vehicle_id          BIGINT,
    service_date        DATE        NOT NULL,
    departure_time      TIME        NOT NULL,
    arrival_time        TIME,
    status              VARCHAR(20) NOT NULL,
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    cancellation_reason VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_trips_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (id),
    CONSTRAINT fk_trips_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT fk_trips_driver FOREIGN KEY (driver_id) REFERENCES drivers (id),
    CONSTRAINT fk_trips_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
);

CREATE INDEX idx_trips_date ON trips (service_date);
CREATE INDEX idx_trips_status ON trips (status);
CREATE INDEX idx_trips_driver ON trips (driver_id);
CREATE INDEX idx_trips_vehicle ON trips (vehicle_id);
