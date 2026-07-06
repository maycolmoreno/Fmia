-- Correlacion automatica de alertas: enlaza alertas de red relacionadas de la misma farmacia
-- a la primera que abrio el incidente (correlation_id = id de esa alerta ancla, NULL si la
-- alerta es ella misma el ancla). Los indices soportan tanto la deduplicacion (dispositivo/
-- farmacia + tipo + estado activo) como la busqueda del ancla de correlacion por farmacia.
ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS correlation_id UUID REFERENCES alerts(id);

CREATE INDEX IF NOT EXISTS idx_alerts_correlation_id ON alerts(correlation_id);
CREATE INDEX IF NOT EXISTS idx_alerts_device_type_status ON alerts(device_id, alert_type, status);
CREATE INDEX IF NOT EXISTS idx_alerts_branch_status ON alerts(branch_id, status);
