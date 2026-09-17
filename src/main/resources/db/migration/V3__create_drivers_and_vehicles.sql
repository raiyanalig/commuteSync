CREATE TABLE drivers (
    id                  BIGSERIAL PRIMARY KEY,
    full_name           VARCHAR(150) NOT NULL,
    phone               VARCHAR(20)  NOT NULL,
    email               VARCHAR(150) NOT NULL,
    license_number      VARCHAR(50)  NOT NULL,
    license_expiry_date DATE         NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    available           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_drivers_email UNIQUE (email),
    CONSTRAINT uk_drivers_license UNIQUE (license_number)
);

CREATE TABLE vehicles (
    id                  BIGSERIAL PRIMARY KEY,
    registration_number VARCHAR(20)  NOT NULL,
    type                VARCHAR(20)  NOT NULL,
    model               VARCHAR(100),
    capacity            INT          NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    assigned_driver_id  BIGINT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_vehicles_registration UNIQUE (registration_number),
    CONSTRAINT uk_vehicles_assigned_driver UNIQUE (assigned_driver_id),
    CONSTRAINT ck_vehicles_capacity CHECK (capacity > 0),
    CONSTRAINT fk_vehicles_driver FOREIGN KEY (assigned_driver_id) REFERENCES drivers (id)
);

CREATE INDEX idx_drivers_status ON drivers (status);
CREATE INDEX idx_vehicles_status ON vehicles (status);
CREATE INDEX idx_vehicles_assigned_driver ON vehicles (assigned_driver_id);
