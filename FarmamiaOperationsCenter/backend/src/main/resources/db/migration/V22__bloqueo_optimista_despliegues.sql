-- Bloqueo optimista para evitar carreras entre el scheduler de orquestacion (cada 60s) y
-- acciones manuales del admin sobre el mismo despliegue/objetivo/estado-de-control.
ALTER TABLE deployments
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE deployment_targets
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE deployment_control_state
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
