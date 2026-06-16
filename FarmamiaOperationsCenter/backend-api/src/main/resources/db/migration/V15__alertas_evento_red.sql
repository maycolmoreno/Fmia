-- Soporte para alertas originadas en eventos de red (MikroTik → Prometheus → Alertmanager → Backend).
-- Las alertas de red no tienen device_id (son a nivel de sucursal), por eso network_event
-- y branch_code_red permiten identificarlas y filtrarlas sin violar el modelo existente.

ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS network_event BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS branch_code_red VARCHAR(30);

CREATE INDEX IF NOT EXISTS ix_alerts_network_event
    ON alerts(network_event)
    WHERE network_event = TRUE;
