-- Farmamia Operations Center
-- Migracion inicial Flyway para el MVP de actualizacion POS.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    city VARCHAR(120),
    zone VARCHAR(120),
    address TEXT,
    is_on_duty BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL REFERENCES branches(id),
    hostname VARCHAR(120) NOT NULL,
    ip_address VARCHAR(45),
    mac_address VARCHAR(32),
    windows_version VARCHAR(120),
    agent_version VARCHAR(40),
    pos_version VARCHAR(40),
    pos_path TEXT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'REGISTERED',
    last_heartbeat_at TIMESTAMPTZ,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_devices_hostname UNIQUE (hostname),
    CONSTRAINT uq_devices_mac_address UNIQUE (mac_address),
    CONSTRAINT ck_devices_status CHECK (
        status IN ('REGISTERED', 'ONLINE', 'OFFLINE', 'DISABLED')
    )
);

CREATE TABLE agent_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ
);

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(180),
    role VARCHAR(40) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_app_users_role CHECK (
        role IN ('ADMIN', 'OPERATOR', 'AUDITOR', 'VIEWER')
    )
);

CREATE TABLE pos_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(40) NOT NULL UNIQUE,
    file_name VARCHAR(260) NOT NULL,
    storage_path TEXT NOT NULL,
    sha256_checksum VARCHAR(64) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'UPLOADED',
    uploaded_by UUID REFERENCES app_users(id),
    approved_by UUID REFERENCES app_users(id),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at TIMESTAMPTZ,
    retired_at TIMESTAMPTZ,
    notes TEXT,
    CONSTRAINT ck_pos_packages_status CHECK (
        status IN ('UPLOADED', 'VALIDATED', 'APPROVED', 'RETIRED', 'REJECTED')
    ),
    CONSTRAINT ck_pos_packages_sha256 CHECK (sha256_checksum ~ '^[a-fA-F0-9]{64}$'),
    CONSTRAINT ck_pos_packages_size CHECK (size_bytes > 0)
);

CREATE TABLE deployments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id UUID NOT NULL REFERENCES pos_packages(id),
    name VARCHAR(180) NOT NULL,
    description TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    pilot_required BOOLEAN NOT NULL DEFAULT TRUE,
    scheduled_at TIMESTAMPTZ,
    official_update_time TIME NOT NULL DEFAULT TIME '23:55:00',
    force_update_time TIME NOT NULL DEFAULT TIME '01:00:00',
    max_failure_percent NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    created_by UUID REFERENCES app_users(id),
    approved_by UUID REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_deployments_status CHECK (
        status IN (
            'DRAFT', 'SCHEDULED', 'PILOT_RUNNING', 'PILOT_COMPLETED',
            'APPROVED', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED', 'CANCELLED'
        )
    ),
    CONSTRAINT ck_deployments_failure_percent CHECK (
        max_failure_percent >= 0 AND max_failure_percent <= 100
    )
);

CREATE TABLE deployment_targets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    deployment_id UUID NOT NULL REFERENCES deployments(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id),
    target_group VARCHAR(40),
    is_pilot BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    old_version VARCHAR(40),
    new_version VARCHAR(40),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    authorized_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_deployment_targets UNIQUE (deployment_id, device_id),
    CONSTRAINT ck_deployment_targets_attempts CHECK (attempt_count >= 0),
    CONSTRAINT ck_deployment_targets_status CHECK (
        status IN (
            'PENDING', 'AUTHORIZED', 'WAITING_WINDOW', 'WAITING_ACTIVITY',
            'DOWNLOADING', 'DOWNLOADED', 'CHECKSUM_VALIDATED', 'BACKING_UP',
            'CLOSING_POS', 'UPDATING', 'VALIDATING', 'COMPLETED', 'FAILED',
            'ROLLBACK_PENDING', 'ROLLING_BACK', 'ROLLBACK_COMPLETED',
            'ROLLBACK_FAILED', 'OFFLINE', 'SKIPPED'
        )
    )
);

