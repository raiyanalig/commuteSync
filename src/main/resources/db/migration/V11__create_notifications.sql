CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(150) NOT NULL,
    type            VARCHAR(40)  NOT NULL,
    title           VARCHAR(150) NOT NULL,
    message         VARCHAR(500) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    reference_type  VARCHAR(20),
    reference_id    BIGINT,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_email, created_at DESC);
CREATE INDEX idx_notifications_recipient_status ON notifications (recipient_email, status);
