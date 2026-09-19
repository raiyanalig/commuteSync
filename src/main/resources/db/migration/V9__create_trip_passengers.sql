CREATE TABLE trip_passengers (
    id           BIGSERIAL PRIMARY KEY,
    trip_id      BIGINT      NOT NULL,
    booking_id   BIGINT      NOT NULL,
    employee_id  BIGINT      NOT NULL,
    seat_number  INT,
    status       VARCHAR(20) NOT NULL,
    confirmed_at TIMESTAMPTZ,
    boarded_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_trip_passengers_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_trip_passengers_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_trip_passengers_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    -- One manifest row per employee per trip.
    CONSTRAINT uk_trip_passengers_trip_employee UNIQUE (trip_id, employee_id),
    -- A seat is taken at most once per trip (cancelled rows release it with NULL).
    CONSTRAINT uk_trip_passengers_trip_seat UNIQUE (trip_id, seat_number),
    CONSTRAINT ck_trip_passengers_seat CHECK (seat_number IS NULL OR seat_number > 0)
);

CREATE INDEX idx_trip_passengers_trip ON trip_passengers (trip_id);
CREATE INDEX idx_trip_passengers_booking ON trip_passengers (booking_id);
