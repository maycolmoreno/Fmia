-- Soporte para agentes POS huerfanos y normalizacion de Grupos TRX.
-- Esta migracion prepara el aprovisionamiento masivo posterior sin imponer
-- restricciones NOT NULL sobre datos preexistentes.

ALTER TABLE devices
    ALTER COLUMN branch_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS ix_devices_orphans_hostname
    ON devices(hostname)
    WHERE branch_id IS NULL;

ALTER TABLE branches
    ADD COLUMN IF NOT EXISTS grupo_trx_id UUID REFERENCES grupos_trx(id);

CREATE INDEX IF NOT EXISTS ix_branches_grupo_trx_id
    ON branches(grupo_trx_id);

-- 1. ELIMINAR PRIMERO la restricción vieja para liberar la columna
ALTER TABLE grupos_trx
    DROP CONSTRAINT IF EXISTS ck_grupos_trx_codigo;

-- 2. AHORA SÍ se puede hacer el UPDATE sin que salte el CHECK
UPDATE grupos_trx
SET codigo = UPPER(codigo)
WHERE codigo ~* '^trx[0-9]{3}$'; -- Usamos ~* para que sea case-insensitive por seguridad

-- 3. CREAR la nueva restricción adaptada a las mayúsculas
ALTER TABLE grupos_trx
    ADD CONSTRAINT ck_grupos_trx_codigo CHECK (codigo ~ '^TRX[0-9]{3}$');