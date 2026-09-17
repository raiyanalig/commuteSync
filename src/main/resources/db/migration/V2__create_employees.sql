CREATE TABLE employees (
    id             BIGSERIAL PRIMARY KEY,
    employee_code  VARCHAR(20)  NOT NULL,
    full_name      VARCHAR(150) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    pickup_point   VARCHAR(150),
    shift_id       BIGINT,
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_employees_code UNIQUE (employee_code),
    CONSTRAINT uk_employees_email UNIQUE (email)
);

-- shift_id is intentionally a soft reference: the Shift module (Module 5) does not
-- exist yet. A later migration will add the foreign key once shifts are introduced.
CREATE INDEX idx_employees_status ON employees (status);
