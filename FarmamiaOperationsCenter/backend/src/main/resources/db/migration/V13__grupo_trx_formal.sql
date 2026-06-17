CREATE TABLE grupos_trx (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(160) NOT NULL,
    descripcion TEXT,
    estado VARCHAR(40) NOT NULL DEFAULT 'ACTIVO',
    maximo_equipos INTEGER NOT NULL DEFAULT 100,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_grupos_trx_estado CHECK (estado IN ('ACTIVO', 'PAUSADO', 'RETIRADO')),
    CONSTRAINT ck_grupos_trx_maximo_equipos CHECK (maximo_equipos > 0 AND maximo_equipos <= 100),
    CONSTRAINT ck_grupos_trx_codigo CHECK (codigo ~ '^trx[0-9]{3}$')
);

CREATE TABLE equipo_pos_grupo_trx (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    equipo_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    grupo_trx_id UUID NOT NULL REFERENCES grupos_trx(id),
    asignado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_equipo_pos_grupo_trx_equipo UNIQUE (equipo_id)
);

CREATE INDEX ix_equipo_pos_grupo_trx_grupo ON equipo_pos_grupo_trx(grupo_trx_id);
CREATE INDEX ix_equipo_pos_grupo_trx_equipo ON equipo_pos_grupo_trx(equipo_id);
CREATE INDEX ix_grupos_trx_estado ON grupos_trx(estado);
CREATE INDEX ix_grupos_trx_activo ON grupos_trx(activo);
