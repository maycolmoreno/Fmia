CREATE TABLE IF NOT EXISTS deployment_waves (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    deployment_id UUID NOT NULL REFERENCES deployments(id) ON DELETE CASCADE,
    wave_number INTEGER NOT NULL,
    name VARCHAR(120) NOT NULL,
    target_group VARCHAR(40),
    is_pilot BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(40) NOT NULL DEFAULT 'PLANNED',
    max_failure_percent NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    auto_pause_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    maintenance_window_start TIME,
    maintenance_window_end TIME,
    planned_targets INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_deployment_waves_number UNIQUE (deployment_id, wave_number),
    CONSTRAINT ck_deployment_waves_status CHECK (
        status IN ('PLANNED', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT ck_deployment_waves_failure_percent CHECK (
        max_failure_percent >= 0 AND max_failure_percent <= 100
    )
);

CREATE TABLE IF NOT EXISTS deployment_control_state (
    deployment_id UUID PRIMARY KEY REFERENCES deployments(id) ON DELETE CASCADE,
    status VARCHAR(40) NOT NULL DEFAULT 'READY',
    auto_pause_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_failure_percent NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    retry_limit INTEGER NOT NULL DEFAULT 2,
    next_wave_number INTEGER NOT NULL DEFAULT 1,
    paused_reason TEXT,
    last_evaluated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_deployment_control_status CHECK (
        status IN ('READY', 'RUNNING', 'PAUSED', 'FAILED', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT ck_deployment_control_failure_percent CHECK (
        max_failure_percent >= 0 AND max_failure_percent <= 100
    ),
    CONSTRAINT ck_deployment_control_retry_limit CHECK (retry_limit >= 0)
);

ALTER TABLE deployment_targets
    ADD COLUMN IF NOT EXISTS wave_id UUID REFERENCES deployment_waves(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_deployment_waves_deployment_status
    ON deployment_waves(deployment_id, status, wave_number);

CREATE INDEX IF NOT EXISTS idx_deployment_targets_wave_status
    ON deployment_targets(wave_id, status);
