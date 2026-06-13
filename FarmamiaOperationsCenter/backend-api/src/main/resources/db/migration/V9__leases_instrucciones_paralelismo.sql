ALTER TABLE deployment_waves
    ADD COLUMN IF NOT EXISTS max_parallel_devices INTEGER NOT NULL DEFAULT 25;

ALTER TABLE deployment_waves
    ADD CONSTRAINT ck_deployment_waves_max_parallel_devices
    CHECK (max_parallel_devices > 0);

ALTER TABLE deployment_targets
    ADD COLUMN IF NOT EXISTS last_instruction_issued_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE deployment_targets
    ADD COLUMN IF NOT EXISTS instruction_lease_until TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_deployment_targets_wave_lease
    ON deployment_targets(wave_id, instruction_lease_until);
