-- Progreso porcentual de descarga por objetivo de despliegue.
-- Actualizado por el agente Windows cada vez que avanza un 10%.
-- Rango 0.00-100.00. NULL mientras no haya iniciado descarga.
ALTER TABLE deployment_targets
    ADD COLUMN IF NOT EXISTS download_progress NUMERIC(5, 2);

COMMENT ON COLUMN deployment_targets.download_progress IS
    'Porcentaje de descarga del paquete ZIP (0.00–100.00). '
    'Actualizado por el agente cada 10% durante estado DOWNLOADING.';
