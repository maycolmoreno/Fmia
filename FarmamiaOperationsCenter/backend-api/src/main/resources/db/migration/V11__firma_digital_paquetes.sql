ALTER TABLE pos_packages
    ADD COLUMN IF NOT EXISTS signature TEXT,
    ADD COLUMN IF NOT EXISTS signature_algorithm VARCHAR(40),
    ADD COLUMN IF NOT EXISTS signing_key_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS signing_public_key_pem TEXT,
    ADD COLUMN IF NOT EXISTS signed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS signature_status VARCHAR(40) NOT NULL DEFAULT 'UNSIGNED';

CREATE INDEX IF NOT EXISTS ix_pos_packages_signature_status
    ON pos_packages(signature_status);
