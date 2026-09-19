CREATE TABLE bookings (
    id                  BIGSERIAL PRIMARY KEY,
    trip_id             BIGINT      NOT NULL,
    employee_id         BIGINT      NOT NULL,
    seat_number         INT,
    status              VARCHAR(20) NOT NULL,
    booked_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cancelled_at        TIMESTAMPTZ,
    cancellation_reason VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_bookings_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_bookings_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    -- One active row per employee/trip; rebooking reactivates a CANCELLED row.
    CONSTRAINT uk_bookings_trip_employee UNIQUE (trip_id, employee_id),
    -- Backstop for concurrency: a seat number can only be taken once per trip.
    -- Cancelled rows release the seat (seat_number = NULL, multiple NULLs allowed).
    CONSTRAINT uk_bookings_trip_seat UNIQUE (trip_id, seat_number),
    CONSTRAINT ck_bookings_seat CHECK (seat_number IS NULL OR seat_number > 0)
);

CREATE INDEX idx_bookings_trip ON bookings (trip_id);
CREATE INDEX idx_bookings_employee ON bookings (employee_id);
CREATE INDEX idx_bookings_status ON bookings (status);
