ALTER TABLE deployment_targets
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_deployment_targets_retry
    ON deployment_targets(deployment_id, status, next_retry_at);
