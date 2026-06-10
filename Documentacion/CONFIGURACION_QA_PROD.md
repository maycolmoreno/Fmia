# Configuracion QA/PROD

## Perfiles

Perfiles soportados:

- `dev`: desarrollo local.
- `qa`: pruebas controladas.
- `prod` o `production`: produccion.

Activar perfil:

```powershell
$env:SPRING_PROFILES_ACTIVE='qa'
```

## Variables obligatorias QA/PROD

```txt
FARMAMIA_DB_URL
FARMAMIA_DB_USER
FARMAMIA_DB_PASSWORD
FARMAMIA_JWT_SECRET
FARMAMIA_CORS_ALLOWED_ORIGINS
FARMAMIA_PACKAGE_STORAGE
```

Produccion con HTTPS interno:

```txt
FARMAMIA_HTTPS_ENABLED=true
FARMAMIA_SSL_KEY_STORE
FARMAMIA_SSL_KEY_STORE_PASSWORD
FARMAMIA_SSL_KEY_STORE_TYPE=PKCS12
```

Opcionales:

```txt
FARMAMIA_API_PORT=8081
FARMAMIA_JWT_EXPIRATION_MINUTES=480
FARMAMIA_SEED_DEMO_ADMIN=false
```

## Reglas de seguridad

- `FARMAMIA_JWT_SECRET` es obligatorio en `qa`, `prod` y `production`.
- El secreto demo `dev-secret-change-me` hace fallar el arranque en perfiles estrictos.
- `admin/admin123` solo debe existir en desarrollo.
- En QA/PROD usar `FARMAMIA_SEED_DEMO_ADMIN=false`.
- No usar hashes `{noop}` fuera de desarrollo.
- Las credenciales de base de datos no deben quedar en archivos versionados.
- El repositorio ZIP debe estar fuera del codigo fuente y configurarse con `FARMAMIA_PACKAGE_STORAGE`.
- Los origenes CORS del panel deben configurarse con `FARMAMIA_CORS_ALLOWED_ORIGINS`, separados por coma.

Ejemplo:

```txt
FARMAMIA_CORS_ALLOWED_ORIGINS=https://ops.farmamia.local,https://ops-qa.farmamia.local
```

## Panel Angular

El panel usa `src/environments/environment.ts` en desarrollo:

```txt
apiBaseUrl=http://localhost:8081
```

En build production usa `src/environments/environment.prod.ts` con `apiBaseUrl` vacio para consumir la API por ruta relativa desde el mismo origen/reverse proxy.

## Semilla demo

La migracion `V3__usuario_admin_desarrollo.sql` esta controlada por el placeholder Flyway:

```txt
seed-demo-admin
```

En `application.yml` queda activa por defecto para desarrollo:

```txt
FARMAMIA_SEED_DEMO_ADMIN=true
```

En `application-qa.yml` y `application-prod.yml` queda desactivada por defecto:

```txt
FARMAMIA_SEED_DEMO_ADMIN=false
```

## Base de datos

QA/PROD no tienen valores por defecto para datasource. Si falta una variable requerida, Spring debe fallar al resolver configuracion.

## Artefactos

No versionar:

- `node_modules/`
- `dist/`
- `target/`
- `bin/`
- `obj/`
- `.vs/`
- `.idea/`
- `*.user`
- `*.log`
- `data/`
- `logs/`
