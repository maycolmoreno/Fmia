# Guia de Verificacion MVP

## Requisitos

- Java 21.
- Node.js compatible con Angular 18.
- .NET SDK instalado. En esta maquina se usa .NET 10 con `DOTNET_ROLL_FORWARD=Major` para ejecutar binarios `net8.0`.
- Docker Desktop activo para ejecutar pruebas de integracion backend con Testcontainers.
- Acceso a internet en el primer uso del Maven Wrapper, para descargar Apache Maven 3.9.9.

## Verificacion unica

Desde la raiz del proyecto:

```powershell
cd FarmamiaOperationsCenter
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\verificar-mvp.ps1
```

El script ejecuta:

- validacion de estructura;
- build Angular;
- tests Angular si existen archivos `*.spec.ts`;
- tests backend con `backend-api\mvnw.cmd test`;
- tests del agente Windows con `DOTNET_ROLL_FORWARD=Major`;
- verificacion de existencia de scripts demo/E2E.

## Verificacion en CI

El workflow esta en:

```txt
.github/workflows/mvp-verification.yml
```

Valida en cada push o pull request:

- backend con Java 21 y `./mvnw test`;
- panel Angular con Node.js 22, `npm ci` y `npm run build`;
- agente Windows con .NET 8 y `dotnet test`;
- parseo de scripts PowerShell de verificacion.

En GitHub Actions, Docker esta disponible en `ubuntu-latest`, por lo que las pruebas Testcontainers del backend deben ejecutarse en CI.

Opciones:

```powershell
.\herramientas\verificacion\verificar-mvp.ps1 -OmitirBackend
.\herramientas\verificacion\verificar-mvp.ps1 -OmitirAngular
.\herramientas\verificacion\verificar-mvp.ps1 -OmitirAgente
.\herramientas\verificacion\verificar-mvp.ps1 -ModoRapido
```

## Backend

Windows:

```powershell
cd FarmamiaOperationsCenter\backend-api
.\mvnw.cmd test
```

Linux/macOS:

```bash
cd FarmamiaOperationsCenter/backend-api
./mvnw test
```

Las pruebas de integracion usan:

```java
@Testcontainers(disabledWithoutDocker = true)
```

Si Docker esta disponible, se ejecutan las pruebas de:

- paquetes ZIP reales;
- instrucciones generadas por despliegue;
- eventos, resultado y alerta;
- seguridad por rol;
- auditoria administrativa;
- filtros de alertas.

Si Docker no esta disponible, JUnit/Testcontainers omite esas pruebas y se mantienen las unitarias.

## Panel Angular

```powershell
cd FarmamiaOperationsCenter\admin-panel
npm run build
```

Si se agregan pruebas:

```powershell
npm test -- --watch=false
```

## Agente Windows

```powershell
cd FarmamiaOperationsCenter\windows-agent
$env:DOTNET_ROLL_FORWARD='Major'
dotnet test Farmamia.Agent.Tests\Farmamia.Agent.Tests.csproj
```

## E2E exitoso y fallido

### Stack local con PostgreSQL instalado en Windows

Este es el modo recomendado si ya tienes PostgreSQL local escuchando en `localhost:5432`.

Credenciales usadas actualmente:

```txt
DB: farmamia_ops
User: postgres
Password: .r4e3w2q1
```

Levantar backend y panel sin Docker:

```powershell
cd FarmamiaOperationsCenter
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\levantar-stack-mvp-local.ps1 -PostgresLocal
```

Detener backend y panel:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\detener-stack-mvp-local.ps1
```

El script no apaga PostgreSQL local.

### Stack local con Docker

Con Docker Desktop activo:

```powershell
cd FarmamiaOperationsCenter
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\levantar-stack-mvp-local.ps1
```

Esto levanta:

- PostgreSQL con `infraestructura/local/docker-compose.mvp.yml`;
- backend en `http://localhost:8081`;
- panel Angular en `http://localhost:4200`;
- logs en `.runtime/logs`.

Ejecutar E2E demo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\ejecutar-e2e-demo-local.ps1
```

Detener stack:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\verificacion\detener-stack-mvp-local.ps1
```

### Flujo manual recomendado

1. Levantar PostgreSQL local o de QA.
2. Levantar backend con variables de entorno del ambiente.
3. Levantar panel Angular.
4. Publicar o instalar agente Windows.
5. Ejecutar escenario exitoso.
6. Ejecutar escenario fallido.

Scripts disponibles:

```powershell
.\herramientas\desarrollo\registrar-agente-demo.ps1
.\herramientas\desarrollo\crear-despliegue-demo.ps1
.\herramientas\desarrollo\crear-actualizacion-exitosa-demo.ps1
.\herramientas\desarrollo\crear-alerta-fallo-demo.ps1
```

Scripts para maquina Windows limpia:

```txt
herramientas/e2e-agente-windows/01-preparar-entorno.ps1
herramientas/e2e-agente-windows/02-instalar-agente.ps1
herramientas/e2e-agente-windows/03-validar-servicio.ps1
herramientas/e2e-agente-windows/04-validar-heartbeat.ps1
herramientas/e2e-agente-windows/05-crear-paquete-demo.ps1
herramientas/e2e-agente-windows/06-crear-despliegue-demo.ps1
herramientas/e2e-agente-windows/07-ejecutar-prueba-exitosa.ps1
herramientas/e2e-agente-windows/08-ejecutar-prueba-fallida.ps1
```

Validaciones esperadas en escenario exitoso:

- objetivo en `COMPLETED`;
- evento `UPDATE_COMPLETED`;
- `posVersion` del equipo actualizada;
- sin alerta nueva por ese objetivo.

Validaciones esperadas en escenario fallido:

- objetivo en `FAILED` o estado de rollback correspondiente;
- alerta `CRITICAL`;
- eventos de validacion/fallo;
- rollback reportado si el fallo ocurre despues de modificar archivos.
