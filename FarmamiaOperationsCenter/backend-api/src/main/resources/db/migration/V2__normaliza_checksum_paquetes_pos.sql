-- Normaliza el tipo del checksum para alinear Flyway, PostgreSQL y Hibernate.

ALTER TABLE pos_packages
    ALTER COLUMN sha256_checksum TYPE VARCHAR(64);
