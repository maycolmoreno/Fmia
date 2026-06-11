-- Usuario administrativo inicial para desarrollo local.
-- Cambiar credenciales antes de usar en un entorno real.

INSERT INTO app_users (username, password_hash, full_name, email, role, is_active)
VALUES (
    'admin',
    '{noop}admin123',
    'Administrador Farmamia',
    'admin@farmamia.local',
    'ADMIN',
    TRUE
)
ON CONFLICT (username) DO NOTHING;