CREATE TABLE update_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    deployment_target_id UUID REFERENCES deployment_targets(id) ON DELETE SET NULL,
    deployment_id UUID REFERENCES deployments(id) ON DELETE SET NULL,
    device_id UUID NOT NULL REFERENCES devices(id),
    event_type VARCHAR(60) NOT NULL,
    event_message TEXT,
    old_version VARCHAR(40),
    new_version VARCHAR(40),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_update_events_type CHECK (
        event_type IN (
            'AUTHORIZED', 'DOWNLOAD_STARTED', 'DOWNLOAD_COMPLETED',
            'CHECKSUM_VALIDATED', 'POS_ACTIVITY_DETECTED', 'USER_WARNING_SENT',
            'POS_CLOSED', 'BACKUP_CREATED', 'UPDATE_STARTED', 'UPDATE_COMPLETED',
            'VALIDATION_OK', 'VALIDATION_FAILED', 'ROLLBACK_STARTED',
            'ROLLBACK_COMPLETED', 'FAILED'
        )
    )
);

CREATE TABLE device_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    pos_version VARCHAR(40),
    disk_free_mb INTEGER,
    disk_total_mb INTEGER,
    pos_process_running BOOLEAN,
    latency_ms INTEGER,
    packet_loss_percent NUMERIC(5,2),
    agent_status VARCHAR(40),
    collected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_device_metrics_disk CHECK (
        disk_free_mb IS NULL OR disk_free_mb >= 0
    ),
    CONSTRAINT ck_device_metrics_latency CHECK (
        latency_ms IS NULL OR latency_ms >= 0
    ),
    CONSTRAINT ck_device_metrics_packet_loss CHECK (
        packet_loss_percent IS NULL
        OR (packet_loss_percent >= 0 AND packet_loss_percent <= 100)
    )
);

CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID REFERENCES branches(id),
    device_id UUID REFERENCES devices(id),
    severity VARCHAR(20) NOT NULL,
    alert_type VARCHAR(60) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    acknowledged_by UUID REFERENCES app_users(id),
    acknowledged_at TIMESTAMPTZ,
    closed_by UUID REFERENCES app_users(id),
    closed_at TIMESTAMPTZ,
    CONSTRAINT ck_alerts_severity CHECK (
        severity IN ('INFO', 'WARNING', 'CRITICAL')
    ),
    CONSTRAINT ck_alerts_status CHECK (
        status IN ('OPEN', 'ACKNOWLEDGED', 'CLOSED')
    )
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES app_users(id),
    actor_device_id UUID REFERENCES devices(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_branches_code ON branches(code);
CREATE INDEX ix_devices_branch_id ON devices(branch_id);
CREATE INDEX ix_devices_last_heartbeat_at ON devices(last_heartbeat_at);
CREATE INDEX ix_agent_tokens_device_id ON agent_tokens(device_id);
CREATE UNIQUE INDEX ux_agent_tokens_one_active_per_device
    ON agent_tokens(device_id)
    WHERE revoked_at IS NULL;
CREATE INDEX ix_pos_packages_status ON pos_packages(status);
CREATE INDEX ix_deployments_status ON deployments(status);
CREATE INDEX ix_deployment_targets_deployment_id ON deployment_targets(deployment_id);
CREATE INDEX ix_deployment_targets_device_id ON deployment_targets(device_id);
CREATE INDEX ix_deployment_targets_status ON deployment_targets(status);
CREATE INDEX ix_update_events_device_created ON update_events(device_id, created_at DESC);
CREATE INDEX ix_update_events_target_created ON update_events(deployment_target_id, created_at DESC);
CREATE INDEX ix_device_metrics_device_collected ON device_metrics(device_id, collected_at DESC);
CREATE INDEX ix_alerts_status_severity ON alerts(status, severity);
CREATE INDEX ix_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX ix_audit_logs_created_at ON audit_logs(created_at DESC);
