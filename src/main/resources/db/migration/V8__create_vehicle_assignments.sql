CREATE TABLE vehicle_assignments (
    id                 BIGSERIAL PRIMARY KEY,
    schedule_id        BIGINT      NOT NULL,
    vehicle_id         BIGINT      NOT NULL,
    passenger_count    INT         NOT NULL,
    allocated_capacity INT         NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vehicle_assignments_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (id),
    CONSTRAINT fk_vehicle_assignments_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    -- A vehicle can be allocated at most once per schedule.
    CONSTRAINT uk_vehicle_assignments_schedule_vehicle UNIQUE (schedule_id, vehicle_id),
    CONSTRAINT ck_vehicle_assignments_passengers CHECK (passenger_count >= 0),
    CONSTRAINT ck_vehicle_assignments_capacity CHECK (allocated_capacity > 0)
);

CREATE INDEX idx_vehicle_assignments_schedule ON vehicle_assignments (schedule_id);
