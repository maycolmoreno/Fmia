-- Coordenadas reales por farmacia (desbloquea mapa real; nullable porque las 645 farmacias
-- no tienen coordenadas cargadas de origen y se van a ir completando de forma incremental).
ALTER TABLE branches
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

ALTER TABLE branches
    ADD CONSTRAINT ck_branches_latitude CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    ADD CONSTRAINT ck_branches_longitude CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180));
