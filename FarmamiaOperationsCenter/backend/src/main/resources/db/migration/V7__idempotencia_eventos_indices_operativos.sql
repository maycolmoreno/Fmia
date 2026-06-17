ALTER TABLE update_events
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(120);

ALTER TABLE update_events
    DROP CONSTRAINT IF EXISTS ck_update_events_type;

ALTER TABLE update_events
    ADD CONSTRAINT ck_update_events_type CHECK (
        event_type IN (
            'AUTHORIZED', 'DOWNLOAD_STARTED', 'DOWNLOAD_COMPLETED',
            'CHECKSUM_VALIDATED', 'POS_ACTIVITY_DETECTED', 'USER_WARNING_SENT',
            'POS_CLOSED', 'BACKUP_CREATED', 'UPDATE_STARTED', 'UPDATE_COMPLETED',
            'UPDATE_FAILED', 'VALIDATION_OK', 'VALIDATION_FAILED', 'ROLLBACK_STARTED',
            'ROLLBACK_COMPLETED', 'ROLLBACK_FAILED', 'FAILED'
        )
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_update_events_device_idempotency
    ON update_events(device_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_devices_status_branch ON devices(status, branch_id);
CREATE INDEX IF NOT EXISTS idx_deployment_targets_deployment_status ON deployment_targets(deployment_id, status);
CREATE INDEX IF NOT EXISTS idx_deployment_targets_device_status ON deployment_targets(device_id, status);
CREATE INDEX IF NOT EXISTS idx_update_events_deployment_created ON update_events(deployment_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_device_status_opened ON alerts(device_id, status, opened_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_created ON audit_logs(actor_user_id, created_at DESC);
