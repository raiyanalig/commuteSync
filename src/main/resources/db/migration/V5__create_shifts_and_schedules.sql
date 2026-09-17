CREATE TABLE shifts (
    id         BIGSERIAL PRIMARY KEY,
    shift_code VARCHAR(20)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    start_time TIME         NOT NULL,
    end_time   TIME         NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_shifts_code UNIQUE (shift_code)
);

CREATE TABLE schedules (
    id             BIGSERIAL PRIMARY KEY,
    shift_id       BIGINT      NOT NULL,
    route_id       BIGINT      NOT NULL,
    service_date   DATE        NOT NULL,
    departure_time TIME        NOT NULL,
    arrival_time   TIME,
    vehicle_id     BIGINT,
    driver_id      BIGINT,
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_schedules_shift FOREIGN KEY (shift_id) REFERENCES shifts (id),
    CONSTRAINT fk_schedules_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT fk_schedules_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT fk_schedules_driver FOREIGN KEY (driver_id) REFERENCES drivers (id),
    CONSTRAINT uk_schedules_slot UNIQUE (service_date, shift_id, route_id, departure_time)
);

-- Close the soft reference left by Module 2 now that shifts exist.
ALTER TABLE employees
    ADD CONSTRAINT fk_employees_shift FOREIGN KEY (shift_id) REFERENCES shifts (id);

CREATE INDEX idx_shifts_status ON shifts (status);
CREATE INDEX idx_schedules_date ON schedules (service_date);
CREATE INDEX idx_schedules_status ON schedules (status);
CREATE INDEX idx_employees_shift ON employees (shift_id);
