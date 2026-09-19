CREATE TABLE trip_locations (
    id          BIGSERIAL PRIMARY KEY,
    trip_id     BIGINT      NOT NULL,
    latitude    NUMERIC(9, 6) NOT NULL,
    longitude   NUMERIC(9, 6) NOT NULL,
    speed_kph   NUMERIC(5, 2),
    trip_status VARCHAR(20) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_trip_locations_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT ck_trip_locations_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_trip_locations_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_trip_locations_speed CHECK (speed_kph IS NULL OR speed_kph >= 0)
);

CREATE INDEX idx_trip_locations_trip_recorded ON trip_locations (trip_id, recorded_at DESC);
