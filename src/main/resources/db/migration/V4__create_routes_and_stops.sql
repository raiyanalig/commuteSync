CREATE TABLE routes (
    id                         BIGSERIAL PRIMARY KEY,
    route_code                 VARCHAR(20)  NOT NULL,
    name                       VARCHAR(150) NOT NULL,
    source                     VARCHAR(150) NOT NULL,
    destination                VARCHAR(150) NOT NULL,
    distance_km                NUMERIC(6, 2),
    estimated_duration_minutes INT,
    status                     VARCHAR(20)  NOT NULL,
    created_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_routes_code UNIQUE (route_code),
    CONSTRAINT ck_routes_distance CHECK (distance_km IS NULL OR distance_km > 0),
    CONSTRAINT ck_routes_duration CHECK (estimated_duration_minutes IS NULL OR estimated_duration_minutes > 0)
);

CREATE TABLE stops (
    id         BIGSERIAL PRIMARY KEY,
    route_id   BIGINT       NOT NULL,
    name       VARCHAR(150) NOT NULL,
    address    VARCHAR(255),
    stop_order INT          NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_stops_route_order UNIQUE (route_id, stop_order),
    CONSTRAINT fk_stops_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE
);

CREATE INDEX idx_routes_status ON routes (status);
CREATE INDEX idx_stops_route ON stops (route_id);
