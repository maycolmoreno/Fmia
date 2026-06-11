-- Controla la semilla demo despues de V3 sin modificar la migracion ya aplicada.
-- En desarrollo queda activa por defecto. En QA/PROD debe usarse:
-- FARMAMIA_SEED_DEMO_ADMIN=false

DO $$
BEGIN
    IF '${seed-demo-admin}' <> 'true' THEN
        DELETE FROM app_users
        WHERE username = 'admin'
          AND password_hash = '{noop}admin123';
    END IF;
END $$;
