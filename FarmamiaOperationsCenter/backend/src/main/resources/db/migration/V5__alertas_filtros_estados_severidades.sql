ALTER TABLE alerts
    DROP CONSTRAINT IF EXISTS ck_alerts_severity;

ALTER TABLE alerts
    ADD CONSTRAINT ck_alerts_severity CHECK (
        severity IN ('INFO', 'WARNING', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    );

ALTER TABLE alerts
    DROP CONSTRAINT IF EXISTS ck_alerts_status;

ALTER TABLE alerts
    ADD CONSTRAINT ck_alerts_status CHECK (
        status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'CLOSED')
    );

CREATE INDEX IF NOT EXISTS ix_alerts_opened_at ON alerts(opened_at);
CREATE INDEX IF NOT EXISTS ix_alerts_type ON alerts(alert_type);
