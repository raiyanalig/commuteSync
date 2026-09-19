CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    action      VARCHAR(40)  NOT NULL,
    actor       VARCHAR(150) NOT NULL,
    entity_type VARCHAR(40),
    entity_id   BIGINT,
    details     VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
