CREATE TABLE campana_grupo_trx (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campana_id UUID NOT NULL REFERENCES deployments(id) ON DELETE CASCADE,
    grupo_trx_id UUID NOT NULL REFERENCES grupos_trx(id),
    orden INTEGER NOT NULL DEFAULT 1,
    estado VARCHAR(40) NOT NULL DEFAULT 'PENDIENTE',
    motivo_pausa TEXT,
    iniciado_en TIMESTAMPTZ,
    finalizado_en TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_campana_grupo_trx UNIQUE (campana_id, grupo_trx_id),
    CONSTRAINT ck_campana_grupo_trx_estado CHECK (
        estado IN (
            'PENDIENTE',
            'EN_EJECUCION',
            'EN_RIESGO',
            'PAUSADO',
            'BLOQUEADO',
            'COMPLETADO',
            'COMPLETADO_CON_FALLOS',
            'FALLIDO'
        )
    )
);

ALTER TABLE deployment_targets
    ADD COLUMN grupo_trx_id UUID REFERENCES grupos_trx(id);

CREATE INDEX ix_campana_grupo_trx_campana ON campana_grupo_trx(campana_id);
CREATE INDEX ix_campana_grupo_trx_grupo ON campana_grupo_trx(grupo_trx_id);
CREATE INDEX ix_campana_grupo_trx_estado ON campana_grupo_trx(estado);
CREATE INDEX ix_deployment_targets_grupo_trx ON deployment_targets(grupo_trx_id);
